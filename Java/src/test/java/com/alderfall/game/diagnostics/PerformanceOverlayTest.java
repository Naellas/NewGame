package com.alderfall.game;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class PerformanceOverlayTest {
    public static void main(String[] args) throws Exception {
        PerformanceOverlay overlay = new PerformanceOverlay();
        check(!overlay.enabled(), "default off");
        overlay.cycle();
        overlay.beginFrame(1200, 800);
        for (int i = 0; i <= 10; i++) overlay.finishFrame(1_000_000_000L + i * 50_000_000L, 12_000_000L, Map.of());
        check(Math.abs(overlay.fps() - 20) < 0.001, "FPS uses paint intervals, not render duration");
        overlay.cycle();
        overlay.beginFrame(1200, 800);
        overlay.addRegion(new Rectangle(-50, -50, 250, 250), 1000);
        check(Math.abs(overlay.sampledNanos() - 1000) < .001, "clipped regional costs conserved across cells");
        overlay.addRegion(new Rectangle(1300, 900, 10, 10), 1000);
        check(Math.abs(overlay.sampledNanos() - 1000) < .001, "offscreen work excluded");
        overlay.beginFrame(640, 480);
        check(overlay.sampledNanos() == 0, "resize / new frame clears heat");
        overlay.cycle();
        overlay.addRegion(new Rectangle(0, 0, 50, 50), 1000);
        check(!overlay.enabled() && overlay.sampledNanos() == 0, "disabled collection is inert");
        RenderMetrics metrics = RenderMetrics.create();
        metrics.setOverlayEnabled(true);
        metrics.record("probe", metrics.start());
        check(metrics.takeOverlayTimings().containsKey("probe"), "live timings without console profiling");
        check(metrics.takeOverlayTimings().isEmpty(), "timings consumed per paint");
        SwingUtilities.invokeAndWait(PerformanceOverlayTest::renderGame);
        System.out.println("Performance overlay passed: FPS, bounds, reset, metrics and live panel rendering.");
    }

    private static void renderGame() {
        GamePanel panel = new GamePanel(Path.of("").toAbsolutePath());
        try {
            Field timer = GamePanel.class.getDeclaredField("timer");
            timer.setAccessible(true);
            ((Timer) timer.get(panel)).stop();
            panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
            GameKeyboardController keyboard = new GameKeyboardController(panel);
            KeyEvent f4 = new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_F4, KeyEvent.CHAR_UNDEFINED);
            keyboard.keyPressed(f4);
            keyboard.keyPressed(f4);
            check(panel.performanceOverlay.enabled() && !panel.performanceOverlay.heatmap(), "F4 global and repeat-safe");
            keyboard.keyReleased(new KeyEvent(panel, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_F4, KeyEvent.CHAR_UNDEFINED));
            keyboard.keyPressed(f4);
            check(panel.performanceOverlay.heatmap(), "F4 heatmap mode");
            panel.state.chooseClass("Mage");
            while (panel.state.mode == GameMode.STORY_INTRO) panel.state.advanceStoryIntro();
            BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics();
            panel.paint(g);
            check(panel.performanceOverlay.sampledNanos() > 0, "real renderer contributes heat samples");
            Path capture = Path.of("temp/performance-overlay/heatmap.png");
            Files.createDirectories(capture.getParent());
            ImageIO.write(image, "png", capture.toFile());
            panel.cyclePerformanceOverlay();
            panel.paint(g);
            check(panel.performanceOverlay.sampledNanos() == 0, "normal rendering disables sampling");
            g.dispose();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            panel.shutdown();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
