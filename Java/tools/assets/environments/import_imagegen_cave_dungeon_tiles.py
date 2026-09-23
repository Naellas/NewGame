from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path

from PIL import Image


ROOT = JAVA_ROOT
SRC = ROOT / "assets" / "source" / "imagegen-cave-dungeon-tilesheet.png"
TERRAIN = ROOT / "assets" / "environments/terrain/common"
DECO = ROOT / "assets" / "environments/props/nature"
SIZE = 48

TILES = [
    ("terrain", "dungeon_cave_floor.png"),
    ("terrain", "dungeon_cave_floor_variant_1.png"),
    ("terrain", "dungeon_cave_floor_variant_2.png"),
    ("terrain", "dungeon_cave_rubble_floor.png"),
    ("terrain", "dungeon_cave_wall.png"),
    ("terrain", "dungeon_cave_wall_cap.png"),
    ("terrain", "dungeon_cave_wall_corner.png"),
    ("terrain", "dungeon_cave_void_edge.png"),
    ("terrain", "dungeon_cave_passage.png"),
    ("terrain", "dungeon_cave_water.png"),
    ("terrain", "dungeon_cave_bridge.png"),
    ("terrain", "dungeon_cave_stair.png"),
    ("deco", "dungeon_prop_cave_torch.png"),
    ("deco", "dungeon_prop_cave_crates.png"),
    ("deco", "dungeon_prop_cave_bedroll.png"),
    ("deco", "dungeon_prop_cave_crystal.png"),
]


from tools.assets.shared.image_ops import crop_cell


def fit_square(cell: Image.Image) -> Image.Image:
    return cell.convert("RGBA").resize((SIZE, SIZE), Image.Resampling.LANCZOS)


def dungeon_void() -> Image.Image:
    return Image.new("RGBA", (SIZE, SIZE), (4, 4, 5, 255))


def main() -> None:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing imagegen source sheet: {SRC}")
    TERRAIN.mkdir(parents=True, exist_ok=True)
    DECO.mkdir(parents=True, exist_ok=True)
    source = Image.open(SRC).convert("RGBA")
    for index, (folder, filename) in enumerate(TILES):
        out_dir = TERRAIN if folder == "terrain" else DECO
        fit_square(crop_cell(source, index)).save(out_dir / filename)
        print(f"Wrote {out_dir / filename}")
    dungeon_void().save(TERRAIN / "dungeon_void.png")
    print(f"Wrote {TERRAIN / 'dungeon_void.png'}")


if __name__ == "__main__":
    main()
