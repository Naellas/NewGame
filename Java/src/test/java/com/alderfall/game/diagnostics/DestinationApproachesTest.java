package com.alderfall.game;

import com.alderfall.game.map.*;
import java.util.*;
import java.nio.file.Path;

public final class DestinationApproachesTest {
    public static void main(String[] args) {
        for (long seed : new long[]{42, 1337}) {
            WorldMap world = new WorldMap(seed);
            var area = world.area(WorldMap.OVERWORLD_ID);
            var approaches = area.destinationApproaches();
            require(approaches.size() > 10, "No useful destination coverage");
            int dungeons = 0, caches = 0, quests = 0, gaps = 0;
            for (var approach : approaches) {
                var trail = approach.trail();
                require(trail.size() >= 5 && trail.size() <= 29, "Unbounded route");
                require(NatureSiteGenerator.walkable(world, trail), "Scenery blocks " + approach.label());
                TilePoint end = trail.get(trail.size() - 1), target = approach.destination();
                require(Math.abs(end.x() - target.x()) + Math.abs(end.y() - target.y()) <= 1,
                        "Trail does not reach its destination");
                if (!approach.mapId().equals(area.id)) {
                    var transition = world.transitionAt(area.id, target.x(), target.y());
                    require(transition != null && world.area(approach.mapId()) != null, "Fake dungeon destination");
                    boolean boss = false, loot = false;
                    for (int floor = 1; floor <= 4; floor++) {
                        String mapId = approach.mapId().replaceFirst("_1$", "_" + floor);
                        if (world.area(mapId) == null) continue;
                        boss |= world.dungeonEncounterSlots(mapId).stream().anyMatch(slot -> slot.boss());
                        loot |= world.area(mapId).props.stream().anyMatch(ChestSystem::isChest);
                    }
                    require(boss, "Dungeon route has no boss: " + approach.label());
                    require(loot, "Dungeon route has no loot chest: " + approach.label());
                    dungeons++;
                } else if (approach.kind().equals("cache")) {
                    WorldProp chest = area.propsAt(target.x(), target.y()).stream().filter(ChestSystem::isChest).findFirst().orElseThrow();
                    ChestSystem storage = new ChestSystem();
                    require(!storage.contents(area.id, chest).isEmpty(), "Empty reward on first visit");
                    storage.contents(area.id, chest).clear();
                    Properties saved = new Properties(); storage.write(saved);
                    ChestSystem loaded = new ChestSystem(); loaded.read(saved);
                    require(loaded.contents(area.id, chest).isEmpty(), "Loot respawned after loading");
                    caches++;
                } else quests++;
                if (approach.gapLength() > 0) gaps++;
            }
            require(dungeons > 0 && caches > 0 && quests > 0 && gaps > 0, "Missing destination variety");
            for (var site : area.natureSites()) require(NatureSiteGenerator.walkable(world, site.trail()), "Quiet trail obstructed");
            var assets = new AssetStore(Path.of("assets").toAbsolutePath());
            for (String asset : List.of("interior_ironbound_chest", "deco_road_signpost", "deco_road_milestone",
                    "location_camp_crates", "location_farmland_fence", "location_farmland_fence_side"))
                require(assets.hasSprite(asset), "Missing route asset: " + asset);
            require(approaches.equals(new WorldMap(seed).area(area.id).destinationApproaches()), "Non-deterministic approaches");
            System.out.printf("Seed %d: %d dungeon/camp routes, %d quest/place routes, %d caches, %d worn gaps.%n",
                    seed, dungeons, quests, caches, gaps);
        }
        System.out.println("Destination approaches passed: real destinations, collision, determinism and persistent loot.");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
