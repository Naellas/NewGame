package com.alderfall.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Measures each available source independently; candidates are never installed into runtime. */
public final class CalibrateMovementRoster {
 static final String[] DIRS={"down","down_left","left","up_left","up","up_right","right","down_right"};
 static Path ROOT=Path.of("tools/reviews/movement-physics/calibration");
 record Measurement(String sprite,MovementLimbProfile profile,double pelvis,int top,int bottom,String evidence){}
 static int opaque(BufferedImage im,int x,int y){return im.getRGB(x,y)>>>24;}
 static double median(List<Double> a,double fallback){if(a.isEmpty())return fallback;a.sort(Double::compare);return a.get(a.size()/2);}
 static Measurement measure(String name,BufferedImage im) {
  int top=144,bottom=0;for(int y=0;y<144;y++)for(int x=0;x<192;x++)if(opaque(im,x,y)>100){top=Math.min(top,y);bottom=Math.max(bottom,y);}
  int body=bottom-top;var cloak=GroundedPartMasks.cloak(im,name);
  var centers=new ArrayList<Double>();
  for(int y=top+(int)(body*.43);y<top+body*.57;y++) {
   var xs=new ArrayList<Double>();for(int x=0;x<192;x++)if(opaque(im,x,y)>100&&!cloak[y][x]&&!GroundedPartMasks.equipment(name,x,y))xs.add((double)x);
   if(!xs.isEmpty())centers.add(median(xs,96));
  }
  double pelvis=median(centers,96);
  boolean side=name.endsWith("_left")||name.endsWith("_right");
  boolean robe=name.matches(".*(?:mage|cleric|lyra|maera|healer|scholar|noble|citizen_woman|baker).*")||name.matches("class_(?:ranger|rogue)_model_up.*"),armor=name.matches(".*(?:knight|cassia|samir|guard|quartermaster).*");
  MovementLimbProfile.Surface surface=robe?MovementLimbProfile.Surface.HIDDEN_BY_ROBE:armor?MovementLimbProfile.Surface.ARMOR:MovementLimbProfile.Surface.EXPOSED;
  double[] widths=new double[5];double[] rows={.58,.65,.76,.83,.92};
  int measured=0;
  for(int i=0;i<5;i++) {
   var samples=new ArrayList<Double>();
   for(int yy=-2;yy<=2;yy++) {
    int y=top+(int)(body*rows[i])+yy;
    double best=Double.POSITIVE_INFINITY;int span=0;
    for(int x=0;x<192;) {
     if(opaque(im,x,y)<=100||cloak[y][x]||GroundedPartMasks.equipment(name,x,y)){x++;continue;}
     int start=x;while(x<192&&opaque(im,x,y)>100&&!cloak[y][x]&&!GroundedPartMasks.equipment(name,x,y))x++;
     int width=x-start;double distance=Math.abs((start+x-1)/2.0-pelvis);
     if(distance<best&&width>=3){best=distance;span=width;}
    }
    if(best<22&&span>=4&&span<=(side?18:28))samples.add(span/(side?1.0:1.8));
   }
   double fallback=body*(armor?.092:.073)*new double[]{1.15,1.22,.85,1,.65}[i];
   widths[i]=Math.max(i==4?5:7,Math.min(i<2?15:12,median(samples,fallback)));
   if(!samples.isEmpty())measured++;
  }
  // Anatomical ordering prevents opaque skirts from defining a pin-thin thigh or huge ankle.
  widths[0]=Math.max(widths[0],widths[2]+1);widths[1]=Math.max(widths[1],widths[2]+2);
  widths[3]=Math.max(widths[3],widths[2]);widths[4]=Math.min(widths[4],widths[2]);
  var profile=MovementLimbProfile.candidate(name,surface,pelvis/192,widths);
  return new Measurement(name,profile,profile.pelvis(pelvis,192),top,bottom,measured+"/5 contour landmarks visible; "+(robe?"covered anatomy inferred":"source row spans"));
 }
 static String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}
 static String js(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ")+"\"";}
 public static void main(String[] args)throws Exception {
  if(args.length>0)ROOT=ROOT.resolve("targeted");
  Files.createDirectories(ROOT.resolve("sheets"));
  var assets=new AssetStore(Path.of("assets"));var actors=new TreeSet<String>(CharacterAnimationAudit.ACTORS);
  assets.assetNames().stream().filter(n->n.startsWith("npc_")&&n.endsWith("_model_down")).map(n->n.substring(0,n.length()-11)).forEach(actors::add);
  if(args.length>0)actors.removeIf(a->!Arrays.asList(args).contains(a));
  var measurements=new TreeMap<String,Measurement>();
  StringBuilder params=new StringBuilder("sprite,surface,pelvis_x,hip_y,thigh_root,thigh,knee,calf,ankle,source_hash,evidence\n");
  for(String actor:actors)for(String dir:DIRS){String name=actor+"_model_"+dir;if(!assets.hasSprite(name))continue;var source=assets.spriteFit(name,192,144);var m=measure(name,source);measurements.put(name,m);
   params.append(name).append(',').append(m.profile.surface).append(',').append(m.pelvis).append(',').append(m.top+Math.round((m.bottom-m.top)*.55));
   for(double v:m.profile.contour())params.append(',').append(v);
   params.append(',').append(Integer.toHexString(Arrays.hashCode(source.getRGB(0,0,192,144,null,0,192)))).append(',').append(csv(m.evidence)).append('\n');
  }
  Files.writeString(ROOT.resolve("measured-profiles.csv"),params);
  StringBuilder audit=new StringBuilder("actor,direction,source,status,poses_checked,physics_status,flags\n"), data=new StringBuilder("window.calibrationResults=[");
  int cases=0,passed=0,poses=0,fallbacks=0,physicsPassed=0,actorIndex=0;
  for(String actor:actors) {
   BufferedImage sheet=new BufferedImage(1000,8*210,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();g.setColor(new Color(23,36,42));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
   int row=0;
   for(String dir:DIRS) {
    cases++;String requested=actor+"_model_"+dir,name=requested;boolean fallback=!assets.hasSprite(name);
    if(fallback){fallbacks++;name=actor+"_model_"+(dir.endsWith("left")?"left":"right");}
    if(!assets.hasSprite(name))name=actor+"_model_down";
    var source=assets.spriteFit(name,192,144);var m=measurements.get(name);int facing=dir.endsWith("left")?-1:dir.endsWith("right")?1:0;
    boolean robe=m.profile.surface==MovementLimbProfile.Surface.HIDDEN_BY_ROBE;
    StringJoiner flags=new StringJoiner("; ");if(fallback)flags.add("directional art fallback");flags.add(m.evidence);if(GroundedPartMasks.carryingPose(name))flags.add("carrying grip retained");
    if(actor.equals("npc_seraphine")&&!dir.equals("right"))flags.add("hair chain disabled until directional mask review");
    if(m.profile.surface==MovementLimbProfile.Surface.EXPOSED&&m.evidence.startsWith("0/"))flags.add("no visible contour evidence; authored cutouts needed");
    String status="checks-pass",physics="not-tested";int checked=0;ClothMotionStudy display=null;
    try {
     for(double speed:new double[]{.5,1,1.5}) {
      var sim=new ClothMotionStudy(source,facing,robe,name,requested).configure(speed,66).physics(false);if(speed==1)display=sim;
      var layers=sim.reviewLegLayers();
      for(int part=0;part<2;part++)for(int y=0;y<layers[part].getHeight();y++) {
       int first=-1,last=-1,count=0;for(int x=0;x<layers[part].getWidth();x++)if(opaque(layers[part],x,y)>100){if(first<0)first=x;last=x;count++;}
       if(count<3||last-first+1!=count)throw new AssertionError("disconnected limb cross-section");
       if(count>sim.colliderRadius()*2)throw new AssertionError("limb exceeds collision envelope");
      }
      double dx=facing,dy=dir.startsWith("up")?-1:dir.startsWith("down")?1:0,mag=Math.hypot(dx,dy);if(mag==0){dx=1;mag=1;}dx/=mag;dy/=mag;
      double travel=LocomotionClock.strideTiles(dx,dy)*48*144/66/LocomotionClock.groundedCadence(speed,dx,dy);
      for(int f=0;f<32;f++) {
       var image=sim.frame(f);checked++;poses++;
       for(var p:sim.skeleton(f))for(double v:p)if(!Double.isFinite(v))throw new AssertionError("nonfinite joint");
       for(int l=0;l<2;l++) {
        var a=sim.leg(f,l);double thigh=Math.hypot(a.kneeX()-a.hipX(),a.kneeY()-a.hipY()),shin=Math.hypot(a.ankleX()-a.kneeX(),a.ankleY()-a.kneeY());
        if(!Double.isFinite(thigh)||Math.abs(thigh-shin)>.01)throw new AssertionError("invalid limb geometry");
        double phase=(f/32.0+l*.5)%1;
        if(phase<.60-1/32.0){var b=sim.leg((f+1)%32,l);if(Math.abs(b.ankleX()-a.ankleX()+dx*travel/32)>.01||Math.abs(b.ankleY()-a.ankleY()+dy*travel/32)>.01)throw new AssertionError("foot contact drift");}
       }
       for(int x=0;x<192;x++)if(opaque(image,x,0)>100||opaque(image,x,143)>100)throw new AssertionError("vertical frame clipping");
       for(int y=0;y<144;y++)if(opaque(image,0,y)>100||opaque(image,191,y)>100)throw new AssertionError("horizontal frame clipping");
      }
     }
     passed++;
    }catch(Exception|AssertionError ex){status="needs-fix";flags.add(ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage());}
    try {
     for(double speed:new double[]{.5,1,1.5}) {
      var sim=new ClothMotionStudy(source,facing,robe,name,requested).configure(speed,66).physics(true);sim.verifyPhysics();
      var off=new ClothMotionStudy(source,facing,robe,name,requested).configure(speed,66).physics(false);
      for(int f=0;f<32;f++){if(!Arrays.deepEquals(sim.skeleton(f),off.skeleton(f)))throw new AssertionError("physics changes joints");sim.frame(f);}
     }
     physics="checks-pass";physicsPassed++;
    }catch(Exception|AssertionError ex){physics="needs-fix";flags.add("physics: "+ex.getMessage());}
    if(display==null)display=new ClothMotionStudy(source,facing,robe,name,requested).configure(1,66).physics(false);
    int y=row++*210;g.setColor(status.equals("checks-pass")?Color.WHITE:Color.ORANGE);g.drawString(dir+" / "+status+(fallback?" / fallback":""),4,y+14);
    for(int col=0;col<5;col++) {
     try {var im=col==0?source:display.frame((col-1)*8);g.drawImage(im,col*200+4,y+25,col*200+196,y+207,20,0,172,144,null);
      g.setColor(Color.LIGHT_GRAY);if(col>0)g.drawString("Rig frame "+((col-1)*8),col*200+90,y+14);
     }catch(Exception ex){g.setColor(Color.ORANGE);g.drawString("Render failed",col*200+5,y+70);}
    }
    audit.append(actor).append(',').append(dir).append(',').append(name).append(',').append(status).append(',').append(checked).append(',').append(physics).append(',').append(csv(flags.toString())).append('\n');
    if(cases>1)data.append(',');data.append("{actor:").append(js(actor)).append(",direction:").append(js(dir)).append(",source:").append(js(name)).append(",status:").append(js(status)).append(",physics:").append(js(physics)).append(",fallback:").append(fallback).append(",flags:").append(js(flags.toString())).append('}');
   }
   g.dispose();ImageIO.write(sheet,"png",ROOT.resolve("sheets/"+actor+".png").toFile());
   if(++actorIndex%10==0)System.out.println("Calibrated "+actorIndex+"/"+actors.size()+" actors; "+passed+"/"+cases+" geometry checks passed; "+physicsPassed+" physics checks passed.");
  }
  Files.writeString(ROOT.resolve("checks.csv"),audit);Files.writeString(ROOT.resolve("results.js"),data.append("];\nwindow.calibrationSummary={actors:").append(actors.size()).append(",cases:").append(cases).append(",sources:").append(measurements.size()).append(",poses:").append(poses).append(",geometryPassed:").append(passed).append(",physicsPassed:").append(physicsPassed).append(",fallbacks:").append(fallbacks).append("};").toString());
  System.out.println("FINISHED: "+cases+" cases; "+poses+" poses; geometry "+passed+"; physics "+physicsPassed+"; "+fallbacks+" directional fallbacks. Integration remains disabled; measured candidates require visual review.");
 }
}
