from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[2]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT
from tools.assets.shared.asset_paths import family_dir

import math
import struct
import zlib
from pathlib import Path

SIZE = 48
OUT = Path("assets")


def png(path: Path, pixels: list[list[tuple[int, int, int, int]]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    height = len(pixels)
    width = len(pixels[0])
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in pixels)

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


def output_path(name: str) -> Path:
    if name.startswith("class_") or name.startswith("player"):
        folder = "player"
    elif name.startswith("deco_"):
        folder = "deco"
    elif name.startswith("npc_"):
        folder = "npcs"
    elif name in {
        "badlands",
        "city",
        "desert",
        "dungeon",
        "forest",
        "grass",
        "marsh",
        "mountain",
        "mountain_massif",
        "mountain_peak",
        "road",
        "tundra",
        "village",
        "water",
    }:
        folder = "terrain"
    else:
        folder = "monsters"
    return family_dir(OUT, folder) / f"{name}.png"


def canvas(size: int = SIZE, color: tuple[int, int, int, int] = (0, 0, 0, 0)) -> list[list[tuple[int, int, int, int]]]:
    return [[color for _ in range(size)] for _ in range(size)]


def rect(img, x1, y1, x2, y2, color):
    for y in range(max(0, y1), min(len(img), y2)):
        for x in range(max(0, x1), min(len(img[0]), x2)):
            img[y][x] = color


def set_px(img, x, y, color):
    if 0 <= y < len(img) and 0 <= x < len(img[0]):
        img[y][x] = color


def shade(color, dr=0, dg=0, db=0):
    r, g, b, a = color
    return (
        min(255, max(0, r + dr)),
        min(255, max(0, g + dg)),
        min(255, max(0, b + db)),
        a,
    )


def circle(img, cx, cy, r, color):
    for y in range(len(img)):
        for x in range(len(img[0])):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                img[y][x] = color


def ellipse(img, cx, cy, rx, ry, color):
    for y in range(max(0, cy - ry), min(len(img), cy + ry + 1)):
        for x in range(max(0, cx - rx), min(len(img[0]), cx + rx + 1)):
            nx = (x - cx) / max(1, rx)
            ny = (y - cy) / max(1, ry)
            if nx * nx + ny * ny <= 1:
                img[y][x] = color


def polygon(img, points, color):
    min_x = max(0, min(x for x, _ in points))
    max_x = min(len(img[0]) - 1, max(x for x, _ in points))
    min_y = max(0, min(y for _, y in points))
    max_y = min(len(img) - 1, max(y for _, y in points))
    for y in range(min_y, max_y + 1):
        for x in range(min_x, max_x + 1):
            inside = False
            j = len(points) - 1
            for i in range(len(points)):
                xi, yi = points[i]
                xj, yj = points[j]
                crosses = (yi > y) != (yj > y)
                if crosses:
                    x_at_y = (xj - xi) * (y - yi) / max(1, yj - yi) + xi
                    if x < x_at_y:
                        inside = not inside
                j = i
            if inside:
                img[y][x] = color


def polyline(img, points, color, width=2):
    for (x1, y1), (x2, y2) in zip(points, points[1:]):
        steps = max(abs(x2 - x1), abs(y2 - y1), 1)
        for i in range(steps + 1):
            t = i / steps
            x = int(x1 + (x2 - x1) * t)
            y = int(y1 + (y2 - y1) * t)
            rect(img, x - width // 2, y - width // 2, x + width, y + width, color)


def tile(base, specks):
    img = canvas(SIZE, base)
    for i, (color, step) in enumerate(specks):
        for y in range((i * 7) % step, SIZE, step):
            for x in range((i * 11) % step, SIZE, step):
                if (x * 3 + y * 5 + i) % 4:
                    rect(img, x, y, x + 2, y + 2, color)
    return img


def noise(x, y, seed=0):
    value = (x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)
    value = (value ^ (value >> 13)) * 1274126177
    return (value ^ (value >> 16)) & 255


def textured_tile(base, palette, seed=0):
    img = canvas(SIZE, base)
    for y in range(SIZE):
        for x in range(SIZE):
            n = noise(x // 2, y // 2, seed)
            r, g, b, a = base
            if n < 72:
                dr, dg, db = palette[0]
            elif n > 188:
                dr, dg, db = palette[1]
            else:
                dr, dg, db = (0, 0, 0)
            img[y][x] = (
                min(255, max(0, r + dr)),
                min(255, max(0, g + dg)),
                min(255, max(0, b + db)),
                a,
            )
    return img


def grass_tile():
    img = textured_tile((92, 163, 75, 255), [(-16, -27, -10), (20, 26, 10)], 1)
    for i in range(30):
        x = (i * 17 + 5) % SIZE
        y = (i * 23 + 9) % SIZE
        color = (118, 191, 84, 255) if i % 3 else (66, 126, 62, 255)
        polyline(img, [(x, y + 4), (x + 2, y), (x + 5, y + 4)], color, 1)
    for x, y, color in ((14, 12, (238, 218, 118, 255)), (34, 29, (236, 234, 246, 255)), (23, 36, (229, 126, 138, 255))):
        rect(img, x, y, x + 2, y + 2, color)
    return img


def forest_tile():
    img = textured_tile((58, 128, 62, 255), [(-10, -20, -9), (14, 24, 12)], 1)
    for i in range(14):
        x = (i * 17 + 5) % SIZE
        y = (i * 23 + 3) % SIZE
        color = (45, 105, 51, 255) if i % 3 else (82, 150, 70, 255)
        ellipse(img, x, y, 5 + i % 2, 2, color)
    for i in range(4):
        x = (i * 19 + 11) % SIZE
        y = (i * 13 + 9) % SIZE
        rect(img, x, y, x + 3, y + 1, (91, 78, 45, 255))
    return img


def mountain_tile():
    img = textured_tile((118, 126, 122, 255), [(-18, -22, -16), (24, 25, 20)], 3)
    for i in range(9):
        cx = 5 + (i * 17) % 40
        cy = 8 + (i * 11) % 34
        ellipse(img, cx, cy, 5 + i % 4, 3 + i % 2, (101, 106, 101, 255))
        ellipse(img, cx - 1, cy - 1, 2 + i % 3, 1, (151, 154, 145, 255))
    return img


def mountain_peak_tile():
    img = canvas(SIZE)
    peaks = [(12, 38, 21), (28, 35, 25), (39, 39, 17)]
    for cx, base_y, h in peaks:
        dark = (88, 94, 102, 255)
        mid = (145, 151, 158, 255)
        snow = (231, 235, 236, 255)
        polyline(img, [(cx - 13, base_y), (cx, base_y - h), (cx + 13, base_y)], dark, 5)
        polyline(img, [(cx - 8, base_y - 2), (cx, base_y - h + 3), (cx + 8, base_y - 2)], mid, 4)
        polyline(img, [(cx - 4, base_y - h + 8), (cx, base_y - h + 1), (cx + 4, base_y - h + 8)], snow, 3)
        ellipse(img, cx, base_y + 1, 11, 3, (69, 82, 78, 190))
    return img


def mountain_massif_tile():
    size = 144
    img = canvas(size)
    peaks = [
        (14, 124, 48, 20, (52, 54, 57, 255), (116, 89, 60, 255)),
        (39, 128, 88, 9, (43, 45, 50, 255), (154, 111, 70, 255)),
        (83, 126, 128, 28, (62, 60, 56, 255), (128, 94, 62, 255)),
        (102, 130, 146, 56, (48, 51, 57, 255), (168, 123, 78, 255)),
    ]
    for left, base_y, right, peak_y, shadow, sun in peaks:
        cx = (left + right) // 2
        polygon(img, [(left, base_y), (cx, peak_y), (right, base_y)], shadow)
        polygon(img, [(cx, peak_y), (right, base_y), (cx + 8, base_y)], sun)
        polygon(img, [(cx - 12, peak_y + 26), (cx, peak_y), (cx + 10, peak_y + 28), (cx + 2, peak_y + 23)], (238, 241, 231, 255))
        polygon(img, [(cx - 2, peak_y + 3), (cx + 11, peak_y + 30), (cx + 1, peak_y + 23)], (202, 213, 213, 255))
        for i in range(7):
            x = left + 5 + (i * 11) % max(1, right - left - 8)
            y1 = peak_y + 28 + (i * 17) % max(1, base_y - peak_y - 35)
            polyline(img, [(x, y1), (x + 7, y1 + 18)], (34, 35, 39, 230), 2)
    for i in range(9):
        x = 8 + (i * 17) % 116
        y = 106 + (i * 11) % 28
        ellipse(img, x, y, 10, 4, (53, 104, 55, 220))
        ellipse(img, x + 3, y - 2, 6, 2, (78, 138, 66, 230))
    for i in range(12):
        x = 5 + (i * 19) % 128
        y = 122 + (i * 7) % 16
        polyline(img, [(x, y), (x + 8, y - 1), (x + 15, y + 2)], (35, 74, 46, 230), 2)
    return img


def water_tile():
    img = textured_tile((47, 132, 190, 255), [(-10, -28, -25), (18, 36, 42)], 4)
    for i in range(8):
        y = 6 + i * 6
        offset = (i * 11) % 18
        polyline(img, [(offset, y), (offset + 8, y + 2), (offset + 18, y - 1), (offset + 30, y + 1)], (120, 208, 232, 255), 1)
    return img


def road_tile():
    base = (190, 151, 91, 255)
    img = textured_tile(base, [(-28, -27, -19), (30, 28, 14)], 5)
    for y in range(SIZE):
        for x in range(SIZE):
            grain = noise(x // 3, y // 2, 22)
            vein = math.sin((x + noise(y, 0, 23) * 0.03) * 0.42) + math.cos((y + x * 0.35) * 0.37)
            if grain < 35:
                img[y][x] = shade(img[y][x], -18, -14, -9)
            elif grain > 220:
                img[y][x] = shade(img[y][x], 18, 16, 8)
            if vein > 1.25 and noise(x, y, 24) > 96:
                img[y][x] = shade(img[y][x], 10, 8, 3)
    for i in range(38):
        x = (i * 17 + noise(i, 3, 25)) % SIZE
        y = (i * 23 + noise(i, 7, 26)) % SIZE
        rx = 1 + (noise(i, 0, 27) % 3)
        ry = 1 + (noise(i, 1, 28) % 2)
        color = (139, 101, 65, 255) if i % 3 else (231, 202, 141, 255)
        ellipse(img, x, y, rx, ry, color)
        if i % 5 == 0:
            set_px(img, x + rx, y, shade(color, -22, -19, -13))
    for i in range(7):
        y = 4 + i * 7 + (noise(i, 4, 29) % 3) - 1
        polyline(
            img,
            [
                (0, y),
                (9, y + (noise(i, 1, 30) % 5) - 2),
                (22, y + (noise(i, 2, 31) % 5) - 2),
                (37, y + (noise(i, 3, 32) % 5) - 2),
                (47, y + (noise(i, 4, 33) % 5) - 2),
            ],
            (215, 181, 116, 160),
            1,
        )
    return img


def desert_tile():
    img = textured_tile((209, 170, 95, 255), [(-19, -23, -14), (28, 25, 12)], 6)
    for i in range(4):
        y = 9 + i * 11
        polyline(img, [(4, y), (16, y - 3), (31, y + 2), (44, y - 1)], (232, 202, 124, 255), 1)
        polyline(img, [(8, y + 5), (21, y + 7), (38, y + 4)], (177, 125, 70, 255), 1)
    return img


def tundra_tile():
    img = textured_tile((205, 224, 232, 255), [(-20, -23, -22), (24, 26, 24)], 7)
    for i in range(18):
        x = (i * 17 + 2) % SIZE
        y = (i * 11 + 5) % SIZE
        rect(img, x, y, x + 5, y + 1, (236, 246, 252, 255))
    return img


def marsh_tile():
    img = textured_tile((77, 125, 85, 255), [(-16, -30, -9), (22, 34, 16)], 8)
    for i in range(7):
        cx = 8 + (i * 17) % 34
        cy = 10 + (i * 23) % 28
        ellipse(img, cx, cy, 7, 4, (57, 103, 86, 255))
        ellipse(img, cx + 2, cy - 1, 4, 2, (84, 146, 113, 255))
    return img


def badlands_tile():
    img = textured_tile((160, 108, 72, 255), [(-28, -23, -16), (25, 18, 11)], 9)
    for i in range(5):
        x = 2 + (i * 13) % 38
        y = 8 + (i * 17) % 30
        polyline(img, [(x, y), (x + 11, y + 7), (x + 22, y + 5)], (112, 78, 61, 255), 2)
    return img


def decoration(kind):
    img = canvas(SIZE)
    if kind == "bush":
        for cx, cy, r in ((20, 28, 8), (27, 25, 9), (31, 31, 7)):
            circle(img, cx, cy, r, (47, 126, 55, 255))
            circle(img, cx - 2, cy - 2, max(2, r - 4), (82, 160, 67, 255))
    elif kind == "flowers":
        for x, y, c in ((18, 26, (238, 218, 118, 255)), (27, 31, (236, 235, 247, 255)), (33, 24, (226, 118, 136, 255))):
            rect(img, x, y, x + 2, y + 6, (52, 128, 57, 255))
            rect(img, x - 1, y - 1, x + 3, y + 2, c)
    elif kind == "grass_clump":
        for x in range(14, 36, 4):
            polyline(img, [(x, 35), (x + 2, 22 + (x % 5)), (x + 5, 35)], (64, 133, 55, 255), 1)
    elif kind == "pine_sapling":
        rect(img, 23, 22, 26, 39, (89, 62, 39, 255))
        for y, w in ((18, 9), (25, 12), (32, 15)):
            polyline(img, [(24 - w, y), (24, y - 12), (24 + w, y)], (30, 102, 53, 255), 4)
    elif kind == "stump":
        rect(img, 18, 24, 31, 38, (104, 74, 45, 255))
        ellipse(img, 24, 24, 7, 4, (150, 101, 60, 255))
    elif kind == "cactus":
        rect(img, 22, 15, 28, 39, (69, 142, 81, 255))
        rect(img, 15, 22, 21, 28, (69, 142, 81, 255))
        rect(img, 29, 27, 35, 33, (69, 142, 81, 255))
        rect(img, 24, 14, 26, 39, (104, 180, 95, 255))
    elif kind == "dry_grass":
        for x in range(14, 36, 5):
            polyline(img, [(x, 37), (x + 1, 24), (x + 5, 37)], (173, 133, 72, 255), 1)
    elif kind == "snow_pine":
        rect(img, 23, 24, 26, 40, (89, 70, 50, 255))
        for y, w in ((20, 8), (27, 12), (34, 15)):
            polyline(img, [(24 - w, y), (24, y - 11), (24 + w, y)], (42, 116, 86, 255), 4)
            polyline(img, [(24 - w + 2, y - 2), (24, y - 11), (24 + w - 2, y - 2)], (236, 246, 252, 255), 2)
    elif kind == "snow_mound":
        ellipse(img, 25, 32, 14, 7, (231, 241, 248, 255))
        ellipse(img, 21, 30, 7, 3, (248, 252, 255, 255))
    elif kind == "reeds":
        for x in range(15, 35, 4):
            polyline(img, [(x, 38), (x + 1, 20)], (48, 101, 62, 255), 1)
            rect(img, x, 17, x + 3, 22, (123, 90, 50, 255))
    elif kind == "mushrooms":
        for x, y in ((18, 33), (27, 29), (34, 35)):
            rect(img, x, y, x + 3, y + 7, (229, 214, 180, 255))
            ellipse(img, x + 2, y, 5, 3, (190, 70, 74, 255))
    elif kind == "bog_grass":
        for x in range(13, 38, 4):
            polyline(img, [(x, 38), (x + 3, 23), (x + 7, 38)], (61, 132, 88, 255), 1)
    else:
        for cx, cy, rx, ry in ((19, 31, 6, 4), (29, 28, 8, 5), (34, 35, 5, 3)):
            ellipse(img, cx, cy, rx, ry, (112, 108, 100, 255))
            ellipse(img, cx - 1, cy - 1, max(2, rx - 3), max(1, ry - 2), (151, 145, 132, 255))
    return img


def draw_cottage(img, x, y, w, h, roof, wall, trim):
    shadow = shade(wall, -38, -32, -24)
    rect(img, x + 1, y + h - 2, x + w - 1, y + h, shadow)
    rect(img, x, y + 8, x + w, y + h, wall)
    polygon(img, [(x - 2, y + 9), (x + w // 2, y - 2), (x + w + 2, y + 9)], roof)
    polyline(img, [(x - 1, y + 10), (x + w // 2, y - 1), (x + w + 1, y + 10)], shade(roof, -42, -28, -18), 1)
    for sx in range(x + 3, x + w - 3, 5):
        polyline(img, [(sx, y + 8), (sx + 3, y + 2)], shade(roof, 25, 17, 8), 1)
    rect(img, x + w // 2 - 3, y + h - 9, x + w // 2 + 3, y + h, (62, 39, 24, 255))
    rect(img, x + 3, y + 14, x + 8, y + 19, (80, 112, 132, 255))
    rect(img, x + w - 8, y + 14, x + w - 3, y + 19, (80, 112, 132, 255))
    rect(img, x + 4, y + 16, x + 7, y + 18, (235, 203, 111, 255))
    rect(img, x + w - 7, y + 16, x + w - 4, y + 18, (235, 203, 111, 255))
    polyline(img, [(x + 2, y + 23), (x + w - 2, y + 23)], trim, 1)
    for px in range(x + 2, x + w - 1, 7):
        set_px(img, px, y + 25, shade(wall, 18, 15, 9))


def city():
    img = textured_tile((133, 119, 91, 255), [(-22, -22, -18), (24, 21, 13)], 40)
    for y in range(0, SIZE, 8):
        offset = 0 if (y // 8) % 2 else 7
        polyline(img, [(0, y), (SIZE, y + (noise(y, 2, 41) % 3) - 1)], (102, 91, 72, 180), 1)
        for x in range(-offset, SIZE, 14):
            polyline(img, [(x, y), (x + 1, min(SIZE - 1, y + 8))], (102, 91, 72, 150), 1)
    rect(img, 5, 29, 43, 41, (105, 94, 71, 255))
    polyline(img, [(8, 34), (22, 29), (39, 33)], (185, 153, 92, 255), 4)
    draw_cottage(img, 8, 15, 20, 24, (128, 58, 45, 255), (214, 193, 143, 255), (111, 74, 44, 255))
    draw_cottage(img, 25, 18, 16, 21, (91, 102, 111, 255), (200, 184, 139, 255), (96, 77, 52, 255))
    rect(img, 32, 8, 38, 21, (170, 151, 106, 255))
    polygon(img, [(30, 8), (35, 1), (40, 8)], (85, 100, 116, 255))
    rect(img, 34, 12, 36, 15, (74, 55, 42, 255))
    for i in range(16):
        x = 4 + (i * 13 + 3) % 39
        y = 4 + (i * 17 + 7) % 39
        color = (167, 139, 91, 255) if i % 2 else (229, 207, 151, 255)
        rect(img, x, y, x + 2, y + 1, color)
    return img


def village():
    img = textured_tile((106, 151, 79, 255), [(-18, -24, -13), (18, 24, 8)], 42)
    for i in range(18):
        x = (i * 19 + 6) % SIZE
        y = (i * 11 + 9) % SIZE
        polyline(img, [(x, y + 3), (x + 2, y), (x + 5, y + 4)], (70, 124, 58, 255), 1)
    ellipse(img, 24, 29, 17, 8, (188, 157, 95, 255))
    ellipse(img, 24, 29, 14, 6, (210, 180, 112, 255))
    draw_cottage(img, 6, 16, 18, 23, (135, 65, 44, 255), (213, 188, 133, 255), (107, 72, 42, 255))
    draw_cottage(img, 24, 14, 19, 25, (154, 82, 48, 255), (203, 179, 127, 255), (105, 70, 43, 255))
    rect(img, 18, 35, 31, 39, (100, 73, 43, 255))
    for i in range(9):
        x = 5 + (i * 17) % 38
        y = 30 + (i * 7) % 11
        ellipse(img, x, y, 2, 1, (76, 123, 58, 255))
    return img


def dungeon():
    img = tile((76, 77, 88, 255), [((94, 96, 110, 255), 8), ((55, 56, 68, 255), 11)])
    rect(img, 11, 18, 37, 42, (38, 39, 47, 255))
    circle(img, 24, 18, 13, (38, 39, 47, 255))
    rect(img, 9, 39, 39, 44, (48, 49, 58, 255))
    return img


def humanoid(body, hair, accent, size=48):
    img = canvas(size)
    s = size / 48
    circle(img, int(24 * s), int(13 * s), int(8 * s), (232, 183, 132, 255))
    rect(img, int(16 * s), int(7 * s), int(32 * s), int(12 * s), hair)
    rect(img, int(15 * s), int(22 * s), int(33 * s), int(38 * s), body)
    rect(img, int(12 * s), int(25 * s), int(18 * s), int(35 * s), accent)
    rect(img, int(30 * s), int(25 * s), int(36 * s), int(35 * s), accent)
    rect(img, int(17 * s), int(38 * s), int(23 * s), int(45 * s), (62, 58, 76, 255))
    rect(img, int(25 * s), int(38 * s), int(31 * s), int(45 * s), (62, 58, 76, 255))
    rect(img, int(20 * s), int(14 * s), int(22 * s), int(16 * s), (31, 30, 36, 255))
    rect(img, int(27 * s), int(14 * s), int(29 * s), int(16 * s), (31, 30, 36, 255))
    return img


def monster(kind, size=48):
    img = canvas(size)
    s = size / 48
    if kind == "slime":
        circle(img, int(24 * s), int(29 * s), int(15 * s), (72, 184, 127, 230))
        rect(img, int(13 * s), int(30 * s), int(35 * s), int(40 * s), (52, 148, 106, 255))
    elif kind == "wolf":
        circle(img, int(25 * s), int(26 * s), int(13 * s), (105, 112, 124, 255))
        polyline(img, [(12, 15), (18, 5), (22, 17)], (105, 112, 124, 255), int(5 * s))
        polyline(img, [(30, 17), (36, 5), (40, 17)], (105, 112, 124, 255), int(5 * s))
    elif kind == "bat":
        circle(img, int(24 * s), int(24 * s), int(7 * s), (71, 60, 102, 255))
        polyline(img, [(19, 24), (3, 14), (11, 34), (20, 27)], (71, 60, 102, 255), int(4 * s))
        polyline(img, [(29, 24), (45, 14), (37, 34), (28, 27)], (71, 60, 102, 255), int(4 * s))
    elif kind == "skeleton":
        circle(img, int(24 * s), int(13 * s), int(8 * s), (220, 216, 190, 255))
        rect(img, int(20 * s), int(22 * s), int(28 * s), int(38 * s), (220, 216, 190, 255))
        polyline(img, [(15, 24), (33, 24), (39, 34)], (220, 216, 190, 255), int(3 * s))
    elif kind == "thornling":
        rect(img, int(21 * s), int(18 * s), int(28 * s), int(39 * s), (88, 61, 39, 255))
        circle(img, int(24 * s), int(21 * s), int(11 * s), (38, 116, 61, 255))
        circle(img, int(18 * s), int(27 * s), int(8 * s), (47, 143, 70, 255))
        circle(img, int(31 * s), int(28 * s), int(9 * s), (34, 104, 58, 255))
        for px, py in ((13, 19), (18, 10), (29, 9), (37, 22), (13, 34), (36, 36)):
            polyline(img, [(24, 25), (px, py)], (74, 49, 34, 255), int(2 * s))
        rect(img, int(19 * s), int(22 * s), int(21 * s), int(24 * s), (238, 211, 83, 255))
        rect(img, int(28 * s), int(22 * s), int(30 * s), int(24 * s), (238, 211, 83, 255))
    elif kind == "sand_stalker":
        ellipse(img, int(24 * s), int(29 * s), int(15 * s), int(9 * s), (173, 117, 62, 255))
        ellipse(img, int(23 * s), int(25 * s), int(10 * s), int(7 * s), (205, 151, 82, 255))
        polyline(img, [(11, 29), (4, 21), (9, 17)], (147, 92, 55, 255), int(3 * s))
        polyline(img, [(37, 29), (44, 21), (39, 17)], (147, 92, 55, 255), int(3 * s))
        for x in (13, 19, 29, 35):
            polyline(img, [(x, 35), (x - 4, 42)], (99, 72, 52, 255), int(2 * s))
        polyline(img, [(24, 21), (26, 11), (21, 7)], (117, 76, 54, 255), int(3 * s))
        rect(img, int(18 * s), int(23 * s), int(20 * s), int(25 * s), (46, 39, 32, 255))
        rect(img, int(28 * s), int(23 * s), int(30 * s), int(25 * s), (46, 39, 32, 255))
    elif kind == "ice_golem":
        polygon(img, [(14, 15), (24, 5), (34, 15), (31, 31), (17, 31)], (139, 202, 221, 255))
        polygon(img, [(13, 29), (35, 29), (39, 43), (9, 43)], (104, 164, 192, 255))
        polygon(img, [(24, 5), (34, 15), (31, 31), (24, 28)], (190, 231, 239, 255))
        polygon(img, [(24, 29), (35, 29), (39, 43), (24, 42)], (143, 209, 228, 255))
        polyline(img, [(13, 30), (5, 36), (8, 44)], (101, 157, 184, 255), int(4 * s))
        polyline(img, [(35, 30), (43, 36), (40, 44)], (101, 157, 184, 255), int(4 * s))
        rect(img, int(19 * s), int(17 * s), int(21 * s), int(20 * s), (36, 83, 116, 255))
        rect(img, int(28 * s), int(17 * s), int(30 * s), int(20 * s), (36, 83, 116, 255))
    elif kind == "bog_beast":
        ellipse(img, int(24 * s), int(31 * s), int(16 * s), int(11 * s), (72, 114, 73, 255))
        circle(img, int(19 * s), int(21 * s), int(8 * s), (86, 133, 85, 255))
        circle(img, int(30 * s), int(22 * s), int(9 * s), (62, 101, 70, 255))
        polyline(img, [(11, 30), (5, 23), (9, 38)], (48, 86, 64, 255), int(4 * s))
        polyline(img, [(37, 30), (43, 23), (39, 38)], (48, 86, 64, 255), int(4 * s))
        for x in (16, 23, 31):
            polyline(img, [(x, 18), (x - 2, 9)], (43, 101, 61, 255), int(1 * s))
            rect(img, int((x - 1) * s), int(8 * s), int((x + 2) * s), int(11 * s), (134, 104, 56, 255))
        rect(img, int(17 * s), int(23 * s), int(20 * s), int(25 * s), (232, 220, 116, 255))
        rect(img, int(29 * s), int(23 * s), int(32 * s), int(25 * s), (232, 220, 116, 255))
    elif kind == "ember_imp":
        circle(img, int(24 * s), int(18 * s), int(9 * s), (174, 65, 45, 255))
        rect(img, int(16 * s), int(25 * s), int(32 * s), int(40 * s), (127, 52, 54, 255))
        polygon(img, [(17, 13), (12, 5), (21, 11)], (80, 49, 60, 255))
        polygon(img, [(31, 13), (36, 5), (27, 11)], (80, 49, 60, 255))
        polyline(img, [(16, 30), (6, 24), (10, 19)], (115, 47, 54, 255), int(3 * s))
        polyline(img, [(32, 30), (42, 24), (38, 19)], (115, 47, 54, 255), int(3 * s))
        polygon(img, [(21, 39), (24, 29), (28, 39), (24, 45)], (232, 119, 44, 255))
        polygon(img, [(23, 38), (24, 32), (26, 38), (24, 42)], (255, 208, 79, 255))
        rect(img, int(19 * s), int(18 * s), int(21 * s), int(20 * s), (255, 204, 76, 255))
        rect(img, int(28 * s), int(18 * s), int(30 * s), int(20 * s), (255, 204, 76, 255))
    else:
        circle(img, int(24 * s), int(15 * s), int(8 * s), (90, 155, 84, 255))
        rect(img, int(15 * s), int(23 * s), int(33 * s), int(39 * s), (116, 92, 60, 255))
        polyline(img, [(31, 25), (42, 16)], (165, 165, 154, 255), int(2 * s))
    return img


def upscale(source, factor):
    return [[px for px in row for _ in range(factor)] for row in source for _ in range(factor)]


def main():
    OUT.mkdir(exist_ok=True)
    assets = {
        "grass": grass_tile(),
        "forest": forest_tile(),
        "mountain": mountain_tile(),
        "mountain_peak": mountain_peak_tile(),
        "water": water_tile(),
        "road": road_tile(),
        # Desert ground now uses the authored desert_sand_wind texture.
        "tundra": tundra_tile(),
        "marsh": marsh_tile(),
        "badlands": badlands_tile(),
        "city": city(),
        "village": village(),
        "dungeon": dungeon(),
        "player": humanoid((62, 92, 168, 255), (78, 45, 35, 255), (231, 195, 80, 255)),
        "player_battle": upscale(humanoid((62, 92, 168, 255), (78, 45, 35, 255), (231, 195, 80, 255)), 3),
        "npc_marla": humanoid((152, 76, 92, 255), (105, 64, 44, 255), (226, 177, 83, 255)),
        "npc_ren": humanoid((70, 70, 112, 255), (205, 205, 188, 255), (120, 168, 196, 255)),
        "class_knight": upscale(humanoid((92, 105, 124, 255), (75, 55, 38, 255), (205, 205, 216, 255)), 2),
        "class_mage": upscale(humanoid((91, 70, 160, 255), (50, 45, 70, 255), (226, 112, 84, 255)), 2),
        "class_ranger": upscale(humanoid((72, 132, 80, 255), (110, 73, 40, 255), (180, 130, 65, 255)), 2),
    }
    for key in ["slime", "wolf", "bat", "skeleton", "goblin", "thornling", "sand_stalker", "ice_golem", "bog_beast", "ember_imp"]:
        assets[key] = upscale(monster(key), 3)
    for key in [
        "bush",
        "flowers",
        "grass_clump",
        "pine_sapling",
        "stump",
        "cactus",
        "dry_grass",
        "snow_pine",
        "snow_mound",
        "reeds",
        "mushrooms",
        "bog_grass",
        "rocks",
    ]:
        assets[f"deco_{key}"] = decoration(key)
    if not output_path("mountain_massif").exists():
        assets["mountain_massif"] = mountain_massif_tile()
    for name, img in assets.items():
        png(output_path(name), img)

    # Keep detailed sheet cutouts for characters, mobs, and UI icons. Terrain
    # remains procedural so it stays tileable and can be layered by the renderer.
    try:
        from tools.assets.extract_sheet_assets import main as extract_sheet_assets

        extract_sheet_assets()
    except Exception as exc:
        print(f"Skipped sheet extraction: {exc}")
    try:
        from tools.assets.characters.generate_npc_assets import main as generate_npc_assets

        generate_npc_assets()
    except Exception as exc:
        print(f"Skipped NPC sheet extraction: {exc}")
    try:
        from tools.assets.characters.extract_npc_variation_assets import main as extract_npc_variation_assets

        extract_npc_variation_assets()
    except Exception as exc:
        print(f"Skipped NPC variation extraction: {exc}")
    try:
        from tools.assets.environments.generate_player_model_assets import main as generate_player_model_assets

        generate_player_model_assets()
    except Exception as exc:
        print(f"Skipped player model sheet extraction: {exc}")
    try:
        from tools.assets.monsters.generate_monster_assets import main as generate_monster_assets

        generate_monster_assets()
    except Exception as exc:
        print(f"Skipped detailed monster generation: {exc}")


if __name__ == "__main__":
    main()
