from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path

from PIL import Image


ROOT = JAVA_ROOT
SOURCE = ROOT / "assets" / "source" / "imagegen-dungeon-overworld-support-props-orthogonal-alpha.png"
OUT = ROOT / "assets" / "environments/locations"
TARGET_SIZE = 280
MAX_SUBJECT_SIZE = 246

SPRITES = (
    ("location_dungeon_collapsed_wall.png", 0, 0),
    ("location_dungeon_broken_altar.png", 1, 0),
    ("location_dungeon_rubble_cairn.png", 0, 1),
    ("location_dungeon_grave_slabs.png", 1, 1),
)


def fit_to_canvas(sprite: Image.Image) -> Image.Image:
    alpha_box = sprite.getchannel("A").getbbox()
    if alpha_box is None:
        raise ValueError("Generated sprite crop is empty")
    sprite = sprite.crop(alpha_box)
    scale = min(MAX_SUBJECT_SIZE / sprite.width, MAX_SUBJECT_SIZE / sprite.height)
    size = (max(1, round(sprite.width * scale)), max(1, round(sprite.height * scale)))
    sprite = sprite.resize(size, Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (TARGET_SIZE, TARGET_SIZE), (0, 0, 0, 0))
    canvas.alpha_composite(sprite, ((TARGET_SIZE - size[0]) // 2, TARGET_SIZE - size[1] - 10))
    return canvas


def main() -> None:
    sheet = Image.open(SOURCE).convert("RGBA")
    width, height = sheet.size
    OUT.mkdir(parents=True, exist_ok=True)
    for filename, col, row in SPRITES:
        crop = sheet.crop((
            width * col // 2,
            height * row // 2,
            width * (col + 1) // 2,
            height * (row + 1) // 2,
        ))
        fit_to_canvas(crop).save(OUT / filename)
        print(f"Wrote {OUT / filename}")


if __name__ == "__main__":
    main()
