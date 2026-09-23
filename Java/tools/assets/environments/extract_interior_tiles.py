from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path

from PIL import Image

SRC = Path("assets/source/imagegen-interior-tiles-depth-sheet.png")
OUT = Path("assets/environments/interiors")
SIZE = 48

NAMES = [
    "interior_floor_warm_a",
    "interior_floor_warm_b",
    "interior_floor_dark",
    "interior_floor_worn",
    "interior_wall_stone",
    "interior_wall_timber",
    "interior_wall_plaster",
    "interior_wall_corner",
    "interior_door_open",
    "interior_door_closed",
    "interior_rug_red",
    "interior_rug_teal",
    "interior_floor_shop",
    "interior_floor_tavern",
    "interior_floor_study",
    "interior_void_edge",
]


def is_chroma(pixel: tuple[int, int, int, int]) -> bool:
    r, g, b, _ = pixel
    return r > 160 and b > 160 and g < 90


def remove_chroma(img: Image.Image) -> Image.Image:
    from tools.assets.shared import image_ops
    return image_ops.remove_chroma(img, is_chroma=is_chroma)


def fit_tile(img: Image.Image) -> Image.Image:
    from tools.assets.shared import image_ops
    return image_ops.fit_tile(img, SIZE=SIZE)


def main() -> None:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing interior tile sheet: {SRC}")
    OUT.mkdir(parents=True, exist_ok=True)
    source = Image.open(SRC).convert("RGBA")
    x_edges = [round(source.width * i / 4) for i in range(5)]
    y_edges = [round(source.height * i / 4) for i in range(5)]
    for index, name in enumerate(NAMES):
        col = index % 4
        row = index // 4
        cell = source.crop((x_edges[col], y_edges[row], x_edges[col + 1], y_edges[row + 1]))
        fit_tile(remove_chroma(cell)).save(OUT / f"{name}.png")


if __name__ == "__main__":
    main()
