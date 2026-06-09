package com.alderfall.game.ui;

import com.alderfall.game.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VillageSidebarRenderer {
    private final AssetStore assets;
    private final GameState state;
    private final Effects effects;
    private int villageListScroll;
    private int lastVillageTab = -1;
    private String lastVillagePropCategory = "";
    private String lastInteriorAssetCategory = "";
    private String villageBuildSearch = "";
    private String lastVillageBuildSearch = "";
    private boolean villageSearchFocused;
    private Rectangle villageSearchBounds = new Rectangle();

    public VillageSidebarRenderer(AssetStore assets, GameState state, Effects effects) {
        this.assets = assets;
        this.state = state;
        this.effects = effects;
    }

    public boolean updateSearchFocus(java.awt.Point point) {
        villageSearchFocused = villageSearchApplies() && villageSearchBounds.contains(point);
        return villageSearchFocused;
    }

    public void clearSearchFocus() {
        villageSearchFocused = false;
    }

    public boolean searchFocused() {
        return villageSearchFocused;
    }

    public void scrollList(int delta) {
        villageListScroll = Math.max(0, villageListScroll + delta);
    }

    private int viewHeight() {
        return effects.viewHeight();
    }

    private void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled) {
        effects.actionButton(g, x, y, w, h, label, action, fill, border, enabled);
    }

    private void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth) {
        effects.drawClippedString(g, text, x, y, maxWidth);
    }

    private void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines) {
        effects.drawWrapped(g, text, x, y, width, lineHeight, maxLines);
    }

    private void drawCenteredIn(Graphics2D g, String text, int x, int y, int width) {
        effects.drawCenteredIn(g, text, x, y, width);
    }

    private void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows) {
        effects.drawScrollIndicator(g, x, y, h, totalRows, scroll, visibleRows);
    }

    private String buildingPreviewSprite(VillageManager.BuildingPlan plan) {
        return effects.buildingPreviewSprite(plan);
    }

    private String villageCostDisplay(VillageManager.VillageCost cost) {
        return effects.villageCostDisplay(cost);
    }

    private void exportMapEditorDesign() {
        effects.exportMapEditorDesign();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String assetLabel(String asset) {
        String cleaned = asset.replace("city_prop_", "").replace("interior_", "").replace("deco_", "").replace('_', ' ');
        if (cleaned.length() > 15) {
            cleaned = cleaned.substring(0, 14) + ".";
        }
        return cleaned;
    }

    public void drawVillageSidebar(Graphics2D g, int left) {
        if (lastVillageTab != state.villageTab
                || !lastVillagePropCategory.equals(state.selectedVillagePropCategory)
                || !lastInteriorAssetCategory.equals(state.selectedInteriorAssetCategory)
                || !lastVillageBuildSearch.equals(villageBuildSearch)) {
            villageListScroll = 0;
            lastVillageTab = state.villageTab;
            lastVillagePropCategory = state.selectedVillagePropCategory;
            lastInteriorAssetCategory = state.selectedInteriorAssetCategory;
            lastVillageBuildSearch = villageBuildSearch;
        }
        int x = left + 18;
        int w = GameConfig.SIDEBAR_WIDTH - 36;
        int y = 32;
        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.setColor(new Color(243, 238, 219));
        g.drawString("Oathstead", x, y);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(198, 202, 211));
        drawClippedString(g, (state.isManagedVillageInterior() ? "Interior" : "Village")
                + " | Storage " + state.villageStorageUsed() + "/" + state.villageStorageCapacity()
                + " | Gold " + state.player.gold, x, y + 22, w);

        String[] tabs = {"Build", "Tiles", "Props", "Inside", "Workers"};
        int tabY = y + 44;
        int tabW = (w - 8) / 2;
        for (int i = 0; i < tabs.length; i++) {
            int tab = i;
            boolean selected = state.villageTab == i;
            int bx = x + (i % 2) * (tabW + 8);
            int by = tabY + (i / 2) * 30;
            actionButton(g, bx, by, tabW, 24, tabs[i], () -> setVillageTabFromSidebar(tab),
                    selected ? new Color(57, 76, 60) : new Color(35, 39, 54),
                    selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
        }

        int actionY = tabY + 94;
        int modeW = (w - 8) / 2;
        sidebarModeButton(g, x, actionY, modeW, "Place", "place", new Color(126, 176, 95));
        sidebarModeButton(g, x + modeW + 8, actionY, modeW, "Move", "move", new Color(169, 137, 74));
        sidebarModeButton(g, x, actionY + 30, modeW, "Upgrade", "upgrade", new Color(125, 107, 166));
        sidebarModeButton(g, x + modeW + 8, actionY + 30, modeW, "Delete", "delete", new Color(149, 96, 88));

        int listY = actionY + 72;
        if (state.config.showMapEditorButton || state.isMapEditorMap()) {
            listY = drawMapEditorSidebarControls(g, x, listY, w) + 10;
        }
        if (state.villageTab == 2) {
            listY = drawVillagePropCategoryButtons(g, x, listY, w) + 10;
        }
        if (state.villageTab == 3) {
            listY = drawInteriorCategoryButtons(g, x, listY, w) + 10;
        }
        if (villageSearchApplies()) {
            listY = drawVillageSearchBox(g, x, listY, w) + 10;
        } else {
            villageSearchFocused = false;
            villageSearchBounds = new Rectangle();
        }
        int footerH = 92;
        int listH = Math.max(120, viewHeight() - listY - footerH);
        if (state.villageTab == 0) {
            drawVillageSidebarBuildings(g, x, listY, w, listH);
        } else if (state.villageTab == 1) {
            drawVillageSidebarTiles(g, x, listY, w, listH);
        } else if (state.villageTab == 2) {
            drawVillageSidebarProps(g, x, listY, w, listH);
        } else if (state.villageTab == 3) {
            drawVillageSidebarInteriors(g, x, listY, w, listH);
        } else {
            drawVillageSidebarWorkers(g, x, listY, w, listH);
        }

        int statusY = viewHeight() - 76;
        g.setColor(new Color(11, 13, 20, 212));
        g.fillRoundRect(x, statusY, w, 44, 8, 8);
        g.setColor(new Color(74, 82, 105));
        g.drawRoundRect(x, statusY, w, 44, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(198, 202, 211));
        drawWrapped(g, state.status, x + 10, statusY + 18, w - 20, 14, 2);
        actionButton(g, x, viewHeight() - 26, w, 22, "Close Village", state::toggleVillage,
                new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    private int drawMapEditorSidebarControls(Graphics2D g, int x, int y, int w) {
        int h = state.isMapEditorMap() ? 62 : 118;
        g.setColor(new Color(11, 13, 20, 205));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(74, 82, 105));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(246, 224, 151));
        drawClippedString(g, state.isMapEditorMap() ? "Map Editor: " + state.world.label(state.currentMapId) : "Map Editor",
                x + 10, y + 19, w - 20);
        if (state.isMapEditorMap()) {
            int buttonW = (w - 28) / 2;
            actionButton(g, x + 10, y + 28, buttonW, 24, "Export", this::exportMapEditorDesign,
                    new Color(57, 71, 102), new Color(110, 127, 160), true);
            actionButton(g, x + 18 + buttonW, y + 28, buttonW, 24, "Exit Editor", state::exitMapEditor,
                    new Color(83, 61, 61), new Color(149, 96, 88), true);
            return y + h;
        }
        int buttonW = 28;
        int valueX = x + 92;
        actionButton(g, x + 10, y + 30, buttonW, 24, "<", state::previousMapEditorKind,
                new Color(35, 39, 54), new Color(86, 98, 128), true);
        drawCenteredIn(g, state.selectedMapEditorKindLabel(), valueX, y + 48, w - 184);
        actionButton(g, x + w - 38, y + 30, buttonW, 24, ">", state::nextMapEditorKind,
                new Color(35, 39, 54), new Color(86, 98, 128), true);
        actionButton(g, x + 10, y + 62, buttonW, 24, "<", state::previousMapEditorSize,
                new Color(35, 39, 54), new Color(86, 98, 128), true);
        drawCenteredIn(g, state.selectedMapEditorSizeLabel(), valueX, y + 80, w - 184);
        actionButton(g, x + w - 38, y + 62, buttonW, 24, ">", state::nextMapEditorSize,
                new Color(35, 39, 54), new Color(86, 98, 128), true);
        actionButton(g, x + 10, y + 92, w - 20, 20, "Enter Editor", state::enterMapEditor,
                new Color(57, 76, 60), new Color(126, 176, 95), true);
        return y + h;
    }

    private void setVillageTabFromSidebar(int tab) {
        state.setVillageTab(tab);
        villageListScroll = 0;
    }

    public boolean handleVillageSearchKey(KeyEvent event) {
        int code = event.getKeyCode();
        if (!villageSearchApplies()) {
            villageSearchFocused = false;
            return false;
        }
        if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER) {
            villageSearchFocused = false;
            return true;
        }
        if (code == KeyEvent.VK_BACK_SPACE) {
            if (!villageBuildSearch.isEmpty()) {
                villageBuildSearch = villageBuildSearch.substring(0, villageBuildSearch.length() - 1);
            }
            return true;
        }
        if (code == KeyEvent.VK_DELETE) {
            villageBuildSearch = "";
            return true;
        }
        char ch = event.getKeyChar();
        if ((Character.isLetterOrDigit(ch) || ch == ' ' || ch == '-' || ch == '_') && villageBuildSearch.length() < 32) {
            villageBuildSearch += ch;
            return true;
        }
        return true;
    }

    private void sidebarModeButton(Graphics2D g, int x, int y, int w, String label, String action, Color border) {
        actionButton(g, x, y, w, 24, label, () -> state.setVillageEditAction(action),
                action.equals(state.villageEditAction) ? new Color(57, 76, 60) : new Color(35, 39, 54),
                border, true);
    }

    private int drawVillagePropCategoryButtons(Graphics2D g, int x, int y, int w) {
        List<String> categories = outdoorAssetCategoriesForCurrentEditor();
        int buttonW = (w - 8) / 2;
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            boolean selected = category.equals(state.selectedVillagePropCategory);
            int bx = x + (i % 2) * (buttonW + 8);
            int by = y + (i / 2) * 26;
            actionButton(g, bx, by, buttonW, 22, category, () -> {
                selectOutdoorAssetCategoryForCurrentEditor(category);
                villageListScroll = 0;
            }, selected ? new Color(57, 76, 60) : new Color(35, 39, 54),
                    selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
        }
        return y + Math.max(1, (categories.size() + 1) / 2) * 26;
    }

    private int drawInteriorCategoryButtons(Graphics2D g, int x, int y, int w) {
        List<String> categories = VillageManager.interiorAssetCategories();
        int buttonW = (w - 8) / 2;
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            boolean selected = category.equals(state.selectedInteriorAssetCategory)
                    || ("All".equals(category) && !VillageManager.interiorAssetCategories().contains(state.selectedInteriorAssetCategory));
            int bx = x + (i % 2) * (buttonW + 8);
            int by = y + (i / 2) * 26;
            actionButton(g, bx, by, buttonW, 22, category, () -> {
                state.selectInteriorAssetCategory(category);
                villageListScroll = 0;
            }, selected ? new Color(57, 76, 60) : new Color(35, 39, 54),
                    selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
        }
        return y + Math.max(1, (categories.size() + 1) / 2) * 26;
    }

    private int drawVillageSearchBox(Graphics2D g, int x, int y, int w) {
        villageSearchBounds = new Rectangle(x, y, w, 28);
        g.setColor(villageSearchFocused ? new Color(24, 30, 42, 238) : new Color(16, 19, 28, 220));
        g.fillRoundRect(x, y, w, 28, 8, 8);
        g.setColor(villageSearchFocused ? new Color(145, 166, 214) : new Color(74, 82, 105));
        g.drawRoundRect(x, y, w, 28, 8, 8);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        String text = villageBuildSearch.isBlank() ? "Search" : villageBuildSearch;
        g.setColor(villageBuildSearch.isBlank() ? new Color(132, 138, 152) : new Color(228, 231, 238));
        drawClippedString(g, text, x + 10, y + 18, w - 20);
        return y + 28;
    }

    private boolean villageSearchApplies() {
        return state.mode == GameMode.VILLAGE && (state.villageTab == 0 || state.villageTab == 2 || state.villageTab == 3);
    }

    private List<String> outdoorAssetCategoriesForCurrentEditor() {
        if (!state.isMapEditorMap()) {
            return VillageManager.outdoorAssetCategories();
        }
        Map<String, Boolean> categories = new LinkedHashMap<>();
        for (String category : VillageManager.outdoorAssetCategories()) {
            categories.put(category, true);
        }
        for (String asset : assets.assetNames()) {
            if (isEditorOutdoorAsset(asset)) {
                categories.put(editorAssetCategory(asset), true);
            }
        }
        return List.copyOf(categories.keySet());
    }

    private List<VillageManager.PlaceableAsset> outdoorAssetsForCurrentEditor(String category) {
        if (!state.isMapEditorMap()) {
            return VillageManager.outdoorAssets(category);
        }
        Map<String, VillageManager.PlaceableAsset> byAsset = new LinkedHashMap<>();
        for (VillageManager.PlaceableAsset asset : VillageManager.outdoorAssets()) {
            if (asset.category().equals(category)) {
                byAsset.put(asset.asset(), asset);
            }
        }
        for (String asset : assets.assetNames()) {
            if (!isEditorOutdoorAsset(asset) || !editorAssetCategory(asset).equals(category)) {
                continue;
            }
            byAsset.putIfAbsent(asset, new VillageManager.PlaceableAsset(asset, editorAssetLabel(asset), 48,
                    VillageManager.VillageCost.free(), category));
        }
        if (byAsset.isEmpty()) {
            return VillageManager.outdoorAssets(category);
        }
        return List.copyOf(byAsset.values());
    }

    private void selectOutdoorAssetCategoryForCurrentEditor(String category) {
        if (!state.isMapEditorMap()) {
            state.selectVillagePropCategory(category);
            return;
        }
        List<VillageManager.PlaceableAsset> options = outdoorAssetsForCurrentEditor(category);
        if (options.isEmpty()) {
            return;
        }
        state.selectedVillagePropCategory = category;
        state.selectedVillageAsset = options.get(0).asset();
        state.setVillageEditAction("place");
    }

    private boolean isEditorOutdoorAsset(String asset) {
        return asset != null
                && (asset.startsWith("deco_")
                || asset.startsWith("city_")
                || asset.startsWith("village_")
                || asset.startsWith("player_village_")
                || asset.startsWith("location_"))
                && !asset.startsWith("interior_")
                && !asset.endsWith("_anim");
    }

    private String editorAssetCategory(String asset) {
        if (asset.startsWith("city_")) {
            return "City Props";
        }
        if (asset.startsWith("village_") || asset.startsWith("player_village_")) {
            return "Village Props";
        }
        if (asset.startsWith("location_")) {
            return "Location Props";
        }
        if (asset.startsWith("deco_")) {
            return "Nature Props";
        }
        return "Props";
    }

    private String editorAssetLabel(String asset) {
        return assetLabel(asset
                .replace("city_prop_", "")
                .replace("village_prop_", "")
                .replace("player_village_", "")
                .replace("location_", "")
                .replace("deco_", "")
                .replace('_', ' '));
    }

    private List<VillageManager.BuildingPlan> filteredBuildingPlans(List<VillageManager.BuildingPlan> plans) {
        String query = normalizedVillageSearch();
        if (query.isBlank()) {
            return plans;
        }
        return plans.stream()
                .filter(plan -> searchable(plan.label(), plan.style(), plan.description()).contains(query))
                .toList();
    }

    private List<VillageManager.PlaceableAsset> filteredPlaceableAssets(List<VillageManager.PlaceableAsset> items) {
        String query = normalizedVillageSearch();
        if (query.isBlank()) {
            return items;
        }
        return items.stream()
                .filter(item -> searchable(item.label(), item.asset(), item.category()).contains(query))
                .toList();
    }

    private String normalizedVillageSearch() {
        return villageBuildSearch == null ? "" : villageBuildSearch.strip().toLowerCase();
    }

    private String searchable(String... parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(part.toLowerCase().replace('_', ' '));
            result.append(' ').append(part.toLowerCase());
        }
        return result.toString();
    }

    private void drawVillageSidebarBuildings(Graphics2D g, int x, int y, int w, int h) {
        List<VillageManager.BuildingPlan> plans = filteredBuildingPlans(VillageManager.buildingPlans());
        int rowH = 72;
        int visibleRows = Math.max(1, h / rowH);
        int scroll = clamp(villageListScroll, 0, Math.max(0, plans.size() - visibleRows));
        villageListScroll = scroll;
        Graphics2D listG = (Graphics2D) g.create();
        listG.setClip(x, y, w, h);
        int rowY = y - scroll * rowH;
        for (VillageManager.BuildingPlan plan : plans) {
            if (rowY + rowH > y && rowY < y + h) {
                drawVillageSidebarBuildingRow(listG, plan, x, rowY, w, rowH - 8);
            }
            rowY += rowH;
        }
        listG.dispose();
        drawScrollIndicator(g, x + w + 4, y, h, plans.size(), scroll, visibleRows);
    }

    private void drawVillageSidebarBuildingRow(Graphics2D g, VillageManager.BuildingPlan plan, int x, int y, int w, int h) {
        boolean selected = plan.style().equals(state.selectedVillageBuildingStyle);
        boolean affordable = state.canAffordVillageCost(plan.cost());
        actionButton(g, x, y, w, h, "", () -> state.selectVillageBuildingStyle(plan.style()),
                selected ? new Color(41, 61, 48, 242) : new Color(24, 29, 41, 235),
                selected ? new Color(126, 176, 95) : affordable ? new Color(86, 98, 128) : new Color(92, 74, 74), true);
        g.drawImage(assets.spriteFit(buildingPreviewSprite(plan), 48, 48), x + 8, y + 8, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(238, 239, 244));
        drawClippedString(g, plan.label(), x + 64, y + 20, w - 72);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(affordable ? new Color(176, 182, 196) : new Color(222, 143, 132));
        drawClippedString(g, plan.width() + "x" + plan.depth() + " | " + villageCostDisplay(plan.cost()), x + 64, y + 38, w - 72);
        g.setColor(new Color(151, 177, 112));
        drawClippedString(g, plan.storageCapacity() > 0 ? "Storage +" + plan.storageCapacity()
                : plan.workerRole().isBlank() ? "Max Lv " + plan.maxLevel() : VillageManager.workerRole(plan.workerRole()).label(),
                x + 64, y + 55, w - 72);
    }

    private void drawVillageSidebarTiles(Graphics2D g, int x, int y, int w, int h) {
        List<VillageManager.TilePlan> tiles = state.isManagedVillageInterior()
                ? VillageManager.tilePlans().stream()
                .filter(tile -> tile.tile() == 'i' || tile.tile() == 'o')
                .toList()
                : VillageManager.tilePlans();
        int rowH = 58;
        int visibleRows = Math.max(1, h / rowH);
        int scroll = clamp(villageListScroll, 0, Math.max(0, tiles.size() - visibleRows));
        villageListScroll = scroll;
        Graphics2D listG = (Graphics2D) g.create();
        listG.setClip(x, y, w, h);
        int rowY = y - scroll * rowH;
        for (VillageManager.TilePlan tile : tiles) {
            if (rowY + rowH > y && rowY < y + h) {
                boolean selected = tile.tile() == state.selectedVillageTile;
                actionButton(listG, x, rowY, w, rowH - 8, "", () -> state.selectVillageTile(tile.tile()),
                        selected ? new Color(41, 61, 48, 242) : new Color(24, 29, 41, 235),
                        selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
                listG.drawImage(assets.tile(tile.tile(), 38), x + 8, rowY + 6, null);
                listG.setFont(new Font("SansSerif", Font.BOLD, 13));
                listG.setColor(new Color(238, 239, 244));
                drawClippedString(listG, tile.label(), x + 56, rowY + 20, w - 64);
                listG.setFont(new Font("SansSerif", Font.PLAIN, 11));
                listG.setColor(new Color(176, 182, 196));
                drawClippedString(listG, villageCostDisplay(tile.cost()), x + 56, rowY + 38, w - 64);
            }
            rowY += rowH;
        }
        listG.dispose();
        drawScrollIndicator(g, x + w + 4, y, h, tiles.size(), scroll, visibleRows);
    }

    private void drawVillageSidebarProps(Graphics2D g, int x, int y, int w, int h) {
        List<VillageManager.PlaceableAsset> props = filteredPlaceableAssets(outdoorAssetsForCurrentEditor(state.selectedVillagePropCategory));
        drawVillageSidebarAssetRows(g, x, y, w, h, props, false);
    }

    private void drawVillageSidebarInteriors(Graphics2D g, int x, int y, int w, int h) {
        drawVillageSidebarAssetRows(g, x, y, w, h,
                filteredPlaceableAssets(VillageManager.interiorAssets(state.selectedInteriorAssetCategory)), true);
    }

    private void drawVillageSidebarAssetRows(Graphics2D g, int x, int y, int w, int h, List<VillageManager.PlaceableAsset> items, boolean interior) {
        int rowH = 58;
        int visibleRows = Math.max(1, h / rowH);
        int scroll = clamp(villageListScroll, 0, Math.max(0, items.size() - visibleRows));
        villageListScroll = scroll;
        Graphics2D listG = (Graphics2D) g.create();
        listG.setClip(x, y, w, h);
        int rowY = y - scroll * rowH;
        for (VillageManager.PlaceableAsset item : items) {
            if (rowY + rowH > y && rowY < y + h) {
                boolean selected = interior ? item.asset().equals(state.selectedInteriorAsset) : item.asset().equals(state.selectedVillageAsset);
                actionButton(listG, x, rowY, w, rowH - 8, "",
                        () -> {
                            if (interior) {
                                state.selectInteriorAsset(item.asset());
                            } else {
                                state.selectVillageAsset(item.asset());
                            }
                        },
                        selected ? new Color(41, 61, 48, 242) : new Color(24, 29, 41, 235),
                        selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
                listG.drawImage(assets.spriteFit(item.asset(), 40, 40), x + 8, rowY + 5, null);
                listG.setFont(new Font("SansSerif", Font.BOLD, 13));
                listG.setColor(new Color(238, 239, 244));
                drawClippedString(listG, item.label(), x + 56, rowY + 20, w - 64);
                listG.setFont(new Font("SansSerif", Font.PLAIN, 11));
                listG.setColor(new Color(176, 182, 196));
                drawClippedString(listG, villageCostDisplay(item.cost()), x + 56, rowY + 38, w - 64);
            }
            rowY += rowH;
        }
        listG.dispose();
        drawScrollIndicator(g, x + w + 4, y, h, items.size(), scroll, visibleRows);
    }

    private void drawVillageSidebarWorkers(Graphics2D g, int x, int y, int w, int h) {
        List<Actor> allies = new ArrayList<>();
        allies.addAll(state.stationedAllies());
        allies.addAll(state.activeAllies());
        if (allies.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(176, 182, 196));
            drawWrapped(g, "No allies available for village work.", x, y + 22, w, 16, 3);
            return;
        }
        int rowH = 62;
        int visibleRows = Math.max(1, h / rowH);
        int scroll = clamp(villageListScroll, 0, Math.max(0, allies.size() - visibleRows));
        villageListScroll = scroll;
        Graphics2D listG = (Graphics2D) g.create();
        listG.setClip(x, y, w, h);
        int rowY = y - scroll * rowH;
        for (Actor ally : allies) {
            if (rowY + rowH > y && rowY < y + h) {
                boolean stationed = state.villageAllies.contains(ally.name);
                listG.setColor(new Color(24, 29, 41, 235));
                listG.fillRoundRect(x, rowY, w, rowH - 8, 8, 8);
                listG.setColor(stationed ? new Color(126, 176, 95) : new Color(86, 98, 128));
                listG.drawRoundRect(x, rowY, w, rowH - 8, 8, 8);
                listG.drawImage(assets.spriteFit(ally.worldSprite, 34, 42), x + 8, rowY + 5, null);
                listG.setFont(new Font("SansSerif", Font.BOLD, 13));
                listG.setColor(new Color(238, 239, 244));
                drawClippedString(listG, ally.name, x + 50, rowY + 19, w - 112);
                listG.setFont(new Font("SansSerif", Font.PLAIN, 11));
                listG.setColor(new Color(176, 182, 196));
                drawClippedString(listG, stationed ? VillageManager.workerRole(state.villageRoleFor(ally.name)).label() : ally.className,
                        x + 50, rowY + 38, w - 112);
                actionButton(listG, x + w - 58, rowY + 14, 48, 24, stationed ? "Recall" : "Set",
                        () -> {
                            if (stationed) {
                                state.recallAlly(ally.name);
                            } else {
                                state.stationAlly(ally.name);
                            }
                        },
                        stationed ? new Color(57, 76, 60) : new Color(69, 62, 88),
                        stationed ? new Color(126, 176, 95) : new Color(125, 107, 166), true);
            }
            rowY += rowH;
        }
        listG.dispose();
        drawScrollIndicator(g, x + w + 4, y, h, allies.size(), scroll, visibleRows);
    }


    public interface Effects {
        int viewHeight();

        void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled);

        public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth);

        public void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines);

        public void drawCenteredIn(Graphics2D g, String text, int x, int y, int width);

        public void drawScrollIndicator(Graphics2D g, int x, int y, int h, int totalRows, int scroll, int visibleRows);

        public String buildingPreviewSprite(VillageManager.BuildingPlan plan);

        public String villageCostDisplay(VillageManager.VillageCost cost);

        void exportMapEditorDesign();
    }
}
