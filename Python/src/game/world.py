from __future__ import annotations

import math
from dataclasses import dataclass
from random import choice, random, sample

from .config import MONSTER_GROUP_CHANCES, MONSTER_SPAWN_CHANCES
from .entities import NPC

OVERWORLD_ID = "overworld"
DEFAULT_WORLD_SEED = 0
_WORLD_SEED = DEFAULT_WORLD_SEED
HOUSE_SPAWN = (11, 14)
HOUSE_EXITS = ((11, 15), (12, 15))
START_POSITION = (112, 158)
CITY_GATE_CENTERS = {
    "city_riverside": (82, 105),
    "city_archive": (152, 145),
    "city_highwall": (205, 78),
    "city_belltower": (228, 185),
    "city_sanctum": (150, 230),
}
CITY_SURFACE_EXITS = {
    "city_riverside": (82, 107),
    "city_archive": (152, 147),
    "city_highwall": (205, 80),
    "city_belltower": (228, 187),
    "city_sanctum": (150, 232),
}
VILLAGE_CENTERS = {
    "village_oakhaven": (112, 158),
    "village_snowrest": (83, 62),
    "village_dunewick": (102, 245),
    "village_mireford": (240, 153),
}
VILLAGE_SURFACE_EXITS = {
    "village_oakhaven": (112, 159),
    "village_snowrest": (83, 63),
    "village_dunewick": (102, 246),
    "village_mireford": (240, 154),
}
DUNGEON_ENTRANCES = {
    "dungeon_stonegate_1": (196, 62),
    "dungeon_miredepth_1": (255, 177),
    "dungeon_frosthollow_1": (72, 218),
    "dungeon_blackvault_1": (194, 235),
}


@dataclass(frozen=True)
class CityBuilding:
    key: str
    x1: int
    y1: int
    x2: int
    y2: int
    style: str = "house"
    palette: int = 0

    @property
    def width(self) -> int:
        return self.x2 - self.x1 + 1

    @property
    def depth(self) -> int:
        return self.y2 - self.y1 + 1

    @property
    def anchor(self) -> tuple[int, int]:
        return self.x1, self.y1

    def contains(self, x: int, y: int) -> bool:
        return self.x1 <= x <= self.x2 and self.y1 <= y <= self.y2


@dataclass(frozen=True)
class LocationPatch:
    kind: str
    cx: int
    cy: int
    rx: int
    ry: int
    salt: int

    def contains(self, x: int, y: int) -> bool:
        nx = (x - self.cx) / max(1.0, self.rx)
        ny = (y - self.cy) / max(1.0, self.ry)
        ripple = math.sin((x + self.salt) * 0.47) * 0.06 + math.cos((y - self.salt) * 0.39) * 0.05
        return nx * nx + ny * ny <= 1.0 + ripple


OVERWORLD_LOCATIONS: tuple[LocationPatch, ...] = ()


