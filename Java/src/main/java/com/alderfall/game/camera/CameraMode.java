package com.alderfall.game.camera;

public enum CameraMode {
    LOCKED("locked", "Locked"),
    SMOOTH("smooth", "Smooth"),
    LOOK_AHEAD("look_ahead", "Look-Ahead"),
    DEAD_ZONE("dead_zone", "Dead Zone");

    public final String key;
    public final String label;

    CameraMode(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public static CameraMode fromKey(String key) {
        for (CameraMode mode : values()) {
            if (mode.key.equals(key)) {
                return mode;
            }
        }
        return SMOOTH;
    }
}
