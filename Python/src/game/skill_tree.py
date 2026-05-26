from __future__ import annotations

from dataclasses import dataclass, field

from .entities import Ability


COMMON_TREE_NAME = "Common"


@dataclass(frozen=True)
class SkillNode:
    id: str
    name: str
    description: str
    max_rank: int
    x: int
    y: int
    requires: tuple[str, ...] = ()
    effects: dict[str, int] = field(default_factory=dict)
    ability: Ability | None = None
    tree: str = COMMON_TREE_NAME


COMMON_SKILL_TREE = {
    "survival": SkillNode(
        "survival",
        "Survival",
        "+7 max HP per rank.",
        3,
        0,
        0,
        effects={"max_hp": 7},
    ),
    "merchant_sense": SkillNode(
        "merchant_sense",
        "Tradescraft",
        "Consumables restore +3 HP and +2 MP per rank.",
        3,
        2,
        0,
    ),
    "campfire_music": SkillNode(
        "campfire_music",
        "Campfire Music",
        "Learn Campfire Hymn, a modest party heal.",
        1,
        4,
        0,
        ability=Ability("Campfire Hymn", 22, 7, "heal", "ally"),
    ),
    "forager": SkillNode(
        "forager",
        "Forager",
        "Find steadier battle spoils and keep moving longer.",
        2,
        0,
        1,
        requires=("survival",),
        effects={"max_mp": 3},
    ),
    "scavenger": SkillNode(
        "scavenger",
        "Scavenger",
        "+10% battle gold per rank.",
        3,
        1,
        1,
        requires=("survival",),
    ),
    "battle_medic": SkillNode(
        "battle_medic",
        "Field Medicine",
        "Healing items restore +8 extra HP.",
        1,
        2,
        1,
        requires=("merchant_sense",),
    ),
    "shared_training": SkillNode(
        "shared_training",
        "Road Songs",
        "Allies deal +1 damage per rank.",
        3,
        3,
        1,
        requires=("campfire_music",),
    ),
    "pathfinder": SkillNode(
        "pathfinder",
        "Pathfinder",
        "+1 attack and +1 defense per rank.",
        2,
        4,
        1,
        requires=("campfire_music",),
        effects={"attack": 1, "defense": 1},
    ),
    "settlement_lore": SkillNode(
        "settlement_lore",
        "Settlement Lore",
        "+5 max MP and +1 defense per rank.",
        2,
        2,
        2,
        requires=("scavenger", "battle_medic"),
        effects={"max_mp": 5, "defense": 1},
    ),
}


KNIGHT_SKILL_TREE = {
    "iron_body": SkillNode(
        "iron_body",
        "Iron Body",
        "+9 max HP per rank.",
        3,
        0,
        0,
        effects={"max_hp": 9},
        tree="Knight",
    ),
    "weapon_training": SkillNode(
        "weapon_training",
        "Weapon Training",
        "+2 attack per rank.",
        3,
        2,
        0,
        effects={"attack": 2},
        tree="Knight",
    ),
    "warded_armor": SkillNode(
        "warded_armor",
        "Warded Armor",
        "+1 defense per rank.",
        3,
        4,
        0,
        effects={"defense": 1},
        tree="Knight",
    ),
    "power_strike": SkillNode(
        "power_strike",
        "Power Strike",
        "Learn Power Strike, a strong low-cost attack.",
        1,
        1,
        1,
        requires=("weapon_training",),
        ability=Ability("Power Strike", 30, 7),
        tree="Knight",
    ),
    "guard_mastery": SkillNode(
        "guard_mastery",
        "Guard Mastery",
        "Take 1 less damage per rank.",
        3,
        3,
        1,
        requires=("warded_armor",),
        tree="Knight",
    ),
    "stalwart_guard": SkillNode(
        "stalwart_guard",
        "Stalwart Guard",
        "Defend restores +1 extra MP per rank.",
        2,
        4,
        1,
        requires=("warded_armor",),
        tree="Knight",
    ),
    "second_wind": SkillNode(
        "second_wind",
        "Second Wind",
        "Learn Second Wind, a self-heal for long fights.",
        1,
        1,
        2,
        requires=("iron_body", "power_strike"),
        ability=Ability("Second Wind", 32, 9, "heal"),
        tree="Knight",
    ),
    "blade_flurry": SkillNode(
        "blade_flurry",
        "Blade Flurry",
        "Basic attacks gain +3 damage per rank.",
        2,
        2,
        2,
        requires=("power_strike", "guard_mastery"),
        tree="Knight",
    ),
    "heroic_resolve": SkillNode(
        "heroic_resolve",
        "Heroic Resolve",
        "+20 max HP, +2 attack, and +2 defense.",
        1,
        3,
        3,
        requires=("second_wind", "blade_flurry", "stalwart_guard"),
        effects={"max_hp": 20, "attack": 2, "defense": 2},
        tree="Knight",
    ),
}


