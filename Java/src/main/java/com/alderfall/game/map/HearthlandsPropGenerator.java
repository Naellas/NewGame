package com.alderfall.game.map;

import com.alderfall.game.HearthlandsFolklore;
import com.alderfall.game.GameData;
import com.alderfall.game.Npc;
import com.alderfall.game.Terrain;
import com.alderfall.game.TilePoint;
import com.alderfall.game.WorldProp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Authored local anchors with deterministic, road-safe placement on generated maps. */
final class HearthlandsPropGenerator {
    private HearthlandsPropGenerator() { }

    static void addSettlement(WorldMap world, MapArea area) {
        switch (area.id) {
            case "village_elderford" -> {
                place(world, area, 19, 8, 6, HearthlandsFolklore.WARD, 42);
            }
            case "town_moonspire", "town_briarbridge" -> {
                // The everyday hearth tradition survives beside the royal institutions.
                place(world, area, 12, 16, 28, HearthlandsFolklore.OVEN, 70);
                place(world, area, 22, 16, 12, HearthlandsFolklore.WARD, 42);
            }
            default -> { }
        }
    }

    static void addOverworld(WorldMap world, MapArea area) {
        // Work outward from the camp, following existing settlement approaches.
        for (TilePoint center : List.of(new TilePoint(138, 165),
                new TilePoint(170, 180), new TilePoint(131, 121))) {
            List<TilePoint> candidates = candidates(area, center.x(), center.y(), 14);
            for (TilePoint point : candidates) {
                int distance = distance(point, center.x(), center.y());
                if (distance < 5 || !nearRoad(area, point)) continue;
                if (clear(world, area, point.x(), point.y(), 0)) {
                    clearGroundCover(area, point, 0);
                    area.addProp(new WorldProp(point.x(), point.y(), HearthlandsFolklore.WARD, 44));
                    break;
                }
            }
        }
    }

    private static void place(WorldMap world, MapArea area, int x, int y, int radius, String asset, int size) {
        for (TilePoint point : candidates(area, x, y, radius)) {
            if (clear(world, area, point.x(), point.y(), size > 48 ? 1 : 0)) {
                clearGroundCover(area, point, size > 48 ? 1 : 0);
                area.addProp(new WorldProp(point.x(), point.y(), asset, size));
                return;
            }
        }
    }

    private static List<TilePoint> candidates(MapArea area, int x, int y, int radius) {
        List<TilePoint> result = new ArrayList<>();
        for (int yy = Math.max(3, y - radius); yy <= Math.min(area.height() - 4, y + radius); yy++) {
            for (int xx = Math.max(3, x - radius); xx <= Math.min(area.width() - 4, x + radius); xx++) {
                if (Math.abs(xx - x) + Math.abs(yy - y) <= radius) result.add(new TilePoint(xx, yy));
            }
        }
        result.sort(Comparator.comparingInt((TilePoint p) -> distance(p, x, y))
                .thenComparingInt(TilePoint::y).thenComparingInt(TilePoint::x));
        return result;
    }

    private static boolean clear(WorldMap world, MapArea area, int x, int y, int margin) {
        // Reserve the visible canopy as well as its anchor; do not cover doors or existing props.
        for (int yy = y - margin; yy <= y + 1; yy++) {
            for (int xx = x - margin; xx <= x + margin; xx++) {
                char tile = area.tileAt(xx, yy);
                if (!Terrain.passable(tile) || Terrain.connectingRoad(tile) || "cudwkx~".indexOf(tile) >= 0
                        || area.propsAt(xx, yy).stream().anyMatch(p -> !groundCover(p))
                        || area.landmarks.containsKey(new TilePoint(xx, yy))
                        || world.cityBuildingAt(area.id, xx, yy) != null
                        || world.cityBuildingAt(area.id, xx, yy - 1) != null
                        || world.transitionAt(area.id, xx, yy) != null) return false;
                for (Npc npc : world.npcs(area.id)) {
                    if (npc.x() == xx && npc.y() == yy) return false;
                }
                for (Npc npc : GameData.NPCS) {
                    if (npc.mapId().equals(area.id) && npc.x() == xx && npc.y() == yy) return false;
                }
            }
        }
        return true;
    }

    private static boolean groundCover(WorldProp prop) {
        String asset = prop.asset();
        return prop.size() <= 48 && asset.startsWith("deco_")
                && (asset.contains("grass") || asset.contains("flower") || asset.contains("clover")
                || asset.contains("pebble") || asset.contains("leaves") || asset.contains("leaf"));
    }

    private static void clearGroundCover(MapArea area, TilePoint point, int margin) {
        area.props.removeIf(p -> groundCover(p) && Math.abs(p.x() - point.x()) <= margin
                && p.y() >= point.y() - margin && p.y() <= point.y() + 1);
    }

    private static boolean nearRoad(MapArea area, TilePoint point) {
        for (int y = point.y() - 3; y <= point.y() + 3; y++) {
            for (int x = point.x() - 3; x <= point.x() + 3; x++) {
                if (Terrain.connectingRoad(area.tileAt(x, y))) return true;
            }
        }
        return false;
    }

    private static int distance(TilePoint point, int x, int y) {
        return Math.abs(point.x() - x) + Math.abs(point.y() - y);
    }
}
