package com.alderfall.game.inventory;

import java.util.ArrayList;
import java.util.List;

public record Equipment(
        String key,
        String name,
        String slot,
        String icon,
        ItemRarity rarity,
        int attackBonus,
        int defenseBonus,
        int hpBonus,
        int mpBonus,
        int strengthBonus,
        int intelligenceBonus,
        int dexterityBonus,
        int charismaBonus,
        int constitutionBonus,
        int willpowerBonus,
        int spellDamageBonus,
        int critChanceBonus,
        int critDamageBonus,
        int healingPowerBonus,
        int damageReductionBonus,
        int minLevel,
        int maxLevel,
        int cost,
        String uniqueEffect,
        String description
) {
    public List<String> statLines() {
        List<String> lines = new ArrayList<>();
        if (attackBonus != 0) {
            lines.add("ATK +" + attackBonus);
        }
        if (defenseBonus != 0) {
            lines.add("DEF +" + defenseBonus);
        }
        if (hpBonus != 0) {
            lines.add("HP +" + hpBonus);
        }
        if (mpBonus != 0) {
            lines.add("MP +" + mpBonus);
        }
        if (strengthBonus != 0) {
            lines.add("STR +" + strengthBonus);
        }
        if (intelligenceBonus != 0) {
            lines.add("INT +" + intelligenceBonus);
        }
        if (dexterityBonus != 0) {
            lines.add("DEX +" + dexterityBonus);
        }
        if (charismaBonus != 0) {
            lines.add("CHA +" + charismaBonus);
        }
        if (constitutionBonus != 0) {
            lines.add("CON +" + constitutionBonus);
        }
        if (willpowerBonus != 0) {
            lines.add("WIL +" + willpowerBonus);
        }
        if (spellDamageBonus != 0) {
            lines.add("Spell DMG +" + spellDamageBonus);
        }
        if (critChanceBonus != 0) {
            lines.add("Crit +" + critChanceBonus + "%");
        }
        if (critDamageBonus != 0) {
            lines.add("Crit DMG +" + critDamageBonus + "%");
        }
        if (healingPowerBonus != 0) {
            lines.add("Healing +" + healingPowerBonus);
        }
        if (damageReductionBonus != 0) {
            lines.add("Resist +" + damageReductionBonus + "%");
        }
        return lines;
    }

    public boolean hasUniqueEffect() {
        return uniqueEffect != null && !uniqueEffect.isBlank();
    }

    public String rarityLine() {
        return rarity.label();
    }

    public boolean canEquipAt(int level) {
        return level >= minLevel;
    }

    public boolean isAvailableAt(int level) {
        return level >= minLevel && level <= maxLevel;
    }

    public String levelRangeLine() {
        return maxLevel >= 99 ? "Lv " + minLevel + "+" : "Lv " + minLevel + "-" + maxLevel;
    }
}
