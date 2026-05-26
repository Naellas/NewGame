package com.alderfall.game;

import java.util.List;
import java.util.Map;

public final class MonsterAbilities {
    private static final Map<String, List<MonsterAbility>> ABILITIES = Map.ofEntries(
            ability("slime", new MonsterAbility("Slime Splash", 5, 0.36, "acid", List.of(new MonsterAbilityStatus("weak", 0.30)))),
            ability("wolf", new MonsterAbility("Hamstring Bite", 8, 0.34, "fang", List.of(new MonsterAbilityStatus("weak", 0.45)))),
            ability("bat", new MonsterAbility("Shriek", 6, 0.35, "sonic", List.of(new MonsterAbilityStatus("weak", 0.35)))),
            ability("skeleton", new MonsterAbility("Bone Rattle", 7, 0.35, "bone", List.of(new MonsterAbilityStatus("vulnerable", 0.30)))),
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
            ability("spider", new MonsterAbility("Web Spit", 10, 0.40, "web", List.of(new MonsterAbilityStatus("weak", 0.55)))),
            ability("wraith", new MonsterAbility("Ashen Hex", 13, 0.42, "shadow", List.of(new MonsterAbilityStatus("weak", 0.55)))),
            ability("orc", new MonsterAbility("Brutal Cleave", 13, 0.38, "cleave", List.of(new MonsterAbilityStatus("vulnerable", 0.35)))),
            ability("thornling",
                    new MonsterAbility("Briar Lash", 12, 0.42, "thorn", List.of(new MonsterAbilityStatus("poison", 0.45))),
                    new MonsterAbility("Rootskin", 0, 0.28, MonsterAbility.Kind.BUFF, "ward", 4, 0.60, List.of(new MonsterAbilityStatus("regeneration", "self", 1.0, 4)))),
            ability("sand_stalker",
                    new MonsterAbility("Sand Veil", 11, 0.44, "dust", List.of(new MonsterAbilityStatus("weak", 0.60))),
                    new MonsterAbility("Burrow Strike", 18, 0.30, MonsterAbility.Kind.DAMAGE, "fang", 3, null, List.of(new MonsterAbilityStatus("vulnerable", 0.40)))),
            ability("ice_golem",
                    new MonsterAbility("Glacier Slam", 24, 0.42, "frost", List.of(new MonsterAbilityStatus("weak", 0.80))),
                    new MonsterAbility("Icebound Shell", 0, 0.30, MonsterAbility.Kind.BUFF, "ward", 4, 0.65,
                            List.of(new MonsterAbilityStatus("shield", "self", 1.0, 12), new MonsterAbilityStatus("fortified", "self")))),
            ability("bog_beast",
                    new MonsterAbility("Mire Drag", 20, 0.44, "acid",
                            List.of(new MonsterAbilityStatus("poison", 0.55), new MonsterAbilityStatus("weak", 0.40))),
                    new MonsterAbility("Swamp Renewal", 0, 0.26, MonsterAbility.Kind.BUFF, "ward", 4, 0.50,
                            List.of(new MonsterAbilityStatus("regeneration", "self", 1.0, 7)))),
            ability("ember_imp",
                    new MonsterAbility("Spark Spit", 16, 0.48, "fire", List.of(new MonsterAbilityStatus("burn", 0.75))),
                    new MonsterAbility("Kindle Hide", 0, 0.26, MonsterAbility.Kind.BUFF, "fire", 3, 0.55,
                            List.of(new MonsterAbilityStatus("haste", "self"))))
    );

    private MonsterAbilities() {
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
}
