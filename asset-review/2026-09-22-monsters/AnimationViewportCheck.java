package com.alderfall.game;
import java.awt.image.BufferedImage;
import java.nio.file.*;
public class AnimationViewportCheck {
 static int visibleHeight(BufferedImage image){int lo=image.getHeight(),hi=-1;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)>8){lo=Math.min(lo,y);hi=Math.max(hi,y);}return hi-lo+1;}
 static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
 public static void main(String[] args)throws Exception{
  Path review=Path.of("../asset-review/2026-09-22-monsters"),fixture=review.resolve("viewport-fixture");Files.createDirectories(fixture);
  Files.copy(Path.of("assets/characters/monsters/animations/skeleton_attack_anim.png"),fixture.resolve("test_attack_anim.png"),StandardCopyOption.REPLACE_EXISTING);
  Files.writeString(fixture.resolve("test_attack_anim.frames"),"6\n");Path bounds=fixture.resolve("test_attack_anim.framebounds");
  Files.writeString(bounds,"0 138 362 390\n");AssetStore valid=new AssetStore(fixture);int fitted=visibleHeight(valid.animatedSpriteFit("test","attack",160,160,0));
  for(String invalid:new String[]{"-1 0 400 900","0 0 0 200","not a rectangle","0 0 99999 99999"}){
   Files.writeString(bounds,invalid);AssetStore fallback=new AssetStore(fixture);int untrimmed=visibleHeight(fallback.animatedSpriteFit("test","attack",160,160,0));require(fitted>untrimmed*1.5,"Invalid viewport must fall back without clipping or zooming");
  }
  AssetStore live=new AssetStore(Path.of("assets"));for(String name:new String[]{"skeleton","spider"})for(int[] size:new int[][]{{128,128},{96,72},{200,300}}){
   require(live.animatedSpriteFrameCount(name,"attack",size[0],size[1])==6,"Frame count changed with display aspect ratio");for(int f=0;f<6;f++)require(visibleHeight(live.animatedSpriteFit(name,"attack",size[0],size[1],f))>20,"Empty attack frame");
  }
  String result="Animation checks passed: both six-frame strips across three display sizes; valid shared viewport; four malformed/out-of-range metadata fallbacks.\n";Files.writeString(review.resolve("animation-verification.txt"),result);System.out.print(result);
 }
}
