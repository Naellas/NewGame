package com.alderfall.game;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Exercises the revised segments through world interactions, explicit exchanges, and saves. */
public final class CompanionQuestSegmentTest {
    private static int assertions;
    private static final StringBuilder transcript = new StringBuilder("# Companion segment playthroughs\n\n");

    public static void main(String[] args) throws Exception {
        GameConfig config = GameConfig.load(Path.of(""));
        Files.createDirectories(Path.of("out-story-refinement"));
        for (String id : List.of("seraphine_chain_2", "aria_chain_2", "lyra_chain_3", "rafiq_chain_2", "lyra_chain_6")) {
            playSegment(config, id);
        }
        testMigration(config);
        Files.writeString(Path.of("out-story-refinement/companion-segments.md"), transcript);
        System.out.println("Companion segment checks passed: " + assertions);
    }

    private static GameState fresh(GameConfig config) {
        GameState s = new GameState(config);
        s.chooseClass("Mage");
        while (s.mode == GameMode.STORY_INTRO) s.advanceStoryIntro();
        return s;
    }

    private static void openChapter(GameState s, Quest q) {
        Npc owner = owner(q);
        Quest earlier = s.quests.get(owner.questId());
        while (earlier != null && earlier != q) {
            earlier.completed = true;
            earlier = s.quests.get(earlier.nextQuestId);
        }
        s.activeNpc = owner;
        s.handleActiveNpcQuestAction();
        check(q.accepted, "Cannot accept " + q.id);
        check(s.recruitedIds.isEmpty(), "Test unexpectedly recruited a required companion");
    }

