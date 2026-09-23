package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import com.alderfall.game.map.WorldMap;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Run from Java/: java -Djava.awt.headless=true -cp out-folklore-check com.alderfall.game.FolkloreWorldTest [--render] */
public final class FolkloreWorldTest {
    private static final List<String> MAPS = List.of(WorldMap.PLAYER_VILLAGE_ID, "village_oakhaven",
            "village_elderford", "city_archive", "dungeon_redcap_camp_1");

    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        for (String asset : List.of(FolkloreContent.HEARTH, FolkloreContent.SEEDHOUSE, FolkloreContent.BOUNDARY)) {
            require(assets.hasSprite(asset), "Missing asset: " + asset);
            BufferedImage image = ImageIO.read(Path.of("assets/environments/settlements/city/folklore", asset + ".png").toFile());
            require(image.getColorModel().hasAlpha() && (image.getRGB(0, 0) >>> 24) == 0, "Opaque backdrop: " + asset);
        }
        for (long seed : new long[]{0, 42, 1024}) {
            WorldMap world = new WorldMap(seed);
            require(world.cityBuildings("village_oakhaven").stream().anyMatch(b ->
                            FolkloreContent.SEEDHOUSE.equals(HearthlandsFolklore.buildingAsset("village_oakhaven", b))),
                    "Seedhouse building was not placed, seed=" + seed);
            for (String id : MAPS) {
                if (!world.hasMap(id)) {
                    continue; // Seeded Redcap camp may not exist if its generator finds no suitable terrain.
                }
                MapArea area = world.area(id);
                List<WorldProp> props = area.props.stream()
                        .filter(p -> !FolkloreContent.observation(id, p.asset()).isBlank()).toList();
                require(!props.isEmpty(), "No folklore in " + id);
                Set<TilePoint> reached = reachable(world, id);
                for (WorldProp prop : props) {
                    require(assets.hasSprite(prop.asset()), "Missing prop " + prop.asset());
                    require(area.propsAt(prop.x(), prop.y()).contains(prop), "Prop missing from spatial index");
                    require(reached.stream().anyMatch(p -> Math.abs(p.x() - prop.x()) + Math.abs(p.y() - prop.y()) <= 3),
                            "Inaccessible folklore: " + id + " " + prop);
                    if (prop.asset().startsWith("folklore_")) {
                        require(world.transitionAt(id, prop.x(), prop.y()) == null, "Prop covers transition");
                        require(world.cityBuildingAt(id, prop.x(), prop.y()) == null, "Prop covers building");
                    }
                }
                if (seed == 0) {
                    System.out.println(id + ": " + props);
                }
            }
            WorldProp hearth = world.area(WorldMap.PLAYER_VILLAGE_ID).props.stream()
                    .filter(p -> p.asset().equals(FolkloreContent.HEARTH) || p.asset().equals("village_prop_clay_oven"))
                    .findFirst().orElseThrow();
            world.setPlayerVillageStage(2);
            require(world.area(WorldMap.PLAYER_VILLAGE_ID).props.stream().filter(p -> p.asset().equals(hearth.asset())).count() == 1,
                    "Expansion lost or duplicated shared hearth");
            world.clearPlayerVillageCustomizations();
            require(world.area(WorldMap.PLAYER_VILLAGE_ID).props.contains(hearth), "Camp reset lost shared hearth");
        }
        System.out.println("Folklore world checks passed for 3 seeds.");
        if (List.of(args).contains("--render")) {
            render();
        }
    }

    private static Set<TilePoint> reachable(WorldMap world, String id) {
        MapArea area = world.area(id);
        TilePoint start = null;
        for (int y = 0; y < area.height() && start == null; y++) {
            for (int x = 0; x < area.width(); x++) {
                if (world.transitionAt(id, x, y) != null && world.isPassable(id, x, y)) {
                    start = new TilePoint(x, y);
                    break;
                }
            }
        }
        require(start != null, "No reachable entrance for " + id);
        Set<TilePoint> reached = new HashSet<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        queue.add(start);
        reached.add(start);
        while (!queue.isEmpty()) {
            TilePoint p = queue.remove();
            for (TilePoint next : List.of(new TilePoint(p.x() - 1, p.y()), new TilePoint(p.x() + 1, p.y()),
                    new TilePoint(p.x(), p.y() - 1), new TilePoint(p.x(), p.y() + 1))) {
                if (world.isPassable(id, next.x(), next.y()) && reached.add(next)) {
                    queue.add(next);
                }
            }
        }
        return reached;
    }

    private static void render() throws Exception {
        Path output = Path.of("../asset-review/folklore");
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath().normalize());
            try {
                ((Timer) field("timer").get(panel)).stop();
                GameState state = (GameState) field("state").get(panel);
                state.chooseClass("Mage");
                while (state.mode == GameMode.STORY_INTRO) {
                    state.advanceStoryIntro();
                }
                state.config.renderQuality = "high";
                state.zoom = 100;
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                for (String id : MAPS) {
                    if (!state.world.hasMap(id)) {
                        continue;
                    }
                    WorldProp focus = state.world.area(id).props.stream()
                            .filter(p -> p.asset().startsWith("folklore_") || !FolkloreContent.observation(id, p.asset()).isBlank())
                            .sorted(java.util.Comparator.comparingInt(p -> p.asset().equals(FolkloreContent.SEEDHOUSE) ? 0
                                    : p.asset().equals(FolkloreContent.HEARTH) ? 1 : 2))
                            .findFirst().orElseThrow();
                    state.currentMapId = id;
                    state.playerX = focus.x();
                    state.playerY = focus.y() + 2;
                    state.status = FolkloreContent.observation(id, focus.asset());
                    BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    panel.paint(g);
                    g.dispose();
                    ImageIO.write(image, "png", output.resolve(id + ".png").toFile());
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

    private static void require(boolean passed, String message) {
        if (!passed) {
            throw new IllegalStateException(message);
        }
    }
}