def _build_overworld(seed: int = DEFAULT_WORLD_SEED) -> list[str]:
    cols, rows = 300, 300
    seed_salt = seed & 0xFFFFFFFF
    grid = [["w" for _ in range(cols)] for _ in range(rows)]
    land: set[tuple[int, int]] = set()

    def island_score(x: int, y: int) -> float:
        phase_a = (seed_salt % 997) * 0.001
        phase_b = ((seed_salt >> 10) % 997) * 0.001
        nx = (x - 151) / 131.0
        ny = (y - 152) / 124.0
        score = 1.0 - (nx * nx + ny * ny)
        score += 0.14 * math.sin(x * 0.051 + phase_a * 7.0) + 0.11 * math.cos(y * 0.067 - phase_b * 6.0)
        score += 0.08 * math.sin((x + y) * 0.037 + phase_b * 9.0) - 0.07 * math.cos((x - y) * 0.049 + phase_a * 5.0)
        score += 0.05 * math.sin((x * 2 + y) * 0.028 + phase_a * 11.0)
        score += 0.06 * (math.sin((x * 0.031 + y * 0.017) + phase_b * 13.0))
        if x < 18 or x > cols - 16 or y < 14 or y > rows - 14:
            score -= 0.45
        if x < 48 and y < 70:
            score -= 0.35
        if x > 250 and y < 70:
            score -= 0.22
        if x > 258 and y > 238:
            score -= 0.30
        return score

    for y in range(rows):
        for x in range(cols):
            if island_score(x, y) > 0.04:
                land.add((x, y))
                grid[y][x] = "g"

    for y in range(rows):
        for x in range(cols):
            if (x, y) not in land:
                continue
            neighbors = (
                (x + 1, y),
                (x - 1, y),
                (x, y + 1),
                (x, y - 1),
                (x + 1, y + 1),
                (x - 1, y - 1),
            )
            if any(point not in land for point in neighbors):
                grid[y][x] = "s"

    def fill_rect(x1: int, y1: int, x2: int, y2: int, tile: str) -> None:
        for y in range(max(0, y1), min(rows, y2 + 1)):
            for x in range(max(0, x1), min(cols, x2 + 1)):
                if tile == "w" or (x, y) in land:
                    grid[y][x] = tile

    def fill_oval(cx: int, cy: int, rx: int, ry: int, tile: str) -> None:
        for y in range(max(0, cy - ry), min(rows, cy + ry + 1)):
            for x in range(max(0, cx - rx), min(cols, cx + rx + 1)):
                nx = (x - cx) / max(1.0, rx)
                ny = (y - cy) / max(1.0, ry)
                if nx * nx + ny * ny <= 1.0 and (tile == "w" or (x, y) in land):
                    grid[y][x] = tile

    def raise_land_oval(cx: int, cy: int, rx: int, ry: int, tile: str) -> None:
        for y in range(max(0, cy - ry), min(rows, cy + ry + 1)):
            for x in range(max(0, cx - rx), min(cols, cx + rx + 1)):
                nx = (x - cx) / max(1.0, rx)
                ny = (y - cy) / max(1.0, ry)
                if nx * nx + ny * ny <= 1.0:
                    land.add((x, y))
                    grid[y][x] = tile

    def raise_land_path(points: list[tuple[int, int]], width: int, tile: str) -> None:
        x, y = points[0]

        def mark_ground(px: int, py: int) -> None:
            for oy in range(-width, width + 1):
                for ox in range(-width, width + 1):
                    if abs(ox) + abs(oy) > width + 1:
                        continue
                    tx, ty = px + ox, py + oy
                    if 0 <= tx < cols and 0 <= ty < rows:
                        land.add((tx, ty))
                        grid[ty][tx] = tile

        mark_ground(x, y)
        for tx, ty in points[1:]:
            guard = 0
            while (x, y) != (tx, ty) and guard < 300:
                guard += 1
                sx = 1 if tx > x else -1 if tx < x else 0
                sy = 1 if ty > y else -1 if ty < y else 0
                seed = (x * 67280421310721 + y * 1315423911 + guard * 31) & 3
                if sx and (not sy or seed):
                    x += sx
                elif sy:
                    y += sy
                mark_ground(x, y)

    def terrain_hash(x: int, y: int, salt: int = 0) -> int:
        value = (x * 374761393 + y * 668265263 + (salt + seed_salt * 17) * 1442695040888963407) & 0xFFFFFFFF
        value = (value ^ (value >> 13)) * 1274126177
        return (value ^ (value >> 16)) & 0xFFFFFFFF

    def terrain_noise(x: float, y: float, scale: int, salt: int = 0) -> float:
        gx = math.floor(x / scale)
        gy = math.floor(y / scale)
        tx = (x / scale) - gx
        ty = (y / scale) - gy
        tx = tx * tx * (3.0 - 2.0 * tx)
        ty = ty * ty * (3.0 - 2.0 * ty)

        def corner(ix: int, iy: int) -> float:
            return (terrain_hash(ix, iy, salt) & 1023) / 1023.0

        top = corner(gx, gy) * (1.0 - tx) + corner(gx + 1, gy) * tx
        bottom = corner(gx, gy + 1) * (1.0 - tx) + corner(gx + 1, gy + 1) * tx
        return top * (1.0 - ty) + bottom * ty

    def organic_metric(x: int, y: int, cx: int, cy: int, rx: int, ry: int, salt: int) -> float:
        warp_x = (terrain_noise(x, y, 19, salt + 11) - 0.5) * rx * 0.32
        warp_y = (terrain_noise(x, y, 17, salt + 23) - 0.5) * ry * 0.32
        nx = (x - cx + warp_x) / max(1.0, rx)
        ny = (y - cy + warp_y) / max(1.0, ry)
        angle = math.atan2(ny, nx)
        radial = math.hypot(nx, ny)
        scallop = math.sin(angle * 3.0 + salt * 0.17) * 0.10 + math.cos(angle * 5.0 - salt * 0.11) * 0.07
        grain = (terrain_noise(x, y, 11, salt + 37) - 0.5) * 0.24
        fine = (terrain_noise(x, y, 5, salt + 41) - 0.5) * 0.10
        return radial - scallop - grain - fine

    def paint_biome(cx: int, cy: int, rx: int, ry: int, core: str, edge: str, salt: int = 0) -> None:
        salt = salt or (cx * 31 + cy * 17 + ord(core) * 43)
        margin = max(8, int(max(rx, ry) * 0.28))
        for y in range(max(0, cy - ry - margin), min(rows, cy + ry + margin + 1)):
            for x in range(max(0, cx - rx - margin), min(cols, cx + rx + margin + 1)):
                if (x, y) not in land:
                    continue
                dist = organic_metric(x, y, cx, cy, rx, ry, salt)
                if dist <= 0.80:
                    grid[y][x] = core
                elif dist <= 1.10:
                    roll = terrain_hash(x, y, salt + 53) & 255
                    chance = int((1.10 - dist) * 720)
                    if roll < chance:
                        grid[y][x] = edge

    def paint_biome_patch(cx: int, cy: int, rx: int, ry: int, tile: str, salt: int) -> None:
        for y in range(max(0, cy - ry - 4), min(rows, cy + ry + 5)):
            for x in range(max(0, cx - rx - 4), min(cols, cx + rx + 5)):
                if (x, y) not in land or grid[y][x] in {"w", "r", "c", "u", "d"}:
                    continue
                if organic_metric(x, y, cx, cy, rx, ry, salt) <= 0.92:
                    grid[y][x] = tile

    def seed_biome_granules() -> None:
        patch_specs = [
            ("f", 118, 128, 118, 80, 36, 10, 4),
            ("f", 176, 111, 76, 62, 30, 8, 5),
            ("v", 212, 145, 75, 61, 25, 7, 6),
            ("s", 87, 226, 65, 58, 30, 8, 7),
            ("b", 178, 225, 55, 53, 18, 6, 8),
            ("n", 68, 67, 65, 49, 24, 7, 9),
            ("m", 143, 176, 78, 68, 24, 6, 10),
        ]
        for tile, base_x, base_y, spread_x, spread_y, count, max_rx, salt in patch_specs:
            for i in range(count):
                hx = terrain_hash(i, salt, 3)
                hy = terrain_hash(i, salt, 5)
                cx = base_x + int((hx % (spread_x * 2 + 1)) - spread_x)
                cy = base_y + int((hy % (spread_y * 2 + 1)) - spread_y)
                rx = 2 + (terrain_hash(i, salt, 7) % max_rx)
                ry = 2 + (terrain_hash(i, salt, 11) % max(3, max_rx - 1))
                paint_biome_patch(cx, cy, rx, ry, tile, salt * 101 + i)

    def paint_pond(cx: int, cy: int, rx: int, ry: int, salt: int) -> None:
        for y in range(max(0, cy - ry - 4), min(rows, cy + ry + 5)):
            for x in range(max(0, cx - rx - 4), min(cols, cx + rx + 5)):
                if (x, y) not in land:
                    continue
                dist = organic_metric(x, y, cx, cy, rx, ry, salt)
                if dist <= 0.76:
                    grid[y][x] = "w"
                elif dist <= 1.03 and grid[y][x] in {"g", "f", "s"}:
                    grid[y][x] = "v" if terrain_hash(x, y, salt + 71) % 100 < 45 else "g"

    def seed_midland_ponds() -> None:
        anchors = [
            (125, 151, 6, 4),
            (139, 134, 5, 3),
            (153, 171, 7, 5),
            (171, 143, 4, 4),
            (183, 162, 5, 4),
            (132, 184, 4, 3),
            (158, 117, 5, 4),
            (106, 137, 4, 3),
            (194, 135, 4, 3),
        ]
        for i, (cx, cy, rx, ry) in enumerate(anchors):
            jitter_x = (terrain_hash(cx, cy, i) % 7) - 3
            jitter_y = (terrain_hash(cy, cx, i) % 7) - 3
            paint_pond(cx + jitter_x, cy + jitter_y, rx, ry, 801 + i * 37)

    def paint_water_body(cx: int, cy: int, rx: int, ry: int, salt: int) -> None:
        for y in range(max(0, cy - ry - 6), min(rows, cy + ry + 7)):
            for x in range(max(0, cx - rx - 6), min(cols, cx + rx + 7)):
                dist = organic_metric(x, y, cx, cy, rx, ry, salt)
                if dist <= 0.90 and ((x, y) in land or grid[y][x] == "w"):
                    grid[y][x] = "w"

    def shifted(value: int, span: int, salt: int) -> int:
        return value + int(terrain_hash(value, salt, 909) % (span * 2 + 1)) - span

    def road_path(points: list[tuple[int, int]]) -> None:
        def mark_road(rx: int, ry: int) -> None:
            if not (0 <= rx < cols and 0 <= ry < rows) or (rx, ry) not in land:
                return
            mountain_crossing = grid[ry][rx] in {"m", "q"}
            grid[ry][rx] = "r"
            if not mountain_crossing:
                return
            for ox, oy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
                px, py = rx + ox, ry + oy
                if 0 <= px < cols and 0 <= py < rows and grid[py][px] == "m":
                    grid[py][px] = "q"

        x, y = points[0]
        mark_road(x, y)
        for tx, ty in points[1:]:
            guard = 0
            last_axis = ""
            run_len = 0
            while (x, y) != (tx, ty) and guard < 300:
                guard += 1
                sx = 1 if tx > x else -1 if tx < x else 0
                sy = 1 if ty > y else -1 if ty < y else 0
                seed = (x * 928371 + y * 364479 + tx * 811 + ty * 1297 + guard * 17) & 7
                axis = "x" if sx and (not sy or seed in {0, 1, 3, 5, 7}) else "y"
                if run_len >= 5:
                    if axis == "x" and sy:
                        axis = "y"
                    elif axis == "y" and sx:
                        axis = "x"
                    elif axis == "x" and 3 < y < rows - 4:
                        axis = "wobble_y"
                    elif axis == "y" and 3 < x < cols - 4:
                        axis = "wobble_x"
                if axis == "x" and sx:
                    x += sx
                elif axis == "y" and sy:
                    y += sy
                elif axis == "wobble_y":
                    y += 1 if ((x + tx + guard) & 1) else -1
                elif axis == "wobble_x":
                    x += 1 if ((y + ty + guard) & 1) else -1
                if axis == last_axis:
                    run_len += 1
                else:
                    last_axis = axis
                    run_len = 1
                mark_road(x, y)

    def mountain_pass_path(points: list[tuple[int, int]], width: int = 1) -> None:
        x, y = points[0]

        def mark_pass(px: int, py: int) -> None:
            for oy in range(-width, width + 1):
                for ox in range(-width, width + 1):
                    if abs(ox) + abs(oy) > width + 1:
                        continue
                    tx, ty = px + ox, py + oy
                    if 0 <= tx < cols and 0 <= ty < rows and grid[ty][tx] == "m":
                        grid[ty][tx] = "q"

        mark_pass(x, y)
        for tx, ty in points[1:]:
            guard = 0
            while (x, y) != (tx, ty) and guard < 300:
                guard += 1
                sx = 1 if tx > x else -1 if tx < x else 0
                sy = 1 if ty > y else -1 if ty < y else 0
                seed = (x * 421531 + y * 104729 + tx * 811 + ty * 1297 + guard * 23) & 7
                if sx and (not sy or seed in {0, 2, 3, 5, 7}):
                    x += sx
                elif sy:
                    y += sy
                mark_pass(x, y)

    def soften_straight_roads() -> None:
        def near_landmark(x: int, y: int) -> bool:
            for dy in range(-2, 3):
                for dx in range(-2, 3):
                    if 0 <= x + dx < cols and 0 <= y + dy < rows and grid[y + dy][x + dx] in {"c", "u", "d"}:
                        return True
            return False

        def restore_ground(x: int, y: int) -> None:
            for nx, ny in ((x, y - 1), (x - 1, y), (x + 1, y), (x, y + 1), (x - 1, y - 1), (x + 1, y + 1)):
                if 0 <= nx < cols and 0 <= ny < rows and grid[ny][nx] in {"g", "f", "s", "n", "v", "b", "m"}:
                    grid[y][x] = grid[ny][nx]
                    return
            grid[y][x] = "g"

        for y in range(rows):
            run_start = -1
            for x in range(cols + 1):
                road = x < cols and grid[y][x] == "r"
                if road and run_start < 0:
                    run_start = x
                elif not road and run_start >= 0:
                    if x - run_start > 8:
                        for bx in range(run_start + 5, x, 7):
                            if not near_landmark(bx, y):
                                restore_ground(bx, y)
                    run_start = -1

        for x in range(cols):
            run_start = -1
            for y in range(rows + 1):
                road = y < rows and grid[y][x] == "r"
                if road and run_start < 0:
                    run_start = y
                elif not road and run_start >= 0:
                    if y - run_start > 8:
                        for by in range(run_start + 5, y, 7):
                            if not near_landmark(x, by):
                                restore_ground(x, by)
                    run_start = -1

    paint_biome(shifted(82, 7, 101), shifted(63, 5, 102), 45, 33, "n", "m", 101)
    paint_biome(shifted(109, 8, 107), shifted(244, 6, 108), 58, 34, "s", "b", 107)
    paint_biome(shifted(235, 7, 113), shifted(169, 6, 114), 53, 42, "v", "f", 113)
    paint_biome(shifted(203, 7, 127), shifted(99, 6, 128), 48, 38, "f", "g", 127)
    paint_biome(shifted(197, 5, 131), shifted(67, 4, 132), 35, 24, "m", "f", 131)
    paint_biome(shifted(74, 6, 137), shifted(217, 5, 138), 37, 30, "m", "s", 137)
    paint_biome(shifted(169, 7, 139), shifted(161, 6, 140), 43, 38, "m", "f", 139)
    paint_biome(shifted(236, 5, 149), shifted(83, 4, 150), 28, 24, "b", "m", 149)
    paint_biome(shifted(186, 6, 151), shifted(242, 5, 152), 34, 24, "b", "s", 151)
    paint_biome(shifted(121, 7, 157), shifted(142, 6, 158), 50, 36, "f", "g", 157)
    seed_biome_granules()
    seed_midland_ponds()

    paint_water_body(shifted(48, 4, 201), shifted(139, 7, 202), 18, 38, 201)
    paint_water_body(shifted(137, 5, 203), shifted(91, 4, 204), 19, 17, 203)
    paint_water_body(shifted(270, 4, 205), shifted(163, 6, 206), 26, 37, 205)
    paint_water_body(shifted(228, 5, 207), shifted(261, 5, 208), 24, 30, 207)
    paint_water_body(shifted(69, 5, 209), shifted(273, 4, 210), 27, 18, 209)
    paint_water_body(shifted(178, 5, 211), shifted(34, 4, 212), 23, 15, 211)
    fill_rect(18, 0, 43, 39, "w")
    fill_rect(262, 0, 292, 47, "w")
    fill_rect(263, 252, 299, 299, "w")

    raise_land_oval(83, 62, 11, 8, "n")
    raise_land_path([(83, 62), (78, 83), (82, 105)], 2, "n")

    mountain_pass_path([(83, 62), (111, 66), (154, 67), (196, 62)], width=1)
    mountain_pass_path([(94, 123), (121, 139), (152, 145)], width=1)
    mountain_pass_path([(150, 230), (171, 232), (194, 235)], width=1)
    mountain_pass_path([(72, 218), (89, 227), (102, 245)], width=1)

    road_path([(82, 105), (94, 123), (112, 158), (132, 153), (152, 145)])
    road_path([(152, 145), (169, 128), (189, 100), (205, 78)])
    road_path([(205, 78), (200, 67), (196, 62)])
    road_path([(152, 145), (176, 153), (202, 171), (228, 185), (255, 177)])
    road_path([(228, 185), (208, 206), (194, 235), (150, 230)])
    road_path([(112, 158), (99, 181), (72, 218), (102, 245)])
    road_path([(82, 105), (78, 83), (83, 62)])
    road_path([(112, 158), (128, 181), (145, 207), (150, 230)])
    road_path([(240, 153), (228, 185)])

    city_footprints = (
        (
            (-3, 0),
            (-2, -1),
            (-2, 0),
            (-2, 1),
            (-1, -2),
            (-1, -1),
            (-1, 0),
            (-1, 1),
            (-1, 2),
            (0, -3),
            (0, -2),
            (0, -1),
            (0, 0),
            (0, 1),
            (0, 2),
            (0, 3),
            (1, -2),
            (1, -1),
            (1, 0),
            (1, 1),
            (1, 2),
            (2, -1),
            (2, 0),
            (2, 1),
            (3, 0),
            (2, 2),
            (-2, 2),
        ),
        (
            (-3, -1),
            (-3, 0),
            (-2, -2),
            (-2, -1),
            (-2, 0),
            (-2, 1),
            (-1, -3),
            (-1, -2),
            (-1, -1),
            (-1, 0),
            (-1, 1),
            (-1, 2),
            (0, -2),
            (0, -1),
            (0, 0),
            (0, 1),
            (0, 2),
            (1, -2),
            (1, -1),
            (1, 0),
            (1, 1),
            (1, 2),
            (2, -1),
            (2, 0),
            (2, 1),
            (3, 0),
            (2, 2),
            (3, 1),
        ),
        (
            (-2, -2),
            (-2, -1),
            (-2, 0),
            (-2, 1),
            (-2, 2),
            (-1, -3),
            (-1, -2),
            (-1, -1),
            (-1, 0),
            (-1, 1),
            (-1, 2),
            (-1, 3),
            (0, -3),
            (0, -2),
            (0, -1),
            (0, 0),
            (0, 1),
            (0, 2),
            (0, 3),
            (1, -2),
            (1, -1),
            (1, 0),
            (1, 1),
            (1, 2),
            (2, -2),
            (2, -1),
            (2, 0),
            (2, 1),
            (2, 2),
            (3, 0),
        ),
    )
    village_footprints = (
        (
            (-2, 0),
            (-1, -1),
            (-1, 0),
            (-1, 1),
            (0, -2),
            (0, -1),
            (0, 0),
            (0, 1),
            (0, 2),
            (1, -1),
            (1, 0),
            (1, 1),
            (2, 0),
            (2, 1),
            (-2, 1),
        ),
        (
            (-2, -1),
            (-2, 0),
            (-1, -2),
            (-1, -1),
            (-1, 0),
            (-1, 1),
            (0, -2),
            (0, -1),
            (0, 0),
            (0, 1),
            (0, 2),
            (1, -1),
            (1, 0),
            (1, 1),
            (2, 0),
            (2, 1),
        ),
    )

    def stamp_settlement(cx: int, cy: int, tile: str, offsets: tuple[tuple[int, int], ...]) -> None:
        for ox, oy in offsets:
            nx, ny = cx + ox, cy + oy
            if 0 <= nx < cols and 0 <= ny < rows and ((nx, ny) in land or tile in {"c", "u"}):
                grid[ny][nx] = tile

    def seed_settlement_roads(cx: int, cy: int, tile: str) -> None:
        for dx, dy, steps in ((-1, 0, 4), (1, 0, 4), (0, -1, 4), (0, 1, 5)):
            for step in range(1, steps + 1):
                nx = cx + dx * step
                ny = cy + dy * step
                if 0 <= nx < cols and 0 <= ny < rows and grid[ny][nx] != tile:
                    grid[ny][nx] = "r"

    locations: list[LocationPatch] = []

    def near_any_tile(x: int, y: int, tiles: set[str], radius: int) -> bool:
        for oy in range(-radius, radius + 1):
            for ox in range(-radius, radius + 1):
                if abs(ox) + abs(oy) > radius + 1:
                    continue
                nx, ny = x + ox, y + oy
                if 0 <= nx < cols and 0 <= ny < rows and grid[ny][nx] in tiles:
                    return True
        return False

    def far_from_locations(x: int, y: int, min_distance: int) -> bool:
        return all((x - patch.cx) ** 2 + (y - patch.cy) ** 2 >= min_distance * min_distance for patch in locations)

    def choose_location_center(
        *,
        salt: int,
        base_x: int,
        base_y: int,
        spread_x: int,
        spread_y: int,
        allowed: set[str],
        near_tiles: set[str] | None = None,
        near_radius: int = 4,
        min_distance: int = 12,
    ) -> tuple[int, int] | None:
        fallback: tuple[int, int] | None = None
        for attempt in range(360):
            hx = terrain_hash(attempt, salt, 19)
            hy = terrain_hash(attempt, salt, 23)
            x = base_x + int(hx % (spread_x * 2 + 1)) - spread_x
            y = base_y + int(hy % (spread_y * 2 + 1)) - spread_y
            if not (4 <= x < cols - 4 and 4 <= y < rows - 4):
                continue
            if grid[y][x] not in allowed:
                continue
            if near_any_tile(x, y, {"w", "c", "u", "d"}, 3):
                continue
            if not far_from_locations(x, y, min_distance):
                continue
            fallback = fallback or (x, y)
            if near_tiles is None or near_any_tile(x, y, near_tiles, near_radius):
                return x, y
        return fallback

    def add_location(kind: str, cx: int, cy: int, rx: int, ry: int, salt: int) -> None:
        locations.append(LocationPatch(kind, cx, cy, rx, ry, salt))

    for i, (cx, cy) in enumerate(CITY_GATE_CENTERS.values()):
        stamp_settlement(cx, cy, "c", city_footprints[i % len(city_footprints)])
        seed_settlement_roads(cx, cy, "c")

    for i, (vx, vy) in enumerate(VILLAGE_CENTERS.values()):
        stamp_settlement(vx, vy, "u", village_footprints[i % len(village_footprints)])
        seed_settlement_roads(vx, vy, "u")

    for dx, dy in DUNGEON_ENTRANCES.values():
        for ny in range(dy - 2, dy + 3):
            for nx in range(dx - 2, dx + 3):
                if 0 <= nx < cols and 0 <= ny < rows and abs(nx - dx) + abs(ny - dy) <= 3:
                    grid[ny][nx] = "r"
        for nx, ny in ((dx - 3, dy), (dx + 3, dy), (dx, dy - 3), (dx, dy + 3)):
            if 0 <= nx < cols and 0 <= ny < rows:
                grid[ny][nx] = "r"
        grid[dy][dx] = "d"

    farm_anchors = (
        VILLAGE_CENTERS["village_oakhaven"],
        CITY_GATE_CENTERS["city_riverside"],
        CITY_GATE_CENTERS["city_archive"],
        VILLAGE_CENTERS["village_dunewick"],
    )
    for i, (base_x, base_y) in enumerate(farm_anchors):
        center = choose_location_center(
            salt=700 + i * 37,
            base_x=base_x,
            base_y=base_y,
            spread_x=18,
            spread_y=16,
            allowed={"g", "s"},
            near_tiles={"r", "u", "c"},
            near_radius=6,
            min_distance=10,
        )
        if center:
            add_location("farmland", center[0], center[1], 5 + (terrain_hash(i, 713, 3) % 3), 4 + (terrain_hash(i, 719, 5) % 2), 710 + i)

    camp_anchors = (
        (96, 110),
        (186, 115),
        (221, 201),
        (132, 222),
        (210, 244),
    )
    for i, (base_x, base_y) in enumerate(camp_anchors):
        center = choose_location_center(
            salt=820 + i * 43,
            base_x=base_x,
            base_y=base_y,
            spread_x=28,
            spread_y=24,
            allowed={"g", "f", "s", "b", "q"},
            near_tiles={"r", "q"},
            near_radius=8,
            min_distance=14,
        )
        if center:
            add_location("goblin_camp", center[0], center[1], 4 + (terrain_hash(i, 823, 7) % 3), 4 + (terrain_hash(i, 829, 11) % 3), 830 + i)

    for i, (dx, dy) in enumerate(DUNGEON_ENTRANCES.values()):
        add_location("graveyard", dx, dy, 6 + (terrain_hash(dx, dy, 907) % 2), 5 + (terrain_hash(dx, dy, 911) % 2), 910 + i)

    global OVERWORLD_LOCATIONS
    OVERWORLD_LOCATIONS = tuple(locations)

    return ["".join(row) for row in grid]


