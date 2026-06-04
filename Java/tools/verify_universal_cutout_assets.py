from __future__ import annotations

import json
import sys
from dataclasses import asdict, dataclass
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
if str(TOOLS) not in sys.path:
    sys.path.insert(0, str(TOOLS))

import extract_beach_assets as beach
import extract_directional_character_assets as directional
import extract_goblin_monster_assets as goblins
import extract_item_icon_sheets as item_icons
import extract_mage_directional_assets as mage
import extract_monster_expansion_assets as monsters
import extract_new_npc_class_assets as npcs
import extract_npc_variation_assets as npc_variations
import generate_monster_assets as base_monsters
import generate_npc_assets as base_npcs
import generate_player_model_assets as player_models
import process_walk15_animations as walk15
import slice_imagegen_village_assets as village
import slice_resource_props as resources
from asset_paths import animation_dir, character_dir, player_dir
from universal_cutout import touches_transparency


OUT_DIR = ROOT / "assets" / "cutout_tests" / "end_results" / "asset_replacement_proof"
REPORT = OUT_DIR / "asset_replacement_report.json"
CONTACT_SHEET = OUT_DIR / "asset_replacement_contact_sheet.png"


@dataclass(frozen=True)
class AssetSpec:
    path: Path
    mode: str
    allow_edge: bool = False


@dataclass(frozen=True)
class AssetCheck:
    path: str
    mode: str
    size: tuple[int, int]
    visible_pixels: int
    visible_bbox: tuple[int, int, int, int] | None
    edge_visible_pixels: int
    edge_allowed: bool
    visible_background_pixels: int
    transparent_colored_pixels: int
    passed: bool


def village_names() -> list[str]:
    names = [
        "village_building_blacksmith",
        "village_building_bakery",
        "village_building_apothecary",
        "village_building_fishing_hut",
        "village_building_warehouse",
        "village_building_mine",
        "village_building_forestry_hut",
        "village_building_hunting_camp",
        "village_building_farmstead",
        "village_building_granary",
        "village_building_watchtower",
        "village_building_shrine",
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
        "imagegen_city_cottage",
        "imagegen_city_workshop",
        "imagegen_city_hall",
        "imagegen_city_study",
        "imagegen_city_row",
        "imagegen_city_warehouse",
        "village_building_workshop",
        "village_building_garden",
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
    ]
    names.extend(village.level_building_names(2))
    names.extend(village.level_building_names(3))
    return sorted(set(names))


def expected_assets() -> list[AssetSpec]:
    assets: list[AssetSpec] = []
    for name in village_names():
        mode = "green" if name.endswith("_lvl2") or name.endswith("_lvl3") else "auto"
        assets.append(AssetSpec(village.output_path(name), mode))

    for prop_folder, prop_name, icon_folder, icon_name in resources.PROPS:
        assets.append(AssetSpec(ROOT / "assets" / prop_folder / f"{prop_name}.png", "magenta"))
        if icon_name is not None:
            assets.append(AssetSpec(ROOT / "assets" / icon_folder / f"{icon_name}.png", "magenta"))

    for name in beach.TERRAIN_NAMES:
        assets.append(AssetSpec(ROOT / "assets" / "terrain" / f"{name}.png", "auto", allow_edge=True))
    for name in beach.PROP_NAMES:
        assets.append(AssetSpec(ROOT / "assets" / "beach" / f"{name}.png", "magenta"))
    for name in beach.ITEM_ICON_SOURCES:
        assets.append(AssetSpec(ROOT / "assets" / "items" / f"{name}.png", "magenta"))

    for names in item_icons.GROUPS.values():
        for name in names:
            assets.append(AssetSpec(ROOT / "assets" / "items" / f"{name}.png", "magenta"))

    for row in goblins.GRID_NAMES:
        for name in row:
            assets.append(AssetSpec(ROOT / "assets" / "monsters" / f"{name}.png", "green"))
    for row in monsters.GRID_NAMES:
        for name in row:
            assets.append(AssetSpec(ROOT / "assets" / "monsters" / f"{name}.png", "green"))
    for row in base_monsters.GRID_NAMES:
        for name in row:
            assets.append(AssetSpec(ROOT / "assets" / "monsters" / f"{name}.png", "magenta"))

    for name in npcs.NAMES:
        assets.append(AssetSpec(ROOT / "assets" / "npcs" / f"{name}_model.png", "magenta"))
        assets.append(AssetSpec(ROOT / "assets" / "npcs" / f"{name}.png", "magenta"))
    for name in npc_variations.NAMES:
        assets.append(AssetSpec(ROOT / "assets" / "npcs" / f"{name}_model.png", "magenta"))
        assets.append(AssetSpec(ROOT / "assets" / "npcs" / f"{name}.png", "magenta"))
    for name in base_npcs.BOXES:
        assets.append(AssetSpec(ROOT / "assets" / "npcs" / f"{name}_model.png", "magenta"))

    player_output_names = set(player_models.NAMES) | {"class_rogue_model"}
    player_output_names.update({"player", "player_battle", "class_knight", "class_mage", "class_ranger", "class_cleric", "class_rogue"})
    for name in player_output_names:
        assets.append(AssetSpec(player_dir(ROOT / "assets", name) / f"{name}.png", "magenta"))

    for direction in mage.DIRECTIONS:
        assets.append(AssetSpec(player_dir(ROOT / "assets", "class_mage_model") / f"class_mage_model_{direction}.png", "magenta"))
    assets.append(AssetSpec(player_dir(ROOT / "assets", "class_mage_model") / "class_mage_model.png", "magenta"))
    assets.append(AssetSpec(player_dir(ROOT / "assets", "class_mage") / "class_mage.png", "magenta"))

    for _, out_dir, names in directional.SHEETS:
        for name in names:
            for direction in directional.DIRECTIONS:
                target_dir = character_dir(ROOT / "assets", name, ROOT / out_dir)
                assets.append(AssetSpec(target_dir / f"{name}_{direction}.png", "magenta"))

    for _, _, direction, names in walk15.SHEETS:
        for name in names:
            stem = f"{name}_{direction}_walk_anim"
            assets.append(AssetSpec(animation_dir(ROOT / "assets", stem) / f"{stem}.png", "magenta", allow_edge=True))

    by_path: dict[Path, AssetSpec] = {}
    for spec in assets:
        previous = by_path.get(spec.path)
        if previous is None:
            by_path[spec.path] = spec
            continue
        by_path[spec.path] = AssetSpec(
            spec.path,
            previous.mode if previous.mode != "auto" else spec.mode,
            previous.allow_edge and spec.allow_edge,
        )
    return sorted(by_path.values(), key=lambda item: item.path.as_posix())


