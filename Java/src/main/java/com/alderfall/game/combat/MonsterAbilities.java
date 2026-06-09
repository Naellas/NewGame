package com.alderfall.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MonsterAbilities {
    private static final Map<String, List<MonsterAbility>> ABILITIES = Map.ofEntries(
            ability("slime",
                    new MonsterAbility("Slime Splash", 5, 0.36, "acid", List.of(new MonsterAbilityStatus("weak", 0.30))),
                    new MonsterAbility("Split Skin", 0, 0.26, MonsterAbility.Kind.BUFF, "acid", 4, 0.52,
                            List.of(new MonsterAbilityStatus("shield", "self", 1.0, 8), new MonsterAbilityStatus("regeneration", "self", 1.0, 3)))),
            ability("sheep",
                    new MonsterAbility("Panic Kick", 7, 0.32, "impact", List.of(new MonsterAbilityStatus("weak", 0.25))),
                    new MonsterAbility("Fleece Guard", 0, 0.20, MonsterAbility.Kind.BUFF, "ward", 4, 0.45,
                            List.of(new MonsterAbilityStatus("shield", "self", 1.0, 6)))),
            ability("doe",
                    new MonsterAbility("Startled Hoof", 8, 0.34, "impact", List.of(new MonsterAbilityStatus("vulnerable", 0.28))),
                    new MonsterAbility("Forest Sprint", 0, 0.22, MonsterAbility.Kind.BUFF, "dust", 3, null,
                            List.of(new MonsterAbilityStatus("haste", "self")))),
            ability("mountain_goat",
                    new MonsterAbility("Cliff Bash", 10, 0.36, "impact", List.of(new MonsterAbilityStatus("vulnerable", 0.32))),
                    new MonsterAbility("Surefoot Brace", 0, 0.22, MonsterAbility.Kind.BUFF, "ward", 3, null,
                            List.of(new MonsterAbilityStatus("fortified", "self")))),
            ability("wolf",
                    new MonsterAbility("Hamstring Bite", 8, 0.34, "fang", List.of(new MonsterAbilityStatus("weak", 0.45))),
                    new MonsterAbility("Pack Howl", 0, 0.22, MonsterAbility.Kind.BUFF, "sonic", 3, null,
                            List.of(new MonsterAbilityStatus("haste", "self")))),
            ability("frost_wolf",
                    new MonsterAbility("Rime Bite", 12, 0.42, "frost", List.of(new MonsterAbilityStatus("weak", 0.60))),
                    new MonsterAbility("White Hide", 0, 0.24, MonsterAbility.Kind.BUFF, "frost", 4, 0.58,
                            List.of(new MonsterAbilityStatus("fortified", "self"), new MonsterAbilityStatus("shield", "self", 1.0, 8)))),
            ability("meadow_wolf",
                    new MonsterAbility("Pack Snap", 9, 0.36, "fang", List.of(new MonsterAbilityStatus("vulnerable", 0.30))),
                    new MonsterAbility("Low Circle", 0, 0.20, MonsterAbility.Kind.BUFF, "dust", 3, null,
                            List.of(new MonsterAbilityStatus("haste", "self")))),
            ability("stag",
                    new MonsterAbility("Crown Rush", 14, 0.38, "fang", List.of(new MonsterAbilityStatus("vulnerable", 0.36))),
                    new MonsterAbility("Proud Stand", 0, 0.22, MonsterAbility.Kind.BUFF, "ward", 4, 0.55,
                            List.of(new MonsterAbilityStatus("fortified", "self"), new MonsterAbilityStatus("shield", "self", 1.0, 8)))),
            ability("bat",
                    new MonsterAbility("Shriek", 6, 0.35, "sonic", List.of(new MonsterAbilityStatus("weak", 0.35))),
                    new MonsterAbility("Darting Wings", 0, 0.22, MonsterAbility.Kind.BUFF, "dust", 3, null,
                            List.of(new MonsterAbilityStatus("haste", "self")))),
            ability("crypt_bat", new MonsterAbility("Grave Shriek", 9, 0.38, "sonic", List.of(new MonsterAbilityStatus("weak", 0.45)))),
            ability("skeleton",
                    new MonsterAbility("Bone Rattle", 7, 0.35, "bone", List.of(new MonsterAbilityStatus("vulnerable", 0.30))),
                    new MonsterAbility("Grave Guard", 0, 0.22, MonsterAbility.Kind.BUFF, "ward", 4, 0.50,
                            List.of(new MonsterAbilityStatus("fortified", "self"), new MonsterAbilityStatus("shield", "self", 1.0, 7)))),
            ability("goblin", new MonsterAbility("Dirty Trick", 9, 0.36, "dust", List.of(new MonsterAbilityStatus("weak", 0.35)))),
            ability("goblin_scout", new MonsterAbility("Knife Feint", 8, 0.38, "slash", List.of(new MonsterAbilityStatus("vulnerable", 0.25)))),
            ability("goblin_archer",
                    new MonsterAbility("Barbed Arrow", 11, 0.44, "pierce", List.of(new MonsterAbilityStatus("weak", 0.35))),
                    new MonsterAbility("Duck Away", 0, 0.24, MonsterAbility.Kind.BUFF, "dust", 3, null, List.of(new MonsterAbilityStatus("haste", "self")))),
            ability("goblin_trapper",
                    new MonsterAbility("Snare Cord", 12, 0.42, "web", List.of(new MonsterAbilityStatus("weak", 0.50))),
                    new MonsterAbility("Barbed Hook", 14, 0.30, "pierce", List.of(new MonsterAbilityStatus("vulnerable", 0.35)))),
            ability("goblin_skirmisher",
                    new MonsterAbility("Flanking Cut", 15, 0.42, "slash", List.of(new MonsterAbilityStatus("vulnerable", 0.38))),
                    new MonsterAbility("Quick Step", 0, 0.22, MonsterAbility.Kind.BUFF, "dust", 3, null, List.of(new MonsterAbilityStatus("haste", "self")))),
            ability("goblin_shaman",
                    new MonsterAbility("Hex Pebble", 16, 0.44, "shadow", List.of(new MonsterAbilityStatus("weak", 0.55))),
                    new MonsterAbility("Campfire Charm", 0, 0.28, MonsterAbility.Kind.BUFF, "fire", 3, 0.45,
                            List.of(new MonsterAbilityStatus("regeneration", "self", 1.0, 5)))),
            ability("hobgoblin_guard",
                    new MonsterAbility("Shield Slam", 17, 0.40, "shield", List.of(new MonsterAbilityStatus("vulnerable", 0.35))),
                    new MonsterAbility("Hold Line", 0, 0.24, MonsterAbility.Kind.BUFF, "ward", 3, null,
                            List.of(new MonsterAbilityStatus("fortified", "self")))),
            ability("goblin_warlord",
                    new MonsterAbility("Commanding Chop", 22, 0.42, "cleave", List.of(new MonsterAbilityStatus("vulnerable", 0.45))),
                    new MonsterAbility("War Banner", 0, 0.26, MonsterAbility.Kind.BUFF, "ward", 3, null,
                            List.of(new MonsterAbilityStatus("haste", "self"), new MonsterAbilityStatus("fortified", "self")))),
            ability("goblin_king",
                    new MonsterAbility("Crownbreaker", 27, 0.46, "cleave", List.of(new MonsterAbilityStatus("vulnerable", 0.55))),
                    new MonsterAbility("Royal Spite", 20, 0.34, "dust", List.of(new MonsterAbilityStatus("weak", 0.65))),
                    new MonsterAbility("Stolen Standard", 0, 0.26, MonsterAbility.Kind.BUFF, "ward", 4, 0.55,
                            List.of(new MonsterAbilityStatus("shield", "self", 1.0, 14), new MonsterAbilityStatus("regeneration", "self", 1.0, 6)))),
            ability("bandit_cutthroat",
                    new MonsterAbility("Low Slash", 13, 0.42, "slash", List.of(new MonsterAbilityStatus("weak", 0.35)))),
            ability("bandit_archer",
                    new MonsterAbility("Pinning Shot", 14, 0.42, "pierce", List.of(new MonsterAbilityStatus("weak", 0.45))),
                    new MonsterAbility("Fade Back", 0, 0.22, MonsterAbility.Kind.BUFF, "dust", 3, null,
                            List.of(new MonsterAbilityStatus("haste", "self")))),
            ability("bandit_captain",
                    new MonsterAbility("Captain's Cut", 20, 0.44, "slash", List.of(new MonsterAbilityStatus("vulnerable", 0.45))),
                    new MonsterAbility("Black Banner", 0, 0.26, MonsterAbility.Kind.BUFF, "ward", 3, 0.50,
                            List.of(new MonsterAbilityStatus("fortified", "self"), new MonsterAbilityStatus("haste", "self")))),
            ability("red_dragon",
                    new MonsterAbility("Flame Bite", 30, 0.46, "fire", List.of(new MonsterAbilityStatus("burn", 0.75))),
                    new MonsterAbility("Wing Buffet", 20, 0.30, "dust", List.of(new MonsterAbilityStatus("weak", 0.55)))),
            ability("elder_dragon",
                    new MonsterAbility("Elderflame", 38, 0.48, "fire", List.of(new MonsterAbilityStatus("burn", 0.85))),
                    new MonsterAbility("Ancient Dread", 26, 0.34, "shadow", List.of(new MonsterAbilityStatus("vulnerable", 0.55))),
                    new MonsterAbility("Scale Ward", 0, 0.24, MonsterAbility.Kind.BUFF, "ward", 4, 0.60,
                            List.of(new MonsterAbilityStatus("shield", "self", 1.0, 18), new MonsterAbilityStatus("fortified", "self")))),
            ability("marsh_drake",
                    new MonsterAbility("Bog Breath", 20, 0.44, "acid",
                            List.of(new MonsterAbilityStatus("poison", 0.55), new MonsterAbilityStatus("weak", 0.35)))),
            ability("mountain_drake",
                    new MonsterAbility("Crag Bite", 23, 0.42, "cleave", List.of(new MonsterAbilityStatus("vulnerable", 0.42))),
                    new MonsterAbility("Ridge Guard", 0, 0.22, MonsterAbility.Kind.BUFF, "ward", 3, null,
                            List.of(new MonsterAbilityStatus("fortified", "self")))),
            ability("spider", new MonsterAbility("Web Spit", 10, 0.40, "web", List.of(new MonsterAbilityStatus("weak", 0.55)))),
            ability("wraith", new MonsterAbility("Ashen Hex", 13, 0.42, "shadow", List.of(new MonsterAbilityStatus("weak", 0.55)))),
            ability("orc", new MonsterAbility("Brutal Cleave", 13, 0.38, "cleave", List.of(new MonsterAbilityStatus("vulnerable", 0.35)))),
            ability("orc_raider",
                    new MonsterAbility("Raider Chop", 16, 0.40, "cleave", List.of(new MonsterAbilityStatus("vulnerable", 0.38)))),
            ability("orc_berserker",
                    new MonsterAbility("Frenzy Hew", 22, 0.44, "cleave", List.of(new MonsterAbilityStatus("vulnerable", 0.46))),
                    new MonsterAbility("Blood Heat", 0, 0.26, MonsterAbility.Kind.BUFF, "ward", 3, null,
                            List.of(new MonsterAbilityStatus("haste", "self")))),
            ability("orc_shaman",
                    new MonsterAbility("Rot Hex", 19, 0.44, "shadow", List.of(new MonsterAbilityStatus("weak", 0.55))),
                    new MonsterAbility("War Smoke", 0, 0.24, MonsterAbility.Kind.BUFF, "dust", 3, null,
                            List.of(new MonsterAbilityStatus("shield", "self", 1.0, 10)))),
            ability("orc_shieldbearer",
                    new MonsterAbility("Shield Hook", 18, 0.40, "shield", List.of(new MonsterAbilityStatus("vulnerable", 0.38))),
                    new MonsterAbility("Brace Wall", 0, 0.28, MonsterAbility.Kind.BUFF, "ward", 3, null,
                            List.of(new MonsterAbilityStatus("fortified", "self"), new MonsterAbilityStatus("shield", "self", 1.0, 10)))),
            ability("bone_knight",
                    new MonsterAbility("Grave Cleave", 21, 0.44, "bone", List.of(new MonsterAbilityStatus("vulnerable", 0.48))),
                    new MonsterAbility("Oath of Dust", 0, 0.24, MonsterAbility.Kind.BUFF, "ward", 4, 0.55,
                            List.of(new MonsterAbilityStatus("fortified", "self"), new MonsterAbilityStatus("shield", "self", 1.0, 10)))),
            ability("crypt_revenant",
                    new MonsterAbility("Lantern Hex", 23, 0.44, "shadow", List.of(new MonsterAbilityStatus("weak", 0.62))),
                    new MonsterAbility("Cold Return", 0, 0.24, MonsterAbility.Kind.BUFF, "frost", 4, 0.50,
                            List.of(new MonsterAbilityStatus("regeneration", "self", 1.0, 6)))),
            ability("elder_wraith",
                    new MonsterAbility("Hollow Crown", 28, 0.46, "shadow", List.of(new MonsterAbilityStatus("weak", 0.70))),
                    new MonsterAbility("Grave Tide", 18, 0.30, "frost", List.of(new MonsterAbilityStatus("vulnerable", 0.45))),
                    new MonsterAbility("Unquiet Veil", 0, 0.26, MonsterAbility.Kind.BUFF, "ward", 4, 0.55,
                            List.of(new MonsterAbilityStatus("shield", "self", 1.0, 16), new MonsterAbilityStatus("regeneration", "self", 1.0, 7)))),
            ability("orc_champion",
                    new MonsterAbility("Champion's Break", 25, 0.45, "cleave", List.of(new MonsterAbilityStatus("vulnerable", 0.55))),
                    new MonsterAbility("Blood Roar", 0, 0.24, MonsterAbility.Kind.BUFF, "ward", 4, 0.60,
                            List.of(new MonsterAbilityStatus("haste", "self"), new MonsterAbilityStatus("fortified", "self")))),
            ability("thornling",
                    new MonsterAbility("Briar Lash", 12, 0.42, "thorn", List.of(new MonsterAbilityStatus("poison", 0.45))),
                    new MonsterAbility("Rootskin", 0, 0.28, MonsterAbility.Kind.BUFF, "ward", 4, 0.60, List.of(new MonsterAbilityStatus("regeneration", "self", 1.0, 4)))),
            ability("moss_stag",
                    new MonsterAbility("Antler Rush", 13, 0.38, "fang", List.of(new MonsterAbilityStatus("vulnerable", 0.35))),
                    new MonsterAbility("Green Renewal", 0, 0.22, MonsterAbility.Kind.BUFF, "ward", 3, 0.50,
                            List.of(new MonsterAbilityStatus("regeneration", "self", 1.0, 4)))),
            ability("crystal_hare",
                    new MonsterAbility("Prism Kick", 12, 0.40, "arcane", List.of(new MonsterAbilityStatus("vulnerable", 0.35))),
                    new MonsterAbility("Shard Flash", 0, 0.24, MonsterAbility.Kind.BUFF, "ward", 3, null,
                            List.of(new MonsterAbilityStatus("haste", "self")))),
            ability("bramble_boar",
                    new MonsterAbility("Thorn Charge", 17, 0.42, "thorn",
                            List.of(new MonsterAbilityStatus("poison", 0.35), new MonsterAbilityStatus("vulnerable", 0.35)))),
            ability("sand_stalker",
                    new MonsterAbility("Sand Veil", 11, 0.44, "dust", List.of(new MonsterAbilityStatus("weak", 0.60))),
                    new MonsterAbility("Burrow Strike", 18, 0.30, MonsterAbility.Kind.DAMAGE, "fang", 3, null, List.of(new MonsterAbilityStatus("vulnerable", 0.40)))),
            ability("glass_scorpion",
                    new MonsterAbility("Glass Sting", 16, 0.44, "poison", List.of(new MonsterAbilityStatus("poison", 0.65))),
                    new MonsterAbility("Mirror Carapace", 0, 0.24, MonsterAbility.Kind.BUFF, "ward", 4, 0.58,
                            List.of(new MonsterAbilityStatus("shield", "self", 1.0, 10), new MonsterAbilityStatus("fortified", "self")))),
            ability("ice_golem",
                    new MonsterAbility("Glacier Slam", 24, 0.42, "frost", List.of(new MonsterAbilityStatus("weak", 0.80))),
                    new MonsterAbility("Icebound Shell", 0, 0.30, MonsterAbility.Kind.BUFF, "ward", 4, 0.65,
                            List.of(new MonsterAbilityStatus("shield", "self", 1.0, 12), new MonsterAbilityStatus("fortified", "self")))),
            ability("stoneback_goat",
                    new MonsterAbility("Stone Horn", 16, 0.40, "cleave", List.of(new MonsterAbilityStatus("vulnerable", 0.35)))),
            ability("snow_lynx",
                    new MonsterAbility("Whiteout Pounce", 18, 0.42, "frost",
                            List.of(new MonsterAbilityStatus("weak", 0.45), new MonsterAbilityStatus("vulnerable", 0.35)))),
            ability("bog_beast",
                    new MonsterAbility("Mire Drag", 20, 0.44, "acid",
                            List.of(new MonsterAbilityStatus("poison", 0.55), new MonsterAbilityStatus("weak", 0.40))),
                    new MonsterAbility("Swamp Renewal", 0, 0.26, MonsterAbility.Kind.BUFF, "ward", 4, 0.50,
                            List.of(new MonsterAbilityStatus("regeneration", "self", 1.0, 7)))),
            ability("swamp_troll",
                    new MonsterAbility("Fen Club", 24, 0.42, "acid",
                            List.of(new MonsterAbilityStatus("poison", 0.45), new MonsterAbilityStatus("vulnerable", 0.35))),
                    new MonsterAbility("Muck Renewal", 0, 0.28, MonsterAbility.Kind.BUFF, "ward", 4, 0.60,
                            List.of(new MonsterAbilityStatus("regeneration", "self", 1.0, 8)))),
            ability("frost_troll",
                    new MonsterAbility("Ice Maul", 26, 0.42, "frost", List.of(new MonsterAbilityStatus("weak", 0.70))),
                    new MonsterAbility("Cold Hide", 0, 0.26, MonsterAbility.Kind.BUFF, "ward", 4, 0.55,
                            List.of(new MonsterAbilityStatus("shield", "self", 1.0, 12), new MonsterAbilityStatus("fortified", "self")))),
            ability("hill_giant",
                    new MonsterAbility("Treeclub Smash", 27, 0.40, "cleave", List.of(new MonsterAbilityStatus("vulnerable", 0.42)))),
            ability("stone_giant",
                    new MonsterAbility("Boulder Fist", 30, 0.42, "cleave", List.of(new MonsterAbilityStatus("vulnerable", 0.48))),
                    new MonsterAbility("Stonehide", 0, 0.24, MonsterAbility.Kind.BUFF, "ward", 4, null,
                            List.of(new MonsterAbilityStatus("fortified", "self"), new MonsterAbilityStatus("shield", "self", 1.0, 12)))),
            ability("fire_giant",
                    new MonsterAbility("Cinder Maul", 33, 0.44, "fire",
                            List.of(new MonsterAbilityStatus("burn", 0.70), new MonsterAbilityStatus("vulnerable", 0.35))),
                    new MonsterAbility("Heat Shimmer", 0, 0.22, MonsterAbility.Kind.BUFF, "fire", 4, 0.62,
                            List.of(new MonsterAbilityStatus("haste", "self"), new MonsterAbilityStatus("shield", "self", 1.0, 12)))),
            ability("reed_serpent",
                    new MonsterAbility("Reed Coil", 15, 0.42, "acid",
                            List.of(new MonsterAbilityStatus("weak", 0.40), new MonsterAbilityStatus("poison", 0.45)))),
            ability("river_eel",
                    new MonsterAbility("Current Lash", 11, 0.38, "acid", List.of(new MonsterAbilityStatus("weak", 0.40))),
                    new MonsterAbility("Numbing Coil", 13, 0.34, "sonic", List.of(new MonsterAbilityStatus("vulnerable", 0.38)))),
            ability("ash_scorpion",
                    new MonsterAbility("Cinder Sting", 18, 0.44, "fire",
                            List.of(new MonsterAbilityStatus("burn", 0.55), new MonsterAbilityStatus("poison", 0.35)))),
            ability("ember_tortoise",
                    new MonsterAbility("Coal Snap", 15, 0.38, "fire", List.of(new MonsterAbilityStatus("burn", 0.45))),
                    new MonsterAbility("Basalt Shell", 0, 0.28, MonsterAbility.Kind.BUFF, "ward", 4, 0.60,
                            List.of(new MonsterAbilityStatus("shield", "self", 1.0, 10), new MonsterAbilityStatus("fortified", "self")))),
            ability("ember_imp",
                    new MonsterAbility("Spark Spit", 16, 0.48, "fire", List.of(new MonsterAbilityStatus("burn", 0.75))),
                    new MonsterAbility("Kindle Hide", 0, 0.26, MonsterAbility.Kind.BUFF, "fire", 3, 0.55,
                            List.of(new MonsterAbilityStatus("haste", "self"))))
    );

    private MonsterAbilities() {
    }

    public static List<MonsterAbility> eliteAbilitiesFor(GameData.MonsterSpec monsterSpec) {
        int strikePower = Math.max(12, monsterSpec.attack() + 6);
        int shieldPower = Math.max(9, monsterSpec.defense() + 8);
        String strikeEffect = switch (monsterSpec.species()) {
            case "dragon", "elemental" -> "fire";
            case "undead" -> "shadow";
            case "insect", "reptile", "plant" -> "poison";
            case "bat" -> "sonic";
            case "giant", "orc" -> "cleave";
            default -> "slash";
        };
        return List.of(
                new MonsterAbility("Exploit Opening", strikePower, 0.36, strikeEffect,
                        List.of(new MonsterAbilityStatus("vulnerable", 0.55))),
                new MonsterAbility("Elite Focus", 0, 0.24, MonsterAbility.Kind.BUFF, "ward", 4, 0.62,
                        List.of(new MonsterAbilityStatus("shield", "self", 1.0, shieldPower),
                                new MonsterAbilityStatus("haste", "self")))
        );
    }

    public static List<MonsterAbility> bossAbilitiesFor(GameData.MonsterSpec monsterSpec, int phase) {
        int bossPower = Math.max(18, monsterSpec.attack() + 8 + phase * 4);
        String key = monsterSpec.key();
        String effect = bossEffectFor(monsterSpec);
        List<MonsterAbility> abilities = new ArrayList<>();
        abilities.add(new MonsterAbility("Boss Pressure", bossPower, 0.30 + phase * 0.04, effect, List.of(
                new MonsterAbilityStatus("vulnerable", 0.45 + phase * 0.08)
        )));
        abilities.add(new MonsterAbility("Commanding Presence", 0, 0.18 + phase * 0.03,
                MonsterAbility.Kind.BUFF, "ward", 4, phase >= 2 ? 0.78 : 0.62, List.of(
                new MonsterAbilityStatus("shield", "self", 1.0, Math.max(14, monsterSpec.defense() + 12 + phase * 4)),
                new MonsterAbilityStatus("fortified", "self")
        )));
        if (phase >= 2) {
            abilities.add(new MonsterAbility(bossPhaseTwoName(key), bossPower + 5, 0.36, effect, List.of(
                    new MonsterAbilityStatus("weak", 0.58),
                    new MonsterAbilityStatus("vulnerable", 0.38)
            )));
        }
        if (phase >= 3) {
            abilities.add(new MonsterAbility(bossFinalName(key), bossPower + 11, 0.42, effect, List.of(
                    new MonsterAbilityStatus("weak", 0.72),
                    new MonsterAbilityStatus("burn".equals(effect) || "fire".equals(effect) ? "burn" : "vulnerable", 0.58)
            )));
        }
        return List.copyOf(abilities);
    }

    public static List<MonsterAbility> forMonster(GameData.MonsterSpec monsterSpec) {
        List<MonsterAbility> abilities = ABILITIES.get(monsterSpec.key());
        if (abilities != null) {
            return abilities;
        }
        return ABILITIES.getOrDefault(monsterSpec.sprite(), List.of());
    }

    private static Map.Entry<String, List<MonsterAbility>> ability(String key, MonsterAbility... abilities) {
        return Map.entry(key, List.of(abilities));
    }

    private static String bossEffectFor(GameData.MonsterSpec monsterSpec) {
        String key = monsterSpec.key();
        if (key.contains("dragon") || key.contains("flame") || key.contains("cinder")) {
            return "fire";
        }
        if (key.contains("frost")) {
            return "frost";
        }
        if (key.contains("bell") || key.contains("demon") || key.contains("void") || key.contains("shadow")) {
            return "shadow";
        }
        if (key.contains("king") || key.contains("banner")) {
            return "cleave";
        }
        return switch (monsterSpec.species()) {
            case "dragon" -> "fire";
            case "undead", "demon" -> "shadow";
            case "giant", "orc", "goblin" -> "cleave";
            default -> "slash";
        };
    }

    private static String bossPhaseTwoName(String key) {
        if (key.contains("dragon")) {
            return "Wing Tyranny";
        }
        if (key.contains("queen") || key.contains("vaelthara")) {
            return "Mercy Edict";
        }
        if (key.contains("king")) {
            return "Royal Decree";
        }
        return "Phase Break";
    }

    private static String bossFinalName(String key) {
        if (key.contains("dragon")) {
            return "Worldfire Breath";
        }
        if (key.contains("queen") || key.contains("vaelthara")) {
            return "Crown of Mercy";
        }
        if (key.contains("king")) {
            return "Stolen Crown";
        }
        return "Final Edict";
    }
}
