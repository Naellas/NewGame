package com.alderfall.game.map;

import com.alderfall.game.*;
import java.util.*;

/** Gate-to-square streets, quieter feeder lanes and continuous public paving. */
public final class TownStreets {
    private TownStreets() { }
    private static final int[][] STEPS = {{0,-1},{1,0},{0,1},{-1,0}};
    public static boolean square(MapArea area, int x, int y) {
        if (!area.id.startsWith("town_")) return false;
        for (var e : area.landmarks.entrySet())
            if ((e.getValue().equals("Civic Plaza") || e.getValue().equals("Market Square"))
                    && Math.abs(x - e.getKey().x()) <= 6 && Math.abs(y - e.getKey().y()) <= 3) return true;
        return false;
    }
    static void finish(WorldMap world, MapArea area) {
        TilePoint center = area.landmarks.entrySet().stream().filter(e -> e.getValue().equals("Civic Plaza"))
                .map(Map.Entry::getKey).findFirst().orElseThrow();
        Map<TilePoint, TilePoint> previous = new HashMap<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        previous.put(center, null); queue.add(center);
        while (!queue.isEmpty()) {
            TilePoint p = queue.remove();
            for (int[] d : STEPS) {
                TilePoint n = new TilePoint(p.x()+d[0], p.y()+d[1]);
                char tile = area.tileAt(n.x(), n.y());
                if (previous.containsKey(n) || !street(tile) || !world.isPassable(area.id,n.x(),n.y())) continue;
                previous.put(n,p); queue.add(n);
            }
        }
        Set<TilePoint> main = new HashSet<>();
        for (TilePoint end : previous.keySet()) {
            String label = area.landmarks.get(end);
            if (area.tileAt(end.x(),end.y()) != Terrain.CITY_GATE
                    && !Set.of("Market Square", "Public Garden").contains(label == null ? "" : label)) continue;
            for (TilePoint p=end; p!=null; p=previous.get(p)) main.add(p);
        }
        // Distance follows the street network, so private lanes do not become arterial just by proximity.
        Map<TilePoint,Integer> distance = new HashMap<>(); queue.clear();
        main.stream().sorted(Comparator.comparingInt(TilePoint::y).thenComparingInt(TilePoint::x)).forEach(p->{distance.put(p,0);queue.add(p);});
        while (!queue.isEmpty()) {
            TilePoint p=queue.remove();
            for(int[] d:STEPS) {
                TilePoint n=new TilePoint(p.x()+d[0],p.y()+d[1]);
                if(distance.containsKey(n)||!previous.containsKey(n))continue;
                distance.put(n,distance.get(p)+1);queue.add(n);
            }
        }
        for(int y=0;y<area.height();y++)for(int x=0;x<area.width();x++) {
            char tile=area.tileAt(x,y);
            if("rTK8".indexOf(tile)<0)continue; // Keep gates, bridges and regional boardwalks.
            TilePoint p=new TilePoint(x,y);
            area.tiles[y][x]=main.contains(p)?'K':distance.getOrDefault(p,99)<=6?'T':'8';
        }
        area.props.replaceAll(p -> {
            if (!lamp(p.asset())) return p;
            TilePoint side = roadSide(area, p.x(), p.y());
            String facing = side.x() < 0 ? "left" : side.x() > 0 ? "right" : "iron";
            return new WorldProp(p.x(), p.y(), "city_prop_refresh_lamp_" + facing, p.size(), p.visualSlot());
        });
        area.markVisualChange();
    }
    public static boolean lamp(String asset) {
        return asset.startsWith("city_prop_refresh_lamp_") || asset.equals("city_prop_street_lamp");
    }
    public static TilePoint roadSide(MapArea area, int x, int y) {
        for (int radius = 1; radius <= 3; radius++) for (int[] d : STEPS)
            if (Terrain.connectingRoad(area.tileAt(x + d[0]*radius, y + d[1]*radius)))
                return new TilePoint(d[0], d[1]);
        return new TilePoint(0,0);
    }
    private static boolean street(char t) { return Terrain.connectingRoad(t)||"pjalCGU".indexOf(t)>=0; }
}
