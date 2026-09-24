package com.alderfall.game.render.world;

import com.alderfall.game.AssetStore;
import com.alderfall.game.InteriorStyle;
import com.alderfall.game.map.WorldMap;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** Sheet-derived surfaces composed along exposed room boundaries. No corner sprites. */
public final class ConnectedInteriorWalls {
    private ConnectedInteriorWalls() { }

    public static String material(String mapId) {
        if (mapId.contains("interior_eastern")) return "eastern";
        if (mapId.contains("cistern_house") || mapId.contains("interior_stone")) return "stone";
        return InteriorStyle.forMap(mapId) == InteriorStyle.ARCHIVE ? "paneled" : "timber";
    }

    private static String asset(String material, String piece) {
        return "interior_module_" + material + "_" + piece;
    }

    /** Mirrored repeats share identical boundary pixels, even on negative coordinates. */
    public static void floor(Graphics2D g, AssetStore assets, String material,
                             int wx, int wy, int px, int py, int ts) {
        BufferedImage image = assets.image(asset(material, "floor"), ts, ts);
        boolean flipX = Math.floorMod(wx, 2) == 1, flipY = Math.floorMod(wy, 2) == 1;
        g.drawImage(image, px, py, px + ts, py + ts,
                flipX ? ts : 0, flipY ? ts : 0, flipX ? 0 : ts, flipY ? 0 : ts, null);
    }

    public static boolean floor(char tile) {
        return tile == 'i' || tile == 'z' || tile == 'k' || tile == 'e';
    }

    private static boolean wall(char tile) { return tile == 'o' || tile == 'e'; }

    /** The same silhouette drives terrain, foreground occlusion and wall mounting. */
    public static Rectangle bounds(WorldMap world, String map, int x, int y, int px, int py, int ts) {
        boolean n = wall(world.tileAt(map, x, y - 1)), s = wall(world.tileAt(map, x, y + 1));
        boolean e = wall(world.tileAt(map, x + 1, y)), w = wall(world.tileAt(map, x - 1, y));
        int rail = Math.max(4, ts / 3), margin = (ts - rail) / 2;
        if ((n || s) && !e && !w) return new Rectangle(px + margin, py, rail, ts);
        boolean rear = floor(world.tileAt(map, x, y + 1))
                || (!floor(world.tileAt(map, x, y - 1))
                && (e && floor(world.tileAt(map, x + 1, y + 1))
                || w && floor(world.tileAt(map, x - 1, y + 1))));
        int left = (n || s) && !w ? margin : 0;
        int right = (n || s) && !e ? ts - margin - rail : 0;
        return new Rectangle(px + left, rear ? py - ts : py, ts - left - right, rear ? ts * 2 : ts);
    }

    public static boolean side(WorldMap world, String map, int x, int y) {
        return (wall(world.tileAt(map, x, y - 1)) || wall(world.tileAt(map, x, y + 1)))
                && !wall(world.tileAt(map, x - 1, y)) && !wall(world.tileAt(map, x + 1, y));
    }

    public static com.alderfall.game.TilePoint mountAt(WorldMap world, String map, double x, double y) {
        int tx = (int) Math.floor(x), ty = (int) Math.floor(y);
        // A rear face projects one tile north of its saved wall anchor.
        for (int row = ty + 1; row >= ty; row--) {
            if (world.tileAt(map, tx, row) == 'o' && !side(world, map, tx, row)
                    && bounds(world, map, tx, row, tx * 48, row * 48, 48).contains(x * 48, y * 48))
                return new com.alderfall.game.TilePoint(tx, row);
        }
        return new com.alderfall.game.TilePoint(tx, ty);
    }

    public static void door(Graphics2D g, AssetStore assets, String material,
                            int px, int py, int ts, boolean horizontal, boolean start, boolean end) {
        int post = Math.max(3, ts / 8), rail = Math.max(4, ts / 3);
        if (horizontal) {
            if (start) g.drawImage(assets.image(asset(material, "post"), post, ts), px, py, null);
            if (end) g.drawImage(assets.image(asset(material, "post"), post, ts), px + ts - post, py, null);
            g.drawImage(assets.image(asset(material, "cap"), ts, post), px, py, null);
        } else {
            // Ends of a vertical wall terminate at an open threshold.
            int margin = (ts - rail) / 2;
            if (start) g.drawImage(assets.image(asset(material, "cap"), rail, post), px + margin, py, null);
            if (end) g.drawImage(assets.image(asset(material, "base"), rail, post), px + margin, py + ts - post, null);
        }
    }

