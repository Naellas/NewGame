package com.alderfall.game;

import com.alderfall.game.render.world.WorldPropRenderer;
import com.alderfall.game.render.world.ConnectedInteriorWalls;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorldRenderer {
    private static final Color WORLD_VOID_COLOR = new Color(5, 6, 9);
    // Smaller chunks keep a one-step zoom-out from generating a large block of
    // terrain that is still mostly off-screen. Even with a larger entry limit,
    // their worst-case pixel footprint is below the previous 12x12 layout.
    private static final int TERRAIN_CHUNK_TILES = 4;
    private static final int TERRAIN_CHUNK_CACHE_LIMIT = 384;

    private final AssetStore assets;
    private final TerrainPainter terrainPainter;
    private final WorldPropRenderer propRenderer;
    private final LightingPainter lightingPainter;
    private final LayeredTerrainRenderer layeredTerrain;
    private final EditorTerrainRevisions editorTerrain=new EditorTerrainRevisions();
    private long terrainBuildCount;
    private boolean editorGroundVisible=true;
    private boolean editorSceneActive;
    private final CavernTerrainRenderer cavernTerrain;
    private final List<WorldPropRenderer.PropRenderData> visiblePropRenders = new ArrayList<>();
    private com.alderfall.game.map.WorldMap terrainCacheWorld;
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
        this.layeredTerrain = new LayeredTerrainRenderer(assets,editorTerrain);
        this.cavernTerrain = new CavernTerrainRenderer(assets);
    }

    void drawTerrainBase(Graphics2D g, TerrainContext context) {
        drawTerrainBase(g, context, true);
        drawSettlementWalls(g, context);
    }

    void prepareEditorTerrain(GameState state,boolean props){editorSceneActive=true;editorGroundVisible=props;editorTerrain.prepare(state.world.area(state.currentMapId),props);}
    void finishEditorTerrain(){editorSceneActive=false;editorTerrain.finish();editorGroundVisible=true;}
    java.awt.Rectangle editorPropBounds(WorldProp prop,GameState state,int size){return propRenderer.prepare(prop,new PropContext(state,0,0,1,1,size,100,0)).bounds();}
    long terrainBuildCount(){return terrainBuildCount;}

    void drawCachedTerrainBase(Graphics2D g, TerrainContext context) {
        if ((!editorSceneActive || editorGroundVisible) && com.alderfall.game.map.WorldMap.PLAYER_VILLAGE_ID.equals(context.state().currentMapId))
            editorTerrain.prepareVillage(context.state());
        else if (!context.state().currentMapId.startsWith("editor_")) editorTerrain.finish();
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
        drawSettlementWalls(g, context);
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
                if (LayeredTerrainRenderer.supports(context.mapKind())) {
                    if (nearWater(context, wx, wy)) {
                        drawLayeredWater(g, context, wx, wy, sx * context.tileSize(), sy * context.tileSize());
                    }
                    continue;
                }
                char tile = context.state().world.tileAt(mapId, wx, wy);
                char terrainTile = terrainPainter.visibleTerrainTile(tile, wx, wy);
                if ("city".equals(context.mapKind())) {
                    if (tile == 'x') terrainTile = settlementWallGround(mapId);
                    else if (tile == Terrain.CITY_GATE) terrainTile = Terrain.COBBLESTONE_ROAD;
                }
                if (terrainTile == 'w' || terrainTile == '~') {
                    terrainPainter.drawWaterAnimation(g, wx, wy, sx * context.tileSize(), sy * context.tileSize(), context.tileSize());
                }
            }
        }
    }

    private void drawTerrainBase(Graphics2D g, TerrainContext context, boolean drawAnimations) {
        String mapId = context.state().currentMapId;
        var cavern = "dungeon".equals(context.mapKind()) ? context.state().world.dungeonContext(mapId) : null;
        // Rear interior faces extend upward. Include the next row so its upper
        // half is painted into this chunk, rather than clipped at every fourth row.
        int rows = context.visibleRows() + ("interior".equals(context.mapKind()) ? 1 : 0);
        for (int sy = 0; sy < rows; sy++) {
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
                var currentArea = context.state().world.area(mapId);
                char visualGround = currentArea == null ? tile : currentArea.approachGroundAt(wx, wy, tile);
                char terrainTile = terrainPainter.visibleTerrainTile(visualGround, wx, wy);
                if (CavernTerrainRenderer.supports(cavern)) {
                    cavernTerrain.draw(g, context.state(), cavern, wx, wy, px, py, context.tileSize());
                    continue;
                }
                if (LayeredTerrainRenderer.supports(context.mapKind())) {
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
        drawNatureTrails(g, context);
        drawQuarterTileDetails(g, context);
    }

    private void drawNatureTrails(Graphics2D g, TerrainContext context) {
        var area = context.state().world.area(context.state().currentMapId);
        if (area == null) return;
        int size = context.tileSize();
        record TrailDrawing(int x, int y, char terrain, List<TilePoint> trail, int gapStart, int gapLength, boolean rough) { }
        var drawings = new ArrayList<TrailDrawing>();
        for (var site : area.natureSites()) drawings.add(new TrailDrawing(site.x(), site.y(), site.terrain(), site.trail(), -10, 0, false));
        for (var site : area.destinationApproaches()) drawings.add(new TrailDrawing(site.destination().x(), site.destination().y(),
                site.biome(), site.trail(), site.gapStart(), site.gapLength(), true));
        for (var site : drawings) {
            if (site.x() + 32 < context.camX() || site.x() - 32 > context.camX() + context.visibleCols()
                    || site.y() + 32 < context.camY() || site.y() - 32 > context.camY() + context.visibleRows()) continue;
            Color earth = switch (site.terrain()) {
                case 's', 'P' -> new Color(155, 129, 83); case 'n' -> new Color(119, 132, 137);
                case 'b' -> new Color(109, 75, 55); default -> new Color(107, 93, 59);
            };
            var trail = site.trail();
            for (int i = 1; i < trail.size(); i++) {
                if (i >= site.gapStart() && i < site.gapStart() + site.gapLength()) continue;
                var a = trail.get(i - 1); var b = trail.get(i);
                // Soft layered dabs fade into the landscape at the trail mouth. Absolute positions avoid chunk seams.
                double fade = Math.min(1, i / 3.0);
                for (int step = 0; step < 7; step++) {
                    if (site.rough() && Math.floorMod(a.x() * 31 + a.y() * 13 + step, 13) == 0) continue;
                    double t = step / 7.0;
                    double x = a.x() + .5 + (b.x() - a.x()) * t;
                    double y = a.y() + .5 + (b.y() - a.y()) * t;
                    for (int layer = 0; layer < 3; layer++) {
                        int diameter = Math.max(2, (int) (size * ((site.rough() ? .36 : .44) - layer * .08)));
                        g.setColor(new Color(earth.getRed(), earth.getGreen(), earth.getBlue(), (int) ((10 + layer * 5) * fade)));
                        int px = (int) Math.round(x * size) - context.camX() * size;
                        int py = (int) Math.round(y * size) - context.camY() * size;
                        g.fillOval(px - diameter / 2, py - diameter / 2, diameter, diameter);
                    }
                }
            }
        }
    }

    /** Bake static plants into the existing terrain chunks: no extra per-frame images or depth sorting. */
    void drawQuarterTileDetails(Graphics2D g, TerrainContext context) {
        if(!editorGroundVisible)return;
        if (assets == null) return;
        var state = context.state();
        WorldPropRenderer painter = propRenderer == null ? new WorldPropRenderer(assets, state, null) : propRenderer;
        var propContext = new PropContext(state, context.camX(), context.camY(), context.visibleCols(),
                context.visibleRows(), context.tileSize(), context.zoom(), 0, 0, 0);
        var details = new ArrayList<WorldPropRenderer.PropRenderData>();
        // Include adjacent owners so a sprite crossing a chunk edge is clipped identically on both sides.
        for (WorldProp prop : state.world.propsInBounds(state.currentMapId, context.camX() - 1, context.camY() - 1,
                context.camX() + context.visibleCols() + 1, context.camY() + context.visibleRows() + 1)) {
            if (prop.visualSlot() >= 0) details.add(painter.prepare(prop, propContext));
        }
        details.sort(java.util.Comparator.comparingDouble(WorldPropRenderer.PropRenderData::depthY)
                .thenComparingDouble(WorldPropRenderer.PropRenderData::depthX)
                .thenComparing(WorldPropRenderer.PropRenderData::asset)
                .thenComparingInt(render -> render.prop().size()));
        for (var detail : details) painter.drawPropImage(g, detail.asset(), detail.bounds().x, detail.bounds().y,
                detail.width(), detail.height(), 1.0f);
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
        if (terrainCacheWorld != context.state().world) {
            terrainChunkCache.clear();
            terrainCacheWorld = context.state().world;
        }
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
        long visualRevision = context.state().world.visualRevision(context.state().currentMapId);
        visualRevision=editorTerrain.revision(context.state().world.area(context.state().currentMapId),
                originX,originY,TERRAIN_CHUNK_TILES,
                TERRAIN_CHUNK_TILES + ("interior".equals(context.mapKind()) ? 1 : 0),visualRevision);
        TerrainChunk cached = terrainChunkCache.get(key);
        if (cached != null && cached.visualRevision == visualRevision) {
            return cached.image;
        }

        int size = TERRAIN_CHUNK_TILES * context.tileSize();
        TerrainChunk reusable = closestTerrainChunk(key, visualRevision);
        if (reusable != null) {
            BufferedImage image = scaleTerrainChunk(reusable.image, size);
            terrainChunkCache.put(key, new TerrainChunk(image, visualRevision, true));
            return image;
        }

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        terrainBuildCount++;
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
        terrainChunkCache.put(key, new TerrainChunk(image, visualRevision, false));
        return image;
    }

    private TerrainChunk closestTerrainChunk(TerrainChunkKey target, long visualRevision) {
        TerrainChunk closestNative = null;
        TerrainChunk closestAny = null;
        int nativeDistance = Integer.MAX_VALUE;
        int anyDistance = Integer.MAX_VALUE;
        for (Map.Entry<TerrainChunkKey, TerrainChunk> entry : terrainChunkCache.entrySet()) {
            TerrainChunkKey candidateKey = entry.getKey();
            TerrainChunk candidate = entry.getValue();
            if (candidate.visualRevision != visualRevision
                    || candidateKey.tileSize == target.tileSize
                    || candidateKey.chunkX != target.chunkX
                    || candidateKey.chunkY != target.chunkY
                    || candidateKey.mapWidth != target.mapWidth
                    || candidateKey.mapHeight != target.mapHeight
                    || !candidateKey.mapId.equals(target.mapId)
                    || !candidateKey.mapKind.equals(target.mapKind)) {
                continue;
            }
            int distance = Math.abs(candidateKey.tileSize - target.tileSize);
            if (distance < anyDistance) {
                closestAny = candidate;
                anyDistance = distance;
            }
            if (!candidate.derived && distance < nativeDistance) {
                closestNative = candidate;
                nativeDistance = distance;
            }
        }
        return closestNative != null ? closestNative : closestAny;
    }

    private BufferedImage scaleTerrainChunk(BufferedImage source, int size) {
        int type = source.getType() == BufferedImage.TYPE_INT_RGB
                ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;
        BufferedImage scaled = new BufferedImage(size, size, type);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.drawImage(source, 0, 0, size, size, null);
        graphics.dispose();
        return scaled;
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

    void rebuildNearbyProps(PropContext context, List<WorldProp> nearbyProps) {
        nearbyProps.clear();
        for (WorldProp prop : context.state().world.propsInTileOrder(
                context.state().currentMapId,
                context.camX() - 4,
                context.camY() - 4,
                context.camX() + context.visibleCols() + 4,
                context.camY() + context.visibleRows() + 4
        )) {
            if (prop.visualSlot() < 0) nearbyProps.add(prop);
        }
    }

    void drawGroundPropOverlays(Graphics2D g, PropContext context, List<WorldProp> nearbyProps) {
        int inset = scaled(2, context.zoom());
        for (WorldProp prop : nearbyProps) {
            if (!isGroundProp(prop.asset())) {
                continue;
            }
            if (usesLayeredLocationGround(context, prop)) continue;
            // Location dressing must not stamp square paving over a continuous travel surface.
            if (LayeredTerrainRenderer.supports(context.state().world.kind(context.state().currentMapId))
                    && !prop.asset().equals("location_farmland_tilled")
                    && RoadSurface.isRoad(context.state().world.tileAt(context.state().currentMapId, prop.x(), prop.y()))) continue;
            if (!isWorldPropVisible(prop, context)) {
                continue;
            }
            drawGroundPropOverlay(g, prop, context, inset);
        }
    }

    private boolean usesLayeredLocationGround(PropContext context, WorldProp prop) {
        if (!com.alderfall.game.map.WorldMap.OVERWORLD_ID.equals(context.state().currentMapId)
                || prop.asset().equals("location_farmland_tilled")) return false;
        return context.state().world.usesLayeredLocationGroundAt(prop.x(), prop.y());
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
        drawVisibleProps(g, context, nearbyProps, visibleProps, null);
    }

    void drawVisibleProps(Graphics2D g, PropContext context, List<WorldProp> nearbyProps, List<WorldProp> visibleProps, WorldDepthRenderer depth) {
        // Exporters without an actor pass still use the same wall/prop ordering.
        boolean localDepth = depth == null && "interior".equals(context.state().world.kind(context.state().currentMapId));
        if (localDepth) { depth = new WorldDepthRenderer(); depth.begin(context.tileSize()); }
        visibleProps.clear();
        visiblePropRenders.clear();
        for (WorldProp prop : nearbyProps) {
            if (prop.visualSlot() >= 0) continue;
            if (prop.asset().equals("location_overgrown_landing") && usesLayeredLocationGround(context, prop)) continue;
            if (!isGroundProp(prop.asset()) && isWorldPropVisible(prop, context)) {
                visiblePropRenders.add(propRenderer.prepare(prop, context));
            }
        }
        if ("interior".equals(context.state().world.kind(context.state().currentMapId))) {
            visiblePropRenders.sort(java.util.Comparator.comparingDouble(WorldPropRenderer.PropRenderData::depthY));
        } else if (PropPlacement.usesOffsets(context.state().world, context.state().currentMapId)) {
            visiblePropRenders.sort(java.util.Comparator
                    .comparingInt(WorldPropRenderer.PropRenderData::depthLayer)
                    .thenComparingDouble(WorldPropRenderer.PropRenderData::depthY)
                    .thenComparingDouble(WorldPropRenderer.PropRenderData::depthX)
                    .thenComparing(WorldPropRenderer.PropRenderData::asset)
                    .thenComparingInt(render -> render.prop().size()));
        }
        if (depth != null && "interior".equals(context.state().world.kind(context.state().currentMapId))) {
            var world = context.state().world;
            String map = context.state().currentMapId;
            int ts = context.tileSize();
            for (int y = context.camY(); y <= context.camY() + context.visibleRows() + 1; y++) {
                for (int x = context.camX(); x <= context.camX() + context.visibleCols(); x++) {
                    if (world.tileAt(map, x, y) != 'o') continue;
                    int wx = x, wy = y, px = (x - context.camX()) * ts, py = (y - context.camY()) * ts;
                    var silhouette = ConnectedInteriorWalls.bounds(world, map, x, y, px, py, ts);
                    // Restore the floor hidden by the cached upward projection.
                    // Otherwise a faded foreground wall would expose its baked copy.
                    char behind = world.tileAt(map, x, y - 1);
                    if (silhouette.y < py && ConnectedInteriorWalls.floor(behind)) {
                        Graphics2D under = (Graphics2D) g.create();
                        under.clip(silhouette);
                        terrainPainter.drawHouseTile(under, behind, x, y - 1, px, py - ts);
                        under.dispose();
                    }
                    if (ConnectedInteriorWalls.floor(behind) || ConnectedInteriorWalls.side(world, map, x, y)) {
                        Graphics2D under = (Graphics2D) g.create();
                        under.clip(silhouette);
                        terrainPainter.drawHouseTile(under, 'i', x, y, px, py);
                        under.dispose();
                    }
                    double wallDepth = py + ts;
                    if (ConnectedInteriorWalls.side(world, map, x, y)) {
                        // Side rails occlude tall sprites across the room boundary,
                        // while remaining scenery that the player's bubble can reveal.
                        if (world.tileAt(map, x, y + 1) == 'o' && !ConnectedInteriorWalls.side(world, map, x, y + 1)) {
                            var next = ConnectedInteriorWalls.bounds(world, map, x, y + 1, px, py + ts, ts);
                            silhouette.height = Math.min(silhouette.height, Math.max(0, next.y - silhouette.y));
                        }
                        for (var prop : visiblePropRenders) {
                            if (!prop.asset().startsWith("interior_wall_") && silhouette.intersects(prop.bounds()))
                                wallDepth = Math.max(wallDepth, (prop.depthY() - context.camY()) * ts + .01);
                        }
                    }
                    if (silhouette.isEmpty()) continue;
                    depth.wall(g, wallDepth, silhouette, target -> {
                        target.clip(silhouette);
                        ConnectedInteriorWalls.draw(target, assets, world, map, wx, wy, px, py, ts);
                    });
                }
            }
        }
        for (WorldPropRenderer.PropRenderData render : visiblePropRenders) {
            visibleProps.add(render.prop());
            if (depth == null || render.placement().kind() == PropPlacement.Kind.COVER
                    || render.asset().equals("location_overgrown_landing")) {
                propRenderer.drawWorldProp(g, render, context);
            } else {
                java.awt.Rectangle bounds = new java.awt.Rectangle(render.bounds());
                bounds.grow(context.tileSize(), context.tileSize());
                depth.scenery(g, (render.depthY() - context.camY()) * context.tileSize(), bounds,
                        target -> propRenderer.drawWorldProp(target, render, context));
            }
        }
        if (localDepth) depth.draw(g);
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
        } else if ((tile == 'x' || tile == 'o') && !"city".equals(context.mapKind())) {
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

    /**
     * Draws wall art after terrain chunks, so it can extend beyond collision tiles like a tree canopy.
     * Straight runs use multi-tile modules; towers and gates are drawn last to cover every join.
     */
    private void drawSettlementWalls(Graphics2D g, TerrainContext context) {
        if (!"city".equals(context.mapKind()) || assets == null) {
            return;
        }
        String mapId = context.state().currentMapId;
        int tileSize = context.tileSize();
        String prefix = settlementWallAssetPrefix(mapId);
        // Settlement maps are small; scanning the complete ring prevents a long run whose start is
        // off-camera from disappearing when the camera is looking at its middle.
        int minX = 0;
        int minY = 0;
        int maxX = context.mapWidth();
        int maxY = context.mapHeight();

        // Horizontal artwork represents three collision tiles and slightly overlaps the next module.
        for (int wy = minY; wy < maxY; wy++) {
            for (int wx = minX; wx < maxX; wx++) {
                if (!horizontalCityWallTile(context, wx, wy)
                        || horizontalCityWallTile(context, wx - 1, wy)) continue;
                int run = 0;
                while (horizontalCityWallTile(context, wx + run, wy)) run++;
                for (int offset = 0; offset < run; offset += 3) {
                    int span = Math.min(3, run - offset);
                    int drawWidth = Math.round((span + 0.36f) * tileSize);
                    int drawHeight = Math.round(tileSize * 2.5f);
                    double center = wx + offset + span / 2.0;
                    int drawX = (int) Math.round((center - context.camX()) * tileSize - drawWidth / 2.0);
                    int drawY = (wy - context.camY() + 1) * tileSize - drawHeight;
                    g.drawImage(assets.image(prefix + "_horizontal_seamless", drawWidth, drawHeight), drawX, drawY, null);
                }
            }
        }

        // Vertical artwork is deliberately wider than its collision column and spans two tiles at a time.
        for (int wx = minX; wx < maxX; wx++) {
            for (int wy = minY; wy < maxY; wy++) {
                if (!verticalCityWallTile(context, wx, wy)
                        || verticalCityWallTile(context, wx, wy - 1)) continue;
                int run = 0;
                while (verticalCityWallTile(context, wx, wy + run)) run++;
                for (int offset = 0; offset < run; offset += 3) {
                    int span = Math.min(3, run - offset);
                    int drawWidth = Math.round(tileSize * 1.5f);
                    int drawHeight = Math.round(Math.max(3.0f, span + 0.38f) * tileSize);
                    double center = wy + offset + span / 2.0;
                    int drawX = (int) Math.round((wx - context.camX() + 0.5) * tileSize - drawWidth / 2.0);
                    int drawY = (int) Math.round((center - context.camY()) * tileSize - drawHeight / 2.0);
                    g.drawImage(assets.image(prefix + "_vertical_seamless", drawWidth, drawHeight), drawX, drawY, null);
                }
            }
        }

        // Gatehouses cover their run once; adjacent straight modules terminate underneath their sockets.
        for (int wy = minY; wy < maxY; wy++) {
            for (int wx = minX; wx < maxX; wx++) {
                if (context.state().world.tileAt(mapId, wx, wy) != Terrain.CITY_GATE) continue;
                boolean horizontal = cityWallNode(context, wx - 1, wy) || cityWallNode(context, wx + 1, wy);
                boolean vertical = cityWallNode(context, wx, wy - 1) || cityWallNode(context, wx, wy + 1);
                drawSettlementGate(g, context, prefix, wx, wy,
                        (wx - context.camX()) * tileSize, (wy - context.camY()) * tileSize,
                        horizontal && !vertical);
            }
        }

        // Corner and endpoint towers are the top visual layer and hide all segment seams.
        for (int wy = minY; wy < maxY; wy++) {
            for (int wx = minX; wx < maxX; wx++) {
                if (context.state().world.tileAt(mapId, wx, wy) != 'x') continue;
                boolean horizontal = cityWallNode(context, wx - 1, wy) || cityWallNode(context, wx + 1, wy);
                boolean vertical = cityWallNode(context, wx, wy - 1) || cityWallNode(context, wx, wy + 1);
                if (horizontal == vertical && horizontal) {
                    drawWallTower(g, context, prefix + "_tower", wx, wy, 3.4f, 3.8f);
                } else if (!horizontal && !vertical) {
                    drawWallTower(g, context, prefix + "_end_tower", wx, wy, 3.0f, 3.8f);
                }
            }
        }
    }

    private boolean horizontalCityWallTile(TerrainContext context, int x, int y) {
        if (x < 0 || y < 0 || x >= context.mapWidth() || y >= context.mapHeight()
                || context.state().world.tileAt(context.state().currentMapId, x, y) != 'x') return false;
        boolean horizontal = cityWallNode(context, x - 1, y) || cityWallNode(context, x + 1, y);
        boolean vertical = cityWallNode(context, x, y - 1) || cityWallNode(context, x, y + 1);
        return horizontal && !vertical;
    }

    private boolean verticalCityWallTile(TerrainContext context, int x, int y) {
        if (x < 0 || y < 0 || x >= context.mapWidth() || y >= context.mapHeight()
                || context.state().world.tileAt(context.state().currentMapId, x, y) != 'x') return false;
        boolean horizontal = cityWallNode(context, x - 1, y) || cityWallNode(context, x + 1, y);
        boolean vertical = cityWallNode(context, x, y - 1) || cityWallNode(context, x, y + 1);
        return vertical && !horizontal;
    }

    private void drawWallTower(Graphics2D g, TerrainContext context, String asset, int wx, int wy,
                               float widthInTiles, float heightInTiles) {
        int tileSize = context.tileSize();
        int drawWidth = Math.round(tileSize * widthInTiles);
        int drawHeight = Math.round(tileSize * heightInTiles);
        int centerX = Math.round((wx - context.camX() + 0.5f) * tileSize);
        int bottomY = (wy - context.camY() + 1) * tileSize;
        g.drawImage(assets.spriteFit(asset, drawWidth, drawHeight),
                centerX - drawWidth / 2, bottomY - drawHeight, null);
    }

    private void drawSettlementGate(Graphics2D g, TerrainContext context, String prefix,
                                    int wx, int wy, int px, int py, boolean horizontal) {
        String mapId = context.state().currentMapId;
        int tileSize = context.tileSize();
        int run = 1;
        if (horizontal) {
            if (context.state().world.tileAt(mapId, wx - 1, wy) == Terrain.CITY_GATE) return;
            while (context.state().world.tileAt(mapId, wx + run, wy) == Terrain.CITY_GATE) run++;
        } else {
            if (context.state().world.tileAt(mapId, wx, wy - 1) == Terrain.CITY_GATE) return;
            while (context.state().world.tileAt(mapId, wx, wy + run) == Terrain.CITY_GATE) run++;
        }
        int drawWidth = horizontal ? Math.max(Math.round(tileSize * 5.2f), (run + 2) * tileSize)
                : Math.round(tileSize * 3.2f);
        int drawHeight = horizontal ? Math.round(tileSize * 3.4f) : Math.round(tileSize * 4.2f);
        int centerX = px + (horizontal ? run * tileSize / 2 : tileSize / 2);
        String module = horizontal ? "gate_horizontal_clean" : "gate_vertical_clean";
        int drawY = horizontal
                ? py + tileSize - drawHeight
                : py + run * tileSize / 2 - drawHeight / 2;
        g.drawImage(assets.spriteFit(prefix + "_" + module, drawWidth, drawHeight),
                centerX - drawWidth / 2, drawY, null);
    }

    private boolean cityWallNode(TerrainContext context, int x, int y) {
        if (x < 0 || y < 0 || x >= context.mapWidth() || y >= context.mapHeight()) return false;
        char tile = context.state().world.tileAt(context.state().currentMapId, x, y);
        return tile == 'x' || tile == Terrain.CITY_GATE;
    }

    private static String settlementWallAssetPrefix(String mapId) {
        return switch (RegionalSettlementIdentity.region(mapId)) {
            case NORTH -> "city_wall_north";
            case SUN -> "city_wall_sun";
            case FEN -> "city_wall_fen";
            case FREEHOLDS -> "city_wall_freeholds";
            default -> "city_wall_hearth";
        };
    }

    private static char settlementWallGround(String mapId) {
        return switch (RegionalSettlementIdentity.region(mapId)) {
            case NORTH, FREEHOLDS -> 'n';
            case SUN -> 's';
            case FEN -> 'v';
            default -> 'g';
        };
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

    private record TerrainChunk(BufferedImage image, long visualRevision, boolean derived) {
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
            int frame,
            double windStrength,
            double windRadians
    ) {
        public PropContext(GameState state, int camX, int camY, int visibleCols, int visibleRows,
                           int tileSize, int zoom, int frame) {
            this(state, camX, camY, visibleCols, visibleRows, tileSize, zoom, frame,
                    state.windStrength(), state.windRadians());
        }
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