MAGE_SKILL_TREE = {
    "battle_focus": SkillNode(
        "battle_focus",
        "Battle Focus",
        "+6 max MP per rank.",
        3,
        0,
        0,
        effects={"max_mp": 6},
        tree="Mage",
    ),
    "channeling": SkillNode(
        "channeling",
        "Channeling",
        "Healing abilities restore +4 HP per rank.",
        3,
        2,
        0,
        requires=("battle_focus",),
        tree="Mage",
    ),
    "spellcraft": SkillNode(
        "spellcraft",
        "Spellcraft",
        "Damaging abilities gain +2 power per rank.",
        3,
        4,
        0,
        effects={"max_mp": 2},
        tree="Mage",
    ),
    "mana_well": SkillNode(
        "mana_well",
        "Mana Well",
        "+4 max MP and +1 defense per rank.",
        2,
        0,
        1,
        requires=("battle_focus",),
        effects={"max_mp": 4, "defense": 1},
        tree="Mage",
    ),
    "ether_flow": SkillNode(
        "ether_flow",
        "Ether Flow",
        "Recover +1 MP per rank after using an ability.",
        2,
        2,
        1,
        requires=("channeling",),
        tree="Mage",
    ),
    "elemental_precision": SkillNode(
        "elemental_precision",
        "Elemental Precision",
        "+1 attack and +3 max MP per rank.",
        2,
        4,
        1,
        requires=("spellcraft",),
        effects={"attack": 1, "max_mp": 3},
        tree="Mage",
    ),
    "arcane_burst": SkillNode(
        "arcane_burst",
        "Arcane Burst",
        "Learn Arcane Burst, a heavy magical attack.",
        1,
        2,
        2,
        requires=("ether_flow", "elemental_precision"),
        ability=Ability("Arcane Burst", 46, 14),
        tree="Mage",
    ),
    "renewing_ward": SkillNode(
        "renewing_ward",
        "Renewing Ward",
        "Learn Renewing Ward, a strong ally heal.",
        1,
        3,
        2,
        requires=("channeling", "ether_flow"),
        ability=Ability("Renewing Ward", 34, 12, "heal", "ally"),
        tree="Mage",
    ),
    "archmage_resolve": SkillNode(
        "archmage_resolve",
        "Archmage Resolve",
        "+18 max MP, +2 attack, and +1 defense.",
        1,
        3,
        3,
        requires=("arcane_burst", "renewing_ward"),
        effects={"max_mp": 18, "attack": 2, "defense": 1},
        tree="Mage",
    ),
}


RANGER_SKILL_TREE = {
    "trail_sense": SkillNode(
        "trail_sense",
        "Trail Sense",
        "+1 attack and +1 defense per rank.",
        2,
        0,
        0,
        effects={"attack": 1, "defense": 1},
        tree="Ranger",
    ),
    "quickdraw_drills": SkillNode(
        "quickdraw_drills",
        "Quickdraw Drills",
        "+2 attack per rank.",
        3,
        2,
        0,
        effects={"attack": 2},
        tree="Ranger",
    ),
    "wild_medicine": SkillNode(
        "wild_medicine",
        "Wild Medicine",
        "+6 max HP and +3 max MP per rank.",
        2,
        4,
        0,
        effects={"max_hp": 6, "max_mp": 3},
        tree="Ranger",
    ),
    "marked_shot": SkillNode(
        "marked_shot",
        "Marked Shot",
        "Learn Marked Shot, a clean single-target attack.",
        1,
        1,
        1,
        requires=("trail_sense", "quickdraw_drills"),
        ability=Ability("Marked Shot", 29, 7),
        tree="Ranger",
    ),
    "keen_edge": SkillNode(
        "keen_edge",
        "Keen Edge",
        "+8% chance per rank for basic attacks to hit hard.",
        3,
        3,
        1,
        requires=("quickdraw_drills",),
        tree="Ranger",
    ),
    "field_salve": SkillNode(
        "field_salve",
        "Field Salve",
        "Learn Field Salve, an efficient ally heal.",
        1,
        4,
        1,
        requires=("wild_medicine",),
        ability=Ability("Field Salve", 28, 9, "heal", "ally"),
        tree="Ranger",
    ),
    "twin_fang": SkillNode(
        "twin_fang",
        "Twin Fang",
        "Learn Twin Fang, a heavy ranger strike.",
        1,
        2,
        2,
        requires=("marked_shot", "keen_edge"),
        ability=Ability("Twin Fang", 40, 12),
        tree="Ranger",
    ),
    "pack_coordination": SkillNode(
        "pack_coordination",
        "Pack Coordination",
        "Allies deal +1 more damage per rank.",
        2,
        3,
        2,
        requires=("keen_edge", "field_salve"),
        tree="Ranger",
    ),
    "warden_resolve": SkillNode(
        "warden_resolve",
        "Warden Resolve",
        "+12 max HP, +8 max MP, +2 attack, and +1 defense.",
        1,
        3,
        3,
        requires=("twin_fang", "pack_coordination"),
        effects={"max_hp": 12, "max_mp": 8, "attack": 2, "defense": 1},
        tree="Ranger",
    ),
}


CLASS_SKILL_TREES = {
    "Knight": KNIGHT_SKILL_TREE,
    "Mage": MAGE_SKILL_TREE,
    "Ranger": RANGER_SKILL_TREE,
}

SKILL_TREES = {
    COMMON_TREE_NAME: COMMON_SKILL_TREE,
    **CLASS_SKILL_TREES,
}

SKILL_TREE = {
    **COMMON_SKILL_TREE,
    **KNIGHT_SKILL_TREE,
    **MAGE_SKILL_TREE,
    **RANGER_SKILL_TREE,
}

SKILL_ORDER = tuple(SKILL_TREE)
SKILL_ABILITY_NAMES = {
    node.ability.name
    for node in SKILL_TREE.values()
    if node.ability is not None
}


def skill_tree_for_class(class_name: str) -> dict[str, SkillNode]:
    return CLASS_SKILL_TREES.get(class_name, {})


def available_skill_tree(class_name: str) -> dict[str, SkillNode]:
    return {
        **COMMON_SKILL_TREE,
        **skill_tree_for_class(class_name),
    }


def skill_order_for_class(class_name: str) -> tuple[str, ...]:
    return tuple(available_skill_tree(class_name))


def skill_spent(allocations: dict[str, int]) -> int:
    return sum(max(0, int(rank)) for rank in allocations.values())


def skill_respec_cost(level: int, spent: int) -> int:
    return 35 + max(0, level - 1) * 8 + max(0, spent) * 12
