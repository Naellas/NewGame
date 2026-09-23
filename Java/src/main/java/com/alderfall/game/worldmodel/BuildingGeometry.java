package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Sprite sizing and solid ground bases, independent of camera and zoom. */
public final class BuildingGeometry {
    private BuildingGeometry() { }

    public static boolean standalone(String mapId, String kind, CityBuilding building) {
        if (!RegionalSettlementIdentity.buildingAsset(mapId, building).isEmpty()
                || !WesternReachFolklore.buildingAsset(mapId, building).isEmpty()) return true;
        if (("village".equals(kind) || building.key() != null && building.key().startsWith("player_"))
                && VillageManager.isManagedBuildingStyle(building.style())) return true;
        return List.of("warehouse", "blacksmith", "bakery", "restaurant", "apothecary", "alchemist",
                "forestry_hut", "farmstead", "fishing_hut", "workshop", "carpenter", "mine", "granary",
                "watchtower", "shrine", "hall", "barracks", "inn", "arena", "mage_tower", "bell_tower",
                "sun_shrine", "river_hall").contains(building.style());
    }

    public static List<Rectangle2D.Double> footprints(WorldMap world, String mapId, CityBuilding building) {
        List<Rectangle2D.Double> result = new ArrayList<>();
        List<TilePoint> doors = world.cityBuildingDoorTiles(building);
        boolean single = standalone(mapId, world.kind(mapId), building);
        int[] size = targetSize(mapId, building, building.width() * GameConfig.TILE, GameConfig.TILE);
        boolean tower = List.of("watchtower", "mage_tower", "bell_tower").contains(building.style());
        int parts = single ? 1 : moduleCount(building);
        for (int index = 0; index < parts; index++) {
            var visual = single ? new Rectangle2D.Double(
                    doors.get(0).x() + .5 - size[0] / (2.0 * GameConfig.TILE),
                    building.y2() + 1 - (size[1] + 1.0) / GameConfig.TILE,
                    size[0] / (double) GameConfig.TILE, size[1] / (double) GameConfig.TILE)
                    : moduleVisualBounds(building, index, GameConfig.TILE);
            if (!single) visual = new Rectangle2D.Double(visual.x / GameConfig.TILE, visual.y / GameConfig.TILE,
                    visual.width / GameConfig.TILE, visual.height / GameConfig.TILE);
            double width = visual.width * (tower ? .76 : .90);
            double depth = Math.min(building.depth() - .12,
                    Math.max(1.15, visual.height * (tower ? .65 : .80)));
            double bottom = building.y2() + 1.0 - 1.0 / GameConfig.TILE;
            double left = visual.getCenterX() - width / 2;
            double right = visual.getCenterX() + width / 2;
            result.add(new Rectangle2D.Double(left, bottom - depth, Math.max(.5, right - left), depth));
        }
        return List.copyOf(result);
    }

    public static int moduleCount(CityBuilding building) {
        boolean civic = List.of("hall", "guild", "barracks", "warehouse").contains(building.style());
        if (civic) return Math.max(1, Math.min(3, (building.width() + 2) / 4));
        int modules = Math.max(1, Math.min(building.width(), (building.width() + 1) / 2));
        return "row".equals(building.style()) && building.width() >= 6 ? Math.min(4, modules) : modules;
    }

