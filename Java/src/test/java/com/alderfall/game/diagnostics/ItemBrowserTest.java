package com.alderfall.game;

import com.alderfall.game.ui.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Exercises filtered identity, text input and real shop transactions without a window. */
public final class ItemBrowserTest {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        ItemBrowser browser = new ItemBrowser();
        List<String> keys = List.of("iron_sword", "iron_ingot", "health_potion");
        require(ItemBrowser.categoryOf("iron_sword") == ItemBrowser.Category.WEAPONS, "Weapon classification");
        require(ItemBrowser.categoryOf("iron_ingot") == ItemBrowser.Category.MATERIALS, "Material classification");
        require(browser.items(keys).equals(keys), "All preserves collection order");
        browser.category = ItemBrowser.Category.ARMOR; browser.query = "IRON INGOT";
        require(browser.items(keys).equals(List.of("iron_ingot")), "Search finds result across tabs");
        require(browser.category == ItemBrowser.Category.MATERIALS, "Search switches active tab");
        browser.query = "no such item";
        require(browser.items(keys).isEmpty(), "Empty results");
        browser.focused = true; browser.query = ""; browser.typed('i'); browser.typed('1');
        require(browser.query.equals("i1"), "Text accepts hotkey characters");
        require(browser.keyPressed(new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE, (char)27)), "Escape consumed");
        require(!browser.focused, "Escape releases search focus");

        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        state.chooseClass("Knight"); state.mode = GameMode.SHOP;
        state.activeShop = GameData.SHOPS.values().iterator().next();
        state.player.gold = 100000;
        List<UiButton> buttons = new ArrayList<>();
        List<TooltipZone> tips = new ArrayList<>();
        ShopRenderer.Effects effects = (ShopRenderer.Effects) Proxy.newProxyInstance(ShopRenderer.Effects.class.getClassLoader(),
                new Class<?>[]{ShopRenderer.Effects.class}, (proxy, method, params) -> switch(method.getName()) {
                    case "buttons" -> buttons;
                    case "tooltipZones" -> tips;
                    case "gameAreaCenteredX" -> 20;
                    case "shopItemScroll" -> 0;
                    case "drawOverlayBase" -> {
                        Graphics2D graphics = (Graphics2D) params[0]; graphics.setColor(new Color(17, 20, 29));
                        graphics.fillRoundRect((int)params[1], (int)params[2], (int)params[3], (int)params[4], 16, 16); yield null;
                    }
                    case "drawClippedString" -> {
                        Graphics2D graphics = (Graphics2D) params[0]; String text = (String) params[1];
                        while (graphics.getFontMetrics().stringWidth(text) > (int)params[4] && !text.isEmpty()) text = text.substring(0, text.length()-1);
                        graphics.drawString(text, (int)params[2], (int)params[3]); yield null;
                    }
                    default -> null;
                });
        ShopRenderer renderer = new ShopRenderer(new AssetStore(Path.of("assets")), state, effects);
        BufferedImage image = new BufferedImage(1120, 810, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        renderer.drawShop(g);
        UiButton tile = buttons.stream().filter(b -> b.label().startsWith("trade:true:")).findFirst().orElseThrow();
        String key = tile.label().substring("trade:true:".length());
        int gold = state.player.gold, count = state.player.inventory.getOrDefault(key, 0);
        Point start = new Point(tile.bounds().x + 40, tile.bounds().y + 40);
        renderer.press(start); renderer.move(new Point(600, 350));
        require(renderer.release(new Point(600, 350)), "Drag consumes click");
        require(state.player.gold == gold - GameData.itemCost(key), "Drag buys exactly one");
        require(state.player.inventory.getOrDefault(key, 0) == count + 1, "Purchased identity preserved");
        buttons.clear(); tips.clear(); renderer.packBrowser.query = GameData.itemName(key); renderer.drawShop(g);
        UiButton sell = buttons.stream().filter(b -> b.label().equals("trade:false:" + key)).findFirst().orElseThrow();
        start = new Point(sell.bounds().x + 40, sell.bounds().y + 40);
        renderer.press(start); renderer.move(new Point(100, 350)); renderer.release(new Point(100, 350));
        require(state.player.inventory.getOrDefault(key, 0) == count, "Filtered drag sells correct item");
        gold = state.player.gold;
        renderer.press(new Point(tile.bounds().x + 40, tile.bounds().y + 40)); renderer.move(new Point(5, 5)); renderer.release(new Point(5, 5));
        require(state.player.gold == gold, "Outside drop cancels transaction");
        state.player.gold = 0; renderer.useVisibleItem(0);
        require(state.player.gold == 0, "Unaffordable purchase rejected");
        state.player.gold = 250;
        buttons.clear(); tips.clear(); renderer.drawShop(g); g.dispose();
        if (args.length > 0) ImageIO.write(image, "png", Path.of(args[0]).toFile());
        System.out.println("Item browser and shop transaction checks passed.");
    }
}
