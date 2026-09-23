package com.alderfall.game;

import com.alderfall.game.map.*;
import java.nio.file.Path;
import java.util.*;

public final class NatureSpacesTest {
    public static void main(String[] args) {
        AssetStore assets = new AssetStore(Path.of("assets").toAbsolutePath());
        for (char terrain : "gfsbnqvP".toCharArray()) for (int i = 1; i <= 8; i++)
            require(assets.hasSprite("deco_ground_" + NatureSiteGenerator.biome(terrain) + "_" + i), "Missing biome sprite");
        for (long seed : new long[]{42, 1337}) {
            WorldMap world = new WorldMap(seed);
            var area = world.area(WorldMap.OVERWORLD_ID);
            var sites = area.natureSites();
            require(sites.size() >= 5 && sites.size() < 160, "Unexpected site count: " + sites.size());
            for (var site : sites) {
                require(assets.hasSprite(site.focal()), "Missing focal asset");
                require(site.trail().size() >= 7 && site.trail().size() <= 17, "Unbounded trail");
                require(NatureSiteGenerator.walkable(world, site.trail()), "Blocked trail");
                require(world.locationKindAt(area.id, site.x(), site.y()) == null, "Site overlaps authored location");
                for (var other : sites) if (other != site)
                    require(Math.hypot(site.x() - other.x(), site.y() - other.y()) >= 23, "Crowded sites");
            }
            var signature = sites.toString();
            require(signature.equals(new WorldMap(seed).area(area.id).natureSites().toString()), "Non-deterministic sites");
            long details = area.props.stream().filter(p -> p.asset().startsWith("deco_ground_")).count();
            System.out.println("Seed " + seed + ": " + sites.size() + " sites, " + details + " cached ground sprites; paths clear.");
        }
        System.out.println("Nature spaces checks passed.");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
