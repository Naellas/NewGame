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
                        TilePoint entry = state.world.interiorEntryPoint(id);
                        state.playerX = entry.x(); state.playerY = entry.y();
                        ((CameraController) field(GamePanel.class, "cameraController").get(panel)).resetToPlayer();
                        int tile = (int) Math.ceil(GameConfig.HEIGHT / (double) area.height());
                        int width = tile * area.width();
                        // Set the review viewport to the map's aspect ratio, using the same terrain,
                        // furniture, occlusion and lighting pipeline as interactive play.
                        field(GamePanel.class, "renderWidth").setInt(panel, width);
                        BufferedImage image = new BufferedImage(width, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = image.createGraphics();
                        render.invoke(panel, g); g.dispose();
                        ImageIO.write(image, "png", output.resolve(theme + "-" + seed + ".png").toFile());
                    }
                    System.out.println("Captured " + theme + ": four footprint seeds.");
                }
            } catch (Exception ex) { throw new IllegalStateException(ex); }
            finally { panel.shutdown(); }
        });
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
}
