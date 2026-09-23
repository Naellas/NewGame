package com.alderfall.game.map;

import com.alderfall.game.*;
import java.util.*;

/** Trails to real, predefined gameplay destinations; never creates placeholder quest markers. */
public final class DestinationApproaches {
    public record Approach(String id, String label, String kind, String mapId, TilePoint destination,
                           char biome, List<TilePoint> trail, int gapStart, int gapLength) {
        public Approach { trail = List.copyOf(trail); }
        public boolean gap(int segment) { return segment >= gapStart && segment < gapStart + gapLength; }
    }
    private record Target(String id, String label, String kind, String mapId, TilePoint point) { }
    private record Node(TilePoint point, double cost, int steps) { }
    private static final String MAP = WorldMap.OVERWORLD_ID;
    private static final int[][] DIRS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
    private DestinationApproaches() { }

    public static void populate(WorldMap world, int seed) {
        MapArea area = world.area(MAP);
        List<Target> targets = new ArrayList<>();
        for (var marker : world.adventureMarkers()) targets.add(new Target(marker.mapId(), marker.label(),
                marker.kind(), marker.mapId(), new TilePoint(marker.x(), marker.y())));
        for (var marker : world.campaignMarkers()) {
            TilePoint point = world.campaignPlacePoint(marker.id(), 0);
            if (targets.stream().noneMatch(t -> distance(t.point, point) < 9))
                targets.add(new Target("place:" + marker.id(), marker.label(), marker.kind(), MAP, point));
        }
        List<Approach> approaches = new ArrayList<>();
        Set<TilePoint> reserved = new HashSet<>();
        for (var site : area.natureSites()) reserved.addAll(site.trail());
        for (Target target : targets) {
            TilePoint endpoint = endpoint(world, target.point);
            if (endpoint == null) continue;
            List<TilePoint> route = findRoute(world, target, endpoint, seed);
            if (route.size() < 5 || !NatureSiteGenerator.walkable(world, route)) continue;
            int hash = Math.floorMod(target.id.hashCode() ^ seed, 1000);
            Approach approach = new Approach(target.id, target.label, target.kind, target.mapId,
                    target.point, nearbyBiome(area, endpoint), route,
                    hash % 3 == 0 && route.size() > 8 ? route.size() / 2 : -10, hash % 3 == 0 ? 2 : 0);
            approaches.add(approach); reserved.addAll(route);
        }
        // Some quiet clearings become small persistent caches, retaining their original walkable approach.
        List<NatureSiteGenerator.Site> quiet = new ArrayList<>(area.natureSites());
        for (var site : area.natureSites()) {
            if (Math.floorMod(site.x() * 31 + site.y() * 17 + seed, 5) != 0) continue;
            TilePoint end = site.trail().get(site.trail().size() - 1);
            TilePoint chestPoint = freeSide(world, area, end, reserved, site.trail());
            if (chestPoint == null) continue;
            var chest = new WorldProp(chestPoint.x(), chestPoint.y(), "interior_ironbound_chest", 30);
            area.addProp(chest);
            approaches.add(new Approach("cache:" + site.x() + ":" + site.y(), "Forgotten wayfarer's cache",
                    "cache", MAP, chestPoint, site.terrain(), site.trail(), 3, 2));
            reserved.addAll(site.trail()); quiet.remove(site);
        }
        area.setNatureSites(quiet);
        // Dress after reserving every approach so no fence or clue can occupy another destination's route.
        for (Approach approach : approaches) dress(world, area, approach, reserved, seed);
        area.setDestinationApproaches(approaches);
    }

