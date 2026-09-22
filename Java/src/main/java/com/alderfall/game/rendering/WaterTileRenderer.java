package com.alderfall.game;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WaterTileRenderer {
    private static final int BASE_TILE_SIZE = 48;
    private static final int VARIANTS = 24;
    private static final int MAX_CACHE_IMAGES = 640;

    private final Map<String, BufferedImage> cache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return size() > MAX_CACHE_IMAGES;
        }
    };

    public void draw(Graphics2D g, int wx, int wy, int px, int py, int tileSize, int frame,
              String mapId, WeatherSystem weather, WeatherQuality quality) {
        int variant = Math.floorMod(wx * 928371 + wy * 364479 + mapId.hashCode(), VARIANTS);
        int phaseCount = phaseCount(quality);
        int phase = Math.floorMod(frame / frameDivisor(quality), phaseCount);
        int waveBucket = Math.max(0, Math.min(12, (int) Math.round(weather.waterWave() * 12.0)));
        int windBucket = Math.max(-6, Math.min(6, (int) Math.round(weather.windX() * 6.0)));
        String key = tileSize + ":" + quality.key + ":" + variant + ":" + phase + ":" + waveBucket + ":" + windBucket;
        BufferedImage image = cache.computeIfAbsent(key,
                ignored -> createTile(tileSize, quality, variant, phase, phaseCount, waveBucket / 12.0, windBucket / 6.0));
        g.drawImage(image, px, py, null);
    }

    private BufferedImage createTile(int tileSize, WeatherQuality quality, int variant, int phase,
                                     int phaseCount, double wave, double windX) {
        BufferedImage image = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        double t = phase / (double) Math.max(1, phaseCount);
        int seed = Math.abs(variant * 80141 + 19391);
        drawDepthPulse(g, tileSize, seed, t, wave, windX);
        drawCurrentLines(g, tileSize, seed, t, wave, windX, quality);
        drawCrossRipples(g, tileSize, seed, t, wave, quality);
        drawGlints(g, tileSize, seed, t, wave, quality);
        g.dispose();
        return image;
    }

    private void drawDepthPulse(Graphics2D g, int tileSize, int seed, double t, double wave, double windX) {
        float alpha = (float) (0.055 + wave * 0.08);
        int offsetX = rel(tileSize, (int) Math.round(windX * wave * 4.0));
        int offsetY = rel(tileSize, (int) Math.round(Math.sin(t * Math.PI * 2.0 + seed * 0.01) * 2.0));
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g.setPaint(new GradientPaint(0, rel(tileSize, 8), new Color(43, 105, 154),
                tileSize, tileSize, new Color(98, 168, 188)));
        g.fillOval(rel(tileSize, 3) + offsetX, rel(tileSize, 5) + offsetY,
                tileSize - rel(tileSize, 6), tileSize - rel(tileSize, 10));
        g.setComposite(oldComposite);
    }

    private void drawCurrentLines(Graphics2D g, int tileSize, int seed, double t, double wave, double windX,
                                  WeatherQuality quality) {
        int count = switch (quality) {
            case HIGH -> 9;
            case BALANCED -> 7;
            case PERFORMANCE -> 5;
            case LOW_SPEC -> 2;
        };
        for (int i = 0; i < count; i++) {
            int localSeed = Math.abs(seed + i * 13757);
            double phase = t * Math.PI * 2.0 + i * 0.87 + seed * 0.005;
            int yBase = Math.floorMod((int) Math.round((i * tileSize / (double) count)
                    + t * tileSize * (0.25 + wave * 0.35) + localSeed / 29.0), tileSize + rel(tileSize, 10)) - rel(tileSize, 5);
            int x = rel(tileSize, -8) + Math.floorMod(localSeed / 11 + (int) Math.round(windX * t * tileSize), Math.max(1, rel(tileSize, 17))) - rel(tileSize, 8);
            int length = rel(tileSize, 14 + Math.floorMod(localSeed / 41, 24)) + (int) Math.round(wave * rel(tileSize, 16));
            int amplitude = Math.max(1, rel(tileSize, 1 + (int) Math.round(wave * 2.0)));
            int alpha = 38 + (int) Math.round(wave * 78.0) + Math.floorMod(localSeed / 53, 36);
            g.setComposite(AlphaComposite.SrcOver.derive(Math.min(0.58f, alpha / 255.0f)));
            g.setColor(new Color(183, 232, 244));
            g.setStroke(new BasicStroke(Math.max(1f, relStroke(tileSize, 0.70f + (float) wave * 0.72f)),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            drawWaveStroke(g, x, yBase, length, amplitude, phase);
        }
    }

    private void drawCrossRipples(Graphics2D g, int tileSize, int seed, double t, double wave, WeatherQuality quality) {
        int count = switch (quality) {
            case HIGH, BALANCED -> 3;
            case PERFORMANCE -> 2;
            case LOW_SPEC -> 0;
        };
        g.setStroke(new BasicStroke(Math.max(1f, relStroke(tileSize, 0.55f)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < count; i++) {
            int localSeed = Math.abs(seed + i * 42073);
            int x = Math.floorMod(localSeed / 17 + (int) Math.round(t * tileSize * 0.65), tileSize + rel(tileSize, 16)) - rel(tileSize, 8);
            int y = Math.floorMod(localSeed / 37 + (int) Math.round(Math.sin(t * Math.PI * 2.0 + i) * rel(tileSize, 4)), tileSize);
            int w = rel(tileSize, 12 + Math.floorMod(localSeed / 71, 16));
            int h = Math.max(1, rel(tileSize, 3 + Math.floorMod(localSeed / 97, 4)));
            g.setComposite(AlphaComposite.SrcOver.derive((float) (0.06 + wave * 0.08)));
            g.setColor(new Color(222, 248, 250));
            g.drawArc(x, y, w, h, 8, 156);
        }
    }

    private void drawGlints(Graphics2D g, int tileSize, int seed, double t, double wave, WeatherQuality quality) {
        if (quality == WeatherQuality.PERFORMANCE || quality == WeatherQuality.LOW_SPEC || wave < 0.25) {
            return;
        }
        int count = quality == WeatherQuality.HIGH ? 2 : 1;
        g.setColor(new Color(235, 252, 255));
        for (int i = 0; i < count; i++) {
            int localSeed = Math.abs(seed + i * 9901);
            double pulse = Math.sin(t * Math.PI * 2.0 + localSeed * 0.01);
            if (pulse < 0.25) {
                continue;
            }
            int x = Math.floorMod(localSeed / 23, Math.max(1, tileSize - rel(tileSize, 8))) + rel(tileSize, 4);
            int y = Math.floorMod(localSeed / 47, Math.max(1, tileSize - rel(tileSize, 8))) + rel(tileSize, 4);
            int r = Math.max(1, rel(tileSize, 1 + Math.floorMod(localSeed / 73, 2)));
            g.setComposite(AlphaComposite.SrcOver.derive((float) ((pulse - 0.25) * 0.20)));
            g.drawLine(x - r * 2, y, x + r * 2, y);
            g.drawLine(x, y - r, x, y + r);
        }
    }

    private void drawWaveStroke(Graphics2D g, int x, int y, int length, int amplitude, double phase) {
        int segments = 5;
        int prevX = x;
        int prevY = y + (int) Math.round(Math.sin(phase) * amplitude);
        for (int i = 1; i <= segments; i++) {
            int nextX = x + length * i / segments;
            int nextY = y + (int) Math.round(Math.sin(phase + i * 0.72) * amplitude);
            g.drawLine(prevX, prevY, nextX, nextY);
            prevX = nextX;
            prevY = nextY;
        }
    }

    private int phaseCount(WeatherQuality quality) {
        return switch (quality) {
            case HIGH -> 18;
            case BALANCED -> 14;
            case PERFORMANCE -> 10;
            case LOW_SPEC -> 4;
        };
    }

    private int frameDivisor(WeatherQuality quality) {
        return switch (quality) {
            case HIGH -> 1;
            case BALANCED -> 2;
            case PERFORMANCE -> 3;
            case LOW_SPEC -> 6;
        };
    }

    private int rel(int tileSize, int value) {
        if (value == 0) {
            return 0;
        }
        int scaled = Math.round(value * tileSize / (float) BASE_TILE_SIZE);
        return value < 0 ? Math.min(-1, scaled) : Math.max(1, scaled);
    }

    private float relStroke(int tileSize, float value) {
        return Math.max(0.6f, value * tileSize / BASE_TILE_SIZE);
    }
}
