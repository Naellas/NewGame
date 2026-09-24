package com.alderfall.game.map;

import com.alderfall.game.*;
import java.util.*;

/** Connected parcels cut from the land left by roads, foundations and reserved public spaces. */
public final class TownLandPlots {
    private TownLandPlots() { }
    static final int[][] STEPS = {{0,-1},{1,0},{0,1},{-1,0}};
    static final Comparator<TilePoint> ORDER = Comparator.comparingInt(TilePoint::y).thenComparingInt(TilePoint::x);
    public enum Use { HOUSE_GARDEN, HERB_GARDEN, CIVIC_GARDEN, TRADE_YARD }
    public record Plot(String owner, Use use, Set<TilePoint> cells, List<TilePoint> path) {
        public Plot { cells = Collections.unmodifiableSet(new LinkedHashSet<>(cells)); path = List.copyOf(path); }
        public TilePoint entrance() { return path.getFirst(); }
        public TilePoint center() { return path.getLast(); }
    }

    static List<Plot> plan(Set<TilePoint> land, List<CityBuilding> buildings, Set<TilePoint> streetEdges) {
        List<Set<TilePoint>> parcels = partition(land, 64);
        parcels.sort(Comparator.<Set<TilePoint>>comparingInt(Set::size).reversed()
                .thenComparing(p -> p.iterator().next(), ORDER));
        List<Plot> result = new ArrayList<>();
        for (Set<TilePoint> cells : parcels) {
            if (cells.size() < 14 || result.size() >= 14) continue;
            if (cells.stream().filter(p -> interior(cells,p) == 9).count() < 4) continue;
            double meanX = cells.stream().mapToInt(TilePoint::x).average().orElseThrow();
            double meanY = cells.stream().mapToInt(TilePoint::y).average().orElseThrow();
            // A usable interior near the parcel's centroid gives it an intentional focus.
            TilePoint center = cells.stream().max(Comparator.<TilePoint>comparingInt(p -> interior(cells, p))
                    .thenComparingDouble(p -> -Math.abs(p.x()-meanX)-Math.abs(p.y()-meanY))
                    .thenComparing(ORDER.reversed())).orElseThrow();
            TilePoint entrance = cells.stream().filter(streetEdges::contains)
                    .min(Comparator.<TilePoint>comparingInt(p -> Math.abs(p.x()-center.x())+Math.abs(p.y()-center.y()))
                            .thenComparing(ORDER)).orElse(null);
            if (entrance == null) continue;
            Map<TilePoint, TilePoint> previous = paths(cells, entrance);
            List<TilePoint> path = new ArrayList<>();
            for (TilePoint p = center; p != null; p = previous.get(p)) path.add(p);
            Collections.reverse(path);
            CityBuilding owner = buildings.stream().min(Comparator.<CityBuilding>comparingInt(b ->
                    Math.max(0, Math.max(b.x1()-center.x(), center.x()-b.x2()))
                    + Math.max(0, Math.max(b.y1()-center.y(), center.y()-b.y2())))
                    .thenComparing(CityBuilding::key)).orElseThrow();
            Use use = switch (owner.style()) {
                case "house", "row" -> Use.HOUSE_GARDEN;
                case "alchemist", "apothecary" -> Use.HERB_GARDEN;
                case "blacksmith", "carpenter", "workshop", "warehouse", "fishing_hut", "shop", "market", "inn" -> Use.TRADE_YARD;
                default -> Use.CIVIC_GARDEN;
            };
            result.add(new Plot(owner.key(), use, cells, path));
        }
        return List.copyOf(result);
    }

    /** Road-shaped components are split by competing flood fills, never by rectangular clipping. */
    static List<Set<TilePoint>> partition(Set<TilePoint> land, int maximum) {
        if (maximum < 2) throw new IllegalArgumentException("Maximum parcel size must be at least two");
        Set<TilePoint> remaining = new LinkedHashSet<>(land.stream().sorted(ORDER).toList());
        List<Set<TilePoint>> result = new ArrayList<>();
        while (!remaining.isEmpty()) {
            Set<TilePoint> component = paths(remaining, remaining.iterator().next()).keySet();
            remaining.removeAll(component);
            split(new LinkedHashSet<>(component), maximum, result);
        }
        return result;
    }

