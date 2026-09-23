package com.alderfall.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Track individual feet, not image hashes: contacts must alternate and the swing foot must lift. */
public final class WalkingStrideTest {
    public static void main(String[] args) {
        BufferedImage actor = new BufferedImage(192, 144, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = actor.createGraphics();
        g.setColor(Color.GRAY); g.fillRect(78, 6, 36, 82);
        g.setColor(Color.RED); g.fillRect(78, 86, 12, 51);
        g.setColor(Color.BLUE); g.fillRect(102, 86, 12, 51); g.dispose();
        BufferedImage contact = WalkingStride.frame(actor, 0, 1, false);
        BufferedImage passing = WalkingStride.frame(actor, 8, 1, false);
        BufferedImage opposite = WalkingStride.frame(actor, 16, 1, false);
        double[] left = foot(contact, Color.RED.getRGB()), right = foot(contact, Color.BLUE.getRGB());
        double[] nextLeft = foot(opposite, Color.RED.getRGB()), nextRight = foot(opposite, Color.BLUE.getRGB());
        require(nextLeft[0] - left[0] > 25, "Rear foot never steps forward");
        require(right[0] - nextRight[0] > 25, "Front foot never moves back");
        require(left[1] - foot(passing, Color.RED.getRGB())[1] >= 8, "Swing foot never leaves the ground");
        require(Math.abs(right[1] - foot(passing, Color.BLUE.getRGB())[1]) <= 1, "Planted foot slides vertically");
        // At contact both boots are fully visible; in passing one may be occluded by the near leg.
        for (int f : new int[]{0, 16}) {
            BufferedImage image = WalkingStride.frame(actor, f, 1, false);
            require(foot(image, Color.RED.getRGB())[2] >= 9 && foot(image, Color.BLUE.getRGB())[2] >= 9,
                    "Foot is stretched into a thin smear at frame " + f);
        }
        BufferedImage leftPassing = WalkingStride.frame(actor, 8, -1, false);
        require(foot(leftPassing, Color.RED.getRGB())[1] < foot(leftPassing, Color.BLUE.getRGB())[1],
                "Left-facing return step has no lifted foot");
        System.out.println("Walking stride checks passed: alternating contacts, swing-foot clearance, planted-foot stability and intact boots.");
    }

    private static double[] foot(BufferedImage image, int color) {
        int bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
            if (image.getRGB(x, y) == color) bottom = Math.max(bottom, y);
        int count = 0, min = image.getWidth(), max = 0; double sum = 0;
        for (int x = 0; x < image.getWidth(); x++) if (image.getRGB(x, bottom) == color) {
            count++; sum += x; min = Math.min(min, x); max = Math.max(max, x);
        }
        return new double[]{sum / Math.max(1, count), bottom, max - min + 1};
    }

    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}

