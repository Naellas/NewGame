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
        Path output = Path.of(args.length > 0 ? args[0] : "exports/worldgen-preview");
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
                if (args.length > 1 && args[1].equals("roaming")) {
                    previewEvents(panel, state, output);
                    return;
                }
                AssetStore assets = new AssetStore(Path.of("assets"));
                for (String name : List.of("deco_crossing_charter_marker", "deco_crossing_flood_bell")) {
                    if (!assets.hasSprite(name)) throw new IllegalStateException("Missing asset: " + name);
                    BufferedImage source = ImageIO.read(Path.of("assets/environments/locations", name + ".png").toFile());
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

    private static void previewEvents(GamePanel panel, GameState state, Path output) throws Exception {
        state.playerX = 1; state.playerY = 1;
        TilePoint traveler = null;
        for (int y = 30; y < 288 && traveler == null; y++) for (int x = 30; x < 288; x++) {
            if (state.roamingEvents.spawnTravelerAt(state, x, y, 99)) { traveler = new TilePoint(x, y); break; }
        }
        if (traveler == null) throw new AssertionError("No traveler position");
        state.playerX = traveler.x() + 1; state.playerY = traveler.y();
        capture(panel, output.resolve("wounded-traveler.png"));
        state.roamingEvents.reset();
        state.playerX = traveler.x() + 3;
        state.roamingEvents.spawnTrapAt(state, traveler.x(), traveler.y(), 99);
        capture(panel, output.resolve("ground-trap.png"));
        state.roamingEvents.reset();
        boolean fish = false;
        for (int y = 12; y < 288 && !fish; y++) for (int x = 12; x < 288; x++) {
            if (!state.roamingEvents.spawnFishAt(state, x, y, 99)) continue;
            for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int ax = x + step[0], ay = y + step[1];
                if (state.world.isPassable(state.currentMapId, ax, ay)
                        && state.world.waterDepth(state.currentMapId, ax, ay) != WaterDepth.DEEP
                        && PropCollision.clear(state.world, state.currentMapId, ax + .5, ay + .5)) {
                    state.playerX = ax; state.playerY = ay;
                    break;
                }
            }
            capture(panel, output.resolve("fish-run.png"));
            fish = true;
            break;
        }
        if (!fish) throw new AssertionError("No fish position");
        state.roamingEvents.reset();
        state.playerX = 1; state.playerY = 1;
        TilePoint bush = null;
        for (int y = 12; y < 288 && bush == null; y++) for (int x = 12; x < 288; x++) {
            if (state.roamingEvents.spawnAmbushAt(state, x, y, 77)) { bush = new TilePoint(x, y); break; }
        }
        if (bush == null) throw new AssertionError("No ambush road");
        state.playerX = bush.x() - 3; state.playerY = bush.y();
        boolean purse = false;
        for (int dy = -2; dy <= 2 && !purse; dy++) for (int dx = -2; dx <= 2; dx++) {
            if (state.roamingEvents.spawnPurseAt(state, bush.x() + dx, bush.y() + dy, 123)) { purse = true; break; }
        }
        if (!purse) throw new AssertionError("No nearby purse position");
        capture(panel, output.resolve("road-objects.png"));
        state.playerX = bush.x(); state.playerY = bush.y();
        if (!state.roamingEvents.triggerEnteredEvent(state)) throw new AssertionError("Ambush popup missing");
        state.revealActiveDialogueLineInstantly();
        capture(panel, output.resolve("ambush-popup.png"));
        state.closeOverlay();
        state.mode = GameMode.EXPLORE;
        for (WorldProp shrine : state.world.props(state.currentMapId)) {
            state.roamingEvents.reset();
            if (!state.roamingEvents.spawnShrineAt(state, shrine, 0)) continue;
            state.playerX = shrine.x() - 2; state.playerY = shrine.y() + 1;
            field("frame").setInt(panel, 75);
            for (int variant = 0; variant < 3; variant++) {
                state.roamingEvents.reset();
                state.roamingEvents.spawnShrineAt(state, shrine, variant);
                capture(panel, output.resolve("shrine-" + variant + ".png"));
            }
            break;
        }
        RoamingEventWorkflow.openLostSatchelPrompt(state, 123);
        state.revealActiveDialogueLineInstantly();
        capture(panel, output.resolve("deliberate-interaction.png"));
        state.closeOverlay();
        state.roamingEvents.reset();
        for (String id : List.of("greyharbor_cache_run", "camp_ledger")) {
            Quest quest = state.quests.get(id);
            quest.accepted = true;
            if (id.equals("camp_ledger")) quest.stageIndex = 1;
            state.resetQuestMonsterRuntime();
            GameState.QuestObjective objective = state.activeQuestObjectives().stream()
                    .filter(o -> o.questId().equals(id) && !o.markerOnly()).findFirst().orElseThrow();
            state.currentMapId = objective.mapId();
            state.playerX = objective.x() - 1; state.playerY = objective.y() + 1;
            capture(panel, output.resolve(id + ".png"));
        }
        System.out.println("Captured world objects, ambush popup, shrine palettes and active cache/document quests.");
    }

    private static void capture(GamePanel panel, Path output) throws Exception {
        ((CameraController) field("cameraController").get(panel)).resetToPlayer();
        BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); panel.paint(g); g.dispose();
        ImageIO.write(image, "png", output.toFile());
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = GamePanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}

