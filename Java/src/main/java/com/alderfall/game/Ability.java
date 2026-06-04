package com.alderfall.game;

import java.util.ArrayList;
import java.util.List;

public record Ability(
        String name,
        int power,
        int cost,
        AbilityKind kind,
        String target,
        List<String> damageTypes,
        int cooldown,
        ScalingProfile scaling,
        String effectKey,
        List<AbilityStatus> statusHints,
        VisualResolution visualResolution,
        List<String> tags
) {
    public Ability {
        damageTypes = normalizedDamageTypes(kind, damageTypes);
        cooldown = Math.max(0, cooldown);
        scaling = scaling == null ? inferredScaling(name, kind) : scaling;
        effectKey = effectKey == null ? "" : effectKey.strip().toLowerCase();
        statusHints = statusHints == null ? List.of() : List.copyOf(statusHints);
        visualResolution = visualResolution == null ? VisualResolution.SINGLE : visualResolution;
        tags = normalizedTags(tags);
    }

    public Ability(String name, int power, int cost, AbilityKind kind) {
        this(name, power, cost, kind, "enemy");
    }

    public Ability(String name, int power, int cost) {
        this(name, power, cost, AbilityKind.DAMAGE, "enemy");
    }

    public Ability(String name, int power, int cost, AbilityKind kind, String target) {
        this(name, power, cost, kind, target, inferredDamageTypes(name, kind));
    }

    public Ability(String name, int power, int cost, AbilityKind kind, String target, String... damageTypes) {
        this(name, power, cost, kind, target, List.of(damageTypes));
    }

    public Ability(String name, int power, int cost, AbilityKind kind, String target, List<String> damageTypes) {
        this(name, power, cost, kind, target, damageTypes, 0, inferredScaling(name, kind), "", List.of(), VisualResolution.SINGLE, List.of());
    }

    public enum AbilityKind {
        DAMAGE,
        HEAL,
        DEFEND
    }

    public enum ScalingProfile {
        WEAPON,
        AGILITY,
        ARCANE,
        DIVINE,
        NATURE,
        GUARD,
        TRIAGE
    }

    public enum VisualResolution {
        SINGLE,
        CHAIN,
        MULTI_PROJECTILE,
        AOE
    }

    public Ability withCooldown(int cooldown) {
        return new Ability(name, power, cost, kind, target, damageTypes, cooldown, scaling, effectKey, statusHints, visualResolution, tags);
    }

    public Ability withScaling(ScalingProfile scaling) {
        return new Ability(name, power, cost, kind, target, damageTypes, cooldown, scaling, effectKey, statusHints, visualResolution, tags);
    }

    public Ability withEffect(String effectKey) {
        return new Ability(name, power, cost, kind, target, damageTypes, cooldown, scaling,
                uniqueEffectKey(name, effectKey), statusHints, visualResolution, tags);
    }

    public Ability withStatuses(AbilityStatus... statuses) {
        return new Ability(name, power, cost, kind, target, damageTypes, cooldown, scaling, effectKey, List.of(statuses), visualResolution, tags);
    }

    public Ability withVisualResolution(VisualResolution visualResolution) {
        return new Ability(name, power, cost, kind, target, damageTypes, cooldown, scaling, effectKey,
                statusHints, visualResolution, tags);
    }

    public Ability withTags(String... tags) {
        List<String> merged = new ArrayList<>(this.tags);
        for (String tag : tags) {
            String normalized = normalizeTag(tag);
            if (!normalized.isBlank() && !merged.contains(normalized)) {
                merged.add(normalized);
            }
        }
        return new Ability(name, power, cost, kind, target, damageTypes, cooldown, scaling, effectKey, statusHints, visualResolution, merged);
    }

    public Ability renamed(String name) {
        return new Ability(name, power, cost, kind, target, damageTypes, cooldown, scaling,
                uniqueEffectKey(name, effectKey), statusHints, visualResolution, tags);
    }

    public boolean hasTag(String tag) {
        return tags.contains(normalizeTag(tag));
    }

    public Ability upgraded(int extraPower, int extraCost) {
        return new Ability(name, power + extraPower, cost + extraCost, kind, target, damageTypes, cooldown, scaling, effectKey,
                statusHints, visualResolution, tags);
    }

    public static String uniqueEffectKey(String abilityName, String fallback) {
        String lowered = abilityName == null ? "" : abilityName.toLowerCase();
        if (lowered.contains("firebolt")) return "firebolt_unique";
        if (lowered.contains("frost lance")) return "frost_lance_unique";
        if (lowered.contains("water jet")) return "water_jet_unique";
        if (lowered.equals("mend") || lowered.contains("field mend") || lowered.contains("warm hands")) return "mend_unique";
        if (lowered.contains("piercing shot") || lowered.contains("marked shot") || lowered.contains("debt shot")) return "piercing_shot_unique";
        if (lowered.contains("snare") || lowered.contains("lockjaw")) return "snare_arrow_unique";
        if (lowered.contains("radiant bolt")) return "radiant_bolt_unique";
        if (lowered.contains("smoke veil") || lowered.contains("smoke bomb") || lowered.contains("smoke screen")) return "smoke_veil_unique";
        if (lowered.contains("hemostatic")) return "hemostatic_strike_unique";
        if (lowered.contains("shield ram")) return "shield_ram_unique";
        if (lowered.contains("dual cut") || lowered.contains("crescent cut")) return "dual_cut_unique";
        if (lowered.contains("shadow salve")) return "shadow_salve_unique";
        if (lowered.contains("beastcall") || lowered.contains("fang chorus") || lowered.contains("herdcall")) return "beastcall_howl_unique";
        if (lowered.contains("noonflare") || lowered.contains("noonstar") || lowered.contains("solar flare")
                || lowered.contains("sunburst")) return "noonflare_unique";
        if (lowered.contains("thorn lash")) return "thorn_lash_unique";
        if (lowered.contains("stone guard") || lowered.contains("granite brace")
                || lowered.contains("bedrock shelter")) return "stone_guard_unique";
        if (lowered.contains("inferno") || lowered.contains("phoenix")) return "inferno_script_unique";
        if (lowered.contains("glacier") || lowered.contains("absolute winter")) return "glacier_prison_unique";
        if (lowered.contains("ley reversal")) return "ley_reversal_unique";
        if (lowered.contains("chaos bloom") || lowered.contains("prismatic collapse")) return "chaos_bloom_unique";
        if (lowered.contains("heartline") || lowered.contains("horizon kill")) return "heartline_shot_unique";
        if (lowered.contains("stormline") || lowered.contains("skyfang") || lowered.contains("chain spark")) return "stormline_volley_unique";
        if (lowered.contains("mercy") || lowered.contains("wellspring") || lowered.contains("choir of return")) return "mercy_wellspring_unique";
        if (lowered.contains("aegis circle") || lowered.contains("sanctuary pulse")) return "aegis_circle_unique";
        if (lowered.contains("caustic") || lowered.contains("acid")) return "caustic_flask_unique";
        if (lowered.contains("citadel") || lowered.contains("living rampart")) return "citadel_protocol_unique";
        if (lowered.contains("briar tempest") || lowered.contains("verdant sentence")) return "briar_tempest_unique";
        if (lowered.contains("halo") || lowered.contains("gold ward")) return "halo_bastion_unique";
        if (lowered.contains("titan") || lowered.contains("continental break")) return "titan_hammer_unique";
        if (lowered.contains("final challenge")) return "final_challenge_unique";
        if (lowered.contains("panacea")) return "panacea_toss_unique";
        if (lowered.contains("primeval") || lowered.contains("petal chorus")) return "primeval_bloom_unique";
        return fallback == null ? "" : fallback;
    }

    public static List<String> inferredDamageTypes(String abilityName, AbilityKind kind) {
        if (kind != AbilityKind.DAMAGE) {
            return List.of();
        }
        String lowered = abilityName == null ? "" : abilityName.toLowerCase();
        List<String> types = new ArrayList<>();
        if (lowered.contains("chaos") || lowered.contains("prismatic") || lowered.contains("worldfire")
                || lowered.contains("meteor") || lowered.contains("comet") || lowered.contains("legend")) {
            types.add("chaos");
        }
        if (lowered.contains("arcane") || lowered.contains("rune") || lowered.contains("star")) {
            types.add("arcane");
        }
        if (lowered.contains("fire") || lowered.contains("ember") || lowered.contains("flare")
                || lowered.contains("candle")) {
            types.add("fire");
        }
        if (lowered.contains("lightning") || lowered.contains("spark") || lowered.contains("storm")) {
            types.add("lightning");
        }
        if (lowered.contains("frost") || lowered.contains("ice") || lowered.contains("rime")) {
            types.add("ice");
        }
        if (lowered.contains("water") || lowered.contains("tide") || lowered.contains("current")) {
            types.add("water");
        }
        if (lowered.contains("acid")) {
            types.add("acid");
        }
        if (lowered.contains("poison") || lowered.contains("venom")) {
            types.add("poison");
        }
        if (lowered.contains("shadow") || lowered.contains("night") || lowered.contains("moon")
                || lowered.contains("dusk") || lowered.contains("eclipse") || lowered.contains("garrote")) {
            types.add("dark");
        }
        if (lowered.contains("radiant") || lowered.contains("holy") || lowered.contains("sun")
                || lowered.contains("dawn") || lowered.contains("noon") || lowered.contains("seraphic")) {
            types.add("holy");
        }
        if (lowered.contains("thorn") || lowered.contains("briar") || lowered.contains("vine")
                || lowered.contains("root") || lowered.contains("bloom") || lowered.contains("grove")
                || lowered.contains("bramble") || lowered.contains("elderwood") || lowered.contains("primeval")) {
            types.add("nature");
        }
        if (types.isEmpty() || projectileOrWeaponImpact(lowered)) {
            types.add(0, "physical");
        }
        return dedupe(types);
    }

    private static ScalingProfile inferredScaling(String abilityName, AbilityKind kind) {
        if (kind == AbilityKind.DEFEND) {
            return ScalingProfile.GUARD;
        }
        String lowered = abilityName == null ? "" : abilityName.toLowerCase();
        if (kind == AbilityKind.HEAL) {
            if (lowered.contains("tonic") || lowered.contains("suture") || lowered.contains("triage")
                    || lowered.contains("bandage") || lowered.contains("first aid")) {
                return ScalingProfile.TRIAGE;
            }
            if (lowered.contains("grove") || lowered.contains("root") || lowered.contains("bloom")
                    || lowered.contains("green") || lowered.contains("spring") || lowered.contains("petal")) {
                return ScalingProfile.NATURE;
            }
            if (lowered.contains("sun") || lowered.contains("dawn") || lowered.contains("radiant")
                    || lowered.contains("holy") || lowered.contains("prayer") || lowered.contains("blessing")
                    || lowered.contains("hymn") || lowered.contains("chorus")) {
                return ScalingProfile.DIVINE;
            }
            return ScalingProfile.TRIAGE;
        }
        if (lowered.contains("arcane") || lowered.contains("rune") || lowered.contains("star")
                || lowered.contains("meteor") || lowered.contains("comet") || lowered.contains("spark")
                || lowered.contains("frost") || lowered.contains("fire") || lowered.contains("water")) {
            return ScalingProfile.ARCANE;
        }
        if (lowered.contains("thorn") || lowered.contains("briar") || lowered.contains("vine")
                || lowered.contains("root") || lowered.contains("grove") || lowered.contains("bloom")
                || lowered.contains("petal") || lowered.contains("canopy")) {
            return ScalingProfile.NATURE;
        }
        if (lowered.contains("radiant") || lowered.contains("holy") || lowered.contains("sun")
                || lowered.contains("dawn") || lowered.contains("lantern") || lowered.contains("judgment")) {
            return ScalingProfile.DIVINE;
        }
        if (lowered.contains("shadow") || lowered.contains("smoke") || lowered.contains("night")
                || lowered.contains("veil") || lowered.contains("knife") || lowered.contains("fang")
                || lowered.contains("shot") || lowered.contains("arrow") || lowered.contains("volley")) {
            return ScalingProfile.AGILITY;
        }
        return ScalingProfile.WEAPON;
    }

    private static boolean projectileOrWeaponImpact(String lowered) {
        return lowered.contains("bolt")
                || lowered.contains("lance")
                || lowered.contains("dart")
                || lowered.contains("shot")
                || lowered.contains("arrow")
                || lowered.contains("slash")
                || lowered.contains("cut")
                || lowered.contains("knife")
                || lowered.contains("strike")
                || lowered.contains("bash")
                || lowered.contains("blow")
                || lowered.contains("hammer")
                || lowered.contains("ram")
                || lowered.contains("sweep")
                || lowered.contains("swing")
                || lowered.contains("fang");
    }

    private static List<String> normalizedDamageTypes(AbilityKind kind, List<String> rawTypes) {
        if (kind != AbilityKind.DAMAGE) {
            return List.of();
        }
        if (rawTypes == null || rawTypes.isEmpty()) {
            return List.of("physical");
        }
        return dedupe(rawTypes.stream()
                .map(Ability::normalizeDamageType)
                .filter(type -> !type.isBlank())
                .toList());
    }

    private static String normalizeDamageType(String type) {
        if (type == null) {
            return "";
        }
        String normalized = type.strip().toLowerCase();
        return switch (normalized) {
            case "frost" -> "ice";
            case "void", "shadow" -> "dark";
            case "radiant" -> "holy";
            case "poisonous" -> "poison";
            default -> normalized;
        };
    }

    private static List<String> dedupe(List<String> rawTypes) {
        List<String> types = new ArrayList<>();
        for (String type : rawTypes) {
            String normalized = normalizeDamageType(type);
            if (!normalized.isBlank() && !types.contains(normalized)) {
                types.add(normalized);
            }
        }
        return types.isEmpty() ? List.of("physical") : List.copyOf(types);
    }

    private static List<String> normalizedTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String tag : rawTags) {
            String clean = normalizeTag(tag);
            if (!clean.isBlank() && !normalized.contains(clean)) {
                normalized.add(clean);
            }
        }
        return List.copyOf(normalized);
    }

    private static String normalizeTag(String tag) {
        return tag == null ? "" : tag.strip().toLowerCase().replace(' ', '_').replace('-', '_');
    }
}
