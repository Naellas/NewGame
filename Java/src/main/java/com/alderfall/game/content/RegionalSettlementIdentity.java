package com.alderfall.game;

import java.util.List;

/** Cultural profiles are explicit: political proximity must not turn Snowrest into a river town. */
public final class RegionalSettlementIdentity {
    public enum Region { HEARTH, RIVER, NORTH, SUN, FEN, FREEHOLDS, NONE }

    public record District(String name, TilePoint center, TilePoint work, TilePoint gathering,
                           String keeperName, List<String> dialogue) { }

    public record TownBuildingSpec(String key, String style, String label) { }

    public record TownProfile(String identity, List<String> districts, List<TownBuildingSpec> buildings) { }

    private RegionalSettlementIdentity() { }

    public static Region region(String mapId) {
        return switch (mapId) {
            case "city_archive", "town_moonspire", "village_oakhaven", "village_elderford" -> Region.HEARTH;
            case "city_riverside", "town_briarbridge", "village_foxbarrow" -> Region.RIVER;
            case "city_highwall", "town_ironvale", "village_snowrest", "village_pineward" -> Region.NORTH;
            case "city_sanctum", "town_embermarket", "village_dunewick", "village_sunmere", "village_redcairn" -> Region.SUN;
            case "city_belltower", "town_reedwatch", "village_mireford", "village_glimmerfen", "village_stormfen" -> Region.FEN;
            case "town_northwatch", "village_cairnvale", "town_greyharbor" -> Region.FREEHOLDS;
            default -> Region.NONE;
        };
    }

    /** Named civic programs keep towns within one region from becoming copies of the regional capital. */
    public static TownProfile townProfile(String mapId) {
        return switch (mapId) {
            case "town_briarbridge" -> new TownProfile(
                    "Open-bough hospitality, bridge charters, orchards and river work",
                    List.of("Charter Quay", "Abbey Orchards"),
                    List.of(
                            new TownBuildingSpec("briarbridge_bridge_court", "river_hall", "Briarbridge Bridge Court"),
                            new TownBuildingSpec("bridge_inn", "inn", "Briarbridge Guest Abbey"),
                            new TownBuildingSpec("river_warehouse", "warehouse", "Charter Granary"),
                            new TownBuildingSpec("inner_west_row", "row", "Briarbridge Ferry Lodge"),
                            new TownBuildingSpec("south_mid_shop", "fishing_hut", "Millwheel Workshop")
                    ));
            case "town_ironvale" -> new TownProfile(
                    "Ironworking, winter stores, remembered names and disciplined pass defense",
                    List.of("Forge Ward", "Names Court"),
                    List.of(
                            new TownBuildingSpec("ironvale_forge_keep", "arena", "Ironvale Forge Keep"),
                            new TownBuildingSpec("north_armory", "blacksmith", "North Armory"),
                            new TownBuildingSpec("inner_west_row", "row", "Ironvale Hall of Names"),
                            new TownBuildingSpec("inner_east_row", "row", "Ironvale Winter Smokehouse"),
                            new TownBuildingSpec("east_barracks", "barracks", "Pass Barracks")
                    ));
            case "town_moonspire" -> new TownProfile(
                    "Public surveys, practical scholarship, shared seed stores and household records",
                    List.of("Survey Close", "Seed Ledger Ward"),
                    List.of(
                            new TownBuildingSpec("moonspire_mage_tower", "mage_tower", "Moonspire Survey Tower"),
                            new TownBuildingSpec("west_stacks", "guild", "Moonspire Map Stacks"),
                            new TownBuildingSpec("inner_west_row", "row", "Moonspire Common Granary"),
                            new TownBuildingSpec("west_scriptorium", "guild", "Public Scriptorium"),
                            new TownBuildingSpec("south_mid_shop", "alchemist", "Illuminators' Workshop")
                    ));
            case "town_reedwatch" -> new TownProfile(
                    "Flood warnings, reed craft, listening customs and maintained plank walks",
                    List.of("Bell Landing", "Reedwright Walk"),
                    List.of(
                            new TownBuildingSpec("reedwatch_bell_tower", "bell_tower", "Reedwatch Listening Tower"),
                            new TownBuildingSpec("bellwright_shop", "workshop", "Bellwrights' Workshop"),
                            new TownBuildingSpec("inner_west_row", "row", "Reedwatch Flood Bellhouse"),
                            new TownBuildingSpec("inner_east_row", "row", "Reedworkers' House"),
                            new TownBuildingSpec("south_mid_shop", "fishing_hut", "Eelers' Landing")
                    ));
            case "town_embermarket" -> new TownProfile(
                    "First-cup hospitality, caravan shelter, irrigation and public water accounting",
                    List.of("First-Cup Court", "Cistern Ward"),
                    List.of(
                            new TownBuildingSpec("embermarket_sun_court", "sun_shrine", "Embermarket Sun Court"),
                            new TownBuildingSpec("west_reliquary", "guild", "Water Ledger House"),
                            new TownBuildingSpec("inner_west_row", "row", "Embermarket Cistern House"),
                            new TownBuildingSpec("inner_east_row", "row", "Roadside Caravanserai"),
                            new TownBuildingSpec("south_apothecary", "apothecary", "Irrigators' Apothecary")
                    ));
            case "town_northwatch" -> new TownProfile(
                    "Independent rescue crews, signal keeping, rope work and shelter for stranded travelers",
                    List.of("Signal Yard", "Rescue Close"),
                    List.of(
                            new TownBuildingSpec("northwatch_signal_tower", "watchtower", "Northwatch Signal Tower"),
                            new TownBuildingSpec("north_armory", "carpenter", "Ropewrights' Shed"),
                            new TownBuildingSpec("inner_west_row", "row", "Northwatch Rescue Lodge"),
                            new TownBuildingSpec("east_barracks", "warehouse", "Winter Rescue Stores"),
                            new TownBuildingSpec("west_guardhouse", "inn", "Stranded Travelers' Hall")
                    ));
            case "town_greyharbor" -> new TownProfile(
                    "Storm rescue, crew remembrance, net work and stores raised above the harbor",
                    List.of("Beacon Quay", "Returned Crews Walk"),
                    List.of(
                            new TownBuildingSpec("greyharbor_storm_beacon", "watchtower", "Greyharbor Storm Beacon"),
                            new TownBuildingSpec("bellwright_shop", "fishing_hut", "Netmakers' Loft"),
                            new TownBuildingSpec("inner_west_row", "row", "Greyharbor Rescue Lodge"),
                            new TownBuildingSpec("north_chapel_row", "warehouse", "Raised Storm Stores"),
                            new TownBuildingSpec("bellkeepers_lodge", "inn", "Returned Crews House")
                    ));
            default -> null;
        };
    }