    @SuppressWarnings("unchecked")
    private static void playSegment(GameConfig config, String id) throws Exception {
        GameState s = fresh(config);
        Quest q = s.quests.get(id);
        openChapter(s, q);
        transcript.append("## ").append(q.title).append("\n\n").append(q.description).append("\n\n");
        SaveSystem saves = new SaveSystem(Files.createTempDirectory(Path.of("out-story-refinement"), "companion-save-"));
        boolean partialSaved = false;
        int actions = 0;
        String lastPrintedStage = "";
        while (!q.ready()) {
            check(actions++ < 40, "No progress in " + id + ": " + q.activeStage().id());
            String stage = q.activeStage().id();
            Quest.QuestStage authored = q.activeStage();
            if (!lastPrintedStage.equals(stage)) {
                transcript.append("**").append(authored.title()).append("**\n\n")
                        .append("Instruction: ").append(authored.progressDialog()).append("\n\n");
                lastPrintedStage = stage;
            }
            if (q.activeObjectiveKind().markedObjective()) {
                var objective = s.activeQuestObjectives().stream().filter(o -> o.questId().equals(id)).findFirst().orElseThrow();
                check(s.world.isPassable(objective.mapId(), objective.x(), objective.y()), "Blocked evidence " + stage);
                check(s.world.transitionAt(objective.mapId(), objective.x(), objective.y()) == null, "Evidence on transition " + stage);
                s.currentMapId = objective.mapId();
                s.playerX = objective.x();
                s.playerY = objective.y();
                s.mode = GameMode.EXPLORE;
                int previous = q.progress;
                s.interact();
                check(q.progress > previous || !stage.equals(q.activeStage().id()), "World interaction did not advance " + stage + ": " + s.status);
                Map<String, Integer> cargo = Map.copyOf(q.cargo);
                int after = q.progress;
                call(s, "interactQuestObjective", new Class<?>[]{GameState.QuestObjective.class}, objective);
                check(q.progress == after && cargo.equals(q.cargo), "Duplicate clue or cargo " + stage);
                if (!partialSaved && !q.cargo.isEmpty()) {
                    String expectedStage = q.activeStage().id();
                    int expectedProgress = q.progress;
                    saves.save(s, "Partial supplies");
                    GameState restored = fresh(config);
                    check(saves.load(restored, s.currentSaveId), "Partial save failed");
                    s = restored;
                    q = s.quests.get(id);
                    check(cargo.equals(q.cargo) && q.progress == expectedProgress && q.activeStage().id().equals(expectedStage), "Partial cargo did not round-trip");
                    partialSaved = true;
                }
            } else if (q.activeObjectiveKind().combatObjective()) {
                Object guard = ((List<?>) call(s, "questMonstersForQuest", new Class<?>[]{Quest.class}, q)).get(0);
                call(s, "startQuestMonsterBattle", new Class<?>[]{guard.getClass()}, guard);
                call(s, "recordQuestProgress", new Class<?>[]{String.class}, q.activeTarget());
                call(s, "clearDefeatedQuestMonster", new Class<?>[]{});
                s.mode = GameMode.EXPLORE;
            } else if (q.activeObjectiveKind() == Quest.ObjectiveKind.CHOICE) {
                s.activeNpc = owner(q);
                var session = (DialogueLibrary.DialogueSession) call(s, "startDialogueSessionFor", new Class<?>[]{Npc.class}, s.activeNpc);
                session.choose(session.optionLabels().indexOf("Discuss " + q.title + "."));
                var choice = session.optionPreviews().stream().filter(o -> o.effect().startsWith("quest:outcome:")).findFirst().orElseThrow();
                transcript.append("Player: ").append(choice.label()).append("\n\n");
                session.choose(session.optionLabels().indexOf(choice.label()));
                String expectedReply = QuestNarrative.clean(session.line());
                Npc companion = owner(q);
                s.currentMapId = companion.mapId();
                var p = s.npcPosition(companion);
                s.playerX = p.x();
                s.playerY = p.y();
                s.mode = GameMode.EXPLORE;
                check(s.talkToNpc(companion), "Could not start the decision conversation");
                String topic = "Discuss " + q.title + ".";
                int introductions = 0;
                while (!s.activeNpcDialogOptions().contains(topic) && introductions++ < 5) {
                    s.revealActiveDialogueLineInstantly();
                    s.selectDialogOption(0);
                }
                int topicIndex = s.activeNpcDialogOptions().indexOf(topic);
                check(topicIndex >= 0, "Decision topic absent from actual dialogue menu");
                s.revealActiveDialogueLineInstantly();
                s.selectDialogOption(topicIndex);
                int choiceIndex = s.activeNpcDialogOptions().indexOf(choice.label());
                check(choiceIndex >= 0, "Decision absent from actual dialogue menu");
                s.revealActiveDialogueLineInstantly();
                s.selectDialogOption(choiceIndex);
                check(s.activeNpcDialogLine().contains(expectedReply), "Quest refresh hid the companion's answer");
                transcript.append("Reply: ").append(expectedReply).append("\n\n");
            } else {
                List<Npc> contacts = (List<Npc>) call(s, "conversationObjectiveMarkerNpcs", new Class<?>[]{Quest.class}, q);
                check(contacts.size() == 1, "Missing named contact " + stage);
                Npc contact = contacts.get(0);
                check(s.world.isPassable(contact.mapId(), contact.x(), contact.y()), "Blocked contact " + stage);
                check(s.questForNpc(contact) == q, "Contact has no quest conversation " + stage);
                String effect = "quest:discuss:" + id + ":" + stage;
                s.activeNpc = owner(q);
                call(s, "applyDialogueEffect", new Class<?>[]{String.class}, effect);
                check(q.activeStage().id().equals(stage) && q.progress == 0, "Companion substituted for recipient " + stage);
                s.activeNpc = contact;
                if (!CompanionQuestContent.requiredCargo(stage).isEmpty()) {
                    Map<String, Integer> cargo = Map.copyOf(q.cargo);
                    check(CompanionQuestContent.canHandOver(q), "Normal route did not supply handover " + stage);
                    var session = (DialogueLibrary.DialogueSession) call(s, "startDialogueSessionFor", new Class<?>[]{Npc.class}, contact);
                    String key = CompanionQuestContent.requiredCargo(stage).keySet().iterator().next();
                    q.cargo.put(key, q.cargo.get(key) - 1);
                    check(!session.matches(q), "Missing cargo did not invalidate conversation");
                    call(s, "applyDialogueEffect", new Class<?>[]{String.class}, effect);
                    check(q.activeStage().id().equals(stage) && q.progress == 0, "Insufficient cargo was accepted");
                    q.cargo.clear();
                    q.cargo.putAll(cargo);
                    if (stage.equals("lyra_eda_treatment")) {
                        q.observedStages.remove("lyra_contaminated_rope");
                        call(s, "applyDialogueEffect", new Class<?>[]{String.class}, effect);
                        check(cargo.equals(q.cargo) && q.progress == 0, "Treatment consumed supplies before rope containment");
                        q.observedStages.add("lyra_contaminated_rope");
                    }
                }
                call(s, "applyDialogueEffect", new Class<?>[]{String.class}, effect);
                check(q.observedStages.contains(stage), "Explicit exchange failed " + stage);
                Map<String, Integer> afterCargo = Map.copyOf(q.cargo);
                int afterProgress = q.progress;
                call(s, "applyDialogueEffect", new Class<?>[]{String.class}, effect);
                check(afterCargo.equals(q.cargo) && q.progress == afterProgress, "Repeated exchange changed state " + stage);
                if (!CompanionQuestContent.requiredCargo(stage).isEmpty()) {
                    check(q.cargo.isEmpty(), "Delivery did not consume cargo " + stage);
                    saves.save(s, "Handover completed");
                    GameState restored = fresh(config);
                    check(saves.load(restored, s.currentSaveId), "Handover save failed");
                    s = restored;
                    q = s.quests.get(id);
                    check(q.cargo.isEmpty() && q.observedStages.contains(stage), "Handover replayed or cargo returned on load");
                }
            }
            if (q.observedStages.contains(stage)) transcript.append("Recorded result: ").append(authored.readyDialog()).append("\n\n");
        }
        if (q.activeObjectiveKind() == Quest.ObjectiveKind.TALK) {
            int gold = s.player.gold;
            s.handleActiveNpcQuestAction();
            check(!q.completed && s.player.gold == gold, "Witness awarded the companion's final reward");
        }
        s.activeNpc = owner(q);
        int before = s.player.gold;
        s.handleActiveNpcQuestAction();
        check(q.completed && s.player.gold == before + q.rewardGold, "Final reward incorrect " + id);
        int rewarded = s.player.gold;
        s.handleActiveNpcQuestAction();
        check(s.player.gold == rewarded, "Repeated completion gave another reward " + id);
        saves.save(s, "Completed segment");
        GameState restored = fresh(config);
        check(saves.load(restored, s.currentSaveId), "Completed save failed " + id);
        Quest loaded = restored.quests.get(id);
        check(loaded.completed && loaded.observedStages.equals(q.observedStages) && loaded.cargo.equals(q.cargo), "Completion did not persist " + id);
        String map = q.stages.stream().filter(st -> st.objectiveKind() == Quest.ObjectiveKind.TALK || st.objectiveKind() == Quest.ObjectiveKind.DELIVER)
                .findFirst().orElseThrow().objectiveMapId();
        var aftermath = CompanionQuestContent.aftermath(restored.quests, restored.world, map);
        check(!aftermath.isEmpty(), "No persistent aftermath for " + id);
        List<Npc> visible = (List<Npc>) call(restored, "npcsForMap", new Class<?>[]{String.class}, map);
        for (Npc npc : aftermath) {
            check(visible.stream().filter(n -> n.name().equals(npc.name())).count() == 1, "Aftermath contact missing or duplicated in world");
            String expectedSprite = q.stages.stream().filter(st -> st.target().equals(npc.name())).findFirst().orElseThrow().objectiveAsset();
            check(npc.sprite().equals(expectedSprite), "Aftermath changed the witness's appearance");
        }
        transcript.append("After return: ").append(q.activeCompleteDialog()).append("\n\n")
                .append("After reloading: ").append(aftermath.get(0).dialog().get(0)).append("\n\n");
    }

