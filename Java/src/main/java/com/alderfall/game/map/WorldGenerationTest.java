package com.alderfall.game.map;

import com.alderfall.game.Terrain;
import com.alderfall.game.TilePoint;
import com.alderfall.game.WorldProp;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

/** Focused regression checks for road crossings, habitat palettes, and destination access. */
public final class WorldGenerationTest {
    public static void main(String[] args) throws Exception {
        checkSyntheticRoutes();
        for (long seed : new long[]{0, 1, 42, 2026, -17}) {
            WorldMap world = new WorldMap(seed);
            checkWorld(world);
            checkGroves(world);
            if (seed == 0) {
                WorldMap repeat = new WorldMap(seed);
                require(Arrays.deepEquals(world.area(WorldMap.OVERWORLD_ID).tiles,
                        repeat.area(WorldMap.OVERWORLD_ID).tiles), "Terrain must repeat for a seed");
                require(world.props(WorldMap.OVERWORLD_ID).equals(repeat.props(WorldMap.OVERWORLD_ID)),
                        "Props must repeat for a seed");
            }
            System.out.println("World generation passed for seed " + seed);
        }
    }

    private static void checkSyntheticRoutes() {
        char[][] grid = filled(30, 24, 'g');
        for (int y = 0; y < grid.length; y++) {
            for (int x = 12; x <= 15; x++) grid[y][x] = 'w';
        }
        List<TilePoint> route = new OverworldRoadPlanner(grid, 8)
                .route(new TilePoint(3, 3), new TilePoint(25, 19));
        require(!route.isEmpty(), "River crossing route missing");
        int waterY = -1;
        int waterCount = 0;
        for (int i = 0; i < route.size(); i++) {
            TilePoint p = route.get(i);
            if (i > 0) require(distance(p, route.get(i - 1)) == 1, "Road has a gap");
            if (grid[p.y()][p.x()] == 'w') {
                if (waterY < 0) waterY = p.y();
                require(p.y() == waterY, "Bridge turns in water");
                waterCount++;
                grid[p.y()][p.x()] = 'B';
            }
        }
        require(waterCount == 4, "Crossing must take the short width of the river");
        List<TilePoint> reuse = new OverworldRoadPlanner(grid, 8)
                .route(new TilePoint(5, waterY + 1), new TilePoint(23, waterY + 1));
        require(reuse.stream().filter(p -> grid[p.y()][p.x()] == 'B').count() == 4,
                "Nearby roads should reuse the existing crossing");
        char[][] lake = filled(30, 24, 'g');
        for (int y = 7; y <= 16; y++) Arrays.fill(lake[y], 9, 21, 'w');
        List<TilePoint> detour = new OverworldRoadPlanner(lake, 8)
                .route(new TilePoint(3, 12), new TilePoint(26, 12));
        require(!detour.isEmpty() && detour.stream().noneMatch(p -> lake[p.y()][p.x()] == 'w'),
                "A broad lake should get a land detour, not artificial islands");
        char[][] ocean = filled(24, 12, 'w');
        for (char[] row : ocean) { row[0] = 'g'; row[23] = 'g'; }
        require(new OverworldRoadPlanner(ocean, 8).route(new TilePoint(0, 5), new TilePoint(23, 5)).isEmpty(),
                "Unbridgeable water must not be silently filled");
    }

