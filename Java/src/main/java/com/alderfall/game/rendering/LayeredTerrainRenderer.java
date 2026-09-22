package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** World-space material coverage, sampled independently of tile/chunk drawing order. */
final class LayeredTerrainRenderer {
    private static final int TEXTURE_SIZE = 48;
    private final AssetStore assets;
    private final Map<String, int[]> textures = new HashMap<>();
    private final Map<Key, Surface> surfaces = new LinkedHashMap<>(128, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, Surface> entry) {
            // Keep enough small tiles for zoomed-out water views without growing pixel memory unbounded.
            return size() > Math.max(256, Math.min(4096, 8_388_608 / (tileSize * tileSize)));
        }
    };
    private WorldMap world;
    private int tileSize;
    private List<WorldMap.GroundRegion> regions = List.of();

    LayeredTerrainRenderer(AssetStore assets) {
        this.assets = assets;
    }

    Surface tile(GameState state, WorldRenderer.TerrainPainter painter, int x, int y, int size) {
        if (world != state.world || tileSize != size) {
            world = state.world;
            tileSize = size;
            regions = world.groundRegions();
            surfaces.clear();
        }
        long fingerprint = 1469598103934665603L;
        // Includes the neighborhood used to infer road/settlement underlays.
        for (int oy = -6; oy <= 6; oy++) {
            for (int ox = -6; ox <= 6; ox++) {
                fingerprint = (fingerprint ^ world.tileAt(state.currentMapId, x + ox, y + oy)) * 1099511628211L;
            }
        }
        Key key = new Key(state.currentMapId, x, y, size, fingerprint);
        Surface cached = surfaces.get(key);
        if (cached != null) return cached;
        Surface result = compose(state, painter, x, y, size);
        surfaces.put(key, result);
        return result;
    }

    private Surface compose(GameState state, WorldRenderer.TerrainPainter painter, int tx, int ty, int size) {
        char[][] materials = new char[3][3];
        Map<Character, int[]> materialTextures = new HashMap<>();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int wx = tx + x - 1, wy = ty + y - 1;
                char raw = world.tileAt(state.currentMapId, wx, wy);
                char material = painter.visibleTerrainTile(raw, wx, wy);
                if (material == 'd') material = 'g';
                materials[y][x] = material;
                materialTextures.computeIfAbsent(material, c -> texture(Terrain.assetName(c), false));
            }
        }
        List<WorldMap.GroundRegion> nearby = regions.stream().filter(r ->
                Math.abs(tx + 0.5 - r.x() - 0.5) < r.radiusX() + 2
                        && Math.abs(ty + 0.5 - r.y() - 0.5) < r.radiusY() + 2).toList();
        if (materialTextures.size() == 1 && nearby.isEmpty()) {
            char material = materials[1][1];
            int[] sample = materialTextures.get(material);
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) {
                int v = (int) ((y + 0.5) * TEXTURE_SIZE / size);
                for (int x = 0; x < size; x++) {
                    int u = (int) ((x + 0.5) * TEXTURE_SIZE / size);
                    pixels[y * size + x] = sample[v * TEXTURE_SIZE + u];
                }
            }
            image.setRGB(0, 0, size, size, pixels, 0, size);
            return new Surface(image, new Rectangle2D.Float(0, 0, size, size), water(material));
        }
        int[] soil = texture("location_graveyard_dirt", true);
        int[] gravel = texture("location_dungeon_approach_path", true);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        int[] pixels = new int[size * size];
        boolean[] wet = new boolean[pixels.length];
        char[] types = new char[4];
        double[] weights = new double[4];
        int[][] samples = new int[4][];
        for (int py = 0; py < size; py++) {
            double y = ty + (py + 0.5) / size;
            for (int px = 0; px < size; px++) {
                double x = tx + (px + 0.5) / size;
                // The same coordinate always produces the same edge, including across chunks.
                double warpX = (noise(x / 1.8, y / 1.8, 71) - 0.5) * 0.38;
                double warpY = (noise(x / 1.8, y / 1.8, 137) - 0.5) * 0.38;
                double sx = x - 0.5 + warpX, sy = y - 0.5 + warpY;
                int ix = (int) Math.floor(sx), iy = (int) Math.floor(sy);
                double fx = smooth(sx - ix), fy = smooth(sy - iy);
                int count = 0;
                double waterCoverage = 0;
                for (int corner = 0; corner < 4; corner++) {
                    int ox = corner & 1, oy = corner >> 1;
                    char type = materials[iy - ty + 1 + oy][ix - tx + 1 + ox];
                    double w = (ox == 0 ? 1 - fx : fx) * (oy == 0 ? 1 - fy : fy);
                    if (water(type)) waterCoverage += w;
                    int slot = 0;
                    while (slot < count && types[slot] != type) slot++;
                    if (slot == count) {
                        types[count] = type;
                        weights[count] = 0;
                        samples[count] = materialTextures.get(type);
                        count++;
                    }
                    weights[slot] += w;
                }
                double max = 0;
                char dominant = types[0];
                for (int i = 0; i < count; i++) {
                    if (weights[i] > max) { max = weights[i]; dominant = types[i]; }
                }
                int u = Math.floorMod((int) Math.floor(x * TEXTURE_SIZE), TEXTURE_SIZE);
                int v = Math.floorMod((int) Math.floor(y * TEXTURE_SIZE), TEXTURE_SIZE);
                int uv = v * TEXTURE_SIZE + u;
                double red = 0, green = 0, blue = 0, sum = 0, waterAlpha = 0;
                double grain = (noise(x * 11, y * 11, 211) - 0.5) * 0.10;
                boolean shore = waterCoverage > 0.001 && waterCoverage < 0.999;
                for (int i = 0; i < count; i++) {
                    double feather = shore ? 0.11 : types[i] == 'n' ? 0.23 : 0.38;
                    double coverage = Math.max(0, weights[i] - max + feather + grain * ((types[i] & 1) == 0 ? 1 : -1));
                    double w = coverage * coverage;
                    int rgb = samples[i][uv];
                    red += ((rgb >> 16) & 255) * w;
                    green += ((rgb >> 8) & 255) * w;
                    blue += (rgb & 255) * w;
                    sum += w;
                    if (water(types[i])) waterAlpha += w;
                }
                waterAlpha /= sum;
                int rgb = color(red / sum, green / sum, blue / sum);
                // A narrow wet bank and shallow shelf follow the coverage boundary.
                if (shore) {
                    double bank = smoothRange(0.06, 0.32, waterCoverage) * (1 - waterAlpha);
                    rgb = mix(rgb, soil[uv], bank * 0.62);
                    double shallow = (1 - smoothRange(0.50, 0.94, waterCoverage)) * waterAlpha;
                    rgb = mix(rgb, 0x598b87, shallow * 0.25);
                }
                if (!water(dominant) && dominant != 'm' && dominant != 'q' && waterAlpha < 0.01) {
                    for (WorldMap.GroundRegion region : nearby) {
                        double dx = x - region.x() - 0.5;
                        double clearing = region.coverage(x, y);
                        // One connected dirt footprint, with a worn route through its center.
                        double track = 1 - smoothRange(0.40, 1.1, Math.abs(dx + Math.sin(y * 0.45) * 0.30));
                        double wear = clearing * (0.76 + noise(x / 0.8, y / 0.8, 347) * 0.20);
                        rgb = mix(rgb, soil[uv], wear);
                        rgb = mix(rgb, gravel[uv], clearing * track * 0.70);
                    }
                }
                pixels[py * size + px] = 0xff000000 | rgb;
                wet[py * size + px] = waterAlpha > 0.55;
            }
        }
        image.setRGB(0, 0, size, size, pixels, 0, size);
        Path2D.Float waterClip = new Path2D.Float();
        boolean hasWater = false;
        boolean fullWater = true;
        for (boolean pixel : wet) fullWater &= pixel;
        if (fullWater) return new Surface(image, new Rectangle2D.Float(0, 0, size, size), true);
        for (int y = 0; y < size; y++) {
            int x = 0;
            while (x < size) {
                if (!wet[y * size + x]) { x++; continue; }
                int start = x++;
                while (x < size && wet[y * size + x]) x++;
                waterClip.append(new Rectangle2D.Float(start, y, x - start, 1), false);
                hasWater = true;
            }
        }
        return new Surface(image, waterClip, hasWater);
    }

    private int[] texture(String name, boolean interior) {
        String key = name + (interior ? ":interior" : "");
        return textures.computeIfAbsent(key, ignored -> {
            // Sample only the interior of old ground stamps: their painted square border is not terrain.
            BufferedImage image = assets.image(name, interior ? 96 : TEXTURE_SIZE, interior ? 96 : TEXTURE_SIZE);
            return image.getRGB(interior ? 24 : 0, interior ? 24 : 0, TEXTURE_SIZE, TEXTURE_SIZE, null, 0, TEXTURE_SIZE);
        });
    }

    private static boolean water(char tile) { return tile == 'w' || tile == '~'; }
    private static double smooth(double t) { return t * t * (3 - 2 * t); }
    private static double smoothRange(double low, double high, double value) {
        return smooth(Math.max(0, Math.min(1, (value - low) / (high - low))));
    }
    private static int color(double r, double g, double b) {
        return ((int) r << 16) | ((int) g << 8) | (int) b;
    }
    private static int mix(int a, int b, double alpha) {
        return color(((a >> 16) & 255) * (1 - alpha) + ((b >> 16) & 255) * alpha,
                ((a >> 8) & 255) * (1 - alpha) + ((b >> 8) & 255) * alpha,
                (a & 255) * (1 - alpha) + (b & 255) * alpha);
    }
    private static double noise(double x, double y, int seed) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        double fx = smooth(x - ix), fy = smooth(y - iy);
        double top = random(ix, iy, seed) * (1 - fx) + random(ix + 1, iy, seed) * fx;
        double bottom = random(ix, iy + 1, seed) * (1 - fx) + random(ix + 1, iy + 1, seed) * fx;
        return top * (1 - fy) + bottom * fy;
    }
    private static double random(int x, int y, int seed) {
        int value = x * 374761393 + y * 668265263 + seed * 1274126177;
        value = (value ^ (value >>> 13)) * 1274126177;
        return ((value ^ (value >>> 16)) & 65535) / 65535.0;
    }

    record Surface(BufferedImage image, Shape waterClip, boolean hasWater) { }
    private record Key(String mapId, int x, int y, int size, long fingerprint) { }
}
