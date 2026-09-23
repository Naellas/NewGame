package com.alderfall.game.map;

import com.alderfall.game.Terrain;
import com.alderfall.game.TilePoint;
import com.alderfall.game.WorldProp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class OverworldLandmarkPropGenerator {
    private OverworldLandmarkPropGenerator() {
    }

    static void addDiscoverabilityProps(MapArea area) {
        boolean[][] occupied = occupiedTiles(area);
        List<Map.Entry<TilePoint, String>> landmarks = new ArrayList<>(area.landmarks.entrySet());
        landmarks.sort(Comparator
                .comparingInt((Map.Entry<TilePoint, String> entry) -> entry.getKey().y())
                .thenComparingInt(entry -> entry.getKey().x()));

        for (Map.Entry<TilePoint, String> entry : landmarks) {
            TilePoint point = entry.getKey();
            String label = entry.getValue();
            char tile = area.tileAt(point.x(), point.y());
            if (tile == 'c' || tile == 'u') {
                addSettlementHooks(area, occupied, point, label, tile);
            } else {
                addLocationHooks(area, occupied, point, label, tile);
            }
        }
    }

    private static void addSettlementHooks(MapArea area, boolean[][] occupied, TilePoint point, String label, char tile) {
        int salt = labelSalt(label, point);
        placeNearRoad(area, occupied, point,
                tile == 'c' ? "deco_imagen_milestone" : "deco_imagen_signpost",
                42, 3, 8, salt);
        placeNearNatural(area, occupied, point,
                tile == 'c' ? "deco_imagen_road_camp" : "location_camp_fire",
                tile == 'c' ? 48 : 38, 4, 7, salt + 19);
        placeNearNatural(area, occupied, point,
                settlementShrineAsset(label, salt), 48, 5, 9, salt + 37);
        if (isForestVillage(label)) {
            placeNearNatural(area, occupied, point, "deco_tree_elder_harvestable", 76, 5, 10, salt + 53);
        }
    }

    private static void addLocationHooks(MapArea area, boolean[][] occupied, TilePoint point, String label, char tile) {
        // Playable dungeons already have regional, purpose-specific blueprint markers.
        if (tile == 'd') return;
        String lower = label.toLowerCase(Locale.ROOT);
        int salt = labelSalt(label, point);
        if (lower.contains("camp")) {
            placeNearNatural(area, occupied, point, "location_camp_fire", 38, 1, 4, salt);
            placeNearNatural(area, occupied, point, "deco_imagen_road_camp", 48, 3, 6, salt + 11);
            placeNearRoad(area, occupied, point, "deco_imagen_signpost", 40, 3, 9, salt + 17);
            return;
        }
        if (lower.contains("farm")) {
            placeNearRoad(area, occupied, point, "deco_imagen_signpost", 38, 2, 8, salt);
            placeNearNatural(area, occupied, point, "location_farmland_hay_bales", 46, 2, 5, salt + 13);
            return;
        }
        if (lower.contains("grave") || lower.contains("crypt") || lower.contains("gate")) {
            placeNearNatural(area, occupied, point, "deco_imagen_shrine_stone", 48, 2, 6, salt);
            placeNearNatural(area, occupied, point, "deco_imagen_green_rune_stone", 44, 3, 7, salt + 23);
            placeNearRoad(area, occupied, point, "deco_mountain_cairn", 42, 3, 9, salt + 31);
            return;
        }
        if (lower.contains("cave")) {
            placeNearNatural(area, occupied, point, "deco_mountain_pass_way_cairn", 44, 2, 6, salt);
            placeNearNatural(area, occupied, point, "deco_imagen_green_rune_stone", 42, 3, 7, salt + 29);
            return;
        }
        if (lower.contains("ruin") || lower.contains("castle")) {
            placeNearNatural(area, occupied, point, "location_ruin_standing_stones", 58, 2, 6, salt);
            placeNearNatural(area, occupied, point, "deco_imagen_stone_stack", 42, 3, 7, salt + 41);
            return;
        }
        placeNearRoad(area, occupied, point, "deco_imagen_milestone", 38, 3, 9, salt);
        if (tile != 'd') {
            placeNearNatural(area, occupied, point, "deco_imagen_shrine_stone", 44, 3, 7, salt + 47);
        }
    }

    private static String settlementShrineAsset(String label, int salt) {
        String lower = label.toLowerCase(Locale.ROOT);
        if (lower.contains("snow") || lower.contains("pine") || lower.contains("highwall")) {
            return "deco_imagen_tundra_rune_stone";
        }
        if (lower.contains("sanctum") || lower.contains("sun") || lower.contains("dune") || lower.contains("ember")) {
            return "deco_imagen_shrine_stone";
        }
        return Math.floorMod(salt, 3) == 0 ? "deco_forest_shrine_stone" : "deco_imagen_shrine_stone";
    }

    private static boolean isForestVillage(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        return lower.contains("oak") || lower.contains("pine") || lower.contains("elder") || lower.contains("fox");
    }

    private static boolean placeNearRoad(MapArea area, boolean[][] occupied, TilePoint center, String asset,
                                         int size, int minRadius, int maxRadius, int salt) {
        return placeNearby(area, occupied, center, asset, size, minRadius, maxRadius, salt,
                tile -> Terrain.connectingRoad(tile));
    }

    private static boolean placeNearNatural(MapArea area, boolean[][] occupied, TilePoint center, String asset,
                                            int size, int minRadius, int maxRadius, int salt) {
        return placeNearby(area, occupied, center, asset, size, minRadius, maxRadius, salt,
                tile -> Terrain.passable(tile) && tile != 'c' && tile != 'u' && tile != 'd'
                        && !Terrain.connectingRoad(tile) && tile != 'w' && tile != '~');
    }

    private static boolean placeNearby(MapArea area, boolean[][] occupied, TilePoint center, String asset,
                                       int size, int minRadius, int maxRadius, int salt, TileRule tileRule) {
        TilePoint target = findNearby(area, occupied, center, minRadius, maxRadius, salt, tileRule);
        if (target == null) {
            return false;
        }
        area.addProp(new WorldProp(target.x(), target.y(), asset, size));
        occupied[target.y()][target.x()] = true;
        return true;
    }

    private static TilePoint findNearby(MapArea area, boolean[][] occupied, TilePoint center,
                                        int minRadius, int maxRadius, int salt, TileRule tileRule) {
        List<TilePoint> candidates = new ArrayList<>();
        for (int y = center.y() - maxRadius; y <= center.y() + maxRadius; y++) {
            for (int x = center.x() - maxRadius; x <= center.x() + maxRadius; x++) {
                int distance = Math.abs(x - center.x()) + Math.abs(y - center.y());
                if (distance < minRadius || distance > maxRadius || !inside(area, x, y)) {
                    continue;
                }
                if (occupied[y][x] || !tileRule.accept(area.tileAt(x, y))) {
                    continue;
                }
                candidates.add(new TilePoint(x, y));
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator
                .comparingInt((TilePoint point) -> Math.abs(point.x() - center.x()) + Math.abs(point.y() - center.y()))
                .thenComparingInt(point -> Math.floorMod(point.x() * 7349 + point.y() * 9127 + salt, 997))
                .thenComparingInt(TilePoint::y)
                .thenComparingInt(TilePoint::x));
        return candidates.get(0);
    }

    private static boolean[][] occupiedTiles(MapArea area) {
        boolean[][] occupied = new boolean[area.height()][area.width()];
        for (WorldProp prop : area.props) {
            if (inside(area, prop.x(), prop.y())) {
                occupied[prop.y()][prop.x()] = true;
            }
        }
        return occupied;
    }

    private static boolean inside(MapArea area, int x, int y) {
        return x >= 0 && y >= 0 && x < area.width() && y < area.height();
    }

    private static int labelSalt(String label, TilePoint point) {
        return (label == null ? 0 : label.hashCode()) ^ point.x() * 928371 ^ point.y() * 364479;
    }

    private interface TileRule {
        boolean accept(char tile);
    }
}
