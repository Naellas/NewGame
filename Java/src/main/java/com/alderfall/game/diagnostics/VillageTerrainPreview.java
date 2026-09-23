package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Actual game renders for regional village ground and the beach/shallow-water transition. */
public final class VillageTerrainPreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length > 0 ? args[0] : "exports/village-terrain");
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
                for (String id : new String[]{"village_dunewick", "village_snowrest", "village_mireford", "village_oakhaven", "overworld", "overworld_desert", "overworld_road"}) {
                    boolean overworld = id.startsWith("overworld");
                    state.currentMapId = overworld ? "overworld" : id;
                    state.playerX = id.equals("overworld_desert") ? 102 : id.equals("overworld_road") ? 112 : overworld ? 136 : 17;
                    state.playerY = id.equals("overworld_desert") ? 245 : id.equals("overworld_road") ? 158 : overworld ? 91 : 14;
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    panel.paint(g);
                    g.dispose();
                    ImageIO.write(image, "png", output.resolve(id + ".png").toFile());
                    System.out.println("Rendered " + id);
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
