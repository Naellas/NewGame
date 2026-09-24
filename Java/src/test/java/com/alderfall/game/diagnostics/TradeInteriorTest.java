package com.alderfall.game;

import com.alderfall.game.map.InteriorLayout;
import com.alderfall.game.map.WorldMap;
import java.util.Map;

/** Profession routing, joined authored groups and usable modular workstations. */
public final class TradeInteriorTest {
    public static void main(String[] args) throws Exception {
        WorldMap world = new WorldMap(42);
        var theme = WorldMap.class.getDeclaredMethod("interiorTheme", CityBuilding.class, int.class);
        theme.setAccessible(true);
        for (var entry : Map.of("alchemist", "alchemy", "apothecary", "alchemy", "bakery", "bakery",
                "blacksmith", "blacksmith", "workshop", "carpenter").entrySet()) {
            var building = new CityBuilding("market_shop", 2, 2, 5, 5, entry.getKey(), 0);
            require(entry.getValue().equals(theme.invoke(world, building, 42)), "Wrong trade layout: " + entry);
        }
        for (String profession : new String[]{"joinery", "bakehouse", "apothecary", "archive", "smith"}) {
            String layout = switch(profession) {
                case "joinery" -> "carpenter"; case "bakehouse" -> "bakery"; case "apothecary" -> "alchemy";
                case "archive" -> "study"; default -> "blacksmith";
            };
            var plan = InteriorLayout.compose(layout, 0, InteriorStyle.HEARTHLANDS);
            for (String kind : new String[]{"storage", "worktop"}) {
                String asset = "interior_" + profession + "_" + kind;
                require(plan.props().stream().anyMatch(p -> p.asset().equals(asset)
                        && ConnectedFurniture.connections(p, plan.props()) != 0), "Missing authored run: " + asset);
            }
        }
        for (var entry : Map.of("smith_worktop", CraftingSystem.Workstation.ANVIL,
                "smith_hearth", CraftingSystem.Workstation.ANVIL,
                "joinery_worktop", CraftingSystem.Workstation.CARPENTER,
                "bakehouse_worktop", CraftingSystem.Workstation.OVEN,
                "apothecary_worktop", CraftingSystem.Workstation.ALCHEMY).entrySet()) {
            String asset = "interior_" + entry.getKey();
            require(CraftingSystem.workstationForAsset(asset) == entry.getValue(), "Nonfunctional workstation: " + asset);
            require(CraftingSystem.workstationFootprint(asset)[0] == 2, "Incomplete crafting reach footprint");
        }
        System.out.println("Trade interiors passed: profession routing, ten authored modular runs, five crafting stations.");
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
