package com.alderfall.game;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

/** Authored campaign branches, changed objective contracts, and save compatibility. */
public final class MainStoryDialogueTest {
    private static int checks;
    private static final List<String> REVISED = List.of("ms_watchtower_bells", "ms_shrine_shadow",
            "ms_frosthollow_standard", "ms_ember_socket_rite");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("out-story-refinement"));
        GameConfig config = GameConfig.load(Path.of(""));
        testConversations();
        for (String id : REVISED) testChapter(config, id);
        testMigration(config);
        testLiveMenu(config);
        exportTranscript(Path.of(args.length == 0 ? "out-story-refinement/main-story-dialogue.md" : args[0]));
        System.out.println("Main story dialogue checks passed: " + checks);
    }

    private static void testConversations() {
        for (String id : GameData.MAIN_STORY_QUEST_IDS) {
            Quest q = GameData.QUESTS.get(id).copy();
            Npc owner = owner(q);
            check(MainStoryContent.supports(q), "Missing authored campaign quest " + id);
            check(!MainStoryContent.topics(q, owner).isEmpty(), "No story questions " + id);
            for (int stage = 0; stage < q.stages.size(); stage++) {
                q.stageIndex = stage;
                q.accepted = true;
                q.progress = 0;
                q.observedStages.clear();
                Npc speaker = id.equals("ms_kingdoms_answer") ? npc(q.activeTargetNpcId()) : owner;
                var session = session(speaker, q);
                choose(session, "Discuss " + q.title + ".");
                for (MainStoryContent.Topic topic : MainStoryContent.topics(q, speaker)) {
                    check(session.optionLabels().contains(topic.question()) == topic.evidence().isBlank(),
                            "Unobserved discovery leaked into menu: " + topic.question());
                }
                for (Quest.QuestStage observed : q.stages) q.observedStages.add(observed.id());
                for (MainStoryContent.Topic topic : MainStoryContent.topics(q, speaker)) {
                    var fresh = session(speaker, q);
                    choose(fresh, "Discuss " + q.title + ".");
                    assertTopic(fresh, topic);
                }
            }
            // Optional questions never grant the stage's reward or acknowledge an exchange.
            check(q.progress == 0 && !q.completed, "Story browsing completed a quest " + id);
        }
        Quest coalition = GameData.QUESTS.get("ms_kingdoms_answer").copy();
        check(MainStoryContent.subject(coalition, npc("Maelis")).contains("Ask Mirella"), "Maelis speaks as Mirella");
        check(MainStoryContent.subject(coalition, npc("Mirella")).contains("barges"), "Contact lost its own opening");
        Quest shrine = GameData.QUESTS.get("ms_shrine_shadow").copy();
        shrine.observedStages.add("south_closed_outlet");
        check(QuestNarrative.uncertainty(shrine).contains("establish that"), "Confirmed confinement becomes speculation again");
    }

    private static void assertTopic(DialogueLibrary.DialogueSession session, MainStoryContent.Topic topic) {
        var effect = choose(session, topic.question());
        check(effect.effect().isBlank() && effect.relationshipDelta() == 0, "Information branch has a gameplay effect");
        check(session.line().equals(topic.answer()), "Selected reply did not get its authored answer");
        // Current authored exchanges have one follow-up each.
        for (MainStoryContent.Topic child : topic.replies()) {
            var reply = choose(session, child.question());
            check(reply.effect().isBlank() && reply.relationshipDelta() == 0, "Follow-up changes progress/approval");
            check(session.line().equals(child.answer()), "Follow-up contradicts selected subject");
        }
    }

    @SuppressWarnings("unchecked")
    private static void testChapter(GameConfig config, String id) throws Exception {
        GameState state = fresh(config);
        Quest q = state.quests.get(id);
        completePrerequisites(state, q);
        state.activeNpc = owner(q);
        state.handleActiveNpcQuestAction();
        check(q.accepted, "Chapter cannot be accepted: " + id);
        check(state.recruitedIds.isEmpty(), "Unexpected companion requirement");
        SaveSystem saves = new SaveSystem(Files.createTempDirectory(Path.of("out-story-refinement"), "main-chapter-"));
        int actions = 0;
        while (!q.ready()) {
            check(actions++ < 8, "Chapter stuck " + id);
            String stage = q.activeStage().id();
            var oldMenu = session(owner(q), q);
            choose(oldMenu, "Discuss " + q.title + ".");
            if (q.activeObjectiveKind().combatObjective()) {
                check(state.activeQuestObjectives().stream().noneMatch(o -> o.questId().equals(id)
                        && o.stageId().equals("north_fallen_standard")), "Standard can be read before battle");
                Object monster = ((List<?>) call(state, "questMonstersForQuest", new Class<?>[]{Quest.class}, q)).get(0);
                call(state, "startQuestMonsterBattle", new Class<?>[]{monster.getClass()}, monster);
                call(state, "recordQuestProgress", new Class<?>[]{String.class}, q.activeTarget());
                call(state, "clearDefeatedQuestMonster", new Class<?>[]{});
                state.mode = GameMode.EXPLORE;
            } else {
                var objective = state.activeQuestObjectives().stream().filter(o -> o.questId().equals(id)).findFirst().orElseThrow();
                check(state.world.isPassable(objective.mapId(), objective.x(), objective.y()), "Blocked evidence " + stage);
                state.currentMapId = objective.mapId();
                state.playerX = objective.x();
                state.playerY = objective.y();
                state.mode = GameMode.EXPLORE;
                state.interact();
                check(q.observedStages.contains(stage), "World action did not record " + stage + ": " + state.status);
                int after = q.progress;
                String next = q.activeStage().id();
                call(state, "interactQuestObjective", new Class<?>[]{GameState.QuestObjective.class}, objective);
                check(q.progress == after && q.activeStage().id().equals(next), "Old interaction executed twice " + stage);
            }
            check(!oldMenu.matches(q), "Old dialogue did not invalidate " + stage);
            check(oldMenu.choose(0).effect().isBlank(), "Stale dialogue executed an action");
            check(q.observedStages.contains(stage), "Finding not preserved " + stage);
            String next = q.activeStage().id();
            var facts = List.copyOf(q.observedStages);
            saves.save(state, "Main story discovery");
            GameState loaded = fresh(config);
            check(saves.load(loaded, state.currentSaveId), "Chapter save failed");
            state = loaded;
            q = state.quests.get(id);
            check(next.equals(q.activeStage().id()) && facts.equals(List.copyOf(q.observedStages)), "Progress/facts lost after reload");
            if (stage.equals("ember_disconnect")) {
                check(!q.observedStages.contains("ember_open_outlet"), "Disconnecting falsely releases guest");
                check(QuestNarrative.findings(q).contains("not yet been released"), "Journal skips unfinished release");
            }
        }
        state.activeNpc = owner(q);
        int gold = state.player.gold;
        state.handleActiveNpcQuestAction();
        check(q.completed && state.player.gold > gold, "Final report did not complete " + id);
        String stone = GameData.mainStoryStoneReward(id);
        check(stone.isBlank() || state.player.hasItem(stone), "Stone reward lost " + id);
        int awarded = state.player.gold;
        state.handleActiveNpcQuestAction();
        check(state.player.gold == awarded, "Duplicate reward " + id);
        saves.save(state, "Completed main chapter");
        GameState loaded = fresh(config);
        check(saves.load(loaded, state.currentSaveId), "Completed chapter did not load");
        loaded.activeNpc = owner(q);
        loaded.handleActiveNpcQuestAction();
        check(loaded.player.gold == awarded, "Reload replayed reward " + id);
    }

    private static void testMigration(GameConfig config) throws Exception {
        SaveSystem saves = new SaveSystem(Path.of("out-story-refinement"));
        Method read = SaveSystem.class.getDeclaredMethod("readQuests", String.class, GameState.class);
        read.setAccessible(true);
        for (String id : REVISED) {
            GameState state = fresh(config);
            String notice = (String) read.invoke(saves, id + ":true:false:1:0", state);
            Quest q = state.quests.get(id);
            check(q.accepted && q.progress == 0 && q.observedStages.isEmpty(), "Old counter invented new discoveries " + id);
            check(notice.contains(q.title), "Migration not explained " + id);
            read.invoke(saves, id + ":true:true:1:0", state);
            check(q.completed, "Legacy completed quest revoked " + id);
            check(QuestNarrative.subject(q).contains("no detailed findings"), "Legacy completion invented new actions " + id);
        }
    }

    private static void completePrerequisites(GameState state, Quest q) {
        String required = GameData.mainStoryPrerequisite(q.id);
        if (!required.isBlank()) {
            Quest prior = state.quests.get(required);
            if (!prior.completed) {
                completePrerequisites(state, prior);
                prior.completed = true;
            }
        }
        for (Quest prior : state.quests.values()) {
            if (q.id.equals(prior.nextQuestId) && !prior.completed) {
                completePrerequisites(state, prior);
                prior.completed = true;
            }
        }
    }

    private static void testLiveMenu(GameConfig config) {
        GameState s = fresh(config);
        Npc maelis = npc("Maelis");
        s.currentMapId = maelis.mapId();
        var p = s.npcPosition(maelis);
        s.playerX = p.x();
        s.playerY = p.y();
        check(s.talkToNpc(maelis), "Cannot speak to Maelis");
        String topic = "Discuss " + s.quests.get("ms_wake_ashes").title + ".";
        int intro = 0;
        while (!s.activeNpcDialogOptions().contains(topic) && intro++ < 5) select(s, 0);
        select(s, s.activeNpcDialogOptions().indexOf(topic));
        select(s, s.activeNpcDialogOptions().indexOf("Vaelthara let me live. Why?"));
        check(s.activeNpcDialogLine().contains("cannot tell you why"), "Live menu uses old omniscient reply");
        select(s, s.activeNpcDialogOptions().indexOf("Then bringing me here puts you in danger."));
        check(s.activeNpcDialogLine().contains("did not bring us this war"), "Live menu lost connected follow-up");
        check(!s.quests.get("ms_wake_ashes").accepted, "Question silently accepted quest");
    }

    private static void select(GameState s, int index) {
        check(index >= 0, "Missing public dialogue option");
        s.revealActiveDialogueLineInstantly();
        s.selectDialogOption(index);
    }

    private static void exportTranscript(Path path) throws Exception {
        StringBuilder out = new StringBuilder("# Main story: playable dialogue and objectives\n\n"
                + "Generated from the runtime content by `MainStoryDialogueTest`. Evidence-gated questions appear only after the named observation. "
                + "Optional questions award no progress or approval. Combat resolution is fixture-driven in the integration checks; inspections use world interactions.\n\n");
        for (String id : List.of("ms_wake_ashes", "ms_road_dust", "ms_oathstead_stand", "ms_names_dust",
                "ms_stolen_index", "ms_first_socket", "ms_watchtower_bells", "ms_raiders_pass", "ms_frosthollow_standard",
                "ms_shrine_shadow", "ms_caravan_glass", "ms_ember_socket_rite", "ms_bell_alone", "ms_medicine_mireford",
                "ms_miredepth_below", "ms_toll_ledger", "ms_redcap_trade", "ms_glowing_mud", "ms_orchard_ward",
                "ms_names_cold_stone", "ms_cold_road", "ms_missing_bell_rope", "ms_blackvault_mark", "ms_camp_defending",
                "ms_kingdoms_answer", "ms_twelve_stones_gate")) {
            Quest q = GameData.QUESTS.get(id).copy();
            out.append("## ").append(q.title).append(" (`").append(id).append("`)\n\n").append(q.description).append("\n\n");
            for (Quest.QuestStage stage : q.stages) {
                out.append("### ").append(stage.title()).append("\n\n")
                        .append("Opening: ").append(stage.startDialog()).append("\n\n")
                        .append("Action: ").append(stage.progressDialog()).append("\n\n")
                        .append("Observed result: ").append(stage.readyDialog()).append("\n\n");
            }
            out.append("Report: ").append(q.stages.get(q.stages.size() - 1).completeDialog()).append("\n\n");
            for (MainStoryContent.Topic topic : MainStoryContent.topics(q, owner(q))) exportTopic(out, topic);
            if (id.equals("ms_kingdoms_answer")) {
                for (Quest.QuestStage stage : q.stages) {
                    out.append("Speaker: ").append(stage.targetNpcId()).append("\n\n");
                    for (MainStoryContent.Topic topic : MainStoryContent.topics(q, npc(stage.targetNpcId()))) exportTopic(out, topic);
                }
            }
        }
        Files.writeString(path, out);
    }

    private static void exportTopic(StringBuilder out, MainStoryContent.Topic topic) {
        if (!topic.evidence().isBlank()) out.append("Requires observed evidence: `").append(topic.evidence()).append("`.\n\n");
        out.append("> Player: ").append(topic.question()).append("\n>\n> NPC: ").append(topic.answer()).append("\n\n");
        for (MainStoryContent.Topic child : topic.replies()) exportTopic(out, child);
    }

    private static Npc owner(Quest q) {
        return GameData.NPCS.stream().filter(n -> n.questId() != null).filter(n -> {
            Quest candidate = GameData.QUESTS.get(n.questId());
            return candidate != null && candidate.mainStoryQuest() && candidate.chainOwnerId.equals(q.chainOwnerId);
        }).findFirst().orElseThrow();
    }

    private static Npc npc(String name) { return GameData.NPCS.stream().filter(n -> n.name().equals(name)).findFirst().orElseThrow(); }
    private static DialogueLibrary.DialogueSession session(Npc npc, Quest q) { return DialogueLibrary.startSession(npc, q, "road", 0, new Random(1)); }
    private static DialogueLibrary.DialogueChoiceResult choose(DialogueLibrary.DialogueSession s, String label) {
        int i = s.optionLabels().indexOf(label);
        check(i >= 0, "Missing option " + label);
        return s.choose(i);
    }
    private static GameState fresh(GameConfig config) {
        GameState s = new GameState(config);
        s.chooseClass("Mage");
        while (s.mode == GameMode.STORY_INTRO) s.advanceStoryIntro();
        return s;
    }
    private static Object call(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(target, args);
    }
    private static void check(boolean pass, String message) {
        checks++;
        if (!pass) throw new AssertionError(message);
    }
}
