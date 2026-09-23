from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import struct
import zlib
from pathlib import Path


OUT = Path("assets") / "environments/terrain/biomes/forest"
SIZE = 64
TRANSPARENT = (0, 0, 0, 0)


def write_png(path: Path, pixels: list[list[tuple[int, int, int, int]]]) -> None:
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for color in row:
            raw.extend(color)

    def chunk(kind: bytes, data: bytes) -> bytes:
        checksum = zlib.crc32(kind + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", checksum)

    png = bytearray(b"\x89PNG\r\n\x1a\n")
    png.extend(chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)))
    png.extend(chunk(b"IDAT", zlib.compress(bytes(raw), 9)))
    png.extend(chunk(b"IEND", b""))
    path.write_bytes(png)


def blank() -> list[list[tuple[int, int, int, int]]]:
    return [[TRANSPARENT for _ in range(SIZE)] for _ in range(SIZE)]


def mix(dst: tuple[int, int, int, int], src: tuple[int, int, int, int]) -> tuple[int, int, int, int]:
    sr, sg, sb, sa = src
    if sa == 255:
        return src
    if sa == 0:
        return dst
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


def put(pixels: list[list[tuple[int, int, int, int]]], x: int, y: int, color: tuple[int, int, int, int]) -> None:
    if 0 <= x < SIZE and 0 <= y < SIZE:
        pixels[y][x] = mix(pixels[y][x], color)


def rect(pixels: list[list[tuple[int, int, int, int]]], x1: int, y1: int, x2: int, y2: int, color: tuple[int, int, int, int]) -> None:
    for y in range(max(0, y1), min(SIZE, y2 + 1)):
        for x in range(max(0, x1), min(SIZE, x2 + 1)):
            put(pixels, x, y, color)


def ellipse(
    pixels: list[list[tuple[int, int, int, int]]],
    cx: int,
    cy: int,
    rx: int,
    ry: int,
    color: tuple[int, int, int, int],
) -> None:
    for y in range(cy - ry, cy + ry + 1):
        for x in range(cx - rx, cx + rx + 1):
            if ((x - cx) / max(1, rx)) ** 2 + ((y - cy) / max(1, ry)) ** 2 <= 1:
                put(pixels, x, y, color)


def triangle(
    pixels: list[list[tuple[int, int, int, int]]],
    a: tuple[int, int],
    b: tuple[int, int],
    c: tuple[int, int],
    color: tuple[int, int, int, int],
) -> None:
    min_x = max(0, min(a[0], b[0], c[0]))
    max_x = min(SIZE - 1, max(a[0], b[0], c[0]))
    min_y = max(0, min(a[1], b[1], c[1]))
    max_y = min(SIZE - 1, max(a[1], b[1], c[1]))
    den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
    if den == 0:
        return
    for y in range(min_y, max_y + 1):
        for x in range(min_x, max_x + 1):
            wa = ((b[1] - c[1]) * (x - c[0]) + (c[0] - b[0]) * (y - c[1])) / den
            wb = ((c[1] - a[1]) * (x - c[0]) + (a[0] - c[0]) * (y - c[1])) / den
            wc = 1 - wa - wb
            if wa >= 0 and wb >= 0 and wc >= 0:
                put(pixels, x, y, color)


