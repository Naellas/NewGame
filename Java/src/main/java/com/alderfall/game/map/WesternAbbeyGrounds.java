package com.alderfall.game.map;

import com.alderfall.game.*;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Public orchard and maintained sealed threshold from the Open Bough premise. */
final class WesternAbbeyGrounds {
    static final TilePoint GARDEN_EXIT = new TilePoint(13, 20);
    static final TilePoint GARDEN_ARRIVAL = new TilePoint(13, 19);
    static final TilePoint GARDEN_STAIRS = new TilePoint(13, 4);
    static final TilePoint CELLAR_EXIT = new TilePoint(9, 12);
    static final TilePoint CELLAR_ARRIVAL = new TilePoint(9, 11);

    private WesternAbbeyGrounds() { }

    static MapArea garden() {
        char[][] tiles = filled(27, 23, 'g');
        for (int y = 0; y < tiles.length; y++) {
            for (int x = 0; x < tiles[0].length; x++) {
                if (x < 2 || y < 2 || x > 24 || y > 20) tiles[y][x] = 'f';
                if (x == 13 && y >= 4 && y <= 20) tiles[y][x] = Terrain.DIRT_ROAD;
                if ((y == 9 || y == 16) && x >= 5 && x <= 21) tiles[y][x] = Terrain.DIRT_ROAD;
            }
        }
        MapArea area = new MapArea(WesternReachFolklore.GARDEN_ID, "Briarbridge Guest Orchard", "village", tiles);
        for (int x = 2; x <= 24; x++) {
            prop(area, x, 2, "village_fence_auto", 40);
            if (Math.abs(x - 13) > 1) prop(area, x, 20, "village_fence_auto", 40);
        }
        for (int y = 3; y < 20; y++) {
            prop(area, 2, y, "village_fence_auto", 40);
            prop(area, 24, y, "village_fence_auto", 40);
        }
        for (int y : new int[]{7, 12, 15})
            for (int x : new int[]{5, 9, 18, 22}) prop(area, x, y, "deco_tree_round", 82);
        prop(area, 6, 18, "deco_tree_young", 54);
        prop(area, 20, 18, "deco_tree_young", 54);
        prop(area, 5, 17, "village_prop_seedling_tray", 36);
        prop(area, 7, 17, "village_prop_farm_tools", 38);
        prop(area, 9, 17, "village_prop_bench", 44);
        prop(area, 17, 17, "village_prop_bench", 44);
        prop(area, 19, 17, "village_prop_produce_basket", 38);
        prop(area, 10, 5, "village_prop_woodpile", 38);
        prop(area, 16, 5, "city_prop_vine_lantern_post", 44);
        prop(area, 13, 4, "location_dungeon_stair_entrance", 48);
        prop(area, 13, 20, WesternReachFolklore.GARDEN_GATE, 86);
        area.landmarks.put(GARDEN_EXIT, "Return to Briarbridge");
        area.landmarks.put(GARDEN_STAIRS, "Steps to the Mirror Undercroft");
        area.landmarks.put(new TilePoint(13, 16), "The Petition Walk");
        return area;
    }

    static MapArea undercroft() {
        char[][] tiles = filled(19, 15, 'H');
        for (int y = 0; y < tiles.length; y++)
            for (int x = 0; x < tiles[0].length; x++) {
                if (x < 2 || y < 3 || x > 16 || y > 13) tiles[y][x] = 'X';
            }
        // A physical wall keeps the sealed door closed; there is no hidden portal.
        for (int x = 8; x <= 10; x++) {
            tiles[3][x] = 'X';
            tiles[4][x] = 'X';
        }
        MapArea area = new MapArea(WesternReachFolklore.UNDERCROFT_ID, "Briarbridge Mirror Undercroft", "interior", tiles);
        prop(area, 9, 4, WesternReachFolklore.SEALED_MIRROR, 96);
        prop(area, 5, 6, "dungeon_prop_lantern_stand", 44);
        prop(area, 13, 6, "dungeon_prop_lantern_stand", 44);
        prop(area, 5, 9, "location_camp_crates", 42);
        prop(area, 14, 9, "village_prop_woodpile", 38);
        prop(area, 9, 12, "location_dungeon_stair_entrance", 48);
        area.landmarks.put(CELLAR_EXIT, "Return to the Guest Orchard");
        area.landmarks.put(new TilePoint(9, 5), "The Sealed Guest Passage");
        return area;
    }

