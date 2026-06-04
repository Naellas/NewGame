from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


OUT_DIR = Path("assets") / "locations" / "quest"
SIZE = 64


def canvas() -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    return image, ImageDraw.Draw(image)


def shadow(draw: ImageDraw.ImageDraw) -> None:
    draw.ellipse((13, 47, 51, 57), fill=(20, 18, 15, 72))


def outline_rect(draw: ImageDraw.ImageDraw, box, fill, outline=(55, 42, 29, 255), width=2) -> None:
    draw.rounded_rectangle(box, radius=3, fill=outline)
    inner = (box[0] + width, box[1] + width, box[2] - width, box[3] - width)
    draw.rounded_rectangle(inner, radius=2, fill=fill)


def save(name: str, image: Image.Image) -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    image.save(OUT_DIR / f"{name}.png")


def broken_road_signs() -> None:
    image, draw = canvas()
    shadow(draw)
    draw.line((31, 18, 28, 50), fill=(78, 50, 27, 255), width=5)
    draw.line((33, 19, 30, 50), fill=(142, 91, 43, 255), width=2)
    outline_rect(draw, (14, 16, 44, 27), (176, 115, 52, 255))
    draw.polygon([(39, 16), (51, 22), (39, 28)], fill=(60, 44, 29, 255))
    draw.polygon([(40, 18), (48, 22), (40, 26)], fill=(180, 119, 54, 255))
    draw.line((17, 19, 37, 24), fill=(90, 50, 34, 255), width=2)
    draw.line((16, 27, 28, 36), fill=(79, 48, 29, 255), width=4)
    draw.line((17, 28, 27, 35), fill=(170, 105, 48, 255), width=2)
    draw.polygon([(29, 35), (43, 39), (30, 44)], fill=(69, 47, 32, 255))
    draw.polygon([(31, 36), (40, 39), (31, 42)], fill=(168, 103, 48, 255))
    save("quest_broken_road_signs", image)


def roadwatch_warning_marks() -> None:
    image, draw = canvas()
    shadow(draw)
    draw.line((18, 43, 31, 27, 46, 43), fill=(82, 53, 31, 255), width=3)
    draw.arc((17, 29, 47, 57), 200, 340, fill=(126, 93, 58, 255), width=3)
    draw.line((31, 15, 31, 48), fill=(76, 55, 35, 255), width=5)
    draw.line((33, 16, 33, 48), fill=(132, 95, 55, 255), width=2)
    draw.line((24, 25, 39, 25), fill=(64, 44, 30, 255), width=4)
    draw.line((25, 26, 38, 26), fill=(166, 119, 62, 255), width=2)
    draw.line((26, 18, 36, 29), fill=(191, 57, 48, 255), width=3)
    draw.line((36, 18, 26, 29), fill=(191, 57, 48, 255), width=3)
    save("quest_roadwatch_warning_marks", image)


def herbs() -> None:
    image, draw = canvas()
    shadow(draw)
    for x, y, color in [
        (22, 35, (74, 151, 75, 255)),
        (31, 31, (91, 184, 94, 255)),
        (40, 36, (61, 135, 78, 255)),
        (28, 42, (104, 188, 112, 255)),
    ]:
        draw.line((32, 49, x, y), fill=(45, 92, 45, 255), width=2)
        draw.ellipse((x - 6, y - 4, x + 6, y + 4), fill=color, outline=(31, 77, 39, 255))
    draw.rectangle((19, 47, 45, 52), fill=(91, 58, 30, 255))
    draw.line((21, 48, 43, 50), fill=(184, 139, 67, 255), width=2)
    save("quest_salve_herbs", image)


