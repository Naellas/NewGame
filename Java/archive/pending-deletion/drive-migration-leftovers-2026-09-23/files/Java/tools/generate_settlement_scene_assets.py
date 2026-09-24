from __future__ import annotations

from pathlib import Path
from random import Random

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "assets"
TILE = 48


Color = tuple[int, int, int, int]


def rgba(color: tuple[int, int, int], alpha: int = 255) -> Color:
    return color[0], color[1], color[2], alpha


def save(img: Image.Image, folder: str, name: str) -> None:
    out = ASSETS / folder
    out.mkdir(parents=True, exist_ok=True)
    img.save(out / f"{name}.png")


def noise_tile(base: tuple[int, int, int], accent: tuple[int, int, int], seed: int) -> Image.Image:
    rng = Random(seed)
    img = Image.new("RGBA", (TILE, TILE), rgba(base))
    pix = img.load()
    for y in range(TILE):
        for x in range(TILE):
            shift = rng.randint(-9, 8)
            if rng.random() < 0.12:
                shift += rng.choice([-14, 14])
            pix[x, y] = rgba(tuple(max(0, min(255, c + shift)) for c in base))
    draw = ImageDraw.Draw(img, "RGBA")
    for _ in range(16):
        x = rng.randrange(TILE)
        y = rng.randrange(TILE)
        draw.point((x, y), fill=rgba(accent, rng.randrange(80, 150)))
    return img


def town_herringbone_cobble() -> Image.Image:
    img = noise_tile((142, 135, 122), (196, 188, 166), 1101)
    draw = ImageDraw.Draw(img, "RGBA")
    mortar = (72, 76, 75, 105)
    for y in range(-8, TILE + 8, 12):
        for x in range(-18, TILE + 18, 18):
            offset = 9 if (y // 12) % 2 else 0
            draw.line((x + offset, y, x + offset + 14, y + 8), fill=mortar, width=2)
            draw.line((x + offset + 14, y + 8, x + offset + 3, y + 16), fill=mortar, width=2)
    draw.rectangle((0, 0, TILE - 1, TILE - 1), outline=(255, 255, 255, 18))
    return img


def town_brick_crosswalk() -> Image.Image:
    img = noise_tile((121, 91, 75), (215, 158, 109), 1103)
    draw = ImageDraw.Draw(img, "RGBA")
    mortar = (61, 54, 52, 120)
    for y in range(3, TILE, 9):
        draw.line((0, y, TILE, y), fill=mortar, width=2)
    for row, y in enumerate(range(3, TILE, 9)):
        for x in range(-8 + (row % 2) * 10, TILE, 20):
            draw.line((x, y, x, y + 9), fill=mortar, width=2)
    draw.rectangle((0, 0, TILE - 1, TILE - 1), outline=(255, 227, 184, 22))
    return img


def village_packed_earth() -> Image.Image:
    img = noise_tile((138, 105, 65), (211, 178, 107), 1201)
    draw = ImageDraw.Draw(img, "RGBA")
    rng = Random(1205)
    for _ in range(18):
        x = rng.randrange(2, TILE - 3)
        y = rng.randrange(2, TILE - 3)
        draw.ellipse((x, y, x + rng.randrange(2, 5), y + rng.randrange(1, 3)), fill=(74, 54, 34, rng.randrange(55, 105)))
    for _ in range(7):
        x = rng.randrange(TILE)
        y = rng.randrange(TILE)
        draw.line((x, y, x + rng.randrange(-8, 9), y + rng.randrange(-4, 5)), fill=(86, 63, 39, 70), width=1)
    return img


def village_plank_walk() -> Image.Image:
    img = Image.new("RGBA", (TILE, TILE), (111, 82, 48, 255))
    draw = ImageDraw.Draw(img, "RGBA")
    rng = Random(1211)
    for y in range(0, TILE, 12):
        color = (112 + rng.randrange(-10, 14), 79 + rng.randrange(-8, 10), 45 + rng.randrange(-6, 7), 255)
        draw.rectangle((0, y, TILE, y + 10), fill=color)
        draw.line((0, y + 10, TILE, y + 10), fill=(55, 39, 26, 170), width=2)
        for x in range(4, TILE, 14):
            draw.ellipse((x, y + 4, x + 2, y + 6), fill=(44, 31, 21, 150))
    for x in range(0, TILE, 24):
        draw.line((x, 0, x, TILE), fill=(63, 45, 29, 65), width=1)
    return img


def dungeon_mosaic_floor() -> Image.Image:
    img = noise_tile((64, 58, 72), (116, 108, 131), 1301)
    draw = ImageDraw.Draw(img, "RGBA")
    mortar = (27, 26, 34, 145)
    for y in range(0, TILE, 12):
        draw.line((0, y, TILE, y), fill=mortar, width=2)
    for x in range(0, TILE, 12):
        draw.line((x, 0, x, TILE), fill=mortar, width=2)
    draw.arc((9, 9, 38, 38), 0, 360, fill=(87, 151, 139, 105), width=2)
    draw.line((24, 8, 24, 40), fill=(87, 151, 139, 75), width=1)
    draw.line((8, 24, 40, 24), fill=(87, 151, 139, 75), width=1)
    return img


def dungeon_cracked_flagstone() -> Image.Image:
    img = noise_tile((58, 57, 62), (128, 120, 114), 1307)
    draw = ImageDraw.Draw(img, "RGBA")
    mortar = (23, 22, 27, 150)
    for y in (13, 29, 42):
        draw.line((0, y, TILE, y + 2), fill=mortar, width=2)
    for x in (10, 25, 39):
        draw.line((x, 0, x + 2, TILE), fill=mortar, width=2)
    cracks = [
        [(6, 8), (14, 15), (13, 24), (20, 31)],
        [(34, 5), (31, 16), (38, 24), (35, 34), (41, 44)],
        [(3, 39), (13, 36), (22, 43)],
    ]
    for crack in cracks:
        draw.line(crack, fill=(18, 17, 20, 170), width=2)
        draw.line(crack, fill=(107, 104, 112, 55), width=1)
    return img


def new_prop_canvas(width: int = 64, height: int = 64) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")
    return img, draw


def shadow(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], alpha: int = 80) -> None:
    draw.ellipse(box, fill=(0, 0, 0, alpha))