    static List<Npc> gardeners() {
        return List.of(
                new Npc(WesternReachFolklore.GARDEN_ID, "Orchard Tender", "npc_citizen_woman", 8, 17,
                        WesternReachFolklore.gardenDialogue(0), null, null),
                new Npc(WesternReachFolklore.GARDEN_ID, "Petition Steward", "npc_citizen_man", 16, 17,
                        WesternReachFolklore.gardenDialogue(1), null, null));
    }

    static List<Npc> keepers() {
        return List.of(new Npc(WesternReachFolklore.UNDERCROFT_ID, "Threshold Keeper", "npc_citizen_man", 12, 7,
                WesternReachFolklore.gardenDialogue(2), null, null));
    }

    static TilePoint townEntrance(WorldMap world) {
        String id = "town_briarbridge";
        CityBuilding abbey = world.cityBuildings(id).stream()
                .filter(b -> WesternReachFolklore.ABBEY.equals(WesternReachFolklore.buildingAsset(id, b)))
                .findFirst().orElse(null);
        if (abbey == null) return null;
        TilePoint door = world.cityBuildingDoorTiles(abbey).get(0);
        TilePoint start = new TilePoint(door.x(), door.y() + 1);
        Set<TilePoint> seen = new HashSet<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        if (!world.isPassable(id, start.x(), start.y())) return null;
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            TilePoint p = queue.removeFirst();
            for (TilePoint next : List.of(new TilePoint(p.x() - 1, p.y()), new TilePoint(p.x() + 1, p.y()),
                    new TilePoint(p.x(), p.y() - 1), new TilePoint(p.x(), p.y() + 1)))
                if (world.isPassable(id, next.x(), next.y()) && seen.add(next)) queue.add(next);
        }
        return seen.stream().filter(p -> clearGate(world, p))
                .sorted(Comparator.comparingInt((TilePoint p) -> Math.abs(p.x() - door.x()) + Math.abs(p.y() - door.y()))
                        .thenComparingInt(TilePoint::y).thenComparingInt(TilePoint::x))
                .findFirst().orElse(null);
    }

    private static boolean clearGate(WorldMap world, TilePoint p) {
        String id = "town_briarbridge";
        if (p.x() < 3 || p.y() < 3 || p.x() >= world.width(id) - 3 || p.y() >= world.height(id) - 3
                || Terrain.connectingRoad(world.tileAt(id, p.x(), p.y()))) return false;
        for (int y = p.y() - 1; y <= p.y() + 1; y++) {
            int x = p.x();
            if (!world.isPassable(id, x, y) || world.cityBuildingAt(id, x, y) != null
                    || world.cityBuildingAt(id, x, y - 1) != null || world.transitionAt(id, x, y) != null
                    || !world.area(id).propsAt(x, y).isEmpty()) return false;
            for (Npc npc : world.npcs(id)) if (npc.x() == x && npc.y() == y) return false;
            for (Npc npc : GameData.NPCS) if (id.equals(npc.mapId()) && npc.x() == x && npc.y() == y) return false;
        }
        return true;
    }

    private static char[][] filled(int width, int height, char tile) {
        char[][] result = new char[height][width];
        for (char[] row : result) Arrays.fill(row, tile);
        return result;
    }

    private static void prop(MapArea area, int x, int y, String asset, int size) {
        area.addProp(new WorldProp(x, y, asset, size));
    }
}
