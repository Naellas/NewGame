package com.alderfall.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
public class LatestBuildingsPreview {
 public static void main(String[] args)throws Exception{
  Path review=Path.of("../asset-review/2026-09-22-latest-buildings");
  AssetStore current=new AssetStore(Path.of("assets")), before=new AssetStore(review.resolve("before/Java/assets"));
  java.util.List<String> names=Files.readAllLines(review.resolve("repair-stems.txt"));
  for(int start=0;start<names.size();start+=7){
   int end=Math.min(names.size(),start+7);BufferedImage sheet=new BufferedImage(1060,(end-start)*210+40,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();g.setColor(new Color(28,32,38));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());g.setFont(new Font("SansSerif",Font.PLAIN,14));g.setColor(Color.WHITE);g.drawString("Original / repaired on dark / repaired on light / 48 px fit (actual AssetStore)",15,24);
   for(int i=start;i<end;i++){
    String n=names.get(i);if(!current.hasSprite(n)||!before.hasSprite(n))throw new IllegalStateException("Missing "+n);int y=40+(i-start)*210;g.setColor(Color.WHITE);g.drawString(n,12,y+18);
    Color[] bgs={new Color(70,76,80),new Color(24,35,28),new Color(227,218,198)};
    for(int j=0;j<3;j++){g.setColor(bgs[j]);g.fillRect(12+j*280,y+25,268,176);g.drawImage((j==0?before:current).spriteFit(n,240,166),25+j*280,y+30,null);}
    g.drawImage(current.spriteFit(n,48,48),900,y+106,null);
   }g.dispose();ImageIO.write(sheet,"png",review.resolve("runtime-before-after-"+(start/7+1)+".png").toFile());
  }
  for(String n:Files.readAllLines(review.resolve("latest-runtime-stems.txt")))if(!current.hasSprite(n))throw new IllegalStateException("Latest asset missing: "+n);
  System.out.println("Rendered "+names.size()+" repairs; all 76 latest runtime candidates resolve.");
 }
}
