package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import javax.imageio.ImageIO;
import javax.swing.*;

public class NatureSpaceReview {
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
                Path output = Path.of(args.length > 0 ? args[0] : "temp/nature-spaces/captures"); Files.createDirectories(output);
                if (args.length > 1 && args[1].equals("groundcover")) {
                    s.zoom = 150;
                    for (char ground : new char[]{'n', 'g'}) {
                        TilePoint best = null;
                        long bestScore = -1;
                        for (int y = 12; y < WorldMap.ROWS - 12; y += 5) for (int x = 12; x < WorldMap.COLS - 12; x += 5) {
                            if (s.world.tileAt(s.currentMapId, x, y) != ground) continue;
                            var props = s.world.propsInBounds(s.currentMapId, x - 5, y - 4, x + 5, y + 4);
                            long score = props.stream().filter(p -> p.visualSlot() >= 0).count();
                            if (score > bestScore) { bestScore = score; best = new TilePoint(x, y); }
                        }
                        if (best == null) throw new IllegalStateException("No cover sample");
                        s.playerX = best.x(); s.playerY = best.y();
                        field("freePlayerMapId").set(panel, "");
                        var reset = GamePanel.class.getDeclaredMethod("resetCameraToPlayer"); reset.setAccessible(true); reset.invoke(panel);
                        BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = image.createGraphics(); panel.paint(g); g.dispose();
                        ImageIO.write(image, "png", output.resolve("groundcover-" + ground + ".png").toFile());
                        System.out.println("Ground cover " + ground + " at " + best + ", details=" + bestScore);
                    }
                    return;
                }
                for (var site : s.world.area(s.currentMapId).natureSites().stream().collect(java.util.stream.Collectors.toMap(a -> a.terrain(), a -> a, (a,b) -> a)).values()) {
                    s.playerX = site.x(); s.playerY = site.y() + 3;
                    field("freePlayerMapId").set(panel, "");
                    var reset = GamePanel.class.getDeclaredMethod("resetCameraToPlayer"); reset.setAccessible(true); reset.invoke(panel);
                    BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    for (int i = 0; i < 8; i++) panel.paint(g);
                    ImageIO.write(image, "png", output.resolve("scene-" + com.alderfall.game.map.NatureSiteGenerator.biome(site.terrain()) + ".png").toFile());
                    System.out.println("Scene " + site.terrain() + " at " + site.x() + "," + site.y());
                    g.dispose();
                }
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { panel.shutdown(); }
        });
    }
}
