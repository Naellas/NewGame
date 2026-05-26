from __future__ import annotations

import tkinter as tk
from collections import deque
from pathlib import Path

from generate_assets import png

SRC_MAIN = Path("assets/source/rpg_asset_sheet_concept.png")
SRC_EXTRA = Path("assets/source/monster_sheet_extra.png")
OUT = Path("assets")

# Bounding boxes from manual inspection of generated concept sheets.
# Format: (left, top, right, bottom)
BOXES_MAIN = {
    "grass": (576, 25, 714, 162),
    "forest": (736, 16, 877, 163),
    "mountain": (900, 14, 1040, 164),
    "road": (1064, 31, 1203, 164),
    "water": (576, 182, 713, 317),
    "city": (356, 488, 590, 723),
    "dungeon": (927, 496, 1241, 735),
    "player": (24, 747, 160, 916),
    "player_battle": (24, 747, 160, 916),
    "npc_marla": (423, 762, 554, 905),
    "npc_ren": (595, 766, 723, 906),
    "npc_torin": (768, 764, 905, 907),
    "class_knight": (237, 758, 377, 902),
    "class_mage": (942, 761, 1080, 907),
    "class_ranger": (768, 764, 905, 907),
    "slime": (29, 968, 161, 1069),
    "wolf": (233, 942, 415, 1072),
    "skeleton": (468, 935, 609, 1081),
    "goblin": (689, 941, 839, 1083),
    "bat": (888, 957, 1133, 1059),
    "icon_potion_red": (49, 1105, 130, 1219),
    "icon_potion_blue": (225, 1105, 307, 1219),
    "icon_potion_green": (396, 1104, 481, 1221),
    "icon_sword": (573, 1099, 705, 1229),
    "icon_shield": (774, 1100, 878, 1229),
    "icon_chest": (950, 1112, 1085, 1231),
    "deco_forest_cluster": (576, 336, 714, 478),
}

BOXES_EXTRA = {
    "spider": (26, 214, 614, 800),
    "wraith": (627, 156, 1114, 801),
    "orc": (1110, 159, 1684, 817),
}

CUTOUT_ASSETS = {
    "player",
    "player_battle",
    "npc_marla",
    "npc_ren",
    "npc_torin",
    "class_knight",
    "class_mage",
    "class_ranger",
    "slime",
    "wolf",
    "skeleton",
    "goblin",
    "bat",
    "spider",
    "wraith",
    "orc",
    "deco_forest_cluster",
}
SHEET_ONLY_ASSETS = CUTOUT_ASSETS | {
    "icon_potion_red",
    "icon_potion_blue",
    "icon_potion_green",
    "icon_sword",
    "icon_shield",
    "icon_chest",
}
Pixel = tuple[int, int, int, int]
CUTOUT_MARGIN = 24
EXTRA_CUTOUT_MARGIN = 36
BackgroundSample = tuple[int, int, int]


def output_path(name: str) -> Path:
    if name.startswith("class_") or name.startswith("player"):
        folder = "player"
    elif name.startswith("icon_"):
        folder = "items"
    elif name.startswith("npc_"):
        folder = "npcs"
    elif name.startswith("deco_"):
        folder = "deco"
    elif name in {"grass", "forest", "mountain", "road", "water", "city", "dungeon"}:
        folder = "terrain"
    else:
        folder = "monsters"
    path = OUT / folder / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    return path


def image_pixels(source: tk.PhotoImage, box: tuple[int, int, int, int]) -> list[list[Pixel]]:
    left, top, right, bottom = box
    pixels: list[list[Pixel]] = []
    for y in range(top, bottom):
        row: list[Pixel] = []
        for x in range(left, right):
            r, g, b = source.get(x, y)
            row.append((r, g, b, 255))
        pixels.append(row)
    return pixels


def padded_box(source: tk.PhotoImage, box: tuple[int, int, int, int], padding: int) -> tuple[int, int, int, int]:
    left, top, right, bottom = box
    return (
        max(0, left - padding),
        max(0, top - padding),
        min(source.width(), right + padding),
        min(source.height(), bottom + padding),
    )


def is_paper_candidate(rgb: tuple[int, int, int]) -> bool:
    r, g, b = rgb
    hi = max(r, g, b)
    lo = min(r, g, b)
    sat = hi - lo
    warm_paper = r >= g - 8 and g >= b - 14 and r >= b + 4
    return warm_paper and sat < 82 and r > 145 and g > 125 and b > 96


