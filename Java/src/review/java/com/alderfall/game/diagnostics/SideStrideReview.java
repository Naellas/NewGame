package com.alderfall.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
public class SideStrideReview {
 public static void main(String[] args)throws Exception {
  var assets=new AssetStore(Path.of("assets"));var sheet=new BufferedImage(960,640,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();
  g.setColor(new Color(24,35,42));g.fillRect(0,0,960,640);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
  for(int row=0;row<2;row++)for(int col=0;col<4;col++){
   String direction=row==0?"left":"right";int f=new int[]{0,4,8,16}[col];
   BufferedImage pose=assets.animatedSpriteFit("class_mage_model_"+direction,"walk_grounded_5_66",192,144,f);
   g.drawImage(pose,col*240,row*320+20,col*240+240,row*320+308,36,0,156,144,null);g.setColor(Color.WHITE);g.drawString(direction+" / 0.5x preference / frame "+f,col*240+5,row*320+15);
  }
  g.dispose();Path out=Path.of("tools/reviews/cloth-physics/side-stride-"+(args.length>0?args[0]:"after")+".png");Files.createDirectories(out.getParent());ImageIO.write(sheet,"png",out.toFile());
 }
}
