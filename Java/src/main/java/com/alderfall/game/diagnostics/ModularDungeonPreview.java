package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import com.alderfall.game.map.WorldMap;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Shows modular dungeon artwork at normal scale through the actual game renderer. */
public final class ModularDungeonPreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length > 0 ? args[0] : "exports/modular-dungeons");
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
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                for (String theme : List.of("cave", "crypt", "abandoned_castle", "bandit_camp", "goblin_camp")) {
                    var marker = state.world.adventureMarkers().stream().filter(m -> m.kind().equals(theme)).findFirst().orElseThrow();
                    for (boolean interior : new boolean[]{false, true}) {
                        state.currentMapId = interior ? marker.mapId() : WorldMap.OVERWORLD_ID;
                        var focus = state.world.props(state.currentMapId).stream()
                                .filter(p -> p.asset().startsWith("dungeon_detail_"))
                                .filter(p -> interior || Math.abs(p.x() - marker.x()) + Math.abs(p.y() - marker.y()) < 12)
                                .findFirst().orElseThrow(() -> new IllegalStateException("No modular props: " + theme));
                        state.playerX = focus.x();
                        state.playerY = focus.y() + 2;
                        ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                        var image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                        var graphics = image.createGraphics();
                        panel.paint(graphics);
                        graphics.dispose();
                        Path file = output.resolve(theme + (interior ? "-interior.png" : "-exterior.png"));
                        ImageIO.write(image, "png", file.toFile());
                        System.out.println(file);
                    }
                }
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            } finally { panel.shutdown(); }
        });
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = GamePanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
