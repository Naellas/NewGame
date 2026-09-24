package com.alderfall.game;

import com.alderfall.game.camera.CameraController;
import com.alderfall.game.map.WorldMap;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** District spacing, public-space reservations and optional actual renderer overview. */
public final class TownLayoutTest {
    public static void main(String[] args) throws Exception {
        if (List.of(args).contains("--render-plots-only")) { render(true); return; }
        for (long seed : new long[]{0, 42, 2026}) {
            WorldMap world = new WorldMap(seed);
            for (var site : world.settlementSites()) {
                if (!site.id().startsWith("town_")) continue;
                var area = world.area(site.id());
                var buildings = world.cityBuildings(site.id());
                var signature = buildings.stream().filter(b -> TownBuildingArt.landmark(site.id(), b)).findFirst().orElseThrow();
                require(buildings.stream().filter(b -> TownBuildingArt.landmark(site.id(), b)).count() == 1,
                        "Duplicated signature landmark " + site.id());
                require(buildings.stream().map(CityBuilding::key).distinct().count() == buildings.size(),
                        "Duplicated building identity " + site.id());
                require(signature.width() == 6, "Landmark lacks reserved frontage " + site.id());
                var signatureDoor = world.cityBuildingDoorTiles(signature).getFirst();
                require(area.landmarks.containsKey(signature.outside(signatureDoor, 0, 2)), "Missing landmark forecourt " + site.id());
                require(area.props.stream().anyMatch(p -> TownGardenArt.fountain(p.asset())), "Density displaced fountain " + site.id());
                require(buildings.stream().filter(b -> b.key().startsWith("town_infill_home_")).count() == 4,
                        "Missing infill homes " + site.id());
                var addedResidents = world.npcs(site.id()).stream().filter(n -> n.name().startsWith("Market Porter ")
                        || n.name().startsWith("Market Shopper ") || n.name().startsWith("Garden Tender ")
                        || n.name().startsWith("Local Messenger ") || n.name().startsWith("Quarter Resident ")).toList();
                require(addedResidents.size() == 12, "Missing neighborhood residents " + site.id());
                Set<TilePoint> residentSpots = new HashSet<>();
                for (Npc npc : addedResidents) {
                    require(residentSpots.add(new TilePoint(npc.x(), npc.y())), "Stacked residents " + site.id());
                    require(world.isPassable(site.id(), npc.x(), npc.y())
                            && world.transitionAt(site.id(), npc.x(), npc.y()) == null, "Unsafe resident " + npc.name());
                    for (CityBuilding b : buildings) for (TilePoint door : world.cityBuildingDoorTiles(b))
                        require(!b.outside(door, 0, 1).equals(new TilePoint(npc.x(), npc.y())), "Resident blocks entrance");
                }
                var barn = buildings.stream().filter(b -> b.key().equals("town_service_barn")).findFirst().orElseThrow();
                var barnDoor = world.cityBuildingDoorTiles(barn).getFirst();
                var barnApproach = barn.outside(barnDoor, 0, 1);
                String barnInterior = world.ensureHouseInterior(site.id(), barnDoor.x(), barnDoor.y(), barnApproach.x(), barnApproach.y());
                require(world.area(barnInterior).label.equals("Barn Feed Stores"), "Barn has a house/shop interior " + site.id());
                require(world.area(barnInterior).props.stream().anyMatch(p -> p.asset().contains("grain_sacks")), "Barn lacks feed stores");
                require(area.landmarks.containsValue("Barn Paddock"), "Missing barn paddock " + site.id());
                var paddock = area.landmarks.entrySet().stream().filter(e -> e.getValue().equals("Barn Paddock"))
                        .findFirst().orElseThrow().getKey();
                require(Math.abs(paddock.x() - barnDoor.x()) + Math.abs(paddock.y() - barnDoor.y()) <= 10,
                        "Paddock detached from its barn " + site.id());
                if (buildings.stream().anyMatch(b -> b.style().equals("blacksmith")))
                    require(area.landmarks.containsValue("Smith's Working Yard"), "Missing smith yard " + site.id());
                if (buildings.stream().anyMatch(b -> Set.of("alchemist", "apothecary").contains(b.style())))
                    require(area.landmarks.containsValue("Apothecary Herb Garden"), "Missing herb garden " + site.id());

                require(buildings.stream().anyMatch(b -> b.y1() > area.height() - 11), "Missing suburbs " + site.id());
                require(area.tileAt(9, 9) == 'x' && area.tileAt(area.width() - 11, 9) == 'x',
                        "Missing wall/outskirts setback " + site.id());
                for (String label : List.of("Civic Plaza", "Public Garden", "Market Square")) {
                    var center = area.landmarks.entrySet().stream().filter(e -> e.getValue().equals(label))
                            .findFirst().orElseThrow().getKey();
                    int open = 0;
                    for (int y = center.y() - 3; y <= center.y() + 3; y++)
                        for (int x = center.x() - 6; x <= center.x() + 6; x++) {
                            require(world.cityBuildingAt(site.id(), x, y) == null, "Building consumed " + label);
                            if (world.isPassable(site.id(), x, y)) open++;
                        }
                    require(open >= 65, site.id() + " obstructed " + label + ": " + open);
                }
                for (var b : buildings) {
                    require(b.x1() >= 4 && b.x2() < area.width() - 4 && b.y2() < area.height() - 4,
                            "Building touches wall " + site.id() + ": " + b.key());
                    for (var other : buildings) {
                        if (b == other) continue;
                        require(b.x2() + 1 < other.x1() || other.x2() + 1 < b.x1()
                                || b.y2() + 3 < other.y1() || other.y2() + 3 < b.y1(),
                                "No garden/roof clearance " + site.id() + ": " + b.key() + "/" + other.key());
                    }
                }
            }
        }
        if (List.of(args).contains("--render")) render(false);
        System.out.println("TownLayoutTest passed: reserved squares, wall setbacks and frontage spacing across three seeds.");
    }