def ward_marker() -> None:
    image, draw = canvas()
    shadow(draw)
    draw.polygon([(32, 10), (43, 25), (39, 50), (25, 50), (21, 25)], fill=(53, 54, 57, 255))
    draw.polygon([(32, 14), (39, 26), (36, 46), (28, 46), (25, 26)], fill=(112, 116, 116, 255))
    draw.line((32, 20, 32, 40), fill=(78, 172, 161, 255), width=2)
    draw.arc((24, 24, 40, 39), 25, 335, fill=(78, 172, 161, 255), width=2)
    draw.ellipse((29, 30, 35, 36), fill=(158, 228, 209, 255))
    save("quest_ward_marker", image)


def document_bundle() -> None:
    image, draw = canvas()
    shadow(draw)
    draw.polygon([(17, 19), (43, 14), (50, 44), (24, 50)], fill=(78, 57, 44, 255))
    draw.polygon([(18, 18), (42, 14), (48, 42), (24, 48)], fill=(211, 185, 128, 255))
    draw.polygon([(13, 25), (39, 21), (46, 48), (20, 53)], fill=(236, 214, 161, 255), outline=(108, 78, 49, 255))
    for y in (31, 36, 41):
        draw.line((20, y, 38, y - 3), fill=(102, 77, 54, 255), width=1)
    draw.line((15, 42, 45, 30), fill=(134, 39, 44, 255), width=3)
    save("quest_document_bundle", image)


def medical_supplies() -> None:
    image, draw = canvas()
    shadow(draw)
    outline_rect(draw, (17, 28, 48, 50), (159, 104, 67, 255))
    draw.rectangle((20, 23, 44, 31), fill=(71, 47, 31, 255))
    draw.rectangle((22, 24, 42, 30), fill=(184, 145, 91, 255))
    draw.rectangle((29, 32, 36, 46), fill=(236, 225, 190, 255))
    draw.rectangle((25, 36, 40, 42), fill=(236, 225, 190, 255))
    draw.line((23, 29, 41, 49), fill=(209, 74, 69, 255), width=2)
    save("quest_medical_supplies", image)


def water_skins() -> None:
    image, draw = canvas()
    shadow(draw)
    draw.ellipse((17, 25, 37, 52), fill=(119, 73, 42, 255), outline=(58, 38, 25, 255), width=2)
    draw.ellipse((30, 22, 50, 50), fill=(143, 85, 45, 255), outline=(58, 38, 25, 255), width=2)
    draw.rectangle((24, 19, 31, 28), fill=(72, 43, 25, 255))
    draw.rectangle((37, 17, 44, 26), fill=(72, 43, 25, 255))
    draw.arc((19, 18, 49, 38), 190, 345, fill=(195, 148, 82, 255), width=2)
    draw.ellipse((36, 33, 42, 39), fill=(95, 157, 189, 210))
    save("quest_water_skins", image)


def firewood() -> None:
    image, draw = canvas()
    shadow(draw)
    for y, color in [(43, (114, 72, 38, 255)), (35, (142, 88, 43, 255)), (27, (103, 66, 38, 255))]:
        draw.rounded_rectangle((14, y, 49, y + 8), radius=4, fill=(56, 38, 25, 255))
        draw.rounded_rectangle((16, y + 1, 47, y + 7), radius=4, fill=color)
        draw.ellipse((15, y + 1, 23, y + 7), fill=(185, 137, 72, 255), outline=(75, 49, 30, 255))
    draw.line((18, 31, 48, 48), fill=(176, 46, 43, 255), width=2)
    save("quest_firewood_bundle", image)


def stones() -> None:
    image, draw = canvas()
    shadow(draw)
    rocks = [
        (15, 39, 30, 52, (105, 102, 91, 255)),
        (25, 31, 42, 50, (128, 124, 108, 255)),
        (38, 38, 52, 52, (92, 91, 85, 255)),
        (20, 24, 34, 38, (145, 139, 119, 255)),
    ]
    for x1, y1, x2, y2, color in rocks:
        draw.rounded_rectangle((x1, y1, x2, y2), radius=4, fill=(54, 53, 50, 255))
        draw.rounded_rectangle((x1 + 2, y1 + 2, x2 - 1, y2 - 1), radius=3, fill=color)
    save("quest_causeway_stones", image)


