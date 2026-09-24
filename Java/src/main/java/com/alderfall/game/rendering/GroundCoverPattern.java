package com.alderfall.game;

/** Continuous patch coverage and stable, irregular sprite anchors in world units. */
public final class GroundCoverPattern {
    private GroundCoverPattern() { }
    public record Patch(double coverage, int seed) { }

    public static PropPlacement.Placement anchor(String map, int x, int y, int slot) {
        int seed = mix(x * 73428767 ^ y * 912931 ^ map.hashCode() ^ slot * 1709);
        return new PropPlacement.Placement(.06 + random(seed) * .88,
                .12 + random(seed ^ 0x51ed270b) * .80,
                moduleScale(random(seed ^ 0x6d2b79f5)), PropPlacement.Kind.COVER);
    }

    private static double moduleScale(double roll) {
        // Bounded sizes keep the sprite cache small: tiny accents, common clumps, rare tall clusters.
        if (roll < .18) return .58;
        if (roll < .38) return .78;
        if (roll < .66) return 1.0;
        if (roll < .88) return 1.24;
        return 1.52;
    }

    public static Patch patch(double x, double y, int salt) {
        // Warped, overlapping lobes share coverage across tile and six-tile cell boundaries.
        double wx = x + .55 * Math.sin(y * .91 + salt % 37);
        double wy = y + .45 * Math.sin(x * .73 + salt % 29);
        int cellX = (int) Math.floor(wx / 6), cellY = (int) Math.floor(wy / 6);
        double coverage = 0;
        int dominant = 0;
        for (int cy = cellY - 1; cy <= cellY + 1; cy++) for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
            int seed = mix(cx * 73428767 ^ cy * 912931 ^ salt);
            if (random(seed) < .30) continue;
            double centerX = cx * 6 + 1 + random(seed ^ 713) * 4;
            double centerY = cy * 6 + 1 + random(seed ^ 1709) * 4;
            double radius = 2.8 + random(seed ^ 1237) * 2;
            double stretch = .65 + random(seed ^ 937) * .7;
            double distance = Math.hypot(wx - centerX, (wy - centerY) / stretch);
            double t = Math.max(0, 1 - distance / radius);
            double strength = Math.min(1, t * t * (3 - 2 * t) * 2.4);
            if (strength > coverage) { coverage = strength; dominant = seed; }
        }
        return new Patch(coverage, dominant);
    }

    private static int mix(int seed) {
        int value = (seed ^ (seed >>> 16)) * 0x7feb352d;
        value = (value ^ (value >>> 15)) * 0x846ca68b;
        return value ^ (value >>> 16);
    }
    private static double random(int seed) { return (mix(seed) & 0x7fffffff) / 2147483648.0; }
}
