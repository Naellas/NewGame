package com.alderfall.game;

import com.alderfall.game.inventory.ItemRarity;
import java.util.*;

/** Material properties are independent of equipment templates and survive refinement. */
public final class MaterialCatalog {
    private MaterialCatalog() {}
    public enum Family { METAL, WOOD, CLOTH, HIDE, GEM, MONSTER, REAGENT, STONE, SUPPLY, GLASS }
    public record Stats(int attack, int defense, int hp, int mp, int spell, int crit, int healing) {
        public String summary() {
            List<String> lines = new ArrayList<>();
            if (attack > 0) lines.add("ATK +" + attack);
            if (defense > 0) lines.add("DEF +" + defense);
            if (hp > 0) lines.add("HP +" + hp);
            if (mp > 0) lines.add("MP +" + mp);
            if (spell > 0) lines.add("Spell +" + spell);
            if (crit > 0) lines.add("Crit +" + crit + "%");
            if (healing > 0) lines.add("Healing +" + healing);
            return String.join(", ", lines);
        }
    }
    public record Material(String key, Family family, int tier, int level, ItemRarity rarity, Stats stats) {
        public int requiredSkill() { return new int[]{1, 2, 4, 6, 8}[tier - 1]; }
        public String summary() {
            return "Tier " + tier + " / Lv " + level + " / " + rarity.label() + ". " + stats.summary();
        }
    }
    private static final Map<String, Material> MATERIALS = build();
    public static Material get(String key) { return MATERIALS.get(key); }
    public static Collection<Material> all() { return MATERIALS.values(); }
    private static Map<String, Material> build() {
        Map<String, Material> m = new LinkedHashMap<>();
        add(m, "obsidian_shard", Family.STONE, 3, new Stats(5, 0, 0, 0, 0, 2, 0));
        add(m, "obsidian_glass", Family.GLASS, 3, new Stats(7, 0, 0, 0, 1, 3, 0));
        add(m, "raw_amber", Family.SUPPLY, 2, new Stats(0, 0, 1, 2, 0, 0, 1));
        add(m, "polished_amber", Family.GEM, 3, new Stats(0, 1, 4, 5, 2, 0, 3));
        add(m, "rock_salt", Family.SUPPLY, 1, new Stats(0, 0, 2, 0, 0, 0, 1));
        add(m, "refined_salt", Family.REAGENT, 2, new Stats(0, 1, 2, 0, 0, 0, 2));
        add(m, "cypress_wood", Family.WOOD, 2, new Stats(2, 3, 5, 2, 1, 0, 0));
        String[][] metals = {{"copper", "tin"}, {"iron", "bronze", "bog_iron"},
                {"steel", "silver", "gold", "cobalt"}, {"mithril", "froststeel", "sunmetal", "emberite", "verdant"}, {"adamantite"}};
        for (int t = 0; t < metals.length; t++) for (String metal : metals[t]) {
            int tier = t + 1;
            boolean arcane = Set.of("silver", "gold", "mithril", "sunmetal", "verdant").contains(metal);
            Stats stats = new Stats(tier * 2, tier, tier * 2, arcane ? tier * 2 : 0,
                    arcane ? tier : 0, metal.equals("mithril") ? 2 : 0, metal.equals("verdant") ? 2 : 0);
            if (!metal.equals("steel") && !metal.equals("bronze")) add(m, metal + "_ore", Family.METAL, tier, stats);
            add(m, metal + "_ingot", Family.METAL, tier, stats);
        }
        add(m, "steel_scrap", Family.METAL, 3, new Stats(6, 3, 6, 0, 0, 0, 0));
        String[][] woods = {{"wood", "pine_wood", "deadwood", "palm_wood"},
                {"oak_wood", "birch_wood", "willow_wood", "fruitwood"}, {"maple_wood", "ash_wood", "ironwood"},
                {"elder_wood", "magic_wood", "enchanted_bark"}, {"ancient_wood"}};
        for (int t = 0; t < woods.length; t++) for (String wood : woods[t]) {
            int tier = t + 1;
            add(m, wood, Family.WOOD, tier, new Stats(tier, wood.equals("ironwood") ? 5 : 1,
                    tier, tier * 2, tier, Set.of("birch_wood", "ash_wood", "willow_wood").contains(wood) ? 1 : 0, 0));
        }
        add(m, "linen_cloth", Family.CLOTH, 1, new Stats(0, 1, 2, 2, 0, 0, 0));
        add(m, "wool_cloth", Family.CLOTH, 2, new Stats(0, 2, 6, 4, 0, 0, 1));
        add(m, "silk_cloth", Family.CLOTH, 3, new Stats(0, 2, 2, 6, 2, 1, 0));
        add(m, "moonweave_cloth", Family.CLOTH, 4, new Stats(0, 3, 6, 10, 4, 0, 2));
        add(m, "starweave_cloth", Family.CLOTH, 5, new Stats(0, 4, 10, 14, 6, 0, 3));
        add(m, "tanned_leather", Family.HIDE, 1, new Stats(0, 2, 5, 0, 0, 1, 0));
        add(m, "hardened_leather", Family.HIDE, 2, new Stats(0, 4, 7, 0, 0, 0, 0));
        add(m, "reinforced_leather", Family.HIDE, 3, new Stats(0, 5, 10, 0, 0, 1, 0));
        add(m, "frosthide_leather", Family.HIDE, 4, new Stats(0, 6, 14, 4, 1, 0, 0));
        add(m, "dragonscale_leather", Family.HIDE, 5, new Stats(2, 8, 18, 0, 2, 1, 0));
        add(m, "spider_silk", Family.CLOTH, 2, new Stats(0, 1, 0, 2, 0, 1, 0));
        add(m, "thick_hide", Family.HIDE, 2, new Stats(0, 2, 6, 0, 0, 0, 0));
        add(m, "plant_fiber", Family.CLOTH, 1, new Stats(0, 1, 2, 1, 0, 0, 0));
        add(m, "palm_frond", Family.CLOTH, 1, new Stats(0, 1, 3, 0, 0, 0, 0));
        add(m, "wool", Family.CLOTH, 2, new Stats(0, 2, 4, 3, 0, 0, 1));
        add(m, "skin", Family.HIDE, 1, new Stats(0, 2, 4, 0, 0, 1, 0));
        add(m, "scale", Family.HIDE, 3, new Stats(1, 4, 8, 0, 0, 0, 0));
        add(m, "bone", Family.MONSTER, 1, new Stats(2, 1, 2, 0, 0, 0, 0));
        add(m, "horn", Family.MONSTER, 2, new Stats(3, 0, 0, 0, 0, 2, 0));
        add(m, "venom_sac", Family.MONSTER, 3, new Stats(2, 0, 0, 0, 0, 3, 0));
        add(m, "ember_shard", Family.GEM, 3, new Stats(2, 0, 0, 3, 4, 0, 0));
        add(m, "frost_shard", Family.GEM, 3, new Stats(0, 2, 0, 6, 3, 0, 0));
        add(m, "crystal_dust", Family.GEM, 1, new Stats(0, 0, 0, 3, 2, 0, 0));
        add(m, "seashell", Family.GEM, 1, new Stats(0, 1, 0, 2, 1, 0, 1));
        add(m, "glowroot", Family.REAGENT, 4, new Stats(0, 0, 4, 8, 3, 0, 4));
        add(m, "herb_leaf", Family.REAGENT, 1, new Stats(0, 0, 2, 0, 0, 0, 2));
        add(m, "flower_blossom", Family.REAGENT, 2, new Stats(0, 0, 0, 3, 0, 0, 3));
        add(m, "mushroom_spores", Family.REAGENT, 2, new Stats(0, 0, 0, 4, 2, 0, 0));
        for (String key : List.of("stone", "flint", "clay"))
            add(m, key, Family.STONE, 1, new Stats(key.equals("flint") ? 2 : 1, 1, 0, 0, 0, 0, 0));
        for (String key : List.of("wild_meat", "raw_fish", "garden_vegetables", "coconut", "herb_seed", "coal"))
            add(m, key, Family.SUPPLY, 1, new Stats(0, 0, 2, 0, 0, 0, 1));
        return Collections.unmodifiableMap(m);
    }
    private static void add(Map<String, Material> m, String key, Family family, int tier, Stats stats) {
        ItemRarity rarity = tier == 5 ? ItemRarity.LEGENDARY : tier >= 3 ? ItemRarity.RARE
                : tier == 2 ? ItemRarity.UNCOMMON : ItemRarity.COMMON;
        m.put(key, new Material(key, family, tier, 1 + (tier - 1) * 4, rarity, stats));
    }
}
