from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = JAVA_ROOT
CITY_DIR = ROOT / "assets" / "environments/settlements/city"


def shadow(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int]) -> None:
    draw.ellipse(box, fill=(12, 16, 18, 82))


def outline(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], fill: tuple[int, int, int, int], edge: tuple[int, int, int, int]) -> None:
    draw.polygon(points, fill=edge)
    inset = [(x, y + 2 if y < sum(p[1] for p in points) / len(points) else y - 1) for x, y in points]
    draw.polygon(inset, fill=fill)


def sprite(size: int = 96) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img, "RGBA")


def save(img: Image.Image, name: str) -> None:
    CITY_DIR.mkdir(parents=True, exist_ok=True)
    img.save(CITY_DIR / f"{name}.png")


def add_noise(img: Image.Image, alpha: int = 30) -> Image.Image:
    noise = Image.new("RGBA", img.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(noise, "RGBA")
    for y in range(0, img.height, 3):
        for x in range((y // 3) % 2, img.width, 6):
            draw.point((x, y), fill=(255, 255, 255, alpha))
    return Image.alpha_composite(img, noise)


def draw_granary() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (19, 76, 78, 90))
    draw.rectangle((29, 39, 70, 79), fill=(139, 93, 50, 255), outline=(70, 48, 34, 255))
    for x in range(34, 68, 9):
        draw.line((x, 42, x, 77), fill=(101, 66, 39, 210), width=2)
    outline(draw, [(24, 40), (49, 19), (75, 40)], (132, 55, 45, 255), (73, 39, 38, 255))
    draw.rectangle((40, 58, 58, 79), fill=(74, 45, 30, 255), outline=(45, 31, 24, 255))
    draw.rectangle((60, 48, 68, 59), fill=(213, 174, 91, 255), outline=(93, 67, 42, 255))
    return add_noise(img)


def draw_watchtower() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (24, 78, 73, 90))
    draw.rectangle((38, 31, 58, 80), fill=(109, 78, 48, 255), outline=(55, 42, 34, 255))
    for y in (43, 55, 67):
        draw.line((35, y, 61, y + 8), fill=(69, 50, 37, 220), width=2)
        draw.line((61, y, 35, y + 8), fill=(69, 50, 37, 220), width=2)
    draw.rectangle((27, 25, 69, 40), fill=(129, 84, 49, 255), outline=(61, 43, 33, 255))
    outline(draw, [(24, 26), (48, 11), (72, 26)], (76, 92, 77, 255), (42, 55, 48, 255))
    draw.rectangle((35, 31, 42, 37), fill=(236, 184, 84, 230))
    draw.rectangle((54, 31, 61, 37), fill=(236, 184, 84, 230))
    return add_noise(img)


def draw_shrine() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (24, 75, 73, 88))
    draw.rectangle((31, 53, 65, 79), fill=(117, 117, 109, 255), outline=(57, 62, 62, 255))
    draw.rectangle((38, 42, 58, 56), fill=(139, 137, 124, 255), outline=(68, 72, 70, 255))
    outline(draw, [(28, 43), (48, 25), (68, 43)], (77, 105, 88, 255), (41, 57, 52, 255))
    draw.ellipse((42, 31, 54, 43), fill=(238, 193, 87, 190))
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow, "RGBA")
    gd.ellipse((31, 22, 65, 55), fill=(245, 200, 95, 65))
    img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(4)))
    return add_noise(img)


def draw_workshop() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (17, 76, 81, 90))
    draw.rectangle((26, 43, 74, 79), fill=(121, 84, 57, 255), outline=(63, 42, 34, 255))
    outline(draw, [(21, 44), (50, 23), (79, 44)], (95, 88, 80, 255), (45, 46, 45, 255))
    draw.rectangle((36, 57, 52, 79), fill=(65, 43, 32, 255), outline=(42, 31, 25, 255))
    draw.rectangle((57, 51, 68, 61), fill=(230, 169, 82, 230), outline=(76, 55, 38, 255))
    draw.rectangle((68, 29, 74, 43), fill=(80, 67, 59, 255), outline=(42, 38, 35, 255))
    draw.ellipse((66, 21, 79, 31), fill=(75, 80, 77, 110))
    return add_noise(img)


