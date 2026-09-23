package com.alderfall.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

public class NpcReviewCheck {
 static final Path REVIEW=Path.of("../asset-review/2026-09-22-npcs");
 static final String[] REFRESHED={"bartender","blacksmith","citizen_man","citizen_woman","merchant","orin"};
 static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
 static void visible(BufferedImage im,String label){int count=0;for(int y=0;y<im.getHeight();y++)for(int x=0;x<im.getWidth();x++)if((im.getRGB(x,y)>>>24)>32)count++;check(count>20,"Empty render: "+label);}
 public static void main(String[] args)throws Exception{
  AssetStore assets=new AssetStore(Path.of("assets")),before=new AssetStore(REVIEW.resolve("before/Java/assets"));
  Set<String> catalog=new TreeSet<>();for(Npc npc:GameData.NPCS)catalog.add(npc.sprite());
  for(String name:catalog){check(assets.hasSprite(name),"Missing NPC reference: "+name);visible(assets.spriteFit(name,96,128),name);}
  int statics=0,strips=0,frames=0;
  try(var files=Files.walk(Path.of("assets/npcs"))){for(Path p:files.filter(f->f.toString().endsWith(".png")).toList()){
   String stem=p.getFileName().toString().replace(".png","");
   if(!stem.endsWith("_anim")){visible(assets.spriteFit(stem,43,58),stem);statics++;continue;}
   int split=stem.lastIndexOf('_',stem.length()-6);String name=stem.substring(0,split),action=stem.substring(split+1,stem.length()-5);
   int expected=Integer.parseInt(Files.readString(p.resolveSibling(stem+".frames")).trim());
   for(int[] size:new int[][]{{43,58},{96,128},{160,160}}){check(assets.animatedSpriteFrameCount(name,action,size[0],size[1])==expected,"Wrong frame count: "+stem);for(int f=0;f<expected;f++){visible(assets.animatedSpriteFit(name,action,size[0],size[1],f),stem+":"+f);frames++;}}
   strips++;
  }}
  BufferedImage preview=new BufferedImage(1040,6*195+40,BufferedImage.TYPE_INT_RGB);Graphics2D g=preview.createGraphics();g.setColor(new Color(43,49,55));g.fillRect(0,0,preview.getWidth(),preview.getHeight());g.setColor(Color.WHITE);g.drawString("Before / Updated / Left / Right / Back / Gameplay size       (actual game renderer)",12,24);
  for(int i=0;i<REFRESHED.length;i++){
   String n="npc_"+REFRESHED[i]+"_model";int y=40+i*195;g.setColor(Color.WHITE);g.drawString(REFRESHED[i],12,y+15);
   g.drawImage(before.spriteFit(n+"_down",110,145),20,y+33,null);
   int k=1;for(String d:new String[]{"down","left","right","up"}){g.drawImage(assets.spriteFit(n+"_"+d,110,145),k*165+20,y+33,null);k++;}
   g.drawImage(assets.spriteFit(n+"_down",43,58),885,y+102,null);g.drawImage(assets.animatedSpriteFit(n+"_left","walk",43,58,3),955,y+102,null);
  }g.dispose();ImageIO.write(preview,"png",REVIEW.resolve("npc-before-after.png").toFile());
  BufferedImage directions=new BufferedImage(700,7*165,BufferedImage.TYPE_INT_RGB);g=directions.createGraphics();g.setColor(new Color(55,62,69));g.fillRect(0,0,700,directions.getHeight());int row=0;
  for(String n:new String[]{"elowen","garruk","kael","ren","torin","vexa","quartermaster"}){g.setColor(Color.WHITE);g.drawString(n+": left / right / walking left / walking right",10,row*165+20);int col=0;for(String d:new String[]{"left","right"}){g.drawImage(assets.spriteFit("npc_"+n+"_model_"+d,90,125),20+col++*165,row*165+30,null);}for(String d:new String[]{"left","right"})g.drawImage(assets.animatedSpriteFit("npc_"+n+"_model_"+d,"walk",90,125,3),20+col++*165,row*165+30,null);row++;}g.dispose();ImageIO.write(directions,"png",REVIEW.resolve("corrected-directions.png").toFile());
  BufferedImage chores=new BufferedImage(900,330,BufferedImage.TYPE_INT_RGB);g=chores.createGraphics();g.setColor(new Color(55,62,69));g.fillRect(0,0,900,330);row=0;for(String[] job:new String[][]{{"blacksmith","hammer"},{"merchant","inspect"}}){for(int f=0;f<6;f++)g.drawImage(assets.animatedSpriteFit("npc_"+job[0]+"_model_down",job[1],110,145,f),f*150+10,row*165+10,null);row++;}g.dispose();ImageIO.write(chores,"png",REVIEW.resolve("work-animations.png").toFile());
  String result="PASS: "+catalog.size()+" unique NPC catalog references, "+statics+" static NPC assets, "+strips+" animation strips; "+frames+" frame renders across three display sizes.\n";Files.writeString(REVIEW.resolve("runtime-verification.txt"),result);System.out.print(result);
 }
}
