package com.alderfall.game;

import java.awt.image.BufferedImage;

/** Source-art ownership for the three calibrated right-facing study actors. */
final class ConceptPartMasks {
    static boolean[][] cloak(BufferedImage source,String name) {
        int w=source.getWidth(),h=source.getHeight();
        boolean[][] mask=new boolean[h][w], seed=new boolean[h][w];
        boolean knight=name.startsWith("class_knight"),seraphine=name.startsWith("npc_seraphine");
        if(!knight&&!seraphine)return mask;
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
