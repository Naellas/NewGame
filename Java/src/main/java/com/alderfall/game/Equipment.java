package com.alderfall.game;

import java.util.ArrayList;
import java.util.List;

public record Equipment(
        String key,
        String name,
        String slot,
        String icon,
        int attackBonus,
        int defenseBonus,
        int hpBonus,
        int mpBonus,
        int minLevel,
        int maxLevel,
        int cost,
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
        return lines;
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
