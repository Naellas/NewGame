package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Regression checks for deterministic, non-repeating natural ground meshes. */
public final class NaturalTerrainVariantTest {
    public static void main(String[] args) {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath().normalize()));
        AssetStore assets = new AssetStore(Path.of("assets"));
        var painter = (WorldRenderer.TerrainPainter) Proxy.newProxyInstance(
                WorldRenderer.TerrainPainter.class.getClassLoader(),
                new Class<?>[]{WorldRenderer.TerrainPainter.class},
                (proxy, method, values) -> switch (method.getName()) {
                    case "visibleTerrainTile" -> values[0];
                    case "terrainImageName" -> Terrain.assetName((char) values[0]);
                    default -> null;
                });
        state.currentMapId = state.world.createEditorMap("editor_natural_mesh", "Natural Mesh", "overworld", 32, 24);
        MapArea area = state.world.area(state.currentMapId);
        LayeredTerrainRenderer renderer = new LayeredTerrainRenderer(assets);

        for (char material : new char[]{'g', 'f', 'n', 'v', 'b'}) {
            String base = Terrain.assetName(material);
            int count = LayeredTerrainRenderer.naturalVariantCount(material);
            for (int i = 0; i < count; i++) {
                String asset = i == 0 ? base : base + "_variant_" + i;
                require(assets.hasSprite(asset), "Missing natural terrain variant: " + asset);
            }
            area.fillTiles(0, 0, area.width() - 1, area.height() - 1, material);
            Set<Integer> signatures = new HashSet<>();
            BufferedImage previous = null;
            double worstSeam = 0;
            for (int x = 6; x < 14; x++) {
                BufferedImage tile = renderer.tile(state, painter, x, 9, 48).image();
                signatures.add(Arrays.hashCode(pixels(tile)));
                if (previous != null) worstSeam = Math.max(worstSeam, seamDifference(previous, tile));
                previous = tile;
            }
            require(signatures.size() >= 5, "Natural terrain still repeats visibly: " + base);
            require(worstSeam < 26, "Natural terrain has a hard tile seam: " + base + " delta=" + worstSeam);
        }
        System.out.println("Natural terrain variants passed: five biomes use deterministic world-space meshes without hard tile seams.");
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static double seamDifference(BufferedImage left, BufferedImage right) {
        double difference = 0;
        for (int y = 0; y < left.getHeight(); y++) {
            int a = left.getRGB(left.getWidth() - 1, y);
            int b = right.getRGB(0, y);
            difference += Math.abs((a >> 16 & 255) - (b >> 16 & 255));
            difference += Math.abs((a >> 8 & 255) - (b >> 8 & 255));
            difference += Math.abs((a & 255) - (b & 255));
        }
        return difference / (left.getHeight() * 3.0);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
