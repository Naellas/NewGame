from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path

from PIL import Image

from tools.assets.shared.universal_cutout import CutoutSettings, universal_cutout


SRC = Path("assets/source/imagegen-npc-sheet.png")
OUT = Path("assets/characters/npcs/townsfolk")

# Three high-detail companion cells across the generated sheet.
BOXES = {
    "npc_marla": (0, 0, 512, 1024),
    "npc_ren": (512, 0, 1024, 1024),
    "npc_torin": (1024, 0, 1536, 1024),
}


def main() -> None:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing generated NPC sheet: {SRC}")
    OUT.mkdir(parents=True, exist_ok=True)
    source = Image.open(SRC).convert("RGBA")
    settings = CutoutSettings(mode="magenta", padding=8, keep_largest_only=True, spill_passes=8)
    for name, box in BOXES.items():
        universal_cutout(source.crop(box), settings).save(OUT / f"{name}_model.png")


if __name__ == "__main__":
    main()
