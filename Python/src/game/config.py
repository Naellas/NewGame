import json
from pathlib import Path
from typing import Any

WIDTH = 1440
HEIGHT = 900
TILE = 48
MAP_COLS = 24
MAP_ROWS = 17
SIDEBAR_WIDTH = WIDTH - MAP_COLS * TILE
FPS_MS = 50
RENDER_MODE = "balanced"
BATTLE_RENDER_MODE = "clean"
FAST_RENDER = RENDER_MODE == "fast"

ASSET_DIR = "assets"

PROJECT_ROOT = Path(__file__).resolve().parents[2]
GAMEPLAY_CONFIG_PATH = PROJECT_ROOT / "config" / "gameplay.json"

DEFAULT_GAMEPLAY_CONFIG: dict[str, Any] = {
    "graphics": {
        "render_mode": "balanced",
        "battle_render_mode": "clean",
    },
    "monster_spawn_chances": {
        "wild": 0.12,
        "dungeon": 0.18,
        "dungeon_entrance": 0.40,
    },
    "monster_group_chances": {
        "two_monsters": 0.30,
        "three_monsters": 0.10,
    },
}


def _deep_merge(defaults: dict[str, Any], overrides: dict[str, Any]) -> dict[str, Any]:
    merged = dict(defaults)
    for key, value in overrides.items():
        if isinstance(value, dict) and isinstance(merged.get(key), dict):
            merged[key] = _deep_merge(merged[key], value)
        else:
            merged[key] = value
    return merged


def _load_gameplay_config() -> dict[str, Any]:
    if not GAMEPLAY_CONFIG_PATH.exists():
        return dict(DEFAULT_GAMEPLAY_CONFIG)
    try:
        loaded = json.loads(GAMEPLAY_CONFIG_PATH.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return dict(DEFAULT_GAMEPLAY_CONFIG)
    if not isinstance(loaded, dict):
        return dict(DEFAULT_GAMEPLAY_CONFIG)
    return _deep_merge(DEFAULT_GAMEPLAY_CONFIG, loaded)


def _chance(section: str, key: str) -> float:
    default = float(DEFAULT_GAMEPLAY_CONFIG[section][key])
    value = GAMEPLAY_CONFIG.get(section, {}).get(key, default)
    try:
        chance = float(value)
    except (TypeError, ValueError):
        return default
    return max(0.0, min(1.0, chance))


GAMEPLAY_CONFIG = _load_gameplay_config()
RENDER_MODE = str(GAMEPLAY_CONFIG.get("graphics", {}).get("render_mode", RENDER_MODE))
BATTLE_RENDER_MODE = str(GAMEPLAY_CONFIG.get("graphics", {}).get("battle_render_mode", BATTLE_RENDER_MODE))
FAST_RENDER = RENDER_MODE == "fast"
MONSTER_SPAWN_CHANCES = {
    "wild": _chance("monster_spawn_chances", "wild"),
    "dungeon": _chance("monster_spawn_chances", "dungeon"),
    "dungeon_entrance": _chance("monster_spawn_chances", "dungeon_entrance"),
}
MONSTER_GROUP_CHANCES = {
    "two_monsters": _chance("monster_group_chances", "two_monsters"),
    "three_monsters": _chance("monster_group_chances", "three_monsters"),
}


def save_gameplay_config() -> None:
    GAMEPLAY_CONFIG_PATH.parent.mkdir(parents=True, exist_ok=True)
    GAMEPLAY_CONFIG_PATH.write_text(json.dumps(GAMEPLAY_CONFIG, indent=2), encoding="utf-8")


def set_chance(section: str, key: str, value: float) -> None:
    GAMEPLAY_CONFIG.setdefault(section, {})[key] = round(max(0.0, min(1.0, value)), 2)
    if section == "monster_spawn_chances" and key in MONSTER_SPAWN_CHANCES:
        MONSTER_SPAWN_CHANCES[key] = _chance(section, key)
    elif section == "monster_group_chances" and key in MONSTER_GROUP_CHANCES:
        MONSTER_GROUP_CHANCES[key] = _chance(section, key)
    save_gameplay_config()


def set_graphics(render_mode: str | None = None, battle_render_mode: str | None = None) -> None:
    global RENDER_MODE, BATTLE_RENDER_MODE, FAST_RENDER
    graphics = GAMEPLAY_CONFIG.setdefault("graphics", {})
    if render_mode is not None:
        RENDER_MODE = render_mode if render_mode in {"fast", "balanced", "full"} else "balanced"
        graphics["render_mode"] = RENDER_MODE
        FAST_RENDER = RENDER_MODE == "fast"
    if battle_render_mode is not None:
        BATTLE_RENDER_MODE = battle_render_mode if battle_render_mode in {"clean", "detailed"} else "clean"
        graphics["battle_render_mode"] = BATTLE_RENDER_MODE
    save_gameplay_config()
