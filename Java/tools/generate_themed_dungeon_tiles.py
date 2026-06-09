from __future__ import annotations

from pathlib import Path

from generate_dungeon_tiles import SIZE, brick_surface, canvas, circle, ellipse, line, noise, png, rect, shade


OUT = Path(__file__).resolve().parents[1] / "assets" / "terrain"
Color = tuple[int, int, int, int]


def scatter(img: list[list[Color]], base: Color, seed: int, count: int = 36) -> None:
    for i in range(count):
        x = noise(i, 5, seed) % SIZE
        y = noise(i, 11, seed + 1) % SIZE
        color = shade(base, -24, -20, -16) if i % 3 == 0 else shade(base, 26, 23, 18)
        rect(img, x, y, min(SIZE, x + 1 + i % 3), min(SIZE, y + 1 + (i + 1) % 2), color)


def cave_floor() -> list[list[Color]]:
    base = (51, 55, 58, 255)
    img = canvas(base)
    for y in range(SIZE):
        for x in range(SIZE):
            n = noise(x, y, 301)
            if n < 58:
                img[y][x] = shade(base, -18, -16, -14)
            elif n > 204:
                img[y][x] = shade(base, 20, 19, 17)
    for i in range(9):
        cx = 5 + noise(i, 3, 302) % 38
        cy = 5 + noise(i, 7, 303) % 38
        ellipse(img, cx, cy, 3 + i % 4, 2 + (i + 1) % 3, shade(base, -21, -18, -13))
    scatter(img, base, 304, 44)
    return img


def cave_wall() -> list[list[Color]]:
    img = cave_floor()
    dark = (24, 27, 30, 255)
    edge = (84, 88, 92, 255)
    rect(img, 0, 0, SIZE, 10, dark)
    for i in range(7):
        x = noise(i, 13, 311) % SIZE
        y = 8 + noise(i, 17, 312) % 24
        line(img, x, y, max(0, x - 8 + i), min(SIZE - 1, y + 15), dark, 2)
        line(img, x + 1, y, min(SIZE - 1, x + 3), min(SIZE - 1, y + 10), edge)
    rect(img, 0, 38, SIZE, SIZE, (34, 37, 40, 255))
    return img


def prison_floor() -> list[list[Color]]:
    base = (49, 50, 58, 255)
    img = brick_surface(base, 321)
    iron = (26, 28, 34, 255)
    rust = (112, 70, 46, 255)
    for x in range(7, SIZE, 13):
        line(img, x, 0, x, SIZE, iron)
        if x % 2 == 1:
            rect(img, x - 1, 18, x + 2, 22, rust)
    for y in (11, 25, 39):
        line(img, 0, y, SIZE, y, (31, 33, 40, 255))
    scatter(img, base, 322, 28)
    return img


def sewer_walkway() -> list[list[Color]]:
    base = (46, 61, 55, 255)
    img = brick_surface(base, 331)
    slime = (58, 103, 73, 255)
    wet = (81, 118, 103, 255)
    for y in range(7, SIZE, 15):
        line(img, 0, y, SIZE, y + 2, (28, 39, 36, 255), 2)
    for i in range(10):
        ellipse(img, noise(i, 4, 332) % SIZE, noise(i, 8, 333) % SIZE, 4, 2, slime if i % 2 else wet)
    scatter(img, base, 334, 30)
    return img


def sewer_water() -> list[list[Color]]:
    base = (37, 83, 65, 255)
    img = canvas(base)
    for y in range(SIZE):
        for x in range(SIZE):
            n = noise(x, y, 341)
            if n < 42:
                img[y][x] = (25, 55, 48, 255)
            elif n > 204:
                img[y][x] = (68, 119, 86, 255)
    for y in range(6, SIZE, 11):
        line(img, 0, y, SIZE, y + noise(y, 3, 342) % 5 - 2, (85, 138, 100, 255))
    for i in range(7):
        circle(img, noise(i, 6, 343) % SIZE, noise(i, 12, 344) % SIZE, 1 + i % 2, (116, 157, 104, 210))
    return img


def castle_floor() -> list[list[Color]]:
    base = (74, 70, 75, 255)
    img = brick_surface(base, 351)
    mortar = (35, 34, 40, 255)
    for y in range(0, SIZE, 12):
        line(img, 0, y, SIZE, y, mortar)
    for x in range(0, SIZE, 12):
        line(img, x, 0, x, SIZE, mortar)
    for i in range(5):
        line(img, 5 + i * 9, 4 + i * 6, 16 + i * 7, 9 + i * 6, (111, 103, 104, 255))
    scatter(img, base, 352, 24)
    return img


def crypt_floor() -> list[list[Color]]:
    base = (62, 58, 66, 255)
    img = brick_surface(base, 361)
    bone = (158, 148, 124, 255)
    dark = (32, 30, 38, 255)
    for i in range(8):
        cx = 4 + noise(i, 2, 362) % 40
        cy = 6 + noise(i, 9, 363) % 36
        line(img, cx - 3, cy, cx + 3, cy, bone)
        circle(img, cx - 4, cy, 1, bone)
        circle(img, cx + 4, cy, 1, bone)
    for y in (15, 31):
        line(img, 0, y, SIZE, y, dark)
    scatter(img, base, 364, 28)
    return img


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    tiles = {
        "dungeon_cave_floor.png": cave_floor(),
        "dungeon_cave_wall.png": cave_wall(),
        "dungeon_prison_floor.png": prison_floor(),
        "dungeon_sewer_walkway.png": sewer_walkway(),
        "dungeon_sewer_water.png": sewer_water(),
        "dungeon_castle_floor.png": castle_floor(),
        "dungeon_crypt_floor.png": crypt_floor(),
    }
    for filename, image in tiles.items():
        png(OUT / filename, image)
        print(f"Wrote {OUT / filename}")


if __name__ == "__main__":
    main()
