package com.alderfall.game;

/** Shared material identity for cavern terrain and room dressing. */
public final class CavernStyle {
    private CavernStyle() { }

    public static boolean natural(String theme) {
        return theme.equals("cave") || theme.equals("goblin_camp");
    }

    public static java.util.List<String> resources(String biome, int depth) {
        return switch (biome) {
            case "ice" -> java.util.List.of("deco_ore_froststeel_vein", "deco_ore_iron_vein",
                    "deco_ore_crystal_vein", "deco_ore_coal_deposit", "deco_ore_silver_vein",
                    depth > 2 ? "deco_ore_mithril_vein" : "deco_ore_copper_vein");
            case "moss" -> java.util.List.of("deco_ore_bog_iron_vein", "deco_mushrooms",
                    "deco_forest_ancient_roots", "deco_ore_copper_vein", "deco_ore_coal_deposit",
                    depth > 1 ? "deco_ore_verdant_vein" : "deco_forest_blue_mushroom_ring");
            case "sand" -> java.util.List.of("deco_ore_sunmetal_vein", "deco_ore_copper_vein",
                    "deco_ore_rock_salt_vein", "deco_ore_coal_deposit", "deco_ore_iron_vein",
                    depth > 1 ? "deco_ore_emberite_vein" : "deco_ore_obsidian_vein");
            default -> java.util.List.of("deco_ore_iron_vein", "deco_ore_copper_vein",
                    "deco_ore_coal_deposit", "deco_imagen_pale_mushroom_ring", "deco_ore_tin_vein",
                    depth > 1 ? "deco_ore_silver_vein" : "deco_ore_crystal_vein");
        };
    }

    public static java.util.List<String> dressing(String biome, String theme) {
        if (theme.equals("goblin_camp")) return java.util.List.of(
                "dungeon_detail_goblin_totem", "dungeon_detail_goblin_scrap_heap",
                "dungeon_detail_goblin_cookpot", "dungeon_detail_bandit_bedroll");
        return switch (biome) {
            case "ice" -> java.util.List.of("deco_tundra_ice_crystals", "dungeon_detail_cave_stalagmites",
                    "dungeon_prop_lantern_stand", "dungeon_detail_bandit_supply_sacks");
            case "moss" -> java.util.List.of("dungeon_detail_cave_root_arch", "dungeon_detail_cave_rock_cluster",
                    "deco_marsh_twisted_roots", "dungeon_prop_lantern_stand");
            case "sand" -> java.util.List.of("deco_desert_sun_bleached_bones", "dungeon_detail_cave_stalagmites",
                    "location_camp_crates", "dungeon_prop_lantern_stand");
            default -> java.util.List.of("dungeon_detail_cave_stalagmites", "dungeon_detail_cave_rock_cluster",
                    "location_camp_crates", "dungeon_prop_lantern_stand");
        };
    }

    public static String biome(String region, char exterior) {
        if (exterior == 'n') return "ice";
        if (exterior == 'v' || exterior == 'w') return "moss";
        if (exterior == 's' || exterior == 'b') return "sand";
        return switch (region) {
            case "highwall", "northroad" -> "ice";
            case "belltower" -> "moss";
            case "sanctum" -> "sand";
            default -> exterior == 'f' ? "moss" : "stone";
        };
    }

    public static String detail(String biome, String asset) {
        if (!asset.equals("dungeon_detail_cave_mineral_cluster")
                && !asset.equals("dungeon_detail_cave_stalagmites")) return asset;
        return switch (biome) {
            case "moss" -> asset.endsWith("stalagmites")
                    ? "dungeon_detail_cave_root_arch" : "dungeon_detail_cave_rock_cluster";
            case "ice" -> asset.endsWith("stalagmites") ? asset : "deco_tundra_ice_crystals";
            case "sand" -> "dungeon_detail_cave_stalagmites";
            default -> asset;
        };
    }
}
