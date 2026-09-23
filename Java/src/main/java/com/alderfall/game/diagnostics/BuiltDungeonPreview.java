package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Real renderer captures of every constructed dungeon style and its playable contents. */
public final class BuiltDungeonPreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length > 0 ? args[0] : "../asset-review/built-dungeons");
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
                for (String theme : List.of("crypt", "prison", "sewer", "bandit_camp", "abandoned_castle")) {
                    var marker = state.world.adventureMarkers().stream().filter(m -> m.kind().equals(theme)).findFirst().orElseThrow();
                    for (int floor : theme.equals("abandoned_castle") ? new int[]{1,3} : new int[]{1}) {
                        state.currentMapId = marker.mapId().substring(0,marker.mapId().lastIndexOf('_')+1)+floor;
                        var focus = state.world.dungeonEncounterSlots(state.currentMapId).get(0);
                        state.playerX=focus.x(); state.playerY=focus.y();
                        state.resetDungeonMonsterRuntime();
                        ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                        var image = new BufferedImage(GameConfig.WIDTH,GameConfig.HEIGHT,BufferedImage.TYPE_INT_RGB);
                        var g=image.createGraphics(); panel.paint(g); g.dispose();
                        String name = theme.equals("abandoned_castle") ? (floor==1 ? "gothic" : "arcane") : theme;
                        ImageIO.write(image,"png",output.resolve(name+".png").toFile());
                        System.out.println(name+": "+state.currentMapId);
                    }
                }
            } catch(Exception e) { throw new IllegalStateException(e); }
            finally { panel.shutdown(); }
        });
    }
    private static Field field(String name) throws Exception {
        Field field=GamePanel.class.getDeclaredField(name); field.setAccessible(true); return field;
    }
}
