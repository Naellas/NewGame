package com.alderfall.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;

/** Palette variants of existing shrine art, with restrained event-only particles. */
public final class ShrineEventVisuals {
    private ShrineEventVisuals() {}
    private static final Set<String> SHRINES = Set.of("deco_imagen_shrine_stone", "deco_forest_shrine_stone",
            "folklore_boundary_shrine");
    private static final Color[] COLORS = {new Color(89, 195, 220), new Color(221, 170, 76), new Color(170, 124, 215)};
    public static boolean supports(String asset) { return SHRINES.contains(asset); }
    public static final class Sprites {
        private record Key(BufferedImage source, int variant) {}
        private final Map<Key, BufferedImage> cache = new java.util.LinkedHashMap<>(32, .75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Key, BufferedImage> e) { return size() > 96; }
        };
        public BufferedImage tint(BufferedImage source, int variant) {
            if (variant < 0) return source;
            return cache.computeIfAbsent(new Key(source, Math.floorMod(variant, 3)), key -> {
                BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                g.drawImage(source, 0, 0, null);
                g.setComposite(AlphaComposite.SrcAtop.derive(.32f));
                g.setColor(COLORS[key.variant()]); g.fillRect(0, 0, image.getWidth(), image.getHeight());
                g.dispose();
                return image;
            });
        }
    }
    public static void glow(Graphics2D g, Rectangle bounds, int variant, int frame) {
        Color color = COLORS[Math.floorMod(variant, 3)];
        Graphics2D effect = (Graphics2D) g.create();
        int width = Math.max(12, bounds.width * 2 / 3);
        int center = bounds.x + bounds.width / 2, foot = bounds.y + bounds.height - 3;
        effect.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
        effect.fillOval(center - width / 2, foot - width / 6, width, width / 3);
        for (int i = 0; i < 5; i++) {
            double phase = Math.floorMod(frame + i * 29, 150) / 150.0;
            int x = center + (int) (Math.sin(i * 2.4 + phase * 3) * width * .32);
            int y = foot - (int) (bounds.height * phase);
            int alpha = (int) (150 * Math.sin(Math.PI * phase));
            effect.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            int size = Math.max(2, bounds.width / 20);
            effect.fillOval(x, y, size, size);
        }
        effect.dispose();
    }
}
