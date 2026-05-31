from __future__ import annotations

from pathlib import Path

from PIL import Image

SRC = Path("assets/source/imagegen-interior-wall-orientation-sheet.png")
OUT = Path("assets/interiors")
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
    out = img.convert("RGBA")
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a > 0 and is_chroma((r, g, b, a)):
                px[x, y] = (r, g, b, 0)
    return out


def fit_tile(img: Image.Image) -> Image.Image:
    bbox = img.getchannel("A").getbbox()
    if bbox is None:
        return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    cropped = img.crop(bbox)
    scale = min(SIZE / cropped.width, SIZE / cropped.height)
    draw_w = max(1, int(round(cropped.width * scale)))
    draw_h = max(1, int(round(cropped.height * scale)))
    resized = cropped.resize((draw_w, draw_h), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    canvas.alpha_composite(resized, ((SIZE - draw_w) // 2, (SIZE - draw_h) // 2))
    return canvas


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
