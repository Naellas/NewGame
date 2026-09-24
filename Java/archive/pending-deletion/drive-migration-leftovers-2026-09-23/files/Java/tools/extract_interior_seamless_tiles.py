from pathlib import Path
from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets" / "source" / "imagegen-interior-seamless-wall-floor-sheet.png"
OUT = ROOT / "assets" / "interiors"
KEY = (255, 0, 255)

BOXES = {
    "interior_wall_horizontal_center": (68, 110, 482, 402),
    "interior_wall_horizontal_left": (552, 110, 886, 402),
    "interior_wall_horizontal_right": (960, 110, 1296, 402),
    "interior_wall_vertical_side": (1476, 110, 1642, 404),
    "interior_floor_blend": (72, 478, 432, 788),
    "interior_floor_blend_alt": (504, 478, 866, 788),
    "interior_wall_horizontal_cap": (922, 616, 1286, 698),
    "interior_wall_horizontal_shadow": (1348, 618, 1708, 698),
}


def remove_chroma(image: Image.Image) -> Image.Image:
    image = image.convert("RGBA")
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            if r > 220 and b > 180 and g < 80:
                pixels[x, y] = (0, 0, 0, 0)
    return image


def crop_floor(sheet: Image.Image, box: tuple[int, int, int, int]) -> Image.Image:
    x1, y1, x2, y2 = box
    # Use the inner plank field so the exported tile has no baked outer border.
    crop = sheet.crop((x1 + 28, y1 + 24, x2 - 28, y2 - 24)).convert("RGBA")
    side = min(crop.width, crop.height)
    left = (crop.width - side) // 2
    top = (crop.height - side) // 2
    crop = crop.crop((left, top, left + side, top + side))
    return crop.resize((48, 48), Image.Resampling.LANCZOS)


def crop_wall(sheet: Image.Image, box: tuple[int, int, int, int]) -> Image.Image:
    crop = remove_chroma(sheet.crop(box))
    return crop.resize((48, 48), Image.Resampling.LANCZOS)


def crop_strip(sheet: Image.Image, box: tuple[int, int, int, int]) -> Image.Image:
    crop = remove_chroma(sheet.crop(box))
    return crop.resize((96, 24), Image.Resampling.LANCZOS)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    sheet = Image.open(SOURCE).convert("RGBA")
    for name, box in BOXES.items():
        if name.startswith("interior_floor"):
            image = crop_floor(sheet, box)
        elif name.endswith("_cap") or name.endswith("_shadow"):
            image = crop_strip(sheet, box)
        else:
            image = crop_wall(sheet, box)
        image.save(OUT / f"{name}.png")


if __name__ == "__main__":
    main()
