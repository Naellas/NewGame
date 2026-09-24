
import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT
from tools.assets.shared.asset_paths import asset_file
from pathlib import Path

from PIL import Image


ROOT = JAVA_ROOT
SOURCE = ROOT / "assets" / "source"
CITY = ROOT / "assets" / "environments/settlements/city"
CITY_BUILDINGS = CITY / "buildings"
CITY_PROPS = CITY / "props" / "city"


def green_key(image: Image.Image) -> Image.Image:
    image = image.convert("RGBA")
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            if a and g > 120 and r < 120 and b < 120 and g - max(r, b) > 45:
                pixels[x, y] = (r, g, b, 0)
    return image


def trim(image: Image.Image, padding: int = 12) -> Image.Image:
    bbox = image.getbbox()
    if bbox is None:
        return image
    left = max(0, bbox[0] - padding)
    top = max(0, bbox[1] - padding)
    right = min(image.width, bbox[2] + padding)
    bottom = min(image.height, bbox[3] + padding)
    return image.crop((left, top, right, bottom))


def save_cutout(source_name: str, output_name: str) -> None:
    image = Image.open(SOURCE / source_name)
    destination = asset_file(ROOT / "assets", f"environments/settlements/city/buildings/city/{output_name}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    trim(green_key(image), padding=18).save(destination)


def save_alley_props() -> None:
    sheet = Image.open(SOURCE / "imagegen-city-alley-plants.png")
    cols, rows = 4, 2
    cell_w = sheet.width // cols
    cell_h = sheet.height // rows
    names = [
        "city_prop_ivy_wall_planter.png",
        "city_prop_potted_shrub_tall.png",
        "city_prop_flower_crate.png",
        "city_prop_vine_trellis.png",
        "city_prop_moss_barrel_planter.png",
        "city_prop_herb_planter_narrow.png",
        "city_prop_vine_lantern_post.png",
        "city_prop_flower_baskets.png",
    ]
    for index, name in enumerate(names):
        col = index % cols
        row = index // cols
        cell = sheet.crop((col * cell_w, row * cell_h, (col + 1) * cell_w, (row + 1) * cell_h))
        trim(green_key(cell), padding=10).save(CITY_PROPS / name)


def main() -> None:
    CITY.mkdir(parents=True, exist_ok=True)
    CITY_BUILDINGS.mkdir(parents=True, exist_ok=True)
    CITY_PROPS.mkdir(parents=True, exist_ok=True)
    save_cutout("imagegen-city-civic-hall-cutout.png", "city_building_civic_hall_imagegen.png")
    save_cutout("imagegen-city-watchtower-cutout.png", "city_building_watchtower_imagegen.png")
    save_alley_props()


if __name__ == "__main__":
    main()
