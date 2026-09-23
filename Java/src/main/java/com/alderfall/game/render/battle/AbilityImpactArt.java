package com.alderfall.game.render.battle;

import com.alderfall.game.AssetStore;
import com.alderfall.game.ClassAbilityVfx;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/** Dedicated imagegen impact artwork, separate from each ability's traveling projectile. */
public final class AbilityImpactArt {
    public static final Map<String, String> SPRITES = Map.ofEntries(
            Map.entry("root_memory", "fx_impact_root_memory_v2"),
            Map.entry("mend_unique", "fx_impact_mend_v2"),
            Map.entry("grove_hymn", "fx_impact_grove_hymn_v2"),
            Map.entry("seraphic_hymn", "fx_impact_seraphic_hymn_v2"),
            Map.entry("thorn_lash_unique", "fx_impact_thorn_lash_v2"),
            Map.entry("briar_tempest_unique", "fx_impact_briar_tempest_v2"),
            Map.entry("glacier_prison_unique", "fx_impact_glacier_prison_v2"),
            Map.entry("ley_detonation", "fx_impact_ley_detonation_v2"),
            Map.entry("firebolt_unique", "fx_impact_firebolt_v2"),
            Map.entry("inferno_script_unique", "fx_impact_inferno_script_v2"));

    private final AssetStore assets;

    public AbilityImpactArt(AssetStore assets) { this.assets = assets; }

    public boolean has(String kind) {
        String sprite = SPRITES.get(kind);
        return sprite != null && assets.hasSprite(sprite);
    }

    public boolean has(ClassAbilityVfx.Profile profile) {
        return profile != null && assets.hasSprite(profile.sheet());
    }

    public BufferedImage texture(ClassAbilityVfx.Profile profile) {
        return assets.effectAtlasCell(profile.sheet(), profile.cell(), profile.columns(), profile.rows());
    }

    public void draw(Graphics2D g, ClassAbilityVfx.Profile profile, int x, int y, double width, double height, double p) {
        if (!has(profile) || p < 0 || p >= 1) return;
        BufferedImage image = texture(profile);
        Graphics2D fx = (Graphics2D) g.create();
        fx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        fx.translate(x - width / 2, y - height * 0.55);
        fx.scale(width / 512, height / 512);
        switch (profile.family()) {
            case SLASH -> drawSweep(fx, image, p, false);
            case SHADOW -> drawShadow(fx, image, p);
            case PIERCE -> drawSweep(fx, image, p, true);
            case BASH -> {
                if (profile.sheet().contains("stonebreaker") || profile.name().startsWith("@stonebreaker/")) {
                    if (p < 0.60) drawGrowth(fx, image, Math.min(0.55, p * 0.55 / 0.44), true);
                    else drawFragments(fx, image, p, true, false);
                } else if (p < 0.24) {
                    Graphics2D hit = (Graphics2D) fx.create();
                    double reach = ease(p / 0.24) * 436;
                    hit.setComposite(AlphaComposite.SrcOver.derive((float) ease(p / 0.10)));
                    hit.clip(new java.awt.geom.Ellipse2D.Double(256 - reach, 290 - reach, reach * 2, reach * 2));
                    hit.drawImage(image, 0, 0, null); hit.dispose();
                } else drawFragments(fx, image, 0.60 + (p - 0.24) / 0.76 * 0.40, true, false);
            }
            case ROOT -> drawGrowth(fx, image, p, false);
            case ICE -> {
                if (p < 0.60) drawGrowth(fx, image, p, true);
                else drawFragments(fx, image, p, true, false);
            }
            case HEAL -> drawHealing(fx, image, p, false);
            case WARD -> drawWard(fx, image, p);
            case VEIL -> {
                fx.translate(Math.sin(p * 9) * 18, -p * 30);
                drawHealing(fx, image, p, true);
            }
            case LIGHTNING -> {
                Graphics2D bolt = (Graphics2D) fx.create();
                bolt.setComposite(AlphaComposite.SrcOver.derive((float) ((0.4 + Math.abs(Math.sin(p * 18)) * 0.6) * (1 - p))));
                int reveal = (int) (512 * Math.min(1, p * 7));
                bolt.clipRect(0, 0, 512, reveal);
                bolt.drawImage(image, (int) (Math.sin(p * 35) * 4), 0, null); bolt.dispose();
            }
            case FIRE, ARCANE, HOLY, POISON -> drawFragments(fx, image, p, false,
                    profile.family() == ClassAbilityVfx.Family.ARCANE || profile.family() == ClassAbilityVfx.Family.HOLY);
        }
        drawMotes(fx, p, tint(profile), profile.restorative() || profile.family() == ClassAbilityVfx.Family.ROOT);
        fx.dispose();
    }

