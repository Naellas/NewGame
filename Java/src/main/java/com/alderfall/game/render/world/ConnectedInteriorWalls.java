package com.alderfall.game.render.world;

import com.alderfall.game.AssetStore;
import com.alderfall.game.InteriorStyle;
import com.alderfall.game.map.WorldMap;
import com.alderfall.game.map.InteriorFlooring;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** Sheet-derived surfaces composed along exposed room boundaries. No corner sprites. */
public final class ConnectedInteriorWalls {
    private ConnectedInteriorWalls() { }

    public static String material(String mapId) {
        if (mapId.contains("interior_rustic")) return "rustic";
        if (mapId.contains("interior_eastern")) return "eastern";
        if (mapId.contains("cistern_house") || mapId.contains("interior_stone")) return "stone";
        return InteriorStyle.forMap(mapId) == InteriorStyle.ARCHIVE ? "paneled" : "timber";
    }

    /** Rear faces inherit the finish of the room directly in front of them. */
    public static String wallMaterial(WorldMap world, String map, int x, int y) {
        String shell = material(map);
        if (world.tileAt(map, x, y) != 'o' || side(world, map, x, y)) return shell;
        if (floor(world.tileAt(map, x, y + 1)))
            return InteriorFlooring.material(world, map, x, y + 1, shell).equals("stone") ? "stone" : shell;
        // An outer turn has no floor directly below its spine. Continue the
        // adjacent room's finish into this last partial-width face only.
        if (!floor(world.tileAt(map, x, y - 1))) {
            for (int dx : new int[]{-1, 1}) {
                if (world.tileAt(map, x + dx, y) == 'o' && floor(world.tileAt(map, x + dx, y + 1))
                        && InteriorFlooring.material(world, map, x + dx, y + 1, shell).equals("stone")) return "stone";
            }
        }
        return shell;
    }

