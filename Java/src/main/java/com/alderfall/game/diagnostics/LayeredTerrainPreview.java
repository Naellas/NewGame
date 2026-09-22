package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import com.alderfall.game.map.WorldMap;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Actual game renders of the continuous terrain and location footprints. */
public final class LayeredTerrainPreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length > 1 ? args[1] : "exports/layered-terrain");
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
                ArrayList<Scene> scenes = new ArrayList<>();
                scenes.add(new Scene("shoreline", 123, 99));
                scenes.add(new Scene("snow-edge", 123, 91));
                WorldMap.GroundRegion camp = state.world.groundRegions().stream()
                        .filter(r -> r.kind().equals("goblin_camp"))
                        .min(java.util.Comparator.comparingInt(r -> Math.abs(r.x() - 160) + Math.abs(r.y() - 145)))
                        .orElseThrow();
                scenes.add(new Scene("camp", camp.x(), camp.y()));
                if (args.length > 0 && args[0].equals("--props")) {
                    state.world.props(state.currentMapId).stream().filter(p ->
                            Math.abs(p.x() - camp.x()) < 4 && Math.abs(p.y() - camp.y()) < 4)
                            .forEach(p -> System.out.printf("%d,%d %s coverage=%.2f%n", p.x(), p.y(), p.asset(),
                                    state.world.campGroundCoverage(p.x() + 0.5, p.y() + 0.5)));
                }
                for (Scene scene : scenes) {
                    state.playerX = scene.x;
                    state.playerY = scene.y;
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    long started = System.nanoTime();
                    panel.paint(g);
                    g.dispose();
                    ImageIO.write(image, "png", output.resolve(scene.name + ".png").toFile());
                    System.out.printf("%s (%d,%d): first frame %.0fms%n", scene.name, scene.x, scene.y,
                            (System.nanoTime() - started) / 1_000_000.0);
                }
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            } finally {
                panel.shutdown();
            }
        });
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = GamePanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    private record Scene(String name, int x, int y) { }
}
