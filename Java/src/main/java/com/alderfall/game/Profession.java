package com.alderfall.game;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum Profession {
    WOODCUTTING("woodcutting", "Woodcutting"),
    FISHING("fishing", "Fishing"),
    MINING("mining", "Mining"),
    CRAFTING("crafting", "Crafting"),
    WEAVING("weaving", "Weaving"),
    LEATHERWORKING("leatherworking", "Leatherworking"),
    COOKING("cooking", "Cooking"),
    SURVIVAL("survival", "Survival");

    public static final int BASE_MAX_LEVEL = 10;
    public static final int MAX_LEVEL = BASE_MAX_LEVEL;
    public static final int ABSOLUTE_MAX_LEVEL = 16;
    public static final List<Profession> ALL = List.of(values());

    private final String id;
    private final String label;

    Profession(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public static Profession byId(String id) {
        for (Profession profession : values()) {
            if (profession.id.equals(id)) {
                return profession;
            }
        }
        return null;
    }

    public static String label(String id) {
        Profession profession = byId(id);
        return profession == null ? id : profession.label();
    }

    public static int xpForLevel(int level) {
        return xpForLevel(level, BASE_MAX_LEVEL);
    }

    public static int xpForLevel(int level, int cap) {
        int clamped = Math.max(1, Math.min(Math.max(1, cap), level));
        int xp = 0;
        for (int rank = 1; rank < clamped; rank++) {
            xp += xpToNext(rank);
        }
        return xp;
    }

    public static int xpToNext(int level) {
        return 24 + Math.max(1, level) * 16;
    }

    public static int levelForXp(int xp) {
        return levelForXp(xp, BASE_MAX_LEVEL);
    }

    public static int levelForXp(int xp, int cap) {
        int level = 1;
        int maxLevel = Math.max(1, Math.min(ABSOLUTE_MAX_LEVEL, cap));
        int remaining = Math.max(0, xp);
        while (level < maxLevel && remaining >= xpToNext(level)) {
            remaining -= xpToNext(level);
            level++;
        }
        return level;
    }

    public static Map<String, Integer> seedXp(String name, String sprite, String className) {
        Map<String, Integer> seeded = new LinkedHashMap<>();
        for (Profession profession : ALL) {
            seeded.put(profession.id(), 0);
        }
        boost(seeded, SURVIVAL, 2);
        String text = ((name == null ? "" : name) + " " + (sprite == null ? "" : sprite) + " " + (className == null ? "" : className)).toLowerCase();
        if (text.contains("wood") || text.contains("rowan") || text.contains("grove") || text.contains("ranger") || text.contains("forester")) {
            boost(seeded, WOODCUTTING, 2);
            boost(seeded, SURVIVAL, 1);
        }
        if (text.contains("fish") || text.contains("dock") || text.contains("net") || text.contains("river") || text.contains("mire")) {
            boost(seeded, FISHING, 2);
        }
        if (text.contains("stone") || text.contains("miner") || text.contains("blacksmith") || text.contains("garruk") || text.contains("orin")) {
            boost(seeded, MINING, 2);
            boost(seeded, CRAFTING, 1);
        }
        if (text.contains("smith") || text.contains("guard") || text.contains("knight") || text.contains("builder") || text.contains("worker")) {
            boost(seeded, CRAFTING, 2);
        }
        if (text.contains("seam") || text.contains("weav") || text.contains("basket") || text.contains("baker") || text.contains("forager")) {
            boost(seeded, WEAVING, 2);
        }
        if (text.contains("tanner") || text.contains("furrier") || text.contains("hunter") || text.contains("rogue") || text.contains("night")) {
            boost(seeded, LEATHERWORKING, 2);
            boost(seeded, SURVIVAL, 1);
        }
        if (text.contains("cook") || text.contains("baker") || text.contains("hearth") || text.contains("medic")) {
            boost(seeded, COOKING, 2);
        }
        return seeded;
    }

    private static void boost(Map<String, Integer> seeded, Profession profession, int level) {
        seeded.put(profession.id(), Math.max(seeded.getOrDefault(profession.id(), 0), xpForLevel(level)));
    }
}
