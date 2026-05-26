package com.alderfall.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MapArea {
    public final String id;
    public final String label;
    public final String kind;
    public final char[][] tiles;
    public final Map<TilePoint, String> landmarks = new HashMap<>();
    public final List<WorldProp> props = new ArrayList<>();

    public MapArea(String id, String label, String kind, char[][] tiles) {
        this.id = id;
        this.label = label;
        this.kind = kind;
        this.tiles = tiles;
    }

    public int width() {
        return tiles[0].length;
    }

    public int height() {
        return tiles.length;
    }

    public char tileAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width() || y >= height()) {
            return 'm';
        }
        return tiles[y][x];
    }
}
