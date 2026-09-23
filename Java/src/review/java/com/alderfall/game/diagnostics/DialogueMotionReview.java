package com.alderfall.game;

import com.alderfall.game.ui.DialogueFigureAnimation;
import com.alderfall.game.ui.DialogueAnimationLayers;
import com.alderfall.game.ui.DialogueRigProfile;
import com.alderfall.game.ui.DialogueBodyMotion;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Exports actual runtime poses, including their transition history, for local browser review. */
public final class DialogueMotionReview {
    private static void assertFeet(BufferedImage source,BufferedImage frame,DialogueBodyMotion.Rig rig,int left,int top,double scale,String sprite) {
        int checked=0;
        for(int y=0;y<800;y++)for(int x=0;x<480;x++)if(rig.planted((x-left)/scale,(y-top)/scale)) {
            if(source.getRGB(x,y)!=frame.getRGB(x,y))throw new AssertionError("Foot contact moved: "+sprite+" at "+x+","+y);
            if((source.getRGB(x,y)>>>24)>8)checked++;
        }
        if(checked==0)throw new AssertionError("Foot constraints missed artwork: "+sprite);
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of("tools/reviews/dialogue-motion");
        Files.createDirectories(root.resolve("frames"));
        AssetStore assets = new AssetStore(Path.of("assets"));
        java.util.List<Path> roster;
        try (var files = Files.walk(Path.of("assets"))) {
            roster = files.filter(p -> p.toString().endsWith("_dialogue_sprite.png")).sorted().toList();
        }
        StringBuilder data = new StringBuilder("window.dialogueRoster=[\n");
        long renderNanos = 0; int samples = 0;
        for (Path file : roster) {
            String sprite = file.getFileName().toString().replace(".png", "");
            BufferedImage source = assets.spriteFit(sprite, 480, 800);
            int[] original = source.getRGB(0,0,480,800,null,0,480);
            int left=480,top=800,bottom=-1,right=-1;
            for(int y=0;y<800;y++)for(int x=0;x<480;x++)if((source.getRGB(x,y)>>>24)>8){left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
            double normalizedScale=(bottom-top+1)/1000.0;
            var bodyRig=DialogueBodyMotion.rig(sprite,(right-left)/normalizedScale*.5,(right-left+1)/normalizedScale);
            boolean lowerMoved=false;
            int direction = sprite.startsWith("class_") ? -1 : 1;
            DialogueFigureAnimation animation = new DialogueFigureAnimation();
            DialogueRigProfile profile=DialogueRigProfile.forSprite(sprite);
            boolean attachments=!profile.parts().isEmpty();
            DialogueFigureAnimation withoutPhysics=new DialogueFigureAnimation(
                    new DialogueAnimationLayers.Settings(true,true,true,true,true,false,false));
            BufferedImage atlas = new BufferedImage(480*8,800*12,BufferedImage.TYPE_INT_ARGB);
            BufferedImage heads = new BufferedImage(240*8,220*12,BufferedImage.TYPE_INT_ARGB);
            BufferedImage rigAtlas=attachments ? new BufferedImage(480*8,800*12,BufferedImage.TYPE_INT_ARGB) : null;
            BufferedImage rigHeads=attachments ? new BufferedImage(240*8,220*12,BufferedImage.TYPE_INT_ARGB) : null;
            Graphics2D ra=attachments ? rigAtlas.createGraphics() : null, rh=attachments ? rigHeads.createGraphics() : null;
            Graphics2D ag = atlas.createGraphics(), hg = heads.createGraphics();
            ag.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            for (int frame = 0; frame < 96; frame++) {
                long ms = frame*1000L/24;
                boolean speech = frame >= 8 && frame < 52;
                boolean listening = direction < 0 ? speech : frame >= 52 && frame < 76;
                boolean speaking = direction > 0 && speech;
                int mouth = DialogueFigureAnimation.mouthPose("Speaking", !speaking,ms);
                BufferedImage result = new BufferedImage(480,800,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = result.createGraphics();
                long start = System.nanoTime();
                animation.draw(g,source,sprite,0,0,ms,mouth,speaking,listening,direction);
                renderNanos += System.nanoTime()-start; samples++;
                g.dispose();
                if(attachments) {
                    BufferedImage plain=new BufferedImage(480,800,BufferedImage.TYPE_INT_ARGB);
                    Graphics2D pg=plain.createGraphics();
                    withoutPhysics.draw(pg,source,sprite,0,0,ms,mouth,speaking,listening,direction);pg.dispose();
                    ra.drawImage(plain,frame%8*480,frame/8*800,null);
                    rh.drawImage(plain,frame%8*240,frame/8*220,frame%8*240+240,frame/8*220+220,120,0,360,220,null);
                    assertFeet(source,plain,bodyRig,left,top,normalizedScale,sprite);
                }
                assertFeet(source,result,bodyRig,left,top,normalizedScale,sprite);
                if(!Arrays.equals(source.getRGB(0,450,480,150,null,0,480),result.getRGB(0,450,480,150,null,0,480)))lowerMoved=true;
                ag.drawImage(result,frame%8*480,frame/8*800,null);
                hg.drawImage(result,frame%8*240,frame/8*220,frame%8*240+240,frame/8*220+220,120,0,360,220,null);
                if (frame == 40) {
                    BufferedImage repeated = new BufferedImage(480,800,BufferedImage.TYPE_INT_ARGB);
                    Graphics2D repeatGraphics = repeated.createGraphics();
                    animation.draw(repeatGraphics,source,sprite,0,0,ms,mouth,speaking,listening,direction);
                    repeatGraphics.dispose();
                    if (!Arrays.equals(repeated.getRGB(0,0,480,800,null,0,480),result.getRGB(0,0,480,800,null,0,480)))
                        throw new AssertionError("Repeated repaint changed pose: " + sprite);
                    BufferedImage idle = new BufferedImage(480,800,BufferedImage.TYPE_INT_ARGB);
                    Graphics2D ig = idle.createGraphics();
                    new DialogueFigureAnimation().draw(ig,source,sprite,0,0,ms,mouth); ig.dispose();
                    if (Arrays.equals(idle.getRGB(0,0,480,400,null,0,480),result.getRGB(0,0,480,400,null,0,480)))
                        throw new AssertionError("Missing upper-body articulation: " + sprite);
                }
            }
            if(!lowerMoved)throw new AssertionError("Whole-body motion missing: "+sprite);
            ag.dispose(); hg.dispose();
            if(attachments) {
                ra.dispose();rh.dispose();
                ImageIO.write(rigAtlas,"png",root.resolve("frames/"+sprite+"-rig-body.png").toFile());
                ImageIO.write(rigHeads,"png",root.resolve("frames/"+sprite+"-rig-face.png").toFile());
            }
            if (!Arrays.equals(original,source.getRGB(0,0,480,800,null,0,480))) throw new AssertionError("Source mutated");
            ImageIO.write(source,"png",root.resolve("frames/"+sprite+"-source.png").toFile());
            ImageIO.write(atlas,"png",root.resolve("frames/"+sprite+"-body.png").toFile());
            ImageIO.write(heads,"png",root.resolve("frames/"+sprite+"-face.png").toFile());
            data.append("{id:'").append(sprite).append("',side:").append(direction)
                    .append(",origin:[").append(left).append(',').append(top).append("],scale:").append((bottom-top+1)/1000.0);
            if(profile.lips()!=null) {
                var lip=profile.lips();data.append(",lips:[").append(lip.leftX()).append(',').append(lip.leftY()).append(',')
                    .append(lip.middleX()).append(',').append(lip.middleY()).append(',').append(lip.rightX()).append(',').append(lip.rightY()).append(']');
            }
            data.append(",feet:[");
            for(var foot:bodyRig.feet())data.append('[').append(foot.x()).append(',').append(foot.y()).append(',').append(foot.width()).append(',').append(foot.height()).append("],");
            data.append("],colliders:[");
            for(var collider:DialogueBodyMotion.colliders(sprite))data.append('[').append(collider.x()).append(',').append(collider.top()).append(',').append(collider.bottom()).append(',').append(collider.radius()).append("],");
            data.append("],parts:[");
            for(var part:profile.parts())data.append("{name:'").append(part.name()).append("',kind:'").append(part.kind())
                    .append("',pivot:[").append(part.pivotX()).append(',').append(part.pivotY()).append("],points:")
                    .append(Arrays.toString(part.polygon())).append("},");
            data.append("]},\n");
            System.out.println("Reviewed 96 frames: " + sprite);
        }
        Files.writeString(root.resolve("roster.js"),data.append("];\n").toString());
        System.out.printf(Locale.ROOT,"Runtime export passed: %d frames, %.2f ms/figure average (includes cold caches).%n",samples,renderNanos/1e6/samples);
    }
}