def draw_garden() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (18, 76, 81, 90))
    draw.rounded_rectangle((22, 46, 76, 78), radius=5, fill=(91, 116, 67, 245), outline=(53, 75, 48, 255), width=2)
    for x in (31, 43, 55, 67):
        draw.line((x, 49, x - 5, 76), fill=(70, 96, 55, 190), width=2)
    for x, y, color in ((32, 57, (229, 210, 94, 255)), (47, 63, (218, 111, 127, 255)), (60, 55, (141, 187, 91, 255)), (67, 68, (230, 226, 173, 255))):
        draw.ellipse((x - 4, y - 4, x + 4, y + 4), fill=color, outline=(54, 77, 48, 180))
    outline(draw, [(23, 46), (50, 31), (77, 46)], (144, 92, 56, 230), (72, 48, 34, 230))
    return add_noise(img)


def draw_warehouse() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (15, 76, 84, 90))
    draw.rectangle((22, 40, 78, 80), fill=(119, 82, 50, 255), outline=(59, 42, 32, 255), width=2)
    for x in (30, 43, 56, 69):
        draw.line((x, 43, x, 78), fill=(82, 54, 35, 210), width=2)
    outline(draw, [(17, 41), (50, 18), (83, 41)], (92, 88, 80, 255), (45, 45, 43, 255))
    draw.rectangle((38, 58, 62, 80), fill=(73, 48, 33, 255), outline=(42, 30, 24, 255), width=2)
    draw.rectangle((63, 51, 73, 61), fill=(218, 166, 82, 240), outline=(83, 59, 39, 255))
    return add_noise(img)


def draw_mine() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (14, 76, 83, 90))
    draw.polygon([(21, 79), (30, 45), (49, 24), (70, 45), (80, 79)], fill=(96, 94, 87, 255), outline=(46, 48, 49, 255))
    draw.polygon([(34, 79), (39, 52), (49, 41), (60, 52), (66, 79)], fill=(45, 40, 38, 255), outline=(26, 24, 24, 255))
    draw.rectangle((30, 50, 69, 56), fill=(117, 76, 43, 255), outline=(63, 42, 31, 255))
    draw.rectangle((36, 56, 42, 80), fill=(103, 68, 40, 255))
    draw.rectangle((58, 56, 64, 80), fill=(103, 68, 40, 255))
    draw.ellipse((65, 70, 80, 82), fill=(62, 64, 62, 255), outline=(32, 35, 35, 255))
    return add_noise(img)


def draw_forestry_hut() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (17, 76, 81, 90))
    draw.rectangle((27, 43, 73, 80), fill=(118, 79, 48, 255), outline=(58, 39, 30, 255), width=2)
    outline(draw, [(21, 44), (50, 20), (80, 44)], (72, 106, 64, 255), (38, 58, 42, 255))
    for x in (34, 47, 60):
        draw.line((x, 45, x, 78), fill=(84, 53, 33, 220), width=2)
    draw.rectangle((39, 59, 55, 80), fill=(64, 42, 30, 255), outline=(38, 28, 23, 255))
    draw.rectangle((61, 51, 70, 61), fill=(229, 174, 85, 230), outline=(78, 55, 36, 255))
    draw.line((22, 70, 36, 59), fill=(138, 92, 45, 255), width=4)
    return add_noise(img)


def draw_hunting_camp() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (18, 76, 81, 90))
    draw.polygon([(23, 78), (38, 37), (53, 78)], fill=(119, 73, 52, 255), outline=(57, 39, 33, 255))
    draw.polygon([(47, 78), (63, 36), (79, 78)], fill=(91, 70, 53, 255), outline=(50, 39, 32, 255))
    draw.line((26, 78, 79, 78), fill=(63, 43, 32, 255), width=3)
    draw.arc((34, 26, 70, 58), 190, 350, fill=(188, 147, 82, 255), width=3)
    draw.line((52, 44, 60, 30), fill=(188, 147, 82, 255), width=2)
    draw.ellipse((62, 70, 74, 82), fill=(70, 47, 34, 255))
    return add_noise(img)


