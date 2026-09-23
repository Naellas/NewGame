from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


ROOT = JAVA_ROOT
ROAD_DIR = ROOT / "assets" / "environments/terrain/roads"
SOURCE_DIR = ROOT / "assets" / "source"

TILE = 48
SCALE = 4
SIZE = TILE * SCALE
CENTER = SIZE / 2.0


def clamp(value: float, lo: int = 0, hi: int = 255) -> int:
    return max(lo, min(hi, int(round(value))))


def smoothstep(edge0: float, edge1: float, value: float) -> float:
    if edge0 == edge1:
        return 1.0 if value >= edge1 else 0.0
    t = max(0.0, min(1.0, (value - edge0) / (edge1 - edge0)))
    return t * t * (3.0 - 2.0 * t)


def hash_noise(x: int, y: int, seed: int) -> float:
    n = x * 374761393 + y * 668265263 + seed * 1442695041
    n = (n ^ (n >> 13)) * 1274126177
    n = n ^ (n >> 16)
    return (n & 0xffff) / 0xffff


def signed_rect_distance(x: float, y: float, rect: tuple[float, float, float, float]) -> float:
    left, top, right, bottom = rect
    dx = max(left - x, 0.0, x - right)
    dy = max(top - y, 0.0, y - bottom)
    outside = math.hypot(dx, dy)
    if dx > 0.0 or dy > 0.0:
        return outside
    return -min(x - left, right - x, y - top, bottom - y)


def signed_circle_distance(x: float, y: float, cx: float, cy: float, radius: float) -> float:
    return math.hypot(x - cx, y - cy) - radius


def road_rects(bits: int) -> list[tuple[float, float, float, float]]:
    half = 16.0 * SCALE
    rects = [(CENTER - half, CENTER - half, CENTER + half, CENTER + half)]
    if bits & 1:
        rects.append((CENTER - half, -2 * SCALE, CENTER + half, CENTER))
    if bits & 2:
        rects.append((CENTER - half, CENTER, CENTER + half, SIZE + 2 * SCALE))
    if bits & 4:
        rects.append((-2 * SCALE, CENTER - half, CENTER, CENTER + half))
    if bits & 8:
        rects.append((CENTER, CENTER - half, SIZE + 2 * SCALE, CENTER + half))
    return rects


def road_signed_distance(x: float, y: float, bits: int) -> float:
    if bits == 0:
        return signed_circle_distance(x, y, CENTER, CENTER, 15.0 * SCALE)
    return min(signed_rect_distance(x, y, rect) for rect in road_rects(bits))


