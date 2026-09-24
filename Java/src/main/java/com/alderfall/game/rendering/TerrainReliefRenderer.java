package com.alderfall.game;

import java.awt.image.BufferedImage;

/** Continuous contour banks drawn in the ground plane, so existing actor/camera anchors stay exact. */
final class TerrainReliefRenderer {
    private static final double FACE_SCALE=.625;
    private TerrainReliefRenderer() { }
    static void apply(BufferedImage image,TerrainElevation.Field field,int tx,int ty,int size) {
        if(!field.active())return;
        // World-space halo makes bank faces identical across tile and cache chunk boundaries.
        int halo=(int)Math.ceil(4*FACE_SCALE*size)+2;
        for(int x=0;x<size;x++) {
            double wx=tx+(x+.5)/size;
            double previous=field.heightAt(wx,ty-(halo+.5)/size);
            double faceTop=-100,faceBottom=-100,drop=0;
            ElevationMaterial material=ElevationMaterial.EARTH;
            for(int y=-halo;y<size;y++) {
                double wy=ty+(y+.5)/size;
                double h=field.heightAt(wx,wy);
                if(h>previous+.1)faceBottom=-100;
                if(previous-h>.1) {
                    faceTop=wy;drop=(previous-h)*FACE_SCALE;faceBottom=wy+drop;
                    material=field.materialAt(wx,wy-1.0/size);
                }
                if(y>=0) {
                    int color=image.getRGB(x,y);
                    double trail=field.rampAt(wx,wy);
                    if(trail>.1) {
                        if(trail>.55)faceBottom=-100;
                        // Narrow worn-earth ramps visibly link the terraces to existing passes.
                        double mix=Math.min(.55,trail*.55);
                        color=0xff000000 | ((int)(((color>>16)&255)*(1-mix)+151*mix)<<16)
                            | ((int)(((color>>8)&255)*(1-mix)+130*mix)<<8)
                            | (int)((color&255)*(1-mix)+91*mix);
                    }
                    if(wy<faceBottom) {
                        double down=wy-faceTop;
                        int bank=material.pixel(wx,faceTop,down,drop);
                        if(down<Math.min(drop*.3,.045) && material!=ElevationMaterial.FROST)
                            bank=shade(color,.68);
                        image.setRGB(x,y,bank);
                    } else {
                        double light=1;
                        if(wy<faceBottom+.08)light=.84+.16*(wy-faceBottom)/.08;
                        if(h-previous>.1)light*=.87;
                        if(h>0 && (field.heightAt(wx-.045,wy)<h-.1||field.heightAt(wx+.045,wy)<h-.1))light*=.87;
                        image.setRGB(x,y,shade(color,light));
                    }
                }
                previous=h;
            }
        }
    }
    private static int shade(int rgb,double light) {
        return 0xff000000 | ((int)(((rgb>>16)&255)*light)<<16)
            | ((int)(((rgb>>8)&255)*light)<<8) | (int)((rgb&255)*light);
    }
}