def sample_background_palette(pixels: list[list[Pixel]]) -> list[BackgroundSample]:
    height = len(pixels)
    width = len(pixels[0])
    border = max(4, min(width, height) // 14)
    buckets: dict[tuple[int, int, int], list[tuple[int, int, int]]] = {}

    for y, row in enumerate(pixels):
        for x, px in enumerate(row):
            if not (x < border or y < border or x >= width - border or y >= height - border):
                continue
            r, g, b, _ = px
            if not is_paper_candidate((r, g, b)):
                continue
            key = (r // 10, g // 10, b // 10)
            buckets.setdefault(key, []).append((r, g, b))

    ranked = sorted(buckets.values(), key=len, reverse=True)
    samples: list[BackgroundSample] = []
    for bucket in ranked[:6]:
        count = len(bucket)
        samples.append(
            (
                sum(px[0] for px in bucket) // count,
                sum(px[1] for px in bucket) // count,
                sum(px[2] for px in bucket) // count,
            )
        )
    return samples or [(229, 217, 194)]


def is_background_pixel(rgb: tuple[int, int, int], samples: list[BackgroundSample]) -> bool:
    if not is_paper_candidate(rgb):
        return False
    r, g, b = rgb
    for sample in samples:
        dist = abs(r - sample[0]) + abs(g - sample[1]) + abs(b - sample[2])
        channel_close = abs(r - sample[0]) < 54 and abs(g - sample[1]) < 54 and abs(b - sample[2]) < 54
        if dist < 118 and channel_close:
            return True
    return False


def remove_connected_background(pixels: list[list[Pixel]], samples: list[BackgroundSample]) -> None:
    height = len(pixels)
    width = len(pixels[0])
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
        if not (0 <= x < width and 0 <= y < height):
            continue
        if seen[y][x]:
            continue
        seen[y][x] = True
        r, g, b, _ = pixels[y][x]
        if not is_background_pixel((r, g, b), samples):
            continue
        pixels[y][x] = (r, g, b, 0)
        q.append((x + 1, y))
        q.append((x - 1, y))
        q.append((x, y + 1))
        q.append((x, y - 1))


def trim_alpha(pixels: list[list[Pixel]], padding: int = 2) -> list[list[Pixel]]:
    height = len(pixels)
    width = len(pixels[0])
    xs: list[int] = []
    ys: list[int] = []
    for y, row in enumerate(pixels):
        for x, px in enumerate(row):
            if px[3]:
                xs.append(x)
                ys.append(y)
    if not xs:
        return pixels
    left = max(0, min(xs) - padding)
    right = min(width, max(xs) + padding + 1)
    top = max(0, min(ys) - padding)
    bottom = min(height, max(ys) + padding + 1)
    return [row[left:right] for row in pixels[top:bottom]]


def is_floor_shadow_pixel(px: Pixel) -> bool:
    r, g, b, a = px
    if not a:
        return False
    hi = max(r, g, b)
    lo = min(r, g, b)
    sat = hi - lo
    warm_or_neutral = r + 10 >= b and g + 14 >= b
    return 92 <= hi <= 215 and lo >= 64 and sat < 44 and warm_or_neutral


def clear_floor_shadow(pixels: list[list[Pixel]]) -> None:
    height = len(pixels)
    width = len(pixels[0])
    start_y = int(height * 0.58)
    for _ in range(10):
        to_clear: list[tuple[int, int]] = []
        for y in range(start_y, height):
            for x in range(width):
                if not is_floor_shadow_pixel(pixels[y][x]):
                    continue
                touches_cutout_edge = False
                for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                    if 0 <= nx < width and 0 <= ny < height and pixels[ny][nx][3] == 0:
                        touches_cutout_edge = True
                        break
                if touches_cutout_edge:
                    to_clear.append((x, y))
        if not to_clear:
            return
        for x, y in to_clear:
            r, g, b, _ = pixels[y][x]
            pixels[y][x] = (r, g, b, 0)


def clear_stray_components(pixels: list[list[Pixel]], min_pixels: int = 18) -> None:
    height = len(pixels)
    width = len(pixels[0])
    seen = [[False for _ in range(width)] for _ in range(height)]
    components: list[list[tuple[int, int]]] = []
    for y in range(height):
        for x in range(width):
            if seen[y][x] or not pixels[y][x][3]:
                continue
            component: list[tuple[int, int]] = []
            q: deque[tuple[int, int]] = deque([(x, y)])
            seen[y][x] = True
            while q:
                cx, cy = q.popleft()
                component.append((cx, cy))
                for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
                    if not (0 <= nx < width and 0 <= ny < height):
                        continue
                    if seen[ny][nx] or not pixels[ny][nx][3]:
                        continue
                    seen[ny][nx] = True
                    q.append((nx, ny))
            components.append(component)

    if not components:
        return
    main = max(components, key=len)
    main_top = min(y for _, y in main)
    main_bottom = max(y for _, y in main)
    for component in components:
        if component is main:
            continue
        top = min(y for _, y in component)
        bottom = max(y for _, y in component)
        overlaps_main_height = bottom >= main_top - 4 and top <= main_bottom + 4
        if len(component) >= min_pixels and overlaps_main_height:
                continue
        for cx, cy in component:
            r, g, b, _ = pixels[cy][cx]
            pixels[cy][cx] = (r, g, b, 0)


def extract(source: Path, boxes: dict[str, tuple[int, int, int, int]]) -> None:
    src = tk.PhotoImage(file=source.as_posix())
    for name, box in boxes.items():
        if name not in SHEET_ONLY_ASSETS:
            continue
        if name in CUTOUT_ASSETS:
            margin = EXTRA_CUTOUT_MARGIN if name in BOXES_EXTRA else CUTOUT_MARGIN
            box = padded_box(src, box, margin)
        pixels = image_pixels(src, box)
        if name in CUTOUT_ASSETS:
            samples = sample_background_palette(pixels)
            remove_connected_background(pixels, samples)
            pixels = trim_alpha(pixels)
            clear_floor_shadow(pixels)
            clear_stray_components(pixels)
            pixels = trim_alpha(pixels)
        png(output_path(name), pixels)


def main() -> None:
    if not SRC_MAIN.exists():
        raise FileNotFoundError(f"Missing source sheet: {SRC_MAIN}")
    OUT.mkdir(exist_ok=True)
    root = tk.Tk()
    root.withdraw()
    try:
        extract(SRC_MAIN, BOXES_MAIN)
        if SRC_EXTRA.exists():
            extract(SRC_EXTRA, BOXES_EXTRA)
    finally:
        root.destroy()


if __name__ == "__main__":
    main()
