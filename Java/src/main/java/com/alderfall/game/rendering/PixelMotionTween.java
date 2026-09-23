package com.alderfall.game;

import java.awt.image.BufferedImage;

/** Cached bidirectional block motion for in-betweens, retaining nearest-neighbor pixel edges. */
final class PixelMotionTween {
    private static final int GRID = 12, SEARCH = 8;
    private final BufferedImage a, b;
    private final int w, h, cols, rows;
    private final float[][] forward, backward;

    PixelMotionTween(BufferedImage a, BufferedImage b) {
        this.a = a; this.b = b; w = a.getWidth(); h = a.getHeight();
        cols = (w + GRID - 1) / GRID + 1; rows = (h + GRID - 1) / GRID + 1;
        forward = flow(a, b); backward = flow(b, a);
    }

    BufferedImage at(double t) {
        if (t <= 0) return a;
        if (t >= 1) return b;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double ax = interpolate(forward[0], x, y), ay = interpolate(forward[1], x, y);
            double bx = interpolate(backward[0], x, y), by = interpolate(backward[1], x, y);
            int pa = pixel(a, (int) Math.round(x - t * ax), (int) Math.round(y - t * ay));
            int pb = pixel(b, (int) Math.round(x - (1 - t) * bx), (int) Math.round(y - (1 - t) * by));
            // Keep one opaque silhouette. Cross-fading mismatched cape/head outlines
            // creates translucent double bodies, especially at the cycle seam.
            out.setRGB(x, y, t < .5 ? pa : pb);
        }
        return out;
    }

    static int mix(int a, int b, double t) {
        double aa = (a >>> 24) * (1 - t), ab = (b >>> 24) * t, alpha = aa + ab;
        if (alpha < 1) return 0;
        int r = (int) Math.round(((a >> 16 & 255) * aa + (b >> 16 & 255) * ab) / alpha);
        int g = (int) Math.round(((a >> 8 & 255) * aa + (b >> 8 & 255) * ab) / alpha);
        int bl = (int) Math.round(((a & 255) * aa + (b & 255) * ab) / alpha);
        return (int) Math.round(alpha) << 24 | r << 16 | g << 8 | bl;
    }

    private float[][] flow(BufferedImage from, BufferedImage to) {
        float[][] result = new float[2][cols * rows];
        for (int gy = 0; gy < rows; gy++) for (int gx = 0; gx < cols; gx++) {
            int cx = gx * GRID, cy = gy * GRID, bestX = 0, bestY = 0;
            long best = Long.MAX_VALUE;
            for (int dy = -SEARCH; dy <= SEARCH; dy += 2) for (int dx = -SEARCH; dx <= SEARCH; dx += 2) {
                long score = (dx * dx + dy * dy) * 12L;
                for (int py = -6; py <= 6; py += 3) for (int px = -6; px <= 6; px += 3) {
                    int p = pixel(from, cx + px, cy + py), q = pixel(to, cx + px + dx, cy + py + dy);
                    int ap = p >>> 24, aq = q >>> 24;
                    score += Math.abs(ap - aq) * 3;
                    if (ap > 32 && aq > 32) score += Math.abs((p >> 16 & 255) - (q >> 16 & 255))
                            + Math.abs((p >> 8 & 255) - (q >> 8 & 255)) + Math.abs((p & 255) - (q & 255));
                }
                if (score < best) { best = score; bestX = dx; bestY = dy; }
            }
            result[0][gy * cols + gx] = bestX; result[1][gy * cols + gx] = bestY;
        }
        return result;
    }

    private double interpolate(float[] field, int x, int y) {
        int cx = x / GRID, cy = y / GRID;
        double tx = (x % GRID) / (double) GRID, ty = (y % GRID) / (double) GRID;
        int i = cy * cols + cx;
        return (field[i] * (1 - tx) + field[i + 1] * tx) * (1 - ty)
                + (field[i + cols] * (1 - tx) + field[i + cols + 1] * tx) * ty;
    }

    private int pixel(BufferedImage image, int x, int y) {
        return x < 0 || y < 0 || x >= w || y >= h ? 0 : image.getRGB(x, y);
    }
}
