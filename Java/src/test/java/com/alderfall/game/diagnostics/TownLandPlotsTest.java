package com.alderfall.game.map;

import com.alderfall.game.*;
import java.util.*;

public final class TownLandPlotsTest {
    public static void main(String[] args) {
        Set<TilePoint> land = new LinkedHashSet<>();
        // Two banks beside a bent, two-tile-wide road, with a notch on the western bank.
        for (int y=0; y<18; y++) for (int x=0; x<24; x++) {
            int road = y < 8 ? 9 : 12;
            if (x==road || x==road+1 || x<4 && y<5) continue;
            land.add(new TilePoint(x,y));
        }
        var pieces = TownLandPlots.partition(land,64);
        Set<TilePoint> seen = new HashSet<>();
        boolean irregular = false;
        for (var cells : pieces) {
            require(cells.size()<=64 && connected(cells), "Disconnected or oversized parcel");
            for (TilePoint p : cells) require(land.contains(p) && seen.add(p), "Overlap or road consumed");
            irregular |= irregular(cells);
        }
        require(seen.equals(land) && irregular, "Subdivision lost land or ignored the road shape");
        List<TilePoint> reverse = new ArrayList<>(land); Collections.reverse(reverse);
        require(pieces.equals(TownLandPlots.partition(new LinkedHashSet<>(reverse),64)), "Insertion order changed subdivision");
        Set<TilePoint> square = new LinkedHashSet<>();
        for(int y=0;y<8;y++)for(int x=0;x<8;x++)square.add(new TilePoint(x,y));
        var outline=TownLandPlots.borderVertices(square);
        require(outline.size()==24, "Square border is not a single inset contour");
        for(TilePoint p:outline) {
            long neighbors=Arrays.stream(TownLandPlots.STEPS).filter(d->outline.contains(new TilePoint(p.x()+d[0],p.y()+d[1]))).count();
            require(neighbors==2,"Border produces hedge junctions on a simple contour");
        }
        for (long seed : new long[]{0,42,2026}) {
            WorldMap world = new WorldMap(seed);
            int irregularCount=0;
            for (var site : world.settlementSites()) {
                if (!site.id().startsWith("town_")) continue;
                var area = world.area(site.id()); seen.clear();
                require(area.townPlots().size()>=3, "Missing plot subdivision " + site.id());
                require(area.townPlots().stream().map(TownLandPlots.Plot::use).distinct().count()>=2,
                        "No plot identity variation " + site.id());
                for (var plot : area.townPlots()) {
                    require(connected(plot.cells()), "Disconnected plot " + site.id());
                    require(world.cityBuildings(site.id()).stream().anyMatch(b->b.key().equals(plot.owner())), "Unknown owner");
                    for (TilePoint p : plot.cells()) {
                        require(seen.add(p), "Overlapping plots");
                        require(!Terrain.connectingRoad(area.tileAt(p.x(),p.y())), "Plot consumed street");
                        require(world.cityBuildingAt(site.id(),p.x(),p.y())==null, "Plot consumed a building");
                        require(world.transitionAt(site.id(),p.x(),p.y())==null, "Plot consumed an entrance");
                    }
                    for (TilePoint p : plot.path()) {
                        require(plot.cells().contains(p) && area.propAt(p.x(),p.y())==null, "Furnished the access spine");
                        require(PropCollision.clear(world,site.id(),p.x()+.5,p.y()+.5), "Blocked plot access");
                    }
                    for (int i=1;i<plot.path().size();i++) {
                        TilePoint a=plot.path().get(i-1),b=plot.path().get(i);
                        require(PropCollision.canTravel(world,site.id(),a.x()+.5,a.y()+.5,b.x()+.5,b.y()+.5), "Broken plot path");
                    }
                    if (irregular(plot.cells())) irregularCount++;
                }
                System.out.println(seed+" "+site.id()+": "+area.townPlots().size()+" owned plots");
            }
            require(irregularCount>=7, "Generated parcels lost irregular shapes");
        }
        System.out.println("TownLandPlotsTest passed: coverage, connectivity, identity, road shapes and access.");
    }

    private static boolean irregular(Set<TilePoint> cells) {
        int width=cells.stream().mapToInt(TilePoint::x).max().orElseThrow()-cells.stream().mapToInt(TilePoint::x).min().orElseThrow()+1;
        int height=cells.stream().mapToInt(TilePoint::y).max().orElseThrow()-cells.stream().mapToInt(TilePoint::y).min().orElseThrow()+1;
        return cells.size()<width*height;
    }
    private static boolean connected(Set<TilePoint> cells) {
        Set<TilePoint> seen=new HashSet<>();ArrayDeque<TilePoint> queue=new ArrayDeque<>();
        TilePoint start=cells.iterator().next();queue.add(start);seen.add(start);
        while(!queue.isEmpty()) {TilePoint p=queue.removeFirst();for(int[] d:TownLandPlots.STEPS) {
            TilePoint n=new TilePoint(p.x()+d[0],p.y()+d[1]);if(cells.contains(n)&&seen.add(n))queue.add(n);
        }}
        return seen.size()==cells.size();
    }
    private static void require(boolean value,String message) {if(!value)throw new IllegalStateException(message);}
}
