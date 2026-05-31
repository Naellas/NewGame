from __future__ import annotations

from pathlib import Path

from PIL import Image

from chroma_cutout import chroma_cutout, fit


MODEL_SRC = Path("assets/source/imagegen-new-npc-class-models.png")
PORTRAIT_SRC = Path("assets/source/imagegen-new-npc-class-portraits.png")
OUT = Path("assets/npcs")

NAMES = [
    "npc_liora",
    "npc_garruk",
    "npc_kael",
    "npc_nyx",
    "npc_rowan",
    "npc_mira_sunwarden",
    "npc_vexa",
    "npc_orin",
    "npc_sable",
    "npc_elowen",
]


def scale_model_to_reference(image: Image.Image, reference_height: int = 875) -> Image.Image:
    source = image.convert("RGBA")
    scale = reference_height / max(1, source.height)
    width = max(1, round(source.width * scale))
    height = max(1, round(source.height * scale))
    return source.resize((width, height), Image.Resampling.LANCZOS)


def cells(source: Image.Image) -> list[Image.Image]:
    cell_w = source.width // 5
    cell_h = source.height // 2
    inset = max(4, min(cell_w, cell_h) // 80)
    out: list[Image.Image] = []
    for index in range(len(NAMES)):
        col = index % 5
        row = index // 5
        left = col * cell_w + inset
        top = row * cell_h + inset
        right = (col + 1) * cell_w - inset
        bottom = (row + 1) * cell_h - inset
        out.append(source.crop((left, top, right, bottom)))
    return out


def extract_models() -> None:
    if not MODEL_SRC.exists():
        raise FileNotFoundError(f"Missing NPC class model sheet: {MODEL_SRC}")
    source = Image.open(MODEL_SRC).convert("RGBA")
    for name, cell in zip(NAMES, cells(source)):
        cutout = chroma_cutout(cell, "magenta", padding=8, stray_max_gap=24)
        scale_model_to_reference(cutout).save(OUT / f"{name}_model.png")


def extract_portraits() -> None:
    if not PORTRAIT_SRC.exists():
        raise FileNotFoundError(f"Missing NPC class portrait sheet: {PORTRAIT_SRC}")
    source = Image.open(PORTRAIT_SRC).convert("RGBA")
    for name, cell in zip(NAMES, cells(source)):
        cutout = chroma_cutout(cell, "magenta", padding=8, stray_max_gap=16)
        fit(cutout, 96, 96, bottom_align=False).save(OUT / f"{name}.png")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    extract_models()
    extract_portraits()


if __name__ == "__main__":
    main()
