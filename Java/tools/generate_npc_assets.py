from __future__ import annotations

from pathlib import Path

from PIL import Image

from chroma_cutout import chroma_cutout


SRC = Path("assets/source/imagegen-npc-sheet.png")
OUT = Path("assets/npcs")

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
    for name, box in BOXES.items():
        chroma_cutout(source.crop(box), "magenta", padding=8, keep_largest_only=True).save(OUT / f"{name}_model.png")


if __name__ == "__main__":
    main()
