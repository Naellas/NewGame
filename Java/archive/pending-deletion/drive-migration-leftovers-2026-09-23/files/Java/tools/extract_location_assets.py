from __future__ import annotations

from pathlib import Path

from PIL import Image


SRC = Path("assets/source/imagegen-location-tilesheet.png")
OUT = Path("assets/locations")
GRID_NAMES = (
    ("location_farmland_tilled", "location_farmland_wheat", "location_farmland_scarecrow", "location_farmland_hay_bales"),
    ("location_farmland_fence", "location_camp_tent", "location_camp_fire", "location_camp_palisade"),
    ("location_camp_crates", "location_graveyard_dirt", "location_graveyard_tombstones", "location_crypt_entrance"),
    ("location_graveyard_iron_fence", "location_graveyard_dead_stump", "location_graveyard_skull_marker", "location_graveyard_path"),
)


def keyed_alpha(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    pixels = rgba.load()
    width, height = rgba.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if g > 185 and r < 80 and b < 80:
                pixels[x, y] = (r, g, b, 0)
            elif g > 145 and g > r * 1.8 and g > b * 1.8:
                alpha = max(0, min(255, int((max(r, b) - 35) * 5)))
                pixels[x, y] = (r, g, b, min(a, alpha))
    return rgba


def square_trim(image: Image.Image, padding_ratio: float = 0.08) -> Image.Image:
    bbox = image.getbbox()
    if bbox is None:
        return image
    left, top, right, bottom = bbox
    width = right - left
    height = bottom - top
    padding = max(4, int(max(width, height) * padding_ratio))
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
    cell_h = source.height // 4
    OUT.mkdir(parents=True, exist_ok=True)
    for row, names in enumerate(GRID_NAMES):
        for col, name in enumerate(names):
            left = col * cell_w
            top = row * cell_h
            right = source.width if col == 3 else (col + 1) * cell_w
            bottom = source.height if row == 3 else (row + 1) * cell_h
            cell = source.crop((left, top, right, bottom))
            asset = square_trim(keyed_alpha(cell))
            asset.save(OUT / f"{name}.png")


if __name__ == "__main__":
    extract()
