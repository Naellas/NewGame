from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "assets" / "source"
CITY_DIR = ROOT / "assets" / "city"


def transparent_light_checker(img: Image.Image) -> Image.Image:
    out = img.convert("RGBA")
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if r > 212 and g > 212 and b > 212 and max(r, g, b) - min(r, g, b) < 12:
                pixels[x, y] = (r, g, b, 0)
    return trim(out)


def transparent_dark_sheet(img: Image.Image) -> Image.Image:
    out = img.convert("RGBA")
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if r < 18 and g < 34 and b < 48:
                pixels[x, y] = (r, g, b, 0)
    return trim(out)


def transparent_green_key(img: Image.Image) -> Image.Image:
    out = img.convert("RGBA")
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if g > 150 and r < 95 and b < 95 and g - max(r, b) > 70:
                pixels[x, y] = (r, g, b, 0)
    return trim(keep_largest_alpha_component(out))


def keep_largest_alpha_component(img: Image.Image) -> Image.Image:
    alpha = img.getchannel("A")
    pixels = alpha.load()
    width, height = img.size
    visited = bytearray(width * height)
    best: list[tuple[int, int]] = []
    for y in range(height):
        for x in range(width):
            index = y * width + x
            if visited[index] or pixels[x, y] == 0:
                continue
            stack = [(x, y)]
            visited[index] = 1
            component: list[tuple[int, int]] = []
            while stack:
                cx, cy = stack.pop()
                component.append((cx, cy))
                for ny in range(max(0, cy - 1), min(height, cy + 2)):
                    for nx in range(max(0, cx - 1), min(width, cx + 2)):
                        next_index = ny * width + nx
                        if not visited[next_index] and pixels[nx, ny] > 0:
                            visited[next_index] = 1
                            stack.append((nx, ny))
            if len(component) > len(best):
                best = component
    if not best:
        return img
    keep = {point for point in best}
    out = img.copy()
    out_pixels = out.load()
    for y in range(height):
        for x in range(width):
            if pixels[x, y] > 0 and (x, y) not in keep:
                r, g, b, a = out_pixels[x, y]
                out_pixels[x, y] = (r, g, b, 0)
    return out


def trim(img: Image.Image, pad: int = 8) -> Image.Image:
    alpha = img.getchannel("A")
    box = alpha.getbbox()
    if box is None:
        return img
    left = max(0, box[0] - pad)
    top = max(0, box[1] - pad)
    right = min(img.width, box[2] + pad)
    bottom = min(img.height, box[3] + pad)
    return canvas_pad(img.crop((left, top, right, bottom)), 18)


def canvas_pad(img: Image.Image, pad: int) -> Image.Image:
    out = Image.new("RGBA", (img.width + pad * 2, img.height + pad * 2), (0, 0, 0, 0))
    out.alpha_composite(img, (pad, pad))
    return out


def crop_cell(sheet: Image.Image, col: int, row: int, cols: int, rows: int) -> Image.Image:
    x1 = round(col * sheet.width / cols)
    y1 = round(row * sheet.height / rows)
    x2 = round((col + 1) * sheet.width / cols)
    y2 = round((row + 1) * sheet.height / rows)
    return sheet.crop((x1, y1, x2, y2))


def crop_cell_bleed(sheet: Image.Image, col: int, row: int, cols: int, rows: int, bleed: int) -> Image.Image:
    x1 = round(col * sheet.width / cols)
    y1 = round(row * sheet.height / rows)
    x2 = round((col + 1) * sheet.width / cols)
    y2 = round((row + 1) * sheet.height / rows)
    return sheet.crop((max(0, x1 - bleed), max(0, y1 - bleed), min(sheet.width, x2 + bleed), min(sheet.height, y2 + bleed)))


def save(img: Image.Image, name: str) -> None:
    CITY_DIR.mkdir(parents=True, exist_ok=True)
    img.save(CITY_DIR / f"{name}.png")


