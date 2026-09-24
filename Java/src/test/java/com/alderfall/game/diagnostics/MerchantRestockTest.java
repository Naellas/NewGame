package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.*;
import java.util.*;

/** Regional catalogues, finite deliveries, persisted depletion and Gameplay settings. */
public final class MerchantRestockTest {
    public static void main(String[] args) throws Exception {
        Shop mine = Shop.forTrade("mine", "Mine");
        ShopInventory inventory = new ShopInventory();
        Set<String> sunOffers = new HashSet<>(), northOffers = new HashSet<>();
        for (int day = 1; day <= 61; day += 3) {
            Map<String, Integer> sun = inventory.stock("sun mine", mine, InteriorStyle.SUNREALM, day, 3);
            Map<String, Integer> north = inventory.stock("north mine", mine, InteriorStyle.STORMBOUND, day, 3);
            require(!north.containsKey("sunmetal_ore") && !north.containsKey("emberite_ore"), "Southern ore leaked north");
            require(!sun.containsKey("froststeel_ore") && !sun.containsKey("mithril_ore"), "Northern ore leaked south");
            sunOffers.addAll(sun.keySet()); northOffers.addAll(north.keySet());
        }
        require(sunOffers.containsAll(List.of("sunmetal_ore", "emberite_ore", "obsidian_shard")), "Southern variants never offered");
        require(northOffers.containsAll(List.of("froststeel_ore", "mithril_ore", "cobalt_ore")), "Northern variants never offered");
        Shop smith = GameData.SHOPS.get("blacksmith");
        for (InteriorStyle region : InteriorStyle.values()) {
            for (Shop shop : GameData.SHOPS.values()) {
                var stock = inventory.stock(region + shop.id(), shop, region, 1, 3);
                require(!stock.isEmpty(), "Empty trade " + shop.id() + " in " + region);
                for (String key : stock.keySet()) {
                    require(GameData.itemCost(key) > 0 && !GameData.itemName(key).equals(key), "Invalid item " + key);
                    require(MerchantStock.local(key, region), "Nonlocal item " + key);
                    if (Set.of("apothecary", "bakery", "inn", "farmstead", "mine", "warehouse").contains(shop.id()))
                        require(!GameData.isEquipment(key), "Off-trade equipment " + shop.id() + ": " + key);
                }
            }
            List<String> crafted = AssemblyCrafting.regionalVendorStock(region, 42);
            require(crafted.size() == AssemblyCrafting.BLUEPRINTS.size() * AssemblyCrafting.Quality.values().length,
                    "Missing blueprint or quality in " + region);
            for (String key : crafted) require(GameData.equipment(key) != null && MerchantStock.local(key, region), "Invalid regional assembly");
        }
        require(MerchantStock.candidates(smith).contains("starforged_plate"), "High-level smith variants omitted");
        require(MerchantStock.candidates(GameData.SHOPS.get("carpenter")).contains("new_weapon_frostpine_bow"), "Bow variants omitted");
        require(!MerchantStock.local("gear1~sword~STANDARD~sunmetal_ingot~STANDARD+wood~STANDARD+bone~STANDARD+-", InteriorStyle.STORMBOUND), "Assembled material bypass");

        Shop farm = Shop.forTrade("farmstead", "Farm");
        inventory.clear();
        var original = new LinkedHashMap<>(inventory.stock("farmer A", farm, InteriorStyle.HEARTHLANDS, 1, 3));
        String item = "garden_vegetables";
        for (int i = 0; i < original.get(item); i++) require(inventory.purchase("farmer A", item), "Purchase fails early");
        require(!inventory.purchase("farmer A", item), "Purchase exceeds stock");
        require(inventory.stock("farmer A", farm, InteriorStyle.HEARTHLANDS, 3, 3).get(item) == 0, "Early refill");
        require(inventory.stock("farmer B", farm, InteriorStyle.HEARTHLANDS, 3, 3).get(item) > 0, "Merchants share depletion");
        Properties props = new Properties(); inventory.write(props);
        ShopInventory restored = new ShopInventory(); restored.read(props);
        require(restored.stock("farmer A", farm, InteriorStyle.HEARTHLANDS, 3, 3).get(item) == 0, "Load refills stock");
        var next = restored.stock("farmer A", farm, InteriorStyle.HEARTHLANDS, 4, 3);
        require(next.get(item) > 0 && restored.nextDay("farmer A", 3) == 7, "Exact restock boundary");
        restored.stock("farmer A", farm, InteriorStyle.HEARTHLANDS, 20, 3);
        require(restored.nextDay("farmer A", 3) == 22, "Skipped days drift restock schedule");
        restored.read(new Properties());
        require(restored.stock("old save", farm, InteriorStyle.HEARTHLANDS, 9, 3).get(item) > 0, "Old saves get no goods");

        Path scratch = Files.createTempDirectory(Path.of("temp"), "merchant-restock-");
        Files.createDirectories(scratch.resolve("config"));
        Files.writeString(scratch.resolve("config/gameplay.json"), "{\"shop_restock_days\": 5}");
        GameConfig config = GameConfig.load(scratch);
        require(config.shopRestockDays == 5, "JSON setting ignored");
        config.shopRestockDays = 7; config.save(scratch);
        require(GameConfig.loadWithSettings(scratch).shopRestockDays == 7, "Gameplay preference not persisted");
        require(GameConfig.clampShopRestockDays(0) == 1 && GameConfig.clampShopRestockDays(50) == 30, "Restock bounds");
        GameState state = new GameState(config); state.chooseClass("Knight");
        state.currentMapId = "house_village_snowrest_10_12";
        require(state.shopRegion() == InteriorStyle.STORMBOUND, "Interior loses region");
        state.currentMapId = "town_northwatch";
        require(state.shopRegion() == InteriorStyle.STORMBOUND, "Northern freehold loses climate");
        state.currentMapId = "village_dunewick";
        require(state.shopRegion() == InteriorStyle.SUNREALM, "Sun settlement loses region");
        state.currentMapId = WorldMap.PLAYER_VILLAGE_ID;
        state.activeNpc = null; state.activeShop = farm; state.mode = GameMode.SHOP;
        state.player.gold = 10000;
        int index = state.shopStock(farm).indexOf(item);
        int available = state.shopQuantities(farm).get(item);
        state.player.gold = 0;
        state.buyShopItem(index);
        require(state.shopQuantities(farm).get(item) == available, "Failed purchase consumes stock");
        state.player.gold = 10000;
        for (int i = 0; i < available; i++) state.buyShopItem(index);
        int gold = state.player.gold;
        state.buyShopItem(index);
        require(state.player.gold == gold && state.shopQuantities(farm).get(item) == 0, "Sold-out purchase charges gold");
        SaveSystem saves = new SaveSystem(scratch); saves.save(state, "Merchant test");
        String saveId = state.currentSaveId;
        state.shopInventory.clear();
        require(saves.load(state, saveId), "Save reload failed");
        state.activeNpc = null; state.activeShop = farm;
        require(state.shopQuantities(farm).get(item) == 0, "SaveSystem loses depleted stock");
        state.worldTick = 7 * GameState.TICKS_PER_GAME_DAY;
        require(state.shopQuantities(farm).get(item) > 0, "Configured seven-day refill failed");
        System.out.println("Merchant restocks passed: regional variants, all assembly families/qualities, depletion, timing, save/load, settings.");
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
