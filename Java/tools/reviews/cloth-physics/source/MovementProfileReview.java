package com.alderfall.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Geometry, texture coverage and rollout inventory for the review-only profile pilots. */
public final class MovementProfileReview {
 public static void main(String[] args)throws Exception {
  var assets=new AssetStore(Path.of("assets"));var root=Path.of("tools/reviews/movement-physics");
  var actors=new TreeSet<String>(CharacterAnimationAudit.ACTORS);
  assets.assetNames().stream().filter(n->n.startsWith("npc_")&&n.endsWith("_model_down")).map(n->n.substring(0,n.length()-11)).forEach(actors::add);
  StringBuilder csv=new StringBuilder("actor,direction,source_available,profile_status,next_step\n");int total=0,available=0,pilots=0;
  for(String actor:actors)for(String dir:new String[]{"down","down_left","left","up_left","up","up_right","right","down_right"}) {
   String sprite=actor+"_model_"+dir;boolean source=assets.hasSprite(sprite);boolean pilot=MovementLimbProfile.find(sprite)!=null;
   total++;if(source)available++;if(pilot)pilots++;
   csv.append(actor).append(',').append(dir).append(',').append(source).append(',').append(pilot?"pilot":"pending").append(',')
      .append(pilot?"visual review at gameplay size":source?"calibrate pelvis contours source masks and equipment depth":"review directional fallback before calibration").append('\n');
  }
  Files.writeString(root.resolve("profile-coverage.csv"),csv);
  var review=new BufferedImage(1440,960,BufferedImage.TYPE_INT_RGB);var g=review.createGraphics();g.setColor(new Color(23,36,42));g.fillRect(0,0,1440,960);
  int row=0,poses=0;
  for(String sprite:new String[]{"npc_seraphine_model_right","class_knight_model_right","class_mage_model_right"}) {
   var profile=MovementLimbProfile.find(sprite);boolean robe=profile.surface==MovementLimbProfile.Surface.HIDDEN_BY_ROBE;var source=assets.spriteFit(sprite,192,144);
   var sim=new ClothMotionStudy(source,1,robe,sprite).configure(1,66).physics(true);
   var layers=sim.reviewLegLayers();
   for(int part=0;part<2;part++) {
    var layer=layers[part];
    for(int y=0;y<layer.getHeight();y++) {
     int first=-1,last=-1,count=0;
     for(int x=0;x<layer.getWidth();x++)if((layer.getRGB(x,y)>>>24)>100){if(first<0)first=x;last=x;count++;}
     if(count<5||last-first+1!=count)throw new AssertionError("Missing or disconnected limb cross-section: "+sprite);
     if(count>2*profile.colliderRadius())throw new AssertionError("Contour exceeds collision envelope");
    }
   }
   for(double speed:new double[]{.5,1,1.5}) {
    var on=new ClothMotionStudy(source,1,robe,sprite).configure(speed,66).physics(true);
    var off=new ClothMotionStudy(source,1,robe,sprite).configure(speed,66).physics(false);on.verifyPhysics();
    double travel=LocomotionClock.strideTiles(1,0)*48*144/66/LocomotionClock.groundedCadence(speed,1,0);
    for(int f=0;f<32;f++) {
     poses++;if(!Arrays.deepEquals(on.skeleton(f),off.skeleton(f)))throw new AssertionError("Physics changes skeleton");
     for(int l=0;l<2;l++) {
      var a=on.leg(f,l);if(!a.equals(off.leg(f,l)))throw new AssertionError("Physics changes legs");
      double phase=(f/32.0+l*.5)%1;
      if(phase<.60-1/32.0){var b=on.leg((f+1)%32,l);if(Math.abs(b.ankleX()-a.ankleX()+travel/32)>.001||a.ankleY()!=b.ankleY())throw new AssertionError("Foot slides");}
     }
    }
   }
   int y=row++*320;g.setColor(Color.WHITE);g.drawString(profile.label,8,y+18);
   for(int col=0;col<6;col++) {
    int x=col*240;g.setColor(Color.WHITE);g.drawString(col==0?"Source":col==1?"Thigh / shin / boot":"Frame "+((col-2)*8),x+8,y+36);
    if(col==1){for(int part=0;part<3;part++){var im=layers[part];g.drawImage(im,x+35+part*60,y+80,im.getWidth()*3,im.getHeight()*3,null);}}
    else {var im=col==0?source:sim.frame((col-2)*8);g.drawImage(im,x,y+40,x+240,y+316,36,0,156,144,null);}
   }
  }
  g.dispose();ImageIO.write(review,"png",root.resolve("profile-pilots.png").toFile());
  Files.writeString(root.resolve("profile-coverage.js"),"window.movementProfileCoverage={actors:"+actors.size()+",directions:"+total+",sources:"+available+",pilots:"+pilots+"};");
  if(MovementLimbProfile.find("npc_seraphine_model_up_right")!=null||MovementLimbProfile.find("npc_bartender_model_right")!=null)throw new AssertionError("Uncalibrated direction acquired a pilot");
  System.out.println("PASS: "+poses+" pilot poses, connected contour rows, collision envelopes, planted feet and independent physics. Inventory: "+actors.size()+" actors / "+total+" directions / "+pilots+" pilots.");
 }
}
