package com.alderfall.game;

/** Generated garden entrances and regional water features. Gates occupy three clear walking tiles. */
public final class TownGardenArt {
    private TownGardenArt() { }
    public static boolean gate(String asset) { return asset.equals("town_fence_gate") || asset.startsWith("town_garden_gate_"); }
    public static boolean fountain(String asset) { return asset.startsWith("town_fountain_"); }
    public static String fountainFor(String mapId) {
        if (mapId.equals("town_greyharbor")) return "town_fountain_civic";
        return switch (RegionalSettlementIdentity.region(mapId)) {
            case NORTH, FREEHOLDS -> "town_fountain_north";
            case SUN -> "town_fountain_sun";
            default -> "town_fountain_civic";
        };
    }
    /** Political Freeholds span both snowy uplands and a temperate coast. */
    public static char gardenGround(String id) {
        if (id.equals("town_greyharbor")) return 'g';
        return switch (RegionalSettlementIdentity.region(id)) {
            case NORTH, FREEHOLDS -> 'n';
            case SUN -> 's';
            case FEN -> 'v';
            default -> 'g';
        };
    }
    public static char courtGround(String id) {
        return id.equals("town_reedwatch") || id.equals("town_greyharbor") ? 'U'
                : id.equals("town_embermarket") ? 'V' : 'p';
    }
    public static String gardenPlant(String id) {
        return switch (id) {
            case "town_briarbridge" -> "deco_tree_round";
            case "town_moonspire" -> "deco_tree_young";
            case "town_reedwatch" -> "deco_tree_cypress_harvestable";
            case "town_embermarket" -> "deco_beach_palm";
            case "town_greyharbor" -> "deco_tree_pine";
            default -> "deco_tree_pine";
        };
    }
    /** Small paired objects tell what is grown, traded and repaired here. */
    public static String[] localGoods(String id) {
        return switch (id) {
            case "town_briarbridge" -> new String[]{"village_prop_produce_basket", "village_prop_beehive"};
            case "town_ironvale" -> new String[]{"village_prop_ore_pile", "village_prop_anvil_stump"};
            case "town_moonspire" -> new String[]{"village_prop_seedling_tray", "village_prop_herb_barrel"};
            case "town_reedwatch" -> new String[]{"village_prop_fish_rack", "village_prop_drying_rack"};
            case "town_embermarket" -> new String[]{"city_prop_refresh_pottery", "village_prop_water_trough"};
            case "town_greyharbor" -> new String[]{"village_prop_net_drying_rack", "city_prop_refresh_barrel"};
            default -> new String[]{"village_prop_woodpile", "village_prop_tool_rack"};
        };
    }
    public static String gardenGate(String mapId) {
        if (mapId.equals("town_greyharbor")) return "town_garden_gate_arbor";
        return switch (RegionalSettlementIdentity.region(mapId)) {
            case NORTH, FREEHOLDS, SUN -> "town_garden_gate_iron";
            case FEN, RIVER -> "town_garden_gate_arbor";
            default -> "town_garden_gate_hedge";
        };
    }
}
