from __future__ import annotations

from pathlib import Path

from PIL import Image

from universal_cutout import CutoutSettings, clean_spill_edges, fit, scrub_transparent_rgb, universal_cutout


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
    resized = source.resize((width, height), Image.Resampling.NEAREST)
    cleaned = clean_spill_edges(resized, "magenta", 8)
    return scrub_transparent_rgb(cleaned)


def cells(source: Image.Image, bleed: int = 48) -> list[Image.Image]:
    cell_w = source.width // 5
    cell_h = source.height // 2
    inset = max(4, min(cell_w, cell_h) // 80)
    out: list[Image.Image] = []
    for index in range(len(NAMES)):
        col = index % 5
        row = index // 5
        left = max(0, col * cell_w + inset - bleed)
        top = max(0, row * cell_h + inset - bleed)
        right = min(source.width, (col + 1) * cell_w - inset + bleed)
        bottom = min(source.height, (row + 1) * cell_h - inset + bleed)
        out.append(source.crop((left, top, right, bottom)))
    return out


def extract_models() -> None:
    if not MODEL_SRC.exists():
        raise FileNotFoundError(f"Missing NPC class model sheet: {MODEL_SRC}")
    source = Image.open(MODEL_SRC).convert("RGBA")
    settings = CutoutSettings(
        mode="magenta",
        padding=12,
        spill_passes=8,
        stray_max_gap=96,
        drop_edge_strays=True,
        drop_above_strays=True,
        drop_below_strays=True,
    )
    for name, cell in zip(NAMES, cells(source, bleed=48)):
        cutout = universal_cutout(cell, settings)
        scale_model_to_reference(cutout).save(OUT / f"{name}_model.png")


def extract_portraits() -> None:
    if not PORTRAIT_SRC.exists():
        raise FileNotFoundError(f"Missing NPC class portrait sheet: {PORTRAIT_SRC}")
    source = Image.open(PORTRAIT_SRC).convert("RGBA")
    settings = CutoutSettings(mode="magenta", padding=8, spill_passes=6, stray_max_gap=16)
    for name, cell in zip(NAMES, cells(source, bleed=0)):
        cutout = universal_cutout(cell, settings)
        fit(cutout, 96, 96, bottom_align=False).save(OUT / f"{name}.png")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    extract_models()
    extract_portraits()


if __name__ == "__main__":
    main()
