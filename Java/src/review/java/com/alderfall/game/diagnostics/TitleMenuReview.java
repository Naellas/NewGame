package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Exports the actual menu at common window sizes for visual review. */
public final class TitleMenuReview {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]).toAbsolutePath();
        Path output = root.getParent().resolve("asset-review/title-screen");
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            try {
                GamePanel panel = new GamePanel(root);
                Field timer = GamePanel.class.getDeclaredField("timer");
                timer.setAccessible(true);
                ((Timer) timer.get(panel)).stop();
                for (int[] size : new int[][]{{1920, 1080}, {1440, 900}, {1280, 720}}) {
                    panel.setSize(size[0], size[1]);
                    BufferedImage image = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    panel.paint(g);
                    g.dispose();
                    ImageIO.write(image, "png", output.resolve("menu-" + size[0] + "x" + size[1] + ".png").toFile());
                }
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        System.out.println("Title menu review exported to " + output);
        System.exit(0);
    }
}
