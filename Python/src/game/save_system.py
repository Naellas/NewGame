from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from .data import QUESTS
from .entities import Ability, Actor
from .skill_tree import SKILL_TREE, skill_spent
from .world import HOUSE_EXITS, MAPS, OVERWORLD_ID, START_POSITION, TRANSITIONS, current_world_seed, set_world_seed

SAVE_VERSION = 1
SAVES_DIR = Path(__file__).resolve().parents[2] / "saves"
LEGACY_SAVE_PATH = SAVES_DIR / "savegame.json"


@dataclass(frozen=True)
class SaveSummary:
    save_id: str
    path: Path
    character_name: str
    class_name: str
    level: int
    gold: int
    map_id: str
    world_seed: int
    modified: float


def save_exists() -> bool:
    return bool(list_save_summaries())


def save_path(save_id: str) -> Path:
    return SAVES_DIR / f"{sanitize_save_id(save_id)}.json"


def sanitize_save_id(value: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "-", value.lower()).strip("-")
    return slug or "adventurer"


def unique_save_id(character_name: str) -> str:
    base = sanitize_save_id(character_name)
    candidate = base
    index = 2
    while save_path(candidate).exists():
        candidate = f"{base}-{index}"
        index += 1
    return candidate


def list_save_summaries() -> list[SaveSummary]:
    summaries: list[SaveSummary] = []
    if not SAVES_DIR.exists():
        return summaries
    for path in sorted(SAVES_DIR.glob("*.json"), key=lambda p: p.stat().st_mtime, reverse=True):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        if not isinstance(data, dict) or not isinstance(data.get("player"), dict):
            continue
        player = data["player"]
        world = data.get("world", {}) if isinstance(data.get("world"), dict) else {}
        summaries.append(
            SaveSummary(
                path.stem,
                path,
                str(player.get("name", "Arin")),
                str(player.get("class_name", "Knight")),
                int(player.get("level", 1)),
                int(player.get("gold", 0)),
                str(world.get("map_id", OVERWORLD_ID)),
                int(world.get("seed", 0)),
                path.stat().st_mtime,
            )
        )
    return summaries


def ability_to_data(ability: Ability) -> dict[str, Any]:
    return {
        "name": ability.name,
        "power": ability.power,
        "cost": ability.cost,
        "kind": ability.kind,
        "target": ability.target,
    }


def ability_from_data(data: dict[str, Any]) -> Ability:
    return Ability(
        str(data.get("name", "Strike")),
        int(data.get("power", 8)),
        int(data.get("cost", 0)),
        str(data.get("kind", "damage")),
        str(data.get("target", "enemy")),
    )


def actor_to_data(actor: Actor) -> dict[str, Any]:
    return {
        "name": actor.name,
        "sprite": actor.sprite,
        "class_name": actor.class_name,
        "max_hp": actor.max_hp,
        "hp": actor.hp,
        "max_mp": actor.max_mp,
        "mp": actor.mp,
        "attack": actor.attack,
        "defense": actor.defense,
        "level": actor.level,
        "xp": actor.xp,
        "gold": actor.gold,
        "abilities": [ability_to_data(ability) for ability in actor.abilities],
        "inventory": dict(actor.inventory),
        "perks": list(actor.perks),
        "skill_points": actor.skill_points,
        "skill_allocations": dict(actor.skill_allocations),
        "equipment": dict(actor.equipment),
    }


def actor_from_data(data: dict[str, Any]) -> Actor:
    actor = Actor(
        str(data.get("name", "Arin")),
        str(data.get("sprite", "player")),
        str(data.get("class_name", "Knight")),
        int(data.get("max_hp", 58)),
        int(data.get("hp", data.get("max_hp", 58))),
        int(data.get("max_mp", 16)),
        int(data.get("mp", data.get("max_mp", 16))),
        int(data.get("attack", 12)),
        int(data.get("defense", 4)),
        level=int(data.get("level", 1)),
        xp=int(data.get("xp", 0)),
        gold=int(data.get("gold", 0)),
    )
    actor.hp = max(0, min(actor.hp, actor.max_hp))
    actor.mp = max(0, min(actor.mp, actor.max_mp))
    actor.abilities = [
        ability_from_data(ability)
        for ability in data.get("abilities", [])
        if isinstance(ability, dict)
    ]
    inventory: dict[str, int] = {}
    for key, value in data.get("inventory", {}).items():
        try:
            amount = int(value)
        except (TypeError, ValueError):
            continue
        if amount > 0:
            inventory[str(key)] = amount
    actor.inventory = inventory
    raw_equipment = data.get("equipment", {})
    if isinstance(raw_equipment, dict):
        actor.equipment = {
            str(slot): str(item_key)
            for slot, item_key in raw_equipment.items()
            if isinstance(slot, str) and isinstance(item_key, str)
        }
    actor.perks = [str(perk) for perk in data.get("perks", [])]
    allocations: dict[str, int] = {}
    raw_allocations = data.get("skill_allocations", {})
    if isinstance(raw_allocations, dict):
        for key, value in raw_allocations.items():
            if key not in SKILL_TREE:
                continue
            try:
                rank = int(value)
            except (TypeError, ValueError):
                continue
            max_rank = SKILL_TREE[key].max_rank
            if rank > 0:
                allocations[str(key)] = min(rank, max_rank)
    actor.skill_allocations = allocations
    default_points = max(0, actor.level - 1 - skill_spent(allocations))
    try:
        actor.skill_points = max(0, int(data.get("skill_points", default_points)))
    except (TypeError, ValueError):
        actor.skill_points = default_points
    return actor


