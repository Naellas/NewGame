package com.alderfall.game.map;

public record WorldMapViewport(int screenX, int screenY, int screenW, int screenH,
                               double worldX, double worldY, double worldW, double worldH) {
    public static WorldMapViewport full() {
        return new WorldMapViewport(0, 0, 1, 1, 0.0, 0.0, WorldMap.COLS, WorldMap.ROWS);
    }
}
