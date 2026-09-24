package com.alderfall.game.map;

import com.alderfall.game.TilePoint;
import com.alderfall.game.WorldProp;
import com.alderfall.game.RegionalSettlementIdentity.District;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MapArea {
    public final String id;
    public final String label;
    public final String kind;
    public final char[][] tiles;
    private long visualRevision = 1L;
    private List<DestinationApproaches.Approach> destinationApproaches = List.of();
    private final Map<TilePoint, Character> approachGround = new HashMap<>();
    public List<DestinationApproaches.Approach> destinationApproaches() { return destinationApproaches; }
    public void setDestinationApproaches(List<DestinationApproaches.Approach> approaches) {
        destinationApproaches = List.copyOf(approaches); markVisualChange();
    }
    public char approachGroundAt(int x, int y, char fallback) {
        return approachGround.getOrDefault(new TilePoint(x, y), fallback);
    }
    public void setApproachGround(TilePoint point, char ground) {
        approachGround.put(point, ground); markVisualChange();
    }
    private List<TownLandPlots.Plot> townPlots = List.of();
    public List<TownLandPlots.Plot> townPlots() { return townPlots; }
    public void setTownPlots(List<TownLandPlots.Plot> plots) { townPlots = List.copyOf(plots); markVisualChange(); }
    private List<NatureSiteGenerator.Site> natureSites = List.of();
    public List<NatureSiteGenerator.Site> natureSites() { return natureSites; }
    public void setNatureSites(List<NatureSiteGenerator.Site> sites) {
        natureSites = List.copyOf(sites);
        markVisualChange();
    }
    public final Set<TilePoint> interiorRugs = new RevisionSet<>();
    /** Derived from the composed room plan on generation/load; independent of furniture collision. */
    public final Map<TilePoint, String> interiorFloorMaterials = new RevisionMap<>();
    public final Map<TilePoint, String> landmarks = new RevisionMap<>();
    public final List<WorldProp> props = new IndexedPropList();
    public final List<District> districts = new ArrayList<>();
    private final List<WorldProp> orderedProps = new ArrayList<>();
    private final Map<TilePoint, List<WorldProp>> propsByTile = new HashMap<>();

    public MapArea(String id, String label, String kind, char[][] tiles) {
        this.id = id;
        this.label = label;
        this.kind = kind;
        this.tiles = tiles;
        if ("interior".equals(kind)) {
            for (int y = 0; y < tiles.length; y++) for (int x = 0; x < tiles[y].length; x++) {
                if (tiles[y][x] == 'z') interiorRugs.add(new TilePoint(x, y));
            }
        }
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

    public boolean setTile(int x, int y, char tile) {
        if (x < 0 || y < 0 || x >= width() || y >= height() || tiles[y][x] == tile) {
            return false;
        }
        tiles[y][x] = tile;
        markVisualChange();
        return true;
    }

    public void fillTiles(int x1, int y1, int x2, int y2, char tile) {
        boolean changed = false;
        for (int y = Math.max(0, y1); y <= Math.min(height() - 1, y2); y++) {
            for (int x = Math.max(0, x1); x <= Math.min(width() - 1, x2); x++) {
                if (tiles[y][x] != tile) {
                    tiles[y][x] = tile;
                    changed = true;
                }
            }
        }
        if (changed) {
            markVisualChange();
        }
    }

    public long visualRevision() {
        return visualRevision;
    }

    public void markVisualChange() {
        visualRevision++;
    }

    public void invalidateAfter(long previousRevision) {
        visualRevision = Math.max(visualRevision, previousRevision) + 1;
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
        WorldProp moved = new WorldProp(x, y, prop.asset(), prop.size(), prop.visualSlot(), prop.offsetX(), prop.offsetY());
        addProp(moved);
        return moved;
    }

    public WorldProp propAt(int x, int y) {
        List<WorldProp> matches = propsByTile.get(new TilePoint(x, y));
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        for (int i = matches.size() - 1; i >= 0; i--) {
            if (matches.get(i).visualSlot() < 0) return matches.get(i);
        }
        return null;
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
        if (prop.visualSlot() >= 0) markVisualChange();
    }

    private void unindexProp(WorldProp prop) {
        TilePoint point = new TilePoint(prop.x(), prop.y());
        List<WorldProp> matches = propsByTile.get(point);
        if (matches == null) {
            return;
        }
        matches.remove(prop);
        if (prop.visualSlot() >= 0) markVisualChange();
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
            if (orderedProps.stream().anyMatch(prop -> prop.visualSlot() >= 0)) markVisualChange();
            orderedProps.clear();
            propsByTile.clear();
        }
    }

    private final class RevisionMap<K, V> extends HashMap<K, V> {
        @Override
        public V put(K key, V value) {
            boolean existed = containsKey(key);
            V previous = super.put(key, value);
            if (!existed || !Objects.equals(previous, value)) {
                markVisualChange();
            }
            return previous;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> values) {
            for (Map.Entry<? extends K, ? extends V> entry : values.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public V remove(Object key) {
            if (!containsKey(key)) {
                return null;
            }
            V removed = super.remove(key);
            markVisualChange();
            return removed;
        }

        @Override
        public void clear() {
            if (!isEmpty()) {
                super.clear();
                markVisualChange();
            }
        }
    }

    private final class RevisionSet<E> extends HashSet<E> {
        @Override
        public boolean add(E value) {
            boolean changed = super.add(value);
            if (changed) {
                markVisualChange();
            }
            return changed;
        }

        @Override
        public boolean addAll(Collection<? extends E> values) {
            boolean changed = false;
            for (E value : values) {
                changed |= add(value);
            }
            return changed;
        }

        @Override
        public boolean remove(Object value) {
            boolean changed = super.remove(value);
            if (changed) {
                markVisualChange();
            }
            return changed;
        }

        @Override
        public void clear() {
            if (!isEmpty()) {
                super.clear();
                markVisualChange();
            }
        }
    }
}
