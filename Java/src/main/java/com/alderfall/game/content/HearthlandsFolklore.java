package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.util.List;

/** The first outward worldbuilding pass: Oathstead and its Crownlands neighbours. */
public final class HearthlandsFolklore {
    public static final String OVEN = "folklore_shared_oven";
    public static final String SEEDHOUSE = "folklore_seedhouse";
    public static final String WARD = "folklore_hearth_ward";

    private HearthlandsFolklore() { }

    public static String buildingName(String mapId, CityBuilding building) {
        if (building == null) return "";
        if ("village_oakhaven".equals(mapId) && "west_cottage".equals(building.key())) {
            return "House of Returning Seed";
        }
        if ("village_elderford".equals(mapId) && "north_cottage".equals(building.key())) {
            return "Elderford Seed Exchange";
        }
        return "";
    }

    public static String buildingAsset(String mapId, CityBuilding building) {
        if (buildingName(mapId, building).isEmpty()) return "";
        return "village_oakhaven".equals(mapId) ? FolkloreContent.SEEDHOUSE : SEEDHOUSE;
    }

    public static String buildingDescription(String mapId, CityBuilding building) {
        if (buildingName(mapId, building).isEmpty()) return "";
        return "A seed-and-leaf lintel shelters drawers of planting seed. Repaired roof tiles and borrowed tools "
                + "show how neighbours keep this house working. Seed is returned after harvest; the first bread goes to an ill neighbour.";
    }

    public static List<String> seedhouseDialogue(String mapId, int voice) {
        if (voice != 0) return List.of("The first loaf is for a neighbour who cannot bake today.",
                "I am learning the oven repairs as well as the recipes. A hearth needs someone who knows both.");
        return "village_oakhaven".equals(mapId)
                ? List.of("Welcome to the House of Returning Seed. We keep planting seed and share the work of this oven.",
                        "The Keeper of the Returning Seed is honoured by what grows again. We test the seed before putting it away.",
                        "A donor offered roof repairs in exchange for closing the teaching garden. We have not agreed to those terms.")
                : List.of("This is Elderford's seed exchange. These shelves hold seed brought back after harvest.",
                        "Oakhaven helped us after the flood. Now we send back seed with notes about where it grew best.",
                        "The bread on that shelf is already promised. Sharing an oven means remembering who cannot come here themselves.");
    }

    public static String observation(String mapId, WorldProp prop) {
        if (prop == null) return "";
        if (OVEN.equals(prop.asset())) {
            return WorldMap.PLAYER_VILLAGE_ID.equals(mapId)
                    ? "The shared oven has a shelf for bread meant for sick neighbours. Dry wood and a repaired flue keep the hearth useful."
                    : "The shared oven has a swept bread shelf and dry fuel under its roof. The first loaf is set aside for a neighbour too ill to bake.";
        }
        if (WARD.equals(prop.asset())) {
            return "Fresh straw and a mended roof mark this household ward. Someone has swept its shelf and replaced the bread; there are no coins in the dish.";
        }
        if (WorldMap.PLAYER_VILLAGE_ID.equals(mapId)) {
            return switch (prop.asset()) {
                case "location_camp_crates" -> "Oathstead's rescue store holds blankets and spare bowls. Arriving households can borrow what they need.";
                case "village_prop_seedling_tray" -> "These seedlings came from neighbouring gardens. The labels record who can explain their care.";
                default -> FolkloreContent.observation(mapId, prop.asset());
            };
        }
        if ("dungeon_redcap_camp_1".equals(mapId)) {
            return switch (prop.asset()) {
                case "location_camp_crates" -> "Food sacks share this stack with patched boots and spare handles. The camp needs carriers and repairers as well as guards.";
                case "location_camp_fire" -> "Cooking pots hang above the embers. Separate bowls have been set out beside the workers' bedrolls.";
                default -> FolkloreContent.observation(mapId, prop.asset());
            };
        }
        return FolkloreContent.observation(mapId, prop.asset());
    }

    static String[] conversation(String mapId, int seed) {
        return switch (mapId) {
            case WorldMap.PLAYER_VILLAGE_ID -> switch (Math.floorMod(seed, 3)) {
                case 0 -> new String[]{"Leave the first loaf on the oven shelf. I promised to carry it to our sick neighbour.",
                        "Take my basket. I will cover the next batch while you are away."};
                case 1 -> new String[]{"The new arrivals need blankets. Do we have enough in the rescue store?",
                        "Two dry ones. I will mend the torn one before nightfall."};
                default -> new String[]{"Someone left a coin at the ward again. Its roof still needs fixing.",
                        "I have a spare tile. Help me hold the ladder after supper."};
            };
            case "village_oakhaven" -> switch (Math.floorMod(seed, 3)) {
                case 0 -> new String[]{"The House of Returning Seed has beans left, if your last planting failed.",
                        "I will ask the keeper. I can bring back seed after harvest."};
                case 1 -> new String[]{"The keeper says the Hearthkin ignored a jewel and helped mend the oven instead.",
                        "Then I will bring the hinge I promised. We need that door to close either way."};
                default -> new String[]{"A donor offered to repair the seedhouse roof if we close the garden to visitors.",
                        "We teach the children there. Ask for the terms in writing before anyone agrees."};
            };
            case "village_elderford" -> switch (Math.floorMod(seed, 2)) {
                case 0 -> new String[]{"Keep the dry seed upstairs. Last year's flood reached the lower shelf.",
                        "I marked the waterline on the post. The new keeper should see it too."};
                default -> new String[]{"Oakhaven lent us planting seed after the flood. Our sacks are ready to go back.",
                        "Send the planting notes with them. These beans liked the higher ground."};
            };
            case "city_archive" -> switch (Math.floorMod(seed, 2)) {
                case 0 -> new String[]{"The village ward is missing from this register, but its keeper has repair accounts going back thirty years.",
                        "Copy the accounts beside the map. A blank space is not proof that nobody lived there."};
                default -> new String[]{"Oakhaven's garden appears as private land in this copy. The older page calls it shared ground.",
                        "Keep both pages. The people using the garden deserve to know when that changed."};
            };
            case "town_briarbridge" -> switch (Math.floorMod(seed, 2)) {
                case 0 -> new String[]{"Tell the traveller whether that bread is a gift before you hand it over.",
                        "Freely given. I want no one owing me a favour for their supper."};
                default -> new String[]{"The bridge charter promises free grain passage during famine. The toll clerk wants the newer copy.",
                        "Bring both to the Bridge Court. The millers should hear the difference."};
            };
            case "town_moonspire" -> switch (Math.floorMod(seed, 2)) {
                case 0 -> new String[]{"The survey shows an empty plot where three households share their oven.",
                        "Bring the surveyor here while they are baking. We can correct the map together."};
                default -> new String[]{"The Hearth Guild asked who will tend the oven after the tenants leave.",
                        "Put that in the new lease. A shared meal should include the people taking over."};
            };
            default -> null;
        };
    }
}
