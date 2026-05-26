from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image

SRC = Path("assets/source/imagegen-npc-sheet.png")
OUT = Path("assets/npcs")

# 3 fixed cells across the generated sheet, each containing one NPC.
BOXES = {
    "npc_marla": (0, 0, 512, 1024),
    "npc_ren": (512, 0, 1024, 1024),
    "npc_torin": (1024, 0, 1536, 1024),
}

Pixel = tuple[int, int, int, int]


def is_chroma(rgb: tuple[int, int, int]) -> bool:
    r, g, b = rgb
    if r > 145 and b > 135 and g < 132 and abs(r - b) < 135:
        return True
    if r > 110 and b > 95 and g < 80 and r >= g + 45 and b >= g + 35:
        return True
    return False


def cut_connected_chroma(crop: Image.Image) -> Image.Image:
    img = crop.convert("RGBA")
    px = img.load()
    width, height = img.size
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
        r, g, b, _ = px[x, y]
        if not is_chroma((r, g, b)):
            continue
        px[x, y] = (r, g, b, 0)
        q.append((x + 1, y))
        q.append((x - 1, y))
        q.append((x, y + 1))
        q.append((x, y - 1))
    return img


def trim_alpha(img: Image.Image, padding: int = 8) -> Image.Image:
    alpha = img.getchannel("A")
    bbox = alpha.getbbox()
    if bbox is None:
        return img
    left, top, right, bottom = bbox
    left = max(0, left - padding)
    top = max(0, top - padding)
    right = min(img.width, right + padding)
    bottom = min(img.height, bottom + padding)
    return img.crop((left, top, right, bottom))


def extract_box(source: Image.Image, box: tuple[int, int, int, int]) -> Image.Image:
    return trim_alpha(cut_connected_chroma(source.crop(box)))


def main() -> None:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing generated NPC sheet: {SRC}")
    OUT.mkdir(parents=True, exist_ok=True)
    source = Image.open(SRC).convert("RGBA")
    for name, box in BOXES.items():
        extract_box(source, box).save(OUT / f"{name}_model.png")


if __name__ == "__main__":
    main()
