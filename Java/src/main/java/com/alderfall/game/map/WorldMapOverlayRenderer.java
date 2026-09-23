package com.alderfall.game.map;

import com.alderfall.game.Quest;
import com.alderfall.game.Terrain;
import com.alderfall.game.TilePoint;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.List;

public final class WorldMapOverlayRenderer {
    private WorldMapOverlayRenderer() {
    }

    public static int screenX(WorldMapViewport viewport, double wx) {
        return viewport.screenX() + (int) Math.round((wx - viewport.worldX()) * viewport.screenW() / viewport.worldW());
    }

    public static int screenY(WorldMapViewport viewport, double wy) {
        return viewport.screenY() + (int) Math.round((wy - viewport.worldY()) * viewport.screenH() / viewport.worldH());
    }

    public static double worldX(WorldMapViewport viewport, int screenX) {
        return viewport.worldX() + (screenX - viewport.screenX()) * viewport.worldW() / viewport.screenW();
    }

    public static double worldY(WorldMapViewport viewport, int screenY) {
        return viewport.worldY() + (screenY - viewport.screenY()) * viewport.worldH() / viewport.screenH();
    }

    public static boolean visible(WorldMapViewport viewport, int wx, int wy, double margin) {
        return wx >= viewport.worldX() - margin
                && wy >= viewport.worldY() - margin
                && wx <= viewport.worldX() + viewport.worldW() + margin
                && wy <= viewport.worldY() + viewport.worldH() + margin;
    }

    public static void drawGrid(Graphics2D g, WorldMapViewport viewport) {
        double tileW = viewport.screenW() / viewport.worldW();
        double tileH = viewport.screenH() / viewport.worldH();
        double minTile = Math.min(tileW, tileH);
        if (minTile < 5.0) {
            return;
        }
        int step = minTile >= 18.0 ? 1 : minTile >= 10.0 ? 5 : 10;
        g.setColor(new Color(255, 255, 255, minTile >= 18.0 ? 42 : 30));
        for (int wx = Math.max(0, (int) Math.ceil(viewport.worldX() / step) * step);
             wx <= Math.min(WorldMap.COLS, (int) Math.ceil(viewport.worldX() + viewport.worldW())); wx += step) {
            int sx = screenX(viewport, wx);
            g.drawLine(sx, viewport.screenY(), sx, viewport.screenY() + viewport.screenH());
        }
        for (int wy = Math.max(0, (int) Math.ceil(viewport.worldY() / step) * step);
             wy <= Math.min(WorldMap.ROWS, (int) Math.ceil(viewport.worldY() + viewport.worldH())); wy += step) {
            int sy = screenY(viewport, wy);
            g.drawLine(viewport.screenX(), sy, viewport.screenX() + viewport.screenW(), sy);
        }
    }

    public static void drawKingdomLabels(Graphics2D g, WorldMapViewport viewport, List<WorldMap.Kingdom> kingdoms, WorldMapLabels labels) {
        for (WorldMap.Kingdom kingdom : kingdoms) {
            if (!visible(viewport, kingdom.centerX(), kingdom.centerY(), 12.0)) {
                continue;
            }
            int px = screenX(viewport, kingdom.centerX() + 0.5);
            int py = screenY(viewport, kingdom.centerY() + 0.5);
            labels.add(kingdom.name(), px, py, new Color(245, 246, 236), 0);
        }
    }

    public static String shortSettlementLabel(String label) {
        return label.replace(" City", "").replace(" Town", "").replace(" Village", "");
    }

    public static Rectangle drawSettlementMarker(Graphics2D g, WorldMapViewport viewport,
                                                 WorldMap.SettlementSite settlement, WorldMap.Kingdom kingdom,
                                                 boolean playerSettlement, WorldMapLabels labels, boolean showLabel) {
        int px = screenX(viewport, settlement.x() + 0.5);
        int py = screenY(viewport, settlement.y() + 0.5);
        Color color = playerSettlement ? new Color(255, 226, 128)
                : kingdom == null ? new Color(245, 214, 117) : new Color(kingdom.colorRgb());
        g.setColor(new Color(8, 10, 16, 166));
        g.fillOval(px - 7, py - 7, 15, 15);
        g.setColor(color);
        if (playerSettlement) {
            g.fillOval(px - 5, py - 5, 11, 11);
        } else {
            g.fillRect(px - 4, py - 4, 9, 9);
        }
        g.setColor(new Color(245, 246, 236, 220));
        if (playerSettlement) {
            g.drawOval(px - 6, py - 6, 13, 13);
        } else {
            g.drawRect(px - 5, py - 5, 11, 11);
        }
        Rectangle bounds = new Rectangle(px - 8, py - 8, 17, 17);
        labels.reserve(bounds);
        if (showLabel) labels.add(shortSettlementLabel(settlement.label()), px, py, color, playerSettlement ? 70 : 50);
        return bounds;
    }

