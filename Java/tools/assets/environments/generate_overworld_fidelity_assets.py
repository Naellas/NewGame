from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import math
import random
import shutil
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = JAVA_ROOT
TERRAIN_DIR = ROOT / "assets" / "environments/terrain/common"
ROAD_DIR = ROOT / "assets" / "environments/terrain/roads"
BACKUP_DIR = ROOT / "assets" / "environments/terrain/common" / "before_fidelity_refresh"
TILE = 48
SCALE = 4
SOURCE_SHEET = ROOT / "assets" / "source" / "imagegen-overworld-terrain-fidelity-nosnow.png"
FOREST_SHEET = ROOT / "assets" / "source" / "imagegen-overworld-forest-swatches.png"
SNOW_SHEET = ROOT / "assets" / "source" / "imagegen-overworld-snow-swatches.png"


def clamp(value: int) -> int:
    return max(0, min(255, value))


def mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return (
        clamp(int(a[0] + (b[0] - a[0]) * t)),
        clamp(int(a[1] + (b[1] - a[1]) * t)),
        clamp(int(a[2] + (b[2] - a[2]) * t)),
    )


def periodic_noise(x: int, y: int, seed: int, octaves: tuple[tuple[int, float], ...]) -> float:
    rnd = random.Random(seed)
    value = 0.0
    amp_total = 0.0
    nx = x / TILE
    ny = y / TILE
    for freq, amp in octaves:
        phase_x = rnd.random() * math.tau
        phase_y = rnd.random() * math.tau
        phase_d = rnd.random() * math.tau
        value += math.sin(math.tau * freq * nx + phase_x) * amp
        value += math.cos(math.tau * freq * ny + phase_y) * amp
        value += math.sin(math.tau * freq * (nx + ny) + phase_d) * amp * 0.55
        amp_total += amp * 2.55
    return value / max(0.001, amp_total)