def draw_farmstead() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (17, 76, 82, 90))
    draw.rectangle((27, 42, 73, 80), fill=(143, 92, 52, 255), outline=(70, 47, 34, 255), width=2)
    outline(draw, [(21, 43), (50, 22), (79, 43)], (159, 63, 47, 255), (78, 38, 34, 255))
    draw.rectangle((40, 58, 56, 80), fill=(75, 45, 31, 255), outline=(43, 30, 24, 255))
    draw.rectangle((59, 51, 68, 61), fill=(226, 178, 85, 230), outline=(85, 61, 38, 255))
    for x in (20, 29, 74, 83):
        draw.line((x, 70, x, 84), fill=(218, 181, 80, 255), width=2)
        draw.line((x - 3, 76, x + 3, 72), fill=(218, 181, 80, 220), width=2)
    return add_noise(img)


def draw_blacksmith() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (15, 77, 84, 90))
    draw.rectangle((24, 42, 76, 80), fill=(109, 82, 61, 255), outline=(54, 43, 35, 255), width=2)
    draw.rectangle((28, 46, 48, 80), fill=(98, 62, 43, 255), outline=(48, 34, 28, 255))
    draw.rectangle((51, 55, 67, 80), fill=(50, 39, 34, 255), outline=(28, 24, 22, 255), width=2)
    outline(draw, [(19, 43), (49, 21), (82, 43)], (74, 80, 78, 255), (40, 45, 45, 255))
    draw.rectangle((67, 28, 75, 43), fill=(80, 71, 65, 255), outline=(41, 38, 35, 255))
    draw.ellipse((64, 18, 81, 31), fill=(72, 76, 73, 110))
    draw.polygon([(32, 61), (43, 57), (46, 68), (35, 72)], fill=(143, 146, 137, 255), outline=(63, 66, 64, 255))
    draw.ellipse((23, 61, 38, 76), fill=(223, 111, 50, 205), outline=(96, 47, 31, 180))
    return add_noise(img)


def draw_bakery() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (17, 76, 82, 90))
    draw.rectangle((27, 43, 73, 80), fill=(151, 100, 61, 255), outline=(72, 48, 35, 255), width=2)
    outline(draw, [(21, 44), (50, 22), (79, 44)], (156, 78, 55, 255), (77, 42, 36, 255))
    draw.rectangle((39, 59, 55, 80), fill=(74, 45, 32, 255), outline=(42, 30, 24, 255))
    draw.rectangle((58, 51, 69, 62), fill=(232, 183, 90, 230), outline=(86, 61, 38, 255))
    draw.rounded_rectangle((24, 58, 36, 70), radius=3, fill=(79, 62, 48, 255), outline=(42, 33, 27, 255))
    for x, y in ((27, 55), (32, 56), (30, 60)):
        draw.ellipse((x, y, x + 8, y + 5), fill=(226, 176, 91, 255), outline=(120, 78, 42, 160))
    return add_noise(img)


def draw_apothecary() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (17, 76, 82, 90))
    draw.rectangle((27, 43, 73, 80), fill=(112, 91, 66, 255), outline=(57, 45, 35, 255), width=2)
    outline(draw, [(21, 44), (50, 23), (79, 44)], (80, 112, 83, 255), (43, 60, 47, 255))
    draw.rectangle((40, 59, 56, 80), fill=(61, 43, 32, 255), outline=(38, 28, 23, 255))
    draw.rectangle((59, 51, 69, 62), fill=(214, 185, 112, 230), outline=(82, 63, 43, 255))
    draw.rectangle((31, 50, 37, 58), fill=(82, 153, 101, 255), outline=(36, 76, 49, 255))
    draw.rectangle((32, 46, 36, 50), fill=(196, 225, 152, 255))
    draw.line((68, 39, 76, 29), fill=(102, 77, 45, 255), width=2)
    draw.ellipse((72, 25, 80, 33), fill=(94, 151, 83, 220))
    return add_noise(img)


