from __future__ import annotations

from pathlib import Path

from PIL import Image


SRC = Path("assets/source/imagegen-goblin-monster-sheet.png")
OUT = Path("assets/monsters")
GRID_NAMES = (
    ("goblin_scout", "goblin_archer", "goblin_trapper", "goblin_skirmisher"),
    ("goblin_shaman", "hobgoblin_guard", "goblin_warlord", "goblin_king"),
)


def keyed_alpha(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    pixels = rgba.load()
    width, height = rgba.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            max_rb = max(r, b)
            key_strength = g - int(max_rb * 1.45)
            if g > 185 and max_rb < 118 and key_strength > 42:
                pixels[x, y] = (r, g, b, 0)
            elif g > 145 and max_rb < 140 and key_strength > 18:
                alpha = max(0, min(255, int((max_rb - 28) * 4.4 + (255 - g) * 0.45)))
                pixels[x, y] = (r, g, b, min(a, alpha))
    return despill_edges(rgba)


def despill_edges(image: Image.Image) -> Image.Image:
    source = image.copy()
    src = source.load()
    dst = image.load()
    width, height = image.size

    def near_transparent(px: int, py: int) -> bool:
        for oy in range(-2, 3):
            for ox in range(-2, 3):
                nx, ny = px + ox, py + oy
                if 0 <= nx < width and 0 <= ny < height and src[nx, ny][3] < 96:
                    return True
        return False

    for y in range(height):
        for x in range(width):
            r, g, b, a = src[x, y]
            if a == 0:
                continue
            max_rb = max(r, b)
            green_spill = g > max_rb + 22 and g > 90
            if not green_spill:
                continue
            if a < 230 or near_transparent(x, y):
                cleaned_g = min(g, int(max_rb * 1.08 + 34))
                dst[x, y] = (r, cleaned_g, b, a)
    return image


def square_trim(image: Image.Image, padding_ratio: float = 0.06) -> Image.Image:
    bbox = image.getbbox()
    if bbox is None:
        return image
    left, top, right, bottom = bbox
    width = right - left
    height = bottom - top
    padding = max(6, int(max(width, height) * padding_ratio))
    left = max(0, left - padding)
    top = max(0, top - padding)
    right = min(image.width, right + padding)
    bottom = min(image.height, bottom + padding)
    cropped = image.crop((left, top, right, bottom))
    side = max(cropped.width, cropped.height)
    framed = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    framed.alpha_composite(cropped, ((side - cropped.width) // 2, (side - cropped.height) // 2))
    return framed


def extract() -> None:
    source = Image.open(SRC).convert("RGBA")
    cell_w = source.width // 4
    cell_h = source.height // 2
    OUT.mkdir(parents=True, exist_ok=True)
    for row, names in enumerate(GRID_NAMES):
        for col, name in enumerate(names):
            left = col * cell_w
            top = row * cell_h
            right = source.width if col == 3 else (col + 1) * cell_w
            bottom = source.height if row == 1 else (row + 1) * cell_h
            cell = source.crop((left, top, right, bottom))
            asset = square_trim(keyed_alpha(cell))
            asset.save(OUT / f"{name}.png")


if __name__ == "__main__":
    extract()
