from pathlib import Path

from PIL import Image

from universal_cutout import CutoutSettings, universal_cutout


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets" / "source" / "imagegen-resource-props-sheet.png"

PROPS = [
    ("forest", "deco_tree_oak_harvestable", "items", "material_oak_wood"),
    ("forest", "deco_tree_birch_harvestable", "items", "material_birch_wood"),
    ("forest", "deco_tree_pine_harvestable", "items", "material_pine_wood"),
    ("forest", "deco_tree_willow_harvestable", "items", "material_willow_wood"),
    ("forest", "deco_tree_maple_harvestable", "items", "material_maple_wood"),
    ("forest", "deco_tree_ash_harvestable", "items", "material_ash_wood"),
    ("forest", "deco_tree_elder_harvestable", "items", "material_elder_wood"),
    ("forest", "deco_tree_magical_harvestable", "items", "material_magic_wood"),
    ("forest", "deco_tree_deadwood_harvestable", "items", "material_deadwood"),
    ("forest", "deco_tree_fruit_harvestable", "items", "material_fruitwood"),
    ("mountain", "deco_ore_iron_vein", "items", None),
    ("mountain", "deco_ore_copper_vein", "items", "material_copper_ore"),
    ("mountain", "deco_ore_coal_deposit", "items", None),
    ("mountain", "deco_ore_mithril_vein", "items", "material_mithril_ore"),
    ("mountain", "deco_ore_cobalt_vein", "items", "material_cobalt_ore"),
    ("mountain", "deco_ore_adamantite_vein", "items", "material_adamantite_ore"),
    ("mountain", "deco_ore_tin_vein", "items", "material_tin_ore"),
    ("mountain", "deco_ore_silver_vein", "items", "material_silver_ore"),
    ("mountain", "deco_ore_gold_vein", "items", "material_gold_ore"),
    ("mountain", "deco_ore_crystal_vein", "items", None),
    ("mountain", "deco_ore_steel_scrap", "items", "material_steel_scrap"),
    ("forest", "deco_wood_ironwood_log_pile", "items", "material_ironwood"),
    ("forest", "deco_tree_enchanted_stump", "items", "material_enchanted_bark"),
    ("forest", "deco_wood_fallen_ash_log", "items", "material_ash_wood"),
    ("forest", "deco_tree_glowing_root_cluster", "items", "material_glowroot"),
]


def chroma_to_alpha(img: Image.Image) -> Image.Image:
    return universal_cutout(img, CutoutSettings(mode="magenta", padding=8, trim=False))


def cropped_cell(sheet: Image.Image, index: int) -> Image.Image:
    cols = rows = 5
    col = index % cols
    row = index // cols
    left = round(sheet.width * col / cols)
    top = round(sheet.height * row / rows)
    right = round(sheet.width * (col + 1) / cols)
    bottom = round(sheet.height * (row + 1) / rows)
    return universal_cutout(sheet.crop((left, top, right, bottom)), CutoutSettings(mode="magenta", padding=8))


def save_icon(image: Image.Image, path: Path) -> None:
    icon = image.copy()
    icon.thumbnail((96, 96), Image.Resampling.LANCZOS)
    out = Image.new("RGBA", (96, 96), (0, 0, 0, 0))
    out.alpha_composite(icon, ((96 - icon.width) // 2, (96 - icon.height) // 2))
    out.save(path)


def main() -> None:
    sheet = Image.open(SOURCE)
    for index, (prop_folder, prop_name, icon_folder, icon_name) in enumerate(PROPS):
        image = cropped_cell(sheet, index)
        prop_path = ROOT / "assets" / prop_folder / f"{prop_name}.png"
        prop_path.parent.mkdir(parents=True, exist_ok=True)
        image.save(prop_path)
        if icon_name is not None:
            icon_path = ROOT / "assets" / icon_folder / f"{icon_name}.png"
            icon_path.parent.mkdir(parents=True, exist_ok=True)
            if not icon_path.exists():
                save_icon(image, icon_path)


if __name__ == "__main__":
    main()
