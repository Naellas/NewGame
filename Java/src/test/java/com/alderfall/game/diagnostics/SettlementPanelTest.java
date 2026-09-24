package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Exercises actual sidebar actions and exports current game renders for review. */
public final class SettlementPanelTest {
    @SuppressWarnings("unchecked")
    private static List<UiButton> buttons(GamePanel panel) throws Exception {
        Field f=GamePanel.class.getDeclaredField("buttons");f.setAccessible(true);
        return (List<UiButton>)f.get(panel);
    }
    private static void click(GamePanel panel,String label) throws Exception {
        buttons(panel).stream().filter(b->b.label().equals(label)).findFirst().orElseThrow().action().run();
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(()-> {
            GamePanel panel=new GamePanel(Path.of("").toAbsolutePath());
            try {
                Field timer=GamePanel.class.getDeclaredField("timer");timer.setAccessible(true);((Timer)timer.get(panel)).stop();
                panel.setSize(GameConfig.WIDTH,GameConfig.HEIGHT);
                GameState state=panel.state;state.chooseClass("Knight");
                while(state.mode==GameMode.STORY_INTRO)state.advanceStoryIntro();
                state.currentMapId=WorldMap.PLAYER_VILLAGE_ID;state.mode=GameMode.VILLAGE;
                state.config.creativeBuildMode=true;state.config.showMapEditorButton=false;
                state.playerX=14;state.playerY=17;
                state.worldTick=GameState.TICKS_PER_GAME_DAY/2;
                CityBuilding building=state.world.placePlayerVillageBuilding("house",5,5);
                if(building==null)throw new AssertionError("Preview building placement");
                BufferedImage image=new BufferedImage(GameConfig.WIDTH,GameConfig.HEIGHT,BufferedImage.TYPE_INT_RGB);
                var g=image.createGraphics();Path out=Path.of("temp/settlement-identity");Files.createDirectories(out);
                panel.paint(g);click(panel,"Manage");panel.paint(g);
                click(panel,"Upgrade");
                if(state.world.playerVillageBuildingLevel(building)!=2)throw new AssertionError("Upgrade action");
                panel.paint(g);ImageIO.write(image,"png",out.resolve("manage.png").toFile());
                click(panel,"Civic");panel.paint(g);
                if(buttons(panel).stream().anyMatch(b->b.label().equals("Upgrade")))throw new AssertionError("Category filter");
                buttons(panel).stream().filter(b->b.label().equals("Housing")).reduce((a,b)->b).orElseThrow().action().run();panel.paint(g);
                // The first Manage button is the tab; invoke the building card's later button.
                buttons(panel).stream().filter(b->b.label().equals("Manage")).reduce((a,b)->b).orElseThrow().action().run();
                if(state.mode!=GameMode.BUILDING_ASSIGNMENT)throw new AssertionError("Building details");
                panel.paint(g);click(panel,"Improve to 3");
                if(state.world.playerVillageBuildingLevel(building)!=3)throw new AssertionError("Details upgrade");
                panel.paint(g);ImageIO.write(image,"png",out.resolve("building.png").toFile());
                click(panel,"Close");
                if(state.mode!=GameMode.VILLAGE || state.villageTab!=5)throw new AssertionError("Return to town management");
                state.setVillageTab(0);panel.paint(g);
                ImageIO.write(image,"png",out.resolve("build.png").toFile());
                click(panel,"Open building catalogue");panel.paint(g);
                ImageIO.write(image,"png",out.resolve("catalogue.png").toFile());
                click(panel,"Select Cottage");
                if(state.villageCatalogOpen || !"house".equals(state.selectedVillageBuildingStyle))throw new AssertionError("Catalogue selection");
                panel.paint(g);click(panel,"Housing");panel.paint(g);
                ImageIO.write(image,"png",out.resolve("housing.png").toFile());
                Actor worker=new Actor("Settlement artisan","npc_blacksmith","Knight",50,10,5,5);
                state.allies.add(worker);state.villageAllies.add(worker.name);
                CityBuilding workplace=SettlementEconomyTest.place(state,"shop");
                click(panel,"Workers");panel.paint(g);click(panel,"Set");
                if(!worker.name.equals(state.pendingWorkplaceAlly))throw new AssertionError("Set enters map selection");
                state.handleVillageWorldClick(workplace.x1(),workplace.y1());
                if(!state.workersForBuilding(workplace.key()).contains(worker.name))throw new AssertionError("Selected workplace");
                panel.paint(g);ImageIO.write(image,"png",out.resolve("workers.png").toFile());
                state.mode=GameMode.SETTLEMENT_BOARD;panel.paint(g);
                ImageIO.write(image,"png",out.resolve("board.png").toFile());
                state.mode=GameMode.VILLAGE;panel.paint(g);
                click(panel,"Terrain");panel.paint(g);click(panel,"raise");
                if(!state.villageTerrainTool.equals("raise"))throw new AssertionError("Raise button");
                click(panel,"Height brush 1");
                if(state.villageBrushRadius!=1)throw new AssertionError("Brush button");
                int before=state.world.playerVillageHeights().size();
                state.handleVillageWorldClick(24,7);
                if(state.world.playerVillageHeights().size()<=before)throw new AssertionError("Height brush edits world");
                panel.paint(g);ImageIO.write(image,"png",out.resolve("terrain.png").toFile());
                g.dispose();
            }catch(Exception e){throw new IllegalStateException(e);}finally{panel.shutdown();}
        });
        System.out.println("SettlementPanelTest passed: Manage upgrade and terrain controls rendered and clicked");
    }
}