def current_world_seed() -> int:
    return _WORLD_SEED


def set_world_seed(seed: int) -> None:
    global _WORLD_SEED
    _WORLD_SEED = int(seed) & 0xFFFFFFFF
    if "MAPS" in globals():
        MAPS[OVERWORLD_ID] = _build_overworld(_WORLD_SEED)


def _blank(cols: int, rows: int, floor: str = "r", wall: str = "m") -> list[list[str]]:
    grid = [[floor for _ in range(cols)] for _ in range(rows)]
    for x in range(cols):
        grid[0][x] = wall
        grid[rows - 1][x] = wall
    for y in range(rows):
        grid[y][0] = wall
        grid[y][cols - 1] = wall
    return grid


def _rect(grid: list[list[str]], x1: int, y1: int, x2: int, y2: int, tile: str) -> None:
    rows = len(grid)
    cols = len(grid[0])
    for y in range(max(0, y1), min(rows, y2 + 1)):
        for x in range(max(0, x1), min(cols, x2 + 1)):
            grid[y][x] = tile


def _rows(grid: list[list[str]]) -> list[str]:
    return ["".join(row) for row in grid]


def _paint_if(
    grid: list[list[str]],
    x1: int,
    y1: int,
    x2: int,
    y2: int,
    tile: str,
    allowed: set[str] | None = None,
) -> None:
    rows = len(grid)
    cols = len(grid[0])
    for y in range(max(0, y1), min(rows, y2 + 1)):
        for x in range(max(0, x1), min(cols, x2 + 1)):
            if allowed is None or grid[y][x] in allowed:
                grid[y][x] = tile


