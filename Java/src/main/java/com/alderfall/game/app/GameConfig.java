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
    public double monsterLevelScaling = 0.60;
    public int masterVolume = 80;
    public int musicVolume = 70;
    public int sfxVolume = 75;
    public int footstepVolume = 55;
    public double combatAnimationSpeed = 1.0;
    public double walkAnimationSpeed = 1.0;
    public int shopRestockDays = 3;

    public static int clampShopRestockDays(int days) { return Math.max(1, Math.min(30, days)); }

    public static double clampWalkAnimationSpeed(double speed) {
        return Double.isFinite(speed) ? Math.max(0.5, Math.min(2.0, speed)) : 1.0;
    }

    public static double clampCombatAnimationSpeed(double speed) {
        return Double.isFinite(speed) ? Math.max(0.5, Math.min(3.0, speed)) : 1.0;
    }
    public boolean creativeBuildMode = true;
    public boolean creativeCraftingMode = false;
    public boolean showMapEditorButton = true;
    public String cameraMode = "smooth";
    public int cameraSmoothing = 68;
    public boolean cameraLookAhead = true;
    public String movementSpeed = "normal";
    public String renderQuality = "high";
    public String weatherQuality = "high";

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
        GameConfig config = new GameConfig(
                readChance(text, "wild", 0.12),
                readChance(text, "dungeon", 0.18),
                readChance(text, "dungeon_entrance", 0.40),
                readChance(text, "two_monsters", 0.30),
                readChance(text, "three_monsters", 0.10)
        );
        Matcher restock = Pattern.compile("\"shop_restock_days\"\\s*:\\s*(-?[0-9]+)").matcher(text);
        if (restock.find()) {
            try { config.shopRestockDays = clampShopRestockDays(Integer.parseInt(restock.group(1))); }
            catch (NumberFormatException ignored) { }
        }
        return config;
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
            config.monsterLevelScaling = readDouble(props, "monsterLevelScaling", config.monsterLevelScaling);
            config.masterVolume = readInt(props, "masterVolume", config.masterVolume);
            config.musicVolume = readInt(props, "musicVolume", config.musicVolume);
            config.sfxVolume = readInt(props, "sfxVolume", config.sfxVolume);
            config.footstepVolume = readInt(props, "footstepVolume", config.footstepVolume);
            config.shopRestockDays = clampShopRestockDays(readInt(props, "shopRestockDays", config.shopRestockDays));
            try {
                config.walkAnimationSpeed = clampWalkAnimationSpeed(Double.parseDouble(props.getProperty("walkAnimationSpeed", "1.0")));
            } catch (NumberFormatException ignored) {
                config.walkAnimationSpeed = 1.0;
            }
            try {
                config.combatAnimationSpeed = clampCombatAnimationSpeed(Double.parseDouble(props.getProperty("combatAnimationSpeed", "1.0")));
            } catch (NumberFormatException ignored) {
                config.combatAnimationSpeed = 1.0;
            }
            config.creativeCraftingMode = readBoolean(props, "creativeCraftingMode", false);
            config.creativeBuildMode = readBoolean(props, "creativeBuildMode", config.creativeBuildMode);
            config.showMapEditorButton = readBoolean(props, "showMapEditorButton", config.showMapEditorButton);
            config.cameraMode = readChoice(props, "cameraMode", config.cameraMode, "locked", "smooth", "look_ahead", "dead_zone");
            config.cameraSmoothing = readInt(props, "cameraSmoothing", config.cameraSmoothing);
            config.cameraLookAhead = readBoolean(props, "cameraLookAhead", config.cameraLookAhead);
            config.movementSpeed = readChoice(props, "movementSpeed", config.movementSpeed, "relaxed", "normal", "quick");
            config.renderQuality = readChoice(props, "renderQuality", config.renderQuality, "high", "balanced", "performance", "low_spec");
            config.weatherQuality = readChoice(props, "weatherQuality", config.weatherQuality, "high", "balanced", "performance", "low_spec");
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
        props.setProperty("monsterLevelScaling", Double.toString(monsterLevelScaling));
        props.setProperty("masterVolume", Integer.toString(masterVolume));
        props.setProperty("musicVolume", Integer.toString(musicVolume));
        props.setProperty("sfxVolume", Integer.toString(sfxVolume));
        props.setProperty("footstepVolume", Integer.toString(footstepVolume));
        props.setProperty("shopRestockDays", Integer.toString(clampShopRestockDays(shopRestockDays)));
        props.setProperty("combatAnimationSpeed", Double.toString(clampCombatAnimationSpeed(combatAnimationSpeed)));
        props.setProperty("walkAnimationSpeed", Double.toString(clampWalkAnimationSpeed(walkAnimationSpeed)));
        props.setProperty("creativeCraftingMode", Boolean.toString(creativeCraftingMode));
        props.setProperty("creativeBuildMode", Boolean.toString(creativeBuildMode));
        props.setProperty("showMapEditorButton", Boolean.toString(showMapEditorButton));
        props.setProperty("cameraMode", cameraMode);
        props.setProperty("cameraSmoothing", Integer.toString(cameraSmoothing));
        props.setProperty("cameraLookAhead", Boolean.toString(cameraLookAhead));
        props.setProperty("movementSpeed", movementSpeed);
        props.setProperty("renderQuality", renderQuality);
        props.setProperty("weatherQuality", weatherQuality);
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

    private static boolean readBoolean(Properties props, String key, boolean fallback) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(value);
    }

    private static String readChoice(Properties props, String key, String fallback, String... allowed) {
        String value = props.getProperty(key, fallback);
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase();
        for (String choice : allowed) {
            if (choice.equals(normalized)) {
                return normalized;
            }
        }
        return fallback;
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
            case "footsteps" -> footstepVolume = clampPercent(footstepVolume + delta);
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
