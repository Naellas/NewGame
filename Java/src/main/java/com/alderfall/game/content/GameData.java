package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import com.alderfall.game.inventory.Equipment;
import com.alderfall.game.inventory.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class GameData {
    public static final String STORY_PORTAL_ASSET = "story_void_portal";
    public static final List<String> MAGIC_STONE_KEYS = List.of(
            "stone_memory",
            "stone_iron",
            "stone_ember",
            "stone_tides",
            "stone_hunger",
            "stone_roots",
            "stone_graves",
            "stone_frost",
            "stone_bells",
            "stone_ash",
            "stone_oaths",
            "stone_dawn"
    );
    public static final Set<String> MAIN_STORY_QUEST_IDS = Set.of(
            "ms_wake_ashes",
            "ms_road_dust",
            "ms_oathstead_stand",
            "ms_names_dust",
            "ms_stolen_index",
            "ms_first_socket",
            "ms_watchtower_bells",
            "ms_raiders_pass",
            "ms_frosthollow_standard",
            "ms_shrine_shadow",
            "ms_caravan_glass",
            "ms_ember_socket_rite",
            "ms_bell_alone",
            "ms_medicine_mireford",
            "ms_miredepth_below",
            "ms_toll_ledger",
            "ms_redcap_trade",
            "ms_glowing_mud",
            "ms_orchard_ward",
            "ms_names_cold_stone",
            "ms_cold_road",
            "ms_missing_bell_rope",
            "ms_blackvault_mark",
            "ms_camp_defending",
            "ms_kingdoms_answer",
            "ms_twelve_stones_gate"
    );
    private static final Map<String, String> MAIN_STORY_STONE_REWARDS = Map.ofEntries(
            Map.entry("ms_first_socket", "stone_memory"),
            Map.entry("ms_frosthollow_standard", "stone_iron"),
            Map.entry("ms_ember_socket_rite", "stone_ember"),
            Map.entry("ms_miredepth_below", "stone_tides"),
            Map.entry("ms_glowing_mud", "stone_hunger"),
            Map.entry("ms_orchard_ward", "stone_roots"),
            Map.entry("ms_names_cold_stone", "stone_graves"),
            Map.entry("ms_cold_road", "stone_frost"),
            Map.entry("ms_missing_bell_rope", "stone_bells"),
            Map.entry("ms_blackvault_mark", "stone_ash"),
            Map.entry("ms_camp_defending", "stone_oaths"),
            Map.entry("ms_kingdoms_answer", "stone_dawn")
    );
    private static final Map<String, String> MAIN_STORY_PREREQUISITES = Map.ofEntries(
            Map.entry("ms_names_dust", "ms_oathstead_stand"),
            Map.entry("ms_watchtower_bells", "ms_first_socket"),
            Map.entry("ms_shrine_shadow", "ms_first_socket"),
            Map.entry("ms_bell_alone", "ms_first_socket"),
            Map.entry("ms_toll_ledger", "ms_first_socket"),
            Map.entry("ms_orchard_ward", "ms_first_socket"),
            Map.entry("ms_names_cold_stone", "ms_first_socket"),
            Map.entry("ms_cold_road", "ms_first_socket"),
            Map.entry("ms_missing_bell_rope", "ms_first_socket"),
            Map.entry("ms_blackvault_mark", "ms_first_socket"),
            Map.entry("ms_camp_defending", "ms_first_socket"),
            Map.entry("ms_kingdoms_answer", "ms_camp_defending"),
            Map.entry("ms_twelve_stones_gate", "ms_kingdoms_answer")
    );
    private static final Map<String, String> MAIN_STORY_REQUIRED_ITEMS = Map.of();
    public static final List<String> EQUIPMENT_SLOTS = List.of(
            "weapon",
            "shield",
            "helmet",
            "pauldrons",
            "chestpiece",
            "gloves",
            "belt",
            "leggings",
            "boots",
            "ring",
            "necklace"
    );
    public static final List<String> DAMAGE_TYPES = List.of(
            "physical",
            "poison",
            "acid",
            "fire",
            "lightning",
            "ice",
            "water",
            "arcane",
            "dark",
            "chaos",
            "holy",
            "nature"
    );
    public static final Map<String, MonsterLore> MONSTER_LORE = Map.ofEntries(
            lore("slime", "A wet, stubborn clot of marsh life that squeezes through boots, cracks, and bad plans.",
                    List.of("acid"), Map.of("lightning", 1.25, "ice", 1.15), Map.of("acid", 0.65, "poison", 0.70)),
            lore("frost_wolf", "Its breath pearls into hoarfrost before the bite lands.",
                    List.of("ice", "physical"), Map.of("fire", 1.30), Map.of("ice", 0.60)),
            lore("snow_lynx", "A white-furred hunter that vanishes whenever the wind lifts loose snow.",
                    List.of("ice", "physical"), Map.of("fire", 1.25), Map.of("ice", 0.70)),
            lore("ice_golem", "Old mountain ice packed around a patient stone heart.",
                    List.of("ice", "physical"), Map.of("fire", 1.45, "chaos", 1.15), Map.of("ice", 0.45, "water", 0.75, "physical", 0.90)),
            lore("frost_troll", "Cold knits its wounds almost as quickly as steel opens them.",
                    List.of("ice", "physical"), Map.of("fire", 1.35), Map.of("ice", 0.55, "water", 0.80)),
            lore("ember_imp", "A coal-bright nuisance with a laugh like kindling catching.",
                    List.of("fire", "chaos"), Map.of("water", 1.50, "ice", 1.15), Map.of("fire", 0.50, "chaos", 0.85)),
            lore("ember_tortoise", "A slow furnace under a basalt shell, stubborn enough to make patience look nervous.",
                    List.of("fire", "physical"), Map.of("water", 1.35), Map.of("fire", 0.60, "physical", 0.85)),
            lore("ash_scorpion", "Its sting carries desert ash and venom in equal measure.",
                    List.of("fire", "poison", "physical"), Map.of("water", 1.25), Map.of("fire", 0.75, "poison", 0.65)),
            lore("fire_giant", "A walking forge-king whose footfalls leave the air wavering.",
                    List.of("fire", "physical"), Map.of("water", 1.35, "ice", 1.15), Map.of("fire", 0.50, "physical", 0.90)),
            lore("red_dragon", "Young only by dragon standards, which is not very reassuring.",
                    List.of("fire", "physical"), Map.of("ice", 1.20, "water", 1.15), Map.of("fire", 0.45, "poison", 0.70)),
            lore("elder_dragon", "Ancient heat wearing scales, pride, and old disaster like a crown.",
                    List.of("fire", "dark", "physical"), Map.of("ice", 1.15, "holy", 1.10), Map.of("fire", 0.40, "dark", 0.70, "chaos", 0.85)),
            lore("skeleton", "Bone held upright by a memory that refuses to be finished.",
                    List.of("dark", "physical"), Map.of("holy", 1.35, "physical", 1.10), Map.of("poison", 0.35, "dark", 0.70)),
            lore("bone_knight", "A martial oath rattling around inside old armor.",
                    List.of("dark", "physical"), Map.of("holy", 1.30, "chaos", 1.10), Map.of("poison", 0.35, "dark", 0.65, "physical", 0.90)),
            lore("wraith", "A torn shape of grief and cold shadow, barely persuaded to stay in one place.",
                    List.of("dark"), Map.of("holy", 1.45, "arcane", 1.15), Map.of("physical", 0.55, "dark", 0.55, "poison", 0.35)),
            lore("crypt_revenant", "A returned sentinel with lantern-cold hands and unfinished orders.",
                    List.of("dark", "ice"), Map.of("holy", 1.40, "fire", 1.15), Map.of("dark", 0.60, "ice", 0.75, "poison", 0.35)),
            lore("elder_wraith", "A crown of silence around a thing that has forgotten how to die.",
                    List.of("dark", "ice"), Map.of("holy", 1.50, "chaos", 1.15), Map.of("physical", 0.50, "dark", 0.50, "poison", 0.30)),
            lore("thornling", "A roadside bramble with a grudge and excellent aim.",
                    List.of("nature", "poison"), Map.of("fire", 1.30, "ice", 1.10), Map.of("nature", 0.60, "water", 0.75)),
            lore("moss_stag", "Green moss grows between its antlers like a second crown.",
                    List.of("nature", "physical"), Map.of("fire", 1.20), Map.of("nature", 0.70, "water", 0.85)),
            lore("bramble_boar", "A thorny charge with tusks at the front and bad decisions everywhere else.",
                    List.of("nature", "poison", "physical"), Map.of("fire", 1.25), Map.of("nature", 0.70, "poison", 0.75)),
            lore("bog_beast", "Something the marsh built from teeth, mud, and appetite.",
                    List.of("acid", "water", "physical"), Map.of("lightning", 1.25, "ice", 1.10), Map.of("water", 0.70, "acid", 0.65)),
            lore("swamp_troll", "A mound of fen muscle that trusts mud more than armor.",
                    List.of("acid", "water", "physical"), Map.of("lightning", 1.20, "ice", 1.10), Map.of("water", 0.75, "acid", 0.70)),
            lore("river_eel", "A silver snap of river muscle that moves faster than the eye wants.",
                    List.of("water", "lightning"), Map.of("ice", 1.20, "nature", 1.10), Map.of("water", 0.55, "lightning", 0.75)),
            lore("goblin_shaman", "Campfire smoke, petty curses, and more confidence than wisdom.",
                    List.of("dark", "fire"), Map.of("holy", 1.20, "water", 1.10), Map.of("dark", 0.80)),
            lore("crystal_hare", "Pretty, fast, and much sharper than its outline suggests.",
                    List.of("arcane", "physical"), Map.of("physical", 1.15, "chaos", 1.15), Map.of("arcane", 0.60)),
            lore("glass_scorpion", "A mirror-bright carapace hides venom under every angle.",
                    List.of("poison", "physical"), Map.of("physical", 1.15, "chaos", 1.10), Map.of("poison", 0.55, "arcane", 0.75)),
            lore("sand_stalker", "It follows shade instead of footprints, which feels personally rude.",
                    List.of("physical", "dark"), Map.of("water", 1.20, "ice", 1.10), Map.of("dark", 0.80)),
            lore("marsh_drake", "A low-winged drake with bog breath and territorial opinions.",
                    List.of("acid", "poison", "physical"), Map.of("lightning", 1.20, "ice", 1.10), Map.of("acid", 0.65, "poison", 0.70)),
            lore("mountain_drake", "All ridgeline muscle and stone-hard scales.",
                    List.of("physical"), Map.of("ice", 1.10, "arcane", 1.10), Map.of("physical", 0.85)),
            lore("stone_giant", "A cliff that learned to be offended.",
                    List.of("physical"), Map.of("water", 1.15, "nature", 1.10, "chaos", 1.10), Map.of("physical", 0.80, "poison", 0.50)),
            lore("stoneback_goat", "A goat that treats gravity as a polite suggestion.",
                    List.of("physical"), Map.of("water", 1.10), Map.of("physical", 0.90)),
            lore("orc_shaman", "War smoke and old rot prayers gather around its hands.",
                    List.of("dark", "poison"), Map.of("holy", 1.25), Map.of("dark", 0.75, "poison", 0.70)),
            lore("goblin_king", "A stolen crown, a loud command voice, and a camp full of bad ideas.",
                    List.of("physical", "dark"), Map.of("holy", 1.15, "chaos", 1.10), Map.of("dark", 0.80))
    );

    private GameData() {
    }

    public static Actor createPlayer(String className) {
        return createPlayer(className, "Hero");
    }

    public static Actor createPlayer(String className, String playerName) {
        String name = playerName == null || playerName.isBlank() ? "Hero" : playerName.strip();
        Actor actor = switch (className) {
            case "Mage" -> new Actor(name, "class_mage", "Mage", 42, 34, 8, 2);
            case "Ranger" -> new Actor(name, "class_ranger", "Ranger", 48, 22, 11, 3);
            case "Cleric" -> new Actor(name, "class_cleric", "Cleric", 50, 30, 9, 3);
            case "Rogue" -> new Actor(name, "class_rogue", "Rogue", 46, 24, 13, 2);
            default -> new Actor(name, "class_knight", "Knight", 58, 16, 12, 4);
        };
        actor.abilities.addAll(classAbilities(actor.className));
        actor.gold = 25;
        actor.addItem("potion_small", 2);
        actor.addItem("ether", 1);
        actor.addItem("rusty_sword", 1);
        actor.equipItem("rusty_sword");
        return actor;
    }

    public static List<Ability> classAbilities(String className) {
        return switch (className) {
            case "Mage" -> List.of(
                    new Ability("Firebolt", 22, 6, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.ARCANE).withEffect("ember").withStatuses(new AbilityStatus("burn", "enemy", 0.55)),
                    new Ability("Frost Lance", 18, 5, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.ARCANE).withEffect("frost").withStatuses(new AbilityStatus("weak", "enemy")),
                    new Ability("Ley Detonation", 28, 9, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.ARCANE).withEffect("ley_detonation").withStatuses(new AbilityStatus("vulnerable", "enemy", 0.45)).withVisualResolution(Ability.VisualResolution.AOE).withTags("consume_vulnerable"),
                    new Ability("Mend", 18, 5, Ability.AbilityKind.HEAL).withScaling(Ability.ScalingProfile.TRIAGE).withStatuses(new AbilityStatus("regeneration", "self"))
            );
            case "Ranger" -> List.of(
                    new Ability("Piercing Shot", 18, 5, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.AGILITY).withEffect("volley"),
                    new Ability("Rain Volley", 15, 4, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(2).withScaling(Ability.ScalingProfile.AGILITY).withEffect("volley").withVisualResolution(Ability.VisualResolution.MULTI_PROJECTILE),
                    new Ability("Snare Arrow", 16, 6, Ability.AbilityKind.DAMAGE).withCooldown(2).withScaling(Ability.ScalingProfile.AGILITY).withEffect("nature").withStatuses(new AbilityStatus("weak", "enemy", 0.8)),
                    new Ability("Herbal Remedy", 16, 5, Ability.AbilityKind.HEAL).withScaling(Ability.ScalingProfile.NATURE).withStatuses(new AbilityStatus("regeneration", "target"))
            );
            case "Cleric", "Medic" -> List.of(
                    new Ability("Radiant Bolt", 18, 5, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.DIVINE).withEffect("holy").withStatuses(new AbilityStatus("vulnerable", "enemy", 0.45)),
                    new Ability("Blessing", 20, 6, Ability.AbilityKind.HEAL, "ally").withScaling(Ability.ScalingProfile.DIVINE).withStatuses(new AbilityStatus("regeneration", "target")),
                    new Ability("Ward Prayer", 16, 5, Ability.AbilityKind.HEAL, "ally").withCooldown(2).withScaling(Ability.ScalingProfile.DIVINE).withStatuses(new AbilityStatus("shield", "target"), new AbilityStatus("regeneration", "target", 0.65)).withTags("overheal_shield")
            );
            case "Rogue", "Dune Guide" -> List.of(
                    new Ability("Shadowstep Cut", 20, 6, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.AGILITY).withEffect("dark"),
                    new Ability("Venom Edge", 16, 5, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.AGILITY).withEffect("poison").withStatuses(new AbilityStatus("poison", "enemy")),
                    new Ability("Kidney Shot", 24, 7, Ability.AbilityKind.DAMAGE).withCooldown(2).withScaling(Ability.ScalingProfile.AGILITY).withEffect("impact").withStatuses(new AbilityStatus("vulnerable", "enemy")).withTags("consume_vulnerable"),
                    new Ability("Smoke Veil", 0, 4, Ability.AbilityKind.DEFEND, "self").withCooldown(3).withScaling(Ability.ScalingProfile.AGILITY).withEffect("dust").withStatuses(new AbilityStatus("shield", "self"), new AbilityStatus("haste", "self", 0.55))
            );
            case "Battle Medic" -> List.of(
                    new Ability("Hemostatic Strike", 15, 4, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.TRIAGE).withEffect("impact").withStatuses(new AbilityStatus("weak", "enemy", 0.5)),
                    new Ability("Emergency Mend", 22, 6, Ability.AbilityKind.HEAL, "ally").withCooldown(2).withScaling(Ability.ScalingProfile.TRIAGE).withStatuses(new AbilityStatus("regeneration", "target")).withTags("cleanse", "overheal_shield"),
                    new Ability("Tonic Toss", 16, 5, Ability.AbilityKind.HEAL, "party").withCooldown(3).withScaling(Ability.ScalingProfile.TRIAGE).withEffect("heal").withTags("cleanse"),
                    new Ability("Painkiller Rotation", 0, 6, Ability.AbilityKind.DEFEND, "self").withCooldown(3).withScaling(Ability.ScalingProfile.TRIAGE).withStatuses(new AbilityStatus("fortified", "self"), new AbilityStatus("haste", "self", 0.4))
            );
            case "Ironwall" -> List.of(
                    new Ability("Shield Ram", 18, 5, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.GUARD).withEffect("bash").withStatuses(new AbilityStatus("weak", "enemy", 0.45)),
                    new Ability("Hold Line", 0, 4, Ability.AbilityKind.DEFEND, "self").withCooldown(2).withScaling(Ability.ScalingProfile.GUARD).withStatuses(new AbilityStatus("shield", "self"), new AbilityStatus("fortified", "self")),
                    new Ability("Armor Patch", 16, 5, Ability.AbilityKind.HEAL, "self").withScaling(Ability.ScalingProfile.GUARD).withStatuses(new AbilityStatus("shield", "self")),
                    new Ability("Interpose", 0, 8, Ability.AbilityKind.DEFEND, "self").withCooldown(4).withScaling(Ability.ScalingProfile.GUARD).withStatuses(new AbilityStatus("shield", "self"), new AbilityStatus("fortified", "self"), new AbilityStatus("regeneration", "self", 0.5)).withTags("party_guard")
            );
            case "Bladedancer" -> List.of(
                    new Ability("Dual Cut", 22, 6, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.AGILITY).withEffect("slash"),
                    new Ability("Spinning Arc", 15, 5, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(2).withScaling(Ability.ScalingProfile.AGILITY).withEffect("cleave").withVisualResolution(Ability.VisualResolution.AOE),
                    new Ability("Rhythm Break", 30, 8, Ability.AbilityKind.DAMAGE).withCooldown(3).withScaling(Ability.ScalingProfile.AGILITY).withEffect("slash").withStatuses(new AbilityStatus("vulnerable", "enemy")),
                    new Ability("Grace Step", 0, 4, Ability.AbilityKind.DEFEND, "self").withCooldown(2).withScaling(Ability.ScalingProfile.AGILITY).withStatuses(new AbilityStatus("haste", "self"), new AbilityStatus("shield", "self", 0.55))
            );
            case "Veilrunner" -> List.of(
                    new Ability("Quick Cut", 20, 5, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.AGILITY).withEffect("slash"),
                    new Ability("Smoke Bomb", 14, 5, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.AGILITY).withEffect("dust").withStatuses(new AbilityStatus("weak", "enemy", 0.55)).withVisualResolution(Ability.VisualResolution.AOE),
                    new Ability("Exit Wound", 28, 7, Ability.AbilityKind.DAMAGE).withCooldown(2).withScaling(Ability.ScalingProfile.AGILITY).withEffect("dark").withStatuses(new AbilityStatus("vulnerable", "enemy", 0.6)),
                    new Ability("Shadow Salve", 16, 5, Ability.AbilityKind.HEAL, "self").withScaling(Ability.ScalingProfile.AGILITY).withStatuses(new AbilityStatus("haste", "self"))
            );
            case "Wildspeaker" -> List.of(
                    new Ability("Thorn Dart", 18, 5, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.NATURE).withEffect("nature"),
                    new Ability("Green Balm", 20, 6, Ability.AbilityKind.HEAL, "ally").withScaling(Ability.ScalingProfile.NATURE).withStatuses(new AbilityStatus("regeneration", "target")),
                    new Ability("Beastcall Howl", 0, 7, Ability.AbilityKind.DEFEND, "self").withCooldown(3).withScaling(Ability.ScalingProfile.NATURE).withStatuses(new AbilityStatus("haste", "self"), new AbilityStatus("fortified", "self", 0.45)),
                    new Ability("Bramble Wave", 14, 5, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(2).withScaling(Ability.ScalingProfile.NATURE).withEffect("nature").withStatuses(new AbilityStatus("weak", "enemy", 0.35)).withVisualResolution(Ability.VisualResolution.AOE)
            );
            case "Sunwarden" -> List.of(
                    new Ability("Sunstrike", 18, 5, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.DIVINE).withEffect("radiant").withStatuses(new AbilityStatus("vulnerable", "enemy", 0.35)),
                    new Ability("Lantern Blessing", 22, 7, Ability.AbilityKind.HEAL, "ally").withScaling(Ability.ScalingProfile.DIVINE).withStatuses(new AbilityStatus("shield", "target")),
                    new Ability("Noonflare", 25, 8, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.DIVINE).withEffect("ember").withStatuses(new AbilityStatus("burn", "enemy", 0.45)).withVisualResolution(Ability.VisualResolution.AOE),
                    new Ability("Hold Fast", 0, 4, Ability.AbilityKind.DEFEND, "self").withCooldown(2).withScaling(Ability.ScalingProfile.GUARD).withStatuses(new AbilityStatus("fortified", "self"), new AbilityStatus("shield", "self"))
            );
            case "Thornbinder" -> List.of(
                    new Ability("Thorn Lash", 20, 6, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.NATURE).withEffect("nature").withStatuses(new AbilityStatus("poison", "enemy", 0.35)),
                    new Ability("Root Burst", 15, 6, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.NATURE).withEffect("nature").withStatuses(new AbilityStatus("weak", "enemy", 0.5)).withVisualResolution(Ability.VisualResolution.AOE),
                    new Ability("Bitter Salve", 18, 6, Ability.AbilityKind.HEAL, "ally").withScaling(Ability.ScalingProfile.NATURE).withStatuses(new AbilityStatus("regeneration", "target")),
                    new Ability("Thorn Tax", 0, 7, Ability.AbilityKind.DEFEND, "self").withCooldown(3).withScaling(Ability.ScalingProfile.NATURE).withStatuses(new AbilityStatus("shield", "self"), new AbilityStatus("poison", "enemy", 0.35))
            );
            case "Stonebreaker" -> List.of(
                    new Ability("Hammer Blow", 24, 6, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.WEAPON).withEffect("bash"),
                    new Ability("Stone Guard", 0, 4, Ability.AbilityKind.DEFEND, "self").withCooldown(2).withScaling(Ability.ScalingProfile.GUARD).withStatuses(new AbilityStatus("fortified", "self"), new AbilityStatus("shield", "self")),
                    new Ability("Armor Splitter", 28, 8, Ability.AbilityKind.DAMAGE).withCooldown(2).withScaling(Ability.ScalingProfile.WEAPON).withEffect("bash").withStatuses(new AbilityStatus("vulnerable", "enemy")).withTags("armor_breaker"),
                    new Ability("Seismic Sweep", 16, 6, Ability.AbilityKind.DAMAGE, "all_enemies").withCooldown(3).withScaling(Ability.ScalingProfile.WEAPON).withEffect("bash").withStatuses(new AbilityStatus("weak", "enemy", 0.45)).withVisualResolution(Ability.VisualResolution.AOE)
            );
            case "Nightblade" -> List.of(
                    new Ability("Night Slash", 24, 6, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.AGILITY).withEffect("dark"),
                    new Ability("Poison Kiss", 18, 6, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.AGILITY).withEffect("poison").withStatuses(new AbilityStatus("poison", "enemy")),
                    new Ability("Execution Mark", 20, 7, Ability.AbilityKind.DAMAGE).withCooldown(3).withScaling(Ability.ScalingProfile.AGILITY).withEffect("execution_mark").withStatuses(new AbilityStatus("vulnerable", "enemy"), new AbilityStatus("weak", "enemy", 0.4)).withTags("execute"),
                    new Ability("Fade", 0, 4, Ability.AbilityKind.DEFEND, "self").withCooldown(2).withScaling(Ability.ScalingProfile.AGILITY).withEffect("dark").withStatuses(new AbilityStatus("haste", "self"), new AbilityStatus("shield", "self", 0.5))
            );
            case "Grovekeeper" -> List.of(
                    new Ability("Vine Flick", 16, 5, Ability.AbilityKind.DAMAGE).withScaling(Ability.ScalingProfile.NATURE).withEffect("nature").withStatuses(new AbilityStatus("weak", "enemy", 0.35)),
                    new Ability("Bloom Heal", 22, 7, Ability.AbilityKind.HEAL, "ally").withScaling(Ability.ScalingProfile.NATURE).withStatuses(new AbilityStatus("regeneration", "target")),
                    new Ability("Grove Hymn", 12, 8, Ability.AbilityKind.HEAL, "party").withCooldown(2).withScaling(Ability.ScalingProfile.NATURE).withEffect("grove_hymn").withStatuses(new AbilityStatus("regeneration", "target")).withTags("overheal_shield"),
                    new Ability("Root Memory", 18, 12, Ability.AbilityKind.HEAL, "party").withCooldown(4).withScaling(Ability.ScalingProfile.NATURE).withEffect("root_memory").withStatuses(new AbilityStatus("shield", "target"), new AbilityStatus("fortified", "target", 0.65)).withTags("revive_party", "overheal_shield")
            );
            default -> List.of(
                    new Ability("Shield Bash", 16, 4, Ability.AbilityKind.DAMAGE),
                    new Ability("Bulwark", 0, 3, Ability.AbilityKind.DEFEND),
                    new Ability("First Aid", 14, 5, Ability.AbilityKind.HEAL)
            );
        };
    }

    public static final Map<String, MonsterSpec> MONSTERS = Map.ofEntries(
            monster("slime", "slime", "Bog Slime", "slime", 24, 7, 1, 8, 4),

            monster("beast", "sheep", "Wild Sheep", "sheep", 26, 6, 1, 8, 4),
            monster("beast", "doe", "Woodland Doe", "doe", 30, 8, 1, 10, 6),
            monster("beast", "crystal_hare", "Crystal Hare", "crystal_hare", 32, 12, 3, 18, 14),
            monster("beast", "mountain_goat", "Mountain Goat", "mountain_goat", 34, 9, 2, 13, 7),
            monster("beast", "wolf", "Grey Wolf", "wolf", 32, 10, 2, 12, 7),
            monster("beast", "meadow_wolf", "Meadow Wolf", "wolf", 34, 11, 2, 14, 8),
            monster("beast", "stag", "Crown Stag", "stag", 44, 12, 3, 18, 12),
            monster("beast", "moss_stag", "Moss Stag", "moss_stag", 48, 13, 4, 22, 14),
            monster("beast", "frost_wolf", "Frost Wolf", "frost_wolf", 46, 15, 4, 30, 24),
            monster("beast", "snow_lynx", "Snow Lynx", "snow_lynx", 52, 18, 4, 38, 30),
            monster("beast", "bramble_boar", "Bramble Boar", "bramble_boar", 58, 17, 6, 36, 28),
            monster("beast", "stoneback_goat", "Stoneback Goat", "stoneback_goat", 54, 16, 6, 34, 26),
            monster("beast", "ember_tortoise", "Ember Tortoise", "ember_tortoise", 76, 18, 10, 48, 42),

            monster("bat", "bat", "Cave Bat", "bat", 22, 9, 1, 10, 5),
            monster("bat", "crypt_bat", "Crypt Bat", "crypt_bat", 36, 13, 2, 22, 15),

            monster("bandit", "bandit_cutthroat", "Bandit Cutthroat", "bandit_cutthroat", 42, 13, 2, 22, 18),
            monster("bandit", "bandit_archer", "Bandit Archer", "bandit_archer", 38, 14, 2, 24, 20),
            monster("bandit", "bandit_captain", "Bandit Captain", "bandit_captain", 78, 22, 6, 62, 52),

            monster("dragon", "red_dragon", "Red Dragon", "red_dragon", 132, 31, 10, 140, 125),
            monster("dragon", "elder_dragon", "Elder Dragon", "elder_dragon", 190, 39, 15, 220, 190),

            monster("drake", "marsh_drake", "Marsh Drake", "marsh_drake", 82, 22, 7, 62, 52),
            monster("drake", "mountain_drake", "Mountain Drake", "mountain_drake", 96, 25, 8, 76, 66),

            monster("goblin", "goblin", "Goblin Raider", "goblin", 36, 11, 2, 15, 10),
            monster("goblin", "goblin_scout", "Goblin Scout", "goblin_scout", 30, 10, 1, 13, 8),
            monster("goblin", "goblin_archer", "Goblin Archer", "goblin_archer", 34, 13, 1, 17, 12),
            monster("goblin", "goblin_trapper", "Goblin Trapper", "goblin_trapper", 38, 13, 2, 20, 15),
            monster("goblin", "goblin_skirmisher", "Goblin Skirmisher", "goblin_skirmisher", 44, 15, 3, 25, 18),
            monster("goblin", "goblin_shaman", "Goblin Shaman", "goblin_shaman", 48, 16, 3, 34, 26),
            monster("goblin", "hobgoblin_guard", "Hobgoblin Guard", "hobgoblin_guard", 62, 18, 6, 44, 34),
            monster("goblin", "goblin_warlord", "Goblin Warlord", "goblin_warlord", 82, 22, 7, 70, 58),
            monster("goblin", "goblin_king", "Goblin King", "goblin_king", 116, 27, 9, 120, 95),

            monster("giant", "hill_giant", "Hill Giant", "hill_giant", 124, 28, 9, 116, 96),
            monster("giant", "stone_giant", "Stone Giant", "stone_giant", 146, 30, 13, 145, 122),
            monster("giant", "fire_giant", "Fire Giant", "fire_giant", 154, 33, 11, 160, 138),

            monster("insect", "spider", "Cave Spider", "spider", 52, 14, 4, 23, 16),
            monster("insect", "glass_scorpion", "Glass Scorpion", "glass_scorpion", 58, 18, 6, 38, 32),
            monster("insect", "ash_scorpion", "Ash Scorpion", "ash_scorpion", 72, 22, 7, 52, 44),

            monster("orc", "orc", "Orc Brute", "orc", 66, 18, 5, 34, 28),
            monster("orc", "orc_raider", "Orc Raider", "orc_raider", 72, 20, 5, 46, 38),
            monster("orc", "orc_berserker", "Orc Berserker", "orc_berserker", 86, 24, 4, 66, 54),
            monster("orc", "orc_shaman", "Orc Shaman", "orc_shaman", 68, 21, 4, 58, 48),
            monster("orc", "orc_shieldbearer", "Orc Shieldbearer", "orc_shieldbearer", 92, 21, 9, 72, 60),
            monster("orc", "orc_champion", "Orc Champion", "orc", 112, 27, 9, 108, 92),

            monster("plant", "thornling", "Briar Thornling", "thornling", 44, 13, 4, 20, 13),

            monster("reptile", "sand_stalker", "Sand Stalker", "sand_stalker", 62, 19, 5, 36, 30),
            monster("reptile", "bog_beast", "Bog Beast", "bog_beast", 84, 23, 8, 58, 50),
            monster("reptile", "reed_serpent", "Reed Serpent", "reed_serpent", 64, 20, 5, 42, 34),
            monster("reptile", "river_eel", "River Eel", "river_eel", 38, 14, 2, 24, 18),

            monster("troll", "swamp_troll", "Swamp Troll", "swamp_troll", 108, 25, 9, 88, 74),
            monster("troll", "frost_troll", "Frost Troll", "frost_troll", 116, 27, 10, 102, 88),

            monster("undead", "skeleton", "Restless Skeleton", "skeleton", 42, 12, 3, 18, 12),
            monster("undead", "bone_knight", "Bone Knight", "skeleton", 92, 23, 9, 78, 64),
            monster("undead", "wraith", "Ash Wraith", "wraith", 58, 17, 4, 30, 21),
            monster("undead", "crypt_revenant", "Crypt Revenant", "wraith", 104, 25, 8, 92, 78),
            monster("undead", "elder_wraith", "Elder Wraith", "wraith", 128, 29, 10, 130, 110),

            monster("elemental", "ice_golem", "Ice Golem", "ice_golem", 96, 25, 11, 72, 66),
            monster("elemental", "ember_imp", "Ember Imp", "ember_imp", 54, 20, 4, 42, 36),

            monster("demon", "void_knight", "Void Knight", "void_knight", 168, 32, 15, 170, 120),
            monster("demon", "flame_herald", "Flame Herald", "flame_herald", 146, 36, 10, 165, 115),
            monster("demon", "frost_witch", "Frost Witch", "frost_witch", 138, 34, 11, 160, 110),
            monster("demon", "shadow_beast", "Shadow Beast", "shadow_beast", 176, 31, 13, 175, 125),
            monster("demon", "kharvok_banner_bound", "Kharvok the Banner-Bound", "void_knight", 190, 34, 16, 190, 135),
            monster("demon", "velmora_bell_drowned", "Velmora, the Bell-Drowned", "shadow_beast", 182, 33, 13, 185, 130),
            monster("beast", "rootmaw_stag", "Rootmaw Stag", "moss_stag", 150, 28, 10, 150, 95),
            monster("undead", "nameless_warden", "The Nameless Warden", "void_knight", 176, 31, 15, 175, 118),
            monster("insect", "hailback_broodmother", "Hailback Broodmother", "spider", 164, 30, 12, 160, 105),
            monster("demon", "sareth_cinder_knife", "Sareth, the Cinder Knife", "flame_herald", 170, 38, 9, 175, 122),
            monster("demon", "morvane_mercy_taker", "Morvane the Mercy-Taker", "void_knight", 205, 36, 16, 210, 150),
            monster("demon", "vaelthara", "Vaelthara, Demon Queen of Mercy", "demon_queen", 360, 44, 19, 520, 360),
            monster("demon", "demon_queen", "Vaelthara, Demon Queen of Mercy", "demon_queen", 360, 44, 19, 520, 360)
    );

    public static final Map<String, Quest> QUESTS = Map.ofEntries(
            quest("slime_help", "Marla's Remedy", "Clear three Bog Slimes from the road marshes.", "Bog Slime", 3, 35, 24),
            quest("crypt_lights", "Lights in the Crypt", "Defeat two Restless Skeletons below the old stone dungeon.", "Restless Skeleton", 2, 60, 42),
            quest("orc_siege", "Siege Warning", "Break the raider vanguard by defeating one Orc Brute.", "Orc Brute", 1, 95, 70),
            quest("shadow_swarm", "Swarm in the Rafters", "Defeat three Cave Bats from the old dungeon roof.", "Cave Bat", 3, 75, 58),
            quest("wraith_hunt", "Wraith Hunt", "Defeat one Elder Wraith and restore the crypt ward.", "Elder Wraith", 1, 140, 110),
            quest("broodmother", "The Broodmother Below", "Hunt the Acid Broodmother deep in the lower vault.", "Acid Broodmother", 1, 210, 160),
            quest("winter_fangs", "Winter Fangs", "Cull two Frost Wolves stalking the northern pass.", "Frost Wolf", 2, 135, 105),
            quest("ice_golem_marks", "Marks in the Whiteout", "Defeat one Ice Golem grinding the mountain cairns into powder.", "Ice Golem", 1, 130, 98,
                    "Olin: The cairns are being crushed flat. Snow is bad enough without the landmarks deciding to walk away.",
                    "Olin: Find the Ice Golem by the broken stone marks. It moves slowly, which is the only polite thing about it.",
                    "Olin: If the pass is quiet, bring the tale back before the snow edits it.",
                    "Olin: The cairns will stand another season. Around here that counts as optimism."),
            quest("sand_stalker_hunt", "Glass Tracks", "Defeat two Sand Stalkers following caravans out of Dunewick.", "Sand Stalker", 2, 118, 88,
                    "Imani: Something has learned to follow shade instead of footprints. That is bad news for anyone with a shadow.",
                    "Imani: Hunt two Sand Stalkers where the dunes sing underfoot.",
                    "Imani: The glass tracks have stopped. Come back while the wells are still calm.",
                    "Imani: Good. The next caravan can argue about prices instead of monsters."),
            quest("bog_beast_bounty", "Teeth Under the Planks", "Defeat one Bog Beast worrying the walkways near Mireford.", "Bog Beast", 1, 122, 92,
                    "Vell: One of the deep things has started chewing pilings. I would like my floor to stay above my ankles.",
                    "Vell: The Bog Beast surfaces where the water bubbles without wind.",
                    "Vell: If the planks stopped shivering, come tell me.",
                    "Vell: The village sounds like wood again instead of teeth. That is an improvement."),
            quest("thornling_roots", "Roots with Knives", "Defeat three Briar Thornlings crowding the green road out of Oakhaven.", "Briar Thornling", 3, 72, 56,
                    "Lin: The hedges are throwing knives at wagons again. I miss when plants kept their opinions private.",
                    "Lin: Cut back three Briar Thornlings before they stitch the road shut.",
                    "Lin: If the road is passable, I have salve and a very smug thank-you ready.",
                    "Lin: The hedges look offended. Perfect. They can be offended from a safer distance."),
            quest("trapline_cleanup", "Tripwires at Dawn", "Defeat two Goblin Trappers setting snares along Highwall's patrol road.", "Goblin Trapper", 2, 96, 72,
                    "Yaro: Goblin trappers are patient, tidy, and terrible neighbors.",
                    "Yaro: Break two traplines before sunrise patrol starts collecting arrows with their boots.",
                    "Yaro: If you found the trappers, report before someone congratulates a bush for bravery.",
                    "Yaro: Clean work. Highwall's riders may even keep their ankles today."),
            quest("ember_imp_coals", "Coals That Laugh", "Defeat three Ember Imps nesting in the sun-baked ruins near Sanctum.", "Ember Imp", 3, 104, 82,
                    "Kera: Ember Imps have started laughing from cold hearths. That means the ruins are feeding them.",
                    "Kera: Snuff three of them before the desert wind carries sparks into town.",
                    "Kera: If the laughter stopped, Sanctum owes you quiet.",
                    "Kera: The hearths are only hearths again. A small mercy, but a useful one."),
            quest("spider_silk_tangle", "Silk in the Bells", "Defeat two Cave Spiders nesting above Belltower's rope loft.", "Cave Spider", 2, 86, 64,
                    "Corso: Bell ropes should not twitch when no one pulls them.",
                    "Corso: Clear two Cave Spiders before the bells start ringing warnings nobody asked for.",
                    "Corso: If the loft is clean, come back down and pretend this was ordinary maintenance.",
                    "Corso: The ropes are rope again. That is exactly as exciting as I like rope to be."),
            quest("bread_for_road", "Bread for the Road", "Gather four wheat sheaves from the marked farm outside Oakhaven.", "Wheat Sheaf", 4, 30, 20,
                    Quest.ObjectiveKind.GATHER, "farmland", 0, "location_farmland_wheat", null,
                    "Edda: We can mend cloaks and wheels, but not empty bellies. The farm west of town still has standing wheat.",
                    "Edda: Bring what you can carry. Four good sheaves should keep the roadwatch fed.",
                    "Edda: That is enough grain for bread and barter both. Come warm your hands.",
                    "Edda: Fresh bread has a way of making frightened folk brave again."),
            quest("stolen_supplies", "Crates in the Smoke", "Recover two stolen supply crates from the marked raider camp.", "Supply Crate", 2, 55, 38,
                    Quest.ObjectiveKind.GATHER, "goblin_camp", 0, "location_camp_crates", null,
                    "Mira: Raiders dragged our medical crates into a camp by the road. They left wheel ruts and bad singing behind.",
                    "Mira: Two marked crates will do. Do not sort the labels while arrows are flying.",
                    "Mira: Those are ours. Bring them here before someone mistakes bandages for kindling.",
                    "Mira: Splints, salves, clean cloth. You just saved more lives than a sword usually does."),
            quest("goblin_crown", "Crown in the Camp", "Hunt the Goblin King in the marked raider camp and break the camp's command.", "Goblin King", 1, 150, 120,
                    Quest.ObjectiveKind.DEFEAT, "goblin_camp", 1, "goblin_king", "goblin_king",
                    "Rook: The camp has a crowned brute giving orders now. Leave him alive and every ambush gets smarter.",
                    "Rook: The king keeps to the center of the marked camp. When you see the crown, make him answer for it.",
                    "Rook: If the crown is gone, Highwall's road can breathe again. Report in.",
                    "Rook: Good. A camp without a king argues with itself long enough for people to get home."),
            quest("hay_for_horses", "Hay for the Watch", "Gather three hay bales from the marked farm so the road horses can keep moving.", "Hay Bale", 3, 32, 22,
                    Quest.ObjectiveKind.GATHER, "farmland", 0, "location_farmland_hay_bales", null,
                    "Joss: The watch has horses and no feed. Take clean hay from the west field before rain spoils it.",
                    "Joss: Three bales should hold the team through another patrol.",
                    "Joss: That is enough. Bring it back before the horses eat fence posts out of spite.",
                    "Joss: Good hay is boring until it is the reason help arrives on time."),
            quest("scarecrow_watch", "Eyes in the Stalks", "Inspect the old scarecrow in the marked farm and make sure raiders have not used it as a dead drop.", "Scarecrow", 1, 24, 18,
                    Quest.ObjectiveKind.VISIT, "farmland", 0, "location_farmland_scarecrow", null,
                    "Rowan: Someone keeps turning our scarecrow toward the road. I would rather it be wind than raiders.",
                    "Rowan: Check the scarecrow closely. Look for cords, carved marks, anything too clever for straw.",
                    "Rowan: You saw enough. Come tell me whether I should laugh or worry.",
                    "Rowan: A marked knot on the pole. I knew it. We will move the stores tonight."),
            quest("camp_smoke", "Smoke Signals", "Douse two signal fires in the marked raider camp before Highwall's patrol rides past.", "Campfire", 2, 58, 42,
                    Quest.ObjectiveKind.VISIT, "goblin_camp", 0, "location_camp_fire", null,
                    "Lysa: Raiders signal with greenwood smoke. Put out two fires and their ambush will be blind.",
                    "Lysa: Kick dirt over the marked fires. Quick hands, quiet feet.",
                    "Lysa: No smoke means no signal. Report back while the road is still ours.",
                    "Lysa: The patrol passed under clear air. That is the kind of victory people notice only if it fails."),
            quest("camp_ledger", "Ledger Under Canvas", "Search the marked raider tent for the list of stolen tolls.", "Raider Tent", 1, 66, 46,
                    Quest.ObjectiveKind.VISIT, "goblin_camp", 2, "location_camp_tent", null,
                    "Toma: Raiders tax the dunes now, apparently. Find their ledger and I can tell who paid to stay alive.",
                    "Toma: Their tent should be marked. Look under bedding, cookpots, and anything that smells worse than both.",
                    "Toma: If you found the ledger, Dunewick can settle debts without swinging at shadows.",
                    "Toma: Names, routes, bribes. Paper can cut deeper than a knife when it reaches the right hands."),
            quest("palisade_gaps", "Gaps in the Stakes", "Mark three weak palisade points in the raider camp for the next village militia push.", "Palisade", 3, 70, 52,
                    Quest.ObjectiveKind.VISIT, "goblin_camp", 3, "location_camp_palisade", null,
                    "Noll: Mireford can raise a militia, but only if we know where the camp wall wants to fall.",
                    "Noll: Mark three weak palisade points. Rotten stakes, loose lashings, soft mud under posts.",
                    "Noll: Three gaps are enough for farmers to become a problem.",
                    "Noll: Good marks. We will make noise at one gap and enter through another."),
            quest("grave_rubbings", "Names Beneath Lichen", "Take rubbings from three marked tombstones near Stonegate.", "Tombstone", 3, 64, 48,
                    Quest.ObjectiveKind.VISIT, "graveyard", 0, "location_graveyard_tombstones", null,
                    "Pela: The old grave names are disappearing under lichen, and missing names make restless dead easier to forget.",
                    "Pela: Take rubbings from three marked stones. Charcoal, paper, patience.",
                    "Pela: Bring the names back while they are still names.",
                    "Pela: These are legible enough. The Archive will speak them again."),
            quest("skull_wards", "Skulls on the Fence", "Inspect two skull ward markers around the graveyard and confirm which still hold a charm.", "Ward Marker", 2, 88, 66,
                    Quest.ObjectiveKind.VISIT, "graveyard", 1, "location_graveyard_skull_marker", null,
                    "Cal: Some ward skulls still hum. Some just grin. I need to know which are lying.",
                    "Cal: Inspect two marked skull wards. Do not put your fingers in the eye sockets unless you like surprises.",
                    "Cal: If the charms are spent, Sanctum must send replacements before moonrise.",
                    "Cal: One charm alive, one cold. That is bad news, but useful bad news."),
            quest("winter_records", "Frost on Old Stone", "Inspect two marked tombstones near Frosthollow before snow buries their inscriptions.", "Frosted Tombstone", 2, 78, 58,
                    Quest.ObjectiveKind.VISIT, "graveyard", 2, "location_graveyard_tombstones", null,
                    "Asta: Snowrest keeps cairn records, but Frosthollow's old stones are vanishing under ice.",
                    "Asta: Read two marked stones before weather edits them for us.",
                    "Asta: Come back with what the frost left readable.",
                    "Asta: That is enough to mend the ledger. The dead deserve accurate bookkeeping."),
            quest("wolf_pelt_order", "Ten Winter Pelts", "Defeat ten Grey Wolves so Oakhaven's tanner can finish warm cloaks for the roadwatch.", "Grey Wolf", 10, 120, 90,
                    "Sori: The watch needs cloaks, and the wolves have been rude enough to bring the pelts to the road.",
                    "Sori: Ten clean pelts will do. Try to leave more cloak than claw.",
                    "Sori: That is enough wolf trouble for one season. Bring the pelts here.",
                    "Sori: Warm cloaks, fewer bitten travelers, and my apprentice stops looking haunted. Good work."),
            quest("peddler_ledger", "A Peddler's Ledger", "Defeat two Goblin Scouts who took a road peddler's account book.", "Goblin Scout", 2, 68, 52,
                    "Orren: Goblin scouts stole my ledger. I can forgive stolen raisins, but not arithmetic.",
                    "Orren: Two scouts ran east with it. If they sell my debt notes, half the market will become dramatic.",
                    "Orren: You have the look of someone who solved a math problem with steel.",
                    "Orren: My ledger is back. I will travel with your party if you need someone who can count arrows and coins."),
            quest("goat_bell_roundup", "Bells on the Ridge", "Defeat three Mountain Goats that keep smashing Snowrest's warning bells.", "Mountain Goat", 3, 64, 48,
                    "Una: My goats have joined the mountain's opinion of architecture.",
                    "Una: Drive off three bell-smashers before the pass forgets how to warn us.",
                    "Una: If the bells survived, come collect your thanks before another goat forms a committee.",
                    "Una: The bells ring properly again. Loud, honest, and only a little dented."),
            quest("market_road_clearance", "Market Road Clearance", "Defeat four Goblin Raiders harassing small caravans near Riverside.", "Goblin Raider", 4, 92, 70,
                    "Nessa: A market is only as brave as the road into it.",
                    "Nessa: Four raiders have been taxing baskets, boots, and patience. Clear them out.",
                    "Nessa: If the road is moving again, I have coin and a better story ready.",
                    "Nessa: Riverside will hear wagons instead of warnings tonight."),
            mainStoryQuest("ms_wake_ashes", "Wake Among Ashes", "Maelis asks you to inspect the burned road shrine and broken ward-stone near Oathstead before anyone calls the attack a normal raid.", "Broken Ward-Stone", 2, 70, 60,
                    "maelis", "ms_road_dust", Quest.ObjectiveKind.VISIT, "farmland", 1, "location_farmland_scarecrow", null,
                    "Maelis: You look like someone death rejected for paperwork reasons. Do not call what happened mercy. Mercy leaves a person whole.",
                    "Maelis: Inspect the road shrine and ward-stone. Bring me facts, not panic.",
                    "Maelis: The stone hums badly enough to worry me. Come back.",
                    "Maelis: I believe enough to worry. In Alderfall, that is the beginning of trust."),
            mainStoryQuest("ms_road_dust", "Proof in the Road Dust", "Gather demon ash, broken ward fragments, and the charred banner that prove Vaelthara's attack was a warning to every kingdom.", "Demon Ash", 3, 85, 75,
                    "maelis", "ms_oathstead_stand", Quest.ObjectiveKind.GATHER, "goblin_camp", 4, "location_camp_fire", null,
                    "Maelis: Crowns will ask for proof. Priests will ask for signs. Here, we start with ash under the fingernails.",
                    "Maelis: Demon ash clings warm. Gather enough that Archive City cannot pretend this is campfire soot.",
                    "Maelis: The ash, ward fragments, and burned banner agree. Bring them here.",
                    "Maelis: This was not a raid. It was a warning. Selene in Archive City must see the shape of it."),
            mainStoryQuest("ms_oathstead_stand", "Oathstead Must Stand", "Before chasing ancient answers, clear wolves from the road and help Oathstead gather crates, wheat, and enough courage to remain a camp.", "Grey Wolf", 6, 105, 90,
                    "maelis", "ms_camp_defending", Quest.ObjectiveKind.DEFEAT, null, 0, null, "wolf",
                    "Maelis: If the world is ending, it can wait until we have enough wheat for supper. Hungry people fight each other before demons.",
                    "Maelis: Clear six wolves near the camp road while the others stack crates and count grain.",
                    "Maelis: The road is quiet enough to hear ourselves think. Return.",
                    "Maelis: Oathstead is not safe, but it is no longer helpless. Go to Archive City."),
            mainStoryQuest("ms_names_dust", "Names Under Dust", "Selene has you inspect burned ledgers, a sealed oath cabinet, and the missing Demon War shelf in Archive City.", "Burned Royal Ledger", 3, 120, 110,
                    "selene", "ms_stolen_index", Quest.ObjectiveKind.VISIT, "graveyard", 0, "location_graveyard_tombstones", null,
                    "Selene: History does not vanish. It is moved, relabeled, sealed, misquoted, or made inconvenient.",
                    "Selene: Inspect the ledger, oath cabinet, and missing war section. The absence is the evidence.",
                    "Selene: The missing records have left a shape behind. Bring me the pattern.",
                    "Selene: The old war was locked behind twelve socket stones. Someone removed the twelfth reference by hand."),
            mainStoryQuest("ms_stolen_index", "The Stolen Index", "Bandits at Crowhook stole the index pages that explain where the old portal record was hidden.", "Bandit Cutthroat", 8, 135, 125,
                    "selene", "ms_first_socket", Quest.ObjectiveKind.DEFEAT, "bandit_camp", 0, "bandit_cutthroat", "bandit_cutthroat",
                    "Selene: The archive survived fires, floods, kings, and reforms. Mostly kings. It can survive bandits if you hurry.",
                    "Selene: Recover the stolen pages from Crowhook. Ink matters when kingdoms are trying not to remember.",
                    "Selene: The pages are damaged, but the phrase survives. Return.",
                    "Selene: Twelve stones for the road no crown may own. Now I can show you the first socket."),
            mainStoryQuest("ms_first_socket", "The First Socket", "Visit the Old Oath Vault beneath Archive City and inspect the socket mural and empty pedestal.", "Royal Socket Mural", 2, 150, 145,
                    "selene", null, Quest.ObjectiveKind.VISIT, "graveyard", 1, "location_graveyard_skull_marker", null,
                    "Selene: Truth is not a torch. It is a blade. Hold it wrongly and everyone bleeds.",
                    "Selene: Inspect the mural and pedestal. If the vault accepts you, take only what answers.",
                    "Selene: The empty socket recognized the old oath. Return carefully.",
                    "Selene: The Stone of Memory rests in your pack. Alderfall's oldest lie has become a weight you can carry."),
            mainStoryQuest("ms_watchtower_bells", "Watchtower Without Bells", "Odrick sends you to inspect the frozen watchtower, dead campfire, broken signal bell, and blood marks near Highwall Pass.", "Broken Signal Bell", 3, 115, 105,
                    "odrick", "ms_raiders_pass", Quest.ObjectiveKind.VISIT, "graveyard", 2, "location_graveyard_tombstones", null,
                    "Odrick: I have seen men blame demons for bad boots and poor scouting. Bring back my road.",
                    "Odrick: Inspect the tower signs. Highwall soldiers do not abandon bells while their hands remain attached.",
                    "Odrick: The bell was cut, the fire abandoned, and the blood carried off. Report.",
                    "Odrick: Panic is just another enemy formation. Now we know where its front stands."),
            mainStoryQuest("ms_raiders_pass", "Raiders at the Pass", "Break the raiders and orc brutes using Highwall's stolen roadwatch signals.", "Orc Brute", 3, 140, 130,
                    "odrick", "ms_frosthollow_standard", Quest.ObjectiveKind.DEFEAT, "goblin_camp", 2, "orc", "orc",
                    "Odrick: If Vaelthara commands raiders, she understands war. If she commands fear, she is worse.",
                    "Odrick: Break the brutes at the pass. The raiders will learn what formation means from the other side.",
                    "Odrick: The pass is bloodied but open. Return before command turns into gossip.",
                    "Odrick: Highwall listens now. I can name the demon carrying our banner."),
            mainStoryQuest("ms_frosthollow_standard", "Frosthollow Standard", "Enter Frosthollow Cave and defeat Kharvok the Banner-Bound, the war demon stitching surrendered flags into command magic.", "Kharvok the Banner-Bound", 1, 190, 170,
                    "odrick", null, Quest.ObjectiveKind.DEFEAT, "graveyard", 2, "void_knight", "kharvok_banner_bound",
                    "Odrick: Kharvok counts knees bent. Make him count losses.",
                    "Odrick: Expect raiders near the banner. It teaches dead men where to march.",
                    "Odrick: If the banner fell, bring me the iron truth inside it.",
                    "Odrick: Take the Stone of Iron. Courage without vanity has weight."),
            mainStoryQuest("ms_shrine_shadow", "Shrine Without Shadow", "Solari asks you to inspect the cracked sun altar, dead caravan beasts, and old ward-stone at the Sunken Shrine.", "Cracked Sun Altar", 3, 115, 105,
                    "solari", "ms_caravan_glass", Quest.ObjectiveKind.VISIT, "goblin_camp", 5, "location_camp_fire", null,
                    "Solari: The sun does not forgive. It reveals.",
                    "Solari: Inspect the altar, caravan dead, and ward-stone. Vaelthara burns like guilt, not fire.",
                    "Solari: The shrine has shown its wound. Return while the heat is honest.",
                    "Solari: The darkness has a body now. We will give it a boundary."),
            mainStoryQuest("ms_caravan_glass", "Caravan of Glass", "Clear ember imps from the shrine road and recover crates and sun oil before the rite can be attempted.", "Ember Imp", 6, 130, 120,
                    "solari", "ms_ember_socket_rite", Quest.ObjectiveKind.DEFEAT, "goblin_camp", 5, "ember_imp", "ember_imp",
                    "Solari: Ember remembers shape: bone, glass, oath, sin. All leave color in flame.",
                    "Solari: Snuff six imps while the caravan stores are gathered. Careless fire is bad worship.",
                    "Solari: The road is hot but clear. Bring the report.",
                    "Solari: You have earned the right to carry heat without letting it rule you."),
            mainStoryQuest("ms_ember_socket_rite", "The Ember Socket Rite", "Gather ember crystal, sunsteppe glass, and demon ash, then visit the Sanctum forge shrine to shape the Stone of Ember.", "Sanctum Forge Shrine", 1, 150, 145,
                    "solari", null, Quest.ObjectiveKind.VISIT, "goblin_camp", 4, "location_camp_fire", null,
                    "Solari: You ask for a stone. Stones are easy. It is the right to carry one that must be earned.",
                    "Solari: Bring ember crystal, sunsteppe glass, and demon ash to the forge shrine. The rite will know whether you lied.",
                    "Solari: The shrine took the ingredients and left a breathing coal. Return.",
                    "Solari: The Stone of Ember is warm without burning. That is how you know it still belongs to us."),
            mainStoryQuest("ms_bell_alone", "The Bell That Rang Alone", "Ysra sends you to inspect the marsh bell tower, wet bell rope, muddy footprints, and reed shrine.", "Bell Rope", 3, 115, 105,
                    "ysra", "ms_medicine_mireford", Quest.ObjectiveKind.VISIT, "graveyard", 1, "location_graveyard_skull_marker", null,
                    "Ysra: A bell rings for fog, flood, funeral, or fool. Today I am deciding which you are.",
                    "Ysra: Inspect the rope, footprints, and reed shrine. In the Fenlands, silence is rarely empty.",
                    "Ysra: The bell rang with no hand on it. Return before the marsh improves the story.",
                    "Ysra: If Vaelthara reached the bells, she reached the roads beneath the water."),
            mainStoryQuest("ms_medicine_mireford", "Medicine for Mireford", "Gather fever reed and clean water skins, deliver them to Mireford, and prove the Fenlands can trust you with more than rumors.", "Fever Reed", 6, 125, 115,
                    "ysra", "ms_miredepth_below", Quest.ObjectiveKind.GATHER, "farmland", 2, "location_farmland_wheat", null,
                    "Ysra: City folk hear mud and think filth. We hear mud and know whether the road will hold.",
                    "Ysra: Gather fever reed and water skins for Mireford. Medicine first, mysteries second.",
                    "Ysra: Mireford has what it needs for tonight. Return.",
                    "Ysra: Useful hands make better witnesses. Now I will name the cave."),
            mainStoryQuest("ms_miredepth_below", "Miredepth Below", "Enter Miredepth Cave and defeat Velmora, the Bell-Drowned, before her drowned bells call more dead to the surface.", "Velmora, the Bell-Drowned", 1, 190, 170,
                    "ysra", null, Quest.ObjectiveKind.DEFEAT, "graveyard", 1, "shadow_beast", "velmora_bell_drowned",
                    "Ysra: Velmora kept ringing during the flood. Vaelthara turned devotion into hunger.",
                    "Ysra: Go below. If the drowned answer, make sure they do not answer twice.",
                    "Ysra: The underwater bell is quiet. Return.",
                    "Ysra: The Stone of Tides is heavier than it looks because it is full of old warnings."),
            mainStoryQuest("ms_toll_ledger", "The Toll Ledger", "Mirella asks you to inspect Briarbridge's toll ledger, empty crates, and silent bridge guard post.", "Toll Ledger", 3, 115, 105,
                    "mirella", "ms_redcap_trade", Quest.ObjectiveKind.VISIT, "farmland", 0, "location_farmland_scarecrow", null,
                    "Mirella: Panic has logistics. Find me the missing supplies, and I will decide whether your apocalypse deserves space on my desk.",
                    "Mirella: Inspect the ledger, crates, and guard post. A lying ledger is still a confession.",
                    "Mirella: The numbers are too neat. Come back.",
                    "Mirella: The Reach will fall by closed bridges before glorious battles if we let this spread."),
            mainStoryQuest("ms_redcap_trade", "Redcap Trade", "Clear Redcap Goblins from the stolen supply route and recover their crates for Riverside Reach.", "Goblin Raider", 8, 135, 125,
                    "mirella", "ms_glowing_mud", Quest.ObjectiveKind.DEFEAT, "goblin_camp", 0, "goblin", "goblin",
                    "Mirella: A kingdom is roads, receipts, grain, and tomorrow's bridge still being there.",
                    "Mirella: Defeat the redcaps holding our crates. Try to leave me enough evidence for a polite scandal.",
                    "Mirella: The route moves again. Return.",
                    "Mirella: River barons can smell fear through sealed letters. This buys us time."),
            mainStoryQuest("ms_glowing_mud", "Glowing Thing in the Mud", "Only after Mirella names the hunt do Redcap scavengers reveal the mud-caked socket stone in their stolen goods.", "Goblin Skirmisher", 1, 150, 145,
                    "mirella", null, Quest.ObjectiveKind.DEFEAT, "goblin_camp", 0, "goblin_skirmisher", "goblin_skirmisher",
                    "Mirella: One scavenger found something glowing and decided mud was a vault. Efficient, in its way.",
                    "Mirella: Kill the marked scavenger. The stone will not show itself before this hunt is sworn.",
                    "Mirella: The mud gave up a hollow little answer. Return.",
                    "Mirella: The Stone of Hunger looks back. Do not let it learn your habits."),
            mainStoryQuest("ms_orchard_ward", "The Orchard Ward", "Elder Rowan needs corrupted trees, the old orchard ward, and the Rootmaw Stag dealt with before the roots learn hatred.", "Rootmaw Stag", 1, 170, 150,
                    "rowan", null, Quest.ObjectiveKind.DEFEAT, "farmland", 2, "moss_stag", "rootmaw_stag",
                    "Elder Rowan: Roots remember where blood fell. Folk forget because folk are merciful to themselves.",
                    "Elder Rowan: Inspect the trees and ward, then face the stag if the ash has taken it.",
                    "Elder Rowan: Rootmaw is down. Bring back what the ward surrendered.",
                    "Elder Rowan: The Stone of Roots is not clean, but it still remembers how to hold."),
            mainStoryQuest("ms_names_cold_stone", "Names on Cold Stone", "Gravekeeper Hollis asks you to inspect scratched tombstones and stop the Nameless Warden in Stonegate Crypt.", "The Nameless Warden", 1, 170, 150,
                    "hollis", null, Quest.ObjectiveKind.DEFEAT, "graveyard", 0, "void_knight", "nameless_warden",
                    "Hollis: Scratch a name from stone, and you do not erase a person. You untie them.",
                    "Hollis: Read the stones, mind the crypt door, and put the Warden back where memory belongs.",
                    "Hollis: The dead are less confused. Return.",
                    "Hollis: The Stone of Graves keeps names badly, which is still better than forgetting."),
            mainStoryQuest("ms_cold_road", "The Cold Road", "Captain Elric needs firewood, medicine crates, frost wolves cleared, and the Hailback Broodmother stopped.", "Hailback Broodmother", 1, 170, 150,
                    "elric", null, Quest.ObjectiveKind.DEFEAT, "graveyard", 2, "spider", "hailback_broodmother",
                    "Captain Elric: I have sick children, missing sleds, and one road full of wolves. Put your prophecy in line.",
                    "Captain Elric: Bring survival supplies first. If the broodmother guards them, kill the problem.",
                    "Captain Elric: The crates are out of the webbing. Return.",
                    "Captain Elric: The Stone of Frost came from the nest cold enough to make a box shiver."),
            mainStoryQuest("ms_missing_bell_rope", "The Missing Bell Rope", "Bellwright Nessa repairs the bell network with reed fiber, iron hooks, and the old foundation where a quiet stone is hidden.", "Old Bell Foundation", 2, 145, 130,
                    "nessa", null, Quest.ObjectiveKind.VISIT, "graveyard", 1, "location_graveyard_skull_marker", null,
                    "Bellwright Nessa: A bell is not just metal. It is distance made useful.",
                    "Bellwright Nessa: Inspect the winch, gather rope and hooks, then check the old foundation. Disasters begin with frayed rope.",
                    "Bellwright Nessa: The foundation hums under the repair. Return.",
                    "Bellwright Nessa: The Stone of Bells gives no sound, but nearby metal waits to ring."),
            mainStoryQuest("ms_blackvault_mark", "The Blackvault Mark", "Ash-Scribe Damar sends you into Blackvault Ruins to inspect skull wards, the black altar, and defeat Sareth, the Cinder Knife.", "Sareth, the Cinder Knife", 1, 185, 165,
                    "damar", null, Quest.ObjectiveKind.DEFEAT, "goblin_camp", 5, "flame_herald", "sareth_cinder_knife",
                    "Ash-Scribe Damar: Blackvault has been dead for a century. Yesterday, it started making new shadows.",
                    "Ash-Scribe Damar: Read the skull wards and black altar. Sareth kills witnesses who understand signs.",
                    "Ash-Scribe Damar: The Cinder Knife is ash. Return with the mark before it cools.",
                    "Ash-Scribe Damar: The Stone of Ash leaves soot on the hand, but the hand remains yours."),
            mainStoryQuest("ms_camp_defending", "A Camp Worth Defending", "Build Oathstead's defenses, inspect the central oath marker, and repel Morvane the Mercy-Taker's raid.", "Morvane the Mercy-Taker", 1, 210, 180,
                    "maelis", "ms_kingdoms_answer", Quest.ObjectiveKind.RAID_DEFENSE, "oathstead_camp", 0, "player_village_quest_board", "morvane_mercy_taker",
                    "Maelis: Oathstead does not need heroes first. It needs hands. Then it needs a gate that closes.",
                    "Maelis: Gather timber, stone, and food. When Morvane comes to punish hope, make the camp answer.",
                    "Maelis: The raid broke against the palisade. Return to the oath marker.",
                    "Maelis: Oathstead survived. The ground is burned, and the people are still here."),
            mainStoryQuest("ms_kingdoms_answer", "The Kingdoms Answer", "Carry oath fragments between Mirella, Odrick, Selene, Ysra, Solari, and Maelis once the other stones prove Alderfall can trust you.", "Kingdom Oath", 5, 180, 160,
                    "maelis", "ms_twelve_stones_gate", Quest.ObjectiveKind.VISIT, "farmland", 1, "location_farmland_scarecrow", null,
                    "Maelis: You came here half-dead. Now half the realm is following your footsteps. Try not to look too surprised.",
                    "Maelis: Visit the five kingdom voices. We need trust from roads, ledgers, bells, records, and fire.",
                    "Maelis: Five answers, one cause. Come home.",
                    "Maelis: For the first time in living memory, the five kingdoms have given part of their trust to the same cause."),
            mainStoryQuest("ms_twelve_stones_gate", "The Twelve Stones of the Gate", "Bring all twelve socket stones to the Old Gate of Alderfall and activate the portal to the Hollow Throne.", "Old Gate of Alderfall", 1, 0, 220,
                    "maelis", null, Quest.ObjectiveKind.VISIT, null, 0, null, null,
                    "Maelis: The Old Gate belongs to no crown. That is why it may still answer.",
                    "Maelis: Carry the twelve stones to the Gate. When it opens, Vaelthara gets the survivor she should have killed.",
                    "Maelis: The Gate is awake. Step through only when you mean it.",
                    "Maelis: The Old Gate opens. Alderfall's broken oaths have become a road."),
            companionStagedQuest("seraphine_chain_1", "A Signature in Red", "Seraphine breaks into a Riverside counting house for a contract that should have died with its owner.", 82, 64,
                    "seraphine", "seraphine_chain_2", List.of(
                            questStage("seraphine_chain_1_ledger", "Read The Toll Ledger", "Suspicious Toll Ledger", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 0, "quest_document_bundle", null, "", "",
                                    "Seraphine: Careful. In Riverside, if you stare at a ledger too long, someone charges you rent.",
                                    "Seraphine: Start with the ledger. Official ink loves confessing in columns nobody reads.",
                                    "Seraphine: The tolls are being charged to dead accounts. Charming, in the way rot is charming.",
                                    "Seraphine: Good. The ledger blinked first."),
                            questStage("seraphine_chain_1_seal", "Find The Red Seal", "Wine-Stained Noble Seal", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 0, "quest_ward_marker", null, "", "",
                                    "Seraphine: The desk, the seal, the ledger. All very dead on paper and inconveniently alive in practice.",
                                    "Seraphine: Find the red seal. If it is still warm, do not touch it with bare hope.",
                                    "Seraphine: That seal should be dead. Its owner certainly is.",
                                    "Seraphine: Someone is making corpses sign paperwork again."),
                            questStage("seraphine_chain_1_choice", "Choose The First Lie", "Counting House Evidence", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "seraphine_first_lie",
                                    "Seraphine: Now choose how we leave this room: quiet, righteous, or extremely memorable.",
                                    "Seraphine: Freedom begins with evidence, but style is not forbidden.",
                                    "Seraphine: Chosen. Riverside will dislike all versions, which is how I know we are near truth.",
                                    "Seraphine: You did not ask me to be grateful for being believed. Excellent start."))),
            companionStagedQuest("seraphine_chain_2", "Bridge Toll Lies", "Forged bridge notes reveal that supplies and debt records are being redirected through false tolls.", 104, 82,
                    "seraphine", "seraphine_chain_3", List.of(
                            questStage("seraphine_chain_2_notes", "Gather Forged Notes", "Forged Toll Notes", 3, Quest.ObjectiveKind.GATHER,
                                    "farmland", 0, "quest_document_bundle", null, "", "",
                                    "Seraphine: Numbers lie better than people. People sweat. Ink just sits there looking official.",
                                    "Seraphine: Gather the forged notes. Let the bridge tax its own dignity for once.",
                                    "Seraphine: These notes match too neatly. Mistakes are ugly. This is elegant.",
                                    "Seraphine: Elegant fraud is still fraud, just better dressed."),
                            questStage("seraphine_chain_2_crates", "Inspect Empty Crates", "Empty Supply Crates", 2, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 0, "location_camp_crates", null, "", "",
                                    "Seraphine: Empty crates tell a better story than full pockets.",
                                    "Seraphine: Check the crates. Supplies do not vanish; they are persuaded elsewhere.",
                                    "Seraphine: Medicine, rope, grain. All billed, none delivered.",
                                    "Seraphine: Someone built a debt machine out of hunger."),
                            questStage("seraphine_chain_2_witnesses", "Ask Bridge Witnesses", "Bridge Toll Witnesses", 2, Quest.ObjectiveKind.ASK_AROUND,
                                    null, 0, null, null, "", "",
                                    "Seraphine: People pay a false toll twice: once in coin, once in shame.",
                                    "Seraphine: Ask around. I want the version spoken before fear edits it.",
                                    "Seraphine: The witnesses agree on the clerk stamp and disagree on the collector's face.",
                                    "Seraphine: Good. A stamp can be followed. A face can be bought."))),
            companionStagedQuest("seraphine_chain_3", "The Clerk Who Vanished", "A clerk who helped build the false contracts disappears before he can decide whether fear or conscience owns him.", 126, 100,
                    "seraphine", "seraphine_chain_4", List.of(
                            questStage("seraphine_chain_3_room", "Search The Clerk's Room", "Missing Clerk's Room", 2, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_document_bundle", null, "", "",
                                    "Seraphine: Clerks do not vanish. They file themselves somewhere inconvenient.",
                                    "Seraphine: Look for the desk, burned book, and loose floorboard. Try not to tidy the crime scene.",
                                    "Seraphine: He burned the account book badly and hid the page well. Panic with training.",
                                    "Seraphine: He knew too much and ran too late. A common Riverside tragedy."),
                            questStage("seraphine_chain_3_clerk", "Find The Hidden Clerk", "Hidden Clerk", 1, Quest.ObjectiveKind.TALK,
                                    "farmland", 1, "npc_citizen_man", null, "", "",
                                    "Seraphine: If he is alive, he is not brave. That may be the only reason he is alive.",
                                    "Seraphine: Find him. Do not promise safety unless you intend to become inconvenient about it.",
                                    "Seraphine: He named the red notary's seal and shook like a man signing his own grave.",
                                    "Seraphine: Fear can be testimony. Unpleasant, but admissible."),
                            questStage("seraphine_chain_3_choice", "Decide The Clerk's Protection", "Clerk Testimony", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "seraphine_clerk_truth",
                                    "Seraphine: Now decide whether we expose him, hide him, or make his testimony too public to kill.",
                                    "Seraphine: Pick the kind of mercy that survives paperwork.",
                                    "Seraphine: Chosen. I hope your conscience has comfortable shoes.",
                                    "Seraphine: You understand that freedom sometimes needs witnesses, not heroes."))),
            companionStagedQuest("seraphine_chain_4", "Knives in the Warehouse", "The false debt network keeps torn contract pages behind hired blades in a river warehouse.", 148, 118,
                    "seraphine", "seraphine_chain_5", List.of(
                            questStage("seraphine_chain_4_door", "Break The Warehouse Door", "Warehouse Guard", 3, Quest.ObjectiveKind.DEFEND,
                                    "goblin_camp", 1, null, "bandit_cutthroat", "", "",
                                    "Seraphine: Try not to bleed on the paperwork. Some of it is evidence. Some may be worth more than us.",
                                    "Seraphine: Break the warehouse guard. If they wanted gentle visitors, they should not have hired knives.",
                                    "Seraphine: The door is ours and several opinions about us are now horizontal.",
                                    "Seraphine: Good. Violence is vulgar, but sometimes it opens cabinets."),
                            questStage("seraphine_chain_4_pages", "Recover Torn Pages", "Torn Contract Pages", 3, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 1, "quest_document_bundle", null, "", "",
                                    "Seraphine: Keep the pages readable. Dead ink is easier to question than dead guards.",
                                    "Seraphine: Search every crate. Contracts love hiding beside honest cargo.",
                                    "Seraphine: There it is. My family name, tied up neatly like a pig for market.",
                                    "Seraphine: The page says Vale still owes. The page is about to be disappointed."),
                            questStage("seraphine_chain_4_key", "Take The Black Ledger Key", "Black Ledger Key", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 1, "icon_chest", null, "", "",
                                    "Seraphine: A black ledger key means the real book is elsewhere. Naturally.",
                                    "Seraphine: Find it before someone realizes we are literate and armed.",
                                    "Seraphine: Key found. It smells of brass, river rot, and expensive cowardice.",
                                    "Seraphine: Now we can open the part of the lie they paid to keep closed."))),
            companionStagedQuest("seraphine_chain_5", "Vale Was Never Free", "Seraphine returns to the ruined Vale counting room and learns her family's debt was never allowed to end.", 168, 132,
                    "seraphine", "seraphine_chain_6", List.of(
                            questStage("seraphine_chain_5_room", "Enter The Vale Counting Room", "Vale Counting Room", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_inspection_cache", null, "", "",
                                    "Seraphine: I usually avoid nostalgia. It has poor manners and knows where I sleep.",
                                    "Seraphine: Search the counting room. If the walls accuse us, let them finish.",
                                    "Seraphine: The account wall kept every payment except the ones that freed us.",
                                    "Seraphine: That is not bookkeeping. That is a cage with columns."),
                            questStage("seraphine_chain_5_contract", "Read The Protection Contract", "Old Protection Contract", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_document_bundle", null, "", "",
                                    "Seraphine: My father signed for one winter of safety. The ink learned immortality.",
                                    "Seraphine: Read it. I want the exact shape of the trap.",
                                    "Seraphine: The clause renews through fear. That is almost poetic, which makes me angrier.",
                                    "Seraphine: My father thought a signature could save us. He was right for one winter."),
                            questStage("seraphine_chain_5_signet", "Recover The Broken Signet", "Broken Vale Signet", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_ward_marker", null, "", "",
                                    "Seraphine: The family signet is here somewhere. A small metal apology.",
                                    "Seraphine: Find it. I want proof Vale belonged to itself before the baron renamed it debt.",
                                    "Seraphine: Broken, but ours. That feels annoyingly dramatic.",
                                    "Seraphine: Wrong for everything after, but ours before that."),
                            questStage("seraphine_chain_5_choice", "Choose What Vale Means", "Vale Family Debt", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "seraphine_family_debt",
                                    "Seraphine: Say it plainly. Was my father foolish, desperate, brave, or all three in a cheap coat?",
                                    "Seraphine: Choose carefully. I am listening for whether you mistake survival for consent.",
                                    "Seraphine: Chosen. I can hate the trap without hating everyone caught in it.",
                                    "Seraphine: That is a heavier freedom than revenge, unfortunately."))),
            companionStagedQuest("seraphine_chain_6", "The Dead Baron's Estate", "The dead baron's estate is still active on paper, guarded by criminals and administrators keeping old debt alive.", 188, 150,
                    "seraphine", "seraphine_chain_7", List.of(
                            questStage("seraphine_chain_6_estate", "Find The Legal Shell", "Dead Baron's Estate", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 2, "quest_inspection_cache", null, "", "",
                                    "Seraphine: The man has been dead twelve years and still owns more people than most living nobles.",
                                    "Seraphine: Find the estate records. Ghosts are less frightening than paperwork with guards.",
                                    "Seraphine: The estate has no master, only signatures pretending to be one.",
                                    "Seraphine: The estate is not haunted by a ghost. Worse. It is haunted by administrators."),
                            questStage("seraphine_chain_6_cutthroats", "Clear The Estate Knives", "Estate Cutthroats", 3, Quest.ObjectiveKind.DEFEND,
                                    "goblin_camp", 2, null, "bandit_captain", "", "",
                                    "Seraphine: His estate is a legal shell with knives in it. Crack both.",
                                    "Seraphine: Clear the guards. Leave at least one filing cabinet emotionally damaged.",
                                    "Seraphine: The knives are down. The paper looks nervous.",
                                    "Seraphine: I enjoy a law office that suddenly understands consequences."),
                            questStage("seraphine_chain_6_cabinet", "Open The Contract Cabinet", "Sealed Contract Cabinet", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 2, "icon_chest", null, "", "",
                                    "Seraphine: The cabinet is the estate's heart. Naturally, it has a lock and no shame.",
                                    "Seraphine: Open it. If the law grows another tooth, break that too.",
                                    "Seraphine: The cabinet names the Red Notary. A signature with a pulse behind it.",
                                    "Seraphine: So the dead baron was a mask. I hate when villains delegate."))),
            companionStagedQuest("seraphine_chain_7", "The Red Notary", "A demon-touched contract keeper binds Riverside names beyond death and turns obligation into chains.", 218, 176,
                    "seraphine", "seraphine_chain_8", List.of(
                            questStage("seraphine_chain_7_archive", "Enter The Hidden Archive", "Hidden Bridge Archive", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_document_bundle", null, "", "",
                                    "Seraphine: Every name owes something, he says. I am tired of being an invoice.",
                                    "Seraphine: Find the hidden archive. If the walls whisper interest rates, ignore them.",
                                    "Seraphine: The contract wall is alive enough to be rude.",
                                    "Seraphine: Good. We found the throat of the lie."),
                            questStage("seraphine_chain_7_notary", "Break The Red Notary", "The Red Notary", 1, Quest.ObjectiveKind.DEFEAT,
                                    "graveyard", 1, null, "void_knight", "", "",
                                    "Seraphine: There he is. Obligation in a robe, pretending chains are a civic service.",
                                    "Seraphine: Break him. Not because he owns my name. Because he does not.",
                                    "Seraphine: The red seal is ash. Freedom costs smoke, apparently.",
                                    "Seraphine: I thought freedom would smell less like demon smoke."),
                            questStage("seraphine_chain_7_lockbox", "Open The Blood-Sealed Lockbox", "Blood-Sealed Lockbox", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "icon_chest", null, "", "",
                                    "Seraphine: The lockbox survived. Of course it did. Cruelty keeps backups.",
                                    "Seraphine: Open it. I want the last clause where I can see it die.",
                                    "Seraphine: The last clause is simple: names can be inherited as debt. Monstrous little sentence.",
                                    "Seraphine: Now it is evidence, not destiny."),
                            questStage("seraphine_chain_7_choice", "Choose What Freedom Costs", "Red Notary Aftermath", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "seraphine_red_notary",
                                    "Seraphine: Choose what we do with his records: burn them, publish them, or keep them where victims can reclaim names.",
                                    "Seraphine: Freedom is not tidy. Choose the mess we can defend.",
                                    "Seraphine: Chosen. The dead baron can object in whatever pit keeps him.",
                                    "Seraphine: I am no longer an invoice. Try not to make a speech about it."))),
            companionStagedQuest("seraphine_chain_8", "No One Owns Vale", "At Oathstead, Seraphine decides that freedom can include promises she chooses for herself.", 240, 192,
                    "seraphine", null, List.of(
                            questStage("seraphine_chain_8_marker", "Walk The Oath Marker", "Oathstead Oath Marker", 1, Quest.ObjectiveKind.VISIT,
                                    "farmland", 2, "quest_ward_marker", null, "", "",
                                    "Seraphine: I burned the contract. Not the past, sadly. That is harder to set on fire.",
                                    "Seraphine: Walk the oath marker with me. I need to know this promise has no hidden clause.",
                                    "Seraphine: The oath marker is infuriatingly direct. No fine print. Suspicious.",
                                    "Seraphine: Oathstead still feels like a choice. That may be its worst habit."),
                            questStage("seraphine_chain_8_ashes", "Inspect The Burned Contract", "Burned Contract Remains", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_document_bundle", null, "", "",
                                    "Seraphine: Ash is not erasure. It is evidence that something finally stopped pretending.",
                                    "Seraphine: Inspect the remains. I want to see whether the past has any teeth left.",
                                    "Seraphine: The old contract is dead enough to be honest.",
                                    "Seraphine: No one owns Vale. I may need to hear that more than once."),
                            questStage("seraphine_chain_8_choice", "Choose The Promise", "Seraphine's Chosen Promise", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "seraphine_oathstead_promise",
                                    "Seraphine: I can leave, stay, help, or call all of this temporary until it becomes embarrassing.",
                                    "Seraphine: Help me choose the promise that is mine, not another polite chain.",
                                    "Seraphine: Chosen. Annoyingly, that feels different from being bound.",
                                    "Seraphine: No one owns Vale. But I can choose to stand somewhere. Annoyingly, that seems to be here."))),
            companionStagedQuest("maera_chain_1", "A Footnote With Teeth", "Maera proves that official Crown history edited an older road out of the record.", 76, 62,
                    "maera", "maera_chain_2", List.of(
                            questStage("maera_chain_1_map", "Inspect The Censored Star Map", "Censored Star Map", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 0, "quest_document_bundle", null, "", "",
                                    "Maera: If you came to tell me the archives are always right, please stand somewhere flammable.",
                                    "Maera: Start with the star map. The censorship is not subtle, which is almost insulting.",
                                    "Maera: The missing constellation was scraped out, not damaged.",
                                    "Maera: Good. The map has teeth under the polite ink."),
                            questStage("maera_chain_1_record", "Compare The Royal Route Record", "Royal Route Record", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 0, "quest_document_bundle", null, "", "",
                                    "Maera: Now compare the official record. One of these documents is lying less than the other.",
                                    "Maera: Bring me the contradiction before the Archive improves it into obedience.",
                                    "Maera: The road existed before the Crown claims it was built.",
                                    "Maera: History has been wearing borrowed boots."),
                            questStage("maera_chain_1_miri", "Ask The Apprentice", "Archive Apprentice", 1, Quest.ObjectiveKind.TALK,
                                    "graveyard", 0, "npc_citizen_woman", null, "", "",
                                    "Maera: Apprentice Miri reshelves forbidden things badly, which makes her my favorite kind of witness.",
                                    "Maera: Ask what she moved. Kindly. Terrified apprentices misfile truth on instinct.",
                                    "Maera: She saw the missing constellation in an uncatalogued folio.",
                                    "Maera: Excellent. A witness, a contradiction, and a future disciplinary hearing."),
                            questStage("maera_chain_1_choice", "Choose The First Citation", "Forbidden Footnote", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "maera_first_truth",
                                    "Maera: Choose how we handle the first proof: quietly preserve it, cite it publicly, or bait the Archive into denying it.",
                                    "Maera: Truth needs strategy unless you enjoy funerals with bibliography.",
                                    "Maera: Chosen. I will pretend this was a measured scholarly decision.",
                                    "Maera: Good. The footnote has teeth now."))),
            companionStagedQuest("maera_chain_2", "Ink Under Moonlight", "Maera gathers politically disloyal materials to restore a forbidden star-route chart.", 104, 82,
                    "maera", "maera_chain_3", List.of(
                            questStage("maera_chain_2_ink", "Gather Moonwell Ink", "Moonwell Ink", 2, Quest.ObjectiveKind.GATHER,
                                    "farmland", 0, "quest_document_bundle", null, "", "",
                                    "Maera: Official ink refuses to show unofficial roads. Fortunately, the moon has fewer political loyalties.",
                                    "Maera: Gather moonwell ink. Yes, scholarship has shopping lists.",
                                    "Maera: The ink glows when the map name is spoken. Rude, but useful.",
                                    "Maera: Good. This ink remembers light properly."),
                            questStage("maera_chain_2_wings", "Gather Star Moth Wings", "Star Moth Wings", 2, Quest.ObjectiveKind.GATHER,
                                    "farmland", 1, "quest_salve_herbs", null, "", "",
                                    "Maera: Star moth wings hold reflected routes better than vellum. Do not ask why; it ruins the mystique.",
                                    "Maera: Take only shed wings. I need evidence, not tiny corpses.",
                                    "Maera: These will hold the correction without smearing.",
                                    "Maera: The moths are better archivists than several registrars I could name."),
                            questStage("maera_chain_2_parchment", "Find Clean Parchment", "Clean Parchment", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_document_bundle", null, "", "",
                                    "Maera: Clean parchment is required because forbidden truth deserves better than napkins.",
                                    "Maera: Find parchment that has not been stamped, taxed, blessed, or corrected into nonsense.",
                                    "Maera: Good. No royal watermark. How indecently honest.",
                                    "Maera: Now the chart can breathe without asking permission."))),
            companionStagedQuest("maera_chain_3", "The Map That Moved", "An old observatory chart shifts under moonlight and points toward a road the Crown erased.", 126, 100,
                    "maera", "maera_chain_4", List.of(
                            questStage("maera_chain_3_device", "Inspect The Star Device", "Rotating Star Device", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_inspection_cache", null, "", "",
                                    "Maera: Maps do not move unless they are wrong, enchanted, or trying to apologize.",
                                    "Maera: Inspect the device. Let moonlight do the rude part.",
                                    "Maera: The device turns toward a road no current chart admits.",
                                    "Maera: That is not apology. That is testimony."),
                            questStage("maera_chain_3_lens", "Recover The Cracked Lens", "Old Observatory Lens", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_ward_marker", null, "", "",
                                    "Maera: The lens cracked along an old route line. Convenient damage is rarely damage.",
                                    "Maera: Bring it carefully. Broken glass can still focus history.",
                                    "Maera: The crack completes the missing constellation.",
                                    "Maera: The stars remember the road. The Crownlands decided not to."),
                            questStage("maera_chain_3_chart", "Open The Sealed Northern Chart", "Sealed Northern Chart", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "icon_chest", null, "", "",
                                    "Maera: The northern chart is sealed. Naturally. Truth loves a dramatic latch.",
                                    "Maera: Open it and do not let the seal convince you obedience is scholarship.",
                                    "Maera: The chart points toward the Old Gate by omission and shadow.",
                                    "Maera: We have a road, a direction, and a growing list of people who lied."),
                            questStage("maera_chain_3_choice", "Choose The Interpretation", "Moving Map Interpretation", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "maera_moving_map",
                                    "Maera: Choose our working theory: royal censorship, demon pressure, old protection, or all three being dreadful together.",
                                    "Maera: A theory is a knife. Pick one sharp enough and do not wave it at friends.",
                                    "Maera: Chosen. I reserve the right to add angry marginalia.",
                                    "Maera: Good. Now we have a hypothesis with a pulse."))),
            companionStagedQuest("maera_chain_4", "The Archivist's Warning", "Maera confronts the disciplinary record that ended her research and learns who chose obedience over evidence.", 146, 116,
                    "maera", "maera_chain_5", List.of(
                            questStage("maera_chain_4_notice", "Read The Disciplinary Notice", "Disciplinary Notice", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 0, "quest_document_bundle", null, "", "",
                                    "Maera: If Registrar Pell asks, we are not investigating forbidden records. We are admiring dust.",
                                    "Maera: Find the notice. The Archive hides knives in polite paper.",
                                    "Maera: They called the question irresponsible before they answered it.",
                                    "Maera: Cowards often discover procedure at convenient moments."),
                            questStage("maera_chain_4_seal", "Recover The Revoked Seal", "Revoked Research Seal", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 0, "quest_ward_marker", null, "", "",
                                    "Maera: My revoked seal is probably filed under 'examples for discouraging ambition.'",
                                    "Maera: Recover it. I want the insult where I can annotate it.",
                                    "Maera: The seal was revoked after the evidence arrived, not before.",
                                    "Maera: That is what cowards call a question with evidence."),
                            questStage("maera_chain_4_registrar", "Question The Registrar", "Archive Registrar", 1, Quest.ObjectiveKind.TALK,
                                    "graveyard", 0, "npc_ren", null, "", "",
                                    "Maera: Someone signed the warning. Let us ask whether their spine survived the appointment.",
                                    "Maera: Ask plainly who ordered the correction buried.",
                                    "Maera: The registrar blames royal pressure and calls it stewardship. Predictable. Useful.",
                                    "Maera: Stewardship is a lovely word for locking truth in a cupboard."),
                            questStage("maera_chain_4_choice", "Choose How To Defy The Warning", "Archivist's Warning", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "maera_archive_warning",
                                    "Maera: Choose our reply: obey the ban publicly, ignore it quietly, or make the ban part of the evidence.",
                                    "Maera: Defiance is more useful when filed properly.",
                                    "Maera: Chosen. Curiosity remains contagious.",
                                    "Maera: Excellent. I do love responsible treason."))),
            companionStagedQuest("maera_chain_5", "Cultists in the Stacks", "Cultists steal star-route pages from the restricted archive, proving Maera's map matters beyond scholarship.", 168, 132,
                    "maera", "maera_chain_6", List.of(
                            questStage("maera_chain_5_stacks", "Defend The Restricted Stacks", "Archive Cultists", 3, Quest.ObjectiveKind.DEFEND,
                                    "goblin_camp", 0, null, "wraith", "", "",
                                    "Maera: Wonderful. I was worried this was only academically dangerous.",
                                    "Maera: Clear the stacks and keep the stolen pages out of cult hands.",
                                    "Maera: The stacks are quiet again, which in an archive is either peace or ambush recovery.",
                                    "Maera: If cultists want the same map, either I am right or about to become a cautionary lecture."),
                            questStage("maera_chain_5_pages", "Recover Stolen Star Pages", "Stolen Star Pages", 3, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 0, "quest_document_bundle", null, "", "",
                                    "Maera: Find the pages. The cultists may have marked what frightened them.",
                                    "Maera: Keep them dry, unburned, and out of devotional chanting range.",
                                    "Maera: The margins mention the Gate. In a dead language, because of course they do.",
                                    "Maera: Academic danger has become practical danger. How efficient."),
                            questStage("maera_chain_5_seal", "Inspect The Broken Moon Seal", "Broken Moon Seal", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 0, "quest_ward_marker", null, "", "",
                                    "Maera: The seal broke inward. That means something entered through permission.",
                                    "Maera: Inspect it before anyone sweeps up the implication.",
                                    "Maera: The seal was opened by a registrar key and closed by ash.",
                                    "Maera: Treason with office access. My least favorite genre."),
                            questStage("maera_chain_5_choice", "Choose What The Cult Proof Means", "Cultist Star Pages", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "maera_cult_pages",
                                    "Maera: Choose what we do with proof cultists want the map: warn the Archive, hide the route, or bait the thieves.",
                                    "Maera: Scholarship has acquired teeth and a murder schedule.",
                                    "Maera: Chosen. I will update my risk assessment from 'annoying' to 'bloodstained.'",
                                    "Maera: Good. We are still choosing, which is better than merely reacting."))),
            companionStagedQuest("maera_chain_6", "The Quill Family Correction", "Maera learns her mother found the same erased route and hid the correction for a stubborn daughter.", 188, 150,
                    "maera", "maera_chain_7", List.of(
                            questStage("maera_chain_6_study", "Search The Quill Study", "Quill Family Study", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_inspection_cache", null, "", "",
                                    "Maera: My mother was not wrong. I need you to understand that before I do something extremely illegal.",
                                    "Maera: Search the study. Family shame has sharp corners.",
                                    "Maera: The room was searched before us, badly and officially.",
                                    "Maera: Someone wanted her work gone but not understood."),
                            questStage("maera_chain_6_notes", "Read Her Mother's Notes", "Mother's Route Notes", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_document_bundle", null, "", "",
                                    "Maera: My mother wrote like every sentence might be cross-examined by cowards.",
                                    "Maera: Read the notes. Carefully. She trusted precision more than safety.",
                                    "Maera: She found the same missing route and named the same contradiction.",
                                    "Maera: She knew. She knew and kept writing."),
                            questStage("maera_chain_6_witness", "Ask The Old Study Keeper", "Old Study Keeper", 1, Quest.ObjectiveKind.TALK,
                                    "graveyard", 2, "npc_citizen_woman", null, "", "",
                                    "Maera: Someone kept this room from being emptied completely. I want to thank or interrogate them.",
                                    "Maera: Ask what my mother hid and why they let it survive.",
                                    "Maera: She left the correction where only a stubborn daughter would look.",
                                    "Maera: I feel observed by a ghost with very high standards."),
                            questStage("maera_chain_6_choice", "Choose The Family Truth", "Quill Family Correction", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "maera_family_correction",
                                    "Maera: Choose how we carry her work: as vindication, inheritance, warning, or unfinished collaboration.",
                                    "Maera: Do not make her a martyr if she was trying to remain a scholar.",
                                    "Maera: Chosen. That is kinder than a statue and more useful than a sob.",
                                    "Maera: Good. We carry her correction forward, not her silence."))),
            companionStagedQuest("maera_chain_7", "The Forbidden Map Vault", "The erased road and five-kingdom chart wait under a vault guarded by corrupted censorship magic.", 218, 176,
                    "maera", "maera_chain_8", List.of(
                            questStage("maera_chain_7_vault", "Enter The Forbidden Map Vault", "Forbidden Map Vault", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_ward_marker", null, "", "",
                                    "Maera: The vault is sealed against unauthorized truth. I feel personally invited.",
                                    "Maera: Find the entry marks. Vaults enjoy pretending locks are moral arguments.",
                                    "Maera: The seal recognizes royal authority and fears older roads.",
                                    "Maera: Good. The door is already making admissions."),
                            questStage("maera_chain_7_warden", "Break The Inkbound Warden", "Inkbound Warden", 1, Quest.ObjectiveKind.DEFEAT,
                                    "graveyard", 1, null, "elder_wraith", "", "",
                                    "Maera: Unauthorized truth detected, I imagine it will say. Try not to look too authorized.",
                                    "Maera: Break the warden. I refuse to be murdered by a filing policy.",
                                    "Maera: The warden is down. The silence feels unemployed.",
                                    "Maera: I will footnote the fight later."),
                            questStage("maera_chain_7_chart", "Read The Five-Kingdom Chart", "Five-Kingdom Star Chart", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_document_bundle", null, "", "",
                                    "Maera: Now read the chart before someone invents a committee.",
                                    "Maera: Find the erased road and the Gate's old notation.",
                                    "Maera: The road is real. The Gate is real.",
                                    "Maera: Every official map has been lying by omission."),
                            questStage("maera_chain_7_choice", "Choose How Truth Leaves The Vault", "Forbidden Map Truth", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "maera_vault_truth",
                                    "Maera: Choose how this truth leaves: copied, memorized, smuggled, or declared loudly enough to be dangerous.",
                                    "Maera: Truth leaving a vault is a birth and a burglary.",
                                    "Maera: Chosen. I can hear several future officials developing headaches.",
                                    "Maera: Good. The truth is no longer alone in the dark."))),
            companionStagedQuest("maera_chain_8", "Properly Footnoted Treason", "Maera brings the completed star-route map to Oathstead and chooses where dangerous truth belongs.", 240, 192,
                    "maera", null, List.of(
                            questStage("maera_chain_8_table", "Inspect Oathstead's Map Table", "Oathstead Map Table", 1, Quest.ObjectiveKind.VISIT,
                                    "farmland", 2, "quest_document_bundle", null, "", "",
                                    "Maera: I have committed treason in three citation styles.",
                                    "Maera: Inspect Oathstead's map table with me. The truth needs a place to stand.",
                                    "Maera: The table is uneven, underfunded, and honest. A promising archive.",
                                    "Maera: Oathstead may be the first place that needs this map more than it fears it."),
                            questStage("maera_chain_8_route", "Complete The Star-Route Map", "Completed Star-Route Map", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_document_bundle", null, "", "",
                                    "Maera: Place the final route. If the ink screams, take notes.",
                                    "Maera: Finish it cleanly. Dangerous truth still deserves good handwriting.",
                                    "Maera: The route and the oath agree. That is either hope or excellent dramatic timing.",
                                    "Maera: The map is complete enough to frighten useful people."),
                            questStage("maera_chain_8_choice", "Choose The Archive", "Maera's Living Archive", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "maera_oathstead_archive",
                                    "Maera: Choose what I build here: public archive, hidden copy, field map, or a scandal with shelves.",
                                    "Maera: I want the truth used, not worshipped. Help me be less insufferable about that.",
                                    "Maera: Chosen. I will become only moderately unbearable.",
                                    "Maera: If Alderfall survives, I am rewriting the maps. If it does not, I am haunting the footnotes."))),
            companionStagedQuest("cassia_chain_1", "The Woman at the Gate", "Cassia lets the player inspect Highwall's gate before she says which memories still cut.", 104, 80,
                    "cassia", "cassia_chain_2", List.of(
                            questStage("cassia_chain_1_shield", "Inspect The Dented Shield", "Highwall Gate Shield", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_ward_marker", null, "", "",
                                    "Cassia: Say what you came to say. Gates do not open wider because people mumble at them.",
                                    "Cassia: Start with the shield. Highwall keeps accusations polished and dents honest.",
                                    "Cassia: The dent came from the outside. A civilian tool, not a raider axe.",
                                    "Cassia: This shield remembers hands that were not enemies."),
                            questStage("cassia_chain_1_winch", "Test The Old Gate Winch", "Old Gate Winch", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_inspection_cache", null, "", "",
                                    "Cassia: The winch is older than half the officers who pretend to understand it.",
                                    "Cassia: Test it. Slowly. Machines reveal cowardice better than speeches.",
                                    "Cassia: The release catches. Someone repaired delay into the mechanism.",
                                    "Cassia: Good. The gate did not simply close. It was made slow to open."),
                            questStage("cassia_chain_1_plaque", "Read The Memorial Plaque", "Highwall Memorial Plaque", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_document_bundle", null, "", "",
                                    "Cassia: Read the plaque. Highwall likes grief when it is carved into obedient stone.",
                                    "Cassia: Count which names are missing. Missing names say more than praised ones.",
                                    "Cassia: The plaque honors soldiers and forgets the families outside the gate.",
                                    "Cassia: This gate remembers every hand that struck it from the wrong side."),
                            questStage("cassia_chain_1_choice", "Name What The Gate Kept", "Highwall Gate Memory", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "cassia_gate_memory",
                                    "Cassia: Choose what we call this place: duty, guilt, cowardice, or an old wound still giving orders.",
                                    "Cassia: Say it plainly. Comfort makes a poor witness.",
                                    "Cassia: Chosen. I will not thank you for honesty. I will remember it.",
                                    "Cassia: Good. The gate has spoken enough for one day."))),
            companionStagedQuest("cassia_chain_2", "Frost Road Discipline", "Cassia tests whether the player can hold a dangerous road without turning fear into noise.", 126, 100,
                    "cassia", "cassia_chain_3", List.of(
                            questStage("cassia_chain_2_wolves", "Break The Wolf Pack", "Frost Wolf", 3, Quest.ObjectiveKind.DEFEAT,
                                    "graveyard", 0, null, "frost_wolf", "", "",
                                    "Cassia: You want my time? Earn it on the road. Roads tell the truth about people.",
                                    "Cassia: Clear the wolves first. Panic runs faster when it has teeth behind it.",
                                    "Cassia: The pack scattered. You kept your line.",
                                    "Cassia: Breath steady. Weapon steady. Better than a speech."),
                            questStage("cassia_chain_2_raiders", "Push Back The Raiders", "Orc Raider", 2, Quest.ObjectiveKind.DEFEAT,
                                    "goblin_camp", 2, null, "orc_raider", "", "",
                                    "Cassia: Wolves follow hunger. Raiders follow permission. Break both.",
                                    "Cassia: Push them off the road before they teach travelers to beg.",
                                    "Cassia: The road is open, if not peaceful.",
                                    "Cassia: Good. You did not confuse cruelty with strength."),
                            questStage("cassia_chain_2_marker", "Inspect The Road Marker", "Frost Road Marker", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 0, "quest_roadwatch_warning_marks", null, "", "",
                                    "Cassia: A held road leaves marks. Look for the marker before pride edits the lesson.",
                                    "Cassia: Inspect it and tell me where the patrol failed.",
                                    "Cassia: The marker was turned away from the pass. Someone wanted delay.",
                                    "Cassia: Roads tell the truth about people. This one is talking."),
                            questStage("cassia_chain_2_choice", "Choose The Road Lesson", "Frost Road Lesson", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "cassia_frost_road",
                                    "Cassia: Choose what matters most here: discipline, mercy, speed, or asking why the warning was turned.",
                                    "Cassia: A soldier who learns only one lesson becomes a tool.",
                                    "Cassia: Chosen. You came back with weapon, breath, and thought. Good.",
                                    "Cassia: I will hear what you have to say next."))),
            companionStagedQuest("cassia_chain_3", "The Survivor's Complaint", "Cassia asks the player to bring back survivor evidence without softening what she did.", 146, 116,
                    "cassia", "cassia_chain_4", List.of(
                            questStage("cassia_chain_3_house", "Visit The Survivor's House", "Survivor's House", 1, Quest.ObjectiveKind.VISIT,
                                    "farmland", 1, "quest_inspection_cache", null, "", "",
                                    "Cassia: There are stories about me in Ironvale. Some are true. The worst ones are incomplete.",
                                    "Cassia: Go to the house. Do not apologize on my behalf. That is theft with manners.",
                                    "Cassia: The room still holds the night like smoke.",
                                    "Cassia: Good. Now the evidence has a floor under it."),
                            questStage("cassia_chain_3_token", "Inspect The Burned Token", "Burned Family Token", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_document_bundle", null, "", "",
                                    "Cassia: The token belonged to a child outside the gate. I remember the sound more than the face.",
                                    "Cassia: Inspect it. Do not make it symbolic before it has been real.",
                                    "Cassia: Half the name survived the burn.",
                                    "Cassia: A half-name is still a person."),
                            questStage("cassia_chain_3_letter", "Read The Complaint Letter", "Survivor's Complaint", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_document_bundle", null, "", "",
                                    "Cassia: Read the complaint. Let anger write in its own hand.",
                                    "Cassia: Bring the truth back with its teeth still in.",
                                    "Cassia: The letter says I followed orders and still chose the hand on the lever.",
                                    "Cassia: That is correct. Ugly, but correct."),
                            questStage("cassia_chain_3_choice", "Choose What The Survivor Deserved", "Survivor's Truth", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "cassia_survivor_truth",
                                    "Cassia: Choose what we owe the survivor: apology, record, restitution, or a promise that this gate will not close again.",
                                    "Cassia: Do not choose the gentle answer because it is easier to carry.",
                                    "Cassia: Chosen. I closed the gate. That part is true.",
                                    "Cassia: Truth does not get lighter because you add context."))),
            companionStagedQuest("cassia_chain_4", "Orders in Blue Steel", "Cassia searches the abandoned watch office for the order that turned duty into a shield for cowardice.", 168, 132,
                    "cassia", "cassia_chain_5", List.of(
                            questStage("cassia_chain_4_orders", "Recover Torn Command Orders", "Torn Command Orders", 3, Quest.ObjectiveKind.GATHER,
                                    "graveyard", 2, "quest_document_bundle", null, "", "",
                                    "Cassia: I do not need comfort. I need the correct facts.",
                                    "Cassia: Recover the torn orders. Paper lies less when torn.",
                                    "Cassia: The signatures match. The coward did not even change his hand.",
                                    "Cassia: So. He signed it, then buried it."),
                            questStage("cassia_chain_4_desk", "Inspect The Command Desk", "Old Command Desk", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_inspection_cache", null, "", "",
                                    "Cassia: A command desk remembers elbows, fear, and spilled ink.",
                                    "Cassia: Check the marks. Someone forced a choice and later sanded the edge.",
                                    "Cassia: The drawer was pried open after the raid.",
                                    "Cassia: He hid the order after the dead could not contradict him."),
                            questStage("cassia_chain_4_horn", "Inspect The Rusted Signal Horn", "Rusted Signal Horn", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_ward_marker", null, "", "",
                                    "Cassia: The signal horn should have called the gate open.",
                                    "Cassia: Inspect it. Silence can be manufactured.",
                                    "Cassia: The mouthpiece was packed with wax.",
                                    "Cassia: Cowardice with a seal, and silence with a plug."),
                            questStage("cassia_chain_4_choice", "Choose What Orders Mean", "Blue Steel Orders", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "cassia_blue_steel_orders",
                                    "Cassia: Choose the lesson: orders bind, orders accuse, orders require judgment, or orders are where cowards hide.",
                                    "Cassia: Say it cleanly. I have obeyed enough fog.",
                                    "Cassia: Chosen. That answer is heavier than comfort.",
                                    "Cassia: Good. Weight is honest."))),
            companionStagedQuest("cassia_chain_5", "The Captain Who Lied", "Cassia traces the buried order to Captain Varran and decides whether truth can be dirty and still necessary.", 188, 150,
                    "cassia", "cassia_chain_6", List.of(
                            questStage("cassia_chain_5_varran", "Question Retired Captain Varran", "Retired Captain Varran", 1, Quest.ObjectiveKind.TALK,
                                    "graveyard", 2, "npc_torin", null, "", "",
                                    "Cassia: If Varran lied, I want to know. If he did not, I want to know that too.",
                                    "Cassia: Ask him plainly. Command trained him to survive questions, not answer them.",
                                    "Cassia: He remembers enough to avoid every useful word.",
                                    "Cassia: His silence has rank on it."),
                            questStage("cassia_chain_5_chest", "Search Varran's Campaign Chest", "Campaign Chest", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "icon_chest", null, "", "",
                                    "Cassia: Search the chest. Men like him lock away proof and call it discipline.",
                                    "Cassia: Bring what he thought command could hide.",
                                    "Cassia: The chest holds a clean copy of the order.",
                                    "Cassia: He kept the truth close enough to fear it."),
                            questStage("cassia_chain_5_copy", "Read The Hidden Order Copy", "Hidden Order Copy", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_document_bundle", null, "", "",
                                    "Cassia: Read it. I want no mercy from uncertainty.",
                                    "Cassia: Find the line that names the gate and the civilians.",
                                    "Cassia: The order sacrificed them and made me its hand.",
                                    "Cassia: I wanted the truth clean. It is not."),
                            questStage("cassia_chain_5_choice", "Choose How Varran Is Answered", "Varran's Lie", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "cassia_varran_truth",
                                    "Cassia: Choose the answer: expose him, force confession, protect the record first, or make him face the survivors.",
                                    "Cassia: Revenge is loud. Justice should be able to stand quietly.",
                                    "Cassia: Chosen. That does not excuse me from carrying my part.",
                                    "Cassia: But I will not carry his lie for him."))),
            companionStagedQuest("cassia_chain_6", "Raiders at Old Flint Gate", "A new attack forces Cassia to relive the gate decision and choose people over obedient fear.", 208, 166,
                    "cassia", "cassia_chain_7", List.of(
                            questStage("cassia_chain_6_raiders", "Break The Raiders", "Orc Raider", 4, Quest.ObjectiveKind.DEFEND,
                                    "goblin_camp", 2, null, "orc_raider", "", "",
                                    "Cassia: Old gate. New blood. I am tired of history having no imagination.",
                                    "Cassia: Break the raiders and keep the road breathing.",
                                    "Cassia: The raiders are down. The gate still has to answer.",
                                    "Cassia: Good. This time the people behind us are not abandoned."),
                            questStage("cassia_chain_6_brutes", "Hold Against The Brutes", "Orc Brute", 2, Quest.ObjectiveKind.DEFEAT,
                                    "goblin_camp", 2, null, "orc", "", "",
                                    "Cassia: Brutes test hinges by throwing bodies at them. Hold.",
                                    "Cassia: Do not chase glory away from the gate.",
                                    "Cassia: The line held because no one ran to look heroic.",
                                    "Cassia: Discipline can protect instead of excuse. Remember that."),
                            questStage("cassia_chain_6_chain", "Inspect The Broken Chain", "Broken Gate Chain", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 2, "quest_ward_marker", null, "", "",
                                    "Cassia: The chain decides whether a gate is wall or doorway.",
                                    "Cassia: Inspect it. I want the gate able to open before anyone praises it for closing.",
                                    "Cassia: The break can be repaired from this side.",
                                    "Cassia: The gate held. More importantly, it opened when it needed to."),
                            questStage("cassia_chain_6_choice", "Choose The New Gate Order", "Old Flint Gate Order", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "cassia_old_gate",
                                    "Cassia: Choose the standing order: hold until all cross, open under fire, split the shield line, or trust judgment over command.",
                                    "Cassia: Put the words where frightened hands can find them.",
                                    "Cassia: Chosen. That is an order I could have lived with.",
                                    "Cassia: It is one I can live by now."))),
            companionStagedQuest("cassia_chain_7", "The Ironwall Trial", "Cassia breaks the champion guarding Varran's public lie and decides what truth should demand.", 232, 184,
                    "cassia", "cassia_chain_8", List.of(
                            questStage("cassia_chain_7_yard", "Enter The Trial Yard", "Highwall Trial Yard", 1, Quest.ObjectiveKind.VISIT,
                                    "graveyard", 2, "quest_inspection_cache", null, "", "",
                                    "Cassia: Highwall survives by obedience, he says. I say he hides behind it.",
                                    "Cassia: Enter the yard. Let them see we came with facts, not a tantrum.",
                                    "Cassia: The yard is waiting for spectacle. Deny it comfort.",
                                    "Cassia: Good. Truth can stand without shouting first."),
                            questStage("cassia_chain_7_rack", "Inspect The Shield Rack", "Trial Shield Rack", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_ward_marker", null, "", "",
                                    "Cassia: The shield rack is arranged like a sermon about obedience.",
                                    "Cassia: Inspect it. Hypocrisy usually leaves fingerprints where it polishes.",
                                    "Cassia: Varran's crest is fresh over older scorch marks.",
                                    "Cassia: He painted command over fear and called it history."),
                            questStage("cassia_chain_7_champion", "Defeat Varran's Champion", "Orc Champion", 1, Quest.ObjectiveKind.DEFEAT,
                                    "goblin_camp", 2, null, "orc_champion", "", "",
                                    "Cassia: His champion says truth weakens walls. Break the argument.",
                                    "Cassia: Keep your feet. Let him spend his strength protecting a lie.",
                                    "Cassia: The champion is down. The lie has lost its armor.",
                                    "Cassia: Lies weaken walls. Truth just shows where the cracks already were."),
                            questStage("cassia_chain_7_choice", "Choose The Public Record", "Ironwall Trial Record", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "cassia_ironwall_trial",
                                    "Cassia: Choose what the record says: Varran lied, Cassia obeyed and failed, Highwall abandoned civilians, or all of it together.",
                                    "Cassia: Do not carve an easier truth because stone is expensive.",
                                    "Cassia: Chosen. The record will bruise people who deserve bruising.",
                                    "Cassia: Good. Let the wall learn to hold weight honestly."))),
            companionStagedQuest("cassia_chain_8", "Gate Open, Shield Raised", "Cassia inspects Oathstead's gate and chooses duty as judgment rather than obedience.", 252, 202,
                    "cassia", null, List.of(
                            questStage("cassia_chain_8_gate", "Inspect Oathstead's Gate", "Oathstead Gate", 1, Quest.ObjectiveKind.VISIT,
                                    "farmland", 2, "quest_ward_marker", null, "", "",
                                    "Cassia: Oathstead's gate is badly hung. I like it anyway.",
                                    "Cassia: Inspect it with me. This place needs hinges before speeches.",
                                    "Cassia: The gate opens faster than it closes. Sensible.",
                                    "Cassia: Oathstead may be badly built enough to become honest."),
                            questStage("cassia_chain_8_shield", "Inspect The Repaired Shield", "Cassia's Repaired Shield", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_inspection_cache", null, "", "",
                                    "Cassia: My shield has new straps. Old dents. Both matter.",
                                    "Cassia: Inspect it. I want no symbol that forgets what it survived.",
                                    "Cassia: The repair holds without hiding the old wound.",
                                    "Cassia: Good. That is the kind of strength I trust now."),
                            questStage("cassia_chain_8_choice", "Choose The Duty", "Cassia's Oathstead Duty", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "cassia_oathstead_duty",
                                    "Cassia: Choose what I become here: gate captain, shield trainer, witness for the dead, or guard who questions orders.",
                                    "Cassia: Do not flatter me. Give me work that can look me in the eye.",
                                    "Cassia: Chosen. I will stand here.",
                                    "Cassia: Not because I was ordered. Because I looked, judged, and chose."))),
            companionStagedQuest("lyra_chain_1", "The Roadside Clinic", "Lyra turns the player's first offer of help into practical field care before anyone is allowed to make speeches.", 90, 70,
                    "lyra", "lyra_chain_2", List.of(
                            questStage("lyra_chain_1_bandages", "Gather Clean Bandages", "Clean Bandages", 2, Quest.ObjectiveKind.GATHER,
                                    "farmland", 1, "quest_supply_cache", null, "", "",
                                    "Lyra: If you are here to help, wash your hands. If you are here to stare, stand where patients cannot see you.",
                                    "Lyra: Bandages first. Sympathy bleeds through fingers faster than linen does.",
                                    "Lyra: These will hold a wound closed long enough for hope to become useful.",
                                    "Lyra: Good. Mercy travels faster when it has bandages."),
                            questStage("lyra_chain_1_reed", "Gather Fever Reed", "Fever Reed", 2, Quest.ObjectiveKind.GATHER,
                                    "farmland", 1, "quest_salve_herbs", null, "", "",
                                    "Lyra: Fever reed next. Pick the green stems, not the dramatic ones. Medicine dislikes theater.",
                                    "Lyra: Gather enough to cool travelers before the fever starts making decisions.",
                                    "Lyra: The reed is fresh. It smells like mud and second chances.",
                                    "Lyra: Good. Fever will have to argue with us now."),
                            questStage("lyra_chain_1_water", "Fill Water Skins", "Clean Water Skins", 2, Quest.ObjectiveKind.GATHER,
                                    "farmland", 1, "quest_water_skins", null, "", "",
                                    "Lyra: Water before courage. Courage is useless if the patient dies thirsty.",
                                    "Lyra: Fill clean skins. Clean matters. I am fond of patients surviving my help.",
                                    "Lyra: The water is clear enough to trust and heavy enough to matter.",
                                    "Lyra: Now the clinic can breathe."),
                            questStage("lyra_chain_1_choice", "Choose The First Triage", "Roadside Triage Rule", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "lyra_first_triage",
                                    "Lyra: Choose what we prioritize when too many hands reach at once: fever, bleeding, fear, or the quiet patient no one sees.",
                                    "Lyra: There is no clean answer. There is only the answer you can keep working after.",
                                    "Lyra: Chosen. I will hold you to it when the room gets loud.",
                                    "Lyra: Good. Now you are helping, not hovering."))),
            companionStagedQuest("lyra_chain_2", "Names on the Cot", "Lyra asks the player to count missing supplies and remembered patients before investigating who harmed the clinic.", 112, 88,
                    "lyra", "lyra_chain_3", List.of(
                            questStage("lyra_chain_2_ledger", "Read The Patient Ledger", "Patient Ledger", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_document_bundle", null, "", "",
                                    "Lyra: I write names because pain makes people disappear if you let it.",
                                    "Lyra: Read the ledger. Count people, not just symptoms.",
                                    "Lyra: The names are careful. The last entries are not in Lyra's hand.",
                                    "Lyra: Someone used my records after I closed the tent."),
                            questStage("lyra_chain_2_shelf", "Inspect The Empty Shelf", "Empty Medicine Shelf", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_supply_cache", null, "", "",
                                    "Lyra: The shelf is empty in the wrong pattern. Theft has a shape.",
                                    "Lyra: Count what is missing, not just what is gone.",
                                    "Lyra: Fever tonic, clot thread, clean needle oil. Someone chose harm with a list.",
                                    "Lyra: That is not hunger. That is planning."),
                            questStage("lyra_chain_2_cloak", "Inspect The Bloodied Cloak", "Bloodied Travel Cloak", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_inspection_cache", null, "", "",
                                    "Lyra: The cloak was left where patients could see it. Cruelty likes an audience.",
                                    "Lyra: Inspect it. Patient blood tells time if you listen properly.",
                                    "Lyra: The stains are old, then fresh, then dragged through marsh mud.",
                                    "Lyra: Whoever did this wanted fear walking ahead of sickness."),
                            questStage("lyra_chain_2_choice", "Choose How Names Are Kept", "Remembered Patients", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "lyra_names_on_cot",
                                    "Lyra: Choose what these names need from us: witness, warning, justice, or care for the living first.",
                                    "Lyra: Do not make grief decorative. It has work to do.",
                                    "Lyra: Chosen. They were not forgotten.",
                                    "Lyra: That matters, even when it is not enough."))),
            companionStagedQuest("lyra_chain_3", "Medicine That Lied", "Lyra follows altered records and a marked bell winch to prove someone dressed harm as treatment.", 134, 106,
                    "lyra", "lyra_chain_4", List.of(
                            questStage("lyra_chain_3_records", "Inspect Altered Clinic Records", "Altered Clinic Records", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_document_bundle", null, "", "",
                                    "Lyra: My records were altered. I hate bad medicine more than monsters.",
                                    "Lyra: Check the page margins. Bad lies press too hard.",
                                    "Lyra: The dosage was changed after the patients worsened.",
                                    "Lyra: Harm left fingerprints in my handwriting."),
                            questStage("lyra_chain_3_winch", "Inspect The Marked Bell Winch", "Marked Bell Winch", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_ward_marker", null, "", "",
                                    "Lyra: The bell winch carried medicine before it carried warnings.",
                                    "Lyra: Inspect the oil mark and rope groove. A delivery changed hands here.",
                                    "Lyra: The mark matches a marsh camp seal, not a clinic stamp.",
                                    "Lyra: The medicine left the road and came back wearing a lie."),
                            questStage("lyra_chain_3_patient", "Question The Shivering Patient", "Shivering Patient", 1, Quest.ObjectiveKind.TALK,
                                    "farmland", 1, "npc_citizen_woman", null, "", "",
                                    "Lyra: One patient woke enough to be angry. Good. Anger means blood is moving.",
                                    "Lyra: Ask what the tonic tasted like. Patients notice what healers miss.",
                                    "Lyra: Bitter root, grave salt, and cheap smoke. That was not my medicine.",
                                    "Lyra: Sabotage confirmed. Harm travels quickly when dressed as care."),
                            questStage("lyra_chain_3_choice", "Choose The Clinic Response", "False Medicine Proof", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "lyra_false_medicine",
                                    "Lyra: Choose our response: warn every clinic, hunt the brewer, protect the patients, or trace the buyer.",
                                    "Lyra: All of them matter. We choose first, not only.",
                                    "Lyra: Chosen. I will know which apology to refuse.",
                                    "Lyra: Good. Now the lie has a wound we can treat."))),
            companionStagedQuest("lyra_chain_4", "The Bitter Vial", "Lyra breaks the false-medicine brew and turns its ugly ingredients into an antidote.", 156, 124,
                    "lyra", "lyra_chain_5", List.of(
                            questStage("lyra_chain_4_shaman", "Break The False Brewer", "Goblin Shaman", 1, Quest.ObjectiveKind.DEFEAT,
                                    "goblin_camp", 1, null, "goblin_shaman", "", "",
                                    "Lyra: Someone brewed medicine to make fear look like sickness.",
                                    "Lyra: Break the recipe and the hand stirring it.",
                                    "Lyra: The brewer is down. The pot still hates everyone nearby.",
                                    "Lyra: Good. Malice is easier to treat when it stops chanting."),
                            questStage("lyra_chain_4_vial", "Recover The Bitter Vial", "Bitter Vial", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 1, "quest_supply_cache", null, "", "",
                                    "Lyra: Find the vial before the smoke teaches the evidence to vanish.",
                                    "Lyra: Keep it corked. I want the smell alive enough to accuse itself.",
                                    "Lyra: Bitter root, grave salt, cheap malice.",
                                    "Lyra: I can work with this."),
                            questStage("lyra_chain_4_roots", "Gather Antidote Roots", "Antidote Roots", 2, Quest.ObjectiveKind.GATHER,
                                    "farmland", 1, "quest_salve_herbs", null, "", "",
                                    "Lyra: The antidote needs roots that grow in clean mud. Rare, but less rare than decency.",
                                    "Lyra: Gather enough for the sick and the next sick. There is always a next.",
                                    "Lyra: These will cut the fever without cutting the patient down with it.",
                                    "Lyra: Good. Treatment can begin before blame finishes dressing."),
                            questStage("lyra_chain_4_choice", "Choose The Antidote Rule", "Bitter Vial Lesson", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "lyra_antidote_rule",
                                    "Lyra: Choose the rule for the cure: give it freely, track every dose, reserve some for relapse, or teach others the recipe.",
                                    "Lyra: Mercy without method runs out. Method without mercy becomes a locked shelf.",
                                    "Lyra: Chosen. That is a rule I can keep while tired.",
                                    "Lyra: The fever will break. The work will not."))),
            companionStagedQuest("lyra_chain_5", "The Patient She Lost", "Lyra returns to the cot where obedience cost a patient and asks the player to name the failure plainly.", 176, 140,
                    "lyra", "lyra_chain_6", List.of(
                            questStage("lyra_chain_5_cot", "Inspect The Old Clinic Cot", "Old Clinic Cot", 1, Quest.ObjectiveKind.VISIT,
                                    "graveyard", 1, "quest_inspection_cache", null, "", "",
                                    "Lyra: My first patient died while I waited for permission. That sentence has teeth.",
                                    "Lyra: Look at the cot. I need the failure named plainly.",
                                    "Lyra: The cot is clean now. That does not make it kind.",
                                    "Lyra: Waiting felt obedient. It was still a choice."),
                            questStage("lyra_chain_5_tag", "Read The Bell Tag", "Old Bell Tag", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_document_bundle", null, "", "",
                                    "Lyra: The bell tag marked who was allowed help first. As if pain queues politely.",
                                    "Lyra: Read it. The order matters. The order killed.",
                                    "Lyra: Civilians were listed after soldiers, even when the fever moved faster.",
                                    "Lyra: I followed the tag and called it triage."),
                            questStage("lyra_chain_5_cloak", "Inspect The Travel Cloak", "Lost Patient's Cloak", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_inspection_cache", null, "", "",
                                    "Lyra: The cloak belonged to the one I did not reach in time.",
                                    "Lyra: Inspect it. Do not turn them into a lesson before remembering they were cold.",
                                    "Lyra: The stitching was repaired twice. Someone expected more road.",
                                    "Lyra: I will not make waiting holy because it was orderly."),
                            questStage("lyra_chain_5_choice", "Choose What Failure Teaches", "First Lost Patient", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "lyra_first_patient",
                                    "Lyra: Choose what this failure teaches: disobey sooner, record every name, save who can be saved, or never let distance decide care.",
                                    "Lyra: Be honest. I have had enough gentle lies to last a lifetime.",
                                    "Lyra: Chosen. I will not make that mistake neatly again.",
                                    "Lyra: The room has finished being kind. I have not."))),
            companionStagedQuest("lyra_chain_6", "Care That Moves", "Lyra builds a traveling clinic kit and clears a road so patients do not have to wait for mercy to arrive late.", 198, 158,
                    "lyra", "lyra_chain_7", List.of(
                            questStage("lyra_chain_6_supplies", "Gather Traveling Clinic Supplies", "Travel Clinic Supplies", 3, Quest.ObjectiveKind.GATHER,
                                    "farmland", 1, "quest_supply_cache", null, "", "",
                                    "Lyra: A clinic that cannot move becomes furniture.",
                                    "Lyra: Gather supplies that can survive rain, panic, and being dropped by frightened hands.",
                                    "Lyra: The bags are heavy enough to matter.",
                                    "Lyra: Good. Shelves are useful. Roads are where people bleed."),
                            questStage("lyra_chain_6_road", "Clear The Patient Road", "Goblin Raider", 3, Quest.ObjectiveKind.DEFEND,
                                    "goblin_camp", 1, null, "goblin", "", "",
                                    "Lyra: Patients do not schedule disasters. Roads should stop pretending they can.",
                                    "Lyra: Clear what blocks the route. I will not lose someone to geography.",
                                    "Lyra: The road is open and deeply unimpressed by heroics.",
                                    "Lyra: Good. The road has a pulse again."),
                            questStage("lyra_chain_6_delivery", "Deliver The Field Kit", "Field Clinic Kit", 1, Quest.ObjectiveKind.DELIVER,
                                    null, 0, null, null, "lyra", "",
                                    "Lyra: Bring the kit back to me. I want to feel the weight before I trust it.",
                                    "Lyra: If the straps hold, I can leave before dawn and arrive before regret.",
                                    "Lyra: The kit opens fast. That matters more than looking tidy.",
                                    "Lyra: Care can move now."),
                            questStage("lyra_chain_6_choice", "Choose The Traveling Rule", "Moving Clinic Rule", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "lyra_moving_care",
                                    "Lyra: Choose how the clinic travels: toward worst wounds, toward forgotten places, along warning bells, or wherever no one else goes.",
                                    "Lyra: A route is a promise. Make one we can keep.",
                                    "Lyra: Chosen. I can work with that road.",
                                    "Lyra: Good. Mercy has legs."))),
            companionStagedQuest("lyra_chain_7", "The Fever Nest", "Lyra traces the false fever to poisoned rope silk and a tainted bell shrine, then decides how the cure is carried.", 224, 178,
                    "lyra", "lyra_chain_8", List.of(
                            questStage("lyra_chain_7_spiders", "Clear The Poisoned Nest", "Cave Spider", 4, Quest.ObjectiveKind.DEFEAT,
                                    "goblin_camp", 3, null, "spider", "", "",
                                    "Lyra: The source is under old silk. I am done treating symptoms while poison breeds.",
                                    "Lyra: Empty the nest. I want the clinic to remember rescue, not paperwork.",
                                    "Lyra: The nest is quiet. I hate quiet nests almost as much as loud ones.",
                                    "Lyra: Good. The fever has fewer legs now."),
                            questStage("lyra_chain_7_silk", "Gather Poisoned Rope Silk", "Poisoned Rope Silk", 2, Quest.ObjectiveKind.GATHER,
                                    "goblin_camp", 3, "quest_supply_cache", null, "", "",
                                    "Lyra: The rope silk carried the sickness along the bell line.",
                                    "Lyra: Gather enough to prove the path and burn the rest.",
                                    "Lyra: The silk sweats when warmed. That is as unpleasant as it is useful.",
                                    "Lyra: Now we know how the fever traveled."),
                            questStage("lyra_chain_7_bell", "Inspect The Infected Shrine Bell", "Infected Shrine Bell", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_ward_marker", null, "", "",
                                    "Lyra: A bell should warn the living, not spread sickness between them.",
                                    "Lyra: Inspect the shrine. Keep your gloves on.",
                                    "Lyra: The bell was washed in false tonic and rung over open water.",
                                    "Lyra: The fever ends here if we are careful and lucky. I prefer careful."),
                            questStage("lyra_chain_7_choice", "Choose How The Cure Travels", "Fever Cure Route", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "lyra_fever_cure",
                                    "Lyra: Choose the cure route: bell towers, road clinics, village shelves, or trained hands in every settlement.",
                                    "Lyra: Cure is not a bottle. It is a system that arrives on time.",
                                    "Lyra: Chosen. My bag is already packed.",
                                    "Lyra: The fever ends here. The work does not."))),
            companionStagedQuest("lyra_chain_8", "Mercy That Travels", "Lyra builds Oathstead into a standing home for care that can still leave at dawn.", 246, 196,
                    "lyra", null, List.of(
                            questStage("lyra_chain_8_cot", "Inspect Oathstead's Clinic Cot", "Oathstead Clinic Cot", 1, Quest.ObjectiveKind.VISIT,
                                    "farmland", 2, "quest_inspection_cache", null, "", "",
                                    "Lyra: Oathstead has too many wounds and not enough shelves. That is promising.",
                                    "Lyra: Inspect the cot with me. I want care that can leave at dawn and still come home.",
                                    "Lyra: The cot is plain, clean, and easy to move. Excellent bedside manner.",
                                    "Lyra: Maybe stopping does not always mean someone is being abandoned."),
                            questStage("lyra_chain_8_shelf", "Stock The Medicine Shelf", "Oathstead Medicine Shelf", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_supply_cache", null, "", "",
                                    "Lyra: Shelves are promises with labels.",
                                    "Lyra: Stock it for fever, cuts, fear, childbirth, old grief, and the kind of stupidity that wears armor.",
                                    "Lyra: The shelf is not full. Full shelves are myths told by rich cities.",
                                    "Lyra: It is enough to begin. Enough is a holy word."),
                            questStage("lyra_chain_8_choice", "Choose The Clinic's Promise", "Lyra's Oathstead Clinic", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "lyra_oathstead_clinic",
                                    "Lyra: Choose what we build here: road clinic, village infirmary, bell-route aid network, or a place that sends care before anyone begs.",
                                    "Lyra: I used to think mercy had to keep moving. Stop too long and you lose someone on the next road.",
                                    "Lyra: Chosen. Maybe mercy can travel from a place that stands.",
                                    "Lyra: Care that cannot travel is only furniture. I travel now, and I come back here."))),
            companionStagedQuest("samir_chain_1", "Cinders in the Reliquary", "Samir opens his family's reliquary after ember imps expose the old seal it was built to hide.", 104, 82,
                    "samir", "samir_chain_2", List.of(
                            questStage("samir_chain_1_imps", "Snuff The Reliquary Imps", "Ember Imp", 3, Quest.ObjectiveKind.DEFEAT,
                                    "goblin_camp", 5, null, "ember_imp", "", "",
                                    "Samir: My family guarded a reliquary and called it duty. I called it a locked door with hymns.",
                                    "Samir: Snuff the imps before the past becomes smoke.",
                                    "Samir: The imps are quiet. The reliquary is not.",
                                    "Samir: Fire is honest when it burns. People are less reliable."),
                            questStage("samir_chain_1_reliquary", "Inspect The Family Reliquary", "Family Reliquary", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 5, "quest_ward_marker", null, "", "",
                                    "Samir: The reliquary was polished every dawn and questioned never.",
                                    "Samir: Inspect the hinge marks. Reverence leaves fingerprints too.",
                                    "Samir: The reliquary was opened recently from the inside.",
                                    "Samir: The harder truth is always patient."),
                            questStage("samir_chain_1_seal", "Read The Inner Seal", "Inner Reliquary Seal", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 5, "quest_document_bundle", null, "", "",
                                    "Samir: The fire spared the inner seal. Of course it did.",
                                    "Samir: Read it. If the words ask for obedience, ask what they fear.",
                                    "Samir: The seal names a witness, not a keeper.",
                                    "Samir: My family inherited a locked question and called it faith."),
                            questStage("samir_chain_1_choice", "Choose What The Seal Means", "Reliquary Truth", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "samir_reliquary_truth",
                                    "Samir: Choose what we call this: inheritance, warning, prison, or a question my family was afraid to ask.",
                                    "Samir: Speak carefully. Some words become doors.",
                                    "Samir: Chosen. I need to decide whether I am relieved.",
                                    "Samir: Good. A locked door with hymns is still a locked door."))),
            companionStagedQuest("samir_chain_2", "Oil for the Dawn Lamp", "Samir gathers supplies for a forbidden rite meant to make doubt visible rather than shameful.", 126, 96,
                    "samir", "samir_chain_3", List.of(
                            questStage("samir_chain_2_oil", "Gather Clean Lamp Oil", "Clean Lamp Oil", 2, Quest.ObjectiveKind.GATHER,
                                    "farmland", 2, "quest_lamp_supplies", null, "", "",
                                    "Samir: Rituals begin with supplies. Doubt, sadly, is not flammable enough on its own.",
                                    "Samir: Bring clean oil. A dishonest flame is just a sermon with heat.",
                                    "Samir: The oil is clear. It has not yet been told what to believe.",
                                    "Samir: Good. The lamp will answer to what is there, not what is convenient."),
                            questStage("samir_chain_2_salt", "Gather Ash Salt", "Ash Salt", 2, Quest.ObjectiveKind.GATHER,
                                    "goblin_camp", 5, "quest_supply_cache", null, "", "",
                                    "Samir: Ash salt remembers what fire changed and what it failed to change.",
                                    "Samir: Gather enough for the rite. Not enough for spectacle.",
                                    "Samir: The salt is bitter. Useful things often are.",
                                    "Samir: Good. Doubt has a taste now."),
                            questStage("samir_chain_2_thread", "Gather Prayer Thread", "Prayer Thread", 1, Quest.ObjectiveKind.GATHER,
                                    "farmland", 2, "quest_lamp_supplies", null, "", "",
                                    "Samir: Prayer thread binds the lamp. It should not bind the hand holding it.",
                                    "Samir: Find thread that has not been knotted by temple decree.",
                                    "Samir: The thread is plain. No seal, no threat, no borrowed certainty.",
                                    "Samir: Good. A lantern is only holy if it can be carried."),
                            questStage("samir_chain_2_choice", "Choose The Rite's Purpose", "Forbidden Dawn Rite", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "samir_forbidden_rite",
                                    "Samir: Choose what the rite asks: whether the light lies, whether doubt is witness, whether faith can disobey, or whether I am afraid.",
                                    "Samir: I would prefer a simple question. That is how I know it would be cowardly.",
                                    "Samir: Chosen. Return before Sanctum notices the missing ceremony.",
                                    "Samir: The lamp is ready to be honest."))),
            companionStagedQuest("samir_chain_3", "Lantern Without Permission", "Samir inspects the ward seal he was forbidden to question and asks the player to witness bad light plainly.", 148, 118,
                    "samir", "samir_chain_4", List.of(
                            questStage("samir_chain_3_seal", "Inspect The Ward Seal", "Ward Seal", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_ward_marker", null, "", "",
                                    "Samir: The seal is not protecting Sanctum from darkness. It is hiding who first cracked the light.",
                                    "Samir: Inspect it with your own hands. I need a witness who was not raised to bow.",
                                    "Samir: The seal burns cold at the center.",
                                    "Samir: That is not holiness. That is light taught to lie."),
                            questStage("samir_chain_3_shadow", "Trace The Backward Shadow", "Backward Shrine Shadow", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_inspection_cache", null, "", "",
                                    "Samir: Shadows should not point toward dawn. Even doubt has manners.",
                                    "Samir: Trace it. Tell me where the light refuses to reveal itself.",
                                    "Samir: The shadow runs under the altar, not away from it.",
                                    "Samir: A lantern is useless if it only shines where permitted."),
                            questStage("samir_chain_3_acolyte", "Question The Doubtful Acolyte", "Doubtful Acolyte", 1, Quest.ObjectiveKind.TALK,
                                    "graveyard", 2, "npc_mira_sunwarden", null, "", "",
                                    "Samir: One acolyte saw the same shadow and apologized for seeing it.",
                                    "Samir: Ask them what they were told to forget.",
                                    "Samir: They were ordered to call the false dawn a blessing.",
                                    "Samir: Faith that needs witnesses silenced is only fear with candles."),
                            questStage("samir_chain_3_choice", "Choose How Doubt Speaks", "Questioned Light", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "samir_questioned_light",
                                    "Samir: Choose how doubt speaks: as warning, prayer, accusation, or the first honest light in the room.",
                                    "Samir: I was taught doubt was a crack. Perhaps it is a window.",
                                    "Samir: Chosen. If the seal answered, it answered plainly.",
                                    "Samir: Good. We have evidence that can breathe."))),
            companionStagedQuest("samir_chain_4", "Ashes of the First Keeper", "Samir learns his ancestor was a witness forced into obedience, not the obedient keeper Sanctum celebrates.", 170, 136,
                    "samir", "samir_chain_5", List.of(
                            questStage("samir_chain_4_envoy", "Scatter The Ember Envoy", "Ember Imp", 4, Quest.ObjectiveKind.DEFEAT,
                                    "goblin_camp", 5, null, "ember_imp", "", "",
                                    "Samir: My ancestor was not a keeper. He was a witness who was told to kneel.",
                                    "Samir: The envoy guards his record with a crowd of sparks. Scatter them.",
                                    "Samir: The sparks are gone. The ash remains inconveniently literate.",
                                    "Samir: Good. Sanctum cannot call smoke metaphor if we bring the page."),
                            questStage("samir_chain_4_record", "Recover The Ancestor Record", "First Keeper Record", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 5, "quest_document_bundle", null, "", "",
                                    "Samir: Find the record before heat eats the names.",
                                    "Samir: If the record survived, bring it before Sanctum improves it into obedience.",
                                    "Samir: The first keeper refused the first lie.",
                                    "Samir: I can refuse the next one."),
                            questStage("samir_chain_4_kneeling", "Inspect The Kneeling Mark", "Kneeling Mark", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 5, "quest_ward_marker", null, "", "",
                                    "Samir: The floor is marked where they made him kneel.",
                                    "Samir: Inspect it. Sacred stone remembers pressure.",
                                    "Samir: The mark faces away from the altar.",
                                    "Samir: They did not ask him to worship. They asked him to submit."),
                            questStage("samir_chain_4_choice", "Choose The First Keeper's Name", "First Keeper Witness", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "samir_first_keeper",
                                    "Samir: Choose what he was: keeper, witness, rebel, or a faithful man punished for seeing clearly.",
                                    "Samir: Names are small altars. Build this one honestly.",
                                    "Samir: Chosen. I think he would have breathed easier hearing it.",
                                    "Samir: Good. The first lie has lost one heir."))),
            companionStagedQuest("samir_chain_5", "The Hymn That Lied", "A censored hymn and family prayer-chain reveal that Samir's mother's grief was witness, not weakness.", 190, 152,
                    "samir", "samir_chain_6", List.of(
                            questStage("samir_chain_5_hymn", "Read The Censored Dawn Hymn", "Censored Dawn Hymn", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 0, "quest_document_bundle", null, "", "",
                                    "Samir: They taught me a hymn with a verse missing. You can hear the scar if you know where to breathe.",
                                    "Samir: Find the hymn. I want the silence named.",
                                    "Samir: The missing verse calls doubt a lamp carried through smoke.",
                                    "Samir: My teachers cut out the part that might have saved me years."),
                            questStage("samir_chain_5_tile", "Inspect The Cracked Sun Tile", "Cracked Sun Tile", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 0, "quest_ward_marker", null, "", "",
                                    "Samir: The tile cracked on the word obedience.",
                                    "Samir: Inspect it. Even stone objects eventually.",
                                    "Samir: The crack forms an old dawn mark, older than Sanctum's present rite.",
                                    "Samir: The light was never as obedient as they claimed."),
                            questStage("samir_chain_5_chain", "Inspect The Family Prayer Chain", "Family Prayer Chain", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 0, "quest_lamp_supplies", null, "", "",
                                    "Samir: My mother held this chain when she stopped singing the official verse.",
                                    "Samir: Inspect the links. Grief leaves different marks than fear.",
                                    "Samir: Three links are worn where her thumb repeated the missing line.",
                                    "Samir: My mother's grief was not weakness. It was witness."),
                            questStage("samir_chain_5_choice", "Choose The Missing Verse", "Missing Dawn Verse", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "samir_missing_verse",
                                    "Samir: Choose how we sing the missing verse: quietly, publicly, as mourning, or as refusal.",
                                    "Samir: I am tired of holy songs that cannot survive honest breath.",
                                    "Samir: Chosen. Return with the missing verse.",
                                    "Samir: Good. Silence has lost its melody."))),
            companionStagedQuest("samir_chain_6", "Prayer in the Ash Field", "Samir faces the thing wearing his mother's prayer-chain and learns whether anger can serve the light.", 212, 168,
                    "samir", "samir_chain_7", List.of(
                            questStage("samir_chain_6_field", "Enter The Ash Field", "Ash Field", 1, Quest.ObjectiveKind.VISIT,
                                    "goblin_camp", 5, "quest_inspection_cache", null, "", "",
                                    "Samir: The prayer-chain was buried with my mother. Something is wearing it.",
                                    "Samir: Enter the ash field. Walk carefully. Grief hides under soft ground.",
                                    "Samir: The ash is warm where no fire remains.",
                                    "Samir: Something learned to wear mourning as bait."),
                            questStage("samir_chain_6_wraith", "Free The Prayer Chain", "Ash Wraith", 1, Quest.ObjectiveKind.DEFEAT,
                                    "graveyard", 1, null, "ash_wraith", "", "",
                                    "Samir: Free the chain and the grief attached to it.",
                                    "Samir: Strike cleanly. Do not hate the sorrow it is using.",
                                    "Samir: The wraith is gone. The chain is only metal and memory now.",
                                    "Samir: I need to learn whether anger can be holy."),
                            questStage("samir_chain_6_chain", "Recover His Mother's Chain", "Mother's Prayer Chain", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_lamp_supplies", null, "", "",
                                    "Samir: Bring me the chain. Slowly, if you can.",
                                    "Samir: I want to know whether my hand remembers the missing verse.",
                                    "Samir: The chain is cold now. Peaceful would be too simple a word.",
                                    "Samir: Anger can be a lamp too, if it shows where harm stood."),
                            questStage("samir_chain_6_choice", "Choose What Anger Serves", "Holy Anger", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "samir_holy_anger",
                                    "Samir: Choose what this anger serves: protection, truth, mourning, or refusal to let harm call itself sacred.",
                                    "Samir: I was taught anger darkens the soul. I now suspect it depends where one points the lamp.",
                                    "Samir: Chosen. I can carry that without worshipping it.",
                                    "Samir: Good. The chain is not a leash anymore."))),
            companionStagedQuest("samir_chain_7", "Dawn Chosen Twice", "Samir confronts the false dawn's herald and rejects light used as a leash.", 240, 190,
                    "samir", "samir_chain_8", List.of(
                            questStage("samir_chain_7_altar", "Enter The False Dawn Altar", "False Dawn Altar", 1, Quest.ObjectiveKind.VISIT,
                                    "goblin_camp", 5, "quest_ward_marker", null, "", "",
                                    "Samir: The first dawn was assigned to me. The second one I choose.",
                                    "Samir: Enter the altar. If the light orders you to kneel, disappoint it.",
                                    "Samir: The altar shines too brightly to reveal anything.",
                                    "Samir: Brightness is not proof. It is only volume."),
                            questStage("samir_chain_7_symbol", "Inspect The Inverted Sun Symbol", "Inverted Sun Symbol", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 5, "quest_ward_marker", null, "", "",
                                    "Samir: The symbol is upside down and still arrogant.",
                                    "Samir: Inspect it. Bad light loves familiar shapes.",
                                    "Samir: The symbol burns around the old dawn mark rather than through it.",
                                    "Samir: The true light was buried, not killed."),
                            questStage("samir_chain_7_herald", "Break The Flame Herald", "Flame Herald", 1, Quest.ObjectiveKind.DEFEAT,
                                    "goblin_camp", 5, null, "flame_herald", "", "",
                                    "Samir: Break the herald. I will not let light be used as a leash.",
                                    "Samir: Stand firm. False dawn hates a witness who remains upright.",
                                    "Samir: The herald is ash. The altar is quieter and more honest for it.",
                                    "Samir: Dawn is not obedience. Dawn is choosing light again."),
                            questStage("samir_chain_7_choice", "Choose The Second Dawn", "Second Dawn", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "samir_second_dawn",
                                    "Samir: Choose what the second dawn means: revealed truth, chosen faith, mercy after fire, or doubt carried openly.",
                                    "Samir: The dawn is not proven by brightness. It is proven by what it reveals.",
                                    "Samir: Chosen. Return when the ash has settled.",
                                    "Samir: Good. I can stand in this light without kneeling to it."))),
            companionStagedQuest("samir_chain_8", "A Lantern at Oathstead", "Samir places his lantern at Oathstead and chooses a faith that can question without going dark.", 262, 208,
                    "samir", null, List.of(
                            questStage("samir_chain_8_marker", "Inspect Oathstead's Oath Marker", "Oathstead Oath Marker", 1, Quest.ObjectiveKind.VISIT,
                                    "farmland", 2, "quest_ward_marker", null, "", "",
                                    "Samir: Oathstead does not ask me to kneel before the lamp. That is new.",
                                    "Samir: Inspect the marker with me. I want to know whether an oath can stand without a chain.",
                                    "Samir: The marker is plain. It does not demand my certainty.",
                                    "Samir: That may be why I trust it."),
                            questStage("samir_chain_8_lantern", "Place The Dawn Lantern", "Oathstead Dawn Lantern", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_lamp_supplies", null, "", "",
                                    "Samir: Place the lantern where travelers can see it and priests can fail to own it.",
                                    "Samir: Keep the flame low. A useful light does not need to shout.",
                                    "Samir: The flame holds steady without burning white.",
                                    "Samir: I thought doubt was a wound in faith. Perhaps it is how faith breathes."),
                            questStage("samir_chain_8_tablet", "Read The Repaired Oath Tablet", "Repaired Oath Tablet", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_document_bundle", null, "", "",
                                    "Samir: The tablet is repaired with an obvious seam. I requested that.",
                                    "Samir: Read it. Broken things should not be forced to pretend they were always whole.",
                                    "Samir: The oath names witness before obedience.",
                                    "Samir: Good. That order matters."),
                            questStage("samir_chain_8_choice", "Choose The Light That Stays", "Samir's Oathstead Light", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "samir_oathstead_light",
                                    "Samir: Choose what I keep here: shrine, lantern road, doubtful chapel, or a place where faith answers questions.",
                                    "Samir: I want a light that can question. I want people less afraid of honest shadow.",
                                    "Samir: Chosen. I will stand here.",
                                    "Samir: Not as Sanctum's witness. As my own."))),
            companionStagedQuest("aria_chain_1", "Road Scout's Warning", "Aria notices the player walking past small road signs that would have killed a careless traveler.", 58, 44,
                    "aria", "aria_chain_2", List.of(
                            questStage("aria_chain_1_marks", "Read The Road", "Roadwatch Warning Marks", 2, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 0, "quest_roadwatch_warning_marks", null, "", "",
                                    "Aria: You walked past two snares and a warning mark. Either you are brave or decorative.",
                                    "Aria: Look down. The road talks if you stop admiring your boots.",
                                    "Aria: That is enough to prove the marks were placed for someone who knew roadwatch code.",
                                    "Aria: Good. You can look down and still see things. Rare skill."),
                            questStage("aria_chain_1_locals", "Ask The Locals", "Oakhaven Road Rumors", 2, Quest.ObjectiveKind.ASK_AROUND,
                                    null, 0, null, null, "", "",
                                    "Aria: Marks tell one half. People tell the other half badly, but usefully.",
                                    "Aria: Ask around. I want separate mouths, not one loud rumor.",
                                    "Aria: The stories agree where they should not. Someone moved through here on purpose.",
                                    "Aria: Good. Rumor becomes evidence when it stops rehearsing."),
                            questStage("aria_chain_1_snares", "Lift The Snares", "Snare Cord", 2, Quest.ObjectiveKind.GATHER,
                                    "farmland", 1, "quest_roadwatch_warning_marks", null, "", "",
                                    "Aria: Now take the cords. Hands remember knots better than witnesses remember faces.",
                                    "Aria: Bring the snares back intact. I want to know who tied them.",
                                    "Aria: That cord is scout work copied by someone with meaner hands.",
                                    "Aria: You noticed the road before it bled. That is the beginning of trust."))),
            companionStagedQuest("aria_chain_2", "Missing Road Signs", "Broken road signs and scratched trees reveal a deliberate attempt to make the safe road lie.", 92, 72,
                    "aria", "aria_chain_3", List.of(
                            questStage("aria_chain_2_signs", "Recover The Signs", "Broken Road Signs", 3, Quest.ObjectiveKind.GATHER,
                                    "farmland", 2, "quest_broken_road_signs", null, "", "",
                                    "Aria: Road signs do not wander off. People move them. Usually people with knives.",
                                    "Aria: Bring the broken signs. Do not trust any arrow that looks newly helpful.",
                                    "Aria: These were snapped, not lost. Someone wanted the road to lie.",
                                    "Aria: Good. Wood breaks differently when a hand is angry."),
                            questStage("aria_chain_2_scratches", "Follow The Scratches", "Scratched Tree Marks", 2, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 3, "quest_roadwatch_warning_marks", null, "", "",
                                    "Aria: The trees were marked after the signs were moved. That means a second pass.",
                                    "Aria: Follow the scratches and tell me whether they guide travelers or herd them.",
                                    "Aria: Herding, then. Someone wanted strangers walking into a waiting mouth.",
                                    "Aria: The road is lying in two languages now."),
                            questStage("aria_chain_2_carter", "Find The Lost Carter", "Lost Carter", 1, Quest.ObjectiveKind.TALK,
                                    "farmland", 1, "npc_citizen_man", null, "", "",
                                    "Aria: A carter vanished yesterday. If he is alive, he saw where the false trail bends.",
                                    "Aria: Find him. Ask calmly. Frightened people hand you truth sideways.",
                                    "Aria: He saw green cloaks near the bend. Not roadwatch green. Costume green.",
                                    "Aria: Now we have a witness and a direction. I hate both, usefully."))),
            companionStagedQuest("aria_chain_3", "Underestimated on Purpose", "Aria uses the enemy's belief that she is harmless to draw out the false scouts.", 118, 94,
                    "aria", "aria_chain_4", List.of(
                            questStage("aria_chain_3_bait", "Speak With The Bait", "Nervous Bait", 1, Quest.ObjectiveKind.TALK,
                                    "farmland", 2, "npc_citizen_woman", null, "", "",
                                    "Aria: They left someone shaking by the road so kind fools would stop.",
                                    "Aria: Talk first. If the fear is real, we help. If it is staged, we learn where the hooks are.",
                                    "Aria: Real fear, false story. Someone told her what to say.",
                                    "Aria: Being used as bait leaves a smell. I know it too well."),
                            questStage("aria_chain_3_ambush", "Break The Ambush", "False Scout Ambush", 3, Quest.ObjectiveKind.DEFEND,
                                    "goblin_camp", 0, null, "goblin_scout", "", "",
                                    "Aria: They think I am bait. That is rude, but useful.",
                                    "Aria: Let them underestimate us, then charge them for the lesson.",
                                    "Aria: The ambush is broken. Their footprints are suddenly very honest.",
                                    "Aria: Being underestimated saves time. They explain their whole plan with their feet."),
                            questStage("aria_chain_3_choice", "Choose The Lesson", "Ambush Confession", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "aria_ambush_lesson",
                                    "Aria: One of them talked. Now decide what kind of truth we carry back.",
                                    "Aria: Truth, mercy, accountability. Choose one without pretending the others disappear.",
                                    "Aria: Chosen, then. The road will remember how we named this.",
                                    "Aria: You did not rush the answer. I noticed."))),
            companionStagedQuest("aria_chain_4", "The Foxglove Trail", "Aria follows foxglove markers left by her mother, learning the safe-road code was meant for forgotten travelers.", 142, 114,
                    "aria", "aria_chain_5", List.of(
                            questStage("aria_chain_4_flowers", "Read The Foxglove", "Foxglove Trail Markers", 3, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 0, "quest_roadwatch_warning_marks", null, "", "",
                                    "Aria: Foxglove is pretty, poisonous, and easy to overlook. I respect that.",
                                    "Aria: Find the markers. My mother planted safe roads where nobody important looked.",
                                    "Aria: Three flowers, three turns, one safe line through a bad road.",
                                    "Aria: My mother trusted small things to survive arrogant people."),
                            questStage("aria_chain_4_cache", "Open The Scout Cache", "Old Scout Cache", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 1, "icon_chest", null, "", "",
                                    "Aria: There should be a cache where the third marker points. If it is gone, we learn who wanted it gone.",
                                    "Aria: Open it carefully. My mother trapped lessons, not just boxes.",
                                    "Aria: The cache held route ink, dried food, and a child's spare boot. That was her work.",
                                    "Aria: A good road should remember small people. She wrote that down."),
                            questStage("aria_chain_4_woodcutter", "Question The Woodcutter", "Oakhaven Woodcutter", 1, Quest.ObjectiveKind.TALK,
                                    "farmland", 3, "npc_citizen_man", null, "", "",
                                    "Aria: Someone near Oakhaven saw the cache before us. Woodcutters notice paths everyone else calls trees.",
                                    "Aria: Ask what he saw. Do not make him brave before he is ready.",
                                    "Aria: He saw scout green moving at dusk, but the gait was wrong.",
                                    "Aria: Costume green again. The road is wearing someone else's face."))),
            companionStagedQuest("aria_chain_5", "The Scout Who Did Not Return", "Aria reaches the abandoned camp where her mother vanished and must separate hope from proof.", 166, 132,
                    "aria", "aria_chain_6", List.of(
                            questStage("aria_chain_5_camp", "Search The Camp", "Abandoned Scout Camp", 2, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 2, "location_camp_fire", null, "", "",
                                    "Aria: I do not need comfort. I need to know where the trail ended.",
                                    "Aria: Check the fire and the torn cloak. Be careful with hope.",
                                    "Aria: She made camp here injured, hungry, and still thinking clearly.",
                                    "Aria: That sounds like her. Infuriatingly."),
                            questStage("aria_chain_5_map", "Recover The Route Map", "Unfinished Route Map", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 2, "quest_broken_road_signs", null, "", "",
                                    "Aria: There should be a map if she had breath left to make one.",
                                    "Aria: Find it. If the line stops, let it stop honestly.",
                                    "Aria: The route continues past the camp. She was not done.",
                                    "Aria: Hope is dangerous. Evidence is worse."),
                            questStage("aria_chain_5_choice", "Name The Hope", "Hope And Proof", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "aria_mother_truth",
                                    "Aria: Say it plainly. Do we follow this as rescue, burial, or debt?",
                                    "Aria: Choose the word we can survive carrying.",
                                    "Aria: Then that is the word. I will not pretend it is lighter than it is.",
                                    "Aria: Thank you for not making comfort do the work of truth."))),
            companionStagedQuest("aria_chain_6", "Bandits in Scout Green", "False scouts stole Aria's mother's route token and used roadwatch trust as camouflage.", 190, 152,
                    "aria", "aria_chain_7", List.of(
                            questStage("aria_chain_6_rumors", "Separate Rumor From Uniform", "False Scout Rumors", 2, Quest.ObjectiveKind.ASK_AROUND,
                                    null, 0, null, null, "", "",
                                    "Aria: They stole the green. Trust without earning it. That makes me mean.",
                                    "Aria: Ask who saw the uniforms. I want details, not fear wearing certainty.",
                                    "Aria: The witnesses agree on the badge and disagree on the people wearing it.",
                                    "Aria: Good. Real scouts leave habits. Thieves leave costumes."),
                            questStage("aria_chain_6_patrol", "Break The False Patrol", "False Scout Patrol", 3, Quest.ObjectiveKind.DEFEND,
                                    "goblin_camp", 3, null, "bandit_archer", "", "",
                                    "Aria: The false patrol is close. They expect gratitude before the knife.",
                                    "Aria: Break them before they sell another safe road.",
                                    "Aria: The patrol is broken. Their green looks cheaper on the ground.",
                                    "Aria: I would call that justice, but justice usually has better boots."),
                            questStage("aria_chain_6_token", "Recover The Token", "Route Token", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 3, "icon_chest", null, "", "",
                                    "Aria: Search their pack. If they have my mother's token, do not open your hand until I ask.",
                                    "Aria: Find the token. Some things deserve a witness before they are touched.",
                                    "Aria: That is hers. Either they found her body, or they found who took her.",
                                    "Aria: Keep it sealed a moment longer. I need to breathe around it first."))),
            companionStagedQuest("aria_chain_7", "Rootmaw Crossing", "The final foxglove marker points to a corrupted crossing where Aria's mother's last safe path still holds.", 222, 176,
                    "aria", "aria_chain_8", List.of(
                            questStage("aria_chain_7_marker", "Read The Final Marker", "Final Foxglove Marker", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_roadwatch_warning_marks", null, "", "",
                                    "Aria: The last marker is at Rootmaw Crossing. Whatever guards it does not need words.",
                                    "Aria: Read the final foxglove before we disturb what is waiting.",
                                    "Aria: The marker says safe road, last breath, keep moving. She wrote it for strangers.",
                                    "Aria: Even dying, she was still getting people home."),
                            questStage("aria_chain_7_rescue", "Free The Trapped Scout", "Trapped Scout", 1, Quest.ObjectiveKind.RESCUE,
                                    "farmland", 1, "npc_citizen_woman", "rootmaw_stag", "", "",
                                    "Aria: Someone is trapped near the crossing. If we save them, we learn whether the path still serves the living.",
                                    "Aria: Reach them before the guardian decides fear is trespass.",
                                    "Aria: They are alive. The crossing remembers mercy, somehow.",
                                    "Aria: That was not a grave. Good. I needed one thing here not to be a grave."),
                            questStage("aria_chain_7_guardian", "Face Rootmaw", "Rootmaw Stag", 1, Quest.ObjectiveKind.DEFEAT,
                                    "farmland", 1, null, "rootmaw_stag", "", "",
                                    "Aria: Now the thing wearing the crossing has to answer.",
                                    "Aria: Bring down the stag. Not for glory. For passage.",
                                    "Aria: The crossing is open. Return before I forget how to breathe.",
                                    "Aria: She marked the safe path all the way to the end."),
                            questStage("aria_chain_7_choice", "Choose What The End Means", "Aria's Last Marker", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "aria_crossing_truth",
                                    "Aria: Tell me what we carry from this. Grief, pride, anger, or all of them badly folded.",
                                    "Aria: Choose honestly. I am tired of tidy endings.",
                                    "Aria: Then that is the shape of it. Not clean, but ours.",
                                    "Aria: I can stand under that truth without vanishing."))),
            companionStagedQuest("aria_chain_8", "A Road With My Name On It", "Aria brings the recovered roadwatch code to Oathstead and chooses what kind of scout she becomes.", 244, 194,
                    "aria", null, List.of(
                            questStage("aria_chain_8_map", "Draft Oathstead's Road", "Oathstead Road Map", 2, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_broken_road_signs", null, "", "",
                                    "Aria: I used to want them to stop underestimating me. Now I think I will charge extra for it.",
                                    "Aria: Mark the road Oathstead actually uses, not the one a proud mapmaker would invent.",
                                    "Aria: The map is honest enough to be useful. That is rarer than beauty.",
                                    "Aria: Oathstead can have roads that do not lie."),
                            questStage("aria_chain_8_gate", "Plant The Foxglove", "Foxglove By The Gate", 1, Quest.ObjectiveKind.VISIT,
                                    "farmland", 2, "quest_roadwatch_warning_marks", null, "", "",
                                    "Aria: Plant foxglove by the gate. Pretty, poisonous, overlooked. A fair warning and a fair welcome.",
                                    "Aria: Put it where people leaving can see it, and people returning can forgive it.",
                                    "Aria: There. A small sign that says home can still have teeth.",
                                    "Aria: My mother's road reaches this far now."),
                            questStage("aria_chain_8_choice", "Choose The Road Ahead", "Aria's Future Road", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "aria_oathstead_future",
                                    "Aria: I can stay, scout, teach, or keep pretending I am only passing through.",
                                    "Aria: Help me choose the honest version, not the easiest one.",
                                    "Aria: Chosen. I hate how steady that feels.",
                                    "Aria: Oathstead needs roads that do not lie. I can help with that."))),
            companionStagedQuest("vesper_chain_1", "The Green Under White", "Vesper tends a root circle that should be sleeping, but something beneath the snow is waking in pain.", 84, 66,
                    "vesper", "vesper_chain_2", List.of(
                            questStage("vesper_chain_1_circle", "Read The Root Circle", "Snowroot Circle", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_ward_marker", null, "", "",
                                    "Vesper: Step lightly. Snow is only quiet because it is listening.",
                                    "Vesper: Read the circle first. Do not assume waking means healing.",
                                    "Vesper: The roots woke too early. That is rarely joy.",
                                    "Vesper: The snow answered by refusing to stay quiet."),
                            questStage("vesper_chain_1_seed", "Inspect The Frozen Seed Bowl", "Frozen Seed Bowl", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_salve_herbs", null, "", "",
                                    "Vesper: Seeds know patience. A frozen bowl with restless seeds is either miracle or warning.",
                                    "Vesper: Look for cracks, warmth, or rot. Each tells a different kind of truth.",
                                    "Vesper: The bowl is cold outside and warm underneath. Something below is breathing wrong.",
                                    "Vesper: Warmth in the wrong place can kill as neatly as frost."),
                            questStage("vesper_chain_1_choice", "Name The Waking", "The Waking Root", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "vesper_first_root",
                                    "Vesper: Say what you think this is: healing, hunger, warning, or grief with leaves.",
                                    "Vesper: Choose gently. The land hears names before people do.",
                                    "Vesper: Chosen. I will not make the root carry a prettier word than it earned.",
                                    "Vesper: Good. The first kindness is not lying to the wound."))),
            companionStagedQuest("vesper_chain_2", "Firewood and Feverroot", "Vesper insists on practical winter care before interpreting omens.", 112, 88,
                    "vesper", "vesper_chain_3", List.of(
                            questStage("vesper_chain_2_firewood", "Gather Firewood", "Firewood Bundles", 3, Quest.ObjectiveKind.GATHER,
                                    "farmland", 0, "quest_firewood_bundle", null, "", "",
                                    "Vesper: People ask druids for signs. I ask for firewood. Guess which keeps children alive.",
                                    "Vesper: Bring wood first. Prophecy is warmer after the hearth catches.",
                                    "Vesper: Good. That is enough wood for a night that wants to be cruel.",
                                    "Vesper: Warmth first. Always."),
                            questStage("vesper_chain_2_feverroot", "Gather Feverroot", "Feverroot And Winter Moss", 2, Quest.ObjectiveKind.GATHER,
                                    "farmland", 1, "quest_salve_herbs", null, "", "",
                                    "Vesper: Now root and moss. Small green things keep more promises than kings.",
                                    "Vesper: Take what is needed. Leave enough living for tomorrow.",
                                    "Vesper: The moss is bitter and useful. Most honest medicines are.",
                                    "Vesper: Good. Prophecy second."),
                            questStage("vesper_chain_2_patient", "Check The Feverish Child", "Feverish Child", 1, Quest.ObjectiveKind.TALK,
                                    "farmland", 1, "npc_citizen_woman", null, "", "",
                                    "Vesper: One child heard the roots in her sleep. Children are poor liars and excellent warnings.",
                                    "Vesper: Ask softly. Fear makes dreams dress as monsters.",
                                    "Vesper: She dreamed of green under ice, knocking from below.",
                                    "Vesper: That is not an omen. That is someone trapped enough to learn rhythm."),
                            questStage("vesper_chain_2_choice", "Choose The Care", "Winter Care", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "vesper_practical_care",
                                    "Vesper: Choose what comes first if the road worsens: chase the sign, protect the sick, or wake the grove.",
                                    "Vesper: There is no pure answer. Only the one we will be responsible for.",
                                    "Vesper: Chosen. Responsibility is heavier than certainty, but kinder.",
                                    "Vesper: Good. Care that cannot choose is only panic with clean hands."))),
            companionStagedQuest("vesper_chain_3", "The Frozen Burrow", "Animals are fleeing frozen burrows filled with black root growth.", 138, 110,
                    "vesper", "vesper_chain_4", List.of(
                            questStage("vesper_chain_3_wolves", "Drive Off The Wolves", "Frost Wolf", 3, Quest.ObjectiveKind.DEFEAT,
                                    "graveyard", 0, null, "frost_wolf", "", "",
                                    "Vesper: When animals abandon shelter in winter, something worse than cold has moved in.",
                                    "Vesper: Clear the wolves. Do not hate them for fleeing a bad home.",
                                    "Vesper: The wolves are gone. Their tracks circle the burrows, afraid to return.",
                                    "Vesper: Fear teaches clean maps."),
                            questStage("vesper_chain_3_burrows", "Inspect The Burrows", "Frozen Burrows", 2, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 0, "quest_inspection_cache", null, "", "",
                                    "Vesper: Now look where they would rather starve than sleep.",
                                    "Vesper: Inspect the burrows. Black roots do not belong in winter beds.",
                                    "Vesper: The roots are not spreading. They are reaching.",
                                    "Vesper: Reaching means want. Want can be worse than rot."),
                            questStage("vesper_chain_3_growth", "Collect Black Root Threads", "Black Root Threads", 1, Quest.ObjectiveKind.GATHER,
                                    "graveyard", 0, "quest_salve_herbs", null, "", "",
                                    "Vesper: Take one thread. Carefully. Some plants bite by becoming memory.",
                                    "Vesper: Bring it intact. I need to know whether it remembers spring.",
                                    "Vesper: It is black outside and green inside. That is different, and worse.",
                                    "Vesper: Something living learned to look dead."))),
            companionStagedQuest("vesper_chain_4", "Snowroot Memory", "Vesper returns to her family grove and confronts the grief she mistook for personal failure.", 162, 128,
                    "vesper", "vesper_chain_5", List.of(
                            questStage("vesper_chain_4_grove", "Find The Family Grove", "Snowroot Grove", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_ward_marker", null, "", "",
                                    "Vesper: My family kept spring under the snow. Then one year, spring did not answer.",
                                    "Vesper: Search the grove. Old grief likes roots.",
                                    "Vesper: The grove is sealed, not dead. That is a crueler sort of silence.",
                                    "Vesper: I have been mourning a locked door."),
                            questStage("vesper_chain_4_names", "Read The Carved Names", "Carved Family Names", 2, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_document_bundle", null, "", "",
                                    "Vesper: Names hold when voices stop. Read them kindly.",
                                    "Vesper: Find where the carving changes. Families hide warnings in habits.",
                                    "Vesper: The last names were carved by one hand. In haste.",
                                    "Vesper: Someone stayed behind to make the seal hold."),
                            questStage("vesper_chain_4_charm", "Recover The Green Charm", "Buried Green Charm", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_ward_marker", null, "", "",
                                    "Vesper: The charm should be near the oldest root. If it remembers green, so did they.",
                                    "Vesper: Dig carefully. Do not make a grave out of impatience.",
                                    "Vesper: The charm is warm. Barely, but warmly.",
                                    "Vesper: I thought the grove died because I was too young to save it."),
                            questStage("vesper_chain_4_choice", "Choose What The Grove Kept", "Snowroot Memory", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "vesper_family_grove",
                                    "Vesper: Was this protection, fear, sacrifice, or a mistake that saved lives anyway?",
                                    "Vesper: Choose without polishing the answer into comfort.",
                                    "Vesper: Chosen. Roots may have a crueler truth, but it is still truth.",
                                    "Vesper: I can carry that better than blame without shape."))),
            companionStagedQuest("vesper_chain_5", "The Druid Who Buried Spring", "An old druid's warning reveals that the grove was sealed intentionally to stop corruption from spreading.", 184, 146,
                    "vesper", "vesper_chain_6", List.of(
                            questStage("vesper_chain_5_cave", "Find The Hermit Cave", "Hermit Cave", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 2, "quest_inspection_cache", null, "", "",
                                    "Vesper: Old druids hide mistakes under wisdom. I would like to dig.",
                                    "Vesper: Find the cave. Do not trust anything that calls burial mercy too quickly.",
                                    "Vesper: The cave is sealed with winter vows and old guilt.",
                                    "Vesper: The silence here was maintained, not natural."),
                            questStage("vesper_chain_5_druid", "Question The Old Druid", "Old Hermit Druid", 1, Quest.ObjectiveKind.TALK,
                                    "goblin_camp", 2, "npc_citizen_man", null, "", "",
                                    "Vesper: If the old keeper lives, I want the truth from a breathing mouth.",
                                    "Vesper: Ask plainly. Respectfully, if possible. Firmly, if necessary.",
                                    "Vesper: They sealed the grove to save winter from green corruption.",
                                    "Vesper: I hate that I understand."),
                            questStage("vesper_chain_5_chest", "Open The Seed Chest", "Sealed Seed Chest", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 2, "icon_chest", null, "", "",
                                    "Vesper: The chest holds what they could not risk planting.",
                                    "Vesper: Open it. If hope survived in a box, it deserves fresh air.",
                                    "Vesper: The seeds are alive. Angry, patient, and alive.",
                                    "Vesper: They buried spring to save winter."),
                            questStage("vesper_chain_5_choice", "Judge The Burial", "Buried Spring Choice", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "vesper_buried_spring",
                                    "Vesper: Choose how we name what they did: mercy, cowardice, sacrifice, or fear with good intentions.",
                                    "Vesper: Do not let understanding become absolution too quickly.",
                                    "Vesper: Chosen. The old druid's silence has cracked enough to breathe through.",
                                    "Vesper: Good. We can understand a wound without worshipping it."))),
            companionStagedQuest("vesper_chain_6", "Frosthollow Roots", "The corruption feeding the grove runs through Frosthollow roots guarded by spiders.", 208, 166,
                    "vesper", "vesper_chain_7", List.of(
                            questStage("vesper_chain_6_spiders", "Clear The Root Nest", "Cave Spider", 4, Quest.ObjectiveKind.DEFEAT,
                                    "goblin_camp", 3, null, "spider", "", "",
                                    "Vesper: The cave has been drinking from the grove. We are going to make it choke.",
                                    "Vesper: Clear the spiders. They are guarding hunger, not home.",
                                    "Vesper: The nest is broken. The roots are still trembling.",
                                    "Vesper: The cave has fewer teeth now."),
                            questStage("vesper_chain_6_shards", "Gather Frozen Root Shards", "Frozen Root Shards", 3, Quest.ObjectiveKind.GATHER,
                                    "goblin_camp", 3, "quest_salve_herbs", null, "", "",
                                    "Vesper: Bring shards. Barely alive is still alive.",
                                    "Vesper: Take the pieces that still hold green. Leave the dead ice behind.",
                                    "Vesper: These roots still remember green. Barely.",
                                    "Vesper: Barely is enough."),
                            questStage("vesper_chain_6_wall", "Read The Cave Root Wall", "Cave Root Wall", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 3, "quest_ward_marker", null, "", "",
                                    "Vesper: The wall will show where the pain flows.",
                                    "Vesper: Read it like a scar, not a map.",
                                    "Vesper: The corruption runs back to the sealed spring pool.",
                                    "Vesper: Then we know where winter learned to be a cage."))),
            companionStagedQuest("vesper_chain_7", "The Buried Spring", "The sealed grove was never dead, only afraid to grow through a corrupted guardian.", 236, 188,
                    "vesper", "vesper_chain_8", List.of(
                            questStage("vesper_chain_7_pool", "Find The Sealed Spring Pool", "Sealed Spring Pool", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_ward_marker", null, "", "",
                                    "Vesper: The grove was never dead. It was afraid to grow.",
                                    "Vesper: Find the pool. Step where the mud remembers feet.",
                                    "Vesper: The pool is sealed under ice thin enough to hear.",
                                    "Vesper: Something below is still asking permission to live."),
                            questStage("vesper_chain_7_guardian", "Break The Winterroot Hollow", "Winterroot Hollow", 1, Quest.ObjectiveKind.DEFEAT,
                                    "graveyard", 2, null, "ice_golem", "", "",
                                    "Vesper: The guardian is old magic wrapped around panic.",
                                    "Vesper: Break the hollow. Not the grove. The fear holding it closed.",
                                    "Vesper: The hollow is broken. The ice is no longer pretending to be peace.",
                                    "Vesper: It was never dead. Just afraid to grow."),
                            questStage("vesper_chain_7_shoot", "Find The First Green Shoot", "First Green Shoot", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_salve_herbs", null, "", "",
                                    "Vesper: Look for green. Small is acceptable. Small is often how survival begins.",
                                    "Vesper: Bring back mud on your boots and no lies in your mouth.",
                                    "Vesper: There. One shoot. Rude enough to live.",
                                    "Vesper: Spring is not gentle. It breaks the ground to arrive."),
                            questStage("vesper_chain_7_choice", "Choose How Spring Returns", "The Buried Spring", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "vesper_spring_return",
                                    "Vesper: Choose how we wake it: quickly, carefully, publicly, or only after the roots consent.",
                                    "Vesper: Growth can harm when it forgets what it is growing through.",
                                    "Vesper: Chosen. The grove will not be forced to perform recovery.",
                                    "Vesper: That is the kind of spring I can serve."))),
            companionStagedQuest("vesper_chain_8", "Snowroot Blooms", "Vesper carries a snowroot cutting to Oathstead and chooses spring in uncertain ground.", 258, 206,
                    "vesper", null, List.of(
                            questStage("vesper_chain_8_garden", "Read Oathstead's Soil", "Oathstead Garden Soil", 1, Quest.ObjectiveKind.VISIT,
                                    "farmland", 2, "quest_ward_marker", null, "", "",
                                    "Vesper: I brought a root that survived being buried. It seemed appropriate.",
                                    "Vesper: Inspect the soil with me. A home should ask before becoming a garden.",
                                    "Vesper: The soil is tired, but not unwilling.",
                                    "Vesper: Oathstead understands tired things better than most places."),
                            questStage("vesper_chain_8_cutting", "Plant The Snowroot Cutting", "Snowroot Cutting", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_salve_herbs", null, "", "",
                                    "Vesper: Plant it shallow. Roots that survived burial deserve an easy first breath.",
                                    "Vesper: Put it where people can see that spring is work, not decoration.",
                                    "Vesper: The cutting holds. Carefully, but truly.",
                                    "Vesper: Spring has accepted uncertain ground before."),
                            questStage("vesper_chain_8_choice", "Choose The Bloom", "Vesper's Oathstead Bloom", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "vesper_oathstead_growth",
                                    "Vesper: I can stay, wander, tend, or pretend I am only checking the roots.",
                                    "Vesper: Help me choose the truth that lets things keep growing.",
                                    "Vesper: Chosen. That feels like a root finding water.",
                                    "Vesper: Spring is not gentle. It breaks the ground to arrive."))),
            companionStagedQuest("rafiq_chain_1", "The Duelist in Debt", "Rafiq's performance falters when the player inspects the debt marks and duel notices he has been joking around.", 106, 82,
                    "rafiq", "rafiq_chain_2", List.of(
                            questStage("rafiq_chain_1_notice", "Inspect The Duel Notice", "Duel Notice", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_document_bundle", null, "", "",
                                    "Rafiq: If you are here to collect, take a number. If you are here to admire, stand closer.",
                                    "Rafiq: Inspect the notice. I promise the embarrassing parts are tastefully arranged.",
                                    "Rafiq: The notice was posted after the duel was already decided.",
                                    "Rafiq: That is not justice. That is theater with blades."),
                            questStage("rafiq_chain_1_token", "Inspect The Cracked Glass Token", "Cracked Glass Token", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_glasssteel_shards", null, "", "",
                                    "Rafiq: The token cracked before I ran. It has always had better timing than I do.",
                                    "Rafiq: Hold it to the light. Glass is vain, but rarely stupid.",
                                    "Rafiq: The crack splits around a second sigil hidden under the glaze.",
                                    "Rafiq: Someone signed the duel twice, once in public and once in money."),
                            questStage("rafiq_chain_1_debt", "Read The Unpaid Debt Mark", "Unpaid Debt Mark", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_document_bundle", null, "", "",
                                    "Rafiq: The debt mark is mine. The handwriting is not. One of us is more elegant.",
                                    "Rafiq: Read it carefully. I would like my shame itemized correctly.",
                                    "Rafiq: The debt was transferred through three hands before it reached his name.",
                                    "Rafiq: Yes, the debt is mine. No, the story is not as flattering as I usually make it."),
                            questStage("rafiq_chain_1_choice", "Choose How The Debt Is Named", "Rafiq's First Debt", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "rafiq_first_debt",
                                    "Rafiq: Choose what we call this mess: debt, trap, cowardice, or a beautiful man running out of jokes.",
                                    "Rafiq: Be cruel if it helps. I have survived mirrors.",
                                    "Rafiq: Chosen. The debt has stopped performing.",
                                    "Rafiq: Good. Now I suppose I should stop performing too. Briefly."))),
            companionStagedQuest("rafiq_chain_2", "Water and Witnesses", "Rafiq learns that apology becomes believable only after the village has water, herbs, and a reason to listen.", 130, 104,
                    "rafiq", "rafiq_chain_3", List.of(
                            questStage("rafiq_chain_2_water", "Gather Water Skins", "Water Skins", 3, Quest.ObjectiveKind.GATHER,
                                    "farmland", 2, "quest_water_skins", null, "", "",
                                    "Rafiq: I have apologized beautifully. They remain thirsty. A tragic failure of performance.",
                                    "Rafiq: Bring water first. A dry throat forgives no one, even me.",
                                    "Rafiq: The water skins are full. Already more useful than my last apology.",
                                    "Rafiq: Sensible. The useful part should happen before the charming part."),
                            questStage("rafiq_chain_2_herbs", "Gather Glassstep Herbs", "Glassstep Herbs", 2, Quest.ObjectiveKind.GATHER,
                                    "farmland", 2, "quest_salve_herbs", null, "", "",
                                    "Rafiq: Glassstep herbs grow where the ground cuts your boots and your pride.",
                                    "Rafiq: Gather enough for the elders. Preferably without bleeding poetically.",
                                    "Rafiq: The herbs are bitter enough to be medicinal and moral.",
                                    "Rafiq: Good. The elders will have fewer reasons to hate me. They are inventive, though."),
                            questStage("rafiq_chain_2_elder", "Deliver Supplies To The Elders", "Dunewick Elders", 1, Quest.ObjectiveKind.DELIVER,
                                    null, 0, null, null, "rafiq", "",
                                    "Rafiq: Take the supplies to the elders with me. If I speak first, throw a waterskin at my head.",
                                    "Rafiq: Let the water and herbs arrive before my mouth does damage.",
                                    "Rafiq: They thanked you first. Sensible.",
                                    "Rafiq: You did the useful part, and I survived watching someone else be useful."),
                            questStage("rafiq_chain_2_choice", "Choose The Apology's Shape", "Dunewick Apology", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "rafiq_water_witnesses",
                                    "Rafiq: Choose what I owe them next: labor, truth, repayment, or silence long enough to be educational.",
                                    "Rafiq: I fear several of those are character-building.",
                                    "Rafiq: Chosen. I will attempt growth without making it theatrical.",
                                    "Rafiq: Good. Witnesses prefer actions that do not ask for applause."))),
            companionStagedQuest("rafiq_chain_3", "The Duel He Ran From", "The old dueling yard reveals that Rafiq fled because victory had been arranged and an opponent died anyway.", 152, 122,
                    "rafiq", "rafiq_chain_4", List.of(
                            questStage("rafiq_chain_3_yard", "Visit The Old Dueling Yard", "Old Dueling Yard", 1, Quest.ObjectiveKind.VISIT,
                                    "graveyard", 2, "quest_inspection_cache", null, "", "",
                                    "Rafiq: I did not run because I feared losing. I ran because winning had been arranged.",
                                    "Rafiq: Step into the yard. Try not to look impressed; it encourages ghosts of ego.",
                                    "Rafiq: The circle was swept after the blood dried.",
                                    "Rafiq: Someone wanted the place clean enough to lie in."),
                            questStage("rafiq_chain_3_rack", "Inspect The Broken Blade Rack", "Broken Blade Rack", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_ward_marker", null, "", "",
                                    "Rafiq: The rack held two practice blades and one murder pretending to be sport.",
                                    "Rafiq: Inspect the cuts. Wood remembers cheap tricks better than audiences do.",
                                    "Rafiq: One blade was weighted to turn badly on the third exchange.",
                                    "Rafiq: Winning had been arranged. Losing had been arranged harder."),
                            questStage("rafiq_chain_3_sand", "Read The Blood-Marked Sand", "Blood-Marked Sand", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 2, "quest_supply_cache", null, "", "",
                                    "Rafiq: The sand remembers more honestly than people. Unfortunately, sand gives terrible testimony.",
                                    "Rafiq: Search the pattern before wind and shame finish arguing.",
                                    "Rafiq: The blood falls outside the expected line of a clean duel.",
                                    "Rafiq: He died after I ran. That is the part my jokes keep stepping around."),
                            questStage("rafiq_chain_3_choice", "Choose What Running Meant", "Rigged Duel Truth", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "rafiq_rigged_duel",
                                    "Rafiq: Choose what my running meant: cowardice, refusal, survival, or a mistake made inside a trap.",
                                    "Rafiq: Do not rescue me too quickly. I am charming, not innocent.",
                                    "Rafiq: Chosen. The sand has testified as much as sand can.",
                                    "Rafiq: Good. Let us find witnesses with mouths next."))),
            companionStagedQuest("rafiq_chain_4", "Blades at Embermarket", "Hired blades attack before Rafiq can reach witnesses, and their contracts reveal who still benefits from his disgrace.", 174, 138,
                    "rafiq", "rafiq_chain_5", List.of(
                            questStage("rafiq_chain_4_blades", "Break The Hired Blades", "Hired Duelists", 3, Quest.ObjectiveKind.DEFEAT,
                                    "bandit_camp", 0, null, "bandit_cutthroat", "", "",
                                    "Rafiq: If we are attacked, please note how unfairly handsome I look under pressure.",
                                    "Rafiq: Break the hired blades. Leave one ego intact if possible; they are useful when frightened.",
                                    "Rafiq: The blades are down. Several egos have fled the scene.",
                                    "Rafiq: Good. I can pretend I planned almost none of that."),
                            questStage("rafiq_chain_4_contracts", "Recover Duel Contracts", "Duel Contracts", 3, Quest.ObjectiveKind.SEARCH,
                                    "bandit_camp", 0, "quest_document_bundle", null, "", "",
                                    "Rafiq: Search their packs. Paid murder always thinks paperwork makes it respectable.",
                                    "Rafiq: Gather the contracts before cowardice eats the signatures.",
                                    "Rafiq: These contracts smell of money, perfume, and cowardice.",
                                    "Rafiq: Embermarket's traditional bouquet."),
                            questStage("rafiq_chain_4_sash", "Find The Purple Sash Token", "Purple Sash Token", 1, Quest.ObjectiveKind.SEARCH,
                                    "bandit_camp", 0, "quest_supply_cache", null, "", "",
                                    "Rafiq: A purple sash means bad taste, worse friends, and someone expensive.",
                                    "Rafiq: Find the token. If it matches the hidden sigil, I will become insufferably correct.",
                                    "Rafiq: It matches. I am correct and trying to be tasteful about it.",
                                    "Rafiq: The same patron bought the duel and the silence after it."),
                            questStage("rafiq_chain_4_choice", "Choose How To Use The Contracts", "Embermarket Contracts", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "rafiq_embermarket_contracts",
                                    "Rafiq: Choose our move: expose the patron, protect witnesses, bait the buyer, or pay the debt with the truth.",
                                    "Rafiq: Revenge would be prettier. Annoyingly, justice may last longer.",
                                    "Rafiq: Chosen. I will pretend I prefer the responsible option.",
                                    "Rafiq: Good. The contracts have learned to accuse."))),
            companionStagedQuest("rafiq_chain_5", "The Glass Debt", "Rafiq's debt points back to a glassmaker, a broken mirror, and the sister he tried to buy time for.", 196, 156,
                    "rafiq", "rafiq_chain_6", List.of(
                            questStage("rafiq_chain_5_house", "Visit The Glassmaker's House", "Glassmaker's House", 1, Quest.ObjectiveKind.VISIT,
                                    "farmland", 2, "quest_inspection_cache", null, "", "",
                                    "Rafiq: I prefer lies where I am selfish. They are easier to laugh at.",
                                    "Rafiq: Visit the glassmaker's house. Be gentle with the truth; it is badly dressed.",
                                    "Rafiq: The house is empty except for heat marks and unpaid patience.",
                                    "Rafiq: The debt has a name other than mine now. Several, unfortunately."),
                            questStage("rafiq_chain_5_order", "Inspect The Unpaid Glass Order", "Unpaid Glass Order", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_document_bundle", null, "", "",
                                    "Rafiq: The order was for a bridal mirror, a freedom token, and enough lies to survive a banquet.",
                                    "Rafiq: Read it. I bought time, not dignity.",
                                    "Rafiq: The order was rushed and paid with borrowed dueling credit.",
                                    "Rafiq: Time is what poor people call hope when sold in pieces."),
                            questStage("rafiq_chain_5_mirror", "Inspect The Cracked Mirror", "Cracked Mirror", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_glasssteel_shards", null, "", "",
                                    "Rafiq: The mirror cracked before delivery. A dramatic object. We understand one another.",
                                    "Rafiq: Inspect the backing. Glassmakers hide messages where vain people refuse to look.",
                                    "Rafiq: The backing names my sister's contract holder.",
                                    "Rafiq: I was not buying escape. I was buying a delay before a cage closed."),
                            questStage("rafiq_chain_5_choice", "Choose What The Debt Was For", "Rafiq's Sister Letter", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "rafiq_glass_debt",
                                    "Rafiq: Choose what this debt means: sacrifice, foolishness, love with bad accounting, or a debt no one should have been forced to pay.",
                                    "Rafiq: Please avoid making me noble. I itch.",
                                    "Rafiq: Chosen. I bought time. Not freedom.",
                                    "Rafiq: Good. Now we must decide what time was for."))),
            companionStagedQuest("rafiq_chain_6", "Second Chance, Sharp Edge", "Rafiq earns the repair of his blade by gathering glasssteel and clearing the road before facing the one who framed him.", 218, 174,
                    "rafiq", "rafiq_chain_7", List.of(
                            questStage("rafiq_chain_6_shards", "Gather Glasssteel Shards", "Glasssteel Shards", 3, Quest.ObjectiveKind.GATHER,
                                    "goblin_camp", 5, "quest_glasssteel_shards", null, "", "",
                                    "Rafiq: A second chance should be earned with sweat, danger, and preferably an excellent blade.",
                                    "Rafiq: Gather glasssteel. Try not to admire your reflection unless it improves.",
                                    "Rafiq: The shards are clean enough to cut and honest enough to hurt.",
                                    "Rafiq: Good. The blade may yet forgive my hand."),
                            questStage("rafiq_chain_6_raiders", "Clear The Badlands Road", "Orc Raider", 3, Quest.ObjectiveKind.DEFEND,
                                    "goblin_camp", 5, null, "orc_raider", "", "",
                                    "Rafiq: Raiders have been admiring the glasssteel. With axes. Tasteless.",
                                    "Rafiq: Remove them before the road learns another bad habit.",
                                    "Rafiq: The road is open. The raiders are less decorative now.",
                                    "Rafiq: Good. Second chances dislike witnesses who stab them."),
                            questStage("rafiq_chain_6_blade", "Repair The Curved Blade", "Repaired Curved Blade", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_glasssteel_shards", null, "", "",
                                    "Rafiq: Now the repair. If I shake, kindly pretend it is artistic tension.",
                                    "Rafiq: Set the shard into the curve. The blade and I both need to look honest.",
                                    "Rafiq: The edge holds. It reflects badly, which is how I know it is accurate.",
                                    "Rafiq: There. Sharp again. The sword too."),
                            questStage("rafiq_chain_6_choice", "Choose The Second Chance", "Sharp Second Chance", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "rafiq_second_chance",
                                    "Rafiq: Choose what this chance is for: clearing my name, freeing my sister, facing Nadim, or becoming useful before forgiven.",
                                    "Rafiq: I was hoping for all of them with better hair.",
                                    "Rafiq: Chosen. The blade heard you. Very rude of it.",
                                    "Rafiq: Good. Let us stop rehearsing and arrive."))),
            companionStagedQuest("rafiq_chain_7", "The Glassstep Duel", "Rafiq faces Nadim's rigged circle and chooses quiet truth over applause or revenge.", 246, 196,
                    "rafiq", "rafiq_chain_8", List.of(
                            questStage("rafiq_chain_7_shrine", "Visit The Glass Shrine", "Glass Shrine Dueling Circle", 1, Quest.ObjectiveKind.VISIT,
                                    "goblin_camp", 5, "quest_ward_marker", null, "", "",
                                    "Rafiq: Charm does not parry, he says. He has clearly met poorer charm.",
                                    "Rafiq: Meet him at the glass circle. Let the witness stone hear the ending.",
                                    "Rafiq: The circle is polished enough to flatter a lie.",
                                    "Rafiq: Good. Let us scratch it."),
                            questStage("rafiq_chain_7_circle", "Inspect The Circle Of Glass", "Circle Of Glass", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 5, "quest_glasssteel_shards", null, "", "",
                                    "Rafiq: The circle was built to catch every stumble and call it destiny.",
                                    "Rafiq: Inspect the footing. Nadim prefers fate when he has oiled the floor.",
                                    "Rafiq: There. The west arc was dusted to turn a quick step into a fall.",
                                    "Rafiq: Even destiny needs maintenance, apparently."),
                            questStage("rafiq_chain_7_nadim", "Defeat Nadim's Champion", "Nadim Of The Shattered Step", 1, Quest.ObjectiveKind.DEFEAT,
                                    "bandit_camp", 0, null, "bandit_captain", "", "",
                                    "Rafiq: Nadim says a broken reputation cuts deeper. Let us test his medical theory.",
                                    "Rafiq: Fight clean enough that the witness stone gets bored with excuses.",
                                    "Rafiq: Nadim is down. The circle has lost its favorite liar.",
                                    "Rafiq: I wanted applause. Then revenge. Now I mostly want quiet. Disturbing growth."),
                            questStage("rafiq_chain_7_choice", "Choose The Witness Stone Record", "Glassstep Witness Stone", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "rafiq_glassstep_duel",
                                    "Rafiq: Choose what the stone records: Nadim's guilt, the rigged duel, my running, or the whole ugly arrangement.",
                                    "Rafiq: Do not carve me prettier than I was. The stone deserves better lighting.",
                                    "Rafiq: Chosen. If the duel is over, let the ending be useful.",
                                    "Rafiq: Good. Applause can go infect someone else."))),
            companionStagedQuest("rafiq_chain_8", "Glass Remembers Light", "Rafiq places his repaired glass charm at Oathstead and chooses a second chance built by action.", 268, 214,
                    "rafiq", null, List.of(
                            questStage("rafiq_chain_8_blade", "Inspect The Repaired Blade", "Repaired Curved Blade", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_glasssteel_shards", null, "", "",
                                    "Rafiq: Oathstead is muddy, badly dressed, and honest. I am trying not to take that personally.",
                                    "Rafiq: Inspect the blade with me. I want this promise to cut clean.",
                                    "Rafiq: The repair shows. I find this personally insulting and thematically useful.",
                                    "Rafiq: It will hold because we stopped pretending it was never broken."),
                            questStage("rafiq_chain_8_charm", "Place The Glass Charm", "Repaired Glass Charm", 1, Quest.ObjectiveKind.VISIT,
                                    "farmland", 2, "quest_glasssteel_shards", null, "", "",
                                    "Rafiq: The charm goes near the oath marker. Somewhere it can catch light without becoming a sermon.",
                                    "Rafiq: Place it carefully. Glass remembers light, and apparently I remember consequences.",
                                    "Rafiq: The charm catches the light without hiding the crack.",
                                    "Rafiq: Honest glass. Dangerous precedent."),
                            questStage("rafiq_chain_8_choice", "Choose The Second Chance Built Here", "Rafiq's Oathstead Chance", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "rafiq_oathstead_chance",
                                    "Rafiq: Choose what I become here: dueling teacher, debt witness, blade at the gate, or a man who stays before he deserves it.",
                                    "Rafiq: I will accept any answer except 'decorative.' I would accept it beautifully, but no.",
                                    "Rafiq: Chosen. I will stay.",
                                    "Rafiq: Not because I deserve it. Because I intend to."))),
            companionStagedQuest("calder_chain_1", "The Bridge That Complained", "Calder teaches the player to read a bridge before it collapses and names neglect as a kind of danger.", 92, 72,
                    "calder", "calder_chain_2", List.of(
                            questStage("calder_chain_1_beam", "Inspect The Cracked Beam", "Cracked Bridge Beam", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_causeway_stones", null, "", "",
                                    "Calder: Bridge is talking. Mostly insults. Means it is close to falling.",
                                    "Calder: Look at the beam first. Wood complains before it kills someone.",
                                    "Calder: Crack runs along old rot, then fresh strain. Bad repair on worse patience.",
                                    "Calder: See that crack? That is what happens when people call maintenance expensive."),
                            questStage("calder_chain_1_footing", "Check The Loose Footing", "Loose Stone Footing", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_ward_marker", null, "", "",
                                    "Calder: Footing next. People look at planks because planks make noise.",
                                    "Calder: Check the stones. Quiet things hold the loud things up.",
                                    "Calder: The west footing sank two fingers since last flood.",
                                    "Calder: Bridge is not weak. It is being asked to lie."),
                            questStage("calder_chain_1_mudline", "Read The Mudline", "Bridge Mudline", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_inspection_cache", null, "", "",
                                    "Calder: Mudline tells you where water argued and won.",
                                    "Calder: Read it before rain edits the confession.",
                                    "Calder: Water climbed past the warning notch three times.",
                                    "Calder: The bridge explained who ignored it. People, mostly."),
                            questStage("calder_chain_1_choice", "Choose What The Bridge Says", "Bridge Warning", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "calder_bridge_warning",
                                    "Calder: Choose what this bridge is saying: neglect, bad craft, short coin, or a warning nobody wanted to pay for.",
                                    "Calder: Say it plain. Pretty words do not hold beams.",
                                    "Calder: Chosen. You have heard enough wood complain.",
                                    "Calder: Good. Now we fix before we explain."))),
            companionStagedQuest("calder_chain_2", "Tools Before Talk", "Calder refuses prophecy and politics until the repair has timber, nails, rope, and hands willing to work.", 118, 94,
                    "calder", "calder_chain_3", List.of(
                            questStage("calder_chain_2_timber", "Gather Straight Timber", "Straight Timber", 3, Quest.ObjectiveKind.GATHER,
                                    "farmland", 1, "quest_firewood_bundle", null, "", "",
                                    "Calder: You want answers? Bring nails. Answers without nails do not hold weight.",
                                    "Calder: Timber first. Straight if possible. Honest if not.",
                                    "Calder: Timber is dry enough and stubborn enough.",
                                    "Calder: Good. Now hope has something less stupid to lean on."),
                            questStage("calder_chain_2_nails", "Gather Iron Nails", "Iron Nails", 2, Quest.ObjectiveKind.GATHER,
                                    "farmland", 1, "quest_supply_cache", null, "", "",
                                    "Calder: Nails. Small things. Everyone notices when they fail.",
                                    "Calder: Bring enough to fasten the repair and a few more for human optimism.",
                                    "Calder: These will bite properly.",
                                    "Calder: Good. Small work keeps big work from making speeches."),
                            questStage("calder_chain_2_rope", "Gather Rope Coils", "Rope Coils", 2, Quest.ObjectiveKind.GATHER,
                                    "farmland", 1, "quest_inspection_cache", null, "", "",
                                    "Calder: Rope holds by admitting it needs other strands.",
                                    "Calder: Gather coils that do not creak like excuses.",
                                    "Calder: The rope is good. Rough, tight, no vanity.",
                                    "Calder: Fine rope. It knows its job."),
                            questStage("calder_chain_2_choice", "Choose The Work Order", "Bridge Repair Order", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "calder_tools_before_talk",
                                    "Calder: Choose the order: brace first, replace first, lighten the load, or close the bridge until it stops trying to kill people.",
                                    "Calder: Convenient is not the same as sound.",
                                    "Calder: Chosen. Your hands are carrying something real now.",
                                    "Calder: Good. Now we can talk about less useful things."))),
            companionStagedQuest("calder_chain_3", "The Weight Test", "Calder load-tests the repaired bridge and shows that trust is not a feeling until it has carried weight.", 142, 114,
                    "calder", "calder_chain_4", List.of(
                            questStage("calder_chain_3_beam", "Inspect The Repaired Beam", "Repaired Bridge Beam", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_causeway_stones", null, "", "",
                                    "Calder: Everything breaks. Good craft just tells you before it kills someone.",
                                    "Calder: Check the repaired beam. Praise it later if it earns that nonsense.",
                                    "Calder: The repair flexes without splitting.",
                                    "Calder: Good. It is not lying yet."),
                            questStage("calder_chain_3_supports", "Check The Rope Supports", "Rope Supports", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_inspection_cache", null, "", "",
                                    "Calder: Rope supports share strain if people let them.",
                                    "Calder: Check every knot. Trust should be load-tested.",
                                    "Calder: The knots pulled tight and held.",
                                    "Calder: That will do. Not forever. Nothing does."),
                            questStage("calder_chain_3_anchors", "Inspect The Stone Anchors", "Stone Anchors", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_ward_marker", null, "", "",
                                    "Calder: Anchors decide whether the whole clever thing becomes river trash.",
                                    "Calder: Inspect them. Stone is patient until insulted.",
                                    "Calder: The east anchor has settled into clean weight.",
                                    "Calder: This will hold long enough if people keep caring."),
                            questStage("calder_chain_3_choice", "Choose The Maintenance Rule", "Bridge Maintenance Rule", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "calder_weight_test",
                                    "Calder: Choose the rule: inspect after flood, limit loads, train locals, or write warnings people cannot pretend not to see.",
                                    "Calder: A bridge survives by being checked after everyone stops clapping.",
                                    "Calder: Chosen. The bridge has stopped lying for now.",
                                    "Calder: Good. Long enough is a kind of honest."))),
            companionStagedQuest("calder_chain_4", "Stones From the Old Causeway", "Calder gathers old causeway stones and clears the marsh road because strong repair needs material with memory.", 166, 132,
                    "calder", "calder_chain_5", List.of(
                            questStage("calder_chain_4_stones", "Gather Causeway Stones", "Causeway Stones", 4, Quest.ObjectiveKind.GATHER,
                                    "graveyard", 1, "quest_causeway_stones", null, "", "",
                                    "Calder: Old stones are better than new lies. Heavy, though.",
                                    "Calder: Bring stones set by people who expected grandchildren.",
                                    "Calder: These are dense, square, and smug. Good stone.",
                                    "Calder: Proper arrogance. I respect it."),
                            questStage("calder_chain_4_beasts", "Clear The Marsh Road", "Bog Beast", 3, Quest.ObjectiveKind.DEFEAT,
                                    "goblin_camp", 3, null, "bog_beast", "", "",
                                    "Calder: Marsh has teeth today. It is allowed opinions, not ownership.",
                                    "Calder: Move anything with teeth off the causeway.",
                                    "Calder: The road is quieter. Still damp. Cannot fix everything.",
                                    "Calder: Good. The marsh can complain from a distance."),
                            questStage("calder_chain_4_foundation", "Inspect The Old Foundation", "Old Causeway Foundation", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_ward_marker", null, "", "",
                                    "Calder: Old foundation next. Want to know what it thought it was holding.",
                                    "Calder: Inspect the bedding stones. Builders leave fingerprints in weight.",
                                    "Calder: The foundation was made for heavier traffic than Mireford remembers.",
                                    "Calder: Someone here planned beyond themselves. Rare. Useful."),
                            questStage("calder_chain_4_choice", "Choose How Old Stone Is Used", "Old Causeway Stone", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "calder_causeway_stones",
                                    "Calder: Choose where these stones go: bridge footings, flood wall, road anchors, or Oathstead's future repairs.",
                                    "Calder: Good material should solve more than one fool mistake.",
                                    "Calder: Chosen. Return before the marsh decides it owns the road.",
                                    "Calder: Good. Old stone gets another job."))),
            companionStagedQuest("calder_chain_5", "The Bridge He Lost", "Calder returns to the collapsed flood bridge and asks the player not to rescue him from blame before the facts are known.", 190, 152,
                    "calder", "calder_chain_6", List.of(
                            questStage("calder_chain_5_bridge", "Visit The Collapsed Flood Bridge", "Collapsed Flood Bridge", 1, Quest.ObjectiveKind.VISIT,
                                    "graveyard", 1, "quest_inspection_cache", null, "", "",
                                    "Calder: I built this. Then it fell. Both facts matter.",
                                    "Calder: Visit the collapse. Do not rescue me from blame.",
                                    "Calder: The river still hits the gap like it has a grudge.",
                                    "Calder: Fair. I would too."),
                            questStage("calder_chain_5_support", "Inspect The Broken Support Stone", "Broken Support Stone", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_causeway_stones", null, "", "",
                                    "Calder: Support stone failed first. Or confessed first. Same sometimes.",
                                    "Calder: Inspect the break. Stone breaks honest if you know where to look.",
                                    "Calder: The stone split around a vein I should have rejected.",
                                    "Calder: Small mistake. Big river. Dead people."),
                            questStage("calder_chain_5_plank", "Read The Memorial Plank", "Memorial Plank", 1, Quest.ObjectiveKind.SEARCH,
                                    "graveyard", 1, "quest_document_bundle", null, "", "",
                                    "Calder: The memorial plank has names. Names are load too.",
                                    "Calder: Read them. No skipping because grief is inconvenient.",
                                    "Calder: Twelve names. Three were children.",
                                    "Calder: I remember the number. I do not deserve to forget it."),
                            questStage("calder_chain_5_choice", "Choose What Blame Carries", "Lost Bridge Blame", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "calder_lost_bridge",
                                    "Calder: Choose what this place says: my fault, bad material, ignored warning, or a whole village pretending need made risk vanish.",
                                    "Calder: Do not smooth it. Smooth stone slips.",
                                    "Calder: Chosen. The bridge has told the whole ugly thing.",
                                    "Calder: Good. Now we look at records, not mercy."))),
            companionStagedQuest("calder_chain_6", "Blame Has Bad Mortar", "Records show Calder was denied proper material and signed anyway, making truth heavier than innocence.", 214, 170,
                    "calder", "calder_chain_7", List.of(
                            questStage("calder_chain_6_ledger", "Inspect The Flood Repair Ledger", "Flood Repair Ledger", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_document_bundle", null, "", "",
                                    "Calder: Do not look for proof to make me innocent. Look for proof to make the bridge make sense.",
                                    "Calder: Read the ledger. Truth has weight; lift properly.",
                                    "Calder: The stone order was cut in half two days before repair.",
                                    "Calder: There. One ugly hand."),
                            questStage("calder_chain_6_supply", "Find The Missing Supply Entry", "Missing Supply Entry", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_supply_cache", null, "", "",
                                    "Calder: Missing supplies leave holes. Holes are very honest.",
                                    "Calder: Find the entry someone wanted the river to wash clean.",
                                    "Calder: The best stone was sold upriver and replaced with cheaper breakage.",
                                    "Calder: They shorted the stone."),
                            questStage("calder_chain_6_complaint", "Open The Sealed Complaint", "Sealed Village Complaint", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 1, "quest_document_bundle", null, "", "",
                                    "Calder: Complaint was sealed because officials dislike being useful after the fact.",
                                    "Calder: Open it. Let paper stop pretending.",
                                    "Calder: The village warned the footing was sinking. I was never shown this.",
                                    "Calder: I signed anyway. Truth has two ugly hands."),
                            questStage("calder_chain_6_choice", "Choose The Repair Of Blame", "Bad Mortar Truth", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "calder_bad_mortar",
                                    "Calder: Choose what gets repaired first: public record, bridge fund, families owed, or my habit of carrying all the weight alone.",
                                    "Calder: Careful. Innocence is not the goal. Sense is.",
                                    "Calder: Chosen. The records have stopped pretending.",
                                    "Calder: Good. Blame set correctly can hold repair in place."))),
            companionStagedQuest("calder_chain_7", "The Stonebreaker's Span", "Calder defends a restored span from the marsh and proves repair can hold under real pressure.", 244, 194,
                    "calder", "calder_chain_8", List.of(
                            questStage("calder_chain_7_span", "Inspect The Damaged Span", "Damaged Causeway Span", 1, Quest.ObjectiveKind.VISIT,
                                    "goblin_camp", 3, "quest_inspection_cache", null, "", "",
                                    "Calder: Something is testing the span. I dislike being tested by teeth.",
                                    "Calder: Inspect the damage before we start swinging. Wood first, teeth second.",
                                    "Calder: The span is bruised, not broken.",
                                    "Calder: Good. Bruised things still carry if you stop the biting."),
                            questStage("calder_chain_7_beasts", "Drive Off The Bog Beasts", "Bog Beast", 3, Quest.ObjectiveKind.DEFEND,
                                    "goblin_camp", 3, null, "bog_beast", "", "",
                                    "Calder: Beasts are chewing at the supports. Rude engineering critique.",
                                    "Calder: Clear them without trampling the brace work.",
                                    "Calder: Teeth gone. Braces still standing.",
                                    "Calder: That is the kind of victory I like. Useful and quiet."),
                            questStage("calder_chain_7_support", "Check The Central Support", "Restored Central Support", 1, Quest.ObjectiveKind.SEARCH,
                                    "goblin_camp", 3, "quest_causeway_stones", null, "", "",
                                    "Calder: Central support decides whether everyone gets home dry or remembered.",
                                    "Calder: Check it. If the support stands, we breathe.",
                                    "Calder: It settled deeper under stress, exactly as planned.",
                                    "Calder: Bridge held. So did we. I will not make poetry of it."),
                            questStage("calder_chain_7_choice", "Choose What The Span Proves", "Stonebreaker's Span", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "calder_stonebreaker_span",
                                    "Calder: Choose what this proves: good craft, shared weight, second repair, or that fear can be useful if it checks the supports.",
                                    "Calder: Pride kills bridges. So does despair.",
                                    "Calder: Chosen. The support stands.",
                                    "Calder: Good. Standing is answer enough."))),
            companionStagedQuest("calder_chain_8", "What Holds", "Calder reinforces Oathstead and chooses a place worth building because it is honest about leaning.", 266, 212,
                    "calder", null, List.of(
                            questStage("calder_chain_8_gate", "Inspect The Reinforced Gate", "Oathstead Reinforced Gate", 1, Quest.ObjectiveKind.VISIT,
                                    "farmland", 2, "quest_ward_marker", null, "", "",
                                    "Calder: This camp leans. Badly. But it leans toward standing.",
                                    "Calder: Inspect the reinforced gate. I want to know what we are asking it to hold.",
                                    "Calder: Gate is ugly. Hinges are honest. I approve halfway.",
                                    "Calder: Oathstead knows it needs repair. Better than most cities."),
                            questStage("calder_chain_8_plank", "Inspect The New Bridge Plank", "Oathstead Bridge Plank", 1, Quest.ObjectiveKind.SEARCH,
                                    "farmland", 2, "quest_causeway_stones", null, "", "",
                                    "Calder: New plank by the low crossing. Small thing. Small things drown people too.",
                                    "Calder: Check the grain and pins. This promise should carry muddy boots before noble ideas.",
                                    "Calder: Plank holds. Pins bite. Mud remains opinionated.",
                                    "Calder: Good. Work is honest."),
                            questStage("calder_chain_8_choice", "Choose What Holds At Oathstead", "Calder's Oathstead Work", 1, Quest.ObjectiveKind.CHOICE,
                                    null, 0, null, null, "", "calder_oathstead_work",
                                    "Calder: Choose what I build here: gate, bridge, workshop, or people who check each other's weight before the flood.",
                                    "Calder: Not perfect. Strong enough. That is most of life.",
                                    "Calder: Chosen. I can build here.",
                                    "Calder: Not perfect. Strong enough. That is most of life.")))
    );

    public static final Map<String, Item> ITEMS = ItemCatalog.items();

    public static final Map<String, Equipment> EQUIPMENT = EquipmentCatalog.equipmentItems();

    public static final Map<String, StatusEffect> STATUS_EFFECTS = Map.ofEntries(
            Map.entry("poison", new StatusEffect("Poison", "poison", 3, 6, "Takes damage each turn", "debuff")),
            Map.entry("weak", new StatusEffect("Weak", "weak", 2, 0, "Damage reduced by 30%", "debuff")),
            Map.entry("vulnerable", new StatusEffect("Vulnerable", "vulnerable", 2, 0, "Takes 25% more damage", "debuff")),
            Map.entry("fortified", new StatusEffect("Fortified", "fortified", 2, 0, "Damage reduced by 25%", "buff")),
            Map.entry("haste", new StatusEffect("Haste", "haste", 3, 0, "Basic attacks hit twice", "buff")),
            Map.entry("burn", new StatusEffect("Burn", "burn", 4, 8, "Takes more damage each turn", "debuff")),
            Map.entry("regeneration", new StatusEffect("Regeneration", "regeneration", 3, 4, "Recovers HP each turn", "buff")),
            Map.entry("shield", new StatusEffect("Shield", "shield", 2, 12, "Blocks incoming damage", "buff"))
    );

    public static final Map<String, String> STATUS_ICON_LABELS = Map.of(
            "poison", "PS",
            "weak", "WK",
            "vulnerable", "VN",
            "fortified", "FT",
            "haste", "HS",
            "burn", "BR",
            "regeneration", "RG",
            "shield", "SH"
    );

    public static final Map<String, Shop> SHOPS = Map.ofEntries(
            Map.entry("riverside", new Shop("riverside", "Riverside Market", List.of(
                    "potion_small", "ether", "guard_tonic", "escape_scroll", "traveler_cloak", "iron_sword", "iron_ring", "silver_mace",
                    "scout_hood", "road_belt", "trail_leggings",
                    "rustbit_saber", "riverguard_blade", "cinderedge", "frostvein_sword", "thorncarver",
                    "moonlit_rapier", "sunfall_longsword", "stormglass_blade", "duskwake_sword", "kingsroad_claymore",
                    "marshlight_cutlass", "obsidian_fang", "starforged_sword", "heartstone_blade", "oathbreaker_edge",
                    "oakheart_staff", "ashwind_staff", "embercore_staff", "frostroot_staff", "stormcall_staff",
                    "moonwell_staff", "sunspire_staff", "thornbinder_staff", "bogsong_staff", "glassdune_staff",
                    "archive_scepter", "starfall_staff", "heartstone_crook", "voidglass_staff", "dawnweave_staff",
                    "padded_gambeson", "scout_leathers", "ranger_jerkin", "duelist_jacket", "nightweave_coat",
                    "marshrunner_mantle", "dunewrap_vest", "frostleaf_cloak", "thornsilk_armor", "stormhide_jacket",
                    "suncloth_mantle", "archive_mantle", "starweave_robes", "moonwater_coat", "heartwood_vest",
                    "copper_signet", "river_pearl_charm", "emberglass_ring", "frostdrop_pendant", "stormbead_brooch",
                    "sunward_medallion", "moonthread_necklace", "thornroot_charm", "marshlight_seal", "dunestar_talisman",
                    "archive_lens", "shadowband", "heartstone_locket", "starrelic_ring", "phoenix_crown_pin",
                    "recipe_book_camp_cookery", "recipe_book_fisher_knots", "recipe_book_oak_bow", "recipe_book_apothecary_salve",
                    "new_weapon_ashfall_cutlass", "new_weapon_briarhook_dagger", "new_weapon_militia_pike", "new_weapon_willow_shortbow", "new_weapon_apprentice_wand",
                    "new_weapon_copper_warpick", "new_weapon_roadwatch_mace", "new_weapon_field_sickle", "new_weapon_emberglass_saber", "new_weapon_frostpine_bow",
                    "new_weapon_stormthread_wand", "new_weapon_moonlit_dirk", "new_weapon_oakwarden_spear", "new_weapon_sunspoke_mace", "new_weapon_gravesalt_flail",
                    "new_weapon_duskneedle_rapier", "new_armor_mended_linen_hood", "new_armor_roadmender_gloves", "new_armor_copper_thread_belt", "new_armor_patchwork_greaves",
                    "new_armor_ashguard_boots", "new_armor_willowhide_jerkin", "new_armor_tin_pauldrons", "new_armor_raincloak_mantle", "new_armor_briarwatch_helm",
                    "new_armor_embersewn_gloves", "new_armor_frostfelt_sash", "new_armor_stormhide_boots", "new_armor_moonbuckled_leggings", "new_armor_oaken_pauldrons",
                    "new_armor_sunwashed_robe", "new_armor_gravesalt_cowl", "new_accessory_tin_luck_ring", "new_accessory_ash_cord_charm", "new_accessory_riverglass_bead",
                    "new_accessory_copper_prayer_loop", "new_accessory_wayfarer_buckle", "new_accessory_willowseed_pendant", "new_accessory_small_storm_brooch", "new_accessory_moonmilk_pearl",
                    "new_accessory_emberglass_signet", "new_accessory_frostpine_talisman", "new_accessory_stormthread_torque", "new_accessory_moonlit_ear_cuff", "new_accessory_oakwarden_knot",
                    "new_accessory_sunspoke_medal", "new_accessory_gravesalt_rosary", "new_accessory_duskneedle_band", "new_potion_minor_redcap_draught", "new_potion_clearwater_flask",
                    "new_potion_mint_ether_sip", "new_potion_bandage_balm", "new_potion_salted_travel_broth", "new_potion_copperleaf_tonic", "new_potion_blue_candle_tea",
                    "new_potion_thornbite_antidote", "new_potion_emberwarm_elixir", "new_potion_frostwake_cordial", "new_potion_stormbreath_tonic", "new_potion_moonmilk_salve",
                    "new_potion_oakskin_decoction", "new_potion_sunspoke_remedy", "new_potion_gravesalt_cleanser", "new_potion_duskstep_smoke_vial"
            ))),
            Map.entry("highwall", new Shop("highwall", "Highwall Quartermaster", List.of(
                    "potion_small", "potion_large", "ether", "battle_kit", "escape_scroll", "steel_sword", "iron_mail", "ranger_coat",
                    "shadow_dagger", "iron_helm", "guard_gauntlets", "iron_boots", "steel_greaves",
                    "woodcutter_axe", "raider_hatchet", "ironbeard_axe", "steelcleaver", "frosthew_axe",
                    "embermaul_axe", "stormsplitter", "thornbite_axe", "bonehook_axe", "obsidian_chopper",
                    "sunward_axe", "marshreaper", "mountainfall_axe", "royal_halberd_axe", "starbreaker_axe",
                    "river_buckler", "oaken_roundshield", "iron_kite_shield", "towerguard_shield", "frostguard_aegis",
                    "emberward_shield", "stormwall_shield", "suncrest_shield", "shadowglass_shield", "thornwall_shield",
                    "marshreed_ward", "bonebound_shield", "royal_heater", "starforged_aegis", "heartstone_bulwark",
                    "riveted_mail", "guard_cuirass", "steel_bastion_plate", "mountain_plate", "boneguard_plate",
                    "obsidian_plate", "frostbound_plate", "emberforged_plate", "stormguard_plate", "sunwarden_plate",
                    "shadowplate_harness", "thornplate_mail", "marshbulwark_plate", "royal_wardplate", "starforged_plate",
                    "recipe_book_iron_blades", "recipe_book_iron_mail", "recipe_book_steel_plate",
                    "new_weapon_cobalt_halberd", "new_weapon_redreef_axe", "new_weapon_archive_thornstaff", "new_weapon_rimehook_blade", "new_weapon_lanternfall_bow",
                    "new_weapon_widowglass_kris", "new_weapon_thunderroot_maul", "new_weapon_mirechant_staff", "new_weapon_kingsmark_glaive", "new_weapon_hearthflame_falchion",
                    "new_weapon_starshard_wand", "new_weapon_nightwater_trident", "new_armor_cobalt_mail", "new_armor_redreef_waders", "new_armor_archive_shoulderplates",
                    "new_armor_rimeguard_helm", "new_armor_lanternscale_coat", "new_armor_widowglass_gauntlets", "new_armor_thunderroot_girdle", "new_armor_mirechant_waders",
                    "new_armor_kingsmark_sabatons", "new_armor_hearthflame_cuirass", "new_armor_starshard_hood", "new_armor_nightwater_mantle", "new_accessory_cobalt_lens",
                    "new_accessory_redreef_shell_charm", "new_accessory_archive_pageweight", "new_accessory_rimehook_locket", "new_accessory_lanternfall_reliquary", "new_accessory_widowglass_ring",
                    "new_accessory_thunderroot_idol", "new_accessory_mirechant_seal", "new_accessory_kingsmark_circlet", "new_accessory_hearthflame_chain", "new_accessory_starshard_astrolabe",
                    "new_accessory_nightwater_signet", "new_potion_cobalt_focus_phial", "new_potion_redreef_brine", "new_potion_archive_ink_tonic", "new_potion_rimehook_liniment",
                    "new_potion_lanternfall_restorative", "new_potion_widowglass_antivenom", "new_potion_thunderroot_charge", "new_potion_mirechant_poultice", "new_potion_kingsmark_battle_kit",
                    "new_potion_hearthflame_reviver", "new_potion_starshard_ether", "new_potion_nightwater_draught"
            ))),
            Map.entry("crypt_vendor", new Shop("crypt_vendor", "Crypt Provisioner", List.of(
                    "potion_large", "ether", "guard_tonic", "battle_kit", "escape_scroll", "flame_staff", "acolyte_mantle",
                    "night_leathers", "warden_plate", "phoenix_feather", "focus_sash", "softstep_boots", "warded_pauldrons",
                    "stormglass_blade", "duskwake_sword", "kingsroad_claymore", "marshlight_cutlass", "obsidian_fang",
                    "starforged_sword", "heartstone_blade", "oathbreaker_edge",
                    "stormcall_staff", "moonwell_staff", "sunspire_staff", "thornbinder_staff", "bogsong_staff",
                    "glassdune_staff", "archive_scepter", "starfall_staff", "heartstone_crook", "voidglass_staff",
                    "dawnweave_staff", "sunward_axe", "marshreaper", "mountainfall_axe", "royal_halberd_axe",
                    "starbreaker_axe", "shadowglass_shield", "thornwall_shield", "marshreed_ward", "bonebound_shield",
                    "royal_heater", "starforged_aegis", "heartstone_bulwark", "suncloth_mantle", "archive_mantle",
                    "starweave_robes", "moonwater_coat", "heartwood_vest", "shadowplate_harness", "thornplate_mail",
                    "marshbulwark_plate", "royal_wardplate", "starforged_plate", "archive_lens", "shadowband",
                    "heartstone_locket", "starrelic_ring", "phoenix_crown_pin", "recipe_book_escape_scrolls",
                    "new_weapon_the_bellringer", "new_weapon_glass_choir_scepter", "new_weapon_oathroot_cleaver", "new_weapon_vesper_thornbow", "new_weapon_samir_sunlance",
                    "new_weapon_cassia_gatehammer", "new_weapon_maera_marginblade", "new_weapon_lyra_mercy_staff", "new_weapon_dawn_crown_brand", "new_weapon_worldroot_scythe",
                    "new_weapon_alderfall_starcutter", "new_weapon_the_twelfth_silence", "new_armor_bellringer_plate", "new_armor_glass_choir_robes", "new_armor_oathroot_pauldrons",
                    "new_armor_vesper_thornveil", "new_armor_samir_sunmail", "new_armor_cassia_gateplate", "new_armor_maera_marginsash", "new_armor_lyra_mercywraps",
                    "new_armor_dawn_crown_regalia", "new_armor_worldroot_carapace", "new_armor_alderfall_starplate", "new_armor_mantle_of_the_twelfth", "new_accessory_the_bellringer_clapper",
                    "new_accessory_glass_choir_halo", "new_accessory_oathroot_promise_band", "new_accessory_vesper_thornheart", "new_accessory_samir_sun_index", "new_accessory_cassia_gate_sigil",
                    "new_accessory_maera_margin_lens", "new_accessory_lyra_mercybell", "new_accessory_dawn_crown_phoenix_pin", "new_accessory_worldroot_seedstone", "new_accessory_alderfall_starrelic",
                    "new_accessory_seal_of_the_twelfth", "new_potion_bellringer_incense", "new_potion_glass_choir_serum", "new_potion_oathroot_fortifier", "new_potion_vesper_thorn_salve",
                    "new_potion_samir_sunbrew", "new_potion_cassia_gate_tonic", "new_potion_maera_margin_tea", "new_potion_lyra_mercy_vial", "new_potion_dawn_crown_elixir",
                    "new_potion_worldroot_panacea", "new_potion_alderfall_star_phial", "new_potion_twelfth_silence_flask"
            )))
    );

    public static final Map<String, RecruitSpec> RECRUITS = Map.ofEntries(
            recruit("marla", "Marla", "npc_marla", "Cleric", 44, 24, 7, 2,
                    List.of(new Ability("Field Mend", 18, 5, Ability.AbilityKind.HEAL)),
                    Map.of("potion_small", 1)),
            recruit("ren", "Ren", "npc_ren", "Mage", 38, 34, 10, 2,
                    List.of(new Ability("Rune Flare", 20, 6, Ability.AbilityKind.DAMAGE)),
                    Map.of("ether", 1)),
            recruit("torin", "Torin", "npc_torin", "Knight", 56, 14, 12, 4,
                    List.of(new Ability("Guarding Strike", 18, 5, Ability.AbilityKind.DAMAGE)),
                    Map.of("guard_tonic", 1)),
            recruit("eira", "Eira", "npc_marla", "Ranger", 46, 20, 12, 3,
                    List.of(
                            new Ability("Marked Shot", 19, 5, Ability.AbilityKind.DAMAGE),
                            new Ability("Trail Salve", 16, 5, Ability.AbilityKind.HEAL)
                    ),
                    Map.of("potion_small", 1)),
            recruit("bran", "Bran", "npc_torin", "Knight", 52, 14, 11, 4,
                    List.of(new Ability("Roadwarden's Cut", 17, 4, Ability.AbilityKind.DAMAGE)),
                    Map.of("guard_tonic", 1)),
            recruit("niva", "Niva", "npc_ren", "Cleric", 40, 30, 9, 3,
                    List.of(
                            new Ability("Blue Candle", 18, 5, Ability.AbilityKind.DAMAGE),
                            new Ability("Warm Hands", 18, 6, Ability.AbilityKind.HEAL)
                    ),
                    Map.of("ether", 1)),
            recruit("sela", "Sela", "npc_marla", "Rogue", 44, 22, 12, 2,
                    List.of(new Ability("Mirage Cut", 20, 6, Ability.AbilityKind.DAMAGE)),
                    Map.of("potion_small", 1)),
            recruit("fen", "Fen", "npc_ren", "Mage", 42, 32, 10, 2,
                    List.of(
                            new Ability("Boglight Hex", 20, 6, Ability.AbilityKind.DAMAGE),
                            new Ability("Reed Charm", 18, 6, Ability.AbilityKind.HEAL)
                    ),
                    Map.of("ether", 1)),
            recruit("liora", "Liora", "npc_liora", "Battle Medic", 46, 32, 8, 3,
                    List.of(new Ability("Clean Bandage", 20, 6, Ability.AbilityKind.HEAL, "ally")),
                    Map.of("potion_small", 2, "ether", 1)),
            recruit("garruk", "Garruk", "npc_garruk", "Ironwall", 68, 12, 10, 7,
                    List.of(new Ability("Brace the Door", 0, 5, Ability.AbilityKind.DEFEND, "self")),
                    Map.of("guard_tonic", 2)),
            recruit("kael", "Kael", "npc_kael", "Bladedancer", 48, 24, 14, 3,
                    List.of(new Ability("Silver Feint", 22, 6, Ability.AbilityKind.DAMAGE)),
                    Map.of("battle_kit", 1)),
            recruit("nyx", "Nyx", "npc_nyx", "Veilrunner", 44, 28, 13, 3,
                    List.of(new Ability("Smoke Needle", 18, 6, Ability.AbilityKind.DAMAGE)),
                    Map.of("potion_small", 1, "ether", 1)),
            recruit("rowan_wildspeaker", "Rowan", "npc_rowan", "Wildspeaker", 50, 30, 10, 4,
                    List.of(new Ability("Foxglove Charm", 18, 6, Ability.AbilityKind.HEAL, "ally")),
                    Map.of("ether", 1)),
            recruit("mira_sunwarden", "Mira Sunwarden", "npc_mira_sunwarden", "Sunwarden", 58, 26, 11, 5,
                    List.of(new Ability("Lantern Guard", 0, 5, Ability.AbilityKind.DEFEND, "self")),
                    Map.of("guard_tonic", 1, "potion_small", 1)),
            recruit("vexa", "Vexa", "npc_vexa", "Thornbinder", 48, 34, 12, 3,
                    List.of(new Ability("Briar Hex", 22, 7, Ability.AbilityKind.DAMAGE)),
                    Map.of("ether", 1)),
            recruit("orin", "Orin", "npc_orin", "Stonebreaker", 62, 16, 15, 5,
                    List.of(new Ability("Granite Hook", 24, 7, Ability.AbilityKind.DAMAGE)),
                    Map.of("battle_kit", 1)),
            recruit("sable", "Sable", "npc_sable", "Nightblade", 42, 26, 15, 2,
                    List.of(new Ability("Black Venom", 20, 7, Ability.AbilityKind.DAMAGE)),
                    Map.of("potion_small", 1)),
            recruit("elowen", "Elowen", "npc_elowen", "Grovekeeper", 44, 36, 8, 4,
                    List.of(new Ability("Springwater", 18, 8, Ability.AbilityKind.HEAL, "party")
                            .withCooldown(2).withScaling(Ability.ScalingProfile.NATURE).withEffect("water")
                            .withStatuses(new AbilityStatus("regeneration", "target"))),
                    Map.of("ether", 2)),
            recruit("orren", "Orren", "npc_merchant", "Rogue", 40, 24, 10, 3,
                    List.of(new Ability("Ledger Jab", 17, 5, Ability.AbilityKind.DAMAGE)),
                    Map.of("potion_small", 1, "trail_rations", 1)),
            recruit("sori", "Sori", "npc_citizen_woman", "Ranger", 44, 20, 11, 3,
                    List.of(new Ability("Tanner's Mark", 18, 5, Ability.AbilityKind.DAMAGE)),
                    Map.of("cooked_meat", 1)),
            recruit("berta", "Berta", "npc_baker", "Battle Medic", 48, 28, 8, 4,
                    List.of(new Ability("Hot Broth", 18, 6, Ability.AbilityKind.HEAL, "party")),
                    Map.of("trail_rations", 2)),
            recruit("safa", "Safa", "npc_citizen_woman", "Cleric", 42, 30, 8, 3,
                    List.of(new Ability("Wellwater Blessing", 20, 6, Ability.AbilityKind.HEAL, "ally")),
                    Map.of("ether", 1, "potion_small", 1)),
            recruit("seraphine", "Seraphine Vale", "npc_seraphine", "Veilrunner", 44, 30, 13, 3,
                    List.of(new Ability("Silkknife", 21, 6, Ability.AbilityKind.DAMAGE)),
                    Map.of("potion_small", 1, "softstep_boots", 1)),
            recruit("maera", "Maera Quill", "npc_maera", "Mage", 40, 38, 10, 2,
                    List.of(new Ability("Star Margin", 22, 7, Ability.AbilityKind.DAMAGE)),
                    Map.of("ether", 2)),
            recruit("cassia", "Cassia Flint", "npc_cassia", "Ironwall", 70, 14, 11, 7,
                    List.of(new Ability("Breach Brace", 0, 5, Ability.AbilityKind.DEFEND, "self")),
                    Map.of("guard_tonic", 2)),
            recruit("lyra", "Lyra Bell", "npc_lyra", "Battle Medic", 48, 32, 8, 4,
                    List.of(new Ability("Steady Hands", 22, 7, Ability.AbilityKind.HEAL, "ally")),
                    Map.of("battle_kit", 1, "potion_small", 1)),
            recruit("samir", "Samir Dawn", "npc_samir", "Sunwarden", 58, 28, 11, 5,
                    List.of(new Ability("Forbidden Lantern", 19, 6, Ability.AbilityKind.DAMAGE)),
                    Map.of("ether", 1, "guard_tonic", 1)),
            recruit("aria", "Aria Foxglove", "npc_aria", "Ranger", 48, 22, 12, 3,
                    List.of(new Ability("Debt Shot", 20, 6, Ability.AbilityKind.DAMAGE)),
                    Map.of("trail_rations", 1)),
            recruit("vesper", "Vesper Snowroot", "npc_vesper", "Grovekeeper", 46, 36, 8, 4,
                    List.of(new Ability("Root Memory", 20, 12, Ability.AbilityKind.HEAL, "party")
                            .withCooldown(4).withScaling(Ability.ScalingProfile.NATURE).withEffect("root_memory")
                            .withStatuses(new AbilityStatus("shield", "target"), new AbilityStatus("fortified", "target", 0.65), new AbilityStatus("regeneration", "target"))
                            .withTags("revive_party", "overheal_shield")),
                    Map.of("ether", 2)),
            recruit("rafiq", "Rafiq Glass", "npc_rafiq", "Bladedancer", 50, 24, 14, 3,
                    List.of(new Ability("Glassstep Feint", 23, 7, Ability.AbilityKind.DAMAGE)),
                    Map.of("battle_kit", 1)),
            recruit("calder", "Calder Reed", "npc_calder", "Stonebreaker", 64, 18, 15, 5,
                    List.of(new Ability("Foundation Crack", 24, 7, Ability.AbilityKind.DAMAGE)),
                    Map.of("guard_tonic", 1))
    );

    public static final List<Npc> NPCS = List.of(
            new Npc("city_riverside", "Marla", "npc_marla", 17, 11, List.of(
                    "I was a field medic before Riverside had walls.",
                    "The marsh fever is back, and the slimes carry it along the herb road.",
                    "Clear the road and I will travel with you. Alderfall needs hands that can mend."
            ), "slime_help", "riverside", "marla", 0),
            new Npc("city_riverside", "Peddler Nessa", "npc_merchant", 12, 14, List.of(
                    "I sell needles, soap, dried apples, and occasionally excellent rumors.",
                    "The market road is being squeezed by raiders. Clear it and my prices may stop twitching."
            ), "market_road_clearance", "riverside"),
            new Npc("city_riverside", "Dockhand Hobb", "npc_citizen_man", 22, 13, List.of(
                    "River fog makes everyone poetic until the crates need lifting.",
                    "If you hear splashing under the pier, assume it wants your lunch."
            ), null, null),
            new Npc("city_riverside", "Seamstress Vala", "npc_citizen_woman", 14, 9, List.of(
                    "I can tell where someone traveled by the mud in their hem.",
                    "Road dust, grave ash, marsh silt. Your cloak has been having a full career."
            ), null, null),
            new Npc("city_archive", "Archivist Ren", "npc_ren", 17, 11, List.of(
                    "The Archive keeps the names of everyone the old wards failed to save.",
                    "Now the dead are leaving their shelves and walking below Stonegate.",
                    "Bring me proof the crypt can be quieted, and I will carry the record beside you."
            ), "crypt_lights", null, "ren", 0),
            new Npc("city_archive", "Map-Seller Dain", "npc_merchant", 12, 10, List.of(
                    "Every map is a promise made before the rain gets a vote.",
                    "I mark safe roads in ink and dangerous roads in very expensive ink."
            ), null, "riverside"),
            new Npc("city_archive", "Apprentice Miri", "npc_citizen_woman", 20, 14, List.of(
                    "Ren says shelving is a sacred duty. I say the ladders are too tall.",
                    "If a book whispers, whisper back politely. That is the first Archive rule I believed."
            ), null, null),
            new Npc("city_archive", "Scribe Pela", "npc_citizen_woman", 24, 11, List.of(
                    "Ink fades. Stone flakes. Memory is not as sturdy as people claim.",
                    "There are grave names near Stonegate that must be copied before the weather wins."
            ), "grave_rubbings", null),
            new Npc("city_highwall", "Captain Torin", "npc_torin", 16, 11, List.of(
                    "I lost a patrol at the west stones, and the raiders learned our names from their packs.",
                    "Their brute leads every raid. Break him and the road opens again.",
                    "Do that, and Highwall owes you my shield arm."
            ), "orc_siege", null, "torin", 0),
            new Npc("city_highwall", "Canteen Cook Berta", "npc_baker", 13, 16, List.of(
                    "A guard marches farther after soup than after speeches.",
                    "Hire me and I will keep your camp fed, patched, and loudly supervised."
            ), null, null, "berta", 75),
            new Npc("city_highwall", "Gate Clerk Halen", "npc_citizen_man", 19, 15, List.of(
                    "If the gate ledger says you left, please try to return in roughly the same number of pieces.",
                    "Paperwork dislikes heroic ambiguity."
            ), null, null),
            new Npc("city_highwall", "Signal Keeper Lysa", "npc_citizen_woman", 12, 13, List.of(
                    "Smoke is faster than a horse when raiders know how to shape it.",
                    "Douse their campfires and Highwall's next patrol may pass unseen."
            ), "camp_smoke", null),
            new Npc("city_highwall", "Scout Rook", "npc_ren", 21, 12, List.of(
                    "Highwall scouts found a crown nailed together from coins, bones, and stolen buckles.",
                    "The one wearing it has turned scattered raiders into a camp with orders. That cannot stand."
            ), "goblin_crown", null),
            new Npc("city_highwall", "Trapmaster Yaro", "npc_blacksmith", 23, 14, List.of(
                    "I teach scouts how to spot snares by the way grass apologizes.",
                    "Goblin trappers have been too neat by half. Break their line before dawn patrol rides."
            ), "trapline_cleanup", null),
            new Npc("city_belltower", "Bellkeeper Ilya", "npc_marla", 17, 9, List.of(
                    "The bell tower used to keep ships and spirits honest.",
                    "Quiet those wings and the city can sleep again."
            ), "shadow_swarm", "highwall"),
            new Npc("city_belltower", "Net-Mender Corso", "npc_citizen_man", 21, 12, List.of(
                    "Bell ropes, fishing nets, spider silk; I mend what I can and swear at the rest.",
                    "Something is nesting in the rope loft. I would like the bells to stop twitching."
            ), "spider_silk_tangle", null),
            new Npc("city_sanctum", "Warden Sol", "npc_ren", 15, 10, List.of(
                    "Sanctum was built around the first ward-stone.",
                    "Wraiths drift through cracks we cannot seal quickly enough."
            ), "wraith_hunt", null),
            new Npc("city_sanctum", "Brother Cal", "npc_torin", 12, 15, List.of(
                    "A ward marker can fail silently for years, then complain all at once.",
                    "Check the skull wards near the old graves. Tell me which still hold."
            ), "skull_wards", null),
            new Npc("city_sanctum", "Quartermaster Vesh", "npc_quartermaster", 21, 11, List.of(
                    "Lower tunnels chew through supplies and nerves alike.",
                    "If you descend, take every vial you can carry."
            ), "broodmother", "crypt_vendor"),
            new Npc("city_sanctum", "Eira", "npc_marla", 17, 16, List.of(
                    "Frost wolves have started tearing down my cairns.",
                    "Bring me two pelts and I will guide your party through any whiteout."
            ), "winter_fangs", null, "eira", 0),
            new Npc("city_sanctum", "Ash Watcher Kera", "npc_vexa", 24, 15, List.of(
                    "The desert ruins laugh at night now. Stone should not have that much personality.",
                    "Ember Imps are nesting in cold hearths. Snuff them before the wind learns their joke."
            ), "ember_imp_coals", null),
            new Npc("village_oakhaven", "Edda", "npc_baker", 13, 8, List.of(
                    "Oakhaven keeps the old road fed, even when the road bites back.",
                    "The near farm still has wheat standing. Bring me sheaves and I will open the market stores."
            ), "bread_for_road", "riverside"),
            new Npc("village_oakhaven", "Hedgewise Lin", "npc_citizen_woman", 22, 11, List.of(
                    "The green road used to smell like clover. Now it smells like angry roots.",
                    "Briar Thornlings are crowding the wagons. Cut them back before the hedges win an election."
            ), "thornling_roots", null),
            new Npc("village_oakhaven", "Tanner Sori", "npc_citizen_woman", 7, 9, List.of(
                    "The roadwatch needs winter cloaks before the cold starts making decisions.",
                    "Bring me ten wolf pelts and I will join your camp. Someone has to keep the seams honest."
            ), "wolf_pelt_order", null, "sori", 0),
            new Npc("village_oakhaven", "Finch", "npc_citizen_man", 21, 8, List.of(
                    "I am not allowed near the scarecrow anymore.",
                    "It looked lonely. That is all I am saying without a lawyer."
            ), null, null),
            new Npc("village_oakhaven", "Farmer Joss", "npc_citizen_man", 19, 14, List.of(
                    "A patrol without fed horses is just a group of tired people standing in a road.",
                    "Bring hay from the west field and I will keep the watch mounts from chewing the gate."
            ), "hay_for_horses", null),
            new Npc("village_oakhaven", "Rowan", "npc_ren", 8, 15, List.of(
                    "That scarecrow keeps turning when nobody admits touching it.",
                    "I need someone brave enough to inspect straw without pretending straw cannot be suspicious."
            ), "scarecrow_watch", null),
            new Npc("village_oakhaven", "Mira", "npc_merchant", 10, 10, List.of(
                    "I mark every crate that leaves this village. Raiders took two with my blue cord still tied on.",
                    "Their camp is marked on your map. Bring the crates back before the sick have to make do with prayers."
            ), "stolen_supplies", null),
            new Npc("village_oakhaven", "Bran", "npc_torin", 16, 9, List.of(
                    "I kept the roadwatch until my company scattered at Stonegate.",
                    "Pay my contract and I will keep your camp standing when the night gets loud."
                ), null, null, "bran", 40),
            new Npc("village_oakhaven", "Liora", "npc_liora", 22, 9, List.of(
                    "I stitch people first and lecture them later. It keeps morale surprisingly high.",
                    "Hire me and your camp gets clean bandages, warm tea, and fewer dramatic last words."
            ), null, null, "liora", 70),
            new Npc("village_oakhaven", "Rowan Wildspeaker", "npc_rowan", 6, 11, List.of(
                    "The hedges have been gossiping about bootprints and bad steel.",
                    "I can ask the wilds to watch your back, if your coin is as honest as your footsteps."
            ), null, null, "rowan_wildspeaker", 95),
            new Npc("village_snowrest", "Niva", "npc_ren", 14, 8, List.of(
                    "Snowrest is the last warm hearth before the pass.",
                    "I read tracks in snow like scripture. Hire me and I will read the road ahead."
                ), null, "highwall", "niva", 90),
            new Npc("village_snowrest", "Garruk Ironwall", "npc_garruk", 9, 10, List.of(
                    "Snow makes cowards of hinges and heroes of shields.",
                    "Pay my rate and I will stand where the road gets narrow."
            ), null, null, "garruk", 120),
            new Npc("village_snowrest", "Elowen", "npc_elowen", 20, 12, List.of(
                    "Even frost remembers spring if you know how to ask.",
                    "I can keep wounds closing and spirits rooted, for a fair share of the road."
            ), null, null, "elowen", 110),
            new Npc("village_snowrest", "Cairnwatch Asta", "npc_marla", 18, 9, List.of(
                    "Snow covers mistakes and history with equal enthusiasm.",
                    "Read the old Frosthollow stones before the next whiteout turns them blank."
                ), "winter_records", null),
            new Npc("village_snowrest", "Goatkeeper Una", "npc_citizen_woman", 11, 14, List.of(
                    "Goats are not evil. They are just ambitious in directions we regret.",
                    "Three of mine keep smashing pass bells. Persuade them from a distance."
            ), "goat_bell_roundup", null),
            new Npc("village_snowrest", "Furrier Pem", "npc_merchant", 16, 13, List.of(
                    "Good mittens are proof civilization deserves another chance.",
                    "I buy pelts, mend gloves, and refuse to discuss socks before noon."
            ), null, "highwall"),
            new Npc("village_snowrest", "Pass Guide Olin", "npc_citizen_man", 21, 13, List.of(
                    "Every pass marker has a story. Lately something large has been ending those stories early.",
                    "Find the Ice Golem before the whiteout makes the mountain look newly invented."
            ), "ice_golem_marks", null),
            new Npc("village_dunewick", "Sela", "npc_marla", 14, 8, List.of(
                    "Dunewick survives by water, shade, and stubbornness.",
                    "I know which wells lie and which badland trails ambush the careless. My fee is fair."
                ), null, "riverside", "sela", 85),
            new Npc("village_dunewick", "Rain-Seer Imani", "npc_mira_sunwarden", 21, 14, List.of(
                    "My family has argued about one rainstorm for three generations. That is desert history for you.",
                    "Now Sand Stalkers follow caravan shade. Hunt them before the next water run."
            ), "sand_stalker_hunt", null),
            new Npc("village_dunewick", "Orren the Peddler", "npc_merchant", 11, 13, List.of(
                    "I can sell you a button, a kettle, or a theory about why your boots squeak.",
                    "Goblin scouts stole my ledger. Recover it and I will keep your party's accounts balanced."
            ), "peddler_ledger", "riverside", "orren", 0),
            new Npc("village_dunewick", "Wellkeeper Safa", "npc_citizen_woman", 17, 15, List.of(
                    "Water has moods. The trick is noticing before it ruins breakfast.",
                    "Hire me and I will keep your skins full and your wounds clean."
            ), null, null, "safa", 90),
            new Npc("village_dunewick", "Spice Peddler Rafi", "npc_merchant", 24, 9, List.of(
                    "Cumin, salt, sun-pepper, and one jar I refuse to identify until it stops humming.",
                    "Buy something before the wind seasons it for free."
            ), null, "riverside"),
            new Npc("village_dunewick", "Kael", "npc_kael", 8, 12, List.of(
                    "Two blades means twice the upkeep, but half the boredom.",
                    "If your party needs a fighter who can move, I am listening."
            ), null, null, "kael", 100),
            new Npc("village_dunewick", "Nyx", "npc_nyx", 20, 8, List.of(
                    "Some trails are safer when nobody knows you took them.",
                    "My smoke vials cost extra. My discretion does not."
            ), null, null, "nyx", 105),
            new Npc("village_dunewick", "Orin Stonebreaker", "npc_orin", 23, 12, List.of(
                    "A hammer solves doors, armor, and certain conversations.",
                    "Hire me if you want the problem to become flatter."
            ), null, null, "orin", 115),
            new Npc("village_dunewick", "Toma", "npc_ren", 18, 10, List.of(
                    "Raiders have started keeping ledgers. That is either civilization or a warning sign.",
                    "Find the book in their tent and Dunewick will know who bought safe passage."
                ), "camp_ledger", null),
            new Npc("village_mireford", "Fen", "npc_ren", 14, 8, List.of(
                    "Mireford is built on planks, patience, and listening to things under the water.",
                    "Pay for my charms and I will make the marsh answer to us for once."
                ), null, "crypt_vendor", "fen", 100),
            new Npc("village_mireford", "Reedcutter Vell", "npc_citizen_woman", 22, 12, List.of(
                    "If a walkway complains, I listen. Lately the planks are using language.",
                    "A Bog Beast is chewing the pilings. I want it persuaded with steel."
            ), "bog_beast_bounty", null),
            new Npc("village_mireford", "Basketmaker Jun", "npc_citizen_man", 12, 12, List.of(
                    "A good basket holds reeds, apples, secrets, and occasionally a very embarrassed frog.",
                    "The marsh gives materials freely. It charges interest in boots."
            ), null, null),
            new Npc("village_mireford", "Lantern Seller Pella", "npc_merchant", 17, 14, List.of(
                    "A lantern is just a little sun with better manners.",
                    "Mine keep burning in fog, rain, and most supernatural sulking."
            ), null, "crypt_vendor"),
            new Npc("village_mireford", "Mira Sunwarden", "npc_mira_sunwarden", 9, 12, List.of(
                    "A lantern is a promise that darkness has edges.",
                    "I can guard your line and mend what gets through it."
            ), null, null, "mira_sunwarden", 125),
            new Npc("village_mireford", "Vexa", "npc_vexa", 22, 13, List.of(
                    "Thorns are honest. They warn you once, then keep their word.",
                    "Bring me along if you want the battlefield to grow teeth."
            ), null, null, "vexa", 120),
            new Npc("village_mireford", "Sable", "npc_sable", 8, 9, List.of(
                    "Names are noisy. Footsteps are worse.",
                    "Pay in coin, speak softly, and point me at the lock or the throat."
            ), null, null, "sable", 130),
            new Npc("village_mireford", "Old Noll", "npc_citizen_man", 19, 10, List.of(
                    "People call a palisade a wall because it sounds braver.",
                    "Find me the weak stakes and Mireford can make a doorway where raiders expect a fence."
            ), "palisade_gaps", null),
            // Main story NPCs use QuestType.MAIN_STORY quests, which draw red quest markers.
            new Npc("village_oathstead_camp", "Maelis", "npc_story_maelis", 14, 14, List.of(
                    "You look like someone death rejected for paperwork reasons.",
                    "Do not call what Vaelthara did mercy. Mercy leaves a person whole.",
                    "Oathstead does not need heroes first. It needs hands."
            ), "ms_wake_ashes", null),
            new Npc("city_archive", "Selene", "npc_story_selene", 11, 12, List.of(
                    "History does not vanish. It is moved, relabeled, sealed, misquoted, or made inconvenient.",
                    "The first lie was not that the Demon Queen died. It was that the kingdoms defeated her together.",
                    "Truth is not a torch. It is a blade. Hold it wrongly and everyone bleeds."
            ), "ms_names_dust", null),
            new Npc("city_highwall", "Odrick", "npc_story_odrick", 24, 10, List.of(
                    "Panic is just another enemy formation. Break its front and it scatters.",
                    "Highwall stands because someone freezes on watch while others sleep.",
                    "You want my trust? Bring back my road."
            ), "ms_watchtower_bells", null),
            new Npc("city_sanctum", "Solari", "npc_story_solari", 18, 8, List.of(
                    "The sun does not forgive. It reveals.",
                    "Vaelthara does not burn like fire. Fire consumes and ends. She burns like guilt.",
                    "You ask for a stone. Stones are easy. The right to carry one must be earned."
            ), "ms_shrine_shadow", null),
            new Npc("city_belltower", "Ysra", "npc_story_ysra", 20, 11, List.of(
                    "In the Fenlands, silence is rarely empty.",
                    "A bell rings for fog, flood, funeral, or fool. Today I am deciding which you are.",
                    "If Vaelthara reached the bells, she reached the roads beneath the water."
            ), "ms_bell_alone", null),
            new Npc("city_riverside", "Mirella", "npc_story_mirella", 20, 8, List.of(
                    "A kingdom is roads, receipts, grain, and tomorrow's bridge still being there.",
                    "I do not dismiss your Demon Queen. I merely note that panic also has logistics.",
                    "Find me the missing supplies, and I will decide whether your apocalypse deserves space on my desk."
            ), "ms_toll_ledger", null),
            new Npc("village_oakhaven", "Elder Rowan", "npc_story_elder_rowan", 6, 11, List.of(
                    "Roots remember where blood fell. Folk forget because folk are merciful to themselves.",
                    "That orchard ward was old before my grandmother had teeth.",
                    "Black roots mean something hurt the forest badly enough that it learned hatred."
            ), "ms_orchard_ward", null),
            new Npc("city_archive", "Gravekeeper Hollis", "npc_story_gravekeeper_hollis", 23, 15, List.of(
                    "People think gravekeeping is about the dead. It is not. The dead are easy.",
                    "Scratch a name from stone, and you do not erase a person. You untie them.",
                    "The Nameless Warden guards the idea that someone should remember."
            ), "ms_names_cold_stone", null),
            new Npc("village_snowrest", "Captain Elric Snowrest", "npc_story_captain_elric_snowrest", 18, 9, List.of(
                    "Snow does not care about bravery. Neither does fever.",
                    "Bring firewood first. Then we can discuss demons.",
                    "A missing medicine crate kills more people than most monsters."
            ), "ms_cold_road", null),
            new Npc("village_glimmerfen", "Bellwright Nessa", "npc_story_bellwright_nessa", 18, 10, List.of(
                    "A bell is not just metal. It is distance made useful.",
                    "Bad rope kills more travelers than bad monsters.",
                    "You want old magic? Fine. Start by holding this hook straight."
            ), "ms_missing_bell_rope", null),
            new Npc("village_redcairn", "Ash-Scribe Damar", "npc_story_ash_scribe_damar", 20, 12, List.of(
                    "Ash is honest. It tells you what endured.",
                    "Blackvault has been dead for a century. Yesterday, it started making new shadows.",
                    "I do not fear ruins. I fear ruins that begin keeping appointments."
            ), "ms_blackvault_mark", null),
            new Npc("city_riverside", "Seraphine Vale", "npc_seraphine", 20, 10, List.of(
                    "Silk is stronger than people think. So are women who learned to smile before drawing a knife.",
                    "My old counting-house secrets are loose on the road. Help me fold them away and I may trust your camp.",
                    "Trust costs more than coin. Spend patience."
            ), "seraphine_chain_1", null, "seraphine", 0),
            new Npc("city_archive", "Maera Quill", "npc_maera", 15, 14, List.of(
                    "The Archive shelves approved histories. I prefer the margins where truth forgets to behave.",
                    "There is a missing constellation in the old maps. Find it with me and I will bring the stars to your side.",
                    "Do not flatter me. Bring evidence."
            ), "maera_chain_1", null, "maera", 0),
            new Npc("city_highwall", "Cassia Flint", "npc_cassia", 11, 12, List.of(
                    "Armor is only loud when the person inside still needs convincing.",
                    "Highwall left trophies in enemy hands. Help me retrieve the names and I will stand in your line.",
                    "I do not join causes. I join people who hold."
            ), "cassia_chain_1", null, "cassia", 0),
            new Npc("city_belltower", "Lyra Bell", "npc_lyra", 13, 12, List.of(
                    "A pretty face gets patients talking. A steady hand keeps them alive.",
                    "Someone turned my clinic records into a weapon. Help me find who, and my kit travels with you.",
                    "Care is not soft. It is stubborn."
            ), "lyra_chain_1", null, "lyra", 0),
            new Npc("city_sanctum", "Samir Dawn", "npc_samir", 20, 14, List.of(
                    "Sanctum taught me every proper prayer except the one for leaving.",
                    "My family reliquary is burning around a secret. Help me open it and I will carry the lantern beyond these walls.",
                    "Faith without questions is only furniture."
            ), "samir_chain_1", null, "samir", 0),
            new Npc("village_oakhaven", "Aria Foxglove", "npc_aria", 24, 14, List.of(
                    "I know how to look harmless. The road taught me that harmless people get underestimated.",
                    "My sister left through briars and debt. Help me find the shape of that choice.",
                    "If you want loyalty, do not rush the roots."
            ), "aria_chain_1", null, "aria", 0),
            new Npc("village_snowrest", "Vesper Snowroot", "npc_vesper", 7, 15, List.of(
                    "Winter is not death. It is a long inhale.",
                    "There is a living grove under the pass. Prove I am not dreaming and I will follow the green road with you.",
                    "Warmth given too quickly melts trust."
            ), "vesper_chain_1", null, "vesper", 0),
            new Npc("village_dunewick", "Rafiq Glass", "npc_rafiq", 18, 16, List.of(
                    "I have won duels, lost arguments, and misplaced only one caravan's worth of dignity.",
                    "Help me untangle a rigged duel and I will lend your party two blades and fewer excuses.",
                    "I flirt with danger because danger rarely knows when it is being mocked."
            ), "rafiq_chain_1", null, "rafiq", 0),
            new Npc("village_mireford", "Calder Reed", "npc_calder", 15, 15, List.of(
                    "Mud lies. Stone keeps accounts.",
                    "Something is chewing at Mireford's foundation stones. Help me save them and I will put my hammer where your road is weakest.",
                    "Do not ask me to smile before the building stops sinking."
            ), "calder_chain_1", null, "calder", 0)
    );

    private static Map.Entry<String, MonsterSpec> monster(
            String species,
            String key,
            String name,
            String sprite,
            int hp,
            int attack,
            int defense,
            int xp,
            int gold
    ) {
        return Map.entry(key, new MonsterSpec(key, name, species, sprite, hp, attack, defense, xp, gold));
    }

    private static Map.Entry<String, Quest> quest(
            String id,
            String title,
            String description,
            String target,
            int needed,
            int rewardGold,
            int rewardXp
    ) {
        return Map.entry(id, new Quest(id, title, description, target, needed, rewardGold, rewardXp));
    }

    private static Map.Entry<String, Quest> quest(
            String id,
            String title,
            String description,
            String target,
            int needed,
            int rewardGold,
            int rewardXp,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        return Map.entry(id, new Quest(id, title, description, target, needed, rewardGold, rewardXp,
                Quest.ObjectiveKind.DEFEAT, WorldMap.OVERWORLD_ID, null, 0, null, null,
                startDialog, progressDialog, readyDialog, completeDialog));
    }

    private static Map.Entry<String, Quest> quest(
            String id,
            String title,
            String description,
            String target,
            int needed,
            int rewardGold,
            int rewardXp,
            Quest.ObjectiveKind objectiveKind,
            String objectiveLocationKind,
            int objectiveLocationIndex,
            String objectiveAsset,
            String monsterKey,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        return Map.entry(id, new Quest(id, title, description, target, needed, rewardGold, rewardXp,
                objectiveKind, WorldMap.OVERWORLD_ID, objectiveLocationKind, objectiveLocationIndex,
                objectiveAsset, monsterKey, startDialog, progressDialog, readyDialog, completeDialog));
    }

    private static Map.Entry<String, Quest> mainStoryQuest(
            String id,
            String title,
            String description,
            String target,
            int needed,
            int rewardGold,
            int rewardXp,
            String chainOwnerId,
            String nextQuestId,
            Quest.ObjectiveKind objectiveKind,
            String objectiveLocationKind,
            int objectiveLocationIndex,
            String objectiveAsset,
            String monsterKey,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        String objectiveMapId = objectiveKind.defenseMinigameObjective()
                ? raidDefenseMapId(objectiveLocationKind)
                : WorldMap.OVERWORLD_ID;
        return Map.entry(id, new Quest(id, title, description, target, needed, rewardGold, rewardXp,
                objectiveKind, objectiveMapId, objectiveLocationKind, objectiveLocationIndex,
                objectiveAsset, monsterKey, startDialog, progressDialog, readyDialog, completeDialog,
                Quest.QuestType.MAIN_STORY, chainOwnerId, nextQuestId));
    }

    private static String raidDefenseMapId(String objectiveLocationKind) {
        if (objectiveLocationKind == null || objectiveLocationKind.isBlank() || "oathstead_camp".equals(objectiveLocationKind)) {
            return WorldMap.PLAYER_VILLAGE_ID;
        }
        String key = objectiveLocationKind.strip();
        if (key.startsWith("city_") || key.startsWith("town_") || key.startsWith("village_")) {
            return key;
        }
        return WorldMap.PLAYER_VILLAGE_ID;
    }

    private static Map.Entry<String, Quest> companionQuest(
            String id,
            String title,
            String description,
            String target,
            int needed,
            int rewardGold,
            int rewardXp,
            String chainOwnerId,
            String nextQuestId,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        Quest.ObjectiveKind objectiveKind = Quest.ObjectiveKind.DEFEAT;
        String locationKind = null;
        int locationIndex = 0;
        String objectiveAsset = null;
        String monsterKey = null;
        switch (id) {
            case "maera_chain_1" -> {
                objectiveKind = Quest.ObjectiveKind.VISIT;
                locationKind = "graveyard";
                objectiveAsset = "location_graveyard_tombstones";
            }
            case "lyra_chain_2", "samir_chain_2", "vesper_chain_1" -> {
                objectiveKind = Quest.ObjectiveKind.VISIT;
                locationKind = "graveyard";
                locationIndex = id.equals("samir_chain_2") ? 1 : 2;
                objectiveAsset = id.equals("samir_chain_2") ? "location_graveyard_skull_marker" : "location_graveyard_tombstones";
            }
            case "aria_chain_1" -> {
                objectiveKind = Quest.ObjectiveKind.GATHER;
                locationKind = "farmland";
                objectiveAsset = "quest_roadwatch_warning_marks";
            }
            case "aria_chain_2" -> {
                objectiveKind = Quest.ObjectiveKind.GATHER;
                locationKind = "farmland";
                objectiveAsset = "quest_broken_road_signs";
            }
            case "calder_chain_1" -> {
                objectiveKind = Quest.ObjectiveKind.VISIT;
                locationKind = "goblin_camp";
                locationIndex = 3;
                objectiveAsset = "location_camp_palisade";
            }
            default -> {
                monsterKey = monsterKeyForName(target);
                if (monsterKey == null || monsterKey.isBlank()) {
                    objectiveKind = companionStoryObjectiveKind(target);
                    locationKind = companionStoryLocationKind(chainOwnerId, id);
                    locationIndex = Math.floorMod(id.hashCode(), 4);
                    objectiveAsset = companionStoryObjectiveAsset(objectiveKind, target);
                }
            }
        }
        return Map.entry(id, new Quest(id, title, description, target, needed, rewardGold, rewardXp,
                objectiveKind, WorldMap.OVERWORLD_ID, locationKind, locationIndex, objectiveAsset, monsterKey,
                startDialog, progressDialog, readyDialog, completeDialog,
                Quest.QuestType.COMPANION, chainOwnerId, nextQuestId));
    }

    private static Map.Entry<String, Quest> companionStageSearch(
            String id,
            String title,
            String description,
            String target,
            int needed,
            int rewardGold,
            int rewardXp,
            String chainOwnerId,
            String nextQuestId,
            String locationKind,
            int locationIndex,
            String objectiveAsset,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        return companionStage(id, title, description, target, needed, rewardGold, rewardXp,
                chainOwnerId, nextQuestId, Quest.ObjectiveKind.SEARCH, locationKind, locationIndex,
                objectiveAsset, null, "", "", startDialog, progressDialog, readyDialog, completeDialog);
    }

    private static Map.Entry<String, Quest> companionStageTalk(
            String id,
            String title,
            String description,
            String target,
            int rewardGold,
            int rewardXp,
            String chainOwnerId,
            String nextQuestId,
            String targetNpcId,
            String locationKind,
            int locationIndex,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        return companionStage(id, title, description, target, 1, rewardGold, rewardXp,
                chainOwnerId, nextQuestId, Quest.ObjectiveKind.TALK, locationKind, locationIndex,
                null, null, targetNpcId, "", startDialog, progressDialog, readyDialog, completeDialog);
    }

    private static Map.Entry<String, Quest> companionStageRescue(
            String id,
            String title,
            String description,
            String target,
            int rewardGold,
            int rewardXp,
            String chainOwnerId,
            String nextQuestId,
            String targetNpcId,
            String locationKind,
            int locationIndex,
            String monsterKey,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        return companionStage(id, title, description, target, 1, rewardGold, rewardXp,
                chainOwnerId, nextQuestId, Quest.ObjectiveKind.RESCUE, locationKind, locationIndex,
                null, monsterKey, targetNpcId, "", startDialog, progressDialog, readyDialog, completeDialog);
    }

    private static Map.Entry<String, Quest> companionStageDefend(
            String id,
            String title,
            String description,
            String target,
            int needed,
            int rewardGold,
            int rewardXp,
            String chainOwnerId,
            String nextQuestId,
            String locationKind,
            int locationIndex,
            String monsterKey,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        return companionStage(id, title, description, target, needed, rewardGold, rewardXp,
                chainOwnerId, nextQuestId, Quest.ObjectiveKind.DEFEND, locationKind, locationIndex,
                null, monsterKey, "", "", startDialog, progressDialog, readyDialog, completeDialog);
    }

    private static Map.Entry<String, Quest> companionStageChoice(
            String id,
            String title,
            String description,
            String target,
            int rewardGold,
            int rewardXp,
            String chainOwnerId,
            String nextQuestId,
            String branchOutcomeKey,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        return companionStage(id, title, description, target, 1, rewardGold, rewardXp,
                chainOwnerId, nextQuestId, Quest.ObjectiveKind.CHOICE, null, 0,
                null, null, "", branchOutcomeKey, startDialog, progressDialog, readyDialog, completeDialog);
    }

    private static Map.Entry<String, Quest> companionStageOathstead(
            String id,
            String title,
            String description,
            String target,
            int needed,
            int rewardGold,
            int rewardXp,
            String chainOwnerId,
            String nextQuestId,
            String objectiveAsset,
            String branchOutcomeKey,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        return companionStage(id, title, description, target, needed, rewardGold, rewardXp,
                chainOwnerId, nextQuestId, Quest.ObjectiveKind.VISIT, null, 0,
                objectiveAsset, null, "", branchOutcomeKey, startDialog, progressDialog, readyDialog, completeDialog);
    }

    private static Map.Entry<String, Quest> companionStage(
            String id,
            String title,
            String description,
            String target,
            int needed,
            int rewardGold,
            int rewardXp,
            String chainOwnerId,
            String nextQuestId,
            Quest.ObjectiveKind objectiveKind,
            String objectiveLocationKind,
            int objectiveLocationIndex,
            String objectiveAsset,
            String monsterKey,
            String targetNpcId,
            String branchOutcomeKey,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        return Map.entry(id, new Quest(id, title, description, target, needed, rewardGold, rewardXp,
                objectiveKind, WorldMap.OVERWORLD_ID, objectiveLocationKind, objectiveLocationIndex,
                objectiveAsset, monsterKey, startDialog, progressDialog, readyDialog, completeDialog,
                Quest.QuestType.COMPANION, chainOwnerId, nextQuestId, targetNpcId, branchOutcomeKey));
    }

    private static Map.Entry<String, Quest> companionStagedQuest(
            String id,
            String title,
            String description,
            int rewardGold,
            int rewardXp,
            String chainOwnerId,
            String nextQuestId,
            List<Quest.QuestStage> stages
    ) {
        Quest.QuestStage first = stages == null || stages.isEmpty()
                ? questStage(id + "_stage_1", title, title, 1, Quest.ObjectiveKind.VISIT,
                null, 0, null, null, "", "", description, description, description, description)
                : stages.get(0);
        return Map.entry(id, new Quest(id, title, description, first.target(), first.needed(), rewardGold, rewardXp,
                first.objectiveKind(), first.objectiveMapId(), first.objectiveLocationKind(), first.objectiveLocationIndex(),
                first.objectiveAsset(), first.monsterKey(), first.startDialog(), first.progressDialog(),
                first.readyDialog(), first.completeDialog(), Quest.QuestType.COMPANION, chainOwnerId, nextQuestId,
                first.targetNpcId(), first.branchOutcomeKey(), stages));
    }

    private static Quest.QuestStage questStage(
            String id,
            String title,
            String target,
            int needed,
            Quest.ObjectiveKind objectiveKind,
            String objectiveLocationKind,
            int objectiveLocationIndex,
            String objectiveAsset,
            String monsterKey,
            String targetNpcId,
            String branchOutcomeKey,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        return new Quest.QuestStage(id, title, target, needed, objectiveKind, WorldMap.OVERWORLD_ID,
                objectiveLocationKind, objectiveLocationIndex, objectiveAsset, monsterKey, targetNpcId,
                branchOutcomeKey, startDialog, progressDialog, readyDialog, completeDialog);
    }

    private static Quest.ObjectiveKind companionStoryObjectiveKind(String target) {
        String compact = target == null ? "" : target.toLowerCase(Locale.ROOT);
        if (compact.contains("note") || compact.contains("ink") || compact.contains("page")
                || compact.contains("supply") || compact.contains("bandage") || compact.contains("water")
                || compact.contains("wood") || compact.contains("timber") || compact.contains("stone")
                || compact.contains("shard") || compact.contains("badge") || compact.contains("tag")
                || compact.contains("order") || compact.contains("herb") || compact.contains("rope")
                || compact.contains("moss") || compact.contains("contract")) {
            return Quest.ObjectiveKind.GATHER;
        }
        return Quest.ObjectiveKind.VISIT;
    }

    private static String companionStoryLocationKind(String chainOwnerId, String questId) {
        String owner = chainOwnerId == null ? "" : chainOwnerId;
        return switch (owner) {
            case "seraphine", "maera", "samir", "rafiq" -> "graveyard";
            case "cassia", "calder" -> "goblin_camp";
            case "lyra", "aria" -> "farmland";
            case "vesper" -> "graveyard";
            default -> Math.floorMod(questId.hashCode(), 2) == 0 ? "farmland" : "graveyard";
        };
    }

    private static String companionStoryObjectiveAsset(Quest.ObjectiveKind objectiveKind, String target) {
        String key = target == null ? "" : target.toLowerCase(Locale.ROOT);
        if (key.contains("road sign")) {
            return "quest_broken_road_signs";
        }
        if (key.contains("roadwatch") || key.contains("warning mark")) {
            return "quest_roadwatch_warning_marks";
        }
        if (key.contains("foxglove")) {
            return "quest_foxglove_markers";
        }
        if (key.contains("bandage") || key.contains("clinic") || key.contains("field kit") || key.contains("medical")) {
            return "quest_medical_supplies";
        }
        if (key.contains("water skin")) {
            return "quest_water_skins";
        }
        if (key.contains("firewood") || key.contains("timber")) {
            return "quest_firewood_bundle";
        }
        if (key.contains("stone") || key.contains("causeway") || key.contains("beam")) {
            return "quest_causeway_stones";
        }
        if (key.contains("glass") || key.contains("shard")) {
            return "quest_glasssteel_shards";
        }
        if (key.contains("lamp") || key.contains("lantern") || key.contains("oil")) {
            return "quest_lamp_supplies";
        }
        if (key.contains("herb") || key.contains("moss") || key.contains("root")) {
            return "quest_salve_herbs";
        }
        if (key.contains("ward") || key.contains("seal") || key.contains("marker") || key.contains("oath")) {
            return "quest_ward_marker";
        }
        if (key.contains("note") || key.contains("ledger") || key.contains("contract") || key.contains("map")
                || key.contains("order") || key.contains("hymn") || key.contains("letter") || key.contains("record")
                || key.contains("complaint") || key.contains("page") || key.contains("parchment") || key.contains("ink")) {
            return "quest_document_bundle";
        }
        if (key.contains("shield") || key.contains("gate") || key.contains("winch") || key.contains("rope")
                || key.contains("bell") || key.contains("chest") || key.contains("cot") || key.contains("charm")
                || key.contains("token") || key.contains("notice") || key.contains("room") || key.contains("sand")) {
            return "quest_inspection_cache";
        }
        if (objectiveKind == Quest.ObjectiveKind.GATHER) {
            return "quest_supply_cache";
        }
        return "quest_inspection_cache";
    }

    public static String monsterKeyForName(String monsterName) {
        for (Map.Entry<String, MonsterSpec> entry : MONSTERS.entrySet()) {
            if (entry.getValue().name().equals(monsterName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static boolean isMainStoryQuest(String questId) {
        return questId != null && MAIN_STORY_QUEST_IDS.contains(questId);
    }

    public static String mainStoryStoneReward(String questId) {
        return MAIN_STORY_STONE_REWARDS.getOrDefault(questId, "");
    }

    public static String mainStoryPrerequisite(String questId) {
        return MAIN_STORY_PREREQUISITES.getOrDefault(questId, "");
    }

    public static String mainStoryRequiredItem(String questId) {
        return MAIN_STORY_REQUIRED_ITEMS.getOrDefault(questId, "");
    }

    public static boolean isMagicStone(String itemKey) {
        return itemKey != null && MAGIC_STONE_KEYS.contains(itemKey);
    }

    private static Map.Entry<String, RecruitSpec> recruit(
            String id,
            String name,
            String sprite,
            String className,
            int hp,
            int mp,
            int attack,
            int defense,
            List<Ability> abilities,
            Map<String, Integer> inventory
    ) {
        return Map.entry(id, new RecruitSpec(id, name, sprite, className, hp, mp, attack, defense, abilities, inventory));
    }

    public static String itemName(String key) {
        return EquipmentCatalog.itemName(key);
    }

    public static String itemIcon(String key) {
        return EquipmentCatalog.itemIcon(key);
    }

    public static int itemCost(String key) {
        return EquipmentCatalog.itemCost(key);
    }

    public static boolean isEquipment(String key) {
        return EquipmentCatalog.isEquipment(key);
    }

    public static Equipment equipment(String key) {
        return EquipmentCatalog.equipment(key);
    }

    public static String rollAffixedKey(String baseKey, Random random) {
        return EquipmentCatalog.rollAffixedKey(baseKey, random);
    }

    public static String rollContextualLoot(String mapKind, char terrain, int dungeonTier, List<MonsterSpec> specs, Random random) {
        return EquipmentCatalog.rollContextualLoot(mapKind, terrain, dungeonTier, specs, random);
    }
    public static List<AbilityStatus> statusHintsForAbility(String abilityName, Ability.AbilityKind kind) {
        String lowered = abilityName.strip().toLowerCase();
        List<AbilityStatus> exact = switch (lowered) {
            case "frost lance" -> List.of(new AbilityStatus("weak", "enemy"));
            case "firebolt" -> List.of(new AbilityStatus("burn", "enemy", 0.55));
            case "water jet" -> List.of(new AbilityStatus("weak", "enemy", 0.35));
            case "bulwark" -> List.of(new AbilityStatus("shield", "self"), new AbilityStatus("fortified", "self"));
            case "war cry" -> List.of(new AbilityStatus("fortified", "self"), new AbilityStatus("haste", "self"));
            case "smoke screen" -> List.of(new AbilityStatus("shield", "self"), new AbilityStatus("haste", "self"));
            case "smoke veil", "sunlit aegis" -> List.of(new AbilityStatus("shield", "self"), new AbilityStatus("haste", "self", 0.45));
            case "shieldbreaker", "chain spark" -> List.of(new AbilityStatus("vulnerable", "enemy"));
            case "arcane burst" -> List.of(new AbilityStatus("vulnerable", "enemy", 0.65));
            case "poison arrow", "venom edge" -> List.of(new AbilityStatus("poison", "enemy"));
            case "radiant bolt" -> List.of(new AbilityStatus("vulnerable", "enemy", 0.45));
            case "second wind", "herbal remedy", "mend" -> List.of(new AbilityStatus("regeneration", "self"));
            case "rallying tune", "mana bloom" -> List.of(new AbilityStatus("regeneration", "target"));
            case "aegis mend", "renewing ward", "field salve", "ward prayer", "reviving chorus", "blessing" -> List.of(new AbilityStatus("regeneration", "target"), new AbilityStatus("shield", "target"));
            default -> List.of();
        };
        if (!exact.isEmpty()) {
            return exact;
        }
        java.util.ArrayList<AbilityStatus> hints = new java.util.ArrayList<>();
        if (lowered.contains("poison") || lowered.contains("venom")) {
            hints.add(new AbilityStatus("poison", "enemy"));
        }
        if (lowered.contains("frost") || lowered.contains("ice")) {
            hints.add(new AbilityStatus("weak", "enemy"));
        }
        if (lowered.contains("fire") || lowered.contains("ember")) {
            hints.add(new AbilityStatus("burn", "enemy", 0.5));
        }
        if (lowered.contains("shield") || lowered.contains("ward") || lowered.contains("bulwark")) {
            hints.add(new AbilityStatus("shield", kind == Ability.AbilityKind.HEAL ? "target" : "self"));
        }
        if (lowered.contains("radiant") || lowered.contains("sunlit")) {
            hints.add(new AbilityStatus("vulnerable", "enemy", 0.4));
        }
        if (lowered.contains("mend") || lowered.contains("salve") || lowered.contains("renew") || lowered.contains("blessing") || lowered.contains("chorus")) {
            hints.add(new AbilityStatus("regeneration", kind == Ability.AbilityKind.HEAL ? "target" : "self"));
        }
        return hints;
    }

    public static List<AbilityStatus> statusHintsForAbility(Ability ability) {
        if (ability == null) {
            return List.of();
        }
        if (!ability.statusHints().isEmpty()) {
            return ability.statusHints();
        }
        return statusHintsForAbility(ability.name(), ability.kind());
    }

    public static String battleEffectForAbility(Ability ability) {
        if (ability == null) {
            return "strike";
        }
        if (!ability.effectKey().isBlank()) {
            return ability.effectKey();
        }
        return battleEffectForAbility(ability.name(), ability.kind());
    }

    public static String battleEffectForAbility(String abilityName, Ability.AbilityKind kind) {
        String uniqueEffect = Ability.uniqueEffectKey(abilityName, "");
        if (!uniqueEffect.isBlank()) {
            return uniqueEffect;
        }
        if (kind == Ability.AbilityKind.HEAL) {
            return "heal";
        }
        if (kind == Ability.AbilityKind.DEFEND) {
            return "shield";
        }
        String lowered = abilityName == null ? "" : abilityName.toLowerCase();
        if (lowered.contains("chain spark")) {
            return "lightning";
        }
        if (lowered.contains("legend") || lowered.contains("savant") || lowered.contains("sovereign")
                || lowered.contains("covenant") || lowered.contains("crown") || lowered.contains("ancient")
                || lowered.contains("prismatic") || lowered.contains("meteor") || lowered.contains("lionheart")
                || lowered.contains("worldfire") || lowered.contains("worldsplitter") || lowered.contains("primeval")
                || lowered.contains("noonstar") || lowered.contains("citadel") || lowered.contains("seraphic")
                || lowered.contains("elderwood") || lowered.contains("moonless") || lowered.contains("finale")) {
            return "prismatic";
        }
        if (lowered.contains("arcane") || lowered.contains("rune") || lowered.contains("star")
                || lowered.contains("astral")) {
            return "arcane";
        }
        if (lowered.contains("water") || lowered.contains("tide") || lowered.contains("current")
                || lowered.contains("springwater")) {
            return "water";
        }
        if (lowered.contains("fire") || lowered.contains("ember") || lowered.contains("flare")
                || lowered.contains("candle")) {
            return "ember";
        }
        if (lowered.contains("radiant") || lowered.contains("holy") || lowered.contains("sun")
                || lowered.contains("dawn") || lowered.contains("lantern") || lowered.contains("pilgrim")
                || lowered.contains("judgment")) {
            return lowered.contains("bolt") ? "holy" : "radiant";
        }
        if (lowered.contains("frost") || lowered.contains("ice") || lowered.contains("rime")) {
            return "frost";
        }
        if (lowered.contains("poison") || lowered.contains("venom") || lowered.contains("foxglove")) {
            return "poison";
        }
        if (lowered.contains("volley") || lowered.contains("shot") || lowered.contains("arrow")
                || lowered.contains("dart") || lowered.contains("needle")) {
            return "volley";
        }
        if (lowered.contains("shadow") || lowered.contains("night") || lowered.contains("dusk")
                || lowered.contains("eclipse") || lowered.contains("moon") || lowered.contains("fade")
                || lowered.contains("blindside") || lowered.contains("garrote")) {
            return "dark";
        }
        if (lowered.contains("smoke") || lowered.contains("dust") || lowered.contains("vanish")) {
            return "dust";
        }
        if (lowered.contains("thorn") || lowered.contains("briar") || lowered.contains("vine")
                || lowered.contains("root") || lowered.contains("grove") || lowered.contains("bloom")
                || lowered.contains("green") || lowered.contains("petal") || lowered.contains("canopy")) {
            return "nature";
        }
        if (lowered.contains("quake") || lowered.contains("stone") || lowered.contains("granite")
                || lowered.contains("faultline") || lowered.contains("avalanche")) {
            return "bash";
        }
        if (lowered.contains("fang") || lowered.contains("claw") || lowered.contains("knife storm")
                || lowered.contains("knife rain") || lowered.contains("fan of knives")) {
            return "claw";
        }
        if (lowered.contains("charge") || lowered.contains("rush") || lowered.contains("ram")
                || lowered.contains("slam") || lowered.contains("bash") || lowered.contains("blow")
                || lowered.contains("hammer")) {
            return "bash";
        }
        if (lowered.contains("sweep") || lowered.contains("arc") || lowered.contains("cleave")) {
            return "cleave";
        }
        if (lowered.contains("strike") || lowered.contains("jab")) {
            return "impact";
        }
        if (lowered.contains("storm")) {
            return "sonic";
        }
        if (lowered.contains("slash") || lowered.contains("cut") || lowered.contains("knife")
                || lowered.contains("feint")) {
            return "slash";
        }
        return "strike";
    }

    public static MonsterLore loreFor(MonsterSpec spec) {
        if (spec == null) {
            return new MonsterLore("Unknown creature.", List.of("physical"), Map.of(), Map.of());
        }
        MonsterLore lore = MONSTER_LORE.get(spec.key());
        if (lore != null) {
            return lore;
        }
        return new MonsterLore(defaultMonsterFlavor(spec), defaultMonsterDamageTypes(spec), Map.of(), Map.of());
    }

    public static List<String> damageTypesForAbility(Ability ability) {
        if (ability == null || ability.kind() != Ability.AbilityKind.DAMAGE) {
            return List.of("physical");
        }
        return normalizeDamageTypes(ability.damageTypes());
    }

    public static List<String> damageTypesForEffect(String effect) {
        if (effect == null || effect.isBlank()) {
            return List.of("physical");
        }
        String kind = effect.strip().toLowerCase();
        return switch (kind) {
            case "acid" -> List.of("acid");
            case "fire", "burn", "ember" -> List.of("fire");
            case "frost", "rime" -> List.of("ice");
            case "water", "current" -> List.of("water");
            case "lightning", "spark" -> List.of("lightning");
            case "arcane", "rune" -> List.of("arcane");
            case "void", "shadow", "dark" -> List.of("dark");
            case "prismatic", "chaos" -> List.of("chaos");
            case "holy", "radiant" -> List.of("holy");
            case "poison" -> List.of("poison");
            case "thorn", "web", "nature" -> List.of("nature");
            case "sonic", "bone", "dust", "howl" -> List.of("physical", "dark");
            case "cleave", "slash", "fang", "claw", "volley", "pierce", "strike", "impact", "bash", "shield" -> List.of("physical");
            default -> List.of("physical");
        };
    }

    public static List<String> normalizeDamageTypes(List<String> rawTypes) {
        if (rawTypes == null || rawTypes.isEmpty()) {
            return List.of("physical");
        }
        java.util.ArrayList<String> types = new java.util.ArrayList<>();
        for (String rawType : rawTypes) {
            String type = normalizeDamageType(rawType);
            if (DAMAGE_TYPES.contains(type) && !types.contains(type)) {
                types.add(type);
            }
        }
        return types.isEmpty() ? List.of("physical") : List.copyOf(types);
    }

    public static String normalizeDamageType(String rawType) {
        if (rawType == null) {
            return "";
        }
        String type = rawType.strip().toLowerCase();
        return switch (type) {
            case "frost" -> "ice";
            case "void", "shadow" -> "dark";
            case "radiant" -> "holy";
            default -> type;
        };
    }

    public static double monsterDamageMultiplier(MonsterSpec spec, List<String> rawTypes) {
        List<String> types = normalizeDamageTypes(rawTypes);
        MonsterLore lore = loreFor(spec);
        double total = 0.0;
        for (String type : types) {
            total += monsterDamageMultiplier(lore, type);
        }
        return total / Math.max(1, types.size());
    }

    public static double monsterKnowledgeDamageMultiplier(int defeatedCount, int intelligence) {
        return 1.0 + monsterInsightPercent(defeatedCount, intelligence) / 100.0 * 0.20;
    }

    public static List<String> matchingVulnerabilities(MonsterSpec spec, List<String> rawTypes) {
        MonsterLore lore = loreFor(spec);
        List<String> types = normalizeDamageTypes(rawTypes);
        java.util.ArrayList<String> matches = new java.util.ArrayList<>();
        for (String type : types) {
            Double multiplier = lore.vulnerabilities().get(type);
            if (multiplier != null && multiplier > 1.0 && !matches.contains(type)) {
                matches.add(type);
            }
        }
        return List.copyOf(matches);
    }

    public static String monsterFlavorText(MonsterSpec spec) {
        MonsterLore lore = loreFor(spec);
        return lore.flavor() + " " + monsterBehaviorText(spec);
    }

    public static String damageTypeLabel(String type) {
        String normalized = normalizeDamageType(type);
        if (normalized.isBlank()) {
            return "Unknown";
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    public static String damageTypeListLabel(List<String> rawTypes) {
        return String.join(" + ", normalizeDamageTypes(rawTypes).stream().map(GameData::damageTypeLabel).toList());
    }

    public static String monsterModifierSummary(MonsterSpec spec) {
        MonsterLore lore = loreFor(spec);
        String weak = lore.vulnerabilities().isEmpty()
                ? "none known"
                : String.join(", ", lore.vulnerabilities().keySet().stream().map(GameData::damageTypeLabel).toList());
        String resist = lore.resistances().isEmpty()
                ? "none known"
                : String.join(", ", lore.resistances().keySet().stream().map(GameData::damageTypeLabel).toList());
        return "Weak: " + weak + ". Resists: " + resist + ".";
    }

    public static int monsterInsightLevel(int defeatedCount, int intelligence) {
        int percent = monsterInsightPercent(defeatedCount, intelligence);
        if (percent >= 85) {
            return 4;
        }
        if (percent >= 60) {
            return 3;
        }
        if (percent >= 35) {
            return 2;
        }
        if (percent >= 10) {
            return 1;
        }
        return 0;
    }

    public static int monsterInsightPercent(int defeatedCount, int intelligence) {
        int count = Math.max(0, defeatedCount);
        int smart = Math.max(1, intelligence);
        int percent = Math.min(82, count * 18) + Math.min(18, smart);
        if (count <= 0 && smart < 10) {
            return 0;
        }
        return Math.max(1, Math.min(100, percent));
    }

    private static double monsterDamageMultiplier(MonsterLore lore, String type) {
        if (lore.vulnerabilities().containsKey(type)) {
            return lore.vulnerabilities().get(type);
        }
        if (lore.resistances().containsKey(type)) {
            return lore.resistances().get(type);
        }
        return 1.0;
    }

    private static String monsterBehaviorText(MonsterSpec spec) {
        return switch (spec.key()) {
            case "slime" -> "It tests the ground with slow ripples, then surges toward warmth, metal, and careless ankles. Its body dulls toxins but shivers when cold or lightning disturbs the whole mass at once.";
            case "frost_wolf" -> "It circles until breath clouds the air between hunter and prey, then snaps for hamstrings. Fire unsettles its pack rhythm and melts the rime that protects its hide.";
            case "snow_lynx" -> "It waits above sightlines, using falling snow as cover before committing to one bright, sudden pounce. Heat ruins its concealment faster than steel does.";
            case "ice_golem" -> "It moves like a glacier deciding to become a fist, slow until the last crushing step. Flame opens fractures through the ice shell, while cold and water mostly feed its mass.";
            case "frost_troll" -> "It trusts cold-stiffened flesh and stubborn regeneration, trading wounds it expects to outlast. Sustained fire is the cleanest way to make those wounds stay honest.";
            case "ember_imp" -> "It darts in close, spits sparks, then retreats behind its own smoke and laughter. Water snuffs the furnace under its skin, making even small hits land with ugly certainty.";
            case "ember_tortoise" -> "It advances like a banked forge, patient behind a basalt shell until the enemy tires. Water and hard cold rob the shell of heat, but ordinary blows skid across it.";
            case "ash_scorpion" -> "It hides under ash crusts and strikes when footsteps shake the sand. Venom runs through the sting, but water clots the ash and spoils its footing.";
            case "fire_giant" -> "It fights like a walking kiln, using reach and heat shimmer to turn distance into punishment. Dousing magic cools the joints first, then the temper.";
            case "red_dragon" -> "It bullies the battlefield with flame, wing pressure, and the expectation that smaller things should flee. Cold interrupts the rhythm of its breath before courage interrupts anything else.";
            case "elder_dragon" -> "It reads attacks with old patience, answering weakness with flame and old malice. Holy force and cold precision are among the few things it does not simply dismiss.";
            case "skeleton" -> "It has no fear, no breath, and no soft organs to bargain with. Holy force disrupts the memory holding it upright, while poison is mostly wasted on old bone.";
            case "bone_knight" -> "It remembers drills better than death, guarding its center and punishing sloppy swings. Consecrated strikes loosen the oath inside the armor.";
            case "wraith" -> "It avoids weight and edge, dragging warmth out of the room before the real attack arrives. Holy and arcane pressure give its shape something to lose.";
            case "crypt_revenant" -> "It fights like a watchman still guarding a door that no longer exists. Fire wakes pain in the returned flesh, and holy light contests the lantern-cold will beneath it.";
            case "elder_wraith" -> "It is less a ghost than a weather system of regret, gathering silence before it breaks morale. Sacred force cuts through the hush; ordinary weapons struggle to find enough body.";
            case "thornling" -> "It lashes from cover, then roots itself before retaliation arrives. Fire burns the living lattice faster than it can braid itself back together.";
            case "moss_stag" -> "It lowers its crown and charges only after the ground itself seems to brace. Flame withers the mossy growths that feed its renewal.";
            case "bramble_boar" -> "It turns pain into direction and direction into a thorny stampede. Fire clears the bramble armor, though the charge behind it remains very real.";
            case "bog_beast" -> "It surfaces under bridges and walkways, dragging prey toward waterlogged ground. Lightning travels through the muck beautifully, which is inconvenient for the beast.";
            case "swamp_troll" -> "It uses mud as shield, medicine, and excuse to keep coming. Ice and lightning interrupt the wet regeneration that makes it so irritatingly durable.";
            case "river_eel" -> "It moves by reflex and current, biting where armor gaps touch water. Ice stiffens the current around it and turns speed into exposure.";
            case "goblin_shaman" -> "It hides behind louder goblins, tossing curses from wherever the smoke is thickest. Holy pressure breaks its hexwork, and water makes its little fire rites sulk.";
            case "crystal_hare" -> "It looks fragile until the light catches every cutting angle at once. Forceful hits and chaos magic crack the pattern it uses to bend harm away.";
            case "glass_scorpion" -> "It waits perfectly still, relying on glare and venom to make caution arrive too late. Heavy impacts and chaotic force fracture the mirrored carapace.";
            case "sand_stalker" -> "It follows shade, not tracks, and attacks when the sun hides the warning. Water collapses its dust veil and leaves the hunter much easier to read.";
            case "marsh_drake" -> "It fights low to the ground, breathing rot and acid before closing with teeth. Lightning punishes the wet scales, while acid only makes it meaner.";
            case "mountain_drake" -> "It uses ridges like walls, forcing enemies into bad angles before the bite. Arcane pressure finds cracks that ordinary blades must work harder to open.";
            case "stone_giant" -> "It is a landslide with opinions, slow to turn but dreadful once committed. Water, roots, and chaos all exploit fractures that brute force barely notices.";
            case "stoneback_goat" -> "It sets its hooves and trusts its armored spine to absorb the answer. Water-slick footing makes even this stubborn climber misjudge a charge.";
            case "orc_shaman" -> "It mixes war smoke with rot-prayers, weakening victims before the raiders arrive. Holy damage tears through the ritual knots holding its curses together.";
            case "goblin_king" -> "It survives by making every nearby goblin more dangerous, then blaming them when the plan fails. Holy and chaos damage both make the stolen crown feel less convincing.";
            default -> defaultMonsterBehaviorText(spec);
        };
    }

    private static String defaultMonsterBehaviorText(MonsterSpec spec) {
        return switch (spec.species()) {
            case "beast" -> "It watches posture, scent, and hesitation more than weapons. Repeated fights teach where it commits its weight before striking.";
            case "bat" -> "It attacks in ugly little arcs, using sound and ceiling shadows to confuse distance. Calm timing matters more than chasing every flutter.";
            case "bandit" -> "It fights like someone who expects tricks to solve courage. Pressure and clean target selection make the plan come apart.";
            case "dragon", "drake" -> "It uses scale, breath, and reach to make the fight happen on its terms. Studying the rhythm of its breath reveals the safer openings.";
            case "goblin" -> "It prefers unfair angles, cheap shots, and retreat routes already chosen. The more you fight them, the easier their little feints are to read.";
            case "giant" -> "Its attacks are simple in shape and enormous in consequence. Knowledge helps you punish the recovery after each overcommitted swing.";
            case "insect" -> "It trusts armor, venom, and sudden movement. Watch the limbs rather than the head and its intent becomes clearer.";
            case "orc" -> "It leans into momentum and intimidation, often overvaluing the next heavy blow. Familiarity turns that aggression into predictable openings.";
            case "reptile" -> "It stays still until stillness becomes speed. Patient observation is the difference between reading the lunge and wearing it.";
            case "troll" -> "It expects to win the long exchange. Learned pressure keeps its healing from becoming a second health bar.";
            case "undead" -> "It lacks fear but not pattern. Once the binding logic is understood, every strike can aim at the thing holding it together.";
            case "elemental" -> "It is less animal than rule, bound to the element that shaped it. Understanding that rule turns spectacle into target practice.";
            default -> "Each encounter adds a little more shape to rumor: how it moves, what it trusts, and where that trust can be broken.";
        };
    }

    private static List<String> defaultMonsterDamageTypes(MonsterSpec spec) {
        return switch (spec.species()) {
            case "dragon" -> List.of("fire", "physical");
            case "undead" -> List.of("dark", "physical");
            case "plant" -> List.of("nature");
            case "reptile", "drake" -> List.of("physical", "acid");
            case "insect" -> List.of("physical", "poison");
            case "elemental" -> spec.key().contains("ice") ? List.of("ice", "physical") : List.of("fire", "chaos");
            default -> List.of("physical");
        };
    }

    private static String defaultMonsterFlavor(MonsterSpec spec) {
        return switch (spec.species()) {
            case "beast" -> "A wild thing of tooth, hoof, or claw, reading the road by scent and fear.";
            case "bat" -> "A fluttering shape from cave-dark ceilings and old rafters.";
            case "bandit" -> "Human trouble with a blade, a plan, and flexible morals.";
            case "dragon", "drake" -> "Scale, hunger, and old fire folded into a hunting shape.";
            case "goblin" -> "Small, quick, and happiest when the fight is unfair.";
            case "giant" -> "Too large for most sensible tactics.";
            case "insect" -> "Chitin, venom, and a patient little hunting mind.";
            case "orc" -> "A hard-shouldered raider bred by war roads and louder war songs.";
            case "reptile" -> "Cold eyes, sudden motion, and a bite worth respecting.";
            case "troll" -> "Heavy muscle and worse regeneration than anyone asked for.";
            case "undead" -> "The dead seldom fight fairly. They have had too much time to practice.";
            case "elemental" -> "Raw weather and old stone forced into a body.";
            default -> "A hostile creature with habits still waiting to be studied.";
        };
    }

    private static Map.Entry<String, MonsterLore> lore(
            String key,
            String flavor,
            List<String> damageTypes,
            Map<String, Double> vulnerabilities,
            Map<String, Double> resistances
    ) {
        return Map.entry(key, new MonsterLore(
                flavor,
                normalizeDamageTypes(damageTypes),
                normalizedModifierMap(vulnerabilities),
                normalizedModifierMap(resistances)
        ));
    }

    private static Map<String, Double> normalizedModifierMap(Map<String, Double> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Double> normalized = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : modifiers.entrySet()) {
            String type = normalizeDamageType(entry.getKey());
            if (DAMAGE_TYPES.contains(type) && entry.getValue() != null && entry.getValue() > 0.0) {
                normalized.put(type, entry.getValue());
            }
        }
        return Map.copyOf(normalized);
    }

    public record MonsterLore(
            String flavor,
            List<String> damageTypes,
            Map<String, Double> vulnerabilities,
            Map<String, Double> resistances
    ) {
    }

    public record MonsterSpec(String key, String name, String species, String sprite, int hp, int attack, int defense, int xp, int gold) {
        public Actor createActor() {
            return new Actor(name, sprite, "Monster", hp, 0, attack, defense);
        }
    }

    public record RecruitSpec(
            String id,
            String name,
            String sprite,
            String className,
            int hp,
            int mp,
            int attack,
            int defense,
            List<Ability> abilities,
            Map<String, Integer> inventory
    ) {
        public Actor createActor() {
            Actor actor = new Actor(name, sprite, className, hp, mp, attack, defense);
            actor.abilities.addAll(classAbilities(className));
            actor.abilities.addAll(abilities);
            for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
                actor.addItem(entry.getKey(), entry.getValue());
            }
            return actor;
        }
    }
}