def _organic_fill(grid: list[list[str]], cx: int, cy: int, rx: int, ry: int, tile: str, salt: int) -> None:
    rows = len(grid)
    cols = len(grid[0])
    for y in range(max(1, cy - ry - 2), min(rows - 1, cy + ry + 3)):
        for x in range(max(1, cx - rx - 2), min(cols - 1, cx + rx + 3)):
            nx = (x - cx) / max(1.0, rx)
            ny = (y - cy) / max(1.0, ry)
            angle = math.atan2(ny, nx)
            radial = math.hypot(nx, ny)
            ripple = math.sin(angle * 3.0 + salt * 0.41) * 0.11 + math.cos(angle * 5.0 - salt * 0.19) * 0.08
            grain = math.sin((x * 0.27 + y * 0.19 + salt) * 0.73) * 0.06 + math.cos((x * 0.14 - y * 0.23 + salt) * 0.59) * 0.04
            if radial <= 1.0 + ripple + grain:
                grid[y][x] = tile


def _edge_wall_from_floor(grid: list[list[str]], floor_tiles: set[str]) -> None:
    rows = len(grid)
    cols = len(grid[0])
    updates: list[tuple[int, int]] = []
    for y in range(1, rows - 1):
        for x in range(1, cols - 1):
            if grid[y][x] not in floor_tiles:
                continue
            neighbors = (grid[y - 1][x], grid[y + 1][x], grid[y][x - 1], grid[y][x + 1])
            if any(tile == "g" or tile == "m" for tile in neighbors):
                updates.append((x, y))
    for x, y in updates:
        grid[y][x] = "x"


def _city_gate_tiles(cols: int, rows: int) -> dict[str, tuple[tuple[int, int], tuple[int, int]]]:
    cx = cols // 2
    cy = rows // 2
    return {
        "north": ((cx - 1, 0), (cx, 0)),
        "south": ((cx - 1, rows - 1), (cx, rows - 1)),
        "west": ((0, cy - 1), (0, cy)),
        "east": ((cols - 1, cy - 1), (cols - 1, cy)),
    }


def _city_spawn_point(cols: int, rows: int) -> tuple[int, int]:
    cx = cols // 2
    return cx, rows - 3


def _village_spawn_point(cols: int, rows: int) -> tuple[int, int]:
    cx = cols // 2
    return cx, rows - 3


CITY_VARIANT_BY_MAP = {
    "city_riverside": "riverside",
    "city_archive": "archive",
    "city_highwall": "highwall",
    "city_belltower": "belltower",
    "city_sanctum": "sanctum",
}

CITY_BUILDING_TEMPLATES: dict[str, tuple[CityBuilding, ...]] = {
    "riverside": (
        CityBuilding("river_warehouse", 3, 4, 7, 5, "warehouse", 0),
        CityBuilding("apothecary_row", 9, 4, 13, 5, "shop", 1),
        CityBuilding("north_gate_homes", 18, 4, 20, 5, "house", 2),
        CityBuilding("bridge_inn", 21, 4, 26, 5, "inn", 2),
        CityBuilding("east_storehouse", 28, 4, 31, 5, "warehouse", 0),
        CityBuilding("inner_west_row", 7, 8, 10, 9, "row", 1),
        CityBuilding("west_market_house", 3, 8, 6, 10, "shop", 2),
        CityBuilding("inner_east_row", 23, 8, 26, 9, "row", 0),
        CityBuilding("east_market_house", 27, 8, 30, 10, "shop", 1),
        CityBuilding("west_homes", 4, 14, 9, 17, "row", 0),
        CityBuilding("river_hall", 11, 15, 15, 17, "hall", 1),
        CityBuilding("east_homes", 22, 14, 29, 17, "row", 2),
        CityBuilding("southwest_corner_row", 2, 19, 5, 20, "row", 2),
        CityBuilding("south_tavern", 8, 19, 13, 20, "inn", 1),
        CityBuilding("south_mid_shop", 18, 19, 20, 20, "shop", 1),
        CityBuilding("south_stables", 21, 19, 26, 20, "shop", 0),
    ),
    "archive": (
        CityBuilding("west_stacks", 3, 3, 8, 5, "guild", 1),
        CityBuilding("north_study", 10, 3, 14, 5, "house", 0),
        CityBuilding("north_gate_homes", 18, 4, 20, 5, "house", 2),
        CityBuilding("east_stacks", 21, 3, 27, 5, "guild", 2),
        CityBuilding("illuminator_house", 28, 4, 31, 5, "shop", 1),
        CityBuilding("inner_west_row", 7, 8, 10, 9, "row", 1),
        CityBuilding("west_scriptorium", 3, 8, 6, 10, "guild", 0),
        CityBuilding("inner_east_row", 23, 8, 26, 9, "row", 0),
        CityBuilding("east_scriptorium", 27, 8, 30, 10, "guild", 2),
        CityBuilding("west_archive_homes", 4, 14, 9, 17, "row", 1),
        CityBuilding("records_hall", 11, 15, 15, 17, "hall", 2),
        CityBuilding("east_archive_homes", 22, 14, 29, 17, "row", 0),
        CityBuilding("southwest_corner_row", 2, 19, 5, 20, "row", 2),
        CityBuilding("south_bindery", 8, 19, 13, 20, "shop", 1),
        CityBuilding("south_mid_shop", 18, 19, 20, 20, "shop", 1),
        CityBuilding("south_reading_room", 21, 19, 26, 20, "guild", 0),
        CityBuilding("southeast_scribes", 28, 19, 30, 20, "house", 2),
    ),
    "highwall": (
        CityBuilding("west_barracks", 3, 4, 8, 5, "barracks", 0),
        CityBuilding("north_armory", 10, 4, 14, 5, "guild", 1),
        CityBuilding("north_gate_homes", 18, 4, 20, 5, "house", 2),
        CityBuilding("east_barracks", 21, 4, 27, 5, "barracks", 2),
        CityBuilding("east_guardhouse", 28, 8, 31, 10, "barracks", 0),
        CityBuilding("inner_west_row", 11, 8, 13, 9, "row", 1),
        CityBuilding("west_guardhouse", 3, 8, 6, 10, "barracks", 1),
        CityBuilding("inner_east_row", 23, 8, 26, 9, "row", 0),
        CityBuilding("west_lower_row", 5, 15, 11, 17, "row", 0),
        CityBuilding("captains_hall", 12, 15, 15, 17, "hall", 2),
        CityBuilding("east_lower_row", 22, 15, 30, 17, "row", 1),
        CityBuilding("southwest_corner_row", 6, 19, 7, 20, "row", 2),
        CityBuilding("south_armory", 8, 19, 13, 20, "shop", 2),
        CityBuilding("south_mid_shop", 18, 19, 20, 20, "shop", 1),
        CityBuilding("south_quarters", 21, 19, 26, 20, "house", 0),
    ),
    "belltower": (
        CityBuilding("west_chime_row", 3, 3, 8, 5, "row", 0),
        CityBuilding("bellwright_shop", 10, 3, 14, 5, "shop", 2),
        CityBuilding("north_gate_homes", 18, 4, 20, 5, "house", 2),
        CityBuilding("north_chapel_row", 21, 3, 27, 5, "hall", 1),
        CityBuilding("east_chime_house", 28, 4, 31, 5, "house", 0),
        CityBuilding("inner_west_row", 7, 8, 10, 9, "row", 1),
        CityBuilding("west_market_house", 3, 8, 6, 10, "shop", 1),
        CityBuilding("inner_east_row", 23, 8, 26, 9, "row", 0),
        CityBuilding("east_market_house", 27, 8, 30, 10, "shop", 2),
        CityBuilding("west_lower_chimes", 4, 14, 9, 17, "row", 1),
        CityBuilding("bellkeepers_lodge", 11, 15, 15, 17, "inn", 0),
        CityBuilding("east_lower_chimes", 22, 14, 29, 17, "row", 2),
        CityBuilding("southwest_corner_row", 2, 19, 5, 20, "row", 2),
        CityBuilding("south_bellfoundry", 8, 19, 13, 20, "guild", 1),
        CityBuilding("south_mid_shop", 18, 19, 20, 20, "shop", 1),
        CityBuilding("south_homes", 21, 19, 26, 20, "house", 0),
        CityBuilding("southeast_chime_house", 28, 19, 30, 20, "house", 2),
    ),
    "sanctum": (
        CityBuilding("west_cloister", 3, 3, 8, 5, "hall", 2),
        CityBuilding("north_cells", 10, 3, 14, 5, "house", 1),
        CityBuilding("north_gate_homes", 18, 4, 20, 5, "house", 2),
        CityBuilding("east_cloister", 21, 3, 27, 5, "hall", 0),
        CityBuilding("east_reliquary", 28, 4, 31, 5, "guild", 2),
        CityBuilding("inner_west_row", 7, 8, 10, 9, "row", 1),
        CityBuilding("west_reliquary", 3, 8, 6, 10, "guild", 1),
        CityBuilding("inner_east_row", 23, 8, 26, 9, "row", 0),
        CityBuilding("east_scribe_house", 27, 8, 30, 10, "shop", 0),
        CityBuilding("west_lower_cells", 4, 14, 9, 17, "row", 0),
        CityBuilding("warden_hall", 11, 15, 15, 17, "hall", 1),
        CityBuilding("east_lower_cells", 22, 14, 29, 17, "row", 2),
        CityBuilding("southwest_corner_row", 2, 19, 5, 20, "row", 2),
        CityBuilding("south_apothecary", 8, 19, 13, 20, "shop", 1),
        CityBuilding("south_mid_shop", 18, 19, 20, 20, "shop", 1),
        CityBuilding("south_sanctum_homes", 21, 19, 26, 20, "house", 0),
        CityBuilding("southeast_cells", 28, 19, 30, 20, "house", 2),
    ),
}


