from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import math
import struct
import zlib
from pathlib import Path


OUT = Path("assets") / "effects/weather"
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


def blend(
    pixels: list[list[tuple[int, int, int, int]]],
    x: int,
    y: int,
    color: tuple[int, int, int, int],
) -> None:
    if not (0 <= x < W and 0 <= y < H):
        return
    sr, sg, sb, sa = color
    if sa <= 0:
        return
    dr, dg, db, da = pixels[y][x]
    src = sa / 255
    dst = da / 255
    out = src + dst * (1 - src)
    if out <= 0:
        pixels[y][x] = TRANSPARENT
        return
    pixels[y][x] = (
        int((sr * src + dr * dst * (1 - src)) / out),
        int((sg * src + dg * dst * (1 - src)) / out),
        int((sb * src + db * dst * (1 - src)) / out),
        int(out * 255),
    )


def ellipse(
    pixels: list[list[tuple[int, int, int, int]]],
    cx: int,
    cy: int,
    rx: int,
    ry: int,
    color: tuple[int, int, int, int],
    *,
    softness: float = 0.18,
) -> None:
    pad_x = max(2, int(rx * softness) + 1)
    pad_y = max(2, int(ry * softness) + 1)
    for y in range(cy - ry - pad_y, cy + ry + pad_y + 1):
        for x in range(cx - rx - pad_x, cx + rx + pad_x + 1):
            nx = (x - cx) / max(1, rx)
            ny = (y - cy) / max(1, ry)
            dist = math.sqrt(nx * nx + ny * ny)
            if dist > 1 + softness:
                continue
            fade = 1.0 if dist <= 1 else max(0.0, 1 - (dist - 1) / softness)
            alpha = int(color[3] * fade)
            blend(pixels, x, y, (color[0], color[1], color[2], alpha))


def line(pixels: list[list[tuple[int, int, int, int]]], x1: int, y1: int, x2: int, y2: int, color: tuple[int, int, int, int]) -> None:
    steps = max(abs(x2 - x1), abs(y2 - y1), 1)
    for i in range(steps + 1):
        t = i / steps
        blend(pixels, round(x1 + (x2 - x1) * t), round(y1 + (y2 - y1) * t), color)


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
        rx = 12 + (i % 3) * 3
        ry = max(8, rx - 4)
        ellipse(pixels, x, y, rx, ry, (250, 249, 245, 188), softness=0.34)
        ellipse(pixels, x + 2, y - 2, max(5, rx - 6), max(5, ry - 5), (255, 255, 255, 150), softness=0.38)


def cloud(name: str, lumps: list[tuple[int, int, int, int]], base_y: int, tint: tuple[int, int, int]) -> None:
    pixels = blank()
    shade = (224, 234, 235, 170)
    blush = (252, 248, 246, 210)
    edge = (241, 241, 237, 178)
    white = (255, 255, 255, 232)
    for cx, cy, rx, ry in lumps:
        ellipse(pixels, cx, cy + 7, rx + 3, ry + 2, edge, softness=0.30)
    ellipse(pixels, 128, base_y + 20, 106, 24, edge, softness=0.34)
    ellipse(pixels, 128, base_y + 29, 100, 18, shade, softness=0.42)
    for cx, cy, rx, ry in lumps:
        ellipse(pixels, cx, cy, rx, ry, blush, softness=0.28)
        ellipse(pixels, cx + rx // 5, cy - ry // 5, max(8, rx - 11), max(7, ry - 9), white, softness=0.36)
    scallop_ring(pixels, 128, base_y + 20, 104, 25, 19)
    for cx, cy, rx, ry in lumps:
        ellipse(pixels, cx - rx // 3, cy + ry // 3, max(10, rx // 2), max(5, ry // 3), (255, 255, 255, 78), softness=0.55)
    for x in range(42, 214, 10):
        y = base_y + 38 + int(math.sin(x * 0.15) * 2)
        line(pixels, x, y, x + 6, y - 1, (211, 225, 226, 145))
    for x in range(50, 208, 16):
        y = base_y + 45 + int(math.sin(x * 0.12) * 2)
        blend(pixels, x, y, (tint[0], tint[1], tint[2], 150))
    write_png(OUT / f"{name}.png", pixels)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    cloud(
        "cloud_billow_0",
        [(54, 64, 27, 16), (91, 55, 34, 23), (130, 49, 38, 26), (170, 55, 35, 23), (209, 66, 26, 15)],
        66,
        (232, 240, 241),
    )
    cloud(
        "cloud_billow_1",
        [(45, 68, 23, 14), (78, 58, 30, 20), (116, 50, 38, 27), (155, 46, 35, 24), (192, 57, 32, 20), (224, 70, 22, 13)],
        68,
        (235, 242, 243),
    )
    cloud(
        "cloud_billow_2",
        [(61, 65, 32, 19), (101, 55, 35, 23), (140, 58, 32, 21), (177, 63, 30, 18), (213, 71, 24, 13)],
        70,
        (231, 240, 241),
    )


if __name__ == "__main__":
    main()
