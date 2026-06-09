package com.alderfall.game.map;

import com.alderfall.game.CityBuilding;
import com.alderfall.game.GameData;
import com.alderfall.game.Npc;
import com.alderfall.game.Terrain;
import com.alderfall.game.TilePoint;
import com.alderfall.game.VillageManager;
import com.alderfall.game.WorldProp;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WorldMap {
    public static final int COLS = 300;
    public static final int ROWS = 300;
    public static final TilePoint START_POSITION = new TilePoint(112, 158);
    public static final TilePoint OAKHAVEN_POSITION = new TilePoint(124, 151);
    public static final String OVERWORLD_ID = "overworld";
    public static final String PLAYER_VILLAGE_ID = "village_oathstead_camp";
    private static final String PLAYER_VILLAGE_EXIT_MARKER_ASSET = "player_village_exit_marker";
    private static final int PLAYER_VILLAGE_EXIT_MARKER_SIZE = 38;
    private static final int MAX_CONTINUOUS_BRIDGE_SPAN = 8;
    private static final int CITY_COBBLESTONE_ROAD_RADIUS = 38;
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
    private final Set<String> removedPlayerVillageProps = new HashSet<>();
    private final Map<String, List<WorldProp>> playerInteriorProps = new HashMap<>();
    private final Map<String, Map<TilePoint, Character>> playerInteriorTiles = new HashMap<>();
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
        return area == null ? null : area.propAt(x, y);
    }

    public List<Npc> npcs(String mapId) {
        return interiorNpcs.getOrDefault(mapId, List.of());
    }

    public TilePoint npcHome(Npc npc) {
        if (npc == null) {
            return new TilePoint(0, 0);
        }
        if (!isSettlementMapKind(npc.mapId())) {
            return closestPassableNear(npc.mapId(), npc.x(), npc.y(), npc.x(), npc.y(), 5, npcStableHash(npc));
        }
        CityBuilding building = homeBuildingForNpc(npc);
        if (building != null) {
            return buildingNpcDoorSpot(npc.mapId(), building, npcStableHash(npc));
        }
        return closestPassableNear(npc.mapId(), npc.x(), npc.y(), npc.x(), npc.y(), 5, npcStableHash(npc));
    }

    public TilePoint npcWorkTarget(Npc npc, String mapId) {
        if (npc == null || mapId == null || !mapId.equals(npc.mapId()) || !isSettlementMapKind(mapId)) {
            return null;
        }
        CityBuilding building = workBuildingForNpc(npc);
        return building == null ? null : buildingNpcDoorSpot(mapId, building, npcStableHash(npc) + 31);
    }

    public List<WorldProp> propsAt(String mapId, int x, int y) {
        MapArea area = area(mapId);
        return area == null ? List.of() : area.propsAt(x, y);
    }

    public List<WorldProp> propsInBounds(String mapId, int minX, int minY, int maxX, int maxY) {
        MapArea area = area(mapId);
        return area == null ? List.of() : area.propsInBounds(minX, minY, maxX, maxY);
    }

    public List<WorldProp> propsInTileOrder(String mapId, int minX, int minY, int maxX, int maxY) {
        MapArea area = area(mapId);
        return area == null ? List.of() : area.propsInTileOrder(minX, minY, maxX, maxY);
    }

    public boolean removePropAt(String mapId, int x, int y, String asset) {
        MapArea area = area(mapId);
        if (area == null) {
            return false;
        }
        List<WorldProp> matches = area.propsAt(x, y);
        for (int i = matches.size() - 1; i >= 0; i--) {
            WorldProp prop = matches.get(i);
            if (prop.asset().equals(asset)) {
                return area.removeProp(prop);
            }
        }
        return false;
    }

    public boolean restoreProp(String mapId, WorldProp prop) {
        MapArea area = area(mapId);
        if (area == null || prop == null) {
            return false;
        }
        area.addProp(prop);
        return true;
    }

    public int mapCount() {
        return maps.size();
    }

    public int totalPropCount() {
        int count = 0;
        for (MapArea area : maps.values()) {
            count += area.props.size();
        }
        return count;
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

    public TilePoint townPortalPoint(String mapId) {
        MapArea area = area(mapId);
        if (area == null) {
            return null;
        }
        for (WorldProp prop : area.props) {
            if (isTownPortalAsset(prop.asset())) {
                return new TilePoint(prop.x(), prop.y());
            }
        }
        return null;
    }

    public TilePoint townPortalArrival(String mapId) {
        TilePoint portal = townPortalPoint(mapId);
        if (portal == null) {
            return null;
        }
        int[][] candidates = {
                {0, 1}, {-1, 1}, {1, 1}, {0, 0}, {-1, 0}, {1, 0}, {0, -1}
        };
        for (int[] candidate : candidates) {
            int x = portal.x() + candidate[0];
            int y = portal.y() + candidate[1];
            if (isPassable(mapId, x, y)) {
                return new TilePoint(x, y);
            }
        }
        return portal;
    }

    public boolean isTownPortalAsset(String asset) {
        return asset != null && asset.startsWith("town_portal_");
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

    public Set<String> removedPlayerVillageProps() {
        return Set.copyOf(removedPlayerVillageProps);
    }

    public Map<String, List<WorldProp>> playerInteriorProps() {
        Map<String, List<WorldProp>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<WorldProp>> entry : playerInteriorProps.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copy;
    }

    public Map<String, Map<TilePoint, Character>> playerInteriorTiles() {
        Map<String, Map<TilePoint, Character>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<TilePoint, Character>> entry : playerInteriorTiles.entrySet()) {
            copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
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
            playerVillageStage++;
            expandPlayerVillage(10);
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
        removedPlayerVillageProps.clear();
        playerInteriorProps.clear();
        playerInteriorTiles.clear();
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
            shiftedArea.addProp(new WorldProp(prop.x() + margin, prop.y() + margin, prop.asset(), prop.size()));
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
        // Settlement exits use transitions on passable ground; no visual road grid is stamped here.
    }

    private void registerPlayerVillageTransitions() {
        transitions.entrySet().removeIf(entry ->
                entry.getKey().startsWith(PLAYER_VILLAGE_ID + ":")
                        || (OVERWORLD_ID.equals(entry.getKey().split(":", 2)[0])
                        && PLAYER_VILLAGE_ID.equals(entry.getValue().targetMapId())));
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        if (area == null) {
            return;
        }
        int offset = playerVillageExpansionOffset();
        int ox = START_POSITION.x();
        int oy = START_POSITION.y();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (Math.abs(dx) + Math.abs(dy) <= 3) {
                    TilePoint entry = playerVillageEntryPoint(dx, dy, offset, area.width(), area.height());
                    addTransition(OVERWORLD_ID, ox + dx, oy + dy, PLAYER_VILLAGE_ID, entry.x(), entry.y(), "You enter Oathstead Camp.");
                }
            }
        }
        addSettlementBorderExits(PLAYER_VILLAGE_ID, area, ox, oy, "Oathstead Camp");
        refreshPlayerVillageExitMarkers(area, offset, area.width() - 1, area.height() - 1);
    }

    private TilePoint playerVillageEntryPoint(int dx, int dy, int offset, int width, int height) {
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx < 0 ? new TilePoint(1, 10 + offset) : new TilePoint(width - 2, 10 + offset);
        }
        if (dy < 0) {
            return new TilePoint(14 + offset, 1);
        }
        if (dy > 0) {
            return new TilePoint(14 + offset, height - 2);
        }
        return new TilePoint(14 + offset, 1);
    }

    private void refreshPlayerVillageExitMarkers(MapArea area, int offset, int eastX, int southY) {
        area.props.removeIf(prop -> PLAYER_VILLAGE_EXIT_MARKER_ASSET.equals(prop.asset()));
        addPlayerVillageExitMarker(area, 14 + offset, 0);
        addPlayerVillageExitMarker(area, 14 + offset, southY);
        addPlayerVillageExitMarker(area, 0, 10 + offset);
        addPlayerVillageExitMarker(area, eastX, 10 + offset);
    }

    private void addPlayerVillageExitMarker(MapArea area, int x, int y) {
        if (x < 0 || y < 0 || x >= area.width() || y >= area.height()) {
            return;
        }
        area.addProp(new WorldProp(x, y, PLAYER_VILLAGE_EXIT_MARKER_ASSET, PLAYER_VILLAGE_EXIT_MARKER_SIZE));
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
        connectPlayerVillageBuildingPath(building);
        return building;
    }

    public boolean restorePlayerVillageBuilding(CityBuilding building) {
        if (building == null || !building.key().startsWith("player_")
                || !canPlacePlayerVillageBuilding(building.x1(), building.y1(), building.width(), building.depth(), null)) {
            return false;
        }
        mutableCityBuildings(PLAYER_VILLAGE_ID).add(building);
        playerVillageBuildingLevels.putIfAbsent(building.key(), 1);
        connectPlayerVillageBuildingPath(building);
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
        connectPlayerVillageBuildingPath(moved);
        return true;
    }

    private void connectPlayerVillageBuildingPath(CityBuilding building) {
        // Player village buildings sit in clearings; do not auto-paint blocky access roads.
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
        if (!Terrain.passable(tile) || tile == 'h' || tile == 'w' || tile == 'x' || tile == 'c' || tile == 'u'
                || tile == 'd' || tile == 'i' || tile == 'o') {
            return false;
        }
        if (transitionAt(PLAYER_VILLAGE_ID, x, y) != null || cityBuildingAt(PLAYER_VILLAGE_ID, x, y) != null) {
            return false;
        }
        if (playerVillagePropAt(x, y) != null && Terrain.connectingRoad(tile)) {
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
        area.addProp(prop);
        return true;
    }

    public boolean canPlacePlayerVillageProp(int x, int y, WorldProp ignoredProp) {
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        if (area == null || x < 1 || y < 1 || x >= area.width() - 1 || y >= area.height() - 1
                || !Terrain.passable(area.tileAt(x, y)) || Terrain.connectingRoad(area.tileAt(x, y)) || area.tileAt(x, y) == 'h'
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
        area.addProp(prop);
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
        if (prop == null || PLAYER_VILLAGE_EXIT_MARKER_ASSET.equals(prop.asset())) {
            return false;
        }
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        boolean customProp = playerVillageProps.remove(prop);
        boolean removed = customProp;
        if (area != null) {
            removed = area.removeProp(prop) || removed;
        }
        if (removed && !customProp) {
            removedPlayerVillageProps.add(playerVillagePropKey(prop));
        }
        return removed;
    }

    public boolean restoreRemovedPlayerVillageProp(WorldProp prop) {
        if (prop == null || PLAYER_VILLAGE_EXIT_MARKER_ASSET.equals(prop.asset())) {
            return false;
        }
        boolean added = removedPlayerVillageProps.add(playerVillagePropKey(prop));
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        if (area != null) {
            area.props.removeIf(existing -> existing.x() == prop.x()
                    && existing.y() == prop.y()
                    && existing.asset().equals(prop.asset()));
        }
        return added;
    }

    public static String playerVillagePropKey(WorldProp prop) {
        return prop.x() + ":" + prop.y() + ":" + prop.asset();
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
            area.removeProp(prop);
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

    public boolean setPlayerInteriorTile(String mapId, int x, int y, char tile) {
        MapArea area = maps.get(mapId);
        if (!canSetPlayerInteriorTile(area, x, y)) {
            return false;
        }
        char resolved = tile == 'o' ? 'o' : 'i';
        area.tiles[y][x] = resolved;
        playerInteriorTiles.computeIfAbsent(mapId, ignored -> new LinkedHashMap<>())
                .put(new TilePoint(x, y), resolved);
        return true;
    }

    public boolean restorePlayerInteriorTile(String mapId, int x, int y, char tile) {
        if (mapId == null) {
            return false;
        }
        char resolved = tile == 'o' ? 'o' : 'i';
        playerInteriorTiles.computeIfAbsent(mapId, ignored -> new LinkedHashMap<>())
                .put(new TilePoint(x, y), resolved);
        MapArea area = maps.get(mapId);
        if (area == null) {
            return true;
        }
        if (!canSetPlayerInteriorTile(area, x, y)) {
            return false;
        }
        area.tiles[y][x] = resolved;
        return true;
    }

    private boolean canSetPlayerInteriorTile(MapArea area, int x, int y) {
        if (area == null || !"interior".equals(area.kind)
                || x < 1 || y < 1 || x >= area.width() - 1 || y >= area.height() - 1) {
            return false;
        }
        if (transitionAt(area.id, x, y) != null) {
            return false;
        }
        return interiorPropAt(area, x, y) == null;
    }

    private WorldProp interiorPropAt(MapArea area, int x, int y) {
        if (area == null) {
            return null;
        }
        for (int i = area.props.size() - 1; i >= 0; i--) {
            WorldProp prop = area.props.get(i);
            int[] footprint = interiorHitboxFootprint(prop.asset());
            if (x >= prop.x() && y >= prop.y()
                    && x < prop.x() + footprint[0]
                    && y < prop.y() + footprint[1]) {
                return prop;
            }
        }
        return null;
    }

    public WorldTransition transitionAt(String mapId, int x, int y) {
        return transitions.get(transitionKey(mapId, x, y));
    }

    public TilePoint overworldEntranceFor(String mapId) {
        if (mapId == null || mapId.isBlank() || OVERWORLD_ID.equals(mapId)) {
            return null;
        }
        TilePoint direct = directOverworldEntranceFor(mapId);
        if (direct != null) {
            return direct;
        }
        Set<String> seen = new HashSet<>();
        String cursor = mapId;
        for (int depth = 0; depth < 8 && seen.add(cursor); depth++) {
            for (Map.Entry<String, WorldTransition> entry : transitions.entrySet()) {
                String fromMap = transitionMapId(entry.getKey());
                if (!cursor.equals(fromMap)) {
                    continue;
                }
                WorldTransition transition = entry.getValue();
                if (OVERWORLD_ID.equals(transition.targetMapId())) {
                    return new TilePoint(transition.targetX(), transition.targetY());
                }
                TilePoint targetDirect = directOverworldEntranceFor(transition.targetMapId());
                if (targetDirect != null) {
                    return targetDirect;
                }
                cursor = transition.targetMapId();
                break;
            }
        }
        return null;
    }

    private TilePoint directOverworldEntranceFor(String mapId) {
        for (Map.Entry<String, WorldTransition> entry : transitions.entrySet()) {
            String key = entry.getKey();
            WorldTransition transition = entry.getValue();
            if (!mapId.equals(transition.targetMapId()) || !key.startsWith(OVERWORLD_ID + ":")) {
                continue;
            }
            String[] parts = key.split(":");
            if (parts.length == 3) {
                try {
                    return new TilePoint(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String transitionMapId(String key) {
        int first = key.indexOf(':');
        return first < 0 ? key : key.substring(0, first);
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

    public boolean isEditorMap(String mapId) {
        return mapId != null && mapId.startsWith("editor_") && maps.containsKey(mapId);
    }

    public String createEditorMap(String mapId, String label, String kind, int width, int height) {
        String normalizedKind = switch (kind == null ? "" : kind) {
            case "city", "village", "interior" -> kind;
            default -> "village";
        };
        int safeWidth = Math.max(10, Math.min(80, width));
        int safeHeight = Math.max(8, Math.min(60, height));
        MapArea area = new MapArea(mapId, label, normalizedKind, editorTiles(normalizedKind, safeWidth, safeHeight));
        maps.put(mapId, area);
        if ("city".equals(normalizedKind) || "village".equals(normalizedKind)) {
            cityBuildings.put(mapId, new ArrayList<>());
        } else {
            cityBuildings.remove(mapId);
        }
        interiorNpcs.put(mapId, List.of());
        transitions.entrySet().removeIf(entry -> entry.getKey().startsWith(mapId + ":"));
        return mapId;
    }

    private char[][] editorTiles(String kind, int width, int height) {
        char[][] grid = new char[height][width];
        char fill = "city".equals(kind) ? 'p' : "interior".equals(kind) ? 'i' : 'g';
        for (int y = 0; y < height; y++) {
            Arrays.fill(grid[y], fill);
        }
        if ("interior".equals(kind)) {
            for (int x = 0; x < width; x++) {
                grid[0][x] = 'o';
                grid[height - 1][x] = 'o';
            }
            for (int y = 0; y < height; y++) {
                grid[y][0] = 'o';
                grid[y][width - 1] = 'o';
            }
            int doorX = Math.max(1, width / 2);
            grid[height - 1][doorX] = 'i';
            if (doorX > 1) {
                grid[height - 1][doorX - 1] = 'i';
            }
        }
        return grid;
    }

    public boolean setEditorTile(String mapId, int x, int y, char tile) {
        MapArea area = maps.get(mapId);
        if (!isEditorMap(mapId) || area == null || x < 1 || y < 1 || x >= area.width() - 1 || y >= area.height() - 1) {
            return false;
        }
        if ("interior".equals(area.kind)) {
            char resolved = tile == 'o' || tile == 'K' || tile == 'p' || tile == 'j' || tile == 'a' ? 'o' : 'i';
            if (editorInteriorPropAt(mapId, x, y) != null) {
                return false;
            }
            area.tiles[y][x] = resolved;
            return true;
        }
        if (!Terrain.passable(tile) || tile == 'h' || tile == 'w' || tile == 'x' || tile == 'c' || tile == 'u' || tile == 'd') {
            return false;
        }
        if (transitionAt(mapId, x, y) != null || cityBuildingAt(mapId, x, y) != null) {
            return false;
        }
        WorldProp prop = editorPropAt(mapId, x, y);
        if (prop != null && Terrain.connectingRoad(tile)) {
            return false;
        }
        area.tiles[y][x] = tile;
        return true;
    }

    public CityBuilding placeEditorBuilding(String mapId, String style, int x, int y) {
        MapArea area = maps.get(mapId);
        if (!isEditorMap(mapId) || area == null || "interior".equals(area.kind)) {
            return null;
        }
        VillageManager.BuildingPlan plan = VillageManager.buildingPlan(style);
        if (!canPlaceEditorBuilding(mapId, x, y, plan.width(), plan.depth(), null)) {
            return null;
        }
        List<CityBuilding> buildings = mutableCityBuildings(mapId);
        int palette = Math.floorMod(x * 31 + y * 17 + buildings.size(), 3);
        String key = "editor_" + plan.style() + "_" + x + "_" + y + "_" + buildings.size();
        CityBuilding building = new CityBuilding(key, x, y, x + plan.width() - 1, y + plan.depth() - 1, plan.style(), palette);
        buildings.add(building);
        return building;
    }

    public boolean moveEditorBuilding(String mapId, CityBuilding building, int x, int y) {
        if (!isEditorMap(mapId) || building == null || !canPlaceEditorBuilding(mapId, x, y, building.width(), building.depth(), building)) {
            return false;
        }
        removeEditorBuilding(mapId, building);
        CityBuilding moved = new CityBuilding(building.key(), x, y, x + building.width() - 1, y + building.depth() - 1, building.style(), building.palette());
        mutableCityBuildings(mapId).add(moved);
        return true;
    }

    public boolean removeEditorBuilding(String mapId, CityBuilding building) {
        return isEditorMap(mapId) && building != null && mutableCityBuildings(mapId).remove(building);
    }

    private boolean canPlaceEditorBuilding(String mapId, int x, int y, int width, int depth, CityBuilding ignoredBuilding) {
        MapArea area = maps.get(mapId);
        if (area == null || x < 1 || y < 1 || x + width >= area.width() - 1 || y + depth >= area.height() - 1) {
            return false;
        }
        for (int yy = y; yy < y + depth; yy++) {
            for (int xx = x; xx < x + width; xx++) {
                boolean ignoredTile = ignoredBuilding != null && ignoredBuilding.contains(xx, yy);
                if (!ignoredTile && (!Terrain.passable(area.tileAt(xx, yy)) || area.tileAt(xx, yy) == 'w')) {
                    return false;
                }
                if (transitionAt(mapId, xx, yy) != null || editorPropAt(mapId, xx, yy) != null) {
                    return false;
                }
            }
        }
        for (CityBuilding other : cityBuildings(mapId)) {
            if (other.equals(ignoredBuilding)) {
                continue;
            }
            if (rectsOverlap(x, y, x + width - 1, y + depth - 1, other.x1(), other.y1(), other.x2(), other.y2())) {
                return false;
            }
        }
        return true;
    }

    public boolean addEditorProp(String mapId, int x, int y, String asset, int size) {
        MapArea area = maps.get(mapId);
        if (!isEditorMap(mapId) || area == null || "interior".equals(area.kind)
                || x < 1 || y < 1 || x >= area.width() - 1 || y >= area.height() - 1
                || !Terrain.passable(area.tileAt(x, y)) || cityBuildingAt(mapId, x, y) != null
                || editorPropAt(mapId, x, y) != null) {
            return false;
        }
        area.addProp(new WorldProp(x, y, asset, size));
        return true;
    }

    public WorldProp editorPropAt(String mapId, int x, int y) {
        MapArea area = maps.get(mapId);
        if (area == null) {
            return null;
        }
        return area.propAt(x, y);
    }

    public boolean removeEditorProp(String mapId, WorldProp prop) {
        MapArea area = maps.get(mapId);
        return isEditorMap(mapId) && area != null && prop != null && area.removeProp(prop);
    }

    public boolean moveEditorProp(String mapId, WorldProp prop, int x, int y) {
        if (prop == null || !removeEditorProp(mapId, prop)) {
            return false;
        }
        if (addEditorProp(mapId, x, y, prop.asset(), prop.size())) {
            return true;
        }
        restoreProp(mapId, prop);
        return false;
    }

    public boolean addEditorInteriorProp(String mapId, int x, int y, String asset, int size) {
        MapArea area = maps.get(mapId);
        if (!isEditorMap(mapId) || area == null || !"interior".equals(area.kind) || !canPlaceInteriorAsset(area, x, y, asset)) {
            return false;
        }
        applyInteriorProp(area, new WorldProp(x, y, asset, size));
        return true;
    }

    public WorldProp editorInteriorPropAt(String mapId, int x, int y) {
        MapArea area = maps.get(mapId);
        if (area == null) {
            return null;
        }
        for (int i = area.props.size() - 1; i >= 0; i--) {
            WorldProp prop = area.props.get(i);
            int[] footprint = interiorHitboxFootprint(prop.asset());
            if (x >= prop.x() && y >= prop.y()
                    && x < prop.x() + footprint[0]
                    && y < prop.y() + footprint[1]) {
                return prop;
            }
        }
        return null;
    }

    public boolean removeEditorInteriorProp(String mapId, WorldProp prop) {
        MapArea area = maps.get(mapId);
        if (!isEditorMap(mapId) || area == null || prop == null) {
            return false;
        }
        boolean removed = area.removeProp(prop);
        if (removed) {
            clearInteriorFootprint(area, prop);
        }
        return removed;
    }

    public boolean moveEditorInteriorProp(String mapId, WorldProp prop, int x, int y) {
        if (prop == null || !removeEditorInteriorProp(mapId, prop)) {
            return false;
        }
        if (addEditorInteriorProp(mapId, x, y, prop.asset(), prop.size())) {
            return true;
        }
        applyInteriorProp(maps.get(mapId), prop);
        return false;
    }

    public String exportEditorMap(String mapId) {
        MapArea area = maps.get(mapId);
        if (!isEditorMap(mapId) || area == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        out.append("id=").append(area.id).append('\n');
        out.append("label=").append(area.label).append('\n');
        out.append("kind=").append(area.kind).append('\n');
        out.append("width=").append(area.width()).append('\n');
        out.append("height=").append(area.height()).append('\n');
        for (int y = 0; y < area.height(); y++) {
            out.append("tile.").append(y).append('=').append(new String(area.tiles[y])).append('\n');
        }
        int propIndex = 0;
        for (WorldProp prop : area.props) {
            out.append("prop.").append(propIndex++).append('=')
                    .append(prop.x()).append(',').append(prop.y()).append(',')
                    .append(prop.asset()).append(',').append(prop.size()).append('\n');
        }
        int buildingIndex = 0;
        for (CityBuilding building : cityBuildings(mapId)) {
            out.append("building.").append(buildingIndex++).append('=')
                    .append(building.style()).append(',').append(building.x1()).append(',')
                    .append(building.y1()).append(',').append(building.width()).append(',')
                    .append(building.depth()).append('\n');
        }
        return out.toString();
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
            if (!isEmptyPlayerVillageInterior(sourceMapId, building)) {
                addHouseProps(house, theme, seed);
            }
            ensureInteriorNavigable(house);
            applyPlayerInteriorTiles(house);
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

    public TilePoint playerVillageRespawnPoint() {
        MapArea area = maps.get(PLAYER_VILLAGE_ID);
        if (area == null) {
            return new TilePoint(0, 0);
        }
        int offset = playerVillageExpansionOffset();
        int x = Math.max(1, Math.min(area.width() - 2, 14 + offset));
        int y = Math.max(1, Math.min(area.height() - 2, 17 + offset));
        return safePassablePoint(PLAYER_VILLAGE_ID, x, y);
    }

    private TilePoint safePassablePoint(String mapId, int x, int y) {
        int width = width(mapId);
        int height = height(mapId);
        x = Math.max(0, Math.min(Math.max(0, width - 1), x));
        y = Math.max(0, Math.min(Math.max(0, height - 1), y));
        if (isPassable(mapId, x, y)) {
            return new TilePoint(x, y);
        }
        int limit = Math.max(width, height);
        for (int radius = 1; radius <= limit; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != radius) {
                        continue;
                    }
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx >= 0 && ny >= 0 && nx < width && ny < height && isPassable(mapId, nx, ny)) {
                        return new TilePoint(nx, ny);
                    }
                }
            }
        }
        return new TilePoint(0, 0);
    }

    private boolean isEmptyPlayerVillageInterior(String sourceMapId, CityBuilding building) {
        return PLAYER_VILLAGE_ID.equals(sourceMapId)
                && building != null
                && building.key() != null
                && building.key().startsWith("player_");
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

    private boolean isSettlementMapKind(String mapId) {
        String mapKind = kind(mapId);
        return "city".equals(mapKind) || "village".equals(mapKind);
    }

    private CityBuilding homeBuildingForNpc(Npc npc) {
        if (isHomelessNpc(npc)) {
            CityBuilding inn = firstBuildingWithStyle(npc.mapId(), "inn");
            if (inn != null) {
                return inn;
            }
        }
        CityBuilding work = workBuildingForNpc(npc);
        if (work != null && !List.of("barracks", "arena", "bell_tower", "sun_shrine").contains(work.style())) {
            return work;
        }
        CityBuilding residence = firstBuildingWithStyle(npc.mapId(), "house", "row");
        if (residence != null) {
            return residence;
        }
        CityBuilding inn = firstBuildingWithStyle(npc.mapId(), "inn");
        return inn != null ? inn : work;
    }

    private CityBuilding workBuildingForNpc(Npc npc) {
        String text = npcText(npc);
        String[] styles;
        if (containsAny(text, "captain", "guard", "watch", "warden", "scout", "knight", "gate")) {
            styles = new String[]{"barracks", "watchtower", "arena", "hall"};
        } else if (containsAny(text, "smith", "trapmaster", "iron", "stonebreaker", "miner")) {
            styles = new String[]{"blacksmith", "mine", "workshop", "barracks", "shop"};
        } else if (containsAny(text, "baker", "cook", "canteen", "bread", "soup", "hearth")) {
            styles = new String[]{"bakery", "inn", "shop"};
        } else if (containsAny(text, "merchant", "peddler", "seller", "quartermaster", "market", "ledger")
                || npc.shopId() != null) {
            styles = new String[]{"shop", "restaurant", "warehouse", "inn"};
        } else if (containsAny(text, "archivist", "scribe", "apprentice", "clerk", "map", "record")) {
            styles = new String[]{"guild", "hall", "mage_tower", "shop"};
        } else if (containsAny(text, "cleric", "brother", "medic", "healer", "ward", "sunwarden")) {
            styles = new String[]{"sun_shrine", "hall", "apothecary", "alchemist", "guild"};
        } else if (containsAny(text, "dock", "fish", "net", "river", "wellkeeper")) {
            styles = new String[]{"fishing_hut", "warehouse", "inn"};
        } else if (containsAny(text, "carpenter", "builder", "basket", "seam", "weav", "tanner", "furrier", "mender")) {
            styles = new String[]{"carpenter", "workshop", "forestry_hut", "shop", "warehouse"};
        } else if (containsAny(text, "farmer", "goat", "grove", "hedge", "reedcutter", "wildspeaker")) {
            styles = new String[]{"farmstead", "forestry_hut", "house", "row"};
        } else {
            return null;
        }
        return firstBuildingWithStyle(npc.mapId(), styles);
    }

    private CityBuilding firstBuildingWithStyle(String mapId, String... styles) {
        List<String> wanted = Arrays.asList(styles);
        for (CityBuilding building : cityBuildings(mapId)) {
            if (wanted.contains(building.style())) {
                return building;
            }
        }
        return null;
    }

    private TilePoint buildingNpcDoorSpot(String mapId, CityBuilding building, int seed) {
        List<TilePoint> doors = cityBuildingDoorTiles(building);
        int start = doors.isEmpty() ? 0 : Math.floorMod(seed, doors.size());
        for (int i = 0; i < doors.size(); i++) {
            TilePoint door = doors.get((start + i) % doors.size());
            int[][] spots = {{0, 1}, {-1, 1}, {1, 1}, {0, 2}, {-1, 2}, {1, 2}};
            for (int[] spot : spots) {
                int x = door.x() + spot[0];
                int y = door.y() + spot[1];
                if (isPassable(mapId, x, y)) {
                    return new TilePoint(x, y);
                }
            }
        }
        return closestPassableNear(mapId, building.anchor().x(), building.y2() + 1,
                building.anchor().x(), building.y2() + 1, 5, seed);
    }

    private TilePoint closestPassableNear(String mapId, int targetX, int targetY, int fallbackX, int fallbackY,
                                          int radius, int seed) {
        if (isPassable(mapId, targetX, targetY)) {
            return new TilePoint(targetX, targetY);
        }
        int start = Math.floorMod(seed, 8);
        int[][] ring = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
        for (int r = 1; r <= radius; r++) {
            for (int i = 0; i < ring.length; i++) {
                int[] direction = ring[(start + i) % ring.length];
                for (int step = 1; step <= r; step++) {
                    int x = targetX + direction[0] * step;
                    int y = targetY + direction[1] * step;
                    if (isPassable(mapId, x, y)) {
                        return new TilePoint(x, y);
                    }
                }
            }
        }
        return new TilePoint(fallbackX, fallbackY);
    }

    private boolean isHomelessNpc(Npc npc) {
        return containsAny(npcText(npc), "homeless", "vagrant", "beggar", "wanderer", "traveler", "drifter", "refugee", "hire me");
    }

    private String npcText(Npc npc) {
        return ((npc.name() == null ? "" : npc.name()) + " "
                + (npc.sprite() == null ? "" : npc.sprite()) + " "
                + (npc.shopId() == null ? "" : npc.shopId()) + " "
                + String.join(" ", npc.dialog())).toLowerCase();
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private int npcStableHash(Npc npc) {
        int hash = 17;
        hash = hash * 31 + (npc.name() == null ? 0 : npc.name().hashCode());
        hash = hash * 31 + (npc.mapId() == null ? 0 : npc.mapId().hashCode());
        return hash == Integer.MIN_VALUE ? 0 : Math.abs(hash);
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
        } else if (building.width() <= 3 || VillageManager.isManagedBuildingStyle(building.style()) || List.of(
                "hall", "guild", "barracks", "warehouse", "inn", "arena", "mage_tower", "bell_tower", "sun_shrine",
                "river_hall", "workshop", "carpenter", "alchemist", "restaurant"
        ).contains(building.style())) {
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
        overworld.landmarks.put(new TilePoint(183, 248), "The Old Gate of Alderfall");
        overworld.addProp(new WorldProp(183, 248, GameData.STORY_PORTAL_ASSET, 88));
        OverworldLandmarkPropGenerator.addDiscoverabilityProps(overworld);
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
    }

    private void addCity(String id, String label, int ox, int oy, String variant) {
        boolean town = label.contains("Town");
        MapArea area = new MapArea(id, label, "city", town ? townTiles(variant) : cityTiles(variant));
        List<CityBuilding> buildings = new ArrayList<>(cityBuildingTemplates(variant));
        tuneSettlementBuildingDensity(buildings, area, id, variant, true, town);
        addCityInfillBuildings(buildings, area, variant);
        addMajorCivicBuildings(buildings, area, variant, town);
        if (town) {
            addTownLandmarkBuildings(buildings, area, variant);
        }
        buildings = splitOversizedSettlementBuildings(buildings);
        cityBuildings.put(id, buildings);
        applySettlementLayoutPlan(area, variant, town);
        connectSettlementBuildingPaths(area, buildings, Terrain.COBBLESTONE_ROAD);
        addTownParks(area, variant, town ? 2 : 3);
        normalizeCityRoadsAndPaving(area, variant);
        addSettlementDistrictProps(area, variant, town);
        addCityProps(area, variant);
        addBusinessYardPropClusters(area, variant);
        if (town) {
            addTownProps(area, variant);
        }
        maps.put(id, area);
        interiorNpcs.put(id, placeInquiryNpcs(id, buildings, variant, town ? 5 : 6));
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (Math.abs(dx) + Math.abs(dy) <= 3) {
                    TilePoint entry = cityEntryPoint(area, dx, dy);
                    addTransition(OVERWORLD_ID, ox + dx, oy + dy, id, entry.x(), entry.y(), "You enter " + label + ".");
                }
            }
        }
        addSettlementBorderExits(id, area, ox, oy, label);
        area.landmarks.put(new TilePoint(17, 11), label);
        addTownPortal(area, variant, true);
        addSettlementSite(id, label, label.contains("Town") ? "Town" : "City", ox, oy);
    }

    private void addVillage(String id, String label, int ox, int oy, String variant) {
        MapArea area = new MapArea(id, label, "village", villageTiles(variant));
        List<CityBuilding> buildings = villageBuildingTemplates(id, variant);
        cityBuildings.put(id, buildings);
        addVillageProps(area, variant);
        addTownParks(area, variant, 1 + Math.floorMod(id.hashCode(), 2));
        maps.put(id, area);
        interiorNpcs.put(id, placeInquiryNpcs(id, buildings, variant, 4));
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (Math.abs(dx) + Math.abs(dy) <= 3) {
                    TilePoint entry = villageEntryPoint(dx, dy);
                    addTransition(OVERWORLD_ID, ox + dx, oy + dy, id, entry.x(), entry.y(), "You enter " + label + ".");
                }
            }
        }
        addSettlementBorderExits(id, area, ox, oy, label);
        area.landmarks.put(new TilePoint(14, 9), label);
        addTownPortal(area, variant, false);
        addSettlementSite(id, label, "Village", ox, oy);
    }

    private List<Npc> placeInquiryNpcs(String mapId, List<CityBuilding> buildings, String variant, int desiredCount) {
        List<Npc> npcs = new ArrayList<>();
        Set<TilePoint> occupied = new HashSet<>();
        int targetCount = Math.max(1, desiredCount);
        int step = Math.max(1, buildings.size() / targetCount);
        for (int i = 0; i < buildings.size() && npcs.size() < targetCount; i += step) {
            CityBuilding building = buildings.get(i);
            List<TilePoint> doors = cityBuildingDoorTiles(building);
            if (doors.isEmpty()) {
                continue;
            }
            TilePoint door = doors.get(Math.floorMod(mapId.hashCode() + i, doors.size()));
            int x = door.x();
            int y = door.y() + 1;
            TilePoint point = new TilePoint(x, y);
            if (!isPassable(mapId, x, y) || !occupied.add(point)) {
                continue;
            }
            String buildingKey = building.key() == null ? building.x1() + "_" + building.y1() : building.key();
            int roll = Math.floorMod(mapId.hashCode() + buildingKey.hashCode() + i * 37, 4);
            String role = switch (roll) {
                case 0 -> "Street Guide";
                case 1 -> "Local Clerk";
                case 2 -> "Doorward";
                default -> "Town Crier";
            };
            String sprite = switch (roll) {
                case 0 -> "npc_citizen_man";
                case 1 -> "npc_citizen_woman";
                case 2 -> "npc_torin";
                default -> "npc_merchant";
            };
            String place = label(mapId);
            String name = settlementNpcName(mapId, variant, buildingKey, roll, npcs.size());
            npcs.add(new Npc(mapId, name, sprite, x, y, List.of(
                    role + ": I keep track of the doors and stories around " + place + ".",
                    "Places: Ask me about nearby buildings if you want the useful version, not the signboard version.",
                    "Work: The " + variant + " streets teach you where to look before they teach you what it means."
            ), null, null));
        }
        return npcs;
    }

    private String settlementNpcName(String mapId, String variant, String buildingKey, int roleRoll, int index) {
        int seed = Math.abs((mapId + ":" + variant + ":" + buildingKey + ":" + roleRoll + ":" + index).hashCode());
        String[] givenNames = settlementGivenNames(variant);
        String[] bynames = settlementBynames(variant);
        String given = givenNames[Math.floorMod(seed, givenNames.length)];
        String byname = bynames[Math.floorMod(seed / 17, bynames.length)];
        return given + " " + byname;
    }

    private String[] settlementGivenNames(String variant) {
        return switch (variant) {
            case "snow" -> new String[]{"Asta", "Borin", "Elric", "Fenna", "Hald", "Ivara", "Noll", "Pem", "Siv", "Torr"};
            case "desert", "sanctum" -> new String[]{"Amal", "Bahir", "Dima", "Farid", "Imani", "Jalen", "Nura", "Rafi", "Safa", "Toma"};
            case "marsh", "belltower" -> new String[]{"Bessa", "Corso", "Fen", "Jun", "Lysa", "Merrit", "Pella", "Reed", "Vell", "Ysra"};
            case "archive" -> new String[]{"Aren", "Dain", "Hollis", "Ilyen", "Maera", "Miri", "Pela", "Ren", "Sel", "Vannis"};
            case "highwall" -> new String[]{"Berta", "Cass", "Halen", "Korr", "Lysa", "Odrick", "Rook", "Tarin", "Vesh", "Yaro"};
            default -> new String[]{"Aster", "Bran", "Cala", "Dain", "Edda", "Finch", "Hale", "Liora", "Mira", "Sori"};
        };
    }

    private String[] settlementBynames(String variant) {
        return switch (variant) {
            case "snow" -> new String[]{"Snowmark", "Frostlane", "Cairnstep", "Hearthwatch", "Passkeep", "Whitebough"};
            case "desert", "sanctum" -> new String[]{"Sunwater", "Glassroad", "Wellward", "Ashmarket", "Dunebell", "Saltwake"};
            case "marsh", "belltower" -> new String[]{"Reedwake", "Fenlight", "Bellwater", "Mistbridge", "Lanternrun", "Willowmark"};
            case "archive" -> new String[]{"Inkmargin", "Dusthall", "Mapkeep", "Stonepage", "Quillward", "Booklane"};
            case "highwall" -> new String[]{"Gatewake", "Ironpost", "Watchroad", "Shieldbell", "Bannerhold", "Stonewatch"};
            default -> new String[]{"Oaklane", "Greenroad", "Hearthfield", "Mossward", "Briarstep", "Willowbend"};
        };
    }

    private void addPlayerCamp(String id, String label, int ox, int oy, String variant) {
        cityBuildings.put(id, new ArrayList<>());
        MapArea area = new MapArea(id, label, "village", playerCampTiles(variant));
        addPlayerCampProps(area);
        maps.put(id, area);
        if (PLAYER_VILLAGE_ID.equals(id)) {
            registerPlayerVillageTransitions();
        } else {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) <= 3) {
                        TilePoint entry = villageEntryPoint(dx, dy);
                        addTransition(OVERWORLD_ID, ox + dx, oy + dy, id, entry.x(), entry.y(), "You enter " + label + ".");
                    }
                }
            }
            addSettlementBorderExits(id, area, ox, oy, label);
        }
        area.landmarks.put(new TilePoint(14, 17), label);
        addTownPortal(area, variant, false);
        addSettlementSite(id, label, "Player Settlement", ox, oy);
    }

    private void addSettlementBorderExits(String id, MapArea area, int ox, int oy, String label) {
        String message = "You leave " + label + ".";
        int southY = area.height() - 1;
        int eastX = area.width() - 1;
        for (int x = 0; x < area.width(); x++) {
            addTransition(id, x, 0, OVERWORLD_ID, ox, oy - 3, message);
            addTransition(id, x, southY, OVERWORLD_ID, ox, oy + 3, message);
        }
        for (int y = 1; y < southY; y++) {
            addTransition(id, 0, y, OVERWORLD_ID, ox - 3, oy, message);
            addTransition(id, eastX, y, OVERWORLD_ID, ox + 3, oy, message);
        }
    }

    private void addSettlementSite(String id, String label, String kind, int x, int y) {
        settlementSites.add(new SettlementSite(id, label, kind, x, y, kingdomAt(x, y).id()));
    }

    private void addTownPortal(MapArea area, String variant, boolean city) {
        TilePoint point = findTownPortalSite(area, city);
        clearTownPortalFootprintProps(area, point.x(), point.y());
        paveTownPortalPad(area, point.x(), point.y(), variant);
        area.addProp(new WorldProp(point.x(), point.y(), townPortalAsset(variant, city), city ? 104 : 96));
        area.landmarks.put(point, "Town Portal");
    }

    private TilePoint findTownPortalSite(MapArea area, boolean city) {
        TilePoint[] preferred = city
                ? new TilePoint[]{
                new TilePoint(17, 15), new TilePoint(17, 16), new TilePoint(18, 15),
                new TilePoint(15, 15), new TilePoint(20, 15), new TilePoint(12, 10), new TilePoint(23, 10)
        }
                : new TilePoint[]{
                new TilePoint(18, 12), new TilePoint(17, 12), new TilePoint(20, 12),
                new TilePoint(18, 16), new TilePoint(10, 10), new TilePoint(24, 10)
        };
        int salt = area.id.hashCode();
        TilePoint best = null;
        int bestScore = Integer.MAX_VALUE;
        int cx = city ? 17 : 18;
        int cy = city ? 15 : 14;
        for (int i = 0; i < preferred.length; i++) {
            TilePoint point = preferred[i];
            int score = townPortalSiteScore(area, point.x(), point.y(), cx, cy, i * 3);
            if (score < bestScore) {
                bestScore = score;
                best = point;
            }
        }
        for (int radius = 0; radius < Math.max(area.width(), area.height()); radius++) {
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int x = cx - radius; x <= cx + radius; x++) {
                    if (Math.abs(x - cx) != radius && Math.abs(y - cy) != radius) {
                        continue;
                    }
                    int score = townPortalSiteScore(area, x, y, cx, cy, Math.floorMod(hash(x, y, salt), 17));
                    if (score < bestScore) {
                        bestScore = score;
                        best = new TilePoint(x, y);
                    }
                }
            }
        }
        return best == null ? (city ? new TilePoint(17, 15) : new TilePoint(18, 14)) : best;
    }

    private int townPortalSiteScore(MapArea area, int anchorX, int anchorY, int centerX, int centerY, int salt) {
        if (!canPlaceTownPortal(area, anchorX, anchorY, true)) {
            return Integer.MAX_VALUE;
        }
        int score = Math.abs(anchorX - centerX) * 14 + Math.abs(anchorY - centerY) * 14 + salt;
        int roadTiles = 0;
        int gardenTiles = 0;
        int plazaTiles = 0;
        int props = 0;
        for (int y = anchorY - 2; y <= anchorY + 2; y++) {
            for (int x = anchorX - 2; x <= anchorX + 2; x++) {
                char tile = area.tileAt(x, y);
                if (Terrain.connectingRoad(tile)) {
                    roadTiles++;
                }
                if (tile == 'g' || tile == 'f' || tile == 'y' || tile == 'v' || tile == 'n' || tile == 's') {
                    gardenTiles++;
                }
                if (tile == 'p' || tile == 'j' || tile == 'l' || tile == 'C' || tile == 'G') {
                    plazaTiles++;
                }
                if (area.propAt(x, y) != null) {
                    props++;
                }
            }
        }
        score += roadTiles * 16;
        score += Math.max(0, props - 2) * 12;
        score -= Math.min(8, gardenTiles) * 7;
        score -= Math.min(8, plazaTiles) * 3;
        if (Terrain.connectingRoad(area.tileAt(anchorX, anchorY))) {
            score += 90;
        }
        return score;
    }

    private boolean canPlaceTownPortal(MapArea area, int anchorX, int anchorY, boolean allowProps) {
        for (int y = anchorY - 1; y <= anchorY + 1; y++) {
            for (int x = anchorX - 1; x <= anchorX + 1; x++) {
                if (!insideSettlementInterior(area, x, y)
                        || !Terrain.passable(area.tileAt(x, y))
                        || transitionAt(area.id, x, y) != null
                        || cityBuildingAt(area.id, x, y) != null
                        || (!allowProps && area.propAt(x, y) != null)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void clearTownPortalFootprintProps(MapArea area, int anchorX, int anchorY) {
        area.props.removeIf(prop -> Math.abs(prop.x() - anchorX) <= 1 && Math.abs(prop.y() - anchorY) <= 1);
    }

    private String townPortalAsset(String variant, boolean city) {
        return switch (variant) {
            case "snow", "highwall" -> "town_portal_snow";
            case "desert", "sanctum" -> "town_portal_desert";
            case "marsh", "belltower" -> "town_portal_marsh";
            case "green" -> "town_portal_green";
            default -> city ? "town_portal_city" : "town_portal_green";
        };
    }

    private TilePoint cityEntryPoint(MapArea area, int dx, int dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx < 0 ? new TilePoint(1, 12) : new TilePoint(area.width() - 2, 12);
        }
        if (dy < 0) {
            return new TilePoint(17, 1);
        }
        if (dy > 0) {
            return new TilePoint(17, area.height() - 2);
        }
        return new TilePoint(17, Math.min(area.height() - 2, 21));
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
        addDungeon(id, label, ox, oy, depth, "crypt");
    }

    private void addDungeon(String id, String label, int ox, int oy, int depth, String theme) {
        int floors = Math.max(3, Math.min(4, depth + 2));
        String firstFloor = dungeonFloorId(id, 1);
        for (int floor = 1; floor <= floors; floor++) {
            String floorId = dungeonFloorId(id, floor);
            MapArea area = new MapArea(floorId, dungeonFloorLabel(label, theme, floor, floors), "dungeon", dungeonTiles(theme, floor, floors, floorId));
            addDungeonProps(area, theme, floor, floors);
            addDungeonStairProps(area, theme, floor, floors);
            maps.put(floorId, area);
            area.landmarks.put(new TilePoint(2, 12), floor == 1 ? label : dungeonFloorLabel(label, theme, floor, floors));
            if (floor < floors) {
                area.landmarks.put(new TilePoint(34, 18), "Stairs Down");
            }
            if (floor > 1) {
                area.landmarks.put(new TilePoint(1, 12), "Stairs Up");
            }
        }
        addTransition(OVERWORLD_ID, ox, oy, firstFloor, 2, 12, "You descend into " + label + ".");
        addTransition(firstFloor, 1, 12, OVERWORLD_ID, ox, oy, "You climb back to the surface.");
        for (int floor = 1; floor < floors; floor++) {
            String upper = dungeonFloorId(id, floor);
            String lower = dungeonFloorId(id, floor + 1);
            addTransition(upper, 34, 18, lower, 2, 12, "You descend to floor " + (floor + 1) + ".");
            addTransition(lower, 1, 12, upper, 33, 18, "You climb back to floor " + floor + ".");
        }
    }

    private void addDungeon(AdventureSite site) {
        addDungeon(site.id, site.label, site.x, site.y, site.depth, site.kind);
    }

    public WorldTransition dungeonEscapeTransition(String mapId) {
        if (!"dungeon".equals(kind(mapId))) {
            return null;
        }
        String cursor = mapId;
        Set<String> seen = new HashSet<>();
        while (seen.add(cursor)) {
            WorldTransition surface = transitionFromToKind(cursor, "overworld", false);
            if (surface != null) {
                return new WorldTransition(surface.targetMapId(), surface.targetX(), surface.targetY(), "The scroll carries you back to the surface.");
            }
            WorldTransition up = transitionFromToKind(cursor, "dungeon", true);
            if (up == null) {
                return null;
            }
            cursor = up.targetMapId();
        }
        return null;
    }

    private WorldTransition transitionFromToKind(String fromMapId, String targetKind, boolean preferShallowerDungeon) {
        int currentFloor = dungeonFloorNumber(fromMapId);
        WorldTransition fallback = null;
        String prefix = fromMapId + ":";
        for (Map.Entry<String, WorldTransition> entry : transitions.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            WorldTransition transition = entry.getValue();
            if (!targetKind.equals(kind(transition.targetMapId()))) {
                continue;
            }
            if (!preferShallowerDungeon) {
                return transition;
            }
            if (dungeonFloorNumber(transition.targetMapId()) < currentFloor) {
                return transition;
            }
            fallback = transition;
        }
        return fallback;
    }

    private String dungeonFloorId(String id, int floor) {
        return id.replaceFirst("_\\d+$", "_" + floor);
    }

    private String dungeonFloorLabel(String label, String theme, int floor, int floors) {
        if (floor == 1) {
            return label + " - " + themedDungeonUpperLabel(theme);
        }
        if (floor == floors) {
            return label + " - " + themedDungeonDeepLabel(theme);
        }
        return label + " - " + themedDungeonMiddleLabel(theme, floor);
    }

    private String themedDungeonUpperLabel(String theme) {
        return switch (theme) {
            case "cave" -> "Upper Caverns";
            case "crypt" -> "Grave Halls";
            case "abandoned_castle" -> "Outer Keep";
            case "prison" -> "Cell Blocks";
            case "sewer" -> "Drainworks";
            case "goblin_camp" -> "Warren Tunnels";
            case "bandit_camp" -> "Smuggler Vaults";
            default -> "Upper Halls";
        };
    }

    private String themedDungeonMiddleLabel(String theme, int floor) {
        return switch (theme) {
            case "cave" -> "Crystal Galleries";
            case "crypt" -> "Ossuary " + floor;
            case "abandoned_castle" -> "Collapsed Ward";
            case "prison" -> "Iron Gallery";
            case "sewer" -> "Cistern " + floor;
            case "goblin_camp" -> "Root Cellars";
            case "bandit_camp" -> "Contraband Halls";
            default -> "Floor " + floor;
        };
    }

    private String themedDungeonDeepLabel(String theme) {
        return switch (theme) {
            case "cave" -> "Deep Grotto";
            case "crypt" -> "Sealed Tomb";
            case "abandoned_castle" -> "Black Vault";
            case "prison" -> "Oubliette";
            case "sewer" -> "Sunken Sump";
            case "goblin_camp" -> "Boss Den";
            case "bandit_camp" -> "Captain's Lockup";
            default -> "Deepest Floor";
        };
    }

    private int dungeonFloorNumber(String mapId) {
        int underscore = mapId.lastIndexOf('_');
        if (underscore < 0 || underscore == mapId.length() - 1) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(mapId.substring(underscore + 1)));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void addDungeonStairProps(MapArea area, String theme, int floor, int floors) {
        String stairAsset = switch (theme) {
            case "cave" -> "location_overgrown_cave_entrance";
            case "abandoned_castle", "prison" -> "location_castle_ruins";
            default -> "location_dungeon_stair_entrance";
        };
        if (floor > 1) {
            area.addProp(new WorldProp(1, 12, stairAsset, 46));
        }
        if (floor < floors) {
            area.addProp(new WorldProp(34, 18, stairAsset, 50));
        }
    }

    private void addTransition(String fromMap, int fromX, int fromY, String toMap, int toX, int toY, String message) {
        transitions.put(transitionKey(fromMap, fromX, fromY), new WorldTransition(toMap, toX, toY, message));
    }

    private String transitionKey(String mapId, int x, int y) {
        return mapId + ":" + x + ":" + y;
    }

    private void paveTownPortalPad(MapArea area, int centerX, int centerY, String variant) {
        char plaza = portalPadPavementTile(variant);
        for (int y = centerY - 1; y <= centerY + 1; y++) {
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                if (x < 0 || y < 0 || x >= area.width() || y >= area.height()
                        || cityBuildingAt(area.id, x, y) != null
                        || transitionAt(area.id, x, y) != null
                        || area.tileAt(x, y) == 'w' || area.tileAt(x, y) == '~' || area.tileAt(x, y) == 'x') {
                    continue;
                }
                area.tiles[y][x] = plaza;
            }
        }
    }

    private char portalPadPavementTile(String variant) {
        return switch (variant) {
            case "sanctum" -> 'b';
            case "belltower" -> 'y';
            case "highwall" -> 'p';
            default -> 'C';
        };
    }

    private char[][] cityTiles(String variant) {
        char[][] grid = filled(36, 28, 'p');
        char road = Terrain.COBBLESTONE_ROAD;
        border(grid, 'x');
        rect(grid, 16, 0, 17, 27, road);
        rect(grid, 0, 11, 35, 12, road);
        rect(grid, 2, 6, 31, 6, road);
        rect(grid, 2, 18, 31, 18, road);
        rect(grid, 6, 21, 27, 21, road);
        rect(grid, 13, 9, 21, 14, 'y');
        rect(grid, 15, 10, 19, 13, 'p');
        if ("riverside".equals(variant)) {
            rect(grid, 2, 3, 31, 3, 'w');
            rect(grid, 16, 3, 17, 3, road);
            rect(grid, 4, 7, 6, 7, 'a');
            rect(grid, 26, 19, 28, 20, 'a');
        } else if ("archive".equals(variant)) {
            rect(grid, 4, 7, 6, 8, 'y');
            rect(grid, 27, 7, 29, 8, 'y');
            rect(grid, 12, 8, 22, 8, 'a');
            rect(grid, 16, 7, 17, 8, 't');
        } else if ("highwall".equals(variant)) {
            rect(grid, 3, 2, 10, 2, 'x');
            rect(grid, 23, 2, 30, 2, 'x');
            rect(grid, 3, 21, 8, 21, 'x');
            rect(grid, 25, 21, 30, 21, 'x');
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
        grid[27][16] = road;
        grid[27][17] = road;
        grid[0][16] = road;
        grid[0][17] = road;
        grid[11][0] = road;
        grid[12][0] = road;
        grid[11][35] = road;
        grid[12][35] = road;
        return expandCityFootprint(grid, variant);
    }

    private char[][] expandCityFootprint(char[][] base, String variant) {
        int width = cityFootprintWidth(variant);
        int height = cityFootprintHeight(variant);
        if (width <= base[0].length && height <= base.length) {
            return base;
        }

        char[][] grid = filled(Math.max(width, base[0].length), Math.max(height, base.length), cityExpansionGroundTile(variant));
        for (int y = 0; y < base.length; y++) {
            System.arraycopy(base[y], 0, grid[y], 0, base[y].length);
        }

        int oldEast = base[0].length - 1;
        int oldSouth = base.length - 1;
        if (grid[0].length > base[0].length) {
            for (int y = 1; y < oldSouth; y++) {
                if (grid[y][oldEast] == 'x') {
                    grid[y][oldEast] = 'p';
                }
            }
        }
        if (grid.length > base.length) {
            for (int x = 1; x < oldEast; x++) {
                if (grid[oldSouth][x] == 'x') {
                    grid[oldSouth][x] = 'p';
                }
            }
        }
        if (grid[0].length > base[0].length && grid.length > base.length && grid[oldSouth][oldEast] == 'x') {
            grid[oldSouth][oldEast] = 'p';
        }

        border(grid, 'x');
        paintExpandedCitySurfaces(grid);
        applyCityFootprintNotches(grid, variant);
        softenExpandedCityCorners(grid, variant, base[0].length, base.length);
        markCityExits(grid);
        return grid;
    }

    private char cityExpansionGroundTile(String variant) {
        return switch (variant) {
            case "highwall" -> 'n';
            case "sanctum" -> 's';
            case "belltower" -> 'v';
            case "archive" -> 'y';
            default -> 'g';
        };
    }

    private int cityFootprintWidth(String variant) {
        return switch (variant) {
            case "riverside" -> 44;
            case "archive", "belltower" -> 42;
            case "sanctum" -> 40;
            case "highwall" -> 38;
            default -> 40;
        };
    }

    private int cityFootprintHeight(String variant) {
        return switch (variant) {
            case "highwall" -> 36;
            case "archive", "sanctum" -> 34;
            case "belltower" -> 32;
            case "riverside" -> 30;
            default -> 30;
        };
    }

    private void paintExpandedCitySurfaces(char[][] grid) {
        int width = grid[0].length;
        int height = grid.length;
        if (width > 36) {
            rectIf(grid, 32, 4, width - 3, 5, 'l', 'p', 'j', 'g', 'y', 's', 'n', 'v');
            rectIf(grid, 32, 7, width - 4, 10, 'j', 'p', 'l', 'g', 'y', 's', 'n', 'v');
            rectIf(grid, 32, 14, width - 4, 17, 'j', 'p', 'l', 'g', 'y', 's', 'n', 'v');
            rectIf(grid, 30, 19, width - 5, 22, 'j', 'p', 'l', 'g', 'y', 's', 'n', 'v');
            rectIf(grid, 34, 23, width - 3, Math.min(height - 3, 26), 'l', 'p', 'g', 'y', 's', 'n', 'v');
        }
        if (height > 28) {
            rectIf(grid, 3, 23, 13, height - 3, 'j', 'p', 'l', 'g', 'y', 's', 'n', 'v');
            rectIf(grid, 20, 23, 31, height - 3, 'j', 'p', 'l', 'g', 'y', 's', 'n', 'v');
            rectIf(grid, 5, height - 5, Math.min(width - 6, 34), height - 4, 'l', 'p', 'g', 'y', 's', 'n', 'v');
        }
    }

    private void extendCityRoads(char[][] grid, int baseWidth, int baseHeight) {
        char road = Terrain.COBBLESTONE_ROAD;
        int width = grid[0].length;
        int height = grid.length;
        rect(grid, 16, baseHeight - 1, 17, height - 1, road);
        rect(grid, baseWidth - 1, 11, width - 1, 12, road);
        rect(grid, 32, 6, width - 5, 6, road);
        rect(grid, 32, 18, width - 5, 18, road);
        rect(grid, 28, 21, width - 9, 21, road);
        if (height > baseHeight + 2) {
            rect(grid, 4, height - 6, width - 5, height - 6, road);
            rect(grid, 16, height - 6, 17, height - 1, road);
        }
    }

    private void applyCityFootprintNotches(char[][] grid, String variant) {
        int width = grid[0].length;
        int height = grid.length;
        if ("belltower".equals(variant)) {
            rect(grid, width - 9, 1, width - 2, 7, 'x');
            rect(grid, 1, height - 6, 7, height - 2, 'x');
        } else if ("archive".equals(variant)) {
            rect(grid, width - 7, 1, width - 2, 5, 'x');
        } else if ("sanctum".equals(variant)) {
            rect(grid, 1, height - 7, 8, height - 2, 'x');
        } else if ("highwall".equals(variant)) {
            rect(grid, width - 6, height - 8, width - 2, height - 2, 'x');
        }
    }

    private void softenExpandedCityCorners(char[][] grid, String variant, int baseWidth, int baseHeight) {
        char ground = cityExpansionGroundTile(variant);
        int width = grid[0].length;
        int height = grid.length;
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                char tile = grid[y][x];
                if (!isDecorativePavingTile(tile) || Terrain.connectingRoad(tile)) {
                    continue;
                }
                boolean expandedEdge = x >= baseWidth - 1 || y >= baseHeight - 1;
                int naturalNeighbors = nearbyTileCount(grid, x, y, ground, 1);
                int wallNeighbors = nearbyTileCount(grid, x, y, 'x', 1);
                int roadNeighbors = nearbyConnectingRoadCount(grid, x, y, 1);
                int roll = Math.floorMod(hash(x, y, variant.hashCode() + 2437), 100);
                if ((expandedEdge && naturalNeighbors >= 2 && roadNeighbors == 0 && roll < 72)
                        || (wallNeighbors >= 2 && naturalNeighbors >= 1 && roll < 82)
                        || (isHardCityCorner(grid, x, y) && roll < 88)) {
                    grid[y][x] = ground;
                }
            }
        }
    }

    private boolean isDecorativePavingTile(char tile) {
        return tile == 'p' || tile == 'j' || tile == 'l' || tile == 'a' || tile == 'C' || tile == 'G';
    }

    private boolean isHardCityCorner(char[][] grid, int x, int y) {
        return grid[y - 1][x] == 'x' && grid[y][x - 1] == 'x'
                || grid[y - 1][x] == 'x' && grid[y][x + 1] == 'x'
                || grid[y + 1][x] == 'x' && grid[y][x - 1] == 'x'
                || grid[y + 1][x] == 'x' && grid[y][x + 1] == 'x';
    }

    private int nearbyTileCount(char[][] grid, int x, int y, char wanted, int radius) {
        int count = 0;
        for (int yy = Math.max(0, y - radius); yy <= Math.min(grid.length - 1, y + radius); yy++) {
            for (int xx = Math.max(0, x - radius); xx <= Math.min(grid[0].length - 1, x + radius); xx++) {
                if ((xx != x || yy != y) && grid[yy][xx] == wanted) {
                    count++;
                }
            }
        }
        return count;
    }

    private int nearbyConnectingRoadCount(char[][] grid, int x, int y, int radius) {
        int count = 0;
        for (int yy = Math.max(0, y - radius); yy <= Math.min(grid.length - 1, y + radius); yy++) {
            for (int xx = Math.max(0, x - radius); xx <= Math.min(grid[0].length - 1, x + radius); xx++) {
                if ((xx != x || yy != y) && Terrain.connectingRoad(grid[yy][xx])) {
                    count++;
                }
            }
        }
        return count;
    }

    private void markCityExits(char[][] grid) {
        char road = Terrain.COBBLESTONE_ROAD;
        int width = grid[0].length;
        int height = grid.length;
        grid[0][16] = road;
        grid[0][17] = road;
        grid[height - 1][16] = road;
        grid[height - 1][17] = road;
        grid[11][0] = road;
        grid[12][0] = road;
        grid[11][width - 1] = road;
        grid[12][width - 1] = road;
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
        rectIf(grid, 3, 4, 32, 4, 'l', 'p', 'C');
        rectIf(grid, 3, 22, 32, 22, 'y', 'p', 'C', 'G');
        rectIf(grid, 4, 23, 9, 25, 'y', 'p', 'C', 'G');
        rectIf(grid, 25, 23, 31, 25, 'y', 'p', 'C', 'G');
        softenTownPaving(grid, variant);
        return grid;
    }

    private void softenTownPaving(char[][] grid, String variant) {
        char ground = cityExpansionGroundTile(variant);
        for (int y = 1; y < grid.length - 1; y++) {
            for (int x = 1; x < grid[0].length - 1; x++) {
                if (!isDecorativePavingTile(grid[y][x]) || Terrain.connectingRoad(grid[y][x])) {
                    continue;
                }
                int roadNeighbors = nearbyConnectingRoadCount(grid, x, y, 1);
                int naturalNeighbors = nearbyTileCount(grid, x, y, ground, 1)
                        + nearbyTileCount(grid, x, y, 'y', 1)
                        + nearbyTileCount(grid, x, y, 'g', 1);
                int roll = Math.floorMod(hash(x, y, variant.hashCode() + 2801), 100);
                if ((naturalNeighbors >= 3 && roadNeighbors <= 1 && roll < 78)
                        || (roadNeighbors == 0 && roll < 16)) {
                    grid[y][x] = ground;
                }
            }
        }
    }

    private void applySettlementLayoutPlan(MapArea area, String variant, boolean town) {
        openConfusingInteriorWallRuns(area, variant, town);
        stampSettlementMarketCore(area, variant, town);
        if ("highwall".equals(variant)) {
            stampHighwallDistrictPlan(area, town);
        }
    }

    private void openConfusingInteriorWallRuns(MapArea area, String variant, boolean town) {
        if (!"highwall".equals(variant)) {
            return;
        }
        int minimumRun = town ? 7 : 9;
        char surface = town ? 'C' : 'p';
        for (int y = 2; y < area.height() - 2; y++) {
            int x = 2;
            while (x < area.width() - 2) {
                if (area.tiles[y][x] != 'x') {
                    x++;
                    continue;
                }
                int start = x;
                while (x < area.width() - 2 && area.tiles[y][x] == 'x') {
                    x++;
                }
                int end = x - 1;
                int length = end - start + 1;
                if (length >= minimumRun) {
                    for (int xx = start + 3; xx <= end - 3; xx++) {
                        area.tiles[y][xx] = surface;
                    }
                }
            }
        }
    }

    private void stampSettlementMarketCore(MapArea area, String variant, boolean town) {
        int centerX = Math.min(area.width() - 7, 17);
        int centerY = Math.min(area.height() - 8, 12);
        char court = town ? 'G' : 'a';
        char edge = town ? 'C' : 'p';
        paintDistrictSurface(area, centerX - 6, centerY - 4, centerX + 7, centerY + 4, edge);
        paintDistrictSurface(area, centerX - 4, centerY - 2, centerX + 5, centerY + 2, court);
        if ("highwall".equals(variant)) {
            paintDistrictSurface(area, centerX - 5, centerY - 3, centerX + 6, centerY + 3, town ? 'C' : 'p');
        }
    }

    private void stampHighwallDistrictPlan(MapArea area, boolean town) {
        paintDistrictSurface(area, 11, 3, 22, 5, 'C');
        paintDistrictSurface(area, 4, 18, 15, 23, 'C');
        paintDistrictSurface(area, 20, 20, 32, Math.min(area.height() - 4, 29), 'p');
        paintDistrictSurface(area, 26, 13, Math.min(area.width() - 4, 34), 18, town ? 'C' : 'p');
        if (town) {
            paintDistrictSurface(area, 14, 14, 22, 18, 'G');
            paintDistrictSurface(area, 6, 24, 14, Math.min(area.height() - 4, 30), 'n');
        }
    }

    private void paintDistrictSurface(MapArea area, int x1, int y1, int x2, int y2, char tile) {
        int minX = Math.max(1, x1);
        int minY = Math.max(1, y1);
        int maxX = Math.min(area.width() - 2, x2);
        int maxY = Math.min(area.height() - 2, y2);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (canPaintDistrictSurfaceTile(area, x, y)) {
                    area.tiles[y][x] = tile;
                }
            }
        }
    }

    private boolean canPaintDistrictSurfaceTile(MapArea area, int x, int y) {
        char tile = area.tileAt(x, y);
        return Terrain.passable(tile)
                && !Terrain.connectingRoad(tile)
                && tile != 'w'
                && tile != '~'
                && tile != 't'
                && cityBuildingAt(area.id, x, y) == null
                && transitionAt(area.id, x, y) == null;
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
                    building("illuminator_house", 28, 4, 31, 5, "apothecary", 1),
                    building("inner_west_row", 7, 8, 10, 9, "row", 1),
                    building("west_scriptorium", 3, 8, 6, 10, "guild", 0),
                    building("inner_east_row", 23, 8, 26, 9, "row", 0),
                    building("east_scriptorium", 27, 8, 30, 10, "guild", 2),
                    building("west_archive_homes", 4, 14, 9, 17, "row", 1),
                    building("records_hall", 11, 15, 15, 17, "hall", 2),
                    building("east_archive_homes", 22, 14, 29, 17, "row", 0),
                    building("southwest_corner_row", 2, 19, 5, 20, "row", 2),
                    building("south_bindery", 8, 19, 13, 20, "workshop", 1),
                    building("south_mid_shop", 18, 19, 20, 20, "alchemist", 1),
                    building("south_reading_room", 21, 19, 26, 20, "guild", 0),
                    building("southeast_scribes", 28, 19, 30, 20, "house", 2)
            );
            case "highwall" -> List.of(
                    building("west_barracks", 3, 4, 8, 5, "barracks", 0),
                    building("north_armory", 10, 4, 14, 5, "blacksmith", 1),
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
                    building("south_armory", 8, 19, 13, 20, "blacksmith", 2),
                    building("south_mid_shop", 18, 19, 20, 20, "carpenter", 1),
                    building("south_quarters", 21, 19, 26, 20, "house", 0)
            );
            case "belltower" -> List.of(
                    building("west_chime_row", 3, 3, 8, 5, "row", 0),
                    building("bellwright_shop", 10, 3, 14, 5, "workshop", 2),
                    building("north_gate_homes", 18, 4, 20, 5, "house", 2),
                    building("north_chapel_row", 21, 3, 27, 5, "hall", 1),
                    building("east_chime_house", 28, 4, 31, 5, "house", 0),
                    building("inner_west_row", 7, 8, 10, 9, "row", 1),
                    building("west_market_house", 3, 8, 6, 10, "bakery", 1),
                    building("inner_east_row", 23, 8, 26, 9, "row", 0),
                    building("east_market_house", 27, 8, 30, 10, "shop", 2),
                    building("west_lower_chimes", 4, 14, 9, 17, "row", 1),
                    building("bellkeepers_lodge", 11, 15, 15, 17, "inn", 0),
                    building("east_lower_chimes", 22, 14, 29, 17, "row", 2),
                    building("southwest_corner_row", 2, 19, 5, 20, "row", 2),
                    building("south_bellfoundry", 8, 19, 13, 20, "blacksmith", 1),
                    building("south_mid_shop", 18, 19, 20, 20, "restaurant", 1),
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
                    building("east_scribe_house", 27, 8, 30, 10, "apothecary", 0),
                    building("west_lower_cells", 4, 14, 9, 17, "row", 0),
                    building("warden_hall", 11, 15, 15, 17, "hall", 1),
                    building("east_lower_cells", 22, 14, 29, 17, "row", 2),
                    building("southwest_corner_row", 2, 19, 5, 20, "row", 2),
                    building("south_apothecary", 8, 19, 13, 20, "apothecary", 1),
                    building("south_mid_shop", 18, 19, 20, 20, "restaurant", 1),
                    building("south_sanctum_homes", 21, 19, 26, 20, "house", 0),
                    building("southeast_cells", 28, 19, 30, 20, "house", 2)
            );
            default -> List.of(
                    building("river_warehouse", 3, 4, 7, 5, "warehouse", 0),
                    building("apothecary_row", 9, 4, 13, 5, "apothecary", 1),
                    building("north_gate_homes", 18, 4, 20, 5, "house", 2),
                    building("bridge_inn", 21, 4, 26, 5, "inn", 2),
                    building("east_storehouse", 28, 4, 31, 5, "warehouse", 0),
                    building("inner_west_row", 7, 8, 10, 9, "row", 1),
                    building("west_market_house", 3, 8, 6, 10, "bakery", 2),
                    building("inner_east_row", 23, 8, 26, 9, "row", 0),
                    building("east_market_house", 27, 8, 30, 10, "restaurant", 1),
                    building("west_homes", 4, 14, 9, 17, "row", 0),
                    building("river_hall", 11, 15, 15, 17, "hall", 1),
                    building("east_homes", 22, 14, 29, 17, "row", 2),
                    building("southwest_corner_row", 2, 19, 5, 20, "row", 2),
                    building("south_tavern", 8, 19, 13, 20, "inn", 1),
                    building("south_mid_shop", 18, 19, 20, 20, "fishing_hut", 1),
                    building("south_stables", 21, 19, 26, 20, "workshop", 0)
            );
        };
    }

    private CityBuilding building(String key, int x1, int y1, int x2, int y2, String style, int palette) {
        return new CityBuilding(key, x1, y1, x2, y2, style, palette);
    }

    private void addCityInfillBuildings(List<CityBuilding> buildings, MapArea area, String variant) {
        addScoredSettlementBuildings(buildings, area, variant, 3, settlementInfillLots(variant), "infill");
    }

    private void addTownLandmarkBuildings(List<CityBuilding> buildings, MapArea area, String variant) {
        CityBuilding landmark = switch (variant) {
            case "archive" -> building("moonspire_mage_tower", 16, 14, 20, 17, "mage_tower", 2);
            case "highwall" -> building("ironvale_arena", 16, 14, 21, 17, "arena", 1);
            case "belltower" -> building("reedwatch_bell_tower", 16, 14, 20, 17, "bell_tower", 0);
            case "sanctum" -> building("embermarket_sun_shrine", 16, 14, 20, 17, "sun_shrine", 2);
            default -> building("briarbridge_river_hall", 16, 14, 20, 17, "river_hall", 0);
        };
        if (canAddCityInfillBuilding(buildings, area, landmark)) {
            buildings.add(landmark);
        }
    }

    private void addMajorCivicBuildings(List<CityBuilding> buildings, MapArea area, String variant, boolean town) {
        CityBuilding[] candidates = switch (variant) {
            case "archive" -> new CityBuilding[]{
                    building(town ? "moonspire_grand_college" : "grand_archive_college", 31, 8, 38, 11, "hall", 2),
                    building("southern_observatory_manor", 22, 24, 30, 27, "mage_tower", 1)
            };
            case "highwall" -> new CityBuilding[]{
                    building(town ? "ironvale_command_keep" : "highwall_command_keep", 22, 24, 29, 28, "hall", 2),
                    building("marshal_arsenal", 5, 31, 12, 34, "barracks", 1)
            };
            case "belltower" -> new CityBuilding[]{
                    building(town ? "reedwatch_chime_manor" : "great_belfry_hall", 32, 14, 39, 17, "bell_tower", 0),
                    building("bellfounder_manor", 23, 24, 30, 27, "hall", 1)
            };
            case "sanctum" -> new CityBuilding[]{
                    building(town ? "embermarket_sun_court" : "sunspire_court", 28, 23, 36, 26, "sun_shrine", 2),
                    building("pilgrim_manor", 21, 29, 30, 32, "hall", 1)
            };
            default -> new CityBuilding[]{
                    building(town ? "briarbridge_manor" : "riverrun_manor", 34, 14, 41, 17, "river_hall", 2),
                    building("east_wharf_hall", 3, 23, 10, 26, "hall", 0)
            };
        };
        int added = 0;
        int limit = town ? 1 : 2;
        for (CityBuilding candidate : candidates) {
            if (added >= limit) {
                return;
            }
            if (canAddCityInfillBuilding(buildings, area, candidate)) {
                buildings.add(candidate);
                added++;
            }
        }
    }

    private boolean canAddCityInfillBuilding(List<CityBuilding> buildings, MapArea area, CityBuilding candidate) {
        for (CityBuilding building : buildings) {
            if (rectsOverlap(candidate.x1(), candidate.y1(), candidate.x2(), candidate.y2(),
                    building.x1(), building.y1(), building.x2(), building.y2())) {
                return false;
            }
        }
        for (int y = candidate.y1(); y <= candidate.y2(); y++) {
            for (int x = candidate.x1(); x <= candidate.x2(); x++) {
                char tile = area.tileAt(x, y);
                if (Terrain.connectingRoad(tile) || tile == 'm' || tile == 'w' || tile == 'x' || tile == 't') {
                    return false;
                }
            }
        }
        return true;
    }

    private void tuneSettlementBuildingDensity(List<CityBuilding> buildings, MapArea area, String id, String variant,
                                               boolean city, boolean town) {
        int profile = Math.floorMod(id.hashCode(), 3);
        if (town && profile == 0 && buildings.size() > 12) {
            buildings.removeIf(building -> building.key().equals("southwest_corner_row"));
        }
        if (!city) {
            return;
        }
        int target = town ? profile : Math.min(3, profile + 1);
        addScoredSettlementBuildings(buildings, area, variant, target, settlementDensityLots(variant), "extra");
    }

    private void addScoredSettlementBuildings(List<CityBuilding> buildings, MapArea area, String variant, int limit,
                                              List<BuildingLotCandidate> lots, String prefix) {
        if (limit <= 0) {
            return;
        }
        List<ScoredBuildingCandidate> scored = new ArrayList<>();
        int salt = area.id.hashCode() ^ variant.hashCode() ^ prefix.hashCode();
        for (BuildingLotCandidate lot : lots) {
            for (String style : lot.styles()) {
                CityBuilding candidate = building(prefix + "_" + lot.key() + "_" + style, lot.x1(), lot.y1(),
                        lot.x2(), lot.y2(), style, Math.floorMod(lot.palette() + style.hashCode(), 3));
                if (!canAddCityInfillBuilding(buildings, area, candidate)) {
                    continue;
                }
                int score = settlementBuildingCandidateScore(area, candidate, variant, salt);
                if (score > 0) {
                    scored.add(new ScoredBuildingCandidate(candidate, score));
                }
            }
        }
        scored.sort(Comparator
                .comparingInt(ScoredBuildingCandidate::score).reversed()
                .thenComparing(candidate -> candidate.building().key()));
        int added = 0;
        Set<String> usedLots = new HashSet<>();
        for (ScoredBuildingCandidate candidate : scored) {
            if (added >= limit) {
                return;
            }
            CityBuilding building = candidate.building();
            String lotKey = building.x1() + ":" + building.y1() + ":" + building.x2() + ":" + building.y2();
            if (usedLots.contains(lotKey) || !canAddCityInfillBuilding(buildings, area, building)) {
                continue;
            }
            buildings.add(building);
            usedLots.add(lotKey);
            added++;
        }
    }

    private List<BuildingLotCandidate> settlementInfillLots(String variant) {
        List<BuildingLotCandidate> lots = new ArrayList<>(List.of(
                lot("north_gap", 14, 4, 15, 5, 1, "house", "bakery", "alchemist", "carpenter"),
                lot("east_corner", 31, 14, 32, 16, 2, "house", "shop", "apothecary", "workshop", "restaurant"),
                lot("south_lane", 14, 19, 15, 20, 0, "house", "shop", "carpenter", "bakery"),
                lot("west_lane", 1, 14, 2, 16, 0, "house", "forestry_hut", "farmstead"),
                lot("east_lane", 31, 8, 32, 10, 1, "workshop", "house", "bakery")
        ));
        if ("highwall".equals(variant)) {
            lots.add(lot("armory_yard", 14, 19, 15, 20, 1, "blacksmith", "watchtower", "barracks"));
        } else if ("archive".equals(variant)) {
            lots.add(lot("ink_alley", 31, 14, 32, 16, 2, "apothecary", "workshop", "guild"));
        } else if ("belltower".equals(variant)) {
            lots.add(lot("bellwright_corner", 31, 8, 32, 10, 1, "workshop", "blacksmith", "bakery"));
        } else if ("sanctum".equals(variant)) {
            lots.add(lot("herb_oratory", 14, 4, 15, 5, 1, "sun_shrine", "apothecary", "house"));
        } else {
            lots.add(lot("wharf_trade", 31, 14, 32, 16, 0, "warehouse", "fishing_hut", "restaurant"));
        }
        return lots;
    }

    private List<BuildingLotCandidate> settlementDensityLots(String variant) {
        List<BuildingLotCandidate> lots = new ArrayList<>(List.of(
                lot("laneway", 1, 14, 2, 16, 0, "house", "forestry_hut", "farmstead"),
                lot("corner_trade", 31, 14, 32, 16, 2, "shop", "restaurant", "alchemist", "carpenter"),
                lot("canal_store", 14, 4, 15, 5, 1, "warehouse", "bakery", "carpenter"),
                lot("south_lean_to", 27, 19, 30, 20, 1, "house", "row", "workshop", "farmstead", "restaurant")
        ));
        if ("highwall".equals(variant)) {
            lots.add(lot("forge_slot", 8, 19, 13, 20, 2, "blacksmith", "workshop", "warehouse"));
        } else if ("sanctum".equals(variant)) {
            lots.add(lot("healer_slot", 8, 19, 13, 20, 1, "apothecary", "sun_shrine", "guild"));
        } else if ("archive".equals(variant)) {
            lots.add(lot("binder_slot", 8, 19, 13, 20, 1, "carpenter", "guild", "alchemist"));
        } else if ("belltower".equals(variant)) {
            lots.add(lot("foundry_slot", 8, 19, 13, 20, 1, "blacksmith", "workshop", "warehouse"));
        } else {
            lots.add(lot("green_trade_slot", 8, 19, 13, 20, 1, "restaurant", "bakery", "carpenter"));
        }
        return lots;
    }

    private BuildingLotCandidate lot(String key, int x1, int y1, int x2, int y2, int palette, String... styles) {
        return new BuildingLotCandidate(key, x1, y1, x2, y2, palette, List.of(styles));
    }

    private int settlementBuildingCandidateScore(MapArea area, CityBuilding building, String variant, int salt) {
        int score = 25 + Math.floorMod(hash(building.x1(), building.y1(), salt + building.style().hashCode()), 17);
        int frontage = buildingFrontageRoadCount(area, building);
        int green = nearbyTileCount(area.tiles, building.anchor().x(), building.anchor().y(), 'g', 3)
                + nearbyTileCount(area.tiles, building.anchor().x(), building.anchor().y(), 'f', 3)
                + nearbyTileCount(area.tiles, building.anchor().x(), building.anchor().y(), 'y', 2);
        int market = nearbyTileCount(area.tiles, building.anchor().x(), building.anchor().y(), 'a', 3)
                + nearbyTileCount(area.tiles, building.anchor().x(), building.anchor().y(), 'G', 3);
        int water = nearbyTileCount(area.tiles, building.anchor().x(), building.anchor().y(), 'w', 5);
        boolean edge = building.x1() <= 2 || building.x2() >= area.width() - 3
                || building.y1() <= 4 || building.y2() >= area.height() - 4;
        int width = building.width();
        int depth = building.depth();
        switch (building.style()) {
            case "watchtower", "mage_tower", "bell_tower" -> {
                score += (depth >= 3 ? 22 : -15) + (width <= 3 ? 20 : -6) + (frontage > 0 ? 8 : 0);
                if ("belltower".equals(variant) || "archive".equals(variant)) score += 18;
                if (green > 8) score -= 10;
            }
            case "sun_shrine", "shrine" -> {
                score += (green > 4 ? 18 : 0) + (frontage > 0 ? 12 : 0) + ("sanctum".equals(variant) ? 28 : 0);
                if (width < 3) score -= 12;
            }
            case "blacksmith" -> {
                score += ("highwall".equals(variant) || "belltower".equals(variant) ? 26 : 4) + frontage * 3;
                if (green > 10) score -= 14;
            }
            case "bakery", "restaurant" -> score += market * 4 + frontage * 5 + ("sanctum".equals(variant) ? 6 : 0);
            case "apothecary", "alchemist" -> score += green * 3 + ("sanctum".equals(variant) || "archive".equals(variant) ? 18 : 0);
            case "forestry_hut", "farmstead" -> {
                score += green * 4 + (edge ? 12 : -4);
                if (market > 4) score -= 8;
            }
            case "fishing_hut" -> score += water * 5 + (edge ? 10 : 0);
            case "warehouse" -> score += (width >= 4 ? 16 : 0) + frontage * 4 + water * 2 + (edge ? 10 : 0);
            case "workshop", "carpenter", "shop" -> score += frontage * 4 + market * 3 + (width >= 3 ? 8 : 0);
            case "guild" -> score += ("archive".equals(variant) ? 20 : 4) + frontage * 2;
            case "row", "house" -> score += (green > 3 ? 8 : 0) + (market > 5 ? -4 : 0);
            case "barracks" -> score += ("highwall".equals(variant) ? 24 : 2) + frontage * 3;
            case "inn" -> score += frontage * 5 + market * 2 + (width >= 4 ? 8 : 0);
            default -> score += frontage * 2;
        }
        if (frontage == 0 && !List.of("forestry_hut", "farmstead").contains(building.style())) {
            score -= 18;
        }
        return score;
    }

    private int buildingFrontageRoadCount(MapArea area, CityBuilding building) {
        int count = 0;
        int y = building.y2() + 1;
        for (int x = building.x1(); x <= building.x2(); x++) {
            if (Terrain.connectingRoad(area.tileAt(x, y))) {
                count++;
            }
        }
        return count;
    }

    private List<CityBuilding> splitOversizedSettlementBuildings(List<CityBuilding> buildings) {
        List<CityBuilding> split = new ArrayList<>();
        for (CityBuilding building : buildings) {
            if (!shouldSplitSettlementBuilding(building)) {
                split.add(building);
                continue;
            }
            int part = 0;
            int x = building.x1();
            while (x <= building.x2()) {
                int remaining = building.x2() - x + 1;
                int chunk = Math.min(splitBuildingChunkWidth(building), remaining);
                if (remaining - chunk == 1) {
                    chunk++;
                }
                String key = building.key() + "_part_" + (char) ('a' + part);
                split.add(new CityBuilding(
                        key,
                        x,
                        building.y1(),
                        x + chunk - 1,
                        building.y2(),
                        building.style(),
                        Math.floorMod(building.palette() + part, 3)
                ));
                x += chunk + 1;
                part++;
            }
        }
        return split;
    }

    private boolean shouldSplitSettlementBuilding(CityBuilding building) {
        if (!List.of("row", "guild", "house", "shop").contains(building.style())) {
            return false;
        }
        return building.width() >= 6 && building.depth() <= 4;
    }

    private int splitBuildingChunkWidth(CityBuilding building) {
        return switch (building.style()) {
            case "row", "guild" -> 3;
            default -> 2;
        };
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

    private List<CityBuilding> villageBuildingTemplates(String id, String variant) {
        List<CityBuilding> buildings = new ArrayList<>(List.of(
                building("west_cottage", 4, 4, 7, 6, "house", 0),
                building("north_cottage", 9, 3, 12, 5, "shop", 1),
                building("east_cottage", 16, 4, 20, 6, "house", 2),
                building("southwest_cottage", 5, 12, 9, 14, "row", 0),
                building("south_cottage", 12, 13, 15, 15, "house", 1),
                building("southeast_cottage", 18, 11, 22, 14, "inn", 2)
        ));
        int profile = Math.floorMod(id.hashCode(), 3);
        if (profile == 0) {
            buildings.remove(buildings.size() - 1);
        } else if (profile == 2) {
            buildings.add(building("east_outbuilding", 24, 4, 27, 6, "house", 1));
            buildings.add(building("south_workyard", 23, 12, 27, 14, "shop", 2));
        } else if ("desert".equals(variant) || "marsh".equals(variant)) {
            buildings.add(building("edge_worker_hut", 23, 12, 25, 14, "house", 0));
        }
        return buildings;
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
        addVillageRoadNetwork(grid, Terrain.DIRT_ROAD);
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
        addPlayerVillageBuildableClearings(grid);
        return grid;
    }

    private void addVillageRoadNetwork(char[][] grid, char road) {
        rect(grid, 13, 0, 14, 27, road);
        rect(grid, 0, 9, 35, 10, road);
        rect(grid, 7, 15, 14, 15, road);
        rect(grid, 14, 15, 22, 15, road);
        rect(grid, 4, 21, 24, 21, road);
    }

    private void connectSettlementBuildingPaths(MapArea area, List<CityBuilding> buildings, char roadTile) {
        if (area == null || buildings == null || buildings.isEmpty()) {
            return;
        }
        int salt = area.id.hashCode();
        for (CityBuilding building : buildings) {
            List<TilePoint> doors = cityBuildingDoorTiles(building);
            for (int index = 0; index < doors.size(); index++) {
                TilePoint door = doors.get(index);
                TilePoint approach = nearestSettlementPathStart(area, door.x(), door.y() + 1, salt + index);
                if (approach == null) {
                    continue;
                }
                carvePathToNearestSettlementRoad(area, approach, salt + building.key().hashCode() + index * 37, roadTile);
            }
            stampBuildingFrontagePath(area, building, salt + building.key().hashCode(), roadTile);
        }
    }

    private TilePoint nearestSettlementPathStart(MapArea area, int x, int y, int salt) {
        if (canUseSettlementPathTile(area, x, y)) {
            return new TilePoint(x, y);
        }
        TilePoint best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int radius = 1; radius <= 3; radius++) {
            for (int yy = y - radius; yy <= y + radius; yy++) {
                for (int xx = x - radius; xx <= x + radius; xx++) {
                    int distance = Math.abs(xx - x) + Math.abs(yy - y);
                    if (distance > radius || !canUseSettlementPathTile(area, xx, yy)) {
                        continue;
                    }
                    int score = distance * 100 + Math.floorMod(hash(xx, yy, salt), 17);
                    if (score < bestScore) {
                        bestScore = score;
                        best = new TilePoint(xx, yy);
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private void carvePathToNearestSettlementRoad(MapArea area, TilePoint start, int salt, char roadTile) {
        if (isRoadTile(area.tileAt(start.x(), start.y()))) {
            stampSettlementPathTile(area, start.x(), start.y(), salt, roadTile);
            return;
        }
        boolean[][] visited = new boolean[area.height()][area.width()];
        TilePoint[][] previous = new TilePoint[area.height()][area.width()];
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        visited[start.y()][start.x()] = true;
        queue.add(start);

        TilePoint end = null;
        while (!queue.isEmpty()) {
            TilePoint point = queue.removeFirst();
            if (!point.equals(start) && isRoadTile(area.tileAt(point.x(), point.y()))) {
                end = point;
                break;
            }
            int[][] directions = settlementPathDirections(point.x(), point.y(), salt);
            for (int[] direction : directions) {
                int nx = point.x() + direction[0];
                int ny = point.y() + direction[1];
                if (!canUseSettlementPathTile(area, nx, ny) || visited[ny][nx]) {
                    continue;
                }
                visited[ny][nx] = true;
                previous[ny][nx] = point;
                queue.addLast(new TilePoint(nx, ny));
            }
        }
        if (end == null) {
            return;
        }
        TilePoint point = end;
        while (point != null) {
            stampSettlementPathTile(area, point.x(), point.y(), salt, roadTile);
            if (point.equals(start)) {
                break;
            }
            point = previous[point.y()][point.x()];
        }
    }

    private int[][] settlementPathDirections(int x, int y, int salt) {
        int[][] base = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        int[][] directions = new int[base.length][2];
        int offset = Math.floorMod(hash(x, y, salt), base.length);
        for (int i = 0; i < base.length; i++) {
            directions[i] = base[(i + offset) % base.length];
        }
        return directions;
    }

    private void stampBuildingFrontagePath(MapArea area, CityBuilding building, int salt, char roadTile) {
        int y = building.y2() + 1;
        if (y < 0 || y >= area.height()) {
            return;
        }
        for (int x = building.x1(); x <= building.x2(); x++) {
            stampSettlementPathTile(area, x, y, salt, roadTile);
        }
    }

    private boolean canUseSettlementPathTile(MapArea area, int x, int y) {
        if (x < 0 || y < 0 || x >= area.width() || y >= area.height()) {
            return false;
        }
        if (cityBuildingAt(area.id, x, y) != null) {
            return false;
        }
        char tile = area.tileAt(x, y);
        return tile != 'w' && tile != '~' && tile != 'x' && tile != 'm' && tile != 'o' && tile != 'h' && tile != 't'
                && (Terrain.passable(tile) || isRoadTile(tile));
    }

    private void stampSettlementPathTile(MapArea area, int x, int y, int salt, char roadTile) {
        if (!canUseSettlementPathTile(area, x, y)) {
            return;
        }
        if (isRoadTile(area.tileAt(x, y))) {
            return;
        }
        area.tiles[y][x] = roadTile;
    }

    private void normalizeCityRoadsAndPaving(MapArea area, String variant) {
        if (area == null || !"city".equals(area.kind)) {
            return;
        }
        reinforceCityRoadBackbone(area);
        removeOrphanDecorativePaving(area, variant);
        addRoadsidePavingVariety(area, variant);
    }

    private void reinforceCityRoadBackbone(MapArea area) {
        char road = Terrain.COBBLESTONE_ROAD;
        int width = area.width();
        int height = area.height();
        for (int y = 0; y < height; y++) {
            stampCityRoadIfOpen(area, 16, y, road);
            stampCityRoadIfOpen(area, 17, y, road);
        }
        for (int x = 0; x < width; x++) {
            stampCityRoadIfOpen(area, x, 11, road);
            stampCityRoadIfOpen(area, x, 12, road);
        }
        for (int x = 2; x < width - 2; x++) {
            stampCityRoadIfOpen(area, x, 6, road);
            stampCityRoadIfOpen(area, x, 18, road);
            if (height > 28) {
                stampCityRoadIfOpen(area, x, height - 6, road);
            }
        }
        int[] secondaryColumns = {6, 10, 24, 28, Math.max(4, width - 8)};
        for (int column : secondaryColumns) {
            if (column <= 1 || column >= width - 1) {
                continue;
            }
            for (int y = 4; y < height - 4; y++) {
                if (y == 11 || y == 12 || y == 6 || y == 18 || (height > 28 && y == height - 6)
                        || nearbyConnectingRoadCount(area.tiles, column, y, 2) > 0
                        || Math.floorMod(hash(column, y, area.id.hashCode() ^ 5843), 100) < 64) {
                    stampCityRoadIfOpen(area, column, y, road);
                }
            }
        }
        for (int y = 0; y < height; y++) {
            if (y % 7 == 0 || y == height - 1) {
                stampCityRoadIfOpen(area, 16, y, road);
                stampCityRoadIfOpen(area, 17, y, road);
            }
        }
    }

    private void stampCityRoadIfOpen(MapArea area, int x, int y, char road) {
        if (x < 0 || y < 0 || x >= area.width() || y >= area.height()
                || cityBuildingAt(area.id, x, y) != null
                || transitionAt(area.id, x, y) != null
                || area.tileAt(x, y) == 'w' || area.tileAt(x, y) == '~' || area.tileAt(x, y) == 'x') {
            return;
        }
        area.tiles[y][x] = road;
    }

    private void removeOrphanDecorativePaving(MapArea area, String variant) {
        char ground = cityExpansionGroundTile(variant);
        char[][] next = copyTiles(area.tiles);
        for (int y = 1; y < area.height() - 1; y++) {
            for (int x = 1; x < area.width() - 1; x++) {
                char tile = area.tiles[y][x];
                if (!isDecorativePavingTile(tile) || Terrain.connectingRoad(tile)) {
                    continue;
                }
                int roadNeighbors = nearbyConnectingRoadCount(area.tiles, x, y, 1);
                int roadReach = nearbyConnectingRoadCount(area.tiles, x, y, 2);
                int sameNeighbors = nearbyTileCount(area.tiles, x, y, tile, 1);
                boolean nearBuildingFront = cityBuildingAt(area.id, x, y - 1) != null
                        || cityBuildingAt(area.id, x, y + 1) != null;
                int roll = Math.floorMod(hash(x, y, area.id.hashCode() ^ variant.hashCode() ^ 9041), 100);
                if (roadNeighbors == 0 && (!nearBuildingFront || roadReach == 0)) {
                    next[y][x] = ground;
                } else if (sameNeighbors >= 3 && roadNeighbors <= 1 && roll < 72) {
                    next[y][x] = ground;
                } else if (sameNeighbors >= 2 && roadNeighbors == 0 && roll < 88) {
                    next[y][x] = ground;
                }
            }
        }
        for (int y = 0; y < area.height(); y++) {
            System.arraycopy(next[y], 0, area.tiles[y], 0, area.width());
        }
    }

    private void addRoadsidePavingVariety(MapArea area, String variant) {
        for (int y = 1; y < area.height() - 1; y++) {
            for (int x = 1; x < area.width() - 1; x++) {
                char tile = area.tileAt(x, y);
                if (Terrain.connectingRoad(tile) || cityBuildingAt(area.id, x, y) != null
                        || transitionAt(area.id, x, y) != null || area.propAt(x, y) != null) {
                    continue;
                }
                int roadNeighbors = nearbyConnectingRoadCount(area.tiles, x, y, 1);
                if (roadNeighbors == 0 || !Terrain.passable(tile)) {
                    continue;
                }
                area.tiles[y][x] = roadsidePavingTileAvoidingMatches(area.tiles, x, y, variant);
            }
        }
    }

    private char roadsidePavingTileAvoidingMatches(char[][] tiles, int x, int y, String variant) {
        char first = roadsidePavingTile(variant, x, y);
        if (nearbyTileCount(tiles, x, y, first, 1) < 2) {
            return first;
        }
        char[] alternatives = roadsidePavingAlternatives(variant);
        for (char alternative : alternatives) {
            if (nearbyTileCount(tiles, x, y, alternative, 1) < 2) {
                return alternative;
            }
        }
        return first;
    }

    private char[] roadsidePavingAlternatives(String variant) {
        return switch (variant) {
            case "archive" -> new char[]{'y', 'p', 'l', 'C'};
            case "sanctum" -> new char[]{'s', 'b', 'p', 'C'};
            case "highwall" -> new char[]{'n', 'p', 'l', 'C'};
            case "belltower" -> new char[]{'v', 'y', 'l', 'C'};
            default -> new char[]{'p', 'l', 'C', 'y'};
        };
    }

    private char roadsidePavingTile(String variant, int x, int y) {
        int roll = Math.floorMod(hash(x, y, variant.hashCode() ^ 7027), 100);
        if ("archive".equals(variant)) {
            return roll < 50 ? 'y' : (roll < 76 ? 'p' : 'l');
        }
        if ("sanctum".equals(variant)) {
            return roll < 48 ? 's' : (roll < 72 ? 'b' : 'p');
        }
        if ("highwall".equals(variant)) {
            return roll < 50 ? 'n' : (roll < 75 ? 'p' : 'l');
        }
        if ("belltower".equals(variant)) {
            return roll < 45 ? 'v' : (roll < 72 ? 'y' : 'l');
        }
        return roll < 48 ? 'p' : (roll < 76 ? 'l' : 'C');
    }

    private char[][] copyTiles(char[][] tiles) {
        char[][] copy = new char[tiles.length][tiles[0].length];
        for (int y = 0; y < tiles.length; y++) {
            System.arraycopy(tiles[y], 0, copy[y], 0, tiles[y].length);
        }
        return copy;
    }

    private void applyRoadMaterialSegments(MapArea area, char material, int salt, int chance) {
        if (area == null) {
            return;
        }
        if ("village".equals(area.kind) && material == Terrain.VILLAGE_ROAD) {
            return;
        }
        for (int y = 0; y < area.height(); y++) {
            for (int x = 0; x < area.width(); x++) {
                if (area.tiles[y][x] != Terrain.DIRT_ROAD) {
                    continue;
                }
                if (roadMaterialSegmentSelected(area, x, y, salt, chance)) {
                    area.tiles[y][x] = material;
                }
            }
        }
    }

    private boolean roadMaterialSegmentSelected(MapArea area, int x, int y, int salt, int chance) {
        boolean vertical = Terrain.connectingRoad(area.tileAt(x, y - 1)) || Terrain.connectingRoad(area.tileAt(x, y + 1));
        boolean horizontal = Terrain.connectingRoad(area.tileAt(x - 1, y)) || Terrain.connectingRoad(area.tileAt(x + 1, y));
        int along;
        int cross;
        if (vertical && !horizontal) {
            along = y;
            cross = x;
        } else if (horizontal && !vertical) {
            along = x;
            cross = y;
        } else if (Math.floorMod(hash(x, y, salt), 2) == 0) {
            along = x;
            cross = y;
        } else {
            along = y;
            cross = x;
        }
        int shift = Math.floorMod(hash(cross, salt, 271), 7);
        int period = 11;
        int local = Math.floorMod(along + shift, period);
        int block = Math.floorDiv(along + shift, period);
        int roll = Math.floorMod(hash(cross, block, salt), 100);
        int length = 4 + Math.floorMod(hash(cross, block, salt + 17), 3);
        return roll < chance && local < length;
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

    private char[][] dungeonTiles(String theme, int depth, int floors, String mapId) {
        return switch (theme) {
            case "cave" -> caveDungeonTiles(depth, floors, mapId);
            case "crypt" -> cryptDungeonTiles(depth, floors, mapId);
            case "abandoned_castle" -> castleDungeonTiles(depth, floors, mapId);
            case "prison" -> prisonDungeonTiles(depth, floors, mapId);
            case "sewer" -> sewerDungeonTiles(depth, floors, mapId);
            case "goblin_camp", "bandit_camp" -> strongholdDungeonTiles(theme, depth, floors, mapId);
            default -> cryptDungeonTiles(depth, floors, mapId);
        };
    }

    private char[][] cryptDungeonTiles(int depth, int floors, String mapId) {
        char[][] grid = structuredDungeonTiles(depth, floors, mapId, 'Q');
        rectIf(grid, 5, 5, 9, 8, 'M', 'Q', 'd', 'F', 'R');
        rectIf(grid, 13, 11, 22, 14, depth == floors ? 'S' : 'D', 'Q', 'd', 'F', 'R', 'M');
        return grid;
    }

    private char[][] castleDungeonTiles(int depth, int floors, String mapId) {
        char[][] grid = structuredDungeonTiles(depth, floors, mapId, 'H');
        rectIf(grid, 12, 10, 23, 15, depth == floors ? 'S' : 'D', 'H', 'd', 'F', 'R');
        rectIf(grid, 4, 3, 11, 7, 'F', 'H', 'd', 'R');
        rectIf(grid, 24, 4, 32, 8, 'D', 'H', 'd', 'F');
        return grid;
    }

    private char[][] prisonDungeonTiles(int depth, int floors, String mapId) {
        char[][] grid = filled(36, 26, 'x');
        rect(grid, 1, 11, 9, 14, 'I');
        rect(grid, 8, 12, 30, 13, 'I');
        rect(grid, 29, 12, 34, 20, 'I');
        rect(grid, 13, 5, 22, 9, 'I');
        rect(grid, 13, 16, 22, 21, 'I');
        for (int x = 5; x <= 29; x += 4) {
            rect(grid, x, 4, x + 2, 9, 'I');
            rect(grid, x, 16, x + 2, 22, 'I');
            if (x + 2 < grid[0].length - 1) {
                grid[10][x + 1] = 'I';
                grid[15][x + 1] = 'I';
            }
        }
        rect(grid, 22, 6, 30, 9, depth == floors ? 'S' : 'I');
        rect(grid, 4, 19, 9, 22, 'R');
        ensureDungeonEndpoints(grid, depth, floors, 'I');
        decorateDungeonFloors(grid, depth, 1800 + mapId.hashCode(), 'I');
        return grid;
    }

    private char[][] sewerDungeonTiles(int depth, int floors, String mapId) {
        char[][] grid = filled(36, 26, 'x');
        rect(grid, 1, 11, 8, 14, 'J');
        rect(grid, 8, 11, 34, 13, 'J');
        rect(grid, 16, 4, 20, 22, 'J');
        rect(grid, 4, 4, 14, 8, 'J');
        rect(grid, 22, 4, 32, 8, 'J');
        rect(grid, 4, 18, 14, 22, 'J');
        rect(grid, 22, 17, 34, 22, 'J');
        rect(grid, 9, 5, 11, 21, 'W');
        rect(grid, 24, 5, 26, 21, 'W');
        rect(grid, 11, 10, 24, 11, 'W');
        rect(grid, 11, 14, 24, 15, 'W');
        rect(grid, 17, 11, 19, 14, 'J');
        if (depth == floors) {
            rectIf(grid, 27, 18, 33, 21, 'S', 'J');
        } else {
            rectIf(grid, 5, 5, 13, 7, 'M', 'J');
        }
        ensureDungeonEndpoints(grid, depth, floors, 'J');
        decorateDungeonFloors(grid, depth, 1900 + mapId.hashCode(), 'J');
        return grid;
    }

    private char[][] caveDungeonTiles(int depth, int floors, String mapId) {
        char[][] grid = filled(36, 26, 'O');
        int salt = 2100 + depth * 151 + mapId.hashCode();
        List<TilePoint> rooms = List.of(
                new TilePoint(5, 12), new TilePoint(9, 5), new TilePoint(17, 7),
                new TilePoint(24, 11), new TilePoint(11, 19), new TilePoint(22, 19),
                new TilePoint(31, 18)
        );
        for (int i = 0; i < rooms.size(); i++) {
            TilePoint room = rooms.get(i);
            carveDungeonRoom(grid, room, 3 + Math.floorMod(hash(i, depth, salt), 3), 2 + Math.floorMod(hash(depth, i, salt), 2), salt + i * 19);
            if (i > 0) {
                carveDungeonCorridor(grid, rooms.get(i - 1), room, salt + i * 31);
            }
        }
        replaceTile(grid, 'd', 'N');
        rectIf(grid, 7, 4, 11, 6, 'M', 'N');
        rectIf(grid, 19, 18, 25, 21, depth == floors ? 'S' : 'R', 'N');
        rect(grid, 27, 8, 29, 12, 'W');
        ensureDungeonEndpoints(grid, depth, floors, 'N');
        decorateDungeonFloors(grid, depth, salt, 'N');
        return grid;
    }

    private char[][] strongholdDungeonTiles(String theme, int depth, int floors, String mapId) {
        char floor = theme.equals("goblin_camp") ? 'R' : 'H';
        char[][] grid = structuredDungeonTiles(depth, floors, mapId, floor);
        rectIf(grid, 4, 18, 12, 22, theme.equals("goblin_camp") ? 'M' : 'I', floor, 'd', 'F', 'R', 'H');
        rectIf(grid, 28, 15, 34, 22, depth == floors ? 'S' : floor, floor, 'd', 'F', 'R', 'H');
        return grid;
    }

    private char[][] structuredDungeonTiles(int depth, int floors, String mapId, char baseFloor) {
        char[][] grid = filled(36, 26, 'x');
        int salt = 1100 + depth * 137 + mapId.hashCode();

        rect(grid, 1, 11, 8, 14, baseFloor);
        rect(grid, 4, 3, 11, 7, baseFloor);
        rect(grid, 13, 3, 21, 6, baseFloor);
        rect(grid, 24, 4, 32, 8, baseFloor);
        rect(grid, 12, 10, 23, 15, baseFloor);
        rect(grid, 4, 18, 12, 22, baseFloor);
        rect(grid, 16, 18, 24, 22, baseFloor);
        rect(grid, 28, 15, 34, 22, baseFloor);

        rect(grid, 8, 12, 14, 13, baseFloor);
        rect(grid, 7, 7, 8, 12, baseFloor);
        rect(grid, 11, 5, 13, 6, baseFloor);
        rect(grid, 21, 5, 25, 6, baseFloor);
        rect(grid, 22, 8, 23, 12, baseFloor);
        rect(grid, 8, 14, 9, 18, baseFloor);
        rect(grid, 12, 20, 16, 21, baseFloor);
        rect(grid, 20, 15, 21, 18, baseFloor);
        rect(grid, 24, 19, 28, 20, baseFloor);
        rect(grid, 31, 8, 32, 15, baseFloor);

        if (depth == 1) {
            rectIf(grid, 13, 11, 22, 14, 'D', baseFloor);
            rectIf(grid, 5, 4, 10, 6, 'M', baseFloor);
        } else if (depth == 2) {
            rect(grid, 15, 10, 20, 13, 'w');
            rect(grid, 17, 12, 18, 13, baseFloor);
            rectIf(grid, 28, 16, 33, 21, 'S', baseFloor);
        } else {
            rectIf(grid, 12, 10, 23, 15, 'F', baseFloor);
            rectIf(grid, 28, 15, 34, 22, 'S', baseFloor);
            rectIf(grid, 4, 18, 12, 22, 'R', baseFloor);
        }

        decorateDungeonFloors(grid, depth, salt, baseFloor);
        ensureDungeonEndpoints(grid, depth, floors, baseFloor);
        return grid;
    }

    private void ensureDungeonEndpoints(char[][] grid, int depth, int floors, char floorTile) {
        rect(grid, 1, 11, 4, 14, floorTile);
        grid[12][1] = floorTile;
        grid[12][2] = floorTile;
        if (depth < floors) {
            rect(grid, 32, 17, 35, 20, floorTile);
            grid[18][34] = floorTile;
            grid[18][33] = floorTile;
        }
    }

    private void replaceTile(char[][] grid, char from, char to) {
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                if (grid[y][x] == from) {
                    grid[y][x] = to;
                }
            }
        }
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

    private void decorateDungeonFloors(char[][] grid, int depth, int salt, char baseFloor) {
        for (int y = 1; y < grid.length - 1; y++) {
            for (int x = 1; x < grid[0].length - 1; x++) {
                if (grid[y][x] != baseFloor) {
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
        rectIf(grid, 21, 14, 26, 17, depth > 1 ? 'S' : 'D', baseFloor, 'F', 'M', 'R', 'L');
        rectIf(grid, 5, 5, 9, 8, 'M', baseFloor, 'F', 'R');
        rectIf(grid, 12, 4, 16, 6, 'F', baseFloor, 'M', 'R');
    }

    private String interiorTheme(CityBuilding building, int seed) {
        if (building == null) {
            return Math.floorMod(seed, 4) == 0 ? "tavern" : "home";
        }
        String key = building.key();
        String style = building.style();
        if (key.contains("inn") || key.contains("lodge") || "inn".equals(style) || "restaurant".equals(style)) {
            return "inn";
        }
        if (key.contains("tavern")) {
            return "tavern";
        }
        if (key.contains("armory") || key.contains("barracks") || key.contains("bellfoundry")
                || "barracks".equals(style) || "mine".equals(style) || "blacksmith".equals(style)) {
            return "blacksmith";
        }
        if (key.contains("workshop") || key.contains("bellwright") || key.contains("bindery") || key.contains("stables")
                || key.startsWith("player_shop") || "forestry_hut".equals(style) || "workshop".equals(style)
                || "carpenter".equals(style)) {
            return "carpenter";
        }
        if (key.contains("apothecary") || key.contains("market") || key.contains("shop")
                || "shop".equals(style) || "warehouse".equals(style) || "hunting_camp".equals(style)
                || "apothecary".equals(style) || "alchemist".equals(style) || "fishing_hut".equals(style)) {
            return "shop";
        }
        if ("bakery".equals(style) || key.contains("bakery") || key.contains("baker")) {
            return "bakery";
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
            case "bakery" -> "Bakery Interior";
            case "inn" -> "Inn Interior";
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
            case "bakery" -> 22;
            case "inn" -> 25;
            case "tavern" -> 24;
            case "shop" -> 22;
            case "study" -> 23;
            default -> Math.floorMod(seed, 3) == 0 ? 16 : 17;
        };
        int height = switch (theme) {
            case "blacksmith", "carpenter", "study" -> 14;
            case "tavern", "inn" -> 15;
            case "bakery" -> 14;
            case "shop" -> 13;
            default -> Math.floorMod(seed / 7, 2) == 0 ? 11 : 12;
        };
        char[][] grid = filled(width, height, 'x');
        buildEnclosedInteriorShell(grid);
        addInteriorRooms(grid, theme);
        int doorX = Math.max(1, width / 2 - 1);
        grid[height - 2][doorX] = 'e';
        grid[height - 2][doorX + 1] = 'e';

        switch (theme) {
            case "tavern" -> {
                rect(grid, 9, height - 4, 16, height - 3, 'z');
            }
            case "inn" -> {
                rect(grid, 10, height - 4, 15, height - 3, 'z');
            }
            default -> {
                rect(grid, width / 2 - 2, 6, width / 2 + 2, 8, 'z');
            }
        }
        return grid;
    }

    private void buildEnclosedInteriorShell(char[][] grid) {
        int width = grid[0].length;
        int height = grid.length;
        rect(grid, 1, 1, width - 2, height - 2, 'i');
        rect(grid, 1, 1, width - 2, 1, 'o');
        rect(grid, 1, height - 2, width - 2, height - 2, 'o');
        rect(grid, 1, 1, 1, height - 2, 'o');
        rect(grid, width - 2, 1, width - 2, height - 2, 'o');
    }

    private void addInteriorRooms(char[][] grid, String theme) {
        int width = grid[0].length;
        int height = grid.length;
        switch (theme) {
            case "tavern" -> {
                interiorWallH(grid, 9, width - 10, height - 6, width / 2);
            }
            case "inn" -> {
                interiorWallH(grid, 9, width - 5, 5, width - 8);
                interiorWallH(grid, 9, width - 5, height - 5, width / 2);
                interiorWallV(grid, width - 8, 2, height - 6, 6);
            }
            case "bakery" -> {
                interiorWallH(grid, 3, width - 4, 5, width / 2);
            }
            case "blacksmith" -> {
                interiorWallH(grid, 8, width - 4, 5, 12);
            }
            case "carpenter" -> {
                interiorWallH(grid, 10, width - 4, 6, 15);
            }
            case "shop" -> {
                interiorWallH(grid, 3, width - 4, 5, width / 2);
            }
            case "study" -> {
                interiorWallH(grid, 3, width - 4, 5, width / 2);
                interiorWallH(grid, 3, width - 4, height - 5, width / 2 + 2);
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
        if (reservedPlayerInteriorEditSpot(area, x, y, hitbox[0], hitbox[1])) {
            return;
        }
        if (!canPlaceInteriorAsset(area, x, y, asset)) {
            return;
        }
        if (!isInteriorOverlayAsset(asset) && !isInteriorWallDecorAsset(asset) && !isInteriorPassThroughAsset(asset)) {
            int[] placement = interiorPlacementFootprint(asset);
            rect(area.tiles, x, y, x + placement[0] - 1, y + placement[1] - 1, 'k');
        }
        area.addProp(new WorldProp(x, y, asset, 48));
    }

    private void applyPlayerInteriorProps(MapArea area) {
        for (WorldProp prop : playerInteriorProps.getOrDefault(area.id, List.of())) {
            if (canPlaceInteriorAsset(area, prop.x(), prop.y(), prop.asset())) {
                applyInteriorProp(area, prop);
            }
        }
    }

    private void applyPlayerInteriorTiles(MapArea area) {
        Map<TilePoint, Character> tiles = playerInteriorTiles.getOrDefault(area.id, Map.of());
        for (Map.Entry<TilePoint, Character> entry : tiles.entrySet()) {
            TilePoint point = entry.getKey();
            if (canSetPlayerInteriorTile(area, point.x(), point.y())) {
                area.tiles[point.y()][point.x()] = entry.getValue() == 'o' ? 'o' : 'i';
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
        area.addProp(prop);
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
            case "interior_bed_vertical", "interior_bakery_oven", "interior_resident_bed", "interior_traveler_trunk",
                    "interior_herb_drying_rack_v", "interior_linen_shelf", "interior_anvil_tool_rack",
                    "interior_grain_sacks_v", "interior_inn_screen_chest", "interior_table_v_top",
                    "interior_table_v_middle", "interior_table_v_bottom", "interior_bench_v" -> new int[]{1, 2};
            case "interior_tavern_bar", "interior_shop_counter", "interior_carpenter_table",
                    "interior_alchemy_station", "interior_cooking_station", "interior_herb_drying_rack",
                    "interior_wall_window_wide", "interior_wall_plant_shelf", "interior_wall_herb_rack",
                    "interior_floor_bushy_planter", "interior_aquarium_table", "interior_carpenter_workbench",
                    "interior_metal_crate", "interior_bakery_counter", "interior_tavern_counter",
                    "interior_long_table_benches", "interior_sawhorse_planks", "interior_storage_counter",
                    "interior_low_cupboard", "interior_table_h_left", "interior_table_h_middle",
                    "interior_table_h_right", "interior_bench_h", "interior_banquet_table_h",
                    "interior_stool_table_h", "interior_study_desk_h", "interior_counter_corner_h" -> new int[]{2, 1};
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
                    "interior_chair_north_alt", "interior_chair_south_alt", "interior_chair_east_alt",
                    "interior_chair_west_alt",
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

    private void ensureInteriorNavigable(MapArea area) {
        if (area == null || !"interior".equals(area.kind)) {
            return;
        }
        int guard = 0;
        while (guard++ < 48) {
            boolean[][] reachable = reachableInteriorTiles(area);
            TilePoint blockedTarget = firstUnreachableInteriorFloor(area, reachable);
            if (blockedTarget == null) {
                return;
            }
            WorldProp blocker = bestInteriorNavigationBlocker(area, reachable, blockedTarget);
            if (blocker == null) {
                return;
            }
            clearInteriorFootprint(area, blocker);
            area.removeProp(blocker);
        }
    }

    private boolean[][] reachableInteriorTiles(MapArea area) {
        boolean[][] reachable = new boolean[area.height()][area.width()];
        TilePoint start = interiorEntryPointForArea(area);
        if (!interiorConnectivityWalkable(area, start.x(), start.y())) {
            return reachable;
        }
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        reachable[start.y()][start.x()] = true;
        queue.add(start);
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        while (!queue.isEmpty()) {
            TilePoint point = queue.removeFirst();
            for (int[] dir : dirs) {
                int nx = point.x() + dir[0];
                int ny = point.y() + dir[1];
                if (nx < 1 || ny < 1 || nx >= area.width() - 1 || ny >= area.height() - 1
                        || reachable[ny][nx] || !interiorConnectivityWalkable(area, nx, ny)) {
                    continue;
                }
                reachable[ny][nx] = true;
                queue.addLast(new TilePoint(nx, ny));
            }
        }
        return reachable;
    }

    private TilePoint interiorEntryPointForArea(MapArea area) {
        int startY = Math.max(1, area.height() - 3);
        int centerX = Math.max(1, area.width() / 2);
        for (int radius = 0; radius < Math.max(area.width(), area.height()); radius++) {
            for (int dy = 0; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int x = centerX + dx;
                    int y = startY - dy;
                    if (x <= 0 || y <= 0 || x >= area.width() - 1 || y >= area.height() - 1) {
                        continue;
                    }
                    if (interiorConnectivityWalkable(area, x, y)) {
                        return new TilePoint(x, y);
                    }
                }
            }
        }
        return new TilePoint(Math.max(1, area.width() / 2), Math.max(1, area.height() - 3));
    }

    private TilePoint firstUnreachableInteriorFloor(MapArea area, boolean[][] reachable) {
        for (int y = 1; y < area.height() - 1; y++) {
            for (int x = 1; x < area.width() - 1; x++) {
                if (!reachable[y][x] && interiorConnectivityWalkable(area, x, y)) {
                    return new TilePoint(x, y);
                }
            }
        }
        return null;
    }

    private boolean interiorConnectivityWalkable(MapArea area, int x, int y) {
        char tile = area.tileAt(x, y);
        return Terrain.passable(tile) || (tile == 'k' && isOpenInteriorFurnitureTile(area, x, y));
    }

    private WorldProp bestInteriorNavigationBlocker(MapArea area, boolean[][] reachable, TilePoint blockedTarget) {
        WorldProp best = null;
        int bestScore = Integer.MAX_VALUE;
        TilePoint entry = interiorEntryPointForArea(area);
        for (WorldProp prop : area.props) {
            if (isInteriorOverlayAsset(prop.asset()) || isInteriorWallDecorAsset(prop.asset())
                    || isInteriorPassThroughAsset(prop.asset())) {
                continue;
            }
            int[] footprint = interiorPlacementFootprint(prop.asset());
            boolean nearReachable = false;
            boolean nearBlocked = false;
            for (int y = prop.y(); y < prop.y() + footprint[1]; y++) {
                for (int x = prop.x(); x < prop.x() + footprint[0]; x++) {
                    nearReachable |= adjacentReachableInteriorTile(reachable, x, y);
                    nearBlocked |= Math.abs(x - blockedTarget.x()) + Math.abs(y - blockedTarget.y()) <= 2;
                }
            }
            if (!nearReachable && !nearBlocked) {
                continue;
            }
            int score = Math.abs(prop.x() - blockedTarget.x()) + Math.abs(prop.y() - blockedTarget.y())
                    + Math.abs(prop.x() - entry.x()) + Math.abs(prop.y() - entry.y()) / 2;
            if (isTableAsset(prop.asset()) || isCounterOrWorkbenchAsset(prop.asset())) {
                score += 4;
            }
            if (score < bestScore) {
                bestScore = score;
                best = prop;
            }
        }
        return best;
    }

    private boolean adjacentReachableInteriorTile(boolean[][] reachable, int x, int y) {
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (ny >= 0 && ny < reachable.length && nx >= 0 && nx < reachable[ny].length && reachable[ny][nx]) {
                return true;
            }
        }
        return false;
    }

    private void addTableSet(MapArea area, int x, int y) {
        addFurniture(area, x, y, "interior_round_table");
        addFurniture(area, x, y, Math.floorMod(x * 31 + y * 17 + area.id.hashCode(), 3) == 0
                ? "interior_tabletop_candle"
                : "interior_tabletop_place_setting");
        addFurniture(area, x, y - 1, "interior_chair_north");
        addFurniture(area, x, y + 1, "interior_chair_south");
        addFurniture(area, x - 1, y, "interior_chair_west");
        addFurniture(area, x + 1, y, "interior_chair_east");
    }

    private void addModularTableSet(MapArea area, int x, int y, boolean horizontal) {
        if (horizontal) {
            addFurniture(area, x, y, "interior_table_h_left");
            addFurniture(area, x + 2, y, "interior_table_h_middle");
            addFurniture(area, x + 4, y, "interior_table_h_right");
            addFurniture(area, x + 1, y, "interior_tabletop_place_setting");
            addFurniture(area, x + 3, y, "interior_tabletop_candle");
            addFurniture(area, x, y - 1, "interior_bench_h");
            addFurniture(area, x + 2, y - 1, "interior_bench_h");
            addFurniture(area, x + 4, y - 1, "interior_bench_h");
            addFurniture(area, x, y + 1, "interior_bench_h");
            addFurniture(area, x + 2, y + 1, "interior_bench_h");
            addFurniture(area, x + 4, y + 1, "interior_bench_h");
            addFurniture(area, x - 1, y, "interior_chair_west_alt");
            addFurniture(area, x + 6, y, "interior_chair_east_alt");
            return;
        }
        addFurniture(area, x, y, "interior_table_v_top");
        addFurniture(area, x, y + 2, "interior_table_v_middle");
        addFurniture(area, x, y + 4, "interior_table_v_bottom");
        addFurniture(area, x, y + 1, "interior_tabletop_place_setting");
        addFurniture(area, x, y + 3, "interior_tabletop_candle");
        addFurniture(area, x - 1, y, "interior_bench_v");
        addFurniture(area, x - 1, y + 2, "interior_bench_v");
        addFurniture(area, x - 1, y + 4, "interior_bench_v");
        addFurniture(area, x + 1, y, "interior_bench_v");
        addFurniture(area, x + 1, y + 2, "interior_bench_v");
        addFurniture(area, x + 1, y + 4, "interior_bench_v");
        addFurniture(area, x, y - 1, "interior_chair_north_alt");
        addFurniture(area, x, y + 6, "interior_chair_south_alt");
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
                    area.addProp(new WorldProp(x, y, asset, size));
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
                            area.addProp(new WorldProp(x + ox, y + oy, pairedAsset, decorationSize(tile, roll + 17, pairedAsset)));
                            occupiedProps[y + oy][x + ox] = true;
                        }
                    }
                }
            }
        }
        addResourceNodeProps(area, salt, occupiedProps, tallProps);
        addSoftBiomeProps(area, salt, occupiedProps);
    }

    private void addResourceNodeProps(MapArea area, int salt, boolean[][] occupiedProps, boolean[][] tallProps) {
        for (int y = 2; y < area.height() - 2; y++) {
            for (int x = 2; x < area.width() - 2; x++) {
                char tile = area.tileAt(x, y);
                if (occupiedProps[y][x] || !supportsResourceNode(tile)) {
                    continue;
                }
                int roll = Math.abs(hash(x, y, salt + area.id.hashCode() + 9091)) % 10000;
                int chance = resourceNodeChance(area, x, y, tile);
                if (roll >= chance) {
                    continue;
                }
                int patchSeed = hash(x / 6, y / 6, salt + tile * 191 + area.id.hashCode());
                String asset = resourceNodeFor(area, tile, x, y, roll, patchSeed);
                if (asset.isBlank()) {
                    continue;
                }
                if (isTallProp(asset) && shouldThinTallProp(tile, asset, tallProps, x, y, roll)) {
                    continue;
                }
                area.addProp(new WorldProp(x, y, asset, decorationSize(tile, roll, asset)));
                occupiedProps[y][x] = true;
                if (isTallProp(asset)) {
                    tallProps[y][x] = true;
                }
            }
        }
    }

    private boolean supportsResourceNode(char tile) {
        return switch (tile) {
            case 'f', 'q', 'P', 'v', 'g', 'n', 'b', 's' -> true;
            default -> false;
        };
    }

    private int resourceNodeChance(MapArea area, int x, int y, char tile) {
        int waterDistance = distanceToAny(area, x, y, 3, new char[]{'w'});
        int mountainDistance = distanceToAny(area, x, y, 3, new char[]{'m'});
        int forestDistance = distanceToAny(area, x, y, 3, new char[]{'f'});
        int chance = switch (tile) {
            case 'f' -> 190;
            case 'q' -> 220;
            case 'P' -> waterDistance <= 2 ? 170 : 110;
            case 'v' -> waterDistance <= 2 ? 130 : 90;
            case 'g' -> forestDistance <= 2 ? 95 : 55;
            case 'n' -> mountainDistance <= 2 ? 150 : 85;
            case 'b' -> mountainDistance <= 2 ? 135 : 85;
            case 's' -> mountainDistance <= 2 ? 90 : 45;
            default -> 0;
        };
        if (prefersMountainResourceNode(tile, mountainDistance)) {
            chance += switch (tile) {
                case 'q' -> 140;
                case 'n', 'b' -> 130;
                case 'g', 's' -> 115;
                case 'f' -> 85;
                case 'P', 'v' -> 60;
                default -> 0;
            };
            if (mountainDistance <= 1) {
                chance += 60;
            }
        }
        if (distanceToDifferentNatural(area, x, y, tile, 2) <= 1) {
            chance += 35;
        }
        return chance;
    }

    private String resourceNodeFor(MapArea area, char tile, int x, int y, int roll, int patchSeed) {
        boolean nearWater = distanceToAny(area, x, y, 3, new char[]{'w', '~'}) <= 2;
        int mountainDistance = distanceToAny(area, x, y, 3, new char[]{'m'});
        boolean nearMountain = mountainDistance <= 2 || distanceToAny(area, x, y, 4, new char[]{'q'}) <= 3;
        boolean snow = distanceToAny(area, x, y, 3, new char[]{'n'}) <= 2;
        if (prefersMountainResourceNode(tile, mountainDistance)) {
            return mountainResourceNodeFor(snow, tile == 'q', roll, patchSeed);
        }
        return switch (tile) {
            case 'f' -> forestResourceNodeFor(area, x, y, roll, patchSeed);
            case 'q' -> mountainResourceNodeFor(snow, true, roll, patchSeed);
            case 'P' -> nearWater
                    ? weightedDecoration(roll, patchSeed, new DecorationOption[]{
                            option("deco_beach_shells", 34),
                            option("deco_beach_coconuts", 22),
                            option("deco_beach_driftwood", 24),
                            option("deco_beach_palm", 8, 260)
                    })
                    : weightedDecoration(roll, patchSeed, new DecorationOption[]{
                            option("deco_beach_grass", 32),
                            option("deco_beach_coconuts", 20),
                            option("deco_beach_driftwood", 16)
                    });
            case 'v' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_reeds", nearWater ? 42 : 22),
                    option("deco_mushrooms", 18),
                    option("deco_marsh_firefly_reeds", nearWater ? 8 : 3, 160)
            });
            case 'g' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_grass_herb_patch", 34),
                    option("deco_grass_wildflowers", 22),
                    option("deco_flowers", 18),
                    option("deco_grass_stone_stack", nearMountain ? 14 : 6, 240)
            });
            case 'n' -> nearMountain
                    ? weightedDecoration(roll, patchSeed, new DecorationOption[]{
                            option("deco_tundra_ice_crystals", 28),
                            option("deco_tundra_rocks", 24),
                            option("deco_snow_pine", 18)
                    })
                    : weightedDecoration(roll, patchSeed, new DecorationOption[]{
                            option("deco_tundra_frost_bush", 28),
                            option("deco_snow_pine", 18),
                            option("deco_snow_mound", 18)
                    });
            case 'b' -> nearMountain
                    ? weightedDecoration(roll, patchSeed, new DecorationOption[]{
                            option("deco_badlands_rocks", 34),
                            option("deco_badlands_red_spire", 12, 260),
                            option("deco_badlands_skull_marker", 8, 150)
                    })
                    : weightedDecoration(roll, patchSeed, new DecorationOption[]{
                            option("deco_badlands_dry_grass", 30),
                            option("deco_badlands_rocks", 22)
                    });
            case 's' -> nearMountain
                    ? weightedDecoration(roll, patchSeed, new DecorationOption[]{
                            option("deco_desert_rocks", 32),
                            option("deco_desert_blooming_cactus", 10, 220),
                            option("deco_desert_jar_cache", 8, 140)
                    })
                    : weightedDecoration(roll, patchSeed, new DecorationOption[]{
                            option("deco_cactus", 24),
                            option("deco_dry_grass", 22),
                            option("deco_desert_blooming_cactus", 8, 180)
                    });
            default -> "";
        };
    }

    private boolean prefersMountainResourceNode(char tile, int mountainDistance) {
        if (tile == 'q') {
            return true;
        }
        return mountainDistance <= 2
                && Terrain.passable(tile)
                && !Terrain.connectingRoad(tile)
                && tile != 'w'
                && tile != '~';
    }

    private String forestResourceNodeFor(MapArea area, int x, int y, int roll, int patchSeed) {
        boolean forestCore = naturalNeighborCount(area, x, y, 'f') >= 16;
        boolean edge = distanceToDifferentNatural(area, x, y, 'f', 2) <= 1;
        return weightedDecoration(roll, patchSeed, new DecorationOption[]{
                option("deco_tree_oak_harvestable", forestCore ? 14 : 7),
                option("deco_tree_birch_harvestable", forestCore ? 10 : 8),
                option("deco_tree_pine_harvestable", forestCore ? 12 : 9),
                option("deco_tree_willow_harvestable", edge ? 10 : 4),
                option("deco_tree_maple_harvestable", forestCore ? 9 : 5),
                option("deco_tree_ash_harvestable", forestCore ? 8 : 5),
                option("deco_tree_fruit_harvestable", edge ? 8 : 3, 320),
                option("deco_tree_elder_harvestable", forestCore ? 5 : 2, 180),
                option("deco_tree_magical_harvestable", forestCore ? 4 : 1, 95),
                option("deco_tree_deadwood_harvestable", edge ? 7 : 4, 260),
                option("deco_wood_ironwood_log_pile", forestCore ? 5 : 2, 150),
                option("deco_tree_enchanted_stump", forestCore ? 3 : 1, 85),
                option("deco_wood_fallen_ash_log", 5, 280),
                option("deco_tree_glowing_root_cluster", forestCore ? 3 : 1, 70)
        });
    }

    private String mountainResourceNodeFor(boolean snow, boolean pass, int roll, int patchSeed) {
        return weightedDecoration(roll, patchSeed, new DecorationOption[]{
                option("deco_ore_iron_vein", pass ? 10 : 14),
                option("deco_ore_copper_vein", pass ? 9 : 13),
                option("deco_ore_coal_deposit", pass ? 12 : 14),
                option("deco_ore_tin_vein", 7, 480),
                option("deco_ore_silver_vein", 5, 230),
                option("deco_ore_gold_vein", 3, 160),
                option("deco_ore_mithril_vein", snow ? 5 : 2, 115),
                option("deco_ore_cobalt_vein", snow ? 6 : 3, 180),
                option("deco_ore_adamantite_vein", 2, 55),
                option("deco_ore_crystal_vein", snow ? 6 : 4, 135),
                option("deco_ore_steel_scrap", pass ? 5 : 2, 100)
        });
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
                area.addProp(new WorldProp(x, y, asset, softDecorationSize(asset, roll)));
                occupiedProps[y][x] = true;
            }
        }
    }

    private int softDecorationChance(MapArea area, int x, int y, char tile) {
        int waterDistance = distanceToAny(area, x, y, 3, new char[]{'w'});
        int roadDistance = distanceToRoad(area, x, y, 3);
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
        boolean waterShore = isShoreWater(area, x, y, tile);
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
                    option("deco_water_lily_pad_cluster", 44),
                    option("deco_water_duckweed_patch", 28),
                    option("deco_water_shore_reeds", waterShore ? 26 : 0)
            });
            case '~' -> weightedDecoration(roll, patchSeed, new DecorationOption[]{
                    option("deco_water_shore_reeds", waterShore ? 36 : 18),
                    option("deco_water_lily_pad_cluster", 24),
                    option("deco_water_duckweed_patch", 18)
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
        int roadDistance = distanceToRoad(area, x, y, 4);
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
        if (!Terrain.connectingRoad(tile) && roadDistance == 0) {
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
                case "deco_tree_birch_harvestable", "deco_tree_pine_harvestable",
                        "deco_tree_deadwood_harvestable" -> 72;
                case "deco_tree_magical_harvestable", "deco_tree_elder_harvestable",
                        "deco_tree_fruit_harvestable" -> 86;
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
                    "deco_water_lily_pad_cluster", "deco_water_shore_reeds", "deco_water_duckweed_patch",
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
                    "deco_mountain_cairn", "deco_mountain_pass_way_cairn", "deco_beach_driftwood",
                    "deco_ore_iron_vein", "deco_ore_copper_vein", "deco_ore_coal_deposit",
                    "deco_ore_tin_vein", "deco_ore_silver_vein", "deco_ore_gold_vein",
                    "deco_ore_mithril_vein", "deco_ore_cobalt_vein", "deco_ore_adamantite_vein",
                    "deco_ore_crystal_vein", "deco_ore_steel_scrap",
                    "deco_wood_ironwood_log_pile", "deco_wood_fallen_ash_log" -> 36 + jitter * 4;
            case "deco_forest_ancient_roots", "deco_forest_shrine_stone", "deco_forest_fairy_pool",
                    "deco_imagen_grass_pond", "deco_marsh_lily_pool", "deco_marsh_bubble_pool",
                    "deco_mountain_spring_pool", "deco_mountain_pass_snowmelt_pool",
                    "deco_tree_enchanted_stump", "deco_tree_glowing_root_cluster" -> 58 + jitter * 6;
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

    private void addSettlementDistrictProps(MapArea area, String variant, boolean town) {
        addMarketCoreProps(area, variant, town);
        if ("highwall".equals(variant)) {
            addHighwallDistrictProps(area, town);
        }
    }

    private void addMarketCoreProps(MapArea area, String variant, boolean town) {
        int centerX = Math.min(area.width() - 7, 17);
        int centerY = Math.min(area.height() - 8, 12);
        placeCityPropIfFree(area, centerX - 4, centerY - 3, "city_prop_news_kiosk", 40);
        placeCityPropIfFree(area, centerX + 6, centerY - 2, "city_prop_wagon_awning", 46);
        placeCityPropIfFree(area, centerX - 5, centerY + 3, town ? "city_prop_market_green" : "city_prop_market_red", 46);
        placeCityPropIfFree(area, centerX + 5, centerY + 3, "city_prop_market_yellow", 46);
        placeCityPropIfFree(area, centerX - 7, centerY, "city_prop_street_lamp", 36);
        placeCityPropIfFree(area, centerX + 8, centerY, "city_prop_street_lamp", 36);
        if ("archive".equals(variant)) {
            placeCityPropIfFree(area, centerX, centerY - 4, "city_prop_flower_crate", 36);
        } else if ("sanctum".equals(variant)) {
            placeCityPropIfFree(area, centerX, centerY - 4, "city_prop_planter_stone", 38);
        }
    }

    private void addHighwallDistrictProps(MapArea area, boolean town) {
        addDistrictPropLine(area, 11, 3, 22, 3, "city_prop_street_lamp", 34, 5);
        addDistrictPropLine(area, 5, 18, 15, 18, "location_graveyard_iron_fence", 42, 4);
        addDistrictPropLine(area, 20, 20, 32, 20, "location_graveyard_iron_fence", 42, 5);
        placeCityPropIfFree(area, 8, 19, "village_prop_anvil_stump", 44);
        placeCityPropIfFree(area, 10, 20, "village_prop_ore_cart", 44);
        placeCityPropIfFree(area, 13, 20, "village_prop_tool_rack", 42);
        placeCityPropIfFree(area, 25, 21, "village_prop_training_dummy", 44);
        placeCityPropIfFree(area, 29, 21, "city_prop_crate", 40);
        placeCityPropIfFree(area, 31, 23, "city_prop_barrel_stack", 42);
        placeCityPropIfFree(area, 29, 16, "city_prop_stone_bench", 36);
        placeCityPropIfFree(area, 32, 16, "city_prop_street_lamp", 36);
        if (town) {
            placeCityPropIfFree(area, 18, 16, "village_prop_training_dummy", 44);
            placeCityPropIfFree(area, 21, 16, "city_prop_crate", 38);
        }
    }

    private void addDistrictPropLine(MapArea area, int x1, int y1, int x2, int y2, String asset, int size, int spacing) {
        int dx = Integer.compare(x2, x1);
        int dy = Integer.compare(y2, y1);
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int step = 0; step <= steps; step++) {
            if (spacing > 1 && step % spacing == spacing - 1) {
                continue;
            }
            int x = x1 + dx * step;
            int y = y1 + dy * step;
            placeCityPropIfFree(area, x, y, asset, size);
        }
    }

    private void addCityProps(MapArea area, String variant) {
        for (int y = 2; y < area.height() - 2; y++) {
            for (int x = 2; x < area.width() - 2; x++) {
                if (cityBuildingAt(area.id, x, y) != null) {
                    continue;
                }
                char tile = area.tileAt(x, y);
                int roll = Math.floorMod(hash(x, y, area.id.hashCode() ^ variant.hashCode()), 100);
                if (roll >= cityPropPlacementChance(area, x, y, tile, variant)) {
                    continue;
                }
                CityPropCandidate prop = bestCityPropCandidate(area, x, y, tile, variant, roll);
                if (prop != null) {
                    placeCityPropIfFree(area, x, y, prop.asset(), prop.size());
                }
            }
        }
        addCityAlleyProps(area);
    }

    private int cityPropPlacementChance(MapArea area, int x, int y, char tile, String variant) {
        int chance;
        if (tile == 'a' || tile == 'G') {
            chance = 32;
        } else if (tile == 'y' || tile == 'g' || tile == 'f' || tile == 'v') {
            chance = 22;
        } else if (tile == 'p' || tile == 'C') {
            chance = 11;
        } else if (tile == 'j' || tile == 'l') {
            chance = 9;
        } else if (Terrain.connectingRoad(tile)) {
            chance = 5;
        } else {
            chance = 0;
        }
        if (nearbyBuildingStyle(area, x, y, 2) != null) {
            chance += 5;
        }
        if ("archive".equals(variant) && tile == 'y') {
            chance += 5;
        }
        if ("sanctum".equals(variant) && (tile == 's' || tile == 'b')) {
            chance += 6;
        }
        return Math.min(42, chance);
    }

    private CityPropCandidate bestCityPropCandidate(MapArea area, int x, int y, char tile, String variant, int roll) {
        List<CityPropCandidate> candidates = new ArrayList<>();
        String nearbyStyle = nearbyBuildingStyle(area, x, y, 3);
        boolean road = Terrain.connectingRoad(tile) || nearbyConnectingRoadCount(area.tiles, x, y, 1) > 0;
        boolean market = tile == 'a' || tile == 'G' || nearbyTileCount(area.tiles, x, y, 'a', 2) > 0;
        boolean green = tile == 'g' || tile == 'f' || tile == 'y' || nearbyTileCount(area.tiles, x, y, 'g', 2) > 2;
        boolean paved = tile == 'p' || tile == 'C' || tile == 'j' || tile == 'l';
        addCityPropCandidate(candidates, "city_prop_street_lamp", 34 + roll % 8, road ? 44 : 12, roll);
        addCityPropCandidate(candidates, "city_prop_stone_bench", 34 + roll % 8, green ? 42 : 14, roll);
        addCityPropCandidate(candidates, "city_prop_planter_stone", 34 + roll % 10, green || paved ? 36 : 8, roll);
        addCityPropCandidate(candidates, "city_prop_flower_pot", 30 + roll % 8, green ? 34 : 12, roll);
        addCityPropCandidate(candidates, "city_prop_plant_box", 34 + roll % 8, green ? 38 : 10, roll);
        if (green && nearbyPropCount(area, x, y, 2) < 3) {
            String[] canopy = parkCanopyAssets(variant);
            String[] understory = parkUnderstoryAssets(variant);
            String tree = canopy[Math.floorMod(roll + x + y, canopy.length)];
            String low = understory[Math.floorMod(roll + x * 3 + y, understory.length)];
            addCityPropCandidate(candidates, tree, parkPropSize(tree, roll + x * 19 + y), road ? 34 : 46, roll);
            addCityPropCandidate(candidates, low, parkPropSize(low, roll + x * 11 + y), 36, roll);
        }
        if (market) {
            addCityPropCandidate(candidates, "city_prop_market_red", 44 + roll % 8, 48, roll);
            addCityPropCandidate(candidates, "city_prop_market_yellow", 44 + roll % 8, 46, roll);
            addCityPropCandidate(candidates, "city_prop_market_green", 44 + roll % 8, 46, roll);
            addCityPropCandidate(candidates, "city_prop_wagon_awning", 46 + roll % 8, 42, roll);
            addCityPropCandidate(candidates, "city_prop_news_kiosk", 40 + roll % 8, 32, roll);
        }
        if (paved || market) {
            addCityPropCandidate(candidates, "city_prop_crate", 34 + roll % 8, 26, roll);
            addCityPropCandidate(candidates, "city_prop_barrel", 34 + roll % 8, 24, roll);
            addCityPropCandidate(candidates, "city_prop_cart", 40 + roll % 10, 28, roll);
            addCityPropCandidate(candidates, "city_prop_barrel_stack", 38 + roll % 8, 24, roll);
        }
        addBusinessPropCandidates(candidates, nearbyStyle, variant, roll);
        if ("archive".equals(variant)) {
            addCityPropCandidate(candidates, "city_prop_flower_crate", 34 + roll % 8, green ? 32 : 16, roll);
            addCityPropCandidate(candidates, "city_prop_flower_baskets", 34 + roll % 8, green ? 28 : 12, roll);
        } else if ("highwall".equals(variant)) {
            addCityPropCandidate(candidates, "village_prop_training_dummy", 42 + roll % 8, paved ? 24 : 8, roll);
        } else if ("belltower".equals(variant)) {
            addCityPropCandidate(candidates, "city_prop_bunting_pole", 38 + roll % 8, road ? 30 : 12, roll);
        } else if ("sanctum".equals(variant)) {
            addCityPropCandidate(candidates, "village_prop_herb_barrel", 38 + roll % 8, green ? 32 : 18, roll);
        }
        candidates.removeIf(candidate -> isDisallowedOutdoorProp(candidate.asset()));
        return candidates.stream()
                .max(Comparator.comparingInt(CityPropCandidate::score)
                        .thenComparing(CityPropCandidate::asset))
                .orElse(null);
    }

    private void addBusinessPropCandidates(List<CityPropCandidate> candidates, String style, String variant, int roll) {
        if (style == null) {
            return;
        }
        switch (style) {
            case "bakery" -> {
                addCityPropCandidate(candidates, "village_prop_clay_oven", 44 + roll % 8, 58, roll);
                addCityPropCandidate(candidates, "village_prop_grain_sacks", 40 + roll % 8, 52, roll);
                addCityPropCandidate(candidates, "village_prop_produce_basket", 36 + roll % 8, 34, roll);
            }
            case "blacksmith" -> {
                addCityPropCandidate(candidates, "village_prop_anvil_stump", 42 + roll % 8, 62, roll);
                addCityPropCandidate(candidates, "village_prop_tool_rack", 40 + roll % 8, 50, roll);
                addCityPropCandidate(candidates, "village_prop_ore_cart", 42 + roll % 8, 38, roll);
            }
            case "workshop", "shop" -> {
                addCityPropCandidate(candidates, "village_prop_sawhorse", 42 + roll % 8, 58, roll);
                addCityPropCandidate(candidates, "village_prop_tool_rack", 40 + roll % 8, 50, roll);
                addCityPropCandidate(candidates, "village_prop_woodpile", 38 + roll % 8, 34, roll);
            }
            case "forestry_hut" -> {
                addCityPropCandidate(candidates, "village_prop_log_stack", 42 + roll % 8, 62, roll);
                addCityPropCandidate(candidates, "village_prop_chopping_block", 40 + roll % 8, 54, roll);
                addCityPropCandidate(candidates, "village_prop_woodpile", 40 + roll % 8, 52, roll);
            }
            case "apothecary" -> {
                addCityPropCandidate(candidates, "village_prop_herb_barrel", 42 + roll % 8, 62, roll);
                addCityPropCandidate(candidates, "village_prop_seedling_tray", 38 + roll % 8, 50, roll);
                addCityPropCandidate(candidates, "city_prop_flower_crate", 34 + roll % 8, 44, roll);
            }
            case "warehouse" -> {
                addCityPropCandidate(candidates, "city_prop_crate", 40 + roll % 8, 54, roll);
                addCityPropCandidate(candidates, "city_prop_barrel_stack", 42 + roll % 8, 48, roll);
                addCityPropCandidate(candidates, "city_prop_cart", 44 + roll % 8, 42, roll);
            }
            case "farmstead" -> {
                addCityPropCandidate(candidates, "village_prop_hay_bales", 40 + roll % 8, 56, roll);
                addCityPropCandidate(candidates, "village_prop_farm_tools", 40 + roll % 8, 48, roll);
                addCityPropCandidate(candidates, "village_prop_produce_basket", 38 + roll % 8, 44, roll);
            }
            case "sun_shrine", "shrine" -> {
                addCityPropCandidate(candidates, "city_prop_planter_stone", 40 + roll % 8, 42, roll);
                addCityPropCandidate(candidates, "city_prop_stone_bench", 38 + roll % 8, 36, roll);
            }
            default -> {
            }
        }
        if ("snow".equals(variant)) {
            addCityPropCandidate(candidates, "city_prop_source_snow_bush", 34 + roll % 8, 22, roll);
        }
    }

    private void addCityPropCandidate(List<CityPropCandidate> candidates, String asset, int size, int baseScore, int roll) {
        if (baseScore <= 0) {
            return;
        }
        candidates.add(new CityPropCandidate(asset, size, baseScore + Math.floorMod(asset.hashCode() + roll, 13)));
    }

    private String nearbyBuildingStyle(MapArea area, int x, int y, int radius) {
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (CityBuilding building : cityBuildings(area.id)) {
            int dx = Math.max(0, Math.max(building.x1() - x, x - building.x2()));
            int dy = Math.max(0, Math.max(building.y1() - y, y - building.y2()));
            int distance = dx + dy;
            if (distance <= radius && distance < bestDistance) {
                best = building.style();
                bestDistance = distance;
            }
        }
        return best;
    }

    private void addBusinessYardPropClusters(MapArea area, String variant) {
        int salt = area.id.hashCode() ^ variant.hashCode() ^ 14039;
        for (CityBuilding building : cityBuildings(area.id)) {
            String[] assets = businessYardAssets(building.style(), variant);
            if (assets.length == 0) {
                continue;
            }
            int frontY = building.y2() + 1;
            if (frontY < 0 || frontY >= area.height()) {
                continue;
            }
            int center = building.x1() + building.width() / 2;
            int[][] offsets = building.width() >= 4
                    ? new int[][]{{0, 0}, {-1, 0}, {1, 0}, {-2, 1}, {2, 1}, {0, 1}}
                    : new int[][]{{0, 0}, {-1, 0}, {1, 0}, {0, 1}};
            int placed = 0;
            int target = Math.min(assets.length, building.width() >= 4 ? 3 : 2);
            for (int i = 0; i < offsets.length && placed < target; i++) {
                int x = center + offsets[i][0];
                int y = frontY + offsets[i][1];
                String asset = assets[Math.floorMod(salt + building.key().hashCode() + i, assets.length)];
                if (placeBusinessYardProp(area, x, y, asset, businessYardPropSize(asset, salt + i))) {
                    placed++;
                }
            }
        }
    }

    private String[] businessYardAssets(String style, String variant) {
        return switch (style) {
            case "bakery", "restaurant" -> new String[]{
                    "village_prop_clay_oven", "village_prop_grain_sacks", "village_prop_produce_basket"
            };
            case "blacksmith" -> new String[]{
                    "village_prop_anvil_stump", "village_prop_tool_rack", "village_prop_ore_cart"
            };
            case "workshop", "carpenter" -> new String[]{
                    "village_prop_sawhorse", "village_prop_tool_rack", "village_prop_woodpile"
            };
            case "forestry_hut" -> new String[]{
                    "village_prop_log_stack", "village_prop_chopping_block", "village_prop_woodpile"
            };
            case "apothecary", "alchemist" -> new String[]{
                    "village_prop_herb_barrel", "village_prop_seedling_tray", "city_prop_flower_crate"
            };
            case "fishing_hut" -> new String[]{
                    "village_prop_fish_rack", "village_prop_net_drying_rack", "city_prop_crate"
            };
            case "warehouse", "granary" -> new String[]{
                    "city_prop_crate", "city_prop_barrel_stack", "city_prop_cart", "village_prop_grain_sacks"
            };
            case "farmstead" -> new String[]{
                    "village_prop_hay_bales", "village_prop_farm_tools", "village_prop_produce_basket"
            };
            default -> new String[0];
        };
    }

    private boolean placeBusinessYardProp(MapArea area, int x, int y, String asset, int size) {
        TilePoint target = nearestBusinessYardPavement(area, x, y);
        if (target == null) {
            return false;
        }
        x = target.x();
        y = target.y();
        if (isDisallowedOutdoorProp(asset)
                || x < 0 || y < 0 || x >= area.width() || y >= area.height()
                || cityBuildingAt(area.id, x, y) != null
                || transitionAt(area.id, x, y) != null
                || area.propAt(x, y) != null
                || !Terrain.passable(area.tileAt(x, y))
                || Terrain.connectingRoad(area.tileAt(x, y))
                || !isDecorativePavingTile(area.tileAt(x, y))) {
            return false;
        }
        area.addProp(new WorldProp(x, y, asset, size));
        return true;
    }

    private TilePoint nearestBusinessYardPavement(MapArea area, int x, int y) {
        TilePoint best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int radius = 0; radius <= 3; radius++) {
            for (int yy = y - radius; yy <= y + radius; yy++) {
                for (int xx = x - radius; xx <= x + radius; xx++) {
                    int distance = Math.abs(xx - x) + Math.abs(yy - y);
                    if (distance > radius || xx < 0 || yy < 0 || xx >= area.width() || yy >= area.height()) {
                        continue;
                    }
                    if (!isDecorativePavingTile(area.tileAt(xx, yy))
                            || Terrain.connectingRoad(area.tileAt(xx, yy))
                            || cityBuildingAt(area.id, xx, yy) != null
                            || transitionAt(area.id, xx, yy) != null
                            || area.propAt(xx, yy) != null) {
                        continue;
                    }
                    int roadAdjacency = nearbyConnectingRoadCount(area.tiles, xx, yy, 1);
                    int score = distance * 100 - roadAdjacency * 12
                            + Math.floorMod(hash(xx, yy, area.id.hashCode() ^ 17491), 19);
                    if (score < bestScore) {
                        bestScore = score;
                        best = new TilePoint(xx, yy);
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private int businessYardPropSize(String asset, int salt) {
        int jitter = Math.floorMod(salt + asset.hashCode(), 5);
        if (asset.contains("oven") || asset.contains("cart") || asset.contains("rack")) {
            return 42 + jitter * 2;
        }
        return 38 + jitter * 2;
    }

    private void addCityAlleyProps(MapArea area) {
        String[] alleyPlants = {
                "city_prop_potted_shrub_tall",
                "city_prop_flower_crate",
                "city_prop_moss_barrel_planter",
                "city_prop_vine_lantern_post",
                "city_prop_flower_baskets"
        };
        int[][] anchors = {
                {5, 7, 34}, {13, 7, 36}, {20, 7, 34}, {31, 8, 38},
                {5, 13, 34}, {14, 14, 32}, {21, 14, 34}, {30, 14, 36},
                {6, 19, 34}, {14, 20, 32}, {20, 20, 34}, {29, 19, 36}
        };
        for (int i = 0; i < anchors.length; i++) {
            int x = anchors[i][0];
            int y = anchors[i][1];
            if (!canPlaceCityAlleyProp(area, x, y)) {
                continue;
            }
            String asset = alleyPlants[Math.floorMod(area.id.hashCode() + i * 3, alleyPlants.length)];
            placeCityPropIfFree(area, x, y, asset, anchors[i][2]);
        }
        addHorizontalAlleyGreenery(area, alleyPlants);
    }

    private boolean canPlaceCityAlleyProp(MapArea area, int x, int y) {
        char tile = area.tileAt(x, y);
        return insideSettlementInterior(area, x, y)
                && Terrain.passable(tile)
                && !Terrain.connectingRoad(tile)
                && tile != 'q'
                && tile != 'B'
                && tile != 'w'
                && cityBuildingAt(area.id, x, y) == null
                && area.props.stream().noneMatch(prop -> prop.x() == x && prop.y() == y)
                && nearbyPropCount(area, x, y, 1) < 4;
    }

    private void addHorizontalAlleyGreenery(MapArea area, String[] alleyPlants) {
        int[] alleys = {4, 7, 13, 20, 22};
        for (int lane = 0; lane < alleys.length; lane++) {
            int y = alleys[lane];
            for (int x = 3; x < area.width() - 3; x += 2) {
                char tile = area.tileAt(x, y);
                if (tile != 'l' && tile != 'j' && tile != 'y' && tile != 'C' && tile != 'G') {
                    continue;
                }
                int roll = Math.floorMod(hash(x, y, area.id.hashCode() + lane * 431), 100);
                if (roll >= 34 || !canPlaceCityAlleyProp(area, x, y)) {
                    continue;
                }
                String asset = alleyPlants[Math.floorMod(roll + x + area.id.hashCode(), alleyPlants.length)];
                placeCityPropIfFree(area, x, y, asset, 30 + roll % 10);
            }
        }
    }

    private void addTownProps(MapArea area, String variant) {
        String[] commons = {
                "city_prop_news_kiosk", "city_prop_wagon_awning", "city_prop_bunting_pole", "city_prop_stone_bench"
        };
        int[][] anchors = {
                {12, 8, 42}, {22, 8, 44}, {10, 17, 36}, {26, 17, 38}, {17, 20, 42}
        };
        for (int i = 0; i < anchors.length; i++) {
            int[] anchor = anchors[i];
            placeCityPropIfFree(area, anchor[0], anchor[1], commons[Math.floorMod(area.id.hashCode() + i, commons.length)], anchor[2]);
        }
    }

    private void addTownParks(MapArea area, String variant, int parkCount) {
        TilePoint[] candidates = {
                new TilePoint(6, 23), new TilePoint(28, 23), new TilePoint(6, 8), new TilePoint(28, 8),
                new TilePoint(12, 16), new TilePoint(23, 16), new TilePoint(10, 4), new TilePoint(25, 4),
                new TilePoint(8, 20), new TilePoint(27, 20), new TilePoint(15, 23), new TilePoint(21, 23)
        };
        int placed = 0;
        int salt = area.id.hashCode() ^ variant.hashCode();
        for (int i = 0; i < candidates.length && placed < parkCount; i++) {
            TilePoint point = candidates[Math.floorMod(i + salt, candidates.length)];
            int[][] sizes = "village".equals(area.kind)
                    ? new int[][]{{4, 3}, {3, 3}}
                    : new int[][]{{6, 4}, {5, 4}, {4, 3}};
            for (int[] size : sizes) {
                if (canPlaceParkStamp(area, point.x(), point.y(), size[0], size[1])) {
                    stampPark(area, point.x(), point.y(), size[0], size[1], variant, salt + placed * 41);
                    placed++;
                    break;
                }
            }
        }
        int extraPavedParks = "village".equals(area.kind) ? 1 : 2;
        addVacantPavedParks(area, variant, Math.max(0, parkCount + extraPavedParks - placed), salt);
    }

    private void addVacantPavedParks(MapArea area, String variant, int remaining, int salt) {
        if (remaining <= 0) {
            return;
        }
        int[][] sizes = "village".equals(area.kind)
                ? new int[][]{{4, 3}, {3, 3}}
                : new int[][]{{6, 4}, {5, 4}, {4, 3}};
        int placed = 0;
        for (int y = 4; y < area.height() - 4 && placed < remaining; y++) {
            int startX = 3 + Math.floorMod(salt + y * 17, 4);
            for (int x = startX; x < area.width() - 3 && placed < remaining; x += 2) {
                for (int[] size : sizes) {
                    if (canPlaceVacantPavedParkStamp(area, x, y, size[0], size[1])) {
                        stampPark(area, x, y, size[0], size[1], variant, salt + 97 + placed * 53);
                        placed++;
                        x += size[0];
                        break;
                    }
                }
            }
        }
    }

    private boolean canPlaceParkStamp(MapArea area, int centerX, int centerY, int width, int height) {
        int x1 = centerX - width / 2;
        int y1 = centerY - height / 2;
        int x2 = x1 + width - 1;
        int y2 = y1 + height - 1;
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                if (!insideSettlementInterior(area, x, y)
                        || transitionAt(area.id, x, y) != null
                        || cityBuildingAt(area.id, x, y) != null
                        || area.propAt(x, y) != null) {
                    return false;
                }
                char tile = area.tileAt(x, y);
                if (Terrain.connectingRoad(tile) || tile == 'w' || tile == 'x' || tile == 't') {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean canPlaceVacantPavedParkStamp(MapArea area, int centerX, int centerY, int width, int height) {
        if (!canPlaceParkStamp(area, centerX, centerY, width, height)) {
            return false;
        }
        int x1 = centerX - width / 2;
        int y1 = centerY - height / 2;
        int pavedTiles = 0;
        int totalTiles = width * height;
        for (int y = y1; y < y1 + height; y++) {
            for (int x = x1; x < x1 + width; x++) {
                if (parkReplaceablePavedTile(area.tileAt(x, y))) {
                    pavedTiles++;
                }
            }
        }
        return pavedTiles * 4 >= totalTiles * 3;
    }

    private boolean parkReplaceablePavedTile(char tile) {
        return tile == 'p' || tile == 'j' || tile == 'l' || tile == 'a' || tile == 'C' || tile == 'G';
    }

    private void stampPark(MapArea area, int centerX, int centerY, int width, int height, String variant, int salt) {
        int x1 = centerX - width / 2;
        int y1 = centerY - height / 2;
        char ground = parkGroundTile(variant);
        char trim = parkTrimTile(variant);
        char path = parkPathTile(variant);
        for (int y = y1; y < y1 + height; y++) {
            for (int x = x1; x < x1 + width; x++) {
                boolean edge = x == x1 || y == y1 || x == x1 + width - 1 || y == y1 + height - 1;
                boolean footpath = !edge
                        && (x == centerX || (y == centerY && Math.floorMod(hash(x, y, salt + 13), 100) < 68));
                area.tiles[y][x] = footpath ? path : (edge && Math.floorMod(hash(x, y, salt), 100) < 56 ? trim : ground);
            }
        }
        int layout = Math.floorMod(salt, 5);
        String[] canopy = parkCanopyAssets(variant);
        String[] understory = parkUnderstoryAssets(variant);
        int[][] corners = {
                {x1, y1},
                {x1 + width - 1, y1},
                {x1 + width - 1, y1 + height - 1},
                {x1, y1 + height - 1}
        };
        int primaryCorner = layout % corners.length;
        int secondaryCorner = Math.floorMod(primaryCorner + 2 + layout % 2, corners.length);
        placeParkPropIfFree(area, corners[primaryCorner][0], corners[primaryCorner][1],
                canopy[Math.floorMod(salt, canopy.length)], salt);
        if (width >= 6 && height >= 4) {
            placeParkPropIfFree(area, corners[secondaryCorner][0], corners[secondaryCorner][1],
                    canopy[Math.floorMod(salt / 7 + 1, canopy.length)], salt + 19);
        }
        for (int i = 0; i < corners.length; i++) {
            if (i == primaryCorner || (width >= 6 && height >= 4 && i == secondaryCorner)) {
                continue;
            }
            placeParkPropIfFree(area, corners[i][0], corners[i][1],
                    understory[Math.floorMod(salt + i * 11, understory.length)], salt + i * 23);
        }
        if (layout == 1 || layout == 4) {
            placeParkPropIfFree(area, centerX, Math.max(y1 + 1, centerY - 1), parkWaterAsset(variant), salt + 31);
        } else {
            placeParkPropIfFree(area, centerX, centerY, parkAccentAsset(variant), salt + 37);
        }
        String parkObject = parkObjectAsset(variant, layout);
        placeParkPropIfFree(area, centerX - 1, centerY + height / 2 - 1, parkObject, salt + 43);
        if (width >= 5 && layout % 3 != 0) {
            placeParkPropIfFree(area, centerX + 1, centerY + height / 2 - 1, "city_prop_street_lamp", salt + 47);
        }
        addDenseParkFlora(area, x1, y1, width, height, variant, salt + 71);
    }

    private void addDenseParkFlora(MapArea area, int x1, int y1, int width, int height, String variant, int salt) {
        String[] canopy = parkCanopyAssets(variant);
        String[] understory = parkUnderstoryAssets(variant);
        for (int y = y1; y < y1 + height; y++) {
            for (int x = x1; x < x1 + width; x++) {
                if (area.propAt(x, y) != null || !Terrain.passable(area.tileAt(x, y))
                        || Terrain.connectingRoad(area.tileAt(x, y))) {
                    continue;
                }
                int roll = Math.floorMod(hash(x, y, salt), 100);
                if (roll < 44) {
                    String asset = understory[Math.floorMod(roll + x * 5 + y, understory.length)];
                    placeParkPropIfFree(area, x, y, asset, salt + roll);
                } else if (width >= 5 && height >= 4 && roll < 55 && nearbyPropCount(area, x, y, 2) < 3) {
                    String asset = canopy[Math.floorMod(roll + x + y * 3, canopy.length)];
                    placeParkPropIfFree(area, x, y, asset, salt + roll);
                }
            }
        }
    }

    private char parkGroundTile(String variant) {
        return switch (variant) {
            case "highwall", "snow" -> 'n';
            case "sanctum", "desert" -> 's';
            case "belltower", "marsh" -> 'v';
            case "archive" -> 'y';
            default -> 'g';
        };
    }

    private char parkTrimTile(String variant) {
        return switch (variant) {
            case "highwall", "snow" -> 'p';
            case "sanctum", "desert" -> 'b';
            case "belltower", "marsh" -> 'y';
            case "archive" -> 'p';
            default -> 'f';
        };
    }

    private char parkPathTile(String variant) {
        return switch (variant) {
            case "highwall", "snow", "archive" -> 'p';
            case "sanctum", "desert" -> 'b';
            case "belltower", "marsh" -> 'U';
            default -> 'l';
        };
    }

    private String[] parkCanopyAssets(String variant) {
        return switch (variant) {
            case "highwall", "snow" -> new String[]{
                    "deco_tree_blue_pine", "deco_snow_pine", "deco_tree_pine"
            };
            case "sanctum", "desert" -> new String[]{
                    "deco_cactus", "deco_desert_blooming_cactus", "city_prop_potted_shrub_tall"
            };
            case "belltower", "marsh" -> new String[]{
                    "deco_tree_willow_harvestable", "deco_tree_round", "city_prop_potted_shrub_tall"
            };
            case "archive" -> new String[]{
                    "deco_tree_round", "deco_tree_young", "city_prop_potted_shrub_tall"
            };
            default -> new String[]{
                    "deco_tree_oak", "deco_tree_round", "deco_tree_young"
            };
        };
    }

    private String[] parkUnderstoryAssets(String variant) {
        return switch (variant) {
            case "highwall", "snow" -> new String[]{
                    "city_prop_source_snow_bush", "deco_snow_mound", "city_prop_source_snow_stump"
            };
            case "sanctum", "desert" -> new String[]{
                    "deco_dry_grass", "city_prop_planter_stone", "deco_desert_jar_cache"
            };
            case "belltower", "marsh" -> new String[]{
                    "city_prop_source_reeds", "deco_bog_grass", "deco_reeds"
            };
            case "archive" -> new String[]{
                    "city_prop_herb_planter_narrow", "city_prop_flower_baskets", "deco_soft_purple_flowers"
            };
            default -> new String[]{
                    "deco_bush", "deco_flowers", "deco_grass_wildflowers", "city_prop_flower_crate"
            };
        };
    }

    private String parkObjectAsset(String variant, int layout) {
        if ("sanctum".equals(variant) || "desert".equals(variant)) {
            return layout % 2 == 0 ? "city_prop_planter_stone" : "city_prop_stone_bench";
        }
        if ("archive".equals(variant)) {
            return layout % 2 == 0 ? "city_prop_flower_crate" : "city_prop_stone_bench";
        }
        return layout % 2 == 0 ? "city_prop_stone_bench" : "city_prop_plant_box";
    }

    private String parkAccentAsset(String variant) {
        return switch (variant) {
            case "highwall", "snow" -> "town_park_accent_snow";
            case "sanctum", "desert" -> "town_park_accent_desert";
            case "belltower", "marsh" -> "town_park_accent_marsh";
            case "archive" -> "town_park_accent_city";
            default -> "town_park_accent_green";
        };
    }

    private String parkWaterAsset(String variant) {
        return switch (variant) {
            case "highwall", "snow" -> "deco_tundra_thaw_pond";
            case "sanctum", "desert" -> "deco_desert_oasis_pool";
            case "belltower", "marsh" -> "deco_marsh_lily_pool";
            default -> "deco_grass_pond";
        };
    }

    private boolean placeParkPropIfFree(MapArea area, int x, int y, String asset, int salt) {
        return placeCityPropIfFree(area, x, y, asset, parkPropSize(asset, salt));
    }

    private int parkPropSize(String asset, int salt) {
        int jitter = Math.floorMod(salt + asset.hashCode(), 5);
        if (isForestTree(asset)) {
            return decorationSize('f', salt, asset);
        }
        if (asset.equals("deco_snow_pine")) {
            return 74 + jitter * 5;
        }
        if (asset.equals("deco_cactus") || asset.equals("deco_desert_blooming_cactus")) {
            return 58 + jitter * 4;
        }
        if (asset.equals("city_prop_potted_shrub_tall")) {
            return 46 + jitter * 3;
        }
        if (asset.startsWith("town_park_accent_")) {
            return 50 + jitter * 4;
        }
        if (asset.contains("pond") || asset.contains("pool") || asset.contains("oasis")) {
            return 44 + jitter * 3;
        }
        if (asset.contains("bench") || asset.contains("lamp")) {
            return 34 + jitter * 2;
        }
        if (asset.startsWith("city_prop_")) {
            return 34 + jitter * 3;
        }
        return decorationSize('g', salt, asset);
    }

    private boolean insideSettlementInterior(MapArea area, int x, int y) {
        return x > 1 && y > 1 && x < area.width() - 2 && y < area.height() - 2;
    }

    private boolean placeCityPropIfFree(MapArea area, int x, int y, String asset, int size) {
        if (isDisallowedOutdoorProp(asset) || !canPlaceCityAlleyProp(area, x, y)) {
            return false;
        }
        if (requiresPavementForTownProp(asset) && !isDecorativePavingTile(area.tileAt(x, y))) {
            return false;
        }
        area.addProp(new WorldProp(x, y, asset, size));
        return true;
    }

    private boolean requiresPavementForTownProp(String asset) {
        if (asset == null) {
            return false;
        }
        if (asset.startsWith("town_portal_")
                || asset.startsWith("town_park_accent_")
                || asset.startsWith("deco_")
                || asset.equals("mountain_goat")
                || asset.equals("sheep")
                || asset.startsWith("village_fence_")) {
            return false;
        }
        return asset.startsWith("city_prop_") || asset.startsWith("village_prop_") || asset.equals("city_lantern");
    }

    private boolean isDisallowedOutdoorProp(String asset) {
        if (asset == null) {
            return true;
        }
        String lower = asset.toLowerCase();
        return lower.contains("window")
                || lower.startsWith("interior_wall_")
                || lower.startsWith("city_building_")
                || lower.contains("wall_planter")
                || lower.contains("vine_trellis")
                || lower.contains("herb_planter_narrow")
                || lower.contains("ivy_wall_planter")
                || lower.contains("stone_arch")
                || lower.contains("arched")
                || lower.contains("graveyard_tombstone")
                || lower.contains("dungeon_grave")
                || lower.contains("crypt_sarcophagus");
    }

    private int nearbyPropCount(MapArea area, int x, int y, int radius) {
        int count = 0;
        for (WorldProp prop : area.props) {
            if (Math.abs(prop.x() - x) <= radius && Math.abs(prop.y() - y) <= radius) {
                count++;
            }
        }
        return count;
    }

    private void addVillageProps(MapArea area, String variant) {
        addBiomeProps(area, variant.hashCode());
        area.addProp(new WorldProp(14, 9, "city_prop_fountain_small", 42));
        area.addProp(new WorldProp(12, 15, "deco_road_signpost", 36));
        area.addProp(new WorldProp(15, 12, "village_prop_clay_oven", 42));
        area.addProp(new WorldProp(10, 8, "village_prop_wash_line", 48));
        area.addProp(new WorldProp(17, 15, "village_prop_seedling_tray", 36));
        area.addProp(new WorldProp(23, 15, "village_prop_compost_bin", 38));
        if ("snow".equals(variant)) {
            area.addProp(new WorldProp(4, 3, "city_prop_source_snow_bush", 34));
            area.addProp(new WorldProp(23, 16, "city_prop_source_snow_stump", 32));
            area.addProp(new WorldProp(8, 15, "deco_snow_mound", 30));
        } else if ("desert".equals(variant)) {
            area.addProp(new WorldProp(4, 13, "deco_cactus", 42));
            area.addProp(new WorldProp(23, 5, "deco_desert_jar_cache", 36));
            area.addProp(new WorldProp(21, 15, "deco_dry_grass", 30));
        } else if ("marsh".equals(variant)) {
            area.addProp(new WorldProp(3, 8, "city_prop_source_reeds", 34));
            area.addProp(new WorldProp(24, 13, "deco_marsh_lily_pool", 38));
            area.addProp(new WorldProp(20, 6, "deco_bog_grass", 30));
        } else {
            area.addProp(new WorldProp(5, 15, "deco_flowers", 30));
            area.addProp(new WorldProp(21, 5, "deco_bush", 32));
        }
        addVillagePastures(area, variant);
        addVillageWorkyardProps(area, variant);
    }

    private void addVillagePastures(MapArea area, String variant) {
        int[][] candidates = {
                {26, 17, 7, 5},
                {3, 18, 7, 5},
                {27, 8, 5, 4}
        };
        int target = 1 + Math.floorMod(area.id.hashCode(), 2);
        int placed = 0;
        for (int i = 0; i < candidates.length && placed < target; i++) {
            int[] pasture = candidates[Math.floorMod(i + area.id.hashCode(), candidates.length)];
            if (stampVillagePasture(area, pasture[0], pasture[1], pasture[2], pasture[3], variant, i)) {
                placed++;
            }
        }
    }

    private boolean stampVillagePasture(MapArea area, int x1, int y1, int width, int height, String variant, int salt) {
        int x2 = x1 + width - 1;
        int y2 = y1 + height - 1;
        if (!canPlaceVillagePasture(area, x1, y1, x2, y2)) {
            return false;
        }
        char ground = "desert".equals(variant) ? 'b' : ("snow".equals(variant) ? 'q' : 'g');
        for (int y = y1 + 1; y < y2; y++) {
            for (int x = x1 + 1; x < x2; x++) {
                if (Terrain.passable(area.tileAt(x, y)) && !Terrain.connectingRoad(area.tileAt(x, y))) {
                    area.tiles[y][x] = ground;
                }
            }
        }
        int gateX = x1 + width / 2;
        for (int x = x1; x <= x2; x++) {
            if (x != gateX) {
                addVillageFenceProp(area, x, y1);
                addVillageFenceProp(area, x, y2);
            }
        }
        for (int y = y1 + 1; y < y2; y++) {
            addVillageFenceProp(area, x1, y);
            addVillageFenceProp(area, x2, y);
        }
        placeVillagePastureProp(area, x1 + 2, y1 + 2, "village_prop_water_trough", 38);
        placeVillagePastureProp(area, x2 - 2, y2 - 1, "village_prop_hay_bales", 36);
        placeVillagePastureProp(area, x1 + 1 + Math.floorMod(salt + area.id.hashCode(), Math.max(1, width - 3)),
                y1 + 1, Math.floorMod(area.id.hashCode() + salt, 3) == 0 ? "mountain_goat" : "sheep", 34);
        return true;
    }

    private boolean canPlaceVillagePasture(MapArea area, int x1, int y1, int x2, int y2) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                if (!insideSettlementInterior(area, x, y)
                        || transitionAt(area.id, x, y) != null
                        || cityBuildingAt(area.id, x, y) != null
                        || area.propAt(x, y) != null) {
                    return false;
                }
                char tile = area.tileAt(x, y);
                if (!Terrain.passable(tile) || Terrain.connectingRoad(tile) || tile == 'w' || tile == 'x') {
                    return false;
                }
            }
        }
        return true;
    }

    private void addVillageFenceProp(MapArea area, int x, int y) {
        if (area.propAt(x, y) == null) {
            area.addProp(new WorldProp(x, y, "village_fence_auto", 40));
        }
    }

    private void placeVillagePastureProp(MapArea area, int x, int y, String asset, int size) {
        if (area.propAt(x, y) == null && cityBuildingAt(area.id, x, y) == null && Terrain.passable(area.tileAt(x, y))) {
            area.addProp(new WorldProp(x, y, asset, size));
        }
    }

    private void addVillageWorkyardProps(MapArea area, String variant) {
        String[] farmProps = {
                "village_prop_hay_stack", "village_prop_farm_tools", "village_prop_chicken_coop",
                "village_prop_produce_basket", "village_prop_grain_sacks", "village_prop_tool_rack"
        };
        int[][] anchors = {
                {3, 15, 38}, {8, 18, 36}, {12, 18, 38}, {17, 17, 36}, {24, 8, 38}, {28, 15, 36}
        };
        int salt = area.id.hashCode() ^ variant.hashCode();
        for (int i = 0; i < anchors.length; i++) {
            int[] anchor = anchors[i];
            String asset = farmProps[Math.floorMod(salt + i * 5, farmProps.length)];
            placeCityPropIfFree(area, anchor[0], anchor[1], asset, anchor[2]);
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
        area.addProp(prop);
    }

    private void addDungeonProps(MapArea area, String theme, int floor, int floors) {
        for (int y = 3; y < area.height() - 3; y++) {
            for (int x = 3; x < area.width() - 3; x++) {
                int roll = Math.abs(hash(x, y, area.id.hashCode())) % 100;
                char tile = area.tileAt(x, y);
                if (isDungeonDecorFloor(tile) && roll < dungeonPropChance(theme, area, x, y)) {
                    String asset = dungeonDecorationFor(theme, tile, x, y, roll + area.id.hashCode());
                    area.addProp(new WorldProp(x, y, asset, dungeonPropSize(asset, roll)));
                }
            }
        }
        addDungeonSetPieces(area, theme, floor, floors);
    }

    private boolean isDungeonDecorFloor(char tile) {
        return switch (tile) {
            case 'd', 'D', 'F', 'M', 'R', 'S', 'L', 'N', 'I', 'J', 'H', 'Q' -> true;
            default -> false;
        };
    }

    private int dungeonPropChance(String theme, MapArea area, int x, int y) {
        int open = 0;
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int[] dir : dirs) {
            if (isDungeonDecorFloor(area.tileAt(x + dir[0], y + dir[1]))) {
                open++;
            }
        }
        int cluster = Math.abs(hash(x / 3, y / 3, area.id.hashCode())) % 100;
        int base = switch (theme) {
            case "cave" -> 7;
            case "sewer" -> 9;
            case "prison" -> 10;
            case "crypt" -> 12;
            case "abandoned_castle" -> 11;
            default -> 9;
        };
        if (open <= 1) {
            base += 5;
        } else if (open >= 4) {
            base -= 4;
        }
        if (cluster < 28) {
            base += 9;
        }
        return Math.max(3, Math.min(26, base));
    }

    private String dungeonDecorationFor(String theme, char tile, int x, int y, int seed) {
        if (tile == 'S') {
            return seed % 2 == 0 ? "dungeon_prop_rune_pillar" : "location_dungeon_broken_altar";
        }
        if (tile == 'M') {
            return pick(new String[]{"deco_soft_mossy_rock", "deco_imagen_pale_mushroom_ring", "deco_imagen_green_rune_stone"}, seed);
        }
        if (tile == 'R') {
            return pick(new String[]{"location_dungeon_rubble_cairn", "location_dungeon_collapsed_wall", "deco_imagen_flat_stone_stack"}, seed);
        }
        return switch (theme) {
            case "cave" -> pick(new String[]{
                    "deco_rocks", "deco_imagen_crystal_cluster", "quest_cave_rune_cache",
                    "deco_imagen_pale_mushroom_ring", "dungeon_prop_lantern_stand"
            }, seed);
            case "crypt" -> pick(new String[]{
                    "location_crypt_sarcophagus", "location_dungeon_grave_slabs", "location_graveyard_skull_marker",
                    "dungeon_prop_rune_pillar", "location_dungeon_braziers", "location_graveyard_tombstones"
            }, seed);
            case "abandoned_castle" -> pick(new String[]{
                    "location_dungeon_collapsed_wall", "location_dungeon_broken_altar", "dungeon_prop_relic_crate",
                    "dungeon_prop_lantern_stand", "location_camp_crates", "deco_imagen_flat_stone_stack"
            }, seed);
            case "prison" -> pick(new String[]{
                    "dungeon_prop_chain_stand", "dungeon_prop_lantern_stand", "location_graveyard_skull_marker",
                    "dungeon_prop_relic_crate", "location_camp_crates"
            }, seed);
            case "sewer" -> pick(new String[]{
                    "dungeon_prop_lantern_stand", "location_camp_crates", "deco_soft_water_wet_stones",
                    "deco_soft_water_reeds_gold", "deco_imagen_marsh_bubble_pool"
            }, seed);
            case "goblin_camp" -> pick(new String[]{
                    "location_camp_crates", "location_graveyard_skull_marker", "location_camp_fire",
                    "dungeon_prop_chain_stand", "dungeon_prop_relic_crate"
            }, seed);
            case "bandit_camp" -> pick(new String[]{
                    "location_camp_crates", "dungeon_prop_relic_crate", "dungeon_prop_lantern_stand",
                    "dungeon_prop_chain_stand", "deco_imagen_road_camp"
            }, seed);
            default -> pick(new String[]{
                    "dungeon_prop_rune_pillar", "dungeon_prop_lantern_stand",
                    "dungeon_prop_chain_stand", "dungeon_prop_relic_crate"
            }, seed);
        };
    }

    private int dungeonPropSize(String asset, int roll) {
        return switch (asset) {
            case "location_dungeon_collapsed_wall" -> 54;
            case "location_dungeon_broken_altar", "location_crypt_sarcophagus" -> 50;
            case "location_dungeon_braziers" -> 48;
            case "location_graveyard_tombstones", "location_dungeon_grave_slabs" -> 44;
            case "location_camp_fire", "location_camp_crates", "location_graveyard_skull_marker" -> 36;
            case "quest_cave_rune_cache", "deco_imagen_crystal_cluster", "deco_imagen_marsh_bubble_pool" -> 40;
            default -> roll < 2 ? 44 : 34 + roll % 10;
        };
    }

    private void addDungeonSetPieces(MapArea area, String theme, int floor, int floors) {
        switch (theme) {
            case "cave" -> {
                area.addProp(new WorldProp(8, 5, "deco_imagen_crystal_cluster", 46));
                area.addProp(new WorldProp(24, 19, floor == floors ? "dungeon_prop_rune_pillar" : "quest_cave_rune_cache", 44));
            }
            case "crypt" -> {
                area.addProp(new WorldProp(17, 12, floor == floors ? "location_dungeon_broken_altar" : "location_crypt_sarcophagus", 52));
                area.addProp(new WorldProp(7, 20, "location_dungeon_grave_slabs", 46));
            }
            case "abandoned_castle" -> {
                area.addProp(new WorldProp(17, 5, "dungeon_prop_relic_crate", 42));
                area.addProp(new WorldProp(30, 18, floor == floors ? "location_dungeon_broken_altar" : "location_dungeon_collapsed_wall", 54));
            }
            case "prison" -> {
                area.addProp(new WorldProp(6, 5, "dungeon_prop_chain_stand", 42));
                area.addProp(new WorldProp(18, 18, floor == floors ? "location_graveyard_skull_marker" : "dungeon_prop_lantern_stand", 42));
            }
            case "sewer" -> {
                area.addProp(new WorldProp(18, 12, "dungeon_prop_lantern_stand", 40));
                area.addProp(new WorldProp(30, 19, floor == floors ? "deco_imagen_marsh_bubble_pool" : "deco_soft_water_wet_stones", 42));
            }
            default -> area.addProp(new WorldProp(17, 12, "dungeon_prop_lantern_stand", 40));
        }
    }

    private void addHouseProps(MapArea area, String theme, int seed) {
        switch (theme) {
            case "blacksmith" -> {
                addFurniture(area, 3, 2, "interior_forge");
                addFurniture(area, 5, 2, "interior_anvil_tool_rack");
                addFurniture(area, 8, 2, "interior_anvil");
                addFurniture(area, 10, 2, "interior_metal_crate");
                addFurniture(area, area.width() - 7, 2, "interior_shop_counter");
                addFurniture(area, area.width() - 4, 2, "interior_metal_crate");
                addFurniture(area, 4, 7, "interior_storage_counter");
                addFurniture(area, 7, 7, "interior_metal_crate");
                addFurniture(area, 10, 7, "interior_anvil_tool_rack");
                addFurniture(area, 12, 7, "interior_stove");
                addFurniture(area, 15, 7, "interior_metal_crate");
                addFurniture(area, 4, 10, "interior_barrels");
                addFurniture(area, area.width() - 4, 9, "interior_traveler_trunk");
                addFurniture(area, area.width() - 5, area.height() - 4, "interior_hearth_pot");
            }
            case "carpenter" -> {
                addFurniture(area, 3, 2, "interior_carpenter_workbench");
                addFurniture(area, 6, 2, "interior_sawhorse_planks");
                addFurniture(area, 9, 2, "interior_low_cupboard");
                addFurniture(area, area.width() - 9, 2, "interior_crates");
                addFurniture(area, area.width() - 6, 2, "interior_barrels");
                addFurniture(area, 4, 7, "interior_low_cupboard");
                addFurniture(area, 7, 7, "interior_stool_table_h");
                addFurniture(area, 11, 7, "interior_sawhorse_planks");
                addFurniture(area, area.width() - 9, 7, "interior_carpenter_workbench");
                addFurniture(area, area.width() - 6, 7, "interior_sawhorse_planks");
                addFurniture(area, 4, 10, "interior_crates");
                addFurniture(area, 8, 10, "interior_carpenter_workbench");
                addFurniture(area, area.width() - 5, area.height() - 4, "interior_storage_counter");
            }
            case "bakery" -> {
                addFurniture(area, 3, 2, "interior_bakery_oven");
                addFurniture(area, 8, 2, "interior_bakery_counter");
                addFurniture(area, 11, 2, "interior_bakery_counter");
                addFurniture(area, area.width() - 7, 2, "interior_cooking_station");
                addFurniture(area, area.width() - 4, 2, "interior_grain_sacks_v");
                addFurniture(area, 6, 8, "interior_banquet_table_h");
                addFurniture(area, 6, 9, "interior_bench_h");
                addFurniture(area, 10, 8, "interior_stool_table_h");
                addFurniture(area, 10, 9, "interior_bench_h");
                addFurniture(area, 14, 8, "interior_round_table");
                addFurniture(area, 14, 8, "interior_tabletop_meal");
                addFurniture(area, 17, 8, "interior_bakery_counter");
                addFurniture(area, 4, 11, "interior_grain_sacks_v");
                addFurniture(area, area.width() - 5, area.height() - 4, "interior_storage_counter");
            }
            case "inn" -> {
                addFurniture(area, 2, 2, "interior_tavern_counter");
                addFurniture(area, 5, 2, "interior_tavern_counter");
                addFurniture(area, 8, 2, "interior_storage_counter");
                addFurniture(area, 11, 2, "interior_barrels");
                addFurniture(area, area.width() - 7, 3, "interior_resident_bed");
                addFurniture(area, area.width() - 4, 3, "interior_traveler_trunk");
                addFurniture(area, area.width() - 7, 7, "interior_resident_bed");
                addFurniture(area, area.width() - 4, 7, "interior_inn_screen_chest");
                addModularTableSet(area, 6, 8, true);
                addFurniture(area, 15, 8, "interior_banquet_table_h");
                addFurniture(area, 15, 7, "interior_bench_h");
                addFurniture(area, 15, 9, "interior_bench_h");
                addTableSet(area, 5, 12);
                addFurniture(area, 11, 12, "interior_hearth_pot");
                addFurniture(area, area.width() - 6, area.height() - 4, "interior_linen_shelf");
            }
            case "tavern" -> {
                addFurniture(area, 2, 2, "interior_tavern_counter");
                addFurniture(area, 5, 2, "interior_tavern_counter");
                addFurniture(area, 8, 2, "interior_tavern_counter");
                addFurniture(area, 6, 2, "interior_barrels");
                addFurniture(area, area.width() - 6, 3, "interior_resident_bed");
                addFurniture(area, area.width() - 4, 5, "interior_traveler_trunk");
                addFurniture(area, area.width() - 3, 3, "interior_oven");
                addModularTableSet(area, 7, 8, true);
                addFurniture(area, 15, 9, "interior_banquet_table_h");
                addFurniture(area, 15, 8, "interior_bench_h");
                addFurniture(area, 15, 10, "interior_bench_h");
                addTableSet(area, area.width() - 5, 10);
                addTableSet(area, 5, 12);
                addFurniture(area, 11, 13, "interior_hearth_pot");
            }
            case "shop" -> {
                addFurniture(area, 3, 2, "interior_shop_counter");
                addFurniture(area, 6, 2, "interior_bookshelf");
                addFurniture(area, 8, 2, "interior_bookshelf");
                addFurniture(area, area.width() - 7, 2, "interior_shop_counter");
                addFurniture(area, area.width() - 4, 2, "interior_crates");
                addFurniture(area, 3, 7, "interior_counter_corner_h");
                addFurniture(area, 6, 7, "interior_shop_counter");
                addFurniture(area, 10, 7, "interior_low_cupboard");
                addFurniture(area, 5, 8, "interior_stool_table_h");
                addFurniture(area, 9, 8, "interior_banquet_table_h");
                addFurniture(area, 13, 8, "interior_crates");
                addFurniture(area, 16, 7, "interior_barrels");
                addFurniture(area, area.width() - 6, area.height() - 4, "interior_alchemy_station");
            }
            case "study" -> {
                addFurniture(area, 3, 2, "interior_bookshelf");
                addFurniture(area, 5, 2, "interior_bookshelf");
                addFurniture(area, 7, 2, "interior_bookshelf");
                addFurniture(area, area.width() - 7, 2, "interior_bookshelf");
                addFurniture(area, area.width() - 5, 2, "interior_bookshelf");
                addFurniture(area, area.width() - 3, 2, "interior_bookshelf");
                addFurniture(area, 3, 3, "interior_study_desk_h");
                addFurniture(area, 7, 3, "interior_study_desk_h");
                addFurniture(area, 12, 7, "interior_round_table");
                addFurniture(area, 12, 7, "interior_tabletop_candle");
                addFurniture(area, 16, 7, "interior_study_desk_h");
                addFurniture(area, 3, 10, "interior_bookshelf");
                addFurniture(area, 6, 10, "interior_side_table");
                addFurniture(area, 10, 10, "interior_aquarium_table");
                addFurniture(area, 15, 10, "interior_alchemy_station");
                addFurniture(area, area.width() - 6, area.height() - 4, "interior_alchemy_station");
            }
            default -> {
                addFurniture(area, 2, 2, "interior_resident_bed");
                addFurniture(area, 4, 2, "interior_traveler_trunk");
                addFurniture(area, area.width() - 6, 2, "interior_bookshelf");
                addFurniture(area, 3, 6, "interior_round_table");
                addFurniture(area, 3, 6, "interior_tabletop_meal");
                addFurniture(area, 2, 6, "interior_chair_west");
                addFurniture(area, 4, 6, "interior_chair_east");
                addFurniture(area, area.width() - 8, 6, "interior_storage_counter");
                addFurniture(area, area.width() - 5, 6, "interior_linen_shelf");
                addFurniture(area, 5, area.height() - 5, Math.floorMod(seed, 2) == 0 ? "interior_stove" : "interior_hearth_pot");
            }
        }
        addInteriorAccentFurniture(area, theme, seed);
        addInteriorLightSources(area, theme, seed);
        addRankedInteriorFurnishings(area, theme, seed);
    }

    private void addRankedInteriorFurnishings(MapArea area, String theme, int seed) {
        int target = interiorDensityTarget(area, theme);
        String[] assets = rankedInteriorAssetsFor(theme);
        int guard = 0;
        while (interiorDensityCount(area) < target && guard++ < target * 2) {
            RankedInteriorCandidate best = null;
            for (String asset : assets) {
                int[] footprint = interiorHitboxFootprint(asset);
                for (int y = 2; y < area.height() - 2; y++) {
                    for (int x = 2; x < area.width() - 2; x++) {
                        if (x + footprint[0] >= area.width() - 1 || y + footprint[1] >= area.height() - 1
                                || reservedPlayerInteriorEditSpot(area, x, y, footprint[0], footprint[1])
                                || !canPlaceInteriorAsset(area, x, y, asset)) {
                            continue;
                        }
                        int score = interiorPlacementScore(area, theme, seed, x, y, asset);
                        if (best == null || score > best.score()) {
                            best = new RankedInteriorCandidate(x, y, asset, score);
                        }
                    }
                }
            }
            if (best == null || best.score() < 28) {
                break;
            }
            addFurniture(area, best.x(), best.y(), best.asset());
            addSurfaceDetailFor(area, best.x(), best.y(), best.asset(), seed + guard * 31);
            addRankedCompanionClusterFor(area, best.x(), best.y(), best.asset(), seed + guard * 47);
        }
    }

    private boolean reservedPlayerInteriorEditSpot(MapArea area, int x, int y, int width, int height) {
        return area.id.startsWith("house_" + PLAYER_VILLAGE_ID)
                && rectsOverlap(x, y, x + width - 1, y + height - 1, 8, 4, 8, 4);
    }

    private int interiorDensityTarget(MapArea area, String theme) {
        int floorArea = 0;
        for (int y = 1; y < area.height() - 1; y++) {
            for (int x = 1; x < area.width() - 1; x++) {
                char tile = area.tileAt(x, y);
                if (tile == 'i' || tile == 'z' || tile == 'k' || tile == 'e') {
                    floorArea++;
                }
            }
        }
        int areaTarget = Math.max(18, floorArea / 7);
        int themeTarget = switch (theme) {
            case "inn", "tavern" -> 38;
            case "study" -> 35;
            case "shop", "bakery", "carpenter" -> 33;
            case "blacksmith" -> 30;
            default -> 24;
        };
        return Math.min(themeTarget, areaTarget + 8);
    }

    private int interiorDensityCount(MapArea area) {
        int count = 0;
        for (WorldProp prop : area.props) {
            if (!isInteriorWallDecorAsset(prop.asset()) && !isInteriorOverlayAsset(prop.asset())) {
                count++;
            }
        }
        return count;
    }

    private String[] rankedInteriorAssetsFor(String theme) {
        return switch (theme) {
            case "blacksmith" -> new String[]{
                    "interior_anvil", "interior_anvil_tool_rack", "interior_metal_crate", "interior_forge",
                    "interior_storage_counter", "interior_low_cupboard", "interior_barrels", "interior_crates",
                    "interior_stool_table_h", "interior_hearth_pot", "interior_flower_pot", "interior_herb_pot"
            };
            case "carpenter" -> new String[]{
                    "interior_carpenter_workbench", "interior_sawhorse_planks", "interior_low_cupboard",
                    "interior_storage_counter", "interior_stool_table_h", "interior_crates", "interior_barrels",
                    "interior_side_table", "interior_planting_pot", "interior_floor_sapling_pot", "interior_flower_pot"
            };
            case "bakery" -> new String[]{
                    "interior_bakery_counter", "interior_cooking_station", "interior_cookpot_stand",
                    "interior_grain_sacks_v", "interior_barrels", "interior_banquet_table_h", "interior_stool_table_h",
                    "interior_bench_h", "interior_round_table", "interior_chair_north", "interior_chair_south",
                    "interior_chair_east", "interior_chair_west", "interior_herb_planter", "interior_flower_pot"
            };
            case "inn", "tavern" -> new String[]{
                    "interior_tavern_counter", "interior_barrels", "interior_banquet_table_h", "interior_stool_table_h",
                    "interior_bench_h", "interior_round_table", "interior_chair_north", "interior_chair_south",
                    "interior_chair_east", "interior_chair_west", "interior_side_table", "interior_traveler_trunk",
                    "interior_linen_shelf", "interior_hearth_pot", "interior_flower_pot", "interior_herb_pot"
            };
            case "shop" -> new String[]{
                    "interior_shop_counter", "interior_counter_corner_h", "interior_low_cupboard", "interior_bookshelf",
                    "interior_crates", "interior_barrels", "interior_stool_table_h", "interior_banquet_table_h",
                    "interior_round_table", "interior_chair_north", "interior_chair_south", "interior_chair_east",
                    "interior_chair_west", "interior_flower_pot", "interior_herb_planter"
            };
            case "study" -> new String[]{
                    "interior_bookshelf", "interior_study_desk_h", "interior_side_table", "interior_aquarium_table",
                    "interior_alchemy_station", "interior_round_table", "interior_chair_north", "interior_chair_south",
                    "interior_chair_east", "interior_chair_west", "interior_flower_pot",
                    "interior_floor_leafy_plant"
            };
            default -> new String[]{
                    "interior_bookshelf", "interior_side_table", "interior_storage_counter", "interior_round_table",
                    "interior_chair_north", "interior_chair_south", "interior_chair_east", "interior_chair_west",
                    "interior_flower_pot", "interior_herb_pot", "interior_floor_leafy_plant", "interior_traveler_trunk"
            };
        };
    }

    private int interiorPlacementScore(MapArea area, String theme, int seed, int x, int y, String asset) {
        int[] footprint = interiorHitboxFootprint(asset);
        int horizontalWalls = adjacentHorizontalWallCount(area, x, y, footprint[0], footprint[1]);
        int verticalWalls = adjacentVerticalWallCount(area, x, y, footprint[0], footprint[1]);
        int wallTotal = horizontalWalls + verticalWalls;
        int minDistance = nearestInteriorPropDistance(area, x, y, footprint[0], footprint[1]);
        int score = 24 + Math.floorMod(hash(x, y, seed + asset.hashCode()), 17);

        if (minDistance == 1) {
            score -= 14;
        } else if (minDistance == 2 || minDistance == 3) {
            score += 18;
        } else if (minDistance >= 5) {
            score += isFillerInteriorAsset(asset) ? 22 : 6;
        }

        if (isChairAsset(asset)) {
            int tableScore = directionalTableScore(area, x, y, asset);
            if (tableScore <= 0) {
                return -1000;
            }
            score += 72 + tableScore * 18 - wallTotal * 8;
        } else if (isBenchAsset(asset)) {
            score += nearbyRoleScore(area, x, y, "table", 2) * 18 - wallTotal * 5;
        } else if (isTableAsset(asset)) {
            score += wallTotal == 0 ? 58 : -22;
            score += interiorCenterScore(area, x, y);
        } else if (isBookshelfAsset(asset)) {
            score += horizontalWalls > 0 ? 72 : -42;
            score += verticalWalls > 0 ? 8 : 0;
        } else if (isVerticalInteriorFurniture(asset)) {
            score += verticalWalls > 0 ? 66 : -26;
            score += horizontalWalls > 0 ? 12 : 0;
        } else if (isCookingInteriorAsset(asset)) {
            score += nearbyRoleScore(area, x, y, "counter", 3) * 18;
            score += nearbyRoleScore(area, x, y, "storage", 3) * 12;
            score += nearbyRoleScore(area, x, y, "herb", 3) * 12;
        } else if (isMetalInteriorAsset(asset)) {
            score += nearbyRoleScore(area, x, y, "metal", 3) * 18;
            score += nearbyRoleScore(area, x, y, "forge", 4) * 24;
            score += wallTotal > 0 ? 10 : 0;
        } else if (isStorageInteriorAsset(asset)) {
            score += wallTotal > 0 ? 34 : -8;
            score += nearbyRoleScore(area, x, y, "work", 3) * 9;
        } else if (isFillerInteriorAsset(asset)) {
            score += wallTotal > 0 ? 24 : 6;
            score += minDistance >= 4 ? 30 : 0;
        } else if (isCounterOrWorkbenchAsset(asset)) {
            score += horizontalWalls > 0 || verticalWalls > 0 ? 34 : -4;
            score += nearbyRoleScore(area, x, y, "storage", 3) * 8;
        }

        score += themeInteriorAssetBonus(theme, asset);
        score -= sameInteriorAssetCount(area, asset) * assetRepeatPenalty(asset);
        return score;
    }

    private int themeInteriorAssetBonus(String theme, String asset) {
        return switch (theme) {
            case "blacksmith" -> isMetalInteriorAsset(asset) || asset.contains("forge") ? 30 : 0;
            case "carpenter" -> asset.contains("carpenter") || asset.contains("sawhorse") || asset.contains("planks") ? 30 : 0;
            case "bakery" -> isCookingInteriorAsset(asset) || asset.contains("bakery") || asset.contains("grain") ? 26 : 0;
            case "inn", "tavern" -> isTableAsset(asset) || isBenchAsset(asset) || isChairAsset(asset)
                    || asset.contains("barrel") || asset.contains("counter") ? 20 : 0;
            case "shop" -> asset.contains("counter") || asset.contains("crates") || asset.contains("bookshelf") ? 22 : 0;
            case "study" -> asset.contains("bookshelf") || asset.contains("study") || asset.contains("alchemy")
                    || asset.contains("aquarium") ? 28 : 0;
            default -> 0;
        };
    }

    private void addSurfaceDetailFor(MapArea area, int x, int y, String asset, int seed) {
        if (isSurfaceDetailAnchor(asset)) {
            String detail = Math.floorMod(seed + asset.hashCode(), 4) == 0
                    ? "interior_tabletop_candle"
                    : Math.floorMod(seed + asset.hashCode(), 4) == 1
                    ? "interior_tabletop_meal"
                    : "interior_tabletop_place_setting";
            addFurniture(area, x, y, detail);
        }
    }

    private void addRankedCompanionClusterFor(MapArea area, int x, int y, String asset, int seed) {
        if (asset.equals("interior_round_table")) {
            addFurniture(area, x, y - 1, "interior_chair_north");
            addFurniture(area, x, y + 1, "interior_chair_south");
            addFurniture(area, x - 1, y, "interior_chair_west");
            addFurniture(area, x + 1, y, "interior_chair_east");
            return;
        }
        if (asset.equals("interior_banquet_table_h") || asset.equals("interior_stool_table_h")) {
            addFurniture(area, x, y - 1, "interior_bench_h");
            addFurniture(area, x, y + 1, "interior_bench_h");
            addFurniture(area, x - 1, y, "interior_chair_west_alt");
            addFurniture(area, x + 2, y, "interior_chair_east_alt");
            return;
        }
        if (isCookingInteriorAsset(asset)) {
            addFurniture(area, x - 2, y, asset.contains("bakery") ? "interior_bakery_counter" : "interior_storage_counter");
            addFurniture(area, x + 2, y, "interior_barrels");
            addFurniture(area, x, y + 1, Math.floorMod(seed, 2) == 0 ? "interior_herb_planter" : "interior_herb_pot");
            return;
        }
        if (isMetalInteriorAsset(asset)) {
            addFurniture(area, x - 2, y, "interior_metal_crate");
            addFurniture(area, x + 1, y, "interior_anvil_tool_rack");
            addFurniture(area, x, y + 2, "interior_metal_crate");
            return;
        }
        if (asset.contains("carpenter") || asset.contains("sawhorse")) {
            addFurniture(area, x + 2, y, "interior_sawhorse_planks");
            addFurniture(area, x - 2, y, "interior_crates");
            addFurniture(area, x, y + 1, "interior_low_cupboard");
            return;
        }
        if (isBookshelfAsset(asset)) {
            addFurniture(area, x + 1, y, Math.floorMod(seed, 2) == 0 ? "interior_flower_pot" : "interior_side_table");
        }
    }

    private int adjacentHorizontalWallCount(MapArea area, int x, int y, int width, int height) {
        int count = 0;
        for (int xx = x; xx < x + width; xx++) {
            if (area.tileAt(xx, y - 1) == 'o') {
                count++;
            }
            if (area.tileAt(xx, y + height) == 'o') {
                count++;
            }
        }
        return count;
    }

    private int adjacentVerticalWallCount(MapArea area, int x, int y, int width, int height) {
        int count = 0;
        for (int yy = y; yy < y + height; yy++) {
            if (area.tileAt(x - 1, yy) == 'o') {
                count++;
            }
            if (area.tileAt(x + width, yy) == 'o') {
                count++;
            }
        }
        return count;
    }

    private int nearestInteriorPropDistance(MapArea area, int x, int y, int width, int height) {
        int best = 99;
        int cx = x + width / 2;
        int cy = y + height / 2;
        for (WorldProp prop : area.props) {
            if (isInteriorWallDecorAsset(prop.asset()) || isInteriorOverlayAsset(prop.asset())) {
                continue;
            }
            int[] footprint = interiorHitboxFootprint(prop.asset());
            int px = prop.x() + footprint[0] / 2;
            int py = prop.y() + footprint[1] / 2;
            best = Math.min(best, Math.abs(cx - px) + Math.abs(cy - py));
        }
        return best;
    }

    private int nearbyRoleScore(MapArea area, int x, int y, String role, int radius) {
        int score = 0;
        for (WorldProp prop : area.props) {
            if (Math.abs(prop.x() - x) + Math.abs(prop.y() - y) > radius || !interiorAssetMatchesRole(prop.asset(), role)) {
                continue;
            }
            score++;
        }
        return Math.min(4, score);
    }

    private boolean interiorAssetMatchesRole(String asset, String role) {
        return switch (role) {
            case "table" -> isSeatingAnchorAsset(asset);
            case "counter" -> hasInteriorAssetTag(asset, InteriorAssetTag.COUNTER)
                    || hasInteriorAssetTag(asset, InteriorAssetTag.WORK);
            case "storage" -> isStorageInteriorAsset(asset);
            case "herb" -> asset.contains("herb") || asset.contains("plant") || asset.contains("flower");
            case "metal" -> isMetalInteriorAsset(asset);
            case "forge" -> hasInteriorAssetTag(asset, InteriorAssetTag.FORGE) || hasInteriorAssetTag(asset, InteriorAssetTag.METAL);
            case "work" -> hasInteriorAssetTag(asset, InteriorAssetTag.WORK);
            default -> false;
        };
    }

    private int directionalTableScore(MapArea area, int x, int y, String chairAsset) {
        int tx = x;
        int ty = y;
        if (hasInteriorAssetTag(chairAsset, InteriorAssetTag.CHAIR_NORTH)) {
            ty = y + 1;
        } else if (hasInteriorAssetTag(chairAsset, InteriorAssetTag.CHAIR_SOUTH)) {
            ty = y - 1;
        } else if (hasInteriorAssetTag(chairAsset, InteriorAssetTag.CHAIR_EAST)) {
            tx = x - 1;
        } else if (hasInteriorAssetTag(chairAsset, InteriorAssetTag.CHAIR_WEST)) {
            tx = x + 1;
        } else {
            return 0;
        }
        int score = 0;
        for (WorldProp prop : area.props) {
            if (!isSeatingAnchorAsset(prop.asset())) {
                continue;
            }
            int[] footprint = interiorHitboxFootprint(prop.asset());
            if (tx >= prop.x() && tx < prop.x() + footprint[0]
                    && ty >= prop.y() && ty < prop.y() + footprint[1]) {
                score++;
            }
        }
        return score;
    }

    private int interiorCenterScore(MapArea area, int x, int y) {
        int cx = area.width() / 2;
        int cy = area.height() / 2;
        int distance = Math.abs(x - cx) + Math.abs(y - cy);
        return Math.max(0, 28 - distance * 3);
    }

    private int sameInteriorAssetCount(MapArea area, String asset) {
        int count = 0;
        for (WorldProp prop : area.props) {
            if (prop.asset().equals(asset)) {
                count++;
            }
        }
        return count;
    }

    private int assetRepeatPenalty(String asset) {
        if (isChairAsset(asset)) {
            return 7;
        }
        if (isFillerInteriorAsset(asset) || isBenchAsset(asset) || isStorageInteriorAsset(asset)) {
            return 3;
        }
        if (isBookshelfAsset(asset)) {
            return 4;
        }
        return 8;
    }

    private boolean isChairAsset(String asset) {
        return hasInteriorAssetTag(asset, InteriorAssetTag.CHAIR);
    }

    private boolean isBenchAsset(String asset) {
        return hasInteriorAssetTag(asset, InteriorAssetTag.BENCH);
    }

    private boolean isTableAsset(String asset) {
        return hasInteriorAssetTag(asset, InteriorAssetTag.DINING_TABLE);
    }

    private boolean isSeatingAnchorAsset(String asset) {
        return hasInteriorAssetTag(asset, InteriorAssetTag.DINING_TABLE)
                || hasInteriorAssetTag(asset, InteriorAssetTag.DESK);
    }

    private boolean isSurfaceDetailAnchor(String asset) {
        return hasInteriorAssetTag(asset, InteriorAssetTag.SURFACE)
                || hasInteriorAssetTag(asset, InteriorAssetTag.DINING_TABLE)
                || hasInteriorAssetTag(asset, InteriorAssetTag.DESK)
                || hasInteriorAssetTag(asset, InteriorAssetTag.COUNTER)
                || hasInteriorAssetTag(asset, InteriorAssetTag.WORK);
    }

    private boolean isBookshelfAsset(String asset) {
        return hasInteriorAssetTag(asset, InteriorAssetTag.BOOKSHELF);
    }

    private boolean isVerticalInteriorFurniture(String asset) {
        return hasInteriorAssetTag(asset, InteriorAssetTag.VERTICAL);
    }

    private boolean isCookingInteriorAsset(String asset) {
        return hasInteriorAssetTag(asset, InteriorAssetTag.COOKING);
    }

    private boolean isMetalInteriorAsset(String asset) {
        return hasInteriorAssetTag(asset, InteriorAssetTag.METAL);
    }

    private boolean isStorageInteriorAsset(String asset) {
        return hasInteriorAssetTag(asset, InteriorAssetTag.STORAGE);
    }

    private boolean isCounterOrWorkbenchAsset(String asset) {
        return hasInteriorAssetTag(asset, InteriorAssetTag.COUNTER)
                || hasInteriorAssetTag(asset, InteriorAssetTag.WORK);
    }

    private boolean isFillerInteriorAsset(String asset) {
        return hasInteriorAssetTag(asset, InteriorAssetTag.FILLER);
    }

    private boolean hasInteriorAssetTag(String asset, InteriorAssetTag tag) {
        return interiorAssetTags(asset).contains(tag);
    }

    private Set<InteriorAssetTag> interiorAssetTags(String asset) {
        if (asset == null || asset.isBlank()) {
            return Set.of();
        }
        return switch (asset) {
            case "interior_chair_north", "interior_chair_north_alt" ->
                    Set.of(InteriorAssetTag.CHAIR, InteriorAssetTag.CHAIR_NORTH, InteriorAssetTag.SEATING);
            case "interior_chair_south", "interior_chair_south_alt" ->
                    Set.of(InteriorAssetTag.CHAIR, InteriorAssetTag.CHAIR_SOUTH, InteriorAssetTag.SEATING);
            case "interior_chair_east", "interior_chair_east_alt" ->
                    Set.of(InteriorAssetTag.CHAIR, InteriorAssetTag.CHAIR_EAST, InteriorAssetTag.SEATING);
            case "interior_chair_west", "interior_chair_west_alt" ->
                    Set.of(InteriorAssetTag.CHAIR, InteriorAssetTag.CHAIR_WEST, InteriorAssetTag.SEATING);
            case "interior_bench_h", "interior_bench_v" ->
                    Set.of(InteriorAssetTag.BENCH, InteriorAssetTag.SEATING, asset.endsWith("_v")
                            ? InteriorAssetTag.VERTICAL : InteriorAssetTag.HORIZONTAL);
            case "interior_round_table" ->
                    Set.of(InteriorAssetTag.DINING_TABLE, InteriorAssetTag.SURFACE);
            case "interior_banquet_table_h", "interior_stool_table_h", "interior_long_table_benches",
                    "interior_table_h_left", "interior_table_h_middle", "interior_table_h_right" ->
                    Set.of(InteriorAssetTag.DINING_TABLE, InteriorAssetTag.SURFACE, InteriorAssetTag.HORIZONTAL);
            case "interior_table_v_top", "interior_table_v_middle", "interior_table_v_bottom" ->
                    Set.of(InteriorAssetTag.DINING_TABLE, InteriorAssetTag.SURFACE, InteriorAssetTag.VERTICAL);
            case "interior_study_desk_h" ->
                    Set.of(InteriorAssetTag.DESK, InteriorAssetTag.SURFACE, InteriorAssetTag.HORIZONTAL);
            case "interior_aquarium_table" ->
                    Set.of(InteriorAssetTag.DISPLAY, InteriorAssetTag.HORIZONTAL);
            case "interior_bookshelf" ->
                    Set.of(InteriorAssetTag.BOOKSHELF, InteriorAssetTag.STORAGE, InteriorAssetTag.VERTICAL);
            case "interior_bakery_counter", "interior_tavern_counter", "interior_shop_counter",
                    "interior_counter_corner_h", "interior_storage_counter", "interior_low_cupboard" ->
                    Set.of(InteriorAssetTag.COUNTER, InteriorAssetTag.STORAGE, InteriorAssetTag.SURFACE,
                            InteriorAssetTag.HORIZONTAL);
            case "interior_carpenter_workbench", "interior_sawhorse_planks", "interior_carpenter_table",
                    "interior_alchemy_station" ->
                    Set.of(InteriorAssetTag.WORK, InteriorAssetTag.SURFACE, InteriorAssetTag.HORIZONTAL);
            case "interior_cooking_station", "interior_cookpot_stand", "interior_hearth_pot",
                    "interior_stove", "interior_oven", "interior_bakery_oven" ->
                    Set.of(InteriorAssetTag.COOKING, InteriorAssetTag.SURFACE);
            case "interior_anvil", "interior_anvil_tool_rack", "interior_metal_crate", "interior_forge" ->
                    Set.of(InteriorAssetTag.METAL, asset.contains("forge") ? InteriorAssetTag.FORGE : InteriorAssetTag.STORAGE);
            case "interior_barrels", "interior_crates", "interior_traveler_trunk",
                    "interior_linen_shelf", "interior_grain_sacks_v", "interior_inn_screen_chest" ->
                    Set.of(InteriorAssetTag.STORAGE, asset.endsWith("_v") || asset.contains("trunk")
                            || asset.contains("linen") || asset.contains("screen")
                            ? InteriorAssetTag.VERTICAL : InteriorAssetTag.HORIZONTAL);
            case "interior_side_table" ->
                    Set.of(InteriorAssetTag.STORAGE, InteriorAssetTag.SURFACE);
            case "interior_flower_pot", "interior_herb_pot", "interior_herb_planter",
                    "interior_planting_pot", "interior_sprout_planter", "interior_floor_leafy_plant",
                    "interior_floor_sapling_pot", "interior_floor_bushy_planter",
                    "interior_floor_reed_pot", "interior_floor_flower_planter",
                    "interior_vine_trellis" ->
                    Set.of(InteriorAssetTag.FILLER);
            case "interior_resident_bed", "interior_bed_vertical", "interior_herb_drying_rack_v" ->
                    Set.of(InteriorAssetTag.VERTICAL);
            default -> Set.of();
        };
    }

    private enum InteriorAssetTag {
        BENCH,
        BOOKSHELF,
        CHAIR,
        CHAIR_EAST,
        CHAIR_NORTH,
        CHAIR_SOUTH,
        CHAIR_WEST,
        COOKING,
        COUNTER,
        DESK,
        DINING_TABLE,
        DISPLAY,
        FILLER,
        FORGE,
        HORIZONTAL,
        METAL,
        SEATING,
        STORAGE,
        SURFACE,
        VERTICAL,
        WORK
    }

    private record RankedInteriorCandidate(int x, int y, String asset, int score) {
    }

    private record BuildingLotCandidate(String key, int x1, int y1, int x2, int y2, int palette,
                                        List<String> styles) {
    }

    private record ScoredBuildingCandidate(CityBuilding building, int score) {
    }

    private record CityPropCandidate(String asset, int size, int score) {
    }

    private void addInteriorLightSources(MapArea area, String theme, int seed) {
        int width = area.width();
        int height = area.height();
        addFurniture(area, 3, 1, "interior_wall_sconce_lamp");
        addFurniture(area, Math.max(4, width / 2), 1, Math.floorMod(seed, 2) == 0
                ? "interior_wall_sconce_lamp"
                : "interior_wall_window_small");
        addFurniture(area, width - 4, 1, "interior_wall_sconce_lamp");
        switch (theme) {
            case "blacksmith" -> {
                addFurniture(area, 10, 6, "interior_hearth_pot");
                addFurniture(area, width - 5, 6, "interior_wall_sconce_lamp");
            }
            case "carpenter" -> {
                addFurniture(area, width - 5, 6, "interior_wall_sconce_lamp");
                addFurniture(area, 6, height - 5, "interior_tabletop_candle");
            }
            case "bakery" -> {
                addFurniture(area, 14, 8, "interior_tabletop_candle");
                addFurniture(area, width - 6, height - 5, "interior_wall_sconce_lamp");
            }
            case "inn", "tavern" -> {
                addFurniture(area, 9, 8, "interior_tabletop_candle");
                addFurniture(area, 16, 9, "interior_tabletop_candle");
                addFurniture(area, width - 5, height - 5, "interior_wall_sconce_lamp");
            }
            case "shop" -> {
                addFurniture(area, 7, 8, "interior_tabletop_candle");
                addFurniture(area, width - 6, height - 5, "interior_wall_sconce_lamp");
            }
            case "study" -> {
                addFurniture(area, 4, 5, "interior_tabletop_candle");
                addFurniture(area, 15, 8, "interior_tabletop_candle");
                addFurniture(area, width - 6, height - 5, "interior_wall_crystal_ornament");
            }
            default -> {
                addFurniture(area, width / 2, 7, "interior_tabletop_candle");
                addFurniture(area, width - 5, height - 5, "interior_wall_sconce_lamp");
            }
        }
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
                addFurniture(area, 7, height - 5, roll % 2 == 0 ? "interior_storage_counter" : "interior_metal_crate");
                addFurniture(area, width - 5, 6, "interior_oven");
                addFurniture(area, width - 7, height - 4, "interior_herb_drying_rack");
                if (roll % 3 == 0) {
                    addFurniture(area, width - 4, height - 5, "interior_grain_sacks_v");
                }
            }
            case "carpenter" -> {
                addFurniture(area, width / 2 + 4, 1, "interior_wall_herb_rack");
                addFurniture(area, width - 6, height - 5, "interior_planting_pot");
                addFurniture(area, width - 9, height - 5, "interior_sprout_planter");
                addFurniture(area, 6, height - 5, "interior_side_table");
                addFurniture(area, 6, height - 5, "interior_seed_bowl");
                addFurniture(area, 9, height - 5, "interior_floor_sapling_pot");
                addFurniture(area, width - 4, height - 5, "interior_traveler_trunk");
                addFurniture(area, width / 2, height - 5, "interior_sawhorse_planks");
            }
            case "bakery" -> {
                addFurniture(area, width / 2 + 4, 1, "interior_wall_window_small");
                addFurniture(area, width - 6, 6, "interior_grain_sacks_v");
                addFurniture(area, 4, height - 5, "interior_cookpot_stand");
                addFurniture(area, 8, height - 5, "interior_bakery_counter");
                addFurniture(area, width - 8, height - 5, "interior_flower_pot");
            }
            case "tavern", "inn" -> {
                addFurniture(area, width / 2 + 4, 1, "interior_wall_window_small");
                addFurniture(area, width - 6, 6, "interior_barrels");
                addFurniture(area, 4, height - 5, roll % 2 == 0 ? "interior_stove" : "interior_oven");
                addFurniture(area, 8, height - 5, "interior_cooking_station");
                addFurniture(area, width - 8, height - 5, "interior_herb_pot");
                addFurniture(area, width - 5, height - 6, "interior_floor_flower_planter");
                addFurniture(area, width - 4, height - 5, "interior_traveler_trunk");
                if (roll % 3 != 1) {
                    addFurniture(area, width / 2 + 4, height - 4, "interior_inn_screen_chest");
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
                addFurniture(area, width - 4, height - 5, roll % 2 == 0 ? "interior_linen_shelf" : "interior_storage_counter");
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
        List<Npc> npcs = new ArrayList<>();
        switch (theme) {
            case "blacksmith" -> {
                npcs.add(new Npc(mapId, "Blacksmith Garran", "npc_blacksmith", x - 1, y, List.of(
                        "Keep clear of the coals. Iron remembers careless hands.",
                        "If the road has teeth, bring me the metal to answer it."
                ), null, "highwall"));
                npcs.add(new Npc(mapId, "Coal Runner Tavi", "npc_citizen_woman", x + 2, y + 1, List.of(
                        "The ingots are sorted by temper, price, and how loudly they complain.",
                        "Mind the metal crates. They bite ankles before they become swords."
                ), null, null));
            }
            case "carpenter" -> {
                npcs.add(new Npc(mapId, "Carpenter Elian", "npc_citizen_man", x - 1, y, List.of(
                        "Good wood tells you how it wants to hold weight.",
                        "Bring timber and hide, and a bench like this can turn travel into preparedness."
                ), null, null));
                npcs.add(new Npc(mapId, "Apprentice Noll", "npc_citizen_woman", x + 2, y + 1, List.of(
                        "I plane boards until they stop arguing with the square.",
                        "Elian says a good workbench is mostly patience with legs."
                ), null, null));
            }
            case "bakery" -> {
                npcs.add(new Npc(mapId, "Baker Mera", "npc_baker", x - 1, y, List.of(
                        "Bread is settlement architecture you can eat.",
                        "The oven keeps better time than most town clocks."
                ), null, "riverside"));
                npcs.add(new Npc(mapId, "Hungry Regular", "npc_citizen_man", x + 2, y + 1, List.of(
                        "I am here for bread, gossip, and the heroic avoidance of chores.",
                        "Fresh loaves make bad news briefly negotiate."
                ), null, null));
            }
            case "inn" -> {
                npcs.add(new Npc(mapId, "Innkeeper Senn", "npc_innkeeper", x - 1, y, List.of(
                        "Beds upstairs, stew near the hearth, trouble preferably outside.",
                        "Travelers talk in their sleep. The honest ones apologize."
                ), null, "riverside"));
                npcs.add(new Npc(mapId, "Road-Tired Traveler", "npc_citizen_woman", x + 2, y, List.of(
                        "I paid for a bed, a lock, and three hours where nobody says 'urgent'.",
                        "The trunk is mine. The dust on it belongs to three kingdoms."
                ), null, null));
                npcs.add(new Npc(mapId, "Tablehand Cor", "npc_bartender", x, y + 2, List.of(
                        "If the table wobbles, wedge a rumor under the short leg.",
                        "People order ale, then serve themselves confessions."
                ), null, null));
            }
            case "tavern" -> {
                npcs.add(new Npc(mapId, "Bartender Senn", "npc_bartender", x - 1, y, List.of(
                        "A warm room buys more courage than most speeches.",
                        "Travelers talk when the cups are full. Listen long enough and every road has a rumor."
                ), null, "riverside"));
                npcs.add(new Npc(mapId, "Corner Regular", "npc_citizen_man", x + 2, y + 1, List.of(
                        "I saw nothing, heard everything, and remember selectively.",
                        "The chair by the wall is mine unless someone interesting needs it."
                ), null, null));
            }
            case "shop" -> npcs.add(new Npc(mapId, "Merchant Vale", "npc_merchant", x, y, List.of(
                    "Everything useful has a price. Everything priceless is usually trouble.",
                    "Look around. The shelves are better organized than the world outside."
            ), null, "riverside"));
            case "study" -> npcs.add(new Npc(mapId, "Archivist's Aide", "npc_citizen_woman", x, y, List.of(
                    "Please mind the shelves. Some of these records survived worse than weather.",
                    "A city is only stone unless someone remembers what happened inside it."
            ), null, null));
            default -> {
                npcs.add(new Npc(mapId, interiorResidentName(mapId, theme, seed, 0),
                        Math.floorMod(seed, 2) == 0 ? "npc_citizen_man" : "npc_citizen_woman", x - 1, y, List.of(
                        "Come in, but mind the floorboards.",
                        "Every house in Alderfall has a packed bag by the door now."
                ), null, null));
                npcs.add(new Npc(mapId, interiorResidentName(mapId, theme, seed, 1),
                        Math.floorMod(seed, 2) == 0 ? "npc_citizen_woman" : "npc_citizen_man",
                        x + 2, y + 1, List.of(
                        "The bed is made, the trunk is packed, and the window sticks in rain.",
                        "A home is mostly chores that learned your name."
                ), null, null));
            }
        }
        return npcs;
    }

    private String interiorResidentName(String mapId, String theme, int seed, int index) {
        int nameSeed = Math.abs((mapId + ":" + theme + ":" + seed + ":" + index).hashCode());
        String[] givenNames = {
                "Alda", "Bryn", "Cala", "Donn", "Elia", "Fenn", "Galen", "Hara",
                "Iven", "Jora", "Kell", "Lysa", "Marn", "Niva", "Oren", "Pella"
        };
        String[] bynames = {
                "Hearthlow", "Doorwick", "Warmstep", "Trunkwell", "Ashroom", "Windowmere",
                "Fieldcot", "Lowbench", "Candlemark", "Stonefloor", "Mosslint", "Roadrest"
        };
        return givenNames[Math.floorMod(nameSeed, givenNames.length)] + " "
                + bynames[Math.floorMod(nameSeed / 19, bynames.length)];
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
                        area.addProp(new WorldProp(x, y, ground, 52));
                    }
                    String entrance = adventureEntranceFor(patch, tile);
                    if (entrance != null) {
                        area.addProp(new WorldProp(x, y, entrance, locationPropSize(entrance, seed)));
                        continue;
                    }
                    double centerPull = patch.centerPull(x, y);
                    int chance = switch (patch.kind) {
                        case "farmland" -> 24;
                        case "goblin_camp", "bandit_camp" -> 34;
                        case "graveyard", "crypt", "abandoned_castle", "prison", "sewer" -> 32;
                        case "cave" -> 28;
                        case "forest_shrine", "hidden_grove" -> 30;
                        case "cave_mouth", "ruined_watchpost", "old_road_marker" -> 34;
                        default -> 26;
                    };
                    chance = Math.min(58, chance + (int) Math.round(centerPull * 18.0));
                    if (Terrain.connectingRoad(tile)) {
                        chance = Math.max(10, chance - 16);
                    }
                    if (seed % 100 >= chance) {
                        continue;
                    }
                    String asset = locationDecorationFor(patch, x, y, seed);
                    area.addProp(new WorldProp(x, y, asset, locationPropSize(asset, seed)));
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
            case "graveyard", "crypt", "abandoned_castle", "prison" -> {
                if (tile == 'd' || (Math.abs(dx) <= 1 && dy >= 0 && dy <= patch.ry)) {
                    yield "location_dungeon_approach_path";
                }
                yield seed % 100 < 42 + (int) Math.round(patch.centerPull(x, y) * 18.0)
                        ? "location_graveyard_dirt"
                        : null;
            }
            case "sewer" -> {
                if (tile == 'd' || (Math.abs(dx) <= 1 && dy >= 0 && dy <= patch.ry)) {
                    yield "location_dungeon_approach_path";
                }
                yield seed % 100 < 38 + (int) Math.round(patch.centerPull(x, y) * 18.0)
                        ? (seed % 3 == 0 ? "deco_soft_water_wet_stones" : "location_graveyard_dirt")
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
            case "forest_shrine", "hidden_grove" -> seed % 100 < 28 + (int) Math.round(patch.centerPull(x, y) * 22.0)
                    ? "deco_soft_leaf_litter"
                    : null;
            case "cave_mouth", "ruined_watchpost" -> {
                if (Math.abs(dx) <= 1 && dy >= 0 && dy <= patch.ry) {
                    yield "location_dungeon_approach_path";
                }
                yield seed % 100 < 34 + (int) Math.round(patch.centerPull(x, y) * 18.0)
                        ? "location_graveyard_dirt"
                        : null;
            }
            case "old_road_marker" -> Terrain.connectingRoad(tile) || seed % 100 < 38
                    ? "location_graveyard_path"
                    : null;
            default -> null;
        };
    }

    private String adventureEntranceFor(LocationPatch patch, char tile) {
        if (tile != 'd' || !isPrimaryAdventurePatch(patch)) {
            return null;
        }
        return switch (patch.kind) {
            case "cave" -> "location_overgrown_cave_entrance";
            case "crypt", "graveyard" -> "location_dungeon_stair_entrance";
            case "prison" -> "location_castle_ruins";
            case "sewer" -> "location_dungeon_stair_entrance";
            case "goblin_camp" -> "location_goblin_hut";
            case "bandit_camp" -> "location_bandit_outpost";
            case "abandoned_castle" -> "location_castle_ruins";
            default -> null;
        };
    }

    private boolean isPrimaryAdventurePatch(LocationPatch patch) {
        for (AdventureSite site : adventureSites) {
            if (site.x() == patch.cx && site.y() == patch.cy && site.kind().equals(patch.kind)) {
                return true;
            }
        }
        return false;
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
                return seed % 2 == 0 ? "location_dungeon_collapsed_wall" : "location_dungeon_rubble_cairn";
            }
            String[] options = {
                    "deco_rocks", "location_ruin_standing_stones", "location_dungeon_rubble_cairn",
                    "deco_imagen_forest_roots", "location_graveyard_dead_stump", "location_graveyard_path"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("crypt")) {
            if (nearAnyTile(x, y, new char[]{'d'}, 1)) {
                return seed % 2 == 0 ? "location_dungeon_braziers" : "location_dungeon_broken_altar";
            }
            if (edge) {
                return "location_graveyard_iron_fence";
            }
            String[] options = {
                    "location_crypt_sarcophagus", "location_graveyard_tombstones", "location_overgrown_landing",
                    "location_dungeon_grave_slabs", "location_dungeon_rubble_cairn",
                    "location_graveyard_skull_marker", "location_graveyard_dead_stump"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("abandoned_castle")) {
            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                return seed % 2 == 0 ? "location_dungeon_collapsed_wall" : "location_dungeon_broken_altar";
            }
            if (edge) {
                return seed % 2 == 0 ? "location_graveyard_iron_fence" : "location_dungeon_rubble_cairn";
            }
            String[] options = {
                    "location_dungeon_collapsed_wall", "location_dungeon_broken_altar",
                    "location_dungeon_rubble_cairn", "location_dungeon_grave_slabs",
                    "location_crypt_sarcophagus", "location_graveyard_tombstones", "deco_imagen_flat_stone_stack"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("prison")) {
            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                return seed % 2 == 0 ? "location_castle_ruins" : "dungeon_prop_chain_stand";
            }
            if (edge) {
                return seed % 2 == 0 ? "location_graveyard_iron_fence" : "location_dungeon_collapsed_wall";
            }
            String[] options = {
                    "location_dungeon_collapsed_wall", "dungeon_prop_chain_stand", "dungeon_prop_lantern_stand",
                    "location_dungeon_rubble_cairn", "location_camp_crates", "location_graveyard_skull_marker"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("sewer")) {
            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                return seed % 2 == 0 ? "location_dungeon_braziers" : "location_dungeon_rubble_cairn";
            }
            if (edge) {
                return seed % 2 == 0 ? "deco_soft_water_reeds_gold" : "deco_soft_water_wet_stones";
            }
            String[] options = {
                    "deco_soft_water_wet_stones", "deco_soft_water_reeds_gold", "deco_imagen_marsh_bubble_pool",
                    "location_dungeon_rubble_cairn", "location_camp_crates", "dungeon_prop_lantern_stand"
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
                    "location_graveyard_skull_marker", "location_dungeon_grave_slabs",
                    "location_dungeon_rubble_cairn", "location_dungeon_broken_altar"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("forest_shrine")) {
            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                return "quest_forest_relic";
            }
            if (edge) {
                return seed % 2 == 0 ? "deco_imagen_forest_roots" : "deco_tree_young";
            }
            String[] options = {
                    "quest_forest_relic", "quest_ward_marker", "deco_imagen_shrine_stone",
                    "location_ruin_standing_stones", "deco_imagen_green_rune_stone",
                    "deco_soft_mossy_rock", "deco_soft_purple_flowers"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("hidden_grove")) {
            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                return seed % 2 == 0 ? "quest_mushroom_samples" : "deco_forest_mushrooms";
            }
            String[] options = {
                    "quest_mushroom_samples", "deco_forest_mushrooms", "deco_imagen_forest_roots",
                    "deco_tree_young", "deco_soft_dense_meadow_flowers", "deco_soft_moss_stones"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("cave_mouth")) {
            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                return seed % 2 == 0 ? "location_overgrown_cave_entrance" : "quest_cave_rune_cache";
            }
            if (edge) {
                return "location_dungeon_rubble_cairn";
            }
            String[] options = {
                    "quest_cave_rune_cache", "location_dungeon_collapsed_wall", "location_dungeon_rubble_cairn",
                    "deco_rocks", "deco_imagen_crystal_cluster", "location_ruin_standing_stones"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("ruined_watchpost")) {
            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                return "quest_watchpost_signal";
            }
            if (edge) {
                return seed % 2 == 0 ? "location_dungeon_collapsed_wall" : "location_camp_palisade";
            }
            String[] options = {
                    "quest_watchpost_signal", "quest_trail_marker_post", "location_dungeon_collapsed_wall",
                    "location_dungeon_rubble_cairn", "quest_broken_road_signs", "location_camp_crates"
            };
            return pick(options, seed);
        }
        if (patch.kind.equals("old_road_marker")) {
            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                return "quest_trail_marker_post";
            }
            String[] options = {
                    "quest_trail_marker_post", "quest_roadwatch_warning_marks", "quest_broken_road_signs",
                    "quest_supply_cache", "deco_imagen_milestone", "location_camp_crates"
            };
            return pick(options, seed);
        }
        return "deco_bush";
    }

    private int locationPropSize(String asset, int seed) {
        int base = 42 + seed % 8;
        return switch (asset) {
            case "location_camp_fire", "location_camp_crates", "location_graveyard_skull_marker" -> 38;
            case "dungeon_prop_chain_stand", "dungeon_prop_lantern_stand" -> 40;
            case "deco_soft_water_wet_stones", "deco_soft_water_reeds_gold", "deco_imagen_marsh_bubble_pool" -> 42;
            case "quest_forest_relic", "quest_mushroom_samples", "quest_cave_rune_cache",
                    "quest_trail_marker_post", "quest_watchpost_signal" -> 42;
            case "location_dungeon_approach_path" -> 50;
            case "location_farmland_fence", "location_graveyard_iron_fence" -> 46;
            case "location_camp_tent", "location_camp_palisade", "location_farmland_scarecrow",
                    "location_graveyard_tombstones" -> 54;
            case "location_ruin_standing_stones", "location_overgrown_landing" -> 58;
            case "location_crypt_sarcophagus" -> 56;
            case "location_dungeon_rubble_cairn", "location_dungeon_grave_slabs" -> 52;
            case "location_dungeon_broken_altar" -> 58;
            case "location_dungeon_collapsed_wall" -> 60;
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
            case 'r', 'T', 'K' -> roadDecorationFor(area, x, y, roll, patchSeed);
            default -> pick(new String[]{"deco_bush", "deco_flowers"}, roll);
        };
    }

    private String grassDecorationFor(MapArea area, int x, int y, int roll, int patchSeed) {
        boolean nearRoad = distanceToRoad(area, x, y, 2) <= 1;
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
                option("deco_tree_oak_harvestable", forestCore ? 8 : 4),
                option("deco_tree_birch_harvestable", forestCore ? 6 : 5),
                option("deco_tree_pine_harvestable", forestCore ? 7 : 5),
                option("deco_tree_willow_harvestable", edge ? 6 : 3),
                option("deco_tree_maple_harvestable", forestCore ? 6 : 3),
                option("deco_tree_ash_harvestable", forestCore ? 5 : 3),
                option("deco_tree_fruit_harvestable", edge ? 5 : 2, 420),
                option("deco_tree_elder_harvestable", forestCore ? 3 : 1, 220),
                option("deco_tree_magical_harvestable", forestCore ? 2 : 1, 120),
                option("deco_tree_deadwood_harvestable", edge ? 4 : 2, 360),
                option("deco_wood_ironwood_log_pile", forestCore ? 3 : 1, 180),
                option("deco_tree_enchanted_stump", forestCore ? 2 : 1, 120),
                option("deco_wood_fallen_ash_log", 4, 420),
                option("deco_tree_glowing_root_cluster", forestCore ? 2 : 1, 95),
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
        boolean pass = area.tileAt(x, y) == 'q' || distanceToRoad(area, x, y, 2) <= 1;
        boolean snow = distanceToAny(area, x, y, 3, new char[]{'n'}) <= 2;
        return weightedDecoration(roll, patchSeed, new DecorationOption[]{
                option("deco_mountain_rocks", pass ? 28 : 46),
                option("deco_rocks", 24),
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
                || asset.equals("deco_tree_young")
                || asset.equals("deco_tree_oak_harvestable")
                || asset.equals("deco_tree_birch_harvestable")
                || asset.equals("deco_tree_pine_harvestable")
                || asset.equals("deco_tree_willow_harvestable")
                || asset.equals("deco_tree_maple_harvestable")
                || asset.equals("deco_tree_ash_harvestable")
                || asset.equals("deco_tree_elder_harvestable")
                || asset.equals("deco_tree_magical_harvestable")
                || asset.equals("deco_tree_deadwood_harvestable")
                || asset.equals("deco_tree_fruit_harvestable");
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
                || asset.equals("deco_forest_fern")
                || asset.equals("deco_tree_enchanted_stump")
                || asset.equals("deco_tree_glowing_root_cluster");
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

    private int distanceToRoad(MapArea area, int x, int y, int radius) {
        int best = radius + 1;
        for (int oy = -radius; oy <= radius; oy++) {
            for (int ox = -radius; ox <= radius; ox++) {
                int distance = Math.abs(ox) + Math.abs(oy);
                if (distance >= best) {
                    continue;
                }
                if (Terrain.connectingRoad(area.tileAt(x + ox, y + oy))) {
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

    private boolean isShoreWater(MapArea area, int x, int y, char tile) {
        if (tile == '~') {
            return true;
        }
        if (tile != 'w') {
            return false;
        }
        return distanceToDifferentNatural(area, x, y, tile, 2) <= 1
                || distanceToAny(area, x, y, 2, new char[]{'~'}) <= 1;
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
                if (islandScore(x, y, seedSalt) > 0.01) {
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
        upgradeRoadsNearCitiesToCobblestone();
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
        addAdventureSite("prison", "dungeon_ironbarrow_1", "Ironbarrow Prison",
                129, 119, 1, 6, 5, 950, new TilePoint(131, 121));
        addAdventureSite("sewer", "dungeon_belltower_sluice_1", "Belltower Sluice",
                238, 183, 1, 7, 5, 960, new TilePoint(228, 185));

        TilePoint goblinCamp = chooseLocationCenter(
                980 + seedSalt, 176, 112, 38, 30,
                new char[]{'g', 'f', 's', 'b'}, new char[]{'r', 'T', 'K', 'q'}, 7, 15
        );
        if (goblinCamp != null) {
            addAdventureSite("goblin_camp", "dungeon_redcap_camp_1", "Redcap Goblin Camp",
                    goblinCamp.x(), goblinCamp.y(), 1, 5, 5, 981, new TilePoint(152, 145));
        }

        TilePoint banditCamp = chooseLocationCenter(
                990 + seedSalt, 214, 218, 34, 28,
                new char[]{'g', 'f', 's', 'b', 'v'}, new char[]{'r', 'T', 'K', 'q'}, 7, 15
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
                    18, 16, new char[]{'g', 's'}, new char[]{'r', 'T', 'K', 'u', 'c'}, 6, 10
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
                    28, 24, new char[]{'g', 'f', 's', 'b', 'q'}, new char[]{'r', 'T', 'K', 'q'}, 8, 14
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

        int[][] forestAnchors = {
                {109, 91}, {153, 119}, {183, 171}, {232, 139}, {72, 183}, {211, 208}
        };
        for (int i = 0; i < forestAnchors.length; i++) {
            TilePoint center = chooseLocationCenter(
                    1040 + i * 47 + seedSalt, forestAnchors[i][0], forestAnchors[i][1],
                    24, 22, new char[]{'f', 'g', 'v'}, new char[]{'f'}, 5, 12
            );
            if (center != null) {
                String kind = i % 2 == 0 ? "forest_shrine" : "hidden_grove";
                addLocationPatch(kind, center.x(), center.y(), 4 + hash(i, 1049, 5) % 3, 4 + hash(i, 1051, 7) % 3, 1050 + i);
            }
        }

        int[][] caveMouthAnchors = {
                {64, 215}, {202, 64}, {257, 183}, {184, 239}, {126, 226}
        };
        for (int i = 0; i < caveMouthAnchors.length; i++) {
            TilePoint center = chooseLocationCenter(
                    1120 + i * 53 + seedSalt, caveMouthAnchors[i][0], caveMouthAnchors[i][1],
                    22, 20, new char[]{'q', 'm', 'f', 'b'}, new char[]{'q', 'm', 'd'}, 6, 12
            );
            if (center != null) {
                addLocationPatch("cave_mouth", center.x(), center.y(), 4 + hash(i, 1129, 7) % 3, 4 + hash(i, 1133, 11) % 2, 1130 + i);
            }
        }

        int[][] roadAnchors = {
                {132, 130}, {209, 105}, {234, 184}, {96, 247}, {169, 199}, {253, 224}
        };
        for (int i = 0; i < roadAnchors.length; i++) {
            TilePoint center = chooseLocationCenter(
                    1200 + i * 59 + seedSalt, roadAnchors[i][0], roadAnchors[i][1],
                    20, 18, new char[]{'g', 'f', 's', 'b', 'v', 'r', 'T', 'K'}, new char[]{'r', 'T', 'K'}, 5, 10
            );
            if (center != null) {
                String kind = i % 2 == 0 ? "ruined_watchpost" : "old_road_marker";
                addLocationPatch(kind, center.x(), center.y(), 4 + hash(i, 1201, 5) % 3, 3 + hash(i, 1207, 7) % 3, 1210 + i);
            }
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
                case "forest_shrine" -> "Forest Shrine";
                case "hidden_grove" -> "Hidden Grove";
                case "cave_mouth" -> "Cave Mouth";
                case "ruined_watchpost" -> "Ruined Watchpost";
                case "old_road_marker" -> "Old Road Marker";
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
        double nx = (x - 151) / 143.0;
        double ny = (y - 152) / 136.0;
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
        return tile == 'w' || tile == '~' || Terrain.connectingRoad(tile);
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
        int bridgeRun = isBridgeTile(x, y) ? 1 : 0;
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
                if (isBridgeableWaterTile(x, y) && bridgeRun >= MAX_CONTINUOUS_BRIDGE_SPAN) {
                    stampBridgeLanding(x, y);
                    bridgeRun = 0;
                }
                markRoad(x, y);
                bridgeRun = isBridgeTile(x, y) ? bridgeRun + 1 : 0;
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

    private void upgradeRoadsNearCitiesToCobblestone() {
        int[][] cityCenters = {
                {82, 105}, {152, 145}, {205, 78}, {228, 185}, {150, 230},
                {131, 121}, {225, 108}, {170, 180}, {259, 205}, {126, 269}
        };
        for (int[] city : cityCenters) {
            upgradeConnectedCityApproachRoads(city[0], city[1]);
        }
    }

    private void upgradeConnectedCityApproachRoads(int cityX, int cityY) {
        int[][] distance = new int[ROWS][COLS];
        for (int[] row : distance) {
            Arrays.fill(row, -1);
        }
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        distance[cityY][cityX] = 0;
        queue.add(new TilePoint(cityX, cityY));
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        while (!queue.isEmpty()) {
            TilePoint point = queue.removeFirst();
            int currentDistance = distance[point.y()][point.x()];
            if (currentDistance >= CITY_COBBLESTONE_ROAD_RADIUS) {
                continue;
            }
            for (int[] dir : dirs) {
                int nx = point.x() + dir[0];
                int ny = point.y() + dir[1];
                if (!insideOverworld(nx, ny) || distance[ny][nx] >= 0 || !Terrain.connectingRoad(tiles[ny][nx])) {
                    continue;
                }
                distance[ny][nx] = currentDistance + 1;
                if (tiles[ny][nx] == Terrain.DIRT_ROAD || tiles[ny][nx] == 'q') {
                    tiles[ny][nx] = Terrain.COBBLESTONE_ROAD;
                }
                queue.addLast(new TilePoint(nx, ny));
            }
        }
    }

    private boolean isBridgeableWaterTile(int x, int y) {
        if (x < 0 || y < 0 || x >= COLS || y >= ROWS) {
            return false;
        }
        return tiles[y][x] == 'w' || tiles[y][x] == '~' || tiles[y][x] == 'B';
    }

    private boolean isBridgeTile(int x, int y) {
        return x >= 0 && y >= 0 && x < COLS && y < ROWS && tiles[y][x] == 'B';
    }

    private void stampBridgeLanding(int cx, int cy) {
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                int x = cx + ox;
                int y = cy + oy;
                if (x < 0 || y < 0 || x >= COLS || y >= ROWS || !isBridgeableWaterTile(x, y)) {
                    continue;
                }
                tiles[y][x] = Math.abs(ox) + Math.abs(oy) <= 1 ? 's' : '~';
            }
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
        softenSettlementApproaches(cx, cy, tile);
        connectSettlementGateRoads(cx, cy);
        landmarks.put(new TilePoint(cx, cy), name);
    }

    private void connectSettlementGateRoads(int cx, int cy) {
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int[] dir : dirs) {
            for (int distance = 0; distance <= 3; distance++) {
                markSettlementRoad(cx + dir[0] * distance, cy + dir[1] * distance);
            }
            TilePoint anchor = new TilePoint(cx + dir[0] * 3, cy + dir[1] * 3);
            TilePoint nearbyRoad = nearestRoadBeyondSettlementGate(anchor, dir[0], dir[1]);
            if (nearbyRoad != null) {
                roadPath(List.of(anchor, nearbyRoad));
            }
        }
    }

    private TilePoint nearestRoadBeyondSettlementGate(TilePoint gate, int dx, int dy) {
        TilePoint best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int distance = 1; distance <= 9; distance++) {
            int lateralLimit = distance <= 3 ? 2 : 3;
            for (int lateral = -lateralLimit; lateral <= lateralLimit; lateral++) {
                int x = gate.x() + dx * distance + (dy == 0 ? 0 : lateral);
                int y = gate.y() + dy * distance + (dx == 0 ? 0 : lateral);
                if (!insideOverworld(x, y) || !isRoadTile(tiles[y][x])) {
                    continue;
                }
                int score = distance * 8 + Math.abs(lateral);
                if (score < bestScore) {
                    bestScore = score;
                    best = new TilePoint(x, y);
                }
            }
            if (best != null && distance >= 3) {
                return best;
            }
        }
        return best;
    }

    private void markSettlementRoad(int x, int y) {
        if (!insideOverworld(x, y)) {
            return;
        }
        if (tiles[y][x] == 'w' || tiles[y][x] == '~' || tiles[y][x] == 'B') {
            tiles[y][x] = 'B';
        } else if (tiles[y][x] == 'm' || tiles[y][x] == 'q') {
            tiles[y][x] = 'q';
        } else {
            tiles[y][x] = 'r';
        }
    }

    private boolean isRoadTile(char tile) {
        return Terrain.connectingRoad(tile);
    }

    private void softenSettlementApproaches(int cx, int cy, char settlementTile) {
        char apron = blendedSettlementApronTile(cx, cy);
        for (int y = cy - 5; y <= cy + 5; y++) {
            for (int x = cx - 5; x <= cx + 5; x++) {
                if (!insideOverworld(x, y)) {
                    continue;
                }
                int chebyshev = Math.max(Math.abs(x - cx), Math.abs(y - cy));
                int manhattan = Math.abs(x - cx) + Math.abs(y - cy);
                if (chebyshev < 3 || chebyshev > 5 || manhattan > 7 || isProtectedOverworldTile(tiles[y][x])) {
                    continue;
                }
                int chance = chebyshev == 3 ? 62 : chebyshev == 4 ? 34 : 16;
                if (Math.floorMod(hash(x, y, 577 + settlementTile), 100) < chance) {
                    tiles[y][x] = apron;
                }
            }
        }
    }

    private char blendedSettlementApronTile(int cx, int cy) {
        char best = 'g';
        int bestScore = -1;
        char[] candidates = {'g', 'f', 's', 'n', 'v', 'b', 'P'};
        for (char candidate : candidates) {
            int score = 0;
            for (int y = cy - 6; y <= cy + 6; y++) {
                for (int x = cx - 6; x <= cx + 6; x++) {
                    if (!insideOverworld(x, y) || tiles[y][x] != candidate) {
                        continue;
                    }
                    int distance = Math.max(Math.abs(x - cx), Math.abs(y - cy));
                    score += Math.max(1, 7 - distance);
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private boolean insideOverworld(int x, int y) {
        return x >= 0 && y >= 0 && x < COLS && y < ROWS;
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
