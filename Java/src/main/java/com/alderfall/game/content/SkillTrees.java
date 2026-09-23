package com.alderfall.game;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkillTrees {
    public static final String COMMON_TREE_NAME = "Common";
    public static final Map<String, SkillNode> COMMON_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> KNIGHT_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> MAGE_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> RANGER_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> CLERIC_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> ROGUE_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> PROFESSION_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> WOODCUTTING_PROFESSION_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> FISHING_PROFESSION_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> MINING_PROFESSION_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> CRAFTING_PROFESSION_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> WEAVING_PROFESSION_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> LEATHERWORKING_PROFESSION_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> COOKING_PROFESSION_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> SURVIVAL_PROFESSION_TREE = new LinkedHashMap<>();
    public static final Map<String, Map<String, SkillNode>> PROFESSION_SKILL_TREES = new LinkedHashMap<>();
    public static final Map<String, SkillNode> BATTLE_MEDIC_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> IRONWALL_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> BLADEDANCER_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> VEILRUNNER_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> WILDSPEAKER_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> SUNWARDEN_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> THORNBINDER_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> STONEBREAKER_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> NIGHTBLADE_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> GROVEKEEPER_SKILL_TREE = new LinkedHashMap<>();
    public static final Map<String, SkillNode> SKILL_TREE = new LinkedHashMap<>();
    public static final Set<String> SKILL_ABILITY_NAMES;

    static {
        add(COMMON_SKILL_TREE, node("survival", "Survival", "+7 max HP per rank.", 3, 0, 0, List.of(), Map.of("max_hp", 7), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("merchant_sense", "Tradescraft", "Consumables restore +3 HP and +2 MP per rank.", 3, 2, 0, List.of(), Map.of(), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("campfire_music", "Campfire Music", "Learn Campfire Hymn, a modest heal.", 1, 4, 0, List.of(), Map.of(), new Ability("Campfire Hymn", 22, 7, Ability.AbilityKind.HEAL, "ally"), COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("forager", "Forager", "Find steadier battle spoils and keep moving longer.", 2, 0, 1, List.of("survival"), Map.of("max_mp", 3), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("scavenger", "Scavenger", "+10% battle gold per rank.", 3, 1, 1, List.of("survival"), Map.of(), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("battle_medic", "Field Medicine", "Healing items restore +8 extra HP.", 1, 2, 1, List.of("merchant_sense"), Map.of(), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("shared_training", "Road Songs", "Allies deal +1 damage per rank.", 3, 3, 1, List.of("campfire_music"), Map.of(), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("pathfinder", "Pathfinder", "+1 attack and +1 defense per rank.", 2, 4, 1, List.of("campfire_music"), Map.of("attack", 1, "defense", 1), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("settlement_lore", "Settlement Lore", "+5 max MP and +1 defense per rank.", 2, 2, 2, List.of("scavenger", "battle_medic"), Map.of("max_mp", 5, "defense", 1), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("hard_won_lessons", "Hard-Won Lessons", "+8% XP from battles and quests per rank.", 2, 0, 3, List.of("scavenger"), Map.of(), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("evasive_footwork", "Evasive Footwork", "+1 defense and +4 max MP per rank.", 2, 1, 3, List.of("pathfinder"), Map.of("defense", 1, "max_mp", 4), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("rallying_tune", "Rallying Tune", "Learn Rallying Tune, a party heal.", 1, 2, 3, List.of("campfire_music", "shared_training"), Map.of(), new Ability("Rallying Tune", 18, 10, Ability.AbilityKind.HEAL, "party"), COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("seasoned_adventurer", "Seasoned Adventurer", "+10 HP, +6 MP, +1 attack, and +1 defense.", 1, 3, 4, List.of("settlement_lore", "hard_won_lessons", "evasive_footwork"), Map.of("max_hp", 10, "max_mp", 6, "attack", 1, "defense", 1), null, COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("quick_toss", "Quick Toss", "Learn Quick Toss, a cheap utility attack.", 1, 0, 4, List.of("scavenger", "hard_won_lessons"), Map.of(), new Ability("Quick Toss", 24, 5), COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("herbal_poultice", "Herbal Poultice", "Learn Herbal Poultice, a reliable field heal.", 1, 1, 4, List.of("forager", "battle_medic"), Map.of(), new Ability("Herbal Poultice", 26, 8, Ability.AbilityKind.HEAL, "ally"), COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("signal_flare", "Signal Flare", "Learn Signal Flare, a modest attack against all foes.", 1, 2, 5, List.of("pathfinder", "rallying_tune"), Map.of(), new Ability("Signal Flare", 18, 10, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.AOE), COMMON_TREE_NAME));
        add(COMMON_SKILL_TREE, node("last_stand_camp", "Last Stand Camp", "Learn Last Stand Camp, a defensive emergency stance.", 1, 3, 5, List.of("seasoned_adventurer", "herbal_poultice"), Map.of(), new Ability("Last Stand Camp", 0, 7, Ability.AbilityKind.DEFEND, "self"), COMMON_TREE_NAME));

        addProfessionSkillNodes();

        add(KNIGHT_SKILL_TREE, node("iron_body", "Iron Body", "+9 max HP per rank.", 3, 0, 0, List.of(), Map.of("max_hp", 9), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("weapon_training", "Weapon Training", "+2 attack per rank.", 3, 2, 0, List.of(), Map.of("attack", 2), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("warded_armor", "Warded Armor", "+1 defense per rank.", 3, 4, 0, List.of(), Map.of("defense", 1), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("power_strike", "Power Strike", "Learn Power Strike, a strong low-cost attack.", 1, 1, 1, List.of("weapon_training"), Map.of(), new Ability("Power Strike", 30, 7), "Knight"));
        add(KNIGHT_SKILL_TREE, node("guard_mastery", "Guard Mastery", "Take 1 less damage per rank.", 3, 3, 1, List.of("warded_armor"), Map.of(), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("stalwart_guard", "Stalwart Guard", "Defend restores +1 extra MP per rank.", 2, 4, 1, List.of("warded_armor"), Map.of(), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("second_wind", "Second Wind", "Learn Second Wind, a self-heal for long fights.", 1, 1, 2, List.of("iron_body", "power_strike"), Map.of(), new Ability("Second Wind", 32, 9, Ability.AbilityKind.HEAL, "self"), "Knight"));
        add(KNIGHT_SKILL_TREE, node("blade_flurry", "Blade Flurry", "Basic attacks gain +3 damage per rank.", 2, 2, 2, List.of("power_strike", "guard_mastery"), Map.of(), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("heroic_resolve", "Heroic Resolve", "+20 max HP, +2 attack, and +2 defense.", 1, 3, 3, List.of("second_wind", "blade_flurry", "stalwart_guard"), Map.of("max_hp", 20, "attack", 2, "defense", 2), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("shieldbreaker", "Shieldbreaker", "Learn Shieldbreaker, a strike that exposes foes.", 1, 0, 3, List.of("power_strike", "guard_mastery"), Map.of(), new Ability("Shieldbreaker", 34, 9), "Knight"));
        add(KNIGHT_SKILL_TREE, node("unyielding", "Unyielding", "Below half HP, take 2 less damage per rank.", 3, 1, 3, List.of("iron_body", "guard_mastery"), Map.of(), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("war_cry", "War Cry", "Learn War Cry, a guarding stance that hastes you.", 1, 2, 3, List.of("blade_flurry"), Map.of(), new Ability("War Cry", 0, 8, Ability.AbilityKind.DEFEND, "self"), "Knight"));
        add(KNIGHT_SKILL_TREE, node("aegis_mend", "Aegis Mend", "Learn Aegis Mend, a shielded ally heal.", 1, 3, 4, List.of("second_wind", "stalwart_guard"), Map.of(), new Ability("Aegis Mend", 28, 10, Ability.AbilityKind.HEAL, "ally"), "Knight"));
        add(KNIGHT_SKILL_TREE, node("vanguard_legend", "Vanguard Legend", "+18 HP, +3 attack, and +3 defense.", 1, 4, 4, List.of("heroic_resolve", "shieldbreaker", "war_cry"), Map.of("max_hp", 18, "attack", 3, "defense", 3), null, "Knight"));
        add(KNIGHT_SKILL_TREE, node("banner_guard", "Banner Guard", "Learn Banner Guard, a stalwart party-facing defense stance.", 1, 1, 5, List.of("aegis_mend", "heroic_resolve"), Map.of(), new Ability("Banner Guard", 0, 10, Ability.AbilityKind.DEFEND, "self"), "Knight"));
        add(KNIGHT_SKILL_TREE, node("lionheart_charge", "Lionheart Charge", "Learn Lionheart Charge, a sweeping strike against all foes.", 1, 3, 5, List.of("shieldbreaker", "vanguard_legend"), Map.of(), new Ability("Lionheart Charge", 28, 14, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.AOE), "Knight"));

        add(MAGE_SKILL_TREE, node("battle_focus", "Battle Focus", "+6 max MP per rank.", 3, 0, 0, List.of(), Map.of("max_mp", 6), null, "Mage"));
        add(MAGE_SKILL_TREE, node("channeling", "Channeling", "Healing abilities restore +4 HP per rank.", 3, 2, 0, List.of("battle_focus"), Map.of(), null, "Mage"));
        add(MAGE_SKILL_TREE, node("spellcraft", "Spellcraft", "Damaging abilities gain +2 power per rank.", 3, 4, 0, List.of(), Map.of("max_mp", 2), null, "Mage"));
        add(MAGE_SKILL_TREE, node("mana_well", "Mana Well", "+4 max MP and +1 defense per rank.", 2, 0, 1, List.of("battle_focus"), Map.of("max_mp", 4, "defense", 1), null, "Mage"));
        add(MAGE_SKILL_TREE, node("ether_flow", "Ether Flow", "Recover +1 MP per rank after using an ability.", 2, 2, 1, List.of("channeling"), Map.of(), null, "Mage"));
        add(MAGE_SKILL_TREE, node("elemental_precision", "Elemental Precision", "+1 attack and +3 max MP per rank.", 2, 4, 1, List.of("spellcraft"), Map.of("attack", 1, "max_mp", 3), null, "Mage"));
        add(MAGE_SKILL_TREE, node("arcane_burst", "Arcane Burst", "Learn Arcane Burst, a heavy magical attack.", 1, 2, 2, List.of("ether_flow", "elemental_precision"), Map.of(), new Ability("Arcane Burst", 46, 14), "Mage"));
        add(MAGE_SKILL_TREE, node("renewing_ward", "Renewing Ward", "Learn Renewing Ward, a strong heal.", 1, 3, 2, List.of("channeling", "ether_flow"), Map.of(), new Ability("Renewing Ward", 34, 12, Ability.AbilityKind.HEAL, "ally"), "Mage"));
        add(MAGE_SKILL_TREE, node("archmage_resolve", "Archmage Resolve", "+18 max MP, +2 attack, and +1 defense.", 1, 3, 3, List.of("arcane_burst", "renewing_ward"), Map.of("max_mp", 18, "attack", 2, "defense", 1), null, "Mage"));
        add(MAGE_SKILL_TREE, node("overchannel", "Overchannel", "Damaging abilities gain +3 power per rank.", 2, 0, 3, List.of("spellcraft", "elemental_precision"), Map.of(), null, "Mage"));
        add(MAGE_SKILL_TREE, node("chain_spark", "Chain Spark", "Learn Chain Spark, hitting every enemy.", 1, 1, 3, List.of("arcane_burst"), Map.of(), new Ability("Chain Spark", 30, 15, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.CHAIN), "Mage"));
        add(MAGE_SKILL_TREE, node("mana_bloom", "Mana Bloom", "Learn Mana Bloom, a party-wide restorative spell.", 1, 2, 3, List.of("renewing_ward"), Map.of(), new Ability("Mana Bloom", 20, 13, Ability.AbilityKind.HEAL, "party"), "Mage"));
        add(MAGE_SKILL_TREE, node("frostbite", "Frostbite", "Weak foes take +5% damage per rank.", 2, 3, 4, List.of("chain_spark", "elemental_precision"), Map.of(), null, "Mage"));
        add(MAGE_SKILL_TREE, node("star_savant", "Star Savant", "+20 MP, +3 attack, and +2 defense.", 1, 4, 4, List.of("archmage_resolve", "overchannel", "mana_bloom"), Map.of("max_mp", 20, "attack", 3, "defense", 2), null, "Mage"));
        add(MAGE_SKILL_TREE, node("meteor_thread", "Meteor Thread", "Learn Meteor Thread, a precise high-power spell.", 1, 0, 5, List.of("overchannel", "chain_spark"), Map.of(), new Ability("Meteor Thread", 58, 18), "Mage"));
        add(MAGE_SKILL_TREE, node("astral_refuge", "Astral Refuge", "Learn Astral Refuge, a party-wide restorative ward.", 1, 2, 5, List.of("mana_bloom", "star_savant"), Map.of(), new Ability("Astral Refuge", 24, 16, Ability.AbilityKind.HEAL, "party"), "Mage"));

        add(RANGER_SKILL_TREE, node("trail_sense", "Trail Sense", "+1 attack and +1 defense per rank.", 2, 0, 0, List.of(), Map.of("attack", 1, "defense", 1), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("quickdraw_drills", "Quickdraw Drills", "+2 attack per rank.", 3, 2, 0, List.of(), Map.of("attack", 2), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("wild_medicine", "Wild Medicine", "+6 max HP and +3 max MP per rank.", 2, 4, 0, List.of(), Map.of("max_hp", 6, "max_mp", 3), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("marked_shot", "Marked Shot", "Learn Marked Shot, a clean single-target attack.", 1, 1, 1, List.of("trail_sense", "quickdraw_drills"), Map.of(), new Ability("Marked Shot", 29, 7), "Ranger"));
        add(RANGER_SKILL_TREE, node("keen_edge", "Keen Edge", "+8% chance per rank for basic attacks to hit hard.", 3, 3, 1, List.of("quickdraw_drills"), Map.of(), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("field_salve", "Field Salve", "Learn Field Salve, an efficient heal.", 1, 4, 1, List.of("wild_medicine"), Map.of(), new Ability("Field Salve", 28, 9, Ability.AbilityKind.HEAL, "ally"), "Ranger"));
        add(RANGER_SKILL_TREE, node("twin_fang", "Twin Fang", "Learn Twin Fang, a heavy ranger strike.", 1, 2, 2, List.of("marked_shot", "keen_edge"), Map.of(), new Ability("Twin Fang", 40, 12), "Ranger"));
        add(RANGER_SKILL_TREE, node("pack_coordination", "Pack Coordination", "Allies deal +1 more damage per rank.", 2, 3, 2, List.of("keen_edge", "field_salve"), Map.of(), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("warden_resolve", "Warden Resolve", "+12 max HP, +8 max MP, +2 attack, and +1 defense.", 1, 3, 3, List.of("twin_fang", "pack_coordination"), Map.of("max_hp", 12, "max_mp", 8, "attack", 2, "defense", 1), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("poison_arrow", "Poison Arrow", "Learn Poison Arrow, a venomous shot.", 1, 0, 3, List.of("marked_shot"), Map.of(), new Ability("Poison Arrow", 27, 8), "Ranger"));
        add(RANGER_SKILL_TREE, node("volley_mastery", "Volley Mastery", "Multi-target attacks gain +2 damage per rank.", 3, 1, 3, List.of("quickdraw_drills", "keen_edge"), Map.of(), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("hawk_eye", "Hawk Eye", "Basic attack crit chance and crit damage improve per rank.", 2, 2, 3, List.of("keen_edge"), Map.of(), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("smoke_screen", "Smoke Screen", "Learn Smoke Screen, a defensive vanish.", 1, 3, 4, List.of("field_salve", "pack_coordination"), Map.of(), new Ability("Smoke Screen", 0, 8, Ability.AbilityKind.DEFEND, "self"), "Ranger"));
        add(RANGER_SKILL_TREE, node("wildwarden", "Wildwarden", "+16 HP, +10 MP, +3 attack, and +2 defense.", 1, 4, 4, List.of("warden_resolve", "poison_arrow", "hawk_eye"), Map.of("max_hp", 16, "max_mp", 10, "attack", 3, "defense", 2), null, "Ranger"));
        add(RANGER_SKILL_TREE, node("pinning_volley", "Pinning Volley", "Learn Pinning Volley, a multi-target ranger attack.", 1, 0, 5, List.of("poison_arrow", "volley_mastery"), Map.of(), new Ability("Pinning Volley", 26, 13, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.MULTI_PROJECTILE), "Ranger"));
        add(RANGER_SKILL_TREE, node("greenway_remedy", "Greenway Remedy", "Learn Greenway Remedy, a party heal for extended journeys.", 1, 3, 5, List.of("smoke_screen", "wildwarden"), Map.of(), new Ability("Greenway Remedy", 22, 14, Ability.AbilityKind.HEAL, "party"), "Ranger"));

        add(CLERIC_SKILL_TREE, node("steady_hands", "Steady Hands", "+5 max MP and +1 defense per rank.", 3, 0, 0, List.of(), Map.of("max_mp", 5, "defense", 1), null, "Cleric"));
        add(CLERIC_SKILL_TREE, node("sanctuary_vow", "Sanctuary Vow", "+8 max HP per rank.", 3, 2, 0, List.of(), Map.of("max_hp", 8), null, "Cleric"));
        add(CLERIC_SKILL_TREE, node("radiant_bolt", "Radiant Bolt", "Learn Radiant Bolt, a searing holy strike.", 1, 4, 0, List.of("steady_hands"), Map.of(), new Ability("Radiant Bolt", 27, 7), "Cleric"));
        add(CLERIC_SKILL_TREE, node("ward_prayer", "Ward Prayer", "Learn Ward Prayer, a protective heal.", 1, 1, 1, List.of("steady_hands", "sanctuary_vow"), Map.of(), new Ability("Ward Prayer", 30, 10, Ability.AbilityKind.HEAL, "ally"), "Cleric"));
        add(CLERIC_SKILL_TREE, node("grace_flow", "Grace Flow", "Recover +1 MP per rank after using an ability.", 2, 3, 1, List.of("ward_prayer"), Map.of(), null, "Cleric"));
        add(CLERIC_SKILL_TREE, node("sunlit_aegis", "Sunlit Aegis", "Learn Sunlit Aegis, a group-turn defensive stance.", 1, 4, 1, List.of("radiant_bolt"), Map.of(), new Ability("Sunlit Aegis", 0, 8, Ability.AbilityKind.DEFEND, "self"), "Cleric"));
        add(CLERIC_SKILL_TREE, node("reviving_chorus", "Reviving Chorus", "Learn Reviving Chorus, a strong restorative hymn.", 1, 2, 2, List.of("ward_prayer", "grace_flow"), Map.of(), new Ability("Reviving Chorus", 42, 14, Ability.AbilityKind.HEAL, "ally"), "Cleric"));
        add(CLERIC_SKILL_TREE, node("dawn_resolve", "Dawn Resolve", "+14 max HP, +12 max MP, and +2 defense.", 1, 3, 3, List.of("reviving_chorus", "sunlit_aegis"), Map.of("max_hp", 14, "max_mp", 12, "defense", 2), null, "Cleric"));
        add(CLERIC_SKILL_TREE, node("judgment_chorus", "Judgment Chorus", "Learn Judgment Chorus, a radiant strike against all foes.", 1, 0, 3, List.of("radiant_bolt", "grace_flow"), Map.of(), new Ability("Judgment Chorus", 25, 12, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.AOE), "Cleric"));
        add(CLERIC_SKILL_TREE, node("mercy_guard", "Mercy Guard", "Learn Mercy Guard, a defensive prayer for crisis turns.", 1, 1, 4, List.of("ward_prayer", "sunlit_aegis"), Map.of(), new Ability("Mercy Guard", 0, 8, Ability.AbilityKind.DEFEND, "self"), "Cleric"));
        add(CLERIC_SKILL_TREE, node("pilgrim_light", "Pilgrim Light", "Learn Pilgrim Light, a measured party heal.", 1, 2, 4, List.of("reviving_chorus", "dawn_resolve"), Map.of(), new Ability("Pilgrim Light", 22, 13, Ability.AbilityKind.HEAL, "party"), "Cleric"));
        add(CLERIC_SKILL_TREE, node("saintly_oath", "Saintly Oath", "+12 HP, +14 MP, +1 attack, and +2 defense.", 1, 3, 5, List.of("judgment_chorus", "pilgrim_light"), Map.of("max_hp", 12, "max_mp", 14, "attack", 1, "defense", 2), null, "Cleric"));

        add(ROGUE_SKILL_TREE, node("footwork", "Footwork", "+1 attack and +1 defense per rank.", 3, 0, 0, List.of(), Map.of("attack", 1, "defense", 1), null, "Rogue"));
        add(ROGUE_SKILL_TREE, node("knife_work", "Knife Work", "+2 attack per rank.", 3, 2, 0, List.of(), Map.of("attack", 2), null, "Rogue"));
        add(ROGUE_SKILL_TREE, node("smoke_pockets", "Smoke Pockets", "+4 max MP per rank.", 2, 4, 0, List.of("footwork"), Map.of("max_mp", 4), null, "Rogue"));
        add(ROGUE_SKILL_TREE, node("shadowstep_cut", "Shadowstep Cut", "Learn Shadowstep Cut, a fast shadow strike.", 1, 1, 1, List.of("footwork", "knife_work"), Map.of(), new Ability("Shadowstep Cut", 31, 8), "Rogue"));
        add(ROGUE_SKILL_TREE, node("venom_edge", "Venom Edge", "Learn Venom Edge, a poison-laced attack.", 1, 3, 1, List.of("knife_work"), Map.of(), new Ability("Venom Edge", 24, 7), "Rogue"));
        add(ROGUE_SKILL_TREE, node("smoke_veil", "Smoke Veil", "Learn Smoke Veil, a guarded evasive stance.", 1, 4, 1, List.of("smoke_pockets"), Map.of(), new Ability("Smoke Veil", 0, 6, Ability.AbilityKind.DEFEND, "self"), "Rogue"));
        add(ROGUE_SKILL_TREE, node("knife_storm", "Knife Storm", "Learn Knife Storm, a sharp burst attack.", 1, 2, 2, List.of("shadowstep_cut", "venom_edge"), Map.of(), new Ability("Knife Storm", 42, 13), "Rogue"));
        add(ROGUE_SKILL_TREE, node("night_resolve", "Night Resolve", "+10 max HP, +6 max MP, +3 attack, and +1 defense.", 1, 3, 3, List.of("knife_storm", "smoke_veil"), Map.of("max_hp", 10, "max_mp", 6, "attack", 3, "defense", 1), null, "Rogue"));
        add(ROGUE_SKILL_TREE, node("cheap_shot", "Cheap Shot", "Learn Cheap Shot, a low-cost exploit attack.", 1, 0, 3, List.of("shadowstep_cut"), Map.of(), new Ability("Cheap Shot", 27, 6), "Rogue"));
        add(ROGUE_SKILL_TREE, node("fan_of_knives", "Fan of Knives", "Learn Fan of Knives, a multi-target blade burst.", 1, 1, 4, List.of("cheap_shot", "knife_storm"), Map.of(), new Ability("Fan of Knives", 25, 12, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.MULTI_PROJECTILE), "Rogue"));
        add(ROGUE_SKILL_TREE, node("evasion_drills", "Evasion Drills", "+2 defense and +5 max MP per rank.", 2, 3, 4, List.of("smoke_veil", "night_resolve"), Map.of("defense", 2, "max_mp", 5), null, "Rogue"));
        add(ROGUE_SKILL_TREE, node("shadow_mastery", "Shadow Mastery", "+12 MP, +4 attack, and +2 defense.", 1, 2, 5, List.of("fan_of_knives", "evasion_drills"), Map.of("max_mp", 12, "attack", 4, "defense", 2), null, "Rogue"));

        add(BATTLE_MEDIC_SKILL_TREE, node("medic_triage", "Triage", "+6 max MP and +1 defense per rank.", 3, 0, 0, List.of(), Map.of("max_mp", 6, "defense", 1), null, "Battle Medic"));
        add(BATTLE_MEDIC_SKILL_TREE, node("medic_pack", "Medic Pack", "+7 max HP per rank.", 3, 2, 0, List.of(), Map.of("max_hp", 7), null, "Battle Medic"));
        add(BATTLE_MEDIC_SKILL_TREE, node("field_suture", "Field Suture", "Learn Field Suture, a strong ally heal.", 1, 1, 1, List.of("medic_triage"), Map.of(), new Ability("Field Suture", 34, 10, Ability.AbilityKind.HEAL, "ally"), "Battle Medic"));
        add(BATTLE_MEDIC_SKILL_TREE, node("antidote_drill", "Antidote Drill", "+4 max MP and +1 attack per rank.", 2, 3, 1, List.of("medic_pack"), Map.of("max_mp", 4, "attack", 1), null, "Battle Medic"));
        add(BATTLE_MEDIC_SKILL_TREE, node("stabilize_party", "Stabilize Party", "Learn Stabilize Party, a party-wide heal.", 1, 2, 2, List.of("field_suture", "antidote_drill"), Map.of(), new Ability("Stabilize Party", 19, 12, Ability.AbilityKind.HEAL, "party"), "Battle Medic"));
        add(BATTLE_MEDIC_SKILL_TREE, node("combat_surgeon", "Combat Surgeon", "+12 HP, +10 MP, and +2 defense.", 1, 2, 3, List.of("stabilize_party"), Map.of("max_hp", 12, "max_mp", 10, "defense", 2), null, "Battle Medic"));
        add(BATTLE_MEDIC_SKILL_TREE, node("trauma_ward", "Trauma Ward", "Learn Trauma Ward, a defensive emergency stance.", 1, 0, 3, List.of("field_suture"), Map.of(), new Ability("Trauma Ward", 0, 8, Ability.AbilityKind.DEFEND, "self"), "Battle Medic"));
        add(BATTLE_MEDIC_SKILL_TREE, node("last_stand_tonic", "Last Stand Tonic", "Learn Last Stand Tonic, a stronger party recovery.", 1, 3, 4, List.of("stabilize_party", "combat_surgeon"), Map.of(), new Ability("Last Stand Tonic", 25, 15, Ability.AbilityKind.HEAL, "party"), "Battle Medic"));
        add(BATTLE_MEDIC_SKILL_TREE, node("surgeon_general", "Surgeon General", "+10 HP, +16 MP, and +2 defense.", 1, 2, 5, List.of("trauma_ward", "last_stand_tonic"), Map.of("max_hp", 10, "max_mp", 16, "defense", 2), null, "Battle Medic"));

        add(IRONWALL_SKILL_TREE, node("ironwall_frame", "Ironwall Frame", "+10 max HP per rank.", 3, 0, 0, List.of(), Map.of("max_hp", 10), null, "Ironwall"));
        add(IRONWALL_SKILL_TREE, node("tower_shield", "Tower Shield", "+2 defense per rank.", 3, 2, 0, List.of(), Map.of("defense", 2), null, "Ironwall"));
        add(IRONWALL_SKILL_TREE, node("shield_anchor", "Shield Anchor", "Learn Shield Anchor, a fortified stance.", 1, 1, 1, List.of("tower_shield"), Map.of(), new Ability("Shield Anchor", 0, 6, Ability.AbilityKind.DEFEND, "self"), "Ironwall"));
        add(IRONWALL_SKILL_TREE, node("line_crusher", "Line Crusher", "+2 attack per rank.", 2, 3, 1, List.of("ironwall_frame"), Map.of("attack", 2), null, "Ironwall"));
        add(IRONWALL_SKILL_TREE, node("wallbreaker_slam", "Wallbreaker Slam", "Learn Wallbreaker Slam, a punishing tank strike.", 1, 2, 2, List.of("shield_anchor", "line_crusher"), Map.of(), new Ability("Wallbreaker Slam", 38, 11), "Ironwall"));
        add(IRONWALL_SKILL_TREE, node("unmoving_bastion", "Unmoving Bastion", "+18 HP, +3 defense, and +1 attack.", 1, 2, 3, List.of("wallbreaker_slam"), Map.of("max_hp", 18, "defense", 3, "attack", 1), null, "Ironwall"));
        add(IRONWALL_SKILL_TREE, node("battering_charge", "Battering Charge", "Learn Battering Charge, a broad shield assault.", 1, 0, 3, List.of("line_crusher", "wallbreaker_slam"), Map.of(), new Ability("Battering Charge", 26, 13, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.AOE), "Ironwall"));
        add(IRONWALL_SKILL_TREE, node("fortress_oath", "Fortress Oath", "Learn Fortress Oath, a deep defensive stance.", 1, 4, 3, List.of("shield_anchor", "unmoving_bastion"), Map.of(), new Ability("Fortress Oath", 0, 10, Ability.AbilityKind.DEFEND, "self"), "Ironwall"));
        add(IRONWALL_SKILL_TREE, node("living_rampart", "Living Rampart", "+22 HP, +1 attack, and +3 defense.", 1, 2, 5, List.of("battering_charge", "fortress_oath"), Map.of("max_hp", 22, "attack", 1, "defense", 3), null, "Ironwall"));

        add(BLADEDANCER_SKILL_TREE, node("dancer_balance", "Dancer Balance", "+1 attack and +1 defense per rank.", 3, 0, 0, List.of(), Map.of("attack", 1, "defense", 1), null, "Bladedancer"));
        add(BLADEDANCER_SKILL_TREE, node("twin_forms", "Twin Forms", "+2 attack per rank.", 3, 2, 0, List.of(), Map.of("attack", 2), null, "Bladedancer"));
        add(BLADEDANCER_SKILL_TREE, node("crescent_cut", "Crescent Cut", "Learn Crescent Cut, a precise fighter attack.", 1, 1, 1, List.of("twin_forms"), Map.of(), new Ability("Crescent Cut", 32, 8), "Bladedancer"));
        add(BLADEDANCER_SKILL_TREE, node("flowing_guard", "Flowing Guard", "+5 max HP and +4 max MP per rank.", 2, 3, 1, List.of("dancer_balance"), Map.of("max_hp", 5, "max_mp", 4), null, "Bladedancer"));
        add(BLADEDANCER_SKILL_TREE, node("silver_arc", "Silver Arc", "Learn Silver Arc, hitting every enemy.", 1, 2, 2, List.of("crescent_cut", "flowing_guard"), Map.of(), new Ability("Silver Arc", 24, 12, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.AOE), "Bladedancer"));
        add(BLADEDANCER_SKILL_TREE, node("duelist_grace", "Duelist Grace", "+8 HP, +8 MP, +3 attack, and +1 defense.", 1, 2, 3, List.of("silver_arc"), Map.of("max_hp", 8, "max_mp", 8, "attack", 3, "defense", 1), null, "Bladedancer"));
        add(BLADEDANCER_SKILL_TREE, node("mirror_step", "Mirror Step", "Learn Mirror Step, a guarded repositioning stance.", 1, 0, 3, List.of("flowing_guard"), Map.of(), new Ability("Mirror Step", 0, 7, Ability.AbilityKind.DEFEND, "self"), "Bladedancer"));
        add(BLADEDANCER_SKILL_TREE, node("finale_cut", "Finale Cut", "Learn Finale Cut, a heavy precision finisher.", 1, 4, 3, List.of("crescent_cut", "duelist_grace"), Map.of(), new Ability("Finale Cut", 48, 14), "Bladedancer"));
        add(BLADEDANCER_SKILL_TREE, node("storm_of_silver", "Storm of Silver", "+10 HP, +10 MP, +4 attack, and +1 defense.", 1, 2, 5, List.of("mirror_step", "finale_cut"), Map.of("max_hp", 10, "max_mp", 10, "attack", 4, "defense", 1), null, "Bladedancer"));

        add(VEILRUNNER_SKILL_TREE, node("veil_footwork", "Veil Footwork", "+1 attack and +1 defense per rank.", 3, 0, 0, List.of(), Map.of("attack", 1, "defense", 1), null, "Veilrunner"));
        add(VEILRUNNER_SKILL_TREE, node("smoke_cache", "Smoke Cache", "+5 max MP per rank.", 3, 2, 0, List.of(), Map.of("max_mp", 5), null, "Veilrunner"));
        add(VEILRUNNER_SKILL_TREE, node("blindside", "Blindside", "Learn Blindside, a fast rogue strike.", 1, 1, 1, List.of("veil_footwork"), Map.of(), new Ability("Blindside", 30, 8), "Veilrunner"));
        add(VEILRUNNER_SKILL_TREE, node("vanish_trick", "Vanish Trick", "Learn Vanish Trick, an evasive stance.", 1, 3, 1, List.of("smoke_cache"), Map.of(), new Ability("Vanish Trick", 0, 6, Ability.AbilityKind.DEFEND, "self"), "Veilrunner"));
        add(VEILRUNNER_SKILL_TREE, node("smoke_bomb", "Smoke Bomb", "Learn Smoke Bomb, striking every enemy.", 1, 2, 2, List.of("blindside", "vanish_trick"), Map.of(), new Ability("Smoke Bomb", 22, 11, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.AOE), "Veilrunner"));
        add(VEILRUNNER_SKILL_TREE, node("silent_route", "Silent Route", "+10 MP, +3 attack, and +2 defense.", 1, 2, 3, List.of("smoke_bomb"), Map.of("max_mp", 10, "attack", 3, "defense", 2), null, "Veilrunner"));
        add(VEILRUNNER_SKILL_TREE, node("hidden_wire", "Hidden Wire", "Learn Hidden Wire, a sharp low-cost strike.", 1, 0, 3, List.of("blindside"), Map.of(), new Ability("Hidden Wire", 31, 7), "Veilrunner"));
        add(VEILRUNNER_SKILL_TREE, node("knife_rain", "Knife Rain", "Learn Knife Rain, a wide attack from concealment.", 1, 4, 3, List.of("smoke_bomb", "silent_route"), Map.of(), new Ability("Knife Rain", 27, 13, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.MULTI_PROJECTILE), "Veilrunner"));
        add(VEILRUNNER_SKILL_TREE, node("exit_strategy", "Exit Strategy", "+12 MP, +4 attack, and +2 defense.", 1, 2, 5, List.of("hidden_wire", "knife_rain"), Map.of("max_mp", 12, "attack", 4, "defense", 2), null, "Veilrunner"));

        add(WILDSPEAKER_SKILL_TREE, node("root_lore", "Root Lore", "+6 max MP and +1 defense per rank.", 3, 0, 0, List.of(), Map.of("max_mp", 6, "defense", 1), null, "Wildspeaker"));
        add(WILDSPEAKER_SKILL_TREE, node("beast_courage", "Beast Courage", "+7 max HP per rank.", 3, 2, 0, List.of(), Map.of("max_hp", 7), null, "Wildspeaker"));
        add(WILDSPEAKER_SKILL_TREE, node("briar_call", "Briar Call", "Learn Briar Call, a druidic attack.", 1, 1, 1, List.of("root_lore"), Map.of(), new Ability("Briar Call", 28, 8), "Wildspeaker"));
        add(WILDSPEAKER_SKILL_TREE, node("green_mend", "Green Mend", "Learn Green Mend, a restorative ally spell.", 1, 3, 1, List.of("beast_courage"), Map.of(), new Ability("Green Mend", 28, 9, Ability.AbilityKind.HEAL, "ally"), "Wildspeaker"));
        add(WILDSPEAKER_SKILL_TREE, node("grove_chorus", "Grove Chorus", "Learn Grove Chorus, a party heal.", 1, 2, 2, List.of("briar_call", "green_mend"), Map.of(), new Ability("Grove Chorus", 18, 12, Ability.AbilityKind.HEAL, "party"), "Wildspeaker"));
        add(WILDSPEAKER_SKILL_TREE, node("elder_bark", "Elder Bark", "+12 HP, +12 MP, and +2 defense.", 1, 2, 3, List.of("grove_chorus"), Map.of("max_hp", 12, "max_mp", 12, "defense", 2), null, "Wildspeaker"));
        add(WILDSPEAKER_SKILL_TREE, node("briar_surge", "Briar Surge", "Learn Briar Surge, a stronger druidic attack.", 1, 0, 3, List.of("briar_call"), Map.of(), new Ability("Briar Surge", 42, 12), "Wildspeaker"));
        add(WILDSPEAKER_SKILL_TREE, node("grove_shelter", "Grove Shelter", "Learn Grove Shelter, a protective stance.", 1, 4, 3, List.of("green_mend", "elder_bark"), Map.of(), new Ability("Grove Shelter", 0, 8, Ability.AbilityKind.DEFEND, "self"), "Wildspeaker"));
        add(WILDSPEAKER_SKILL_TREE, node("wild_covenant", "Wild Covenant", "+14 HP, +14 MP, +1 attack, and +2 defense.", 1, 2, 5, List.of("briar_surge", "grove_shelter"), Map.of("max_hp", 14, "max_mp", 14, "attack", 1, "defense", 2), null, "Wildspeaker"));

        add(SUNWARDEN_SKILL_TREE, node("sun_vow", "Sun Vow", "+8 max HP and +1 defense per rank.", 3, 0, 0, List.of(), Map.of("max_hp", 8, "defense", 1), null, "Sunwarden"));
        add(SUNWARDEN_SKILL_TREE, node("lantern_prayer", "Lantern Prayer", "+5 max MP per rank.", 3, 2, 0, List.of(), Map.of("max_mp", 5), null, "Sunwarden"));
        add(SUNWARDEN_SKILL_TREE, node("dawn_mace", "Dawn Mace", "Learn Dawn Mace, a radiant strike.", 1, 1, 1, List.of("sun_vow"), Map.of(), new Ability("Dawn Mace", 30, 8), "Sunwarden"));
        add(SUNWARDEN_SKILL_TREE, node("lantern_aegis", "Lantern Aegis", "Learn Lantern Aegis, a guarded stance.", 1, 3, 1, List.of("lantern_prayer"), Map.of(), new Ability("Lantern Aegis", 0, 7, Ability.AbilityKind.DEFEND, "self"), "Sunwarden"));
        add(SUNWARDEN_SKILL_TREE, node("sunlit_mend", "Sunlit Mend", "Learn Sunlit Mend, a shielded ally heal.", 1, 2, 2, List.of("dawn_mace", "lantern_aegis"), Map.of(), new Ability("Sunlit Mend", 32, 11, Ability.AbilityKind.HEAL, "ally"), "Sunwarden"));
        add(SUNWARDEN_SKILL_TREE, node("solar_oath", "Solar Oath", "+16 HP, +8 MP, +1 attack, and +2 defense.", 1, 2, 3, List.of("sunlit_mend"), Map.of("max_hp", 16, "max_mp", 8, "attack", 1, "defense", 2), null, "Sunwarden"));
        add(SUNWARDEN_SKILL_TREE, node("solar_flare", "Solar Flare", "Learn Solar Flare, a burst against all foes.", 1, 0, 3, List.of("dawn_mace"), Map.of(), new Ability("Solar Flare", 25, 12, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.AOE), "Sunwarden"));
        add(SUNWARDEN_SKILL_TREE, node("dawn_shelter", "Dawn Shelter", "Learn Dawn Shelter, a party-wide restorative ward.", 1, 4, 3, List.of("lantern_aegis", "sunlit_mend"), Map.of(), new Ability("Dawn Shelter", 22, 14, Ability.AbilityKind.HEAL, "party"), "Sunwarden"));
        add(SUNWARDEN_SKILL_TREE, node("crown_of_light", "Crown of Light", "+18 HP, +10 MP, +2 attack, and +2 defense.", 1, 2, 5, List.of("solar_flare", "dawn_shelter"), Map.of("max_hp", 18, "max_mp", 10, "attack", 2, "defense", 2), null, "Sunwarden"));

        add(THORNBINDER_SKILL_TREE, node("thorn_lattice", "Thorn Lattice", "+1 attack and +5 max MP per rank.", 3, 0, 0, List.of(), Map.of("attack", 1, "max_mp", 5), null, "Thornbinder"));
        add(THORNBINDER_SKILL_TREE, node("bark_skin", "Bark Skin", "+1 defense and +6 max HP per rank.", 3, 2, 0, List.of(), Map.of("defense", 1, "max_hp", 6), null, "Thornbinder"));
        add(THORNBINDER_SKILL_TREE, node("thorn_snare", "Thorn Snare", "Learn Thorn Snare, a weakening druid strike.", 1, 1, 1, List.of("thorn_lattice"), Map.of(), new Ability("Thorn Snare", 30, 9), "Thornbinder"));
        add(THORNBINDER_SKILL_TREE, node("barbed_bloom", "Barbed Bloom", "Learn Barbed Bloom, hitting every enemy.", 1, 3, 1, List.of("bark_skin"), Map.of(), new Ability("Barbed Bloom", 24, 12, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.AOE), "Thornbinder"));
        add(THORNBINDER_SKILL_TREE, node("root_cage", "Root Cage", "+6 max MP and +2 defense.", 2, 2, 2, List.of("thorn_snare", "barbed_bloom"), Map.of("max_mp", 6, "defense", 2), null, "Thornbinder"));
        add(THORNBINDER_SKILL_TREE, node("wild_judgment", "Wild Judgment", "+12 HP, +12 MP, and +3 attack.", 1, 2, 3, List.of("root_cage"), Map.of("max_hp", 12, "max_mp", 12, "attack", 3), null, "Thornbinder"));
        add(THORNBINDER_SKILL_TREE, node("thorn_wall", "Thorn Wall", "Learn Thorn Wall, a guarded binding stance.", 1, 0, 3, List.of("bark_skin", "root_cage"), Map.of(), new Ability("Thorn Wall", 0, 8, Ability.AbilityKind.DEFEND, "self"), "Thornbinder"));
        add(THORNBINDER_SKILL_TREE, node("bramble_verdict", "Bramble Verdict", "Learn Bramble Verdict, a heavy thorn strike.", 1, 4, 3, List.of("thorn_snare", "wild_judgment"), Map.of(), new Ability("Bramble Verdict", 45, 13), "Thornbinder"));
        add(THORNBINDER_SKILL_TREE, node("verdant_doom", "Verdant Doom", "+12 HP, +16 MP, +4 attack, and +1 defense.", 1, 2, 5, List.of("thorn_wall", "bramble_verdict"), Map.of("max_hp", 12, "max_mp", 16, "attack", 4, "defense", 1), null, "Thornbinder"));

        add(STONEBREAKER_SKILL_TREE, node("stone_sinew", "Stone Sinew", "+9 max HP per rank.", 3, 0, 0, List.of(), Map.of("max_hp", 9), null, "Stonebreaker"));
        add(STONEBREAKER_SKILL_TREE, node("hammer_drills", "Hammer Drills", "+2 attack per rank.", 3, 2, 0, List.of(), Map.of("attack", 2), null, "Stonebreaker"));
        add(STONEBREAKER_SKILL_TREE, node("faultline_swing", "Faultline Swing", "Learn Faultline Swing, a bruising fighter attack.", 1, 1, 1, List.of("hammer_drills"), Map.of(), new Ability("Faultline Swing", 38, 10), "Stonebreaker"));
        add(STONEBREAKER_SKILL_TREE, node("braced_shoulders", "Braced Shoulders", "+2 defense per rank.", 2, 3, 1, List.of("stone_sinew"), Map.of("defense", 2), null, "Stonebreaker"));
        add(STONEBREAKER_SKILL_TREE, node("quake_blow", "Quake Blow", "Learn Quake Blow, hitting every enemy.", 1, 2, 2, List.of("faultline_swing", "braced_shoulders"), Map.of(), new Ability("Quake Blow", 26, 13, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.AOE), "Stonebreaker"));
        add(STONEBREAKER_SKILL_TREE, node("mountain_heart", "Mountain Heart", "+18 HP, +3 attack, and +2 defense.", 1, 2, 3, List.of("quake_blow"), Map.of("max_hp", 18, "attack", 3, "defense", 2), null, "Stonebreaker"));
        add(STONEBREAKER_SKILL_TREE, node("stoneguard", "Stoneguard", "Learn Stoneguard, a rooted defensive stance.", 1, 0, 3, List.of("braced_shoulders"), Map.of(), new Ability("Stoneguard", 0, 8, Ability.AbilityKind.DEFEND, "self"), "Stonebreaker"));
        add(STONEBREAKER_SKILL_TREE, node("avalanche_swing", "Avalanche Swing", "Learn Avalanche Swing, a heavy attack against all foes.", 1, 4, 3, List.of("quake_blow", "mountain_heart"), Map.of(), new Ability("Avalanche Swing", 30, 15, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.AOE), "Stonebreaker"));
        add(STONEBREAKER_SKILL_TREE, node("bedrock_legend", "Bedrock Legend", "+22 HP, +4 attack, and +2 defense.", 1, 2, 5, List.of("stoneguard", "avalanche_swing"), Map.of("max_hp", 22, "attack", 4, "defense", 2), null, "Stonebreaker"));

        add(NIGHTBLADE_SKILL_TREE, node("night_training", "Night Training", "+2 attack per rank.", 3, 0, 0, List.of(), Map.of("attack", 2), null, "Nightblade"));
        add(NIGHTBLADE_SKILL_TREE, node("black_cloak", "Black Cloak", "+1 defense and +4 max MP per rank.", 3, 2, 0, List.of(), Map.of("defense", 1, "max_mp", 4), null, "Nightblade"));
        add(NIGHTBLADE_SKILL_TREE, node("garrote_cut", "Garrote Cut", "Learn Garrote Cut, a lethal rogue strike.", 1, 1, 1, List.of("night_training"), Map.of(), new Ability("Garrote Cut", 35, 9), "Nightblade"));
        add(NIGHTBLADE_SKILL_TREE, node("poison_cache", "Poison Cache", "+5 max MP and +1 attack per rank.", 2, 3, 1, List.of("black_cloak"), Map.of("max_mp", 5, "attack", 1), null, "Nightblade"));
        add(NIGHTBLADE_SKILL_TREE, node("midnight_knife", "Midnight Knife", "Learn Midnight Knife, a shadowy burst attack.", 1, 2, 2, List.of("garrote_cut", "poison_cache"), Map.of(), new Ability("Midnight Knife", 44, 13), "Nightblade"));
        add(NIGHTBLADE_SKILL_TREE, node("perfect_silence", "Perfect Silence", "+10 MP, +4 attack, and +1 defense.", 1, 2, 3, List.of("midnight_knife"), Map.of("max_mp", 10, "attack", 4, "defense", 1), null, "Nightblade"));
        add(NIGHTBLADE_SKILL_TREE, node("nocturne_veil", "Nocturne Veil", "Learn Nocturne Veil, a guarded shadow stance.", 1, 0, 3, List.of("black_cloak"), Map.of(), new Ability("Nocturne Veil", 0, 7, Ability.AbilityKind.DEFEND, "self"), "Nightblade"));
        add(NIGHTBLADE_SKILL_TREE, node("eclipse_flurry", "Eclipse Flurry", "Learn Eclipse Flurry, a wide shadow assault.", 1, 4, 3, List.of("midnight_knife", "perfect_silence"), Map.of(), new Ability("Eclipse Flurry", 29, 14, Ability.AbilityKind.DAMAGE, "all_enemies").withVisualResolution(Ability.VisualResolution.MULTI_PROJECTILE), "Nightblade"));
        add(NIGHTBLADE_SKILL_TREE, node("dusk_sovereign", "Dusk Sovereign", "+12 MP, +5 attack, and +2 defense.", 1, 2, 5, List.of("nocturne_veil", "eclipse_flurry"), Map.of("max_mp", 12, "attack", 5, "defense", 2), null, "Nightblade"));

        add(GROVEKEEPER_SKILL_TREE, node("gentle_roots", "Gentle Roots", "+6 max MP and +1 defense per rank.", 3, 0, 0, List.of(), Map.of("max_mp", 6, "defense", 1), null, "Grovekeeper"));
        add(GROVEKEEPER_SKILL_TREE, node("spring_satchel", "Spring Satchel", "+7 max HP per rank.", 3, 2, 0, List.of(), Map.of("max_hp", 7), null, "Grovekeeper"));
        add(GROVEKEEPER_SKILL_TREE, node("petal_mend", "Petal Mend", "Learn Petal Mend, an efficient ally heal.", 1, 1, 1, List.of("gentle_roots"), Map.of(), new Ability("Petal Mend", 30, 9, Ability.AbilityKind.HEAL, "ally"), "Grovekeeper"));
        add(GROVEKEEPER_SKILL_TREE, node("thorn_whisper", "Thorn Whisper", "Learn Thorn Whisper, a light druid attack.", 1, 3, 1, List.of("spring_satchel"), Map.of(), new Ability("Thorn Whisper", 26, 8), "Grovekeeper"));
        add(GROVEKEEPER_SKILL_TREE, node("renewing_grove", "Renewing Grove", "Learn Renewing Grove, a party-wide heal.", 1, 2, 2, List.of("petal_mend", "thorn_whisper"), Map.of(), new Ability("Renewing Grove", 21, 13, Ability.AbilityKind.HEAL, "party"), "Grovekeeper"));
        add(GROVEKEEPER_SKILL_TREE, node("heartwood_keeper", "Heartwood Keeper", "+14 HP, +14 MP, and +2 defense.", 1, 2, 3, List.of("renewing_grove"), Map.of("max_hp", 14, "max_mp", 14, "defense", 2), null, "Grovekeeper"));
        add(GROVEKEEPER_SKILL_TREE, node("canopy_ward", "Canopy Ward", "Learn Canopy Ward, a defensive growth stance.", 1, 0, 3, List.of("gentle_roots", "renewing_grove"), Map.of(), new Ability("Canopy Ward", 0, 8, Ability.AbilityKind.DEFEND, "self"), "Grovekeeper"));
        add(GROVEKEEPER_SKILL_TREE, node("bloom_rite", "Bloom Rite", "Learn Bloom Rite, a stronger party heal.", 1, 4, 3, List.of("petal_mend", "heartwood_keeper"), Map.of(), new Ability("Bloom Rite", 26, 15, Ability.AbilityKind.HEAL, "party"), "Grovekeeper"));
        add(GROVEKEEPER_SKILL_TREE, node("ancient_grove", "Ancient Grove", "+16 HP, +16 MP, +1 attack, and +2 defense.", 1, 2, 5, List.of("canopy_ward", "bloom_rite"), Map.of("max_hp", 16, "max_mp", 16, "attack", 1, "defense", 2), null, "Grovekeeper"));

        addDeepClassSkillNodes();

        SKILL_TREE.putAll(COMMON_SKILL_TREE);
        SKILL_TREE.putAll(KNIGHT_SKILL_TREE);
        SKILL_TREE.putAll(MAGE_SKILL_TREE);
        SKILL_TREE.putAll(RANGER_SKILL_TREE);
        SKILL_TREE.putAll(CLERIC_SKILL_TREE);
        SKILL_TREE.putAll(ROGUE_SKILL_TREE);
        SKILL_TREE.putAll(PROFESSION_SKILL_TREE);
        SKILL_TREE.putAll(BATTLE_MEDIC_SKILL_TREE);
        SKILL_TREE.putAll(IRONWALL_SKILL_TREE);
        SKILL_TREE.putAll(BLADEDANCER_SKILL_TREE);
        SKILL_TREE.putAll(VEILRUNNER_SKILL_TREE);
        SKILL_TREE.putAll(WILDSPEAKER_SKILL_TREE);
        SKILL_TREE.putAll(SUNWARDEN_SKILL_TREE);
        SKILL_TREE.putAll(THORNBINDER_SKILL_TREE);
        SKILL_TREE.putAll(STONEBREAKER_SKILL_TREE);
        SKILL_TREE.putAll(NIGHTBLADE_SKILL_TREE);
        SKILL_TREE.putAll(GROVEKEEPER_SKILL_TREE);
        SKILL_ABILITY_NAMES = SKILL_TREE.values().stream()
                .filter(node -> node.ability() != null)
                .map(node -> node.ability().name())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private SkillTrees() {
    }

    public static Map<String, SkillNode> skillTreeForClass(String className) {
        return switch (className) {
            case "Mage" -> MAGE_SKILL_TREE;
            case "Ranger" -> RANGER_SKILL_TREE;
            case "Knight" -> KNIGHT_SKILL_TREE;
            case "Cleric" -> CLERIC_SKILL_TREE;
            case "Rogue" -> ROGUE_SKILL_TREE;
            case "Battle Medic" -> BATTLE_MEDIC_SKILL_TREE;
            case "Ironwall" -> IRONWALL_SKILL_TREE;
            case "Bladedancer" -> BLADEDANCER_SKILL_TREE;
            case "Veilrunner" -> VEILRUNNER_SKILL_TREE;
            case "Wildspeaker" -> WILDSPEAKER_SKILL_TREE;
            case "Sunwarden" -> SUNWARDEN_SKILL_TREE;
            case "Thornbinder" -> THORNBINDER_SKILL_TREE;
            case "Stonebreaker" -> STONEBREAKER_SKILL_TREE;
            case "Nightblade" -> NIGHTBLADE_SKILL_TREE;
            case "Grovekeeper" -> GROVEKEEPER_SKILL_TREE;
            case "Medic" -> CLERIC_SKILL_TREE;
            case "Archivist", "Snow Seer", "Marsh Witch" -> MAGE_SKILL_TREE;
            case "Captain", "Roadwarden" -> KNIGHT_SKILL_TREE;
            case "Frost Scout", "Dune Guide" -> RANGER_SKILL_TREE;
            default -> Map.of();
        };
    }

    public static Map<String, SkillNode> professionSkillTree(String professionId) {
        return PROFESSION_SKILL_TREES.getOrDefault(professionId, WOODCUTTING_PROFESSION_TREE);
    }

    public static Map<String, SkillNode> availableSkillTree(String className) {
        Map<String, SkillNode> available = new LinkedHashMap<>(COMMON_SKILL_TREE);
        available.putAll(skillTreeForClass(className));
        available.putAll(PROFESSION_SKILL_TREE);
        return available;
    }

    public static int spent(Map<String, Integer> allocations) {
        return allocations.values().stream().mapToInt(value -> Math.max(0, value)).sum();
    }

    public static boolean isProfessionSkill(String skillKey) {
        return PROFESSION_SKILL_TREE.containsKey(skillKey);
    }

    public static int professionSpent(Map<String, Integer> allocations) {
        return allocations.entrySet().stream()
                .filter(entry -> isProfessionSkill(entry.getKey()))
                .mapToInt(entry -> Math.max(0, entry.getValue()))
                .sum();
    }

    public static int respecCost(int level, int spent) {
        return 35 + Math.max(0, level - 1) * 8 + Math.max(0, spent) * 12;
    }

    private static void add(Map<String, SkillNode> tree, SkillNode node) {
        tree.put(node.id(), node);
    }

    private static void addProfession(Map<String, SkillNode> tree, SkillNode node) {
        add(tree, node);
        add(PROFESSION_SKILL_TREE, node);
    }

    private static SkillNode node(String id, String name, String description, int maxRank, int x, int y,
                                  List<String> requires, Map<String, Integer> effects, Ability ability, String tree) {
        int ranks = ability != null && maxRank == 1 ? 3 : maxRank;
        return new SkillNode(id, name, description, ranks, x, y, requires, effects, ability, tree);
    }

    private static SkillNode nodeAtLevel(String id, String name, String description, int maxRank, int levelRequirement,
                                         int x, int y, List<String> requires, Map<String, Integer> effects,
                                         Ability ability, String tree) {
        int ranks = ability != null && maxRank == 1 ? 3 : maxRank;
        return new SkillNode(id, name, description, ranks, levelRequirement, x, y, requires, effects, ability, tree);
    }

    private static void addDeepClassSkillNodes() {
        addMastery(KNIGHT_SKILL_TREE, "Knight", "knight", "vanguard_legend", "Iron", "Hold the Line", "Royal Cleave", "Oathbound Bulwark", "Crown Splitter", Map.of("max_hp", 8, "defense", 1), Map.of("attack", 1, "defense", 1));
        addMastery(MAGE_SKILL_TREE, "Mage", "mage", "star_savant", "Astral", "Ley Focus", "Comet Spiral", "Star Mantle", "Worldfire Sigil", Map.of("max_mp", 8, "attack", 1), Map.of("max_mp", 5, "defense", 1));
        addMastery(RANGER_SKILL_TREE, "Ranger", "ranger", "wildwarden", "Warden", "Trail Tempo", "Storm Volley", "Camouflage Break", "Skyfang Barrage", Map.of("attack", 1, "max_mp", 4), Map.of("max_hp", 5, "defense", 1));
        addMastery(CLERIC_SKILL_TREE, "Cleric", "cleric", "saintly_oath", "Saint", "Hymnal Study", "Dawnfall", "Consecrated Circle", "Seraphic Hymn", Map.of("max_mp", 7, "defense", 1), Map.of("max_hp", 5, "max_mp", 5));
        addMastery(ROGUE_SKILL_TREE, "Rogue", "rogue", "shadow_mastery", "Shadow", "Cutpurse Flow", "Blackout Blades", "Vanish Protocol", "King's Last Whisper", Map.of("attack", 2, "max_mp", 2), Map.of("defense", 1, "max_mp", 4));
        addMastery(BATTLE_MEDIC_SKILL_TREE, "Battle Medic", "medic", "surgeon_general", "Medic", "Pressure Drill", "Bone Saw Sweep", "Emergency Rotation", "Miracle Triage", Map.of("max_mp", 6, "defense", 1), Map.of("max_hp", 4, "max_mp", 4));
        addMastery(IRONWALL_SKILL_TREE, "Ironwall", "ironwall", "living_rampart", "Rampart", "Brace Cycle", "Ironquake", "Redoubt Protocol", "Citadel Breaker", Map.of("max_hp", 9, "defense", 1), Map.of("attack", 1, "defense", 2));
        addMastery(BLADEDANCER_SKILL_TREE, "Bladedancer", "dancer", "storm_of_silver", "Silver", "Footfall Cadence", "Ribbon Storm", "Silken Guard", "Finale of Knives", Map.of("attack", 1, "defense", 1), Map.of("attack", 2, "max_mp", 3));
        addMastery(VEILRUNNER_SKILL_TREE, "Veilrunner", "veilrunner", "exit_strategy", "Veil", "Exit Tempo", "Glass Smoke", "Hidden Route", "Null Step", Map.of("attack", 1, "max_mp", 5), Map.of("defense", 1, "max_mp", 4));
        addMastery(WILDSPEAKER_SKILL_TREE, "Wildspeaker", "wildspeaker", "wild_covenant", "Wild", "Root Accord", "Briar Tempest", "Old Growth Veil", "Elderwood Judgment", Map.of("max_hp", 5, "max_mp", 5), Map.of("attack", 1, "defense", 1));
        addMastery(SUNWARDEN_SKILL_TREE, "Sunwarden", "sunwarden", "crown_of_light", "Solar", "Votive Rhythm", "Sunburst Road", "Halo Bastion", "Noonstar Edict", Map.of("max_hp", 7, "defense", 1), Map.of("attack", 1, "max_mp", 5));
        addMastery(THORNBINDER_SKILL_TREE, "Thornbinder", "thornbinder", "verdant_doom", "Thorn", "Barbcraft", "Bramble Storm", "Rooted Veil", "Verdant Sentence", Map.of("attack", 1, "max_mp", 6), Map.of("defense", 1, "max_hp", 5));
        addMastery(STONEBREAKER_SKILL_TREE, "Stonebreaker", "stonebreaker", "bedrock_legend", "Stone", "Fault Reader", "Mountain Collapse", "Granite Brace", "Worldsplitter", Map.of("max_hp", 8, "attack", 1), Map.of("attack", 1, "defense", 1));
        addMastery(NIGHTBLADE_SKILL_TREE, "Nightblade", "nightblade", "dusk_sovereign", "Dusk", "Silent Pulse", "Eclipse Rain", "Night Mantle", "Moonless Verdict", Map.of("attack", 2, "max_mp", 3), Map.of("defense", 1, "max_mp", 5));
        addMastery(GROVEKEEPER_SKILL_TREE, "Grovekeeper", "grovekeeper", "ancient_grove", "Grove", "Sapflow Study", "Petal Tempest", "Canopy Circle", "Primeval Bloom", Map.of("max_hp", 5, "max_mp", 6), Map.of("defense", 1, "max_mp", 5));
        addBranchingMasteries();
    }

    private static void addMastery(Map<String, SkillNode> tree, String treeName, String prefix, String anchor,
                                   String theme, String passiveName, String areaName, String guardName,
                                   String ultimateName, Map<String, Integer> firstEffects, Map<String, Integer> secondEffects) {
        add(tree, nodeAtLevel(prefix + "_mastery_drills", theme + " Drills", "+scaling combat stats per rank.", 4, 24, 0, 6, List.of(anchor), firstEffects, null, treeName));
        add(tree, nodeAtLevel(prefix + "_signature_forms", passiveName, "+deeper class scaling and resource control per rank.", 4, 30, 1, 6, List.of(anchor), secondEffects, null, treeName));
        add(tree, nodeAtLevel(prefix + "_area_art", areaName, "Upgradeable multi-target class art with a tactical cooldown.", 3, 36, 2, 6, List.of(prefix + "_mastery_drills"), Map.of(), masteryAreaAbility(treeName, areaName), treeName));
        add(tree, nodeAtLevel(prefix + "_guard_art", guardName, "Upgradeable defensive stance for dangerous turns.", 3, 42, 3, 6, List.of(prefix + "_signature_forms"), Map.of(), masteryGuardAbility(treeName, guardName), treeName));
        add(tree, nodeAtLevel(prefix + "_apex_training", theme + " Apex", "+major late-tree stats per rank.", 3, 50, 1, 7, List.of(prefix + "_area_art", prefix + "_guard_art"), Map.of("max_hp", 5, "max_mp", 5, "attack", 1, "defense", 1), null, treeName));
        add(tree, nodeAtLevel(prefix + "_ultimate_art", ultimateName, "A level-60 capstone that gathers the tree's final oath into one overwhelming art.", 3, 60, 2, 8, List.of(prefix + "_apex_training"), Map.of(), masteryUltimateAbility(treeName, ultimateName), treeName));
    }

    private record BranchPath(
            String key,
            String title,
            String description,
            Map<String, Integer> initiateEffects,
            Map<String, Integer> masteryEffects,
            Ability art,
            Ability apex
    ) {
    }

    private static void addBranchingMasteries() {
        addBranches(KNIGHT_SKILL_TREE, "Knight", "knight", "vanguard_legend", List.of(
                branch("vanguard", "Vanguard", "front-line control and party-cover discipline.", Map.of("max_hp", 6, "defense", 1), Map.of("max_hp", 8, "defense", 1), guardArt("Line Anchor", 8, "party_guard"), damageArt("Crownward Cleave", 42, 17, "all_enemies", Ability.ScalingProfile.WEAPON, "cleave", "armor_breaker", new AbilityStatus("weak", "enemy", 0.65))),
                branch("duelist", "Duelist", "single-target pressure and exposed-opening punishment.", Map.of("attack", 1, "max_hp", 4), Map.of("attack", 2, "max_mp", 3), damageArt("Riposte Promise", 34, 12, "enemy", Ability.ScalingProfile.WEAPON, "slash", "consume_vulnerable", new AbilityStatus("vulnerable", "enemy")), damageArt("Final Challenge", 54, 20, "enemy", Ability.ScalingProfile.WEAPON, "slash", "execute", new AbilityStatus("vulnerable", "enemy"))),
                branch("bulwark", "Bulwark", "survival, mitigation, and long-fight tempo.", Map.of("max_hp", 8, "defense", 1), Map.of("max_hp", 10, "defense", 2), guardArt("Iron Refuge", 9, "party_guard"), guardArt("Unbroken Standard", 16, "party_guard")),
                branch("warlord", "Warlord", "morale-driven offense and sweeping command.", Map.of("attack", 1, "max_mp", 4), Map.of("attack", 1, "defense", 1, "max_mp", 5), damageArt("Banner Sweep", 30, 13, "all_enemies", Ability.ScalingProfile.WEAPON, "cleave", "", new AbilityStatus("vulnerable", "enemy", 0.45)), damageArt("Royal Order", 50, 19, "all_enemies", Ability.ScalingProfile.WEAPON, "prismatic", "armor_breaker", new AbilityStatus("vulnerable", "enemy", 0.7)))
        ));
        addBranches(MAGE_SKILL_TREE, "Mage", "mage", "star_savant", List.of(
                branch("fire", "Fire Mage", "burn stacking, blast windows, and explosive area pressure.", Map.of("attack", 1, "max_mp", 6), Map.of("attack", 2, "max_mp", 6), damageArt("Inferno Script", 34, 14, "all_enemies", Ability.ScalingProfile.ARCANE, "fire", "", new AbilityStatus("burn", "enemy", 0.85)), damageArt("Phoenix Equation", 56, 22, "all_enemies", Ability.ScalingProfile.ARCANE, "ember", "consume_vulnerable", new AbilityStatus("burn", "enemy"))),
                branch("ice", "Ice Mage", "weakening control and safer attrition.", Map.of("defense", 1, "max_mp", 6), Map.of("defense", 1, "max_mp", 8), damageArt("Glacier Prison", 28, 12, "all_enemies", Ability.ScalingProfile.ARCANE, "frost", "", new AbilityStatus("weak", "enemy")), damageArt("Absolute Winter", 48, 20, "all_enemies", Ability.ScalingProfile.ARCANE, "frost", "consume_vulnerable", new AbilityStatus("weak", "enemy"), new AbilityStatus("vulnerable", "enemy", 0.45))),
                branch("arcane", "Arcane Mage", "precise ley manipulation and exposed-opening conversion.", Map.of("max_mp", 9), Map.of("attack", 1, "max_mp", 10), damageArt("Ley Reversal", 38, 13, "enemy", Ability.ScalingProfile.ARCANE, "arcane", "consume_vulnerable", new AbilityStatus("vulnerable", "enemy", 0.65)), damageArt("Astral Verdict", 58, 22, "enemy", Ability.ScalingProfile.ARCANE, "arcane", "consume_vulnerable", new AbilityStatus("vulnerable", "enemy"))),
                branch("chaos", "Chaos Mage", "volatile multi-type bursts and high-risk capstones.", Map.of("attack", 1, "max_mp", 5), Map.of("attack", 2, "defense", 1), damageArt("Chaos Bloom", 32, 15, "all_enemies", Ability.ScalingProfile.ARCANE, "chaos", "", new AbilityStatus("vulnerable", "enemy", 0.4), new AbilityStatus("burn", "enemy", 0.35)), damageArt("Prismatic Collapse", 62, 25, "all_enemies", Ability.ScalingProfile.ARCANE, "prismatic", "armor_breaker", new AbilityStatus("vulnerable", "enemy", 0.75)))
        ));
        addBranches(RANGER_SKILL_TREE, "Ranger", "ranger", "wildwarden", List.of(
                branch("sharpshooter", "Sharpshooter", "clean shots, execution windows, and boss focus.", Map.of("attack", 2, "max_mp", 2), Map.of("attack", 2, "max_mp", 5), damageArt("Heartline Shot", 36, 13, "enemy", Ability.ScalingProfile.AGILITY, "volley", "execute", new AbilityStatus("vulnerable", "enemy", 0.5)), damageArt("Horizon Kill", 58, 21, "enemy", Ability.ScalingProfile.AGILITY, "volley", "execute", new AbilityStatus("vulnerable", "enemy"))),
                branch("trapper", "Trapper", "slows, snares, and weakness management.", Map.of("defense", 1, "max_mp", 4), Map.of("defense", 1, "attack", 1), damageArt("Snarefield", 24, 11, "all_enemies", Ability.ScalingProfile.AGILITY, "nature", "", new AbilityStatus("weak", "enemy", 0.9)), damageArt("Lockjaw Ground", 46, 19, "all_enemies", Ability.ScalingProfile.AGILITY, "nature", "consume_vulnerable", new AbilityStatus("weak", "enemy"))),
                branch("beastwarden", "Beastwarden", "pack tempo and party survival.", Map.of("max_hp", 6, "attack", 1), Map.of("max_hp", 8, "defense", 1), guardArt("Pack Cover", 8, "party_guard"), damageArt("Fang Chorus", 48, 18, "all_enemies", Ability.ScalingProfile.AGILITY, "howl", "poison_combo", new AbilityStatus("weak", "enemy", 0.55))),
                branch("stormcaller", "Stormcaller", "lightning volleys and fast multi-target pressure.", Map.of("attack", 1, "max_mp", 5), Map.of("attack", 2, "max_mp", 4), damageArt("Stormline Volley", 30, 13, "all_enemies", Ability.ScalingProfile.AGILITY, "lightning", "", Ability.VisualResolution.MULTI_PROJECTILE, new AbilityStatus("vulnerable", "enemy", 0.35)), damageArt("Skyfang Barrage", 54, 21, "all_enemies", Ability.ScalingProfile.AGILITY, "lightning", "consume_vulnerable", Ability.VisualResolution.MULTI_PROJECTILE, new AbilityStatus("weak", "enemy", 0.65)))
        ));
        addBranches(CLERIC_SKILL_TREE, "Cleric", "cleric", "saintly_oath", List.of(
                branch("life", "Life Binder", "strong restores, cleansing, and overheal shields.", Map.of("max_mp", 7, "defense", 1), Map.of("max_mp", 8, "max_hp", 5), healArt("Mercy Wellspring", 30, 14, "party", Ability.ScalingProfile.DIVINE, "heal", "cleanse"), healArt("Choir of Return", 44, 24, "party", Ability.ScalingProfile.DIVINE, "seraphic_hymn", "revive_party")),
                branch("aegis", "Aegis Saint", "wards, shields, and protected recovery.", Map.of("defense", 2, "max_mp", 3), Map.of("defense", 2, "max_hp", 6), guardArt("Aegis Circle", 10, "party_guard"), healArt("Sanctuary Pulse", 34, 18, "party", Ability.ScalingProfile.DIVINE, "ward", "overheal_shield")),
                branch("dawn", "Dawn Priest", "radiant pressure and exposed foes.", Map.of("attack", 1, "max_mp", 6), Map.of("attack", 2, "max_mp", 5), damageArt("Dawnfall", 34, 14, "all_enemies", Ability.ScalingProfile.DIVINE, "radiant", "", new AbilityStatus("vulnerable", "enemy", 0.7)), damageArt("Seraphic Spear", 56, 22, "enemy", Ability.ScalingProfile.DIVINE, "holy", "execute", new AbilityStatus("vulnerable", "enemy"))),
                branch("inquisitor", "Inquisitor", "single-target judgment and vulnerability conversion.", Map.of("attack", 1, "defense", 1), Map.of("attack", 2, "defense", 1), damageArt("Confession Brand", 36, 13, "enemy", Ability.ScalingProfile.DIVINE, "holy", "consume_vulnerable", new AbilityStatus("vulnerable", "enemy")), damageArt("Final Absolution", 58, 23, "enemy", Ability.ScalingProfile.DIVINE, "radiant", "execute", new AbilityStatus("vulnerable", "enemy")))
        ));
        addBranches(ROGUE_SKILL_TREE, "Rogue", "rogue", "shadow_mastery", rogueBranches("Rogue"));
        addBranches(BLADEDANCER_SKILL_TREE, "Bladedancer", "dancer", "storm_of_silver", rogueBranches("Bladedancer"));
        addBranches(VEILRUNNER_SKILL_TREE, "Veilrunner", "veilrunner", "exit_strategy", rogueBranches("Veilrunner"));
        addBranches(NIGHTBLADE_SKILL_TREE, "Nightblade", "nightblade", "dusk_sovereign", rogueBranches("Nightblade"));
        addBranches(BATTLE_MEDIC_SKILL_TREE, "Battle Medic", "medic", "surgeon_general", medicBranches());
        addBranches(IRONWALL_SKILL_TREE, "Ironwall", "ironwall", "living_rampart", ironwallBranches());
        addBranches(WILDSPEAKER_SKILL_TREE, "Wildspeaker", "wildspeaker", "wild_covenant", natureBranches("Wildspeaker"));
        addBranches(THORNBINDER_SKILL_TREE, "Thornbinder", "thornbinder", "verdant_doom", natureBranches("Thornbinder"));
        addBranches(GROVEKEEPER_SKILL_TREE, "Grovekeeper", "grovekeeper", "ancient_grove", natureBranches("Grovekeeper"));
        addBranches(SUNWARDEN_SKILL_TREE, "Sunwarden", "sunwarden", "crown_of_light", sunwardenBranches());
        addBranches(STONEBREAKER_SKILL_TREE, "Stonebreaker", "stonebreaker", "bedrock_legend", stonebreakerBranches());
    }

    private static List<BranchPath> rogueBranches(String treeName) {
        String mark = shortClassMark(treeName);
        return List.of(
                branch("assassin", "Assassin", "execute timing and marked-target finishers.", Map.of("attack", 2, "max_mp", 2), Map.of("attack", 2, "max_mp", 5), damageArt(mark + " Killing Angle", 38, 13, "enemy", Ability.ScalingProfile.AGILITY, "dark", "execute", new AbilityStatus("vulnerable", "enemy", 0.55)), damageArt(mark + " Last Breath Contract", 58, 22, "enemy", Ability.ScalingProfile.AGILITY, "dark", "execute", new AbilityStatus("vulnerable", "enemy"))),
                branch("saboteur", "Saboteur", "poisons, blinds, and team setup.", Map.of("attack", 1, "defense", 1), Map.of("attack", 1, "defense", 1, "max_mp", 4), damageArt(mark + " Black Powder Cut", 28, 12, "all_enemies", Ability.ScalingProfile.AGILITY, "dust", "", new AbilityStatus("weak", "enemy", 0.75), new AbilityStatus("poison", "enemy", 0.4)), damageArt(mark + " Ruin the Room", 50, 20, "all_enemies", Ability.ScalingProfile.AGILITY, "poison", "poison_combo", new AbilityStatus("poison", "enemy"), new AbilityStatus("weak", "enemy", 0.55))),
                branch("duelist", "Duelist", "parry-like defense and exposed-opening conversion.", Map.of("attack", 1, "max_hp", 4), Map.of("attack", 2, "defense", 1), damageArt(mark + " Needle Riposte", 36, 13, "enemy", Ability.ScalingProfile.AGILITY, "slash", "consume_vulnerable", new AbilityStatus("vulnerable", "enemy", 0.65)), guardArt(mark + " Perfect Exit", 8, "")),
                branch("shade", "Shade", "dark mobility, self-preservation, and late burst.", Map.of("defense", 1, "max_mp", 5), Map.of("attack", 1, "defense", 1, "max_mp", 5), guardArt(mark + " Shadow Double", 7, ""), damageArt(mark + " Moonless Verdict", 56, 22, "all_enemies", Ability.ScalingProfile.AGILITY, "dark", "execute", new AbilityStatus("vulnerable", "enemy", 0.75)))
        );
    }

    private static List<BranchPath> medicBranches() {
        return List.of(
                branch("surgeon", "Field Surgeon", "cleansing, emergency recovery, and triage throughput.", Map.of("max_mp", 6, "defense", 1), Map.of("max_mp", 8, "defense", 1), healArt("Sterile Rush", 30, 13, "ally", Ability.ScalingProfile.TRIAGE, "heal", "cleanse"), healArt("Miracle Triage", 44, 23, "party", Ability.ScalingProfile.TRIAGE, "heal", "revive_party")),
                branch("chemist", "Chemist", "party tonics and attrition control.", Map.of("max_mp", 7), Map.of("max_mp", 9, "attack", 1), healArt("Panacea Toss", 24, 14, "party", Ability.ScalingProfile.TRIAGE, "heal", "cleanse"), damageArt("Caustic Flask", 36, 14, "all_enemies", Ability.ScalingProfile.TRIAGE, "acid", "poison_combo", new AbilityStatus("poison", "enemy", 0.75))),
                branch("combatant", "Combatant", "medical strikes and weakness pressure.", Map.of("attack", 1, "defense", 1), Map.of("attack", 2, "max_hp", 5), damageArt("Bone Saw Line", 34, 13, "all_enemies", Ability.ScalingProfile.TRIAGE, "impact", "", new AbilityStatus("weak", "enemy", 0.75)), damageArt("Pressure Point Cascade", 54, 21, "enemy", Ability.ScalingProfile.TRIAGE, "impact", "execute", new AbilityStatus("vulnerable", "enemy"))),
                branch("stabilizer", "Stabilizer", "party shields and defensive recovery.", Map.of("defense", 2, "max_hp", 4), Map.of("defense", 2, "max_mp", 5), guardArt("Painkiller Wall", 9, "party_guard"), healArt("Emergency Rotation", 34, 18, "party", Ability.ScalingProfile.TRIAGE, "ward", "overheal_shield"))
        );
    }

    private static List<BranchPath> ironwallBranches() {
        return List.of(
                branch("citadel", "Citadel", "maximum party cover and immovable defense.", Map.of("max_hp", 9, "defense", 1), Map.of("max_hp", 10, "defense", 2), guardArt("Citadel Protocol", 11, "party_guard"), guardArt("Living Rampart", 18, "party_guard")),
                branch("breaker", "Breaker", "guard-scaled damage and armor breaking.", Map.of("attack", 1, "defense", 1), Map.of("attack", 2, "defense", 1), damageArt("Ironquake", 34, 14, "all_enemies", Ability.ScalingProfile.GUARD, "bash", "armor_breaker", new AbilityStatus("weak", "enemy", 0.65)), damageArt("Citadel Breaker", 58, 23, "enemy", Ability.ScalingProfile.GUARD, "bash", "armor_breaker", new AbilityStatus("vulnerable", "enemy"))),
                branch("sentinel", "Sentinel", "reactionary control and protective tempo.", Map.of("defense", 2, "max_mp", 3), Map.of("defense", 2, "max_mp", 5), damageArt("Warning Slam", 28, 11, "enemy", Ability.ScalingProfile.GUARD, "impact", "consume_vulnerable", new AbilityStatus("weak", "enemy")), guardArt("Redoubt Signal", 9, "party_guard")),
                branch("juggernaut", "Juggernaut", "slow overwhelming offense.", Map.of("attack", 2, "max_hp", 5), Map.of("attack", 2, "max_hp", 8), damageArt("Gatecrash", 40, 15, "enemy", Ability.ScalingProfile.GUARD, "bash", "armor_breaker", new AbilityStatus("vulnerable", "enemy", 0.55)), damageArt("Rampart Collapse", 58, 23, "all_enemies", Ability.ScalingProfile.GUARD, "cleave", "armor_breaker", new AbilityStatus("weak", "enemy")))
        );
    }

    private static List<BranchPath> natureBranches(String treeName) {
        String mark = shortClassMark(treeName);
        return List.of(
                branch("warden", "Warden", "party protection through living roots.", Map.of("max_hp", 6, "defense", 1), Map.of("max_hp", 8, "defense", 1), guardArt(mark + " Canopy Circle", 9, "party_guard"), healArt(mark + " Old Growth Veil", 30, 18, "party", Ability.ScalingProfile.NATURE, "nature", "overheal_shield")),
                branch("bloom", "Bloomcaller", "healing throughput and revival bloom.", Map.of("max_mp", 7, "defense", 1), Map.of("max_mp", 8, "max_hp", 5), healArt(mark + " Petal Chorus", 30, 14, "party", Ability.ScalingProfile.NATURE, "heal", "overheal_shield"), healArt(mark + " Primeval Bloom", 42, 24, "party", Ability.ScalingProfile.NATURE, "nature", "revive_party")),
                branch("thorn", "Thornheart", "poison, brambles, and punishing control.", Map.of("attack", 1, "max_mp", 5), Map.of("attack", 2, "max_mp", 5), damageArt(mark + " Briar Tempest", 32, 14, "all_enemies", Ability.ScalingProfile.NATURE, "nature", "poison_combo", new AbilityStatus("poison", "enemy", 0.65), new AbilityStatus("weak", "enemy", 0.45)), damageArt(mark + " Verdant Sentence", 56, 22, "all_enemies", Ability.ScalingProfile.NATURE, "thorn", "poison_combo", new AbilityStatus("poison", "enemy"), new AbilityStatus("vulnerable", "enemy", 0.55))),
                branch("wild", "Wild Covenant", "beast tempo and aggressive nature damage.", Map.of("attack", 1, "max_hp", 5), Map.of("attack", 2, "defense", 1), damageArt(mark + " Elderwood Judgment", 40, 16, "enemy", Ability.ScalingProfile.NATURE, "nature", "consume_vulnerable", new AbilityStatus("vulnerable", "enemy", 0.65)), damageArt(mark + " Ancient Herdcall", 54, 21, "all_enemies", Ability.ScalingProfile.NATURE, "howl", "execute", new AbilityStatus("weak", "enemy")))
        );
    }

    private static String shortClassMark(String treeName) {
        return switch (treeName) {
            case "Bladedancer" -> "Silver";
            case "Veilrunner" -> "Veil";
            case "Nightblade" -> "Dusk";
            case "Wildspeaker" -> "Wild";
            case "Thornbinder" -> "Thorn";
            case "Grovekeeper" -> "Grove";
            default -> treeName;
        };
    }

    private static List<BranchPath> sunwardenBranches() {
        return List.of(
                branch("solar", "Solar Judge", "radiant exposure and burn pressure.", Map.of("attack", 1, "max_mp", 6), Map.of("attack", 2, "max_mp", 5), damageArt("Sunburst Road", 34, 14, "all_enemies", Ability.ScalingProfile.DIVINE, "radiant", "", new AbilityStatus("burn", "enemy", 0.55), new AbilityStatus("vulnerable", "enemy", 0.45)), damageArt("Noonstar Edict", 58, 23, "all_enemies", Ability.ScalingProfile.DIVINE, "radiant", "consume_vulnerable", new AbilityStatus("vulnerable", "enemy"))),
                branch("lantern", "Lantern Keeper", "healing light and steadied allies.", Map.of("max_mp", 7, "defense", 1), Map.of("max_hp", 6, "max_mp", 7), healArt("Lantern Refuge", 30, 15, "party", Ability.ScalingProfile.DIVINE, "heal", "overheal_shield"), healArt("Pilgrim Sun", 42, 23, "party", Ability.ScalingProfile.DIVINE, "radiant", "cleanse")),
                branch("halo", "Halo Bastion", "party-cover wards and durable light.", Map.of("defense", 2, "max_hp", 5), Map.of("defense", 2, "max_mp", 5), guardArt("Halo Bastion", 10, "party_guard"), healArt("Gold Ward", 34, 18, "party", Ability.ScalingProfile.DIVINE, "ward", "overheal_shield")),
                branch("zeal", "Zealot", "close-range holy force and execution.", Map.of("attack", 2, "max_hp", 4), Map.of("attack", 2, "defense", 1), damageArt("Censer Crush", 38, 13, "enemy", Ability.ScalingProfile.DIVINE, "impact", "consume_vulnerable", new AbilityStatus("vulnerable", "enemy")), damageArt("White Flame Verdict", 58, 22, "enemy", Ability.ScalingProfile.DIVINE, "holy", "execute", new AbilityStatus("burn", "enemy")))
        );
    }

    private static List<BranchPath> stonebreakerBranches() {
        return List.of(
                branch("fault", "Fault Reader", "targeted armor breaking and expose windows.", Map.of("attack", 1, "defense", 1), Map.of("attack", 2, "defense", 1), damageArt("Faultline Pick", 38, 13, "enemy", Ability.ScalingProfile.WEAPON, "bash", "armor_breaker", new AbilityStatus("vulnerable", "enemy")), damageArt("Worldsplitter", 62, 23, "enemy", Ability.ScalingProfile.WEAPON, "prismatic", "armor_breaker", new AbilityStatus("vulnerable", "enemy"), new AbilityStatus("weak", "enemy", 0.5))),
                branch("avalanche", "Avalanche", "heavy area strikes and battlefield control.", Map.of("attack", 2, "max_hp", 4), Map.of("attack", 2, "max_hp", 7), damageArt("Mountain Collapse", 36, 15, "all_enemies", Ability.ScalingProfile.WEAPON, "bash", "armor_breaker", new AbilityStatus("weak", "enemy", 0.65)), damageArt("Avalanche Sentence", 58, 22, "all_enemies", Ability.ScalingProfile.WEAPON, "cleave", "armor_breaker", new AbilityStatus("vulnerable", "enemy", 0.65))),
                branch("granite", "Granite Oath", "guard discipline and stubborn survival.", Map.of("max_hp", 8, "defense", 1), Map.of("max_hp", 10, "defense", 2), guardArt("Granite Brace", 9, "party_guard"), guardArt("Bedrock Shelter", 16, "party_guard")),
                branch("titan", "Titan", "slow boss-crushing single hits.", Map.of("attack", 2, "max_mp", 3), Map.of("attack", 3, "max_hp", 4), damageArt("Titan Hammer", 44, 16, "enemy", Ability.ScalingProfile.WEAPON, "impact", "consume_vulnerable", new AbilityStatus("vulnerable", "enemy", 0.5)), damageArt("Continental Break", 64, 24, "enemy", Ability.ScalingProfile.WEAPON, "bash", "execute", new AbilityStatus("weak", "enemy")))
        );
    }

    private static BranchPath branch(String key, String title, String description, Map<String, Integer> initiateEffects,
                                     Map<String, Integer> masteryEffects, Ability art, Ability apex) {
        return new BranchPath(key, title, description, initiateEffects, masteryEffects, art, apex);
    }

    private static void addBranches(Map<String, SkillNode> tree, String treeName, String prefix, String anchor, List<BranchPath> branches) {
        for (int i = 0; i < branches.size(); i++) {
            BranchPath branch = branches.get(i);
            Ability branchArt = uniqueTreeAbility(tree, branch.art(), branch.title());
            Ability branchApex = uniqueTreeAbility(tree, branch.apex(), branch.title());
            String id = prefix + "_" + branch.key();
            int band = i / 2;
            int mainX = (i % 2) * 3;
            int sideX = mainX + 1;
            int baseY = 9 + band * 5;
            add(tree, nodeAtLevel(id + "_initiate", branch.title() + " Initiate",
                    "Begin the " + branch.title() + " lane: " + branch.description(), 3, 18, mainX, baseY,
                    List.of(anchor), branch.initiateEffects(), null, treeName));
            add(tree, nodeAtLevel(id + "_art", branchArt.name(),
                    "Signature " + branch.title() + " art. Ranks improve the ability while keeping its cooldown and scaling identity.", 3, 28, mainX, baseY + 1,
                    List.of(id + "_initiate"), Map.of(), branchArt, treeName));
            add(tree, nodeAtLevel(id + "_technique", branch.title() + " Technique",
                    "Side specialty for the " + branch.title() + " path. Adds flexible stats without forcing the whole lane.", 2, 34, sideX, baseY + 1,
                    List.of(id + "_art"), sideEffects(branch.initiateEffects()), null, treeName));
            add(tree, nodeAtLevel(id + "_mastery", branch.title() + " Mastery",
                    "Deepen the " + branch.title() + " lane for late-journey stat growth.", 3, 44, mainX, baseY + 2,
                    List.of(id + "_art"), branch.masteryEffects(), null, treeName));
            add(tree, nodeAtLevel(id + "_doctrine", branch.title() + " Doctrine",
                    "Optional side doctrine that sharpens this specialization before its apex.", 2, 52, sideX, baseY + 2,
                    List.of(id + "_mastery"), sideEffects(branch.masteryEffects()), null, treeName));
            add(tree, nodeAtLevel(id + "_apex", branchApex.name(),
                    "Level-58 " + branch.title() + " apex art that carries this lane into the level-60 capstone.", 2, 58, mainX, baseY + 3,
                    List.of(id + "_mastery"), Map.of(), branchApex, treeName));
        }
    }

    private static Ability uniqueTreeAbility(Map<String, SkillNode> tree, Ability ability, String prefix) {
        boolean duplicate = tree.values().stream()
                .anyMatch(node -> node.ability() != null && node.ability().name().equals(ability.name()));
        if (!duplicate) {
            return ability;
        }
        String renamed = prefix + " " + ability.name();
        return ability.renamed(renamed);
    }

    private static Map<String, Integer> sideEffects(Map<String, Integer> effects) {
        Map<String, Integer> side = new LinkedHashMap<>();
        for (var entry : effects.entrySet()) {
            side.put(entry.getKey(), Math.max(1, entry.getValue() / 2));
        }
        if (side.isEmpty()) {
            side.put("max_mp", 4);
        }
        return Map.copyOf(side);
    }

    private static Ability damageArt(String name, int power, int cost, String target, Ability.ScalingProfile scaling,
                                     String effect, String tag, AbilityStatus... statuses) {
        return damageArt(name, power, cost, target, scaling, effect, tag, defaultDamageVisualResolution(target), statuses);
    }

    private static Ability damageArt(String name, int power, int cost, String target, Ability.ScalingProfile scaling,
                                     String effect, String tag, Ability.VisualResolution visualResolution, AbilityStatus... statuses) {
        Ability ability = new Ability(name, power, cost, Ability.AbilityKind.DAMAGE, target)
                .withCooldown("all_enemies".equals(target) ? 3 : 2)
                .withScaling(scaling)
                .withEffect(effect)
                .withStatuses(statuses)
                .withVisualResolution(visualResolution);
        return tag == null || tag.isBlank() ? ability : ability.withTags(tag);
    }

    private static Ability.VisualResolution defaultDamageVisualResolution(String target) {
        return "all_enemies".equals(target) ? Ability.VisualResolution.AOE : Ability.VisualResolution.SINGLE;
    }

    private static Ability healArt(String name, int power, int cost, String target, Ability.ScalingProfile scaling,
                                   String effect, String tag) {
        Ability ability = new Ability(name, power, cost, Ability.AbilityKind.HEAL, target)
                .withCooldown("party".equals(target) ? 4 : 2)
                .withScaling(scaling)
                .withEffect(effect)
                .withStatuses(new AbilityStatus("regeneration", "target", 0.65), new AbilityStatus("shield", "target", 0.55));
        return tag == null || tag.isBlank() ? ability : ability.withTags(tag, "overheal_shield");
    }

    private static Ability guardArt(String name, int cost, String tag) {
        Ability ability = new Ability(name, 0, cost, Ability.AbilityKind.DEFEND, "self")
                .withCooldown(4)
                .withScaling(Ability.ScalingProfile.GUARD)
                .withEffect("shield")
                .withStatuses(new AbilityStatus("shield", "self"), new AbilityStatus("fortified", "self"));
        return tag == null || tag.isBlank() ? ability : ability.withTags(tag);
    }

    private static Ability masteryAreaAbility(String treeName, String name) {
        Ability ability = switch (treeName) {
            case "Knight" -> new Ability(name, 30, 14, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.WEAPON).withEffect("cleave").withStatuses(new AbilityStatus("vulnerable", "enemy", 0.45));
            case "Mage" -> new Ability(name, 30, 14, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.ARCANE).withEffect("arcane").withStatuses(new AbilityStatus("vulnerable", "enemy", 0.55));
            case "Ranger" -> new Ability(name, 30, 14, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.AGILITY).withEffect("volley").withStatuses(new AbilityStatus("weak", "enemy", 0.45));
            case "Cleric" -> new Ability(name, 30, 14, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.DIVINE).withEffect("radiant").withStatuses(new AbilityStatus("vulnerable", "enemy", 0.5));
            case "Rogue", "Bladedancer", "Veilrunner", "Nightblade" -> new Ability(name, 30, 14, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.AGILITY).withEffect("dark").withStatuses(new AbilityStatus("poison", "enemy", 0.35));
            case "Battle Medic" -> new Ability(name, 24, 13, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.TRIAGE).withEffect("impact").withStatuses(new AbilityStatus("weak", "enemy", 0.6));
            case "Ironwall" -> new Ability(name, 26, 13, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.GUARD).withEffect("bash").withStatuses(new AbilityStatus("weak", "enemy", 0.55));
            case "Wildspeaker", "Thornbinder", "Grovekeeper" -> new Ability(name, 28, 14, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.NATURE).withEffect("nature").withStatuses(new AbilityStatus("weak", "enemy", 0.5));
            case "Sunwarden" -> new Ability(name, 30, 14, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.DIVINE).withEffect("ember").withStatuses(new AbilityStatus("burn", "enemy", 0.45));
            case "Stonebreaker" -> new Ability(name, 32, 15, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.WEAPON).withEffect("bash").withStatuses(new AbilityStatus("vulnerable", "enemy", 0.45));
            default -> new Ability(name, 30, 14, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3);
        };
        return ability.withVisualResolution("Ranger".equals(treeName)
                ? Ability.VisualResolution.MULTI_PROJECTILE
                : Ability.VisualResolution.AOE);
    }

    private static Ability masteryGuardAbility(String treeName, String name) {
        Ability ability = new Ability(name, 0, 10, Ability.AbilityKind.DEFEND, "self").withCooldown(4).withScaling(Ability.ScalingProfile.GUARD);
        return switch (treeName) {
            case "Rogue", "Bladedancer", "Veilrunner", "Nightblade" -> ability.withScaling(Ability.ScalingProfile.AGILITY).withEffect("dust").withStatuses(new AbilityStatus("shield", "self"), new AbilityStatus("haste", "self"));
            case "Mage" -> ability.withScaling(Ability.ScalingProfile.ARCANE).withEffect("arcane").withStatuses(new AbilityStatus("shield", "self"), new AbilityStatus("fortified", "self"));
            case "Cleric", "Sunwarden" -> ability.withScaling(Ability.ScalingProfile.DIVINE).withEffect("radiant").withStatuses(new AbilityStatus("shield", "self"), new AbilityStatus("regeneration", "self"));
            case "Wildspeaker", "Thornbinder", "Grovekeeper" -> ability.withScaling(Ability.ScalingProfile.NATURE).withEffect("nature").withStatuses(new AbilityStatus("shield", "self"), new AbilityStatus("regeneration", "self"), new AbilityStatus("fortified", "self", 0.45));
            case "Battle Medic" -> ability.withScaling(Ability.ScalingProfile.TRIAGE).withEffect("heal").withStatuses(new AbilityStatus("shield", "self"), new AbilityStatus("regeneration", "self"));
            default -> ability.withStatuses(new AbilityStatus("shield", "self"), new AbilityStatus("fortified", "self"));
        };
    }

    private static Ability masteryUltimateAbility(String treeName, String name) {
        Ability ability = switch (treeName) {
            case "Knight" -> new Ability(name, 58, 22, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(5).withScaling(Ability.ScalingProfile.WEAPON).withEffect("prismatic").withStatuses(new AbilityStatus("vulnerable", "enemy"));
            case "Mage" -> new Ability(name, 60, 24, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(5).withScaling(Ability.ScalingProfile.ARCANE).withEffect("prismatic").withStatuses(new AbilityStatus("burn", "enemy", 0.5), new AbilityStatus("vulnerable", "enemy", 0.7));
            case "Ranger" -> new Ability(name, 54, 21, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(4).withScaling(Ability.ScalingProfile.AGILITY).withEffect("volley").withStatuses(new AbilityStatus("weak", "enemy"), new AbilityStatus("vulnerable", "enemy", 0.45));
            case "Cleric" -> new Ability(name, 42, 24, Ability.AbilityKind.HEAL, "party").withCooldown(5).withScaling(Ability.ScalingProfile.DIVINE).withEffect("seraphic_hymn").withStatuses(new AbilityStatus("shield", "target"), new AbilityStatus("regeneration", "target")).withTags("cleanse", "overheal_shield");
            case "Battle Medic" -> new Ability(name, 44, 23, Ability.AbilityKind.HEAL, "party").withCooldown(5).withScaling(Ability.ScalingProfile.TRIAGE).withEffect("heal").withStatuses(new AbilityStatus("shield", "target"), new AbilityStatus("regeneration", "target"));
            case "Ironwall" -> new Ability(name, 0, 20, Ability.AbilityKind.DEFEND, "self").withCooldown(5).withScaling(Ability.ScalingProfile.GUARD).withEffect("shield").withStatuses(new AbilityStatus("shield", "self"), new AbilityStatus("fortified", "self"), new AbilityStatus("regeneration", "self"));
            case "Grovekeeper" -> new Ability(name, 40, 24, Ability.AbilityKind.HEAL, "party").withCooldown(5).withScaling(Ability.ScalingProfile.NATURE).withEffect("root_memory").withStatuses(new AbilityStatus("shield", "target"), new AbilityStatus("regeneration", "target"), new AbilityStatus("fortified", "target", 0.6)).withTags("revive_party", "overheal_shield");
            case "Wildspeaker", "Thornbinder" -> new Ability(name, 56, 22, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(5).withScaling(Ability.ScalingProfile.NATURE).withEffect("prismatic").withStatuses(new AbilityStatus("weak", "enemy"), new AbilityStatus("poison", "enemy", 0.45));
            case "Sunwarden" -> new Ability(name, 56, 23, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(5).withScaling(Ability.ScalingProfile.DIVINE).withEffect("radiant").withStatuses(new AbilityStatus("burn", "enemy", 0.55), new AbilityStatus("vulnerable", "enemy", 0.55));
            case "Rogue", "Bladedancer", "Veilrunner", "Nightblade" -> new Ability(name, 56, 22, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(4).withScaling(Ability.ScalingProfile.AGILITY).withEffect("dark").withStatuses(new AbilityStatus("vulnerable", "enemy"), new AbilityStatus("poison", "enemy", 0.45));
            case "Stonebreaker" -> new Ability(name, 60, 23, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(5).withScaling(Ability.ScalingProfile.WEAPON).withEffect("worldsplitter").withStatuses(new AbilityStatus("vulnerable", "enemy"), new AbilityStatus("weak", "enemy", 0.5)).withTags("armor_breaker");
            default -> new Ability(name, 54, 20, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(5);
        };
        if (ability.kind() != Ability.AbilityKind.DAMAGE || !"all_enemies".equals(ability.target())) {
            return ability;
        }
        return ability.withVisualResolution("Ranger".equals(treeName)
                ? Ability.VisualResolution.MULTI_PROJECTILE
                : Ability.VisualResolution.AOE);
    }

    private static void addProfessionSkillNodes() {
        addProfession(WOODCUTTING_PROFESSION_TREE, node("trade_foundations", "Trade Foundations", "+1 cap to every profession and +1 effective profession level per rank.", 2, 2, 0, List.of(), Map.of(), null, "Professions"));
        addProfession(WOODCUTTING_PROFESSION_TREE, node("forester_path", "Forester Path", "+2 Woodcutting cap and better tree yields per rank.", 2, 1, 1, List.of("trade_foundations"), Map.of(), null, "Woodcutting"));
        addProfession(WOODCUTTING_PROFESSION_TREE, node("coppice_planning", "Coppice Planning", "+1 Woodcutting cap; forest gathering leans toward steady wood.", 2, 0, 2, List.of("forester_path"), Map.of(), null, "Woodcutting"));
        addProfession(WOODCUTTING_PROFESSION_TREE, node("resin_tapping", "Resin Tapping", "Woodcutting can produce extra fiber or herbs from mature trees.", 2, 2, 2, List.of("forester_path"), Map.of(), null, "Woodcutting"));
        addProfession(WOODCUTTING_PROFESSION_TREE, node("heartwood_harvest", "Heartwood Harvest", "+1 Woodcutting cap and effective level; rare tree yields improve.", 1, 1, 3, List.of("coppice_planning", "resin_tapping"), Map.of(), null, "Woodcutting"));

        addProfession(FISHING_PROFESSION_TREE, node("trade_foundations", "Trade Foundations", "+1 cap to every profession and +1 effective profession level per rank.", 2, 2, 0, List.of(), Map.of(), null, "Professions"));
        addProfession(FISHING_PROFESSION_TREE, node("angler_path", "Angler Path", "+2 Fishing cap and better water yields per rank.", 2, 1, 1, List.of("trade_foundations"), Map.of(), null, "Fishing"));
        addProfession(FISHING_PROFESSION_TREE, node("tide_reader", "Tide Reader", "+1 Fishing cap; water gathering finds more useful shoreline goods.", 2, 0, 2, List.of("angler_path"), Map.of(), null, "Fishing"));
        addProfession(FISHING_PROFESSION_TREE, node("netcraft", "Netcraft", "Fishing gets extra effective levels and more reliable raw fish.", 2, 2, 2, List.of("angler_path"), Map.of(), null, "Fishing"));
        addProfession(FISHING_PROFESSION_TREE, node("deepwater_bounty", "Deepwater Bounty", "+1 Fishing cap and effective level; rare lure finds improve.", 1, 1, 3, List.of("tide_reader", "netcraft"), Map.of(), null, "Fishing"));

        addProfession(MINING_PROFESSION_TREE, node("trade_foundations", "Trade Foundations", "+1 cap to every profession and +1 effective profession level per rank.", 2, 2, 0, List.of(), Map.of(), null, "Professions"));
        addProfession(MINING_PROFESSION_TREE, node("prospector_path", "Prospector Path", "+2 Mining cap and better ore yields per rank.", 2, 1, 1, List.of("trade_foundations"), Map.of(), null, "Mining"));
        addProfession(MINING_PROFESSION_TREE, node("seam_sense", "Seam Sense", "+1 Mining cap; mountain gathering finds more ore and coal.", 2, 0, 2, List.of("prospector_path"), Map.of(), null, "Mining"));
        addProfession(MINING_PROFESSION_TREE, node("blast_mining", "Blast Mining", "Mining gains effective levels and stronger bonus-roll output.", 2, 2, 2, List.of("prospector_path"), Map.of(), null, "Mining"));
        addProfession(MINING_PROFESSION_TREE, node("gem_cutting", "Gem Cutting", "+1 Mining cap and effective level; crystal dust finds improve.", 1, 1, 3, List.of("seam_sense", "blast_mining"), Map.of(), null, "Mining"));

        addProfession(CRAFTING_PROFESSION_TREE, node("trade_foundations", "Trade Foundations", "+1 cap to every profession and +1 effective profession level per rank.", 2, 2, 0, List.of(), Map.of(), null, "Professions"));
        addProfession(CRAFTING_PROFESSION_TREE, node("artisan_path", "Artisan Path", "+2 Crafting cap and faster workshop tasks per rank.", 2, 1, 1, List.of("trade_foundations"), Map.of(), null, "Crafting"));
        addProfession(CRAFTING_PROFESSION_TREE, node("measured_cuts", "Measured Cuts", "+1 Crafting cap; crafted batches stretch materials further.", 2, 0, 2, List.of("artisan_path"), Map.of(), null, "Crafting"));
        addProfession(CRAFTING_PROFESSION_TREE, node("jig_templates", "Jig Templates", "Crafting gains effective levels and occasional extra output.", 2, 2, 2, List.of("artisan_path"), Map.of(), null, "Crafting"));
        addProfession(CRAFTING_PROFESSION_TREE, node("masterwork_fittings", "Masterwork Fittings", "+1 Crafting cap and effective level; high-end craft output improves.", 1, 1, 3, List.of("measured_cuts", "jig_templates"), Map.of(), null, "Crafting"));

        addProfession(WEAVING_PROFESSION_TREE, node("trade_foundations", "Trade Foundations", "+1 cap to every profession and +1 effective profession level per rank.", 2, 2, 0, List.of(), Map.of(), null, "Professions"));
        addProfession(WEAVING_PROFESSION_TREE, node("tailor_path", "Tailor Path", "+1 Weaving and Leatherworking cap; cloth and hide work improves.", 2, 1, 1, List.of("trade_foundations"), Map.of(), null, "Weaving"));
        addProfession(WEAVING_PROFESSION_TREE, node("loom_logic", "Loom Logic", "+2 Weaving cap and better plant fiber or wool results.", 2, 0, 2, List.of("tailor_path"), Map.of(), null, "Weaving"));
        addProfession(WEAVING_PROFESSION_TREE, node("dye_baths", "Dye Baths", "Weaving gains effective levels and more herb-rich gathering results.", 2, 2, 2, List.of("tailor_path"), Map.of(), null, "Weaving"));
        addProfession(WEAVING_PROFESSION_TREE, node("sailcloth_patterns", "Sailcloth Patterns", "+1 Weaving cap and effective level; large batches improve.", 1, 1, 3, List.of("loom_logic", "dye_baths"), Map.of(), null, "Weaving"));

        addProfession(LEATHERWORKING_PROFESSION_TREE, node("trade_foundations", "Trade Foundations", "+1 cap to every profession and +1 effective profession level per rank.", 2, 2, 0, List.of(), Map.of(), null, "Professions"));
        addProfession(LEATHERWORKING_PROFESSION_TREE, node("tailor_path", "Tailor Path", "+1 Weaving and Leatherworking cap; cloth and hide work improves.", 2, 1, 1, List.of("trade_foundations"), Map.of(), null, "Leatherworking"));
        addProfession(LEATHERWORKING_PROFESSION_TREE, node("tanner_path", "Tanner Path", "+2 Leatherworking cap and better skin or bone results.", 2, 0, 2, List.of("tailor_path"), Map.of(), null, "Leatherworking"));
        addProfession(LEATHERWORKING_PROFESSION_TREE, node("curing_racks", "Curing Racks", "Leatherworking gains effective levels and steadier hide output.", 2, 2, 2, List.of("tailor_path"), Map.of(), null, "Leatherworking"));
        addProfession(LEATHERWORKING_PROFESSION_TREE, node("reinforced_hide", "Reinforced Hide", "Leatherworking gets stronger bonus rolls from hide-heavy tasks.", 2, 3, 3, List.of("curing_racks"), Map.of(), null, "Leatherworking"));
        addProfession(LEATHERWORKING_PROFESSION_TREE, node("saddle_stitch", "Saddle Stitch", "+1 Leatherworking cap; complex leather batches improve.", 1, 1, 3, List.of("tanner_path", "curing_racks"), Map.of(), null, "Leatherworking"));

        addProfession(COOKING_PROFESSION_TREE, node("trade_foundations", "Trade Foundations", "+1 cap to every profession and +1 effective profession level per rank.", 2, 2, 0, List.of(), Map.of(), null, "Professions"));
        addProfession(COOKING_PROFESSION_TREE, node("provisioner_path", "Provisioner Path", "+1 Cooking and Survival cap; food preparation improves.", 2, 1, 1, List.of("trade_foundations"), Map.of(), null, "Cooking"));
        addProfession(COOKING_PROFESSION_TREE, node("spice_blends", "Spice Blends", "+2 Cooking cap and better herb or vegetable results.", 2, 0, 2, List.of("provisioner_path"), Map.of(), null, "Cooking"));
        addProfession(COOKING_PROFESSION_TREE, node("stockpot_rhythm", "Stockpot Rhythm", "Cooking gains effective levels and more output from recipes.", 2, 2, 2, List.of("provisioner_path"), Map.of(), null, "Cooking"));
        addProfession(COOKING_PROFESSION_TREE, node("feast_planning", "Feast Planning", "+1 Cooking cap and effective level; large meal output improves.", 1, 1, 3, List.of("spice_blends", "stockpot_rhythm"), Map.of(), null, "Cooking"));

        addProfession(SURVIVAL_PROFESSION_TREE, node("trade_foundations", "Trade Foundations", "+1 cap to every profession and +1 effective profession level per rank.", 2, 2, 0, List.of(), Map.of(), null, "Professions"));
        addProfession(SURVIVAL_PROFESSION_TREE, node("provisioner_path", "Provisioner Path", "+1 Cooking and Survival cap; trail provisioning improves.", 2, 1, 1, List.of("trade_foundations"), Map.of(), null, "Survival"));
        addProfession(SURVIVAL_PROFESSION_TREE, node("trailcraft_path", "Trailcraft Path", "+2 Survival cap and better camp-resource results.", 2, 0, 2, List.of("provisioner_path"), Map.of(), null, "Survival"));
        addProfession(SURVIVAL_PROFESSION_TREE, node("weather_eye", "Weather Eye", "Survival gains effective levels and steadier field finds.", 2, 2, 2, List.of("provisioner_path"), Map.of(), null, "Survival"));
        addProfession(SURVIVAL_PROFESSION_TREE, node("snare_lines", "Snare Lines", "Survival can turn field work into extra skin, bone, or food.", 2, 3, 3, List.of("weather_eye"), Map.of(), null, "Survival"));
        addProfession(SURVIVAL_PROFESSION_TREE, node("emergency_cache", "Emergency Cache", "+1 Survival cap and effective level; rare field finds improve.", 1, 1, 3, List.of("trailcraft_path", "weather_eye"), Map.of(), null, "Survival"));

        for (Profession profession : List.of(Profession.SMITHING, Profession.TAILORING, Profession.CARPENTRY,
                Profession.ARMORCRAFT, Profession.ALCHEMY, Profession.JEWELLERY)) {
            Map<String, SkillNode> tree = new LinkedHashMap<>();
            String training = profession.id() + "_training";
            String mastery = profession.id() + "_mastery";
            addProfession(tree, node(training, profession.label() + " Training",
                    "+1 effective " + profession.label() + " level per rank. Improves crafting quality and speed.",
                    2, 1, 0, List.of(), Map.of(), null, profession.label()));
            addProfession(tree, node(mastery, profession.label() + " Mastery",
                    "+1 profession cap and effective level per rank. Higher workmanship improves final stats.",
                    2, 1, 1, List.of(training), Map.of(), null, profession.label()));
            PROFESSION_SKILL_TREES.put(profession.id(), tree);
        }

        SkillNode masterOfTrades = node("master_of_trades", "Master of Trades", "+1 cap and +1 effective level to every profession.", 1, 3, 4, List.of("trade_foundations"), Map.of(), null, "Professions");
        addProfession(WOODCUTTING_PROFESSION_TREE, masterOfTrades);
        addProfession(FISHING_PROFESSION_TREE, masterOfTrades);
        addProfession(MINING_PROFESSION_TREE, masterOfTrades);
        addProfession(CRAFTING_PROFESSION_TREE, masterOfTrades);
        addProfession(WEAVING_PROFESSION_TREE, masterOfTrades);
        addProfession(LEATHERWORKING_PROFESSION_TREE, masterOfTrades);
        addProfession(COOKING_PROFESSION_TREE, masterOfTrades);
        addProfession(SURVIVAL_PROFESSION_TREE, masterOfTrades);

        PROFESSION_SKILL_TREES.put(Profession.WOODCUTTING.id(), WOODCUTTING_PROFESSION_TREE);
        PROFESSION_SKILL_TREES.put(Profession.FISHING.id(), FISHING_PROFESSION_TREE);
        PROFESSION_SKILL_TREES.put(Profession.MINING.id(), MINING_PROFESSION_TREE);
        PROFESSION_SKILL_TREES.put(Profession.CRAFTING.id(), CRAFTING_PROFESSION_TREE);
        PROFESSION_SKILL_TREES.put(Profession.WEAVING.id(), WEAVING_PROFESSION_TREE);
        PROFESSION_SKILL_TREES.put(Profession.LEATHERWORKING.id(), LEATHERWORKING_PROFESSION_TREE);
        PROFESSION_SKILL_TREES.put(Profession.COOKING.id(), COOKING_PROFESSION_TREE);
        PROFESSION_SKILL_TREES.put(Profession.SURVIVAL.id(), SURVIVAL_PROFESSION_TREE);
    }
}