def city_prop_news_kiosk() -> Image.Image:
    img, draw = new_prop_canvas(72, 72)
    shadow(draw, (13, 57, 59, 67))
    draw.rectangle((22, 24, 50, 58), fill=(113, 73, 45, 255), outline=(54, 39, 31, 255))
    draw.polygon([(18, 25), (36, 12), (54, 25)], fill=(156, 48, 55, 255), outline=(77, 35, 39, 255))
    for x in (27, 39):
        draw.rectangle((x, 30, x + 7, 46), fill=(230, 213, 168, 255), outline=(68, 56, 48, 230))
        draw.line((x + 2, 34, x + 5, 34), fill=(83, 73, 64, 185), width=1)
        draw.line((x + 2, 38, x + 5, 38), fill=(83, 73, 64, 150), width=1)
    draw.rectangle((25, 50, 47, 56), fill=(82, 57, 38, 255))
    return img


def city_prop_wagon_awning() -> Image.Image:
    img, draw = new_prop_canvas(80, 72)
    shadow(draw, (14, 56, 66, 67))
    draw.rectangle((19, 39, 61, 56), fill=(126, 78, 45, 255), outline=(59, 41, 29, 255))
    for x in (26, 54):
        draw.ellipse((x - 4, 51, x + 5, 60), fill=(43, 35, 29, 255))
        draw.ellipse((x - 1, 54, x + 2, 57), fill=(128, 105, 77, 255))
    draw.polygon([(14, 34), (24, 18), (56, 18), (66, 34)], fill=(202, 174, 94, 255), outline=(83, 68, 45, 255))
    for x in range(23, 58, 10):
        draw.line((x, 19, x - 5, 34), fill=(159, 55, 58, 255), width=5)
    draw.line((18, 36, 62, 36), fill=(77, 54, 38, 220), width=3)
    return img


def city_prop_bunting_pole() -> Image.Image:
    img, draw = new_prop_canvas(56, 72)
    shadow(draw, (18, 59, 39, 67), 60)
    draw.line((28, 15, 28, 60), fill=(80, 58, 40, 255), width=4)
    draw.line((18, 22, 44, 28), fill=(58, 47, 43, 235), width=2)
    flags = [(20, 23, (178, 59, 65)), (28, 25, (64, 119, 168)), (36, 27, (214, 177, 80))]
    for x, y, color in flags:
        draw.polygon([(x, y), (x + 7, y + 2), (x + 2, y + 9)], fill=rgba(color), outline=(47, 39, 39, 220))
    draw.rectangle((23, 58, 33, 63), fill=(91, 82, 72, 255), outline=(42, 35, 32, 255))
    return img


