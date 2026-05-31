from __future__ import annotations

from pathlib import Path

from PIL import Image

from chroma_cutout import chroma_cutout


SRC = Path("assets/source/monster_sheet_new.png")
OUT = Path("assets/monsters")

GRID_NAMES = (
    ("slime", "wolf", "skeleton", "thornling", "sand_stalker"),
    ("ice_golem", "bog_beast", "ember_imp", "spider", "wraith"),
)

MARGINS = {
    "skeleton": (50, 12, 8, 28),
    "sand_stalker": (86, 16, 8, 28),
    "spider": (86, 16, 8, 28),
    "thornling": (24, 24, 8, 28),
    "wraith": (24, 0, 8, 0),
    "ember_imp": (16, 16, 8, 28),
}


def main() -> None:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing generated monster sheet: {SRC}")
    OUT.mkdir(parents=True, exist_ok=True)
    source = Image.open(SRC).convert("RGBA")
    cell_w = source.width // 5
    cell_h = source.height // 2
    for row, names in enumerate(GRID_NAMES):
        for col, name in enumerate(names):
            margin_left, margin_right, margin_top, margin_bottom = MARGINS.get(name, (0, 0, 8, 28))
            left = max(0, col * cell_w - margin_left)
            top = max(0, row * cell_h - margin_top)
            right = source.width if col == 4 else min(source.width, (col + 1) * cell_w + margin_right)
            bottom = source.height if row == 1 else min(source.height, (row + 1) * cell_h + margin_bottom)
            chroma_cutout(
                source.crop((left, top, right, bottom)),
                "magenta",
                padding=8,
                keep_largest_only=True,
            ).save(OUT / f"{name}.png")


if __name__ == "__main__":
    main()
