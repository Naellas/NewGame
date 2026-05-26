from __future__ import annotations

from .equipment import ALL_EQUIPMENT
from .entities import Ability, CharacterClass, Quest
from .monsters import MONSTERS


CLASSES = {
    "Knight": CharacterClass(
        "Knight",
        max_hp=58,
        max_mp=16,
        attack=12,
        defense=4,
        abilities=[
            Ability("Shield Bash", 16, 4),
            Ability("Bulwark", 0, 3, "defend"),
            Ability("First Aid", 14, 5, "heal"),
        ],
    ),
    "Mage": CharacterClass(
        "Mage",
        max_hp=42,
        max_mp=34,
        attack=8,
        defense=2,
        abilities=[
            Ability("Firebolt", 22, 6),
            Ability("Frost Lance", 18, 5),
            Ability("Mend", 18, 5, "heal"),
        ],
    ),
    "Ranger": CharacterClass(
        "Ranger",
        max_hp=48,
        max_mp=22,
        attack=11,
        defense=3,
        abilities=[
            Ability("Piercing Shot", 18, 5),
            Ability("Rain Volley", 15, 4),
            Ability("Herbal Remedy", 16, 5, "heal"),
        ],
    ),
}

CLASS_UNLOCKS = {
    "Knight": {
        3: Ability("Guardian Slash", 23, 6),
        5: Ability("War Cry", 0, 7, "defend"),
        7: Ability("Aegis Mend", 26, 9, "heal", "ally"),
        9: Ability("Linebreaker", 38, 12),
    },
    "Mage": {
        3: Ability("Arc Nova", 28, 8),
        5: Ability("Mana Bloom", 24, 7, "heal"),
        7: Ability("Chain Spark", 34, 10),
        9: Ability("Renewing Ward", 32, 12, "heal", "ally"),
    },
    "Ranger": {
        3: Ability("Poison Arrow", 24, 6),
        5: Ability("Falcon Rush", 30, 8),
        7: Ability("Medicinal Salve", 28, 9, "heal", "ally"),
        9: Ability("Twin Fang Shot", 40, 12),
    },
}

CLASS_PERKS = {
    "Knight": {
        2: ("vitality", "Vitality: +3 extra HP on every future level."),
        4: ("shield_training", "Shield Training: Defend restores +2 more MP."),
        6: ("guardian", "Guardian: allies take 2 less damage while you stand."),
        8: ("riposte", "Riposte: basic attacks hit harder after level 8."),
    },
    "Mage": {
        2: ("focus", "Focus: +2 extra MP on every future level."),
        4: ("spark_mastery", "Spark Mastery: damaging spells gain +3 power."),
        6: ("clarity", "Clarity: recover 1 MP after casting."),
        8: ("soulflare", "Soulflare: basic attacks gain a magic surge."),
    },
    "Ranger": {
        2: ("fieldcraft", "Fieldcraft: find extra consumables from monster loot."),
        4: ("quickdraw", "Quickdraw: basic attacks hit harder."),
        6: ("triage", "Triage: healing items restore +8 HP."),
        8: ("pack_tactics", "Pack Tactics: allies strike for more damage."),
    },
}

RECRUITS = {
    "marla": {
        "name": "Marla",
        "sprite": "npc_marla",
        "class_name": "Medic",
        "max_hp": 44,
        "max_mp": 24,
        "attack": 7,
        "defense": 2,
        "abilities": [Ability("Field Mend", 18, 5, "heal", "ally")],
        "inventory": {"potion_small": 1},
    },
    "ren": {
        "name": "Ren",
        "sprite": "npc_ren",
        "class_name": "Archivist",
        "max_hp": 38,
        "max_mp": 34,
        "attack": 10,
        "defense": 2,
        "abilities": [Ability("Rune Flare", 20, 6)],
        "inventory": {"ether": 1},
    },
    "torin": {
        "name": "Torin",
        "sprite": "npc_torin",
        "class_name": "Captain",
        "max_hp": 56,
        "max_mp": 14,
        "attack": 12,
        "defense": 4,
        "abilities": [Ability("Guarding Strike", 18, 5)],
        "inventory": {"guard_tonic": 1},
    },
    "eira": {
        "name": "Eira",
        "sprite": "npc_marla",
        "class_name": "Frost Scout",
        "max_hp": 46,
        "max_mp": 20,
        "attack": 12,
        "defense": 3,
        "abilities": [Ability("Marked Shot", 19, 5), Ability("Trail Salve", 16, 5, "heal", "ally")],
        "inventory": {"potion_small": 1},
    },
    "bran": {
        "name": "Bran",
        "sprite": "npc_torin",
        "class_name": "Roadwarden",
        "max_hp": 52,
        "max_mp": 14,
        "attack": 11,
        "defense": 4,
        "abilities": [Ability("Roadwarden's Cut", 17, 4)],
        "inventory": {"guard_tonic": 1},
    },
    "niva": {
        "name": "Niva",
        "sprite": "npc_ren",
        "class_name": "Snow Seer",
        "max_hp": 40,
        "max_mp": 30,
        "attack": 9,
        "defense": 3,
        "abilities": [Ability("Blue Candle", 18, 5), Ability("Warm Hands", 18, 6, "heal", "ally")],
        "inventory": {"ether": 1},
    },
    "sela": {
        "name": "Sela",
        "sprite": "npc_marla",
        "class_name": "Dune Guide",
        "max_hp": 44,
        "max_mp": 22,
        "attack": 12,
        "defense": 2,
        "abilities": [Ability("Mirage Cut", 20, 6)],
        "inventory": {"potion_small": 1},
    },
    "fen": {
        "name": "Fen",
        "sprite": "npc_ren",
        "class_name": "Marsh Witch",
        "max_hp": 42,
        "max_mp": 32,
        "attack": 10,
        "defense": 2,
        "abilities": [Ability("Boglight Hex", 20, 6), Ability("Reed Charm", 18, 6, "heal", "ally")],
        "inventory": {"ether": 1},
    },
}

