from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT
from tools.assets.shared.asset_paths import asset_file

from pathlib import Path

from PIL import Image

from tools.assets.shared.universal_cutout import CutoutSettings, fit, universal_cutout


ROOT = JAVA_ROOT
SOURCE = ROOT / "assets" / "source"
TERRAIN_OUT = ROOT / "assets" / "environments/terrain/common"
BEACH_OUT = ROOT / "assets" / "environments/terrain/biomes/beach"
ITEMS_OUT = ROOT / "assets" / "items"

TERRAIN_SOURCE = SOURCE / "imagegen-beach-terrain-sheet.png"
PROPS_SOURCE = SOURCE / "imagegen-beach-props-sheet.png"

TERRAIN_NAMES = [
    "beach",
    "beach_variant_1",
    "submerged_sand",
    "submerged_sand_variant_1",
]

PROP_NAMES = [
    "deco_beach_palm",
    "deco_beach_palm_cluster",
    "deco_beach_coconuts",
    "deco_beach_shells",
    "deco_beach_driftwood",
    "deco_beach_grass",
    "village_prop_palm_shade",
    "village_prop_net_drying_rack",
]

ITEM_ICON_SOURCES = {
    "material_coconut": "deco_beach_coconuts",
    "material_seashell": "deco_beach_shells",
    "material_palm_frond": "village_prop_palm_shade",
    "item_palm_shade": "village_prop_palm_shade",
    "item_net_drying_rack": "village_prop_net_drying_rack",
}


def split_terrain() -> None:
    sheet = Image.open(TERRAIN_SOURCE).convert("RGBA")
    TERRAIN_OUT.mkdir(parents=True, exist_ok=True)
    cell_w = sheet.width // len(TERRAIN_NAMES)
    for index, name in enumerate(TERRAIN_NAMES):
        # Retired: shallow water now uses continuous water coverage, without sand stamps.
        if name.startswith("submerged_sand"):
            continue
        left = index * cell_w
        right = sheet.width if index == len(TERRAIN_NAMES) - 1 else (index + 1) * cell_w
        cell = sheet.crop((left, 0, right, sheet.height))
        side = min(cell.width, cell.height)
        left_crop = (cell.width - side) // 2
        top_crop = (cell.height - side) // 2
        tile = cell.crop((left_crop, top_crop, left_crop + side, top_crop + side))
        tile.resize((64, 64), Image.Resampling.BICUBIC).save(TERRAIN_OUT / f"{name}.png")


def split_props() -> dict[str, Image.Image]:
    sheet = Image.open(PROPS_SOURCE).convert("RGBA")
    BEACH_OUT.mkdir(parents=True, exist_ok=True)
    cols = 4
    rows = 2
    cell_w = sheet.width // cols
    cell_h = sheet.height // rows
    props: dict[str, Image.Image] = {}
    for index, name in enumerate(PROP_NAMES):
        col = index % cols
        row = index // cols
        left = col * cell_w
        top = row * cell_h
        right = sheet.width if col == cols - 1 else (col + 1) * cell_w
        bottom = sheet.height if row == rows - 1 else (row + 1) * cell_h
        cell = sheet.crop((left, top, right, bottom))
        cutout = universal_cutout(
            cell,
            CutoutSettings(mode="magenta", padding=12, stray_max_gap=18, spill_passes=6),
        )
        fitted = fit(cutout, 128, 128, bottom_align=True, margin=4)
        fitted.save(BEACH_OUT / f"{name}.png")
        props[name] = fitted
    return props


def write_item_icons(props: dict[str, Image.Image]) -> None:
    ITEMS_OUT.mkdir(parents=True, exist_ok=True)
    for icon, source in ITEM_ICON_SOURCES.items():
        destination = asset_file(ROOT / "assets", f"items/{icon}.png")
        destination.parent.mkdir(parents=True, exist_ok=True)
        fit(props[source], 64, 64, bottom_align=False, margin=4).save(destination)


def main() -> None:
    if not TERRAIN_SOURCE.exists():
        raise FileNotFoundError(f"Missing terrain source: {TERRAIN_SOURCE}")
    if not PROPS_SOURCE.exists():
        raise FileNotFoundError(f"Missing prop source: {PROPS_SOURCE}")
    split_terrain()
    props = split_props()
    write_item_icons(props)
    terrain_count = sum(not name.startswith("submerged_sand") for name in TERRAIN_NAMES)
    print(f"Extracted {terrain_count} beach terrain tiles and {len(PROP_NAMES)} beach props.")


if __name__ == "__main__":
    main()