def _paint_city_buildings(grid: list[list[str]], variant: str) -> None:
    for building in CITY_BUILDING_TEMPLATES[variant]:
        _paint_if(grid, building.x1, building.y1, building.x2, building.y2, "h", allowed={"p", "j", "l", "a", "y", "g"})


def _paint_city_surface_zones(grid: list[list[str]]) -> None:
    cols, rows = len(grid[0]), len(grid)
    _paint_if(grid, 2, 7, cols - 3, 7, "l", allowed={"p"})
    _paint_if(grid, 2, 13, cols - 3, 13, "l", allowed={"p"})
    _paint_if(grid, 2, 20, cols - 3, 20, "l", allowed={"p"})
    for x in (7, 21):
        _paint_if(grid, x, 7, x + 4, 10, "j", allowed={"p", "l"})
    _paint_if(grid, 2, 14, 13, 17, "j", allowed={"p", "l"})
    _paint_if(grid, 18, 14, cols - 3, 17, "j", allowed={"p", "l"})
    _paint_if(grid, 2, 19, 13, 20, "j", allowed={"p", "l"})
    _paint_if(grid, 18, 19, cols - 3, 20, "j", allowed={"p", "l"})


def _city_layout(variant: str) -> list[str]:
    # City tile legend:
    # p = plaza stone, h = house/building, x = city wall, r = road, w = water
    # a = market awnings, y = garden/courtyard, t = tower/monument
    # l = side street, j = residential forecourt
    cols, rows = 34, 24
    cx, cy = cols // 2, rows // 2
    grid = _blank(cols, rows, floor="m", wall="m")

    _rect(grid, 1, 1, cols - 2, rows - 2, "p")
    _rect(grid, 1, 1, cols - 2, 1, "x")
    _rect(grid, 1, rows - 2, cols - 2, rows - 2, "x")
    _rect(grid, 1, 1, 1, rows - 2, "x")
    _rect(grid, cols - 2, 1, cols - 2, rows - 2, "x")

    _paint_if(grid, cx - 1, 0, cx, rows - 1, "r", allowed={"p", "m", "x"})
    _paint_if(grid, 0, cy - 1, cols - 1, cy, "r", allowed={"p", "m", "x"})
    _paint_if(grid, 2, 6, cols - 3, 6, "r", allowed={"p", "x"})
    _paint_if(grid, 2, 18, cols - 3, 18, "r", allowed={"p", "x"})
    _paint_if(grid, 6, 21, cols - 7, 21, "r", allowed={"p", "x"})

    _paint_if(grid, cx - 4, cy - 3, cx + 4, cy + 3, "p")
    _paint_if(grid, cx - 2, cy - 2, cx + 2, cy + 2, "y")
    _paint_if(grid, cx - 1, cy - 1, cx + 1, cy + 1, "p")

    gates = _city_gate_tiles(cols, rows)
    for gate_tiles in gates.values():
        for gx, gy in gate_tiles:
            grid[gy][gx] = "r"
            if gx + 1 < cols and grid[gy][gx + 1] == "x":
                grid[gy][gx + 1] = "r"
            if gx - 1 >= 0 and grid[gy][gx - 1] == "x":
                grid[gy][gx - 1] = "r"
            if gy + 1 < rows and grid[gy + 1][gx] == "x":
                grid[gy + 1][gx] = "r"
            if gy - 1 >= 0 and grid[gy - 1][gx] == "x":
                grid[gy - 1][gx] = "r"

    if variant == "riverside":
        _paint_if(grid, 2, 3, cols - 3, 3, "w", allowed={"p"})
        _paint_if(grid, cx - 1, 3, cx, 3, "r", allowed={"w"})
        _paint_if(grid, 4, 7, 6, 7, "a", allowed={"p"})
        _paint_if(grid, cols - 8, 19, cols - 6, 20, "a", allowed={"p"})
    elif variant == "archive":
        _paint_if(grid, 4, 7, 6, 8, "y", allowed={"p"})
        _paint_if(grid, cols - 7, 7, cols - 5, 8, "y", allowed={"p"})
        _paint_if(grid, cx - 5, cy - 4, cx + 5, cy - 4, "a", allowed={"p"})
        _paint_if(grid, cx - 1, cy - 5, cx, cy - 4, "t", allowed={"p"})
    elif variant == "highwall":
        _paint_if(grid, 3, 2, cols - 4, 2, "x", allowed={"p", "x"})
        _paint_if(grid, 3, rows - 3, cols - 4, rows - 3, "x", allowed={"p", "x"})
        _paint_if(grid, cx - 8, cy - 5, cx - 7, cy - 4, "t", allowed={"p"})
        _paint_if(grid, cx + 2, cy + 3, cx + 3, cy + 4, "t", allowed={"p"})
    elif variant == "belltower":
        _paint_if(grid, cx - 1, 4, cx, cy + 4, "d", allowed={"p", "r"})
        _paint_if(grid, cx - 2, cy - 5, cx - 1, cy - 3, "t", allowed={"p"})
        _paint_if(grid, cx - 10, cy + 3, cx - 6, cy + 4, "a", allowed={"p"})
    elif variant == "sanctum":
        _paint_if(grid, cx - 6, cy - 5, cx + 6, cy - 5, "x", allowed={"p"})
        _paint_if(grid, cx - 6, cy + 1, cx + 6, cy + 1, "x", allowed={"p"})
        _paint_if(grid, cx - 10, cy - 3, cx - 7, cy + 1, "y", allowed={"p"})
        _paint_if(grid, cx + 7, cy - 1, cx + 10, cy + 3, "y", allowed={"p"})
        _paint_if(grid, cx - 1, cy - 4, cx, cy - 3, "t", allowed={"p"})

    _paint_city_surface_zones(grid)
    _paint_city_buildings(grid, variant)

    return _rows(grid)


def _village_layout(variant: str) -> list[str]:
    cols, rows = 28, 20
    cx, cy = cols // 2, rows // 2
    outskirts = "f"
    if variant == "snow":
        outskirts = "n"
    elif variant == "desert":
        outskirts = "s"
    elif variant == "marsh":
        outskirts = "v"
    grid = _blank(cols, rows, floor=outskirts, wall="m")

    _organic_fill(grid, cx, cy, 11, 7, "g", 211 + len(variant))
    _organic_fill(grid, cx - 3, cy + 1, 8, 6, "g", 229 + len(variant))
    _paint_if(grid, cx - 1, 0, cx, rows - 1, "r", allowed={"g", outskirts, "m"})
    _paint_if(grid, 0, cy - 1, cols - 1, cy, "r", allowed={"g", outskirts, "m"})
    _paint_if(grid, 2, rows - 4, cols - 3, rows - 4, "r", allowed={"g", outskirts, "m"})

    cottage_blocks = (
        (4, 4, 7, 6),
        (9, 3, 12, 5),
        (16, 4, 20, 6),
        (5, 12, 9, 14),
        (12, 13, 15, 15),
        (18, 11, 22, 14),
    )
    for x1, y1, x2, y2 in cottage_blocks:
        _paint_if(grid, x1, y1, x2, y2, "h", allowed={"g"})
    _paint_if(grid, cx - 1, cy - 1, cx + 1, cy + 1, "p", allowed={"g"})

    if variant == "marsh":
        _paint_if(grid, 2, cy - 4, 5, cy + 2, "v", allowed={"g", "r"})
        _paint_if(grid, cols - 6, cy - 2, cols - 3, cy + 4, "v", allowed={"g", "r"})
        _paint_if(grid, 3, cy - 1, 4, cy, "w", allowed={"v"})
        _paint_if(grid, cols - 5, cy + 1, cols - 4, cy + 2, "w", allowed={"v"})
    elif variant == "snow":
        _paint_if(grid, 3, 2, cols - 4, 3, "n")
        _paint_if(grid, 3, rows - 4, cols - 4, rows - 4, "n")
    elif variant == "desert":
        _paint_if(grid, 3, 2, cols - 4, 3, "s")
        _paint_if(grid, 3, rows - 4, cols - 4, rows - 4, "s")
    else:
        _paint_if(grid, 3, 2, cols - 4, 3, "f")
        _paint_if(grid, 3, rows - 4, cols - 4, rows - 4, "f")

    gates = _city_gate_tiles(cols, rows)
    for gate_tiles in gates.values():
        for gx, gy in gate_tiles:
            grid[gy][gx] = "r"

    return _rows(grid)


def _dungeon_layout(depth: int) -> list[str]:
    cols, rows = 30, 22
    grid = _blank(cols, rows, floor="x", wall="x")
    _rect(grid, 2, 2, cols - 3, rows - 3, "r")
    _rect(grid, 5, 5, 11, 9, "d")
    _rect(grid, 17, 4, 24, 8, "d")
    _rect(grid, 6, 13, 12, 18, "d")
    _rect(grid, 18, 13, 26, 18, "d")
    _rect(grid, 13, 9, 16, 12, "r")
    _rect(grid, 1, 10, cols - 2, 11, "r")
    _rect(grid, 14, 1, 15, rows - 2, "r")
    if depth > 1:
        _rect(grid, 13, 9, 16, 12, "w")
        grid[10][14] = "r"
        grid[10][15] = "r"
    return _rows(grid)