    public static TownBuildingSpec townBuilding(String mapId, CityBuilding building) {
        TownProfile profile = townProfile(mapId);
        if (profile == null || building == null || building.key() == null) return null;
        for (TownBuildingSpec spec : profile.buildings()) {
            if (building.key().equals(spec.key()) || building.key().startsWith(spec.key() + "_part_")) return spec;
        }
        return null;
    }

    public static String townDistrictName(String mapId, int index) {
        TownProfile profile = townProfile(mapId);
        if (profile == null || profile.districts().isEmpty()) return null;
        return profile.districts().get(Math.floorMod(index, profile.districts().size()));
    }

    /** Authored overworld silhouettes; an empty value lets the renderer use its generic fallback. */
    public static String overworldAsset(String mapId) {
        String town = switch (mapId) {
            case "city_archive", "town_moonspire" -> "city_overworld_archive_court_imagegen";
            case "town_briarbridge" -> "city_overworld_town_briarbridge_imagegen";
            case "town_ironvale" -> "city_overworld_town_ironvale_imagegen";
            case "town_reedwatch" -> "city_overworld_town_reedwatch_imagegen";
            case "town_embermarket" -> "city_overworld_town_embermarket_imagegen";
            case "town_northwatch" -> "city_overworld_town_northwatch_imagegen";
            case "town_greyharbor" -> "city_overworld_town_greyharbor_imagegen";
            default -> "";
        };
        if (!town.isEmpty() || !mapId.startsWith("village_")) return town;
        return switch (region(mapId)) {
            case HEARTH -> "city_overworld_village_hearth_imagegen";
            case RIVER -> "city_overworld_village_river_imagegen";
            case NORTH -> "city_overworld_village_north_imagegen";
            case SUN -> "city_overworld_village_sun_imagegen";
            case FEN -> "city_overworld_village_fen_imagegen";
            case FREEHOLDS -> "city_overworld_village_freeholds_imagegen";
            case NONE -> "";
        };
    }

