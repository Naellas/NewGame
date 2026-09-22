package com.alderfall.game;

import java.util.List;

/** The outward western pass; inner Crownlands content belongs to HearthlandsFolklore. */
public final class WesternReachFolklore {
    public static final String ABBEY = "folklore_briarbridge_guest_abbey";
    public static final String ORCHARD_HOUSE = "folklore_returning_seedhouse";
    public static final String BOUNDARY = "folklore_boundary_shrine";
    public static final String CAMP_SUPPLIES = "folklore_redcap_supply_shelter";
    public static final String GARDEN_GATE = "folklore_open_bough_gate";
    public static final String SEALED_MIRROR = "folklore_sealed_mirror_door";
    public static final String GARDEN_ID = "garden_briarbridge_abbey";
    public static final String UNDERCROFT_ID = "undercroft_briarbridge_abbey";

    private WesternReachFolklore() { }

    public static String buildingName(String mapId, CityBuilding building) {
        if (building == null) return "";
        String key = building.key();
        if (mapId.equals("town_briarbridge") && (key.equals("bridge_inn") || key.startsWith("bridge_inn_")))
            return "Briarbridge Guest Abbey";
        if (mapId.equals("city_riverside") && (key.equals("river_hall") || key.startsWith("river_hall_")))
            return "Riverside Charter Hall";
        if (mapId.equals("village_foxbarrow") && key.equals("west_cottage"))
            return "Foxbarrow Orchard House";
        return "";
    }

    public static String buildingAsset(String mapId, CityBuilding building) {
        return switch (buildingName(mapId, building)) {
            case "Briarbridge Guest Abbey" -> ABBEY;
            case "Foxbarrow Orchard House" -> ORCHARD_HOUSE;
            default -> "";
        };
    }

    public static String buildingDescription(String mapId, CityBuilding building) {
        return switch (buildingName(mapId, building)) {
            case "Briarbridge Guest Abbey" -> "A carved open bough crowns pale stone and patched slate. A sheltered petition bench faces the public approach. Travelers may ask for a hearing before accepting a host's invitation.";
            case "Riverside Charter Hall" -> "River trade pays for this hall. Its clerks preserve crossing agreements, including duties owed by toll collectors as well as travelers.";
            case "Foxbarrow Orchard House" -> "A seed lintel shelters planting drawers and a bread oven. Orchard keepers lend tools and explain which gifts from the woodland need their terms spoken aloud.";
            default -> "";
        };
    }

    public static String interiorTheme(String mapId) {
        return switch (mapId) {
            case "town_briarbridge" -> "inn";
            case "city_riverside" -> "study";
            default -> "bakery";
        };
    }

    public static String residentName(String mapId, int index) {
        return switch (mapId) {
            case "town_briarbridge" -> index == 0 ? "Open Bough Host" : "Abbey Gardener";
            case "city_riverside" -> index == 0 ? "Crossing Clerk" : "River Advocate";
            default -> index == 0 ? "Orchard Keeper" : "Orchard Neighbor";
        };
    }

    public static String observation(String mapId, String asset) {
        if (asset.equals(GARDEN_GATE))
            return "The open bough marks the abbey's public orchard. Shelter here does not require accepting a private host's invitation.";
        if (asset.equals(SEALED_MIRROR))
            return "Oak bars seal the tarnished mirror-door. Its old guest passage is closed; there is no open way through the glass.";
        if (asset.equals(CAMP_SUPPLIES))
            return "A patched canopy keeps Redcap's grain, spare handles and boots dry. The camp has workers to feed and equipment to mend.";
        if (asset.equals(BOUNDARY))
            return "Fresh cord and clean shears mark a tended woodland boundary. The tools are for maintenance; the bowl holds no coins.";
        return switch (mapId) {
            case GARDEN_ID -> switch (asset) {
                case "deco_tree_young", "village_prop_seedling_tray" -> "Young trees mark completed journeys. The abbey records who will water each one after the travelers leave.";
                case "village_prop_bench" -> "The petition bench is open to anyone asking to be heard. Resting here does not settle another person's claim over a guest.";
                case "village_prop_produce_basket" -> "A basket of orchard fruit is freely shared. The gardener asks only that unripe fruit stays on the trees.";
                case "village_prop_farm_tools" -> "Clean shears and spare ties wait beside the orchard rows. Remembering a journey includes tending the tree planted for it.";
                case "location_dungeon_stair_entrance" -> "Worn steps descend beneath the orchard roots to the abbey's sealed guest passage.";
                default -> "";
            };
            case UNDERCROFT_ID -> switch (asset) {
                case "location_camp_crates" -> "Spare hinges and sealing timber are stored near the mirror. The abbey maintains this closed threshold instead of abandoning it.";
                case "location_dungeon_stair_entrance" -> "Daylight reaches these stairs. They lead back to the public orchard.";
                default -> "";
            };
            case "town_briarbridge" -> switch (asset) {
                case "village_prop_bench" -> "The abbey's petition bench faces the public path. A traveler may ask for a hearing before either claimant takes them away.";
                case "village_prop_clay_oven" -> "Bread is cooling for travelers. Ordinary meals carry no hidden bargain; a formal invitation must name its host and terms.";
                case "village_prop_seedling_tray" -> "Young trees wait by the guest house. The Open Bough plants a tree to remember a completed journey.";
                default -> "";
            };
            case "city_riverside" -> switch (asset) {
                case "city_prop_cart" -> "A grain cart waits near the market. Crossing charters govern when a baron may take a toll, and when passage must remain free.";
                case "village_prop_bench" -> "Petitioners share a bench with river workers. A bridge charter binds its keepers as well as the people crossing.";
                default -> "";
            };
            case "village_foxbarrow" -> switch (asset) {
                case "village_prop_produce_basket" -> "Orchard fruit is set out for neighbors. A gift from the Briar Courts would need its terms declared; this basket is simply shared food.";
                case "village_prop_farm_tools" -> "Pruning tools have been cleaned and put away. Orchard keepers ask about the grove's season before cutting living wood.";
                default -> "";
            };
            case "dungeon_redcap_camp_1" -> switch (asset) {
                case "location_camp_fire" -> "Redcap's cooking fire serves workers as well as guards. A camp needs meals and repairs, whatever its leaders intend.";
                case "location_camp_crates" -> "Provision crates have been dragged in from the supply route. The camp depends on carriers as well as fighters.";
                case "village_prop_woodpile" -> "Split timber and spare stakes lie ready for repairs. Redcap's occupants expect to remain here.";
                default -> "";
            };
            default -> "";
        };
    }

