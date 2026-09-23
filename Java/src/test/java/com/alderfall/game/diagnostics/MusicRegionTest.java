package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.Files;
import java.nio.file.Path;

/** Checks cue selection without requiring a physical audio device. */
public final class MusicRegionTest {
    public static void main(String[] args) {
        Path root = Path.of("").toAbsolutePath();
        GameState state = new GameState(GameConfig.load(root));
        GameAudioController audio = new GameAudioController(state,
                new MusicManager(root.resolve("assets/music")), new SoundManager(root.resolve("assets/sounds")));
        int checked = 0;
        for (WorldMap.Kingdom kingdom : state.world.kingdoms()) {
            state.currentMapId = WorldMap.OVERWORLD_ID;
            state.playerX = kingdom.centerX();
            state.playerY = kingdom.centerY();
            assertCue(audio, root, "region_" + kingdom.id());
            checked++;
        }
        for (WorldMap.SettlementSite site : state.world.settlementSites()) {
            state.currentMapId = site.id();
            state.playerX = 10;
            state.playerY = 10;
            TilePoint entrance = state.world.overworldEntranceFor(site.id());
            if (entrance == null) throw new AssertionError("Missing settlement entrance: " + site.id());
            assertCue(audio, root, "town_" + state.world.kingdomAt(entrance.x(), entrance.y()).id());
            checked++;
        }
        // Verify known dungeon maps through the actual world catalog.
        for (WorldMap.AdventureMarker marker : state.world.adventureMarkers()) {
            String mapId = marker.mapId();
            if (!"dungeon".equals(state.world.kind(mapId))) continue;
            var context = state.world.dungeonContext(mapId);
            String family = switch (context.theme()) {
                case "cave" -> "cave";
                case "abandoned_castle" -> "castle";
                case "prison" -> "prison";
                case "bandit_camp" -> "bandit";
                default -> "crypt";
            };
            for (int floor = 1; floor <= context.floors(); floor++) {
                state.currentMapId = mapId.replaceFirst("_\\d+$", "_" + floor);
                assertCue(audio, root, "dungeon_" + family
                        + (floor >= Math.max(2, context.floors() - 1) ? "_depths" : ""));
                checked++;
            }
        }
        for (WorldMap.SettlementSite site : state.world.settlementSites()) {
            if (state.world.cityBuildings(site.id()).isEmpty()) continue;
            CityBuilding building = state.world.cityBuildings(site.id()).get(0);
            String interior = state.world.ensureHouseInterior(site.id(), building.anchor().x(),
                    building.anchor().y(), building.anchor().x(), building.y2() + 1);
            if (interior == null) throw new AssertionError("Missing interior for " + site.id());
            state.currentMapId = interior;
            TilePoint entrance = state.world.overworldEntranceFor(site.id());
            assertCue(audio, root, "town_" + state.world.kingdomAt(entrance.x(), entrance.y()).id());
            checked++;
        }
        audio.shutdown();
        System.out.println("MusicRegionTest passed: " + checked + " regional, settlement and dungeon cues.");
    }

    private static void assertCue(GameAudioController audio, Path root, String expected) {
        String actual = audio.worldMusicKeyForCurrentPosition();
        if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
        if (!Files.isRegularFile(root.resolve("assets/music/" + actual + ".wav"))) {
            throw new AssertionError("Missing track: " + actual);
        }
    }
}
