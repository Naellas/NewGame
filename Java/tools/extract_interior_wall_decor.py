from __future__ import annotations

from pathlib import Path

from PIL import Image

SRC = Path("assets/source/imagegen-interior-wall-decor-plants-sheet.png")
OUT = Path("assets/interiors")

NAMES = [
    "interior_wall_flower_pot",
    "interior_wall_ivy_planter",
    "interior_wall_sconce_lamp",
    "interior_wall_window_small",
    "interior_wall_window_wide",
    "interior_wall_plant_shelf",
    "interior_wall_herb_rack",
    "interior_wall_crystal_ornament",
    "interior_floor_leafy_plant",
    "interior_floor_sapling_pot",
    "interior_floor_bushy_planter",
    "interior_floor_reed_pot",
    "interior_floor_flower_planter",
    "interior_vine_trellis",
    "interior_rug_runner",
    "interior_aquarium_table",
]

FOOTPRINTS = {
    "interior_wall_window_wide": (96, 48),
    "interior_wall_plant_shelf": (96, 48),
    "interior_wall_herb_rack": (96, 48),
    "interior_floor_bushy_planter": (96, 48),
    "interior_aquarium_table": (96, 48),
    "interior_vine_trellis": (48, 96),
}


def is_chroma(pixel: tuple[int, int, int, int]) -> bool:
    r, g, b, _ = pixel
    return r > 140 and b > 140 and g < 100


def remove_chroma(img: Image.Image) -> Image.Image:
    out = img.convert("RGBA")
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a > 0 and is_chroma((r, g, b, a)):
                px[x, y] = (r, g, b, 0)
    return out


def trim(img: Image.Image, padding: int = 2) -> Image.Image:
    bbox = img.getchannel("A").getbbox()
    if bbox is None:
        return img
    left, top, right, bottom = bbox
    return img.crop((
        max(0, left - padding),
        max(0, top - padding),
        min(img.width, right + padding),
        min(img.height, bottom + padding),
    ))


def fit_to_footprint(img: Image.Image, width: int, height: int) -> Image.Image:
    margin = 1
    max_w = width - margin * 2
    max_h = height - margin * 2
    scale = min(max_w / img.width, max_h / img.height)
    draw_w = max(1, int(round(img.width * scale)))
    draw_h = max(1, int(round(img.height * scale)))
    resized = img.resize((draw_w, draw_h), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    canvas.alpha_composite(resized, ((width - draw_w) // 2, height - draw_h - margin))
    return canvas


def main() -> None:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing wall decor sheet: {SRC}")
    OUT.mkdir(parents=True, exist_ok=True)
    source = Image.open(SRC).convert("RGBA")
    x_edges = [round(source.width * i / 4) for i in range(5)]
    y_edges = [round(source.height * i / 4) for i in range(5)]
    for index, name in enumerate(NAMES):
        col = index % 4
        row = index // 4
        cell = source.crop((x_edges[col], y_edges[row], x_edges[col + 1], y_edges[row + 1]))
        cutout = trim(remove_chroma(cell))
        width, height = FOOTPRINTS.get(name, (48, 48))
        fit_to_footprint(cutout, width, height).save(OUT / f"{name}.png")


if __name__ == "__main__":
    main()
