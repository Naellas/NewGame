package com.alderfall.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;

public final class SaveSystem {
    private static final DateTimeFormatter SAVE_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final Path savesDir;
    private final Path legacySavePath;

    public record SaveSummary(
            String saveId,
            String characterId,
            String saveName,
            String name,
            String className,
            int level,
            String mapId,
            int gold,
            int activeQuests,
            int completedQuests,
            int partySize,
            long savedAtMillis
    ) {
    }

    public SaveSystem(Path javaRoot) {
        this.savesDir = javaRoot.resolve("saves");
        this.legacySavePath = savesDir.resolve("save.properties");
    }

    public boolean exists() {
        return !listSaves().isEmpty();
    }

    public List<SaveSummary> listSaves() {
        List<SaveSummary> summaries = new ArrayList<>();
        if (!Files.exists(savesDir)) {
            return summaries;
        }
        try (var stream = Files.walk(savesDir, 2)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".properties"))
                    .forEach(path -> readSummary(path, summaries));
        } catch (IOException ignored) {
        }
        summaries.sort(Comparator.comparingLong(SaveSummary::savedAtMillis).reversed());
        return summaries;
    }

    public List<SaveSummary> listSavesForCharacter(String characterId) {
        String cleaned = saveIdFor(characterId);
        return listSaves().stream()
                .filter(summary -> summary.characterId().equals(cleaned))
                .toList();
    }

    public static String saveIdFor(String name) {
        String base = name == null ? "" : name.strip().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        base = base.replaceAll("^-+|-+$", "");
        return base.isBlank() ? "adventure" : base.substring(0, Math.min(80, base.length()));
    }

    public void save(GameState state) throws IOException {
        save(state, state.pendingSaveName);
    }

    public void save(GameState state, String saveName) throws IOException {
        state.refreshPlayerVillageGrowth();
        String characterId = saveIdFor(state.player.name);
        String displaySaveName = saveName == null || saveName.isBlank() ? state.world.label(state.currentMapId) : saveName.strip();
        String saveId = nextSaveId(state, displaySaveName);
        Path path = savePath(saveId);
        Files.createDirectories(path.getParent());
        Properties props = new Properties();
        SavePosition position = normalizedSavePosition(state);
        props.setProperty("saveId", saveId);
        props.setProperty("characterId", characterId);
        props.setProperty("saveName", displaySaveName);
        props.setProperty("savedAt", Instant.now().toString());
        props.setProperty("name", state.player.name);
        props.setProperty("className", state.player.className);
        props.setProperty("mapId", position.mapId());
        props.setProperty("zoom", Integer.toString(state.zoom));
        props.setProperty("worldTick", Integer.toString(state.worldTick));
        props.setProperty("x", Integer.toString(position.x()));
        props.setProperty("y", Integer.toString(position.y()));
        props.setProperty("hp", Integer.toString(state.player.hp));
        props.setProperty("mp", Integer.toString(state.player.mp));
        props.setProperty("maxHp", Integer.toString(state.player.maxHp));
        props.setProperty("maxMp", Integer.toString(state.player.maxMp));
        props.setProperty("attack", Integer.toString(state.player.attack));
        props.setProperty("defense", Integer.toString(state.player.defense));
        props.setProperty("level", Integer.toString(state.player.level));
        props.setProperty("xp", Integer.toString(state.player.xp));
        props.setProperty("gold", Integer.toString(state.player.gold));
        props.setProperty("inventory", writeInventory(state.player));
        props.setProperty("equipment", writeEquipment(state.player));
        props.setProperty("skillPoints", Integer.toString(state.player.skillPoints));
        props.setProperty("skillAllocations", writeSkillAllocations(state.player));
        props.setProperty("professionXp", writeProfessionXp(state.player));
        props.setProperty("quests", writeQuests(state));
        props.setProperty("harvestedQuestResources", writeHarvestedQuestResources(state));
        props.setProperty("npcRelationships", writeNpcRelationships(state));
        props.setProperty("recruits", writeRecruits(state));
        props.setProperty("villageAllies", writeVillageAllies(state));
        props.setProperty("villageWorkerRoles", writeVillageWorkerRoles(state));
        props.setProperty("villageBuildingAssignments", writeVillageBuildingAssignments(state));
        props.setProperty("villageStorage", writeVillageStorage(state));
        props.setProperty("playerVillageStage", Integer.toString(state.world.playerVillageStage()));
        props.setProperty("villageTiles", writeVillageTiles(state));
        props.setProperty("villageBuildings", writeVillageBuildings(state));
        props.setProperty("villageBuildingLevels", writeVillageBuildingLevels(state));
        props.setProperty("villageProps", writeVillageProps(state));
        props.setProperty("villageInteriorProps", writeVillageInteriorProps(state));
        writeActor(props, "player.", state.player);
        props.setProperty("allyCount", Integer.toString(state.allies.size()));
        for (int i = 0; i < state.allies.size(); i++) {
            writeActor(props, "ally." + i + ".", state.allies.get(i));
        }
        try (var out = Files.newOutputStream(path)) {
            props.store(out, "Echoes of Alderfall Java save");
        }
        state.currentSaveId = saveId;
    }

    private String nextSaveId(GameState state, String saveName) {
        String characterId = saveIdFor(state.player.name);
        String base = saveIdFor(saveName);
        String timestamp = LocalDateTime.now().format(SAVE_ID_TIME);
        String saveId = characterId + "/" + saveIdFor(base + "-" + timestamp);
        int suffix = 2;
        while (Files.exists(savePath(saveId))) {
            saveId = characterId + "/" + saveIdFor(base + "-" + timestamp + "-" + suffix);
            suffix++;
        }
        return saveId;
    }

    public boolean load(GameState state) throws IOException {
        List<SaveSummary> saves = listSaves();
        if (saves.isEmpty()) {
            return false;
        }
        return load(state, saves.get(0).saveId());
    }

    public boolean load(GameState state, String saveId) throws IOException {
        Path path = savePath(saveId);
        if (!Files.exists(path) && "save".equals(saveId) && Files.exists(legacySavePath)) {
            path = legacySavePath;
        }
        if (!Files.exists(path)) {
            return false;
        }
        Properties props = new Properties();
        try (var in = Files.newInputStream(path)) {
            props.load(in);
        }
        Actor player = readPlayer(props);
        state.player = player;
        state.syncSkillAbilities();
        state.currentSaveId = props.getProperty("saveId", fallbackSaveIdForPath(path));
        state.pendingPlayerName = player.name;
        state.pendingSaveName = props.getProperty("saveName", state.world.label(props.getProperty("mapId", WorldMap.OVERWORLD_ID)));
        String mapId = props.getProperty("mapId", WorldMap.OVERWORLD_ID);
        state.setZoom(readInt(props, "zoom", state.zoom));
        state.worldTick = readInt(props, "worldTick", 0);
        int x = readInt(props, "x", WorldMap.START_POSITION.x());
        int y = readInt(props, "y", WorldMap.START_POSITION.y());
        state.battle = null;
        state.activeNpc = null;
        state.activeShop = null;
        state.dialogIndex = 0;
        state.mode = GameMode.EXPLORE;
        state.world.clearPlayerVillageCustomizations();
        state.villageStorage.clear();
        state.villageWorkerRoles.clear();
        state.villageBuildingAssignments.clear();
        state.world.setPlayerVillageStage(readInt(props, "playerVillageStage", 1));
        readVillageStorage(props.getProperty("villageStorage", ""), state);
        readVillageTiles(props.getProperty("villageTiles", ""), state);
        readVillageBuildings(props.getProperty("villageBuildings", ""), state);
        readVillageBuildingLevels(props.getProperty("villageBuildingLevels", ""), state);
        readVillageProps(props.getProperty("villageProps", ""), state);
        readVillageInteriorProps(props.getProperty("villageInteriorProps", ""), state);
        readQuests(props.getProperty("quests", ""), state);
        readHarvestedQuestResources(props.getProperty("harvestedQuestResources", ""), state);
        readNpcRelationships(props.getProperty("npcRelationships", ""), state);
        readParty(props, state);
        readVillageAllies(props.getProperty("villageAllies", ""), state);
        readVillageWorkerRoles(props.getProperty("villageWorkerRoles", ""), state);
        readVillageBuildingAssignments(props.getProperty("villageBuildingAssignments", ""), state);
        if (!state.world.hasMap(mapId) || mapId.startsWith("house_" + WorldMap.PLAYER_VILLAGE_ID + "_")) {
            if (mapId.startsWith("house_" + WorldMap.PLAYER_VILLAGE_ID + "_")) {
                mapId = WorldMap.PLAYER_VILLAGE_ID;
                x = 14;
                y = 17;
            } else {
                mapId = WorldMap.OVERWORLD_ID;
                x = WorldMap.START_POSITION.x();
                y = WorldMap.START_POSITION.y();
            }
        }
        TilePoint safePosition = safePosition(state.world, mapId, x, y);
        state.currentMapId = mapId;
        state.playerX = safePosition.x();
        state.playerY = safePosition.y();
        state.syncAllSkillAbilities();
        state.refreshPlayerVillageGrowth();
        state.resetNpcRuntime();
        state.resetDungeonMonsterRuntime();
        state.status = "Loaded save.";
        return true;
    }

    private SavePosition normalizedSavePosition(GameState state) {
        String mapId = state.currentMapId;
        int x = state.playerX;
        int y = state.playerY;
        if ("interior".equals(state.world.kind(mapId))) {
            WorldTransition currentExit = state.world.transitionAt(mapId, x, y);
            if (currentExit != null && state.world.hasMap(currentExit.targetMapId())) {
                return new SavePosition(currentExit.targetMapId(), currentExit.targetX(), currentExit.targetY());
            }
            int startY = Math.max(0, state.world.height(mapId) - 4);
            for (int exitY = startY; exitY < state.world.height(mapId); exitY++) {
                for (int exitX = 0; exitX < state.world.width(mapId); exitX++) {
                    WorldTransition exit = state.world.transitionAt(mapId, exitX, exitY);
                    if (exit != null && state.world.hasMap(exit.targetMapId())) {
                        return new SavePosition(exit.targetMapId(), exit.targetX(), exit.targetY());
                    }
                }
            }
            for (int exitX : List.of(11, 12)) {
                WorldTransition exit = state.world.transitionAt(mapId, exitX, 15);
                if (exit != null && state.world.hasMap(exit.targetMapId())) {
                    return new SavePosition(exit.targetMapId(), exit.targetX(), exit.targetY());
                }
            }
        }
        return new SavePosition(mapId, x, y);
    }

    private TilePoint safePosition(WorldMap world, String mapId, int x, int y) {
        int width = world.width(mapId);
        int height = world.height(mapId);
        x = clamp(x, 0, Math.max(0, width - 1));
        y = clamp(y, 0, Math.max(0, height - 1));
        if (world.isPassable(mapId, x, y)) {
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
                    if (nx >= 0 && ny >= 0 && nx < width && ny < height && world.isPassable(mapId, nx, ny)) {
                        return new TilePoint(nx, ny);
                    }
                }
            }
        }
        return WorldMap.OVERWORLD_ID.equals(mapId) ? WorldMap.START_POSITION : new TilePoint(0, 0);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Path savePath(String saveId) {
        String normalized = saveId == null ? "" : saveId.replace('\\', '/');
        String[] parts = normalized.split("/", 2);
        if (parts.length == 2) {
            return savesDir.resolve(saveIdFor(parts[0])).resolve(saveIdFor(parts[1]) + ".properties");
        }
        return savesDir.resolve(saveIdFor(normalized) + ".properties");
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            return 0L;
        }
    }

    private void readSummary(Path path, List<SaveSummary> summaries) {
        Properties props = new Properties();
        try (var in = Files.newInputStream(path)) {
            props.load(in);
            String fallbackId = fallbackSaveIdForPath(path);
            QuestCounts questCounts = questCounts(props.getProperty("quests", ""));
            long modifiedAt = lastModified(path);
            String characterId = props.getProperty("characterId", characterIdForSummary(path, props));
            String saveName = props.getProperty("saveName", fallbackSaveName(fallbackId, props.getProperty("name", "Hero")));
            summaries.add(new SaveSummary(
                    props.getProperty("saveId", fallbackId),
                    saveIdFor(characterId),
                    saveName,
                    props.getProperty("name", "Hero"),
                    props.getProperty("className", "Knight"),
                    readInt(props, "level", 1),
                    props.getProperty("mapId", WorldMap.OVERWORLD_ID),
                    readInt(props, "gold", 0),
                    questCounts.active(),
                    questCounts.completed(),
                    1 + Math.max(0, readInt(props, "allyCount", countCsv(props.getProperty("recruits", "")))),
                    readSavedAt(props.getProperty("savedAt", ""), modifiedAt)
            ));
        } catch (IOException ignored) {
        }
    }

    private String fallbackSaveIdForPath(Path path) {
        String fileName = path.getFileName().toString();
        String stem = fileName.substring(0, fileName.length() - ".properties".length());
        Path parent = path.getParent();
        if (parent != null && savesDir.equals(parent.getParent())) {
            return parent.getFileName().toString() + "/" + stem;
        }
        return stem;
    }

    private String characterIdForSummary(Path path, Properties props) {
        Path parent = path.getParent();
        if (parent != null && savesDir.equals(parent.getParent())) {
            return parent.getFileName().toString();
        }
        return saveIdFor(props.getProperty("name", "Hero"));
    }

    private String fallbackSaveName(String fallbackId, String characterName) {
        String cleaned = fallbackId.replace('\\', '/');
        int slash = cleaned.indexOf('/');
        if (slash >= 0 && slash < cleaned.length() - 1) {
            cleaned = cleaned.substring(slash + 1);
        }
        String characterPrefix = saveIdFor(characterName) + "-";
        if (cleaned.startsWith(characterPrefix)) {
            cleaned = cleaned.substring(characterPrefix.length());
        }
        cleaned = cleaned.replaceAll("-?\\d{8}-\\d{6}(-\\d+)?$", "");
        cleaned = cleaned.replace('-', ' ').strip();
        return cleaned.isBlank() ? "Adventure" : titleCase(cleaned);
    }

    private String titleCase(String value) {
        StringBuilder result = new StringBuilder();
        for (String word : value.split("\\s+")) {
            if (word.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }
        return result.toString();
    }

    private long readSavedAt(String value, long fallback) {
        if (value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private QuestCounts questCounts(String value) {
        int active = 0;
        int completed = 0;
        if (value.isBlank()) {
            return new QuestCounts(active, completed);
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":");
            if (fields.length != 4) {
                continue;
            }
            boolean accepted = Boolean.parseBoolean(fields[1]);
            boolean done = Boolean.parseBoolean(fields[2]);
            if (done) {
                completed++;
            } else if (accepted) {
                active++;
            }
        }
        return new QuestCounts(active, completed);
    }

    private int countCsv(String value) {
        if (value.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private String writeInventory(Actor player) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : player.inventory.entrySet()) {
            joiner.add(entry.getKey() + ":" + entry.getValue());
        }
        return joiner.toString();
    }

    private void readInventory(String value, Actor player) {
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] pair = part.split(":", 2);
            if (pair.length != 2) {
                continue;
            }
            try {
                int amount = Integer.parseInt(pair[1]);
                if (amount > 0) {
                    player.addItem(pair[0], amount);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private String writeEquipment(Actor player) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : player.equipment.entrySet()) {
            joiner.add(entry.getKey() + ":" + entry.getValue());
        }
        return joiner.toString();
    }

    private void readEquipment(String value, Actor player) {
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] pair = part.split(":", 2);
            if (pair.length == 2) {
                Equipment equipment = GameData.EQUIPMENT.get(pair[1]);
                if (equipment != null) {
                    player.equipment.put(equipment.slot(), pair[1]);
                }
            }
        }
    }

    private String writeSkillAllocations(Actor player) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : player.skillAllocations.entrySet()) {
            joiner.add(entry.getKey() + ":" + entry.getValue());
        }
        return joiner.toString();
    }

    private void readSkillAllocations(String value, Actor player) {
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] pair = part.split(":", 2);
            if (pair.length != 2 || !SkillTrees.SKILL_TREE.containsKey(pair[0])) {
                continue;
            }
            try {
                int rank = Integer.parseInt(pair[1]);
                int maxRank = SkillTrees.SKILL_TREE.get(pair[0]).maxRank();
                if (rank > 0) {
                    player.skillAllocations.put(pair[0], Math.min(rank, maxRank));
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private String writeProfessionXp(Actor player) {
        StringJoiner joiner = new StringJoiner(",");
        for (Profession profession : Profession.ALL) {
            int xp = player.professionXp.getOrDefault(profession.id(), 0);
            if (xp > 0) {
                joiner.add(profession.id() + ":" + xp);
            }
        }
        return joiner.toString();
    }

    private void readProfessionXp(String value, Actor player) {
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] pair = part.split(":", 2);
            if (pair.length != 2 || Profession.byId(pair[0]) == null) {
                continue;
            }
            try {
                int xp = Integer.parseInt(pair[1]);
                if (xp >= 0) {
                    player.professionXp.put(pair[0], xp);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private String writeQuests(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (Quest quest : state.quests.values()) {
            joiner.add(quest.id + ":" + quest.accepted + ":" + quest.completed + ":" + quest.progress);
        }
        return joiner.toString();
    }

    private String writeRecruits(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (String recruitId : state.recruitedIds) {
            joiner.add(recruitId);
        }
        return joiner.toString();
    }

    private String writeVillageAllies(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (String allyName : state.villageAllies) {
            joiner.add(allyName);
        }
        return joiner.toString();
    }

    private String writeVillageWorkerRoles(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.villageWorkerRoles.entrySet()) {
            joiner.add(escape(entry.getKey()) + ":" + entry.getValue());
        }
        return joiner.toString();
    }

    private String writeVillageBuildingAssignments(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.villageBuildingAssignments.entrySet()) {
            joiner.add(entry.getKey() + ":" + escape(entry.getValue()));
        }
        return joiner.toString();
    }

    private String writeVillageStorage(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.villageStorage.entrySet()) {
            joiner.add(entry.getKey() + ":" + entry.getValue());
        }
        return joiner.toString();
    }

    private String writeVillageTiles(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.world.playerVillageTiles().entrySet()) {
            joiner.add(entry.getKey().x() + ":" + entry.getKey().y() + ":" + entry.getValue());
        }
        return joiner.toString();
    }

    private String writeVillageBuildings(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (CityBuilding building : state.world.playerVillageBuildings()) {
            joiner.add(String.join(":",
                    building.key(),
                    Integer.toString(building.x1()),
                    Integer.toString(building.y1()),
                    Integer.toString(building.x2()),
                    Integer.toString(building.y2()),
                    building.style(),
                    Integer.toString(building.palette())
            ));
        }
        return joiner.toString();
    }

    private String writeVillageBuildingLevels(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.world.playerVillageBuildingLevels().entrySet()) {
            joiner.add(entry.getKey() + ":" + entry.getValue());
        }
        return joiner.toString();
    }

    private String writeVillageProps(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (WorldProp prop : state.world.playerVillageProps()) {
            joiner.add(prop.x() + ":" + prop.y() + ":" + prop.asset() + ":" + prop.size());
        }
        return joiner.toString();
    }

    private String writeVillageInteriorProps(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.world.playerInteriorProps().entrySet()) {
            for (WorldProp prop : entry.getValue()) {
                joiner.add(entry.getKey() + ":" + prop.x() + ":" + prop.y() + ":" + prop.asset() + ":" + prop.size());
            }
        }
        return joiner.toString();
    }

    private String writeHarvestedQuestResources(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (String resource : state.harvestedQuestResources) {
            joiner.add(resource);
        }
        return joiner.toString();
    }

    private String writeNpcRelationships(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.npcRelationships.entrySet()) {
            if (entry.getValue() != 0) {
                joiner.add(escape(entry.getKey()) + ":" + entry.getValue());
            }
        }
        return joiner.toString();
    }

    private void readHarvestedQuestResources(String value, GameState state) {
        state.harvestedQuestResources.clear();
        if (value.isBlank()) {
            return;
        }
        for (String resource : value.split(",")) {
            if (!resource.isBlank()) {
                state.harvestedQuestResources.add(resource);
            }
        }
    }

    private void readNpcRelationships(String value, GameState state) {
        state.npcRelationships.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", 2);
            if (fields.length != 2) {
                continue;
            }
            try {
                int relationship = Math.max(-100, Math.min(100, Integer.parseInt(fields[1])));
                if (relationship != 0) {
                    state.npcRelationships.put(unescape(fields[0]), relationship);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readRecruits(String value, GameState state) {
        state.allies.clear();
        state.recruitedIds.clear();
        state.villageAllies.clear();
        state.villageWorkerRoles.clear();
        state.villageBuildingAssignments.clear();
        if (value.isBlank()) {
            return;
        }
        for (String recruitId : value.split(",")) {
            state.recruitAlly(recruitId);
        }
    }

    private void readVillageAllies(String value, GameState state) {
        state.villageAllies.clear();
        if (value.isBlank()) {
            return;
        }
        for (String allyName : value.split(",")) {
            String cleaned = allyName.strip();
            if (!cleaned.isBlank() && state.allies.stream().anyMatch(ally -> ally.name.equals(cleaned))) {
                state.villageAllies.add(cleaned);
            }
        }
    }

    private void readVillageWorkerRoles(String value, GameState state) {
        state.villageWorkerRoles.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", 2);
            if (fields.length != 2) {
                continue;
            }
            String name = unescape(fields[0]);
            if (state.villageAllies.contains(name)) {
                state.villageWorkerRoles.put(name, VillageManager.workerRole(fields[1]).id());
            }
        }
    }

    private void readVillageBuildingAssignments(String value, GameState state) {
        state.villageBuildingAssignments.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", 2);
            if (fields.length != 2) {
                continue;
            }
            String buildingKey = fields[0];
            String allyName = unescape(fields[1]);
            if (state.playerVillageBuildingByKey(buildingKey) != null
                    && state.villageAllies.contains(allyName)
                    && state.stationedAllies().stream().anyMatch(ally -> ally.name.equals(allyName))) {
                state.villageBuildingAssignments.put(buildingKey, allyName);
            }
        }
    }

    private void readVillageStorage(String value, GameState state) {
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":");
            if (fields.length != 2) {
                continue;
            }
            try {
                int amount = Integer.parseInt(fields[1]);
                if (amount > 0) {
                    state.villageStorage.put(fields[0], amount);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readVillageTiles(String value, GameState state) {
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":");
            if (fields.length != 3 || fields[2].isBlank()) {
                continue;
            }
            try {
                state.world.restorePlayerVillageTile(
                        Integer.parseInt(fields[0]),
                        Integer.parseInt(fields[1]),
                        fields[2].charAt(0)
                );
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readVillageBuildings(String value, GameState state) {
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":");
            if (fields.length != 7) {
                continue;
            }
            try {
                CityBuilding building = new CityBuilding(
                        fields[0],
                        Integer.parseInt(fields[1]),
                        Integer.parseInt(fields[2]),
                        Integer.parseInt(fields[3]),
                        Integer.parseInt(fields[4]),
                        fields[5],
                        Integer.parseInt(fields[6])
                );
                state.world.restorePlayerVillageBuilding(building);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readVillageBuildingLevels(String value, GameState state) {
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":");
            if (fields.length != 2) {
                continue;
            }
            try {
                state.world.setPlayerVillageBuildingLevel(fields[0], Integer.parseInt(fields[1]));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readVillageProps(String value, GameState state) {
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":");
            if (fields.length != 4) {
                continue;
            }
            try {
                WorldProp prop = new WorldProp(
                        Integer.parseInt(fields[0]),
                        Integer.parseInt(fields[1]),
                        fields[2],
                        Integer.parseInt(fields[3])
                );
                if (!isStarterCampProp(prop)) {
                    state.world.restorePlayerVillageProp(prop);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private boolean isStarterCampProp(WorldProp prop) {
        return (prop.x() == 15 && prop.y() == 13 && "location_camp_fire".equals(prop.asset()))
                || (prop.x() == 18 && prop.y() == 14 && "location_camp_crates".equals(prop.asset()))
                || (prop.x() == 11 && prop.y() == 15 && "deco_road_signpost".equals(prop.asset()))
                || (prop.x() == 22 && prop.y() == 17 && "village_prop_woodpile".equals(prop.asset()));
    }

    private void readVillageInteriorProps(String value, GameState state) {
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":");
            if (fields.length != 5) {
                continue;
            }
            try {
                state.world.restorePlayerInteriorProp(fields[0], new WorldProp(
                        Integer.parseInt(fields[1]),
                        Integer.parseInt(fields[2]),
                        fields[3],
                        Integer.parseInt(fields[4])
                ));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readQuests(String value, GameState state) {
        for (Quest quest : state.quests.values()) {
            quest.accepted = false;
            quest.completed = false;
            quest.progress = 0;
        }
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":");
            if (fields.length != 4) {
                continue;
            }
            Quest quest = state.quests.get(fields[0]);
            if (quest == null) {
                continue;
            }
            quest.accepted = Boolean.parseBoolean(fields[1]);
            quest.completed = Boolean.parseBoolean(fields[2]);
            try {
                quest.progress = Integer.parseInt(fields[3]);
            } catch (NumberFormatException ignored) {
                quest.progress = 0;
            }
        }
    }

    private void writeActor(Properties props, String prefix, Actor actor) {
        props.setProperty(prefix + "name", actor.name);
        props.setProperty(prefix + "sprite", actor.sprite);
        props.setProperty(prefix + "className", actor.className);
        props.setProperty(prefix + "hp", Integer.toString(actor.hp));
        props.setProperty(prefix + "mp", Integer.toString(actor.mp));
        props.setProperty(prefix + "maxHp", Integer.toString(actor.maxHp));
        props.setProperty(prefix + "maxMp", Integer.toString(actor.maxMp));
        props.setProperty(prefix + "attack", Integer.toString(actor.attack));
        props.setProperty(prefix + "defense", Integer.toString(actor.defense));
        props.setProperty(prefix + "level", Integer.toString(actor.level));
        props.setProperty(prefix + "xp", Integer.toString(actor.xp));
        props.setProperty(prefix + "gold", Integer.toString(actor.gold));
        props.setProperty(prefix + "inventory", writeInventory(actor));
        props.setProperty(prefix + "equipment", writeEquipment(actor));
        props.setProperty(prefix + "skillPoints", Integer.toString(actor.skillPoints));
        props.setProperty(prefix + "skillAllocations", writeSkillAllocations(actor));
        props.setProperty(prefix + "professionXp", writeProfessionXp(actor));
        props.setProperty(prefix + "abilityCount", Integer.toString(actor.abilities.size()));
        for (int i = 0; i < actor.abilities.size(); i++) {
            Ability ability = actor.abilities.get(i);
            String abilityPrefix = prefix + "ability." + i + ".";
            props.setProperty(abilityPrefix + "name", ability.name());
            props.setProperty(abilityPrefix + "power", Integer.toString(ability.power()));
            props.setProperty(abilityPrefix + "cost", Integer.toString(ability.cost()));
            props.setProperty(abilityPrefix + "kind", ability.kind().name());
            props.setProperty(abilityPrefix + "target", ability.target());
        }
    }

    private Actor readPlayer(Properties props) {
        Actor fallback = GameData.createPlayer(props.getProperty("className", "Knight"), props.getProperty("name", "Hero"));
        if (props.containsKey("player.name")) {
            return readActor(props, "player.", fallback);
        }
        fallback.maxHp = readInt(props, "maxHp", fallback.maxHp);
        fallback.maxMp = readInt(props, "maxMp", fallback.maxMp);
        fallback.attack = readInt(props, "attack", fallback.attack);
        fallback.defense = readInt(props, "defense", fallback.defense);
        fallback.level = readInt(props, "level", fallback.level);
        fallback.xp = readInt(props, "xp", fallback.xp);
        fallback.gold = readInt(props, "gold", fallback.gold);
        fallback.inventory.clear();
        readInventory(props.getProperty("inventory", ""), fallback);
        if (props.containsKey("equipment")) {
            fallback.equipment.clear();
            readEquipment(props.getProperty("equipment", ""), fallback);
        }
        fallback.skillPoints = readInt(props, "skillPoints", Math.max(0, fallback.level - 1));
        fallback.skillAllocations.clear();
        readSkillAllocations(props.getProperty("skillAllocations", ""), fallback);
        readProfessionXp(props.getProperty("professionXp", ""), fallback);
        fallback.hp = clamp(readInt(props, "hp", fallback.maxHp), 0, fallback.maxHp);
        fallback.mp = clamp(readInt(props, "mp", fallback.maxMp), 0, fallback.maxMp);
        return fallback;
    }

    private Actor readActor(Properties props, String prefix, Actor fallback) {
        String name = props.getProperty(prefix + "name", fallback.name);
        String sprite = props.getProperty(prefix + "sprite", fallback.sprite);
        String className = props.getProperty(prefix + "className", fallback.className);
        int maxHp = readInt(props, prefix + "maxHp", fallback.maxHp);
        int maxMp = readInt(props, prefix + "maxMp", fallback.maxMp);
        int attack = readInt(props, prefix + "attack", fallback.attack);
        int defense = readInt(props, prefix + "defense", fallback.defense);
        Actor actor = new Actor(name, sprite, className, Math.max(1, maxHp), Math.max(0, maxMp), Math.max(1, attack), Math.max(0, defense));
        actor.level = readInt(props, prefix + "level", fallback.level);
        actor.xp = readInt(props, prefix + "xp", fallback.xp);
        actor.gold = readInt(props, prefix + "gold", fallback.gold);
        actor.inventory.clear();
        readInventory(props.getProperty(prefix + "inventory", ""), actor);
        actor.equipment.clear();
        readEquipment(props.getProperty(prefix + "equipment", ""), actor);
        actor.skillPoints = readInt(props, prefix + "skillPoints", fallback.skillPoints);
        actor.skillAllocations.clear();
        readSkillAllocations(props.getProperty(prefix + "skillAllocations", ""), actor);
        readProfessionXp(props.getProperty(prefix + "professionXp", ""), actor);
        actor.abilities.clear();
        int abilityCount = readInt(props, prefix + "abilityCount", -1);
        if (abilityCount >= 0) {
            for (int i = 0; i < abilityCount; i++) {
                Ability ability = readAbility(props, prefix + "ability." + i + ".");
                if (ability != null) {
                    actor.abilities.add(ability);
                }
            }
        } else {
            actor.abilities.addAll(fallback.abilities);
        }
        actor.hp = clamp(readInt(props, prefix + "hp", actor.maxHp), 0, actor.maxHp);
        actor.mp = clamp(readInt(props, prefix + "mp", actor.maxMp), 0, actor.maxMp);
        return actor;
    }

    private Ability readAbility(Properties props, String prefix) {
        String name = props.getProperty(prefix + "name", "");
        if (name.isBlank()) {
            return null;
        }
        Ability.AbilityKind kind;
        try {
            kind = Ability.AbilityKind.valueOf(props.getProperty(prefix + "kind", Ability.AbilityKind.DAMAGE.name()));
        } catch (IllegalArgumentException ex) {
            kind = Ability.AbilityKind.DAMAGE;
        }
        return new Ability(
                name,
                readInt(props, prefix + "power", 8),
                readInt(props, prefix + "cost", 0),
                kind,
                props.getProperty(prefix + "target", "enemy")
        );
    }

    private void readParty(Properties props, GameState state) {
        state.allies.clear();
        state.recruitedIds.clear();
        state.villageAllies.clear();
        state.villageWorkerRoles.clear();
        String recruits = props.getProperty("recruits", "");
        if (!recruits.isBlank()) {
            for (String recruitId : recruits.split(",")) {
                if (!recruitId.isBlank() && GameData.RECRUITS.containsKey(recruitId) && !state.recruitedIds.contains(recruitId)) {
                    state.recruitedIds.add(recruitId);
                }
            }
        }
        int allyCount = readInt(props, "allyCount", -1);
        if (allyCount >= 0) {
            for (int i = 0; i < allyCount; i++) {
                String prefix = "ally." + i + ".";
                String recruitId = i < state.recruitedIds.size() ? state.recruitedIds.get(i) : "";
                Actor fallback = GameData.RECRUITS.containsKey(recruitId)
                        ? GameData.RECRUITS.get(recruitId).createActor()
                        : new Actor("Companion", "npc_marla", "Companion", 40, 12, 8, 2);
                state.allies.add(readActor(props, prefix, fallback));
            }
            return;
        }
        for (String recruitId : List.copyOf(state.recruitedIds)) {
            GameData.RecruitSpec spec = GameData.RECRUITS.get(recruitId);
            if (spec != null) {
                state.allies.add(spec.createActor());
            }
        }
    }

    private int readInt(Properties props, String key, int fallback) {
        try {
            return Integer.parseInt(props.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String escape(String value) {
        return value.replace("%", "%25").replace(":", "%3A").replace(",", "%2C");
    }

    private String unescape(String value) {
        return value.replace("%2C", ",").replace("%3A", ":").replace("%25", "%");
    }

    private record SavePosition(String mapId, int x, int y) {
    }

    private record QuestCounts(int active, int completed) {
    }
}
