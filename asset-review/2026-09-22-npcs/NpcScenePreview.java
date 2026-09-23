package com.alderfall.game;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
public class NpcScenePreview {
 static Field field(String n)throws Exception{Field f=GamePanel.class.getDeclaredField(n);f.setAccessible(true);return f;}
 public static void main(String[] args)throws Exception{
  Path output=Path.of("../asset-review/2026-09-22-npcs/scenes");Files.createDirectories(output);
  SwingUtilities.invokeAndWait(()->{GamePanel panel=new GamePanel(Path.of("").toAbsolutePath());try{
   ((javax.swing.Timer)field("timer").get(panel)).stop();GameState s=(GameState)field("state").get(panel);s.chooseClass("Mage");while(s.mode==GameMode.STORY_INTRO)s.advanceStoryIntro();s.config.renderQuality="high";panel.setSize(GameConfig.WIDTH,GameConfig.HEIGHT);
   for(String[] job:new String[][]{{"merchant","city_riverside","npc_merchant"},{"blacksmith","city_highwall","npc_blacksmith"},{"orin","village_dunewick","npc_orin"}}){
    Npc npc=GameData.NPCS.stream().filter(n->n.mapId().equals(job[1])&&n.sprite().equals(job[2])).findFirst().orElseThrow();s.currentMapId=npc.mapId();s.playerX=npc.x()+1;s.playerY=npc.y()+1;s.mode=GameMode.EXPLORE;
    BufferedImage im=new BufferedImage(GameConfig.WIDTH,GameConfig.HEIGHT,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();panel.paint(g);g.dispose();ImageIO.write(im,"png",output.resolve(job[0]+".png").toFile());
    if(job[0].equals("merchant")){s.activeNpc=npc;s.dialogIndex=0;s.mode=GameMode.DIALOG;g=im.createGraphics();panel.paint(g);g.dispose();ImageIO.write(im,"png",output.resolve("merchant-dialogue.png").toFile());}
   }
  }catch(Exception e){throw new RuntimeException(e);}finally{panel.shutdown();}});
 }
}
