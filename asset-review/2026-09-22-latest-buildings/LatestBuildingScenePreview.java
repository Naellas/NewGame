package com.alderfall.game;
import com.alderfall.game.camera.CameraController;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
public class LatestBuildingScenePreview {
 static Field field(String name)throws Exception{Field f=GamePanel.class.getDeclaredField(name);f.setAccessible(true);return f;}
 static void capture(GamePanel p,Path path)throws Exception{((CameraController)field("cameraController").get(p)).resetToPlayer();BufferedImage im=new BufferedImage(GameConfig.WIDTH,GameConfig.HEIGHT,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();p.paint(g);g.dispose();ImageIO.write(im,"png",path.toFile());}
 public static void main(String[] args)throws Exception{
  Path output=Path.of("../asset-review/2026-09-22-latest-buildings/scenes");Files.createDirectories(output);
  SwingUtilities.invokeAndWait(()->{GamePanel p=new GamePanel(Path.of("").toAbsolutePath().normalize());try{
   ((Timer)field("timer").get(p)).stop();GameState s=(GameState)field("state").get(p);s.chooseClass("Mage");while(s.mode==GameMode.STORY_INTRO)s.advanceStoryIntro();s.config.renderQuality="high";s.zoom=100;s.worldTick=GameState.TICKS_PER_GAME_DAY/2;p.setSize(GameConfig.WIDTH,GameConfig.HEIGHT);
   boolean found=false;for(var site:s.world.settlementSites()){for(var b:s.world.cityBuildings(site.id())){if(RegionalBuildingTypes.type(site.id(),b)!=RegionalBuildingTypes.Type.RESCUE_LODGE)continue;TilePoint door=s.world.cityBuildingDoorTiles(b).get(0);s.currentMapId=site.id();s.playerX=door.x();s.playerY=door.y()+3;s.status="Rescue lodge: repaired roof and preserved rescue equipment";capture(p,output.resolve("rescue-lodge.png"));found=true;break;}if(found)break;}
   s.currentMapId="city_sanctum";s.status="Sun-region gate cutout check";found=false;
   for(int y=0;y<s.world.height(s.currentMapId)&&!found;y++)for(int x=0;x<s.world.width(s.currentMapId);x++)if(s.world.tileAt(s.currentMapId,x,y)==Terrain.CITY_GATE){s.playerX=x;s.playerY=Math.min(y+2,s.world.height(s.currentMapId)-1);capture(p,output.resolve("sun-gate.png"));found=true;break;}
  }catch(Exception e){throw new RuntimeException(e);}finally{p.shutdown();}});
 }
}
