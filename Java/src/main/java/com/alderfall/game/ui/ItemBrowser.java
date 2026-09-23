package com.alderfall.game.ui;

import com.alderfall.game.*;
import com.alderfall.game.inventory.Equipment;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;

/** Shared category, search and paging state for a single item collection. */
public final class ItemBrowser {
    public enum Category {
        ALL("All"), MATERIALS("Materials"), WEAPONS("Weapons"), ARMOR("Armor"),
        CONSUMABLES("Consumables"), ACCESSORIES("Accessories"), MISC("Misc");
        final String label;
        Category(String label) { this.label = label; }
    }
    public Category category = Category.ALL;
    public String query = "";
    public boolean focused;
    public int scroll;
    private Rectangle search = new Rectangle();
    private Rectangle bounds = new Rectangle();

    public static Category categoryOf(String key) {
        Equipment gear = GameData.equipment(key);
        if (gear != null) return switch (gear.slot()) {
            case "weapon" -> Category.WEAPONS;
            case "ring", "necklace" -> Category.ACCESSORIES;
            default -> Category.ARMOR;
        };
        if (CraftingSystem.isRecipeBookItem(key)) return Category.MISC;
        if (GameData.ITEMS.containsKey(key)) return Category.CONSUMABLES;
        if (CraftingSystem.isCraftingOnlyItem(key)) {
            String icon = GameData.itemIcon(key);
            if (icon.startsWith("material_") || icon.startsWith("ingredient_")) return Category.MATERIALS;
            return Category.MISC;
        }
        return Category.MISC;
    }
    public boolean matches(String key) {
        return GameData.itemName(key).toLowerCase(Locale.ROOT).contains(query.strip().toLowerCase(Locale.ROOT));
    }
    public List<String> items(List<String> source) {
        List<String> matches = source.stream().filter(this::matches).toList();
        if (!query.isBlank() && category != Category.ALL && !matches.isEmpty()
                && matches.stream().noneMatch(k -> categoryOf(k) == category)) {
            category = categoryOf(matches.get(0));
            scroll = 0;
        }
        return matches.stream().filter(k -> category == Category.ALL || categoryOf(k) == category).toList();
    }
    public void focusAt(Point point) { focused = point != null && search.contains(point); }
    public boolean keyPressed(KeyEvent event) {
        if (!focused) return false;
        if (event.getKeyCode() == KeyEvent.VK_ESCAPE || event.getKeyCode() == KeyEvent.VK_ENTER
                || event.getKeyCode() == KeyEvent.VK_TAB) focused = false;
        if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE && !query.isEmpty()) {
            query = event.isControlDown() ? "" : query.substring(0, query.length() - 1); scroll = 0;
        }
        return true;
    }
    public void typed(char c) {
        if (focused && !Character.isISOControl(c) && c != KeyEvent.CHAR_UNDEFINED && query.length() < 64) {
            query += c; scroll = 0;
        }
    }
    public void wheel(Point point, int amount) {
        if (point != null && bounds.contains(point)) scroll = Math.max(0, scroll + amount * 5);
    }
    public void draw(Graphics2D g, int x, int y, int w, int h, List<UiButton> buttons) {
        bounds = new Rectangle(x, y, w, h);
        search = new Rectangle(x, y, w - 32, 30);
        g.setColor(new Color(14, 19, 28)); g.fillRoundRect(x, y, w, 30, 8, 8);
        g.setColor(focused ? new Color(246, 214, 134) : new Color(95, 108, 137));
        g.drawRoundRect(x, y, w, 30, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        String text = query.isEmpty() ? "Search items..." : query + (focused ? "|" : "");
        while (g.getFontMetrics().stringWidth(text) > w - 48) text = text.substring(1);
        g.drawString(text, x + 10, y + 20);
        g.drawString("x", x + w - 20, y + 20);
        buttons.add(new UiButton(new Rectangle(x + w - 30, y, 30, 30), "Clear search", () -> { query = ""; scroll = 0; focused = true; }));
        int tabW = (w - 18) / 4;
        for (Category tab : Category.values()) {
            int i = tab.ordinal(), tx = x + (i % 4) * (tabW + 6), ty = y + 38 + (i / 4) * 36;
            boolean active = category == tab;
            g.setColor(active ? new Color(72, 66, 43) : new Color(29, 35, 47));
            g.fillRoundRect(tx, ty, tabW, 30, 6, 6);
            g.setColor(active ? new Color(255, 216, 122) : new Color(144, 158, 183));
            g.drawRoundRect(tx, ty, tabW, 30, 6, 6);
            if (active) g.fillRect(tx + 6, ty + 27, tabW - 12, 3);
            drawIcon(g, tab, tx + 7, ty + 7);
            g.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 10));
            g.drawString(tab.label, tx + 28, ty + 19);
            buttons.add(new UiButton(new Rectangle(tx, ty, tabW, 30), "Category: " + tab.label,
                    () -> { category = tab; scroll = 0; focused = false; }));
        }
    }
    private static void drawIcon(Graphics2D g, Category tab, int x, int y) {
        switch (tab) {
            case ALL -> { for (int i=0;i<4;i++) g.drawRect(x+(i%2)*9,y+(i/2)*9,6,6); }
            case MATERIALS -> { g.drawPolygon(new int[]{x,x+5,x+15,x+18,x+12,x+3},new int[]{y+11,y+3,y+3,y+11,y+16,y+16},6); g.drawLine(x,y+11,x+18,y+11); }
            case WEAPONS -> { g.drawLine(x+2,y+16,x+16,y+2); g.drawLine(x+1,y+10,x+8,y+17); g.drawLine(x+12,y+2,x+16,y+2); g.drawLine(x+16,y+2,x+16,y+6); }
            case ARMOR -> g.drawPolygon(new int[]{x,x+8,x+16,x+14,x+8,x+2},new int[]{y+2,y,y+2,y+11,y+17,y+11},6);
            case CONSUMABLES -> { g.drawRect(x+6,y,6,5); g.drawOval(x+2,y+5,14,12); g.drawLine(x+4,y+12,x+14,y+12); }
            case ACCESSORIES -> { g.drawOval(x+2,y+5,14,12); g.drawPolygon(new int[]{x+5,x+9,x+13,x+9},new int[]{y+3,y,y+3,y+7},4); }
            case MISC -> { g.drawRoundRect(x+1,y+5,16,12,4,4); g.drawArc(x+5,y,8,10,0,180); }
        }
    }
}
