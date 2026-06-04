from __future__ import annotations

import argparse
import json
from collections import deque
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Literal

from PIL import Image


Color = tuple[int, int, int, int]
Rgb = tuple[int, int, int]
Mode = Literal["auto", "alpha", "magenta", "green", "dark", "light", "paper"]


@dataclass(frozen=True)
class CutoutSettings:
    mode: Mode = "auto"
    padding: int = 8
    square: bool = False
    connected: bool = True
    global_key: bool | None = None
    clean_spill: bool = True
    spill_passes: int = 3
    clear_strays: bool = True
    stray_min_pixels: int = 18
    stray_max_gap: int | None = None
    keep_largest_only: bool = False
    drop_edge_strays: bool = False
    edge_stray_margin: int = 1
    drop_above_strays: bool = False
    above_stray_gap: int = 16
    drop_below_strays: bool = False
    below_stray_gap: int = 16
    drop_small_green_matte: bool = False
    small_green_matte_max_pixels: int = 64
    scrub_transparent_rgb: bool = True
    trim: bool = True


@dataclass(frozen=True)
class CutoutReport:
    mode: str
    source_size: tuple[int, int]
    output_size: tuple[int, int]
    visible_bbox: tuple[int, int, int, int] | None
    visible_pixels: int
    edge_background_pixels: int
    components: int

    @property
    def passed(self) -> bool:
        return self.visible_pixels > 0 and self.edge_background_pixels == 0


def detect_key_kind(image: Image.Image) -> str:
    mode = detect_mode(image)
    return "green" if mode == "green" else "magenta"


