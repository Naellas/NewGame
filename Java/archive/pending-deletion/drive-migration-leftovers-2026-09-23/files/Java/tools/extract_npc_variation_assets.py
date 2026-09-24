from __future__ import annotations

from pathlib import Path

from PIL import Image

from universal_cutout import CutoutSettings, fit, universal_cutout


MODEL_SRC = Path("assets/source/imagegen-npc-models-pixel-sheet.png")
PORTRAIT_SRC = Path("assets/source/imagegen-npc-portraits-pixel-sheet.png")
OUT = Path("assets/npcs")

NAMES = [
    "npc_blacksmith",
    "npc_merchant",
    "npc_citizen_woman",
    "npc_citizen_man",
    "npc_bartender",
    "npc_baker",
    "npc_quartermaster",
    "npc_innkeeper",
]


def cells(source: Image.Image) -> list[Image.Image]:
    cell_w = source.width // 4
    cell_h = source.height // 2
    out: list[Image.Image] = []
    for index in range(len(NAMES)):
        col = index % 4
        row = index // 4
        out.append(source.crop((col * cell_w, row * cell_h, (col + 1) * cell_w, (row + 1) * cell_h)))
    return out


def extract_models() -> None:
    if not MODEL_SRC.exists():
        raise FileNotFoundError(f"Missing NPC variation sheet: {MODEL_SRC}")
    source = Image.open(MODEL_SRC).convert("RGBA")
    settings = CutoutSettings(mode="magenta", padding=8, keep_largest_only=True, spill_passes=8)
    for name, cell in zip(NAMES, cells(source)):
        # Keep these in the same high-detail family as Marla/Ren/Torin.
        universal_cutout(cell, settings).save(OUT / f"{name}_model.png")


def extract_portraits() -> None:
    if not PORTRAIT_SRC.exists():
        raise FileNotFoundError(f"Missing NPC portrait sheet: {PORTRAIT_SRC}")
    source = Image.open(PORTRAIT_SRC).convert("RGBA")
    settings = CutoutSettings(mode="magenta", padding=8, keep_largest_only=True, spill_passes=6)
    for name, cell in zip(NAMES, cells(source)):
        cutout = universal_cutout(cell, settings)
        fit(cutout, 96, 96, bottom_align=False).save(OUT / f"{name}.png")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    extract_models()
    extract_portraits()


if __name__ == "__main__":
    main()
