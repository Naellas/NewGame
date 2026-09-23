package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.BiPredicate;

/** Cached by each resident; routes use streets and never cut building corners. */
final class TownNpcNavigation {
    private static final int[][] DIRECTIONS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

    private TownNpcNavigation() { }

    static List<TilePoint> checkpoints(Npc npc, WorldMap world, AmbientNpcAi.Routine routine) {
        List<TilePoint> stops = new ArrayList<>();
        TilePoint anchor = routine.target();
        stops.add(anchor);
        if (routine.activity() != AmbientNpcAi.Activity.PATROLLING) {
            // Small, purposeful movements at a work/social stop; sleeping residents stay put.
            if (routine.roamRadius() > 0 && routine.activity() != AmbientNpcAi.Activity.SHELTERING
                    && routine.activity() != AmbientNpcAi.Activity.RETURNING_HOME) {
                int offset = Math.floorMod(npc.name().hashCode(), DIRECTIONS.length);
                for (int i = 0; i < DIRECTIONS.length && stops.size() < 3; i++) {
                    int[] d = DIRECTIONS[(i + offset) % DIRECTIONS.length];
                    TilePoint p = new TilePoint(anchor.x() + d[0] * routine.roamRadius(),
                            anchor.y() + d[1] * routine.roamRadius());
                    if (world.isPassable(npc.mapId(), p.x(), p.y())
                            && world.propAt(npc.mapId(), p.x(), p.y()) == null
                            && world.transitionAt(npc.mapId(), p.x(), p.y()) == null) stops.add(p);
                }
            }
            return List.copyOf(stops);
        }

        // A stable circuit through nearby street furniture, rather than a single patrol tile.
        List<TilePoint> candidates = new ArrayList<>();
        for (WorldProp prop : world.props(npc.mapId())) {
            String asset = prop.asset();
            if (asset == null || !(asset.contains("gate") || asset.contains("lamp")
                    || asset.contains("banner") || asset.contains("notice") || asset.contains("well"))) continue;
            for (int[] d : DIRECTIONS) {
                TilePoint p = new TilePoint(prop.x() + d[0], prop.y() + d[1]);
                if (distance(anchor, p) <= 18 && world.isPassable(npc.mapId(), p.x(), p.y())
                        && world.transitionAt(npc.mapId(), p.x(), p.y()) == null
                        && Terrain.roadLike(world.tileAt(npc.mapId(), p.x(), p.y()))) {
                    candidates.add(p);
                    break;
                }
            }
        }
        // Sparse villages still get a circuit, using actual walkable street checkpoints.
        for (int[] d : DIRECTIONS) {
            for (int r = 7; r >= 3; r--) {
                TilePoint p = new TilePoint(anchor.x() + d[0] * r, anchor.y() + d[1] * r);
                if (world.isPassable(npc.mapId(), p.x(), p.y())
                        && world.transitionAt(npc.mapId(), p.x(), p.y()) == null
                        && Terrain.roadLike(world.tileAt(npc.mapId(), p.x(), p.y()))) {
                    candidates.add(p);
                    break;
                }
            }
        }
        while (stops.size() < 4) {
            TilePoint previous = stops.get(stops.size() - 1);
            TilePoint next = candidates.stream()
                    .filter(p -> stops.stream().allMatch(s -> distance(s, p) >= 3))
                    .min(Comparator.comparingInt(p -> distance(previous, p))).orElse(null);
            if (next == null) break;
            stops.add(next);
        }
        if (Math.floorMod(npc.name().hashCode(), 2) == 0 && stops.size() > 2) {
            Collections.reverse(stops.subList(1, stops.size()));
        }
        return List.copyOf(stops);
    }

    static int distance(TilePoint a, TilePoint b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }

    /** A* with bounded work, static terrain and a snapshot of occupied tiles. */
    static List<TilePoint> path(int width, int height, TilePoint start, TilePoint target,
                               BiPredicate<Integer, Integer> walkable,
                               BiPredicate<Integer, Integer> road) {
        if (!inside(start, width, height) || !inside(target, width, height) || start.equals(target)
                || !walkable.test(target.x(), target.y())) return List.of();
        int[] costs = new int[width * height];
        int[] parents = new int[costs.length];
        Arrays.fill(costs, Integer.MAX_VALUE);
        Arrays.fill(parents, -1);
        PriorityQueue<PathNode> open = new PriorityQueue<>(Comparator.comparingInt(PathNode::priority)
                .thenComparingInt(PathNode::index));
        int origin = start.y() * width + start.x();
        int goal = target.y() * width + target.x();
        costs[origin] = 0;
        open.add(new PathNode(origin, start.x(), start.y(), 0, distance(start, target) * 10));
        int visited = 0;
        while (!open.isEmpty() && visited < 4096) {
            PathNode node = open.remove();
            if (node.cost() != costs[node.index()]) continue;
            if (node.index() == goal) {
                List<TilePoint> result = new ArrayList<>();
                for (int i = goal; i != origin; i = parents[i]) result.add(new TilePoint(i % width, i / width));
                Collections.reverse(result);
                return result;
            }
            visited++;
            for (int[] d : DIRECTIONS) {
                int x = node.x() + d[0], y = node.y() + d[1];
                if (x < 0 || y < 0 || x >= width || y >= height || !walkable.test(x, y)) continue;
                int index = y * width + x;
                int cost = node.cost() + (road.test(x, y) ? 10 : 17);
                if (cost >= costs[index]) continue;
                costs[index] = cost;
                parents[index] = node.index();
                int heuristic = (Math.abs(x - target.x()) + Math.abs(y - target.y())) * 10;
                open.add(new PathNode(index, x, y, cost, cost + heuristic));
            }
        }
        return List.of();
    }

    private static boolean inside(TilePoint p, int width, int height) {
        return p.x() >= 0 && p.y() >= 0 && p.x() < width && p.y() < height;
    }
}
