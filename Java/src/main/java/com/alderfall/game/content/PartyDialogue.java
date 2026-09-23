package com.alderfall.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Authored pair scenes; shared hunts use ordinary quest persistence and journal entries. */
public final class PartyDialogue {
    public record Pair(String first, String second, String title, List<String> road,
                       List<String> offer, List<String> victory) {
        public String questId() { return "party_hunt_" + first + "_" + second; }
        public String names() { return name(first) + " and " + name(second); }
    }

    public static final List<Pair> PAIRS = List.of(
        new Pair("cassia", "lyra", "Room to Breathe",
            List.of("You keep checking my stride. Is it that bad?", "Only when you think nobody is looking. Tell me which knee.", "Left. I'd rather you heard it from me than from the stairs."),
            List.of("Lyra, will you take a dungeon leader with me? I want to practice holding the front while you work.", "If we both go, yes. But we retreat when someone needs it. Finishing a fight isn't worth losing a patient.", "Agreed. One leader, both of us present. We choose the dungeon together."),
            List.of("The leader is down. Lyra, who needs you first?", "Let me check everyone before we move. Keep that space clear.", "I'll stay here. The rest can wait.")),
        new Pair("maera", "samir", "Faith under Pressure",
            List.of("Do you always pray before opening a book?", "Only when you tell me its last reader caught fire.", "A sensible distinction. I'll mark those covers."),
            List.of("Samir, I'd like us to face a dungeon leader together. I need someone who will challenge my judgment before I cast.", "I'll question you. You'll have to let me finish, even if you're certain.", "Fair. One dungeon leader with both of us there. Certainty can wait until afterward."),
            List.of("We won. That doesn't prove every decision I made was sound.", "Good. Then we can talk about them without calling it a trial of faith.", "Over something warm, preferably. My hands are still shaking.")),
        new Pair("aria", "vesper", "A Way Back Out",
            List.of("You stopped me stepping on a mushroom. Is it poisonous?", "No. You had the whole path to choose from.", "Fair. Next one gets a wider berth."),
            List.of("Vesper, come after a dungeon leader with me. I want another pair of eyes on the way back out.", "Yes, if we leave the rest alone when we can. Going underground isn't permission to kill everything living there.", "One leader. Both of us. I'll keep looking behind us as well as ahead."),
            List.of("That's the leader. I'm ready to head back.", "So am I. We don't need to search every corner for another fight.", "A remarkably sensible end to an expedition.")),
        new Pair("seraphine", "rafiq", "No Convenient Exit",
            List.of("Was that bow for me or for your reflection?", "For you. The reflection already knows how impressive I am.", "Then it has been given misleading information."),
            List.of("Rafiq, a modest proposal: face a dungeon leader with me, and stay until the fight is settled.", "You make staying sound suspiciously difficult. But yes. I want to be someone you can count on.", "Then we both come. Retreat together if we must; no disappearing and calling it strategy."),
            List.of("You stayed. I noticed.", "I was afraid, if that spoils the effect.", "It improves it. I didn't ask you to be fearless.")),
        new Pair("calder", "cassia", "Hold Together",
            List.of("That shield strap needs tightening.", "You can tell from there?", "I can hear it. Give me a moment when we stop."),
            List.of("Cassia, take a dungeon leader with me. I want to work beside someone who'll tell me when my position is wrong.", "I will. You tell me when I'm holding ground we ought to give up.", "Done. One leader with both of us in the fight. Neither of us has to hold alone."),
            List.of("Still standing. I could use a rest before another fight.", "Take it. I'll watch while you catch your breath.", "Thank you. That used to be harder to ask."))
    );

