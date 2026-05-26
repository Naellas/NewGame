package com.alderfall.game;

import java.util.List;
import java.util.Map;

public final class StatusEffects {
    public static final Map<String, StatusEffect> ALL = Map.ofEntries(
            entry("poison", "Poison", 3, 6, "Takes damage each turn", "debuff"),
            entry("weak", "Weak", 2, 0, "Damage reduced by 30%", "debuff"),
            entry("vulnerable", "Vulnerable", 2, 0, "Takes 25% more damage", "debuff"),
            entry("fortified", "Fortified", 2, 0, "Damage reduced by 25%", "buff"),
            entry("haste", "Haste", 3, 0, "Damage increased", "buff"),
            entry("burn", "Burn", 4, 8, "Takes more damage each turn", "debuff"),
            entry("regeneration", "Regeneration", 3, 4, "Recovers HP each turn", "buff"),
            entry("shield", "Shield", 2, 12, "Blocks incoming damage", "buff")
    );

    private static final Map<String, List<AbilityStatus>> ABILITY_STATUS_EFFECTS = Map.ofEntries(
            Map.entry("poison arrow", List.of(new AbilityStatus("poison"))),
            Map.entry("frost lance", List.of(new AbilityStatus("weak"))),
            Map.entry("firebolt", List.of(new AbilityStatus("burn", "enemy", 0.55))),
            Map.entry("arc nova", List.of(new AbilityStatus("burn", "enemy", 0.65))),
            Map.entry("chain spark", List.of(new AbilityStatus("vulnerable", "enemy", 0.45))),
            Map.entry("bulwark", List.of(new AbilityStatus("shield", "self"), new AbilityStatus("fortified", "self"))),
            Map.entry("war cry", List.of(new AbilityStatus("fortified", "self"))),
            Map.entry("second wind", List.of(new AbilityStatus("regeneration", "self"))),
            Map.entry("aegis mend", List.of(new AbilityStatus("regeneration", "target"), new AbilityStatus("shield", "target"))),
            Map.entry("renewing ward", List.of(new AbilityStatus("regeneration", "target"), new AbilityStatus("shield", "target"))),
            Map.entry("herbal remedy", List.of(new AbilityStatus("regeneration", "self"))),
            Map.entry("medicinal salve", List.of(new AbilityStatus("regeneration", "target"))),
            Map.entry("field mend", List.of(new AbilityStatus("regeneration", "target"))),
            Map.entry("mend", List.of(new AbilityStatus("regeneration", "self"))),
            Map.entry("mana bloom", List.of(new AbilityStatus("regeneration", "self")))
    );

    private StatusEffects() {
    }

    public static List<AbilityStatus> hintsForAbility(String abilityName, Ability.AbilityKind kind) {
        String lowered = abilityName.trim().toLowerCase();
        List<AbilityStatus> configured = ABILITY_STATUS_EFFECTS.get(lowered);
        if (configured != null) {
            return configured;
        }
        java.util.ArrayList<AbilityStatus> hints = new java.util.ArrayList<>();
        if (lowered.contains("poison") || lowered.contains("venom")) {
            hints.add(new AbilityStatus("poison"));
        }
        if (lowered.contains("frost") || lowered.contains("ice")) {
            hints.add(new AbilityStatus("weak"));
        }
        if (lowered.contains("fire") || lowered.contains("ember")) {
            hints.add(new AbilityStatus("burn", "enemy", 0.5));
        }
        if (lowered.contains("shield") || lowered.contains("ward") || lowered.contains("bulwark")) {
            hints.add(new AbilityStatus("shield", kind == Ability.AbilityKind.HEAL ? "target" : "self"));
        }
        if (lowered.contains("mend") || lowered.contains("salve") || lowered.contains("renew")) {
            hints.add(new AbilityStatus("regeneration", kind == Ability.AbilityKind.HEAL ? "target" : "self"));
        }
        return List.copyOf(hints);
    }

    public static String iconLabel(String key) {
        return switch (key) {
            case "poison" -> "PS";
            case "weak" -> "WK";
            case "vulnerable" -> "VN";
            case "fortified" -> "FT";
            case "haste" -> "HS";
            case "burn" -> "BR";
            case "regeneration" -> "RG";
            case "shield" -> "SH";
            default -> key.length() <= 2 ? key.toUpperCase() : key.substring(0, 2).toUpperCase();
        };
    }

    private static Map.Entry<String, StatusEffect> entry(
            String key,
            String name,
            int duration,
            int power,
            String description,
            String affectType
    ) {
        return Map.entry(key, new StatusEffect(name, key, duration, power, description, affectType));
    }
}
