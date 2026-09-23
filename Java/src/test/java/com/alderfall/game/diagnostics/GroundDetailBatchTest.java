package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;

/** Cached/direct agreement at chunk seams, camera offsets and several zooms; invalidation after removal. */
public final class GroundDetailBatchTest {
    public static void main(String[] args) {
        var state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        state.currentMapId = WorldMap.OVERWORLD_ID;
        var area = state.world.area(state.currentMapId);
        for (var prop : new ArrayList<>(area.propsInBounds(12, 12, 36, 36))) area.removeProp(prop);
        area.fillTiles(12, 12, 36, 36, 'g');
        area.setNatureSites(java.util.List.of(new com.alderfall.game.map.NatureSiteGenerator.Site(
                26, 25, 'g', "deco_grass_stone_stack", java.util.List.of(
                new TilePoint(19, 23), new TilePoint(20, 23), new TilePoint(21, 23),
                new TilePoint(22, 23), new TilePoint(23, 23), new TilePoint(24, 23),
                new TilePoint(24, 24), new TilePoint(25, 24), new TilePoint(26, 24)))));
        area.setDestinationApproaches(java.util.List.of(new com.alderfall.game.map.DestinationApproaches.Approach(
                "fixture", "Rough trail fixture", "cache", area.id, new TilePoint(26, 25), 'g',
                java.util.List.of(new TilePoint(19, 25), new TilePoint(20, 25), new TilePoint(21, 25),
                        new TilePoint(22, 25), new TilePoint(23, 25), new TilePoint(24, 25),
                        new TilePoint(25, 25), new TilePoint(26, 25)), 3, 2)));
        area.setTile(24, 25, 'r');
        area.setApproachGround(new TilePoint(24, 25), 'g');
        for (int y = 19; y <= 29; y++) for (int x = 19; x <= 29; x++) {
            area.addProp(new WorldProp(x, y, "deco_grass_wildflowers", 22, (x + y) % 4));
        }
        var assets = new AssetStore(Path.of("assets").toAbsolutePath());
        require(assets.hasSprite("deco_grass_wildflowers"), "Missing detail asset");
        var painter = (WorldRenderer.TerrainPainter) Proxy.newProxyInstance(WorldRenderer.TerrainPainter.class.getClassLoader(),
                new Class<?>[]{WorldRenderer.TerrainPainter.class}, (proxy, method, values) -> switch (method.getName()) {
                    case "visibleTerrainTile" -> values[0];
                    case "terrainImageName" -> Terrain.assetName((char) values[0]);
                    default -> null;
                });
        for (int size : new int[]{24, 48, 72}) {
            state.zoom = size * 100 / 48;
            var renderer = new WorldRenderer(assets, painter, null, null);
            for (int camera : new int[]{20, 21, 23}) {
                var context = new WorldRenderer.TerrainContext(state, camera, camera, 6, 6,
                        area.width(), area.height(), size, state.zoom, "overworld");
                var direct = draw(renderer, context, false);
                var cold = draw(renderer, context, true);
                var warm = draw(renderer, context, true);
                require(equal(direct, cold), "Chunk seam or camera mismatch, tile size=" + size + ", camera=" + camera);
                require(equal(cold, warm), "Warm cache changes pixels");
            }
        }
        state.zoom = 100;
        var renderer = new WorldRenderer(assets, painter, null, null);
        var context = new WorldRenderer.TerrainContext(state, 20, 20, 6, 6, area.width(), area.height(), 48, 100, "overworld");
        var before = draw(renderer, context, true);
        for (var prop : new ArrayList<>(area.propsInBounds(12, 12, 36, 36))) area.removeProp(prop);
        var after = draw(renderer, context, true);
        require(!equal(before, after), "Removed details remain baked into cache");
        require(equal(after, draw(renderer, context, false)), "Invalidation does not match direct terrain");
        System.out.println("Ground detail batching passed: three zooms, three camera offsets, chunk seams, cold/warm cache and removal invalidation.");
    }
    private static BufferedImage draw(WorldRenderer renderer, WorldRenderer.TerrainContext context, boolean cached) {
        var image = new BufferedImage(context.visibleCols() * context.tileSize(), context.visibleRows() * context.tileSize(), BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        if (cached) renderer.drawCachedTerrainBase(g, context); else renderer.drawTerrainBase(g, context);
        g.dispose(); return image;
    }
    private static boolean equal(BufferedImage a, BufferedImage b) {
        return java.util.Arrays.equals(a.getRGB(0, 0, a.getWidth(), a.getHeight(), null, 0, a.getWidth()),
                b.getRGB(0, 0, b.getWidth(), b.getHeight(), null, 0, b.getWidth()));
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
