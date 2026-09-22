package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import com.alderfall.game.map.MapArea;
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

/** Regional geography, inhabitants and route checks; optional actual game captures. */
public final class RegionalSettlementTest {
    private static final List<String> PREVIEWS = List.of("village_elderford", "city_riverside",
            "city_highwall", "city_sanctum", "city_belltower", "town_greyharbor",
            "village_snowrest", "village_sunmere", "village_mireford");

    public static void main(String[] args) throws Exception {
        if (List.of(args).contains("--render-only")) { render(); return; }
        AssetStore assets = new AssetStore(Path.of("assets"));
        for (String name : List.of("regional_north_turf_house", "regional_sun_courtyard_house", "regional_fen_stilt_house")) {
            require(assets.hasSprite(name), "Missing house " + name);
            BufferedImage image = ImageIO.read(Path.of("assets/city/regional", name + ".png").toFile());
            require(image.getColorModel().hasAlpha() && image.getRGB(0, 0) >>> 24 == 0, "Opaque house " + name);
        }
        for (long seed : new long[]{0, 42, 2026}) {
            WorldMap world = new WorldMap(seed);
            Set<String> missing = new TreeSet<>();
            for (var site : world.settlementSites()) for (WorldProp prop : world.props(site.id())) {
                if (!assets.hasSprite(prop.asset())) missing.add(prop.asset());
            }
            require(missing.isEmpty(), "Missing settlement assets: " + missing);
            int count = 0;
            for (WorldMap.SettlementSite site : world.settlementSites()) {
                String id = site.id();
                RegionalSettlementIdentity.Region region = RegionalSettlementIdentity.region(id);
                if (region == RegionalSettlementIdentity.Region.NONE) continue;
                count++;
                MapArea area = world.area(id);
                require(area.districts.size() == 2, "Expected two commons " + id + ": " + area.districts);
                Set<TilePoint> reached = reachable(world, id);
                for (var district : area.districts) {
                    require(reached.contains(district.center()) && reached.contains(district.work())
                            && reached.contains(district.gathering()), "Isolated common " + id + " " + district);
                    require(area.propAt(district.work().x(), district.work().y()) == null, "Occupied workplace " + id);
                    require(area.propAt(district.gathering().x(), district.gathering().y()) == null, "Occupied gathering " + id);
                    require(area.propAt(district.center().x(), district.center().y()) != null, "Missing common feature " + id);
                    Npc npc = world.npcs(id).stream().filter(n -> n.name().equals(district.keeperName())).findFirst().orElseThrow();
                    var daytime = AmbientNpcAi.routineFor(npc, world, id, 600, WeatherCondition.CLEAR, 1);
                    var evening = AmbientNpcAi.routineFor(npc, world, id, 1140, WeatherCondition.CLEAR, 1);
                    require(daytime.activity() == AmbientNpcAi.Activity.WORKING && daytime.target().equals(district.work()), "Wrong work routine " + id);
                    require(evening.activity() == AmbientNpcAi.Activity.SOCIALIZING && evening.target().equals(district.gathering()), "Wrong evening routine " + id);
                    if (region == RegionalSettlementIdentity.Region.SUN) {
                        require(AmbientNpcAi.routineFor(npc, world, id, 780, WeatherCondition.CLEAR, 1).activity()
                                == AmbientNpcAi.Activity.RESTING, "No midday rest " + id);
                    }
                }
                for (WorldProp prop : area.props) {
                    require(assets.hasSprite(prop.asset()), "Missing settlement asset " + prop.asset() + " in " + id);
                }
                for (int y = 0; y < area.height(); y++) for (int x = 0; x < area.width(); x++) {
                    // Every border tile has a leave trigger, including isolated terrain beyond walls.
                    // Interior gates must be reachable, and each side must retain a working exit.
                    if (x > 0 && y > 0 && x < area.width() - 1 && y < area.height() - 1
                            && world.transitionAt(id, x, y) != null && world.isPassable(id, x, y))
                        require(reached.contains(new TilePoint(x, y)), "Unreachable exit " + id + " " + x + "," + y);
                }
                require(reached.stream().anyMatch(p -> p.x() == 0)
                        && reached.stream().anyMatch(p -> p.y() == 0)
                        && reached.stream().anyMatch(p -> p.x() == area.width() - 1)
                        && reached.stream().anyMatch(p -> p.y() == area.height() - 1), "Lost directional exit " + id);
                for (CityBuilding building : world.cityBuildings(id)) {
                    require(world.cityBuildingDoorTiles(building).stream()
                            .anyMatch(p -> reached.contains(new TilePoint(p.x(), p.y() + 1))), "Inaccessible building " + id + " " + building.key());
                }
                if (region == RegionalSettlementIdentity.Region.FEN || region == RegionalSettlementIdentity.Region.SUN
                        || region == RegionalSettlementIdentity.Region.RIVER) {
                    require(tiles(area, 'w') >= 5, "Missing regional water " + id);
                }
                if (region == RegionalSettlementIdentity.Region.FEN)
                    require(tiles(area, Terrain.PLANK_ROAD) >= 20, "Missing plank paths " + id);
            }
            require(count == 24, "Unexpected coverage " + count);
            require(world.area(WorldMap.PLAYER_VILLAGE_ID).districts.isEmpty(), "Generated over player camp");
            System.out.println("Regional settlements passed: seed=" + seed + ", maps=" + count + ", commons=" + count * 2);
        }
        WorldMap first = new WorldMap(42), second = new WorldMap(42);
        for (WorldMap.SettlementSite site : first.settlementSites()) {
            require(Arrays.deepEquals(first.area(site.id()).tiles, second.area(site.id()).tiles), "Non-deterministic terrain " + site.id());
            require(first.area(site.id()).districts.equals(second.area(site.id()).districts), "Non-deterministic commons " + site.id());
        }
        var safePosition = SaveSystem.class.getDeclaredMethod("safePosition", WorldMap.class, String.class, int.class, int.class);
        safePosition.setAccessible(true);
        MapArea fen = first.area("village_mireford");
        Set<TilePoint> fenRoutes = reachable(first, fen.id);
        for (int y = 0; y < fen.height(); y++) for (int x = 0; x < fen.width(); x++) {
            if (fen.tileAt(x, y) != 'w') continue;
            TilePoint moved = (TilePoint) safePosition.invoke(new SaveSystem(Path.of("")), first, fen.id, x, y);
            require(first.isPassable(fen.id, moved.x(), moved.y()) && fenRoutes.contains(moved),
                    "Old save position cannot escape new channel at " + x + "," + y);
        }
        System.out.println("Deterministic regeneration and saved-position recovery passed.");
        if (List.of(args).contains("--render")) render();
    }