def _house_interior_layout(seed: int) -> list[str]:
    cols, rows = 24, 17
    grid = _blank(cols, rows, floor="i", wall="o")
    _rect(grid, 11, 15, 12, 15, "e")
    if seed % 4 == 0:
        _rect(grid, 3, 3, 5, 4, "k")
        _rect(grid, 17, 3, 20, 4, "k")
        _rect(grid, 8, 7, 15, 9, "z")
        _rect(grid, 18, 11, 20, 13, "k")
        _rect(grid, 4, 11, 6, 13, "k")
    elif seed % 4 == 1:
        _rect(grid, 3, 2, 8, 2, "k")
        _rect(grid, 17, 2, 20, 5, "k")
        _rect(grid, 7, 7, 12, 9, "z")
        _rect(grid, 4, 12, 7, 13, "k")
        _rect(grid, 15, 10, 17, 11, "k")
    elif seed % 4 == 2:
        _rect(grid, 3, 3, 4, 7, "k")
        _rect(grid, 15, 3, 20, 5, "k")
        _rect(grid, 8, 10, 15, 11, "z")
        _rect(grid, 17, 8, 19, 9, "k")
        _rect(grid, 5, 12, 7, 13, "k")
    else:
        _rect(grid, 3, 2, 6, 4, "k")
        _rect(grid, 15, 2, 20, 2, "k")
        _rect(grid, 5, 8, 11, 10, "z")
        _rect(grid, 17, 9, 20, 13, "k")
        _rect(grid, 4, 13, 6, 13, "k")
    return _rows(grid)


def house_interior_id(source_map_id: str, wx: int, wy: int) -> str:
    return f"house_{source_map_id}_{wx}_{wy}"


MAPS: dict[str, list[str]] = {
    OVERWORLD_ID: _build_overworld(_WORLD_SEED),
    "city_riverside": _city_layout("riverside"),
    "city_archive": _city_layout("archive"),
    "city_highwall": _city_layout("highwall"),
    "city_belltower": _city_layout("belltower"),
    "city_sanctum": _city_layout("sanctum"),
    "village_oakhaven": _village_layout("green"),
    "village_snowrest": _village_layout("snow"),
    "village_dunewick": _village_layout("desert"),
    "village_mireford": _village_layout("marsh"),
    "dungeon_stonegate_1": _dungeon_layout(1),
    "dungeon_stonegate_2": _dungeon_layout(2),
    "dungeon_miredepth_1": _dungeon_layout(1),
    "dungeon_frosthollow_1": _dungeon_layout(1),
    "dungeon_blackvault_1": _dungeon_layout(1),
}

CITY_BUILDINGS: dict[str, tuple[CityBuilding, ...]] = {
    map_id: CITY_BUILDING_TEMPLATES[variant]
    for map_id, variant in CITY_VARIANT_BY_MAP.items()
}
_CITY_BUILDING_TILE_LOOKUP: dict[str, dict[tuple[int, int], CityBuilding]] = {
    map_id: {
        (x, y): building
        for building in buildings
        for y in range(building.y1, building.y2 + 1)
        for x in range(building.x1, building.x2 + 1)
    }
    for map_id, buildings in CITY_BUILDINGS.items()
}


def city_buildings_for_map(map_id: str) -> tuple[CityBuilding, ...]:
    return CITY_BUILDINGS.get(map_id, ())


def city_building_at(map_id: str, x: int, y: int) -> CityBuilding | None:
    return _CITY_BUILDING_TILE_LOOKUP.get(map_id, {}).get((x, y))


