package com.alderfall.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

final class Pathfinder {
    private static final int[][] DIRECTIONS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1},
            {1, 1},
            {-1, 1},
            {1, -1},
            {-1, -1}
    };
    private static final int NORMAL_TILE_COST = 100;
    private static final int ROAD_TILE_COST = 60;
    private static final int DIAGONAL_COST_MULTIPLIER = 141;

    private Pathfinder() {
    }

    static TilePoint nearestTarget(GameState state, int targetX, int targetY) {
        if (playerWalkable(state, targetX, targetY)) {
            return new TilePoint(targetX, targetY);
        }
        TilePoint best = null;
        int bestPlayerDistance = Integer.MAX_VALUE;
        int bestTargetDistance = Integer.MAX_VALUE;
        for (int radius = 1; radius < 10; radius++) {
            for (int y = targetY - radius; y <= targetY + radius; y++) {
                for (int x = targetX - radius; x <= targetX + radius; x++) {
                    int targetDistance = Math.abs(x - targetX) + Math.abs(y - targetY);
                    if (targetDistance != radius || !playerWalkable(state, x, y)) {
                        continue;
                    }
                    int playerDistance = Math.abs(x - state.playerX) + Math.abs(y - state.playerY);
                    if (playerDistance < bestPlayerDistance
                            || (playerDistance == bestPlayerDistance && targetDistance < bestTargetDistance)) {
                        best = new TilePoint(x, y);
                        bestPlayerDistance = playerDistance;
                        bestTargetDistance = targetDistance;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    static List<TilePoint> findPath(GameState state, TilePoint start, TilePoint target) {
        int width = state.world.width(state.currentMapId);
        int height = state.world.height(state.currentMapId);
        if (!inBounds(start.x(), start.y(), width, height) || !inBounds(target.x(), target.y(), width, height)) {
            return List.of();
        }
        int startIndex = index(start.x(), start.y(), width);
        int targetIndex = index(target.x(), target.y(), width);
        if (startIndex == targetIndex) {
            return new ArrayList<>();
        }
        int cells = width * height;
        PropCollision.Anchor[] anchors = new PropCollision.Anchor[cells];
        boolean[] checkedAnchors = new boolean[cells];
        PriorityQueue<PathNode> frontier = new PriorityQueue<>(Comparator.comparingInt(PathNode::priority));
        int[] cameFrom = new int[cells];
        int[] costSoFar = new int[cells];
        Arrays.fill(cameFrom, -1);
        Arrays.fill(costSoFar, Integer.MAX_VALUE);
        frontier.add(new PathNode(startIndex, start.x(), start.y(), 0, heuristic(start.x(), start.y(), target)));
        costSoFar[startIndex] = 0;

        while (!frontier.isEmpty()) {
            PathNode currentNode = frontier.poll();
            int currentIndex = currentNode.index();
            if (currentIndex == targetIndex) {
                break;
            }
            int currentCost = costSoFar[currentIndex];
            if (currentNode.cost() != currentCost) {
                continue;
            }
            for (int[] direction : DIRECTIONS) {
                int nx = currentNode.x() + direction[0];
                int ny = currentNode.y() + direction[1];
                if (!inBounds(nx, ny, width, height) || !canStep(state, currentNode.x(), currentNode.y(), nx, ny,
                        anchors, checkedAnchors, width)) {
                    continue;
                }
                int nextIndex = index(nx, ny, width);
                int newCost = currentCost + movementCost(state, currentNode.x(), currentNode.y(), nx, ny);
                if (newCost < costSoFar[nextIndex]) {
                    costSoFar[nextIndex] = newCost;
                    cameFrom[nextIndex] = currentIndex;
                    frontier.add(new PathNode(nextIndex, nx, ny, newCost, newCost + heuristic(nx, ny, target)));
                }
            }
        }

        if (cameFrom[targetIndex] == -1) {
            return List.of();
        }
        List<TilePoint> path = new ArrayList<>();
        int current = targetIndex;
        while (current != startIndex) {
            path.add(new TilePoint(current % width, current / width));
            current = cameFrom[current];
        }
        Collections.reverse(path);
        return path;
    }

    static boolean walkable(GameState state, int x, int y) {
        return playerWalkable(state, x, y) && state.npcAt(state.currentMapId, x, y) == null;
    }

    static boolean playerWalkable(GameState state, int x, int y) {
        return x >= 0
                && y >= 0
                && x < state.world.width(state.currentMapId)
                && y < state.world.height(state.currentMapId)
                && PropCollision.navigationAnchor(state.world, state.currentMapId, x, y) != null
                && (state.residentsYieldToPlayer(state.currentMapId) || state.npcAt(state.currentMapId, x, y) == null)
                && state.blockingQuestObjectiveAt(state.currentMapId, x, y) == null;
    }

    private static boolean canStep(GameState state, int fromX, int fromY, int toX, int toY,
                                   PropCollision.Anchor[] anchors, boolean[] checked, int width) {
        if ((!state.residentsYieldToPlayer(state.currentMapId) && state.npcAt(state.currentMapId, toX, toY) != null)
                || state.blockingQuestObjectiveAt(state.currentMapId, toX, toY) != null) return false;
        var from = anchor(state, fromX, fromY, anchors, checked, width);
        var to = anchor(state, toX, toY, anchors, checked, width);
        if (from == null || to == null) return false;
        if (fromX != toX && fromY != toY
                && (!playerWalkable(state, toX, fromY) || !playerWalkable(state, fromX, toY))) return false;
        return PropCollision.canTravel(state.world, state.currentMapId, from.x(), from.y(), to.x(), to.y());
    }

    private static PropCollision.Anchor anchor(GameState state, int x, int y,
                                               PropCollision.Anchor[] anchors, boolean[] checked, int width) {
        int i = index(x, y, width);
        if (!checked[i]) {
            anchors[i] = PropCollision.navigationAnchor(state.world, state.currentMapId, x, y);
            checked[i] = true;
        }
        return anchors[i];
    }

    private static int movementCost(GameState state, int fromX, int fromY, int toX, int toY) {
        int cost = Terrain.roadLike(state.world.tileAt(state.currentMapId, toX, toY)) ? ROAD_TILE_COST : NORMAL_TILE_COST;
        cost = (int) Math.round(cost * state.world.waterDepth(state.currentMapId, toX, toY).movementCost);
        if (Math.abs(toX - fromX) == 1 && Math.abs(toY - fromY) == 1) {
            cost = cost * DIAGONAL_COST_MULTIPLIER / 100;
        }
        // Prefer space around people, but never declare a doorway unreachable just because of a resident.
        if (state.npcAt(state.currentMapId, toX, toY) != null) cost += 180;
        return cost;
    }

    private static boolean inBounds(int x, int y, int width, int height) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    private static int index(int x, int y, int width) {
        return y * width + x;
    }

    private static int heuristic(int x, int y, TilePoint target) {
        int dx = Math.abs(x - target.x());
        int dy = Math.abs(y - target.y());
        int diagonal = Math.min(dx, dy);
        int straight = Math.max(dx, dy) - diagonal;
        return (diagonal * DIAGONAL_COST_MULTIPLIER / 100 + straight) * ROAD_TILE_COST;
    }
}