    public static void drawMapMarker(Graphics2D g, WorldMapViewport viewport, int wx, int wy,
                                     String label, int labelDx, int labelDy) {
        int px = screenX(viewport, wx + 0.5);
        int py = screenY(viewport, wy + 0.5);
        g.setColor(new Color(245, 214, 117));
        g.fillRect(px - 3, py - 3, 7, 7);
        drawMapLabel(g, label, px + labelDx, py + labelDy, new Color(245, 214, 117));
    }

    /** Keep every site icon visible; omit crowded text until zoom/hover makes room. */
    public static Rectangle drawCampaignMarker(Graphics2D g, WorldMapViewport viewport,
                                                WorldMap.CampaignMarker site, WorldMapLabels labels, boolean showLabel) {
        int px = screenX(viewport, site.x() + 0.5);
        int py = screenY(viewport, site.y() + 0.5);
        g.setColor(new Color(10, 15, 20));
        g.fillOval(px - 6, py - 6, 13, 13);
        g.setColor(new Color(245, 214, 117));
        if (site.kind().contains("camp")) {
            g.drawPolygon(new int[]{px - 4, px, px + 4}, new int[]{py + 3, py - 4, py + 3}, 3);
        } else {
            g.drawRect(px - 3, py - 3, 6, 6);
        }
        Rectangle bounds = new Rectangle(px - 7, py - 7, 15, 15);
        labels.reserve(bounds);
        if (showLabel) labels.add(site.label(), px, py, new Color(245, 214, 117), 20);
        return bounds;
    }

    public static Rectangle drawQuestMarker(Graphics2D g, WorldMapViewport viewport, Quest.ObjectiveKind kind,
                                       String title, TilePoint marker, Color color,
                                       boolean mainStory, boolean dungeonHazard, WorldMapLabels labels, boolean showLabel, boolean focused) {
        int px = screenX(viewport, marker.x() + 0.5);
        int py = screenY(viewport, marker.y() + 0.5);
        if (focused) {
            g.setColor(new Color(255, 223, 118, 75));
            g.fillOval(px - 17, py - 17, 35, 35);
            g.setColor(new Color(255, 223, 118));
            g.drawOval(px - 14, py - 14, 29, 29);
        }
        if (dungeonHazard) {
            drawSkull(g, px, py - 18);
        }
        g.setColor(new Color(0, 0, 0, 155));
        g.fillOval(px - 8, py - 9, 17, 17);
        g.setColor(color);
        g.fillOval(px - 6, py - 7, 13, 13);
        Polygon tail = new Polygon(new int[]{px - 4, px + 4, px}, new int[]{py + 2, py + 2, py + 10}, 3);
        g.fillPolygon(tail);
        g.setColor(mainStory ? new Color(255, 212, 82) : new Color(245, 246, 236));
        g.drawOval(px - 7, py - 8, 15, 15);
        g.drawPolygon(tail);
        g.setColor(new Color(18, 20, 24));
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        String glyph = switch (kind) {
            case GATHER -> "G";
            case DELIVER -> "D";
            case VISIT -> "V";
            case SEARCH -> "S";
            case TALK -> "T";
            case ASK_AROUND -> "?";
            case REPORT -> "R";
            case ESCORT -> "E";
            case RESCUE -> "!";
            case DEFEND -> "P";
            case RAID_DEFENSE -> "R";
            case CHOICE -> "C";
            case DEFEAT -> "B";
        };
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(glyph, px - metrics.stringWidth(glyph) / 2, py + 3);
        Rectangle bounds = new Rectangle(px - 9, py - (dungeonHazard ? 26 : 10), 19, dungeonHazard ? 38 : 22);
        labels.reserve(bounds);
        if (showLabel) labels.add(title, px, py, new Color(245, 246, 236), focused ? 120 : mainStory ? 100 : 90);
        return bounds;
    }

    private static void drawSkull(Graphics2D g, int cx, int cy) {
        g.setColor(new Color(0, 0, 0, 155));
        g.fillOval(cx - 7, cy - 7, 14, 14);
        g.setColor(new Color(246, 247, 232, 235));
        g.fillOval(cx - 6, cy - 6, 12, 12);
        g.fillRect(cx - 4, cy + 1, 8, 5);
        g.setColor(new Color(28, 24, 26, 230));
        g.fillOval(cx - 4, cy - 2, 3, 3);
        g.fillOval(cx + 1, cy - 2, 3, 3);
        g.drawLine(cx - 3, cy + 5, cx + 3, cy + 5);
    }

