package com.alderfall.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
public final class SeraphinePlacementReview {
 public static void main(String[] args)throws Exception {
  var assets=new AssetStore(Path.of("assets"));var source=assets.spriteFit("npc_seraphine_model_right",192,144);
  if(args[0].equals("calder-source")){ImageIO.write(assets.spriteFit("npc_calder_model_down",192,144),"png",Path.of("tools/reviews/movement-physics/calibration/calder-source.png").toFile());return;}
  var sim=new ClothMotionStudy(source,1,false,"npc_seraphine_model_right").configure(1,66).physics(true);
  for(double speed:new double[]{.5,1,1.5}) {
   var check=new ClothMotionStudy(source,1,false,"npc_seraphine_model_right").configure(speed,66).physics(true);
   check.verifyPhysics();
   double stride=LocomotionClock.strideTiles(1,0)*48*144/66/LocomotionClock.groundedCadence(speed,1,0);
   for(int f=0;f<32;f++) {
    var a=check.leg(f,0);var b=check.leg(f,1);var pelvis=check.skeleton(f)[0];
    if(Math.abs((a.hipX()+b.hipX())/2-pelvis[0])>.001||Math.abs(a.hipY()-pelvis[1])>.001)throw new AssertionError("Detached pelvis");
    if(pelvis[0]<104||pelvis[0]>108)throw new AssertionError("Hip anchor outside source pelvis");
    for(int l=0;l<2;l++) {
     var q=check.leg(f,l);double thigh=Math.hypot(q.kneeX()-q.hipX(),q.kneeY()-q.hipY()),shin=Math.hypot(q.ankleX()-q.kneeX(),q.ankleY()-q.kneeY());
     if(!Double.isFinite(thigh)||Math.abs(thigh-shin)>.001)throw new AssertionError("Invalid limb lengths");
     double phase=(f/32.0+l*.5)%1;
     if(phase<.60-1/32.0) {
      var next=check.leg((f+1)%32,l);
      if(Math.abs(next.ankleX()-q.ankleX()+stride/32)>.001||Math.abs(next.ankleY()-q.ankleY())>.001)throw new AssertionError("Foot slides in stance");
     }
    }
   }
  }
  System.out.println("PASS: 96 Seraphine poses, pelvis attachment, limb geometry and planted foot travel.");
  var out=new BufferedImage(1440,660,BufferedImage.TYPE_INT_RGB);var g=out.createGraphics();g.setColor(new Color(23,36,42));g.fillRect(0,0,1440,660);
  for(int i=0;i<12;i++){
   int x=i%6*240,y=i/6*330,f=i==0?0:(i-1)*3;
   var im=i==0?source:sim.frame(f);g.drawImage(im,x,y+25,x+240,y+313,36,0,156,144,null);
   g.setColor(Color.WHITE);g.drawString(i==0?"Source":"Frame "+f,x+8,y+18);
   if(i==0)for(int row=60;row<=100;row+=10){g.setColor(Color.GRAY);g.drawLine(x,y+25+row*2,x+240,y+25+row*2);g.drawString("y="+row,x+5,y+25+row*2);}
  }
  g.dispose();ImageIO.write(out,"png",Path.of("tools/reviews/movement-physics/seraphine-"+args[0]+".png").toFile());
  if(args[0].equals("volume")) {
   var before=ImageIO.read(Path.of("tools/reviews/movement-physics/seraphine-volume-before-strip.png").toFile());
   var comparison=new BufferedImage(960,680,BufferedImage.TYPE_INT_RGB);var cg=comparison.createGraphics();cg.setColor(new Color(23,36,42));cg.fillRect(0,0,960,680);
   for(int i=0;i<4;i++)for(int row=0;row<2;row++) {
    int f=new int[]{0,8,16,24}[i],x=i*240,y=row*340;
    var picture=row==0?before.getSubimage(f*192,0,192,144):sim.frame(f);
    cg.drawImage(picture,x,y+30,x+240,y+318,36,0,156,144,null);
    cg.setColor(Color.WHITE);cg.drawString((row==0?"Before":"Tapered contours")+" - frame "+f,x+6,y+20);
   }
   cg.dispose();ImageIO.write(comparison,"png",Path.of("tools/reviews/movement-physics/seraphine-volume-comparison.png").toFile());
  }
  System.out.println("pelvis="+java.util.Arrays.toString(sim.skeleton(0)[0])+" legs="+sim.leg(0,0)+" / "+sim.leg(0,1));
 }
}