    private static void checkWorld(WorldMap world) {
        MapArea area = world.area(WorldMap.OVERWORLD_ID);
        boolean[][] reached = new boolean[area.height()][area.width()];
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        queue.add(WorldMap.START_POSITION);
        reached[WorldMap.START_POSITION.y()][WorldMap.START_POSITION.x()] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            TilePoint p = queue.remove();
            for (int[] dir : dirs) {
                int x = p.x() + dir[0], y = p.y() + dir[1];
                if (x < 0 || y < 0 || x >= area.width() || y >= area.height()
                        || reached[y][x] || !world.isPassable(area.id, x, y)) continue;
                reached[y][x] = true;
                queue.add(new TilePoint(x, y));
            }
        }
        for (WorldMap.SettlementSite site : world.settlementSites()) {
            require(reached[site.y()][site.x()], "Unreachable settlement: " + site.label());
        }
        for (WorldMap.AdventureMarker site : world.adventureMarkers()) {
            require(reached[site.y()][site.x()], "Unreachable adventure: " + site.label());
        }
        int bridges = 0;
        for (int y = 0; y < area.height(); y++) {
            for (int x = 0; x < area.width(); x++) {
                if (area.tileAt(x, y) != 'B') continue;
                boolean horizontal = area.tileAt(x - 1, y) == 'B' || area.tileAt(x + 1, y) == 'B';
                boolean vertical = area.tileAt(x, y - 1) == 'B' || area.tileAt(x, y + 1) == 'B';
                require(!(horizontal && vertical), "Bent or branched bridge at " + x + "," + y);
                int dx = horizontal ? 1 : 0, dy = horizontal ? 0 : 1;
                if (!horizontal && !vertical) {
                    horizontal = Terrain.connectingRoad(area.tileAt(x - 1, y))
                            && Terrain.connectingRoad(area.tileAt(x + 1, y));
                    dx = horizontal ? 1 : 0;
                    dy = horizontal ? 0 : 1;
                }
                if (area.tileAt(x - dx, y - dy) == 'B') continue;
                int length = 0;
                while (area.tileAt(x + dx * length, y + dy * length) == 'B') length++;
                require(length <= 8, "Overlong bridge at " + x + "," + y);
                require(Terrain.connectingRoad(area.tileAt(x - dx, y - dy))
                                && Terrain.connectingRoad(area.tileAt(x + dx * length, y + dy * length)),
                        "Bridge missing a road landing at " + x + "," + y);
                bridges++;
            }
        }
        require(bridges > 0, "No crossings generated");
        for (WorldProp prop : area.props) {
            if (!prop.asset().startsWith("deco_crossing_")) continue;
            require(!Terrain.connectingRoad(area.tileAt(prop.x(), prop.y())), "Crossing prop occupies road");
            require(Terrain.passable(area.tileAt(prop.x(), prop.y())), "Crossing prop is in water");
        }
        System.out.println("  " + bridges + " straight crossings; all destinations reachable");
    }

    private static void checkGroves(WorldMap world) throws Exception {
        Method decoration = WorldMap.class.getDeclaredMethod("forestDecorationFor", MapArea.class,
                int.class, int.class, int.class, int.class);
        Method resource = WorldMap.class.getDeclaredMethod("forestResourceNodeFor", MapArea.class,
                int.class, int.class, int.class, int.class);
        Method family = WorldMap.class.getDeclaredMethod("treeFamily", String.class);
        Method grove = WorldMap.class.getDeclaredMethod("groveFamily", MapArea.class, int.class, int.class, int.class);
        for (Method method : List.of(decoration, resource, family, grove)) method.setAccessible(true);
        MapArea dry = new MapArea("grove_test", "Grove", "overworld", filled(64, 64, 'f'));
        MapArea wet = new MapArea("grove_test", "Grove", "overworld", filled(64, 64, 'f'));
        wet.tiles[32][31] = 'w';
        for (MapArea area : List.of(dry, wet)) {
            String dominant = (String) grove.invoke(world, area, 32, 32, area == wet ? 1 : 6);
            for (Method method : List.of(decoration, resource)) {
                int trees = 0, matches = 0;
                for (int roll = 0; roll < 1000; roll++) {
                    String asset = (String) method.invoke(world, area, 32, 32, roll, 1234567 + roll * 997);
                    String species = (String) family.invoke(world, asset);
                    if (species.isEmpty()) continue;
                    trees++;
                    if (species.equals(dominant)) matches++;
                    require(area == wet || !species.equals("willow"), "Willow selected far from water");
                }
                require(trees > 100 && matches > trees * 0.60,
                        "Grove dominance lost in " + method.getName() + ": " + dominant + " " + matches + "/" + trees);
            }
        }
    }

    private static char[][] filled(int width, int height, char tile) {
        char[][] grid = new char[height][width];
        for (char[] row : grid) Arrays.fill(row, tile);
        return grid;
    }

    private static int distance(TilePoint a, TilePoint b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
