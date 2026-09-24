package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;

public final class VillageEditorToolsTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        GameState s=new GameState(GameConfig.load(Path.of(".")));s.chooseClass("Knight");
        while(s.mode==GameMode.STORY_INTRO)s.advanceStoryIntro();
        s.currentMapId=WorldMap.PLAYER_VILLAGE_ID;s.mode=GameMode.VILLAGE;s.config.creativeBuildMode=true;
        s.setVillageTab(2);s.selectedVillageAsset="city_prop_refresh_barrel";
        int x=0,y=0;
        outer:for(int yy=3;yy<25;yy++)for(int xx=3;xx<25;xx++)if(s.world.canPlacePlayerVillageProp(xx,yy,null)){x=xx;y=yy;break outer;}
        check(x>0,"Placement fixture");
        s.villageGridSnap=false;s.placeVillageAt(x+.25,y+.75);
        WorldProp first=s.world.playerVillagePropAt(x,y);
        check(first!=null&&first.offsetX()==-12&&first.offsetY()==-12,"Off-grid pointer precision");
        int before=s.world.playerVillageProps().size();s.placeVillageAt(x+.75,y+.5);
        check(s.world.playerVillageProps().size()==before,"Collision on rejects overlap");
        s.villagePlacementCollisions=false;s.placeVillageAt(x+.75,y+.5);
        check(s.world.playerVillageProps().size()==before+1,"Collision off allows shared tile");
        WorldProp top=s.world.playerVillagePropAt(x,y);
        WorldProp nudged=s.nudgeVillageEditorProp(top,40,0);
        check(nudged.x()==x+1,"Off-grid nudge crosses tile boundary");
        check(s.world.playerVillageProps().contains(first),"Nudge preserves other stacked prop");
        s.selectVillagePropForMove(nudged);s.placeVillageAt(x+.6,y+.6);
        WorldProp moved=s.world.playerVillagePropAt(x,y);
        check(moved.offsetX()==5&&moved.offsetY()==-19,"Move uses exact pointer position");
        s.removeVillageEditorProp(moved);check(s.world.playerVillageProps().contains(first),"Remove affects one layer");
        check(!s.world.addPlayerVillageProp(-1,y,"city_prop_refresh_barrel",40,0,0,false),"Boundary remains protected");
        CityBuilding house=SettlementEconomyTest.place(s,"house");
        String interior=s.world.ensureHouseInterior(WorldMap.PLAYER_VILLAGE_ID,house.x1(),house.y1(),house.x1(),house.y2()+1);
        int ix=0,iy=0;String asset="interior_table";
        outer:for(int yy=2;yy<s.world.height(interior)-4;yy++)for(int xx=2;xx<s.world.width(interior)-4;xx++)
            if(s.world.addPlayerInteriorProp(interior,xx,yy,asset,48,0,0,false)){ix=xx;iy=yy;break outer;}
        check(ix>0,"Interior placement fixture");
        check(s.world.addPlayerInteriorProp(interior,ix,iy,asset,48,12,-8,false),"Interior stacking");
        Path root=Files.createTempDirectory(Files.createDirectories(Path.of("temp/settlement-identity")),"editor-save-");
        SaveSystem saves=new SaveSystem(root);saves.save(s,"Village editor tools");String id=s.currentSaveId;
        check(saves.load(s,id),"Saved editor placements");
        s.world.ensureHouseInterior(WorldMap.PLAYER_VILLAGE_ID,house.x1(),house.y1(),house.x1(),house.y2()+1);
        final int px=ix,py=iy;
        check(s.world.area(interior).props.stream().filter(p->p.x()==px&&p.y()==py&&p.asset().equals(asset)).count()==2,"Both interior layers render after reload");
        WorldProp upper=s.world.playerInteriorPropAt(interior,ix,iy);
        check(s.world.removePlayerInteriorProp(interior,upper),"Remove interior layer");
        check(s.world.tileAt(interior,ix,iy)=='k',"Remaining furniture retains collision");
        SwingUtilities.invokeAndWait(()->{
            GamePanel panel=new GamePanel(Path.of("."),false);
            try{
                panel.setSize(GameConfig.WIDTH,GameConfig.HEIGHT);
                var state=panel.editorState();state.currentMapId=WorldMap.PLAYER_VILLAGE_ID;state.mode=GameMode.VILLAGE;state.setVillageTab(2);
                BufferedImage image=new BufferedImage(GameConfig.WIDTH,GameConfig.HEIGHT,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.paint(g);
                var open=GamePanel.class.getDeclaredMethod("openContextMenu",Point.class);open.setAccessible(true);
                check((boolean)open.invoke(panel,new Point(420,360)),"Right-click menu opens in village");panel.paint(g);
                var field=GamePanel.class.getDeclaredField("buttons");field.setAccessible(true);
                @SuppressWarnings("unchecked") List<UiButton> buttons=(List<UiButton>)field.get(panel);
                buttons.stream().filter(v->v.label().equals("Grid snap: On")).reduce((a,b)->b).orElseThrow().action().run();
                check(!state.villageGridSnap,"Context menu grid toggle");
                buttons.stream().filter(v->v.label().equals("Collision: On")).reduce((a,b)->b).orElseThrow().action().run();
                check(!state.villagePlacementCollisions,"Context menu collision toggle");panel.paint(g);
                javax.imageio.ImageIO.write(image,"png",Path.of("temp/settlement-identity/editor-menu.png").toFile());g.dispose();
            }catch(Exception e){throw new RuntimeException(e);}finally{panel.shutdown();}
        });
        System.out.println("VillageEditorToolsTest passed: free placement, overlap, exact moves/removal, save/load and context menu");
    }
}