    public static String buildingAsset(String mapId, CityBuilding building) {
        RegionalBuildingTypes.Type institution = RegionalBuildingTypes.type(mapId, building);
        if (institution != null) return institution.asset;
        if (building == null || !List.of("house", "row").contains(building.style())) return "";
        if (Math.floorMod(building.key().hashCode(), 2) == 0) return "";
        return switch (region(mapId)) {
            case NORTH -> "regional_north_turf_house";
            case FREEHOLDS -> mapId.equals("town_greyharbor") ? "regional_fen_stilt_house" : "regional_north_turf_house";
            case SUN -> "regional_sun_courtyard_house";
            case FEN -> "regional_fen_stilt_house";
            default -> "";
        };
    }

    public static String arrival(String mapId) {
        return switch (region(mapId)) {
            case HEARTH -> "Orchard paths meet shared ovens and the busy seed commons.";
            case RIVER -> "The streets open toward water, orchard ground and the carts waiting by the common.";
            case NORTH -> "Low turf roofs shelter working yards; winter stores and named cairns stand close to home.";
            case SUN -> "Pale courtyard walls gather around water; planted ground follows the irrigation channels.";
            case FEN -> "Reed roofs rise above wet ground. Plank walks link the landing, gardens and bellkeepers.";
            case FREEHOLDS -> "A rescue yard and crew cairns share the road into this independent freehold.";
            default -> "";
        };
    }

    public static String districtName(Region region, int index) {
        return switch (region) {
            case HEARTH -> index == 0 ? "Seed Commons" : "Orchard Walk";
            case RIVER -> index == 0 ? "River Common" : "Orchard Landing";
            case NORTH -> index == 0 ? "Winter Stores Yard" : "Names Cairn";
            case SUN -> index == 0 ? "Guest Water Court" : "Irrigators' Garden";
            case FEN -> index == 0 ? "Listening Landing" : "Reed Garden";
            case FREEHOLDS -> index == 0 ? "Rescue Yard" : "Crew Cairns";
            default -> "Common";
        };
    }

    public static String role(Region region) {
        return switch (region) {
            case HEARTH -> "Seed Tender";
            case RIVER -> "Common Keeper";
            case NORTH -> "Winter Steward";
            case SUN -> "Water Tender";
            case FEN -> "Landing Keeper";
            case FREEHOLDS -> "Rescue Steward";
            default -> "Neighbour";
        };
    }

    public static List<String> dialogue(Region region, int index) {
        return switch (region) {
            case HEARTH -> index == 0
                    ? List.of("These beds supply the shared seed stores. We keep notes on what grew, not just what was planted.",
                            "After the day's work we meet by the common. Someone always has a tool to borrow or a roof to mend.")
                    : List.of("Leave the winter pruning until the grove has finished resting. Fallen branches are ours to clear.",
                            "The orchard path bends around the old roots. Moving the path was easier than moving the trees.");
            case RIVER -> index == 0
                    ? List.of("The common is where carriers wait and neighbours settle whose cart goes next.",
                            "A crossing charter names the keeper's duties too. A seal alone does not cancel them.")
                    : List.of("The orchard feeds the river markets. Fruit from our baskets is freely given when we say so.",
                            "If a stranger offers a formal woodland invitation, ask who is hosting and what is expected.");
            case NORTH -> index == 0
                    ? List.of("Dry fuel and grain come before a new banner. These stores have to outlast the pass closing.",
                            "Hedra's keepers ask us to leave room for a stranger. That means counting another blanket now.")
                    : List.of("We read the names here before winter, including the people who never carried a sword.",
                            "A family can correct the record. We do not let a commander's pride decide who is remembered.");
            case SUN -> index == 0
                    ? List.of("A traveller's first cup comes before negotiations. The council accounts for it with the other water.",
                            "We work the channels early and late. At midday I join the others in the shaded court.")
                    : List.of("Follow the wet soil. Only the beds supplied by the channel can carry this planting.",
                            "A ward does not clear a blocked channel. That is why I carry these tools.");
            case FEN -> index == 0
                    ? List.of("The landing bell carries a warning over the reed beds. Check the flood marks before moving a load.",
                            "Some bells can be heard below water. That does not make every noise an answer from the Deep Listeners.")
                    : List.of("We tend the garden from the plank walk. The channel has to stay open beside it.",
                            "Come back toward evening. We meet at the landing when the day's repairs are done.");
            case FREEHOLDS -> index == 0
                    ? List.of("Rescue stores belong to the people who maintain them. A shipowner still answers to the harbour council.",
                            "We check ropes and blankets before hearing the day's claims. Someone may need them while we argue.")
                    : List.of("These stones remember crews, not only captains. Bring a missing name and we will hear it.",
                            "The north road brings strangers with useful skills. Nobody repairs a harbour alone.");
            default -> List.of("There is work to do before evening.");
        };
    }
}
