package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Arrays;

/** Rendering-only coverage, chunk seams, zoom, and edit invalidation checks. */
public final class LayeredTerrainTest {
    public static void main(String[] args) {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath().normalize()));
        state.currentMapId = state.world.createEditorMap("editor_layers", "Layers", "village", 30, 24);
        MapArea area = state.world.area(state.currentMapId);
        for (int y = 0; y < area.tiles.length; y++) {
            for (int x = 0; x < area.tiles[y].length; x++) {
                area.tiles[y][x] = x < 12 ? 'w' : y < 10 ? 'n' : 'g';
            }
        }
        char[][] before = Arrays.stream(area.tiles).map(char[]::clone).toArray(char[][]::new);
        AssetStore assets = new AssetStore(Path.of("assets"));
        WorldRenderer.TerrainPainter painter = (WorldRenderer.TerrainPainter) Proxy.newProxyInstance(
                WorldRenderer.TerrainPainter.class.getClassLoader(), new Class<?>[]{WorldRenderer.TerrainPainter.class},
                (proxy, method, values) -> switch (method.getName()) {
                    case "visibleTerrainTile" -> values[0];
                    case "terrainImageName" -> Terrain.assetName((char) values[0]);
                    default -> null;
                });
        LayeredTerrainRenderer layers = new LayeredTerrainRenderer(assets);
        for (int size : new int[]{24, 36, 48, 72}) {
            var sea = layers.tile(state, painter, 5, 14, size);
            var land = layers.tile(state, painter, 18, 14, size);
            require(sea.hasWater() && sea.waterClip().contains(size / 2.0, size / 2.0), "Sea lost its water mask");
            require(!land.hasWater(), "Land gained water animation");
            int mixed = 0;
            for (int y = 4; y < 18; y++) {
                for (int x = 11; x <= 12; x++) {
                    var edge = layers.tile(state, painter, x, y, size);
                    int wet = 0;
                    for (int py = 0; py < size; py++) for (int px = 0; px < size; px++) {
                        if (edge.waterClip().contains(px + 0.5, py + 0.5) && edge.hasWater()) wet++;
                        require((edge.image().getRGB(px, py) >>> 24) == 255, "Transparent hole in terrain");
                    }
                    if (wet > 0 && wet < size * size) mixed++;
                    require(edge.waterClip().contains(size / 2.0, size / 2.0) == (x < 12),
                            "Shore moved past the gameplay tile center");
                }
            }
            require(mixed > 0, "Coast still follows only square tile borders");
            require(layers.tile(state, painter, 5, 14, size) == sea, "Unchanged tile was recomposed");
        }
        WorldRenderer renderer = new WorldRenderer(assets, painter, null, null);
        BufferedImage direct = render(renderer, state, false, 6, 5, 18, 14);
        BufferedImage cached = render(renderer, state, true, 6, 5, 18, 14);
        compare(direct, cached);
        BufferedImage scrolled = render(renderer, state, true, 7, 6, 16, 12);
        compare(direct.getSubimage(24, 24, 16 * 24, 12 * 24), scrolled);
        require(Arrays.deepEquals(before, area.tiles), "Rendering mutated gameplay tiles");
        var old = layers.tile(state, painter, 12, 14, 48);
        area.tiles[14][11] = 'g';
        require(layers.tile(state, painter, 12, 14, 48) != old, "Neighbor edit did not invalidate surface");
        BufferedImage edited = render(renderer, state, true, 6, 5, 18, 14);
        compare(render(renderer, state, false, 6, 5, 18, 14), edited);
        require(!Arrays.equals(cached.getRGB(0, 0, 432, 336, null, 0, 432),
                edited.getRGB(0, 0, 432, 336, null, 0, 432)), "Edit did not update cached chunks");
        for (var region : state.world.groundRegions()) {
            require(region.coverage(region.x() + 0.5, region.y() + 0.5) == 1, "Camp center is not grounded");
            require(region.coverage(region.x() + region.radiusX() + 4, region.y()) == 0, "Camp footprint leaks");
        }
        System.out.println("Layered terrain passed: masks at four zooms, chunk/scroll equivalence, cache reuse and edit invalidation; gameplay unchanged.");
    }

    private static BufferedImage render(WorldRenderer renderer, GameState state, boolean cached,
                                        int x, int y, int cols, int rows) {
        BufferedImage image = new BufferedImage(cols * 24, rows * 24, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        var context = new WorldRenderer.TerrainContext(state, x, y, cols, rows, 30, 24, 24, 50, "overworld");
        if (cached) renderer.drawCachedTerrainBase(g, context); else renderer.drawTerrainBase(g, context);
        g.dispose();
        return image;
    }

    private static void compare(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) for (int x = 0; x < a.getWidth(); x++) {
            require(a.getRGB(x, y) == b.getRGB(x, y), "Chunk or camera seam at " + x + "," + y);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
