package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Trade boundaries include upgrades and the actual NPC dialogue/purchase path. */
public final class ShopStockTest {
    public static void main(String[] args) throws Exception {
        for (String trade : List.of("blacksmith", "carpenter", "shop", "workshop", "apothecary",
                "alchemist", "alchemy", "inn", "tavern", "bakery", "warehouse", "forestry_hut",
                "mine", "hunting_camp", "farmstead", "garden", "granary", "fishing_hut", "guild",
                "shrine", "watchtower", "house", "row")) {
            List<String> stock = new ArrayList<>(Shop.baseStock(trade));
            stock.addAll(Shop.upgradedStock(trade));
            for (String key : stock) {
                require(!GameData.itemName(key).equals(key), "Unknown stock: " + trade + ": " + key);
                require(GameData.itemCost(key) > 0, "Unpriced stock: " + key);
                if (List.of("farmstead", "garden", "granary", "bakery", "inn", "apothecary", "alchemy").contains(trade))
                    require(GameData.equipment(key) == null, "Equipment in " + trade + ": " + key);
            }
        }
        require(Shop.baseStock("carpenter").containsAll(List.of("wood", "oak_wood", "new_weapon_willow_shortbow")), "Carpenter supplies");
        require(Shop.baseStock("apothecary").containsAll(List.of("herb_leaf", "potion_small", "herbal_salve")), "Herbalist supplies");
        require(Shop.baseStock("blacksmith").stream().allMatch(GameData::isEquipment), "Smith sells equipment");
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        state.chooseClass("Knight");
        var interiors = WorldMap.class.getDeclaredMethod("interiorNpcsFor", String.class, String.class, int.class, int.class, int.class);
        interiors.setAccessible(true);
        for (var entry : Map.of("blacksmith", "blacksmith", "carpenter", "carpenter", "alchemy", "apothecary",
                "bakery", "bakery", "inn", "inn", "tavern", "inn").entrySet()) {
            @SuppressWarnings("unchecked")
            List<Npc> residents = (List<Npc>) interiors.invoke(state.world, "test_interior", entry.getKey(), 24, 24, 42);
            require(Shop.forNpc(residents.getFirst()).stock().equals(Shop.baseStock(entry.getValue())), "Interior shop " + entry);
            require(residents.stream().skip(1).allMatch(n -> Shop.forNpc(n) == null), "Customers/apprentices are not merchants");
        }
        for (NpcJob job : List.of(NpcJob.farmer(), NpcJob.herbalist(), NpcJob.woodcutter())) {
            // A stale general market ID must not override a worker's trade.
            Npc worker = new Npc(state.currentMapId, "Test Worker", "npc_citizen_man", state.playerX, state.playerY,
                    List.of("My goods."), null, "highwall", job);
            state.mode = GameMode.EXPLORE;
            require(state.talkToNpc(worker), "Worker dialogue opens");
            state.openActiveShop();
            require(state.mode == GameMode.SHOP, "Worker shop opens");
            String item = state.shopStock(state.activeShop).getFirst();
            state.player.gold = 10000;
            int count = state.player.inventory.getOrDefault(item, 0);
            state.buyShopItem(0);
            require(state.player.inventory.getOrDefault(item, 0) == count + 1, "Worker purchase delivers goods");
            require(state.player.gold == 10000 - GameData.itemCost(item), "Worker purchase costs gold");
        }
        System.out.println("Shop stock passed: trade boundaries, upgrades, generated merchants, worker purchases.");
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
