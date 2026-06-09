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

    BattleRenderer(AssetStore assets) {
        this.assets = assets;
    }

    void drawBackdrop(Graphics2D g, Battle battle, int x, int y, int w, int h) {
        g.drawImage(assets.cover(battle.backdrop, w, h), x, y, null);
        g.setPaint(new GradientPaint(x, y, new Color(8, 10, 16, 70), x, y + h, new Color(8, 10, 16, 218)));
        g.fillRect(x, y, w, h);
        g.setPaint(null);
        g.setColor(new Color(20, 18, 18, 92));
        g.fillRect(x, y + h - 300, w, 300);
    }

    BattleActorPose actorPose(Battle battle, Actor actor, boolean enemySide) {
        BattleActionAnimation animation = battle.activeAnimation();
        if (animation == null || actor == null || !animation.involves(actor)) {
            return BattleActorPose.REST;
        }
        int direction = enemySide ? -1 : 1;
        boolean source = actor == animation.source;
        String kind = animation.effectKind == null ? "strike" : animation.effectKind;
        String className = actor.className == null ? "" : actor.className;
        if (source) {
            return sourcePose(animation, className, kind, direction);
        }
        if (animation.stage() != BattleActionAnimation.Stage.IMPACT) {
            return BattleActorPose.REST;
        }
        double impact = Math.sin(animation.impactProgress() * Math.PI);
        return new BattleActorPose(
                (int) Math.round(-direction * impact * 14.0),
                (int) Math.round(-impact * 4.0),
                direction * impact * 0.045,
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

    boolean physicalEffect(String kind) {
        return switch (kind == null ? "" : kind) {
            case "strike", "impact", "bash", "slash", "cleave", "fang", "claw", "volley", "pierce" -> true;
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

    private BattleActorPose sourcePose(BattleActionAnimation animation, String className, String kind, int direction) {
        boolean physical = physicalEffect(kind) || isPhysicalClass(className);
        boolean ranged = isRangedClass(className) || "volley".equals(kind) || "pierce".equals(kind);
        boolean magic = !physical || isMagicClass(className) || magicalEffect(kind);
        return switch (animation.stage()) {
            case CAST -> {
                double p = animation.castProgress();
                if (ranged) {
                    yield new BattleActorPose(
                            (int) Math.round(-direction * p * 10.0),
                            (int) Math.round(-Math.sin(p * Math.PI) * 5.0),
                            -direction * (0.08 + p * 0.05),
                            0.98,
                            1.04
                    );
                }
                if (physical) {
                    double coil = Math.sin(p * Math.PI);
                    yield new BattleActorPose(
                            (int) Math.round(-direction * coil * 12.0),
                            (int) Math.round(coil * 4.0),
                            -direction * coil * 0.085,
                            1.03,
                            0.98
                    );
                }
                double weave = Math.sin(p * Math.PI * 4.0);
                yield new BattleActorPose(
                        (int) Math.round(weave * 4.0),
                        (int) Math.round(-Math.sin(p * Math.PI) * 12.0),
                        weave * 0.06,
                        1.0 - Math.sin(p * Math.PI) * 0.02,
                        1.0 + Math.sin(p * Math.PI) * 0.05
                );
            }
            case TRAVEL -> {
                double p = animation.travelProgress();
                if (physical) {
                    double thrust = Math.sin(p * Math.PI);
                    yield new BattleActorPose(
                            (int) Math.round(direction * (10.0 + thrust * 18.0)),
                            (int) Math.round(-thrust * 3.0),
                            direction * thrust * 0.08,
                            1.04,
                            0.98
                    );
                }
                double settle = 1.0 - p;
                yield new BattleActorPose(
                        (int) Math.round(Math.sin(p * Math.PI * 2.0) * 3.0),
                        (int) Math.round(-settle * 6.0),
                        Math.sin(p * Math.PI * 2.0) * 0.025,
                        1.0,
                        1.0 + settle * 0.025
                );
            }
            case IMPACT -> {
                double p = 1.0 - animation.impactProgress();
                if (physical) {
                    yield new BattleActorPose(
                            (int) Math.round(direction * p * 12.0),
                            0,
                            direction * p * 0.04,
                            1.0,
                            1.0
                    );
                }
                yield new BattleActorPose(0, (int) Math.round(-p * 4.0), 0.0, 1.0, 1.0);
            }
            case DONE -> BattleActorPose.REST;
        };
    }
}
