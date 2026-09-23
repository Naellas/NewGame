package com.alderfall.game.map;

import com.alderfall.game.*;
import java.util.*;

/** Final composition pass for generated settlements, after authored districts and portals exist. */
public final class SettlementDressing {
    private SettlementDressing() { }
    private static final Set<String> LAMPS = Set.of("city_prop_street_lamp", "city_prop_vine_lantern_post", "city_lantern");
    private static final String[] MARKET = {"city_prop_refresh_stall_blue", "city_prop_refresh_crate_green",
            "city_prop_refresh_barrel", "city_prop_refresh_stall_canvas", "city_prop_refresh_basket_fruit",
            "city_prop_refresh_cart", "village_prop_grain_sacks", "city_prop_news_kiosk",
            "city_prop_refresh_stall_red", "city_prop_refresh_pottery", "city_prop_refresh_crate_closed",
            "city_prop_refresh_sacks", "city_prop_refresh_stall_green", "city_prop_refresh_crate_red"};
    private static final String[] PLANTS = {"city_prop_refresh_planter_herbs", "city_prop_refresh_planter_pink",
            "city_prop_refresh_planter_white", "city_prop_refresh_planter_blue", "city_prop_refresh_planter_cypress",
            "city_prop_refresh_bench", "city_prop_flower_baskets", "city_prop_stone_bench"};
    public static boolean lamp(String asset) { return LAMPS.contains(asset) || asset.startsWith("city_prop_refresh_lamp_"); }

    public static void refine(WorldMap world, MapArea area) {
        if (area.id.equals(WorldMap.PLAYER_VILLAGE_ID) || area.id.startsWith("editor_")
                || !(area.kind.equals("city") || area.kind.equals("village"))) return;
        // Only ordinary street furniture is recomposed. Quest, regional and player-authored props stay intact.
        var ordinary = area.props.stream().filter(p -> lamp(p.asset()) || p.asset().startsWith("city_prop_market_")
                || p.asset().equals("city_prop_moss_barrel_planter") || SettlementSpriteScale.matches(p.asset())).toList();
        ordinary.forEach(area::removeProp);
        int marketIndex = Math.floorMod(area.id.hashCode(), MARKET.length);
        for (var prop : ordinary) {
            if (lamp(prop.asset())) continue;
            boolean market = prop.asset().startsWith("city_prop_market_") || Arrays.asList(MARKET).contains(prop.asset());
            String[] palette = market ? MARKET : PLANTS;
            int index = market ? marketIndex++ : prop.x() * 3 + prop.y();
            TilePoint point = findShoulder(world, area, prop.x(), prop.y());
            if (point == null) continue;
            long crowded = area.propsInBounds(point.x() - 2, point.y() - 2, point.x() + 2, point.y() + 2).stream()
                    .filter(p -> Arrays.asList(palette).contains(p.asset())).count();
            if (crowded >= 2) continue;
            String asset = palette[Math.floorMod(index, palette.length)];
            int size = asset.contains("market_") || asset.contains("cart") || asset.contains("kiosk") ? 38 : 28;
            if (SettlementSpriteScale.matches(asset)) size = SettlementSpriteScale.size(asset);
            area.addProp(new WorldProp(point.x(), point.y(), asset, size));
        }
        placeStreetLights(world, area);
        area.markVisualChange();
    }

    private static TilePoint findShoulder(WorldMap world, MapArea area, int x, int y) {
        for (int radius = 0; radius <= 2; radius++) for (int dy = -radius; dy <= radius; dy++)
            for (int dx = -radius; dx <= radius; dx++) {
                if (Math.abs(dx) + Math.abs(dy) != radius) continue;
                if (free(world, area, x + dx, y + dy)) return new TilePoint(x + dx, y + dy);
            }
        return null;
    }

    private static boolean free(WorldMap world, MapArea area, int x, int y) {
        TilePoint point = new TilePoint(x, y);
        if (area.districts.stream().anyMatch(d -> d.work().equals(point) || d.gathering().equals(point) || d.center().equals(point))) return false;
        if (world.npcs(area.id).stream().anyMatch(n -> n.x() == x && n.y() == y)) return false;
        if (x < 2 || y < 2 || x >= area.width() - 2 || y >= area.height() - 2
                || Terrain.connectingRoad(area.tileAt(x, y)) || !world.isGroundPassable(area.id, x, y)
                || world.cityBuildingAt(area.id, x, y) != null || area.propAt(x, y) != null
                || world.transitionAt(area.id, x, y) != null || area.landmarks.containsKey(new TilePoint(x, y))) return false;
        for (var b : world.cityBuildings(area.id)) for (var door : world.cityBuildingDoorTiles(b))
            if (Math.abs(door.x() - x) <= 1 && y >= door.y() && y <= door.y() + 2) return false;
        return area.propsInBounds(x - 2, y - 2, x + 2, y + 2).stream()
                .noneMatch(p -> p.asset().startsWith("town_portal_"));
    }

    private record LampCandidate(int x, int y, int priority) { }
    private static void placeStreetLights(WorldMap world, MapArea area) {
        var candidates = new ArrayList<LampCandidate>();
        for (int y = 2; y < area.height() - 2; y++) for (int x = 2; x < area.width() - 2; x++) {
            if (!Terrain.connectingRoad(area.tileAt(x, y))) continue;
            boolean north = road(area, x, y - 1), south = road(area, x, y + 1);
            boolean east = road(area, x + 1, y), west = road(area, x - 1, y);
            int degree = (north ? 1 : 0) + (south ? 1 : 0) + (east ? 1 : 0) + (west ? 1 : 0);
            boolean junction = degree >= 3, corner = (north || south) && (east || west);
            if (!junction && !corner && Math.floorMod((north || south ? y : x) + area.id.hashCode(), 6) != 0) continue;
            int side = Math.floorMod((north || south ? y : x) / 6 + area.id.hashCode(), 2) == 0 ? 1 : -1;
            int[][] offsets = north || south ? new int[][]{{side, 0}, {-side, 0}} : new int[][]{{0, side}, {0, -side}};
            for (int[] offset : offsets) {
                int px = x + offset[0], py = y + offset[1];
                if (free(world, area, px, py)) candidates.add(new LampCandidate(px, py, junction ? 0 : corner ? 1 : 2));
            }
        }
        candidates.sort(Comparator.comparingInt(LampCandidate::priority).thenComparingInt(LampCandidate::y)
                .thenComparingInt(LampCandidate::x));
        var placed = new ArrayList<TilePoint>();
        for (var c : candidates) {
            if (!free(world, area, c.x, c.y) || placed.stream().anyMatch(p -> Math.hypot(p.x() - c.x, p.y() - c.y) < 5)) continue;
            // Brackets point into the adjacent travel lane; north/south shoulders use a vertical lamp.
            String asset = "city_prop_refresh_lamp_" + (road(area, c.x + 1, c.y) ? "right"
                    : road(area, c.x - 1, c.y) ? "left" : "iron");
            area.addProp(new WorldProp(c.x, c.y, asset, SettlementSpriteScale.size(asset)));
            placed.add(new TilePoint(c.x, c.y));
        }
    }
    private static boolean road(MapArea area, int x, int y) { return Terrain.connectingRoad(area.tileAt(x, y)); }
}
