from __future__ import annotations

from dataclasses import dataclass


@dataclass
class Equipment:
    key: str
    name: str
    slot: str
    icon: str
    attack_bonus: int = 0
    defense_bonus: int = 0
    hp_bonus: int = 0
    mp_bonus: int = 0
    cost: int = 0
    description: str = ""

    def stat_lines(self) -> list[str]:
        lines: list[str] = []
        if self.attack_bonus:
            lines.append(f"ATK +{self.attack_bonus}")
        if self.defense_bonus:
            lines.append(f"DEF +{self.defense_bonus}")
        if self.hp_bonus:
            lines.append(f"HP +{self.hp_bonus}")
        if self.mp_bonus:
            lines.append(f"MP +{self.mp_bonus}")
        return lines


WEAPONS = {
    "rusty_sword": Equipment(
        "rusty_sword",
        "Rusty Sword",
        "weapon",
        "icon_sword",
        attack_bonus=1,
        cost=8,
        description="Old steel with one useful edge",
    ),
    "iron_sword": Equipment(
        "iron_sword",
        "Iron Sword",
        "weapon",
        "icon_sword",
        attack_bonus=3,
        cost=25,
        description="A sturdy blade of forged iron",
    ),
    "steel_sword": Equipment(
        "steel_sword",
        "Steel Sword",
        "weapon",
        "icon_sword",
        attack_bonus=6,
        cost=60,
        description="A keen edge of tempered steel",
    ),
    "enchanted_blade": Equipment(
        "enchanted_blade",
        "Enchanted Blade",
        "weapon",
        "icon_sword",
        attack_bonus=10,
        mp_bonus=4,
        cost=120,
        description="A blade infused with ancient magic",
    ),
    "stormbrand": Equipment(
        "stormbrand",
        "Stormbrand",
        "weapon",
        "icon_sword",
        attack_bonus=13,
        mp_bonus=6,
        cost=190,
        description="A bright blade that hums before rain",
    ),
    "hunter_knife": Equipment(
        "hunter_knife",
        "Hunter Knife",
        "weapon",
        "icon_sword",
        attack_bonus=3,
        defense_bonus=1,
        cost=28,
        description="Fast, balanced, and easy to carry",
    ),
    "oak_bow": Equipment(
        "oak_bow",
        "Oak Bow",
        "weapon",
        "icon_bow",
        attack_bonus=4,
        cost=30,
        description="A reliable ranger's weapon",
    ),
    "ash_bow": Equipment(
        "ash_bow",
        "Ash Bow",
        "weapon",
        "icon_bow",
        attack_bonus=7,
        cost=65,
        description="Carved from hardened ash wood",
    ),
    "storm_bow": Equipment(
        "storm_bow",
        "Storm Bow",
        "weapon",
        "icon_sword",
        attack_bonus=11,
        mp_bonus=3,
        cost=145,
        description="Its string snaps like distant thunder",
    ),
    "flame_staff": Equipment(
        "flame_staff",
        "Flame Staff",
        "weapon",
        "icon_staff",
        attack_bonus=2,
        mp_bonus=8,
        cost=55,
        description="Crackling with fire magic",
    ),
    "frost_staff": Equipment(
        "frost_staff",
        "Frost Staff",
        "weapon",
        "icon_staff",
        attack_bonus=3,
        mp_bonus=10,
        cost=75,
        description="Radiates an icy chill",
    ),
    "star_staff": Equipment(
        "star_staff",
        "Star Staff",
        "weapon",
        "icon_potion_blue",
        attack_bonus=5,
        mp_bonus=16,
        cost=160,
        description="A focus for patient, precise spellwork",
    ),
}

ARMOR = {
    "traveler_cloak": Equipment(
        "traveler_cloak",
        "Traveler Cloak",
        "armor",
        "icon_chest",
        defense_bonus=1,
        hp_bonus=4,
        cost=15,
        description="Weathered cloth with hidden stitching",
    ),
    "leather_vest": Equipment(
        "leather_vest",
        "Leather Vest",
        "armor",
        "icon_armor",
        defense_bonus=2,
        cost=20,
        description="Light and flexible protection",
    ),
    "iron_mail": Equipment(
        "iron_mail",
        "Iron Mail",
        "armor",
        "icon_armor",
        defense_bonus=4,
        hp_bonus=6,
        cost=50,
        description="Protective iron plating",
    ),
    "steel_plate": Equipment(
        "steel_plate",
        "Steel Plate",
        "armor",
        "icon_armor",
        defense_bonus=6,
        hp_bonus=12,
        cost=100,
        description="Heavy steel armor",
    ),
    "warden_plate": Equipment(
        "warden_plate",
        "Warden Plate",
        "armor",
        "icon_shield",
        defense_bonus=8,
        hp_bonus=18,
        cost=170,
        description="Built to hold a line when the line breaks",
    ),
    "mage_robes": Equipment(
        "mage_robes",
        "Mage Robes",
        "armor",
        "icon_robes",
        defense_bonus=1,
        mp_bonus=6,
        cost=40,
        description="Embued with magical resonance",
    ),
    "silkweave_robes": Equipment(
        "silkweave_robes",
        "Silkweave Robes",
        "armor",
        "icon_potion_blue",
        defense_bonus=3,
        mp_bonus=12,
        cost=105,
        description="Soft robes threaded with warding sigils",
    ),
    "ranger_coat": Equipment(
        "ranger_coat",
        "Ranger Coat",
        "armor",
        "icon_chest",
        defense_bonus=4,
        hp_bonus=8,
        mp_bonus=3,
        cost=90,
        description="Quiet leather reinforced for long roads",
    ),
}

ACCESSORIES = {
    "iron_ring": Equipment(
        "iron_ring",
        "Iron Ring",
        "accessory",
        "icon_ring",
        attack_bonus=1,
        defense_bonus=1,
        cost=30,
        description="A simple band of iron",
    ),
    "vitality_charm": Equipment(
        "vitality_charm",
        "Vitality Charm",
        "accessory",
        "icon_charm",
        hp_bonus=8,
        cost=45,
        description="Increases maximum health",
    ),
    "mana_stone": Equipment(
        "mana_stone",
        "Mana Stone",
        "accessory",
        "icon_gem",
        mp_bonus=6,
        cost=50,
        description="Resonates with magical energy",
    ),
    "warrior_brooch": Equipment(
        "warrior_brooch",
        "Warrior Brooch",
        "accessory",
        "icon_sword",
        attack_bonus=3,
        hp_bonus=4,
        cost=70,
        description="A brass pin worn by old captains",
    ),
    "warding_seal": Equipment(
        "warding_seal",
        "Warding Seal",
        "accessory",
        "icon_shield",
        defense_bonus=3,
        mp_bonus=4,
        cost=75,
        description="A small charm etched with defensive runes",
    ),
    "phoenix_feather": Equipment(
        "phoenix_feather",
        "Phoenix Feather",
        "accessory",
        "icon_potion_red",
        hp_bonus=14,
        mp_bonus=8,
        cost=130,
        description="Warm to the touch and difficult to look away from",
    ),
}

ALL_EQUIPMENT = {**WEAPONS, **ARMOR, **ACCESSORIES}
