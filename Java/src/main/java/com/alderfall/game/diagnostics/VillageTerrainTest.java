package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Arrays;

/** Real village chunks, road material consistency, and retired-asset fallback checks. */
public final class VillageTerrainTest {
    public static void main(String[] args) {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath().normalize()));
        AssetStore assets = new AssetStore(Path.of("assets"));
        for (String name : new String[]{"submerged_sand", "submerged_sand_variant_1", "village_packed_earth"}) {
            require(!assets.hasSprite(name), "Retired texture is still in runtime catalog: " + name);
        }
        for (char tile : new char[]{'~', 'V', '8'}) require(assets.hasSprite(Terrain.assetName(tile)), "Missing terrain fallback");
        var painter = (WorldRenderer.TerrainPainter) Proxy.newProxyInstance(WorldRenderer.TerrainPainter.class.getClassLoader(),
                new Class<?>[]{WorldRenderer.TerrainPainter.class}, (proxy, method, values) -> switch (method.getName()) {
                    case "visibleTerrainTile" -> values[0];
                    case "terrainImageName" -> Terrain.assetName((char) values[0]);
                    default -> null;
                });
        WorldRenderer renderer = new WorldRenderer(assets, painter, null, null);
        for (String id : new String[]{"village_dunewick", "village_snowrest", "village_mireford", "village_oakhaven"}) {
            state.currentMapId = id;
            MapArea area = state.world.area(id);
            char[][] original = Arrays.stream(area.tiles).map(char[]::clone).toArray(char[][]::new);
            for (int size : new int[]{24, 48}) {
                compare(render(renderer, state, size, false), render(renderer, state, size, true));
            }
            require(Arrays.deepEquals(original, area.tiles), "Village render changed walkability");
            for (WorldProp p : area.props) {
                if (!p.asset().startsWith("deco_") || area.landmarks.containsKey(new TilePoint(p.x(), p.y()))
                        || state.world.transitionAt(id, p.x(), p.y()) != null) continue;
                require(state.world.cityBuildingAt(id, p.x(), p.y()) == null, "Natural prop inside village building: " + p);
            }
            WorldProp tree = new WorldProp(4, 4, "deco_tree_birch_harvestable", 76);
            require(PropPlacement.at(state.world, id, tree).kind() == PropPlacement.Kind.TREE, "Village props still snap to old anchor");
        }
        state.currentMapId = state.world.createEditorMap("editor_village_surface_check", "Surface", "village", 12, 12);
        MapArea area = state.world.area(state.currentMapId);
        LayeredTerrainRenderer layers = new LayeredTerrainRenderer(assets);
        BufferedImage reference = null;
        for (char road : new char[]{'r', 'T', '8', 'V'}) {
            area.fillTiles(0, 0, area.width() - 1, area.height() - 1, 'g');
            for (int y = 0; y < 12; y++) area.setTile(5, y, road);
            BufferedImage image = layers.tile(state, painter, 5, 5, 48).image();
            if (reference == null) reference = image; else compare(reference, image);
        }
        area.fillTiles(0, 0, area.width() - 1, area.height() - 1, '~');
        var shallow = layers.tile(state, painter, 5, 5, 48);
        require(shallow.hasWater() && shallow.waterClip().contains(24, 24), "Shallow water lost its animation mask");
        for (int pixel : shallow.image().getRGB(0, 0, 48, 48, null, 0, 48)) {
            require((pixel & 255) > ((pixel >> 16) & 255), "Warm sand stamp remains in shallow water");
        }
        System.out.println("Village terrain passed: four regional villages, cached/direct pixels at two zooms, coherent road materials, water masks and retired-asset fallbacks.");
    }
    private static BufferedImage render(WorldRenderer renderer, GameState state, int size, boolean cached) {
        BufferedImage image = new BufferedImage(20 * size, 18 * size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        var context = new WorldRenderer.TerrainContext(state, 7, 5, 20, 18, state.world.width(state.currentMapId),
                state.world.height(state.currentMapId), size, size * 100 / 48, "village");
        if (cached) renderer.drawCachedTerrainBase(g, context); else renderer.drawTerrainBase(g, context);
        g.dispose();
        return image;
    }
    private static void compare(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) for (int x = 0; x < a.getWidth(); x++) {
            require(a.getRGB(x, y) == b.getRGB(x, y), "Terrain seam/material mismatch at " + x + "," + y);
        }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
