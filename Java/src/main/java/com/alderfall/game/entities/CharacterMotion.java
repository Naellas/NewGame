package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Set;

/** Small, cached secondary motion for characters without an authored action strip. */
final class CharacterMotion {
    static final int FRAMES = 24;
    private static final Set<String> ACTIONS = Set.of("idle", "attack", "cast", "shoot", "defend", "item", "hit");

    static boolean supports(String sprite, String action) {
        return sprite != null && action != null && ACTIONS.contains(action)
                && (sprite.startsWith("class_") || sprite.startsWith("npc_"))
                && (sprite.endsWith("_model") || sprite.endsWith("_battle_sprite") || sprite.matches(".*_combat_v[234]"));
    }

    static BufferedImage frame(BufferedImage source, String action, int frame) {
        int w = source.getWidth(), h = source.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        double p = Math.floorMod(frame, FRAMES) / (double) (FRAMES - 1);
        double breath = Math.sin(p * Math.PI * 2);
        double pulse = Math.sin(p * Math.PI);
        double lean = switch (action) {
            case "attack" -> p < 7.0 / 12 ? -smooth(p / (7.0 / 12))
                    : p < .75 ? -1 + 2.5 * smooth((p - 7.0 / 12) / (2.0 / 12)) : 1.5 * (1 - smooth((p - .75) / .25));
            case "shoot" -> -pulse;
            case "cast", "item" -> pulse * .5;
            case "hit" -> -Math.sin(Math.min(1, p * 3) * Math.PI) * (1 - p);
            case "defend" -> -pulse * .6;
            default -> breath * .12;
        };
        // Continuous deformation keeps the painted silhouette connected; soles stay planted.
        for (int y = 0; y < h; y++) {
            double upper = Math.max(0, 1 - y / (h * .90));
            int dx = (int) Math.round(lean * w * .025 * Math.sin(upper * Math.PI / 2));
            int sampleY = Math.max(0, Math.min(h - 1, y + (int) Math.round(
                    ("idle".equals(action) ? breath * .024 : pulse * .008) * h * upper)));
            g.drawImage(source, dx, y, dx + w, y + 1, 0, sampleY, w, sampleY + 1, null);
        }
        g.dispose();
        return out;
    }

    private static double smooth(double t) { return t * t * (3 - 2 * t); }
}