def glass_shards() -> None:
    image, draw = canvas()
    shadow(draw)
    shards = [
        [(22, 47), (30, 18), (35, 46)],
        [(34, 49), (48, 26), (46, 51)],
        [(15, 50), (22, 33), (27, 52)],
    ]
    for poly in shards:
        draw.polygon(poly, fill=(64, 180, 190, 210), outline=(181, 247, 244, 255))
    draw.line((28, 24, 32, 44), fill=(238, 255, 255, 255), width=1)
    draw.line((41, 31, 45, 48), fill=(238, 255, 255, 255), width=1)
    save("quest_glasssteel_shards", image)


def lamp_supplies() -> None:
    image, draw = canvas()
    shadow(draw)
    draw.ellipse((20, 35, 42, 54), fill=(102, 67, 36, 255), outline=(52, 36, 24, 255), width=2)
    draw.rectangle((25, 24, 37, 40), fill=(184, 133, 62, 255), outline=(61, 42, 24, 255))
    draw.polygon([(31, 10), (37, 24), (25, 24)], fill=(248, 208, 83, 230), outline=(142, 91, 35, 255))
    draw.line((15, 47, 50, 35), fill=(218, 212, 184, 255), width=3)
    draw.line((16, 50, 51, 38), fill=(89, 66, 45, 255), width=1)
    save("quest_lamp_supplies", image)


def foxglove() -> None:
    image, draw = canvas()
    shadow(draw)
    draw.line((32, 51, 31, 18), fill=(48, 110, 54, 255), width=3)
    for x, y in [(26, 26), (37, 29), (25, 36), (39, 40)]:
        draw.ellipse((x - 5, y - 5, x + 5, y + 5), fill=(178, 80, 142, 255), outline=(86, 41, 90, 255))
        draw.ellipse((x - 1, y - 1, x + 2, y + 2), fill=(237, 178, 218, 255))
    draw.rectangle((18, 48, 46, 53), fill=(88, 55, 32, 255))
    draw.line((21, 49, 42, 51), fill=(184, 123, 61, 255), width=2)
    save("quest_foxglove_markers", image)


def inspection_cache() -> None:
    image, draw = canvas()
    shadow(draw)
    outline_rect(draw, (16, 29, 49, 51), (128, 84, 48, 255))
    draw.rectangle((18, 25, 47, 33), fill=(83, 56, 34, 255))
    draw.rectangle((20, 26, 45, 31), fill=(169, 113, 58, 255))
    draw.ellipse((28, 35, 37, 44), fill=(88, 100, 107, 255), outline=(43, 47, 51, 255))
    draw.line((20, 43, 46, 33), fill=(206, 196, 142, 255), width=2)
    save("quest_inspection_cache", image)


def supply_cache() -> None:
    image, draw = canvas()
    shadow(draw)
    outline_rect(draw, (15, 31, 33, 51), (143, 95, 52, 255))
    outline_rect(draw, (32, 26, 50, 51), (111, 78, 49, 255))
    draw.line((16, 39, 32, 39), fill=(222, 176, 86, 255), width=2)
    draw.line((34, 36, 49, 36), fill=(204, 154, 78, 255), width=2)
    draw.rectangle((22, 24, 39, 30), fill=(216, 205, 154, 255), outline=(91, 73, 46, 255))
    save("quest_supply_cache", image)


def main() -> None:
    broken_road_signs()
    roadwatch_warning_marks()
    herbs()
    ward_marker()
    document_bundle()
    medical_supplies()
    water_skins()
    firewood()
    stones()
    glass_shards()
    lamp_supplies()
    foxglove()
    inspection_cache()
    supply_cache()
    print(f"Wrote quest objective assets to {OUT_DIR}")


if __name__ == "__main__":
    main()