def city_prop_stone_bench() -> Image.Image:
    img, draw = new_prop_canvas(72, 48)
    shadow(draw, (13, 34, 58, 44), 65)
    draw.rounded_rectangle((15, 21, 58, 30), radius=3, fill=(139, 134, 125, 255), outline=(66, 64, 61, 255))
    draw.rectangle((20, 30, 27, 38), fill=(91, 88, 83, 255))
    draw.rectangle((47, 30, 54, 38), fill=(91, 88, 83, 255))
    draw.line((19, 24, 54, 24), fill=(185, 179, 164, 130), width=1)
    return img


def village_prop_clay_oven() -> Image.Image:
    img, draw = new_prop_canvas(72, 72)
    shadow(draw, (16, 55, 58, 66), 75)
    draw.ellipse((19, 19, 55, 58), fill=(168, 93, 55, 255), outline=(76, 47, 35, 255))
    draw.rectangle((23, 39, 51, 59), fill=(151, 79, 47, 255))
    draw.ellipse((28, 36, 46, 54), fill=(36, 29, 26, 255))
    draw.arc((25, 17, 50, 50), 200, 340, fill=(225, 145, 82, 120), width=2)
    draw.rectangle((43, 12, 50, 26), fill=(92, 62, 46, 255), outline=(45, 33, 27, 255))
    return img


def village_prop_compost_bin() -> Image.Image:
    img, draw = new_prop_canvas(72, 56)
    shadow(draw, (13, 41, 59, 51), 65)
    draw.rectangle((17, 22, 56, 44), fill=(91, 64, 39, 255), outline=(47, 35, 25, 255))
    for x in range(22, 55, 8):
        draw.line((x, 23, x - 2, 43), fill=(57, 42, 31, 160), width=2)
    draw.polygon([(19, 22), (33, 13), (54, 22)], fill=(68, 105, 55, 255))
    draw.line((25, 18, 45, 22), fill=(117, 139, 75, 180), width=2)
    return img


def village_prop_wash_line() -> Image.Image:
    img, draw = new_prop_canvas(88, 64)
    shadow(draw, (15, 50, 73, 60), 45)
    draw.line((20, 18, 20, 53), fill=(76, 54, 33, 255), width=4)
    draw.line((68, 18, 68, 53), fill=(76, 54, 33, 255), width=4)
    draw.line((20, 22, 68, 20), fill=(54, 42, 34, 235), width=2)
    pieces = [(25, 22, 35, 41, (218, 218, 192)), (39, 21, 49, 37, (87, 139, 166)), (53, 21, 63, 43, (181, 77, 82))]
    for x1, y1, x2, y2, color in pieces:
        draw.rectangle((x1, y1, x2, y2), fill=rgba(color), outline=(50, 42, 38, 160))
        draw.rectangle((x1 + 2, y1 - 2, x1 + 4, y1), fill=(91, 71, 47, 255))
    return img


def village_prop_seedling_tray() -> Image.Image:
    img, draw = new_prop_canvas(64, 48)
    shadow(draw, (11, 34, 54, 43), 55)
    draw.rectangle((13, 22, 52, 36), fill=(101, 65, 39, 255), outline=(48, 35, 25, 255))
    for x in range(17, 50, 8):
        draw.line((x, 23, x, 35), fill=(51, 38, 28, 120), width=1)
        draw.ellipse((x - 2, 17, x + 5, 24), fill=(70, 138, 64, 255), outline=(33, 76, 39, 200))
    return img


def dungeon_prop_rune_pillar() -> Image.Image:
    img, draw = new_prop_canvas(72, 88)
    shadow(draw, (19, 71, 54, 83), 80)
    draw.rectangle((25, 17, 48, 72), fill=(88, 84, 96, 255), outline=(36, 34, 43, 255))
    draw.polygon([(21, 17), (36, 8), (52, 17)], fill=(111, 105, 119, 255), outline=(42, 39, 48, 255))
    draw.rectangle((23, 69, 51, 76), fill=(62, 58, 68, 255), outline=(30, 28, 34, 255))
    rune = (91, 228, 198, 220)
    draw.line((36, 27, 31, 39, 39, 39, 34, 52, 43, 52), fill=rune, width=2)
    draw.ellipse((33, 58, 40, 65), outline=rune, width=2)
    return img


