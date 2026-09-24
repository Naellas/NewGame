package com.alderfall.game;

/** Contact cadence follows actual travel, so walls, idle poses and teleports stay silent. */
final class FootstepTracker {
    private double x = Double.NaN, y, phase;
    private final SoundVariations variations = new SoundVariations();
    private int contactCooldown;

    boolean advance(double nextX, double nextY, double speed) {
        if (contactCooldown > 0) contactCooldown--;
        double dx = nextX - x, dy = nextY - y;
        double distance = Math.hypot(dx, dy);
        x = nextX; y = nextY;
        if (!Double.isFinite(distance) || distance > 2) { phase = 0; contactCooldown = 0; return false; }
        if (distance < .0001) return false;
        boolean grounded = System.getProperty("alderfall.movementStyle", "grounded").equals("grounded");
        double stride = grounded ? LocomotionClock.strideTiles(dx, dy) : LocomotionClock.STRIDE_TILES;
        phase += distance * 2 / stride * (grounded ? LocomotionClock.groundedCadence(speed, dx, dy)
                : GameConfig.clampWalkAnimationSpeed(speed));
        if (phase < 1) return false;
        phase %= 1;
        if (contactCooldown > 0) return false;
        contactCooldown = Math.max(1, (150 + GameConfig.FPS_MS - 1) / GameConfig.FPS_MS);
        return true;
    }

    String sample(char terrain) {
        return variations.next("foot_" + surface(terrain), 8);
    }

    static String surface(char tile) {
        return switch (tile) {
            case 'g', 'f', 'y', 'M' -> "grass";
            case 's', 'P' -> "sand";
            case 'n' -> "snow";
            case 'v', '~', 'w', 'Y', 'W' -> "water";
            case 'B', 'U', '7', 'i', 'k' -> "wood";
            case 'z' -> "cloth";
            case 'r', 'T', 'V', '8', 'A', 'b', 'u', 'e' -> "dirt";
            default -> "stone";
        };
    }
}
