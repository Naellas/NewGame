package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Capture actual regional sites through the game renderer, with the player on a valid floor. */
public final class CavernPreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length == 0 ? "../asset-review/regional-caverns" : args[0]);
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
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                var seen = new HashSet<String>();
                var biomes = new HashSet<String>();
                for (var marker : state.world.adventureMarkers()) {
                    var context = state.world.dungeonContext(marker.mapId());
                    if (context == null || !CavernStyle.natural(context.theme())) continue;
                    String biome = CavernTerrainRenderer.biome(context);
                    biomes.add(biome);
                    String name = marker.mapId().equals("dungeon_redcap_camp_1") ? "redcap" : biome;
                    if (!seen.add(name)) continue;
                    state.currentMapId = marker.mapId();
                    var focus = state.world.dungeonEncounterSlots(marker.mapId()).get(0);
                    state.playerX = focus.x();
                    state.playerY = focus.y();
                    state.resetDungeonMonsterRuntime();
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    var image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    var g = image.createGraphics();
                    panel.paint(g);
                    g.dispose();
                    ImageIO.write(image, "png", output.resolve(name + ".png").toFile());
                    System.out.println(name + ": " + marker.mapId() + " / " + context.region() + " / " + context.exterior());
                }
                if (!biomes.containsAll(java.util.List.of("ice", "moss", "sand", "stone")))
                    throw new IllegalStateException("Missing regional preview: " + biomes);
            } catch (Exception ex) { throw new IllegalStateException(ex); }
            finally { panel.shutdown(); }
        });
    }

    private static Field field(String name) throws Exception {
        Field field = GamePanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
