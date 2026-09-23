from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path

from PIL import Image


ROOT = JAVA_ROOT
SOURCE = ROOT / "assets" / "environments/terrain/biomes/water" / "generated" / "water-props-imagegen-alpha.png"
OUT = ROOT / "assets" / "environments/terrain/biomes/water"
TARGET_SIZE = 64
MAX_SUBJECT_SIZE = 58

SPRITES = (
    ("deco_water_lily_pad_cluster.png", 0, 1),
    ("deco_water_shore_reeds.png", 1, 2),
    ("deco_water_duckweed_patch.png", 2, 3),
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
    canvas.alpha_composite(sprite, ((TARGET_SIZE - size[0]) // 2, (TARGET_SIZE - size[1]) // 2))
    return canvas


def main() -> None:
    sheet = Image.open(SOURCE).convert("RGBA")
    width, height = sheet.size
    OUT.mkdir(parents=True, exist_ok=True)
    for filename, start, end in SPRITES:
        crop = sheet.crop((width * start // 3, 0, width * end // 3, height))
        fit_to_canvas(crop).save(OUT / filename)
        print(f"Wrote {OUT / filename}")


if __name__ == "__main__":
    main()
