package com.alderfall.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

final class WorldRenderer {
    private static final Color WORLD_VOID_COLOR = new Color(5, 6, 9);

    private final AssetStore assets;
    private final TerrainPainter terrainPainter;
    private final PropPainter propPainter;
    private final LightingPainter lightingPainter;

    WorldRenderer(AssetStore assets, TerrainPainter terrainPainter, PropPainter propPainter, LightingPainter lightingPainter) {
        this.assets = assets;
        this.terrainPainter = terrainPainter;
        this.propPainter = propPainter;
        this.lightingPainter = lightingPainter;
    }

    void drawTerrainBase(Graphics2D g, TerrainContext context) {
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
                if (terrainTile == 'w' || terrainTile == '~') {
                    terrainPainter.drawWaterAnimation(g, wx, wy, px, py, context.tileSize());
                }
                drawTileOverlay(g, context, tile, wx, wy, px, py);
            }
        }
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
            propPainter.drawWorldProp(g, prop, context.camX(), context.camY(), context.tileSize());
        }
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

    record TerrainContext(
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

    record PropContext(
            GameState state,
            int camX,
            int camY,
            int visibleCols,
            int visibleRows,
            int tileSize,
            int zoom
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

    interface TerrainPainter {
        char visibleTerrainTile(char tile, int wx, int wy);

        String terrainImageName(char terrainTile, int wx, int wy);

        void drawTerrainEdges(Graphics2D g, char terrainTile, int wx, int wy, int px, int py);

        void drawWaterAnimation(Graphics2D g, int wx, int wy, int px, int py, int tileSize);

        void drawHouseTile(Graphics2D g, char tile, int wx, int wy, int px, int py);
    }

    interface PropPainter {
        void drawWorldProp(Graphics2D g, WorldProp prop, int camX, int camY, int tileSize);
    }

    interface LightingPainter {
        void drawBiomeLightTints(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize);

        void drawLightTileSpill(Graphics2D g, int camX, int camY, int visibleCols, int visibleRows, int tileSize);

        void drawWorldVignette(Graphics2D g);
    }
}
