package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Horizontal furniture runs. Bit 0 joins the left edge; bit 1 joins the right. */
public final class ConnectedFurniture {
    private ConnectedFurniture() {}
    private static final Set<String> ASSETS = Set.of("interior_wall_window_oak_segment", "interior_storage_counter", "interior_shop_counter",
            "interior_bakery_counter", "interior_tavern_counter", "interior_tavern_bar",
            "interior_bookshelf", "interior_pantry_shelf", "interior_tools_shelf", "interior_supplies_shelf",
            "interior_joinery_storage", "interior_bakehouse_storage", "interior_apothecary_storage", "interior_archive_storage",
            "interior_joinery_worktop", "interior_bakehouse_worktop", "interior_apothecary_worktop", "interior_archive_worktop",
            "interior_smith_storage", "interior_smith_worktop",
            "interior_carpenter_workbench", "interior_carpenter_table", "interior_alchemy_station",
            "interior_cooking_station", "interior_study_desk_h", "interior_low_cupboard", "interior_crockery_cupboard");
    private static final Set<String> OUTDOOR_RUNS = Set.of("city_prop_refresh_bench",
            "city_prop_refresh_planter_herbs", "city_prop_refresh_stall_canvas", "town_hedge_h", "town_fence_h");
    public static boolean boundary(String asset) {
        return asset.startsWith("town_hedge_") || asset.startsWith("town_fence_") || TownGardenArt.gate(asset);
    }
    public static boolean outdoor(String asset) { return OUTDOOR_RUNS.contains(asset) || boundary(asset); }
    public static boolean supports(String asset) { return ASSETS.contains(asset) || OUTDOOR_RUNS.contains(asset) || ModularMarket.supports(asset); }
    public static int connectionWidth(String asset) {
        return ModularMarket.supports(asset) ? 2 : OUTDOOR_RUNS.contains(asset) ? 1 : WorldMap.interiorVisualFootprint(asset)[0];
    }

    public static int connections(WorldProp prop, List<WorldProp> props) {
        if (!supports(prop.asset())) return 0;
        int width = connectionWidth(prop.asset());
        int mask = 0;
        for (WorldProp neighbor : props) {
            if (window(prop.asset()) && (neighbor.offsetX() != prop.offsetX() || neighbor.offsetY() != prop.offsetY())) continue;
            if (neighbor.y() != prop.y() || !(neighbor.asset().equals(prop.asset()) || ModularMarket.supports(prop.asset()) && ModularMarket.supports(neighbor.asset()))) continue;
            if (neighbor.x() + width == prop.x()) mask |= 1;
            if (prop.x() + width == neighbor.x()) mask |= 2;
        }
        return mask;
    }

    public static boolean window(String asset) { return "interior_wall_window_oak_segment".equals(asset); }

    /** Cache belongs to the renderer so artwork cannot leak between asset stores. */
    public static final class Sprites {
        private record Key(String asset, int width, int height, int mask) {}
        private final Map<Key, BufferedImage> cache = new java.util.LinkedHashMap<>(64, .75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Key, BufferedImage> entry) {
                return size() > 256;
            }
        };
        public BufferedImage image(AssetStore assets, String asset, int width, int height, int mask) {
            if (mask == 0) return assets.spriteFit(asset, width, height);
            return cache.computeIfAbsent(new Key(asset, width, height, mask), key -> {
                BufferedImage source = assets.spriteFit(asset, width, height);
                int left = width, right = -1;
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                    if ((source.getRGB(x, y) >>> 24) < 16) continue;
                    left = Math.min(left, x); right = Math.max(right, x);
                }
                if (right < left) return source;
                // Keep the fitted artwork at its original scale. Only the connection bands
                // repeat; stretching the cropped body would widen drawers, tools and books.
                int post = Math.max(1, (right - left + 1) / 10);
                int from = left + ((mask & 1) != 0 ? post : 0);
                int to = right + 1 - ((mask & 2) != 0 ? post : 0);
                BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = result.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int bodyLeft = (mask & 1) != 0 ? from : 0;
                int bodyRight = (mask & 2) != 0 ? to : width;
                g.drawImage(source, bodyLeft, 0, bodyRight, height,
                        bodyLeft, 0, bodyRight, height, null);
                // Mirror a full-width edge patch at 1:1 scale. Thin repeated strips
                // turn hinges into ribs; single-column extrusion creates smeared posts.
                int band = Math.max(1, to - from);
                if ((mask & 1) != 0) {
                    for (int end = from; end > 0; end -= band) {
                        int count = Math.min(band, end);
                        g.drawImage(source, end - count, 0, end, height,
                                from + count, 0, from, height, null);
                    }
                }
                if ((mask & 2) != 0) {
                    for (int start = to; start < width; start += band) {
                        int count = Math.min(band, width - start);
                        g.drawImage(source, start, 0, start + count, height,
                                to, 0, to - count, height, null);
                    }
                }
                g.dispose();
                return result;
            });
        }
    }
}
