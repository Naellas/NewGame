package com.alderfall.game;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Set;

final class BattleRenderer {
    private static final Set<String> PHYSICAL_CLASS_NAMES = Set.of(
            "Knight",
            "Rogue",
            "Ironwall",
            "Bladedancer",
            "Veilrunner",
            "Sunwarden",
            "Stonebreaker",
            "Nightblade"
    );
    private static final Set<String> RANGED_CLASS_NAMES = Set.of("Ranger");
    private static final Set<String> MAGIC_CLASS_NAMES = Set.of(
            "Mage",
            "Cleric",
            "Battle Medic",
            "Wildspeaker",
            "Thornbinder",
            "Grovekeeper"
    );

    private final AssetStore assets;
    private final java.util.Map<BufferedImage, BufferedImage> hitMasks = new java.util.WeakHashMap<>();

    BattleRenderer(AssetStore assets) {
        this.assets = assets;
    }

    void drawBackdrop(Graphics2D g, Battle battle, int x, int y, int w, int h) {
        String painted = battle.backdrop + "_painted";
        String backdrop = assets.hasSprite(painted) ? painted : battle.backdrop;
        g.drawImage(assets.cover(backdrop, w, h), x, y, null);
        g.setPaint(new GradientPaint(x, y, new Color(8, 10, 16, 24), x, y + h, new Color(8, 10, 16, 110)));
        g.fillRect(x, y, w, h);
        g.setPaint(null);
        g.setColor(new Color(20, 18, 18, 92));
        g.fillRect(x, y + h - 180, w, 180);
    }

    BattleActorPose actorPose(Battle battle, Actor actor, boolean enemySide) {
        BattleActionAnimation animation = battle.activeAnimation();
        if (animation == null || actor == null || !animation.involves(actor)) {
            return BattleActorPose.REST;
        }
        int direction = enemySide ? -1 : 1;
        boolean source = actor == animation.source;
        String kind = animation.effectKind == null ? "strike" : animation.effectKind;
        if (source) {
            return sourcePose(animation, animation.visualProfile() == null ? kind : animation.visualProfile().poseKind(), direction);
        }
        double progress = animation.collisionProgress(animation.targets.indexOf(actor));
        boolean restorative = animation.actionKind() == null ? restorativeEffect(kind)
                : animation.actionKind() != Ability.AbilityKind.DAMAGE;
        if (progress < 0 || progress >= 1 || restorative) {
            return BattleActorPose.REST;
        }
        double impact = Math.sin(Math.min(1, progress * 4) * Math.PI * 0.5) * Math.pow(1 - progress, 2);
        return new BattleActorPose(
                (int) Math.round(-direction * impact * 24.0),
                (int) Math.round(-impact * 7.0),
                direction * impact * 0.085,
                1.0 + impact * 0.035,
                1.0 - impact * 0.025
        );
    }

