from __future__ import annotations

import argparse
from collections import deque
from pathlib import Path

from PIL import Image

SRC_CITY_SHEET = Path("assets/source/imagegen-city-tilesheet.png")
OUT = Path("assets/city/split")
DEFAULT_TILE_CANDIDATES = (64, 48, 32, 96, 128)

Rgb = tuple[int, int, int]
Component = list[tuple[int, int]]


def clamp(value: int, low: int, high: int) -> int:
    return max(low, min(high, value))


def is_warm_paper(rgb: Rgb) -> bool:
    r, g, b = rgb
    hi = max(r, g, b)
    lo = min(r, g, b)
    sat = hi - lo
    return r >= g - 8 and g >= b - 14 and r >= b + 4 and sat < 84 and r > 135 and g > 112 and b > 88


def detect_tile_size(width: int, height: int, candidates: tuple[int, ...]) -> int:
    best_score = -10**9
    best_size = candidates[0]
    for size in candidates:
        if size <= 0:
            continue
        score = 0
        if width % size == 0:
            score += 1000
        if height % size == 0:
            score += 1000
        if width % size < max(2, size // 10):
            score += 120
        if height % size < max(2, size // 10):
            score += 120
        if 16 <= size <= 128:
            score += 20
        if score > best_score:
            best_score = score
            best_size = size
    return best_size


def region_pixels(img: Image.Image, box: tuple[int, int, int, int]) -> list[list[Rgb]]:
    left, top, right, bottom = box
    crop = img.crop((left, top, right, bottom)).convert("RGB")
    px = crop.load()
    width, height = crop.size
    return [[px[x, y] for x in range(width)] for y in range(height)]


def border_palette(region: list[list[Rgb]]) -> list[Rgb]:
    height = len(region)
    width = len(region[0])
    ring = max(2, min(width, height) // 10)
    buckets: dict[tuple[int, int, int], list[Rgb]] = {}
    for y in range(height):
        for x in range(width):
            if ring <= x < width - ring and ring <= y < height - ring:
                continue
            rgb = region[y][x]
            if not is_warm_paper(rgb):
                continue
            key = (rgb[0] // 12, rgb[1] // 12, rgb[2] // 12)
            buckets.setdefault(key, []).append(rgb)
    ranked = sorted(buckets.values(), key=len, reverse=True)[:6]
    if not ranked:
        return [(228, 216, 194)]
    palette: list[Rgb] = []
    for group in ranked:
        n = len(group)
        palette.append((sum(p[0] for p in group) // n, sum(p[1] for p in group) // n, sum(p[2] for p in group) // n))
    return palette


def is_background(rgb: Rgb, palette: list[Rgb]) -> bool:
    if not is_warm_paper(rgb):
        return False
    r, g, b = rgb
    for br, bg, bb in palette:
        dr = abs(r - br)
        dg = abs(g - bg)
        db = abs(b - bb)
        if dr + dg + db < 130 and dr < 58 and dg < 58 and db < 58:
            return True
    return False


def foreground_mask(region: list[list[Rgb]], palette: list[Rgb]) -> list[list[bool]]:
    return [[not is_background(px, palette) for px in row] for row in region]


def connected_components(mask: list[list[bool]]) -> list[Component]:
    height = len(mask)
    width = len(mask[0])
    seen = [[False for _ in range(width)] for _ in range(height)]
    components: list[Component] = []
    for y in range(height):
        for x in range(width):
            if seen[y][x] or not mask[y][x]:
                continue
            seen[y][x] = True
            q: deque[tuple[int, int]] = deque([(x, y)])
            comp: Component = []
            while q:
                cx, cy = q.popleft()
                comp.append((cx, cy))
                for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
                    if not (0 <= nx < width and 0 <= ny < height):
                        continue
                    if seen[ny][nx] or not mask[ny][nx]:
                        continue
                    seen[ny][nx] = True
                    q.append((nx, ny))
            components.append(comp)
    return components


def score_component(
    comp: Component,
    core_left: int,
    core_top: int,
    core_right: int,
    core_bottom: int,
    region_width: int,
    region_height: int,
) -> float:
    overlap = 0
    min_x = region_width
    min_y = region_height
    max_x = 0
    max_y = 0
    center_x = 0.0
    center_y = 0.0
    for x, y in comp:
        center_x += x
        center_y += y
        if core_left <= x < core_right and core_top <= y < core_bottom:
            overlap += 1
        min_x = min(min_x, x)
        min_y = min(min_y, y)
        max_x = max(max_x, x)
        max_y = max(max_y, y)
    if overlap == 0:
        return -10**9

    area = len(comp)
    cx = center_x / max(1, area)
    cy = center_y / max(1, area)
    tile_cx = (core_left + core_right) / 2
    tile_cy = (core_top + core_bottom) / 2
    distance = abs(cx - tile_cx) + abs(cy - tile_cy)
    touches_edge = min_x == 0 or min_y == 0 or max_x == region_width - 1 or max_y == region_height - 1
    return overlap * 4.2 + area * 0.2 - distance * 0.7 - (18 if touches_edge else 0)


def tile_cutout(
    img: Image.Image,
    tx: int,
    ty: int,
    tile_size: int,
    context: int,
    min_area: int,
    trim_padding: int,
) -> Image.Image | None:
    width, height = img.size
    left = tx * tile_size
    top = ty * tile_size
    right = min(width, left + tile_size)
    bottom = min(height, top + tile_size)

    ex_left = clamp(left - context, 0, width)
    ex_top = clamp(top - context, 0, height)
    ex_right = clamp(right + context, 0, width)
    ex_bottom = clamp(bottom + context, 0, height)

    region = region_pixels(img, (ex_left, ex_top, ex_right, ex_bottom))
    palette = border_palette(region)
    mask = foreground_mask(region, palette)
    components = connected_components(mask)
    if not components:
        return None

    core_left = left - ex_left
    core_top = top - ex_top
    core_right = right - ex_left
    core_bottom = bottom - ex_top
    region_width = ex_right - ex_left
    region_height = ex_bottom - ex_top

    filtered = [c for c in components if len(c) >= min_area]
    if not filtered:
        filtered = components
    best = max(
        filtered,
        key=lambda c: score_component(c, core_left, core_top, core_right, core_bottom, region_width, region_height),
    )
    if score_component(best, core_left, core_top, core_right, core_bottom, region_width, region_height) < -100:
        return None

    best_set = set(best)
    tile = img.crop((left, top, right, bottom)).convert("RGBA")
    tile_px = tile.load()
    tile_w, tile_h = tile.size
    for y in range(tile_h):
        for x in range(tile_w):
            rx = x + core_left
            ry = y + core_top
            if (rx, ry) not in best_set:
                r, g, b, _ = tile_px[x, y]
                tile_px[x, y] = (r, g, b, 0)

    bbox = tile.getbbox()
    if bbox is None:
        return None
    l, t, r, b = bbox
    l = max(0, l - trim_padding)
    t = max(0, t - trim_padding)
    r = min(tile_w, r + trim_padding)
    b = min(tile_h, b + trim_padding)
    return tile.crop((l, t, r, b))


def extract_city_tiles(
    source: Path,
    out_dir: Path,
    tile_size: int | None,
    context: int,
    min_area: int,
    trim_padding: int,
    keep_empty: bool,
) -> None:
    if not source.exists():
        raise FileNotFoundError(f"Missing source sheet: {source}")

    img = Image.open(source).convert("RGBA")
    width, height = img.size
    chosen_tile = tile_size or detect_tile_size(width, height, DEFAULT_TILE_CANDIDATES)
    cols = width // chosen_tile
    rows = height // chosen_tile
    if cols <= 0 or rows <= 0:
        raise ValueError(f"Invalid tile size {chosen_tile} for {width}x{height} sheet.")

    out_dir.mkdir(parents=True, exist_ok=True)
    written = 0
    empty = 0
    print(f"City sheet: {width}x{height}")
    print(f"Using tile size: {chosen_tile} ({cols}x{rows} grid)")
    print(f"Context: {context}px, min area: {min_area}, trim padding: {trim_padding}")

    for ty in range(rows):
        for tx in range(cols):
            tile = tile_cutout(img, tx, ty, chosen_tile, context, min_area, trim_padding)
            name = f"city_tile_r{ty:02d}_c{tx:02d}.png"
            out_path = out_dir / name
            if tile is None:
                empty += 1
                if keep_empty:
                    Image.new("RGBA", (chosen_tile, chosen_tile), (0, 0, 0, 0)).save(out_path)
                    written += 1
                continue
            tile.save(out_path)
            written += 1

    print(f"Done. Saved {written} tiles to {out_dir}")
    if empty:
        print(f"Skipped empty tiles: {empty}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Context-aware tilesheet splitter for city assets.")
    parser.add_argument("--source", type=Path, default=SRC_CITY_SHEET, help="Source tilesheet path.")
    parser.add_argument("--out", type=Path, default=OUT, help="Output directory.")
    parser.add_argument("--tile-size", type=int, default=0, help="Tile size. Use 0 to auto-detect.")
    parser.add_argument("--context", type=int, default=10, help="Neighbor context margin in pixels.")
    parser.add_argument("--min-area", type=int, default=28, help="Minimum connected-component area in pixels.")
    parser.add_argument("--trim-padding", type=int, default=1, help="Padding around final trimmed cutout.")
    parser.add_argument("--keep-empty", action="store_true", help="Also write transparent images for empty tiles.")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    extract_city_tiles(
        source=args.source,
        out_dir=args.out,
        tile_size=args.tile_size if args.tile_size > 0 else None,
        context=max(0, args.context),
        min_area=max(1, args.min_area),
        trim_padding=max(0, args.trim_padding),
        keep_empty=args.keep_empty,
    )


if __name__ == "__main__":
    main()
