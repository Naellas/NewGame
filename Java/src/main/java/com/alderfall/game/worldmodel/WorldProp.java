package com.alderfall.game;

/** Visual offsets are reference pixels at 48 pixels per tile; solid prop footprints follow these offsets. */
public record WorldProp(int x, int y, String asset, int size, int visualSlot, int offsetX, int offsetY) {
    public WorldProp shifted(int dx,int dy) {
        int ox=offsetX+dx,oy=offsetY+dy;
        int tx=Math.floorDiv(ox+24,48),ty=Math.floorDiv(oy+24,48);
        return new WorldProp(x+tx,y+ty,asset,size,visualSlot,ox-tx*48,oy-ty*48);
    }
    public WorldProp(int x,int y,String asset,int size,int visualSlot) { this(x,y,asset,size,visualSlot,0,0); }
    public WorldProp(int x,int y,String asset,int size) { this(x,y,asset,size,-1,0,0); }
    public WorldProp {
        if (Math.abs((long)offsetX)>48 || Math.abs((long)offsetY)>48) throw new IllegalArgumentException("Offsets must be between -48 and 48.");
    }
}
