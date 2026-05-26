from __future__ import annotations

import ctypes
import math
import os
from pathlib import Path
from random import randrange
import sys
import tkinter as tk
from dataclasses import dataclass
from typing import Callable

from . import config as game_config
from .assets import AssetStore
from .battle import Battle
from .config import (
    FPS_MS,
    HEIGHT as BASE_HEIGHT,
    MAP_COLS as BASE_MAP_COLS,
    MAP_ROWS as BASE_MAP_ROWS,
    SIDEBAR_WIDTH as BASE_SIDEBAR_WIDTH,
    TILE as BASE_TILE,
    WIDTH as BASE_WIDTH,
)
from .data import CLASS_PERKS, CLASS_UNLOCKS, CLASSES, ITEMS, QUESTS, RECRUITS, SHOPS, STORY_BEATS, item_cost, item_icon, item_name
from .decorations import (
    BIOME_DECORATIONS,
    FOREST_FEATURE_DECORATIONS,
    FOREST_FLOOR_DECORATIONS,
    FOREST_TREE_DECORATIONS,
    LOCATION_DECORATIONS,
    LOCATION_GROUND_ASSETS,
    all_decoration_assets,
    decoration_primary_tag,
    decoration_spawn_profile,
    decoration_size,
    weighted_decoration_choice,
)
from .effects import STATUS_EFFECTS, STATUS_ICON_LABELS, EffectStack, status_hints_for_ability
from .equipment import ALL_EQUIPMENT
from .entities import Ability, Actor, NPC
from .music import MusicManager
from .renderer import WorldRenderer
from .save_system import list_save_summaries, load_game, reset_quest_state, save_exists, save_game, unique_save_id
from .skill_tree import (
    COMMON_SKILL_TREE,
    SKILL_ABILITY_NAMES,
    SKILL_TREE,
    SkillNode,
    available_skill_tree,
    skill_order_for_class,
    skill_respec_cost,
    skill_spent,
    skill_tree_for_class,
)
from .world import (
    CITY_GATE_CENTERS,
    CITY_SURFACE_EXITS,
    CityBuilding,
    DUNGEON_ENTRANCES,
    HOUSE_SPAWN,
    MAP_LABELS,
    MAPS,
    OVERWORLD_ID,
    START_POSITION,
    VILLAGE_CENTERS,
    VILLAGE_SURFACE_EXITS,
    city_buildings_for_map,
    city_building_at,
    city_building_door_tiles,
    city_building_entry_at,
    current_world_seed,
    describe_tile,
    ensure_house_interior,
    is_passable,
    location_at,
    map_name,
    map_theme,
    npcs_for_map,
    roll_event,
    set_world_seed,
    tile_at,
    transition_at,
    world_size,
)

TILE_ASSETS = {
    "g": "grass",
    "f": "forest",
    "s": "desert",
    "n": "tundra",
    "v": "marsh",
    "b": "badlands",
    "m": "mountain_massif_tile",
    "q": "mountain_massif_tile",
    "w": "water",
    "r": "road",
    "c": "city",
    "u": "village",
    "d": "dungeon",
    "p": "road",
    "j": "road",
    "l": "road",
    "a": "city",
    "y": "grass",
    "t": "city",
    "h": "city",
    "x": "mountain",
    "i": "road",
    "e": "road",
    "z": "road",
    "k": "city",
    "o": "city",
}
VILLAGE_OVERWORLD_CLUSTER_ASSETS = {
    "village_oakhaven": "city_overworld_village_green",
    "village_snowrest": "city_overworld_village_snow",
    "village_dunewick": "city_overworld_village_desert",
    "village_mireford": "city_overworld_village_marsh",
}
CITY_SOURCE_SNOW_PROPS = (
    ("city_prop_source_barrel_open", 23),
    ("city_prop_source_crate_low", 22),
    ("city_prop_source_barrel_tall", 23),
    ("city_prop_source_crate_tall", 24),
    ("city_prop_source_stone_pile", 22),
    ("city_prop_source_snow_bush", 22),
    ("city_prop_source_reeds", 21),
    ("city_prop_source_snow_stump", 21),
)
CITY_OVERWORLD_TEMPERATE_PROPS = (
    ("city_prop_barrel", 18),
    ("city_prop_barrel_stack", 19),
    ("city_prop_crate", 18),
    ("city_prop_cart", 22),
    ("city_prop_flower_pot", 17),
    ("city_prop_plant_box", 18),
    ("city_prop_planter_stone", 18),
    ("city_prop_market_green", 20),
    ("city_prop_market_red", 20),
    ("city_prop_market_yellow", 20),
)
BLEND_COLORS = {
    "g": "#6f8f42",
    "f": "#334f2b",
    "s": "#c5a05c",
    "n": "#c9dbe3",
    "v": "#426a56",
    "b": "#8d6549",
    "m": "#737b7c",
    "q": "#686459",
    "w": "#3f8fb9",
    "r": "#c69b64",
    "c": "#b99668",
}
MAP_TILE_COLORS = {
    "g": "#5f9f4a",
    "f": "#235c36",
    "s": "#caa15d",
    "n": "#dce8f0",
    "v": "#406b55",
    "b": "#8f6f55",
    "m": "#7d858f",
    "q": "#777168",
    "w": "#2e87bd",
    "r": "#d9b36f",
    "c": "#d7bf82",
    "u": "#b9ad76",
    "d": "#3b3348",
    "p": "#b9ae97",
    "j": "#9a9384",
    "l": "#7d8580",
    "a": "#bf8f61",
    "y": "#76a864",
    "t": "#8a8272",
    "h": "#9a623e",
    "x": "#777a7c",
    "i": "#8a6241",
    "e": "#5b3624",
    "z": "#9b4d44",
    "k": "#6f4a2d",
    "o": "#6f5740",
}
MAP_PAINT_COLORS = {
    "1": "#ff5f7d",
    "2": "#ffb357",
    "3": "#f9ef64",
    "4": "#6ee580",
    "5": "#7ecbff",
    "6": "#b18bff",
    "7": "#f59cff",
    "8": "#ffffff",
}
MAP_PAINT_LABELS = {
    "1": "Rose",
    "2": "Amber",
    "3": "Sun",
    "4": "Leaf",
    "5": "Sky",
    "6": "Violet",
    "7": "Bloom",
    "8": "Chalk",
}
TERRAIN_BLEND_TILES = {"g", "f", "s", "n", "v", "b", "m", "q", "w"}
TERRAIN_BLEND_PRIORITY = {
    "w": 0,
    "s": 1,
    "n": 1,
    "v": 1,
    "b": 2,
    "g": 3,
    "f": 4,
    "q": 4,
    "m": 5,
}
TERRAIN_EDGE_HIGHLIGHTS = {
    "g": "#91b95d",
    "f": "#5f8450",
    "s": "#ead28a",
    "n": "#eef7fb",
    "v": "#668b6d",
    "b": "#bd8b62",
    "m": "#b8bec2",
    "q": "#b9ad95",
    "w": "#8ee6ff",
}
MUSIC_DIR = Path(__file__).resolve().parents[2] / "assets" / "music"
MUSIC_BY_THEME = {
    "g": "zone_grasslands",
    "r": "zone_grasslands",
    "f": "zone_forest",
    "s": "zone_desert",
    "n": "zone_tundra",
    "v": "zone_marsh",
    "b": "zone_badlands",
    "m": "zone_mountains",
    "q": "zone_mountains",
    "w": "zone_water",
    "c": "town_village",
    "u": "town_village",
    "d": "dungeon_crypt",
}
BOSS_MUSIC_KEYS = {
    "acid_broodmother",
    "bone_knight",
    "crypt_revenant",
    "elder_wraith",
    "goblin_king",
    "goblin_warlord",
    "ice_golem",
    "orc_champion",
}
WORLD_MUSIC_MODES = {"world", "dialog", "shop", "map", "skills", "inventory"}
MUSIC_HOLD_MODES = {"pause_menu", "settings", "confirm_action"}
WINDOW_BG = "#0f1118"
WIDTH = BASE_WIDTH
HEIGHT = BASE_HEIGHT
TILE = BASE_TILE
MAP_COLS = BASE_MAP_COLS
MAP_ROWS = BASE_MAP_ROWS
SIDEBAR_WIDTH = BASE_SIDEBAR_WIDTH
MAP_BOTTOM = MAP_ROWS * TILE


def enable_windows_dpi_awareness() -> None:
    try:
        ctypes.windll.shcore.SetProcessDpiAwareness(1)
    except (AttributeError, OSError):
        try:
            ctypes.windll.user32.SetProcessDPIAware()
        except (AttributeError, OSError):
            pass


@dataclass
class ClickZone:
    x1: int
    y1: int
    x2: int
    y2: int
    action: Callable[[], None]

    def hit(self, x: int, y: int) -> bool:
        return self.x1 <= x <= self.x2 and self.y1 <= y <= self.y2


@dataclass
class HoverZone:
    x1: int
    y1: int
    x2: int
    y2: int
    title: str
    body: str

    def hit(self, x: int, y: int) -> bool:
        return self.x1 <= x <= self.x2 and self.y1 <= y <= self.y2


@dataclass(frozen=True)
class DecorationCandidate:
    asset: str
    tag: str
    seed: int
    chance: int
    cluster: float
    size: int
    ox: int
    oy: int
    variant: int


