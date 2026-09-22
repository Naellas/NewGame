package com.alderfall.game.map;

import com.alderfall.game.FolkloreContent;
import com.alderfall.game.GameData;
import com.alderfall.game.Npc;
import com.alderfall.game.Terrain;
import com.alderfall.game.TilePoint;
import com.alderfall.game.WorldProp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A deliberately bounded first region, with deterministic, route-safe placement. */
final class FolklorePropGenerator {
    private FolklorePropGenerator() { }

    static void populate(WorldMap world) {
        place(world, "village_oakhaven", 7, 17, FolkloreContent.BOUNDARY, 56, 9);
        place(world, "village_elderford", 24, 17, FolkloreContent.BOUNDARY, 56, 10);
        place(world, "village_elderford", 7, 18, "village_prop_woodpile", 40, 8);
        place(world, "city_archive", 17, 15, "village_prop_clay_oven", 54, 14);
        place(world, "city_archive", 19, 17, "village_prop_seedling_tray", 36, 14);
        place(world, "dungeon_redcap_camp_1", 9, 7, "location_camp_fire", 42, 12);
        place(world, "dungeon_redcap_camp_1", 11, 7, "location_camp_crates", 40, 12);
        place(world, "dungeon_redcap_camp_1", 12, 10, "village_prop_woodpile", 40, 12);

        // Follow the existing route and real settlement positions; no new quest markers.
        place(world, WorldMap.OVERWORLD_ID, 117, 155, FolkloreContent.BOUNDARY, 50, 7);
        place(world, WorldMap.OVERWORLD_ID, 130, 155, FolkloreContent.BOUNDARY, 50, 7);
        place(world, WorldMap.OVERWORLD_ID, 141, 158, FolkloreContent.BOUNDARY, 50, 7);
    }

    private static void place(WorldMap world, String mapId, int cx, int cy, String asset, int size, int radius) {
        if (!world.hasMap(mapId)) {
            return;
        }
        MapArea area = world.area(mapId);
        List<TilePoint> candidates = new ArrayList<>();
        for (int y = Math.max(2, cy - radius); y < Math.min(area.height() - 2, cy + radius + 1); y++) {
            for (int x = Math.max(2, cx - radius); x < Math.min(area.width() - 2, cx + radius + 1); x++) {
                if (Math.abs(x - cx) + Math.abs(y - cy) <= radius && clear(world, area, x, y, size)) {
                    candidates.add(new TilePoint(x, y));
                }
            }
        }
        candidates.sort(Comparator.comparingInt((TilePoint p) -> Math.abs(p.x() - cx) + Math.abs(p.y() - cy))
                .thenComparingInt(TilePoint::y).thenComparingInt(TilePoint::x));
        if (!candidates.isEmpty()) {
            TilePoint p = candidates.get(0);
            int padding = size > 80 ? 2 : 1;
            area.props.removeIf(prop -> Math.abs(prop.x() - p.x()) <= padding
                    && prop.y() >= p.y() - padding && prop.y() <= p.y() + 1 && replaceableGroundCover(prop));
            area.addProp(new WorldProp(p.x(), p.y(), asset, size));
        }
    }

    private static boolean clear(WorldMap world, MapArea area, int x, int y, int size) {
        char tile = area.tileAt(x, y);
        if (Terrain.ROAD_LIKE.contains(tile) || Terrain.connectingRoad(tile) || tile == '5' || tile == '6') {
            return false;
        }
        // Large silhouettes need a clear apron, including the tile south of their base.
        int padding = size > 80 ? 2 : 1;
        for (int yy = y - padding; yy <= y + 1; yy++) {
            for (int xx = x - padding; xx <= x + padding; xx++) {
                if (!world.isPassable(area.id, xx, yy) || world.cityBuildingAt(area.id, xx, yy) != null
                        || world.transitionAt(area.id, xx, yy) != null || area.landmarks.containsKey(new TilePoint(xx, yy))
                        || area.propsAt(xx, yy).stream().anyMatch(prop -> !replaceableGroundCover(prop))) {
                    return false;
                }
                if (size > 80 && Terrain.ROAD_LIKE.contains(area.tileAt(xx, yy))) {
                    return false;
                }
                for (Npc npc : world.npcs(area.id)) {
                    if (npc.x() == xx && npc.y() == yy) {
                        return false;
                    }
                }
                for (Npc npc : GameData.NPCS) {
                    if (npc.mapId().equals(area.id) && npc.x() == xx && npc.y() == yy) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean replaceableGroundCover(WorldProp prop) {
        String asset = prop.asset();
        return asset.startsWith("deco_") && prop.size() <= 48
                && (asset.contains("grass") || asset.contains("flower") || asset.contains("pebble")
                || asset.contains("leaf") || asset.contains("leaves") || asset.contains("clover"));
    }
}
