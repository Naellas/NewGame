package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.Files;
import java.nio.file.Path;

/** Exercises the actual interaction, dialogue action, cargo and save paths. */
public final class FurnitureQuestTest {
    public static void main(String[] args) throws Exception {
        GameConfig config = GameConfig.load(Path.of(""));
        for (int seed : new int[]{0, 42, -1024}) {
            WorldMap world = new WorldMap(seed);
            Npc keeper = FurnitureQuestContent.keeper(world);
            require(keeper != null, "Oakhaven inn missing at seed " + seed);
            for (String stage : new String[]{"provisions_counter", "provisions_ledger", "provisions_recover", "provisions_return"}) {
                var prop = FurnitureQuestContent.furniture(world, keeper.mapId(), FurnitureQuestContent.binding(stage));
                require(prop != null, "Missing furniture " + stage);
            }
        }
        GameState state = fresh(config);
        Npc keeper = FurnitureQuestContent.keeper(state.world);
        Quest quest = state.quests.get(FurnitureQuestContent.ID);
        require(state.questForNpc(keeper) == quest, "Keeper does not offer quest");
        state.currentMapId = keeper.mapId();
        state.activeNpc = keeper;
        state.handleActiveNpcQuestAction();
        require(quest.accepted, "Quest acceptance failed");
        state.closeOverlay();
        var first = objective(state);
        // A stale/wrong-map interaction must not produce evidence or cargo.
        var method = GameState.class.getDeclaredMethod("interactQuestObjective", GameState.QuestObjective.class);
        method.setAccessible(true);
        state.currentMapId = WorldMap.OVERWORLD_ID;
        method.invoke(state, first);
        require(quest.stageIndex == 0, "Wrong-map interaction accepted");
        use(state);
        require(quest.activeStage().id().equals("provisions_ledger"), "Counter did not advance");
        method.invoke(state, first);
        require(quest.activeStage().id().equals("provisions_ledger"), "Stale interaction advanced");
        var pantry = FurnitureQuestContent.furniture(state.world, keeper.mapId(), FurnitureQuestContent.binding("provisions_ledger"));
        require(!FurnitureQuestContent.decoration(state, pantry).isEmpty(), "Ledger detail absent");
        use(state);
        require(quest.activeStage().id().equals("provisions_explain"), "Ledger did not advance");
        require(FurnitureQuestContent.decoration(state, pantry).isEmpty(), "Read ledger still displayed");
        var talk = objective(state);
        require(talk.mapId().equals(keeper.mapId()) && talk.markerOnly(), "Testimony moved away from keeper");
        require(quest.cargo.isEmpty(), "Supplies granted before permission");
        state.activeNpc = keeper;
        state.handleActiveNpcQuestAction();
        require(quest.activeStage().id().equals("provisions_recover"), "Explicit testimony failed");
        state.closeOverlay();
        use(state);
        require(quest.cargo.getOrDefault("senn_provisions", 0) == 1, "Parcel missing");
        Path directory = Files.createTempDirectory(Path.of("temp/furniture-quests"), "save-");
        SaveSystem saves = new SaveSystem(directory);
        saves.save(state, "Furniture quest regression");
        GameState restored = fresh(config);
        require(saves.load(restored, state.currentSaveId), "Save load failed");
        quest = restored.quests.get(FurnitureQuestContent.ID);
        require(quest.activeStage().id().equals("provisions_return") && quest.cargo.getOrDefault("senn_provisions", 0) == 1,
                "Stage/cargo not restored");
        quest.cargo.clear();
        use(restored);
        require(!quest.ready(), "Delivery accepted without parcel");
        quest.cargo.put("senn_provisions", 1);
        use(restored);
        require(quest.ready() && quest.cargo.isEmpty(), "Delivery did not consume parcel");
        var counter = FurnitureQuestContent.furniture(restored.world, restored.currentMapId, FurnitureQuestContent.binding("provisions_return"));
        require(!FurnitureQuestContent.decoration(restored, counter).isEmpty(), "Delivered parcel invisible");
        keeper = FurnitureQuestContent.keeper(restored.world);
        restored.activeNpc = keeper;
        int before = restored.player.gold;
        restored.handleActiveNpcQuestAction();
        require(quest.completed && restored.player.gold == before + quest.rewardGold, "Reward missing");
        restored.handleActiveNpcQuestAction();
        require(restored.player.gold == before + quest.rewardGold, "Repeated reward");
        saves.save(restored, "Completed furniture quest");
        GameState completed = fresh(config);
        require(saves.load(completed, restored.currentSaveId), "Completed save failed");
        require(completed.quests.get(FurnitureQuestContent.ID).completed, "Completion lost");
        // Interior saves intentionally resume outside; re-enter the regenerated room.
        completed.currentMapId = FurnitureQuestContent.keeper(completed.world).mapId();
        var restoredCounter = FurnitureQuestContent.furniture(completed.world, completed.currentMapId,
                FurnitureQuestContent.binding("provisions_return"));
        require(!FurnitureQuestContent.decoration(completed, restoredCounter).isEmpty(), "Delivered parcel lost on reload");
        System.out.println("Furniture quest passed: three world seeds, interactions, testimony, cargo, save/load, visuals, reward replay.");
    }
    private static GameState fresh(GameConfig config) {
        GameState state = new GameState(config);
        state.chooseClass("Mage");
        while (state.mode == GameMode.STORY_INTRO) state.advanceStoryIntro();
        return state;
    }
    private static GameState.QuestObjective objective(GameState state) {
        return state.activeQuestObjectives().stream().filter(o -> o.questId().equals(FurnitureQuestContent.ID)).findFirst().orElseThrow();
    }
    private static void use(GameState state) {
        var objective = objective(state);
        state.currentMapId = objective.mapId();
        int[] footprint = WorldMap.interiorVisualFootprint(objective.asset());
        // Deliberately approach the far end of wide counters, not only their origin tile.
        for (int y = objective.y() + footprint[1]; y >= objective.y() - 1; y--)
            for (int x = objective.x() + footprint[0]; x >= objective.x() - 1; x--) {
                if (state.questObjectiveDistance(objective, x, y) != 1
                        || !state.world.isPassable(state.currentMapId, x, y)) continue;
                state.playerX = x; state.playerY = y;
                state.interact();
                return;
            }
        throw new AssertionError("No accessible front edge: " + objective);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
