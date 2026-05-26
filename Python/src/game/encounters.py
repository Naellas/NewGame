from __future__ import annotations

from dataclasses import dataclass


@dataclass
class Encounter:
    id: str
    name: str
    description: str
    enemies: list[str]
    terrain: str = "g"
    difficulty: int = 1
    xp_factor: float = 1.0
    gold_factor: float = 1.0


ENCOUNTERS = {
    "marsh_slimes": Encounter(
        "marsh_slimes",
        "Bog Slimes",
        "Marshy area overrun with gelatinous creatures",
        ["slime", "slime", "slime"],
        terrain="g",
        difficulty=1,
    ),
    "wolf_pack": Encounter(
        "wolf_pack",
        "Wolf Pack",
        "Dangerous predators prowling the wilderness",
        ["wolf", "wolf"],
        terrain="g",
        difficulty=2,
    ),
    "bat_roost": Encounter(
        "bat_roost",
        "Bat Roost",
        "Caves filled with echolocation cries",
        ["bat", "bat", "bat"],
        terrain="d",
        difficulty=2,
    ),
    "skeleton_guardians": Encounter(
        "skeleton_guardians",
        "Skeleton Guardians",
        "Undead sentries protecting the crypt",
        ["skeleton", "skeleton"],
        terrain="d",
        difficulty=3,
    ),
    "goblin_raider_camp": Encounter(
        "goblin_raider_camp",
        "Raider Camp",
        "Small goblin settlement",
        ["goblin", "goblin", "goblin"],
        terrain="g",
        difficulty=3,
    ),
    "spider_nest": Encounter(
        "spider_nest",
        "Spider Nest",
        "Massive webs span the cavern",
        ["spider"],
        terrain="d",
        difficulty=4,
        xp_factor=1.3,
        gold_factor=1.2,
    ),
    "wraith_haunt": Encounter(
        "wraith_haunt",
        "Wraith Haunt",
        "Spectral apparitions phase through stone",
        ["wraith", "wraith"],
        terrain="d",
        difficulty=5,
        xp_factor=1.5,
    ),
    "orc_outpost": Encounter(
        "orc_outpost",
        "Orc Outpost",
        "Fortified position of brute warriors",
        ["orc", "goblin"],
        terrain="g",
        difficulty=5,
        gold_factor=1.4,
    ),
    "hybrid_threat": Encounter(
        "hybrid_threat",
        "Hybrid Threat",
        "Mixed monstrosities in deadly formation",
        ["dire_wolf", "acid_slime", "shadow_bat"],
        terrain="d",
        difficulty=6,
        xp_factor=1.4,
        gold_factor=1.3,
    ),
    "thornwood_ambush": Encounter(
        "thornwood_ambush",
        "Thornwood Ambush",
        "Living brambles close ranks in the old forest",
        ["thornling", "thornling", "goblin"],
        terrain="f",
        difficulty=4,
        xp_factor=1.2,
    ),
    "sunsteppe_hunters": Encounter(
        "sunsteppe_hunters",
        "Sunsteppe Hunters",
        "Heat-hardened raiders and burrowing stalkers",
        ["sand_stalker", "ember_imp"],
        terrain="s",
        difficulty=6,
        gold_factor=1.3,
    ),
    "frozen_bulwark": Encounter(
        "frozen_bulwark",
        "Frozen Bulwark",
        "A crystalline guardian blocks the northern pass",
        ["ice_golem"],
        terrain="n",
        difficulty=7,
        xp_factor=1.6,
        gold_factor=1.4,
    ),
    "mire_horror": Encounter(
        "mire_horror",
        "Mire Horror",
        "The marsh rises with teeth and reeds",
        ["bog_beast", "acid_slime"],
        terrain="v",
        difficulty=6,
        xp_factor=1.4,
    ),
    "apex_predator": Encounter(
        "apex_predator",
        "Apex Predator",
        "Single powerful beast",
        ["acid_broodmother"],
        terrain="d",
        difficulty=8,
        xp_factor=2.0,
        gold_factor=1.8,
    ),
}
