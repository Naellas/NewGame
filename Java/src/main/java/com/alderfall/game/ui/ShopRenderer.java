package com.alderfall.game.ui;

import com.alderfall.game.*;

import com.alderfall.game.inventory.Equipment;
import com.alderfall.game.inventory.Item;
import com.alderfall.game.inventory.ItemRarity;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public final class ShopRenderer {
    private final AssetStore assets;
    private final GameState state;
    private final Effects effects;

    public ShopRenderer(AssetStore assets, GameState state, Effects effects) {
        this.assets = assets;
        this.state = state;
        this.effects = effects;
    }

    private int shopItemScroll() {
        return effects.shopItemScroll();
    }

    private void setShopItemScroll(int value) {
        effects.setShopItemScroll(value);
    }

    private int gameAreaCenteredX(int width) {
        return effects.gameAreaCenteredX(width);
    }

    private void drawOverlayBase(Graphics2D g, int x, int y, int w, int h) {
        effects.drawOverlayBase(g, x, y, w, h);
    }

    private void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled) {
        effects.actionButton(g, x, y, w, h, label, action, fill, border, enabled);
    }

    private void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth) {
        effects.drawClippedString(g, text, x, y, maxWidth);
    }

    private void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows) {
        effects.drawScrollIndicator(g, x, y, h, totalRows, scroll, visibleRows);
    }

    public void drawShop(Graphics2D g) {
        Shop shop = state.activeShop;
        if (shop == null) {
            return;
        }
        int panelW = 1060;
        int panelX = gameAreaCenteredX(panelW);
        int panelY = 72;
        int panelH = 704;
        drawOverlayBase(g, panelX, panelY, panelW, panelH);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.setColor(new Color(244, 239, 220));
        g.drawString(shop.name(), panelX + 44, panelY + 54);
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        g.setColor(new Color(210, 213, 222));
        g.drawString("Gold: " + state.player.gold, panelX + 44, panelY + 88);
        g.setColor(new Color(96, 88, 72, 120));
        g.drawLine(panelX + 44, panelY + 122, panelX + panelW - 44, panelY + 122);

        List<String> stock = shop.availableStock(state.player.level);
        List<String> inventory = new ArrayList<>(state.player.inventory.keySet());
        int columnY = panelY + 158;
        int columnH = panelH - 230;
        int gap = 28;
        int columnW = (panelW - 88 - gap) / 2;
        int shopX = panelX + 44;
        int inventoryX = shopX + columnW + gap;
        int rowH = 58;
        int visibleRows = Math.max(1, columnH / rowH);
        int shopMaxScroll = Math.max(0, stock.size() - visibleRows);
        setShopItemScroll(Math.max(0, Math.min(shopItemScroll(), shopMaxScroll)));

        drawTradeColumnHeader(g, shopX, columnY - 28, columnW, "Shop Stock");
        int shopEnd = Math.min(stock.size(), shopItemScroll() + visibleRows);
        for (int i = shopItemScroll(); i < shopEnd; i++) {
            int buttonIndex = i;
            String itemKey = stock.get(i);
            Item item = GameData.ITEMS.get(itemKey);
            Equipment equipment = GameData.equipment(itemKey);
            int cost = GameData.itemCost(itemKey);
            if (cost <= 0) {
                continue;
            }
            boolean affordable = state.player.gold >= cost;
            int y = columnY + (i - shopItemScroll()) * rowH;
            drawTradeRow(g, shopX, y, columnW, rowH - 8, itemKey,
                    equipment == null ? itemBenefit(item) : equipmentBenefit(equipment),
                    "Buy " + cost + "g", () -> state.buyShopItem(buttonIndex), affordable);
        }
        drawScrollIndicator(g, shopX + columnW + 4, columnY, visibleRows * rowH - 8, stock.size(), shopItemScroll(), visibleRows);

        drawTradeColumnHeader(g, inventoryX, columnY - 28, columnW, "Your Inventory");
        int invEnd = Math.min(inventory.size(), shopItemScroll() + visibleRows);
        for (int i = shopItemScroll(); i < invEnd; i++) {
            int buttonIndex = i;
            String itemKey = inventory.get(i);
            Item item = GameData.ITEMS.get(itemKey);
            Equipment equipment = GameData.equipment(itemKey);
            int value = Math.max(1, GameData.itemCost(itemKey) / 2);
            int count = state.player.inventory.getOrDefault(itemKey, 0);
            int y = columnY + (i - shopItemScroll()) * rowH;
            drawTradeRow(g, inventoryX, y, columnW, rowH - 8, itemKey,
                    "x" + count + "  " + (equipment == null ? itemBenefit(item) : equipmentBenefit(equipment)),
                    "Sell " + value + "g", () -> state.sellShopItem(buttonIndex), count > 0);
        }
        drawScrollIndicator(g, inventoryX + columnW + 4, columnY, visibleRows * rowH - 8, inventory.size(), shopItemScroll(), visibleRows);

        if (state.activeNpc != null && state.activeNpc.recruitId() != null && state.activeNpc.recruitCost() > 0) {
            if (state.isRecruited(state.activeNpc.recruitId())) {
                g.setColor(new Color(144, 215, 150));
                g.setFont(new Font("SansSerif", Font.BOLD, 15));
                g.drawString(state.npcDisplayName(state.activeNpc) + " travels with you.", panelX + 44, panelY + panelH - 72);
            } else {
                actionButton(g, panelX + 44, panelY + panelH - 88, 258, 34,
                        "Hire " + state.npcDisplayName(state.activeNpc) + " - " + state.activeNpc.recruitCost() + "g",
                        state::hireActiveRecruit, new Color(69, 62, 88), new Color(125, 107, 166), true);
            }
        }
        g.setColor(new Color(196, 198, 205));
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Move goods between shop stock and your inventory with Buy/Sell. Wheel scrolls both columns.", panelX + 44, panelY + panelH - 32);
        actionButton(g, panelX + panelW - 176, panelY + panelH - 54, 132, 36, "Leave Shop", state::closeOverlay, new Color(83, 61, 61), new Color(149, 96, 88), true);
    }

    private void drawTradeColumnHeader(Graphics2D g, int x, int y, int w, String label) {
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(244, 213, 141));
        g.drawString(label, x + 4, y + 18);
        g.setColor(new Color(96, 88, 72, 120));
        g.drawLine(x, y + 26, x + w, y + 26);
    }

    private void drawTradeRow(Graphics2D g, int x, int y, int w, int h, String itemKey, String detail,
                              String action, Runnable runnable, boolean enabled) {
        drawShopRowBackground(g, x, y, w, h, enabled);
        Color rarity = rarityColorForItem(itemKey);
        g.setColor(new Color(238, 231, 207));
        g.fillRoundRect(x + 10, y + 7, 36, 36, 6, 6);
        g.setColor(rarity);
        g.drawRoundRect(x + 10, y + 7, 36, 36, 6, 6);
        g.drawImage(assets.sprite(GameData.itemIcon(itemKey), 30), x + 13, y + 10, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(rarity);
        drawClippedString(g, GameData.itemName(itemKey), x + 56, y + 19, w - 180);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(176, 182, 196));
        drawClippedString(g, detail, x + 56, y + 38, w - 180);
        actionButton(g, x + w - 112, y + 11, 94, 28, action, runnable,
                enabled ? new Color(68, 90, 53) : new Color(58, 59, 66), new Color(110, 139, 92), enabled);
    }

    private void drawShopRowBackground(Graphics2D g, int x, int y, int w, int h, boolean enabled) {
        Color top = enabled ? new Color(31, 36, 48, 236) : new Color(30, 31, 38, 182);
        Color bottom = enabled ? new Color(21, 25, 35, 236) : new Color(22, 23, 29, 182);
        g.setPaint(new GradientPaint(x, y, top, x + w, y + h, bottom));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(enabled ? new Color(93, 103, 130, 150) : new Color(72, 70, 80, 120));
        g.drawRoundRect(x, y, w, h, 10, 10);
        g.setColor(new Color(255, 255, 255, enabled ? 18 : 8));
        g.drawLine(x + 14, y + 1, x + w - 14, y + 1);
    }

    private String itemBenefit(Item item) {
        if (item == null) {
            return "Field provision";
        }
        if ("escape_scroll".equals(item.key())) {
            return item.rarityLine() + "   Escapes a dungeon instantly";
        }
        String effect = item.effectDescription() == null || item.effectDescription().isBlank()
                ? ""
                : " " + item.effectDescription();
        if (item.heal() > 0 && item.mp() > 0) {
            return item.rarityLine() + "   Restores " + item.heal() + " HP and " + item.mp() + " MP." + effect;
        }
        if (item.heal() > 0) {
            return item.rarityLine() + "   Restores " + item.heal() + " HP." + effect;
        }
        if (item.mp() > 0) {
            return item.rarityLine() + "   Restores " + item.mp() + " MP." + effect;
        }
        return item.rarityLine() + "   Field provision." + effect;
    }

    public static Color rarityColorForItem(String itemKey) {
        Equipment equipment = GameData.equipment(itemKey);
        if (equipment != null) {
            return rarityColor(equipment.rarity());
        }
        Item item = GameData.ITEMS.get(itemKey);
        return item == null ? new Color(235, 236, 240) : rarityColor(item.rarity());
    }

    public static Color rarityColor(ItemRarity rarity) {
        if (rarity == null) {
            return new Color(235, 236, 240);
        }
        return switch (rarity) {
            case COMMON -> new Color(238, 239, 244);
            case UNCOMMON -> new Color(109, 205, 122);
            case RARE -> new Color(94, 154, 255);
            case UNIQUE -> new Color(255, 165, 70);
            case LEGENDARY -> new Color(245, 86, 86);
        };
    }

    private String equipmentBenefit(Equipment equipment) {
        String stats = String.join("  ", equipment.statLines());
        String level = equipment.rarityLine() + "   " + equipment.levelRangeLine();
        return stats.isBlank()
                ? level + "   " + equipment.description()
                : level + "   " + equipment.slot() + "   " + stats;
    }


    public interface Effects {
        int shopItemScroll();

        void setShopItemScroll(int value);

        int gameAreaCenteredX(int width);

        public void drawOverlayBase(Graphics2D g, int x, int y, int w, int h);

        void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled);

        public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth);

        public void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows);
    }
}
