package com.alderfall.game.ui;

import com.alderfall.game.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

public final class VillageOverlayRenderer {
    private final AssetStore assets;
    private final GameState state;
    private final Effects effects;

    public VillageOverlayRenderer(AssetStore assets, GameState state, Effects effects) {
        this.assets = assets;
        this.state = state;
        this.effects = effects;
    }

    private int gameAreaWidth() {
        return effects.gameAreaWidth();
    }

    private int viewHeight() {
        return effects.viewHeight();
    }

    private int villagePanelTop() {
        return effects.villagePanelTop();
    }

    private int gameAreaCenteredX(int width) {
        return effects.gameAreaCenteredX(width);
    }

    private int centeredY(int height) {
        return effects.centeredY(height);
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

    private void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines) {
        effects.drawWrapped(g, text, x, y, width, lineHeight, maxLines);
    }

    private void drawCenteredIn(Graphics2D g, String text, int x, int y, int width) {
        effects.drawCenteredIn(g, text, x, y, width);
    }

    private void addButton(Rectangle bounds, String label, Runnable action) {
        effects.addButton(bounds, label, action);
    }

    private void addTooltip(Rectangle bounds, String title, String body) {
        effects.addTooltip(bounds, title, body);
    }

    private String professionSummary(Actor actor) {
        return effects.professionSummary(actor);
    }

    private String shortText(String text, int maxChars) {
        return effects.shortText(text, maxChars);
    }