    public static List<String> localDialogue(String mapId, int voice, List<String> fallback) {
        List<List<String>> voices = switch (mapId) {
            case "town_briarbridge" -> List.of(
                    List.of("The Guest Abbey keeps a petition bench for anyone asking to be heard. A pursuer's title does not settle the matter.",
                            "We honor the Wayfarer Beneath the Bough by sheltering strangers without claiming to own them.",
                            "The open-bough gate beside the abbey leads to our public orchard. Ask who is hosting before accepting a formal invitation; supper from our kitchen is just supper."),
                    List.of("A novice thinks every stranger in the orchard is a disguised court noble. I mostly catch them taking unripe pears.",
                            "We plant a tree when someone returns from a long journey. That means watering it after the celebration.",
                            "The Briar Courts care about promises. So do we, but we want the terms spoken where everyone can hear."));
            case "city_riverside" -> List.of(
                    List.of("The Charter Hall keeps the old crossing agreements. They set duties for the toll collectors too.",
                            "A bridge needs timber and labor. That does not mean its keeper can charge whatever hunger will make people pay.",
                            "Bring the old terms into the argument. A lord's seal is evidence of a claim, not the whole history."),
                    List.of("Merchants want the river road open. The farms want their grain to reach market without another toll.",
                            "The Open Bough's hosts take in travelers while the town argues about who must pay for the crossing.",
                            "Stories about a bridge refusing its own lord are a warning here: remember what your charter actually promises."));
            case "village_foxbarrow" -> List.of(
                    List.of("The Orchard House keeps spare seed and tools. If your planting fails, ask before you go hungry.",
                            "Fruit from a neighbor is a gift. If a visitor from the Briar Courts offers you something, ask whether it is freely given.",
                            "Most days the woods are just wet and full of work. Strange visitors are no excuse to stop looking after the orchard."),
                    List.of("That cord on the woodland marker was replaced last week. Our boundary needs care, not a heap of coins.",
                            "A court messenger may call a gift a kindness and remember it later as a debt. Have the terms spoken plainly.",
                            "We keep the road to Riverside clear. The orchard cannot eat all its own harvest."));
            default -> List.of();
        };
        return voices.isEmpty() ? fallback : voices.get(Math.floorMod(voice, voices.size()));
    }

    public static List<String> gardenDialogue(int voice) {
        return switch (voice) {
            case 0 -> List.of("Those young trees mark journeys that ended safely. I ask who will water each one before we plant it.",
                    "The novice suspects our orchard visitors are disguised nobles. I worry more about them pocketing unripe pears.",
                    "You may take fruit from the basket. It is freely given. The steps at the back lead to the old sealed mirror-door.");
            case 1 -> List.of("If someone is pursuing you, you can ask to be heard at this bench. The abbey's shelter does not make you our servant.",
                    "A formal guest invitation names its host and terms. You may refuse it and still ask for our protection.",
                    "A lord's seal and a Briar envoy's claim both need examining. Neither tells us what the person being claimed wants.");
            default -> List.of("The mirror-door once admitted otherworldly guests. Those bars keep the old crossing closed.",
                    "We replace rotten timber and check the brackets. A closed door still needs a keeper.",
                    "There is no invitation to accept here today. The stairs behind you return to the orchard.");
        };
    }
}
