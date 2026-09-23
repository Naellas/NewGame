package com.alderfall.game;

import com.alderfall.game.map.*;
import java.nio.file.Path;
import java.util.*;

public final class SettlementCompositionTest {
    public static void main(String[] args) {
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        AssetStore assets = new AssetStore(Path.of("assets"));
        for (String mapId : List.of("city_sanctum", "town_embermarket", "city_highwall", "town_briarbridge")) {
            var area = state.world.area(mapId);
            var lights = area.props.stream().filter(p -> SettlementDressing.lamp(p.asset())).toList();
            require(lights.size() >= 3, "No street-light structure in " + mapId);
            for (var lamp : lights) {
                require(!Terrain.connectingRoad(area.tileAt(lamp.x(), lamp.y())), "Lamp in travel lane");
                for (var other : lights) if (lamp != other)
                    require(Math.hypot(lamp.x() - other.x(), lamp.y() - other.y()) >= 5, "Clustered lamps");
                for (var building : state.world.cityBuildings(mapId)) for (var door : state.world.cityBuildingDoorTiles(building))
                    require(!(Math.abs(door.x() - lamp.x()) <= 1 && lamp.y() >= door.y() && lamp.y() <= door.y() + 2), "Lamp blocks entrance");
            }
            long reds = area.props.stream().filter(p -> p.asset().equals("city_prop_market_red")).count();
            require(reds <= 3, "Red stalls still dominate " + mapId);
            for (var prop : area.props) if (prop.asset().startsWith("city_prop_"))
                require(assets.hasSprite(prop.asset()), "Missing composition asset " + prop.asset());
            System.out.println(mapId + ": " + lights.size() + " spaced lamps, " + reds + " red stalls");
        }
        var playerArea = state.world.area(WorldMap.PLAYER_VILLAGE_ID);
        var before = List.copyOf(playerArea.props);
        SettlementDressing.refine(state.world, playerArea);
        require(before.equals(playerArea.props), "Player settlement was redressed");
        state.chooseClass("Mage"); state.mode = GameMode.EXPLORE; state.currentMapId = "city_archive";
        var row = state.world.cityBuildings(state.currentMapId).stream()
                .filter(b -> b.style().equals("row") && !BuildingGeometry.standalone(state.currentMapId, "city", b)).findFirst().orElseThrow();
        var base = state.world.buildingFootprints(state.currentMapId, row).get(0);
        double wallY = base.y + .3;
        state.playerX = (int) Math.floor(base.x - .3); state.playerY = (int) Math.floor(wallY);
        require(!state.moveFreeExploreTo(base.x - .3, wallY, base.x + .3, wallY), "Player phases through upper side wall");
        state.playerX = (int) Math.floor(base.getCenterX()); state.playerY = row.y2() - 1;
        require(PropCollision.navigationAnchor(state.world, state.currentMapId, state.playerX, state.playerY) == null, "Recovery fixture not inside wall");
        state.recoverBlockedSettlementPosition();
        require(PropCollision.navigationAnchor(state.world, state.currentMapId, state.playerX, state.playerY) != null, "Old save remains trapped inside wall");
        System.out.println("Settlement composition passed: variety, lamps, entrances, player content, upper walls and saved-position recovery.");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