def edge_wobble(x: float, y: float, bits: int) -> float:
    seed = bits * 101 + 7919
    coarse = hash_noise(int(x // (12 * SCALE)), int(y // (12 * SCALE)), seed)
    medium = hash_noise(int(x // (6 * SCALE)), int(y // (6 * SCALE)), seed + 37)
    wave = math.sin(x * 0.041 + seed * 0.07) + math.cos(y * 0.047 - seed * 0.05)

    # Keep connection edges stable so adjacent tiles meet cleanly.
    edge_lock = 1.0
    if (bits & 1) and y < 8 * SCALE:
        edge_lock = min(edge_lock, y / (8 * SCALE))
    if (bits & 2) and y > SIZE - 8 * SCALE:
        edge_lock = min(edge_lock, (SIZE - y) / (8 * SCALE))
    if (bits & 4) and x < 8 * SCALE:
        edge_lock = min(edge_lock, x / (8 * SCALE))
    if (bits & 8) and x > SIZE - 8 * SCALE:
        edge_lock = min(edge_lock, (SIZE - x) / (8 * SCALE))
    edge_lock = max(0.0, min(1.0, edge_lock))
    return ((coarse - 0.5) * 4.8 + (medium - 0.5) * 2.4 + wave * 0.95) * SCALE * edge_lock


def pixel_color(x: int, y: int, bits: int, signed_distance: float, alpha: float) -> tuple[int, int, int, int]:
    seed = bits * 97 + 1309
    fine = (hash_noise(x, y, seed + 11) - 0.5) * 8.0
    patch = (hash_noise(x // (9 * SCALE), y // (9 * SCALE), seed + 31) - 0.5) * 7.0
    soft_edge = smoothstep(-4.0 * SCALE, 9.0 * SCALE, signed_distance)
    r = 153 + fine + patch - soft_edge * 12
    g = 113 + fine * 0.55 + patch * 0.45 - soft_edge * 8
    b = 67 + fine * 0.28 + patch * 0.22 - soft_edge * 7
    return clamp(r), clamp(g), clamp(b), clamp(alpha)


def road_overlay(bits: int) -> Image.Image:
    seed = bits * 97 + 1309
    feather = 6.4 * SCALE
    base = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pixels = base.load()
    texture_path = ROAD_DIR / "road.png"
    texture = None
    if texture_path.exists():
        texture = Image.open(texture_path).convert("RGBA").resize((SIZE, SIZE), Image.Resampling.BICUBIC)
        texture_pixels = texture.load()
    else:
        texture_pixels = None

    for y in range(SIZE):
        for x in range(SIZE):
            sd = road_signed_distance(x + 0.5, y + 0.5, bits)
            sd += edge_wobble(x + 0.5, y + 0.5, bits)
            alpha = (1.0 - smoothstep(-1.5 * SCALE, feather, sd)) * 222.0
            if alpha <= 1.0:
                continue
            if texture_pixels is None:
                pixels[x, y] = pixel_color(x, y, bits, sd, alpha)
            else:
                tr, tg, tb, _ = texture_pixels[x, y]
                fine = (hash_noise(x, y, seed + 11) - 0.5) * 5.0
                soft_edge = smoothstep(-4.0 * SCALE, 9.0 * SCALE, sd)
                pixels[x, y] = (
                    clamp(tr + fine - soft_edge * 12),
                    clamp(tg + fine * 0.6 - soft_edge * 8),
                    clamp(tb + fine * 0.35 - soft_edge * 6),
                    clamp(alpha),
                )

    draw = ImageDraw.Draw(base, "RGBA")
    rng = random.Random(seed)
    for _ in range(14):
        x = rng.randrange(4 * SCALE, SIZE - 4 * SCALE)
        y = rng.randrange(4 * SCALE, SIZE - 4 * SCALE)
        sd = road_signed_distance(x, y, bits)
        if sd > -2.0 * SCALE:
            continue
        w = rng.choice((1, 1, 2)) * SCALE
        h = rng.choice((1, 1, 1, 2)) * SCALE
        color = rng.choice((
            (100, 72, 45, 24),
            (226, 184, 116, 22),
            (137, 99, 61, 26),
            (190, 146, 91, 20),
        ))
        if rng.random() < 0.45:
            draw.line((x - w, y, x + w, y + rng.randrange(-2, 3)), fill=color, width=max(1, SCALE // 2))
        else:
            draw.ellipse((x - w, y - h, x + w, y + h), fill=color)

    alpha = base.getchannel("A").filter(ImageFilter.GaussianBlur(0.28 * SCALE))
    base.putalpha(alpha)
    return base.resize((TILE, TILE), Image.Resampling.LANCZOS)


def write_preview() -> None:
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)
    margin = 4
    sheet = Image.new("RGBA", (4 * TILE + 5 * margin, 4 * TILE + 5 * margin), (34, 39, 35, 255))
    for bits in range(16):
        img = Image.open(ROAD_DIR / f"road_overlay_{bits:02x}.png").convert("RGBA")
        x = margin + (bits % 4) * (TILE + margin)
        y = margin + (bits // 4) * (TILE + margin)
        sheet.alpha_composite(img, (x, y))
    sheet.save(SOURCE_DIR / "preview-road-autotiles.png")


def write_junction_filler() -> None:
    texture_path = ROAD_DIR / "road.png"
    if texture_path.exists():
        texture = Image.open(texture_path).convert("RGBA").resize((SIZE, SIZE), Image.Resampling.BICUBIC)
    else:
        texture = Image.new("RGBA", (SIZE, SIZE), (153, 113, 67, 255))
    filler = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pixels = filler.load()
    texture_pixels = texture.load()
    feather = 4.0 * SCALE
    half = 14.0 * SCALE
    seed = 21569
    for y in range(SIZE):
        for x in range(SIZE):
            dx = abs(x + 0.5 - CENTER) - half
            dy = abs(y + 0.5 - CENTER) - half
            outside = max(dx, dy)
            wobble = (hash_noise(x // (7 * SCALE), y // (7 * SCALE), seed) - 0.5) * 2.0 * SCALE
            alpha = (1.0 - smoothstep(-1.0 * SCALE, feather, outside + wobble)) * 214.0
            if alpha <= 1.0:
                continue
            tr, tg, tb, _ = texture_pixels[x, y]
            edge = smoothstep(-2.0 * SCALE, feather, outside + wobble)
            pixels[x, y] = (
                clamp(tr - edge * 9),
                clamp(tg - edge * 6),
                clamp(tb - edge * 5),
                clamp(alpha),
            )
    alpha = filler.getchannel("A").filter(ImageFilter.GaussianBlur(0.25 * SCALE))
    filler.putalpha(alpha)
    filler.resize((TILE, TILE), Image.Resampling.LANCZOS).save(ROAD_DIR / "road_junction_filler.png")


def main() -> None:
    ROAD_DIR.mkdir(parents=True, exist_ok=True)
    for bits in range(16):
        road_overlay(bits).save(ROAD_DIR / f"road_overlay_{bits:02x}.png")
    write_junction_filler()
    write_preview()


if __name__ == "__main__":
    main()
