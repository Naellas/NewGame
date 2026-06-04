from __future__ import annotations

import struct
import zlib
from pathlib import Path


SIZE = 64
OUT = Path("assets")
TRANSPARENT = (0, 0, 0, 0)
Color = tuple[int, int, int, int]
Pixels = list[list[Color]]


def write_png(path: Path, pixels: Pixels) -> None:
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for color in row:
            raw.extend(color)

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )


def blank() -> Pixels:
    return [[TRANSPARENT for _ in range(SIZE)] for _ in range(SIZE)]


def blend(dst: Color, src: Color) -> Color:
    sr, sg, sb, sa = src
    if sa == 0:
        return dst
    if sa == 255:
        return src
    dr, dg, db, da = dst
    alpha = sa / 255
    out_a = sa + da * (1 - alpha)
    if out_a <= 0:
        return TRANSPARENT
    return (
        int((sr * sa + dr * da * (1 - alpha)) / out_a),
        int((sg * sa + dg * da * (1 - alpha)) / out_a),
        int((sb * sa + db * da * (1 - alpha)) / out_a),
        int(out_a),
    )


def put(pixels: Pixels, x: int, y: int, color: Color) -> None:
    if 0 <= x < SIZE and 0 <= y < SIZE:
        pixels[y][x] = blend(pixels[y][x], color)


def rect(pixels: Pixels, x1: int, y1: int, x2: int, y2: int, color: Color) -> None:
    for y in range(max(0, y1), min(SIZE, y2 + 1)):
        for x in range(max(0, x1), min(SIZE, x2 + 1)):
            put(pixels, x, y, color)


def ellipse(pixels: Pixels, cx: int, cy: int, rx: int, ry: int, color: Color) -> None:
    for y in range(cy - ry, cy + ry + 1):
        for x in range(cx - rx, cx + rx + 1):
            if ((x - cx) / max(1, rx)) ** 2 + ((y - cy) / max(1, ry)) ** 2 <= 1:
                put(pixels, x, y, color)


def line(pixels: Pixels, x1: int, y1: int, x2: int, y2: int, color: Color, width: int = 1) -> None:
    steps = max(abs(x2 - x1), abs(y2 - y1), 1)
    radius = width // 2
    for i in range(steps + 1):
        t = i / steps
        x = round(x1 + (x2 - x1) * t)
        y = round(y1 + (y2 - y1) * t)
        for oy in range(-radius, radius + 1):
            for ox in range(-radius, radius + 1):
                put(pixels, x + ox, y + oy, color)


def polygon(pixels: Pixels, points: list[tuple[int, int]], color: Color) -> None:
    min_x = max(0, min(x for x, _ in points))
    max_x = min(SIZE - 1, max(x for x, _ in points))
    min_y = max(0, min(y for _, y in points))
    max_y = min(SIZE - 1, max(y for _, y in points))
    for y in range(min_y, max_y + 1):
        for x in range(min_x, max_x + 1):
            inside = False
            j = len(points) - 1
            for i, (xi, yi) in enumerate(points):
                xj, yj = points[j]
                if (yi > y) != (yj > y):
                    x_at_y = (xj - xi) * (y - yi) / max(1, yj - yi) + xi
                    if x < x_at_y:
                        inside = not inside
                j = i
            if inside:
                put(pixels, x, y, color)


def sparkle(pixels: Pixels, points: list[tuple[int, int]], color: Color) -> None:
    for x, y in points:
        put(pixels, x, y, color)
        put(pixels, x + 1, y, color)
        put(pixels, x, y + 1, color)


