from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path

from PIL import Image, ImageDraw

from tools.assets.shared.universal_cutout import CutoutSettings, universal_cutout


SRC = Path("assets/source/imagegen-monster-expansion-sheet.png")
OUT = Path("assets/characters/monsters")
PREVIEW = Path("assets/source/preview-monster-expansion.png")

GRID_NAMES = (
    ("red_dragon", "mountain_drake", "marsh_drake", "bandit_cutthroat"),
    ("bandit_archer", "bandit_captain", "orc_raider", "orc_berserker"),
    ("orc_shaman", "orc_shieldbearer", "swamp_troll", "frost_troll"),
    ("hill_giant", "stone_giant", "fire_giant", "elder_dragon"),
)


def cell_box(width: int, height: int, row: int, col: int) -> tuple[int, int, int, int]:
    cell_w = width / 4
    cell_h = height / 4
    pad_x = 96
    pad_y = 96
    left = max(0, round(col * cell_w) - pad_x)
    top = max(0, round(row * cell_h) - pad_y)
    right = min(width, round((col + 1) * cell_w) + pad_x)
    bottom = min(height, round((row + 1) * cell_h) + pad_y)
    return left, top, right, bottom


def render_preview(images: dict[str, Image.Image]) -> None:
    cell_w = 240
    cell_h = 220
    label_h = 22
    preview = Image.new("RGBA", (cell_w * 4, (cell_h + label_h) * 4), (15, 16, 22, 255))
    draw = ImageDraw.Draw(preview)
    for row, names in enumerate(GRID_NAMES):
        for col, name in enumerate(names):
            source = images[name]
            sprite = source.copy()
            sprite.thumbnail((cell_w - 24, cell_h - 18), Image.Resampling.NEAREST)
            x = col * cell_w + (cell_w - sprite.width) // 2
            y = row * (cell_h + label_h) + cell_h - sprite.height - 8
            preview.alpha_composite(sprite, (x, y))
            draw.text((col * cell_w + 8, row * (cell_h + label_h) + cell_h + 2), name, fill=(236, 238, 242, 255))
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    preview.save(PREVIEW)


def main() -> None:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing generated monster expansion sheet: {SRC}")
    OUT.mkdir(parents=True, exist_ok=True)
    source = Image.open(SRC).convert("RGBA")
    extracted: dict[str, Image.Image] = {}
    settings = CutoutSettings(
        mode="green",
        padding=12,
        global_key=True,
        stray_max_gap=96,
        drop_edge_strays=True,
        drop_above_strays=True,
        drop_below_strays=True,
        drop_small_green_matte=True,
    )
    for row, names in enumerate(GRID_NAMES):
        for col, name in enumerate(names):
            cutout = universal_cutout(
                source.crop(cell_box(source.width, source.height, row, col)),
                settings,
            )
            cutout.save(OUT / f"{name}.png")
            extracted[name] = cutout
    render_preview(extracted)
    print(f"Extracted {len(extracted)} monster expansion sprites.")


if __name__ == "__main__":
    main()
