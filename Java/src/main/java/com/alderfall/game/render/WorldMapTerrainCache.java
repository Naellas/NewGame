package com.alderfall.game.render;

import com.alderfall.game.map.WorldMap;
import com.alderfall.game.Terrain;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
        int[][] colors = new int[WorldMap.ROWS][WorldMap.COLS];
        for (int wy = Math.max(0, (int)worldY); wy < Math.min(WorldMap.ROWS, Math.ceil(worldY+worldH)); wy++) {
            for (int wx = Math.max(0, (int)worldX); wx < Math.min(WorldMap.COLS, Math.ceil(worldX+worldW)); wx++) {
                Color color = atlasColor(world, world.tileAt(WorldMap.OVERWORLD_ID, wx, wy), wx, wy);
                if (kingdoms) color = kingdomMapColor(color, world.kingdomAt(wx, wy));
                colors[wy][wx] = color.getRGB();
            }
        }
        for (int py = 0; py < height; py++) {
            int wy = clamp((int) Math.floor(worldY + (py + 0.5) * worldH / height), 0, WorldMap.ROWS - 1);
            for (int px = 0; px < width; px++) {
                int wx = clamp((int) Math.floor(worldX + (px + 0.5) * worldW / width), 0, WorldMap.COLS - 1);
                image.setRGB(px, py, colors[wy][wx]);
            }
        }
        drawTerrainSymbols(image, world, worldX, worldY, worldW, worldH);
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

    public static Color terrainColor(char tile) {
        return switch (tile) {
            case 'w', '~' -> new Color(48, 103, 128);
            case 'g' -> new Color(129, 155, 99);
            case 'f' -> new Color(65, 105, 79);
            case 'm' -> new Color(135, 139, 133);
            case 's' -> new Color(194, 165, 109);
            case 'n' -> new Color(212, 224, 217);
            case 'v' -> new Color(89, 129, 116);
            case 'b' -> new Color(159, 124, 96);
            case 'P' -> new Color(215, 196, 140);
            default -> Terrain.color(tile);
        };
    }

    private Color atlasColor(WorldMap world, char tile, int x, int y) {
        Color base = terrainColor(tile);
        int shade = Math.floorMod(x * 31 + y * 73 + x * y * 7, 9) - 4;
        if (tile == 'w' || tile == '~') {
            boolean coast = false;
            for (int[] d : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
                char near = world.tileAt(WorldMap.OVERWORLD_ID, clamp(x+d[0],0,WorldMap.COLS-1), clamp(y+d[1],0,WorldMap.ROWS-1));
                if (near != 'w' && near != '~') coast = true;
            }
            shade = coast ? 24 : shade;
        } else if (Terrain.CONNECTING_ROAD.contains(tile)) {
            return new Color(221, 196, 145);
        } else if (tile == 'm') {
            shade += (int)(12 * Math.sin(x * .65 + y * .4));
        }
        return new Color(clamp(base.getRed()+shade,0,255), clamp(base.getGreen()+shade,0,255), clamp(base.getBlue()+shade,0,255));
    }

    /** Small cartographic symbols stay anchored to terrain when panning and zooming. */
    private void drawTerrainSymbols(BufferedImage image, WorldMap world, double x, double y, double w, double h) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double sx = image.getWidth() / w, sy = image.getHeight() / h;
        int stride = Math.min(sx, sy) < 3 ? 6 : 4;
        for (int wy = Math.max(0, (int)y / stride * stride); wy < Math.min(WorldMap.ROWS, y+h); wy += stride) {
            for (int wx = Math.max(0, (int)x / stride * stride); wx < Math.min(WorldMap.COLS, x+w); wx += stride) {
                char tile = world.tileAt(WorldMap.OVERWORLD_ID, wx, wy);
                int px = (int)Math.round((wx+.5-x)*sx), py = (int)Math.round((wy+.5-y)*sy);
                int r = (int)Math.max(2, Math.min(9, sx * 1.1));
                if (tile == 'm') {
                    g.setColor(new Color(55, 65, 65, 130));
                    g.fillPolygon(new int[]{px-r,px,px+r}, new int[]{py+r,py-r,py+r},3);
                    g.setColor(new Color(236, 233, 207, 170));
                    g.drawLine(px-r,py+r,px,py-r);
                    g.drawLine(px,py-r,px+2,py);
                } else if (tile == 'f') {
                    g.setColor(new Color(26, 66, 49, 125));
                    g.fillPolygon(new int[]{px-r,px,px+r}, new int[]{py+2,py-r-2,py+2},3);
                    g.drawLine(px,py+2,px,py+5);
                    g.setColor(new Color(175, 190, 129, 80));
                    g.drawLine(px-r,py+2,px,py-r-2);
                } else if (tile == 'w' && Math.floorMod(wx + wy, 3) == 0) {
                    g.setColor(new Color(166, 208, 210, 55));
                    g.drawArc(px-r,py,r*2,3,180,180);
                } else if (tile == 's' || tile == 'b') {
                    g.setColor(new Color(103, 79, 51, 55));
                    g.drawArc(px-r,py,r*2,r,10,160);
                } else if (tile == 'v') {
                    g.setColor(new Color(36, 81, 72, 100));
                    g.drawLine(px-3,py,px+3,py);
                    g.drawLine(px,py,px-1,py-4);
                }
            }
        }
        g.dispose();
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
