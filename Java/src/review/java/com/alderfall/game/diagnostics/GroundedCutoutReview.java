package com.alderfall.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
public final class GroundedCutoutReview {
 public static void main(String[] args)throws Exception {
  Path root=Path.of("tools/reviews/characters/grounded-cutouts");Files.createDirectories(root);
  for(String actor:new String[]{"class_ranger","npc_calder","npc_baker","npc_bartender","npc_blacksmith","npc_citizen_man"}) {
   AssetStore assets=new AssetStore(Path.of("assets"));
   BufferedImage sheet=new BufferedImage(960,1280,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();g.setColor(new Color(24,35,42));g.fillRect(0,0,960,1280);
   g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
   String[] directions={"down_left","down_right","up_left","up_right"};
   for(int row=0;row<4;row++)for(int col=0;col<4;col++) {
    String name=actor+"_model_"+directions[row];
    if(!assets.hasSprite(name))name=actor+"_model_"+(directions[row].endsWith("left")?"left":"right");
    String action="walk_grounded";
    if(!name.endsWith("_"+directions[row]))action="walk_grounded_10_66_"+CharacterAnimationAudit.DIRECTIONS.indexOf(directions[row]);
    BufferedImage frame=col==0?assets.spriteFit(name,192,144):assets.animatedSpriteFit(name,action,192,144,(col-1)*8);
    g.drawImage(frame,col*240,row*320+20,col*240+240,row*320+308,36,0,156,144,null);
    g.setColor(Color.WHITE);g.drawString(directions[row]+(col==0?" source":" frame "+((col-1)*8)),col*240+4,row*320+15);
   }
   g.dispose();ImageIO.write(sheet,"png",root.resolve(actor+"-"+(args.length>0?args[0]:"after")+".png").toFile());
  }
 }
}
