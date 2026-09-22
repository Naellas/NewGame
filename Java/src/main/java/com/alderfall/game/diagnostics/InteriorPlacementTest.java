package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Generation regression: every walkable tile reachable, every tabletop detail supported. */
public final class InteriorPlacementTest {
    public static void main(String[] args) {
        int checked = 0;
        Set<InteriorStyle> styles = new HashSet<>();
        for (long seed : new long[]{0, 42, 1024}) {
            WorldMap world = new WorldMap(seed);
            for (String source : List.of("town_briarbridge", "village_oakhaven", "village_snowrest",
                    "village_dunewick", "village_mireford", "city_archive")) {
                for (CityBuilding building : world.cityBuildings(source).stream().limit(3).toList()) {
                    String id = world.ensureHouseInterior(source, building.x1(), building.y1(), 13, 8);
                    styles.add(InteriorStyle.forMap(id));
                    check(world, id);
                    checked++;
                }
            }
        }
        require(styles.size() == 6, "Missing regional style");
        System.out.println("Interior placement checks passed: " + checked + " interiors, 3 seeds, 6 styles.");
    }

    private static void check(WorldMap world, String id) {
        TilePoint entry = world.interiorEntryPoint(id);
        require(world.isPassable(id, entry.x(), entry.y()), "Blocked entry " + id);
        Set<TilePoint> seen = new HashSet<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        seen.add(entry); queue.add(entry);
        while (!queue.isEmpty()) {
            TilePoint p = queue.remove();
            for (int[] d : new int[][]{{1,0}, {-1,0}, {0,1}, {0,-1}}) {
                TilePoint q = new TilePoint(p.x() + d[0], p.y() + d[1]);
                if (q.x() >= 0 && q.y() >= 0 && q.x() < world.width(id) && q.y() < world.height(id)
                        && world.isPassable(id, q.x(), q.y()) && seen.add(q)) queue.add(q);
            }
        }
        for (int y = 1; y < world.height(id) - 1; y++) {
            for (int x = 1; x < world.width(id) - 1; x++) {
                require(!world.isPassable(id, x, y) || seen.contains(new TilePoint(x,y)), "Unreachable floor " + id);
            }
        }
        for (WorldProp detail : world.props(id)) {
            if (!detail.asset().startsWith("interior_tabletop_") && !detail.asset().equals("interior_seed_bowl")
                    && !detail.asset().equals("interior_mortar_pestle") && !detail.asset().equals("interior_flower_vase")) continue;
            boolean supported = world.props(id).stream().anyMatch(p -> {
                if (p == detail || p.asset().startsWith("interior_tabletop_")) return false;
                boolean surface = p.asset().contains("table") || p.asset().contains("counter")
                        || p.asset().contains("cupboard")
                        || p.asset().contains("desk") || p.asset().contains("workbench")
                        || p.asset().contains("alchemy") || p.asset().contains("sawhorse");
                int[] size = world.interiorVisualFootprint(p.asset());
                return surface && detail.x() >= p.x() && detail.y() >= p.y()
                        && detail.x() < p.x() + size[0] && detail.y() < p.y() + size[1];
            });
            require(supported, "Unsupported detail " + id + " " + detail);
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