def draw_fishing_hut() -> Image.Image:
    img, draw = sprite()
    shadow(draw, (16, 77, 83, 90))
    draw.rectangle((27, 44, 73, 80), fill=(116, 86, 58, 255), outline=(58, 43, 33, 255), width=2)
    outline(draw, [(21, 45), (50, 24), (80, 45)], (79, 104, 112, 255), (42, 58, 65, 255))
    draw.rectangle((39, 59, 55, 80), fill=(63, 43, 32, 255), outline=(38, 28, 23, 255))
    draw.rectangle((60, 52, 69, 62), fill=(208, 181, 106, 230), outline=(82, 61, 39, 255))
    draw.line((24, 76, 78, 76), fill=(74, 101, 111, 180), width=3)
    draw.arc((22, 39, 40, 68), 270, 80, fill=(160, 126, 74, 255), width=2)
    draw.line((31, 37, 31, 66), fill=(160, 126, 74, 255), width=2)
    draw.ellipse((68, 68, 81, 75), fill=(126, 149, 142, 255), outline=(58, 78, 75, 255))
    return add_noise(img)


def draw_well() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (12, 49, 52, 59))
    draw.ellipse((17, 33, 47, 51), fill=(102, 105, 99, 255), outline=(52, 57, 57, 255), width=2)
    draw.rectangle((17, 25, 47, 42), fill=(118, 120, 111, 255), outline=(52, 57, 57, 255))
    draw.ellipse((18, 18, 46, 34), fill=(49, 82, 95, 255), outline=(52, 57, 57, 255), width=2)
    outline(draw, [(13, 21), (32, 8), (51, 21)], (125, 72, 48, 255), (72, 45, 36, 255))
    return add_noise(img)


def draw_notice_board() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (16, 51, 50, 58))
    draw.rectangle((29, 35, 35, 54), fill=(86, 58, 38, 255))
    draw.rectangle((17, 16, 48, 36), fill=(126, 82, 47, 255), outline=(62, 43, 31, 255), width=2)
    for box in ((21, 20, 30, 29), (34, 20, 44, 31), (25, 30, 39, 34)):
        draw.rectangle(box, fill=(223, 199, 142, 255), outline=(92, 71, 48, 180))
    return add_noise(img)


def draw_tool_rack() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (13, 51, 52, 58))
    draw.rectangle((16, 26, 49, 32), fill=(105, 68, 40, 255), outline=(58, 38, 29, 255))
    draw.line((22, 25, 18, 51), fill=(91, 70, 49, 255), width=3)
    draw.line((32, 25, 32, 52), fill=(91, 70, 49, 255), width=3)
    draw.line((42, 25, 47, 51), fill=(91, 70, 49, 255), width=3)
    draw.polygon([(16, 50), (22, 45), (25, 52)], fill=(139, 142, 132, 255))
    draw.rectangle((42, 45, 51, 50), fill=(142, 142, 126, 255))
    return add_noise(img)


def draw_woodpile() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (13, 48, 52, 58))
    for y, xs in ((43, (16, 28, 40)), (36, (22, 34)), (30, (28,))):
        for x in xs:
            draw.rounded_rectangle((x, y, x + 16, y + 7), radius=3, fill=(130, 82, 43, 255), outline=(68, 43, 30, 255))
            draw.ellipse((x + 1, y + 1, x + 7, y + 6), fill=(168, 111, 61, 255))
    return add_noise(img)


def draw_beehive() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (19, 49, 47, 57))
    for i, y in enumerate((22, 28, 34, 40, 46)):
        draw.rounded_rectangle((22 - i, y, 43 + i, y + 8), radius=5, fill=(205, 157, 66, 255), outline=(111, 78, 40, 230))
    draw.ellipse((29, 42, 36, 49), fill=(48, 43, 35, 255))
    return add_noise(img)


