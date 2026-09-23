package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import com.alderfall.game.map.WorldMap;
import com.alderfall.game.map.WorldTransition;
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

/** Run from Java/: java -Djava.awt.headless=true -cp out-western-check com.alderfall.game.WesternReachWorldTest [--render] */
public final class WesternReachWorldTest {
    private static final List<String> MAPS = List.of("town_briarbridge", "city_riverside", "village_foxbarrow", "dungeon_redcap_camp_1",
            WesternReachFolklore.GARDEN_ID, WesternReachFolklore.UNDERCROFT_ID);

    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        for (String asset : List.of(WesternReachFolklore.ABBEY, WesternReachFolklore.ORCHARD_HOUSE,
                WesternReachFolklore.BOUNDARY, WesternReachFolklore.CAMP_SUPPLIES,
                WesternReachFolklore.GARDEN_GATE, WesternReachFolklore.SEALED_MIRROR)) {
            require(assets.hasSprite(asset), "Missing asset: " + asset);
            BufferedImage image = ImageIO.read(Path.of("assets/environments/settlements/city/folklore", asset + ".png").toFile());
            require(image.getColorModel().hasAlpha() && (image.getRGB(0, 0) >>> 24) == 0, "Opaque backdrop: " + asset);
        }
        for (long seed : new long[]{0, 42, 1024}) {
            WorldMap world = new WorldMap(seed);
            checkAbbeyRoutes(world);
            require(world.props("town_briarbridge").stream().anyMatch(p -> p.asset().equals("village_prop_bench")), "Missing petition bench");
            require(world.props("city_riverside").stream().anyMatch(p -> p.asset().equals("village_prop_bench")), "Missing Charter Hall bench");
            if (world.hasMap("dungeon_redcap_camp_1")) {
                require(world.props("dungeon_redcap_camp_1").stream().anyMatch(p -> p.asset().equals(WesternReachFolklore.CAMP_SUPPLIES)), "Missing supply shelter");
                require(world.props(WorldMap.OVERWORLD_ID).stream().anyMatch(p -> p.asset().equals(WesternReachFolklore.CAMP_SUPPLIES)), "Missing camp approach shelter");
            }
            for (String id : MAPS) {
                if (!world.hasMap(id)) {
                    continue; // Seeded Redcap camp may not exist if its generator finds no suitable terrain.
                }
                MapArea area = world.area(id);
                List<WorldProp> props = area.props.stream()
                        .filter(p -> !WesternReachFolklore.observation(id, p.asset()).isBlank()).toList();
                require(!props.isEmpty(), "No folklore in " + id);
                Set<TilePoint> reached = reachable(world, id);
                for (WorldProp prop : props) {
                    require(assets.hasSprite(prop.asset()), "Missing prop " + prop.asset());
                    require(area.propsAt(prop.x(), prop.y()).contains(prop), "Prop missing from spatial index");
                    require(reached.stream().anyMatch(p -> Math.abs(p.x() - prop.x()) + Math.abs(p.y() - prop.y()) <= 3),
                            "Inaccessible folklore: " + id + " " + prop);
                    if (prop.asset().startsWith("folklore_")) {
                        require(prop.asset().equals(WesternReachFolklore.GARDEN_GATE)
                                || world.transitionAt(id, prop.x(), prop.y()) == null, "Prop covers transition");
                        require(world.cityBuildingAt(id, prop.x(), prop.y()) == null, "Prop covers building");
                    }
                }
                if (seed == 0) {
                    System.out.println(id + ": " + props);
                }
            }
            for (String id : MAPS.subList(0, 3)) {
                List<CityBuilding> named = world.cityBuildings(id).stream()
                        .filter(b -> !WesternReachFolklore.buildingName(id, b).isEmpty()).toList();
                require(!named.isEmpty(), "Missing authored building in " + id + ": " + world.cityBuildings(id));
                require(world.npcs(id).stream().anyMatch(npc -> npc.dialog().stream().anyMatch(line ->
                        line.contains("Guest Abbey") || line.contains("Charter Hall") || line.contains("Orchard House")
                        || line.contains("court messenger") || line.contains("novice") || line.contains("Merchants"))),
                        "No local folklore voice in " + id);
                Set<TilePoint> reached = reachable(world, id);
                for (CityBuilding building : named) {
                    require(world.cityBuildingDoorTiles(building).stream().anyMatch(p ->
                            reached.contains(new TilePoint(p.x(), p.y() + 1))), "Blocked entrance " + building);
                    TilePoint door = world.cityBuildingDoorTiles(building).get(0);
                    String interior = world.ensureHouseInterior(id, building.x1(), building.y1(), door.x(), door.y() + 1);
                    require(world.label(interior).equals(WesternReachFolklore.buildingName(id, building)), "Interior lost local name");
                    require(!world.npcs(interior).isEmpty(), "Local interior has no hosts");
                    Set<TilePoint> indoorRoutes = reachable(world, interior);
                    for (Npc host : world.npcs(interior)) {
                        require(indoorRoutes.stream().anyMatch(p -> Math.abs(p.x() - host.x()) + Math.abs(p.y() - host.y()) <= 1),
                                "Unreachable host: " + host.name());
                    }
                }
            }
        }
        checkAbbeySaves();
        System.out.println("Western reach asset, placement, dialogue, entrance, orchard routes and interior checks passed for 3 seeds; abbey save/load passed.");
        if (List.of(args).contains("--render")) {
            render();
        }
    }

    private static void checkAbbeyRoutes(WorldMap world) {
        for (String id : List.of(WesternReachFolklore.GARDEN_ID, WesternReachFolklore.UNDERCROFT_ID)) {
            require(world.hasMap(id), "Missing abbey grounds: " + id);
            Set<TilePoint> routes = reachable(world, id);
            MapArea area = world.area(id);
            for (int y = 0; y < area.height(); y++) for (int x = 0; x < area.width(); x++) {
                WorldTransition route = world.transitionAt(id, x, y);
                if (route == null) continue;
                require(routes.contains(new TilePoint(x, y)), "Unreachable abbey exit: " + id);
                require(world.isPassable(route.targetMapId(), route.targetX(), route.targetY()), "Blocked arrival");
                require(world.transitionAt(route.targetMapId(), route.targetX(), route.targetY()) == null,
                        "Arrival immediately enters another transition");
            }
            for (Npc npc : world.npcs(id)) {
                require(routes.stream().anyMatch(p -> Math.abs(p.x() - npc.x()) + Math.abs(p.y() - npc.y()) <= 1),
                        "Unreachable abbey resident: " + npc.name());
            }
        }
        WorldProp gate = world.props("town_briarbridge").stream()
                .filter(p -> p.asset().equals(WesternReachFolklore.GARDEN_GATE)).findFirst().orElseThrow();
        require(reachable(world, "town_briarbridge").contains(new TilePoint(gate.x(), gate.y())), "Unreachable town gate");
        WorldTransition entrance = world.transitionAt("town_briarbridge", gate.x(), gate.y());
        require(entrance != null && entrance.targetMapId().equals(WesternReachFolklore.GARDEN_ID), "Gate has no orchard route");
        require(world.isPassable(entrance.targetMapId(), entrance.targetX(), entrance.targetY()), "Blocked orchard arrival");
        require(!world.isPassable(WesternReachFolklore.UNDERCROFT_ID, 9, 4), "Sealed mirror is passable");
        require(world.transitionAt(WesternReachFolklore.UNDERCROFT_ID, 9, 4) == null, "Sealed mirror is a portal");
    }

    private static void checkAbbeySaves() throws Exception {
        Path temporary = Files.createTempDirectory("alderfall-western-save-");
        try {
            SaveSystem saves = new SaveSystem(temporary);
            GameState state = new GameState(GameConfig.load(temporary));
            state.chooseClass("Mage");
            while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
            for (String id : List.of(WesternReachFolklore.GARDEN_ID, WesternReachFolklore.UNDERCROFT_ID)) {
                state.currentMapId = id;
                state.playerX = id.equals(WesternReachFolklore.GARDEN_ID) ? 13 : 9;
                state.playerY = id.equals(WesternReachFolklore.GARDEN_ID) ? 19 : 11;
                saves.save(state, id);
                String saveId = saves.listSaves().stream().filter(s -> s.saveName().equals(id)).findFirst().orElseThrow().saveId();
                GameState loaded = new GameState(GameConfig.load(temporary));
                require(saves.load(loaded, saveId), "Abbey save could not load");
                // Existing save policy resumes interior saves just outside their exit.
                int expectedY = id.equals(WesternReachFolklore.GARDEN_ID) ? 19 : 5;
                require(loaded.currentMapId.equals(WesternReachFolklore.GARDEN_ID)
                                && loaded.playerX == 13 && loaded.playerY == expectedY,
                        "Abbey save did not restore its safe orchard position");
            }
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
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
        Path output = Path.of("../asset-review/western-reach");
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
                state.worldTick = GameState.TICKS_PER_GAME_DAY / 2;
                state.zoom = 100;
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                for (String id : MAPS) {
                    if (!state.world.hasMap(id)) {
                        continue;
                    }
                    WorldProp focus = state.world.area(id).props.stream()
                            .filter(p -> !WesternReachFolklore.observation(id, p.asset()).isBlank())
                            .sorted(java.util.Comparator.comparingInt(p -> p.asset().equals(WesternReachFolklore.CAMP_SUPPLIES) ? 0 : 1))
                            .findFirst().orElseThrow();
                    state.currentMapId = id;
                    state.playerX = focus.x();
                    state.playerY = focus.y() + 2;
                    state.status = WesternReachFolklore.observation(id, focus.asset());
                    if (id.equals(WesternReachFolklore.GARDEN_ID)) {
                        state.playerX = 13;
                        state.playerY = 14;
                        state.status = "Briarbridge Guest Orchard: journey trees, shared food and the petition walk.";
                    }
                    CityBuilding building = state.world.cityBuildings(id).stream()
                            .filter(b -> !WesternReachFolklore.buildingAsset(id, b).isEmpty()).findFirst().orElse(null);
                    if (building != null) {
                        TilePoint door = state.world.cityBuildingDoorTiles(building).get(0);
                        state.playerX = door.x();
                        state.playerY = door.y() + 1;
                        state.status = WesternReachFolklore.buildingName(id, building);
                    }
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
