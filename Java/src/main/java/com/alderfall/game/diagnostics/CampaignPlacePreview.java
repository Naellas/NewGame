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

/** Capture the real game renderer, including permanent markers without an accepted quest. */
public final class CampaignPlacePreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length > 0 ? args[0] : "out-story-refinement/campaign-places");
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
                for (String id : new String[]{"redcap_supply_camp", "sunken_guest_shrine", "oakhaven_orchard", "reedbank_landing", "highwall_watch", "world_map"}) {
                    state.currentMapId = "overworld";
                    TilePoint point = state.world.campaignPlacePoint(id.equals("world_map") ? "sunken_guest_shrine" : id, 0);
                    state.playerX = point.x();
                    state.playerY = point.y();
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    if (id.equals("world_map")) state.mode = GameMode.WORLD_MAP;
                    BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    panel.paint(g);
                    g.dispose();
                    ImageIO.write(image, "png", output.resolve(id + ".png").toFile());
                    System.out.println("Rendered " + id);
                }
                Npc joss = GameData.NPCS.stream().filter(n -> "hay_for_horses".equals(n.questId())).findFirst().orElseThrow();
                state.mode = GameMode.EXPLORE;
                state.currentMapId = joss.mapId();
                TilePoint npcPoint = state.npcPosition(joss);
                state.playerX = npcPoint.x();
                state.playerY = npcPoint.y();
                ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                state.talkToNpc(joss);
                String topic = "Discuss " + state.quests.get("hay_for_horses").title + ".";
                int tries = 0;
                while (!state.activeNpcDialogOptions().contains(topic) && tries++ < 8) {
                    state.revealActiveDialogueLineInstantly();
                    state.selectDialogOption(0);
                }
                state.revealActiveDialogueLineInstantly();
                state.selectDialogOption(state.activeNpcDialogOptions().indexOf(topic));
                for (int page = 1; page <= 8; page++) {
                    state.revealActiveDialogueLineInstantly();
                    BufferedImage shot = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = shot.createGraphics();
                    panel.paint(g);
                    g.dispose();
                    ImageIO.write(shot, "png", output.resolve("joss_" + page + ".png").toFile());
                    if (!state.activeNpcDialogOptions().equals(java.util.List.of("Continue"))) break;
                    state.selectDialogOption(0);
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
