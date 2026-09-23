package com.alderfall.game;

import java.nio.file.*;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import com.alderfall.game.ui.InventoryRenderer;

public final class ChestSystemTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        state.chooseClass("Knight");
        state.mode = GameMode.EXPLORE;
        state.currentMapId = state.world.adventureMarkers().stream().map(m -> m.mapId())
                .filter(id -> state.world.props(id).stream().anyMatch(ChestSystem::isChest)).findFirst().orElseThrow();
        WorldProp chest = state.world.props(state.currentMapId).stream().filter(ChestSystem::isChest).findFirst().orElseThrow();
        state.playerX = chest.x(); state.playerY = chest.y();
        state.openChest(chest);
        check(state.mode == GameMode.CHEST, "Adjacent chest opens");
        Map<String, Integer> original = new LinkedHashMap<>(state.chestContents());
        check(original.keySet().stream().anyMatch(k -> MaterialCatalog.get(k) != null), "Material loot");
        check(original.keySet().stream().anyMatch(k -> GameData.EQUIPMENT.containsKey(k)), "Weapon loot");
        check(original.equals(new ChestSystem().contents(state.currentMapId, chest)), "Stable loot");
        String key = original.keySet().iterator().next();
        int before = state.player.inventory.getOrDefault(key, 0);
        state.transferChestItem(key, true, 1);
        check(state.player.inventory.get(key) == before + 1, "Take one");
        state.transferChestItem(key, false, 1);
        check(original.equals(state.chestContents()), "Deposit one conserves contents");
        state.transferChestItem(key, true, -1);
        check(original.equals(state.chestContents()), "Reject negative quantity");
        for (String item : new ArrayList<>(state.chestContents().keySet())) state.transferChestItem(item, true, Integer.MAX_VALUE);
        check(state.chestContents().isEmpty(), "Take all empties chest");
        state.closeOverlay(); state.openChest(chest);
        check(state.chestContents().isEmpty(), "Reopening does not refill");
        Path saveRoot = Files.createTempDirectory("alderfall-chest-test-");
        SaveSystem saves = new SaveSystem(saveRoot);
        saves.save(state, "Empty chest");
        String saveId = state.currentSaveId;
        state.transferChestItem(key, false, 1);
        check(saves.load(state, saveId), "Load save");
        state.openChest(chest);
        check(state.chestContents().isEmpty(), "Empty chest persists through actual save/load");
        state.transferChestItem(key, false, 1);
        saves.save(state, "Stored item");
        saveId = state.currentSaveId;
        state.transferChestItem(key, true, 1);
        saves.load(state, saveId); state.openChest(chest);
        check(state.chestContents().getOrDefault(key, 0) == 1, "Stored item persists");
        state.closeOverlay();
        int packBefore = state.player.inventory.getOrDefault(key, 0);
        state.transferChestItem(key, true, 1);
        check(packBefore == state.player.inventory.getOrDefault(key, 0), "No transfer when closed");
        state.playerX += 10; state.openChest(chest);
        check(state.mode == GameMode.EXPLORE, "Remote chest cannot open");
        state.playerX -= 10; state.openChest(chest);
        render(state);
        ChestSystem legacy = new ChestSystem(); legacy.read(new Properties());
        check(!legacy.contents("legacy", chest).isEmpty(), "Old saves receive loot");
        System.out.println("ChestSystemTest passed: loot, transfers, interaction guards, rendering, save/load.");
    }

    private static void render(GameState state) throws Exception {
        java.util.List<UiButton> buttons = new ArrayList<>();
        java.util.List<TooltipZone> tips = new ArrayList<>();
        InventoryRenderer.Effects effects = (InventoryRenderer.Effects) Proxy.newProxyInstance(
                InventoryRenderer.Effects.class.getClassLoader(), new Class<?>[]{InventoryRenderer.Effects.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "gameAreaWidth" -> 1200;
                    case "viewHeight" -> 900;
                    case "gameAreaCenteredX" -> (1200 - (int) args[0]) / 2;
                    case "inventoryDragZones", "inventoryDropZones" -> new ArrayList<>();
                    case "tooltipZones" -> tips;
                    case "buttons" -> buttons;
                    case "actionButton" -> {
                        if ((boolean) args[9]) buttons.add(new UiButton(new Rectangle((int) args[1], (int) args[2], (int) args[3], (int) args[4]), (String) args[5], (Runnable) args[6]));
                        yield null;
                    }
                    default -> null;
                });
        InventoryRenderer renderer = new InventoryRenderer(new AssetStore(Path.of("assets").toAbsolutePath()), state, effects);
        BufferedImage image = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        renderer.drawChest(g); g.dispose();
        check(!tips.isEmpty(), "Item tooltips rendered");
        check(buttons.stream().anyMatch(b -> b.label().equals("Take 1")), "Take button rendered");
        check(buttons.stream().anyMatch(b -> b.label().equals("Store 1")), "Store button rendered");
    }
}