    public void drawVillageManager(Graphics2D g) {
        int x = 24;
        int y = villagePanelTop();
        int w = gameAreaWidth() - 48;
        int h = viewHeight() - y - 24;
        drawOverlayBase(g, x, y, w, h);

        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Oathstead Camp", x + 24, y + 38);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(198, 202, 211));
        g.drawString((state.isManagedVillageInterior() ? "Interior layout" : "Village layout")
                + " | Storage " + state.villageStorageUsed() + "/" + state.villageStorageCapacity()
                + " | Gold " + state.player.gold, x + 24, y + 62);

        String[] tabs = {"Build", "Tiles", "Props", "Inside", "Workers"};
        for (int i = 0; i < tabs.length; i++) {
            int tab = i;
            boolean selected = state.villageTab == i;
            actionButton(g, x + 214 + i * 88, y + 24, 78, 30, tabs[i], () -> state.setVillageTab(tab),
                    selected ? new Color(57, 76, 60) : new Color(35, 39, 54),
                    selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
        }

        actionButton(g, x + w - 434, y + 24, 92, 30, "Place", () -> state.setVillageEditAction("place"),
                "place".equals(state.villageEditAction) ? new Color(57, 76, 60) : new Color(35, 39, 54),
                new Color(126, 176, 95), true);
        actionButton(g, x + w - 334, y + 24, 92, 30, "Move", () -> state.setVillageEditAction("move"),
                "move".equals(state.villageEditAction) ? new Color(71, 63, 42) : new Color(35, 39, 54),
                new Color(169, 137, 74), true);
        actionButton(g, x + w - 234, y + 24, 92, 30, "Upgrade", () -> state.setVillageEditAction("upgrade"),
                "upgrade".equals(state.villageEditAction) ? new Color(69, 62, 88) : new Color(35, 39, 54),
                new Color(125, 107, 166), true);
        actionButton(g, x + w - 134, y + 24, 92, 30, "Delete", () -> state.setVillageEditAction("delete"),
                "delete".equals(state.villageEditAction) ? new Color(84, 50, 50) : new Color(35, 39, 54),
                new Color(149, 96, 88), true);

        if (state.villageTab == 0) {
            drawVillageBuildingTools(g, x + 24, y + 92, w - 48);
        } else if (state.villageTab == 1) {
            drawVillageTileTools(g, x + 24, y + 92, w - 48);
        } else if (state.villageTab == 2) {
            drawVillageAssetTools(g, x + 24, y + 92, w - 48);
        } else if (state.villageTab == 3) {
            drawVillageInteriorTools(g, x + 24, y + 92, w - 48);
        } else {
            drawVillageAllyTools(g, x + 24, y + 88, w - 48);
        }

        actionButton(g, x + w - 118, y + h - 42, 82, 28, "Close", state::toggleVillage,
                new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    public void drawBuildingAssignment(Graphics2D g) {
        CityBuilding building = state.activeVillageBuilding;
        int panelW = 760;
        int panelH = 560;
        int x = gameAreaCenteredX(panelW);
        int y = Math.max(54, (viewHeight() - panelH) / 2);
        drawOverlayBase(g, x, y, panelW, panelH);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.setColor(new Color(244, 239, 220));
        String title = building == null ? "Building Assignment" : VillageManager.buildingLabel(building.style());
        g.drawString(title, x + 32, y + 46);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(198, 202, 211));
        VillageManager.WorkerRole buildingRole = building == null
                ? VillageManager.workerRole("idle")
                : VillageManager.workerRole(VillageManager.buildingPlan(building.style()).workerRole());
        String assigned = building == null ? "" : state.assignedAllyForBuilding(building.key());
        String assignment = assigned.isBlank() ? "Unassigned" : "Assigned: " + assigned;
        g.drawString(assignment + " | Work: " + buildingRole.label(), x + 32, y + 74);
        drawWrapped(g, buildingRole.description(), x + 32, y + 94, panelW - 64, 17, 2);
        if (building != null && building.key().startsWith("player_")) {
            g.setColor(new Color(151, 177, 112));
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            drawClippedString(g, state.buildingInteriorSummary(building), x + 32, y + 132, panelW - 64);
        }

        int listX = x + 32;
        int listY = y + 156;
        int rowH = 66;
        List<Actor> workers = state.stationedAllies();
        if (workers.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(176, 182, 196));
            drawWrapped(g, "No companions are set to city attendance. Open Village > Workers and station someone first.", listX, listY + 24, panelW - 64, 18, 3);
        }
        int maxRows = 5;
        for (int i = 0; i < Math.min(maxRows, workers.size()); i++) {
            Actor ally = workers.get(i);
            int rowY = listY + i * rowH;
            boolean selected = building != null && ally.name.equals(state.assignedAllyForBuilding(building.key()));
            String otherBuilding = state.buildingAssignmentForAlly(ally.name);
            g.setColor(new Color(28, 32, 43, 235));
            g.fillRoundRect(listX, rowY, panelW - 64, rowH - 10, 8, 8);
            g.setColor(selected ? new Color(126, 176, 95) : new Color(86, 98, 128));
            g.drawRoundRect(listX, rowY, panelW - 64, rowH - 10, 8, 8);
            g.drawImage(assets.spriteFit(ally.worldSprite, 38, 48), listX + 10, rowY + 4, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(235, 236, 240));
            drawClippedString(g, ally.name, listX + 60, rowY + 20, panelW - 260);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            String detail = professionSummary(ally);
            if (!otherBuilding.isBlank() && (building == null || !otherBuilding.equals(building.key()))) {
                CityBuilding other = state.playerVillageBuildingByKey(otherBuilding);
                detail = "At " + (other == null ? "another building" : VillageManager.buildingLabel(other.style())) + " | " + detail;
            }
            drawClippedString(g, shortText(detail, 84), listX + 60, rowY + 40, panelW - 260);
            String allyName = ally.name;
            actionButton(g, listX + panelW - 190, rowY + 15, 104, 28, selected ? "Assigned" : "Assign",
                    () -> state.assignAllyToActiveBuilding(allyName),
                    selected ? new Color(57, 76, 60) : new Color(69, 62, 88),
                    selected ? new Color(126, 176, 95) : new Color(125, 107, 166), building != null);
        }
        if (workers.size() > maxRows) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            g.drawString("Open Village > Workers for the full city-attendance roster.", listX, listY + maxRows * rowH + 8);
        }

        actionButton(g, x + 32, y + panelH - 54, 138, 34, "Enter Inside", state::enterActiveVillageBuilding,
                new Color(52, 79, 92), new Color(92, 140, 160), building != null);
        actionButton(g, x + 188, y + panelH - 54, 118, 34, "Open Shop", state::openActiveVillageBuildingShop,
                new Color(57, 76, 60), new Color(126, 176, 95), building != null && !assigned.isBlank());
        actionButton(g, x + panelW - 330, y + panelH - 54, 138, 34, "Clear", state::clearActiveBuildingAssignment,
                new Color(83, 61, 61), new Color(149, 96, 88), building != null && !assigned.isBlank());
        actionButton(g, x + panelW - 172, y + panelH - 54, 138, 34, "Close", state::closeOverlay,
                new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    private void drawVillageBuildingTools(Graphics2D g, int x, int y, int w) {
        int gap = 12;
        int idealCardW = 150;
        int perRow = Math.max(3, w / (idealCardW + gap));
        int cardW = Math.max(144, (w - gap * (perRow - 1)) / perRow);
        int cardH = 116;
        List<VillageManager.BuildingPlan> plans = VillageManager.buildingPlans();
        for (int i = 0; i < plans.size(); i++) {
            VillageManager.BuildingPlan plan = plans.get(i);
            int cardX = x + (i % perRow) * (cardW + gap);
            int cardY = y + (i / perRow) * (cardH + gap);
            drawVillageBuildingCard(g, plan, cardX, cardY, cardW, cardH);
        }
        int rows = Math.max(1, (plans.size() + perRow - 1) / perRow);
        int infoY = y + rows * (cardH + gap) + 14;
        VillageManager.BuildingPlan selected = VillageManager.buildingPlan(state.selectedVillageBuildingStyle);
        g.setColor(new Color(14, 17, 25, 190));
        g.fillRoundRect(x, infoY - 18, w, 68, 8, 8);
        g.setColor(new Color(82, 92, 116));
        g.drawRoundRect(x, infoY - 18, w, 68, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(246, 224, 151));
        drawClippedString(g, selected.label() + "  " + selected.width() + "x" + selected.depth()
                + "  " + villageCostDisplay(selected.cost()), x + 14, infoY + 2, w - 28);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(198, 202, 211));
        drawClippedString(g, selected.description(), x + 14, infoY + 23, w - 28);
        drawVillageStorageSummary(g, x + 14, infoY + 44, w - 28);
        drawVillagePlacementStatus(g, x, infoY + 78, w);
    }

    private void drawVillageBuildingCard(Graphics2D g, VillageManager.BuildingPlan plan, int x, int y, int w, int h) {
        boolean selected = plan.style().equals(state.selectedVillageBuildingStyle);
        boolean affordable = state.canAffordVillageCost(plan.cost());
        Rectangle bounds = new Rectangle(x, y, w, h);
        addButton(bounds, "village-build:" + plan.style(), () -> state.selectVillageBuildingStyle(plan.style()));

        Color fill = selected ? new Color(41, 61, 48, 242) : new Color(24, 29, 41, 235);
        Color border = selected ? new Color(126, 176, 95) : affordable ? new Color(86, 98, 128) : new Color(92, 74, 74);
        g.setColor(fill);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(border);
        g.drawRoundRect(x, y, w, h, 8, 8);

        g.setColor(new Color(9, 12, 18, 170));
        int previewX = x + (w - 62) / 2;
        g.fillRoundRect(previewX, y + 9, 62, 54, 7, 7);
        g.setColor(new Color(71, 80, 96, 170));
        g.drawRoundRect(previewX, y + 9, 62, 54, 7, 7);
        g.drawImage(assets.spriteFit(buildingPreviewSprite(plan), 56, 50), previewX + 3, y + 11, null);

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(238, 239, 244));
        drawCenteredIn(g, plan.label(), x + 8, y + 76, w - 16);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(affordable ? new Color(176, 182, 196) : new Color(222, 143, 132));
        drawCenteredIn(g, plan.width() + "x" + plan.depth() + " | " + villageCostDisplay(plan.cost()), x + 8, y + 93, w - 16);

        String utility = plan.storageCapacity() > 0
                ? "Storage +" + plan.storageCapacity()
                : plan.workerRole().isBlank() ? "Max Lv " + plan.maxLevel() : VillageManager.workerRole(plan.workerRole()).label();
        g.setColor(new Color(151, 177, 112));
        drawCenteredIn(g, utility, x + 8, y + 109, w - 16);

        if (!affordable) {
            g.setColor(new Color(8, 10, 14, 110));
            g.fillRoundRect(x, y, w, h, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(new Color(238, 190, 164));
            drawCenteredIn(g, "Need materials", x, y + h - 10, w);
        }
        addTooltip(bounds, plan.label(), plan.description() + " " + villageCostDisplay(plan.cost()) + ".");
    }

    public String buildingPreviewSprite(VillageManager.BuildingPlan plan) {
        if (plan.sprites().isEmpty()) {
            return "city_building_town_gabled";
        }
        return plan.sprites().get(0);
    }

    public String villageCostDisplay(VillageManager.VillageCost cost) {
        if (state.config.creativeBuildMode) {
            String planned = VillageManager.costLabel(cost);
            return "Free now" + ("Free".equals(planned) ? "" : " | Plan: " + planned);
        }
        return "Cost: " + VillageManager.costLabel(cost);
    }

    private void drawVillageTileTools(Graphics2D g, int x, int y, int w) {
        int buttonW = 118;
        int buttonH = 64;
        int gap = 14;
        List<VillageManager.TilePlan> tiles = VillageManager.tilePlans();
        for (int i = 0; i < tiles.size(); i++) {
            VillageManager.TilePlan option = tiles.get(i);
            boolean selected = option.tile() == state.selectedVillageTile;
            int buttonX = x + i * (buttonW + gap);
            actionButton(g, buttonX, y, buttonW, buttonH, "", () -> state.selectVillageTile(option.tile()),
                    selected ? new Color(57, 76, 60) : new Color(28, 32, 43),
                    selected ? new Color(126, 176, 95) : new Color(82, 92, 116), true);
            g.drawImage(assets.tile(option.tile(), 34), buttonX + 42, y + 6, null);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(new Color(222, 225, 232));
            drawCenteredIn(g, option.label(), buttonX, y + 54, buttonW);
            g.setColor(new Color(176, 182, 196));
            drawCenteredIn(g, VillageManager.costLabel(option.cost()), buttonX, y + 78, buttonW);
        }
        drawVillagePlacementStatus(g, x, y + buttonH + 42, w);
    }

    private void drawVillageAssetTools(Graphics2D g, int x, int y, int w) {
        int buttonW = 106;
        int buttonH = 66;
        int gap = 12;
        int perRow = Math.max(1, w / (buttonW + gap));
        List<String> categories = VillageManager.outdoorAssetCategories();
        int categoryW = 86;
        int categoryH = 28;
        int categoryPerRow = Math.max(1, w / (categoryW + gap));
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            boolean selected = category.equals(state.selectedVillagePropCategory);
            int buttonX = x + (i % categoryPerRow) * (categoryW + gap);
            int buttonY = y + (i / categoryPerRow) * (categoryH + 8);
            actionButton(g, buttonX, buttonY, categoryW, categoryH, category, () -> state.selectVillagePropCategory(category),
                    selected ? new Color(57, 76, 60) : new Color(28, 32, 43),
                    selected ? new Color(126, 176, 95) : new Color(82, 92, 116), true);
        }
        int categoryRows = Math.max(1, (categories.size() + categoryPerRow - 1) / categoryPerRow);
        int assetY = y + categoryRows * (categoryH + 8) + 10;
        List<VillageManager.PlaceableAsset> assetsList = VillageManager.outdoorAssets(state.selectedVillagePropCategory);
        for (int i = 0; i < assetsList.size(); i++) {
            VillageManager.PlaceableAsset option = assetsList.get(i);
            String asset = option.asset();
            boolean selected = asset.equals(state.selectedVillageAsset);
            int buttonX = x + (i % perRow) * (buttonW + gap);
            int buttonY = assetY + (i / perRow) * (buttonH + 20);
            actionButton(g, buttonX, buttonY, buttonW, buttonH, "", () -> state.selectVillageAsset(asset),
                    selected ? new Color(57, 76, 60) : new Color(28, 32, 43),
                    selected ? new Color(126, 176, 95) : new Color(82, 92, 116), true);
            g.drawImage(assets.spriteFit(asset, 42, 42), buttonX + 32, buttonY + 7, null);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(new Color(222, 225, 232));
            drawCenteredIn(g, assetLabel(option.label()), buttonX, buttonY + 58, buttonW);
        }
        int rows = Math.max(1, (assetsList.size() + perRow - 1) / perRow);
        drawVillageStorageSummary(g, x, assetY + rows * (buttonH + 20) + 10, w);
        drawVillagePlacementStatus(g, x, assetY + rows * (buttonH + 20) + 34, w);
    }

