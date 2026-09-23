package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/** Rigid upper arm, forearm and hand/held-object layers parented to three joints. */
final class ConceptArmRig {
    record Pose(double shoulderX, double shoulderY, double elbowX, double elbowY, double wristX, double wristY,
                double upperAngle, double lowerAngle, double handX, double handY, double handAngle) { }
    private final BufferedImage torso;
    private final Chain[] chains = new Chain[2];
    private final int[][] owner;
    private final int facing;
    private final double handLength;

    ConceptArmRig(BufferedImage source, int facing, int top, int body, double center, String name) {
        this.facing = facing; handLength = body * .045;
        boolean knightSide=name.startsWith("class_knight")&&facing!=0;
        boolean seraphineSide=name.startsWith("npc_seraphine")&&facing!=0;
        boolean[][] cloak=ConceptPartMasks.cloak(source,name);
        boolean mageSide = name.startsWith("class_mage") && facing != 0;
        int w = source.getWidth(), h = source.getHeight();
        boolean rogue = name.startsWith("class_rogue"), ranger = name.startsWith("class_ranger");
        boolean calder = name.startsWith("npc_calder"), samir = name.startsWith("npc_samir");
        double staffX = name.startsWith("class_cleric") ? staffColumn(source, top, body, center) : Double.NaN;
        int staffArm = Double.isNaN(staffX) ? -1 : facing == 0 ? (staffX < center ? 1 : 0) : ((staffX - center) * facing > 0 ? 0 : 1);
        boolean bowGrip = ranger && !name.matches(".*_model_(?:left|right)");
        double wristOffset = rogue ? -.05 : ranger ? -.045 : calder ? -.015 : samir ? .025 : 0;
        double wristHeight = calder ? .61 : samir ? .60 : name.startsWith("npc_lyra") ? .52 : .56;
        for (int i = 0; i < 2; i++) {
            double side = i == 0 ? 1 : -1;
            double sx = center + (facing == 0 ? side * body * .10 : facing * body * (i == 0 ? .025 : -.075));
            double ex = center + (facing == 0 ? side * body * .13 : facing * body * (i == 0 ? .06 : -.055));
            double wx = center + (facing == 0 ? side * body * .14 : facing * body * (i == 0 ? .09 : wristOffset));
            double sy = top + body * .27, ey = top + body * .415, wy = top + body * wristHeight;
            if (mageSide && i == 1) { sx = center - facing * body * .02; ex = center + facing * body * .01; wx = center + facing * body * .05; wy = top + body * .53; }
            if(knightSide&&i==1){sx=89;sy=39;ex=89;ey=58;wx=96;wy=75;}
            if(seraphineSide&&i==1){sx=102;sy=44;ex=103;ey=63;wx=105;wy=77;}
            if (i == staffArm) { wx = staffX; wy = top + body * .46; ex = (sx + wx) / 2; ey = (sy + wy) / 2; }
            chains[i] = new Chain(w, h, sx, sy, ex, ey, wx, wy,
                    (i == staffArm ? .02 : bowGrip ? .035 : samir || calder ? .09 : .20) * (facing == 0 ? .35 : 1));
        }
        owner = new int[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            owner[y][x] = -1;
            int pixel = source.getRGB(x, y);
            if ((pixel >>> 24) < 32 || cloak[y][x]) continue;
            double best = Double.POSITIVE_INFINITY;
            for (int i = 0; i < 2; i++) {
                if ((mageSide || knightSide || seraphineSide) && i == 0) continue; // Completely occluded in this source: no invented second hand.
                // A side-view far arm is occluded by the chest. Only extract its
                // visible outer edge; do not mistake the breastplate for an arm.
                if (facing != 0 && i == 0 && i != staffArm && facing * (x - center) < body * .085) continue;
                Chain c = chains[i];
                double hand = Math.hypot(x - c.wx, y - (c.wy + body * .018));
                double lower = distance(x, y, c.ex, c.ey, c.wx, c.wy);
                double upper = distance(x, y, c.sx, c.sy + 2, c.ex, c.ey);
                int part = hand < body * .047 ? 2 : lower < body * .039 ? 1 : upper < body * .045 ? 0 : -1;
                if(seraphineSide&&i==1) {
                    // The bright patch to the right is exposed thigh, not a second palm.
                    boolean palm=x>=102&&x<=108&&y>=76&&y<=84&&!(x>=107&&y>=83);
                    part=palm?2:lower<3.5&&y>=61&&y<=77&&x<=107?1:upper<4.7&&y<64&&x<=106?0:-1;
                }
                double score = part == 2 ? hand : part == 1 ? lower : upper;
                if (i == staffArm && Math.abs(x - staffX) < body * (y < top + body * .16 ? .055 : .024)) {
                    part = 2; score = -1;
                }
                // The knife and hammer share the hand transform instead of bending with cloth.
                if (i == 1 && facing != 0 && (rogue || calder || samir)) {
                    double tx = c.wx + facing * body * (rogue ? .075 : calder ? -.11 : .025);
                    double ty = c.wy + body * (rogue ? .11 : calder ? .17 : .13);
                    if (y >= c.wy && distance(x, y, c.wx, c.wy, tx, ty) < body * (calder ? .047 : .032)) {
                        part = 2; score = 0;
                    }
                }
                if (part >= 0 && score < best) { best = score; owner[y][x] = i * 3 + part; }
            }
            if (owner[y][x] >= 0) chains[owner[y][x] / 3].parts[owner[y][x] % 3].setRGB(x, y, pixel);
        }
        // Hidden overlap at elbow/wrist prevents a crack as adjacent rigid pieces rotate.
        // Restrict this underlap to pixels already owned by the same arm.
        for(int i=0;i<2;i++) {
            Chain c=chains[i];
            for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(owner[y][x]>=0&&owner[y][x]/3==i) {
                int p=source.getRGB(x,y);
                if(Math.hypot(x-c.ex,y-c.ey)<2.5){c.parts[0].setRGB(x,y,p);c.parts[1].setRGB(x,y,p);}
                if(Math.hypot(x-c.wx,y-c.wy)<2){c.parts[1].setRGB(x,y,p);c.parts[2].setRGB(x,y,p);}
            }
        }
        torso = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int pixel = source.getRGB(x, y);
            if (owner[y][x] >= 0) {
                pixel = 0;
                // Restore the small body area occluded by the arm; never stretch the torso.
                if (Math.abs(x - center) < body * .10) {
                    double red = 0, green = 0, blue = 0, weight = 0;
                    int radius = Math.max(5, (int) (body * .09));
                    for (int dy = -radius; dy <= radius; dy++) for (int dx = -radius; dx <= radius; dx++) {
                        int xx = x + dx, yy = y + dy;
                        if (xx < 0 || xx >= w || yy < 0 || yy >= h || owner[yy][xx] >= 0) continue;
                        int sample = source.getRGB(xx, yy);
                        if ((sample >>> 24) < 100) continue;
                        double a = 1.0 / (1 + dx * dx + dy * dy);
                        red += ((sample >> 16) & 255) * a; green += ((sample >> 8) & 255) * a;
                        blue += (sample & 255) * a; weight += a;
                    }
                    if (weight > 0) pixel = 0xff000000 | ((int) (red / weight) << 16) | ((int) (green / weight) << 8) | (int) (blue / weight);
                }
            }
            torso.setRGB(x, y, pixel);
        }
    }

    int ownerAt(int x,int y) { return owner[y][x]; }

    BufferedImage torso() { return torso; }

    Pose pose(int frame, int which) {
        Chain c = chains[which];
        double phase = Math.floorMod(frame, WorldCharacterAnimation.FRAMES) * Math.PI * 2 / WorldCharacterAnimation.FRAMES;
        double swing = Math.cos(phase) * c.amplitude * (which == 0 ? 1 : -1) * (facing == 0 ? 1 : facing);
        double upper = swing, lower = swing * .65 + Math.sin(phase) * .025 * Math.min(1, c.amplitude / .20);
        double bob = Math.round(-Math.pow(Math.sin(phase), 2));
        double ex = c.sx + rotateX(c.ex - c.sx, c.ey - c.sy, upper);
        double ey = c.sy + bob + rotateY(c.ex - c.sx, c.ey - c.sy, upper);
        double wx = ex + rotateX(c.wx - c.ex, c.wy - c.ey, lower);
        double wy = ey + rotateY(c.wx - c.ex, c.wy - c.ey, lower);
        double hand = lower + Math.sin(phase - .25) * .035;
        return new Pose(c.sx, c.sy + bob, ex, ey, wx, wy, upper, lower,
                wx + rotateX(0, handLength, hand), wy + rotateY(0, handLength, hand), hand);
    }

    void draw(Graphics2D g, int frame, int which) {
        Chain c = chains[which]; Pose p = pose(frame, which);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        drawPart(g, c.parts[0], c.sx, c.sy, p.shoulderX, p.shoulderY, p.upperAngle);
        drawPart(g, c.parts[1], c.ex, c.ey, p.elbowX, p.elbowY, p.lowerAngle);
        // Wrist translation is inherited exactly; grip pixels rotate as one rigid object.
        drawPart(g, c.parts[2], c.wx, c.wy, p.wristX, p.wristY, p.handAngle);
    }

    private static void drawPart(Graphics2D g, BufferedImage part, double x, double y, double tx, double ty, double angle) {
        AffineTransform transform = new AffineTransform();
        transform.translate(tx, ty); transform.rotate(angle); transform.translate(-x, -y);
        g.drawImage(part, transform, null);
    }
    private static double rotateX(double x, double y, double a) { return x * Math.cos(a) - y * Math.sin(a); }
    private static double rotateY(double x, double y, double a) { return x * Math.sin(a) + y * Math.cos(a); }
    private static double staffColumn(BufferedImage image, int top, int body, double center) {
        int best = 0, column = -1;
        for (int x = 0; x < image.getWidth(); x++) {
            if (Math.abs(x - center) < body * .12 || Math.abs(x - center) > body * .32) continue;
            int count = 0;
            for (int y = top; y < top + body * .75; y++) {
                int p = image.getRGB(x, y), r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
                if ((p >>> 24) > 100 && r > 90 && r > g * .95 && g > b * 1.3) count++;
            }
            if (count > best) { best = count; column = x; }
        }
        return best > body * .28 ? column : Double.NaN;
    }
    private static double distance(double x, double y, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double t = Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)));
        return Math.hypot(x - ax - t * dx, y - ay - t * dy);
    }
    private static final class Chain {
        final double sx, sy, ex, ey, wx, wy, amplitude;
        final BufferedImage[] parts = new BufferedImage[3];
        Chain(int w, int h, double sx, double sy, double ex, double ey, double wx, double wy, double amplitude) {
            this.sx = sx; this.sy = sy; this.ex = ex; this.ey = ey; this.wx = wx; this.wy = wy; this.amplitude = amplitude;
            for (int i = 0; i < 3; i++) parts[i] = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }
    }
}
