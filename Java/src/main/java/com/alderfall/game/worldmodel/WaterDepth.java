package com.alderfall.game;

/** Outdoor water depth, derived from the bank so existing maps and saves gain shelves. */
public enum WaterDepth {
    DRY("Dry ground", 0, 1.0),
    SHALLOW("Shallow water", 1, 1.2),
    WADING("Waist-deep water", 2, 1.65),
    DEEP("Deep water", 3, 1.0);

    public final String label;
    public final int level;
    public final double movementCost;

    WaterDepth(String label, int level, double movementCost) {
        this.label = label;
        this.level = level;
        this.movementCost = movementCost;
    }

    public boolean walkable() { return this == SHALLOW || this == WADING; }
}
