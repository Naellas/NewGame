package com.alderfall.game;

import com.alderfall.game.map.WorldMapLabels;
import com.alderfall.game.map.WorldMapViewport;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Crowded-label regression and screenshots of the actual map UI. */
public final class WorldMapReviewTest {
    public static void main(String[] args) throws Exception {
        BufferedImage canvas = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        var g = canvas.createGraphics();
        var labels = new WorldMapLabels(new WorldMapViewport(0, 0, 640, 400, 0, 0, 300, 300));
        Rectangle icon = new Rectangle(290, 190, 20, 20);
        labels.reserve(icon);
        for (int i = 0; i < 60; i++) labels.add("Crowded destination " + i, 300, 200, Color.WHITE, i);
        var placed = labels.draw(g);
        if (placed.isEmpty() || !placed.get(0).text().endsWith("59")) throw new AssertionError("Priority label lost");
        for (int i = 0; i < placed.size(); i++) {
            Rectangle bounds = placed.get(i).bounds();
            if (!new Rectangle(0, 0, 640, 400).contains(bounds) || bounds.intersects(icon)) throw new AssertionError("Label covers marker or edge");
            for (int j = i + 1; j < placed.size(); j++) if (bounds.intersects(placed.get(j).bounds())) throw new AssertionError("Overlapping labels");
        }
        g.dispose();
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath());
            try {
                ((Timer)field("timer").get(panel)).stop();
                GameState state = (GameState)field("state").get(panel);
                state.chooseClass("Mage");
                while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
                state.quests.values().stream().limit(14).forEach(quest -> quest.accepted = true);
                state.mode = GameMode.WORLD_MAP;
                Path output = Path.of("../asset-review/world-map");
                Files.createDirectories(output);
                for (int i = 0; i < 4; i++) {
                    int width = i == 3 ? 1280 : 1920, height = i == 3 ? 720 : 1080;
                    panel.setSize(width, height);
                    field("worldMapZoom").setDouble(panel, i == 0 ? 1 : 1.56);
                    field("worldMapSiteLabels").setBoolean(panel, i != 2);
                    field("worldMapTownLabels").setBoolean(panel, i != 2);
                    if (i == 2) {
                        panel.focusNextWorldMapQuest();
                        if (field("worldMapFocusedQuest").get(panel) == null) throw new AssertionError("Quest focus did not select a destination");
                    }
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    var graphics = image.createGraphics();
                    panel.paint(graphics);
                    graphics.dispose();
                    ImageIO.write(image, "png", output.resolve("map-" + i + ".png").toFile());
                }
                System.out.println("World map checks passed: collision-free labels, priority ordering, four UI renders.");
            } catch (Exception e) { throw new IllegalStateException(e); }
            finally { panel.shutdown(); }
        });
    }
    private static Field field(String name) throws Exception {
        Field field = GamePanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
