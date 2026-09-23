package com.alderfall.game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Exercises real battle classification and encounter-scoped score selection without an audio device. */
public final class CombatMusicTest {
    public static void main(String[] args) {
        Path root = Path.of("").toAbsolutePath();
        GameState state = new GameState(GameConfig.load(root));
        GameAudioController audio = new GameAudioController(state,
                new MusicManager(root.resolve("assets/music")), new SoundManager(root.resolve("assets/sounds")));
        checkPool(state, audio, root, "slime", false, 4, "regular");
        checkPool(state, audio, root, "slime", true, 3, "elite");
        checkPool(state, audio, root, "goblin_king", false, 4, "boss");
        // These bosses were absent from the old hard-coded music list.
        for (String key : List.of("red_dragon", "demon_queen", "vaelthara")) {
            state.battle = battle(key, false, 1);
            require(audio.battleMusicTrack().startsWith("battle_boss"), "Missed story boss: " + key);
        }
        state.battle = battle("slime", false, 3);
        require(state.battle.enemies.stream().noneMatch(state.battle::isBossEnemy), "Test formation unexpectedly has a boss");
        String mobTrack = audio.battleMusicTrack();
        require(!mobTrack.startsWith("battle_boss") && !mobTrack.startsWith("battle_elite"),
                "An ordinary group received special encounter music");
        audio.shutdown();
        System.out.println("CombatMusicTest passed: 11 tracks, real elite/boss flags, no repeats, stable cues and ordinary groups.");
    }

    private static void checkPool(GameState state, GameAudioController audio, Path root,
                                  String monster, boolean elite, int count, String tier) {
        Set<String> tracks = new HashSet<>();
        String previous = "";
        for (int i = 0; i < count * 2; i++) {
            state.battle = battle(monster, elite, 1);
            String track = audio.battleMusicTrack();
            require(!track.equals(previous), "Repeated " + tier + " cue between encounters");
            require(tier.equals("regular") ? !track.startsWith("battle_elite") && !track.startsWith("battle_boss")
                    : track.startsWith("battle_" + tier), "Wrong tier: " + track);
            require(Files.isRegularFile(root.resolve("assets/music/" + track + ".wav")), "Missing " + track);
            for (int frame = 0; frame < 120; frame++) require(track.equals(audio.battleMusicTrack()), "Cue restarted");
            state.battle.enemy.hp = 0;
            require(track.equals(audio.battleMusicTrack()), "Cue downgraded after enemy death");
            tracks.add(track);
            previous = track;
        }
        require(tracks.size() == count, "Not all " + tier + " tracks reachable: " + tracks);
    }

    private static Battle battle(String key, boolean elite, int size) {
        GameData.MonsterSpec spec = GameData.MONSTERS.get(key);
        require(spec != null, "Unknown test monster: " + key);
        Random random = new Random(42) {
            @Override public double nextDouble() { return elite ? 0.0 : 0.99; }
        };
        return new Battle(GameData.createPlayer("Knight"), List.of(),
                java.util.Collections.nCopies(size, spec), random, 'g', "overworld");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
