package com.alderfall.game;

import java.util.Set;

/** Authored enemy cycles face screen-left, toward the player formation. */
public final class MonsterAttackAnimations {
    public static final Set<String> SPRITES = Set.of(
            "bandit_cutthroat", "bandit_archer", "bandit_captain",
            "goblin", "goblin_scout", "goblin_archer", "goblin_trapper", "goblin_skirmisher",
            "goblin_shaman", "hobgoblin_guard", "goblin_warlord", "goblin_king",
            "orc", "orc_raider", "orc_berserker", "orc_shaman", "orc_shieldbearer",
            "sheep", "doe", "crystal_hare", "mountain_goat", "wolf", "stag", "moss_stag",
            "frost_wolf", "snow_lynx", "bramble_boar", "stoneback_goat", "ember_tortoise",
            "marsh_drake", "mountain_drake", "red_dragon", "elder_dragon", "skeleton", "wraith",
            "hill_giant", "stone_giant", "fire_giant", "swamp_troll", "frost_troll", "ice_golem",
            "slime", "bat", "crypt_bat", "spider", "glass_scorpion", "ash_scorpion", "sand_stalker",
            "bog_beast", "reed_serpent", "river_eel", "thornling", "ember_imp",
            "void_knight", "flame_herald", "frost_witch", "shadow_beast", "demon_queen");

    private MonsterAttackAnimations() {}

    /** Keep legacy skeleton/spider strips available while selecting their new cycles. */
    public static String sheetName(String sprite, String action) {
        String version = "attack".equals(action) && ("skeleton".equals(sprite) || "spider".equals(sprite)) ? "_v2" : "";
        return sprite + "_" + action + version + "_anim";
    }

    public static boolean supports(String sprite) {
        return sprite != null && SPRITES.contains(sprite);
    }

    public static boolean playsAttack(BattleActionAnimation animation, Actor actor) {
        return actor != null && supports(actor.sprite) && animation != null && animation.source == actor
                && (animation.actionKind() == null || animation.actionKind() == Ability.AbilityKind.DAMAGE);
    }
}
