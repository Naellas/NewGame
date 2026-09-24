package com.alderfall.game;

public record CityBuilding(
        String key,
        int x1,
        int y1,
        int x2,
        int y2,
        String style,
        int palette,
        Facing facing
) {
    public enum Facing {
        SOUTH(0, 1), WEST(-1, 0), EAST(1, 0), NORTH(0, -1);
        public final int dx, dy;
        Facing(int dx, int dy) { this.dx = dx; this.dy = dy; }
    }

    public CityBuilding(String key, int x1, int y1, int x2, int y2, String style, int palette) {
        this(key, x1, y1, x2, y2, style, palette, Facing.SOUTH);
    }

    public CityBuilding { if (facing == null) facing = Facing.SOUTH; }

    /** Local right/outward offsets, shared by entrances, paths and NPC standing spots. */
    public TilePoint outside(TilePoint door, int sideways, int distance) {
        return new TilePoint(door.x() + facing.dy * sideways + facing.dx * distance,
                door.y() - facing.dx * sideways + facing.dy * distance);
    }

    public boolean approachContains(TilePoint door, int x, int y, int length) {
        int dx = x - door.x(), dy = y - door.y();
        int outward = dx * facing.dx + dy * facing.dy;
        return outward >= 0 && outward <= length && Math.abs(dx * facing.dy - dy * facing.dx) <= 1;
    }
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
