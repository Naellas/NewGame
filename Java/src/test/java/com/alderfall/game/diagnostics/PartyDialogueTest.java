package com.alderfall.game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

/** Pair coverage, player consent, battle attribution, and ordinary save round trips. */
public final class PartyDialogueTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static GameState fresh(GameConfig config) {
        GameState state = new GameState(config);
        state.chooseClass("Mage");
        while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
        return state;
    }
    private static void recruit(GameState state, String id) {
        state.recruitedIds.add(id);
        state.allies.add(GameData.RECRUITS.get(id).createActor());
    }
    private static void finishScene(GameState state, boolean accept) {
        GameState.TravelBanterPrompt prompt = state.partyDialogue.reply(state, 0);
        while (prompt != null) {
            boolean offer = prompt.options().getFirst().equals("Let's take this on together.");
            prompt = state.partyDialogue.reply(state, offer && !accept ? 1 : 0);
        }
    }
    public static void main(String[] args) throws Exception {
        GameConfig config = GameConfig.load(Path.of("").toAbsolutePath());
        check(PartyDialogue.ROAD_PAIRS.size() == 36, "Every pair of nine companions needs a scene");
        HashSet<String> keys = new HashSet<>();
        GameState state = fresh(config);
        for (PartyDialogue.Pair pair : PartyDialogue.ROAD_PAIRS) {
            check(keys.add(List.of(pair.first(), pair.second()).stream().sorted().toList().toString()), "Duplicate pair");
            state.allies.clear(); state.recruitedIds.clear(); state.villageAllies.clear();
            state.partyDialogue.reset();
            recruit(state, pair.first());
            check(state.partyDialogue.ambient(state) == null, "Absent companion spoke");
            recruit(state, pair.second());
            var first = state.partyDialogue.ambient(state);
            check(first != null && first.speaker().equals(PartyDialogue.name(pair.first())), "Wrong first speaker");
            var second = state.partyDialogue.reply(state, 0);
            check(second != null && second.speaker().equals(PartyDialogue.name(pair.second())), "Listener did not respond");
            state.villageAllies.add(PartyDialogue.name(pair.second()));
            check(state.partyDialogue.reply(state, 0) == null, "Departed speaker continued scene");
            state.villageAllies.clear(); state.partyDialogue.reset();
            state.partyDialogue.questEvent(state, "quest_start:" + pair.second() + "_chain_1");
            check(state.partyDialogue.startPending(state) != null, "Second companion's quest lacks a comment");
        }
        for (PartyDialogue.Pair pair : PartyDialogue.PAIRS) {
            state.allies.clear(); state.recruitedIds.clear(); state.partyDialogue.reset();
            recruit(state, pair.first()); recruit(state, pair.second());
            Quest quest = state.quests.get(pair.questId());
            state.partyDialogue.ambient(state); finishScene(state, false);
            check(!quest.accepted, "Road chat accepted a quest");
            check(state.partyDialogue.ambient(state) != null, "Offer did not follow road chat");
            finishScene(state, false);
            check(!quest.accepted, "Declined quest accepted");
            check(state.partyDialogue.ambient(state) == null, "Scene cooldown ignored");
            state.worldTick += 12001;
            state.partyDialogue.ambient(state); finishScene(state, true);
            check(quest.accepted, "Explicit offer acceptance failed");
            check(state.activeQuestObjectives().stream().noneMatch(o -> o.questId().equals(quest.id)), "Hunt spawned a false world objective");
            var record = GameState.class.getDeclaredMethod("recordQuestProgress", String.class);
            record.setAccessible(true); record.invoke(state, "Dungeon leader");
            check(quest.progress == 0, "Name-only kill credited shared hunt");
            Battle battle = new Battle(state.player, state.activeAllies(), GameData.MONSTERS.get("slime"), state.random);
            battle.finished = true; battle.victory = true;
            state.partyDialogue.victory(state, battle, false);
            check(!quest.completed, "Ordinary enemy credited as boss");
            Battle solo = new Battle(state.player, List.of(state.activeAllies().getFirst()), GameData.MONSTERS.get("slime"), state.random);
            solo.finished = true; solo.victory = true;
            state.partyDialogue.victory(state, solo, true);
            check(!quest.completed, "Missing participant credited");
            battle.fled = true;
            state.partyDialogue.victory(state, battle, true);
            check(!quest.completed, "Flee credited");
            battle.fled = false;
            int gold = state.player.gold;
            state.partyDialogue.victory(state, battle, true);
            check(quest.completed && state.player.gold == gold + quest.rewardGold, "Victory did not grant reward");
            check(state.companionMemories(pair.first()).stream().anyMatch(m -> m.category().equals("shared_quest")), "First participant forgot victory");
            check(state.companionMemories(pair.second()).stream().anyMatch(m -> m.category().equals("shared_quest")), "Second participant forgot victory");
            state.partyDialogue.victory(state, battle, true);
            check(state.player.gold == gold + quest.rewardGold, "Repeated victory duplicated reward");
        }
        Path saveRoot = Files.createTempDirectory("alderfall-party-test-");
        SaveSystem saves = new SaveSystem(saveRoot);
        Quest unfinished = state.quests.get(PartyDialogue.PAIRS.getFirst().questId());
        unfinished.completed = false; unfinished.progress = 0;
        saves.save(state, "Party dialogue regression");
        GameState loaded = fresh(config);
        check(saves.load(loaded), "Save failed to load");
        check(loaded.quests.get(unfinished.id).accepted && !loaded.quests.get(unfinished.id).completed, "Accepted hunt lost on load");
        for (PartyDialogue.Pair pair : PartyDialogue.PAIRS.subList(1, PartyDialogue.PAIRS.size())) {
            check(loaded.quests.get(pair.questId()).completed, "Completed hunt lost on load");
        }
        check(loaded.player.gold == state.player.gold, "Load replayed reward");
        check(loaded.companionMemories("calder").stream().anyMatch(m -> m.category().equals("shared_quest")), "Shared memory lost on load");
        // Exercise the real dungeon encounter and battle-result hook, without automating tactics.
        Quest integration = loaded.quests.get(PartyDialogue.PAIRS.getLast().questId());
        integration.completed = false; integration.progress = 0;
        var marker = loaded.world.adventureMarkers().getFirst();
        int floors = loaded.world.dungeonContext(marker.mapId()).floors();
        loaded.currentMapId = marker.mapId().replaceFirst("_\\d+$", "_" + floors);
        var boss = loaded.dungeonMonsterMotionsForMap(loaded.currentMapId).stream().filter(m -> m.boss()).findFirst().orElseThrow();
        var at = GameState.class.getDeclaredMethod("dungeonMonsterAt", String.class, int.class, int.class);
        at.setAccessible(true);
        Object runtime = at.invoke(loaded, loaded.currentMapId, boss.x(), boss.y());
        var start = GameState.class.getDeclaredMethod("startDungeonMonsterBattle", runtime.getClass());
        start.setAccessible(true); start.invoke(loaded, runtime);
        loaded.battle.finished = true; loaded.battle.victory = true;
        loaded.tickBattle();
        check(integration.completed, "Real dungeon victory hook missed the shared quest");
        int rewarded = loaded.player.gold;
        loaded.tickBattle();
        check(loaded.player.gold == rewarded, "Repeated battle-result processing duplicated rewards");
        loaded.leaveFinishedBattle();
        var update = GameState.class.getDeclaredMethod("updateTravelBanter");
        update.setAccessible(true); update.invoke(loaded);
        check(loaded.activeTravelBanter() != null && loaded.partyDialogue.owns(loaded.activeTravelBanter()), "Victory conversation not delivered to travel UI");
        String firstSpeaker = loaded.activeTravelBanter().speaker();
        loaded.replyToTravelBanter(0);
        check(loaded.activeTravelBanter() != null && !firstSpeaker.equals(loaded.activeTravelBanter().speaker()), "Travel UI did not advance to the partner");
        System.out.println("Party dialogue checks passed: " + checks);
    }
}
