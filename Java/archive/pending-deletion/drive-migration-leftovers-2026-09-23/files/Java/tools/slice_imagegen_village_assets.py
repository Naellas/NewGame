from __future__ import annotations

from pathlib import Path

from PIL import Image

from universal_cutout import CutoutSettings, universal_cutout

ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "assets" / "source"
CITY_DIR = ROOT / "assets" / "city"


def transparent_light_checker(img: Image.Image) -> Image.Image:
    return universal_cutout(img, CutoutSettings(mode="light", padding=26, global_key=False, drop_edge_strays=True))


def transparent_dark_sheet(img: Image.Image) -> Image.Image:
    return universal_cutout(img, CutoutSettings(mode="dark", padding=26, global_key=False, drop_edge_strays=True))


def transparent_green_key(img: Image.Image) -> Image.Image:
    return universal_cutout(
        img,
        CutoutSettings(
            mode="green",
            padding=10,
            global_key=True,
            stray_max_gap=24,
            drop_edge_strays=True,
            drop_above_strays=True,
            drop_below_strays=True,
            drop_small_green_matte=True,
        ),
    )


def crop_cell(sheet: Image.Image, col: int, row: int, cols: int, rows: int) -> Image.Image:
    x1 = round(col * sheet.width / cols)
    y1 = round(row * sheet.height / rows)
    x2 = round((col + 1) * sheet.width / cols)
    y2 = round((row + 1) * sheet.height / rows)
    return sheet.crop((x1, y1, x2, y2))


def crop_cell_bleed(sheet: Image.Image, col: int, row: int, cols: int, rows: int, bleed: int) -> Image.Image:
    x1 = round(col * sheet.width / cols)
    y1 = round(row * sheet.height / rows)
    x2 = round((col + 1) * sheet.width / cols)
    y2 = round((row + 1) * sheet.height / rows)
    return sheet.crop((max(0, x1 - bleed), max(0, y1 - bleed), min(sheet.width, x2 + bleed), min(sheet.height, y2 + bleed)))


def output_path(name: str) -> Path:
    if name.startswith("village_prop_"):
        return CITY_DIR / "props" / "village" / f"{name}.png"
    if name.startswith("village_building_") or name.startswith("imagegen_city_"):
        return CITY_DIR / "buildings" / "village" / f"{name}.png"
    return CITY_DIR / f"{name}.png"


def save(img: Image.Image, name: str) -> None:
    path = output_path(name)
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def slice_sheet(path: Path, names: list[str], cols: int, rows: int, transparent=transparent_light_checker, bleed: int = 0) -> None:
    sheet = Image.open(path)
    for index, name in enumerate(names):
        if bleed > 0:
            cell = crop_cell_bleed(sheet, index % cols, index // cols, cols, rows, bleed)
        else:
            cell = crop_cell(sheet, index % cols, index // cols, cols, rows)
        save(transparent(cell), name)


def level_building_names(level: int) -> list[str]:
    suffix = f"_lvl{level}"
    return [
        "imagegen_city_cottage" + suffix,
        "village_building_warehouse" + suffix,
        "village_building_forestry_hut" + suffix,
        "village_building_mine" + suffix,
        "village_building_hunting_camp" + suffix,
        "village_building_farmstead" + suffix,
        "village_building_workshop" + suffix,
        "imagegen_city_hall" + suffix,
        "imagegen_city_study" + suffix,
        "imagegen_city_row" + suffix,
        "village_building_granary" + suffix,
        "village_building_watchtower" + suffix,
        "village_building_shrine" + suffix,
        "village_building_garden" + suffix,
        "village_building_blacksmith" + suffix,
        "village_building_bakery" + suffix,
        "village_building_apothecary" + suffix,
        "village_building_fishing_hut" + suffix,
    ]


def slice_city_exteriors() -> None:
    sheet = Image.open(SOURCE_DIR / "imagegen-overworld-city-exteriors-nosnow.png")
    picks = {
        "imagegen_city_cottage": (917, 667, 971, 743),
        "imagegen_city_workshop": (776, 762, 860, 829),
        "imagegen_city_hall": (26, 831, 174, 897),
        "imagegen_city_study": (1435, 844, 1508, 911),
        "imagegen_city_row": (477, 831, 601, 897),
        "imagegen_city_warehouse": (350, 830, 460, 897),
    }
    for name, box in picks.items():
        save(transparent_dark_sheet(sheet.crop(box)), name)


def main() -> None:
    slice_sheet(
        SOURCE_DIR / "imagegen-village-buildings.png",
        [
            "village_building_blacksmith",
            "village_building_bakery",
            "village_building_apothecary",
            "village_building_fishing_hut",
        ],
        4,
        1,
    )
    slice_sheet(
        SOURCE_DIR / "imagegen-village-resource-buildings.png",
        [
            "village_building_warehouse",
            "village_building_mine",
            "village_building_forestry_hut",
            "village_building_hunting_camp",
            "village_building_farmstead",
            "village_building_granary",
            "village_building_watchtower",
            "village_building_shrine",
        ],
        4,
        2,
    )
    slice_sheet(
        SOURCE_DIR / "imagegen-village-props.png",
        [
            "village_prop_anvil_stump",
            "village_prop_sawhorse",
            "village_prop_chopping_block",
            "village_prop_ore_pile",
            "village_prop_mine_lantern",
            "village_prop_chicken_coop",
            "village_prop_fish_rack",
            "village_prop_herb_barrel",
            "village_prop_log_stack",
            "village_prop_grain_sacks",
            "village_prop_water_trough",
            "village_prop_fence_segment",
        ],
        4,
        3,
    )
    slice_city_exteriors()
    slice_sheet(
        SOURCE_DIR / "imagegen-village-decor-buildings-props.png",
        [
            "village_building_workshop",
            "village_building_garden",
            "imagegen_city_row",
            "imagegen_city_cottage",
            "village_prop_well",
            "village_prop_notice_board",
            "village_prop_tool_rack",
            "village_prop_beehive",
            "village_prop_training_dummy",
            "village_prop_drying_rack",
            "village_prop_hay_stack",
            "village_prop_hay_bales",
            "village_prop_produce_basket",
            "village_prop_bench",
            "village_prop_farm_tools",
            "village_prop_woodpile",
        ],
        4,
        4,
    )
    slice_sheet(
        SOURCE_DIR / "imagegen-village-buildings-lvl2.png",
        level_building_names(2),
        6,
        3,
        transparent_green_key,
        bleed=56,
    )
    slice_sheet(
        SOURCE_DIR / "imagegen-village-buildings-lvl3.png",
        level_building_names(3),
        6,
        3,
        transparent_green_key,
        bleed=56,
    )


if __name__ == "__main__":
    main()
