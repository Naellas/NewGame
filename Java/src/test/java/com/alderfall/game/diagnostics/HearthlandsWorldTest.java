package com.alderfall.game;

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

/** Checks the outward extension without writing settings or player saves. */
public final class HearthlandsWorldTest {
    private static final List<String> MAPS = List.of("village_elderford", "town_briarbridge", "town_moonspire");

    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        for (String asset : List.of(HearthlandsFolklore.OVEN, HearthlandsFolklore.SEEDHOUSE, HearthlandsFolklore.WARD)) {
            require(assets.hasSprite(asset), "Missing asset " + asset);
            BufferedImage image = ImageIO.read(Path.of("assets/environments/settlements/city/folklore", asset + ".png").toFile());
            require(image.getColorModel().hasAlpha() && image.getRGB(0, 0) >>> 24 == 0, "Opaque background " + asset);
        }
        for (long seed : new long[]{0, 42, 1024}) {
            WorldMap world = new WorldMap(seed);
            for (String id : MAPS) {
                List<WorldProp> props = world.props(id).stream().filter(p -> p.asset().equals(HearthlandsFolklore.OVEN)
                        || p.asset().equals(HearthlandsFolklore.WARD)).toList();
                require(props.size() == (id.equals("village_elderford") ? 1 : 2), "Missing placements " + id + " seed=" + seed + " " + props);
                Set<TilePoint> reachable = reachable(world, id);
                for (WorldProp prop : props) {
                    require(world.area(id).propsAt(prop.x(), prop.y()).contains(prop), "Unindexed prop");
                    require(world.transitionAt(id, prop.x(), prop.y()) == null, "Covered exit");
                    require(world.cityBuildingAt(id, prop.x(), prop.y()) == null, "Covered building");
                    require(!Terrain.connectingRoad(world.tileAt(id, prop.x(), prop.y())), "Covered road");
                    require(reachable.contains(new TilePoint(prop.x(), prop.y())), "Inaccessible prop " + prop);
                    require(!HearthlandsFolklore.observation(id, prop).isBlank(), "Silent folklore prop");
                }
                System.out.println("seed=" + seed + " " + id + " " + props);
            }
            CityBuilding exchange = world.cityBuildings("village_elderford").stream()
                    .filter(b -> !HearthlandsFolklore.buildingName("village_elderford", b).isEmpty()).findFirst().orElseThrow();
            require(exchange.style().equals("farmstead"), "Seed exchange lacks a working interior style");
            Set<TilePoint> villageRoute = reachable(world, "village_elderford");
            require(world.cityBuildingDoorTiles(exchange).stream()
                    .anyMatch(p -> villageRoute.contains(new TilePoint(p.x(), p.y() + 1))), "Seed exchange door inaccessible");
            require(world.props(WorldMap.OVERWORLD_ID).stream().filter(p -> p.asset().equals(HearthlandsFolklore.WARD)).count() == 3,
                    "Missing outward road wards, seed=" + seed);
        }
        require(!HearthlandsFolklore.observation(WorldMap.PLAYER_VILLAGE_ID,
                new WorldProp(1, 1, FolkloreContent.HEARTH, 80)).isBlank(), "Existing camp hearth lost its observation");
        GameState state = new GameState(GameConfig.load(Path.of("")));
        state.chooseClass("Mage");
        while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
        for (String id : List.of("village_oakhaven", "village_elderford")) {
            CityBuilding house = state.world.cityBuildings(id).stream()
                    .filter(b -> !HearthlandsFolklore.buildingName(id, b).isEmpty()).findFirst().orElseThrow();
            TilePoint door = state.world.cityBuildingDoorTiles(house).get(0);
            state.currentMapId = id; state.playerX = door.x(); state.playerY = door.y() + 1;
            state.interact();
            require(state.currentMapId.startsWith("house_" + id), "Cannot enter seedhouse " + id);
            require(state.world.label(state.currentMapId).equals(HearthlandsFolklore.buildingName(id, house)), "Interior lost name");
            require(state.world.npcs(state.currentMapId).stream().anyMatch(n -> n.name().equals("Seed Keeper")), "No seed keeper");
            require(reachable(state.world, state.currentMapId).contains(new TilePoint(state.playerX, state.playerY)), "Interior exit inaccessible");
            state.playerX = state.world.width(state.currentMapId) / 2 - 1;
            state.playerY = state.world.height(state.currentMapId) - 2;
            state.interact();
            require(state.currentMapId.equals(id), "Cannot leave seedhouse " + id);
        }
        System.out.println("Hearthlands extension checks passed for 3 seeds.");
        if (List.of(args).contains("--render")) render();
    }

    private static Set<TilePoint> reachable(WorldMap world, String id) {
        Set<TilePoint> visited = new HashSet<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        for (int y = 0; y < world.height(id) && queue.isEmpty(); y++) {
            for (int x = 0; x < world.width(id); x++) {
                if (world.transitionAt(id, x, y) != null && world.isPassable(id, x, y)) {
                    queue.add(new TilePoint(x, y)); break;
                }
            }
        }
        require(!queue.isEmpty(), "No entrance " + id);
        visited.add(queue.peek());
        while (!queue.isEmpty()) {
            TilePoint p = queue.remove();
            for (TilePoint next : List.of(new TilePoint(p.x() - 1, p.y()), new TilePoint(p.x() + 1, p.y()),
                    new TilePoint(p.x(), p.y() - 1), new TilePoint(p.x(), p.y() + 1))) {
                if (next.x() >= 0 && next.y() >= 0 && next.x() < world.width(id) && next.y() < world.height(id)
                        && world.isPassable(id, next.x(), next.y()) && visited.add(next)) queue.add(next);
            }
        }
        return visited;
    }

    private static void render() throws Exception {
        Path output = Path.of("../asset-review/hearthlands");
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath().normalize());
            try {
                ((Timer) field("timer").get(panel)).stop();
                GameState state = (GameState) field("state").get(panel);
                state.chooseClass("Mage");
                while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
                state.config.renderQuality = "high";
                state.worldTick = GameState.TICKS_PER_GAME_DAY / 2;
                state.zoom = 100;
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                for (String id : MAPS) {
                    WorldProp focus = state.world.props(id).stream().filter(p -> p.asset().equals(HearthlandsFolklore.OVEN)
                            || p.asset().equals(HearthlandsFolklore.WARD)).findFirst().orElseThrow();
                    state.currentMapId = id;
                    state.playerX = focus.x(); state.playerY = focus.y() + 2;
                    state.status = HearthlandsFolklore.observation(id, focus);
                    if (id.equals("village_elderford")) { state.playerX = 11; state.playerY = 8; }
                    BufferedImage image = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = image.createGraphics();
                    panel.paint(graphics); graphics.dispose();
                    ImageIO.write(image, "png", output.resolve(id + ".png").toFile());
                }
            } catch (Exception ex) { throw new IllegalStateException(ex); }
            finally { panel.shutdown(); }
        });
    }

    private static Field field(String name) throws Exception {
        Field field = GamePanel.class.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
