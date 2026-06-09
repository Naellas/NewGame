package com.alderfall.game;

record PlacementPulse(String mapId, int x, int y, int startFrame) {
    static final int DURATION = 22;
}
