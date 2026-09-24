package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.awt.geom.Rectangle2D;

/** Ground footprints in world tile units, independent of camera, zoom and canopy height. */
public final class PropCollision {
    public static final double PLAYER_RADIUS = 0.14;
    private PropCollision() { }

    public static Rectangle2D.Double footprint(WorldMap world, String mapId, WorldProp prop) {
        String asset = prop.asset();
        if (TownGardenArt.fountain(asset)) return new Rectangle2D.Double(prop.x()+prop.offsetX()/48.0, prop.y()+prop.offsetY()/48.0, 2, 2);
        if (ModularMarket.supports(asset)) return new Rectangle2D.Double(prop.x()+prop.offsetX()/48.0, prop.y()+prop.offsetY()/48.0, 2, 1);
        if(world.worldPropBlocksMovement(asset))return new Rectangle2D.Double(prop.x()+prop.offsetX()/48.0,prop.y()+prop.offsetY()/48.0,1,1);
        if (!asset.startsWith("deco_") || prop.visualSlot() >= 0) return null;
        var placement = PropPlacement.at(world, mapId, prop);
        var kind = PropPlacement.kind(asset);
        double width;
        double depth;
        if (kind == PropPlacement.Kind.TREE || asset.contains("tree_elder")) {
            width = 0.18;
            depth = 0.13;
        } else if (kind == PropPlacement.Kind.ROCK && !asset.contains("pebble")) {
            width = 0.46;
            depth = 0.25;
        } else if ((asset.contains("dense") && asset.contains("bush")) || asset.contains("thicket")) {
            width = 0.55;
            depth = 0.30;
        } else return null;
        double size = Math.max(1, prop.size()) / 48.0 * placement.scale();
        width *= size;
        depth *= size;
        // Sprite bottoms are ground anchors. Most of the base lies just above that anchor.
        return new Rectangle2D.Double(prop.x() + placement.x() + prop.offsetX()/48.0 - width / 2,
                prop.y() + placement.y() + prop.offsetY()/48.0 - depth * 0.8, width, depth);
    }

