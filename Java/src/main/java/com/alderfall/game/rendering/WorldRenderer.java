package com.alderfall.game;

import com.alderfall.game.render.world.WorldPropRenderer;

import java.awt.Color;
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
        terrainChunkCache.put(key, new TerrainChunk(image, fingerprint));
        return image;
    }

    private long terrainChunkFingerprint(TerrainContext context, int originX, int originY) {
        String mapId = context.state().currentMapId;
        long hash = 1469598103934665603L;
        hash = mix(hash, mapId.hashCode());
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
            if (!isWorldPropVisible(prop, context)) {
                continue;
            }
            int px = (prop.x() - context.camX()) * context.tileSize() - inset;
            int py = (prop.y() - context.camY()) * context.tileSize() - inset;
            g.drawImage(assets.image(prop.asset(), context.tileSize() + inset * 2, context.tileSize() + inset * 2), px, py, null);
        }
    }

    void drawVisibleProps(Graphics2D g, PropContext context, List<WorldProp> nearbyProps, List<WorldProp> visibleProps) {
        visibleProps.clear();
        for (WorldProp prop : nearbyProps) {
            if (!isGroundProp(prop.asset()) && isWorldPropVisible(prop, context)) {
                visibleProps.add(prop);
            }
        }
        for (WorldProp prop : visibleProps) {
            propRenderer.drawWorldProp(g, prop, context);
        }
    }

    int propRenderSize(String asset, int logicalSize, int tileSize) {
        return propRenderer.propRenderSize(asset, logicalSize, tileSize);
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
        return prop.x() >= context.camX() && prop.y() >= context.camY()
                && prop.x() < context.camX() + context.visibleCols()
                && prop.y() < context.camY() + context.visibleRows();
    }

    private static boolean isGroundProp(String asset) {
        return asset.equals("location_farmland_tilled")
                || asset.equals("location_graveyard_dirt")
                || asset.equals("location_graveyard_path")
                || asset.equals("location_dungeon_approach_path");
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
