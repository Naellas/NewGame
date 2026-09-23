package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** Continuous dungeon masses with exposed rock or masonry faces; shared with the cavern pass. */
public final class CavernTerrainRenderer {
    private final AssetStore assets;
    private final Map<String, BufferedImage> textures = new HashMap<>();

    public CavernTerrainRenderer(AssetStore assets) { this.assets = assets; }

    public static boolean supports(WorldMap.DungeonContext context) {
        return context != null && (CavernStyle.natural(context.theme()) || BuiltDungeonStyle.supports(context.theme()));
    }

    public static String biome(WorldMap.DungeonContext context) {
        if (context == null) return "stone";
        if (BuiltDungeonStyle.supports(context.theme())) return BuiltDungeonStyle.material(context.theme(),context.floor());
        // Physical conditions take priority when a site's political region crosses a biome.
        return CavernStyle.biome(context.region(), context.exterior());
    }

    private static boolean solid(char tile) { return "OxZoX".indexOf(tile) >= 0; }

    public void draw(Graphics2D graphics, GameState state, WorldMap.DungeonContext context,
                     int wx, int wy, int px, int py, int size) {
        String map = state.currentMapId;
        char tile = state.world.tileAt(map, wx, wy);
        String biome = biome(context);
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(px, py);
        g.clipRect(0, 0, size, size);
        boolean north = solid(state.world.tileAt(map, wx, wy - 1));
        boolean east = solid(state.world.tileAt(map, wx + 1, wy));
        boolean south = solid(state.world.tileAt(map, wx, wy + 1));
        boolean west = solid(state.world.tileAt(map, wx - 1, wy));
        paint(g, biome, "floor", wx, wy, size);
        if (solid(tile)) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape outline = BuiltDungeonStyle.masonry(biome) ? new Rectangle(0,0,size,size)
                    : outline(size, !north && !west, !north && !east, !south && !east, !south && !west);
            g.clip(outline);
            paint(g, biome, "roof", wx, wy, size);
            g.setColor(new Color(3, 5, 8, 175));
            g.fillRect(0, 0, size, size);
            int rim = Math.max(3, size / 9);
            if (!south) {
                int height = Math.max(12, size * (BuiltDungeonStyle.masonry(biome) ? 3 : 2) / 5);
                BufferedImage face = assets.image(asset(biome,"face"), size * 2, height);
                int offset = Math.floorMod(wx, 2) * size;
                g.drawImage(face, 0, size-height, size, size, offset, 0, offset+size, height, null);
            }
            // Side and rear lips remain shallow, leaving a legible floor silhouette.
            BufferedImage lip = assets.image(asset(biome,"face"), size, size);
            if (!north) g.drawImage(lip, 0, 0, size, rim, 0, 0, size, size/5, null);
            if (!west) g.drawImage(lip, 0, 0, rim, size, 0, 0, size/6, size, null);
            if (!east) g.drawImage(lip, size-rim, 0, size, size, size*5/6, 0, size, size, null);
        } else {
            // Gravel gathers at walls, with quiet open ground through the center of rooms.
            if (north || east || south || west || tile == 'E' || tile == 'R' || tile == 'M') {
                g.setComposite(AlphaComposite.SrcOver.derive((north || east || south || west) ? .68f : .24f));
                paint(g, biome, "gravel", wx, wy, size);
                g.setComposite(AlphaComposite.SrcOver);
            }
            int shadow = Math.max(3, size / 8);
            if (context.theme().equals("sewer")) {
                g.setColor(new Color(34,65,43,55));
                g.fillRect(0,0,size,size);
            }
            if (north) shade(g, 0, 0, size, shadow, true);
            if (west) shade(g, 0, 0, shadow, size, false);
            if (east) shade(g, size, 0, -shadow, size, false);
            if (south) shade(g, 0, size, size, -shadow, true);
            if (tile == 'Y' || tile == 'W') {
                g.setColor(biome.equals("ice") ? new Color(115, 186, 221, 130)
                        : biome.equals("moss") ? new Color(35, 66, 49, 155) : new Color(28, 61, 74, 150));
                g.fillRect(0, 0, size, size);
            }
            if (tile == 'S' || tile == 'L' || tile == '3' || tile == '4') {
                // Preserve the sparse gameplay landmarks without importing mismatched floor borders.
                String asset = tile == 'S' || tile == '4' ? "dungeon_boss_sigil_floor" : "dungeon_torch_floor";
                g.setComposite(AlphaComposite.SrcOver.derive(.55f));
                g.drawImage(assets.imageWithoutBorder(asset, size, size), 0, 0, null);
            }
        }
        g.dispose();
    }

    private void paint(Graphics2D g, String biome, String role, int x, int y, int size) {
        int period = size * 2;
        String key = biome + role + size;
        BufferedImage texture = textures.computeIfAbsent(key, ignored -> {
            BufferedImage source = assets.image(asset(biome,role), period, period);
            BufferedImage result = new BufferedImage(period*2, period*2, BufferedImage.TYPE_INT_RGB);
            Graphics2D brush = result.createGraphics();
            // Mirrored repeat joins identical edge pixels and avoids rectangular texture seams.
            for (int row = 0; row < 2; row++) for (int col = 0; col < 2; col++) {
                brush.drawImage(source, col*period, row*period, (col+1)*period, (row+1)*period,
                        col == 0 ? 0 : period, row == 0 ? 0 : period,
                        col == 0 ? period : 0, row == 0 ? period : 0, null);
            }
            brush.dispose();
            return result;
        });
        g.setPaint(new TexturePaint(texture, new Rectangle(-Math.floorMod(x,4)*size,
                -Math.floorMod(y,4)*size, period*2, period*2)));
        g.fillRect(0, 0, size, size);
    }

    private static void shade(Graphics2D g, int x, int y, int width, int height, boolean vertical) {
        g.setPaint(new GradientPaint(x, y, new Color(0, 0, 0, 100),
                vertical ? x : x+width, vertical ? y+height : y, new Color(0, 0, 0, 0)));
        g.fillRect(Math.min(x, x+width), Math.min(y, y+height), Math.abs(width), Math.abs(height));
    }

    private static String asset(String material, String role) {
        return (BuiltDungeonStyle.masonry(material) ? "masonry_" : "cavern_") + material + "_" + role;
    }

    private static Shape outline(int size, boolean nw, boolean ne, boolean se, boolean sw) {
        double r = size * .18;
        Path2D p = new Path2D.Double();
        p.moveTo(nw ? r : 0, 0);
        p.lineTo(ne ? size-r : size, 0);
        if (ne) p.quadTo(size, 0, size, r);
        p.lineTo(size, se ? size-r : size);
        if (se) p.quadTo(size, size, size-r, size);
        p.lineTo(sw ? r : 0, size);
        if (sw) p.quadTo(0, size, 0, size-r);
        p.lineTo(0, nw ? r : 0);
        if (nw) p.quadTo(0, 0, r, 0);
        p.closePath();
        return p;
    }
}