def hash_noise(x: int, y: int, seed: int) -> float:
    value = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xFFFFFFFF
    value = (value ^ (value >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((value ^ (value >> 16)) & 0xFFFF) / 32767.5 - 1.0


def granular_noise(x: int, y: int, seed: int) -> float:
    total = 0.0
    weight = 0.0
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            w = 2.0 if dx == 0 and dy == 0 else (1.0 if dx == 0 or dy == 0 else 0.55)
            total += hash_noise(x + dx, y + dy, seed) * w
            weight += w
    return total / weight


def terrain_base(
    name: str,
    base: tuple[int, int, int],
    low: tuple[int, int, int],
    high: tuple[int, int, int],
    *,
    seed: int,
    water: bool = False,
) -> Image.Image:
    img = Image.new("RGBA", (TILE, TILE))
    px = img.load()
    for y in range(TILE):
        for x in range(TILE):
            n = granular_noise(x, y, seed)
            t = max(0.0, min(1.0, 0.50 + n * 0.68))
            color = mix(low, high, t)
            color = mix(color, base, 0.46)
            if water:
                ripple = math.sin((x * 0.65 + y * 0.18) + seed) * 0.08
                color = mix(color, high, max(0.0, ripple))
            px[x, y] = (*color, 255)
    return img


def draw_grass_details(draw: ImageDraw.ImageDraw, rng: random.Random, density: int, palette: list[tuple[int, int, int]]) -> None:
    for _ in range(density):
        x = rng.randrange(4, TILE - 4)
        y = rng.randrange(4, TILE - 4)
        color = palette[rng.randrange(len(palette))]
        if rng.random() < 0.42:
            dx = rng.choice((-2, -1, 1, 2))
            dy = rng.choice((-2, -1, 1))
            draw.line((x, y, x + dx, y + dy), fill=(*color, 74), width=1)
        else:
            draw.point((x, y), fill=(*color, 96))
    for _ in range(max(2, density // 14)):
        x = rng.randrange(6, TILE - 6)
        y = rng.randrange(6, TILE - 6)
        flower = rng.choice(((218, 202, 87), (220, 145, 174), (196, 211, 236), (238, 230, 184)))
        draw.point((x, y), fill=(*flower, 170))
        draw.point((x + 1, y), fill=(*flower, 120))


def draw_pebbles(draw: ImageDraw.ImageDraw, rng: random.Random, count: int, palette: list[tuple[int, int, int]]) -> None:
    for _ in range(count):
        x = rng.randrange(5, TILE - 5)
        y = rng.randrange(5, TILE - 5)
        r = rng.choice((1, 1, 2))
        color = palette[rng.randrange(len(palette))]
        draw.ellipse((x - r, y - r, x + r, y + r), fill=(*color, 170))


def make_tile(name: str, variant: int = 0) -> Image.Image:
    specs = {
        "grass": ((72, 142, 65), (45, 103, 51), (115, 170, 73), 101),
        "forest": ((45, 101, 55), (31, 73, 43), (79, 133, 64), 107),
        "desert": ((205, 163, 93), (165, 118, 69), (231, 196, 119), 113),
        "tundra": ((194, 216, 221), (145, 175, 184), (235, 244, 238), 127),
        "marsh": ((68, 115, 78), (39, 78, 63), (111, 145, 82), 131),
        "badlands": ((154, 101, 70), (105, 70, 58), (194, 130, 82), 137),
        "mountain": ((108, 111, 104), (64, 74, 76), (164, 153, 127), 149),
        "mountain_massif_tile": ((99, 103, 98), (55, 65, 67), (156, 145, 121), 151),
        "water": ((48, 127, 166), (32, 86, 135), (89, 172, 192), 157),
        "road": ((186, 141, 85), (139, 97, 62), (221, 181, 112), 163),
    }
    base, low, high, seed = specs[name]
    seed += variant * 1009
    img = terrain_base(name, base, low, high, seed=seed, water=name == "water")
    details = Image.new("RGBA", (TILE, TILE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(details, "RGBA")
    rng = random.Random(seed * 997)
    if name in {"grass", "forest", "marsh"}:
        density = 115 if name == "grass" else 80
        grass_palette = [(91, 151, 68), (131, 178, 75), (48, 105, 54), (176, 178, 83)]
        if name == "forest":
            grass_palette = [(38, 93, 46), (68, 125, 58), (91, 129, 68), (28, 68, 42)]
        if name == "marsh":
            grass_palette = [(73, 132, 74), (126, 142, 77), (43, 89, 68), (91, 126, 63)]
        draw_grass_details(draw, rng, density, grass_palette)
        if name == "marsh":
            for _ in range(4):
                x = rng.randrange(7, TILE - 12)
                y = rng.randrange(8, TILE - 10)
                draw.ellipse((x, y, x + rng.randrange(5, 10), y + rng.randrange(2, 5)), fill=(40, 89, 88, 120))
    elif name in {"desert", "badlands", "road"}:
        draw_pebbles(draw, rng, 28 if name == "road" else 20, [(124, 98, 70), (219, 186, 122), (95, 77, 62)])
        for _ in range(8):
            x = rng.randrange(5, TILE - 12)
            y = rng.randrange(5, TILE - 5)
            draw.line((x, y, x + rng.randrange(5, 13), y + rng.randrange(-1, 2)), fill=(238, 203, 134, 82), width=1)
    elif name == "tundra":
        draw_pebbles(draw, rng, 13, [(131, 151, 156), (230, 239, 235), (168, 190, 194)])
        for _ in range(14):
            x = rng.randrange(4, TILE - 7)
            y = rng.randrange(4, TILE - 4)
            draw.line((x, y, x + rng.randrange(3, 8), y), fill=(242, 248, 247, 120), width=1)
    elif name in {"mountain", "mountain_massif_tile"}:
        draw_pebbles(draw, rng, 36, [(55, 62, 63), (130, 126, 111), (181, 166, 135)])
        for _ in range(8):
            x = rng.randrange(4, TILE - 13)
            y = rng.randrange(4, TILE - 5)
            draw.line((x, y, x + rng.randrange(7, 15), y + rng.randrange(-2, 3)), fill=(51, 58, 59, 110), width=1)
    elif name == "water":
        for _ in range(18):
            x = rng.randrange(3, TILE - 11)
            y = rng.randrange(4, TILE - 4)
            draw.arc((x, y, x + rng.randrange(7, 15), y + rng.randrange(3, 7)), 190, 340, fill=(159, 216, 219, 110), width=1)
    img = Image.alpha_composite(img, details)
    img.putalpha(255)
    return img


def save_backups(names: list[str]) -> None:
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    for name in names:
        src = TERRAIN_DIR / f"{name}.png"
        dst = BACKUP_DIR / f"{name}.png"
        if src.exists() and not dst.exists():
            shutil.copy2(src, dst)


def bezier(points: list[tuple[float, float]], samples: int = 18) -> list[tuple[int, int]]:
    if len(points) == 2:
        return [(round(points[0][0]), round(points[0][1])), (round(points[1][0]), round(points[1][1]))]
    result: list[tuple[int, int]] = []
    for i in range(samples + 1):
        t = i / samples
        if len(points) == 3:
            x = (1 - t) ** 2 * points[0][0] + 2 * (1 - t) * t * points[1][0] + t**2 * points[2][0]
            y = (1 - t) ** 2 * points[0][1] + 2 * (1 - t) * t * points[1][1] + t**2 * points[2][1]
        else:
            x = points[0][0]
            y = points[0][1]
        result.append((round(x), round(y)))
    return result


def scaled(points: list[tuple[int, int]]) -> list[tuple[int, int]]:
    return [(x * SCALE, y * SCALE) for x, y in points]


def road_overlay(bits: int) -> Image.Image:
    from tools.assets.environments.generate_road_autotiles import road_overlay as autotile_road_overlay

    return autotile_road_overlay(bits)


def road_texture(bits: int) -> Image.Image:
    fallback = make_tile("road", bits % 4).convert("RGBA")
    if not SOURCE_SHEET.exists():
        return fallback
    sheet = Image.open(SOURCE_SHEET).convert("RGBA")
    boxes = [
        (679, 184, 823, 332),
        (842, 184, 987, 332),
        (1007, 185, 1151, 332),
        (1169, 185, 1317, 331),
    ]
    left, top, right, bottom = boxes[bits % len(boxes)]
    inset = 22
    return edge_normalized(sheet.crop((left + inset, top + inset, right - inset, bottom - inset)))


def edge_normalized(crop: Image.Image) -> Image.Image:
    img = crop.convert("RGBA").resize((TILE, TILE), Image.Resampling.LANCZOS)
    px = img.load()
    # Match opposing borders with a soft ramp. This preserves the hand-painted
    # center while making repeated 48px tiles less likely to show seams.
    for y in range(TILE):
        left = px[0, y]
        right = px[TILE - 1, y]
        avg = tuple((left[i] + right[i]) // 2 for i in range(4))
        for x in range(5):
            strength = (5 - x) / 5 * 0.65
            for xx in (x, TILE - 1 - x):
                cur = px[xx, y]
                px[xx, y] = tuple(clamp(int(cur[i] * (1 - strength) + avg[i] * strength)) for i in range(4))
    for x in range(TILE):
        top = px[x, 0]
        bottom = px[x, TILE - 1]
        avg = tuple((top[i] + bottom[i]) // 2 for i in range(4))
        for y in range(5):
            strength = (5 - y) / 5 * 0.65
            for yy in (y, TILE - 1 - y):
                cur = px[x, yy]
                px[x, yy] = tuple(clamp(int(cur[i] * (1 - strength) + avg[i] * strength)) for i in range(4))
    img.putalpha(255)
    return img


def color_stats(img: Image.Image) -> tuple[tuple[float, float, float], tuple[float, float, float]]:
    pixels = list(img.convert("RGB").getdata())
    mean = tuple(sum(pixel[i] for pixel in pixels) / len(pixels) for i in range(3))
    variance = tuple(sum((pixel[i] - mean[i]) ** 2 for pixel in pixels) / len(pixels) for i in range(3))
    std = tuple(max(1.0, math.sqrt(value)) for value in variance)
    return mean, std


def match_palette(
    img: Image.Image,
    target_mean: tuple[float, float, float],
    target_std: tuple[float, float, float],
    strength: float = 0.72,
) -> Image.Image:
    src_mean, src_std = color_stats(img)
    out = img.convert("RGBA")
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            channels = [r, g, b]
            adjusted = []
            for i, value in enumerate(channels):
                normalized = (value - src_mean[i]) / src_std[i]
                target = target_mean[i] + normalized * target_std[i]
                adjusted.append(clamp(int(value * (1 - strength) + target * strength)))
            px[x, y] = (adjusted[0], adjusted[1], adjusted[2], a)
    return out


def is_dark_sheet_background(rgb: tuple[int, int, int]) -> bool:
    r, g, b = rgb
    return r < 22 and g < 48 and 35 <= b <= 90 and b >= g - 6


def component_swatches(source: Path, *, count: int = 8, inset: int = 34) -> list[Image.Image]:
    if not source.exists():
        return []
    sheet = Image.open(source).convert("RGBA")
    rgb = sheet.convert("RGB")
    px = rgb.load()
    width, height = rgb.size
    seen = [[False for _ in range(width)] for _ in range(height)]
    components: list[tuple[int, int, int, int, int]] = []

    for y in range(height):
        for x in range(width):
            if seen[y][x] or is_dark_sheet_background(px[x, y]):
                continue
            stack = [(x, y)]
            seen[y][x] = True
            left = right = x
            top = bottom = y
            area = 0
            while stack:
                cx, cy = stack.pop()
                area += 1
                left = min(left, cx)
                right = max(right, cx)
                top = min(top, cy)
                bottom = max(bottom, cy)
                for nx, ny in ((cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
                    if not (0 <= nx < width and 0 <= ny < height):
                        continue
                    if seen[ny][nx] or is_dark_sheet_background(px[nx, ny]):
                        continue
                    seen[ny][nx] = True
                    stack.append((nx, ny))
            bw = right - left + 1
            bh = bottom - top + 1
            if area >= 10_000 and bw >= 110 and bh >= 110:
                components.append((left, top, right, bottom, area))

    components = sorted(components, key=lambda item: item[4], reverse=True)[:count]
    components.sort(key=lambda item: (item[1] // 80, item[0]))
    swatches: list[Image.Image] = []
    for left, top, right, bottom, _area in components:
        crop_box = (
            min(right - 8, left + inset),
            min(bottom - 8, top + inset),
            max(left + 8, right - inset),
            max(top + 8, bottom - inset),
        )
        if crop_box[2] <= crop_box[0] or crop_box[3] <= crop_box[1]:
            crop_box = (left, top, right + 1, bottom + 1)
        swatches.append(edge_normalized(sheet.crop(crop_box)))
    return swatches


def normalized_swatches(swatches: list[Image.Image], *, contrast: float = 0.82, strength: float = 0.72) -> list[Image.Image]:
    if not swatches:
        return []
    means, stds = zip(*(color_stats(img) for img in swatches))
    target_mean = tuple(sum(mean[i] for mean in means) / len(means) for i in range(3))
    target_std = tuple(sum(std[i] for std in stds) / len(stds) * contrast for i in range(3))
    return [match_palette(img, target_mean, target_std, strength=strength) for img in swatches]


def extract_grass_swatches() -> list[Image.Image]:
    if not SOURCE_SHEET.exists():
        return []
    sheet = Image.open(SOURCE_SHEET).convert("RGBA")
    boxes = [
        (23, 24, 170, 170),
        (188, 23, 333, 170),
        (352, 23, 497, 170),
        (516, 24, 660, 170),
        (679, 23, 823, 169),
        (842, 24, 987, 170),
        (1007, 23, 1151, 170),
        (1172, 25, 1316, 170),
    ]
    swatches: list[Image.Image] = []
    for left, top, right, bottom in boxes:
        inset = 18
        swatches.append(edge_normalized(sheet.crop((left + inset, top + inset, right - inset, bottom - inset))))
    return normalized_swatches(swatches, contrast=0.82, strength=0.72)


def extract_forest_swatches() -> list[Image.Image]:
    if not FOREST_SHEET.exists():
        return []
    sheet = Image.open(FOREST_SHEET).convert("RGBA")
    xs = [(20, 374), (403, 752), (785, 1135), (1164, 1514)]
    ys = [(106, 489), (527, 910)]
    swatches: list[Image.Image] = []
    for top, bottom in ys:
        for left, right in xs:
            inset = 46
            swatches.append(edge_normalized(sheet.crop((left + inset, top + inset, right - inset, bottom - inset))))
    return normalized_swatches(swatches, contrast=0.72, strength=0.66)


def extract_snow_swatches() -> list[Image.Image]:
    if not SNOW_SHEET.exists():
        return []
    sheet = Image.open(SNOW_SHEET).convert("RGBA")
    boxes = [
        (24, 107, 372, 487),
        (406, 107, 752, 487),
        (786, 107, 1130, 487),
        (1168, 107, 1512, 487),
        (406, 533, 752, 914),
        (786, 533, 1130, 914),
        (1168, 533, 1512, 914),
        (78, 150, 318, 430),
    ]
    swatches: list[Image.Image] = []
    for left, top, right, bottom in boxes:
        inset = 46
        swatches.append(edge_normalized(sheet.crop((left + inset, top + inset, right - inset, bottom - inset))))
    return normalized_swatches(swatches, contrast=0.54, strength=0.84)


def main() -> None:
    terrain_names = [
        "grass",
        "forest",
        # Authored desert_sand_wind replaces the repeating pebble variants.
        "tundra",
        "marsh",
        "badlands",
        "mountain",
        "mountain_massif_tile",
        "water",
        "road",
    ]
    save_backups(terrain_names)
    for name in terrain_names:
        make_tile(name, 0).save(TERRAIN_DIR / f"{name}.png")
        for variant in (1, 2, 3):
            make_tile(name, variant).save(TERRAIN_DIR / f"{name}_variant_{variant}.png")

    grass_swatches = extract_grass_swatches()
    if grass_swatches:
        for index, img in enumerate(grass_swatches):
            name = "grass.png" if index == 0 else f"grass_variant_{index}.png"
            img.save(TERRAIN_DIR / name)
    forest_swatches = extract_forest_swatches()
    if forest_swatches:
        for index, img in enumerate(forest_swatches):
            name = "forest.png" if index == 0 else f"forest_variant_{index}.png"
            img.save(TERRAIN_DIR / name)
    snow_swatches = extract_snow_swatches()
    if snow_swatches:
        for index, img in enumerate(snow_swatches):
            name = "tundra.png" if index == 0 else f"tundra_variant_{index}.png"
            img.save(TERRAIN_DIR / name)

    ROAD_DIR.mkdir(parents=True, exist_ok=True)
    for bits in range(16):
        road_overlay(bits).save(ROAD_DIR / f"road_overlay_{bits:02x}.png")


if __name__ == "__main__":
    main()
