from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class StatusEffect:
    name: str
    key: str
    duration: int
    power: int
    description: str
    affect_type: str = "debuff"


@dataclass
class EffectStack:
    effect: StatusEffect
    turns_remaining: int
    power: int = field(default=0)

    def __post_init__(self) -> None:
        if self.power == 0:
            self.power = self.effect.power

    def tick(self) -> bool:
        self.turns_remaining -= 1
        return self.turns_remaining > 0

    def refresh(self, new_power: int | None = None) -> None:
        self.turns_remaining = self.effect.duration
        if new_power is not None:
            self.power = max(self.power, new_power)


@dataclass(frozen=True)
class AbilityStatus:
    key: str
    target: str = "enemy"
    chance: float = 1.0


STATUS_EFFECTS = {
    "poison": StatusEffect(
        "Poison",
        "poison",
        3,
        6,
        "Takes damage each turn",
        "debuff",
    ),
    "weak": StatusEffect(
        "Weak",
        "weak",
        2,
        0,
        "Damage reduced by 30%",
        "debuff",
    ),
    "vulnerable": StatusEffect(
        "Vulnerable",
        "vulnerable",
        2,
        0,
        "Takes 25% more damage",
        "debuff",
    ),
    "fortified": StatusEffect(
        "Fortified",
        "fortified",
        2,
        0,
        "Damage reduced by 25%",
        "buff",
    ),
    "haste": StatusEffect(
        "Haste",
        "haste",
        3,
        0,
        "Basic attacks hit twice",
        "buff",
    ),
    "burn": StatusEffect(
        "Burn",
        "burn",
        4,
        8,
        "Takes more damage each turn",
        "debuff",
    ),
    "regeneration": StatusEffect(
        "Regeneration",
        "regeneration",
        3,
        4,
        "Recovers HP each turn",
        "buff",
    ),
    "shield": StatusEffect(
        "Shield",
        "shield",
        2,
        12,
        "Blocks incoming damage",
        "buff",
    ),
}

STATUS_ICON_LABELS = {
    "poison": "PS",
    "weak": "WK",
    "vulnerable": "VN",
    "fortified": "FT",
    "haste": "HS",
    "burn": "BR",
    "regeneration": "RG",
    "shield": "SH",
}

ABILITY_STATUS_EFFECTS = {
    "poison arrow": (AbilityStatus("poison", "enemy"),),
    "frost lance": (AbilityStatus("weak", "enemy"),),
    "firebolt": (AbilityStatus("burn", "enemy", 0.55),),
    "arc nova": (AbilityStatus("burn", "enemy", 0.65),),
    "chain spark": (AbilityStatus("vulnerable", "enemy", 0.45),),
    "bulwark": (AbilityStatus("shield", "self"), AbilityStatus("fortified", "self")),
    "war cry": (AbilityStatus("fortified", "self"),),
    "second wind": (AbilityStatus("regeneration", "self"),),
    "aegis mend": (AbilityStatus("regeneration", "target"), AbilityStatus("shield", "target")),
    "renewing ward": (AbilityStatus("regeneration", "target"), AbilityStatus("shield", "target")),
    "herbal remedy": (AbilityStatus("regeneration", "self"),),
    "medicinal salve": (AbilityStatus("regeneration", "target"),),
    "field mend": (AbilityStatus("regeneration", "target"),),
    "mend": (AbilityStatus("regeneration", "self"),),
    "mana bloom": (AbilityStatus("regeneration", "self"),),
}


def status_hints_for_ability(ability_name: str, kind: str = "damage") -> tuple[AbilityStatus, ...]:
    lowered = ability_name.strip().lower()
    if lowered in ABILITY_STATUS_EFFECTS:
        return ABILITY_STATUS_EFFECTS[lowered]
    hints: list[AbilityStatus] = []
    if "poison" in lowered or "venom" in lowered:
        hints.append(AbilityStatus("poison", "enemy"))
    if "frost" in lowered or "ice" in lowered:
        hints.append(AbilityStatus("weak", "enemy"))
    if "fire" in lowered or "ember" in lowered:
        hints.append(AbilityStatus("burn", "enemy", 0.5))
    if "shield" in lowered or "ward" in lowered or "bulwark" in lowered:
        hints.append(AbilityStatus("shield", "self" if kind != "heal" else "target"))
    if "mend" in lowered or "salve" in lowered or "renew" in lowered:
        hints.append(AbilityStatus("regeneration", "target" if kind == "heal" else "self"))
    return tuple(hints)
