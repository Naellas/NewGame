package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Actual renderer captures, with the same building in each regional palette. */
public final class InteriorRenderTest {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length > 0 ? args[0] : "exports/interiors");
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
                panel.setSize(1600, 1100);
                for (String source : List.of("town_briarbridge", "village_oakhaven", "village_snowrest",
                        "village_dunewick", "village_mireford", "city_archive")) {
                    CityBuilding building = state.world.cityBuildings(source).stream()
                            .filter(b -> b.style().contains("inn") || b.style().contains("tavern"))
                            .findFirst().orElse(state.world.cityBuildings(source).get(0));
                    String id = state.world.ensureHouseInterior(source, building.x1(), building.y1(), 13, 8);
                    state.currentMapId = id;
                    TilePoint entry = state.world.interiorEntryPoint(id);
                    state.playerX = entry.x(); state.playerY = entry.y();
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    for (int hour : new int[]{12, 23}) {
                        state.worldTick = 0;
                        while (state.timeOfDayMinutes() != hour * 60) state.worldTick++;
                        BufferedImage target = new BufferedImage(1600, 1100, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = target.createGraphics();
                        panel.paint(g); g.dispose();
                        ImageIO.write(target, "png", output.resolve(source + "-" + hour + ".png").toFile());
                        if (List.of(args).contains("--benchmark")) {
                            long[] samples = new long[30];
                            Graphics2D measure = target.createGraphics();
                            for (int i = -10; i < samples.length; i++) {
                                field("frame").setInt(panel, i + 20);
                                long start = System.nanoTime();
                                panel.paint(measure);
                                if (i >= 0) samples[i] = System.nanoTime() - start;
                            }
                            measure.dispose();
                            java.util.Arrays.sort(samples);
                            System.out.printf(java.util.Locale.ROOT, "%s %02d:00 median=%.1fms p95=%.1fms%n",
                                    source, hour, samples[15] / 1_000_000.0, samples[28] / 1_000_000.0);
                        }
                    }
                    System.out.println(source + " -> " + id + " props=" + state.world.props(id).size());
                }
            } catch (Exception ex) { throw new IllegalStateException(ex); }
            finally { panel.shutdown(); }
        });
    }

    private static Field field(String name) throws Exception {
        Field field = GamePanel.class.getDeclaredField(name); field.setAccessible(true); return field;
    }
}
