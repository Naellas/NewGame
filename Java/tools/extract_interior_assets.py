from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image

SRC = Path("assets/source/imagegen-interior-modular-pixel-sheet-v2.png")
FALLBACK_SRC = Path("assets/source/imagegen-interior-modular-pixel-sheet.png")
OUT = Path("assets/interiors")

NAMES = [
    "interior_round_table",
    "interior_chair_north",
    "interior_chair_south",
    "interior_chair_east",
    "interior_chair_west",
    "interior_bed_vertical",
    "interior_tavern_bar",
    "interior_shop_counter",
    "interior_bookshelf",
    "interior_stove",
    "interior_anvil",
    "interior_forge",
    "interior_oven",
    "interior_crates",
    "interior_barrels",
    "interior_hearth_pot",
]

FOOTPRINTS = {
    "interior_bed_vertical": (48, 96),
    "interior_tavern_bar": (96, 48),
    "interior_shop_counter": (96, 48),
}


def is_chroma(pixel: tuple[int, int, int, int]) -> bool:
    r, g, b, _ = pixel
    return r > 80 and b > 80 and g < 115 and r + b > g * 2 + 80


def remove_chroma(img: Image.Image) -> Image.Image:
    out = img.convert("RGBA")
    px = out.load()
    width, height = out.size
    for y in range(height):
        for x in range(width):
            if is_chroma(px[x, y]):
                r, g, b, _ = px[x, y]
                px[x, y] = (r, g, b, 0)
    return out


def remove_sheet_artifacts(img: Image.Image) -> Image.Image:
    out = img.convert("RGBA")
    px = out.load()
    width, height = out.size
    seen = [[False for _ in range(width)] for _ in range(height)]
    components: list[list[tuple[int, int]]] = []

    for sy in range(height):
        for sx in range(width):
            if seen[sy][sx] or px[sx, sy][3] == 0:
                continue
            q: deque[tuple[int, int]] = deque([(sx, sy)])
            seen[sy][sx] = True
            comp: list[tuple[int, int]] = []
            while q:
                x, y = q.popleft()
                comp.append((x, y))
                for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                    if not (0 <= nx < width and 0 <= ny < height):
                        continue
                    if seen[ny][nx] or px[nx, ny][3] == 0:
                        continue
                    seen[ny][nx] = True
                    q.append((nx, ny))
            components.append(comp)

    for comp in components:
        min_x = min(x for x, _ in comp)
        max_x = max(x for x, _ in comp)
        min_y = min(y for _, y in comp)
        max_y = max(y for _, y in comp)
        comp_w = max_x - min_x + 1
        comp_h = max_y - min_y + 1
        area = len(comp)
        thin_rule = (comp_w > width * 0.45 and comp_h <= 8) or (comp_h > height * 0.45 and comp_w <= 8)
        tiny_speck = area < 10
        if thin_rule or tiny_speck:
            for x, y in comp:
                r, g, b, _ = px[x, y]
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
    x = (width - draw_w) // 2
    y = height - draw_h - margin
    canvas.alpha_composite(resized, (x, y))
    return canvas


def main() -> None:
    src = SRC if SRC.exists() else FALLBACK_SRC
    if not src.exists():
        raise FileNotFoundError(f"Missing modular interior sheet: {src}")
    OUT.mkdir(parents=True, exist_ok=True)

    source = Image.open(src).convert("RGBA")
    x_edges = [round(source.width * i / 4) for i in range(5)]
    y_edges = [round(source.height * i / 4) for i in range(5)]
    for index, name in enumerate(NAMES):
        col = index % 4
        row = index // 4
        cell = source.crop((x_edges[col], y_edges[row], x_edges[col + 1], y_edges[row + 1]))
        cutout = trim(remove_sheet_artifacts(remove_chroma(cell)))
        width, height = FOOTPRINTS.get(name, (48, 48))
        fit_to_footprint(cutout, width, height).save(OUT / f"{name}.png")


if __name__ == "__main__":
    main()
