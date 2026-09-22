package com.alderfall.game.render.world;

import com.alderfall.game.map.WorldMap;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.HashMap;
import java.util.Map;

/** Cached room visibility. Doors transmit light; solid walls and void stop it. */
final class InteriorLightField {
    private final Map<String, Shape> visibility = new HashMap<>();
    private String mapId = "";
    private long geometry;
    private Shape floor;

    void update(WorldMap world, String id, int tileSize) {
        long hash = tileSize;
        for (int y = 0; y < world.height(id); y++) {
            for (int x = 0; x < world.width(id); x++) hash = hash * 31 + world.tileAt(id, x, y);
        }
        if (id.equals(mapId) && geometry == hash) return;
        mapId = id; geometry = hash; visibility.clear();
        Path2D shape = new Path2D.Double();
        for (int y = 0; y < world.height(id); y++) {
            for (int x = 0; x < world.width(id); x++) {
                if (!blocks(world.tileAt(id, x, y))) {
                    shape.append(new java.awt.Rectangle(x * tileSize, y * tileSize, tileSize, tileSize), false);
                }
            }
        }
        floor = shape;
    }

    Shape floor() { return floor; }

    Shape visible(WorldMap world, double sourceX, double sourceY, int radius, int tileSize) {
        String key = sourceX + ":" + sourceY + ":" + radius;
        return visibility.computeIfAbsent(key, ignored -> {
            double ox = sourceX, oy = sourceY;
            int tx = (int) Math.floor(ox / tileSize), ty = (int) Math.floor(oy / tileSize);
            // Wall lamps and windows emit into the room, just beyond their mounting face.
            if (blocks(world.tileAt(mapId, tx, ty))) {
                if (!blocks(world.tileAt(mapId, tx, ty + 1))) oy = (ty + 1.02) * tileSize;
                else if (!blocks(world.tileAt(mapId, tx, ty - 1))) oy = (ty - 0.02) * tileSize;
            }
            Path2D polygon = new Path2D.Double();
            double step = Math.max(1, tileSize / 12.0);
            for (int ray = 0; ray < 192; ray++) {
                double angle = ray * Math.PI * 2 / 192;
                double dx = Math.cos(angle), dy = Math.sin(angle);
                double distance = step;
                for (; distance < radius; distance += step) {
                    int x = (int) Math.floor((ox + dx * distance) / tileSize);
                    int y = (int) Math.floor((oy + dy * distance) / tileSize);
                    if (blocks(world.tileAt(mapId, x, y))) break;
                }
                double x = ox + dx * Math.min(radius, distance);
                double y = oy + dy * Math.min(radius, distance);
                if (ray == 0) polygon.moveTo(x, y); else polygon.lineTo(x, y);
            }
            polygon.closePath();
            return polygon;
        });
    }

    static boolean blocks(char tile) { return tile == 'o' || tile == 'x'; }
}
