package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import com.alderfall.game.map.WorldMap;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Actual game renders of the continuous terrain and location footprints. */
public final class LayeredTerrainPreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length > 1 ? args[1] : "exports/layered-terrain");
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
                if (args.length > 0 && args[0].equals("--biomes")) {
                    previewBiomes(state, output);
                    return;
                }
                ArrayList<Scene> scenes = new ArrayList<>();
                scenes.add(new Scene("shoreline", 123, 99));
                scenes.add(new Scene("snow-edge", 123, 91));
                if (args.length > 0 && args[0].equals("--water")) {
                    Scene wading = null;
                    int distance = Integer.MAX_VALUE;
                    for (int y = 90; y < 110; y++) for (int x = 120; x < 140; x++) {
                        int next = Math.abs(x - 125) + Math.abs(y - 99);
                        if (state.world.waterDepth(state.currentMapId, x, y) == WaterDepth.WADING && next < distance) {
                            wading = new Scene("wading", x, y);
                            distance = next;
                        }
                    }
                    if (wading == null) throw new IllegalStateException("No wading shelf in review scene");
                    scenes.add(wading);
                }
                WorldMap.GroundRegion camp = state.world.groundRegions().stream()
                        .filter(r -> r.kind().equals("goblin_camp"))
                        .min(java.util.Comparator.comparingInt(r -> Math.abs(r.x() - 160) + Math.abs(r.y() - 145)))
                        .orElseThrow();
                scenes.add(new Scene("camp", camp.x(), camp.y()));
                if (args.length > 0 && args[0].equals("--props")) {
                    state.world.props(state.currentMapId).stream().filter(p ->
                            Math.abs(p.x() - camp.x()) < 4 && Math.abs(p.y() - camp.y()) < 4)
                            .forEach(p -> System.out.printf("%d,%d %s coverage=%.2f%n", p.x(), p.y(), p.asset(),
                                    state.world.campGroundCoverage(p.x() + 0.5, p.y() + 0.5)));
                }
                if (args.length > 0 && args[0].equals("--swamp")) {
                    scenes.clear();
                    scenes.add(new Scene("swamp", 229, 175));
                    scenes.add(new Scene("swamp-banks", 235, 163));
                }
                for (Scene scene : scenes) {
                    state.playerX = scene.x;
                    state.playerY = scene.y;
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    long started = System.nanoTime();
                    panel.paint(g);
                    g.dispose();
                    ImageIO.write(image, "png", output.resolve(scene.name + ".png").toFile());
                    System.out.printf("%s (%d,%d): first frame %.0fms%n", scene.name, scene.x, scene.y,
                            (System.nanoTime() - started) / 1_000_000.0);
                }
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            } finally {
                panel.shutdown();
            }
        });
    }

    private static void previewBiomes(GameState state, Path output) throws Exception {
        var painter = (WorldRenderer.TerrainPainter) java.lang.reflect.Proxy.newProxyInstance(
                WorldRenderer.TerrainPainter.class.getClassLoader(), new Class<?>[]{WorldRenderer.TerrainPainter.class},
                (proxy, method, values) -> switch (method.getName()) {
                    case "visibleTerrainTile" -> values[0];
                    case "terrainImageName" -> Terrain.assetName((char) values[0]);
                    default -> null;
                });
        var renderer = new LayeredTerrainRenderer(new AssetStore(Path.of("assets")));
        state.currentMapId = state.world.createEditorMap("editor_surface_review", "Surface review", "overworld", 24, 24);
        var area = state.world.area(state.currentMapId);
        BufferedImage sheet = new BufferedImage(1536, 544, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        char[] materials = "gfnsbvPm".toCharArray();
        for (int i = 0; i < materials.length; i++) {
            char material = materials[i];
            area.fillTiles(0, 0, 23, 23, material);
            int left = i % 4 * 384, top = i / 4 * 272;
            for (int y = 0; y < 5; y++) for (int x = 0; x < 8; x++)
                g.drawImage(renderer.tile(state, painter, x + 4, y + 5, 48).image(), left + x * 48, top + 32 + y * 48, null);
            g.setColor(java.awt.Color.WHITE); g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
            g.drawString(Terrain.assetName(material) + " / " + LayeredTerrainRenderer.naturalVariantCount(material) + " styles", left + 8, top + 22);
        }
        g.dispose();
        ImageIO.write(sheet, "png", output.resolve("biome-surfaces.png").toFile());
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = GamePanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    private record Scene(String name, int x, int y) { }
}