    private void drawVillageInteriorTools(Graphics2D g, int x, int y, int w) {
        int buttonW = 98;
        int buttonH = 64;
        int gap = 14;
        int perRow = Math.max(1, w / (buttonW + gap));
        List<VillageManager.PlaceableAsset> interiors = VillageManager.interiorAssets();
        for (int i = 0; i < interiors.size(); i++) {
            VillageManager.PlaceableAsset option = interiors.get(i);
            String asset = option.asset();
            boolean selected = asset.equals(state.selectedInteriorAsset);
            int buttonX = x + (i % perRow) * (buttonW + gap);
            int buttonY = y + (i / perRow) * (buttonH + 18);
            actionButton(g, buttonX, buttonY, buttonW, buttonH, "", () -> state.selectInteriorAsset(asset),
                    selected ? new Color(57, 76, 60) : new Color(28, 32, 43),
                    selected ? new Color(126, 176, 95) : new Color(82, 92, 116), true);
            g.drawImage(assets.spriteFit(asset, 42, 42), buttonX + 28, buttonY + 8, null);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(new Color(222, 225, 232));
            drawCenteredIn(g, assetLabel(option.label()), buttonX, buttonY + 58, buttonW);
        }
        int rows = Math.max(1, (interiors.size() + perRow - 1) / perRow);
        drawVillagePlacementStatus(g, x, y + rows * (buttonH + 18) + 14, w);
    }

