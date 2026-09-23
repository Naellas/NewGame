from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path

from PIL import Image

from tools.assets.shared.asset_paths import player_dir
from tools.assets.shared.universal_cutout import CutoutSettings, fit, universal_cutout


SRC = Path("assets/source/imagegen-mage-directional-model-sheet.png")
ASSETS = Path("assets")

DIRECTIONS = ("down", "up", "left", "right")


def main() -> None:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing generated mage directional sheet: {SRC}")
    out_dir = player_dir(ASSETS, "class_mage_model")
    out_dir.mkdir(parents=True, exist_ok=True)
    source = Image.open(SRC).convert("RGBA")
    cell_w = source.width // len(DIRECTIONS)
    settings = CutoutSettings(
        mode="magenta",
        padding=12,
        stray_max_gap=64,
        drop_edge_strays=True,
        drop_above_strays=True,
        drop_below_strays=True,
    )
    for index, direction in enumerate(DIRECTIONS):
        bleed = 72
        left = max(0, index * cell_w - bleed)
        right = min(source.width, (source.width if index == len(DIRECTIONS) - 1 else (index + 1) * cell_w) + bleed)
        sprite = universal_cutout(source.crop((left, 0, right, source.height)), settings)
        sprite.save(out_dir / f"class_mage_model_{direction}.png")
        if direction == "down":
            sprite.save(out_dir / "class_mage_model.png")
            fit(sprite, 144, 160, margin=2).save(out_dir / "class_mage.png")


if __name__ == "__main__":
    main()