    private static Pair road(String first, String second, String opening, String reply) {
        return new Pair(first, second, "", List.of(opening, reply), List.of(), List.of());
    }
    public static final List<Pair> ROAD_PAIRS;
    static {
        var pairs = new ArrayList<>(PAIRS);
        pairs.addAll(List.of(
            road("seraphine", "maera", "Do you correct every book you borrow?", "Only in pencil. I'm a guest, not a vandal."),
            road("seraphine", "cassia", "You stand where you can see both doors. Habit?", "Yes. You sit where you can reach one. Same question."),
            road("seraphine", "lyra", "You never ask whether someone can pay before treating them.", "Bleeding is a poor time to negotiate."),
            road("seraphine", "samir", "Would your temple welcome someone who came only for supper?", "I'd welcome them. We could discuss the temple after they ate."),
            road("seraphine", "aria", "You checked that purse rather quickly.", "My own purse. I like to know we're both paying attention."),
            road("seraphine", "vesper", "Could you grow roses without thorns?", "You could wear gloves. That asks less of the roses."),
            road("seraphine", "calder", "What would you charge to build a door that never sticks?", "Less than a door that only opens for people you like."),
            road("maera", "cassia", "May I sketch the dents in your shield?", "When we stop. And ask before you put names beside them."),
            road("maera", "lyra", "I forgot lunch again, didn't I?", "You asked me that yesterday. Put the bread beside your notes."),
            road("maera", "aria", "How do you remember a route without writing it down?", "I remember the turns I got wrong. Very effective. Not recommended."),
            road("maera", "vesper", "Does that plant have a name I can look up?", "Several. Draw its leaves too, or your book may send you to the wrong one."),
            road("maera", "rafiq", "Your account of that duel changes each time you tell it.", "The footwork doesn't. I'll show you that part without commentary."),
            road("maera", "calder", "Do you keep your early drawings?", "Especially the wrong ones. Saves me making the same mistake twice."),
            road("cassia", "samir", "Does prayer help when you're afraid?", "Sometimes. Having someone stay beside me helps more."),
            road("cassia", "aria", "Give me a signal before you scout ahead.", "Two fingers, then I wait for your nod. I don't want you guessing either."),
            road("cassia", "vesper", "Those roots would trip someone in armor.", "Then warn whoever follows us. Cutting them won't make every path safe."),
            road("cassia", "rafiq", "You don't have to salute me.", "Good. I wasn't certain I was doing it correctly."),
            road("lyra", "samir", "Would you sit with someone even if they didn't want a prayer?", "Of course. Silence doesn't offend me."),
            road("lyra", "aria", "Let me see that scratch before you cover it.", "All right. But you may have to hold the sleeve. It sticks."),
            road("lyra", "vesper", "Will this herb keep if I dry it?", "The leaves will. Leave the root in the ground and we can gather again."),
            road("lyra", "rafiq", "Is the limp real this time?", "Yes. I realize I've made this unnecessarily difficult for both of us."),
            road("lyra", "calder", "You carry enough tools for three people.", "I could let someone else carry the hammer. That would be a start."),
            road("samir", "aria", "You went very quiet when I began singing.", "I was listening. You're allowed to finish."),
            road("samir", "vesper", "Do you mind if I say a blessing before we eat?", "No. Just keep the food covered. Blessings won't discourage the flies."),
            road("samir", "rafiq", "You can say you're worried without making it a joke.", "I know. Give me a moment to find the words."),
            road("samir", "calder", "Do you ever miss the islands?", "Often. Mostly when someone here serves fish they've boiled into surrender."),
            road("aria", "rafiq", "Your buckle catches the light from quite a distance.", "An excellent warning. I'll cover it before you make me crawl anywhere."),
            road("aria", "calder", "Could you make my boot soles quieter?", "I can mend the loose heel. After that, you'll have to stop kicking stones."),
            road("vesper", "rafiq", "Did you pick that flower for someone?", "I was going to. You've made leaving it there seem rather more promising."),
            road("vesper", "calder", "Could your next bridge leave more room along the bank?", "Show me how much. I want that in the drawing before anyone cuts timber."),
            road("rafiq", "calder", "Do you ever make something simply because it's beautiful?", "Yes. A chair can hold someone's weight and still be worth looking at.")
        ));
        ROAD_PAIRS = List.copyOf(pairs);
    }

