package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
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

/** Renders the generated crossing assets in their actual overworld surroundings. */
public final class WorldGenerationPreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of("exports/worldgen-preview");
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
                AssetStore assets = new AssetStore(Path.of("assets"));
                for (String name : List.of("deco_crossing_charter_marker", "deco_crossing_flood_bell")) {
                    if (!assets.hasSprite(name)) throw new IllegalStateException("Missing asset: " + name);
                    BufferedImage source = ImageIO.read(Path.of("assets/locations", name + ".png").toFile());
                    if (!source.getColorModel().hasAlpha() || (source.getRGB(0, 0) >>> 24) != 0) {
                        throw new IllegalStateException("Crossing sprite lacks transparency: " + name);
                    }
                    WorldProp focus = state.world.props(WorldMap.OVERWORLD_ID).stream()
                            .filter(prop -> prop.asset().equals(name))
                            .sorted(java.util.Comparator.comparingInt(prop ->
                                    state.world.tileAt(prop.x(), prop.y()) == 'n' ? 1 : 0))
                            .findFirst().orElseThrow();
                    state.playerX = focus.x();
                    state.playerY = focus.y();
                    for (int[] offset : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
                        int x = focus.x() + offset[0], y = focus.y() + offset[1];
                        if (state.world.isPassable(WorldMap.OVERWORLD_ID, x, y)) {
                            state.playerX = x;
                            state.playerY = y;
                            break;
                        }
                    }
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = image.createGraphics();
                    panel.paint(graphics);
                    graphics.dispose();
                    Path destination = output.resolve(name + ".png");
                    ImageIO.write(image, "png", destination.toFile());
                    System.out.println(destination + " at " + focus.x() + "," + focus.y());
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
