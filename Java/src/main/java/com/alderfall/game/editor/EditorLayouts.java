package com.alderfall.game.editor;

import com.alderfall.game.*;
import java.util.*;

/** Reproducible editable starting points, using the village building catalogue. */
public final class EditorLayouts {
    private EditorLayouts() {}
    public static MapDocument create(String label,String kind,int width,int height,long seed,boolean generate) {
        char floor=switch(kind) { case "dungeon" -> 'x'; case "interior" -> 'i'; default -> 'g'; };
        MapDocument d=new MapDocument(label,kind,width,height,floor); d.seed=seed;
        if (!generate) {
            if (kind.equals("dungeon")) for (int y=1;y<height-1;y++) Arrays.fill(d.tiles[y],1,width-1,'d');
            if (kind.equals("interior")) border(d,'o');
            return d;
        }
        Random random=new Random(seed);
        if (kind.equals("dungeon")) dungeon(d,random);
        else if (kind.equals("interior")) { border(d,'o'); d.landmarks.put(new TilePoint(d.spawnX,d.spawnY),"Entrance"); }
        else settlement(d,random);
        return d;
    }
    private static void settlement(MapDocument d,Random r) {
        int cx=d.width()/2,cy=d.height()/2;
        for (int y=1;y<d.height()-1;y++) for (int x=1;x<d.width()-1;x++)
            if (Math.abs(x-cx)<=1 || Math.abs(y-cy)<=1 || x%14<2 || y%12<2) d.tiles[y][x]=d.kind.equals("city")?'K':'8';
        if (d.kind.equals("city")) { border(d,'x'); d.tiles[0][cx]='c'; d.tiles[d.height()-1][cx]='c'; d.tiles[cy][0]='c'; d.tiles[cy][d.width()-1]='c'; }
        var plans=VillageManager.buildingPlans().stream().filter(b -> b.width()<=8 && b.depth()<=6).toList();
        for (int y=3;y+8<d.height();y+=12) for (int x=3;x+10<d.width();x+=14) {
            var b=plans.get(r.nextInt(plans.size()));
            if (x<=cx+2 && x+b.width()>=cx-2 || y<=cy+2 && y+b.depth()>=cy-2) continue;
            if (d.placeBuilding(b.style(),x,y,CityBuilding.Facing.SOUTH)) {
                int index=d.buildings.size()-1;
                CityBuilding placed=d.buildings.get(index);
                d.buildings.set(index,new CityBuilding("layout_"+index,placed.x1(),placed.y1(),placed.x2(),placed.y2(),placed.style(),placed.palette(),placed.facing()));
            }
            int door=x+b.width()/2;
            for (int yy=y+b.depth();yy<Math.min(y+11,d.height()-1);yy++) d.tiles[yy][door]='8';
        }
        d.spawnX=cx; d.spawnY=cy;
        d.landmarks.put(new TilePoint(cx,cy),"Market square");
        d.landmarks.put(new TilePoint(Math.max(1,cx/2),Math.max(1,cy/2)),"Residential quarter");
        d.landmarks.put(new TilePoint(Math.min(d.width()-2,cx+cx/2),Math.max(1,cy/2)),"Crafts quarter");
    }
    private static void dungeon(MapDocument d,Random r) {
        List<TilePoint> centers=new ArrayList<>();
        for (int y=2;y+5<d.height();y+=12) for (int x=2;x+5<d.width();x+=14) {
            int w=Math.min(6+r.nextInt(5),d.width()-x-1), h=Math.min(5+r.nextInt(4),d.height()-y-1);
            for (int yy=y;yy<y+h;yy++) Arrays.fill(d.tiles[yy],x,x+w,'d');
            TilePoint c=new TilePoint(x+w/2,y+h/2);
            if (!centers.isEmpty()) {
                TilePoint prev=centers.get(centers.size()-1);
                for (int xx=Math.min(prev.x(),c.x());xx<=Math.max(prev.x(),c.x());xx++) d.tiles[prev.y()][xx]='d';
                for (int yy=Math.min(prev.y(),c.y());yy<=Math.max(prev.y(),c.y());yy++) d.tiles[yy][c.x()]='d';
            }
            centers.add(c); d.landmarks.put(c,"Chamber " + centers.size());
        }
        if (centers.isEmpty()) { d.tiles[d.spawnY][d.spawnX]='d'; return; }
        TilePoint entrance=centers.get(0),boss=centers.get(centers.size()-1);
        d.spawnX=entrance.x(); d.spawnY=entrance.y(); d.landmarks.put(entrance,"Entrance");
        if (centers.size()>1) { d.tiles[boss.y()][boss.x()]='S'; d.landmarks.put(boss,"Boss chamber (layout marker)"); }
    }
    private static void border(MapDocument d,char c) {
        Arrays.fill(d.tiles[0],c); Arrays.fill(d.tiles[d.height()-1],c);
        for (char[] row:d.tiles) { row[0]=c; row[d.width()-1]=c; }
    }
}
