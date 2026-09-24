package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import com.alderfall.game.map.InteriorLayout;
import com.alderfall.game.map.MapArea;
import com.alderfall.game.map.WorldMap;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Full-shell review images through the actual world renderer, without the HUD covering rooms. */
public final class InteriorDesignPreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length > 0 ? args[0] : "exports/interior-designs");
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath().normalize());
            try {
                ((Timer) field(GamePanel.class, "timer").get(panel)).stop();
                GameState state = (GameState) field(GamePanel.class, "state").get(panel);
                state.chooseClass("Mage");
                while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
                state.config.renderQuality = "high";
                state.zoom = 100;
                field(GamePanel.class, "uiHidden").setBoolean(panel, true);
                while (state.timeOfDayMinutes() != 12 * 60) state.worldTick++;
                Method place = WorldMap.class.getDeclaredMethod("addFurniture", MapArea.class, int.class, int.class, String.class);
                Method render = GamePanel.class.getDeclaredMethod("drawWorld", Graphics2D.class);
                place.setAccessible(true); render.setAccessible(true);
                if (args.length > 1 && args[1].equals("furniture-quest")) {
                    Npc keeper = FurnitureQuestContent.keeper(state.world);
                    if (keeper == null) throw new AssertionError("Missing Oakhaven inn");
                    state.currentMapId = keeper.mapId();
                    Quest quest = state.quests.get(FurnitureQuestContent.ID);
                    quest.accepted = true;
                    for (int stage : new int[]{1, 3, 4, 5}) {
                        quest.stageIndex = Math.min(stage, quest.stages.size() - 1);
                        quest.progress = stage == 5 ? 1 : 0;
                        quest.observedStages.clear();
                        for (int i = 0; i < stage; i++) quest.observedStages.add(quest.stages.get(i).id());
                        Method invalidate = GameState.class.getDeclaredMethod("invalidateQuestObjectiveCache");
                        invalidate.setAccessible(true); invalidate.invoke(state);
                        capture(panel, state, render, output.resolve("furniture-quest-" + stage + ".png"));
                    }
                    System.out.println("Captured furniture quest: ledger, reserve parcel, carried parcel, delivered parcel.");
                    return;
                }
                for (String theme : InteriorLayout.THEMES) {
                    if (args.length > 1 && !theme.equals(args[1])) continue;
                    String source = switch (theme) {
                        case "remembrance_hall", "rescue_lodge" -> "village_snowrest";
                        case "bellhouse", "reedworks" -> "village_mireford";
                        case "caravanserai", "cistern_house" -> "village_dunewick";
                        case "ferry_lodge" -> "town_briarbridge";
                        case "study" -> "city_archive";
                        default -> "village_oakhaven";
                    };
                    for (int seed = 0; seed < 4; seed++) {
                        String id = "house_" + source + "_design_" + theme + "_" + seed;
                        InteriorLayout plan = InteriorLayout.compose(theme, seed, InteriorStyle.forMap(id));
                        state.world.createEditorMap(id, theme, "interior", plan.tiles()[0].length, plan.tiles().length);
                        MapArea area = state.world.area(id);
                        area.interiorRugs.clear();
                        for (int y = 0; y < area.height(); y++) {
                            area.tiles[y] = plan.tiles()[y].clone();
                            for (int x = 0; x < area.width(); x++) if (area.tiles[y][x] == 'z') area.interiorRugs.add(new TilePoint(x, y));
                        }
                        for (WorldProp prop : plan.props()) place.invoke(state.world, area, prop.x(), prop.y(), prop.asset());
                        if (area.props.size() != plan.props().size()) throw new AssertionError("Incomplete preview: " + id);
                        state.currentMapId = id;
                        TilePoint viewpoint = null;
                        if (args.length > 2) {
                            String[] xy = args[2].split(",");
                            viewpoint = new TilePoint(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                        }
                        capture(panel, state, render, output.resolve(theme + "-" + seed + ".png"), viewpoint);
                    }
                    System.out.println("Captured " + theme + ": four footprint seeds.");
                }
            } catch (Exception ex) { throw new IllegalStateException(ex); }
            finally { panel.shutdown(); }
        });
    }

    private static void capture(GamePanel panel, GameState state, Method render, Path output) throws Exception {
        capture(panel, state, render, output, null);
    }

    private static void capture(GamePanel panel, GameState state, Method render, Path output, TilePoint viewpoint) throws Exception {
        MapArea area = state.world.area(state.currentMapId);
        TilePoint entry = state.world.interiorEntryPoint(state.currentMapId);
        if (viewpoint != null) entry = viewpoint;
        state.playerX = entry.x(); state.playerY = entry.y();
        ((CameraController) field(GamePanel.class, "cameraController").get(panel)).resetToPlayer();
        // Leave a tile of headroom and a tile below the shell. The ordinary
        // viewport uses ceil-to-fill sizing, which crops tall rear-wall caps.
        int tile = GameConfig.HEIGHT / (area.height() + 2);
        field(GamePanel.class, "editorTileSize").setInt(panel, tile);
        int width = tile * area.width();
        field(GamePanel.class, "renderWidth").setInt(panel, width);
        BufferedImage image = new BufferedImage(width, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new java.awt.Color(12, 13, 18));
        g.fillRect(0, 0, width, GameConfig.HEIGHT);
        g.translate(0, tile);
        render.invoke(panel, g); g.dispose();
        ImageIO.write(image, "png", output.toFile());
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
}
