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
    private int assignmentPage;
    private int cataloguePage;
    private String catalogueCategory="All";
    private String assignmentBuildingKey = "";

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
        int w=Math.min(1080,gameAreaWidth()-48),h=Math.min(800,viewHeight()-64);
        int x=gameAreaCenteredX(w),y=centeredY(h);
        g.setColor(new Color(5,9,13,190));g.fillRect(0,0,gameAreaWidth(),viewHeight());
        SettlementPanelStyle.card(g,x,y,w,h);
        g.setFont(new Font("Serif",Font.BOLD,32));g.setColor(SettlementPanelStyle.GOLD);
        g.drawString("Build Oathstead",x+28,y+45);
        g.setFont(new Font("SansSerif",Font.PLAIN,13));g.setColor(SettlementPanelStyle.MUTED);
        g.drawString("Choose a plan, then place it in your settlement.   Gold "+state.player.gold,x+28,y+70);
        String[] categories={"All","Housing","Production","Civic"};
        for(int i=0;i<4;i++){String category=categories[i];
            actionButton(g,x+28+i*126,y+88,116,30,category,()->{catalogueCategory=category;cataloguePage=0;},
                    category.equals(catalogueCategory)?new Color(44,82,68):new Color(25,35,44),SettlementPanelStyle.GOLD,true);
        }
        List<VillageManager.BuildingPlan> plans=VillageManager.buildingPlans().stream()
                .filter(p -> catalogueCategory.equals("All")||catalogueCategory.equals(VillageManager.buildingCategory(p.style()))).toList();
        int cols=w>=850?3:2,rows=Math.max(1,(h-192)/178),perPage=cols*rows;
        int pages=Math.max(1,(plans.size()+perPage-1)/perPage);cataloguePage=Math.min(cataloguePage,pages-1);
        int cw=(w-72)/cols;
        for(int i=cataloguePage*perPage;i<Math.min(plans.size(),(cataloguePage+1)*perPage);i++){
            var plan=plans.get(i);int n=i-cataloguePage*perPage,bx=x+28+(n%cols)*(cw+8),by=y+134+(n/cols)*178;
            SettlementPanelStyle.card(g,bx,by,cw,166);
            g.drawImage(assets.spriteFit(VillageManager.buildingLevelSprite(plan.style(),1),74,84),bx+10,by+8,null);
            g.setColor(SettlementPanelStyle.INK);g.setFont(new Font("Serif",Font.BOLD,19));
            drawClippedString(g,plan.label(),bx+92,by+28,cw-102);
            g.setFont(new Font("SansSerif",Font.PLAIN,12));g.setColor(SettlementPanelStyle.MUTED);
            int beds=SettlementEconomy.housing(plan.style(),1);
            drawWrapped(g,beds>0?beds+" beds. More beds with each tier.":"1 worker. More slots at tiers 3 and 5.",bx+92,by+49,cw-102,15,3);
            g.setColor(state.canAffordVillageCost(plan.cost())?SettlementPanelStyle.GOLD:new Color(224,151,126));
            drawWrapped(g,VillageManager.costLabel(plan.cost()),bx+12,by+108,cw-24,14,2);
            actionButton(g,bx+12,by+134,cw-24,24,"Select "+plan.label(),()->{state.selectVillageBuildingStyle(plan.style());state.villageCatalogOpen=false;},new Color(44,82,68),SettlementPanelStyle.TEAL,true);
        }
        actionButton(g,x+28,y+h-44,100,28,"Previous",()->cataloguePage--,new Color(25,35,44),SettlementPanelStyle.MUTED,cataloguePage>0);
        actionButton(g,x+140,y+h-44,100,28,"Next",()->cataloguePage++,new Color(25,35,44),SettlementPanelStyle.MUTED,cataloguePage+1<pages);
        actionButton(g,x+w-142,y+h-44,114,28,"Back to town",()->state.villageCatalogOpen=false,new Color(44,60,62),SettlementPanelStyle.GOLD,true);
    }

    public void drawBuildingAssignment(Graphics2D g) {
        CityBuilding building = state.activeVillageBuilding;
        int panelW = Math.min(900, gameAreaWidth()-40);
        int panelH = Math.min(740, viewHeight()-80);
        int x = gameAreaCenteredX(panelW);
        int y = Math.max(54, (viewHeight() - panelH) / 2);
        drawOverlayBase(g, x, y, panelW, panelH);
        String key=building==null?"":building.key();
        if(!key.equals(assignmentBuildingKey)){assignmentBuildingKey=key;assignmentPage=0;}
        int level=state.world.playerVillageBuildingLevel(building);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(SettlementPanelStyle.GOLD);
        g.drawString("OATHSTEAD  /  BUILDING MANAGEMENT",x+32,y+26);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.setColor(new Color(244, 239, 220));
        String title = building == null ? "Building Assignment" : state.generatedBuildingName(building);
        drawClippedString(g, title, x + 32, y + 57, panelW - 260);
        g.setFont(new Font("SansSerif",Font.PLAIN,12));g.setColor(SettlementPanelStyle.GOLD);
        g.drawString(VillageManager.tierLabel(level)+"  /  "+level+" of 6",x+panelW-225,y+38);
        SettlementPanelStyle.tiers(g,x+panelW-225,y+50,190,level);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(198, 202, 211));
        boolean housing=building!=null && SettlementEconomy.housing(building.style(),level)>0;
        String assigned = building == null ? "" : state.assignedAllyForBuilding(building.key());
        String assignment = assigned.isBlank() ? "Unassigned" : "Assigned: " + assigned;
        drawClippedString(g,(building==null?"":"Type: "+VillageManager.buildingLabel(building.style())+" | ")+assignment+(building==null?"":" | "+state.workersForBuilding(building.key()).size()+"/"+SettlementEconomy.slots(building.style(),level)+" staff | "+SettlementEconomy.housing(building.style(),level)+" beds"),x+32,y+81,panelW-64);
        SettlementPanelStyle.card(g,x+24,y+96,panelW-48,150);
        if (building != null && building.key().startsWith("player_")) {
            g.drawImage(assets.spriteFit(VillageManager.buildingLevelSprite(building.style(),level),112,120),x+36,y+108,null);
            int infoX=x+166, infoW=panelW-360;
            g.setColor(SettlementPanelStyle.INK);g.setFont(new Font("SansSerif",Font.BOLD,13));
            g.drawString(level>=6?"MASTERWORK":"NEXT IMPROVEMENT",infoX,y+119);
            g.setFont(new Font("SansSerif",Font.PLAIN,12));g.setColor(SettlementPanelStyle.MUTED);
            drawWrapped(g,VillageManager.tierImprovement(Math.min(6,level+1)),infoX,y+140,infoW,15,2);
            g.setColor(SettlementPanelStyle.TEAL);
            drawWrapped(g,VillageManager.upgradeBenefit(building.style(),level),infoX,y+175,infoW,15,2);
            g.setColor(SettlementPanelStyle.MUTED);
            drawClippedString(g,state.buildingInteriorSummary(building),infoX,y+225,panelW-215);
            VillageManager.VillageCost cost=VillageManager.upgradeCost(building.style(),level);
            if(cost!=null) {
                drawWrapped(g,villageCostDisplay(cost),x+panelW-182,y+127,145,15,4);
                actionButton(g,x+panelW-182,y+191,145,28,"Improve to "+(level+1),
                        () -> state.upgradeManagedBuilding(building),new Color(44,82,68),SettlementPanelStyle.TEAL,state.canAffordVillageCost(cost));
            }
        }

        boolean compact=panelH<650;
        if(building!=null && !compact) {
            int tw=(panelW-64)/6;
            for(int tier=1;tier<=6;tier++) {
                int tx=x+32+(tier-1)*tw;
                g.setColor(tier==level?new Color(43,78,69):new Color(24,34,43));
                g.fillRoundRect(tx,y+257,tw-6,79,8,8);
                g.drawImage(assets.spriteFit(VillageManager.buildingLevelSprite(building.style(),tier),tw-18,57),tx+6,y+261,null);
                g.setColor(tier==level?SettlementPanelStyle.TEAL:SettlementPanelStyle.MUTED);
                g.setFont(new Font("SansSerif",Font.PLAIN,10));
                drawClippedString(g,tier+"  "+VillageManager.tierLabel(tier),tx+8,y+328,tw-18);
            }
        }
        int listX = x + 32;
        int listY = y + (compact?279:367);
        g.setColor(SettlementPanelStyle.GOLD);g.setFont(new Font("SansSerif",Font.BOLD,12));
        g.drawString(housing?"RESIDENTS - beds assigned automatically":"STAFFING",listX,listY-14);
        int rowH = 66;
        List<Actor> workers = state.stationedAllies();
        if(housing) workers=workers.stream().filter(a -> {
            CityBuilding home=state.housingForAlly(a.name);
            return home!=null && home.key().equals(building.key());
        }).toList();
        if (workers.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(176, 182, 196));
            drawWrapped(g, "No companions are set to city attendance. Open Village > Workers and station someone first.", listX, listY + 24, panelW - 64, 18, 3);
        }
        int maxRows = Math.max(1, Math.min(4,(panelH-(compact?377:465))/rowH));
        int pages=Math.max(1,(workers.size()+maxRows-1)/maxRows);
        assignmentPage=Math.max(0,Math.min(assignmentPage,pages-1));
        for (int i = assignmentPage*maxRows; i < Math.min((assignmentPage+1)*maxRows, workers.size()); i++) {
            Actor ally = workers.get(i);
            int rowY = listY + (i-assignmentPage*maxRows) * rowH;
            boolean selected = building != null && state.workersForBuilding(building.key()).contains(ally.name);
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
            String detail = state.housingForAlly(ally.name)==null?"Needs housing before working":professionSummary(ally);
            if(building!=null && state.housingForAlly(ally.name)!=null) {
                var output=SettlementEconomy.output(building.style());
                int skill=SettlementEconomy.skill(ally,output),tools=state.buildingToolScore(building);
                detail="Skill "+skill+" | Tools "+tools+" | "+SettlementEconomy.amount(output,level,skill,tools)+" / cycle | Rare "+Math.round(100*SettlementEconomy.rareChance(level,skill,tools))+"%";
            }
            if (!otherBuilding.isBlank() && (building == null || !otherBuilding.equals(building.key()))) {
                CityBuilding other = state.playerVillageBuildingByKey(otherBuilding);
                detail = "At " + (other == null ? "another building" : VillageManager.buildingLabel(other.style())) + " | " + detail;
            }
            drawClippedString(g, shortText(detail, 84), listX + 60, rowY + 40, panelW - 260);
            String allyName = ally.name;
            actionButton(g, listX + panelW - 190, rowY + 15, 104, 28, housing?"Resident":selected ? "Remove" : "Assign",
                    () -> {if(selected)state.unassignVillageWorker(allyName);else state.assignAllyToActiveBuilding(allyName);},
                    selected ? new Color(57, 76, 60) : new Color(69, 62, 88),
                    selected ? new Color(126, 176, 95) : new Color(125, 107, 166), building != null && !housing);
        }
        if (workers.size() > maxRows) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            int py=y+panelH-91;
            actionButton(g,x+panelW-208,py,72,24,"Previous",() -> assignmentPage--,new Color(34,51,62),SettlementPanelStyle.MUTED,assignmentPage>0);
            actionButton(g,x+panelW-124,py,72,24,"Next",() -> assignmentPage++,new Color(34,51,62),SettlementPanelStyle.MUTED,assignmentPage<pages-1);
            g.drawString("Staff page "+(assignmentPage+1)+" / "+pages,listX,py+17);
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
        return VillageManager.buildingLevelSprite(plan.style(), 1);
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
