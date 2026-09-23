package com.alderfall.game;

import java.util.List;

/** Architecture and contents of constructed dungeons, separate from natural cavern geology. */
public final class BuiltDungeonStyle {
    private BuiltDungeonStyle() { }
    public static boolean supports(String theme) {
        return List.of("crypt", "prison", "sewer", "bandit_camp", "abandoned_castle", "vampire_castle").contains(theme);
    }
    public static String material(String theme, int floor) {
        return switch (theme) {
            case "crypt" -> "crypt";
            case "abandoned_castle" -> floor > 2 ? "arcane" : "gothic";
            case "vampire_castle" -> "gothic";
            default -> "prison";
        };
    }
    public static boolean masonry(String material) {
        return List.of("prison", "crypt", "gothic", "arcane").contains(material);
    }
    public static String propAsset(String asset) {
        return switch (asset) {
            case "dungeon_prop_cave_crates" -> "location_camp_crates";
            case "dungeon_prop_cave_bedroll" -> "dungeon_detail_bandit_bedroll";
            case "dungeon_prop_cave_torch" -> "dungeon_prop_lantern_stand";
            case "dungeon_prop_cave_crystal" -> "deco_imagen_crystal_cluster";
            default -> asset;
        };
    }
    public static List<String> resources(String theme, String region, char exterior, int floor) {
        String regional = CavernStyle.resources(CavernStyle.biome(region,exterior),floor).get(0);
        return switch (theme) {
            case "crypt" -> List.of("deco_ore_steel_scrap", "deco_ore_silver_vein", "deco_ore_coal_deposit",
                    "deco_imagen_pale_mushroom_ring", "deco_ore_crystal_vein", regional);
            case "sewer" -> List.of("deco_ore_steel_scrap", "deco_mushrooms", "deco_ore_bog_iron_vein",
                    "deco_forest_blue_mushroom_ring", "deco_ore_copper_vein", "deco_ore_coal_deposit");
            case "abandoned_castle", "vampire_castle" -> List.of("deco_ore_steel_scrap", "deco_ore_silver_vein",
                    "deco_ore_crystal_vein", "deco_ore_coal_deposit", regional,
                    floor > 2 ? "deco_ore_mithril_vein" : "deco_ore_copper_vein");
            default -> List.of("deco_ore_steel_scrap", "deco_ore_copper_vein", "deco_ore_coal_deposit",
                    "deco_ore_tin_vein", "deco_imagen_pale_mushroom_ring", regional);
        };
    }
    public static List<String> dressing(String theme, int floor) {
        return switch (theme) {
            case "crypt" -> List.of("dungeon_detail_crypt_ossuary", "dungeon_detail_crypt_urn",
                    "dungeon_detail_crypt_grave_marker", "dungeon_detail_crypt_offering");
            case "abandoned_castle", "vampire_castle" -> floor > 2
                    ? List.of("dungeon_prop_rune_pillar", "deco_imagen_crystal_cluster", "dungeon_detail_vampire_candelabrum", "dungeon_detail_vampire_gargoyle")
                    : List.of("dungeon_detail_vampire_banner", "dungeon_detail_vampire_coffin", "dungeon_detail_vampire_gargoyle", "dungeon_detail_vampire_candelabrum");
            case "sewer" -> List.of("dungeon_detail_cave_rock_cluster", "deco_marsh_twisted_roots", "dungeon_prop_lantern_stand", "location_camp_crates");
            case "prison" -> List.of("dungeon_prop_chain_stand", "dungeon_detail_bandit_bedroll", "dungeon_prop_lantern_stand", "dungeon_detail_bandit_supply_sacks");
            default -> List.of("dungeon_detail_bandit_weapon_rack", "dungeon_detail_bandit_supply_sacks", "dungeon_detail_bandit_bedroll", "dungeon_detail_bandit_barricade");
        };
    }
}
