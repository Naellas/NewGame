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
    private static final List<String> PREVIEWS = List.of(
            "city_riverside", "city_archive", "city_highwall", "city_sanctum", "city_belltower",
            "town_briarbridge", "town_ironvale", "town_moonspire", "town_reedwatch",
            "town_embermarket", "town_northwatch", "town_greyharbor",
            "village_elderford", "village_snowrest", "village_sunmere", "village_mireford");

    public static void main(String[] args) throws Exception {
        if (List.of(args).contains("--render-only")) { render(); return; }
        boolean layoutOnly = List.of(args).contains("--layout-only");
        AssetStore assets = new AssetStore(Path.of("assets"));
        for (String name : List.of("regional_north_turf_house", "regional_sun_courtyard_house", "regional_fen_stilt_house")) {
            require(assets.hasSprite(name), "Missing house " + name);
            BufferedImage image = ImageIO.read(Path.of("assets/environments/settlements/city/regional", name + ".png").toFile());
            require(image.getColorModel().hasAlpha() && image.getRGB(0, 0) >>> 24 == 0, "Opaque house " + name);
        }
        for (String style : List.of("hearth", "north", "sun", "fen", "freeholds")) {
            for (String module : List.of("horizontal", "vertical", "horizontal_seamless", "vertical_seamless",
                    "tower", "end_tower", "gate_horizontal", "gate_vertical",
                    "gate_horizontal_clean", "gate_vertical_clean")) {
                String name = "city_wall_" + style + "_" + module;
                require(assets.hasSprite(name), "Missing city wall module " + name);
                BufferedImage image = ImageIO.read(Path.of("assets/environments/settlements/city/walls", name + ".png").toFile());
                require(image != null && image.getColorModel().hasAlpha()
                                && (module.endsWith("seamless") || image.getRGB(0, 0) >>> 24 <= 8),
                        "Opaque city wall module " + name);
            }
        }
        WorldMap identityWorld = new WorldMap(42);
        Set<String> overworldAssets = new TreeSet<>();
        Set<List<String>> townPrograms = new HashSet<>();
        Set<List<String>> townDistrictPrograms = new HashSet<>();
        for (WorldMap.SettlementSite site : identityWorld.settlementSites()) {
            String name = RegionalSettlementIdentity.overworldAsset(site.id());
            if (!name.isEmpty()) overworldAssets.add(name);
            if (!"Town".equals(site.kind())) continue;
            RegionalSettlementIdentity.TownProfile profile = RegionalSettlementIdentity.townProfile(site.id());
            require(profile != null && !profile.identity().isBlank(), "Missing town profile " + site.id());
            require(profile.districts().size() == 2 && profile.districts().stream().noneMatch(String::isBlank),
                    "Missing named town districts " + site.id());
            for (RegionalSettlementIdentity.TownBuildingSpec spec : profile.buildings()) {
                CityBuilding building = identityWorld.cityBuildings(site.id()).stream()
                        .filter(candidate -> candidate.key().equals(spec.key())
                                || candidate.key().startsWith(spec.key() + "_part_"))
                        .findFirst().orElseThrow(() -> new IllegalStateException(
                                "Missing " + spec.label() + " in " + site.id()));
                require(building.style().equals(spec.style()), "Wrong style for " + spec.label() + " in " + site.id());
            }
            townPrograms.add(profile.buildings().stream()
                    .map(spec -> spec.key() + ":" + spec.style()).toList());
            townDistrictPrograms.add(profile.districts());
        }
        require(townPrograms.size() == 7, "Town building programs are not unique: " + townPrograms);
        require(townDistrictPrograms.size() == 7, "Town district programs are not unique: " + townDistrictPrograms);
        require(overworldAssets.size() == 13, "Unexpected authored overworld asset coverage: " + overworldAssets);
        for (String name : overworldAssets) {
            require(assets.hasSprite(name), "Missing overworld settlement " + name);
            BufferedImage image = ImageIO.read(Path.of("assets/environments/settlements/city/overworld", name + ".png").toFile());
            require(image != null && image.getColorModel().hasAlpha() && image.getRGB(0, 0) >>> 24 == 0,
                    "Opaque overworld settlement " + name);
        }
        for (long seed : new long[]{0, 42, 2026}) {
            WorldMap world = new WorldMap(seed);
            Set<String> missing = new TreeSet<>();
            for (var site : world.settlementSites()) for (WorldProp prop : world.props(site.id())) {
                if (!assets.hasSprite(prop.asset())) missing.add(prop.asset());
            }
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
                    if (!layoutOnly) require(assets.hasSprite(prop.asset()),
                            "Missing settlement asset " + prop.asset() + " in " + id);
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
                if ("Town".equals(site.kind()) || "City".equals(site.kind())) {
                    assertContinuousCityWall(world, id, area);
                }
                for (CityBuilding building : world.cityBuildings(id)) {
                    require(world.cityBuildingDoorTiles(building).stream()
                            .anyMatch(p -> reached.contains(new TilePoint(p.x(), p.y() + 1))), "Inaccessible building " + id + " " + building.key());
                }
                if ("Town".equals(site.kind())) {
                    RegionalSettlementIdentity.TownProfile profile = RegionalSettlementIdentity.townProfile(id);
                    require(area.districts.stream().map(RegionalSettlementIdentity.District::name).toList()
                                    .equals(profile.districts()),
                            "Generated quarters do not match town identity " + id + ": " + area.districts);
                    require(interiorTiles(area, 'x') >= 40, "Town lacks an authored wall ring " + id);
                    for (CityBuilding building : world.cityBuildings(id)) {
                        int foundationTiles = 0;
                        for (int y = building.y1(); y <= building.y2(); y++) {
                            for (int x = building.x1(); x <= building.x2(); x++) {
                                char tile = area.tileAt(x, y);
                                require(Terrain.passable(tile) && tile != 'w' && tile != '~',
                                        "Building has an unsafe underlying tile " + id + " " + building.key()
                                                + " @" + x + "," + y + "=" + area.tileAt(x, y));
                                if (tile == 'p' || tile == 'C' || tile == 'U' || tile == 'V') foundationTiles++;
                            }
                        }
                        int footprintArea = building.width() * (building.y2() - building.y1() + 1);
                        int requiredFoundation = footprintArea / 6;
                        if (requiredFoundation < 1) requiredFoundation = 1;
                        require(foundationTiles >= requiredFoundation,
                                "Building lacks a coherent foundation " + id + " " + building.key());
                        require(world.cityBuildingDoorTiles(building).stream()
                                        .map(door -> new TilePoint(door.x(), door.y() + 1))
                                        .anyMatch(point -> roadReachesBorder(world, id, point)),
                                "Institution lacks a gate-connected street " + id + " " + building.key());
                    }
                    require(longestStraightRoadRun(world, id) <= 28,
                            "Town restored a map-wide straight avenue " + id + ": " + longestStraightRoadRun(world, id));
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
            if (!layoutOnly) require(missing.isEmpty(), "Missing settlement assets: " + missing);
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

    private static long interiorTiles(MapArea area, char tile) {
        long count = 0;
        for (int y = 1; y < area.height() - 1; y++)
            for (int x = 1; x < area.width() - 1; x++) if (area.tileAt(x, y) == tile) count++;
        return count;
    }

    private static void assertContinuousCityWall(WorldMap world, String id, MapArea area) {
        Set<TilePoint> nodes = new HashSet<>();
        int corners = 0;
        int gates = 0;
        for (int y = 0; y < area.height(); y++) for (int x = 0; x < area.width(); x++) {
            char tile = area.tileAt(x, y);
            if (tile != 'x' && tile != Terrain.CITY_GATE) continue;
            require(x > 0 && y > 0 && x < area.width() - 1 && y < area.height() - 1,
                    "Wall swallowed the outskirts boundary " + id + " @" + x + "," + y);
            require(tile == Terrain.CITY_GATE ? world.isPassable(id, x, y) : !world.isPassable(id, x, y),
                    "Incorrect wall collision " + id + " @" + x + "," + y);
            if (tile == Terrain.CITY_GATE) {
                boolean horizontalWall = area.tileAt(x - 1, y) == 'x' || area.tileAt(x + 1, y) == 'x';
                TilePoint approachA = horizontalWall ? new TilePoint(x, y - 1) : new TilePoint(x - 1, y);
                TilePoint approachB = horizontalWall ? new TilePoint(x, y + 1) : new TilePoint(x + 1, y);
                require(world.isPassable(id, approachA.x(), approachA.y())
                                && world.isPassable(id, approachB.x(), approachB.y())
                                && Terrain.connectingRoad(area.tileAt(approachA.x(), approachA.y()))
                                && Terrain.connectingRoad(area.tileAt(approachB.x(), approachB.y())),
                        "Gate approach is obstructed " + id + " @" + x + "," + y);
            }
            nodes.add(new TilePoint(x, y));
            if (tile == Terrain.CITY_GATE) gates++;
        }
        require(nodes.size() >= 60, "Undersized city wall " + id + ": " + nodes.size());
        require(gates == 4, "City wall must have one gate per side " + id + ": " + gates);
        int minX = nodes.stream().mapToInt(TilePoint::x).min().orElseThrow();
        int maxX = nodes.stream().mapToInt(TilePoint::x).max().orElseThrow();
        int minY = nodes.stream().mapToInt(TilePoint::y).min().orElseThrow();
        int maxY = nodes.stream().mapToInt(TilePoint::y).max().orElseThrow();
        require(nodes.stream().filter(p -> area.tileAt(p.x(), p.y()) == Terrain.CITY_GATE && p.y() == minY).count() == 1
                        && nodes.stream().filter(p -> area.tileAt(p.x(), p.y()) == Terrain.CITY_GATE && p.y() == maxY).count() == 1
                        && nodes.stream().filter(p -> area.tileAt(p.x(), p.y()) == Terrain.CITY_GATE && p.x() == minX).count() == 1
                        && nodes.stream().filter(p -> area.tileAt(p.x(), p.y()) == Terrain.CITY_GATE && p.x() == maxX).count() == 1,
                "City wall gates are not distributed one per side " + id);
        for (TilePoint node : nodes) {
            boolean horizontal = nodes.contains(new TilePoint(node.x() - 1, node.y()))
                    || nodes.contains(new TilePoint(node.x() + 1, node.y()));
            boolean vertical = nodes.contains(new TilePoint(node.x(), node.y() - 1))
                    || nodes.contains(new TilePoint(node.x(), node.y() + 1));
            int degree = 0;
            if (nodes.contains(new TilePoint(node.x() - 1, node.y()))) degree++;
            if (nodes.contains(new TilePoint(node.x() + 1, node.y()))) degree++;
            if (nodes.contains(new TilePoint(node.x(), node.y() - 1))) degree++;
            if (nodes.contains(new TilePoint(node.x(), node.y() + 1))) degree++;
            require(degree == 2, "Detached or branching wall " + id + " @" + node + " degree=" + degree
                    + " neighbors=" + area.tileAt(node.x() - 1, node.y()) + area.tileAt(node.x() + 1, node.y())
                    + area.tileAt(node.x(), node.y() - 1) + area.tileAt(node.x(), node.y() + 1));
            if (horizontal && vertical) corners++;
        }
        require(corners == 4, "City wall is not one orthogonal rectangle " + id + ": corners=" + corners);
        Set<TilePoint> connected = new HashSet<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        TilePoint first = nodes.iterator().next();
        connected.add(first);
        queue.add(first);
        while (!queue.isEmpty()) {
            TilePoint point = queue.removeFirst();
            for (TilePoint next : List.of(new TilePoint(point.x() - 1, point.y()),
                    new TilePoint(point.x() + 1, point.y()), new TilePoint(point.x(), point.y() - 1),
                    new TilePoint(point.x(), point.y() + 1))) {
                if (nodes.contains(next) && connected.add(next)) queue.addLast(next);
            }
        }
        require(connected.size() == nodes.size(), "Detached wall segment " + id);
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

    private static boolean roadReachesBorder(WorldMap world, String id, TilePoint start) {
        if (!Terrain.connectingRoad(world.tileAt(id, start.x(), start.y()))) return false;
        Set<TilePoint> visited = new HashSet<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        visited.add(start); queue.add(start);
        while (!queue.isEmpty()) {
            TilePoint point = queue.removeFirst();
            if (point.x() == 0 || point.y() == 0
                    || point.x() == world.width(id) - 1 || point.y() == world.height(id) - 1) return true;
            for (TilePoint next : List.of(new TilePoint(point.x() - 1, point.y()), new TilePoint(point.x() + 1, point.y()),
                    new TilePoint(point.x(), point.y() - 1), new TilePoint(point.x(), point.y() + 1))) {
                if (next.x() < 0 || next.y() < 0 || next.x() >= world.width(id) || next.y() >= world.height(id)
                        || visited.contains(next) || !Terrain.connectingRoad(world.tileAt(id, next.x(), next.y()))) continue;
                visited.add(next); queue.addLast(next);
            }
        }
        return false;
    }

    private static int longestStraightRoadRun(WorldMap world, String id) {
        int longest = 0;
        for (int y = 0; y < world.height(id); y++) {
            int run = 0;
            for (int x = 0; x < world.width(id); x++) {
                run = Terrain.connectingRoad(world.tileAt(id, x, y)) ? run + 1 : 0;
                longest = Math.max(longest, run);
            }
        }
        for (int x = 0; x < world.width(id); x++) {
            int run = 0;
            for (int y = 0; y < world.height(id); y++) {
                run = Terrain.connectingRoad(world.tileAt(id, x, y)) ? run + 1 : 0;
                longest = Math.max(longest, run);
            }
        }
        return longest;
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
