package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;

/** Cardinal thresholds, real input paths, solid bases and stable interior identity. */
public final class CardinalBuildingTest {
    private static final List<String> TOWNS = List.of("town_briarbridge", "town_ironvale", "town_moonspire",
            "town_reedwatch", "town_embermarket", "town_northwatch", "town_greyharbor");
    public static void main(String[] args) throws Exception {
        var catalog = new AssetCatalog(Path.of("assets"));
        for (long seed : new long[]{0, 42, 2026}) {
            var world = new WorldMap(seed);
            for (String map : TOWNS) {
                Set<CityBuilding.Facing> directions = EnumSet.noneOf(CityBuilding.Facing.class);
                for (var b : world.cityBuildings(map)) {
                    directions.add(b.facing());
                    if (b.facing() == CityBuilding.Facing.SOUTH) continue;
                    var door = world.cityBuildingDoorTiles(b).getFirst();
                    var approach = b.outside(door, 0, 1);
                    require(world.isPassable(map, approach.x(), approach.y()), "Blocked approach " + b.key());
                    require(Terrain.connectingRoad(world.tileAt(map, approach.x(), approach.y())), "Unconnected path " + b.key());
                    require(world.cityBuildingEntryAt(map, door.x(), door.y(), approach.x(), approach.y()) == b,
                            "Entrance wrong side " + b.key());
                    for (var side : CityBuilding.Facing.values()) if (side != b.facing())
                        require(world.cityBuildingEntryAt(map, door.x(), door.y(), door.x() + side.dx, door.y() + side.dy) == null,
                                "Entry through wrong wall " + b.key());
                    var base = world.buildingFootprints(map, b).getFirst();
                    require(!PropCollision.clear(world, map, base.getCenterX(), base.getCenterY()), "Hollow building");
                    require(PropCollision.clear(world, map, approach.x() + .5, approach.y() + .5), "Approach radius blocked");
                    var art = ImageIO.read(catalog.findAsset(TownBuildingArt.asset(map, b)).toFile());
                    require(art.getColorModel().hasAlpha() && art.getRGB(0, 0) >>> 24 == 0, "Opaque art");
                    String house = world.ensureHouseInterior(map, door.x(), door.y(), approach.x(), approach.y());
                    require(house.equals(world.ensureHouseInterior(map, b.x1(), b.y1(), approach.x(), approach.y())),
                            "Door changed interior identity");
                }
                require(directions.size() == 4, "Missing cardinal direction " + map);
            }
        }
        var state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        state.chooseClass("Mage"); state.mode = GameMode.EXPLORE;
        for (String map : TOWNS) for (var b : state.world.cityBuildings(map)) {
            if (b.facing() == CityBuilding.Facing.SOUTH) continue;
            var door = state.world.cityBuildingDoorTiles(b).getFirst();
            var approach = b.outside(door, 0, 1);
            for (int method = 0; method < 3; method++) {
                state.currentMapId = map; state.playerX = approach.x(); state.playerY = approach.y();
                if (method == 0) require(state.enterBuilding(b), "Explicit entry failed");
                else if (method == 1) state.interact();
                else require(state.moveFreeExploreTo(approach.x() + .5, approach.y() + .5,
                        door.x() + .5, door.y() + .5), "Walking entry failed " + b.facing());
                require(!state.currentMapId.equals(map), "Did not enter " + b.facing() + " method=" + method);
                var interior = state.world.area(state.currentMapId);
                boolean exitFound = false;
                exits: for (int y = 0; y < interior.height(); y++) for (int x = 0; x < interior.width(); x++) {
                    var exit = state.world.transitionAt(state.currentMapId, x, y);
                    if (exit == null) continue;
                    state.playerX = x; state.playerY = y; state.interact(); exitFound = true; break exits;
                }
                require(exitFound && state.currentMapId.equals(map) && state.playerX == approach.x()
                        && state.playerY == approach.y(), "Wrong return side " + b.facing());
            }
        }
        System.out.println("CardinalBuildingTest passed: seven towns, four directions, three seeds, three entry methods and return positions.");
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
