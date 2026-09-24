package com.alderfall.game;

import com.alderfall.game.ui.DialogueFigureAnimation;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Compares the sparse renderer to the dense reference and checks viewport cache invalidation. */
public final class DialogueMeshTest {
    private static BufferedImage frame(DialogueFigureAnimation renderer,BufferedImage source,String sprite,long time,Rectangle clip) {
        BufferedImage image=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();g.setClip(clip);
        renderer.draw(g,source,sprite,0,0,time,2,true,false,1);g.dispose();return image;
    }
    public static void main(String[] args) throws Exception {
        String previous=System.getProperty("alderfall.dialogueDenseReference");
        try {
            AssetStore assets=new AssetStore(Path.of("assets"));
            BufferedImage sheet=new BufferedImage(1440,800,BufferedImage.TYPE_INT_ARGB);
            Graphics2D sg=sheet.createGraphics();int actor=0;
            for(String sprite:new String[]{"npc_aria_dialogue_sprite","npc_vesper_dialogue_sprite","class_mage_dialogue_sprite"}) {
                BufferedImage source=assets.spriteFit(sprite,480,800);
                System.setProperty("alderfall.dialogueDenseReference","false");var mesh=new DialogueFigureAnimation();
                System.setProperty("alderfall.dialogueDenseReference","true");var dense=new DialogueFigureAnimation();
                long error=0,count=0;int holes=0;
                for(int f=0;f<12;f++) {
                    long time=f*83L;
                    BufferedImage a=frame(mesh,source,sprite,time,null),b=frame(dense,source,sprite,time,null);
                    for(int y=0;y<800;y++)for(int x=0;x<480;x++) {
                        int ca=a.getRGB(x,y),cb=b.getRGB(x,y);
                        if((cb>>>24)==255) {
                            if((ca>>>24)<240)holes++;
                            for(int shift=0;shift<24;shift+=8){error+=Math.abs((ca>>shift&255)-(cb>>shift&255));count++;}
                        }
                    }
                    if(f==11) {
                        // Cropping must not change the mesh or reuse a stale partial frame.
                        Rectangle crop=new Rectangle(130,0,220,245);
                        BufferedImage clipped=frame(mesh,source,sprite,time,crop);
                        for(int y=0;y<245;y++)for(int x=130;x<350;x++)if(a.getRGB(x,y)!=clipped.getRGB(x,y))throw new AssertionError("Viewport changed mesh pixels");
                        BufferedImage full=frame(mesh,source,sprite,time,null);
                        if(!Arrays.equals(a.getRGB(0,0,480,800,null,0,480),full.getRGB(0,0,480,800,null,0,480)))throw new AssertionError("Partial frame reused as full frame");
                        sg.drawImage(a,actor*480,0,null);
                    }
                }
                double mean=error/(double)count;
                System.out.printf(Locale.ROOT,"%s mean RGB difference %.3f; opacity differences %d%n",sprite,mean,holes);
                if(mean>5)throw new AssertionError("Mesh drifted from dense reference");
                if(holes>60)throw new AssertionError("Mesh introduced opacity seams");
                actor++;
            }
            sg.dispose();Files.createDirectories(Path.of("temp/dialogue-mesh"));
            ImageIO.write(sheet,"png",Path.of("temp/dialogue-mesh/mesh-figures.png").toFile());
            System.out.println("DialogueMeshTest passed.");
        } finally {if(previous==null)System.clearProperty("alderfall.dialogueDenseReference");else System.setProperty("alderfall.dialogueDenseReference",previous);}
    }
}
