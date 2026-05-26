package com.alderfall.game;

import java.util.List;
import java.util.Map;

public final class GameData {
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
                    new Ability("Firebolt", 22, 6, Ability.AbilityKind.DAMAGE),
                    new Ability("Frost Lance", 18, 5, Ability.AbilityKind.DAMAGE),
                    new Ability("Mend", 18, 5, Ability.AbilityKind.HEAL)
            );
            case "Ranger" -> List.of(
                    new Ability("Piercing Shot", 18, 5, Ability.AbilityKind.DAMAGE),
                    new Ability("Rain Volley", 15, 4, Ability.AbilityKind.DAMAGE),
                    new Ability("Herbal Remedy", 16, 5, Ability.AbilityKind.HEAL)
            );
            default -> List.of(
                    new Ability("Shield Bash", 16, 4, Ability.AbilityKind.DAMAGE),
                    new Ability("Bulwark", 0, 3, Ability.AbilityKind.DEFEND),
                    new Ability("First Aid", 14, 5, Ability.AbilityKind.HEAL)
            );
        };
    }

    public static final Map<String, MonsterSpec> MONSTERS = Map.ofEntries(
            entry("slime", "Bog Slime", "slime", 24, 7, 1, 8, 4),
            entry("wolf", "Grey Wolf", "wolf", 32, 10, 2, 12, 7),
            entry("bat", "Cave Bat", "bat", 22, 9, 1, 10, 5),
            entry("skeleton", "Restless Skeleton", "skeleton", 42, 12, 3, 18, 12),
            entry("goblin", "Goblin Raider", "goblin", 36, 11, 2, 15, 10),
            entry("goblin_scout", "Goblin Scout", "goblin_scout", 30, 10, 1, 13, 8),
            entry("goblin_archer", "Goblin Archer", "goblin_archer", 34, 13, 1, 17, 12),
            entry("goblin_trapper", "Goblin Trapper", "goblin_trapper", 38, 13, 2, 20, 15),
            entry("goblin_skirmisher", "Goblin Skirmisher", "goblin_skirmisher", 44, 15, 3, 25, 18),
            entry("goblin_shaman", "Goblin Shaman", "goblin_shaman", 48, 16, 3, 34, 26),
            entry("hobgoblin_guard", "Hobgoblin Guard", "hobgoblin_guard", 62, 18, 6, 44, 34),
            entry("goblin_warlord", "Goblin Warlord", "goblin_warlord", 82, 22, 7, 70, 58),
            entry("goblin_king", "Goblin King", "goblin_king", 116, 27, 9, 120, 95),
            entry("spider", "Cave Spider", "spider", 52, 14, 4, 23, 16),
            entry("wraith", "Ash Wraith", "wraith", 58, 17, 4, 30, 21),
            entry("orc", "Orc Brute", "orc", 66, 18, 5, 34, 28),
            entry("thornling", "Briar Thornling", "thornling", 44, 13, 4, 20, 13),
            entry("sand_stalker", "Sand Stalker", "sand_stalker", 62, 19, 5, 36, 30),
            entry("ice_golem", "Ice Golem", "ice_golem", 96, 25, 11, 72, 66),
            entry("bog_beast", "Bog Beast", "bog_beast", 84, 23, 8, 58, 50),
            entry("ember_imp", "Ember Imp", "ember_imp", 54, 20, 4, 42, 36)
    );

    public static final Map<String, Quest> QUESTS = Map.ofEntries(
            quest("slime_help", "Marla's Remedy", "Clear three Bog Slimes from the road marshes.", "Bog Slime", 3, 35, 24),
            quest("crypt_lights", "Lights in the Crypt", "Defeat two Restless Skeletons below the old stone dungeon.", "Restless Skeleton", 2, 60, 42),
            quest("orc_siege", "Siege Warning", "Break the raider vanguard by defeating one Orc Brute.", "Orc Brute", 1, 95, 70),
            quest("shadow_swarm", "Swarm in the Rafters", "Defeat three Cave Bats from the old dungeon roof.", "Cave Bat", 3, 75, 58),
            quest("wraith_hunt", "Wraith Hunt", "Defeat one Elder Wraith and restore the crypt ward.", "Elder Wraith", 1, 140, 110),
            quest("broodmother", "The Broodmother Below", "Hunt the Acid Broodmother deep in the lower vault.", "Acid Broodmother", 1, 210, 160),
            quest("winter_fangs", "Winter Fangs", "Cull two Frost Wolves stalking the northern pass.", "Frost Wolf", 2, 135, 105),
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
                    "Rook: Good. A camp without a king argues with itself long enough for people to get home.")
    );

    public static final Map<String, Item> ITEMS = Map.ofEntries(
            Map.entry("potion_small", new Item("potion_small", "Small Potion", "icon_potion_red", 18, 28, 0)),
            Map.entry("potion_large", new Item("potion_large", "Large Potion", "icon_potion_green", 42, 60, 0)),
            Map.entry("ether", new Item("ether", "Ether", "icon_potion_blue", 28, 0, 22)),
            Map.entry("guard_tonic", new Item("guard_tonic", "Guard Tonic", "icon_shield", 35, 16, 10)),
            Map.entry("battle_kit", new Item("battle_kit", "Battle Kit", "icon_sword", 55, 36, 16))
    );

    public static final Map<String, Equipment> EQUIPMENT = Map.ofEntries(
            equipment("rusty_sword", "Rusty Sword", "weapon", "icon_sword", 1, 0, 0, 0, 8, "Old steel with one useful edge"),
            equipment("iron_sword", "Iron Sword", "weapon", "icon_sword", 3, 0, 0, 0, 25, "A sturdy blade of forged iron"),
            equipment("steel_sword", "Steel Sword", "weapon", "icon_sword", 6, 0, 0, 0, 60, "A keen edge of tempered steel"),
            equipment("enchanted_blade", "Enchanted Blade", "weapon", "icon_sword", 10, 0, 0, 4, 120, "A blade infused with ancient magic"),
            equipment("stormbrand", "Stormbrand", "weapon", "icon_sword", 13, 0, 0, 6, 190, "A bright blade that hums before rain"),
            equipment("hunter_knife", "Hunter Knife", "weapon", "icon_sword", 3, 1, 0, 0, 28, "Fast, balanced, and easy to carry"),
            equipment("oak_bow", "Oak Bow", "weapon", "icon_sword", 4, 0, 0, 0, 30, "A reliable ranger weapon"),
            equipment("ash_bow", "Ash Bow", "weapon", "icon_sword", 7, 0, 0, 0, 65, "Carved from hardened ash wood"),
            equipment("storm_bow", "Storm Bow", "weapon", "icon_sword", 11, 0, 0, 3, 145, "Its string snaps like distant thunder"),
            equipment("flame_staff", "Flame Staff", "weapon", "icon_potion_blue", 2, 0, 0, 8, 55, "Crackling with fire magic"),
            equipment("frost_staff", "Frost Staff", "weapon", "icon_potion_blue", 3, 0, 0, 10, 75, "Radiates an icy chill"),
            equipment("star_staff", "Star Staff", "weapon", "icon_potion_blue", 5, 0, 0, 16, 160, "A focus for patient, precise spellwork"),
            equipment("traveler_cloak", "Traveler Cloak", "armor", "icon_chest", 0, 1, 4, 0, 15, "Weathered cloth with hidden stitching"),
            equipment("leather_vest", "Leather Vest", "armor", "icon_chest", 0, 2, 0, 0, 20, "Light and flexible protection"),
            equipment("iron_mail", "Iron Mail", "armor", "icon_chest", 0, 4, 6, 0, 50, "Protective iron plating"),
            equipment("steel_plate", "Steel Plate", "armor", "icon_chest", 0, 6, 12, 0, 100, "Heavy steel armor"),
            equipment("warden_plate", "Warden Plate", "armor", "icon_shield", 0, 8, 18, 0, 170, "Built to hold a line when the line breaks"),
            equipment("mage_robes", "Mage Robes", "armor", "icon_potion_blue", 0, 1, 0, 6, 40, "Imbued with magical resonance"),
            equipment("silkweave_robes", "Silkweave Robes", "armor", "icon_potion_blue", 0, 3, 0, 12, 105, "Soft robes threaded with warding sigils"),
            equipment("ranger_coat", "Ranger Coat", "armor", "icon_chest", 0, 4, 8, 3, 90, "Quiet leather reinforced for long roads"),
            equipment("iron_ring", "Iron Ring", "accessory", "icon_shield", 1, 1, 0, 0, 30, "A simple band of iron"),
            equipment("vitality_charm", "Vitality Charm", "accessory", "icon_potion_red", 0, 0, 8, 0, 45, "Increases maximum health"),
            equipment("mana_stone", "Mana Stone", "accessory", "icon_potion_blue", 0, 0, 0, 6, 50, "Resonates with magical energy"),
            equipment("warrior_brooch", "Warrior Brooch", "accessory", "icon_sword", 3, 0, 4, 0, 70, "A brass pin worn by old captains"),
            equipment("warding_seal", "Warding Seal", "accessory", "icon_shield", 0, 3, 0, 4, 75, "A small charm etched with defensive runes"),
            equipment("phoenix_feather", "Phoenix Feather", "accessory", "icon_potion_red", 0, 0, 14, 8, 130, "Warm to the touch and difficult to look away from")
    );

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
            Map.entry("riverside", new Shop("riverside", "Riverside Market", List.of("potion_small", "ether", "guard_tonic", "traveler_cloak", "iron_sword", "iron_ring"))),
            Map.entry("highwall", new Shop("highwall", "Highwall Quartermaster", List.of("potion_small", "potion_large", "ether", "battle_kit", "steel_sword", "iron_mail", "ranger_coat"))),
            Map.entry("crypt_vendor", new Shop("crypt_vendor", "Crypt Provisioner", List.of("potion_large", "ether", "guard_tonic", "battle_kit", "flame_staff", "warden_plate", "phoenix_feather")))
    );

    public static final Map<String, RecruitSpec> RECRUITS = Map.ofEntries(
            recruit("marla", "Marla", "npc_marla", "Medic", 44, 24, 7, 2,
                    List.of(new Ability("Field Mend", 18, 5, Ability.AbilityKind.HEAL)),
                    Map.of("potion_small", 1)),
            recruit("ren", "Ren", "npc_ren", "Archivist", 38, 34, 10, 2,
                    List.of(new Ability("Rune Flare", 20, 6, Ability.AbilityKind.DAMAGE)),
                    Map.of("ether", 1)),
            recruit("torin", "Torin", "npc_torin", "Captain", 56, 14, 12, 4,
                    List.of(new Ability("Guarding Strike", 18, 5, Ability.AbilityKind.DAMAGE)),
                    Map.of("guard_tonic", 1)),
            recruit("eira", "Eira", "npc_marla", "Frost Scout", 46, 20, 12, 3,
                    List.of(
                            new Ability("Marked Shot", 19, 5, Ability.AbilityKind.DAMAGE),
                            new Ability("Trail Salve", 16, 5, Ability.AbilityKind.HEAL)
                    ),
                    Map.of("potion_small", 1)),
            recruit("bran", "Bran", "npc_torin", "Roadwarden", 52, 14, 11, 4,
                    List.of(new Ability("Roadwarden's Cut", 17, 4, Ability.AbilityKind.DAMAGE)),
                    Map.of("guard_tonic", 1)),
            recruit("niva", "Niva", "npc_ren", "Snow Seer", 40, 30, 9, 3,
                    List.of(
                            new Ability("Blue Candle", 18, 5, Ability.AbilityKind.DAMAGE),
                            new Ability("Warm Hands", 18, 6, Ability.AbilityKind.HEAL)
                    ),
                    Map.of("ether", 1)),
            recruit("sela", "Sela", "npc_marla", "Dune Guide", 44, 22, 12, 2,
                    List.of(new Ability("Mirage Cut", 20, 6, Ability.AbilityKind.DAMAGE)),
                    Map.of("potion_small", 1)),
            recruit("fen", "Fen", "npc_ren", "Marsh Witch", 42, 32, 10, 2,
                    List.of(
                            new Ability("Boglight Hex", 20, 6, Ability.AbilityKind.DAMAGE),
                            new Ability("Reed Charm", 18, 6, Ability.AbilityKind.HEAL)
                    ),
                    Map.of("ether", 1))
    );

    public static final List<Npc> NPCS = List.of(
            new Npc("city_riverside", "Marla", "npc_marla", 17, 11, List.of(
                    "I was a field medic before Riverside had walls.",
                    "The marsh fever is back, and the slimes carry it along the herb road.",
                    "Clear the road and I will travel with you. Alderfall needs hands that can mend."
            ), "slime_help", "riverside", "marla", 0),
            new Npc("city_archive", "Archivist Ren", "npc_ren", 17, 11, List.of(
                    "The Archive keeps the names of everyone the old wards failed to save.",
                    "Now the dead are leaving their shelves and walking below Stonegate.",
                    "Bring me proof the crypt can be quieted, and I will carry the record beside you."
            ), "crypt_lights", null, "ren", 0),
            new Npc("city_highwall", "Captain Torin", "npc_torin", 16, 11, List.of(
                    "I lost a patrol at the west stones, and the raiders learned our names from their packs.",
                    "Their brute leads every raid. Break him and the road opens again.",
                    "Do that, and Highwall owes you my shield arm."
            ), "orc_siege", null, "torin", 0),
            new Npc("city_highwall", "Scout Rook", "npc_ren", 21, 12, List.of(
                    "Highwall scouts found a crown nailed together from coins, bones, and stolen buckles.",
                    "The one wearing it has turned scattered raiders into a camp with orders. That cannot stand."
            ), "goblin_crown", null),
            new Npc("city_belltower", "Bellkeeper Ilya", "npc_marla", 17, 9, List.of(
                    "The bell tower used to keep ships and spirits honest.",
                    "Quiet those wings and the city can sleep again."
            ), "shadow_swarm", "highwall"),
            new Npc("city_sanctum", "Warden Sol", "npc_ren", 15, 10, List.of(
                    "Sanctum was built around the first ward-stone.",
                    "Wraiths drift through cracks we cannot seal quickly enough."
            ), "wraith_hunt", null),
            new Npc("city_sanctum", "Quartermaster Vesh", "npc_torin", 21, 11, List.of(
                    "Lower tunnels chew through supplies and nerves alike.",
                    "If you descend, take every vial you can carry."
            ), "broodmother", "crypt_vendor"),
            new Npc("city_sanctum", "Eira", "npc_marla", 17, 16, List.of(
                    "Frost wolves have started tearing down my cairns.",
                    "Bring me two pelts and I will guide your party through any whiteout."
            ), "winter_fangs", null, "eira", 0),
            new Npc("village_oakhaven", "Edda", "npc_marla", 13, 8, List.of(
                    "Oakhaven keeps the old road fed, even when the road bites back.",
                    "The near farm still has wheat standing. Bring me sheaves and I will open the market stores."
            ), "bread_for_road", "riverside"),
            new Npc("village_oakhaven", "Mira", "npc_marla", 10, 10, List.of(
                    "I mark every crate that leaves this village. Raiders took two with my blue cord still tied on.",
                    "Their camp is marked on your map. Bring the crates back before the sick have to make do with prayers."
            ), "stolen_supplies", null),
            new Npc("village_oakhaven", "Bran", "npc_torin", 16, 9, List.of(
                    "I kept the roadwatch until my company scattered at Stonegate.",
                    "Pay my contract and I will keep your camp standing when the night gets loud."
            ), null, null, "bran", 40),
            new Npc("village_snowrest", "Niva", "npc_ren", 14, 8, List.of(
                    "Snowrest is the last warm hearth before the pass.",
                    "I read tracks in snow like scripture. Hire me and I will read the road ahead."
            ), null, "highwall", "niva", 90),
            new Npc("village_dunewick", "Sela", "npc_marla", 14, 8, List.of(
                    "Dunewick survives by water, shade, and stubbornness.",
                    "I know which wells lie and which badland trails ambush the careless. My fee is fair."
            ), null, "riverside", "sela", 85),
            new Npc("village_mireford", "Fen", "npc_ren", 14, 8, List.of(
                    "Mireford is built on planks, patience, and listening to things under the water.",
                    "Pay for my charms and I will make the marsh answer to us for once."
            ), null, "crypt_vendor", "fen", 100)
    );

    private static Map.Entry<String, MonsterSpec> entry(
            String key,
            String name,
            String sprite,
            int hp,
            int attack,
            int defense,
            int xp,
            int gold
    ) {
        return Map.entry(key, new MonsterSpec(key, name, sprite, hp, attack, defense, xp, gold));
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
        Item item = ITEMS.get(key);
        if (item != null) {
            return item.name();
        }
        Equipment equipment = EQUIPMENT.get(key);
        return equipment == null ? key : equipment.name();
    }

    public static String itemIcon(String key) {
        Item item = ITEMS.get(key);
        if (item != null) {
            return item.icon();
        }
        Equipment equipment = EQUIPMENT.get(key);
        return equipment == null ? "icon_chest" : equipment.icon();
    }

    public static int itemCost(String key) {
        Item item = ITEMS.get(key);
        if (item != null) {
            return item.cost();
        }
        Equipment equipment = EQUIPMENT.get(key);
        return equipment == null ? 0 : equipment.cost();
    }

    public static List<AbilityStatus> statusHintsForAbility(String abilityName, Ability.AbilityKind kind) {
        String lowered = abilityName.strip().toLowerCase();
        List<AbilityStatus> exact = switch (lowered) {
            case "frost lance" -> List.of(new AbilityStatus("weak", "enemy"));
            case "firebolt" -> List.of(new AbilityStatus("burn", "enemy", 0.55));
            case "bulwark" -> List.of(new AbilityStatus("shield", "self"), new AbilityStatus("fortified", "self"));
            case "second wind", "herbal remedy", "mend" -> List.of(new AbilityStatus("regeneration", "self"));
            case "renewing ward", "field salve" -> List.of(new AbilityStatus("regeneration", "target"), new AbilityStatus("shield", "target"));
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
        if (lowered.contains("mend") || lowered.contains("salve") || lowered.contains("renew")) {
            hints.add(new AbilityStatus("regeneration", kind == Ability.AbilityKind.HEAL ? "target" : "self"));
        }
        return hints;
    }

    private static Map.Entry<String, Equipment> equipment(
            String key,
            String name,
            String slot,
            String icon,
            int attackBonus,
            int defenseBonus,
            int hpBonus,
            int mpBonus,
            int cost,
            String description
    ) {
        return Map.entry(key, new Equipment(key, name, slot, icon, attackBonus, defenseBonus, hpBonus, mpBonus, cost, description));
    }

    public record MonsterSpec(String key, String name, String sprite, int hp, int attack, int defense, int xp, int gold) {
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
            actor.abilities.addAll(abilities);
            for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
                actor.addItem(entry.getKey(), entry.getValue());
            }
            return actor;
        }
    }
}