def slice_sheet(path: Path, names: list[str], cols: int, rows: int, transparent=transparent_light_checker, bleed: int = 0) -> None:
    sheet = Image.open(path)
    for index, name in enumerate(names):
        if bleed > 0:
            cell = crop_cell_bleed(sheet, index % cols, index // cols, cols, rows, bleed)
        else:
            cell = crop_cell(sheet, index % cols, index // cols, cols, rows)
        save(transparent(cell), name)


def level_building_names(level: int) -> list[str]:
    suffix = f"_lvl{level}"
    return [
        "imagegen_city_cottage" + suffix,
        "village_building_warehouse" + suffix,
        "village_building_forestry_hut" + suffix,
        "village_building_mine" + suffix,
        "village_building_hunting_camp" + suffix,
        "village_building_farmstead" + suffix,
        "village_building_workshop" + suffix,
        "imagegen_city_hall" + suffix,
        "imagegen_city_study" + suffix,
        "imagegen_city_row" + suffix,
        "village_building_granary" + suffix,
        "village_building_watchtower" + suffix,
        "village_building_shrine" + suffix,
        "village_building_garden" + suffix,
        "village_building_blacksmith" + suffix,
        "village_building_bakery" + suffix,
        "village_building_apothecary" + suffix,
        "village_building_fishing_hut" + suffix,
    ]


def slice_city_exteriors() -> None:
    sheet = Image.open(SOURCE_DIR / "imagegen-overworld-city-exteriors-nosnow.png")
    picks = {
        "imagegen_city_cottage": (917, 667, 971, 743),
        "imagegen_city_workshop": (776, 762, 860, 829),
        "imagegen_city_hall": (26, 831, 174, 897),
        "imagegen_city_study": (1435, 844, 1508, 911),
        "imagegen_city_row": (477, 831, 601, 897),
        "imagegen_city_warehouse": (350, 830, 460, 897),
    }
    for name, box in picks.items():
        save(transparent_dark_sheet(sheet.crop(box)), name)


def main() -> None:
    slice_sheet(
        SOURCE_DIR / "imagegen-village-buildings.png",
        [
            "village_building_blacksmith",
            "village_building_bakery",
            "village_building_apothecary",
            "village_building_fishing_hut",
        ],
        4,
        1,
    )
    slice_sheet(
        SOURCE_DIR / "imagegen-village-resource-buildings.png",
        [
            "village_building_warehouse",
            "village_building_mine",
            "village_building_forestry_hut",
            "village_building_hunting_camp",
            "village_building_farmstead",
            "village_building_granary",
            "village_building_watchtower",
            "village_building_shrine",
        ],
        4,
        2,
    )
    slice_sheet(
        SOURCE_DIR / "imagegen-village-props.png",
        [
            "village_prop_anvil_stump",
            "village_prop_sawhorse",
            "village_prop_chopping_block",
            "village_prop_ore_pile",
            "village_prop_mine_lantern",
            "village_prop_chicken_coop",
            "village_prop_fish_rack",
            "village_prop_herb_barrel",
            "village_prop_log_stack",
            "village_prop_grain_sacks",
            "village_prop_water_trough",
            "village_prop_fence_segment",
        ],
        4,
        3,
    )
    slice_city_exteriors()
    slice_sheet(
        SOURCE_DIR / "imagegen-village-decor-buildings-props.png",
        [
            "village_building_workshop",
            "village_building_garden",
            "imagegen_city_row",
            "imagegen_city_cottage",
            "village_prop_well",
            "village_prop_notice_board",
            "village_prop_tool_rack",
            "village_prop_beehive",
            "village_prop_training_dummy",
            "village_prop_drying_rack",
            "village_prop_hay_stack",
            "village_prop_hay_bales",
            "village_prop_produce_basket",
            "village_prop_bench",
            "village_prop_farm_tools",
            "village_prop_woodpile",
        ],
        4,
        4,
    )
    slice_sheet(
        SOURCE_DIR / "imagegen-village-buildings-lvl2.png",
        level_building_names(2),
        6,
        3,
        transparent_green_key,
        bleed=56,
    )
    slice_sheet(
        SOURCE_DIR / "imagegen-village-buildings-lvl3.png",
        level_building_names(3),
        6,
        3,
        transparent_green_key,
        bleed=56,
    )


if __name__ == "__main__":
    main()
