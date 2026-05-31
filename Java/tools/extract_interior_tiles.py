from __future__ import annotations

from pathlib import Path

from PIL import Image

SRC = Path("assets/source/imagegen-interior-tiles-depth-sheet.png")
OUT = Path("assets/interiors")
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
