from __future__ import annotations

from pathlib import Path

from PIL import Image

from chroma_cutout import chroma_cutout, fit


SRC = Path("assets/source/imagegen-mage-directional-model-sheet.png")
OUT = Path("assets/player")

DIRECTIONS = ("down", "up", "left", "right")


def main() -> None:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing generated mage directional sheet: {SRC}")
    OUT.mkdir(parents=True, exist_ok=True)
    source = Image.open(SRC).convert("RGBA")
    cell_w = source.width // len(DIRECTIONS)
    for index, direction in enumerate(DIRECTIONS):
        left = index * cell_w
        right = source.width if index == len(DIRECTIONS) - 1 else (index + 1) * cell_w
        sprite = chroma_cutout(
            source.crop((left, 0, right, source.height)),
            "magenta",
            padding=8,
            keep_largest_only=True,
        )
        sprite.save(OUT / f"class_mage_model_{direction}.png")
        if direction == "down":
            sprite.save(OUT / "class_mage_model.png")
            fit(sprite, 144, 160, margin=2).save(OUT / "class_mage.png")


if __name__ == "__main__":
    main()