    public static void drawCompactLegend(Graphics2D g, int x, int y, int width, int height) {
        if (height < 70) return;
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Legend", x, y);
        drawLegendQuest(g, x, y + 26, new Color(112, 220, 128), "Provision");
        drawLegendQuest(g, x + width / 2, y + 26, new Color(203, 157, 232), "Talk");
        drawLegendQuest(g, x, y + 50, new Color(233, 89, 83), "Battle");
        drawLegendPlayer(g, x + width / 2, y + 50, "You");
        if (height < 190) return;
        char[] tiles = {'g', 'f', 's', 'n', 'v', 'b', 'P', 'm', 'w'};
        String[] names = {"Meadow", "Oldwood", "Sunsteppe", "Frostfield", "Marsh", "Badlands", "Beach", "Mountain", "Water"};
        for (int i = 0; i < tiles.length; i++) {
            drawLegendSwatch(g, x + (i % 2) * width / 2, y + 82 + (i / 2) * 23,
                    width / 2, com.alderfall.game.render.WorldMapTerrainCache.terrainColor(tiles[i]), names[i]);
        }
    }

    public static void drawLegend(Graphics2D g, int x, int y, int width,
                                  boolean showKingdoms, List<WorldMap.Kingdom> kingdoms) {
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Legend", x, y);
        int rowY = y + 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('g'), "Meadow");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('f'), "Oldwood");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('s'), "Sunsteppe");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('n'), "Frostfield");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('v'), "Marsh");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('b'), "Badlands");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('P'), "Beach");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('m'), "Mountain");
        rowY += 24;
        drawLegendSwatch(g, x, rowY, width, Terrain.color('w'), "Water");
        rowY += 34;
        drawLegendSettlement(g, x, rowY, "Settlement");
        rowY += 26;
        drawLegendSettlement(g, x, rowY, "Named site (hover)");
        rowY += 26;
        drawLegendQuest(g, x, rowY, new Color(112, 220, 128), "Provision");
        rowY += 26;
        drawLegendQuest(g, x, rowY, new Color(203, 157, 232), "Talk");
        rowY += 26;
        drawLegendQuest(g, x, rowY, new Color(233, 89, 83), "Battle");
        rowY += 26;
        drawLegendQuest(g, x, rowY, new Color(220, 50, 47), "Story");
        rowY += 26;
        drawLegendQuest(g, x, rowY, new Color(233, 89, 83), "Dungeon");
        rowY += 26;
        drawLegendPlayer(g, x, rowY, "You");
        if (showKingdoms) {
            rowY += 34;
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.setColor(new Color(244, 239, 220));
            g.drawString("Kingdoms", x, rowY);
            rowY += 24;
            for (WorldMap.Kingdom kingdom : kingdoms) {
                drawLegendSwatch(g, x, rowY, width, new Color(kingdom.colorRgb()), kingdom.name());
                rowY += 22;
            }
        }
    }

    private static void drawMapLabel(Graphics2D g, String label, int x, int y, Color color) {
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics metrics = g.getFontMetrics();
        int textW = metrics.stringWidth(label);
        int textH = metrics.getHeight();
        g.setColor(new Color(8, 10, 16, 188));
        g.fillRoundRect(x - 4, y - textH + 4, textW + 8, textH + 2, 6, 6);
        g.setColor(color);
        g.drawString(label, x, y);
    }

    private static void drawLegendSwatch(Graphics2D g, int x, int y, int width, Color color, String label) {
        g.setColor(color);
        g.fillRect(x, y - 12, 16, 16);
        g.setColor(new Color(18, 23, 32));
        g.drawRect(x, y - 12, 16, 16);
        drawLegendText(g, x + 24, y + 1, width - 24, label);
    }

    private static void drawLegendSettlement(Graphics2D g, int x, int y, String label) {
        g.setColor(new Color(245, 214, 117));
        g.fillRect(x + 5, y - 9, 8, 8);
        drawLegendText(g, x + 24, y, 92, label);
    }

    private static void drawLegendQuest(Graphics2D g, int x, int y, Color color, String label) {
        g.setColor(new Color(0, 0, 0, 155));
        g.fillOval(x + 2, y - 16, 16, 16);
        g.setColor(color);
        g.fillOval(x + 4, y - 14, 12, 12);
        g.fillPolygon(new Polygon(new int[]{x + 7, x + 13, x + 10}, new int[]{y - 5, y - 5, y + 1}, 3));
        g.setColor(new Color(245, 246, 236));
        g.drawOval(x + 3, y - 15, 14, 14);
        drawLegendText(g, x + 24, y, 92, label);
    }

    private static void drawLegendPlayer(Graphics2D g, int x, int y, String label) {
        g.setColor(new Color(255, 245, 174));
        g.fillOval(x + 4, y - 12, 11, 11);
        g.setColor(Color.BLACK);
        g.drawOval(x + 4, y - 12, 11, 11);
        drawLegendText(g, x + 24, y, 92, label);
    }

    private static void drawLegendText(Graphics2D g, int x, int y, int width, String label) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(210, 213, 222));
        FontMetrics metrics = g.getFontMetrics();
        String display = label;
        while (metrics.stringWidth(display) > width && display.length() > 4) {
            display = display.substring(0, display.length() - 4) + "...";
        }
        g.drawString(display, x, y);
    }
}
