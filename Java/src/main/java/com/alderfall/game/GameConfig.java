package com.alderfall.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameConfig {
    public static final int WIDTH = 1440;
    public static final int HEIGHT = 900;
    public static final int TILE = 48;
    public static final int MAP_COLS = 24;
    public static final int MAP_ROWS = 17;
    public static final int SIDEBAR_WIDTH = WIDTH - MAP_COLS * TILE;
    public static final int FPS_MS = 50;

    public final double wildEncounterChance;
    public final double dungeonEncounterChance;
    public final double dungeonEntranceEncounterChance;
    public final double twoMonsterChance;
    public final double threeMonsterChance;

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

    public static GameConfig load(Path pythonRoot) {
        Path configPath = pythonRoot.resolve("config").resolve("gameplay.json");
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
}
