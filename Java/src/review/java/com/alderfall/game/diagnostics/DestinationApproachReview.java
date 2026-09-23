package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import javax.imageio.ImageIO;
import javax.swing.*;

public class DestinationApproachReview {
    static Field field(String name) throws Exception {
        Field f = GamePanel.class.getDeclaredField(name); f.setAccessible(true); return f;
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath());
            try {
                ((Timer) field("timer").get(panel)).stop();
                GameState s = (GameState) field("state").get(panel);
                s.chooseClass("Mage");
                while (s.mode == GameMode.STORY_INTRO) s.advanceStoryIntro();
                s.currentMapId = WorldMap.OVERWORLD_ID;
                s.worldTick = GameState.TICKS_PER_GAME_DAY / 2;
                s.zoom = 100; s.config.renderQuality = "high";
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                Path output = Path.of("../asset-review/destination-approaches"); Files.createDirectories(output);
                for (var site : s.world.area(s.currentMapId).destinationApproaches().stream().collect(java.util.stream.Collectors.toMap(a -> a.kind().equals("cache") ? "cache" : a.kind().contains("camp") ? "camp" : a.mapId().equals(s.currentMapId) ? "quest" : "dungeon", a -> a, (a,b) -> a)).values()) {
                    var middle = site.trail().get(site.trail().size() / 2);
                    s.playerX = middle.x(); s.playerY = middle.y();
                    field("freePlayerMapId").set(panel, "");
                    var reset = GamePanel.class.getDeclaredMethod("resetCameraToPlayer"); reset.setAccessible(true); reset.invoke(panel);
                    BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    for (int i = 0; i < 8; i++) panel.paint(g);
                    ImageIO.write(image, "png", output.resolve("scene-" + site.kind() + ".png").toFile());
                    System.out.println(site.label() + " at " + site.destination() + "; trail " + site.trail().size() + " tiles");
                    g.dispose();
                }
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { panel.shutdown(); }
        });
    }
}