def draw_training_dummy() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (16, 51, 50, 58))
    draw.rectangle((30, 19, 35, 53), fill=(100, 68, 42, 255))
    draw.line((18, 30, 47, 30), fill=(120, 78, 42, 255), width=4)
    draw.ellipse((24, 11, 41, 27), fill=(142, 91, 48, 255), outline=(69, 45, 31, 255))
    draw.rectangle((23, 30, 42, 45), fill=(119, 74, 47, 255), outline=(69, 45, 31, 255))
    draw.line((26, 34, 39, 41), fill=(188, 145, 82, 230), width=2)
    draw.line((39, 34, 26, 41), fill=(188, 145, 82, 230), width=2)
    return add_noise(img)


def draw_ore_cart() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (12, 48, 55, 58))
    draw.polygon([(14, 28), (51, 28), (45, 47), (20, 47)], fill=(92, 67, 48, 255), outline=(48, 36, 30, 255))
    for x, y in ((20, 22), (30, 18), (41, 22)):
        draw.polygon([(x, y), (x + 8, y + 4), (x + 4, y + 11), (x - 4, y + 7)], fill=(112, 115, 112, 255), outline=(54, 57, 57, 255))
    draw.ellipse((18, 43, 28, 53), fill=(38, 37, 35, 255))
    draw.ellipse((39, 43, 49, 53), fill=(38, 37, 35, 255))
    return add_noise(img)


def draw_log_stack() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (11, 48, 55, 58))
    for y, xs in ((42, (13, 29, 45)), (34, (21, 37)), (26, (29,))):
        for x in xs:
            draw.rounded_rectangle((x, y, x + 17, y + 8), radius=4, fill=(126, 79, 40, 255), outline=(64, 42, 30, 255))
            draw.ellipse((x + 1, y + 1, x + 8, y + 7), fill=(170, 112, 60, 255), outline=(83, 52, 31, 180))
    return add_noise(img)


def draw_drying_rack() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (12, 50, 54, 58))
    draw.line((18, 20, 15, 54), fill=(92, 62, 40, 255), width=3)
    draw.line((46, 20, 50, 54), fill=(92, 62, 40, 255), width=3)
    draw.line((17, 24, 47, 24), fill=(108, 72, 42, 255), width=3)
    for x in (23, 32, 41):
        draw.polygon([(x, 27), (x + 7, 31), (x + 5, 45), (x - 2, 42)], fill=(126, 82, 58, 255), outline=(70, 45, 36, 230))
    return add_noise(img)


def draw_hay_stack() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (11, 47, 55, 58))
    bales = (
        (13, 39, 31, 50, (196, 148, 61, 255)),
        (29, 39, 47, 50, (215, 173, 76, 255)),
        (21, 29, 41, 40, (224, 184, 82, 255)),
        (38, 31, 52, 44, (184, 132, 55, 255)),
        (27, 20, 43, 31, (235, 195, 92, 255)),
    )
    for box in bales:
        x1, y1, x2, y2, fill = box
        draw.rounded_rectangle((x1, y1, x2, y2), radius=4, fill=fill, outline=(113, 81, 38, 235), width=2)
        draw.line((x1 + 3, y1 + 4, x2 - 3, y1 + 2), fill=(242, 211, 111, 150), width=1)
        draw.line((x1 + 4, y2 - 3, x2 - 4, y2 - 5), fill=(132, 92, 42, 115), width=1)
        draw.line((x1 + 6, y1 + 2, x1 + 10, y2 - 2), fill=(248, 218, 111, 120), width=1)
    for x, y in ((17, 38), (23, 30), (34, 19), (46, 32), (51, 44)):
        draw.line((x, y, x - 5, y + 10), fill=(235, 199, 91, 150), width=1)
    return add_noise(img)


