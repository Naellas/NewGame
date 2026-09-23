package com.alderfall.game.ui;

import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.util.Arrays;

/** Exclusive, feathered attachment ownership; rasterized once per fitted artwork. */
final class DialoguePartMask {
    final byte[] owner;
    final float[] weight;
    int left, right;
    DialoguePartMask(DialogueRigProfile profile, Rectangle art, int width, int height) {
        owner=new byte[width*height];Arrays.fill(owner,(byte)-1);
        left=width;right=0;
        weight=new float[owner.length];
        double scale=art.height/1000.0;
        if(scale<=0)return;
        for(int partIndex=0;partIndex<profile.parts().size();partIndex++) {
            var part=profile.parts().get(partIndex);double[] polygon=part.polygon();
            Path2D path=new Path2D.Double();path.moveTo(polygon[0],polygon[1]);
            for(int i=2;i<polygon.length;i+=2)path.lineTo(polygon[i],polygon[i+1]);path.closePath();
            var box=path.getBounds2D();
            int left=Math.max(0,(int)(art.x+box.getMinX()*scale)),right=Math.min(width,(int)Math.ceil(art.x+box.getMaxX()*scale));
            int top=Math.max(0,(int)(art.y+box.getMinY()*scale)),bottom=Math.min(height,(int)Math.ceil(art.y+box.getMaxY()*scale));
            for(int y=top;y<bottom;y++)for(int x=left;x<right;x++) {
                double px=(x-art.x)/scale,py=(y-art.y)/scale;
                if(!path.contains(px,py))continue;
                double edge=Double.POSITIVE_INFINITY;
                for(int i=0;i<polygon.length;i+=2) {
                    int next=(i+2)%polygon.length;
                    edge=Math.min(edge,java.awt.geom.Line2D.ptSegDist(polygon[i],polygon[i+1],polygon[next],polygon[next+1],px,py));
                }
                double t=Math.min(1,edge/part.feather());float w=(float)(t*t*(3-2*t));
                int pixel=y*width+x;
                if(w>weight[pixel]){owner[pixel]=(byte)partIndex;weight[pixel]=w;this.left=Math.min(this.left,x);this.right=Math.max(this.right,x+1);}
            }
        }
    }
}