def line(
    pixels: list[list[tuple[int, int, int, int]]],
    x1: int,
    y1: int,
    x2: int,
    y2: int,
    color: tuple[int, int, int, int],
    width: int = 1,
) -> None:
    steps = max(abs(x2 - x1), abs(y2 - y1), 1)
    for i in range(steps + 1):
        t = i / steps
        x = round(x1 + (x2 - x1) * t)
        y = round(y1 + (y2 - y1) * t)
        for oy in range(-(width // 2), width // 2 + 1):
            for ox in range(-(width // 2), width // 2 + 1):
                put(pixels, x + ox, y + oy, color)


def sparkle(pixels: list[list[tuple[int, int, int, int]]], points: list[tuple[int, int]], color: tuple[int, int, int, int]) -> None:
    for x, y in points:
        put(pixels, x, y, color)
        put(pixels, x + 1, y, color)
        put(pixels, x, y + 1, color)


def shadow_color(color: tuple[int, int, int, int], amount: int) -> tuple[int, int, int, int]:
    return (max(0, color[0] - amount), max(0, color[1] - amount), max(0, color[2] - amount), color[3])


def light_color(color: tuple[int, int, int, int], amount: int) -> tuple[int, int, int, int]:
    return (min(255, color[0] + amount), min(255, color[1] + amount), min(255, color[2] + amount), color[3])


def pine(name: str, palette: tuple[tuple[int, int, int, int], ...]) -> None:
    pixels = blank()
    dark, mid, light, edge = palette
    ellipse(pixels, 32, 53, 12, 3, (20, 64, 36, 255))
    rect(pixels, 29, 33, 35, 55, (97, 61, 35, 255))
    rect(pixels, 31, 31, 33, 55, (137, 91, 49, 255))
    triangle(pixels, (32, 4), (14, 31), (50, 31), dark)
    triangle(pixels, (32, 13), (10, 42), (54, 42), mid)
    triangle(pixels, (32, 24), (13, 52), (51, 52), dark)
    triangle(pixels, (32, 9), (24, 28), (40, 28), light)
    triangle(pixels, (28, 18), (18, 39), (36, 36), (max(0, mid[0] - 12), max(0, mid[1] - 18), max(0, mid[2] - 10), 255))
    triangle(pixels, (38, 23), (30, 48), (51, 44), (min(255, mid[0] + 10), min(255, mid[1] + 15), min(255, mid[2] + 7), 255))
    line(pixels, 18, 31, 31, 27, edge, 1)
    line(pixels, 28, 42, 49, 39, edge, 1)
    line(pixels, 18, 49, 34, 46, edge, 1)
    line(pixels, 25, 23, 38, 20, (max(0, edge[0] - 5), max(0, edge[1] - 12), max(0, edge[2] - 6), 255), 1)
    line(pixels, 19, 40, 32, 37, (max(0, edge[0] - 5), max(0, edge[1] - 12), max(0, edge[2] - 6), 255), 1)
    sparkle(pixels, [(25, 20), (38, 25), (21, 38), (43, 45), (31, 31), (36, 50)], light)
    write_png(OUT / f"{name}.png", pixels)


def broadleaf(name: str, palette: tuple[tuple[int, int, int, int], ...]) -> None:
    pixels = blank()
    dark, mid, light, yellow = palette
    outline = shadow_color(dark, 36)
    deepest = shadow_color(dark, 18)
    branch = (82, 54, 32, 255)
    bark = (127, 82, 43, 255)

    if "round" in name:
        ellipse(pixels, 32, 55, 13, 4, (18, 54, 30, 210))
        rect(pixels, 29, 33, 36, 57, branch)
        rect(pixels, 31, 31, 34, 57, bark)
        line(pixels, 32, 40, 23, 32, branch, 2)
        line(pixels, 33, 39, 43, 31, branch, 2)
        for cx, cy, rx, ry in ((32, 21, 17, 15), (21, 33, 13, 12), (43, 33, 13, 12), (32, 39, 17, 12)):
            ellipse(pixels, cx, cy, rx + 3, ry + 3, outline)
        for cx, cy, rx, ry, color in (
            (32, 21, 17, 15, dark),
            (21, 33, 13, 12, mid),
            (43, 33, 13, 12, mid),
            (32, 39, 17, 12, deepest),
            (32, 29, 14, 9, shadow_color(dark, 8)),
        ):
            ellipse(pixels, cx, cy, rx, ry, color)
        ellipse(pixels, 37, 18, 8, 6, light)
        ellipse(pixels, 24, 29, 6, 5, yellow)
        ellipse(pixels, 44, 34, 6, 4, light_color(mid, 18))
        line(pixels, 18, 41, 45, 41, outline, 2)
        sparkle(pixels, [(39, 19), (31, 13), (20, 33), (45, 32), (30, 45)], light_color(light, 18))
        write_png(OUT / f"{name}.png", pixels)
        return

    ellipse(pixels, 32, 55, 15, 4, (18, 54, 30, 210))
    rect(pixels, 28, 31, 37, 57, branch)
    rect(pixels, 31, 29, 34, 57, bark)
    line(pixels, 32, 39, 21, 27, branch, 3)
    line(pixels, 33, 38, 45, 25, branch, 3)
    line(pixels, 31, 45, 18, 39, branch, 2)
    line(pixels, 34, 45, 49, 39, branch, 2)

    for cx, cy, rx, ry in ((32, 21, 20, 16), (19, 33, 16, 13), (45, 32, 17, 13), (31, 41, 21, 13)):
        ellipse(pixels, cx, cy, rx + 3, ry + 3, outline)
    for cx, cy, rx, ry, color in (
        (32, 21, 20, 16, dark),
        (19, 33, 16, 13, mid),
        (45, 32, 17, 13, mid),
        (31, 41, 21, 13, deepest),
        (31, 27, 15, 10, dark),
        (41, 39, 12, 8, shadow_color(mid, 18)),
    ):
        ellipse(pixels, cx, cy, rx, ry, color)

    ellipse(pixels, 38, 18, 10, 7, light)
    ellipse(pixels, 24, 27, 8, 5, yellow)
    ellipse(pixels, 47, 28, 7, 5, light_color(mid, 16))
    ellipse(pixels, 26, 42, 9, 5, shadow_color(dark, 8))
    line(pixels, 15, 33, 30, 37, outline, 2)
    line(pixels, 38, 34, 54, 30, outline, 2)
    line(pixels, 23, 48, 43, 47, outline, 2)
    sparkle(pixels, [(42, 21), (29, 16), (18, 36), (50, 36), (35, 13), (24, 45)], light_color(light, 18))
    write_png(OUT / f"{name}.png", pixels)


def young_tree() -> None:
    pixels = blank()
    ellipse(pixels, 32, 53, 9, 3, (18, 54, 30, 210))
    rect(pixels, 29, 30, 35, 56, (78, 52, 30, 255))
    rect(pixels, 31, 29, 33, 56, (132, 86, 44, 255))
    for x1, y1, x2, y2 in ((32, 38, 18, 31), (32, 35, 46, 28), (32, 44, 23, 47), (32, 43, 45, 45)):
        line(pixels, x1, y1, x2, y2, (78, 55, 30, 255), 2)
    for cx, cy, rx, ry in ((18, 30, 9, 6), (46, 28, 10, 7), (23, 46, 8, 6), (45, 44, 9, 6), (32, 24, 12, 9)):
        ellipse(pixels, cx, cy, rx + 2, ry + 2, (10, 49, 25, 255))
    for cx, cy, rx, ry, color in (
        (18, 30, 9, 6, (38, 113, 48, 255)),
        (46, 28, 10, 7, (73, 154, 62, 255)),
        (23, 46, 8, 6, (28, 95, 43, 255)),
        (45, 44, 9, 6, (102, 166, 67, 255)),
        (32, 24, 12, 9, (46, 126, 52, 255)),
    ):
        ellipse(pixels, cx, cy, rx, ry, color)
    ellipse(pixels, 36, 20, 5, 3, (145, 190, 79, 255))
    ellipse(pixels, 49, 27, 4, 3, (153, 198, 84, 255))
    sparkle(pixels, [(35, 21), (48, 27), (18, 28), (43, 42)], (164, 210, 95, 255))
    write_png(OUT / "deco_tree_young.png", pixels)


def forest_fern() -> None:
    pixels = blank()
    ellipse(pixels, 32, 48, 13, 2, (21, 63, 35, 255))
    for base_x in (21, 29, 37, 45):
        line(pixels, base_x, 48, base_x - 7, 36, (24, 93, 43, 255), 2)
        line(pixels, base_x, 48, base_x + 6, 35, (30, 112, 48, 255), 2)
        for k in range(4):
            y = 45 - k * 3
            line(pixels, base_x - k - 1, y, base_x - k - 6, y - 2, (72, 147, 62, 255), 1)
            line(pixels, base_x + k + 1, y, base_x + k + 6, y - 2, (83, 159, 68, 255), 1)
    sparkle(pixels, [(24, 38), (39, 34), (48, 41), (30, 44)], (126, 181, 82, 255))
    write_png(OUT / "deco_forest_fern.png", pixels)


def forest_log() -> None:
    pixels = blank()
    ellipse(pixels, 32, 48, 15, 3, (22, 60, 34, 255))
    rect(pixels, 17, 35, 49, 44, (103, 67, 38, 255))
    rect(pixels, 17, 39, 49, 47, (78, 49, 30, 255))
    ellipse(pixels, 17, 41, 5, 7, (133, 91, 52, 255))
    ellipse(pixels, 17, 41, 3, 4, (58, 37, 25, 255))
    ellipse(pixels, 49, 41, 5, 7, (118, 77, 43, 255))
    line(pixels, 22, 37, 45, 36, (151, 107, 60, 255), 1)
    line(pixels, 23, 43, 44, 44, (52, 35, 24, 255), 1)
    for x, y in ((29, 34), (35, 33), (41, 35), (25, 47)):
        ellipse(pixels, x, y, 3, 2, (51, 120, 53, 255))
    write_png(OUT / "deco_forest_log.png", pixels)


def forest_mushrooms() -> None:
    pixels = blank()
    ellipse(pixels, 32, 48, 11, 2, (22, 60, 34, 255))
    for x, y, cap in ((23, 42, (169, 64, 46, 255)), (34, 38, (202, 82, 58, 255)), (43, 44, (221, 151, 75, 255))):
        rect(pixels, x - 1, y, x + 1, 49, (224, 207, 164, 255))
        ellipse(pixels, x, y, 6, 4, cap)
        rect(pixels, x - 5, y, x + 5, y + 2, cap)
        put(pixels, x - 2, y - 1, (245, 226, 176, 255))
        put(pixels, x + 3, y + 1, (245, 226, 176, 255))
    for x in (18, 29, 39, 50):
        line(pixels, x, 49, x + 2, 43, (66, 130, 55, 255), 1)
    write_png(OUT / "deco_forest_mushrooms.png", pixels)


def forest_moss_rock() -> None:
    pixels = blank()
    ellipse(pixels, 32, 49, 12, 2, (22, 60, 34, 255))
    ellipse(pixels, 32, 41, 17, 10, (72, 86, 72, 255))
    ellipse(pixels, 24, 39, 8, 6, (95, 108, 88, 255))
    ellipse(pixels, 39, 44, 10, 6, (45, 60, 55, 255))
    line(pixels, 20, 41, 33, 36, (129, 140, 113, 255), 1)
    ellipse(pixels, 28, 35, 9, 4, (52, 126, 54, 255))
    ellipse(pixels, 38, 38, 8, 3, (75, 149, 60, 255))
    sparkle(pixels, [(27, 34), (35, 37), (43, 38)], (126, 178, 78, 255))
    write_png(OUT / "deco_forest_moss_rock.png", pixels)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    pine(
        "deco_tree_pine",
        ((10, 64, 36, 255), (22, 101, 47, 255), (91, 158, 61, 255), (7, 49, 31, 255)),
    )
    pine(
        "deco_tree_blue_pine",
        ((14, 54, 55, 255), (24, 88, 71, 255), (87, 145, 92, 255), (9, 42, 48, 255)),
    )
    broadleaf(
        "deco_tree_oak",
        ((27, 86, 39, 255), (51, 126, 52, 255), (105, 166, 72, 255), (132, 154, 62, 255)),
    )
    broadleaf(
        "deco_tree_round",
        ((34, 97, 44, 255), (72, 139, 55, 255), (126, 171, 71, 255), (86, 151, 64, 255)),
    )
    young_tree()
    forest_fern()
    forest_log()
    forest_mushrooms()
    forest_moss_rock()


if __name__ == "__main__":
    main()
