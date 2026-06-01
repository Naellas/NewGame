from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
LOCATIONS_DIR = ROOT / "assets" / "locations"
SOURCE_DIR = ROOT / "assets" / "source"
TILE = 48
SCALE = 4


def clamp(value: float, lo: int = 0, hi: int = 255) -> int:
    return max(lo, min(hi, int(round(value))))


def smoothstep(edge0: float, edge1: float, value: float) -> float:
    if edge0 == edge1:
        return 1.0 if value >= edge1 else 0.0
    t = max(0.0, min(1.0, (value - edge0) / (edge1 - edge0)))
    return t * t * (3.0 - 2.0 * t)


def source_texture() -> Image.Image:
    path = LOCATIONS_DIR / "location_farmland_tilled.png"
    if not path.exists():
        return Image.new("RGBA", (TILE * SCALE, TILE * SCALE), (111, 77, 46, 255))
    image = Image.open(path).convert("RGBA")
    left = int(image.width * 0.24)
    top = int(image.height * 0.20)
    right = int(image.width * 0.76)
    bottom = int(image.height * 0.78)
    return image.crop((left, top, right, bottom)).resize((TILE * SCALE, TILE * SCALE), Image.Resampling.BICUBIC)


def alpha_feather(width: int, height: int, horizontal: bool) -> Image.Image:
    alpha = Image.new("L", (width, height), 0)
    pixels = alpha.load()
    center = (height if horizontal else width) / 2.0
    half = (height if horizontal else width) * 0.48
    feather = max(2.0, (height if horizontal else width) * 0.10)
    for y in range(height):
        for x in range(width):
            axis = y if horizontal else x
            d = abs(axis + 0.5 - center) - half
            pixels[x, y] = clamp((1.0 - smoothstep(0.0, feather, d)) * 246)
    return alpha.filter(ImageFilter.GaussianBlur(0.35 * SCALE))


def dense_field_tile(texture: Image.Image) -> Image.Image:
    return texture.resize((TILE, TILE), Image.Resampling.LANCZOS)


def connector_vertical(texture: Image.Image) -> Image.Image:
    width = 36 * SCALE
    height = TILE * SCALE
    src = texture.crop(((TILE * SCALE - width) // 2, 0, (TILE * SCALE + width) // 2, height))
    src.putalpha(alpha_feather(width, height, horizontal=False))
    return src.resize((36, TILE), Image.Resampling.LANCZOS)


def connector_horizontal(texture: Image.Image) -> Image.Image:
    width = TILE * SCALE
    height = 36 * SCALE
    src = texture.crop((0, (TILE * SCALE - height) // 2, width, (TILE * SCALE + height) // 2))
    src.putalpha(alpha_feather(width, height, horizontal=True))
    return src.resize((TILE, 36), Image.Resampling.LANCZOS)


def junction_filler(texture: Image.Image) -> Image.Image:
    size = TILE * SCALE
    src = texture.copy()
    alpha = Image.new("L", (size, size), 0)
    pixels = alpha.load()
    center = size / 2.0
    half = size * 0.49
    feather = size * 0.06
    for y in range(size):
        for x in range(size):
            d = max(abs(x + 0.5 - center), abs(y + 0.5 - center)) - half
            pixels[x, y] = clamp((1.0 - smoothstep(0.0, feather, d)) * 250)
    src.putalpha(alpha.filter(ImageFilter.GaussianBlur(0.22 * SCALE)))
    return src.resize((TILE, TILE), Image.Resampling.LANCZOS)


def write_preview() -> None:
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)
    sheet = Image.new("RGBA", (TILE * 3, TILE * 3), (59, 92, 54, 255))
    field = Image.open(LOCATIONS_DIR / "field_farmland_tilled_dense.png").convert("RGBA")
    vertical = Image.open(LOCATIONS_DIR / "field_connector_vertical.png").convert("RGBA")
    horizontal = Image.open(LOCATIONS_DIR / "field_connector_horizontal.png").convert("RGBA")
    filler = Image.open(LOCATIONS_DIR / "field_junction_filler.png").convert("RGBA")
    for y in range(2):
        for x in range(2):
            sheet.alpha_composite(field, (x * TILE, y * TILE))
    sheet.alpha_composite(vertical, (TILE - vertical.width // 2, 0))
    sheet.alpha_composite(vertical, (TILE - vertical.width // 2, TILE))
    sheet.alpha_composite(horizontal, (0, TILE - horizontal.height // 2))
    sheet.alpha_composite(horizontal, (TILE, TILE - horizontal.height // 2))
    sheet.alpha_composite(filler, (TILE - filler.width // 2, TILE - filler.height // 2))
    sheet.save(SOURCE_DIR / "preview-field-connectors.png")


def main() -> None:
    LOCATIONS_DIR.mkdir(parents=True, exist_ok=True)
    texture = source_texture()
    dense_field_tile(texture).save(LOCATIONS_DIR / "field_farmland_tilled_dense.png")
    connector_vertical(texture).save(LOCATIONS_DIR / "field_connector_vertical.png")
    connector_horizontal(texture).save(LOCATIONS_DIR / "field_connector_horizontal.png")
    junction_filler(texture).save(LOCATIONS_DIR / "field_junction_filler.png")
    write_preview()


if __name__ == "__main__":
    main()
