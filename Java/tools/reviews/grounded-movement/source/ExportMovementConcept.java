package com.alderfall.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Locale;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.ImageOutputStream;

/** Standalone visual experiment. Compile explicitly with game sources; not part of the game build. */
public class ExportMovementConcept {
    static final double DISTANCE = .75 * 48 * 144 / 66.0;
    static final Path ROOT = Path.of("tools/reviews/grounded-movement");
    public static void main(String[] args) throws Exception {
        Files.createDirectories(ROOT.resolve("frames"));
        AssetStore assets = new AssetStore(Path.of("assets"));
        String[] ids = {"class_mage", "npc_seraphine", "class_knight"};
        String[] labels = {"Mage", "Seraphine", "Knight"};
        StringBuilder data = new StringBuilder("window.movementConcept={distance:" + DISTANCE + ",actors:[");
        BufferedImage[] mageCurrent = null, mageProposed = null;
        for (int a = 0; a < ids.length; a++) {
            String name = ids[a] + "_model_right";
            BufferedImage still = assets.spriteFit(name,192,144);
            ImageIO.write(still,"png",ROOT.resolve("frames/"+ids[a]+"-source.png").toFile());
            if(a==0)verifyMageHand(still);
            ArticulatedWalkingMotion current = new ArticulatedWalkingMotion(still,1,a==0,name);
            BufferedImage[] originals = new BufferedImage[32];
            StringBuilder poses = new StringBuilder("[");
            for (int f=0;f<32;f++) {
                originals[f]=assets.animatedSpriteFit(name,"walk_articulated",192,144,f);
                if(f>0)poses.append(',');poses.append('[');
                for(int leg=0;leg<2;leg++) { if(leg>0)poses.append(',');var p=current.leg(f,leg);pose(poses,p.hipX(),p.hipY(),p.kneeX(),p.kneeY(),p.ankleX(),p.ankleY()); }
                poses.append(']');
            }
            poses.append(']'); writeStrip(ids[a]+"-current", originals);
            if(a>0)data.append(',');
            data.append("{id:'").append(ids[a]).append("',label:'").append(labels[a]).append("',current:{src:'frames/").append(ids[a]).append("-current.png',stance:0.5,poses:").append(poses).append("},proposed:{");
            int ci=0;
            for(int cadence:new int[]{85,100,120}) {
                GroundedMotionConcept proposal = new GroundedMotionConcept(still,1,a==0,name).cadence(cadence/100.0);
                verifySkeleton(proposal);
                if(a!=0)verifyCloak(proposal);
                BufferedImage[] frames = new BufferedImage[32]; poses=new StringBuilder("[");
                for(int f=0;f<32;f++) {
                    frames[f]=proposal.frame(f);
                    if(f>0)poses.append(',');poses.append('[');
                    for(int leg=0;leg<2;leg++){if(leg>0)poses.append(',');var p=proposal.leg(f,leg);pose(poses,p.hipX(),p.hipY(),p.kneeX(),p.kneeY(),p.ankleX(),p.ankleY());}
                    poses.append(']');
                }
                poses.append(']'); String file=ids[a]+"-grounded-"+cadence;writeStrip(file,frames);
                StringBuilder skeleton=new StringBuilder("[");
                for(int f=0;f<32;f++) { if(f>0)skeleton.append(','); skeleton.append('[');
                    double[][] joints=proposal.skeleton(f);
                    for(int j=0;j<joints.length;j++){if(j>0)skeleton.append(',');pose(skeleton,joints[j]);}
                    skeleton.append(']');
                }
                skeleton.append(']');
                if(ci++>0)data.append(',');data.append("'").append(cadence).append("':{src:'frames/").append(file).append(".png',stance:0.6,poses:").append(poses).append(",skeleton:").append(skeleton).append('}');
                // A planted foot must keep exactly the same world X throughout its stance.
                for(int leg=0;leg<2;leg++) {
                    double anchor=Double.NaN;
                    for(int step=0;step<32;step++) {
                        double t=(step/32.0+leg*.5)%1;
                        if(t>=.6){anchor=Double.NaN;continue;}
                        double world=proposal.leg(step,leg).ankleX()+DISTANCE/(cadence/100.0)*step/32.0;
                        if(Double.isNaN(anchor))anchor=world;
                        if(Math.abs(anchor-world)>1e-6)throw new IllegalStateException("Contact drift");
                    }
                }
                if(a==0&&cadence==100){mageCurrent=originals;mageProposed=frames;}
            }
            data.append("}}");
        }
        data.append("]};\n");Files.writeString(ROOT.resolve("preview-data.js"),data);
        gif(mageCurrent,mageProposed);
        cutoutReview();
        System.out.println("Concept exported: three actors, three cadences, planted-foot, rigid-bone, connected-cloak and Mage hand-ownership checks passed. Production movement unchanged.");
    }
    static void cutoutReview() throws Exception {
        BufferedImage sheet=new BufferedImage(1200,1800,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();
        g.setColor(new Color(24,35,42));g.fillRect(0,0,1200,1800);
        String[] ids={"class_knight","npc_seraphine","class_mage"};
        for(int a=0;a<3;a++) {
            BufferedImage source=ImageIO.read(ROOT.resolve("frames/"+ids[a]+"-source.png").toFile());
            BufferedImage strip=ImageIO.read(ROOT.resolve("frames/"+ids[a]+"-grounded-100.png").toFile());
            for(int i=0;i<3;i++) {
                g.drawImage(i==0?source:strip,400*i,600*a,400*i+384,600*a+576,i==0?48:(i==1?0:8)*192+48,0,i==0?144:(i==1?0:8)*192+144,144,null);
                g.setColor(Color.WHITE);g.drawString(ids[a]+(i==0?" source":i==1?" frame 1":" frame 9"),i*400+10,a*600+590);
            }
        }
        g.dispose();ImageIO.write(sheet,"png",ROOT.resolve("cutout-review.png").toFile());
    }
    static void verifyCloak(GroundedMotionConcept rig) {
        for(int f=0;f<32;f++) {
            BufferedImage cape=rig.cloakFrame(f);int w=cape.getWidth(),h=cape.getHeight(),total=0,largest=0;
            boolean[][] visited=new boolean[h][w];
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                if((cape.getRGB(x,y)>>>24)<100)continue;
                total++;
                if(visited[y][x])continue;
                var queue=new java.util.ArrayDeque<int[]>();queue.add(new int[]{x,y});visited[y][x]=true;int count=0;
                while(!queue.isEmpty()) {
                    int[] p=queue.remove();count++;
                    for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                        int xx=p[0]+dx,yy=p[1]+dy;
                        if(xx<0||xx>=w||yy<0||yy>=h||visited[yy][xx]||(cape.getRGB(xx,yy)>>>24)<100)continue;
                        visited[yy][xx]=true;queue.add(new int[]{xx,yy});
                    }
                }
                largest=Math.max(largest,count);
            }
            if(total<150||largest<total*.90)throw new IllegalStateException("Detached cape in frame "+f+": "+largest+"/"+total);
        }
    }
    static void verifyMageHand(BufferedImage still) {
        // Calibrated source-space finger/knuckle region in the 192x144 Mage standing sprite.
        var rig=new ConceptArmRig(still,1,6,131,93.39807692307691,"class_mage_model_right");
        int checked=0;
        for(int y=75;y<=81;y++)for(int x=98;x<=103;x++) {
            int p=still.getRGB(x,y),r=(p>>16)&255,g=(p>>8)&255,b=p&255;
            if((p>>>24)>100&&r>110&&g>65&&r>g*1.1&&g>b*1.15) {
                if(rig.ownerAt(x,y)!=5)throw new IllegalStateException("Mage hand left on torso at "+x+","+y);
                checked++;
            }
        }
        if(checked<5)throw new IllegalStateException("Mage source changed: recalibrate hand fixture");
    }
    static void verifySkeleton(GroundedMotionConcept rig) {
        int[][] edges={{0,1},{1,2},{2,3},{1,4},{4,5},{5,6},{6,7},{1,8},{8,9},{9,10},{10,11}};
        double[][] reference=rig.skeleton(0);
        for(int f=0;f<32;f++) {
            double[][] joints=rig.skeleton(f);
            for(int[] e:edges) {
                double expected=Math.hypot(reference[e[0]][0]-reference[e[1]][0],reference[e[0]][1]-reference[e[1]][1]);
                double actual=Math.hypot(joints[e[0]][0]-joints[e[1]][0],joints[e[0]][1]-joints[e[1]][1]);
                if(!Double.isFinite(actual)||Math.abs(actual-expected)>1e-8)throw new IllegalStateException("Changing upper-body bone length");
            }
        }
    }
    static void pose(StringBuilder b,double... p){b.append('[');for(int i=0;i<p.length;i++){if(i>0)b.append(',');b.append(String.format(Locale.ROOT,"%.4f",p[i]));}b.append(']');}
    static void writeStrip(String name,BufferedImage[] frames)throws Exception{
        BufferedImage strip=new BufferedImage(192*32,144,BufferedImage.TYPE_INT_ARGB);Graphics2D g=strip.createGraphics();
        for(int f=0;f<32;f++)g.drawImage(frames[f],192*f,0,null);g.dispose();ImageIO.write(strip,"png",ROOT.resolve("frames/"+name+".png").toFile());
    }
    static void gif(BufferedImage[] current,BufferedImage[] proposed)throws Exception{
        ImageWriter writer=ImageIO.getImageWritersByFormatName("gif").next();
        try(ImageOutputStream stream=ImageIO.createImageOutputStream(ROOT.resolve("mage-comparison.gif").toFile())){
            writer.setOutput(stream);writer.prepareWriteSequence(null);
            for(int frame=0;frame<32;frame++){
                BufferedImage image=new BufferedImage(900,380,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
                g.setColor(new Color(19,27,33));g.fillRect(0,0,900,380);g.setFont(new Font("SansSerif",Font.BOLD,19));
                for(int panel=0;panel<2;panel++){
                    int ox=panel*450;g.setColor(new Color(236,228,209));g.drawString(panel==0?"CURRENT MOVEMENT":"GROUNDED CONCEPT",ox+28,35);
                    double scale=1.7, shift=frame/32.0*DISTANCE*scale;
                    g.setColor(new Color(45,55,56));g.fillRect(ox+12,310,426,40);
                    g.setColor(new Color(105,119,106));g.drawLine(ox+12,310,ox+438,310);
                    g.setClip(ox+12,45,426,306);
                    double tile=DISTANCE*scale/2;
                    for(double x=-tile;x<650;x+=tile){int px=ox+(int)Math.round(x-shift%tile);g.drawLine(px,311,px-12,348);}
                    g.drawImage(panel==0?current[frame]:proposed[frame],ox+225-(int)(96*scale),310-(int)(137*scale),(int)(192*scale),(int)(144*scale),null);
                    g.setClip(null);
                }
                g.setFont(new Font("SansSerif",Font.PLAIN,13));g.setColor(new Color(175,188,190));g.drawString("Same travel speed. Watch the boots against the ground. Review-only prototype; game unchanged.",28,370);g.dispose();
                if(frame==4)ImageIO.write(image,"png",ROOT.resolve("comparison-poster.png").toFile());
                var params=writer.getDefaultWriteParam();var metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image),params);
                String format=metadata.getNativeMetadataFormatName();IIOMetadataNode tree=(IIOMetadataNode)metadata.getAsTree(format);
                IIOMetadataNode control=new IIOMetadataNode("GraphicControlExtension");control.setAttribute("disposalMethod","none");control.setAttribute("userInputFlag","FALSE");control.setAttribute("transparentColorFlag","FALSE");control.setAttribute("delayTime","7");control.setAttribute("transparentColorIndex","0");tree.appendChild(control);
                if(frame==0){IIOMetadataNode ext=new IIOMetadataNode("ApplicationExtensions"),loop=new IIOMetadataNode("ApplicationExtension");loop.setAttribute("applicationID","NETSCAPE");loop.setAttribute("authenticationCode","2.0");loop.setUserObject(new byte[]{1,0,0});ext.appendChild(loop);tree.appendChild(ext);}
                metadata.setFromTree(format,tree);writer.writeToSequence(new IIOImage(image,null,metadata),params);
            }
            writer.endWriteSequence();
        }finally{writer.dispose();}
    }
}
