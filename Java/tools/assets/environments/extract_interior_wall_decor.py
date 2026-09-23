from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path

from PIL import Image

SRC = Path("assets/source/imagegen-interior-wall-decor-plants-sheet.png")
OUT = Path("assets/environments/interiors")

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
    from tools.assets.shared import image_ops
    return image_ops.remove_chroma(img, is_chroma=is_chroma)


from tools.assets.shared.image_ops import trim


from tools.assets.shared.image_ops import fit_footprint_lanczos as fit_to_footprint


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
