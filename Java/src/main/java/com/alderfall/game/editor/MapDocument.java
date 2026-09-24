package com.alderfall.game.editor;

import com.alderfall.game.*;
import com.alderfall.game.map.MapArea;
import com.alderfall.game.map.WorldMap;
import java.awt.Rectangle;
import java.util.*;

/** Editable, detached map data. Runtime objects are always copies. */
public final class MapDocument {
    public static final int MAX_SIZE = 160;
    public static final String TILES = "gfsnvbP~mqABwrTKcu dpjlayCGVU78DFMRSLNEIJHQ123456OYWXZthxiezko".replace(" ", "");
    public String label;
    public final String kind;
    public final char[][] tiles;
    public final List<WorldProp> props = new ArrayList<>();
    public final List<CityBuilding> buildings = new ArrayList<>();
    public final Map<TilePoint, String> landmarks = new LinkedHashMap<>();
    public int spawnX, spawnY;
    public long seed;
    public boolean prefab;

    public MapDocument(String label, String kind, int width, int height, char fill) {
        if (width < 1 || height < 1 || width > MAX_SIZE || height > MAX_SIZE)
            throw new IllegalArgumentException("Map dimensions must be 1–160 tiles.");
        if (!Set.of("city", "village", "dungeon", "interior").contains(kind))
            throw new IllegalArgumentException("Unknown map kind: " + kind);
        this.label = label; this.kind = kind;
        tiles = new char[height][width];
        for (char[] row : tiles) Arrays.fill(row, fill);
        spawnX = width / 2; spawnY = height / 2;
    }
    public int width() { return tiles[0].length; }
    public int height() { return tiles.length; }
    public boolean contains(int x, int y) { return x >= 0 && y >= 0 && x < width() && y < height(); }
    public MapDocument copy() {
        MapDocument d = new MapDocument(label, kind, width(), height(), 'g');
        for (int y = 0; y < height(); y++) d.tiles[y] = tiles[y].clone();
        d.props.addAll(props); d.buildings.addAll(buildings); d.landmarks.putAll(landmarks);
        d.spawnX = spawnX; d.spawnY = spawnY; d.seed = seed; d.prefab = prefab;
        return d;
    }
    public void install(WorldMap world, String id) {
        MapArea area = new MapArea(id, label, kind, copy().tiles);
        area.props.addAll(props); area.landmarks.putAll(landmarks);
        world.installEditorMap(area, buildings);
    }
    public int propIndex(int x, int y) {
        for (int i = props.size() - 1; i >= 0; i--) if (props.get(i).x() == x && props.get(i).y() == y) return i;
        return -1;
    }
    public int buildingIndex(int x, int y) {
        for (int i = buildings.size() - 1; i >= 0; i--) if (buildings.get(i).contains(x, y)) return i;
        return -1;
    }
    public void paint(int x, int y, int size, char tile) {
        for (int yy = y - size / 2; yy < y - size / 2 + size; yy++)
            for (int xx = x - size / 2; xx < x - size / 2 + size; xx++)
                if (contains(xx, yy)) tiles[yy][xx] = tile;
    }
    public void fill(int x, int y, char tile) {
        if (!contains(x, y) || tiles[y][x] == tile) return;
        char from = tiles[y][x];
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        queue.add(new TilePoint(x, y)); tiles[y][x] = tile;
        while (!queue.isEmpty()) {
            TilePoint p = queue.remove();
            for (int[] delta : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int xx = p.x() + delta[0], yy = p.y() + delta[1];
                if (contains(xx, yy) && tiles[yy][xx] == from) { tiles[yy][xx] = tile; queue.add(new TilePoint(xx, yy)); }
            }
        }
    }
    public boolean placeBuilding(String style, int x, int y, CityBuilding.Facing facing) {
        requireSettlement();
        var plan = VillageManager.buildingPlan(style);
        int w = plan.width(), h = plan.depth();
        if (facing == CityBuilding.Facing.EAST || facing == CityBuilding.Facing.WEST) { int tmp = w; w = h; h = tmp; }
        if (!contains(x, y) || !contains(x + w - 1, y + h - 1)) return false;
        Rectangle box = new Rectangle(x, y, w, h);
        for (CityBuilding b : buildings) if (box.intersects(rect(b))) return false;
        buildings.add(new CityBuilding(UUID.randomUUID().toString(), x, y, x + w - 1, y + h - 1, style, 0, facing));
        return true;
    }
    private void requireSettlement() {
        if (!kind.equals("city") && !kind.equals("village"))
            throw new IllegalArgumentException("Functional building lots belong on city or village maps. Use props and terrain for interiors and dungeons.");
    }
    public void faceBuilding(int index, CityBuilding.Facing facing) {
        CityBuilding b=buildings.get(index);
        boolean oldSideways=b.facing()==CityBuilding.Facing.EAST || b.facing()==CityBuilding.Facing.WEST;
        boolean newSideways=facing==CityBuilding.Facing.EAST || facing==CityBuilding.Facing.WEST;
        int w=oldSideways==newSideways?b.width():b.depth(), h=oldSideways==newSideways?b.depth():b.width();
        Rectangle footprint=new Rectangle(b.x1(),b.y1(),w,h);
        if (!contains(b.x1()+w-1,b.y1()+h-1)) throw new IllegalArgumentException("Rotated building does not fit in the map.");
        for (int i=0;i<buildings.size();i++) if(i!=index && footprint.intersects(rect(buildings.get(i))))
            throw new IllegalArgumentException("Rotated building overlaps another lot.");
        buildings.set(index,new CityBuilding(b.key(),b.x1(),b.y1(),b.x1()+w-1,b.y1()+h-1,b.style(),b.palette(),facing));
    }
    public static Rectangle rect(CityBuilding b) { return new Rectangle(b.x1(), b.y1(), b.width(), b.depth()); }
    public MapDocument extract(Rectangle r) {
        if (!contains(r.x, r.y) || !contains(r.x + r.width - 1, r.y + r.height - 1))
            throw new IllegalArgumentException("Selection is outside the map.");
        MapDocument d = new MapDocument(label + " prefab", kind, r.width, r.height, 'g');
        d.prefab = true; d.seed = seed;
        for (int y = 0; y < r.height; y++) System.arraycopy(tiles[r.y + y], r.x, d.tiles[y], 0, r.width);
        for (WorldProp p : props) if (r.contains(p.x(), p.y())) d.props.add(new WorldProp(p.x()-r.x, p.y()-r.y, p.asset(), p.size(), p.visualSlot(), p.offsetX(), p.offsetY()));
        for (CityBuilding b : buildings) {
            if (r.intersects(rect(b)) && !r.contains(rect(b))) throw new IllegalArgumentException("Selection cuts through a building. Include its entire footprint.");
            if (r.contains(rect(b))) d.buildings.add(translate(b, -r.x, -r.y));
        }
        landmarks.forEach((p, s) -> { if (r.contains(p.x(), p.y())) d.landmarks.put(new TilePoint(p.x()-r.x, p.y()-r.y), s); });
        d.spawnX = Math.max(0, Math.min(r.width-1, spawnX-r.x)); d.spawnY = Math.max(0, Math.min(r.height-1, spawnY-r.y));
        return d;
    }
    public void stamp(MapDocument source, int x, int y, boolean terrain) {
        if (!source.buildings.isEmpty()) requireSettlement();
        if (!contains(x, y) || !contains(x+source.width()-1, y+source.height()-1))
            throw new IllegalArgumentException("The entire prefab must fit inside the map.");
        List<CityBuilding> additions = source.buildings.stream().map(b -> translate(b, x, y)).toList();
        for (CityBuilding a : additions) for (CityBuilding b : buildings)
            if (rect(a).intersects(rect(b))) throw new IllegalArgumentException("Prefab overlaps an existing building.");
        if (terrain) for (int yy=0; yy<source.height(); yy++) System.arraycopy(source.tiles[yy], 0, tiles[y+yy], x, source.width());
        for (WorldProp p : source.props) props.add(new WorldProp(p.x()+x, p.y()+y, p.asset(), p.size(), p.visualSlot(), p.offsetX(), p.offsetY()));
        buildings.addAll(additions);
        source.landmarks.forEach((p,s) -> landmarks.put(new TilePoint(p.x()+x, p.y()+y), s));
    }
    private static int rotatedSlot(int slot){if(slot<128||slot>131)return slot;int q=slot-128;return 128+(1-q/2)+(q%2)*2;}
    public MapDocument rotate() {
        MapDocument d = new MapDocument(label, kind, height(), width(), 'g');
        d.prefab = prefab; d.seed = seed;
        for (int y=0; y<height(); y++) for (int x=0; x<width(); x++) d.tiles[x][height()-1-y] = tiles[y][x];
        for (WorldProp p : props) d.props.add(new WorldProp(height()-1-p.y(), p.x(), p.asset(), p.size(), rotatedSlot(p.visualSlot()), -p.offsetY(), p.offsetX()));
        for (CityBuilding b : buildings) d.buildings.add(new CityBuilding(b.key(), height()-1-b.y2(), b.x1(), height()-1-b.y1(), b.x2(), b.style(), b.palette(), clockwise(b.facing())));
        landmarks.forEach((p,s) -> d.landmarks.put(new TilePoint(height()-1-p.y(), p.x()), s));
        d.spawnX=height()-1-spawnY; d.spawnY=spawnX;
        return d;
    }
    public static CityBuilding.Facing clockwise(CityBuilding.Facing f) {
        return switch (f) { case SOUTH -> CityBuilding.Facing.WEST; case WEST -> CityBuilding.Facing.NORTH; case NORTH -> CityBuilding.Facing.EAST; case EAST -> CityBuilding.Facing.SOUTH; };
    }
    public static CityBuilding translate(CityBuilding b, int x, int y) {
        return new CityBuilding(UUID.randomUUID().toString(), b.x1()+x, b.y1()+y, b.x2()+x, b.y2()+y, b.style(), b.palette(), b.facing());
    }
    public boolean sameContent(MapDocument b) {
        return label.equals(b.label) && kind.equals(b.kind) && Arrays.deepEquals(tiles,b.tiles) && props.equals(b.props)
            && buildings.equals(b.buildings) && landmarks.equals(b.landmarks) && spawnX==b.spawnX && spawnY==b.spawnY && seed==b.seed && prefab==b.prefab;
    }
}