    public static boolean clear(WorldMap world, String mapId, double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) return false;
        double r = PLAYER_RADIUS;
        for (int ty = (int) Math.floor(y - r); ty <= (int) Math.floor(y + r); ty++) {
            for (int tx = (int) Math.floor(x - r); tx <= (int) Math.floor(x + r); tx++) {
                if (!world.isGroundPassable(mapId, tx, ty)) return false;
            }
        }
        for (CityBuilding building : world.cityBuildings(mapId)) {
            if (building.x2() + 2 < x - r || building.x1() - 1 > x + r
                    || building.y2() + 1 < y - r || building.y1() > y + r) continue;
            for (var base : world.buildingFootprints(mapId, building)) {
                if (base.intersects(x - r, y - r, r * 2, r * 2)) return false;
            }
        }
        int reach=mapId.startsWith("editor_")?8:3;
        for (WorldProp prop : world.propsInBounds(mapId, (int) Math.floor(x) - reach,
                (int) Math.floor(y) - reach, (int) Math.floor(x) + reach+1, (int) Math.floor(y) + reach+1)) {
            var bounds = footprint(world, mapId, prop);
            if (bounds != null && bounds.intersects(x - r, y - r, r * 2, r * 2)) return false;
        }
        return true;
    }

    /** Swept player box: catches thin obstacles even when both endpoints are clear. */
    public static boolean canTravel(WorldMap world, String mapId, double x1, double y1, double x2, double y2) {
        if (!Double.isFinite(x1 + y1 + x2 + y2)) return false;
        if (!world.elevation(mapId).canTravel(x1,y1,x2,y2)) return false;
        double r = PLAYER_RADIUS;
        int minX = (int) Math.floor(Math.min(x1, x2) - r), maxX = (int) Math.floor(Math.max(x1, x2) + r);
        int minY = (int) Math.floor(Math.min(y1, y2) - r), maxY = (int) Math.floor(Math.max(y1, y2) + r);
        for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) {
            if (!world.isGroundPassable(mapId, x, y)
                    && new Rectangle2D.Double(x - r, y - r, 1 + r * 2, 1 + r * 2).intersectsLine(x1, y1, x2, y2)) return false;
        }
        for (CityBuilding building : world.cityBuildings(mapId)) {
            if (building.x2() + 2 < minX || building.x1() - 1 > maxX + 1
                    || building.y2() + 1 < minY || building.y1() > maxY + 1) continue;
            for (var b : world.buildingFootprints(mapId, building)) {
                if (new Rectangle2D.Double(b.x - r, b.y - r, b.width + r * 2, b.height + r * 2)
                        .intersectsLine(x1, y1, x2, y2)) return false;
            }
        }
        int reach=mapId.startsWith("editor_")?8:3;
        for (WorldProp prop : world.propsInBounds(mapId, minX - reach, minY - reach, maxX + reach+1, maxY + reach+1)) {
            var b = footprint(world, mapId, prop);
            if (b != null && new Rectangle2D.Double(b.x - r, b.y - r, b.width + r * 2, b.height + r * 2)
                    .intersectsLine(x1, y1, x2, y2)) return false;
        }
        return true;
    }

    public record Anchor(double x, double y) { }

    /** Join a click route from the player's actual fractional position without snapping through a trunk. */
    public static java.util.List<Anchor> localRoute(WorldMap world, String mapId, Anchor start, Anchor target) {
        if (canTravel(world, mapId, start.x(), start.y(), target.x(), target.y())) return java.util.List.of(target);
        if (Math.hypot(start.x() - target.x(), start.y() - target.y()) > 3) return java.util.List.of();
        var nodes = new java.util.ArrayList<Anchor>();
        nodes.add(start);
        nodes.add(target);
        int minX = (int) Math.floor(Math.min(start.x(), target.x())) - 1;
        int minY = (int) Math.floor(Math.min(start.y(), target.y())) - 1;
        int maxX = (int) Math.floor(Math.max(start.x(), target.x())) + 1;
        int maxY = (int) Math.floor(Math.max(start.y(), target.y())) + 1;
        for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) {
            for (int slot = 0; slot < 4; slot++) {
                double px = x + 0.25 + (slot % 2) * 0.5, py = y + 0.25 + (slot / 2) * 0.5;
                if (clear(world, mapId, px, py)) nodes.add(new Anchor(px, py));
            }
        }
        double[] cost = new double[nodes.size()];
        int[] previous = new int[nodes.size()];
        boolean[] done = new boolean[nodes.size()];
        java.util.Arrays.fill(cost, Double.POSITIVE_INFINITY);
        java.util.Arrays.fill(previous, -1);
        cost[0] = 0;
        for (int step = 0; step < nodes.size(); step++) {
            int current = -1;
            for (int i = 0; i < nodes.size(); i++) if (!done[i] && Double.isFinite(cost[i])
                    && (current < 0 || cost[i] < cost[current])) current = i;
            if (current < 0 || current == 1) break;
            done[current] = true;
            Anchor a = nodes.get(current);
            for (int i = 1; i < nodes.size(); i++) {
                Anchor b = nodes.get(i);
                double distance = Math.hypot(a.x() - b.x(), a.y() - b.y());
                if (done[i] || distance > 0.76 || cost[current] + distance >= cost[i]) continue;
                if (canTravel(world, mapId, a.x(), a.y(), b.x(), b.y())) {
                    cost[i] = cost[current] + distance;
                    previous[i] = current;
                }
            }
        }
        if (previous[1] < 0) return java.util.List.of();
        var result = new java.util.ArrayList<Anchor>();
        for (int i = 1; i != 0; i = previous[i]) result.add(nodes.get(i));
        java.util.Collections.reverse(result);
        return result;
    }

    /** Prefer the old center, then a free quarter-tile slot for narrow passages. */
    public static Anchor navigationAnchor(WorldMap world, String mapId, int x, int y) {
        if (!world.isPassable(mapId, x, y)) return null;
        if (clear(world, mapId, x + 0.5, y + 0.5)) return new Anchor(x + 0.5, y + 0.5);
        for (int slot = 0; slot < 4; slot++) {
            double px = x + 0.25 + (slot % 2) * 0.5;
            double py = y + 0.25 + (slot / 2) * 0.5;
            if (clear(world, mapId, px, py)) return new Anchor(px, py);
        }
        return null;
    }
}
