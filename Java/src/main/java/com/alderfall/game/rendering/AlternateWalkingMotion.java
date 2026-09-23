package com.alderfall.game;

import java.awt.image.BufferedImage;

/** A stable cutout with independent foot tracks; never interpolates between different drawings. */
final class AlternateWalkingMotion {
    private AlternateWalkingMotion() { }

    static BufferedImage frame(BufferedImage source, int frame, int facing, boolean robes) {
        int w = source.getWidth(), h = source.getHeight(), top = h, bottom = 0;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (opaque(source, x, y)) {
            top = Math.min(top, y); bottom = Math.max(bottom, y);
        }
        if (top >= bottom) return source;
        double phase = Math.floorMod(frame, WorldCharacterAnimation.FRAMES) / (double) WorldCharacterAnimation.FRAMES;
        int body = bottom - top;
        int hip = top + (int) (body * (robes ? .78 : .59));
        int ankle = hip + (int) ((bottom - hip) * .76);
        int left = w, right = 0;
        for (int y = ankle; y <= bottom; y++) for (int x = 0; x < w; x++) if (opaque(source, x, y)) {
            left = Math.min(left, x); right = Math.max(right, x);
        }
        int split = (left + right + 1) / 2;
        double[] feet = new double[2];
        for (int leg = 0; leg < 2; leg++) {
            double total = 0; int count = 0;
            for (int y = ankle; y <= bottom; y++) for (int x = leg == 0 ? 0 : split; x < (leg == 0 ? split : w); x++)
                if (opaque(source, x, y)) { total += x; count++; }
            feet[leg] = count == 0 ? split + (leg == 0 ? -5 : 5) : total / count;
        }
        double reach = facing == 0 ? 4 : Math.min(body * .14, (bottom - hip) * .65);
        double bob = -1.5 * Math.pow(Math.sin(phase * Math.PI * 2), 2);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        // Fixed layer order preserves limb identity when the feet pass one another.
        for (int leg = 0; leg < 2; leg++) {
            double t = (phase + leg * .5) % 1;
            double lift = t < .5 ? 0 : Math.pow(Math.sin((t - .5) * Math.PI * 2), 2) * Math.min(12, (bottom - hip) * .30);
            double target = (facing == 0 ? feet[leg] : split) + (facing == 0 ? 1 : facing) * reach * footTrack(t);
            double verticalScale = (ankle - hip - lift - bob) / Math.max(1, ankle - hip);
            for (int y = (int) Math.floor(hip + bob); y <= bottom; y++) {
                double sy = y >= ankle - lift ? y + lift : hip + (y - hip - bob) / verticalScale;
                int sourceY = (int) Math.round(sy);
                if (sourceY < hip || sourceY > bottom || y < 0) continue;
                double bend = Math.max(0, Math.min(1, (sy - hip) / Math.max(1, ankle - hip)));
                double shift = (target - feet[leg]) * bend;
                // A small forward knee lead on the returning limb, zero at hip and ankle.
                shift += (facing == 0 ? 1 : facing) * Math.sin(Math.PI * bend) * lift * .45;
                for (int x = 0; x < w; x++) {
                    int sx = (int) Math.round(x - shift);
                    if (sx < 0 || sx >= w || (sx < split) != (leg == 0)) continue;
                    int pixel = source.getRGB(sx, sourceY);
                    if ((pixel >>> 24) != 0) out.setRGB(x, y, pixel);
                }
            }
        }
        // Shoulder-to-wrist displacement is continuous and counter to the same-side leg.
        // Keep the head and torso core stable; move peripheral hands/equipment together.
        double center = torsoCenter(source, top + (int) (body * .28), top + (int) (body * .42));
        double shoulder = top + body * .25, wrist = top + body * .54;
        for (int y = 0; y < hip; y++) {
            double arm = y < shoulder ? 0 : y < wrist ? (y - shoulder) / (wrist - shoulder)
                    : Math.max(0, (hip - y) / Math.max(1, hip - wrist));
            double wave = Math.sin(phase * Math.PI * 2) * (robes ? 5 : 7) * arm;
            int dy = y + (int) Math.round(bob);
            if (dy < 0) continue;
            for (int x = 0; x < w; x++) {
                double side = x < center ? -1 : 1;
                double edge = Math.max(0, Math.min(1, (Math.abs(x - center) - body * .045) / (body * .07)));
                int sx = (int) Math.round(x - wave * side * edge);
                if (sx < 0 || sx >= w) continue;
                int pixel = source.getRGB(sx, y);
                if ((pixel >>> 24) != 0) out.setRGB(x, dy, pixel);
            }
        }
        return out;
    }

    /** Constant-speed planted phase; smooth return with matching endpoint velocity. */
    static double footTrack(double phase) {
        if (phase < .5) return 1 - 4 * phase;
        double u = (phase - .5) * 2;
        return -1 - 2 * u + 12 * u * u - 8 * u * u * u;
    }

    private static double torsoCenter(BufferedImage image, int from, int to) {
        double total = 0; int count = 0;
        for (int y = from; y <= to; y++) for (int x = 0; x < image.getWidth(); x++)
            if (opaque(image, x, y)) { total += x; count++; }
        return count == 0 ? image.getWidth() / 2.0 : total / count;
    }

    private static boolean opaque(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) > 100;
    }
}
