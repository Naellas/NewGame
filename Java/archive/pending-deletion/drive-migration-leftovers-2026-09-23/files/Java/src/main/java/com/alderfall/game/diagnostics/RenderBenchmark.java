package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Repeatable software-rendering benchmark; does not measure display/GPU presentation. */
public final class RenderBenchmark {
    private RenderBenchmark() { }

    public static void main(String[] args) throws Exception {
        int samples = args.length > 0 ? Integer.parseInt(args[0]) : 240;
        if (samples < 1) {
            throw new IllegalArgumentException("Sample count must be positive");
        }
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath().normalize());
            try {
                ((Timer) field("timer").get(panel)).stop();
                GameState state = (GameState) field("state").get(panel);
                state.chooseClass("Mage");
                while (state.mode == GameMode.STORY_INTRO) {
                    state.advanceStoryIntro();
                }
                state.config.renderQuality = "high";
                state.zoom = 100;
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                BufferedImage target = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = target.createGraphics();
                try {
                    run(panel, graphics, samples, "starting-village");
                    state.currentMapId = com.alderfall.game.map.WorldMap.OVERWORLD_ID;
                    state.playerX = com.alderfall.game.map.WorldMap.START_POSITION.x();
                    state.playerY = com.alderfall.game.map.WorldMap.START_POSITION.y();
                    run(panel, graphics, samples, "overworld");
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

    private static void run(GamePanel panel, Graphics2D graphics, int samples, String scene)
            throws ReflectiveOperationException {
        Field frame = field("frame");
        long[] times = new long[samples];
        for (int i = -120; i < samples; i++) {
            frame.setInt(panel, i + 120);
            long start = System.nanoTime();
            panel.paint(graphics);
            if (i >= 0) {
                times[i] = System.nanoTime() - start;
            }
        }
        double mean = Arrays.stream(times).average().orElse(0) / 1_000_000.0;
        Arrays.sort(times);
        System.out.printf(Locale.ROOT, "%s samples=%d mean=%.2fms median=%.2fms p95=%.2fms%n",
                scene, samples, mean, times[samples / 2] / 1_000_000.0,
                times[(int) Math.ceil(samples * 0.95) - 1] / 1_000_000.0);
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = GamePanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
