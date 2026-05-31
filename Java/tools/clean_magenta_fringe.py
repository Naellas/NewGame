from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "assets"
MAP_ASSET_FOLDERS = {
    "badlands",
    "city",
    "deco",
    "desert",
    "forest",
    "grass",
    "locations",
    "marsh",
    "mountain",
    "road",
    "terrain",
    "tundra",
    "water",
}
SKIP_NAME_PARTS = (
    "_before_",
    "_candidate",
    "_preview",
    "_source",
    "_try",
    "generated_source",
)
SKIP_DIRS = {
    "before_fidelity_refresh",
    "source",
}
Color = tuple[int, int, int, int]


def is_magenta_spill(color: Color) -> bool:
    r, g, b, a = color
    if a == 0:
        return False
    purple_bias = r > g + 18 and b > g + 34 and b >= r - 22 and r >= 42 and b >= 64
    saturated_edge = g <= 64 and (r + b) > (g * 3 + 120)
    return purple_bias and saturated_edge


def near_transparent(px, width: int, height: int, x: int, y: int, radius: int = 2) -> bool:
    for oy in range(-radius, radius + 1):
        for ox in range(-radius, radius + 1):
            if ox == 0 and oy == 0:
                continue
            nx = x + ox
            ny = y + oy
            if nx < 0 or ny < 0 or nx >= width or ny >= height:
                return True
            if px[nx, ny][3] <= 12:
                return True
    return False


def replacement_color(px, width: int, height: int, x: int, y: int, radius: int = 3) -> Color | None:
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
            if color[3] <= 24 or is_magenta_spill(color):
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
    return (
        red // weight_sum,
        green // weight_sum,
        blue // weight_sum,
        alpha // weight_sum,
    )


def clean_image(image: Image.Image) -> tuple[Image.Image, int]:
    out = image.convert("RGBA")
    original = out.copy()
    src = original.load()
    dst = out.load()
    width, height = out.size
    changed = 0

    for y in range(height):
        for x in range(width):
            current = src[x, y]
            if not is_magenta_spill(current) or not near_transparent(src, width, height, x, y):
                continue
            replacement = replacement_color(src, width, height, x, y)
            if replacement is None:
                dst[x, y] = (current[0], current[1], current[2], 0)
            else:
                dst[x, y] = (replacement[0], replacement[1], replacement[2], current[3])
            changed += 1

    return out, changed


def candidate_paths(root: Path) -> list[Path]:
    paths: list[Path] = []
    for folder in sorted(MAP_ASSET_FOLDERS):
        base = root / folder
        if not base.exists():
            continue
        paths.extend(sorted(base.rglob("*.png")))
    return [
        path
        for path in paths
        if not any(part in SKIP_DIRS for part in path.parts)
        and not any(part in path.stem for part in SKIP_NAME_PARTS)
    ]


def clean_assets(root: Path, dry_run: bool) -> int:
    total_changed = 0
    files_changed = 0
    for path in candidate_paths(root):
        image = Image.open(path)
        cleaned, changed = clean_image(image)
        if changed == 0:
            continue
        total_changed += changed
        files_changed += 1
        if not dry_run:
            cleaned.save(path)
        print(f"{changed:5d} px  {path.relative_to(root)}")
    verb = "Would clean" if dry_run else "Cleaned"
    print(f"{verb} {total_changed} fringe pixels in {files_changed} files.")
    return files_changed


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Remove magenta chroma-key spill from map tiles and props.")
    parser.add_argument("--assets", type=Path, default=ASSETS, help="Assets root to scan.")
    parser.add_argument("--dry-run", action="store_true", help="Report affected files without writing them.")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    clean_assets(args.assets, args.dry_run)


if __name__ == "__main__":
    main()
