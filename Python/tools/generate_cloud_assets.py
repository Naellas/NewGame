from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path


OUT = Path("assets") / "weather"
W = 256
H = 128
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
    png.extend(chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 6, 0, 0, 0)))
    png.extend(chunk(b"IDAT", zlib.compress(bytes(raw), 9)))
    png.extend(chunk(b"IEND", b""))
    path.write_bytes(png)


def blank() -> list[list[tuple[int, int, int, int]]]:
    return [[TRANSPARENT for _ in range(W)] for _ in range(H)]


def put(pixels: list[list[tuple[int, int, int, int]]], x: int, y: int, color: tuple[int, int, int, int]) -> None:
    if 0 <= x < W and 0 <= y < H:
        pixels[y][x] = color


def ellipse(pixels: list[list[tuple[int, int, int, int]]], cx: int, cy: int, rx: int, ry: int, color: tuple[int, int, int, int]) -> None:
    for y in range(cy - ry, cy + ry + 1):
        for x in range(cx - rx, cx + rx + 1):
            nx = (x - cx) / max(1, rx)
            ny = (y - cy) / max(1, ry)
            if nx * nx + ny * ny <= 1:
                put(pixels, x, y, color)


def line(pixels: list[list[tuple[int, int, int, int]]], x1: int, y1: int, x2: int, y2: int, color: tuple[int, int, int, int]) -> None:
    steps = max(abs(x2 - x1), abs(y2 - y1), 1)
    for i in range(steps + 1):
        t = i / steps
        put(pixels, round(x1 + (x2 - x1) * t), round(y1 + (y2 - y1) * t), color)


def scallop_ring(
    pixels: list[list[tuple[int, int, int, int]]],
    center_x: int,
    center_y: int,
    radius_x: int,
    radius_y: int,
    count: int,
) -> None:
    for i in range(count):
        t = i / count
        angle = math.pi * (0.08 + 0.86 * t)
        x = center_x + int(math.cos(angle) * radius_x)
        y = center_y - int(math.sin(angle) * radius_y)
        r = 13 + (i % 3) * 3
        ellipse(pixels, x, y, r, r, (245, 238, 235, 255))
        ellipse(pixels, x + 2, y - 2, max(5, r - 6), max(5, r - 7), (255, 255, 255, 255))


def cloud(name: str, lumps: list[tuple[int, int, int, int]], base_y: int, tint: tuple[int, int, int]) -> None:
    pixels = blank()
    shade = (202, 215, 214, 255)
    blush = (247, 235, 233, 255)
    edge = (231, 229, 225, 255)
    white = (255, 255, 255, 255)
    for cx, cy, rx, ry in lumps:
        ellipse(pixels, cx, cy + 8, rx, ry, edge)
    ellipse(pixels, 128, base_y + 24, 102, 28, edge)
    ellipse(pixels, 128, base_y + 34, 94, 22, shade)
    for cx, cy, rx, ry in lumps:
        ellipse(pixels, cx, cy, rx, ry, blush)
        ellipse(pixels, cx + rx // 5, cy - ry // 5, max(8, rx - 12), max(7, ry - 10), white)
    scallop_ring(pixels, 128, base_y + 22, 104, 28, 16)
    for x in range(42, 214, 8):
        y = base_y + 44 + int(math.sin(x * 0.15) * 3)
        line(pixels, x, y, x + 5, y - 1, (185, 204, 205, 255))
    for x in range(50, 208, 14):
        y = base_y + 51 + int(math.sin(x * 0.12) * 2)
        put(pixels, x, y, (tint[0], tint[1], tint[2], 255))
    write_png(OUT / f"{name}.png", pixels)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    cloud(
        "cloud_billow_0",
        [(66, 62, 32, 22), (104, 49, 42, 30), (148, 46, 45, 32), (190, 62, 34, 22)],
        68,
        (216, 229, 230),
    )
    cloud(
        "cloud_billow_1",
        [(54, 68, 26, 19), (88, 54, 33, 25), (129, 43, 46, 34), (174, 52, 38, 28), (213, 69, 27, 18)],
        72,
        (220, 230, 231),
    )
    cloud(
        "cloud_billow_2",
        [(72, 65, 38, 25), (118, 53, 38, 27), (157, 58, 31, 22), (199, 69, 32, 20)],
        74,
        (212, 225, 226),
    )


if __name__ == "__main__":
    main()
