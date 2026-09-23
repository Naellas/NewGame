package com.alderfall.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
public class MonsterReviewCheck {
 public static void main(String[] args)throws Exception{
  Path review=Path.of("../asset-review/2026-09-22-monsters");
  AssetStore assets=new AssetStore(Path.of("assets"));
  AssetStore before=new AssetStore(review.resolve("before/Java/assets"));
  Set<String> names=new TreeSet<>();for(var spec:GameData.MONSTERS.values())names.add(spec.sprite());
  for(String n:names){if(!assets.hasSprite(n))throw new IllegalStateException("Missing catalog monster: "+n);if(assets.spriteFit(n,128,128)==null)throw new IllegalStateException("Failed to render "+n);}
  BufferedImage family=new BufferedImage(1000,800,BufferedImage.TYPE_INT_RGB);Graphics2D fg=family.createGraphics();fg.setColor(new Color(55,61,68));fg.fillRect(0,0,1000,800);
  String[] familyNames={"goblin","goblin_scout","goblin_skirmisher","hobgoblin_guard","goblin_king","orc","orc_raider","orc_berserker","orc_shieldbearer","orc_shaman","bat","crypt_bat","wolf","frost_wolf","snow_lynx","red_dragon","mountain_drake","marsh_drake","elder_dragon","shadow_beast"};
  for(int i=0;i<familyNames.length;i++){int x=i%5*200,y=i/5*200;fg.setColor(Color.WHITE);fg.drawString(familyNames[i],x+6,y+18);fg.drawImage(assets.spriteFit(familyNames[i],128,128),x+35,y+40,null);}fg.dispose();ImageIO.write(family,"png",review.resolve("family-consistency.png").toFile());
  String[] variants={"goblin","goblin_scout","goblin_archer","goblin_trapper","goblin_skirmisher","goblin_shaman","hobgoblin_guard","goblin_warlord","goblin_king","bandit_cutthroat","bandit_archer","bandit_captain"};
  BufferedImage groups=new BufferedImage(900,1000,BufferedImage.TYPE_INT_RGB);Graphics2D gg=groups.createGraphics();gg.setColor(new Color(54,60,68));gg.fillRect(0,0,900,1000);
  for(int i=0;i<variants.length;i++){int x=i%3*300,y=i/3*250;gg.setColor(Color.WHITE);gg.drawString(variants[i],x+10,y+20);gg.drawImage(assets.spriteFit(variants[i],160,160),x+15,y+45,null);gg.drawImage(assets.spriteFit(variants[i],48,48),x+215,y+155,null);}gg.dispose();ImageIO.write(groups,"png",review.resolve("goblin-bandit-family.png").toFile());
  BufferedImage animations=new BufferedImage(1200,400,BufferedImage.TYPE_INT_RGB);Graphics2D ag=animations.createGraphics();ag.setColor(new Color(60,66,74));ag.fillRect(0,0,1200,400);
  int row=0;for(String n:java.util.List.of("skeleton","spider")){
   int count=assets.animatedSpriteFrameCount(n,"attack",160,160);if(count!=6)throw new IllegalStateException(n+" inferred "+count+" frames instead of six");
   ag.setColor(Color.WHITE);ag.drawString(n+" attack: six frames",10,row*200+20);
   for(int f=0;f<count;f++){BufferedImage frame=assets.animatedSpriteFit(n,"attack",160,160,f);if(frame==null)throw new IllegalStateException("Missing frame "+f);ag.drawImage(frame,f*200+20,row*200+30,null);}
   row++;
  }ag.dispose();ImageIO.write(animations,"png",review.resolve("animation-runtime.png").toFile());
  java.util.List<String> repaired=Files.readAllLines(review.resolve("repair-stems.txt"));
  for(int start=0;start<repaired.size();start+=5){
   BufferedImage out=new BufferedImage(1000,Math.min(5,repaired.size()-start)*215+40,BufferedImage.TYPE_INT_RGB);Graphics2D g=out.createGraphics();g.setColor(new Color(30,34,40));g.fillRect(0,0,out.getWidth(),out.getHeight());g.setColor(Color.WHITE);g.drawString("Original / repaired dark / repaired light / 48 px (actual AssetStore)",10,22);
   for(int i=start;i<Math.min(start+5,repaired.size());i++){
    String n=repaired.get(i);int y=40+(i-start)*215;g.setColor(Color.WHITE);g.drawString(n,10,y+15);
    Color[] bg={new Color(65,70,75),new Color(20,29,35),new Color(229,216,193)};
    for(int c=0;c<3;c++){g.setColor(bg[c]);g.fillRect(c*280+8,y+22,265,183);g.drawImage((c==0?before:assets).spriteFit(n,250,175),c*280+15,y+25,null);}
    g.drawImage(assets.spriteFit(n,48,48),900,y+100,null);
   }g.dispose();ImageIO.write(out,"png",review.resolve("runtime-before-after-"+(start/5+1)+".png").toFile());
  }
  String result="Catalog entries: "+GameData.MONSTERS.size()+"; unique sprite stems: "+names.size()+"; all resolve and render; compared repairs: "+repaired.size()+".\n";
  Files.writeString(review.resolve("runtime-verification.txt"),result);System.out.print(result);
 }
}