    public static void draw(Graphics2D g, AssetStore assets, WorldMap world, String map,
                            int wx, int wy, int px, int py, int ts) {
        boolean n = wall(world.tileAt(map, wx, wy - 1));
        boolean e = wall(world.tileAt(map, wx + 1, wy));
        boolean s = wall(world.tileAt(map, wx, wy + 1));
        boolean w = wall(world.tileAt(map, wx - 1, wy));
        boolean fn = floor(world.tileAt(map, wx, wy - 1));
        boolean fs = floor(world.tileAt(map, wx, wy + 1));
        boolean fe = floor(world.tileAt(map, wx + 1, wy));
        boolean fw = floor(world.tileAt(map, wx - 1, wy));
        String material = material(map);
        int rail = Math.max(4, ts / 3), margin = (ts - rail) / 2;
        int trim = Math.max(2, ts / 8);
        boolean vertical = (n || s) && !e && !w;
        boolean rear = fs || (!fn && (e && floor(world.tileAt(map, wx + 1, wy + 1))
                || w && floor(world.tileAt(map, wx - 1, wy + 1))));
        if (vertical) {
            g.setColor(new Color(12, 13, 18));
            g.fillRect(px, py, ts, ts);
            // The visual cutaway is thinner than the collision cell. Continue the
            // neighboring floor up to its face instead of leaving a black gutter.
            Shape clip = g.getClip();
            if (fw) {
                g.clipRect(px, py, margin, ts);
                floor(g, assets, material, wx, wy, px, py, ts);
                g.setColor(InteriorStyle.forMap(map).materialTint);
                g.fillRect(px, py, ts, ts);
                g.setClip(clip);
            }
            if (fe) {
                g.clipRect(px + margin + rail, py, ts - margin - rail, ts);
                floor(g, assets, material, wx, wy, px, py, ts);
                g.setColor(InteriorStyle.forMap(map).materialTint);
                g.fillRect(px, py, ts, ts);
                g.setClip(clip);
            }
            BufferedImage side = assets.image(asset(material, "side"), rail, ts);
            boolean flip = Math.floorMod(wy, 2) == 1;
            g.drawImage(side, px + margin, py, px + margin + rail, py + ts,
                    0, flip ? ts : 0, rail, flip ? 0 : ts, null);
            g.setColor(InteriorStyle.forMap(map).materialTint);
            g.fillRect(px + margin, py, rail, ts);
            // Floor-facing edges only; no repeated horizontal end caps in a run.
            g.setColor(new Color(0, 0, 0, 70));
            if (fe) g.fillRect(px + margin + rail, py, Math.max(2, trim / 2), ts);
            if (fw) g.fillRect(px + margin - Math.max(2, trim / 2), py, Math.max(2, trim / 2), ts);
            if (!n) g.drawImage(assets.image(asset(material, "cap"), rail, trim), px + margin, py, null);
            if (!s) g.drawImage(assets.image(asset(material, "base"), rail, trim), px + margin, py + ts - trim, null);
            return;
        }

        // South-facing rear walls retain the two-tile face. Foreground walls are
        // cut down to one tile so their rooms and furniture remain readable.
        Rectangle face = bounds(world, map, wx, wy, px, py, ts);
        int top = face.y, height = face.height;
        g.setColor(new Color(12, 13, 18));
        g.fillRect(px, py, ts, ts);
        // Corner ends finish flush with the side rail, rather than projecting a
        // full block into the void. Continue the floor on the room side of the turn.
        Shape originalClip = g.getClip();
        if (fn || fs) {
            g.clipRect(px, py, ts, ts);
            floor(g, assets, material, wx, wy, px, py, ts);
            g.setColor(InteriorStyle.forMap(map).materialTint);
            g.fillRect(px, py, ts, ts);
            g.setClip(originalClip);
        }
        g.drawImage(assets.image(asset(material, "face"), face.width, height), face.x, top, null);
        g.drawImage(assets.image(asset(material, "cap"), face.width, trim), face.x, top, null);
        g.drawImage(assets.image(asset(material, "base"), face.width, trim), face.x, py + ts - trim, null);
        // Posts are structural: at ends and junctions, never on every repeat.
        int post = Math.max(3, ts / 8);
        if (!w) g.drawImage(assets.image(asset(material, "post"), post, height), face.x, top, null);
        if (!e) g.drawImage(assets.image(asset(material, "post"), post, height), face.x + face.width - post, top, null);
        if (n || s) {
            g.drawImage(assets.image(asset(material, "post"), post, height), px + (ts - post) / 2, top, null);
            // Matching cap width meets the vertical run at the south seam.
            if (s) g.drawImage(assets.image(asset(material, "side"), rail, trim), px + margin, py + ts - trim, null);
        }
        g.setColor(InteriorStyle.forMap(map).materialTint);
        g.fillRect(face.x, top, face.width, height);
    }
}
