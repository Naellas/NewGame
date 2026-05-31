package com.alderfall.game;

import java.util.List;
import java.util.Map;

public final class GameData {
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
                    new Ability("Firebolt", 22, 6, Ability.AbilityKind.DAMAGE),
                    new Ability("Frost Lance", 18, 5, Ability.AbilityKind.DAMAGE),
                    new Ability("Mend", 18, 5, Ability.AbilityKind.HEAL)
            );
            case "Ranger" -> List.of(
                    new Ability("Piercing Shot", 18, 5, Ability.AbilityKind.DAMAGE),
                    new Ability("Rain Volley", 15, 4, Ability.AbilityKind.DAMAGE, "all_enemies"),
                    new Ability("Herbal Remedy", 16, 5, Ability.AbilityKind.HEAL)
            );
            case "Cleric", "Medic" -> List.of(
                    new Ability("Radiant Bolt", 18, 5, Ability.AbilityKind.DAMAGE),
                    new Ability("Blessing", 20, 6, Ability.AbilityKind.HEAL, "ally"),
                    new Ability("Ward Prayer", 16, 5, Ability.AbilityKind.HEAL, "ally")
            );
            case "Rogue", "Dune Guide" -> List.of(
                    new Ability("Shadowstep Cut", 20, 6, Ability.AbilityKind.DAMAGE),
                    new Ability("Venom Edge", 16, 5, Ability.AbilityKind.DAMAGE),
                    new Ability("Smoke Veil", 0, 4, Ability.AbilityKind.DEFEND, "self")
            );
            case "Battle Medic" -> List.of(
                    new Ability("Hemostatic Strike", 15, 4, Ability.AbilityKind.DAMAGE),
                    new Ability("Emergency Mend", 22, 6, Ability.AbilityKind.HEAL, "ally"),
                    new Ability("Tonic Toss", 16, 5, Ability.AbilityKind.HEAL, "party")
            );
            case "Ironwall" -> List.of(
                    new Ability("Shield Ram", 18, 5, Ability.AbilityKind.DAMAGE),
                    new Ability("Hold Line", 0, 4, Ability.AbilityKind.DEFEND, "self"),
                    new Ability("Armor Patch", 16, 5, Ability.AbilityKind.HEAL, "self")
            );
            case "Bladedancer" -> List.of(
                    new Ability("Dual Cut", 22, 6, Ability.AbilityKind.DAMAGE),
                    new Ability("Spinning Arc", 15, 5, Ability.AbilityKind.DAMAGE, "all_enemies"),
                    new Ability("Grace Step", 0, 4, Ability.AbilityKind.DEFEND, "self")
            );
            case "Veilrunner" -> List.of(
                    new Ability("Quick Cut", 20, 5, Ability.AbilityKind.DAMAGE),
                    new Ability("Smoke Bomb", 14, 5, Ability.AbilityKind.DAMAGE, "all_enemies"),
                    new Ability("Shadow Salve", 16, 5, Ability.AbilityKind.HEAL, "self")
            );
            case "Wildspeaker" -> List.of(
                    new Ability("Thorn Dart", 18, 5, Ability.AbilityKind.DAMAGE),
                    new Ability("Green Balm", 20, 6, Ability.AbilityKind.HEAL, "ally"),
                    new Ability("Bramble Wave", 14, 5, Ability.AbilityKind.DAMAGE, "all_enemies")
            );
            case "Sunwarden" -> List.of(
                    new Ability("Sunstrike", 18, 5, Ability.AbilityKind.DAMAGE),
                    new Ability("Lantern Blessing", 22, 7, Ability.AbilityKind.HEAL, "ally"),
                    new Ability("Hold Fast", 0, 4, Ability.AbilityKind.DEFEND, "self")
            );
            case "Thornbinder" -> List.of(
                    new Ability("Thorn Lash", 20, 6, Ability.AbilityKind.DAMAGE),
                    new Ability("Root Burst", 15, 6, Ability.AbilityKind.DAMAGE, "all_enemies"),
                    new Ability("Bitter Salve", 18, 6, Ability.AbilityKind.HEAL, "ally")
            );
            case "Stonebreaker" -> List.of(
                    new Ability("Hammer Blow", 24, 6, Ability.AbilityKind.DAMAGE),
                    new Ability("Stone Guard", 0, 4, Ability.AbilityKind.DEFEND, "self"),
                    new Ability("Seismic Sweep", 16, 6, Ability.AbilityKind.DAMAGE, "all_enemies")
            );
            case "Nightblade" -> List.of(
                    new Ability("Night Slash", 24, 6, Ability.AbilityKind.DAMAGE),
                    new Ability("Poison Kiss", 18, 6, Ability.AbilityKind.DAMAGE),
                    new Ability("Fade", 0, 4, Ability.AbilityKind.DEFEND, "self")
            );
            case "Grovekeeper" -> List.of(
                    new Ability("Vine Flick", 16, 5, Ability.AbilityKind.DAMAGE),
                    new Ability("Bloom Heal", 22, 7, Ability.AbilityKind.HEAL, "ally"),
                    new Ability("Grove Hymn", 16, 9, Ability.AbilityKind.HEAL, "party")
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
            entry("sheep", "Wild Sheep", "sheep", 26, 6, 1, 8, 4),
            entry("mountain_goat", "Mountain Goat", "mountain_goat", 34, 9, 2, 13, 7),
            entry("doe", "Woodland Doe", "doe", 30, 8, 1, 10, 6),
            entry("stag", "Crown Stag", "stag", 44, 12, 3, 18, 12),
            entry("wolf", "Grey Wolf", "wolf", 32, 10, 2, 12, 7),
            entry("meadow_wolf", "Meadow Wolf", "wolf", 34, 11, 2, 14, 8),
            entry("moss_stag", "Moss Stag", "moss_stag", 48, 13, 4, 22, 14),
            entry("frost_wolf", "Frost Wolf", "frost_wolf", 46, 15, 4, 30, 24),
            entry("stoneback_goat", "Stoneback Goat", "stoneback_goat", 54, 16, 6, 34, 26),
            entry("bat", "Cave Bat", "bat", 22, 9, 1, 10, 5),
            entry("crypt_bat", "Crypt Bat", "crypt_bat", 36, 13, 2, 22, 15),
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
            entry("bone_knight", "Bone Knight", "skeleton", 92, 23, 9, 78, 64),
            entry("crypt_revenant", "Crypt Revenant", "wraith", 104, 25, 8, 92, 78),
            entry("elder_wraith", "Elder Wraith", "wraith", 128, 29, 10, 130, 110),
            entry("orc_champion", "Orc Champion", "orc", 112, 27, 9, 108, 92),
            entry("thornling", "Briar Thornling", "thornling", 44, 13, 4, 20, 13),
            entry("sand_stalker", "Sand Stalker", "sand_stalker", 62, 19, 5, 36, 30),
            entry("glass_scorpion", "Glass Scorpion", "glass_scorpion", 58, 18, 6, 38, 32),
            entry("ice_golem", "Ice Golem", "ice_golem", 96, 25, 11, 72, 66),
            entry("bog_beast", "Bog Beast", "bog_beast", 84, 23, 8, 58, 50),
            entry("reed_serpent", "Reed Serpent", "reed_serpent", 64, 20, 5, 42, 34),
            entry("river_eel", "River Eel", "river_eel", 38, 14, 2, 24, 18),
            entry("ash_scorpion", "Ash Scorpion", "ash_scorpion", 72, 22, 7, 52, 44),
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
                    "Nessa: Riverside will hear wagons instead of warnings tonight.")
    );

    public static final Map<String, Item> ITEMS = Map.ofEntries(
            Map.entry("potion_small", new Item("potion_small", "Small Potion", "icon_potion_red", 18, 28, 0)),
            Map.entry("potion_large", new Item("potion_large", "Large Potion", "icon_potion_green", 42, 60, 0)),
            Map.entry("ether", new Item("ether", "Ether", "icon_potion_blue", 28, 0, 22)),
            Map.entry("guard_tonic", new Item("guard_tonic", "Guard Tonic", "icon_shield", 35, 16, 10)),
            Map.entry("battle_kit", new Item("battle_kit", "Battle Kit", "icon_sword", 55, 36, 16)),
            Map.entry("cooked_meat", new Item("cooked_meat", "Cooked Meat", "icon_potion_red", 16, 22, 0)),
            Map.entry("trail_rations", new Item("trail_rations", "Trail Rations", "icon_potion_green", 34, 42, 8)),
            Map.entry("vegetable_stew", new Item("vegetable_stew", "Vegetable Stew", "icon_potion_green", 24, 30, 8)),
            Map.entry("herbal_salve", new Item("herbal_salve", "Herbal Salve", "icon_potion_green", 24, 34, 0)),
            Map.entry("focus_tea", new Item("focus_tea", "Focus Tea", "icon_potion_blue", 22, 8, 18))
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
            equipment("shadow_dagger", "Shadow Dagger", "weapon", "icon_sword", 8, 1, 0, 4, 115, "A quiet blade that vanishes in poor light"),
            equipment("silver_mace", "Silver Mace", "weapon", "icon_shield", 5, 2, 4, 8, 110, "A polished mace used by road chaplains"),
            equipment("flame_staff", "Flame Staff", "weapon", "icon_potion_blue", 2, 0, 0, 8, 55, "Crackling with fire magic"),
            equipment("frost_staff", "Frost Staff", "weapon", "icon_potion_blue", 3, 0, 0, 10, 75, "Radiates an icy chill"),
            equipment("star_staff", "Star Staff", "weapon", "icon_potion_blue", 5, 0, 0, 16, 160, "A focus for patient, precise spellwork"),
            equipment("traveler_cloak", "Traveler Cloak", "chestpiece", "icon_chest", 0, 1, 4, 0, 15, "Weathered cloth with hidden stitching"),
            equipment("leather_vest", "Leather Vest", "chestpiece", "icon_chest", 0, 2, 0, 0, 20, "Light and flexible protection"),
            equipment("iron_mail", "Iron Mail", "chestpiece", "icon_chest", 0, 4, 6, 0, 50, "Protective iron plating"),
            equipment("steel_plate", "Steel Plate", "chestpiece", "icon_chest", 0, 6, 12, 0, 100, "Heavy steel armor"),
            equipment("warden_plate", "Warden Plate", "chestpiece", "icon_shield", 0, 8, 18, 0, 170, "Built to hold a line when the line breaks"),
            equipment("mage_robes", "Mage Robes", "chestpiece", "icon_potion_blue", 0, 1, 0, 6, 40, "Imbued with magical resonance"),
            equipment("silkweave_robes", "Silkweave Robes", "chestpiece", "icon_potion_blue", 0, 3, 0, 12, 105, "Soft robes threaded with warding sigils"),
            equipment("ranger_coat", "Ranger Coat", "chestpiece", "icon_chest", 0, 4, 8, 3, 90, "Quiet leather reinforced for long roads"),
            equipment("acolyte_mantle", "Acolyte Mantle", "chestpiece", "icon_potion_blue", 0, 3, 10, 8, 95, "Layered cloth bright with stitched wards"),
            equipment("night_leathers", "Night Leathers", "chestpiece", "icon_chest", 2, 3, 6, 4, 100, "Soft black leather cut for quick movement"),
            equipment("scout_hood", "Scout Hood", "helmet", "icon_chest", 0, 1, 3, 2, 32, "A hood with a weatherproof brow"),
            equipment("iron_helm", "Iron Helm", "helmet", "icon_shield", 0, 3, 5, 0, 58, "Plain iron protection for plain bad days"),
            equipment("warded_pauldrons", "Warded Pauldrons", "pauldrons", "icon_shield", 0, 3, 6, 3, 80, "Shoulder plates etched with small warding marks"),
            equipment("duelist_gloves", "Duelist Gloves", "gloves", "icon_sword", 2, 1, 0, 2, 56, "Grip-stiffened gloves for quick work"),
            equipment("guard_gauntlets", "Guard Gauntlets", "gloves", "icon_shield", 1, 3, 4, 0, 74, "Heavy gloves with plated knuckles"),
            equipment("road_belt", "Road Belt", "belt", "icon_chest", 0, 1, 5, 3, 42, "Pouches, buckles, and room for one more useful thing"),
            equipment("focus_sash", "Focus Sash", "belt", "icon_potion_blue", 0, 0, 0, 8, 78, "A sash knotted around a cool blue charm"),
            equipment("trail_leggings", "Trail Leggings", "leggings", "icon_chest", 0, 2, 5, 1, 48, "Reinforced cloth for thorny roads"),
            equipment("steel_greaves", "Steel Greaves", "leggings", "icon_shield", 0, 4, 8, 0, 92, "Leg guards that make retreat sound expensive"),
            equipment("softstep_boots", "Softstep Boots", "boots", "icon_chest", 1, 1, 2, 4, 64, "Quiet soles with a ranger's stitch"),
            equipment("iron_boots", "Iron Boots", "boots", "icon_shield", 0, 3, 6, 0, 70, "Not graceful, but dependable"),
            equipment("iron_ring", "Iron Ring", "ring", "icon_shield", 1, 1, 0, 0, 30, "A simple band of iron"),
            equipment("vitality_charm", "Vitality Charm", "necklace", "icon_potion_red", 0, 0, 8, 0, 45, "Increases maximum health"),
            equipment("mana_stone", "Mana Stone", "necklace", "icon_potion_blue", 0, 0, 0, 6, 50, "Resonates with magical energy"),
            equipment("warrior_brooch", "Warrior Brooch", "necklace", "icon_sword", 3, 0, 4, 0, 70, "A brass pin worn by old captains"),
            equipment("warding_seal", "Warding Seal", "ring", "icon_shield", 0, 3, 0, 4, 75, "A small charm etched with defensive runes"),
            equipment("phoenix_feather", "Phoenix Feather", "necklace", "icon_potion_red", 0, 0, 14, 8, 130, "Warm to the touch and difficult to look away from"),
            equipment("rustbit_saber", "Rustbit Saber", "weapon", "rustbit_saber", 2, 0, 0, 0, 1, 3, 18, "A cheap saber with a stubborn edge"),
            equipment("riverguard_blade", "Riverguard Blade", "weapon", "riverguard_blade", 4, 0, 2, 0, 2, 4, 36, "Militia steel balanced for wet roads"),
            equipment("cinderedge", "Cinderedge", "weapon", "cinderedge", 6, 0, 0, 2, 3, 5, 58, "A blackened blade that holds heat"),
            equipment("frostvein_sword", "Frostvein Sword", "weapon", "frostvein_sword", 7, 1, 0, 2, 4, 6, 78, "Cold metal with pale blue seams"),
            equipment("thorncarver", "Thorncarver", "weapon", "thorncarver", 8, 1, 4, 0, 5, 7, 98, "A hooked sword made for cutting briars and armor straps"),
            equipment("moonlit_rapier", "Moonlit Rapier", "weapon", "moonlit_rapier", 9, 1, 0, 4, 6, 8, 124, "A narrow blade that flashes in dim light"),
            equipment("sunfall_longsword", "Sunfall Longsword", "weapon", "sunfall_longsword", 10, 2, 5, 0, 7, 9, 150, "Bright steel carried by roadwardens"),
            equipment("stormglass_blade", "Stormglass Blade", "weapon", "stormglass_blade", 12, 0, 0, 6, 8, 10, 188, "A glassy edge that hums in rain"),
            equipment("duskwake_sword", "Duskwake Sword", "weapon", "duskwake_sword", 13, 1, 4, 4, 9, 11, 225, "Shadowed steel with a quiet grip"),
            equipment("kingsroad_claymore", "Kingsroad Claymore", "weapon", "kingsroad_claymore", 15, 2, 8, 0, 10, 12, 270, "A broad sword fit for clearing a line"),
            equipment("marshlight_cutlass", "Marshlight Cutlass", "weapon", "marshlight_cutlass", 16, 2, 0, 6, 11, 13, 322, "Curved steel with a pale marsh glow"),
            equipment("obsidian_fang", "Obsidian Fang", "weapon", "obsidian_fang", 18, 1, 0, 8, 12, 14, 390, "A dark volcanic blade that drinks torchlight"),
            equipment("starforged_sword", "Starforged Sword", "weapon", "starforged_sword", 20, 2, 8, 8, 13, 16, 480, "A silver star-metal blade with a singing edge"),
            equipment("heartstone_blade", "Heartstone Blade", "weapon", "heartstone_blade", 23, 3, 12, 8, 15, 18, 610, "Forged around a warm red shard"),
            equipment("oathbreaker_edge", "Oathbreaker Edge", "weapon", "oathbreaker_edge", 26, 2, 10, 12, 18, 99, 760, "A forbidden blade made for ending old promises"),
            equipment("oakheart_staff", "Oakheart Staff", "weapon", "oakheart_staff", 1, 0, 0, 5, 1, 3, 20, "A simple oak focus for first spells"),
            equipment("ashwind_staff", "Ashwind Staff", "weapon", "ashwind_staff", 2, 0, 0, 8, 2, 4, 42, "Light ash wood carved with wind marks"),
            equipment("embercore_staff", "Embercore Staff", "weapon", "embercore_staff", 3, 0, 0, 11, 3, 5, 66, "A warm focus with a coal-bright heart"),
            equipment("frostroot_staff", "Frostroot Staff", "weapon", "frostroot_staff", 3, 1, 0, 13, 4, 6, 86, "Pale rootwood that never thaws"),
            equipment("stormcall_staff", "Stormcall Staff", "weapon", "stormcall_staff", 4, 0, 0, 16, 5, 7, 112, "A forked staff that smells of rain"),
            equipment("moonwell_staff", "Moonwell Staff", "weapon", "moonwell_staff", 4, 0, 4, 18, 6, 8, 140, "A silver focus used for patient healing rites"),
            equipment("sunspire_staff", "Sunspire Staff", "weapon", "sunspire_staff", 5, 1, 6, 20, 7, 9, 170, "Brightly capped and warm in the hand"),
            equipment("thornbinder_staff", "Thornbinder Staff", "weapon", "thornbinder_staff", 6, 1, 8, 21, 8, 10, 205, "Wrapped in living vine and careful knots"),
            equipment("bogsong_staff", "Bogsong Staff", "weapon", "bogsong_staff", 6, 2, 10, 23, 9, 11, 245, "A reed-bound focus that hums near water"),
            equipment("glassdune_staff", "Glassdune Staff", "weapon", "glassdune_staff", 7, 0, 0, 27, 10, 12, 292, "Desert glass set in polished darkwood"),
            equipment("archive_scepter", "Archive Scepter", "weapon", "archive_scepter", 8, 1, 0, 30, 11, 13, 350, "A scholar's focus with exacting sigils"),
            equipment("starfall_staff", "Starfall Staff", "weapon", "starfall_staff", 9, 1, 0, 34, 12, 14, 420, "A staff topped with cold starlight"),
            equipment("heartstone_crook", "Heartstone Crook", "weapon", "heartstone_crook", 10, 2, 16, 34, 13, 16, 510, "A healer's crook set with a red shard"),
            equipment("voidglass_staff", "Voidglass Staff", "weapon", "voidglass_staff", 12, 1, 0, 40, 15, 18, 645, "Black glass bends light around its crown"),
            equipment("dawnweave_staff", "Dawnweave Staff", "weapon", "dawnweave_staff", 13, 2, 10, 44, 18, 99, 790, "A radiant focus woven with gold thread"),
            equipment("woodcutter_axe", "Woodcutter Axe", "weapon", "woodcutter_axe", 3, 0, 2, 0, 1, 3, 22, "A practical axe with battlefield ambitions"),
            equipment("raider_hatchet", "Raider Hatchet", "weapon", "raider_hatchet", 5, 0, 0, 0, 2, 4, 40, "Fast and ugly in close quarters"),
            equipment("ironbeard_axe", "Ironbeard Axe", "weapon", "ironbeard_axe", 7, 1, 0, 0, 3, 5, 68, "An iron axe with a heavy lower bite"),
            equipment("steelcleaver", "Steelcleaver", "weapon", "steelcleaver", 9, 1, 4, 0, 4, 6, 92, "A wide blade made for splitting armor"),
            equipment("frosthew_axe", "Frosthew Axe", "weapon", "frosthew_axe", 10, 1, 0, 3, 5, 7, 118, "A cold axe from the northern pass"),
            equipment("embermaul_axe", "Embermaul Axe", "weapon", "embermaul_axe", 12, 0, 6, 2, 6, 8, 148, "Its head glows at the cutting edge"),
            equipment("stormsplitter", "Stormsplitter", "weapon", "stormsplitter", 13, 1, 0, 5, 7, 9, 180, "Built to crack shields like thunder"),
            equipment("thornbite_axe", "Thornbite Axe", "weapon", "thornbite_axe", 14, 2, 8, 0, 8, 10, 216, "A hooked axe wrapped in barbed vine"),
            equipment("bonehook_axe", "Bonehook Axe", "weapon", "bonehook_axe", 16, 1, 10, 0, 9, 11, 260, "A grim hook for dragging foes off balance"),
            equipment("obsidian_chopper", "Obsidian Chopper", "weapon", "obsidian_chopper", 18, 0, 0, 6, 10, 12, 315, "Volcanic glass honed to a cruel shine"),
            equipment("sunward_axe", "Sunward Axe", "weapon", "sunward_axe", 19, 2, 8, 4, 11, 13, 372, "A bright axe carried by temple guards"),
            equipment("marshreaper", "Marshreaper", "weapon", "marshreaper", 21, 2, 12, 2, 12, 14, 445, "A long axe made for cutting reeds and monsters"),
            equipment("mountainfall_axe", "Mountainfall Axe", "weapon", "mountainfall_axe", 24, 3, 16, 0, 13, 16, 540, "Heavy enough to make stone listen"),
            equipment("royal_halberd_axe", "Royal Halberd Axe", "weapon", "royal_halberd_axe", 27, 3, 12, 4, 15, 18, 680, "A ceremonial weapon that forgot to be harmless"),
            equipment("starbreaker_axe", "Starbreaker Axe", "weapon", "starbreaker_axe", 31, 2, 18, 8, 18, 99, 840, "A massive axe with a star-metal crescent"),
            equipment("river_buckler", "River Buckler", "shield", "river_buckler", 0, 2, 3, 0, 1, 3, 18, "A small shield with a water-dark rim"),
            equipment("oaken_roundshield", "Oaken Roundshield", "shield", "oaken_roundshield", 0, 3, 6, 0, 2, 4, 34, "Layered oak bound in iron"),
            equipment("iron_kite_shield", "Iron Kite Shield", "shield", "iron_kite_shield", 0, 5, 8, 0, 3, 5, 60, "A pointed shield for steady advances"),
            equipment("towerguard_shield", "Towerguard Shield", "shield", "towerguard_shield", 0, 7, 14, 0, 4, 6, 88, "Tall enough to hide behind properly"),
            equipment("frostguard_aegis", "Frostguard Aegis", "shield", "frostguard_aegis", 0, 8, 16, 3, 5, 7, 118, "Cold steel that numbs incoming blows"),
            equipment("emberward_shield", "Emberward Shield", "shield", "emberward_shield", 1, 9, 14, 4, 6, 8, 150, "A shield warm enough to dry rain"),
            equipment("stormwall_shield", "Stormwall Shield", "shield", "stormwall_shield", 0, 11, 18, 5, 7, 9, 184, "It rattles before lightning strikes"),
            equipment("suncrest_shield", "Suncrest Shield", "shield", "suncrest_shield", 1, 12, 22, 4, 8, 10, 222, "A bright crest painted over layered steel"),
            equipment("shadowglass_shield", "Shadowglass Shield", "shield", "shadowglass_shield", 2, 12, 16, 7, 9, 11, 270, "Dark glass backed by black iron"),
            equipment("thornwall_shield", "Thornwall Shield", "shield", "thornwall_shield", 1, 14, 26, 2, 10, 12, 325, "A living shield that grips the arm"),
            equipment("marshreed_ward", "Marshreed Ward", "shield", "marshreed_ward", 0, 15, 30, 5, 11, 13, 386, "Braided reed over hidden steel ribs"),
            equipment("bonebound_shield", "Bonebound Shield", "shield", "bonebound_shield", 2, 16, 28, 4, 12, 14, 460, "Bone plates lashed to a dark core"),
            equipment("royal_heater", "Royal Heater", "shield", "royal_heater", 1, 18, 34, 5, 13, 16, 560, "A polished heater shield with old heraldry"),
            equipment("starforged_aegis", "Starforged Aegis", "shield", "starforged_aegis", 2, 21, 38, 8, 15, 18, 710, "A shield hammered from bright fallen metal"),
            equipment("heartstone_bulwark", "Heartstone Bulwark", "shield", "heartstone_bulwark", 3, 24, 46, 8, 18, 99, 880, "A heavy ward built around a red shard"),
            equipment("padded_gambeson", "Padded Gambeson", "chestpiece", "padded_gambeson", 0, 2, 6, 0, 1, 3, 20, "Quilted cloth with honest stitching"),
            equipment("scout_leathers", "Scout Leathers", "chestpiece", "scout_leathers", 1, 3, 7, 1, 2, 4, 38, "Quiet leather with travel cuts"),
            equipment("ranger_jerkin", "Ranger Jerkin", "chestpiece", "ranger_jerkin", 1, 4, 10, 2, 3, 5, 64, "A reinforced jerkin for long patrols"),
            equipment("duelist_jacket", "Duelist Jacket", "chestpiece", "duelist_jacket", 2, 4, 8, 3, 4, 6, 88, "Cut short for fast footwork"),
            equipment("nightweave_coat", "Nightweave Coat", "chestpiece", "nightweave_coat", 2, 5, 10, 5, 5, 7, 115, "Black leather that swallows torchlight"),
            equipment("marshrunner_mantle", "Marshrunner Mantle", "chestpiece", "marshrunner_mantle", 1, 6, 14, 4, 6, 8, 145, "Waxed cloth that shrugs off damp"),
            equipment("dunewrap_vest", "Dunewrap Vest", "chestpiece", "dunewrap_vest", 2, 6, 12, 6, 7, 9, 176, "Layered desert cloth with hidden plates"),
            equipment("frostleaf_cloak", "Frostleaf Cloak", "chestpiece", "frostleaf_cloak", 1, 7, 16, 7, 8, 10, 212, "A pale cloak that keeps warmth close"),
            equipment("thornsilk_armor", "Thornsilk Armor", "chestpiece", "thornsilk_armor", 3, 7, 18, 5, 9, 11, 255, "Soft armor threaded with green briar silk"),
            equipment("stormhide_jacket", "Stormhide Jacket", "chestpiece", "stormhide_jacket", 2, 8, 16, 9, 10, 12, 306, "Oiled hide that crackles under pressure"),
            equipment("suncloth_mantle", "Suncloth Mantle", "chestpiece", "suncloth_mantle", 1, 9, 22, 8, 11, 13, 365, "Gold-threaded cloth used by healers"),
            equipment("archive_mantle", "Archive Mantle", "chestpiece", "archive_mantle", 0, 9, 14, 14, 12, 14, 430, "A scholar's mantle lined with ward notes"),
            equipment("starweave_robes", "Starweave Robes", "chestpiece", "starweave_robes", 2, 10, 18, 16, 13, 16, 520, "Robes stitched with cold stars"),
            equipment("moonwater_coat", "Moonwater Coat", "chestpiece", "moonwater_coat", 3, 11, 24, 14, 15, 18, 650, "A flowing coat that glimmers like deep water"),
            equipment("heartwood_vest", "Heartwood Vest", "chestpiece", "heartwood_vest", 4, 13, 30, 12, 18, 99, 800, "Living wood plates over supple leather"),
            equipment("riveted_mail", "Riveted Mail", "chestpiece", "riveted_mail", 0, 5, 12, 0, 1, 3, 34, "Basic mail rings on a padded backing"),
            equipment("guard_cuirass", "Guard Cuirass", "chestpiece", "guard_cuirass", 0, 7, 18, 0, 2, 4, 60, "A plain cuirass used by gate guards"),
            equipment("steel_bastion_plate", "Steel Bastion Plate", "chestpiece", "steel_bastion_plate", 0, 9, 24, 0, 3, 5, 92, "Heavy steel with a thick breastplate"),
            equipment("mountain_plate", "Mountain Plate", "chestpiece", "mountain_plate", 0, 11, 30, 0, 4, 6, 130, "Dense armor from cold mountain forges"),
            equipment("boneguard_plate", "Boneguard Plate", "chestpiece", "boneguard_plate", 1, 12, 32, 0, 5, 7, 168, "Bone plates over black mail"),
            equipment("obsidian_plate", "Obsidian Plate", "chestpiece", "obsidian_plate", 1, 13, 28, 4, 6, 8, 210, "Glossy plate with a volcanic bite"),
            equipment("frostbound_plate", "Frostbound Plate", "chestpiece", "frostbound_plate", 0, 15, 38, 4, 7, 9, 256, "Blue-white metal that resists shock"),
            equipment("emberforged_plate", "Emberforged Plate", "chestpiece", "emberforged_plate", 2, 15, 36, 5, 8, 10, 310, "Red seams glow between heavy plates"),
            equipment("stormguard_plate", "Stormguard Plate", "chestpiece", "stormguard_plate", 1, 17, 42, 6, 9, 11, 370, "Armor grounded with copper channels"),
            equipment("sunwarden_plate", "Sunwarden Plate", "chestpiece", "sunwarden_plate", 1, 18, 48, 6, 10, 12, 440, "Bright armor made for holding sacred doors"),
            equipment("shadowplate_harness", "Shadowplate Harness", "chestpiece", "shadowplate_harness", 3, 18, 38, 9, 11, 13, 520, "Black plate with silent leather joins"),
            equipment("thornplate_mail", "Thornplate Mail", "chestpiece", "thornplate_mail", 2, 20, 52, 5, 12, 14, 610, "Layered plates shaped like overlapping leaves"),
            equipment("marshbulwark_plate", "Marshbulwark Plate", "chestpiece", "marshbulwark_plate", 1, 21, 58, 7, 13, 16, 730, "Heavy armor sealed against water and rot"),
            equipment("royal_wardplate", "Royal Wardplate", "chestpiece", "royal_wardplate", 2, 24, 66, 8, 15, 18, 910, "Old royal plate rebuilt with modern wards"),
            equipment("starforged_plate", "Starforged Plate", "chestpiece", "starforged_plate", 3, 28, 78, 10, 18, 99, 1120, "A near-mythic suit of fallen star-metal"),
            equipment("copper_signet", "Copper Signet", "ring", "copper_signet", 1, 1, 2, 0, 1, 3, 18, "A simple ring stamped with a road mark"),
            equipment("river_pearl_charm", "River Pearl Charm", "necklace", "river_pearl_charm", 0, 1, 5, 3, 2, 4, 36, "A small pearl tied on blue cord"),
            equipment("emberglass_ring", "Emberglass Ring", "ring", "emberglass_ring", 2, 0, 0, 5, 3, 5, 62, "A red glass ring that never cools"),
            equipment("frostdrop_pendant", "Frostdrop Pendant", "necklace", "frostdrop_pendant", 0, 2, 6, 5, 4, 6, 84, "A pendant beaded with permanent frost"),
            equipment("stormbead_brooch", "Stormbead Brooch", "necklace", "stormbead_brooch", 2, 1, 0, 7, 5, 7, 110, "A copper brooch that clicks before storms"),
            equipment("sunward_medallion", "Sunward Medallion", "necklace", "sunward_medallion", 1, 2, 10, 6, 6, 8, 140, "A warm medallion favored by field healers"),
            equipment("moonthread_necklace", "Moonthread Necklace", "necklace", "moonthread_necklace", 0, 1, 6, 12, 7, 9, 172, "Silver thread knotted around pale gems"),
            equipment("thornroot_charm", "Thornroot Charm", "belt", "thornroot_charm", 2, 2, 12, 4, 8, 10, 208, "A knotted charm that smells of green earth"),
            equipment("marshlight_seal", "Marshlight Seal", "ring", "marshlight_seal", 1, 3, 14, 6, 9, 11, 250, "A seal ring lit like a distant lantern"),
            equipment("dunestar_talisman", "Dunestar Talisman", "necklace", "dunestar_talisman", 3, 1, 0, 12, 10, 12, 300, "A desert charm cut from pale glass"),
            equipment("archive_lens", "Archive Lens", "necklace", "archive_lens", 1, 2, 0, 18, 11, 13, 360, "A polished lens in a brass setting"),
            equipment("shadowband", "Shadowband", "ring", "shadowband", 4, 2, 6, 10, 12, 14, 430, "A dark ring that blurs at the edges"),
            equipment("heartstone_locket", "Heartstone Locket", "necklace", "heartstone_locket", 1, 3, 26, 12, 13, 16, 525, "A red locket with a steady heartbeat"),
            equipment("starrelic_ring", "Starrelic Ring", "ring", "starrelic_ring", 4, 3, 10, 18, 15, 18, 660, "A ring of silver metal and impossible light"),
            equipment("phoenix_crown_pin", "Phoenix Crown Pin", "necklace", "phoenix_crown_pin", 5, 4, 30, 16, 18, 99, 820, "A bright pin shaped like a rising flame")
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
            Map.entry("riverside", new Shop("riverside", "Riverside Market", List.of(
                    "potion_small", "ether", "guard_tonic", "traveler_cloak", "iron_sword", "iron_ring", "silver_mace",
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
                    "archive_lens", "shadowband", "heartstone_locket", "starrelic_ring", "phoenix_crown_pin"
            ))),
            Map.entry("highwall", new Shop("highwall", "Highwall Quartermaster", List.of(
                    "potion_small", "potion_large", "ether", "battle_kit", "steel_sword", "iron_mail", "ranger_coat",
                    "shadow_dagger", "iron_helm", "guard_gauntlets", "iron_boots", "steel_greaves",
                    "woodcutter_axe", "raider_hatchet", "ironbeard_axe", "steelcleaver", "frosthew_axe",
                    "embermaul_axe", "stormsplitter", "thornbite_axe", "bonehook_axe", "obsidian_chopper",
                    "sunward_axe", "marshreaper", "mountainfall_axe", "royal_halberd_axe", "starbreaker_axe",
                    "river_buckler", "oaken_roundshield", "iron_kite_shield", "towerguard_shield", "frostguard_aegis",
                    "emberward_shield", "stormwall_shield", "suncrest_shield", "shadowglass_shield", "thornwall_shield",
                    "marshreed_ward", "bonebound_shield", "royal_heater", "starforged_aegis", "heartstone_bulwark",
                    "riveted_mail", "guard_cuirass", "steel_bastion_plate", "mountain_plate", "boneguard_plate",
                    "obsidian_plate", "frostbound_plate", "emberforged_plate", "stormguard_plate", "sunwarden_plate",
                    "shadowplate_harness", "thornplate_mail", "marshbulwark_plate", "royal_wardplate", "starforged_plate"
            ))),
            Map.entry("crypt_vendor", new Shop("crypt_vendor", "Crypt Provisioner", List.of(
                    "potion_large", "ether", "guard_tonic", "battle_kit", "flame_staff", "acolyte_mantle",
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
                    "heartstone_locket", "starrelic_ring", "phoenix_crown_pin"
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
                    List.of(new Ability("Springwater", 20, 7, Ability.AbilityKind.HEAL, "party")),
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
                    Map.of("ether", 1, "potion_small", 1))
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
            ), "palisade_gaps", null)
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
        if (equipment != null) {
            return equipment.name();
        }
        String craftingName = CraftingSystem.itemName(key);
        return craftingName == null ? key : craftingName;
    }

    public static String itemIcon(String key) {
        Item item = ITEMS.get(key);
        if (item != null) {
            return item.icon();
        }
        Equipment equipment = EQUIPMENT.get(key);
        if (equipment != null) {
            return equipment.icon();
        }
        String craftingIcon = CraftingSystem.itemIcon(key);
        return craftingIcon == null ? "icon_chest" : craftingIcon;
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
        return equipment(key, name, slot, icon, attackBonus, defenseBonus, hpBonus, mpBonus, 1, 99, cost, description);
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
            int minLevel,
            int maxLevel,
            int cost,
            String description
    ) {
        return Map.entry(key, new Equipment(key, name, slot, icon, attackBonus, defenseBonus, hpBonus, mpBonus, minLevel, maxLevel, cost, description));
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
            actor.abilities.addAll(classAbilities(className));
            actor.abilities.addAll(abilities);
            for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
                actor.addItem(entry.getKey(), entry.getValue());
            }
            return actor;
        }
    }
}
