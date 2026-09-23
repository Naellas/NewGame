package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import com.alderfall.game.map.WorldMap;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Real-render snapshots for regional settlement silhouettes and complete location blueprints. */
public final class LocationIdentityPreview {
    private LocationIdentityPreview() { }

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length > 0 ? args[0] : "exports/location-identities");
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

                Map<String, TilePoint> captures = new LinkedHashMap<>();
                captures.put("archive-city", new TilePoint(152, 145));
                captures.put("highwall-city", new TilePoint(205, 78));
                captures.put("sanctum-city", new TilePoint(150, 230));
                captures.put("belltower-city", new TilePoint(228, 185));
                addSettlement(captures, state.world, "town-briarbridge", "town_briarbridge");
                addSettlement(captures, state.world, "town-ironvale", "town_ironvale");
                addSettlement(captures, state.world, "town-reedwatch", "town_reedwatch");
                addSettlement(captures, state.world, "town-embermarket", "town_embermarket");
                addSettlement(captures, state.world, "town-northwatch", "town_northwatch");
                addSettlement(captures, state.world, "town-greyharbor", "town_greyharbor");
                addSettlement(captures, state.world, "village-hearth", "village_oakhaven");
                addSettlement(captures, state.world, "village-river", "village_foxbarrow");
                addSettlement(captures, state.world, "village-north", "village_snowrest");
                addSettlement(captures, state.world, "village-sun", "village_dunewick");
                addSettlement(captures, state.world, "village-fen", "village_mireford");
                addSettlement(captures, state.world, "village-freeholds", "village_cairnvale");
                addLocation(captures, state.world, "blackvault-fortress", "abandoned_castle");
                addLocation(captures, state.world, "redcap-camp", "goblin_camp");

                for (var capture : captures.entrySet()) {
                    state.playerX = capture.getValue().x();
                    state.playerY = capture.getValue().y() + 3;
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

    private static void addLocation(Map<String, TilePoint> captures, WorldMap world, String name, String kind) {
        WorldMap.LocationSite site = world.locationSites(kind).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing location kind " + kind));
        captures.put(name, new TilePoint(site.x(), site.y()));
    }

    private static void addSettlement(Map<String, TilePoint> captures, WorldMap world, String name, String id) {
        WorldMap.SettlementSite site = world.settlementSites().stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing settlement " + id));
        captures.put(name, new TilePoint(site.x(), site.y()));
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = GamePanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
