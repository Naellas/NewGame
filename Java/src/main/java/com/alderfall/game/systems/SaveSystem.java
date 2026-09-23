package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import com.alderfall.game.map.WorldTransition;
import com.alderfall.game.inventory.Equipment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;

public final class SaveSystem {
    private static final DateTimeFormatter SAVE_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_SAVED_COMPANION_MEMORIES = 18;
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
        writeSave(state, saveId, characterId, displaySaveName, path);
    }

    public void overwrite(GameState state, String saveId, String saveName) throws IOException {
        Path path = savePath(saveId);
        if (!Files.exists(path)) {
            throw new IOException("Save file no longer exists.");
        }
        state.refreshPlayerVillageGrowth();
        String characterId = saveIdFor(state.player.name);
        String displaySaveName = saveName == null || saveName.isBlank() ? state.world.label(state.currentMapId) : saveName.strip();
        Files.createDirectories(path.getParent());
        writeSave(state, saveId, characterId, displaySaveName, path);
    }

    private void writeSave(GameState state, String saveId, String characterId, String displaySaveName, Path path) throws IOException {
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
        props.setProperty("strength", Integer.toString(state.player.strength));
        props.setProperty("intelligence", Integer.toString(state.player.intelligence));
        props.setProperty("dexterity", Integer.toString(state.player.dexterity));
        props.setProperty("charisma", Integer.toString(state.player.charisma));
        props.setProperty("constitution", Integer.toString(state.player.constitution));
        props.setProperty("willpower", Integer.toString(state.player.willpower));
        props.setProperty("level", Integer.toString(state.player.level));
        props.setProperty("xp", Integer.toString(state.player.xp));
        props.setProperty("gold", Integer.toString(state.player.gold));
        props.setProperty("inventory", writeInventory(state.player));
        state.chests.write(props);
        props.setProperty("equipment", writeEquipment(state.player));
        props.setProperty("skillPoints", Integer.toString(state.player.skillPoints));
        props.setProperty("statPoints", Integer.toString(state.player.statPoints));
        props.setProperty("professionSkillPoints", Integer.toString(state.player.professionSkillPoints));
        props.setProperty("skillAllocations", writeSkillAllocations(state.player));
        props.setProperty("professionXp", writeProfessionXp(state.player));
        props.setProperty("quests", writeQuests(state));
        props.setProperty("questBranchOutcomes", writeQuestBranchOutcomes(state));
        props.setProperty("harvestedQuestResources", writeHarvestedQuestResources(state));
        props.setProperty("harvestedResourceNodes", writeHarvestedResourceNodes(state));
        props.setProperty("unlockedRecipes", writeUnlockedRecipes(state));
        props.setProperty("npcRelationships", writeNpcRelationships(state));
        props.setProperty("companionMemoryJournal", writeCompanionMemoryJournal(state));
        props.setProperty("companionSoftRequests", writeCompanionSoftRequests(state));
        props.setProperty("companionDialogueTopicCounts", writeCompanionDialogueTopicCounts(state));
        props.setProperty("fulfilledCompanionSoftRequests", writeFulfilledCompanionSoftRequests(state));
        props.setProperty("companionRelationshipMilestones", writeCompanionRelationshipMilestones(state));
        props.setProperty("introducedNpcs", writeIntroducedNpcs(state));
        props.setProperty("npcKnowledge", writeNpcKnowledge(state));
        props.setProperty("monsterInsights", writeMonsterInsights(state));
        props.setProperty("monsterKnownVulnerabilities", writeMonsterKnownVulnerabilities(state));
        props.setProperty("settlementVisits", writeSettlementVisits(state));
        props.setProperty("buildingKnowledge", writeBuildingKnowledge(state));
        props.setProperty("worldAbilityTimers", writeWorldAbilityTimers(state));
        props.setProperty("recruits", writeRecruits(state));
        props.setProperty("villageAllies", writeVillageAllies(state));
        props.setProperty("romancedCompanions", writeCompanionIds(state.romancedCompanionIds));
        props.setProperty("marriedCompanions", writeCompanionIds(state.marriedCompanionIds));
        props.setProperty("villageWorkerRoles", writeVillageWorkerRoles(state));
        props.setProperty("villageBuildingAssignments", writeVillageBuildingAssignments(state));
        props.setProperty("villageStorage", writeVillageStorage(state));
        props.setProperty("playerVillageStage", Integer.toString(state.world.playerVillageStage()));
        props.setProperty("villageTiles", writeVillageTiles(state));
        props.setProperty("villageBuildings", writeVillageBuildings(state));
        props.setProperty("villageBuildingLevels", writeVillageBuildingLevels(state));
        props.setProperty("villageProps", writeVillageProps(state));
        props.setProperty("removedVillageProps", writeRemovedVillageProps(state));
        props.setProperty("villageInteriorProps", writeVillageInteriorProps(state));
        props.setProperty("villageInteriorTiles", writeVillageInteriorTiles(state));
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
        state.chests.read(props);
        state.activeChest = null;
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
        readRemovedVillageProps(props.getProperty("removedVillageProps", ""), state);
        readVillageProps(props.getProperty("villageProps", ""), state);
        readVillageInteriorTiles(props.getProperty("villageInteriorTiles", ""), state);
        readVillageInteriorProps(props.getProperty("villageInteriorProps", ""), state);
        state.ensureWeeklyNpcQuests();
        String questMigrationNote = readQuests(props.getProperty("quests", ""), state);
        readQuestBranchOutcomes(props.getProperty("questBranchOutcomes", ""), state);
        readHarvestedQuestResources(props.getProperty("harvestedQuestResources", ""), state);
        readHarvestedResourceNodes(props.getProperty("harvestedResourceNodes", ""), state);
        readUnlockedRecipes(props.getProperty("unlockedRecipes", ""), state);
        readNpcRelationships(props.getProperty("npcRelationships", ""), state);
        readCompanionMemoryJournal(props.getProperty("companionMemoryJournal", ""), state);
        readCompanionSoftRequests(props.getProperty("companionSoftRequests", ""), state);
        readCompanionDialogueTopicCounts(props.getProperty("companionDialogueTopicCounts", ""), state);
        readFulfilledCompanionSoftRequests(props.getProperty("fulfilledCompanionSoftRequests", ""), state);
        readCompanionRelationshipMilestones(props.getProperty("companionRelationshipMilestones", ""), state);
        readIntroducedNpcs(props.getProperty("introducedNpcs", ""), state);
        readNpcKnowledge(props.getProperty("npcKnowledge", ""), state);
        readMonsterInsights(props.getProperty("monsterInsights", ""), state);
        readMonsterKnownVulnerabilities(props.getProperty("monsterKnownVulnerabilities", ""), state);
        readSettlementVisits(props.getProperty("settlementVisits", ""), state);
        readBuildingKnowledge(props.getProperty("buildingKnowledge", ""), state);
        readWorldAbilityTimers(props.getProperty("worldAbilityTimers", ""), state);
        readParty(props, state);
        readVillageAllies(props.getProperty("villageAllies", ""), state);
        readCompanionIds(props.getProperty("romancedCompanions", ""), state.romancedCompanionIds);
        readCompanionIds(props.getProperty("marriedCompanions", ""), state.marriedCompanionIds);
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
        if (props.getProperty("settlementVisits", "").isBlank()) {
            state.recordSettlementVisit(state.currentMapId);
        }
        state.syncAllSkillAbilities();
        state.refreshPlayerVillageGrowth();
        state.resetNpcRuntime();
        state.resetDungeonMonsterRuntime();
        state.status = "Loaded save." + questMigrationNote;
        return true;
    }

    public boolean importCharacter(GameState state, String saveId) throws IOException {
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
        readParty(props, state);
        state.currentSaveId = saveIdFor(player.name);
        state.pendingPlayerName = player.name;
        state.pendingSaveName = "";
        state.currentMapId = WorldMap.PLAYER_VILLAGE_ID;
        state.playerX = 14;
        state.playerY = 17;
        state.setZoom(readInt(props, "zoom", state.zoom));
        state.worldTick = 0;
        state.battle = null;
        state.activeNpc = null;
        state.activeShop = null;
        state.activeVillageBuilding = null;
        state.dialogIndex = 0;
        state.partyScreenIndex = 0;
        state.storyPage = 0;
        state.mode = GameMode.STORY_INTRO;
        state.world.clearPlayerVillageCustomizations();
        state.world.setPlayerVillageStage(1);
        state.chests.clear();
        state.activeChest = null;
        state.villageStorage.clear();
        state.villageAllies.clear();
        state.romancedCompanionIds.clear();
        state.marriedCompanionIds.clear();
        state.villageWorkerRoles.clear();
        state.villageBuildingAssignments.clear();
        state.resetVillageInterface();
        state.ensureWeeklyNpcQuests();
        readQuests("", state);
        state.harvestedQuestResources.clear();
        readHarvestedResourceNodes("", state);
        state.npcRelationships.clear();
        state.companionMemoryJournal.clear();
        state.companionSoftRequests.clear();
        state.companionDialogueTopicCounts.clear();
        state.fulfilledCompanionSoftRequests.clear();
        state.companionRelationshipMilestones.clear();
        state.introducedNpcKeys.clear();
        state.npcKnowledge.clear();
        state.monsterInsights.clear();
        state.monsterKnownVulnerabilities.clear();
        state.settlementVisits.clear();
        state.buildingKnowledge.clear();
        state.recordSettlementVisit(state.currentMapId);
        state.syncAllSkillAbilities();
        state.refreshPlayerVillageGrowth();
        state.resetNpcRuntime();
        state.resetDungeonMonsterRuntime();
        state.status = "Imported " + player.name + " into a new adventure.";
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
            if (fields.length < 4) {
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
                Equipment equipment = GameData.equipment(pair[1]);
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
            joiner.add(quest.id + ":" + quest.accepted + ":" + quest.completed + ":" + quest.progress + ":" + quest.stageIndex
                    + ":" + escape(quest.activeStage().id()) + ":" + quest.contentRevision
                    + ":" + escape(String.join("|", quest.observedStages))
                    + ":" + escape(quest.cargo.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                            .collect(java.util.stream.Collectors.joining("|"))));
        }
        return joiner.toString();
    }

    private String writeQuestBranchOutcomes(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.questBranchOutcomes.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            joiner.add(escape(entry.getKey()) + ":" + escape(entry.getValue()));
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

    private String writeCompanionIds(Iterable<String> companionIds) {
        StringJoiner joiner = new StringJoiner(",");
        for (String companionId : companionIds) {
            if (companionId != null && GameData.RECRUITS.containsKey(companionId)) {
                joiner.add(companionId);
            }
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

    private String writeRemovedVillageProps(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (String propKey : state.world.removedPlayerVillageProps()) {
            joiner.add(propKey);
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

    private String writeVillageInteriorTiles(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.world.playerInteriorTiles().entrySet()) {
            for (var tile : entry.getValue().entrySet()) {
                joiner.add(entry.getKey() + ":" + tile.getKey().x() + ":" + tile.getKey().y() + ":" + tile.getValue());
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

    private String writeHarvestedResourceNodes(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (String resource : state.harvestedResourceNodes) {
            joiner.add(resource);
        }
        return joiner.toString();
    }

    private String writeUnlockedRecipes(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (String recipeKey : state.unlockedRecipeKeys) {
            if (CraftingSystem.recipeByKey(recipeKey) != null) {
                joiner.add(recipeKey);
            }
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

    private String writeCompanionMemoryJournal(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.companionMemoryJournal.entrySet()) {
            if (!GameData.RECRUITS.containsKey(entry.getKey())) {
                continue;
            }
            for (GameState.CompanionMemory memory : entry.getValue()) {
                if (memory.text().isBlank()) {
                    continue;
                }
                joiner.add(entry.getKey() + ":"
                        + memory.worldTick() + ":"
                        + escape(memory.category()) + ":"
                        + escape(memory.text()));
            }
        }
        return joiner.toString();
    }

    private String writeCompanionSoftRequests(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.companionSoftRequests.entrySet()) {
            if (GameData.RECRUITS.containsKey(entry.getKey()) && !entry.getValue().isBlank()) {
                joiner.add(entry.getKey() + ":" + escape(entry.getValue()));
            }
        }
        return joiner.toString();
    }

    private String writeCompanionDialogueTopicCounts(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.companionDialogueTopicCounts.entrySet()) {
            if (!entry.getKey().isBlank() && entry.getValue() > 0) {
                joiner.add(escape(entry.getKey()) + ":" + Math.min(999, entry.getValue()));
            }
        }
        return joiner.toString();
    }

    private String writeFulfilledCompanionSoftRequests(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (String requestKey : state.fulfilledCompanionSoftRequests) {
            if (!requestKey.isBlank()) {
                joiner.add(escape(requestKey));
            }
        }
        return joiner.toString();
    }

    private String writeCompanionRelationshipMilestones(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (String milestoneKey : state.companionRelationshipMilestones) {
            if (!milestoneKey.isBlank()) {
                joiner.add(escape(milestoneKey));
            }
        }
        return joiner.toString();
    }

    private String writeIntroducedNpcs(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (String key : state.introducedNpcKeys) {
            if (!key.isBlank()) {
                joiner.add(escape(key));
            }
        }
        return joiner.toString();
    }

    private String writeNpcKnowledge(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.npcKnowledge.entrySet()) {
            if (!entry.getKey().isBlank() && entry.getValue() > 0) {
                joiner.add(escape(entry.getKey()) + ":" + entry.getValue());
            }
        }
        return joiner.toString();
    }

    private String writeMonsterInsights(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.monsterInsights.entrySet()) {
            if (GameData.MONSTERS.containsKey(entry.getKey()) && entry.getValue() > 0) {
                joiner.add(entry.getKey() + ":" + entry.getValue());
            }
        }
        return joiner.toString();
    }

    private String writeSettlementVisits(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.settlementVisits.entrySet()) {
            if (state.world.hasMap(entry.getKey()) && entry.getValue() > 0) {
                joiner.add(escape(entry.getKey()) + ":" + entry.getValue());
            }
        }
        return joiner.toString();
    }

    private String writeBuildingKnowledge(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.buildingKnowledge.entrySet()) {
            if (!entry.getKey().isBlank() && entry.getValue() > 0) {
                joiner.add(escape(entry.getKey()) + ":" + entry.getValue());
            }
        }
        return joiner.toString();
    }

    private String writeMonsterKnownVulnerabilities(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (var entry : state.monsterKnownVulnerabilities.entrySet()) {
            if (!GameData.MONSTERS.containsKey(entry.getKey()) || entry.getValue().isEmpty()) {
                continue;
            }
            List<String> types = entry.getValue().stream()
                    .map(GameData::normalizeDamageType)
                    .filter(GameData.DAMAGE_TYPES::contains)
                    .sorted()
                    .toList();
            if (!types.isEmpty()) {
                joiner.add(entry.getKey() + ":" + String.join("|", types));
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
                for (Quest quest : state.quests.values()) {
                    if (quest.contentRevision != 0) continue;
                    for (int i = 0; i < quest.stages.size(); i++) {
                        String legacy = quest.id + "@stage" + i + ":";
                        if (resource.contains(legacy)) {
                            resource = resource.replace(legacy, quest.id + "@" + quest.stages.get(i).id() + ":");
                            break;
                        }
                    }
                }
                state.harvestedQuestResources.add(resource);
            }
        }
    }

    private void readHarvestedResourceNodes(String value, GameState state) {
        state.restoreHarvestedResourceNodes();
        state.harvestedResourceNodes.clear();
        if (!value.isBlank()) {
            for (String resource : value.split(",")) {
                if (!resource.isBlank()) {
                    state.harvestedResourceNodes.add(resource);
                }
            }
        }
        state.applyHarvestedResourceNodes();
    }

    private void readUnlockedRecipes(String value, GameState state) {
        state.unlockedRecipeKeys.clear();
        state.ensureStarterRecipeUnlocks();
        if (value.isBlank()) {
            return;
        }
        for (String recipeKey : value.split(",")) {
            String key = recipeKey.strip();
            if (CraftingSystem.recipeByKey(key) != null) {
                state.unlockedRecipeKeys.add(key);
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
                String key = unescape(fields[0]);
                int relationship = Math.max(GameState.MIN_NPC_RELATIONSHIP,
                        Math.min(GameState.maxRelationshipForKey(key), Integer.parseInt(fields[1])));
                if (relationship != 0) {
                    state.npcRelationships.put(key, relationship);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readCompanionMemoryJournal(String value, GameState state) {
        state.companionMemoryJournal.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", 4);
            if (fields.length != 4 || !GameData.RECRUITS.containsKey(fields[0])) {
                continue;
            }
            try {
                int tick = Math.max(0, Integer.parseInt(fields[1]));
                String category = unescape(fields[2]);
                String text = unescape(fields[3]);
                if (!text.isBlank()) {
                    List<GameState.CompanionMemory> memories = state.companionMemoryJournal
                            .computeIfAbsent(fields[0], ignored -> new ArrayList<>());
                    memories.add(new GameState.CompanionMemory(tick, category, text));
                    while (memories.size() > MAX_SAVED_COMPANION_MEMORIES) {
                        memories.remove(0);
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readCompanionSoftRequests(String value, GameState state) {
        state.companionSoftRequests.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", 2);
            if (fields.length != 2 || !GameData.RECRUITS.containsKey(fields[0])) {
                continue;
            }
            String requestId = unescape(fields[1]).strip();
            if (!requestId.isBlank()) {
                state.companionSoftRequests.put(fields[0], requestId);
            }
        }
    }

    private void readCompanionDialogueTopicCounts(String value, GameState state) {
        state.companionDialogueTopicCounts.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", 2);
            if (fields.length != 2) {
                continue;
            }
            String key = unescape(fields[0]).strip();
            if (key.isBlank()) {
                continue;
            }
            try {
                int count = Math.max(0, Math.min(999, Integer.parseInt(fields[1])));
                if (count > 0) {
                    state.companionDialogueTopicCounts.put(key, count);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readFulfilledCompanionSoftRequests(String value, GameState state) {
        state.fulfilledCompanionSoftRequests.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String requestKey = unescape(part).strip();
            if (!requestKey.isBlank()) {
                state.fulfilledCompanionSoftRequests.add(requestKey);
            }
        }
    }

    private void readCompanionRelationshipMilestones(String value, GameState state) {
        state.companionRelationshipMilestones.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String milestoneKey = unescape(part).strip();
            if (!milestoneKey.isBlank()) {
                state.companionRelationshipMilestones.add(milestoneKey);
            }
        }
    }

    private void readIntroducedNpcs(String value, GameState state) {
        state.introducedNpcKeys.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String key = unescape(part).strip();
            if (!key.isBlank()) {
                state.introducedNpcKeys.add(key);
            }
        }
    }

    private void readNpcKnowledge(String value, GameState state) {
        state.npcKnowledge.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", 2);
            if (fields.length != 2) {
                continue;
            }
            String key = unescape(fields[0]);
            if (key.isBlank()) {
                continue;
            }
            try {
                int count = Math.max(0, Math.min(8, Integer.parseInt(fields[1])));
                if (count > 0) {
                    state.npcKnowledge.put(key, count);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readMonsterInsights(String value, GameState state) {
        state.monsterInsights.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", 2);
            if (fields.length != 2 || !GameData.MONSTERS.containsKey(fields[0])) {
                continue;
            }
            try {
                int count = Math.max(0, Integer.parseInt(fields[1]));
                if (count > 0) {
                    state.monsterInsights.put(fields[0], count);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readSettlementVisits(String value, GameState state) {
        state.settlementVisits.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", 2);
            if (fields.length != 2) {
                continue;
            }
            String mapId = unescape(fields[0]);
            if (!state.world.hasMap(mapId)) {
                continue;
            }
            try {
                int count = Math.max(0, Integer.parseInt(fields[1]));
                if (count > 0) {
                    state.settlementVisits.put(mapId, count);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readBuildingKnowledge(String value, GameState state) {
        state.buildingKnowledge.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", 2);
            if (fields.length != 2) {
                continue;
            }
            String key = unescape(fields[0]);
            if (key.isBlank()) {
                continue;
            }
            try {
                int count = Math.max(0, Math.min(6, Integer.parseInt(fields[1])));
                if (count > 0) {
                    state.buildingKnowledge.put(key, count);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readMonsterKnownVulnerabilities(String value, GameState state) {
        state.monsterKnownVulnerabilities.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", 2);
            if (fields.length != 2 || !GameData.MONSTERS.containsKey(fields[0])) {
                continue;
            }
            for (String rawType : fields[1].split("\\|")) {
                state.recordMonsterVulnerability(fields[0], rawType);
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

    private void readCompanionIds(String value, java.util.Set<String> target) {
        target.clear();
        if (value.isBlank()) {
            return;
        }
        for (String companionId : value.split(",")) {
            String cleaned = companionId.strip();
            if (!cleaned.isBlank() && GameData.RECRUITS.containsKey(cleaned)) {
                target.add(cleaned);
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

    private void readRemovedVillageProps(String value, GameState state) {
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":");
            if (fields.length != 3) {
                continue;
            }
            try {
                WorldProp prop = new WorldProp(
                        Integer.parseInt(fields[0]),
                        Integer.parseInt(fields[1]),
                        fields[2],
                        48
                );
                if (!isStarterCampProp(prop)) {
                    state.world.restoreRemovedPlayerVillageProp(prop);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void readVillageInteriorTiles(String value, GameState state) {
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":");
            if (fields.length != 4) {
                continue;
            }
            try {
                char tile = fields[3].isBlank() ? 'i' : fields[3].charAt(0);
                state.world.restorePlayerInteriorTile(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), tile);
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

    private String readQuests(String value, GameState state) {
        state.partyDialogue.reset();
        List<String> revisedInvestigations = new ArrayList<>();
        for (Quest quest : state.quests.values()) {
            quest.accepted = false;
            quest.completed = false;
            quest.progress = 0;
            quest.stageIndex = 0;
            quest.observedStages.clear();
            quest.cargo.clear();
        }
        if (value.isBlank()) {
            return "";
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", -1);
            if (fields.length < 4) {
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
            if (fields.length >= 5) {
                try {
                    quest.stageIndex = Math.max(0, Math.min(Integer.parseInt(fields[4]), quest.stages.size() - 1));
                } catch (NumberFormatException ignored) {
                    quest.stageIndex = 0;
                }
            }
            int savedRevision = fields.length >= 7 ? parseInt(fields[6], 0) : 0;
            if (quest.completed && savedRevision != quest.contentRevision) {
                quest.stageIndex = quest.stages.size() - 1;
                quest.progress = quest.activeNeeded();
                continue;
            }
            if (!quest.completed && savedRevision != quest.contentRevision) {
                quest.stageIndex = 0;
                quest.progress = 0;
                if (quest.accepted) revisedInvestigations.add(quest.title);
                continue;
            }
            if (fields.length >= 6) {
                String stageId = unescape(fields[5]);
                for (int i = 0; i < quest.stages.size(); i++) {
                    if (quest.stages.get(i).id().equals(stageId)) {
                        quest.stageIndex = i;
                        break;
                    }
                }
            }
            quest.progress = Math.max(0, Math.min(quest.progress, quest.activeNeeded()));
            if (fields.length >= 9) {
                for (String cargo : unescape(fields[8]).split("\\|")) {
                    String[] entry = cargo.split("=", 2);
                    if (entry.length == 2 && parseInt(entry[1], 0) > 0)
                        quest.cargo.put(entry[0], parseInt(entry[1], 0));
                }
            }
            if (fields.length >= 8 && !fields[7].isBlank()) {
                for (String id : unescape(fields[7]).split("\\|")) {
                    if (quest.stages.stream().anyMatch(stage -> stage.id().equals(id))) quest.observedStages.add(id);
                }
            } else if (savedRevision == quest.contentRevision) {
                for (int i = 0; i < quest.stageIndex; i++) quest.observedStages.add(quest.stages.get(i).id());
                if (quest.ready() || quest.completed) quest.observedStages.add(quest.activeStage().id());
            }
        }
        return revisedInvestigations.isEmpty() ? "" : " Revised investigations need their new evidence checked: "
                + String.join(", ", revisedInvestigations) + ". Completed quests and rewards were preserved.";
    }

    private void readQuestBranchOutcomes(String value, GameState state) {
        state.questBranchOutcomes.clear();
        if (value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", 2);
            if (fields.length != 2) {
                continue;
            }
            String key = unescape(fields[0]).strip();
            String outcome = unescape(fields[1]).strip();
            if (!key.isBlank() && !outcome.isBlank()) {
                state.questBranchOutcomes.put(key, outcome);
            }
        }
    }

    private String writeWorldAbilityTimers(GameState state) {
        StringJoiner joiner = new StringJoiner(",");
        for (Map.Entry<String, Integer> entry : state.worldAbilityTimers.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                joiner.add(escape(entry.getKey()) + ":" + entry.getValue());
            }
        }
        return joiner.toString();
    }

    private void readWorldAbilityTimers(String value, GameState state) {
        state.worldAbilityTimers.clear();
        if (value == null || value.isBlank()) {
            return;
        }
        for (String part : value.split(",")) {
            String[] fields = part.split(":", 2);
            if (fields.length != 2) {
                continue;
            }
            int remaining = parseInt(fields[1], 0);
            if (remaining > 0) {
                state.worldAbilityTimers.put(unescape(fields[0]).strip(), remaining);
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
        props.setProperty(prefix + "strength", Integer.toString(actor.strength));
        props.setProperty(prefix + "intelligence", Integer.toString(actor.intelligence));
        props.setProperty(prefix + "dexterity", Integer.toString(actor.dexterity));
        props.setProperty(prefix + "charisma", Integer.toString(actor.charisma));
        props.setProperty(prefix + "constitution", Integer.toString(actor.constitution));
        props.setProperty(prefix + "willpower", Integer.toString(actor.willpower));
        props.setProperty(prefix + "level", Integer.toString(actor.level));
        props.setProperty(prefix + "xp", Integer.toString(actor.xp));
        props.setProperty(prefix + "gold", Integer.toString(actor.gold));
        props.setProperty(prefix + "inventory", writeInventory(actor));
        props.setProperty(prefix + "equipment", writeEquipment(actor));
        props.setProperty(prefix + "skillPoints", Integer.toString(actor.skillPoints));
        props.setProperty(prefix + "statPoints", Integer.toString(actor.statPoints));
        props.setProperty(prefix + "professionSkillPoints", Integer.toString(actor.professionSkillPoints));
        props.setProperty(prefix + "skillAllocations", writeSkillAllocations(actor));
        props.setProperty(prefix + "professionXp", writeProfessionXp(actor));
        props.setProperty(prefix + "activeAbilityLoadout", writeActiveAbilityLoadout(actor));
        props.setProperty(prefix + "abilityLoadoutPresets", writeAbilityLoadoutPresets(actor));
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

    private String writeActiveAbilityLoadout(Actor actor) {
        actor.sanitizeAbilityLoadout();
        StringJoiner joiner = new StringJoiner(",");
        for (String abilityName : actor.activeAbilityNames) {
            if (abilityName != null && !abilityName.isBlank()) {
                joiner.add(escape(abilityName));
            }
        }
        return joiner.toString();
    }

    private String writeAbilityLoadoutPresets(Actor actor) {
        actor.sanitizeAbilityLoadoutPresets();
        StringJoiner presets = new StringJoiner(";");
        for (Map.Entry<String, List<String>> entry : actor.abilityLoadoutPresets.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            StringJoiner names = new StringJoiner("|");
            for (String abilityName : entry.getValue()) {
                names.add(escape(abilityName));
            }
            presets.add(escape(entry.getKey()) + ":" + names);
        }
        return presets.toString();
    }

    private void readAbilityLoadoutPresets(String value, Actor actor) {
        actor.abilityLoadoutPresets.clear();
        if (value == null || value.isBlank()) {
            return;
        }
        for (String part : value.split(";")) {
            String[] fields = part.split(":", 2);
            if (fields.length != 2) {
                continue;
            }
            String preset = unescape(fields[0]).strip();
            List<String> names = new ArrayList<>();
            for (String name : fields[1].split("\\|")) {
                String abilityName = unescape(name).strip();
                if (!abilityName.isBlank() && !names.contains(abilityName)) {
                    names.add(abilityName);
                }
            }
            if (!preset.isBlank()) {
                actor.abilityLoadoutPresets.put(preset, names);
            }
        }
        actor.sanitizeAbilityLoadoutPresets();
    }

    private List<String> readActiveAbilityLoadout(String value) {
        List<String> loadout = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return loadout;
        }
        for (String part : value.split(",")) {
            String abilityName = unescape(part).strip();
            if (!abilityName.isBlank() && !loadout.contains(abilityName)
                    && loadout.size() < Actor.MAX_ACTIVE_ABILITIES) {
                loadout.add(abilityName);
            }
        }
        return loadout;
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
        fallback.strength = Math.max(1, readInt(props, "strength", fallback.strength));
        fallback.intelligence = Math.max(1, readInt(props, "intelligence", fallback.intelligence));
        fallback.dexterity = Math.max(1, readInt(props, "dexterity", fallback.dexterity));
        fallback.charisma = Math.max(1, readInt(props, "charisma", fallback.charisma));
        fallback.constitution = Math.max(1, readInt(props, "constitution", fallback.constitution));
        fallback.willpower = Math.max(1, readInt(props, "willpower", fallback.willpower));
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
        fallback.statPoints = readInt(props, "statPoints", 0);
        fallback.professionSkillPoints = readInt(props, "professionSkillPoints", fallback.professionSkillPoints);
        fallback.skillAllocations.clear();
        readSkillAllocations(props.getProperty("skillAllocations", ""), fallback);
        readProfessionXp(props.getProperty("professionXp", ""), fallback);
        fallback.setAbilityLoadout(readActiveAbilityLoadout(props.getProperty("activeAbilityLoadout", "")));
        readAbilityLoadoutPresets(props.getProperty("abilityLoadoutPresets", ""), fallback);
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
        int strength = readInt(props, prefix + "strength", fallback.strength);
        int intelligence = readInt(props, prefix + "intelligence", fallback.intelligence);
        int dexterity = readInt(props, prefix + "dexterity", fallback.dexterity);
        int charisma = readInt(props, prefix + "charisma", fallback.charisma);
        int constitution = readInt(props, prefix + "constitution", fallback.constitution);
        int willpower = readInt(props, prefix + "willpower", fallback.willpower);
        Actor actor = new Actor(name, sprite, className, Math.max(1, maxHp), Math.max(0, maxMp), Math.max(1, attack), Math.max(0, defense),
                Math.max(1, strength), Math.max(1, intelligence), Math.max(1, dexterity), Math.max(1, charisma),
                Math.max(1, constitution), Math.max(1, willpower));
        actor.level = readInt(props, prefix + "level", fallback.level);
        actor.xp = readInt(props, prefix + "xp", fallback.xp);
        actor.gold = readInt(props, prefix + "gold", fallback.gold);
        actor.inventory.clear();
        readInventory(props.getProperty(prefix + "inventory", ""), actor);
        actor.equipment.clear();
        readEquipment(props.getProperty(prefix + "equipment", ""), actor);
        actor.skillPoints = readInt(props, prefix + "skillPoints", fallback.skillPoints);
        actor.statPoints = readInt(props, prefix + "statPoints", fallback.statPoints);
        actor.professionSkillPoints = readInt(props, prefix + "professionSkillPoints", fallback.professionSkillPoints);
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
        actor.setAbilityLoadout(readActiveAbilityLoadout(props.getProperty(prefix + "activeAbilityLoadout", "")));
        readAbilityLoadoutPresets(props.getProperty(prefix + "abilityLoadoutPresets", ""), actor);
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
        state.romancedCompanionIds.clear();
        state.marriedCompanionIds.clear();
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

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
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
