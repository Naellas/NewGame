from __future__ import annotations

import math
from typing import TYPE_CHECKING

from .world import MAPS, OVERWORLD_ID, city_buildings_for_map, npcs_for_map, tile_at

if TYPE_CHECKING:
    from .app import GameApp


class WorldRenderer:
    def __init__(self, app: GameApp) -> None:
        self.app = app
        self.static_key: tuple[str, int, int, str, tuple[int, int, int, int, int]] | None = None
        self.static_offset: tuple[int, int] = (0, 0)

    def draw(self) -> None:
        if self.app.render_mode == "fast":
            self.static_key = None
            self.app.canvas.delete("all")
            self.draw_fast()
        elif self.app.render_mode == "balanced":
            self.draw_balanced_cached()
        else:
            self.static_key = None
            self.app.canvas.delete("all")
            self.app.draw_world_full()

    def draw_balanced_cached(self) -> None:
        app = self.app
        key = (app.current_map_id, app.camera_x, app.camera_y, app.render_mode, app.layout_key())
        if key != self.static_key:
            app.canvas.delete("world_static")
            start_id = self.last_canvas_item_id()
            self.draw_balanced_static()
            self.tag_items_created_after("world_static", start_id)
            self.static_key = key
            self.static_offset = (0, 0)

        offset = self.camera_pixel_offset()
        dx = offset[0] - self.static_offset[0]
        dy = offset[1] - self.static_offset[1]
        if dx or dy:
            app.canvas.move("world_static", dx, dy)
            self.static_offset = offset

        app.canvas.delete("world_dynamic")
        start_id = self.last_canvas_item_id()
        app.draw_forest_liveliness()
        self.draw_actors()
        app.draw_cloud_layer()
        self.draw_viewport_mask()
        app.draw_sidebar()
        self.tag_items_created_after("world_dynamic", start_id)

    def last_canvas_item_id(self) -> int:
        items = self.app.canvas.find_all()
        return items[-1] if items else 0

    def tag_items_created_after(self, tag: str, start_id: int) -> None:
        end_id = self.last_canvas_item_id()
        if end_id <= start_id:
            return
        # Canvas item IDs increase monotonically; tagging this ID span avoids
        # expensive full-canvas set diffs each frame in dense maps.
        for item_id in range(start_id + 1, end_id + 1):
            self.app.canvas.addtag_withtag(tag, item_id)

    def camera_pixel_offset(self) -> tuple[int, int]:
        app = self.app
        return (
            -int(round((app.camera_view_x - app.camera_x) * app.tile_size())),
            -int(round((app.camera_view_y - app.camera_y) * app.tile_size())),
        )

    def draw_viewport_mask(self) -> None:
        app = self.app
        map_bottom = app.map_rows() * app.tile_size()
        map_right = app.map_cols() * app.tile_size()
        app.canvas.create_rectangle(0, map_bottom, map_right, app.canvas_height, fill="#16171f", outline="")

    def draw_city_building_entities(self) -> None:
        app = self.app
        app._drawn_city_buildings.clear()
        for building in city_buildings_for_map(app.current_map_id):
            if building.x2 < app.camera_x - 5 or building.x1 > app.camera_x + app.map_cols() + 5:
                continue
            if building.y2 < app.camera_y - 5 or building.y1 > app.camera_y + app.map_rows() + 3:
                continue
            app.draw_city_building_entity(
                building.x1 - app.camera_x,
                building.y1 - app.camera_y,
                building.x1,
                building.y1,
                building,
            )

    def draw_fast(self) -> None:
        app = self.app
        city_map = app.current_map_id.startswith("city_")
        dungeon_map = app.current_map_id.startswith("dungeon_")
        app._drawn_city_buildings.clear()
        rows = MAPS.get(app.current_map_id, MAPS[OVERWORLD_ID])
        map_cols = len(rows[0])
        map_rows = len(rows)
        tile_size = app.tile_size()
        for sy in range(app.map_rows() + 1):
            wy = app.camera_y + sy
            for sx in range(app.map_cols() + 1):
                wx = app.camera_x + sx
                tile = rows[wy][wx] if 0 <= wx < map_cols and 0 <= wy < map_rows else "m"
                px = sx * tile_size
                py = sy * tile_size
                if city_map:
                    app.draw_city_ground_tile(sx, sy, wx, wy, tile)
                else:
                    app.canvas.create_rectangle(px, py, px + tile_size, py + tile_size, fill=app.map_tile_color(tile), outline="")
                    if app.current_map_id == OVERWORLD_ID:
                        app.draw_location_ground(sx, sy, wx, wy, tile)
                if not city_map and tile in {"r", "p", "c", "d"}:
                    road = "#d9b36f"
                    if dungeon_map:
                        road = "#65587d"
                    inset = max(7, int(tile_size * 0.29))
                    app.canvas.create_rectangle(px + inset, py + inset, px + tile_size - inset, py + tile_size - inset, fill=road, outline="")
                if tile == "w" and app.frame % 4 == 0:
                    app.draw_water_shimmer(sx, sy, wx, wy)
                elif city_map and tile in {"a", "y", "t"}:
                    app.draw_city_feature_tile(sx, sy, wx, wy, tile)
                elif tile == "h":
                    if not city_map:
                        app.canvas.create_rectangle(px + 7, py + 7, px + tile_size - 7, py + tile_size - 5, fill="#8b4b32", outline="#5f2f26")
                elif tile == "x":
                    app.canvas.create_rectangle(px + 4, py + 4, px + tile_size - 4, py + tile_size - 4, fill="#777a7c", outline="#5f5f5a")
                elif tile == "d" and not dungeon_map:
                    app.canvas.create_image(px + tile_size // 2, py + tile_size // 2, image=app.assets.get("dungeon", max(24, int(tile_size * 0.75))))
                if app.current_map_id == OVERWORLD_ID and tile == "c" and app.is_city_cluster_center(wx, wy):
                    app.canvas.create_image(px + tile_size // 2, py + tile_size // 2 - 5, image=app.assets.get("city", max(24, int(tile_size * 0.88))))

        if city_map:
            self.draw_city_building_entities()

        self.draw_actors()
        app.draw_cloud_layer()
        self.draw_viewport_mask()
        app.draw_sidebar()

    def draw_balanced(self) -> None:
        self.draw_balanced_static()
        self.app.draw_forest_liveliness()
        self.draw_actors()
        self.app.draw_cloud_layer()
        self.draw_viewport_mask()
        self.app.draw_sidebar()

    def draw_balanced_static(self) -> None:
        app = self.app
        current_map_id = app.current_map_id
        rows = MAPS.get(current_map_id, MAPS[OVERWORLD_ID])
        map_cols = len(rows[0])
        map_rows = len(rows)
        overworld = app.current_map_id == OVERWORLD_ID
        city_map = current_map_id.startswith(("city_", "village_"))
        city_interior_map = current_map_id.startswith("city_")
        dungeon_map = current_map_id.startswith("dungeon_")
        house_map = current_map_id.startswith("house_")
        visible_tiles: list[tuple[int, int, int, int, str]] = []
        tile_size = app.tile_size()
        for sy in range(app.map_rows() + 1):
            wy = app.camera_y + sy
            for sx in range(app.map_cols() + 1):
                wx = app.camera_x + sx
                tile = rows[wy][wx] if 0 <= wx < map_cols and 0 <= wy < map_rows else "m"
                visible_tiles.append((sx, sy, wx, wy, tile))
                if city_interior_map:
                    app.draw_city_ground_tile(sx, sy, wx, wy, tile)
                    continue
                name = app.tile_assets[tile]
                if overworld and tile == "r":
                    name = app.path_base_asset(wx, wy)
                elif overworld and tile == "c":
                    name = app.settlement_base_asset(wx, wy)
                elif overworld and tile == "u":
                    name = app.settlement_base_asset(wx, wy)
                elif current_map_id.startswith("village_") and tile == "r":
                    name = "grass"
                elif city_map and not overworld:
                    if tile == "p":
                        name = "city_cobble"
                    elif tile == "r":
                        name = "city_snow_cobble" if current_map_id.startswith("village_snow") else "city_cobble"
                    elif tile in {"h", "x", "a", "j", "l", "t"}:
                        name = "city_snow_cobble" if current_map_id.startswith("village_snow") else "city_cobble"
                elif overworld and tile == "m":
                    name = app.mountain_base_asset(wx, wy)
                elif overworld and tile == "q":
                    name = app.mountain_pass_base_asset(wx, wy)
                elif dungeon_map and tile in {"r", "d"}:
                    name = "dungeon_floor"
                elif dungeon_map and tile == "x":
                    name = "dungeon_wall"
                variant = app.terrain_variant(wx, wy, tile)
                blended = tile in {"g", "f", "s", "n", "v", "b", "m", "q", "w", "r", "p", "j", "l", "a", "y", "x"} or (overworld and tile in {"c", "u"})
                draw_size = tile_size + 2 if blended else tile_size
                draw_x = sx * tile_size - 1 if blended else sx * tile_size
                draw_y = sy * tile_size - 1 if blended else sy * tile_size
                app.canvas.create_image(draw_x, draw_y, anchor="nw", image=app.assets.get(name, draw_size, variant=variant))

        for sx, sy, wx, wy, tile in visible_tiles:
            if overworld:
                app.draw_biome_blend(sx, sy, wx, wy, tile)
                app.draw_mountain_foothill_blend(sx, sy, wx, wy)
                app.draw_location_ground(sx, sy, wx, wy, tile)
            if house_map and tile in {"i", "e", "z"}:
                app.draw_house_floor_tile(sx, sy, wx, wy, tile)
            elif house_map and tile == "k":
                app.draw_house_furniture_tile(sx, sy, wx, wy)
            elif house_map and tile == "o":
                app.draw_house_wall_tile(sx, sy, wx, wy)
            elif tile in {"w", "r", "p", "a", "y", "d"}:
                if tile == "w":
                    app.draw_water_shimmer(sx, sy, wx, wy)
                    if overworld:
                        app.draw_coastline_overlay(sx, sy, wx, wy)
                elif tile == "p" and not overworld and not city_interior_map:
                    app.draw_city_plaza(sx, sy, wx, wy)
                elif tile in {"a", "y"} and not overworld:
                    app.draw_city_feature_tile(sx, sy, wx, wy, tile)
                elif tile in {"r", "d"} and not overworld and not city_interior_map:
                    app.draw_path_overlay(sx, sy, wx, wy, tile, city_map, dungeon_map)
            if overworld and tile != "r":
                app.draw_biome_detail(sx, sy, wx, wy, tile)

        if overworld:
            for sx, sy, wx, wy, tile in visible_tiles:
                if tile == "m":
                    app.draw_mountain_massif(sx, sy, wx, wy)
            for sx, sy, wx, wy, tile in visible_tiles:
                if tile == "r":
                    app.draw_path_overlay(sx, sy, wx, wy, tile, False, False)
                    app.draw_biome_detail(sx, sy, wx, wy, tile)

        app._drawn_city_buildings.clear()
        for sx, sy, wx, wy, tile in visible_tiles:
            if overworld and tile == "c":
                if app.is_city_cluster_center(wx, wy):
                    app.draw_overworld_city_cluster(sx, sy, wx, wy)
            elif overworld and tile == "u":
                if app.is_village_cluster_center(wx, wy):
                    app.draw_overworld_village_cluster(sx, sy, wx, wy)
            elif tile == "h" and not city_interior_map:
                app.draw_city_house(sx, sy, wx, wy)
            elif tile == "t":
                app.draw_city_feature_tile(sx, sy, wx, wy, tile)
            elif tile == "x":
                app.draw_stone_wall(sx, sy, wx, wy)
            elif tile == "d" and dungeon_map:
                app.draw_dungeon_floor_detail(sx, sy, wx, wy)
        if city_interior_map:
            self.draw_city_building_entities()

    def draw_actors(self) -> None:
        app = self.app
        tile_size = app.tile_size()
        actor_size = max(tile_size, int(round(tile_size * 1.16)))
        for npc in npcs_for_map(app.current_map_id):
            if app.camera_x <= npc.x <= app.camera_x + app.map_cols() and app.camera_y <= npc.y <= app.camera_y + app.map_rows():
                px = int((npc.x - app.camera_view_x) * tile_size + tile_size // 2)
                py = int((npc.y - app.camera_view_y) * tile_size + tile_size // 2)
                pulse = int(2 * math.sin((app.frame + npc.x * 3 + npc.y * 5) / 8))
                model_name = f"{npc.sprite}_model" if app.assets.get_native(f"{npc.sprite}_model") else npc.sprite
                app.canvas.create_oval(px - 12, py + tile_size // 2 - 10, px + 12, py + tile_size // 2 - 4, fill="#111820", outline="")
                app.canvas.create_image(
                    px,
                    py - pulse,
                    image=app.assets.get_character_pose(model_name, size=actor_size, facing="down", step=(app.frame // 12) % 4),
                )
                marker = app.npc_marker(npc)
                if marker:
                    app.canvas.create_image(px, py - tile_size // 2 + 8, image=app.assets.get(marker, max(14, int(tile_size * 0.42))))

        bob = int(3 * math.sin(app.frame / 4))
        px = int((app.render_x - app.camera_view_x) * tile_size + tile_size // 2)
        py = int((app.render_y - app.camera_view_y) * tile_size + tile_size // 2 - bob)
        moving = abs(app.player_x - app.render_x) > 0.03 or abs(app.player_y - app.render_y) > 0.03
        step = (app.frame // 3) % 4 if moving else (app.walk_frame if app.walk_timer > 0 else 0)
        app.canvas.create_image(
            px,
            py,
            image=app.assets.get_class_character_pose(app.player_class_name, size=actor_size, facing=app.facing, step=step),
        )
