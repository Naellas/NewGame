package com.alderfall.game;

public record Ability(String name, int power, int cost, AbilityKind kind, String target) {
    public Ability(String name, int power, int cost, AbilityKind kind) {
        this(name, power, cost, kind, "enemy");
    }

    public Ability(String name, int power, int cost) {
        this(name, power, cost, AbilityKind.DAMAGE, "enemy");
    }

    public enum AbilityKind {
        DAMAGE,
        HEAL,
        DEFEND
    }
}
