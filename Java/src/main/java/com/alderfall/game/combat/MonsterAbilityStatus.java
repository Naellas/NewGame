package com.alderfall.game;

public record MonsterAbilityStatus(String key, String target, double chance, Integer power) {
    public MonsterAbilityStatus(String key) {
        this(key, "target", 1.0, null);
    }

    public MonsterAbilityStatus(String key, String target) {
        this(key, target, 1.0, null);
    }

    public MonsterAbilityStatus(String key, double chance) {
        this(key, "target", chance, null);
    }
}
