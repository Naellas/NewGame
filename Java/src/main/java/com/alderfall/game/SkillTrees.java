package com.alderfall.game;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkillTrees {
    public static final String COMMON_TREE_NAME = "Common";
    public static final Map<String, SkillNode> COMMON_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> KNIGHT_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> MAGE_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> RANGER_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> SKILL_TREE = new LinkedHashMap<>();
    public static final Set<String> SKILL_ABILITY_NAMES;

    static {
        add(COMMON_SKILL_TREE, node("survival", "Survival", "+7 max HP per rank.", 3, 0, 0, List.of(), Map.of("max_hp", 7), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("merchant_sense", "Tradescraft", "Consumables restore +3 HP and +2 MP per rank.", 3, 2, 0, List.of(), Map.of(), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("campfire_music", "Campfire Music", "Learn Campfire Hymn, a modest heal.", 1, 4, 0, List.of(), Map.of(), new Ability("Campfire Hymn", 22, 7, Ability.AbilityKind.HEAL, "ally"), COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("forager", "Forager", "Find steadier battle spoils and keep moving longer.", 2, 0, 1, List.of("survival"), Map.of("max_mp", 3), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("scavenger", "Scavenger", "+10% battle gold per rank.", 3, 1, 1, List.of("survival"), Map.of(), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("battle_medic", "Field Medicine", "Healing items restore +8 extra HP.", 1, 2, 1, List.of("merchant_sense"), Map.of(), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("shared_training", "Road Songs", "Allies deal +1 damage per rank.", 3, 3, 1, List.of("campfire_music"), Map.of(), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("pathfinder", "Pathfinder", "+1 attack and +1 defense per rank.", 2, 4, 1, List.of("campfire_music"), Map.of("attack", 1, "defense", 1), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("settlement_lore", "Settlement Lore", "+5 max MP and +1 defense per rank.", 2, 2, 2, List.of("scavenger", "battle_medic"), Map.of("max_mp", 5, "defense", 1), null, COMMON_TREE_NAME));

        add(KNIGHT_SKILL_TREE, node("iron_body", "Iron Body", "+9 max HP per rank.", 3, 0, 0, List.of(), Map.of("max_hp", 9), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("weapon_training", "Weapon Training", "+2 attack per rank.", 3, 2, 0, List.of(), Map.of("attack", 2), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("warded_armor", "Warded Armor", "+1 defense per rank.", 3, 4, 0, List.of(), Map.of("defense", 1), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("power_strike", "Power Strike", "Learn Power Strike, a strong low-cost attack.", 1, 1, 1, List.of("weapon_training"), Map.of(), new Ability("Power Strike", 30, 7), "Knight"));
        add(KNIGHT_SKILL_TREE, node("guard_mastery", "Guard Mastery", "Take 1 less damage per rank.", 3, 3, 1, List.of("warded_armor"), Map.of(), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("stalwart_guard", "Stalwart Guard", "Defend restores +1 extra MP per rank.", 2, 4, 1, List.of("warded_armor"), Map.of(), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("second_wind", "Second Wind", "Learn Second Wind, a self-heal for long fights.", 1, 1, 2, List.of("iron_body", "power_strike"), Map.of(), new Ability("Second Wind", 32, 9, Ability.AbilityKind.HEAL, "self"), "Knight"));
        add(KNIGHT_SKILL_TREE, node("blade_flurry", "Blade Flurry", "Basic attacks gain +3 damage per rank.", 2, 2, 2, List.of("power_strike", "guard_mastery"), Map.of(), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("heroic_resolve", "Heroic Resolve", "+20 max HP, +2 attack, and +2 defense.", 1, 3, 3, List.of("second_wind", "blade_flurry", "stalwart_guard"), Map.of("max_hp", 20, "attack", 2, "defense", 2), null, "Knight"));

        add(MAGE_SKILL_TREE, node("battle_focus", "Battle Focus", "+6 max MP per rank.", 3, 0, 0, List.of(), Map.of("max_mp", 6), null, "Mage"));
        add(MAGE_SKILL_TREE, node("channeling", "Channeling", "Healing abilities restore +4 HP per rank.", 3, 2, 0, List.of("battle_focus"), Map.of(), null, "Mage"));
        add(MAGE_SKILL_TREE, node("spellcraft", "Spellcraft", "Damaging abilities gain +2 power per rank.", 3, 4, 0, List.of(), Map.of("max_mp", 2), null, "Mage"));
        add(MAGE_SKILL_TREE, node("mana_well", "Mana Well", "+4 max MP and +1 defense per rank.", 2, 0, 1, List.of("battle_focus"), Map.of("max_mp", 4, "defense", 1), null, "Mage"));
        add(MAGE_SKILL_TREE, node("ether_flow", "Ether Flow", "Recover +1 MP per rank after using an ability.", 2, 2, 1, List.of("channeling"), Map.of(), null, "Mage"));
        add(MAGE_SKILL_TREE, node("elemental_precision", "Elemental Precision", "+1 attack and +3 max MP per rank.", 2, 4, 1, List.of("spellcraft"), Map.of("attack", 1, "max_mp", 3), null, "Mage"));
        add(MAGE_SKILL_TREE, node("arcane_burst", "Arcane Burst", "Learn Arcane Burst, a heavy magical attack.", 1, 2, 2, List.of("ether_flow", "elemental_precision"), Map.of(), new Ability("Arcane Burst", 46, 14), "Mage"));
        add(MAGE_SKILL_TREE, node("renewing_ward", "Renewing Ward", "Learn Renewing Ward, a strong heal.", 1, 3, 2, List.of("channeling", "ether_flow"), Map.of(), new Ability("Renewing Ward", 34, 12, Ability.AbilityKind.HEAL, "ally"), "Mage"));
        add(MAGE_SKILL_TREE, node("archmage_resolve", "Archmage Resolve", "+18 max MP, +2 attack, and +1 defense.", 1, 3, 3, List.of("arcane_burst", "renewing_ward"), Map.of("max_mp", 18, "attack", 2, "defense", 1), null, "Mage"));

        add(RANGER_SKILL_TREE, node("trail_sense", "Trail Sense", "+1 attack and +1 defense per rank.", 2, 0, 0, List.of(), Map.of("attack", 1, "defense", 1), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("quickdraw_drills", "Quickdraw Drills", "+2 attack per rank.", 3, 2, 0, List.of(), Map.of("attack", 2), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("wild_medicine", "Wild Medicine", "+6 max HP and +3 max MP per rank.", 2, 4, 0, List.of(), Map.of("max_hp", 6, "max_mp", 3), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("marked_shot", "Marked Shot", "Learn Marked Shot, a clean single-target attack.", 1, 1, 1, List.of("trail_sense", "quickdraw_drills"), Map.of(), new Ability("Marked Shot", 29, 7), "Ranger"));
        add(RANGER_SKILL_TREE, node("keen_edge", "Keen Edge", "+8% chance per rank for basic attacks to hit hard.", 3, 3, 1, List.of("quickdraw_drills"), Map.of(), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("field_salve", "Field Salve", "Learn Field Salve, an efficient heal.", 1, 4, 1, List.of("wild_medicine"), Map.of(), new Ability("Field Salve", 28, 9, Ability.AbilityKind.HEAL, "ally"), "Ranger"));
        add(RANGER_SKILL_TREE, node("twin_fang", "Twin Fang", "Learn Twin Fang, a heavy ranger strike.", 1, 2, 2, List.of("marked_shot", "keen_edge"), Map.of(), new Ability("Twin Fang", 40, 12), "Ranger"));
        add(RANGER_SKILL_TREE, node("pack_coordination", "Pack Coordination", "Allies deal +1 more damage per rank.", 2, 3, 2, List.of("keen_edge", "field_salve"), Map.of(), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("warden_resolve", "Warden Resolve", "+12 max HP, +8 max MP, +2 attack, and +1 defense.", 1, 3, 3, List.of("twin_fang", "pack_coordination"), Map.of("max_hp", 12, "max_mp", 8, "attack", 2, "defense", 1), null, "Ranger"));

        SKILL_TREE.putAll(COMMON_SKILL_TREE);
        SKILL_TREE.putAll(KNIGHT_SKILL_TREE);
        SKILL_TREE.putAll(MAGE_SKILL_TREE);
        SKILL_TREE.putAll(RANGER_SKILL_TREE);
        SKILL_ABILITY_NAMES = SKILL_TREE.values().stream()
                .filter(node -> node.ability() != null)
                .map(node -> node.ability().name())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private SkillTrees() {
    }

    public static Map<String, SkillNode> skillTreeForClass(String className) {
        return switch (className) {
            case "Mage" -> MAGE_SKILL_TREE;
            case "Ranger" -> RANGER_SKILL_TREE;
            case "Knight" -> KNIGHT_SKILL_TREE;
            default -> Map.of();
        };
    }

    public static Map<String, SkillNode> availableSkillTree(String className) {
        Map<String, SkillNode> available = new LinkedHashMap<>(COMMON_SKILL_TREE);
        available.putAll(skillTreeForClass(className));
        return available;
    }

    public static int spent(Map<String, Integer> allocations) {
        return allocations.values().stream().mapToInt(value -> Math.max(0, value)).sum();
    }

    public static int respecCost(int level, int spent) {
        return 35 + Math.max(0, level - 1) * 8 + Math.max(0, spent) * 12;
    }

    private static void add(Map<String, SkillNode> tree, SkillNode node) {
        tree.put(node.id(), node);
    }

    private static SkillNode node(String id, String name, String description, int maxRank, int x, int y,
                                  List<String> requires, Map<String, Integer> effects, Ability ability, String tree) {
        return new SkillNode(id, name, description, maxRank, x, y, requires, effects, ability, tree);
    }
}
