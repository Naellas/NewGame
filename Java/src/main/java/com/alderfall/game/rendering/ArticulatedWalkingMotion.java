package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/** Two complete jointed legs behind an independent clothing layer. */
final class ArticulatedWalkingMotion {
    record Leg(double hipX, double hipY, double kneeX, double kneeY, double ankleX, double ankleY) { }
    private final BufferedImage source, upper, lower, boot;
    private final int facing, top, bottom, body, hip, bootHeight, legWidth;
    private final boolean robes;
    private final double center, length, reach, footCenter;
    private final WalkingArmRig arms;
    private final double travelY;

    ArticulatedWalkingMotion(BufferedImage source, int facing, boolean robes) {
        this(source, facing, robes, "");
    }

    ArticulatedWalkingMotion(BufferedImage source, int facing, boolean robes, String name) {
        this.source = source; this.facing = facing;
        travelY = name.contains("_model_up") ? -.35 : name.contains("_model_down") ? .35 : 0;
        int first = source.getHeight(), last = 0;
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++)
            if (opaque(x, y)) { first = Math.min(first, y); last = Math.max(last, y); }
        top = first; bottom = last; body = Math.max(1, bottom - top);
        // Clothing length never determines the anatomical hip or the length of a leg.
        hip = top + (int) Math.round(body * .55);
        center = averageX(top + (int) (body * .12), top + (int) (body * .23)) - facing * body * .025;
        this.robes = robes || continuousSkirt();
        bootHeight = Math.max(5, (int) (body * .07));
        footCenter = averageX(bottom - bootHeight + 1, bottom);
        legWidth = Math.max(5, (int) Math.round(body * .065));
        reach = body * .105;
        length = Math.hypot(bottom - bootHeight - hip + Math.abs(travelY) * reach, reach) / 2 + 1;
        upper = texture(hip, hip + (bottom - bootHeight - hip) / 2);
        lower = texture(hip + (bottom - bootHeight - hip) / 2, bottom - bootHeight);
        int bw = legWidth + 6;
        boot = new BufferedImage(bw, bootHeight + 1, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < boot.getHeight(); y++) for (int x = 0; x < bw; x++) {
            int sx = (int) Math.round(footCenter) + x - bw / 2;
            if (sx >= 0 && sx < source.getWidth()) boot.setRGB(x, y, source.getRGB(sx, bottom - bootHeight + y));
        }
        arms = new WalkingArmRig(source, facing, top, body, center, name);
    }

    Leg leg(int frame, int which) {
        double phase = Math.floorMod(frame, WorldCharacterAnimation.FRAMES) / (double) WorldCharacterAnimation.FRAMES;
        double t = (phase + which * .5) % 1;
        double swing = t < .5 ? 0 : Math.sin((t - .5) * Math.PI * 2);
        double lift = 7 * swing * swing;
        double lane = which == 0 ? -1 : 1;
        double hx = center + lane * (facing == 0 ? legWidth * .62 : 1.5);
        double hy = hip - Math.pow(Math.sin(phase * Math.PI * 2), 2);
        double ax = facing == 0 ? hx + lane * .5 * Math.sin(t * Math.PI * 2)
                : center + facing * reach * AlternateWalkingMotion.footTrack(t) + lane * 1.5;
        double ay = bottom - bootHeight - lift - (which == 0 ? 1 : 0) + travelY * reach * AlternateWalkingMotion.footTrack(t);
        double dx = ax - hx, dy = ay - hy, d = Math.hypot(dx, dy);
        double bend = Math.sqrt(Math.max(0, length * length - d * d / 4));
        double kx = (hx + ax) / 2 + (facing == 0 ? lane * Math.min(2, bend) : facing * dy / d * bend);
        double ky = (hy + ay) / 2 - (facing == 0 ? 0 : facing * dx / d * bend);
        return new Leg(hx, hy, kx, ky, ax, ay);
    }

    WalkingArmRig.Pose arm(int frame, int which) { return arms.pose(frame, which); }

    BufferedImage frame(int frame) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int which = 0; which < 2; which++) {
            Leg p = leg(frame, which);
            // The far limb is a complete limb even when it was hidden in the standing drawing.
            drawSegment(g, upper, p.hipX, p.hipY, p.kneeX, p.kneeY, which == 0);
            drawSegment(g, lower, p.kneeX, p.kneeY, p.ankleX, p.ankleY, which == 0);
            g.drawImage(which == 0 ? shade(boot) : boot, (int) Math.round(p.ankleX - boot.getWidth() / 2.0), (int) Math.round(p.ankleY), null);
        }
        arms.draw(g, frame, 0);
        g.dispose();
        double phase = Math.floorMod(frame, WorldCharacterAnimation.FRAMES) * Math.PI * 2 / WorldCharacterAnimation.FRAMES;
        int bob = (int) Math.round(-Math.pow(Math.sin(phase), 2));
        int hem = top + (int) (body * .91);
        for (int y = top; y <= bottom; y++) for (int x = 0; x < source.getWidth(); x++) {
            int pixel = arms.torso().getRGB(x, y);
            if ((pixel >>> 24) <= 100) continue;
            boolean clothing;
            if (robes) clothing = y < hem;
            else {
                double axis = center + (footCenter - center) * Math.max(0, (y - hip) / (double) Math.max(1, bottom - hip));
                clothing = y < hip || (y < bottom - bootHeight - 3 && Math.abs(x - axis) > legWidth * 1.3);
            }
            if (!clothing) continue;
            // Only loose cloth below the belt may sway. The chest/abdomen never expand.
            double clothWeight = Math.max(0, (y - (top + body * .60)) / (body * .40));
            double cloth = Math.sin(phase - clothWeight * 1.2) * 2.5 * clothWeight;
            int tx = (int) Math.round(x + cloth), ty = y + bob;
            if (tx >= 0 && tx < out.getWidth() && ty >= 0 && ty < out.getHeight()) out.setRGB(tx, ty, pixel);
        }
        g = out.createGraphics(); arms.draw(g, frame, 1); g.dispose();
        return out;
    }

    private boolean continuousSkirt() {
        // A broad, unbroken lower silhouette is clothing, not two exposed legs.
        int half = Math.max(4, (int) (body * .07));
        for (double row : new double[]{.74, .81, .86}) {
            int count = 0, total = 0, y = top + (int) (body * row);
            for (int x = (int) center - half; x <= (int) center + half; x++) {
                total++;
                if (x >= 0 && x < source.getWidth() && opaque(x, y)) count++;
            }
            if (count < total * .95) return false;
        }
        return true;
    }

    private BufferedImage texture(int from, int to) {
        BufferedImage result = new BufferedImage(legWidth, Math.max(2, to - from + 1), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < result.getHeight(); y++) for (int x = 0; x < legWidth; x++) {
            double t = (from + y - hip) / (double) Math.max(1, bottom - hip);
            int axis = (int) Math.round(center + (footCenter - center) * t);
            int sy = from + y;
            // Hidden trouser sections use the visible shin's palette, not robe fabric.
            if (robes) sy = Math.max(top, bottom - bootHeight - 3 + y % 3);
            int sx = Math.max(0, Math.min(source.getWidth() - 1, axis + x - legWidth / 2));
            int pixel = source.getRGB(sx, sy);
            if ((pixel >>> 24) < 100) {
                pixel = source.getRGB((int) Math.round(footCenter), bottom - 2);
                if ((pixel >>> 24) < 100) pixel = 0xff302b26;
            }
            if (x == 0 || x == legWidth - 1) pixel = darken(pixel, .72);
            result.setRGB(x, y, pixel);
        }
        return result;
    }

    private static void drawSegment(Graphics2D g, BufferedImage image, double x1, double y1, double x2, double y2, boolean far) {
        AffineTransform transform = new AffineTransform();
        transform.translate(x1, y1); transform.rotate(Math.atan2(y2 - y1, x2 - x1) - Math.PI / 2);
        transform.scale(1, (Math.hypot(x2 - x1, y2 - y1) + 2) / image.getHeight());
        transform.translate(-image.getWidth() / 2.0, -1);
        g.drawImage(far ? shade(image) : image, transform, null);
    }

    private static BufferedImage shade(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) for (int x = 0; x < out.getWidth(); x++) out.setRGB(x, y, darken(source.getRGB(x, y), .78));
        return out;
    }
    private static int darken(int p, double factor) { return (p & 0xff000000) | ((int) (((p >> 16) & 255) * factor) << 16) | ((int) (((p >> 8) & 255) * factor) << 8) | (int) ((p & 255) * factor); }
    private boolean opaque(int x, int y) { return (source.getRGB(x, y) >>> 24) > 100; }
    private double averageX(int from, int to) {
        double sum = 0; int count = 0;
        for (int y = from; y <= to; y++) for (int x = 0; x < source.getWidth(); x++) if (opaque(x, y)) { sum += x; count++; }
        return count == 0 ? source.getWidth() / 2.0 : sum / count;
    }
}