    private void drawVillageAllyTools(Graphics2D g, int x, int y, int w) {
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(246, 224, 151));
        g.drawString("Traveling", x, y);
        g.drawString("City Attendance", x + w / 2, y);
        drawVillageStorageSummary(g, x, y + 232, w);
        List<Actor> stationable = state.allies.stream()
                .filter(ally -> !state.villageAllies.contains(ally.name))
                .toList();
        drawVillageAllyColumn(g, stationable, x, y + 18, w / 2 - 18, true);
        drawVillageAllyColumn(g, state.stationedAllies(), x + w / 2, y + 18, w / 2 - 18, false);
    }

    private void drawVillageAllyColumn(Graphics2D g, List<Actor> allies, int x, int y, int w, boolean station) {
        if (allies.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(176, 182, 196));
            g.drawString("No allies here.", x, y + 24);
            return;
        }
        int rowY = y;
        for (Actor ally : allies) {
            g.setColor(new Color(28, 32, 43, 235));
            g.fillRoundRect(x, rowY, w, 46, 8, 8);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(x, rowY, w, 46, 8, 8);
            g.drawImage(assets.spriteFit(ally.worldSprite, 34, 42), x + 8, rowY + 2, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(new Color(235, 236, 240));
            g.drawString(ally.name, x + 50, rowY + 19);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            String role = station ? ally.className + "  Lv " + ally.level
                    : ally.className + "  Lv " + ally.level + " | " + VillageManager.workerRole(state.villageRoleFor(ally.name)).label();
            g.drawString(role, x + 50, rowY + 35);
            String name = ally.name;
            actionButton(g, x + w - 94, rowY + 9, 78, 28, station ? "Station" : "Recall",
                    () -> {
                        if (station) {
                            state.stationAlly(name);
                        } else {
                            state.recallAlly(name);
                        }
                    },
                    station ? new Color(69, 62, 88) : new Color(57, 76, 60),
                    station ? new Color(125, 107, 166) : new Color(126, 176, 95), true);
            if (!station) {
                drawWorkerRoleButtons(g, name, x + 50, rowY + 50, w - 58);
                rowY += 90;
            } else {
                rowY += 54;
            }
            if (rowY > villagePanelTop() + 222) {
                break;
            }
        }
    }

    private void drawWorkerRoleButtons(Graphics2D g, String allyName, int x, int y, int w) {
        String[][] roles = {
                {"idle", "Idle"},
                {"farmer", "Farm"},
                {"forester", "Wood"},
                {"miner", "Mine"},
                {"hunter", "Hunt"},
                {"hauler", "Haul"},
                {"builder", "Build"}
        };
        int buttonW = Math.max(38, Math.min(52, (w - 6 * 6) / roles.length));
        for (int i = 0; i < roles.length; i++) {
            String roleId = roles[i][0];
            boolean selected = roleId.equals(state.villageRoleFor(allyName));
            actionButton(g, x + i * (buttonW + 6), y, buttonW, 22, roles[i][1],
                    () -> state.assignVillageRole(allyName, roleId),
                    selected ? new Color(57, 76, 60) : new Color(35, 39, 54),
                    selected ? new Color(126, 176, 95) : new Color(86, 98, 128), true);
        }
    }

    private void drawVillageStorageSummary(Graphics2D g, int x, int y, int w) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(198, 202, 211));
        StringBuilder line = new StringBuilder("Stores: ");
        String[] resources = {
                "wood", "stone", "iron_ore", "coal", "flint", "plant_fiber", "herb_seed", "herb_leaf",
                "trail_rations", "garden_vegetables", "wild_meat", "raw_fish", "skin", "bone", "horn",
                "seashell", "shell_lure", "ember_shard"
        };
        boolean any = false;
        for (String resource : resources) {
            int amount = state.villageStoredAmount(resource);
            if (amount <= 0) {
                continue;
            }
            if (any) {
                line.append(" | ");
            }
            line.append(GameData.itemName(resource)).append(" ").append(amount);
            any = true;
        }
        if (!any) {
            line.append("empty");
        }
        drawClippedString(g, line.toString(), x, y, w);
    }

    private void drawVillagePlacementStatus(Graphics2D g, int x, int y, int w) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(198, 202, 211));
        String target = state.villageTab == 3 ? "interior tiles" : "village tiles";
        String mode = state.villageEditMode ? state.villageEditAction : "place";
        drawClippedString(g, "Mode: " + mode + "   Target: " + target + "   " + state.status, x, y, w);
        if (state.pendingVillageMoveSource != null) {
            g.setColor(new Color(246, 224, 151));
            drawClippedString(g, "Moving from " + state.pendingVillageMoveSource.x() + ", "
                    + state.pendingVillageMoveSource.y(), x, y + 22, w);
        }
    }

    private String assetLabel(String asset) {
        String cleaned = asset.replace("city_prop_", "").replace("interior_", "").replace("deco_", "").replace('_', ' ');
        if (cleaned.length() > 15) {
            cleaned = cleaned.substring(0, 14) + ".";
        }
        return cleaned;
    }


    public interface Effects {
        int gameAreaWidth();

        int viewHeight();

        int villagePanelTop();

        int gameAreaCenteredX(int width);

        int centeredY(int height);

        public void drawOverlayBase(Graphics2D g, int x, int y, int w, int h);

        void actionButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action, Color fill, Color border, boolean enabled);

        public void drawClippedString(Graphics2D g, String text, int x, int y, int maxWidth);

        public void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines);

        public void drawCenteredIn(Graphics2D g, String text, int x, int y, int width);

        void addButton(Rectangle bounds, String label, Runnable action);

        void addTooltip(Rectangle bounds, String title, String body);

        String professionSummary(Actor actor);

        String shortText(String text, int maxChars);
    }
}
