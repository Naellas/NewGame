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
    private String buildingCategory = "All";
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
        g.setFont(new Font("Serif", Font.BOLD, 25));
        g.setColor(new Color(243, 238, 219));
        g.drawString("OATHSTEAD", x, y);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(198, 202, 211));
        drawClippedString(g, (state.isManagedVillageInterior() ? "Interior" : state.villageStage().kind())
                + " | Storage " + state.villageStorageUsed() + "/" + state.villageStorageCapacity()
                + " | Gold " + state.player.gold, x, y + 22, w);

        String[] tabs = {"Build", "Terrain", "Props", "Inside", "Workers", "Manage", "Housing"};
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

        int actionY = tabY + 124;
        int modeW = (w - 8) / 2;
        boolean editing=state.villageTab<=3;
        if(editing) {
            int bw=(w-12)/3;
            sidebarModeButton(g,x,actionY,bw,"Place","place",SettlementPanelStyle.TEAL);
            sidebarModeButton(g,x+bw+6,actionY,bw,"Move","move",SettlementPanelStyle.GOLD);
            sidebarModeButton(g,x+2*(bw+6),actionY,bw,"Remove","delete",new Color(149,96,88));
        }
        int listY=actionY+(editing?38:4);
        if(state.villageTab==2 || state.villageTab==3)listY=drawPropOffsets(g,x,listY,w)+8;
        if (state.villageTab == 1 && !state.isManagedVillageInterior() && !state.isMapEditorMap()) {
            listY = drawTerrainTools(g, x, listY, w) + 8;
        }
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
        if (state.villageTab == 0 || state.villageTab == 5) {
            String[] categories = {"All", "Housing", "Production", "Civic"};
            int cw=(w-12)/4;
            for(int i=0;i<categories.length;i++) {
                String category=categories[i];
                actionButton(g,x+i*(cw+4),listY,cw,24,category,
                        () -> { buildingCategory=category; villageListScroll=0; },
                        category.equals(buildingCategory)?new Color(42,83,74):new Color(27,39,47),
                        category.equals(buildingCategory)?SettlementPanelStyle.TEAL:new Color(64,82,91),true);
            }
            listY+=34;
        }
        if(state.villageTab==0){
            actionButton(g,x,listY,w,30,"Open building catalogue",()->state.villageCatalogOpen=true,new Color(44,60,62),SettlementPanelStyle.GOLD,true);
            listY+=40;
        }
        if (state.villageTab == 5) listY=drawTownOverview(g,x,listY,w)+12;
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
        } else if (state.villageTab == 5) {
            drawManagedBuildings(g, x, listY, w, listH);
        } else if(state.villageTab==6) {
            drawHousing(g,x,listY,w,listH);
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
        return state.mode == GameMode.VILLAGE && state.villageTab != 4;
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
        plans=plans.stream().filter(plan -> "All".equals(buildingCategory)
                || buildingCategory.equals(VillageManager.buildingCategory(plan.style()))).toList();
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
        int rowH = 110;
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
        g.drawImage(assets.spriteFit(buildingPreviewSprite(plan), 52, 64), x + 6, y + 4, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(238, 239, 244));
        drawClippedString(g, plan.label(), x + 64, y + 20, w - 72);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(affordable ? new Color(176, 182, 196) : new Color(222, 143, 132));
        drawWrapped(g,villageCostDisplay(plan.cost()),x+64,y+38,w-76,14,2);
        g.setColor(new Color(151, 177, 112));
        int beds=SettlementEconomy.housing(plan.style(),1);
        String benefit=beds>0?beds+" beds":SettlementEconomy.slots(plan.style(),1)+" worker | "+SettlementEconomy.output(plan.style()).resource().replace('_',' ');
        drawClippedString(g,benefit,x+64,y+72,w-76);
        g.setColor(SettlementPanelStyle.MUTED);
        drawClippedString(g,selected?"Click the map to place":"Select to build  /  "+plan.width()+" x "+plan.depth(),x+64,y+91,w-76);

    }

    private void drawVillageSidebarTiles(Graphics2D g, int x, int y, int w, int h) {
        List<VillageManager.TilePlan> tiles = state.isManagedVillageInterior()
                ? VillageManager.tilePlans().stream()
                .filter(tile -> tile.tile() == 'i' || tile.tile() == 'o')
                .toList()
                : VillageManager.tilePlans().stream().filter(tile -> tile.tile() != 'i' && tile.tile() != 'o').toList();
        tiles = tiles.stream().filter(tile -> searchable(tile.label(),tile.description()).contains(normalizedVillageSearch())).toList();
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
                actionButton(listG, x, rowY, w, rowH - 8, "", () -> { state.selectVillageTile(tile.tile()); state.villageTerrainTool = "paint"; },
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

    private void drawVillageSidebarWorkers(Graphics2D g,int x,int y,int w,int h) {
        if(!state.pendingWorkplaceAlly.isBlank()) {
            SettlementPanelStyle.card(g,x,y,w,78);
            g.setColor(SettlementPanelStyle.GOLD);g.setFont(new Font("SansSerif",Font.BOLD,13));
            drawWrapped(g,"Select workplace for "+state.pendingWorkplaceAlly,x+10,y+20,w-20,17,2);
            actionButton(g,x+10,y+46,w-20,24,"Cancel selection",state::cancelWorkplaceSelection,new Color(35,39,54),SettlementPanelStyle.MUTED,true);
            y+=88;h-=88;
        }
        List<Actor> workers=new ArrayList<>(state.stationedAllies());
        for(Actor ally:state.activeAllies())if(!workers.contains(ally))workers.add(ally);
        int rowH=116,visible=Math.max(1,h/rowH);
        villageListScroll=clamp(villageListScroll,0,Math.max(0,workers.size()-visible));
        for(int i=villageListScroll;i<Math.min(workers.size(),villageListScroll+visible);i++) {
            Actor ally=workers.get(i);int by=y+(i-villageListScroll)*rowH;
            SettlementPanelStyle.card(g,x,by,w,rowH-8);
            g.drawImage(assets.spriteFit(ally.worldSprite,34,48),x+8,by+8,null);
            g.setFont(new Font("SansSerif",Font.BOLD,14));g.setColor(SettlementPanelStyle.INK);
            drawClippedString(g,ally.name,x+52,by+22,w-62);
            CityBuilding workplace=state.playerVillageBuildingByKey(state.buildingAssignmentForAlly(ally.name));
            CityBuilding home=state.housingForAlly(ally.name);
            g.setFont(new Font("SansSerif",Font.PLAIN,11));g.setColor(home==null?SettlementPanelStyle.GOLD:SettlementPanelStyle.TEAL);
            drawClippedString(g,home==null?"Unhoused - cannot work":"Housed in "+VillageManager.buildingLabel(home.style()),x+52,by+40,w-62);
            g.setColor(SettlementPanelStyle.MUTED);
            String job=workplace==null?"No workplace":VillageManager.buildingLabel(workplace.style())+" | Skill "+SettlementEconomy.skill(ally,SettlementEconomy.output(workplace.style()));
            drawClippedString(g,job,x+12,by+62,w-24);
            actionButton(g,x+10,by+75,w/2-15,24,"Set",()->state.beginWorkplaceSelection(ally.name),new Color(44,82,68),SettlementPanelStyle.TEAL,true);
            actionButton(g,x+w/2+5,by+75,w/2-15,24,"Recall",()->state.recallAlly(ally.name),new Color(35,39,54),SettlementPanelStyle.MUTED,state.villageAllies.contains(ally.name));
        }
        if(workers.isEmpty()){g.setColor(SettlementPanelStyle.MUTED);drawWrapped(g,"Recruit residents at the town board.",x,y+22,w,18,3);}
        drawScrollIndicator(g,x+w+4,y,h,workers.size(),villageListScroll,visible);
    }

    private void drawHousing(Graphics2D g,int x,int y,int w,int h) {
        int residents=state.stationedAllies().size(),beds=state.villageHousingCapacity();
        SettlementPanelStyle.card(g,x,y,w,90);
        SettlementPanelStyle.metric(g,"Residents",Integer.toString(residents),x+12,y+25);
        SettlementPanelStyle.metric(g,"Beds",Integer.toString(beds),x+w/2,y+25);
        g.setFont(new Font("SansSerif",Font.PLAIN,12));g.setColor(SettlementPanelStyle.GOLD);
        drawClippedString(g,Math.max(0,residents-beds)+" unhoused | Beds assigned automatically",x+12,y+73,w-24);
        List<Actor> people=state.stationedAllies();
        int visible=Math.max(1,(h-104)/56);
        villageListScroll=clamp(villageListScroll,0,Math.max(0,people.size()-visible));
        for(int i=villageListScroll;i<Math.min(people.size(),villageListScroll+visible);i++) {
            Actor a=people.get(i);int by=y+104+(i-villageListScroll)*56;
            CityBuilding home=state.housingForAlly(a.name);
            SettlementPanelStyle.card(g,x,by,w,50);g.setColor(SettlementPanelStyle.INK);
            drawClippedString(g,a.name,x+12,by+20,w-24);
            g.setColor(home==null?SettlementPanelStyle.GOLD:SettlementPanelStyle.MUTED);
            drawClippedString(g,home==null?"Needs a cottage or longhouse":VillageManager.buildingLabel(home.style())+" / Tier "+state.world.playerVillageBuildingLevel(home),x+12,by+39,w-24);
        }
        drawScrollIndicator(g,x+w+4,y+104,h-104,people.size(),villageListScroll,visible);
    }

    private int drawPropOffsets(Graphics2D g,int x,int y,int w) {
        actionButton(g,x,y,w/2-3,24,state.villageGridSnap?"Grid snap: On":"Grid snap: Off",()->state.villageGridSnap=!state.villageGridSnap,new Color(35,39,54),SettlementPanelStyle.TEAL,true);
        actionButton(g,x+w/2+3,y,w/2-3,24,state.villagePlacementCollisions?"Collision: On":"Collision: Off",()->state.villagePlacementCollisions=!state.villagePlacementCollisions,new Color(35,39,54),SettlementPanelStyle.GOLD,true);
        y+=32;

        g.setColor(SettlementPanelStyle.MUTED);g.setFont(new Font("SansSerif",Font.PLAIN,11));
        drawClippedString(g,"Placement offset: "+state.villagePropOffsetX+", "+state.villagePropOffsetY+" px",x,y+13,w);
        String[] labels={"X -","X +","Y -","Y +","Reset"};int bw=(w-16)/5;
        for(int i=0;i<5;i++){final int choice=i;
            actionButton(g,x+i*(bw+4),y+21,bw,23,labels[i],()->{
                if(choice==0)state.villagePropOffsetX=Math.max(-24,state.villagePropOffsetX-4);
                if(choice==1)state.villagePropOffsetX=Math.min(24,state.villagePropOffsetX+4);
                if(choice==2)state.villagePropOffsetY=Math.max(-24,state.villagePropOffsetY-4);
                if(choice==3)state.villagePropOffsetY=Math.min(24,state.villagePropOffsetY+4);
                if(choice==4){state.villagePropOffsetX=0;state.villagePropOffsetY=0;}
            },new Color(35,39,54),SettlementPanelStyle.MUTED,true);
        }
        return y+44;
    }

    private int drawTerrainTools(Graphics2D g, int x, int y, int w) {
        String[] tools = {"paint", "raise", "lower", "level", "smooth", "restore"};
        int bw = (w-8)/3;
        for (int i=0;i<tools.length;i++) {
            String tool = tools[i];
            actionButton(g,x+(i%3)*(bw+4),y+(i/3)*27,bw,23,tool,
                    () -> { state.villageTerrainTool=tool; state.setVillageEditAction("place"); },
                    tool.equals(state.villageTerrainTool)?new Color(57,76,60):new Color(35,39,54),new Color(126,176,95),true);
        }
        actionButton(g,x,y+56,w/2-3,24,"Height brush " + (state.villageBrushRadius*2+1),
                () -> state.villageBrushRadius=(state.villageBrushRadius+1)%3,
                new Color(35,39,54),new Color(86,98,128),true);
        actionButton(g,x+w/2+3,y+56,w/2-3,24,"Level " + state.villageTargetHeight,
                () -> state.villageTargetHeight=state.villageTargetHeight>=4?0:state.villageTargetHeight+.25,
                new Color(35,39,54),new Color(86,98,128),true);
        return y+80;
    }

    private void drawManagedBuildings(Graphics2D g, int x, int y, int w, int h) {
        List<CityBuilding> buildings = state.world.playerVillageBuildings().stream()
                .filter(b -> "All".equals(buildingCategory) || buildingCategory.equals(VillageManager.buildingCategory(b.style())))
                .filter(b -> searchable(state.generatedBuildingName(b),VillageManager.buildingLabel(b.style()),state.assignedAllyForBuilding(b.key()))
                        .contains(normalizedVillageSearch())).toList();
        int rowH=218, visible=Math.max(1,h/rowH);
        villageListScroll=clamp(villageListScroll,0,Math.max(0,buildings.size()-visible));
        if (buildings.isEmpty()) {
            drawWrapped(g,"No matching buildings. Place a building using Build.",x,y+20,w,18,3);
            return;
        }
        for (int i=villageListScroll;i<Math.min(buildings.size(),villageListScroll+visible);i++) {
            CityBuilding b=buildings.get(i); int by=y+(i-villageListScroll)*rowH;
            int level=state.world.playerVillageBuildingLevel(b);
            VillageManager.VillageCost cost=VillageManager.upgradeCost(b.style(),level);
            SettlementPanelStyle.card(g,x,by,w,rowH-10);
            g.drawImage(assets.spriteFit(VillageManager.buildingLevelSprite(b.style(),level),62,66),x+10,by+8,null);
            g.setColor(SettlementPanelStyle.INK); g.setFont(new Font("SansSerif",Font.BOLD,15));
            drawClippedString(g,state.generatedBuildingName(b),x+82,by+23,w-96);
            g.setFont(new Font("SansSerif",Font.PLAIN,11));
            g.setColor(SettlementPanelStyle.GOLD);
            drawClippedString(g,VillageManager.tierLabel(level)+"  /  "+level+" of 6",x+82,by+40,w-96);
            SettlementPanelStyle.tiers(g,x+82,by+50,w-98,level);
            String worker=state.assignedAllyForBuilding(b.key());
            g.setColor(worker.isBlank()?SettlementPanelStyle.GOLD:SettlementPanelStyle.TEAL);
            drawClippedString(g,state.workersForBuilding(b.key()).size()+" / "+SettlementEconomy.slots(b.style(),level)+" workers | "+SettlementEconomy.housing(b.style(),level)+" beds",x+82,by+72,w-96);
            g.setColor(SettlementPanelStyle.INK);
            drawWrapped(g,VillageManager.upgradeBenefit(b.style(),level),x+12,by+94,w-24,13,2);
            g.setColor(cost==null||state.canAffordVillageCost(cost)?SettlementPanelStyle.MUTED:new Color(230,154,129));
            drawWrapped(g,cost==null?"Fully improved":villageCostDisplay(cost),x+12,by+122,w-24,12,2);
            g.setColor(SettlementPanelStyle.TEAL);
            drawWrapped(g,state.villageForecastLabel(b),x+12,by+156,w-24,13,2);
            actionButton(g,x+10,by+177,w/2-15,25,"Manage",() -> state.openBuildingAssignment(b),new Color(34,51,62),new Color(79,114,127),true);
            actionButton(g,x+w/2+5,by+177,w/2-15,25,"Upgrade",() -> state.upgradeManagedBuilding(b),new Color(44,82,68),SettlementPanelStyle.TEAL,cost!=null&&state.canAffordVillageCost(cost));
        }
        drawScrollIndicator(g,x+w+4,y,h,buildings.size(),villageListScroll,visible);
    }

    private int drawTownOverview(Graphics2D g, int x, int y, int w) {
        VillageManager.SettlementStage next=state.nextVillageStage();
        int h=next==null?130:228;
        SettlementPanelStyle.card(g,x,y,w,h);
        long staffed=state.world.playerVillageBuildings().stream()
                .filter(b -> !state.assignedAllyForBuilding(b.key()).isBlank()).count();
        SettlementPanelStyle.metric(g,"Buildings",Integer.toString(state.villageBuildingCount()),x+14,y+25);
        SettlementPanelStyle.metric(g,"Staffed",Long.toString(staffed),x+w/3+8,y+25);
        SettlementPanelStyle.metric(g,"Stores",state.villageStorageUsed()+"/"+state.villageStorageCapacity(),x+w*2/3,y+25);
        g.setFont(new Font("SansSerif",Font.BOLD,11));g.setColor(SettlementPanelStyle.GOLD);
        drawClippedString(g,next==null?"METROPOLIS - settlement complete":"NEXT: "+next.title(),x+14,y+66,w-28);
        if(next!=null) {
            int pw=(w-42)/2;
            SettlementPanelStyle.progress(g,"Level",state.player.level,next.requiredPlayerLevel(),x+14,y+86,pw);
            SettlementPanelStyle.progress(g,"Buildings",state.villageBuildingCount(),next.requiredBuildings(),x+28+pw,y+86,pw);
            SettlementPanelStyle.progress(g,"Allies",state.villageAllies.size(),next.requiredAllies(),x+14,y+114,pw);
            SettlementPanelStyle.progress(g,"Quests",state.completedQuestCount(),next.requiredCompletedQuests(),x+28+pw,y+114,pw);
            SettlementPanelStyle.progress(g,"Stores",state.villageStorageUsed(),next.requiredStorageUsed(),x+14,y+142,pw);
            SettlementPanelStyle.progress(g,"Ground",state.villageDevelopedTileCount(),next.requiredDevelopedTiles(),x+28+pw,y+142,pw);
        }
        g.setColor(SettlementPanelStyle.TEAL);g.setFont(new Font("SansSerif",Font.PLAIN,11));
        drawClippedString(g,"Next day: "+state.villageForecast(null).gold()+"g (with current staff)",x+14,y+h-36,w-28);
        drawClippedString(g,"Last: "+state.villageLastProduction,x+14,y+h-18,w-28);
        return y+h;
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