    private void drawSweep(Graphics2D g, BufferedImage image, double p, boolean piercing) {
        int head = (int) (ease(p / 0.38) * 600);
        int tail = (int) (ease((p - 0.28) / 0.72) * 600);
        Graphics2D sweep = (Graphics2D) g.create();
        sweep.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, (1 - p) * 2)));
        sweep.translate(piercing ? p * 30 : 0, piercing ? 0 : Math.sin(p * Math.PI) * -8);
        sweep.clipRect(tail, 0, Math.max(0, head - tail), 512);
        sweep.drawImage(image, 0, 0, null);
        sweep.dispose();
    }

    /** Shadow ribbons gather in staggered bands before sweeping through the target. */
    private void drawShadow(Graphics2D g, BufferedImage image, double p) {
        for (int band = 0; band < 16; band++) {
            double delay = (band % 4) * 0.022;
            double build = ease((p - delay) / 0.26);
            double dissolve = ease((p - 0.50 - delay) / 0.38);
            if (build == 0 || dissolve == 1) continue;
            Graphics2D ribbon = (Graphics2D) g.create();
            ribbon.setComposite(AlphaComposite.SrcOver.derive((float) (build * (1 - dissolve) * 0.94)));
            int shift = (int) ((1 - build) * -55 + dissolve * 70);
            int top = band * 32;
            ribbon.drawImage(image, shift, top, 512 + shift, top + 32,
                    0, top, 512, top + 32, null);
            ribbon.dispose();
        }
    }

    private void drawWard(Graphics2D g, BufferedImage image, double p) {
        for (int segment = 0; segment < 8; segment++) {
            double build = ease((p - segment * 0.025) / 0.25);
            double fade = 1 - ease((p - 0.70) / 0.30);
            if (build == 0) continue;
            g.setComposite(AlphaComposite.SrcOver.derive((float) (build * fade * 0.88)));
            int left = segment * 64, shift = (int) ((1 - build) * (segment < 4 ? -28 : 28));
            g.drawImage(image, left + shift, 0, left + 64 + shift, 512, left, 0, left + 64, 512, null);
        }
    }

    public Color tint(ClassAbilityVfx.Profile profile) {
        return switch (profile.family()) {
            case FIRE -> new Color(255, 159, 65);
            case ICE, LIGHTNING -> new Color(169, 226, 251);
            case ARCANE, SHADOW, VEIL -> new Color(187, 151, 223);
            case ROOT, POISON -> new Color(158, 191, 100);
            case HEAL -> new Color(184, 237, 174);
            case HOLY, WARD -> new Color(246, 223, 154);
            case BASH -> new Color(192, 166, 118);
            default -> new Color(227, 220, 195);
        };
    }

    public void drawCharge(Graphics2D g, ClassAbilityVfx.Profile profile, int[] source, double p) {
        if (!has(profile)) return;
        Graphics2D fx = (Graphics2D) g.create();
        fx.setColor(tint(profile));
        for (int i = 0; i < 9; i++) {
            double angle = i * 2.399 + p * 2;
            double distance = 9 + (1 - p) * (18 + i % 3 * 10);
            int x = source[0] + (int) (Math.cos(angle) * distance);
            int y = source[1] + (int) (Math.sin(angle) * distance * 0.6);
            fx.setComposite(AlphaComposite.SrcOver.derive((float) (0.2 + p * 0.6)));
            fx.fillRect(x, y, 2 + i % 3, 2 + i % 2);
        }
        fx.dispose();
    }

    public void drawProjectile(Graphics2D g, ClassAbilityVfx.Profile profile, int[] from, int[] to, double p) {
        if (!has(profile)) return;
        BufferedImage image = texture(profile);
        for (int trail = 3; trail >= 0; trail--) {
            double t = Math.max(0, p - trail * 0.07);
            Graphics2D head = (Graphics2D) g.create();
            head.translate(from[0] + (to[0] - from[0]) * t, from[1] + (to[1] - from[1]) * t);
            head.rotate(Math.atan2(to[1] - from[1], to[0] - from[0]));
            head.setComposite(AlphaComposite.SrcOver.derive(trail == 0 ? 0.95f : 0.18f / trail));
            int width = profile.family() == ClassAbilityVfx.Family.PIERCE ? 95 : 72;
            int height = profile.family() == ClassAbilityVfx.Family.PIERCE ? 36 : 60;
            head.drawImage(image, -width / 2, -height / 2, width, height, null);
            head.dispose();
        }
    }

    public void draw(Graphics2D g, String kind, int x, int y, double width, double height, double progress) {
        if (!has(kind) || progress < 0 || progress >= 1) return;
        // Fixed raster size keeps animation scaling out of AssetStore's size-keyed cache.
        BufferedImage image = assets.spriteFit(SPRITES.get(kind), 512, 512);
        Graphics2D fx = (Graphics2D) g.create();
        fx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        fx.translate(x - width / 2, y - height * 0.55);
        fx.scale(width / 512, height / 512);
        if (kind.contains("root") || kind.contains("thorn") || kind.contains("briar")) {
            drawGrowth(fx, image, progress, false);
            drawMotes(fx, progress, kind.equals("root_memory") ? new Color(158, 229, 166) : new Color(154, 117, 57), true);
        } else if (kind.contains("glacier")) {
            if (progress < 0.60) drawGrowth(fx, image, progress, true);
            else drawFragments(fx, image, progress, true, false);
            drawMotes(fx, progress, new Color(207, 239, 253), false);
        } else if (kind.equals("mend_unique") || kind.contains("hymn")) {
            drawHealing(fx, image, progress, kind.equals("seraphic_hymn"));
            drawMotes(fx, progress, kind.equals("grove_hymn") ? new Color(191, 236, 167) : new Color(255, 226, 164), true);
        } else {
            drawFragments(fx, image, progress, false, kind.equals("ley_detonation"));
            drawMotes(fx, progress, kind.equals("ley_detonation") ? new Color(205, 147, 249) : new Color(255, 171, 61), false);
        }
        fx.dispose();
    }

    /** Staggered columns reveal intact root/ice textures upwards from the ground. */
    private void drawGrowth(Graphics2D g, BufferedImage image, double p, boolean ice) {
        for (int column = 0; column < 16; column++) {
            double delay = Math.abs(column - 7.5) * 0.013 + (column % 3) * 0.017;
            double grow = ease((p - delay) / (ice ? 0.29 : 0.37));
            int visible = (int) Math.round(512 * grow);
            if (visible == 0) continue;
            double fade = 1 - ease((p - 0.64 - column % 4 * 0.025) / 0.27);
            int sway = ice ? 0 : (int) (Math.sin(p * 13 + column * 0.6) * 2 * grow);
            g.setComposite(AlphaComposite.SrcOver.derive((float) (fade * 0.94)));
            int left = column * 32, top = 512 - visible;
            // A soft stepped growth front avoids exposing a straight crop line.
            if (visible > 10) g.drawImage(image, left + sway, top + 10, left + 32 + sway, 512,
                    left, top + 10, left + 32, 512, null);
            g.setComposite(AlphaComposite.SrcOver.derive((float) (fade * 0.45)));
            g.drawImage(image, left + sway, top, left + 32 + sway, Math.min(512, top + 10),
                    left, top, left + 32, Math.min(512, top + 10), null);
        }
    }

    /** Light forms from the feet upwards, then flows through and leaves the recipient. */
    private void drawHealing(Graphics2D g, BufferedImage image, double p, boolean wings) {
        for (int band = 0; band < 32; band++) {
            double altitude = 1 - band / 31.0;
            double appear = ease((p - altitude * 0.28) / 0.18);
            double depart = ease((p - 0.54 - altitude * 0.13) / 0.30);
            double alpha = appear * (1 - depart) * (0.78 + 0.12 * Math.sin(p * 18 - altitude * 7));
            if (alpha <= 0) continue;
            int drift = (int) (depart * 65 + Math.sin(p * 8) * altitude * 4);
            int wave = (int) (Math.sin(p * 12 - altitude * 4) * (wings ? 2 : 5) * appear);
            g.setComposite(AlphaComposite.SrcOver.derive((float) alpha));
            int top = band * 16;
            g.drawImage(image, wave, top - drift, 512 + wave, top + 16 - drift,
                    0, top, 512, top + 16, null);
        }
    }

    /** Coherent arrival, then independently moving textured flame, rune or ice fragments. */
    private void drawFragments(Graphics2D g, BufferedImage image, double p, boolean ice, boolean arcane) {
        double arrive = ice ? 1 : ease(p / (arcane ? 0.30 : 0.18));
        double breakup = ease((p - (ice ? 0.60 : 0.43)) / (ice ? 0.40 : 0.57));
        // Keep the texture continuous during arrival; crossfade into irregular fragments.
        if (breakup < 0.35) {
            Graphics2D whole = (Graphics2D) g.create();
            double scale = 0.20 + arrive * 0.80;
            whole.setComposite(AlphaComposite.SrcOver.derive((float) ((ice ? 1 : Math.min(1, p / 0.04))
                    * (1 - breakup / 0.35) * 0.94)));
            whole.translate(256, 310);
            whole.scale(scale, scale);
            whole.drawImage(image, -256, -310, null);
            whole.dispose();
        }
        if (breakup == 0) return;
        for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) {
            int sx = col * 64, sy = row * 64;
            double dx = sx + 32 - 256, dy = sy + 32 - 310;
            double local = breakup * (0.5 + Math.floorMod(col * 7 + row * 13, 11) / 14.0);
            double spread = (ice ? 1 : 0.20 + arrive * 0.80) + local * (arcane ? 0.75 : 0.50);
            double alpha = Math.min(1, breakup / 0.20) * (1 - breakup) * 0.94;
            if (alpha <= 0) continue;
            Graphics2D piece = (Graphics2D) g.create();
            piece.setComposite(AlphaComposite.SrcOver.derive((float) alpha));
            piece.translate(256 + dx * spread, 310 + dy * spread + (ice ? local * local * 115 : -local * 90));
            piece.rotate((col % 2 == 0 ? 1 : -1) * local * (ice ? 0.8 : 0.25));
            double size = (0.20 + arrive * 0.80) * (1 - local * (ice ? 0.15 : 0.45));
            piece.scale(size, size);
            // Cut natural shard/ember silhouettes rather than displaying rectangular tiles.
            if (ice || arcane) {
                piece.clip(new java.awt.Polygon(new int[]{-12, -3, 18, 8, -15}, new int[]{12, -31, -9, 31, 23}, 5));
            } else {
                piece.clip(new java.awt.Polygon(new int[]{-24, -9, 1, 8, 25, 15, -6},
                        new int[]{10, -8, -32, -4, -17, 20, 30}, 7));
            }
            piece.drawImage(image, -32, -32, 32, 32, sx, sy, sx + 64, sy + 64, null);
            piece.dispose();
        }
    }

    /** Fine pixel fragments continue moving after the main impact has dispersed. */
    private void drawMotes(Graphics2D g, double p, Color color, boolean rising) {
        for (int i = 0; i < 26; i++) {
            double start = (i % 7) * 0.036;
            double life = (p - start) / 0.72;
            if (life <= 0 || life >= 1) continue;
            double angle = i * 2.399963;
            double radius = rising ? 95 + i % 4 * 22 : 20 + life * (110 + i % 4 * 30);
            int x = 256 + (int) (Math.cos(angle) * radius + Math.sin(life * 9 + i) * 12);
            int y = rising ? 470 - (int) (life * (290 + i % 3 * 45))
                    : 285 + (int) (Math.sin(angle) * radius * 0.6 - life * 65);
            g.setComposite(AlphaComposite.SrcOver.derive((float) (Math.sin(life * Math.PI) * 0.85)));
            g.setColor(color);
            int size = Math.max(2, (int) (6 * (1 - life)));
            g.fillRect(x, y, size, size + (rising ? 2 : 0));
        }
    }

    private static double ease(double value) {
        double p = Math.max(0, Math.min(1, value));
        return p * p * (3 - 2 * p);
    }

    public void drawFrozenShell(Graphics2D g, int x, int y, int width, int height, double formation) {
        String sprite = "fx_impact_frozen_shell_v2";
        if (!assets.hasSprite(sprite)) return;
        Graphics2D fx = (Graphics2D) g.create();
        fx.setComposite(AlphaComposite.SrcOver.derive(0.83f));
        fx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        fx.translate(x, y);
        fx.scale(width / 512.0, height / 512.0);
        BufferedImage image = assets.spriteFit(sprite, 512, 512);
        if (formation < 1) drawGrowth(fx, image, formation * 0.55, true);
        else fx.drawImage(image, 0, 0, null);
        fx.dispose();
    }

    /** Full affected formation, with margins for the outermost targets. */
    public static int[] groupBounds(List<int[]> targets) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int[] target : targets) {
            minX = Math.min(minX, target[0]); maxX = Math.max(maxX, target[0]);
            minY = Math.min(minY, target[1]); maxY = Math.max(maxY, target[1]);
        }
        if (targets.isEmpty()) return new int[]{0, 0, 0, 0};
        return new int[]{(minX + maxX) / 2, (minY + maxY) / 2,
                Math.max(300, maxX - minX + 260), Math.max(250, maxY - minY + 230)};
    }
}
