from __future__ import annotations

import os
import tkinter as tk
from pathlib import Path

from generate_assets import png

SRC = Path("assets/source/monster_sheet_new.png")
OUT = Path("assets/monsters")

# Bounding boxes in the generated sheet. Format: (left, top, right, bottom).
BOXES = {
    "thornling": (36, 185, 326, 628),
    "sand_stalker": (348, 190, 784, 632),
    "ice_golem": (833, 153, 1166, 635),
    "bog_beast": (1202, 196, 1618, 647),
    "ember_imp": (1632, 237, 1958, 632),
}


Pixel = tuple[int, int, int, int]


def is_chroma(rgb: tuple[int, int, int]) -> bool:
    r, g, b = rgb
    pure_key = (r > 215 and g < 90 and b > 185) or abs(r - 255) + g + abs(b - 255) < 118
    fringe = g < 45 and r > 58 and b > 58 and abs(r - b) < 112
    soft_fringe = g < 78 and r > 118 and b > 96 and abs(r - b) < 124
    return pure_key or fringe or soft_fringe


def despill(rgb: tuple[int, int, int]) -> tuple[int, int, int]:
    r, g, b = rgb
    if r > g + 80 and b > g + 70:
        r = max(g + 74, r - 34)
        b = max(g + 64, b - 42)
    return r, g, b


def extract_box(source: tk.PhotoImage, box: tuple[int, int, int, int]) -> list[list[Pixel]]:
    left, top, right, bottom = box
    pixels: list[list[Pixel]] = []
    for y in range(top, bottom):
        row: list[Pixel] = []
        for x in range(left, right):
            rgb = source.get(x, y)
            if is_chroma(rgb):
                row.append((0, 0, 0, 0))
                continue
            r, g, b = despill(rgb)
            row.append((r, g, b, 255))
        pixels.append(row)
    return trim_alpha(pixels, 8)


def trim_alpha(pixels: list[list[Pixel]], padding: int) -> list[list[Pixel]]:
    height = len(pixels)
    width = len(pixels[0])
    xs: list[int] = []
    ys: list[int] = []
    for y, row in enumerate(pixels):
        for x, px in enumerate(row):
            if px[3]:
                xs.append(x)
                ys.append(y)
    if not xs:
        return pixels

    left = max(0, min(xs) - padding)
    right = min(width, max(xs) + padding + 1)
    top = max(0, min(ys) - padding)
    bottom = min(height, max(ys) + padding + 1)
    return [row[left:right] for row in pixels[top:bottom]]


def main() -> None:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing generated monster sheet: {SRC}")
    OUT.mkdir(exist_ok=True)
    root = tk.Tk()
    root.withdraw()
    try:
        source = tk.PhotoImage(master=root, file=os.fspath(SRC))
        for name, box in BOXES.items():
            png(OUT / f"{name}.png", extract_box(source, box))
    finally:
        root.destroy()


if __name__ == "__main__":
    main()