    private static int distance(TilePoint a, TilePoint b) { return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y()); }

    private static TilePoint endpoint(WorldMap world, TilePoint target) {
        if (PropCollision.clear(world, MAP, target.x() + .5, target.y() + .5)) return target;
        // Interaction with a chest/building/objective works from its immediate approach tile.
        for (int[] d : DIRS) {
            TilePoint p = new TilePoint(target.x() + d[0], target.y() + d[1]);
            if (PropCollision.clear(world, MAP, p.x() + .5, p.y() + .5)) return p;
        }
        return null;
    }

    /** Bounded weighted search: natural ground is preferred over laying another road on an existing road. */
    private static List<TilePoint> findRoute(WorldMap world, Target target, TilePoint start, int seed) {
        MapArea area = world.area(MAP);
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::cost)
                .thenComparingInt(n -> n.point.y()).thenComparingInt(n -> n.point.x()));
        Map<TilePoint, Double> costs = new HashMap<>();
        Map<TilePoint, TilePoint> parent = new HashMap<>();
        open.add(new Node(start, 0, 0)); costs.put(start, 0.0); parent.put(start, start);
        TilePoint goal = null, fallback = null;
        int expanded = 0;
        while (!open.isEmpty() && expanded++ < 2400) {
            Node n = open.remove();
            if (n.cost > costs.get(n.point)) continue;
            char tile = area.tileAt(n.point.x(), n.point.y());
            if (n.steps >= 5 && n.steps <= 28 && "rTK78".indexOf(tile) >= 0
                    && distance(n.point, start) >= 5) { goal = n.point; break; }
            if (n.steps >= 10 && fallback == null && distance(n.point, start) >= 8) fallback = n.point;
            if (n.steps >= 28 || distance(start, n.point) > 24) continue;
            for (int[] d : DIRS) {
                TilePoint p = new TilePoint(n.point.x() + d[0], n.point.y() + d[1]);
                char ground = area.tileAt(p.x(), p.y());
                if (NatureSiteGenerator.biome(ground) == null && "rTK78d".indexOf(ground) < 0) continue;
                var transition = world.transitionAt(MAP, p.x(), p.y());
                if (transition != null && !p.equals(target.point)) continue;
                String kind = world.locationKindAt(MAP, p.x(), p.y());
                if (kind != null && !kind.equals(target.kind) && distance(start, p) > 2) continue;
                if (!PropCollision.canTravel(world, MAP, n.point.x() + .5, n.point.y() + .5, p.x() + .5, p.y() + .5)) continue;
                double cost = n.cost + (Terrain.connectingRoad(ground) ? 2.5 : 1)
                        + Math.floorMod(p.x() * 73 + p.y() * 37 + seed, 7) * .05;
                if (cost >= costs.getOrDefault(p, Double.POSITIVE_INFINITY)) continue;
                costs.put(p, cost); parent.put(p, n.point); open.add(new Node(p, cost, n.steps + 1));
            }
        }
        if (goal == null) goal = fallback;
        if (goal == null) return List.of();
        List<TilePoint> route = new ArrayList<>();
        for (TilePoint p = goal; ; p = parent.get(p)) { route.add(p); if (p.equals(start)) break; }
        return route;
    }

    private static char nearbyBiome(MapArea area, TilePoint point) {
        char best = 'g'; int count = -1;
        for (char biome : "gfsnbvPq".toCharArray()) {
            int found = 0;
            for (int y = -5; y <= 5; y++) for (int x = -5; x <= 5; x++)
                if (area.tileAt(point.x() + x, point.y() + y) == biome) found++;
            if (found > count) { count = found; best = biome; }
        }
        return best;
    }

    private static TilePoint freeSide(WorldMap world, MapArea area, TilePoint p, Set<TilePoint> reserved, List<TilePoint> route) {
        for (int[] d : DIRS) {
            TilePoint side = new TilePoint(p.x() + d[0], p.y() + d[1]);
            if (reserved.contains(side) || route.contains(side) || area.propAt(side.x(), side.y()) != null
                    || area.landmarks.containsKey(side) || world.transitionAt(MAP, side.x(), side.y()) != null
                    || NatureSiteGenerator.biome(area.tileAt(side.x(), side.y())) == null
                    || !PropCollision.clear(world, MAP, side.x() + .5, side.y() + .5)) continue;
            return side;
        }
        return null;
    }

    private static void dress(WorldMap world, MapArea area, Approach approach, Set<TilePoint> reserved, int seed) {
        List<TilePoint> trail = approach.trail;
        for (int i = 0; i < trail.size(); i++) {
            TilePoint point = trail.get(i);
            if (!approach.gap(i)) {
                for (var prop : List.copyOf(area.propsAt(point.x(), point.y())))
                    if (prop.visualSlot() >= 0) area.removeProp(prop);
            }
            // Preserve primary roads. Only the final local dirt spur receives a narrower visual surface.
            char ground = area.tileAt(point.x(), point.y());
            if (i > 0 && i >= trail.size() - 7 && (ground == 'r' || ground == 'T')
                    && approach.kind.equals(world.locationKindAt(MAP, point.x(), point.y()))
                    && roadNeighbors(area, point) <= 2)
                area.setApproachGround(point, approach.biome);
            if (i == 1 || i == trail.size() / 2 || i == trail.size() - 2) {
                TilePoint side = freeSide(world, area, point, reserved, trail);
                if (side != null) {
                    String asset = i == 1 ? "deco_road_signpost" : approach.kind.contains("camp")
                            ? "location_camp_crates" : "deco_road_milestone";
                    area.addProp(new WorldProp(side.x(), side.y(), asset, 30));
                    area.landmarks.put(side, (i == 1 ? "Worn trail to " : "Trail marker: ") + approach.label);
                }
            }
            if (i % 3 == 0) {
                TilePoint side = freeSide(world, area, point, reserved, trail);
                if (side != null) {
                    String biome = NatureSiteGenerator.biome(area.tileAt(side.x(), side.y()));
                    area.addProp(new WorldProp(side.x(), side.y(), "deco_ground_" + biome + "_"
                            + (1 + Math.floorMod(i + seed, 8)), 23, i % 4));
                }
            }
        }
        if (Math.floorMod(approach.id.hashCode() + seed, 3) == 0 && trail.size() > 7) {
            int index = trail.size() - 4;
            TilePoint p = trail.get(index), next = trail.get(index + 1);
            int dx = next.x() - p.x(), dy = next.y() - p.y();
            for (int side : new int[]{-1, 1}) {
                TilePoint fence = new TilePoint(p.x() - dy * side, p.y() + dx * side);
                if (reserved.contains(fence) || area.propAt(fence.x(), fence.y()) != null
                        || area.landmarks.containsKey(fence) || world.transitionAt(MAP, fence.x(), fence.y()) != null
                        || NatureSiteGenerator.biome(area.tileAt(fence.x(), fence.y())) == null) continue;
                WorldProp prop = new WorldProp(fence.x(), fence.y(), dx == 0
                        ? "location_farmland_fence" : "location_farmland_fence_side", 42);
                area.addProp(prop);
                if (!NatureSiteGenerator.walkable(world, trail)) area.removeProp(prop);
            }
        }
    }

    private static int roadNeighbors(MapArea area, TilePoint point) {
        int count = 0;
        for (int[] d : DIRS) if (Terrain.connectingRoad(area.tileAt(point.x() + d[0], point.y() + d[1]))) count++;
        return count;
    }
}
