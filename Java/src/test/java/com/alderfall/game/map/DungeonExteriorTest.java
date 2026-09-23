package com.alderfall.game.map;

import com.alderfall.game.TilePoint;
import com.alderfall.game.WorldProp;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

/** Regional placement, surface access, and actual (not fallback) artwork regression. */
public final class DungeonExteriorTest {
    public static void main(String[] args) throws Exception {
        Set<String> assets = new HashSet<>();
        try (var files = Files.walk(Path.of("assets"))) {
            files.filter(p -> p.toString().endsWith(".png"))
                    .forEach(p -> assets.add(p.getFileName().toString().replace(".png", "")));
        }
        for (String name : new String[]{"sand", "ice", "vampire", "arcane"}) {
            var image = ImageIO.read(Path.of("assets/environments/locations/location_exterior_" + name + "_entrance.png").toFile());
            require(image.getColorModel().hasAlpha() && (image.getRGB(0, 0) >>> 24) == 0,
                    "Entrance must have real alpha: " + name);
        }
        for (long seed : new long[]{0, 1, 42, 2026, -17}) {
            WorldMap world = new WorldMap(seed);
            for (var a : world.adventureMarkers()) for (var b : world.adventureMarkers()) {
                if (a.mapId().equals(b.mapId())) continue;
                double distance = Math.hypot(a.x() - b.x(), a.y() - b.y());
                require(distance >= DungeonExteriorCatalog.MIN_SITE_DISTANCE,
                        "Dungeons too close: " + a.label() + " / " + b.label() + " = " + distance);
            }
            for (var spec : DungeonExteriorCatalog.NEW_SITES) {
                var marker = world.adventureMarkers().stream().filter(m -> m.mapId().equals(spec.id())).findFirst().orElseThrow();
                var context = world.dungeonContext(marker.mapId());
                require(world.kingdomAt(marker.x(), marker.y()).id().equals(world.kingdomAt(spec.x(), spec.y()).id()),
                        "Placement escaped its intended region: " + spec.label());
                require(context != null && spec.biomes().indexOf(context.exterior()) >= 0,
                        "Wrong biome: " + spec.label() + " seed=" + seed + " context=" + context);
                String climate = DungeonExteriorCatalog.climate(context.exterior());
                String entrance = DungeonExteriorCatalog.entrance(marker.kind(), spec.intent(), climate);
                require(world.props(WorldMap.OVERWORLD_ID).stream().anyMatch(p -> p.x() == marker.x()
                        && p.y() == marker.y() && p.asset().equals(entrance)), "Missing entrance: " + spec.label());
                var transition = world.transitionAt(WorldMap.OVERWORLD_ID, marker.x(), marker.y());
                require(transition != null && transition.targetMapId().equals(spec.id()), "Broken entrance transition");
                var back = world.transitionAt(spec.id(), transition.targetX(), transition.targetY());
                require(back != null && back.targetMapId().equals(WorldMap.OVERWORLD_ID)
                        && back.targetX() == marker.x() && back.targetY() == marker.y(), "Broken return transition");
                require(reachesOutside(world, marker), "Surface entrance is enclosed: " + spec.label());
                var blueprint = OverworldLocationBlueprints.forSite(marker.kind(), 0, spec.intent(), climate);
                for (var slot : blueprint.slots()) for (String asset : slot.assets()) require(assets.contains(asset), "Missing " + asset);
                for (String asset : blueprint.sideAssets()) require(assets.contains(asset), "Missing " + asset);
                for (String asset : blueprint.scatterAssets()) require(assets.contains(asset), "Missing " + asset);
                if (seed == 0) System.out.println(spec.label() + " | " + marker.x() + "," + marker.y()
                        + " | " + world.kingdomAt(marker.x(), marker.y()).id() + " | " + climate);
            }
            System.out.println("DungeonExteriorTest seed=" + seed + " passed (" + world.adventureMarkers().size() + " sites)");
        }
        WorldMap first = new WorldMap(42), second = new WorldMap(42);
        require(first.adventureMarkers().equals(second.adventureMarkers()), "Non-deterministic placements");
        require(first.props(WorldMap.OVERWORLD_ID).equals(second.props(WorldMap.OVERWORLD_ID)), "Non-deterministic scenery");
    }

    private static boolean reachesOutside(WorldMap world, WorldMap.AdventureMarker marker) {
        Set<TilePoint> visited = new HashSet<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        queue.add(new TilePoint(marker.x(), marker.y()));
        while (!queue.isEmpty()) {
            TilePoint p = queue.removeFirst();
            if (!visited.add(p) || !world.isPassable(WorldMap.OVERWORLD_ID, p.x(), p.y())) continue;
            if (Math.abs(p.x() - marker.x()) + Math.abs(p.y() - marker.y()) >= 12) return true;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) queue.add(new TilePoint(p.x() + d[0], p.y() + d[1]));
        }
        return false;
    }
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }
}