    private static long tiles(MapArea area, char tile) {
        long count = 0;
        for (char[] row : area.tiles) for (char cell : row) if (cell == tile) count++;
        return count;
    }

    private static Set<TilePoint> reachable(WorldMap world, String id) {
        Set<TilePoint> visited = new HashSet<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        for (int y = 0; y < world.height(id) && queue.isEmpty(); y++) for (int x = 0; x < world.width(id); x++) {
            if (world.transitionAt(id, x, y) != null && world.isPassable(id, x, y)) { queue.add(new TilePoint(x, y)); break; }
        }
        require(!queue.isEmpty(), "No entrance " + id);
        visited.add(queue.peek());
        while (!queue.isEmpty()) {
            TilePoint p = queue.remove();
            for (TilePoint next : List.of(new TilePoint(p.x() - 1, p.y()), new TilePoint(p.x() + 1, p.y()),
                    new TilePoint(p.x(), p.y() - 1), new TilePoint(p.x(), p.y() + 1))) {
                if (world.isPassable(id, next.x(), next.y()) && visited.add(next)) queue.add(next);
            }
        }
        return visited;
    }

    private static void render() throws Exception {
        Path output = Path.of("../asset-review/regions");
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
                state.zoom = 80;
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                for (String id : PREVIEWS) {
                    state.currentMapId = id;
                    TilePoint focus = state.world.area(id).districts.get(0).work();
                    state.playerX = focus.x(); state.playerY = focus.y();
                    state.status = RegionalSettlementIdentity.arrival(id);
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
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
