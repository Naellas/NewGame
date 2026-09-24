package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

/** Verify unchanged settlement terrain survives building, prop and material edits. */
public final class SettlementCacheTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static BufferedImage render(GamePanel panel){
        BufferedImage image=new BufferedImage(768,576,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();panel.renderEditorScene(g,0,0,32,24,24,true,false,true);g.dispose();return image;
    }
    public static void main(String[] args)throws Exception{
        GamePanel panel=new GamePanel(Path.of("."),false);
        try{
            GameState s=panel.editorState();s.currentMapId=WorldMap.PLAYER_VILLAGE_ID;
            s.world.setPlayerVillageStage(3);render(panel);long warm=panel.editorTerrainBuildCount();
            render(panel);check(panel.editorTerrainBuildCount()==warm,"Warm settlement reuses terrain");
            CityBuilding b=SettlementEconomyTest.place(s,"house");
            long start=System.nanoTime();render(panel);long changed=panel.editorTerrainBuildCount()-warm;
            check(changed<warm,"Building does not rebuild all visible chunks: "+changed+"/"+warm);
            System.out.println("Building: "+changed+"/"+warm+" chunks, "+(System.nanoTime()-start)/1_000_000+" ms");
            warm=panel.editorTerrainBuildCount();s.world.upgradePlayerVillageBuilding(b);render(panel);
            check(panel.editorTerrainBuildCount()==warm,"Tier sprite change reuses all terrain");
            check(s.world.addPlayerVillageProp(24,12,"city_prop_refresh_barrel",40,8,-4),"Prop fixture");
            render(panel);check(panel.editorTerrainBuildCount()==warm,"Ordinary props reuse all terrain");
            check(s.world.setPlayerVillageTile(23,8,'!'),"Terrain fixture");
            BufferedImage edited=render(panel);changed=panel.editorTerrainBuildCount()-warm;
            check(changed>0&&changed<warm,"Material edits remain local");
            GamePanel fresh=new GamePanel(Path.of("."),false);
            try{
                var f=fresh.editorState();f.currentMapId=s.currentMapId;f.world.setPlayerVillageStage(3);
                CityBuilding copy=f.world.placePlayerVillageBuilding("house",b.x1(),b.y1());f.world.upgradePlayerVillageBuilding(copy);
                f.world.addPlayerVillageProp(24,12,"city_prop_refresh_barrel",40,8,-4);
                f.world.setPlayerVillageTile(23,8,'!');
                BufferedImage expected=render(fresh);
                for(int y=0;y<edited.getHeight();y++)for(int x=0;x<edited.getWidth();x++)
                    check(edited.getRGB(x,y)==expected.getRGB(x,y),"Cache differs from fresh image at "+x+","+y);
            }finally{fresh.shutdown();}
            System.out.println("SettlementCacheTest passed: local rebuilds and fresh-render equality");
        }finally{panel.shutdown();}
    }
}