def draw_hay_bales() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (11, 48, 55, 58))
    for box, fill in (
        ((13, 39, 35, 51), (204, 153, 58, 255)),
        ((32, 39, 53, 51), (220, 176, 74, 255)),
        ((23, 28, 45, 40), (231, 190, 85, 255)),
    ):
        draw.rounded_rectangle(box, radius=3, fill=fill, outline=(111, 78, 36, 255), width=2)
        x1, y1, x2, y2 = box
        draw.line((x1 + 4, y1 + 4, x2 - 4, y1 + 2), fill=(247, 215, 107, 155))
        draw.line((x1 + 4, y2 - 4, x2 - 4, y2 - 3), fill=(132, 91, 41, 110))
        draw.line((x1 + 9, y1 + 1, x1 + 9, y2 - 1), fill=(139, 95, 42, 130))
    return add_noise(img)


def draw_grain_sacks() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (13, 48, 53, 58))
    sacks = (
        (17, 28, 34, 51, (176, 139, 91, 255)),
        (31, 27, 48, 51, (191, 154, 101, 255)),
        (25, 20, 41, 40, (206, 170, 113, 255)),
    )
    for x1, y1, x2, y2, fill in sacks:
        draw.rounded_rectangle((x1, y1, x2, y2), radius=7, fill=fill, outline=(91, 67, 43, 230), width=2)
        draw.line((x1 + 4, y1 + 7, x2 - 4, y1 + 7), fill=(111, 79, 48, 145), width=1)
        draw.arc((x1 + 3, y1 + 5, x2 - 3, y2 - 2), 210, 315, fill=(232, 205, 152, 115), width=1)
    draw.line((32, 21, 37, 17), fill=(118, 82, 45, 255), width=2)
    return add_noise(img)


def draw_produce_basket() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (14, 49, 52, 58))
    draw.arc((17, 20, 49, 51), 190, 350, fill=(112, 72, 39, 255), width=3)
    draw.rounded_rectangle((15, 35, 51, 52), radius=5, fill=(128, 80, 42, 255), outline=(65, 43, 29, 255), width=2)
    for x in (22, 31, 40):
        draw.line((x, 36, x + 3, 51), fill=(167, 111, 57, 180), width=1)
    for x, y, color in (
        (24, 32, (122, 166, 67, 255)),
        (32, 28, (196, 62, 53, 255)),
        (41, 33, (232, 166, 59, 255)),
        (35, 36, (107, 151, 62, 255)),
        (27, 38, (217, 84, 66, 255)),
    ):
        draw.ellipse((x - 4, y - 4, x + 4, y + 4), fill=color, outline=(48, 69, 38, 150))
    return add_noise(img)


def draw_water_trough() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (12, 47, 55, 58))
    draw.polygon([(14, 32), (52, 32), (47, 51), (19, 51)], fill=(108, 77, 49, 255), outline=(55, 40, 31, 255))
    draw.polygon([(17, 34), (49, 34), (45, 43), (21, 43)], fill=(46, 86, 101, 255), outline=(37, 58, 64, 255))
    draw.line((21, 37, 44, 36), fill=(104, 156, 165, 150), width=2)
    draw.rectangle((17, 49, 23, 55), fill=(73, 50, 34, 255))
    draw.rectangle((43, 49, 49, 55), fill=(73, 50, 34, 255))
    return add_noise(img)


def draw_fence_segment() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (10, 50, 56, 58))
    for x in (16, 32, 48):
        draw.rectangle((x - 3, 23, x + 3, 53), fill=(107, 72, 42, 255), outline=(56, 39, 29, 255))
        draw.polygon([(x - 4, 23), (x, 17), (x + 4, 23)], fill=(132, 88, 48, 255), outline=(61, 42, 31, 255))
    for y in (32, 43):
        draw.rounded_rectangle((12, y, 53, y + 5), radius=2, fill=(126, 82, 45, 255), outline=(60, 41, 30, 220))
    return add_noise(img)


def draw_village_bench() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (13, 48, 54, 58))
    draw.rounded_rectangle((15, 31, 50, 38), radius=3, fill=(126, 80, 43, 255), outline=(61, 42, 30, 255), width=2)
    draw.rounded_rectangle((18, 40, 48, 47), radius=3, fill=(111, 69, 38, 255), outline=(56, 38, 29, 255), width=2)
    for x in (21, 43):
        draw.line((x, 47, x - 3, 54), fill=(70, 48, 34, 255), width=3)
        draw.line((x + 3, 47, x + 6, 54), fill=(70, 48, 34, 255), width=3)
    return add_noise(img)


