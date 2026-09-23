package com.alderfall.game;

import java.awt.image.BufferedImage;

/** Reviewed garment, carrying-pose and equipment ownership for source artwork. */
final class GroundedPartMasks {
    static boolean carryingPose(String name) {
        return name.matches("npc_(?:baker|bartender|blacksmith)_model_.*")
                || name.matches("npc_calder_model_(?:up|down)_(?:left|right)");
    }
    static boolean equipment(String name,int x,int y) {
        double[] p=switch(name) {
            case "npc_calder_model_down_left" -> new double[]{117,80,99,123};
            case "npc_calder_model_down_right" -> new double[]{93,83,75,116};
            case "npc_calder_model_up_left" -> new double[]{85,83,109,120};
            case "npc_calder_model_up_right" -> new double[]{114,85,121,121};
            default -> null;
        };
        if(p==null)return false;
        double dx=p[2]-p[0],dy=p[3]-p[1],t=Math.max(0,Math.min(1,((x-p[0])*dx+(y-p[1])*dy)/(dx*dx+dy*dy)));
        return Math.hypot(x-p[0]-t*dx,y-p[1]-t*dy)<3.5
                || Math.pow((x-p[2])/14,2)+Math.pow((y-p[3])/10,2)<1;
    }
    static boolean[][] cloak(BufferedImage source,String name) {
        int w=source.getWidth(),h=source.getHeight();
        boolean[][] mask=new boolean[h][w], seed=new boolean[h][w];
        boolean knight=name.startsWith("class_knight"),seraphine=name.startsWith("npc_seraphine");
        if(name.startsWith("class_ranger")||name.startsWith("class_rogue")) {
            for(int y=78;y<Math.min(h,128);y++)for(int x=0;x<w;x++) {
                int p=source.getRGB(x,y),r=(p>>16)&255,g=(p>>8)&255,b=p&255;
                seed[y][x]=(p>>>24)>32&&g>r*1.08&&g>b*1.10;
            }
            for(int y=78;y<Math.min(h,129);y++)for(int x=1;x<w-1;x++)if((source.getRGB(x,y)>>>24)>32) {
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)if(y+dy<h&&seed[y+dy][x+dx])mask[y][x]=true;
            }
            return mask;
        }
        if((!knight&&!seraphine)||!name.endsWith("_model_right"))return mask;
        // These silhouettes are specific to the normalized 192x144 standing sources.
        // Keep the red fabric and its dark/gold border, excluding the arm and exposed leg.
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            boolean region=knight ? y>=60&&y<=118&&x<= (y<94?100:y<110?96:89)
                : y>=49&&y<=124&&x<= (y<90?102:y<110?98:92);
            int p=source.getRGB(x,y),r=(p>>16)&255,g=(p>>8)&255,b=p&255;
            seed[y][x]=region&&(p>>>24)>32&&r>40&&r>g*1.65&&r>b*1.4;
        }
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            if((source.getRGB(x,y)>>>24)<32)continue;
            for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++) {
                int xx=x+dx,yy=y+dy;
                if(xx>=0&&xx<w&&yy>=0&&yy<h&&dx*dx+dy*dy<=5&&seed[yy][xx])mask[y][x]=true;
            }
        }
        return mask;
    }
}
