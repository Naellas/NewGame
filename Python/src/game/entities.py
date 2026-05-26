from __future__ import annotations

from dataclasses import dataclass, field
from random import randint

from .equipment import ALL_EQUIPMENT, Equipment


@dataclass
class Ability:
    name: str
    power: int
    cost: int
    kind: str = "damage"
    target: str = "enemy"


@dataclass
class CharacterClass:
    name: str
    max_hp: int
    max_mp: int
    attack: int
    defense: int
    abilities: list[Ability]


@dataclass
class Actor:
    name: str
    sprite: str
    class_name: str
    max_hp: int
    hp: int
    max_mp: int
    mp: int
    attack: int
    defense: int
    level: int = 1
    xp: int = 0
    gold: int = 0
    abilities: list[Ability] = field(default_factory=list)
    inventory: dict[str, int] = field(default_factory=dict)
    perks: list[str] = field(default_factory=list)
    skill_points: int = 0
    skill_allocations: dict[str, int] = field(default_factory=dict)
    equipment: dict[str, str] = field(default_factory=dict)

    @property
    def alive(self) -> bool:
        return self.hp > 0

    def heal_full(self) -> None:
        self.hp = self.max_hp
        self.mp = self.max_mp

    def take_damage(self, amount: int) -> int:
        damage = max(1, amount - self.defense)
        self.hp = max(0, self.hp - damage)
        return damage

    def basic_damage(self) -> int:
        return randint(max(1, self.attack - 2), self.attack + 4)

    def xp_to_next(self) -> int:
        return 20 + self.level * 12

    def has_ability(self, ability_name: str) -> bool:
        return any(ability.name == ability_name for ability in self.abilities)

    def has_perk(self, perk_key: str) -> bool:
        return perk_key in self.perks

    def skill_rank(self, skill_key: str) -> int:
        return max(0, int(self.skill_allocations.get(skill_key, 0)))

    def gain_xp(self, amount: int) -> list[str]:
        notes: list[str] = []
        self.xp += amount
        leveled = 0
        while self.xp >= self.xp_to_next():
            self.xp -= self.xp_to_next()
            self.level += 1
            self.skill_points += 1
            leveled += 1
            hp_gain = 6 + self.level // 3
            mp_gain = 2 + self.level // 4
            atk_gain = 1 + (1 if self.level % 3 == 0 else 0)
            def_gain = 1 if self.level % 2 == 0 else 0
            if self.has_perk("vitality"):
                hp_gain += 3
            if self.has_perk("focus"):
                mp_gain += 2
            self.max_hp += hp_gain
            self.max_mp += mp_gain
            self.attack += atk_gain
            self.defense += def_gain
            if self.level % 5 == 0:
                self.max_hp += 4
                self.max_mp += 2
                self.attack += 1
                self.defense += 1
                notes.append("Milestone growth unlocked.")
            notes.append(f"Level {self.level}: +{hp_gain} HP, +{mp_gain} MP, +{atk_gain} ATK, +{def_gain} DEF, +1 skill point.")
        if leveled:
            self.heal_full()
        return notes

    def add_item(self, item_key: str, amount: int = 1) -> None:
        self.inventory[item_key] = self.inventory.get(item_key, 0) + amount

    def has_item(self, item_key: str) -> bool:
        return self.inventory.get(item_key, 0) > 0

    def consume_item(self, item_key: str) -> bool:
        if not self.has_item(item_key):
            return False
        self.inventory[item_key] -= 1
        if self.inventory[item_key] <= 0:
            del self.inventory[item_key]
        return True

    def equipped_item(self, slot: str) -> Equipment | None:
        key = self.equipment.get(slot)
        return ALL_EQUIPMENT.get(key) if key else None

    def equip_item(self, item_key: str) -> tuple[bool, str]:
        item = ALL_EQUIPMENT.get(item_key)
        if not item:
            return False, "That item cannot be equipped."
        if not self.has_item(item_key):
            return False, f"You do not have {item.name}."

        old_key = self.equipment.get(item.slot)
        if old_key:
            self._apply_equipment_bonus(ALL_EQUIPMENT[old_key], -1)
            self.add_item(old_key, 1)

        self.consume_item(item_key)
        self.equipment[item.slot] = item_key
        self._apply_equipment_bonus(item, 1)
        if self.hp > self.max_hp:
            self.hp = self.max_hp
        if self.mp > self.max_mp:
            self.mp = self.max_mp
        return True, f"Equipped {item.name}."

    def unequip_slot(self, slot: str) -> tuple[bool, str]:
        item_key = self.equipment.get(slot)
        if not item_key:
            return False, "Nothing is equipped there."
        item = ALL_EQUIPMENT[item_key]
        self._apply_equipment_bonus(item, -1)
        self.add_item(item_key, 1)
        del self.equipment[slot]
        self.hp = min(self.hp, self.max_hp)
        self.mp = min(self.mp, self.max_mp)
        return True, f"Unequipped {item.name}."

    def _apply_equipment_bonus(self, item: Equipment, direction: int) -> None:
        self.attack += item.attack_bonus * direction
        self.defense += item.defense_bonus * direction
        self.max_hp += item.hp_bonus * direction
        self.max_mp += item.mp_bonus * direction
        if direction > 0:
            self.hp += item.hp_bonus
            self.mp += item.mp_bonus


@dataclass
class NPC:
    name: str
    sprite: str
    x: int
    y: int
    dialog: list[str]
    quest_id: str | None = None
    shop_id: str | None = None
    recruit_id: str | None = None
    recruit_cost: int = 0


@dataclass
class Quest:
    id: str
    title: str
    description: str
    target: str
    needed: int
    reward_gold: int
    reward_xp: int
    progress: int = 0
    accepted: bool = False
    completed: bool = False

    def record(self, target: str) -> None:
        if self.accepted and not self.completed and target == self.target:
            self.progress = min(self.needed, self.progress + 1)

    def ready(self) -> bool:
        return self.accepted and not self.completed and self.progress >= self.needed
