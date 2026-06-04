package com.alderfall.game;

import java.nio.file.Path;

public final class GameDiagnostics {
    private GameDiagnostics() {
    }

    public static void main(String[] args) {
        Path javaRoot = Path.of("").toAbsolutePath().normalize();
        Holder<GameConfig> config = new Holder<>();
        long configNanos = DebugMetrics.timeNanos(() -> config.value = GameConfig.load(javaRoot));

        Holder<GameState> state = new Holder<>();
        long stateNanos = DebugMetrics.timeNanos(() -> state.value = new GameState(config.value));

        long newGameNanos = DebugMetrics.timeNanos(() -> {
            state.value.chooseClass("Mage");
            while (state.value.mode == GameMode.STORY_INTRO) {
                state.value.advanceStoryIntro();
            }
        });

        AssetStore assets = new AssetStore(javaRoot.resolve("assets"));
        long assetWarmupNanos = DebugMetrics.timeNanos(() -> {
            assets.image("grass", 48, 48);
            assets.spriteFit("player_model", 48, 64);
            assets.animatedSpriteFrameCount("player_model", "down_walk", 48, 64);
        });

        System.out.println("Diagnostics");
        System.out.println("configLoad=" + DebugMetrics.millis(configNanos));
        System.out.println("stateCreate=" + DebugMetrics.millis(stateNanos));
        System.out.println("newGameSetup=" + DebugMetrics.millis(newGameNanos));
        System.out.println("assetWarmup=" + DebugMetrics.millis(assetWarmupNanos));
        System.out.println("maps=" + state.value.world.mapCount());
        System.out.println("props=" + state.value.world.totalPropCount());
        System.out.println("assetCache=" + assets.cacheSummary());
    }

    private static final class Holder<T> {
        private T value;
    }
}
