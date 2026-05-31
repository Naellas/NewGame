package com.alderfall.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameConfig {
    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    public static final int TILE = 48;
    public static final int MAP_COLS = 32;
    public static final int MAP_ROWS = 22;
    public static final int SIDEBAR_WIDTH = WIDTH - MAP_COLS * TILE;
    public static final int FPS_MS = 50;

    public double wildEncounterChance;
    public double dungeonEncounterChance;
    public double dungeonEntranceEncounterChance;
    public double twoMonsterChance;
    public double threeMonsterChance;
    public int masterVolume = 80;
    public int musicVolume = 70;
    public int sfxVolume = 75;

    private GameConfig(
            double wildEncounterChance,
            double dungeonEncounterChance,
            double dungeonEntranceEncounterChance,
            double twoMonsterChance,
            double threeMonsterChance
    ) {
        this.wildEncounterChance = wildEncounterChance;
        this.dungeonEncounterChance = dungeonEncounterChance;
        this.dungeonEntranceEncounterChance = dungeonEntranceEncounterChance;
        this.twoMonsterChance = twoMonsterChance;
        this.threeMonsterChance = threeMonsterChance;
    }

    public static GameConfig load(Path javaRoot) {
        Path configPath = javaRoot.resolve("config").resolve("gameplay.json");
        String text = "";
        try {
            if (Files.exists(configPath)) {
                text = Files.readString(configPath);
            }
        } catch (IOException ignored) {
            text = "";
        }
        return new GameConfig(
                readChance(text, "wild", 0.12),
                readChance(text, "dungeon", 0.18),
                readChance(text, "dungeon_entrance", 0.40),
                readChance(text, "two_monsters", 0.30),
                readChance(text, "three_monsters", 0.10)
        );
    }

    public static GameConfig loadWithSettings(Path javaRoot) {
        GameConfig config = load(javaRoot);
        Path settingsPath = javaRoot.resolve("config").resolve("settings.properties");
        if (!Files.exists(settingsPath)) {
            return config;
        }
        Properties props = new Properties();
        try (var in = Files.newInputStream(settingsPath)) {
            props.load(in);
            config.wildEncounterChance = readDouble(props, "wildEncounterChance", config.wildEncounterChance);
            config.dungeonEncounterChance = readDouble(props, "dungeonEncounterChance", config.dungeonEncounterChance);
            config.dungeonEntranceEncounterChance = readDouble(props, "dungeonEntranceEncounterChance", config.dungeonEntranceEncounterChance);
            config.twoMonsterChance = readDouble(props, "twoMonsterChance", config.twoMonsterChance);
            config.threeMonsterChance = readDouble(props, "threeMonsterChance", config.threeMonsterChance);
            config.masterVolume = readInt(props, "masterVolume", config.masterVolume);
            config.musicVolume = readInt(props, "musicVolume", config.musicVolume);
            config.sfxVolume = readInt(props, "sfxVolume", config.sfxVolume);
        } catch (IOException ignored) {
        }
        return config;
    }

    public void save(Path javaRoot) throws IOException {
        Path settingsPath = javaRoot.resolve("config").resolve("settings.properties");
        Files.createDirectories(settingsPath.getParent());
        Properties props = new Properties();
        props.setProperty("wildEncounterChance", Double.toString(wildEncounterChance));
        props.setProperty("dungeonEncounterChance", Double.toString(dungeonEncounterChance));
        props.setProperty("dungeonEntranceEncounterChance", Double.toString(dungeonEntranceEncounterChance));
        props.setProperty("twoMonsterChance", Double.toString(twoMonsterChance));
        props.setProperty("threeMonsterChance", Double.toString(threeMonsterChance));
        props.setProperty("masterVolume", Integer.toString(masterVolume));
        props.setProperty("musicVolume", Integer.toString(musicVolume));
        props.setProperty("sfxVolume", Integer.toString(sfxVolume));
        try (var out = Files.newOutputStream(settingsPath)) {
            props.store(out, "Echoes of Alderfall Java settings");
        }
    }

    private static double readChance(String json, String key, double fallback) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        try {
            double value = Double.parseDouble(matcher.group(1));
            return Math.max(0.0, Math.min(1.0, value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static double readDouble(Properties props, String key, double fallback) {
        try {
            return Math.max(0.0, Math.min(1.0, Double.parseDouble(props.getProperty(key, Double.toString(fallback)))));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static int readInt(Properties props, String key, int fallback) {
        try {
            return Math.max(0, Math.min(100, Integer.parseInt(props.getProperty(key, Integer.toString(fallback)))));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public void adjustChance(String key, double delta) {
        switch (key) {
            case "wild" -> wildEncounterChance = clampChance(wildEncounterChance + delta);
            case "dungeon" -> dungeonEncounterChance = clampChance(dungeonEncounterChance + delta);
            case "dungeon_entrance" -> dungeonEntranceEncounterChance = clampChance(dungeonEntranceEncounterChance + delta);
            case "two_monsters" -> twoMonsterChance = clampChance(twoMonsterChance + delta);
            case "three_monsters" -> threeMonsterChance = clampChance(threeMonsterChance + delta);
            default -> {
            }
        }
    }

    public void adjustVolume(String key, int delta) {
        switch (key) {
            case "master" -> masterVolume = clampPercent(masterVolume + delta);
            case "music" -> musicVolume = clampPercent(musicVolume + delta);
            case "sfx" -> sfxVolume = clampPercent(sfxVolume + delta);
            default -> {
            }
        }
    }

    private double clampChance(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
