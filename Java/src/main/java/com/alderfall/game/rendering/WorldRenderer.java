package com.alderfall.game;

import com.alderfall.game.render.world.WorldPropRenderer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorldRenderer {
    private static final Color WORLD_VOID_COLOR = new Color(5, 6, 9);
    private static final int TERRAIN_CHUNK_TILES = 12;
    private static final int TERRAIN_CHUNK_CACHE_LIMIT = 192;

    private final AssetStore assets;
    private final TerrainPainter terrainPainter;
    private final WorldPropRenderer propRenderer;
    private final LightingPainter lightingPainter;
    private final LayeredTerrainRenderer layeredTerrain;
    private final Map<TerrainChunkKey, TerrainChunk> terrainChunkCache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<TerrainChunkKey, TerrainChunk> eldest) {
            return size() > TERRAIN_CHUNK_CACHE_LIMIT;
        }
    };

    WorldRenderer(AssetStore assets, TerrainPainter terrainPainter, WorldPropRenderer propRenderer, LightingPainter lightingPainter) {
        this.assets = assets;
        this.terrainPainter = terrainPainter;
        this.propRenderer = propRenderer;
        this.lightingPainter = lightingPainter;
        this.layeredTerrain = new LayeredTerrainRenderer(assets);
    }

    void drawTerrainBase(Graphics2D g, TerrainContext context) {
        drawTerrainBase(g, context, true);
    }

    void drawCachedTerrainBase(Graphics2D g, TerrainContext context) {
        int firstChunkX = Math.floorDiv(context.camX(), TERRAIN_CHUNK_TILES);
        int firstChunkY = Math.floorDiv(context.camY(), TERRAIN_CHUNK_TILES);
        int lastChunkX = Math.floorDiv(context.camX() + context.visibleCols() - 1, TERRAIN_CHUNK_TILES);
        int lastChunkY = Math.floorDiv(context.camY() + context.visibleRows() - 1, TERRAIN_CHUNK_TILES);
        for (int chunkY = firstChunkY; chunkY <= lastChunkY; chunkY++) {
            for (int chunkX = firstChunkX; chunkX <= lastChunkX; chunkX++) {
                int originX = chunkX * TERRAIN_CHUNK_TILES;
                int originY = chunkY * TERRAIN_CHUNK_TILES;
                BufferedImage image = cachedTerrainChunk(context, chunkX, chunkY, originX, originY);
                int px = (originX - context.camX()) * context.tileSize();
                int py = (originY - context.camY()) * context.tileSize();
                g.drawImage(image, px, py, null);
            }
        }
    }

    void drawTerrainAnimations(Graphics2D g, TerrainContext context) {
        String mapId = context.state().currentMapId;
        for (int sy = 0; sy < context.visibleRows(); sy++) {
            for (int sx = 0; sx < context.visibleCols(); sx++) {
                int wx = context.camX() + sx;
                int wy = context.camY() + sy;
                if (wx < 0 || wy < 0 || wx >= context.mapWidth() || wy >= context.mapHeight()) {
                    continue;
                }
                if ("overworld".equals(context.mapKind())) {
                    if (nearWater(context, wx, wy)) {
                        drawLayeredWater(g, context, wx, wy, sx * context.tileSize(), sy * context.tileSize());
                    }
                    continue;
                }
                char tile = context.state().world.tileAt(mapId, wx, wy);
                char terrainTile = terrainPainter.visibleTerrainTile(tile, wx, wy);
                if (terrainTile == 'w' || terrainTile == '~') {
                    terrainPainter.drawWaterAnimation(g, wx, wy, sx * context.tileSize(), sy * context.tileSize(), context.tileSize());
                }
            }
        }
    }

    private void drawTerrainBase(Graphics2D g, TerrainContext context, boolean drawAnimations) {
        String mapId = context.state().currentMapId;
        for (int sy = 0; sy < context.visibleRows(); sy++) {
            for (int sx = 0; sx < context.visibleCols(); sx++) {
                int wx = context.camX() + sx;
                int wy = context.camY() + sy;
                int px = sx * context.tileSize();
                int py = sy * context.tileSize();
                if (wx < 0 || wy < 0 || wx >= context.mapWidth() || wy >= context.mapHeight()) {
                    g.setColor(WORLD_VOID_COLOR);
                    g.fillRect(px, py, context.tileSize(), context.tileSize());
                    continue;
                }
                char tile = context.state().world.tileAt(mapId, wx, wy);
                char terrainTile = terrainPainter.visibleTerrainTile(tile, wx, wy);
                if ("overworld".equals(context.mapKind())) {
                    g.drawImage(layeredTerrain.tile(context.state(), terrainPainter, wx, wy, context.tileSize()).image(), px, py, null);
                    if (drawAnimations && nearWater(context, wx, wy)) drawLayeredWater(g, context, wx, wy, px, py);
                    drawTileOverlay(g, context, tile, wx, wy, px, py);
                    continue;
                }
                String terrainImage = terrainPainter.terrainImageName(terrainTile, wx, wy);
                if ("dungeon".equals(context.mapKind()) && terrainImage.startsWith("dungeon")) {
                    g.setColor(new Color(22, 23, 30));
                    g.fillRect(px, py, context.tileSize(), context.tileSize());
                    g.drawImage(assets.imageWithoutBorder(terrainImage, context.tileSize(), context.tileSize()), px, py, null);
                } else {
                    g.drawImage(assets.image(terrainImage, context.tileSize(), context.tileSize()), px, py, null);
                }
                terrainPainter.drawTerrainEdges(g, terrainTile, wx, wy, px, py);
                if (drawAnimations && (terrainTile == 'w' || terrainTile == '~')) {
                    terrainPainter.drawWaterAnimation(g, wx, wy, px, py, context.tileSize());
                }
                drawTileOverlay(g, context, tile, wx, wy, px, py);
            }
        }
    }

    private boolean nearWater(TerrainContext context, int x, int y) {
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                char tile = context.state().world.tileAt(context.state().currentMapId, x + ox, y + oy);
                if (tile == 'w' || tile == '~' || tile == 'B') return true;
            }
        }
        return false;
    }

    private void drawLayeredWater(Graphics2D g, TerrainContext context, int wx, int wy, int px, int py) {
        LayeredTerrainRenderer.Surface surface = layeredTerrain.tile(context.state(), terrainPainter, wx, wy, context.tileSize());
        if (!surface.hasWater()) return;
        if (surface.waterClip() instanceof java.awt.geom.Rectangle2D) {
            terrainPainter.drawWaterAnimation(g, wx, wy, px, py, context.tileSize());
            return;
        }
        Graphics2D water = (Graphics2D) g.create();
        water.translate(px, py);
        water.clip(surface.waterClip());
        terrainPainter.drawWaterAnimation(water, wx, wy, 0, 0, context.tileSize());
        water.dispose();
    }

    private BufferedImage cachedTerrainChunk(TerrainContext context, int chunkX, int chunkY, int originX, int originY) {
        TerrainChunkKey key = new TerrainChunkKey(
                context.state().currentMapId,
                context.mapKind(),
                context.tileSize(),
                context.zoom(),
                context.mapWidth(),
                context.mapHeight(),
                chunkX,
                chunkY
        );
        long fingerprint = terrainChunkFingerprint(context, originX, originY);
        TerrainChunk cached = terrainChunkCache.get(key);
        if (cached != null && cached.fingerprint == fingerprint) {
            return cached.image;
        }

        int size = TERRAIN_CHUNK_TILES * context.tileSize();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D chunkGraphics = image.createGraphics();
        drawTerrainBase(chunkGraphics, new TerrainContext(
                context.state(),
                originX,
                originY,
                TERRAIN_CHUNK_TILES,
                TERRAIN_CHUNK_TILES,
                context.mapWidth(),
                context.mapHeight(),
                context.tileSize(),
                context.zoom(),
                context.mapKind()
        ), false);
        chunkGraphics.dispose();
        image = opaqueTerrainImage(image);
        terrainChunkCache.put(key, new TerrainChunk(image, fingerprint));
        return image;
    }

    // Opaque chunks can use Java2D's copy path instead of blending every pixel each frame.
    // Keep alpha for tilesets with transparent gaps; the check runs only when a chunk is built.
    private BufferedImage opaqueTerrainImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            image.getRGB(0, y, width, 1, row, 0, width);
            for (int pixel : row) {
                if ((pixel >>> 24) != 255) {
                    return image;
                }
            }
        }
        BufferedImage opaque = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = opaque.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return opaque;
    }

    private long terrainChunkFingerprint(TerrainContext context, int originX, int originY) {
        String mapId = context.state().currentMapId;
        long hash = 1469598103934665603L;
        hash = mix(hash, mapId.hashCode());
        hash = mix(hash, System.identityHashCode(context.state().world));
        hash = mix(hash, context.mapKind().hashCode());
        hash = mix(hash, context.mapWidth());
        hash = mix(hash, context.mapHeight());

        int margin = terrainFingerprintMargin(context.mapKind(), mapId);
        int minX = originX - margin;
        int minY = originY - margin;
        int maxX = originX + TERRAIN_CHUNK_TILES + margin;
        int maxY = originY + TERRAIN_CHUNK_TILES + margin;
        for (int wy = minY; wy < maxY; wy++) {
            for (int wx = minX; wx < maxX; wx++) {
                hash = mix(hash, wx);
                hash = mix(hash, wy);
                hash = mix(hash, context.state().world.tileAt(mapId, wx, wy));
                if ("interior".equals(context.mapKind())) {
                    hash = mix(hash, context.state().world.interiorRugAt(mapId, wx, wy) ? 1 : 0);
                }
                String landmark = context.state().world.landmarkAt(mapId, wx, wy);
                if (landmark != null) {
                    hash = mix(hash, landmark.hashCode());
                }
            }
        }
        return hash;
    }

    private int terrainFingerprintMargin(String mapKind, String mapId) {
        if ("overworld".equals(mapKind) || com.alderfall.game.map.WorldMap.OVERWORLD_ID.equals(mapId)) {
            return 6;
        }
        return 1;
    }

    private long mix(long hash, int value) {
        hash ^= value;
        return hash * 1099511628211L;
    }

    void rebuildNearbyProps(PropContext context, List<WorldProp> nearbyProps) {
        nearbyProps.clear();
        for (WorldProp prop : context.state().world.propsInTileOrder(
                context.state().currentMapId,
                context.camX() - 4,
                context.camY() - 4,
                context.camX() + context.visibleCols() + 4,
                context.camY() + context.visibleRows() + 4
        )) {
            nearbyProps.add(prop);
        }
    }

    void drawGroundPropOverlays(Graphics2D g, PropContext context, List<WorldProp> nearbyProps) {
        int inset = scaled(2, context.zoom());
        for (WorldProp prop : nearbyProps) {
            if (!isGroundProp(prop.asset())) {
                continue;
            }
            if (usesLayeredCampGround(context, prop)) continue;
            if (!isWorldPropVisible(prop, context)) {
                continue;
            }
            drawGroundPropOverlay(g, prop, context, inset);
        }
    }

    private boolean usesLayeredCampGround(PropContext context, WorldProp prop) {
        if (!com.alderfall.game.map.WorldMap.OVERWORLD_ID.equals(context.state().currentMapId)
                || prop.asset().equals("location_farmland_tilled")) return false;
        return context.state().world.campGroundCoverage(prop.x() + 0.5, prop.y() + 0.5) > 0.01;
    }

    private void drawGroundPropOverlay(Graphics2D g, WorldProp prop, PropContext context, int inset) {
        int px = (prop.x() - context.camX()) * context.tileSize();
        int py = (prop.y() - context.camY()) * context.tileSize();
        int cropPad = Math.max(inset * 3, scaled(5, context.zoom()));
        int sourceSize = context.tileSize() + cropPad * 4;
        int seed = prop.x() * 928371 + prop.y() * 364479 + prop.asset().hashCode();
        int jitterRange = cropPad * 2 + 1;
        int cropX = cropPad * 2 + Math.floorMod(seed, jitterRange) - cropPad;
        int cropY = cropPad * 2 + Math.floorMod(seed / 17, jitterRange) - cropPad;
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(groundPropOpacity(prop.asset())));
        g.drawImage(
                assets.image(prop.asset(), sourceSize, sourceSize),
                px, py, px + context.tileSize(), py + context.tileSize(),
                cropX, cropY, cropX + context.tileSize(), cropY + context.tileSize(),
                null
        );
        g.setComposite(oldComposite);
    }

    void drawVisibleProps(Graphics2D g, PropContext context, List<WorldProp> nearbyProps, List<WorldProp> visibleProps) {
        visibleProps.clear();
        for (WorldProp prop : nearbyProps) {
            if (prop.asset().equals("location_overgrown_landing") && usesLayeredCampGround(context, prop)) continue;
            if (!isGroundProp(prop.asset()) && isWorldPropVisible(prop, context)) {
                visibleProps.add(prop);
            }
        }
        if ("interior".equals(context.state().world.kind(context.state().currentMapId))) {
            visibleProps.sort(java.util.Comparator.comparingDouble(prop -> {
                int[] footprint = context.state().world.interiorVisualFootprint(prop.asset());
                double bottom = prop.y() + footprint[1];
                return bottom + (prop.asset().startsWith("interior_tabletop_")
                        || prop.asset().equals("interior_seed_bowl") || prop.asset().equals("interior_flower_vase")
                        || prop.asset().equals("interior_mortar_pestle") ? 0.1 : 0);
            }));
        } else if (com.alderfall.game.map.WorldMap.OVERWORLD_ID.equals(context.state().currentMapId)) {
            Map<WorldProp, PropPlacement.Placement> placements = new java.util.HashMap<>();
            for (WorldProp prop : visibleProps) placements.put(prop,
                    PropPlacement.at(context.state().world, context.state().currentMapId, prop));
            visibleProps.sort(java.util.Comparator.<WorldProp>comparingInt(prop ->
                            placements.get(prop).kind() == PropPlacement.Kind.COVER
                                    || prop.asset().equals("location_overgrown_landing") ? 0 : 1)
                    .thenComparingDouble(prop -> placements.get(prop).footY(prop))
                    .thenComparingDouble(prop -> prop.x() + placements.get(prop).x())
                    .thenComparing(WorldProp::asset).thenComparingInt(WorldProp::size));
        }
        for (WorldProp prop : visibleProps) {
            propRenderer.drawWorldProp(g, prop, context);
        }
    }

    int propRenderSize(String asset, int logicalSize, int tileSize) {
        return propRenderer.propRenderSize(asset, logicalSize, tileSize);
    }

    java.awt.Rectangle propBounds(WorldProp prop, int tileSize, int camX, int camY) {
        return propRenderer.propBounds(prop, tileSize, camX, camY);
    }

    java.awt.Rectangle interiorPropBounds(WorldProp prop, int tileSize, int camX, int camY) {
        return propRenderer.interiorBounds(prop, tileSize, camX, camY);
    }

    int propWidth(String asset, int size) {
        return propRenderer.interiorPropWidth(asset, size);
    }

    int propHeight(String asset, int size) {
        return propRenderer.interiorPropHeight(asset, size);
    }

    BufferedImage propImage(String asset, int width, int height) {
        return propRenderer.propImage(asset, width, height);
    }

    void drawPropImage(Graphics2D g, String asset, int x, int y, int width, int height, float opacity) {
        propRenderer.drawPropImage(g, asset, x, y, width, height, opacity);
    }

    void drawFallingGatheredProp(Graphics2D g, PropContext context) {
        propRenderer.drawFallingGatheredProp(g, context);
    }

    void drawLighting(Graphics2D g, LightingContext context) {
        Graphics2D light = (Graphics2D) g.create();
        light.setClip(0, 0, context.width(), context.height());
        light.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        lightingPainter.drawBiomeLightTints(light, context.camX(), context.camY(), context.visibleCols(), context.visibleRows(), context.tileSize());
        lightingPainter.drawLightTileSpill(light, context.camX(), context.camY(), context.visibleCols(), context.visibleRows(), context.tileSize());
        light.dispose();
    }

    void drawVignette(Graphics2D g, int width, int height) {
        Graphics2D light = (Graphics2D) g.create();
        light.setClip(0, 0, width, height);
        lightingPainter.drawWorldVignette(light);
        light.dispose();
    }

    private void drawTileOverlay(Graphics2D g, TerrainContext context, char tile, int wx, int wy, int px, int py) {
        if ("interior".equals(context.mapKind())) {
            terrainPainter.drawHouseTile(g, tile, wx, wy, px, py);
        } else if (tile == 'h') {
            g.setColor(new Color(72, 47, 36, 96));
            g.fillRect(
                    px + scaled(6, context.zoom()),
                    py + scaled(8, context.zoom()),
                    context.tileSize() - scaled(12, context.zoom()),
                    context.tileSize() - scaled(10, context.zoom())
            );
        } else if (tile == 'x' || tile == 'o') {
            g.setColor(new Color(40, 38, 45, 120));
            g.fillRect(px, py, context.tileSize(), context.tileSize());
        }
        String landmark = context.state().world.landmarkAt(context.state().currentMapId, wx, wy);
        if (landmark != null) {
            g.setColor(new Color(255, 245, 174));
            g.fillOval(
                    px + scaled(19, context.zoom()),
                    py + scaled(4, context.zoom()),
                    scaled(10, context.zoom()),
                    scaled(10, context.zoom())
            );
        }
    }

    private static int scaled(int value, int zoom) {
        return Math.max(1, value * zoom / 100);
    }

    private static boolean isWorldPropVisible(WorldProp prop, PropContext context) {
        return prop.x() >= context.camX() - 3 && prop.y() >= context.camY() - 3
                && prop.x() < context.camX() + context.visibleCols() + 2
                && prop.y() < context.camY() + context.visibleRows() + 2;
    }

    private static boolean isGroundProp(String asset) {
        return asset.equals("location_farmland_tilled")
                || asset.equals("location_graveyard_dirt")
                || asset.equals("location_graveyard_path")
                || asset.equals("location_dungeon_approach_path");
    }

    private static float groundPropOpacity(String asset) {
        return switch (asset) {
            case "location_graveyard_dirt" -> 0.88f;
            case "location_graveyard_path" -> 0.90f;
            case "location_farmland_tilled" -> 0.92f;
            default -> 0.95f;
        };
    }

    private record TerrainChunkKey(
            String mapId,
            String mapKind,
            int tileSize,
            int zoom,
            int mapWidth,
            int mapHeight,
            int chunkX,
            int chunkY
    ) {
    }

    private record TerrainChunk(BufferedImage image, long fingerprint) {
    }

    public record TerrainContext(
            GameState state,
            int camX,
            int camY,
            int visibleCols,
            int visibleRows,
            int mapWidth,
            int mapHeight,
            int tileSize,
            int zoom,
            String mapKind
    ) {
    }

    public record PropContext(
            GameState state,
            int camX,
            int camY,
            int visibleCols,
            int visibleRows,
            int tileSize,
            int zoom,
            int frame
    ) {
    }

    record LightingContext(
            int camX,
            int camY,
            int visibleCols,
            int visibleRows,
            int tileSize,
            int width,
            int height
    ) {
    }

    public interface TerrainPainter {
        char visibleTerrainTile(char tile, int wx, int wy);

        String terrainImageName(char terrainTile, int wx, int wy);

        void drawTerrainEdges(Graphics2D g, char terrainTile, int wx, int wy, int px, int py);

        void drawWaterAnimation(Graphics2D g, int wx, int wy, int px, int py, int tileSize);

        void drawHouseTile(Graphics2D g, char tile, int wx, int wy, int px, int py);
    }

    public interface LightingPainter {
        void drawBiomeLightTints(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize);

        void drawLightTileSpill(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize);

        void drawWorldVignette(Graphics2D g);
    }
}
