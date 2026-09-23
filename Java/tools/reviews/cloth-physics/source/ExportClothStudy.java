package com.alderfall.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import java.util.Locale;
public final class ExportClothStudy {
 static Path root=Path.of("tools/reviews/cloth-physics");
 public static void main(String[] args)throws Exception {
  Files.createDirectories(root.resolve("frames"));AssetStore assets=new AssetStore(Path.of("assets"));
  BufferedImage review=new BufferedImage(1200,340,BufferedImage.TYPE_INT_RGB);var rg=review.createGraphics();rg.setColor(new Color(24,35,42));rg.fillRect(0,0,1200,340);
  StringBuilder json=new StringBuilder("window.clothStudy={actors:[");
  String[] actors={"class_mage","npc_seraphine","class_knight"};
  for(int a=0;a<3;a++) {
   if(a>0)json.append(',');json.append("{name:'").append(new String[]{"Mage","Seraphine","Knight"}[a]).append("',profile:'").append(MovementLimbProfile.find(actors[a]+"_model_right").label).append("',robe:").append(a==0).append(",hair:").append(a==1).append(",speeds:{");
   int k=0;for(int requested:new int[]{5,10,15}) {
    if(k++>0)json.append(',');String name=actors[a]+"_model_right";
    BufferedImage source=assets.spriteFit(name,192,144);
    var sim=new ClothMotionStudy(source,1,a==0,name).configure(requested/10.0,66).physics(true);
    sim.verifyPhysics();
    var rigOnly=new ClothMotionStudy(source,1,a==0,name).configure(requested/10.0,66).physics(false);
    double effective=LocomotionClock.groundedCadence(requested/10.0,1,0);
    json.append("'").append(requested).append("':{cadence:").append(effective).append(",colliderRadius:").append(sim.colliderRadius()).append(",base:'frames/").append(actors[a]).append('-').append(requested).append("-base.png',cloth:'frames/").append(actors[a]).append('-').append(requested).append("-cloth.png',rig:'frames/").append(actors[a]).append('-').append(requested).append("-rig.png',legs:[");
    BufferedImage base=new BufferedImage(6144,144,2),cloth=new BufferedImage(6144,144,2);var rig=new BufferedImage(6144,144,2);var rigGraphics=rig.createGraphics();var bg=base.createGraphics();var cg=cloth.createGraphics();
    var baselineRig=new GroundedWalkingMotion(source,1,a==0,name).configure(requested/10.0,66);
    StringBuilder baseLegs=new StringBuilder("["),baseSkeleton=new StringBuilder("[");
    StringBuilder nodes=new StringBuilder("["),skeleton=new StringBuilder("[");
    for(int f=0;f<32;f++) {
     if(f>0){baseLegs.append(',');baseSkeleton.append(',');}baseLegs.append('[');baseSkeleton.append('[');
     for(int l=0;l<2;l++){if(l>0)baseLegs.append(',');var q=baselineRig.leg(f,l);array(baseLegs,q.hipX(),q.hipY(),q.kneeX(),q.kneeY(),q.ankleX(),q.ankleY());}
     double[][] bj=baselineRig.skeleton(f);for(int j=0;j<bj.length;j++){if(j>0)baseSkeleton.append(',');array(baseSkeleton,bj[j]);}baseLegs.append(']');baseSkeleton.append(']');
     bg.drawImage(assets.animatedSpriteFit(name,"walk_grounded_"+requested+"_66",192,144,f),f*192,0,null);
     cg.drawImage(sim.frame(f),f*192,0,null);
     rigGraphics.drawImage(rigOnly.frame(f),f*192,0,null);
     for(int l=0;l<2;l++)if(!sim.leg(f,l).equals(rigOnly.leg(f,l)))throw new AssertionError("Physics changed leg geometry");
     if(!java.util.Arrays.deepEquals(sim.skeleton(f),rigOnly.skeleton(f)))throw new AssertionError("Physics changed body joints");
     if(f>0){json.append(',');nodes.append(',');skeleton.append(',');}json.append('[');nodes.append('[');
     for(int leg=0;leg<2;leg++){if(leg>0)json.append(',');var p=sim.leg(f,leg);array(json,p.hipX(),p.hipY(),p.kneeX(),p.kneeY(),p.ankleX(),p.ankleY());}
     json.append(']');double[][] points=sim.clothNodes(f);
     for(int n=0;n<points.length;n++){if(n>0)nodes.append(',');array(nodes,points[n]);for(double v:points[n])if(!Double.isFinite(v))throw new IllegalStateException("Non-finite cloth node");}
     nodes.append(']');skeleton.append('[');double[][] joints=sim.skeleton(f);for(int j=0;j<joints.length;j++){if(j>0)skeleton.append(',');array(skeleton,joints[j]);}skeleton.append(']');
    }
    bg.dispose();cg.dispose();rigGraphics.dispose();
    if(requested==10)for(int which=0;which<2;which++) {
     int col=a*2+which;rg.drawImage(which==0?base:cloth,col*200,35,col*200+192,323,8*192+48,0,8*192+144,144,null);
     rg.setColor(Color.WHITE);rg.drawString(actors[a]+(which==0?" base":" physics"),col*200+3,20);
    }nodes.append(']');json.append("],nodes:").append(nodes).append(",skeleton:").append(skeleton).append("],baseLegs:").append(baseLegs).append("],baseSkeleton:").append(baseSkeleton).append("]}");
    ImageIO.write(base,"png",root.resolve("frames/"+actors[a]+"-"+requested+"-base.png").toFile());
    ImageIO.write(rig,"png",root.resolve("frames/"+actors[a]+"-"+requested+"-rig.png").toFile());
    ImageIO.write(cloth,"png",root.resolve("frames/"+actors[a]+"-"+requested+"-cloth.png").toFile());
   }json.append("}}");
  }json.append("]};");Files.writeString(root.resolve("data.js"),json);
  rg.dispose();ImageIO.write(review,"png",root.resolve("comparison.png").toFile());
  System.out.println("Cloth study exported: three actors, three cadence preferences, 288 simulated poses; finite spring nodes, pinned roots, loop continuity and capsule-contact checks passed.");
 }
 static void array(StringBuilder b,double...values){b.append('[');for(int i=0;i<values.length;i++){if(i>0)b.append(',');b.append(String.format(Locale.ROOT,"%.4f",values[i]));}b.append(']');}
}
