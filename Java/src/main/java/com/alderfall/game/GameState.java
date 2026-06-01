package com.alderfall.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class GameState {
    public static final int TICKS_PER_GAME_DAY = 7200;
    private static final int WEATHER_BLOCK_TICKS = 1200;
    private static final int VILLAGE_PRODUCTION_TICKS = 900;
    public static final List<String> STORY_INTRO = List.of(
            "In the elder age, five crowns swore the Starforge Oath over the Heartstone of Alderfall. So long as the oath held, rivers ran clean, harvests ripened under gentle moons, and the old roads carried songs between every gate.",
            "Now the Heartstone has cracked. Its echo wakes barrow kings, stirs raiders into armies, and turns border beacons from warning fires into accusations. Riverside blames Highwall. Belltower closes its marsh roads. Sanctum musters beneath desert banners. The Crownlands count their dead.",
            "Oakhaven, small and stubborn beneath the old oaks, is the first village to refuse despair. In its well a silver ember burns without fuel, naming you as a bearer of the lost oath and marking your road across every divided kingdom.",
            "Your quest is not only to survive. You must gather the echoes hidden in vault, fen, frost, and sunsteppe, win allies from rival thrones, and decide whether Alderfall will be reforged as one realm or remade into something stranger."
    );
    public final GameConfig config;
    public final WorldMap world;
    public final Random random = new Random();
    public final Map<String, Quest> quests = new LinkedHashMap<>();
    public final List<Actor> allies = new ArrayList<>();
    public final List<String> recruitedIds = new ArrayList<>();
    public final Set<String> villageAllies = new HashSet<>();
    public final Map<String, String> villageWorkerRoles = new LinkedHashMap<>();
    public final Map<String, String> villageBuildingAssignments = new LinkedHashMap<>();
    public final Map<String, Integer> villageStorage = new LinkedHashMap<>();
    public final Set<String> harvestedQuestResources = new HashSet<>();
    public final Map<String, Integer> npcRelationships = new LinkedHashMap<>();
    public final CraftingSystem crafting = new CraftingSystem();
    public GameMode mode = GameMode.MAIN_MENU;
    public GameMode pauseReturnMode = GameMode.EXPLORE;
    public GameMode settingsReturnMode = GameMode.MAIN_MENU;
    public GameMode saveMenuReturnMode = GameMode.MAIN_MENU;
    public boolean saveMenuCanSave;
    public Actor player = GameData.createPlayer("Knight");
    public String pendingPlayerName = "Arin";
    public String pendingSaveName = "";
    public String currentSaveId = "";
    public String currentMapId = WorldMap.OVERWORLD_ID;
    public int playerX = WorldMap.START_POSITION.x();
    public int playerY = WorldMap.START_POSITION.y();
    public int partyScreenIndex;
    public int villageTab;
    public int settlementBoardTab;
    public boolean villageEditMode;
    public String villageEditAction = "place";
    public String selectedVillageBuildingStyle = "";
    public String selectedVillageAsset = "city_prop_flower_pot";
    public String selectedVillagePropCategory = "Decor";
    public char selectedVillageTile = 'A';
    public String selectedInteriorAsset = "interior_round_table";
    public TilePoint pendingVillageMoveSource;
    public String pendingVillageMoveMapId;
    public Battle battle;
    public Npc activeNpc;
    public Shop activeShop;
    public CityBuilding activeVillageBuilding;
    public int dialogIndex;
    private DialogueLibrary.DialogueSession activeDialogueSession;
    private List<VillageRecruitOption> settlementRecruitOptions = List.of();
    public int zoom = 100;
    public int worldTick;
    public int storyPage;
    public boolean worldMapKingdoms;
    public String status = "Choose New Adventure or load an existing save.";
    private final Map<Npc, NpcRuntime> npcRuntime = new LinkedHashMap<>();
    private final Map<String, List<DungeonMonsterRuntime>> dungeonMonsterRuntime = new LinkedHashMap<>();
    private final Set<String> defeatedDungeonMonsters = new HashSet<>();
    private DungeonMonsterRuntime activeDungeonMonster;

    public GameState(GameConfig config) {
        this.config = config;
        this.world = new WorldMap(0);
        for (Map.Entry<String, Quest> entry : GameData.QUESTS.entrySet()) {
            quests.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public void openMainMenu() {
        activeNpc = null;
        activeShop = null;
        activeVillageBuilding = null;
        battle = null;
        activeDungeonMonster = null;
        activeDialogueSession = null;
        dialogIndex = 0;
        mode = GameMode.MAIN_MENU;
        status = "Choose New Adventure or load an existing save.";
    }

    public void openClassSelect() {
        activeNpc = null;
        activeShop = null;
        activeVillageBuilding = null;
        battle = null;
        activeDungeonMonster = null;
        activeDialogueSession = null;
        dialogIndex = 0;
        mode = GameMode.CLASS_SELECT;
        status = "Choose a class.";
    }

    public void setPendingPlayerName(String name) {
        pendingPlayerName = cleanName(name, 24);
    }

    public void setPendingSaveName(String name) {
        pendingSaveName = cleanName(name, 32);
    }

    public String defaultSaveName() {
        return world.label(currentMapId);
    }

    public void chooseClass(String className) {
        setPendingPlayerName(pendingPlayerName);
        if (pendingPlayerName.isBlank()) {
            pendingPlayerName = "Arin";
        }
        player = GameData.createPlayer(className, pendingPlayerName);
        syncSkillAbilities();
        currentSaveId = SaveSystem.saveIdFor(player.name);
        currentMapId = WorldMap.PLAYER_VILLAGE_ID;
        playerX = 14;
        playerY = 17;
        worldTick = 0;
        world.clearPlayerVillageCustomizations();
        resetNpcRuntime();
        resetDungeonMonsterRuntime();
        allies.clear();
        recruitedIds.clear();
        villageAllies.clear();
        villageWorkerRoles.clear();
        villageBuildingAssignments.clear();
        villageStorage.clear();
        partyScreenIndex = 0;
        resetVillageInterface();
        harvestedQuestResources.clear();
        npcRelationships.clear();
        battle = null;
        activeDungeonMonster = null;
        storyPage = 0;
        mode = GameMode.STORY_INTRO;
        status = className + " hears the first echo of Alderfall.";
    }

    public String storyIntroPage() {
        return STORY_INTRO.get(Math.max(0, Math.min(storyPage, STORY_INTRO.size() - 1)));
    }

    public void advanceStoryIntro() {
        if (mode != GameMode.STORY_INTRO) {
            return;
        }
        if (storyPage < STORY_INTRO.size() - 1) {
            storyPage++;
            status = "The tale continues.";
            return;
        }
        mode = GameMode.EXPLORE;
        status = player.className + " begins at Oathstead Camp. The silver ember points beyond the first fire.";
    }

    public int dayNumber() {
        return Math.max(1, Math.floorDiv(worldTick, TICKS_PER_GAME_DAY) + 1);
    }

    public int timeOfDayMinutes() {
        return Math.floorMod(worldTick, TICKS_PER_GAME_DAY) * 1440 / TICKS_PER_GAME_DAY;
    }

    public boolean isDaytime() {
        int minutes = timeOfDayMinutes();
        return minutes >= 360 && minutes < 1200;
    }

    public String timeLabel() {
        int minutes = timeOfDayMinutes();
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    public String dayPhaseLabel() {
        int hour = timeOfDayMinutes() / 60;
        if (hour >= 5 && hour < 7) {
            return "Dawn";
        }
        if (hour >= 7 && hour < 12) {
            return "Morning";
        }
        if (hour >= 12 && hour < 17) {
            return "Afternoon";
        }
        if (hour >= 17 && hour < 20) {
            return "Dusk";
        }
        return "Night";
    }

    public double daylightLevel() {
        int minutes = timeOfDayMinutes();
        double sun = Math.sin(Math.PI * (minutes - 360) / 720.0);
        if (sun <= 0.0) {
            return 0.18;
        }
        return 0.18 + sun * 0.82;
    }

    public char currentBiomeTile() {
        return biomeTileAt(currentMapId, playerX, playerY);
    }

    public WeatherCondition currentWeather() {
        char biome = currentBiomeTile();
        if (!isOutdoorWeatherMap(currentMapId)) {
            return WeatherCondition.CLEAR;
        }
        int block = Math.floorDiv(worldTick, WEATHER_BLOCK_TICKS);
        int seed = Math.abs(biome * 7349 + block * 9127 + dayNumber() * 1711);
        int roll = Math.floorMod(seed, 100);
        return switch (biome) {
            case 'f' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLEAR, WeatherCondition.CLOUDY, WeatherCondition.CLOUDY, WeatherCondition.RAIN,
                    WeatherCondition.RAIN, WeatherCondition.FOG, WeatherCondition.STORM
            });
            case 's' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLEAR, WeatherCondition.CLEAR, WeatherCondition.CLEAR, WeatherCondition.HEAT_HAZE,
                    WeatherCondition.HEAT_HAZE, WeatherCondition.DUST, WeatherCondition.CLOUDY
            });
            case 'n' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLEAR, WeatherCondition.CLOUDY, WeatherCondition.SNOW, WeatherCondition.SNOW,
                    WeatherCondition.SNOW, WeatherCondition.FOG, WeatherCondition.BLIZZARD
            });
            case 'v' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.FOG, WeatherCondition.FOG, WeatherCondition.CLOUDY, WeatherCondition.RAIN,
                    WeatherCondition.RAIN, WeatherCondition.STORM, WeatherCondition.CLEAR
            });
            case 'b' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLEAR, WeatherCondition.CLEAR, WeatherCondition.DUST, WeatherCondition.DUST,
                    WeatherCondition.HEAT_HAZE, WeatherCondition.CLOUDY, WeatherCondition.STORM
            });
            case 'm', 'q' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLEAR, WeatherCondition.CLOUDY, WeatherCondition.CLOUDY, WeatherCondition.FOG,
                    WeatherCondition.RAIN, WeatherCondition.SNOW, WeatherCondition.STORM
            });
            case 'w' -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLOUDY, WeatherCondition.RAIN, WeatherCondition.RAIN, WeatherCondition.FOG,
                    WeatherCondition.FOG, WeatherCondition.STORM, WeatherCondition.CLEAR
            });
            default -> weightedWeather(roll, new WeatherCondition[]{
                    WeatherCondition.CLEAR, WeatherCondition.CLEAR, WeatherCondition.CLOUDY, WeatherCondition.CLOUDY,
                    WeatherCondition.RAIN, WeatherCondition.FOG, WeatherCondition.STORM
            });
        };
    }

    public String weatherLabel() {
        return currentWeather().label();
    }

    public double windRadians() {
        int block = Math.floorDiv(worldTick, 900);
        char biome = currentBiomeTile();
        int seed = Math.abs(block * 48121 + biome * 1697 + dayNumber() * 337);
        return (Math.floorMod(seed, 360) - 180) * Math.PI / 180.0;
    }

    public double windStrength() {
        WeatherCondition weather = currentWeather();
        int block = Math.floorDiv(worldTick, 900);
        int seed = Math.abs(block * 7349 + currentBiomeTile() * 251 + dayNumber() * 97);
        double gust = Math.floorMod(seed, 100) / 100.0;
        double base = switch (weather) {
            case STORM, BLIZZARD -> 0.72;
            case RAIN, SNOW, DUST -> 0.42;
            case CLOUDY, FOG, HEAT_HAZE -> 0.24;
            case CLEAR -> 0.16;
        };
        return Math.min(1.0, base + gust * 0.28);
    }

    public String windLabel() {
        String[] labels = {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};
        int index = Math.floorMod((int) Math.round(windRadians() / (Math.PI / 4.0)), labels.length);
        return labels[index] + " wind";
    }

    public void openPauseMenu() {
        if (mode == GameMode.MAIN_MENU || mode == GameMode.CLASS_SELECT || mode == GameMode.PAUSE_MENU) {
            return;
        }
        pauseReturnMode = mode;
        mode = GameMode.PAUSE_MENU;
        status = "Paused.";
    }

    public void resumeGame() {
        mode = pauseReturnMode == GameMode.PAUSE_MENU ? GameMode.EXPLORE : pauseReturnMode;
        if (mode == GameMode.BATTLE) {
            status = "Battle!";
        } else if (mode == GameMode.DIALOG && activeNpc != null) {
            status = "Talking to " + activeNpc.name() + ".";
        } else {
            status = world.describe(currentMapId, playerX, playerY);
        }
    }

    public void openSettings() {
        settingsReturnMode = mode == GameMode.SETTINGS ? GameMode.MAIN_MENU : mode;
        mode = GameMode.SETTINGS;
    }

    public void closeSettings() {
        mode = settingsReturnMode == GameMode.SETTINGS ? GameMode.MAIN_MENU : settingsReturnMode;
    }

    public void openSaveMenu(boolean canSave) {
        saveMenuReturnMode = mode == GameMode.SAVE_MENU ? GameMode.MAIN_MENU : mode;
        saveMenuCanSave = canSave;
        if (canSave && pendingSaveName.isBlank()) {
            pendingSaveName = defaultSaveName();
        }
        mode = GameMode.SAVE_MENU;
    }

    public void closeSaveMenu() {
        mode = saveMenuReturnMode == GameMode.SAVE_MENU ? GameMode.MAIN_MENU : saveMenuReturnMode;
    }

    public void adjustChanceSetting(String key, double delta) {
        config.adjustChance(key, delta);
        status = "Settings updated.";
    }

    public void adjustVolumeSetting(String key, int delta) {
        config.adjustVolume(key, delta);
        status = "Audio settings updated.";
    }

    public Npc npcAtPlayer() {
        for (Npc npc : npcsForMap(currentMapId)) {
            TilePoint position = npcPosition(npc);
            if (Math.abs(position.x() - playerX) + Math.abs(position.y() - playerY) <= 1) {
                return npc;
            }
        }
        return null;
    }

    public void interact() {
        if (mode != GameMode.EXPLORE) {
            return;
        }
        if (crafting.active()) {
            status = "Busy: " + crafting.progressLabel() + ".";
            return;
        }
        WorldTransition transition = world.transitionAt(currentMapId, playerX, playerY);
        if (transition != null) {
            applyTransition(transition);
            return;
        }
        TilePoint buildingEntry = adjacentBuildingEntry();
        if (buildingEntry != null && isSettlementMap(currentMapId)) {
            CityBuilding building = world.cityBuildingEntryAt(currentMapId, buildingEntry.x(), buildingEntry.y(), playerX, playerY);
            if (WorldMap.PLAYER_VILLAGE_ID.equals(currentMapId) && building != null) {
                openBuildingAssignment(building);
                return;
            }
            enterBuildingAt(buildingEntry.x(), buildingEntry.y());
            return;
        }
        if (settlementBoardNearPlayer() != null) {
            openSettlementBoard();
            return;
        }
        QuestObjective objective = questObjectiveNearPlayer();
        if (objective != null) {
            interactQuestObjective(objective);
            return;
        }
        QuestInteractible interactible = questInteractibleNearPlayer();
        if (interactible != null) {
            interactQuestInteractible(interactible);
            return;
        }
        Npc npc = npcAtPlayer();
        if (npc == null) {
            status = "No one is close enough to talk.";
            return;
        }
        activeNpc = npc;
        activeShop = npc.shopId() == null ? null : GameData.SHOPS.get(npc.shopId());
        activeDialogueSession = DialogueLibrary.startSession(npc, questForNpc(npc), npcBiomeContext(npc), npcRelationship(npc), random);
        dialogIndex = 0;
        mode = GameMode.DIALOG;
        status = "Talking to " + npc.name() + ".";
    }

    public void advanceDialog() {
        if (mode != GameMode.DIALOG || activeNpc == null) {
            return;
        }
        Quest quest = questForNpc(activeNpc);
        if (quest != null && (!quest.accepted || quest.ready())) {
            handleActiveNpcQuestAction();
            return;
        }
        List<String> options = activeNpcDialogOptions();
        if (!options.isEmpty()) {
            selectDialogOption(Math.max(0, Math.min(dialogIndex, options.size() - 1)));
            return;
        }
        if (handleActiveNpcQuestAction()) {
            return;
        }
        if (activeShop != null) {
            mode = GameMode.SHOP;
            status = activeShop.name() + ". Press 1-" + activeShop.availableStock(player.level).size() + " to buy, Esc to leave.";
        } else {
            if (activeNpc.recruitId() != null && activeNpc.recruitCost() > 0 && !isRecruited(activeNpc.recruitId())) {
                status = activeNpc.name() + "'s companion contract costs " + activeNpc.recruitCost() + " gold. Press H to hire.";
                return;
            }
            closeOverlay();
        }
    }

    public List<String> activeNpcDialogOptions() {
        if (activeNpc == null) {
            return List.of();
        }
        if (activeDialogueSession == null) {
            activeDialogueSession = DialogueLibrary.startSession(activeNpc, questForNpc(activeNpc), npcBiomeContext(activeNpc), npcRelationship(activeNpc), random);
        }
        return activeDialogueSession.optionLabels();
    }

    public String activeNpcDialogLine() {
        if (activeNpc == null) {
            return "...";
        }
        if (activeDialogueSession == null) {
            activeDialogueSession = DialogueLibrary.startSession(activeNpc, questForNpc(activeNpc), npcBiomeContext(activeNpc), npcRelationship(activeNpc), random);
        }
        return activeDialogueSession.line(npcRelationship(activeNpc));
    }

    public void selectDialogOption(int index) {
        if (mode != GameMode.DIALOG || activeNpc == null) {
            return;
        }
        List<String> options = activeNpcDialogOptions();
        if (index >= 0 && index < options.size()) {
            DialogueLibrary.DialogueChoiceResult result = activeDialogueSession.choose(index);
            if (result.relationshipDelta() != 0) {
                adjustNpcRelationship(activeNpc, result.relationshipDelta());
            }
            dialogIndex = 0;
            if (result.relationshipDelta() == 0) {
                status = "Talking to " + activeNpc.name() + ".";
            }
        }
    }

    public int npcRelationship(Npc npc) {
        return npcRelationships.getOrDefault(npcRelationshipKey(npc), 0);
    }

    private void adjustNpcRelationship(Npc npc, int delta) {
        String key = npcRelationshipKey(npc);
        int value = Math.max(-100, Math.min(100, npcRelationships.getOrDefault(key, 0) + delta));
        npcRelationships.put(key, value);
        String direction = delta > 0 ? "improved" : "worsened";
        status = npc.name() + "'s opinion " + direction + " (" + value + ").";
    }

    private String npcRelationshipKey(Npc npc) {
        return npc.mapId() + ":" + npc.name().strip().toLowerCase();
    }

    private String npcBiomeContext(Npc npc) {
        char npcTile = WorldMap.OVERWORLD_ID.equals(npc.mapId())
                ? world.tileAt(npc.mapId(), npc.x(), npc.y())
                : currentBiomeTile();
        return DialogueLibrary.biomeContext(npc, npcTile, currentBiomeTile());
    }

    public void openActiveShop() {
        if (mode == GameMode.DIALOG && activeShop != null) {
            mode = GameMode.SHOP;
            status = activeShop.name() + ". Press 1-" + activeShop.availableStock(player.level).size() + " to buy, Esc to leave.";
        }
    }

    public boolean handleActiveNpcQuestAction() {
        if (activeNpc.questId() == null) {
            return false;
        }
        Quest quest = quests.get(activeNpc.questId());
        if (quest == null) {
            return false;
        }
        if (!quest.accepted) {
            quest.accepted = true;
            status = "Quest accepted: " + quest.title + ". " + quest.startDialog;
            return true;
        } else if (quest.ready()) {
            quest.completed = true;
            player.gold += quest.rewardGold;
            int questXp = (int) Math.round(quest.rewardXp * (1.0 + player.skillRank("hard_won_lessons") * 0.08));
            List<String> notes = player.gainXp(questXp);
            status = "Quest complete: " + quest.title + ". " + quest.completeDialog;
            String recruitNote = recruitAlly(activeNpc.recruitId());
            if (recruitNote != null) {
                status += " " + recruitNote;
            }
            if (!notes.isEmpty()) {
                status += " " + notes.get(notes.size() - 1);
            }
            refreshPlayerVillageGrowth();
            return true;
        } else if (!quest.completed) {
            status = quest.title + ": " + quest.progress + "/" + quest.needed + ". " + quest.progressDialog;
            return true;
        } else {
            status = quest.title + " is already complete.";
            return false;
        }
    }

    public Quest questForNpc(Npc npc) {
        return npc.questId() == null ? null : quests.get(npc.questId());
    }

    public String questGiverName(String questId) {
        Npc npc = questGiver(questId);
        return npc == null ? "the quest giver" : npc.name();
    }

    public String questReturnLocation(String questId) {
        Npc npc = questGiver(questId);
        return npc == null ? "Unknown" : world.label(npc.mapId());
    }

    private Npc questGiver(String questId) {
        for (Npc npc : GameData.NPCS) {
            if (questId.equals(npc.questId())) {
                return npc;
            }
        }
        return null;
    }

    public void closeOverlay() {
        activeNpc = null;
        activeShop = null;
        activeDialogueSession = null;
        activeVillageBuilding = null;
        dialogIndex = 0;
        pendingVillageMoveSource = null;
        pendingVillageMoveMapId = null;
        mode = GameMode.EXPLORE;
    }

    public void gatherNearby() {
        if (mode != GameMode.EXPLORE) {
            return;
        }
        CraftingSystem.GatherCandidate candidate = crafting.findGatherTarget(world, currentMapId, playerX, playerY);
        status = crafting.beginGather(player, activeAllies(), candidate, random);
    }

    public void toggleCrafting() {
        if (mode == GameMode.CRAFTING) {
            closeOverlay();
            return;
        }
        if (mode != GameMode.EXPLORE) {
            return;
        }
        CraftingSystem.Workstation workstation = currentWorkstation();
        mode = GameMode.CRAFTING;
        status = workstation == null ? "Field crafting." : "Crafting at " + workstation.label() + ".";
    }

    public CraftingSystem.Workstation currentWorkstation() {
        for (WorldProp prop : world.props(currentMapId)) {
            CraftingSystem.Workstation workstation = CraftingSystem.workstationForAsset(prop.asset());
            if (workstation == null) {
                continue;
            }
            int[] footprint = CraftingSystem.workstationFootprint(prop.asset());
            for (int yy = prop.y(); yy < prop.y() + footprint[1]; yy++) {
                for (int xx = prop.x(); xx < prop.x() + footprint[0]; xx++) {
                    if (Math.abs(xx - playerX) + Math.abs(yy - playerY) <= 1) {
                        return workstation;
                    }
                }
            }
        }
        return null;
    }

    public List<CraftingSystem.Recipe> availableCraftingRecipes() {
        return CraftingSystem.recipesFor(currentWorkstation());
    }

    public void craftRecipe(String recipeKey) {
        if (mode != GameMode.CRAFTING && mode != GameMode.EXPLORE) {
            return;
        }
        CraftingSystem.Recipe recipe = CraftingSystem.recipeByKey(recipeKey);
        CraftingSystem.Workstation workstation = currentWorkstation();
        if (recipe == null || (recipe.workstation() != null && recipe.workstation() != workstation)) {
            status = "That recipe needs the right workstation.";
            return;
        }
        status = crafting.beginCraft(player, recipe);
        if (crafting.active()) {
            mode = GameMode.EXPLORE;
        }
    }

    public void craftRecipeAt(int index) {
        List<CraftingSystem.Recipe> recipes = availableCraftingRecipes();
        if (index >= 0 && index < recipes.size()) {
            craftRecipe(recipes.get(index).key());
        }
    }

    public void toggleQuestLog() {
        if (mode == GameMode.QUEST_LOG) {
            closeOverlay();
        } else if (mode == GameMode.EXPLORE) {
            mode = GameMode.QUEST_LOG;
        }
    }

    public void toggleInventory() {
        if (mode == GameMode.INVENTORY) {
            closeOverlay();
        } else if (mode == GameMode.EXPLORE) {
            mode = GameMode.INVENTORY;
        }
    }

    public void toggleParty() {
        if (mode == GameMode.PARTY) {
            closeOverlay();
        } else if (mode == GameMode.EXPLORE) {
            partyScreenIndex = Math.max(0, Math.min(partyScreenIndex, partyMembers().size() - 1));
            mode = GameMode.PARTY;
        }
    }

    public void toggleVillage() {
        if (mode == GameMode.VILLAGE) {
            closeOverlay();
        } else if (mode == GameMode.EXPLORE && isManagedVillageContext()) {
            resetVillageInterface();
            if (isManagedVillageInterior()) {
                villageTab = 3;
            }
            mode = GameMode.VILLAGE;
            status = "Oathstead planning board opened.";
        } else if (mode == GameMode.EXPLORE) {
            status = "Return to Oathstead Camp to manage the village.";
        }
    }

    public void openSettlementBoard() {
        if (mode != GameMode.EXPLORE || settlementBoardNearPlayer() == null) {
            status = "Stand beside the Oathstead quest board to inspect growth.";
            return;
        }
        refreshPlayerVillageGrowth();
        if (settlementRecruitOptions.isEmpty()) {
            rerollSettlementRecruits();
        }
        mode = GameMode.SETTLEMENT_BOARD;
        status = "Oathstead quest board opened.";
    }

    public void setSettlementBoardTab(int tab) {
        settlementBoardTab = Math.max(0, Math.min(1, tab));
        if (settlementBoardTab == 1 && settlementRecruitOptions.isEmpty()) {
            rerollSettlementRecruits();
        }
    }

    public WorldProp settlementBoardNearPlayer() {
        if (!WorldMap.PLAYER_VILLAGE_ID.equals(currentMapId)) {
            return null;
        }
        for (WorldProp prop : world.props(currentMapId)) {
            if (!"player_village_quest_board".equals(prop.asset())) {
                continue;
            }
            if (Math.abs(prop.x() - playerX) + Math.abs(prop.y() - playerY) <= 1) {
                return prop;
            }
        }
        return null;
    }

    public VillageManager.SettlementStage villageStage() {
        return VillageManager.settlementStage(world.playerVillageStage());
    }

    public VillageManager.SettlementStage nextVillageStage() {
        return VillageManager.nextSettlementStage(world.playerVillageStage());
    }

    public int completedQuestCount() {
        int count = 0;
        for (Quest quest : quests.values()) {
            if (quest.completed) {
                count++;
            }
        }
        return count;
    }

    public int villageBuildingCount() {
        return world.playerVillageBuildings().size();
    }

    public int villageDevelopedTileCount() {
        return world.playerVillageDevelopedTileCount();
    }

    public int targetVillageStage() {
        return VillageManager.settlementStageFor(
                player.level,
                villageBuildingCount(),
                villageAllies.size(),
                completedQuestCount(),
                villageStorageUsed(),
                villageDevelopedTileCount()
        );
    }

    public boolean refreshPlayerVillageGrowth() {
        int before = world.playerVillageStage();
        int target = targetVillageStage();
        int shift = world.setPlayerVillageStage(target);
        if (shift <= 0 || target <= before) {
            return false;
        }
        if (WorldMap.PLAYER_VILLAGE_ID.equals(currentMapId)) {
            playerX += shift;
            playerY += shift;
        }
        VillageManager.SettlementStage stage = villageStage();
        status = "Oathstead grew to Stage " + stage.stage() + ": " + stage.title()
                + ". The village expanded by 10 tiles in every direction.";
        return true;
    }

    public List<VillageRecruitOption> settlementRecruitOptions() {
        if (settlementRecruitOptions.isEmpty()) {
            rerollSettlementRecruits();
        }
        return settlementRecruitOptions;
    }

    public void rerollSettlementRecruits() {
        List<VillageRecruitOption> options = new ArrayList<>();
        int seed = Math.abs(player.name.hashCode() * 31 + worldTick + world.playerVillageStage() * 97 + allies.size() * 53);
        for (int i = 0; i < 4; i++) {
            options.add(createVillageRecruit(seed + i * 41, i));
        }
        settlementRecruitOptions = options;
        status = "New recruitment notices posted.";
    }

    public void recruitSettlementNpc(int index) {
        List<VillageRecruitOption> options = settlementRecruitOptions();
        if (index < 0 || index >= options.size()) {
            return;
        }
        VillageRecruitOption option = options.get(index);
        if (player.gold < option.goldCost()) {
            status = option.name() + " asks for " + option.goldCost() + " gold.";
            return;
        }
        if (allyByName(option.name()) != null) {
            status = option.name() + " is already with you.";
            return;
        }
        player.gold -= option.goldCost();
        Actor actor = new Actor(option.name(), option.sprite(), option.className(),
                option.hp(), option.mp(), option.attack(), option.defense());
        actor.abilities.addAll(GameData.classAbilities(option.className()));
        actor.level = Math.max(1, Math.min(player.level, option.level()));
        actor.healFull();
        syncSkillAbilities(actor);
        allies.add(actor);
        settlementRecruitOptions = options.stream()
                .filter(candidate -> candidate != option)
                .toList();
        status = option.name() + " joins Oathstead.";
        refreshPlayerVillageGrowth();
    }

    private VillageRecruitOption createVillageRecruit(int seed, int slot) {
        String[] names = {
                "Aster", "Bryn", "Cala", "Dain", "Elian", "Fara", "Galen", "Hale",
                "Iria", "Jory", "Kellan", "Lysa", "Marn", "Nia", "Oren", "Pella",
                "Quin", "Rook", "Sera", "Tavin", "Una", "Vell", "Wren", "Yara"
        };
        String[] epithets = {
                "Greenhand", "Stonewake", "Brightforge", "Roadwatch", "Oakwise", "Hearthbound",
                "Fieldward", "Duskbell", "Ashbrook", "Mossvale", "Emberkin", "Hillborn"
        };
        String[][] classes = {
                {"Worker", "npc_citizen_man", "54", "12", "8", "4"},
                {"Scout", "npc_citizen_woman", "46", "18", "11", "3"},
                {"Guard", "npc_blacksmith", "60", "10", "12", "5"},
                {"Forager", "npc_baker", "50", "16", "9", "3"},
                {"Medic", "npc_elowen", "48", "26", "8", "3"},
                {"Trader", "npc_merchant", "50", "18", "9", "4"}
        };
        int nameIndex = Math.floorMod(seed, names.length);
        int classIndex = Math.floorMod(seed / 7 + slot, classes.length);
        String[] archetype = classes[classIndex];
        String name = names[nameIndex] + " " + epithets[Math.floorMod(seed / 13, epithets.length)];
        while (allyByName(name) != null) {
            name = names[Math.floorMod(++nameIndex, names.length)] + " " + epithets[Math.floorMod(seed / 13, epithets.length)];
        }
        int level = Math.max(1, Math.min(player.level, 1 + world.playerVillageStage() / 2 + Math.floorMod(seed, 3)));
        return new VillageRecruitOption(
                name,
                archetype[0],
                archetype[1],
                level,
                Integer.parseInt(archetype[2]) + level * 2,
                Integer.parseInt(archetype[3]) + level,
                Integer.parseInt(archetype[4]) + level / 2,
                Integer.parseInt(archetype[5]) + level / 3,
                0
        );
    }

    public void resetVillageInterface() {
        villageTab = 0;
        villageEditMode = false;
        villageEditAction = "place";
        pendingVillageMoveSource = null;
        pendingVillageMoveMapId = null;
    }

    public boolean isManagedVillageContext() {
        return WorldMap.PLAYER_VILLAGE_ID.equals(currentMapId) || currentMapId.startsWith("house_" + WorldMap.PLAYER_VILLAGE_ID + "_");
    }

    public boolean isManagedVillageMap() {
        return WorldMap.PLAYER_VILLAGE_ID.equals(currentMapId);
    }

    public boolean isManagedVillageInterior() {
        return currentMapId.startsWith("house_" + WorldMap.PLAYER_VILLAGE_ID + "_") && "interior".equals(world.kind(currentMapId));
    }

    public void setVillageTab(int tab) {
        villageTab = Math.max(0, Math.min(4, tab));
        pendingVillageMoveSource = null;
        pendingVillageMoveMapId = null;
    }

    public void setVillageEditAction(String action) {
        villageEditAction = action == null ? "place" : action;
        villageEditMode = !"place".equals(villageEditAction);
        if (villageEditMode) {
            selectedVillageBuildingStyle = "";
        }
        pendingVillageMoveSource = null;
        pendingVillageMoveMapId = null;
        status = villageEditMode ? "Village edit mode: " + villageEditAction + "." : "Village placement mode.";
    }

    public void selectVillageBuildingStyle(String style) {
        String nextStyle = VillageManager.buildingPlan(style).style();
        if (nextStyle.equals(selectedVillageBuildingStyle) && "place".equals(villageEditAction)) {
            selectedVillageBuildingStyle = "";
            status = "Building placement cleared.";
            return;
        }
        selectedVillageBuildingStyle = nextStyle;
        setVillageEditAction("place");
    }

    public void selectVillageAsset(String asset) {
        VillageManager.PlaceableAsset option = VillageManager.outdoorAsset(asset);
        selectedVillageAsset = option.asset();
        selectedVillagePropCategory = option.category();
        setVillageEditAction("place");
    }

    public void selectVillagePropCategory(String category) {
        List<VillageManager.PlaceableAsset> options = VillageManager.outdoorAssets(category);
        if (options.isEmpty()) {
            return;
        }
        selectedVillagePropCategory = options.get(0).category();
        selectedVillageAsset = options.get(0).asset();
        setVillageEditAction("place");
    }

    public void selectVillageTile(char tile) {
        selectedVillageTile = VillageManager.tilePlan(tile).tile();
        setVillageEditAction("place");
    }

    public void selectInteriorAsset(String asset) {
        selectedInteriorAsset = VillageManager.interiorAsset(asset).asset();
        setVillageEditAction("place");
    }

    public void handleVillageWorldClick(int x, int y) {
        if (mode != GameMode.VILLAGE) {
            return;
        }
        if (villageTab == 3 || isManagedVillageInterior()) {
            handleInteriorPlacementClick(x, y);
            return;
        }
        if (!isManagedVillageMap()) {
            status = "Village building changes must be made at Oathstead Camp.";
            return;
        }
        if (villageTab == 1) {
            handleVillageTileClick(x, y);
            return;
        }
        if (villageTab == 2) {
            handleVillageAssetClick(x, y);
            return;
        }
        handleVillageBuildingClick(x, y);
    }

    private void handleVillageBuildingClick(int x, int y) {
        CityBuilding building = world.cityBuildingAt(WorldMap.PLAYER_VILLAGE_ID, x, y);
        if ("delete".equals(villageEditAction)) {
            if (world.removePlayerVillageBuilding(building)) {
                villageBuildingAssignments.remove(building.key());
                resetNpcRuntime();
                status = "Building removed.";
            } else {
                status = "Only buildings you placed can be removed.";
            }
            return;
        }
        if ("move".equals(villageEditAction)) {
            if (pendingVillageMoveSource == null) {
                if (building == null || !building.key().startsWith("player_")) {
                    status = "Select one of your placed buildings first.";
                    return;
                }
                pendingVillageMoveSource = building.anchor();
                pendingVillageMoveMapId = currentMapId;
                status = "Choose the new building location.";
                return;
            }
            CityBuilding source = world.cityBuildingAt(WorldMap.PLAYER_VILLAGE_ID, pendingVillageMoveSource.x(), pendingVillageMoveSource.y());
            if (world.movePlayerVillageBuilding(source, x, y)) {
                resetNpcRuntime();
                status = "Building moved.";
            } else {
                status = "That location cannot hold this building.";
            }
            pendingVillageMoveSource = null;
            pendingVillageMoveMapId = null;
            return;
        }
        if ("upgrade".equals(villageEditAction)) {
            upgradeVillageBuildingAt(x, y);
            return;
        }
        if (selectedVillageBuildingStyle == null || selectedVillageBuildingStyle.isBlank()) {
            status = "Select a building to place.";
            return;
        }
        VillageManager.BuildingPlan plan = VillageManager.buildingPlan(selectedVillageBuildingStyle);
        if (!canAffordVillageCost(plan.cost())) {
            status = "You need " + VillageManager.costLabel(plan.cost()) + " for " + plan.label() + ".";
            return;
        }
        CityBuilding placed = world.placePlayerVillageBuilding(plan.style(), x, y);
        if (placed != null) {
            spendVillageCost(plan.cost());
        }
        if (placed == null) {
            status = "That building will not fit there.";
        } else if (!refreshPlayerVillageGrowth()) {
            status = "Placed " + villageBuildingLabel(placed.style()) + ".";
        }
    }

    private void upgradeVillageBuildingAt(int x, int y) {
        CityBuilding building = world.cityBuildingAt(WorldMap.PLAYER_VILLAGE_ID, x, y);
        if (building == null || !building.key().startsWith("player_")) {
            status = "Select a building you placed to upgrade.";
            return;
        }
        int level = world.playerVillageBuildingLevel(building);
        VillageManager.BuildingPlan plan = VillageManager.buildingPlan(building.style());
        if (level >= plan.maxLevel()) {
            status = plan.label() + " is already at max level.";
            return;
        }
        VillageManager.VillageCost cost = VillageManager.upgradeCost(building.style(), level);
        if (!canAffordVillageCost(cost)) {
            status = "Upgrade needs " + VillageManager.costLabel(cost) + ".";
            return;
        }
        spendVillageCost(cost);
        if (world.upgradePlayerVillageBuilding(building)) {
            if (!refreshPlayerVillageGrowth()) {
                status = plan.label() + " upgraded to level " + (level + 1) + ".";
            }
        } else {
            status = "That building cannot be upgraded right now.";
        }
    }

    private void handleVillageTileClick(int x, int y) {
        if ("move".equals(villageEditAction)) {
            status = "Tiles can be placed or deleted directly.";
            return;
        }
        char tile = "delete".equals(villageEditAction) ? 'g' : selectedVillageTile;
        VillageManager.TilePlan plan = VillageManager.tilePlan(tile);
        if (!"delete".equals(villageEditAction) && !canAffordVillageCost(plan.cost())) {
            status = "You need " + VillageManager.costLabel(plan.cost()) + " for " + plan.label() + ".";
            return;
        }
        boolean placed = world.setPlayerVillageTile(x, y, tile);
        if (placed && !"delete".equals(villageEditAction)) {
            spendVillageCost(plan.cost());
        }
        if (!placed) {
            status = "That tile cannot be changed.";
        } else if (!refreshPlayerVillageGrowth()) {
            status = plan.label() + " set at " + x + ", " + y + ".";
        }
    }

    private void handleVillageAssetClick(int x, int y) {
        WorldProp prop = world.playerVillagePropAt(x, y);
        if ("delete".equals(villageEditAction)) {
            status = world.removePlayerVillageProp(prop) ? "Village asset removed." : "No placed asset there.";
            return;
        }
        if ("move".equals(villageEditAction)) {
            if (pendingVillageMoveSource == null) {
                if (prop == null) {
                    status = "Select a village asset first.";
                    return;
                }
                pendingVillageMoveSource = new TilePoint(x, y);
                pendingVillageMoveMapId = currentMapId;
                status = "Choose the new asset location.";
                return;
            }
            WorldProp source = world.playerVillagePropAt(pendingVillageMoveSource.x(), pendingVillageMoveSource.y());
            if (world.movePlayerVillageProp(source, x, y)) {
                status = "Village asset moved.";
            } else {
                status = "That asset will not fit there.";
            }
            pendingVillageMoveSource = null;
            pendingVillageMoveMapId = null;
            return;
        }
        VillageManager.PlaceableAsset asset = VillageManager.outdoorAsset(selectedVillageAsset);
        if (!canAffordVillageCost(asset.cost())) {
            status = "You need " + VillageManager.costLabel(asset.cost()) + " for " + asset.label() + ".";
            return;
        }
        boolean placed = world.addPlayerVillageProp(x, y, asset.asset(), asset.size());
        if (placed) {
            spendVillageCost(asset.cost());
        }
        if (!placed) {
            status = "That asset cannot be placed there.";
        } else if (!refreshPlayerVillageGrowth()) {
            status = asset.label() + " placed.";
        }
    }

    private void handleInteriorPlacementClick(int x, int y) {
        if (!isManagedVillageInterior()) {
            status = "Enter a village building to edit its interior.";
            return;
        }
        WorldProp prop = world.playerInteriorPropAt(currentMapId, x, y);
        if ("delete".equals(villageEditAction)) {
            status = world.removePlayerInteriorProp(currentMapId, prop) ? "Interior asset removed." : "No placed interior asset there.";
            return;
        }
        if ("move".equals(villageEditAction)) {
            if (pendingVillageMoveSource == null) {
                if (prop == null) {
                    status = "Select a placed interior asset first.";
                    return;
                }
                pendingVillageMoveSource = new TilePoint(x, y);
                pendingVillageMoveMapId = currentMapId;
                status = "Choose the new interior location.";
                return;
            }
            WorldProp source = currentMapId.equals(pendingVillageMoveMapId)
                    ? world.playerInteriorPropAt(currentMapId, pendingVillageMoveSource.x(), pendingVillageMoveSource.y())
                    : null;
            if (world.movePlayerInteriorProp(currentMapId, source, x, y)) {
                status = "Interior asset moved.";
            } else {
                status = "That interior asset will not fit there.";
            }
            pendingVillageMoveSource = null;
            pendingVillageMoveMapId = null;
            return;
        }
        VillageManager.PlaceableAsset asset = VillageManager.interiorAsset(selectedInteriorAsset);
        if (!canAffordVillageCost(asset.cost())) {
            status = "You need " + VillageManager.costLabel(asset.cost()) + " for " + asset.label() + ".";
            return;
        }
        boolean placed = world.addPlayerInteriorProp(currentMapId, x, y, asset.asset(), asset.size());
        if (placed) {
            spendVillageCost(asset.cost());
        }
        status = placed ? asset.label() + " placed." : "That interior asset cannot be placed there.";
    }

    public String villageBuildingLabel(String style) {
        return VillageManager.buildingLabel(style);
    }

    public void toggleSkills() {
        if (mode == GameMode.SKILLS) {
            closeOverlay();
        } else if (mode == GameMode.EXPLORE) {
            mode = GameMode.SKILLS;
        }
    }

    public void toggleWorldMap() {
        if (mode == GameMode.WORLD_MAP) {
            closeOverlay();
        } else if (mode == GameMode.EXPLORE) {
            mode = GameMode.WORLD_MAP;
        }
    }

    public void toggleWorldMapKingdoms() {
        worldMapKingdoms = !worldMapKingdoms;
        status = worldMapKingdoms ? "Kingdom borders shown on the world map." : "Terrain map shown.";
    }

    public void setZoom(int zoom) {
        this.zoom = Math.max(70, Math.min(150, zoom));
    }

    public void adjustZoom(int delta) {
        setZoom(zoom + delta);
    }

    public void buyShopItem(int index) {
        List<String> stock = activeShop == null ? List.of() : activeShop.availableStock(player.level);
        if (mode != GameMode.SHOP || activeShop == null || index < 0 || index >= stock.size()) {
            return;
        }
        String itemKey = stock.get(index);
        int cost = GameData.itemCost(itemKey);
        if (cost <= 0) {
            return;
        }
        if (player.gold < cost) {
            status = "Not enough gold for " + GameData.itemName(itemKey) + ".";
            return;
        }
        player.gold -= cost;
        player.addItem(itemKey, 1);
        status = "Bought " + GameData.itemName(itemKey) + ".";
    }

    public boolean isRecruited(String recruitId) {
        return recruitId != null && recruitedIds.contains(recruitId);
    }

    public List<Actor> activeAllies() {
        return allies.stream()
                .filter(ally -> !villageAllies.contains(ally.name))
                .toList();
    }

    public List<Actor> stationedAllies() {
        return allies.stream()
                .filter(ally -> villageAllies.contains(ally.name))
                .toList();
    }

    public List<Actor> partyMembers() {
        List<Actor> members = new ArrayList<>();
        members.add(player);
        members.addAll(activeAllies());
        return members;
    }

    public Actor partyScreenActor() {
        List<Actor> members = partyMembers();
        partyScreenIndex = Math.max(0, Math.min(partyScreenIndex, members.size() - 1));
        return members.get(partyScreenIndex);
    }

    public void selectPartyScreenActor(int index) {
        List<Actor> members = partyMembers();
        if (index >= 0 && index < members.size()) {
            partyScreenIndex = index;
            status = "Managing " + members.get(index).name + ".";
        }
    }

    public String recruitAlly(String recruitId) {
        if (recruitId == null || isRecruited(recruitId)) {
            return null;
        }
        GameData.RecruitSpec spec = GameData.RECRUITS.get(recruitId);
        if (spec == null) {
            return null;
        }
        Actor ally = spec.createActor();
        syncSkillAbilities(ally);
        allies.add(ally);
        recruitedIds.add(recruitId);
        return ally.name + " joins your party.";
    }

    public void stationAlly(String allyName) {
        Actor ally = allyByName(allyName);
        if (ally == null) {
            status = "That ally is not available.";
            return;
        }
        villageAllies.add(ally.name);
        villageWorkerRoles.putIfAbsent(ally.name, "idle");
        partyScreenIndex = Math.max(0, Math.min(partyScreenIndex, partyMembers().size() - 1));
        status = ally.name + " is set to city attendance at Oathstead.";
        refreshPlayerVillageGrowth();
    }

    public void recallAlly(String allyName) {
        Actor ally = allyByName(allyName);
        if (ally == null) {
            status = "That ally is not available.";
            return;
        }
        villageAllies.remove(ally.name);
        villageWorkerRoles.remove(ally.name);
        villageBuildingAssignments.values().removeIf(name -> name.equals(ally.name));
        status = ally.name + " rejoins your traveling party.";
    }

    public void openBuildingAssignment(CityBuilding building) {
        if (building == null || !WorldMap.PLAYER_VILLAGE_ID.equals(currentMapId)) {
            return;
        }
        activeNpc = null;
        activeShop = null;
        activeDialogueSession = null;
        activeVillageBuilding = building;
        mode = GameMode.BUILDING_ASSIGNMENT;
        status = "Assign city-attendance companions to " + VillageManager.buildingLabel(building.style()) + ".";
    }

    public void enterActiveVillageBuilding() {
        if (activeVillageBuilding == null) {
            closeOverlay();
            return;
        }
        CityBuilding building = activeVillageBuilding;
        activeVillageBuilding = null;
        enterBuildingAt(building.x1(), building.y2());
    }

    public String assignedAllyForBuilding(String buildingKey) {
        return villageBuildingAssignments.getOrDefault(buildingKey, "");
    }

    public String buildingAssignmentForAlly(String allyName) {
        for (Map.Entry<String, String> entry : villageBuildingAssignments.entrySet()) {
            if (entry.getValue().equals(allyName)) {
                return entry.getKey();
            }
        }
        return "";
    }

    public CityBuilding playerVillageBuildingByKey(String buildingKey) {
        for (CityBuilding building : world.playerVillageBuildings()) {
            if (building.key().equals(buildingKey)) {
                return building;
            }
        }
        return null;
    }

    public void assignAllyToActiveBuilding(String allyName) {
        if (activeVillageBuilding == null) {
            status = "No building selected.";
            return;
        }
        Actor ally = allyByName(allyName);
        if (ally == null || !villageAllies.contains(ally.name)) {
            status = "Set that companion to city attendance before assigning a building.";
            return;
        }
        villageBuildingAssignments.entrySet().removeIf(entry -> entry.getValue().equals(ally.name));
        villageBuildingAssignments.put(activeVillageBuilding.key(), ally.name);
        String roleId = VillageManager.buildingPlan(activeVillageBuilding.style()).workerRole();
        if (!roleId.isBlank()) {
            villageWorkerRoles.put(ally.name, VillageManager.workerRole(roleId).id());
        } else {
            villageWorkerRoles.putIfAbsent(ally.name, "idle");
        }
        status = ally.name + " assigned to " + VillageManager.buildingLabel(activeVillageBuilding.style()) + ".";
        resetNpcRuntime();
    }

    public void clearActiveBuildingAssignment() {
        if (activeVillageBuilding == null) {
            return;
        }
        String removed = villageBuildingAssignments.remove(activeVillageBuilding.key());
        status = removed == null || removed.isBlank()
                ? "No one was assigned there."
                : removed + " is no longer assigned to that building.";
        resetNpcRuntime();
    }

    public void assignVillageRole(String allyName, String roleId) {
        Actor ally = allyByName(allyName);
        if (ally == null || !villageAllies.contains(ally.name)) {
            status = "Station that ally at Oathstead before assigning work.";
            return;
        }
        VillageManager.WorkerRole role = VillageManager.workerRole(roleId);
        if (!role.requiredBuildingStyle().isBlank() && world.playerVillageBuildingCount(role.requiredBuildingStyle()) <= 0) {
            status = role.label() + " requires " + VillageManager.buildingLabel(role.requiredBuildingStyle()) + ".";
            return;
        }
        villageWorkerRoles.put(ally.name, role.id());
        status = ally.name + " assigned as " + role.label() + ".";
        refreshPlayerVillageGrowth();
    }

    public String villageRoleFor(String allyName) {
        return villageWorkerRoles.getOrDefault(allyName, "idle");
    }

    public int villageStorageUsed() {
        int total = 0;
        for (int amount : villageStorage.values()) {
            total += amount;
        }
        return total;
    }

    public int villageStorageCapacity() {
        return world.playerVillageStorageCapacity();
    }

    public int villageStoredAmount(String resource) {
        return villageStorage.getOrDefault(resource, 0);
    }

    public boolean canAffordVillageCost(VillageManager.VillageCost cost) {
        if (config.creativeBuildMode) {
            return true;
        }
        if (cost == null) {
            return true;
        }
        if (player.gold < cost.gold()) {
            return false;
        }
        for (Map.Entry<String, Integer> entry : cost.items().entrySet()) {
            int available = player.inventory.getOrDefault(entry.getKey(), 0) + villageStorage.getOrDefault(entry.getKey(), 0);
            if (available < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public void spendVillageCost(VillageManager.VillageCost cost) {
        if (config.creativeBuildMode || cost == null || cost.isFree()) {
            return;
        }
        player.gold -= cost.gold();
        for (Map.Entry<String, Integer> entry : cost.items().entrySet()) {
            int remaining = entry.getValue();
            int stored = villageStorage.getOrDefault(entry.getKey(), 0);
            int fromStorage = Math.min(stored, remaining);
            if (fromStorage > 0) {
                updateVillageStorage(entry.getKey(), stored - fromStorage);
                remaining -= fromStorage;
            }
            for (int i = 0; i < remaining; i++) {
                player.consumeItem(entry.getKey());
            }
        }
    }

    private int addVillageStorage(String resource, int amount) {
        if (resource == null || resource.isBlank() || amount <= 0) {
            return 0;
        }
        int space = Math.max(0, villageStorageCapacity() - villageStorageUsed());
        int stored = Math.min(space, amount);
        if (stored > 0) {
            villageStorage.merge(resource, stored, Integer::sum);
        }
        return stored;
    }

    private void updateVillageStorage(String resource, int amount) {
        if (amount <= 0) {
            villageStorage.remove(resource);
        } else {
            villageStorage.put(resource, amount);
        }
    }

    private Actor allyByName(String allyName) {
        for (Actor ally : allies) {
            if (ally.name.equals(allyName)) {
                return ally;
            }
        }
        return null;
    }

    public void hireActiveRecruit() {
        if ((mode != GameMode.DIALOG && mode != GameMode.SHOP) || activeNpc == null) {
            return;
        }
        if (activeNpc.recruitId() == null || activeNpc.recruitCost() <= 0) {
            return;
        }
        if (isRecruited(activeNpc.recruitId())) {
            status = activeNpc.name() + " already travels with you.";
            return;
        }
        if (player.gold < activeNpc.recruitCost()) {
            status = activeNpc.name() + "'s contract costs " + activeNpc.recruitCost() + " gold.";
            return;
        }
        player.gold -= activeNpc.recruitCost();
        String recruitNote = recruitAlly(activeNpc.recruitId());
        if (recruitNote == null) {
            player.gold += activeNpc.recruitCost();
            status = activeNpc.name() + " cannot join right now.";
            return;
        }
        status = "Paid " + activeNpc.recruitCost() + " gold. " + recruitNote;
    }

    public void useInventoryItem(int index) {
        if (mode != GameMode.INVENTORY || index < 0 || index >= player.inventory.size()) {
            return;
        }
        String itemKey = player.inventory.keySet().stream().toList().get(index);
        useInventoryItemForActor(partyScreenActor(), itemKey);
    }

    public void useItem(String itemKey) {
        Equipment equipment = GameData.EQUIPMENT.get(itemKey);
        if (equipment != null) {
            status = player.equipItem(itemKey);
            return;
        }
        Item item = GameData.ITEMS.get(itemKey);
        if (item == null || !player.hasItem(itemKey)) {
            status = "No usable " + GameData.itemName(itemKey) + ".";
            return;
        }
        int heal = item.heal();
        int mp = item.mp();
        if (heal > 0) {
            heal += player.skillRank("merchant_sense") * 3;
            if (player.skillRank("battle_medic") > 0) {
                heal += 8;
            }
        }
        if (mp > 0) {
            mp += player.skillRank("merchant_sense") * 2;
        }
        if (battle != null && mode == GameMode.BATTLE && !battle.finished) {
            if (!battle.playerUseItem(item.name(), heal, mp)) {
                status = "Could not use " + item.name() + " right now.";
                return;
            }
            player.consumeItem(itemKey);
            updateAfterBattleAction();
            return;
        } else {
            if (player.hp >= player.maxHp && player.mp >= player.maxMp) {
                status = "You are already refreshed.";
                return;
            }
            player.consumeItem(itemKey);
            player.hp = Math.min(player.maxHp, player.hp + heal);
            player.mp = Math.min(player.maxMp, player.mp + mp);
        }
        status = "Used " + item.name() + ".";
    }

    public void unequipSlot(String slot) {
        if (mode == GameMode.INVENTORY) {
            unequipEquipmentSlot(partyScreenActor(), slot);
        } else {
            status = player.unequipSlot(slot);
        }
    }

    public void usePartyInventoryItem(String itemKey) {
        if ((mode != GameMode.PARTY && mode != GameMode.INVENTORY) || itemKey == null || itemKey.isBlank()) {
            return;
        }
        useInventoryItemForActor(partyScreenActor(), itemKey);
    }

    private void useInventoryItemForActor(Actor actor, String itemKey) {
        if (GameData.EQUIPMENT.containsKey(itemKey)) {
            equipPartyItem(actor, itemKey);
            return;
        }
        Item item = GameData.ITEMS.get(itemKey);
        if (item == null) {
            if (CraftingSystem.isCraftingOnlyItem(itemKey)) {
                status = GameData.itemName(itemKey) + " is used for crafting.";
                return;
            }
            status = "No usable " + GameData.itemName(itemKey) + ".";
            return;
        }
        if (actor.hp >= actor.maxHp && actor.mp >= actor.maxMp) {
            status = actor.name + " is already refreshed.";
            return;
        }
        if (!consumeSharedOrPersonal(actor, itemKey)) {
            status = "No usable " + GameData.itemName(itemKey) + ".";
            return;
        }
        int heal = item.heal();
        int mp = item.mp();
        if (heal > 0) {
            heal += actor.skillRank("merchant_sense") * 3;
            if (actor.skillRank("battle_medic") > 0) {
                heal += 8;
            }
        }
        if (mp > 0) {
            mp += actor.skillRank("merchant_sense") * 2;
        }
        actor.hp = Math.min(actor.maxHp, actor.hp + heal);
        actor.mp = Math.min(actor.maxMp, actor.mp + mp);
        status = "Used " + item.name() + " on " + actor.name + ".";
    }

    public void unequipPartySlot(String slot) {
        if (mode != GameMode.PARTY && mode != GameMode.INVENTORY) {
            return;
        }
        unequipEquipmentSlot(partyScreenActor(), slot);
    }

    public void equipInventoryItemInSlot(String itemKey, String slot) {
        if (mode != GameMode.INVENTORY || itemKey == null || slot == null) {
            return;
        }
        Equipment item = GameData.EQUIPMENT.get(itemKey);
        if (item == null) {
            status = GameData.itemName(itemKey) + " is not gear.";
            return;
        }
        if (!slot.equals(item.slot())) {
            status = item.name() + " belongs in the " + item.slot() + " slot.";
            return;
        }
        equipPartyItem(partyScreenActor(), itemKey);
    }

    private void unequipEquipmentSlot(Actor actor, String slot) {
        String oldKey = actor.equipment.get(slot);
        status = actor.unequipSlot(slot);
        if (oldKey != null && actor != player && actor.consumeItem(oldKey)) {
            player.addItem(oldKey, 1);
            status += " Returned to shared pack.";
        }
    }

    private void equipPartyItem(Actor actor, String itemKey) {
        Equipment item = GameData.EQUIPMENT.get(itemKey);
        if (item == null) {
            status = "That item cannot be equipped.";
            return;
        }
        if (!item.canEquipAt(actor.level)) {
            status = item.name() + " requires level " + item.minLevel() + ".";
            return;
        }
        if (!actor.hasItem(itemKey)) {
            if (!player.hasItem(itemKey)) {
                status = "Shared pack has no " + item.name() + ".";
                return;
            }
            player.consumeItem(itemKey);
            actor.addItem(itemKey, 1);
        }
        String oldKey = actor.equipment.get(item.slot());
        status = actor.equipItem(itemKey).replace("Equipped", actor.name + " equipped");
        if (oldKey != null && actor != player && actor.consumeItem(oldKey)) {
            player.addItem(oldKey, 1);
            status += " Old gear returned to shared pack.";
        }
    }

    private boolean consumeSharedOrPersonal(Actor actor, String itemKey) {
        if (actor.hasItem(itemKey)) {
            return actor.consumeItem(itemKey);
        }
        return player.consumeItem(itemKey);
    }

    public boolean canAllocateSkill(String skillKey) {
        return canAllocateSkill(player, skillKey);
    }

    public boolean canAllocateSkill(Actor actor, String skillKey) {
        if (actor.skillPoints <= 0) {
            status = "No skill points available.";
            return false;
        }
        SkillNode node = SkillTrees.availableSkillTree(actor.className).get(skillKey);
        if (node == null) {
            status = "That skill belongs to a different class.";
            return false;
        }
        if (actor.skillRank(skillKey) >= node.maxRank()) {
            status = node.name() + " is already mastered.";
            return false;
        }
        for (String required : node.requires()) {
            if (actor.skillRank(required) <= 0) {
                SkillNode requiredNode = SkillTrees.SKILL_TREE.get(required);
                status = "Requires " + (requiredNode == null ? required : requiredNode.name()) + ".";
                return false;
            }
        }
        return true;
    }

    public void allocateSkill(String skillKey) {
        allocateSkill(player, skillKey);
    }

    public void allocatePartySkill(String skillKey) {
        if (mode == GameMode.PARTY) {
            allocateSkill(partyScreenActor(), skillKey);
        }
    }

    public void allocateSkill(Actor actor, String skillKey) {
        SkillNode node = SkillTrees.SKILL_TREE.get(skillKey);
        if (node == null || !canAllocateSkill(actor, skillKey)) {
            return;
        }
        actor.skillPoints--;
        actor.skillAllocations.put(skillKey, actor.skillRank(skillKey) + 1);
        applySkillEffects(actor, node, 1);
        syncSkillAbilities(actor);
        status = actor.name + " learned " + node.name() + ".";
    }

    public void respecSkills() {
        respecSkills(player);
    }

    public void respecPartySkills() {
        if (mode == GameMode.PARTY) {
            respecSkills(partyScreenActor());
        }
    }

    public void respecSkills(Actor actor) {
        int spent = SkillTrees.spent(actor.skillAllocations);
        if (spent <= 0) {
            status = "No allocated skills to respec.";
            return;
        }
        int cost = SkillTrees.respecCost(actor.level, spent);
        if (player.gold < cost) {
            status = "Respec costs " + cost + " gold.";
            return;
        }
        for (var entry : actor.skillAllocations.entrySet()) {
            SkillNode node = SkillTrees.SKILL_TREE.get(entry.getKey());
            if (node != null) {
                for (int i = 0; i < entry.getValue(); i++) {
                    applySkillEffects(actor, node, -1);
                }
            }
        }
        player.gold -= cost;
        actor.skillPoints += spent;
        actor.skillAllocations.clear();
        syncSkillAbilities(actor);
        status = actor.name + "'s skills reset for " + cost + " gold.";
    }

    public boolean move(int dx, int dy) {
        if (mode != GameMode.EXPLORE) {
            return false;
        }
        if (crafting.active()) {
            status = "Busy: " + crafting.progressLabel() + ".";
            return false;
        }
        int nx = playerX + dx;
        int ny = playerY + dy;
        if (!world.isPassable(currentMapId, nx, ny)) {
            CityBuilding building = world.cityBuildingEntryAt(currentMapId, nx, ny, playerX, playerY);
            if (building != null && isSettlementMap(currentMapId)) {
                enterBuildingAt(nx, ny);
                return true;
            }
            status = "Blocked by " + Terrain.name(world.tileAt(currentMapId, nx, ny)) + ".";
            return false;
        }
        DungeonMonsterRuntime dungeonMonster = dungeonMonsterAt(currentMapId, nx, ny);
        if (dungeonMonster != null) {
            playerX = nx;
            playerY = ny;
            startDungeonMonsterBattle(dungeonMonster);
            return true;
        }
        Npc npc = npcAt(currentMapId, nx, ny);
        if (npc != null) {
            status = npc.name() + " is there. Press E to talk.";
            return false;
        }
        QuestObjective blockingObjective = blockingQuestObjectiveAt(currentMapId, nx, ny);
        if (blockingObjective != null) {
            status = blockingObjective.target() + " is there. Press E to engage.";
            return false;
        }
        playerX = nx;
        playerY = ny;
        WorldTransition transition = world.transitionAt(currentMapId, playerX, playerY);
        if (transition != null) {
            applyTransition(transition);
            return true;
        }
        status = world.describe(currentMapId, playerX, playerY);
        maybeStartEncounter();
        return true;
    }

    public void tickWorld() {
        int previousTick = worldTick;
        worldTick += crafting.worldTickAdvance();
        int productionBefore = Math.floorDiv(previousTick, VILLAGE_PRODUCTION_TICKS);
        int productionAfter = Math.floorDiv(worldTick, VILLAGE_PRODUCTION_TICKS);
        for (int i = productionBefore; i < productionAfter; i++) {
            tickVillageProduction();
        }
        if (crafting.active()) {
            String finished = crafting.tick(player);
            if (!finished.isBlank()) {
                status = finished;
            }
            return;
        }
        if (mode != GameMode.EXPLORE) {
            return;
        }
        updateNpcMovement();
        updateDungeonMonsterMovement();
    }

    private void tickVillageProduction() {
        if (villageAllies.isEmpty() || villageStorageUsed() >= villageStorageCapacity()) {
            return;
        }
        Map<String, Integer> produced = new LinkedHashMap<>();
        Map<String, Integer> levelSummary = world.playerVillageBuildingRoleLevels();
        for (Actor ally : stationedAllies()) {
            String buildingKey = buildingAssignmentForAlly(ally.name);
            CityBuilding assignedBuilding = buildingKey.isBlank() ? null : playerVillageBuildingByKey(buildingKey);
            if (assignedBuilding == null) {
                continue;
            }
            VillageManager.WorkerRole role = VillageManager.workerRole(villageRoleFor(ally.name));
            if ("idle".equals(role.id()) || role.resource().isBlank()) {
                continue;
            }
            if (!role.requiredBuildingStyle().isBlank() && world.playerVillageBuildingCount(role.requiredBuildingStyle()) <= 0) {
                continue;
            }
            int assignedLevel = world.playerVillageBuildingLevel(assignedBuilding);
            int levelBonus = Math.max(VillageManager.productionLevelBonus(role.id(), levelSummary), assignedLevel - 1);
            int tileBonus = role.preferredTile() == 'q'
                    ? 0
                    : Math.min(2, countVillageTiles(role.preferredTile()) / 8);
            int professionLevel = role.profession().isBlank() ? 1
                    : ally.professionLevel(role.profession()) + ally.professionPracticeBonus(role.profession());
            int amount = Math.max(1, role.baseAmount() + ally.level / 4 + levelBonus + tileBonus + (professionLevel - 1) / 2);
            int stored = addVillageStorage(role.resource(), amount);
            if (stored > 0) {
                produced.merge(role.resource(), stored, Integer::sum);
            }
            if (!role.profession().isBlank()) {
                ally.gainProfessionXp(role.profession(), 8 + amount);
            }
            addSecondaryVillageYield(role.id(), levelBonus, professionLevel, produced);
            addBuildingUpgradeYield(assignedBuilding.style(), assignedLevel, role.id(), professionLevel, produced);
            if (villageStorageUsed() >= villageStorageCapacity()) {
                break;
            }
        }
        if (!produced.isEmpty()) {
            status = "Village stores gained " + resourceList(produced) + ".";
        }
    }

    private void addSecondaryVillageYield(String roleId, int levelBonus, int professionLevel, Map<String, Integer> produced) {
        if (villageStorageUsed() >= villageStorageCapacity()) {
            return;
        }
        if ("miner".equals(roleId) && random.nextDouble() < 0.25 + levelBonus * 0.12 + professionLevel * 0.025) {
            int stored = addVillageStorage("iron_ore", 1);
            if (stored > 0) {
                produced.merge("iron_ore", stored, Integer::sum);
            }
        } else if ("hunter".equals(roleId)) {
            if (random.nextDouble() < 0.50 + professionLevel * 0.025) {
                int stored = addVillageStorage("skin", 1);
                if (stored > 0) {
                    produced.merge("skin", stored, Integer::sum);
                }
            }
            if (random.nextDouble() < 0.20 + levelBonus * 0.08 + professionLevel * 0.02) {
                String resource = random.nextBoolean() ? "bone" : "horn";
                int stored = addVillageStorage(resource, 1);
                if (stored > 0) {
                    produced.merge(resource, stored, Integer::sum);
                }
            }
        } else if ("forester".equals(roleId)) {
            if (random.nextDouble() < 0.18 + professionLevel * 0.025) {
                String resource = random.nextBoolean() ? "plant_fiber" : "herb_seed";
                int stored = addVillageStorage(resource, 1);
                if (stored > 0) {
                    produced.merge(resource, stored, Integer::sum);
                }
            }
        } else if ("farmer".equals(roleId)) {
            if (random.nextDouble() < 0.22 + professionLevel * 0.03) {
                String resource = professionLevel >= 5 && random.nextBoolean() ? "herb_leaf" : "garden_vegetables";
                int stored = addVillageStorage(resource, 1);
                if (stored > 0) {
                    produced.merge(resource, stored, Integer::sum);
                }
            }
        } else if ("fisher".equals(roleId) && random.nextDouble() < 0.25 + professionLevel * 0.035) {
            String resource = professionLevel >= 6 && random.nextDouble() < 0.35
                    ? "shell_lure"
                    : random.nextBoolean() ? "seashell" : "plant_fiber";
            int stored = addVillageStorage(resource, 1);
            if (stored > 0) {
                produced.merge(resource, stored, Integer::sum);
            }
        } else if ("builder".equals(roleId) && random.nextDouble() < 0.18 + professionLevel * 0.025) {
            String resource = professionLevel >= 5 && random.nextBoolean() ? "coal" : "flint";
            int stored = addVillageStorage(resource, 1);
            if (stored > 0) {
                produced.merge(resource, stored, Integer::sum);
            }
        } else if ("hauler".equals(roleId) && random.nextDouble() < 0.16 + professionLevel * 0.02) {
            String[] resources = {"wood", "stone", "plant_fiber", "flint"};
            String resource = resources[random.nextInt(resources.length)];
            int stored = addVillageStorage(resource, 1);
            if (stored > 0) {
                produced.merge(resource, stored, Integer::sum);
            }
        }
    }

    private void addBuildingUpgradeYield(String style, int level, String roleId, int professionLevel, Map<String, Integer> produced) {
        if (level <= 1 || villageStorageUsed() >= villageStorageCapacity()) {
            return;
        }
        int bonus = Math.max(1, level - 1);
        double chance = 0.18 + bonus * 0.16 + professionLevel * 0.015;
        switch (style) {
            case "warehouse" -> {
                if (random.nextDouble() < chance) {
                    String[] resources = {"wood", "stone", "plant_fiber", "flint"};
                    addVillageStorageYield(resources[random.nextInt(resources.length)], bonus, produced);
                }
            }
            case "forestry_hut" -> {
                if (random.nextDouble() < chance) {
                    addVillageStorageYield("wood", bonus, produced);
                }
                if (level >= 3 && random.nextDouble() < 0.28 + professionLevel * 0.015) {
                    addVillageStorageYield(random.nextBoolean() ? "plant_fiber" : "herb_seed", 1, produced);
                }
            }
            case "mine" -> {
                if (random.nextDouble() < chance) {
                    addVillageStorageYield("stone", bonus, produced);
                }
                if (level >= 3 && random.nextDouble() < 0.24 + professionLevel * 0.015) {
                    addVillageStorageYield(random.nextBoolean() ? "iron_ore" : "coal", 1, produced);
                }
            }
            case "hunting_camp" -> {
                if (random.nextDouble() < chance) {
                    addVillageStorageYield(random.nextBoolean() ? "wild_meat" : "skin", bonus, produced);
                }
                if (level >= 3 && random.nextDouble() < 0.22 + professionLevel * 0.012) {
                    addVillageStorageYield(random.nextBoolean() ? "bone" : "horn", 1, produced);
                }
            }
            case "farmstead", "granary", "garden", "bakery" -> {
                if (random.nextDouble() < chance) {
                    String resource = "bakery".equals(style) ? "trail_rations" : "garden_vegetables";
                    addVillageStorageYield(resource, bonus, produced);
                }
                if (level >= 3 && random.nextDouble() < 0.22 + professionLevel * 0.015) {
                    addVillageStorageYield(random.nextBoolean() ? "herb_leaf" : "herb_seed", 1, produced);
                }
            }
            case "shop", "blacksmith" -> {
                if (random.nextDouble() < chance) {
                    addVillageStorageYield("flint", bonus, produced);
                }
                if (level >= 3 && random.nextDouble() < 0.24 + professionLevel * 0.015) {
                    addVillageStorageYield(random.nextBoolean() ? "iron_ore" : "coal", 1, produced);
                }
            }
            case "fishing_hut" -> {
                if (random.nextDouble() < chance) {
                    addVillageStorageYield("raw_fish", bonus, produced);
                }
                if (level >= 3 && random.nextDouble() < 0.24 + professionLevel * 0.015) {
                    addVillageStorageYield(random.nextBoolean() ? "seashell" : "shell_lure", 1, produced);
                }
            }
            case "apothecary" -> {
                if (random.nextDouble() < chance) {
                    addVillageStorageYield(random.nextBoolean() ? "herb_leaf" : "herb_seed", bonus, produced);
                }
            }
            case "shrine" -> {
                if (level >= 3 && random.nextDouble() < 0.08 + professionLevel * 0.01) {
                    addVillageStorageYield("ember_shard", 1, produced);
                }
            }
            default -> {
                if ("builder".equals(roleId) && random.nextDouble() < chance * 0.5) {
                    addVillageStorageYield(random.nextBoolean() ? "stone" : "wood", 1, produced);
                }
            }
        }
    }

    private void addVillageStorageYield(String resource, int amount, Map<String, Integer> produced) {
        if (villageStorageUsed() >= villageStorageCapacity()) {
            return;
        }
        int stored = addVillageStorage(resource, Math.max(1, amount));
        if (stored > 0) {
            produced.merge(resource, stored, Integer::sum);
        }
    }

    private int countVillageTiles(char tile) {
        int count = 0;
        for (int y = 0; y < world.height(WorldMap.PLAYER_VILLAGE_ID); y++) {
            for (int x = 0; x < world.width(WorldMap.PLAYER_VILLAGE_ID); x++) {
                if (world.tileAt(WorldMap.PLAYER_VILLAGE_ID, x, y) == tile) {
                    count++;
                }
            }
        }
        return count;
    }

    private String resourceList(Map<String, Integer> resources) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            parts.add(entry.getValue() + " " + GameData.itemName(entry.getKey()));
        }
        return String.join(", ", parts);
    }

    private WeatherCondition weightedWeather(int roll, WeatherCondition[] conditions) {
        int index = Math.min(conditions.length - 1, roll * conditions.length / 100);
        return conditions[index];
    }

    private boolean isOutdoorWeatherMap(String mapId) {
        String kind = world.kind(mapId);
        return "overworld".equals(kind) || "city".equals(kind) || "village".equals(kind);
    }

    private char biomeTileAt(String mapId, int x, int y) {
        if (WorldMap.OVERWORLD_ID.equals(mapId)) {
            char tile = world.tileAt(mapId, x, y);
            if (isBiomeTile(tile)) {
                return tile;
            }
            return nearbyBiomeTile(mapId, x, y, 'g');
        }
        if (mapId.contains("snowrest")) {
            return 'n';
        }
        if (mapId.contains("dunewick")) {
            return 's';
        }
        if (mapId.contains("mireford")) {
            return 'v';
        }
        if (mapId.contains("highwall")) {
            return 'm';
        }
        if (mapId.contains("belltower")) {
            return 'v';
        }
        if (mapId.contains("sanctum")) {
            return 'b';
        }
        if (mapId.contains("archive")) {
            return 'f';
        }
        if ("dungeon".equals(world.kind(mapId))) {
            return 'd';
        }
        char tile = world.tileAt(mapId, x, y);
        return isBiomeTile(tile) ? tile : 'g';
    }

    private char nearbyBiomeTile(String mapId, int x, int y, char fallback) {
        char best = fallback;
        int bestScore = -1;
        char[] candidates = {'g', 'f', 's', 'n', 'v', 'b', 'm', 'q', 'w'};
        for (char candidate : candidates) {
            int score = 0;
            for (int oy = -4; oy <= 4; oy++) {
                for (int ox = -4; ox <= 4; ox++) {
                    if (world.tileAt(mapId, x + ox, y + oy) == candidate) {
                        score += Math.max(1, 5 - Math.max(Math.abs(ox), Math.abs(oy)));
                    }
                }
            }
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private boolean isBiomeTile(char tile) {
        return tile == 'g' || tile == 'f' || tile == 's' || tile == 'n'
                || tile == 'v' || tile == 'b' || tile == 'm' || tile == 'q' || tile == 'w';
    }

    public void resetNpcRuntime() {
        npcRuntime.clear();
    }

    public void resetDungeonMonsterRuntime() {
        dungeonMonsterRuntime.clear();
        defeatedDungeonMonsters.clear();
        activeDungeonMonster = null;
    }

    public TilePoint npcPosition(Npc npc) {
        NpcRuntime runtime = runtimeFor(npc);
        return new TilePoint(runtime.x, runtime.y);
    }

    public List<NpcMotion> npcMotionsForMap(String mapId) {
        List<NpcMotion> motions = new ArrayList<>();
        for (Npc npc : npcsForMap(mapId)) {
            NpcRuntime runtime = runtimeFor(npc);
            motions.add(new NpcMotion(npc, runtime.x, runtime.y, runtime.fromX, runtime.fromY, runtime.moveStartTick, runtime.facingDx, runtime.facingDy));
        }
        return motions;
    }

    public Npc npcAt(String mapId, int x, int y) {
        for (Npc npc : npcsForMap(mapId)) {
            TilePoint position = npcPosition(npc);
            if (position.x() == x && position.y() == y) {
                return npc;
            }
        }
        return null;
    }

    public List<DungeonMonsterMotion> dungeonMonsterMotionsForMap(String mapId) {
        if (!"dungeon".equals(world.kind(mapId))) {
            return List.of();
        }
        List<DungeonMonsterMotion> motions = new ArrayList<>();
        for (DungeonMonsterRuntime monster : dungeonMonstersForMap(mapId)) {
            if (defeatedDungeonMonsters.contains(monster.id)) {
                continue;
            }
            motions.add(new DungeonMonsterMotion(
                    monster.spec,
                    monster.boss,
                    monster.x,
                    monster.y,
                    monster.fromX,
                    monster.fromY,
                    monster.moveStartTick,
                    monster.facingDx,
                    monster.facingDy
            ));
        }
        return motions;
    }

    private DungeonMonsterRuntime dungeonMonsterAt(String mapId, int x, int y) {
        if (!"dungeon".equals(world.kind(mapId))) {
            return null;
        }
        for (DungeonMonsterRuntime monster : dungeonMonstersForMap(mapId)) {
            if (!defeatedDungeonMonsters.contains(monster.id) && monster.x == x && monster.y == y) {
                return monster;
            }
        }
        return null;
    }

    private List<DungeonMonsterRuntime> dungeonMonstersForMap(String mapId) {
        if (!"dungeon".equals(world.kind(mapId))) {
            return List.of();
        }
        return dungeonMonsterRuntime.computeIfAbsent(mapId, this::generateDungeonMonsters);
    }

    private List<DungeonMonsterRuntime> generateDungeonMonsters(String mapId) {
        List<TilePoint> candidates = new ArrayList<>();
        for (int y = 1; y < world.height(mapId) - 1; y++) {
            for (int x = 1; x < world.width(mapId) - 1; x++) {
                char tile = world.tileAt(mapId, x, y);
                if (!isDungeonFloor(tile) || world.transitionAt(mapId, x, y) != null || (x <= 3 && Math.abs(y - 10) <= 2)) {
                    continue;
                }
                if (!world.propsAt(mapId, x, y).isEmpty()) {
                    continue;
                }
                candidates.add(new TilePoint(x, y));
            }
        }
        Random seeded = new Random(mapId.hashCode() * 31L + 1701L);
        List<DungeonMonsterRuntime> monsters = new ArrayList<>();
        TilePoint bossPoint = farthestDungeonPoint(candidates, new TilePoint(2, 10));
        if (bossPoint != null) {
            candidates.remove(bossPoint);
            String bossKey = chooseDungeonBoss(mapId);
            monsters.add(new DungeonMonsterRuntime(mapId + ":boss", GameData.MONSTERS.getOrDefault(bossKey, GameData.MONSTERS.get("wraith")), true, bossPoint.x(), bossPoint.y(), seeded.nextInt(80)));
        }
        int count = Math.min(candidates.size(), 5 + Math.min(3, dungeonDepth(mapId)));
        for (int i = 0; i < count; i++) {
            int index = seeded.nextInt(candidates.size());
            TilePoint point = candidates.remove(index);
            String key = chooseDungeonRoamer(mapId, seeded);
            monsters.add(new DungeonMonsterRuntime(mapId + ":" + i, GameData.MONSTERS.getOrDefault(key, GameData.MONSTERS.get("skeleton")), false, point.x(), point.y(), seeded.nextInt(80)));
        }
        return monsters;
    }

    private TilePoint farthestDungeonPoint(List<TilePoint> points, TilePoint from) {
        TilePoint best = null;
        int bestDistance = -1;
        for (TilePoint point : points) {
            int distance = Math.abs(point.x() - from.x()) + Math.abs(point.y() - from.y());
            if (distance > bestDistance) {
                best = point;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean isDungeonFloor(char tile) {
        return tile == 'd' || tile == 'D' || tile == 'F' || tile == 'M' || tile == 'R' || tile == 'S' || tile == 'L';
    }

    private int dungeonDepth(String mapId) {
        if (mapId.endsWith("_2") || mapId.contains("deep")) {
            return 2;
        }
        return 1;
    }

    private String chooseDungeonBoss(String mapId) {
        if (mapId.contains("redcap")) {
            return "goblin_warlord";
        }
        if (mapId.contains("crowhook")) {
            return "bandit_captain";
        }
        if (mapId.contains("frost")) {
            return dungeonDepth(mapId) > 1 ? "frost_troll" : "ice_golem";
        }
        if (mapId.contains("mire")) {
            return dungeonDepth(mapId) > 1 ? "swamp_troll" : "crypt_revenant";
        }
        if (mapId.contains("blackvault") || dungeonDepth(mapId) > 1) {
            return mapId.contains("blackvault") ? "elder_dragon" : "elder_wraith";
        }
        return "bone_knight";
    }

    private String chooseDungeonRoamer(String mapId, Random seeded) {
        List<String> pool;
        if (mapId.contains("redcap")) {
            pool = List.of("goblin_scout", "goblin_archer", "goblin_trapper", "goblin_skirmisher", "goblin_shaman");
        } else if (mapId.contains("crowhook")) {
            pool = List.of("bandit_cutthroat", "bandit_archer", "goblin_skirmisher", "orc_raider", "orc_shieldbearer", "bandit_captain");
        } else if (mapId.contains("frost")) {
            pool = List.of("crypt_bat", "frost_wolf", "skeleton", "mountain_drake", "frost_troll", "ice_golem");
        } else if (mapId.contains("mire")) {
            pool = List.of("slime", "spider", "reed_serpent", "bog_beast", "marsh_drake", "swamp_troll", "wraith");
        } else if (mapId.contains("blackvault")) {
            pool = List.of("skeleton", "wraith", "orc_berserker", "ash_scorpion", "stone_giant", "elder_dragon");
        } else {
            pool = List.of("crypt_bat", "skeleton", "spider", "wraith", "bandit_cutthroat", "orc_raider");
        }
        return pool.get(seeded.nextInt(pool.size()));
    }

    private void startDungeonMonsterBattle(DungeonMonsterRuntime monster) {
        List<GameData.MonsterSpec> specs = new ArrayList<>();
        specs.add(monster.spec);
        if (monster.boss) {
            String support = switch (monster.spec.species()) {
                case "bandit" -> "bandit_cutthroat";
                case "goblin" -> "goblin_skirmisher";
                case "orc" -> "orc_raider";
                default -> "crypt_bat";
            };
            specs.add(GameData.MONSTERS.getOrDefault(support, GameData.MONSTERS.get("crypt_bat")));
            if (player.level >= 4 || dungeonDepth(currentMapId) > 1) {
                specs.add(GameData.MONSTERS.getOrDefault("skeleton", GameData.MONSTERS.get("skeleton")));
            }
        }
        activeDungeonMonster = monster;
        battle = new Battle(player, activeAllies(), specs, random, world.tileAt(currentMapId, monster.x, monster.y), world.kind(currentMapId));
        mode = GameMode.BATTLE;
        status = monster.spec.name() + " attacks!";
    }

    private void clearDefeatedDungeonMonster() {
        if (activeDungeonMonster == null) {
            return;
        }
        defeatedDungeonMonsters.add(activeDungeonMonster.id);
        List<DungeonMonsterRuntime> monsters = dungeonMonsterRuntime.get(currentMapId);
        if (monsters != null) {
            monsters.removeIf(monster -> monster.id.equals(activeDungeonMonster.id));
        }
        activeDungeonMonster = null;
    }

    public void maybeStartEncounter() {
        char tile = world.tileAt(currentMapId, playerX, playerY);
        String kind = world.kind(currentMapId);
        if (tile == 'r' || tile == 'B' || tile == 'c' || tile == 'u' || "city".equals(kind) || "village".equals(kind) || "interior".equals(kind)) {
            return;
        }
        double chance = "dungeon".equals(kind) ? config.dungeonEncounterChance : tile == 'd' ? config.dungeonEntranceEncounterChance : config.wildEncounterChance;
        if (random.nextDouble() >= chance) {
            return;
        }
        char monsterTile = "dungeon".equals(kind) ? 'd' : tile;
        List<GameData.MonsterSpec> specs = chooseMonsterGroup(monsterTile).stream()
                .map(key -> GameData.MONSTERS.getOrDefault(key, GameData.MONSTERS.get("slime")))
                .toList();
        battle = new Battle(player, activeAllies(), specs, random, tile, kind);
        mode = GameMode.BATTLE;
        status = "Battle!";
    }

    public List<QuestObjective> activeQuestObjectives() {
        List<QuestObjective> objectives = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (!quest.hasWorldObjective()) {
                continue;
            }
            int count = quest.objectiveKind == Quest.ObjectiveKind.DEFEAT ? 1 : quest.needed - quest.progress;
            for (int i = 0; i < count; i++) {
                String asset = quest.objectiveAsset != null && !quest.objectiveAsset.isBlank()
                        ? quest.objectiveAsset
                        : quest.monsterKey != null ? quest.monsterKey : "icon_chest";
                int variant = quest.objectiveKind == Quest.ObjectiveKind.DEFEAT ? 0 : quest.progress + i;
                TilePoint point = world.objectivePoint(quest.objectiveLocationKind, quest.objectiveLocationIndex, variant, asset);
                objectives.add(new QuestObjective(
                        quest.id,
                        quest.title,
                        quest.target,
                        quest.objectiveMapId,
                        point.x(),
                        point.y(),
                        asset,
                        quest.objectiveKind,
                        quest.monsterKey,
                        quest.objectiveKind == Quest.ObjectiveKind.DEFEAT
                ));
            }
        }
        return objectives;
    }

    public List<QuestInteractible> activeQuestInteractibles(String mapId) {
        List<QuestInteractible> interactibles = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (!quest.hasWorldObjective() || quest.objectiveKind != Quest.ObjectiveKind.GATHER || quest.objectiveAsset == null || quest.objectiveAsset.isBlank()) {
                continue;
            }
            if (!quest.objectiveMapId.equals(mapId)) {
                continue;
            }
            for (WorldProp prop : world.props(mapId)) {
                if (!quest.objectiveAsset.equals(prop.asset())) {
                    continue;
                }
                String locationKind = world.locationKindAt(mapId, prop.x(), prop.y());
                if (quest.objectiveLocationKind != null && !quest.objectiveLocationKind.equals(locationKind)) {
                    continue;
                }
                if (harvestedQuestResources.contains(resourceKey(quest.id, mapId, prop.x(), prop.y(), prop.asset()))) {
                    continue;
                }
                interactibles.add(new QuestInteractible(
                        quest.id,
                        quest.title,
                        quest.target,
                        mapId,
                        prop.x(),
                        prop.y(),
                        prop.asset()
                ));
            }
        }
        return interactibles;
    }

    public QuestObjective questObjectiveAt(String mapId, int x, int y) {
        for (QuestObjective objective : activeQuestObjectives()) {
            if (objective.mapId().equals(mapId) && objective.x() == x && objective.y() == y) {
                return objective;
            }
        }
        return null;
    }

    public QuestObjective blockingQuestObjectiveAt(String mapId, int x, int y) {
        QuestObjective objective = questObjectiveAt(mapId, x, y);
        return objective != null && objective.blocking() ? objective : null;
    }

    public void battleAttack() {
        if (battle == null) {
            return;
        }
        battle.playerAttack();
        updateAfterBattleAction();
    }

    public void battleAbility(int index) {
        if (battle == null) {
            return;
        }
        battle.useAbility(index);
        updateAfterBattleAction();
    }

    public void tickBattle() {
        if (mode != GameMode.BATTLE || battle == null) {
            return;
        }
        battle.tick();
        updateAfterBattleAction();
    }

    public void selectBattleEnemy(int index) {
        if (battle == null || battle.finished || !battle.canAcceptInput()) {
            return;
        }
        battle.selectEnemy(index);
        Actor target = battle.selectedEnemy();
        status = target == null ? "No enemy target." : "Targeting " + target.name + ".";
    }

    public void selectBattlePartyMember(int index) {
        if (battle == null || battle.finished || !battle.canAcceptInput()) {
            return;
        }
        battle.selectPartyMember(index);
        Actor target = battle.selectedPartyMember();
        status = target == null ? "No ally target." : "Ally target: " + target.name + ".";
    }

    public void cycleBattleEnemyTarget(int direction) {
        if (battle == null || battle.finished || !battle.canAcceptInput()) {
            return;
        }
        battle.cycleEnemyTarget(direction);
        Actor target = battle.selectedEnemy();
        status = target == null ? "No enemy target." : "Targeting " + target.name + ".";
    }

    public void cycleBattlePartyTarget(int direction) {
        if (battle == null || battle.finished || !battle.canAcceptInput()) {
            return;
        }
        battle.cyclePartyTarget(direction);
        Actor target = battle.selectedPartyMember();
        status = target == null ? "No ally target." : "Ally target: " + target.name + ".";
    }

    public void revive() {
        player.healFull();
        for (Actor ally : allies) {
            ally.healFull();
        }
        currentMapId = WorldMap.OVERWORLD_ID;
        playerX = WorldMap.START_POSITION.x();
        playerY = WorldMap.START_POSITION.y();
        battle = null;
        activeDungeonMonster = null;
        mode = GameMode.EXPLORE;
        status = "Revived at Oathstead Camp.";
    }

    public void leaveFinishedBattle() {
        if (battle == null || !battle.finished || !battle.victory) {
            return;
        }
        battle = null;
        mode = GameMode.EXPLORE;
        if (!refreshPlayerVillageGrowth()) {
            status = world.describe(currentMapId, playerX, playerY);
        }
    }

    public void applyTransition(WorldTransition transition) {
        currentMapId = transition.targetMapId();
        playerX = transition.targetX();
        playerY = transition.targetY();
        status = transition.message();
    }

    private boolean isSettlementMap(String mapId) {
        String kind = world.kind(mapId);
        return "city".equals(kind) || "village".equals(kind);
    }

    private TilePoint adjacentBuildingEntry() {
        if (isSettlementMap(currentMapId)) {
            CityBuilding building = world.cityBuildingEntryAt(currentMapId, playerX, playerY - 1, playerX, playerY);
            if (building != null) {
                return new TilePoint(playerX, playerY - 1);
            }
            return null;
        }
        return null;
    }

    private void enterBuildingAt(int wx, int wy) {
        String houseMapId = world.ensureHouseInterior(currentMapId, wx, wy, playerX, playerY);
        TilePoint entry = world.interiorEntryPoint(houseMapId);
        currentMapId = houseMapId;
        playerX = entry.x();
        playerY = entry.y();
        status = "You step inside.";
    }

    private QuestObjective questObjectiveNearPlayer() {
        QuestObjective best = null;
        int bestScore = Integer.MAX_VALUE;
        for (QuestObjective objective : activeQuestObjectives()) {
            if (!objective.mapId().equals(currentMapId)) {
                continue;
            }
            int distance = Math.abs(objective.x() - playerX) + Math.abs(objective.y() - playerY);
            int reach = 1;
            if (distance > reach) {
                continue;
            }
            boolean assetMatchesTile = world.propsAt(objective.mapId(), objective.x(), objective.y()).stream()
                    .anyMatch(prop -> prop.asset().equals(objective.asset()));
            int kindPriority = objective.kind() == Quest.ObjectiveKind.VISIT ? -2 : objective.kind() == Quest.ObjectiveKind.GATHER ? -1 : 0;
            int score = distance * 10 + (assetMatchesTile ? 0 : 1) + kindPriority;
            if (score < bestScore) {
                best = objective;
                bestScore = score;
            }
        }
        return best;
    }

    private QuestInteractible questInteractibleNearPlayer() {
        QuestInteractible best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (QuestInteractible interactible : activeQuestInteractibles(currentMapId)) {
            int distance = Math.abs(interactible.x() - playerX) + Math.abs(interactible.y() - playerY);
            if (distance <= 1 && distance < bestDistance) {
                best = interactible;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void interactQuestInteractible(QuestInteractible interactible) {
        Quest quest = quests.get(interactible.questId());
        if (quest == null || quest.completed || quest.objectiveKind != Quest.ObjectiveKind.GATHER) {
            return;
        }
        String resourceKey = resourceKey(quest.id, interactible.mapId(), interactible.x(), interactible.y(), interactible.asset());
        if (harvestedQuestResources.contains(resourceKey)) {
            status = "Already gathered from here.";
            return;
        }
        int oldProgress = quest.progress;
        quest.recordGather(interactible.target());
        if (quest.progress > oldProgress) {
            harvestedQuestResources.add(resourceKey);
            status = "Gathered " + interactible.target() + ". " + quest.title + ": " + quest.progress + "/" + quest.needed + ".";
            if (quest.ready()) {
                status += " " + quest.readyDialog;
            }
        }
    }

    private void interactQuestObjective(QuestObjective objective) {
        Quest quest = quests.get(objective.questId());
        if (quest == null || quest.completed) {
            return;
        }
        if (objective.kind() == Quest.ObjectiveKind.GATHER) {
            recordMarkedObjective(quest, objective, "Gathered");
            return;
        }
        if (objective.kind() == Quest.ObjectiveKind.VISIT) {
            recordMarkedObjective(quest, objective, "Inspected");
            return;
        }
        String monsterKey = objective.monsterKey() == null || objective.monsterKey().isBlank() ? "goblin" : objective.monsterKey();
        GameData.MonsterSpec spec = GameData.MONSTERS.getOrDefault(monsterKey, GameData.MONSTERS.get("goblin"));
        battle = new Battle(player, activeAllies(), List.of(spec), random, world.tileAt(currentMapId, objective.x(), objective.y()), world.kind(currentMapId));
        mode = GameMode.BATTLE;
        status = objective.target() + " answers the challenge.";
    }

    private void updateAfterBattleAction() {
        if (battle != null && battle.finished && battle.victory) {
            if (!battle.questRecorded) {
                for (String defeatedName : battle.defeatedMonsterNames()) {
                    recordQuestProgress(defeatedName);
                }
                battle.questRecorded = true;
            }
            if (!battle.lootGranted) {
                String loot = CraftingSystem.grantLoot(player, battle.monsterSpecs, random);
                if (!loot.isBlank()) {
                    battle.addLog("Loot: " + loot + ".");
                }
                battle.lootGranted = true;
            }
            clearDefeatedDungeonMonster();
            status = "Victory. Press Enter to continue.";
        } else if (battle != null && battle.finished) {
            status = "Defeated. Press R to revive.";
        } else if (battle != null && battle.isAnimating()) {
            String animationStatus = battle.animationStatus();
            status = animationStatus.isBlank() ? "Battle!" : animationStatus;
        } else if (battle != null) {
            Actor actor = battle.activeActor();
            status = actor == null ? "Battle!" : actor.name + "'s turn.";
        }
    }

    private String resourceKey(String questId, String mapId, int x, int y, String asset) {
        return questId + ":" + mapId + ":" + x + ":" + y + ":" + asset;
    }

    private void recordMarkedObjective(Quest quest, QuestObjective objective, String verb) {
        String resourceKey = resourceKey(quest.id, objective.mapId(), objective.x(), objective.y(), objective.asset());
        if (harvestedQuestResources.contains(resourceKey) || resourceAlreadyGatheredForQuest(quest, objective.mapId(), objective.x(), objective.y())) {
            status = "Already handled this objective.";
            return;
        }
        int oldProgress = quest.progress;
        if (objective.kind() == Quest.ObjectiveKind.GATHER) {
            quest.recordGather(objective.target());
        } else if (objective.kind() == Quest.ObjectiveKind.VISIT) {
            quest.recordVisit(objective.target());
        }
        if (quest.progress > oldProgress) {
            harvestedQuestResources.add(resourceKey);
            markGatheredResourcesAt(quest, objective.mapId(), objective.x(), objective.y());
            status = verb + " " + objective.target() + ". " + quest.title + ": " + quest.progress + "/" + quest.needed + ".";
            if (quest.ready()) {
                status += " " + quest.readyDialog;
            }
        }
    }

    private boolean resourceAlreadyGatheredForQuest(Quest quest, String mapId, int x, int y) {
        if (quest.objectiveAsset == null || quest.objectiveAsset.isBlank()) {
            return false;
        }
        for (WorldProp prop : world.propsAt(mapId, x, y)) {
            if (quest.objectiveAsset.equals(prop.asset())
                    && harvestedQuestResources.contains(resourceKey(quest.id, mapId, x, y, prop.asset()))) {
                return true;
            }
        }
        return false;
    }

    private void markGatheredResourcesAt(Quest quest, String mapId, int x, int y) {
        if (quest.objectiveAsset == null || quest.objectiveAsset.isBlank()) {
            return;
        }
        for (WorldProp prop : world.propsAt(mapId, x, y)) {
            if (quest.objectiveAsset.equals(prop.asset())) {
                harvestedQuestResources.add(resourceKey(quest.id, mapId, x, y, prop.asset()));
            }
        }
    }

    public void syncSkillAbilities() {
        syncSkillAbilities(player);
    }

    public void syncAllSkillAbilities() {
        syncSkillAbilities(player);
        for (Actor ally : allies) {
            syncSkillAbilities(ally);
        }
    }

    public void syncSkillAbilities(Actor actor) {
        actor.abilities.removeIf(ability -> SkillTrees.SKILL_ABILITY_NAMES.contains(ability.name()));
        Map<String, SkillNode> availableTree = SkillTrees.availableSkillTree(actor.className);
        for (SkillNode node : availableTree.values()) {
            Ability ability = node.ability();
            if (ability != null && actor.skillRank(node.id()) > 0 && !actor.hasAbility(ability.name())) {
                actor.abilities.add(ability);
            }
        }
    }

    private void applySkillEffects(SkillNode node, int direction) {
        applySkillEffects(player, node, direction);
    }

    private void applySkillEffects(Actor actor, SkillNode node, int direction) {
        double hpRatio = actor.hp / (double) Math.max(1, actor.maxHp);
        double mpRatio = actor.maxMp <= 0 ? 0.0 : actor.mp / (double) actor.maxMp;
        for (var entry : node.effects().entrySet()) {
            int delta = entry.getValue() * direction;
            switch (entry.getKey()) {
                case "max_hp" -> actor.maxHp = Math.max(1, actor.maxHp + delta);
                case "max_mp" -> actor.maxMp = Math.max(0, actor.maxMp + delta);
                case "attack" -> actor.attack = Math.max(1, actor.attack + delta);
                case "defense" -> actor.defense = Math.max(0, actor.defense + delta);
                default -> {
                }
            }
        }
        actor.hp = Math.max(1, Math.min(actor.maxHp, (int) Math.round(actor.maxHp * hpRatio)));
        actor.mp = Math.max(0, Math.min(actor.maxMp, (int) Math.round(actor.maxMp * mpRatio)));
    }

    private void recordQuestProgress(String defeatedTarget) {
        for (Quest quest : quests.values()) {
            int oldProgress = quest.progress;
            quest.record(defeatedTarget);
            if (quest.progress > oldProgress) {
                status = quest.title + ": " + quest.progress + "/" + quest.needed + ".";
                if (quest.ready()) {
                    status += " " + quest.readyDialog;
                }
            }
        }
    }

    private void updateDungeonMonsterMovement() {
        if (!"dungeon".equals(world.kind(currentMapId))) {
            return;
        }
        for (DungeonMonsterRuntime monster : dungeonMonstersForMap(currentMapId)) {
            if (defeatedDungeonMonsters.contains(monster.id)
                    || worldTick - monster.moveStartTick < DungeonMonsterMotion.MOVE_TICKS
                    || worldTick < monster.nextThinkTick) {
                continue;
            }
            monster.nextThinkTick = worldTick + (monster.boss ? 24 + random.nextInt(26) : 16 + random.nextInt(22));
            int distanceToPlayer = Math.abs(playerX - monster.x) + Math.abs(playerY - monster.y);
            if (distanceToPlayer > 4 && random.nextDouble() < (monster.boss ? 0.48 : 0.24)) {
                continue;
            }
            for (int[] direction : dungeonMonsterDirections(monster, distanceToPlayer)) {
                int nx = monster.x + direction[0];
                int ny = monster.y + direction[1];
                if (nx == playerX && ny == playerY) {
                    moveDungeonMonster(monster, nx, ny, direction);
                    startDungeonMonsterBattle(monster);
                    return;
                }
                if (!canDungeonMonsterMoveTo(monster, nx, ny)) {
                    continue;
                }
                moveDungeonMonster(monster, nx, ny, direction);
                break;
            }
        }
    }

    private List<int[]> dungeonMonsterDirections(DungeonMonsterRuntime monster, int distanceToPlayer) {
        int[][] baseDirections = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        List<int[]> directions = new ArrayList<>();
        if (distanceToPlayer <= (monster.boss ? 5 : 7)) {
            int dx = Integer.compare(playerX, monster.x);
            int dy = Integer.compare(playerY, monster.y);
            if (Math.abs(playerX - monster.x) >= Math.abs(playerY - monster.y)) {
                addNpcDirection(directions, dx, 0);
                addNpcDirection(directions, 0, dy);
            } else {
                addNpcDirection(directions, 0, dy);
                addNpcDirection(directions, dx, 0);
            }
        }
        int start = random.nextInt(baseDirections.length);
        for (int i = 0; i < baseDirections.length; i++) {
            int[] direction = baseDirections[(start + i) % baseDirections.length];
            addNpcDirection(directions, direction[0], direction[1]);
        }
        return directions;
    }

    private void moveDungeonMonster(DungeonMonsterRuntime monster, int x, int y, int[] direction) {
        monster.fromX = monster.x;
        monster.fromY = monster.y;
        monster.x = x;
        monster.y = y;
        monster.facingDx = direction[0];
        monster.facingDy = direction[1];
        monster.moveStartTick = worldTick;
    }

    private boolean canDungeonMonsterMoveTo(DungeonMonsterRuntime monster, int x, int y) {
        if (!isDungeonFloor(world.tileAt(currentMapId, x, y))) {
            return false;
        }
        int roamRadius = monster.boss ? 6 : 9;
        if (Math.abs(x - monster.homeX) + Math.abs(y - monster.homeY) > roamRadius) {
            return false;
        }
        if (world.transitionAt(currentMapId, x, y) != null) {
            return false;
        }
        for (DungeonMonsterRuntime other : dungeonMonstersForMap(currentMapId)) {
            if (other == monster || defeatedDungeonMonsters.contains(other.id)) {
                continue;
            }
            if (other.x == x && other.y == y) {
                return false;
            }
        }
        return true;
    }

    private void updateNpcMovement() {
        WeatherCondition weather = currentWeather();
        int minutes = timeOfDayMinutes();
        int day = dayNumber();
        for (Npc npc : npcsForMap(currentMapId)) {
            NpcRuntime runtime = runtimeFor(npc);
            AmbientNpcAi.Routine routine = AmbientNpcAi.routineFor(npc, world, currentMapId, minutes, weather, day);
            if (worldTick - runtime.moveStartTick < NpcMotion.MOVE_TICKS || worldTick < runtime.nextThinkTick) {
                continue;
            }
            runtime.nextThinkTick = worldTick + routine.nextThinkDelay(random);
            int targetDistance = Math.abs(runtime.x - routine.target().x()) + Math.abs(runtime.y - routine.target().y());
            if (targetDistance <= routine.roamRadius() && random.nextDouble() < routine.idleChance()) {
                continue;
            }
            for (int[] direction : npcMoveDirections(runtime, routine)) {
                int nx = runtime.x + direction[0];
                int ny = runtime.y + direction[1];
                if (!canNpcMoveTo(npc, runtime, nx, ny, routine.maxDistanceFromHome())) {
                    continue;
                }
                runtime.fromX = runtime.x;
                runtime.fromY = runtime.y;
                runtime.x = nx;
                runtime.y = ny;
                runtime.facingDx = direction[0];
                runtime.facingDy = direction[1];
                runtime.moveStartTick = worldTick;
                break;
            }
        }
    }

    private List<int[]> npcMoveDirections(NpcRuntime runtime, AmbientNpcAi.Routine routine) {
        int[][] baseDirections = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        List<int[]> directions = new ArrayList<>();
        int distance = Math.abs(runtime.x - routine.target().x()) + Math.abs(runtime.y - routine.target().y());
        if (distance > routine.roamRadius()) {
            int dx = Integer.compare(routine.target().x(), runtime.x);
            int dy = Integer.compare(routine.target().y(), runtime.y);
            if (Math.abs(routine.target().x() - runtime.x) >= Math.abs(routine.target().y() - runtime.y)) {
                addNpcDirection(directions, dx, 0);
                addNpcDirection(directions, 0, dy);
            } else {
                addNpcDirection(directions, 0, dy);
                addNpcDirection(directions, dx, 0);
            }
            addNpcDirection(directions, -dx, 0);
            addNpcDirection(directions, 0, -dy);
        }
        int start = random.nextInt(baseDirections.length);
        for (int i = 0; i < baseDirections.length; i++) {
            int[] direction = baseDirections[(start + i) % baseDirections.length];
            addNpcDirection(directions, direction[0], direction[1]);
        }
        return directions;
    }

    private void addNpcDirection(List<int[]> directions, int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return;
        }
        for (int[] direction : directions) {
            if (direction[0] == dx && direction[1] == dy) {
                return;
            }
        }
        directions.add(new int[]{dx, dy});
    }

    private boolean canNpcMoveTo(Npc npc, NpcRuntime runtime, int x, int y, int maxDistanceFromHome) {
        if (!world.isPassable(npc.mapId(), x, y)) {
            return false;
        }
        if (Math.abs(x - runtime.homeX) + Math.abs(y - runtime.homeY) > maxDistanceFromHome) {
            return false;
        }
        if (npc.mapId().equals(currentMapId) && x == playerX && y == playerY) {
            return false;
        }
        for (Npc otherNpc : npcsForMap(npc.mapId())) {
            if (otherNpc.equals(npc)) {
                continue;
            }
            TilePoint otherPosition = npcPosition(otherNpc);
            if (otherPosition.x() == x && otherPosition.y() == y) {
                return false;
            }
        }
        return true;
    }

    private List<Npc> npcsForMap(String mapId) {
        List<Npc> npcs = new ArrayList<>();
        for (Npc npc : GameData.NPCS) {
            if (npc.mapId().equals(mapId)) {
                npcs.add(npc);
            }
        }
        npcs.addAll(world.npcs(mapId));
        if (WorldMap.PLAYER_VILLAGE_ID.equals(mapId)) {
            addAssignedVillageNpcs(npcs);
        } else if (mapId.startsWith("house_" + WorldMap.PLAYER_VILLAGE_ID + "_")) {
            addAssignedInteriorNpc(npcs, mapId);
        }
        return npcs;
    }

    private void addAssignedVillageNpcs(List<Npc> npcs) {
        if (!isDaytime()) {
            return;
        }
        for (Map.Entry<String, String> entry : villageBuildingAssignments.entrySet()) {
            CityBuilding building = playerVillageBuildingByKey(entry.getKey());
            Actor ally = allyByName(entry.getValue());
            if (building == null || ally == null || !villageAllies.contains(ally.name)) {
                continue;
            }
            TilePoint home = buildingNpcHome(building);
            npcs.add(assignedCompanionNpc(ally, WorldMap.PLAYER_VILLAGE_ID, home.x(), home.y(), building, false));
        }
    }

    private void addAssignedInteriorNpc(List<Npc> npcs, String mapId) {
        if (isDaytime()) {
            return;
        }
        CityBuilding building = playerVillageBuildingForInterior(mapId);
        if (building == null) {
            return;
        }
        Actor ally = allyByName(villageBuildingAssignments.getOrDefault(building.key(), ""));
        if (ally == null || !villageAllies.contains(ally.name)) {
            return;
        }
        TilePoint home = world.interiorEntryPoint(mapId);
        npcs.add(assignedCompanionNpc(ally, mapId, home.x(), Math.max(1, home.y() - 2), building, true));
    }

    private Npc assignedCompanionNpc(Actor ally, String mapId, int x, int y, CityBuilding building, boolean inside) {
        String roleId = villageRoleFor(ally.name);
        VillageManager.WorkerRole role = VillageManager.workerRole(roleId);
        List<String> dialog = new ArrayList<>();
        dialog.add(ally.name + ": " + (inside ? "Off shift inside " : "Working at ")
                + VillageManager.buildingLabel(building.style()) + ". Role: " + role.label() + ".");
        dialog.add("Abilities: " + abilityPreview(ally));
        dialog.add("Skills: " + professionPreview(ally));
        dialog.add("Pack: " + actorInventoryPreview(ally));
        dialog.add("Work: " + role.description());
        return new Npc(mapId, ally.name, ally.sprite, x, y, dialog, null, null, null, 0, ally.professionXp);
    }

    private TilePoint buildingNpcHome(CityBuilding building) {
        List<TilePoint> doors = world.cityBuildingDoorTiles(building);
        for (TilePoint door : doors) {
            int[][] spots = {{0, 1}, {-1, 1}, {1, 1}, {-1, 0}, {1, 0}, {0, 2}};
            for (int[] spot : spots) {
                int x = door.x() + spot[0];
                int y = door.y() + spot[1];
                if (world.isPassable(WorldMap.PLAYER_VILLAGE_ID, x, y)) {
                    return new TilePoint(x, y);
                }
            }
        }
        return new TilePoint(building.x1(), Math.min(world.height(WorldMap.PLAYER_VILLAGE_ID) - 2, building.y2() + 1));
    }

    private CityBuilding playerVillageBuildingForInterior(String mapId) {
        String prefix = "house_" + WorldMap.PLAYER_VILLAGE_ID + "_";
        if (!mapId.startsWith(prefix)) {
            return null;
        }
        String[] parts = mapId.substring(prefix.length()).split("_");
        if (parts.length < 2) {
            return null;
        }
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            return world.cityBuildingAt(WorldMap.PLAYER_VILLAGE_ID, x, y);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String professionPreview(Actor actor) {
        List<String> parts = new ArrayList<>();
        for (Profession profession : Profession.ALL) {
            int level = actor.professionLevel(profession.id());
            if (level > 1) {
                parts.add(profession.label() + " " + level);
            }
        }
        return parts.isEmpty() ? "no trained profession yet" : String.join(", ", parts);
    }

    private String abilityPreview(Actor actor) {
        if (actor.abilities.isEmpty()) {
            return "basic field training";
        }
        List<String> parts = new ArrayList<>();
        for (Ability ability : actor.abilities) {
            parts.add(ability.name());
            if (parts.size() >= 4) {
                break;
            }
        }
        return String.join(", ", parts);
    }

    private String actorInventoryPreview(Actor actor) {
        if (actor.inventory.isEmpty()) {
            return "empty";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : actor.inventory.entrySet()) {
            parts.add(entry.getValue() + " " + GameData.itemName(entry.getKey()));
            if (parts.size() >= 5) {
                break;
            }
        }
        return String.join(", ", parts);
    }

    private NpcRuntime runtimeFor(Npc npc) {
        return npcRuntime.computeIfAbsent(npc, ignored -> new NpcRuntime(npc.x(), npc.y(), Math.max(0, random.nextInt(90))));
    }

    private String cleanName(String name, int maxLength) {
        if (name == null) {
            return "";
        }
        String cleaned = name.replaceAll("\\s+", " ").stripLeading();
        return cleaned.substring(0, Math.min(maxLength, cleaned.length()));
    }

    private String chooseMonster(char tile) {
        List<String> pool = switch (tile) {
            case 'f' -> List.of("doe", "crystal_hare", "moss_stag", "bramble_boar", "thornling", "spider", "bandit_archer", "marsh_drake");
            case 's' -> List.of("sand_stalker", "glass_scorpion", "ember_tortoise", "ember_imp", "bandit_cutthroat", "orc_raider", "orc_berserker", "red_dragon");
            case 'n' -> List.of("frost_wolf", "snow_lynx", "crypt_bat", "frost_troll", "ice_golem", "mountain_drake");
            case 'v' -> List.of("slime", "crystal_hare", "spider", "reed_serpent", "bog_beast", "marsh_drake", "swamp_troll");
            case 'b' -> List.of("ember_tortoise", "goblin_skirmisher", "ash_scorpion", "skeleton", "wraith", "ember_imp", "bandit_cutthroat", "orc_raider", "orc_shaman", "fire_giant", "red_dragon");
            case 'q', 'm' -> List.of("mountain_goat", "stoneback_goat", "snow_lynx", "crypt_bat", "mountain_drake", "stone_giant", "hill_giant", "ice_golem");
            case 'w' -> List.of("river_eel", "slime", "reed_serpent", "marsh_drake");
            case 'd' -> List.of("crypt_bat", "skeleton", "spider", "wraith", "bandit_cutthroat", "orc_raider", "bone_knight");
            default -> List.of("sheep", "doe", "crystal_hare", "meadow_wolf", "bramble_boar", "thornling", "bandit_cutthroat", "bandit_archer");
        };
        int cap = Math.min(pool.size(), player.level < 4 ? 2 : player.level < 7 ? 3 : pool.size());
        return pool.get(random.nextInt(cap));
    }

    private List<String> chooseMonsterGroup(char tile) {
        int count = 1;
        double roll = random.nextDouble();
        if (roll < config.threeMonsterChance && player.level >= 3) {
            count = 3;
        } else if (roll < config.threeMonsterChance + config.twoMonsterChance) {
            count = 2;
        }
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            keys.add(chooseMonster(tile));
        }
        return keys;
    }

    public record NpcMotion(Npc npc, int x, int y, int fromX, int fromY, int moveStartTick, int facingDx, int facingDy) {
        public static final int MOVE_TICKS = 14;
    }

    public record DungeonMonsterMotion(
            GameData.MonsterSpec spec,
            boolean boss,
            int x,
            int y,
            int fromX,
            int fromY,
            int moveStartTick,
            int facingDx,
            int facingDy
    ) {
        public static final int MOVE_TICKS = 18;
    }

    public record QuestObjective(
            String questId,
            String title,
            String target,
            String mapId,
            int x,
            int y,
            String asset,
            Quest.ObjectiveKind kind,
            String monsterKey,
            boolean blocking
    ) {
    }

    public record QuestInteractible(
            String questId,
            String title,
            String target,
            String mapId,
            int x,
            int y,
            String asset
    ) {
    }

    public record VillageRecruitOption(
            String name,
            String className,
            String sprite,
            int level,
            int hp,
            int mp,
            int attack,
            int defense,
            int goldCost
    ) {
    }

    private static final class NpcRuntime {
        final int homeX;
        final int homeY;
        int x;
        int y;
        int fromX;
        int fromY;
        int moveStartTick = -NpcMotion.MOVE_TICKS;
        int facingDx;
        int facingDy = 1;
        int nextThinkTick;

        NpcRuntime(int x, int y, int nextThinkTick) {
            this.homeX = x;
            this.homeY = y;
            this.x = x;
            this.y = y;
            this.fromX = x;
            this.fromY = y;
            this.nextThinkTick = nextThinkTick;
        }
    }

    private static final class DungeonMonsterRuntime {
        final String id;
        final GameData.MonsterSpec spec;
        final boolean boss;
        final int homeX;
        final int homeY;
        int x;
        int y;
        int fromX;
        int fromY;
        int moveStartTick = -DungeonMonsterMotion.MOVE_TICKS;
        int facingDx;
        int facingDy = 1;
        int nextThinkTick;

        DungeonMonsterRuntime(String id, GameData.MonsterSpec spec, boolean boss, int x, int y, int nextThinkTick) {
            this.id = id;
            this.spec = spec;
            this.boss = boss;
            this.homeX = x;
            this.homeY = y;
            this.x = x;
            this.y = y;
            this.fromX = x;
            this.fromY = y;
            this.nextThinkTick = nextThinkTick;
        }
    }
}
