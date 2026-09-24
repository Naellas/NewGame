package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.lang.reflect.Proxy;
import java.nio.file.Path;

/** Swamp dressing, traversable pools and regional water color regressions. */
public final class SwampEnvironmentTest {
    public static void main(String[] args) {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        var world = state.world;
        var overworld = world.area(WorldMap.OVERWORLD_ID);
        int swamp = 0, pools = 0, trees = 0, sedges = 0;
        for (int y = 1; y < overworld.height() - 1; y++) for (int x = 1; x < overworld.width() - 1; x++) {
            if (overworld.tileAt(x, y) == 'v') swamp++;
            if (overworld.tileAt(x, y) == '~') {
                pools++;
                require(world.waterDepth(WorldMap.OVERWORLD_ID, x, y) == WaterDepth.SHALLOW,
                        "Marsh pool lost its shallow depth");
            }
        }
        for (var prop : world.props(WorldMap.OVERWORLD_ID)) {
            if (overworld.tileAt(prop.x(), prop.y()) != 'v') continue;
            if (prop.asset().contains("tree_cypress") || prop.asset().contains("tree_willow")) trees++;
            if (prop.asset().equals("deco_marsh_sedge_clump")) {
                sedges++;
                require(PropCollision.footprint(world, WorldMap.OVERWORLD_ID, prop) == null,
                        "Sedges block walking");
            }
        }
        require(pools > 100 && trees > swamp / 40 && sedges > swamp / 80,
                "Swamp lost its pools, canopy or bank vegetation");
        state.currentMapId = world.createEditorMap("editor_swamp_test", "Swamp", "overworld", 24, 24);
        var area = world.area(state.currentMapId);
        area.fillTiles(0, 0, 23, 23, 'g');
        area.fillTiles(8, 8, 12, 12, '~');
        var painter = (WorldRenderer.TerrainPainter) Proxy.newProxyInstance(
                WorldRenderer.TerrainPainter.class.getClassLoader(), new Class<?>[]{WorldRenderer.TerrainPainter.class},
                (proxy, method, values) -> switch (method.getName()) {
                    case "visibleTerrainTile" -> values[0];
                    case "terrainImageName" -> Terrain.assetName((char) values[0]);
                    default -> null;
                });
        var layers = new LayeredTerrainRenderer(new AssetStore(Path.of("assets")));
        int clear = layers.tile(state, painter, 10, 10, 48).image().getRGB(24, 24);
        area.fillTiles(0, 0, 7, 23, 'v');
        int peat = layers.tile(state, painter, 10, 10, 48).image().getRGB(24, 24);
        require((peat & 255) < (clear & 255) - 25, "Nearby swamp fails to tint cached water");
        require(world.waterDepth(state.currentMapId, 10, 10) == WaterDepth.SHALLOW,
                "Color changed gameplay depth");
        area.fillTiles(0, 0, 7, 23, 'g');
        require(layers.tile(state, painter, 10, 10, 48).image().getRGB(24, 24) == clear,
                "Peat tint leaks into restored grassland water");
        System.out.printf("Swamp passed: %d marsh tiles, %d shallows, %d trees, %d sedges; tint and collision verified.%n",
                swamp, pools, trees, sedges);
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