    private static void render(boolean plotsOnly) throws Exception {
        Path output = Path.of("temp/town-layout/review");
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            GamePanel panel = new GamePanel(Path.of("").toAbsolutePath());
            try {
                ((javax.swing.Timer) field("timer").get(panel)).stop();
                GameState state = (GameState) field("state").get(panel);
                state.chooseClass("Mage");
                while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
                state.worldTick = GameState.TICKS_PER_GAME_DAY / 2;
                state.config.renderQuality = "high";
                panel.setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
                for (var site : state.world.settlementSites()) {
                    if (!site.id().startsWith("town_")) continue;
                    state.currentMapId = site.id();
                    state.zoom = 40;
                    var center = state.world.area(site.id()).landmarks.entrySet().stream()
                            .filter(e -> e.getValue().equals("Civic Plaza")).findFirst().orElseThrow().getKey();
                    state.playerX = center.x(); state.playerY = center.y();
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    BufferedImage frame = new BufferedImage(GameConfig.WIDTH, GameConfig.HEIGHT, BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g;
                    var plot = state.world.area(site.id()).townPlots().stream()
                            .max(java.util.Comparator.comparingInt(p -> p.cells().size())).orElseThrow();
                    state.playerX = plot.center().x(); state.playerY = plot.center().y();
                    state.zoom = 80;
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    g = frame.createGraphics(); panel.paint(g); g.dispose();
                    ImageIO.write(frame, "png", output.resolve(site.id() + "-plots.png").toFile());
                    if (plotsOnly) continue;
                    state.zoom = 40; state.playerX = center.x(); state.playerY = center.y();
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    g = frame.createGraphics(); panel.paint(g); g.dispose();
                    ImageIO.write(frame, "png", output.resolve(site.id() + ".png").toFile());
                    var signature = state.world.cityBuildings(site.id()).stream()
                            .filter(b -> TownBuildingArt.landmark(site.id(), b)).findFirst().orElseThrow();
                    var signatureDoor = state.world.cityBuildingDoorTiles(signature).getFirst();
                    state.zoom = 80;
                    state.playerX = signatureDoor.x(); state.playerY = signatureDoor.y() + 3;
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    g = frame.createGraphics(); panel.paint(g); g.dispose();
                    ImageIO.write(frame, "png", output.resolve(site.id() + "-landmark.png").toFile());
                    var streetGarden = state.world.area(site.id()).landmarks.entrySet().stream()
                            .filter(e -> e.getValue().equals("Street Grove")).map(java.util.Map.Entry::getKey)
                            .sorted(java.util.Comparator.comparingInt(TilePoint::y).thenComparingInt(TilePoint::x))
                            .findFirst().orElse(center);
                    state.zoom = 100;
                    state.playerX = streetGarden.x(); state.playerY = streetGarden.y() + 3;
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    g = frame.createGraphics(); panel.paint(g); g.dispose();
                    ImageIO.write(frame, "png", output.resolve(site.id() + "-street-garden.png").toFile());
                    state.playerX = center.x(); state.playerY = center.y();
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    g = frame.createGraphics(); panel.paint(g); g.dispose();
                    ImageIO.write(frame, "png", output.resolve(site.id() + "-square-seating.png").toFile());
                    state.zoom = 40;
                    state.playerX = center.x();
                    state.playerY = state.world.height(site.id()) - 9;
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    g = frame.createGraphics(); panel.paint(g); g.dispose();
                    ImageIO.write(frame, "png", output.resolve(site.id() + "-suburbs.png").toFile());
                    boolean north = Set.of("town_northwatch", "town_ironvale", "town_moonspire").contains(site.id());
                    state.playerX = north ? 36 : state.world.width(site.id()) - 9;
                    state.playerY = north ? 8 : 27;
                    ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                    g = frame.createGraphics(); panel.paint(g); g.dispose();
                    ImageIO.write(frame, "png", output.resolve(site.id() + "-wall-bay.png").toFile());
                    {
                        state.zoom = 100;
                        for (var b : state.world.cityBuildings(site.id())) {
                            if (b.facing() == CityBuilding.Facing.SOUTH) continue;
                            var door = state.world.cityBuildingDoorTiles(b).getFirst();
                            var approach = b.outside(door, 0, 1);
                            state.playerX = approach.x(); state.playerY = approach.y();
                            ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                            g = frame.createGraphics(); panel.paint(g); g.dispose();
                            ImageIO.write(frame, "png", output.resolve(site.id() + "-house-"
                                    + b.facing().name().toLowerCase(java.util.Locale.ROOT) + ".png").toFile());
                        }
                    }
                    if (site.id().equals("town_northwatch") || site.id().equals("town_embermarket")) {
                        state.zoom = 100;
                        var fountain = state.world.area(site.id()).props.stream()
                                .filter(p -> TownGardenArt.fountain(p.asset())).findFirst().orElseThrow();
                        state.playerX = fountain.x(); state.playerY = fountain.y();
                        ((CameraController) field("cameraController").get(panel)).resetToPlayer();
                        g = frame.createGraphics(); panel.paint(g); g.dispose();
                        ImageIO.write(frame, "png", output.resolve(site.id() + "-garden-details.png").toFile());
                    }
                }
            } catch (Exception e) { throw new IllegalStateException(e); }
            finally { panel.shutdown(); }
        });
    }

    private static Field field(String name) throws Exception {
        Field f = GamePanel.class.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
