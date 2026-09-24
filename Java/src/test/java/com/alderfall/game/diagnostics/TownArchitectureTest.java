package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import com.alderfall.game.map.WorldMap;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Town sprite coverage and actual renderer selection, with optional game captures. */
public final class TownArchitectureTest {
    private static final List<String> TOWNS = List.of("town_briarbridge", "town_ironvale",
            "town_moonspire", "town_reedwatch", "town_embermarket", "town_northwatch", "town_greyharbor");

    public static void main(String[] args) throws Exception {
        assertSpriteScale();
        AssetCatalog catalog = new AssetCatalog(Path.of("assets"));
        Set<String> all = new HashSet<>();
        for (long seed : new long[]{0, 42, 2026}) {
            WorldMap world = new WorldMap(seed);
            int replacements = 0;
            for (String town : TOWNS) {
                Set<String> used = new HashSet<>();
                for (CityBuilding building : world.cityBuildings(town)) {
                    String id = RegionalSettlementIdentity.townBuildingAsset(town, building);
                    if (RegionalBuildingTypes.type(town, building) != null
                            || !WesternReachFolklore.buildingAsset(town, building).isEmpty()) {
                        require(id.isEmpty(), "Named institution overwritten: " + town + "/" + building.key());
                    }
                    if (id.isEmpty()) continue;
                    require(id.startsWith(town + "_") || building.key().equals("town_service_barn"), "Another town's sprite: " + id);
                    Path path = catalog.findAsset(id);
                    require(path != null, "Missing sprite " + id);
                    BufferedImage image = ImageIO.read(path.toFile());
                    require(image.getColorModel().hasAlpha() && image.getRGB(0, 0) >>> 24 == 0,
                            "Sprite lacks transparent background: " + id);
                    require(!world.cityBuildingDoorTiles(building).isEmpty(), "Missing existing door");
                    used.add(id);
                    all.add(id);
                    replacements++;
                }
                require(used.size() == (town.equals("town_moonspire") ? 17 : 16)
                        && used.contains(town + "_house"), "Incomplete town art: " + town + used);
            }
            for (var site : world.settlementSites()) {
                if (TOWNS.contains(site.id())) continue;
                for (CityBuilding building : world.cityBuildings(site.id())) {
                    require(RegionalSettlementIdentity.townBuildingAsset(site.id(), building).isEmpty(),
                            "Unexpected replacement outside the seven towns: " + site.id());
                }
            }
            System.out.println("seed=" + seed + ": " + replacements + " building assignments, 12 base sprites per town plus three cardinal variants.");
        }
        require(all.size() == 109, "Expected 109 active distinct town sprites: " + all);
        boolean render = List.of(args).contains("--render");
        Path output = Path.of("../asset-review/reviews/town-architecture/2026-09-23-uniform-scale");
        if (render) Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath().normalize());
            try {
                ((Timer) field("timer").get(panel)).stop();
                GameState state = (GameState) field("state").get(panel);
                state.chooseClass("Mage");
                while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
                state.config.renderQuality = "high";
                state.worldTick = GameState.TICKS_PER_GAME_DAY / 2;
                state.zoom = 80;
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                Method modular = GamePanel.class.getDeclaredMethod("buildingSprite", CityBuilding.class, int.class, int.class);
                Method standalone = GamePanel.class.getDeclaredMethod("villageBuildingSpriteForLevel", String.class, int.class, CityBuilding.class, int.class, int.class);
                Method single = GamePanel.class.getDeclaredMethod("usesStandaloneSettlementBuilding", String.class, CityBuilding.class);
                Method modules = GamePanel.class.getDeclaredMethod("buildingModuleCount", CityBuilding.class);
                single.setAccessible(true);
                modules.setAccessible(true);
                Method size = GamePanel.class.getDeclaredMethod("standaloneBuildingTargetSize",
                        CityBuilding.class, int.class, int.class);
                size.setAccessible(true);
                AssetStore store = (AssetStore) field("assets").get(panel);
                modular.setAccessible(true);
                standalone.setAccessible(true);
                for (String town : TOWNS) {
                    state.currentMapId = town;
                    Set<String> captured = new HashSet<>();
                    for (CityBuilding building : state.world.cityBuildings(town)) {
                        String id = RegionalSettlementIdentity.townBuildingAsset(town, building);
                        String selected = (String) standalone.invoke(panel, building.style(), 1, building, 0, 0);
                        if (id.isEmpty()) {
                            int[] dimensions = (int[]) size.invoke(panel, building, building.width() * 48, 48);
                            int[] visible = visibleSize(store.spriteFit(selected, dimensions[0], dimensions[1]));
                            require(Math.abs(visible[0] * visible[1] / (180.0 * 180) - 1) < .04,
                                    "Institution uses a different apparent scale: " + selected);
                            continue;
                        }
                        require((boolean) single.invoke(panel, state.world.kind(town), building),
                                "Whole building routed into repeated modules: " + id);
                        require((int) modules.invoke(panel, building) == 1, "Repeated whole-building sprite: " + id);
                        int[] normal = (int[]) size.invoke(panel, building, building.width() * 48, 48);
                        int[] zoomed = (int[]) size.invoke(panel, building, building.width() * 96, 96);
                        require(Math.abs(zoomed[0] - 2 * normal[0]) <= 1
                                && Math.abs(zoomed[1] - 2 * normal[1]) <= 1, "Zoom scale drift: " + id);
                        BufferedImage fitted = store.spriteFit(id, normal[0], normal[1]);
                        int[] visible = visibleSize(fitted);
                        double expectedSize = TownBuildingArt.apparentSize(id);
                        require(Math.abs(visible[0] * visible[1] / (expectedSize * expectedSize) - 1) < .04,
                                "Visible sprite area differs from its landmark/ordinary tier: " + id);
                        if (TownBuildingArt.landmark(town, building))
                            require(expectedSize > 180, "Landmark has ordinary house scale: " + id);
                        CityBuilding wider = new CityBuilding(building.key(), building.x1(), building.y1(),
                                building.x2() + 5, building.y2() + 2, building.style(), building.palette(), building.facing());
                        int[] wide = (int[]) size.invoke(panel, wider, wider.width() * 48, 48);
                        if (id.equals(standalone.invoke(panel, wider.style(), 1, wider, 0, 0))) {
                            require(java.util.Arrays.equals(normal, wide), "Lot dimensions changed sprite scale: " + id);
                        }
                        require(id.equals(modular.invoke(panel, building, 0, 0)), "Modular renderer missed " + id);
                        require(id.equals(standalone.invoke(panel, building.style(), 1, building, 0, 0)), "Standalone renderer missed " + id);
                        String kind = id.substring(town.length() + 1);
                        if (!render || !captured.add(kind)) continue;
                        TilePoint door = state.world.cityBuildingDoorTiles(building).get(0);
                        state.playerX = door.x(); state.playerY = door.y() + 3;
                        state.status = town.replace('_', ' ') + " - " + kind;
                        ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                        BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                        Graphics2D graphics = image.createGraphics();
                        panel.paint(graphics); graphics.dispose();
                        ImageIO.write(image, "png", output.resolve(town + "-" + kind + ".png").toFile());
                    }
                    if (render) {
                        var area = state.world.area(town);
                        for (int gy = 0; gy < area.height(); gy++) for (int gx = 0; gx < area.width(); gx++) {
                            if (area.tileAt(gx, gy) != Terrain.CITY_GATE) continue;
                            state.playerX = gx; state.playerY = gy;
                            state.status = town + " gateway " + gx + "," + gy;
                            ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                            BufferedImage frame = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                            Graphics2D graphics = frame.createGraphics();
                            panel.paint(graphics); graphics.dispose();
                            ImageIO.write(frame, "png", output.resolve(town + "-gate-" + gx + "-" + gy + ".png").toFile());
                        }
                    }
                }
            } catch (Exception ex) { throw new IllegalStateException(ex); }
            finally { panel.shutdown(); }
        });
        System.out.println("TownArchitectureTest passed: 109 active transparent town sprites, both renderer paths, preserved institution selection.");
    }

    @SuppressWarnings("unchecked")
    private static void assertSpriteScale() throws Exception {
        AssetStore store = new AssetStore(Path.of("assets"));
        Field sources = AssetStore.class.getDeclaredField("sourceCache");
        sources.setAccessible(true);
        var cache = (java.util.Map<String, BufferedImage>) sources.get(store);
        int[][] variants = {{100, 200, 0, 0, 100, 200}, {500, 500, 140, 90, 100, 200},
                {600, 1000, 80, 50, 300, 600}};
        int[] reference = null;
        for (int i = 0; i < variants.length; i++) {
            int[] v = variants[i];
            BufferedImage image = new BufferedImage(v[0], v[1], BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(v[2], v[3], v[4], v[5]);
            graphics.dispose();
            String id = "scale_fixture_" + i;
            cache.put(id, image);
            int[] size = TownBuildingArt.targetSize(store, id, 48);
            if (reference == null) reference = size;
            require(java.util.Arrays.equals(reference, size), "Padding or export resolution changed scale");
            int[] visible = visibleSize(store.spriteFit(id, size[0], size[1]));
            require(Math.abs(visible[0] * visible[1] / (180.0 * 180) - 1) < .04,
                    "Sprite fit did not preserve normalized size");
            require(Math.abs(visible[1] / (double) visible[0] - 2) < .02, "Sprite proportions distorted");
        }
    }

    private static int[] visibleSize(BufferedImage image) {
        int minX = image.getWidth(), minY = image.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            if ((image.getRGB(x, y) >>> 24) <= 8) continue;
            minX = Math.min(minX, x); minY = Math.min(minY, y);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
        }
        return new int[]{maxX - minX + 1, maxY - minY + 1};
    }

    private static Field field(String name) throws Exception {
        Field result = GamePanel.class.getDeclaredField(name); result.setAccessible(true); return result;
    }
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