def quest_state_to_data() -> dict[str, dict[str, Any]]:
    return {
        quest_id: {
            "progress": quest.progress,
            "accepted": quest.accepted,
            "completed": quest.completed,
        }
        for quest_id, quest in QUESTS.items()
    }


def reset_quest_state() -> None:
    for quest in QUESTS.values():
        quest.progress = 0
        quest.accepted = False
        quest.completed = False


def apply_quest_state(data: dict[str, Any]) -> None:
    reset_quest_state()
    for quest_id, state in data.items():
        if quest_id not in QUESTS or not isinstance(state, dict):
            continue
        quest = QUESTS[quest_id]
        quest.progress = max(0, min(quest.needed, int(state.get("progress", 0))))
        quest.accepted = bool(state.get("accepted", False))
        quest.completed = bool(state.get("completed", False))


def build_save_data(app: Any) -> dict[str, Any]:
    if not app.player:
        raise ValueError("Start an adventure before saving.")
    map_id = app.current_map_id
    x = app.player_x
    y = app.player_y
    if str(map_id).startswith("house_"):
        exit_transition = TRANSITIONS.get((map_id, HOUSE_EXITS[0][0], HOUSE_EXITS[0][1]))
        if exit_transition:
            map_id = exit_transition.target_map_id
            x = exit_transition.target_x
            y = exit_transition.target_y
    return {
        "version": SAVE_VERSION,
        "save_id": getattr(app, "current_save_id", ""),
        "player": actor_to_data(app.player),
        "allies": [actor_to_data(ally) for ally in app.allies],
        "world": {
            "map_id": map_id,
            "x": x,
            "y": y,
            "facing": app.facing,
            "seed": getattr(app, "world_seed", current_world_seed()),
            "map_paint": app.export_world_map_paint() if hasattr(app, "export_world_map_paint") else {},
        },
        "class": {
            "selected": app.selected_class,
            "player_class_name": app.player_class_name,
        },
        "quests": quest_state_to_data(),
        "messages": list(app.messages[:8]),
    }


def apply_save_data(app: Any, data: dict[str, Any]) -> None:
    if int(data.get("version", 0)) > SAVE_VERSION:
        raise ValueError("This save was created by a newer version of the game.")
    if not isinstance(data.get("player"), dict):
        raise ValueError("Save file is missing player data.")

    app.player = actor_from_data(data["player"])
    app.allies = [
        actor_from_data(ally)
        for ally in data.get("allies", [])
        if isinstance(ally, dict)
    ]
    apply_quest_state(data.get("quests", {}))

    world = data.get("world", {})
    seed = int(world.get("seed", 0))
    set_world_seed(seed)
    app.world_seed = seed
    if hasattr(app, "clear_world_caches"):
        app.clear_world_caches()
    if hasattr(app, "import_world_map_paint"):
        app.import_world_map_paint(world.get("map_paint", {}))
    map_id = str(world.get("map_id", OVERWORLD_ID))
    if map_id not in MAPS:
        map_id = OVERWORLD_ID
    x = int(world.get("x", START_POSITION[0]))
    y = int(world.get("y", START_POSITION[1]))
    rows = MAPS[map_id]
    x = max(0, min(x, len(rows[0]) - 1))
    y = max(0, min(y, len(rows) - 1))
    app.set_map_position(map_id, x, y)
    app.facing = str(world.get("facing", "down"))
    app.walk_frame = 0
    app.walk_timer = 0

    class_data = data.get("class", {})
    app.selected_class = str(class_data.get("selected", app.player.class_name))
    app.player_class_name = str(class_data.get("player_class_name", app.player.class_name))
    app.battle = None
    if hasattr(app, "active_dialog_npc"):
        app.active_dialog_npc = None
    app.active_shop_npc = None
    app.mode = "world"
    app.messages = ["Game loaded."] + [
        str(message)
        for message in data.get("messages", [])
        if isinstance(message, str)
    ][:5]


def save_game(app: Any, save_id: str | None = None) -> Path:
    if not app.player:
        raise ValueError("Start an adventure before saving.")
    selected_id = save_id or getattr(app, "current_save_id", "")
    if not selected_id:
        selected_id = unique_save_id(app.player.name)
    selected_id = sanitize_save_id(selected_id)
    app.current_save_id = selected_id
    path = save_path(selected_id)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(build_save_data(app), indent=2), encoding="utf-8")
    return path


def load_game(app: Any, save_id: str | None = None) -> Path:
    selected_id = save_id or getattr(app, "current_save_id", "")
    path = save_path(selected_id) if selected_id else None
    if path is None or not path.exists():
        summaries = list_save_summaries()
        if summaries:
            path = summaries[0].path
        elif LEGACY_SAVE_PATH.exists():
            path = LEGACY_SAVE_PATH
        else:
            path = save_path(selected_id or "savegame")
    if not path.exists():
        raise FileNotFoundError("No save file exists yet.")
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError("Save file is not valid.")
    apply_save_data(app, data)
    app.current_save_id = path.stem
    return path