    private record Scene(Pair pair, List<String> lines, boolean offer, String key) {}
    private final Deque<Scene> pending = new ArrayDeque<>();
    private final Map<String, Integer> lastPlayed = new HashMap<>();
    private Scene active;
    private int turn;

    public static String name(String id) { return GameData.RECRUITS.get(id).name(); }
    public static boolean isShared(Quest quest) { return quest.id.startsWith("party_hunt_"); }
    public static Pair pair(Quest quest) {
        return PAIRS.stream().filter(p -> p.questId().equals(quest.id)).findFirst().orElse(null);
    }
    public void install(GameState state) {
        for (Pair pair : PAIRS) {
            String description = "Defeat one dungeon leader with " + pair.names()
                + " in the battle party. Any dungeon's boss qualifies; ordinary enemies do not."
                + " Complete automatically on victory. If either companion stays home, the quest remains open.";
            state.quests.put(pair.questId(), new Quest(pair.questId(), pair.title(), description,
                "Dungeon leader", 1, 60, 80, Quest.ObjectiveKind.DEFEAT,
                com.alderfall.game.map.WorldMap.OVERWORLD_ID, "", 0, "", "",
                description, description, "The dungeon leader is defeated; the shared expedition is complete.",
                pair.names() + " defeated a dungeon leader together. Both companions remember the expedition."));
        }
    }
    public void reset() { active = null; pending.clear(); lastPlayed.clear(); }
    private boolean present(GameState state, Pair pair) {
        var names = state.activeAllies().stream().map(a -> a.name).toList();
        return names.contains(name(pair.first())) && names.contains(name(pair.second()));
    }
    public boolean owns(GameState.TravelBanterPrompt prompt) {
        return prompt != null && prompt.tags().contains("party_scene");
    }
    public boolean valid(GameState state) { return active != null && present(state, active.pair()); }
    public void cancel() { active = null; }

