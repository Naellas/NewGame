package com.alderfall.game;

import java.util.List;

public record MonsterAbility(
        String name,
        int power,
        double chance,
        Kind kind,
        String effect,
        int cooldown,
        Double hpBelow,
        List<MonsterAbilityStatus> statuses
) {
    public enum Kind {
        DAMAGE,
        BUFF
    }

    public MonsterAbility(String name, int power, double chance, String effect, List<MonsterAbilityStatus> statuses) {
        this(name, power, chance, Kind.DAMAGE, effect, 2, null, statuses);
    }

    public MonsterAbility(String name, int power, double chance, Kind kind, String effect, int cooldown, Double hpBelow, List<MonsterAbilityStatus> statuses) {
        this.name = name;
        this.power = power;
        this.chance = chance;
        this.kind = kind;
        this.effect = effect;
        this.cooldown = cooldown;
        this.hpBelow = hpBelow;
        this.statuses = List.copyOf(statuses);
    }
}
