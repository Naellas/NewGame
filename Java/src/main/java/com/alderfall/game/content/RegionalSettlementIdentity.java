package com.alderfall.game;

import java.util.List;

/** Cultural profiles are explicit: political proximity must not turn Snowrest into a river town. */
public final class RegionalSettlementIdentity {
    public enum Region { HEARTH, RIVER, NORTH, SUN, FEN, FREEHOLDS, NONE }

    public record District(String name, TilePoint center, TilePoint work, TilePoint gathering,
                           String keeperName, List<String> dialogue) { }

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

    public static String buildingAsset(String mapId, CityBuilding building) {
        RegionalBuildingTypes.Type institution = RegionalBuildingTypes.type(mapId, building);
        if (institution != null) return institution.asset;
        if (building == null || !List.of("house", "row").contains(building.style())) return "";
        if (Math.floorMod(building.key().hashCode(), 4) == 0) return "";
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
