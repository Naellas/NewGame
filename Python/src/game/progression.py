from __future__ import annotations

from dataclasses import dataclass


@dataclass
class LevelCurve:
    name: str
    base_xp: int
    scaling: float
    milestone_interval: int = 5


PROGRESSION_CURVES = {
    "standard": LevelCurve(
        "Standard",
        base_xp=20,
        scaling=0.6,
        milestone_interval=5,
    ),
    "mage": LevelCurve(
        "Mage",
        base_xp=22,
        scaling=0.55,
        milestone_interval=5,
    ),
    "warrior": LevelCurve(
        "Warrior",
        base_xp=18,
        scaling=0.65,
        milestone_interval=5,
    ),
}

STAT_GROWTH = {
    "Knight": {
        "hp": {"base": 6, "level_scale": 0.33, "milestone": 4},
        "mp": {"base": 2, "level_scale": 0.25, "milestone": 2},
        "attack": {"base": 1, "level_scale": 0.33, "milestone": 1},
        "defense": {"base": 0.5, "level_scale": 0.5, "milestone": 1},
    },
    "Mage": {
        "hp": {"base": 4, "level_scale": 0.25, "milestone": 3},
        "mp": {"base": 4, "level_scale": 0.5, "milestone": 2},
        "attack": {"base": 0.5, "level_scale": 0.25, "milestone": 0},
        "defense": {"base": 0, "level_scale": 0.25, "milestone": 1},
    },
    "Ranger": {
        "hp": {"base": 5, "level_scale": 0.3, "milestone": 3},
        "mp": {"base": 3, "level_scale": 0.4, "milestone": 2},
        "attack": {"base": 1, "level_scale": 0.35, "milestone": 1},
        "defense": {"base": 0.25, "level_scale": 0.25, "milestone": 0},
    },
}

MILESTONE_BONUSES = {
    "hp": 4,
    "mp": 2,
    "attack": 1,
    "defense": 1,
}

REWARD_SCALING = {
    "level_1_5": {"xp": 1.0, "gold": 1.0},
    "level_6_10": {"xp": 1.2, "gold": 1.15},
    "level_11_15": {"xp": 1.4, "gold": 1.3},
    "level_16_20": {"xp": 1.6, "gold": 1.5},
    "level_21_plus": {"xp": 1.8, "gold": 1.7},
}
