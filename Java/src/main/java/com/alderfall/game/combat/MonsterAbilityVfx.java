package com.alderfall.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Locale;

/** Enemy presentation only: damage, status, sound and targeting keys stay untouched. */
public final class MonsterAbilityVfx {
    public enum Motion { MELEE, PROJECTILE, SELF }
    public record Profile(String sprite, Motion motion) {
        public String id() { return sprite + "_" + motion.name().toLowerCase(Locale.ROOT); }
    }
    private MonsterAbilityVfx() {}

    public static Profile basic(String sprite) {
        String effect = switch (sprite) {
            case "bandit_archer", "goblin_archer" -> "arrow";
            case "goblin_shaman", "orc_shaman", "wraith", "demon_queen" -> "shadow";
            case "flame_herald", "ember_imp" -> "fire";
            case "frost_witch" -> "frost";
            case "skeleton", "bandit_cutthroat", "bandit_captain", "goblin", "goblin_scout", "goblin_skirmisher", "void_knight" -> "slash";
            case "orc", "orc_raider", "orc_berserker", "goblin_warlord" -> "cleave";
            case "goblin_trapper" -> "slash";
            case "hobgoblin_guard", "orc_shieldbearer", "stag", "moss_stag" -> "impact";
            case "slime" -> "poison";
            case "thornling", "bramble_boar" -> "thorn";
            case "hill_giant", "stone_giant", "fire_giant", "ice_golem", "sheep", "doe", "mountain_goat", "stoneback_goat", "crystal_hare", "goblin_king" -> "impact";
            default -> "claw";
        };
        boolean ranged = java.util.Set.of("arrow", "shadow", "fire", "frost").contains(effect);
        return new Profile("fx_" + effect, ranged ? Motion.PROJECTILE : Motion.MELEE);
    }

    public static Profile forAbility(String sprite, MonsterAbility ability) {
        String name = ability.name().toLowerCase(Locale.ROOT);
        String effect = ability.effect();
        if (ability.kind() == MonsterAbility.Kind.BUFF) {
            boolean healing = ability.statuses().stream().anyMatch(s -> s.key().equals("regeneration"));
            String art = healing ? "heal" : switch (effect) {
                case "frost" -> "frost";
                case "fire" -> "ember";
                case "dust" -> "smoke_veil_unique";
                case "sonic" -> "sonic";
                default -> "shield";
            };
            if (name.contains("icebound") || name.contains("cold hide")) art = "frost";
            if (name.contains("stonehide")) art = "stone_guard_unique";
            return new Profile("fx_" + art, Motion.SELF);
        }
        String art = switch (effect) {
            case "acid", "poison" -> "poison";
            case "fang" -> "claw";
            case "pierce" -> "arrow";
            case "shield" -> "shield_ram_unique";
            case "ward" -> "arcane";
            case "burn" -> "fire";
            case "strike", "bash" -> "impact";
            default -> effect;
        };
        Motion motion = switch (effect) {
            case "fang", "claw", "slash", "cleave", "impact", "strike", "bash", "shield" -> Motion.MELEE;
            default -> Motion.PROJECTILE;
        };
        if (name.matches(".*(bite|sting|pounce|maul|slam|club|fist|kick|hoof|horn|charge|coil|drag|hook).*")) motion = Motion.MELEE;
        if (name.contains("smash") || name.contains("fist") || name.equals("stone horn")) art = "impact";
        if (name.equals("crag bite")) art = "claw";
        if (name.equals("current lash")) art = "water_jet_unique";
        if (name.equals("numbing coil")) art = "lightning";
        if (name.equals("pinning shot")) art = "snare_arrow_unique";
        if (name.equals("barbed arrow")) art = "piercing_shot_unique";
        if (name.contains("breath") || name.equals("elderflame")) motion = Motion.PROJECTILE;
        return new Profile("fx_" + art, motion);
    }

    /** Shared by battle and exported preview: phase 0 charge, 1 travel, 2 impact. */
    public static void draw(Graphics2D g, AssetStore assets, Profile profile, int[] source, int[] target, int phase, double progress) {
        double p = Math.max(0, Math.min(1, progress));
        if (phase == 0 && profile.motion == Motion.MELEE) return;
        if (phase == 1 && profile.motion != Motion.PROJECTILE) return;
        int[] end = profile.motion == Motion.SELF ? source : target;
        double x = end[0], y = end[1], size = 150, angle = 0;
        if (phase > 0 && profile.motion == Motion.PROJECTILE)
            angle = Math.atan2(end[1] - source[1], end[0] - source[0])
                    - com.alderfall.game.render.battle.BattleVfxRenderer.nativeEffectForwardAngle(profile.sprite);
        float alpha = (float)(0.95 * (1 - p * 0.8));
        if (phase == 0) { x = source[0]; y = source[1]; size = 35 + 30 * p; alpha = (float)(0.2 + 0.4 * p); }
        if (phase == 1) {
            x = source[0] + (end[0] - source[0]) * p;
            y = source[1] + (end[1] - source[1]) * p;
            size = 78; alpha = 0.9f;
        }
        int count = assets.effectSpriteFrameCount(profile.sprite);
        BufferedImage image = assets.effectSprite(profile.sprite, (int)size, (int)size, Math.min(count - 1, (int)(p * count)));
        Graphics2D fx = (Graphics2D)g.create();
        fx.setComposite(AlphaComposite.SrcOver.derive(alpha));
        fx.translate(x,y); fx.rotate(angle);
        fx.drawImage(image, -(int)size/2, -(int)size/2, null);
        fx.dispose();
    }
}
