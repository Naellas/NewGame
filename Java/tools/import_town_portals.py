from __future__ import annotations

import shutil
import sys
from collections import deque
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "assets" / "source"
OUT_DIR = ROOT / "assets" / "locations"
VARIANTS = [
    ("town_portal_green", "green"),
    ("town_portal_snow", "snow"),
    ("town_portal_desert", "desert"),
    ("town_portal_marsh", "marsh"),
    ("town_portal_city", "city"),
]


def is_key(pixel: tuple[int, int, int, int]) -> bool:
    r, g, b, _ = pixel
    return r > 210 and b > 185 and g < 80


def keyed_alpha(image: Image.Image) -> Image.Image:
    image = image.convert("RGBA")
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            if is_key((r, g, b, a)):
                pixels[x, y] = (0, 0, 0, 0)
    return image


def component_boxes(image: Image.Image) -> list[tuple[int, int, int, int]]:
    alpha = image.getchannel("A")
    px = alpha.load()
    seen: set[tuple[int, int]] = set()
    boxes: list[tuple[int, int, int, int]] = []
    for y in range(image.height):
        for x in range(image.width):
            if px[x, y] == 0 or (x, y) in seen:
                continue
            queue = deque([(x, y)])
            seen.add((x, y))
            left = right = x
            top = bottom = y
            count = 0
            while queue:
                cx, cy = queue.popleft()
                count += 1
                left = min(left, cx)
                right = max(right, cx)
                top = min(top, cy)
                bottom = max(bottom, cy)
                for nx, ny in ((cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
                    if nx < 0 or ny < 0 or nx >= image.width or ny >= image.height:
                        continue
                    if px[nx, ny] == 0 or (nx, ny) in seen:
                        continue
                    seen.add((nx, ny))
                    queue.append((nx, ny))
            if count > 800:
                boxes.append((left, top, right + 1, bottom + 1))
    return sorted(boxes, key=lambda box: box[0])


def pad_crop(image: Image.Image, box: tuple[int, int, int, int], pad: int = 18) -> Image.Image:
    left, top, right, bottom = box
    crop = image.crop((max(0, left - pad), max(0, top - pad), min(image.width, right + pad), min(image.height, bottom + pad)))
    square = max(crop.width, crop.height)
    out = Image.new("RGBA", (square, square), (0, 0, 0, 0))
    out.alpha_composite(crop, ((square - crop.width) // 2, square - crop.height))
    return out


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python import_town_portals.py <imagegen-sheet.png>")
    source = Path(sys.argv[1]).resolve()
    if not source.exists():
        raise SystemExit(f"Missing source image: {source}")

    SOURCE_DIR.mkdir(parents=True, exist_ok=True)
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, SOURCE_DIR / "imagegen-town-portal-variants.png")

    sheet = keyed_alpha(Image.open(source))
    boxes = component_boxes(sheet)
    if len(boxes) < len(VARIANTS):
        raise SystemExit(f"Expected at least {len(VARIANTS)} portal components, found {len(boxes)}")

    for (asset, _variant), box in zip(VARIANTS, boxes):
        pad_crop(sheet, box).save(OUT_DIR / f"{asset}.png")


if __name__ == "__main__":
    main()
