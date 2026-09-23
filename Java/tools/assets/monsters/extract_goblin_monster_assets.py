from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path

from PIL import Image

from tools.assets.shared.universal_cutout import CutoutSettings, universal_cutout


SRC = Path("assets/source/imagegen-goblin-monster-sheet.png")
OUT = Path("assets/characters/monsters")
GRID_NAMES = (
    ("goblin_scout", "goblin_archer", "goblin_trapper", "goblin_skirmisher"),
    ("goblin_shaman", "hobgoblin_guard", "goblin_warlord", "goblin_king"),
)

SETTINGS = CutoutSettings(
    mode="green",
    padding=12,
    global_key=True,
    stray_max_gap=96,
    drop_edge_strays=True,
    drop_above_strays=True,
    drop_below_strays=True,
    drop_small_green_matte=True,
)


def extract() -> None:
    source = Image.open(SRC).convert("RGBA")
    cell_w = source.width // 4
    cell_h = source.height // 2
    OUT.mkdir(parents=True, exist_ok=True)
    for row, names in enumerate(GRID_NAMES):
        for col, name in enumerate(names):
            bleed = 96
            left = max(0, col * cell_w - bleed)
            top = max(0, row * cell_h - bleed)
            right = min(source.width, (source.width if col == 3 else (col + 1) * cell_w) + bleed)
            bottom = min(source.height, (source.height if row == 1 else (row + 1) * cell_h) + bleed)
            cell = source.crop((left, top, right, bottom))
            asset = universal_cutout(cell, SETTINGS)
            asset.save(OUT / f"{name}.png")


if __name__ == "__main__":
    extract()
