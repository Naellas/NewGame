package com.alderfall.game;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/** Run with java -cp out com.alderfall.game.TownNpcNavigationTest. */
public final class TownNpcNavigationTest {
    public static void main(String[] args) throws Exception {
        TilePoint start = new TilePoint(1, 3), goal = new TilePoint(5, 3);
        BiPredicate<Integer, Integer> wall = (x, y) -> x != 3 || y == 0;
        List<TilePoint> aroundWall = TownNpcNavigation.path(7, 7, start, goal, wall, (x, y) -> false);
        require(!aroundWall.isEmpty() && aroundWall.contains(new TilePoint(3, 0)), "Cannot route around a long wall");
        checkSteps(start, aroundWall, wall);
        require(TownNpcNavigation.path(7, 7, start, goal, (x, y) -> x != 3, (x, y) -> false).isEmpty(),
                "Unreachable target should fail cleanly");
        require(TownNpcNavigation.path(2, 2, new TilePoint(0, 0), new TilePoint(1, 1),
                (x, y) -> x == y, (x, y) -> true).isEmpty(), "Cut a blocked diagonal corner");

        List<TilePoint> street = TownNpcNavigation.path(11, 4, new TilePoint(0, 2), new TilePoint(10, 2),
                (x, y) -> true, (x, y) -> y == 1);
        require(street.stream().filter(p -> p.y() == 1).count() >= 9, "Ignored cheaper street detour");
        TilePoint blocker = street.get(4);
        List<TilePoint> detour = TownNpcNavigation.path(11, 4, new TilePoint(0, 2), new TilePoint(10, 2),
                (x, y) -> !blocker.equals(new TilePoint(x, y)), (x, y) -> y == 1);
        require(!detour.isEmpty() && !detour.contains(blocker), "Did not route around an occupied tile");
        require(TownNpcNavigation.path(7, 7, start, start, wall, (x, y) -> false).isEmpty(), "Already at target");

        GameState state = new GameState(GameConfig.load(Path.of("")));
        state.random.setSeed(42);
        Method update = GameState.class.getDeclaredMethod("updateNpcMovement");
        update.setAccessible(true);
        int totalMoves = 0;
        for (String mapId : List.of("village_elderford", "town_briarbridge", "city_riverside")) {
            require(state.world.hasMap(mapId), "Missing simulation map " + mapId);
            state.currentMapId = mapId;
            state.playerX = 0;
            state.playerY = 0;
            state.worldTick = 9 * GameState.TICKS_PER_GAME_DAY / 24;
            state.resetNpcRuntime();
            Map<Npc, TilePoint> previous = new HashMap<>();
            int moved = 0, circuits = 0;
            for (var motion : state.npcMotionsForMap(mapId)) {
                var routine = AmbientNpcAi.routineFor(motion.npc(), state.world, mapId, 540, WeatherCondition.CLEAR, 1);
                if (routine.activity() == AmbientNpcAi.Activity.PATROLLING
                        && TownNpcNavigation.checkpoints(motion.npc(), state.world, routine).size() > 1) circuits++;
            }
            for (int tick = 0; tick < 1200; tick++) {
                state.worldTick++;
                update.invoke(state);
                for (var motion : state.npcMotionsForMap(mapId)) {
                    TilePoint position = new TilePoint(motion.x(), motion.y());
                    TilePoint before = previous.put(motion.npc(), position);
                    require(state.world.isPassable(mapId, position.x(), position.y()), "NPC entered blocked terrain");
                    // Some authored spawn points are shared; movement must never enter one.
                    if (before != null && !before.equals(position)) {
                        require(TownNpcNavigation.distance(before, position) == 1, "NPC teleported or cut a corner");
                        require(state.world.transitionAt(mapId, position.x(), position.y()) == null, "NPC blocked a doorway");
                        require(state.npcMotionsForMap(mapId).stream().noneMatch(other ->
                                !other.npc().equals(motion.npc()) && other.x() == motion.x() && other.y() == motion.y()),
                                "NPC entered an occupied tile");
                        moved++;
                    }
                }
            }
            require(moved > 20, "Town residents stopped moving: " + mapId);
            totalMoves += moved;
            System.out.println(mapId + ": " + moved + " safe steps, " + circuits + " patrol circuits");
        }
        System.out.println("Town navigation checks passed: " + totalMoves + " simulated steps.");
    }

    private static void checkSteps(TilePoint start, List<TilePoint> path, BiPredicate<Integer, Integer> walkable) {
        TilePoint previous = start;
        for (TilePoint step : path) {
            require(TownNpcNavigation.distance(previous, step) == 1 && walkable.test(step.x(), step.y()), "Invalid path step");
            previous = step;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
