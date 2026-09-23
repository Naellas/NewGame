package com.alderfall.game;

import com.alderfall.game.map.InteriorLayout;
import com.alderfall.game.map.MapArea;
import com.alderfall.game.map.WorldMap;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Evaluate the complete composition, including silent placement rejection and navigation repair. */
public final class InteriorLayoutTest {
    public static void main(String[] args) throws Exception {
        WorldMap world = new WorldMap(42);
        Method place = WorldMap.class.getDeclaredMethod("addFurniture", MapArea.class, int.class, int.class, String.class);
        Method repair = WorldMap.class.getDeclaredMethod("ensureInteriorNavigable", MapArea.class);
        place.setAccessible(true); repair.setAccessible(true);
        int checked = 0;
        Set<String> outlines = new HashSet<>();
        for (String theme : InteriorLayout.THEMES) {
            if (args.length > 0 && !theme.equals(args[0])) continue;
            for (InteriorStyle style : InteriorStyle.values()) {
                for (int seed : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, -1, -42, -1024}) {
                    InteriorLayout plan = InteriorLayout.compose(theme, seed, style);
                    String id = world.createEditorMap("editor_composition", theme, "interior",
                            plan.tiles()[0].length, plan.tiles().length);
                    MapArea area = world.area(id);
                    for (int y = 0; y < area.height(); y++) {
                        area.tiles[y] = plan.tiles()[y].clone();
                        for (int x = 0; x < area.width(); x++) {
                            if (area.tiles[y][x] == 'z') area.interiorRugs.add(new TilePoint(x, y));
                        }
                    }
                    String context = theme + "/" + style + "/" + seed;
                    checkEnclosure(plan.tiles(), context);
                    outlines.add(outline(plan.tiles()));
                    for (WorldProp prop : plan.props()) {
                        place.invoke(world, area, prop.x(), prop.y(), prop.asset());
                        require(area.props.contains(prop), "Rejected planned prop " + context + " " + prop);
                        if (!prop.asset().startsWith("interior_wall_")) {
                            int[] size = world.interiorVisualFootprint(prop.asset());
                            for (int y = prop.y(); y < prop.y() + size[1]; y++) {
                                for (int x = prop.x(); x < prop.x() + size[0]; x++) {
                                    require(!plan.circulation().contains(new TilePoint(x, y)), "Prop on main aisle " + context + " " + prop);
                                }
                            }
                            require(plan.zones().stream().anyMatch(z -> z.contains(prop.x(), prop.y())
                                            && z.contains(prop.x() + size[0] - 1, prop.y() + size[1] - 1)),
                                    "Prop has no functional zone " + context + " " + prop);
                        }
                        if (prop.asset().contains("bed")) {
                            require(plan.zones().stream().anyMatch(z -> (z.purpose().contains("room") || z.purpose().contains("bunk") || z.purpose().equals("sleeping"))
                                    && z.contains(prop.x(), prop.y())), "Bed outside private room");
                        }
                    }
                    int before = area.props.size();
                    checkReachability(world, id, context);
                    repair.invoke(world, area);
                    require(area.props.size() == before, "Navigation repair dismantled a furniture group " + context + ": "
                            + plan.props().stream().filter(p -> !area.props.contains(p)).toList());
                    checkReachability(world, id, context);
                    for (TilePoint rug : area.interiorRugs) {
                        require(world.interiorRugAt(id, rug.x(), rug.y()), "Furniture punched a hole in a rug " + context);
                    }
                    for (TilePoint position : plan.residents()) {
                        require(world.isPassable(id, position.x(), position.y()), "Resident standing in furniture " + context + " " + position);
                    }
                    for (TilePoint position : plan.circulation()) {
                        require(world.isPassable(id, position.x(), position.y()), "Blocked reserved route " + context + " " + position);
                    }
                    for (WorldProp prop : area.props) {
                        if (prop.asset().contains("bench_h")) {
                            require(area.props.stream().anyMatch(table -> table.x() == prop.x()
                                    && Math.abs(table.y() - prop.y()) == 1 && table.asset().equals("interior_banquet_table_h"))
                                    || plan.zones().stream().anyMatch(z -> List.of("waiting", "welcome", "public_hearing", "distribution", "first_cup").contains(z.purpose())
                                    && z.contains(prop.x(), prop.y())), "Unpaired dining bench " + context);
                        }
                    }
                    checked++;
                }
            }
        }
        require(outlines.size() >= 3, "Missing footprint variety");
        System.out.println("Composed layout checks passed: " + checked + " plans; enclosed shells, reachable rooms, intact activity groups; "
                + outlines.size() + " outlines.");
    }

    private static String outline(char[][] tiles) {
        StringBuilder result = new StringBuilder();
        for (char[] row : tiles) {
            for (char tile : row) result.append(tile == 'x' ? 'x' : '.');
            result.append('\n');
        }
        return result.toString();
    }

    private static void checkEnclosure(char[][] tiles, String context) {
        int h = tiles.length, w = tiles[0].length;
        int entry = w / 2 - 1;
        require(tiles[h - 2][entry] == 'e' && tiles[h - 2][entry + 1] == 'e', "Missing centered exit " + context);
        boolean[][] seen = new boolean[h][w];
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        queue.add(new TilePoint(0, 0)); seen[0][0] = true;
        while (!queue.isEmpty()) {
            TilePoint p = queue.remove();
            require(tiles[p.y()][p.x()] == 'x', "Unenclosed floor at " + p + " " + context);
            for (int[] direction : DIRECTIONS) {
                int x = p.x() + direction[0], y = p.y() + direction[1];
                if (x < 0 || y < 0 || x >= w || y >= h || seen[y][x] || tiles[y][x] == 'o') continue;
                if (y == h - 2 && (x == entry || x == entry + 1)) continue; // Only the two actual exits seal the shell.
                seen[y][x] = true; queue.add(new TilePoint(x, y));
            }
        }
        for (int y = 1; y < h - 1; y++) for (int x = 1; x < w - 1; x++) {
            if (tiles[y][x] == 'o' || tiles[y][x] == 'x' || y == h - 2 && (x == entry || x == entry + 1)) continue;
            for (int[] d : DIRECTIONS) require(tiles[y + d[1]][x + d[0]] != 'x', "Floor touches exterior void " + context);
        }
    }

    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private static void checkReachability(WorldMap world, String id, String context) {
        MapArea area = world.area(id);
        Set<TilePoint> seen = new HashSet<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        TilePoint entry = new TilePoint(area.width() / 2 - 1, area.height() - 2);
        seen.add(entry); queue.add(entry);
        while (!queue.isEmpty()) {
            TilePoint p = queue.remove();
            for (int[] d : DIRECTIONS) {
                TilePoint next = new TilePoint(p.x() + d[0], p.y() + d[1]);
                if (world.isPassable(id, next.x(), next.y()) && seen.add(next)) queue.add(next);
            }
        }
        for (int y = 0; y < area.height(); y++) for (int x = 0; x < area.width(); x++) {
            if (world.isPassable(id, x, y) && !seen.contains(new TilePoint(x, y))) {
                StringBuilder grid = new StringBuilder();
                for (int row = 0; row < area.height(); row++) {
                    for (int col = 0; col < area.width(); col++) {
                        grid.append(world.isPassable(id, col, row) && !seen.contains(new TilePoint(col, row)) ? '!' : area.tiles[row][col]);
                    }
                    grid.append('\n');
                }
                throw new AssertionError("Unreachable floor " + context + " " + x + "," + y + "\n" + grid);
            }
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
