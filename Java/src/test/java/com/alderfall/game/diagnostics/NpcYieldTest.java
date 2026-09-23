package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import com.alderfall.game.map.WorldMap;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Player movement and click-route regressions for residents blocking narrow entrances. */
public final class NpcYieldTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        GameState state = new GameState(GameConfig.load(Path.of("")));
        state.chooseClass("Mage");
        state.mode = GameMode.EXPLORE;
        String id = "npc_yield_test";
        char[][] tiles = new char[7][9];
        for (char[] row : tiles) java.util.Arrays.fill(row, 'm');
        for (int x = 1; x <= 7; x++) tiles[3][x] = 'r';
        MapArea area = new MapArea(id, "Yield test", "village", tiles);
        Field mapsField = WorldMap.class.getDeclaredField("maps");
        mapsField.setAccessible(true);
        ((Map<String, MapArea>) mapsField.get(state.world)).put(id, area);
        Npc resident = new Npc(id, "Test resident", "npc_villager", 4, 3,
                List.of("Hello."), null, null, null, 0);
        Field residentsField = WorldMap.class.getDeclaredField("interiorNpcs");
        residentsField.setAccessible(true);
        ((Map<String, List<Npc>>) residentsField.get(state.world)).put(id, List.of(resident));
        state.currentMapId = id;
        reset(state);

        require(state.npcAt(id, 4, 3) != null, "Fixture resident missing");
        require(Pathfinder.playerWalkable(state, 4, 3), "Resident still blocks player route");
        require(!Pathfinder.walkable(state, 4, 3), "Follower placement can overlap resident");
        require(Pathfinder.nearestTarget(state, 4, 3).equals(new TilePoint(4, 3)), "Occupied entrance replaced by wrong target");
        var path = Pathfinder.findPath(state, new TilePoint(3, 3), new TilePoint(6, 3));
        require(path.contains(new TilePoint(4, 3)), "No path through single-width occupied entrance");
        for (TilePoint step : path) require(state.move(step.x() - state.playerX, step.y() - state.playerY), "Route failed during execution");
        require(state.npcPosition(resident).equals(new TilePoint(4, 3)), "Trapped resident was pushed into wall/player");

        reset(state);
        require(state.moveFreeExploreTo(3.8, 3.5, 4.05, 3.5), "Continuous movement blocked at occupied tile boundary");
        require(state.moveFreeExploreTo(4.05, 3.5, 4.7, 3.5), "Could not squeeze through resident tile");
        require(state.moveFreeExploreTo(4.7, 3.5, 5.1, 3.5), "Could not exit resident tile");

        area.setTile(4, 2, 'r');
        reset(state);
        require(state.move(1, 0), "Player could not nudge resident");
        require(state.npcPosition(resident).equals(new TilePoint(4, 2)), "Resident did not sidestep into safe alcove");
        var update = GameState.class.getDeclaredMethod("updateNpcMovement");
        update.setAccessible(true);
        state.worldTick++;
        update.invoke(state);
        require(state.npcPosition(resident).equals(new TilePoint(4, 2)), "Resident immediately moved back into player");

        for (int x = 2; x <= 6; x++) area.setTile(x, 2, 'r');
        reset(state);
        require(!Pathfinder.findPath(state, new TilePoint(3, 3), new TilePoint(6, 3)).contains(new TilePoint(4, 3)),
                "Click route did not prefer available space around resident");
        require(!state.move(0, 1), "Soft resident collision disabled solid walls");
        require(state.npcPosition(resident).equals(new TilePoint(4, 3)), "Failed player move displaced resident");

        // A resident standing on the actual transition must not stop entry.
        var addTransition = WorldMap.class.getDeclaredMethod("addTransition", String.class, int.class, int.class,
                String.class, int.class, int.class, String.class);
        addTransition.setAccessible(true);
        addTransition.invoke(state.world, id, 4, 3, id, 7, 3, "Doorway");
        require(state.move(1, 0) && state.playerX == 7 && state.playerY == 3, "Resident blocked building transition");

        ((Map<String, MapArea>) mapsField.get(state.world)).put(id, new MapArea(id, "Interior", "interior", tiles));
        reset(state);
        require(Pathfinder.playerWalkable(state, 4, 3), "Interior residents block exits");
        ((Map<String, MapArea>) mapsField.get(state.world)).put(id, new MapArea(id, "Dungeon", "dungeon", tiles));
        reset(state);
        require(!Pathfinder.playerWalkable(state, 4, 3), "Non-resident map lost NPC collision");
        require(!state.move(1, 0), "Dungeon NPC could be pushed through");
        System.out.println("NPC yielding passed: narrow corridors, continuous movement, sidesteps, crowd avoidance, doors and solid walls.");
    }

    private static void reset(GameState state) {
        state.playerX = 3;
        state.playerY = 3;
        state.resetNpcRuntime();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
