package com.alderfall.game.render.world;

import com.alderfall.game.*;

import com.alderfall.game.map.WorldMap;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class TerrainFeatureRenderer {
    private final AssetStore assets;
    private final GameState state;
    private final WeatherSystem weather;
    private final WaterTileRenderer waterTileRenderer;
    private final Effects effects;
    private RenderContext context;

    public TerrainFeatureRenderer(AssetStore assets, GameState state, WeatherSystem weather,
                           WaterTileRenderer waterTileRenderer, Effects effects) {
        this.assets = assets;
        this.state = state;
        this.weather = weather;
        this.waterTileRenderer = waterTileRenderer;
        this.effects = effects;
    }

    public void useContext(RenderContext context) {
        this.context = context;
    }

    private int tileSize() {
        return context.tileSize();
    }

    private int scaled(int value) {
        return Math.max(1, value * state.zoom / 100);
    }

    private float scaledStroke(float value) {
        return Math.max(1f, value * state.zoom / 100f);
    }

    private int tileRelative(int value, int tileSize) {
        return Math.max(1, Math.round(value * tileSize / (float) GameConfig.TILE));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public void drawTerrainEdges(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        String mapKind = state.world.kind(state.currentMapId);
        if (!"overworld".equals(mapKind) && !"dungeon".equals(mapKind)) {
            return;
        }
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int i = 0; i < dirs.length; i++) {
            int nx = wx + dirs[i][0];
            int ny = wy + dirs[i][1];
            char other = visibleTerrainTile(state.world.tileAt(state.currentMapId, nx, ny), nx, ny);
                if (other == tile || Terrain.connectingRoad(other) || Terrain.connectingRoad(tile)) {
                    continue;
                }
            if (!shouldBlend(tile, other)) {
                continue;
            }
            drawTerrainTextureBlend(g, tile, other, i, wx, wy, nx, ny, px, py);
        }
        drawDiagonalTerrainBlend(g, tile, wx, wy, px, py);
    }

    public void drawSettlementSurfaceSeams(Graphics2D g, int camX, int camY) {
        String kind = state.world.kind(state.currentMapId);
        if (!"city".equals(kind)) {
            return;
        }
        int ts = tileSize();
        int seam = Math.max(2, tileRelative("city".equals(kind) ? 8 : 6, ts));
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver);
        for (int sy = 0; sy < context.visibleRows(); sy++) {
            for (int sx = 0; sx < context.visibleCols(); sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                if (!isSettlementSurface(tile)) {
                    continue;
                }
                int px = sx * ts;
                int py = sy * ts;
                char east = state.world.tileAt(state.currentMapId, wx + 1, wy);
                if (isSameSettlementSurfaceBand(tile, east)) {
                    g.setColor(settlementSurfaceSeamColor(tile, east));
                    g.fillRect(px + ts - seam / 2, py, seam, ts);
                }
                char south = state.world.tileAt(state.currentMapId, wx, wy + 1);
                if (isSameSettlementSurfaceBand(tile, south)) {
                    g.setColor(settlementSurfaceSeamColor(tile, south));
                    g.fillRect(px, py + ts - seam / 2, ts, seam);
                }
            }
        }
        g.setComposite(oldComposite);
    }

    private boolean isSettlementSurface(char tile) {
        return "pCjlayGV".indexOf(tile) >= 0;
    }

    private boolean isSameSettlementSurfaceBand(char a, char b) {
        if (!isSettlementSurface(a) || !isSettlementSurface(b)) {
            return false;
        }
        return settlementSurfaceBand(a) == settlementSurfaceBand(b);
    }

    private int settlementSurfaceBand(char tile) {
        if (tile == 'G' || tile == 'V' || tile == 'y') {
            return 1;
        }
        if (tile == 'a') {
            return 2;
        }
        return 0;
    }

    private Color settlementSurfaceSeamColor(char a, char b) {
        Color ca = Terrain.color(a);
        Color cb = Terrain.color(b);
        int alpha = settlementSurfaceBand(a) == 0 ? 72 : 46;
        return new Color((ca.getRed() + cb.getRed()) / 2,
                (ca.getGreen() + cb.getGreen()) / 2,
                (ca.getBlue() + cb.getBlue()) / 2,
                alpha);
    }

    private boolean shouldBlend(char tile, char other) {
        if ("dungeon".equals(state.world.kind(state.currentMapId))) {
            return isDungeonFloorMaterial(tile) && isDungeonFloorMaterial(other) && tile != other;
        }
        return isNatural(tile) && isNatural(other) && tile != other;
    }

    private boolean isDungeonFloorMaterial(char tile) {
        return "dDFMRLNEIJHQ1234".indexOf(tile) >= 0;
    }

    public boolean isNatural(char tile) {
        return "gfsnvbmqwP~".indexOf(tile) >= 0;
    }

    private Color edgeColor(char tile, char other) {
        if (tile == 'w' || other == 'w' || tile == '~' || other == '~') {
            return new Color(122, 191, 139, 170);
        }
        Color a = Terrain.color(tile);
        Color b = Terrain.color(other);
        return new Color((a.getRed() + b.getRed()) / 2, (a.getGreen() + b.getGreen()) / 2, (a.getBlue() + b.getBlue()) / 2, 112);
    }

    private void drawTerrainTextureBlend(Graphics2D g, char tile, char other, int direction,
                                         int wx, int wy, int nx, int ny, int px, int py) {
        int ts = tileSize();
        int seed = terrainTransitionSeed(wx, wy, direction, other);
        Polygon transition = terrainTransitionPolygon(px, py, ts, direction, seed);
        BufferedImage texture = "dungeon".equals(state.world.kind(state.currentMapId))
                ? assets.imageWithoutBorder(effects.terrainImageName(other, nx, ny), ts, ts)
                : assets.image(effects.terrainImageName(other, nx, ny), ts, ts);

        Graphics2D blend = (Graphics2D) g.create();
        blend.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        blend.setClip(transition);
        blend.setComposite(AlphaComposite.SrcOver.derive(terrainTextureAlpha(tile, other)));
        blend.drawImage(texture, px, py, null);
        blend.setComposite(AlphaComposite.SrcOver.derive(terrainTintAlpha(tile, other)));
        blend.setColor(edgeColor(tile, other));
        blend.fill(transition);
        blend.dispose();

        drawTerrainTransitionFlecks(g, tile, other, direction, wx, wy, px, py, seed);
    }

    private Polygon terrainTransitionPolygon(int px, int py, int ts, int direction, int seed) {
        int shallow = scaled(3 + Math.floorMod(seed, 4));
        int mid = scaled(7 + Math.floorMod(seed / 7, 7));
        int deep = scaled(11 + Math.floorMod(seed / 17, 9));
        int a = Math.floorMod(seed / 31, Math.max(1, ts / 3)) - ts / 6;
        int b = Math.floorMod(seed / 53, Math.max(1, ts / 3)) - ts / 6;
        return switch (direction) {
            case 0 -> new Polygon(
                    new int[]{px, px + ts / 3 + a, px + ts * 2 / 3 + b, px + ts, px + ts, px},
                    new int[]{py, py + mid, py + shallow, py + deep, py, py},
                    6);
            case 1 -> new Polygon(
                    new int[]{px + ts, px + ts, px + ts - deep, px + ts - shallow, px + ts - mid, px + ts},
                    new int[]{py, py + ts, py + ts, py + ts * 2 / 3 + b, py + ts / 3 + a, py},
                    6);
            case 2 -> new Polygon(
                    new int[]{px, px + ts, px + ts, px + ts * 2 / 3 + b, px + ts / 3 + a, px},
                    new int[]{py + ts, py + ts, py + ts - mid, py + ts - deep, py + ts - shallow, py + ts},
                    6);
            case 3 -> new Polygon(
                    new int[]{px, px + mid, px + shallow, px + deep, px, px},
                    new int[]{py, py + ts / 3 + a, py + ts * 2 / 3 + b, py + ts, py + ts, py},
                    6);
            default -> new Polygon();
        };
    }

    private void drawDiagonalTerrainBlend(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        int[][] diagonals = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        int ts = tileSize();
        for (int i = 0; i < diagonals.length; i++) {
            int nx = wx + diagonals[i][0];
            int ny = wy + diagonals[i][1];
            char other = visibleTerrainTile(state.world.tileAt(state.currentMapId, nx, ny), nx, ny);
            if (!shouldBlend(tile, other)) {
                continue;
            }
            char sideA = visibleTerrainTile(state.world.tileAt(state.currentMapId, wx + diagonals[i][0], wy), wx + diagonals[i][0], wy);
            char sideB = visibleTerrainTile(state.world.tileAt(state.currentMapId, wx, wy + diagonals[i][1]), wx, wy + diagonals[i][1]);
            if (sideA == other || sideB == other) {
                continue;
            }
            int seed = terrainTransitionSeed(wx, wy, i + 8, other);
            int radius = scaled(10 + Math.floorMod(seed, 7));
            Polygon corner = switch (i) {
                case 0 -> new Polygon(new int[]{px, px + radius, px}, new int[]{py, py, py + radius}, 3);
                case 1 -> new Polygon(new int[]{px + ts, px + ts, px + ts - radius}, new int[]{py, py + radius, py}, 3);
                case 2 -> new Polygon(new int[]{px + ts, px + ts - radius, px + ts}, new int[]{py + ts, py + ts, py + ts - radius}, 3);
                default -> new Polygon(new int[]{px, px, px + radius}, new int[]{py + ts, py + ts - radius, py + ts}, 3);
            };
            Graphics2D blend = (Graphics2D) g.create();
            blend.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            blend.setClip(corner);
            blend.setComposite(AlphaComposite.SrcOver.derive(terrainTextureAlpha(tile, other) * 0.72f));
            BufferedImage texture = "dungeon".equals(state.world.kind(state.currentMapId))
                    ? assets.imageWithoutBorder(effects.terrainImageName(other, nx, ny), ts, ts)
                    : assets.image(effects.terrainImageName(other, nx, ny), ts, ts);
            blend.drawImage(texture, px, py, null);
            blend.setComposite(AlphaComposite.SrcOver.derive(terrainTintAlpha(tile, other) * 0.65f));
            blend.setColor(edgeColor(tile, other));
            blend.fill(corner);
            blend.dispose();
        }
    }

    private void drawTerrainTransitionFlecks(Graphics2D g, char tile, char other, int direction,
                                             int wx, int wy, int px, int py, int seed) {
        int ts = tileSize();
        int count = isWaterLike(tile) || isWaterLike(other) ? 7 : 10;
        Graphics2D flecks = (Graphics2D) g.create();
        flecks.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        flecks.setComposite(AlphaComposite.SrcOver.derive(isWaterLike(tile) || isWaterLike(other) ? 0.26f : 0.20f));
        for (int i = 0; i < count; i++) {
            int local = terrainTransitionSeed(wx + i, wy - i, direction + i * 3, other);
            int along = Math.floorMod(local / 11, Math.max(1, ts - scaled(8))) + scaled(4);
            int depth = scaled(2 + Math.floorMod(local / 37, 11));
            int jitter = scaled(Math.floorMod(local / 71, 7) - 3);
            int x = switch (direction) {
                case 1 -> px + ts - depth;
                case 3 -> px + depth;
                default -> px + along;
            };
            int y = switch (direction) {
                case 0 -> py + depth;
                case 2 -> py + ts - depth;
                default -> py + along;
            };
            if (direction == 0 || direction == 2) {
                x += jitter;
            } else {
                y += jitter;
            }
            int w = Math.max(1, scaled(2 + Math.floorMod(local, 4)));
            int h = Math.max(1, scaled(1 + Math.floorMod(local / 5, 3)));
            flecks.setColor(terrainFleckColor(tile, other, local));
            flecks.fillOval(x - w / 2, y - h / 2, w, h);
        }
        flecks.dispose();
    }

    private int terrainTransitionSeed(int wx, int wy, int direction, char other) {
        return Math.abs(wx * 928371 + wy * 364479 + direction * 8191 + other * 131 + state.currentMapId.hashCode());
    }

    private float terrainTextureAlpha(char tile, char other) {
        if (isWaterLike(tile) || isWaterLike(other)) {
            return 0.28f;
        }
        return 0.24f;
    }

    private float terrainTintAlpha(char tile, char other) {
        if (isWaterLike(tile) || isWaterLike(other)) {
            return 0.08f;
        }
        return 0.06f;
    }

    private boolean isWaterLike(char tile) {
        return tile == 'w' || tile == '~';
    }

    private Color terrainFleckColor(char tile, char other, int seed) {
        if (isWaterLike(tile) || isWaterLike(other)) {
            return Math.floorMod(seed, 3) == 0
                    ? new Color(197, 224, 203, 138)
                    : new Color(66, 129, 113, 126);
        }
        Color a = Terrain.color(tile);
        Color b = Terrain.color(other);
        double mix = 0.34 + Math.floorMod(seed, 32) / 100.0;
        int r = clampColor((int) Math.round(a.getRed() * (1.0 - mix) + b.getRed() * mix));
        int gr = clampColor((int) Math.round(a.getGreen() * (1.0 - mix) + b.getGreen() * mix));
        int bl = clampColor((int) Math.round(a.getBlue() * (1.0 - mix) + b.getBlue() * mix));
        return new Color(r, gr, bl, 132);
    }

    public void drawWaterAnimation(Graphics2D g, int wx, int wy, int px, int py, int tileSize) {
        double wave = weather.waterWave();
        int seed = Math.abs(wx * 928371 + wy * 364479 + state.currentMapId.hashCode());
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        Paint oldPaint = g.getPaint();
        Object oldAntialiasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        waterTileRenderer.draw(g, wx, wy, px, py, tileSize, context.frame(), state.currentMapId, weather, effects.weatherQuality());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double rainIntensity = weather.rainIntensity();
        double stormIntensity = weather.stormIntensity();
        double rippleThreshold = effects.weatherQuality().waterRippleThreshold;
        if (rainIntensity > rippleThreshold) {
            drawWaterRainRings(g, wx, wy, px, py, tileSize, seed, false, rainIntensity);
        }
        if (stormIntensity > rippleThreshold) {
            drawWaterRainRings(g, wx, wy, px, py, tileSize, seed, true, stormIntensity);
        }
        // Overworld shorelines now follow a continuous coverage mask, not tile-edge lines.
        if (!"overworld".equals(state.world.kind(state.currentMapId))
                && !"village".equals(state.world.kind(state.currentMapId))) {
            drawWaterShoreFoam(g, wx, wy, px, py, tileSize, wave);
        }
        g.setComposite(oldComposite);
        g.setStroke(oldStroke);
        g.setPaint(oldPaint);
        if (oldAntialiasing != null) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
        }
    }

    private void drawWaterRainRings(Graphics2D g, int wx, int wy, int px, int py, int tileSize, int seed, boolean heavy, double intensity) {
        int rings = (int) Math.round((heavy ? 4 : 2) * intensity);
        if (rings <= 0) {
            return;
        }
        for (int i = 0; i < rings; i++) {
            int phase = Math.floorMod(context.frame() * (heavy ? 2 : 1) + seed / (i + 3) + i * 13, 32);
            if (phase > 20) {
                continue;
            }
            int cx = px + Math.floorMod(seed / (i + 5) + i * 17, Math.max(1, tileSize));
            int cy = py + Math.floorMod(seed / (i + 7) + i * 23, Math.max(1, tileSize));
            int radiusX = scaled(3) + phase * scaled(9) / 20;
            int radiusY = Math.max(1, radiusX / 3);
            float alpha = (float) ((heavy ? 0.33f : 0.22f) * (1.0f - phase / 22.0f) * intensity);
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, alpha)));
            g.setColor(new Color(207, 235, 246));
            g.drawOval(cx - radiusX, cy - radiusY, radiusX * 2, radiusY * 2);
        }
    }

    private void drawWaterShoreFoam(Graphics2D g, int wx, int wy, int px, int py, int tileSize, double wave) {
        float alpha = (float) (0.16 + wave * 0.25);
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g.setColor(new Color(212, 238, 225));
        g.setStroke(new BasicStroke(Math.max(1f, scaledStroke(1.0f + (float) wave)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int inset = scaled(2);
        int wobble = (int) Math.round(Math.sin(context.frame() * 0.09 + wx * 0.6 + wy * 0.4) * scaled(2));
        if (!isWaterTile(wx, wy - 1)) {
            g.drawLine(px + scaled(5), py + inset + wobble, px + tileSize - scaled(5), py + inset - wobble);
        }
        if (!isWaterTile(wx + 1, wy)) {
            g.drawLine(px + tileSize - inset + wobble, py + scaled(5), px + tileSize - inset - wobble, py + tileSize - scaled(5));
        }
        if (!isWaterTile(wx, wy + 1)) {
            g.drawLine(px + scaled(5), py + tileSize - inset - wobble, px + tileSize - scaled(5), py + tileSize - inset + wobble);
        }
        if (!isWaterTile(wx - 1, wy)) {
            g.drawLine(px + inset - wobble, py + scaled(5), px + inset + wobble, py + tileSize - scaled(5));
        }
    }

    public boolean isWaterTile(int x, int y) {
        char tile = visibleTerrainTile(state.world.tileAt(state.currentMapId, x, y), x, y);
        return tile == 'w' || tile == '~';
    }

    public void drawFieldConnectors(Graphics2D g, int camX, int camY) {
        int ts = tileSize();
        int seam = Math.max(8, tileRelative(32, ts));
        int filler = ts;
        BufferedImage vertical = assets.image("field_connector_vertical", seam, ts);
        BufferedImage horizontal = assets.image("field_connector_horizontal", ts, seam);
        BufferedImage center = assets.image("field_junction_filler", filler, filler);
        for (int sy = 0; sy < context.visibleRows(); sy++) {
            for (int sx = 0; sx < context.visibleCols(); sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                if (!isFieldTile(wx, wy)) {
                    continue;
                }
                int px = sx * ts;
                int py = sy * ts;
                if (isFieldTile(wx + 1, wy)) {
                    g.drawImage(vertical, px + ts - seam / 2, py, null);
                }
                if (isFieldTile(wx, wy + 1)) {
                    g.drawImage(horizontal, px, py + ts - seam / 2, null);
                }
                if (isFieldTile(wx + 1, wy) && isFieldTile(wx, wy + 1) && isFieldTile(wx + 1, wy + 1)) {
                    g.drawImage(center, px + ts - filler / 2, py + ts - filler / 2, null);
                }
            }
        }
    }

    private boolean isFieldTile(int wx, int wy) {
        return state.world.tileAt(state.currentMapId, wx, wy) == 'A';
    }

    public void drawRoadConnectors(Graphics2D g, int camX, int camY) {
        int ts = tileSize();
        String mapKind = state.world.kind(state.currentMapId);
        boolean layeredRoads = "village".equals(mapKind) || "overworld".equals(mapKind);
        boolean texturedRoads = isTexturedRoadMap(mapKind) && !layeredRoads;
        for (int sy = 0; sy < context.visibleRows(); sy++) {
            for (int sx = 0; sx < context.visibleCols(); sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                // These roads already have a complete timber/earth surface in the terrain layer.
                if (!Terrain.connectingRoad(tile) || tile == Terrain.PLANK_ROAD || tile == Terrain.PACKED_ROAD) {
                    continue;
                }
                if (isSettlementRoadNode(tile)) {
                    continue;
                }
                int px = sx * ts;
                int py = sy * ts;
                if (tile == 'B') {
                    drawBridgeTile(g, wx, wy, px, py);
                    continue;
                }
                // Overworld and village roads share continuous materials in the ground compositor.
                if (layeredRoads) continue;
                if (drawsRoadOverlay(tile, mapKind)) {
                    drawTexturedRoadTile(g, wx, wy, px, py, tile, "village".equals(mapKind) || "city".equals(mapKind));
                    continue;
                }
                Color road = mapKind.equals("dungeon")
                        ? new Color(93, 82, 115)
                        : new Color(176, 130, 76);
                g.setColor(new Color(52, 38, 22, 150));
                drawRoadShape(g, px + 1, py + 1, wx, wy, scaled(18));
                g.setColor(road);
                drawRoadShape(g, px, py, wx, wy, scaled(16));
                g.setColor(new Color(232, 195, 124, 80));
                if (connectsRoad(wx - 1, wy) || connectsRoad(wx + 1, wy)) {
                    g.drawLine(px + scaled(4), py + ts / 2 - scaled(5), px + ts - scaled(4), py + ts / 2 - scaled(5));
                } else {
                    g.drawLine(px + ts / 2 - scaled(5), py + scaled(4), px + ts / 2 - scaled(5), py + ts - scaled(4));
                }
            }
        }
        if (texturedRoads) {
            drawTexturedRoadJunctionFillers(g, camX, camY, "village".equals(mapKind) || "city".equals(mapKind));
        }
    }

    private boolean isSettlementRoadNode(char tile) {
        return tile == 'c' || tile == 'u' || tile == 'd';
    }

    private void drawTexturedRoadTile(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        double campCoverage = "overworld".equals(state.world.kind(state.currentMapId))
                ? state.world.campGroundCoverage(wx + 0.5, wy + 0.5) : 0;
        if (campCoverage > 0.99) return;
        Graphics2D road = (Graphics2D) g.create();
        road.setComposite(AlphaComposite.SrcOver.derive((float) (1 - campCoverage)));
        drawTexturedRoadSurface(road, wx, wy, px, py, tile, settlementRoad, (float) (1 - campCoverage));
        road.dispose();
    }

    private void drawTexturedRoadSurface(Graphics2D g, int wx, int wy, int px, int py, char tile,
                                         boolean settlementRoad, float opacity) {
        int ts = tileSize();
        if (tile == Terrain.VILLAGE_ROAD || tile == Terrain.COBBLESTONE_ROAD) {
            drawRoadMaterialTexture(g, wx, wy, px, py, tile, settlementRoad);
        }
        int bits = roadBits(wx, wy);
        String suffix = "0" + Integer.toHexString(bits);
        String asset = "road_overlay_" + suffix.substring(suffix.length() - 2);
        float alpha = roadOverlayAlpha(tile) * opacity;
        Composite oldComposite = g.getComposite();
        if (alpha < 1f) {
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        }
        g.drawImage(assets.image(asset, ts, ts), px, py, null);
        g.setComposite(oldComposite);
    }

    private float roadOverlayAlpha(char tile) {
        return tile == Terrain.VILLAGE_ROAD || tile == Terrain.COBBLESTONE_ROAD ? 0.34f : 1.0f;
    }

    private void drawRoadMaterialTexture(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        BufferedImage material = assets.image(Terrain.assetName(tile), ts, ts);
        Graphics2D road = (Graphics2D) g.create();
        road.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        road.setPaint(new TexturePaint(material, new Rectangle(px, py, ts, ts)));
        drawRoadShape(road, px, py, wx, wy, tileRelative(settlementRoad ? 34 : 28, ts));
        road.dispose();
    }

    private void drawTexturedRoadJunctionFillers(Graphics2D g, int camX, int camY, boolean settlementRoad) {
        int ts = tileSize();
        int size = Math.max(6, tileRelative(settlementRoad ? 18 : 16, ts));
        BufferedImage filler = assets.image("road_junction_filler", size, size);
        for (int sy = 0; sy < context.visibleRows() - 1; sy++) {
            for (int sx = 0; sx < context.visibleCols() - 1; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                if ("overworld".equals(state.world.kind(state.currentMapId))
                        && state.world.campGroundCoverage(wx + 1.0, wy + 1.0) > 0.1) continue;
                if (!isRoadJunctionFill(wx, wy)) {
                    continue;
                }
                int px = (sx + 1) * ts - size / 2;
                int py = (sy + 1) * ts - size / 2;
                Composite oldComposite = g.getComposite();
                float alpha = roadJunctionFillerAlpha(wx, wy);
                if (alpha <= 0f) {
                    continue;
                }
                if (alpha < 1f) {
                    g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                }
                g.drawImage(filler, px, py, null);
                g.setComposite(oldComposite);
            }
        }
    }

    private float roadJunctionFillerAlpha(int wx, int wy) {
        char a = state.world.tileAt(state.currentMapId, wx, wy);
        char b = state.world.tileAt(state.currentMapId, wx + 1, wy);
        char c = state.world.tileAt(state.currentMapId, wx, wy + 1);
        char d = state.world.tileAt(state.currentMapId, wx + 1, wy + 1);
        return a == Terrain.VILLAGE_ROAD || b == Terrain.VILLAGE_ROAD || c == Terrain.VILLAGE_ROAD || d == Terrain.VILLAGE_ROAD
                || a == Terrain.COBBLESTONE_ROAD || b == Terrain.COBBLESTONE_ROAD || c == Terrain.COBBLESTONE_ROAD || d == Terrain.COBBLESTONE_ROAD
                ? 0.0f : 1.0f;
    }

    private boolean isRoadJunctionFill(int wx, int wy) {
        return isTexturedRoadTile(wx, wy)
                && isTexturedRoadTile(wx + 1, wy)
                && isTexturedRoadTile(wx, wy + 1)
                && isTexturedRoadTile(wx + 1, wy + 1);
    }

    private boolean isTexturedRoadTile(int wx, int wy) {
        char tile = state.world.tileAt(state.currentMapId, wx, wy);
        return Terrain.texturedRoad(tile);
    }

    private void drawContinuousRoadUnderlay(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        Graphics2D road = (Graphics2D) g.create();
        road.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color terrain = Terrain.color(roadUnderlayTile(tile, wx, wy));
        Color shoulderBase = roadShoulderColor(tile);
        Color coreBase = roadCoreColor(tile);
        drawRoadStroke(road, wx, wy, px, py,
                Math.min(ts + tileRelative(settlementRoad ? 22 : 16, ts), tileRelative(settlementRoad ? 56 : 50, ts)),
                roadBlendColor(terrain, shoulderBase, 0.28, settlementRoad ? 72 : 52));
        drawRoadStroke(road, wx, wy, px, py,
                Math.min(ts + tileRelative(settlementRoad ? 14 : 10, ts), tileRelative(settlementRoad ? 48 : 44, ts)),
                roadBlendColor(terrain, shoulderBase, 0.58, settlementRoad ? 110 : 82));
        drawRoadStroke(road, wx, wy, px, py,
                Math.min(ts + tileRelative(settlementRoad ? 6 : 2, ts), tileRelative(settlementRoad ? 42 : 38, ts)),
                roadWithAlpha(shoulderBase, settlementRoad ? 145 : 112));
        drawRoadStroke(road, wx, wy, px, py,
                Math.min(ts - tileRelative(settlementRoad ? 2 : 6, ts), tileRelative(settlementRoad ? 36 : 30, ts)),
                roadWithAlpha(coreBase, settlementRoad ? 178 : 142));
        road.dispose();
    }

    private Color roadShoulderColor(char tile) {
        return switch (tile) {
            case 'q' -> new Color(116, 112, 101);
            case 'K' -> new Color(124, 119, 109);
            case 'T' -> new Color(132, 105, 65);
            default -> new Color(156, 118, 70);
        };
    }

    private Color roadCoreColor(char tile) {
        return switch (tile) {
            case 'q' -> new Color(126, 120, 106);
            case 'K' -> new Color(143, 137, 124);
            case 'T' -> new Color(143, 111, 68);
            default -> new Color(166, 126, 75);
        };
    }

    private Color roadHighlightColor(char tile, int alpha) {
        Color color = switch (tile) {
            case 'q' -> new Color(190, 184, 166);
            case 'K' -> new Color(213, 205, 187);
            case 'T' -> new Color(205, 163, 96);
            default -> new Color(222, 181, 111);
        };
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(alpha, 0, 255));
    }

    private boolean isSoftRoadMaterial(char tile) {
        return tile == Terrain.DIRT_ROAD || tile == Terrain.VILLAGE_ROAD || tile == 'q';
    }

    private boolean isTexturedRoadMap(String mapKind) {
        return "village".equals(mapKind) || "overworld".equals(mapKind) || "city".equals(mapKind);
    }

    public boolean drawsRoadUnderlay(char tile, String mapKind) {
        return Terrain.texturedRoad(tile) && ("overworld".equals(mapKind) || "village".equals(mapKind));
    }

    private boolean drawsRoadOverlay(char tile, String mapKind) {
        return Terrain.texturedRoad(tile) && isTexturedRoadMap(mapKind);
    }

    private boolean usesRoadBaseTexture(char tile, String mapKind) {
        return Terrain.texturedRoad(tile) && !drawsRoadUnderlay(tile, mapKind);
    }

    private boolean blendsRoadEdgeWithUnderlay(char tile, String mapKind) {
        return isSoftRoadMaterial(tile) && ("overworld".equals(mapKind) || "village".equals(mapKind));
    }

    private Color roadBlendColor(Color first, Color second, double amount, int alpha) {
        double t = clamp(amount, 0.0, 1.0);
        int red = (int) Math.round(first.getRed() + (second.getRed() - first.getRed()) * t);
        int green = (int) Math.round(first.getGreen() + (second.getGreen() - first.getGreen()) * t);
        int blue = (int) Math.round(first.getBlue() + (second.getBlue() - first.getBlue()) * t);
        return new Color(clamp(red, 0, 255), clamp(green, 0, 255), clamp(blue, 0, 255), clamp(alpha, 0, 255));
    }

    private Color roadWithAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(alpha, 0, 255));
    }

    private void drawRoadStroke(Graphics2D g, int wx, int wy, int px, int py, int width, Color color) {
        int ts = tileSize();
        int cx = px + ts / 2;
        int cy = py + ts / 2;
        int bleed = Math.max(2, width / 3);
        boolean north = connectsRoad(wx, wy - 1);
        boolean east = connectsRoad(wx + 1, wy);
        boolean south = connectsRoad(wx, wy + 1);
        boolean west = connectsRoad(wx - 1, wy);
        Stroke oldStroke = g.getStroke();
        g.setColor(color);
        g.setStroke(new BasicStroke(Math.max(1, width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (!north && !east && !south && !west) {
            g.fillOval(cx - width / 2, cy - width / 2, width, width);
        }
        if (north) {
            g.drawLine(cx, cy, cx, py - bleed);
        }
        if (east) {
            g.drawLine(cx, cy, px + ts + bleed, cy);
        }
        if (south) {
            g.drawLine(cx, cy, cx, py + ts + bleed);
        }
        if (west) {
            g.drawLine(cx, cy, px - bleed, cy);
        }
        g.setStroke(oldStroke);
    }

    private void drawRoadJoinBlend(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        int blend = Math.max(2, tileRelative(settlementRoad ? 12 : 9, ts));
        Color color = roadWithAlpha(roadCoreColor(tile), settlementRoad ? 112 : 88);
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(color);
        if (connectsRoad(wx + 1, wy)) {
            g.fillRoundRect(px + ts - blend / 2, py + tileRelative(4, ts), blend, ts - tileRelative(8, ts), blend, blend);
        }
        if (connectsRoad(wx, wy + 1)) {
            g.fillRoundRect(px + tileRelative(4, ts), py + ts - blend / 2, ts - tileRelative(8, ts), blend, blend, blend);
        }
        if (connectsRoad(wx + 1, wy) && connectsRoad(wx, wy + 1)) {
            int r = tileRelative(settlementRoad ? 28 : 23, ts);
            g.fillOval(px + ts - r / 2, py + ts - r / 2, r, r);
        }
        if (connectsRoad(wx - 1, wy) && connectsRoad(wx, wy + 1)) {
            int r = tileRelative(settlementRoad ? 28 : 23, ts);
            g.fillOval(px - r / 2, py + ts - r / 2, r, r);
        }
        if (connectsRoad(wx + 1, wy) && connectsRoad(wx, wy - 1)) {
            int r = tileRelative(settlementRoad ? 28 : 23, ts);
            g.fillOval(px + ts - r / 2, py - r / 2, r, r);
        }
        if (connectsRoad(wx - 1, wy) && connectsRoad(wx, wy - 1)) {
            int r = tileRelative(settlementRoad ? 28 : 23, ts);
            g.fillOval(px - r / 2, py - r / 2, r, r);
        }
        g.setComposite(oldComposite);
    }

    private void drawRoadTextureUnifier(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        int width = Math.min(ts - tileRelative(settlementRoad ? 4 : 8, ts), tileRelative(settlementRoad ? 34 : 28, ts));
        Color color = roadWithAlpha(roadShoulderColor(tile), settlementRoad ? 72 : 52);
        Graphics2D road = (Graphics2D) g.create();
        road.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawRoadStroke(road, wx, wy, px, py, width, color);
        road.dispose();
    }

    private void drawRoadBodyTexture(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        int bits = roadBits(wx, wy);
        int half = tileRelative(settlementRoad ? 18 : 15, ts);
        int count = settlementRoad ? 22 : 16;
        int seed = Math.abs(wx * 928371 + wy * 364479 + tile * 97 + 11003);
        Graphics2D texture = (Graphics2D) g.create();
        texture.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < count; i++) {
            int local = Math.abs(seed + i * 73471 + (seed >> (i % 11)));
            int lx = Math.floorMod(local / 7, Math.max(1, ts));
            int ly = Math.floorMod(local / 31, Math.max(1, ts));
            if (!roadLocalPointInside(lx, ly, bits, half, ts)) {
                int[] adjusted = projectRoadTexturePoint(lx, ly, bits, half, ts);
                lx = adjusted[0];
                ly = adjusted[1];
            }
            int fleckW = Math.max(1, tileRelative(1 + Math.floorMod(local, 3), ts));
            int fleckH = Math.max(1, tileRelative(1 + Math.floorMod(local / 13, 2), ts));
            boolean light = Math.floorMod(local / 17, 4) == 0;
            Color color = roadTextureFleckColor(tile, light);
            texture.setColor(color);
            if (Math.floorMod(local / 23, 3) == 0) {
                texture.setStroke(new BasicStroke(Math.max(1f, scaledStroke(0.8f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                texture.drawLine(px + lx - fleckW, py + ly, px + lx + fleckW * 2, py + ly + Math.floorMod(local / 53, 3) - 1);
            } else {
                texture.fillOval(px + lx, py + ly, fleckW, fleckH);
            }
        }
        texture.dispose();
    }

    private Color roadTextureFleckColor(char tile, boolean light) {
        if (light) {
            return roadHighlightColor(tile, 70);
        }
        Color core = roadCoreColor(tile);
        return new Color(
                Math.max(0, core.getRed() - 70),
                Math.max(0, core.getGreen() - 62),
                Math.max(0, core.getBlue() - 48),
                tile == Terrain.COBBLESTONE_ROAD ? 44 : 56
        );
    }

    private boolean roadLocalPointInside(int lx, int ly, int bits, int half, int ts) {
        int cx = ts / 2;
        int cy = ts / 2;
        if (bits == 0) {
            int dx = lx - cx;
            int dy = ly - cy;
            return dx * dx + dy * dy <= half * half;
        }
        if (Math.abs(lx - cx) <= half && Math.abs(ly - cy) <= half) {
            return true;
        }
        if ((bits & 1) != 0 && Math.abs(lx - cx) <= half && ly <= cy) {
            return true;
        }
        if ((bits & 2) != 0 && Math.abs(lx - cx) <= half && ly >= cy) {
            return true;
        }
        if ((bits & 4) != 0 && Math.abs(ly - cy) <= half && lx <= cx) {
            return true;
        }
        return (bits & 8) != 0 && Math.abs(ly - cy) <= half && lx >= cx;
    }

    private int[] projectRoadTexturePoint(int lx, int ly, int bits, int half, int ts) {
        int cx = ts / 2;
        int cy = ts / 2;
        boolean horizontal = (bits & 12) != 0;
        boolean vertical = (bits & 3) != 0;
        if (horizontal && (!vertical || Math.abs(ly - cy) < Math.abs(lx - cx))) {
            return new int[]{clamp(lx, 0, ts - 1), clamp(ly, cy - half + 1, cy + half - 1)};
        }
        if (vertical) {
            return new int[]{clamp(lx, cx - half + 1, cx + half - 1), clamp(ly, 0, ts - 1)};
        }
        return new int[]{clamp(lx, cx - half + 1, cx + half - 1), clamp(ly, cy - half + 1, cy + half - 1)};
    }

    private void drawRoadEdgeFeather(Graphics2D g, int wx, int wy, int px, int py, char tile, boolean settlementRoad) {
        int ts = tileSize();
        int bits = roadBits(wx, wy);
        int seed = Math.abs(wx * 374761393 + wy * 668265263 + tile * 41 + 2707);
        Graphics2D edge = (Graphics2D) g.create();
        edge.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color underlay = Terrain.color(roadUnderlayTile(tile, wx, wy));
        Color dust = tile == 'q' ? new Color(136, 126, 101) : new Color(177, 130, 73);
        edge.setColor(roadBlendColor(underlay, dust, 0.55, settlementRoad ? 54 : 42));
        int marks = settlementRoad ? 14 : 10;
        int half = tileRelative(settlementRoad ? 21 : 18, ts);
        for (int i = 0; i < marks; i++) {
            int local = Math.abs(seed + i * 92821 + (seed >> (i % 7)));
            int arm = chooseRoadArm(bits, local);
            int along = Math.floorMod(local / 19, Math.max(1, ts));
            int side = Math.floorMod(local / 43, 2) == 0 ? -1 : 1;
            int jitter = tileRelative(Math.floorMod(local / 71, 7) - 3, ts);
            int lx;
            int ly;
            if (arm == 1 || arm == 2) {
                lx = ts / 2 + side * half + jitter;
                ly = arm == 1 ? along / 2 : ts / 2 + along / 2;
            } else {
                lx = arm == 4 ? along / 2 : ts / 2 + along / 2;
                ly = ts / 2 + side * half + jitter;
            }
            int w = tileRelative(5 + Math.floorMod(local, 7), ts);
            int h = Math.max(1, tileRelative(2 + Math.floorMod(local / 11, 3), ts));
            edge.fillOval(px + lx - w / 2, py + ly - h / 2, w, h);
        }
        edge.dispose();
    }

    private int chooseRoadArm(int bits, int seed) {
        if (bits == 0) {
            return 8;
        }
        int[] arms = {1, 2, 4, 8};
        int available = 0;
        for (int arm : arms) {
            if ((bits & arm) != 0) {
                available++;
            }
        }
        int selected = Math.floorMod(seed, Math.max(1, available));
        for (int arm : arms) {
            if ((bits & arm) == 0) {
                continue;
            }
            if (selected == 0) {
                return arm;
            }
            selected--;
        }
        return 8;
    }

    private void drawVillageRoadTile(Graphics2D g, int wx, int wy, int px, int py, char tile) {
        int ts = tileSize();
        Graphics2D road = (Graphics2D) g.create();
        road.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color shoulder = tile == 'q'
                ? new Color(118, 116, 106, 115)
                : new Color(157, 122, 72, 118);
        Color base = tile == 'q'
                ? new Color(120, 117, 105, 178)
                : new Color(164, 126, 75, 176);
        Color light = tile == 'q'
                ? new Color(190, 184, 166, 58)
                : new Color(222, 181, 111, 58);
        drawVillageRoadStroke(road, wx, wy, px, py, Math.min(ts + tileRelative(8, ts), tileRelative(44, ts)), shoulder);
        drawVillageRoadStroke(road, wx, wy, px, py, Math.min(ts - tileRelative(4, ts), tileRelative(34, ts)), base);
        drawVillageRoadStroke(road, wx, wy, px, py, Math.min(ts - tileRelative(18, ts), tileRelative(18, ts)), light);
        road.dispose();
        drawRoadEdgeFlecking(g, wx, wy, px, py);
    }

    private void drawVillageRoadStroke(Graphics2D g, int wx, int wy, int px, int py, int width, Color color) {
        int ts = tileSize();
        int cx = px + ts / 2;
        int cy = py + ts / 2;
        int bleed = Math.max(2, width / 3);
        Stroke oldStroke = g.getStroke();
        g.setColor(color);
        g.setStroke(new BasicStroke(Math.max(1, width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        boolean north = connectsRoad(wx, wy - 1);
        boolean east = connectsRoad(wx + 1, wy);
        boolean south = connectsRoad(wx, wy + 1);
        boolean west = connectsRoad(wx - 1, wy);
        if (!north && !east && !south && !west) {
            g.fillOval(cx - width / 2, cy - width / 2, width, width);
        }
        if (north) {
            g.drawLine(cx, cy, cx, py - bleed);
        }
        if (east) {
            g.drawLine(cx, cy, px + ts + bleed, cy);
        }
        if (south) {
            g.drawLine(cx, cy, cx, py + ts + bleed);
        }
        if (west) {
            g.drawLine(cx, cy, px - bleed, cy);
        }
        g.setStroke(oldStroke);
    }

    private int roadBits(int wx, int wy) {
        int bits = 0;
        if (connectsRoad(wx, wy - 1)) {
            bits |= 1;
        }
        if (connectsRoad(wx, wy + 1)) {
            bits |= 2;
        }
        if (connectsRoad(wx - 1, wy)) {
            bits |= 4;
        }
        if (connectsRoad(wx + 1, wy)) {
            bits |= 8;
        }
        return bits;
    }

    private void drawRoadShape(Graphics2D g, int px, int py, int wx, int wy, int half) {
        int c = tileSize() / 2;
        g.fillRect(px + c - half / 2, py + c - half / 2, half, half);
        if (connectsRoad(wx, wy - 1)) {
            g.fillRect(px + c - half / 2, py, half, c);
        }
        if (connectsRoad(wx + 1, wy)) {
            g.fillRect(px + c, py + c - half / 2, c, half);
        }
        if (connectsRoad(wx, wy + 1)) {
            g.fillRect(px + c - half / 2, py + c, half, c);
        }
        if (connectsRoad(wx - 1, wy)) {
            g.fillRect(px, py + c - half / 2, c, half);
        }
    }

    private boolean connectsRoad(int x, int y) {
        char tile = state.world.tileAt(state.currentMapId, x, y);
        return Terrain.connectingRoad(tile);
    }

    public char visibleTerrainTile(char tile, int wx, int wy) {
        String mapKind = state.world.kind(state.currentMapId);
        if (tile == 'B') {
            return 'w';
        }
        if (drawsRoadUnderlay(tile, mapKind)) {
            return roadUnderlayTile(tile, wx, wy);
        }
        if (WorldMap.OVERWORLD_ID.equals(state.currentMapId) && (tile == 'c' || tile == 'u')) {
            return settlementUnderlayTile(wx, wy);
        }
        return tile;
    }

    public char settlementUnderlayTile(int wx, int wy) {
        char best = 'g';
        int bestScore = -1;
        char[] candidates = {'g', 'f', 's', 'n', 'v', 'b', 'P', 'm', 'q'};
        for (char candidate : candidates) {
            int score = 0;
            for (int oy = -5; oy <= 5; oy++) {
                for (int ox = -5; ox <= 5; ox++) {
                    if (ox == 0 && oy == 0) {
                        continue;
                    }
                    char neighbor = state.world.tileAt(state.currentMapId, wx + ox, wy + oy);
                    if (neighbor != candidate) {
                        continue;
                    }
                    int distance = Math.max(Math.abs(ox), Math.abs(oy));
                    score += Math.max(1, 6 - distance);
                }
            }
            if (score > bestScore || (score == bestScore && terrainBlendPriority(candidate) > terrainBlendPriority(best))) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private int terrainBlendPriority(char tile) {
        return switch (tile) {
            case 'n' -> 8;
            case 's' -> 7;
            case 'P' -> 7;
            case 'v' -> 6;
            case 'b' -> 5;
            case 'f' -> 4;
            case 'm', 'q' -> 3;
            case 'g' -> 2;
            default -> 1;
        };
    }

    private char roadUnderlayTile(char tile, int wx, int wy) {
        if (tile == 'q') {
            return 'q';
        }
        char best = 'g';
        int bestScore = -1;
        char[] candidates = {'g', 'f', 's', 'n', 'v', 'b', 'P', 'm', 'q'};
        for (char candidate : candidates) {
            int score = 0;
            for (int oy = -2; oy <= 2; oy++) {
                for (int ox = -2; ox <= 2; ox++) {
                    if (ox == 0 && oy == 0) {
                        continue;
                    }
                    char neighbor = state.world.tileAt(state.currentMapId, wx + ox, wy + oy);
                    if (neighbor == candidate) {
                        score += Math.abs(ox) + Math.abs(oy) <= 1 ? 3 : 1;
                    }
                }
            }
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private void drawSoftRoadShoulder(Graphics2D g, int wx, int wy, int px, int py) {
        char underlay = roadUnderlayTile(state.world.tileAt(state.currentMapId, wx, wy), wx, wy);
        Color terrain = Terrain.color(underlay);
        Graphics2D roadG = (Graphics2D) g.create();
        roadG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        roadG.setColor(new Color(196, 153, 92, 132));
        drawRoadShape(roadG, px, py, wx, wy, scaled(30));
        roadG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
        roadG.setColor(terrain);
        drawRoadShape(roadG, px, py, wx, wy, scaled(36));
        roadG.dispose();
    }

    private void drawBridgeTile(Graphics2D g, int wx, int wy, int px, int py) {
        int ts = tileSize();
        int bits = roadBits(wx, wy);
        int bridgeBits = 0;
        if (isBridgeAt(wx, wy - 1)) bridgeBits |= 1;
        if (isBridgeAt(wx, wy + 1)) bridgeBits |= 2;
        if (isBridgeAt(wx - 1, wy)) bridgeBits |= 4;
        if (isBridgeAt(wx + 1, wy)) bridgeBits |= 8;
        // A road beside a bank must not turn a straight span into a T-junction.
        if (bridgeBits != 0 && (bridgeBits & 3) == 0) {
            bits = 12;
        } else if (bridgeBits != 0 && (bridgeBits & 12) == 0) {
            bits = 3;
        } else if (bits == 0) {
            bits = 3;
        }
        int half = tileRelative(15, ts);
        int rail = tileRelative(3, ts);
        java.awt.geom.Area deck = bridgeDeckShape(px, py, ts, half, bits);
        Graphics2D bridgeG = (Graphics2D) g.create();
        // Each tile owns exactly its pixels. Arms extend beyond the clip so their
        // outlines cannot leave transverse caps or repaint the neighboring tile.
        bridgeG.clipRect(px, py, ts, ts);
        bridgeG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        bridgeG.setColor(new Color(35, 29, 22, 95));
        bridgeG.setStroke(new BasicStroke(rail + tileRelative(4, ts), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        bridgeG.draw(deck);
        bridgeG.setColor(new Color(127, 91, 56));
        bridgeG.fill(deck);

        Graphics2D boards = (Graphics2D) bridgeG.create();
        boards.clip(deck);
        boolean vertical = (bits & 3) != 0;
        int plankCount = 7;
        int thin = tileRelative(1, ts);
        for (int i = 0; i < plankCount; i++) {
            int start = i * ts / plankCount;
            int end = (i + 1) * ts / plankCount;
            int grain = Math.floorMod((vertical ? wy : wx) * 17 + i * 13, 5);
            boards.setColor(new Color(145 + grain * 4, 107 + grain * 3, 66 + grain * 2));
            if (vertical) boards.fillRect(px, py + start, ts, end - start);
            else boards.fillRect(px + start, py, end - start, ts);
            boards.setColor(new Color(83, 58, 36));
            if (vertical) boards.fillRect(px, py + start, ts, thin);
            else boards.fillRect(px + start, py, thin, ts);
            boards.setColor(new Color(199, 153, 96, 145));
            if (vertical) boards.fillRect(px, py + start + thin, ts, thin);
            else boards.fillRect(px + start + thin, py, thin, ts);
            int c = ts / 2;
            int inset = half - tileRelative(5, ts);
            int middle = (start + end) / 2;
            boards.setColor(new Color(61, 49, 37, 170));
            if (vertical) {
                boards.fillRect(px + c - inset, py + middle, thin, thin);
                boards.fillRect(px + c + inset - thin, py + middle, thin, thin);
            } else {
                boards.fillRect(px + middle, py + c - inset, thin, thin);
                boards.fillRect(px + middle, py + c + inset - thin, thin, thin);
            }
        }
        boards.dispose();
        bridgeG.setColor(new Color(67, 47, 30));
        bridgeG.setStroke(new BasicStroke(rail, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        bridgeG.draw(deck);
        bridgeG.setColor(new Color(182, 139, 85));
        bridgeG.setStroke(new BasicStroke(thin, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        bridgeG.draw(deck);
        drawBridgePosts(bridgeG, px, py, ts, half, bits, bridgeBits, wx, wy);
        bridgeG.dispose();
    }

    private boolean isBridgeAt(int x, int y) {
        return state.world.tileAt(state.currentMapId, x, y) == 'B';
    }

    private java.awt.geom.Area bridgeDeckShape(int px, int py, int ts, int half, int bits) {
        int c = ts / 2;
        int extension = ts;
        java.awt.geom.Area shape = new java.awt.geom.Area(new Rectangle(px + c - half, py + c - half, half * 2, half * 2));
        if ((bits & 1) != 0) shape.add(new java.awt.geom.Area(new Rectangle(px + c - half, py - extension, half * 2, c + extension)));
        if ((bits & 2) != 0) shape.add(new java.awt.geom.Area(new Rectangle(px + c - half, py + c, half * 2, ts + extension - c)));
        if ((bits & 4) != 0) shape.add(new java.awt.geom.Area(new Rectangle(px - extension, py + c - half, c + extension, half * 2)));
        if ((bits & 8) != 0) shape.add(new java.awt.geom.Area(new Rectangle(px + c, py + c - half, ts + extension - c, half * 2)));
        return shape;
    }

    private void drawBridgePosts(Graphics2D g, int px, int py, int ts, int half, int bits,
                                 int bridgeBits, int wx, int wy) {
        int size = tileRelative(5, ts);
        int c = ts / 2;
        int bankInset = tileRelative(4, ts);
        if (bits == 3) {
            if ((bridgeBits & 1) == 0) bridgePostPair(g, px + c, py + bankInset, half, size, true);
            if ((bridgeBits & 2) == 0) bridgePostPair(g, px + c, py + ts - bankInset, half, size, true);
            if (bridgeBits == 3 && Math.floorMod(wy, 2) == 0) bridgePostPair(g, px + c, py + c, half, size, true);
        } else if (bits == 12) {
            if ((bridgeBits & 4) == 0) bridgePostPair(g, px + bankInset, py + c, half, size, false);
            if ((bridgeBits & 8) == 0) bridgePostPair(g, px + ts - bankInset, py + c, half, size, false);
            if (bridgeBits == 12 && Math.floorMod(wx, 2) == 0) bridgePostPair(g, px + c, py + c, half, size, false);
        }
    }

    private void bridgePostPair(Graphics2D g, int cx, int cy, int half, int size, boolean vertical) {
        for (int side : new int[]{-1, 1}) {
            int x = cx + (vertical ? side * half : 0) - size / 2;
            int y = cy + (vertical ? 0 : side * half) - size / 2;
            g.setColor(new Color(65, 45, 29));
            g.fillRect(x, y, size, size);
            g.setColor(new Color(192, 153, 98));
            int edge = Math.max(1, size / 4);
            g.fillRect(x + edge, y + edge, Math.max(1, size - edge * 2), Math.max(1, size - edge * 2));
        }
    }

    private void drawRoadEdgeFlecking(Graphics2D g, int wx, int wy, int px, int py) {
        int ts = tileSize();
        int seed = Math.abs(wx * 928371 + wy * 364479 + 31337);
        boolean north = connectsRoad(wx, wy - 1);
        boolean east = connectsRoad(wx + 1, wy);
        boolean south = connectsRoad(wx, wy + 1);
        boolean west = connectsRoad(wx - 1, wy);
        g.setColor(new Color(83, 63, 37, 48));
        for (int i = 0; i < 4; i++) {
            int offset = 5 + Math.floorMod(seed >> (i * 3), Math.max(1, ts - 10));
            int fleck = Math.max(1, tileRelative(1 + (seed + i) % 2, ts));
            if ((west || east) && !north) {
                g.fillOval(px + offset, py + tileRelative(5 + i % 2 * 3, ts), fleck, fleck);
            }
            if ((west || east) && !south) {
                g.fillOval(px + offset, py + ts - tileRelative(7 + i % 2 * 3, ts), fleck, fleck);
            }
            if ((north || south) && !west) {
                g.fillOval(px + tileRelative(5 + i % 2 * 3, ts), py + offset, fleck, fleck);
            }
            if ((north || south) && !east) {
                g.fillOval(px + ts - tileRelative(7 + i % 2 * 3, ts), py + offset, fleck, fleck);
            }
        }
    }

    public void drawMountainMassifOverlays(Graphics2D g, int camX, int camY) {
        if (!WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            return;
        }
        List<TilePoint> anchors = new ArrayList<>();
        for (int wy = camY - 4; wy < camY + context.visibleRows() + 4; wy++) {
            for (int wx = camX - 4; wx < camX + context.visibleCols() + 4; wx++) {
                if (isMountainMassifAnchor(wx, wy)) {
                    anchors.add(new TilePoint(wx, wy));
                }
            }
        }
        anchors.sort((a, b) -> {
            int byY = Integer.compare(a.y(), b.y());
            return byY != 0 ? byY : Integer.compare(a.x(), b.x());
        });
        int ts = tileSize();
        for (TilePoint anchor : anchors) {
            int seed = mountainAnchorHash(anchor.x(), anchor.y());
            int drawSize = scaled(92 + seed % 34);
            int jitterX = scaled(Math.floorMod(seed / 7, 15) - 7);
            int jitterY = scaled(Math.floorMod(seed / 17, 11) - 5);
            int px = (anchor.x() - camX) * ts + ts / 2 - drawSize / 2 + jitterX;
            int py = (anchor.y() - camY) * ts + ts - drawSize + scaled(8) + jitterY;
            effects.drawShadow(g, px + drawSize / 5, py + drawSize - scaled(15), drawSize * 3 / 5, scaled(12));
            g.drawImage(assets.spriteFit("mountain_massif", drawSize, drawSize), px, py, null);
        }
    }

    private boolean isMountainMassifAnchor(int wx, int wy) {
        if (state.world.tileAt(state.currentMapId, wx, wy) != 'm') {
            return false;
        }
        int mountainCount = 0;
        int closeMountainCount = 0;
        for (int oy = -2; oy <= 2; oy++) {
            for (int ox = -2; ox <= 2; ox++) {
                char tile = state.world.tileAt(state.currentMapId, wx + ox, wy + oy);
                if (tile == 'm') {
                    mountainCount++;
                    if (Math.max(Math.abs(ox), Math.abs(oy)) <= 1) {
                        closeMountainCount++;
                    }
                }
                if (Math.abs(ox) + Math.abs(oy) <= 1 && tile == 'q') {
                    return false;
                }
            }
        }
        if (closeMountainCount < 4 || mountainCount < 8) {
            return false;
        }
        int chance = Math.min(82, 28 + mountainCount * 5 + closeMountainCount * 3);
        int hash = mountainAnchorHash(wx, wy);
        return Math.floorMod(hash, 100) < chance;
    }

    private int mountainAnchorHash(int wx, int wy) {
        return Math.abs(wx * 374761393 + wy * 668265263 + 6203);
    }


    public record RenderContext(int tileSize, int visibleCols, int visibleRows, int frame) {
    }

    public interface Effects {
        WeatherQuality weatherQuality();

        String terrainImageName(char tile, int wx, int wy);

        public void drawShadow(Graphics2D g, int x, int y, int w, int h);
    }
}
