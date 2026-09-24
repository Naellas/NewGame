package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.*;

/** Cardinal garden connections: north=1, south=2, west=4, east=8. */
public final class ConnectedBoundary {
    private ConnectedBoundary() { }
    public static boolean supports(String asset) {
        return !TownGardenArt.gate(asset) && (asset.startsWith("town_hedge_") || asset.startsWith("town_fence_"));
    }
    public static String family(String asset) { return asset.startsWith("town_hedge_") ? "town_hedge" : "town_fence"; }
    public static int connections(WorldMap world, String map, WorldProp prop) {
        if (!supports(prop.asset())) return 0;
        int mask = 0;
        for (int[] d : new int[][]{{0,-1,1}, {0,1,2}, {-1,0,4}, {1,0,8}}) {
            for (WorldProp n : world.propsAt(map, prop.x() + d[0], prop.y() + d[1]))
                if (supports(n.asset()) && family(n.asset()).equals(family(prop.asset()))) mask |= d[2];
            // Gates leave two shoulder cells clear; their outside posts still join the boundary.
            if (d[0] != 0) for (WorldProp n : world.propsAt(map, prop.x() + d[0] * 2, prop.y()))
                if (TownGardenArt.gate(n.asset())) mask |= d[2];
        }
        return mask;
    }

    public static final class Sprites {
        private record Key(String family, int tile, int mask) { }
        private final Map<Key, BufferedImage> cache = new LinkedHashMap<>(64, .75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Key, BufferedImage> e) { return size() > 192; }
        };
        /** A tile-wide, two-tile-high canvas with its shared ground node at (0.5, 1.5). */
        public BufferedImage image(AssetStore assets, String asset, int tile, int mask) {
            String family = family(asset);
            return cache.computeIfAbsent(new Key(family, tile, mask), key -> compose(assets, family, tile, mask));
        }
        private BufferedImage compose(AssetStore assets, String family, int t, int mask) {
            BufferedImage result = new BufferedImage(t, t * 2, BufferedImage.TYPE_INT_ARGB);
            BufferedImage h = ink(assets.spriteFit(family + "_h", 256, 160));
            BufferedImage v = ink(assets.spriteFit(family + "_v", 128, 256));
            Graphics2D g = result.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            boolean hedge = family.equals("town_hedge");
            int center = t / 2, base = t + t / 2;
            int rise = Math.max(2, Math.round(t * (hedge ? .52f : .62f)));
            int thickness = Math.max(3, Math.round(t * (hedge ? .42f : .20f)));
            if ((mask & 1) != 0) g.drawImage(v, center - thickness / 2, t - rise, thickness, base - t + rise, null);
            if ((mask & 12) != 0) {
                int left = (mask & 4) != 0 ? 0 : center - thickness / 2;
                int right = (mask & 8) != 0 ? t : center + (thickness + 1) / 2;
                int trim = Math.max(1, h.getWidth() / 9);
                g.drawImage(h, left, base - rise, right, base, trim, 0, h.getWidth() - trim, h.getHeight(), null);
            }
            if ((mask & 2) != 0) g.drawImage(v, center - thickness / 2, base - rise, thickness, t * 2 - base + rise, null);
            if (hedge) {
                if (mask == 0 || (mask & 3) != 0 && (mask & 12) != 0) {
                    BufferedImage crown = ink(assets.spriteFit("town_hedge_end", 128, 160));
                    g.drawImage(crown, center - thickness / 2, base - rise, thickness, rise, null);
                }
            } else {
                int post = Math.max(2, t / 6);
                g.drawImage(h, center - post / 2, base - rise, center + (post + 1) / 2, base,
                        0, 0, Math.max(1, h.getWidth() / 9), h.getHeight(), null);
            }
            g.dispose();
            return result;
        }
        private static BufferedImage ink(BufferedImage source) {
            int left = source.getWidth(), top = source.getHeight(), right = -1, bottom = -1;
            for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
                if ((source.getRGB(x, y) >>> 24) < 16) continue;
                left = Math.min(left, x); top = Math.min(top, y); right = Math.max(right, x); bottom = Math.max(bottom, y);
            }
            return right < left ? source : source.getSubimage(left, top, right - left + 1, bottom - top + 1);
        }
    }
}