    private static void split(Set<TilePoint> cells, int maximum, List<Set<TilePoint>> result) {
        if (cells.size() <= maximum) { result.add(cells); return; }
        TilePoint a = last(paths(cells, cells.iterator().next()));
        TilePoint b = last(paths(cells, a));
        Map<TilePoint, Integer> owners = new HashMap<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        owners.put(a, 0); owners.put(b, 1); queue.add(a); queue.add(b);
        List<Set<TilePoint>> halves = List.of(new LinkedHashSet<>(), new LinkedHashSet<>());
        while (!queue.isEmpty()) {
            TilePoint p = queue.removeFirst(); int owner = owners.get(p); halves.get(owner).add(p);
            for (int[] d : STEPS) {
                TilePoint n = new TilePoint(p.x()+d[0], p.y()+d[1]);
                if (cells.contains(n) && !owners.containsKey(n)) { owners.put(n, owner); queue.add(n); }
            }
        }
        for (Set<TilePoint> half : halves) split(half, maximum, result);
    }

    private static TilePoint last(Map<TilePoint, TilePoint> visited) {
        TilePoint result = null;
        for (TilePoint p : visited.keySet()) result = p;
        return result;
    }

    private static Map<TilePoint, TilePoint> paths(Set<TilePoint> cells, TilePoint start) {
        Map<TilePoint, TilePoint> previous = new LinkedHashMap<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>(); previous.put(start, null); queue.add(start);
        while (!queue.isEmpty()) {
            TilePoint p = queue.removeFirst();
            for (int[] d : STEPS) {
                TilePoint n = new TilePoint(p.x()+d[0], p.y()+d[1]);
                if (cells.contains(n) && !previous.containsKey(n)) { previous.put(n, p); queue.add(n); }
            }
        }
        return previous;
    }

    /** Trace the edges of an inset core on the vertex lattice; this avoids thick corner grids. */
    static Set<TilePoint> borderVertices(Set<TilePoint> cells) {
        Set<TilePoint> core = new LinkedHashSet<>();
        for (TilePoint p : cells) if (interior(cells,p) == 9) core.add(p);
        // Remove one-cell fingers and isolated tiny cores before tracing a garden border.
        Set<TilePoint> broad = new LinkedHashSet<>();
        for (TilePoint p : core) {
            TilePoint east=new TilePoint(p.x()+1,p.y()), south=new TilePoint(p.x(),p.y()+1), corner=new TilePoint(p.x()+1,p.y()+1);
            if(core.contains(east)&&core.contains(south)&&core.contains(corner)) {
                broad.add(p); broad.add(east); broad.add(south); broad.add(corner);
            }
        }
        core.retainAll(broad);
        if (core.isEmpty()) return Set.of();
        Set<TilePoint> largest = Set.of(), remaining = new LinkedHashSet<>(core);
        while(!remaining.isEmpty()) {
            Set<TilePoint> part = paths(remaining,remaining.iterator().next()).keySet();
            if(part.size()>largest.size())largest=new LinkedHashSet<>(part);
            remaining.removeAll(part);
        }
        core = largest;
        Set<TilePoint> vertices = new LinkedHashSet<>();
        for (TilePoint p : core) {
            int x=p.x(), y=p.y();
            if (!core.contains(new TilePoint(x,y-1))) { vertices.add(new TilePoint(x,y)); vertices.add(new TilePoint(x+1,y)); }
            if (!core.contains(new TilePoint(x+1,y))) { vertices.add(new TilePoint(x+1,y)); vertices.add(new TilePoint(x+1,y+1)); }
            if (!core.contains(new TilePoint(x,y+1))) { vertices.add(new TilePoint(x,y+1)); vertices.add(new TilePoint(x+1,y+1)); }
            if (!core.contains(new TilePoint(x-1,y))) { vertices.add(new TilePoint(x,y)); vertices.add(new TilePoint(x,y+1)); }
        }
        // Joined sprites connect to every adjacent node. Drop ambiguous junctions at
        // tight notches instead of drawing a lattice across their one-tile gaps.
        Set<TilePoint> junctions = new HashSet<>();
        for(TilePoint p:vertices) {
            int neighbors=0;
            for(int[] d:STEPS)if(vertices.contains(new TilePoint(p.x()+d[0],p.y()+d[1])))neighbors++;
            if(neighbors>2)junctions.add(p);
        }
        vertices.removeAll(junctions);
        return vertices;
    }

    static int interior(Set<TilePoint> cells, TilePoint p) {
        int count = 0;
        for (int dy=-1; dy<=1; dy++) for (int dx=-1; dx<=1; dx++)
            if (cells.contains(new TilePoint(p.x()+dx,p.y()+dy))) count++;
        return count;
    }
}
