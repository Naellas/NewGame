package com.alderfall.game;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
public final class GroundedCutoutTest {
 public static void main(String[] args)throws Exception {
  BufferedImage source=new BufferedImage(192,144,BufferedImage.TYPE_INT_ARGB);
  for(int y=6;y<80;y++)for(int x=88;x<=104;x++)source.setRGB(x,y,0xff927848);
  for(int y=80;y<=137;y++)for(int x=78;x<=114;x++)if(x<=86||x>=106)source.setRGB(x,y,0xff59351f);
  var rig=new GroundedWalkingMotion(source,1,false,"fixture_model_down_right").configure(1,66);
  var bootField=GroundedWalkingMotion.class.getDeclaredField("boot");bootField.setAccessible(true);
  BufferedImage boot=(BufferedImage)bootField.get(rig);int count=0;
  for(int y=0;y<boot.getHeight();y++)for(int x=0;x<boot.getWidth();x++)if((boot.getRGB(x,y)>>>24)>100)count++;
  if(count<20)throw new IllegalStateException("Boot sampler selected the gap between feet");
  AssetStore assets=new AssetStore(Path.of("assets"));
  for(String direction:new String[]{"up_left","up_right"})for(int f=0;f<32;f++) {
   BufferedImage frame=assets.animatedSpriteFit("class_ranger_model_"+direction,"walk_grounded",192,144,f);
   for(int y=85;y<104;y++) {
    int missing=0;
    for(int x=90;x<=100;x++)if((frame.getRGB(x,y)>>>24)<100)missing++;
    if(missing>8)throw new IllegalStateException("Hole through rear cloak "+direction+" frame "+f);
   }
  }
  String fallback="walk_grounded_10_66_7";
  if(!WorldCharacterAnimation.action(fallback)||!WorldCharacterAnimation.action("settle_grounded_10_66_7_8"))throw new IllegalStateException("Diagonal fallback action rejected");
  BufferedImage side=assets.animatedSpriteFit("npc_bartender_model_right","walk_grounded_10_66",192,144,0);
  BufferedImage diagonal=assets.animatedSpriteFit("npc_bartender_model_right",fallback,192,144,0);
  if(java.util.Arrays.equals(side.getRGB(0,0,192,144,null,0,192),diagonal.getRGB(0,0,192,144,null,0,192)))throw new IllegalStateException("Side-art fallback lost diagonal gait");
  System.out.println("Cutout checks passed: separated-boot source sampling and continuous Ranger rear cloak across 64 poses.");
 }
}
