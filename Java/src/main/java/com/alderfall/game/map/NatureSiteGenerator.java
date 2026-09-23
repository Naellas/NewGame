package com.alderfall.game.map;

import com.alderfall.game.*;
import java.util.*;

/** Seeded, sparse environmental vignettes. Trails are visual hints, never new road tiles. */
public final class NatureSiteGenerator {
    public record Site(int x, int y, char terrain, String focal, List<TilePoint> trail) {
        public Site { trail = List.copyOf(trail); }
    }
    private static final String MAP = WorldMap.OVERWORLD_ID;
    private NatureSiteGenerator() { }

    public static String biome(char tile) {
        return switch (tile) {
            case 'g' -> "meadow"; case 'f' -> "forest"; case 's' -> "desert";
            case 'b' -> "badlands"; case 'n' -> "tundra"; case 'q' -> "highlands";
            case 'v' -> "marsh"; case 'P' -> "coast"; default -> null;
        };
    }

    public static void populate(WorldMap world, int seed) {
        MapArea area = world.area(MAP);
        Random random = new Random(seed ^ 0x714ea91L);
        List<Site> sites = new ArrayList<>();
        for (int gy = 12; gy < area.height() - 12; gy += 30) {
            for (int gx = 12; gx < area.width() - 12; gx += 30) {
                if (random.nextInt(100) < 30) continue;
                for (int attempt = 0; attempt < 6; attempt++) {
                    int x = gx + random.nextInt(17) - 8, y = gy + random.nextInt(17) - 8;
                    char tile = area.tileAt(x, y);
                    if (biome(tile) == null || tile == 'q' || !candidate(world, area, x, y, tile)) continue;
                    if (sites.stream().anyMatch(s -> Math.hypot(s.x - x, s.y - y) < 23)) continue;
                    List<TilePoint> trail = trail(world, area, new TilePoint(x, y + 1));
                    if (trail.size() < 7) continue;
                    String focal = switch (tile) {
                        case 'f' -> random.nextBoolean() ? "deco_forest_log" : "deco_forest_blue_mushroom_ring";
                        case 's' -> "deco_desert_blooming_cactus";
                        case 'v' -> "deco_marsh_twisted_roots";
                        case 'g' -> "deco_grass_stone_stack";
                        default -> "deco_mountain_cairn";
                    };
                    WorldProp prop = new WorldProp(x, y, focal, 46);
                    area.addProp(prop);
                    if (!walkable(world, trail)) { area.removeProp(prop); continue; }
                    // Clear only generated ground detail. Existing resources and authored props remain intact.
                    Set<TilePoint> cleared = new HashSet<>(trail);
                    for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++)
                        cleared.add(new TilePoint(x + dx, y + dy));
                    for (TilePoint point : cleared) {
                        for (WorldProp detail : List.copyOf(area.propsAt(point.x(), point.y())))
                            if (detail.visualSlot() >= 0) area.removeProp(detail);
                    }
                    // Three low companion patches form a loose crescent around an open southern approach.
                    int[][] offsets = {{-1, 0}, {1, -1}, {-1, -1}};
                    for (int i = 0; i < offsets.length; i++) {
                        int px = x + offsets[i][0], py = y + offsets[i][1];
                        if (area.propAt(px, py) == null && !trail.contains(new TilePoint(px, py))) {
                            for (int slot = 0; slot < 3; slot++) area.addProp(new WorldProp(px, py,
                                    "deco_ground_" + biome(tile) + "_" + (1 + (i * 3 + slot) % 8), 21, slot));
                        }
                    }
                    String accent = switch (tile) {
                        case 'f' -> "deco_forest_mushrooms"; case 'g' -> "deco_grass_herb_patch";
                        case 's', 'b' -> "deco_dry_grass"; case 'v' -> "deco_reeds";
                        default -> "deco_soft_frost_grass";
                    };
                    for (int side : new int[]{-2, 2}) {
                        if (area.propAt(x + side, y - 1) == null && !trail.contains(new TilePoint(x + side, y - 1)))
                            area.addProp(new WorldProp(x + side, y - 1, accent, 30, side < 0 ? 1 : 0));
                    }
                    sites.add(new Site(x, y, tile, focal, trail));
                    break;
                }
            }
        }
        area.setNatureSites(sites);
    }

    private static boolean protectedAt(WorldMap world, MapArea area, int x, int y) {
        if (world.locationKindAt(MAP, x, y) != null) return true;
        for (TilePoint point : area.landmarks.keySet())
            if (Math.abs(point.x() - x) <= 6 && Math.abs(point.y() - y) <= 6) return true;
        return false;
    }

    private static boolean candidate(WorldMap world, MapArea area, int x, int y, char tile) {
        if (area.propAt(x, y) != null) return false;
        for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) {
            if (area.tileAt(x + dx, y + dy) != tile || protectedAt(world, area, x + dx, y + dy)) return false;
        }
        return PropCollision.clear(world, MAP, x + .5, y + 1.5);
    }

    /** Bounded breadth-first search prefers a road; otherwise ends at a nearby open edge of the clearing. */
    private static List<TilePoint> trail(WorldMap world, MapArea area, TilePoint start) {
        Map<TilePoint, TilePoint> parent = new HashMap<>();
        Map<TilePoint, Integer> depth = new HashMap<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        queue.add(start); parent.put(start, start); depth.put(start, 0);
        TilePoint target = null, fallback = null;
        int[][] directions = {{0, 1}, {1, 0}, {-1, 0}, {0, -1}};
        int rotation = Math.floorMod(start.x() * 31 + start.y() * 17, 4);
        while (!queue.isEmpty()) {
            TilePoint point = queue.remove();
            int distance = depth.get(point);
            char tile = area.tileAt(point.x(), point.y());
            if (distance >= 6 && tile != 'q' && Terrain.CONNECTING_ROAD.contains(tile)) { target = point; break; }
            if (distance == 9 && fallback == null) fallback = point;
            if (distance >= 16) continue;
            for (int directionIndex = 0; directionIndex < 4; directionIndex++) {
                int[] direction = directions[(directionIndex + rotation) % 4];
                TilePoint next = new TilePoint(point.x() + direction[0], point.y() + direction[1]);
                char ground = area.tileAt(next.x(), next.y());
                if (parent.containsKey(next) || (biome(ground) == null && ground != 'r' && ground != 'T')
                        || protectedAt(world, area, next.x(), next.y())
                        || !PropCollision.canTravel(world, MAP, point.x() + .5, point.y() + .5, next.x() + .5, next.y() + .5)) continue;
                parent.put(next, point); depth.put(next, distance + 1); queue.add(next);
            }
        }
        if (target == null) target = fallback;
        if (target == null) return List.of();
        List<TilePoint> result = new ArrayList<>();
        for (TilePoint point = target; ; point = parent.get(point)) {
            result.add(point); if (point.equals(start)) break;
        }
        return result;
    }

    public static boolean walkable(WorldMap world, List<TilePoint> trail) {
        for (int i = 0; i < trail.size(); i++) {
            TilePoint a = trail.get(Math.max(0, i - 1)), b = trail.get(i);
            if (!PropCollision.canTravel(world, MAP, a.x() + .5, a.y() + .5, b.x() + .5, b.y() + .5)) return false;
        }
        return true;
    }
}
