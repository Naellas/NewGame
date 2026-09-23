package com.alderfall.game.ui;

import com.alderfall.game.*;

import com.alderfall.game.inventory.Equipment;
import com.alderfall.game.inventory.Item;
import com.alderfall.game.inventory.ItemRarity;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public final class ShopRenderer {
    private final AssetStore assets;
    private final GameState state;
    private final Effects effects;
    public final ItemBrowser stockBrowser = new ItemBrowser();
    public final ItemBrowser packBrowser = new ItemBrowser();
    private final List<TradeTile> tiles = new ArrayList<>();
    private List<String> visibleStock = List.of();
    private Rectangle stockBounds = new Rectangle(), packBounds = new Rectangle();
    private TradeTile drag;
    private Point dragStart, dragPoint;
    private boolean moved;
    private record TradeTile(Rectangle bounds, String key, boolean buying) {}

    public void useVisibleItem(int index) {
        if (index >= 0 && index < visibleStock.size()) trade(visibleStock.get(index), true);
    }
    private void trade(String key, boolean buying) {
        if (state.activeShop == null) return;
        if (buying) state.buyShopItem(state.activeShop.availableStock(state.player.level).indexOf(key));
        else state.sellShopItem(new ArrayList<>(state.player.inventory.keySet()).indexOf(key));
    }
    public void press(Point point) {
        stockBrowser.focusAt(point); packBrowser.focusAt(point);
        drag = null; moved = false;
        if (point == null) return;
        for (TradeTile tile : tiles) if (tile.bounds.contains(point)) {
            drag = tile; dragStart = point; dragPoint = point; break;
        }
    }
    public void move(Point point) {
        if (drag != null && point != null) { dragPoint = point; moved |= dragStart.distance(point) > 4; }
    }
    public boolean release(Point point) {
        if (drag == null) return false;
        if (state.mode == GameMode.SHOP && moved && point != null
                && (drag.buying ? packBounds : stockBounds).contains(point)) trade(drag.key, drag.buying);
        boolean consumed = moved;
        drag = null; dragPoint = null; moved = false;
        return consumed;
    }


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

        tiles.clear();
        int columnW = (panelW - 116) / 2;
        int columnY = panelY + 140;
        int columnH = panelH - 260;
        drawGrid(g, panelX + 44, columnY, columnW, columnH, true);
        drawGrid(g, panelX + 72 + columnW, columnY, columnW, columnH, false);

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
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Drag tiles across to buy / sell one. Click a tile to trade. Wheel scrolls the hovered grid.", panelX + 44, panelY + panelH - 100);
        g.setColor(new Color(246, 224, 151));
        drawClippedString(g, state.status, panelX + 44, panelY + panelH - 22, panelW - 260);
        if (drag != null && moved && dragPoint != null) {
            g.drawImage(assets.sprite(GameData.itemIcon(drag.key), 46), dragPoint.x - 23, dragPoint.y - 23, null);
        }
        actionButton(g, panelX + panelW - 176, panelY + panelH - 54, 132, 36, "Leave Shop", state::closeOverlay, new Color(83, 61, 61), new Color(149, 96, 88), true);
    }

    private void drawGrid(Graphics2D g, int x, int y, int w, int h, boolean buying) {
        ItemBrowser browser = buying ? stockBrowser : packBrowser;
        List<String> source = buying ? state.activeShop.availableStock(state.player.level)
                : new ArrayList<>(state.player.inventory.keySet());
        List<String> items = browser.items(source);
        drawTradeColumnHeader(g, x, y - 24, w, buying ? "Shop Stock / Buy" : "Shared Pack / Sell");
        browser.draw(g, x, y + 12, w, h - 12, effects.buttons());
        int gy = y + 130, cell = 84, gap = 10, cols = Math.max(1, (w + gap) / (cell + gap));
        int rows = Math.max(1, (h - 130 + gap) / (cell + gap)), visible = cols * rows;
        Rectangle target = new Rectangle(x, gy - 4, w, rows * (cell + gap));
        if (buying) stockBounds = target; else packBounds = target;
        g.setColor(new Color(19, 24, 33)); g.fillRoundRect(target.x, target.y, target.width, target.height, 8, 8);
        if (drag != null && drag.buying != buying) {
            g.setColor(new Color(148, 203, 126)); g.drawRoundRect(target.x, target.y, target.width, target.height, 8, 8);
        }
        browser.scroll = Math.max(0, Math.min(browser.scroll, Math.max(0, items.size() - visible)));
        int end = Math.min(items.size(), browser.scroll + visible);
        if (buying) visibleStock = items.subList(browser.scroll, end);
        for (int i = browser.scroll; i < end; i++) {
            String key = items.get(i); int local = i - browser.scroll;
            Rectangle rect = new Rectangle(x + (local % cols) * (cell + gap), gy + (local / cols) * (cell + gap), cell, cell);
            int price = buying ? GameData.itemCost(key) : Math.max(1, GameData.itemCost(key) / 2);
            boolean enabled = !buying || price > 0 && state.player.gold >= price;
            g.setColor(enabled ? new Color(33, 40, 53) : new Color(37, 30, 35));
            g.fillRoundRect(rect.x, rect.y, cell, cell, 8, 8);
            g.setColor(!browser.query.isBlank() ? new Color(255, 216, 122) : rarityColorForItem(key));
            g.drawRoundRect(rect.x, rect.y, cell, cell, 8, 8);
            g.drawImage(assets.sprite(GameData.itemIcon(key), 42), rect.x + 21, rect.y + 8, null);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.setColor(new Color(227, 229, 237));
            drawClippedString(g, GameData.itemName(key), rect.x + 5, rect.y + 62, cell - 10);
            g.setColor(enabled ? new Color(246, 214, 134) : new Color(235, 126, 126));
            g.drawString((buying ? "Buy " : "Sell ") + price + "g", rect.x + 5, rect.y + 77);
            if (!buying) { g.setColor(Color.WHITE); g.drawString("x" + state.player.inventory.get(key), rect.x + 4, rect.y + 14); }
            else if (local < 9) g.drawString(Integer.toString(local + 1), rect.x + 4, rect.y + 14);
            tiles.add(new TradeTile(rect, key, buying));
            effects.buttons().add(new UiButton(rect, "trade:" + buying + ":" + key, () -> trade(key, buying)));
            Equipment gear = GameData.equipment(key);
            String detail = gear == null ? itemBenefit(GameData.ITEMS.get(key)) : equipmentBenefit(gear);
            effects.tooltipZones().add(new TooltipZone(rect, GameData.itemName(key), detail + " "
                    + (buying ? "Buy one for " : "Sell one for ") + price + "g. Drag to the opposite grid."
                    + (enabled ? "" : " Not enough gold."), GameData.itemIcon(key), rarityColorForItem(key)));
        }
        if (items.isEmpty()) { g.setFont(new Font("SansSerif", Font.PLAIN, 14)); g.setColor(Color.LIGHT_GRAY); g.drawString("No items in this category or search.", x + 12, gy + 30); }
        drawScrollIndicator(g, x + w + 4, gy, rows * (cell + gap) - gap, items.size(), browser.scroll, visible);
    }

    private void drawTradeColumnHeader(Graphics2D g, int x, int y, int w, String label) {
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(244, 213, 141));
        g.drawString(label, x + 4, y + 18);
        g.setColor(new Color(96, 88, 72, 120));
        g.drawLine(x, y + 26, x + w, y + 26);
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
        AssemblyCrafting.Component part = AssemblyCrafting.component(itemKey);
        MaterialCatalog.Material material = part == null ? MaterialCatalog.get(itemKey) : part.material();
        if (material != null) {
            ItemRarity rarity = material.rarity();
            if (part != null && part.quality().rarity.ordinal() > rarity.ordinal()) rarity = part.quality().rarity;
            return rarityColor(rarity);
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
        List<UiButton> buttons();
        List<TooltipZone> tooltipZones();
        int shopItemScroll();

        void setShopItemScroll(int value);

        int gameAreaCenteredX(int width);

        public void drawOverlayBase(Graphics2D g, int x, int y, int w, int h);

        void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled);

        public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth);

        public void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows);
    }
}