    private static String asset(String material, String piece) {
        if (material.equals("rustic") && !piece.equals("face") && !piece.equals("floor")) material = "timber";
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

    /** Stone owns the inset timber threshold, avoiding doubled strips at seams. */
    public static void floorBorders(Graphics2D g, AssetStore assets, WorldMap world, String map,
                                    int wx, int wy, int px, int py, int ts, String finish) {
        if (!finish.equals("stone")) return;
        String shell = material(map);
        int thickness = Math.max(2, ts / 8);
        BufferedImage strip = assets.image(asset("timber", "cap"), ts, thickness);
        for (int edge = 0; edge < 4; edge++) {
            int dx = edge == 1 ? 1 : edge == 3 ? -1 : 0;
            int dy = edge == 2 ? 1 : edge == 0 ? -1 : 0;
            if (!floor(world.tileAt(map, wx + dx, wy + dy))
                    || InteriorFlooring.material(world, map, wx + dx, wy + dy, shell).equals("stone")) continue;
            Graphics2D border = (Graphics2D) g.create();
            border.translate(px + (edge == 1 || edge == 2 ? ts : 0),
                    py + (edge == 2 || edge == 3 ? ts : 0));
            border.rotate(edge * Math.PI / 2);
            border.drawImage(strip, 0, 0, null);
            border.setColor(new Color(0, 0, 0, 65));
            border.fillRect(0, thickness, ts, Math.max(1, ts / 48));
            border.dispose();
        }
    }

    private static boolean wall(char tile) { return tile == 'o' || tile == 'e'; }

    /** Visual thickness shared by straight runs, junction posts and thresholds. */
    public static int sideWidth(int tileSize) { return Math.max(3, Math.round(tileSize / 6f)); }

    private static void spine(Graphics2D g, AssetStore assets, String material,
                              int wy, int px, int py, int ts, int top, int bottom) {
        int width = sideWidth(ts), left = px + (ts - width) / 2;
        Shape clip = g.getClip();
        g.clipRect(left, top, width, bottom - top);
        BufferedImage image = assets.image(asset(material, "post"), width, ts);
        // Repeat the shaft, excluding the source panel's header and dado bands.
        int shaftTop = Math.max(1, ts / 8), shaftBottom = Math.max(shaftTop + 1, ts * 5 / 8);
        for (int row = Math.floorDiv(top - py, ts); py + row * ts < bottom; row++) {
            int y = py + row * ts;
            boolean flip = Math.floorMod(wy + row, 2) == 1;
            g.drawImage(image, left, y, left + width, y + ts,
                    0, flip ? shaftBottom : shaftTop, width, flip ? shaftTop : shaftBottom, null);
        }
        g.setClip(clip);
    }

    /** The same silhouette drives terrain, foreground occlusion and wall mounting. */
    public static Rectangle bounds(WorldMap world, String map, int x, int y, int px, int py, int ts) {
        boolean n = wall(world.tileAt(map, x, y - 1)), s = wall(world.tileAt(map, x, y + 1));
        boolean e = wall(world.tileAt(map, x + 1, y)), w = wall(world.tileAt(map, x - 1, y));
        int rail = sideWidth(ts), margin = (ts - rail) / 2;
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

    public static boolean touchesSideEdge(WorldMap world, String map, int x, int y, boolean east) {
        if (world.tileAt(map, x, y) != 'o') return false;
        Rectangle shape = bounds(world, map, x, y, 0, 0, 48);
        return east ? shape.x + shape.width == 48 : shape.x == 0;
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
        int post = sideWidth(ts), rail = sideWidth(ts);
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
        String shell = material(map);
        String material = wallMaterial(world, map, wx, wy);
        int rail = sideWidth(ts), margin = (ts - rail) / 2;
        int trim = Math.max(2, ts / 8);
        boolean vertical = (n || s) && !e && !w;
        if (vertical) {
            g.setColor(new Color(12, 13, 18));
            g.fillRect(px, py, ts, ts);
            // The visual cutaway is thinner than the collision cell. Continue the
            // neighboring floor up to its face instead of leaving a black gutter.
            Shape clip = g.getClip();
            if (fw) {
                g.clipRect(px, py, margin, ts);
                floor(g, assets, InteriorFlooring.material(world, map, wx - 1, wy, shell), wx, wy, px, py, ts);
                g.setColor(InteriorStyle.forMap(map).materialTint);
                g.fillRect(px, py, ts, ts);
                g.setClip(clip);
            }
            if (fe) {
                g.clipRect(px + margin + rail, py, ts - margin - rail, ts);
                floor(g, assets, InteriorFlooring.material(world, map, wx + 1, wy, shell), wx, wy, px, py, ts);
                g.setColor(InteriorStyle.forMap(map).materialTint);
                g.fillRect(px, py, ts, ts);
                g.setClip(clip);
            }
            spine(g, assets, material, wy, px, py, ts, py, py + ts);
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
            floor(g, assets, InteriorFlooring.material(world, map, wx, wy + (fs ? 1 : -1), shell), wx, wy, px, py, ts);
            g.setColor(InteriorStyle.forMap(map).materialTint);
            g.fillRect(px, py, ts, ts);
            g.setClip(originalClip);
        }
        g.drawImage(assets.image(asset(material, "face"), face.width, height), face.x, top, null);
        g.drawImage(assets.image(asset(material, "cap"), face.width, trim), face.x, top, null);
        g.drawImage(assets.image(asset(material, "base"), face.width, trim), face.x, py + ts - trim, null);
        // Posts are structural: at ends and junctions, never on every repeat.
        int post = rail;
        // A turn owns one continuous post. Stacking an end post beside the
        // junction post makes corners twice as thick and introduces a step.
        if (!w && !n && !s) g.drawImage(assets.image(asset(material, "post"), post, height), face.x, top, null);
        if (!e && !n && !s) g.drawImage(assets.image(asset(material, "post"), post, height), face.x + face.width - post, top, null);
        if (n || s) {
            // Caps cross a terminating post; through-posts carry on into the
            // adjacent rail with the exact same width, material and grain phase.
            spine(g, assets, shell, wy, px, py, ts, n ? top : top + trim,
                    s ? py + ts : py + ts - trim);
        }
        // The masonry owns a narrow dressed edge where it meets timber. Keep
        // it inside the stone face so neighboring wall decor and joins stay put.
        if (material.equals("stone")) {
            for (int dx : new int[]{-1, 1}) {
                if (world.tileAt(map, wx + dx, wy) != 'o'
                        || wallMaterial(world, map, wx + dx, wy).equals(material)) continue;
                int edge = dx < 0 ? face.x : face.x + face.width - rail;
                g.drawImage(assets.image(asset(material, "post"), rail, height), edge, top, null);
                g.setColor(new Color(0, 0, 0, 55));
                g.fillRect(dx < 0 ? edge : edge + rail - Math.max(1, ts / 32), top,
                        Math.max(1, ts / 32), height);
            }
        }
        g.setColor(InteriorStyle.forMap(map).materialTint);
        g.fillRect(face.x, top, face.width, height);
    }
}