    void drawActorSprite(Graphics2D g, BufferedImage image, int x, int y, int width, int height, BattleActorPose pose, float alpha) {
        Graphics2D spriteG = (Graphics2D) g.create();
        spriteG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.0f, Math.min(1.0f, alpha))));
        double pivotX = x + width / 2.0;
        double pivotY = y + height * 0.86;
        spriteG.translate(pivotX, pivotY);
        spriteG.rotate(pose.rotation());
        spriteG.scale(pose.scaleX(), pose.scaleY());
        spriteG.drawImage(image, (int) Math.round(-width / 2.0), (int) Math.round(-height * 0.86), width, height, null);
        spriteG.dispose();
    }

    void drawHitFlash(Graphics2D g, BufferedImage image, int x, int y, int width, int height, BattleActorPose pose, int flash) {
        if (flash <= 0) return;
        BufferedImage mask = hitMasks.computeIfAbsent(image, source -> {
            BufferedImage tinted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D tint = tinted.createGraphics();
            tint.drawImage(source, 0, 0, null);
            tint.setComposite(AlphaComposite.SrcIn);
            tint.setColor(new Color(255, 246, 214));
            tint.fillRect(0, 0, tinted.getWidth(), tinted.getHeight());
            tint.dispose();
            return tinted;
        });
        drawActorSprite(g, mask, x, y, width, height, pose, Math.min(0.65f, flash / 18.0f));
    }

    boolean physicalEffect(String kind) {
        return switch (kind == null ? "" : kind) {
            case "strike", "impact", "bash", "slash", "cleave", "fang", "claw", "volley", "pierce" -> true;
            default -> false;
        };
    }

    private boolean restorativeEffect(String kind) {
        return switch (kind) {
            case "heal", "regeneration", "shield", "ward", "item", "grove_hymn", "root_memory", "seraphic_hymn",
                    "mend_unique", "shadow_salve_unique", "mercy_wellspring_unique", "panacea_toss_unique",
                    "primeval_bloom_unique", "smoke_veil_unique", "stone_guard_unique", "aegis_circle_unique",
                    "citadel_protocol_unique", "halo_bastion_unique" -> true;
            default -> false;
        };
    }

    boolean isPhysicalClass(String className) {
        return PHYSICAL_CLASS_NAMES.contains(className);
    }

    boolean isRangedClass(String className) {
        return RANGED_CLASS_NAMES.contains(className);
    }

    boolean isMagicClass(String className) {
        return MAGIC_CLASS_NAMES.contains(className);
    }

    boolean magicalEffect(String kind) {
        return switch (kind == null ? "" : kind) {
            case "heal", "regeneration", "shield", "ward", "fire", "burn", "ember", "water", "spark", "radiant",
                    "holy", "lightning", "arcane", "rune", "void", "dark", "chaos", "frost", "poison", "acid", "web",
                    "thorn", "nature", "shadow", "sonic", "bone", "dust", "howl", "item" -> true;
            default -> false;
        };
    }

    private BattleActorPose sourcePose(BattleActionAnimation animation, String kind, int direction) {
        boolean ranged = "volley".equals(kind) || "pierce".equals(kind) || kind.contains("shot") || kind.contains("arrow") || kind.contains("volley");
        boolean physical = physicalEffect(kind) || ranged || kind.contains("dual_cut") || kind.contains("shield_ram")
                || kind.contains("hemostatic") || kind.contains("titan_hammer") || kind.contains("final_challenge");
        BattleActorPose anticipation = ranged
                ? new BattleActorPose(-direction * 7, 0, -direction * .045, .99, 1.01)
                : physical ? new BattleActorPose(-direction * 12, 3, -direction * .065, 1.02, .98)
                : new BattleActorPose(-direction * 6, -6, -direction * .035, .99, 1.02);
        BattleActorPose release = ranged
                ? new BattleActorPose(-direction * 2, 0, direction * .025, 1, 1)
                : physical ? new BattleActorPose(direction * 26, -2, direction * .07, 1.02, .99)
                : new BattleActorPose(direction * 8, -4, direction * .04, 1, 1.01);
        return switch (animation.stage()) {
            case CAST -> blend(BattleActorPose.REST, anticipation, animation.castProgress());
            case TRAVEL -> blend(anticipation, release, Math.min(1, animation.travelProgress() / .75));
            case IMPACT -> blend(release, BattleActorPose.REST, animation.impactProgress());
            case DONE -> BattleActorPose.REST;
        };
    }

    private static BattleActorPose blend(BattleActorPose from, BattleActorPose to, double progress) {
        double t = progress * progress * (3 - 2 * progress);
        return new BattleActorPose(
                (int) Math.round(from.xOffset() + (to.xOffset() - from.xOffset()) * t),
                (int) Math.round(from.yOffset() + (to.yOffset() - from.yOffset()) * t),
                from.rotation() + (to.rotation() - from.rotation()) * t,
                from.scaleX() + (to.scaleX() - from.scaleX()) * t,
                from.scaleY() + (to.scaleY() - from.scaleY()) * t);
    }
}
