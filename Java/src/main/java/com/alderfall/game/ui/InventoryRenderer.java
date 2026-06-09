package com.alderfall.game.ui;

import com.alderfall.game.*;

import com.alderfall.game.inventory.Equipment;
import com.alderfall.game.inventory.InventoryDrag;
import com.alderfall.game.inventory.InventoryDragZone;
import com.alderfall.game.inventory.InventoryDropKind;
import com.alderfall.game.inventory.InventoryDropZone;
import com.alderfall.game.inventory.Item;
import com.alderfall.game.inventory.ItemRarity;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class InventoryRenderer {
    private final AssetStore assets;
    private final GameState state;
    private final Effects effects;

    public InventoryRenderer(AssetStore assets, GameState state, Effects effects) {
        this.assets = assets;
        this.state = state;
        this.effects = effects;
    }


    private static String slotLabel(String slot) {
        return switch (slot) {
            case "chestpiece" -> "CHEST";
            case "pauldrons" -> "SHOULDERS";
            case "leggings" -> "LEGS";
            default -> slot.toUpperCase();
        };
    }

    private static String shortText(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    public void drawInventory(Graphics2D g) {
        int panelW = Math.min(1180, Math.max(1030, effects.gameAreaWidth() - 72));
        int panelX = effects.gameAreaCenteredX(panelW);
        int panelY = 44;
        int panelH = Math.min(790, effects.viewHeight() - 88);
        Actor actor = state.partyScreenActor();
        effects.inventoryDragZones().clear();
        effects.inventoryDropZones().clear();
        effects.drawOverlayBase(g, panelX, panelY, panelW, panelH);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Inventory", panelX + 36, panelY + 48);
        int selectorX = panelX + 36;
        int selectorY = panelY + 64;
        int selectorW = panelW - 72;
        int selectorH = inventoryPartySelectorHeight();
        drawInventoryPartySelector(g, selectorX, selectorY, selectorW);

        int modelX = panelX + 36;
        int modelY = selectorY + selectorH + 20;
        int modelW = 430;
        int contentBottom = panelY + panelH - 106;
        int contentH = Math.max(320, contentBottom - modelY);
        drawInventoryCharacter(g, actor, modelX, modelY, modelW, contentH);

        int packX = modelX + modelW + 30;
        int packY = modelY;
        int packW = panelX + panelW - 40 - packX;
        int packH = contentH;
        drawInventoryPackGrid(g, packX, packY, packW, packH);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(196, 198, 205));
        g.drawString("Click, drag, or use number keys for visible pack items. Wheel scrolls the grid.", panelX + 36, panelY + panelH - 42);
        g.setColor(new Color(246, 224, 151));
        g.drawString(shortText(state.status, 112), panelX + 36, panelY + panelH - 22);
        effects.actionButton(g, panelX + panelW - 164, panelY + panelH - 62, 124, 34, "Close", state::toggleInventory, new Color(83, 61, 61), new Color(149, 96, 88), true);
        drawInventoryDragGhost(g);
    }

    private void drawInventoryPackGrid(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(20, 23, 31, 220));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(82, 92, 116));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Shared Pack", x + 18, y + 28);

        int gap = 10;
        int cell = 76;
        int gridX = x + 18;
        int gridY = y + 48;
        int gridW = w - 36;
        int gridH = h - 72;
        int cols = Math.max(4, Math.max(1, (gridW + gap) / (cell + gap)));
        int rows = Math.max(3, Math.max(1, (gridH + gap) / (cell + gap)));
        int visible = Math.max(1, cols * rows);
        Rectangle packDrop = new Rectangle(x + 10, y + 42, w - 20, h - 54);
        effects.inventoryDropZones().add(new InventoryDropZone(packDrop, InventoryDropKind.PACK, null));
        drawInventoryDropHint(g, packDrop, "Drop gear here to unequip");

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(state.player.inventory.entrySet());
        int maxScroll = Math.max(0, entries.size() - visible);
        effects.setInventoryItemScroll(Math.max(0, Math.min(effects.inventoryItemScroll(), maxScroll)));
        int end = Math.min(entries.size(), effects.inventoryItemScroll() + visible);
        for (int i = effects.inventoryItemScroll(); i < end; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            int local = i - effects.inventoryItemScroll();
            int col = local % cols;
            int row = local / cols;
            int cellX = gridX + col * (cell + gap);
            int cellY = gridY + row * (cell + gap);
            Rectangle bounds = new Rectangle(cellX, cellY, cell, cell);
            int buttonIndex = i;
            effects.inventoryDragZones().add(new InventoryDragZone(bounds, entry.getKey(), null));
            effects.buttons().add(new UiButton(bounds, "inventory-item:" + entry.getKey(), () -> {
                state.useInventoryItem(buttonIndex);
                effects.repaintPanel();
            }));
            drawInventoryPackCell(g, bounds, entry.getKey(), entry.getValue(), local);
        }
        if (entries.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.setColor(new Color(210, 213, 222));
            g.drawString("Your pack is empty.", gridX, gridY + 32);
        }
        effects.drawScrollIndicator(g, x + w - 12, gridY, Math.min(gridH, rows * (cell + gap) - gap), entries.size(), effects.inventoryItemScroll(), visible);
    }

    private String professionSummary(Actor actor) {
        List<String> parts = new ArrayList<>();
        for (Profession profession : Profession.ALL) {
            parts.add(profession.label() + " " + actor.professionLevel(profession.id()));
        }
        return String.join(" | ", parts);
    }

    private void drawInventoryPackCell(Graphics2D g, Rectangle bounds, String itemKey, int count, int visibleIndex) {
        Equipment equipment = GameData.equipment(itemKey);
        Item item = GameData.ITEMS.get(itemKey);
        boolean craftingOnly = CraftingSystem.isCraftingOnlyItem(itemKey);
        boolean hovered = effects.hoverPoint() != null && bounds.contains(effects.hoverPoint());
        boolean draggedOver = inventoryDragOver(bounds);
        Color border = equipment != null
                ? ShopRenderer.rarityColor(equipment.rarity())
                : item != null ? new Color(103, 151, 117) : new Color(125, 136, 172);
        if (item != null) {
            border = ShopRenderer.rarityColor(item.rarity());
        }
        if (craftingOnly) {
            border = new Color(161, 124, 83);
        }
        if (hovered || draggedOver) {
            border = new Color(151, 177, 112);
        }
        g.setColor(hovered ? new Color(38, 44, 58, 245) : new Color(28, 32, 43, 235));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.setColor(border);
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.drawImage(assets.sprite(GameData.itemIcon(itemKey), 46), bounds.x + 15, bounds.y + 11, null);

        String quantity = "x" + count;
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics metrics = g.getFontMetrics();
        int badgeW = Math.max(28, metrics.stringWidth(quantity) + 12);
        int badgeX = bounds.x + bounds.width - badgeW - 7;
        int badgeY = bounds.y + bounds.height - 24;
        g.setColor(new Color(8, 11, 18, 220));
        g.fillRoundRect(badgeX, badgeY, badgeW, 18, 8, 8);
        g.setColor(new Color(238, 239, 244));
        g.drawString(quantity, badgeX + (badgeW - metrics.stringWidth(quantity)) / 2, badgeY + 13);

        if (visibleIndex < 9) {
            String key = Integer.toString(visibleIndex + 1);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(176, 182, 196));
            g.drawString(key, bounds.x + 8, bounds.y + 14);
        }
        effects.tooltipZones().add(new TooltipZone(bounds, GameData.itemName(itemKey), inventoryTooltip(itemKey, count),
                null, ShopRenderer.rarityColorForItem(itemKey)));
    }

    public List<CraftingSystem.Recipe> displayedCraftingRecipes() {
        CraftingSystem.Workstation workstation = state.currentWorkstation();
        List<CraftingSystem.Recipe> recipes = state.learnedCraftingRecipes();
        if (workstation != null) {
            recipes = recipes.stream()
                    .filter(recipe -> recipe.workstation() == workstation)
                    .toList();
        }
        CraftingSystem.RecipeCategory[] categories = CraftingSystem.RecipeCategory.values();
        if (effects.craftingRecipeCategoryIndex() <= 0 || effects.craftingRecipeCategoryIndex() > categories.length) {
            return recipes;
        }
        CraftingSystem.RecipeCategory selected = categories[effects.craftingRecipeCategoryIndex() - 1];
        return recipes.stream()
                .filter(recipe -> recipe.category() == selected)
                .toList();
    }

    public int craftingRecipeVisibleSlots() {
        int panelW = 940;
        int panelH = 620;
        int gridW = panelW - 72;
        int gridH = panelH - 228;
        int gap = 10;
        int cell = 76;
        int cols = Math.max(4, Math.max(1, (gridW - 24 + gap) / (cell + gap)));
        int rows = Math.max(3, Math.max(1, (gridH - 34 + gap) / (cell + gap)));
        return Math.max(1, cols * rows);
    }

    public void drawCrafting(Graphics2D g) {
        int panelW = 940;
        int panelX = effects.gameAreaCenteredX(panelW);
        int panelY = 86;
        int panelH = 620;
        effects.drawOverlayBase(g, panelX, panelY, panelW, panelH);
        CraftingSystem.Workstation workstation = state.currentWorkstation();
        CraftingSystem.RecipeCategory[] categories = CraftingSystem.RecipeCategory.values();
        if (effects.craftingRecipeCategoryIndex() < 0 || effects.craftingRecipeCategoryIndex() > categories.length) {
            effects.setCraftingRecipeCategoryIndex(0);
        }
        List<CraftingSystem.Recipe> recipes = displayedCraftingRecipes();

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Recipe Book", panelX + 36, panelY + 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString(workstation == null ? "Browsing learned recipes" : "Browsing learned recipes near " + workstation.label(), panelX + 36, panelY + 76);

        int tabX = panelX + 36;
        int tabY = panelY + 92;
        int tabH = 28;
        int tabGap = 6;
        int allW = 54;
        effects.actionButton(g, tabX, tabY, allW, tabH, "All", () -> {
                    effects.setCraftingRecipeCategoryIndex(0);
                    effects.setCraftingRecipeScroll(0);
                },
                effects.craftingRecipeCategoryIndex() == 0 ? new Color(73, 83, 111) : new Color(35, 39, 54),
                effects.craftingRecipeCategoryIndex() == 0 ? new Color(145, 166, 214) : new Color(86, 98, 128), true);
        tabX += allW + tabGap;
        for (int i = 0; i < categories.length; i++) {
            CraftingSystem.RecipeCategory category = categories[i];
            int index = i + 1;
            int tabW = Math.max(76, Math.min(118, g.getFontMetrics().stringWidth(category.label()) + 22));
            effects.actionButton(g, tabX, tabY, tabW, tabH, category.label(), () -> {
                        effects.setCraftingRecipeCategoryIndex(index);
                        effects.setCraftingRecipeScroll(0);
                    },
                    effects.craftingRecipeCategoryIndex() == index ? new Color(73, 83, 111) : new Color(35, 39, 54),
                    effects.craftingRecipeCategoryIndex() == index ? new Color(145, 166, 214) : new Color(86, 98, 128), true);
            tabX += tabW + tabGap;
        }

        int gridX = panelX + 36;
        int gridY = panelY + 146;
        int gridW = panelW - 72;
        int gridH = panelH - 228;
        g.setColor(new Color(20, 23, 31, 220));
        g.fillRoundRect(gridX, gridY, gridW, gridH, 8, 8);
        g.setColor(new Color(82, 92, 116));
        g.drawRoundRect(gridX, gridY, gridW, gridH, 8, 8);

        int gap = 10;
        int cell = 76;
        int cols = Math.max(4, Math.max(1, (gridW - 24 + gap) / (cell + gap)));
        int rows = Math.max(3, Math.max(1, (gridH - 34 + gap) / (cell + gap)));
        int visibleSlots = craftingRecipeVisibleSlots();
        if (recipes.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.setColor(new Color(210, 213, 222));
            g.drawString("No learned recipes in this tab.", gridX + 18, gridY + 38);
        }
        int maxScroll = Math.max(0, recipes.size() - visibleSlots);
        effects.setCraftingRecipeScroll(Math.min(effects.craftingRecipeScroll(), maxScroll));
        int end = Math.min(recipes.size(), effects.craftingRecipeScroll() + visibleSlots);
        for (int i = effects.craftingRecipeScroll(); i < end; i++) {
            CraftingSystem.Recipe recipe = recipes.get(i);
            int local = i - effects.craftingRecipeScroll();
            int col = local % cols;
            int row = local / cols;
            Rectangle bounds = new Rectangle(gridX + 18 + col * (cell + gap), gridY + 28 + row * (cell + gap), cell, cell);
            drawRecipeBookSlot(g, bounds, recipe, local);
        }
        effects.drawScrollIndicator(g, panelX + panelW - 48, gridY + 28, Math.min(gridH - 44, rows * (cell + gap) - gap),
                recipes.size(), effects.craftingRecipeScroll(), visibleSlots);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(196, 198, 205));
        String recipeHint = recipes.size() > visibleSlots
                ? "Wheel scrolls the recipe book. Lit slots can be crafted at your current station."
                : "Lit slots can be crafted at your current station. New recipes come from NPCs, books, shops, and ingredients.";
        g.drawString(recipeHint, panelX + 36, panelY + panelH - 42);
        g.setColor(new Color(246, 224, 151));
        g.drawString(shortText(state.status, 92), panelX + 36, panelY + panelH - 22);
        effects.actionButton(g, panelX + panelW - 164, panelY + panelH - 62, 124, 34, "Close", state::toggleCrafting, new Color(83, 61, 61), new Color(149, 96, 88), true);
    }

    private void drawRecipeBookSlot(Graphics2D g, Rectangle bounds, CraftingSystem.Recipe recipe, int visibleIndex) {
        boolean hovered = effects.hoverPoint() != null && bounds.contains(effects.hoverPoint());
        boolean stationReady = recipe.workstation() == null || recipe.workstation() == state.currentWorkstation();
        boolean craftable = stationReady && CraftingSystem.canCraft(state.player, recipe) && !state.crafting.active();
        Color border = switch (recipe.category()) {
            case CONSUMABLE -> new Color(103, 151, 117);
            case WEAPON -> new Color(170, 120, 92);
            case ARMOR -> new Color(125, 136, 172);
            case TOOL -> new Color(161, 124, 83);
            case SEED -> new Color(139, 166, 92);
            case ACCESSORY -> new Color(152, 126, 184);
            case MATERIAL -> new Color(151, 142, 116);
            case DECOR -> new Color(116, 151, 164);
        };
        if (craftable) {
            border = new Color(151, 177, 112);
        } else if (!stationReady) {
            border = new Color(86, 98, 128);
        }
        if (hovered) {
            border = effects.blend(border, Color.WHITE, 0.22);
        }
        g.setColor(hovered ? new Color(38, 44, 58, 245) : new Color(28, 32, 43, 235));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.setColor(border);
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.drawImage(assets.sprite(GameData.itemIcon(recipe.resultKey()), 42), bounds.x + 17, bounds.y + 9, null);

        if (craftable) {
            effects.buttons().add(new UiButton(bounds, "recipe:" + recipe.key(), () -> {
                state.craftRecipe(recipe.key());
                effects.repaintPanel();
            }));
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(202, 230, 176));
            effects.drawCenteredIn(g, "Ready", bounds.x, bounds.y + bounds.height - 7, bounds.width);
        } else {
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(150, 156, 170));
            effects.drawCenteredIn(g, stationReady ? "Needs" : recipeStationLabel(recipe), bounds.x + 3, bounds.y + bounds.height - 7, bounds.width - 6);
        }
        if (visibleIndex < 9) {
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(176, 182, 196));
            g.drawString(Integer.toString(visibleIndex + 1), bounds.x + 8, bounds.y + 14);
        }
        effects.tooltipZones().add(new TooltipZone(bounds, recipe.name(), recipeTooltip(recipe), GameData.itemIcon(recipe.resultKey()), border));
    }

    private String recipeTooltip(CraftingSystem.Recipe recipe) {
        String output = recipe.resultAmount() + "x " + GameData.itemName(recipe.resultKey());
        String station = "Crafted at: " + recipeStationLabel(recipe) + ".";
        String needs = "Requires: " + CraftingSystem.requirementLabel(recipe) + ".";
        boolean stationReady = recipe.workstation() == null || recipe.workstation() == state.currentWorkstation();
        String readiness = stationReady
                ? CraftingSystem.canCraft(state.player, recipe) ? "Ready to craft here." : "You know this recipe, but need more requirements."
                : "Move to " + recipeStationLabel(recipe) + " to craft it.";
        return recipe.category().label() + ". Output: " + output + ". " + station + " " + needs + " " + recipe.description() + " " + readiness;
    }

    private String recipeStationLabel(CraftingSystem.Recipe recipe) {
        return recipe.workstation() == null ? "Field Crafting" : recipe.workstation().label();
    }

    private void drawInventoryCharacter(Graphics2D g, Actor actor, int x, int y, int w, int h) {
        Rectangle characterDrop = new Rectangle(x + 20, y + 62, w - 40, h - 112);
        effects.inventoryDropZones().add(new InventoryDropZone(characterDrop, InventoryDropKind.CHARACTER, null));

        g.setColor(new Color(20, 23, 31, 220));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(inventoryDragOver(characterDrop) ? new Color(151, 177, 112) : new Color(82, 92, 116));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(246, 224, 151));
        g.drawString(actor.name + "'s Gear", x + 18, y + 28);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(176, 182, 196));
        g.drawString("Drop a potion or ether on the model to use it.", x + 18, y + 48);

        g.setColor(new Color(12, 14, 21, 150));
        int spriteW = 190;
        int spriteH = Math.min(300, Math.max(220, h - 250));
        int spriteX = x + (w - spriteW) / 2;
        int spriteY = y + 96;
        int shadowW = 142;
        int statsH = 106;
        int statsY = y + h - statsH - 18;
        int shadowY = Math.min(statsY - 30, spriteY + spriteH - 30);
        g.fillOval(x + (w - shadowW) / 2, shadowY, shadowW, 34);
        g.drawImage(assets.spriteFit(actor.worldSprite, spriteW, spriteH), spriteX, spriteY, null);

        for (String slot : GameData.EQUIPMENT_SLOTS) {
            drawInventorySlot(g, actor, slot, inventorySlotBounds(slot, x, y, w));
        }
        drawInventoryCharacterStats(g, actor, x + 20, statsY, w - 40, statsH);
    }

    private void drawInventoryCharacterStats(Graphics2D g, Actor actor, int x, int y, int w, int h) {
        g.setColor(new Color(13, 17, 25, 210));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(82, 92, 116, 190));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Stats", x + 12, y + 18);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        int leftX = x + 12;
        int rightX = x + Math.max(190, w / 2);
        g.setColor(new Color(226, 229, 236));
        effects.drawClippedString(g, "Lv " + actor.level + "   HP " + actor.hp + "/" + actor.maxHp + "   MP " + actor.mp + "/" + actor.maxMp,
                leftX, y + 40, Math.max(120, rightX - leftX - 14));
        effects.drawClippedString(g, "ATK " + actor.attack + "   DEF " + actor.defense,
                rightX, y + 40, Math.max(80, x + w - rightX - 12));
        g.setColor(new Color(186, 192, 205));
        effects.drawClippedString(g, "Skill " + actor.skillPoints + "   Stat " + actor.statPoints + "   Prof " + actor.professionSkillPoints,
                leftX, y + 62, Math.max(120, rightX - leftX - 14));
        effects.drawClippedString(g, actor.className,
                rightX, y + 62, Math.max(80, x + w - rightX - 12));
        g.setColor(new Color(176, 182, 196));
        effects.drawClippedString(g, "Professions: " + professionSummary(actor), leftX, y + 86, w - 24);
    }

    private Rectangle inventorySlotBounds(String slot, int x, int y, int w) {
        int leftX = x + 20;
        int rightX = x + w - 116;
        int topY = y + 72;
        int stepY = 72;
        return switch (slot) {
            case "helmet" -> new Rectangle(leftX, topY, 96, 62);
            case "pauldrons" -> new Rectangle(leftX, topY + stepY, 96, 62);
            case "chestpiece" -> new Rectangle(leftX, topY + stepY * 2, 96, 62);
            case "belt" -> new Rectangle(leftX, topY + stepY * 3, 96, 62);
            case "weapon" -> new Rectangle(leftX, topY + stepY * 4, 96, 62);
            case "necklace" -> new Rectangle(rightX, topY, 96, 62);
            case "gloves" -> new Rectangle(rightX, topY + stepY, 96, 62);
            case "ring" -> new Rectangle(rightX, topY + stepY * 2, 96, 62);
            case "leggings" -> new Rectangle(rightX, topY + stepY * 3, 96, 62);
            case "boots" -> new Rectangle(rightX, topY + stepY * 4, 96, 62);
            default -> new Rectangle(leftX, topY, 96, 62);
        };
    }

    private void drawInventorySlot(Graphics2D g, Actor actor, String slot, Rectangle bounds) {
        Equipment equipment = actor.equippedItem(slot);
        effects.inventoryDropZones().add(new InventoryDropZone(bounds, InventoryDropKind.SLOT, slot));
        if (equipment != null) {
            effects.inventoryDragZones().add(new InventoryDragZone(bounds, equipment.key(), slot));
        }
        boolean hoveredDrop = inventoryDragOver(bounds);
        g.setColor(new Color(28, 32, 43, 230));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.setColor(hoveredDrop ? new Color(151, 177, 112) : new Color(82, 92, 116));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.setColor(new Color(246, 224, 151));
        g.drawString(slotLabel(slot), bounds.x + 8, bounds.y + 15);
        if (equipment != null) {
            g.drawImage(assets.sprite(GameData.itemIcon(equipment.key()), 34), bounds.x + 31, bounds.y + 21, null);
        } else {
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(new Color(222, 225, 232));
            String empty = "Empty";
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(empty, bounds.x + (bounds.width - metrics.stringWidth(empty)) / 2, bounds.y + 41);
        }
        if (equipment != null) {
            effects.tooltipZones().add(new TooltipZone(bounds, equipment.name(), equipmentTooltip(equipment),
                    null, ShopRenderer.rarityColor(equipment.rarity())));
        }
    }

    private void drawInventoryDropHint(Graphics2D g, Rectangle bounds, String text) {
        if (effects.inventoryDrag() == null || effects.inventoryDrag().sourceSlot() == null) {
            return;
        }
        boolean hovered = inventoryDragOver(bounds);
        g.setColor(hovered ? new Color(151, 177, 112, 130) : new Color(82, 92, 116, 90));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(hovered ? new Color(238, 239, 244) : new Color(176, 182, 196));
        g.drawString(text, bounds.x + 12, bounds.y + bounds.height - 12);
    }

    private boolean inventoryDragOver(Rectangle bounds) {
        return effects.inventoryDrag() != null && effects.inventoryDragPoint() != null && bounds.contains(effects.inventoryDragPoint());
    }

    private String inventoryTooltip(String itemKey, int count) {
        Equipment equipment = GameData.equipment(itemKey);
        if (equipment != null) {
            return equipmentTooltip(equipment)
                    + "\n\nStack\nCount: " + count
                    + "\n\nEquip\nClick or press the visible number key to equip the selected ally. Drag onto the matching character slot.";
        }
        Item item = GameData.ITEMS.get(itemKey);
        if (CraftingSystem.isCraftingOnlyItem(itemKey)) {
            return "Type\nMaterial\n\nDescription\n" + CraftingSystem.itemDetail(itemKey)
                    + "\n\nStack\nCount: " + count
                    + "\n\nCrafting\nUsed for recipes, upgrades, and workstation projects.";
        }
        if (item == null) {
            return "Type\nField provision\n\nStack\nCount: " + count;
        }
        return itemTooltip(item)
                + "\n\nStack\nCount: " + count
                + "\n\nUse\nClick or press the visible number key to use. Drag onto the character model.";
    }

    private String equipmentTooltip(Equipment equipment) {
        String stats = String.join("  ", equipment.statLines());
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("Type\n")
                .append(equipment.rarityLine()).append(" ").append(slotLabel(equipment.slot()));
        if (!stats.isBlank()) {
            tooltip.append("\n\nStats\n").append(stats);
        }
        tooltip.append("\n\nRequirements\n").append(equipment.levelRangeLine());
        String description = layeredEquipmentDescription(equipment);
        if (!description.isBlank()) {
            tooltip.append("\n\nDescription\n").append(description);
        }
        if (equipment.hasUniqueEffect()) {
            tooltip.append("\n\nEffect\n").append(effectLayerLabel(equipment)).append(": ").append(equipment.uniqueEffect());
        }
        return tooltip.toString();
    }

    private String itemTooltip(Item item) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("Type\n").append(item.rarityLine()).append(" Consumable");
        if ("escape_scroll".equals(item.key())) {
            tooltip.append("\n\nUtility\nEscapes a dungeon instantly.");
        } else if (item.heal() > 0 || item.mp() > 0) {
            tooltip.append("\n\nRecovery\n");
            if (item.heal() > 0 && item.mp() > 0) {
                tooltip.append("Restores ").append(item.heal()).append(" HP and ").append(item.mp()).append(" MP.");
            } else if (item.heal() > 0) {
                tooltip.append("Restores ").append(item.heal()).append(" HP.");
            } else {
                tooltip.append("Restores ").append(item.mp()).append(" MP.");
            }
        } else {
            tooltip.append("\n\nUtility\nField provision.");
        }
        if (item.effectDescription() != null && !item.effectDescription().isBlank()) {
            tooltip.append("\n\nEffect\n").append(item.effectDescription());
        }
        return tooltip.toString();
    }

    private String effectLayerLabel(Equipment equipment) {
        if (equipment.uniqueEffect().startsWith("Affix:")) {
            return "Affix";
        }
        if (equipment.rarity().ordinal() >= ItemRarity.UNIQUE.ordinal()) {
            return equipment.rarityLine() + " property";
        }
        return "Effect";
    }

    private String layeredEquipmentDescription(Equipment equipment) {
        String description = equipment.description() == null ? "" : equipment.description().strip();
        description = removeLeadingSentence(description, equipment.rarityLine());
        description = removeLeadingSentence(description, "Requires level");
        return description;
    }

    private String removeLeadingSentence(String text, String prefix) {
        if (text == null || prefix == null || !text.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return text == null ? "" : text;
        }
        int period = text.indexOf('.');
        if (period < 0 || period + 1 >= text.length()) {
            return "";
        }
        return text.substring(period + 1).stripLeading();
    }

    private void drawInventoryDragGhost(Graphics2D g) {
        if (effects.inventoryDrag() == null || effects.inventoryDragPoint() == null) {
            return;
        }
        Graphics2D ghost = (Graphics2D) g.create();
        ghost.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        int x = effects.inventoryDragPoint().x - 22;
        int y = effects.inventoryDragPoint().y - 22;
        ghost.setColor(new Color(8, 11, 18, 230));
        ghost.fillRoundRect(x - 8, y - 8, 190, 56, 8, 8);
        ghost.setColor(new Color(151, 177, 112));
        ghost.drawRoundRect(x - 8, y - 8, 190, 56, 8, 8);
        ghost.drawImage(assets.sprite(GameData.itemIcon(effects.inventoryDrag().itemKey()), 42), x, y, null);
        ghost.setFont(new Font("SansSerif", Font.BOLD, 13));
        ghost.setColor(new Color(238, 239, 244));
        ghost.drawString(shortText(GameData.itemName(effects.inventoryDrag().itemKey()), 18), x + 50, y + 18);
        ghost.setFont(new Font("SansSerif", Font.PLAIN, 11));
        ghost.setColor(new Color(176, 182, 196));
        ghost.drawString(effects.inventoryDrag().sourceSlot() == null ? "From shared pack" : "Equipped " + slotLabel(effects.inventoryDrag().sourceSlot()), x + 50, y + 36);
        ghost.dispose();
    }

    private void drawInventoryPartySelector(Graphics2D g, int x, int y, int w) {
        List<Actor> members = state.partyMembers();
        int cols = Math.min(5, Math.max(1, members.size()));
        int cardW = (w - Math.max(0, cols - 1) * 8) / cols;
        int cardH = members.size() > 5 ? 38 : 52;
        Actor selected = state.partyScreenActor();
        for (int i = 0; i < members.size(); i++) {
            Actor member = members.get(i);
            int col = i % cols;
            int row = i / cols;
            int cardX = x + col * (cardW + 8);
            int cardY = y + row * (cardH + 6);
            boolean active = member == selected;
            g.setColor(active ? new Color(42, 57, 50, 235) : new Color(28, 32, 43, 235));
            g.fillRoundRect(cardX, cardY, cardW, cardH, 8, 8);
            g.setColor(active ? new Color(103, 151, 117) : new Color(82, 92, 116));
            g.drawRoundRect(cardX, cardY, cardW, cardH, 8, 8);
            int spriteW = cardH <= 44 ? 28 : 34;
            int spriteH = cardH <= 44 ? 34 : 44;
            g.drawImage(assets.spriteFit(member.worldSprite, spriteW, spriteH), cardX + 8, cardY + 4, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(new Color(235, 236, 240));
            g.drawString(shortText(member.name, cardH <= 44 ? 10 : 13), cardX + spriteW + 18, cardY + (cardH <= 44 ? 17 : 21));
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            g.drawString(shortText(member.className + "  Lv " + member.level, 14), cardX + spriteW + 18, cardY + (cardH <= 44 ? 32 : 38));
            int index = i;
            effects.buttons().add(new UiButton(new Rectangle(cardX, cardY, cardW, cardH), "inventory-member:" + member.name, () -> {
                state.selectPartyScreenActor(index);
                effects.setInventoryItemScroll(0);
                effects.resetPartyScreenScroll();
                effects.repaintPanel();
            }));
        }
    }

    private int inventoryPartySelectorHeight() {
        int members = Math.max(1, state.partyMembers().size());
        int cols = Math.min(5, members);
        int rows = (members + cols - 1) / cols;
        int cardH = members > 5 ? 38 : 52;
        return rows * cardH + Math.max(0, rows - 1) * 6;
    }


    public interface Effects {
        int inventoryItemScroll();

        void setInventoryItemScroll(int value);

        int craftingRecipeScroll();

        void setCraftingRecipeScroll(int value);

        int craftingRecipeCategoryIndex();

        void setCraftingRecipeCategoryIndex(int value);

        int gameAreaWidth();

        int gameAreaCenteredX(int width);

        int viewHeight();

        Point hoverPoint();

        InventoryDrag inventoryDrag();

        Point inventoryDragPoint();

        List<UiButton> buttons();

        List<TooltipZone> tooltipZones();

        List<InventoryDragZone> inventoryDragZones();

        List<InventoryDropZone> inventoryDropZones();

        void resetPartyScreenScroll();

        void repaintPanel();

        public void drawOverlayBase(Graphics2D g, int x, int y, int w, int h);

        void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled);

        public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth);

        public void drawCenteredIn(Graphics2D g, String text, int x, int y, int width);

        public void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows);

        Color blend(Color a, Color b, double t);
    }
}
