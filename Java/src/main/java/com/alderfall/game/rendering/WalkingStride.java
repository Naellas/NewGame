package com.alderfall.game;

import java.awt.image.BufferedImage;

/** Pixel-cutout leg articulation. Texture variations alone do not constitute a stride. */
final class WalkingStride {
    private WalkingStride() { }

    static BufferedImage frame(BufferedImage source, int frame, int facing, boolean robes) {
        boolean sideView = facing != 0;
        int w = source.getWidth(), h = source.getHeight();
        int top = h, bottom = 0;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (opaque(source, x, y)) {
            top = Math.min(top, y); bottom = Math.max(bottom, y);
        }
        int hip = top + (int) Math.round((bottom - top) * (robes ? .72 : .60));
        int legHeight = Math.max(1, bottom - hip);
        // Locate the legs rather than using the cape/weapon's full-image center.
        int left = w, right = 0;
        for (int y = hip; y <= bottom; y++) for (int x = 0; x < w; x++) if (opaque(source, x, y)) {
            left = Math.min(left, x); right = Math.max(right, x);
        }
        int split = (left + right + 1) / 2;
        double phase = Math.floorMod(frame, WorldCharacterAnimation.FRAMES) * Math.PI * 2 / WorldCharacterAnimation.FRAMES;
        double bob = -Math.abs(Math.sin(phase)) * 2;
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        // The rear leg is drawn first; each leg stays attached at the hip while its foot
        // moves through contact, passing and return-swing positions.
        for (int leg = 0; leg < 2; leg++) {
            int minX = leg == 0 ? 0 : split, maxX = leg == 0 ? split : w;
            double sumX = 0; int pixels = 0;
            for (int y = bottom - 10; y <= bottom; y++) for (int x = minX; x < maxX; x++) if (opaque(source, x, y)) {
                sumX += x; pixels++;
            }
            double foot = pixels == 0 ? split + (leg == 0 ? -6 : 6) : sumX / pixels;
            double cycle = phase + (leg == 0 ? Math.PI : 0);
            double reach = sideView ? Math.max(12, (bottom - top) * .14) : 3;
            double target = sideView ? split + facing * Math.cos(cycle) * reach : foot + Math.cos(cycle) * reach;
            double swing = Math.max(0, -Math.sin(cycle));
            double lift = swing * (sideView ? 9 : 11);
            double ankle = hip + legHeight * .73;
            double verticalScale = (ankle - hip - lift - bob) / (ankle - hip);
            for (int y = Math.max(0, (int) Math.floor(hip + bob)); y <= bottom; y++) {
                double sourceY = y >= ankle - lift ? y + lift : hip + (y - hip - bob) / verticalScale;
                int sy = (int) Math.round(sourceY);
                if (sy < hip || sy > bottom) continue;
                double t = (sourceY - hip) / legHeight;
                double bend = Math.min(1, t / .73);
                double shift = (target - foot) * bend * bend * (3 - 2 * bend);
                // The bent knee leads the lifted foot during the passing pose.
                shift += Math.sin(Math.PI * bend) * swing * (sideView ? facing * 4 : 1);
                for (int x = 0; x < w; x++) {
                    int sx = (int) Math.round(x - shift);
                    if (sx < minX || sx >= maxX) continue;
                    int pixel = source.getRGB(sx, sy);
                    if ((pixel >>> 24) != 0) result.setRGB(x, y, pixel);
                }
            }
        }
        int offset = (int) Math.round(bob);
        for (int y = 0; y < hip; y++) for (int x = 0; x < w; x++) {
            int dy = y + offset;
            if (dy >= 0) result.setRGB(x, dy, source.getRGB(x, y));
        }
        return result;
    }

    private static boolean opaque(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) > 100;
    }
}
