package com.alderfall.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class DialogueLibrary {
    private DialogueLibrary() {
    }

    public static DialogueSession startSession(Npc npc, Quest quest, String biomeContext, int relationship, Random random) {
        Map<String, DialogueNode> nodes = new LinkedHashMap<>();
        String greeting = greetingQuote(npc, random);
        Personality personality = personalityFor(npc);

        List<DialogueChoice> rootChoices = new ArrayList<>();
        String personal = pick(npcSpecificDialog(npc), random);
        if (!personal.isBlank()) {
            rootChoices.add(new DialogueChoice("Ask about them", "personal"));
            addNode(nodes, "personal", personal, List.of(
                    new DialogueChoice("Listen with care", "personal_more", 2),
                    new DialogueChoice("Press for secrets", "personal_push", -2),
                    new DialogueChoice("Back to topics", "root")
            ));
            addNode(nodes, "personal_more", personalFollowDialog(npc, random), backChoices());
            addNode(nodes, "personal_push", "Personal: " + npc.name() + " closes part of the story behind their eyes. Some doors do not open to force.", backChoices());
        }

        rootChoices.add(new DialogueChoice("Inspect personality", "personality"));
        addNode(nodes, "personality",
                "Personality: " + personality.name() + ". " + personality.description() + " Relationship: {{relationship}}.",
                List.of(
                        new DialogueChoice("Appreciate that", "personality_warm", 1),
                        new DialogueChoice("Challenge that", "personality_challenge", -1),
                        new DialogueChoice("Back to topics", "root")
                ));
        addNode(nodes, "personality_warm", "Personality: " + npc.name() + " seems to appreciate being understood instead of merely questioned.", backChoices());
        addNode(nodes, "personality_challenge", "Personality: " + npc.name() + " takes the challenge in, but their expression cools.", backChoices());

        String questLine = questDialog(quest, false);
        if (!questLine.isBlank()) {
            rootChoices.add(new DialogueChoice("Discuss the quest", "quest"));
            addNode(nodes, "quest", questLine, List.of(
                    new DialogueChoice("Promise to handle it", "quest_more", 1),
                    new DialogueChoice("Question the reward", "quest_doubt", -1),
                    new DialogueChoice("Back to topics", "root")
            ));
            addNode(nodes, "quest_more", questDialog(quest, true), backChoices());
            addNode(nodes, "quest_doubt", "Quest: " + npc.name() + " answers, but the warmth leaves the request. Coin is easier to count than trust.", backChoices());
        } else {
            rootChoices.add(new DialogueChoice("Ask for rumors", "rumor"));
            addNode(nodes, "rumor", rumorDialog(biomeContext, random), List.of(
                    new DialogueChoice("Encourage the story", "rumor_more", 1),
                    new DialogueChoice("Call it nonsense", "rumor_doubt", -1),
                    new DialogueChoice("Back to topics", "root")
            ));
            addNode(nodes, "rumor_more", rumorFollowDialog(biomeContext, random), backChoices());
            addNode(nodes, "rumor_doubt", "Rumor: " + npc.name() + " shrugs, but the next story will not be offered so freely.", backChoices());
        }

        rootChoices.add(new DialogueChoice("Ask about their work", "work"));
        addNode(nodes, "work", professionDialog(npc, random), List.of(
                new DialogueChoice("Respect their craft", "work_more", 1),
                new DialogueChoice("Question their methods", "work_doubt", -1),
                new DialogueChoice("Back to topics", "root")
        ));
        addNode(nodes, "work_more", professionFollowDialog(npc, random), backChoices());
        addNode(nodes, "work_doubt", "Work: " + npc.name() + " gives a thinner answer. Professional pride bruises quietly.", backChoices());

        rootChoices.add(new DialogueChoice("Ask about this place", "local"));
        addNode(nodes, "local", biomeDialog(biomeContext, random), List.of(
                new DialogueChoice("Thank them for warning", "local_more", 1),
                new DialogueChoice("Dismiss the warning", "local_dismiss", -1),
                new DialogueChoice("Back to topics", "root")
        ));
        addNode(nodes, "local_more", biomeFollowDialog(biomeContext, random), backChoices());
        addNode(nodes, "local_dismiss", "Local: " + npc.name() + " lets the warning drop. The land will make its own argument.", backChoices());

        addNode(nodes, "root", "\"" + greeting + "\"", rootChoices);
        return new DialogueSession(nodes);
    }

    private static String greetingQuote(Npc npc, Random random) {
        List<String> candidates = new ArrayList<>();
        for (String line : npc.dialog()) {
            String cleaned = cleanGreetingLine(line);
            if (!cleaned.isBlank()) {
                candidates.add(cleaned);
            }
        }
        if (!candidates.isEmpty()) {
            return pick(candidates, random);
        }
        String work = professionDialog(npc, random);
        int colon = work.indexOf(':');
        if (colon > 0 && colon + 1 < work.length()) {
            work = work.substring(colon + 1).strip();
        }
        if (!work.isBlank()) {
            return work;
        }
        return npc.name() + " nods, measuring the road dust on your boots.";
    }

    private static String cleanGreetingLine(String line) {
        if (line == null) {
            return "";
        }
        String cleaned = line.replaceAll("\\s+", " ").strip();
        if (cleaned.isBlank()) {
            return "";
        }
        String lower = cleaned.toLowerCase();
        if (lower.startsWith("skills:")
                || lower.startsWith("abilities:")
                || lower.startsWith("pack:")
                || lower.startsWith("work:")) {
            return "";
        }
        int colon = cleaned.indexOf(':');
        if (colon > 0 && colon <= 24) {
            cleaned = cleaned.substring(colon + 1).strip();
        }
        return cleaned;
    }

    public static Personality personalityFor(Npc npc) {
        String key = npcDialogKey(npc);
        return switch (key) {
            case "marla", "liora", "elowen" -> new Personality("Compassionate", "They respond well to patience, honest concern, and choices that protect vulnerable people.");
            case "captain torin", "bran", "garruk ironwall" -> new Personality("Disciplined", "They value duty, direct promises, and respect for difficult work.");
            case "archivist ren", "scribe pela", "toma" -> new Personality("Curious", "They warm to careful questions, evidence, and people who treat stories as useful truths.");
            case "sela", "nyx", "sable" -> new Personality("Guarded", "They dislike pressure, but respect discretion, competence, and quiet honesty.");
            case "edda", "farmer joss", "old noll" -> new Personality("Practical", "They trust useful help more than grand speeches and prefer grounded answers.");
            case "hedgewise lin", "rowan wildspeaker", "vexa", "fen" -> new Personality("Wild-Touched", "They listen for respect toward land, weather, roots, and things polite folk ignore.");
            case "rain-seer imani", "bellkeeper ilya", "cairnwatch asta" -> new Personality("Watchful", "They notice tone quickly and reward careful listening over bravado.");
            case "quartermaster vesh" -> new Personality("Exacting", "They appreciate preparation, clear numbers, and people who do not waste supplies.");
            default -> switch (npcProfession(npc)) {
                case "bartender" -> new Personality("Gregarious", "They like curiosity, humor, and travelers who know a rumor is a kind of currency.");
                case "blacksmith" -> new Personality("Proud", "They respect craft, maintenance, and people who understand that tools deserve care.");
                case "merchant" -> new Personality("Transactional", "They respond to fair dealing, sharp observation, and reliable follow-through.");
                case "guard" -> new Personality("Steady", "They value respect, vigilance, and people who do not make danger louder.");
                case "scout" -> new Personality("Observant", "They appreciate restraint, route sense, and questions about what others missed.");
                default -> new Personality("Reserved", "They open up slowly and judge travelers by tone as much as action.");
            };
        };
    }

    private static String relationshipLabel(int relationship) {
        if (relationship >= 60) {
            return "trusted";
        }
        if (relationship >= 25) {
            return "friendly";
        }
        if (relationship <= -60) {
            return "hostile";
        }
        if (relationship <= -25) {
            return "wary";
        }
        return "neutral";
    }

    public static String biomeContext(Npc npc, char npcTile, char fallbackTile) {
        String mapId = npc.mapId();
        if (mapId.startsWith("house_")) {
            mapId = settlementMapFromHouse(mapId);
        }
        if (WorldMap.OVERWORLD_ID.equals(mapId)) {
            return biomeContextForTile(npcTile);
        }
        if (mapId.contains("snowrest") || mapId.contains("pineward") || mapId.contains("highwall") || mapId.contains("ironvale")) {
            return "mountain";
        }
        if (mapId.contains("dunewick") || mapId.contains("sunmere") || mapId.contains("redcairn") || mapId.contains("sanctum") || mapId.contains("embermarket")) {
            return "desert";
        }
        if (mapId.contains("mireford") || mapId.contains("glimmerfen") || mapId.contains("reedwatch") || mapId.contains("belltower") || mapId.contains("riverside")) {
            return "marsh";
        }
        if (mapId.contains("archive") || mapId.contains("moonspire")) {
            return "grave";
        }
        if (mapId.contains("oakhaven") || mapId.contains("foxbarrow") || mapId.contains("elderford") || mapId.contains("briarbridge")) {
            return "forest";
        }
        return biomeContextForTile(fallbackTile);
    }

    private static void addNode(Map<String, DialogueNode> nodes, String id, String line, List<DialogueChoice> choices) {
        nodes.put(id, new DialogueNode(id, line, choices));
    }

    private static List<DialogueChoice> backChoices() {
        return List.of(new DialogueChoice("Back to topics", "root"));
    }

    private static String pick(List<String> pool, Random random) {
        if (pool == null || pool.isEmpty()) {
            return "";
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private static String questDialog(Quest quest, boolean followUp) {
        if (quest == null) {
            return "";
        }
        if (quest.completed) {
            return followUp
                    ? "Quest: They seem lighter now that " + quest.title + " is done, though Alderfall rarely stays grateful for long."
                    : "Quest: " + quest.completeDialog;
        }
        if (quest.ready()) {
            return followUp
                    ? "Quest: The hard part is finished. The useful part is making sure the right hands know it."
                    : "Quest: " + quest.readyDialog;
        }
        if (quest.accepted) {
            return followUp
                    ? "Quest: Focus on " + quest.objectiveAction().toLowerCase() + "ing " + quest.target + ". Progress: " + quest.progress + "/" + quest.needed + "."
                    : "Quest: " + quest.progressDialog + " Progress: " + quest.progress + "/" + quest.needed + ".";
        }
        return followUp
                ? "Quest: If you take the work, it pays " + quest.rewardGold + " gold and " + quest.rewardXp + " experience."
                : "Quest: " + quest.startDialog;
    }

    private static String professionDialog(Npc npc, Random random) {
        String profession = npcProfession(npc);
        List<String> pool = switch (profession) {
            case "bartender" -> List.of(
                    "Rumor: A roadwarden swore the old milestones moved overnight. I told them milestones drink less than roadwardens, but I wrote it down anyway.",
                    "Rumor: Three miners came through with frost in their beards and sand in their packs. Nobody gets that lost by accident.",
                    "Rumor: Someone is buying empty bottles by the crate. Either alchemy is booming or grief is."
            );
            case "blacksmith" -> List.of(
                    "Forge: Snow, sand, marshwater, road dust; every biome ruins metal in its own smug little way.",
                    "Forge: A clean blade tells me its owner has not used it. A nicked blade tells me where they panic.",
                    "Forge: Bring me ore, old hinges, or enemy buckles. I am not romantic about metal."
            );
            case "baker" -> List.of(
                    "Oven: Bread listens to weather. Desert dough dries fast, marsh loaves sulk, and mountain yeast needs more patience than a priest.",
                    "Oven: People confess more over warm bread than under threat. Hunger is honest that way.",
                    "Oven: If the crust cracks too wide, rain is coming. If it stays pale, trouble is already here."
            );
            case "merchant" -> List.of(
                    "Trade: I price goods by distance and dread. Anything hauled through snow or dunes earns its markup honestly.",
                    "Trade: The best cargo is boring. The worst cargo whispers from inside the crate.",
                    "Trade: I can smell counterfeit coin, wet wool, and desperate customers. Only one of those gets a discount."
            );
            case "archivist" -> List.of(
                    "Records: The oldest maps describe Alderfall by smell first: pine resin, wet reeds, hot stone, grave dust.",
                    "Records: History is not dead. It is merely badly indexed and waiting to embarrass the living.",
                    "Records: Any ruin with perfect silence is lying. Stone always remembers something."
            );
            case "guard" -> List.of(
                    "Watch: Trouble changes uniforms by region, but it always leaves tracks if you look before it looks at you.",
                    "Watch: We rotate the night posts so fear does not learn anyone's favorite hiding place.",
                    "Watch: A quiet road is either safe, watched, or bait. I prefer knowing which."
            );
            case "healer" -> List.of(
                    "Remedy: Desert folk need water, mountain folk need warmth, marsh folk need clean cuts. The rest is listening.",
                    "Remedy: Half of medicine is asking where it hurts. The other half is noticing where they avoid answering.",
                    "Remedy: If a wound smells like copper, work quickly. If it smells sweet, work faster."
            );
            case "farmer" -> List.of(
                    "Fieldwork: Good soil tells the truth. If the birds go quiet or the wind changes taste, bring the tools inside.",
                    "Fieldwork: You learn patience from crops, suspicion from crows, and bargaining from weather.",
                    "Fieldwork: A fence is just a polite argument with the wilderness."
            );
            case "scout" -> List.of(
                    "Trailcraft: The safest path is rarely the shortest. I trust bent grass, loose pebbles, and old camp ash more than signs.",
                    "Trailcraft: If the road looks empty, check what the birds are avoiding.",
                    "Trailcraft: I mark routes by what can go wrong there. It makes my maps unpopular and useful."
            );
            case "quartermaster" -> List.of(
                    "Supplies: Count arrows twice, medicine three times, and socks every time it rains. Armies have failed for less.",
                    "Supplies: A heroic charge eats rations like anyone else.",
                    "Supplies: People remember who swung the sword. I remember who packed the bandages."
            );
            default -> List.of(
                    "Local talk: Everyone here watches the horizon. In Alderfall, the horizon usually watches back.",
                    "Local talk: You can tell who is new by who trusts a quiet morning.",
                    "Local talk: If people seem jumpy, it is because the land has started answering back."
            );
        };
        return pick(pool, random);
    }

    private static List<String> npcSpecificDialog(Npc npc) {
        return switch (npcDialogKey(npc)) {
            case "marla" -> List.of(
                    "Personal: I keep a list of patients who lived because someone came back with muddy boots and useful herbs.",
                    "Personal: Riverside calls me stern. Fever calls me worse."
            );
            case "archivist ren" -> List.of(
                    "Personal: I trust ink more than memory, but less than scars.",
                    "Personal: If I join you, I am bringing notebooks. Heroics without citations are just shouting."
            );
            case "scribe pela" -> List.of(
                    "Personal: I sharpen charcoal before knives. A copied name can keep a ghost quieter than steel.",
                    "Personal: The Archive smells of dust, wax, and people pretending not to be afraid."
            );
            case "captain torin" -> List.of(
                    "Personal: Command is mostly counting who did not come back and still giving the next order.",
                    "Personal: Highwall taught me to sleep in armor. I do not recommend it, but it works."
            );
            case "signal keeper lysa" -> List.of(
                    "Personal: Smoke has grammar. Raiders are learning it badly, which makes them dangerous.",
                    "Personal: My worst fear is a clear sky on the wrong day."
            );
            case "scout rook" -> List.of(
                    "Personal: I name every ambush site after what I wish I had eaten instead.",
                    "Personal: A scout's pride is coming back with boring news. I miss pride."
            );
            case "trapmaster yaro" -> List.of(
                    "Personal: I respect a tidy snare. I hate finding one with my shin.",
                    "Personal: Give me wire, patience, and bad intentions, and I can make a road nervous."
            );
            case "bellkeeper ilya" -> List.of(
                    "Personal: Bells do not lie. People hear whatever guilt teaches them.",
                    "Personal: When Belltower goes quiet, even the gulls look over their shoulders."
            );
            case "net-mender corso" -> List.of(
                    "Personal: I can mend rope in the dark. I prefer not to discover what else is in the dark.",
                    "Personal: Spider silk pays well, if it is not still attached to opinions."
            );
            case "warden sol" -> List.of(
                    "Personal: Wards are promises carved deeply enough that stone feels obliged.",
                    "Personal: Sanctum survives because someone always stays awake under the floor."
            );
            case "brother cal" -> List.of(
                    "Personal: I pray politely and carry a mallet for the things that answer impolitely.",
                    "Personal: A charm that hums is comforting. A charm that stops suddenly is paperwork."
            );
            case "quartermaster vesh" -> List.of(
                    "Personal: I can forgive panic. I cannot forgive an empty inventory ledger.",
                    "Personal: If you return with fewer supplies than wounds, I will have questions."
            );
            case "eira" -> List.of(
                    "Personal: I build cairns because the snow tries to make every road a rumor.",
                    "Personal: Wolves respect warmth, hunger, and nothing else."
            );
            case "ash watcher kera" -> List.of(
                    "Personal: I listen to ashes. They are less dramatic than prophets and usually more accurate.",
                    "Personal: If a cold hearth laughs, leave first and investigate second."
            );
            case "edda" -> List.of(
                    "Personal: I have fed cowards, heroes, and people smart enough to be both.",
                    "Personal: Oakhaven survives because nobody here lets an empty bowl stay empty."
            );
            case "hedgewise lin" -> List.of(
                    "Personal: Hedges gossip through roots. Most of it is weather, some of it is murder.",
                    "Personal: I used to prune roses. Now I negotiate with brambles carrying grudges."
            );
            case "farmer joss" -> List.of(
                    "Personal: Horses complain honestly. People could learn from them.",
                    "Personal: If the watch mounts are hungry, everyone suddenly discovers how far walking is."
            );
            case "rowan" -> List.of(
                    "Personal: Straw should stay where you leave it. That is the foundation of civilization.",
                    "Personal: I am trying very hard not to lose an argument with a scarecrow."
            );
            case "mira" -> List.of(
                    "Personal: I tie blue cord on my crates because raiders remember color worse than guilt.",
                    "Personal: A merchant who cannot track losses is just a bard with heavier bags."
            );
            case "bran" -> List.of(
                    "Personal: I charge for my shield because people value what costs them.",
                    "Personal: Roadwatch discipline is simple: stand up, stay awake, do not romanticize dying."
            );
            case "liora" -> List.of(
                    "Personal: I lecture after I stitch. People listen better when their blood is staying put.",
                    "Personal: Tea, bandages, and blunt honesty have saved more camps than speeches."
            );
            case "rowan wildspeaker" -> List.of(
                    "Personal: The wilds do not obey me. We have an understanding and mutual doubts.",
                    "Personal: A friendly hedge is still a hedge. Ask before leaning."
            );
            case "niva" -> List.of(
                    "Personal: Snow writes everything down. My work is reading before the wind edits.",
                    "Personal: Warm hands are holy in Snowrest, whatever the priests say."
            );
            case "garruk ironwall" -> List.of(
                    "Personal: A shield is a door you bring to an argument.",
                    "Personal: I have been called stubborn by avalanches. I consider that professional respect."
            );
            case "elowen" -> List.of(
                    "Personal: Frost remembers spring. Wounds remember gentleness too, given time.",
                    "Personal: I carry seeds because even battlefields eventually need a better idea."
            );
            case "cairnwatch asta" -> List.of(
                    "Personal: My job is proving the dead were here before snow says otherwise.",
                    "Personal: A cairn is just stones until someone depends on it."
            );
            case "pass guide olin" -> List.of(
                    "Personal: I have cursed the same snowdrift for six winters. We are practically kin.",
                    "Personal: The pass does not hate travelers. It simply forgets we are fragile."
            );
            case "sela" -> List.of(
                    "Personal: Shade is a currency in Dunewick. I spend mine carefully.",
                    "Personal: A well that lies is still useful if you know what it wants."
            );
            case "rain-seer imani" -> List.of(
                    "Personal: I inherited one rainstorm, three arguments about it, and a very dry sense of humor.",
                    "Personal: In Dunewick, hope sounds like a bucket hitting water."
            );
            case "kael" -> List.of(
                    "Personal: Two blades are not showing off. Showing off is surviving with one.",
                    "Personal: I like clean exits, sharp steel, and employers who can count."
            );
            case "nyx" -> List.of(
                    "Personal: If I wanted to be found, I would wear brighter boots.",
                    "Personal: Secrets are lighter than armor and usually block more damage."
            );
            case "orin stonebreaker" -> List.of(
                    "Personal: The trick to a hammer is knowing when the world is asking to be simpler.",
                    "Personal: Stone argues slowly. I answer quickly."
            );
            case "toma" -> List.of(
                    "Personal: Ledgers are just confessions with columns.",
                    "Personal: Raiders keeping books is either progress or doom wearing spectacles."
            );
            case "fen" -> List.of(
                    "Personal: The marsh answers questions eventually. The price is usually socks.",
                    "Personal: Boglight is useful because it shows you what wishes it had stayed hidden."
            );
            case "mira sunwarden" -> List.of(
                    "Personal: A lantern is not brave. It simply makes bravery easier to locate.",
                    "Personal: Darkness is less frightening when you give it edges."
            );
            case "vexa" -> List.of(
                    "Personal: Thorns are honest. They announce the terms and keep them.",
                    "Personal: I cultivate plants that make enemies reconsider shortcuts."
            );
            case "sable" -> List.of(
                    "Personal: I dislike noise, loose buckles, and people who call murder messy.",
                    "Personal: Locks are conversations. Most surrender to patience or embarrassment."
            );
            case "old noll" -> List.of(
                    "Personal: A fence is a wall with humility. Raiders never understand humility.",
                    "Personal: I have trusted mud more than men and been disappointed less often."
            );
            default -> List.of();
        };
    }

    private static String rumorDialog(String biomeContext, Random random) {
        List<String> pool = switch (biomeContext) {
            case "mountain" -> List.of(
                    "Rumor: A mule came down from the pass alone with blue candle wax on its saddle.",
                    "Rumor: Someone saw lanterns moving inside a whiteout, all in a perfect line."
            );
            case "desert" -> List.of(
                    "Rumor: A dune near the old ruins rings like a bell when the moon is thin.",
                    "Rumor: Dunewick children trade stories about rain the way city children trade sweets."
            );
            case "marsh" -> List.of(
                    "Rumor: The reeds near the black pools have started repeating names in the wrong voices.",
                    "Rumor: A fisher pulled up a boot full of clean, dry ash. Nobody liked that."
            );
            case "forest" -> List.of(
                    "Rumor: Oakhaven's oldest tree dropped green leaves during frost and nobody slept well after.",
                    "Rumor: There is a deer on the north road that watches campfires until they go out."
            );
            case "grave" -> List.of(
                    "Rumor: One grave bell rang underground for a full minute, then apologized in a child's voice.",
                    "Rumor: The Archive locked a map away because it kept adding fresh graves."
            );
            default -> List.of(
                    "Rumor: A trader paid double for broken charms and refused to say why.",
                    "Rumor: Travelers keep finding the same black feather in different inns."
            );
        };
        return pick(pool, random);
    }

    private static String personalFollowDialog(Npc npc, Random random) {
        List<String> pool = npcSpecificDialog(npc);
        if (pool.size() > 1) {
            return pool.get(random.nextInt(pool.size()));
        }
        return "Personal: " + npc.name() + " studies you for a moment, deciding how much truth the road has earned today.";
    }

    private static String professionFollowDialog(Npc npc, Random random) {
        String profession = npcProfession(npc);
        List<String> pool = switch (profession) {
            case "bartender" -> List.of(
                    "Advice: If three strangers lower their voices at once, refill their cups and remember their boots.",
                    "Advice: Good rumors arrive thirsty. Bad rumors arrive already paid for."
            );
            case "blacksmith" -> List.of(
                    "Advice: Oil your blade after marsh work, warm it slowly after snow, and never trust desert grit near a hinge.",
                    "Advice: If armor pinches in the shop, it will betray you on the road."
            );
            case "baker" -> List.of(
                    "Advice: Eat before a hard road. Empty stomachs make cowards out of sensible people.",
                    "Advice: A village with flour left still believes in tomorrow."
            );
            case "merchant" -> List.of(
                    "Advice: Count payment in the shade, promises in daylight, and enemies twice.",
                    "Advice: The road tax nobody names is fear. Every caravan pays it."
            );
            case "archivist" -> List.of(
                    "Advice: Read inscriptions from the bottom up if the stone is cursed. Old wards love dramatic openings.",
                    "Advice: Never trust a clean ruin. Someone cleaned it for a reason."
            );
            case "guard" -> List.of(
                    "Advice: Keep your back to stone, your fire low, and your exit boring.",
                    "Advice: The first sound in an ambush is usually the least honest one."
            );
            case "healer" -> List.of(
                    "Advice: Wash bites, warm frost, cool burns, and do not let proud people sleep with a fever.",
                    "Advice: The patient who says they are fine is either brave, foolish, or bleeding internally."
            );
            case "farmer" -> List.of(
                    "Advice: Watch animals before weather. They complain early and without politics.",
                    "Advice: If weeds all lean away from a path, pick another path."
            );
            case "scout" -> List.of(
                    "Advice: When tracks vanish, look up. When birds vanish, leave.",
                    "Advice: A trail that looks too easy has been prepared by someone who dislikes you."
            );
            case "quartermaster" -> List.of(
                    "Advice: Spare rope, dry socks, clean water. Everything else is a debate.",
                    "Advice: Never split supplies evenly. Split them by who is most likely to run."
            );
            default -> List.of(
                    "Advice: Ask two locals the same question. If both answer quickly, worry.",
                    "Advice: Keep one hand free and one promise unpaid."
            );
        };
        return pick(pool, random);
    }

    private static String rumorFollowDialog(String biomeContext, Random random) {
        List<String> pool = switch (biomeContext) {
            case "mountain" -> List.of(
                    "Rumor: They heard it from a courier who would not remove their mittens indoors.",
                    "Rumor: Snow muffles lies poorly. That is why mountain rumors arrive half true."
            );
            case "desert" -> List.of(
                    "Rumor: A well-keeper told it first, and well-keepers do not waste breath in the heat.",
                    "Rumor: In the dunes, a story surviving until morning is already suspicious."
            );
            case "marsh" -> List.of(
                    "Rumor: It came from reedcutters, which means it is either exact truth or a joke with boots.",
                    "Rumor: Marsh stories grow in the telling, but the roots are usually real."
            );
            case "forest" -> List.of(
                    "Rumor: Oakhaven children heard it from the old road, and children are better listeners than adults admit.",
                    "Rumor: The forest changes details, not endings."
            );
            default -> List.of(
                    "Rumor: It passed through three towns and lost only one corpse in the retelling.",
                    "Rumor: I do not believe all of it. That is why I believe part of it."
            );
        };
        return pick(pool, random);
    }

    private static String biomeFollowDialog(String biomeContext, Random random) {
        List<String> pool = switch (biomeContext) {
            case "mountain" -> List.of(
                    "Local: Watch for snow that falls sideways but leaves no drift. That means something larger is moving nearby.",
                    "Local: Keep metal wrapped at night. Cold makes honest tools spiteful."
            );
            case "desert" -> List.of(
                    "Local: If you see rain in the desert, check whether anything else can see it too.",
                    "Local: The hottest hours belong to lizards, spirits, and fools. Travel around all three."
            );
            case "marsh" -> List.of(
                    "Local: Step on roots, not shine. Shiny mud has plans.",
                    "Local: If your reflection moves late, stop looking and back away."
            );
            case "forest" -> List.of(
                    "Local: Do not answer voices from the trees unless they use your name correctly twice.",
                    "Local: Moss grows thickest where people stopped hurrying."
            );
            case "badlands" -> List.of(
                    "Local: Red dust hides tracks until sunset, then gives them back in gold light.",
                    "Local: Caves out there breathe warm before storms. That is useful and unpleasant."
            );
            case "grave" -> List.of(
                    "Local: Bring charcoal, salt, and manners near old stones.",
                    "Local: Never count graves aloud unless you know whether the number changed."
            );
            default -> List.of(
                    "Local: Watch what locals avoid stepping on. They learned for you.",
                    "Local: Every place has a warning sound. Survive long enough to learn this one."
            );
        };
        return pick(pool, random);
    }

    private static String biomeDialog(String biomeContext, Random random) {
        List<String> pool = switch (biomeContext) {
            case "mountain" -> List.of(
                    "Local: The snow never really stops up here. It pauses, listens at the shutters, then finds a new way into your boots.",
                    "Local: Mountain folk complain about snow the way sailors complain about water: constantly, correctly, and with affection.",
                    "Local: If the wind drops suddenly in the pass, people stop talking until it explains itself."
            );
            case "desert" -> List.of(
                    "Local: My grandmother said she saw rain on the dunes once. People still ask whether she meant water or a mirage with confidence.",
                    "Local: Desert stories always begin with a well, a shadow, or someone trusting the wrong shimmer.",
                    "Local: You can hear heat here. It ticks in roof beams and makes honest people short-tempered."
            );
            case "marsh" -> List.of(
                    "Local: The marsh keeps every secret twice: once under water, once under fog. Step where the reeds bend back.",
                    "Local: Mire paths move after heavy rain. People pretend that is mud. People are optimistic.",
                    "Local: If the frogs go quiet, check your boots, your purse, and your courage."
            );
            case "forest" -> List.of(
                    "Local: The trees are friendly by daylight. At dusk they begin repeating sounds they should not know.",
                    "Local: Forest people judge weather by leaf backs, birdsong, and whether the old roots ache.",
                    "Local: Oakhaven calls this green country. The green country calls us temporary."
            );
            case "badlands" -> List.of(
                    "Local: Red dust gets into bread, bandages, letters, prayers. After a week you stop fighting it and start naming shades.",
                    "Local: Badlands wind polishes bones and secrets with equal patience.",
                    "Local: There are places out there where your echo comes back tired."
            );
            case "water" -> List.of(
                    "Local: River people can tell depth by color, current by smell, and bad weather by how quiet the gulls get.",
                    "Local: Water roads are roads that remember every mistake.",
                    "Local: If the river looks still, it is either deep, cold, or thinking."
            );
            case "grave" -> List.of(
                    "Local: Around the old graves, people lower their voices even when no one taught them to.",
                    "Local: Graveyard weather feels colder because names keep shade.",
                    "Local: The old stones lean like listeners. I try not to give them fresh gossip."
            );
            default -> List.of(
                    "Local: This place has its own weather, its own warnings, and its own way of testing travelers.",
                    "Local: Every road in Alderfall has a favorite lie. Learning it is how you survive.",
                    "Local: People here read the sky before they read letters."
            );
        };
        return pick(pool, random);
    }

    private static String npcProfession(Npc npc) {
        String text = (npc.name() + " " + npc.sprite() + " " + (npc.shopId() == null ? "" : npc.shopId())).toLowerCase();
        if (text.contains("bartender") || text.contains("tavern") || text.contains("innkeeper")) {
            return "bartender";
        }
        if (text.contains("trapmaster") || text.contains("guide") || text.contains("rain-seer") || text.contains("scout") || text.contains("rook") || text.contains("rowan") || text.contains("fen")) {
            return "scout";
        }
        if (text.contains("blacksmith") || text.contains("forge")) {
            return "blacksmith";
        }
        if (text.contains("baker") || text.contains("edda")) {
            return "baker";
        }
        if (text.contains("merchant") || text.contains("market") || text.contains("sela") || text.contains("mira")) {
            return "merchant";
        }
        if (text.contains("archivist") || text.contains("scribe") || text.contains("ren") || text.contains("pela")) {
            return "archivist";
        }
        if (text.contains("captain") || text.contains("warden") || text.contains("watcher") || text.contains("guard") || text.contains("torin") || text.contains("bran")) {
            return "guard";
        }
        if (text.contains("medic") || text.contains("healer") || text.contains("marla") || text.contains("brother") || text.contains("eira") || text.contains("niva")) {
            return "healer";
        }
        if (text.contains("farmer") || text.contains("joss") || text.contains("hedgewise") || text.contains("reedcutter") || text.contains("net-mender")) {
            return "farmer";
        }
        if (text.contains("quartermaster")) {
            return "quartermaster";
        }
        return "citizen";
    }

    private static String npcDialogKey(Npc npc) {
        return npc.name().strip().toLowerCase();
    }

    private static String settlementMapFromHouse(String mapId) {
        int last = mapId.lastIndexOf('_');
        if (last <= "house_".length()) {
            return mapId;
        }
        int previous = mapId.lastIndexOf('_', last - 1);
        if (previous <= "house_".length()) {
            return mapId;
        }
        return mapId.substring("house_".length(), previous);
    }

    private static String biomeContextForTile(char tile) {
        return switch (tile) {
            case 'n', 'q', 'm' -> "mountain";
            case 's' -> "desert";
            case 'v' -> "marsh";
            case 'f', 'g' -> "forest";
            case 'b' -> "badlands";
            case 'w' -> "water";
            case 'd' -> "grave";
            default -> "road";
        };
    }

    public static final class DialogueSession {
        private final Map<String, DialogueNode> nodes;
        private String currentNodeId = "root";

        private DialogueSession(Map<String, DialogueNode> nodes) {
            this.nodes = nodes;
        }

        public String line() {
            return line(0);
        }

        public String line(int relationship) {
            String line = currentNode().line();
            if (line.contains("{{relationship}}")) {
                return line.replace("{{relationship}}", relationshipLabel(relationship) + " (" + relationship + ")");
            }
            return currentNode().line();
        }

        public List<String> optionLabels() {
            return currentNode().choices().stream()
                    .map(DialogueChoice::label)
                    .toList();
        }

        public DialogueChoiceResult choose(int index) {
            List<DialogueChoice> choices = currentNode().choices();
            if (index < 0 || index >= choices.size()) {
                return new DialogueChoiceResult(0);
            }
            DialogueChoice choice = choices.get(index);
            String nextNodeId = choice.nextNodeId();
            if (!nodes.containsKey(nextNodeId)) {
                return new DialogueChoiceResult(0);
            }
            currentNodeId = nextNodeId;
            return new DialogueChoiceResult(choice.relationshipDelta());
        }

        public boolean hasOptions() {
            return !currentNode().choices().isEmpty();
        }

        private DialogueNode currentNode() {
            return nodes.getOrDefault(currentNodeId, nodes.get("root"));
        }
    }

    private record DialogueNode(String id, String line, List<DialogueChoice> choices) {
    }

    public record DialogueChoiceResult(int relationshipDelta) {
    }

    public record Personality(String name, String description) {
    }

    private record DialogueChoice(String label, String nextNodeId, int relationshipDelta) {
        private DialogueChoice(String label, String nextNodeId) {
            this(label, nextNodeId, 0);
        }
    }
}
