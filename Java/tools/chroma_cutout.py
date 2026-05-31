from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image


Color = tuple[int, int, int, int]


def detect_key_kind(image: Image.Image) -> str:
    rgba = image.convert("RGBA")
    px = rgba.load()
    width, height = rgba.size
    border = max(2, min(width, height) // 32)
    magenta = 0
    green = 0
    total = 0
    for y in range(height):
        for x in range(width):
            if not (x < border or y < border or x >= width - border or y >= height - border):
                continue
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            total += 1
            if is_magenta_key((r, g, b)):
                magenta += 1
            if is_green_key((r, g, b)):
                green += 1
    if green > magenta and green > total * 0.08:
        return "green"
    return "magenta"


def is_magenta_key(rgb: tuple[int, int, int]) -> bool:
    r, g, b = rgb
    if r > 170 and b > 160 and g < 110 and abs(r - b) < 120:
        return True
    return r > 120 and b > 112 and g < 90 and r >= g + 48 and b >= g + 42 and abs(r - b) < 150


def is_green_key(rgb: tuple[int, int, int]) -> bool:
    r, g, b = rgb
    if g > 205 and r < 120 and b < 120:
        return True
    return g > 165 and g >= r + 70 and g >= b + 70 and max(r, b) < 150


def is_key(rgb: tuple[int, int, int], key_kind: str) -> bool:
    return is_green_key(rgb) if key_kind == "green" else is_magenta_key(rgb)


def is_spill(color: Color, key_kind: str) -> bool:
    r, g, b, a = color
    if a == 0:
        return False
    if key_kind == "green":
        neon_green = g > 130 and g >= r + 44 and g >= b + 44 and max(r, b) < 170
        return neon_green
    purple_bias = r > g + 18 and b > g + 32 and b >= r - 30 and r >= 42 and b >= 64
    saturated_edge = g <= 76 and (r + b) > (g * 3 + 118)
    dark_key_matte = r >= 24 and b >= 34 and g <= 46 and r >= g + 16 and b >= g + 24 and b >= r - 52
    bright_key_matte = r >= 70 and b >= 76 and g <= 88 and r >= g + 24 and b >= g + 28 and abs(r - b) < 100
    return (purple_bias and saturated_edge) or dark_key_matte or bright_key_matte


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


def remove_connected_key(image: Image.Image, key_kind: str | None = None) -> Image.Image:
    out = image.convert("RGBA")
    if key_kind is None:
        key_kind = detect_key_kind(out)
    px = out.load()
    width, height = out.size
    seen = [[False for _ in range(width)] for _ in range(height)]
    q: deque[tuple[int, int]] = deque()
    for x in range(width):
        q.append((x, 0))
        q.append((x, height - 1))
    for y in range(height):
        q.append((0, y))
        q.append((width - 1, y))

    while q:
        x, y = q.popleft()
        if not (0 <= x < width and 0 <= y < height) or seen[y][x]:
            continue
        seen[y][x] = True
        r, g, b, a = px[x, y]
        if a == 0:
            continue
        if not is_key((r, g, b), key_kind):
            continue
        px[x, y] = (r, g, b, 0)
        q.append((x + 1, y))
        q.append((x - 1, y))
        q.append((x, y + 1))
        q.append((x, y - 1))
    return out


def remove_key_pixels(image: Image.Image, key_kind: str | None = None) -> Image.Image:
    out = image.convert("RGBA")
    if key_kind is None:
        key_kind = detect_key_kind(out)
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if key_kind == "green":
                key_pixel = g > 220 and r < 95 and b < 95
            else:
                key_pixel = is_key((r, g, b), key_kind)
            if key_pixel:
                px[x, y] = (r, g, b, 0)
    return out


def clean_spill_edges(image: Image.Image, key_kind: str | None = None, passes: int = 2) -> Image.Image:
    out = image.convert("RGBA")
    if key_kind is None:
        key_kind = detect_key_kind(out)
    for _ in range(passes):
        source = out.copy()
        src = source.load()
        dst = out.load()
        width, height = out.size
        for y in range(height):
            for x in range(width):
                color = src[x, y]
                if not is_spill(color, key_kind) or not touches_transparency(src, width, height, x, y):
                    continue
                replacement = neighbor_replacement(src, width, height, x, y, key_kind)
                if replacement is None:
                    dst[x, y] = (color[0], color[1], color[2], 0)
                else:
                    dst[x, y] = (replacement[0], replacement[1], replacement[2], min(color[3], replacement[3]))
    return out


def remove_boundary_spill(image: Image.Image, key_kind: str | None = None, passes: int = 3) -> Image.Image:
    out = image.convert("RGBA")
    if key_kind is None:
        key_kind = detect_key_kind(out)
    for _ in range(passes):
        source = out.copy()
        src = source.load()
        dst = out.load()
        width, height = out.size
        changed = 0
        for y in range(height):
            for x in range(width):
                color = src[x, y]
                if not is_spill(color, key_kind):
                    continue
                if not touches_transparency(src, width, height, x, y, radius=1):
                    continue
                replacement = neighbor_replacement(src, width, height, x, y, key_kind, radius=4)
                if replacement is None:
                    dst[x, y] = (color[0], color[1], color[2], 0)
                else:
                    dst[x, y] = (replacement[0], replacement[1], replacement[2], min(color[3], replacement[3]))
                changed += 1
        if changed == 0:
            break
    return out


def component_gap(a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> int:
    ax1, ay1, ax2, ay2 = a
    bx1, by1, bx2, by2 = b
    dx = max(0, max(ax1, bx1) - min(ax2, bx2))
    dy = max(0, max(ay1, by1) - min(ay2, by2))
    return max(dx, dy)


def clear_stray_components(
    image: Image.Image,
    min_pixels: int = 18,
    max_gap: int | None = None,
    keep_largest_only: bool = False,
) -> Image.Image:
    out = image.convert("RGBA")
    px = out.load()
    width, height = out.size
    seen = [[False for _ in range(width)] for _ in range(height)]
    components: list[list[tuple[int, int]]] = []
    for y in range(height):
        for x in range(width):
            if seen[y][x] or px[x, y][3] == 0:
                continue
            component: list[tuple[int, int]] = []
            q: deque[tuple[int, int]] = deque([(x, y)])
            seen[y][x] = True
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
                        if seen[ny][nx] or px[nx, ny][3] == 0:
                            continue
                        seen[ny][nx] = True
                        q.append((nx, ny))
            components.append(component)
    if not components:
        return out
    main = max(components, key=len)
    main_box = (
        min(x for x, _ in main),
        min(y for _, y in main),
        max(x for x, _ in main) + 1,
        max(y for _, y in main) + 1,
    )
    for component in components:
        if component is main:
            continue
        if keep_largest_only:
            for cx, cy in component:
                r, g, b, _ = px[cx, cy]
                px[cx, cy] = (r, g, b, 0)
            continue
        component_box = (
            min(x for x, _ in component),
            min(y for _, y in component),
            max(x for x, _ in component) + 1,
            max(y for _, y in component) + 1,
        )
        if max_gap is not None and len(component) >= min_pixels and component_gap(main_box, component_box) <= max_gap:
            continue
        if max_gap is None:
            main_top = main_box[1]
            main_bottom = main_box[3] - 1
            top = component_box[1]
            bottom = component_box[3] - 1
            overlaps_main_height = bottom >= main_top - 4 and top <= main_bottom + 4
            if len(component) >= min_pixels and overlaps_main_height:
                continue
        for cx, cy in component:
            r, g, b, _ = px[cx, cy]
            px[cx, cy] = (r, g, b, 0)
    return out


def trim_alpha(image: Image.Image, padding: int = 8) -> Image.Image:
    out = image.convert("RGBA")
    bbox = out.getchannel("A").getbbox()
    if bbox is None:
        return out
    left, top, right, bottom = bbox
    return out.crop((
        max(0, left - padding),
        max(0, top - padding),
        min(out.width, right + padding),
        min(out.height, bottom + padding),
    ))


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


def chroma_cutout(
    image: Image.Image,
    key_kind: str | None = None,
    padding: int = 8,
    square: bool = False,
    stray_max_gap: int | None = None,
    keep_largest_only: bool = False,
) -> Image.Image:
    detected = key_kind or detect_key_kind(image)
    out = remove_connected_key(image, detected)
    out = remove_key_pixels(out, detected)
    out = clean_spill_edges(out, detected)
    out = remove_boundary_spill(out, detected)
    out = clear_stray_components(out, max_gap=stray_max_gap, keep_largest_only=keep_largest_only)
    out = clean_spill_edges(out, detected, passes=1)
    out = remove_boundary_spill(out, detected, passes=1)
    return square_trim(out) if square else trim_alpha(out, padding)


def save_chroma_cutout(source: Image.Image, out_path: Path, key_kind: str | None = None, padding: int = 8) -> None:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    chroma_cutout(source, key_kind, padding).save(out_path)