    public GameState.TravelBanterPrompt startPending(GameState state) {
        while (!pending.isEmpty()) {
            Scene scene = pending.removeFirst();
            if (present(state, scene.pair())) return start(state, scene);
        }
        return null;
    }
    public GameState.TravelBanterPrompt ambient(GameState state) {
        List<Scene> candidates = new ArrayList<>();
        for (Pair pair : ROAD_PAIRS) {
            if (!present(state, pair)) continue;
            Quest quest = state.quests.get(pair.questId());
            boolean offer = quest != null && !quest.accepted && !quest.completed && lastPlayed.containsKey(pair.questId() + ":road");
            String key = pair.questId() + (offer ? ":offer" : ":road");
            if (state.worldTick - lastPlayed.getOrDefault(key, -100000) < 12000) continue;
            candidates.add(new Scene(pair, offer ? pair.offer() : pair.road(), offer, key));
        }
        if (candidates.isEmpty()) return null;
        return start(state, candidates.get(state.random.nextInt(candidates.size())));
    }
    private GameState.TravelBanterPrompt start(GameState state, Scene scene) {
        active = scene;
        turn = 0;
        lastPlayed.put(scene.key(), state.worldTick);
        return prompt(state);
    }
    private GameState.TravelBanterPrompt prompt(GameState state) {
        String speaker = name(turn % 2 == 0 ? active.pair().first() : active.pair().second());
        boolean last = turn == active.lines().size() - 1;
        List<String> options = last ? (active.offer() ? List.of("Let's take this on together.", "Not now.") : List.of("Continue traveling."))
            : List.of("Listen.", "Continue traveling.");
        return new GameState.TravelBanterPrompt(speaker, speaker + ": " + active.lines().get(turn),
            options, List.of(), List.of(), List.of("party_scene"), List.of(), state.worldTick + 900);
    }
    public GameState.TravelBanterPrompt reply(GameState state, int option) {
        if (!valid(state)) { active = null; return null; }
        boolean last = turn == active.lines().size() - 1;
        if (last && active.offer() && option == 0) {
            Quest quest = state.quests.get(active.pair().questId());
            if (!quest.completed) {
                quest.accepted = true;
                state.status = "Shared quest accepted: " + quest.title + ". " + quest.description;
            }
        }
        if (last || option != 0) { active = null; return null; }
        turn++;
        return prompt(state);
    }
    public void questEvent(GameState state, String eventId) {
        if (eventId == null || !(eventId.startsWith("quest_start:") || eventId.startsWith("quest_complete:"))) return;
        Quest quest = state.quests.get(eventId.substring(eventId.indexOf(':') + 1));
        if (quest == null || !quest.companionQuest()) return;
        for (Pair original : ROAD_PAIRS) {
            if (!present(state, original)) continue;
            Pair pair = original;
            if (pair.second().equals(quest.chainOwnerId)) {
                pair = new Pair(original.second(), original.first(), "", List.of(), List.of(), List.of());
            }
            if (!pair.first().equals(quest.chainOwnerId)) continue;
            boolean complete = eventId.startsWith("quest_complete:");
            String opening = complete ? "We've finished " + quest.title + ". Thank you for coming with me."
                : "We've agreed to help with " + quest.title + ". I'd like your company.";
            String reply = switch (pair.second()) {
                case "lyra" -> complete ? "I'm glad I came. Tell me if you need a moment before we move on." : "I'll come. Tell me who needs help first.";
                case "samir" -> complete ? "I'm glad we saw it through. What do you make of it now?" : "Of course. Tell me what you know, and what you're still unsure of.";
                case "vesper" -> complete ? "Take a breath. We can decide where to go next after that." : "I'll come. We'll look carefully before we act.";
                case "rafiq" -> complete ? "I accept thanks in meals. Company was freely given." : "Then company you shall have. Even for the awkward parts.";
                case "seraphine" -> complete ? "You owe me nothing for coming. Let's be clear about that." : "I'll come. Asking for help needn't put you in anyone's debt.";
                case "maera" -> complete ? "I'd like to hear what you think now, before I write my own account." : "I'll bring my notes. Correct me when I get something wrong.";
                case "aria" -> complete ? "Glad I came. Next meal's somewhere with a view of the door, though." : "I'll come. Let's make sure we know the way back.";
                case "calder" -> complete ? "Glad I could help. We can stop a while if you need to." : "I'll come. Tell me what I should bring.";
                default -> complete ? "You saw it through. Take the time you need." : "I'm with you. Tell me where we're heading.";
            };
            queue(new Scene(pair, List.of(opening, reply), false, eventId));
            return;
        }
    }
    private void queue(Scene scene) {
        if (pending.stream().noneMatch(s -> s.key().equals(scene.key()))) pending.addLast(scene);
        while (pending.size() > 4) pending.removeFirst();
    }
    public void victory(GameState state, Battle battle, boolean dungeonBoss) {
        if (!dungeonBoss || !battle.finished || !battle.victory || battle.fled) return;
        var participants = battle.partyMembers().stream().map(a -> a.name).toList();
        for (Pair pair : PAIRS) {
            Quest quest = state.quests.get(pair.questId());
            if (!quest.accepted || quest.completed || !participants.contains(name(pair.first()))
                || !participants.contains(name(pair.second()))) continue;
            quest.progress = 1;
            quest.completed = true;
            state.player.gold += quest.rewardGold;
            state.player.gainXp(quest.rewardXp);
            state.rememberSharedQuest(pair, quest.title);
            queue(new Scene(pair, pair.victory(), false, quest.id + ":victory"));
            state.status = "Shared quest complete: " + quest.title + ".";
        }
    }
}
