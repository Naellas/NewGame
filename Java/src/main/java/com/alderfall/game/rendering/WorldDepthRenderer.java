package com.alderfall.game;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Ground-contact ordering and local, feathered cutaways through foreground scenery. */
public final class WorldDepthRenderer {
    private record Entry(double depth, Rectangle bounds, boolean scenery, Consumer<Graphics2D> paint) { }
    private record Reveal(double depth, float x, float y, float radius) { }
    private final List<Entry> entries = new ArrayList<>();
    private final List<Reveal> reveals = new ArrayList<>();
    private final List<Consumer<Graphics2D>> overlays = new ArrayList<>();
    private BufferedImage scratch;
    private boolean collecting;
    private int tileSize;

    public void begin(int tileSize) {
        entries.clear();
        reveals.clear();
        overlays.clear();
        this.tileSize = tileSize;
        collecting = true;
    }

    public boolean collecting() { return collecting; }

    public void scenery(Graphics2D g, double depth, Rectangle bounds, Consumer<Graphics2D> paint) {
        if (!collecting) { paint.accept(g); return; }
        entries.add(new Entry(depth, new Rectangle(bounds), true, paint));
    }

    public void character(Graphics2D g, Rectangle bounds, Consumer<Graphics2D> paint) {
        if (!collecting) { paint.accept(g); return; }
        double depth = bounds.getMaxY();
        entries.add(new Entry(depth, new Rectangle(bounds), false, paint));
        reveals.add(new Reveal(depth, (float) bounds.getCenterX(),
                bounds.y + bounds.height * 0.48f, Math.max(tileSize * 0.85f, bounds.height * 0.78f)));
    }

    public void overlay(Graphics2D g, Consumer<Graphics2D> paint) {
        if (collecting) overlays.add(paint); else paint.accept(g);
    }

    public void draw(Graphics2D g) {
        collecting = false;
        // Stable ties: characters submitted after scenery stand in front at equal depth.
        entries.sort(Comparator.comparingDouble(Entry::depth));
        for (Entry entry : entries) {
            Rectangle bounds = entry.bounds;
            Rectangle clip = g.getClipBounds();
            if (clip != null) bounds = bounds.intersection(clip);
            if (bounds.isEmpty()) continue;
            boolean cutaway = false;
            if (entry.scenery) for (Reveal reveal : reveals) {
                if (overlaps(entry, reveal)) { cutaway = true; break; }
            }
            if (!cutaway) {
                Graphics2D layer = (Graphics2D) g.create();
                entry.paint.accept(layer);
                layer.dispose();
                continue;
            }
            // Only overlapping scenery uses a reusable, viewport-clipped alpha surface.
            if (scratch == null || scratch.getWidth() < bounds.width || scratch.getHeight() < bounds.height) {
                scratch = new BufferedImage(Math.max(bounds.width, scratch == null ? 1 : scratch.getWidth()),
                        Math.max(bounds.height, scratch == null ? 1 : scratch.getHeight()), BufferedImage.TYPE_INT_ARGB_PRE);
            }
            Graphics2D layer = scratch.createGraphics();
            layer.setComposite(AlphaComposite.Clear);
            layer.fillRect(0, 0, bounds.width, bounds.height);
            layer.setComposite(AlphaComposite.SrcOver);
            layer.setRenderingHints(g.getRenderingHints());
            layer.setClip(0, 0, bounds.width, bounds.height);
            layer.translate(-bounds.x, -bounds.y);
            entry.paint.accept(layer);
            layer.setComposite(AlphaComposite.DstOut);
            for (Reveal reveal : reveals) {
                if (!overlaps(entry, reveal)) continue;
                // Ease the cutaway as the feet approach the object's ground line.
                float amount = (float) Math.min(1, (entry.depth - reveal.depth) / Math.max(1, tileSize * 0.25));
                amount = amount * amount * (3 - 2 * amount);
                int alpha = Math.round(235 * amount);
                layer.setPaint(new RadialGradientPaint(reveal.x, reveal.y, reveal.radius,
                        new float[]{0, 0.35f, 0.7f, 1},
                        new Color[]{new Color(0, 0, 0, alpha), new Color(0, 0, 0, alpha),
                                new Color(0, 0, 0, Math.round(alpha * 0.45f)), new Color(0, 0, 0, 0)}));
                int left = (int) Math.floor(reveal.x - reveal.radius);
                int top = (int) Math.floor(reveal.y - reveal.radius);
                int diameter = (int) Math.ceil(reveal.radius * 2) + 2;
                layer.fillRect(left, top, diameter, diameter);
            }
            layer.dispose();
            g.drawImage(scratch, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height,
                    0, 0, bounds.width, bounds.height, null);
        }
        for (Consumer<Graphics2D> overlay : overlays) {
            Graphics2D layer = (Graphics2D) g.create();
            overlay.accept(layer);
            layer.dispose();
        }
        entries.clear();
        reveals.clear();
        overlays.clear();
    }

    private static boolean overlaps(Entry entry, Reveal reveal) {
        if (entry.depth <= reveal.depth) return false;
        double dx = Math.max(entry.bounds.x - reveal.x, Math.max(0, reveal.x - entry.bounds.getMaxX()));
        double dy = Math.max(entry.bounds.y - reveal.y, Math.max(0, reveal.y - entry.bounds.getMaxY()));
        return dx * dx + dy * dy < reveal.radius * reveal.radius;
    }
}
