package com.alderfall.game;
import java.awt.*;
import java.awt.image.*;
import java.lang.reflect.*;
import java.nio.file.*;
import javax.imageio.*;
import javax.swing.*;
import com.alderfall.game.camera.CameraController;
public class RowHitboxReview {
 static Field field(String n) throws Exception { Field f=GamePanel.class.getDeclaredField(n); f.setAccessible(true); return f; }
 static Object call(GamePanel p,String n,Class<?>[] types,Object...args)throws Exception {Method m=GamePanel.class.getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(p,args);}
 public static void main(String[] args) throws Exception {
 SwingUtilities.invokeAndWait(() -> {
  GamePanel panel = new GamePanel(Path.of("").toAbsolutePath().normalize());
  try {
   ((Timer)field("timer").get(panel)).stop();
   GameState s=(GameState)field("state").get(panel);
   s.chooseClass("Mage"); while(s.mode==GameMode.STORY_INTRO)s.advanceStoryIntro();
   s.currentMapId="city_archive";
   CityBuilding selected=null;
   for(String mapId:java.util.List.of("city_archive","town_briarbridge","city_riverside","city_belltower","city_highwall","town_ironvale")) {
    for(CityBuilding candidate:s.world.cityBuildings(mapId)) {
     if(candidate.style().equals("row") && !BuildingGeometry.standalone(mapId,s.world.kind(mapId),candidate)) {
      s.currentMapId=mapId; selected=candidate; break;
     }
    }
    if(selected!=null)break;
   }
   CityBuilding b=java.util.Objects.requireNonNull(selected,"No modular row fixture");
   var base=s.world.buildingFootprints(s.currentMapId,b).get(0);
   s.playerX=(int)base.getCenterX(); s.playerY=(int)Math.floor(base.y-0.25);
   while(s.timeOfDayMinutes()!=12*60)s.worldTick++;
   call(panel,"syncPlayerAnimationToState",new Class<?>[]{});
   field("freePlayerMapId").set(panel,s.currentMapId);field("freePlayerX").setDouble(panel,base.getCenterX());field("freePlayerY").setDouble(panel,base.y-0.25);
   ((CameraController)field("cameraController").get(panel)).resetToPlayer();
   panel.setSize(1400,1000);
   Files.createDirectories(Path.of("../asset-review/building-hitboxes"));
   for(int zoom:new int[]{75,100,150}){
    s.zoom=zoom;
    BufferedImage im=new BufferedImage(1400,1000,BufferedImage.TYPE_INT_RGB);
    Graphics2D g=im.createGraphics();panel.paint(g);g.dispose();
    Rectangle bounds=(Rectangle)call(panel,"buildingScreenBounds",new Class<?>[]{String.class,CityBuilding.class},s.world.kind(s.currentMapId),b);
    Point center=new Point((int)bounds.getCenterX(),(int)bounds.getCenterY());
    if(!(Boolean)call(panel,"buildingHitTest",new Class<?>[]{CityBuilding.class,Point.class},b,center))throw new AssertionError("Base not selectable at zoom "+zoom);
    Point roof=new Point(center.x,bounds.y-20);
    if((Boolean)call(panel,"buildingHitTest",new Class<?>[]{CityBuilding.class,Point.class},b,roof))throw new AssertionError("Roof steals movement click at zoom "+zoom);
    field("hoverPoint").set(panel,center);
    g=im.createGraphics();panel.paint(g);g.dispose();
    if(zoom==150) {
     g=im.createGraphics();
     double cx=field("lastCameraX").getDouble(panel), cy=field("lastCameraY").getDouble(panel);
     int ts=(Integer)call(panel,"tileSize",new Class<?>[]{});
     g.setColor(new Color(255,80,40,180));
     for(var box:s.world.buildingFootprints(s.currentMapId,b))
      g.draw(new java.awt.geom.Rectangle2D.Double((box.x-cx)*ts,(box.y-cy)*ts,box.width*ts,box.height*ts));
     g.dispose();
     ImageIO.write(im,"png",Path.of("../asset-review/building-hitboxes/row-fixed.png").toFile());
    }
   }
   System.out.println("Actual GamePanel hover checks passed at 75%, 100%, 150% zoom: base selectable, roof passes clicks; captured " + s.generatedBuildingName(b));
  }catch(Exception e){throw new RuntimeException(e);}finally{panel.shutdown();}
 });
 }
}