def city_building_door_tiles(building: CityBuilding) -> tuple[tuple[int, int], ...]:
    if building.width <= 3 or building.style in {"hall", "guild", "barracks", "warehouse"}:
        centers = (building.x1 + building.width // 2,)
    else:
        count = 2 if building.width <= 6 else 3
        if building.style == "row" and building.width >= 8:
            count = 4
        span = max(1, building.width)
        centers = tuple(
            max(building.x1, min(building.x2, int(round(building.x1 + (index + 0.5) * span / count - 0.5))))
            for index in range(count)
        )
    return tuple((x, building.y2) for x in dict.fromkeys(centers))


def city_building_entry_at(map_id: str, x: int, y: int, from_x: int, from_y: int) -> CityBuilding | None:
    building = city_building_at(map_id, x, y)
    if building is None:
        return None
    if y != building.y2:
        return None
    if from_y != y + 1 or from_x != x:
        return None
    return building

MAP_LABELS = {
    OVERWORLD_ID: "Alderfall Overworld",
    "city_riverside": "Riverside City",
    "city_archive": "Archive City",
    "city_highwall": "Highwall City",
    "city_belltower": "Belltower City",
    "city_sanctum": "Sanctum City",
    "village_oakhaven": "Oakhaven Village",
    "village_snowrest": "Snowrest Village",
    "village_dunewick": "Dunewick Village",
    "village_mireford": "Mireford Village",
    "dungeon_stonegate_1": "Stonegate Dungeon",
    "dungeon_stonegate_2": "Stonegate Deep Vault",
    "dungeon_miredepth_1": "Miredepth Dungeon",
    "dungeon_frosthollow_1": "Frosthollow Dungeon",
    "dungeon_blackvault_1": "Blackvault Dungeon",
}

MAP_KIND = {
    OVERWORLD_ID: "overworld",
    "city_riverside": "city",
    "city_archive": "city",
    "city_highwall": "city",
    "city_belltower": "city",
    "city_sanctum": "city",
    "village_oakhaven": "village",
    "village_snowrest": "village",
    "village_dunewick": "village",
    "village_mireford": "village",
    "dungeon_stonegate_1": "dungeon",
    "dungeon_stonegate_2": "dungeon",
    "dungeon_miredepth_1": "dungeon",
    "dungeon_frosthollow_1": "dungeon",
    "dungeon_blackvault_1": "dungeon",
}

TERRAIN_NAMES = {
    "g": "Meadow",
    "f": "Oldwood",
    "s": "Sunsteppe",
    "n": "Frostfield",
    "v": "Marsh",
    "b": "Badlands",
    "m": "Mountain",
    "q": "Mountain Pass",
    "w": "Water",
    "r": "Road",
    "c": "City Gate",
    "u": "Village",
    "p": "Stone Plaza",
    "j": "Residential Court",
    "l": "Side Street",
    "h": "Building",
    "x": "Wall",
    "a": "Market Stalls",
    "y": "Garden Court",
    "t": "City Tower",
    "d": "Dungeon Floor",
    "i": "Interior Floor",
    "e": "Doorway",
    "z": "Rug",
    "k": "Furniture",
    "o": "Interior Wall",
}

LOCATION_NAMES = {
    "farmland": "Farmland",
    "goblin_camp": "Goblin Camp",
    "graveyard": "Graveyard",
}

CITY_TERRAIN_NAMES = {
    "r": "City Road",
    "p": "Stone Plaza",
    "j": "Residential Court",
    "l": "Side Street",
    "a": "Market Row",
    "y": "Garden Court",
    "d": "Bell Walk",
    "w": "Canal",
    "h": "Building",
    "x": "City Wall",
    "t": "City Tower",
}

PASSABLE = {"g", "f", "s", "n", "v", "b", "q", "r", "c", "u", "d", "p", "j", "l", "a", "y", "i", "e", "z"}
ENCOUNTER_TABLE = {
    "g": [(1, ["slime", "wolf"]), (3, ["slime", "wolf", "dire_wolf", "thornling"]), (5, ["wolf", "dire_wolf", "acid_slime", "thornling"]), (7, ["dire_wolf", "frost_wolf", "night_stalker", "bog_beast"])],
    "f": [(1, ["wolf", "goblin", "spider"]), (3, ["goblin", "spider", "dire_wolf", "thornling"]), (5, ["spider", "venom_spider", "orc", "thornling"]), (7, ["venom_spider", "orc_champion", "night_stalker", "bog_beast"])],
    "s": [(1, ["slime", "goblin"]), (3, ["wolf", "goblin", "orc"]), (5, ["dire_wolf", "orc", "orc_champion", "sand_stalker", "ember_imp"]), (7, ["frost_wolf", "orc_champion", "night_stalker", "sand_stalker"])],
    "n": [(1, ["wolf", "bat"]), (3, ["dire_wolf", "shadow_bat"]), (5, ["frost_wolf", "bone_knight", "wraith", "ice_golem"]), (7, ["frost_wolf", "elder_wraith", "crypt_revenant", "ice_golem"])],
    "v": [(1, ["slime", "spider"]), (3, ["acid_slime", "spider", "venom_spider", "bog_beast"]), (5, ["venom_spider", "wraith", "acid_broodmother", "bog_beast"]), (7, ["venom_spider", "elder_wraith", "acid_broodmother", "bog_beast"])],
    "b": [(1, ["goblin", "skeleton"]), (3, ["orc", "skeleton", "wraith", "ember_imp"]), (5, ["bone_knight", "orc_champion", "elder_wraith", "sand_stalker", "ember_imp"]), (7, ["crypt_revenant", "orc_champion", "elder_wraith", "sand_stalker"])],
    "q": [(1, ["wolf", "bat"]), (3, ["dire_wolf", "shadow_bat"]), (5, ["frost_wolf", "orc_champion", "wraith", "ice_golem"]), (7, ["frost_wolf", "elder_wraith", "night_stalker", "ice_golem"])],
    "d": [(1, ["bat", "skeleton", "wraith", "orc"]), (4, ["shadow_bat", "bone_knight", "wraith", "orc", "ember_imp"]), (6, ["bone_knight", "venom_spider", "elder_wraith", "orc_champion", "ember_imp"]), (8, ["crypt_revenant", "acid_broodmother", "elder_wraith", "orc_champion", "ice_golem"])],
}

LOCATION_ENCOUNTER_TABLE = {
    "farmland": [(1, ["slime", "wolf"]), (4, ["slime", "dire_wolf", "thornling"]), (7, ["acid_slime", "dire_wolf", "thornling"])],
    "goblin_camp": [
        (1, ["goblin_scout", "goblin", "goblin_archer"]),
        (3, ["goblin", "goblin_archer", "goblin_trapper", "goblin_skirmisher"]),
        (5, ["goblin_archer", "goblin_trapper", "goblin_skirmisher", "goblin_shaman", "hobgoblin_guard"]),
        (7, ["goblin_skirmisher", "goblin_shaman", "hobgoblin_guard", "goblin_warlord"]),
    ],
    "graveyard": [(1, ["skeleton", "wraith"]), (4, ["skeleton", "wraith", "bone_knight"]), (7, ["wraith", "bone_knight", "crypt_revenant"])],
}

LOCATION_BOSS_GROUPS = {
    "goblin_camp": (
        (6, 0.08, ("goblin_warlord", "goblin_archer", "goblin_trapper")),
        (8, 0.04, ("goblin_king", "hobgoblin_guard", "goblin_shaman")),
    ),
    "graveyard": (
        (7, 0.06, ("crypt_revenant", "bone_knight")),
    ),
}

LOCATION_EVENT_TEXT = {
    "farmland": "Something rustles between the rows.",
    "goblin_camp": "Raiders rush from the camp.",
    "graveyard": "The grave soil shifts underfoot.",
}


@dataclass
class LocationEvent:
    kind: str
    message: str
    monster_key: str | None = None
    monster_keys: list[str] | None = None
    npc: NPC | None = None


@dataclass(frozen=True)
class Transition:
    target_map_id: str
    target_x: int
    target_y: int
    message: str


def _surface_exit_point(
    rows: list[str],
    cx: int,
    cy: int,
    dx: int,
    dy: int,
    blocked_tile: str,
) -> tuple[int, int]:
    cols = len(rows[0])
    height = len(rows)
    fallback: tuple[int, int] | None = None

    def in_bounds(x: int, y: int) -> bool:
        return 0 <= x < cols and 0 <= y < height

    for step in range(1, 11):
        x = cx + dx * step
        y = cy + dy * step
        if not in_bounds(x, y):
            break
        tile = rows[y][x]
        if tile == blocked_tile:
            continue
        if tile == "r":
            return x, y
        if tile in PASSABLE and fallback is None:
            fallback = (x, y)

    # Curved/irregular footprints can push the nearest clear tile one square off
    # the gate axis, so widen the search before falling back to the center.
    for step in range(1, 12):
        for side in (-2, -1, 1, 2):
            x = cx + dx * step + (-dy * side)
            y = cy + dy * step + (dx * side)
            if not in_bounds(x, y):
                continue
            tile = rows[y][x]
            if tile == blocked_tile:
                continue
            if tile == "r":
                return x, y
            if tile in PASSABLE and fallback is None:
                fallback = (x, y)

    return fallback or (cx, cy)


NPCS_BY_MAP: dict[str, list[NPC]] = {
    OVERWORLD_ID: [],
    "city_riverside": [
        NPC(
            "Marla",
            "npc_marla",
            17,
            11,
            [
                "Marla: I was a field medic before Riverside had walls.",
                "Marla: The marsh fever is back, and the slimes carry it along the herb road.",
                "Marla: Clear the road and I will travel with you. Alderfall needs hands that can mend.",
            ],
            "slime_help",
            "riverside",
            "marla",
        ),
    ],
    "city_archive": [
        NPC(
            "Archivist Ren",
            "npc_ren",
            17,
            11,
            [
                "Ren: The Archive keeps the names of everyone the old wards failed to save.",
                "Ren: Now the dead are leaving their shelves and walking below Stonegate.",
                "Ren: Bring me proof the crypt can be quieted, and I will carry the record beside you.",
            ],
            "crypt_lights",
            None,
            "ren",
        ),
    ],
    "city_highwall": [
        NPC(
            "Captain Torin",
            "npc_torin",
            16,
            11,
            [
                "Torin: I lost a patrol at the west stones, and the raiders learned our names from their packs.",
                "Torin: Their brute leads every raid. Break him and the road opens again.",
                "Torin: Do that, and Highwall owes you my shield arm.",
            ],
            "orc_siege",
            None,
            "torin",
        ),
    ],
    "city_belltower": [
        NPC(
            "Bellkeeper Ilya",
            "npc_marla",
            17,
            9,
            [
                "Ilya: The bell tower used to keep ships and spirits honest.",
                "Ilya: Now the bronze shakes at midnight, and bats pour from the rafters.",
                "Ilya: Quiet those wings and the city can sleep again.",
            ],
            "shadow_swarm",
            "highwall",
        ),
    ],
    "city_sanctum": [
        NPC(
            "Warden Sol",
            "npc_ren",
            15,
            10,
            [
                "Sol: Sanctum was built around the first ward-stone, before the cities had names.",
                "Sol: Wraiths drift through cracks we cannot seal quickly enough.",
                "Sol: If you stand there, stand with iron in your heart.",
            ],
            "wraith_hunt",
        ),
        NPC(
            "Quartermaster Vesh",
            "npc_torin",
            21,
            11,
            [
                "Vesh: Lower tunnels chew through supplies and nerves alike.",
                "Vesh: Hunters report a broodmother with acid fangs below the vault.",
                "Vesh: If you descend, take every vial you can carry.",
            ],
            "broodmother",
            "crypt_vendor",
        ),
        NPC(
            "Scout Eira",
            "npc_marla",
            17,
            16,
            [
                "Eira: My cairns mark the northern pass, but frost wolves have started tearing them down.",
                "Eira: They hunt in pairs and circle caravans until the fires die.",
                "Eira: Bring me two pelts and I will guide your party through any whiteout.",
            ],
            "winter_fangs",
            None,
            "eira",
        ),
    ],
    "village_oakhaven": [
        NPC(
            "Edda",
            "npc_marla",
            13,
            8,
            [
                "Edda: Oakhaven keeps the old road fed.",
                "Edda: Traders bring rumors before coin, and every rumor lately has teeth.",
            ],
            shop_id="riverside",
        ),
        NPC(
            "Bran",
            "npc_torin",
            16,
            9,
            [
                "Bran: I kept the roadwatch until my company scattered at Stonegate.",
                "Bran: Pay my contract and I will keep your camp standing when the night gets loud.",
            ],
            recruit_id="bran",
            recruit_cost=40,
        ),
    ],
    "village_snowrest": [
        NPC(
            "Niva",
            "npc_ren",
            14,
            8,
            [
                "Niva: Snowrest is the last warm hearth before the pass.",
                "Niva: I read tracks in snow like scripture. Hire me and I will read the road ahead.",
            ],
            shop_id="highwall",
            recruit_id="niva",
            recruit_cost=90,
        ),
    ],
    "village_dunewick": [
        NPC(
            "Sela",
            "npc_marla",
            14,
            8,
            [
                "Sela: Dunewick survives by water, shade, and stubbornness.",
                "Sela: I know which wells lie and which badland trails ambush the careless. My fee is fair.",
            ],
            shop_id="riverside",
            recruit_id="sela",
            recruit_cost=85,
        ),
    ],
    "village_mireford": [
        NPC(
            "Fen",
            "npc_ren",
            14,
            8,
            [
                "Fen: Mireford is built on planks, patience, and listening to things under the water.",
                "Fen: Pay for my charms and I will make the marsh answer to us for once.",
            ],
            shop_id="crypt_vendor",
            recruit_id="fen",
            recruit_cost=100,
        ),
    ],
}


CITY_ENTER_TEXT = {
    "city_riverside": "You enter Riverside.",
    "city_archive": "You enter the Archive City.",
    "city_highwall": "You enter Highwall.",
    "city_belltower": "You enter Belltower City.",
    "city_sanctum": "You enter Sanctum City.",
}
CITY_LEAVE_TEXT = {
    "city_riverside": "You leave Riverside.",
    "city_archive": "You leave the Archive City.",
    "city_highwall": "You leave Highwall.",
    "city_belltower": "You leave Belltower City.",
    "city_sanctum": "You leave Sanctum City.",
}
VILLAGE_ENTER_TEXT = {
    "village_oakhaven": "You enter Oakhaven Village.",
    "village_snowrest": "You enter Snowrest Village.",
    "village_dunewick": "You enter Dunewick Village.",
    "village_mireford": "You enter Mireford Village.",
}
VILLAGE_LEAVE_TEXT = {
    "village_oakhaven": "You leave Oakhaven.",
    "village_snowrest": "You leave Snowrest.",
    "village_dunewick": "You leave Dunewick.",
    "village_mireford": "You leave Mireford.",
}

TRANSITIONS: dict[tuple[str, int, int], Transition] = {}
for city_map, (cx, cy) in CITY_GATE_CENTERS.items():
    city_rows_data = MAPS[city_map]
    city_cols, city_rows = len(city_rows_data[0]), len(city_rows_data)
    spawn_x, spawn_y = _city_spawn_point(city_cols, city_rows)
    overworld_rows = MAPS[OVERWORLD_ID]
    for dy in range(-5, 6):
        for dx in range(-5, 6):
            ox, oy = cx + dx, cy + dy
            if 0 <= oy < len(overworld_rows) and 0 <= ox < len(overworld_rows[0]) and overworld_rows[oy][ox] == "c":
                TRANSITIONS[(OVERWORLD_ID, ox, oy)] = Transition(city_map, spawn_x, spawn_y, CITY_ENTER_TEXT[city_map])
    city_gates = _city_gate_tiles(city_cols, city_rows)
    city_surface_exits = {
        "north": _surface_exit_point(overworld_rows, cx, cy, 0, -1, "c"),
        "west": _surface_exit_point(overworld_rows, cx, cy, -1, 0, "c"),
        "east": _surface_exit_point(overworld_rows, cx, cy, 1, 0, "c"),
        "south": _surface_exit_point(overworld_rows, cx, cy, 0, 1, "c"),
    }
    CITY_SURFACE_EXITS[city_map] = city_surface_exits["south"]
    city_exits = {
        city_gates["north"][0]: city_surface_exits["north"],
        city_gates["north"][1]: city_surface_exits["north"],
        city_gates["west"][0]: city_surface_exits["west"],
        city_gates["west"][1]: city_surface_exits["west"],
        city_gates["east"][0]: city_surface_exits["east"],
        city_gates["east"][1]: city_surface_exits["east"],
        city_gates["south"][0]: city_surface_exits["south"],
        city_gates["south"][1]: city_surface_exits["south"],
    }
    for (ix, iy), (ex, ey) in city_exits.items():
        TRANSITIONS[(city_map, ix, iy)] = Transition(OVERWORLD_ID, ex, ey, CITY_LEAVE_TEXT[city_map])

for village_map, (vx, vy) in VILLAGE_CENTERS.items():
    village_rows_data = MAPS[village_map]
    village_cols, village_rows = len(village_rows_data[0]), len(village_rows_data)
    spawn_x, spawn_y = _village_spawn_point(village_cols, village_rows)
    overworld_rows = MAPS[OVERWORLD_ID]
    for dy in range(-4, 5):
        for dx in range(-4, 5):
            ox, oy = vx + dx, vy + dy
            if 0 <= oy < len(overworld_rows) and 0 <= ox < len(overworld_rows[0]) and overworld_rows[oy][ox] == "u":
                TRANSITIONS[(OVERWORLD_ID, ox, oy)] = Transition(village_map, spawn_x, spawn_y, VILLAGE_ENTER_TEXT[village_map])
    village_gates = _city_gate_tiles(village_cols, village_rows)
    village_surface_exits = {
        "north": _surface_exit_point(overworld_rows, vx, vy, 0, -1, "u"),
        "west": _surface_exit_point(overworld_rows, vx, vy, -1, 0, "u"),
        "east": _surface_exit_point(overworld_rows, vx, vy, 1, 0, "u"),
        "south": _surface_exit_point(overworld_rows, vx, vy, 0, 1, "u"),
    }
    VILLAGE_SURFACE_EXITS[village_map] = village_surface_exits["south"]
    village_exits = {
        village_gates["north"][0]: village_surface_exits["north"],
        village_gates["north"][1]: village_surface_exits["north"],
        village_gates["west"][0]: village_surface_exits["west"],
        village_gates["west"][1]: village_surface_exits["west"],
        village_gates["east"][0]: village_surface_exits["east"],
        village_gates["east"][1]: village_surface_exits["east"],
        village_gates["south"][0]: village_surface_exits["south"],
        village_gates["south"][1]: village_surface_exits["south"],
    }
    for (ix, iy), (ex, ey) in village_exits.items():
        TRANSITIONS[(village_map, ix, iy)] = Transition(OVERWORLD_ID, ex, ey, VILLAGE_LEAVE_TEXT[village_map])

for dungeon_map, (dx, dy) in DUNGEON_ENTRANCES.items():
    TRANSITIONS[(OVERWORLD_ID, dx, dy)] = Transition(dungeon_map, 2, 10, f"You descend into {MAP_LABELS[dungeon_map]}.")
    TRANSITIONS[(dungeon_map, 1, 10)] = Transition(OVERWORLD_ID, dx, dy, "You climb back to the surface.")

# Stonegate inner depth
TRANSITIONS[("dungeon_stonegate_1", 28, 10)] = Transition("dungeon_stonegate_2", 2, 10, "You descend to the deep vault.")
TRANSITIONS[("dungeon_stonegate_2", 1, 10)] = Transition("dungeon_stonegate_1", 27, 10, "You return to the upper halls.")


def ensure_house_interior(source_map_id: str, wx: int, wy: int, return_x: int, return_y: int) -> str:
    building = city_building_at(source_map_id, wx, wy)
    if building is not None:
        wx, wy = building.anchor
    map_id = house_interior_id(source_map_id, wx, wy)
    if map_id not in MAPS:
        seed = (wx * 928371 + wy * 364479 + sum(ord(ch) for ch in source_map_id)) & 0xFFFFFFFF
        MAPS[map_id] = _house_interior_layout(seed)
        MAP_LABELS[map_id] = "House Interior" if source_map_id.startswith("village_") else "City Interior"
        MAP_KIND[map_id] = "interior"
    for ix, iy in HOUSE_EXITS:
        TRANSITIONS[(map_id, ix, iy)] = Transition(source_map_id, return_x, return_y, "You step back outside.")
    return map_id


def world_size(map_id: str = OVERWORLD_ID) -> tuple[int, int]:
    rows = MAPS.get(map_id, MAPS[OVERWORLD_ID])
    return len(rows[0]), len(rows)


def map_name(map_id: str) -> str:
    return MAP_LABELS.get(map_id, map_id)


def map_theme(map_id: str, x: int, y: int) -> str:
    kind = MAP_KIND.get(map_id, "overworld")
    if kind in {"city", "village"}:
        return "c"
    if kind == "dungeon":
        return "d"
    return tile_at(map_id, x, y)


def tile_at(map_id: str, x: int, y: int) -> str:
    rows = MAPS.get(map_id, MAPS[OVERWORLD_ID])
    cols = len(rows[0])
    h = len(rows)
    if 0 <= x < cols and 0 <= y < h:
        return rows[y][x]
    return "m"


def overworld_locations() -> tuple[LocationPatch, ...]:
    return OVERWORLD_LOCATIONS


def location_at(map_id: str, x: int, y: int) -> LocationPatch | None:
    if map_id != OVERWORLD_ID:
        return None
    for patch in OVERWORLD_LOCATIONS:
        if patch.contains(x, y):
            return patch
    return None


def is_passable(map_id: str, x: int, y: int) -> bool:
    return tile_at(map_id, x, y) in PASSABLE


def npcs_for_map(map_id: str) -> list[NPC]:
    return NPCS_BY_MAP.get(map_id, [])


def npc_at(map_id: str, x: int, y: int) -> NPC | None:
    for npc in npcs_for_map(map_id):
        if npc.x == x and npc.y == y:
            return npc
    return None


def transition_at(map_id: str, x: int, y: int) -> Transition | None:
    return TRANSITIONS.get((map_id, x, y))


def describe_tile(map_id: str, x: int, y: int) -> str:
    if MAP_KIND.get(map_id) == "city":
        tile = tile_at(map_id, x, y)
        return CITY_TERRAIN_NAMES.get(tile, TERRAIN_NAMES.get(tile, "City"))
    if MAP_KIND.get(map_id) == "village":
        return "Village Interior"
    if MAP_KIND.get(map_id) == "dungeon":
        return "Dungeon Interior"
    if MAP_KIND.get(map_id) == "interior":
        return "House Interior"
    location = location_at(map_id, x, y)
    if location:
        return LOCATION_NAMES.get(location.kind, "Location")
    return TERRAIN_NAMES.get(tile_at(map_id, x, y), "Unknown")


def encounter_pool(tile: str, level: int) -> list[str]:
    table = ENCOUNTER_TABLE.get(tile, [])
    result: list[str] = []
    for min_level, entries in table:
        if level >= min_level:
            result = entries
    return result


def location_encounter_pool(kind: str, level: int) -> list[str]:
    table = LOCATION_ENCOUNTER_TABLE.get(kind, [])
    result: list[str] = []
    for min_level, entries in table:
        if level >= min_level:
            result = entries
    return result


def location_encounter_group(kind: str, level: int) -> list[str]:
    for min_level, chance, group in reversed(LOCATION_BOSS_GROUPS.get(kind, ())):
        if level >= min_level and random() < chance:
            return list(group)
    return encounter_group("", level, location_encounter_pool(kind, level))


def encounter_group(tile: str, level: int, pool_override: list[str] | None = None) -> list[str]:
    pool = pool_override if pool_override is not None else encounter_pool(tile, level)
    if not pool:
        return []
    max_size = 1
    if level >= 3:
        max_size = 2
    if level >= 6 and len(pool) >= 3:
        max_size = 3
    size = 1
    if max_size >= 2 and random() < MONSTER_GROUP_CHANCES["two_monsters"]:
        size = 2
    if max_size >= 3 and random() < MONSTER_GROUP_CHANCES["three_monsters"]:
        size = 3
    if size == 1:
        return [choice(pool)]
    return sample(pool, min(size, len(pool)))


def roll_event(map_id: str, x: int, y: int, player_level: int) -> LocationEvent | None:
    npc = npc_at(map_id, x, y)
    if npc:
        return LocationEvent("npc", f"You meet {npc.name}.", npc=npc)

    kind = MAP_KIND.get(map_id, "overworld")
    tile = tile_at(map_id, x, y)
    if kind in {"city", "village"}:
        return None
    if kind == "dungeon":
        group = encounter_group("d", player_level)
        if group and random() < MONSTER_SPAWN_CHANCES["dungeon"]:
            return LocationEvent("battle", "Something stirs in the dungeon.", group[0], group)
        return None

    if tile == "c":
        return LocationEvent("city", "City gates stand nearby.")
    if tile == "u":
        return LocationEvent("village", "A village road opens nearby.")
    if tile == "d":
        location = location_at(map_id, x, y)
        group = location_encounter_group(location.kind, player_level) if location else encounter_group("d", player_level)
        if group and random() < MONSTER_SPAWN_CHANCES["dungeon_entrance"]:
            message = LOCATION_EVENT_TEXT.get(location.kind, "Something stirs in the dungeon approach.") if location else "Something stirs in the dungeon approach."
            return LocationEvent("battle", message, group[0], group)
        return LocationEvent("dungeon", "A cold draft rises from below.")
    location = location_at(map_id, x, y)
    if location:
        group = location_encounter_group(location.kind, player_level)
        chance = MONSTER_SPAWN_CHANCES["wild"]
        if location.kind == "goblin_camp":
            chance *= 1.65
        elif location.kind == "graveyard":
            chance *= 1.45
        elif location.kind == "farmland":
            chance *= 0.75
        if group and random() < min(0.45, chance):
            return LocationEvent("battle", LOCATION_EVENT_TEXT.get(location.kind, "Something moves nearby."), group[0], group)
    group = encounter_group(tile, player_level)
    if group and random() < MONSTER_SPAWN_CHANCES["wild"]:
        message = "Creatures leap from the wilds." if len(group) > 1 else "A creature leaps from the wilds."
        return LocationEvent("battle", message, group[0], group)
    return None
