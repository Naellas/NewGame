from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path

from PIL import Image

SRC = Path("assets/source/imagegen-interior-wall-orientation-sheet.png")
OUT = Path("assets/environments/interiors")
SIZE = 48

NAMES = [
    "interior_wall_front_north",
    "interior_wall_front_south",
    "interior_wall_side_left",
    "interior_wall_side_right",
    "interior_wall_outer_tl",
    "interior_wall_outer_tr",
    "interior_wall_outer_bl",
    "interior_wall_outer_br",
    "interior_wall_inner_nw",
    "interior_wall_inner_ne",
    "interior_wall_inner_sw",
    "interior_wall_inner_se",
    "interior_wall_door_h_open",
    "interior_wall_door_v_open",
    "interior_wall_door_h_closed",
    "interior_wall_door_v_closed",
]


def is_chroma(pixel: tuple[int, int, int, int]) -> bool:
    r, g, b, _ = pixel
    return r > 140 and b > 140 and g < 100


def remove_chroma(img: Image.Image) -> Image.Image:
    from tools.assets.shared import image_ops
    return image_ops.remove_chroma(img, is_chroma=is_chroma)


def fit_tile(img: Image.Image) -> Image.Image:
    from tools.assets.shared import image_ops
    return image_ops.fit_tile(img, SIZE=SIZE)


def main() -> None:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing wall orientation sheet: {SRC}")
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
