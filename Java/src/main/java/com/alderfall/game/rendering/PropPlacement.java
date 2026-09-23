package com.alderfall.game;

import com.alderfall.game.map.WorldMap;

/** Visual anchors in tile units. Gameplay ownership remains on the original tile. */
public final class PropPlacement {
    private PropPlacement() { }

    public enum Kind { FIXED, TREE, ROCK, COVER, CLUTTER }
    public record Placement(double x, double y, double scale, Kind kind) {
        public double footY(WorldProp prop) { return prop.y() + y; }
    }
    private static final Placement FIXED = new Placement(0.5, 1.0, 1.0, Kind.FIXED);
    private static final java.util.Map<String, Kind> KINDS = new java.util.concurrent.ConcurrentHashMap<>();

    public static Placement at(WorldMap world, String mapId, WorldProp prop) {
        if (!usesOffsets(world, mapId)) return FIXED;
        Kind kind = kind(prop.asset());
        if (kind == Kind.FIXED) return FIXED;
        int seed = prop.x() * 73428767 ^ prop.y() * 912931 ^ prop.asset().hashCode() ^ mapId.hashCode();
        // A bounded root footprint prevents neighboring tile rows from collapsing into each other.
        double radius = kind == Kind.TREE ? 0.23 : kind == Kind.COVER ? 0.14 : 0.20;
        double minX = radius, maxX = 1 - radius;
        double minY = kind == Kind.TREE ? 0.48 : 0.32, maxY = 1 - radius;
        char ground = world.tileAt(mapId, prop.x(), prop.y());
        if (protectedEdge(world.tileAt(mapId, prop.x() - 1, prop.y()), ground)) minX = Math.max(minX, 0.42);
        if (protectedEdge(world.tileAt(mapId, prop.x() + 1, prop.y()), ground)) maxX = Math.min(maxX, 0.58);
        if (protectedEdge(world.tileAt(mapId, prop.x(), prop.y() - 1), ground)) minY = Math.max(minY, 0.50);
        if (protectedEdge(world.tileAt(mapId, prop.x(), prop.y() + 1), ground)) maxY = Math.min(maxY, 0.60);
        double x = minX + random(seed) * (maxX - minX);
        double y = minY + random(seed ^ 0x51ed270b) * (maxY - minY);
        // Five sizes per family bound the sprite/shadow caches as well as the visual variation.
        int step = (int) (random(seed ^ 0x6d2b79f5) * 5);
        double scale = switch (kind) {
            case TREE -> 0.84 + step * 0.06;
            case ROCK -> 0.82 + step * 0.08;
            case COVER -> 0.76 + step * 0.09;
            case CLUTTER -> 0.92 + step * 0.03;
            default -> 1;
        };
        return new Placement(x, y, scale, kind);
    }

    public static boolean usesOffsets(WorldMap world, String mapId) {
        return WorldMap.OVERWORLD_ID.equals(mapId)
                || ("village".equals(world.kind(mapId)) && !WorldMap.PLAYER_VILLAGE_ID.equals(mapId)
                && !mapId.startsWith("editor_") && !mapId.equals("player_village"));
    }

    public static Kind kind(String asset) {
        return KINDS.computeIfAbsent(asset, PropPlacement::classify);
    }

    private static Kind classify(String asset) {
        if (asset.equals("location_camp_crates") || asset.equals("location_camp_barrels")) return Kind.CLUTTER;
        if (!asset.startsWith("deco_")) return Kind.FIXED;
        // Authored landmarks and repeated construction pieces must retain their alignment and size.
        for (String word : new String[]{"shrine", "rune", "cairn", "marker", "signpost", "milestone", "totem",
                "camp", "bridge", "charter", "bell", "altar", "gate", "wall", "fence", "elder", "ward"}) {
            if (asset.contains(word)) return Kind.FIXED;
        }
        for (String word : new String[]{"rock", "stone", "boulder", "crystal", "log", "stump", "woodpile"}) {
            if (asset.contains(word)) return Kind.ROCK;
        }
        if (asset.contains("root")) return Kind.COVER;
        if (asset.contains("tree") || asset.contains("pine") || asset.contains("willow")) return Kind.TREE;
        for (String word : new String[]{"soft_", "grass", "flower", "bloom", "fern", "bush", "mushroom",
                "reed", "cattail", "plant", "lily", "duckweed", "floating", "weed", "leaf", "scrub", "pebble", "moss"}) {
            if (asset.contains(word)) return Kind.COVER;
        }
        return Kind.FIXED;
    }

    private static boolean protectedEdge(char neighbor, char ground) {
        return Terrain.connectingRoad(neighbor) || neighbor == 'c' || neighbor == 'u' || neighbor == 'd'
                || (water(neighbor) != water(ground));
    }
    private static boolean water(char tile) { return tile == 'w' || tile == '~'; }
    private static double random(int seed) {
        int value = (seed ^ (seed >>> 16)) * 0x7feb352d;
        value = (value ^ (value >>> 15)) * 0x846ca68b;
        return ((value ^ (value >>> 16)) & 0x7fffffff) / 2147483648.0;
    }
}
