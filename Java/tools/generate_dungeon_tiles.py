from __future__ import annotations

import struct
import zlib
from pathlib import Path

SIZE = 48
OUT = Path("assets") / "terrain"

Color = tuple[int, int, int, int]


def png(path: Path, pixels: list[list[Color]]) -> None:
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


def canvas(color: Color) -> list[list[Color]]:
    return [[color for _ in range(SIZE)] for _ in range(SIZE)]


def shade(color: Color, dr: int = 0, dg: int = 0, db: int = 0) -> Color:
    r, g, b, a = color
    return (max(0, min(255, r + dr)), max(0, min(255, g + dg)), max(0, min(255, b + db)), a)


def noise(x: int, y: int, seed: int) -> int:
    value = (x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)
    value = (value ^ (value >> 13)) * 1274126177
    return (value ^ (value >> 16)) & 255


def set_px(img: list[list[Color]], x: int, y: int, color: Color) -> None:
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img[y][x] = color


def rect(img: list[list[Color]], x1: int, y1: int, x2: int, y2: int, color: Color) -> None:
    for y in range(max(0, y1), min(SIZE, y2)):
        for x in range(max(0, x1), min(SIZE, x2)):
            img[y][x] = color


def circle(img: list[list[Color]], cx: int, cy: int, r: int, color: Color) -> None:
    rr = r * r
    for y in range(cy - r, cy + r + 1):
        for x in range(cx - r, cx + r + 1):
            if (x - cx) ** 2 + (y - cy) ** 2 <= rr:
                set_px(img, x, y, color)


def ellipse(img: list[list[Color]], cx: int, cy: int, rx: int, ry: int, color: Color) -> None:
    for y in range(cy - ry, cy + ry + 1):
        for x in range(cx - rx, cx + rx + 1):
            nx = (x - cx) / max(1, rx)
            ny = (y - cy) / max(1, ry)
            if nx * nx + ny * ny <= 1:
                set_px(img, x, y, color)


def line(img: list[list[Color]], x1: int, y1: int, x2: int, y2: int, color: Color, width: int = 1) -> None:
    steps = max(abs(x2 - x1), abs(y2 - y1), 1)
    for i in range(steps + 1):
        t = i / steps
        x = int(x1 + (x2 - x1) * t)
        y = int(y1 + (y2 - y1) * t)
        rect(img, x - width // 2, y - width // 2, x + width, y + width, color)


def brick_surface(base: Color, seed: int) -> list[list[Color]]:
    img = canvas(base)
    for y in range(SIZE):
        for x in range(SIZE):
            n = noise(x, y, seed)
            if n < 44:
                img[y][x] = shade(base, -19, -17, -14)
            elif n > 214:
                img[y][x] = shade(base, 20, 17, 14)
            elif n > 178:
                img[y][x] = shade(base, 9, 7, 8)
    return img


def dungeon_floor() -> list[list[Color]]:
    base = (58, 54, 72, 255)
    img = brick_surface(base, 21)
    mortar = (31, 29, 41, 255)
    light = (93, 86, 111, 255)
    warm = (109, 82, 66, 255)
    rows = [0, 9, 19, 31, 48]
    for row, (y1, y2) in enumerate(zip(rows, rows[1:])):
        offset = 0 if row % 2 == 0 else 12
        line(img, 0, y1, SIZE, y1, mortar)
        for x in range(-offset, SIZE + 20, 18):
            wobble = noise(x, y1, 22) % 3 - 1
            line(img, x, y1, x + wobble, y2, mortar)
            line(img, x + 1, y1 + 1, x + 1 + wobble, min(SIZE - 1, y2 - 1), light if row % 3 == 0 else (45, 40, 55, 255))
    for i in range(38):
        x = noise(i, 3, 23) % SIZE
        y = noise(i, 7, 24) % SIZE
        color = light if i % 3 else (35, 32, 44, 255)
        rect(img, x, y, x + 2 + i % 3, y + 1, color)
    for points in (((9, 30), (17, 35), (26, 34)), ((31, 12), (36, 15), (41, 14)), ((20, 5), (23, 8), (29, 7))):
        for a, b in zip(points, points[1:]):
            line(img, a[0], a[1], b[0], b[1], (25, 23, 34, 255))
            set_px(img, b[0], b[1] - 1, warm)
    return img


def dungeon_wall() -> list[list[Color]]:
    base = (54, 51, 67, 255)
    img = brick_surface(base, 41)
    dark = (25, 24, 35, 255)
    mid = (74, 70, 88, 255)
    edge = (102, 89, 88, 255)
    cap = (86, 75, 82, 255)
    rect(img, 0, 0, SIZE, 6, dark)
    rect(img, 2, 6, SIZE - 2, 13, cap)
    line(img, 2, 13, SIZE - 2, 13, (121, 86, 65, 255))
    for y in (8, 22, 35):
        line(img, 2, y, SIZE - 2, y, dark)
    for y in (14, 28, 40):
        offset = 0 if y % 4 else 11
        for x in range(-offset, SIZE, 17):
            line(img, x, y - 13, x, y, dark)
    rect(img, 5, 12, 11, SIZE, mid)
    rect(img, SIZE - 11, 12, SIZE - 5, SIZE, (40, 38, 51, 255))
    line(img, 11, 12, 11, SIZE, edge)
    line(img, SIZE - 12, 12, SIZE - 12, SIZE, dark)
    for i in range(16):
        x = 3 + noise(i, 2, 44) % 42
        y = 14 + noise(i, 6, 45) % 29
        rect(img, x, y, x + 2, y + 1, (94, 83, 96, 255) if i % 2 else (31, 29, 40, 255))
    return img


def dungeon_entrance() -> list[list[Color]]:
    img = dungeon_floor()
    shadow = (21, 20, 31, 255)
    stone = (70, 66, 82, 255)
    stone_light = (104, 94, 108, 255)
    warm = (232, 128, 61, 255)
    violet = (153, 92, 245, 255)
    rect(img, 6, 16, 42, 44, shadow)
    ellipse(img, 24, 17, 18, 15, shadow)
    rect(img, 3, 36, 45, 43, (48, 45, 60, 255))
    for x in range(5, 43, 7):
        rect(img, x, 36, x + 5, 42, stone)
        line(img, x, 36, x + 5, 36, stone_light)
    for r in range(18, 25, 3):
        ellipse(img, 24, 18, r, r - 5, stone)
        ellipse(img, 24, 18, r - 3, r - 8, shadow)
    rect(img, 11, 18, 16, 38, stone)
    rect(img, 32, 18, 37, 38, stone)
    line(img, 16, 19, 16, 37, stone_light)
    line(img, 31, 19, 31, 37, (33, 31, 43, 255))
    for x in (10, 38):
        rect(img, x - 1, 22, x + 2, 29, (44, 31, 28, 255))
        circle(img, x, 20, 4, warm)
        circle(img, x, 20, 2, (255, 207, 92, 255))
    circle(img, 24, 27, 5, violet)
    circle(img, 24, 27, 2, (216, 176, 255, 255))
    rect(img, 20, 30, 28, 39, shadow)
    return img


def main() -> None:
    png(OUT / "dungeon_floor.png", dungeon_floor())
    png(OUT / "dungeon_wall.png", dungeon_wall())
    png(OUT / "dungeon.png", dungeon_entrance())


if __name__ == "__main__":
    main()
