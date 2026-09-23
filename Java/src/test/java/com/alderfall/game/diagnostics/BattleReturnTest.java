package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;

public final class BattleReturnTest {
    public static void main(String[] args) {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        state.chooseClass("Mage");
        String map = WorldMap.OVERWORLD_ID;
        var area = state.world.area(map);
        for (var prop : new ArrayList<>(area.propsInBounds(10, 10, 30, 30))) area.removeProp(prop);
        area.fillTiles(10, 10, 30, 30, 'g');
        for (boolean escape : new boolean[]{false, true}) {
            for (String obstacle : new String[]{"wall", "isolated", "deco_tree_oak_harvestable", "deco_ore_iron_vein", "clear"}) {
                area.fillTiles(17, 17, 23, 23, 'g');
                WorldProp prop = null;
                if (obstacle.equals("wall")) area.setTile(20, 20, 'm');
                else if (obstacle.equals("isolated")) {
                    area.setTile(19, 20, 'm'); area.setTile(21, 20, 'm');
                    area.setTile(20, 19, 'm'); area.setTile(20, 21, 'm');
                } else if (!obstacle.equals("clear")) {
                    prop = new WorldProp(20, 20, obstacle, 80);
                    area.addProp(prop);
                }
                state.currentMapId = map;
                state.playerX = 20; state.playerY = 20;
                state.player.healFull();
                state.battle = new Battle(state.player, GameData.MONSTERS.get("slime"), new Random(42));
                state.mode = GameMode.BATTLE;
                state.battle.finished = true;
                state.battle.victory = !escape;
                state.battle.fled = escape;
                if (escape) state.tickBattle();
                else state.leaveFinishedBattle();
                check(state.mode == GameMode.EXPLORE && state.battle == null, "Battle did not close");
                check(state.currentMapId.equals(map), "Return changed map");
                assertCanLeave(state);
                if (obstacle.equals("clear")) check(state.playerX == 20 && state.playerY == 20, "Safe position moved");
                if (prop != null) area.removeProp(prop);
            }
        }
        TilePoint preferred = state.world.playerVillageRespawnPoint();
        state.world.area(WorldMap.PLAYER_VILLAGE_ID).setTile(preferred.x(), preferred.y(), 'm');
        state.player.hp = 0;
        state.revive();
        check(!preferred.equals(new TilePoint(state.playerX, state.playerY)), "Revive reused a blocked point");
        assertCanLeave(state);
        System.out.println("BattleReturnTest passed: victory, escape, revival, walls, isolated tiles, trees, rocks and unchanged safe positions.");
    }

    private static void assertCanLeave(GameState state) {
        double x = state.playerX + 0.5, y = state.playerY + 0.5;
        check(PropCollision.clear(state.world, state.currentMapId, x, y), "Return overlaps an obstacle");
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : directions) {
            if (PropCollision.clear(state.world, state.currentMapId, x + d[0], y + d[1])
                    && PropCollision.canTravel(state.world, state.currentMapId, x, y, x + d[0], y + d[1])) return;
        }
        throw new AssertionError("Return has no walkable exit");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
