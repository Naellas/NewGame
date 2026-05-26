package com.alderfall.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;

public final class SaveSystem {
    private final Path savesDir;
    private final Path legacySavePath;

    public record SaveSummary(String saveId, String name, String className, int level, String mapId) {
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
        try (var stream = Files.list(savesDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".properties"))
                    .sorted(Comparator.comparing(this::lastModified).reversed())
                    .forEach(path -> readSummary(path, summaries));
        } catch (IOException ignored) {
        }
        return summaries;
    }

    public static String saveIdFor(String name) {
        String base = name == null ? "" : name.strip().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        base = base.replaceAll("^-+|-+$", "");
        return base.isBlank() ? "adventure" : base.substring(0, Math.min(36, base.length()));
    }

    public void save(GameState state) throws IOException {
        if (state.currentSaveId == null || state.currentSaveId.isBlank()) {
            state.currentSaveId = saveIdFor(state.player.name);
        }
        save(state, state.currentSaveId);
    }

    public void save(GameState state, String saveId) throws IOException {
        Files.createDirectories(savesDir);
        Properties props = new Properties();
        SavePosition position = normalizedSavePosition(state);
        props.setProperty("saveId", saveIdFor(saveId));
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
        props.setProperty("quests", writeQuests(state));
        props.setProperty("recruits", writeRecruits(state));
        writeActor(props, "player.", state.player);
        props.setProperty("allyCount", Integer.toString(state.allies.size()));
        for (int i = 0; i < state.allies.size(); i++) {
            writeActor(props, "ally." + i + ".", state.allies.get(i));
        }
        try (var out = Files.newOutputStream(savePath(saveId))) {
            props.store(out, "Echoes of Alderfall Java save");
        }
        state.currentSaveId = saveIdFor(saveId);
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
        state.currentSaveId = props.getProperty("saveId", saveIdFor(saveId));
        state.pendingPlayerName = player.name;
        String mapId = props.getProperty("mapId", WorldMap.OVERWORLD_ID);
        state.setZoom(readInt(props, "zoom", state.zoom));
        state.worldTick = readInt(props, "worldTick", 0);
        int x = readInt(props, "x", WorldMap.START_POSITION.x());
        int y = readInt(props, "y", WorldMap.START_POSITION.y());
        if (!state.world.hasMap(mapId)) {
            mapId = WorldMap.OVERWORLD_ID;
            x = WorldMap.START_POSITION.x();
            y = WorldMap.START_POSITION.y();
        }
        TilePoint safePosition = safePosition(state.world, mapId, x, y);
        state.currentMapId = mapId;
        state.playerX = safePosition.x();
        state.playerY = safePosition.y();
        state.battle = null;
        state.activeNpc = null;
        state.activeShop = null;
        state.dialogIndex = 0;
        state.mode = GameMode.EXPLORE;
        readQuests(props.getProperty("quests", ""), state);
        readParty(props, state);
        state.resetNpcRuntime();
        state.status = "Loaded save.";
        return true;
    }

    private SavePosition normalizedSavePosition(GameState state) {
        String mapId = state.currentMapId;
        int x = state.playerX;
        int y = state.playerY;
        if ("interior".equals(state.world.kind(mapId))) {
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
        return savesDir.resolve(saveIdFor(saveId) + ".properties");
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
            String fileName = path.getFileName().toString();
            String fallbackId = fileName.substring(0, fileName.length() - ".properties".length());
            summaries.add(new SaveSummary(
                    props.getProperty("saveId", fallbackId),
                    props.getProperty("name", "Hero"),
                    props.getProperty("className", "Knight"),
                    readInt(props, "level", 1),
                    props.getProperty("mapId", WorldMap.OVERWORLD_ID)
            ));
        } catch (IOException ignored) {
        }
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
            if (pair.length == 2 && GameData.EQUIPMENT.containsKey(pair[1])) {
                player.equipment.put(pair[0], pair[1]);
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

    private void readRecruits(String value, GameState state) {
        state.allies.clear();
        state.recruitedIds.clear();
        if (value.isBlank()) {
            return;
        }
        for (String recruitId : value.split(",")) {
            state.recruitAlly(recruitId);
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

    private record SavePosition(String mapId, int x, int y) {
    }
}
