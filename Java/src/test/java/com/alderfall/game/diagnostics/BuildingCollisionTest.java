package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BuildingCollisionTest {
    public static void main(String[] args) {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        state.chooseClass("Mage");
        state.mode = GameMode.EXPLORE;
        WorldMap world = state.world;
        int checked = 0;
        for (String mapId : List.of("town_briarbridge", "village_oakhaven", "village_snowrest",
                "village_dunewick", "village_mireford", "city_archive", WorldMap.PLAYER_VILLAGE_ID)) {
            for (CityBuilding building : world.cityBuildings(mapId)) {
                var bases = world.buildingFootprints(mapId, building);
                require(!bases.isEmpty(), "Missing base: " + building);
                for (var base : bases) {
                    require(base.height <= building.depth() && base.height > 0, "Base includes roof height");
                    require(!PropCollision.clear(world, mapId, base.getCenterX(), base.getCenterY()), "Base not solid");
                    require(!PropCollision.canTravel(world, mapId, base.getCenterX(), base.y - 0.3,
                            base.getCenterX(), base.getMaxY() + 0.3), "Swept movement tunnels through base");
                }
                for (TilePoint door : world.cityBuildingDoorTiles(building)) {
                    require(world.cityBuildingBlocksMovementAt(mapId, door.x(), door.y()), "Door must still stop movement");
                    require(world.cityBuildingEntryAt(mapId, door.x(), door.y(), door.x(), door.y() + 1) == building,
                            "Door entry lost");
                }

                checked++;
            }
        }
        // A five-tile row has one logical entrance but three visible house sections.
        // Its outer sections must be solid too, not just the strip around that entrance.
        CityBuilding row = new CityBuilding("collision_row_regression", 10, 10, 14, 12, "row", 0);
        var rowBases = BuildingGeometry.footprints(world, "city_archive", row);
        require(BuildingGeometry.moduleCount(row) == 3, "Regression fixture should draw three sections");
        require(rowBases.size() == 3, "Footprints still follow door count instead of drawn sections");
        for (double x = 10.25; x <= 14.75; x += .25) {
            final double sampleX = x;
            require(rowBases.stream().anyMatch(b -> b.contains(sampleX, 11.0)), "Hole along row frontage at " + x);
        }
        String mapId = "town_briarbridge";
        CityBuilding building = world.cityBuildings(mapId).stream().filter(b -> b.depth() >= 3).findFirst().orElseThrow();
        var area = world.area(mapId);
        // Isolate the building from scenery and terrain so we can test its newly opened passage.
        for (var prop : new ArrayList<>(area.propsInBounds(building.x1() - 2, building.y1() - 2,
                building.x2() + 3, building.y2() + 3))) area.removeProp(prop);
        area.fillTiles(building.x1(), building.y1(), building.x2(), building.y2() + 1, 'g');
        var base = world.buildingFootprints(mapId, building).get(0);
        double passageY = base.y - 0.20;
        require(PropCollision.canTravel(world, mapId, base.x, passageY, base.getMaxX(), passageY),
                "Cannot walk behind roof");
        require(PropCollision.clear(world, mapId, base.x - 0.20, base.getCenterY()), "Side clearance remains too large");
        state.currentMapId = mapId;
        state.playerX = (int) Math.floor(base.x);
        state.playerY = (int) Math.floor(passageY);
        require(state.moveFreeExploreTo(base.x, passageY, base.getMaxX(), passageY),
                "Continuous movement still uses whole-tile building hitboxes");
        // Doors must trigger at the front edge without crossing the solid base.
        TilePoint door = world.cityBuildingDoorTiles(building).get(0);
        state.playerX = door.x(); state.playerY = door.y() + 1;
        require(state.moveFreeExploreTo(door.x() + 0.5, door.y() + 1.20,
                door.x() + 0.5, door.y() + 1.10), "Cannot enter through front door");
        require(!mapId.equals(state.currentMapId), "Front door did not enter interior");
        System.out.println("BuildingCollisionTest passed: " + checked + " buildings, solid bases, roof/side clearance, swept movement and doors");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
