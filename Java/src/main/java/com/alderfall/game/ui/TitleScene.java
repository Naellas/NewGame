package com.alderfall.game;

import java.awt.*;
import java.awt.geom.Path2D;

/** Decorative title stage. All geometry uses the same logical viewport as menu hit targets. */
final class TitleScene {
    private static final Color GOLD = new Color(191, 160, 94);

    static void cast(Graphics2D target, AssetStore assets, int width, int height) {
        Graphics2D g = (Graphics2D) target.create();
        int side = (width - 440) / 2;
        int base = height - 112;
        int tall = Math.min(530, (int) (height * .57));
        String[] left = {"npc_story_elder_rowan", "class_cleric", "class_knight", "npc_story_maelis"};
        String[] right = {"npc_story_captain_elric_snowrest", "class_ranger", "npc_story_selene", "class_mage"};
        for (int i = 0; i < 4; i++) {
            int h = (int) (tall * (.84 + i * .07));
            int w = Math.min((int) (h * .72), side * 46 / 100);
            int cx = 48 + (int) ((side - 68) * (.16 + i * .205));
            figure(g, assets, left[i], cx - w / 2, base - h - (3 - i) * 22, w, h);
            figure(g, assets, right[i], width - cx - w / 2, base - h - (3 - i) * 22, w, h);
        }
        g.dispose();
    }

    private static void figure(Graphics2D g, AssetStore assets, String name, int x, int y, int w, int h) {
        g.setColor(new Color(0, 0, 0, 100));
        g.fillOval(x + w / 6, y + h - 10, w * 2 / 3, 20);
        g.drawImage(assets.spriteFit(name + "_dialogue_sprite", w, h), x, y, null);
    }

    static void foreground(Graphics2D target, int width, int height, int frame) {
        Graphics2D g = (Graphics2D) target.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, height - 170, new Color(5, 15, 18, 0),
                0, height, new Color(4, 12, 15, 230)));
        g.fillRect(0, height - 170, width, 170);
        // Foreground fronds overlap the feet, anchoring the cast in the clearing.
        for (int side = 0; side < 2; side++) {
            Graphics2D fern = (Graphics2D) g.create();
            fern.translate(side == 0 ? 0 : width, height);
            if (side == 1) fern.scale(-1, 1);
            for (int i = 0; i < 7; i++) {
                fern.setColor(new Color(9 + i, 24 + i * 2, 23 + i));
                int x = i * 38 - 45;
                int h = 115 + (i * 31) % 80;
                fern.drawLine(x, 0, x + 70, -h);
                for (int j = 1; j < 8; j++) {
                    int px = x + j * 9, py = -h * j / 8;
                    leaf(fern, px, py, -36 + j * 3, -24);
                    leaf(fern, px, py, 42 - j * 3, -5);
                }
            }
            fern.dispose();
        }
        for (int i = 0; i < 26; i++) {
            int x = 45 + (i * 137) % Math.max(1, width - 90);
            int y = height - 95 - (i * 73) % Math.max(1, height / 2);
            x += (int) (Math.sin(frame * .012 + i) * 12);
            y += (int) (Math.cos(frame * .009 + i * 2) * 9);
            int a = 65 + (int) ((Math.sin(frame * .035 + i) + 1) * 65);
            g.setColor(new Color(220, 191, 103, a / 5));
            g.fillOval(x - 4, y - 4, 10, 10);
            g.setColor(new Color(249, 223, 149, a));
            g.fillOval(x, y, 2, 2);
        }
        g.dispose();
    }

    static void frame(Graphics2D target, int x, int y, int w, int h, boolean ornate) {
        Graphics2D g = (Graphics2D) target.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(20, 25, 23, 180));
        g.drawRect(x - 1, y - 1, w + 2, h + 2);
        g.setColor(GOLD);
        g.drawRect(x, y, w, h);
        g.setColor(new Color(191, 160, 94, 85));
        g.drawRect(x + 5, y + 5, w - 10, h - 10);
        for (int sx : new int[]{-1, 1}) for (int sy : new int[]{-1, 1}) {
            Graphics2D corner = (Graphics2D) g.create();
            corner.translate(sx == 1 ? x : x + w, sy == 1 ? y : y + h);
            corner.scale(sx, sy);
            corner.setColor(GOLD);
            corner.setStroke(new BasicStroke(1.4f));
            int size = ornate ? 48 : 13;
            corner.drawLine(0, 10, size, 10);
            corner.drawLine(10, 0, 10, size);
            if (ornate) {
                corner.drawArc(10, 10, 42, 42, 0, 90);
                for (int i = 0; i < 3; i++) {
                    leaf(corner, 15 + i * 11, 10, 9, 7);
                    leaf(corner, 10, 15 + i * 11, 7, 9);
                }
            }
            diamond(corner, 4, 4, ornate ? 5 : 3);
            corner.dispose();
        }
        if (ornate) {
            g.setColor(GOLD);
            diamond(g, x + w / 2, y, 7);
            diamond(g, x + w / 2, y + h, 7);
            g.drawLine(x + w / 2 - 65, y, x + w / 2 - 15, y);
            g.drawLine(x + w / 2 + 15, y, x + w / 2 + 65, y);
        }
        g.dispose();
    }

    private static void diamond(Graphics2D g, int x, int y, int r) {
        g.fillPolygon(new int[]{x, x + r, x, x - r}, new int[]{y - r, y, y + r, y}, 4);
    }

    private static void leaf(Graphics2D g, int x, int y, int dx, int dy) {
        Path2D p = new Path2D.Double();
        p.moveTo(x, y);
        p.quadTo(x + dx * .3 - dy * .25, y + dy * .3 + dx * .25, x + dx, y + dy);
        p.quadTo(x + dx * .65 + dy * .25, y + dy * .65 - dx * .25, x, y);
        g.fill(p);
    }

    private TitleScene() {}
}
