
import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT
from pathlib import Path
from PIL import Image
import argparse
import hashlib
import json
from collections import deque
from tools.assets.shared.asset_paths import asset_file


ROOT = JAVA_ROOT
SOURCE = ROOT / "assets" / "source" / "imagegen-interior-seamless-wall-floor-sheet.png"
OUT = ROOT / "assets" / "environments/interiors"
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


def import_recipe(recipe_path: Path, output_root: Path) -> list[Path]:
    """Explicit architecture crops; source hashes prevent using a different atlas."""
    recipe = json.loads(recipe_path.read_text(encoding="utf-8"))
    sheets = {}
    for name, source in recipe["sources"].items():
        path = recipe_path.parent / source["file"]
        if hashlib.sha256(path.read_bytes()).hexdigest() != source["sha256"]:
            raise ValueError(f"Source checksum mismatch: {path}")
        sheets[name] = Image.open(path).convert("RGBA")
    outputs = []
    for entry in recipe["crops"]:
        sheet = sheets[entry["source"]]
        x1, y1, x2, y2 = entry["box"]
        if not (0 <= x1 < x2 <= sheet.width and 0 <= y1 < y2 <= sheet.height):
            raise ValueError(f"Invalid crop: {entry['asset_file']}")
        crop = sheet.crop((x1, y1, x2, y2))
        if entry.get("clear_navy_background"):
            # Only the border-connected atlas background is removed; dark window
            # panes, curtain folds and frame outlines enclosed by the art survive.
            pixels = crop.load()
            pending = deque((x, y) for y in range(crop.height) for x in range(crop.width)
                            if x in (0, crop.width - 1) or y in (0, crop.height - 1))
            seen = set()
            while pending:
                x, y = pending.popleft()
                if (x, y) in seen or not (0 <= x < crop.width and 0 <= y < crop.height):
                    continue
                seen.add((x, y))
                r, g, b, a = pixels[x, y]
                if r < 35 and g < 40 and b < 50 and b >= r and b >= g:
                    pixels[x, y] = (0, 0, 0, 0)
                    pending.extend(((x-1, y), (x+1, y), (x, y-1), (x, y+1)))
        if entry.get("rotate") == 90:
            crop = crop.transpose(Image.Transpose.ROTATE_90)
        size = tuple(entry["size"])
        padding = entry.get("transparent_padding", 0)
        if padding:
            if padding < 0 or min(size) <= padding * 2:
                raise ValueError(f"Invalid padding: {entry['asset_file']}")
            ink = crop.resize((size[0] - padding * 2, size[1] - padding * 2), Image.Resampling.LANCZOS)
            crop = Image.new("RGBA", size)
            crop.paste(ink, (padding, padding))
        else:
            crop = crop.resize(size, Image.Resampling.LANCZOS)
        path = asset_file(output_root, entry["asset_file"])
        path.parent.mkdir(parents=True, exist_ok=True)
        crop.save(path)
        outputs.append(path)
    return outputs


def main() -> None:
    parser = argparse.ArgumentParser(description="Extract interior architecture using legacy boxes or a hashed crop recipe.")
    parser.add_argument("--recipe", type=Path)
    parser.add_argument("--output-root", type=Path, default=ROOT / "assets")
    args = parser.parse_args()
    if args.recipe:
        outputs = import_recipe(args.recipe.resolve(), args.output_root.resolve())
        print(f"Imported {len(outputs)} architecture pieces")
        return
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
