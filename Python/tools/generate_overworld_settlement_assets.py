from __future__ import annotations

import math
import random
from collections import deque
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
CITY_DIR = ROOT / "assets" / "city"
VILLAGE_SHEET = ROOT / "assets" / "source" / "imagegen-village-tilesheet-nosnow.png"

SIZE = 240


class Component:
    def __init__(self, area: int, box: tuple[int, int, int, int], pixels: set[tuple[int, int]]) -> None:
        self.area = area
        self.box = box
        self.pixels = pixels

    @property
    def width(self) -> int:
        return self.box[2] - self.box[0] + 1

    @property
    def height(self) -> int:
        return self.box[3] - self.box[1] + 1


def is_dark_background(rgb: tuple[int, int, int]) -> bool:
    r, g, b = rgb
    return max(r, g, b) <= 58 and b >= g - 4 and g >= r - 6


def detect_components(img: Image.Image) -> list[Component]:
    rgba = img.convert("RGBA")
    width, height = rgba.size
    px = rgba.load()
    seen = [[False for _ in range(width)] for _ in range(height)]
    components: list[Component] = []

    for y in range(height):
        for x in range(width):
            if seen[y][x] or is_dark_background(px[x, y][:3]):
                continue
            q: deque[tuple[int, int]] = deque([(x, y)])
            seen[y][x] = True
            left = right = x
            top = bottom = y
            points: set[tuple[int, int]] = set()
            while q:
                cx, cy = q.popleft()
                points.add((cx, cy))
                left = min(left, cx)
                right = max(right, cx)
                top = min(top, cy)
                bottom = max(bottom, cy)
                for nx, ny in ((cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
                    if not (0 <= nx < width and 0 <= ny < height):
                        continue
                    if seen[ny][nx] or is_dark_background(px[nx, ny][:3]):
                        continue
                    seen[ny][nx] = True
                    q.append((nx, ny))
            if len(points) >= 500:
                components.append(Component(len(points), (left, top, right, bottom), points))

    components.sort(key=lambda comp: comp.area, reverse=True)
    return components


def component_cutout(sheet: Image.Image, comp: Component) -> Image.Image:
    left, top, right, bottom = comp.box
    crop = sheet.crop((left, top, right + 1, bottom + 1)).convert("RGBA")
    local = {(x - left, y - top) for x, y in comp.pixels}
    px = crop.load()
    for y in range(crop.height):
        for x in range(crop.width):
            if (x, y) not in local:
                r, g, b, _ = px[x, y]
                px[x, y] = (r, g, b, 0)
    bbox = crop.getbbox()
    return crop.crop(bbox) if bbox else crop


def component_color_ratios(sheet: Image.Image, comp: Component) -> tuple[float, float]:
    px = sheet.load()
    wood = 0
    green = 0
    for x, y in comp.pixels:
        r, g, b, _a = px[x, y]
        if r > 82 and 45 <= g <= 145 and b < 100 and r >= g + 10:
            wood += 1
        if g >= r + 8 and g >= b + 5 and g > 78:
            green += 1
    total = max(1, comp.area)
    return wood / total, green / total


def pick_village_assets(sheet: Image.Image) -> dict[str, list[Image.Image]]:
    components = detect_components(sheet)
    houses: list[Image.Image] = []
    barns: list[Image.Image] = []
    props: list[Image.Image] = []
    trees: list[Image.Image] = []
    fence_candidates: list[tuple[int, int, Image.Image]] = []

    for comp in components:
        l, t, _r, _b = comp.box
        if comp.area >= 28_000 and comp.width >= 175 and comp.height >= 170:
            houses.append(component_cutout(sheet, comp))
        elif comp.area >= 20_000 and comp.width >= 225 and comp.height >= 200:
            barns.append(component_cutout(sheet, comp))
        elif 2_800 <= comp.area <= 13_500 and t >= 320 and comp.width >= 45 and comp.height >= 45:
            props.append(component_cutout(sheet, comp))
        elif 2_500 <= comp.area <= 8_500 and 675 <= t <= 930 and comp.height >= 55:
            trees.append(component_cutout(sheet, comp))
        if 2_600 <= comp.area <= 5_500 and t >= 880 and 70 <= comp.width <= 130 and 48 <= comp.height <= 80:
            wood_ratio, green_ratio = component_color_ratios(sheet, comp)
            if wood_ratio >= 0.24 and green_ratio <= 0.42:
                fence_candidates.append((t, l, component_cutout(sheet, comp)))

    fence_candidates.sort(key=lambda item: (item[0], item[1]))
    fences = [item[2] for item in fence_candidates]

    return {
        "houses": houses[:8],
        "barns": barns[:3],
        "props": props[:16],
        "trees": trees[:8],
        "fences": fences[:8],
    }


def clamp(value: int) -> int:
    return max(0, min(255, value))


def tint(img: Image.Image, shift: tuple[int, int, int], saturation: float = 1.0, brightness: float = 1.0) -> Image.Image:
    out = img.convert("RGBA")
    if saturation != 1.0:
        rgb = ImageEnhance.Color(out.convert("RGB")).enhance(saturation)
        out = Image.merge("RGBA", (*rgb.split(), out.getchannel("A")))
    if brightness != 1.0:
        rgb = ImageEnhance.Brightness(out.convert("RGB")).enhance(brightness)
        out = Image.merge("RGBA", (*rgb.split(), out.getchannel("A")))
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            px[x, y] = (clamp(r + shift[0]), clamp(g + shift[1]), clamp(b + shift[2]), a)
    return out


def fit_height(img: Image.Image, height: int) -> Image.Image:
    scale = height / max(1, img.height)
    width = max(1, int(img.width * scale))
    return img.resize((width, height), Image.Resampling.LANCZOS)


def fit_width(img: Image.Image, width: int) -> Image.Image:
    scale = width / max(1, img.width)
    height = max(1, int(img.height * scale))
    return img.resize((width, height), Image.Resampling.LANCZOS)


def paste_anchor(base: Image.Image, sprite: Image.Image, center_x: int, foot_y: int) -> None:
    shadow = Image.new("RGBA", base.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow, "RGBA")
    sw = max(18, int(sprite.width * 0.58))
    sh = max(6, int(sprite.height * 0.10))
    sd.ellipse((center_x - sw // 2, foot_y - sh, center_x + sw // 2, foot_y + sh // 2), fill=(14, 19, 22, 82))
    base.alpha_composite(shadow)
    base.alpha_composite(sprite, (center_x - sprite.width // 2, foot_y - sprite.height))


def draw_textured_polygon(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], fill: tuple[int, int, int, int], seed: int) -> None:
    draw.polygon(points, fill=fill)
    rng = random.Random(seed)
    xs = [x for x, _y in points]
    ys = [y for _x, y in points]
    for _ in range(120):
        x = rng.randrange(min(xs), max(xs) + 1)
        y = rng.randrange(min(ys), max(ys) + 1)
        color = (clamp(fill[0] + rng.randrange(-28, 29)), clamp(fill[1] + rng.randrange(-24, 25)), clamp(fill[2] + rng.randrange(-18, 19)), 90)
        draw.rectangle((x, y, x + rng.randrange(1, 4), y + 1), fill=color)


def draw_roads(draw: ImageDraw.ImageDraw, variant: str) -> None:
    road = {
        "green": (189, 153, 92, 220),
        "snow": (193, 205, 203, 225),
        "desert": (211, 167, 90, 230),
        "marsh": (122, 105, 70, 225),
    }[variant]
    dark = tuple(max(0, c - 48) for c in road[:3]) + (160,)
    paths = [
        ((120, 14), (120, 226)),
        ((15, 120), (225, 120)),
        ((54, 188), (120, 120), (187, 58)),
    ]
    for path in paths:
        draw.line(path, fill=dark, width=22, joint="curve")
        draw.line(path, fill=road, width=15, joint="curve")
        draw.line(path, fill=tuple(min(255, c + 28) for c in road[:3]) + (140,), width=2, joint="curve")


def fence_variant(sprite: Image.Image, variant: str) -> Image.Image:
    if variant == "snow":
        return tint(sprite, (10, 12, 16), saturation=0.76, brightness=1.10)
    if variant == "desert":
        return tint(sprite, (23, 9, -13), saturation=0.88, brightness=1.03)
    if variant == "marsh":
        return tint(sprite, (-13, -4, -7), saturation=0.86, brightness=0.90)
    return sprite


def draw_fences(img: Image.Image, draw: ImageDraw.ImageDraw, variant: str, fences: list[Image.Image]) -> None:
    if not fences:
        rail = (139, 103, 60, 170)
        for box in ((45, 41, 195, 45), (45, 195, 195, 199), (41, 45, 45, 195), (195, 45, 199, 195)):
            draw.rectangle(box, fill=rail)
        return

    horizontal = fence_variant(fit_width(fences[0], 40), variant)
    vertical = horizontal.rotate(90, expand=True, resample=Image.Resampling.BICUBIC)
    top_bottom = [(57, 43), (91, 43), (149, 43), (183, 43), (57, 200), (91, 200), (149, 200), (183, 200)]
    left_right = [(43, 61), (43, 94), (43, 151), (43, 184), (200, 61), (200, 94), (200, 151), (200, 184)]
    for cx, cy in top_bottom:
        img.alpha_composite(horizontal, (cx - horizontal.width // 2, cy - horizontal.height // 2))
    for cx, cy in left_right:
        img.alpha_composite(vertical, (cx - vertical.width // 2, cy - vertical.height // 2))

    post = fence_variant(fit_height(fences[min(1, len(fences) - 1)], 22), variant)
    for cx, cy in ((43, 43), (200, 43), (43, 200), (200, 200), (119, 43), (119, 200), (43, 120), (200, 120)):
        img.alpha_composite(post, (cx - post.width // 2, cy - post.height // 2))


def draw_variant_details(img: Image.Image, draw: ImageDraw.ImageDraw, variant: str) -> None:
    if variant == "snow":
        for box in ((20, 22, 72, 52), (169, 188, 219, 213), (36, 178, 83, 205), (160, 25, 210, 51)):
            draw.ellipse(box, fill=(239, 247, 247, 120))
    elif variant == "desert":
        rng = random.Random(901)
        for _ in range(28):
            x = rng.randrange(25, 215)
            y = rng.randrange(25, 215)
            draw.ellipse((x - 2, y - 1, x + 2, y + 1), fill=(130, 91, 58, 120))
    elif variant == "marsh":
        for box in ((18, 75, 62, 112), (174, 132, 223, 174), (33, 165, 79, 204)):
            draw.ellipse(box, fill=(43, 91, 87, 150), outline=(84, 134, 99, 140), width=2)
        for x, y in ((33, 87), (197, 151), (56, 184)):
            for i in range(8):
                draw.line((x + i * 4, y + 18, x + i * 4 + 2, y + 3), fill=(83, 134, 74, 190), width=1)
    else:
        for box in ((25, 35, 62, 54), (180, 40, 216, 58), (26, 181, 70, 205), (169, 179, 214, 203)):
            draw.ellipse(box, fill=(82, 136, 61, 130))


def make_cluster(assets: dict[str, list[Image.Image]], variant: str) -> Image.Image:
    rng = random.Random({"green": 41, "snow": 43, "desert": 47, "marsh": 53}[variant])
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")
    skirt = [
        (55, 11),
        (180, 13),
        (224, 50),
        (223, 184),
        (182, 224),
        (57, 222),
        (15, 184),
        (17, 51),
    ]
    ground = {
        "green": (105, 151, 75, 236),
        "snow": (209, 226, 226, 236),
        "desert": (190, 145, 82, 238),
        "marsh": (69, 112, 76, 236),
    }[variant]
    village_floor = {
        "green": (186, 166, 100, 230),
        "snow": (180, 193, 189, 230),
        "desert": (202, 158, 91, 230),
        "marsh": (121, 126, 77, 230),
    }[variant]
    draw_textured_polygon(draw, skirt, ground, rng.randrange(10_000))
    draw_textured_polygon(
        draw,
        [(43, 39), (196, 39), (205, 63), (205, 183), (183, 205), (56, 205), (35, 182), (35, 62)],
        village_floor,
        rng.randrange(10_000),
    )
    draw_variant_details(img, draw, variant)
    draw_roads(draw, variant)
    draw_fences(img, draw, variant, assets["fences"])

    houses = assets["houses"] or assets["barns"]
    props = assets["props"]
    placements = [
        (75, 86, 54, 0),
        (160, 86, 52, 1),
        (76, 169, 50, 2),
        (162, 170, 52, 3),
        (119, 133, 48, 4),
        (121, 64, 42, 5),
    ]
    for cx, foot_y, height, index in placements:
        if not houses:
            continue
        sprite = fit_height(houses[index % len(houses)], height)
        if variant == "snow":
            sprite = tint(sprite, (10, 12, 16), saturation=0.82, brightness=1.07)
            cap = Image.new("RGBA", sprite.size, (0, 0, 0, 0))
            cd = ImageDraw.Draw(cap, "RGBA")
            cd.polygon([(5, int(sprite.height * 0.14)), (sprite.width // 2, 0), (sprite.width - 5, int(sprite.height * 0.14)), (sprite.width - 8, int(sprite.height * 0.23)), (8, int(sprite.height * 0.23))], fill=(245, 250, 249, 145))
            sprite.alpha_composite(cap)
        elif variant == "desert":
            sprite = tint(sprite, (22, 8, -10), saturation=0.82, brightness=1.02)
        elif variant == "marsh":
            sprite = tint(sprite, (-14, -2, -6), saturation=0.88, brightness=0.90)
        paste_anchor(img, sprite, cx + rng.randrange(-3, 4), foot_y + rng.randrange(-2, 3))

    prop_positions = [(52, 119, 18), (188, 119, 18), (116, 188, 18), (117, 92, 16), (89, 123, 14), (150, 124, 14)]
    for i, (cx, foot_y, height) in enumerate(prop_positions):
        if not props:
            continue
        sprite = fit_height(props[(i * 3 + rng.randrange(3)) % len(props)], height)
        if variant == "snow":
            sprite = tint(sprite, (8, 12, 14), saturation=0.78, brightness=1.05)
        elif variant == "desert":
            sprite = tint(sprite, (18, 6, -10), saturation=0.88)
        elif variant == "marsh":
            sprite = tint(sprite, (-18, -5, -4), saturation=0.84, brightness=0.92)
        paste_anchor(img, sprite, cx, foot_y)

    if variant in {"green", "marsh"}:
        for tx, ty, scale in ((31, 55, 28), (211, 65, 26), (42, 210, 24), (204, 207, 25)):
            if assets["trees"]:
                tree = fit_height(assets["trees"][(tx + ty) % len(assets["trees"])], scale)
                if variant == "marsh":
                    tree = tint(tree, (-12, 4, -6), saturation=0.90, brightness=0.92)
                paste_anchor(img, tree, tx, ty)

    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow, "RGBA")
    for cx, cy in ((80, 98), (158, 100), (119, 145), (160, 181)):
        gd.ellipse((cx - 3, cy - 4, cx + 3, cy + 4), fill=(241, 184, 82, 80))
    img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(1.2)))
    return img


def main() -> None:
    if not VILLAGE_SHEET.exists():
        raise FileNotFoundError(f"Missing source sheet: {VILLAGE_SHEET}")
    sheet = Image.open(VILLAGE_SHEET).convert("RGBA")
    assets = pick_village_assets(sheet)
    CITY_DIR.mkdir(parents=True, exist_ok=True)
    for variant in ("green", "snow", "desert", "marsh"):
        cluster = make_cluster(assets, variant)
        cluster.save(CITY_DIR / f"city_overworld_village_{variant}.png")
    make_cluster(assets, "green").save(CITY_DIR / "city_overworld_village.png")


if __name__ == "__main__":
    main()
