package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.Path;
import java.util.ArrayList;

public final class SubtileCollisionTest {
    public static void main(String[] args) throws Exception {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        state.chooseClass("Mage");
        state.mode = GameMode.EXPLORE;
        state.currentMapId = WorldMap.OVERWORLD_ID;
        var world = state.world;
        var area = world.area(state.currentMapId);
        int x = 20, y = 20;
        for (var prop : new ArrayList<>(area.propsInBounds(x - 5, y - 5, x + 10, y + 10))) area.removeProp(prop);
        area.fillTiles(x - 5, y - 5, x + 10, y + 10, 'g');
        WorldProp tree = new WorldProp(x, y, "deco_tree_oak_harvestable", 80);
        area.addProp(tree);
        var trunk = PropCollision.footprint(world, state.currentMapId, tree);
        require(trunk != null, "Tree has no trunk");
        require(!PropCollision.clear(world, state.currentMapId, trunk.getCenterX(), trunk.getCenterY()), "Walked through trunk");
        require(!PropCollision.canTravel(world, state.currentMapId, x - 1, trunk.getCenterY(), x + 2, trunk.getCenterY()), "Tunneled through trunk");
        var joinStart = new PropCollision.Anchor(x - 0.25, trunk.getCenterY());
        var joinEnd = new PropCollision.Anchor(x + 1.25, trunk.getCenterY());
        var join = PropCollision.localRoute(world, state.currentMapId, joinStart, joinEnd);
        require(join.size() > 1, "Fractional click route cannot detour around trunk");
        var joinPrevious = joinStart;
        for (var next : join) {
            require(PropCollision.canTravel(world, state.currentMapId, joinPrevious.x(), joinPrevious.y(), next.x(), next.y()),
                    "Fractional route clips trunk");
            joinPrevious = next;
        }
        require(PropCollision.clear(world, state.currentMapId, x + 0.5, y - 0.5), "Canopy blocks ground");
        state.playerX = x;
        state.playerY = y;
        require(!state.moveFreeExploreTo(trunk.getCenterX(), trunk.getCenterY()), "Same-tile movement bypasses collision");
        var start = new TilePoint(x - 2, y);
        var target = new TilePoint(x + 2, y);
        long before = System.nanoTime();
        var path = Pathfinder.findPath(state, start, target);
        require(!path.isEmpty(), "No route around tree");
        var previous = PropCollision.navigationAnchor(world, state.currentMapId, start.x(), start.y());
        for (var tile : path) {
            var next = PropCollision.navigationAnchor(world, state.currentMapId, tile.x(), tile.y());
            require(next != null && PropCollision.canTravel(world, state.currentMapId,
                    previous.x(), previous.y(), next.x(), next.y()), "Path crosses collision");
            previous = next;
        }
        System.out.println("Tree detour: " + path.size() + " steps, " + (System.nanoTime() - before) / 1_000_000 + " ms");
        area.removeProp(tree);
        require(PropCollision.clear(world, state.currentMapId, trunk.getCenterX(), trunk.getCenterY()), "Harvest leaves stale collision");
        WorldProp ore = new WorldProp(x, y, "deco_ore_iron_vein", 48);
        area.addProp(ore);
        var rock = PropCollision.footprint(world, state.currentMapId, ore);
        require(rock != null && !PropCollision.clear(world, state.currentMapId, rock.getCenterX(), rock.getCenterY()), "Ore not solid");
        area.removeProp(ore);
        area.setTile(x + 1, y, 'm');
        require(!PropCollision.clear(world, state.currentMapId, x + 0.95, y + 0.5), "Player clips wall edge");
        require(!PropCollision.canTravel(world, state.currentMapId, x + 0.5, y + 0.5, x + 1.5, y + 1.5), "Cut diagonal wall corner");
        area.setTile(x + 1, y, 'g');
        for (int slot = 0; slot < 4; slot++) area.addProp(new WorldProp(x, y, "deco_forest_mushrooms", 18, slot));
        require(area.propAt(x, y) == null, "Visual detail hides interactive prop");
        require(state.crafting.findGatherTargetAt(world, state.currentMapId, x, y) == null, "Visual detail becomes resource");
        require(PropCollision.clear(world, state.currentMapId, x + 0.5, y + 0.5), "Small plants block movement");
        require(PropCollision.footprint(world, state.currentMapId,
                new WorldProp(x, y, "deco_soft_dense_leaf_bush", 48)) != null, "Dense bush not solid");
        require(world.props(state.currentMapId).stream().filter(p -> p.visualSlot() >= 0).count() > 100, "No quarter-tile details generated");
        var addTransition = WorldMap.class.getDeclaredMethod("addTransition", String.class, int.class, int.class,
                String.class, int.class, int.class, String.class);
        addTransition.setAccessible(true);
        addTransition.invoke(world, state.currentMapId, x + 1, y, state.currentMapId, x + 5, y + 5, "Test doorway");
        area.setTile(x + 1, y, 'm');
        state.playerX = x; state.playerY = y;
        require(state.moveFreeExploreTo(x + 0.8, y + 0.5, x + 0.9, y + 0.5)
                && state.playerX == x + 5 && state.playerY == y + 5, "Footprint prevents entry through authored doorway");
        System.out.println("Subtile collision passed: trunk, canopy, ore, bushes, wall clearance, swept movement, paths and visual-only details.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
