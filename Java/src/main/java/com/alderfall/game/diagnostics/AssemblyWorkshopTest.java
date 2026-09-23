package com.alderfall.game;

import com.alderfall.game.ui.InventoryRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Exercise real workshop navigation and render it without launching a game window. */
public final class AssemblyWorkshopTest {
    public static void main(String[] args) throws Exception {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        state.chooseClass("Knight"); state.mode = GameMode.CRAFTING;
        for (Profession profession : Profession.ALL) state.player.professionXp.put(profession.id(), Profession.xpForLevel(10));
        for (AssemblyCrafting.Slot slot : List.of(AssemblyCrafting.Slot.BLADE, AssemblyCrafting.Slot.HILT)) {
            String material = slot == AssemblyCrafting.Slot.BLADE ? "mithril_ingot" : "wood";
            state.player.addItem(new AssemblyCrafting.Component(slot, MaterialCatalog.get(material), AssemblyCrafting.Quality.MASTERWORK).key(), 1);
        }
        List<UiButton> buttons = new ArrayList<>();
        List<TooltipZone> tips = new ArrayList<>();
        InventoryRenderer.Effects effects = (InventoryRenderer.Effects) Proxy.newProxyInstance(
                InventoryRenderer.Effects.class.getClassLoader(), new Class<?>[]{InventoryRenderer.Effects.class},
                (proxy, method, params) -> {
                    switch (method.getName()) {
                        case "buttons": return buttons;
                        case "tooltipZones": return tips;
                        case "gameAreaCenteredX": return 20;
                        case "gameAreaWidth": return 980;
                        case "viewHeight": return 800;
                        case "drawOverlayBase": {
                            Graphics2D g = (Graphics2D) params[0]; g.setColor(new Color(17, 20, 29));
                            g.fillRoundRect((int) params[1], (int) params[2], (int) params[3], (int) params[4], 16, 16); return null;
                        }
                        case "actionButton": {
                            Graphics2D g = (Graphics2D) params[0];
                            Rectangle bounds = new Rectangle((int) params[1], (int) params[2], (int) params[3], (int) params[4]);
                            String label = (String) params[5]; boolean enabled = (boolean) params[9];
                            if (enabled) buttons.add(new UiButton(bounds, label, (Runnable) params[6]));
                            g.setColor((Color) params[7]); g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
                            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                            g.setColor(enabled ? Color.WHITE : Color.GRAY);
                            g.drawString(label, bounds.x + 8, bounds.y + 20); return null;
                        }
                        default: return method.getReturnType() == int.class ? 0 : null;
                    }
                });
        InventoryRenderer renderer = new InventoryRenderer(new AssetStore(Path.of("assets")), state, effects);
        BufferedImage image = new BufferedImage(980, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        renderer.drawCrafting(g);
        click(buttons, "Equipment Workshop"); buttons.clear(); renderer.drawCrafting(g);
        require(renderer.displayedCraftingRecipes().isEmpty(), "Workshop must disable recipe number shortcuts");
        require(buttons.stream().anyMatch(b -> b.label().startsWith("Copper Ingot")), "Material picker");
        clickPrefix(buttons, "Copper Ingot"); buttons.clear(); renderer.drawCrafting(g);
        require(buttons.stream().anyMatch(b -> b.label().startsWith("Tin Ingot | Owned:")), "Compatible materials listed together");
        require(buttons.stream().noneMatch(b -> b.label().contains("Wood") || b.label().startsWith("Prepare ")), "Picker filters incompatible materials and blocks underlying controls");
        state.player.addItem("copper_ingot", 1); state.player.addItem("iron_ingot", 3);
        buttons.clear(); tips.clear(); renderer.drawCrafting(g);
        UiButton copperTile = buttons.stream().filter(b -> b.label().startsWith("Copper Ingot | Owned:")).findFirst().orElseThrow();
        UiButton bronzeTile = buttons.stream().filter(b -> b.label().startsWith("Bronze Ingot | Owned:")).findFirst().orElseThrow();
        require(spriteColor(image, copperTile.bounds(), new AssetStore(Path.of("assets")).sprite(GameData.itemIcon("copper_ingot"), 64)), "Owned material retains color even below requirement");
        require(!spriteColor(image, bronzeTile.bounds(), new AssetStore(Path.of("assets")).sprite(GameData.itemIcon("bronze_ingot"), 64)), "Unowned sprite is grayscale");
        require(tips.stream().anyMatch(t -> t.title().equals("Copper Ingot") && t.body().contains("Owned: 1 / Required: 3")
                && t.body().contains("ATK") && t.body().contains("Missing: 2")), "Hover tooltip includes stats and exact quantities");
        click(buttons, "Tier ^"); buttons.clear(); renderer.drawCrafting(g);
        require(materialTiles(buttons).get(0).label().startsWith("Adamantite"), "Descending tier sort");
        click(buttons, "Availability"); buttons.clear(); renderer.drawCrafting(g);
        require(materialTiles(buttons).get(0).label().startsWith("Iron Ingot") && materialTiles(buttons).get(1).label().startsWith("Copper Ingot"), "Availability puts sufficient then partial then missing first");
        click(buttons, "Availability v"); buttons.clear(); renderer.drawCrafting(g);
        require(!materialTiles(buttons).get(0).label().startsWith("Iron Ingot"), "Availability sort reverses");
        click(buttons, "Level"); buttons.clear(); renderer.drawCrafting(g);
        require(materialTiles(buttons).get(0).label().startsWith("Copper"), "Level ascending");
        click(buttons, "Rarity"); buttons.clear(); renderer.drawCrafting(g);
        click(buttons, "Rarity ^"); buttons.clear(); renderer.drawCrafting(g);
        require(materialTiles(buttons).get(0).label().startsWith("Adamantite"), "Rarity descending");
        click(buttons, "Tier"); buttons.clear(); renderer.drawCrafting(g);
        if (args.length > 0) ImageIO.write(image, "png", Path.of(args[0] + "-materials.png").toFile());
        state.player.consumeItem("copper_ingot");
        for (int i = 0; i < 3; i++) state.player.consumeItem("iron_ingot");
        click(buttons, "Next"); buttons.clear(); renderer.drawCrafting(g);
        require(buttons.stream().anyMatch(b -> b.label().equals("Previous")), "Material list pagination");
        click(buttons, "Previous"); buttons.clear(); renderer.drawCrafting(g);
        clickPrefix(buttons, "Tin Ingot | Owned:"); buttons.clear(); renderer.drawCrafting(g);
        require(buttons.stream().anyMatch(b -> b.label().startsWith("Tin Ingot (")), "List selection updates slot");
        int[] withoutOrnament = image.getRGB(615, 262, 90, 90, null, 0, 90);
        click(buttons, "Select Ornament material..."); buttons.clear(); renderer.drawCrafting(g);
        clickPrefix(buttons, "Bone | Owned:"); buttons.clear(); renderer.drawCrafting(g);
        require(!java.util.Arrays.equals(withoutOrnament, image.getRGB(615, 262, 90, 90, null, 0, 90)), "Optional material changes planned item colors");
        buttons.stream().filter(b -> b.label().startsWith("Bone (")).reduce((a, b) -> b).orElseThrow().action().run();
        buttons.clear(); renderer.drawCrafting(g);
        click(buttons, "Leave Ornament empty"); buttons.clear(); renderer.drawCrafting(g);
        require(java.util.Arrays.equals(withoutOrnament, image.getRGB(615, 262, 90, 90, null, 0, 90)), "Clearing optional material restores planned preview");
        if (args.length > 0) ImageIO.write(image, "png", Path.of(args[0] + "-prepare.png").toFile());
        state.config.creativeCraftingMode = true; buttons.clear(); renderer.drawCrafting(g);
        require(buttons.stream().anyMatch(b -> b.label().equals("Prepare Blade")), "Creative UI enables preparation without materials or station");
        state.config.creativeCraftingMode = false; buttons.clear(); renderer.drawCrafting(g);
        require(buttons.stream().noneMatch(b -> b.label().equals("Prepare Blade")), "Normal UI restores requirements");
        click(buttons, "Stage 1: Prepare parts >"); buttons.clear(); renderer.drawCrafting(g);
        require(buttons.stream().anyMatch(b -> b.label().contains("Masterwork Mithril Blade")), "Owned blade available");
        require(buttons.stream().anyMatch(b -> b.label().contains("Masterwork Wood Hilt")), "Owned hilt available");
        clickPrefix(buttons, "Masterwork Mithril Blade"); buttons.clear(); renderer.drawCrafting(g);
        require(buttons.stream().anyMatch(b -> b.label().startsWith("Masterwork Mithril Blade | Owned:")), "Prepared part list");
        require(buttons.stream().noneMatch(b -> b.label().contains("Hilt")), "Prepared part list filters by slot");
        clickPrefix(buttons, "Masterwork Mithril Blade | Owned:"); buttons.clear(); renderer.drawCrafting(g);
        click(buttons, "Select Ornament..."); buttons.clear(); renderer.drawCrafting(g);
        click(buttons, "Leave Ornament empty"); buttons.clear(); renderer.drawCrafting(g);
        if (args.length > 0) ImageIO.write(image, "png", Path.of(args[0] + "-assemble.png").toFile());
        state.player.addItem("copper_ingot", 3);
        state.craftComponent(AssemblyCrafting.Slot.BLADE, "copper_ingot");
        require(!state.crafting.active() && state.player.inventory.get("copper_ingot") == 3, "Wrong station consumes nothing");
        state.world.restoreProp(state.currentMapId, new WorldProp(state.playerX + 1, state.playerY, "interior_anvil", 40));
        buttons.clear(); renderer.drawCrafting(g); click(buttons, "Assemble");
        require(state.crafting.active() && state.mode == GameMode.EXPLORE, "Assembly button starts timed game task");
        for (int i = 0; i < 1000 && state.crafting.active(); i++) state.crafting.tick(state.player);
        require(state.player.inventory.keySet().stream().anyMatch(k -> k.startsWith("gear1~")
                && GameData.itemName(k).equals("Masterwork Mithril Sword")), "Workshop delivers previewed item");
        state.mode = GameMode.CRAFTING;
        buttons.clear(); renderer.drawCrafting(g);
        click(buttons, "Type: Sword >"); buttons.clear(); renderer.drawCrafting(g);
        require(buttons.stream().anyMatch(b -> b.label().equals("Helmet"))
                && buttons.stream().anyMatch(b -> b.label().equals("Shield")), "Equipment type list covers new slots");
        if (args.length > 0) ImageIO.write(image, "png", Path.of(args[0] + "-types.png").toFile());
        click(buttons, "Dagger"); buttons.clear(); renderer.drawCrafting(g);
        require(buttons.stream().anyMatch(b -> b.label().equals("Type: Dagger >")), "Blueprint switch");
        click(buttons, "Recipe Book"); buttons.clear(); renderer.drawCrafting(g);
        require(!renderer.displayedCraftingRecipes().isEmpty(), "Recipe book remains accessible");
        g.dispose();
        System.out.println("Workshop navigation and rendering passed.");
    }
    private static List<UiButton> materialTiles(List<UiButton> buttons) {
        return buttons.stream().filter(b -> b.label().contains("| Owned:")).toList();
    }
    private static boolean spriteColor(BufferedImage image, Rectangle bounds, BufferedImage source) {
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            if ((source.getRGB(x, y) >>> 24) < 250) continue;
            int pixel = image.getRGB(bounds.x + 34 + x, bounds.y + 24 + y);
            int r = pixel >> 16 & 255, g = pixel >> 8 & 255, b = pixel & 255;
            if (Math.abs(r - g) > 3 || Math.abs(g - b) > 3) return true;
        }
        return false;
    }
    private static void click(List<UiButton> buttons, String label) {
        buttons.stream().filter(b -> b.label().equals(label)).findFirst().orElseThrow(() -> new AssertionError("Missing button: " + label)).action().run();
    }
    private static void clickPrefix(List<UiButton> buttons, String prefix) {
        buttons.stream().filter(b -> b.label().startsWith(prefix)).findFirst().orElseThrow(() -> new AssertionError("Missing choice: " + prefix)).action().run();
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
