package com.alderfall.game.render.battle;

import com.alderfall.game.*;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class BattleVfxRenderer {
    private static final int MAX_PARTICLES = 22;

    private final AssetStore assets;
    private final Effects effects;

    public BattleVfxRenderer(AssetStore assets, Effects effects) {
        this.assets = assets;
        this.effects = effects;
    }

    public void drawBattleEffect(Graphics2D g, Battle battle, int panelX, int panelY, int panelW, int panelH) {
        BattleActionAnimation animation = battle.activeAnimation();
        if (animation == null && (battle.effectTimer <= 0 || battle.effectTarget == null)) {
            return;
        }
        Actor effectSource = animation == null ? battle.effectSource : animation.source;
        Actor effectTarget = animation == null ? battle.effectTarget : animation.target;
        String kind = animation == null ? battle.effectKind : animation.effectKind;
        kind = kind == null || kind.isBlank() ? "strike" : kind;
        if (effectTarget == null) {
            return;
        }
        int[] source = effects.battleActorCenter(battle, effectSource, panelX, panelY, panelW, panelH);
        int[] target = effects.battleActorCenter(battle, effectTarget, panelX, panelY, panelW, panelH);
        List<int[]> visualTargets = battleEffectTargetCenters(battle, animation, target, panelX, panelY, panelW, panelH);
        BattleActionAnimation.VisualMode visualMode = animation == null
                ? BattleActionAnimation.VisualMode.SINGLE
                : animation.visualMode;
        Graphics2D fx = (Graphics2D) g.create();
        fx.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (animation != null && animation.stage() == BattleActionAnimation.Stage.CAST) {
            drawCastBattleEffect(fx, source, animation.castProgress(), battle.enemies().contains(effectSource));
            fx.dispose();
            return;
        }
        float alpha;
        double phase;
        if (animation == null) {
            alpha = Math.max(0.08f, Math.min(0.85f, battle.effectTimer / 20.0f));
            phase = 1.0 - Math.max(0.0, Math.min(1.0, battle.effectTimer / 20.0));
        } else if (animation.stage() == BattleActionAnimation.Stage.IMPACT) {
            alpha = Math.max(0.08f, (float) (0.85 - animation.impactProgress() * 0.45));
            phase = 1.0;
        } else {
            alpha = 0.85f;
            phase = animation.travelProgress();
        }
        if (drawImageBattleEffect(fx, kind, source, target, visualTargets, visualMode, alpha, phase)) {
            drawBattleEffectParticles(fx, kind, source, target, alpha, phase);
            fx.dispose();
            return;
        }
        fx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        switch (kind) {
            case "heal", "regeneration" -> {
                fx.setColor(new Color(126, 232, 154));
                fx.drawOval(target[0] - 34, target[1] - 42, 68, 68);
                fx.drawOval(target[0] - 22, target[1] - 30, 44, 44);
            }
            case "shield", "ward" -> {
                fx.setColor(new Color(142, 191, 255));
                fx.drawArc(target[0] - 42, target[1] - 50, 84, 94, 205, 130);
                fx.drawArc(target[0] - 32, target[1] - 40, 64, 74, 210, 120);
            }
            case "fire", "burn" -> {
                fx.setColor(new Color(255, 132, 65));
                fx.drawLine(source[0], source[1], target[0], target[1]);
                fx.setColor(new Color(255, 206, 91));
                fx.fillOval(target[0] - 22, target[1] - 22, 44, 44);
            }
            case "frost" -> {
                fx.setColor(new Color(150, 222, 255));
                fx.drawLine(source[0], source[1], target[0], target[1]);
                fx.drawLine(target[0] - 26, target[1], target[0] + 26, target[1]);
                fx.drawLine(target[0], target[1] - 26, target[0], target[1] + 26);
            }
            case "poison", "acid", "web", "thorn" -> {
                fx.setColor(new Color(130, 220, 108));
                fx.drawLine(source[0], source[1], target[0], target[1]);
                fx.drawOval(target[0] - 28, target[1] - 20, 22, 22);
                fx.drawOval(target[0] + 6, target[1] - 32, 28, 28);
            }
            case "volley", "pierce" -> {
                fx.setColor(new Color(246, 224, 151));
                for (int i = -1; i <= 1; i++) {
                    fx.drawLine(source[0], source[1] + i * 10, target[0], target[1] + i * 8);
                }
            }
            case "slash", "cleave", "fang" -> {
                fx.setColor(new Color(255, 240, 206));
                fx.drawArc(target[0] - 48, target[1] - 38, 96, 70, 25, 120);
                fx.drawArc(target[0] - 34, target[1] - 28, 68, 52, 25, 120);
            }
            case "shadow", "sonic", "bone", "dust", "howl" -> {
                fx.setColor(new Color(180, 154, 220));
                fx.drawOval(target[0] - 36, target[1] - 30, 72, 60);
                fx.drawLine(source[0], source[1], target[0], target[1]);
            }
            default -> {
                fx.setColor(new Color(255, 240, 206));
                fx.drawLine(source[0], source[1], target[0], target[1]);
                fx.drawArc(target[0] - 38, target[1] - 28, 76, 52, 25, 120);
            }
        }
        drawBattleEffectParticles(fx, kind, source, target, alpha, phase);
        fx.dispose();
    }

    private List<int[]> battleEffectTargetCenters(Battle battle, BattleActionAnimation animation, int[] fallbackTarget,
                                                  int panelX, int panelY, int panelW, int panelH) {
        ArrayList<int[]> centers = new ArrayList<>();
        if (animation != null) {
            for (Actor actor : animation.targets) {
                if (actor != null) {
                    centers.add(effects.battleActorCenter(battle, actor, panelX, panelY, panelW, panelH));
                }
            }
        }
        if (centers.isEmpty()) {
            centers.add(fallbackTarget);
        }
        return centers;
    }

    private void drawBattleEffectParticles(Graphics2D g, String kind, int[] source, int[] target, float alpha, double phase) {
        if (!battleEffectHasParticles(kind)) {
            return;
        }
        int count = Math.min(MAX_PARTICLES, battleParticleCount(kind));
        Color color = battleParticleColor(kind);
        boolean travel = battleEffectUsesTravelParticles(kind);
        int seed = Math.abs(kind.hashCode() * 31 + source[0] * 17 + target[1] * 23);
        Composite oldComposite = g.getComposite();
        for (int i = 0; i < count; i++) {
            double life = Math.floorMod(effects.frame() * 3 + seed + i * 19, 72) / 72.0;
            double angle = seed * 0.003 + i * 2.399 + effects.frame() * (0.045 + i * 0.002);
            int cx;
            int cy;
            if (travel && phase < 0.98) {
                double p = Math.max(0.05, Math.min(0.95, phase + (i - count / 2.0) * 0.014));
                cx = (int) Math.round(source[0] + (target[0] - source[0]) * p);
                cy = (int) Math.round(source[1] + (target[1] - source[1]) * p);
                cx += (int) Math.round(Math.cos(angle) * (7 + i % 3 * 3));
                cy += (int) Math.round(Math.sin(angle * 1.3) * (6 + i % 4 * 2));
            } else {
                double radius = 18 + i % 5 * 7 + life * 26;
                cx = target[0] + (int) Math.round(Math.cos(angle) * radius);
                cy = target[1] + (int) Math.round(Math.sin(angle * 1.18) * radius * 0.62 - life * 12);
            }
            int size = Math.max(2, effects.scaled(3 + i % 3) - (int) Math.round(life * effects.scaled(2)));
            float particleAlpha = (float) Math.max(0.0, Math.min(0.62, alpha * (1.0 - life) * battleParticleAlpha(kind)));
            g.setComposite(AlphaComposite.SrcOver.derive(particleAlpha));
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 230));
            if ("lightning".equals(kind) && i < 5) {
                int ex = cx + (int) Math.round(Math.cos(angle + 1.1) * effects.scaled(10));
                int ey = cy + (int) Math.round(Math.sin(angle + 1.1) * effects.scaled(8));
                g.drawLine(cx, cy, ex, ey);
            } else if ("water".equals(kind)) {
                g.drawArc(cx - size, cy - size / 2, size * 2, size, (int) Math.round(angle * 80), 155);
            } else if ("nature".equals(kind) || "thorn".equals(kind)) {
                g.fillOval(cx - size, cy - size / 2, size + 2, Math.max(2, size / 2 + 1));
                g.drawLine(cx, cy, cx + (int) Math.round(Math.cos(angle) * size * 1.4), cy + (int) Math.round(Math.sin(angle) * size));
            } else if ("slash".equals(kind) || "cleave".equals(kind) || "claw".equals(kind)
                    || "fang".equals(kind) || "volley".equals(kind) || "pierce".equals(kind)) {
                int ex = cx + (int) Math.round(Math.cos(angle) * effects.scaled(12 + i % 4 * 2));
                int ey = cy + (int) Math.round(Math.sin(angle) * effects.scaled(5 + i % 3));
                g.drawLine(cx, cy, ex, ey);
            } else if ("holy".equals(kind) || "radiant".equals(kind) || "prismatic".equals(kind) || "chaos".equals(kind)) {
                int ray = effects.scaled(3 + i % 3);
                g.drawLine(cx - ray, cy, cx + ray, cy);
                g.drawLine(cx, cy - ray, cx, cy + ray);
            } else {
                g.fillOval(cx - size / 2, cy - size / 2, size, size);
            }
        }
        g.setComposite(oldComposite);
    }

    private boolean battleEffectHasParticles(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            return true;
        }
        return switch (kind) {
            case "heal", "regeneration", "shield", "ward", "fire", "burn", "ember", "spark", "radiant",
                    "holy", "lightning", "arcane", "rune", "void", "dark", "frost", "water", "poison",
                    "acid", "web", "thorn", "nature", "shadow", "dust", "howl", "item", "prismatic",
                    "chaos", "slash", "cleave", "claw", "fang", "volley", "pierce", "strike", "impact",
                    "bash" -> true;
            default -> false;
        };
    }

    private boolean battleEffectUsesTravelParticles(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            return isGeneratedProjectileEffect(kind);
        }
        return switch (kind) {
            case "fire", "burn", "ember", "spark", "radiant", "holy", "lightning", "arcane", "rune",
                    "void", "dark", "frost", "water", "poison", "acid", "web", "thorn", "nature",
                    "shadow", "item", "prismatic", "chaos", "volley", "pierce" -> true;
            default -> false;
        };
    }

    private int battleParticleCount(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            return isGeneratedVolleyEffect(kind) ? 18 : 14;
        }
        return switch (kind) {
            case "prismatic", "chaos" -> 18;
            case "lightning", "fire", "burn", "ember", "void", "dark", "arcane", "rune", "shadow",
                    "water", "nature" -> 14;
            case "heal", "regeneration", "holy", "radiant", "spark", "item", "volley", "pierce" -> 12;
            case "frost", "poison", "acid", "web", "thorn", "slash", "cleave", "claw", "fang" -> 10;
            case "strike", "impact", "bash" -> 7;
            default -> 6;
        };
    }

    private float battleParticleAlpha(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            return 0.58f;
        }
        return switch (kind) {
            case "shadow", "void", "dark", "dust" -> 0.44f;
            case "lightning", "fire", "burn", "ember", "holy", "radiant", "prismatic", "chaos" -> 0.60f;
            case "slash", "cleave", "claw", "fang", "volley", "pierce" -> 0.56f;
            default -> 0.50f;
        };
    }

    private Color battleParticleColor(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            if (kind.contains("frost") || kind.contains("glacier") || kind.contains("water")
                    || kind.contains("stormline")) {
                return new Color(150, 222, 255);
            }
            if (kind.contains("fire") || kind.contains("inferno") || kind.contains("noon")
                    || kind.contains("phoenix")) {
                return new Color(255, 171, 75);
            }
            if (kind.contains("thorn") || kind.contains("briar") || kind.contains("primeval")) {
                return new Color(130, 220, 108);
            }
            if (kind.contains("shot") || kind.contains("arrow")) {
                return new Color(255, 226, 116);
            }
            if (kind.contains("chaos") || kind.contains("ley")) {
                return new Color(255, 130, 205);
            }
            return new Color(220, 210, 235);
        }
        return switch (kind) {
            case "heal", "regeneration" -> new Color(134, 241, 166);
            case "shield", "ward", "frost" -> new Color(150, 222, 255);
            case "water" -> new Color(100, 190, 255);
            case "fire", "burn", "ember" -> new Color(255, 171, 75);
            case "spark", "radiant", "item" -> new Color(255, 226, 116);
            case "holy" -> new Color(255, 245, 180);
            case "lightning" -> new Color(154, 211, 255);
            case "arcane", "rune" -> new Color(174, 130, 255);
            case "prismatic", "chaos" -> new Color(255, 130, 205);
            case "void", "dark", "shadow" -> new Color(143, 106, 218);
            case "poison", "acid", "web", "thorn", "nature" -> new Color(130, 220, 108);
            case "slash", "cleave", "claw", "fang", "volley", "pierce" -> new Color(255, 240, 206);
            case "strike", "impact", "bash" -> new Color(255, 210, 142);
            default -> new Color(220, 210, 235);
        };
    }

    private void drawCastBattleEffect(Graphics2D g, int[] source, double progress, boolean hostile) {
        float alpha = (float) (0.25 + Math.sin(progress * Math.PI) * 0.5);
        int radius = 30 + (int) Math.round(progress * 18);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.05f, Math.min(0.8f, alpha))));
        Color ring = hostile ? new Color(195, 124, 255) : new Color(126, 205, 255);
        Color spark = hostile ? new Color(255, 130, 205) : new Color(255, 236, 160);
        g.setColor(ring);
        g.drawOval(source[0] - radius, source[1] - radius, radius * 2, radius * 2);
        g.drawOval(source[0] - radius / 2, source[1] - radius / 2, radius, radius);
        g.drawLine(source[0] - radius - 6, source[1], source[0] - radius / 2, source[1]);
        g.drawLine(source[0] + radius / 2, source[1], source[0] + radius + 6, source[1]);
        g.setColor(spark);
        for (int i = 0; i < 8; i++) {
            double angle = effects.frame() * 0.08 + i * Math.PI / 4.0;
            int x = source[0] + (int) Math.round(Math.cos(angle) * (radius + 6));
            int y = source[1] + (int) Math.round(Math.sin(angle) * (radius * 0.68 + 4));
            int ray = 3 + i % 2;
            g.drawLine(x - ray, y, x + ray, y);
            g.drawLine(x, y - ray, x, y + ray);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private boolean drawImageBattleEffect(Graphics2D g, String kind, int[] source, int[] target, List<int[]> visualTargets,
                                          BattleActionAnimation.VisualMode visualMode, float alpha, double phase) {
        String sprite = effectSpriteName(kind);
        if (sprite == null) {
            return false;
        }
        double angle = effectTravelAngle(source, target);
        phase = Math.max(0.0, Math.min(1.0, phase));
        double pulse = 0.92 + Math.sin((effects.frame() + phase * 20.0) * 0.32) * 0.08;
        if (visualTargets != null && visualTargets.size() > 1) {
            if (visualMode == BattleActionAnimation.VisualMode.CHAIN) {
                drawChainBattleEffect(g, sprite, kind, source, visualTargets, alpha, phase, pulse);
                return true;
            }
            if (visualMode == BattleActionAnimation.VisualMode.MULTI) {
                drawMultiProjectileBattleEffect(g, sprite, kind, source, visualTargets, alpha, phase, pulse);
                return true;
            }
            if (visualMode == BattleActionAnimation.VisualMode.AOE) {
                drawAoeBattleEffect(g, sprite, kind, source, visualTargets, alpha, phase, pulse);
                return true;
            }
        }
        if (kind.endsWith("_unique")) {
            if (isGeneratedHealEffect(kind)) {
                drawEffectSprite(g, sprite, target[0], target[1] - 4, effectSize(142, pulse), effectSize(142, pulse), 0.0, alpha);
                return true;
            }
            if (isGeneratedWardEffect(kind)) {
                drawEffectSprite(g, sprite, target[0] + 6, target[1] - 4, effectSize(148, pulse), effectSize(162, pulse), 0.0, alpha);
                return true;
            }
            if (isGeneratedMeleeEffect(kind)) {
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(164, pulse), effectSize(124, pulse),
                        crescentImpactAngle(source, target, 0.16), alpha);
                return true;
            }
            if (isGeneratedProjectileEffect(kind)) {
                double travelPhase = easedTravelPhase(phase);
                if (isGeneratedVolleyEffect(kind)) {
                    for (int i = -1; i <= 1; i++) {
                        int[] offsetSource = new int[]{source[0], source[1] + i * 12};
                        int[] offsetTarget = new int[]{target[0], target[1] + i * 10};
                        double offsetAngle = effectTravelAngle(offsetSource, offsetTarget);
                        drawTravelingSprite(g, sprite, offsetSource, offsetTarget, offsetAngle, travelPhase,
                                136, 86, alpha * (i == 0 ? 0.92f : 0.66f));
                    }
                } else {
                    drawTravelingSprite(g, sprite, source, target, angle, travelPhase,
                            generatedProjectileWidth(kind), generatedProjectileHeight(kind), alpha * 0.92f);
                }
                if (phase >= 0.98) {
                    drawEffectSprite(g, sprite, target[0], target[1],
                            effectSize(generatedImpactWidth(kind), pulse),
                            effectSize(generatedImpactHeight(kind), pulse),
                            correctedEffectAngle(sprite, angle), alpha * 0.52f);
                }
                return true;
            }
            drawEffectSprite(g, sprite, target[0], target[1], effectSize(154, pulse), effectSize(134, pulse), 0.0, alpha);
            return true;
        }
        switch (kind) {
            case "heal", "regeneration", "grove_hymn", "root_memory", "seraphic_hymn" -> {
                drawEffectSprite(g, sprite, target[0], target[1] - 4, effectSize(134, pulse), effectSize(134, pulse), 0.0, alpha);
                return true;
            }
            case "shield", "ward" -> {
                drawEffectSprite(g, sprite, target[0] + 10, target[1] - 4, effectSize(128, pulse), effectSize(164, pulse), 0.0, alpha);
                return true;
            }
            case "slash", "fang" -> {
                double slashAngle = crescentImpactAngle(source, target, 0.2);
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(154, pulse), effectSize(120, pulse), slashAngle, alpha);
                return true;
            }
            case "claw" -> {
                double clawAngle = crescentImpactAngle(source, target, 0.1);
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(150, pulse), effectSize(136, pulse), clawAngle, alpha);
                return true;
            }
            case "strike", "impact", "bash" -> {
                drawEffectSprite(g, sprite, target[0], target[1] + 6, effectSize(138, pulse), effectSize(118, pulse), 0.0, alpha);
                return true;
            }
            case "cleave" -> {
                double slashAngle = crescentImpactAngle(source, target, 0.1);
                drawEffectSprite(g, sprite, target[0], target[1] - 2, effectSize(178, pulse), effectSize(126, pulse), slashAngle, alpha);
                return true;
            }
            case "shadow" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 116, 116, alpha);
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(132, pulse), effectSize(132, pulse), 0.0, alpha * 0.62f);
                return true;
            }
            case "sonic" -> {
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(176, pulse), effectSize(136, pulse), 0.0, alpha);
                return true;
            }
            case "bone" -> {
                drawEffectSprite(g, sprite, target[0], target[1] + 10, effectSize(142, pulse), effectSize(128, pulse), 0.0, alpha);
                return true;
            }
            case "dust", "howl" -> {
                drawEffectSprite(g, sprite, target[0], target[1] + 12, effectSize(150, pulse), effectSize(116, pulse), 0.0, alpha);
                return true;
            }
            case "spark", "radiant", "item" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 92, 72, alpha * 0.62f);
                drawEffectSprite(g, sprite, target[0], target[1] + 4, effectSize(136, pulse), effectSize(120, pulse), 0.0, alpha);
                return true;
            }
            case "holy" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 132, 76, alpha);
                drawEffectSprite(g, sprite, target[0], target[1] + 2, 112, 96, correctedEffectAngle(sprite, angle), alpha * 0.58f);
                return true;
            }
            case "lightning" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 132, 86, alpha);
                drawEffectSprite(g, sprite, target[0], target[1], 112, 82, correctedEffectAngle(sprite, angle), alpha * 0.62f);
                return true;
            }
            case "arcane", "rune", "ley_detonation" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 108, 82, alpha * 0.68f);
                drawEffectSprite(g, sprite, target[0], target[1] + 6, effectSize(156, pulse), effectSize(112, pulse), 0.0, alpha * 0.86f);
                return true;
            }
            case "prismatic", "chaos", "worldsplitter" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 132, 92, alpha * 0.76f);
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(184, pulse), effectSize(152, pulse), 0.0, alpha);
                drawEffectSprite(g, "fx_spark", target[0], target[1] - 10, effectSize(118, pulse), effectSize(118, pulse), 0.0, alpha * 0.32f);
                return true;
            }
            case "ember" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 132, 88, alpha);
                drawEffectSprite(g, sprite, target[0], target[1], 96, 72, correctedEffectAngle(sprite, angle), alpha * 0.58f);
                return true;
            }
            case "void", "dark", "execution_mark" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 112, 92, alpha * 0.72f);
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(144, pulse), effectSize(128, pulse), 0.0, alpha * 0.88f);
                return true;
            }
            case "water" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 126, 84, alpha * 0.70f);
                drawEffectSprite(g, "fx_heal", target[0], target[1] + 4, effectSize(122, pulse), effectSize(96, pulse), 0.0, alpha * 0.44f);
                drawEffectSprite(g, sprite, target[0], target[1], 104, 76, correctedEffectAngle(sprite, angle), alpha * 0.52f);
                return true;
            }
            case "volley", "pierce" -> {
                for (int i = -1; i <= 1; i++) {
                    int[] offsetSource = new int[]{source[0], source[1] + i * 10};
                    int[] offsetTarget = new int[]{target[0], target[1] + i * 8};
                    double offsetAngle = effectTravelAngle(offsetSource, offsetTarget);
                    drawTravelingSprite(g, sprite, offsetSource, offsetTarget, offsetAngle, phase, 118, 58, alpha * (i == 0 ? 1.0f : 0.72f));
                }
                return true;
            }
            case "nature" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 132, 88, alpha * 0.78f);
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(142, pulse), effectSize(116, pulse), correctedEffectAngle(sprite, angle), alpha * 0.68f);
                drawEffectSprite(g, "fx_web", target[0], target[1] + 4, effectSize(116, pulse), effectSize(96, pulse), 0.0, alpha * 0.30f);
                return true;
            }
            case "fire", "burn", "frost", "poison", "acid", "web", "thorn" -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 126, 88, alpha);
                drawEffectSprite(g, sprite, target[0], target[1], 96, 78, correctedEffectAngle(sprite, angle), alpha * 0.54f);
                return true;
            }
            default -> {
                drawTravelingSprite(g, sprite, source, target, angle, phase, 124, 78, alpha);
                drawEffectSprite(g, "fx_slash", target[0], target[1], 112, 82, correctedEffectAngle("fx_slash", angle), alpha * 0.5f);
                return true;
            }
        }
    }

    private int effectSize(int base, double multiplier) {
        return Math.max(1, (int) Math.round(base * multiplier));
    }

    private double crescentImpactAngle(int[] source, int[] target, double lift) {
        return source[0] > target[0] ? lift : Math.PI + lift;
    }

    private void drawChainBattleEffect(Graphics2D g, String sprite, String kind, int[] source, List<int[]> targets,
                                       float alpha, double phase, double pulse) {
        int hops = Math.max(1, targets.size());
        double progress = easedTravelPhase(phase) * hops;
        int[] from = source;
        for (int i = 0; i < targets.size(); i++) {
            int[] to = targets.get(i);
            double segment = Math.max(0.0, Math.min(1.0, progress - i));
            double angle = effectTravelAngle(from, to);
            if (segment > 0.0 && segment < 1.0) {
                drawTravelingSprite(g, sprite, from, to, angle, segment,
                        generatedProjectileWidth(kind), generatedProjectileHeight(kind), alpha * 0.95f);
            } else if (segment >= 1.0) {
                drawEffectSprite(g, sprite, to[0], to[1], effectSize(92, pulse), effectSize(72, pulse),
                        correctedEffectAngle(sprite, angle), alpha * 0.34f);
            }
            from = to;
        }
    }

    private void drawMultiProjectileBattleEffect(Graphics2D g, String sprite, String kind, int[] source, List<int[]> targets,
                                                 float alpha, double phase, double pulse) {
        double travelPhase = easedTravelPhase(phase);
        for (int i = 0; i < targets.size(); i++) {
            int[] target = targets.get(i);
            int offset = i - targets.size() / 2;
            int[] offsetSource = new int[]{source[0], source[1] + offset * 10};
            double angle = effectTravelAngle(offsetSource, target);
            drawTravelingSprite(g, sprite, offsetSource, target, angle, travelPhase,
                    generatedProjectileWidth(kind), generatedProjectileHeight(kind), alpha * 0.88f);
            if (phase >= 0.98) {
                drawEffectSprite(g, sprite, target[0], target[1], effectSize(96, pulse), effectSize(76, pulse),
                        correctedEffectAngle(sprite, angle), alpha * 0.42f);
            }
        }
    }

    private void drawAoeBattleEffect(Graphics2D g, String sprite, String kind, int[] source, List<int[]> targets,
                                     float alpha, double phase, double pulse) {
        int[] center = targetGroupCenter(targets);
        double angle = effectTravelAngle(source, center);
        double travelPhase = easedTravelPhase(Math.min(0.9, phase * 1.25));
        if (phase < 0.92) {
            drawTravelingSprite(g, sprite, source, center, angle, travelPhase,
                    generatedProjectileWidth(kind) + 16, generatedProjectileHeight(kind) + 12, alpha * 0.82f);
        }
        double impactProgress = Math.max(0.0, (phase - 0.58) / 0.42);
        if (impactProgress > 0.0 || phase >= 0.98) {
            double blast = 0.72 + impactProgress * 0.48;
            drawEffectSprite(g, sprite, center[0], center[1], effectSize(220, pulse * blast),
                    effectSize(172, pulse * blast), correctedEffectAngle(sprite, angle), alpha * (float) (0.56 + impactProgress * 0.28));
            drawEffectSprite(g, "fx_impact", center[0], center[1] + 8, effectSize(238, blast),
                    effectSize(152, blast), 0.0, alpha * 0.46f);
        }
    }

    private int[] targetGroupCenter(List<int[]> targets) {
        if (targets == null || targets.isEmpty()) {
            return new int[]{0, 0};
        }
        int x = 0;
        int y = 0;
        for (int[] target : targets) {
            x += target[0];
            y += target[1];
        }
        return new int[]{x / targets.size(), y / targets.size()};
    }

    private void drawTravelingSprite(Graphics2D g, String sprite, int[] source, int[] target, double angle, double phase, int width, int height, float alpha) {
        double travel = 0.14 + phase * 0.74;
        int x = (int) Math.round(source[0] + (target[0] - source[0]) * travel);
        int y = (int) Math.round(source[1] + (target[1] - source[1]) * travel);
        double drawAngle = correctedEffectAngle(sprite, angle);
        drawEffectSprite(g, sprite, x, y, width, height, drawAngle, alpha);
        double distance = Math.hypot(target[0] - source[0], target[1] - source[1]);
        double trailStep = Math.max(0.06, Math.min(0.16, 42.0 / Math.max(160.0, distance)));
        for (int i = 1; i <= 3; i++) {
            double trail = Math.max(0.04, travel - trailStep * i);
            int trailX = (int) Math.round(source[0] + (target[0] - source[0]) * trail);
            int trailY = (int) Math.round(source[1] + (target[1] - source[1]) * trail);
            double scale = 0.78 - i * 0.12;
            drawEffectSprite(g, sprite, trailX, trailY, width * scale, height * scale, drawAngle, alpha * (0.26f / i));
        }
    }

    private double easedTravelPhase(double phase) {
        phase = Math.max(0.0, Math.min(1.0, phase));
        return phase * phase * (3.0 - 2.0 * phase);
    }

    private double correctedEffectAngle(String sprite, double travelAngle) {
        return travelAngle - nativeEffectForwardAngle(sprite);
    }

    private double effectTravelAngle(int[] source, int[] target) {
        return Math.atan2(target[1] - source[1], target[0] - source[0]);
    }

    private double nativeEffectForwardAngle(String sprite) {
        return switch (sprite) {
            // Directional sprites are calibrated to the angle their "head" points at rest.
            case "fx_arrow" -> Math.toRadians(-30);
            case "fx_holy" -> Math.toRadians(-42);
            case "fx_lightning" -> Math.toRadians(-36);
            case "fx_web" -> Math.toRadians(-22);
            case "fx_thorn" -> Math.toRadians(-28);
            case "fx_fire", "fx_ember", "fx_poison" -> Math.toRadians(145);
            case "fx_frost" -> Math.toRadians(-38);
            case "fx_slash" -> Math.PI;
            case "fx_firebolt_unique", "fx_frost_lance_unique", "fx_water_jet_unique",
                    "fx_piercing_shot_unique", "fx_snare_arrow_unique", "fx_radiant_bolt_unique",
                    "fx_hemostatic_strike_unique", "fx_shield_ram_unique", "fx_thorn_lash_unique",
                    "fx_inferno_script_unique", "fx_glacier_prison_unique", "fx_ley_reversal_unique",
                    "fx_chaos_bloom_unique", "fx_heartline_shot_unique", "fx_caustic_flask_unique",
                    "fx_briar_tempest_unique", "fx_noonflare_unique", "fx_titan_hammer_unique" -> Math.toRadians(140);
            case "fx_stormline_volley_unique" -> Math.toRadians(-28);
            default -> 0.0;
        };
    }

    private void drawEffectSprite(Graphics2D g, String sprite, int centerX, int centerY, double width, double height, double angle, float alpha) {
        int drawW = Math.max(1, (int) Math.round(width));
        int drawH = Math.max(1, (int) Math.round(height));
        int frameCount = assets.effectSpriteFrameCount(sprite);
        double animationCursor = effects.frame() * 0.62 + Math.floorMod(sprite.hashCode(), 6);
        int animationFrame = (int) Math.floor(animationCursor);
        double frameBlend = animationCursor - Math.floor(animationCursor);
        BufferedImage image = assets.effectSprite(sprite, drawW, drawH, animationFrame);
        Graphics2D spriteG = (Graphics2D) g.create();
        float clampedAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        spriteG.translate(centerX, centerY);
        spriteG.rotate(angle);
        if (frameCount > 1 && frameBlend > 0.02) {
            float firstAlpha = (float) (clampedAlpha * (1.0 - frameBlend * 0.55));
            spriteG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.0f, Math.min(1.0f, firstAlpha))));
            spriteG.drawImage(image, -drawW / 2, -drawH / 2, drawW, drawH, null);
            BufferedImage nextImage = assets.effectSprite(sprite, drawW, drawH, animationFrame + 1);
            float nextAlpha = (float) (clampedAlpha * frameBlend * 0.55);
            spriteG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.0f, Math.min(1.0f, nextAlpha))));
            spriteG.drawImage(nextImage, -drawW / 2, -drawH / 2, drawW, drawH, null);
            spriteG.dispose();
            return;
        }
        spriteG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampedAlpha));
        spriteG.drawImage(image, -drawW / 2, -drawH / 2, drawW, drawH, null);
        spriteG.dispose();
    }

    private boolean isGeneratedProjectileEffect(String kind) {
        return switch (kind) {
            case "firebolt_unique", "frost_lance_unique", "water_jet_unique",
                    "piercing_shot_unique", "snare_arrow_unique", "radiant_bolt_unique",
                    "hemostatic_strike_unique", "shield_ram_unique", "thorn_lash_unique",
                    "inferno_script_unique", "glacier_prison_unique", "ley_reversal_unique",
                    "chaos_bloom_unique", "heartline_shot_unique", "stormline_volley_unique",
                    "caustic_flask_unique", "briar_tempest_unique", "noonflare_unique",
                    "titan_hammer_unique" -> true;
            default -> false;
        };
    }

    private boolean isGeneratedVolleyEffect(String kind) {
        return "stormline_volley_unique".equals(kind);
    }

    private boolean isGeneratedMeleeEffect(String kind) {
        return switch (kind) {
            case "dual_cut_unique", "final_challenge_unique" -> true;
            default -> false;
        };
    }

    private int generatedProjectileWidth(String kind) {
        return switch (kind) {
            case "stormline_volley_unique" -> 142;
            case "heartline_shot_unique", "piercing_shot_unique", "snare_arrow_unique" -> 128;
            case "titan_hammer_unique", "chaos_bloom_unique", "briar_tempest_unique" -> 148;
            default -> 136;
        };
    }

    private int generatedProjectileHeight(String kind) {
        return switch (kind) {
            case "heartline_shot_unique", "piercing_shot_unique", "snare_arrow_unique" -> 72;
            case "titan_hammer_unique", "chaos_bloom_unique", "briar_tempest_unique" -> 106;
            default -> 92;
        };
    }

    private int generatedImpactWidth(String kind) {
        return switch (kind) {
            case "titan_hammer_unique", "chaos_bloom_unique", "briar_tempest_unique" -> 178;
            default -> 146;
        };
    }

    private int generatedImpactHeight(String kind) {
        return switch (kind) {
            case "titan_hammer_unique", "chaos_bloom_unique", "briar_tempest_unique" -> 142;
            default -> 116;
        };
    }

    public String effectSpriteName(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            return "fx_" + kind;
        }
        return switch (kind) {
            case "heal", "regeneration" -> "fx_heal";
            case "grove_hymn" -> "fx_grove_hymn";
            case "root_memory" -> "fx_root_memory";
            case "seraphic_hymn" -> "fx_seraphic_hymn";
            case "shield", "ward" -> "fx_shield";
            case "fire", "burn" -> "fx_fire";
            case "ember" -> "fx_ember";
            case "water" -> "fx_frost";
            case "spark", "radiant", "item" -> "fx_spark";
            case "holy" -> "fx_holy";
            case "lightning" -> "fx_lightning";
            case "arcane", "rune" -> "fx_arcane";
            case "ley_detonation" -> "fx_ley_detonation";
            case "prismatic", "chaos" -> "fx_prismatic";
            case "worldsplitter" -> "fx_worldsplitter";
            case "void", "dark" -> "fx_void_hit";
            case "execution_mark" -> "fx_execution_mark";
            case "frost" -> "fx_frost";
            case "poison", "acid" -> "fx_poison";
            case "web" -> "fx_web";
            case "thorn", "nature" -> "fx_thorn";
            case "volley", "pierce" -> "fx_arrow";
            case "slash" -> "fx_slash";
            case "fang", "claw" -> "fx_claw";
            case "strike", "impact", "bash" -> "fx_impact";
            case "cleave" -> "fx_cleave";
            case "shadow" -> "fx_shadow";
            case "sonic" -> "fx_sonic";
            case "bone" -> "fx_bone";
            case "dust", "howl" -> "fx_dust";
            default -> null;
        };
    }

    public String effectSoundName(String kind) {
        if (kind != null && kind.endsWith("_unique")) {
            if (isGeneratedHealEffect(kind)) {
                return "heal";
            }
            if (isGeneratedWardEffect(kind)) {
                return "shield";
            }
            if (kind.contains("arrow") || kind.contains("shot") || kind.contains("volley")) {
                return "arrow";
            }
            if (kind.contains("fire") || kind.contains("inferno") || kind.contains("noon")) {
                return "fire";
            }
            if (kind.contains("frost") || kind.contains("glacier")) {
                return "frost";
            }
            return "impact";
        }
        return switch (kind) {
            case "heal", "regeneration", "grove_hymn", "root_memory", "seraphic_hymn" -> "heal";
            case "shield", "ward" -> "shield";
            case "fire", "burn", "ember" -> "fire";
            case "frost" -> "frost";
            case "water" -> "heal";
            case "poison", "acid", "web", "thorn", "nature" -> "poison";
            case "volley", "pierce" -> "arrow";
            case "slash", "fang", "strike", "cleave", "claw", "impact", "bash" -> "slash";
            case "shadow", "sonic", "bone", "dust", "howl", "spark", "radiant", "holy", "lightning",
                    "arcane", "rune", "ley_detonation", "void", "dark", "execution_mark",
                    "chaos", "prismatic", "worldsplitter" -> "impact";
            default -> "impact";
        };
    }

    private boolean isGeneratedHealEffect(String kind) {
        return switch (kind) {
            case "mend_unique", "shadow_salve_unique", "mercy_wellspring_unique",
                    "panacea_toss_unique", "primeval_bloom_unique" -> true;
            default -> false;
        };
    }

    private boolean isGeneratedWardEffect(String kind) {
        return switch (kind) {
            case "smoke_veil_unique", "stone_guard_unique", "aegis_circle_unique",
                    "citadel_protocol_unique", "halo_bastion_unique" -> true;
            default -> false;
        };
    }


    public interface Effects {
        int frame();

        int scaled(int value);

        int[] battleActorCenter(Battle battle, Actor actor, int panelX, int panelY, int panelW, int panelH);
    }
}
