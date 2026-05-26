package com.alderfall.game;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorldMap {
    public static final int COLS = 300;
    public static final int ROWS = 300;
    public static final TilePoint START_POSITION = new TilePoint(112, 158);
    public static final String OVERWORLD_ID = "overworld";

    private final char[][] tiles = new char[ROWS][COLS];
    private final Map<TilePoint, String> landmarks = new HashMap<>();
    private final Map<String, MapArea> maps = new LinkedHashMap<>();
    private final Map<String, WorldTransition> transitions = new HashMap<>();
    private final Map<String, List<CityBuilding>> cityBuildings = new HashMap<>();
    private final List<LocationPatch> locationPatches = new ArrayList<>();

    public WorldMap(long seed) {
        build(seed);
        registerMaps();
    }

    public char tileAt(int x, int y) {
        return tileAt(OVERWORLD_ID, x, y);
    }

    public char tileAt(String mapId, int x, int y) {
        MapArea area = area(mapId);
        if (area == null) {
            return 'm';
        }
        return area.tileAt(x, y);
    }

    public boolean isPassable(String mapId, int x, int y) {
        return Terrain.passable(tileAt(mapId, x, y));
    }

    public int width(String mapId) {
        MapArea area = area(mapId);
        return area == null ? COLS : area.width();
    }

    public int height(String mapId) {
        MapArea area = area(mapId);
        return area == null ? ROWS : area.height();
    }

    public String label(String mapId) {
        MapArea area = area(mapId);
        return area == null ? "Unknown" : area.label;
    }

    public String kind(String mapId) {
        MapArea area = area(mapId);
        return area == null ? "overworld" : area.kind;
    }

    public List<WorldProp> props(String mapId) {
        MapArea area = area(mapId);
        return area == null ? List.of() : area.props;
    }

    public WorldTransition transitionAt(String mapId, int x, int y) {
        return transitions.get(transitionKey(mapId, x, y));
    }

    public boolean isInteriorMap(String mapId) {
        return "city".equals(kind(mapId)) || "village".equals(kind(mapId)) || "dungeon".equals(kind(mapId)) || "interior".equals(kind(mapId));
    }

    public MapArea area(String mapId) {
        return maps.getOrDefault(mapId, maps.get(OVERWORLD_ID));
    }

    public boolean hasMap(String mapId) {
        return maps.containsKey(mapId);
    }

    public String ensureHouseInterior(String sourceMapId, int wx, int wy, int returnX, int returnY) {
        CityBuilding building = cityBuildingAt(sourceMapId, wx, wy);
        if (building != null) {
            TilePoint anchor = building.anchor();
            wx = anchor.x();
            wy = anchor.y();
        }
        String mapId = "house_" + sourceMapId + "_" + wx + "_" + wy;
        if (!maps.containsKey(mapId)) {
            String label = "city".equals(kind(sourceMapId)) ? "City Interior" : "House Interior";
            MapArea house = new MapArea(mapId, label, "interior", houseTiles(wx * 928371 + wy * 364479 + sourceMapId.hashCode()));
            addHouseProps(house);
            maps.put(mapId, house);
        }
        addTransition(mapId, 11, 15, sourceMapId, returnX, returnY, "You step back outside.");
        addTransition(mapId, 12, 15, sourceMapId, returnX, returnY, "You step back outside.");
        return mapId;
    }

    public List<CityBuilding> cityBuildings(String mapId) {
        return cityBuildings.getOrDefault(mapId, List.of());
    }

    public CityBuilding cityBuildingAt(String mapId, int x, int y) {
        for (CityBuilding building : cityBuildings(mapId)) {
            if (building.contains(x, y)) {
                return building;
            }
        }
        return null;
    }

    public List<TilePoint> cityBuildingDoorTiles(CityBuilding building) {
        List<Integer> centers = new ArrayList<>();
        if (building.width() <= 3 || List.of("hall", "guild", "barracks", "warehouse").contains(building.style())) {
            centers.add(building.x1() + building.width() / 2);
        } else {
            int count = building.width() <= 6 ? 2 : 3;
            if ("row".equals(building.style()) && building.width() >= 8) {
                count = 4;
            }
            for (int index = 0; index < count; index++) {
                int center = (int) Math.round(building.x1() + (index + 0.5) * building.width() / count - 0.5);
                centers.add(Math.max(building.x1(), Math.min(building.x2(), center)));
            }
        }
        List<TilePoint> doors = new ArrayList<>();
        for (int center : centers) {
            TilePoint door = new TilePoint(center, building.y2());
            if (!doors.contains(door)) {
                doors.add(door);
            }
        }
        return doors;
    }

    public CityBuilding cityBuildingEntryAt(String mapId, int x, int y, int fromX, int fromY) {
        CityBuilding building = cityBuildingAt(mapId, x, y);
        if (building == null || y != building.y2() || fromX != x || fromY != y + 1) {
            return null;
        }
        return cityBuildingDoorTiles(building).contains(new TilePoint(x, y)) ? building : null;
    }

    public boolean isPassable(int x, int y) {
        return isPassable(OVERWORLD_ID, x, y);
    }

    public String describe(String mapId, int x, int y) {
        String landmark = landmarkAt(mapId, x, y);
        if (landmark != null) {
            return landmark;
        }
        LocationPatch location = locationAt(mapId, x, y);
        if (location != null) {
            return location.label();
        }
        return Terrain.name(tileAt(mapId, x, y));
    }

    public String describe(int x, int y) {
        return describe(OVERWORLD_ID, x, y);
    }

    public String landmarkAt(String mapId, int x, int y) {
        MapArea area = area(mapId);
        return area == null ? null : area.landmarks.get(new TilePoint(x, y));
    }

    public String landmarkAt(int x, int y) {
        return landmarkAt(OVERWORLD_ID, x, y);
    }

    public List<LocationSite> locationSites(String kind) {
        List<LocationSite> sites = new ArrayList<>();
        for (LocationPatch patch : locationPatches) {
            if (patch.kind.equals(kind)) {
                sites.add(new LocationSite(patch.kind, patch.label(), patch.cx, patch.cy));
            }
        }
        return sites;
    }

    public TilePoint objectivePoint(String locationKind, int locationIndex, int variant) {
        List<LocationPatch> matches = new ArrayList<>();
        for (LocationPatch patch : locationPatches) {
            if (patch.kind.equals(locationKind)) {
                matches.add(patch);
            }
        }
        if (matches.isEmpty()) {
            return START_POSITION;
        }
        LocationPatch patch = matches.get(Math.floorMod(locationIndex, matches.size()));
        int[][] offsets = {
                {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
                {2, 0}, {-2, 0}, {0, 2}, {0, -2},
                {2, 1}, {-2, 1}, {2, -1}, {-2, -1},
                {1, 2}, {-1, 2}, {1, -2}, {-1, -2}
        };
        int start = Math.floorMod(variant * 5 + patch.salt, offsets.length);
        for (int i = 0; i < offsets.length; i++) {
            int[] offset = offsets[(start + i) % offsets.length];
            int x = patch.cx + offset[0];
            int y = patch.cy + offset[1];
            if (patch.contains(x, y) && isPassable(OVERWORLD_ID, x, y)) {
                return new TilePoint(x, y);
            }
        }
        return new TilePoint(patch.cx, patch.cy);
    }

    private void registerMaps() {
        MapArea overworld = new MapArea(OVERWORLD_ID, "Alderfall Overworld", "overworld", tiles);
        overworld.landmarks.putAll(landmarks);
        addBiomeProps(overworld, 0);
        addLocationProps(overworld);
        maps.put(OVERWORLD_ID, overworld);

        addCity("city_riverside", "Riverside City", 82, 105, "riverside");
        addCity("city_archive", "Archive City", 152, 145, "archive");
        addCity("city_highwall", "Highwall City", 205, 78, "highwall");
        addCity("city_belltower", "Belltower City", 228, 185, "belltower");
        addCity("city_sanctum", "Sanctum City", 150, 230, "sanctum");

        addVillage("village_oakhaven", "Oakhaven Village", 112, 158, "green");
        addVillage("village_snowrest", "Snowrest Village", 83, 62, "snow");
        addVillage("village_dunewick", "Dunewick Village", 102, 245, "desert");
        addVillage("village_mireford", "Mireford Village", 240, 153, "marsh");

        addDungeon("dungeon_stonegate_1", "Stonegate Dungeon", 196, 62, 1);
        addDungeon("dungeon_miredepth_1", "Miredepth Dungeon", 255, 177, 1);
        addDungeon("dungeon_frosthollow_1", "Frosthollow Dungeon", 72, 218, 1);
        addDungeon("dungeon_blackvault_1", "Blackvault Dungeon", 194, 235, 1);
        MapArea deepVault = new MapArea("dungeon_stonegate_2", "Stonegate Deep Vault", "dungeon", dungeonTiles(2));
        addDungeonProps(deepVault);
        maps.put(deepVault.id, deepVault);
        addTransition("dungeon_stonegate_1", 28, 10, "dungeon_stonegate_2", 2, 10, "You descend to the deep vault.");
        addTransition("dungeon_stonegate_2", 1, 10, "dungeon_stonegate_1", 27, 10, "You return to the upper halls.");
    }

    private void addCity(String id, String label, int ox, int oy, String variant) {
        cityBuildings.put(id, cityBuildingTemplates(variant));
        MapArea area = new MapArea(id, label, "city", cityTiles(variant));
        addCityProps(area);
        maps.put(id, area);
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (Math.abs(dx) + Math.abs(dy) <= 3) {
                    TilePoint entry = cityEntryPoint(dx, dy);
                    addTransition(OVERWORLD_ID, ox + dx, oy + dy, id, entry.x(), entry.y(), "You enter " + label + ".");
                }
            }
        }
        addTransition(id, 16, 0, OVERWORLD_ID, ox, oy - 3, "You leave " + label + ".");
        addTransition(id, 17, 0, OVERWORLD_ID, ox, oy - 3, "You leave " + label + ".");
        addTransition(id, 16, 27, OVERWORLD_ID, ox, oy + 3, "You leave " + label + ".");
        addTransition(id, 17, 27, OVERWORLD_ID, ox, oy + 3, "You leave " + label + ".");
        addTransition(id, 0, 11, OVERWORLD_ID, ox - 3, oy, "You leave " + label + ".");
        addTransition(id, 0, 12, OVERWORLD_ID, ox - 3, oy, "You leave " + label + ".");
        addTransition(id, 35, 11, OVERWORLD_ID, ox + 3, oy, "You leave " + label + ".");
        addTransition(id, 35, 12, OVERWORLD_ID, ox + 3, oy, "You leave " + label + ".");
        area.landmarks.put(new TilePoint(17, 11), label);
    }

    private void addVillage(String id, String label, int ox, int oy, String variant) {
        cityBuildings.put(id, villageBuildingTemplates());
        MapArea area = new MapArea(id, label, "village", villageTiles(variant));
        addVillageProps(area, variant);
        maps.put(id, area);
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (Math.abs(dx) + Math.abs(dy) <= 3) {
                    TilePoint entry = villageEntryPoint(dx, dy);
                    addTransition(OVERWORLD_ID, ox + dx, oy + dy, id, entry.x(), entry.y(), "You enter " + label + ".");
                }
            }
        }
        addTransition(id, 13, 0, OVERWORLD_ID, ox, oy - 3, "You leave " + label + ".");
        addTransition(id, 14, 0, OVERWORLD_ID, ox, oy - 3, "You leave " + label + ".");
        addTransition(id, 13, 27, OVERWORLD_ID, ox, oy + 3, "You leave " + label + ".");
        addTransition(id, 14, 27, OVERWORLD_ID, ox, oy + 3, "You leave " + label + ".");
        addTransition(id, 0, 9, OVERWORLD_ID, ox - 3, oy, "You leave " + label + ".");
        addTransition(id, 0, 10, OVERWORLD_ID, ox - 3, oy, "You leave " + label + ".");
        addTransition(id, 35, 9, OVERWORLD_ID, ox + 3, oy, "You leave " + label + ".");
        addTransition(id, 35, 10, OVERWORLD_ID, ox + 3, oy, "You leave " + label + ".");
        area.landmarks.put(new TilePoint(14, 9), label);
    }

    private TilePoint cityEntryPoint(int dx, int dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx < 0 ? new TilePoint(1, 12) : new TilePoint(34, 12);
        }
        if (dy < 0) {
            return new TilePoint(17, 1);
        }
        if (dy > 0) {
            return new TilePoint(17, 26);
        }
        return new TilePoint(17, 21);
    }

    private TilePoint villageEntryPoint(int dx, int dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx < 0 ? new TilePoint(1, 10) : new TilePoint(34, 10);
        }
        if (dy < 0) {
            return new TilePoint(14, 1);
        }
        if (dy > 0) {
            return new TilePoint(14, 26);
        }
        return new TilePoint(14, 17);
    }

    private void addDungeon(String id, String label, int ox, int oy, int depth) {
        MapArea area = new MapArea(id, label, "dungeon", dungeonTiles(depth));
        addDungeonProps(area);
        maps.put(id, area);
        addTransition(OVERWORLD_ID, ox, oy, id, 2, 10, "You descend into " + label + ".");
        addTransition(id, 1, 10, OVERWORLD_ID, ox, oy, "You climb back to the surface.");
        area.landmarks.put(new TilePoint(2, 10), label);
    }

    private void addTransition(String fromMap, int fromX, int fromY, String toMap, int toX, int toY, String message) {
        transitions.put(transitionKey(fromMap, fromX, fromY), new WorldTransition(toMap, toX, toY, message));
    }

    private String transitionKey(String mapId, int x, int y) {
        return mapId + ":" + x + ":" + y;
    }

    private char[][] cityTiles(String variant) {
        char[][] grid = filled(36, 28, 'p');
        border(grid, 'x');
        rect(grid, 16, 0, 17, 27, 'r');
        rect(grid, 0, 11, 35, 12, 'r');
        rect(grid, 2, 6, 31, 6, 'r');
        rect(grid, 2, 18, 31, 18, 'r');
        rect(grid, 6, 21, 27, 21, 'r');
        rect(grid, 13, 9, 21, 14, 'y');
        rect(grid, 15, 10, 19, 13, 'p');
        if ("riverside".equals(variant)) {
            rect(grid, 2, 3, 31, 3, 'w');
            rect(grid, 16, 3, 17, 3, 'r');
            rect(grid, 4, 7, 6, 7, 'a');
            rect(grid, 26, 19, 28, 20, 'a');
        } else if ("archive".equals(variant)) {
            rect(grid, 4, 7, 6, 8, 'y');
            rect(grid, 27, 7, 29, 8, 'y');
            rect(grid, 12, 8, 22, 8, 'a');
            rect(grid, 16, 7, 17, 8, 't');
        } else if ("highwall".equals(variant)) {
            rect(grid, 3, 2, 30, 2, 'x');
            rect(grid, 3, 21, 30, 21, 'x');
            rect(grid, 9, 7, 10, 8, 't');
            rect(grid, 19, 15, 20, 16, 't');
        } else if ("belltower".equals(variant)) {
            rect(grid, 16, 4, 17, 16, 'd');
            rect(grid, 15, 7, 18, 9, 't');
            rect(grid, 7, 15, 11, 16, 'a');
        } else if ("sanctum".equals(variant)) {
            rect(grid, 11, 7, 23, 7, 'x');
            rect(grid, 11, 13, 23, 13, 'x');
            rect(grid, 16, 8, 17, 9, 't');
            rect(grid, 7, 9, 10, 13, 'y');
            rect(grid, 24, 11, 27, 15, 'y');
        }
        paintCitySurfaceZones(grid);
        for (CityBuilding building : cityBuildingTemplates(variant)) {
            rect(grid, building.x1(), building.y1(), building.x2(), building.y2(), 'h');
        }
        grid[27][16] = 'r';
        grid[27][17] = 'r';
        grid[0][16] = 'r';
        grid[0][17] = 'r';
        grid[11][0] = 'r';
        grid[12][0] = 'r';
        grid[11][35] = 'r';
        grid[12][35] = 'r';
        return grid;
    }

    private void paintCitySurfaceZones(char[][] grid) {
        rectIf(grid, 2, 7, 31, 7, 'l', 'p');
        rectIf(grid, 2, 13, 31, 13, 'l', 'p');
        rectIf(grid, 2, 20, 31, 20, 'l', 'p');
        rectIf(grid, 7, 7, 11, 10, 'j', 'p', 'l');
        rectIf(grid, 21, 7, 25, 10, 'j', 'p', 'l');
        rectIf(grid, 2, 14, 13, 17, 'j', 'p', 'l');
        rectIf(grid, 18, 14, 31, 17, 'j', 'p', 'l');
        rectIf(grid, 2, 19, 13, 20, 'j', 'p', 'l');
        rectIf(grid, 18, 19, 31, 20, 'j', 'p', 'l');
    }

    private List<CityBuilding> cityBuildingTemplates(String variant) {
        return switch (variant) {
            case "archive" -> List.of(
                    building("west_stacks", 3, 3, 8, 5, "guild", 1),
                    building("north_study", 10, 3, 14, 5, "house", 0),
                    building("north_gate_homes", 18, 4, 20, 5, "house", 2),
                    building("east_stacks", 21, 3, 27, 5, "guild", 2),
                    building("illuminator_house", 28, 4, 31, 5, "shop", 1),
                    building("inner_west_row", 7, 8, 10, 9, "row", 1),
                    building("west_scriptorium", 3, 8, 6, 10, "guild", 0),
                    building("inner_east_row", 23, 8, 26, 9, "row", 0),
                    building("east_scriptorium", 27, 8, 30, 10, "guild", 2),
                    building("west_archive_homes", 4, 14, 9, 17, "row", 1),
                    building("records_hall", 11, 15, 15, 17, "hall", 2),
                    building("east_archive_homes", 22, 14, 29, 17, "row", 0),
                    building("southwest_corner_row", 2, 19, 5, 20, "row", 2),
                    building("south_bindery", 8, 19, 13, 20, "shop", 1),
                    building("south_mid_shop", 18, 19, 20, 20, "shop", 1),
                    building("south_reading_room", 21, 19, 26, 20, "guild", 0),
                    building("southeast_scribes", 28, 19, 30, 20, "house", 2)
            );
            case "highwall" -> List.of(
                    building("west_barracks", 3, 4, 8, 5, "barracks", 0),
                    building("north_armory", 10, 4, 14, 5, "guild", 1),
                    building("north_gate_homes", 18, 4, 20, 5, "house", 2),
                    building("east_barracks", 21, 4, 27, 5, "barracks", 2),
                    building("east_guardhouse", 28, 8, 31, 10, "barracks", 0),
                    building("inner_west_row", 11, 8, 13, 9, "row", 1),
                    building("west_guardhouse", 3, 8, 6, 10, "barracks", 1),
                    building("inner_east_row", 23, 8, 26, 9, "row", 0),
                    building("west_lower_row", 5, 15, 11, 17, "row", 0),
                    building("captains_hall", 12, 15, 15, 17, "hall", 2),
                    building("east_lower_row", 22, 15, 30, 17, "row", 1),
                    building("southwest_corner_row", 6, 19, 7, 20, "row", 2),
                    building("south_armory", 8, 19, 13, 20, "shop", 2),
                    building("south_mid_shop", 18, 19, 20, 20, "shop", 1),
                    building("south_quarters", 21, 19, 26, 20, "house", 0)
            );
            case "belltower" -> List.of(
                    building("west_chime_row", 3, 3, 8, 5, "row", 0),
                    building("bellwright_shop", 10, 3, 14, 5, "shop", 2),
                    building("north_gate_homes", 18, 4, 20, 5, "house", 2),
                    building("north_chapel_row", 21, 3, 27, 5, "hall", 1),
                    building("east_chime_house", 28, 4, 31, 5, "house", 0),
                    building("inner_west_row", 7, 8, 10, 9, "row", 1),
                    building("west_market_house", 3, 8, 6, 10, "shop", 1),
                    building("inner_east_row", 23, 8, 26, 9, "row", 0),
                    building("east_market_house", 27, 8, 30, 10, "shop", 2),
                    building("west_lower_chimes", 4, 14, 9, 17, "row", 1),
                    building("bellkeepers_lodge", 11, 15, 15, 17, "inn", 0),
                    building("east_lower_chimes", 22, 14, 29, 17, "row", 2),
                    building("southwest_corner_row", 2, 19, 5, 20, "row", 2),
                    building("south_bellfoundry", 8, 19, 13, 20, "guild", 1),
                    building("south_mid_shop", 18, 19, 20, 20, "shop", 1),
                    building("south_homes", 21, 19, 26, 20, "house", 0),
                    building("southeast_chime_house", 28, 19, 30, 20, "house", 2)
            );
            case "sanctum" -> List.of(
                    building("west_cloister", 3, 3, 8, 5, "hall", 2),
                    building("north_cells", 10, 3, 14, 5, "house", 1),
                    building("north_gate_homes", 18, 4, 20, 5, "house", 2),
                    building("east_cloister", 21, 3, 27, 5, "hall", 0),
                    building("east_reliquary", 28, 4, 31, 5, "guild", 2),
                    building("inner_west_row", 7, 8, 10, 9, "row", 1),
                    building("west_reliquary", 3, 8, 6, 10, "guild", 1),
                    building("inner_east_row", 23, 8, 26, 9, "row", 0),
                    building("east_scribe_house", 27, 8, 30, 10, "shop", 0),
                    building("west_lower_cells", 4, 14, 9, 17, "row", 0),
                    building("warden_hall", 11, 15, 15, 17, "hall", 1),
                    building("east_lower_cells", 22, 14, 29, 17, "row", 2),
                    building("southwest_corner_row", 2, 19, 5, 20, "row", 2),
                    building("south_apothecary", 8, 19, 13, 20, "shop", 1),
                    building("south_mid_shop", 18, 19, 20, 20, "shop", 1),
                    building("south_sanctum_homes", 21, 19, 26, 20, "house", 0),
                    building("southeast_cells", 28, 19, 30, 20, "house", 2)
            );
            default -> List.of(
                    building("river_warehouse", 3, 4, 7, 5, "warehouse", 0),
                    building("apothecary_row", 9, 4, 13, 5, "shop", 1),
                    building("north_gate_homes", 18, 4, 20, 5, "house", 2),
                    building("bridge_inn", 21, 4, 26, 5, "inn", 2),
                    building("east_storehouse", 28, 4, 31, 5, "warehouse", 0),
                    building("inner_west_row", 7, 8, 10, 9, "row", 1),
                    building("west_market_house", 3, 8, 6, 10, "shop", 2),
                    building("inner_east_row", 23, 8, 26, 9, "row", 0),
                    building("east_market_house", 27, 8, 30, 10, "shop", 1),
                    building("west_homes", 4, 14, 9, 17, "row", 0),
                    building("river_hall", 11, 15, 15, 17, "hall", 1),
                    building("east_homes", 22, 14, 29, 17, "row", 2),
                    building("southwest_corner_row", 2, 19, 5, 20, "row", 2),
                    building("south_tavern", 8, 19, 13, 20, "inn", 1),
                    building("south_mid_shop", 18, 19, 20, 20, "shop", 1),
                    building("south_stables", 21, 19, 26, 20, "shop", 0)
            );
        };
    }

    private CityBuilding building(String key, int x1, int y1, int x2, int y2, String style, int palette) {
        return new CityBuilding(key, x1, y1, x2, y2, style, palette);
    }

    private List<CityBuilding> villageBuildingTemplates() {
        return List.of(
                building("west_cottage", 4, 4, 7, 6, "house", 0),
                building("north_cottage", 9, 3, 12, 5, "shop", 1),
                building("east_cottage", 16, 4, 20, 6, "house", 2),
                building("southwest_cottage", 5, 12, 9, 14, "row", 0),
                building("south_cottage", 12, 13, 15, 15, "house", 1),
                building("southeast_cottage", 18, 11, 22, 14, "inn", 2)
        );
    }

    private char[][] villageTiles(String variant) {
        char base = switch (variant) {
            case "snow" -> 'n';
            case "desert" -> 's';
            case "marsh" -> 'v';
            default -> 'f';
        };
        char[][] grid = filled(36, 28, base);
        if ("green".equals(variant)) {
            organicFill(grid, 14, 10, 11, 7, 'g', 200 + variant.length());
            organicFill(grid, 5, 5, 4, 3, 'f', 271);
            organicFill(grid, 22, 14, 4, 3, 'f', 277);
        } else if ("snow".equals(variant)) {
            organicFill(grid, 14, 10, 10, 7, 'n', 241);
            organicFill(grid, 4, 4, 4, 3, 'q', 251);
            organicFill(grid, 23, 15, 4, 3, 'q', 257);
        } else if ("desert".equals(variant)) {
            organicFill(grid, 14, 10, 11, 7, 's', 263);
            organicFill(grid, 3, 4, 5, 3, 'b', 269);
            organicFill(grid, 24, 15, 5, 3, 'b', 281);
        } else if ("marsh".equals(variant)) {
            organicFill(grid, 14, 10, 11, 7, 'f', 293);
            organicFill(grid, 5, 7, 5, 4, 'v', 307);
            organicFill(grid, 22, 12, 5, 4, 'v', 311);
        }
        rect(grid, 13, 0, 14, 27, 'r');
        rect(grid, 0, 9, 35, 10, 'r');
        rect(grid, 2, 16, 33, 16, 'r');
        int[][] cottages = {{4, 4, 7, 6}, {9, 3, 12, 5}, {16, 4, 20, 6}, {5, 12, 9, 14}, {12, 13, 15, 15}, {18, 11, 22, 14}};
        for (int[] cottage : cottages) {
            rect(grid, cottage[0], cottage[1], cottage[2], cottage[3], 'h');
        }
        rect(grid, 13, 8, 15, 10, 'p');
        if ("snow".equals(variant)) {
            rect(grid, 2, 2, 5, 2, 'n');
            rect(grid, 22, 17, 25, 17, 'n');
        } else if ("desert".equals(variant)) {
            rect(grid, 2, 13, 4, 14, 'b');
            rect(grid, 23, 4, 25, 5, 'b');
        }
        if ("marsh".equals(variant)) {
            rect(grid, 2, 6, 5, 12, 'v');
            rect(grid, 22, 8, 25, 14, 'v');
            rect(grid, 3, 9, 4, 10, 'w');
            rect(grid, 23, 11, 24, 12, 'w');
        }
        return grid;
    }

    private char[][] dungeonTiles(int depth) {
        char[][] grid = filled(30, 22, 'x');
        rect(grid, 2, 2, 27, 19, 'r');
        rect(grid, 5, 5, 11, 9, 'd');
        rect(grid, 17, 4, 24, 8, 'd');
        rect(grid, 6, 13, 12, 18, 'd');
        rect(grid, 18, 13, 26, 18, 'd');
        rect(grid, 1, 10, 28, 11, 'r');
        rect(grid, 14, 1, 15, 20, 'r');
        if (depth > 1) {
            rect(grid, 13, 9, 16, 12, 'w');
            grid[10][14] = 'r';
            grid[10][15] = 'r';
        }
        return grid;
    }

    private char[][] houseTiles(int seed) {
        char[][] grid = filled(24, 17, 'i');
        border(grid, 'o');
        grid[15][11] = 'e';
        grid[15][12] = 'e';
        switch (Math.abs(seed) % 4) {
            case 0 -> {
                rect(grid, 3, 3, 5, 4, 'k');
                rect(grid, 17, 3, 20, 4, 'k');
                rect(grid, 8, 7, 15, 9, 'z');
                rect(grid, 18, 11, 20, 13, 'k');
                rect(grid, 4, 11, 6, 13, 'k');
            }
            case 1 -> {
                rect(grid, 3, 2, 8, 2, 'k');
                rect(grid, 17, 2, 20, 5, 'k');
                rect(grid, 7, 7, 12, 9, 'z');
                rect(grid, 4, 12, 7, 13, 'k');
                rect(grid, 15, 10, 17, 11, 'k');
            }
            case 2 -> {
                rect(grid, 3, 3, 4, 7, 'k');
                rect(grid, 15, 3, 20, 5, 'k');
                rect(grid, 8, 10, 15, 11, 'z');
                rect(grid, 17, 8, 19, 9, 'k');
                rect(grid, 5, 12, 7, 13, 'k');
            }
            default -> {
                rect(grid, 3, 2, 6, 4, 'k');
                rect(grid, 15, 2, 20, 2, 'k');
                rect(grid, 5, 8, 11, 10, 'z');
                rect(grid, 17, 9, 20, 13, 'k');
                rect(grid, 4, 13, 6, 13, 'k');
            }
        }
        return grid;
    }

    private void addBiomeProps(MapArea area, int salt) {
        boolean[][] occupiedProps = new boolean[area.height()][area.width()];
        boolean[][] tallProps = new boolean[area.height()][area.width()];
        for (int y = 2; y < area.height() - 2; y++) {
            for (int x = 2; x < area.width() - 2; x++) {
                char tile = area.tileAt(x, y);
                if (tile == 'w' || tile == 'c' || tile == 'u' || tile == 'h' || tile == 'x') {
                    continue;
                }
                if (occupiedProps[y][x]) {
                    continue;
                }
                int roll = Math.abs(hash(x, y, salt + area.id.hashCode())) % 1000;
                int cluster = naturalNeighborCount(area, x, y, tile);
                int chance = decorationChance(tile, cluster);
                if (roll < chance) {
                    int patchSeed = hash(x / 5, y / 5, salt + tile * 97 + area.id.hashCode());
                    String asset = decorationFor(tile, roll, patchSeed);
                    if (isTallProp(asset) && shouldThinTallProp(tile, asset, tallProps, x, y, roll)) {
                        asset = lowDecorationFor(tile, roll + 31, patchSeed);
                    }
                    int size = decorationSize(tile, roll, asset);
                    area.props.add(new WorldProp(x, y, asset, size));
                    occupiedProps[y][x] = true;
                    if (isTallProp(asset)) {
                        tallProps[y][x] = true;
                    }
                    if (shouldPairDecoration(tile, roll, chance, asset)) {
                        int ox = roll % 2 == 0 ? 1 : -1;
                        int oy = (roll / 2) % 2 == 0 ? 1 : -1;
                        if (area.tileAt(x + ox, y + oy) == tile && !occupiedProps[y + oy][x + ox]) {
                            String pairedAsset = lowDecorationFor(tile, roll + 17, patchSeed);
                            area.props.add(new WorldProp(x + ox, y + oy, pairedAsset, decorationSize(tile, roll + 17, pairedAsset)));
                            occupiedProps[y + oy][x + ox] = true;
                        }
                    }
                }
            }
        }
    }

    private int decorationChance(char tile, int cluster) {
        int clusteredBonus = switch (tile) {
            case 'f' -> Math.min(108, cluster * 5);
            case 'm', 'q' -> Math.min(38, cluster * 2);
            case 'v' -> Math.min(52, cluster * 3);
            case 's', 'n', 'b' -> Math.min(34, cluster * 2);
            case 'g' -> Math.min(46, cluster * 3);
            default -> Math.min(12, cluster * 2);
        };
        int base = switch (tile) {
            case 'f' -> 198;
            case 'm' -> 66;
            case 'q' -> 52;
            case 'v' -> 74;
            case 's', 'n', 'b' -> 42;
            case 'g' -> 58;
            case 'r' -> 8;
            default -> 3;
        };
        return base + clusteredBonus;
    }

    private int decorationSize(char tile, int roll, String asset) {
        int jitter = Math.floorMod(roll / 11 + asset.hashCode(), 5);
        if (isForestTree(asset)) {
            int base = switch (asset) {
                case "deco_tree_young" -> 58;
                case "deco_tree_blue_pine" -> 74;
                case "deco_tree_pine" -> 78;
                default -> 82;
            };
            return base + jitter * 6;
        }
        return switch (asset) {
            case "deco_flowers", "deco_grass_clump", "deco_grass_wildflowers", "deco_grass_herb_patch",
                    "deco_imagen_meadow_blooms", "deco_forest_fern", "deco_forest_mushrooms",
                    "deco_forest_blue_mushroom_ring", "deco_bog_grass", "deco_reeds", "deco_mushrooms",
                    "deco_dry_grass", "deco_badlands_dry_grass" -> 28 + jitter * 2;
            case "deco_bush", "deco_tundra_frost_bush" -> 32 + jitter * 2;
            case "deco_forest_log", "deco_forest_moss_rock", "deco_rocks", "deco_desert_rocks",
                    "deco_tundra_rocks", "deco_badlands_rocks", "deco_mountain_rocks",
                    "deco_mountain_cairn", "deco_mountain_pass_way_cairn" -> 36 + jitter * 4;
            case "deco_forest_ancient_roots", "deco_forest_shrine_stone", "deco_forest_fairy_pool",
                    "deco_imagen_grass_pond", "deco_marsh_lily_pool", "deco_marsh_bubble_pool",
                    "deco_mountain_spring_pool", "deco_mountain_pass_snowmelt_pool" -> 58 + jitter * 6;
            case "deco_grass_stone_stack",
                    "deco_desert_sun_bleached_bones", "deco_desert_jar_cache", "deco_tundra_rune_stone",
                    "deco_badlands_skull_marker", "deco_badlands_totem_stones",
                    "deco_mountain_crystal_cluster" -> 42 + jitter * 4;
            case "deco_cactus", "deco_desert_blooming_cactus" -> 46 + jitter * 4;
            case "deco_snow_pine", "deco_mountain_scrub_pine", "deco_badlands_red_spire" -> 54 + jitter * 5;
            case "deco_marsh_firefly_reeds", "deco_marsh_twisted_roots" -> 40 + jitter * 4;
            case "deco_road_signpost", "deco_road_milestone", "deco_imagen_signpost",
                    "deco_imagen_milestone" -> 34 + jitter * 2;
            case "deco_imagen_road_camp" -> 44 + jitter * 3;
            default -> switch (tile) {
                case 'm', 'q' -> 40 + roll % 14;
                case 'r' -> 34;
                case 'v' -> 38 + roll % 12;
                default -> 38 + roll % 10;
            };
        };
    }

    private boolean shouldPairDecoration(char tile, int roll, int chance, String asset) {
        return switch (tile) {
            case 'f' -> isForestFlora(asset) && roll < chance / 7;
            case 'g' -> isGrassFlora(asset) && roll < chance / 5;
            case 'v' -> isMarshFlora(asset) && roll < chance / 4;
            case 's', 'n', 'b' -> isSmallNatural(asset) && roll < chance / 8;
            default -> false;
        };
    }

    private boolean shouldThinTallProp(char tile, String asset, boolean[][] tallProps, int x, int y, int roll) {
        if (tile == 'f' && isForestTree(asset)) {
            return nearbyTallPropCount(tallProps, x, y, 1) >= 3 || (nearbyTallPropCount(tallProps, x, y, 2) >= 7 && roll % 3 == 0);
        }
        return nearbyTallProp(tallProps, x, y);
    }

    private boolean nearbyTallProp(boolean[][] tallProps, int x, int y) {
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                if (ox == 0 && oy == 0) {
                    continue;
                }
                int nx = x + ox;
                int ny = y + oy;
                if (ny >= 0 && ny < tallProps.length && nx >= 0 && nx < tallProps[ny].length && tallProps[ny][nx]) {
                    return true;
                }
            }
        }
        return false;
    }

    private int nearbyTallPropCount(boolean[][] tallProps, int x, int y, int radius) {
        int count = 0;
        for (int oy = -radius; oy <= radius; oy++) {
            for (int ox = -radius; ox <= radius; ox++) {
                if (ox == 0 && oy == 0) {
                    continue;
                }
                int nx = x + ox;
                int ny = y + oy;
                if (ny >= 0 && ny < tallProps.length && nx >= 0 && nx < tallProps[ny].length && tallProps[ny][nx]) {
                    count++;
                }
            }
        }
        return count;
    }

    private int naturalNeighborCount(MapArea area, int x, int y, char tile) {
        int count = 0;
        for (int oy = -2; oy <= 2; oy++) {
            for (int ox = -2; ox <= 2; ox++) {
                if (ox == 0 && oy == 0) {
                    continue;
                }
                if (area.tileAt(x + ox, y + oy) == tile) {
                    count++;
                }
            }
        }
        return count;
    }

    private void addCityProps(MapArea area) {
        for (int y = 2; y < area.height() - 2; y++) {
            for (int x = 2; x < area.width() - 2; x++) {
                char tile = area.tileAt(x, y);
                int roll = Math.abs(hash(x, y, area.id.hashCode())) % 100;
                if (tile == 'p' && roll < 10) {
                    String[] options = {
                            "city_prop_fountain_small", "city_prop_crate", "city_prop_barrel",
                            "city_prop_cart", "city_prop_planter_stone"
                    };
                    area.props.add(new WorldProp(x, y, pick(options, roll + area.id.hashCode()), roll < 2 ? 48 : 38 + roll % 12));
                } else if (tile == 'a' && roll < 30) {
                    String[] options = {
                            "city_prop_market_red", "city_prop_market_yellow", "city_prop_market_green",
                            "city_prop_cart", "city_prop_barrel_stack", "city_prop_banner_red"
                    };
                    area.props.add(new WorldProp(x, y, pick(options, roll + area.id.hashCode()), 42 + roll % 12));
                } else if (tile == 'y' && roll < 24) {
                    String[] options = {
                            "city_prop_flower_pot", "city_prop_plant_box", "city_prop_planter_stone",
                            "city_prop_street_lamp"
                    };
                    area.props.add(new WorldProp(x, y, pick(options, roll + area.id.hashCode()), 34 + roll % 10));
                } else if ((tile == 'j' || tile == 'l') && roll < 9) {
                    String[] options = {
                            "city_prop_crate", "city_prop_barrel", "city_prop_source_crate_low",
                            "city_prop_source_barrel_open", "city_prop_street_lamp"
                    };
                    area.props.add(new WorldProp(x, y, pick(options, roll + area.id.hashCode()), 30 + roll % 10));
                } else if (tile == 'r' && roll < 5) {
                    area.props.add(new WorldProp(x, y, roll < 2 ? "city_prop_street_lamp" : "city_prop_banner_blue", 34 + roll % 8));
                }
            }
        }
    }

    private void addVillageProps(MapArea area, String variant) {
        addBiomeProps(area, variant.hashCode());
        area.props.add(new WorldProp(14, 9, "city_prop_fountain_small", 42));
        area.props.add(new WorldProp(12, 15, "deco_road_signpost", 36));
        if ("snow".equals(variant)) {
            area.props.add(new WorldProp(4, 3, "city_prop_source_snow_bush", 34));
            area.props.add(new WorldProp(23, 16, "city_prop_source_snow_stump", 32));
            area.props.add(new WorldProp(8, 15, "deco_snow_mound", 30));
        } else if ("desert".equals(variant)) {
            area.props.add(new WorldProp(4, 13, "deco_cactus", 42));
            area.props.add(new WorldProp(23, 5, "deco_desert_jar_cache", 36));
            area.props.add(new WorldProp(21, 15, "deco_dry_grass", 30));
        } else if ("marsh".equals(variant)) {
            area.props.add(new WorldProp(3, 8, "city_prop_source_reeds", 34));
            area.props.add(new WorldProp(24, 13, "deco_marsh_lily_pool", 38));
            area.props.add(new WorldProp(20, 6, "deco_bog_grass", 30));
        } else {
            area.props.add(new WorldProp(5, 15, "deco_flowers", 30));
            area.props.add(new WorldProp(21, 5, "deco_bush", 32));
        }
    }

    private void addDungeonProps(MapArea area) {
        for (int y = 3; y < area.height() - 3; y++) {
            for (int x = 3; x < area.width() - 3; x++) {
                int roll = Math.abs(hash(x, y, area.id.hashCode())) % 100;
                if (area.tileAt(x, y) == 'd' && roll < 5) {
                    area.props.add(new WorldProp(x, y, roll < 3 ? "location_graveyard_skull_marker" : "location_camp_crates", 38));
                }
            }
        }
    }

    private void addHouseProps(MapArea area) {
        for (int y = 1; y < area.height() - 1; y++) {
            for (int x = 1; x < area.width() - 1; x++) {
                int roll = Math.abs(hash(x, y, area.id.hashCode())) % 100;
                if (area.tileAt(x, y) == 'i' && roll < 4) {
                    area.props.add(new WorldProp(x, y, roll < 2 ? "city_prop_flower_pot" : "city_prop_barrel", 30));
                }
            }
        }
    }

    private void addLocationProps(MapArea area) {
        for (LocationPatch patch : locationPatches) {
            for (int y = Math.max(2, patch.cy - patch.ry - 1); y <= Math.min(area.height() - 3, patch.cy + patch.ry + 1); y++) {
                for (int x = Math.max(2, patch.cx - patch.rx - 1); x <= Math.min(area.width() - 3, patch.cx + patch.rx + 1); x++) {
                    if (!patch.contains(x, y)) {
                        continue;
                    }
                    char tile = area.tileAt(x, y);
                    if (tile == 'w' || tile == 'c' || tile == 'u' || tile == 'h' || tile == 'x') {
                        continue;
                    }
                    int seed = hash(x, y, patch.salt);
                    String ground = locationGroundFor(patch, tile, seed);
                    if (ground != null) {
                        area.props.add(new WorldProp(x, y, ground, 52));
                    }
                    if (patch.kind.equals("graveyard") && tile == 'd') {
                        area.props.add(new WorldProp(x, y, "location_crypt_entrance", 68));
                        continue;
                    }
                    double centerPull = patch.centerPull(x, y);
                    int chance = switch (patch.kind) {
                        case "farmland" -> 24;
                        case "goblin_camp" -> 34;
                        case "graveyard" -> 32;
                        default -> 26;
                    };
                    chance = Math.min(58, chance + (int) Math.round(centerPull * 18.0));
                    if (tile == 'r' || tile == 'q') {
                        chance = Math.max(10, chance - 16);
                    }
                    if (seed % 100 >= chance) {
                        continue;
                    }
                    String asset = locationDecorationFor(patch, x, y, seed);
                    area.props.add(new WorldProp(x, y, asset, locationPropSize(asset, seed)));
                }
            }
        }
    }

    private String locationGroundFor(LocationPatch patch, char tile, int seed) {
        return switch (patch.kind) {
            case "farmland" -> ((patch.salt + patch.cy + tile) / 2 + seed) % 3 == 0
                    ? "location_farmland_wheat"
                    : "location_farmland_tilled";
            case "goblin_camp" -> seed % 4 == 0 ? "location_graveyard_path" : "location_graveyard_dirt";
            case "graveyard" -> tile == 'd' || seed % 100 < 30 ? "location_graveyard_path" : "location_graveyard_dirt";
            default -> null;
        };
    }

    private String locationDecorationFor(LocationPatch patch, int x, int y, int seed) {
        int dx = x - patch.cx;
        int dy = y - patch.cy;
        boolean edge = Math.abs(dx) >= Math.max(2, patch.rx - 1) || Math.abs(dy) >= Math.max(2, patch.ry - 1);
        if (patch.kind.equals("farmland")) {
            if (edge) {
                return "location_farmland_fence";
            }
            String[] options = {
                    "location_farmland_scarecrow", "location_farmland_hay_bales", "location_farmland_wheat",
                    "location_farmland_wheat", "location_farmland_tilled"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("goblin_camp")) {
            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                return seed % 2 == 0 ? "location_camp_fire" : "location_camp_tent";
            }
            if (edge) {
                return "location_camp_palisade";
            }
            String[] options = {
                    "location_camp_tent", "location_camp_crates", "location_camp_fire",
                    "location_graveyard_skull_marker", "deco_imagen_road_camp"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("graveyard")) {
            if (nearAnyTile(x, y, new char[]{'d', 'r'}, 1)) {
                return "location_graveyard_tombstones";
            }
            if (edge) {
                return "location_graveyard_iron_fence";
            }
            String[] options = {
                    "location_graveyard_tombstones", "location_graveyard_dead_stump",
                    "location_graveyard_skull_marker", "location_crypt_entrance"
            };
            return pick(options, seed);
        }
        return "deco_bush";
    }

    private int locationPropSize(String asset, int seed) {
        int base = 42 + seed % 8;
        return switch (asset) {
            case "location_camp_fire", "location_camp_crates", "location_graveyard_skull_marker" -> 38;
            case "location_farmland_fence", "location_graveyard_iron_fence" -> 46;
            case "location_camp_tent", "location_camp_palisade", "location_farmland_scarecrow",
                    "location_graveyard_tombstones" -> 54;
            case "location_crypt_entrance" -> 62;
            default -> base;
        };
    }

    private String decorationFor(char tile, int roll, int patchSeed) {
        return switch (tile) {
            case 'g' -> grassDecorationFor(roll, patchSeed);
            case 'f' -> forestDecorationFor(roll, patchSeed);
            case 's' -> desertDecorationFor(roll, patchSeed);
            case 'n' -> tundraDecorationFor(roll, patchSeed);
            case 'v' -> marshDecorationFor(roll, patchSeed);
            case 'b' -> badlandsDecorationFor(roll, patchSeed);
            case 'q', 'm' -> mountainDecorationFor(roll, patchSeed);
            case 'r' -> pick(new String[]{
                    "deco_road_signpost", "deco_road_milestone", "deco_imagen_signpost",
                    "deco_imagen_milestone", "deco_imagen_road_camp"
            }, clusteredRoll(roll, patchSeed));
            default -> pick(new String[]{"deco_bush", "deco_flowers"}, roll);
        };
    }

    private String grassDecorationFor(int roll, int patchSeed) {
        int group = balancedGroup(roll, patchSeed);
        if (group < 66) {
            return pick(new String[]{"deco_flowers", "deco_grass_clump", "deco_grass_wildflowers",
                    "deco_grass_herb_patch", "deco_imagen_meadow_blooms"}, roll + patchSeed);
        }
        if (group < 88) {
            return "deco_bush";
        }
        return pick(new String[]{"deco_grass_stone_stack", "deco_imagen_grass_pond"}, roll + patchSeed);
    }

    private String forestDecorationFor(int roll, int patchSeed) {
        int group = balancedGroup(roll, patchSeed);
        if (group < 78) {
            return pick(new String[]{"deco_tree_pine", "deco_tree_blue_pine", "deco_tree_oak",
                    "deco_tree_round", "deco_tree_young", "deco_tree_oak", "deco_tree_round"}, roll + patchSeed);
        }
        if (group < 96) {
            return pick(new String[]{"deco_forest_fern", "deco_forest_mushrooms",
                    "deco_forest_blue_mushroom_ring", "deco_forest_moss_rock", "deco_forest_log",
                    "deco_forest_ancient_roots"}, roll + patchSeed);
        }
        return pick(new String[]{"deco_forest_shrine_stone", "deco_forest_fairy_pool"}, roll + patchSeed);
    }

    private String desertDecorationFor(int roll, int patchSeed) {
        int group = balancedGroup(roll, patchSeed);
        if (group < 58) {
            return pick(new String[]{"deco_cactus", "deco_dry_grass", "deco_desert_blooming_cactus"}, roll + patchSeed);
        }
        if (group < 82) {
            return "deco_desert_rocks";
        }
        return pick(new String[]{"deco_desert_sun_bleached_bones", "deco_desert_jar_cache"}, roll + patchSeed);
    }

    private String tundraDecorationFor(int roll, int patchSeed) {
        int group = balancedGroup(roll, patchSeed);
        if (group < 48) {
            return pick(new String[]{"deco_snow_pine", "deco_tundra_frost_bush"}, roll + patchSeed);
        }
        if (group < 78) {
            return pick(new String[]{"deco_snow_mound", "deco_tundra_rocks", "deco_tundra_ice_crystals"}, roll + patchSeed);
        }
        return "deco_tundra_rune_stone";
    }

    private String marshDecorationFor(int roll, int patchSeed) {
        int group = balancedGroup(roll, patchSeed);
        if (group < 62) {
            return pick(new String[]{"deco_reeds", "deco_bog_grass", "deco_mushrooms"}, roll + patchSeed);
        }
        if (group < 88) {
            return pick(new String[]{"deco_marsh_lily_pool", "deco_marsh_bubble_pool",
                    "deco_marsh_firefly_reeds"}, roll + patchSeed);
        }
        return "deco_marsh_twisted_roots";
    }

    private String badlandsDecorationFor(int roll, int patchSeed) {
        int group = balancedGroup(roll, patchSeed);
        if (group < 52) {
            return pick(new String[]{"deco_badlands_rocks", "deco_badlands_dry_grass"}, roll + patchSeed);
        }
        if (group < 78) {
            return "deco_badlands_red_spire";
        }
        return pick(new String[]{"deco_badlands_skull_marker", "deco_badlands_totem_stones"}, roll + patchSeed);
    }

    private String mountainDecorationFor(int roll, int patchSeed) {
        int group = balancedGroup(roll, patchSeed);
        if (group < 58) {
            return pick(new String[]{"deco_mountain_rocks", "deco_mountain_cairn",
                    "deco_mountain_pass_way_cairn", "deco_rocks"}, roll + patchSeed);
        }
        if (group < 78) {
            return "deco_mountain_scrub_pine";
        }
        return pick(new String[]{"deco_mountain_crystal_cluster", "deco_mountain_spring_pool",
                "deco_mountain_pass_snowmelt_pool"}, roll + patchSeed);
    }

    private int balancedGroup(int roll, int patchSeed) {
        int local = Math.floorMod(roll * 31 + patchSeed / 7, 100);
        int patch = Math.floorMod(patchSeed, 100);
        return roll % 100 < 36 ? (local * 2 + patch) / 3 : local;
    }

    private String lowDecorationFor(char tile, int roll, int patchSeed) {
        return switch (tile) {
            case 'f' -> pick(new String[]{"deco_forest_mushrooms", "deco_forest_blue_mushroom_ring",
                    "deco_forest_moss_rock", "deco_forest_log", "deco_forest_ancient_roots"}, roll + patchSeed);
            case 'g' -> pick(new String[]{"deco_flowers", "deco_grass_clump", "deco_grass_wildflowers",
                    "deco_grass_herb_patch", "deco_imagen_meadow_blooms"}, roll + patchSeed);
            case 'v' -> pick(new String[]{"deco_reeds", "deco_bog_grass", "deco_mushrooms",
                    "deco_marsh_lily_pool", "deco_marsh_bubble_pool"}, roll + patchSeed);
            case 's' -> pick(new String[]{"deco_dry_grass", "deco_desert_rocks", "deco_desert_sun_bleached_bones"}, roll + patchSeed);
            case 'n' -> pick(new String[]{"deco_snow_mound", "deco_tundra_rocks", "deco_tundra_frost_bush"}, roll + patchSeed);
            case 'b' -> pick(new String[]{"deco_badlands_rocks", "deco_badlands_dry_grass"}, roll + patchSeed);
            case 'm', 'q' -> pick(new String[]{"deco_mountain_rocks", "deco_mountain_cairn", "deco_rocks"}, roll + patchSeed);
            default -> "deco_grass_clump";
        };
    }

    private boolean isForestTree(String asset) {
        return asset.equals("deco_tree_pine")
                || asset.equals("deco_tree_blue_pine")
                || asset.equals("deco_tree_oak")
                || asset.equals("deco_tree_round")
                || asset.equals("deco_tree_young");
    }

    private boolean isForestFlora(String asset) {
        return asset.equals("deco_forest_fern")
                || asset.equals("deco_forest_mushrooms")
                || asset.equals("deco_forest_blue_mushroom_ring");
    }

    private boolean isGrassFlora(String asset) {
        return asset.equals("deco_flowers")
                || asset.equals("deco_grass_clump")
                || asset.equals("deco_grass_wildflowers")
                || asset.equals("deco_grass_herb_patch")
                || asset.equals("deco_imagen_meadow_blooms");
    }

    private boolean isMarshFlora(String asset) {
        return asset.equals("deco_reeds")
                || asset.equals("deco_bog_grass")
                || asset.equals("deco_mushrooms");
    }

    private boolean isSmallNatural(String asset) {
        return asset.equals("deco_dry_grass")
                || asset.equals("deco_badlands_dry_grass")
                || asset.equals("deco_tundra_frost_bush");
    }

    private boolean isTallProp(String asset) {
        return isForestTree(asset)
                || asset.equals("deco_snow_pine")
                || asset.equals("deco_mountain_scrub_pine")
                || asset.equals("deco_cactus")
                || asset.equals("deco_desert_blooming_cactus")
                || asset.equals("deco_badlands_red_spire")
                || asset.equals("deco_forest_fern");
    }

    private int clusteredRoll(int roll, int patchSeed) {
        int dominant = patchSeed % 5;
        return roll % 100 < 58 ? dominant : roll + patchSeed / 7;
    }

    private String pick(String[] options, int seed) {
        return options[Math.floorMod(seed, options.length)];
    }

    private char[][] filled(int cols, int rows, char tile) {
        char[][] grid = new char[rows][cols];
        for (char[] row : grid) {
            Arrays.fill(row, tile);
        }
        return grid;
    }

    private void border(char[][] grid, char tile) {
        int rows = grid.length;
        int cols = grid[0].length;
        for (int x = 0; x < cols; x++) {
            grid[0][x] = tile;
            grid[rows - 1][x] = tile;
        }
        for (int y = 0; y < rows; y++) {
            grid[y][0] = tile;
            grid[y][cols - 1] = tile;
        }
    }

    private void rect(char[][] grid, int x1, int y1, int x2, int y2, char tile) {
        for (int y = Math.max(0, y1); y <= Math.min(grid.length - 1, y2); y++) {
            for (int x = Math.max(0, x1); x <= Math.min(grid[0].length - 1, x2); x++) {
                grid[y][x] = tile;
            }
        }
    }

    private void rectIf(char[][] grid, int x1, int y1, int x2, int y2, char tile, char... allowed) {
        for (int y = Math.max(0, y1); y <= Math.min(grid.length - 1, y2); y++) {
            for (int x = Math.max(0, x1); x <= Math.min(grid[0].length - 1, x2); x++) {
                for (char candidate : allowed) {
                    if (grid[y][x] == candidate) {
                        grid[y][x] = tile;
                        break;
                    }
                }
            }
        }
    }

    private void organicFill(char[][] grid, int cx, int cy, int rx, int ry, char tile, int salt) {
        for (int y = Math.max(1, cy - ry - 2); y <= Math.min(grid.length - 2, cy + ry + 2); y++) {
            for (int x = Math.max(1, cx - rx - 2); x <= Math.min(grid[0].length - 2, cx + rx + 2); x++) {
                double nx = (x - cx) / Math.max(1.0, rx);
                double ny = (y - cy) / Math.max(1.0, ry);
                double ripple = Math.sin(Math.atan2(ny, nx) * 3.0 + salt * 0.41) * 0.11;
                if (Math.hypot(nx, ny) <= 1.0 + ripple) {
                    grid[y][x] = tile;
                }
            }
        }
    }

    public char rawOverworldTileAt(int x, int y) {
        if (x < 0 || y < 0 || x >= COLS || y >= ROWS) {
            return 'm';
        }
        return tiles[y][x];
    }

    private void build(long seed) {
        for (char[] row : tiles) {
            Arrays.fill(row, 'w');
        }
        boolean[][] land = new boolean[ROWS][COLS];
        int seedSalt = (int) (seed & 0xFFFF_FFFFL);

        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                if (islandScore(x, y, seedSalt) > 0.04) {
                    land[y][x] = true;
                    tiles[y][x] = 'g';
                }
            }
        }

        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                if (!land[y][x]) {
                    continue;
                }
                if (!landAt(land, x + 1, y) || !landAt(land, x - 1, y) || !landAt(land, x, y + 1) || !landAt(land, x, y - 1)) {
                    tiles[y][x] = 's';
                }
            }
        }

        paintBiome(shifted(82, 6, 101, seedSalt), shifted(63, 4, 102, seedSalt), biomeRadius(45), biomeRadius(33), 'n', 'm', seedSalt + 101, land);
        paintBiome(shifted(109, 7, 107, seedSalt), shifted(244, 5, 108, seedSalt), biomeRadius(58), biomeRadius(34), 's', 'b', seedSalt + 107, land);
        paintBiome(shifted(235, 6, 113, seedSalt), shifted(169, 5, 114, seedSalt), biomeRadius(53), biomeRadius(42), 'v', 'f', seedSalt + 113, land);
        paintBiome(shifted(203, 6, 127, seedSalt), shifted(99, 5, 128, seedSalt), biomeRadius(48), biomeRadius(38), 'f', 'g', seedSalt + 127, land);
        paintBiome(shifted(197, 4, 131, seedSalt), shifted(67, 3, 132, seedSalt), biomeRadius(35), biomeRadius(24), 'm', 'f', seedSalt + 131, land);
        paintBiome(shifted(74, 5, 137, seedSalt), shifted(217, 4, 138, seedSalt), biomeRadius(37), biomeRadius(30), 'm', 's', seedSalt + 137, land);
        paintBiome(shifted(169, 6, 139, seedSalt), shifted(161, 5, 140, seedSalt), biomeRadius(43), biomeRadius(38), 'm', 'f', seedSalt + 139, land);
        paintBiome(shifted(236, 4, 149, seedSalt), shifted(83, 3, 150, seedSalt), biomeRadius(28), biomeRadius(24), 'b', 'm', seedSalt + 149, land);
        paintBiome(shifted(186, 5, 151, seedSalt), shifted(242, 4, 152, seedSalt), biomeRadius(34), biomeRadius(24), 'b', 's', seedSalt + 151, land);
        paintBiome(shifted(121, 6, 157, seedSalt), shifted(142, 5, 158, seedSalt), biomeRadius(50), biomeRadius(36), 'f', 'g', seedSalt + 157, land);
        seedBiomeGranules(seedSalt, land);

        paintWaterBody(shifted(48, 3, 201, seedSalt), shifted(139, 6, 202, seedSalt), 18, 38, seedSalt + 201, land);
        paintWaterBody(shifted(137, 4, 203, seedSalt), shifted(91, 3, 204, seedSalt), 19, 17, seedSalt + 203, land);
        paintWaterBody(shifted(270, 3, 205, seedSalt), shifted(163, 5, 206, seedSalt), 26, 37, seedSalt + 205, land);
        paintWaterBody(shifted(228, 4, 207, seedSalt), shifted(261, 4, 208, seedSalt), 24, 30, seedSalt + 207, land);
        paintWaterBody(shifted(69, 4, 209, seedSalt), shifted(273, 3, 210, seedSalt), 27, 18, seedSalt + 209, land);
        paintWaterBody(shifted(178, 4, 211, seedSalt), shifted(34, 3, 212, seedSalt), 23, 15, seedSalt + 211, land);

        raiseLandOval(83, 62, 11, 8, 'n', land);
        raiseLandPath(List.of(new TilePoint(83, 62), new TilePoint(78, 83), new TilePoint(82, 105)), 2, 'n', land);

        roadPath(List.of(new TilePoint(82, 105), new TilePoint(94, 123), START_POSITION, new TilePoint(132, 153), new TilePoint(152, 145)));
        roadPath(List.of(new TilePoint(152, 145), new TilePoint(169, 128), new TilePoint(189, 100), new TilePoint(205, 78)));
        roadPath(List.of(new TilePoint(205, 78), new TilePoint(200, 67), new TilePoint(196, 62)));
        roadPath(List.of(new TilePoint(152, 145), new TilePoint(176, 153), new TilePoint(202, 171), new TilePoint(228, 185), new TilePoint(255, 177)));
        roadPath(List.of(new TilePoint(228, 185), new TilePoint(208, 206), new TilePoint(194, 235), new TilePoint(150, 230)));
        roadPath(List.of(START_POSITION, new TilePoint(99, 181), new TilePoint(72, 218), new TilePoint(102, 245)));
        roadPath(List.of(new TilePoint(82, 105), new TilePoint(78, 83), new TilePoint(83, 62)));
        roadPath(List.of(START_POSITION, new TilePoint(128, 181), new TilePoint(145, 207), new TilePoint(150, 230)));
        roadPath(List.of(new TilePoint(240, 153), new TilePoint(228, 185)));

        stampSettlement(82, 105, 'c', "Riverside Gate");
        stampSettlement(152, 145, 'c', "Archive Gate");
        stampSettlement(205, 78, 'c', "Highwall Gate");
        stampSettlement(228, 185, 'c', "Belltower Gate");
        stampSettlement(150, 230, 'c', "Sanctum Gate");

        stampSettlement(112, 158, 'u', "Oakhaven");
        stampSettlement(83, 62, 'u', "Snowrest");
        stampSettlement(102, 245, 'u', "Dunewick");
        stampSettlement(240, 153, 'u', "Mireford");

        stampDungeon(196, 62, "Stonegate");
        stampDungeon(255, 177, "Miredepth");
        stampDungeon(72, 218, "Frosthollow");
        stampDungeon(194, 235, "Blackvault");
        addLocationPatches(seedSalt);
    }

    private void addLocationPatches(int seedSalt) {
        int[][] farmAnchors = {
                {112, 158}, {82, 105}, {152, 145}, {102, 245}
        };
        for (int i = 0; i < farmAnchors.length; i++) {
            TilePoint center = chooseLocationCenter(
                    700 + i * 37 + seedSalt, farmAnchors[i][0], farmAnchors[i][1],
                    18, 16, new char[]{'g', 's'}, new char[]{'r', 'u', 'c'}, 6, 10
            );
            if (center != null) {
                addLocationPatch("farmland", center.x(), center.y(), 5 + hash(i, 713, 3) % 3, 4 + hash(i, 719, 5) % 2, 710 + i);
            }
        }

        int[][] campAnchors = {
                {96, 110}, {186, 115}, {221, 201}, {132, 222}, {210, 244}
        };
        for (int i = 0; i < campAnchors.length; i++) {
            TilePoint center = chooseLocationCenter(
                    820 + i * 43 + seedSalt, campAnchors[i][0], campAnchors[i][1],
                    28, 24, new char[]{'g', 'f', 's', 'b', 'q'}, new char[]{'r', 'q'}, 8, 14
            );
            if (center != null) {
                addLocationPatch("goblin_camp", center.x(), center.y(), 4 + hash(i, 823, 7) % 3, 4 + hash(i, 829, 11) % 3, 830 + i);
            }
        }

        int[][] dungeons = {
                {196, 62}, {255, 177}, {72, 218}, {194, 235}
        };
        for (int i = 0; i < dungeons.length; i++) {
            int x = dungeons[i][0];
            int y = dungeons[i][1];
            addLocationPatch("graveyard", x, y, 6 + hash(x, y, 907) % 2, 5 + hash(x, y, 911) % 2, 910 + i);
        }
    }

    private TilePoint chooseLocationCenter(int salt, int baseX, int baseY, int spreadX, int spreadY, char[] allowed,
                                           char[] nearTiles, int nearRadius, int minDistance) {
        TilePoint fallback = null;
        for (int attempt = 0; attempt < 360; attempt++) {
            int hx = hash(attempt, salt, 19);
            int hy = hash(attempt, salt, 23);
            int x = baseX + hx % (spreadX * 2 + 1) - spreadX;
            int y = baseY + hy % (spreadY * 2 + 1) - spreadY;
            if (x < 4 || y < 4 || x >= COLS - 4 || y >= ROWS - 4) {
                continue;
            }
            if (!contains(allowed, tiles[y][x]) || nearAnyTile(x, y, new char[]{'w', 'c', 'u', 'd'}, 3)) {
                continue;
            }
            if (!farFromLocations(x, y, minDistance)) {
                continue;
            }
            if (fallback == null) {
                fallback = new TilePoint(x, y);
            }
            if (nearTiles == null || nearAnyTile(x, y, nearTiles, nearRadius)) {
                return new TilePoint(x, y);
            }
        }
        return fallback;
    }

    private boolean contains(char[] tiles, char tile) {
        for (char candidate : tiles) {
            if (candidate == tile) {
                return true;
            }
        }
        return false;
    }

    private boolean nearAnyTile(int x, int y, char[] targets, int radius) {
        for (int oy = -radius; oy <= radius; oy++) {
            for (int ox = -radius; ox <= radius; ox++) {
                if (Math.abs(ox) + Math.abs(oy) > radius + 1) {
                    continue;
                }
                int nx = x + ox;
                int ny = y + oy;
                if (nx >= 0 && ny >= 0 && nx < COLS && ny < ROWS && contains(targets, tiles[ny][nx])) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean farFromLocations(int x, int y, int minDistance) {
        int minDistanceSquared = minDistance * minDistance;
        for (LocationPatch patch : locationPatches) {
            int dx = x - patch.cx;
            int dy = y - patch.cy;
            if (dx * dx + dy * dy < minDistanceSquared) {
                return false;
            }
        }
        return true;
    }

    private void addLocationPatch(String kind, int cx, int cy, int rx, int ry, int salt) {
        LocationPatch patch = new LocationPatch(kind, cx, cy, rx, ry, salt);
        locationPatches.add(patch);
        landmarks.put(new TilePoint(cx, cy), patch.label());
    }

    private LocationPatch locationAt(String mapId, int x, int y) {
        if (!OVERWORLD_ID.equals(mapId)) {
            return null;
        }
        for (LocationPatch patch : locationPatches) {
            if (patch.contains(x, y)) {
                return patch;
            }
        }
        return null;
    }

    private record LocationPatch(String kind, int cx, int cy, int rx, int ry, int salt) {
        private boolean contains(int x, int y) {
            double nx = (x - cx) / Math.max(1.0, rx);
            double ny = (y - cy) / Math.max(1.0, ry);
            double ripple = Math.sin(Math.atan2(ny, nx) * 3.0 + salt * 0.17) * 0.12;
            return Math.hypot(nx, ny) <= 1.0 + ripple;
        }

        private double centerPull(int x, int y) {
            double dx = Math.abs(x - cx) / Math.max(1.0, rx);
            double dy = Math.abs(y - cy) / Math.max(1.0, ry);
            return Math.max(0.0, 1.0 - (dx + dy) * 0.5);
        }

        private String label() {
            return switch (kind) {
                case "farmland" -> "Farmland";
                case "goblin_camp" -> "Raiders' Camp";
                case "graveyard" -> "Graveyard";
                default -> "Landmark";
            };
        }
    }

    public record LocationSite(String kind, String label, int x, int y) {
    }

    private double islandScore(int x, int y, int seedSalt) {
        double phaseA = (seedSalt % 997) * 0.001;
        double phaseB = ((seedSalt >> 10) % 997) * 0.001;
        double nx = (x - 151) / 131.0;
        double ny = (y - 152) / 124.0;
        double score = 1.0 - (nx * nx + ny * ny);
        score += 0.14 * Math.sin(x * 0.051 + phaseA * 7.0) + 0.11 * Math.cos(y * 0.067 - phaseB * 6.0);
        score += 0.08 * Math.sin((x + y) * 0.037 + phaseB * 9.0) - 0.07 * Math.cos((x - y) * 0.049 + phaseA * 5.0);
        score += 0.05 * Math.sin((x * 2 + y) * 0.028 + phaseA * 11.0);
        score += 0.06 * Math.sin((x * 0.031 + y * 0.017) + phaseB * 13.0);
        if (x < 18 || x > COLS - 16 || y < 14 || y > ROWS - 14) {
            score -= 0.45;
        }
        if (x < 48 && y < 70) {
            score -= 0.35;
        }
        if (x > 250 && y < 70) {
            score -= 0.22;
        }
        if (x > 258 && y > 238) {
            score -= 0.30;
        }
        return score;
    }

    private boolean landAt(boolean[][] land, int x, int y) {
        return x >= 0 && y >= 0 && x < COLS && y < ROWS && land[y][x];
    }

    private int shifted(int value, int span, int salt, int seedSalt) {
        return value + hash(value + seedSalt, salt, 909) % (span * 2 + 1) - span;
    }

    private int biomeRadius(int value) {
        return Math.max(4, (int) Math.round(value * 0.90));
    }

    private void paintBiome(int cx, int cy, int rx, int ry, char core, char edge, int salt, boolean[][] land) {
        int margin = Math.max(8, (int) Math.round(Math.max(rx, ry) * 0.32));
        for (int y = Math.max(0, cy - ry - margin); y <= Math.min(ROWS - 1, cy + ry + margin); y++) {
            for (int x = Math.max(0, cx - rx - margin); x <= Math.min(COLS - 1, cx + rx + margin); x++) {
                if (!land[y][x]) {
                    continue;
                }
                double dist = organicMetric(x, y, cx, cy, rx, ry, salt);
                if (dist <= 0.80) {
                    tiles[y][x] = core;
                } else if (dist <= 1.10) {
                    int chance = (int) ((1.10 - dist) * 720.0);
                    if ((hash(x, y, salt + 53) & 255) < chance) {
                        tiles[y][x] = edge;
                    }
                }
            }
        }
    }

    private void seedBiomeGranules(int seedSalt, boolean[][] land) {
        int[][] patchSpecs = {
                {'f', 118, 128, 118, 80, 32, 9, 4},
                {'f', 176, 111, 76, 62, 27, 7, 5},
                {'v', 212, 145, 75, 61, 23, 6, 6},
                {'s', 87, 226, 65, 58, 27, 7, 7},
                {'b', 178, 225, 55, 53, 17, 5, 8},
                {'n', 68, 67, 65, 49, 22, 6, 9},
                {'m', 143, 176, 78, 68, 22, 5, 10}
        };
        for (int[] spec : patchSpecs) {
            char tile = (char) spec[0];
            int baseX = spec[1];
            int baseY = spec[2];
            int spreadX = spec[3];
            int spreadY = spec[4];
            int count = spec[5];
            int maxRadius = spec[6];
            int salt = spec[7] + seedSalt;
            for (int i = 0; i < count; i++) {
                int cx = baseX + hash(i, salt, 3) % (spreadX * 2 + 1) - spreadX;
                int cy = baseY + hash(i, salt, 5) % (spreadY * 2 + 1) - spreadY;
                int rx = 2 + hash(i, salt, 7) % Math.max(1, maxRadius);
                int ry = 2 + hash(i, salt, 11) % Math.max(3, maxRadius - 1);
                paintBiomePatch(cx, cy, rx, ry, tile, salt * 101 + i, land);
            }
        }
    }

    private void paintBiomePatch(int cx, int cy, int rx, int ry, char tile, int salt, boolean[][] land) {
        for (int y = Math.max(0, cy - ry - 4); y <= Math.min(ROWS - 1, cy + ry + 4); y++) {
            for (int x = Math.max(0, cx - rx - 4); x <= Math.min(COLS - 1, cx + rx + 4); x++) {
                if (!land[y][x] || isProtectedOverworldTile(tiles[y][x])) {
                    continue;
                }
                if (organicMetric(x, y, cx, cy, rx, ry, salt) <= 0.92) {
                    tiles[y][x] = tile;
                }
            }
        }
    }

    private boolean isProtectedOverworldTile(char tile) {
        return tile == 'w' || tile == 'r' || tile == 'c' || tile == 'u' || tile == 'd';
    }

    private double organicMetric(int x, int y, int cx, int cy, int rx, int ry, int salt) {
        double warpX = (terrainNoise(x, y, 19, salt + 11) - 0.5) * rx * 0.34;
        double warpY = (terrainNoise(x, y, 17, salt + 23) - 0.5) * ry * 0.34;
        double nx = (x - cx + warpX) / Math.max(1.0, rx);
        double ny = (y - cy + warpY) / Math.max(1.0, ry);
        double angle = Math.atan2(ny, nx);
        double radial = Math.hypot(nx, ny);
        double scallop = Math.sin(angle * 3.0 + salt * 0.17) * 0.12 + Math.cos(angle * 5.0 - salt * 0.11) * 0.08;
        double grain = (terrainNoise(x, y, 11, salt + 37) - 0.5) * 0.28;
        double fine = (terrainNoise(x, y, 5, salt + 41) - 0.5) * 0.12;
        return radial - scallop - grain - fine;
    }

    private double terrainNoise(double x, double y, int scale, int salt) {
        int gx = (int) Math.floor(x / scale);
        int gy = (int) Math.floor(y / scale);
        double tx = x / scale - gx;
        double ty = y / scale - gy;
        tx = tx * tx * (3.0 - 2.0 * tx);
        ty = ty * ty * (3.0 - 2.0 * ty);

        double top = noiseCorner(gx, gy, salt) * (1.0 - tx) + noiseCorner(gx + 1, gy, salt) * tx;
        double bottom = noiseCorner(gx, gy + 1, salt) * (1.0 - tx) + noiseCorner(gx + 1, gy + 1, salt) * tx;
        return top * (1.0 - ty) + bottom * ty;
    }

    private double noiseCorner(int x, int y, int salt) {
        return (hash(x, y, salt) & 1023) / 1023.0;
    }

    private int hash(int x, int y, int salt) {
        long value = x * 374761393L + y * 668265263L + salt * 1442695040888963407L;
        value = (value ^ (value >> 13)) * 1274126177L;
        return (int) ((value ^ (value >> 16)) & 0x7fff_ffff);
    }

    private void paintWaterBody(int cx, int cy, int rx, int ry, int salt, boolean[][] land) {
        for (int y = Math.max(0, cy - ry - 6); y <= Math.min(ROWS - 1, cy + ry + 6); y++) {
            for (int x = Math.max(0, cx - rx - 6); x <= Math.min(COLS - 1, cx + rx + 6); x++) {
                if (organicMetric(x, y, cx, cy, rx, ry, salt) <= 0.92 && land[y][x]) {
                    tiles[y][x] = 'w';
                }
            }
        }
    }

    private void raiseLandOval(int cx, int cy, int rx, int ry, char tile, boolean[][] land) {
        for (int y = Math.max(0, cy - ry); y <= Math.min(ROWS - 1, cy + ry); y++) {
            for (int x = Math.max(0, cx - rx); x <= Math.min(COLS - 1, cx + rx); x++) {
                double nx = (x - cx) / Math.max(1.0, rx);
                double ny = (y - cy) / Math.max(1.0, ry);
                if (nx * nx + ny * ny <= 1.0) {
                    land[y][x] = true;
                    tiles[y][x] = tile;
                }
            }
        }
    }

    private void raiseLandPath(List<TilePoint> points, int width, char tile, boolean[][] land) {
        TilePoint current = points.get(0);
        int x = current.x();
        int y = current.y();
        markGround(x, y, width, tile, land);
        for (TilePoint target : points.subList(1, points.size())) {
            int guard = 0;
            while ((x != target.x() || y != target.y()) && guard++ < 300) {
                if (target.x() != x) {
                    x += target.x() > x ? 1 : -1;
                }
                if (target.y() != y && guard % 2 == 0) {
                    y += target.y() > y ? 1 : -1;
                }
                markGround(x, y, width, tile, land);
            }
        }
    }

    private void markGround(int x, int y, int width, char tile, boolean[][] land) {
        for (int oy = -width; oy <= width; oy++) {
            for (int ox = -width; ox <= width; ox++) {
                if (Math.abs(ox) + Math.abs(oy) > width + 1) {
                    continue;
                }
                int tx = x + ox;
                int ty = y + oy;
                if (tx >= 0 && ty >= 0 && tx < COLS && ty < ROWS) {
                    land[ty][tx] = true;
                    tiles[ty][tx] = tile;
                }
            }
        }
    }

    private void roadPath(List<TilePoint> points) {
        int x = points.get(0).x();
        int y = points.get(0).y();
        markRoad(x, y);
        for (TilePoint target : points.subList(1, points.size())) {
            int guard = 0;
            while ((x != target.x() || y != target.y()) && guard++ < 300) {
                int sx = Integer.compare(target.x(), x);
                int sy = Integer.compare(target.y(), y);
                if (sx != 0 && (sy == 0 || guard % 3 != 0)) {
                    x += sx;
                } else if (sy != 0) {
                    y += sy;
                }
                markRoad(x, y);
            }
        }
    }

    private void markRoad(int x, int y) {
        if (x < 0 || y < 0 || x >= COLS || y >= ROWS) {
            return;
        }
        if (tiles[y][x] == 'm') {
            tiles[y][x] = 'q';
        } else if (tiles[y][x] != 'w') {
            tiles[y][x] = 'r';
        }
    }

    private void stampSettlement(int cx, int cy, char tile, String name) {
        for (int oy = -2; oy <= 2; oy++) {
            for (int ox = -2; ox <= 2; ox++) {
                if (Math.abs(ox) + Math.abs(oy) <= 3) {
                    int x = cx + ox;
                    int y = cy + oy;
                    if (x >= 0 && y >= 0 && x < COLS && y < ROWS) {
                        tiles[y][x] = tile;
                    }
                }
            }
        }
        landmarks.put(new TilePoint(cx, cy), name);
    }

    private void stampDungeon(int cx, int cy, String name) {
        for (int oy = -2; oy <= 2; oy++) {
            for (int ox = -2; ox <= 2; ox++) {
                int x = cx + ox;
                int y = cy + oy;
                if (x >= 0 && y >= 0 && x < COLS && y < ROWS) {
                    tiles[y][x] = 'r';
                }
            }
        }
        tiles[cy][cx] = 'd';
        landmarks.put(new TilePoint(cx, cy), name + " Dungeon");
    }
}
