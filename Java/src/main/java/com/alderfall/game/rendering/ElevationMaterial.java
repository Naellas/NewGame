package com.alderfall.game;

/** World-aligned bank materials: distinct strata, joints, roots and caps, without tiled sprites. */
public enum ElevationMaterial {
    EARTH(116,96,64), ROOTS(82,78,47), GRANITE(116,121,125), FROST(133,148,157),
    SANDSTONE(175,139,89), SHALE(133,83,61), PEAT(66,67,47), COAST(172,166,136), MASONRY(131,127,113);
    private final int r,g,b;
    ElevationMaterial(int r,int g,int b){this.r=r;this.g=g;this.b=b;}
    public static ElevationMaterial forTerrain(char t,String region) {
        return switch(t) {
            case 'f' -> ROOTS;case 'n' -> FROST;case 's' -> SANDSTONE;case 'b' -> SHALE;
            case 'v' -> PEAT;case 'P','~' -> COAST;
            case 'm','q' -> "highwall".equals(region)||"northroad".equals(region)?FROST:
                "sanctum".equals(region)?SANDSTONE:GRANITE;
            case 'K','C','G','p' -> MASONRY;default -> EARTH;
        };
    }
    private static double hash(int x,int y){int n=x*374761393+y*668265263;n=(n^(n>>>13))*1274126177;return ((n^(n>>>16))&255)/255.0;}
    public int pixel(double worldX,double worldY,double down,double drop) {
        int x=(int)Math.floor(worldX*48),y=(int)Math.floor(worldY*48),v=(int)Math.floor(down*48);
        double t=Math.min(1,down/Math.max(.02,drop));
        double detail=(hash(x,y+v)-.5)*13+(hash(x/4,(y+v)/3)-.5)*8;
        double strata=Math.sin(v*.72+Math.sin(x*.026)*2)*4;
        if(this==SANDSTONE||this==SHALE)detail+=strata*2;
        if(this==MASONRY) {
            int band=Math.floorDiv(y+v,11),joint=Math.floorMod(x+(band%2)*13,27);
            if(joint<2||Math.floorMod(y+v,11)==0)detail-=25;
        }
        if(this==GRANITE||this==FROST) {
            double fracture=Math.sin(x*.105+Math.sin((y+v)*.047)*.8);
            double bedding=Math.sin((y+v)*.14+Math.sin(x*.055)*1.2);
            if(Math.abs(fracture)<.08||Math.abs(bedding)<.09)detail-=24;
            detail+=(hash(x/12,(y+v)/9)-.5)*18;
        }
        if(this==COAST)detail+=Math.sin(v*.5+Math.sin(x*.04))*5;
        if(this==ROOTS && Math.floorMod(x+(int)(Math.sin(v*.14+x/29)*5),23)<2)detail-=28;
        if(this==PEAT)detail+=Math.sin(x*.4+v*.14)*6-4*t;
        double light=.98-.22*t;
        int red=(int)((r+detail)*light),green=(int)((g+detail)*light),blue=(int)((b+detail)*light);
        if(this==FROST && v<2+(int)(hash(x/3,y)*4)){red=195;green=207;blue=208;}
        return 0xff000000 | Math.max(0,Math.min(255,red))<<16 | Math.max(0,Math.min(255,green))<<8 | Math.max(0,Math.min(255,blue));
    }
}
