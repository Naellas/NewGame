package com.alderfall.game;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorldMap {
    public static final int COLS = 300;
    public static final int ROWS = 300;
    public static final TilePoint START_POSITION = new TilePoint(112, 158);
    public static final TilePoint OAKHAVEN_POSITION = new TilePoint(124, 151);
    public static final String OVERWORLD_ID = "overworld";
    public static final String PLAYER_VILLAGE_ID = "village_oathstead_camp";
    private static final List<Kingdom> KINGDOMS = List.of(
            new Kingdom("riverside", "Riverside Reach", "Riverside City", 74, 112,
                    "River barons, mill towns, and western bridge keeps.", 0x5BA7D1),
            new Kingdom("highwall", "Highwall March", "Highwall City", 211, 80,
                    "Frost roads, watchtowers, and ironbound northern passes.", 0xAFC7E8),
            new Kingdom("crownlands", "Crownlands of Alderfall", "Archive City", 145, 154,
                    "Old oath seats, oak villages, and the broken royal archive.", 0xD5BE72),
            new Kingdom("belltower", "Belltower Fenlands", "Belltower City", 242, 176,
                    "Marsh bells, reed causeways, and mist-guarded ports.", 0x5FA785),
            new Kingdom("sanctum", "Sanctum Sunrealm", "Sanctum City", 144, 244,
                    "Sunsteppe towns, desert shrines, and badlands caravans.", 0xD6905B)
    );

    private final char[][] tiles = new char[ROWS][COLS];
    private final Map<TilePoint, String> landmarks = new HashMap<>();
    private final Map<String, MapArea> maps = new LinkedHashMap<>();
    private final Map<String, WorldTransition> transitions = new HashMap<>();
    private final Map<String, List<CityBuilding>> cityBuildings = new HashMap<>();
    private final Map<String, List<Npc>> interiorNpcs = new HashMap<>();
    private final List<WorldProp> playerVillageProps = new ArrayList<>();
    private final Map<String, List<WorldProp>> playerInteriorProps = new HashMap<>();
    private final Map<String, Integer> playerVillageBuildingLevels = new HashMap<>();
    private final Map<TilePoint, Character> playerVillageTiles = new LinkedHashMap<>();
    private final List<LocationPatch> locationPatches = new ArrayList<>();
    private final List<AdventureSite> adventureSites = new ArrayList<>();
    private final List<SettlementSite> settlementSites = new ArrayList<>();
    private int playerVillageStage = 1;

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
        MapArea area = area(mapId);
        if (area == null) {
            return Terrain.passable('m');
        }
        if (cityBuildingBlocksMovementAt(mapId, x, y)) {
            return false;
        }
        char tile = area.tileAt(x, y);
        if (Terrain.passable(tile)) {
            return true;
        }
        return tile == 'k' && isOpenInteriorFurnitureTile(area, x, y);
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

    public WorldProp propAt(String mapId, int x, int y) {
        MapArea area = area(mapId);
        if (area == null) {
            return null;
        }
        for (int i = area.props.size() - 1; i >= 0; i--) {
            WorldProp prop = area.props.get(i);
            if (prop.x() == x && prop.y() == y) {
                return prop;
            }
        }
        return null;
    }

    public List<Npc> npcs(String mapId) {
        return interiorNpcs.getOrDefault(mapId, List.of());
    }

    public List<WorldProp> propsAt(String mapId, int x, int y) {
        MapArea area = area(mapId);
        if (area == null) {
            return List.of();
        }
        List<WorldProp> matches = new ArrayList<>();
        for (WorldProp prop : area.props) {
            if (prop.x() == x && prop.y() == y) {
                matches.add(prop);
            }
        }
        return matches;
    }

    public List<Kingdom> kingdoms() {
        return KINGDOMS;
    }

    public Kingdom kingdomAt(int x, int y) {
        Kingdom best = KINGDOMS.get(0);
        long bestScore = Long.MAX_VALUE;
        for (Kingdom kingdom : KINGDOMS) {
            long dx = x - kingdom.centerX();
            long dy = y - kingdom.centerY();
            long score = dx * dx + dy * dy;
            if (score < bestScore) {
                bestScore = score;
                best = kingdom;
            }
        }
        return best;
    }

    public List<SettlementSite> settlementSites() {
        return List.copyOf(settlementSites);
    }

    public boolean isPlayerSettlement(SettlementSite settlement) {
        return settlement != null && PLAYER_VILLAGE_ID.equals(settlement.id());
    }

    public VillageManager.SettlementStage playerVillageSettlementStage() {
        return VillageManager.settlementStage(playerVillageStage);
    }

    public List<CityBuilding> playerVillageBuildings() {
        return cityBuildings(PLAYER_VILLAGE_ID).stream()
                .filter(building -> building.key().startsWith("player_"))
                .toList();
    }

    public List<WorldProp> playerVillageProps() {
        return List.copyOf(playerVillageProps);
    }

    public Map<String, List<WorldProp>> playerInteriorProps() {
        Map<String, List<WorldProp>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<WorldProp>> entry : playerInteriorProps.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copy;
    }

    public Map<String, Integer> playerVillageBuildingLevels() {
        return Map.copyOf(playerVillageBuildingLevels);
    }

    public int playerVillageStage() {
        return playerVillageStage;
    }

    public int setPlayerVillageStage(int stage) {
        int target = Math.max(1, Math.min(stage, VillageManager.settlementStages().size()));
        if (target <= playerVillageStage) {
            return 0;
        }
        int totalShift = 0;
        while (playerVillageStage < target) {
            expandPlayerVillage(10);
            playerVillageStage++;
            totalShift += 10;
        }
        return totalShift;
    }

    public int playerVillagePropCount() {
        return playerVillageProps.size();
    }

    public int playerVillageDevelopedTileCount() {
        int count = playerVillageTiles.size();
        for (CityBuilding building : playerVillageBuildings()) {
            count += Math.max(1, building.width() * building.depth());
        }
        count += playerVillageProps.size();
        return count;
    }

    public Map<TilePoint, Character> playerVillageTiles() {
        return Map.copyOf(playerVillageTiles);
    }

    public int playerVillageBuildingLevel(CityBuilding building) {
        if (building == null || !building.key().startsWith("player_")) {
            return 1;
        }
        return Math.max(1, playerVillageBuildingLevels.getOrDefault(building.key(), 1));
    }

    public boolean setPlayerVillageBuildingLevel(String buildingKey, int level) {
        if (buildingKey == null || level < 1) {
            return false;
        }
        for (CityBuilding building : playerVillageBuildings()) {
            if (building.key().equals(buildingKey)) {
                playerVillageBuildingLevels.put(buildingKey, Math.min(level, VillageManager.buildingPlan(building.style()).maxLevel()));
                return true;
            }
        }
        return false;
    }

    public boolean upgradePlayerVillageBuilding(CityBuilding building) {
        if (building == null || !building.key().startsWith("player_")) {
            return false;
        }
        int level = playerVillageBuildingLevel(building);
        VillageManager.BuildingPlan plan = VillageManager.buildingPlan(building.style());
        if (level >= plan.maxLevel()) {
            return false;
        }
        playerVillageBuildingLevels.put(building.key(), level + 1);
        return true;
    }

    public Map<String, Integer> playerVillageBuildingRoleLevels() {
        Map<String, Integer> levels = new LinkedHashMap<>();
        for (CityBuilding building : playerVillageBuildings()) {
            levels.put(building.style() + ":" + building.key(), playerVillageBuildingLevel(building));
        }
        return levels;
    }

    public int playerVillageBuildingCount(String style) {
        int count = 0;
        for (CityBuilding building : playerVillageBuildings()) {
            if (building.style().equals(style)) {
                count++;
            }
        }
        return count;
    }

    public int playerVillageStorageCapacity() {
        int capacity = 25;
        for (CityBuilding building : playerVillageBuildings()) {
            capacity += VillageManager.storageCapacity(building.style(), playerVillageBuildingLevel(building));
        }
        return capacity;
    }

    public void clearPlayerVillageCustomizations() {
        clearPlayerVillageInteriors();
        playerVillageProps.clear();
        playerInteriorProps.clear();
        playerVillageTiles.clear();
        playerVillageBuildingLevels.clear();
        cityBuildings.put(PLAYER_VILLAGE_ID, new ArrayList<>());
        resetPlayerVillageToBase();
    }

    private void clearPlayerVillageInteriors() {
        String prefix = "house_" + PLAYER_VILLAGE_ID + "_";
        List<String> managedInteriors = maps.keySet().stream()
                .filter(mapId -> mapId.startsWith(prefix))
                .toList();
        for (String mapId : managedInteriors) {
            maps.remove(mapId);
            interiorNpcs.remove(mapId);
            playerInteriorProps.remove(mapId);
        }
        transitions.entrySet().removeIf(entry ->
                entry.getKey().startsWith(prefix)
                        || entry.getValue().targetMapId().startsWith(prefix));
    }

    private void resetPlayerVillageToBase() {
        playerVillageStage = 1;
        MapArea area = new MapArea(PLAYER_VILLAGE_ID, "Oathstead Camp", "village", playerCampTiles("green"));
        addPlayerCampProps(area);
        area.landmarks.put(new TilePoint(14, 17), "Oathstead Camp");
        maps.put(PLAYER_VILLAGE_ID, area);
        cityBuildings.put(PLAYER_VILLAGE_ID, new ArrayList<>());
        registerPlayerVillageTransitions();
    }

    private void expandPlayerVillage(int margin) {
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        if (area == null || margin <= 0) {
            return;
        }
        char[][] expanded = filled(area.width() + margin * 2, area.height() + margin * 2, playerVillageGroundTile());
        for (int y = 0; y < area.height(); y++) {
            System.arraycopy(area.tiles[y], 0, expanded[y + margin], margin, area.width());
        }
        extendPlayerVillageRoads(expanded, margin);

        MapArea shiftedArea = new MapArea(area.id, area.label, area.kind, expanded);
        for (Map.Entry<TilePoint, String> entry : area.landmarks.entrySet()) {
            TilePoint point = entry.getKey();
            shiftedArea.landmarks.put(new TilePoint(point.x() + margin, point.y() + margin), entry.getValue());
        }
        for (WorldProp prop : area.props) {
            shiftedArea.props.add(new WorldProp(prop.x() + margin, prop.y() + margin, prop.asset(), prop.size()));
        }
        maps.put(PLAYER_VILLAGE_ID, shiftedArea);

        List<CityBuilding> shiftedBuildings = new ArrayList<>();
        for (CityBuilding building : cityBuildings(PLAYER_VILLAGE_ID)) {
            shiftedBuildings.add(new CityBuilding(
                    building.key(),
                    building.x1() + margin,
                    building.y1() + margin,
                    building.x2() + margin,
                    building.y2() + margin,
                    building.style(),
                    building.palette()
            ));
        }
        cityBuildings.put(PLAYER_VILLAGE_ID, shiftedBuildings);

        List<WorldProp> shiftedPlayerProps = new ArrayList<>();
        for (WorldProp prop : playerVillageProps) {
            shiftedPlayerProps.add(new WorldProp(prop.x() + margin, prop.y() + margin, prop.asset(), prop.size()));
        }
        playerVillageProps.clear();
        playerVillageProps.addAll(shiftedPlayerProps);

        Map<TilePoint, Character> shiftedTiles = new LinkedHashMap<>();
        for (Map.Entry<TilePoint, Character> entry : playerVillageTiles.entrySet()) {
            TilePoint point = entry.getKey();
            shiftedTiles.put(new TilePoint(point.x() + margin, point.y() + margin), entry.getValue());
        }
        playerVillageTiles.clear();
        playerVillageTiles.putAll(shiftedTiles);

        shiftPlayerVillageInteriors(margin);
        registerPlayerVillageTransitions();
    }

    private void shiftPlayerVillageInteriors(int margin) {
        String prefix = "house_" + PLAYER_VILLAGE_ID + "_";
        List<String> managedInteriors = maps.keySet().stream()
                .filter(mapId -> mapId.startsWith(prefix))
                .toList();
        for (String mapId : managedInteriors) {
            maps.remove(mapId);
            interiorNpcs.remove(mapId);
        }
        transitions.entrySet().removeIf(entry ->
                entry.getKey().startsWith(prefix)
                        || entry.getValue().targetMapId().startsWith(prefix));

        Map<String, List<WorldProp>> shiftedProps = new HashMap<>();
        for (Map.Entry<String, List<WorldProp>> entry : playerInteriorProps.entrySet()) {
            String shiftedMapId = shiftedPlayerInteriorMapId(entry.getKey(), margin);
            shiftedProps.put(shiftedMapId, new ArrayList<>(entry.getValue()));
        }
        playerInteriorProps.clear();
        playerInteriorProps.putAll(shiftedProps);
    }

    private String shiftedPlayerInteriorMapId(String mapId, int margin) {
        String prefix = "house_" + PLAYER_VILLAGE_ID + "_";
        if (!mapId.startsWith(prefix)) {
            return mapId;
        }
        String[] fields = mapId.substring(prefix.length()).split("_");
        if (fields.length != 2) {
            return mapId;
        }
        try {
            int x = Integer.parseInt(fields[0]) + margin;
            int y = Integer.parseInt(fields[1]) + margin;
            return prefix + x + "_" + y;
        } catch (NumberFormatException ignored) {
            return mapId;
        }
    }

    private void extendPlayerVillageRoads(char[][] grid, int offset) {
        int oldWest = offset;
        int oldEast = grid[0].length - offset - 1;
        int oldNorth = offset;
        int oldSouth = grid.length - offset - 1;
        rect(grid, 13 + offset, 0, 14 + offset, grid.length - 1, 'r');
        rect(grid, 0, 9 + offset, grid[0].length - 1, 10 + offset, 'r');
        rect(grid, Math.max(0, oldWest - 8), 16 + offset, Math.min(grid[0].length - 1, oldEast + 8), 16 + offset, 'r');
        rect(grid, oldWest, oldNorth, oldEast, oldNorth, 'g');
        rect(grid, oldWest, oldSouth, oldEast, oldSouth, 'g');
    }

    private void registerPlayerVillageTransitions() {
        transitions.entrySet().removeIf(entry ->
                entry.getKey().startsWith(PLAYER_VILLAGE_ID + ":")
                        || (OVERWORLD_ID.equals(entry.getKey().split(":", 2)[0])
                        && PLAYER_VILLAGE_ID.equals(entry.getValue().targetMapId())));
        int offset = playerVillageExpansionOffset();
        int ox = START_POSITION.x();
        int oy = START_POSITION.y();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (Math.abs(dx) + Math.abs(dy) <= 3) {
                    TilePoint entry = shiftedVillageEntryPoint(dx, dy, offset);
                    addTransition(OVERWORLD_ID, ox + dx, oy + dy, PLAYER_VILLAGE_ID, entry.x(), entry.y(), "You enter Oathstead Camp.");
                }
            }
        }
        int northY = 0;
        int southY = height(PLAYER_VILLAGE_ID) - 1;
        int westX = 0;
        int eastX = width(PLAYER_VILLAGE_ID) - 1;
        addTransition(PLAYER_VILLAGE_ID, 13 + offset, northY, OVERWORLD_ID, ox, oy - 3, "You leave Oathstead Camp.");
        addTransition(PLAYER_VILLAGE_ID, 14 + offset, northY, OVERWORLD_ID, ox, oy - 3, "You leave Oathstead Camp.");
        addTransition(PLAYER_VILLAGE_ID, 13 + offset, southY, OVERWORLD_ID, ox, oy + 3, "You leave Oathstead Camp.");
        addTransition(PLAYER_VILLAGE_ID, 14 + offset, southY, OVERWORLD_ID, ox, oy + 3, "You leave Oathstead Camp.");
        addTransition(PLAYER_VILLAGE_ID, westX, 9 + offset, OVERWORLD_ID, ox - 3, oy, "You leave Oathstead Camp.");
        addTransition(PLAYER_VILLAGE_ID, westX, 10 + offset, OVERWORLD_ID, ox - 3, oy, "You leave Oathstead Camp.");
        addTransition(PLAYER_VILLAGE_ID, eastX, 9 + offset, OVERWORLD_ID, ox + 3, oy, "You leave Oathstead Camp.");
        addTransition(PLAYER_VILLAGE_ID, eastX, 10 + offset, OVERWORLD_ID, ox + 3, oy, "You leave Oathstead Camp.");
    }

    private int playerVillageExpansionOffset() {
        return (Math.max(1, playerVillageStage) - 1) * 10;
    }

    public CityBuilding placePlayerVillageBuilding(String style, int x, int y) {
        VillageManager.BuildingPlan plan = VillageManager.buildingPlan(style);
        int[] size = plan.size();
        if (!canPlacePlayerVillageBuilding(x, y, size[0], size[1], null)) {
            return null;
        }
        List<CityBuilding> buildings = mutableCityBuildings(PLAYER_VILLAGE_ID);
        int palette = Math.floorMod(x * 31 + y * 17 + buildings.size(), 3);
        String key = "player_" + plan.style() + "_" + x + "_" + y + "_" + buildings.size();
        CityBuilding building = new CityBuilding(key, x, y, x + size[0] - 1, y + size[1] - 1, plan.style(), palette);
        buildings.add(building);
        playerVillageBuildingLevels.put(building.key(), 1);
        return building;
    }

    public boolean restorePlayerVillageBuilding(CityBuilding building) {
        if (building == null || !building.key().startsWith("player_")
                || !canPlacePlayerVillageBuilding(building.x1(), building.y1(), building.width(), building.depth(), null)) {
            return false;
        }
        mutableCityBuildings(PLAYER_VILLAGE_ID).add(building);
        playerVillageBuildingLevels.putIfAbsent(building.key(), 1);
        return true;
    }

    public boolean movePlayerVillageBuilding(CityBuilding building, int x, int y) {
        if (building == null || !building.key().startsWith("player_")) {
            return false;
        }
        if (!canPlacePlayerVillageBuilding(x, y, building.width(), building.depth(), building)) {
            return false;
        }
        int level = playerVillageBuildingLevel(building);
        removePlayerVillageBuilding(building);
        CityBuilding moved = new CityBuilding(building.key(), x, y, x + building.width() - 1, y + building.depth() - 1, building.style(), building.palette());
        mutableCityBuildings(PLAYER_VILLAGE_ID).add(moved);
        playerVillageBuildingLevels.put(moved.key(), level);
        return true;
    }

    public boolean removePlayerVillageBuilding(CityBuilding building) {
        if (building == null || !building.key().startsWith("player_")) {
            return false;
        }
        List<CityBuilding> buildings = mutableCityBuildings(PLAYER_VILLAGE_ID);
        boolean removed = buildings.remove(building);
        if (removed) {
            playerVillageBuildingLevels.remove(building.key());
        }
        return removed;
    }

    public boolean canPlacePlayerVillageBuilding(int x, int y, int width, int depth, CityBuilding ignoredBuilding) {
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        if (area == null || x < 1 || y < 1 || x + width >= area.width() - 1 || y + depth >= area.height() - 1) {
            return false;
        }
        for (int yy = y; yy < y + depth; yy++) {
            for (int xx = x; xx < x + width; xx++) {
                char tile = area.tileAt(xx, yy);
                boolean ignoredTile = ignoredBuilding != null && ignoredBuilding.contains(xx, yy);
                if ((!ignoredTile && !Terrain.passable(tile)) || tile == 'w') {
                    return false;
                }
                if (transitionAt(PLAYER_VILLAGE_ID, xx, yy) != null) {
                    return false;
                }
                if (playerVillagePropAt(xx, yy) != null) {
                    return false;
                }
            }
        }
        for (CityBuilding building : cityBuildings(PLAYER_VILLAGE_ID)) {
            if (building.equals(ignoredBuilding)) {
                continue;
            }
            if (rectsOverlap(x, y, x + width - 1, y + depth - 1, building.x1(), building.y1(), building.x2(), building.y2())) {
                return false;
            }
        }
        return true;
    }

    private void clearPlayerVillageTilesIn(CityBuilding building) {
        for (int yy = building.y1(); yy <= building.y2(); yy++) {
            for (int xx = building.x1(); xx <= building.x2(); xx++) {
                playerVillageTiles.remove(new TilePoint(xx, yy));
            }
        }
    }

    public boolean setPlayerVillageTile(int x, int y, char tile) {
        if (!canSetPlayerVillageTile(x, y, tile)) {
            return false;
        }
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        area.tiles[y][x] = tile;
        TilePoint point = new TilePoint(x, y);
        if (tile == playerVillageGroundTile()) {
            playerVillageTiles.remove(point);
        } else {
            playerVillageTiles.put(point, tile);
        }
        return true;
    }

    public boolean canSetPlayerVillageTile(int x, int y, char tile) {
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        if (area == null || x < 1 || y < 1 || x >= area.width() - 1 || y >= area.height() - 1) {
            return false;
        }
        if (!Terrain.passable(tile) || tile == 'h' || tile == 'w' || tile == 'x' || tile == 'c' || tile == 'u' || tile == 'd') {
            return false;
        }
        if (transitionAt(PLAYER_VILLAGE_ID, x, y) != null || cityBuildingAt(PLAYER_VILLAGE_ID, x, y) != null) {
            return false;
        }
        if (playerVillagePropAt(x, y) != null && tile == 'r') {
            return false;
        }
        return true;
    }

    public boolean restorePlayerVillageTile(int x, int y, char tile) {
        return setPlayerVillageTile(x, y, tile);
    }

    public int[] playerBuildingSize(String style) {
        return VillageManager.buildingSize(style);
    }

    public boolean addPlayerVillageProp(int x, int y, String asset, int size) {
        if (!canPlacePlayerVillageProp(x, y, null)) {
            return false;
        }
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        WorldProp prop = new WorldProp(x, y, asset, size);
        playerVillageProps.add(prop);
        area.props.add(prop);
        return true;
    }

    public boolean canPlacePlayerVillageProp(int x, int y, WorldProp ignoredProp) {
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        if (area == null || x < 1 || y < 1 || x >= area.width() - 1 || y >= area.height() - 1
                || !Terrain.passable(area.tileAt(x, y)) || area.tileAt(x, y) == 'r' || area.tileAt(x, y) == 'h'
                || cityBuildingAt(PLAYER_VILLAGE_ID, x, y) != null) {
            return false;
        }
        WorldProp existing = playerVillagePropAt(x, y);
        return existing == null || existing.equals(ignoredProp);
    }

    public boolean restorePlayerVillageProp(WorldProp prop) {
        if (prop == null) {
            return false;
        }
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        if (area == null) {
            return false;
        }
        playerVillageProps.add(prop);
        area.props.add(prop);
        return true;
    }

    public WorldProp playerVillagePropAt(int x, int y) {
        for (int i = playerVillageProps.size() - 1; i >= 0; i--) {
            WorldProp prop = playerVillageProps.get(i);
            if (prop.x() == x && prop.y() == y) {
                return prop;
            }
        }
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        if (area != null) {
            for (int i = area.props.size() - 1; i >= 0; i--) {
                WorldProp prop = area.props.get(i);
                if (prop.x() == x && prop.y() == y) {
                    return prop;
                }
            }
        }
        return null;
    }

    public boolean removePlayerVillageProp(WorldProp prop) {
        if (prop == null) {
            return false;
        }
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        boolean removed = playerVillageProps.remove(prop);
        if (area != null) {
            removed = area.props.remove(prop) || removed;
        }
        return removed;
    }

    public boolean movePlayerVillageProp(WorldProp prop, int x, int y) {
        if (prop == null) {
            return false;
        }
        removePlayerVillageProp(prop);
        if (addPlayerVillageProp(x, y, prop.asset(), prop.size())) {
            return true;
        }
        restorePlayerVillageProp(prop);
        return false;
    }

    public boolean addPlayerInteriorProp(String mapId, int x, int y, String asset, int size) {
        if (!canPlacePlayerInteriorProp(mapId, x, y, asset)) {
            return false;
        }
        MapArea area = maps.get(mapId);
        WorldProp prop = new WorldProp(x, y, asset, size);
        playerInteriorProps.computeIfAbsent(mapId, ignored -> new ArrayList<>()).add(prop);
        applyInteriorProp(area, prop);
        return true;
    }

    public boolean canPlacePlayerInteriorProp(String mapId, int x, int y, String asset) {
        MapArea area = maps.get(mapId);
        return area != null && "interior".equals(area.kind) && canPlaceInteriorAsset(area, x, y, asset);
    }

    public int[] interiorPlacementSize(String asset) {
        int[] footprint = interiorPlacementFootprint(asset);
        return new int[]{footprint[0], footprint[1]};
    }

    public boolean restorePlayerInteriorProp(String mapId, WorldProp prop) {
        if (mapId == null || prop == null) {
            return false;
        }
        playerInteriorProps.computeIfAbsent(mapId, ignored -> new ArrayList<>()).add(prop);
        MapArea area = maps.get(mapId);
        if (area != null && "interior".equals(area.kind) && canPlaceInteriorAsset(area, prop.x(), prop.y(), prop.asset())) {
            applyInteriorProp(area, prop);
        }
        return true;
    }

    public WorldProp playerInteriorPropAt(String mapId, int x, int y) {
        List<WorldProp> props = playerInteriorProps.getOrDefault(mapId, List.of());
        for (int i = props.size() - 1; i >= 0; i--) {
            WorldProp prop = props.get(i);
            int[] footprint = interiorHitboxFootprint(prop.asset());
            if (x >= prop.x() && y >= prop.y()
                    && x < prop.x() + footprint[0]
                    && y < prop.y() + footprint[1]) {
                return prop;
            }
        }
        return null;
    }

    public boolean removePlayerInteriorProp(String mapId, WorldProp prop) {
        if (mapId == null || prop == null) {
            return false;
        }
        List<WorldProp> props = playerInteriorProps.get(mapId);
        boolean removed = props != null && props.remove(prop);
        MapArea area = maps.get(mapId);
        if (area != null) {
            area.props.remove(prop);
            clearInteriorFootprint(area, prop);
        }
        return removed;
    }

    public boolean movePlayerInteriorProp(String mapId, WorldProp prop, int x, int y) {
        if (mapId == null || prop == null) {
            return false;
        }
        removePlayerInteriorProp(mapId, prop);
        if (addPlayerInteriorProp(mapId, x, y, prop.asset(), prop.size())) {
            return true;
        }
        restorePlayerInteriorProp(mapId, prop);
        return false;
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
            int seed = wx * 928371 + wy * 364479 + sourceMapId.hashCode();
            String theme = interiorTheme(building, seed);
            MapArea house = new MapArea(mapId, interiorLabel(theme), "interior", houseTiles(theme, seed));
            addHouseProps(house, theme, seed);
            applyPlayerInteriorProps(house);
            interiorNpcs.put(mapId, PLAYER_VILLAGE_ID.equals(sourceMapId)
                    ? List.of()
                    : interiorNpcsFor(mapId, theme, house.width(), house.height(), seed));
            maps.put(mapId, house);
        }
        MapArea house = area(mapId);
        int doorY = Math.max(1, house.height() - 2);
        int doorX = Math.max(1, house.width() / 2 - 1);
        addTransition(mapId, doorX, doorY, sourceMapId, returnX, returnY, "You step back outside.");
        addTransition(mapId, doorX + 1, doorY, sourceMapId, returnX, returnY, "You step back outside.");
        return mapId;
    }

    public TilePoint interiorEntryPoint(String mapId) {
        MapArea house = maps.get(mapId);
        if (house == null || !"interior".equals(house.kind)) {
            return new TilePoint(0, 0);
        }
        int startY = Math.max(1, house.height() - 3);
        int centerX = Math.max(1, house.width() / 2);
        for (int radius = 0; radius < Math.max(house.width(), house.height()); radius++) {
            for (int dy = 0; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int x = centerX + dx;
                    int y = startY - dy;
                    if (x <= 0 || y <= 0 || x >= house.width() - 1 || y >= house.height() - 1) {
                        continue;
                    }
                    if (transitionAt(mapId, x, y) == null && Terrain.passable(house.tileAt(x, y))) {
                        return new TilePoint(x, y);
                    }
                }
            }
        }
        return new TilePoint(Math.max(1, house.width() / 2), Math.max(1, house.height() - 3));
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

    public boolean cityBuildingBlocksMovementAt(String mapId, int x, int y) {
        CityBuilding building = cityBuildingAt(mapId, x, y);
        if (building == null) {
            return false;
        }
        if (PLAYER_VILLAGE_ID.equals(mapId) && isPlayerVillageBuilding(building)) {
            return playerVillageBuildingBlocksMovement(building, x, y);
        }
        return true;
    }

    private boolean isPlayerVillageBuilding(CityBuilding building) {
        return building.key() != null && building.key().startsWith("player_")
                && VillageManager.isManagedBuildingStyle(building.style());
    }

    private boolean playerVillageBuildingBlocksMovement(CityBuilding building, int x, int y) {
        if (cityBuildingDoorTiles(building).contains(new TilePoint(x, y))) {
            return true;
        }
        int inset = playerVillageBuildingSideInset(building);
        int left = Math.min(building.x2(), building.x1() + inset);
        int right = Math.max(left, building.x2() - inset);
        return x >= left && x <= right && y >= building.y1() && y <= building.y2();
    }

    private int playerVillageBuildingSideInset(CityBuilding building) {
        if (List.of("garden", "shrine", "watchtower").contains(building.style())) {
            return 0;
        }
        if (building.width() >= 6) {
            return 2;
        }
        if (building.width() >= 4) {
            return 1;
        }
        return 0;
    }

    public List<TilePoint> cityBuildingDoorTiles(CityBuilding building) {
        List<Integer> centers = new ArrayList<>();
        if (building.key() != null && building.key().startsWith("player_")) {
            centers.add(building.x1() + building.width() / 2);
        } else if (building.width() <= 3 || List.of("hall", "guild", "barracks", "warehouse").contains(building.style())) {
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

    public String locationKindAt(String mapId, int x, int y) {
        LocationPatch location = locationAt(mapId, x, y);
        return location == null ? null : location.kind;
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
        return objectivePoint(locationKind, locationIndex, variant, null);
    }

    public TilePoint objectivePoint(String locationKind, int locationIndex, int variant, String preferredAsset) {
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
        TilePoint propPoint = objectivePropPoint(patch, variant, preferredAsset);
        if (propPoint != null) {
            return propPoint;
        }
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

    private TilePoint objectivePropPoint(LocationPatch patch, int variant, String preferredAsset) {
        if (preferredAsset == null || preferredAsset.isBlank()) {
            return null;
        }
        MapArea overworld = maps.get(OVERWORLD_ID);
        if (overworld == null) {
            return null;
        }
        List<WorldProp> candidates = new ArrayList<>();
        for (WorldProp prop : overworld.props) {
            if (preferredAsset.equals(prop.asset()) && patch.contains(prop.x(), prop.y())) {
                candidates.add(prop);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort((a, b) -> {
            int da = Math.abs(a.x() - patch.cx) + Math.abs(a.y() - patch.cy);
            int db = Math.abs(b.x() - patch.cx) + Math.abs(b.y() - patch.cy);
            if (da != db) {
                return Integer.compare(da, db);
            }
            int byY = Integer.compare(a.y(), b.y());
            return byY != 0 ? byY : Integer.compare(a.x(), b.x());
        });
        WorldProp prop = candidates.get(Math.floorMod(variant, candidates.size()));
        return new TilePoint(prop.x(), prop.y());
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
        addCity("town_briarbridge", "Briarbridge Town", 131, 121, "riverside");
        addCity("town_ironvale", "Ironvale Town", 225, 108, "highwall");
        addCity("town_moonspire", "Moonspire Town", 170, 180, "archive");
        addCity("town_reedwatch", "Reedwatch Town", 259, 205, "belltower");
        addCity("town_embermarket", "Embermarket Town", 126, 269, "sanctum");

        addPlayerCamp(PLAYER_VILLAGE_ID, "Oathstead Camp", START_POSITION.x(), START_POSITION.y(), "green");
        addVillage("village_oakhaven", "Oakhaven Village", OAKHAVEN_POSITION.x(), OAKHAVEN_POSITION.y(), "green");
        addVillage("village_snowrest", "Snowrest Village", 83, 62, "snow");
        addVillage("village_dunewick", "Dunewick Village", 102, 245, "desert");
        addVillage("village_mireford", "Mireford Village", 240, 153, "marsh");
        addVillage("village_foxbarrow", "Foxbarrow Village", 56, 156, "green");
        addVillage("village_pineward", "Pineward Village", 118, 76, "snow");
        addVillage("village_elderford", "Elderford Village", 138, 165, "green");
        addVillage("village_glimmerfen", "Glimmerfen Village", 268, 138, "marsh");
        addVillage("village_sunmere", "Sunmere Village", 78, 266, "desert");
        addVillage("village_redcairn", "Redcairn Village", 205, 254, "desert");

        for (AdventureSite site : adventureSites) {
            addDungeon(site);
        }
        MapArea deepVault = new MapArea("dungeon_stonegate_2", "Stonegate Deep Vault", "dungeon", dungeonTiles(2));
        addDungeonProps(deepVault);
        maps.put(deepVault.id, deepVault);
        addTransition("dungeon_stonegate_1", 28, 10, "dungeon_stonegate_2", 2, 10, "You descend to the deep vault.");
        addTransition("dungeon_stonegate_2", 1, 10, "dungeon_stonegate_1", 27, 10, "You return to the upper halls.");
    }

    private void addCity(String id, String label, int ox, int oy, String variant) {
        cityBuildings.put(id, cityBuildingTemplates(variant));
        boolean town = label.contains("Town");
        MapArea area = new MapArea(id, label, "city", town ? townTiles(variant) : cityTiles(variant));
        addCityProps(area);
        if (town) {
            addTownProps(area);
        }
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
        addSettlementSite(id, label, label.contains("Town") ? "Town" : "City", ox, oy);
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
        addSettlementSite(id, label, "Village", ox, oy);
    }

    private void addPlayerCamp(String id, String label, int ox, int oy, String variant) {
        cityBuildings.put(id, new ArrayList<>());
        MapArea area = new MapArea(id, label, "village", playerCampTiles(variant));
        addPlayerCampProps(area);
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
        area.landmarks.put(new TilePoint(14, 17), label);
        addSettlementSite(id, label, "Player Settlement", ox, oy);
    }

    private void addSettlementSite(String id, String label, String kind, int x, int y) {
        settlementSites.add(new SettlementSite(id, label, kind, x, y, kingdomAt(x, y).id()));
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

    private TilePoint shiftedVillageEntryPoint(int dx, int dy, int offset) {
        TilePoint base = villageEntryPoint(dx, dy);
        return new TilePoint(base.x() + offset, base.y() + offset);
    }

    private void addDungeon(String id, String label, int ox, int oy, int depth) {
        MapArea area = new MapArea(id, label, "dungeon", dungeonTiles(depth));
        addDungeonProps(area);
        maps.put(id, area);
        addTransition(OVERWORLD_ID, ox, oy, id, 2, 10, "You descend into " + label + ".");
        addTransition(id, 1, 10, OVERWORLD_ID, ox, oy, "You climb back to the surface.");
        area.landmarks.put(new TilePoint(2, 10), label);
    }

    private void addDungeon(AdventureSite site) {
        addDungeon(site.id, site.label, site.x, site.y, site.depth);
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

    private char[][] townTiles(String variant) {
        char[][] grid = cityTiles(variant);
        for (int y = 1; y < grid.length - 1; y++) {
            for (int x = 1; x < grid[0].length - 1; x++) {
                int roll = Math.floorMod(hash(x, y, variant.hashCode() + 1701), 100);
                if (grid[y][x] == 'p' && roll < 42) {
                    grid[y][x] = 'C';
                } else if ((grid[y][x] == 'l' || grid[y][x] == 'a') && roll < 28) {
                    grid[y][x] = 'G';
                } else if (grid[y][x] == 'y' && roll < 20) {
                    grid[y][x] = 'V';
                }
            }
        }
        rectIf(grid, 10, 8, 24, 15, 'C', 'p');
        rectIf(grid, 15, 10, 19, 13, 'G', 'p', 'C');
        rectIf(grid, 2, 20, 31, 20, 'G', 'l', 'p', 'C');
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

    private List<CityBuilding> mutableCityBuildings(String mapId) {
        List<CityBuilding> buildings = cityBuildings.get(mapId);
        if (buildings == null) {
            buildings = new ArrayList<>();
            cityBuildings.put(mapId, buildings);
            return buildings;
        }
        if (!(buildings instanceof ArrayList<?>)) {
            buildings = new ArrayList<>(buildings);
            cityBuildings.put(mapId, buildings);
        }
        return buildings;
    }

    private boolean rectsOverlap(int ax1, int ay1, int ax2, int ay2, int bx1, int by1, int bx2, int by2) {
        return ax1 <= bx2 && ax2 >= bx1 && ay1 <= by2 && ay2 >= by1;
    }

    private void stampBuildingTiles(String mapId, CityBuilding building, char tile) {
        MapArea area = maps.get(mapId);
        if (area == null) {
            return;
        }
        rect(area.tiles, building.x1(), building.y1(), building.x2(), building.y2(), tile);
    }

    private char playerVillageGroundTile() {
        return 'g';
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
        addVillageRoadNetwork(grid);
        addVillageClearings(grid, variant);
        rect(grid, 13, 8, 15, 10, villagePlazaTile(variant));
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

    private char[][] playerCampTiles(String variant) {
        char base = switch (variant) {
            case "snow" -> 'n';
            case "desert" -> 's';
            case "marsh" -> 'v';
            default -> 'f';
        };
        char[][] grid = filled(36, 28, base);
        organicFill(grid, 17, 14, 15, 10, 'g', 431);
        organicFill(grid, 7, 5, 4, 3, 'f', 439);
        organicFill(grid, 28, 22, 5, 3, 'f', 443);
        addVillageRoadNetwork(grid);
        rect(grid, 9, 16, 19, 16, 'r');
        rect(grid, 19, 16, 25, 16, 'r');
        addPlayerVillageBuildableClearings(grid);
        return grid;
    }

    private void addVillageRoadNetwork(char[][] grid) {
        rect(grid, 13, 0, 14, 27, 'r');
        rect(grid, 0, 9, 35, 10, 'r');
        rect(grid, 7, 15, 14, 15, 'r');
        rect(grid, 14, 15, 22, 15, 'r');
    }

    private void addVillageClearings(char[][] grid, String variant) {
        char clearing = villageClearingTile(variant);
        organicFill(grid, 6, 5, 4, 3, clearing, 521 + variant.hashCode());
        organicFill(grid, 12, 5, 4, 3, clearing, 547 + variant.hashCode());
        organicFill(grid, 19, 5, 5, 3, clearing, 563 + variant.hashCode());
        organicFill(grid, 8, 13, 5, 4, clearing, 587 + variant.hashCode());
        organicFill(grid, 14, 14, 4, 3, clearing, 601 + variant.hashCode());
        organicFill(grid, 21, 13, 5, 4, clearing, 617 + variant.hashCode());
    }

    private void addPlayerVillageBuildableClearings(char[][] grid) {
        organicFill(grid, 14, 10, 5, 4, 'g', 631);
        organicFill(grid, 22, 10, 6, 4, 'g', 641);
        organicFill(grid, 9, 21, 6, 4, 'g', 653);
        organicFill(grid, 23, 21, 7, 4, 'g', 661);
        softenRectangularVillageEdges(grid, 'g', 'f');
    }

    private void softenRectangularVillageEdges(char[][] grid, char clearing, char edgeTile) {
        for (int y = 1; y < grid.length - 1; y++) {
            for (int x = 1; x < grid[0].length - 1; x++) {
                if (grid[y][x] != clearing) {
                    continue;
                }
                int forestNeighbors = 0;
                for (int yy = y - 1; yy <= y + 1; yy++) {
                    for (int xx = x - 1; xx <= x + 1; xx++) {
                        if (grid[yy][xx] == edgeTile) {
                            forestNeighbors++;
                        }
                    }
                }
                int roll = Math.floorMod(hash(x, y, 719), 100);
                if (forestNeighbors >= 4 && roll < 38) {
                    grid[y][x] = edgeTile;
                }
            }
        }
    }

    private char villageClearingTile(String variant) {
        return switch (variant) {
            case "snow" -> 'n';
            case "desert" -> 's';
            case "marsh" -> 'f';
            default -> 'g';
        };
    }

    private char villagePlazaTile(String variant) {
        return switch (variant) {
            case "snow" -> 'q';
            case "desert" -> 'a';
            case "marsh" -> 'y';
            default -> 'p';
        };
    }

    private char[][] dungeonTiles(int depth) {
        char[][] grid = filled(30, 22, 'x');
        int salt = 1100 + depth * 137;
        TilePoint[] rooms = {
                new TilePoint(6, 6),
                new TilePoint(14, 5),
                new TilePoint(23, 7),
                new TilePoint(7, 15),
                new TilePoint(17, 14),
                new TilePoint(24, 16)
        };
        int[][] radii = {
                {5, 4}, {4, 3}, {5, 4}, {5, 4}, {5, 4}, {4, 3}
        };
        for (int i = 0; i < rooms.length; i++) {
            carveDungeonRoom(grid, rooms[i], radii[i][0], radii[i][1], salt + i * 31);
        }
        carveDungeonCorridor(grid, new TilePoint(2, 10), rooms[0], salt + 11);
        carveDungeonCorridor(grid, rooms[0], rooms[1], salt + 17);
        carveDungeonCorridor(grid, rooms[1], rooms[2], salt + 23);
        carveDungeonCorridor(grid, rooms[0], rooms[3], salt + 29);
        carveDungeonCorridor(grid, rooms[3], rooms[4], salt + 37);
        carveDungeonCorridor(grid, rooms[4], rooms[5], salt + 41);
        carveDungeonCorridor(grid, rooms[2], rooms[5], salt + 43);
        carveDungeonCorridor(grid, rooms[4], new TilePoint(28, 10), salt + 47);
        grid[10][1] = 'd';
        grid[10][2] = 'd';
        grid[10][28] = 'd';
        decorateDungeonFloors(grid, depth, salt);
        if (depth > 1) {
            organicFill(grid, 14, 10, 3, 2, 'w', salt + 59);
            grid[10][14] = 'd';
            grid[10][15] = 'd';
            rectIf(grid, 15, 12, 19, 16, 'S', 'd', 'D', 'F', 'M', 'R', 'L');
        }
        return grid;
    }

    private void carveDungeonRoom(char[][] grid, TilePoint center, int rx, int ry, int salt) {
        organicFill(grid, center.x(), center.y(), rx, ry, 'd', salt);
        if (Math.floorMod(salt, 3) == 0) {
            organicFill(grid, center.x() + 1, center.y(), Math.max(2, rx - 2), Math.max(2, ry - 1), 'd', salt + 7);
        }
    }

    private void carveDungeonCorridor(char[][] grid, TilePoint from, TilePoint to, int salt) {
        int x = from.x();
        int y = from.y();
        int guard = 0;
        while ((x != to.x() || y != to.y()) && guard++ < 120) {
            int dx = Integer.compare(to.x(), x);
            int dy = Integer.compare(to.y(), y);
            boolean horizontal = Math.abs(to.x() - x) >= Math.abs(to.y() - y);
            if (Math.floorMod(hash(x + guard, y, salt), 5) == 0) {
                horizontal = !horizontal;
            }
            if (horizontal && dx != 0) {
                x += dx;
            } else if (dy != 0) {
                y += dy;
            } else if (dx != 0) {
                x += dx;
            }
            carveDungeonStep(grid, x, y, salt + guard);
        }
    }

    private void carveDungeonStep(char[][] grid, int x, int y, int salt) {
        if (x <= 0 || y <= 0 || x >= grid[0].length - 1 || y >= grid.length - 1) {
            return;
        }
        grid[y][x] = 'd';
        if (Math.floorMod(hash(x, y, salt), 100) < 42) {
            if (x + 1 < grid[0].length - 1) {
                grid[y][x + 1] = 'd';
            }
        }
        if (Math.floorMod(hash(x, y, salt + 3), 100) < 34) {
            if (y + 1 < grid.length - 1) {
                grid[y + 1][x] = 'd';
            }
        }
    }

    private void decorateDungeonFloors(char[][] grid, int depth, int salt) {
        for (int y = 1; y < grid.length - 1; y++) {
            for (int x = 1; x < grid[0].length - 1; x++) {
                if (grid[y][x] != 'd') {
                    continue;
                }
                int roll = Math.floorMod(hash(x, y, salt), 100);
                if (roll < 8) {
                    grid[y][x] = 'R';
                } else if (roll < 15) {
                    grid[y][x] = 'F';
                } else if (roll < 20) {
                    grid[y][x] = 'M';
                } else if (roll < 24) {
                    grid[y][x] = 'D';
                } else if (roll < 27) {
                    grid[y][x] = 'L';
                }
            }
        }
        rectIf(grid, 21, 14, 26, 17, depth > 1 ? 'S' : 'D', 'd', 'F', 'M', 'R', 'L');
        rectIf(grid, 5, 5, 9, 8, 'M', 'd', 'F', 'R');
        rectIf(grid, 12, 4, 16, 6, 'F', 'd', 'M', 'R');
    }

    private String interiorTheme(CityBuilding building, int seed) {
        if (building == null) {
            return Math.floorMod(seed, 4) == 0 ? "tavern" : "home";
        }
        String key = building.key();
        String style = building.style();
        if (key.contains("tavern") || key.contains("inn") || key.contains("lodge") || "inn".equals(style)) {
            return "tavern";
        }
        if (key.contains("armory") || key.contains("barracks") || key.contains("bellfoundry")
                || "barracks".equals(style) || "mine".equals(style) || "blacksmith".equals(style)) {
            return "blacksmith";
        }
        if (key.contains("workshop") || key.contains("bellwright") || key.contains("bindery") || key.contains("stables")
                || key.startsWith("player_shop") || "forestry_hut".equals(style)) {
            return "carpenter";
        }
        if (key.contains("apothecary") || key.contains("market") || key.contains("shop")
                || "shop".equals(style) || "warehouse".equals(style) || "hunting_camp".equals(style)
                || "apothecary".equals(style) || "bakery".equals(style) || "fishing_hut".equals(style)) {
            return "shop";
        }
        if (key.contains("archive") || key.contains("scriptorium") || key.contains("study") || "guild".equals(style) || "hall".equals(style)) {
            return "study";
        }
        return "home";
    }

    private String interiorLabel(String theme) {
        return switch (theme) {
            case "blacksmith" -> "Blacksmith Workshop";
            case "carpenter" -> "Carpenter Workshop";
            case "tavern" -> "Tavern Interior";
            case "shop" -> "Shop Interior";
            case "study" -> "Study Interior";
            default -> "Home Interior";
        };
    }

    private char[][] houseTiles(String theme, int seed) {
        int width = switch (theme) {
            case "blacksmith" -> 20;
            case "carpenter" -> 21;
            case "tavern" -> 24;
            case "shop" -> 22;
            case "study" -> 23;
            default -> Math.floorMod(seed, 3) == 0 ? 16 : 17;
        };
        int height = switch (theme) {
            case "blacksmith", "carpenter", "study" -> 14;
            case "tavern" -> 15;
            case "shop" -> 13;
            default -> Math.floorMod(seed / 7, 2) == 0 ? 11 : 12;
        };
        char[][] grid = filled(width, height, 'x');
        carveInteriorFootprint(grid, theme, seed);
        wrapInteriorWalls(grid);
        addInteriorRooms(grid, theme);
        int doorX = Math.max(1, width / 2 - 1);
        grid[height - 2][doorX] = 'e';
        grid[height - 2][doorX + 1] = 'e';

        switch (theme) {
            case "blacksmith" -> {
                rect(grid, 2, 2, 6, 3, 'k');
                rect(grid, 15, 2, 19, 3, 'k');
                rect(grid, 3, 7, 5, 8, 'k');
                rect(grid, 10, 6, 13, 8, 'k');
                rect(grid, 16, 10, 18, 12, 'k');
            }
            case "carpenter" -> {
                rect(grid, 3, 2, 8, 3, 'k');
                rect(grid, 14, 2, 19, 3, 'k');
                rect(grid, 4, 7, 6, 8, 'k');
                rect(grid, 13, 8, 16, 9, 'k');
                rect(grid, 18, 11, 20, 12, 'k');
            }
            case "tavern" -> {
                rect(grid, 2, 2, 9, 3, 'k');
                rect(grid, 18, 3, 23, 5, 'k');
                rect(grid, 6, 7, 8, 9, 'k');
                rect(grid, 14, 8, 16, 10, 'k');
                rect(grid, 20, 10, 22, 12, 'k');
                rect(grid, 10, 13, 17, 14, 'z');
            }
            case "shop" -> {
                rect(grid, 3, 2, 8, 3, 'k');
                rect(grid, 15, 2, 20, 3, 'k');
                rect(grid, 5, 7, 9, 8, 'k');
                rect(grid, 15, 7, 18, 8, 'k');
                rect(grid, 9, 11, 14, 12, 'z');
            }
            case "study" -> {
                rect(grid, 2, 2, 9, 3, 'k');
                rect(grid, 16, 2, 23, 3, 'k');
                rect(grid, 6, 7, 8, 9, 'k');
                rect(grid, 14, 7, 16, 9, 'k');
                rect(grid, 9, 11, 16, 12, 'z');
            }
            default -> {
                rect(grid, 2, 2, 3, 5, 'k');
                rect(grid, width - 6, 2, width - 3, 3, 'k');
                rect(grid, width / 2 - 2, 6, width / 2 + 2, 8, 'z');
                rect(grid, 4, height - 5, 6, height - 4, 'k');
            }
        }
        return grid;
    }

    private void carveInteriorFootprint(char[][] grid, String theme, int seed) {
        int width = grid[0].length;
        int height = grid.length;
        switch (theme) {
            case "tavern" -> {
                rect(grid, 2, 2, width - 3, height - 3, 'i');
                rect(grid, 5, height - 2, width / 2 + 3, height - 2, 'i');
                if (Math.floorMod(seed, 2) == 0) {
                    rect(grid, width - 8, 5, width - 3, height - 4, 'i');
                }
            }
            case "blacksmith" -> {
                rect(grid, 2, 2, width - 4, height - 3, 'i');
                rect(grid, 2, 8, width - 2, height - 4, 'i');
                rect(grid, width / 2 - 2, height - 2, width / 2 + 2, height - 2, 'i');
            }
            case "shop" -> {
                rect(grid, 2, 2, width - 3, height - 3, 'i');
                rect(grid, width - 7, 5, width - 3, height - 2, 'i');
            }
            case "study" -> {
                rect(grid, 2, 2, width - 3, height - 4, 'i');
                rect(grid, 7, height - 4, width - 8, height - 2, 'i');
                if (Math.floorMod(seed, 2) == 0) {
                    rect(grid, 2, 6, 7, height - 3, 'i');
                }
            }
            default -> {
                rect(grid, 2, 2, width - 3, height - 3, 'i');
                if (Math.floorMod(seed, 3) == 0) {
                    rect(grid, 2, height / 2, width / 2 + 2, height - 2, 'i');
                } else if (Math.floorMod(seed, 3) == 1) {
                    rect(grid, width / 2 - 2, 2, width - 3, height - 2, 'i');
                }
            }
        }
    }

    private void wrapInteriorWalls(char[][] grid) {
        List<TilePoint> walls = new ArrayList<>();
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                if (grid[y][x] != 'x') {
                    continue;
                }
                if (adjacentToInteriorFloor(grid, x, y)) {
                    walls.add(new TilePoint(x, y));
                }
            }
        }
        for (TilePoint wall : walls) {
            grid[wall.y()][wall.x()] = 'o';
        }
    }

    private boolean adjacentToInteriorFloor(char[][] grid, int x, int y) {
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                if (ox == 0 && oy == 0) {
                    continue;
                }
                int nx = x + ox;
                int ny = y + oy;
                if (ny >= 0 && ny < grid.length && nx >= 0 && nx < grid[ny].length && grid[ny][nx] == 'i') {
                    return true;
                }
            }
        }
        return false;
    }

    private void addInteriorRooms(char[][] grid, String theme) {
        int width = grid[0].length;
        int height = grid.length;
        switch (theme) {
            case "tavern" -> {
                interiorWallV(grid, width - 9, 3, 8, 5);
                interiorWallH(grid, 9, width - 10, height - 6, width / 2);
            }
            case "blacksmith" -> {
                interiorWallV(grid, 8, 3, height - 5, 8);
                interiorWallH(grid, 9, width - 4, 5, 12);
            }
            case "carpenter" -> {
                interiorWallV(grid, 10, 3, height - 5, 7);
                interiorWallH(grid, 11, width - 4, 6, 15);
            }
            case "shop" -> {
                interiorWallH(grid, 3, width - 4, 5, width / 2);
                interiorWallV(grid, width - 8, 6, height - 4, height - 5);
            }
            case "study" -> {
                interiorWallV(grid, 10, 4, height - 5, 7);
                interiorWallV(grid, width - 11, 4, height - 5, width - 8);
            }
            default -> {
                interiorWallH(grid, 3, width - 4, height / 2, width / 2);
            }
        }
    }

    private void interiorWallH(char[][] grid, int x1, int x2, int y, int doorX) {
        for (int x = Math.max(1, x1); x <= Math.min(grid[0].length - 2, x2); x++) {
            if (grid[y][x] == 'i' || grid[y][x] == 'z' || grid[y][x] == 'k') {
                grid[y][x] = x == doorX || x == doorX + 1 ? 'e' : 'o';
            }
        }
    }

    private void interiorWallV(char[][] grid, int x, int y1, int y2, int doorY) {
        for (int y = Math.max(1, y1); y <= Math.min(grid.length - 2, y2); y++) {
            if (grid[y][x] == 'i' || grid[y][x] == 'z' || grid[y][x] == 'k') {
                grid[y][x] = y == doorY ? 'e' : 'o';
            }
        }
    }

    private void addFurniture(MapArea area, int x, int y, String asset) {
        int[] hitbox = interiorHitboxFootprint(asset);
        if (x < 1 || y < 1 || x + hitbox[0] >= area.width() - 1 || y + hitbox[1] >= area.height() - 1) {
            return;
        }
        if (!canPlaceInteriorAsset(area, x, y, asset)) {
            return;
        }
        if (!isInteriorOverlayAsset(asset) && !isInteriorWallDecorAsset(asset) && !isInteriorPassThroughAsset(asset)) {
            int[] placement = interiorPlacementFootprint(asset);
            rect(area.tiles, x, y, x + placement[0] - 1, y + placement[1] - 1, 'k');
        }
        area.props.add(new WorldProp(x, y, asset, 48));
    }

    private void applyPlayerInteriorProps(MapArea area) {
        for (WorldProp prop : playerInteriorProps.getOrDefault(area.id, List.of())) {
            if (canPlaceInteriorAsset(area, prop.x(), prop.y(), prop.asset())) {
                applyInteriorProp(area, prop);
            }
        }
    }

    private boolean canPlaceInteriorAsset(MapArea area, int x, int y, String asset) {
        int[] footprint = interiorHitboxFootprint(asset);
        if (isInteriorOverlayAsset(asset)) {
            return canPlaceInteriorOverlay(area, x, y);
        }
        if (isInteriorWallDecorAsset(asset)) {
            return canPlaceInteriorWallDecor(area, x, y, footprint[0], footprint[1]);
        }
        if (isInteriorPassThroughAsset(asset)) {
            return canPlaceInteriorPassThrough(area, x, y, footprint[0], footprint[1]);
        }
        return canPlaceFurniture(area, x, y, asset);
    }

    private void applyInteriorProp(MapArea area, WorldProp prop) {
        int[] hitbox = interiorHitboxFootprint(prop.asset());
        if (prop.x() < 1 || prop.y() < 1
                || prop.x() + hitbox[0] >= area.width() - 1
                || prop.y() + hitbox[1] >= area.height() - 1) {
            return;
        }
        if (!isInteriorOverlayAsset(prop.asset()) && !isInteriorWallDecorAsset(prop.asset()) && !isInteriorPassThroughAsset(prop.asset())) {
            int[] placement = interiorPlacementFootprint(prop.asset());
            rect(area.tiles, prop.x(), prop.y(), prop.x() + placement[0] - 1, prop.y() + placement[1] - 1, 'k');
        }
        area.props.add(prop);
    }

    private void clearInteriorFootprint(MapArea area, WorldProp prop) {
        if (isInteriorOverlayAsset(prop.asset()) || isInteriorWallDecorAsset(prop.asset()) || isInteriorPassThroughAsset(prop.asset())) {
            return;
        }
        int[] footprint = interiorPlacementFootprint(prop.asset());
        rect(area.tiles, prop.x(), prop.y(), prop.x() + footprint[0] - 1, prop.y() + footprint[1] - 1, 'i');
    }

    private boolean canPlaceInteriorOverlay(MapArea area, int x, int y) {
        char tile = area.tileAt(x, y);
        if (tile != 'k' && tile != 'i' && tile != 'z') {
            return false;
        }
        for (WorldProp prop : area.props) {
            if (prop.x() == x && prop.y() == y && isInteriorOverlayAsset(prop.asset())) {
                return false;
            }
        }
        return true;
    }

    private boolean canPlaceInteriorPassThrough(MapArea area, int x, int y, int width, int height) {
        if (x < 1 || y < 1 || x + width >= area.width() - 1 || y + height >= area.height() - 1) {
            return false;
        }
        for (int yy = y; yy < y + height; yy++) {
            for (int xx = x; xx < x + width; xx++) {
                char tile = area.tileAt(xx, yy);
                if (!isInteriorPlacementFloor(area, xx, yy, tile)) {
                    return false;
                }
            }
        }
        for (WorldProp prop : area.props) {
            if (isInteriorOverlayAsset(prop.asset()) || isInteriorWallDecorAsset(prop.asset())) {
                continue;
            }
            int[] other = interiorHitboxFootprint(prop.asset());
            if (rectsOverlap(x, y, x + width - 1, y + height - 1,
                    prop.x(), prop.y(), prop.x() + other[0] - 1, prop.y() + other[1] - 1)) {
                return false;
            }
        }
        return true;
    }

    private boolean canPlaceInteriorWallDecor(MapArea area, int x, int y, int width, int height) {
        if (x < 1 || y < 1 || x + width >= area.width() - 1 || y + height >= area.height() - 1) {
            return false;
        }
        for (int yy = y; yy < y + height; yy++) {
            for (int xx = x; xx < x + width; xx++) {
                if (area.tileAt(xx, yy) != 'o') {
                    return false;
                }
                boolean horizontalFace = isInteriorFloorLike(area.tileAt(xx, yy - 1))
                        || isInteriorFloorLike(area.tileAt(xx, yy + 1));
                if (!horizontalFace) {
                    return false;
                }
            }
        }
        for (WorldProp prop : area.props) {
            if (!isInteriorWallDecorAsset(prop.asset())) {
                continue;
            }
            int[] other = interiorHitboxFootprint(prop.asset());
            if (rectsOverlap(x, y, x + width - 1, y + height - 1,
                    prop.x(), prop.y(), prop.x() + other[0] - 1, prop.y() + other[1] - 1)) {
                return false;
            }
        }
        return true;
    }

    private boolean isInteriorFloorLike(char tile) {
        return tile == 'i' || tile == 'z' || tile == 'k' || tile == 'e';
    }

    private boolean canPlaceFurniture(MapArea area, int x, int y, String asset) {
        int[] hitbox = interiorHitboxFootprint(asset);
        if (x < 1 || y < 1 || x + hitbox[0] >= area.width() - 1 || y + hitbox[1] >= area.height() - 1) {
            return false;
        }
        int[] placement = interiorPlacementFootprint(asset);
        for (int yy = y; yy < y + placement[1]; yy++) {
            for (int xx = x; xx < x + placement[0]; xx++) {
                char tile = area.tileAt(xx, yy);
                if (!isInteriorPlacementFloor(area, xx, yy, tile)) {
                    return false;
                }
            }
        }
        for (WorldProp prop : area.props) {
            if (isInteriorWallDecorAsset(prop.asset())) {
                continue;
            }
            int[] other = interiorPlacementFootprint(prop.asset());
            if (rectsOverlap(x, y, x + placement[0] - 1, y + placement[1] - 1,
                    prop.x(), prop.y(), prop.x() + other[0] - 1, prop.y() + other[1] - 1)) {
                return false;
            }
        }
        return true;
    }

    private boolean isInteriorPlacementFloor(MapArea area, int x, int y, char tile) {
        return tile == 'i' || tile == 'z' || (tile == 'k' && isOpenInteriorFurnitureTile(area, x, y));
    }

    private int[] interiorPlacementFootprint(String asset) {
        if (isInteriorJoinableFurnitureAsset(asset)) {
            return new int[]{1, 1};
        }
        return interiorHitboxFootprint(asset);
    }

    private int[] interiorHitboxFootprint(String asset) {
        return switch (asset) {
            case "interior_bed_vertical" -> new int[]{1, 2};
            case "interior_tavern_bar", "interior_shop_counter", "interior_carpenter_table",
                    "interior_alchemy_station", "interior_cooking_station", "interior_herb_drying_rack",
                    "interior_wall_window_wide", "interior_wall_plant_shelf", "interior_wall_herb_rack",
                    "interior_floor_bushy_planter", "interior_aquarium_table" -> new int[]{2, 1};
            case "interior_vine_trellis" -> new int[]{1, 2};
            default -> new int[]{1, 1};
        };
    }

    private boolean isInteriorJoinableFurnitureAsset(String asset) {
        return switch (asset) {
            case "interior_tavern_bar", "interior_shop_counter", "interior_carpenter_table",
                    "interior_alchemy_station", "interior_cooking_station", "interior_herb_drying_rack",
                    "interior_aquarium_table" -> true;
            default -> false;
        };
    }

    private boolean isInteriorOverlayAsset(String asset) {
        return switch (asset) {
            case "interior_tabletop_place_setting", "interior_tabletop_meal", "interior_tabletop_candle",
                    "interior_flower_vase", "interior_seed_bowl", "interior_mortar_pestle" -> true;
            default -> false;
        };
    }

    private boolean isInteriorWallDecorAsset(String asset) {
        return asset != null && asset.startsWith("interior_wall_")
                && !asset.equals("interior_wall_timber")
                && !asset.equals("interior_wall_plaster")
                && !asset.equals("interior_wall_stone")
                && !asset.equals("interior_wall_corner")
                && !asset.startsWith("interior_wall_front_")
                && !asset.startsWith("interior_wall_side_")
                && !asset.startsWith("interior_wall_inner_")
                && !asset.startsWith("interior_wall_outer_")
                && !asset.startsWith("interior_wall_door_");
    }

    private boolean isInteriorPassThroughAsset(String asset) {
        return switch (asset) {
            case "interior_chair_north", "interior_chair_south", "interior_chair_east", "interior_chair_west",
                    "interior_herb_pot", "interior_flower_pot", "interior_floor_sapling_pot",
                    "interior_floor_reed_pot", "interior_floor_flower_planter", "interior_planting_pot",
                    "interior_sprout_planter", "interior_herb_planter", "interior_rug_runner" -> true;
            default -> false;
        };
    }

    private boolean isOpenInteriorFurnitureTile(MapArea area, int x, int y) {
        if (!"interior".equals(area.kind)) {
            return false;
        }
        for (WorldProp prop : area.props) {
            int[] footprint = interiorPlacementFootprint(prop.asset());
            if (x < prop.x() || y < prop.y() || x >= prop.x() + footprint[0] || y >= prop.y() + footprint[1]) {
                continue;
            }
            if (!isInteriorOverlayAsset(prop.asset()) && !isInteriorWallDecorAsset(prop.asset())
                    && !isInteriorPassThroughAsset(prop.asset())) {
                return false;
            }
        }
        return true;
    }

    private void addTableSet(MapArea area, int x, int y) {
        addFurniture(area, x, y, "interior_round_table");
        addFurniture(area, x, y, Math.floorMod(x * 31 + y * 17 + area.id.hashCode(), 3) == 0
                ? "interior_tabletop_candle"
                : "interior_tabletop_place_setting");
        addFurniture(area, x, y - 1, "interior_chair_south");
        addFurniture(area, x, y + 1, "interior_chair_north");
        addFurniture(area, x - 1, y, "interior_chair_east");
        addFurniture(area, x + 1, y, "interior_chair_west");
    }

    private void addBiomeProps(MapArea area, int salt) {
        boolean[][] occupiedProps = new boolean[area.height()][area.width()];
        boolean[][] tallProps = new boolean[area.height()][area.width()];
        for (int y = 2; y < area.height() - 2; y++) {
            for (int x = 2; x < area.width() - 2; x++) {
                char tile = area.tileAt(x, y);
                if (tile == 'w' || tile == '~' || tile == 'c' || tile == 'u' || tile == 'h' || tile == 'x') {
                    continue;
                }
                if (occupiedProps[y][x]) {
                    continue;
                }
                int roll = Math.abs(hash(x, y, salt + area.id.hashCode())) % 1000;
                int cluster = naturalNeighborCount(area, x, y, tile);
                int chance = adjustedDecorationChance(area, x, y, tile, decorationChance(tile, cluster));
                if (roll < chance) {
                    int patchSeed = hash(x / 5, y / 5, salt + tile * 97 + area.id.hashCode());
                    String asset = decorationFor(area, tile, x, y, roll, patchSeed);
                    if (isTallProp(asset) && shouldThinTallProp(tile, asset, tallProps, x, y, roll)) {
                        asset = lowDecorationFor(area, tile, x, y, roll + 31, patchSeed);
                    }
                    int size = decorationSize(tile, roll, asset);
                    area.props.add(new WorldProp(x, y, asset, size));
                    occupiedProps[y][x] = true;
                    if (isTallProp(asset)) {
                        tallProps[y][x] = true;
                    }
                    if (shouldPairDecoration(tile, roll, chance, asset)) {
                        int[] offset = pairedDecorationOffset(area, x, y, tile, roll);
                        int ox = offset[0];
                        int oy = offset[1];
                        if (area.tileAt(x + ox, y + oy) == tile && !occupiedProps[y + oy][x + ox]) {
                            String pairedAsset = lowDecorationFor(area, tile, x + ox, y + oy, roll + 17, patchSeed);
                            area.props.add(new WorldProp(x + ox, y + oy, pairedAsset, decorationSize(tile, roll + 17, pairedAsset)));
                            occupiedProps[y + oy][x + ox] = true;
                        }
                    }
                }
            }
        }
        addSoftBiomeProps(area, salt, occupiedProps);
    }

    private void addSoftBiomeProps(MapArea area, int salt, boolean[][] occupiedProps) {
        for (int y = 2; y < area.height() - 2; y++) {
            for (int x = 2; x < area.width() - 2; x++) {
                char tile = area.tileAt(x, y);
                if ((!isNaturalBiome(tile) && tile != 'w' && tile != '~') || tile == 'q' || occupiedProps[y][x]) {
                    continue;
                }
                int roll = Math.abs(hash(x, y, salt + area.id.hashCode() + 4049)) % 1000;
                int chance = softDecorationChance(area, x, y, tile);
                if (roll >= chance) {
                    continue;
                }
                int patchSeed = hash(x / 4, y / 4, salt + tile * 131 + area.id.hashCode());
                String asset = softDecorationFor(area, tile, x, y, roll, patchSeed);
                area.props.add(new WorldProp(x, y, asset, softDecorationSize(asset, roll)));
                occupiedProps[y][x] = true;
            }
        }
    }

    private int softDecorationChance(MapArea area, int x, int y, char tile) {
        int waterDistance = distanceToAny(area, x, y, 3, new char[]{'w'});
        int roadDistance = distanceToAny(area, x, y, 3, new char[]{'r', 'q'});
        int edgeDistance = distanceToDifferentNatural(area, x, y, tile, 2);
        int chance = switch (tile) {
            case 'g' -> 190;
            case 'f' -> 150;
            case 'v' -> waterDistance <= 2 ? 230 : 170;
            case 'P' -> waterDistance <= 2 ? 185 : 120;
            case 's' -> 125;
            case 'n' -> 145;
            case 'b' -> 115;
            case 'm' -> 90;
            case 'w' -> 132;
            case '~' -> 92;
            default -> 0;
        };
        if ((tile == 'w' || tile == '~') && distanceToDifferentNatural(area, x, y, tile, 3) <= 2) {
            chance += 60;
        }
        if (edgeDistance <= 1) {
            chance += 30;
        }
        if (roadDistance <= 1 && tile != 'm') {
            chance += 18;
        }
        return Math.min(310, chance);
    }

    private String softDecorationFor(MapArea area, char tile, int x, int y, int roll, int patchSeed) {
        boolean nearWater = distanceToAny(area, x, y, 3, new char[]{'w'}) <= 2;
        boolean nearRock = distanceToAny(area, x, y, 3, new char[]{'m', 'q', 'b'}) <= 2;
        return switch (tile) {
            case 'g' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_soft_dense_meadow_daisies", 22), option("deco_soft_dense_meadow_flowers", 18),
                    option("deco_soft_dense_tall_grass", 16), option("deco_soft_dense_leafy_plant", 10),
                    option("deco_soft_grass_tuft", 22), option("deco_soft_grass_blades", 18),
                    option("deco_soft_clover_low", 18), option("deco_soft_clover_white", 12),
                    option("deco_soft_daisy_small", 10), option("deco_soft_yellow_flowers", 8),
                    option("deco_soft_pebbles", 4)
            });
            case 'f' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_soft_leaf_litter", 28), option("deco_soft_moss_stones", 18),
                    option("deco_soft_dense_leaf_bush", 16), option("deco_soft_dense_berry_bush", 10),
                    option("deco_soft_grass_tuft", 14), option("deco_soft_clover_pink", 10),
                    option("deco_soft_mossy_rock", 8), option("deco_soft_purple_flowers", 5)
            });
            case 'v' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_soft_reeds", nearWater ? 36 : 12),
                    option("deco_soft_water_cattails", nearWater ? 24 : 6),
                    option("deco_soft_water_reeds_gold", nearWater ? 18 : 5),
                    option("deco_soft_dense_marsh_grass", 16),
                    option("deco_soft_grass_blades", 18), option("deco_soft_clover_low", 16),
                    option("deco_soft_blue_wildflowers", 8), option("deco_soft_moss_stones", 6),
                    option("deco_soft_pebbles", nearWater ? 8 : 3)
            });
            case 's' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_soft_dry_grass", 42), option("deco_soft_pebbles", 18),
                    option("deco_soft_pebble_cluster", nearRock ? 18 : 10),
                    option("deco_soft_stone_large", nearRock ? 10 : 4),
                    option("deco_soft_yellow_flowers", nearWater ? 8 : 2)
            });
            case 'P' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_beach_shells", 24),
                    option("deco_beach_grass", nearWater ? 20 : 28),
                    option("deco_beach_coconuts", 12),
                    option("deco_beach_driftwood", nearWater ? 14 : 7),
                    option("deco_soft_pebbles", 8)
            });
            case 'n' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_soft_dense_snow_mound", 22), option("deco_soft_dense_snow_grass", 18),
                    option("deco_soft_dense_frost_reeds", 12), option("deco_soft_dense_snow_boulder", nearRock ? 12 : 6),
                    option("deco_soft_frost_grass", 34), option("deco_soft_pebbles", 14),
                    option("deco_soft_stone_large", nearRock ? 12 : 5),
                    option("deco_soft_blue_flowers", 5, 260),
                    option("deco_soft_alpine_mix", 4, 220)
            });
            case 'b' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_soft_dry_grass", 30), option("deco_soft_pebble_cluster", 24),
                    option("deco_soft_flat_stones", 16), option("deco_soft_stone_large", 10),
                    option("deco_soft_purple_rockflowers", 4, 220)
            });
            case 'm' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_soft_pebbles", 28), option("deco_soft_flat_stones", 22),
                    option("deco_soft_stone_large", 18), option("deco_soft_mossy_boulder", 10),
                    option("deco_soft_alpine_mix", 5, 260)
            });
            case 'w' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_soft_water_lily_white", nearWater ? 18 : 9),
                    option("deco_soft_water_lily_pink", 12),
                    option("deco_soft_water_duckweed", 22),
                    option("deco_soft_water_floating_leaves", 16),
                    option("deco_soft_water_lily_reeds", nearWater ? 20 : 8),
                    option("deco_soft_water_lily_reeds_white", nearWater ? 18 : 7),
                    option("deco_soft_water_sparkle_patch", 8, 420),
                    option("deco_soft_water_weed_patch", 14),
                    option("deco_soft_water_wet_stones", nearRock ? 8 : 3),
                    option("deco_soft_water_shore_grass", nearWater ? 12 : 4),
                    option("deco_soft_water_flower_reeds", nearWater ? 10 : 3)
            });
            case '~' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_soft_water_sparkle_patch", 16, 520),
                    option("deco_soft_water_wet_stones", 10),
                    option("deco_soft_water_weed_patch", 8),
                    option("deco_beach_shells", 5, 300)
            });
            default -> "deco_soft_grass_tuft";
        };
    }

    private int softDecorationSize(String asset, int roll) {
        int jitter = Math.floorMod(roll + asset.hashCode(), 5);
        if (asset.contains("dense_snow")) {
            return 28 + jitter * 4;
        }
        if (asset.contains("water_lily") || asset.contains("duckweed") || asset.contains("floating")
                || asset.contains("sparkle") || asset.contains("weed_patch")) {
            return 30 + jitter * 4;
        }
        if (asset.contains("beach_shells")) {
            return 22 + jitter * 3;
        }
        if (asset.contains("beach_grass")) {
            return 26 + jitter * 3;
        }
        if (asset.contains("water_") || asset.contains("dense_")) {
            return 26 + jitter * 4;
        }
        if (asset.contains("stone") || asset.contains("rock") || asset.contains("pebble")) {
            return 18 + jitter * 3;
        }
        if (asset.contains("reeds") || asset.contains("grass")) {
            return 22 + jitter * 3;
        }
        return 20 + jitter * 3;
    }

    private int adjustedDecorationChance(MapArea area, int x, int y, char tile, int baseChance) {
        int chance = baseChance;
        int roadDistance = distanceToAny(area, x, y, 4, new char[]{'r', 'q'});
        int waterDistance = distanceToAny(area, x, y, 4, new char[]{'w'});
        int edgeDistance = distanceToDifferentNatural(area, x, y, tile, 3);
        if (tile == 'f' && edgeDistance <= 1) {
            chance += 32;
        } else if (tile == 'g' && roadDistance <= 1) {
            chance += 18;
        } else if (tile == 'v' && waterDistance <= 2) {
            chance += 36;
        } else if (tile == 's' && waterDistance <= 2) {
            chance += 18;
        } else if (tile == 'P' && waterDistance <= 2) {
            chance += 30;
        } else if ((tile == 'm' || tile == 'q') && roadDistance <= 1) {
            chance += 20;
        }
        if (tile != 'r' && roadDistance == 0) {
            chance = Math.max(0, chance - 24);
        }
        return Math.max(0, Math.min(340, chance));
    }

    private int decorationChance(char tile, int cluster) {
        int clusteredBonus = switch (tile) {
            case 'f' -> Math.min(108, cluster * 5);
            case 'm', 'q' -> Math.min(38, cluster * 2);
            case 'v' -> Math.min(52, cluster * 3);
            case 'P' -> Math.min(42, cluster * 3);
            case 's', 'n', 'b' -> Math.min(34, cluster * 2);
            case 'g' -> Math.min(46, cluster * 3);
            default -> Math.min(12, cluster * 2);
        };
        int base = switch (tile) {
            case 'f' -> 198;
            case 'm' -> 66;
            case 'q' -> 52;
            case 'v' -> 74;
            case 'P' -> 58;
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
            case "deco_soft_grass_tuft", "deco_soft_clover_white", "deco_soft_daisy_patch",
                    "deco_soft_blue_flowers", "deco_soft_yellow_flowers", "deco_soft_purple_flowers",
                    "deco_soft_grass_blades", "deco_soft_clover_pink", "deco_soft_daisy_small",
                    "deco_soft_alpine_mix", "deco_soft_clover_low", "deco_soft_blue_wildflowers",
                    "deco_soft_stone_large", "deco_soft_pebble_cluster", "deco_soft_mossy_boulder",
                    "deco_soft_flat_stones", "deco_soft_pebbles", "deco_soft_mossy_rock",
                    "deco_soft_moss_stones", "deco_soft_leaf_litter", "deco_soft_dry_grass",
                    "deco_soft_frost_grass", "deco_soft_reeds", "deco_soft_purple_rockflowers",
                    "deco_soft_dense_snow_mound", "deco_soft_dense_snow_boulder", "deco_soft_dense_snow_grass",
                    "deco_soft_dense_frost_reeds", "deco_soft_dense_meadow_daisies", "deco_soft_dense_meadow_flowers",
                    "deco_soft_dense_leafy_plant", "deco_soft_dense_tall_grass", "deco_soft_dense_berry_bush",
                    "deco_soft_dense_leaf_bush", "deco_soft_dense_white_blossom_plant", "deco_soft_dense_blue_flower_bush",
                    "deco_soft_water_cattails", "deco_soft_water_reeds_gold", "deco_soft_water_lily_reeds",
                    "deco_soft_water_lily_reeds_white", "deco_soft_water_lily_white", "deco_soft_water_lily_pink",
                    "deco_soft_water_duckweed", "deco_soft_water_floating_leaves", "deco_soft_water_wet_stones",
                    "deco_soft_water_mossy_boulder", "deco_soft_water_shore_grass", "deco_soft_water_flower_reeds",
                    "deco_soft_dense_calla_plant", "deco_soft_dense_marsh_grass", "deco_soft_water_sparkle_patch",
                    "deco_soft_water_weed_patch", "deco_beach_shells", "deco_beach_grass" -> softDecorationSize(asset, roll);
            case "deco_flowers", "deco_grass_clump", "deco_grass_wildflowers", "deco_grass_herb_patch",
                    "deco_imagen_meadow_blooms", "deco_forest_fern", "deco_forest_mushrooms",
                    "deco_forest_blue_mushroom_ring", "deco_bog_grass", "deco_reeds", "deco_mushrooms",
                    "deco_dry_grass", "deco_badlands_dry_grass" -> 28 + jitter * 2;
            case "deco_bush", "deco_tundra_frost_bush" -> 32 + jitter * 2;
            case "deco_forest_log", "deco_forest_moss_rock", "deco_rocks", "deco_desert_rocks",
                    "deco_tundra_rocks", "deco_badlands_rocks", "deco_mountain_rocks",
                    "deco_mountain_cairn", "deco_mountain_pass_way_cairn", "deco_beach_driftwood" -> 36 + jitter * 4;
            case "deco_forest_ancient_roots", "deco_forest_shrine_stone", "deco_forest_fairy_pool",
                    "deco_imagen_grass_pond", "deco_marsh_lily_pool", "deco_marsh_bubble_pool",
                    "deco_mountain_spring_pool", "deco_mountain_pass_snowmelt_pool" -> 58 + jitter * 6;
            case "deco_grass_stone_stack",
                    "deco_desert_sun_bleached_bones", "deco_desert_jar_cache", "deco_tundra_rune_stone",
                    "deco_badlands_skull_marker", "deco_badlands_totem_stones",
                    "deco_mountain_crystal_cluster" -> 42 + jitter * 4;
            case "deco_cactus", "deco_desert_blooming_cactus" -> 46 + jitter * 4;
            case "deco_beach_coconuts" -> 28 + jitter * 3;
            case "deco_beach_palm" -> 72 + jitter * 6;
            case "deco_beach_palm_cluster" -> 80 + jitter * 7;
            case "village_prop_palm_shade", "village_prop_net_drying_rack" -> 46 + jitter * 4;
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
            case 'P' -> isBeachFlora(asset) && roll < chance / 5;
            case 's', 'n', 'b' -> isSmallNatural(asset) && roll < chance / 8;
            default -> false;
        };
    }

    private boolean shouldThinTallProp(char tile, String asset, boolean[][] tallProps, int x, int y, int roll) {
        if (tile == 'f' && isForestTree(asset)) {
            return nearbyTallPropCount(tallProps, x, y, 1) >= 3 || (nearbyTallPropCount(tallProps, x, y, 2) >= 7 && roll % 3 == 0);
        }
        if (tile == 'P' && isBeachPalm(asset)) {
            return nearbyTallPropCount(tallProps, x, y, 2) >= 2 || roll % 5 == 0;
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
                if ((tile == 'p' || tile == 'C') && roll < 12) {
                    String[] options = {
                            "city_prop_fountain_small", "city_prop_crate", "city_prop_barrel",
                            "city_prop_cart", "city_prop_planter_stone", "city_prop_stone_bench"
                    };
                    area.props.add(new WorldProp(x, y, pick(options, roll + area.id.hashCode()), roll < 2 ? 48 : 38 + roll % 12));
                } else if ((tile == 'a' || tile == 'G') && roll < 30) {
                    String[] options = {
                            "city_prop_market_red", "city_prop_market_yellow", "city_prop_market_green",
                            "city_prop_cart", "city_prop_barrel_stack", "city_prop_banner_red",
                            "city_prop_wagon_awning", "city_prop_news_kiosk"
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

    private void addTownProps(MapArea area) {
        String[] commons = {
                "city_prop_news_kiosk", "city_prop_wagon_awning", "city_prop_bunting_pole", "city_prop_stone_bench"
        };
        int[][] anchors = {
                {12, 8, 42}, {22, 8, 44}, {10, 17, 36}, {26, 17, 38}, {17, 20, 42}
        };
        for (int i = 0; i < anchors.length; i++) {
            int[] anchor = anchors[i];
            if (Terrain.passable(area.tileAt(anchor[0], anchor[1])) && cityBuildingAt(area.id, anchor[0], anchor[1]) == null) {
                area.props.add(new WorldProp(anchor[0], anchor[1], commons[Math.floorMod(area.id.hashCode() + i, commons.length)], anchor[2]));
            }
        }
    }

    private void addVillageProps(MapArea area, String variant) {
        addBiomeProps(area, variant.hashCode());
        area.props.add(new WorldProp(14, 9, "city_prop_fountain_small", 42));
        area.props.add(new WorldProp(12, 15, "deco_road_signpost", 36));
        area.props.add(new WorldProp(15, 12, "village_prop_clay_oven", 42));
        area.props.add(new WorldProp(10, 8, "village_prop_wash_line", 48));
        area.props.add(new WorldProp(17, 15, "village_prop_seedling_tray", 36));
        area.props.add(new WorldProp(23, 15, "village_prop_compost_bin", 38));
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

    private void addPlayerCampProps(MapArea area) {
        addManagedCampProp(area, new WorldProp(15, 13, "location_camp_fire", 38));
        addManagedCampProp(area, new WorldProp(18, 14, "location_camp_crates", 38));
        addManagedCampProp(area, new WorldProp(16, 15, "player_village_quest_board", 42));
        addManagedCampProp(area, new WorldProp(11, 15, "deco_road_signpost", 36));
        addManagedCampProp(area, new WorldProp(22, 17, "village_prop_woodpile", 40));
        addManagedCampProp(area, new WorldProp(20, 18, "village_prop_clay_oven", 42));
        addManagedCampProp(area, new WorldProp(24, 18, "village_prop_seedling_tray", 36));
    }

    private void addManagedCampProp(MapArea area, WorldProp prop) {
        area.props.add(prop);
    }

    private void addDungeonProps(MapArea area) {
        for (int y = 3; y < area.height() - 3; y++) {
            for (int x = 3; x < area.width() - 3; x++) {
                int roll = Math.abs(hash(x, y, area.id.hashCode())) % 100;
                char tile = area.tileAt(x, y);
                if ((tile == 'd' || tile == 'D' || tile == 'F') && roll < 8) {
                    String[] options = {
                            "dungeon_prop_rune_pillar", "dungeon_prop_lantern_stand",
                            "dungeon_prop_chain_stand", "dungeon_prop_relic_crate",
                            "location_graveyard_skull_marker", "location_camp_crates"
                    };
                    area.props.add(new WorldProp(x, y, pick(options, roll + area.id.hashCode()), roll < 2 ? 44 : 36 + roll % 10));
                }
            }
        }
    }

    private void addHouseProps(MapArea area, String theme, int seed) {
        switch (theme) {
            case "blacksmith" -> {
                addFurniture(area, 3, 2, "interior_forge");
                addFurniture(area, 5, 2, "interior_anvil");
                addFurniture(area, area.width() - 7, 2, "interior_shop_counter");
                addFurniture(area, area.width() - 4, 2, "interior_crates");
                addFurniture(area, 4, 7, "interior_barrels");
                addFurniture(area, 11, 7, "interior_stove");
                addFurniture(area, area.width() - 5, area.height() - 4, "interior_hearth_pot");
            }
            case "carpenter" -> {
                addFurniture(area, 3, 2, "interior_carpenter_table");
                addFurniture(area, area.width() - 8, 2, "interior_crates");
                addFurniture(area, area.width() - 5, 2, "interior_barrels");
                addFurniture(area, 4, 7, "interior_bookshelf");
                addFurniture(area, area.width() - 8, 8, "interior_carpenter_table");
                addFurniture(area, area.width() - 5, area.height() - 4, "interior_crates");
            }
            case "tavern" -> {
                addFurniture(area, 2, 2, "interior_tavern_bar");
                addFurniture(area, 4, 2, "interior_tavern_bar");
                addFurniture(area, 6, 2, "interior_barrels");
                addFurniture(area, area.width() - 6, 3, "interior_bed_vertical");
                addFurniture(area, area.width() - 3, 3, "interior_oven");
                addTableSet(area, 7, 8);
                addTableSet(area, 15, 9);
                addTableSet(area, area.width() - 5, 10);
                addFurniture(area, 11, 13, "interior_hearth_pot");
            }
            case "shop" -> {
                addFurniture(area, 3, 2, "interior_shop_counter");
                addFurniture(area, 6, 2, "interior_bookshelf");
                addFurniture(area, area.width() - 7, 2, "interior_shop_counter");
                addFurniture(area, area.width() - 4, 2, "interior_crates");
                addTableSet(area, 7, 8);
                addFurniture(area, 16, 7, "interior_barrels");
                addFurniture(area, area.width() - 6, area.height() - 4, "interior_alchemy_station");
            }
            case "study" -> {
                addFurniture(area, 3, 2, "interior_bookshelf");
                addFurniture(area, 5, 2, "interior_bookshelf");
                addFurniture(area, area.width() - 7, 2, "interior_bookshelf");
                addFurniture(area, area.width() - 5, 2, "interior_bookshelf");
                addTableSet(area, 7, 8);
                addTableSet(area, 15, 8);
                addFurniture(area, area.width() - 6, area.height() - 4, "interior_alchemy_station");
            }
            default -> {
                addFurniture(area, 2, 2, "interior_bed_vertical");
                addFurniture(area, area.width() - 6, 2, "interior_bookshelf");
                addTableSet(area, area.width() / 2, 7);
                addFurniture(area, 5, area.height() - 5, Math.floorMod(seed, 2) == 0 ? "interior_stove" : "interior_hearth_pot");
            }
        }
        addInteriorAccentFurniture(area, theme, seed);
    }

    private void addInteriorAccentFurniture(MapArea area, String theme, int seed) {
        int width = area.width();
        int height = area.height();
        int roll = Math.floorMod(seed, 12);
        addFurniture(area, Math.max(3, width / 2 - 1), 1,
                roll % 2 == 0 ? "interior_wall_window_wide" : "interior_wall_plant_shelf");
        addFurniture(area, 3, 1, roll % 3 == 0 ? "interior_wall_sconce_lamp" : "interior_wall_flower_pot");
        addFurniture(area, width - 4, 1, roll % 3 == 1 ? "interior_wall_ivy_planter" : "interior_wall_sconce_lamp");
        switch (theme) {
            case "blacksmith" -> {
                addFurniture(area, width / 2 + 3, 1, "interior_wall_sconce_lamp");
                addFurniture(area, 7, height - 5, roll % 2 == 0 ? "interior_barrels" : "interior_crates");
                addFurniture(area, width - 5, 6, "interior_oven");
                addFurniture(area, width - 7, height - 4, "interior_herb_drying_rack");
                if (roll % 3 == 0) {
                    addFurniture(area, width - 4, height - 5, "interior_crates");
                }
            }
            case "carpenter" -> {
                addFurniture(area, width / 2 + 4, 1, "interior_wall_herb_rack");
                addFurniture(area, width - 6, height - 5, "interior_planting_pot");
                addFurniture(area, width - 9, height - 5, "interior_sprout_planter");
                addFurniture(area, 6, height - 5, "interior_side_table");
                addFurniture(area, 6, height - 5, "interior_seed_bowl");
                addFurniture(area, 9, height - 5, "interior_floor_sapling_pot");
            }
            case "tavern" -> {
                addFurniture(area, width / 2 + 4, 1, "interior_wall_window_small");
                addFurniture(area, width - 6, 6, "interior_barrels");
                addFurniture(area, 4, height - 5, roll % 2 == 0 ? "interior_stove" : "interior_oven");
                addFurniture(area, 8, height - 5, "interior_cooking_station");
                addFurniture(area, width - 8, height - 5, "interior_herb_pot");
                addFurniture(area, width - 5, height - 6, "interior_floor_flower_planter");
                if (roll % 3 != 1) {
                    addFurniture(area, width / 2 + 4, height - 4, "interior_crates");
                }
            }
            case "shop" -> {
                addFurniture(area, width / 2 + 4, 1, "interior_wall_herb_rack");
                addFurniture(area, width - 5, height - 5, "interior_crates");
                addFurniture(area, 3, height - 5, roll % 2 == 0 ? "interior_barrels" : "interior_bookshelf");
                addFurniture(area, 12, height - 5, "interior_flower_pot");
                addFurniture(area, 14, height - 5, "interior_herb_planter");
                addFurniture(area, width - 8, height - 5, "interior_floor_bushy_planter");
                if (roll % 3 == 2) {
                    addFurniture(area, width / 2 + 2, 8, "interior_shop_counter");
                }
            }
            case "study" -> {
                addFurniture(area, width / 2 + 4, 1, "interior_wall_crystal_ornament");
                addFurniture(area, width - 5, height - 5, "interior_hearth_pot");
                addFurniture(area, 12, height - 5, "interior_mortar_pestle");
                addFurniture(area, 3, height - 5, "interior_flower_vase");
                addFurniture(area, width - 8, height - 5, "interior_aquarium_table");
                if (roll % 2 == 0) {
                    addFurniture(area, 4, height - 5, "interior_bookshelf");
                }
                if (roll % 4 == 1) {
                    addTableSet(area, width / 2, height - 5);
                }
            }
            default -> {
                addFurniture(area, width / 2 + 3, 1, "interior_wall_window_small");
                addFurniture(area, width - 4, height - 5, roll % 2 == 0 ? "interior_crates" : "interior_barrels");
                addFurniture(area, width - 8, height - 5, "interior_planting_pot");
                addFurniture(area, 4, height / 2 + 1, roll % 2 == 0 ? "interior_herb_pot" : "interior_flower_pot");
                addFurniture(area, width - 5, height / 2 + 1, "interior_floor_leafy_plant");
                if (roll % 3 == 0) {
                    addFurniture(area, width - 5, height / 2 + 1, "interior_hearth_pot");
                }
                if (roll % 4 == 2) {
                    addFurniture(area, 3, height - 4, "interior_chair_north");
                }
            }
        }
    }

    private List<Npc> interiorNpcsFor(String mapId, String theme, int width, int height, int seed) {
        int x = Math.max(2, Math.min(width - 3, width / 2));
        int y = Math.max(3, Math.min(height - 4, height / 2 + 2));
        return switch (theme) {
            case "blacksmith" -> List.of(new Npc(mapId, "Blacksmith Garran", "npc_blacksmith", x, y, List.of(
                    "Keep clear of the coals. Iron remembers careless hands.",
                    "If the road has teeth, bring me the metal to answer it."
            ), null, "highwall"));
            case "carpenter" -> List.of(new Npc(mapId, "Carpenter Elian", "npc_citizen_man", x, y, List.of(
                    "Good wood tells you how it wants to hold weight.",
                    "Bring timber and hide, and a table like this can turn travel into preparedness."
            ), null, null));
            case "tavern" -> List.of(new Npc(mapId, "Bartender Senn", "npc_bartender", x, y, List.of(
                    "A warm room buys more courage than most speeches.",
                    "Travelers talk when the cups are full. Listen long enough and every road has a rumor."
            ), null, "riverside"));
            case "shop" -> List.of(new Npc(mapId, "Merchant Vale", "npc_merchant", x, y, List.of(
                    "Everything useful has a price. Everything priceless is usually trouble.",
                    "Look around. The shelves are better organized than the world outside."
            ), null, "riverside"));
            case "study" -> List.of(new Npc(mapId, "Archivist's Aide", "npc_citizen_woman", x, y, List.of(
                    "Please mind the shelves. Some of these records survived worse than weather.",
                    "A city is only stone unless someone remembers what happened inside it."
            ), null, null));
            default -> List.of(new Npc(mapId, Math.floorMod(seed, 2) == 0 ? "Townsperson" : "Householder",
                    Math.floorMod(seed, 2) == 0 ? "npc_citizen_man" : "npc_citizen_woman", x, y, List.of(
                    "Come in, but mind the floorboards.",
                    "Every house in Alderfall has a packed bag by the door now."
            ), null, null));
        };
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
                    String ground = locationGroundFor(patch, x, y, tile, seed);
                    if (ground != null) {
                        area.props.add(new WorldProp(x, y, ground, 52));
                    }
                    String entrance = adventureEntranceFor(patch.kind, tile);
                    if (entrance != null) {
                        area.props.add(new WorldProp(x, y, entrance, locationPropSize(entrance, seed)));
                        continue;
                    }
                    double centerPull = patch.centerPull(x, y);
                    int chance = switch (patch.kind) {
                        case "farmland" -> 24;
                        case "goblin_camp", "bandit_camp" -> 34;
                        case "graveyard", "crypt", "abandoned_castle" -> 32;
                        case "cave" -> 28;
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

    private String locationGroundFor(LocationPatch patch, int x, int y, char tile, int seed) {
        int dx = x - patch.cx;
        int dy = y - patch.cy;
        return switch (patch.kind) {
            case "farmland" -> ((patch.salt + patch.cy + tile) / 2 + seed) % 3 == 0
                    ? "location_farmland_wheat"
                    : "location_farmland_tilled";
            case "goblin_camp", "bandit_camp" -> seed % 4 == 0 ? "location_graveyard_path" : "location_graveyard_dirt";
            case "graveyard", "crypt", "abandoned_castle" -> {
                if (tile == 'd' || (Math.abs(dx) <= 1 && dy >= 0 && dy <= patch.ry)) {
                    yield "location_dungeon_approach_path";
                }
                yield seed % 100 < 42 + (int) Math.round(patch.centerPull(x, y) * 18.0)
                        ? "location_graveyard_dirt"
                        : null;
            }
            case "cave" -> {
                if (tile == 'd' || (Math.abs(dx) <= 1 && dy >= 0 && dy <= patch.ry)) {
                    yield "location_dungeon_approach_path";
                }
                yield seed % 100 < 32 + (int) Math.round(patch.centerPull(x, y) * 16.0)
                        ? "location_graveyard_dirt"
                        : null;
            }
            default -> null;
        };
    }

    private String adventureEntranceFor(String kind, char tile) {
        if (tile != 'd') {
            return null;
        }
        return switch (kind) {
            case "cave" -> "location_overgrown_cave_entrance";
            case "crypt", "graveyard" -> "location_dungeon_stair_entrance";
            case "goblin_camp" -> "location_goblin_hut";
            case "bandit_camp" -> "location_bandit_outpost";
            case "abandoned_castle" -> "location_castle_ruins";
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
                return seed % 3 == 0 ? "location_goblin_hut" : seed % 2 == 0 ? "location_camp_fire" : "location_camp_tent";
            }
            if (edge) {
                return "location_camp_palisade";
            }
            String[] options = {
                    "location_goblin_hut", "location_camp_tent", "location_camp_crates", "location_camp_fire",
                    "location_graveyard_skull_marker", "deco_imagen_road_camp"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("bandit_camp")) {
            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                return "location_bandit_outpost";
            }
            if (edge) {
                return "location_camp_palisade";
            }
            String[] options = {
                    "location_bandit_outpost", "location_camp_tent", "location_camp_crates",
                    "location_camp_fire", "deco_imagen_road_camp"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("cave")) {
            if (nearAnyTile(x, y, new char[]{'d'}, 1)) {
                return seed % 2 == 0 ? "location_ruin_standing_stones" : "deco_rocks";
            }
            String[] options = {
                    "deco_rocks", "location_ruin_standing_stones", "deco_imagen_forest_roots",
                    "location_graveyard_dead_stump", "location_graveyard_path"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("crypt")) {
            if (nearAnyTile(x, y, new char[]{'d'}, 1)) {
                return seed % 2 == 0 ? "location_dungeon_braziers" : "location_ruin_standing_stones";
            }
            if (edge) {
                return "location_graveyard_iron_fence";
            }
            String[] options = {
                    "location_crypt_sarcophagus", "location_graveyard_tombstones", "location_overgrown_landing",
                    "location_graveyard_skull_marker", "location_graveyard_dead_stump"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("abandoned_castle")) {
            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                return "location_castle_ruins";
            }
            if (edge) {
                return seed % 2 == 0 ? "location_graveyard_iron_fence" : "deco_rocks";
            }
            String[] options = {
                    "location_castle_ruins", "location_crypt_sarcophagus",
                    "location_graveyard_tombstones", "deco_imagen_flat_stone_stack"
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
            case "location_dungeon_approach_path" -> 50;
            case "location_farmland_fence", "location_graveyard_iron_fence" -> 46;
            case "location_camp_tent", "location_camp_palisade", "location_farmland_scarecrow",
                    "location_graveyard_tombstones" -> 54;
            case "location_ruin_standing_stones", "location_overgrown_landing" -> 58;
            case "location_crypt_sarcophagus" -> 56;
            case "location_goblin_hut" -> 62;
            case "location_bandit_outpost" -> 64;
            case "location_cave_entrance", "location_overgrown_cave_entrance" -> 68;
            case "location_crypt_entrance", "location_dungeon_stair_entrance" -> 72;
            case "location_dungeon_braziers" -> 66;
            case "location_castle_ruins" -> 74;
            default -> base;
        };
    }

    private String decorationFor(MapArea area, char tile, int x, int y, int roll, int patchSeed) {
        return switch (tile) {
            case 'g' -> grassDecorationFor(area, x, y, roll, patchSeed);
            case 'f' -> forestDecorationFor(area, x, y, roll, patchSeed);
            case 's' -> desertDecorationFor(area, x, y, roll, patchSeed);
            case 'n' -> tundraDecorationFor(area, x, y, roll, patchSeed);
            case 'v' -> marshDecorationFor(area, x, y, roll, patchSeed);
            case 'b' -> badlandsDecorationFor(area, x, y, roll, patchSeed);
            case 'P' -> beachDecorationFor(area, x, y, roll, patchSeed);
            case 'q', 'm' -> mountainDecorationFor(area, x, y, roll, patchSeed);
            case 'r' -> roadDecorationFor(area, x, y, roll, patchSeed);
            default -> pick(new String[]{"deco_bush", "deco_flowers"}, roll);
        };
    }

    private String grassDecorationFor(MapArea area, int x, int y, int roll, int patchSeed) {
        boolean nearRoad = distanceToAny(area, x, y, 2, new char[]{'r', 'q'}) <= 1;
        boolean nearWater = distanceToAny(area, x, y, 3, new char[]{'w'}) <= 2;
        return weightedDecoration(roll, patchSeed, new DecorationOption[]{
                option("deco_grass_clump", 36),
                option("deco_flowers", 18),
                option("deco_grass_wildflowers", nearRoad ? 26 : 16),
                option("deco_grass_herb_patch", nearRoad ? 16 : 9),
                option("deco_bush", nearRoad ? 8 : 18),
                option("deco_imagen_meadow_blooms", 8, 420),
                option("deco_grass_stone_stack", 6, 260),
                option("deco_imagen_grass_pond", nearWater ? 5 : 1, nearWater ? 160 : 35)
        });
    }

    private String forestDecorationFor(MapArea area, int x, int y, int roll, int patchSeed) {
        boolean forestCore = naturalNeighborCount(area, x, y, 'f') >= 16;
        boolean edge = distanceToDifferentNatural(area, x, y, 'f', 2) <= 1;
        return weightedDecoration(roll, patchSeed, new DecorationOption[]{
                option("deco_tree_oak", forestCore ? 28 : 12),
                option("deco_tree_round", forestCore ? 28 : 12),
                option("deco_tree_pine", forestCore ? 18 : 14),
                option("deco_tree_blue_pine", forestCore ? 10 : 8),
                option("deco_tree_young", edge ? 18 : 9),
                option("deco_forest_fern", edge ? 22 : 10),
                option("deco_forest_moss_rock", edge ? 12 : 7),
                option("deco_forest_log", 8, 520),
                option("deco_forest_mushrooms", 9, 520),
                option("deco_forest_blue_mushroom_ring", 4, 180),
                option("deco_forest_ancient_roots", forestCore ? 5 : 2, 120),
                option("deco_forest_shrine_stone", 2, 55),
                option("deco_forest_fairy_pool", 1, 35)
        });
    }

    private String desertDecorationFor(MapArea area, int x, int y, int roll, int patchSeed) {
        boolean nearRock = distanceToAny(area, x, y, 3, new char[]{'b', 'm', 'q'}) <= 2;
        boolean nearWater = distanceToAny(area, x, y, 4, new char[]{'w'}) <= 3;
        return weightedDecoration(roll, patchSeed, new DecorationOption[]{
                option("deco_dry_grass", 34),
                option("deco_cactus", nearWater ? 10 : 24),
                option("deco_desert_blooming_cactus", nearWater ? 12 : 7, 360),
                option("deco_desert_rocks", nearRock ? 28 : 14),
                option("deco_desert_sun_bleached_bones", nearRock ? 6 : 3, 170),
                option("deco_desert_jar_cache", nearWater ? 4 : 2, 80)
        });
    }

    private String tundraDecorationFor(MapArea area, int x, int y, int roll, int patchSeed) {
        boolean nearForest = distanceToAny(area, x, y, 4, new char[]{'f'}) <= 3;
        boolean nearMountain = distanceToAny(area, x, y, 4, new char[]{'m', 'q'}) <= 3;
        return weightedDecoration(roll, patchSeed, new DecorationOption[]{
                option("deco_snow_mound", 28),
                option("deco_tundra_frost_bush", 26),
                option("deco_snow_pine", nearForest ? 28 : 12),
                option("deco_tundra_rocks", nearMountain ? 26 : 12),
                option("deco_tundra_ice_crystals", nearMountain ? 8 : 5, 260),
                option("deco_tundra_rune_stone", 2, 65)
        });
    }

    private String marshDecorationFor(MapArea area, int x, int y, int roll, int patchSeed) {
        boolean nearWater = distanceToAny(area, x, y, 3, new char[]{'w'}) <= 2;
        boolean edge = distanceToDifferentNatural(area, x, y, 'v', 2) <= 1;
        return weightedDecoration(roll, patchSeed, new DecorationOption[]{
                option("deco_reeds", nearWater ? 36 : 16),
                option("deco_bog_grass", nearWater ? 24 : 30),
                option("deco_mushrooms", edge ? 18 : 9),
                option("deco_marsh_lily_pool", nearWater ? 10 : 2, nearWater ? 360 : 70),
                option("deco_marsh_bubble_pool", nearWater ? 8 : 2, nearWater ? 260 : 55),
                option("deco_marsh_firefly_reeds", nearWater ? 6 : 3, 170),
                option("deco_marsh_twisted_roots", edge ? 6 : 2, 120)
        });
    }

    private String badlandsDecorationFor(MapArea area, int x, int y, int roll, int patchSeed) {
        boolean ridge = distanceToAny(area, x, y, 3, new char[]{'m', 'q', 's'}) <= 2;
        return weightedDecoration(roll, patchSeed, new DecorationOption[]{
                option("deco_badlands_rocks", ridge ? 36 : 24),
                option("deco_badlands_dry_grass", 28),
                option("deco_badlands_red_spire", ridge ? 14 : 6, 360),
                option("deco_badlands_skull_marker", 3, 95),
                option("deco_badlands_totem_stones", 2, 55)
        });
    }

    private String beachDecorationFor(MapArea area, int x, int y, int roll, int patchSeed) {
        boolean nearWater = distanceToAny(area, x, y, 3, new char[]{'w', '~'}) <= 2;
        boolean roomy = naturalNeighborCount(area, x, y, 'P') >= 10;
        return weightedDecoration(roll, patchSeed, new DecorationOption[]{
                option("deco_beach_grass", nearWater ? 18 : 30),
                option("deco_beach_shells", nearWater ? 24 : 10),
                option("deco_beach_coconuts", 12),
                option("deco_beach_driftwood", nearWater ? 18 : 7),
                option("deco_beach_palm", roomy ? 12 : 4, 420),
                option("deco_beach_palm_cluster", roomy ? 5 : 1, 180),
                option("village_prop_net_drying_rack", nearWater ? 2 : 0, 70),
                option("village_prop_palm_shade", roomy ? 2 : 0, 60)
        });
    }

    private String mountainDecorationFor(MapArea area, int x, int y, int roll, int patchSeed) {
        boolean pass = area.tileAt(x, y) == 'q' || distanceToAny(area, x, y, 2, new char[]{'q', 'r'}) <= 1;
        boolean snow = distanceToAny(area, x, y, 3, new char[]{'n'}) <= 2;
        return weightedDecoration(roll, patchSeed, new DecorationOption[]{
                option("deco_mountain_rocks", pass ? 22 : 36),
                option("deco_rocks", 18),
                option("deco_mountain_cairn", pass ? 20 : 6, pass ? 520 : 180),
                option("deco_mountain_pass_way_cairn", pass ? 14 : 3, pass ? 440 : 90),
                option("deco_mountain_scrub_pine", snow ? 10 : 18),
                option("deco_mountain_crystal_cluster", 3, 95),
                option("deco_mountain_spring_pool", 2, 55),
                option("deco_mountain_pass_snowmelt_pool", snow ? 3 : 1, snow ? 120 : 35)
        });
    }

    private String roadDecorationFor(MapArea area, int x, int y, int roll, int patchSeed) {
        boolean nearSettlement = distanceToAny(area, x, y, 5, new char[]{'c', 'u'}) <= 4;
        return weightedDecoration(roll, patchSeed, new DecorationOption[]{
                option("deco_road_signpost", nearSettlement ? 28 : 18),
                option("deco_road_milestone", 18),
                option("deco_imagen_signpost", 8, 360),
                option("deco_imagen_milestone", 7, 320),
                option("deco_imagen_road_camp", 2, 60)
        });
    }

    private String lowDecorationFor(MapArea area, char tile, int x, int y, int roll, int patchSeed) {
        return switch (tile) {
            case 'f' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_forest_mushrooms", 16, 560), option("deco_forest_moss_rock", 14),
                    option("deco_forest_log", 10, 520), option("deco_forest_ancient_roots", 4, 140)
            });
            case 'g' -> grassDecorationFor(area, x, y, roll, patchSeed);
            case 'v' -> marshDecorationFor(area, x, y, roll, patchSeed);
            case 'P' -> pick(new String[]{"deco_beach_grass", "deco_beach_shells", "deco_beach_coconuts", "deco_beach_driftwood"}, roll + patchSeed);
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

    private boolean isBeachFlora(String asset) {
        return asset.equals("deco_beach_grass")
                || asset.equals("deco_beach_shells")
                || asset.equals("deco_beach_coconuts");
    }

    private boolean isSmallNatural(String asset) {
        return asset.equals("deco_dry_grass")
                || asset.equals("deco_badlands_dry_grass")
                || asset.equals("deco_tundra_frost_bush")
                || isBeachFlora(asset)
                || asset.equals("deco_beach_driftwood");
    }

    private boolean isBeachPalm(String asset) {
        return asset.equals("deco_beach_palm")
                || asset.equals("deco_beach_palm_cluster");
    }

    private boolean isTallProp(String asset) {
        return isForestTree(asset)
                || isBeachPalm(asset)
                || asset.equals("deco_snow_pine")
                || asset.equals("deco_mountain_scrub_pine")
                || asset.equals("deco_cactus")
                || asset.equals("deco_desert_blooming_cactus")
                || asset.equals("deco_badlands_red_spire")
                || asset.equals("deco_forest_fern");
    }

    private DecorationOption option(String asset, int weight) {
        return new DecorationOption(asset, weight, 1000);
    }

    private DecorationOption option(String asset, int weight, int rarity) {
        return new DecorationOption(asset, weight, rarity);
    }

    private String weightedDecoration(int roll, int patchSeed, DecorationOption[] options) {
        int rarityRoll = Math.floorMod(patchSeed / 13 + roll * 29, 1000);
        int total = 0;
        for (DecorationOption option : options) {
            if (option.weight > 0 && rarityRoll < option.rarity) {
                total += option.weight;
            }
        }
        if (total <= 0) {
            for (DecorationOption option : options) {
                if (option.weight > 0) {
                    return option.asset;
                }
            }
            return "deco_grass_clump";
        }
        int pick = Math.floorMod(roll * 73 + patchSeed / 5, total);
        for (DecorationOption option : options) {
            if (option.weight <= 0 || rarityRoll >= option.rarity) {
                continue;
            }
            pick -= option.weight;
            if (pick < 0) {
                return option.asset;
            }
        }
        return options[0].asset;
    }

    private int[] pairedDecorationOffset(MapArea area, int x, int y, char tile, int roll) {
        int[][] offsets = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
        };
        int start = Math.floorMod(roll / 7, offsets.length);
        for (int i = 0; i < offsets.length; i++) {
            int[] offset = offsets[(start + i) % offsets.length];
            if (area.tileAt(x + offset[0], y + offset[1]) == tile) {
                return offset;
            }
        }
        return offsets[start];
    }

    private int distanceToAny(MapArea area, int x, int y, int radius, char[] targets) {
        int best = radius + 1;
        for (int oy = -radius; oy <= radius; oy++) {
            for (int ox = -radius; ox <= radius; ox++) {
                int distance = Math.abs(ox) + Math.abs(oy);
                if (distance >= best) {
                    continue;
                }
                char tile = area.tileAt(x + ox, y + oy);
                if (contains(targets, tile)) {
                    best = distance;
                }
            }
        }
        return best;
    }

    private int distanceToDifferentNatural(MapArea area, int x, int y, char tile, int radius) {
        int best = radius + 1;
        for (int oy = -radius; oy <= radius; oy++) {
            for (int ox = -radius; ox <= radius; ox++) {
                if (ox == 0 && oy == 0) {
                    continue;
                }
                int distance = Math.abs(ox) + Math.abs(oy);
                if (distance >= best) {
                    continue;
                }
                char other = area.tileAt(x + ox, y + oy);
                if (isNaturalBiome(other) && other != tile) {
                    best = distance;
                }
            }
        }
        return best;
    }

    private boolean isNaturalBiome(char tile) {
        return tile == 'g' || tile == 'f' || tile == 's' || tile == 'n' || tile == 'v' || tile == 'b'
                || tile == 'P' || tile == '~' || tile == 'm' || tile == 'q';
    }

    private String pick(String[] options, int seed) {
        return options[Math.floorMod(seed, options.length)];
    }

    private record DecorationOption(String asset, int weight, int rarity) {
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
        seedSmallLakes(seedSalt, land);
        seedRivers(seedSalt, land);

        raiseLandOval(83, 62, 11, 8, 'n', land);
        raiseLandPath(List.of(new TilePoint(83, 62), new TilePoint(78, 83), new TilePoint(82, 105)), 2, 'n', land);
        raiseLandOval(OAKHAVEN_POSITION.x(), OAKHAVEN_POSITION.y(), 13, 9, 'g', land);
        raiseLandOval(56, 156, 15, 10, 'g', land);
        raiseLandOval(118, 76, 12, 9, 'n', land);
        raiseLandOval(268, 138, 15, 12, 'v', land);
        raiseLandOval(126, 269, 18, 10, 's', land);
        raiseLandOval(205, 254, 15, 11, 'b', land);
        raiseLandPath(List.of(new TilePoint(240, 153), new TilePoint(254, 145), new TilePoint(268, 138)), 2, 'v', land);
        raiseLandPath(List.of(new TilePoint(102, 245), new TilePoint(90, 257), new TilePoint(78, 266)), 2, 's', land);
        seedBeachPatches(seedSalt, land);

        roadPath(List.of(new TilePoint(82, 105), new TilePoint(94, 123), START_POSITION, OAKHAVEN_POSITION, new TilePoint(132, 153), new TilePoint(152, 145)));
        roadPath(List.of(new TilePoint(152, 145), new TilePoint(169, 128), new TilePoint(189, 100), new TilePoint(205, 78)));
        roadPath(List.of(new TilePoint(205, 78), new TilePoint(200, 67), new TilePoint(196, 62)));
        roadPath(List.of(new TilePoint(152, 145), new TilePoint(176, 153), new TilePoint(202, 171), new TilePoint(228, 185), new TilePoint(255, 177)));
        roadPath(List.of(new TilePoint(228, 185), new TilePoint(208, 206), new TilePoint(194, 235), new TilePoint(150, 230)));
        roadPath(List.of(START_POSITION, new TilePoint(99, 181), new TilePoint(72, 218), new TilePoint(102, 245)));
        roadPath(List.of(new TilePoint(82, 105), new TilePoint(78, 83), new TilePoint(83, 62)));
        roadPath(List.of(START_POSITION, new TilePoint(128, 181), new TilePoint(145, 207), new TilePoint(150, 230)));
        roadPath(List.of(new TilePoint(240, 153), new TilePoint(228, 185)));
        roadPath(List.of(new TilePoint(82, 105), new TilePoint(63, 131), new TilePoint(56, 156), START_POSITION));
        roadPath(List.of(START_POSITION, new TilePoint(123, 140), new TilePoint(131, 121), new TilePoint(152, 145)));
        roadPath(List.of(new TilePoint(205, 78), new TilePoint(214, 92), new TilePoint(225, 108), new TilePoint(240, 153)));
        roadPath(List.of(new TilePoint(83, 62), new TilePoint(101, 70), new TilePoint(118, 76), new TilePoint(131, 121)));
        roadPath(List.of(new TilePoint(152, 145), new TilePoint(162, 165), new TilePoint(170, 180), new TilePoint(150, 230)));
        roadPath(List.of(START_POSITION, new TilePoint(126, 162), new TilePoint(138, 165), new TilePoint(170, 180)));
        roadPath(List.of(new TilePoint(240, 153), new TilePoint(254, 145), new TilePoint(268, 138)));
        roadPath(List.of(new TilePoint(228, 185), new TilePoint(244, 196), new TilePoint(259, 205)));
        roadPath(List.of(new TilePoint(102, 245), new TilePoint(90, 257), new TilePoint(78, 266)));
        roadPath(List.of(new TilePoint(150, 230), new TilePoint(137, 249), new TilePoint(126, 269)));
        roadPath(List.of(new TilePoint(150, 230), new TilePoint(177, 242), new TilePoint(205, 254)));

        stampSettlement(82, 105, 'c', "Riverside Gate");
        stampSettlement(152, 145, 'c', "Archive Gate");
        stampSettlement(205, 78, 'c', "Highwall Gate");
        stampSettlement(228, 185, 'c', "Belltower Gate");
        stampSettlement(150, 230, 'c', "Sanctum Gate");
        stampSettlement(131, 121, 'c', "Briarbridge");
        stampSettlement(225, 108, 'c', "Ironvale");
        stampSettlement(170, 180, 'c', "Moonspire");
        stampSettlement(259, 205, 'c', "Reedwatch");
        stampSettlement(126, 269, 'c', "Embermarket");

        stampSettlement(START_POSITION.x(), START_POSITION.y(), 'u', "Oathstead Camp");
        stampSettlement(OAKHAVEN_POSITION.x(), OAKHAVEN_POSITION.y(), 'u', "Oakhaven");
        stampSettlement(83, 62, 'u', "Snowrest");
        stampSettlement(102, 245, 'u', "Dunewick");
        stampSettlement(240, 153, 'u', "Mireford");
        stampSettlement(56, 156, 'u', "Foxbarrow");
        stampSettlement(118, 76, 'u', "Pineward");
        stampSettlement(138, 165, 'u', "Elderford");
        stampSettlement(268, 138, 'u', "Glimmerfen");
        stampSettlement(78, 266, 'u', "Sunmere");
        stampSettlement(205, 254, 'u', "Redcairn");

        addLocationPatches(seedSalt);
        addAdventureSites(seedSalt);
        ensureOverworldTraversable();
    }

    private void addAdventureSites(int seedSalt) {
        addAdventureSite("crypt", "dungeon_stonegate_1", "Stonegate Crypt",
                196, 62, 1, 6, 5, 910, new TilePoint(205, 78));
        addAdventureSite("cave", "dungeon_miredepth_1", "Miredepth Cave",
                255, 177, 1, 6, 5, 920, new TilePoint(228, 185));
        addAdventureSite("cave", "dungeon_frosthollow_1", "Frosthollow Cave",
                72, 218, 1, 5, 5, 930, new TilePoint(102, 245));
        addAdventureSite("abandoned_castle", "dungeon_blackvault_1", "Blackvault Ruins",
                194, 235, 1, 7, 6, 940, new TilePoint(150, 230));

        TilePoint goblinCamp = chooseLocationCenter(
                980 + seedSalt, 176, 112, 38, 30,
                new char[]{'g', 'f', 's', 'b'}, new char[]{'r', 'q'}, 7, 15
        );
        if (goblinCamp != null) {
            addAdventureSite("goblin_camp", "dungeon_redcap_camp_1", "Redcap Goblin Camp",
                    goblinCamp.x(), goblinCamp.y(), 1, 5, 5, 981, new TilePoint(152, 145));
        }

        TilePoint banditCamp = chooseLocationCenter(
                990 + seedSalt, 214, 218, 34, 28,
                new char[]{'g', 'f', 's', 'b', 'v'}, new char[]{'r', 'q'}, 7, 15
        );
        if (banditCamp != null) {
            addAdventureSite("bandit_camp", "dungeon_crowhook_outpost_1", "Crowhook Bandit Camp",
                    banditCamp.x(), banditCamp.y(), 1, 5, 5, 991, new TilePoint(228, 185));
        }
    }

    private void addAdventureSite(String kind, String id, String label, int x, int y, int depth,
                                  int rx, int ry, int salt, TilePoint roadAnchor) {
        addLocationPatch(kind, x, y, rx, ry, salt);
        stampAdventureSite(x, y, label);
        adventureSites.add(new AdventureSite(kind, id, label, x, y, depth));
        if (roadAnchor != null) {
            roadPath(List.of(roadAnchor, new TilePoint(x, y)));
        }
    }

    private void addLocationPatches(int seedSalt) {
        int[][] farmAnchors = {
                {112, 158}, {82, 105}, {152, 145}, {102, 245}, {56, 156}, {138, 165}, {78, 266}, {268, 138}
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
                {96, 110}, {186, 115}, {221, 201}, {132, 222}, {210, 244}, {64, 171}, {230, 112}, {257, 213}
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
                case "bandit_camp" -> "Bandit Camp";
                case "crypt" -> "Crypt";
                case "cave" -> "Cave";
                case "abandoned_castle" -> "Abandoned Castle";
                default -> "Landmark";
            };
        }
    }

    private record AdventureSite(String kind, String id, String label, int x, int y, int depth) {
    }

    public record LocationSite(String kind, String label, int x, int y) {
    }

    public record Kingdom(String id, String name, String seat, int centerX, int centerY, String description, int colorRgb) {
    }

    public record SettlementSite(String id, String label, String kind, int x, int y, String kingdomId) {
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
        return tile == 'w' || tile == '~' || tile == 'B' || tile == 'r' || tile == 'c' || tile == 'u' || tile == 'd';
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

    private void seedBeachPatches(int seedSalt, boolean[][] land) {
        int[][] specs = {
                {48, 139, 4, 3, 501}, {137, 91, 4, 3, 503}, {270, 163, 5, 4, 505},
                {228, 261, 5, 3, 507}, {69, 273, 5, 3, 509}, {178, 34, 4, 3, 511},
                {62, 118, 3, 2, 521}, {171, 116, 3, 2, 523}, {221, 128, 3, 2, 529},
                {83, 185, 3, 2, 531}, {174, 205, 3, 2, 533}, {218, 224, 3, 2, 541},
                {65, 154, 3, 2, 547}, {151, 225, 3, 2, 557}
        };
        for (int[] spec : specs) {
            int anchorX = shifted(spec[0], 4, spec[4], seedSalt);
            int anchorY = shifted(spec[1], 4, spec[4] + 1, seedSalt);
            TilePoint shoreline = nearestShoreline(anchorX, anchorY, 18, land);
            if (shoreline == null) {
                continue;
            }
            int salt = seedSalt + spec[4];
            int rx = spec[2] + hash(shoreline.x(), shoreline.y(), salt) % 2;
            int ry = spec[3] + hash(shoreline.y(), shoreline.x(), salt + 9) % 2;
            paintBeachPatch(shoreline.x(), shoreline.y(), rx, ry, salt, land);
        }
    }

    private TilePoint nearestShoreline(int cx, int cy, int radius, boolean[][] land) {
        TilePoint best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int y = Math.max(1, cy - radius); y <= Math.min(ROWS - 2, cy + radius); y++) {
            for (int x = Math.max(1, cx - radius); x <= Math.min(COLS - 2, cx + radius); x++) {
                if (!land[y][x] || isProtectedOverworldTile(tiles[y][x]) || tiles[y][x] == 'm' || tiles[y][x] == 'q') {
                    continue;
                }
                if (!nearTile(x, y, 2, 'w')) {
                    continue;
                }
                int distance = Math.abs(x - cx) + Math.abs(y - cy);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new TilePoint(x, y);
                }
            }
        }
        return best;
    }

    private void paintBeachPatch(int cx, int cy, int rx, int ry, int salt, boolean[][] land) {
        for (int y = Math.max(1, cy - ry - 3); y <= Math.min(ROWS - 2, cy + ry + 3); y++) {
            for (int x = Math.max(1, cx - rx - 3); x <= Math.min(COLS - 2, cx + rx + 3); x++) {
                if (!land[y][x] || isProtectedOverworldTile(tiles[y][x]) || tiles[y][x] == 'm' || tiles[y][x] == 'q') {
                    continue;
                }
                if (organicMetric(x, y, cx, cy, rx, ry, salt) <= 0.82 && nearTile(x, y, 3, 'w')) {
                    tiles[y][x] = 'P';
                }
            }
        }
        for (int y = Math.max(1, cy - ry - 4); y <= Math.min(ROWS - 2, cy + ry + 4); y++) {
            for (int x = Math.max(1, cx - rx - 4); x <= Math.min(COLS - 2, cx + rx + 4); x++) {
                if (tiles[y][x] != 'w' || !nearTile(x, y, 1, 'P')) {
                    continue;
                }
                if (organicMetric(x, y, cx, cy, rx + 1, ry + 1, salt + 19) <= 1.08
                        && Math.floorMod(hash(x, y, salt + 31), 100) < 72) {
                    tiles[y][x] = '~';
                }
            }
        }
    }

    private boolean nearTile(int x, int y, int radius, char target) {
        for (int oy = -radius; oy <= radius; oy++) {
            for (int ox = -radius; ox <= radius; ox++) {
                if (Math.abs(ox) + Math.abs(oy) > radius) {
                    continue;
                }
                int tx = x + ox;
                int ty = y + oy;
                if (tx >= 0 && ty >= 0 && tx < COLS && ty < ROWS && tiles[ty][tx] == target) {
                    return true;
                }
            }
        }
        return false;
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

    private void seedSmallLakes(int seedSalt, boolean[][] land) {
        int[][] lakeSpecs = {
                {62, 118, 7, 5, 301}, {116, 76, 6, 5, 307}, {171, 116, 7, 4, 311},
                {221, 128, 8, 5, 313}, {249, 105, 6, 4, 317}, {83, 185, 6, 5, 331},
                {129, 205, 5, 4, 337}, {174, 205, 7, 5, 347}, {218, 224, 6, 4, 349},
                {111, 265, 8, 5, 353}, {54, 235, 5, 4, 359}, {202, 55, 6, 4, 367}
        };
        for (int[] spec : lakeSpecs) {
            int salt = seedSalt + spec[4];
            int cx = shifted(spec[0], 5, spec[4], seedSalt);
            int cy = shifted(spec[1], 5, spec[4] + 1, seedSalt);
            int rx = spec[2] + hash(cx, cy, salt) % 3;
            int ry = spec[3] + hash(cy, cx, salt + 5) % 2;
            paintWaterBody(cx, cy, rx, ry, salt, land);
        }
    }

    private void seedRivers(int seedSalt, boolean[][] land) {
        paintRiverPath(List.of(
                new TilePoint(134, 91), new TilePoint(120, 108), new TilePoint(101, 124),
                new TilePoint(79, 136), new TilePoint(52, 139)
        ), 1, seedSalt + 401, land);
        paintRiverPath(List.of(
                new TilePoint(201, 96), new TilePoint(214, 117), new TilePoint(229, 139),
                new TilePoint(245, 154), new TilePoint(274, 162)
        ), 1, seedSalt + 409, land);
        paintRiverPath(List.of(
                new TilePoint(162, 146), new TilePoint(169, 173), new TilePoint(164, 199),
                new TilePoint(151, 225), new TilePoint(142, 252)
        ), 1, seedSalt + 419, land);
        paintRiverPath(List.of(
                new TilePoint(65, 154), new TilePoint(75, 174), new TilePoint(82, 196),
                new TilePoint(72, 218), new TilePoint(49, 230)
        ), 1, seedSalt + 431, land);
    }

    private void paintRiverPath(List<TilePoint> points, int width, int salt, boolean[][] land) {
        int x = points.get(0).x();
        int y = points.get(0).y();
        markWater(x, y, width, land);
        for (TilePoint target : points.subList(1, points.size())) {
            int guard = 0;
            while ((x != target.x() || y != target.y()) && guard++ < 360) {
                int sx = Integer.compare(target.x(), x);
                int sy = Integer.compare(target.y(), y);
                if (sx != 0 && (sy == 0 || Math.floorMod(hash(x, y, salt + guard), 5) < 3)) {
                    x += sx;
                } else if (sy != 0) {
                    y += sy;
                }
                int bend = Math.floorMod(hash(x, y, salt + guard * 17), 11);
                if (bend == 0 && sy != 0) {
                    markWater(x + 1, y, width, land);
                } else if (bend == 1 && sx != 0) {
                    markWater(x, y + 1, width, land);
                }
                markWater(x, y, width, land);
            }
        }
    }

    private void markWater(int x, int y, int width, boolean[][] land) {
        for (int oy = -width; oy <= width; oy++) {
            for (int ox = -width; ox <= width; ox++) {
                if (Math.abs(ox) + Math.abs(oy) > width + 1) {
                    continue;
                }
                int tx = x + ox;
                int ty = y + oy;
                if (tx >= 0 && ty >= 0 && tx < COLS && ty < ROWS && land[ty][tx] && !isProtectedOverworldTile(tiles[ty][tx])) {
                    tiles[ty][tx] = 'w';
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
        if (tiles[y][x] == 'w' || tiles[y][x] == '~' || tiles[y][x] == 'B') {
            tiles[y][x] = 'B';
        } else if (tiles[y][x] == 'm') {
            tiles[y][x] = 'q';
        } else if (tiles[y][x] != 'c' && tiles[y][x] != 'u' && tiles[y][x] != 'd') {
            tiles[y][x] = 'r';
        }
    }

    private void ensureOverworldTraversable() {
        List<TilePoint> anchors = new ArrayList<>(List.of(
                START_POSITION,
                new TilePoint(82, 105), new TilePoint(152, 145), new TilePoint(205, 78),
                new TilePoint(228, 185), new TilePoint(150, 230), new TilePoint(83, 62),
                new TilePoint(102, 245), new TilePoint(240, 153),
                new TilePoint(131, 121), new TilePoint(225, 108), new TilePoint(170, 180),
                new TilePoint(259, 205), new TilePoint(126, 269), new TilePoint(56, 156),
                new TilePoint(118, 76), new TilePoint(138, 165), new TilePoint(268, 138),
                new TilePoint(78, 266), new TilePoint(205, 254)
        ));
        for (AdventureSite site : adventureSites) {
            anchors.add(new TilePoint(site.x, site.y));
        }
        for (TilePoint target : anchors) {
            if (!isReachable(START_POSITION, target)) {
                roadPath(List.of(START_POSITION, target));
            }
        }
    }

    private boolean isReachable(TilePoint start, TilePoint target) {
        if (!Terrain.passable(rawOverworldTileAt(start.x(), start.y())) || !Terrain.passable(rawOverworldTileAt(target.x(), target.y()))) {
            return false;
        }
        boolean[][] seen = new boolean[ROWS][COLS];
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        queue.add(start);
        seen[start.y()][start.x()] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            TilePoint current = queue.removeFirst();
            if (current.equals(target)) {
                return true;
            }
            for (int[] dir : dirs) {
                int nx = current.x() + dir[0];
                int ny = current.y() + dir[1];
                if (nx < 0 || ny < 0 || nx >= COLS || ny >= ROWS || seen[ny][nx] || !Terrain.passable(rawOverworldTileAt(nx, ny))) {
                    continue;
                }
                seen[ny][nx] = true;
                queue.addLast(new TilePoint(nx, ny));
            }
        }
        return false;
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

    private void stampAdventureSite(int cx, int cy, String name) {
        for (int oy = -2; oy <= 2; oy++) {
            for (int ox = -2; ox <= 2; ox++) {
                int x = cx + ox;
                int y = cy + oy;
                if (x >= 0 && y >= 0 && x < COLS && y < ROWS
                        && Math.abs(ox) <= 1 && oy > 0) {
                    tiles[y][x] = 'r';
                }
            }
        }
        tiles[cy][cx] = 'd';
        landmarks.put(new TilePoint(cx, cy), name);
    }
}
