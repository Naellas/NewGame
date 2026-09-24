package com.alderfall.game;

import java.util.List;

/** Whole-building art selection and display sizing; does not alter world footprints or saves. */
public final class TownBuildingArt {
    private TownBuildingArt() { }

    public static String asset(String mapId, CityBuilding building) {
        if (RegionalSettlementIdentity.townProfile(mapId) == null || building == null) return "";
        if (landmark(mapId, building)) {
            String replacement = switch (mapId) {
                case "town_briarbridge" -> "town_briarbridge_bridge_court_grand";
                case "town_ironvale" -> "town_ironvale_forge_keep_grand";
                case "town_embermarket" -> "town_embermarket_sun_court_grand";
                default -> "";
            };
            if (!replacement.isEmpty()) return replacement;
        }
        if (mapId.equals("town_moonspire") && building.key().equals("town_infill_home_0"))
            return "town_moonspire_botanical_glasshouse";
        if (building.key().equals("town_service_barn")) return "town_service_barn_"
                + (TownGardenArt.gardenGround(mapId) == 'n' ? "north"
                : mapId.equals("town_embermarket") ? "sun" : "temperate");
        if (RegionalBuildingTypes.type(mapId, building) != null
                || !WesternReachFolklore.buildingAsset(mapId, building).isEmpty()
                || !HearthlandsFolklore.buildingAsset(mapId, building).isEmpty()) return "";
        if (building.facing() != CityBuilding.Facing.SOUTH)
            return mapId + "_house_" + building.facing().name().toLowerCase(java.util.Locale.ROOT);
        String role = switch (mapId) {
            case "town_briarbridge" -> switch (building.key()) {
                case "briarbridge_bridge_court" -> "bridge_court";
                case "north_gate_homes" -> "cottage";
                case "inner_east_row" -> "townhouse";
                case "briarbridge_manor" -> "manor";
                case "river_warehouse", "east_storehouse" -> "warehouse";
                case "apothecary_row" -> "apothecary";
                case "west_market_house" -> "bakery";
                case "east_market_house" -> "restaurant";
                case "south_tavern" -> "inn";
                case "south_stables" -> "millwork";
                case "river_hall" -> "charterhall";
                default -> "";
            };
            case "town_ironvale" -> switch (building.key()) {
                case "ironvale_forge_keep" -> "forge_keep";
                case "north_gate_homes" -> "cottage";
                case "south_quarters_part_b" -> "townhouse";
                case "west_barracks", "east_barracks" -> "barracks";
                case "west_guardhouse", "east_guardhouse" -> "guardhouse";
                case "north_armory" -> "armory";
                case "south_armory", "infill_armory_yard_blacksmith" -> "forge";
                case "south_mid_shop" -> "carpenter";
                case "captains_hall" -> "captains_hall";
                case "ironvale_command_keep" -> "command_keep";
                case "infill_east_corner_house" -> "courtyard_house";
                default -> "";
            };
            case "town_moonspire" -> switch (building.key()) {
                case "moonspire_mage_tower" -> "survey_tower";
                case "north_gate_homes" -> "cottage";
                case "inner_east_row" -> "townhouse";
                case "west_stacks_part_a", "west_stacks_part_b", "east_stacks_part_a", "east_stacks_part_b" -> "guild_stacks";
                case "west_scriptorium", "east_scriptorium" -> "scriptorium";
                case "illuminator_house" -> "apothecary";
                case "records_hall" -> "records_hall";
                case "south_bindery" -> "bindery";
                case "south_mid_shop", "extra_corner_trade_alchemist" -> "alchemist";
                case "infill_east_lane_bakery" -> "bakery";
                case "southern_observatory_manor" -> "observatory";
                default -> "";
            };
            case "town_reedwatch" -> switch (building.key()) {
                case "reedwatch_bell_tower" -> "listening_tower";
                case "north_gate_homes" -> "cottage";
                case "east_chime_house" -> "townhouse";
                case "bellwright_shop", "infill_south_lane_carpenter" -> "workshop";
                case "north_chapel_row" -> "chapel";
                case "west_market_house" -> "bakery";
                case "east_market_house", "extra_corner_trade_shop" -> "market_shop";
                case "bellkeepers_lodge" -> "lodge";
                case "south_bellfoundry", "infill_bellwright_corner_blacksmith" -> "bellfoundry";
                case "south_mid_shop" -> "fishing_hut";
                case "bellfounder_manor" -> "manor";
                default -> "";
            };
            case "town_embermarket" -> switch (building.key()) {
                case "embermarket_sun_court" -> "sun_court";
                case "north_gate_homes" -> "cottage";
                case "southeast_cells" -> "townhouse";
                case "west_cloister", "east_cloister" -> "cloister";
                case "west_reliquary", "east_reliquary", "embermarket_water_ledger_annex" -> "ledger_house";
                case "east_scribe_house", "south_apothecary" -> "apothecary";
                case "warden_hall" -> "warden_hall";
                case "south_mid_shop" -> "tea_house";
                case "extra_corner_trade_alchemist" -> "alchemist";
                case "infill_south_lane_bakery" -> "bakery";
                case "infill_east_lane_workshop" -> "workshop";
                default -> "";
            };
            case "town_northwatch" -> switch (building.key()) {
                case "northwatch_signal_tower" -> "signal_tower";
                case "north_gate_homes" -> "cottage";
                case "inner_east_row" -> "townhouse";
                case "west_barracks" -> "barracks";
                case "north_armory", "south_mid_shop", "extra_corner_trade_carpenter" -> "ropewright";
                case "east_barracks" -> "warehouse";
                case "east_guardhouse" -> "guardhouse";
                case "west_guardhouse" -> "travelers_inn";
                case "captains_hall" -> "captains_hall";
                case "south_armory", "infill_armory_yard_blacksmith" -> "blacksmith";
                case "ironvale_command_keep" -> "command_hall";
                default -> "";
            };
            case "town_greyharbor" -> switch (building.key()) {
                case "greyharbor_storm_beacon" -> "storm_beacon";
                case "north_gate_homes" -> "cottage";
                case "inner_east_row" -> "townhouse";
                case "bellwright_shop" -> "netmaker";
                case "north_chapel_row" -> "warehouse";
                case "west_market_house" -> "bakery";
                case "east_market_house" -> "market_shop";
                case "bellkeepers_lodge" -> "inn";
                case "south_bellfoundry", "infill_bellwright_corner_blacksmith" -> "foundry";
                case "south_mid_shop" -> "tea_house";
                case "bellfounder_manor" -> "manor";
                default -> "";
            };
            default -> "";
        };
        if (role.isEmpty() && List.of("house", "row").contains(building.style())) {
            // Smaller homes have a larger door-to-facade ratio on tight infill lots.
            role = building.width() <= 2 ? "cottage" : "house";
        }
        return role.isEmpty() ? "" : mapId + "_" + role;
    }

    public static boolean landmark(String mapId, CityBuilding b) {
        var profile = RegionalSettlementIdentity.townProfile(mapId);
        return b != null && profile != null && b.key().equals(profile.buildings().getFirst().key());
    }
    public static int apparentSize(String sprite) {
        return sprite.endsWith("_grand") || List.of("town_moonspire_survey_tower", "town_reedwatch_listening_tower",
                "town_northwatch_signal_tower", "town_greyharbor_storm_beacon").contains(sprite) ? 300 : 180;
    }
    /** Signature institutions have a larger visible silhouette; ordinary buildings retain their scale. */
    public static int[] targetSize(AssetStore assets, String sprite, int ts) {
        return assets.spriteAreaSize(sprite, apparentSize(sprite) * (double) ts / GameConfig.TILE);
    }
}