def dungeon_prop_lantern_stand() -> Image.Image:
    img, draw = new_prop_canvas(64, 80)
    shadow(draw, (20, 65, 45, 74), 65)
    draw.line((33, 20, 33, 67), fill=(49, 43, 39, 255), width=4)
    draw.line((24, 67, 42, 67), fill=(49, 43, 39, 255), width=4)
    draw.arc((22, 17, 44, 39), 180, 360, fill=(61, 52, 45, 255), width=3)
    draw.rounded_rectangle((25, 33, 41, 52), radius=4, fill=(76, 52, 34, 255), outline=(28, 24, 22, 255))
    draw.ellipse((28, 37, 38, 49), fill=(255, 180, 74, 225))
    draw.ellipse((31, 40, 35, 48), fill=(255, 237, 126, 240))
    return img


def dungeon_prop_chain_stand() -> Image.Image:
    img, draw = new_prop_canvas(72, 72)
    shadow(draw, (15, 57, 57, 66), 65)
    for x in (20, 52):
        draw.line((x, 22, x, 57), fill=(64, 58, 60, 255), width=4)
        draw.rectangle((x - 5, 55, x + 5, 61), fill=(43, 39, 42, 255))
    for i in range(8):
        x = 23 + i * 4
        y = 29 + int(abs(i - 3.5) * 1.8)
        draw.ellipse((x, y, x + 6, y + 8), outline=(94, 88, 91, 230), width=2)
    return img


def dungeon_prop_relic_crate() -> Image.Image:
    img, draw = new_prop_canvas(72, 64)
    shadow(draw, (15, 48, 60, 58), 70)
    draw.rectangle((18, 25, 56, 49), fill=(88, 61, 42, 255), outline=(34, 27, 24, 255))
    draw.line((19, 35, 55, 35), fill=(42, 32, 27, 160), width=2)
    draw.line((30, 26, 30, 48), fill=(47, 34, 27, 155), width=3)
    draw.line((45, 26, 45, 48), fill=(47, 34, 27, 155), width=3)
    draw.rectangle((31, 16, 44, 26), fill=(82, 76, 90, 255), outline=(33, 30, 39, 255))
    draw.polygon([(37, 18), (40, 22), (37, 25), (34, 22)], fill=(91, 218, 189, 210))
    return img


def main() -> None:
    save(town_herringbone_cobble(), "terrain", "town_herringbone_cobble")
    save(town_brick_crosswalk(), "terrain", "town_brick_crosswalk")
    save(village_packed_earth(), "terrain", "village_packed_earth")
    save(village_plank_walk(), "terrain", "village_plank_walk")
    save(dungeon_mosaic_floor(), "terrain", "dungeon_mosaic_floor")
    save(dungeon_cracked_flagstone(), "terrain", "dungeon_cracked_flagstone")

    save(city_prop_news_kiosk(), "city", "city_prop_news_kiosk")
    save(city_prop_wagon_awning(), "city", "city_prop_wagon_awning")
    save(city_prop_bunting_pole(), "city", "city_prop_bunting_pole")
    save(city_prop_stone_bench(), "city", "city_prop_stone_bench")
    save(village_prop_clay_oven(), "city", "village_prop_clay_oven")
    save(village_prop_compost_bin(), "city", "village_prop_compost_bin")
    save(village_prop_wash_line(), "city", "village_prop_wash_line")
    save(village_prop_seedling_tray(), "city", "village_prop_seedling_tray")

    save(dungeon_prop_rune_pillar(), "deco", "dungeon_prop_rune_pillar")
    save(dungeon_prop_lantern_stand(), "deco", "dungeon_prop_lantern_stand")
    save(dungeon_prop_chain_stand(), "deco", "dungeon_prop_chain_stand")
    save(dungeon_prop_relic_crate(), "deco", "dungeon_prop_relic_crate")


if __name__ == "__main__":
    main()
