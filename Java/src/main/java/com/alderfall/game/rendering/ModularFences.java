package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import com.alderfall.game.map.WorldMap;

/** N/S/W/E bits agree with the existing village fence atlas. Families never cross-connect. */
public final class ModularFences {
    private ModularFences() { }
    public static String family(String asset) {
        if (asset.startsWith("village_fence_") || asset.equals("village_prop_fence_segment")) return "village";
        if (asset.equals("location_farmland_fence") || asset.equals("location_farmland_fence_side")) return "farm";
        if (asset.equals("location_graveyard_iron_fence") || asset.equals("location_graveyard_iron_fence_side")) return "iron";
        return "";
    }
    public static int connections(WorldMap world, String map, WorldProp prop) {
        String family = family(prop.asset());
        if (family.isEmpty()) return 0;
        int bits = 0;
        int[][] steps = {{0,-1},{0,1},{-1,0},{1,0}};
        for (int i = 0; i < steps.length; i++) for (WorldProp neighbor : world.propsAt(map, prop.x()+steps[i][0], prop.y()+steps[i][1]))
            if (family.equals(family(neighbor.asset()))) { bits |= 1 << i; break; }
        return bits;
    }
    public static final class Sprites {
        private final java.util.Map<String, BufferedImage> cache = new java.util.LinkedHashMap<>(64, .75f, true) {
            @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, BufferedImage> e) { return size() > 256; }
        };
        public BufferedImage image(AssetStore assets, String asset, int size, int mask) {
            String family = family(asset);
            if (mask == 0) return assets.spriteFit(asset, size, size);
            return cache.computeIfAbsent(family + ":" + size + ":" + mask, key -> {
                String base = family.equals("farm") ? "location_farmland_fence" : "location_graveyard_iron_fence";
                BufferedImage horizontal = normalize(assets.spriteFit(family.equals("village") ? "village_fence_12" : base, size, size), size, false);
                BufferedImage vertical = normalize(assets.spriteFit(family.equals("village") ? "village_fence_03" : base + "_side", size, size), size, true);
                BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = result.createGraphics();
                if ((mask & 1) != 0) { g.setClip(0,0,size,size/2+1); g.drawImage(vertical,0,0,null); }
                if ((mask & 4) != 0) { g.setClip(0,0,size/2+1,size); g.drawImage(horizontal,0,0,null); }
                if ((mask & 8) != 0) { g.setClip(size/2,0,size-size/2,size); g.drawImage(horizontal,0,0,null); }
                if ((mask & 2) != 0) { g.setClip(0,size/2,size,size-size/2); g.drawImage(vertical,0,0,null); }
                g.dispose(); return result;
            });
        }
        private static BufferedImage normalize(BufferedImage source, int size, boolean vertical) {
            int left = size, top = size, right = -1, bottom = -1;
            for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                if ((source.getRGB(x, y) >>> 24) < 48) continue;
                left = Math.min(left, x); right = Math.max(right, x);
                top = Math.min(top, y); bottom = Math.max(bottom, y);
            }
            if (right < left) return source;
            BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = result.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            // Connection tips reach cell boundaries; transparent source margins must not create gaps.
            int inset = vertical ? Math.round(size * .38f) : 0;
            if (vertical) {
                // Side artwork has perspective drift. Align its row centers to the north/south axis.
                for (int y = 0; y < size; y++) {
                    int sy = top + y * (bottom - top + 1) / size;
                    int rowLeft = size, rowRight = -1;
                    for (int x = left; x <= right; x++) if ((source.getRGB(x, sy) >>> 24) >= 48) {
                        rowLeft = Math.min(rowLeft, x); rowRight = Math.max(rowRight, x);
                    }
                    if (rowRight < rowLeft) continue;
                    int width = Math.max(2, (rowRight - rowLeft + 1) * (size - inset * 2) / (right - left + 1));
                    g.drawImage(source, (size-width)/2, y, (size-width)/2+width, y+1,
                            rowLeft, sy, rowRight+1, sy+1, null);
                }
            } else g.drawImage(source, 0, size / 6, size, size * 5 / 6,
                    left, top, right + 1, bottom + 1, null);
            g.dispose();
            return result;
        }
    }
}
