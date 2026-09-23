package com.alderfall.game.render;

import com.alderfall.game.map.WorldMap;
import com.alderfall.game.Terrain;
import java.awt.Color;
import java.awt.image.BufferedImage;

public final class WorldMapTerrainCache {
    private BufferedImage terrainCache;
    private BufferedImage kingdomCache;
    private double terrainWorldX;
    private double terrainWorldY;
    private double terrainWorldW;
    private double terrainWorldH;
    private double kingdomWorldX;
    private double kingdomWorldY;
    private double kingdomWorldW;
    private double kingdomWorldH;
    private long terrainRevision = Long.MIN_VALUE;
    private long kingdomRevision = Long.MIN_VALUE;

    public BufferedImage image(WorldMap world, int width, int height, boolean kingdoms) {
        return image(world, width, height, kingdoms, 0.0, 0.0, WorldMap.COLS, WorldMap.ROWS);
    }

    public BufferedImage image(WorldMap world, int width, int height, boolean kingdoms,
                               double worldX, double worldY, double worldW, double worldH) {
        BufferedImage cached = kingdoms ? kingdomCache : terrainCache;
        long revision = world.visualRevision(WorldMap.OVERWORLD_ID);
        long cachedRevision = kingdoms ? kingdomRevision : terrainRevision;
        if (cached != null && cached.getWidth() == width && cached.getHeight() == height
                && cachedRevision == revision
                && sameViewport(kingdoms, worldX, worldY, worldW, worldH)) {
            return cached;
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int py = 0; py < height; py++) {
            int wy = clamp((int) Math.floor(worldY + (py + 0.5) * worldH / height), 0, WorldMap.ROWS - 1);
            for (int px = 0; px < width; px++) {
                int wx = clamp((int) Math.floor(worldX + (px + 0.5) * worldW / width), 0, WorldMap.COLS - 1);
                Color color = Terrain.color(world.tileAt(WorldMap.OVERWORLD_ID, wx, wy));
                if (kingdoms) {
                    color = kingdomMapColor(color, world.kingdomAt(wx, wy));
                }
                image.setRGB(px, py, color.getRGB());
            }
        }
        if (kingdoms) {
            kingdomCache = image;
            kingdomWorldX = worldX;
            kingdomWorldY = worldY;
            kingdomWorldW = worldW;
            kingdomWorldH = worldH;
            kingdomRevision = revision;
        } else {
            terrainCache = image;
            terrainWorldX = worldX;
            terrainWorldY = worldY;
            terrainWorldW = worldW;
            terrainWorldH = worldH;
            terrainRevision = revision;
        }
        return image;
    }

    public void clear() {
        terrainCache = null;
        kingdomCache = null;
        terrainRevision = Long.MIN_VALUE;
        kingdomRevision = Long.MIN_VALUE;
    }

    private boolean sameViewport(boolean kingdoms, double worldX, double worldY, double worldW, double worldH) {
        double cachedX = kingdoms ? kingdomWorldX : terrainWorldX;
        double cachedY = kingdoms ? kingdomWorldY : terrainWorldY;
        double cachedW = kingdoms ? kingdomWorldW : terrainWorldW;
        double cachedH = kingdoms ? kingdomWorldH : terrainWorldH;
        return Math.abs(cachedX - worldX) < 0.001
                && Math.abs(cachedY - worldY) < 0.001
                && Math.abs(cachedW - worldW) < 0.001
                && Math.abs(cachedH - worldH) < 0.001;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Color kingdomMapColor(Color terrain, WorldMap.Kingdom kingdom) {
        Color kingdomColor = new Color(kingdom.colorRgb());
        double blend = 0.42;
        int r = (int) Math.round(terrain.getRed() * (1.0 - blend) + kingdomColor.getRed() * blend);
        int g = (int) Math.round(terrain.getGreen() * (1.0 - blend) + kingdomColor.getGreen() * blend);
        int b = (int) Math.round(terrain.getBlue() * (1.0 - blend) + kingdomColor.getBlue() * blend);
        return new Color(r, g, b);
    }
}
