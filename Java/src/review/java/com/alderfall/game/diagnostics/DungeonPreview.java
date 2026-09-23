package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import com.alderfall.game.map.WorldMap;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Real game-render snapshots for dungeon topology, dressing, stairs, and encounter formations. */
public final class DungeonPreview {
    private DungeonPreview() { }

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length > 0 ? args[0] : "exports/dungeon-layouts");
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath().normalize());
            try {
                ((Timer) field("timer").get(panel)).stop();
                GameState state = (GameState) field("state").get(panel);
                state.chooseClass("Ranger");
                while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
                state.config.renderQuality = "high";
                state.zoom = 75;
                while (state.timeOfDayMinutes() < 720) state.worldTick++;
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);

                Map<String, String> captures = new LinkedHashMap<>();
                captures.put("stonegate-processional", "dungeon_stonegate_1");
                captures.put("stonegate-named-tomb", "dungeon_stonegate_3");
                captures.put("miredepth-galleries", "dungeon_miredepth_2");
                captures.put("blackvault-lower-ward", "dungeon_blackvault_1");
                captures.put("blackvault-crown-tower", "dungeon_blackvault_3");
                captures.put("redcap-warlord-den", "dungeon_redcap_camp_3");

                for (var capture : captures.entrySet()) {
                    String mapId = capture.getValue();
                    state.currentMapId = mapId;
                    WorldMap.DungeonEncounterSlot focus = state.world.dungeonEncounterSlots(mapId).stream()
                            .filter(WorldMap.DungeonEncounterSlot::boss).findFirst()
                            .orElse(state.world.dungeonEncounterSlots(mapId).get(0));
                    state.playerX = focus.x();
                    state.playerY = focus.y();
                    state.resetDungeonMonsterRuntime();
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = image.createGraphics();
                    panel.paint(graphics);
                    graphics.dispose();
                    Path destination = output.resolve(capture.getKey() + ".png");
                    ImageIO.write(image, "png", destination.toFile());
                    System.out.println(destination);
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
}
