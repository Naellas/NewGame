package com.alderfall.game;

public record CityBuilding(
        String key,
        int x1,
        int y1,
        int x2,
        int y2,
        String style,
        int palette
) {
    public int width() {
        return x2 - x1 + 1;
    }

    public int depth() {
        return y2 - y1 + 1;
    }

    public TilePoint anchor() {
        return new TilePoint(x1, y1);
    }

    public boolean contains(int x, int y) {
        return x >= x1 && x <= x2 && y >= y1 && y <= y2;
    }
}