def edge_visible_pixels(image: Image.Image) -> int:
    px = image.load()
    width, height = image.size
    count = 0
    for x in range(width):
        if px[x, 0][3] > 8:
            count += 1
        if height > 1 and px[x, height - 1][3] > 8:
            count += 1
    for y in range(1, max(1, height - 1)):
        if px[0, y][3] > 8:
            count += 1
        if width > 1 and px[width - 1, y][3] > 8:
            count += 1
    return count


def visible_background_pixels(image: Image.Image, mode: str) -> int:
    if mode == "auto":
        return 0
    px = image.load()
    count = 0
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = px[x, y]
            if a <= 8:
                continue
            if not touches_transparency(px, image.width, image.height, x, y, radius=1):
                continue
            if is_strict_screen_color((r, g, b), mode):
                count += 1
    return count


def is_strict_screen_color(rgb: tuple[int, int, int], mode: str) -> bool:
    r, g, b = rgb
    if mode == "green":
        return g >= 220 and r <= 80 and b <= 80 and g >= r + 110 and g >= b + 110
    if mode == "magenta":
        return r >= 245 and b >= 245 and g <= 70
    return False


def transparent_colored_pixels(image: Image.Image) -> int:
    px = image.load()
    count = 0
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = px[x, y]
            if a == 0 and (r or g or b):
                count += 1
    return count


def check_asset(spec: AssetSpec) -> AssetCheck:
    path = spec.path
    mode = spec.mode
    image = Image.open(path).convert("RGBA")
    alpha = image.getchannel("A")
    bbox = alpha.getbbox()
    visible_pixels = sum(alpha.histogram()[9:])
    edge = edge_visible_pixels(image)
    background = visible_background_pixels(image, mode)
    transparent = transparent_colored_pixels(image)
    passed = visible_pixels > 0 and (spec.allow_edge or edge == 0) and background == 0 and transparent == 0
    rel = path.relative_to(ROOT).as_posix()
    return AssetCheck(
        path=rel,
        mode=mode,
        size=image.size,
        visible_pixels=visible_pixels,
        visible_bbox=bbox,
        edge_visible_pixels=edge,
        edge_allowed=spec.allow_edge,
        visible_background_pixels=background,
        transparent_colored_pixels=transparent,
        passed=passed,
    )


def render_contact_sheet(checks: list[AssetCheck]) -> None:
    cell_w = 156
    cell_h = 148
    label_h = 28
    cols = 8
    rows = (len(checks) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell_w, rows * (cell_h + label_h)), (18, 19, 23, 255))
    draw = ImageDraw.Draw(sheet)
    for index, check in enumerate(checks):
        image = Image.open(ROOT / check.path).convert("RGBA")
        image.thumbnail((cell_w - 18, cell_h - 16), Image.Resampling.NEAREST)
        col = index % cols
        row = index // cols
        x = col * cell_w + (cell_w - image.width) // 2
        y = row * (cell_h + label_h) + cell_h - image.height - 8
        sheet.alpha_composite(image, (x, y))
        label = Path(check.path).stem[:22]
        color = (108, 245, 151, 255) if check.passed else (255, 102, 102, 255)
        draw.text((col * cell_w + 6, row * (cell_h + label_h) + cell_h + 4), label, fill=color)
    CONTACT_SHEET.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(CONTACT_SHEET)


def main() -> None:
    specs = expected_assets()
    missing = [spec.path.relative_to(ROOT).as_posix() for spec in specs if not spec.path.exists()]
    if missing:
        raise SystemExit("Missing expected regenerated assets:\n" + "\n".join(missing))

    checks = [check_asset(spec) for spec in specs]
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps([asdict(check) for check in checks], indent=2), encoding="utf-8")
    render_contact_sheet(checks)

    failures = [check for check in checks if not check.passed]
    print(f"checked {len(checks)} regenerated assets")
    print(f"passed {len(checks) - len(failures)}")
    print(f"failed {len(failures)}")
    print(f"report {REPORT.relative_to(ROOT).as_posix()}")
    print(f"contact_sheet {CONTACT_SHEET.relative_to(ROOT).as_posix()}")
    for check in failures[:24]:
        print(
            "FAIL",
            check.path,
            "edge=",
            check.edge_visible_pixels,
            "visible_bg=",
            check.visible_background_pixels,
            "transparent_rgb=",
            check.transparent_colored_pixels,
        )
    if failures:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