def draw_farm_tools() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (14, 50, 52, 58))
    draw.line((23, 17, 19, 52), fill=(112, 76, 42, 255), width=3)
    draw.line((38, 18, 44, 52), fill=(112, 76, 42, 255), width=3)
    draw.line((28, 24, 50, 33), fill=(91, 96, 88, 255), width=2)
    for x in (43, 48, 53):
        draw.line((x, 28, x - 3, 38), fill=(142, 147, 135, 255), width=1)
    draw.arc((13, 44, 28, 58), 180, 350, fill=(142, 147, 135, 255), width=2)
    draw.rounded_rectangle((15, 47, 36, 53), radius=2, fill=(121, 80, 44, 255), outline=(60, 41, 30, 230))
    return add_noise(img)


def draw_anvil_stump() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (13, 49, 53, 58))
    draw.rounded_rectangle((24, 34, 41, 54), radius=3, fill=(104, 67, 38, 255), outline=(55, 38, 28, 255))
    draw.polygon([(15, 28), (46, 28), (53, 33), (43, 39), (20, 39), (12, 34)], fill=(112, 118, 116, 255), outline=(51, 56, 56, 255))
    draw.rectangle((25, 39, 39, 45), fill=(70, 74, 72, 255))
    return add_noise(img)


def draw_sawhorse() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (11, 49, 55, 58))
    draw.rounded_rectangle((14, 25, 51, 31), radius=2, fill=(130, 83, 44, 255), outline=(62, 42, 31, 255))
    for x in (19, 45):
        draw.line((x, 31, x - 7, 54), fill=(93, 61, 38, 255), width=3)
        draw.line((x + 2, 31, x + 11, 54), fill=(93, 61, 38, 255), width=3)
    draw.line((17, 45, 49, 45), fill=(111, 72, 42, 255), width=2)
    return add_noise(img)


def draw_chopping_block() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (15, 49, 51, 58))
    draw.ellipse((22, 20, 43, 30), fill=(166, 108, 58, 255), outline=(74, 48, 32, 255))
    draw.rectangle((22, 25, 43, 53), fill=(125, 79, 42, 255), outline=(69, 45, 31, 255))
    draw.ellipse((22, 46, 43, 56), fill=(107, 67, 38, 255), outline=(58, 39, 29, 255))
    draw.line((35, 16, 47, 40), fill=(121, 83, 48, 255), width=3)
    draw.polygon([(44, 36), (53, 42), (46, 48), (38, 42)], fill=(143, 148, 139, 255), outline=(64, 68, 65, 255))
    return add_noise(img)


def draw_ore_pile() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (12, 49, 54, 58))
    for x, y, color in (
        (20, 37, (91, 95, 94, 255)),
        (31, 30, (122, 125, 119, 255)),
        (39, 39, (82, 85, 84, 255)),
        (27, 43, (105, 108, 104, 255)),
    ):
        draw.polygon([(x, y), (x + 11, y + 4), (x + 6, y + 14), (x - 5, y + 10)], fill=color, outline=(48, 51, 51, 255))
    draw.point((36, 35), fill=(207, 168, 88, 255))
    draw.point((29, 45), fill=(207, 168, 88, 255))
    return add_noise(img)


def draw_mine_lantern() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (18, 50, 48, 58))
    draw.line((32, 16, 32, 26), fill=(83, 62, 40, 255), width=2)
    draw.rectangle((23, 26, 41, 48), fill=(70, 62, 49, 255), outline=(38, 33, 28, 255), width=2)
    draw.rectangle((27, 30, 37, 43), fill=(232, 171, 72, 220), outline=(112, 72, 35, 255))
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow, "RGBA")
    gd.ellipse((17, 23, 47, 53), fill=(241, 182, 77, 70))
    img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(3)))
    return add_noise(img)


