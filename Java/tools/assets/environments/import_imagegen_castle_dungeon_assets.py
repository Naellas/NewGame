from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path

from PIL import Image

from tools.assets.shared.universal_cutout import CutoutSettings, universal_cutout


ROOT = JAVA_ROOT
TERRAIN_SRC = ROOT / "assets" / "source" / "imagegen-castle-dungeon-tilesheet.png"
PROPS_SRC = ROOT / "assets" / "source" / "imagegen-castle-dungeon-props-chroma.png"
TERRAIN = ROOT / "assets" / "environments/terrain/common"
DECO = ROOT / "assets" / "environments/props/nature"
TILE_SIZE = 48
PROP_SIZE = 128

TERRAIN_NAMES = [
    "dungeon_castle_floor.png",
    "dungeon_castle_floor_variant_1.png",
    "dungeon_castle_floor_variant_2.png",
    "dungeon_castle_ceremonial_floor.png",
    "dungeon_castle_wall.png",
    "dungeon_castle_wall_cap.png",
    "dungeon_castle_wall_corner.png",
    "dungeon_castle_pit_edge.png",
    "dungeon_castle_passage.png",
    "dungeon_castle_void.png",
    "dungeon_castle_stair_down.png",
    "dungeon_castle_stair_up.png",
    "dungeon_castle_dais.png",
    "dungeon_castle_column_base.png",
    "dungeon_castle_candle_floor.png",
    "dungeon_castle_boss_sigil.png",
]

PROP_NAMES = [
    "dungeon_prop_castle_candles.png",
    "dungeon_prop_castle_statue.png",
    "dungeon_prop_castle_sarcophagus.png",
    "dungeon_prop_castle_bones.png",
    "dungeon_prop_castle_urns.png",
    "dungeon_prop_castle_chains.png",
    "dungeon_prop_castle_rubble.png",
    "dungeon_prop_castle_altar.png",
    "dungeon_prop_castle_broken_pillars.png",
    "dungeon_prop_castle_skulls.png",
    "dungeon_prop_castle_chest.png",
    "dungeon_prop_castle_cobweb.png",
    "dungeon_prop_castle_hanging_cage.png",
    "dungeon_prop_castle_books.png",
    "dungeon_prop_castle_gravestone.png",
    "dungeon_prop_castle_iron_gate.png",
]


from tools.assets.shared.image_ops import crop_cell


def fit_prop(cell: Image.Image) -> Image.Image:
    cell = universal_cutout(cell, CutoutSettings(
        mode="magenta",
        padding=8,
        square=True,
        global_key=True,
        stray_max_gap=24,
    ))
    bbox = cell.getchannel("A").getbbox()
    if bbox is None:
        return Image.new("RGBA", (PROP_SIZE, PROP_SIZE), (0, 0, 0, 0))
    cropped = cell.crop(bbox)
    scale = min((PROP_SIZE - 8) / cropped.width, (PROP_SIZE - 8) / cropped.height)
    size = (max(1, round(cropped.width * scale)), max(1, round(cropped.height * scale)))
    resized = cropped.resize(size, Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (PROP_SIZE, PROP_SIZE), (0, 0, 0, 0))
    canvas.alpha_composite(resized, ((PROP_SIZE - size[0]) // 2, PROP_SIZE - size[1] - 4))
    return canvas


def main() -> None:
    if not TERRAIN_SRC.exists():
        raise FileNotFoundError(f"Missing terrain sheet: {TERRAIN_SRC}")
    if not PROPS_SRC.exists():
        raise FileNotFoundError(f"Missing prop sheet: {PROPS_SRC}")
    TERRAIN.mkdir(parents=True, exist_ok=True)
    DECO.mkdir(parents=True, exist_ok=True)

    terrain_source = Image.open(TERRAIN_SRC).convert("RGBA")
    for index, filename in enumerate(TERRAIN_NAMES):
        crop_cell(terrain_source, index).resize((TILE_SIZE, TILE_SIZE), Image.Resampling.LANCZOS).save(TERRAIN / filename)
        print(f"Wrote {TERRAIN / filename}")

    prop_source = Image.open(PROPS_SRC).convert("RGBA")
    for index, filename in enumerate(PROP_NAMES):
        fit_prop(crop_cell(prop_source, index)).save(DECO / filename)
        print(f"Wrote {DECO / filename}")


if __name__ == "__main__":
    main()
