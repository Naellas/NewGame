package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

/** Exercises actual encounter entry points, including paths and low-level camp populations. */
public final class LocationEncounterTest {
    public static void main(String[] args) throws Exception {
        GameConfig config = GameConfig.load(Path.of(""));
        config.wildEncounterChance = 1;
        config.dungeonEntranceEncounterChance = 1;
        GameState state = new GameState(config);
        state.currentMapId = WorldMap.OVERWORLD_ID;
        Method group = GameState.class.getDeclaredMethod("chooseMonsterGroup", char.class, String.class);
        group.setAccessible(true);
        int checked = 0;
        int paths = 0;
        for (String kind : List.of("goblin_camp", "bandit_camp", "graveyard", "crypt",
                "cave", "cave_mouth", "abandoned_castle", "prison", "sewer")) {
            List<String> pool = GameState.locationMonsterPool(kind);
            require(pool.stream().allMatch(GameData.MONSTERS::containsKey), "Unknown resident in " + kind);
            for (int level : new int[]{1, 4, 7, 12}) {
                state.player.level = level;
                for (int roll = 0; roll < 100; roll++) {
                    for (Object key : (List<?>) group.invoke(state, 'f', kind)) {
                        require(pool.contains(key), "Foreign monster in " + kind + ": " + key);
                    }
                }
            }
            state.player.level = 1;
            for (var site : state.world.locationSites(kind)) {
                boolean tested = false;
                for (int y = site.y() - 8; y <= site.y() + 8; y++) {
                    for (int x = site.x() - 8; x <= site.x() + 8; x++) {
                        if (!kind.equals(state.world.locationKindAt(state.currentMapId, x, y))
                                || !state.world.isPassable(state.currentMapId, x, y)) continue;
                        boolean road = Terrain.connectingRoad(state.world.tileAt(state.currentMapId, x, y));
                        if (tested && !road) continue;
                        state.playerX = x;
                        state.playerY = y;
                        state.battle = null;
                        state.mode = GameMode.EXPLORE;
                        state.maybeStartEncounter();
                        require(state.battle != null, "Missing encounter in " + kind);
                        require(state.battle.monsterSpecs.stream().allMatch(spec -> pool.contains(spec.key())),
                                "Wrong residents in actual " + kind + " encounter");
                        tested = true;
                        checked++;
                        if (road) paths++;
                    }
                }
                // Overlapping patches can completely cover a site's footprint; the effective
                // locationKindAt above is the same authority used by gameplay.
            }
        }
        require(checked > 0 && paths > 0, "Did not exercise real sites and paths");
        // Ordinary travel roads stay safe outside hostile footprints.
        boolean safeRoad = false;
        for (int y = 0; y < state.world.height(state.currentMapId) && !safeRoad; y++) {
            for (int x = 0; x < state.world.width(state.currentMapId); x++) {
                if (!Terrain.connectingRoad(state.world.tileAt(state.currentMapId, x, y))
                        || state.world.locationKindAt(state.currentMapId, x, y) != null) continue;
                state.playerX = x;
                state.playerY = y;
                state.battle = null;
                state.maybeStartEncounter();
                require(state.battle == null, "Ordinary road became hostile");
                safeRoad = true;
                break;
            }
        }
        require(safeRoad, "No ordinary road tested");
        System.out.println("Location encounters passed: " + checked + " site encounters, " + paths + " on paths.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
