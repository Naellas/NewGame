package com.alderfall.game;

import com.alderfall.game.map.DungeonExteriorCatalog;
import com.alderfall.game.map.WorldMap;
import com.alderfall.game.camera.CameraController;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Actual game-renderer contact sheet and individual surface captures. */
public final class DungeonExteriorPreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of("../asset-review/dungeon-exteriors");
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath().normalize());
            try {
                ((Timer) field("timer").get(panel)).stop();
                GameState state = (GameState) field("state").get(panel);
                state.chooseClass("Mage");
                while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
                state.config.renderQuality = "high";
                state.zoom = 100;
                while (state.timeOfDayMinutes() < 720) state.worldTick++;
                state.currentMapId = WorldMap.OVERWORLD_ID;
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                StringBuilder html = new StringBuilder("<!doctype html><meta charset='utf-8'><title>Dungeon exteriors</title><style>body{background:#172021;color:#eee;font:16px system-ui;max-width:1400px;margin:auto}img{width:100%}section{margin:32px 0}</style><h1>Regional dungeon exteriors</h1>");
                for (var spec : DungeonExteriorCatalog.NEW_SITES) {
                    var marker = state.world.adventureMarkers().stream().filter(m -> m.mapId().equals(spec.id())).findFirst().orElseThrow();
                    state.playerX = marker.x(); state.playerY = marker.y() + 3;
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    BufferedImage capture = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = capture.createGraphics(); panel.paint(g); g.dispose();
                    ImageIO.write(capture, "png", output.resolve(spec.id() + ".png").toFile());
                    html.append("<section><h2>").append(spec.label()).append("</h2><p>").append(spec.intent())
                            .append(" · ").append(marker.x()).append(", ").append(marker.y()).append("</p><img src='")
                            .append(spec.id()).append(".png'></section>");
                }
                Files.writeString(output.resolve("index.html"), html);
            } catch (Exception e) { throw new IllegalStateException(e); }
            finally { panel.shutdown(); }
        });
        System.out.println(output.resolve("index.html").toAbsolutePath().normalize());
    }
    private static Field field(String name) throws Exception {
        Field field = GamePanel.class.getDeclaredField(name); field.setAccessible(true); return field;
    }
}
