package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import com.alderfall.game.map.InteriorLayout;
import com.alderfall.game.map.WorldMap;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Building art, functional rooms, resident access and actual entry/exit integration. */
public final class RegionalBuildingsTest {
    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        if (!List.of(args).contains("--logic-only")) for (var type : RegionalBuildingTypes.Type.values()) {
            require(assets.hasSprite(type.asset), "Missing building asset " + type.asset);
            BufferedImage image = ImageIO.read(Path.of("assets/environments/settlements/city/regional", type.asset + ".png").toFile());
            require(image.getColorModel().hasAlpha() && image.getRGB(0, 0) >>> 24 == 0, "Opaque background " + type.asset);
        }
        for (long seed : new long[]{0, 42}) {
            WorldMap world = new WorldMap(seed);
            EnumSet<RegionalBuildingTypes.Type> seen = EnumSet.noneOf(RegionalBuildingTypes.Type.class);
            int buildings = 0, settlements = 0;
            for (var site : world.settlementSites()) {
                int local = 0;
                for (CityBuilding building : world.cityBuildings(site.id())) {
                    var type = RegionalBuildingTypes.type(site.id(), building);
                    if (type == null) continue;
                    local++; buildings++; seen.add(type);
                    require(RegionalSettlementIdentity.buildingAsset(site.id(), building).equals(type.asset), "Wrong exterior route");
                    TilePoint door = world.cityBuildingDoorTiles(building).get(0);
                    require(world.isPassable(site.id(), door.x(), door.y() + 1), "Blocked approach " + site.id());
                    String id = world.ensureHouseInterior(site.id(), building.x1(), building.y1(), door.x(), door.y() + 1);
                    require(world.label(id).equals(RegionalBuildingTypes.name(site.id(), building)), "Wrong interior name " + id);
                    int layoutSeed = building.anchor().x() * 928371 + building.anchor().y() * 364479 + site.id().hashCode();
                    InteriorLayout plan = InteriorLayout.compose(type.theme, layoutSeed, InteriorStyle.forMap(id));
                    require(world.props(id).equals(plan.props()), "Furniture rejected or removed in " + type + " " + site.id()
                            + "; missing=" + plan.props().stream().filter(p -> !world.props(id).contains(p)).toList());
                    for (WorldProp prop : world.props(id)) require(assets.hasSprite(prop.asset()), "Missing furnishing " + prop.asset());
                    Set<TilePoint> reached = reachable(world, id, world.interiorEntryPoint(id));
                    require(world.npcs(id).size() == 2, "Missing keeper or apprentice " + type);
                    for (Npc npc : world.npcs(id)) {
                        require(reached.contains(new TilePoint(npc.x(), npc.y())), "Unreachable keeper " + type + " " + npc);
                        require(npc.dialog().containsAll(type.dialogue), "Missing local dialogue " + type);
                    }
                    for (TilePoint resident : plan.residents()) require(reached.contains(resident), "Planned work position blocked " + type + " " + resident);
                    var exit = world.transitionAt(id, world.width(id) / 2 - 1, world.height(id) - 2);
                    require(exit != null && exit.targetMapId().equals(site.id()) && exit.targetX() == door.x()
                            && exit.targetY() == door.y() + 1, "Wrong exit " + id);
                }
                if (local > 0) settlements++;
            }
            require(seen.size() == 9 && buildings == 38 && settlements == 24,
                    "Incomplete regional coverage: " + seen + ", buildings=" + buildings + ", maps=" + settlements);
            System.out.println("seed=" + seed + ": 9 types, 38 visitable buildings, 24 settlements; furniture and resident routes passed.");
        }
        GameState state = new GameState(GameConfig.load(Path.of("")));
        start(state);
        Path saveChecks = Path.of("out-regional-buildings-check");
        Files.createDirectories(saveChecks);
        Path saveRoot = Files.createTempDirectory(saveChecks, "save-check-");
        SaveSystem saves = new SaveSystem(saveRoot);
        Set<RegionalBuildingTypes.Type> entered = new HashSet<>();
        for (var site : state.world.settlementSites()) for (var building : state.world.cityBuildings(site.id())) {
            var type = RegionalBuildingTypes.type(site.id(), building);
            if (type == null || !entered.add(type)) continue;
            state.currentMapId = site.id();
            require(state.generatedBuildingName(building).equals(RegionalBuildingTypes.name(site.id(), building)), "Building inspection lost name");
            TilePoint door = state.world.cityBuildingDoorTiles(building).get(0);
            state.playerX = door.x(); state.playerY = door.y() + 1;
            state.interact();
            require(state.currentMapId.startsWith("house_" + site.id()), "Cannot enter " + type);
            saves.save(state, type.theme);
            String saved = saves.listSaves().stream().filter(s -> s.saveName().equals(type.theme)).findFirst().orElseThrow().saveId();
            GameState loaded = new GameState(GameConfig.load(saveRoot));
            require(saves.load(loaded, saved), "Could not load " + type);
            require(loaded.currentMapId.equals(site.id()) && loaded.playerX == door.x() && loaded.playerY == door.y() + 1,
                    "Interior save did not restore at its outside door " + type);
            loaded.interact();
            require(loaded.world.label(loaded.currentMapId).equals(RegionalBuildingTypes.name(site.id(), building)),
                    "Loaded building lost regional identity " + type);
            state.playerX = state.world.width(state.currentMapId) / 2 - 1;
            state.playerY = state.world.height(state.currentMapId) - 2;
            state.interact();
            require(state.currentMapId.equals(site.id()) && state.playerX == door.x() && state.playerY == door.y() + 1,
                    "Cannot return outside " + type);
        }
        System.out.println("All nine building types passed in-game naming, entry, return and save/load checks.");
        if (List.of(args).contains("--render")) render();
    }

    private static Set<TilePoint> reachable(WorldMap world, String id, TilePoint start) {
        Set<TilePoint> seen = new HashSet<>(); ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        seen.add(start); queue.add(start);
        while (!queue.isEmpty()) {
            TilePoint p = queue.removeFirst();
            for (int[] step : new int[][]{{0,-1},{-1,0},{1,0},{0,1}}) {
                TilePoint n = new TilePoint(p.x() + step[0], p.y() + step[1]);
                if (world.isPassable(id, n.x(), n.y()) && seen.add(n)) queue.add(n);
            }
        }
        return seen;
    }

    private static void render() throws Exception {
        Path output = Path.of("../asset-review/regional-buildings"); Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath().normalize());
            try {
                ((Timer) field("timer").get(panel)).stop();
                GameState state = (GameState) field("state").get(panel); start(state);
                state.config.renderQuality = "high"; state.zoom = 100;
                state.worldTick = GameState.TICKS_PER_GAME_DAY / 2;
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                Set<RegionalBuildingTypes.Type> captured = new HashSet<>();
                for (var site : state.world.settlementSites()) for (var building : state.world.cityBuildings(site.id())) {
                    var type = RegionalBuildingTypes.type(site.id(), building);
                    if (type == null || !captured.add(type)) continue;
                    TilePoint door = state.world.cityBuildingDoorTiles(building).get(0);
                    state.currentMapId = site.id(); state.playerX = door.x(); state.playerY = door.y() + 1;
                    state.status = RegionalBuildingTypes.name(site.id(), building);
                    state.playerY = door.y() + 3; // Keep the action prompt from covering the roof in the capture.
                    capture(panel, output.resolve(type.theme + "-outside.png"));
                    state.playerY = door.y() + 1;
                    state.interact();
                    require(state.currentMapId.startsWith("house_"), "Capture did not enter " + type);
                    capture(panel, output.resolve(type.theme + "-inside.png"));
                }
            } catch (Exception e) { throw new IllegalStateException(e); }
            finally { panel.shutdown(); }
        });
    }

    private static void capture(GamePanel panel, Path path) throws Exception {
        ((CameraController) field("cameraController").get(panel)).resetToPlayer();
        BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); panel.paint(g); g.dispose(); ImageIO.write(image, "png", path.toFile());
    }
    private static void start(GameState state) {
        state.chooseClass("Mage"); while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
    }
    private static Field field(String name) throws Exception {
        Field field = GamePanel.class.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
