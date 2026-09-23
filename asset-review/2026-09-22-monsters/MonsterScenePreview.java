package com.alderfall.game;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
public class MonsterScenePreview {
 static Field field(String n)throws Exception{Field f=GamePanel.class.getDeclaredField(n);f.setAccessible(true);return f;}
 public static void main(String[] args)throws Exception{
  Path output=Path.of("../asset-review/2026-09-22-monsters/scenes");Files.createDirectories(output);
  SwingUtilities.invokeAndWait(()->{GamePanel p=new GamePanel(Path.of("").toAbsolutePath());try{
   ((javax.swing.Timer)field("timer").get(p)).stop();GameState s=(GameState)field("state").get(p);s.chooseClass("Mage");while(s.mode==GameMode.STORY_INTRO)s.advanceStoryIntro();s.config.renderQuality="high";p.setSize(GameConfig.WIDTH,GameConfig.HEIGHT);
   String[][] groups={{"goblins","goblin","goblin_scout","goblin_archer","goblin_trapper","goblin_skirmisher"},{"goblin-leaders","goblin_shaman","hobgoblin_guard","goblin_warlord","goblin_king"},{"bandits","bandit_cutthroat","bandit_archer","bandit_captain"},{"orcs","orc","orc_raider","orc_berserker","orc_shaman","orc_shieldbearer"}};
   for(String[] group:groups){java.util.List<GameData.MonsterSpec> specs=new ArrayList<>();for(int i=1;i<group.length;i++)specs.add(GameData.MONSTERS.get(group[i]));s.battle=new Battle(s.player,java.util.List.of(),specs,new Random(1),'g',"overworld");s.battle.backdrop="battle_meadow_hills_backdrop";s.mode=GameMode.BATTLE;
    BufferedImage im=new BufferedImage(GameConfig.WIDTH,GameConfig.HEIGHT,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();p.paint(g);g.dispose();ImageIO.write(im,"png",output.resolve(group[0]+".png").toFile());
   }
  }catch(Exception e){throw new RuntimeException(e);}finally{p.shutdown();}});
 }
}