    private static void testMigration(GameConfig config) throws Exception {
        GameState s = fresh(config);
        SaveSystem saves = new SaveSystem(Path.of("out-story-refinement"));
        String note = (String) call(saves, "readQuests", new Class<?>[]{String.class, GameState.class},
                "rafiq_chain_2:true:false:1:3,seraphine_chain_2:true:true:1:2", s);
        Quest q = s.quests.get("rafiq_chain_2");
        check(note.contains(q.title) && q.stageIndex == 0 && q.cargo.isEmpty(), "Migration invented delivered supplies");
        check(s.quests.get("seraphine_chain_2").completed, "Migration revoked completed refuge quest");
        call(saves, "readQuestBranchOutcomes", new Class<?>[]{String.class, GameState.class}, "rafiq_water_witnesses:protect", s);
        openChapter(s, q);
        q.stageIndex = q.stages.size() - 1;
        s.activeNpc = owner(q);
        String effect = "quest:outcome:" + q.id + ":" + q.activeStage().id() + ":";
        call(s, "applyDialogueEffect", new Class<?>[]{String.class}, effect + "truth");
        check(q.progress == 0 && "protect".equals(s.questBranchOutcome(q)), "Migration allowed replacing an earlier decision");
        var session = (DialogueLibrary.DialogueSession) call(s, "startDialogueSessionFor", new Class<?>[]{Npc.class}, s.activeNpc);
        session.choose(session.optionLabels().indexOf("Discuss " + q.title + "."));
        check(session.optionLabels().contains("Keep our recorded decision."), "No route through retained choice");
        call(s, "applyDialogueEffect", new Class<?>[]{String.class}, effect + "protect");
        check(q.ready() && "protect".equals(s.questBranchOutcome(q)), "Retained decision stranded the revised quest");
    }

    private static Npc owner(Quest q) {
        return GameData.NPCS.stream().filter(n -> q.chainOwnerId.equals(n.recruitId())).findFirst().orElseThrow();
    }

    private static Object call(Object object, String method, Class<?>[] types, Object... args) throws Exception {
        Method m = object.getClass().getDeclaredMethod(method, types);
        m.setAccessible(true);
        return m.invoke(object, args);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new IllegalStateException(message);
    }
}
