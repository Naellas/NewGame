from __future__ import annotations

from dataclasses import dataclass

from .catalog import MONSTERS


@dataclass(frozen=True)
class MonsterAbilityStatus:
    key: str
    target: str = "target"
    chance: float = 1.0
    power: int | None = None


@dataclass(frozen=True)
class MonsterAbility:
    name: str
    power: int
    chance: float
    kind: str = "damage"
    target: str = "target"
    effect: str = "strike"
    cooldown: int = 2
    hp_below: float | None = None
    statuses: tuple[MonsterAbilityStatus, ...] = ()


MONSTER_ABILITIES: dict[str, tuple[MonsterAbility, ...]] = {
    "slime": (
        MonsterAbility("Slime Splash", 5, 0.36, effect="acid", statuses=(MonsterAbilityStatus("weak", chance=0.30),)),
    ),
    "acid_slime": (
        MonsterAbility("Caustic Bubble", 10, 0.48, effect="acid", statuses=(MonsterAbilityStatus("poison", chance=0.75),)),
    ),
    "wolf": (
        MonsterAbility("Hamstring Bite", 8, 0.34, effect="fang", statuses=(MonsterAbilityStatus("weak", chance=0.45),)),
    ),
    "dire_wolf": (
        MonsterAbility("Pack Howl", 0, 0.40, kind="buff", target="self", effect="howl", cooldown=3, statuses=(MonsterAbilityStatus("haste", "self"),)),
        MonsterAbility("Rending Bite", 14, 0.30, effect="fang", statuses=(MonsterAbilityStatus("vulnerable", chance=0.35),)),
    ),
    "frost_wolf": (
        MonsterAbility("Frost Fang", 16, 0.45, effect="frost", statuses=(MonsterAbilityStatus("weak", chance=0.75),)),
    ),
    "bat": (
        MonsterAbility("Shriek", 6, 0.35, effect="sonic", statuses=(MonsterAbilityStatus("weak", chance=0.35),)),
    ),
    "shadow_bat": (
        MonsterAbility("Night Screech", 12, 0.44, effect="shadow", statuses=(MonsterAbilityStatus("weak", chance=0.65),)),
    ),
    "night_stalker": (
        MonsterAbility("Dusk Dive", 17, 0.44, effect="shadow", statuses=(MonsterAbilityStatus("vulnerable", chance=0.45),)),
    ),
    "skeleton": (
        MonsterAbility("Bone Rattle", 7, 0.35, effect="bone", statuses=(MonsterAbilityStatus("vulnerable", chance=0.30),)),
    ),
    "bone_knight": (
        MonsterAbility("Grave Guard", 0, 0.38, kind="buff", target="self", effect="ward", cooldown=3, statuses=(MonsterAbilityStatus("fortified", "self"),)),
        MonsterAbility("Rust Cleave", 14, 0.32, effect="cleave", statuses=(MonsterAbilityStatus("vulnerable", chance=0.35),)),
    ),
    "crypt_revenant": (
        MonsterAbility("Crypt Edict", 18, 0.44, effect="bone", statuses=(MonsterAbilityStatus("weak", chance=0.60),)),
        MonsterAbility("Old Ward", 0, 0.34, kind="buff", target="self", effect="ward", cooldown=3, statuses=(MonsterAbilityStatus("shield", "self", power=9),)),
    ),
    "goblin": (
        MonsterAbility("Dirty Trick", 9, 0.36, effect="dust", statuses=(MonsterAbilityStatus("weak", chance=0.35),)),
    ),
    "goblin_scout": (
        MonsterAbility("Knife Feint", 8, 0.38, effect="slash", statuses=(MonsterAbilityStatus("vulnerable", chance=0.25),)),
    ),
    "goblin_archer": (
        MonsterAbility("Barbed Arrow", 11, 0.44, effect="pierce", statuses=(MonsterAbilityStatus("weak", chance=0.35),)),
        MonsterAbility("Duck Away", 0, 0.24, kind="buff", target="self", effect="dust", cooldown=3, statuses=(MonsterAbilityStatus("haste", "self"),)),
    ),
    "goblin_trapper": (
        MonsterAbility("Snare Toss", 10, 0.46, effect="web", statuses=(MonsterAbilityStatus("weak", chance=0.65),)),
        MonsterAbility("Hooked Jab", 13, 0.30, effect="pierce", statuses=(MonsterAbilityStatus("vulnerable", chance=0.35),)),
    ),
    "goblin_skirmisher": (
        MonsterAbility("Twin Cut", 14, 0.42, effect="slash", statuses=(MonsterAbilityStatus("vulnerable", chance=0.40),)),
    ),
    "goblin_shaman": (
        MonsterAbility("Hex Spark", 16, 0.46, effect="shadow", statuses=(MonsterAbilityStatus("weak", chance=0.65),)),
        MonsterAbility("Rattle Ward", 0, 0.28, kind="buff", target="self", effect="ward", cooldown=3, statuses=(MonsterAbilityStatus("shield", "self", power=7),)),
    ),
    "hobgoblin_guard": (
        MonsterAbility("Shield Jab", 15, 0.38, effect="strike", statuses=(MonsterAbilityStatus("weak", chance=0.45),)),
        MonsterAbility("Hold Line", 0, 0.30, kind="buff", target="self", effect="ward", cooldown=3, statuses=(MonsterAbilityStatus("fortified", "self"),)),
    ),
    "goblin_warlord": (
        MonsterAbility("Raid Leader's Roar", 0, 0.36, kind="buff", target="self", effect="howl", cooldown=3, statuses=(MonsterAbilityStatus("haste", "self"), MonsterAbilityStatus("fortified", "self"))),
        MonsterAbility("Jagged Axe", 21, 0.42, effect="cleave", statuses=(MonsterAbilityStatus("vulnerable", chance=0.55),)),
    ),
    "goblin_king": (
        MonsterAbility("Royal Decree", 0, 0.34, kind="buff", target="self", effect="howl", cooldown=3, statuses=(MonsterAbilityStatus("haste", "self"), MonsterAbilityStatus("shield", "self", power=12))),
        MonsterAbility("Crownbreaker Club", 27, 0.46, effect="strike", statuses=(MonsterAbilityStatus("vulnerable", chance=0.65),)),
        MonsterAbility("Noisy Panic", 18, 0.30, effect="sonic", cooldown=4, hp_below=0.50, statuses=(MonsterAbilityStatus("weak", chance=0.75),)),
    ),
    "orc": (
        MonsterAbility("Brutal Cleave", 13, 0.38, effect="cleave", statuses=(MonsterAbilityStatus("vulnerable", chance=0.35),)),
    ),
    "orc_champion": (
        MonsterAbility("War Drum Roar", 0, 0.36, kind="buff", target="self", effect="howl", cooldown=3, statuses=(MonsterAbilityStatus("haste", "self"),)),
        MonsterAbility("Champion Cleave", 21, 0.38, effect="cleave", statuses=(MonsterAbilityStatus("vulnerable", chance=0.55),)),
    ),
    "spider": (
        MonsterAbility("Web Spit", 10, 0.40, effect="web", statuses=(MonsterAbilityStatus("weak", chance=0.55),)),
    ),
    "venom_spider": (
        MonsterAbility("Venom Needle", 16, 0.48, effect="poison", statuses=(MonsterAbilityStatus("poison", chance=0.80),)),
    ),
    "acid_broodmother": (
        MonsterAbility("Brood Venom", 21, 0.50, effect="poison", statuses=(MonsterAbilityStatus("poison", chance=1.0), MonsterAbilityStatus("vulnerable", chance=0.45))),
        MonsterAbility("Chitin Brace", 0, 0.26, kind="buff", target="self", effect="ward", cooldown=4, hp_below=0.55, statuses=(MonsterAbilityStatus("fortified", "self"), MonsterAbilityStatus("regeneration", "self", power=5))),
    ),
    "wraith": (
        MonsterAbility("Ashen Hex", 13, 0.42, effect="shadow", statuses=(MonsterAbilityStatus("weak", chance=0.55),)),
    ),
    "elder_wraith": (
        MonsterAbility("Soul Chill", 19, 0.48, effect="shadow", statuses=(MonsterAbilityStatus("weak", chance=0.75), MonsterAbilityStatus("vulnerable", chance=0.35))),
        MonsterAbility("Fade Behind", 0, 0.28, kind="buff", target="self", effect="ward", cooldown=3, statuses=(MonsterAbilityStatus("fortified", "self"),)),
    ),
    "thornling": (
        MonsterAbility("Briar Lash", 12, 0.42, effect="thorn", statuses=(MonsterAbilityStatus("poison", chance=0.45),)),
        MonsterAbility("Rootskin", 0, 0.28, kind="buff", target="self", effect="ward", cooldown=4, hp_below=0.60, statuses=(MonsterAbilityStatus("regeneration", "self", power=4),)),
    ),
    "sand_stalker": (
        MonsterAbility("Sand Veil", 11, 0.44, effect="dust", statuses=(MonsterAbilityStatus("weak", chance=0.60),)),
        MonsterAbility("Burrow Strike", 18, 0.30, effect="fang", cooldown=3, statuses=(MonsterAbilityStatus("vulnerable", chance=0.40),)),
    ),
    "ice_golem": (
        MonsterAbility("Glacier Slam", 24, 0.42, effect="frost", statuses=(MonsterAbilityStatus("weak", chance=0.80),)),
        MonsterAbility("Icebound Shell", 0, 0.30, kind="buff", target="self", effect="ward", cooldown=4, hp_below=0.65, statuses=(MonsterAbilityStatus("shield", "self", power=12), MonsterAbilityStatus("fortified", "self"))),
    ),
    "bog_beast": (
        MonsterAbility("Mire Drag", 20, 0.44, effect="acid", statuses=(MonsterAbilityStatus("poison", chance=0.55), MonsterAbilityStatus("weak", chance=0.40))),
        MonsterAbility("Swamp Renewal", 0, 0.26, kind="buff", target="self", effect="ward", cooldown=4, hp_below=0.50, statuses=(MonsterAbilityStatus("regeneration", "self", power=7),)),
    ),
    "ember_imp": (
        MonsterAbility("Spark Spit", 16, 0.48, effect="fire", statuses=(MonsterAbilityStatus("burn", chance=0.75),)),
        MonsterAbility("Kindle Hide", 0, 0.26, kind="buff", target="self", effect="fire", cooldown=3, hp_below=0.55, statuses=(MonsterAbilityStatus("haste", "self"),)),
    ),
}


def monster_abilities_for(monster_key: str) -> tuple[MonsterAbility, ...]:
    abilities = MONSTER_ABILITIES.get(monster_key)
    if abilities is not None:
        return abilities
    sprite = str(MONSTERS.get(monster_key, {}).get("sprite", ""))
    return MONSTER_ABILITIES.get(sprite, ())
