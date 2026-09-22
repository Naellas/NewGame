package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.util.List;

/** Environmental storytelling for the first outward journey from Oathstead. */
public final class FolkloreContent {
    public static final String HEARTH = "folklore_oathstead_hearth";
    public static final String SEEDHOUSE = "folklore_returning_seedhouse";
    public static final String BOUNDARY = "folklore_boundary_shrine";

    private FolkloreContent() { }

    public static String observation(String mapId, String asset) {
        if (asset.equals(BOUNDARY)) {
            return "Fresh cord and clean pruning shears mark a tended boundary. Someone still keeps this stretch of the forest agreement.";
        }
        if (asset.equals(SEEDHOUSE)) {
            return "The House of Returning Seed keeps its seed drawers beside the communal oven. A carved seed marks the lintel.";
        }
        if (asset.equals(HEARTH)) {
            return "Oathstead's shared oven stands under a patched shelter. Two bowls wait beside the bread: a first meal for a new household.";
        }
        return switch (mapId) {
            case WorldMap.PLAYER_VILLAGE_ID -> switch (asset) {
                case "location_camp_crates" -> "Dry stores sit close to the hearth. A camp survives on shared supplies as well as wards.";
                case "village_prop_seedling_tray" -> "Young plants take root in reused trays. Someone has made room for next season, even here.";
                default -> "";
            };
            case "village_oakhaven" -> switch (asset) {
                case "village_prop_clay_oven" -> "A covered loaf waits beside the communal oven. Oakhaven sends the first bread to an ill neighbor.";
                case "village_prop_seedling_tray" -> "Seedlings stand in shared trays. The House of Returning Seed lends living seed, not promises of a harvest.";
                default -> "";
            };
            case "village_elderford" -> switch (asset) {
                case "village_prop_woodpile" -> "Fallen branches are stacked separately from cut wood. The Boundary Stewards keep track of what the forest gives and what people take.";
                case "village_prop_clay_oven" -> "Elderford's communal oven has a newly mended mouth. The households share its upkeep as well as its heat.";
                default -> "";
            };
            case "city_archive" -> switch (asset) {
                case "village_prop_clay_oven" -> "A working communal oven stands among the Archive's stone courts. The Hearth Guild keeps protections that never reached the royal shelves.";
                case "village_prop_seedling_tray" -> "Seed trays occupy a corner of the city court. Village practices have followed their keepers inside the walls.";
                default -> "";
            };
            case "dungeon_redcap_camp_1" -> switch (asset) {
                case "location_camp_fire" -> "Cooking pots share the Redcap fire. Guards and laborers must eat from the same stores, whatever brought them here.";
                case "location_camp_crates" -> "Crates have been dragged in from the supply route. This camp needs provisions; its fires do not feed themselves.";
                case "village_prop_woodpile" -> "Split timber and spare stakes lie ready for repairs. Redcap's occupants expect to remain here.";
                default -> "";
            };
            default -> "";
        };
    }

    /** Only replace local small talk, never authored quests or commuter job instructions. */
    public static List<String> localDialogue(String mapId, int voice, List<String> fallback) {
        List<List<String>> voices = switch (mapId) {
            case "village_oakhaven" -> List.of(
                    List.of("The seedhouse keeps our planting seed dry and our oven working.",
                            "We honor the Keeper of the Returning Seed by returning seed that will actually grow. An empty sack and a prayer won't plant a field.",
                            "The first bread goes to a sick neighbor. After that, the baker gets an argument about whose turn it is to fetch wood."),
                    List.of("A new household shares its first meal with the people leaving. That is how we pass on care of the hearth.",
                            "It is harder when nobody knows where the old tenants went. The Hearth Guild is trying to find a fair custom for that.",
                            "If Oathstead needs seed, tell them to keep the trays out of the smoke."));
            case "village_elderford" -> List.of(
                    List.of("The Boundary Stewards mend the forest markers. Royal surveyors keep moving their lines on paper.",
                            "A grant from Archive City does not tell you when the grove is resting. Ask before you prune.",
                            "We leave tools at the wayshrine because there is work to do. Expensive offerings will not mend its roof."),
                    List.of("Our oven belongs to the households who tend it. We take turns carrying wood and clearing the ash.",
                            "People call it an old protection. I know bread keeps better here than in the empty houses.",
                            "That does not make every rumor about a household spirit true. Look at the house before blaming its keeper."));
            case "city_archive" -> List.of(
                    List.of("The royal shelves hold ward records. The Hearth Guild keeps the ovens working while the clerks search them.",
                            "Some household protections were passed from baker to apprentice and never written down.",
                            "We need the records and the people who remember the work. Neither can replace the other."),
                    List.of("A landlord can change a lease in a morning. Passing responsibility for a protected hearth takes longer.",
                            "The guild wants shared meals for lodgers too. Some owners object to the cost of keeping a public oven.",
                            "Oakhaven has managed it for years. Cities can learn from villages."));
            default -> List.of();
        };
        return voices.isEmpty() ? fallback : voices.get(Math.floorMod(voice, voices.size()));
    }
}