    /** Pixel bounds at camera origin, shared by drawing and ground-footprint placement. */
    public static Rectangle2D.Double moduleVisualBounds(CityBuilding building, int index, int ts) {
        boolean civic = List.of("hall", "guild", "barracks", "warehouse").contains(building.style());
        boolean compact = List.of("guild", "house", "shop", "row").contains(building.style());
        int minH = civic ? 108 : compact ? 102 : 90, maxH = civic ? 166 : compact ? 150 : 134;
        int minW = civic ? 78 : compact ? 72 : 58, maxW = civic ? 128 : compact ? 122 : 104;
        int modules = moduleCount(building), lotW = building.width() * ts;
        int height = Math.max(tileRelative(minH, ts), Math.min(tileRelative(maxH, ts),
                building.depth() * ts + tileRelative(civic ? 50 : 42, ts)));
        int width = Math.max(tileRelative(minW, ts), Math.min(lotW / modules
                + tileRelative(civic ? 42 : 34, ts), tileRelative(maxW, ts)));
        int seed = Math.abs((building.x1() + building.x2()) * 928371
                + (building.y1() + building.y2()) * 364479 + building.palette() * 811);
        int jitter = ((seed >> (index * 4)) & 7) - 3;
        int center = building.x1() * ts + (int) Math.round((index + .5) * lotW / modules);
        return new Rectangle2D.Double(center - width / 2 + jitter * ts / GameConfig.TILE,
                (building.y2() + 1) * ts - height - tileRelative(5, ts), width, height);
    }

    private static int tileRelative(int value, int ts) {
        return Math.max(1, (int) Math.round(value * ts / (double) GameConfig.TILE));
    }

    public static int[] targetSize(String mapId, CityBuilding building, int lotW, int ts) {
        RegionalBuildingTypes.Type regional = RegionalBuildingTypes.type(mapId, building);
        if (regional != null) {
            int height = switch (regional) {
                case REMEMBRANCE_HALL, BELLHOUSE -> 232;
                case CARAVANSERAI, RESCUE_LODGE -> 210;
                case GRANARY -> 188;
                default -> 168;
            };
            return new int[]{Math.min(lotW + tileRelative(12, ts), tileRelative(204, ts)),
                    Math.min(building.depth() * ts + tileRelative(136, ts), tileRelative(height, ts))};
        }
        if ("garden".equals(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(10, ts), tileRelative(144, ts)),
                    Math.min(building.depth() * ts + tileRelative(18, ts), tileRelative(96, ts))
            };
        }
        if (List.of("watchtower", "mage_tower", "bell_tower").contains(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(16, ts), tileRelative(156, ts)),
                    Math.min(building.depth() * ts + tileRelative(88, ts), tileRelative(224, ts))
            };
        }
        if ("barracks".equals(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(28, ts), tileRelative(178, ts)),
                    Math.min(building.depth() * ts + tileRelative(72, ts), tileRelative(190, ts))
            };
        }
        if (List.of("hall", "arena", "river_hall").contains(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(42, ts), tileRelative(252, ts)),
                    Math.min(building.depth() * ts + tileRelative(96, ts), tileRelative(232, ts))
            };
        }
        if (List.of("sun_shrine", "shrine").contains(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(48, ts), tileRelative(236, ts)),
                    Math.min(building.depth() * ts + tileRelative(104, ts), tileRelative(230, ts))
            };
        }
        if ("warehouse".equals(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(42, ts), tileRelative(220, ts)),
                    Math.min(building.depth() * ts + tileRelative(74, ts), tileRelative(198, ts))
            };
        }
        if (List.of("blacksmith", "workshop", "carpenter", "forestry_hut", "farmstead", "fishing_hut").contains(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(34, ts), tileRelative(190, ts)),
                    Math.min(building.depth() * ts + tileRelative(62, ts), tileRelative(176, ts))
            };
        }
        if (List.of("bakery", "restaurant", "apothecary", "alchemist", "granary").contains(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(28, ts), tileRelative(172, ts)),
                    Math.min(building.depth() * ts + tileRelative(58, ts), tileRelative(164, ts))
            };
        }
        if (List.of("inn", "guild").contains(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(32, ts), tileRelative(212, ts)),
                    Math.min(building.depth() * ts + tileRelative(66, ts), tileRelative(188, ts))
            };
        }
        if ("row".equals(building.style())) {
            return new int[]{
                    Math.min(lotW + tileRelative(30, ts), tileRelative(184, ts)),
                    Math.min(building.depth() * ts + tileRelative(52, ts), tileRelative(158, ts))
            };
        }
        return new int[]{
                Math.min(lotW + tileRelative(12, ts), tileRelative(146, ts)),
                Math.min(building.depth() * ts + tileRelative(36, ts), tileRelative(132, ts))
        };
    }

}
