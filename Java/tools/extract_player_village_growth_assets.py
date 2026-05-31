from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "assets" / "player_village"
SOURCE_DIR = ASSET_DIR / "source"


def remove_green_key(img: Image.Image) -> Image.Image:
    out = img.convert("RGBA")
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if g > 170 and r < 90 and b < 90:
                px[x, y] = (r, g, b, 0)
    return out


def trim(img: Image.Image, pad: int = 12) -> Image.Image:
    box = img.getchannel("A").getbbox()
    if box is None:
        return img
    left = max(0, box[0] - pad)
    top = max(0, box[1] - pad)
    right = min(img.width, box[2] + pad)
    bottom = min(img.height, box[3] + pad)
    return img.crop((left, top, right, bottom))


def crop_cell(sheet: Image.Image, index: int, count: int) -> Image.Image:
    left = round(index * sheet.width / count)
    right = round((index + 1) * sheet.width / count)
    return sheet.crop((left, 0, right, sheet.height))


def main() -> None:
    source_arg = Path(sys.argv[1]) if len(sys.argv) > 1 else SOURCE_DIR / "player_village_growth_sheet_chroma.png"
    if not source_arg.exists():
        raise FileNotFoundError(source_arg)

    ASSET_DIR.mkdir(parents=True, exist_ok=True)
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)

    sheet = Image.open(source_arg)
    keyed = remove_green_key(sheet)
    keyed.save(SOURCE_DIR / "player_village_growth_sheet.png")

    for index in range(10):
        cell = trim(crop_cell(keyed, index, 10))
        cell.save(ASSET_DIR / f"player_village_stage_{index + 1:02d}.png")


if __name__ == "__main__":
    main()