STORY_BEATS = [
    "Alderfall's ward-bells are fading, and every road between the settlements is starting to draw monsters.",
    "The city rumors now agree: something below Stonegate is waking the old dead and teaching the wilds to move together.",
    "With allies at your side, the trail points toward Sanctum and the Blackvault, where the island's oldest ward has cracked.",
]

QUESTS = {
    "slime_help": Quest(
        "slime_help",
        "Marla's Remedy",
        "Clear three Bog Slimes from the road marshes.",
        target="Bog Slime",
        needed=3,
        reward_gold=35,
        reward_xp=24,
    ),
    "crypt_lights": Quest(
        "crypt_lights",
        "Lights in the Crypt",
        "Defeat two Restless Skeletons below the old stone dungeon.",
        target="Restless Skeleton",
        needed=2,
        reward_gold=60,
        reward_xp=42,
    ),
    "orc_siege": Quest(
        "orc_siege",
        "Siege Warning",
        "Break the raider vanguard by defeating one Orc Brute.",
        target="Orc Brute",
        needed=1,
        reward_gold=95,
        reward_xp=70,
    ),
    "shadow_swarm": Quest(
        "shadow_swarm",
        "Swarm in the Rafters",
        "Defeat three Cave Bats from the old dungeon roof.",
        target="Cave Bat",
        needed=3,
        reward_gold=75,
        reward_xp=58,
    ),
    "wraith_hunt": Quest(
        "wraith_hunt",
        "Wraith Hunt",
        "Defeat one Elder Wraith and restore the crypt ward.",
        target="Elder Wraith",
        needed=1,
        reward_gold=140,
        reward_xp=110,
    ),
    "broodmother": Quest(
        "broodmother",
        "The Broodmother Below",
        "Hunt the Acid Broodmother deep in the lower vault.",
        target="Acid Broodmother",
        needed=1,
        reward_gold=210,
        reward_xp=160,
    ),
    "winter_fangs": Quest(
        "winter_fangs",
        "Winter Fangs",
        "Cull two Frost Wolves stalking the northern pass.",
        target="Frost Wolf",
        needed=2,
        reward_gold=135,
        reward_xp=105,
    ),
}

ITEMS = {
    "potion_small": {"name": "Small Potion", "icon": "icon_potion_red", "cost": 18, "heal": 28, "mp": 0},
    "potion_large": {"name": "Large Potion", "icon": "icon_potion_green", "cost": 42, "heal": 60, "mp": 0},
    "ether": {"name": "Ether", "icon": "icon_potion_blue", "cost": 28, "heal": 0, "mp": 22},
    "guard_tonic": {"name": "Guard Tonic", "icon": "icon_shield", "cost": 35, "heal": 16, "mp": 10},
    "battle_kit": {"name": "Battle Kit", "icon": "icon_sword", "cost": 55, "heal": 36, "mp": 16},
}


def item_name(item_key: str) -> str:
    if item_key in ITEMS:
        return str(ITEMS[item_key]["name"])
    if item_key in ALL_EQUIPMENT:
        return ALL_EQUIPMENT[item_key].name
    return item_key


def item_icon(item_key: str) -> str:
    if item_key in ITEMS:
        return str(ITEMS[item_key]["icon"])
    if item_key in ALL_EQUIPMENT:
        return ALL_EQUIPMENT[item_key].icon
    return "icon_chest"


def item_cost(item_key: str) -> int:
    if item_key in ITEMS:
        return int(ITEMS[item_key]["cost"])
    if item_key in ALL_EQUIPMENT:
        return ALL_EQUIPMENT[item_key].cost
    return 0

SHOPS = {
    "riverside": {"name": "Riverside Market", "stock": ["potion_small", "ether", "guard_tonic", "rusty_sword", "traveler_cloak", "iron_ring"]},
    "highwall": {"name": "Highwall Quartermaster", "stock": ["potion_small", "potion_large", "ether", "battle_kit", "iron_sword", "oak_bow", "flame_staff", "iron_mail", "vitality_charm"]},
    "crypt_vendor": {"name": "Crypt Provisioner", "stock": ["potion_large", "ether", "guard_tonic", "battle_kit", "steel_sword", "ash_bow", "frost_staff", "mage_robes", "warding_seal"]},
}
