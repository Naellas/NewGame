package com.alderfall.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.StringJoiner;

public final class SaveSystem {
    private final Path savePath;

    public SaveSystem(Path javaRoot) {
        this.savePath = javaRoot.resolve("saves").resolve("save.properties");
    }

    public void save(GameState state) throws IOException {
        Files.createDirectories(savePath.getParent());
        Properties props = new Properties();
        props.setProperty("className", state.player.className);
        props.setProperty("mapId", state.currentMapId);
        props.setProperty("zoom", Integer.toString(state.zoom));
        props.setProperty("x", Integer.toString(state.playerX));
        props.setProperty("y", Integer.toString(state.playerY));
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
        try (var out = Files.newOutputStream(savePath)) {
            props.store(out, "Echoes of Alderfall Java save");
        }
    }

    public boolean load(GameState state) throws IOException {
        if (!Files.exists(savePath)) {
            return false;
        }
        Properties props = new Properties();
        try (var in = Files.newInputStream(savePath)) {
            props.load(in);
        }
        Actor player = GameData.createPlayer(props.getProperty("className", "Knight"));
        player.maxHp = readInt(props, "maxHp", player.maxHp);
        player.maxMp = readInt(props, "maxMp", player.maxMp);
        player.attack = readInt(props, "attack", player.attack);
        player.defense = readInt(props, "defense", player.defense);
        player.level = readInt(props, "level", player.level);
        player.xp = readInt(props, "xp", player.xp);
        player.gold = readInt(props, "gold", player.gold);
        player.inventory.clear();
        readInventory(props.getProperty("inventory", ""), player);
        if (props.containsKey("equipment")) {
            player.equipment.clear();
            readEquipment(props.getProperty("equipment", ""), player);
        }
        player.skillPoints = readInt(props, "skillPoints", Math.max(0, player.level - 1));
        player.skillAllocations.clear();
        readSkillAllocations(props.getProperty("skillAllocations", ""), player);
        player.hp = Math.min(player.maxHp, readInt(props, "hp", player.maxHp));
        player.mp = Math.min(player.maxMp, readInt(props, "mp", player.maxMp));
        state.player = player;
        state.syncSkillAbilities();
        state.currentMapId = props.getProperty("mapId", WorldMap.OVERWORLD_ID);
        state.setZoom(readInt(props, "zoom", state.zoom));
        if (!state.world.hasMap(state.currentMapId)) {
            state.currentMapId = WorldMap.OVERWORLD_ID;
        }
        state.playerX = readInt(props, "x", WorldMap.START_POSITION.x());
        state.playerY = readInt(props, "y", WorldMap.START_POSITION.y());
        state.battle = null;
        state.mode = GameMode.EXPLORE;
        readQuests(props.getProperty("quests", ""), state);
        readRecruits(props.getProperty("recruits", ""), state);
        state.status = "Loaded save.";
        return true;
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
            String[] pair = part.split(":");
            if (pair.length != 2) {
                continue;
            }
            try {
                player.addItem(pair[0], Integer.parseInt(pair[1]));
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
            String[] pair = part.split(":");
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
            String[] pair = part.split(":");
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

    private int readInt(Properties props, String key, int fallback) {
        try {
            return Integer.parseInt(props.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
