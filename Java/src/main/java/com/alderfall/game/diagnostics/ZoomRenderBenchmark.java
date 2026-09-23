package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Locale;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Measures the first and immediately repeated frame at each gameplay zoom level. */
public final class ZoomRenderBenchmark {
    private ZoomRenderBenchmark() { }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath().normalize());
            try {
                ((Timer) field("timer").get(panel)).stop();
                GameState state = (GameState) field("state").get(panel);
                Field frame = field("frame");
                state.chooseClass("Mage");
                while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
                state.config.renderQuality = "high";
                state.currentMapId = WorldMap.OVERWORLD_ID;
                state.playerX = WorldMap.START_POSITION.x();
                state.playerY = WorldMap.START_POSITION.y();
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                BufferedImage target = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = target.createGraphics();
                try {
                    state.setZoom(100);
                    for (int i = 0; i < 20; i++) {
                        frame.setInt(panel, i);
                        panel.paint(graphics);
                    }
                    int index = 20;
                    for (int zoom : new int[]{110, 120, 110, 100, 90, 80, 90, 100}) {
                        state.setZoom(zoom);
                        frame.setInt(panel, index++);
                        double first = renderMillis(panel, graphics);
                        frame.setInt(panel, index++);
                        double repeated = renderMillis(panel, graphics);
                        System.out.printf(Locale.ROOT, "zoom=%d first=%.2fms repeated=%.2fms%n", zoom, first, repeated);
                    }
                } finally {
                    graphics.dispose();
                }
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            } finally {
                panel.shutdown();
            }
        });
    }

    private static double renderMillis(GamePanel panel, Graphics2D graphics) {
        long started = System.nanoTime();
        panel.paint(graphics);
        return (System.nanoTime() - started) / 1_000_000.0;
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = GamePanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
