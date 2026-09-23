package com.alderfall.game.map;

import java.util.List;

/** Surface identity is separate from the existing floor/encounter theme. */
public final class DungeonExteriorCatalog {
    public static final int MIN_SITE_DISTANCE = 28;
    private DungeonExteriorCatalog() { }

    public record Site(String id, String label, String theme, String intent, int x, int y,
                       int depth, String biomes, int roadX, int roadY) { }

    public static final List<Site> NEW_SITES = List.of(
            new Site("dungeon_sunscar_1", "Sunscar Caverns", "cave", "desert_cavern", 117, 292, 2, "sb", 102, 245),
            new Site("dungeon_rimejaw_1", "Rimejaw Cavern", "cave", "ice_cavern", 292, 42, 2, "n", 324, 56),
            new Site("dungeon_briarheart_1", "Briarheart Hollow", "cave", "forest_cavern", 62, 188, 1, "f", 56, 156),
            new Site("dungeon_roseveil_1", "Roseveil Vampire Lair", "abandoned_castle", "vampire_lair", 207, 127, 2, "fgv", 228, 185),
            new Site("dungeon_lastwatch_1", "Lastwatch Graveyard", "crypt", "graveyard", 165, 79, 1, "fgm", 205, 78),
            new Site("dungeon_duneshade_1", "Duneshade Bandit Camp", "bandit_camp", "bandit_camp", 143, 265, 1, "sb", 102, 245),
            new Site("dungeon_thornwall_1", "Thornwall Keep", "abandoned_castle", "castle", 90, 180, 2, "fgm", 82, 105),
            new Site("dungeon_starfall_1", "Starfall Observatory", "abandoned_castle", "magic_tower", 225, 112, 2, "fgmb", 228, 185)
    );

    public static String intent(String id, String theme) {
        for (Site site : NEW_SITES) if (site.id().equals(id)) return site.intent();
        return switch (theme) {
            case "cave" -> id.contains("cairnspire") ? "mine" : "cavern";
            case "abandoned_castle" -> "castle";
            default -> theme;
        };
    }

    /** The surrounding terrain wins over suggestive names or kingdom stereotypes. */
    public static String climate(char tile) {
        return switch (tile) {
            case 'n' -> "ice";
            case 's', 'b' -> "sand";
            case 'v', 'w' -> "marsh";
            case 'f' -> "forest";
            case 'P', '~' -> "coast";
            default -> "stone";
        };
    }

    public static String entrance(String kind, String intent, String climate) {
        if (intent.equals("vampire_lair")) return "location_exterior_vampire_entrance";
        if (intent.equals("magic_tower")) return "location_exterior_arcane_entrance";
        return switch (kind) {
            case "cave" -> switch (climate) {
                case "ice" -> "location_exterior_ice_entrance";
                case "sand" -> "location_exterior_sand_entrance";
                case "forest", "marsh" -> "location_overgrown_cave_entrance";
                default -> "location_cave_dungeon_entrance";
            };
            case "crypt", "graveyard" -> "location_crypt_entrance";
            case "abandoned_castle", "prison" -> "location_dungeon_fortress_gate_imagegen";
            case "sewer" -> "location_dungeon_stair_entrance";
            case "bandit_camp" -> "location_bandit_outpost";
            case "goblin_camp" -> "location_goblin_hut";
            default -> null;
        };
    }

    public static List<String> regionalProps(String climate) {
        return switch (climate) {
            case "ice" -> List.of("deco_tundra_rocks", "deco_tundra_ice_crystals", "deco_snow_pine");
            case "sand" -> List.of("deco_desert_rocks", "deco_desert_sun_bleached_bones", "dungeon_detail_cave_stalagmites");
            case "marsh" -> List.of("deco_marsh_twisted_roots", "deco_soft_water_wet_stones", "deco_reeds");
            case "forest" -> List.of("deco_forest_moss_rock", "deco_forest_ancient_roots", "deco_forest_fern");
            case "coast" -> List.of("deco_soft_water_wet_stones", "deco_rocks", "deco_reeds");
            default -> List.of("deco_mountain_rocks", "deco_rocks", "deco_mountain_scrub_pine");
        };
    }
}