def grass_wildflowers() -> Pixels:
    p = blank()
    ellipse(p, 32, 52, 23, 5, (35, 91, 38, 180))
    for x, h, lean in ((15, 15, -3), (20, 22, 4), (27, 18, -2), (34, 24, 3), (42, 17, -4), (49, 21, 2)):
        line(p, x, 54, x + lean, 54 - h, (52, 130, 54, 255), 2)
        line(p, x + 2, 54, x + 3 + lean, 48 - h // 2, (104, 171, 70, 255), 1)
    for x, y, c in ((18, 37, (245, 220, 93, 255)), (26, 42, (235, 120, 144, 255)), (35, 32, (236, 236, 248, 255)), (44, 39, (183, 126, 224, 255)), (50, 34, (248, 170, 88, 255))):
        ellipse(p, x, y, 3, 2, c)
        put(p, x, y, (255, 250, 188, 255))
    return p


def grass_herb_patch() -> Pixels:
    p = blank()
    ellipse(p, 31, 51, 20, 5, (39, 94, 39, 170))
    for cx, cy in ((21, 43), (30, 37), (39, 44), (45, 36), (26, 50)):
        line(p, cx, 54, cx, cy, (42, 118, 57, 255), 2)
        ellipse(p, cx - 4, cy + 2, 5, 2, (78, 153, 68, 255))
        ellipse(p, cx + 4, cy - 1, 5, 2, (99, 174, 73, 255))
        put(p, cx, cy - 4, (205, 229, 131, 255))
    sparkle(p, [(19, 48), (34, 45), (43, 50)], (133, 193, 87, 255))
    return p


def forest_blue_mushroom_ring() -> Pixels:
    p = blank()
    ellipse(p, 32, 51, 19, 4, (18, 54, 32, 180))
    for x, y, s in ((18, 45, 4), (25, 39, 5), (34, 42, 6), (43, 37, 5), (50, 47, 4)):
        rect(p, x - 1, y, x + 1, 52, (221, 211, 176, 255))
        ellipse(p, x, y, s, max(2, s - 2), (74, 109, 190, 255))
        rect(p, x - s, y, x + s, y + 2, (57, 83, 152, 255))
        put(p, x - 2, y - 1, (190, 218, 255, 255))
        put(p, x + 3, y + 1, (190, 218, 255, 255))
    for x in (16, 22, 31, 45, 53):
        line(p, x, 53, x + 2, 47, (56, 130, 55, 255), 1)
    return p


def forest_shrine_stone() -> Pixels:
    p = blank()
    ellipse(p, 32, 53, 17, 4, (16, 51, 31, 180))
    polygon(p, [(24, 47), (27, 23), (39, 20), (43, 48)], (73, 83, 78, 255))
    polygon(p, [(32, 22), (39, 20), (43, 48), (34, 46)], (51, 62, 60, 255))
    line(p, 27, 31, 38, 29, (134, 146, 127, 255), 1)
    line(p, 27, 39, 40, 38, (42, 50, 47, 255), 1)
    polygon(p, [(30, 34), (34, 28), (38, 34), (34, 40)], (58, 131, 82, 255))
    ellipse(p, 24, 45, 8, 3, (47, 119, 48, 255))
    ellipse(p, 41, 47, 7, 3, (69, 139, 55, 255))
    sparkle(p, [(34, 31), (31, 37), (39, 42)], (121, 184, 102, 255))
    return p


def desert_blooming_cactus() -> Pixels:
    p = blank()
    ellipse(p, 31, 54, 16, 4, (126, 86, 47, 170))
    rect(p, 28, 19, 35, 53, (58, 133, 78, 255))
    rect(p, 31, 18, 33, 53, (103, 180, 95, 255))
    rect(p, 17, 33, 25, 39, (58, 133, 78, 255))
    rect(p, 20, 27, 25, 36, (58, 133, 78, 255))
    rect(p, 37, 38, 47, 44, (58, 133, 78, 255))
    rect(p, 37, 33, 42, 41, (58, 133, 78, 255))
    for x in (29, 34, 21, 39):
        for y in range(23, 50, 8):
            put(p, x, y, (218, 232, 166, 255))
    for x, y in ((31, 16), (21, 26), (42, 32)):
        ellipse(p, x, y, 4, 3, (236, 82, 116, 255))
        put(p, x, y, (255, 221, 112, 255))
    return p


def desert_sun_bleached_bones() -> Pixels:
    p = blank()
    ellipse(p, 33, 53, 22, 4, (138, 93, 48, 140))
    line(p, 19, 43, 45, 34, (230, 217, 180, 255), 4)
    ellipse(p, 17, 44, 5, 4, (239, 229, 195, 255))
    ellipse(p, 47, 33, 5, 4, (216, 199, 158, 255))
    ellipse(p, 37, 47, 10, 7, (225, 211, 173, 255))
    ellipse(p, 40, 46, 3, 2, (91, 69, 54, 255))
    line(p, 11, 52, 27, 49, (169, 119, 64, 255), 1)
    line(p, 43, 51, 55, 47, (223, 184, 99, 255), 1)
    return p


def tundra_ice_crystals() -> Pixels:
    p = blank()
    ellipse(p, 32, 53, 18, 4, (137, 174, 186, 120))
    for pts, fill, edge in (
        ([(22, 52), (27, 23), (33, 52)], (139, 218, 238, 220), (232, 252, 255, 255)),
        ([(31, 53), (39, 14), (47, 53)], (95, 181, 219, 230), (219, 248, 255, 255)),
        ([(42, 52), (48, 28), (54, 52)], (123, 205, 230, 210), (234, 252, 255, 255)),
    ):
        polygon(p, pts, fill)
        line(p, pts[0][0], pts[0][1], pts[1][0], pts[1][1], edge, 1)
    sparkle(p, [(31, 30), (39, 20), (45, 38), (52, 47)], (255, 255, 255, 255))
    return p


def tundra_frost_bush() -> Pixels:
    p = blank()
    ellipse(p, 31, 53, 18, 4, (132, 158, 150, 130))
    for x, y in ((19, 45), (26, 38), (35, 42), (44, 36), (49, 47)):
        line(p, 32, 53, x, y, (72, 103, 86, 255), 2)
        line(p, x, y, x - 4, y + 2, (228, 244, 247, 255), 1)
        line(p, x, y, x + 4, y - 1, (245, 252, 255, 255), 1)
    ellipse(p, 29, 48, 15, 5, (214, 232, 234, 200))
    sparkle(p, [(24, 42), (37, 39), (46, 34)], (255, 255, 255, 255))
    return p


def marsh_lily_pool() -> Pixels:
    p = blank()
    ellipse(p, 32, 48, 22, 10, (42, 93, 91, 210))
    ellipse(p, 36, 45, 15, 6, (69, 135, 111, 170))
    for cx, cy, rx, ry in ((20, 44, 7, 4), (31, 51, 8, 4), (44, 42, 7, 4)):
        ellipse(p, cx, cy, rx, ry, (54, 132, 75, 255))
        line(p, cx, cy, cx + rx - 1, cy - 1, (29, 92, 60, 255), 1)
    ellipse(p, 42, 39, 4, 3, (238, 220, 236, 255))
    put(p, 42, 39, (255, 238, 130, 255))
    line(p, 17, 51, 27, 49, (133, 200, 177, 255), 1)
    line(p, 36, 42, 51, 43, (133, 200, 177, 255), 1)
    return p


def marsh_twisted_roots() -> Pixels:
    p = blank()
    ellipse(p, 32, 53, 22, 4, (35, 77, 55, 160))
    for x1, y1, x2, y2 in ((18, 48, 33, 34), (29, 52, 37, 29), (41, 50, 33, 35), (20, 54, 45, 45)):
        line(p, x1, y1, x2, y2, (73, 55, 37, 255), 3)
        line(p, x1 + 1, y1 - 1, x2 + 1, y2, (122, 83, 48, 255), 1)
    for x in (17, 24, 43, 49):
        line(p, x, 54, x + 2, 35, (47, 110, 61, 255), 1)
        rect(p, x, 31, x + 3, 36, (117, 86, 48, 255))
    ellipse(p, 35, 40, 8, 4, (42, 105, 69, 210))
    return p


def badlands_red_spire() -> Pixels:
    p = blank()
    ellipse(p, 32, 54, 18, 4, (83, 54, 42, 160))
    polygon(p, [(22, 52), (31, 15), (39, 52)], (122, 73, 55, 255))
    polygon(p, [(31, 15), (39, 52), (46, 52), (35, 24)], (167, 96, 61, 255))
    polygon(p, [(19, 52), (25, 33), (31, 52)], (91, 62, 52, 255))
    line(p, 28, 29, 36, 26, (205, 137, 76, 255), 1)
    line(p, 24, 43, 41, 41, (87, 55, 43, 255), 2)
    line(p, 13, 55, 25, 51, (188, 125, 72, 255), 1)
    return p


def badlands_skull_marker() -> Pixels:
    p = blank()
    ellipse(p, 33, 54, 20, 4, (82, 54, 42, 150))
    ellipse(p, 31, 39, 13, 10, (218, 197, 157, 255))
    rect(p, 25, 45, 37, 53, (204, 180, 139, 255))
    ellipse(p, 27, 38, 3, 3, (60, 45, 40, 255))
    ellipse(p, 36, 38, 3, 3, (60, 45, 40, 255))
    polygon(p, [(31, 41), (28, 47), (34, 47)], (96, 72, 56, 255))
    for x in (26, 31, 36):
        line(p, x, 47, x, 52, (116, 92, 70, 255), 1)
    line(p, 14, 51, 50, 46, (113, 74, 54, 255), 2)
    sparkle(p, [(22, 34), (40, 34)], (239, 224, 183, 255))
    return p


def mountain_crystal_cluster() -> Pixels:
    p = blank()
    ellipse(p, 33, 54, 18, 4, (47, 54, 54, 170))
    for pts, fill in (
        ([(20, 53), (25, 30), (32, 53)], (105, 180, 204, 235)),
        ([(29, 54), (37, 16), (45, 54)], (82, 148, 197, 245)),
        ([(42, 53), (49, 32), (55, 53)], (126, 199, 215, 225)),
    ):
        polygon(p, pts, fill)
        line(p, pts[1][0], pts[1][1], pts[0][0], pts[0][1], (232, 251, 255, 255), 1)
    ellipse(p, 28, 51, 12, 5, (91, 96, 88, 255))
    ellipse(p, 42, 52, 10, 4, (67, 76, 75, 255))
    sparkle(p, [(37, 25), (42, 38), (50, 45), (25, 39)], (255, 255, 255, 255))
    return p


def mountain_scrub_pine() -> Pixels:
    p = blank()
    ellipse(p, 33, 55, 18, 4, (45, 56, 50, 170))
    rect(p, 31, 33, 36, 55, (91, 64, 43, 255))
    line(p, 33, 41, 21, 34, (75, 56, 39, 255), 2)
    line(p, 34, 39, 47, 31, (75, 56, 39, 255), 2)
    for cx, cy, rx, ry in ((22, 33, 9, 5), (47, 31, 10, 6), (30, 25, 11, 7), (40, 43, 9, 5), (24, 45, 8, 4)):
        ellipse(p, cx, cy, rx, ry, (45, 99, 65, 255))
        ellipse(p, cx + 2, cy - 1, max(3, rx - 4), max(2, ry - 2), (81, 132, 78, 255))
    line(p, 15, 55, 47, 51, (106, 102, 91, 255), 2)
    return p


def water_lily_pad_cluster() -> Pixels:
    p = blank()
    ellipse(p, 32, 47, 23, 9, (45, 119, 157, 140))
    for cx, cy, rx, ry in ((19, 43, 8, 4), (32, 50, 9, 5), (46, 42, 8, 4)):
        ellipse(p, cx, cy, rx, ry, (43, 131, 74, 245))
        line(p, cx, cy, cx + rx - 1, cy - 1, (22, 92, 54, 255), 1)
    ellipse(p, 31, 46, 4, 3, (242, 226, 242, 255))
    put(p, 31, 46, (255, 237, 117, 255))
    line(p, 13, 52, 26, 51, (134, 212, 232, 190), 1)
    line(p, 37, 39, 55, 41, (134, 212, 232, 190), 1)
    return p


def water_shore_reeds() -> Pixels:
    p = blank()
    ellipse(p, 32, 51, 22, 7, (38, 111, 148, 120))
    for x in (19, 24, 42, 47):
        line(p, x, 55, x + 2, 29, (45, 105, 61, 255), 1)
        rect(p, x, 25, x + 3, 31, (119, 86, 48, 255))
    line(p, 14, 48, 27, 46, (193, 239, 246, 220), 1)
    line(p, 27, 46, 38, 47, (193, 239, 246, 220), 1)
    line(p, 38, 47, 52, 45, (193, 239, 246, 220), 1)
    line(p, 20, 54, 34, 53, (146, 218, 234, 210), 1)
    line(p, 34, 53, 49, 55, (146, 218, 234, 210), 1)
    return p


def road_milestone() -> Pixels:
    p = blank()
    ellipse(p, 32, 54, 16, 4, (104, 74, 46, 140))
    rect(p, 26, 28, 39, 53, (126, 114, 95, 255))
    ellipse(p, 32, 28, 7, 6, (151, 140, 120, 255))
    line(p, 28, 37, 36, 37, (77, 68, 59, 255), 1)
    line(p, 30, 42, 35, 42, (77, 68, 59, 255), 1)
    line(p, 24, 54, 42, 50, (178, 135, 78, 255), 1)
    return p


def road_signpost() -> Pixels:
    p = blank()
    ellipse(p, 33, 54, 16, 4, (104, 74, 46, 140))
    rect(p, 30, 25, 34, 55, (99, 67, 39, 255))
    rect(p, 20, 27, 45, 34, (145, 91, 47, 255))
    polygon(p, [(45, 27), (55, 31), (45, 34)], (145, 91, 47, 255))
    line(p, 23, 30, 42, 30, (190, 130, 67, 255), 1)
    rect(p, 25, 39, 42, 45, (122, 79, 43, 255))
    line(p, 26, 42, 40, 42, (181, 124, 65, 255), 1)
    return p


def grass_pond() -> Pixels:
    p = blank()
    ellipse(p, 32, 50, 24, 8, (48, 94, 53, 125))
    ellipse(p, 31, 45, 19, 9, (54, 134, 151, 225))
    ellipse(p, 35, 42, 12, 5, (93, 172, 180, 150))
    for cx, cy, rx, ry in ((20, 43, 6, 3), (43, 46, 7, 3)):
        ellipse(p, cx, cy, rx, ry, (56, 137, 72, 245))
        line(p, cx, cy, cx + rx - 1, cy - 1, (29, 95, 57, 255), 1)
    line(p, 16, 50, 27, 48, (146, 220, 226, 190), 1)
    line(p, 36, 39, 51, 41, (146, 220, 226, 190), 1)
    for x in (13, 48):
        line(p, x, 54, x + 3, 40, (65, 142, 62, 255), 1)
    return p


def grass_stone_stack() -> Pixels:
    p = blank()
    ellipse(p, 32, 54, 19, 4, (47, 91, 42, 150))
    for cx, cy, rx, ry, color in (
        (25, 50, 8, 4, (101, 111, 95, 255)),
        (36, 49, 10, 5, (124, 132, 111, 255)),
        (31, 42, 8, 4, (86, 101, 91, 255)),
        (38, 36, 6, 3, (141, 148, 125, 255)),
    ):
        ellipse(p, cx, cy, rx, ry, color)
        line(p, cx - rx + 2, cy - 1, cx + rx - 2, cy - 2, (176, 184, 150, 255), 1)
    for x in (18, 45, 50):
        line(p, x, 55, x + 2, 43, (53, 129, 55, 255), 1)
    return p


def forest_fairy_pool() -> Pixels:
    p = blank()
    ellipse(p, 32, 52, 24, 6, (16, 49, 30, 185))
    ellipse(p, 31, 45, 17, 8, (36, 104, 101, 215))
    ellipse(p, 34, 42, 10, 4, (95, 177, 169, 130))
    for cx, cy in ((18, 45), (45, 47), (26, 54)):
        ellipse(p, cx, cy, 5, 3, (42, 121, 62, 255))
    sparkle(p, [(25, 39), (37, 42), (43, 36)], (176, 232, 190, 255))
    line(p, 15, 51, 28, 49, (118, 196, 185, 180), 1)
    line(p, 36, 40, 50, 42, (118, 196, 185, 180), 1)
    return p


def forest_ancient_roots() -> Pixels:
    p = blank()
    ellipse(p, 33, 54, 22, 4, (16, 47, 30, 170))
    for x1, y1, x2, y2 in ((15, 51, 29, 38), (25, 54, 33, 32), (42, 53, 35, 36), (19, 49, 49, 46)):
        line(p, x1, y1, x2, y2, (83, 57, 36, 255), 4)
        line(p, x1 + 1, y1 - 1, x2 + 1, y2, (132, 91, 50, 255), 1)
    ellipse(p, 31, 40, 8, 5, (45, 117, 61, 220))
    ellipse(p, 44, 47, 7, 4, (32, 93, 51, 220))
    sparkle(p, [(28, 37), (36, 44)], (101, 166, 85, 255))
    return p


def desert_oasis_pool() -> Pixels:
    p = blank()
    ellipse(p, 32, 53, 24, 5, (134, 91, 47, 150))
    ellipse(p, 32, 46, 19, 8, (46, 137, 158, 225))
    ellipse(p, 35, 42, 11, 4, (113, 203, 197, 150))
    for x in (18, 47):
        line(p, x, 55, x + 3, 31, (52, 117, 56, 255), 2)
        rect(p, x + 1, 27, x + 4, 33, (144, 97, 49, 255))
    line(p, 14, 50, 28, 49, (170, 228, 225, 190), 1)
    line(p, 34, 40, 52, 42, (170, 228, 225, 190), 1)
    return p


def desert_jar_cache() -> Pixels:
    p = blank()
    ellipse(p, 32, 54, 20, 4, (132, 86, 43, 145))
    for cx, cy, scale in ((23, 44, 7), (34, 40, 9), (44, 47, 6)):
        rect(p, cx - scale // 2, cy - 9, cx + scale // 2, cy + 8, (154, 91, 50, 255))
        ellipse(p, cx, cy - 9, scale // 2 + 1, 3, (190, 126, 67, 255))
        ellipse(p, cx, cy + 8, scale // 2 + 1, 3, (111, 68, 46, 255))
        line(p, cx - scale // 2, cy - 1, cx + scale // 2, cy - 2, (221, 158, 82, 255), 1)
    line(p, 12, 53, 24, 50, (218, 179, 92, 255), 1)
    line(p, 43, 52, 55, 48, (218, 179, 92, 255), 1)
    return p


def tundra_thaw_pond() -> Pixels:
    p = blank()
    ellipse(p, 32, 52, 24, 6, (151, 179, 185, 120))
    ellipse(p, 31, 45, 18, 8, (91, 161, 190, 220))
    ellipse(p, 34, 42, 12, 4, (193, 239, 248, 155))
    for x1, y1, x2, y2 in ((14, 42, 25, 40), (38, 50, 53, 48), (21, 52, 32, 50)):
        line(p, x1, y1, x2, y2, (238, 250, 255, 255), 2)
    sparkle(p, [(28, 39), (42, 43)], (255, 255, 255, 255))
    return p


def tundra_rune_stone() -> Pixels:
    p = blank()
    ellipse(p, 32, 54, 18, 4, (128, 154, 162, 140))
    polygon(p, [(25, 52), (27, 24), (39, 20), (43, 52)], (107, 127, 129, 255))
    polygon(p, [(34, 22), (39, 20), (43, 52), (34, 50)], (78, 98, 105, 255))
    line(p, 29, 34, 37, 31, (210, 242, 255, 255), 1)
    line(p, 33, 31, 33, 44, (163, 217, 238, 255), 1)
    line(p, 28, 44, 38, 42, (210, 242, 255, 255), 1)
    ellipse(p, 28, 51, 9, 3, (225, 242, 246, 210))
    return p


def marsh_bubble_pool() -> Pixels:
    p = blank()
    ellipse(p, 32, 51, 24, 7, (33, 75, 55, 165))
    ellipse(p, 31, 45, 19, 9, (38, 92, 79, 230))
    for cx, cy, r in ((23, 43, 3), (34, 39, 2), (43, 47, 3), (29, 50, 2)):
        ellipse(p, cx, cy, r, r, (129, 190, 162, 150))
        put(p, cx - 1, cy - 1, (210, 244, 221, 190))
    for x in (14, 18, 48, 52):
        line(p, x, 55, x + 2, 31, (48, 112, 62, 255), 1)
        rect(p, x, 27, x + 3, 33, (119, 86, 48, 255))
    return p


def marsh_firefly_reeds() -> Pixels:
    p = blank()
    ellipse(p, 32, 54, 22, 4, (32, 76, 52, 150))
    for x, h in ((15, 20), (21, 28), (28, 24), (36, 31), (44, 22), (50, 27)):
        line(p, x, 55, x + (h % 5) - 2, 55 - h, (42, 107, 58, 255), 2)
        rect(p, x - 1, 51 - h, x + 2, 56 - h, (124, 90, 50, 255))
    sparkle(p, [(23, 34), (38, 29), (47, 39), (17, 43)], (232, 225, 94, 235))
    ellipse(p, 31, 50, 14, 4, (39, 105, 67, 190))
    return p


def badlands_dust_bowl() -> Pixels:
    p = blank()
    ellipse(p, 32, 53, 24, 5, (80, 53, 41, 170))
    ellipse(p, 32, 46, 19, 7, (118, 75, 54, 230))
    ellipse(p, 35, 43, 12, 4, (161, 95, 61, 140))
    for x1, y1, x2, y2 in ((17, 45, 27, 48), (29, 40, 42, 45), (40, 51, 52, 47)):
        line(p, x1, y1, x2, y2, (72, 49, 42, 255), 1)
    sparkle(p, [(25, 41), (43, 43)], (190, 125, 70, 210))
    return p


def badlands_totem_stones() -> Pixels:
    p = blank()
    ellipse(p, 33, 54, 20, 4, (81, 53, 42, 155))
    for cx, h, color in ((23, 19, (98, 67, 55, 255)), (34, 27, (145, 86, 58, 255)), (45, 16, (110, 73, 55, 255))):
        polygon(p, [(cx - 5, 53), (cx - 3, 53 - h), (cx + 4, 50 - h), (cx + 6, 53)], color)
        line(p, cx - 3, 45, cx + 4, 43, (203, 132, 72, 255), 1)
    line(p, 13, 55, 52, 50, (187, 116, 66, 255), 1)
    return p


def mountain_spring_pool() -> Pixels:
    p = blank()
    ellipse(p, 32, 54, 21, 4, (48, 54, 54, 170))
    ellipse(p, 31, 47, 17, 7, (61, 131, 155, 220))
    polygon(p, [(15, 52), (25, 39), (34, 52)], (82, 88, 82, 255))
    polygon(p, [(37, 52), (48, 37), (55, 52)], (103, 102, 91, 255))
    line(p, 22, 45, 34, 43, (171, 229, 235, 190), 1)
    line(p, 34, 43, 48, 45, (171, 229, 235, 190), 1)
    sparkle(p, [(35, 41), (43, 46)], (232, 252, 255, 255))
    return p


def mountain_cairn() -> Pixels:
    p = blank()
    ellipse(p, 32, 54, 17, 4, (44, 50, 50, 165))
    for cx, cy, rx, ry, color in (
        (28, 51, 10, 4, (91, 94, 88, 255)),
        (36, 49, 9, 4, (116, 113, 101, 255)),
        (31, 43, 8, 4, (73, 82, 82, 255)),
        (35, 36, 6, 3, (134, 128, 111, 255)),
        (32, 30, 4, 3, (98, 108, 105, 255)),
    ):
        ellipse(p, cx, cy, rx, ry, color)
        line(p, cx - rx + 2, cy - 1, cx + rx - 2, cy - 2, (164, 157, 132, 255), 1)
    return p


def mountain_pass_snowmelt_pool() -> Pixels:
    p = blank()
    ellipse(p, 32, 53, 23, 4, (82, 74, 61, 145))
    ellipse(p, 31, 46, 18, 6, (71, 135, 157, 210))
    ellipse(p, 20, 43, 8, 3, (225, 238, 237, 220))
    ellipse(p, 44, 50, 9, 3, (225, 238, 237, 220))
    line(p, 18, 49, 31, 48, (177, 229, 232, 180), 1)
    line(p, 35, 43, 51, 44, (177, 229, 232, 180), 1)
    return p


def mountain_pass_way_cairn() -> Pixels:
    p = blank()
    ellipse(p, 32, 54, 17, 4, (77, 66, 51, 150))
    for cx, cy, rx, ry in ((28, 51, 9, 4), (36, 49, 8, 4), (32, 43, 7, 3), (35, 37, 5, 3)):
        ellipse(p, cx, cy, rx, ry, (119, 111, 92, 255))
        line(p, cx - rx + 2, cy - 1, cx + rx - 2, cy - 2, (178, 157, 113, 255), 1)
    rect(p, 18, 29, 21, 54, (98, 67, 39, 255))
    rect(p, 20, 31, 43, 37, (143, 91, 47, 255))
    polygon(p, [(43, 31), (51, 34), (43, 37)], (143, 91, 47, 255))
    return p


def water_driftwood() -> Pixels:
    p = blank()
    ellipse(p, 32, 50, 23, 6, (34, 111, 154, 120))
    line(p, 16, 47, 48, 39, (104, 70, 44, 255), 5)
    line(p, 18, 46, 47, 39, (158, 101, 56, 255), 2)
    line(p, 32, 43, 41, 31, (98, 66, 43, 255), 3)
    line(p, 15, 53, 29, 52, (151, 224, 237, 190), 1)
    line(p, 35, 45, 53, 47, (151, 224, 237, 190), 1)
    return p


def water_duckweed_patch() -> Pixels:
    p = blank()
    ellipse(p, 32, 47, 23, 9, (45, 119, 157, 130))
    for cx, cy, rx, ry in ((17, 44, 4, 2), (23, 49, 5, 3), (31, 42, 4, 2), (39, 50, 5, 3), (47, 44, 4, 2)):
        ellipse(p, cx, cy, rx, ry, (55, 132, 68, 245))
        line(p, cx, cy, cx + rx - 1, cy - 1, (28, 91, 53, 255), 1)
    sparkle(p, [(27, 48), (36, 44), (44, 51)], (104, 178, 88, 220))
    line(p, 14, 52, 27, 51, (134, 212, 232, 170), 1)
    line(p, 37, 39, 55, 41, (134, 212, 232, 170), 1)
    return p


ASSETS = {
    "grass": {
        "deco_grass_wildflowers": grass_wildflowers,
        "deco_grass_herb_patch": grass_herb_patch,
        "deco_grass_pond": grass_pond,
        "deco_grass_stone_stack": grass_stone_stack,
    },
    "forest": {
        "deco_forest_blue_mushroom_ring": forest_blue_mushroom_ring,
        "deco_forest_shrine_stone": forest_shrine_stone,
        "deco_forest_fairy_pool": forest_fairy_pool,
        "deco_forest_ancient_roots": forest_ancient_roots,
    },
    "desert": {
        "deco_desert_blooming_cactus": desert_blooming_cactus,
        "deco_desert_sun_bleached_bones": desert_sun_bleached_bones,
        "deco_desert_oasis_pool": desert_oasis_pool,
        "deco_desert_jar_cache": desert_jar_cache,
    },
    "tundra": {
        "deco_tundra_ice_crystals": tundra_ice_crystals,
        "deco_tundra_frost_bush": tundra_frost_bush,
        "deco_tundra_thaw_pond": tundra_thaw_pond,
        "deco_tundra_rune_stone": tundra_rune_stone,
    },
    "marsh": {
        "deco_marsh_lily_pool": marsh_lily_pool,
        "deco_marsh_twisted_roots": marsh_twisted_roots,
        "deco_marsh_bubble_pool": marsh_bubble_pool,
        "deco_marsh_firefly_reeds": marsh_firefly_reeds,
    },
    "badlands": {
        "deco_badlands_red_spire": badlands_red_spire,
        "deco_badlands_skull_marker": badlands_skull_marker,
        "deco_badlands_dust_bowl": badlands_dust_bowl,
        "deco_badlands_totem_stones": badlands_totem_stones,
    },
    "mountain": {
        "deco_mountain_crystal_cluster": mountain_crystal_cluster,
        "deco_mountain_scrub_pine": mountain_scrub_pine,
        "deco_mountain_spring_pool": mountain_spring_pool,
        "deco_mountain_cairn": mountain_cairn,
        "deco_mountain_pass_snowmelt_pool": mountain_pass_snowmelt_pool,
        "deco_mountain_pass_way_cairn": mountain_pass_way_cairn,
    },
    "water": {
        "deco_water_lily_pad_cluster": water_lily_pad_cluster,
        "deco_water_shore_reeds": water_shore_reeds,
        "deco_water_duckweed_patch": water_duckweed_patch,
    },
    "road": {
        "deco_road_milestone": road_milestone,
        "deco_road_signpost": road_signpost,
    },
}


def main() -> None:
    for biome, assets in ASSETS.items():
        for name, draw in assets.items():
            write_png(OUT / biome / f"{name}.png", draw())


if __name__ == "__main__":
    main()
