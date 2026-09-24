package com.alderfall.game;

import java.util.ArrayList;
import java.util.List;

/** Reads collision frames without consuming combat's damage-resolution events. */
final class BattleSoundTimeline {
    private BattleActionAnimation action;
    private int lastFrame = -1;
    private BattleActionAnimation releasedAction;
    private final SoundVariations variations = new SoundVariations();

    List<String> releases(BattleActionAnimation next) {
        if (next == null) { releasedAction = null; return List.of(); }
        if (!next.released() || next == releasedAction) return List.of();
        releasedAction = next;
        List<String> result = new ArrayList<>();
        String family = family(next);
        String release = switch (family) {
            case "blade", "claw" -> "blade_swing";
            case "blunt", "heavy" -> "blunt_swing";
            case "arrow" -> "arrow_release";
            default -> "";
        };
        if (!release.isEmpty() && !self(next)) result.add(variations.next(release, 3));
        String voice = monsterVoice(next);
        if (!voice.isEmpty() && !self(next)) result.add(variations.next(voice, 3));
        return result;
    }

    List<String> impacts(BattleActionAnimation next) {
        if (next != action) { action = next; lastFrame = -1; }
        List<String> result = new ArrayList<>();
        if (next != null) {
            for (int i = 0; i < next.targets.size(); i++) {
                int collision = next.collisionFrame(i);
                if (collision > lastFrame && collision <= next.frame()) {
                    String sound = family(next);
                    if (!sound.isEmpty()) {
                        sound += switch (sound) { case "fire" -> "_explosion"; case "ice" -> "_shatter"; default -> "_impact"; };
                        // Simultaneous area hits share one cue; staggered collisions remain separate.
                        if (!result.contains(sound)) result.add(sound);
                    }
                }
            }
            lastFrame = next.frame();
        }
        return result.stream().map(sound -> switch (sound) {
            case "fire_explosion", "ice_shatter", "lightning_impact", "arcane_impact" -> sound;
            default -> variations.next(sound, 3);
        }).toList();
    }

    static String flight(BattleActionAnimation action) {
        if (action == null || action.stage() != BattleActionAnimation.Stage.TRAVEL) return "";
        if (self(action) || (action.monsterVisual() != null && action.monsterVisual().motion() != MonsterAbilityVfx.Motion.PROJECTILE)) return "";
        if (action.visualProfile() != null && action.visualProfile().melee()) return "";
        return switch (family(action)) {
            case "fire", "ice", "lightning", "arcane", "shadow", "poison", "root", "holy", "water", "wind" -> family(action) + "_flight";
            default -> "";
        };
    }

    private static boolean self(BattleActionAnimation action) {
        return (action.monsterVisual() != null && action.monsterVisual().motion() == MonsterAbilityVfx.Motion.SELF)
                || (action.actionKind() != null && action.actionKind() != Ability.AbilityKind.DAMAGE);
    }

    static String family(BattleActionAnimation action) {
        if (action == null) return "";
        var profile = action.visualProfile();
        if (profile != null) return switch (profile.family()) {
            case SLASH -> "blade"; case PIERCE -> "arrow"; case BASH -> "blunt";
            case FIRE -> "fire"; case ICE -> "ice"; case LIGHTNING -> "lightning";
            case ARCANE -> "arcane"; case SHADOW, VEIL -> "shadow"; case POISON -> "poison";
            case ROOT -> "root"; case HOLY -> "holy"; case HEAL -> "heal"; case WARD -> "ward";
        };
        var monster = action.monsterVisual();
        if (monster != null) {
            String visual = monster.sprite().replaceFirst("^fx_", "");
            String material = family(visual);
            if (monster.motion() == MonsterAbilityVfx.Motion.MELEE) {
                String name = action.visualName().toLowerCase(java.util.Locale.ROOT);
                if ((material.equals("claw") || material.equals("blade") || material.equals("blunt"))
                        && (name.contains("bite") || name.contains("fang") || name.contains("maul"))) return "bite";
                if (material.equals("blunt") && action.source != null
                        && action.source.sprite.matches(".*(giant|golem|ogre|troll).*")) return "heavy";
            }
            if (!material.isEmpty()) return material;
        }
        return family(action.effectKind);
    }

    static String monsterVoice(BattleActionAnimation action) {
        if (action == null || action.monsterVisual() == null || action.source == null) return "";
        String sprite = action.source.sprite;
        if (sprite.matches(".*(skeleton|bone|lich).*")) return "monster_rattle";
        if (sprite.matches(".*(spider|scorpion|snake|serpent|slime|insect).*")) return "monster_hiss";
        if (sprite.matches(".*(dragon|giant|golem|ogre|troll|bear).*")) return "monster_roar";
        if (sprite.matches(".*(wolf|boar|hound|warg|beast|lion|panther).*")) return "monster_growl";
        return "";
    }

    static String family(String kind) {
        if (kind == null) return "";
        if (kind.contains("fire") || kind.contains("inferno") || kind.contains("ember") || kind.equals("burn") || kind.contains("noon")) return "fire";
        if (kind.contains("frost") || kind.contains("glacier") || (kind.equals("ice") || kind.startsWith("ice_") || kind.contains("icicle"))) return "ice";
        if (kind.contains("lightning") || kind.contains("spark") || kind.contains("thunder")) return "lightning";
        if (kind.contains("arcane") || kind.contains("rune") || kind.contains("ley_") || kind.contains("void") || kind.contains("prismatic")) return "arcane";
        if (kind.contains("heal") || kind.contains("mend") || kind.contains("regeneration") || kind.contains("hymn") || kind.equals("root_memory")) return "heal";
        if (kind.contains("shield") || kind.contains("ward") || kind.contains("guard")) return "ward";
        if (kind.contains("arrow") || kind.contains("shot") || kind.equals("pierce") || kind.equals("volley")) return "arrow";
        if (kind.contains("shadow") || kind.equals("dark") || kind.contains("veil")) return "shadow";
        if (kind.contains("poison") || kind.contains("acid") || kind.equals("web")) return "poison";
        if (kind.contains("thorn") || kind.contains("briar") || kind.contains("root") || kind.equals("nature")) return "root";
        if (kind.contains("holy") || kind.contains("radiant")) return "holy";
        if (kind.contains("water")) return "water";
        if (kind.equals("sonic") || kind.equals("howl") || kind.equals("dust") || kind.contains("wind")) return "wind";
        if (kind.equals("bone")) return "bone";
        if (kind.equals("fang") || kind.equals("bite")) return "bite";
        if (kind.equals("claw")) return "claw";
        if (kind.equals("slash") || kind.equals("cleave") || kind.equals("strike")) return "blade";
        if (kind.equals("impact") || kind.equals("bash")) return "blunt";
        return "";
    }
}
