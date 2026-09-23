package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Arrays;

/** Cross-map road appearance and world-space sand sampling regressions. */
public final class RoadSurfaceTest {
    public static void main(String[] args) {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath().normalize()));
        AssetStore assets = new AssetStore(Path.of("assets"));
        for (String name : new String[]{"desert_sand_wind", "desert_path_packed"}) {
            require(assets.hasSprite(name), "Missing authored material: " + name);
        }
        var painter = (WorldRenderer.TerrainPainter) Proxy.newProxyInstance(WorldRenderer.TerrainPainter.class.getClassLoader(),
                new Class<?>[]{WorldRenderer.TerrainPainter.class}, (proxy, method, values) -> switch (method.getName()) {
                    case "visibleTerrainTile" -> values[0];
                    case "terrainImageName" -> Terrain.assetName((char) values[0]);
                    default -> null;
                });
        String outdoor = com.alderfall.game.map.WorldMap.OVERWORLD_ID;
        LayeredTerrainRenderer renderer = new LayeredTerrainRenderer(assets);
        for (String village : new String[]{"village_dunewick", "village_snowrest", "village_mireford", "village_oakhaven"}) {
            char ground = LayeredTerrainRenderer.villageGround(village);
            for (char road : new char[]{'r', 'T', '8', 'K'}) {
                fill(state.world.area(outdoor), ground, road);
                fill(state.world.area(village), ground, road);
                for (int size : new int[]{24, 48, 72}) {
                    state.currentMapId = outdoor;
                    BufferedImage a = renderer.tile(state, painter, 8, 8, size).image();
                    state.currentMapId = village;
                    BufferedImage b = renderer.tile(state, painter, 8, 8, size).image();
                    require(Arrays.equals(pixels(a), pixels(b)), "Overworld/village road mismatch: " + village + "/" + road);
                }
            }
        }
        state.currentMapId = outdoor;
        MapArea area = state.world.area(outdoor);
        area.fillTiles(0, 0, area.width() - 1, area.height() - 1, 's');
        BufferedImage a = renderer.tile(state, painter, 6, 8, 48).image();
        BufferedImage b = renderer.tile(state, painter, 7, 8, 48).image();
        BufferedImage c = renderer.tile(state, painter, 8, 8, 48).image();
        require(!Arrays.equals(pixels(a), pixels(b)), "Sand still repeats every tile");
        for (int y = 0; y < 48; y++) require(b.getRGB(47, y) == c.getRGB(0, y), "Large sand patch has a wrap seam");
        System.out.println("Road surfaces passed: identical village/overworld materials in four biomes at three zooms, varied adjacent sand tiles, seamless patch wrapping.");
    }
    private static void fill(MapArea area, char ground, char road) {
        area.fillTiles(0, 0, area.width() - 1, area.height() - 1, ground);
        for (int y = 0; y < area.height(); y++) area.setTile(8, y, road);
    }
    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