def draw_chicken_coop() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (11, 50, 55, 58))
    draw.rectangle((17, 30, 50, 52), fill=(128, 77, 45, 255), outline=(65, 42, 30, 255), width=2)
    outline(draw, [(13, 31), (33, 17), (54, 31)], (148, 67, 48, 255), (77, 39, 33, 255))
    draw.rectangle((29, 39, 39, 52), fill=(57, 40, 31, 255), outline=(35, 27, 24, 255))
    for x in (21, 45):
        draw.line((x, 52, x - 3, 57), fill=(74, 48, 32, 255), width=2)
    draw.ellipse((46, 45, 55, 52), fill=(232, 226, 202, 255), outline=(114, 98, 75, 160))
    return add_noise(img)


def draw_fish_rack() -> Image.Image:
    img, draw = sprite(64)
    shadow(draw, (12, 50, 54, 58))
    draw.line((18, 20, 15, 54), fill=(92, 62, 40, 255), width=3)
    draw.line((46, 20, 50, 54), fill=(92, 62, 40, 255), width=3)
    draw.line((17, 25, 47, 25), fill=(108, 72, 42, 255), width=3)
    for x, color in ((22, (125, 149, 142, 255)), (32, (147, 160, 148, 255)), (42, (109, 134, 135, 255))):
        draw.ellipse((x, 30, x + 12, 38), fill=color, outline=(58, 78, 75, 230))
        draw.polygon([(x + 10, 34), (x + 16, 29), (x + 16, 39)], fill=color, outline=(58, 78, 75, 230))
        draw.line((x + 6, 26, x + 6, 31), fill=(210, 189, 122, 220), width=1)
    return add_noise(img)


def main() -> None:
    save(draw_granary(), "village_building_granary")
    save(draw_watchtower(), "village_building_watchtower")
    save(draw_shrine(), "village_building_shrine")
    save(draw_workshop(), "village_building_workshop")
    save(draw_garden(), "village_building_garden")
    save(draw_warehouse(), "village_building_warehouse")
    save(draw_mine(), "village_building_mine")
    save(draw_forestry_hut(), "village_building_forestry_hut")
    save(draw_hunting_camp(), "village_building_hunting_camp")
    save(draw_farmstead(), "village_building_farmstead")
    save(draw_blacksmith(), "village_building_blacksmith")
    save(draw_bakery(), "village_building_bakery")
    save(draw_apothecary(), "village_building_apothecary")
    save(draw_fishing_hut(), "village_building_fishing_hut")
    save(draw_well(), "village_prop_well")
    save(draw_notice_board(), "village_prop_notice_board")
    save(draw_tool_rack(), "village_prop_tool_rack")
    save(draw_woodpile(), "village_prop_woodpile")
    save(draw_beehive(), "village_prop_beehive")
    save(draw_training_dummy(), "village_prop_training_dummy")
    save(draw_ore_cart(), "village_prop_ore_cart")
    save(draw_log_stack(), "village_prop_log_stack")
    save(draw_drying_rack(), "village_prop_drying_rack")
    save(draw_hay_stack(), "village_prop_hay_stack")
    save(draw_hay_bales(), "village_prop_hay_bales")
    save(draw_grain_sacks(), "village_prop_grain_sacks")
    save(draw_produce_basket(), "village_prop_produce_basket")
    save(draw_water_trough(), "village_prop_water_trough")
    save(draw_fence_segment(), "village_prop_fence_segment")
    save(draw_village_bench(), "village_prop_bench")
    save(draw_farm_tools(), "village_prop_farm_tools")
    save(draw_anvil_stump(), "village_prop_anvil_stump")
    save(draw_sawhorse(), "village_prop_sawhorse")
    save(draw_chopping_block(), "village_prop_chopping_block")
    save(draw_ore_pile(), "village_prop_ore_pile")
    save(draw_mine_lantern(), "village_prop_mine_lantern")
    save(draw_chicken_coop(), "village_prop_chicken_coop")
    save(draw_fish_rack(), "village_prop_fish_rack")


if __name__ == "__main__":
    main()