def detect_mode(image: Image.Image) -> str:
    rgba = image.convert("RGBA")
    px = rgba.load()
    width, height = rgba.size
    border = max(2, min(width, height) // 24)
    counts = {"alpha": 0, "magenta": 0, "green": 0, "dark": 0, "light": 0, "paper": 0}
    total = 0
    for x, y in border_points(width, height, border):
        r, g, b, a = px[x, y]
        total += 1
        if a <= 8:
            counts["alpha"] += 1
            continue
        rgb = (r, g, b)
        if is_magenta_key(rgb):
            counts["magenta"] += 1
        if is_green_key(rgb):
            counts["green"] += 1
        if is_dark_key(rgb):
            counts["dark"] += 1
        if is_light_key(rgb):
            counts["light"] += 1
        if is_paper_candidate(rgb):
            counts["paper"] += 1

    if total == 0 or counts["alpha"] / total > 0.86:
        return "alpha"
    ranked = sorted(
        ((name, count) for name, count in counts.items() if name != "alpha"),
        key=lambda item: item[1],
        reverse=True,
    )
    name, count = ranked[0]
    if count / max(1, total - counts["alpha"]) >= 0.12:
        return name
    return "magenta"


def border_points(width: int, height: int, border: int) -> Iterable[tuple[int, int]]:
    for y in range(height):
        for x in range(width):
            if x < border or y < border or x >= width - border or y >= height - border:
                yield x, y


def is_magenta_key(rgb: Rgb) -> bool:
    r, g, b = rgb
    if r > 170 and b > 160 and g < 110 and abs(r - b) < 120:
        return True
    return r > 120 and b > 112 and g < 90 and r >= g + 48 and b >= g + 42 and abs(r - b) < 150


def is_green_key(rgb: Rgb) -> bool:
    r, g, b = rgb
    if g > 205 and r < 120 and b < 120:
        return True
    return g > 150 and g >= r + 50 and g >= b + 50 and max(r, b) < 170


def is_screen_green_matte(rgb: Rgb) -> bool:
    r, g, b = rgb
    if g > 205 and r < 120 and b < 120:
        return True
    return 112 <= g and r <= 58 and b <= 48 and g >= r + 72 and g >= b + 70


def is_green_matte_candidate(rgb: Rgb) -> bool:
    r, g, b = rgb
    return g > 80 and g >= r + 22 and g >= b + 30 and r < 145 and b < 100


def is_dark_key(rgb: Rgb) -> bool:
    r, g, b = rgb
    return r < 28 and g < 45 and b < 62


def is_light_key(rgb: Rgb) -> bool:
    r, g, b = rgb
    return r > 212 and g > 212 and b > 212 and max(rgb) - min(rgb) < 18


def is_paper_candidate(rgb: Rgb) -> bool:
    r, g, b = rgb
    hi = max(rgb)
    lo = min(rgb)
    warm_paper = r >= g - 8 and g >= b - 14 and r >= b + 4
    return warm_paper and hi - lo < 82 and r > 145 and g > 125 and b > 96


def is_key(rgb: Rgb, key_kind: str) -> bool:
    if key_kind == "green":
        return is_green_key(rgb)
    if key_kind == "dark":
        return is_dark_key(rgb)
    if key_kind == "light":
        return is_light_key(rgb)
    if key_kind == "paper":
        return is_paper_candidate(rgb)
    return is_magenta_key(rgb)


def sample_background_palette(image: Image.Image, mode: str, limit: int = 8) -> list[Rgb]:
    rgba = image.convert("RGBA")
    px = rgba.load()
    width, height = rgba.size
    border = max(4, min(width, height) // 14)
    buckets: dict[tuple[int, int, int], list[Rgb]] = {}
    for x, y in border_points(width, height, border):
        r, g, b, a = px[x, y]
        if a <= 8:
            continue
        rgb = (r, g, b)
        if not is_background_pixel(rgb, mode, []):
            continue
        key = (r // 10, g // 10, b // 10)
        buckets.setdefault(key, []).append(rgb)
    ranked = sorted(buckets.values(), key=len, reverse=True)
    samples: list[Rgb] = []
    for bucket in ranked[:limit]:
        count = len(bucket)
        samples.append((
            sum(rgb[0] for rgb in bucket) // count,
            sum(rgb[1] for rgb in bucket) // count,
            sum(rgb[2] for rgb in bucket) // count,
        ))
    return samples


def is_background_pixel(rgb: Rgb, mode: str, samples: list[Rgb]) -> bool:
    if mode == "magenta":
        return is_magenta_key(rgb)
    if mode == "green":
        if samples:
            r, g, b = rgb
            for sr, sg, sb in samples:
                dist = abs(r - sr) + abs(g - sg) + abs(b - sb)
                channel_close = abs(r - sr) < 78 and abs(g - sg) < 88 and abs(b - sb) < 72
                green_screen_bias = g >= r + 32 and g >= b + 45 and r < 120 and b < 90
                if green_screen_bias and dist < 138 and channel_close:
                    return True
        return is_screen_green_matte(rgb)
    if mode == "dark":
        return is_dark_key(rgb)
    if mode == "light":
        return is_light_key(rgb)
    if mode == "paper":
        if not is_paper_candidate(rgb):
            return False
        if not samples:
            return True
        r, g, b = rgb
        for sr, sg, sb in samples:
            dist = abs(r - sr) + abs(g - sg) + abs(b - sb)
            channel_close = abs(r - sr) < 54 and abs(g - sg) < 54 and abs(b - sb) < 54
            if dist < 118 and channel_close:
                return True
        return False
    return False


def remove_connected_key(image: Image.Image, key_kind: str | None = None) -> Image.Image:
    mode = key_kind or detect_key_kind(image)
    return remove_connected_background(image, mode)


def remove_connected_background(image: Image.Image, mode: str | None = None) -> Image.Image:
    out = image.convert("RGBA")
    detected = mode or detect_mode(out)
    if detected == "alpha":
        return out
    samples = sample_background_palette(out, detected)
    px = out.load()
    width, height = out.size
    seen = bytearray(width * height)
    q: deque[tuple[int, int]] = deque()
    for x in range(width):
        q.append((x, 0))
        q.append((x, height - 1))
    for y in range(height):
        q.append((0, y))
        q.append((width - 1, y))

    while q:
        x, y = q.popleft()
        if not (0 <= x < width and 0 <= y < height):
            continue
        index = y * width + x
        if seen[index]:
            continue
        seen[index] = 1
        r, g, b, a = px[x, y]
        if a > 8 and not is_background_pixel((r, g, b), detected, samples):
            continue
        if a > 0:
            px[x, y] = (r, g, b, 0)
        q.append((x + 1, y))
        q.append((x - 1, y))
        q.append((x, y + 1))
        q.append((x, y - 1))
    return out


def remove_key_pixels(image: Image.Image, key_kind: str | None = None) -> Image.Image:
    out = image.convert("RGBA")
    mode = key_kind or detect_key_kind(out)
    if mode == "alpha":
        return out
    samples = sample_background_palette(out, mode)
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if mode == "green":
                key_pixel = is_screen_green_matte((r, g, b))
            else:
                key_pixel = is_background_pixel((r, g, b), mode, samples)
            if key_pixel:
                px[x, y] = (r, g, b, 0)
    return out


def is_spill(color: Color, key_kind: str) -> bool:
    r, g, b, a = color
    if a == 0:
        return False
    if key_kind == "green":
        return is_screen_green_matte((r, g, b))
    if key_kind == "dark":
        return max(r, g, b) < 68 and touches_key_bias((r, g, b), key_kind)
    if key_kind == "light":
        return min(r, g, b) > 220 and max(r, g, b) - min(r, g, b) < 24
    if key_kind == "paper":
        return is_paper_candidate((r, g, b))
    purple_bias = r > g + 18 and b > g + 32 and b >= r - 30 and r >= 42 and b >= 64
    saturated_edge = g <= 76 and (r + b) > (g * 3 + 118)
    dark_key_matte = r >= 24 and b >= 34 and g <= 46 and r >= g + 16 and b >= g + 24 and b >= r - 52
    bright_key_matte = r >= 70 and b >= 76 and g <= 88 and r >= g + 24 and b >= g + 28 and abs(r - b) < 100
    return (purple_bias and saturated_edge) or dark_key_matte or bright_key_matte


def touches_key_bias(rgb: Rgb, key_kind: str) -> bool:
    return is_key(rgb, key_kind)


def touches_transparency(px, width: int, height: int, x: int, y: int, radius: int = 2) -> bool:
    for oy in range(-radius, radius + 1):
        for ox in range(-radius, radius + 1):
            if ox == 0 and oy == 0:
                continue
            nx = x + ox
            ny = y + oy
            if nx < 0 or ny < 0 or nx >= width or ny >= height:
                return True
            if px[nx, ny][3] <= 18:
                return True
    return False


def neighbor_replacement(px, width: int, height: int, x: int, y: int, key_kind: str, radius: int = 3) -> Color | None:
    red = green = blue = alpha = weight_sum = 0
    for oy in range(-radius, radius + 1):
        for ox in range(-radius, radius + 1):
            if ox == 0 and oy == 0:
                continue
            nx = x + ox
            ny = y + oy
            if nx < 0 or ny < 0 or nx >= width or ny >= height:
                continue
            color = px[nx, ny]
            if color[3] <= 36 or is_spill(color, key_kind):
                continue
            distance = abs(ox) + abs(oy)
            weight = max(1, radius + 2 - distance) * color[3]
            red += color[0] * weight
            green += color[1] * weight
            blue += color[2] * weight
            alpha += color[3] * weight
            weight_sum += weight
    if weight_sum == 0:
        return None
    return red // weight_sum, green // weight_sum, blue // weight_sum, alpha // weight_sum


def clean_spill_edges(image: Image.Image, key_kind: str | None = None, passes: int = 2) -> Image.Image:
    out = image.convert("RGBA")
    mode = key_kind or detect_key_kind(out)
    if mode == "alpha":
        return out
    for _ in range(passes):
        source = out.copy()
        src = source.load()
        dst = out.load()
        width, height = out.size
        changed = 0
        for y in range(height):
            for x in range(width):
                color = src[x, y]
                if not is_spill(color, mode) or not touches_transparency(src, width, height, x, y):
                    continue
                replacement = neighbor_replacement(src, width, height, x, y, mode)
                if replacement is None:
                    dst[x, y] = (color[0], color[1], color[2], 0)
                else:
                    dst[x, y] = (replacement[0], replacement[1], replacement[2], min(color[3], replacement[3]))
                changed += 1
        if changed == 0:
            break
    return out


def remove_boundary_spill(image: Image.Image, key_kind: str | None = None, passes: int = 3) -> Image.Image:
    return clean_spill_edges(image, key_kind, passes)


def component_gap(a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> int:
    ax1, ay1, ax2, ay2 = a
    bx1, by1, bx2, by2 = b
    dx = max(0, max(ax1, bx1) - min(ax2, bx2))
    dy = max(0, max(ay1, by1) - min(ay2, by2))
    return max(dx, dy)


def alpha_components(image: Image.Image, threshold: int = 0) -> list[list[tuple[int, int]]]:
    out = image.convert("RGBA")
    px = out.load()
    width, height = out.size
    seen = bytearray(width * height)
    components: list[list[tuple[int, int]]] = []
    for y in range(height):
        for x in range(width):
            index = y * width + x
            if seen[index] or px[x, y][3] <= threshold:
                continue
            component: list[tuple[int, int]] = []
            q: deque[tuple[int, int]] = deque([(x, y)])
            seen[index] = 1
            while q:
                cx, cy = q.popleft()
                component.append((cx, cy))
                for oy in (-1, 0, 1):
                    for ox in (-1, 0, 1):
                        if ox == 0 and oy == 0:
                            continue
                        nx = cx + ox
                        ny = cy + oy
                        if not (0 <= nx < width and 0 <= ny < height):
                            continue
                        next_index = ny * width + nx
                        if seen[next_index] or px[nx, ny][3] <= threshold:
                            continue
                        seen[next_index] = 1
                        q.append((nx, ny))
            components.append(component)
    return components


def clear_stray_components(
    image: Image.Image,
    min_pixels: int = 18,
    max_gap: int | None = None,
    keep_largest_only: bool = False,
) -> Image.Image:
    out = image.convert("RGBA")
    px = out.load()
    components = alpha_components(out)
    if not components:
        return out
    main = max(components, key=len)
    main_box = component_box(main)
    for component in components:
        if component is main:
            continue
        if keep_largest_only:
            clear_component(px, component)
            continue
        box = component_box(component)
        if max_gap is not None and len(component) >= min_pixels and component_gap(main_box, box) <= max_gap:
            continue
        if max_gap is None:
            top = box[1]
            bottom = box[3] - 1
            overlaps_main_height = bottom >= main_box[1] - 4 and top <= main_box[3] + 3
            if len(component) >= min_pixels and overlaps_main_height:
                continue
        clear_component(px, component)
    return out


def clear_edge_stray_components(image: Image.Image, margin: int = 1) -> Image.Image:
    out = image.convert("RGBA")
    px = out.load()
    components = alpha_components(out)
    if not components:
        return out
    main = max(components, key=len)
    for component in components:
        if component is main:
            continue
        if component_touches_side_or_top(component, out.size, margin):
            clear_component(px, component)
    return out


def component_touches_side_or_top(component: list[tuple[int, int]], size: tuple[int, int], margin: int) -> bool:
    width, _ = size
    for x, y in component:
        if x <= margin or x >= width - 1 - margin or y <= margin:
            return True
    return False


def clear_above_stray_components(image: Image.Image, min_gap: int = 16) -> Image.Image:
    out = image.convert("RGBA")
    px = out.load()
    components = alpha_components(out)
    if not components:
        return out
    main = max(components, key=len)
    main_box = component_box(main)
    for component in components:
        if component is main:
            continue
        box = component_box(component)
        if box[3] <= main_box[1] - min_gap:
            clear_component(px, component)
    return out


def clear_below_stray_components(image: Image.Image, min_gap: int = 16) -> Image.Image:
    out = image.convert("RGBA")
    px = out.load()
    components = alpha_components(out)
    if not components:
        return out
    main = max(components, key=len)
    main_box = component_box(main)
    for component in components:
        if component is main:
            continue
        box = component_box(component)
        if box[1] >= main_box[3] + min_gap:
            clear_component(px, component)
    return out


def clear_small_green_matte_components(image: Image.Image, max_pixels: int = 64) -> Image.Image:
    out = image.convert("RGBA")
    px = out.load()
    width, height = out.size
    seen = bytearray(width * height)
    for y in range(height):
        for x in range(width):
            index = y * width + x
            if seen[index] or px[x, y][3] <= 8 or not is_green_matte_candidate(px[x, y][:3]):
                continue
            component: list[tuple[int, int]] = []
            q: deque[tuple[int, int]] = deque([(x, y)])
            seen[index] = 1
            while q:
                cx, cy = q.popleft()
                component.append((cx, cy))
                for ny in range(max(0, cy - 1), min(height, cy + 2)):
                    for nx in range(max(0, cx - 1), min(width, cx + 2)):
                        next_index = ny * width + nx
                        if seen[next_index] or px[nx, ny][3] <= 8 or not is_green_matte_candidate(px[nx, ny][:3]):
                            continue
                        seen[next_index] = 1
                        q.append((nx, ny))
            if len(component) <= max_pixels:
                clear_component(px, component)
    return out


def component_box(component: list[tuple[int, int]]) -> tuple[int, int, int, int]:
    return (
        min(x for x, _ in component),
        min(y for _, y in component),
        max(x for x, _ in component) + 1,
        max(y for _, y in component) + 1,
    )


def clear_component(px, component: list[tuple[int, int]]) -> None:
    for x, y in component:
        r, g, b, _ = px[x, y]
        px[x, y] = (r, g, b, 0)


def trim_alpha(image: Image.Image, padding: int = 8) -> Image.Image:
    out = image.convert("RGBA")
    bbox = out.getchannel("A").getbbox()
    if bbox is None:
        return out
    left, top, right, bottom = bbox
    crop_box = (
        max(0, left - padding),
        max(0, top - padding),
        min(out.width, right + padding),
        min(out.height, bottom + padding),
    )
    cropped = out.crop(crop_box)
    target_width = max(1, right - left + padding * 2)
    target_height = max(1, bottom - top + padding * 2)
    framed = Image.new("RGBA", (target_width, target_height), (0, 0, 0, 0))
    framed.alpha_composite(cropped, (crop_box[0] - left + padding, crop_box[1] - top + padding))
    return framed


def square_trim(image: Image.Image, padding_ratio: float = 0.06) -> Image.Image:
    out = image.convert("RGBA")
    bbox = out.getchannel("A").getbbox()
    if bbox is None:
        return out
    left, top, right, bottom = bbox
    width = right - left
    height = bottom - top
    padding = max(6, int(max(width, height) * padding_ratio))
    cropped = out.crop((
        max(0, left - padding),
        max(0, top - padding),
        min(out.width, right + padding),
        min(out.height, bottom + padding),
    ))
    side = max(cropped.width, cropped.height)
    framed = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    framed.alpha_composite(cropped, ((side - cropped.width) // 2, (side - cropped.height) // 2))
    return framed


def fit(image: Image.Image, width: int, height: int, bottom_align: bool = True, margin: int = 4) -> Image.Image:
    source = trim_alpha(image, 0)
    max_w = width - margin * 2
    max_h = height - margin * 2
    scale = min(max_w / source.width, max_h / source.height)
    draw_w = max(1, int(round(source.width * scale)))
    draw_h = max(1, int(round(source.height * scale)))
    resized = source.resize((draw_w, draw_h), Image.Resampling.NEAREST)
    canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    x = (width - draw_w) // 2
    y = height - draw_h - margin if bottom_align else (height - draw_h) // 2
    canvas.alpha_composite(resized, (x, y))
    return canvas


def scrub_transparent_rgb(image: Image.Image, alpha_threshold: int = 0) -> Image.Image:
    out = image.convert("RGBA")
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a <= alpha_threshold:
                px[x, y] = (0, 0, 0, 0)
    return out


def universal_cutout(image: Image.Image, settings: CutoutSettings | None = None) -> Image.Image:
    settings = settings or CutoutSettings()
    out = image.convert("RGBA")
    mode = detect_mode(out) if settings.mode == "auto" else settings.mode
    if settings.connected:
        out = remove_connected_background(out, mode)
    global_key = settings.global_key
    if global_key is None:
        global_key = mode in {"magenta", "green"}
    if global_key:
        out = remove_key_pixels(out, mode)
    if settings.clean_spill and mode not in {"alpha"}:
        out = clean_spill_edges(out, mode, settings.spill_passes)
    if settings.clear_strays:
        out = clear_stray_components(
            out,
            min_pixels=settings.stray_min_pixels,
            max_gap=settings.stray_max_gap,
            keep_largest_only=settings.keep_largest_only,
        )
    if settings.drop_edge_strays:
        out = clear_edge_stray_components(out, settings.edge_stray_margin)
    if settings.drop_above_strays:
        out = clear_above_stray_components(out, settings.above_stray_gap)
    if settings.drop_below_strays:
        out = clear_below_stray_components(out, settings.below_stray_gap)
    if settings.drop_small_green_matte and mode == "green":
        out = clear_small_green_matte_components(out, settings.small_green_matte_max_pixels)
    if settings.clean_spill and mode not in {"alpha"}:
        out = clean_spill_edges(out, mode, 1)
    if settings.square:
        out = square_trim(out)
    elif settings.trim:
        out = trim_alpha(out, settings.padding)
    if settings.scrub_transparent_rgb:
        out = scrub_transparent_rgb(out)
    return out


def cut_sheet(
    source: Image.Image,
    out_dir: Path,
    cols: int,
    rows: int,
    names: list[str] | None = None,
    bleed: int = 0,
    bleed_left: int | None = None,
    bleed_top: int | None = None,
    bleed_right: int | None = None,
    bleed_bottom: int | None = None,
    settings: CutoutSettings | None = None,
) -> list[Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    outputs: list[Path] = []
    total = cols * rows
    names = names or [f"cell_{index:02d}" for index in range(total)]
    for index, name in enumerate(names[:total]):
        col = index % cols
        row = index // cols
        cell = crop_cell(
            source,
            col,
            row,
            cols,
            rows,
            bleed,
            bleed_left=bleed_left,
            bleed_top=bleed_top,
            bleed_right=bleed_right,
            bleed_bottom=bleed_bottom,
        )
        out = universal_cutout(cell, settings)
        path = out_dir / f"{name}.png"
        out.save(path)
        outputs.append(path)
    return outputs


def crop_cell(
    sheet: Image.Image,
    col: int,
    row: int,
    cols: int,
    rows: int,
    bleed: int = 0,
    *,
    bleed_left: int | None = None,
    bleed_top: int | None = None,
    bleed_right: int | None = None,
    bleed_bottom: int | None = None,
) -> Image.Image:
    left_bleed = bleed if bleed_left is None else bleed_left
    top_bleed = bleed if bleed_top is None else bleed_top
    right_bleed = bleed if bleed_right is None else bleed_right
    bottom_bleed = bleed if bleed_bottom is None else bleed_bottom
    x1 = round(col * sheet.width / cols)
    y1 = round(row * sheet.height / rows)
    x2 = round((col + 1) * sheet.width / cols)
    y2 = round((row + 1) * sheet.height / rows)
    return sheet.crop((
        max(0, x1 - left_bleed),
        max(0, y1 - top_bleed),
        min(sheet.width, x2 + right_bleed),
        min(sheet.height, y2 + bottom_bleed),
    ))


def quality_report(image: Image.Image, mode: str | None = None) -> CutoutReport:
    out = image.convert("RGBA")
    alpha = out.getchannel("A")
    bbox = alpha.getbbox()
    components = alpha_components(out, threshold=8)
    histogram = alpha.histogram()
    visible = sum(histogram[9:])
    detected = mode or detect_mode(out)
    edge_background = count_edge_background(out, detected)
    return CutoutReport(
        mode=detected,
        source_size=out.size,
        output_size=out.size,
        visible_bbox=bbox,
        visible_pixels=visible,
        edge_background_pixels=edge_background,
        components=len(components),
    )


def count_edge_background(image: Image.Image, mode: str) -> int:
    out = image.convert("RGBA")
    px = out.load()
    samples = sample_background_palette(out, mode)
    count = 0
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a <= 8:
                continue
            if not touches_transparency(px, out.width, out.height, x, y, radius=1):
                continue
            if is_background_pixel((r, g, b), mode, samples):
                count += 1
    return count


def parse_names(value: str | None) -> list[str] | None:
    if not value:
        return None
    path = Path(value)
    if path.exists():
        return [line.strip() for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]
    return [part.strip() for part in value.split(",") if part.strip()]


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Universal sprite/object cutout tool for generated asset sheets.")
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--mode", choices=["auto", "alpha", "magenta", "green", "dark", "light", "paper"], default="auto")
    parser.add_argument("--cols", type=int)
    parser.add_argument("--rows", type=int)
    parser.add_argument("--names", help="Comma-separated output names or a text file with one name per line.")
    parser.add_argument("--bleed", type=int, default=0)
    parser.add_argument("--bleed-left", type=int)
    parser.add_argument("--bleed-top", type=int)
    parser.add_argument("--bleed-right", type=int)
    parser.add_argument("--bleed-bottom", type=int)
    parser.add_argument("--padding", type=int, default=8)
    parser.add_argument("--square", action="store_true")
    parser.add_argument("--no-connected", action="store_true")
    parser.add_argument("--global-key", action="store_true")
    parser.add_argument("--no-global-key", action="store_true")
    parser.add_argument("--keep-largest-only", action="store_true")
    parser.add_argument("--stray-max-gap", type=int)
    parser.add_argument("--no-strays", action="store_true")
    parser.add_argument("--drop-edge-strays", action="store_true")
    parser.add_argument("--edge-stray-margin", type=int, default=1)
    parser.add_argument("--drop-above-strays", action="store_true")
    parser.add_argument("--above-stray-gap", type=int, default=16)
    parser.add_argument("--drop-below-strays", action="store_true")
    parser.add_argument("--below-stray-gap", type=int, default=16)
    parser.add_argument("--drop-small-green-matte", action="store_true")
    parser.add_argument("--small-green-matte-max-pixels", type=int, default=64)
    parser.add_argument("--no-spill-clean", action="store_true")
    parser.add_argument("--report", type=Path)
    return parser


def main() -> None:
    args = build_parser().parse_args()
    if args.global_key and args.no_global_key:
        raise SystemExit("--global-key and --no-global-key cannot both be used.")
    global_key = True if args.global_key else False if args.no_global_key else None
    settings = CutoutSettings(
        mode=args.mode,
        padding=args.padding,
        square=args.square,
        connected=not args.no_connected,
        global_key=global_key,
        clean_spill=not args.no_spill_clean,
        clear_strays=not args.no_strays,
        stray_max_gap=args.stray_max_gap,
        keep_largest_only=args.keep_largest_only,
        drop_edge_strays=args.drop_edge_strays,
        edge_stray_margin=args.edge_stray_margin,
        drop_above_strays=args.drop_above_strays,
        above_stray_gap=args.above_stray_gap,
        drop_below_strays=args.drop_below_strays,
        below_stray_gap=args.below_stray_gap,
        drop_small_green_matte=args.drop_small_green_matte,
        small_green_matte_max_pixels=args.small_green_matte_max_pixels,
    )
    source = Image.open(args.input).convert("RGBA")
    reports: list[dict[str, object]] = []
    if args.cols and args.rows:
        outputs = cut_sheet(
            source,
            args.out,
            args.cols,
            args.rows,
            parse_names(args.names),
            args.bleed,
            args.bleed_left,
            args.bleed_top,
            args.bleed_right,
            args.bleed_bottom,
            settings,
        )
        for path in outputs:
            report = quality_report(Image.open(path).convert("RGBA"), args.mode if args.mode != "auto" else None)
            reports.append({"path": path.as_posix(), **asdict(report), "passed": report.passed})
    else:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        out = universal_cutout(source, settings)
        out.save(args.out)
        report = quality_report(out, args.mode if args.mode != "auto" else None)
        reports.append({"path": args.out.as_posix(), **asdict(report), "passed": report.passed})
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(reports, indent=2), encoding="utf-8")
    failed = [report for report in reports if not report["passed"]]
    if failed:
        raise SystemExit(f"{len(failed)} cutout quality checks failed.")


if __name__ == "__main__":
    main()
