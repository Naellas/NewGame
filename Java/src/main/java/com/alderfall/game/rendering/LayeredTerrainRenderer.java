package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** World-space material coverage for the overworld and outdoor settlement maps. */
final class LayeredTerrainRenderer {
    private static final int TEXTURE_SIZE = 48;
    private static final int LARGE_TEXTURE_SIZE = TEXTURE_SIZE * 8;
    private final AssetStore assets;
    private final Map<String, int[]> textures = new HashMap<>();
    private final Map<Character, int[][]> naturalTerrainVariants = new HashMap<>();
    private final Map<FamilyKey, Map<Integer, Variant>> surfaceVariants = new HashMap<>();
    private final Map<Key, Surface> surfaces = new LinkedHashMap<>(128, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, Surface> entry) {
            // Keep enough small tiles for zoomed-out water views without growing pixel memory unbounded.
            boolean remove = size() > Math.max(256, Math.min(4096, 8_388_608 / (tileSize * tileSize)));
            if (remove) removeVariant(entry.getKey());
            return remove;
        }
    };
    private WorldMap world;
    private int tileSize;
    private List<WorldMap.GroundRegion> regions = List.of();

    LayeredTerrainRenderer(AssetStore assets) {
        this.assets = assets;
    }

    public static boolean supports(String kind) {
        return "overworld".equals(kind) || "village".equals(kind) || "city".equals(kind);
    }

    Surface tile(GameState state, WorldRenderer.TerrainPainter painter, int x, int y, int size) {
        if (world != state.world) {
            world = state.world;
            regions = world.groundRegions();
            surfaces.clear();
            surfaceVariants.clear();
        }
        tileSize = size;
        long revision = world.visualRevision(state.currentMapId);
        Key key = new Key(state.currentMapId, x, y, size, revision);
        Surface cached = surfaces.get(key);
        if (cached != null) return cached;
        FamilyKey family = new FamilyKey(state.currentMapId, x, y, revision);
        Variant source = closestVariant(family, size);
        if (source != null) {
            Surface scaled = scaleSurface(source.surface(), size);
            cacheSurface(key, family, scaled, true);
            return scaled;
        }
        Surface result = compose(state, painter, x, y, size);
        cacheSurface(key, family, result, false);
        return result;
    }

    private Variant closestVariant(FamilyKey family, int size) {
        Map<Integer, Variant> variants = surfaceVariants.get(family);
        if (variants == null || variants.isEmpty()) return null;
        Variant closest = null;
        int closestDistance = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Variant> entry : variants.entrySet()) {
            if (entry.getValue().derived()) continue;
            int distance = Math.abs(entry.getKey() - size);
            if (distance < closestDistance) {
                closest = entry.getValue();
                closestDistance = distance;
            }
        }
        if (closest != null) return closest;
        for (Map.Entry<Integer, Variant> entry : variants.entrySet()) {
            int distance = Math.abs(entry.getKey() - size);
            if (distance < closestDistance) {
                closest = entry.getValue();
                closestDistance = distance;
            }
        }
        return closest;
    }

    private Surface scaleSurface(Surface source, int size) {
        int sourceSize = source.image().getWidth();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        graphics.drawImage(source.image(), 0, 0, size, size, null);
        graphics.dispose();
        double scale = size / (double) sourceSize;
        Shape waterClip = AffineTransform.getScaleInstance(scale, scale).createTransformedShape(source.waterClip());
        return new Surface(image, waterClip, source.hasWater());
    }

    private void cacheSurface(Key key, FamilyKey family, Surface surface, boolean derived) {
        surfaces.put(key, surface);
        surfaceVariants.computeIfAbsent(family, ignored -> new HashMap<>())
                .put(key.size(), new Variant(surface, derived));
    }

    private void removeVariant(Key key) {
        FamilyKey family = new FamilyKey(key.mapId(), key.x(), key.y(), key.visualRevision());
        Map<Integer, Variant> variants = surfaceVariants.get(family);
        if (variants == null) return;
        variants.remove(key.size());
        if (variants.isEmpty()) surfaceVariants.remove(family);
    }

    private Surface compose(GameState state, WorldRenderer.TerrainPainter painter, int tx, int ty, int size) {
        boolean village = "village".equals(world.kind(state.currentMapId));
        boolean city = "city".equals(world.kind(state.currentMapId));
        boolean settlement = village || city;
        char settlementGround = villageGround(state.currentMapId);
        char[][] materials = new char[3][3];
        char[][] roadMaterials = new char[3][3];
        char[][] pavingMaterials = new char[3][3];
        Map<Character, int[]> materialTextures = new HashMap<>();
        boolean hasRoadSurface = false;
        boolean hasPavingSurface = false;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int wx = tx + x - 1, wy = ty + y - 1;
                if (settlement) {
                    wx = Math.max(0, Math.min(world.width(state.currentMapId) - 1, wx));
                    wy = Math.max(0, Math.min(world.height(state.currentMapId) - 1, wy));
                }
                char raw = world.tileAt(state.currentMapId, wx, wy);
                char material = settlement
                        ? settlementMaterial(raw, settlementGround, city)
                        : painter.visibleTerrainTile(raw, wx, wy);
                if (settlement && RoadSurface.isMaterial(material)) {
                    if (settlementPaving(raw)) {
                        if (visiblePaving(state.currentMapId, wx, wy)) {
                            char paving = RoadSurface.paving(material);
                            pavingMaterials[y][x] = paving;
                            hasPavingSurface = true;
                            materialTextures.computeIfAbsent(paving, this::materialTexture);
                        }
                    } else {
                        roadMaterials[y][x] = material;
                        hasRoadSurface = true;
                        materialTextures.computeIfAbsent(material, this::materialTexture);
                    }
                    // Roads and courts are coverage layers over regional ground, not square terrain cells.
                    material = RoadSurface.ground(material);
                } else if (!settlement && RoadSurface.isRoad(raw)) {
                    char road = RoadSurface.material(raw, RoadSurface.groundAt(world, state.currentMapId, wx, wy));
                    roadMaterials[y][x] = road;
                    hasRoadSurface = true;
                    materialTextures.computeIfAbsent(road, this::materialTexture);
                    material = RoadSurface.ground(road);
                }
                if (material == 'd') material = 'g';
                materials[y][x] = material;
                materialTextures.computeIfAbsent(material, this::materialTexture);
            }
        }
        List<WorldMap.GroundRegion> nearby = settlement ? List.of() : regions.stream().filter(r ->
                Math.abs(tx + 0.5 - r.x() - 0.5) < r.radiusX() + 2
                        && Math.abs(ty + 0.5 - r.y() - 0.5) < r.radiusY() + 2).toList();
        if (materialTextures.size() == 1 && nearby.isEmpty() && !hasRoadSurface && !hasPavingSurface) {
            char material = materials[1][1];
            int[] sample = materialTextures.get(material);
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) {
                double worldY = ty + (y + 0.5) / size;
                int v = (int) Math.floor(worldY * TEXTURE_SIZE);
                for (int x = 0; x < size; x++) {
                    double worldX = tx + (x + 0.5) / size;
                    int u = (int) Math.floor(worldX * TEXTURE_SIZE);
                    pixels[y * size + x] = sampleTerrainMaterial(material, sample, u, v, worldX, worldY);
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
                int u = (int) Math.floor(x * TEXTURE_SIZE);
                int v = (int) Math.floor(y * TEXTURE_SIZE);
                int uv = Math.floorMod(v, TEXTURE_SIZE) * TEXTURE_SIZE + Math.floorMod(u, TEXTURE_SIZE);
                double red = 0, green = 0, blue = 0, sum = 0, waterAlpha = 0;
                double grain = (noise(x * 11, y * 11, 211) - 0.5) * 0.10;
                boolean shore = waterCoverage > 0.001 && waterCoverage < 0.999;
                for (int i = 0; i < count; i++) {
                    double feather = shore ? 0.11 : types[i] == 'n' ? 0.23 : 0.38;
                    double coverage = Math.max(0, weights[i] - max + feather + grain * ((types[i] & 1) == 0 ? 1 : -1));
                    double w = coverage * coverage;
                    int rgb = sampleTerrainMaterial(types[i], samples[i], u, v, x, y);
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
                        // Keep snow, sand and wetland habitat visible around authored structures.
                        double habitat = dominant == 'n' ? 0.18 : dominant == 's' || dominant == 'b' ? 0.30
                                : dominant == 'v' ? 0.38 : 0.76;
                        boolean burial = region.kind().equals("crypt") || region.kind().equals("graveyard");
                        int clearingRgb = burial ? mix(soil[uv], 0x55515b, 0.22) : soil[uv];
                        clearingRgb = switch (region.intent()) {
                            case "vampire_lair" -> mix(clearingRgb, 0x49353e, 0.30);
                            case "magic_tower" -> mix(gravel[uv], 0x494b70, 0.30);
                            case "castle", "prison" -> mix(clearingRgb, gravel[uv], 0.40);
                            default -> clearingRgb;
                        };
                        rgb = mix(rgb, clearingRgb, wear * habitat);
                        rgb = mix(rgb, gravel[uv], clearing * track * (dominant == 'n' ? 0.24 : 0.58));
                    }
                }
                if (hasPavingSurface && waterAlpha < 0.55) {
                    RoadCoverage paving = pavementCoverage(pavingMaterials, tx, ty, x, y);
                    if (paving.coverage > 0.001) {
                        int[] pavingTexture = materialTextures.get(paving.material);
                        int pavingRgb = pavingTexture[sampleIndex(pavingTexture, u, v)];
                        rgb = mix(rgb, pavingRgb, paving.coverage * 0.72);
                    }
                }
                if (hasRoadSurface && waterAlpha < 0.55) {
                    RoadCoverage road = roadCoverage(roadMaterials, tx, ty, x, y);
                    if (road.coverage > 0.001) {
                        int[] roadTexture = materialTextures.get(road.material);
                        int roadRgb = roadTexture[sampleIndex(roadTexture, u, v)];
                        rgb = mix(rgb, roadRgb, road.coverage * 0.98);
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

    static char villageGround(String mapId) {
        return switch (RegionalSettlementIdentity.region(mapId)) {
            case SUN -> 's';
            case NORTH, FREEHOLDS -> 'n';
            case FEN -> 'v';
            default -> 'g';
        };
    }

    static char villageMaterial(char raw, char ground) {
        return settlementMaterial(raw, ground, false);
    }

    static char settlementMaterial(char raw, char ground, boolean city) {
        return switch (raw) {
            case 'B' -> 'w';
            case 'r', 'T', '8', 'V', 'e' -> RoadSurface.material(city ? 'K' : 'r', ground);
            case 'p', 'j', 'a', 'l', 'C', 'G', 'K', 'q', 't' -> RoadSurface.material('K', ground);
            case 'U', '7' -> 'U';
            case 'y' -> 'g';
            case 'c' -> city ? RoadSurface.material('K', ground) : ground;
            case 'x', 'h', 'u', 'd' -> ground;
            default -> raw;
        };
    }

    private static boolean settlementPaving(char raw) {
        return "pjalCGt".indexOf(raw) >= 0;
    }

    private boolean visiblePaving(String mapId, int x, int y) {
        if (world.cityBuildingAt(mapId, x, y) != null) return true;
        int neighbors = 0;
        for (int oy = -1; oy <= 1; oy++) for (int ox = -1; ox <= 1; ox++) {
            if ((ox != 0 || oy != 0) && settlementPaving(world.tileAt(mapId, x + ox, y + oy))) neighbors++;
        }
        // Keep authored courts, plazas and long aprons; discard one- and two-cell paving litter.
        return neighbors >= 3;
    }

    /** Rounded capsules join neighboring road nodes, producing continuous lanes without tile-shaped shoulders. */
    private static RoadCoverage roadCoverage(char[][] roads, int tx, int ty, double x, double y) {
        double best = 0;
        char material = 0;
        for (int gy = 0; gy < 3; gy++) {
            for (int gx = 0; gx < 3; gx++) {
                char road = roads[gy][gx];
                if (!RoadSurface.isMaterial(road)) continue;
                if (!hasRoadNeighbor(roads, gx, gy)) continue;
                double cx = tx + gx - 0.5;
                double cy = ty + gy - 0.5;
                double radius = RoadSurface.stone(road) ? 0.41 : 0.36;
                double distance = Math.hypot(x - cx, y - cy);
                double coverage = roadEdgeCoverage(distance, radius, x, y);
                if (gx < 2 && RoadSurface.isMaterial(roads[gy][gx + 1])) {
                    double segment = segmentDistance(x, y, cx, cy, cx + 1, cy);
                    coverage = Math.max(coverage, roadEdgeCoverage(segment, radius, x, y));
                }
                if (gy < 2 && RoadSurface.isMaterial(roads[gy + 1][gx])) {
                    double segment = segmentDistance(x, y, cx, cy, cx, cy + 1);
                    coverage = Math.max(coverage, roadEdgeCoverage(segment, radius, x, y));
                }
                if (coverage > best) {
                    best = coverage;
                    material = road;
                }
            }
        }
        return new RoadCoverage(material, best);
    }

    /** Courts and building aprons retain area while their outer boundary receives rounded, weathered corners. */
    private static RoadCoverage pavementCoverage(char[][] paving, int tx, int ty, double x, double y) {
        double warpX = (noise(x / 1.7, y / 1.7, 1823) - 0.5) * 0.18;
        double warpY = (noise(x / 1.7, y / 1.7, 1847) - 0.5) * 0.18;
        double sx = x - 0.5 + warpX, sy = y - 0.5 + warpY;
        int ix = (int) Math.floor(sx), iy = (int) Math.floor(sy);
        double fx = smooth(sx - ix), fy = smooth(sy - iy);
        double field = 0;
        double strongest = 0;
        char material = 0;
        for (int corner = 0; corner < 4; corner++) {
            int ox = corner & 1, oy = corner >> 1;
            char surface = paving[iy - ty + 1 + oy][ix - tx + 1 + ox];
            if (!RoadSurface.isPaving(surface)) continue;
            double weight = (ox == 0 ? 1 - fx : fx) * (oy == 0 ? 1 - fy : fy);
            field += weight;
            if (weight > strongest) {
                    strongest = weight;
                    material = surface;
            }
        }
        double erosion = (noise(x * 4.1, y * 4.1, 1877) - 0.5) * 0.11;
        double coverage = smoothRange(0.28 + erosion, 0.72 + erosion, field);
        return new RoadCoverage(material, coverage);
    }

    private static boolean hasRoadNeighbor(char[][] roads, int x, int y) {
        return x > 0 && RoadSurface.isMaterial(roads[y][x - 1])
                || x < 2 && RoadSurface.isMaterial(roads[y][x + 1])
                || y > 0 && RoadSurface.isMaterial(roads[y - 1][x])
                || y < 2 && RoadSurface.isMaterial(roads[y + 1][x]);
    }

    private static double roadEdgeCoverage(double distance, double radius, double x, double y) {
        double irregularity = (noise(x * 2.7, y * 2.7, 509) - 0.5) * 0.12
                + (noise(x * 8.1, y * 8.1, 811) - 0.5) * 0.035;
        return 1 - smoothRange(radius - 0.11 + irregularity, radius + 0.10 + irregularity, distance);
    }

    private static double segmentDistance(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double length = dx * dx + dy * dy;
        double t = length == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / length));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    private int[] materialTexture(char material) {
        if (material == 's') return largeTexture("desert_sand_wind");
        if (material == '~') {
            int[] cached = textures.get("continuous-shallows");
            if (cached != null) return cached;
            int[] pixels = texture("water", false).clone();
            for (int i = 0; i < pixels.length; i++) pixels[i] = mix(pixels[i], 0x759b91, 0.18);
            textures.put("continuous-shallows", pixels);
            return pixels;
        }
        if (material == 'P') {
            int[] cached = textures.get("muted-beach");
            if (cached != null) return cached;
            int[] pixels = texture("beach", false).clone();
            for (int i = 0; i < pixels.length; i++) pixels[i] = mix(pixels[i], 0xac9d7b, 0.55);
            textures.put("muted-beach", pixels);
            return pixels;
        }
        if (RoadSurface.isPaving(material)) {
            char ground = RoadSurface.pavingGround(material);
            String key = "paving-surface:" + material;
            int[] cached = textures.get(key);
            if (cached != null) return cached;
            int tint = ground == 'n' ? 0x969b98 : ground == 's' ? 0xa48557
                    : ground == 'v' ? 0x666b62 : 0x898b82;
            int[] pixels = proceduralPavingTexture(tint, material);
            textures.put(key, pixels);
            return pixels;
        }
        if (RoadSurface.isMaterial(material)) {
            char ground = RoadSurface.ground(material);
            boolean stone = RoadSurface.stone(material);
            String key = "road-surface:" + material;
            int[] cached = textures.get(key);
            if (cached != null) return cached;
            int tint = ground == 'n' ? (stone ? 0x929791 : 0x847e70)
                    : ground == 's' ? (stone ? 0xb39866 : 0xa78351)
                    : ground == 'v' ? (stone ? 0x72776d : 0x706b58)
                    : (stone ? 0x96958c : 0x907957);
            int[] pixels = proceduralRoadTexture(tint, stone, material);
            textures.put(key, pixels);
            return pixels;
        }
        return texture(Terrain.assetName(material), false);
    }

    /**
     * Natural floors use a softly warped mesh of authored variants. The mesh lives in world space,
     * so camera movement, chunk caching and zoom changes cannot reshuffle the ground beneath actors.
     */
    private int sampleTerrainMaterial(char material, int[] fallback, int u, int v, double worldX, double worldY) {
        int variantCount = naturalVariantCount(material);
        if (variantCount < 2) return fallback[sampleIndex(fallback, u, v)];
        int[][] variants = naturalTerrainVariants.computeIfAbsent(material, this::loadNaturalTerrainVariants);

        int seed = 3109 + material * 47;
        double patchSize = material == 'f' ? 2.15 : material == 'g' ? 2.45 : 2.75;
        double warpX = (noise(worldX / 5.7, worldY / 5.7, seed + 11) - 0.5) * 1.15;
        double warpY = (noise(worldX / 5.7, worldY / 5.7, seed + 29) - 0.5) * 1.15;
        double meshX = worldX / patchSize + warpX;
        double meshY = worldY / patchSize + warpY;
        int cellX = (int) Math.floor(meshX), cellY = (int) Math.floor(meshY);
        double fx = smooth(meshX - cellX), fy = smooth(meshY - cellY);

        int topLeft = naturalVariantAt(cellX, cellY, material, variants.length);
        int topRight = naturalVariantAt(cellX + 1, cellY, material, variants.length);
        int bottomLeft = naturalVariantAt(cellX, cellY + 1, material, variants.length);
        int bottomRight = naturalVariantAt(cellX + 1, cellY + 1, material, variants.length);
        int sampleIndex = sampleIndex(fallback, u, v);
        int top = mix(variants[topLeft][sampleIndex], variants[topRight][sampleIndex], fx);
        int bottom = mix(variants[bottomLeft][sampleIndex], variants[bottomRight][sampleIndex], fx);
        int rgb = mix(top, bottom, fy);

        // A very broad, restrained value shift prevents even blended variant patches reading as a stamp grid.
        double tone = (noise(worldX / 4.8, worldY / 4.8, seed + 71) - 0.5) * 0.10;
        return tone < 0 ? mix(rgb, 0x172015, -tone) : mix(rgb, 0xc0b87a, tone);
    }

    private int[][] loadNaturalTerrainVariants(char material) {
        String base = Terrain.assetName(material);
        int count = naturalVariantCount(material);
        int[][] variants = new int[count][];
        variants[0] = texture(base, false);
        for (int i = 1; i < count; i++) variants[i] = texture(base + "_variant_" + i, false);
        return variants;
    }

    private static int naturalVariantAt(int x, int y, char material, int count) {
        return Math.min(count - 1, (int) Math.floor(random(x, y, 3251 + material * 31) * count));
    }

    static int naturalVariantCount(char material) {
        return switch (material) {
            case 'g', 'f', 'n' -> 8;
            case 'v', 'b' -> 4;
            default -> 1;
        };
    }

    /** A large world-space grain avoids repeating the border of legacy one-tile road stamps. */
    private static int[] proceduralRoadTexture(int tint, boolean stone, char material) {
        int[] pixels = new int[LARGE_TEXTURE_SIZE * LARGE_TEXTURE_SIZE];
        for (int y = 0; y < LARGE_TEXTURE_SIZE; y++) {
            for (int x = 0; x < LARGE_TEXTURE_SIZE; x++) {
                double broad = noise(x / 31.0, y / 31.0, 947 + material);
                double grain = noise(x / 5.5, y / 5.5, 1201 + material);
                double fleck = noise(x / 1.8, y / 1.8, 1693 + material);
                double shade = (broad - 0.5) * (stone ? 18 : 31)
                        + (grain - 0.5) * (stone ? 15 : 19)
                        + (fleck - 0.5) * 6;
                if (stone) {
                    int row = Math.floorDiv(y, 7);
                    int staggeredX = x + Math.floorMod(row, 2) * 5;
                    int cellX = Math.floorDiv(staggeredX, 10);
                    int lx = Math.floorMod(staggeredX, 10);
                    int ly = Math.floorMod(y, 7);
                    int edge = Math.min(Math.min(lx, 9 - lx), Math.min(ly, 6 - ly));
                    double stoneVariation = (random(cellX, row, 2203 + material) - 0.5) * 19;
                    shade += stoneVariation;
                    if (edge == 0) shade -= 25;
                    else if (edge == 1) shade -= 7;
                } else {
                    int cellX = Math.floorDiv(x, 6), cellY = Math.floorDiv(y, 6);
                    double pebbleX = 1.0 + random(cellX, cellY, 2333 + material) * 4.0;
                    double pebbleY = 1.0 + random(cellX, cellY, 2381 + material) * 4.0;
                    double pebble = Math.hypot(Math.floorMod(x, 6) - pebbleX, Math.floorMod(y, 6) - pebbleY);
                    if (pebble < 0.75 && random(cellX, cellY, 2417 + material) > 0.58) shade += 21;
                    else if (pebble < 1.25 && random(cellX, cellY, 2417 + material) > 0.58) shade -= 8;
                }
                int r = clampChannel(((tint >> 16) & 255) + shade);
                int g = clampChannel(((tint >> 8) & 255) + shade * 0.94);
                int b = clampChannel((tint & 255) + shade * 0.80);
                pixels[y * LARGE_TEXTURE_SIZE + x] = (r << 16) | (g << 8) | b;
            }
        }
        return pixels;
    }

    private static int[] proceduralPavingTexture(int tint, char material) {
        int[] pixels = new int[LARGE_TEXTURE_SIZE * LARGE_TEXTURE_SIZE];
        for (int y = 0; y < LARGE_TEXTURE_SIZE; y++) {
            int row = Math.floorDiv(y, 10);
            for (int x = 0; x < LARGE_TEXTURE_SIZE; x++) {
                int staggeredX = x + Math.floorMod(row, 2) * 7;
                int column = Math.floorDiv(staggeredX, 14);
                int lx = Math.floorMod(staggeredX, 14);
                int ly = Math.floorMod(y, 10);
                int edge = Math.min(Math.min(lx, 13 - lx), Math.min(ly, 9 - ly));
                double variation = (random(column, row, 2671 + material) - 0.5) * 24;
                double grain = (noise(x / 3.2, y / 3.2, 2711 + material) - 0.5) * 10;
                double shade = variation * 0.72 + grain + (edge == 0 ? -20 : edge == 1 ? -6 : 0);
                int r = clampChannel(((tint >> 16) & 255) + shade);
                int g = clampChannel(((tint >> 8) & 255) + shade * 0.96);
                int b = clampChannel((tint & 255) + shade * 0.88);
                pixels[y * LARGE_TEXTURE_SIZE + x] = (r << 16) | (g << 8) | b;
            }
        }
        return pixels;
    }

    private static int clampChannel(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }

    private int[] largeTexture(String name) {
        return textures.computeIfAbsent(name + ":large", key -> {
            int[] pixels = assets.image(name, LARGE_TEXTURE_SIZE, LARGE_TEXTURE_SIZE)
                    .getRGB(0, 0, LARGE_TEXTURE_SIZE, LARGE_TEXTURE_SIZE, null, 0, LARGE_TEXTURE_SIZE);
            // Keep the original assets intact; quiet the dunes and distinguish compacted travel surfaces.
            boolean sand = name.equals("desert_sand_wind");
            for (int i = 0; i < pixels.length; i++) pixels[i] = mix(pixels[i], sand ? 0xbda16f : 0x967d56, sand ? 0.40 : 0.24);
            return pixels;
        });
    }

    private static int sampleIndex(int[] texture, int x, int y) {
        if (texture.length == TEXTURE_SIZE * TEXTURE_SIZE) {
            return Math.floorMod(y, TEXTURE_SIZE) * TEXTURE_SIZE + Math.floorMod(x, TEXTURE_SIZE);
        }
        // Mirrored wrapping is continuous even when an image's opposite edges differ slightly.
        return mirror(y, LARGE_TEXTURE_SIZE) * LARGE_TEXTURE_SIZE + mirror(x, LARGE_TEXTURE_SIZE);
    }

    private static int mirror(int coordinate, int size) {
        int value = Math.floorMod(coordinate, size * 2);
        return value < size ? value : size * 2 - 1 - value;
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
    private record Variant(Surface surface, boolean derived) { }
    private record FamilyKey(String mapId, int x, int y, long visualRevision) { }
    private record RoadCoverage(char material, double coverage) { }
    private record Key(String mapId, int x, int y, int size, long visualRevision) { }
}
