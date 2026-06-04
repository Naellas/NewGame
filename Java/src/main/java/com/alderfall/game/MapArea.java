package com.alderfall.game;

import java.util.AbstractList;
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
    public final List<WorldProp> props = new IndexedPropList();
    private final List<WorldProp> orderedProps = new ArrayList<>();
    private final Map<TilePoint, List<WorldProp>> propsByTile = new HashMap<>();

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

    public void addProp(WorldProp prop) {
        props.add(prop);
    }

    public boolean removeProp(WorldProp prop) {
        return props.remove(prop);
    }

    public WorldProp moveProp(WorldProp prop, int x, int y) {
        if (!removeProp(prop)) {
            return null;
        }
        WorldProp moved = new WorldProp(x, y, prop.asset(), prop.size());
        addProp(moved);
        return moved;
    }

    public WorldProp propAt(int x, int y) {
        List<WorldProp> matches = propsByTile.get(new TilePoint(x, y));
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        return matches.get(matches.size() - 1);
    }

    public List<WorldProp> propsAt(int x, int y) {
        List<WorldProp> matches = propsByTile.get(new TilePoint(x, y));
        return matches == null ? List.of() : new ArrayList<>(matches);
    }

    public List<WorldProp> propsInBounds(int minX, int minY, int maxX, int maxY) {
        List<WorldProp> matches = new ArrayList<>();
        int boundedMinX = Math.max(0, minX);
        int boundedMinY = Math.max(0, minY);
        int boundedMaxX = Math.min(width(), maxX);
        int boundedMaxY = Math.min(height(), maxY);
        if (boundedMinX >= boundedMaxX || boundedMinY >= boundedMaxY) {
            return matches;
        }
        long tileCount = (long) (boundedMaxX - boundedMinX) * (boundedMaxY - boundedMinY);
        if (tileCount <= propsByTile.size()) {
            for (int y = boundedMinY; y < boundedMaxY; y++) {
                for (int x = boundedMinX; x < boundedMaxX; x++) {
                    List<WorldProp> tileProps = propsByTile.get(new TilePoint(x, y));
                    if (tileProps != null) {
                        matches.addAll(tileProps);
                    }
                }
            }
            return matches;
        }
        for (Map.Entry<TilePoint, List<WorldProp>> entry : propsByTile.entrySet()) {
            TilePoint point = entry.getKey();
            if (point.x() < minX || point.y() < minY || point.x() >= maxX || point.y() >= maxY) {
                continue;
            }
            matches.addAll(entry.getValue());
        }
        return matches;
    }

    public List<WorldProp> propsInTileOrder(int minX, int minY, int maxX, int maxY) {
        List<WorldProp> matches = new ArrayList<>();
        int boundedMinX = Math.max(0, minX);
        int boundedMinY = Math.max(0, minY);
        int boundedMaxX = Math.min(width(), maxX);
        int boundedMaxY = Math.min(height(), maxY);
        for (int y = boundedMinY; y < boundedMaxY; y++) {
            for (int x = boundedMinX; x < boundedMaxX; x++) {
                List<WorldProp> tileProps = propsByTile.get(new TilePoint(x, y));
                if (tileProps != null) {
                    matches.addAll(tileProps);
                }
            }
        }
        return matches;
    }

    private void indexProp(WorldProp prop) {
        propsByTile.computeIfAbsent(new TilePoint(prop.x(), prop.y()), ignored -> new ArrayList<>()).add(prop);
    }

    private void unindexProp(WorldProp prop) {
        TilePoint point = new TilePoint(prop.x(), prop.y());
        List<WorldProp> matches = propsByTile.get(point);
        if (matches == null) {
            return;
        }
        matches.remove(prop);
        if (matches.isEmpty()) {
            propsByTile.remove(point);
        }
    }

    private final class IndexedPropList extends AbstractList<WorldProp> {
        @Override
        public WorldProp get(int index) {
            return orderedProps.get(index);
        }

        @Override
        public int size() {
            return orderedProps.size();
        }

        @Override
        public void add(int index, WorldProp prop) {
            orderedProps.add(index, prop);
            indexProp(prop);
        }

        @Override
        public WorldProp remove(int index) {
            WorldProp removed = orderedProps.remove(index);
            unindexProp(removed);
            return removed;
        }

        @Override
        public WorldProp set(int index, WorldProp prop) {
            WorldProp previous = orderedProps.set(index, prop);
            unindexProp(previous);
            indexProp(prop);
            return previous;
        }

        @Override
        public void clear() {
            orderedProps.clear();
            propsByTile.clear();
        }
    }
}
