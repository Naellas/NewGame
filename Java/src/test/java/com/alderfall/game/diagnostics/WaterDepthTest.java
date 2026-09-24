package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Arrays;

/** Depth must agree across collision, pathfinding and weather-aware continuous rendering. */
public final class WaterDepthTest {
    public static void main(String[] args) throws Exception {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        state.currentMapId = state.world.createEditorMap("editor_water_test", "Water test", "overworld", 24, 20);
        var area = state.world.area(state.currentMapId);
        area.fillTiles(0, 0, 23, 19, 'w');
        area.fillTiles(0, 0, 4, 19, 'g');
        require(state.world.waterDepth(state.currentMapId, 5, 10) == WaterDepth.SHALLOW, "First shelf");
        require(state.world.waterDepth(state.currentMapId, 6, 10) == WaterDepth.WADING, "Second shelf");
        require(state.world.waterDepth(state.currentMapId, 7, 10) == WaterDepth.DEEP, "Deep interior");
        require(PropCollision.clear(state.world, state.currentMapId, 5.5, 10.5), "Shallows block movement");
        require(PropCollision.clear(state.world, state.currentMapId, 6.5, 10.5), "Wading blocks movement");
        require(!PropCollision.clear(state.world, state.currentMapId, 7.5, 10.5), "Deep water permits movement");
        require(!Pathfinder.findPath(state, new TilePoint(4, 10), new TilePoint(6, 10)).isEmpty(), "No wading path");
        require(Pathfinder.findPath(state, new TilePoint(4, 10), new TilePoint(9, 10)).isEmpty(), "Path enters deep water");
        area.fillTiles(4, 0, 4, 19, 'w');
        require(state.world.waterDepth(state.currentMapId, 6, 10) == WaterDepth.DEEP, "Edited bank leaves stale depths");
        area.fillTiles(12, 10, 12, 10, 'B');
        require(state.world.waterDepth(state.currentMapId, 13, 10) == WaterDepth.DEEP, "Bridge creates shelf");
        area.fillTiles(15, 10, 15, 10, '~');
        require(state.world.waterDepth(state.currentMapId, 15, 10) == WaterDepth.SHALLOW, "Authored shallows");
        var assets = new AssetStore(Path.of("assets"));
        require(assets.hasAnimatedSprite("water_crest", "cycle"), "Missing generated wave strip");
        require(assets.animatedSpriteFrameCount("water_crest", "cycle", 192, 64) == 12, "Wave frame metadata");
        var renderer = new WaterTileRenderer(assets);
        var weather = new WeatherSystem(state);
        for (WeatherQuality quality : WeatherQuality.values()) {
            BufferedImage first = render(renderer, weather, quality, 17, 0);
            BufferedImage next = render(renderer, weather, quality, 18, 0);
            require(difference(first, next) > 0, "Frozen animation: " + quality);
            require(difference(first, next) < 0.8, "Animation reset: " + quality);
            require(Arrays.equals(pixels(first), pixels(render(renderer, weather, quality, 17, 0))), "Unstable frame");
            BufferedImage shifted = render(renderer, weather, quality, 17, 13);
            for (int y = 0; y < 96; y++) for (int x = 0; x < 120; x++)
                require(first.getRGB(x + 13, y) == shifted.getRGB(x, y), "Camera changes wave alignment");
        }
        var field = WeatherSystem.class.getDeclaredField("stormIntensity");
        field.setAccessible(true); field.setDouble(weather, 1);
        var wave = WeatherSystem.class.getDeclaredField("waterWave");
        wave.setAccessible(true); wave.setDouble(weather, .95);
        require(difference(render(renderer, weather, WeatherQuality.HIGH, 90, 0),
                render(renderer, new WeatherSystem(state), WeatherQuality.HIGH, 90, 0)) > .1, "Storm looks calm");
        BufferedImage prior = render(renderer, weather, WeatherQuality.HIGH, 0, 0);
        double[] stormChanges = new double[240];
        for (int frame = 1; frame <= stormChanges.length; frame++) {
            BufferedImage next = render(renderer, weather, WeatherQuality.HIGH, frame, 0);
            stormChanges[frame - 1] = difference(prior, next);
            prior = next;
        }
        Arrays.sort(stormChanges);
        // Moving high-contrast foam changes more pixels than calm lines. Detect exceptional
        // discontinuities relative to normal motion, as well as a 6/255 absolute color budget.
        require(stormChanges[239] < 6 && stormChanges[239] < stormChanges[120] * 2.5,
                "Storm cycle has a temporal discontinuity");
        BufferedImage beforeWeather = render(renderer, weather, WeatherQuality.HIGH, 75, 0);
        wave.setDouble(weather, .949);
        require(difference(beforeWeather, render(renderer, weather, WeatherQuality.HIGH, 75, 0)) < .15,
                "Small weather transition resets wave phase");
        javax.swing.SwingUtilities.invokeAndWait(WaterDepthTest::checkImmersion);
        System.out.println("Water depth passed: shelves, collision, paths, edits, bridges, smooth animation, camera alignment and storms.");
    }

    private static void checkImmersion() {
        GamePanel panel = new GamePanel(Path.of("").toAbsolutePath());
        try {
            var timer = GamePanel.class.getDeclaredField("timer"); timer.setAccessible(true);
            ((javax.swing.Timer) timer.get(panel)).stop();
            var field = GamePanel.class.getDeclaredField("worldDepthRenderer"); field.setAccessible(true);
            WorldDepthRenderer depth = (WorldDepthRenderer) field.get(panel);
            var draw = Arrays.stream(GamePanel.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals("drawCharacterSprite") && m.getParameterCount() == 10)
                    .findFirst().orElseThrow();
            draw.setAccessible(true);
            BufferedImage image = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            depth.begin(48);
            draw.invoke(panel, g, "class_mage_model", 48, 20, 64, 64, 0, 1, null, 26);
            depth.draw(g); g.dispose();
            int visible = 0;
            for (int y = 0; y < 160; y++) for (int x = 0; x < 160; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (y >= 84) require(alpha == 0, "Deferred sprite or shadow leaks below waterline");
                else if (alpha > 0) visible++;
            }
            require(visible > 100, "Immersion hides whole character");
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        } finally { panel.shutdown(); }
    }

    private static BufferedImage render(WaterTileRenderer renderer, WeatherSystem weather,
                                         WeatherQuality quality, int frame, int offset) {
        BufferedImage image = new BufferedImage(144, 96, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new java.awt.Color(39, 99, 122)); g.fillRect(0, 0, 144, 96);
        for (int y = 0; y < 2; y++) for (int x = 0; x < 4; x++)
            renderer.draw(g, x, y, x * 48 - offset, y * 48, 48, frame, "review", weather, quality);
        g.dispose(); return image;
    }
    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }
    private static double difference(BufferedImage a, BufferedImage b) {
        int[] aa = pixels(a), bb = pixels(b); double sum = 0;
        for (int i = 0; i < aa.length; i++) for (int shift : new int[]{0, 8, 16})
            sum += Math.abs((aa[i] >> shift & 255) - (bb[i] >> shift & 255));
        return sum / (aa.length * 3);
    }
    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