class GameApp:
    def __init__(self) -> None:
        enable_windows_dpi_awareness()
        self.root = tk.Tk()
        self.root.title("Echoes of Alderfall")
        self.root.configure(bg=WINDOW_BG)
        self.fullscreen = False
        self.canvas_width = WIDTH
        self.canvas_height = HEIGHT
        self.ui_scale = 1.0
        self.zoom_scale = 1.0
        self.canvas = tk.Canvas(self.root, width=WIDTH, height=HEIGHT, bg="#16171f", highlightthickness=0)
        self.canvas.pack(expand=True, fill="both")
        self.assets = AssetStore(self.root)
        self.music = MusicManager(MUSIC_DIR)
        self.current_map_id = OVERWORLD_ID
        self.world_seed = current_world_seed()
        self.world_cols, self.world_rows = world_size(self.current_map_id)

        self.player_x, self.player_y = START_POSITION
        self.render_x = float(self.player_x)
        self.render_y = float(self.player_y)
        self.facing = "down"
        self.walk_frame = 0
        self.walk_timer = 0
        self.camera_x = 0
        self.camera_y = 0
        self.camera_view_x = 0.0
        self.camera_view_y = 0.0

        self.mode = "main_menu"
        self.selected_class = "Knight"
        self.player_class_name = "Knight"
        self.pending_player_name = "Arin"
        self.current_save_id = ""
        self.save_list_offset = 0
        self.settings_return_mode = "main_menu"
        self.pause_return_mode = "world"
        self.confirm_return_mode = "main_menu"
        self.confirm_title = ""
        self.confirm_message = ""
        self.confirm_action: Callable[[], None] | None = None
        self.player: Actor | None = None
        self.allies: list[Actor] = []
        self.battle: Battle | None = None
        self.active_dialog_npc: NPC | None = None
        self.active_shop_npc: NPC | None = None
        self.messages = ["Choose New Adventure or load an existing save."]
        self.frame = 0
        self.click_zones: list[ClickZone] = []
        self.hover_zones: list[HoverZone] = []
        self.mouse_x = 0
        self.mouse_y = 0
        self.name_var = tk.StringVar(value=self.pending_player_name)
        self.name_entry = tk.Entry(
            self.root,
            textvariable=self.name_var,
            font=("Segoe UI", 16, "bold"),
            justify="center",
            bg="#111722",
            fg="#f4f9ff",
            insertbackground="#f4d58d",
            relief="flat",
        )
        self.world_map_image_cache: dict[tuple[int, int], tk.PhotoImage] = {}
        self.world_map_paint: dict[tuple[int, int], str] = {}
        self.map_painter_enabled = False
        self.map_paint_color = "1"
        self.world_map_draw_bounds: tuple[int, int, int, int, float] | None = None
        self.surface_positions = CITY_SURFACE_EXITS | VILLAGE_SURFACE_EXITS | DUNGEON_ENTRANCES
        self._terrain_variant_cache: dict[tuple[int, int, str], int] = {}
        self._context_base_cache: dict[tuple[str, int, int, tuple[str, ...], int], str] = {}
        self._mountain_exposure_cache: dict[tuple[str, int, int], bool] = {}
        self._cluster_center_cache: dict[tuple[str, str, int, int], bool] = {}
        self._building_topology_cache: dict[tuple[str, int, int], tuple[bool, bool, bool, bool, bool, bool, bool]] = {}
        self._building_frontage_cache: dict[tuple[str, int, int], tuple[int, int]] = {}
        self._drawn_city_buildings: set[tuple[str, str]] = set()
        self._wall_topology_cache: dict[tuple[str, int, int], tuple[bool, bool, bool, bool]] = {}
        self._path_topology_cache: dict[tuple[str, int, int], tuple[bool, bool, bool, bool]] = {}
        self.render_mode = game_config.RENDER_MODE
        self.battle_render_mode = game_config.BATTLE_RENDER_MODE
        self.tile_assets = TILE_ASSETS
        self.world_renderer = WorldRenderer(self)

        self.center_window()
        self.root.protocol("WM_DELETE_WINDOW", lambda: self.exit_game(False))
        self.root.bind("<KeyPress>", self.on_key)
        self.root.bind("<F11>", self.toggle_fullscreen)
        self.root.bind("<Alt-Return>", self.toggle_fullscreen)
        self.root.bind("<Control-f>", self.toggle_fullscreen)
        self.canvas.bind("<Configure>", self.on_canvas_resize)
        self.canvas.bind("<Button-1>", self.on_click)
        self.canvas.bind("<Button-3>", self.on_right_click)
        self.canvas.bind("<B1-Motion>", self.on_drag)
        self.canvas.bind("<B3-Motion>", self.on_right_drag)
        self.canvas.bind("<Motion>", self.on_mouse_move)
        self.canvas.bind("<Leave>", self.on_mouse_leave)
        self.preload_core_assets()
        self.loop()

    def on_canvas_resize(self, event: tk.Event) -> None:
        self.apply_layout(int(event.width), int(event.height))

    def apply_layout(self, width: int, height: int) -> None:
        global WIDTH, HEIGHT, TILE, MAP_COLS, MAP_ROWS, SIDEBAR_WIDTH, MAP_BOTTOM
        width = max(640, width)
        height = max(480, height)
        scale = max(0.72, min(width / BASE_WIDTH, height / BASE_HEIGHT))
        sidebar_width = max(220, int(BASE_SIDEBAR_WIDTH * scale))
        tile = max(28, min(96, int(BASE_TILE * scale * self.zoom_scale)))
        map_cols = max(8, (width - sidebar_width) // tile)
        map_rows = max(7, height // tile)
        changed = (
            width != WIDTH
            or height != HEIGHT
            or tile != TILE
            or map_cols != MAP_COLS
            or map_rows != MAP_ROWS
            or sidebar_width != SIDEBAR_WIDTH
        )
        WIDTH = width
        HEIGHT = height
        TILE = tile
        MAP_COLS = map_cols
        MAP_ROWS = map_rows
        SIDEBAR_WIDTH = sidebar_width
        MAP_BOTTOM = MAP_ROWS * TILE
        self.canvas_width = width
        self.canvas_height = height
        self.ui_scale = scale
        if changed:
            self.canvas.config(width=width, height=height)
            self.world_renderer.static_key = None
            self.update_camera()

    def adjust_zoom(self, delta: float) -> None:
        self.zoom_scale = max(0.70, min(1.55, self.zoom_scale + delta))
        self.apply_layout(self.canvas_width, self.canvas_height)

    def clear_world_caches(self) -> None:
        self.world_map_image_cache.clear()
        self._terrain_variant_cache.clear()
        self._context_base_cache.clear()
        self._mountain_exposure_cache.clear()
        self._cluster_center_cache.clear()
        self._building_topology_cache.clear()
        self._building_frontage_cache.clear()
        self._drawn_city_buildings.clear()
        self._wall_topology_cache.clear()
        self._path_topology_cache.clear()

    def layout_key(self) -> tuple[int, int, int, int, int]:
        return (WIDTH, HEIGHT, TILE, MAP_COLS, MAP_ROWS)

    def tile_size(self) -> int:
        return TILE

    def map_cols(self) -> int:
        return MAP_COLS

    def map_rows(self) -> int:
        return MAP_ROWS

    def start(self) -> None:
        self.root.mainloop()

    def center_window(self) -> None:
        self.root.update_idletasks()
        screen_w = self.root.winfo_screenwidth()
        screen_h = self.root.winfo_screenheight()
        left = max(0, (screen_w - BASE_WIDTH) // 2)
        top = max(0, (screen_h - BASE_HEIGHT) // 2)
        self.root.geometry(f"{BASE_WIDTH}x{BASE_HEIGHT}+{left}+{top}")
        self.apply_layout(BASE_WIDTH, BASE_HEIGHT)

    def toggle_fullscreen(self, event: tk.Event | None = None) -> str:
        self.fullscreen = not self.fullscreen
        self.root.attributes("-fullscreen", self.fullscreen)
        self.root.configure(bg=WINDOW_BG)
        self.canvas.pack_configure(expand=True, fill="both")
        if not self.fullscreen:
            self.root.state("normal")
            self.center_window()
        else:
            self.root.update_idletasks()
            self.apply_layout(self.root.winfo_width(), self.root.winfo_height())
        return "break"

    def exit_fullscreen(self) -> None:
        if not self.fullscreen:
            return
        self.fullscreen = False
        self.root.attributes("-fullscreen", False)
        self.root.state("normal")
        self.center_window()

    def preload_core_assets(self) -> None:
        for size in (TILE, 144, 168):
            self.assets.get("player", size)
            self.assets.get("player_battle", size)
            self.assets.get("player_model", size)
            for klass in ("knight", "mage", "ranger"):
                self.assets.get(f"class_{klass}_model", size)
        for monster in (
            "slime",
            "wolf",
            "bat",
            "goblin",
            "goblin_scout",
            "goblin_archer",
            "goblin_trapper",
            "goblin_skirmisher",
            "goblin_shaman",
            "hobgoblin_guard",
            "goblin_warlord",
            "goblin_king",
            "thornling",
            "sand_stalker",
            "ice_golem",
            "bog_beast",
            "ember_imp",
        ):
            self.assets.get(monster, 144)
            self.assets.get(monster, 168)
        for tile in ("grass", "forest", "mountain", "mountain_massif_tile", "water", "road", "desert", "tundra", "marsh", "badlands"):
            for variant in range(3):
                self.assets.get(tile, TILE, variant=variant)
        for city_asset in (
            "city_house_front",
            "city_roof_detail",
            "city_wall_block",
            "city_wall_gate",
            "city_lantern",
            "city_overworld_village",
            "city_overworld_village_green",
            "city_overworld_village_snow",
            "city_overworld_village_desert",
            "city_overworld_village_marsh",
            "city_building_town_gabled",
            "city_building_town_manor",
            "city_building_town_narrow",
            "city_building_town_shop",
            "city_building_town_small",
            "city_building_town_tall",
            "city_building_stone_hall",
            "city_building_stone_shop",
            "city_building_stone_tower",
            "city_prop_banner_blue",
            "city_prop_banner_red",
            "city_prop_barrel",
            "city_prop_barrel_stack",
            "city_prop_cart",
            "city_prop_crate",
            "city_prop_flower_pot",
            "city_prop_fountain_small",
            "city_prop_market_green",
            "city_prop_market_red",
            "city_prop_market_yellow",
            "city_prop_plant_box",
            "city_prop_planter_stone",
            "city_prop_street_lamp",
            "city_prop_source_barrel_open",
            "city_prop_source_barrel_tall",
            "city_prop_source_crate_low",
            "city_prop_source_crate_tall",
            "city_prop_source_reeds",
            "city_prop_source_snow_bush",
            "city_prop_source_snow_stump",
            "city_prop_source_stone_pile",
        ):
            self.assets.get_fit(city_asset, TILE, TILE)
        self.assets.get("mountain_massif", TILE * 3)
        for decoration in all_decoration_assets():
            self.assets.get(decoration, int(round(TILE * 0.88)))
        for decoration in FOREST_FLOOR_DECORATIONS:
            self.assets.get(decoration, int(round(TILE * 0.62)))
        for decoration in FOREST_FEATURE_DECORATIONS:
            self.assets.get(decoration, int(round(TILE * 1.04)))
        for decoration in FOREST_TREE_DECORATIONS:
            self.assets.get(decoration, int(round(TILE * 1.40)))

    def loop(self) -> None:
        self.frame += 1
        self.tick_world_motion()
        if self.battle:
            self.battle.tick()
        self.update_music()
        self.draw()
        self.root.after(FPS_MS, self.loop)

    def update_music(self) -> None:
        track = self.desired_music_track()
        if track:
            self.music.play(track)
        elif self.mode not in MUSIC_HOLD_MODES:
            self.music.stop()

    def desired_music_track(self) -> str | None:
        if not self.player:
            return None
        if self.mode == "battle" and self.battle:
            return self.battle_music_track()
        if self.mode in WORLD_MUSIC_MODES:
            return self.world_music_track()
        if self.mode in MUSIC_HOLD_MODES:
            return self.music.current_track
        return None

    def world_music_track(self) -> str:
        if self.current_map_id.startswith(("city_", "village_", "house_")):
            return "town_village"
        if self.current_map_id.startswith("dungeon_"):
            return "dungeon_crypt"
        theme = map_theme(self.current_map_id, self.player_x, self.player_y)
        return MUSIC_BY_THEME.get(theme, "zone_grasslands")

    def battle_music_track(self) -> str:
        assert self.battle
        if len(self.battle.enemy_keys) >= 3 or any(key in BOSS_MUSIC_KEYS for key in self.battle.enemy_keys):
            return "battle_boss"
        return "battle_standard"

    def tick_world_motion(self) -> None:
        if self.mode in {"world", "dialog", "shop", "map", "skills", "inventory"}:
            self.render_x += (self.player_x - self.render_x) * 0.28
            self.render_y += (self.player_y - self.render_y) * 0.28
            if abs(self.player_x - self.render_x) < 0.01:
                self.render_x = float(self.player_x)
            if abs(self.player_y - self.render_y) < 0.01:
                self.render_y = float(self.player_y)
        else:
            self.render_x = float(self.player_x)
            self.render_y = float(self.player_y)
        if self.walk_timer > 0:
            self.walk_timer -= 1
        self.update_camera()

    def update_camera(self) -> None:
        max_x = max(0, self.world_cols - MAP_COLS)
        max_y = max(0, self.world_rows - MAP_ROWS)
        target_x = max(0.0, min(float(max_x), self.render_x - MAP_COLS / 2))
        target_y = max(0.0, min(float(max_y), self.render_y - MAP_ROWS / 2))
        self.camera_view_x += (target_x - self.camera_view_x) * 0.24
        self.camera_view_y += (target_y - self.camera_view_y) * 0.24
        if abs(target_x - self.camera_view_x) < 0.01:
            self.camera_view_x = target_x
        if abs(target_y - self.camera_view_y) < 0.01:
            self.camera_view_y = target_y
        self.camera_x = int(self.camera_view_x)
        self.camera_y = int(self.camera_view_y)

    def on_key(self, event: tk.Event) -> None:
        key = event.keysym
        if key == "F11":
            return
        if self.mode == "main_menu":
            if key == "Return":
                self.open_new_game()
            elif key in {"l", "L"} and save_exists():
                self.open_save_list()
            elif key in {"s", "S"}:
                self.open_settings("main_menu")
            return
        if self.mode == "confirm_action":
            if key in {"Escape", "n", "N"}:
                self.cancel_confirmation()
            elif key in {"Return", "y", "Y"}:
                self.accept_confirmation()
            return
        if self.mode == "new_game":
            names = list(CLASSES)
            if key in {"1", "2", "3"}:
                self.selected_class = names[int(key) - 1]
            if key == "Return":
                self.start_selected_class()
            elif key in {"l", "L"}:
                self.open_save_list()
            elif key == "Escape":
                self.open_main_menu()
            return
        if self.mode == "save_list":
            saves = list_save_summaries()
            max_offset = max(0, len(saves) - 5)
            if key in {"Escape", "BackSpace"}:
                self.open_main_menu()
            elif key in {"Up", "w", "W"}:
                self.save_list_offset = max(0, self.save_list_offset - 1)
            elif key in {"Down", "s", "S"}:
                self.save_list_offset = min(max_offset, self.save_list_offset + 1)
            elif key in {"1", "2", "3", "4", "5"}:
                idx = self.save_list_offset + int(key) - 1
                if idx < len(saves):
                    self.load_selected_save(saves[idx].save_id)
            return
        if self.mode == "pause_menu":
            if key == "Escape":
                self.resume_game()
            elif key in {"m", "M"}:
                self.return_to_main_menu()
            elif key in {"s", "S"}:
                self.open_settings("pause_menu")
            return
        if self.mode == "settings":
            if key == "Escape":
                self.close_settings()
            return
        if key == "F5":
            self.save_current_game()
            return
        if key == "F9":
            self.load_current_game()
            return
        if self.mode == "dialog":
            if key == "Escape":
                self.open_pause_menu()
            else:
                self.mode = "world"
            return
        if self.mode == "map":
            if key in MAP_PAINT_COLORS:
                self.map_paint_color = key
                self.map_painter_enabled = True
                self.messages.insert(0, f"Map painter color set to {MAP_PAINT_LABELS[key]}.")
                return
            if key in {"p", "P"}:
                self.map_painter_enabled = not self.map_painter_enabled
                state = "enabled" if self.map_painter_enabled else "disabled"
                self.messages.insert(0, f"Map painter {state}.")
                return
            if key in {"e", "E"}:
                self.map_painter_enabled = True
                self.map_paint_color = "8"
                self.messages.insert(0, "Map painter set to chalk.")
                return
            if key in {"BackSpace", "Delete"}:
                self.clear_world_map_paint()
                self.messages.insert(0, "Cleared world map paint layer.")
                return
            if key in {"Escape", "Return", "m", "M"}:
                self.close_world_map()
            return
        if self.mode == "skills":
            if key in {"Escape", "Return", "k", "K"}:
                self.close_skill_tree()
            return
        if self.mode == "inventory":
            if key in {"Escape", "Return", "i", "I"}:
                self.close_inventory()
            return
        if self.mode == "shop":
            if key in {"Escape", "Return"}:
                self.close_shop()
            return
        if self.mode == "world":
            if key in {"Up", "w", "W"}:
                self.try_move(0, -1)
            elif key in {"Down", "s", "S"}:
                self.try_move(0, 1)
            elif key in {"Left", "a", "A"}:
                self.try_move(-1, 0)
            elif key in {"Right", "d", "D"}:
                self.try_move(1, 0)
            elif key in {"q", "Q"}:
                self.show_quests()
            elif key in {"m", "M"}:
                self.open_world_map()
            elif key in {"k", "K"}:
                self.open_skill_tree()
            elif key in {"i", "I"}:
                self.open_inventory()
            elif key == "Escape":
                self.open_pause_menu()
            return
        if self.mode == "battle" and self.battle:
            if key == "Escape":
                self.open_pause_menu()
                return
            if self.battle.finished and key == "Return":
                self.resolve_battle_end()
                return
            if key in {"Left", "a", "A"}:
                self.battle.cycle_target(-1)
            elif key in {"Right", "Tab"}:
                self.battle.cycle_target(1)
            if key == "space":
                self.battle.player_attack()
            elif key in {"d", "D"}:
                self.battle.player_defend()
            elif key in {"1", "2", "3", "4", "5", "6"}:
                self.battle.use_ability(int(key) - 1)
            elif key == "7":
                self.use_battle_item("potion_small")
            elif key == "8":
                self.use_battle_item("ether")

    def on_click(self, event: tk.Event) -> None:
        x, y = int(event.x), int(event.y)
        for zone in reversed(self.click_zones):
            if zone.hit(x, y):
                zone.action()
                return
        if self.mode == "map" and self.map_painter_enabled and self.paint_world_map_at_screen(x, y, erase=False):
            return
        if self.mode == "world" and x < MAP_COLS * TILE and y < MAP_BOTTOM:
            world_x = int(self.camera_view_x + x / TILE)
            world_y = int(self.camera_view_y + y / TILE)
            self.move_toward_tile(world_x, world_y)
        elif self.mode == "dialog":
            self.mode = "world"

    def on_right_click(self, event: tk.Event) -> None:
        if self.mode != "map":
            return
        self.paint_world_map_at_screen(int(event.x), int(event.y), erase=True)

    def on_drag(self, event: tk.Event) -> None:
        if self.mode == "map" and self.map_painter_enabled:
            self.paint_world_map_at_screen(int(event.x), int(event.y), erase=False)

    def on_right_drag(self, event: tk.Event) -> None:
        if self.mode == "map":
            self.paint_world_map_at_screen(int(event.x), int(event.y), erase=True)

    def on_mouse_move(self, event: tk.Event) -> None:
        self.mouse_x = int(event.x)
        self.mouse_y = int(event.y)

    def on_mouse_leave(self, event: tk.Event) -> None:
        self.mouse_x = -1
        self.mouse_y = -1

    def clean_character_name(self) -> str:
        name = " ".join(self.name_var.get().strip().split())
        return name[:24] or "Arin"

    def open_main_menu(self) -> None:
        self.mode = "main_menu"
        self.messages = ["Choose New Adventure or load an existing save."]

    def open_new_game(self) -> None:
        self.mode = "new_game"
        self.name_var.set(self.pending_player_name)
        self.messages = ["Name your character, choose a class, then begin."]

    def open_save_list(self) -> None:
        self.mode = "save_list"
        self.save_list_offset = 0
        self.messages = ["Choose a save file to load."]

    def load_selected_save(self, save_id: str) -> None:
        self.current_save_id = save_id
        self.load_current_game()

    def open_pause_menu(self) -> None:
        if self.mode in {"pause_menu", "settings", "main_menu", "new_game", "save_list"}:
            return
        self.pause_return_mode = self.mode
        self.mode = "pause_menu"

    def resume_game(self) -> None:
        self.mode = self.pause_return_mode if self.pause_return_mode != "pause_menu" else "world"

    def return_to_main_menu(self) -> None:
        self.battle = None
        self.active_dialog_npc = None
        self.active_shop_npc = None
        self.music.stop()
        self.open_main_menu()

    def open_settings(self, return_mode: str | None = None) -> None:
        if return_mode:
            self.settings_return_mode = return_mode
        else:
            self.settings_return_mode = self.mode
        self.mode = "settings"

    def close_settings(self) -> None:
        self.mode = self.settings_return_mode if self.settings_return_mode != "settings" else "main_menu"

    def open_confirmation(self, title: str, message: str, action: Callable[[], None], return_mode: str) -> None:
        self.confirm_title = title
        self.confirm_message = message
        self.confirm_action = action
        self.confirm_return_mode = return_mode
        self.mode = "confirm_action"

    def cancel_confirmation(self) -> None:
        self.confirm_action = None
        self.mode = self.confirm_return_mode if self.confirm_return_mode != "confirm_action" else "main_menu"

    def accept_confirmation(self) -> None:
        action = self.confirm_action
        self.confirm_action = None
        if action:
            action()

    def exit_game(self, require_confirmation: bool = False) -> None:
        if require_confirmation:
            self.open_confirmation("Exit Game", "Unsaved progress will be lost. Exit now?", lambda: self.exit_game(False), "pause_menu")
            return
        self.music.shutdown()
        self.root.destroy()

    def restart_game(self, require_confirmation: bool = False) -> None:
        if require_confirmation:
            self.open_confirmation("Restart Game", "Unsaved progress will be lost. Restart now?", lambda: self.restart_game(False), "pause_menu")
            return
        self.music.shutdown()
        self.root.destroy()
        os.execl(sys.executable, sys.executable, *sys.argv)

    def adjust_chance(self, section: str, key: str, delta: float) -> None:
        source = game_config.MONSTER_SPAWN_CHANCES if section == "monster_spawn_chances" else game_config.MONSTER_GROUP_CHANCES
        game_config.set_chance(section, key, source[key] + delta)
        self.messages.insert(0, "Settings saved.")

    def set_render_mode(self, mode: str) -> None:
        game_config.set_graphics(render_mode=mode)
        self.render_mode = game_config.RENDER_MODE
        self.world_renderer.static_key = None
        self.messages.insert(0, "Graphics settings saved.")

    def set_battle_render_mode(self, mode: str) -> None:
        game_config.set_graphics(battle_render_mode=mode)
        self.battle_render_mode = game_config.BATTLE_RENDER_MODE
        self.messages.insert(0, "Battle graphics settings saved.")

    def start_selected_class(self) -> None:
        self.pending_player_name = self.clean_character_name()
        self.current_save_id = unique_save_id(self.pending_player_name)
        reset_quest_state()
        self.world_seed = randrange(1, 2**31)
        set_world_seed(self.world_seed)
        self.clear_world_map_paint()
        self.clear_world_caches()
        klass = CLASSES[self.selected_class]
        self.player = Actor(
            self.pending_player_name,
            "player",
            klass.name,
            klass.max_hp,
            klass.max_hp,
            klass.max_mp,
            klass.max_mp,
            klass.attack,
            klass.defense,
        )
        self.player.abilities = [Ability(a.name, a.power, a.cost, a.kind, a.target) for a in klass.abilities]
        self.player.gold = 45
        self.player.skill_points = 0
        self.player.add_item("potion_small", 2)
        self.player.add_item("ether", 1)
        self.player.add_item("rusty_sword", 1)
        self.player.equip_item("rusty_sword")
        self.allies = []
        self.player_class_name = klass.name
        self.current_map_id = OVERWORLD_ID
        self.world_cols, self.world_rows = world_size(self.current_map_id)
        self.player_x, self.player_y = START_POSITION
        self.facing = "down"
        self.walk_frame = 0
        self.walk_timer = 0
        self.mode = "world"
        self.messages = [
            f"{self.player.name} begins as a {klass.name}. World seed {self.world_seed}.",
            self.story_summary(),
            "Seek named locals in settlements; some join after a quest, others work by contract.",
        ]
        self.render_x = float(self.player_x)
        self.render_y = float(self.player_y)
        self.update_camera()
        self.save_current_game()

    def save_current_game(self) -> None:
        try:
            path = save_game(self)
        except Exception as exc:
            self.messages.insert(0, f"Save failed: {exc}")
            return
        self.messages.insert(0, f"Game saved to {path.name}.")

    def load_current_game(self) -> None:
        try:
            path = load_game(self, self.current_save_id or None)
        except Exception as exc:
            if self.player:
                self.messages.insert(0, f"Load failed: {exc}")
            else:
                self.messages = [f"Load failed: {exc}", "Choose New Adventure or load an existing save."]
            return
        if self.player:
            self.sync_skill_abilities()
            self.messages.insert(0, f"Loaded {self.player.name} from {path.name}.")

    def move_toward_tile(self, target_x: int, target_y: int) -> None:
        dx = target_x - self.player_x
        dy = target_y - self.player_y
        if dx == 0 and dy == 0:
            return
        if abs(dx) > abs(dy):
            self.try_move(1 if dx > 0 else -1, 0)
        else:
            self.try_move(0, 1 if dy > 0 else -1)

    def try_move(self, dx: int, dy: int) -> None:
        if self.mode != "world":
            return
        nx, ny = self.player_x + dx, self.player_y + dy
        if not is_passable(self.current_map_id, nx, ny):
            if self.current_map_id.startswith(("city_", "village_")) and tile_at(self.current_map_id, nx, ny) == "h":
                if city_building_entry_at(self.current_map_id, nx, ny, self.player_x, self.player_y):
                    self.enter_city_building(nx, ny, self.player_x, self.player_y)
                    return
                if dy < 0 and self.player_x == nx and self.player_y == ny + 1 and self.is_south_building_frontage(nx, ny):
                    self.enter_city_building(nx, ny, self.player_x, self.player_y)
                    return
                self.messages.insert(0, "Find the doorway.")
                return
            self.messages.insert(0, "The terrain blocks your path.")
            return
        if dx < 0:
            self.facing = "left"
        elif dx > 0:
            self.facing = "right"
        elif dy < 0:
            self.facing = "up"
        elif dy > 0:
            self.facing = "down"
        self.walk_frame = (self.walk_frame + 1) % 4
        self.walk_timer = 10
        self.player_x, self.player_y = nx, ny
        self.resolve_location()

    def enter_city_building(self, wx: int, wy: int, return_x: int, return_y: int) -> None:
        interior_id = ensure_house_interior(self.current_map_id, wx, wy, return_x, return_y)
        self.set_map_position(interior_id, *HOUSE_SPAWN)
        self.facing = "up"
        self.messages.insert(0, "You step inside.")

    def set_map_position(self, map_id: str, x: int, y: int) -> None:
        self.current_map_id = map_id
        self.world_cols, self.world_rows = world_size(map_id)
        x, y = self.safe_spawn_position(map_id, x, y)
        self.player_x = x
        self.player_y = y
        self.render_x = float(x)
        self.render_y = float(y)
        self.camera_view_x = float(max(0, min(max(0, self.world_cols - MAP_COLS), int(x - MAP_COLS / 2))))
        self.camera_view_y = float(max(0, min(max(0, self.world_rows - MAP_ROWS), int(y - MAP_ROWS / 2))))
        self.update_camera()

    def safe_spawn_position(self, map_id: str, x: int, y: int) -> tuple[int, int]:
        def usable(px: int, py: int) -> bool:
            if not is_passable(map_id, px, py):
                return False
            for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
                if is_passable(map_id, px + dx, py + dy):
                    return True
            return False

        if usable(x, y):
            return x, y
        for radius in range(1, 7):
            for py in range(y - radius, y + radius + 1):
                for px in range(x - radius, x + radius + 1):
                    if abs(px - x) + abs(py - y) <= radius and usable(px, py):
                        return px, py
        return x, y

    def check_class_unlocks(self) -> list[str]:
        if not self.player:
            return []
        unlocks = CLASS_UNLOCKS.get(self.player.class_name, {})
        learned: list[str] = []
        for level in sorted(unlocks):
            ability = unlocks[level]
            if self.player.level >= level and not self.player.has_ability(ability.name):
                self.player.abilities.append(Ability(ability.name, ability.power, ability.cost, ability.kind, ability.target))
                learned.append(ability.name)
        for level, (perk_key, text) in sorted(CLASS_PERKS.get(self.player.class_name, {}).items()):
            if self.player.level >= level and not self.player.has_perk(perk_key):
                self.player.perks.append(perk_key)
                learned.append(text)
        self.sync_skill_abilities()
        return learned

    def story_summary(self) -> str:
        completed = sum(1 for quest in QUESTS.values() if quest.completed)
        if completed >= 5:
            return STORY_BEATS[2]
        if completed >= 2:
            return STORY_BEATS[1]
        return STORY_BEATS[0]

    def is_recruited(self, recruit_id: str | None) -> bool:
        if not recruit_id or recruit_id not in RECRUITS:
            return False
        class_name = str(RECRUITS[recruit_id]["class_name"])
        return any(ally.class_name == class_name for ally in self.allies)

    def recruit_ally(self, recruit_id: str | None) -> str | None:
        if not recruit_id or recruit_id not in RECRUITS:
            return None
        if self.is_recruited(recruit_id):
            return None
        data = RECRUITS[recruit_id]
        ally = Actor(
            data["name"],
            data["sprite"],
            data["class_name"],
            int(data["max_hp"]),
            int(data["max_hp"]),
            int(data["max_mp"]),
            int(data["max_mp"]),
            int(data["attack"]),
            int(data["defense"]),
        )
        ally.abilities = [Ability(a.name, a.power, a.cost, a.kind, a.target) for a in data["abilities"]]
        ally.inventory = dict(data.get("inventory", {}))
        self.allies.append(ally)
        return f"{ally.name} joins your party."

    def hire_recruit(self, npc: NPC | None = None) -> None:
        if not self.player:
            return
        npc = npc or self.active_dialog_npc or self.active_shop_npc
        if not npc or not npc.recruit_id or npc.recruit_cost <= 0:
            return
        if self.is_recruited(npc.recruit_id):
            self.messages.insert(0, f"{npc.name} already travels with you.")
            return
        if self.player.gold < npc.recruit_cost:
            self.messages.insert(0, f"{npc.name}'s contract costs {npc.recruit_cost} gold.")
            return
        self.player.gold -= npc.recruit_cost
        recruit_note = self.recruit_ally(npc.recruit_id)
        if recruit_note:
            self.messages.insert(0, f"Paid {npc.recruit_cost} gold for {npc.name}'s contract.")
            self.messages.insert(0, recruit_note)
        else:
            self.player.gold += npc.recruit_cost
            self.messages.insert(0, f"{npc.name} cannot join right now.")

    def npc_quest_updates(self, npc: NPC) -> list[str]:
        if not self.player or not npc.quest_id:
            return []
        quest = QUESTS[npc.quest_id]
        updates: list[str] = []
        if quest.ready():
            quest.completed = True
            self.player.gold += quest.reward_gold
            level_notes = self.player.gain_xp(quest.reward_xp)
            updates.append(f"Quest complete: {quest.title}.")
            recruit_note = self.recruit_ally(npc.recruit_id)
            if recruit_note:
                updates.append(recruit_note)
            for note in level_notes:
                updates.append(note)
            for name in self.check_class_unlocks():
                updates.append(f"New ability learned: {name}.")
        elif not quest.accepted:
            quest.accepted = True
            updates.append(f"Quest accepted: {quest.title}.")
            updates.append(quest.description)
        else:
            updates.append(f"{quest.title}: {quest.progress}/{quest.needed}")
        return updates

    def resolve_location(self) -> None:
        if not self.player:
            return
        transition = transition_at(self.current_map_id, self.player_x, self.player_y)
        if transition:
            self.set_map_position(transition.target_map_id, transition.target_x, transition.target_y)
            self.messages.insert(0, transition.message)
            if self.current_map_id.startswith(("city_", "village_")):
                self.player.heal_full()
                self.messages.insert(0, "You recover near the settlement gate.")
            return

        event = roll_event(self.current_map_id, self.player_x, self.player_y, self.player.level)
        if not event:
            self.messages.insert(0, f"You travel through {describe_tile(self.current_map_id, self.player_x, self.player_y)}.")
            return
        self.messages.insert(0, event.message)
        if event.kind in {"city", "village"}:
            self.player.heal_full()
            self.messages.insert(0, "You feel rested and focused.")
        elif event.kind == "npc" and event.npc:
            updates = self.npc_quest_updates(event.npc)
            if event.npc.shop_id:
                self.open_shop(event.npc, updates)
            else:
                self.mode = "dialog"
                self.active_dialog_npc = event.npc
                self.messages = list(reversed(event.npc.dialog))
                for line in reversed(updates):
                    self.messages.insert(0, line)
        elif event.kind == "battle" and event.monster_key:
            self.start_battle(event.monster_keys or event.monster_key, map_theme(self.current_map_id, self.player_x, self.player_y))

    def open_shop(self, npc: NPC, updates: list[str] | None = None) -> None:
        self.mode = "shop"
        self.active_dialog_npc = npc
        self.active_shop_npc = npc
        lines = list(npc.dialog)
        if updates:
            lines = updates + lines
        if npc.recruit_id and npc.recruit_cost > 0 and not self.is_recruited(npc.recruit_id):
            lines.append(f"{npc.name}'s companion contract: {npc.recruit_cost} gold.")
        lines.append("Browse stock and click an item to buy.")
        self.messages = lines

    def close_shop(self) -> None:
        self.mode = "world"
        self.active_dialog_npc = None
        self.active_shop_npc = None
        self.messages.insert(0, "You leave the shop.")

    def buy_item(self, item_key: str) -> None:
        if not self.player or not self.active_shop_npc:
            return
        cost = item_cost(item_key)
        if self.player.gold < cost:
            self.messages.insert(0, f"Not enough gold for {item_name(item_key)}.")
            return
        self.player.gold -= cost
        self.player.add_item(item_key, 1)
        self.messages.insert(0, f"Bought {item_name(item_key)} for {cost} gold.")

    def use_battle_item(self, item_key: str) -> None:
        if not self.player or not self.battle or self.battle.finished:
            return
        if not self.battle.can_player_act():
            return
        active_actor = self.battle.active_actor()
        item_owner = self.player if self.player.consume_item(item_key) else None
        if item_owner is None and active_actor is not None and active_actor is not self.player and active_actor.consume_item(item_key):
            item_owner = active_actor
        if item_owner is None:
            self.battle.log.insert(0, f"No {ITEMS[item_key]['name']} left.")
            return
        item = ITEMS[item_key]
        if not self.battle.player_use_item(item["name"], heal=int(item["heal"]), mp=int(item["mp"])):
            item_owner.add_item(item_key, 1)

    def start_battle(self, monster_key: str | list[str] | None = None, terrain: str | None = None) -> None:
        if not self.player:
            return
        fallback_pool = ["slime", "wolf", "goblin", "spider", "shadow_bat"]
        self.battle = Battle(
            self.player,
            monster_key or fallback_pool[self.frame % len(fallback_pool)],
            terrain or map_theme(self.current_map_id, self.player_x, self.player_y),
            self.allies,
        )
        self.mode = "battle"

    def resolve_battle_end(self) -> None:
        assert self.battle and self.player
        if self.battle.victory:
            defeated_names = [enemy.name for enemy in self.battle.enemies]
            for quest in QUESTS.values():
                for name in defeated_names:
                    quest.record(name)
            for name in self.check_class_unlocks():
                self.messages.insert(0, f"New ability learned: {name}.")
            self.messages.insert(0, "You return to the road.")
        else:
            self.player.heal_full()
            for ally in self.allies:
                ally.heal_full()
            self.set_map_position(OVERWORLD_ID, *START_POSITION)
            self.messages = ["You wake at the city inn, bruised but alive."]
        self.battle = None
        self.mode = "world"

    def show_quests(self) -> None:
        rows = [f"Story: {self.story_summary()}"]
        for quest in QUESTS.values():
            if quest.accepted and not quest.completed:
                rows.append(f"{quest.title}: {quest.progress}/{quest.needed}")
            elif quest.completed:
                rows.append(f"{quest.title}: complete")
        if len(rows) == 1:
            rows.append("No active quests. Visit city NPCs.")
        self.messages = rows

    def open_world_map(self) -> None:
        if self.mode == "world":
            self.world_map_draw_bounds = None
            self.mode = "map"

    def close_world_map(self) -> None:
        self.world_map_draw_bounds = None
        self.mode = "world"

    def toggle_world_map_painter(self) -> None:
        self.map_painter_enabled = not self.map_painter_enabled

    def select_world_map_paint_color(self, color_key: str) -> None:
        if color_key not in MAP_PAINT_COLORS:
            return
        self.map_paint_color = color_key
        self.map_painter_enabled = True

    def open_skill_tree(self) -> None:
        if self.mode == "world":
            self.mode = "skills"

    def close_skill_tree(self) -> None:
        self.mode = "world"

    def open_inventory(self) -> None:
        if self.mode == "world":
            self.mode = "inventory"

    def close_inventory(self) -> None:
        self.mode = "world"

    def equip_inventory_item(self, item_key: str) -> None:
        if not self.player:
            return
        ok, message = self.player.equip_item(item_key)
        self.messages.insert(0, message)
        if ok:
            self.mode = "inventory"

    def unequip_inventory_slot(self, slot: str) -> None:
        if not self.player:
            return
        _ok, message = self.player.unequip_slot(slot)
        self.messages.insert(0, message)
        self.mode = "inventory"

    def can_allocate_skill(self, skill_key: str) -> tuple[bool, str]:
        if not self.player:
            return False, "No player."
        available_tree = available_skill_tree(self.player.class_name)
        if skill_key not in available_tree:
            return False, "That skill belongs to a different class."
        node = available_tree[skill_key]
        if self.player.skill_points <= 0:
            return False, "No skill points available."
        if self.player.skill_rank(skill_key) >= node.max_rank:
            return False, f"{node.name} is already mastered."
        missing = [
            available_tree[required].name
            for required in node.requires
            if required in available_tree and self.player.skill_rank(required) <= 0
        ]
        if missing:
            return False, f"Requires {', '.join(missing)}."
        return True, ""

    def allocate_skill(self, skill_key: str) -> None:
        if not self.player:
            return
        ok, reason = self.can_allocate_skill(skill_key)
        node = SKILL_TREE[skill_key]
        if not ok:
            self.messages.insert(0, reason)
            return
        self.player.skill_points -= 1
        self.player.skill_allocations[skill_key] = self.player.skill_rank(skill_key) + 1
        self.apply_skill_effects(node, 1)
        self.sync_skill_abilities()
        self.messages.insert(0, f"Skill learned: {node.name}.")

    def apply_skill_effects(self, node: SkillNode, direction: int) -> None:
        assert self.player
        hp_ratio = self.player.hp / max(1, self.player.max_hp)
        mp_ratio = self.player.mp / max(1, self.player.max_mp)
        for stat, amount in node.effects.items():
            delta = int(amount) * direction
            if stat == "max_hp":
                self.player.max_hp = max(1, self.player.max_hp + delta)
            elif stat == "max_mp":
                self.player.max_mp = max(0, self.player.max_mp + delta)
            elif stat == "attack":
                self.player.attack = max(1, self.player.attack + delta)
            elif stat == "defense":
                self.player.defense = max(0, self.player.defense + delta)
        self.player.hp = max(1, min(self.player.max_hp, int(round(self.player.max_hp * hp_ratio))))
        self.player.mp = max(0, min(self.player.max_mp, int(round(self.player.max_mp * mp_ratio))))

    def sync_skill_abilities(self) -> None:
        if not self.player:
            return
        self.player.abilities = [
            ability
            for ability in self.player.abilities
            if ability.name not in SKILL_ABILITY_NAMES
        ]
        available_tree = available_skill_tree(self.player.class_name)
        for skill_key in skill_order_for_class(self.player.class_name):
            node = available_tree[skill_key]
            if node.ability and self.player.skill_rank(skill_key) > 0 and not self.player.has_ability(node.ability.name):
                ability = node.ability
                self.player.abilities.append(Ability(ability.name, ability.power, ability.cost, ability.kind, ability.target))

    def respec_skills(self) -> None:
        if not self.player:
            return
        spent = skill_spent(self.player.skill_allocations)
        if spent <= 0:
            self.messages.insert(0, "No allocated skills to respec.")
            return
        cost = skill_respec_cost(self.player.level, spent)
        if self.player.gold < cost:
            self.messages.insert(0, f"Respec costs {cost} gold.")
            return
        for skill_key, rank in list(self.player.skill_allocations.items()):
            node = SKILL_TREE.get(skill_key)
            if node:
                for _ in range(rank):
                    self.apply_skill_effects(node, -1)
        self.player.gold -= cost
        self.player.skill_points += spent
        self.player.skill_allocations.clear()
        self.sync_skill_abilities()
        self.messages.insert(0, f"Skills reset for {cost} gold.")

    def npc_marker(self, npc: NPC) -> str | None:
        if not npc.quest_id:
            if npc.recruit_id and npc.recruit_cost > 0 and not self.is_recruited(npc.recruit_id):
                return "icon_shield"
            return "icon_chest" if npc.shop_id else None
        quest = QUESTS[npc.quest_id]
        if quest.ready():
            return "icon_chest"
        if not quest.accepted:
            return "icon_potion_blue"
        if npc.shop_id:
            return "icon_shield"
        return "icon_potion_green"

    def add_button(
        self,
        x1: int,
        y1: int,
        x2: int,
        y2: int,
        label: str,
        action: Callable[[], None],
        *,
        fill: str = "#2c3446",
        outline: str = "#5f6b84",
        text: str = "#f2f6ff",
    ) -> None:
        self.canvas.create_rectangle(x1, y1, x2, y2, fill=fill, outline=outline, width=max(1, int(round(2 * self.ui_scale))))
        self.canvas.create_text(
            (x1 + x2) // 2,
            (y1 + y2) // 2,
            text=label,
            fill=text,
            font=("Segoe UI", max(7, int(round(10 * self.ui_scale))), "bold"),
        )
        self.click_zones.append(ClickZone(x1, y1, x2, y2, action))

    def add_hover_zone(self, x1: int, y1: int, x2: int, y2: int, title: str, body: str) -> None:
        self.hover_zones.append(HoverZone(x1, y1, x2, y2, title, body))

    def _active_hover_zone(self) -> HoverZone | None:
        if self.mouse_x < 0 or self.mouse_y < 0:
            return None
        for zone in reversed(self.hover_zones):
            if zone.hit(self.mouse_x, self.mouse_y):
                return zone
        return None

    def draw_hover_tooltip(self) -> None:
        zone = self._active_hover_zone()
        if not zone:
            return
        title = zone.title.strip()
        body = zone.body.strip()
        text = title if not body else f"{title}\n{body}"
        lines = text.splitlines() or [""]
        line_h = max(13, int(14 * self.ui_scale))
        max_chars = max(len(line) for line in lines)
        width = max(180, min(420, 18 + max_chars * max(6, int(7 * self.ui_scale))))
        height = 14 + line_h * len(lines)
        x = min(WIDTH - width - 12, self.mouse_x + 18)
        y = min(HEIGHT - height - 12, self.mouse_y + 18)
        if x < 12:
            x = 12
        if y < 12:
            y = 12
        self.canvas.create_rectangle(x, y, x + width, y + height, fill="#111722", outline="#7a8cae", width=2)
        self.canvas.create_text(
            x + 8,
            y + 8,
            anchor="nw",
            width=width - 16,
            text=text,
            fill="#e8eef9",
            font=("Segoe UI", max(8, int(round(9 * self.ui_scale)))),
        )

    def ability_status_tooltip(self, ability: Ability) -> str:
        hints = status_hints_for_ability(ability.name, ability.kind)
        if not hints:
            return "No additional status effect."
        lines: list[str] = []
        for hint in hints:
            status = STATUS_EFFECTS.get(hint.key)
            if not status:
                continue
            who = "enemy"
            if hint.target == "self":
                who = "you"
            elif hint.target == "target":
                who = "target"
            chance = "" if hint.chance >= 1.0 else f" ({int(hint.chance * 100)}% chance)"
            lines.append(f"- {status.name} on {who}{chance}: {status.description}.")
        return "\n".join(lines) if lines else "No additional status effect."

    def skill_node_tooltip(self, node: SkillNode) -> str:
        lines = [node.description]
        if node.effects:
            stat_names = {"max_hp": "Max HP", "max_mp": "Max MP", "attack": "Attack", "defense": "Defense"}
            bits = []
            for key, amount in node.effects.items():
                sign = "+" if amount >= 0 else ""
                bits.append(f"{sign}{amount} {stat_names.get(key, key)}")
            lines.append("Per rank: " + ", ".join(bits))
        if node.requires:
            names = ", ".join(SKILL_TREE[required].name for required in node.requires if required in SKILL_TREE)
            if names:
                lines.append(f"Requires: {names}")
        if node.ability:
            lines.append(f"Unlocks ability: {node.ability.name} ({node.ability.cost} MP)")
            lines.append(self.ability_status_tooltip(node.ability))
        return "\n".join(lines)

    def status_stack_tooltip(self, stack: EffectStack) -> str:
        base = stack.effect.description
        power_note = ""
        if stack.power > 0:
            if stack.effect.key in {"poison", "burn"}:
                power_note = f" Power: {stack.power} per turn."
            elif stack.effect.key in {"regeneration", "shield"}:
                power_note = f" Power: {stack.power}."
        return f"{base}. {stack.turns_remaining} turn(s) left.{power_note}"

    def map_tile_color(self, tile: str) -> str:
        return MAP_TILE_COLORS.get(tile, "#6b6f63")

    def clear_world_map_paint(self) -> None:
        if not self.world_map_paint:
            return
        self.world_map_paint.clear()
        self.world_map_image_cache.clear()

    def export_world_map_paint(self) -> dict[str, str]:
        encoded: dict[str, str] = {}
        for (wx, wy), color_key in self.world_map_paint.items():
            if color_key not in MAP_PAINT_COLORS:
                continue
            encoded[f"{wx},{wy}"] = color_key
        return encoded

    def import_world_map_paint(self, raw: object) -> None:
        self.world_map_paint.clear()
        if not isinstance(raw, dict):
            self.world_map_image_cache.clear()
            return
        rows = MAPS[OVERWORLD_ID]
        cols = len(rows[0])
        map_h = len(rows)
        for key, value in raw.items():
            if not isinstance(key, str) or not isinstance(value, str):
                continue
            if value not in MAP_PAINT_COLORS:
                continue
            parts = key.split(",", 1)
            if len(parts) != 2:
                continue
            try:
                wx = int(parts[0])
                wy = int(parts[1])
            except ValueError:
                continue
            if 0 <= wx < cols and 0 <= wy < map_h:
                self.world_map_paint[(wx, wy)] = value
        self.world_map_image_cache.clear()

    def paint_world_map_at_screen(self, x: int, y: int, *, erase: bool) -> bool:
        bounds = self.world_map_draw_bounds
        if bounds is None:
            return False
        ox, oy, draw_w, draw_h, scale = bounds
        if x < ox or y < oy or x >= ox + draw_w or y >= oy + draw_h:
            return False
        wx = int((x - ox) / max(0.0001, scale))
        wy = int((y - oy) / max(0.0001, scale))
        rows = MAPS[OVERWORLD_ID]
        cols = len(rows[0])
        map_h = len(rows)
        if not (0 <= wx < cols and 0 <= wy < map_h):
            return False
        key = (wx, wy)
        changed = False
        if erase:
            changed = self.world_map_paint.pop(key, None) is not None
        else:
            color_key = self.map_paint_color if self.map_paint_color in MAP_PAINT_COLORS else "1"
            changed = self.world_map_paint.get(key) != color_key
            self.world_map_paint[key] = color_key
        if changed:
            self.world_map_image_cache.clear()
        return changed

    def get_world_map_image(self, width: int, height: int) -> tk.PhotoImage:
        key = (width, height)
        if key in self.world_map_image_cache:
            return self.world_map_image_cache[key]
        rows = MAPS[OVERWORLD_ID]
        cols = len(rows[0])
        map_h = len(rows)
        image = tk.PhotoImage(width=width, height=height)
        for wy, row in enumerate(rows):
            for wx, tile in enumerate(row):
                x1 = int(wx * width / cols)
                y1 = int(wy * height / map_h)
                x2 = max(x1 + 1, int((wx + 1) * width / cols))
                y2 = max(y1 + 1, int((wy + 1) * height / map_h))
                image.put(self.map_tile_color(tile), to=(x1, y1, x2, y2))
        for (wx, wy), color_key in self.world_map_paint.items():
            if color_key not in MAP_PAINT_COLORS:
                continue
            if not (0 <= wx < cols and 0 <= wy < map_h):
                continue
            x1 = int(wx * width / cols)
            y1 = int(wy * height / map_h)
            x2 = max(x1 + 1, int((wx + 1) * width / cols))
            y2 = max(y1 + 1, int((wy + 1) * height / map_h))
            image.put(MAP_PAINT_COLORS[color_key], to=(x1, y1, x2, y2))
        self.world_map_image_cache[key] = image
        return image

    def draw(self) -> None:
        self.click_zones.clear()
        self.hover_zones.clear()
        self.update_name_entry_visibility()
        if self.mode == "main_menu":
            self.canvas.delete("all")
            self.world_renderer.static_key = None
            self.draw_main_menu()
        elif self.mode == "new_game":
            self.canvas.delete("all")
            self.world_renderer.static_key = None
            self.draw_new_game()
        elif self.mode == "save_list":
            self.canvas.delete("all")
            self.world_renderer.static_key = None
            self.draw_save_list()
        elif self.mode == "pause_menu":
            self.draw_pause_base()
            self.draw_tagged_overlay(self.draw_pause_menu)
        elif self.mode == "settings":
            if self.settings_return_mode in {"pause_menu", "world", "dialog", "shop", "map", "skills", "inventory", "battle"} and self.player:
                self.draw_pause_base()
                self.draw_tagged_overlay(self.draw_settings_overlay)
            else:
                self.canvas.delete("all")
                self.world_renderer.static_key = None
                self.draw_settings_screen()
        elif self.mode == "confirm_action":
            if self.confirm_return_mode == "pause_menu" and self.player:
                self.draw_pause_base()
                self.draw_tagged_overlay(self.draw_confirmation_overlay)
            else:
                self.canvas.delete("all")
                self.world_renderer.static_key = None
                self.draw_title_background()
                self.draw_confirmation_overlay()
        elif self.mode == "battle" and self.battle:
            self.canvas.delete("all")
            self.world_renderer.static_key = None
            try:
                self.draw_battle()
                if not self.canvas.find_all():
                    raise RuntimeError("battle renderer produced an empty frame")
            except Exception as exc:
                print(f"Battle render failed: {exc}", file=sys.stderr)
                self.canvas.delete("all")
                self.draw_battle_recovery()
        else:
            self.canvas.delete("overlay")
            self.draw_world()
            if self.mode == "dialog":
                self.draw_tagged_overlay(self.draw_dialog_overlay)
            elif self.mode == "shop":
                self.draw_tagged_overlay(self.draw_shop_overlay)
            elif self.mode == "map":
                self.draw_tagged_overlay(self.draw_world_map_overlay)
            elif self.mode == "skills":
                self.draw_tagged_overlay(self.draw_skill_tree_overlay)
            elif self.mode == "inventory":
                self.draw_tagged_overlay(self.draw_inventory_overlay)
        self.draw_hover_tooltip()

    def draw_pause_base(self) -> None:
        if self.battle and self.pause_return_mode == "battle":
            self.canvas.delete("all")
            self.world_renderer.static_key = None
            self.draw_battle()
        elif self.player:
            self.canvas.delete("overlay")
            self.draw_world()
        else:
            self.canvas.delete("all")
            self.draw_title_background()

    def draw_tagged_overlay(self, draw_func: Callable[[], None]) -> None:
        before = set(self.canvas.find_all())
        draw_func()
        after = set(self.canvas.find_all())
        for item in after - before:
            self.canvas.addtag_withtag("overlay", item)

    def update_name_entry_visibility(self) -> None:
        if self.mode == "new_game":
            self.name_entry.place(x=430, y=145, width=300, height=42)
        else:
            self.name_entry.place_forget()

    def draw_title_background(self) -> None:
        self.canvas.create_rectangle(0, 0, WIDTH, HEIGHT, fill="#191b25", outline="")
        for i in range(18):
            x = (i * 89 + self.frame * 2) % (WIDTH + 160) - 80
            y = 82 + (i * 37) % 600
            self.canvas.create_line(x, y, x + 48, y - 5, fill="#273143", width=1)

    def draw_main_menu(self) -> None:
        self.draw_title_background()
        self.canvas.create_text(WIDTH // 2, 86, text="Echoes of Alderfall", fill="#f4d58d", font=("Georgia", 34, "bold"))
        self.canvas.create_text(WIDTH // 2, 138, text="Alderfall remembers every name.", fill="#d7e4ef", font=("Segoe UI", 15))
        self.add_button(430, 254, 730, 306, "New Adventure", self.open_new_game, fill="#425d31", outline="#7eb05f")
        if save_exists():
            self.add_button(430, 326, 730, 378, "Load Adventure", self.open_save_list, fill="#394766", outline="#6e7fa0")
            settings_y = 398
        else:
            self.canvas.create_text(WIDTH // 2, 346, text="No saved adventures yet.", fill="#93a0b6", font=("Segoe UI", 12))
            settings_y = 326
        self.add_button(430, settings_y, 730, settings_y + 52, "Settings", lambda: self.open_settings("main_menu"), fill="#4b425d", outline="#7c6b9b")
        self.add_button(430, settings_y + 72, 574, settings_y + 118, "Restart", lambda: self.restart_game(False), fill="#4a4350", outline="#756b80")
        self.add_button(586, settings_y + 72, 730, settings_y + 118, "Exit", lambda: self.exit_game(False), fill="#5b3c3c", outline="#a56f62")
        self.canvas.create_text(WIDTH // 2, HEIGHT - 70, text="F11 toggles fullscreen", fill="#7e899f", font=("Segoe UI", 10))

    def draw_new_game(self) -> None:
        self.draw_title_background()
        self.canvas.create_text(WIDTH // 2, 74, text="New Adventure", fill="#f4d58d", font=("Georgia", 30, "bold"))
        self.canvas.create_text(WIDTH // 2, 124, text="Character Name", fill="#d7e4ef", font=("Segoe UI", 13, "bold"))
        self.canvas.create_rectangle(424, 139, 736, 193, fill="", outline="#5f6b84", width=2)
        self.canvas.create_text(WIDTH // 2, 218, text="Pick a class", fill="#d7e4ef", font=("Segoe UI", 16))
        x = 220
        card_h = 230
        for i, klass in enumerate(CLASSES.values(), start=1):
            selected = klass.name == self.selected_class
            left = x + (i - 1) * 230
            top = 258
            right = left + 190
            bottom = top + card_h
            self.canvas.create_rectangle(
                left,
                top,
                right,
                bottom,
                fill="#2a3141" if selected else "#202734",
                outline="#f4d58d" if selected else "#495267",
                width=3 if selected else 1,
            )
            self.canvas.create_image((left + right) // 2, top + 86, image=self.assets.get(f"class_{klass.name.lower()}", 120))
            self.canvas.create_text((left + right) // 2, top + 160, text=klass.name, fill="#ffffff", font=("Segoe UI", 18, "bold"))
            self.canvas.create_text(
                (left + right) // 2,
                top + 195,
                text=f"HP {klass.max_hp}   MP {klass.max_mp}",
                fill="#c9d2de",
                font=("Segoe UI", 11),
            )
            self.click_zones.append(ClickZone(left, top, right, bottom, lambda n=klass.name: self.select_class(n)))
        self.add_button(410, 530, 650, 580, "Start Adventure", self.start_selected_class, fill="#425d31", outline="#7eb05f")
        if save_exists():
            self.add_button(670, 530, 790, 580, "Load", self.open_save_list, fill="#394766", outline="#6e7fa0")
        self.add_button(32, 32, 142, 66, "Back", self.open_main_menu, fill="#303746", outline="#59667d")

    def draw_settings_screen(self) -> None:
        self.draw_title_background()
        self.canvas.create_text(WIDTH // 2, 74, text="Settings", fill="#f4d58d", font=("Georgia", 30, "bold"))
        self.draw_settings_panel(286, 130, 874, 688)
        self.add_button(32, 32, 142, 66, "Back", self.close_settings, fill="#303746", outline="#59667d")

    def draw_pause_menu(self) -> None:
        panel_x1, panel_y1 = 456, 188
        panel_x2, panel_y2 = 984, 604
        self.canvas.create_rectangle(0, 0, WIDTH, HEIGHT, fill="#080b10", stipple="gray50", outline="")
        self.canvas.create_rectangle(panel_x1, panel_y1, panel_x2, panel_y2, fill="#17202b", outline="#6d7890", width=2)
        self.canvas.create_text(WIDTH // 2, panel_y1 + 54, text="Paused", fill="#f4d58d", font=("Georgia", 28, "bold"))
        self.add_button(panel_x1 + 138, panel_y1 + 96, panel_x2 - 138, panel_y1 + 138, "Resume", self.resume_game, fill="#425d31", outline="#7eb05f")
        self.add_button(panel_x1 + 138, panel_y1 + 150, panel_x2 - 138, panel_y1 + 192, "Save Game", self.save_current_game, fill="#425d31", outline="#7eb05f")
        self.add_button(panel_x1 + 138, panel_y1 + 204, panel_x2 - 138, panel_y1 + 246, "Load Game", self.load_current_game, fill="#394766", outline="#6e7fa0")
        self.add_button(panel_x1 + 138, panel_y1 + 258, panel_x2 - 138, panel_y1 + 300, "Settings", lambda: self.open_settings("pause_menu"), fill="#4b425d", outline="#7c6b9b")
        self.add_button(panel_x1 + 138, panel_y1 + 312, panel_x1 + 274, panel_y1 + 354, "Restart", lambda: self.restart_game(True), fill="#4a4350", outline="#756b80")
        self.add_button(panel_x2 - 274, panel_y1 + 312, panel_x2 - 138, panel_y1 + 354, "Exit", lambda: self.exit_game(True), fill="#5b3c3c", outline="#a56f62")
        self.add_button(panel_x1 + 138, panel_y1 + 366, panel_x2 - 138, panel_y1 + 404, "Main Menu", self.return_to_main_menu, fill="#5b3c3c", outline="#a56f62")

    def draw_confirmation_overlay(self) -> None:
        panel_x1, panel_y1 = 478, 286
        panel_x2, panel_y2 = 962, 502
        self.canvas.create_rectangle(0, 0, WIDTH, HEIGHT, fill="#080b10", stipple="gray50", outline="")
        self.canvas.create_rectangle(panel_x1, panel_y1, panel_x2, panel_y2, fill="#17202b", outline="#a56f62", width=2)
        self.canvas.create_text(WIDTH // 2, panel_y1 + 48, text=self.confirm_title, fill="#f4d58d", font=("Georgia", 22, "bold"))
        self.canvas.create_text(WIDTH // 2, panel_y1 + 94, text=self.confirm_message, fill="#d8dfec", width=360, font=("Segoe UI", 11))
        self.add_button(panel_x1 + 76, panel_y2 - 62, panel_x1 + 216, panel_y2 - 24, "Cancel", self.cancel_confirmation, fill="#394766", outline="#6e7fa0")
        self.add_button(panel_x2 - 216, panel_y2 - 62, panel_x2 - 76, panel_y2 - 24, "Confirm", self.accept_confirmation, fill="#5b3c3c", outline="#a56f62")

    def draw_settings_overlay(self) -> None:
        panel_x1, panel_y1 = 314, 82
        panel_x2, panel_y2 = 1126, 786
        self.canvas.create_rectangle(0, 0, WIDTH, HEIGHT, fill="#080b10", stipple="gray50", outline="")
        self.canvas.create_rectangle(panel_x1, panel_y1, panel_x2, panel_y2, fill="#17202b", outline="#6d7890", width=2)
        self.canvas.create_text(WIDTH // 2, panel_y1 + 36, text="Settings", fill="#f4d58d", font=("Georgia", 24, "bold"))
        self.draw_settings_panel(panel_x1 + 62, panel_y1 + 78, panel_x2 - 62, panel_y2 - 72)
        self.add_button(panel_x2 - 168, panel_y2 - 50, panel_x2 - 34, panel_y2 - 16, "Back", self.close_settings, fill="#394766", outline="#6e7fa0")

    def draw_settings_panel(self, left: int, top: int, right: int, bottom: int) -> None:
        self.canvas.create_text(left, top, anchor="w", text="Graphics", fill="#ffffff", font=("Segoe UI", 15, "bold"))
        self.draw_option_row(left, top + 42, "World Detail", [("Fast", "fast"), ("Balanced", "balanced"), ("Full", "full")], self.render_mode, self.set_render_mode)
        self.draw_option_row(left, top + 104, "Battle Backdrops", [("Clean", "clean"), ("Detailed", "detailed")], self.battle_render_mode, self.set_battle_render_mode)

        self.canvas.create_text(left, top + 186, anchor="w", text="Monster Spawn Rates", fill="#ffffff", font=("Segoe UI", 15, "bold"))
        rows = [
            ("Wild", "monster_spawn_chances", "wild"),
            ("Dungeon", "monster_spawn_chances", "dungeon"),
            ("Dungeon Entrance", "monster_spawn_chances", "dungeon_entrance"),
            ("Two Monster Groups", "monster_group_chances", "two_monsters"),
            ("Three Monster Groups", "monster_group_chances", "three_monsters"),
        ]
        for i, (label, section, key) in enumerate(rows):
            y = top + 230 + i * 56
            values = game_config.MONSTER_SPAWN_CHANCES if section == "monster_spawn_chances" else game_config.MONSTER_GROUP_CHANCES
            value = values[key]
            self.canvas.create_text(left, y + 16, anchor="w", text=label, fill="#d8dfec", font=("Segoe UI", 11, "bold"))
            self.add_button(right - 236, y, right - 202, y + 34, "-", lambda s=section, k=key: self.adjust_chance(s, k, -0.02), fill="#343842")
            self.canvas.create_rectangle(right - 190, y + 8, right - 70, y + 26, fill="#10151f", outline="#3d465a")
            self.canvas.create_rectangle(right - 190, y + 8, right - 190 + int(120 * value), y + 26, fill="#6d8a71", outline="")
            self.canvas.create_text(right - 130, y + 17, text=f"{int(value * 100)}%", fill="#f4f9ff", font=("Segoe UI", 9, "bold"))
            self.add_button(right - 58, y, right - 24, y + 34, "+", lambda s=section, k=key: self.adjust_chance(s, k, 0.02), fill="#343842")

    def draw_option_row(self, left: int, y: int, label: str, options: list[tuple[str, str]], selected: str, action: Callable[[str], None]) -> None:
        self.canvas.create_text(left, y + 18, anchor="w", text=label, fill="#d8dfec", font=("Segoe UI", 11, "bold"))
        x = left + 210
        for text, value in options:
            width = 112
            active = value == selected
            self.add_button(
                x,
                y,
                x + width,
                y + 36,
                text,
                lambda v=value: action(v),
                fill="#425d31" if active else "#303746",
                outline="#7eb05f" if active else "#59667d",
            )
            x += width + 12

    def draw_save_list(self) -> None:
        self.draw_title_background()
        self.canvas.create_text(WIDTH // 2, 74, text="Load Adventure", fill="#f4d58d", font=("Georgia", 30, "bold"))
        saves = list_save_summaries()
        if not saves:
            self.canvas.create_text(WIDTH // 2, 240, text="No saved adventures yet.", fill="#d7e4ef", font=("Segoe UI", 16))
            self.add_button(430, 310, 730, 360, "New Adventure", self.open_new_game, fill="#425d31", outline="#7eb05f")
            self.add_button(32, 32, 142, 66, "Back", self.open_main_menu, fill="#303746", outline="#59667d")
            return
        max_offset = max(0, len(saves) - 5)
        self.save_list_offset = max(0, min(self.save_list_offset, max_offset))
        visible = saves[self.save_list_offset : self.save_list_offset + 5]
        for i, summary in enumerate(visible, start=1):
            top = 132 + (i - 1) * 88
            left, right = 214, 946
            self.canvas.create_rectangle(left, top, right, top + 68, fill="#202734", outline="#495267", width=2)
            self.canvas.create_text(left + 24, top + 18, anchor="w", text=f"{i}. {summary.character_name}", fill="#ffffff", font=("Segoe UI", 15, "bold"))
            detail = f"{summary.class_name}  Lv {summary.level}  Gold {summary.gold}  Seed {summary.world_seed}"
            self.canvas.create_text(left + 24, top + 43, anchor="w", text=detail, fill="#c9d2de", font=("Segoe UI", 10))
            self.canvas.create_text(right - 24, top + 34, anchor="e", text=summary.path.name, fill="#8f9bb0", font=("Segoe UI", 9))
            self.click_zones.append(ClickZone(left, top, right, top + 68, lambda s=summary.save_id: self.load_selected_save(s)))
        if self.save_list_offset > 0:
            self.add_button(966, 132, 1048, 164, "Up", lambda: setattr(self, "save_list_offset", max(0, self.save_list_offset - 1)))
        if self.save_list_offset < max_offset:
            self.add_button(966, 516, 1048, 548, "Down", lambda: setattr(self, "save_list_offset", min(max_offset, self.save_list_offset + 1)))
        self.add_button(32, 32, 142, 66, "Back", self.open_main_menu, fill="#303746", outline="#59667d")

    def select_class(self, name: str) -> None:
        self.selected_class = name

    def terrain_variant(self, x: int, y: int, tile: str) -> int:
        if tile != "w":
            key = (x, y, tile)
            cached = self._terrain_variant_cache.get(key)
            if cached is not None:
                return cached
        seed = (x * 73856093) ^ (y * 19349663) ^ (ord(tile) * 83492791)
        seed &= 0xFFFFFFFF
        if tile == "w":
            return (seed + self.frame // 7) % 4
        if tile in {"g", "f", "n"}:
            variant = seed % 8
        elif tile in {"s", "v", "b", "m", "q", "r"}:
            variant = seed % 4
        elif tile in {"p", "j", "l", "x", "h"}:
            variant = seed % 3
        else:
            variant = 0
        self._terrain_variant_cache[key] = variant
        return variant

    def terrain_seed(self, x: int, y: int, tile: str, salt: int = 0) -> int:
        return ((x * 73856093) ^ (y * 19349663) ^ (ord(tile) * 83492791) ^ (salt * 2654435761)) & 0xFFFFFFFF

    def smooth_biome_noise(self, wx: int, wy: int, tile: str, salt: int, scale: int) -> float:
        gx = math.floor(wx / scale)
        gy = math.floor(wy / scale)
        tx = (wx / scale) - gx
        ty = (wy / scale) - gy
        tx = tx * tx * (3.0 - 2.0 * tx)
        ty = ty * ty * (3.0 - 2.0 * ty)

        def corner(ix: int, iy: int) -> float:
            return (self.terrain_seed(ix, iy, tile, salt) & 1023) / 1023.0

        top = corner(gx, gy) * (1.0 - tx) + corner(gx + 1, gy) * tx
        bottom = corner(gx, gy + 1) * (1.0 - tx) + corner(gx + 1, gy + 1) * tx
        return top * (1.0 - ty) + bottom * ty

    def biome_cluster_strength(self, tile: str, wx: int, wy: int) -> float:
        broad = self.smooth_biome_noise(wx, wy, tile, 401, 10)
        medium = self.smooth_biome_noise(wx, wy, tile, 547, 6)
        grain = (self.terrain_seed(wx, wy, tile, 619) & 255) / 255.0
        return max(0.0, min(1.0, broad * 0.56 + medium * 0.34 + grain * 0.10))

    def near_landmark_tile(self, wx: int, wy: int, radius: int = 1) -> bool:
        for dy in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                nt = tile_at(self.current_map_id, wx + dx, wy + dy)
                if nt in {"r", "c", "u", "d"}:
                    return True
        return False

    def draw_location_ground(self, sx: int, sy: int, wx: int, wy: int, tile: str) -> None:
        location = location_at(self.current_map_id, wx, wy)
        if location is None or tile in {"w", "c", "u"}:
            return
        options = LOCATION_GROUND_ASSETS.get(location.kind)
        if not options:
            return
        seed = self.terrain_seed(wx, wy, location.kind[0], location.salt)
        asset = options[(seed >> 6) % len(options)]
        if location.kind == "farmland" and ((wx + location.salt) // 2 + wy) % 3 == 0:
            asset = "location_farmland_wheat"
        elif location.kind == "graveyard" and (tile == "d" or seed % 100 < 30):
            asset = "location_graveyard_path"
        self.canvas.create_image(
            sx * TILE - 1,
            sy * TILE - 1,
            anchor="nw",
            image=self.assets.get(asset, TILE + 2, variant=self.terrain_variant(wx, wy, tile)),
        )

    def draw_location_decoration(self, sx: int, sy: int, wx: int, wy: int, tile: str) -> bool:
        location = location_at(self.current_map_id, wx, wy)
        if location is None or tile in {"w", "c", "u"}:
            return False
        if location.kind == "graveyard" and tile == "d":
            self.canvas.create_image(
                sx * TILE + TILE // 2,
                sy * TILE + int(TILE * 0.46),
                image=self.assets.get("location_crypt_entrance", int(TILE * 1.36)),
            )
            return True

        options = LOCATION_DECORATIONS.get(location.kind)
        if not options:
            return False
        seed = self.terrain_seed(wx, wy, location.kind[0], location.salt + 101)
        dx = wx - location.cx
        dy = wy - location.cy
        center_pull = max(0.0, 1.0 - (abs(dx) / max(1, location.rx) + abs(dy) / max(1, location.ry)) * 0.5)
        base_chance = {"farmland": 24, "goblin_camp": 34, "graveyard": 32}.get(location.kind, 26)
        chance = min(58, base_chance + int(center_pull * 18))
        if tile == "r":
            chance = max(10, chance - 16)
        if seed % 100 >= chance:
            return False

        asset = weighted_decoration_choice(options, seed >> 8)
        if location.kind == "goblin_camp":
            if abs(dx) <= 1 and abs(dy) <= 1:
                asset = "location_camp_fire" if (seed & 1) else "location_camp_tent"
            elif abs(dx) >= max(2, location.rx - 2) or abs(dy) >= max(2, location.ry - 2):
                asset = "location_camp_palisade"
        elif location.kind == "farmland":
            if abs(dx) >= max(2, location.rx - 1) or abs(dy) >= max(2, location.ry - 1):
                asset = "location_farmland_fence"
        elif location.kind == "graveyard" and self.near_landmark_tile(wx, wy, radius=1):
            asset = "location_graveyard_tombstones"

        size = decoration_size(asset, seed, TILE)
        tag = decoration_primary_tag(asset)
        ox, oy = self.decoration_anchor(wx, wy, tile, tag, seed, self.terrain_seed(location.cx, location.cy, location.kind[0], 1229), center_pull)
        if size > TILE:
            ox = max(int(TILE * 0.34), min(int(TILE * 0.66), ox))
            oy = max(int(TILE * 0.34), min(int(TILE * 0.64), oy))
        self.canvas.create_image(
            sx * TILE + ox,
            sy * TILE + oy,
            image=self.assets.get(asset, size, variant=(seed >> 20) % 3),
        )
        return True

    def mountain_base_asset(self, wx: int, wy: int) -> str:
        return "mountain_massif_tile"

    def mountain_pass_base_asset(self, wx: int, wy: int) -> str:
        return "mountain_massif_tile"

    def path_base_asset(self, wx: int, wy: int) -> str:
        return self.context_base_asset(wx, wy, avoid={"r", "c", "u", "w"})

    def settlement_base_asset(self, wx: int, wy: int) -> str:
        return self.context_base_asset_radius(wx, wy, avoid={"c", "u", "r", "w"}, radius=4)

    def overworld_village_cluster_asset(self, wx: int, wy: int) -> str:
        for map_id, (vx, vy) in VILLAGE_CENTERS.items():
            if wx == vx and wy == vy:
                return VILLAGE_OVERWORLD_CLUSTER_ASSETS.get(map_id, "city_overworld_village")
        return "city_overworld_village"

    def context_base_asset(self, wx: int, wy: int, avoid: set[str]) -> str:
        return self.context_base_asset_radius(wx, wy, avoid, radius=1)

    def context_base_asset_radius(self, wx: int, wy: int, avoid: set[str], radius: int) -> str:
        cache_key = (self.current_map_id, wx, wy, tuple(sorted(avoid)), radius)
        cached = self._context_base_cache.get(cache_key)
        if cached is not None:
            return cached
        counts: dict[str, int] = {}
        for dy in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                if dx == 0 and dy == 0:
                    continue
                nt = tile_at(self.current_map_id, wx + dx, wy + dy)
                if nt in avoid or nt not in TILE_ASSETS:
                    continue
                if nt in TERRAIN_BLEND_TILES and nt != "w":
                    distance = max(abs(dx), abs(dy))
                    weight = max(1, radius + 1 - distance)
                    counts[nt] = counts.get(nt, 0) + weight
        if not counts:
            self._context_base_cache[cache_key] = "grass"
            return "grass"
        chosen = max(counts, key=lambda key: (counts[key], TERRAIN_BLEND_PRIORITY.get(key, 0)))
        asset = TILE_ASSETS[chosen]
        self._context_base_cache[cache_key] = asset
        return asset

    def is_mountain_massif_anchor(self, wx: int, wy: int) -> bool:
        if tile_at(self.current_map_id, wx, wy) != "m":
            return False
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                if tile_at(self.current_map_id, wx + dx, wy + dy) in {"q", "r", "c", "u", "d"}:
                    return False
        area = 0
        for dy in range(3):
            for dx in range(3):
                if tile_at(self.current_map_id, wx + dx, wy + dy) == "m":
                    area += 1
        if area < 5:
            return False
        seed = self.terrain_seed(wx, wy, "m", 97)
        if seed % 100 >= 92:
            return False
        return True

    def is_biome_tile(self, tile: str) -> bool:
        return tile in {"g", "f", "s", "n", "v", "b", "q"}

    def is_city_cluster_center(self, wx: int, wy: int) -> bool:
        key = (self.current_map_id, "city", wx, wy)
        cached = self._cluster_center_cache.get(key)
        if cached is not None:
            return cached
        if self.current_map_id != OVERWORLD_ID or tile_at(self.current_map_id, wx, wy) != "c":
            self._cluster_center_cache[key] = False
            return False
        result = any(wx == cx and wy == cy for cx, cy in CITY_GATE_CENTERS.values())
        self._cluster_center_cache[key] = result
        return result

    def is_village_cluster_center(self, wx: int, wy: int) -> bool:
        key = (self.current_map_id, "village", wx, wy)
        cached = self._cluster_center_cache.get(key)
        if cached is not None:
            return cached
        if self.current_map_id != OVERWORLD_ID or tile_at(self.current_map_id, wx, wy) != "u":
            self._cluster_center_cache[key] = False
            return False
        result = any(wx == vx and wy == vy for vx, vy in VILLAGE_CENTERS.values())
        self._cluster_center_cache[key] = result
        return result

    def draw_biome_blend(self, sx: int, sy: int, wx: int, wy: int, tile: str) -> None:
        if tile not in TERRAIN_BLEND_TILES:
            return
        px = sx * TILE
        py = sy * TILE
        neighbors = {
            "up": tile_at(self.current_map_id, wx, wy - 1),
            "down": tile_at(self.current_map_id, wx, wy + 1),
            "left": tile_at(self.current_map_id, wx - 1, wy),
            "right": tile_at(self.current_map_id, wx + 1, wy),
        }
        for side, nt in neighbors.items():
            if nt == tile or nt not in TERRAIN_BLEND_TILES:
                continue
            if TERRAIN_BLEND_PRIORITY.get(nt, 0) <= TERRAIN_BLEND_PRIORITY.get(tile, 0):
                continue
            self.draw_terrain_edge(px, py, wx, wy, side, nt, 9)
            self.draw_transition_edge_decor(px, py, wx, wy, side, tile, nt)

        corners = {
            "up_left": tile_at(self.current_map_id, wx - 1, wy - 1),
            "up_right": tile_at(self.current_map_id, wx + 1, wy - 1),
            "down_left": tile_at(self.current_map_id, wx - 1, wy + 1),
            "down_right": tile_at(self.current_map_id, wx + 1, wy + 1),
        }
        for corner, nt in corners.items():
            if nt == tile or nt not in TERRAIN_BLEND_TILES:
                continue
            if TERRAIN_BLEND_PRIORITY.get(nt, 0) <= TERRAIN_BLEND_PRIORITY.get(tile, 0):
                continue
            self.draw_terrain_corner(px, py, wx, wy, corner, nt)

    def draw_terrain_edge(self, px: int, py: int, wx: int, wy: int, side: str, tile: str, depth: int) -> None:
        color = BLEND_COLORS[tile]
        seed = self.terrain_seed(wx, wy, tile, {"up": 11, "down": 13, "left": 17, "right": 19}[side])
        points: list[tuple[int, int]] = []
        steps = 6
        for i in range(steps + 1):
            t = i / steps
            wobble = ((seed >> (i * 3)) & 7) - 3
            d = max(5, min(depth + 5, depth + wobble))
            if side == "up":
                points.append((px + int(TILE * t), py + d))
            elif side == "down":
                points.append((px + int(TILE * t), py + TILE - d))
            elif side == "left":
                points.append((px + d, py + int(TILE * t)))
            else:
                points.append((px + TILE - d, py + int(TILE * t)))

        if side == "up":
            polygon = [(px - 1, py - 1), (px + TILE + 1, py - 1)] + list(reversed(points))
        elif side == "down":
            polygon = [(px - 1, py + TILE + 1), (px + TILE + 1, py + TILE + 1)] + list(reversed(points))
        elif side == "left":
            polygon = [(px - 1, py - 1), (px - 1, py + TILE + 1)] + list(reversed(points))
        else:
            polygon = [(px + TILE + 1, py - 1), (px + TILE + 1, py + TILE + 1)] + list(reversed(points))
        self.canvas.create_polygon(*polygon, fill=color, outline="", smooth=True, stipple="gray50")
        self.draw_terrain_edge_detail(points, tile)

    def draw_terrain_edge_detail(self, points: list[tuple[int, int]], tile: str) -> None:
        highlight = TERRAIN_EDGE_HIGHLIGHTS.get(tile)
        if not highlight or len(points) < 2:
            return
        self.canvas.create_line(*points, fill=highlight, width=1, smooth=True, stipple="gray50")

    def draw_terrain_corner(self, px: int, py: int, wx: int, wy: int, corner: str, tile: str) -> None:
        color = BLEND_COLORS[tile]
        seed = self.terrain_seed(wx, wy, tile, {"up_left": 23, "up_right": 29, "down_left": 31, "down_right": 37}[corner])
        size = 11 + seed % 7
        if corner == "up_left":
            points = [(px - 1, py - 1), (px + size, py - 1), (px + size - 2, py + 5), (px + 6, py + size), (px - 1, py + size)]
        elif corner == "up_right":
            points = [(px + TILE + 1, py - 1), (px + TILE - size, py - 1), (px + TILE - size + 2, py + 5), (px + TILE - 6, py + size), (px + TILE + 1, py + size)]
        elif corner == "down_left":
            points = [(px - 1, py + TILE + 1), (px + size, py + TILE + 1), (px + size - 2, py + TILE - 5), (px + 6, py + TILE - size), (px - 1, py + TILE - size)]
        else:
            points = [(px + TILE + 1, py + TILE + 1), (px + TILE - size, py + TILE + 1), (px + TILE - size + 2, py + TILE - 5), (px + TILE - 6, py + TILE - size), (px + TILE + 1, py + TILE - size)]
        self.canvas.create_polygon(*points, fill=color, outline="", smooth=True, stipple="gray50")

    def draw_transition_edge_decor(self, px: int, py: int, wx: int, wy: int, side: str, base_tile: str, edge_tile: str) -> None:
        pair = {base_tile, edge_tile}
        if not (pair & {"f", "n"}):
            return
        seed = self.terrain_seed(wx, wy, edge_tile, {"up": 301, "down": 307, "left": 311, "right": 313}[side])
        chance = 62 if pair == {"g", "f"} else 42
        if seed % 100 >= chance:
            return

        count = 1 + ((seed >> 7) % (2 if pair == {"g", "f"} else 1))
        for i in range(count):
            local = self.terrain_seed(wx, wy, edge_tile, 331 + i * 17)
            along = 9 + ((local >> 4) % max(1, TILE - 18))
            inset = 5 + ((local >> 12) % 10)
            if side == "up":
                x = px + along
                y = py + inset
            elif side == "down":
                x = px + along
                y = py + TILE - inset
            elif side == "left":
                x = px + inset
                y = py + along
            else:
                x = px + TILE - inset
                y = py + along

            if "f" in pair:
                assets = (
                    "deco_forest_fern",
                    "deco_forest_moss_rock",
                    "deco_forest_mushrooms",
                    "deco_imagen_fallen_roots",
                )
                asset = assets[(local >> 19) % len(assets)]
                size = max(12, int(TILE * (0.34 + ((local >> 23) % 3) * 0.06)))
                self.canvas.create_image(x, y, image=self.assets.get(asset, size, variant=(local >> 27) % 3))
            elif "n" in pair:
                radius = 3 + ((local >> 18) % 4)
                self.canvas.create_oval(
                    x - radius - 2,
                    y - radius,
                    x + radius + 2,
                    y + radius,
                    fill="#d8e8e9",
                    outline="",
                    stipple="gray50",
                )
                if base_tile != "n":
                    self.canvas.create_line(x - radius, y + 1, x + radius, y - 1, fill="#91b95d", width=1, stipple="gray50")

    def draw_mountain_foothill_blend(self, sx: int, sy: int, wx: int, wy: int) -> None:
        if tile_at(self.current_map_id, wx, wy) != "m":
            return
        px = sx * TILE
        py = sy * TILE
        for dy, side in ((-1, "up"), (1, "down")):
            nt = tile_at(self.current_map_id, wx, wy + dy)
            if nt != "m" and nt in BLEND_COLORS:
                self.draw_terrain_edge(px, py, wx, wy, side, nt, 7)
        for dx, side in ((-1, "left"), (1, "right")):
            nt = tile_at(self.current_map_id, wx + dx, wy)
            if nt != "m" and nt in BLEND_COLORS:
                self.draw_terrain_edge(px, py, wx, wy, side, nt, 7)

    def draw_biome_detail(self, sx: int, sy: int, wx: int, wy: int, tile: str) -> None:
        px = sx * TILE
        py = sy * TILE
        if location_at(self.current_map_id, wx, wy):
            self.draw_location_decoration(sx, sy, wx, wy, tile)
            return
        self.draw_biome_decoration(sx, sy, wx, wy, tile)
        if tile == "s":
            for i in range(2):
                seed = self.terrain_seed(wx, wy, tile, i)
                y = py + 12 + seed % 24
                self.canvas.create_line(px + 4, y, px + 18, y - 2, px + 34, y + 1, px + 44, y - 1, fill="#e6c77a", width=1, smooth=True)
        elif tile == "n":
            for k in range(4):
                off = (wx * 3 + wy * 5 + k * 11 + self.frame // 7) % TILE
                self.canvas.create_line(px + off, py + 6, px + off + 3, py + 9, fill="#e8f2ff", width=1)
        elif tile == "b":
            self.canvas.create_line(px + 8, py + 10, px + 22, py + 24, fill="#7a6a58", width=2)
            self.canvas.create_line(px + 30, py + 14, px + 40, py + 28, fill="#6f6050", width=2)
        elif tile == "q":
            self.draw_mountain_pass_detail(px, py, wx, wy)

    def draw_mountain_pass_detail(self, px: int, py: int, wx: int, wy: int) -> None:
        seed = self.terrain_seed(wx, wy, "q", 41)
        up = tile_at(self.current_map_id, wx, wy - 1) in {"q", "r"}
        down = tile_at(self.current_map_id, wx, wy + 1) in {"q", "r"}
        left = tile_at(self.current_map_id, wx - 1, wy) in {"q", "r"}
        right = tile_at(self.current_map_id, wx + 1, wy) in {"q", "r"}
        vertical = (up or down) and not (left or right)
        horizontal = (left or right) and not (up or down)
        trail = "#7a5f42" if seed % 3 else "#866a49"
        trail_shadow = "#4e4238"
        trail_light = "#b59469"
        snow = "#dde6e5"
        moss = "#3e6a43"

        if vertical:
            self.canvas.create_polygon(px + 16, py - 1, px + 33, py - 1, px + 37, py + TILE + 1, px + 12, py + TILE + 1, fill=trail_shadow, outline="")
            self.canvas.create_polygon(px + 18, py - 1, px + 31, py - 1, px + 34, py + TILE + 1, px + 15, py + TILE + 1, fill=trail, outline="")
            self.canvas.create_line(px + 20, py, px + 18, py + TILE, fill=trail_light, width=1)
            self.canvas.create_line(px + 31, py, px + 34, py + TILE, fill=trail_shadow, width=1)
        elif horizontal:
            self.canvas.create_polygon(px - 1, py + 16, px + TILE + 1, py + 13, px + TILE + 1, py + 36, px - 1, py + 39, fill=trail_shadow, outline="")
            self.canvas.create_polygon(px - 1, py + 18, px + TILE + 1, py + 16, px + TILE + 1, py + 33, px - 1, py + 36, fill=trail, outline="")
            self.canvas.create_line(px, py + 21, px + TILE, py + 19, fill=trail_light, width=1)
            self.canvas.create_line(px, py + 33, px + TILE, py + 35, fill=trail_shadow, width=1)
        else:
            self.canvas.create_polygon(px + 11, py + 10, px + 39, py + 12, px + 36, py + 39, px + 9, py + 36, fill=trail_shadow, outline="")
            self.canvas.create_polygon(px + 14, py + 12, px + 36, py + 14, px + 33, py + 36, px + 12, py + 34, fill=trail, outline="")

        for i in range(5):
            local = self.terrain_seed(wx, wy, "q", 83 + i * 7)
            x = px + 7 + (local % 34)
            y = py + 9 + ((local >> 8) % 30)
            self.canvas.create_line(x, y, x + 8 + (i % 3), y - 1 + (i % 2) * 3, fill=trail_light, width=1)
        for i in range(2):
            local = self.terrain_seed(wx, wy, "q", 211 + i * 19)
            x = px + 5 + (local % 32)
            y = py + 6 + ((local >> 7) % 29)
            self.canvas.create_oval(x, y, x + 7 + (local & 3), y + 4, fill=trail_shadow, outline="")
        if seed % 100 < 38:
            self.canvas.create_line(px + 5, py + 8, px + 18, py + 5, px + 33, py + 7, fill=snow, width=2, smooth=True)
        if seed % 100 > 55:
            bx = px + 8 + ((seed >> 17) % 28)
            by = py + 11 + ((seed >> 23) % 26)
            self.canvas.create_polygon(bx, by + 3, bx + 5, by, bx + 10, by + 4, bx + 4, by + 7, fill=moss, outline="")
        if tile_at(self.current_map_id, wx, wy - 1) == "m":
            self.canvas.create_line(px + 5, py + 5, px + 18, py + 2, px + 33, py + 5, fill=trail_shadow, width=2, smooth=True)
        if tile_at(self.current_map_id, wx, wy + 1) == "m":
            self.canvas.create_line(px + 6, py + TILE - 6, px + 22, py + TILE - 3, px + 41, py + TILE - 7, fill=trail_shadow, width=2, smooth=True)

    def draw_mountain_massif(self, sx: int, sy: int, wx: int, wy: int) -> None:
        if not self.is_mountain_massif_anchor(wx, wy):
            return
        px = sx * TILE
        py = sy * TILE
        variant = self.terrain_variant(wx, wy, "m")
        self.canvas.create_image(
            px - TILE // 2,
            py - TILE,
            anchor="nw",
            image=self.assets.get("mountain_massif", TILE * 3, variant=variant),
        )

    def draw_mountain_footprint(self, px: int, py: int, wx: int, wy: int) -> None:
        seed = self.terrain_seed(wx, wy, "m", 53)
        up = tile_at(self.current_map_id, wx, wy - 1) == "m"
        down = tile_at(self.current_map_id, wx, wy + 1) == "m"
        left = tile_at(self.current_map_id, wx - 1, wy) == "m"
        right = tile_at(self.current_map_id, wx + 1, wy) == "m"
        up_left = tile_at(self.current_map_id, wx - 1, wy - 1) == "m"
        up_right = tile_at(self.current_map_id, wx + 1, wy - 1) == "m"
        down_left = tile_at(self.current_map_id, wx - 1, wy + 1) == "m"
        down_right = tile_at(self.current_map_id, wx + 1, wy + 1) == "m"

        rock_floor = "#59615e"
        rock_dark = "#3e4544"
        rock_mid = "#6f6b60"
        rock_light = "#a79a84"
        ledge_grass = "#4f7d45"
        moss = "#315f43"

        x1 = px - 1 if left else px + 4 + (seed & 3)
        x2 = px + TILE + 1 if right else px + TILE - 4 - ((seed >> 2) & 3)
        y1 = py - 1 if up else py + 4 + ((seed >> 4) & 3)
        y2 = py + TILE + 1 if down else py + TILE - 4 - ((seed >> 6) & 3)

        # Continuous modular mass. Neighboring mountain tiles overdraw to the
        # shared edge, while exposed sides wobble inward like an autotile mask.
        points = [
            (x1, y1 + ((seed >> 8) & 3)),
            (px + 15, y1 - (1 if up else 0) + ((seed >> 10) & 3)),
            (px + 32, y1 + ((seed >> 12) & 3)),
            (x2, y1 + ((seed >> 14) & 3)),
            (x2 - ((seed >> 16) & 3), py + 17),
            (x2, py + 32),
            (x2 - ((seed >> 18) & 3), y2),
            (px + 30, y2 - ((seed >> 20) & 3)),
            (px + 14, y2 + (1 if down else 0) - ((seed >> 22) & 3)),
            (x1 + ((seed >> 24) & 3), y2),
            (x1, py + 31),
            (x1 + ((seed >> 26) & 3), py + 16),
        ]
        self.canvas.create_polygon(*[coord for point in points for coord in point], fill=rock_floor, outline="", smooth=True)

        if up:
            self.canvas.create_rectangle(px - (1 if left else 0), py - 1, px + TILE + (1 if right else 0), py + 10, fill=rock_floor, outline="")
        if down:
            self.canvas.create_rectangle(px - (1 if left else 0), py + TILE - 10, px + TILE + (1 if right else 0), py + TILE + 1, fill=rock_floor, outline="")
        if left:
            self.canvas.create_rectangle(px - 1, py - (1 if up else 0), px + 10, py + TILE + (1 if down else 0), fill=rock_floor, outline="")
        if right:
            self.canvas.create_rectangle(px + TILE - 10, py - (1 if up else 0), px + TILE + 1, py + TILE + (1 if down else 0), fill=rock_floor, outline="")

        if up and left and not up_left:
            self.canvas.create_polygon(px - 1, py - 1, px + 14, py - 1, px - 1, py + 14, fill=rock_floor, outline="")
        if up and right and not up_right:
            self.canvas.create_polygon(px + TILE + 1, py - 1, px + TILE - 14, py - 1, px + TILE + 1, py + 14, fill=rock_floor, outline="")
        if down and left and not down_left:
            self.canvas.create_polygon(px - 1, py + TILE + 1, px + 14, py + TILE + 1, px - 1, py + TILE - 14, fill=rock_floor, outline="")
        if down and right and not down_right:
            self.canvas.create_polygon(px + TILE + 1, py + TILE + 1, px + TILE - 14, py + TILE + 1, px + TILE + 1, py + TILE - 14, fill=rock_floor, outline="")

        if not up:
            self.draw_mountain_cliff_edge(px, py, wx, wy, "up", rock_dark, rock_light)
        if not down:
            self.draw_mountain_cliff_edge(px, py, wx, wy, "down", rock_dark, rock_light)
        if not left:
            self.draw_mountain_cliff_edge(px, py, wx, wy, "left", rock_dark, rock_light)
        if not right:
            self.draw_mountain_cliff_edge(px, py, wx, wy, "right", rock_dark, rock_light)

        if not up:
            self.canvas.create_line(px + 8, py + 10, px + 22, py + 8, px + 39, py + 11, fill=ledge_grass, width=2, smooth=True)
        if not down:
            self.canvas.create_line(px + 6, py + TILE - 9, px + 21, py + TILE - 7, px + 41, py + TILE - 10, fill=ledge_grass, width=2, smooth=True)
        if not left:
            self.canvas.create_line(px + 9, py + 8, px + 7, py + 23, px + 10, py + 40, fill=ledge_grass, width=2, smooth=True)
        if not right:
            self.canvas.create_line(px + TILE - 9, py + 8, px + TILE - 7, py + 23, px + TILE - 10, py + 40, fill=ledge_grass, width=2, smooth=True)

        for i in range(4):
            tx = px + 7 + ((seed >> (i * 6)) & 30)
            ty = py + 10 + ((seed >> (i * 7 + 3)) & 24)
            self.canvas.create_line(tx, ty, tx + 8 + (i % 3), ty + (2 if i % 2 else -1), fill=rock_mid if i % 2 else rock_dark, width=1)
        if seed % 100 < 42:
            bx = px + 10 + ((seed >> 19) & 24)
            by = py + 20 + ((seed >> 23) & 16)
            self.canvas.create_polygon(bx, by + 5, bx + 4, by, bx + 8, by + 5, bx + 4, by + 9, fill=moss, outline="")

    def draw_mountain_cliff_edge(
        self,
        px: int,
        py: int,
        wx: int,
        wy: int,
        side: str,
        dark: str,
        light: str,
    ) -> None:
        seed = self.terrain_seed(wx, wy, "m", {"up": 61, "down": 67, "left": 71, "right": 73}[side])
        points: list[tuple[int, int]] = []
        for i in range(7):
            t = i / 6
            wobble = ((seed >> (i * 3)) & 5) - 2
            if side == "up":
                points.append((px + int(TILE * t), py + 7 + wobble))
            elif side == "down":
                points.append((px + int(TILE * t), py + TILE - 7 - wobble))
            elif side == "left":
                points.append((px + 7 + wobble, py + int(TILE * t)))
            else:
                points.append((px + TILE - 7 - wobble, py + int(TILE * t)))
        self.canvas.create_line(*points, fill=dark, width=5, smooth=True)
        self.canvas.create_line(*points, fill=light, width=2, smooth=True)
        for i, (x, y) in enumerate(points[1:-1], start=1):
            if i % 2 == 0:
                if side in {"up", "down"}:
                    self.canvas.create_line(x, y, x + 2, y + (8 if side == "up" else -8), fill=dark, width=1)
                else:
                    self.canvas.create_line(x, y, x + (8 if side == "left" else -8), y + 2, fill=dark, width=1)

    def draw_biome_decoration(self, sx: int, sy: int, wx: int, wy: int, tile: str) -> None:
        options = BIOME_DECORATIONS.get(tile)
        if not options:
            return
        if tile == "f":
            self.draw_forest_mesh(sx, sy, wx, wy, options)
            return
        candidate = self.biome_decoration_candidate(wx, wy, tile)
        if candidate is None:
            return
        if self.decoration_spacing_suppressed(wx, wy, candidate):
            return
        self.canvas.create_image(
            sx * TILE + candidate.ox,
            sy * TILE + candidate.oy,
            image=self.assets.get(candidate.asset, candidate.size, variant=candidate.variant),
        )

    def biome_decoration_candidate(self, wx: int, wy: int, tile: str) -> DecorationCandidate | None:
        options = BIOME_DECORATIONS.get(tile)
        if not options or tile == "f":
            return None
        seed = self.terrain_seed(wx, wy, tile, 7)
        cluster = self.biome_cluster_strength(tile, wx, wy)
        base_chance = {
            "g": 8,
            "s": 7,
            "n": 9,
            "v": 16,
            "b": 8,
            "m": 6,
            "q": 7,
            "w": 10,
        }.get(tile, 8)
        cluster_gain = {
            "g": 44,
            "s": 40,
            "n": 36,
            "v": 42,
            "b": 32,
            "m": 26,
            "q": 28,
            "w": 30,
        }.get(tile, 36)
        patch_seed = self.terrain_seed(wx // 5, wy // 5, tile, 433)
        dominant = weighted_decoration_choice(options, patch_seed >> 8)
        local = weighted_decoration_choice(options, seed >> 8)
        dominant_bias = 58 if cluster >= 0.58 else 31
        asset = dominant if ((seed >> 13) % 100) < dominant_bias else local
        tag = decoration_primary_tag(asset)
        profile = decoration_spawn_profile(asset)
        chance = base_chance + int(cluster * cluster_gain * profile.cluster_gain_scale)
        if self.near_landmark_tile(wx, wy, radius=1):
            chance -= 10 if tile in {"g", "s", "n", "b"} else 7
        if tile in {"m", "q"} and self.near_landmark_tile(wx, wy, radius=2):
            chance -= 10
        chance = int(round(chance * profile.chance_scale))
        chance = max(2, min(profile.max_chance, chance))
        if seed % 100 >= chance:
            return None
        size = self.biome_decoration_size(asset, seed)
        ox, oy = self.decoration_anchor(wx, wy, tile, tag, seed, patch_seed, cluster)
        if size > TILE:
            ox = max(int(TILE * 0.32), min(int(TILE * 0.68), ox))
            oy = max(int(TILE * 0.34), min(int(TILE * 0.64), oy))
        return DecorationCandidate(asset, tag, seed, chance, cluster, size, ox, oy, 0)

    def decoration_anchor(self, wx: int, wy: int, tile: str, tag: str, seed: int, patch_seed: int, cluster: float) -> tuple[int, int]:
        if tile == "r":
            return self.road_decoration_anchor(wx, wy, seed)
        if tag == "ground":
            anchor_x = int(TILE * (0.26 + ((patch_seed >> 17) % 23) / 100.0))
            anchor_y = int(TILE * (0.30 + ((patch_seed >> 22) % 19) / 100.0))
            spread_x = max(1, int(TILE * (0.24 - cluster * 0.08)))
            spread_y = max(1, int(TILE * (0.22 - cluster * 0.07)))
        else:
            lane = (seed >> 28) & 3
            lane_x = (0.34, 0.66, 0.38, 0.62)[lane]
            lane_y = (0.40, 0.43, 0.64, 0.61)[lane]
            anchor_x = int(TILE * lane_x)
            anchor_y = int(TILE * lane_y)
            spread_x = max(1, int(TILE * 0.10))
            spread_y = max(1, int(TILE * 0.08))
        ox = anchor_x + ((seed >> 20) % (spread_x * 2 + 1)) - spread_x
        oy = anchor_y + ((seed >> 24) % (spread_y * 2 + 1)) - spread_y
        return ox, oy

    def road_decoration_anchor(self, wx: int, wy: int, seed: int) -> tuple[int, int]:
        side = -1 if ((seed >> 19) & 1) == 0 else 1
        vertical_road = tile_at(self.current_map_id, wx, wy - 1) == "r" or tile_at(self.current_map_id, wx, wy + 1) == "r"
        horizontal_road = tile_at(self.current_map_id, wx - 1, wy) == "r" or tile_at(self.current_map_id, wx + 1, wy) == "r"
        vertical = vertical_road and (not horizontal_road or ((seed >> 21) & 1) == 0)
        if vertical:
            ox = int(TILE * (0.27 if side < 0 else 0.73))
            oy = int(TILE * (0.34 + ((seed >> 24) % 32) / 100.0))
        else:
            ox = int(TILE * (0.34 + ((seed >> 24) % 32) / 100.0))
            oy = int(TILE * (0.27 if side < 0 else 0.73))
        return ox, oy

    def decoration_spacing_suppressed(self, wx: int, wy: int, candidate: DecorationCandidate) -> bool:
        profile = decoration_spawn_profile(candidate.asset)
        if profile.spacing_radius <= 0:
            return False
        my_priority = self.decoration_spacing_priority(wx, wy, candidate)
        radius = profile.spacing_radius
        for dy in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                if dx == 0 and dy == 0:
                    continue
                if abs(dx) + abs(dy) > radius + 1:
                    continue
                nt = tile_at(self.current_map_id, wx + dx, wy + dy)
                other = self.biome_decoration_candidate(wx + dx, wy + dy, nt)
                if other is None or other.tag not in profile.spacing_tags:
                    continue
                if self.decoration_spacing_priority(wx + dx, wy + dy, other) < my_priority:
                    return True
        return False

    def decoration_spacing_priority(self, wx: int, wy: int, candidate: DecorationCandidate) -> tuple[int, int, int, int]:
        tag_rank = {"road": 0, "feature": 1, "water": 2, "tree": 3, "ground": 4}.get(candidate.tag, 5)
        return (candidate.seed % 100, tag_rank, wy, wx)

    def biome_decoration_size(self, asset: str, seed: int) -> int:
        return decoration_size(asset, seed, TILE)

    def draw_forest_mesh(self, sx: int, sy: int, wx: int, wy: int, options: tuple[str, ...]) -> None:
        px = sx * TILE
        py = sy * TILE
        seed = self.terrain_seed(wx, wy, "f", 113)
        neighbors = {
            "up": tile_at(self.current_map_id, wx, wy - 1) == "f",
            "down": tile_at(self.current_map_id, wx, wy + 1) == "f",
            "left": tile_at(self.current_map_id, wx - 1, wy) == "f",
            "right": tile_at(self.current_map_id, wx + 1, wy) == "f",
        }
        self.draw_forest_floor_mesh(px, py, wx, wy, neighbors)
        interior = all(neighbors.values())
        roll = seed % 100
        if roll < (30 if interior else 38):
            count = 0
        elif roll < (88 if interior else 86):
            count = 1
        else:
            count = 2
        placements: list[tuple[int, int, int, str, int]] = []
        if self.terrain_seed(wx, wy, "f", 151) % 100 < (9 if interior else 15):
            local = self.terrain_seed(wx, wy, "f", 157)
            asset = FOREST_FEATURE_DECORATIONS[(local >> 8) % len(FOREST_FEATURE_DECORATIONS)]
            size = int(round(TILE * (0.86 + ((local >> 13) % 3) * 0.09)))
            ox = int(TILE * 0.29) + ((local >> 18) % max(1, int(TILE * 0.44)))
            oy = int(TILE * 0.44) + ((local >> 23) % max(1, int(TILE * 0.31)))
            half = size // 2
            if not neighbors["left"]:
                ox = max(min(TILE - 1, half + 2), ox)
            if not neighbors["right"]:
                ox = min(max(0, TILE - half - 2), ox)
            if not neighbors["up"]:
                oy = max(min(TILE - 1, half + 4), oy)
            if not neighbors["down"]:
                oy = min(max(0, TILE - half - 4), oy)
            placements.append((oy, ox, size, asset, (local >> 5) % 3))
        for i in range(count):
            local = self.terrain_seed(wx, wy, "f", 131 + i * 17)
            asset = FOREST_TREE_DECORATIONS[(local >> 8) % len(FOREST_TREE_DECORATIONS)]
            size = int(round(TILE * (1.20 + ((local >> 13) % 3) * 0.09)))
            if asset in {"deco_tree_oak", "deco_tree_round"}:
                size += int(round(TILE * 0.14))
            ox = int(TILE * 0.24) + ((local >> 18) % max(1, int(TILE * 0.56)))
            oy = int(TILE * 0.27) + ((local >> 24) % max(1, int(TILE * 0.42)))
            if i == 0:
                ox = int(TILE * 0.22) + ((local >> 18) % max(1, int(TILE * 0.31)))
                oy = int(TILE * 0.27) + ((local >> 24) % max(1, int(TILE * 0.29)))
            elif i == 1:
                ox = int(TILE * 0.58) + ((local >> 18) % max(1, int(TILE * 0.25)))
                oy = int(TILE * 0.39) + ((local >> 24) % max(1, int(TILE * 0.29)))
            half = size // 2
            if not neighbors["left"]:
                ox = max(min(TILE - 1, half - 2), ox)
            if not neighbors["right"]:
                ox = min(max(0, TILE - half + 2), ox)
            if not neighbors["up"]:
                oy = max(min(TILE - 1, half + 2), oy)
            if not neighbors["down"]:
                oy = min(max(0, TILE - half - 1), oy)
            placements.append((oy, ox, size, asset, (local >> 5) % 3))
        for oy, ox, size, asset, variant in sorted(placements):
            self.canvas.create_image(px + ox, py + oy, image=self.assets.get(asset, size, variant=variant))

    def draw_forest_floor_mesh(
        self,
        px: int,
        py: int,
        wx: int,
        wy: int,
        neighbors: dict[str, bool],
    ) -> None:
        if self.terrain_seed(wx, wy, "f", 197) % 100 >= 18:
            return
        for i in range(1):
            local = self.terrain_seed(wx, wy, "f", 211 + i * 23)
            asset = FOREST_FLOOR_DECORATIONS[(local >> 7) % len(FOREST_FLOOR_DECORATIONS)]
            size = int(round(TILE * (0.52 + ((local >> 12) % 3) * 0.07)))
            ox = int(TILE * 0.17) + ((local >> 17) % max(1, int(TILE * 0.71)))
            oy = int(TILE * 0.38) + ((local >> 23) % max(1, int(TILE * 0.50)))
            half = size // 2
            if not neighbors["left"]:
                ox = max(half + 2, ox)
            if not neighbors["right"]:
                ox = min(TILE - half - 2, ox)
            if not neighbors["up"]:
                oy = max(half + 4, oy)
            if not neighbors["down"]:
                oy = min(TILE - half - 4, oy)
            self.canvas.create_image(px + ox, py + oy, image=self.assets.get(asset, size, variant=(local >> 4) % 3))

    def draw_forest_liveliness(self) -> None:
        if self.current_map_id != OVERWORLD_ID:
            return
        for sy in range(MAP_ROWS + 1):
            wy = self.camera_y + sy
            for sx in range(MAP_COLS + 1):
                wx = self.camera_x + sx
                if tile_at(self.current_map_id, wx, wy) != "f":
                    continue
                self.draw_forest_tile_liveliness(sx, sy, wx, wy)

    def draw_forest_tile_liveliness(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        if self.terrain_seed(wx, wy, "f", 307) % 100 >= 18:
            return
        for i in range(1):
            seed = self.terrain_seed(wx, wy, "f", 337 + i * 19)
            if (seed + self.frame // 12) % 3 == 0:
                continue
            sway = math.sin((self.frame + wx * 5 + wy * 3 + i * 17) / 16)
            x = px + 7 + ((seed >> 8) % 34) + int(sway * 3)
            y = py + 10 + ((seed >> 16) % 26)
            if tile_at(self.current_map_id, wx - 1, wy) != "f":
                x = max(px + 8, x)
            if tile_at(self.current_map_id, wx + 1, wy) != "f":
                x = min(px + TILE - 10, x)
            if tile_at(self.current_map_id, wx, wy - 1) != "f":
                y = max(py + 10, y)
            if tile_at(self.current_map_id, wx, wy + 1) != "f":
                y = min(py + TILE - 10, y)
            color = "#8dbf5a" if i == 0 else "#4f9a49"
            self.canvas.create_line(
                x - 5,
                y,
                x - 1,
                y - 2,
                x + 5,
                y,
                fill=color,
                width=1,
                smooth=True,
                stipple="gray75",
            )

    def draw_cloud_layer(self) -> None:
        if self.current_map_id != OVERWORLD_ID:
            return
        viewport_w = MAP_COLS * TILE
        viewport_h = MAP_ROWS * TILE
        camera_px = self.camera_view_x * TILE
        camera_py = self.camera_view_y * TILE
        world_cols, world_rows = world_size(self.current_map_id)
        world_w = world_cols * TILE
        world_h = world_rows * TILE
        cell = 840
        min_cell_x = int((camera_px - 360) // cell) - 1
        max_cell_x = int((camera_px + viewport_w + 360) // cell) + 1
        min_cell_y = int((camera_py - 280) // cell) - 1
        max_cell_y = int((camera_py + viewport_h + 280) // cell) + 1
        for cy in range(min_cell_y, max_cell_y + 1):
            for cx in range(min_cell_x, max_cell_x + 1):
                seed = ((cx * 92837111) ^ (cy * 689287499) ^ 0xC10D5) & 0xFFFFFFFF
                anchor_x = (cx * cell + 120 + (seed % 560)) % world_w
                anchor_y = (cy * cell + 80 + ((seed >> 9) % 560)) % world_h
                sprite_name = f"cloud_billow_{(seed >> 18) % 3}"
                sprite = self.assets.get_native(sprite_name)
                if sprite is None:
                    continue
                width = sprite.width()
                height = sprite.height()
                drift_x = 0.10 + ((seed >> 21) % 7) * 0.018
                drift_y = 0.012 + ((seed >> 25) % 5) * 0.006
                orbit_x = math.sin((self.frame + (seed & 127)) / 260) * 34
                orbit_y = math.sin((self.frame + ((seed >> 7) & 127)) / 340) * 18
                cloud_x = (anchor_x + self.frame * drift_x + orbit_x) % world_w
                cloud_y = (anchor_y + self.frame * drift_y + orbit_y) % world_h
                for copy_x in (cloud_x, cloud_x - world_w, cloud_x + world_w):
                    for copy_y in (cloud_y, cloud_y - world_h, cloud_y + world_h):
                        screen_x = copy_x - camera_px
                        screen_y = copy_y - camera_py
                        if -width - 140 <= screen_x <= viewport_w + 140 and -height - 160 <= screen_y <= viewport_h + 120:
                            self.draw_cloud_shadow(screen_x + 34.0, screen_y + height - 22.0, width - 52, height)
                            self.canvas.create_image(screen_x, screen_y, anchor="nw", image=sprite)

    def draw_cloud_shadow(self, x: float, y: float, width: int, height: int) -> None:
        shadow = "#3f6659"
        self.canvas.create_oval(x, y, x + width * 55 // 100, y + height * 20 // 100, fill=shadow, outline="", stipple="gray12")
        self.canvas.create_oval(x + width * 28 // 100, y - 4, x + width, y + height * 22 // 100, fill=shadow, outline="", stipple="gray12")
        self.canvas.create_oval(x + width * 15 // 100, y + 5, x + width * 82 // 100, y + height * 30 // 100, fill=shadow, outline="", stipple="gray25")

    def draw_steppe_tile(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        seed = ((wx * 73856093) ^ (wy * 19349663)) & 0xFF
        base = "#cfa864" if seed % 3 == 0 else ("#d9b873" if seed % 3 == 1 else "#c49a58")
        self.canvas.create_rectangle(px - 1, py - 1, px + TILE + 1, py + TILE + 1, fill=base, outline="")

        dune = "#e3c985"
        shadow = "#b9874f"
        for i in range(3):
            y = py + 10 + i * 13 + ((seed + i * 7) % 5) - 2
            phase = (seed + i * 19) % 14
            self.canvas.create_line(px + 4, y, px + 14 + phase, y - 3, px + 28 + phase, y + 2, px + TILE - 5, y - 1, fill=dune, width=1, smooth=True)
            if i < 2:
                self.canvas.create_line(px + 8, y + 5, px + 20 + phase, y + 7, px + 36, y + 4, fill=shadow, width=1, stipple="gray50", smooth=True)

        for i in range(3):
            x = px + 8 + ((seed + i * 13) % 32)
            y = py + 8 + ((seed * 3 + i * 11) % 32)
            color = "#a77946" if i % 2 else "#e8cf91"
            self.canvas.create_rectangle(x, y, x + 2, y + 1, fill=color, outline="")

    def draw_coastline_overlay(self, sx: int, sy: int, wx: int, wy: int) -> None:
        if tile_at(self.current_map_id, wx, wy) != "w":
            return
        px = sx * TILE
        py = sy * TILE
        sand = "#d7b577"
        foam = "#8ee6ff"
        neighbors = {
            "up": tile_at(self.current_map_id, wx, wy - 1),
            "down": tile_at(self.current_map_id, wx, wy + 1),
            "left": tile_at(self.current_map_id, wx - 1, wy),
            "right": tile_at(self.current_map_id, wx + 1, wy),
        }
        for side, nt in neighbors.items():
            if nt == "w":
                continue
            shore_tile = "s" if nt in {"s", "r", "c"} or nt not in BLEND_COLORS else nt
            if side == "up":
                self.draw_terrain_edge(px, py, wx, wy, "up", shore_tile, 8)
                self.canvas.create_line(px + 3, py + 10, px + TILE - 4, py + 9, fill=foam, width=1, smooth=True)
            elif side == "down":
                self.draw_terrain_edge(px, py, wx, wy, "down", shore_tile, 8)
                self.canvas.create_line(px + 3, py + TILE - 10, px + TILE - 4, py + TILE - 9, fill=foam, width=1, smooth=True)
            elif side == "left":
                self.draw_terrain_edge(px, py, wx, wy, "left", shore_tile, 8)
                self.canvas.create_line(px + 10, py + 3, px + 9, py + TILE - 4, fill=foam, width=1, smooth=True)
            elif side == "right":
                self.draw_terrain_edge(px, py, wx, wy, "right", shore_tile, 8)
                self.canvas.create_line(px + TILE - 10, py + 3, px + TILE - 9, py + TILE - 4, fill=foam, width=1, smooth=True)

    def draw_city_ground_tile(self, sx: int, sy: int, wx: int, wy: int, tile: str) -> None:
        px = sx * TILE
        py = sy * TILE
        stone_tiles = {"p", "j", "l", "r", "h", "a", "t", "d"}
        if tile in stone_tiles:
            if tile == "r":
                base = "#7d8586"
                mortar = "#697172"
                highlight = "#aab2af"
            elif tile == "l":
                base = "#747d79"
                mortar = "#606966"
                highlight = "#9fa8a2"
            elif tile == "j":
                base = "#918b7f"
                mortar = "#746f65"
                highlight = "#bcb4a4"
            elif tile == "h":
                base = "#878d8a"
                mortar = "#717875"
                highlight = "#b6b9b0"
            elif tile == "a":
                base = "#8d9188"
                mortar = "#747a73"
                highlight = "#b9b8ac"
            else:
                base = "#929894"
                mortar = "#78807d"
                highlight = "#c0c2ba"
            self.canvas.create_rectangle(px, py, px + TILE, py + TILE, fill=base, outline="")

            block_h = max(12, int(TILE * 0.30))
            block_w = max(15, int(TILE * 0.36))
            world_y0 = wy * TILE
            first_y = (world_y0 // block_h) * block_h
            for gy in range(first_y, world_y0 + TILE + block_h, block_h):
                local_y = gy - world_y0
                y = py + local_y
                if 0 <= local_y <= TILE:
                    self.canvas.create_line(px, y, px + TILE, y, fill=mortar, width=1, stipple="gray50")
                row = gy // block_h
                offset = (block_w // 2) if row & 1 else 0
                world_x0 = wx * TILE
                first_x = ((world_x0 - offset) // block_w) * block_w + offset
                for gx in range(first_x, world_x0 + TILE + block_w, block_w):
                    local_x = gx - world_x0
                    x = px + local_x
                    top = max(py, y)
                    bottom = min(py + TILE, y + block_h)
                    if 0 <= local_x <= TILE and bottom > top:
                        self.canvas.create_line(x, top, x, bottom, fill=mortar, width=1, stipple="gray25")

            seed = self.terrain_seed(wx, wy, tile, 509)
            if seed % 7 == 0:
                self.canvas.create_line(px + 5, py + 9 + (seed % max(1, TILE - 18)), px + TILE - 6, py + 10 + (seed % max(1, TILE - 18)), fill=highlight, width=1, stipple="gray75")
        elif tile == "y":
            self.canvas.create_rectangle(px, py, px + TILE, py + TILE, fill="#6d985d", outline="")
            self.canvas.create_rectangle(px, py, px + TILE, py + TILE, fill="#94aa65", outline="", stipple="gray50")
        elif tile == "w":
            self.canvas.create_image(px, py, anchor="nw", image=self.assets.get("water", TILE + 2))
        elif tile == "x":
            self.canvas.create_rectangle(px, py, px + TILE, py + TILE, fill="#59646b", outline="")
        elif tile == "m":
            self.canvas.create_rectangle(px, py, px + TILE, py + TILE, fill="#242832", outline="")
        else:
            self.canvas.create_rectangle(px, py, px + TILE, py + TILE, fill=self.map_tile_color(tile), outline="")

        self.draw_city_ground_blend(px, py, wx, wy, tile)
        if tile in {"p", "j"}:
            self.draw_city_street_prop(sx, sy, wx, wy, tile)

    def draw_city_ground_blend(self, px: int, py: int, wx: int, wy: int, tile: str) -> None:
        stone_tiles = {"p", "j", "l", "r", "h", "a", "t", "d"}

        def kind(value: str) -> str:
            if value in stone_tiles:
                return "street" if value in {"r", "l"} else "stone"
            if value == "y":
                return "garden"
            if value == "w":
                return "water"
            if value == "x":
                return "wall"
            return "outside"

        own = kind(tile)
        neighbors = (
            ("up", tile_at(self.current_map_id, wx, wy - 1)),
            ("down", tile_at(self.current_map_id, wx, wy + 1)),
            ("left", tile_at(self.current_map_id, wx - 1, wy)),
            ("right", tile_at(self.current_map_id, wx + 1, wy)),
        )

        for side, neighbor in neighbors:
            other = kind(neighbor)
            if other == own:
                continue
            if {own, other} <= {"stone", "street"}:
                color = "#aeb5ae" if own == "street" else "#747c7d"
                width = max(2, int(TILE * 0.05))
            elif "garden" in {own, other}:
                color = "#839b66" if own == "garden" else "#778a62"
                width = max(4, int(TILE * 0.10))
            elif "water" in {own, other}:
                color = "#7fc9dd"
                width = max(3, int(TILE * 0.07))
            else:
                color = "#454d52"
                width = max(3, int(TILE * 0.07))

            if side == "up":
                self.canvas.create_rectangle(px, py, px + TILE, py + width, fill=color, outline="", stipple="gray50")
            elif side == "down":
                self.canvas.create_rectangle(px, py + TILE - width, px + TILE, py + TILE, fill=color, outline="", stipple="gray50")
            elif side == "left":
                self.canvas.create_rectangle(px, py, px + width, py + TILE, fill=color, outline="", stipple="gray50")
            elif side == "right":
                self.canvas.create_rectangle(px + TILE - width, py, px + TILE, py + TILE, fill=color, outline="", stipple="gray50")

    def draw_city_street_prop(self, sx: int, sy: int, wx: int, wy: int, tile: str) -> None:
        if not self.current_map_id.startswith("city_") or tile not in {"p", "j", "a", "y"}:
            return

        above_building = city_building_at(self.current_map_id, wx, wy - 1)
        if above_building and wy - 1 == above_building.y2:
            return

        nearby_building = above_building is not None or any(
            city_building_at(self.current_map_id, wx + dx, wy + dy) is not None
            for dx, dy in ((-1, 0), (1, 0), (0, 1))
        )
        seed = self.terrain_seed(wx, wy, tile, 911)
        roll = seed % 100
        route_neighbors = sum(
            1
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1))
            if tile_at(self.current_map_id, wx + dx, wy + dy) in {"r", "l"}
        )
        if tile in {"p", "j"} and route_neighbors >= 2:
            return
        if tile == "a":
            if roll >= 44:
                return
        elif tile == "y":
            if roll >= 13:
                return
        elif tile == "j":
            if not nearby_building or roll >= 20:
                return
        elif nearby_building:
            if roll >= 9:
                return
        else:
            return

        if tile == "a":
            choices = (
                ("city_prop_market_red", 62),
                ("city_prop_market_green", 62),
                ("city_prop_market_yellow", 60),
                ("city_prop_cart", 66),
                ("city_prop_crate", 43),
                ("city_prop_barrel_stack", 48),
            )
        elif tile == "y":
            choices = (
                ("city_prop_fountain_small", 62),
                ("city_prop_plant_box", 47),
                ("city_prop_flower_pot", 42),
            )
        elif tile == "j" or above_building:
            choices = (
                ("city_prop_street_lamp", 70),
                ("city_prop_banner_red", 66),
                ("city_prop_banner_blue", 66),
                ("city_prop_barrel_stack", 48),
                ("city_prop_crate", 43),
                ("city_prop_plant_box", 46),
            )
        else:
            choices = (
                ("city_prop_barrel", 46),
                ("city_prop_crate", 42),
                ("city_prop_flower_pot", 40),
                ("city_prop_plant_box", 44),
            )

        name, target_h = choices[(seed >> 7) % len(choices)]
        native = self.assets.get_native(name)
        if native is None or native.height() <= 0:
            return
        ratio = native.width() / native.height()
        prop_h = max(16, int(target_h * (0.88 + ((seed >> 14) & 7) * 0.025)))
        prop_w = max(10, int(prop_h * ratio))
        px = sx * TILE
        py = sy * TILE
        jitter_x = ((seed >> 3) & 15) - 7
        jitter_y = ((seed >> 11) & 7) - 3
        x = px + (TILE - prop_w) // 2 + jitter_x
        y = py + TILE - prop_h - 4 + jitter_y
        if prop_w <= TILE - 4:
            x = max(px + 2, min(px + TILE - prop_w - 2, x))
        if prop_h <= TILE - 2:
            y = max(py + 1, min(py + TILE - prop_h - 2, y))
        self.canvas.create_oval(
            x + max(2, prop_w // 8),
            y + prop_h - max(7, prop_h // 6),
            x + prop_w - max(2, prop_w // 8),
            y + prop_h - 1,
            fill="#182026",
            outline="",
            stipple="gray50",
        )
        self.canvas.create_image(x, y, anchor="nw", image=self.assets.get_fit(name, prop_w, prop_h, variant=(seed >> 19) % 3))

    def draw_city_plaza(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        seed = self.terrain_seed(wx, wy, "p", 43)
        base_asset = "city_plaza_round" if seed % 11 == 0 else "city_cobble"
        self.canvas.create_image(px, py, anchor="nw", image=self.assets.get(base_asset, TILE))
        if seed % 6 == 0:
            self.canvas.create_rectangle(px + 8, py + 32, px + 23, py + 36, fill="#7d6241", outline="#5a452e")
            self.canvas.create_oval(px + 30, py + 29, px + 38, py + 37, fill="#6a8d55", outline="")
        elif seed % 6 == 1:
            self.canvas.create_line(px + 12, py + 14, px + 30, py + 8, fill="#d1c6aa", width=1)
            self.canvas.create_line(px + 18, py + 24, px + 39, py + 18, fill="#8d836e", width=1)
        elif seed % 6 == 2:
            self.canvas.create_oval(px + 17, py + 16, px + 31, py + 30, fill="#8a806e", outline="#6e6658")
            self.canvas.create_oval(px + 21, py + 20, px + 27, py + 26, fill="#a8d7e4", outline="")
        if (seed >> 2) % 4 == 0:
            self.canvas.create_line(px + 6, py + TILE // 2, px + TILE - 6, py + TILE // 2, fill="#737a82", width=1, stipple="gray75")

    def draw_house_floor_tile(self, sx: int, sy: int, wx: int, wy: int, tile: str) -> None:
        px = sx * TILE
        py = sy * TILE
        seed = self.terrain_seed(wx, wy, tile, 71)
        base = "#8b6542" if (wx + wy) % 2 == 0 else "#7c593a"
        self.canvas.create_rectangle(px, py, px + TILE, py + TILE, fill=base, outline="")
        for y in range(py + 7, py + TILE, 10):
            self.canvas.create_line(px, y, px + TILE, y + ((seed >> (y & 7)) & 1), fill="#5f3f2a", width=1)
        for x in range(px + 8, px + TILE, 16):
            self.canvas.create_line(x, py + 2, x, py + TILE - 2, fill="#a47a50", width=1, stipple="gray75")
        if tile == "z":
            rug = "#9b4d44" if seed % 2 else "#3f6b76"
            self.canvas.create_rectangle(px + 4, py + 7, px + TILE - 4, py + TILE - 7, fill=rug, outline="#5a2e2e", width=2)
            self.canvas.create_rectangle(px + 9, py + 13, px + TILE - 9, py + TILE - 13, outline="#d0b46e", width=1)
        elif tile == "e":
            self.canvas.create_rectangle(px + 8, py + 10, px + TILE - 8, py + TILE - 4, fill="#51331f", outline="#24140d")
            self.canvas.create_line(px + 12, py + 15, px + TILE - 12, py + 15, fill="#d6bd72", width=1)
            self.canvas.create_oval(px + TILE - 17, py + 29, px + TILE - 13, py + 33, fill="#d6bd72", outline="")

    def draw_house_wall_tile(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        seed = self.terrain_seed(wx, wy, "o", 79)
        self.canvas.create_rectangle(px, py, px + TILE, py + TILE, fill="#6f5740", outline="")
        self.canvas.create_rectangle(px + 2, py + 2, px + TILE - 2, py + TILE - 2, fill="#8d7450", outline="#4f3828")
        for y in range(py + 9, py + TILE, 13):
            self.canvas.create_line(px + 4, y, px + TILE - 4, y, fill="#b29a6d", width=1)
        if seed % 5 == 0:
            self.canvas.create_rectangle(px + 17, py + 15, px + 31, py + 28, fill="#4e3424", outline="#2e1e16")
            self.canvas.create_rectangle(px + 19, py + 17, px + 29, py + 26, fill="#d19a43", outline="")
        elif seed % 5 == 1:
            self.canvas.create_rectangle(px + 10, py + 11, px + 38, py + 16, fill="#5a3822", outline="#2e1e16")
            for x in range(px + 13, px + 36, 7):
                self.canvas.create_rectangle(x, py + 17, x + 3, py + 28, fill="#b88a4f", outline="")

    def draw_house_furniture_tile(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        seed = self.terrain_seed(wx, wy, "k", 83)
        self.draw_house_floor_tile(sx, sy, wx, wy, "i")
        kind = seed % 4
        if kind == 0:
            self.canvas.create_rectangle(px + 7, py + 12, px + 39, py + 33, fill="#6d4329", outline="#372116")
            self.canvas.create_rectangle(px + 11, py + 15, px + 35, py + 21, fill="#d9c185", outline="")
            self.canvas.create_rectangle(px + 11, py + 22, px + 35, py + 30, fill="#8f4d3f", outline="")
        elif kind == 1:
            self.canvas.create_oval(px + 12, py + 13, px + 36, py + 37, fill="#7b4d2d", outline="#372116")
            self.canvas.create_oval(px + 17, py + 18, px + 31, py + 32, fill="#b78b51", outline="")
        elif kind == 2:
            self.canvas.create_rectangle(px + 9, py + 8, px + 38, py + 39, fill="#5b3a27", outline="#2c1b12")
            for y in range(py + 13, py + 36, 6):
                self.canvas.create_line(px + 11, y, px + 36, y, fill="#a27848", width=1)
            for x in range(px + 13, px + 35, 7):
                self.canvas.create_rectangle(x, py + 15, x + 3, py + 34, fill="#c99d5e", outline="")
        else:
            self.canvas.create_rectangle(px + 8, py + 18, px + 40, py + 34, fill="#74452b", outline="#2c1b12")
            self.canvas.create_rectangle(px + 13, py + 11, px + 35, py + 21, fill="#5b3a27", outline="#2c1b12")
            self.canvas.create_oval(px + 18, py + 20, px + 24, py + 26, fill="#6a8d55", outline="")

    def draw_city_feature_tile(self, sx: int, sy: int, wx: int, wy: int, tile: str) -> None:
        if tile == "a":
            if not self.current_map_id.startswith("city_"):
                self.draw_city_plaza(sx, sy, wx, wy)
            self.draw_market_stalls(sx, sy, wx, wy)
            self.draw_city_street_prop(sx, sy, wx, wy, tile)
        elif tile == "y":
            self.draw_garden_court(sx, sy, wx, wy)
            self.draw_city_street_prop(sx, sy, wx, wy, tile)
        elif tile == "t":
            self.draw_city_tower(sx, sy, wx, wy)

    def draw_market_stalls(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        palette = ("#a53e35", "#2f6f91", "#d0a13d", "#4c7c51")
        for i, (ox, oy) in enumerate(((7, 9), (26, 11), (10, 29))):
            seed = self.terrain_seed(wx, wy, "a", 17 + i)
            cloth = palette[seed % len(palette)]
            self.canvas.create_rectangle(px + ox, py + oy + 8, px + ox + 15, py + oy + 15, fill="#6c4a2f", outline="#3c281b")
            self.canvas.create_polygon(px + ox - 2, py + oy + 8, px + ox + 7, py + oy, px + ox + 17, py + oy + 8, fill=cloth, outline="#4d3026")
            for stripe in range(0, 15, 5):
                self.canvas.create_line(px + ox + stripe, py + oy + 4, px + ox + stripe + 2, py + oy + 8, fill="#f0d88d", width=1)
        self.canvas.create_rectangle(px + 28, py + 31, px + 38, py + 35, fill="#8b6336", outline="#51391f")
        self.canvas.create_oval(px + 31, py + 24, px + 37, py + 31, fill="#d4b44f", outline="")

    def draw_garden_court(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        if self.current_map_id.startswith("city_"):
            up = tile_at(self.current_map_id, wx, wy - 1) == "y"
            down = tile_at(self.current_map_id, wx, wy + 1) == "y"
            left = tile_at(self.current_map_id, wx - 1, wy) == "y"
            right = tile_at(self.current_map_id, wx + 1, wy) == "y"
            border = "#b7ad82"
            if not up:
                self.canvas.create_rectangle(px + 2, py + 2, px + TILE - 2, py + 5, fill=border, outline="")
            if not down:
                self.canvas.create_rectangle(px + 2, py + TILE - 5, px + TILE - 2, py + TILE - 2, fill=border, outline="")
            if not left:
                self.canvas.create_rectangle(px + 2, py + 2, px + 5, py + TILE - 2, fill=border, outline="")
            if not right:
                self.canvas.create_rectangle(px + TILE - 5, py + 2, px + TILE - 2, py + TILE - 2, fill=border, outline="")
        else:
            self.canvas.create_rectangle(px, py, px + TILE, py + TILE, fill="#7aaa62", outline="")
            self.canvas.create_rectangle(px + 3, py + 3, px + TILE - 3, py + TILE - 3, outline="#a99b78", width=2)
            self.canvas.create_line(px + 4, py + TILE // 2, px + TILE - 4, py + TILE // 2, fill="#c8b889", width=5)
            self.canvas.create_line(px + TILE // 2, py + 4, px + TILE // 2, py + TILE - 4, fill="#c8b889", width=5)
            self.canvas.create_oval(px + 17, py + 17, px + 31, py + 31, fill="#6a8d55", outline="#496b3c")
        for i in range(8):
            ox = 6 + ((wx * 7 + wy * 11 + i * 13) % 36)
            oy = 6 + ((wx * 5 + wy * 17 + i * 9) % 36)
            color = "#d86f85" if i % 2 else "#f0d36d"
            self.canvas.create_rectangle(px + ox, py + oy, px + ox + 2, py + oy + 2, fill=color, outline="")

    def draw_city_tower(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        stone = "#928b7b"
        dark = "#5c554b"
        light = "#bdb39d"
        self.canvas.create_rectangle(px, py, px + TILE, py + TILE, fill="#8d8474", outline="")
        self.canvas.create_oval(px + 7, py + 5, px + TILE - 7, py + TILE - 4, fill=dark, outline="")
        self.canvas.create_oval(px + 9, py + 3, px + TILE - 9, py + TILE - 8, fill=stone, outline=dark, width=2)
        for yy in range(py + 12, py + 39, 9):
            self.canvas.create_line(px + 12, yy, px + TILE - 12, yy, fill=light, width=1)
        self.canvas.create_rectangle(px + 20, py + 28, px + 28, py + 41, fill="#3e2a20", outline="#241711")
        self.canvas.create_arc(px + 20, py + 20, px + 28, py + 34, start=0, extent=180, fill="#3e2a20", outline="#241711")
        self.canvas.create_rectangle(px + 11, py + 12, px + 17, py + 18, fill="#2f2824", outline="#1b1613")
        self.canvas.create_rectangle(px + 31, py + 12, px + 37, py + 18, fill="#2f2824", outline="#1b1613")

    def city_building_visual_block(self, building: CityBuilding) -> tuple[CityBuilding, ...]:
        row = sorted(
            (
                candidate
                for candidate in city_buildings_for_map(self.current_map_id)
                if candidate.y2 == building.y2 and abs(candidate.y1 - building.y1) <= 2
            ),
            key=lambda candidate: (candidate.x1, candidate.x2),
        )
        try:
            index = row.index(building)
        except ValueError:
            return (building,)

        left = index
        while left > 0 and row[left].x1 - row[left - 1].x2 <= 2:
            left -= 1
        right = index
        while right + 1 < len(row) and row[right + 1].x1 - row[right].x2 <= 2:
            right += 1
        return tuple(row[left : right + 1])

    def draw_city_building_entity(self, sx: int, sy: int, wx: int, wy: int, building: CityBuilding) -> None:
        key = (self.current_map_id, building.key)
        if key in self._drawn_city_buildings:
            return

        block = self.city_building_visual_block(building)
        for member in block:
            self._drawn_city_buildings.add((self.current_map_id, member.key))

        camera_x = wx - sx
        camera_y = wy - sy
        block_x1 = min(member.x1 for member in block)
        block_x2 = max(member.x2 for member in block)
        front_y = building.y2
        street_y = int((front_y - camera_y + 1) * TILE)
        block_left = int((block_x1 - camera_x) * TILE)
        block_right = int((block_x2 + 1 - camera_x) * TILE)
        foundation_left = block_left + max(5, int(TILE * 0.10))
        foundation_right = block_right - max(5, int(TILE * 0.10))
        self.canvas.create_rectangle(foundation_left, street_y - 10, foundation_right, street_y - 3, fill="#2d3637", outline="")
        self.canvas.create_line(foundation_left, street_y - 11, foundation_right, street_y - 11, fill="#737a74", width=1)
        if len(block) > 1:
            for prev, nxt in zip(block, block[1:]):
                gap_left = int((prev.x2 + 1 - camera_x) * TILE)
                gap_right = int((nxt.x1 - camera_x) * TILE)
                if gap_right <= gap_left:
                    continue
                self.canvas.create_rectangle(
                    gap_left + 3,
                    street_y - int(TILE * 1.62),
                    gap_right - 3,
                    street_y - int(TILE * 1.48),
                    fill="#7f3f28",
                    outline="#4f2b22",
                )
                self.canvas.create_rectangle(gap_left + 4, street_y - 10, gap_right - 4, street_y - 3, fill="#30383a", outline="")

        def ratio_for(name: str) -> float:
            native = self.assets.get_native(name)
            if native is None or native.height() <= 0:
                return 1.0
            return native.width() / native.height()

        def draw_sprite(name: str, center_x: float, front_y: int, target_h: int, variant: int = 0) -> tuple[int, int, int, int]:
            ratio = ratio_for(name)
            sprite_h = max(int(TILE * 1.15), target_h)
            sprite_w = max(int(TILE * 0.95), int(sprite_h * ratio))
            sprite_x = int(center_x - sprite_w / 2)
            sprite_y = front_y - sprite_h
            shadow_w = max(int(TILE * 0.90), int(sprite_w * 0.78))
            shadow_h = max(8, int(TILE * 0.18))
            self.canvas.create_oval(
                int(center_x - shadow_w / 2),
                front_y - shadow_h,
                int(center_x + shadow_w / 2),
                front_y + shadow_h // 2,
                fill="#111820",
                outline="",
                stipple="gray50",
            )
            self.canvas.create_image(
                sprite_x,
                sprite_y,
                anchor="nw",
                image=self.assets.get_fit(name, sprite_w, sprite_h, variant=variant),
            )
            return sprite_x, sprite_y, sprite_w, sprite_h

        def draw_member(member: CityBuilding) -> None:
            px = int((member.x1 - camera_x) * TILE)
            py = int((member.y1 - camera_y) * TILE)
            lot_w = member.width * TILE
            lot_h = member.depth * TILE
            parcel_street_y = py + lot_h
            seed = self.terrain_seed(member.x1 + member.x2, member.y1 + member.y2, "h", 179 + member.palette * 17)
            civic = member.style in {"hall", "guild", "barracks", "warehouse"}

            if civic:
                civic_sprites = {
                    "hall": ("city_building_stone_hall", "city_building_stone_shop"),
                    "guild": ("city_building_stone_shop", "city_building_stone_hall"),
                    "barracks": ("city_building_stone_tower", "city_building_stone_shop"),
                    "warehouse": ("city_building_stone_shop", "city_building_town_shop"),
                }.get(member.style, ("city_building_stone_hall",))
                module_count = max(1, min(4, (member.width + 2) // 3))
                if len(block) > 1 and member.width >= 5:
                    module_count = min(4, module_count + 1)
                target_h = max(int(TILE * 1.82), min(int(TILE * 2.30), lot_h + int(TILE * 0.42)))
                spacing = lot_w / max(1, module_count)
                for index in range(module_count):
                    name = civic_sprites[(seed + member.palette * 5 + index * 2) % len(civic_sprites)]
                    module_h = target_h - (index % 2) * max(0, int(TILE * 0.06))
                    ratio = ratio_for(name)
                    max_module_w = spacing + int(TILE * 0.42)
                    if module_h * ratio > max_module_w:
                        module_h = max(int(TILE * 1.58), int(max_module_w / max(0.1, ratio)))
                    jitter = ((seed >> (index * 5)) & 5) - 2
                    center_x = px + spacing * (index + 0.5) + jitter
                    draw_sprite(name, center_x, parcel_street_y - 3, module_h, member.palette + index)
            else:
                sprite_sets = {
                    "shop": ("city_building_town_shop", "city_building_town_gabled", "city_building_town_small"),
                    "inn": ("city_building_town_gabled", "city_building_town_shop", "city_building_town_tall"),
                    "row": (
                        "city_building_town_narrow",
                        "city_building_town_gabled",
                        "city_building_town_tall",
                        "city_building_town_small",
                        "city_building_town_shop",
                    ),
                    "house": ("city_building_town_gabled", "city_building_town_small", "city_building_town_tall"),
                }
                sprites = sprite_sets.get(member.style, sprite_sets["house"])
                module_count = max(1, min(member.width, (member.width + 1) // 2))
                if len(block) > 1 and member.width >= 3:
                    module_count = min(member.width, module_count + 1)
                if member.style == "row" and member.width >= 6:
                    module_count = min(member.width, module_count + 1)
                target_h = max(int(TILE * 1.86), min(int(TILE * 2.26), lot_h + int(TILE * 0.48)))
                span_w = lot_w + int(TILE * 0.16)
                spacing = span_w / max(1, module_count)
                start = px + lot_w / 2 - span_w / 2 + spacing / 2
                for index in range(module_count):
                    sprite_name = sprites[(seed + member.palette + index * 3) % len(sprites)]
                    module_h = target_h - (index % 2) * max(0, int(TILE * 0.05))
                    ratio = ratio_for(sprite_name)
                    max_module_w = spacing + int(TILE * 0.28)
                    if module_h * ratio > max_module_w:
                        module_h = max(int(TILE * 1.55), int(max_module_w / max(0.1, ratio)))
                    jitter = ((seed >> (index * 4)) & 5) - 2
                    center_x = start + spacing * index + jitter
                    draw_sprite(sprite_name, center_x, parcel_street_y - 3, module_h, member.palette + index)

            for door_x, door_y in city_building_door_tiles(member):
                marker_x = int((door_x - camera_x) * TILE)
                marker_y = int((door_y - camera_y + 1) * TILE)
                self.canvas.create_rectangle(
                    marker_x + int(TILE * 0.23),
                    marker_y - 9,
                    marker_x + int(TILE * 0.77),
                    marker_y - 3,
                    fill="#2d241d",
                    outline="#7a6041",
                )
                self.canvas.create_line(
                    marker_x + int(TILE * 0.29),
                    marker_y - 2,
                    marker_x + int(TILE * 0.71),
                    marker_y - 2,
                    fill="#caa262",
                    width=2,
                )

        for member in block:
            draw_member(member)

        block_width = block_x2 - block_x1 + 1
        if block_width >= 4:
            lantern_h = max(15, int(TILE * 0.34))
            lantern_w = max(8, int(TILE * 0.17))
            lantern_xs = [block_left + max(2, int(TILE * 0.13)), block_right - lantern_w - max(2, int(TILE * 0.13))]
            for prev, nxt in zip(block, block[1:]):
                gap = nxt.x1 - prev.x2 - 1
                if 0 <= gap <= 1:
                    lantern_xs.append(int((prev.x2 + 1 - camera_x) * TILE + (gap + 1) * TILE / 2 - lantern_w / 2))
            for lx in dict.fromkeys(lantern_xs):
                self.canvas.create_image(
                    int(lx),
                    int(street_y - lantern_h - TILE * 0.30),
                    anchor="nw",
                    image=self.assets.get_fit("city_lantern", lantern_w, lantern_h),
                )

    def draw_city_house(self, sx: int, sy: int, wx: int, wy: int) -> None:
        if self.current_map_id.startswith(("city_", "village_")):
            building = city_building_at(self.current_map_id, wx, wy)
            if building is not None:
                self.draw_city_building_entity(sx, sy, wx, wy, building)
                return
            self.draw_modular_building_tile(sx, sy, wx, wy)
            return
        px = sx * TILE
        py = sy * TILE
        up = tile_at(self.current_map_id, wx, wy - 1) == "h"
        down = tile_at(self.current_map_id, wx, wy + 1) == "h"
        left = tile_at(self.current_map_id, wx - 1, wy) == "h"
        right = tile_at(self.current_map_id, wx + 1, wy) == "h"
        street_down = tile_at(self.current_map_id, wx, wy + 1) in {"r", "p"}
        street_left = tile_at(self.current_map_id, wx - 1, wy) in {"r", "p"}
        street_right = tile_at(self.current_map_id, wx + 1, wy) in {"r", "p"}

        roof = "#8b4b32" if (wx * 3 + wy) % 2 == 0 else "#75412f"
        roof_dark = "#5f2f26"
        roof_light = "#aa6840"
        wall = "#d7c499" if (wx + wy) % 2 == 0 else "#cfbd94"
        timber = "#704b2b"
        stone = "#8e8d83"

        self.canvas.create_rectangle(px, py, px + TILE, py + TILE, fill=wall, outline="")

        if not up:
            x1 = px - (8 if left else 0)
            x2 = px + TILE + (8 if right else 0)
            self.canvas.create_rectangle(x1, py + 2, x2, py + 19, fill=roof, outline=roof_dark)
            for k in range(x1 + 4, x2, 11):
                self.canvas.create_line(k, py + 3, k + 9, py + 18, fill=roof_light, width=1)
            self.canvas.create_line(x1, py + 20, x2, py + 20, fill=roof_dark, width=2)
            if (not left) or ((wx + wy) % 4 == 0 and right):
                self.canvas.create_polygon(px + 8, py + 20, px + TILE // 2, py - 6, px + TILE - 8, py + 20, fill="#9b5837", outline=roof_dark)
                self.canvas.create_rectangle(px + 20, py + 9, px + 29, py + 17, fill="#d6a24c", outline="#4d2d20")
        elif not down:
            self.canvas.create_rectangle(px, py, px + TILE, py + 5, fill="#a68b62", outline="")

        if not left:
            self.canvas.create_rectangle(px, py + 18, px + 5, py + TILE, fill=stone, outline="#67675f")
        if not right:
            self.canvas.create_rectangle(px + TILE - 5, py + 18, px + TILE, py + TILE, fill=stone, outline="#67675f")
        if not down:
            self.canvas.create_rectangle(px - (2 if left else 0), py + TILE - 6, px + TILE + (2 if right else 0), py + TILE, fill="#7d6f5a", outline="")

        if not left:
            self.canvas.create_line(px + 10, py + 22, px + 10, py + TILE - 8, fill=timber, width=2)
        if not right:
            self.canvas.create_line(px + TILE - 10, py + 22, px + TILE - 10, py + TILE - 8, fill=timber, width=2)
        if not up or (wy % 2 == 0):
            self.canvas.create_line(px + 6, py + 25, px + TILE - 6, py + 25, fill=timber, width=2)
        if not down or (wy % 2 == 1):
            self.canvas.create_line(px + 6, py + 39, px + TILE - 6, py + 39, fill=timber, width=2)
        if (wx + wy) % 3 == 0 and (not left or not right):
            self.canvas.create_line(px + 12, py + 25, px + TILE - 12, py + 39, fill=timber, width=2)

        facade = (street_down and not down) or (street_left and not left) or (street_right and not right)
        if facade and not down:
            door_x = px + (14 if (wx + wy) % 2 else 18)
            self.canvas.create_rectangle(door_x, py + 28, door_x + 14, py + TILE - 5, fill="#51331f", outline="#2f2018")
            self.canvas.create_arc(door_x, py + 20, door_x + 14, py + 34, start=0, extent=180, fill="#51331f", outline="#2f2018")
            self.canvas.create_rectangle(door_x + 2, py + 28, door_x + 12, py + 33, fill="#1f1712", outline="")
            self.canvas.create_oval(door_x + 10, py + 39, door_x + 13, py + 42, fill="#d6bd72", outline="")
        elif (wx * 7 + wy * 5) % 4 != 0:
            wx1 = px + 15 + ((wx + wy) % 2) * 4
            self.canvas.create_rectangle(wx1, py + 26, wx1 + 14, py + 35, fill="#5d3a22", outline="#2f2018")
            self.canvas.create_rectangle(wx1 + 2, py + 28, wx1 + 12, py + 33, fill="#d5a64e", outline="")

        if facade and (wx * 5 + wy) % 3 == 0:
            self.canvas.create_rectangle(px + 7, py + 22, px + 18, py + 28, fill="#7a382c", outline="#4b241f")
            self.canvas.create_line(px + 8, py + 29, px + 20, py + 29, fill="#d7bd7d", width=2)

    def building_row_bounds(self, wx: int, wy: int) -> tuple[int, int]:
        left = wx
        while tile_at(self.current_map_id, left - 1, wy) == "h":
            left -= 1
        right = wx
        while tile_at(self.current_map_id, right + 1, wy) == "h":
            right += 1
        return left, right

    def is_south_building_frontage(self, wx: int, wy: int) -> bool:
        if tile_at(self.current_map_id, wx, wy) != "h":
            return False
        down_is_building = tile_at(self.current_map_id, wx, wy + 1) == "h"
        front_is_walkable = is_passable(self.current_map_id, wx, wy + 1)
        return (not down_is_building) and front_is_walkable

    def building_frontage_span(self, wx: int, wy: int) -> tuple[int, int]:
        key = (self.current_map_id, wx, wy)
        cached = self._building_frontage_cache.get(key)
        if cached is not None:
            return cached
        left = wx
        while self.is_south_building_frontage(left - 1, wy):
            left -= 1
        right = wx
        while self.is_south_building_frontage(right + 1, wy):
            right += 1
        span = (left, right)
        for tx in range(left, right + 1):
            self._building_frontage_cache[(self.current_map_id, tx, wy)] = span
        return span

    def draw_modular_building_tile(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        topology_key = (self.current_map_id, wx, wy)
        topology = self._building_topology_cache.get(topology_key)
        if topology is None:
            topology = (
                tile_at(self.current_map_id, wx, wy - 1) == "h",
                tile_at(self.current_map_id, wx, wy + 1) == "h",
                tile_at(self.current_map_id, wx - 1, wy) == "h",
                tile_at(self.current_map_id, wx + 1, wy) == "h",
                tile_at(self.current_map_id, wx, wy + 1) in {"r", "p", "a", "y"},
                tile_at(self.current_map_id, wx - 1, wy) in {"r", "p", "a", "y"},
                tile_at(self.current_map_id, wx + 1, wy) in {"r", "p", "a", "y"},
            )
            self._building_topology_cache[topology_key] = topology
        up, down, left, right, street_down, street_left, street_right = topology

        seed = self.terrain_seed(wx, wy, "h", 59)
        facade_south = (not down) and street_down
        roof_edge = not up
        side_wall = not left or not right
        village = self.current_map_id.startswith("village_")
        snowy = self.current_map_id.startswith("village_snow")

        ground_asset = "city_snow_cobble" if snowy else "city_cobble"
        self.canvas.create_image(px, py, anchor="nw", image=self.assets.get_fit(ground_asset, TILE, TILE, variant=seed % 3))

        body_h = max(22, int(TILE * (0.60 if village else 0.64)))
        body_y = py + TILE - body_h
        body_asset = "city_wall_block" if not snowy else "city_snow_cobble"
        self.canvas.create_image(px, body_y, anchor="nw", image=self.assets.get_fit(body_asset, TILE, body_h, variant=(seed >> 2) % 3))

        if side_wall:
            side_w = max(7, int(TILE * 0.15))
            if not left:
                self.canvas.create_image(px, body_y, anchor="nw", image=self.assets.get_fit("city_wall_block", side_w, body_h, variant=(seed >> 4) % 3))
            if not right:
                self.canvas.create_image(px + TILE - side_w, body_y, anchor="nw", image=self.assets.get_fit("city_wall_block", side_w, body_h, variant=(seed >> 5) % 3))

        # Front facades for south-facing streets.
        if facade_south:
            front_w = max(28, int(TILE * 0.88))
            front_h = max(20, int(TILE * 0.52))
            fx = px + (TILE - front_w) // 2
            fy = py + TILE - front_h - max(1, int(TILE * 0.02))
            self.canvas.create_image(fx, fy, anchor="nw", image=self.assets.get_fit("city_wall_gate", front_w, front_h, variant=(seed >> 6) % 3))
            left_x, right_x = self.building_frontage_span(wx, wy)
            span_w = right_x - left_x + 1
            span_seed = self.terrain_seed((left_x + right_x) // 2, wy, "h", 131)
            step = 2 if village else 3
            offset = span_seed % step
            local_idx = wx - left_x
            anchor = (local_idx - offset) % step == 0
            edge_anchor = local_idx in {0, span_w - 1} and span_w <= step + 1
            if anchor or edge_anchor:
                house_native = self.assets.get_native("city_house_front")
                house_w = max(TILE + 24, int(TILE * (2.85 if village else 3.05)))
                if house_native is not None and house_native.height() > 0:
                    house_ratio = house_native.width() / house_native.height()
                    house_h = max(TILE + 24, int(house_w / max(0.1, house_ratio)))
                else:
                    house_h = max(TILE + 42, int(TILE * (2.70 if village else 2.85)))
                hx = px + TILE // 2 - house_w // 2
                hy = py + TILE - house_h - max(1, int(TILE * 0.03))
                self.canvas.create_image(hx, hy, anchor="nw", image=self.assets.get_fit("city_house_front", house_w, house_h, variant=(span_seed >> 5) % 3))
            elif (seed % 4) == 1:
                lantern_h = max(12, int(TILE * 0.30))
                lantern_w = max(7, int(TILE * 0.18))
                lx = px + (TILE // 2) + (8 if (wx & 1) else -14)
                ly = py + max(8, int(TILE * 0.18))
                self.canvas.create_image(lx, ly, anchor="nw", image=self.assets.get_fit("city_lantern", lantern_w, lantern_h))

        # Roof line on top-facing edges to make block silhouettes coherent.
        if roof_edge:
            over_l = max(0, int(TILE * 0.14)) if not left else 0
            over_r = max(0, int(TILE * 0.14)) if not right else 0
            roof_h = max(16, int(TILE * 0.38))
            roof_img = self.assets.get_fit("city_roof_detail", TILE + over_l + over_r, roof_h)
            shadow_y = py + max(11, int(TILE * 0.22))
            self.canvas.create_rectangle(px + 2, shadow_y, px + TILE - 2, shadow_y + 2, fill="#3f2f24", outline="")
            self.canvas.create_image(px - over_l, py - max(3, int(TILE * 0.10)), anchor="nw", image=roof_img)

    def draw_overworld_village_tile(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        up = tile_at(self.current_map_id, wx, wy - 1) == "u"
        down = tile_at(self.current_map_id, wx, wy + 1) == "u"
        left = tile_at(self.current_map_id, wx - 1, wy) == "u"
        right = tile_at(self.current_map_id, wx + 1, wy) == "u"
        self.canvas.create_rectangle(px + 7, py + 7, px + TILE - 7, py + TILE - 7, fill="#c5b06f", outline="#7f6d3d")
        if up:
            self.canvas.create_rectangle(px + 13, py, px + TILE - 13, py + 12, fill="#c5b06f", outline="")
        if down:
            self.canvas.create_rectangle(px + 13, py + TILE - 12, px + TILE - 13, py + TILE, fill="#c5b06f", outline="")
        if left:
            self.canvas.create_rectangle(px, py + 13, px + 12, py + TILE - 13, fill="#c5b06f", outline="")
        if right:
            self.canvas.create_rectangle(px + TILE - 12, py + 13, px + TILE, py + TILE - 13, fill="#c5b06f", outline="")
        if self.is_village_cluster_center(wx, wy):
            self.canvas.create_polygon(px + 10, py + 28, px + 24, py + 14, px + 38, py + 28, fill="#8d4931", outline="#53291d")
            self.canvas.create_rectangle(px + 13, py + 28, px + 35, py + 39, fill="#d5c28e", outline="#7f6d3d")
            self.canvas.create_rectangle(px + 21, py + 32, px + 27, py + 39, fill="#4a2b1b", outline="")
        else:
            self.canvas.create_rectangle(px + 13, py + 20, px + 35, py + 34, fill="#d5c28e", outline="#7f6d3d")
            self.canvas.create_polygon(px + 10, py + 20, px + 24, py + 9, px + 38, py + 20, fill="#8d4931", outline="#53291d")

    def draw_overworld_village_cluster(self, sx: int, sy: int, wx: int, wy: int) -> None:
        asset_name = self.overworld_village_cluster_asset(wx, wy)
        if self.assets.get_native(asset_name) is not None:
            cluster_size = TILE * 5
            cluster_x = sx * TILE + TILE // 2 - cluster_size // 2
            cluster_y = sy * TILE + TILE // 2 - cluster_size // 2
            self.canvas.create_image(
                cluster_x,
                cluster_y,
                anchor="nw",
                image=self.assets.get_fit(asset_name, cluster_size, cluster_size),
            )
            return

        x = (sx - 1) * TILE
        y = (sy - 1) * TILE
        size = TILE * 3
        ground = "#c6b46f"
        path = "#d5b879"
        path_dark = "#ad8b58"
        wall = "#88774d"
        wall_light = "#b8a56a"
        roof = "#9e4632"
        roof_alt = "#b45a39"
        wood = "#654228"

        skirt = [
            (x + 24, y + 12),
            (x + size - 22, y + 15),
            (x + size - 12, y + 40),
            (x + size - 17, y + size - 26),
            (x + size - 41, y + size - 12),
            (x + 26, y + size - 16),
            (x + 12, y + size - 42),
            (x + 15, y + 30),
        ]
        self.canvas.create_polygon(*[coord for point in skirt for coord in point], fill="#82b45c", outline="", smooth=True)
        self.canvas.create_polygon(
            x + 22,
            y + 22,
            x + size - 22,
            y + 22,
            x + size - 20,
            y + size - 24,
            x + 24,
            y + size - 20,
            fill=ground,
            outline="",
            smooth=True,
        )
        for i in range(22):
            px = x + 18 + ((i * 19 + wx * 5) % 108)
            py = y + 18 + ((i * 23 + wy * 3) % 108)
            self.canvas.create_rectangle(px, py, px + 3, py + 1, fill="#ddcc8a" if i % 2 else "#a9955d", outline="")

        # Crossroad and entry paths.
        self.canvas.create_line(x + size // 2, y + 5, x + size // 2, y + size - 5, fill=path_dark, width=14)
        self.canvas.create_line(x + 6, y + size // 2, x + size - 6, y + size // 2, fill=path_dark, width=14)
        self.canvas.create_line(x + size // 2, y + 5, x + size // 2, y + size - 5, fill=path, width=10)
        self.canvas.create_line(x + 6, y + size // 2, x + size - 6, y + size // 2, fill=path, width=10)

        self.canvas.create_rectangle(x + 24, y + 24, x + size - 24, y + size - 24, outline=wall, width=4)
        for px in range(x + 24, x + size - 23, 12):
            self.canvas.create_rectangle(px, y + 22, px + 5, y + 28, fill=wall_light, outline=wall)
            self.canvas.create_rectangle(px, y + size - 28, px + 5, y + size - 22, fill=wall_light, outline=wall)
        for py in range(y + 24, y + size - 23, 12):
            self.canvas.create_rectangle(x + 22, py, x + 28, py + 5, fill=wall_light, outline=wall)
            self.canvas.create_rectangle(x + size - 28, py, x + size - 22, py + 5, fill=wall_light, outline=wall)

        houses = [
            (x + 36, y + 35, 23, 17, roof),
            (x + 86, y + 35, 23, 17, roof_alt),
            (x + 36, y + 91, 23, 17, roof_alt),
            (x + 86, y + 91, 23, 17, roof),
            (x + 62, y + 64, 22, 16, roof),
        ]
        for hx, hy, hw, hh, color in houses:
            self.canvas.create_rectangle(hx, hy, hx + hw, hy + hh, fill="#d6c18b", outline="#79663e")
            self.canvas.create_polygon(hx - 2, hy, hx + hw // 2, hy - 9, hx + hw + 2, hy, fill=color, outline="#5d2a22")
            self.canvas.create_line(hx + 3, hy + 4, hx + hw - 3, hy + 4, fill="#eddda5", width=1)
            self.canvas.create_rectangle(hx + 4, hy + 8, hx + 8, hy + 12, fill=wood, outline="")
            self.canvas.create_rectangle(hx + hw - 9, hy + 8, hx + hw - 5, hy + 12, fill=wood, outline="")
            self.canvas.create_rectangle(hx + hw // 2 - 2, hy + hh - 6, hx + hw // 2 + 3, hy + hh, fill="#3f281a", outline="")

        self.canvas.create_rectangle(x + 65, y + 15, x + 79, y + 32, fill=wall_light, outline=wall)
        self.canvas.create_polygon(x + 62, y + 15, x + 72, y + 4, x + 82, y + 15, fill="#40576a", outline="#243746")
        self.canvas.create_rectangle(x + 65, y + size - 32, x + 79, y + size - 15, fill=wall_light, outline=wall)
        self.canvas.create_polygon(x + 62, y + size - 32, x + 72, y + size - 43, x + 82, y + size - 32, fill="#40576a", outline="#243746")

        for i in range(10):
            px = x + 22 + ((i * 27 + wx) % 104)
            py = y + 22 + ((i * 17 + wy) % 104)
            self.canvas.create_rectangle(px, py, px + 4, py + 2, fill="#719d52", outline="")

    def overworld_city_uses_snow_props(self, wx: int, wy: int) -> bool:
        if self.current_map_id != OVERWORLD_ID:
            return False
        cold = 0
        temperate = 0
        for dy in range(-7, 8):
            for dx in range(-7, 8):
                distance = abs(dx) + abs(dy)
                if distance > 8:
                    continue
                tile = tile_at(self.current_map_id, wx + dx, wy + dy)
                if tile in {"n", "m", "q"}:
                    cold += max(1, 9 - distance)
                elif tile in {"g", "f", "v", "s", "b"}:
                    temperate += max(1, 7 - distance)
        return cold >= 8 and cold >= temperate * 0.35

    def draw_scaled_city_prop(self, name: str, x: int, y: int, target_h: int, seed: int) -> None:
        native = self.assets.get_native(name)
        if native is None or native.height() <= 0:
            return
        ratio = native.width() / native.height()
        prop_h = max(10, int(target_h * (0.92 + ((seed >> 8) & 3) * 0.04)))
        prop_w = max(8, int(prop_h * ratio))
        self.canvas.create_oval(
            x + max(1, prop_w // 8),
            y + prop_h - max(4, prop_h // 5),
            x + prop_w - max(1, prop_w // 8),
            y + prop_h,
            fill="#111820",
            outline="",
            stipple="gray50",
        )
        self.canvas.create_image(x, y, anchor="nw", image=self.assets.get_fit(name, prop_w, prop_h, variant=(seed >> 18) % 3))

    def draw_overworld_city_props(self, x: int, y: int, size: int, wx: int, wy: int) -> None:
        snowy = self.overworld_city_uses_snow_props(wx, wy)
        choices = CITY_SOURCE_SNOW_PROPS if snowy else CITY_OVERWORLD_TEMPERATE_PROPS
        anchors = (
            (0.18, 0.29),
            (0.35, 0.21),
            (0.61, 0.25),
            (0.77, 0.38),
            (0.25, 0.67),
            (0.47, 0.73),
            (0.69, 0.66),
            (0.82, 0.78),
        )
        for index, (rx, ry) in enumerate(anchors):
            seed = self.terrain_seed(wx + index * 3, wy - index * 5, "c", 1423 + index * 41)
            if seed % 100 >= (76 if snowy else 68):
                continue
            name, target_h = choices[(seed >> 6) % len(choices)]
            px = x + int(size * rx) + (((seed >> 12) & 7) - 3)
            py = y + int(size * ry) + (((seed >> 16) & 7) - 3)
            self.draw_scaled_city_prop(name, px, py, max(12, int(target_h * size / (TILE * 5))), seed)

    def draw_overworld_city_tile_prop(self, px: int, py: int, wx: int, wy: int, blocked_by_road: bool) -> None:
        if blocked_by_road:
            return
        seed = self.terrain_seed(wx, wy, "c", 733)
        if seed % 100 >= 24:
            return
        choices = CITY_SOURCE_SNOW_PROPS if self.overworld_city_uses_snow_props(wx, wy) else CITY_OVERWORLD_TEMPERATE_PROPS
        name, target_h = choices[(seed >> 5) % len(choices)]
        prop_h = max(11, int(target_h * 0.70))
        x = px + 6 + ((seed >> 10) % max(1, TILE - 24))
        y = py + TILE - prop_h - 5 - ((seed >> 15) & 5)
        self.draw_scaled_city_prop(name, x, y, prop_h, seed)

    def draw_overworld_city_cluster(self, sx: int, sy: int, wx: int, wy: int) -> None:
        x = (sx - 1) * TILE
        y = (sy - 1) * TILE
        size = TILE * 3
        if self.assets.get_native("city_overworld_cluster") is not None:
            cluster_size = TILE * 5
            cluster_x = sx * TILE + TILE // 2 - cluster_size // 2
            cluster_y = sy * TILE + TILE // 2 - cluster_size // 2
            self.canvas.create_image(
                cluster_x,
                cluster_y,
                anchor="nw",
                image=self.assets.get_fit("city_overworld_cluster", cluster_size, cluster_size),
            )
            self.draw_overworld_city_props(cluster_x, cluster_y, cluster_size, wx, wy)
            return

        wall = "#9aa1a2"
        wall_dark = "#505861"
        wall_light = "#dcecf2"
        city_floor = "#c4b37c"
        road = "#b89a62"
        road_dark = "#74624c"
        plaza = "#e9d79f"
        roof = "#9f4634"
        roof_alt = "#b95f3f"
        roof_dark = "#542820"
        stone = "#747b80"
        snow = "#eef8fb"
        snow_shadow = "#b6cbd7"

        skirt = [
            (x + 28, y + 7),
            (x + 110, y + 5),
            (x + 139, y + 31),
            (x + 136, y + 105),
            (x + 109, y + 137),
            (x + 33, y + 135),
            (x + 5, y + 104),
            (x + 7, y + 31),
        ]
        self.canvas.create_polygon(*[coord for point in skirt for coord in point], fill="#d9ebef", outline="", smooth=True)
        for i in range(22):
            px = x + 10 + ((i * 23 + wx * 11) % 123)
            py = y + 9 + ((i * 19 + wy * 13) % 124)
            color = "#b9d1dc" if i % 3 else "#f7fbff"
            self.canvas.create_rectangle(px, py, px + 3, py + 1, fill=color, outline="")
        wall_points = [
            (x + 12, y + 27),
            (x + 45, y + 10),
            (x + 91, y + 8),
            (x + 130, y + 25),
            (x + 137, y + 66),
            (x + 125, y + 116),
            (x + 83, y + 136),
            (x + 32, y + 128),
            (x + 8, y + 90),
        ]
        flat_points = [coord for point in wall_points for coord in point]
        self.canvas.create_polygon(*flat_points, fill=city_floor, outline="", smooth=True)
        for i in range(36):
            px = x + 13 + ((i * 29 + wx * 7) % 118)
            py = y + 14 + ((i * 17 + wy * 5) % 112)
            color = "#e8d9a3" if i % 2 else ("#9c8a62" if i % 5 else "#f4fbff")
            self.canvas.create_rectangle(px, py, px + 3, py + 1, fill=color, outline="")

        self.canvas.create_polygon(*flat_points, fill="", outline=wall_dark, width=9, smooth=True)
        self.canvas.create_polygon(*flat_points, fill="", outline=wall, width=6, smooth=True)
        self.canvas.create_polygon(*flat_points, fill="", outline=wall_light, width=2, smooth=True)

        for i, ((x1, y1), (x2, y2)) in enumerate(zip(wall_points, wall_points[1:] + wall_points[:1])):
            steps = max(1, int(math.hypot(x2 - x1, y2 - y1) // 12))
            for step in range(steps):
                t = (step + 0.5) / steps
                cx = int(x1 + (x2 - x1) * t)
                cy = int(y1 + (y2 - y1) * t)
                self.canvas.create_rectangle(cx - 2, cy - 2, cx + 2, cy + 2, fill=wall_dark, outline="")
                if step % 2 == 0:
                    self.canvas.create_rectangle(cx - 3, cy - 5, cx + 3, cy - 3, fill=snow, outline="")

        gates = [
            (x + 65, y + 127, x + 83, y + 142),
            (x + 128, y + 62, x + 143, y + 80),
        ]
        for gx1, gy1, gx2, gy2 in gates:
            self.canvas.create_rectangle(gx1, gy1, gx2, gy2, fill=road, outline="")
            self.canvas.create_line(gx1, gy1, gx2, gy2, fill=road_dark, width=1)

        tower_points = [
            (x + 24, y + 29),
            (x + 58, y + 10),
            (x + 112, y + 17),
            (x + 134, y + 74),
            (x + 98, y + 126),
            (x + 36, y + 121),
        ]
        for tx, ty in tower_points:
            self.canvas.create_rectangle(tx - 6, ty - 5, tx + 6, ty + 12, fill=wall, outline=wall_dark)
            self.canvas.create_line(tx - 4, ty + 1, tx + 4, ty + 1, fill=wall_light, width=1)
            self.canvas.create_polygon(tx - 9, ty - 5, tx, ty - 17, tx + 9, ty - 5, fill="#4d6476", outline="#273744")
            self.canvas.create_line(tx - 6, ty - 8, tx, ty - 15, tx + 6, ty - 8, fill=snow, width=2)

        road_paths = [
            (x + 74, y + 24, x + 74, y + 131),
            (x + 24, y + 72, x + 128, y + 72),
            (x + 48, y + 34, x + 96, y + 106),
            (x + 34, y + 108, x + 113, y + 44),
        ]
        for x1, y1, x2, y2 in road_paths:
            self.canvas.create_line(x1, y1, x2, y2, fill=road_dark, width=9)
            self.canvas.create_line(x1, y1, x2, y2, fill=road, width=6)
            self.canvas.create_line(x1, y1, x2, y2, fill="#e2c78a", width=1)

        self.canvas.create_oval(x + 57, y + 55, x + 91, y + 89, fill=plaza, outline="#b8955f", width=2)
        self.canvas.create_oval(x + 65, y + 63, x + 83, y + 81, fill="#f4e7bc", outline="#cdb673")
        self.canvas.create_oval(x + 68, y + 66, x + 80, y + 78, fill="#bde7ef", outline="#7caeb9")

        keep = (x + 92, y + 42)
        self.canvas.create_rectangle(keep[0], keep[1], keep[0] + 26, keep[1] + 32, fill="#cfc18b", outline=stone, width=2)
        self.canvas.create_rectangle(keep[0] + 4, keep[1] + 9, keep[0] + 9, keep[1] + 15, fill="#5d3a22", outline="")
        self.canvas.create_rectangle(keep[0] + 17, keep[1] + 9, keep[0] + 22, keep[1] + 15, fill="#5d3a22", outline="")
        self.canvas.create_polygon(keep[0] - 3, keep[1], keep[0] + 13, keep[1] - 15, keep[0] + 29, keep[1], fill="#6d7c8d", outline="#405063")
        self.canvas.create_line(keep[0] + 1, keep[1] - 3, keep[0] + 13, keep[1] - 14, keep[0] + 26, keep[1] - 3, fill=snow, width=2)

        houses = [
            (x + 27, y + 45, 24, 15, roof),
            (x + 35, y + 84, 22, 15, roof_alt),
            (x + 57, y + 31, 22, 14, roof_alt),
            (x + 88, y + 88, 24, 15, roof),
            (x + 59, y + 105, 21, 13, roof),
            (x + 108, y + 76, 20, 13, roof_alt),
            (x + 21, y + 102, 18, 12, roof),
            (x + 101, y + 22, 18, 12, roof_alt),
            (x + 44, y + 115, 18, 12, roof_alt),
        ]
        for hx, hy, hw, hh, color in houses:
            self.canvas.create_rectangle(hx, hy, hx + hw, hy + hh, fill="#dfcf9c", outline="#8a7657")
            self.canvas.create_polygon(hx - 2, hy, hx + hw // 2, hy - 8, hx + hw + 2, hy, fill=color, outline=roof_dark)
            self.canvas.create_line(hx + 1, hy + 1, hx + hw - 1, hy + 1, fill=snow, width=2)
            self.canvas.create_line(hx + 2, hy + 4, hx + hw - 2, hy + 4, fill="#f0dfa7", width=1)
            self.canvas.create_rectangle(hx + 4, hy + 6, hx + 8, hy + 9, fill="#d79f47", outline="#5d3a22")
            if hw > 20:
                self.canvas.create_rectangle(hx + hw - 8, hy + 6, hx + hw - 4, hy + 9, fill="#d79f47", outline="#5d3a22")
            self.canvas.create_rectangle(hx + hw // 2 - 2, hy + hh - 5, hx + hw // 2 + 3, hy + hh, fill="#4a2b1b", outline="")

        stalls = [
            (x + 47, y + 64, "#4f7fb4"),
            (x + 96, y + 68, "#d7c45c"),
            (x + 68, y + 92, "#8fc36a"),
        ]
        for sx1, sy1, color in stalls:
            self.canvas.create_rectangle(sx1, sy1, sx1 + 10, sy1 + 7, fill="#d8c28a", outline="#8a7657")
            self.canvas.create_rectangle(sx1 - 1, sy1 - 4, sx1 + 11, sy1 + 1, fill=color, outline="#6e2f28")

        for i in range(14):
            px = x + 18 + ((i * 31 + wx) % 108)
            py = y + 22 + ((i * 23 + wy) % 103)
            color = "#6f9f54" if i % 2 else "#8cbf65"
            self.canvas.create_rectangle(px, py, px + 4, py + 2, fill=color, outline="")
        self.draw_overworld_city_props(x, y, size, wx, wy)

    def draw_overworld_town_tile(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        cx = px + TILE // 2
        cy = py + TILE // 2
        up = tile_at(self.current_map_id, wx, wy - 1) == "c"
        down = tile_at(self.current_map_id, wx, wy + 1) == "c"
        left = tile_at(self.current_map_id, wx - 1, wy) == "c"
        right = tile_at(self.current_map_id, wx + 1, wy) == "c"
        road_up = tile_at(self.current_map_id, wx, wy - 1) == "r"
        road_down = tile_at(self.current_map_id, wx, wy + 1) == "r"
        road_left = tile_at(self.current_map_id, wx - 1, wy) == "r"
        road_right = tile_at(self.current_map_id, wx + 1, wy) == "r"

        seed = self.terrain_seed(wx, wy, "c", 173)
        district = "#cbb77d" if (seed & 1) else "#d6c188"
        district_dark = "#a98f62"
        cobble = "#eadba7"
        road_fill = "#c99f65"
        road_shadow = "#73614e"
        wall = "#9ba4a5"
        wall_dark = "#535b62"
        wall_light = "#dbe8e9"
        roof = "#8e3f34" if (seed % 3) else "#49687d"
        roof_alt = "#b75a3f" if (seed % 5) else "#6a7988"
        facade = "#dccb98"
        outline = "#78684e"

        center_point: tuple[int, int] | None = None
        for city_cx, city_cy in CITY_GATE_CENTERS.values():
            if abs(wx - city_cx) <= 6 and abs(wy - city_cy) <= 6:
                center_point = (city_cx, city_cy)
                break
        main_vertical = center_point is not None and wx == center_point[0]
        main_horizontal = center_point is not None and wy == center_point[1]

        self.canvas.create_rectangle(px + 4, py + 4, px + TILE - 4, py + TILE - 4, fill=district, outline="")
        if up:
            self.canvas.create_rectangle(px + 4, py, px + TILE - 4, py + 8, fill=district, outline="")
        if down:
            self.canvas.create_rectangle(px + 4, py + TILE - 8, px + TILE - 4, py + TILE, fill=district, outline="")
        if left:
            self.canvas.create_rectangle(px, py + 4, px + 8, py + TILE - 4, fill=district, outline="")
        if right:
            self.canvas.create_rectangle(px + TILE - 8, py + 4, px + TILE, py + TILE - 4, fill=district, outline="")
        if up and left:
            self.canvas.create_rectangle(px, py, px + 8, py + 8, fill=district, outline="")
        if up and right:
            self.canvas.create_rectangle(px + TILE - 8, py, px + TILE, py + 8, fill=district, outline="")
        if down and left:
            self.canvas.create_rectangle(px, py + TILE - 8, px + 8, py + TILE, fill=district, outline="")
        if down and right:
            self.canvas.create_rectangle(px + TILE - 8, py + TILE - 8, px + TILE, py + TILE, fill=district, outline="")

        for i in range(10):
            sx1 = px + 7 + ((seed >> (i % 13)) + i * 11) % 34
            sy1 = py + 7 + ((seed >> ((i + 3) % 13)) + i * 7) % 34
            self.canvas.create_rectangle(sx1, sy1, sx1 + 3, sy1 + 1, fill=cobble if i % 3 else district_dark, outline="")

        road_w = max(12, int(TILE * 0.29))
        half = road_w // 2
        if main_vertical or road_up or road_down:
            y1 = py - 2 if road_up or main_vertical else cy
            y2 = py + TILE + 2 if road_down or main_vertical else cy
            self.canvas.create_rectangle(cx - half - 1, y1, cx + half + 1, y2, fill=road_shadow, outline="")
            self.canvas.create_rectangle(cx - half, y1, cx + half, y2, fill=road_fill, outline="")
            self.canvas.create_line(cx, max(py + 4, y1), cx, min(py + TILE - 4, y2), fill="#ead08d", width=1)
        if main_horizontal or road_left or road_right:
            x1 = px - 2 if road_left or main_horizontal else cx
            x2 = px + TILE + 2 if road_right or main_horizontal else cx
            self.canvas.create_rectangle(x1, cy - half - 1, x2, cy + half + 1, fill=road_shadow, outline="")
            self.canvas.create_rectangle(x1, cy - half, x2, cy + half, fill=road_fill, outline="")
            self.canvas.create_line(max(px + 4, x1), cy, min(px + TILE - 4, x2), cy, fill="#ead08d", width=1)

        if self.is_city_cluster_center(wx, wy):
            self.canvas.create_oval(px + 10, py + 10, px + TILE - 10, py + TILE - 10, fill="#ead79b", outline="#a98353", width=2)
            self.canvas.create_rectangle(cx - 8, cy - 5, cx + 8, cy + 10, fill="#d8c899", outline=outline)
            self.canvas.create_polygon(cx - 11, cy - 5, cx, cy - 16, cx + 11, cy - 5, fill=roof_alt, outline="#49342d")
            self.canvas.create_rectangle(cx - 2, cy + 3, cx + 3, cy + 10, fill="#4b2f20", outline="")
        else:
            slots = (
                (px + 7, py + 9, 14, 11, roof),
                (px + TILE - 21, py + 9, 14, 11, roof_alt),
                (px + 7, py + TILE - 20, 15, 11, roof_alt),
                (px + TILE - 22, py + TILE - 20, 15, 11, roof),
            )
            for idx, (hx, hy, hw, hh, roof_color) in enumerate(slots):
                slot_cx = hx + hw // 2
                slot_cy = hy + hh // 2
                if main_vertical and abs(slot_cx - cx) < half + 7:
                    continue
                if main_horizontal and abs(slot_cy - cy) < half + 7:
                    continue
                if (seed + idx * 17) % 5 == 0 and not (not up or not down or not left or not right):
                    continue
                self.canvas.create_rectangle(hx, hy + 4, hx + hw, hy + hh + 5, fill=facade, outline=outline)
                self.canvas.create_polygon(hx - 2, hy + 4, hx + hw // 2, hy - 4, hx + hw + 2, hy + 4, fill=roof_color, outline="#49342d")
                self.canvas.create_rectangle(hx + hw // 2 - 2, hy + hh, hx + hw // 2 + 2, hy + hh + 5, fill="#4b2f20", outline="")

        self.draw_overworld_city_tile_prop(
            px,
            py,
            wx,
            wy,
            main_vertical
            or main_horizontal
            or road_up
            or road_down
            or road_left
            or road_right
            or self.is_city_cluster_center(wx, wy),
        )

        def wall_segment(x1: int, y1: int, x2: int, y2: int) -> None:
            self.canvas.create_rectangle(x1, y1, x2, y2, fill=wall_dark, outline="")
            self.canvas.create_rectangle(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill=wall, outline="")
            if x2 - x1 > y2 - y1:
                self.canvas.create_line(x1 + 2, y1 + 2, x2 - 2, y1 + 2, fill=wall_light, width=1)
            else:
                self.canvas.create_line(x1 + 2, y1 + 2, x1 + 2, y2 - 2, fill=wall_light, width=1)

        gate = max(15, int(TILE * 0.36))
        if not up:
            if road_up:
                wall_segment(px + 4, py + 2, cx - gate // 2, py + 8)
                wall_segment(cx + gate // 2, py + 2, px + TILE - 4, py + 8)
            else:
                wall_segment(px + 5, py + 2, px + TILE - 5, py + 8)
        if not down:
            if road_down:
                wall_segment(px + 4, py + TILE - 8, cx - gate // 2, py + TILE - 2)
                wall_segment(cx + gate // 2, py + TILE - 8, px + TILE - 4, py + TILE - 2)
            else:
                wall_segment(px + 5, py + TILE - 8, px + TILE - 5, py + TILE - 2)
        if not left:
            if road_left:
                wall_segment(px + 2, py + 4, px + 8, cy - gate // 2)
                wall_segment(px + 2, cy + gate // 2, px + 8, py + TILE - 4)
            else:
                wall_segment(px + 2, py + 5, px + 8, py + TILE - 5)
        if not right:
            if road_right:
                wall_segment(px + TILE - 8, py + 4, px + TILE - 2, cy - gate // 2)
                wall_segment(px + TILE - 8, cy + gate // 2, px + TILE - 2, py + TILE - 4)
            else:
                wall_segment(px + TILE - 8, py + 5, px + TILE - 2, py + TILE - 5)

        for tower_x, tower_y, needed in (
            (px + 4, py + 4, (not up and not left)),
            (px + TILE - 12, py + 4, (not up and not right)),
            (px + 4, py + TILE - 12, (not down and not left)),
            (px + TILE - 12, py + TILE - 12, (not down and not right)),
        ):
            if needed:
                self.canvas.create_rectangle(tower_x, tower_y, tower_x + 8, tower_y + 8, fill=wall_dark, outline="")
                self.canvas.create_rectangle(tower_x + 1, tower_y + 1, tower_x + 7, tower_y + 7, fill=wall, outline="")

    def draw_stone_wall(self, sx: int, sy: int, wx: int, wy: int) -> None:
        if self.current_map_id.startswith(("city_", "village_")):
            self.draw_modular_wall_tile(sx, sy, wx, wy)
            return
        if self.current_map_id.startswith("dungeon_"):
            self.draw_dungeon_wall_tile(sx, sy, wx, wy)
            return
        px = sx * TILE
        py = sy * TILE
        city_map = self.current_map_id.startswith("city_")
        base = "#898982" if city_map else ("#79808a" if (wx + wy) % 2 == 0 else "#6f7783")
        mortar = "#5f5f5a" if city_map else "#646b75"
        self.canvas.create_rectangle(px, py, px + TILE, py + TILE, fill=base, outline="")
        for row, yy in enumerate(range(py + 8, py + TILE, 10)):
            self.canvas.create_line(px + 3, yy, px + TILE - 3, yy, fill="#aaa9a0" if city_map else "#8e95a0", width=1)
            offset = 0 if row % 2 == 0 else 12
            for xx in range(px - offset, px + TILE, 24):
                self.canvas.create_line(xx, yy - 9, xx, yy, fill=mortar, width=1, stipple="gray50")
        if city_map and (wx * 5 + wy) % 7 == 0:
            self.canvas.create_oval(px + 18, py + 16, px + 30, py + 31, fill="#252522", outline="#111")
            self.canvas.create_line(px + 20, py + 23, px + 28, py + 23, fill="#696960", width=1)

    def draw_modular_wall_tile(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        topology_key = (self.current_map_id, wx, wy)
        topology = self._wall_topology_cache.get(topology_key)
        if topology is None:
            topology = (
                tile_at(self.current_map_id, wx, wy - 1) == "x",
                tile_at(self.current_map_id, wx, wy + 1) == "x",
                tile_at(self.current_map_id, wx - 1, wy) == "x",
                tile_at(self.current_map_id, wx + 1, wy) == "x",
            )
            self._wall_topology_cache[topology_key] = topology
        up, down, left, right = topology
        seed = self.terrain_seed(wx, wy, "x", 67)
        wall_name = "city_snow_cobble" if self.current_map_id.startswith("village_snow") else "city_wall_block"
        self.canvas.create_image(px, py, anchor="nw", image=self.assets.get_fit(wall_name, TILE, TILE, variant=seed % 3))

        trim_h = max(6, int(TILE * 0.14))
        trim_w = max(6, int(TILE * 0.14))
        trim_variant = (seed >> 3) % 3
        if not up:
            self.canvas.create_image(px, py, anchor="nw", image=self.assets.get_fit("city_wall_block", TILE, trim_h, variant=trim_variant))
        if not down:
            self.canvas.create_image(px, py + TILE - trim_h, anchor="nw", image=self.assets.get_fit("city_wall_block", TILE, trim_h, variant=(trim_variant + 1) % 3))
        if not left:
            self.canvas.create_image(px, py, anchor="nw", image=self.assets.get_fit("city_wall_block", trim_w, TILE, variant=(trim_variant + 2) % 3))
        if not right:
            self.canvas.create_image(px + TILE - trim_w, py, anchor="nw", image=self.assets.get_fit("city_wall_block", trim_w, TILE, variant=(trim_variant + 1) % 3))

        if (seed % 9 == 0) and not up:
            lantern_h = max(12, int(TILE * 0.28))
            lantern_w = max(7, int(TILE * 0.18))
            lantern = self.assets.get_fit("city_lantern", lantern_w, lantern_h)
            lx = px + TILE // 2 - lantern_w // 2
            ly = py + trim_h + max(2, int(TILE * 0.08))
            self.canvas.create_image(lx, ly, anchor="nw", image=lantern)

    def draw_dungeon_wall_tile(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        up = tile_at(self.current_map_id, wx, wy - 1) == "x"
        down = tile_at(self.current_map_id, wx, wy + 1) == "x"
        left = tile_at(self.current_map_id, wx - 1, wy) == "x"
        right = tile_at(self.current_map_id, wx + 1, wy) == "x"
        seed = self.terrain_seed(wx, wy, "x", 17)

        self.canvas.create_rectangle(px + 3, py + 5, px + TILE - 3, py + 13, fill="#5f6274", outline="")
        if not up:
            self.canvas.create_rectangle(px + 1, py, px + TILE - 1, py + 6, fill="#8d8196", outline="")
            for xx in range(px + 4, px + TILE, 14):
                self.canvas.create_rectangle(xx, py + 1, xx + 8, py + 7, fill="#b4a472", outline="#5f5568")
        if down:
            self.canvas.create_rectangle(px + 2, py + TILE - 7, px + TILE - 2, py + TILE, fill="#272937", outline="")
        if not left:
            self.canvas.create_line(px + 2, py + 5, px + 2, py + TILE - 4, fill="#787c91", width=2)
        if not right:
            self.canvas.create_line(px + TILE - 3, py + 5, px + TILE - 3, py + TILE - 4, fill="#272936", width=2)

        for yy in range(py + 15, py + TILE - 4, 11):
            self.canvas.create_line(px + 5, yy, px + TILE - 5, yy, fill="#383a4a", width=1)
        for i, xx in enumerate(range(px + 9, px + TILE - 5, 13)):
            top = py + 16 + ((i + wx + wy) % 2) * 11
            self.canvas.create_line(xx, top, xx, min(py + TILE - 5, top + 10), fill="#383a4a", width=1)

        if not down and seed % 100 < 48:
            arch_fill = "#20222e"
            arch_edge = "#6b6f83"
            self.canvas.create_oval(px + 10, py + 13, px + 38, py + 41, fill=arch_fill, outline=arch_edge, width=1)
            self.canvas.create_rectangle(px + 10, py + 27, px + 38, py + 42, fill=arch_fill, outline=arch_edge)
            self.canvas.create_rectangle(px + 12, py + 27, px + 36, py + 43, fill=arch_fill, outline="")
            if seed % 7 == 0:
                glow = "#8d77c0"
                self.canvas.create_oval(px + 22, py + 21, px + 28, py + 27, fill=glow, outline="")
        elif seed % 100 < 12:
            self.canvas.create_oval(px + 20, py + 18, px + 28, py + 26, fill="#7f73aa", outline="")

    def draw_dungeon_floor_detail(self, sx: int, sy: int, wx: int, wy: int) -> None:
        px = sx * TILE
        py = sy * TILE
        seed = self.terrain_seed(wx, wy, "d", 31)
        self.canvas.create_rectangle(px + 5, py + 5, px + TILE - 5, py + TILE - 5, outline="#302c3d", width=1)
        self.canvas.create_line(px + 6, py + 12, px + TILE - 6, py + 12, fill="#746b87", width=1)
        self.canvas.create_line(px + 6, py + 30, px + TILE - 6, py + 30, fill="#2c293a", width=1)
        self.canvas.create_line(px + 17, py + 6, px + 17, py + TILE - 6, fill="#2c293a", width=1)
        self.canvas.create_line(px + 34, py + 6, px + 34, py + TILE - 6, fill="#746b87", width=1)
        for i in range(3):
            ox = 9 + ((seed >> (i * 4)) % 30)
            oy = 8 + ((seed >> (i * 5 + 2)) % 30)
            color = "#746b87" if i % 2 else "#383447"
            self.canvas.create_rectangle(px + ox, py + oy, px + ox + 2, py + oy + 1, fill=color, outline="")
        if (wx * 3 + wy + self.frame // 20) % 13 == 0:
            pulse = 1 + (self.frame // 5) % 2
            self.canvas.create_oval(px + 20 - pulse, py + 20 - pulse, px + 28 + pulse, py + 28 + pulse, fill="#8c7bc0", outline="")

    def is_path_tile(self, tile: str) -> bool:
        return tile in {"r", "p", "c", "u", "d"}

    def draw_path_overlay(self, sx: int, sy: int, wx: int, wy: int, tile: str, city_map: bool, dungeon_map: bool) -> None:
        if tile not in {"r", "p", "c"}:
            return
        if city_map and self.current_map_id.startswith("city_"):
            return
        px = sx * TILE
        py = sy * TILE
        cx = px + TILE // 2
        cy = py + TILE // 2
        if tile == "c":
            half = 12
        elif tile == "r":
            half = 10
        else:
            half = 12
        if dungeon_map:
            road_fill = "#65587d"
            road_highlight = "#80709f"
            road_shadow = "#4c405f"
        elif city_map:
            road_fill = "#968a78"
            road_highlight = "#c8baa0"
            road_shadow = "#766d60"
        else:
            road_fill = "#caa56a"
            road_highlight = "#e8cf96"
            road_shadow = "#9f7d4f"

        topology_key = (self.current_map_id, wx, wy)
        topology = self._path_topology_cache.get(topology_key)
        if topology is None:
            topology = (
                self.is_path_tile(tile_at(self.current_map_id, wx, wy - 1)),
                self.is_path_tile(tile_at(self.current_map_id, wx, wy + 1)),
                self.is_path_tile(tile_at(self.current_map_id, wx - 1, wy)),
                self.is_path_tile(tile_at(self.current_map_id, wx + 1, wy)),
            )
            self._path_topology_cache[topology_key] = topology
        up, down, left, right = topology

        if not city_map and not dungeon_map:
            bits = (1 if up else 0) | (2 if down else 0) | (4 if left else 0) | (8 if right else 0)
            overlay_name = f"road_overlay_{bits:02x}"
            if self.assets.get_native(overlay_name) is not None:
                self.canvas.create_image(px, py, anchor="nw", image=self.assets.get(overlay_name, TILE))
                return

            road_width = 15
            shoulder_width = 21

            def offset(a: int, b: int, salt: int, span: int = 4) -> int:
                return ((a * 928371 + b * 364479 + salt * 811) % (span * 2 + 1)) - span

            endpoints: list[tuple[str, int, int]] = []
            if up:
                endpoints.append(("up", cx + offset(wx, wy, 1), py - 3))
            if down:
                endpoints.append(("down", cx + offset(wx, wy + 1, 1), py + TILE + 3))
            if left:
                endpoints.append(("left", px - 3, cy + offset(wx, wy, 2)))
            if right:
                endpoints.append(("right", px + TILE + 3, cy + offset(wx + 1, wy, 2)))

            if not endpoints:
                endpoints.append(("center", cx + offset(wx, wy, 3, 2), cy + offset(wx, wy, 4, 2)))

            center_x = int(sum(point[1] for point in endpoints) / len(endpoints))
            center_y = int(sum(point[2] for point in endpoints) / len(endpoints))
            center_x = min(px + TILE - 12, max(px + 12, center_x + offset(wx, wy, 5, 2)))
            center_y = min(py + TILE - 12, max(py + 12, center_y + offset(wx, wy, 6, 2)))

            def draw_curve(points: list[tuple[int, int]]) -> None:
                coords = [coord for point in points for coord in point]
                self.canvas.create_line(*coords, fill=road_shadow, width=shoulder_width, smooth=True, capstyle=tk.ROUND, joinstyle=tk.ROUND)
                self.canvas.create_line(*coords, fill=road_fill, width=road_width, smooth=True, capstyle=tk.ROUND, joinstyle=tk.ROUND)

            endpoint_map = {name: (x, y) for name, x, y in endpoints}
            if up and down and not left and not right:
                draw_curve([endpoint_map["up"], (center_x, center_y), endpoint_map["down"]])
            elif left and right and not up and not down:
                draw_curve([endpoint_map["left"], (center_x, center_y), endpoint_map["right"]])
            elif len(endpoints) == 2:
                draw_curve([(endpoints[0][1], endpoints[0][2]), (center_x, center_y), (endpoints[1][1], endpoints[1][2])])
            else:
                for _name, end_x, end_y in endpoints:
                    mid_x = (center_x + end_x) // 2 + offset(wx + end_x, wy + end_y, 7, 2)
                    mid_y = (center_y + end_y) // 2 + offset(wx + end_y, wy + end_x, 8, 2)
                    draw_curve([(center_x, center_y), (mid_x, mid_y), (end_x, end_y)])
                if len(endpoints) >= 3:
                    self.canvas.create_oval(center_x - 9, center_y - 9, center_x + 9, center_y + 9, fill=road_shadow, outline="")
                    self.canvas.create_oval(center_x - 6, center_y - 6, center_x + 6, center_y + 6, fill=road_fill, outline="")

            for i in range(10):
                ox = 8 + ((wx * 11 + wy * 7 + i * 13) % 32)
                oy = 9 + ((wx * 5 + wy * 17 + i * 9) % 30)
                color = "#b78f58" if i % 3 else "#eed8a4"
                self.canvas.create_oval(px + ox, py + oy, px + ox + 3, py + oy + 1, fill=color, outline="")
            if up or down:
                self.canvas.create_line(center_x, py + 5, center_x + offset(wx, wy, 30, 2), py + TILE - 5, fill=road_highlight, width=1, smooth=True)
            if left or right:
                self.canvas.create_line(px + 5, center_y, px + TILE - 5, center_y + offset(wx, wy, 31, 2), fill=road_highlight, width=1, smooth=True)
            return

        if dungeon_map:
            shoulder = 17
            half = 14
            self.canvas.create_rectangle(cx - shoulder, cy - shoulder, cx + shoulder, cy + shoulder, fill="#2f2b40", outline="")
            if up:
                self.canvas.create_rectangle(cx - shoulder, py - 1, cx + shoulder, cy, fill="#2f2b40", outline="")
            if down:
                self.canvas.create_rectangle(cx - shoulder, cy, cx + shoulder, py + TILE + 1, fill="#2f2b40", outline="")
            if left:
                self.canvas.create_rectangle(px - 1, cy - shoulder, cx, cy + shoulder, fill="#2f2b40", outline="")
            if right:
                self.canvas.create_rectangle(cx, cy - shoulder, px + TILE + 1, cy + shoulder, fill="#2f2b40", outline="")

            self.canvas.create_rectangle(cx - half, cy - half, cx + half, cy + half, fill=road_fill, outline="#3c344d")
            if up:
                self.canvas.create_rectangle(cx - half, py - 1, cx + half, cy, fill=road_fill, outline="")
            if down:
                self.canvas.create_rectangle(cx - half, cy, cx + half, py + TILE + 1, fill=road_fill, outline="")
            if left:
                self.canvas.create_rectangle(px - 1, cy - half, cx, cy + half, fill=road_fill, outline="")
            if right:
                self.canvas.create_rectangle(cx, cy - half, px + TILE + 1, cy + half, fill=road_fill, outline="")

            for y in range(py + 9, py + TILE, 12):
                self.canvas.create_line(cx - half + 1, y, cx + half - 1, y, fill=road_shadow, width=1)
            for x in range(px + 9, px + TILE, 12):
                self.canvas.create_line(x, cy - half + 1, x, cy + half - 1, fill=road_shadow, width=1)
            if up or down:
                self.canvas.create_line(cx - 6, py + 2, cx - 6, py + TILE - 2, fill=road_highlight, width=1)
                self.canvas.create_line(cx + 7, py + 2, cx + 7, py + TILE - 2, fill="#514663", width=1)
            if left or right:
                self.canvas.create_line(px + 2, cy - 6, px + TILE - 2, cy - 6, fill=road_highlight, width=1)
                self.canvas.create_line(px + 2, cy + 7, px + TILE - 2, cy + 7, fill="#514663", width=1)
            return

        if city_map:
            half = 14
        shoulder = half + (5 if not city_map and not dungeon_map else 3)
        self.canvas.create_rectangle(cx - shoulder, cy - shoulder, cx + shoulder, cy + shoulder, fill=road_shadow, outline="")
        if up:
            self.canvas.create_rectangle(cx - shoulder, py - 1, cx + shoulder, cy, fill=road_shadow, outline="")
        if down:
            self.canvas.create_rectangle(cx - shoulder, cy, cx + shoulder, py + TILE + 1, fill=road_shadow, outline="")
        if left:
            self.canvas.create_rectangle(px - 1, cy - shoulder, cx, cy + shoulder, fill=road_shadow, outline="")
        if right:
            self.canvas.create_rectangle(cx, cy - shoulder, px + TILE + 1, cy + shoulder, fill=road_shadow, outline="")

        self.canvas.create_rectangle(cx - half, cy - half, cx + half, cy + half, fill=road_fill, outline="")
        if up:
            self.canvas.create_rectangle(cx - half, py - 1, cx + half, cy, fill=road_fill, outline="")
        if down:
            self.canvas.create_rectangle(cx - half, cy, cx + half, py + TILE + 1, fill=road_fill, outline="")
        if left:
            self.canvas.create_rectangle(px - 1, cy - half, cx, cy + half, fill=road_fill, outline="")
        if right:
            self.canvas.create_rectangle(cx, cy - half, px + TILE + 1, cy + half, fill=road_fill, outline="")

        if not city_map and not dungeon_map:
            for i in range(8):
                ox = 8 + ((wx * 11 + wy * 7 + i * 13) % 32)
                oy = 12 + ((wx * 5 + wy * 17 + i * 9) % 24)
                color = "#b78f58" if i % 2 else "#eed8a4"
                self.canvas.create_rectangle(px + ox, py + oy, px + ox + 4, py + oy + 1, fill=color, outline="")

        if up or down:
            self.canvas.create_line(cx, py + 2, cx, py + TILE - 2, fill=road_highlight, width=2)
        if left or right:
            self.canvas.create_line(px + 2, cy, px + TILE - 2, cy, fill=road_highlight, width=2)
        if city_map:
            for y in range(py + 8, py + TILE, 12):
                self.canvas.create_line(cx - half + 2, y, cx + half - 2, y, fill="#6f6659", width=1, stipple="gray50")
            for x in range(px + 8, px + TILE, 12):
                self.canvas.create_line(x, cy - half + 2, x, cy + half - 2, fill="#6f6659", width=1, stipple="gray50")

    def draw_water_shimmer(self, sx: int, sy: int, wx: int, wy: int) -> None:
        phase = (self.frame + wx * 5 + wy * 7) % 48
        px = sx * TILE
        py = sy * TILE
        self.canvas.create_line(px + phase, py + 8, px + min(TILE, phase + 14), py + 8, fill="#92dcff", width=1)
        self.canvas.create_line(px + ((phase + 18) % TILE), py + 28, px + min(TILE, (phase + 18) % TILE + 10), py + 28, fill="#6bc7f2", width=1)

    def draw_world(self) -> None:
        self.world_renderer.draw()

    def draw_world_full(self) -> None:
        city_map = self.current_map_id.startswith("city_")
        dungeon_map = self.current_map_id.startswith("dungeon_")
        house_map = self.current_map_id.startswith("house_")
        overworld = self.current_map_id == OVERWORLD_ID
        self._drawn_city_buildings.clear()
        visible_tiles: list[tuple[int, int, int, int, str]] = []
        for sy in range(MAP_ROWS):
            wy = self.camera_y + sy
            for sx in range(MAP_COLS):
                wx = self.camera_x + sx
                tile = tile_at(self.current_map_id, wx, wy)
                visible_tiles.append((sx, sy, wx, wy, tile))
                if city_map:
                    self.draw_city_ground_tile(sx, sy, wx, wy, tile)
                else:
                    name = TILE_ASSETS[tile]
                    if overworld and tile == "m":
                        name = self.mountain_base_asset(wx, wy)
                    elif overworld and tile == "q":
                        name = self.mountain_pass_base_asset(wx, wy)
                    elif overworld and tile == "r":
                        name = self.path_base_asset(wx, wy)
                    elif overworld and tile == "c":
                        name = self.settlement_base_asset(wx, wy)
                    elif overworld and tile == "u":
                        name = self.settlement_base_asset(wx, wy)
                    elif dungeon_map and tile in {"r", "d"}:
                        name = "dungeon_floor"
                    elif dungeon_map and tile == "x":
                        name = "dungeon_wall"
                    variant = self.terrain_variant(wx, wy, tile)
                    blended = tile in {"g", "f", "s", "n", "v", "b", "m", "q", "w", "r", "p", "j", "l", "a", "y", "x"}
                    if overworld and tile == "c":
                        blended = True
                    draw_size = TILE + 2 if blended else TILE
                    draw_x = sx * TILE - 1 if blended else sx * TILE
                    draw_y = sy * TILE - 1 if blended else sy * TILE
                    self.canvas.create_image(draw_x, draw_y, anchor="nw", image=self.assets.get(name, draw_size, variant=variant))
                    if overworld:
                        self.draw_biome_blend(sx, sy, wx, wy, tile)
                        self.draw_mountain_foothill_blend(sx, sy, wx, wy)
                        self.draw_location_ground(sx, sy, wx, wy, tile)
                        if tile != "r":
                            self.draw_biome_detail(sx, sy, wx, wy, tile)
                if house_map and tile in {"i", "e", "z"}:
                    self.draw_house_floor_tile(sx, sy, wx, wy, tile)
                elif house_map and tile == "k":
                    self.draw_house_furniture_tile(sx, sy, wx, wy)
                elif house_map and tile == "o":
                    self.draw_house_wall_tile(sx, sy, wx, wy)
                elif tile == "w":
                    self.draw_water_shimmer(sx, sy, wx, wy)
                    if overworld:
                        self.draw_coastline_overlay(sx, sy, wx, wy)
                elif tile == "p" and not city_map:
                    self.draw_city_plaza(sx, sy, wx, wy)
                elif tile in {"a", "y", "t"}:
                    self.draw_city_feature_tile(sx, sy, wx, wy, tile)
                elif tile == "h":
                    if city_building_at(self.current_map_id, wx, wy) is None:
                        self.draw_city_house(sx, sy, wx, wy)
                elif tile == "x":
                    self.draw_stone_wall(sx, sy, wx, wy)
                elif tile == "d" and dungeon_map:
                    self.draw_dungeon_floor_detail(sx, sy, wx, wy)
                if not city_map and not (overworld and tile == "c"):
                    self.draw_path_overlay(sx, sy, wx, wy, tile, city_map, dungeon_map)
        if overworld:
            for sx, sy, wx, wy, tile in visible_tiles:
                if tile == "m":
                    self.draw_mountain_massif(sx, sy, wx, wy)
            for sx, sy, wx, wy, tile in visible_tiles:
                if tile == "r":
                    self.draw_path_overlay(sx, sy, wx, wy, tile, False, False)
                    self.draw_biome_detail(sx, sy, wx, wy, tile)
            for sx, sy, wx, wy, tile in visible_tiles:
                if tile == "c" and self.is_city_cluster_center(wx, wy):
                    self.draw_overworld_city_cluster(sx, sy, wx, wy)
                elif tile == "u" and self.is_village_cluster_center(wx, wy):
                    self.draw_overworld_village_cluster(sx, sy, wx, wy)

        self._drawn_city_buildings.clear()
        if city_map:
            for building in city_buildings_for_map(self.current_map_id):
                if building.x2 < self.camera_x - 5 or building.x1 > self.camera_x + MAP_COLS + 5:
                    continue
                if building.y2 < self.camera_y - 5 or building.y1 > self.camera_y + MAP_ROWS + 3:
                    continue
                self.draw_city_building_entity(
                    building.x1 - self.camera_x,
                    building.y1 - self.camera_y,
                    building.x1,
                    building.y1,
                    building,
                )
        else:
            for sx, sy, wx, wy, tile in visible_tiles:
                if tile == "h" and city_building_at(self.current_map_id, wx, wy) is not None:
                    self.draw_city_house(sx, sy, wx, wy)

        self.draw_forest_liveliness()
        for npc in npcs_for_map(self.current_map_id):
            if self.camera_x <= npc.x < self.camera_x + MAP_COLS and self.camera_y <= npc.y < self.camera_y + MAP_ROWS:
                sx = npc.x - self.camera_x
                sy = npc.y - self.camera_y
                pulse = int(2 * math.sin((self.frame + npc.x * 3 + npc.y * 5) / 8))
                self.canvas.create_oval(
                    sx * TILE + 12,
                    sy * TILE + TILE - 10,
                    sx * TILE + TILE - 12,
                    sy * TILE + TILE - 4,
                    fill="#111820",
                    outline="",
                )
                self.canvas.create_image(
                    sx * TILE + TILE // 2,
                    sy * TILE + TILE // 2 - pulse,
                    image=self.assets.get_character_pose(
                        f"{npc.sprite}_model" if self.assets.get_native(f"{npc.sprite}_model") else npc.sprite,
                        size=int(round(TILE * 1.16)),
                        facing="down",
                        step=(self.frame // 12) % 4,
                    ),
                )
                marker = self.npc_marker(npc)
                if marker:
                    self.canvas.create_image(sx * TILE + TILE // 2, sy * TILE + 8, image=self.assets.get(marker, 20))

        bob = int(3 * math.sin(self.frame / 4))
        px = int((self.render_x - self.camera_x) * TILE + TILE // 2)
        py = int((self.render_y - self.camera_y) * TILE + TILE // 2 - bob)
        moving = abs(self.player_x - self.render_x) > 0.03 or abs(self.player_y - self.render_y) > 0.03
        step = (self.frame // 3) % 4 if moving else (self.walk_frame if self.walk_timer > 0 else 0)
        self.canvas.create_image(
            px,
            py,
            image=self.assets.get_class_character_pose(self.player_class_name, size=int(round(TILE * 1.16)), facing=self.facing, step=step),
        )
        self.draw_cloud_layer()
        self.draw_sidebar()

    def draw_sidebar(self) -> None:
        assert self.player
        left = MAP_COLS * TILE
        s = self.ui_scale

        def px(value: int) -> int:
            return int(value * s)

        def font(size: int, weight: str | None = None) -> tuple[str, int] | tuple[str, int, str]:
            scaled = max(7, int(round(size * s)))
            return ("Segoe UI", scaled, weight) if weight else ("Segoe UI", scaled)

        self.canvas.create_rectangle(left, 0, WIDTH, HEIGHT, fill="#202431", outline="")
        self.canvas.create_text(left + px(24), px(24), anchor="w", text=self.player.name, fill="#ffffff", font=font(19, "bold"))
        self.canvas.create_text(left + px(24), px(50), anchor="w", text=f"Lv {self.player.level}  Gold {self.player.gold}", fill="#d5dae6", font=font(12))
        self.bar(left + px(24), px(82), self.player.hp, self.player.max_hp, "#d95f5f", "HP")
        self.bar(left + px(24), px(112), self.player.mp, self.player.max_mp, "#5b86d6", "MP")
        self.bar(left + px(24), px(142), self.player.xp, self.player.xp_to_next(), "#9b7ce1", "XP")
        self.canvas.create_text(
            left + px(24),
            px(174),
            anchor="w",
            text=f"Place: {describe_tile(self.current_map_id, self.player_x, self.player_y)}",
            fill="#f4d58d",
            font=font(12, "bold"),
        )
        self.canvas.create_text(
            left + px(24),
            px(194),
            anchor="w",
            text=f"Map: {map_name(self.current_map_id)} ({self.player_x},{self.player_y})",
            fill="#c3ccdc",
            font=font(10),
        )

        self.add_button(left + px(96), px(220), left + px(156), px(248), "Up", lambda: self.try_move(0, -1))
        self.add_button(left + px(36), px(254), left + px(96), px(282), "Left", lambda: self.try_move(-1, 0))
        self.add_button(left + px(96), px(254), left + px(156), px(282), "Down", lambda: self.try_move(0, 1))
        self.add_button(left + px(156), px(254), left + px(216), px(282), "Right", lambda: self.try_move(1, 0))
        self.add_button(left + px(24), px(292), left + px(216), px(322), "Quest Log", self.show_quests, fill="#394766")
        self.add_button(left + px(24), px(330), left + px(216), px(360), "World Map", self.open_world_map, fill="#3d4f42", outline="#6d8a71")
        self.add_button(left + px(24), px(368), left + px(216), px(398), f"Skills ({self.player.skill_points})", self.open_skill_tree, fill="#57456a", outline="#8f74ac")
        self.add_button(left + px(24), px(406), left + px(216), px(436), "Inventory", self.open_inventory, fill="#5b4a35", outline="#9a7a50")

        self.draw_sidebar_minimap(left + px(24), px(458), SIDEBAR_WIDTH - px(48), px(72))
        self.draw_zoom_control(left + px(24), px(546), SIDEBAR_WIDTH - px(48))

        self.canvas.create_text(left + px(24), px(602), anchor="w", text="Party", fill="#ffffff", font=font(12, "bold"))
        party_line = ", ".join(ally.name for ally in self.allies) if self.allies else "No allies recruited"
        self.canvas.create_text(left + px(24), px(624), anchor="w", width=SIDEBAR_WIDTH - px(42), text=party_line, fill="#cfd7e6", font=font(9))
        if self.player.perks:
            self.canvas.create_text(left + px(24), px(644), anchor="w", width=SIDEBAR_WIDTH - px(42), text=f"Perks: {len(self.player.perks)} unlocked", fill="#a9d6ff", font=font(9, "bold"))

        self.canvas.create_text(left + px(24), px(670), anchor="w", text="Equipped", fill="#ffffff", font=font(12, "bold"))
        ix = left + px(24)
        iy = px(692)
        for slot in ("weapon", "armor", "accessory"):
            equipped = self.player.equipped_item(slot)
            label = equipped.name if equipped else "None"
            self.canvas.create_text(ix, iy, anchor="w", text=f"{slot.title()}: {label}", fill="#d8dfec", font=font(9))
            iy += px(18)

        log_y = px(728)
        self.canvas.create_text(left + px(24), log_y, anchor="w", text="Log", fill="#ffffff", font=font(13, "bold"))
        line_gap = max(16, px(24))
        available = max(1, (HEIGHT - (log_y + px(18))) // line_gap)
        for i, msg in enumerate(self.messages[: min(6, available)]):
            self.canvas.create_text(left + px(24), log_y + px(26) + i * line_gap, anchor="nw", width=SIDEBAR_WIDTH - px(42), text=msg, fill="#cfd7e6", font=font(9))

    def draw_zoom_control(self, x: int, y: int, width: int) -> None:
        s = self.ui_scale
        label_font = ("Segoe UI", max(8, int(round(10 * s))), "bold")
        self.canvas.create_text(x, y, anchor="w", text="Zoom", fill="#d8dfec", font=label_font)
        minus_w = max(28, int(34 * s))
        plus_w = minus_w
        button_h = max(24, int(28 * s))
        track_x1 = x + minus_w + int(10 * s)
        track_x2 = x + width - plus_w - int(10 * s)
        track_y = y + int(24 * s)
        self.add_button(x, y + int(14 * s), x + minus_w, y + int(14 * s) + button_h, "-", lambda: self.adjust_zoom(-0.08), fill="#343842")
        self.add_button(x + width - plus_w, y + int(14 * s), x + width, y + int(14 * s) + button_h, "+", lambda: self.adjust_zoom(0.08), fill="#343842")
        self.canvas.create_rectangle(track_x1, track_y - 3, track_x2, track_y + 3, fill="#111722", outline="#526077")
        t = (self.zoom_scale - 0.70) / (1.55 - 0.70)
        knob_x = track_x1 + int((track_x2 - track_x1) * t)
        self.canvas.create_oval(knob_x - 6, track_y - 6, knob_x + 6, track_y + 6, fill="#f4d58d", outline="#fff1ba")
        percent = int(round(self.zoom_scale * 100))
        self.canvas.create_text(track_x2, y, anchor="e", text=f"{percent}%", fill="#93a0b6", font=("Segoe UI", max(7, int(round(9 * s)))))
        segment_count = 12
        zone_h = max(18, int(22 * s))
        for index in range(segment_count):
            x1 = track_x1 + int((track_x2 - track_x1) * index / segment_count)
            x2 = track_x1 + int((track_x2 - track_x1) * (index + 1) / segment_count)
            target = 0.70 + (1.55 - 0.70) * ((index + 0.5) / segment_count)
            self.click_zones.append(ClickZone(x1, track_y - zone_h // 2, x2, track_y + zone_h // 2, lambda z=target: self.set_zoom(z)))

    def set_zoom(self, zoom: float) -> None:
        self.zoom_scale = max(0.70, min(1.55, zoom))
        self.apply_layout(self.canvas_width, self.canvas_height)

    def draw_sidebar_minimap(self, x: int, y: int, width: int, height: int) -> None:
        rows = MAPS[OVERWORLD_ID]
        cols = len(rows[0])
        map_h = len(rows)
        scale = min(width / cols, height / map_h)
        draw_w = int(cols * scale)
        draw_h = int(map_h * scale)
        self.canvas.create_rectangle(x - 2, y - 2, x + width + 2, y + height + 2, fill="#141822", outline="#3d465a")
        ox = x + (width - draw_w) // 2
        oy = y + (height - draw_h) // 2
        self.canvas.create_image(ox, oy, anchor="nw", image=self.get_world_map_image(draw_w, draw_h))
        if self.current_map_id == OVERWORLD_ID:
            px = ox + int((self.player_x + 0.5) * scale)
            py = oy + int((self.player_y + 0.5) * scale)
        else:
            surface = self.surface_position_for_current_map()
            px = ox + int((surface[0] + 0.5) * scale)
            py = oy + int((surface[1] + 0.5) * scale)
        self.canvas.create_oval(px - 3, py - 3, px + 3, py + 3, fill="#fff4a8", outline="#1b1a10")

    def draw_world_map_overlay(self) -> None:
        assert self.player
        panel_x1, panel_y1 = 84, 54
        panel_x2, panel_y2 = WIDTH - 84, HEIGHT - 54
        self.canvas.create_rectangle(0, 0, WIDTH, HEIGHT, fill="#080b10", stipple="gray50", outline="")
        self.canvas.create_rectangle(panel_x1, panel_y1, panel_x2, panel_y2, fill="#17202b", outline="#6d7890", width=2)
        self.canvas.create_text(panel_x1 + 28, panel_y1 + 24, anchor="w", text="Alderfall World Map", fill="#f4d58d", font=("Georgia", 22, "bold"))
        self.canvas.create_text(panel_x1 + 28, panel_y1 + 52, anchor="w", text=f"Current area: {map_name(self.current_map_id)}", fill="#d8dfec", font=("Segoe UI", 11))

        rows = MAPS[OVERWORLD_ID]
        cols = len(rows[0])
        map_h = len(rows)
        map_x1 = panel_x1 + 34
        map_y1 = panel_y1 + 86
        map_x2 = panel_x2 - 270
        map_y2 = panel_y2 - 34
        scale = min((map_x2 - map_x1) / cols, (map_y2 - map_y1) / map_h)
        draw_w = int(cols * scale)
        draw_h = int(map_h * scale)
        ox = map_x1 + ((map_x2 - map_x1) - draw_w) // 2
        oy = map_y1 + ((map_y2 - map_y1) - draw_h) // 2
        self.world_map_draw_bounds = (ox, oy, draw_w, draw_h, scale)
        map_outline = "#ffcc75" if self.map_painter_enabled else "#344356"
        self.canvas.create_rectangle(ox - 8, oy - 8, ox + draw_w + 8, oy + draw_h + 8, fill="#0c1118", outline=map_outline, width=2)
        self.canvas.create_image(ox, oy, anchor="nw", image=self.get_world_map_image(draw_w, draw_h))

        if self.current_map_id == OVERWORLD_ID:
            vx1 = ox + int(self.camera_x * scale)
            vy1 = oy + int(self.camera_y * scale)
            vx2 = ox + int((self.camera_x + MAP_COLS) * scale)
            vy2 = oy + int((self.camera_y + MAP_ROWS) * scale)
            self.canvas.create_rectangle(vx1, vy1, vx2, vy2, outline="#fff4a8", width=2)
            player_world_x, player_world_y = self.player_x, self.player_y
        else:
            player_world_x, player_world_y = self.surface_position_for_current_map()

        self.draw_world_map_markers(ox, oy, scale)
        px = ox + int((player_world_x + 0.5) * scale)
        py = oy + int((player_world_y + 0.5) * scale)
        pulse = 3 + (self.frame // 8) % 3
        self.canvas.create_oval(px - pulse, py - pulse, px + pulse, py + pulse, fill="#fff4a8", outline="#2a2108", width=2)
        self.canvas.create_text(px + 9, py - 9, anchor="w", text="You", fill="#fff4a8", font=("Segoe UI", 9, "bold"))

        legend_x = panel_x2 - 236
        legend_y = panel_y1 + 94
        self.canvas.create_text(legend_x, legend_y - 34, anchor="w", text="Regions", fill="#ffffff", font=("Segoe UI", 14, "bold"))
        legend = [
            ("Meadow", "g"),
            ("Oldwood", "f"),
            ("Sunsteppe", "s"),
            ("Frostfield", "n"),
            ("Marsh", "v"),
            ("Badlands", "b"),
            ("Mountains", "m"),
            ("Water", "w"),
            ("Roads", "r"),
            ("Cities", "c"),
            ("Villages", "u"),
            ("Dungeons", "d"),
        ]
        for i, (label, tile) in enumerate(legend):
            y = legend_y + i * 20
            self.canvas.create_rectangle(legend_x, y, legend_x + 16, y + 16, fill=self.map_tile_color(tile), outline="#111820")
            self.canvas.create_text(legend_x + 26, y + 8, anchor="w", text=label, fill="#d8dfec", font=("Segoe UI", 10))

        marker_y = panel_y2 - 242
        self.canvas.create_text(legend_x, marker_y, anchor="w", text="Markers", fill="#ffffff", font=("Segoe UI", 13, "bold"))
        self.canvas.create_oval(legend_x, marker_y + 22, legend_x + 14, marker_y + 36, fill="#f4d58d", outline="#111820")
        self.canvas.create_text(legend_x + 26, marker_y + 29, anchor="w", text="City gate", fill="#d8dfec", font=("Segoe UI", 10))
        self.canvas.create_rectangle(legend_x + 2, marker_y + 52, legend_x + 14, marker_y + 64, fill="#9b7ce1", outline="#111820")
        self.canvas.create_text(legend_x + 26, marker_y + 58, anchor="w", text="Dungeon", fill="#d8dfec", font=("Segoe UI", 10))

        painter_y = panel_y2 - 152
        self.canvas.create_text(legend_x, painter_y, anchor="w", text="Painter", fill="#ffffff", font=("Segoe UI", 13, "bold"))
        state_label = "On" if self.map_painter_enabled else "Off"
        state_fill = "#446840" if self.map_painter_enabled else "#4b3d3d"
        self.add_button(
            legend_x + 124,
            painter_y - 13,
            legend_x + 194,
            painter_y + 13,
            state_label,
            self.toggle_world_map_painter,
            fill=state_fill,
        )
        self.canvas.create_text(legend_x, painter_y + 20, anchor="w", text="LMB paint, RMB erase, drag to draw", fill="#cfd7e6", font=("Segoe UI", 9))
        self.canvas.create_text(legend_x, painter_y + 36, anchor="w", text="1-8 color, P toggle, Del clear", fill="#96a8c3", font=("Segoe UI", 9))
        swatch_y = painter_y + 52
        for idx, key in enumerate(MAP_PAINT_COLORS):
            x1 = legend_x + idx * 24
            y1 = swatch_y
            x2 = x1 + 18
            y2 = y1 + 18
            outline = "#ffe8a1" if key == self.map_paint_color else "#1c2330"
            width = 2 if key == self.map_paint_color else 1
            self.canvas.create_rectangle(x1, y1, x2, y2, fill=MAP_PAINT_COLORS[key], outline=outline, width=width)
            self.canvas.create_text(x1 + 9, y2 + 10, text=key, fill="#d8dfec", font=("Segoe UI", 8))
            self.click_zones.append(ClickZone(x1, y1, x2, y2, lambda color_key=key: self.select_world_map_paint_color(color_key)))
        self.canvas.create_text(
            legend_x,
            swatch_y + 34,
            anchor="w",
            text=f"Selected: {MAP_PAINT_LABELS.get(self.map_paint_color, 'Rose')}",
            fill="#f4d58d",
            font=("Segoe UI", 9, "bold"),
        )
        self.add_button(legend_x + 124, swatch_y + 28, legend_x + 214, swatch_y + 56, "Clear", self.clear_world_map_paint, fill="#573f40")
        self.add_button(panel_x2 - 170, panel_y2 - 44, panel_x2 - 28, panel_y2 - 14, "Close", self.close_world_map, fill="#394766")

    def surface_position_for_current_map(self) -> tuple[int, int]:
        return self.surface_positions.get(self.current_map_id, (self.player_x, self.player_y))

    def draw_world_map_markers(self, ox: int, oy: int, scale: float) -> None:
        for city_map, (cx, cy) in CITY_GATE_CENTERS.items():
            x = ox + int((cx + 0.5) * scale)
            y = oy + int((cy + 0.5) * scale)
            self.canvas.create_oval(x - 5, y - 5, x + 5, y + 5, fill="#f4d58d", outline="#2a2108")
            self.canvas.create_text(x + 8, y, anchor="w", text=MAP_LABELS.get(city_map, "City").replace(" City", ""), fill="#fff2c7", font=("Segoe UI", 8, "bold"))
        for dungeon_map, (dx, dy) in DUNGEON_ENTRANCES.items():
            x = ox + int((dx + 0.5) * scale)
            y = oy + int((dy + 0.5) * scale)
            self.canvas.create_rectangle(x - 5, y - 5, x + 5, y + 5, fill="#9b7ce1", outline="#171322")
        for village_map, (vx, vy) in VILLAGE_CENTERS.items():
            x = ox + int((vx + 0.5) * scale)
            y = oy + int((vy + 0.5) * scale)
            self.canvas.create_polygon(x, y - 6, x + 6, y, x, y + 6, x - 6, y, fill="#c7b66f", outline="#201b10")
            self.canvas.create_text(x + 8, y, anchor="w", text=MAP_LABELS.get(village_map, "Village").replace(" Village", ""), fill="#e8dfaa", font=("Segoe UI", 8, "bold"))

    def draw_dialog_overlay(self) -> None:
        panel_x1, panel_y1, panel_x2, panel_y2 = 120, 500, MAP_COLS * TILE - 24, 705
        self.canvas.create_rectangle(panel_x1, panel_y1, panel_x2, panel_y2, fill="#1c2030", outline="#4b5570", width=2)
        text_x = panel_x1 + 22
        text_width = panel_x2 - panel_x1 - 44
        if self.active_dialog_npc:
            portrait_x1 = panel_x1 + 20
            portrait_y1 = panel_y1 + 20
            portrait_x2 = portrait_x1 + 116
            portrait_y2 = panel_y2 - 58
            self.draw_npc_portrait(self.active_dialog_npc, portrait_x1, portrait_y1, portrait_x2, portrait_y2)
            text_x = portrait_x2 + 22
            text_width = panel_x2 - text_x - 26
        for i, msg in enumerate(self.messages[:4]):
            self.canvas.create_text(text_x, panel_y1 + 22 + i * 30, anchor="nw", width=text_width, text=msg, fill="#e0e8f5", font=("Segoe UI", 11))
        if (
            self.active_dialog_npc
            and self.active_dialog_npc.recruit_id
            and self.active_dialog_npc.recruit_cost > 0
            and not self.is_recruited(self.active_dialog_npc.recruit_id)
        ):
            self.add_button(
                panel_x2 - 318,
                panel_y2 - 44,
                panel_x2 - 178,
                panel_y2 - 14,
                f"Hire {self.active_dialog_npc.recruit_cost}",
                lambda npc=self.active_dialog_npc: self.hire_recruit(npc),
                fill="#5b4a35",
                outline="#9a7a50",
            )
        self.add_button(panel_x2 - 162, panel_y2 - 44, panel_x2 - 22, panel_y2 - 14, "Continue", self.close_dialog, fill="#3a4864")

    def draw_npc_portrait(self, npc: NPC, x1: int, y1: int, x2: int, y2: int) -> None:
        self.canvas.create_rectangle(x1, y1, x2, y2, fill="#232b40", outline="#61708e", width=2)
        self.canvas.create_rectangle(x1 + 6, y1 + 6, x2 - 6, y2 - 28, fill="#151a26", outline="#38445c")
        self.canvas.create_image((x1 + x2) // 2, y1 + 52, image=self.assets.get(npc.sprite, 96))
        self.canvas.create_text((x1 + x2) // 2, y2 - 16, text=npc.name, fill="#f4d58d", font=("Segoe UI", 9, "bold"), width=x2 - x1 - 12)

    def draw_shop_overlay(self) -> None:
        if not self.active_shop_npc:
            return
        assert self.player
        shop = SHOPS[self.active_shop_npc.shop_id]
        panel_x1, panel_y1, panel_x2, panel_y2 = 72, 110, MAP_COLS * TILE - 40, 690
        self.canvas.create_rectangle(panel_x1, panel_y1, panel_x2, panel_y2, fill="#1a2030", outline="#5a6884", width=2)
        self.draw_npc_portrait(self.active_shop_npc, panel_x1 + 18, panel_y1 + 18, panel_x1 + 130, panel_y1 + 144)
        self.canvas.create_text(panel_x1 + 148, panel_y1 + 24, anchor="w", text=shop["name"], fill="#ffffff", font=("Segoe UI", 18, "bold"))
        self.canvas.create_text(panel_x2 - 24, panel_y1 + 22, anchor="e", text=f"Gold: {self.player.gold}", fill="#f4d58d", font=("Segoe UI", 12, "bold"))
        if self.active_shop_npc.recruit_id and self.active_shop_npc.recruit_cost > 0:
            if self.is_recruited(self.active_shop_npc.recruit_id):
                self.canvas.create_text(panel_x2 - 24, panel_y1 + 60, anchor="e", text="Travels with you", fill="#9ed49b", font=("Segoe UI", 10, "bold"))
            else:
                self.add_button(
                    panel_x2 - 172,
                    panel_y1 + 48,
                    panel_x2 - 30,
                    panel_y1 + 82,
                    f"Hire {self.active_shop_npc.recruit_cost}",
                    lambda npc=self.active_shop_npc: self.hire_recruit(npc),
                    fill="#5b4a35",
                    outline="#9a7a50",
                )

        row_y = panel_y1 + 164
        col_w = (panel_x2 - panel_x1 - 54) // 2
        for idx, item_key in enumerate(shop["stock"]):
            col = idx % 2
            row = idx // 2
            x = panel_x1 + 18 + col * (col_w + 18)
            y = row_y + row * 82
            self.canvas.create_rectangle(x, y, x + col_w, y + 72, fill="#232b40", outline="#3d4b68")
            self.canvas.create_image(x + 26, y + 36, image=self.assets.get(item_icon(item_key), 28))
            self.canvas.create_text(x + 50, y + 24, anchor="w", text=item_name(item_key), fill="#ecf1ff", font=("Segoe UI", 11, "bold"))
            effect = []
            if item_key in ITEMS:
                item = ITEMS[item_key]
                if item["heal"]:
                    effect.append(f"+{item['heal']} HP")
                if item["mp"]:
                    effect.append(f"+{item['mp']} MP")
            elif item_key in ALL_EQUIPMENT:
                gear = ALL_EQUIPMENT[item_key]
                effect.append(gear.slot.title())
                effect.extend(gear.stat_lines())
            self.canvas.create_text(x + 50, y + 48, anchor="w", width=col_w - 172, text=" / ".join(effect), fill="#b9c7df", font=("Segoe UI", 9))
            self.add_button(x + col_w - 120, y + 18, x + col_w - 14, y + 54, f"Buy {item_cost(item_key)}", lambda k=item_key: self.buy_item(k), fill="#445a35")

        for i, msg in enumerate(self.messages[:3]):
            self.canvas.create_text(panel_x1 + 148, panel_y1 + 58 + i * 24, anchor="nw", width=panel_x2 - panel_x1 - 184, text=msg, fill="#d4def2", font=("Segoe UI", 9))
        self.add_button(panel_x2 - 160, panel_y2 - 42, panel_x2 - 30, panel_y2 - 12, "Leave Shop", self.close_shop, fill="#5b3c3c")

    def draw_inventory_overlay(self) -> None:
        assert self.player
        panel_x1, panel_y1, panel_x2, panel_y2 = 64, 70, MAP_COLS * TILE - 36, HEIGHT - 44
        self.canvas.create_rectangle(panel_x1, panel_y1, panel_x2, panel_y2, fill="#1a2030", outline="#6a5f49", width=2)
        self.canvas.create_text(panel_x1 + 22, panel_y1 + 24, anchor="w", text="Inventory", fill="#ffffff", font=("Segoe UI", 18, "bold"))
        self.canvas.create_text(
            panel_x2 - 24,
            panel_y1 + 24,
            anchor="e",
            text=f"ATK {self.player.attack}  DEF {self.player.defense}  HP {self.player.hp}/{self.player.max_hp}  MP {self.player.mp}/{self.player.max_mp}",
            fill="#f4d58d",
            font=("Segoe UI", 11, "bold"),
        )

        left_col = panel_x1 + 22
        right_col = panel_x1 + 448
        self.canvas.create_text(left_col, panel_y1 + 64, anchor="w", text="Equipped", fill="#ecf1ff", font=("Segoe UI", 13, "bold"))
        y = panel_y1 + 92
        for slot in ("weapon", "armor", "accessory"):
            item = self.player.equipped_item(slot)
            self.canvas.create_rectangle(left_col, y, left_col + 380, y + 78, fill="#232b40", outline="#3d4b68")
            self.canvas.create_text(left_col + 16, y + 18, anchor="w", text=slot.title(), fill="#b9c7df", font=("Segoe UI", 9, "bold"))
            if item:
                self.canvas.create_image(left_col + 30, y + 48, image=self.assets.get(item.icon, 28))
                self.canvas.create_text(left_col + 54, y + 38, anchor="w", text=item.name, fill="#ffffff", font=("Segoe UI", 11, "bold"))
                self.canvas.create_text(left_col + 54, y + 58, anchor="w", text=" / ".join(item.stat_lines()), fill="#d8dfec", font=("Segoe UI", 9))
                self.add_button(left_col + 270, y + 24, left_col + 362, y + 56, "Unequip", lambda s=slot: self.unequip_inventory_slot(s), fill="#5b3c3c")
            else:
                self.canvas.create_text(left_col + 22, y + 48, anchor="w", text="None", fill="#8f9bb0", font=("Segoe UI", 10))
            y += 88

        self.canvas.create_text(right_col, panel_y1 + 64, anchor="w", text="Bag", fill="#ecf1ff", font=("Segoe UI", 13, "bold"))
        bag_items = sorted(self.player.inventory.items(), key=lambda pair: item_name(pair[0]))
        if not bag_items:
            self.canvas.create_text(right_col, panel_y1 + 104, anchor="w", text="Your bag is empty.", fill="#b9c7df", font=("Segoe UI", 10))
        y = panel_y1 + 92
        for item_key, count in bag_items[:12]:
            is_equipment = item_key in ALL_EQUIPMENT
            self.canvas.create_rectangle(right_col, y, panel_x2 - 22, y + 56, fill="#232b40", outline="#3d4b68")
            self.canvas.create_image(right_col + 24, y + 28, image=self.assets.get(item_icon(item_key), 24))
            suffix = f" x{count}" if count > 1 else ""
            self.canvas.create_text(right_col + 48, y + 18, anchor="w", text=f"{item_name(item_key)}{suffix}", fill="#ffffff", font=("Segoe UI", 10, "bold"))
            if is_equipment:
                gear = ALL_EQUIPMENT[item_key]
                detail = f"{gear.slot.title()} / " + " / ".join(gear.stat_lines())
                self.add_button(panel_x2 - 118, y + 13, panel_x2 - 38, y + 43, "Equip", lambda k=item_key: self.equip_inventory_item(k), fill="#445a35")
            else:
                item = ITEMS[item_key]
                bits = []
                if item["heal"]:
                    bits.append(f"+{item['heal']} HP")
                if item["mp"]:
                    bits.append(f"+{item['mp']} MP")
                detail = "Battle item / " + " / ".join(bits)
            self.canvas.create_text(right_col + 48, y + 38, anchor="w", text=detail, fill="#b9c7df", font=("Segoe UI", 9))
            y += 64

        for i, msg in enumerate(self.messages[:3]):
            self.canvas.create_text(panel_x1 + 22, panel_y2 - 74 + i * 18, anchor="w", text=msg, fill="#d4def2", font=("Segoe UI", 9))
        self.add_button(panel_x2 - 132, panel_y2 - 42, panel_x2 - 24, panel_y2 - 12, "Close", self.close_inventory, fill="#394766")

    def draw_skill_tree_overlay(self) -> None:
        assert self.player
        panel_x1, panel_y1, panel_x2, panel_y2 = 44, 40, WIDTH - 44, HEIGHT - 34
        self.canvas.create_rectangle(0, 0, WIDTH, HEIGHT, fill="#080b10", stipple="gray50", outline="")
        self.canvas.create_rectangle(panel_x1, panel_y1, panel_x2, panel_y2, fill="#171d2a", outline="#6d7890", width=2)
        self.canvas.create_text(panel_x1 + 24, panel_y1 + 22, anchor="w", text="Skill Trees", fill="#ffffff", font=("Segoe UI", 20, "bold"))
        self.canvas.create_text(
            panel_x1 + 24,
            panel_y1 + 50,
            anchor="w",
            text=f"{self.player.class_name} personal tree + common tree    Points: {self.player.skill_points}    Gold: {self.player.gold}    Spent: {skill_spent(self.player.skill_allocations)}",
            fill="#f4d58d",
            font=("Segoe UI", 11, "bold"),
        )

        detail_x = panel_x2 - 302
        tree_x1 = panel_x1 + 34
        tree_x2 = detail_x - 24
        col_w = 118
        row_h = 62
        node_w = 106
        node_h = 48
        group_origins = {
            self.player.class_name: (tree_x1 + 22, panel_y1 + 126),
            "Common": (tree_x1 + 22, panel_y1 + 394),
        }
        groups = [
            (f"{self.player.class_name} Personal", self.player.class_name, skill_tree_for_class(self.player.class_name), "#d2b66f"),
            ("Common", "Common", COMMON_SKILL_TREE, "#82c09f"),
        ]

        self.canvas.create_rectangle(tree_x1, panel_y1 + 84, tree_x2, panel_y2 - 28, fill="#131925", outline="#2f3a4e")
        for label, tree_name, _tree, color in groups:
            origin_x, origin_y = group_origins[tree_name]
            self.canvas.create_text(origin_x, origin_y - 24, anchor="w", text=label, fill=color, font=("Segoe UI", 11, "bold"))

        centers: dict[str, tuple[int, int]] = {}
        available_tree = available_skill_tree(self.player.class_name)
        for _label, tree_name, tree, _color in groups:
            origin_x, origin_y = group_origins[tree_name]
            for skill_key, node in tree.items():
                centers[skill_key] = (origin_x + node.x * col_w + node_w // 2, origin_y + node.y * row_h + node_h // 2)
        for skill_key in skill_order_for_class(self.player.class_name):
            node = available_tree[skill_key]
            x2, y2 = centers[skill_key]
            for required in node.requires:
                if required not in centers:
                    continue
                x1, y1 = centers[required]
                active = self.player.skill_rank(required) > 0
                self.canvas.create_line(x1, y1, x2, y2, fill="#8f74ac" if active else "#374052", width=3)

        for _label, tree_name, tree, _color in groups:
            origin_x, origin_y = group_origins[tree_name]
            for skill_key, node in tree.items():
                rank = self.player.skill_rank(skill_key)
                ok, _ = self.can_allocate_skill(skill_key)
                x = origin_x + node.x * col_w
                y = origin_y + node.y * row_h
                fill = "#30384a"
                outline = "#536176"
                if rank >= node.max_rank:
                    fill = "#315447"
                    outline = "#82c09f"
                elif rank > 0:
                    fill = "#384864"
                    outline = "#8aa7dc"
                elif ok:
                    fill = "#403450"
                    outline = "#b38bdb"
                self.canvas.create_rectangle(x, y, x + node_w, y + node_h, fill=fill, outline=outline, width=2)
                self.canvas.create_text(x + 7, y + 7, anchor="nw", width=node_w - 14, text=node.name, fill="#ffffff", font=("Segoe UI", 8, "bold"))
                self.canvas.create_text(x + 7, y + 30, anchor="nw", width=42, text=f"{rank}/{node.max_rank}", fill="#f4d58d", font=("Segoe UI", 8, "bold"))
                self.add_button(x + 66, y + 27, x + node_w - 7, y + 43, "+", lambda k=skill_key: self.allocate_skill(k), fill="#4d5d78" if ok else "#343842", outline=outline)
                self.add_hover_zone(x, y, x + node_w, y + node_h, node.name, self.skill_node_tooltip(node))

        detail_y = panel_y1 + 84
        self.canvas.create_rectangle(detail_x, detail_y, panel_x2 - 24, panel_y2 - 92, fill="#202737", outline="#3e4b62")
        self.canvas.create_text(detail_x + 18, detail_y + 18, anchor="w", text="Next Choices", fill="#ffffff", font=("Segoe UI", 13, "bold"))
        y = detail_y + 48
        relevant = [
            skill_key
            for skill_key in skill_order_for_class(self.player.class_name)
            if self.player.skill_rank(skill_key) > 0 or self.can_allocate_skill(skill_key)[0]
        ]
        if not relevant:
            relevant = list(skill_order_for_class(self.player.class_name)[:8])
        for skill_key in relevant[:10]:
            node = available_tree[skill_key]
            rank = self.player.skill_rank(skill_key)
            ok, reason = self.can_allocate_skill(skill_key)
            color = "#e7edf8" if ok or rank > 0 else "#8f98aa"
            self.canvas.create_text(detail_x + 18, y, anchor="nw", width=252, text=f"{node.name} {rank}/{node.max_rank}", fill=color, font=("Segoe UI", 9, "bold"))
            detail = node.description if ok or rank > 0 else reason
            self.canvas.create_text(detail_x + 18, y + 18, anchor="nw", width=252, text=detail, fill="#b9c7df", font=("Segoe UI", 8))
            y += 48

        self.canvas.create_text(detail_x + 18, panel_y2 - 124, anchor="w", text="Unlocked Abilities", fill="#ffffff", font=("Segoe UI", 11, "bold"))
        abilities = [
            available_tree[key].ability.name
            for key in skill_order_for_class(self.player.class_name)
            if available_tree[key].ability and self.player.skill_rank(key) > 0
        ]
        ability_line = ", ".join(abilities) if abilities else "None yet"
        self.canvas.create_text(detail_x + 18, panel_y2 - 102, anchor="w", width=252, text=ability_line, fill="#d4def2", font=("Segoe UI", 9))

        spent = skill_spent(self.player.skill_allocations)
        cost = skill_respec_cost(self.player.level, spent)
        self.add_button(panel_x2 - 284, panel_y2 - 62, panel_x2 - 146, panel_y2 - 26, f"Respec {cost}", self.respec_skills, fill="#66423b", outline="#a56f62")
        self.add_button(panel_x2 - 132, panel_y2 - 62, panel_x2 - 24, panel_y2 - 26, "Close", self.close_skill_tree, fill="#394766")

    def close_dialog(self) -> None:
        self.mode = "world"
        self.active_dialog_npc = None

    def battle_backdrop_name(self, terrain: str) -> str | None:
        if terrain == "c":
            return None
        if terrain == "d":
            if "blackvault" in self.current_map_id:
                return "battle_dungeon_crypt_backdrop"
            if "miredepth" in self.current_map_id or "frosthollow" in self.current_map_id:
                return "battle_dungeon_cavern_backdrop"
            return "battle_dungeon_hall_backdrop"
        if terrain == "n":
            return "battle_snow_backdrop"
        if terrain in {"s", "b"}:
            return "battle_desert_backdrop"
        if terrain in {"m", "q"}:
            return "battle_mountain_backdrop"
        if terrain in {"g", "r"}:
            return "battle_plains_backdrop"
        return "battle_forest_backdrop"

    def draw_battle_particles(self, terrain: str) -> None:
        if terrain == "n":
            for i in range(42):
                x = (i * 97 + self.frame * 3) % (WIDTH + 40) - 20
                y = 42 + ((i * 53 + self.frame * 2) % 500)
                r = 1 + (i % 2)
                self.canvas.create_oval(x - r, y - r, x + r, y + r, fill="#eef8ff", outline="")
        elif terrain in {"s", "b"}:
            for i in range(28):
                x = (i * 113 + self.frame * 5) % (WIDTH + 100) - 50
                y = 330 + ((i * 41 + self.frame) % 250)
                color = "#f3d38d" if i % 2 else "#d9b66f"
                self.canvas.create_line(x, y, x + 42, y - 3, fill=color, width=1)
        elif terrain in {"f", "v", "m"}:
            for i in range(24):
                x = (i * 131 + self.frame * 2) % (WIDTH + 80) - 40
                y = 210 + ((i * 37 + self.frame) % 360)
                color = "#b6dd72" if i % 3 else "#78a95a"
                self.canvas.create_line(x, y, x + 8, y + 3, fill=color, width=1)
        elif terrain in {"g", "r"}:
            for i in range(22):
                x = (i * 89 + self.frame * 2) % (WIDTH + 100) - 50
                y = 360 + ((i * 31 + self.frame // 2) % 250)
                color = "#e8f2b0" if i % 2 else "#9fca68"
                self.canvas.create_line(x, y, x + 18, y - 1, fill=color, width=1)

    def draw_dungeon_battle_background(self) -> None:
        self.canvas.create_rectangle(0, 0, WIDTH, HEIGHT, fill="#1d1b2a", outline="")
        for y in range(0, 288, 48):
            shade = "#343145" if (y // 48) % 2 == 0 else "#2d2a3d"
            self.canvas.create_rectangle(0, y, WIDTH, y + 48, fill=shade, outline="")
            self.canvas.create_line(0, y + 47, WIDTH, y + 47, fill="#1f1d2b", width=2)
        for x in range(0, WIDTH + 1, 96):
            self.canvas.create_line(x, 0, x, 288, fill="#242233", width=1)
            self.canvas.create_line(x + 48, 0, x + 48, 288, fill="#3e3a50", width=1)

        for x in range(-20, WIDTH + 120, 140):
            self.canvas.create_rectangle(x + 12, 94, x + 58, 294, fill="#252334", outline="#4a465b", width=2)
            self.canvas.create_oval(x + 12, 50, x + 58, 138, fill="#252334", outline="#4a465b", width=2)
            self.canvas.create_rectangle(x + 16, 94, x + 54, 294, fill="#1b1a28", outline="")
            self.canvas.create_rectangle(x + 88, 88, x + 110, 296, fill="#454356", outline="#242232")
            self.canvas.create_rectangle(x + 83, 76, x + 115, 96, fill="#565468", outline="#242232")

        for x in (182, WIDTH - 220, WIDTH // 2 + 80):
            flicker = (self.frame + x // 11) % 6
            self.canvas.create_oval(x - 34, 148 - flicker, x + 34, 222 + flicker, fill="#5f3b36", outline="", stipple="gray50")
            self.canvas.create_oval(x - 13, 163 - flicker, x + 13, 199 + flicker, fill="#ff9b45", outline="")
            self.canvas.create_oval(x - 7, 170, x + 7, 193, fill="#ffd36d", outline="")
            self.canvas.create_rectangle(x - 4, 198, x + 4, 222, fill="#34272c", outline="")

        self.canvas.create_rectangle(0, 286, WIDTH, 320, fill="#262334", outline="")
        self.canvas.create_line(0, 286, WIDTH, 286, fill="#6d667c", width=2)
        horizon_y = 320
        self.canvas.create_polygon(0, horizon_y, WIDTH, horizon_y, WIDTH, HEIGHT, 0, HEIGHT, fill="#3b354e", outline="")
        for y in range(horizon_y + 8, HEIGHT, 34):
            color = "#47405a" if ((y - horizon_y) // 34) % 2 == 0 else "#322d43"
            self.canvas.create_polygon(0, y, WIDTH, y - 20, WIDTH, y + 14, 0, y + 34, fill=color, outline="")
        center_x = WIDTH // 2
        for i, x in enumerate(range(-160, WIDTH + 220, 86)):
            drift = (i % 3 - 1) * 18
            self.canvas.create_line(center_x + drift, horizon_y, x, HEIGHT, fill="#262236", width=1)
        for i in range(52):
            x = (i * 83 + self.frame) % WIDTH
            y = horizon_y + 18 + ((i * 47) % (HEIGHT - horizon_y - 28))
            shade = "#5c536d" if i % 3 else "#29253a"
            self.canvas.create_rectangle(x, y, x + 7 + (i % 4), y + 2, fill=shade, outline="")
        for i in range(22):
            x = (i * 127 + self.frame * 2) % (WIDTH + 80) - 40
            y = 246 + ((i * 37 + self.frame) % 300)
            self.canvas.create_line(x, y, x + 10, y + 2, fill="#8c7bc0", width=1)

    def draw_clean_battle_background(self, terrain: str) -> None:
        if terrain == "d":
            backdrop_name = self.battle_backdrop_name(terrain)
            backdrop = self.assets.get_backdrop_crop(backdrop_name, WIDTH) if backdrop_name else None
            if backdrop:
                self.canvas.create_rectangle(0, 0, WIDTH, HEIGHT, fill="#1d1b2a", outline="")
                self.canvas.create_image(0, 0, anchor="nw", image=backdrop)
                for i in range(24):
                    x = (i * 127 + self.frame * 2) % (WIDTH + 80) - 40
                    y = 246 + ((i * 37 + self.frame) % 300)
                    self.canvas.create_line(x, y, x + 10, y + 2, fill="#8c7bc0", width=1)
            else:
                self.draw_dungeon_battle_background()
            return
        palettes = {
            "g": ("#6aa0d4", "#244622", "#436f39", "#91ca70"),
            "f": ("#5f8db4", "#18391f", "#355d31", "#76b964"),
            "s": ("#83a8d5", "#6b5832", "#8f7745", "#d3b76e"),
            "n": ("#92b7d8", "#556170", "#75848e", "#b8d8d3"),
            "v": ("#5a8388", "#1e4039", "#426a52", "#7dba79"),
            "b": ("#86766d", "#3e3028", "#6a4d37", "#b78c5c"),
            "m": ("#71869f", "#2e3846", "#50634e", "#93ad72"),
            "w": ("#5d94d1", "#1f4f6e", "#3f7f88", "#76c5ce"),
            "r": ("#7f8fa4", "#3f3427", "#63533a", "#c6a25f"),
            "c": ("#8592a8", "#3f4554", "#6e5f4f", "#9b876b"),
            "d": ("#403d56", "#222131", "#302d42", "#615a7f"),
        }
        sky, horizon, field, platform = palettes.get(terrain, palettes["g"])
        backdrop = None
        backdrop_name = self.battle_backdrop_name(terrain)
        if backdrop_name:
            backdrop = self.assets.get_backdrop_crop(backdrop_name, WIDTH)
        if backdrop:
            self.canvas.create_rectangle(0, 0, WIDTH, HEIGHT, fill=field, outline="")
            self.canvas.create_image(0, 0, anchor="nw", image=backdrop)
            self.draw_battle_particles(terrain)
        else:
            self.canvas.create_rectangle(0, 0, WIDTH, 260, fill=sky, outline="")
            self.canvas.create_rectangle(0, 260, WIDTH, 360, fill=horizon, outline="")
            self.canvas.create_rectangle(0, 360, WIDTH, HEIGHT, fill=field, outline="")

            for i in range(24):
                x = (i * 71 + self.frame * 2) % (WIDTH + 120) - 60
                y = 396 + ((i * 31 + self.frame // 3) % 250)
                color = "#bde88a" if i % 2 else "#75bc59"
                self.canvas.create_line(x, y, x + 16 + (i % 4) * 4, y - 1, fill=color, width=1)


    def draw_battle_background(self, terrain: str) -> None:
        if self.battle_render_mode == "clean":
            self.draw_clean_battle_background(terrain)
            return
        palettes = {
            "g": ("#6c9fd4", "#496f3c", "#243f23", "#8fc66a"),
            "f": ("#4a6f8f", "#2d4f2f", "#172b1d", "#507c45"),
            "s": ("#89a4c9", "#7b6b42", "#433722", "#c3a36a"),
            "n": ("#9fb8d8", "#7e8797", "#565f6f", "#c8d8ea"),
            "v": ("#4c6b71", "#35564a", "#203b35", "#5d876c"),
            "b": ("#786a62", "#5b4e44", "#342c27", "#927d66"),
            "m": ("#6f7788", "#4a4f60", "#2f3441", "#8d95a0"),
            "w": ("#4f79b8", "#2c4c7a", "#18345d", "#5aaed8"),
            "r": ("#8a7a64", "#69553a", "#3c2f20", "#d7b27d"),
            "c": ("#6e7484", "#565c6d", "#3f4554", "#7b6a58"),
            "d": ("#403d56", "#2b293d", "#222131", "#3a3550"),
        }
        sky, ground, horizon, accent = palettes.get(terrain, palettes["g"])
        if self.render_mode == "fast":
            self.canvas.create_rectangle(0, 0, WIDTH, 260, fill=sky, outline="")
            self.canvas.create_rectangle(0, 260, WIDTH, HEIGHT, fill=ground, outline="")
            self.canvas.create_rectangle(0, 228, WIDTH, 318, fill=horizon, outline="")
            self.canvas.create_oval(610, 350, 1060, 464, fill=accent, outline="#4f7f42")
            self.canvas.create_oval(110, 430, 550, 566, fill=accent, outline="#4f7f42")
            for i in range(12):
                x = (i * 127 + self.frame * 3) % (WIDTH + 80) - 40
                y = 342 + (i * 41) % 230
                self.canvas.create_line(x, y, x + 38, y - 2, fill="#dcefc8", width=1)
            return
        if self.render_mode == "balanced":
            self.canvas.create_rectangle(0, 0, WIDTH, 260, fill=sky, outline="")
            self.canvas.create_rectangle(0, 260, WIDTH, HEIGHT, fill=ground, outline="")
            self.canvas.create_rectangle(0, 228, WIDTH, 318, fill=horizon, outline="")
            self.canvas.create_oval(606, 348, 1050, 462, fill=accent, outline="#5fa34d", width=2)
            self.canvas.create_oval(116, 430, 544, 566, fill=accent, outline="#5fa34d", width=2)
            self.canvas.create_oval(660, 396, 940, 456, fill="#416d42", outline="", stipple="gray12")
            self.canvas.create_oval(198, 466, 482, 530, fill="#3b6540", outline="", stipple="gray12")
            for i in range(24):
                x = (i * 71 + self.frame * 2) % (WIDTH + 120) - 60
                y = 336 + ((i * 31 + self.frame // 3) % 270)
                self.canvas.create_line(x, y, x + 16 + (i % 4) * 4, y - 1, fill="#bde88a" if i % 2 else "#75bc59", width=1)
            return
        if terrain == "d":
            self.canvas.create_rectangle(0, 0, WIDTH, 140, fill="#3e3a55", outline="")
            for x in range(0, WIDTH, 96):
                self.canvas.create_rectangle(x + 12, 112, x + 32, 230, fill="#2a253a", outline="")
                self.canvas.create_rectangle(x + 62, 112, x + 82, 230, fill="#2a253a", outline="")
            for y in range(276, 560, 28):
                shade = "#2d2a3f" if (y // 28) % 2 == 0 else "#272438"
                self.canvas.create_rectangle(0, y, WIDTH, y + 28, fill=shade, outline="")
            for x in range(-60, WIDTH + 60, 58):
                self.canvas.create_line(x, 278, x + 170, 560, fill="#3a3550", width=1)
            for x in (120, WIDTH - 120):
                flicker = (self.frame // 2) % 4
                self.canvas.create_oval(x - 12, 210 - flicker, x + 12, 232 + flicker, fill="#ff9345", outline="")
                self.canvas.create_rectangle(x - 3, 232, x + 3, 246, fill="#3d2d2d", outline="")
            return

        if terrain == "c":
            self.canvas.create_rectangle(0, 0, WIDTH, 200, fill="#8592a8", outline="")
            for x in range(0, WIDTH, 90):
                self.canvas.create_rectangle(x, 150, x + 70, 236, fill="#535d72", outline="")
                self.canvas.create_rectangle(x + 8, 138, x + 20, 150, fill="#535d72", outline="")
                self.canvas.create_rectangle(x + 30, 138, x + 42, 150, fill="#535d72", outline="")
                self.canvas.create_rectangle(x + 52, 138, x + 64, 150, fill="#535d72", outline="")
            for y in range(290, 560, 30):
                shade = "#7b6a58" if (y // 30) % 2 == 0 else "#6e5f4f"
                self.canvas.create_rectangle(0, y, WIDTH, y + 30, fill=shade, outline="")
            for x in range(0, WIDTH, 56):
                self.canvas.create_line(x, 290, x + 120, 560, fill="#5e5144", width=1)
            return

        backdrop = self.assets.get_backdrop("battle_forest_backdrop", WIDTH, HEIGHT)
        if backdrop:
            self.canvas.create_image(0, 0, anchor="nw", image=backdrop)
            rendered_backdrop = True
        else:
            rendered_backdrop = False
            self.canvas.create_rectangle(0, 0, WIDTH, 260, fill=sky, outline="")
            self.canvas.create_rectangle(0, 260, WIDTH, HEIGHT, fill=ground, outline="")
            self.canvas.create_rectangle(0, 230, WIDTH, 290, fill=horizon, outline="")

        self.draw_battle_depth_layers(terrain, horizon, ground, rendered_backdrop)
        self.draw_battle_floor(terrain, ground, accent)

    def draw_battle_depth_layers(self, terrain: str, horizon: str, ground: str, rendered_backdrop: bool) -> None:
        scroll = (self.frame // 3) % 160
        treeline = self.assets.get_treeline_cutout("battle_treeline_band", WIDTH, 210, 0.30)
        hills = self.assets.get_hill_strip("battle_forest_backdrop", WIDTH, 132, 0.58)
        if rendered_backdrop:
            self.canvas.create_rectangle(0, 216, WIDTH, 304, fill=horizon, outline="")
        else:
            self.canvas.create_rectangle(0, 244, WIDTH, 314, fill=horizon, outline="")

        if not rendered_backdrop and not treeline:
            for x in range(-160 - scroll // 5, WIDTH + 180, 160):
                peak = 166 + ((x // 160) % 3) * 26
                color = "#152719" if terrain in {"g", "f", "v"} else "#303642"
                self.canvas.create_polygon(x, 282, x + 92, peak, x + 184, 282, fill=color, outline="")
        if treeline and terrain in {"g", "f", "v", "m"}:
            band_offset = (self.frame // 10) % WIDTH
            sway = int(math.sin(self.frame / 24) * 2)
            y = 144 + sway
            self.canvas.create_image(-band_offset, y, anchor="nw", image=treeline)
            self.canvas.create_image(WIDTH - band_offset, y, anchor="nw", image=treeline)
            self.canvas.create_image((WIDTH * 2) - band_offset, y, anchor="nw", image=treeline)
            self.draw_battle_wind_streaks(186, 308, "#d3f0dd", 5, 9, 32)
        elif terrain in {"g", "f", "v", "m"}:
            tree_top = 214 if rendered_backdrop else 176
            tree_base = 326 if rendered_backdrop else 306
            spacing = 92 if rendered_backdrop else 74
            for x in range(-80 - scroll, WIDTH + 120, spacing):
                trunk = "#2d3520" if terrain != "m" else "#292f3a"
                leaf = "#19351f" if terrain != "m" else "#323a49"
                self.canvas.create_rectangle(x + 31, 254, x + 39, tree_base, fill=trunk, outline="")
                self.canvas.create_polygon(x, 270, x + 35, tree_top, x + 72, 270, fill=leaf, outline="")
                self.canvas.create_polygon(x + 6, 306, x + 35, tree_top + 42, x + 68, 306, fill=leaf, outline="")
        if hills and terrain in {"g", "f", "v", "m"}:
            hill_offset = (self.frame // 7) % WIDTH
            hill_y = 252 + int(math.sin(self.frame / 30) * 1)
            self.canvas.create_image(-hill_offset, hill_y, anchor="nw", image=hills)
            self.canvas.create_image(WIDTH - hill_offset, hill_y, anchor="nw", image=hills)
            self.draw_battle_wind_streaks(hill_y + 12, hill_y + 88, "#d7edc7", 7, 8, 38)
        if terrain == "w":
            for x in range(0, WIDTH, 64):
                phase = (self.frame * 3 + x) % 64
                self.canvas.create_line(x + phase, 258, x + phase + 26, 258, fill="#a6d8ff", width=2)
        self.canvas.create_rectangle(0, 315, WIDTH, 326, fill=ground, outline="")

    def draw_battle_wind_streaks(self, y1: int, y2: int, color: str, speed: int, count: int, length: int) -> None:
        span = y2 - y1
        for i in range(count):
            y = y1 + ((i * 23 + self.frame // 3) % max(1, span))
            x = (self.frame * speed + i * 127) % (WIDTH + length * 2) - length
            wave = int(math.sin((self.frame + i * 17) / 9) * 4)
            self.canvas.create_line(x, y, x + length // 2, y + wave, x + length, y, fill=color, width=1, smooth=True)

    def draw_battle_floor(self, terrain: str, ground: str, accent: str) -> None:
        palettes = {
            "g": ("#6fba57", "#8bd56a", "#3f7f39", "#e3d08a"),
            "f": ("#4f8f4b", "#70bd5e", "#2f6037", "#bfd686"),
            "s": ("#c9ad6a", "#dfc57f", "#9d7948", "#ead48a"),
            "n": ("#b8d8d3", "#d7ece9", "#7fa7aa", "#eef6ff"),
            "v": ("#4d8a63", "#69ae76", "#285c46", "#9ed199"),
            "b": ("#9d7653", "#bd9367", "#684a38", "#d3b175"),
            "m": ("#78916a", "#a3ba7b", "#4f654e", "#d6d8a0"),
            "w": ("#4c99aa", "#78c5ce", "#2f6877", "#bcecff"),
        }
        far, mid, dark, light = palettes.get(terrain, palettes["g"])
        horizon_y = 320
        bottom_y = HEIGHT
        self.canvas.create_polygon(0, horizon_y, WIDTH, horizon_y, WIDTH, bottom_y, 0, bottom_y, fill=far, outline="")
        self.canvas.create_polygon(0, horizon_y + 26, WIDTH, horizon_y + 4, WIDTH, horizon_y + 124, 0, horizon_y + 156, fill=mid, outline="")
        self.canvas.create_polygon(0, horizon_y + 112, WIDTH, horizon_y + 80, WIDTH, bottom_y, 0, bottom_y, fill=ground, outline="")

        bands = [
            (338, "#a4dd7b", "gray75"),
            (372, "#83c864", "gray75"),
            (422, "#6cb357", "gray50"),
            (492, "#579447", "gray75"),
            (578, "#477c3d", "gray50"),
        ]
        for y, color, stipple in bands:
            self.canvas.create_polygon(0, y - 10, WIDTH, y - 22, WIDTH, y + 28, 0, y + 42, fill=color, outline="")

        self.canvas.create_oval(606, 348, 1050, 462, fill="#72c65a", outline="#5fa34d", width=2)
        self.canvas.create_oval(682, 363, 934, 426, fill="#90d66f", outline="#69af53", width=1)
        self.canvas.create_oval(116, 430, 544, 566, fill="#6fbd56", outline="#5fa34d", width=2)
        self.canvas.create_oval(216, 450, 470, 524, fill="#88d36a", outline="#69af53", width=1)
        self.canvas.create_oval(-160, 548, 522, 720, fill="#5ea64d", outline="")
        self.canvas.create_oval(810, 524, 1390, 694, fill="#609f50", outline="")
        self.canvas.create_oval(660, 396, 940, 456, fill="#416d42", outline="", stipple="gray12")
        self.canvas.create_oval(198, 466, 482, 530, fill="#3b6540", outline="", stipple="gray12")

        for i in range(38):
            x = (i * 47 + self.frame * 2) % (WIDTH + 120) - 60
            y = 334 + ((i * 31 + self.frame // 3) % 270)
            length = 10 + (i % 5) * 7 + max(0, (y - horizon_y) // 70)
            color = light if i % 4 == 0 else ("#bde88a" if i % 2 else "#75bc59")
            self.canvas.create_line(x, y, x + length, y - 1 - (i % 3), fill=color, width=1)

        for i in range(24):
            x = (i * 61 + 17) % WIDTH
            y = 348 + ((i * 37) % 242)
            blade = 5 + int((y - horizon_y) / 34) + (i % 3)
            self.canvas.create_line(x, y, x + 4, y - blade, fill=dark, width=1)
            self.canvas.create_line(x + 5, y, x + 10, y - max(4, blade - 3), fill="#9edf73", width=1)
        for i in range(22):
            x = (i * 89 + 31) % WIDTH
            y = 356 + ((i * 43) % 220)
            r = 2 + (i % 4)
            self.canvas.create_oval(x - r, y - r // 2, x + r * 2, y + r, fill="#54794a", outline="")
        self.draw_battle_wind_streaks(350, 560, "#e3f7ca", 8, 8, 44)

    def draw_status_icons(self, actor: Actor, x: int, y: int, align: str = "left") -> None:
        assert self.battle
        stacks = self.battle.get_statuses(actor)
        if not stacks:
            return
        icon_size = 24
        gap = 6
        total_w = len(stacks) * icon_size + (len(stacks) - 1) * gap
        start_x = x if align == "left" else x - total_w
        for idx, stack in enumerate(stacks):
            key = stack.effect.key
            label = STATUS_ICON_LABELS.get(key, stack.effect.name[:2].upper())
            buff = stack.effect.affect_type == "buff"
            fill = "#2f5a45" if buff else "#5f3a3a"
            outline = "#8ed1aa" if buff else "#df9c9c"
            ix = start_x + idx * (icon_size + gap)
            iy = y
            self.canvas.create_rectangle(ix, iy, ix + icon_size, iy + icon_size, fill=fill, outline=outline, width=2)
            self.canvas.create_text(ix + icon_size // 2, iy + 10, text=label, fill="#ffffff", font=("Segoe UI", 8, "bold"))
            self.canvas.create_text(ix + icon_size // 2, iy + 19, text=str(stack.turns_remaining), fill="#f7e49f", font=("Segoe UI", 7, "bold"))
            self.add_hover_zone(ix, iy, ix + icon_size, iy + icon_size, stack.effect.name, self.status_stack_tooltip(stack))

    def draw_ability_effects(
        self,
        player_x: int,
        monster_x: int,
        enemy_positions: list[tuple[int, int]] | None = None,
        party_positions: list[tuple[int, int]] | None = None,
    ) -> None:
        assert self.battle
        kind = self.battle.effect_kind
        t = self.battle.effect_timer
        if t > 0:
            pulse = max(1, t)
            if kind == "fire":
                for i in range(6):
                    ox = (i * 17 + self.frame * 5) % 70 - 35
                    oy = (i * 11 + self.frame * 3) % 40 - 20
                    r = 8 + (pulse % 4)
                    self.canvas.create_oval(
                        monster_x + ox - r,
                        300 + oy - r,
                        monster_x + ox + r,
                        300 + oy + r,
                        fill="#ff8a3d",
                        outline="#ffd37a",
                        width=1,
                    )
            elif kind == "frost":
                for i in range(8):
                    ox = (i * 14 + self.frame * 4) % 90 - 45
                    self.canvas.create_line(monster_x + ox, 250, monster_x + ox + 10, 340, fill="#8fd9ff", width=2)
            elif kind == "poison":
                for i in range(8):
                    ox = (i * 16 + self.frame * 2) % 96 - 48
                    oy = (i * 9 + self.frame * 4) % 70
                    r = 5 + (i % 3)
                    self.canvas.create_oval(monster_x + ox - r, 332 - oy - r, monster_x + ox + r, 332 - oy + r, fill="#70d45c", outline="#a7f38a")
            elif kind == "volley":
                for i in range(4):
                    y = 250 + i * 26
                    shift = (self.frame * 14 + i * 50) % 280
                    self.canvas.create_line(player_x + 40 + shift, y, player_x + 88 + shift, y + 18, fill="#d8e8ff", width=2)
            elif kind == "slash":
                self.canvas.create_arc(monster_x - 110, 210, monster_x + 40, 350, start=300, extent=140, style=tk.ARC, outline="#ffe7b3", width=5)
                self.canvas.create_arc(monster_x - 90, 220, monster_x + 60, 360, start=300, extent=120, style=tk.ARC, outline="#ffc67d", width=3)
            elif kind == "shield":
                radius = 70 - min(40, pulse * 2)
                self.canvas.create_arc(player_x - radius, 300 - radius, player_x + radius, 300 + radius, start=30, extent=300, style=tk.ARC, outline="#9ed1ff", width=4)
            elif kind == "heal":
                for r in (24, 38, 52):
                    rr = r + (20 - pulse)
                    self.canvas.create_oval(player_x - rr, 300 - rr, player_x + rr, 300 + rr, outline="#9ff0b6", width=2)
            elif kind == "item":
                for i in range(10):
                    ox = (i * 21 + self.frame * 7) % 120 - 60
                    oy = (i * 13 + self.frame * 6) % 80 - 40
                    self.canvas.create_line(player_x + ox - 4, 305 + oy, player_x + ox + 4, 305 + oy, fill="#f4f9ff", width=2)
                    self.canvas.create_line(player_x + ox, 301 + oy - 4, player_x + ox, 309 + oy + 4, fill="#f4f9ff", width=2)
            elif kind == "strike":
                for i in range(10):
                    angle = i * 36
                    dx = int(math.cos(math.radians(angle)) * (20 + pulse))
                    dy = int(math.sin(math.radians(angle)) * (20 + pulse))
                    self.canvas.create_line(monster_x, 302, monster_x + dx, 302 + dy, fill="#ffd8b2", width=2)
        if enemy_positions and party_positions:
            self.draw_monster_ability_effects(enemy_positions, party_positions)

    def draw_monster_ability_effects(self, enemy_positions: list[tuple[int, int]], party_positions: list[tuple[int, int]]) -> None:
        assert self.battle
        effect = self.battle.monster_effect
        if not effect:
            return
        if effect.source_index >= len(enemy_positions):
            return
        sx, sy = enemy_positions[effect.source_index]
        if effect.target_party_index < 0:
            tx, ty = sx, sy
        else:
            tx, ty = party_positions[effect.target_party_index] if effect.target_party_index < len(party_positions) else party_positions[0]
        t = max(1, effect.timer)
        progress = 1.0 - min(1.0, t / 20)
        cx = int(sx + (tx - sx) * progress)
        cy = int(sy - 22 + (ty - sy) * progress)
        kind = effect.kind

        if kind in {"fire", "poison", "acid", "frost", "shadow", "dust"}:
            palettes = {
                "fire": ("#ff8a3d", "#ffd37a", "#7c3226"),
                "poison": ("#79d65e", "#c9f7a8", "#31592e"),
                "acid": ("#9adf5c", "#e9f7a0", "#4d6d32"),
                "frost": ("#8fd9ff", "#e6fbff", "#456b85"),
                "shadow": ("#8d7bc9", "#c7b7ff", "#312b4d"),
                "dust": ("#d6b277", "#f3ddb0", "#8a6540"),
            }
            fill, outline, trail = palettes[kind]
            for i in range(7):
                drift = i * 0.12
                px = int(sx + (tx - sx) * min(1.0, progress + drift - 0.18))
                py = int(sy - 28 + (ty - sy) * min(1.0, progress + drift - 0.18))
                ox = ((i * 19 + self.frame * 3) % 34) - 17
                oy = ((i * 11 + self.frame * 2) % 28) - 14
                r = 4 + (i % 3) + (0 if kind == "dust" else t % 3)
                self.canvas.create_oval(px + ox - r, py + oy - r, px + ox + r, py + oy + r, fill=fill, outline=outline)
            self.canvas.create_line(sx - 10, sy - 26, cx, cy, tx + 8, ty - 52, fill=trail, width=2, smooth=True)
        elif kind == "web":
            for i in range(5):
                oy = -46 + i * 12
                self.canvas.create_line(sx - 18, sy - 36 + i * 5, tx - 34 + i * 17, ty + oy, fill="#d8e1d5", width=2)
            self.canvas.create_arc(tx - 58, ty - 104, tx + 58, ty + 12, start=25, extent=130, style=tk.ARC, outline="#f5fff0", width=2)
            self.canvas.create_arc(tx - 48, ty - 92, tx + 48, ty, start=205, extent=115, style=tk.ARC, outline="#b9cbb8", width=2)
        elif kind in {"cleave", "fang", "thorn", "bone"}:
            colors = {
                "cleave": ("#ffe7b3", "#b15f42"),
                "fang": ("#fff0cf", "#8b5441"),
                "thorn": ("#b6e17a", "#456d38"),
                "bone": ("#ded8c7", "#746f68"),
            }
            bright, dark = colors[kind]
            self.canvas.create_arc(tx - 94, ty - 122, tx + 44, ty + 22, start=295, extent=135, style=tk.ARC, outline=bright, width=5)
            self.canvas.create_arc(tx - 76, ty - 108, tx + 62, ty + 32, start=302, extent=116, style=tk.ARC, outline=dark, width=2)
            for i in range(4):
                x = tx - 34 + i * 21
                y = ty - 74 + (i % 2) * 12
                self.canvas.create_line(x, y, x + 12, y - 20, fill=bright, width=2)
        elif kind in {"sonic", "howl"}:
            for i, radius in enumerate((28, 48, 68, 88)):
                rr = radius + (20 - t) * 2
                color = "#d7e6ff" if i % 2 else "#9db7d7"
                self.canvas.create_oval(sx - rr, sy - rr - 34, sx + rr, sy + rr - 34, outline=color, width=2)
            self.canvas.create_line(sx - 14, sy - 42, tx - 18, ty - 72, fill="#b8c8e8", width=2, smooth=True)
        elif kind == "ward":
            for radius, color in ((46, "#a8d8ff"), (62, "#d7e6ff"), (78, "#7ea0c0")):
                rr = radius + (20 - t)
                self.canvas.create_arc(sx - rr, sy - rr - 24, sx + rr, sy + rr - 24, start=25, extent=310, style=tk.ARC, outline=color, width=3)
            for i in range(8):
                angle = math.radians(i * 45 + self.frame * 4)
                x = sx + int(math.cos(angle) * 52)
                y = sy - 24 + int(math.sin(angle) * 32)
                self.canvas.create_rectangle(x - 2, y - 2, x + 2, y + 2, fill="#eef8ff", outline="")

    def monster_idle_motion(self, enemy: Actor, index: int) -> tuple[int, int]:
        label = f"{enemy.name} {enemy.sprite}".lower()
        phase = self.frame + index * 13
        if "bat" in label or "wraith" in label or "stalker" in label:
            return int(math.sin(phase / 9) * 4), int(math.sin(phase / 7) * 7)
        if "slime" in label or "bog" in label:
            return 0, int(abs(math.sin(phase / 8)) * 4)
        if "imp" in label:
            return int(math.sin(phase / 5) * 2), int(math.sin(phase / 6) * 5)
        if "golem" in label or "skeleton" in label or "knight" in label or "revenant" in label:
            return 0, int(math.sin(phase / 14) * 2)
        return 0, int(math.sin(phase / 10) * 3)

    def draw_monster_ambient(self, enemy: Actor, x: int, y: int, size: int, index: int) -> None:
        if not enemy.alive:
            return
        label = f"{enemy.name} {enemy.sprite}".lower()
        phase = self.frame + index * 17
        if "imp" in label or "ember" in label:
            for i in range(4):
                ox = (i * 23 + phase * 2) % max(1, size // 2) - size // 4
                oy = (i * 13 + phase * 3) % max(1, size // 3)
                r = 3 + i % 2
                self.canvas.create_oval(x + ox - r, y + size // 4 - oy - r, x + ox + r, y + size // 4 - oy + r, fill="#ff9b45", outline="#ffd36d")
        elif "ice" in label or "frost" in label:
            for i in range(4):
                ox = (i * 29 + phase) % max(1, size // 2) - size // 4
                oy = (i * 17 + phase * 2) % max(1, size // 3) - size // 6
                self.canvas.create_polygon(x + ox, y + oy - 7, x + ox + 5, y + oy, x + ox, y + oy + 7, x + ox - 5, y + oy, fill="#d8f7ff", outline="#8fd9ff")
        elif "wraith" in label or "shadow" in label or "night" in label:
            for i in range(3):
                ox = -size // 4 + i * size // 4 + int(math.sin((phase + i * 11) / 8) * 8)
                self.canvas.create_arc(x + ox - 18, y - 52, x + ox + 18, y + 18, start=80, extent=180, style=tk.ARC, outline="#8d7bc9", width=2)
        elif "spider" in label or "slime" in label or "bog" in label:
            for i in range(4):
                ox = (i * 31 + phase) % max(1, size // 2) - size // 4
                oy = (i * 19 + phase * 2) % max(1, size // 4)
                r = 2 + i % 3
                self.canvas.create_oval(x + ox - r, y + size // 4 - oy - r, x + ox + r, y + size // 4 - oy + r, fill="#79d65e", outline="#c9f7a8")
        elif "thorn" in label:
            for i in range(5):
                ox = (i * 23 + phase) % max(1, size // 2) - size // 4
                oy = (i * 11 + phase) % max(1, size // 5)
                self.canvas.create_line(x + ox, y + size // 5 - oy, x + ox + 8, y + size // 5 - oy - 10, fill="#b6e17a", width=2)

    def draw_enemy_battle_shadow(self, x: int, y: int, sprite_size: int, alive: bool) -> None:
        width = int(sprite_size * 0.78)
        height = max(16, int(sprite_size * 0.16))
        ground_y = y + sprite_size // 2 - 8
        fill = "#31533c" if alive else "#4f6752"
        stipple = "gray25" if alive else "gray12"
        self.canvas.create_oval(
            x - width // 2,
            ground_y - height // 2,
            x + width // 2,
            ground_y + height // 2,
            fill=fill,
            outline="",
            stipple=stipple,
        )
        self.canvas.create_oval(
            x - width // 4,
            ground_y - height // 4,
            x + width // 4,
            ground_y + height // 4,
            fill="#243b2c",
            outline="",
            stipple="gray12",
        )

    def draw_target_marker(self, x: int, y: int, sprite_size: int, idle: int) -> None:
        half = sprite_size // 2
        top = y - idle - half
        bottom = y - idle + half
        left = x - half
        right = x + half
        pulse = 2 if (self.frame // 12) % 2 else 0
        self.canvas.create_rectangle(left - 8, top - 8, right + 8, bottom + 8, outline="#1a1f2c", width=6)
        self.canvas.create_rectangle(left - 8, top - 8, right + 8, bottom + 8, outline="#f3d277", width=3)
        corner = 28
        for sx in (left - 14, right + 14):
            ex = sx + corner if sx < x else sx - corner
            self.canvas.create_line(sx, top - 14, ex, top - 14, fill="#fff3b0", width=3)
            self.canvas.create_line(sx, bottom + 14, ex, bottom + 14, fill="#fff3b0", width=3)
        arrow_y = top - 38 - pulse
        self.canvas.create_polygon(
            x,
            arrow_y + 28,
            x - 20,
            arrow_y,
            x + 20,
            arrow_y,
            fill="#f4c84f",
            outline="#6b4716",
            width=2,
        )
        self.canvas.create_polygon(
            x,
            arrow_y + 20,
            x - 11,
            arrow_y + 5,
            x + 11,
            arrow_y + 5,
            fill="#fff0a8",
            outline="",
        )

    def battle_party_slots(self, terrain: str) -> list[tuple[int, int]]:
        foot_adjust = {
            "c": -10,
            "d": 8,
            "n": 4,
            "s": 8,
            "b": 8,
            "m": -4,
            "q": -4,
            "w": 6,
        }.get(terrain, 0)
        if self.battle_render_mode == "clean" and self.battle_backdrop_name(terrain):
            foot_adjust += 8
        return [
            (318, 626 + foot_adjust),
            (178, 658 + foot_adjust),
            (456, 658 + foot_adjust),
            (82, 610 + foot_adjust),
            (570, 610 + foot_adjust),
        ]

    def battle_enemy_slots(self, terrain: str, count: int) -> list[tuple[int, int]]:
        foot_adjust = {
            "c": -14,
            "d": 18,
            "n": 14,
            "s": 18,
            "b": 18,
            "m": 4,
            "q": 4,
            "w": 10,
        }.get(terrain, 8)
        if self.battle_render_mode == "clean" and self.battle_backdrop_name(terrain):
            foot_adjust += 14
        if count <= 1:
            return [(830, 626 + foot_adjust)]
        if count == 2:
            return [(760, 612 + foot_adjust), (1018, 650 + foot_adjust)]
        if count == 3:
            return [(800, 608 + foot_adjust), (1040, 650 + foot_adjust), (660, 650 + foot_adjust)]
        return [
            (795, 604 + foot_adjust),
            (1040, 642 + foot_adjust),
            (675, 642 + foot_adjust),
            (1130, 590 + foot_adjust),
            (900, 674 + foot_adjust),
        ]

    def draw_battle_actor_plate(self, actor: Actor, x: int, y: int, width: int, active: bool, accent: str) -> None:
        has_mp = actor.max_mp > 0
        height = 72 if has_mp else 52
        outline = accent if active and actor.alive else "#516075"
        fill = "#1b2332" if actor.alive else "#2a2026"
        self.canvas.create_rectangle(x, y, x + width, y + height, fill=fill, outline=outline, width=2)
        self.canvas.create_text(
            x + 12,
            y + 15,
            anchor="w",
            width=width - 72,
            text=actor.name,
            fill="#ffffff" if actor.alive else "#c9a2a2",
            font=("Segoe UI", 10, "bold"),
        )
        self.draw_status_icons(actor, x + width - 10, y + 6, "right")
        self.bar(x + 12, y + 30, actor.hp, actor.max_hp, "#d95f5f", "HP", width=width - 24, height=14)
        if has_mp:
            self.bar(x + 12, y + 50, actor.mp, actor.max_mp, "#5b86d6", "MP", width=width - 24, height=14)

    def battle_panel_actor(self, active_actor: Actor | None) -> Actor:
        assert self.battle
        if active_actor and self.battle.is_party_actor(active_actor):
            return active_actor
        living = self.battle.living_party()
        return living[0] if living else self.player

    def draw_battle_portrait_panel(self, actor: Actor, x: int, y: int, width: int, height: int) -> None:
        self.canvas.create_rectangle(x, y, x + width, y + height, fill="#121926", outline="#465773", width=2)
        portrait_bottom = y + min(68, max(54, height - 44))
        self.canvas.create_rectangle(x + 8, y + 8, x + width - 8, portrait_bottom, fill="#0b1018", outline="#2e3b50")
        portrait_size = max(52, min(68, portrait_bottom - y - 10))
        if actor is self.player:
            portrait = self.assets.get_class_character_pose(self.player_class_name, size=portrait_size, facing=self.facing, step=0, battle=True)
        else:
            portrait = self.assets.get(actor.sprite, portrait_size)
        self.canvas.create_image(x + width // 2, y + 8 + (portrait_bottom - y - 8) // 2, image=portrait)
        name_fill = "#ffffff" if actor.alive else "#c9a2a2"
        self.canvas.create_text(x + 10, y + height - 42, anchor="w", width=width - 20, text=actor.name, fill=name_fill, font=("Segoe UI", 9, "bold"))
        self.bar(x + 10, y + height - 28, actor.hp, actor.max_hp, "#d95f5f", "HP", width=width - 20, height=12)
        if actor.max_mp > 0:
            self.bar(x + 10, y + height - 13, actor.mp, actor.max_mp, "#5b86d6", "MP", width=width - 20, height=12)

    def draw_battle(self) -> None:
        assert self.battle and self.player
        battle = self.battle
        self.draw_battle_background(battle.terrain)

        idle = int(3 * math.sin(self.frame / 5))
        active_actor = battle.active_actor()
        current_enemy = battle.current_enemy()
        party_slots = self.battle_party_slots(battle.terrain)
        enemy_slots = self.battle_enemy_slots(battle.terrain, len(battle.enemies))
        positions: dict[int, tuple[int, int]] = {}

        def draw_turn_marker(x: int, y: int, size: int, color: str) -> None:
            pulse = 3 if (self.frame // 10) % 2 else 0
            half = size // 2
            self.canvas.create_oval(
                x - half - 10 - pulse,
                y + half - 28 - pulse,
                x + half + 10 + pulse,
                y + half + 10 + pulse,
                outline=color,
                width=3,
            )
            self.canvas.create_text(x, y - half - 32, text="TURN", fill=color, font=("Segoe UI", 9, "bold"))

        for idx, actor in enumerate(battle.party_members()[: len(party_slots)]):
            base_x, foot_y = party_slots[idx]
            size = 132 if actor is self.player else 96
            x = base_x + battle.player_offset + (battle.player_lunge if actor is active_actor else 0)
            y = foot_y - size // 2 + idle
            positions[id(actor)] = (x, y)
            if actor is active_actor and actor.alive:
                draw_turn_marker(x, y, size, "#9fd5ff")
            if actor.alive or (self.frame // 12) % 2 == 0:
                if actor is self.player:
                    image = self.assets.get_class_character_pose(self.player_class_name, size=size, facing=self.facing, step=0, battle=True)
                else:
                    image = self.assets.get(actor.sprite, size)
                self.canvas.create_image(x, y, image=image)

        for idx, enemy in enumerate(battle.enemies[: len(enemy_slots)]):
            base_x, foot_y = enemy_slots[idx]
            enemy_size = 136 if len(battle.enemies) > 1 else 168
            if len(battle.enemies) > 3:
                enemy_size = 116
            lunge = battle.enemy_lunges.get(idx, battle.monster_lunge if enemy is active_actor else 0)
            mx, my = self.monster_idle_motion(enemy, idx)
            ex = base_x + battle.monster_offset - lunge + mx
            ey = foot_y - enemy_size // 2 - idle + my
            positions[id(enemy)] = (ex, ey)
            faded = not enemy.alive
            if not faded or (battle.monster_fade // 16) % 2 == 0:
                self.draw_enemy_battle_shadow(ex, ey, enemy_size, enemy.alive)
                if enemy is active_actor and enemy.alive:
                    draw_turn_marker(ex, ey, enemy_size, "#ffb6a8")
                if enemy is current_enemy and enemy.alive and len(battle.living_enemies()) > 1:
                    self.draw_target_marker(ex, ey, enemy_size, 0)
                self.draw_monster_ambient(enemy, ex, ey, enemy_size, idx)
                self.canvas.create_image(ex, ey, image=self.assets.get(enemy.sprite, enemy_size))
            if enemy.alive and not battle.finished:
                half = enemy_size // 2
                self.click_zones.append(ClickZone(ex - half, ey - half, ex + half, ey + half, lambda i=idx: battle.select_enemy(i)))

        effect_source = battle.effect_source or active_actor
        effect_target = battle.effect_target or current_enemy
        source_x = positions.get(id(effect_source), (318, 464))[0] if effect_source else 318
        target_x = positions.get(id(effect_target), (795, 438))[0] if effect_target else 795
        enemy_positions = [positions[id(enemy)] for enemy in battle.enemies[: len(enemy_slots)] if id(enemy) in positions]
        party_positions = [positions[id(actor)] for actor in battle.party_members()[: len(party_slots)] if id(actor) in positions]
        if not battle.is_enemy_actor(effect_source):
            self.draw_ability_effects(source_x, target_x, enemy_positions, party_positions)
        elif enemy_positions and party_positions:
            self.draw_monster_ability_effects(enemy_positions, party_positions)

        if battle.flash_timer:
            self.canvas.create_rectangle(0, 0, WIDTH, HEIGHT, fill="#f3f6ff", stipple="gray50", outline="")

        for floater in battle.floaters:
            self.canvas.create_text(floater.x, floater.y, text=floater.text, fill=floater.color, font=("Segoe UI", 20, "bold"))

        for idx, enemy in enumerate(battle.enemies[:5]):
            selected = enemy is current_enemy and enemy.alive and len(battle.living_enemies()) > 1
            active = enemy is active_actor or selected
            self.draw_battle_actor_plate(enemy, WIDTH - 260, 24 + idx * 60, 236, active, "#ffb6a8" if enemy is active_actor else "#f3d277")

        panel_x1, panel_y1, panel_x2, panel_y2 = 24, HEIGHT - 188, WIDTH - 24, HEIGHT - 20
        self.canvas.create_rectangle(panel_x1, panel_y1, panel_x2, panel_y2, fill="#1c2230", outline="#5d6c86", width=2)
        self.canvas.create_rectangle(panel_x1 + 8, panel_y1 + 8, panel_x2 - 8, panel_y2 - 8, outline="#2f3c52", width=1)
        self.canvas.create_line(panel_x1 + 18, panel_y1 + 18, panel_x1 + 138, panel_y1 + 18, fill="#d2b66f", width=2)
        self.canvas.create_line(panel_x2 - 138, panel_y1 + 18, panel_x2 - 18, panel_y1 + 18, fill="#d2b66f", width=2)
        self.canvas.create_rectangle(panel_x1 + 18, panel_y1 + 14, panel_x1 + 26, panel_y1 + 22, fill="#d2b66f", outline="")
        self.canvas.create_rectangle(panel_x2 - 26, panel_y1 + 14, panel_x2 - 18, panel_y1 + 22, fill="#d2b66f", outline="")
        if battle.finished:
            turn_text = "Victory" if battle.victory else "Defeat"
        elif active_actor:
            side = "Enemy" if battle.is_enemy_actor(active_actor) else "Party"
            turn_text = f"{side} turn: {active_actor.name}"
        else:
            turn_text = "Battle"
        self.canvas.create_text(panel_x1 + 28, panel_y1 + 24, anchor="w", text=turn_text, fill="#f4f9ff", font=("Segoe UI", 12, "bold"))
        if battle.living_enemies():
            self.canvas.create_text(panel_x2 - 28, panel_y1 + 24, anchor="e", text=f"Target: {current_enemy.name}", fill="#f3d277", font=("Segoe UI", 10, "bold"))

        portrait_actor = self.battle_panel_actor(active_actor)
        portrait_x = panel_x1 + 18
        portrait_y = panel_y1 + 36
        portrait_w = 144
        self.draw_battle_portrait_panel(portrait_actor, portrait_x, portrait_y, portrait_w, panel_y2 - portrait_y - 18)
        content_x = portrait_x + portrait_w + 24
        log_y = panel_y1 + 82
        self.canvas.create_rectangle(content_x, log_y, panel_x2 - 24, panel_y2 - 18, fill="#121824", outline="#303b52")

        if battle.finished:
            label = "Continue" if battle.victory else "Revive"
            fill = "#425d31" if battle.victory else "#6a3d3d"
            self.add_button(content_x, panel_y1 + 42, content_x + 140, panel_y1 + 76, label, self.resolve_battle_end, fill=fill)
        elif active_actor and battle.is_party_actor(active_actor):
            by = panel_y1 + 42
            self.add_button(content_x, by, content_x + 102, by + 34, "Attack", battle.player_attack, fill="#5c4f32")
            self.add_hover_zone(content_x, by, content_x + 102, by + 34, "Attack", "Deal physical damage with the active party member.")
            self.add_button(content_x + 108, by, content_x + 210, by + 34, "Defend", battle.player_defend, fill="#304f67")
            self.add_hover_zone(content_x + 108, by, content_x + 210, by + 34, "Defend", "Raise guard, gain MP, and apply Shield + Fortified.")
            self.add_button(content_x + 216, by, content_x + 318, by + 34, "Potion", lambda: self.use_battle_item("potion_small"), fill="#355246")
            self.add_hover_zone(content_x + 216, by, content_x + 318, by + 34, "Potion", "Use Small Potion on the active party member.")
            self.add_button(content_x + 324, by, content_x + 426, by + 34, "Ether", lambda: self.use_battle_item("ether"), fill="#364a67")
            self.add_hover_zone(content_x + 324, by, content_x + 426, by + 34, "Ether", "Use Ether on the active party member.")
            slots = [
                (content_x + 442, by),
                (content_x + 566, by),
                (content_x + 690, by),
                (content_x + 814, by),
                (content_x + 938, by),
                (content_x + 1062, by),
            ]
            for idx, ability in enumerate(active_actor.abilities[:6]):
                x, y = slots[idx]
                label = f"{ability.name} ({ability.cost})"
                if len(label) > 18:
                    label = f"{ability.name[:12]}... ({ability.cost})"
                self.add_button(x, y, x + 116, y + 30, label, lambda i=idx: battle.use_ability(i), fill="#4c355d")
                desc = f"{ability.kind.title()} ability. Cost: {ability.cost} MP.\n{self.ability_status_tooltip(ability)}"
                self.add_hover_zone(x, y, x + 116, y + 30, ability.name, desc)
        else:
            waiting = f"{active_actor.name} is acting..." if active_actor else "Resolving turn..."
            self.canvas.create_text(content_x, panel_y1 + 58, anchor="w", text=waiting, fill="#d8dfec", font=("Segoe UI", 11, "bold"))
        for i, msg in enumerate(battle.log[:6]):
            self.canvas.create_text(content_x + 12, log_y + 12 + i * 12, anchor="w", text=msg, fill="#d8dfec", font=("Segoe UI", 9))

    def draw_battle_recovery(self) -> None:
        assert self.battle and self.player
        battle = self.battle
        self.canvas.create_rectangle(0, 0, WIDTH, HEIGHT, fill="#141821", outline="")
        self.canvas.create_rectangle(0, 0, WIDTH, 250, fill="#253247", outline="")
        self.canvas.create_rectangle(0, 250, WIDTH, HEIGHT, fill="#30402c", outline="")
        for y in range(300, HEIGHT, 42):
            self.canvas.create_line(0, y, WIDTH, y - 28, fill="#3f5138", width=2)

        active_actor = battle.active_actor()
        current_enemy = battle.current_enemy()
        party_slots = self.battle_party_slots(battle.terrain)
        enemy_slots = self.battle_enemy_slots(battle.terrain, len(battle.enemies))

        def actor_token(actor: Actor, x: int, y: int, enemy: bool = False) -> None:
            fill = "#375d86" if not enemy else "#7a3d36"
            outline = "#9fd5ff" if actor is active_actor else ("#f3d277" if actor is current_enemy else "#e1e8f4")
            if not actor.alive:
                fill = "#40333a"
                outline = "#8d6b70"
            self.canvas.create_oval(x - 46, y - 82, x + 46, y + 10, fill=fill, outline="#10141d", width=5)
            self.canvas.create_oval(x - 42, y - 78, x + 42, y + 6, fill=fill, outline=outline, width=2)
            self.canvas.create_text(x, y - 36, text=actor.name[:12], fill="#ffffff", width=80, font=("Segoe UI", 9, "bold"))
            self.bar(x - 58, y + 20, actor.hp, actor.max_hp, "#d95f5f", "HP", width=116, height=13)
            if actor.max_mp > 0:
                self.bar(x - 58, y + 38, actor.mp, actor.max_mp, "#5b86d6", "MP", width=116, height=13)

        for idx, actor in enumerate(battle.party_members()[: len(party_slots)]):
            x, y = party_slots[idx]
            actor_token(actor, x, y, False)

        for idx, enemy in enumerate(battle.enemies[: len(enemy_slots)]):
            x, y = enemy_slots[idx]
            actor_token(enemy, x, y, True)
            if enemy.alive and not battle.finished:
                self.click_zones.append(ClickZone(x - 60, y - 88, x + 60, y + 58, lambda i=idx: battle.select_enemy(i)))

        panel_x1, panel_y1, panel_x2, panel_y2 = 24, HEIGHT - 188, WIDTH - 24, HEIGHT - 20
        self.canvas.create_rectangle(panel_x1, panel_y1, panel_x2, panel_y2, fill="#1c2230", outline="#5d6c86", width=2)
        if battle.finished:
            title = "Victory" if battle.victory else "Defeat"
        elif active_actor:
            side = "Enemy" if battle.is_enemy_actor(active_actor) else "Party"
            title = f"{side} turn: {active_actor.name}"
        else:
            title = "Battle"
        self.canvas.create_text(panel_x1 + 20, panel_y1 + 24, anchor="w", text=title, fill="#f4f9ff", font=("Segoe UI", 12, "bold"))
        if battle.living_enemies():
            self.canvas.create_text(panel_x2 - 20, panel_y1 + 24, anchor="e", text=f"Target: {current_enemy.name}", fill="#f3d277", font=("Segoe UI", 10, "bold"))

        content_x = panel_x1 + 20
        if battle.finished:
            label = "Continue" if battle.victory else "Revive"
            self.add_button(content_x, panel_y1 + 46, content_x + 140, panel_y1 + 80, label, self.resolve_battle_end, fill="#425d31" if battle.victory else "#6a3d3d")
            log_x = content_x + 160
        elif active_actor and battle.is_party_actor(active_actor):
            by = panel_y1 + 46
            self.add_button(content_x, by, content_x + 102, by + 34, "Attack", battle.player_attack, fill="#5c4f32")
            self.add_button(content_x + 108, by, content_x + 210, by + 34, "Defend", battle.player_defend, fill="#304f67")
            self.add_button(content_x + 216, by, content_x + 318, by + 34, "Potion", lambda: self.use_battle_item("potion_small"), fill="#355246")
            self.add_button(content_x + 324, by, content_x + 426, by + 34, "Ether", lambda: self.use_battle_item("ether"), fill="#364a67")
            for idx, ability in enumerate(active_actor.abilities[:4]):
                x = content_x + 442 + idx * 124
                label = f"{ability.name} ({ability.cost})"
                if len(label) > 18:
                    label = f"{ability.name[:12]}... ({ability.cost})"
                self.add_button(x, by, x + 116, by + 30, label, lambda i=idx: battle.use_ability(i), fill="#4c355d")
            log_x = content_x
        else:
            waiting = f"{active_actor.name} is acting..." if active_actor else "Resolving turn..."
            self.canvas.create_text(content_x, panel_y1 + 62, anchor="w", text=waiting, fill="#d8dfec", font=("Segoe UI", 11, "bold"))
            log_x = content_x

        log_y = panel_y1 + 94
        self.canvas.create_rectangle(log_x, log_y, panel_x2 - 20, panel_y2 - 18, fill="#121824", outline="#303b52")
        for i, msg in enumerate(battle.log[:5]):
            self.canvas.create_text(log_x + 12, log_y + 12 + i * 13, anchor="w", text=msg, fill="#d8dfec", font=("Segoe UI", 9))

    def bar(self, x: int, y: int, value: int, maximum: int, color: str, label: str, width: int | None = None, height: int | None = None) -> None:
        width = width if width is not None else int(170 * self.ui_scale)
        height = height if height is not None else max(14, int(18 * self.ui_scale))
        ratio = max(0.0, value) / max(1, maximum)
        self.canvas.create_rectangle(x, y, x + width, y + height, fill="#11131a", outline="#3a4050")
        self.canvas.create_rectangle(x, y, x + int(width * ratio), y + height, fill=color, outline="")
        self.canvas.create_text(
            x + int(8 * self.ui_scale),
            y + height // 2,
            anchor="w",
            text=f"{label} {value}/{maximum}",
            fill="#ffffff",
            font=("Segoe UI", max(7, int(round(9 * self.ui_scale))), "bold"),
        )


def main() -> None:
    GameApp().start()
