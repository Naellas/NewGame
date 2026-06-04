package com.alderfall.game;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

final class WeatherLayerCacheStore {
    private final Map<String, WeatherLayerCache> caches = new HashMap<>();

    void drawFaded(Graphics2D g, int width, int height, String key, int zoom, int frame,
                   float alpha, int interval, Painter painter) {
        WeatherLayerCache cache = cache("faded:" + key);
        String cacheKey = key + ":" + width + "x" + height + ":" + zoom;
        int cacheFrame = frame / Math.max(1, interval);
        if (needsPaint(cache, width, height, cacheKey, cacheFrame)) {
            cache.image = paintBuffer(cache.image, width, height, painter);
            cache.cacheKey = cacheKey;
            cache.cacheFrame = cacheFrame;
        }
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampAlpha(alpha)));
        g.drawImage(cache.image, 0, 0, null);
        g.setComposite(oldComposite);
    }

    void drawStable(Graphics2D g, int width, int height, String key, int zoom, int frame,
                    int interval, Painter painter) {
        WeatherLayerCache cache = cache("stable:" + key);
        String cacheKey = key + ":" + width + "x" + height + ":" + zoom;
        int cacheFrame = frame / Math.max(1, interval);
        if (needsPaint(cache, width, height, cacheKey, cacheFrame)) {
            cache.image = paintBuffer(cache.image, width, height, painter);
            cache.cacheKey = cacheKey;
            cache.cacheFrame = cacheFrame;
        }
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(cache.image, 0, 0, null);
        g.setComposite(oldComposite);
    }

    void drawPrecipitation(Graphics2D g, int width, int height, String key, int zoom, int frame,
                           int intensityBucket, int interval, Painter painter, FrameShift xShift, FrameShift yShift) {
        WeatherLayerCache cache = cache("precipitation:" + key);
        String cacheKey = key + ":" + width + "x" + height + ":" + zoom + ":" + intensityBucket;
        int cacheFrame = frame / Math.max(1, interval);
        if (needsPaint(cache, width, height, cacheKey, cacheFrame)) {
            cache.image = paintBuffer(cache.image, width, height, painter);
            cache.cacheKey = cacheKey;
            cache.cacheFrame = cacheFrame;
            cache.sourceFrame = frame;
        }
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver);
        int frameDelta = Math.max(0, frame - cache.sourceFrame);
        drawWrapped(g, cache.image, xShift.shift(frameDelta), yShift.shift(frameDelta));
        g.setComposite(oldComposite);
    }

    void drawScrollingTile(Graphics2D g, BufferedImage sprite, int width, int height, String key, int zoom,
                           int tileW, int tileH, int shiftX, int shiftY, float alpha) {
        if (alpha <= 0.0f || tileW <= 0 || tileH <= 0) {
            return;
        }
        int bufferW = bufferSpan(width, tileW);
        int bufferH = bufferSpan(height, tileH);
        WeatherLayerCache cache = cache("scrolling:" + key);
        String cacheKey = key + ":" + bufferW + "x" + bufferH + ":" + zoom + ":" + tileW + "x" + tileH;
        if (cache.image == null
                || cache.image.getWidth() != bufferW
                || cache.image.getHeight() != bufferH
                || !cacheKey.equals(cache.cacheKey)) {
            cache.image = paintBuffer(cache.image, bufferW, bufferH, layer -> {
                for (int y = 0; y < bufferH; y += tileH) {
                    for (int x = 0; x < bufferW; x += tileW) {
                        layer.drawImage(sprite, x, y, tileW, tileH, null);
                    }
                }
            });
            cache.cacheKey = cacheKey;
            cache.cacheFrame = -1;
        }
        Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampAlpha(alpha)));
        drawWrapped(g, cache.image, shiftX, shiftY);
        g.setComposite(oldComposite);
    }

    private boolean needsPaint(WeatherLayerCache cache, int width, int height, String cacheKey, int cacheFrame) {
        return cache.image == null
                || cache.image.getWidth() != width
                || cache.image.getHeight() != height
                || !cacheKey.equals(cache.cacheKey)
                || cache.cacheFrame != cacheFrame;
    }

    private WeatherLayerCache cache(String key) {
        return caches.computeIfAbsent(key, ignored -> new WeatherLayerCache());
    }

    private BufferedImage paintBuffer(BufferedImage buffer, int width, int height, Painter painter) {
        if (buffer == null || buffer.getWidth() != width || buffer.getHeight() != height) {
            buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D layer = buffer.createGraphics();
        layer.setComposite(AlphaComposite.Clear);
        layer.fillRect(0, 0, width, height);
        layer.setComposite(AlphaComposite.SrcOver);
        layer.setClip(0, 0, width, height);
        painter.paint(layer);
        layer.dispose();
        return buffer;
    }

    private void drawWrapped(Graphics2D g, BufferedImage buffer, int shiftX, int shiftY) {
        int width = buffer.getWidth();
        int height = buffer.getHeight();
        int x = Math.floorMod(shiftX, Math.max(1, width));
        int y = Math.floorMod(shiftY, Math.max(1, height));
        if (x == 0 && y == 0) {
            g.drawImage(buffer, 0, 0, null);
            return;
        }
        int startX = x == 0 ? 0 : x - width;
        int startY = y == 0 ? 0 : y - height;
        for (int drawY = startY; drawY < height; drawY += height) {
            for (int drawX = startX; drawX < width; drawX += width) {
                g.drawImage(buffer, drawX, drawY, null);
            }
        }
    }

    private int bufferSpan(int viewportSpan, int tileSpan) {
        return Math.max(tileSpan, ((Math.max(1, viewportSpan) + tileSpan - 1) / tileSpan) * tileSpan);
    }

    private float clampAlpha(float alpha) {
        return Math.max(0.0f, Math.min(1.0f, alpha));
    }

    @FunctionalInterface
    interface Painter {
        void paint(Graphics2D g);
    }

    @FunctionalInterface
    interface FrameShift {
        int shift(int frameDelta);
    }

    private static final class WeatherLayerCache {
        private BufferedImage image;
        private String cacheKey = "";
        private int cacheFrame = -1;
        private int sourceFrame = -1;
    }
}
