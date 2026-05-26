from __future__ import annotations

import math
import os
import tkinter as tk
from collections import deque
from pathlib import Path

from .config import ASSET_DIR, TILE


class AssetStore:
    TILE_MARGINS: dict[str, int] = {}
    CITY_CUTOUT_ASSETS = {
        "city_house_front",
        "city_roof_detail",
        "city_wall_block",
        "city_wall_gate",
        "city_lantern",
        "city_bridge",
    }

    ASSET_FOLDERS = {
        "badlands": "terrain",
        "city": "terrain",
        "desert": "terrain",
        "dungeon": "terrain",
        "dungeon_floor": "terrain",
        "dungeon_wall": "terrain",
        "forest": "terrain",
        "grass": "terrain",
        "marsh": "terrain",
        "mountain": "terrain",
        "mountain_massif": "terrain",
        "mountain_massif_tile": "terrain",
        "mountain_peak": "terrain",
        "road": "terrain",
        "tundra": "terrain",
        "village": "terrain",
        "water": "terrain",
    }

    VARIANT_SHIFTS = {
        "grass": [(0, 0, 0), (8, 5, -3), (-6, -2, 4)],
        "forest": [(0, 0, 0), (6, 8, -4), (-7, -4, 3)],
        "mountain": [(0, 0, 0), (8, 8, 10), (-10, -8, -8)],
        "mountain_massif_tile": [(0, 0, 0), (6, 4, -3), (-5, -4, 5)],
        "water": [(0, 0, 0), (0, 10, 16), (0, 4, 10), (-3, 6, 14)],
        "road": [(0, 0, 0), (10, 6, -3), (-10, -4, 3)],
        "desert": [(0, 0, 0), (9, 6, -4), (-7, -5, 3)],
        "tundra": [(0, 0, 0), (8, 10, 12), (-8, -7, -3)],
        "marsh": [(0, 0, 0), (4, 8, -5), (-7, -5, 6)],
        "badlands": [(0, 0, 0), (10, 3, -5), (-9, -5, 4)],
        "city": [(0, 0, 0), (7, 5, 1), (-6, -5, -4)],
        "village": [(0, 0, 0), (7, 8, -4), (-7, -5, 3)],
        "dungeon": [(0, 0, 0), (5, 4, 8), (-6, -5, -7)],
        "dungeon_floor": [(0, 0, 0), (6, 4, 7), (-7, -5, -6)],
        "dungeon_wall": [(0, 0, 0), (5, 4, 6), (-6, -5, -6)],
        "deco_forest_cluster": [(0, 0, 0), (6, 10, -4), (-8, -7, 3)],
        "deco_forest_pine_cluster": [(0, 0, 0), (5, 9, -5), (-7, -6, 4)],
        "deco_forest_broadleaf_cluster": [(0, 0, 0), (7, 8, -3), (-8, -5, 2)],
        "deco_forest_mixed_cluster": [(0, 0, 0), (6, 9, -4), (-7, -6, 3)],
        "deco_tree_pine": [(0, 0, 0), (8, 11, -6), (-9, -8, 5)],
        "deco_tree_blue_pine": [(0, 0, 0), (4, 9, -3), (-7, -6, 7)],
        "deco_tree_oak": [(0, 0, 0), (9, 8, -4), (-8, -5, 3)],
        "deco_tree_round": [(0, 0, 0), (7, 9, -2), (-7, -6, 4)],
        "deco_tree_young": [(0, 0, 0), (8, 10, -5), (-8, -6, 4)],
        "deco_forest_fern": [(0, 0, 0), (8, 10, -4), (-7, -6, 4)],
        "deco_forest_log": [(0, 0, 0), (9, 6, -3), (-7, -5, 3)],
        "deco_forest_mushrooms": [(0, 0, 0), (7, 6, -3), (-6, -5, 4)],
        "deco_forest_moss_rock": [(0, 0, 0), (7, 7, -2), (-7, -6, 4)],
        "mountain_massif": [(0, 0, 0), (7, 5, -4), (-8, -6, 6)],
    }

    TEXTURE_VARIANT_COUNTS = {
        "forest": 8,
        "grass": 8,
        "tundra": 8,
    }

    CLASS_TINTS = {
        "Knight": (0, 0, 0),
        "Mage": (22, -18, 34),
        "Ranger": (-14, 16, -18),
    }

    def __init__(self, root: tk.Tk) -> None:
        self.root = root
        self.images: dict[str, tk.PhotoImage] = {}

    def get(self, name: str, size: int = TILE, variant: int = 0) -> tk.PhotoImage:
        variant = self._texture_variant(name, variant)
        key = f"{name}:{size}:{variant}"
        if key in self.images:
            return self.images[key]
        path = self._asset_path(name, variant=variant)
        if path.exists():
            # Load with Tkinter directly (fast for PNG files)
            image = tk.PhotoImage(file=os.fspath(path))
            texture_variant = self._is_texture_variant_path(name, variant, path)
            image = self._apply_city_cutout_alpha(name, image)
            if name in self.TILE_MARGINS:
                image = self._crop_inner(image, self.TILE_MARGINS[name])
            image = self._fit_size(image, size)
            if not texture_variant:
                image = self._apply_variant(name, image, variant)
            self.images[key] = image
            return image
        return self._fallback(key, name, size)

    def get_native(self, name: str) -> tk.PhotoImage | None:
        key = f"native:{name}"
        if key in self.images:
            return self.images[key]
        path = self._asset_path(name)
        if not path.exists():
            return None
        image = tk.PhotoImage(file=os.fspath(path))
        image = self._apply_city_cutout_alpha(name, image)
        self.images[key] = image
        return image

    def get_fit(self, name: str, width: int, height: int, variant: int = 0) -> tk.PhotoImage:
        variant = self._texture_variant(name, variant)
        key = f"fit:{name}:{width}:{height}:{variant}"
        if key in self.images:
            return self.images[key]
        path = self._asset_path(name, variant=variant)
        if not path.exists():
            return self._fallback(key, name, max(1, min(width, height)))
        image = tk.PhotoImage(file=os.fspath(path))
        texture_variant = self._is_texture_variant_path(name, variant, path)
        image = self._apply_city_cutout_alpha(name, image)
        if name in self.TILE_MARGINS:
            image = self._crop_inner(image, self.TILE_MARGINS[name])
        image = self._fit_rect(image, max(1, width), max(1, height))
        if not texture_variant:
            image = self._apply_variant(name, image, variant)
        self.images[key] = image
        return image

    def _asset_path(self, name: str, variant: int = 0) -> Path:
        category = self._asset_folder(name)
        if category:
            if variant > 0:
                variant_path = Path(ASSET_DIR) / category / f"{name}_variant_{variant}.png"
                if variant_path.exists():
                    return variant_path
            category_path = Path(ASSET_DIR) / category / f"{name}.png"
            if category_path.exists():
                return category_path
        if variant > 0:
            variant_root = Path(ASSET_DIR) / f"{name}_variant_{variant}.png"
            if variant_root.exists():
                return variant_root
        root_path = Path(ASSET_DIR) / f"{name}.png"
        if root_path.exists():
            return root_path
        for path in Path(ASSET_DIR).rglob(f"{name}.png"):
            if path == root_path:
                continue
            return path
        return root_path

    def _is_texture_variant_path(self, name: str, variant: int, path: Path) -> bool:
        return variant > 0 and path.name == f"{name}_variant_{variant}.png"

    def _texture_variant(self, name: str, variant: int) -> int:
        count = self.TEXTURE_VARIANT_COUNTS.get(name)
        if not count:
            return variant
        return variant % count

    def _apply_city_cutout_alpha(self, name: str, image: tk.PhotoImage) -> tk.PhotoImage:
        if name not in self.CITY_CUTOUT_ASSETS:
            return image
        width = image.width()
        height = image.height()
        if width <= 2 or height <= 2:
            return image

        visited: set[tuple[int, int]] = set()
        queue: deque[tuple[int, int]] = deque()

        def keylike(x: int, y: int) -> bool:
            r, g, b = image.get(x, y)
            if max(r, g, b) > 54:
                return False
            return b >= g - 2 and g >= r - 3

        for x in range(width):
            if keylike(x, 0):
                queue.append((x, 0))
            if keylike(x, height - 1):
                queue.append((x, height - 1))
        for y in range(height):
            if keylike(0, y):
                queue.append((0, y))
            if keylike(width - 1, y):
                queue.append((width - 1, y))

        while queue:
            x, y = queue.popleft()
            if (x, y) in visited or not (0 <= x < width and 0 <= y < height):
                continue
            if not keylike(x, y):
                continue
            visited.add((x, y))
            image.transparency_set(x, y, True)
            queue.append((x + 1, y))
            queue.append((x - 1, y))
            queue.append((x, y + 1))
            queue.append((x, y - 1))

        return image

    def _asset_folder(self, name: str) -> str | None:
        if name.startswith("battle_"):
            return "battle"
        if name.startswith("city_"):
            return "city"
        if name.startswith("location_"):
            return "locations"
        if name.startswith("class_") or name.startswith("player"):
            return "player"
        if name.startswith("deco_"):
            return "deco"
        if name.startswith("icon_"):
            return "items"
        if name.startswith("npc_"):
            return "npcs"
        if name.startswith("road_"):
            return "road"
        return self.ASSET_FOLDERS.get(name)

    def get_backdrop(self, name: str, width: int, height: int) -> tk.PhotoImage | None:
        key = f"backdrop:{name}:{width}:{height}"
        if key in self.images:
            return self.images[key]
        path = self._asset_path(name)
        if not path.exists():
            return None

        source = tk.PhotoImage(file=os.fspath(path))
        source_w = source.width()
        source_h = source.height()
        if source_w >= width and source_h >= height:
            left = max(0, (source_w - width) // 2)
            top = max(0, (source_h - height) // 2)
            image = source.copy(from_coords=(left, top, left + width, top + height))
        else:
            image = self._fit_rect(source, width, height)
        self.images[key] = image
        return image

    def get_backdrop_crop(self, name: str, width: int) -> tk.PhotoImage | None:
        key = f"backdrop_crop:{name}:{width}"
        if key in self.images:
            return self.images[key]
        path = self._asset_path(name)
        if not path.exists():
            return None

        source = tk.PhotoImage(file=os.fspath(path))
        source_w = source.width()
        source_h = source.height()
        crop_w = min(width, source_w)
        left = max(0, (source_w - crop_w) // 2)
        image = source.copy(from_coords=(left, 0, left + crop_w, source_h))
        if crop_w < width:
            framed = tk.PhotoImage(width=width, height=source_h)
            framed.copy(image, from_coords=(0, 0, crop_w, source_h))
            image = framed
        self.images[key] = image
        return image

    def get_backdrop_slice(self, name: str, width: int, height: int, top_ratio: float) -> tk.PhotoImage | None:
        key = f"backdrop_slice:{name}:{width}:{height}:{top_ratio:.2f}"
        if key in self.images:
            return self.images[key]
        path = self._asset_path(name)
        if not path.exists():
            return None

        source = tk.PhotoImage(file=os.fspath(path))
        source_w = source.width()
        source_h = source.height()
        if source_w >= width and source_h >= height:
            left = max(0, (source_w - width) // 2)
            top = max(0, min(source_h - height, int((source_h - height) * top_ratio)))
            image = source.copy(from_coords=(left, top, left + width, top + height))
        else:
            image = self._fit_rect(source, width, height)
        self.images[key] = image
        return image

    def get_hill_strip(self, name: str, width: int, height: int, top_ratio: float) -> tk.PhotoImage | None:
        key = f"hill_strip:{name}:{width}:{height}:{top_ratio:.2f}"
        if key in self.images:
            return self.images[key]
        base = self.get_backdrop_slice(name, width, height, top_ratio)
        if not base:
            return None

        hill = tk.PhotoImage(width=width, height=height)
        for y in range(height):
            for x in range(width):
                curve = int(
                    height * 0.24
                    + 9 * self._wave(x, 0.017, 0)
                    + 5 * self._wave(x, 0.041, 1.7)
                )
                if y < curve:
                    hill.transparency_set(x, y, True)
                    continue
                r, g, b = base.get(x, y)
                depth = (y - curve) / max(1, height - curve)
                r = min(255, max(0, int(r * (0.70 + depth * 0.16))))
                g = min(255, max(0, int(g * (0.78 + depth * 0.14))))
                b = min(255, max(0, int(b * (0.64 + depth * 0.10))))
                hill.put(f"#{r:02x}{g:02x}{b:02x}", to=(x, y))
        self.images[key] = hill
        return hill

    def get_treeline_cutout(self, name: str, width: int, height: int, top_ratio: float) -> tk.PhotoImage | None:
        key = f"treeline_cutout:{name}:{width}:{height}:{top_ratio:.2f}"
        if key in self.images:
            return self.images[key]
        base = self.get_backdrop_slice(name, width, height, top_ratio)
        if not base:
            return None

        cutout = tk.PhotoImage(width=width, height=height)
        for y in range(height):
            for x in range(width):
                r, g, b = base.get(x, y)
                green = g > 42 and g >= r + 4 and g >= b * 0.72
                deep_green = y > height * 0.32 and g > 35 and r < 105 and b < 120
                trunk = y > height * 0.46 and r < 115 and g < 105 and b < 90
                sky_or_mountain = b >= g + 8 and y < height * 0.72
                cloud = r > 165 and g > 170 and b > 170
                if y < height * 0.18 or cloud or sky_or_mountain or not (green or deep_green or trunk):
                    cutout.transparency_set(x, y, True)
                    continue
                depth = min(1.0, y / max(1, height))
                r = min(255, max(0, int(r * (0.72 + depth * 0.16))))
                g = min(255, max(0, int(g * (0.76 + depth * 0.14))))
                b = min(255, max(0, int(b * (0.68 + depth * 0.10))))
                cutout.put(f"#{r:02x}{g:02x}{b:02x}", to=(x, y))
        self.images[key] = cutout
        return cutout

    def get_character_pose(self, name: str, size: int = TILE, facing: str = "down", step: int = 0) -> tk.PhotoImage:
        key = f"pose:{name}:{size}:{facing}:{step % 4}"
        if key in self.images:
            return self.images[key]
        image = self.get(name, size).copy()
        stride = 0
        phase = step % 4
        if phase == 1:
            stride = -2
        elif phase == 3:
            stride = 2
        elif phase == 2:
            stride = 1

        if facing == "left":
            image = self._mirror_horizontal(image)
            stride = -stride
        elif facing == "up":
            image = self._tint_copy(image, -20, -18, -14)
        elif facing == "right":
            image = self._tint_copy(image, 4, 4, 6)

        if stride:
            image = self._stride_lower(image, stride)
        self.images[key] = image
        return image

    def get_class_character_pose(
        self,
        class_name: str,
        *,
        size: int = TILE,
        facing: str = "down",
        step: int = 0,
        battle: bool = False,
    ) -> tk.PhotoImage:
        key = f"class_pose:{class_name}:{size}:{facing}:{step % 4}:{int(battle)}"
        if key in self.images:
            return self.images[key]
        model_name = f"class_{class_name.lower()}_model"
        if self.get_native(model_name):
            image = self.get_character_pose(model_name, size=size, facing=facing, step=step)
            self.images[key] = image
            return image
        if not battle and self.get_native("player_model"):
            image = self.get_character_pose("player_model", size=size, facing=facing, step=step)
            dr, dg, db = self.CLASS_TINTS.get(class_name, (0, 0, 0))
            if dr or dg or db:
                image = self._tint_copy(image, dr, dg, db)
            self.images[key] = image
            return image
        base_name = "player_battle" if battle else "player"
        image = self.get_character_pose(base_name, size=size, facing=facing, step=step)
        dr, dg, db = self.CLASS_TINTS.get(class_name, (0, 0, 0))
        if dr or dg or db:
            image = self._tint_copy(image, dr, dg, db)
        self.images[key] = image
        return image

    def _crop_inner(self, image: tk.PhotoImage, margin: int) -> tk.PhotoImage:
        width = image.width()
        height = image.height()
        if width <= margin * 2 + 2 or height <= margin * 2 + 2:
            return image
        return image.copy(from_coords=(margin, margin, width - margin, height - margin))

    def _apply_variant(self, name: str, image: tk.PhotoImage, variant: int) -> tk.PhotoImage:
        shifts = self.VARIANT_SHIFTS.get(name)
        if not shifts:
            return image
        dr, dg, db = shifts[variant % len(shifts)]
        if dr == 0 and dg == 0 and db == 0:
            return image
        tinted = tk.PhotoImage(width=image.width(), height=image.height())
        for y in range(image.height()):
            for x in range(image.width()):
                if image.transparency_get(x, y):
                    tinted.transparency_set(x, y, True)
                    continue
                r, g, b = image.get(x, y)
                r = min(255, max(0, r + dr))
                g = min(255, max(0, g + dg))
                b = min(255, max(0, b + db))
                tinted.put(f"#{r:02x}{g:02x}{b:02x}", to=(x, y))
        return tinted

    def _fit_size(self, image: tk.PhotoImage, size: int) -> tk.PhotoImage:
        width = image.width()
        height = image.height()
        if width == size and height == size:
            return image

        scale = min(size / max(1, width), size / max(1, height))
        new_width = max(1, int(width * scale))
        new_height = max(1, int(height * scale))
        framed = tk.PhotoImage(width=size, height=size)
        offset_x = (size - new_width) // 2
        offset_y = (size - new_height) // 2

        for y in range(new_height):
            src_y = min(height - 1, int(y * height / new_height))
            for x in range(new_width):
                src_x = min(width - 1, int(x * width / new_width))
                if image.transparency_get(src_x, src_y):
                    framed.transparency_set(offset_x + x, offset_y + y, True)
                    continue
                r, g, b = image.get(src_x, src_y)
                framed.put(f"#{r:02x}{g:02x}{b:02x}", to=(offset_x + x, offset_y + y))
        return framed

    def _fit_rect(self, image: tk.PhotoImage, width: int, height: int) -> tk.PhotoImage:
        src_w = image.width()
        src_h = image.height()
        framed = tk.PhotoImage(width=width, height=height)
        for y in range(height):
            src_y = min(src_h - 1, int(y * src_h / height))
            for x in range(width):
                src_x = min(src_w - 1, int(x * src_w / width))
                if image.transparency_get(src_x, src_y):
                    framed.transparency_set(x, y, True)
                    continue
                r, g, b = image.get(src_x, src_y)
                framed.put(f"#{r:02x}{g:02x}{b:02x}", to=(x, y))
        return framed

    def _wave(self, x: int, frequency: float, phase: float) -> float:
        return math.sin(x * frequency + phase)

    def _mirror_horizontal(self, image: tk.PhotoImage) -> tk.PhotoImage:
        width = image.width()
        height = image.height()
        mirrored = tk.PhotoImage(width=width, height=height)
        for y in range(height):
            for x in range(width):
                src_x = width - 1 - x
                if image.transparency_get(src_x, y):
                    mirrored.transparency_set(x, y, True)
                    continue
                r, g, b = image.get(src_x, y)
                mirrored.put(f"#{r:02x}{g:02x}{b:02x}", to=(x, y))
        return mirrored

    def _tint_copy(self, image: tk.PhotoImage, dr: int, dg: int, db: int) -> tk.PhotoImage:
        tinted = tk.PhotoImage(width=image.width(), height=image.height())
        for y in range(image.height()):
            for x in range(image.width()):
                if image.transparency_get(x, y):
                    tinted.transparency_set(x, y, True)
                    continue
                r, g, b = image.get(x, y)
                r = min(255, max(0, r + dr))
                g = min(255, max(0, g + dg))
                b = min(255, max(0, b + db))
                tinted.put(f"#{r:02x}{g:02x}{b:02x}", to=(x, y))
        return tinted

    def _stride_lower(self, image: tk.PhotoImage, offset: int) -> tk.PhotoImage:
        width = image.width()
        height = image.height()
        split = int(height * 0.58)
        stride = tk.PhotoImage(width=width, height=height)
        for y in range(height):
            for x in range(width):
                stride.transparency_set(x, y, True)

        for y in range(height):
            row_shift = 0
            if y >= split:
                row_shift = offset if ((y - split) // 3) % 2 == 0 else -offset
            for x in range(width):
                if image.transparency_get(x, y):
                    continue
                tx = x + row_shift
                if not (0 <= tx < width):
                    continue
                r, g, b = image.get(x, y)
                stride.put(f"#{r:02x}{g:02x}{b:02x}", to=(tx, y))
        return stride

    def _fallback(self, key: str, name: str, size: int) -> tk.PhotoImage:
        palette = {
            "grass": "#5cad55",
            "forest": "#287044",
            "mountain": "#7e8791",
            "water": "#3c89d0",
            "road": "#c9a46d",
            "city": "#b66a42",
            "dungeon": "#555466",
            "player": "#f2d16b",
        }
        color = palette.get(name, "#d35f5f")
        image = tk.PhotoImage(width=size, height=size)
        image.put(color, to=(0, 0, size, size))
        self.images[key] = image
        return image
