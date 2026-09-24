package com.alderfall.game.ui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/** Inverse deformation sampled on a grid, textured with native Java2D cubic interpolation. */
final class DialogueDeformationMesh {
    interface Warp { void map(double x,double y,double[] result,int index); }
    static void draw(BufferedImage source,BufferedImage target,Rectangle viewport,int[] first,int[] last,Rectangle[] details,Warp warp,int step) {
        if(viewport.isEmpty())return;
        int width=source.getWidth(),height=source.getHeight();
        Graphics2D g=target.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        Path2D.Double triangle=new Path2D.Double();
        int tile=step*4;
        for(int y=viewport.y/tile*tile;y<viewport.y+viewport.height;y+=tile) {
            int y1=Math.min(height,y+tile),start=width,end=-1;
            for(int row=y;row<y1;row++){start=Math.min(start,first[row]);end=Math.max(end,last[row]);}
            for(int x=viewport.x/tile*tile;x<viewport.x+viewport.width;x+=tile) {
                if(x+tile<start||x>end)continue;
                cell(g,source,viewport,triangle,details,warp,x,y,Math.min(width,x+tile),y1,Math.max(4,step/3));
            }
        }
        g.dispose();
    }
    private static void cell(Graphics2D g,BufferedImage source,Rectangle viewport,Path2D.Double clip,Rectangle[] details,Warp warp,int x0,int y0,int x1,int y1,int minimum) {
        if(x1<=viewport.x||x0>=viewport.x+viewport.width||y1<=viewport.y||y0>=viewport.y+viewport.height)return;
        int mx=(x0+x1)/2,my=(y0+y1)/2;
        double[] p=new double[18];
        warp.map(x0,y0,p,0);warp.map(x1,y0,p,2);warp.map(x1,y1,p,4);warp.map(x0,y1,p,6);
        warp.map(mx,y0,p,8);warp.map(x1,my,p,10);warp.map(mx,y1,p,12);warp.map(x0,my,p,14);warp.map(mx,my,p,16);
        double tx=(mx-x0)/(double)(x1-x0),ty=(my-y0)/(double)(y1-y0),error=0;
        for(int i=0;i<4;i++)for(int axis=0;axis<2;axis++) {
            double t=i==0?tx:i==1?ty:i==2?1-tx:1-ty;
            error=Math.max(error,Math.abs(p[8+i*2+axis]-(p[i*2+axis]*(1-t)+p[((i+1)%4)*2+axis]*t)));
        }
        for(int axis=0;axis<2;axis++) {
            double predicted=tx>=ty?p[axis]*(1-tx)+p[2+axis]*(tx-ty)+p[4+axis]*ty:p[axis]*(1-ty)+p[4+axis]*tx+p[6+axis]*(ty-tx);
            error=Math.max(error,Math.abs(p[16+axis]-predicted));
        }
        boolean detail=false;
        if(x1-x0>minimum*2 && y1-y0>minimum*2)for(Rectangle region:details)if(region.intersects(x0,y0,x1-x0,y1-y0)){detail=true;break;}
        if((detail||error>.10) && x1-x0>minimum && y1-y0>minimum) {
            cell(g,source,viewport,clip,details,warp,x0,y0,mx,my,minimum);cell(g,source,viewport,clip,details,warp,mx,y0,x1,my,minimum);
            cell(g,source,viewport,clip,details,warp,x0,my,mx,y1,minimum);cell(g,source,viewport,clip,details,warp,mx,my,x1,y1,minimum);
        } else {
            if(Math.abs(p[0]+p[4]-p[2]-p[6])+Math.abs(p[1]+p[5]-p[3]-p[7])<.06) {
                // A rigid/affine quad needs one texture pass instead of two triangle bounds.
                patch(g,source,viewport,clip,p,0,2,6,x0,y0,x1,y0,x0,y1,true);
            } else {
                patch(g,source,viewport,clip,p,0,2,4,x0,y0,x1,y0,x1,y1,false);
                patch(g,source,viewport,clip,p,0,4,6,x0,y0,x1,y1,x0,y1,false);
            }
        }
    }
    private static void patch(Graphics2D g,BufferedImage source,Rectangle viewport,Path2D.Double clip,double[] p,
                                 int a,int b,int c,double ax,double ay,double bx,double by,double cx,double cy,boolean rectangle) {
        double ux=p[b]-p[a],uy=p[b+1]-p[a+1],vx=p[c]-p[a],vy=p[c+1]-p[a+1];
        double determinant=ux*vy-uy*vx;
        if(Math.abs(determinant)<1e-8)return;
        double m00=((bx-ax)*vy-(cx-ax)*uy)/determinant,m01=((cx-ax)*ux-(bx-ax)*vx)/determinant;
        double m10=((by-ay)*vy-(cy-ay)*uy)/determinant,m11=((cy-ay)*ux-(by-ay)*vx)/determinant;
        AffineTransform transform=new AffineTransform(m00,m10,m01,m11,ax-m00*p[a]-m01*p[a+1],ay-m10*p[a]-m11*p[a+1]);
        clip.reset();clip.moveTo(ax,ay);clip.lineTo(bx,by);if(rectangle)clip.lineTo(bx,cy);clip.lineTo(cx,cy);clip.closePath();
        g.setClip(viewport);g.clip(clip);g.drawImage(source,transform,null);
    }
}
