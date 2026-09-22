package com.alderfall.game.map;

import com.alderfall.game.Terrain;
import com.alderfall.game.TilePoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/** Routes over ground; water crossings are indivisible, straight bank-to-bank edges. */
final class OverworldRoadPlanner {
    private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
    private final char[][] tiles;
    private final int width;
    private final int height;
    private final int maxSpan;

    OverworldRoadPlanner(char[][] tiles, int maxSpan) {
        this.tiles = tiles;
        this.width = tiles[0].length;
        this.height = tiles.length;
        this.maxSpan = maxSpan;
    }

    List<TilePoint> route(TilePoint from, TilePoint to) {
        TilePoint start = nearestGround(from);
        TilePoint goal = nearestGround(to);
        if (start == null || goal == null) {
            return List.of();
        }
        int[] costs = new int[width * height];
        int[] parents = new int[costs.length];
        Arrays.fill(costs, Integer.MAX_VALUE);
        Arrays.fill(parents, -1);
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(Node::estimate)
                .thenComparingLong(Node::deviation).thenComparingInt(Node::index));
        int first = start.y() * width + start.x();
        int last = goal.y() * width + goal.x();
        costs[first] = 0;
        open.add(new Node(first, 0, heuristic(start.x(), start.y(), goal), 0));
        while (!open.isEmpty()) {
            Node node = open.remove();
            if (node.cost != costs[node.index]) {
                continue;
            }
            if (node.index == last) {
                return reconstruct(parents, first, last);
            }
            int x = node.index % width;
            int y = node.index / width;
            for (int[] dir : DIRECTIONS) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (!inside(nx, ny)) {
                    continue;
                }
                int span = 0;
                boolean existing = tiles[ny][nx] == 'B';
                boolean valid = true;
                while (inside(nx, ny) && water(tiles[ny][nx])) {
                    span++;
                    // Never attach to the side of a bridge, cross it, or widen it.
                    if (span > maxSpan || (tiles[ny][nx] == 'B') != existing
                            || bridgeAt(nx + dir[1], ny + dir[0])
                            || bridgeAt(nx - dir[1], ny - dir[0])) {
                        valid = false;
                        break;
                    }
                    nx += dir[0];
                    ny += dir[1];
                }
                if (!valid || !ground(nx, ny)) {
                    continue;
                }
                int stepCost = groundCost(tiles[ny][nx]);
                if (span > 0) {
                    stepCost += existing ? span * 8 : 35 + span * 22;
                }
                int next = ny * width + nx;
                int cost = node.cost + stepCost;
                if (cost >= costs[next]) {
                    continue;
                }
                costs[next] = cost;
                parents[next] = node.index;
                long deviation = Math.abs((long) (nx - start.x()) * (goal.y() - start.y())
                        - (long) (ny - start.y()) * (goal.x() - start.x()));
                open.add(new Node(next, cost, cost + heuristic(nx, ny, goal), deviation));
            }
        }
        return List.of();
    }

    private List<TilePoint> reconstruct(int[] parents, int first, int last) {
        List<TilePoint> reversed = new ArrayList<>();
        int current = last;
        while (current != first) {
            int parent = parents[current];
            int x = current % width;
            int y = current / width;
            int px = parent % width;
            int py = parent / width;
            int dx = Integer.compare(px, x);
            int dy = Integer.compare(py, y);
            while (x != px || y != py) {
                reversed.add(new TilePoint(x, y));
                x += dx;
                y += dy;
            }
            current = parent;
        }
        reversed.add(new TilePoint(first % width, first / width));
        Collections.reverse(reversed);
        return reversed;
    }

    private TilePoint nearestGround(TilePoint point) {
        if (ground(point.x(), point.y())) {
            return point;
        }
        // A waypoint may lie inside a lake. Move the waypoint to a bank, not the bank to it.
        // Existing bridge endpoints prefer one of their own banks.
        if (bridgeAt(point.x(), point.y())) {
            for (int distance = 1; distance <= maxSpan + 1; distance++) {
                for (int[] dir : DIRECTIONS) {
                    int x = point.x() + dir[0] * distance;
                    int y = point.y() + dir[1] * distance;
                    if (ground(x, y) && (distance == 1
                            || bridgeAt(x - dir[0], y - dir[1]))) {
                        return new TilePoint(x, y);
                    }
                }
            }
        }
        for (int radius = 1; radius < width + height; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int dx = radius - Math.abs(dy);
                if (ground(point.x() - dx, point.y() + dy)) {
                    return new TilePoint(point.x() - dx, point.y() + dy);
                }
                if (ground(point.x() + dx, point.y() + dy)) {
                    return new TilePoint(point.x() + dx, point.y() + dy);
                }
            }
        }
        return null;
    }

    private static int groundCost(char tile) {
        if (Terrain.connectingRoad(tile)) {
            return 8;
        }
        return switch (tile) {
            case 'm' -> 55;
            case 'v' -> 18;
            case 'f' -> 12;
            default -> 10;
        };
    }

    private int heuristic(int x, int y, TilePoint goal) {
        return (Math.abs(x - goal.x()) + Math.abs(y - goal.y())) * 8;
    }

    private boolean ground(int x, int y) {
        return inside(x, y) && !water(tiles[y][x])
                && (Terrain.passable(tiles[y][x]) || tiles[y][x] == 'm');
    }

    private boolean bridgeAt(int x, int y) {
        return inside(x, y) && tiles[y][x] == 'B';
    }

    private boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    private static boolean water(char tile) {
        return tile == 'w' || tile == '~' || tile == 'B';
    }

    private record Node(int index, int cost, int estimate, long deviation) { }
}
