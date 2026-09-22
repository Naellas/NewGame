package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

/** Behavior checks for grounded quest conversations, completion, and migration. */
public final class QuestNarrativeTest {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        GameConfig config = GameConfig.load(root);
        GameState state = fresh(config);
        testEvidence(state);
        testPlacement(state);
        testExplicitChoices(state);
        testConversationCredit(state);
        testKingdomCommitments(config);
        testWitnesses(config);
        testAllCompanionChoices(config);
        testCatalogDialogues();
        testSaveRoundTrip(config, root);
        testLegacyMigration(config, root);
        System.out.println("Quest narrative checks passed: " + assertions);
    }

    private static GameState fresh(GameConfig config) {
        GameState state = new GameState(config);
        state.chooseClass("Mage");
        while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
        return state;
    }

    private static void testEvidence(GameState state) throws Exception {
        Quest q = state.quests.get("ms_wake_ashes");
        q.accepted = true;
        check(!QuestNarrative.findings(q).contains("damage is concentrated"), "Future finding leaked before inspection");
        var first = state.activeQuestObjectives().stream().filter(o -> o.questId().equals(q.id)).findFirst().orElseThrow();
        call(state, "interactQuestObjective", new Class<?>[]{GameState.QuestObjective.class}, first);
        check(q.stageIndex == 1, "First shrine inspection did not advance");
        check(q.observedStages.contains("shrine_damage"), "First finding was not remembered");
        check(!QuestNarrative.findings(q).contains("loose fragment"), "Second shrine finding leaked early");
        call(state, "interactQuestObjective", new Class<?>[]{GameState.QuestObjective.class}, first);
        check(q.progress == 0, "Stale shrine objective advanced the next stage");
        var second = state.activeQuestObjectives().stream().filter(o -> o.questId().equals(q.id)).findFirst().orElseThrow();
        call(state, "interactQuestObjective", new Class<?>[]{GameState.QuestObjective.class}, second);
        check(q.ready() && q.observedStages.contains("shrine_socket"), "Second finding did not complete investigation");
        call(state, "interactQuestObjective", new Class<?>[]{GameState.QuestObjective.class}, second);
        check(q.progress == 1, "Repeated evidence added progress");
    }

    private static void testPlacement(GameState state) {
        for (String map : List.of("story_road_shrine", "story_oath_vault", "city_archive", "village_snowrest")) {
            for (int i = 0; i < 12; i++) {
                TilePoint point = state.world.storyObjectivePoint(map, i);
                check(state.world.isPassable(map, point.x(), point.y()), "Blocked story anchor: " + map);
                check(state.world.transitionAt(map, point.x(), point.y()) == null, "Evidence placed on a transition: " + map);
            }
        }
        boolean shrine = false;
        for (int y = 0; y < state.world.height(WorldMap.PLAYER_VILLAGE_ID); y++) {
            for (int x = 0; x < state.world.width(WorldMap.PLAYER_VILLAGE_ID); x++) {
                var t = state.world.transitionAt(WorldMap.PLAYER_VILLAGE_ID, x, y);
                if (t != null && t.targetMapId().equals("story_road_shrine")) {
                    shrine = true;
                    check(state.world.isPassable(WorldMap.PLAYER_VILLAGE_ID, x, y), "Shrine entrance is blocked");
                }
            }
        }
        check(shrine, "Oathstead has no shrine entrance");
    }

    private static void testKingdomCommitments(GameConfig config) throws Exception {
        GameState state = fresh(config);
        Quest q = state.quests.get("ms_kingdoms_answer");
        state.quests.get("ms_camp_defending").completed = true;
        for (String stone : GameData.MAGIC_STONE_KEYS) if (!stone.equals("stone_dawn")) state.player.addItem(stone, 1);
        Npc maelis = GameData.NPCS.stream().filter(n -> "ms_wake_ashes".equals(n.questId())).findFirst().orElseThrow();
        state.activeNpc = maelis;
        check((boolean) call(state, "canAcceptActiveStoryQuest", new Class<?>[]{Quest.class}, q), "Required campaign content is gated by personal approval");
        q.accepted = true;
        for (int i = 0; i < q.stages.size(); i++) {
            String stageId = q.activeStage().id();
            Npc contact = GameData.NPCS.stream().filter(n -> n.name().equals(q.activeTargetNpcId())).findFirst().orElseThrow();
            state.activeNpc = companion("vesper");
            String effect = "quest:discuss:" + q.id + ":" + stageId;
            call(state, "applyDialogueEffect", new Class<?>[]{String.class}, effect);
            check(!q.observedStages.contains(stageId), "An absent representative's commitment was invented");
            state.activeNpc = contact;
            check(state.questForNpc(contact) == q, "Representative cannot discuss the coalition quest");
            call(state, "applyDialogueEffect", new Class<?>[]{String.class}, effect);
            check(q.observedStages.contains(stageId), "Representative's commitment was not recorded");
            int nextStage = q.stageIndex;
            int nextProgress = q.progress;
            call(state, "applyDialogueEffect", new Class<?>[]{String.class}, effect);
            check(q.stageIndex == nextStage && q.progress == nextProgress, "Stale commitment advanced another speaker");
        }
        check(q.ready() && q.observedStages.size() == 5, "Five actual commitments did not complete the objective");
    }

    @SuppressWarnings("unchecked")
    private static void testWitnesses(GameConfig config) throws Exception {
        GameState state = fresh(config);
        for (int i = 1; i < 6; i++) state.quests.get("aria_chain_" + i).completed = true;
        Quest q = state.quests.get("aria_chain_6");
        q.accepted = true;
        for (int i = 0; i < q.stages.size(); i++) if (q.stages.get(i).objectiveKind() == Quest.ObjectiveKind.ASK_AROUND) q.stageIndex = i;
        String stage = q.activeStage().id();
        int needed = q.activeNeeded();
        java.util.Set<String> witnesses = new java.util.HashSet<>();
        for (int i = 0; i < needed; i++) {
            List<Npc> contacts = (List<Npc>) call(state, "conversationObjectiveMarkerNpcs", new Class<?>[]{Quest.class}, q);
            check(!contacts.isEmpty(), "Witness investigation ran out of eligible contacts");
            Npc npc = contacts.get(0);
            check(witnesses.add(npc.name()), "Previously interviewed witness offered again");
            check(state.questForNpc(npc) == q, "Witness has no quest conversation");
            state.activeNpc = npc;
            call(state, "applyDialogueEffect", new Class<?>[]{String.class}, "quest:discuss:" + q.id + ":" + stage);
            if (i < needed - 1) check(!q.observedStages.contains(stage), "Partial testimony became a completed finding");
        }
        check(q.observedStages.contains(stage), "Distinct testimony did not complete investigation");
    }

    private static void testAllCompanionChoices(GameConfig config) throws Exception {
        GameState state = fresh(config);
        int choices = 0;
        for (Quest q : state.quests.values()) {
            if (!q.companionQuest()) continue;
            Npc npc = companion(q.chainOwnerId);
            for (int i = 0; i < q.stages.size(); i++) {
                if (q.stages.get(i).objectiveKind() != Quest.ObjectiveKind.CHOICE) continue;
                for (Quest previous : state.quests.values()) if (previous.companionQuest()
                        && previous.chainOwnerId.equals(q.chainOwnerId)) previous.completed = true;
                q.completed = false;
                q.accepted = true;
                q.stageIndex = i;
                q.progress = 0;
                state.activeNpc = npc;
                var session = DialogueLibrary.startSession(npc, q, "forest", 0, new Random(1));
                choose(session, "Discuss " + q.title);
                String effect = session.optionPreviews().stream().filter(o -> o.effect().startsWith("quest:outcome:"))
                        .findFirst().orElseThrow().effect();
                String key = q.outcomeKey();
                call(state, "applyDialogueEffect", new Class<?>[]{String.class}, effect);
                check(state.questBranchOutcomes.containsKey(key), "Companion decision failed: " + q.id);
                choices++;
            }
        }
        check(choices >= 50, "Companion choice coverage unexpectedly shrank");
    }

    private static void testExplicitChoices(GameState state) throws Exception {
        for (int i = 1; i < 8; i++) state.quests.get("vesper_chain_" + i).completed = true;
        Quest q = state.quests.get("vesper_chain_8");
        Npc npc = companion("vesper");
        q.accepted = true;
        for (int i = 0; i < q.stages.size(); i++) if (q.stages.get(i).objectiveKind() == Quest.ObjectiveKind.CHOICE) q.stageIndex = i;
        check(q.activeObjectiveKind() == Quest.ObjectiveKind.CHOICE, "Missing Vesper decision stage");
        state.activeNpc = npc;
        state.mode = GameMode.DIALOG;
        call(state, "recordConversationQuestObjectives", new Class<?>[]{Npc.class}, npc);
        check(q.progress == 0 && !state.questBranchOutcomes.containsKey(q.outcomeKey()), "Greeting made a decision");
        DialogueLibrary.DialogueSession session = DialogueLibrary.startSession(npc, q, "snow", 100, new Random(1));
        choose(session, "Discuss " + q.title);
        String effect = session.optionPreviews().stream().filter(o -> o.effect().startsWith("quest:outcome:"))
                .findFirst().orElseThrow().effect();
        String key = q.outcomeKey();
        call(state, "applyDialogueEffect", new Class<?>[]{String.class}, effect.replace(q.activeStage().id(), "stale_stage"));
        check(q.progress == 0, "Stale decision stage was accepted");
        call(state, "applyDialogueEffect", new Class<?>[]{String.class}, effect);
        check(state.questBranchOutcomes.containsKey(key), "Explicit choice was not recorded");
        String outcome = state.questBranchOutcomes.get(key);
        call(state, "applyDialogueEffect", new Class<?>[]{String.class}, effect);
        check(outcome.equals(state.questBranchOutcomes.get(key)), "Repeated choice changed the outcome");
        check(!session.matches(q), "Old conversation remained valid after decision");
        for (int i = 1; i < 8; i++) state.quests.get("vesper_chain_" + i).completed = false;
    }

    private static void testConversationCredit(GameState state) throws Exception {
        Quest q = state.quests.get("vesper_chain_2");
        q.accepted = true;
        q.stageIndex = 2;
        q.progress = 0;
        Npc npc = GameData.NPCS.stream().filter(n -> n.name().equals("Goatkeeper Una")).findFirst().orElseThrow();
        state.activeNpc = npc;
        check(q.activeObjectiveKind().conversationObjective(), "Vesper report missing");
        // The public greeting must not complete a report, even at the correct speaker.
        state.currentMapId = npc.mapId();
        var position = state.npcPosition(npc);
        state.playerX = position.x();
        state.playerY = position.y();
        state.mode = GameMode.EXPLORE;
        state.talkToNpc(npc);
        check(q.progress == 0, "Opening dialogue completed the report");
        call(state, "applyDialogueEffect", new Class<?>[]{String.class}, "quest:discuss:" + q.id + ":" + q.activeStage().id());
        check(q.stageIndex == 3 && q.observedStages.contains("vesper_chain_2_patient"), "Explicit testimony did not complete");
        Quest stolen = state.quests.get("ms_stolen_index");
        stolen.accepted = true;
        call(state, "recordQuestProgress", new Class<?>[]{String.class}, "Bandit Cutthroat");
        check(stolen.progress == 0, "Unrelated cutthroat credited to Crowhook cache guards");
        List<?> guards = (List<?>) call(state, "questMonstersForQuest", new Class<?>[]{Quest.class}, stolen);
        Object guard = guards.get(0);
        call(state, "startQuestMonsterBattle", new Class<?>[]{guard.getClass()}, guard);
        call(state, "recordQuestProgress", new Class<?>[]{String.class}, "Bandit Cutthroat");
        check(stolen.progress == 1, "Marked Crowhook guard did not count");
        var destination = (WorldMap.AdventureMarker) call(state, "dungeonMarkerFor", new Class<?>[]{Quest.class}, stolen);
        check(destination != null, "Crowhook has no dungeon destination");
        state.currentMapId = destination.mapId();
        List<?> enemies = (List<?>) call(state, "dungeonMonstersForMap", new Class<?>[]{String.class}, destination.mapId());
        Object enemy = enemies.get(0);
        call(state, "startDungeonMonsterBattle", new Class<?>[]{enemy.getClass()}, enemy);
        call(state, "recordQuestProgress", new Class<?>[]{String.class}, "Bandit Cutthroat");
        check(stolen.progress == 2, "Corresponding dungeon encounter did not count");
    }

    private static void testCatalogDialogues() {
        for (Quest template : GameData.QUESTS.values()) {
            Npc npc = template.companionQuest() ? companion(template.chainOwnerId)
                    : GameData.NPCS.stream().filter(n -> template.id.equals(n.questId())).findFirst().orElse(null);
            if (npc == null) continue;
            for (int i = 0; i < template.stages.size(); i++) {
                Quest q = template.copy();
                q.accepted = true;
                q.stageIndex = i;
                var session = DialogueLibrary.startSession(npc, q, "forest", 0, new Random(1));
                choose(session, "Discuss " + q.title);
                choose(session, "What have we actually established?");
                check(session.line().contains("not recorded a finding"), "Unknown stage facts leaked: " + q.id);
                check(session.optionPreviews().stream().allMatch(o -> o.relationshipDelta() == 0), "Evidence browsing farms approval");
                q.progress++;
                check(!session.matches(q), "Session did not detect progress change");
            }
        }
    }

    private static void testSaveRoundTrip(GameConfig config, Path root) throws Exception {
        Path dir = Files.createTempDirectory(root.resolve("out-story-refinement"), "quest-save-");
        SaveSystem saves = new SaveSystem(dir);
        GameState state = fresh(config);
        Quest q = state.quests.get("ms_wake_ashes");
        q.accepted = true;
        q.recordVisit(q.activeTarget());
        q.advanceStage();
        state.questBranchOutcomes.put("test_recorded_decision", "protect");
        saves.save(state, "Narrative regression");
        GameState loaded = fresh(config);
        check(saves.load(loaded, state.currentSaveId), "Save failed to load");
        Quest restored = loaded.quests.get(q.id);
        check(restored.activeStage().id().equals("shrine_socket"), "Stable stage did not round-trip");
        check(restored.observedStages.equals(q.observedStages), "Evidence did not round-trip");
        check("protect".equals(loaded.questBranchOutcomes.get("test_recorded_decision")), "Decision did not round-trip");
        Npc maelis = GameData.NPCS.stream().filter(n -> "ms_wake_ashes".equals(n.questId())).findFirst().orElseThrow();
        loaded.activeNpc = maelis;
        restored.recordVisit(restored.activeTarget());
        int gold = loaded.player.gold;
        loaded.handleActiveNpcQuestAction();
        int awarded = loaded.player.gold;
        check(awarded > gold, "Reward not granted");
        loaded.handleActiveNpcQuestAction();
        check(loaded.player.gold == awarded, "Repeated turn-in duplicated reward");
        saves.save(loaded, "Completed narrative regression");
        GameState reloaded = fresh(config);
        check(saves.load(reloaded, loaded.currentSaveId), "Completed save failed to load");
        reloaded.activeNpc = maelis;
        reloaded.handleActiveNpcQuestAction();
        check(reloaded.player.gold == awarded, "Reload allowed a duplicate reward");
        reloaded.world.setPlayerVillageStage(3);
        testPlacement(reloaded);
    }

    private static void testLegacyMigration(GameConfig config, Path root) throws Exception {
        GameState state = fresh(config);
        SaveSystem saves = new SaveSystem(root.resolve("out-story-refinement"));
        Method read = SaveSystem.class.getDeclaredMethod("readQuests", String.class, GameState.class);
        read.setAccessible(true);
        String migration = (String) read.invoke(saves, "ms_wake_ashes:true:false:1:0,ms_first_socket:true:true:2:0,vesper_chain_1:true:false:0:1", state);
        check(migration.contains(state.quests.get("ms_wake_ashes").title), "Revised investigation reset was not explained");
        check(state.quests.get("ms_wake_ashes").progress == 0, "Legacy generic count invented a specific clue");
        check(state.quests.get("ms_first_socket").completed, "Completed legacy quest was revoked");
        check(state.quests.get("vesper_chain_1").stageIndex == 1, "Compatible companion stage lost progress");
        check(!state.quests.get("vesper_chain_1").observedStages.isEmpty(), "Compatible legacy observations were lost");
    }

    private static Npc companion(String id) {
        return GameData.NPCS.stream().filter(n -> id.equals(n.recruitId())).findFirst().orElseThrow();
    }

    private static void choose(DialogueLibrary.DialogueSession session, String label) {
        int index = session.optionLabels().indexOf(label);
        if (index < 0) index = session.optionLabels().indexOf(label + ".");
        check(index >= 0, "Missing dialogue option: " + label);
        session.choose(index);
    }

    private static Object call(Object target, String method, Class<?>[] types, Object... values) throws Exception {
        Method reflected = target.getClass().getDeclaredMethod(method, types);
        reflected.setAccessible(true);
        return reflected.invoke(target, values);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new IllegalStateException(message);
    }
}
