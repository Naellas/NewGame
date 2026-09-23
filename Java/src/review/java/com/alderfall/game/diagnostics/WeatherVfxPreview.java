package com.alderfall.game;

import com.alderfall.game.render.world.WorldAtmosphereRenderer;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Real-scene weather previews plus a regression check for backwards/stuttering precipitation. */
public final class WeatherVfxPreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length == 0 ? "../asset-review/weather-vfx" : args[0]);
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath());
            try {
                ((Timer) field(GamePanel.class, "timer").get(panel)).stop();
                GameState state = (GameState) field(GamePanel.class, "state").get(panel);
                state.chooseClass("Mage");
                while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
                state.currentMapId = "village_oakhaven";
                state.playerX = 14;
                state.playerY = 16;
                state.worldTick = GameState.TICKS_PER_GAME_DAY / 2;
                state.config.renderQuality = "high";
                state.setZoom(100);
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                WeatherSystem weather = (WeatherSystem) field(GamePanel.class, "weather").get(panel);
                @SuppressWarnings("unchecked")
                Map<WeatherCondition, Double> weights = (Map<WeatherCondition, Double>)
                        field(WeatherSystem.class, "intensities").get(weather);
                WorldAtmosphereRenderer renderer = (WorldAtmosphereRenderer)
                        field(GamePanel.class, "worldAtmosphereRenderer").get(panel);
                for (WeatherCondition condition : WeatherCondition.values()) {
                    weights.clear();
                    weights.put(condition, 1.0);
                    weather.refreshRenderMetrics();
                    field(GamePanel.class, "frame").setInt(panel, 127);
                    BufferedImage scene = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = scene.createGraphics();
                    panel.paint(g);
                    g.dispose();
                    ImageIO.write(scene, "png", output.resolve(condition.name().toLowerCase() + ".png").toFile());
                }
                for (WeatherQuality quality : WeatherQuality.values()) {
                    state.config.weatherQuality = quality.key;
                    checkFall(renderer, false);
                    checkFall(renderer, true);
                }
                System.out.println("Weather previews rendered; rain and snow descend smoothly across cache boundaries at all quality settings.");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                panel.shutdown();
            }
        });
    }

    private static void checkFall(WorldAtmosphereRenderer renderer, boolean snow) {
        double previous = -1;
        for (int frame = 0; frame < 12; frame++) {
            renderer.useContext(new WorldAtmosphereRenderer.RenderContext(frame, 27, 15, 48, 1280, 720));
            BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            if (snow) renderer.drawSnow(g, 1280, 720, 1, false, 1.0);
            else renderer.drawRain(g, 1280, 720, 1, false, 1.0);
            g.dispose();
            double weightedY = 0, weight = 0;
            for (int y = 0; y < 720; y++) for (int x = 0; x < 1280; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                weightedY += y * alpha;
                weight += alpha;
            }
            if (weight == 0) throw new IllegalStateException("Missing precipitation");
            double center = weightedY / weight;
            if (previous >= 0 && (center < previous || center - previous > (snow ? 3 : 20))) {
                throw new IllegalStateException("Precipitation reversed or jumped at frame " + frame);
            }
            previous = center;
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
