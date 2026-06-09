package com.alderfall.game;

public record AbilityStatus(String key, String target, double chance) {
    public AbilityStatus(String key) {
        this(key, "enemy", 1.0);
    }

    public AbilityStatus(String key, String target) {
        this(key, target, 1.0);
    }
}
