package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import com.alderfall.game.map.WorldTransition;
import com.alderfall.game.inventory.Equipment;
import com.alderfall.game.inventory.Item;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class GameState {
    public static final int DEFAULT_ZOOM = 100;
    public static final int MIN_ZOOM = 70;
    public static final int MAX_ZOOM = 200;
    public static final int ZOOM_STEP = 5;
    public final ChestSystem chests = new ChestSystem();
    public WorldProp activeChest;

    public record DialogueVideoPrompt(String key, String title, String caption, String assetPath) {
    }
    public record RoamingEventChoice(String label, Runnable action) {
    }
    public record RoamingEventPrompt(String title, String sprite, String description, List<RoamingEventChoice> choices) {
        public RoamingEventPrompt {
            choices = choices == null ? List.of() : List.copyOf(choices);
        }
    }

    public static final int TICKS_PER_GAME_DAY = 7200;
    private static final int VILLAGE_PRODUCTION_TICKS = 900;
    private static final int SIDE_QUEST_REFRESH_TICKS = TICKS_PER_GAME_DAY * 8;
    private static final int VILLAGE_RECRUIT_RELATIONSHIP = 55;
    private static final int COMPANION_RECRUIT_RELATIONSHIP = 20;
    public static final int MIN_NPC_RELATIONSHIP = -100;
    public static final int MAX_NPC_RELATIONSHIP = 100;
    public static final int MAX_COMPANION_TRUST = 300;
    private static final int MAX_ACTIVE_COMPANIONS = 3;
    private static final int MAX_ENEMY_GROUP_SIZE = 5;
    private static final int MAX_COMPANION_MEMORIES = 18;
    private static final int TRAVEL_BANTER_DURATION_TICKS = 360;
    private static final int TRAVEL_COMMENT_DURATION_TICKS = 520;
    private static final int TOWN_CONVERSATION_DURATION_TICKS = 220;
    public static final List<String> STORY_INTRO = List.of(
            "You went to an old road shrine to investigate a reported ritual. Vaelthara was there. The ward broke, the shrine burned, and you escaped wounded. You do not yet know why its protection failed.",
            "Maelis brought you to Oathstead, a camp sheltering people driven from the road. Its palisade is unfinished. If the same failure reaches its wards, the people who took you in will have nowhere safe to go.",
            "Across Alderfall, communities keep different protections: northern cairns, southern shrine flames, western river charms, and the bells of the Fenlands. Their keepers know parts of a craft that no single kingdom fully understands.",
            "Maelis asks you to examine the burned shrine. Recover what survived, find someone who can read it, and help Oathstead prepare. Defeating Vaelthara will take more than surviving her a second time."
    );
    private static final String[] BUILDING_OWNER_NAMES = {
            "Bran", "Torin", "Marla", "Elowen", "Kael", "Liora", "Mira", "Orin",
            "Rowan", "Sable", "Vexa", "Aster", "Bryn", "Cala", "Dain", "Fara"
    };
    private static final String[] BUILDING_PLACE_ROOTS = {
            "Riverrun", "Oakwatch", "Stonegate", "Mossgate", "Bellmere", "Cinderford",
            "Willowbend", "Highford", "Dawncross", "Briarbridge", "Moonwell", "Emberfall"
    };
    private static final String[] BUILDING_SIGNS = {
            "Blue Lantern", "Copper Kettle", "Silver Stag", "Mended Boot", "Quiet Bell",
            "Golden Reed", "Ashen Swan", "Wayfarer's Star", "Green Hammer", "River Pearl"
    };
    private static final String[] BOOKSHELF_MONSTER_LORE_KEYS = {
            "slime", "wolf", "frost_wolf", "skeleton", "wraith", "spider", "ash_scorpion",
            "marsh_drake", "sand_stalker", "ember_imp", "ice_golem", "bandit_archer"
    };
    public record FastTravelDestination(String mapId, String label, String kind, int cost, String portalAsset) {
    }
    public record WorldAbilityOption(Actor actor, Ability ability, String effectKey, String label, String description) {
    }
    private record EnvironmentSnapshot(
            String mapId,
            int playerX,
            int playerY,
            int worldTick,
            long visualRevision,
            char biome,
            WeatherCondition weather,
            double windRadians,
            double windStrength
    ) {
    }
    public final GameConfig config;
    public final WorldMap world;
    private final WeatherController weatherController;
    private final LandmarkDiscoveryLog landmarkDiscoveryLog = new LandmarkDiscoveryLog();
    public final Random random = new Random();
    public final Map<String, Quest> quests = new LinkedHashMap<>();
    public final List<Actor> allies = new ArrayList<>();
    public final List<String> recruitedIds = new ArrayList<>();
    public final Set<String> villageAllies = new HashSet<>();
    public final Set<String> romancedCompanionIds = new HashSet<>();
    public final Set<String> marriedCompanionIds = new HashSet<>();
    public final Map<String, String> villageWorkerRoles = new LinkedHashMap<>();
    public final Map<String, String> villageBuildingAssignments = new LinkedHashMap<>();
    public final Map<String, List<String>> villageBuildingDecorations = new LinkedHashMap<>();
    public final Map<String, Integer> villageStorage = new LinkedHashMap<>();
    public final Set<String> harvestedQuestResources = new HashSet<>();
    public final Set<String> harvestedResourceNodes = new HashSet<>();
    public final Map<String, String> questBranchOutcomes = new LinkedHashMap<>();
    public final Set<String> unlockedRecipeKeys = new HashSet<>();
    public final Map<String, Integer> npcRelationships = new LinkedHashMap<>();
    public final Map<String, List<CompanionMemory>> companionMemoryJournal = new LinkedHashMap<>();
    public final Map<String, String> companionSoftRequests = new LinkedHashMap<>();
    public final Map<String, Integer> companionDialogueTopicCounts = new LinkedHashMap<>();
    public final Set<String> fulfilledCompanionSoftRequests = new HashSet<>();
    public final Set<String> companionRelationshipMilestones = new HashSet<>();
    public final Set<String> introducedNpcKeys = new HashSet<>();
    public final Map<String, Integer> npcKnowledge = new LinkedHashMap<>();
    private final Map<String, String> weeklyNpcQuestIds = new LinkedHashMap<>();
    private int weeklyQuestBlock = -1;
    public final Map<String, Integer> monsterInsights = new LinkedHashMap<>();
    public final Map<String, Set<String>> monsterKnownVulnerabilities = new LinkedHashMap<>();
    public final Map<String, Integer> settlementVisits = new LinkedHashMap<>();
    public final Map<String, Integer> buildingKnowledge = new LinkedHashMap<>();
    public final Map<String, Integer> worldAbilityTimers = new LinkedHashMap<>();
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
    public String selectedInteriorAssetCategory = "All";
    public TilePoint pendingVillageMoveSource;
    public String pendingVillageMoveMapId;
    public int mapEditorKindIndex;
    public int mapEditorSizeIndex = 1;
    private String mapEditorReturnMapId = "";
    private int mapEditorReturnX;
    private int mapEditorReturnY;
    private int mapEditorSerial;
    public Battle battle;
    public CampDefenseMinigame defenseRaid;
    private String activeDefenseQuestId = "";
    private boolean demonQueenBattleActive;
    private boolean demonQueenDefeated;
    public Npc activeNpc;
    public Actor activePartyTalkActor;
    public Shop activeShop;
    public CityBuilding activeVillageBuilding;
    public int dialogIndex;
    private DialogueLibrary.DialogueSession activeDialogueSession;
    private DialogueVideoPrompt activeDialogueVideo;
    private RoamingEventPrompt activeRoamingEventPrompt;
    private String activeNpcIntroLine = "";
    private String activeQuestDialogueLine = "";
    private String activeShopDialogueBuildingKey = "";
    private String activeDialogueRevealKey = "";
    private int activeDialogueRevealChars = Integer.MAX_VALUE;
    private long activeDialogueRevealStartedAtMs;
    private List<VillageRecruitOption> settlementRecruitOptions = List.of();
    public int zoom = DEFAULT_ZOOM;
    public int worldTick;
    public int storyPage;
    public boolean worldMapKingdoms;
    public String status = "Choose New Adventure or load an existing save.";
    private final Map<Npc, NpcRuntime> npcRuntime = new LinkedHashMap<>();
    private final Map<String, List<Npc>> npcListCache = new LinkedHashMap<>();
    private final Map<String, TilePoint> commuterWorkSiteCache = new LinkedHashMap<>();
    private int npcListCacheTick = -1;
    private final Map<String, List<DungeonMonsterRuntime>> dungeonMonsterRuntime = new LinkedHashMap<>();
    private final Set<String> defeatedDungeonMonsters = new HashSet<>();
    private final Map<String, List<QuestMonsterRuntime>> questMonsterRuntime = new LinkedHashMap<>();
    private final Map<String, List<QuestNpcRuntime>> questNpcRuntime = new LinkedHashMap<>();
    private List<QuestObjective> activeQuestObjectiveCache;
    private final Map<String, WorldProp> removedResourceNodeProps = new LinkedHashMap<>();
    private DungeonMonsterRuntime activeDungeonMonster;
    private QuestMonsterRuntime activeQuestMonster;
    private String pendingResourceNodeMapId = "";
    private TilePoint pendingResourceNodeTile;
    private String pendingResourceNodeAsset = "";
    private String pendingReadBookshelfMapId = "";
    private TilePoint pendingReadBookshelfTile;
    public String activeGatherMapId = "";
    public CraftingSystem.GatherCandidate activeGatherCandidate;
    public String lastGatheredPropMapId = "";
    public WorldProp lastGatheredProp;
    public int lastGatheredPropWorldTick = -1;
    private TravelBanterPrompt activeTravelBanter;
    final PartyDialogue partyDialogue = new PartyDialogue();
    private final List<PendingCompanionComment> pendingCompanionComments = new ArrayList<>();
    private final Map<String, Integer> recentTravelBanterKeys = new LinkedHashMap<>();
    private AmbientNpcConversation activeTownConversation;
    private final Map<String, Integer> recentTownConversationKeys = new LinkedHashMap<>();
    private int nextTravelBanterTick = 420;
    private int nextTownConversationTick = 240;
    private static final int DIALOGUE_REVEAL_MS_PER_CHAR = 12;
    private static final int DIALOGUE_REVEAL_INITIAL_CHARS = 2;
    private EnvironmentSnapshot environmentSnapshot;

    public GameState(GameConfig config) {
        this.config = config;
        this.world = new WorldMap(0);
        this.weatherController = new WeatherController(world);
        for (Map.Entry<String, Quest> entry : GameData.QUESTS.entrySet()) {
            quests.put(entry.getKey(), entry.getValue().copy());
        }
        ensureStarterRecipeUnlocks();
        partyDialogue.install(this);
    }

    public void openMainMenu() {
        activeNpc = null;
        activePartyTalkActor = null;
        activeShop = null;
        activeVillageBuilding = null;
        battle = null;
        defenseRaid = null;
        activeDungeonMonster = null;
        activeQuestMonster = null;
        activeDialogueSession = null;
        activeNpcIntroLine = "";
        activeQuestDialogueLine = "";
        activeShopDialogueBuildingKey = "";
        dialogIndex = 0;
        activeTravelBanter = null;
        activeTownConversation = null;
        pendingCompanionComments.clear();
        recentTownConversationKeys.clear();
        mode = GameMode.MAIN_MENU;
        status = "Choose New Adventure or load an existing save.";
    }

    public void openClassSelect() {
        activeNpc = null;
        activePartyTalkActor = null;
        activeShop = null;
        activeVillageBuilding = null;
        battle = null;
        defenseRaid = null;
        activeDungeonMonster = null;
        activeQuestMonster = null;
        activeDialogueSession = null;
        activeNpcIntroLine = "";
        activeQuestDialogueLine = "";
        activeShopDialogueBuildingKey = "";
        dialogIndex = 0;
        activeTravelBanter = null;
        activeTownConversation = null;
        pendingCompanionComments.clear();
        recentTownConversationKeys.clear();
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
        partyDialogue.reset();
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
        resetQuestMonsterRuntime();
        allies.clear();
        recruitedIds.clear();
        villageAllies.clear();
        romancedCompanionIds.clear();
        marriedCompanionIds.clear();
        villageWorkerRoles.clear();
        villageBuildingAssignments.clear();
        villageBuildingDecorations.clear();
        villageStorage.clear();
        chests.clear();
        activeChest = null;
        partyScreenIndex = 0;
        resetVillageInterface();
        harvestedQuestResources.clear();
        restoreHarvestedResourceNodes();
        harvestedResourceNodes.clear();
        questBranchOutcomes.clear();
        unlockedRecipeKeys.clear();
        ensureStarterRecipeUnlocks();
        npcRelationships.clear();
        companionMemoryJournal.clear();
        companionSoftRequests.clear();
        companionDialogueTopicCounts.clear();
        fulfilledCompanionSoftRequests.clear();
        companionRelationshipMilestones.clear();
        introducedNpcKeys.clear();
        npcKnowledge.clear();
        weeklyNpcQuestIds.clear();
        weeklyQuestBlock = -1;
        monsterInsights.clear();
        monsterKnownVulnerabilities.clear();
        settlementVisits.clear();
        buildingKnowledge.clear();
        activeTravelBanter = null;
        activeTownConversation = null;
        pendingCompanionComments.clear();
        recentTownConversationKeys.clear();
        nextTravelBanterTick = 420;
        nextTownConversationTick = 240;
        recordSettlementVisit(currentMapId);
        battle = null;
        defenseRaid = null;
        activeDungeonMonster = null;
        activeQuestMonster = null;
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
        return currentEnvironment().biome();
    }

    public WeatherCondition currentWeather() {
        return currentEnvironment().weather();
    }

    public String weatherLabel() {
        return weatherController.weatherLabel(currentWeather());
    }

    public double windRadians() {
        return currentEnvironment().windRadians();
    }

    public double windStrength() {
        return currentEnvironment().windStrength();
    }

    public String windLabel() {
        return weatherController.windLabel(windRadians());
    }

    private EnvironmentSnapshot currentEnvironment() {
        long visualRevision = world.visualRevision(currentMapId);
        EnvironmentSnapshot cached = environmentSnapshot;
        if (cached != null
                && cached.mapId().equals(currentMapId)
                && cached.playerX() == playerX
                && cached.playerY() == playerY
                && cached.worldTick() == worldTick
                && cached.visualRevision() == visualRevision) {
            return cached;
        }
        char biome = biomeTileAt(currentMapId, playerX, playerY);
        int day = dayNumber();
        WeatherCondition weather = weatherController.currentWeather(currentMapId, biome, worldTick, day);
        EnvironmentSnapshot refreshed = new EnvironmentSnapshot(
                currentMapId,
                playerX,
                playerY,
                worldTick,
                visualRevision,
                biome,
                weather,
                weatherController.windRadians(biome, worldTick, day),
                weatherController.windStrength(weather, biome, worldTick, day)
        );
        environmentSnapshot = refreshed;
        return refreshed;
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
            status = "Talking to " + npcDisplayName(activeNpc) + ".";
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

    public boolean talkToNpc(Npc npc) {
        if (mode != GameMode.EXPLORE || npc == null) {
            return false;
        }
        if (crafting.active()) {
            status = "Busy: " + crafting.progressLabel() + ".";
            return false;
        }
        if (!npc.mapId().equals(currentMapId)) {
            status = npcDisplayName(npc) + " is not here.";
            return false;
        }
        TilePoint position = npcPosition(npc);
        if (Math.abs(position.x() - playerX) + Math.abs(position.y() - playerY) > 1) {
            status = npcDisplayName(npc) + " is too far away.";
            return false;
        }
        String npcKey = npcRelationshipKey(npc);
        boolean firstMeeting = introducedNpcKeys.add(npcKey);
        if (firstMeeting) {
            recordNpcKnowledge(npc, 1);
            activeNpcIntroLine = npcIntroductionLine(npc);
        } else {
            activeNpcIntroLine = "";
        }
        activeQuestDialogueLine = "";
        activeShopDialogueBuildingKey = "";
        activeNpc = npc;
        activeShop = npc.shopId() == null ? null : GameData.SHOPS.get(npc.shopId());
        activeDialogueSession = startDialogueSessionFor(npc);
        dialogIndex = 0;
        mode = GameMode.DIALOG;
        String questNote = ""; // A greeting is not testimony, delivery, or a decision.
        status = "Talking to " + npc.name() + "." + questNote;
        return true;
    }

    public boolean talkToPartyAlly(Actor ally) {
        if ((mode != GameMode.EXPLORE && mode != GameMode.PARTY && mode != GameMode.SKILLS) || ally == null) {
            return false;
        }
        if (crafting.active()) {
            status = "Busy: " + crafting.progressLabel() + ".";
            return false;
        }
        if (!activeAllies().contains(ally)) {
            status = ally.name + " is not traveling with you.";
            return false;
        }
        Npc sourceNpc = npcForAlly(ally);
        String relationshipMapId = sourceNpc == null ? currentMapId : sourceNpc.mapId();
        List<String> dialog = sourceNpc == null ? partyAllyDialog(ally) : sourceNpc.dialog();
        Npc npc = new Npc(
                relationshipMapId,
                ally.name,
                ally.sprite,
                playerX,
                playerY,
                dialog,
                sourceNpc == null ? null : sourceNpc.questId(),
                sourceNpc == null ? null : sourceNpc.shopId(),
                sourceNpc == null ? null : sourceNpc.recruitId(),
                0,
                ally.professionXp,
                null
        );
        introducedNpcKeys.add(npcRelationshipKey(npc));
        recordNpcKnowledge(npc, 1);
        activeNpc = npc;
        activePartyTalkActor = ally;
        activeShop = null;
        activeNpcIntroLine = "";
        activeQuestDialogueLine = "";
        activeShopDialogueBuildingKey = "";
        activeDialogueSession = startDialogueSessionFor(npc);
        dialogIndex = 0;
        mode = GameMode.DIALOG;
        String questNote = "";
        status = "Talking to " + ally.name + "." + questNote;
        return true;
    }

    public void openRoamingEventDialogue(String title, String sprite, List<String> dialog) {
        String safeTitle = title == null || title.isBlank() ? "Roadside Event" : title.strip();
        String safeSprite = sprite == null || sprite.isBlank() ? "npc_merchant" : sprite.strip();
        List<String> safeDialog = dialog == null || dialog.isEmpty()
                ? List.of(safeTitle + ": The road gives you a small choice before moving on.")
                : List.copyOf(dialog);
        Npc npc = new Npc(currentMapId, safeTitle, safeSprite, playerX, playerY, safeDialog, null, null);
        introducedNpcKeys.add(npcRelationshipKey(npc));
        activeNpc = npc;
        activePartyTalkActor = null;
        activeShop = null;
        activeRoamingEventPrompt = null;
        activeNpcIntroLine = "";
        activeQuestDialogueLine = "";
        activeShopDialogueBuildingKey = "";
        activeDialogueSession = startDialogueSessionFor(npc);
        dialogIndex = 0;
        mode = GameMode.DIALOG;
        status = "Encounter: " + safeTitle + ".";
    }

    public void openRoamingEventPrompt(String title, String sprite, String description, List<RoamingEventChoice> choices) {
        String safeTitle = title == null || title.isBlank() ? "Roadside Event" : title.strip();
        String safeSprite = sprite == null || sprite.isBlank() ? "npc_merchant" : sprite.strip();
        String safeDescription = description == null || description.isBlank()
                ? safeTitle + ": The road gives you a choice before moving on."
                : description.strip();
        List<RoamingEventChoice> safeChoices = choices == null || choices.isEmpty()
                ? List.of(new RoamingEventChoice("Leave it alone", () -> resolveRoamingEventPrompt(
                safeTitle + ": You leave the sign undisturbed and return to the road.",
                "You leave the encounter alone.")))
                : List.copyOf(choices);
        activeRoamingEventPrompt = new RoamingEventPrompt(safeTitle, safeSprite, safeDescription, safeChoices);
        Npc npc = new Npc(currentMapId, safeTitle, safeSprite, playerX, playerY, List.of(safeDescription), null, null);
        introducedNpcKeys.add(npcRelationshipKey(npc));
        activeNpc = npc;
        activePartyTalkActor = null;
        activeShop = null;
        activeNpcIntroLine = "";
        activeQuestDialogueLine = "";
        activeShopDialogueBuildingKey = "";
        activeDialogueSession = null;
        dialogIndex = 0;
        mode = GameMode.DIALOG;
        status = "Encounter: " + safeTitle + ". Choose an approach.";
    }

    public boolean roamingEventPromptActive() {
        return activeRoamingEventPrompt != null;
    }

    public void resolveRoamingEventPrompt(String resultLine, String statusText) {
        if (activeRoamingEventPrompt == null) {
            return;
        }
        String title = activeRoamingEventPrompt.title();
        activeRoamingEventPrompt = null;
        activeDialogueSession = null;
        activeQuestDialogueLine = resultLine == null || resultLine.isBlank()
                ? title + ": The moment passes."
                : resultLine.strip();
        activeShopDialogueBuildingKey = "";
        dialogIndex = 0;
        status = statusText == null || statusText.isBlank() ? title + " resolved." : statusText.strip();
    }

    public void startRoamingEventBattle(List<String> monsterKeys, String statusText) {
        List<String> keys = monsterKeys == null || monsterKeys.isEmpty() ? List.of("bandit_cutthroat") : monsterKeys;
        List<GameData.MonsterSpec> specs = monsterSpecsForKeys(keys);
        battle = createBattle(specs, world.tileAt(currentMapId, playerX, playerY), world.kind(currentMapId));
        prepareBattleKnowledge(battle);
        activeRoamingEventPrompt = null;
        activeNpc = null;
        activePartyTalkActor = null;
        activeShop = null;
        activeDialogueSession = null;
        activeQuestDialogueLine = "";
        activeShopDialogueBuildingKey = "";
        activeNpcIntroLine = "";
        dialogIndex = 0;
        mode = GameMode.BATTLE;
        status = statusText == null || statusText.isBlank() ? "The encounter turns violent." : statusText.strip();
    }

    private List<String> partyAllyDialog(Actor ally) {
        List<String> dialog = new ArrayList<>();
        dialog.add(ally.name + ": I am with you. Say the word and I will adjust my pace.");
        dialog.add("Class: " + ally.className + ". Level " + ally.level + ". " + abilityPreview(ally));
        dialog.add("Pack: " + actorInventoryPreview(ally));
        dialog.add("Skills: " + professionPreview(ally));
        dialog.add("Road: I will keep one step off your heel so we do not crowd the same ground.");
        return dialog;
    }

    private Npc npcForAlly(Actor ally) {
        if (ally == null) {
            return null;
        }
        String recruitId = recruitIdForAlly(ally);
        if (!recruitId.isBlank()) {
            for (Npc npc : GameData.NPCS) {
                if (recruitId.equals(npc.recruitId())) {
                    return npc;
                }
            }
        }
        for (Npc npc : GameData.NPCS) {
            if (npc.name().equals(ally.name)) {
                return npc;
            }
        }
        return null;
    }

    private String recruitIdForAlly(Actor ally) {
        if (ally == null) {
            return "";
        }
        for (String recruitId : recruitedIds) {
            GameData.RecruitSpec spec = GameData.RECRUITS.get(recruitId);
            if (spec != null && spec.name().equals(ally.name)) {
                return recruitId;
            }
        }
        for (Map.Entry<String, GameData.RecruitSpec> entry : GameData.RECRUITS.entrySet()) {
            GameData.RecruitSpec spec = entry.getValue();
            if (spec.name().equals(ally.name) && spec.sprite().equals(ally.sprite)) {
                return entry.getKey();
            }
        }
        return "";
    }

    private boolean recruitedCompanionNpc(Npc npc) {
        return npc != null && npc.recruitId() != null && isRecruited(npc.recruitId());
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
        if (storyPortalNearPlayer() != null) {
            interactStoryPortal();
            return;
        }
        if (townPortalNearPlayer() != null) {
            openFastTravel();
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
        WorldProp chest = chestNearPlayer();
        if (chest != null) {
            openChest(chest);
            return;
        }
        if (studyNearbyBookshelf()) {
            return;
        }
        Npc npc = npcAtPlayer();
        if (npc == null) {
            for (WorldProp prop : world.propsInBounds(currentMapId, playerX - 1, playerY - 1, playerX + 2, playerY + 2)) {
                String observation = WesternReachFolklore.observation(currentMapId, prop.asset());
                if (observation.isEmpty()) observation = HearthlandsFolklore.observation(currentMapId, prop);
                if (!observation.isEmpty()) {
                    status = observation;
                    return;
                }
            }
            status = "No one is close enough to talk.";
            return;
        }
        talkToNpc(npc);
    }

    public WorldProp chestNearPlayer() {
        return world.propsInBounds(currentMapId, playerX - 1, playerY - 1, playerX + 2, playerY + 2).stream()
                .filter(ChestSystem::isChest)
                .filter(p -> Math.max(Math.abs(p.x() - playerX), Math.abs(p.y() - playerY)) <= 1)
                .min(java.util.Comparator.comparingInt(p -> Math.abs(p.x() - playerX) + Math.abs(p.y() - playerY)))
                .orElse(null);
    }

    public void openChest(WorldProp chest) {
        if (mode != GameMode.EXPLORE || chest == null || !ChestSystem.isChest(chest)
                || !world.props(currentMapId).contains(chest)
                || Math.max(Math.abs(chest.x() - playerX), Math.abs(chest.y() - playerY)) > 1) return;
        activeChest = chest;
        chests.contents(currentMapId, chest);
        mode = GameMode.CHEST;
        status = "Chest opened. Take items or store supplies from your shared pack.";
    }

    public Map<String, Integer> chestContents() {
        return activeChest == null ? Map.of() : chests.contents(currentMapId, activeChest);
    }

    public void transferChestItem(String key, boolean taking, int amount) {
        if (mode != GameMode.CHEST || activeChest == null
                || !world.props(currentMapId).contains(activeChest)
                || Math.max(Math.abs(activeChest.x() - playerX), Math.abs(activeChest.y() - playerY)) > 1) return;
        Map<String, Integer> contents = chestContents();
        int moved = ChestSystem.transfer(taking ? contents : player.inventory,
                taking ? player.inventory : contents, key, amount);
        if (moved > 0) status = (taking ? "Took " : "Stored ") + moved + " x " + GameData.itemName(key) + ".";
    }

    public WorldProp townPortalNearPlayer() {
        if (!isSettlementMap(currentMapId)) {
            return null;
        }
        WorldProp best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (WorldProp prop : world.props(currentMapId)) {
            if (!world.isTownPortalAsset(prop.asset())) {
                continue;
            }
            int distance = Math.max(Math.abs(prop.x() - playerX), Math.abs(prop.y() - playerY));
            if (distance <= 1 && distance < bestDistance) {
                best = prop;
                bestDistance = distance;
            }
        }
        return best;
    }

    public WorldProp storyPortalNearPlayer() {
        WorldProp best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (WorldProp prop : world.props(currentMapId)) {
            if (!GameData.STORY_PORTAL_ASSET.equals(prop.asset())) {
                continue;
            }
            int distance = Math.max(Math.abs(prop.x() - playerX), Math.abs(prop.y() - playerY));
            if (distance <= 1 && distance < bestDistance) {
                best = prop;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void interactStoryPortal() {
        if (mainStoryComplete()) {
            status = "The Old Gate is quiet. Vaelthara is defeated; the settlements still have wards to repair and promises to keep.";
            return;
        }
        Quest gateQuest = quests.get("ms_twelve_stones_gate");
        if (gateQuest == null || !gateQuest.accepted) {
            status = "The Old Gate of Alderfall is dormant. Maelis has not prepared the twelve-stone oath yet.";
            return;
        }
        int stones = magicStoneCount();
        if (stones < GameData.MAGIC_STONE_KEYS.size()) {
            status = "The Old Gate shows " + stones + "/" + GameData.MAGIC_STONE_KEYS.size()
                    + " stones lit. Red-marked main story quests reveal the missing sockets.";
            return;
        }
        List<GameData.MonsterSpec> specs = monsterSpecsForKeys(List.of(
                "vaelthara", "morvane_mercy_taker", "kharvok_banner_bound", "velmora_bell_drowned", "sareth_cinder_knife"
        ));
        battle = createBattle(specs, world.tileAt(currentMapId, playerX, playerY), world.kind(currentMapId));
        prepareBattleKnowledge(battle);
        demonQueenBattleActive = true;
        mode = GameMode.BATTLE;
        status = "Vaelthara: You saw one shrine fall and built a refuge behind another ward. Give me the stones. I will keep Oathstead fed and its gates guarded. Its people will remain under my protection until I release them.";
        battle.addLog(status);
    }

    private int magicStoneCount() {
        int count = 0;
        for (String key : GameData.MAGIC_STONE_KEYS) {
            if (player.hasItem(key)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasAllMagicStones() {
        return magicStoneCount() >= GameData.MAGIC_STONE_KEYS.size();
    }

    private boolean hasAllMagicStonesExcept(String exceptKey) {
        for (String key : GameData.MAGIC_STONE_KEYS) {
            if (!key.equals(exceptKey) && !player.hasItem(key)) {
                return false;
            }
        }
        return true;
    }

    private boolean mainStoryComplete() {
        Quest gateQuest = quests.get("ms_twelve_stones_gate");
        return demonQueenDefeated || (gateQuest != null && gateQuest.completed);
    }

    private boolean studyNearbyBookshelf() {
        WorldProp shelf = nearbyBookshelf();
        if (shelf == null) {
            return false;
        }
        beginBookshelfReading(shelf);
        return true;
    }

    public WorldProp readableBookshelfAtTile(int x, int y) {
        if (!"interior".equals(world.kind(currentMapId))) {
            return null;
        }
        for (WorldProp prop : world.props(currentMapId)) {
            if ("interior_bookshelf".equals(prop.asset()) && prop.x() == x && prop.y() == y) {
                return prop;
            }
        }
        return null;
    }

    public void readBookshelfAtTile(int x, int y) {
        if (mode != GameMode.EXPLORE) {
            return;
        }
        WorldProp shelf = readableBookshelfAtTile(x, y);
        if (shelf == null) {
            status = "There are no readable books there.";
            return;
        }
        if (Math.abs(shelf.x() - playerX) + Math.abs(shelf.y() - playerY) > 1) {
            status = "Move closer to read the bookshelf.";
            return;
        }
        beginBookshelfReading(shelf);
    }

    private void beginBookshelfReading(WorldProp shelf) {
        if (crafting.active()) {
            status = "Busy: " + crafting.progressLabel() + ".";
            return;
        }
        pendingReadBookshelfMapId = currentMapId;
        pendingReadBookshelfTile = new TilePoint(shelf.x(), shelf.y());
        int ticks = Math.max(36, 84 - Math.max(1, player.intelligence) * 2);
        status = crafting.beginReading("bookshelf", ticks);
        if (!crafting.active()) {
            pendingReadBookshelfMapId = "";
            pendingReadBookshelfTile = null;
        }
    }

    private void studyBookshelf(WorldProp shelf, String mapId) {
        String recipeNote = learnRecipeFromBookshelf(shelf, mapId);
        if (!recipeNote.isBlank()) {
            status = recipeNote;
            return;
        }
        String monsterKey = bookshelfMonsterLoreKey(shelf, mapId);
        GameData.MonsterSpec spec = GameData.MONSTERS.get(monsterKey);
        if (spec != null && monsterInsightCount(monsterKey) < 4) {
            recordMonsterInsight(monsterKey);
            status = "You study a shelf note on " + spec.name() + ". Monster knowledge improved.";
            return;
        }
        String worldNote = bookshelfWorldKnowledgeMessage(mapId);
        status = worldNote.isBlank()
                ? "You read a few marked pages about Alderfall's roads, weather, and old borders."
                : worldNote;
    }

    private WorldProp nearbyBookshelf() {
        if (!"interior".equals(world.kind(currentMapId))) {
            return null;
        }
        WorldProp best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (WorldProp prop : world.props(currentMapId)) {
            if (!"interior_bookshelf".equals(prop.asset())) {
                continue;
            }
            int distance = Math.abs(prop.x() - playerX) + Math.abs(prop.y() - playerY);
            if (distance <= 1 && distance < bestDistance) {
                best = prop;
                bestDistance = distance;
            }
        }
        return best;
    }

    private String bookshelfMonsterLoreKey(WorldProp shelf, String mapId) {
        int seed = mapId.hashCode() * 31 + shelf.x() * 928371 + shelf.y() * 364479;
        return BOOKSHELF_MONSTER_LORE_KEYS[Math.floorMod(seed, BOOKSHELF_MONSTER_LORE_KEYS.length)];
    }

    private String bookshelfWorldKnowledgeMessage(String mapId) {
        CityBuilding building = buildingForInteriorMap(mapId);
        String sourceMapId = sourceMapForInterior(mapId);
        if (building != null && !sourceMapId.isBlank()) {
            String message = gainBuildingKnowledgeMessage(sourceMapId, building, 1, "The bookshelf");
            if (!message.isBlank()) {
                return message;
            }
        }
        if (!sourceMapId.isBlank()) {
            recordSettlementVisit(sourceMapId);
            return "You read marginal notes about " + world.label(sourceMapId) + ". Local knowledge improved.";
        }
        return "";
    }

    private String learnRecipeFromBookshelf(WorldProp shelf, String mapId) {
        for (CraftingSystem.Recipe recipe : CraftingSystem.recipesFromBookcase(mapId, shelf.x(), shelf.y())) {
            String note = unlockRecipe(recipe.key(), "The bookshelf teaches");
            if (!note.isBlank()) {
                return note;
            }
        }
        return "";
    }

    public void openFastTravel() {
        if (townPortalNearPlayer() == null) {
            status = "No town portal is close enough.";
            return;
        }
        mode = GameMode.FAST_TRAVEL;
        status = "Choose a destination.";
    }

    public List<FastTravelDestination> fastTravelDestinations() {
        List<FastTravelDestination> destinations = new ArrayList<>();
        WorldMap.SettlementSite current = currentSettlementSite();
        for (WorldMap.SettlementSite settlement : world.settlementSites()) {
            if (settlement.id().equals(currentMapId) || world.townPortalArrival(settlement.id()) == null) {
                continue;
            }
            int cost = fastTravelCost(current, settlement);
            String portalAsset = switch (settlement.kingdomId()) {
                case "highwall" -> "town_portal_snow";
                case "belltower" -> "town_portal_marsh";
                case "sanctum" -> "town_portal_desert";
                case "crownlands" -> settlement.kind().equals("Village") ? "town_portal_green" : "town_portal_city";
                default -> settlement.kind().equals("Village") ? "town_portal_green" : "town_portal_city";
            };
            destinations.add(new FastTravelDestination(settlement.id(), settlement.label(), settlement.kind(), cost, portalAsset));
        }
        return destinations;
    }

    private WorldMap.SettlementSite currentSettlementSite() {
        for (WorldMap.SettlementSite settlement : world.settlementSites()) {
            if (settlement.id().equals(currentMapId)) {
                return settlement;
            }
        }
        return null;
    }

    private int fastTravelCost(WorldMap.SettlementSite current, WorldMap.SettlementSite target) {
        if (current == null || target == null) {
            return 15;
        }
        int distance = Math.abs(current.x() - target.x()) + Math.abs(current.y() - target.y());
        int cost = 10 + distance / 12;
        if ("City".equals(target.kind()) || "Town".equals(target.kind())) {
            cost += 4;
        }
        if (current.kingdomId().equals(target.kingdomId())) {
            cost = Math.max(8, cost - 3);
        }
        return Math.min(45, Math.max(8, cost));
    }

    public void fastTravelTo(int index) {
        if (mode != GameMode.FAST_TRAVEL) {
            return;
        }
        List<FastTravelDestination> destinations = fastTravelDestinations();
        if (index < 0 || index >= destinations.size()) {
            return;
        }
        FastTravelDestination destination = destinations.get(index);
        if (player.gold < destination.cost()) {
            status = destination.label() + " costs " + destination.cost() + " gold.";
            return;
        }
        TilePoint arrival = world.townPortalArrival(destination.mapId());
        if (arrival == null) {
            status = "That portal is dormant.";
            return;
        }
        player.gold -= destination.cost();
        currentMapId = destination.mapId();
        playerX = arrival.x();
        playerY = arrival.y();
        recordSettlementVisit(currentMapId);
        mode = GameMode.EXPLORE;
        regenerateResourceNodesOnTownEntry();
        status = "Paid " + destination.cost() + " gold. The portal carries you to " + destination.label() + ".";
    }

    public void advanceDialog() {
        if (mode != GameMode.DIALOG || activeNpc == null) {
            return;
        }
        if (!activeNpcIntroLine.isBlank()) {
            activeNpcIntroLine = "";
            dialogIndex = 0;
            status = "Talking to " + activeNpc.name() + ".";
            return;
        }
        if (!activeQuestDialogueLine.isBlank()) {
            CityBuilding shopDialogueBuilding = activeShopDialogueBuilding();
            if (shopDialogueBuilding != null) {
                List<String> options = activeNpcDialogOptions();
                if (!options.isEmpty()) {
                    selectDialogOption(Math.max(0, Math.min(dialogIndex, options.size() - 1)));
                    return;
                }
            }
            clearActiveQuestDialogueLine();
            return;
        }
        Quest quest = questForNpc(activeNpc);
        if (quest != null && quest.ready()) {
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
        if (activeRoamingEventPrompt != null) {
            return activeRoamingEventPrompt.choices().stream()
                    .map(RoamingEventChoice::label)
                    .toList();
        }
        if (activeDialogueSession == null) {
            activeDialogueSession = startDialogueSessionFor(activeNpc);
        }
        if (!activeNpcIntroLine.isBlank()) {
            return List.of("Continue");
        }
        if (!activeQuestDialogueLine.isBlank()) {
            if (activeShopDialogueBuilding() != null) {
                return List.of("Open shop", "Continue");
            }
            return List.of("Continue");
        }
        List<String> options = new ArrayList<>(activeDialogueSession.optionLabels());
        if (activePartyTalkActor != null) {
            return options;
        }
        if (!activeNpcDialogueAtRoot()) {
            return options;
        }
        CityBuilding inquiryBuilding = activeNpcPlaceInquiryBuilding();
        if (inquiryBuilding != null) {
            options.add("Ask about " + generatedBuildingName(inquiryBuilding));
        }
        CityBuilding shopBuilding = activeNpcAssignedShopBuilding();
        if (shopBuilding != null) {
            options.add("Ask about their shop");
        }
        return options;
    }

    public List<DialogueOptionIntent> activeNpcDialogOptionIntents() {
        if (activeNpc == null) {
            return List.of();
        }
        if (activeRoamingEventPrompt != null) {
            return activeRoamingEventPrompt.choices().stream()
                    .map(choice -> new DialogueOptionIntent("action", "", 0))
                    .toList();
        }
        if (activeDialogueSession == null) {
            activeDialogueSession = startDialogueSessionFor(activeNpc);
        }
        if (!activeNpcIntroLine.isBlank()) {
            return List.of(new DialogueOptionIntent("continue", "", 0));
        }
        if (!activeQuestDialogueLine.isBlank()) {
            if (activeShopDialogueBuilding() != null) {
                return List.of(
                        new DialogueOptionIntent("shop", "", 0),
                        new DialogueOptionIntent("continue", "", 0)
                );
            }
            return List.of(new DialogueOptionIntent("continue", "", 0));
        }
        List<DialogueOptionIntent> intents = new ArrayList<>(activeDialogueSession.optionPreviews().stream()
                .map(GameState::dialogueOptionIntent)
                .toList());
        if (activePartyTalkActor != null) {
            return intents;
        }
        if (!activeNpcDialogueAtRoot()) {
            return intents;
        }
        CityBuilding inquiryBuilding = activeNpcPlaceInquiryBuilding();
        if (inquiryBuilding != null) {
            intents.add(new DialogueOptionIntent("info", "", 0));
        }
        CityBuilding shopBuilding = activeNpcAssignedShopBuilding();
        if (shopBuilding != null) {
            intents.add(new DialogueOptionIntent("shop_info", "", 0));
        }
        return intents;
    }

    private static DialogueOptionIntent dialogueOptionIntent(DialogueLibrary.DialogueChoicePreview preview) {
        String effect = preview == null ? "" : preview.effect();
        int delta = preview == null ? 0 : preview.relationshipDelta();
        return new DialogueOptionIntent(dialogueIntentKind(effect, delta), effect, delta);
    }

    private static String dialogueIntentKind(String effect, int relationshipDelta) {
        if (effect == null || effect.isBlank()) {
            if (relationshipDelta > 0) {
                return "rapport_gain";
            }
            if (relationshipDelta < 0) {
                return "rapport_risk";
            }
            return "topic";
        }
        if (effect.startsWith("quest:accept:")) {
            return "quest_accept";
        }
        if (effect.startsWith("quest:turnin:")) {
            return "quest_turnin";
        }
        if (effect.startsWith("quest:outcome:")) {
            return "quest_choice";
        }
        if (effect.startsWith("dialogue_video:")) {
            return "scene";
        }
        if (effect.startsWith("recruit:")) {
            return "recruit";
        }
        if (effect.startsWith("milestone:")) {
            return "milestone";
        }
        return switch (effect) {
            case "romance:start", "romance:date_plan", "romance:date" -> "romance";
            case "romance:end" -> "romance_end";
            case "marriage:accept" -> "marriage";
            case "companion_request:accept" -> "promise";
            default -> relationshipDelta > 0 ? "rapport_gain" : relationshipDelta < 0 ? "rapport_risk" : "action";
        };
    }

    public record DialogueOptionIntent(String kind, String effect, int relationshipDelta) {
        public DialogueOptionIntent {
            kind = kind == null ? "topic" : kind.strip();
            effect = effect == null ? "" : effect.strip();
        }
    }

    public boolean activeNpcIntroductionPending() {
        return activeNpc != null && !activeNpcIntroLine.isBlank();
    }

    public boolean activeNpcDialogueAtRoot() {
        if (activeRoamingEventPrompt != null) {
            return false;
        }
        if (activeNpc == null
                || activeNpcIntroLine != null && !activeNpcIntroLine.isBlank()
                || activeQuestDialogueLine != null && !activeQuestDialogueLine.isBlank()) {
            return false;
        }
        if (activeDialogueSession == null) {
            activeDialogueSession = startDialogueSessionFor(activeNpc);
        }
        return activeDialogueSession.atRoot();
    }

    public String activeNpcDialogLine() {
        return currentActiveDialogueLineParts().combined();
    }

    public String revealedActiveNpcDialogLine() {
        return revealedActiveNpcDialogLineParts().combined();
    }

    public DialogueLibrary.DialogueLine activeNpcDialogLineParts() {
        return currentActiveDialogueLineParts();
    }

    public DialogueLibrary.DialogueLine revealedActiveNpcDialogLineParts() {
        DialogueLibrary.DialogueLine line = currentActiveDialogueLineParts();
        syncDialogueReveal(line);
        return revealedDialogueLine(line);
    }

    public boolean activeDialogueRevealComplete() {
        DialogueLibrary.DialogueLine line = currentActiveDialogueLineParts();
        syncDialogueReveal(line);
        return activeDialogueRevealChars >= dialogueRevealLength(line);
    }

    public void revealActiveDialogueLineInstantly() {
        DialogueLibrary.DialogueLine line = currentActiveDialogueLineParts();
        syncDialogueReveal(line);
        activeDialogueRevealChars = Integer.MAX_VALUE;
    }

    private DialogueLibrary.DialogueLine currentActiveDialogueLineParts() {
        if (activeNpc == null) {
            return new DialogueLibrary.DialogueLine("", "...");
        }
        if (activeRoamingEventPrompt != null) {
            return DialogueLibrary.DialogueLine.from(activeRoamingEventPrompt.description());
        }
        if (activeDialogueSession == null) {
            activeDialogueSession = startDialogueSessionFor(activeNpc);
        }
        if (!activeNpcIntroLine.isBlank()) {
            return DialogueLibrary.DialogueLine.from(activeNpcIntroLine);
        }
        if (!activeQuestDialogueLine.isBlank()) {
            return DialogueLibrary.DialogueLine.from(activeQuestDialogueLine);
        }
        return activeDialogueSession.structuredLine(npcRelationship(activeNpc));
    }

    private void syncDialogueReveal(DialogueLibrary.DialogueLine line) {
        String key = dialogueRevealKey(line);
        if (!key.equals(activeDialogueRevealKey)) {
            activeDialogueRevealKey = key;
            activeDialogueRevealStartedAtMs = System.currentTimeMillis();
            activeDialogueRevealChars = DIALOGUE_REVEAL_INITIAL_CHARS;
        }
        int total = dialogueRevealLength(line);
        if (total <= DIALOGUE_REVEAL_INITIAL_CHARS) {
            activeDialogueRevealChars = Integer.MAX_VALUE;
            return;
        }
        if (activeDialogueRevealChars >= total) {
            return;
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - activeDialogueRevealStartedAtMs);
        int progressed = DIALOGUE_REVEAL_INITIAL_CHARS + (int) (elapsed / DIALOGUE_REVEAL_MS_PER_CHAR);
        activeDialogueRevealChars = Math.min(total, Math.max(activeDialogueRevealChars, progressed));
    }

    private String dialogueRevealKey(DialogueLibrary.DialogueLine line) {
        return (activeNpc == null ? "" : npcRelationshipKey(activeNpc))
                + "|" + (line == null ? "" : line.narration())
                + "\u0000"
                + (line == null ? "" : line.speech());
    }

    private int dialogueRevealLength(DialogueLibrary.DialogueLine line) {
        if (line == null) {
            return 0;
        }
        return line.narration().length() + line.speech().length();
    }

    private DialogueLibrary.DialogueLine revealedDialogueLine(DialogueLibrary.DialogueLine line) {
        if (line == null) {
            return new DialogueLibrary.DialogueLine("", "");
        }
        int remaining = Math.max(0, activeDialogueRevealChars);
        String narration = line.narration();
        String speech = line.speech();
        int narrationChars = Math.min(narration.length(), remaining);
        String revealedNarration = narration.substring(0, narrationChars);
        remaining = Math.max(0, remaining - narration.length());
        int speechChars = Math.min(speech.length(), remaining);
        String revealedSpeech = speech.substring(0, speechChars);
        return new DialogueLibrary.DialogueLine(revealedNarration, revealedSpeech);
    }

    public DialogueVideoPrompt activeDialogueVideo() {
        return activeDialogueVideo;
    }

    public boolean dialogueVideoActive() {
        return activeDialogueVideo != null;
    }

    public void dismissDialogueVideo() {
        if (activeDialogueVideo == null) {
            return;
        }
        String title = activeDialogueVideo.title();
        activeDialogueVideo = null;
        status = title + " settled back into conversation.";
    }

    private void clearActiveQuestDialogueLine() {
        activeQuestDialogueLine = "";
        activeShopDialogueBuildingKey = "";
        dialogIndex = 0;
        activeDialogueRevealKey = "";
        activeDialogueRevealChars = Integer.MAX_VALUE;
        if (activeNpc != null) {
            status = "Talking to " + activeNpc.name() + ".";
        }
    }

    public void selectDialogOption(int index) {
        if (mode != GameMode.DIALOG || activeNpc == null) {
            return;
        }
        if (!activeDialogueRevealComplete()) {
            revealActiveDialogueLineInstantly();
            return;
        }
        if (activeRoamingEventPrompt != null) {
            List<RoamingEventChoice> choices = activeRoamingEventPrompt.choices();
            if (index >= 0 && index < choices.size()) {
                choices.get(index).action().run();
            }
            return;
        }
        if (!activeNpcIntroLine.isBlank()) {
            activeNpcIntroLine = "";
            dialogIndex = 0;
            status = "Talking to " + activeNpc.name() + ".";
            return;
        }
        if (!activeQuestDialogueLine.isBlank()) {
            if (activeShopDialogueBuilding() != null && index == 0) {
                openActiveShopDialogueShop();
                return;
            }
            clearActiveQuestDialogueLine();
            return;
        }
        if (activeDialogueSession == null) {
            activeDialogueSession = startDialogueSessionFor(activeNpc);
        }
        List<String> baseOptions = activeDialogueSession.optionLabels();
        if (activeDialogueSession.atRoot()) {
            int customIndex = baseOptions.size();
            CityBuilding inquiryBuilding = activeNpcPlaceInquiryBuilding();
            if (inquiryBuilding != null) {
                if (index == customIndex) {
                    askActiveNpcAboutPlace(inquiryBuilding);
                    return;
                }
                customIndex++;
            }
            CityBuilding shopBuilding = activeNpcAssignedShopBuilding();
            if (shopBuilding != null && index == customIndex) {
                askActiveNpcAboutAssignedShop(shopBuilding);
                return;
            }
        }
        if (index >= 0 && index < baseOptions.size()) {
            if (!activeDialogueSession.matches(questForNpc(activeNpc))) {
                activeDialogueSession = startDialogueSessionFor(activeNpc);
                status = "The situation changed. Review the current conversation before choosing.";
                return;
            }
            DialogueLibrary.DialogueChoiceResult result = activeDialogueSession.choose(index);
            recordActiveCompanionDialogueTopic(result.repeatKey());
            activeShopDialogueBuildingKey = "";
            if (result.relationshipDelta() != 0) {
                adjustNpcRelationship(activeNpc, result.relationshipDelta());
            }
            if (result.effect() != null && !result.effect().isBlank()) {
                String selectedReply = activeDialogueSession.line(npcRelationship(activeNpc));
                applyDialogueEffect(result.effect());
                if (activeDialogueSession != null && !activeDialogueSession.matches(questForNpc(activeNpc))) {
                    activeDialogueSession = startDialogueSessionFor(activeNpc);
                }
                if (result.effect().startsWith("quest:outcome:")) {
                    String[] decision = result.effect().split(":", 5);
                    Quest decided = decision.length == 5 ? quests.get(decision[2]) : null;
                    String decisionKey = decided == null ? "" : decided.stages.stream()
                            .filter(stage -> stage.id().equals(decision[3])).map(Quest.QuestStage::branchOutcomeKey)
                            .filter(key -> !key.isBlank()).findFirst().orElse(decided.id);
                    if (decided != null && decided.observedStages.contains(decision[3])
                            && decision[4].equals(questBranchOutcomes.get(decisionKey))) {
                        activeQuestDialogueLine = questSpeakerLine(selectedReply);
                    }
                }
            }
            dialogIndex = 0;
            if (result.relationshipDelta() == 0 && (result.effect() == null || result.effect().isBlank())) {
                status = "Talking to " + activeNpc.name() + ".";
            }
        }
    }

    private void recordActiveCompanionDialogueTopic(String repeatKey) {
        if (repeatKey == null || repeatKey.isBlank()) {
            return;
        }
        String recruitId = activeCompanionRecruitId();
        if (recruitId.isBlank()) {
            return;
        }
        String key = recruitId + ":" + repeatKey;
        companionDialogueTopicCounts.merge(key, 1, (oldValue, increment) -> Math.min(999, oldValue + increment));
    }

    public boolean activeNpcCanTeachRecipe() {
        return activeNpc != null && !unlearnedNpcRecipes(activeNpc).isEmpty();
    }

    public void learnRecipeFromActiveNpc() {
        if (mode != GameMode.DIALOG || activeNpc == null) {
            return;
        }
        List<CraftingSystem.Recipe> recipes = unlearnedNpcRecipes(activeNpc);
        if (recipes.isEmpty()) {
            status = activeNpc.name() + " has no new recipes to teach right now.";
            return;
        }
        CraftingSystem.Recipe recipe = recipes.get(0);
        String note = unlockRecipe(recipe.key(), activeNpc.name() + " teaches");
        if (!note.isBlank()) {
            adjustNpcRelationship(activeNpc, 1);
            dialogIndex = 0;
            status = note + " Relationship improved.";
        }
    }

    private List<CraftingSystem.Recipe> unlearnedNpcRecipes(Npc npc) {
        ensureStarterRecipeUnlocks();
        return CraftingSystem.recipesTaughtByNpc(npc).stream()
                .filter(recipe -> !unlockedRecipeKeys.contains(recipe.key()))
                .toList();
    }

    public int npcRelationship(Npc npc) {
        return npcRelationships.getOrDefault(npcRelationshipKey(npc), 0);
    }

    public boolean knowsNpc(Npc npc) {
        return npc != null && introducedNpcKeys.contains(npcRelationshipKey(npc));
    }

    public String npcDisplayName(Npc npc) {
        return knowsNpc(npc) ? npc.name() : "Stranger";
    }

    public String npcKnowledgeKey(Npc npc) {
        return npc == null ? "" : npcRelationshipKey(npc);
    }

    public int npcKnowledgeCount(Npc npc) {
        return npcKnowledge.getOrDefault(npcKnowledgeKey(npc), 0);
    }

    public int npcKnowledgeLevel(Npc npc) {
        int count = npcKnowledgeCount(npc);
        int intelligence = Math.max(1, player.intelligence);
        if (count <= 0) {
            return intelligence >= 12 ? 1 : 0;
        }
        return Math.min(5, 1 + count / 2 + intelligence / 6);
    }

    private void recordNpcKnowledge(Npc npc, int amount) {
        if (npc == null) {
            return;
        }
        String key = npcKnowledgeKey(npc);
        int previous = npcKnowledge.getOrDefault(key, 0);
        int next = Math.min(8, previous + Math.max(1, amount));
        if (next > previous) {
            npcKnowledge.put(key, next);
        }
    }

    private String npcIntroductionLine(Npc npc) {
        NpcBackstories.Profile history = NpcBackstories.profile(npc);
        if (history != null) return history.introduction();
        String sample = npc.dialog().isEmpty() ? "" : stripSpeakerPrefix(npc.dialog().get(0), npc.name());
        String role = npcRoleIntroduction(npc);
        String line = "I am " + npc.name() + role + ".";
        if (!sample.isBlank()) {
            line += " " + sample;
        }
        return line;
    }

    private String stripSpeakerPrefix(String line, String speaker) {
        if (line == null) {
            return "";
        }
        String cleaned = line.strip();
        String prefix = speaker + ":";
        if (cleaned.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return cleaned.substring(prefix.length()).strip();
        }
        return cleaned;
    }

    private String npcRoleIntroduction(Npc npc) {
        if (npc.job() != null) return ", a " + npc.job().roleLabel().toLowerCase(java.util.Locale.ROOT);
        if (npc.recruitId() != null && GameData.RECRUITS.containsKey(npc.recruitId())) {
            return ", a " + GameData.RECRUITS.get(npc.recruitId()).className().toLowerCase();
        }
        if (npc.shopId() != null && GameData.SHOPS.containsKey(npc.shopId())) {
            return ", keeper of " + GameData.SHOPS.get(npc.shopId()).name();
        }
        return "";
    }

    private void adjustNpcRelationship(Npc npc, int delta) {
        String key = npcRelationshipKey(npc);
        int adjustedDelta = relationshipDeltaWithCharisma(delta);
        if (delta > 0 && specialCompanionNpc(npc)) {
            adjustedDelta = Math.max(1, adjustedDelta / 2);
        }
        int previous = npcRelationships.getOrDefault(key, 0);
        int value = Math.max(MIN_NPC_RELATIONSHIP, Math.min(maxRelationshipForNpc(npc), previous + adjustedDelta));
        npcRelationships.put(key, value);
        maybeQueueRelationshipMilestonePrompt(npc, previous, value);
        String direction = adjustedDelta > 0 ? "improved" : "worsened";
        String bonus = adjustedDelta > delta ? " (CHA +" + (adjustedDelta - delta) + ")" : "";
        status = npc.name() + "'s opinion " + direction + " (" + value + ")" + bonus + ".";
    }

    private int relationshipDeltaWithCharisma(int delta) {
        if (delta <= 0) {
            return delta;
        }
        return delta + Math.max(0, (player.charisma - 10) / 4);
    }

    private String npcRelationshipKey(Npc npc) {
        Npc canonical = sourceNpcForState(npc);
        Npc keyed = canonical == null ? npc : canonical;
        String key = rawNpcStateKey(keyed);
        String rawKey = rawNpcStateKey(npc);
        migrateNpcStateKey(rawKey, key);
        return key;
    }

    private String rawNpcStateKey(Npc npc) {
        return npc == null ? "" : npc.mapId() + ":" + npc.name().strip().toLowerCase();
    }

    private void migrateNpcStateKey(String oldKey, String newKey) {
        if (oldKey.isBlank() || newKey.isBlank() || oldKey.equals(newKey)) {
            return;
        }
        Integer oldRelationship = npcRelationships.remove(oldKey);
        if (oldRelationship != null) {
            int existing = npcRelationships.getOrDefault(newKey, oldRelationship);
            npcRelationships.put(newKey, Math.max(existing, oldRelationship));
        }
        Integer oldKnowledge = npcKnowledge.remove(oldKey);
        if (oldKnowledge != null) {
            int existing = npcKnowledge.getOrDefault(newKey, oldKnowledge);
            npcKnowledge.put(newKey, Math.max(existing, oldKnowledge));
        }
        if (introducedNpcKeys.remove(oldKey)) {
            introducedNpcKeys.add(newKey);
        }
        String weeklyQuestId = weeklyNpcQuestIds.remove(oldKey);
        if (weeklyQuestId != null) {
            weeklyNpcQuestIds.putIfAbsent(newKey, weeklyQuestId);
        }
    }

    public static int maxRelationshipForKey(String key) {
        return companionRelationshipKey(key) ? MAX_COMPANION_TRUST : MAX_NPC_RELATIONSHIP;
    }

    private int maxRelationshipForNpc(Npc npc) {
        return specialCompanionNpc(npc) ? MAX_COMPANION_TRUST : MAX_NPC_RELATIONSHIP;
    }

    public boolean isRomancedCompanion(String recruitId) {
        return recruitId != null && romancedCompanionIds.contains(recruitId);
    }

    public boolean isMarriedCompanion(String recruitId) {
        return recruitId != null && marriedCompanionIds.contains(recruitId);
    }

    private void applyDialogueEffect(String effect) {
        if (effect != null && effect.startsWith("quest:discuss:")) {
            String[] parts = effect.split(":", 4);
            Quest quest = parts.length == 4 ? quests.get(parts[2]) : null;
            if (quest != null && quest.accepted && !quest.completed && !quest.ready()
                    && quest.activeStage().id().equals(parts[3]) && quest.activeObjectiveKind() != Quest.ObjectiveKind.CHOICE
                    && quest.activeObjectiveKind().conversationObjective() && conversationObjectiveMatches(quest, activeNpc)) {
                String result = quest.activeReadyDialog();
                String stageId = quest.activeStage().id();
                if (!CompanionQuestContent.canHandOver(quest)) {
                    activeQuestDialogueLine = "The required supplies or preparation are missing. " + quest.activeProgressDialog();
                    status = activeQuestDialogueLine;
                    return;
                }
                String note = recordConversationQuestObjective(quest, activeNpc);
                activeQuestDialogueLine = quest.observedStages.contains(stageId) ? questSpeakerLine(result)
                        : note.isBlank() ? "We have already recorded this exchange. Speak to another marked contact."
                        : "Your account is recorded. We still need the other marked contacts before drawing a conclusion.";
                status = note;
                activeDialogueSession = startDialogueSessionFor(activeNpc);
            } else {
                activeQuestDialogueLine = "That exchange is no longer available. Check the current objective.";
            }
            return;
        }
        if (effect != null && effect.startsWith("dialogue_video:")) {
            triggerDialogueVideo(effect.substring("dialogue_video:".length()).strip());
            return;
        }
        if (effect != null && effect.startsWith("quest:accept:")) {
            applyQuestAcceptEffect(effect);
            return;
        }
        if (effect != null && effect.startsWith("quest:turnin:")) {
            applyQuestTurnInEffect(effect);
            return;
        }
        if (effect != null && effect.startsWith("quest:outcome:")) {
            applyQuestOutcomeEffect(effect);
            return;
        }
        if (effect != null && effect.startsWith("recruit:")) {
            applyRecruitEffect(effect);
            return;
        }
        String recruitId = activeCompanionRecruitId();
        if (recruitId.isBlank()) {
            return;
        }
        if (effect != null && effect.startsWith("milestone:")) {
            applyRelationshipMilestoneEffect(recruitId, effect);
            return;
        }
        String companionName = companionName(recruitId);
        switch (effect) {
            case "romance:start" -> {
                romancedCompanionIds.add(recruitId);
                publishCompanionEvent("romance:start:" + recruitId, "Chose affection with " + companionName,
                        List.of("commitment", "loyalty", "mercy"), 1, "relationship",
                        "Chose affection honestly.", true);
                status = companionName + "'s affection deepens.";
            }
            case "romance:end" -> {
                romancedCompanionIds.remove(recruitId);
                publishCompanionEvent("romance:end:" + recruitId, "Asked " + companionName + " for space",
                        List.of("truth", "patience"), 0, "relationship",
                        "Chose distance instead of pretending affection.", true);
                status = companionName + " gives you space.";
            }
            case "romance:date_plan" -> {
                recordCompanionMemory(recruitId, "romance", "Planned a quiet date with the player.");
                status = companionName + " agrees to find a quieter place with you.";
            }
            case "romance:date" -> {
                romancedCompanionIds.add(recruitId);
                recordCompanionMemory(recruitId, "romance", "Shared a quiet date with the player.");
                publishCompanionEvent("romance:date:" + recruitId, "Shared a quiet date with " + companionName,
                        List.of("commitment", "loyalty", "patience"), 1, "relationship",
                        "Shared a quiet date with the player.", true);
                status = companionName + " will remember this quiet hour with you.";
            }
            case "marriage:accept" -> {
                romancedCompanionIds.add(recruitId);
                marriedCompanionIds.add(recruitId);
                publishCompanionEvent("marriage:accept:" + recruitId, "Made a promise with " + companionName,
                        List.of("commitment", "loyalty", "oathstead"), 2, "relationship",
                        "Made a lasting promise at Oathstead.", true);
                status = companionName + " accepts your proposal. Oathstead will have another promise to celebrate.";
            }
            case "companion_request:accept" -> {
                status = acceptSoftCompanionRequest(recruitId);
            }
            default -> {
            }
        }
    }

    private void triggerDialogueVideo(String rawKey) {
        String key = rawKey == null || rawKey.isBlank() ? "dialogue:scene" : rawKey.strip();
        String[] parts = key.split(":");
        String sceneType = parts.length > 0 && !parts[0].isBlank() ? parts[0] : "dialogue";
        String recruitId = parts.length > 1 && !parts[1].isBlank() ? parts[1] : activeCompanionRecruitId();
        String companionName = recruitId == null || recruitId.isBlank() ? "this conversation" : companionName(recruitId);
        String safeKey = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "_");
        String title = switch (sceneType) {
            case "flirt" -> "A Quiet Look with " + companionName;
            case "milestone" -> "A Turning Point with " + companionName;
            case "quest" -> "A Story Beat with " + companionName;
            default -> "A Dialogue Scene";
        };
        String caption = switch (sceneType) {
            case "flirt" -> companionName + " lets the moment linger instead of turning it back into work.";
            case "milestone" -> companionName + " stays with the choice long enough for it to matter.";
            case "quest" -> companionName + " carries the quest's consequence into a more intimate scene.";
            default -> "The conversation pauses for a staged scene.";
        };
        activeDialogueVideo = new DialogueVideoPrompt(key, title, caption, "assets/videos/dialogue/" + safeKey + ".mp4");
        if (recruitId != null && !recruitId.isBlank()) {
            recordCompanionMemory(recruitId, sceneType, "Shared a dialogue scene with the player: " + title + ".");
        }
        status = title + " is ready to play.";
    }

    private void applyRecruitEffect(String effect) {
        if (activeNpc == null || effect == null) {
            return;
        }
        String recruitId = effect.substring("recruit:".length()).strip();
        if (recruitId.isBlank() || !recruitId.equals(activeNpc.recruitId())) {
            return;
        }
        if (!companionRecruitmentEligible(recruitId, activeNpc)) {
            status = activeNpc.name() + " needs relationship " + COMPANION_RECRUIT_RELATIONSHIP
                    + " or one completed companion chapter before joining. Current: " + npcRelationship(activeNpc) + ".";
            return;
        }
        String recruitNote = recruitAlly(recruitId);
        status = recruitNote == null ? activeNpc.name() + " cannot join right now." : recruitNote;
        activeDialogueSession = startDialogueSessionFor(activeNpc);
    }

    private void applyQuestAcceptEffect(String effect) {
        if (activeNpc == null || effect == null) {
            return;
        }
        String questId = effect.substring("quest:accept:".length()).strip();
        Quest quest = questForNpc(activeNpc);
        if (quest == null || quest.accepted || quest.completed || !quest.id.equals(questId)) {
            return;
        }
        handleActiveNpcQuestAction();
    }

    private void applyQuestTurnInEffect(String effect) {
        if (activeNpc == null || effect == null) {
            return;
        }
        String questId = effect.substring("quest:turnin:".length()).strip();
        Quest quest = questForNpc(activeNpc);
        if (quest == null || !quest.ready() || quest.completed || !quest.id.equals(questId)) {
            return;
        }
        handleActiveNpcQuestAction();
    }

    private void applyQuestOutcomeEffect(String effect) {
        if (effect == null) {
            return;
        }
        String[] fields = effect.split(":", 5);
        if (fields.length != 5) {
            return;
        }
        Quest quest = quests.get(fields[2].strip());
        if (quest == null || !quest.accepted || quest.completed || quest.ready()
                || quest.activeObjectiveKind() != Quest.ObjectiveKind.CHOICE
                || !quest.activeStage().id().equals(fields[3])
                || activeNpc == null || !conversationObjectiveMatches(quest, activeNpc)) {
            return;
        }
        String outcome = normalizeQuestOutcome(fields[4]);
        if (outcome.isBlank()) return;
        String recorded = questBranchOutcomes.get(quest.outcomeKey());
        if (recorded != null) {
            if (!recorded.equals(outcome)) return;
            // Revised segments may need new evidence, but an old decision remains binding.
            quest.recordConversation();
            invalidateQuestObjectiveCache();
            status = "Your earlier decision is retained: " + readableQuestOutcome(recorded) + ".";
            if (quest.ready()) autoAdvanceReadyQuestStage(quest);
            return;
        }
        if (!DialogueLibrary.allowedQuestOutcome(activeNpc, quest, outcome)) return;
        questBranchOutcomes.put(quest.outcomeKey(), outcome);
        if (quest.accepted && !quest.ready() && quest.activeObjectiveKind() == Quest.ObjectiveKind.CHOICE) {
            quest.recordConversation();
            invalidateQuestObjectiveCache();
        }
        String label = readableQuestOutcome(outcome);
        if (quest.companionQuest() && quest.chainOwnerId != null && !quest.chainOwnerId.isBlank()) {
            recordCompanionMemory(quest.chainOwnerId, "quest_choice", companionQuestChoiceMemory(quest, outcome, label));
        }
        publishCompanionEvent("quest_outcome:" + quest.outcomeKey() + ":" + outcome,
                "Chose " + label + " in " + quest.title,
                List.of("truth", "choice", "consequence", quest.companionQuest() ? "companion_quest" : "local"),
                quest.companionQuest() ? 1 : 0,
                "quest_choice",
                "Chose " + label + " during " + quest.title + ".",
                quest.companionQuest());
        status = "Choice recorded: " + label + ".";
        if (quest.ready()) {
            if (autoAdvanceReadyQuestStage(quest)) {
                return;
            }
            status += " " + quest.activeReadyDialog();
        }
    }

    private String companionQuestStartMemory(Quest quest) {
        String title = quest == null ? "the work" : quest.title;
        String owner = quest == null ? "" : quest.chainOwnerId;
        return switch (owner == null ? "" : owner) {
            case "seraphine" -> "Agreed to help Seraphine expose the first lie behind " + title + " without dressing it as heroics.";
            case "maera" -> "Agreed to help Maera keep the map, route record, and witness in " + title + " together before the Archive could separate them.";
            case "cassia" -> "Agreed to stand with Cassia at the breach opened by " + title + ".";
            case "lyra" -> "Agreed to help Lyra before " + title + " could turn more people into patients.";
            case "samir" -> "Agreed to carry Samir's question through " + title + " without forcing easy certainty.";
            case "aria" -> "Accepted Aria's warning that the road in " + title + " might be bait and chose to follow anyway.";
            case "vesper" -> "Agreed to help Vesper inspect the Snowrest road breaks and trace black sap back through " + title + ".";
            case "rafiq" -> "Agreed to help Rafiq face the truth behind " + title + " before charm could improve the story.";
            case "calder" -> "Agreed to help Calder carry the weight behind " + title + " with both hands.";
            default -> "Started " + title + " together.";
        };
    }

    private String companionQuestStageMemory(Quest quest, String completedStage) {
        String title = quest == null ? "the quest" : quest.title;
        String stage = completedStage == null || completedStage.isBlank() ? "a stage" : completedStage;
        String owner = quest == null ? "" : quest.chainOwnerId;
        return switch (owner == null ? "" : owner) {
            case "seraphine" -> "Finished " + stage + " in " + title + "; Seraphine noticed which piece of ink finally made the lie nervous.";
            case "maera" -> "Finished " + stage + " in " + title + "; Maera kept the physical evidence and witness account in the same record.";
            case "cassia" -> "Finished " + stage + " in " + title + "; Cassia watched the facts hold under pressure.";
            case "lyra" -> "Finished " + stage + " in " + title + "; Lyra remembered who would have suffered if the player had waited.";
            case "samir" -> "Finished " + stage + " in " + title + "; Samir let the evidence trouble the old lesson.";
            case "aria" -> "Finished " + stage + " in " + title + "; Aria noticed the player reading the second trail before trusting the first.";
            case "vesper" -> "Finished " + stage + " in " + title + "; Vesper saw the player check sap, witnesses, and risk before cutting roots.";
            case "rafiq" -> "Finished " + stage + " in " + title + "; Rafiq remembered the moment truth became harder to joke around.";
            case "calder" -> "Finished " + stage + " in " + title + "; Calder saw the player check the strain before praising the repair.";
            default -> "Completed " + stage + " in " + title + ".";
        };
    }

    private String companionQuestCompletionMemory(Quest quest) {
        String title = quest == null ? "the quest" : quest.title;
        String owner = quest == null ? "" : quest.chainOwnerId;
        return switch (owner == null ? "" : owner) {
            case "seraphine" -> "Completed " + title + "; Seraphine keeps thinking about the moment the player made the lie answer back.";
            case "maera" -> "Completed " + title + "; Maera remembers the player preserving the proof before deciding who should hear it.";
            case "cassia" -> "Completed " + title + "; Cassia remembers that the player stood where the line actually moved.";
            case "lyra" -> "Completed " + title + "; Lyra remembers the player choosing care while there was still time for it to matter.";
            case "samir" -> "Completed " + title + "; Samir remembers the player letting doubt become guidance instead of shame.";
            case "aria" -> "Completed " + title + "; Aria remembers the player spotting the false trail and still walking beside her.";
            case "vesper" -> "Completed " + title + "; Vesper remembers the player tracing the road damage back to the sealed grove.";
            case "rafiq" -> "Completed " + title + "; Rafiq remembers the player staying when the truth stopped being charming.";
            case "calder" -> "Completed " + title + "; Calder remembers the player helping carry the weight after the first crack showed.";
            default -> "Completed " + title + " together.";
        };
    }

    private String companionQuestChoiceMemory(Quest quest, String outcome, String label) {
        String title = quest == null ? "the choice" : quest.title;
        String owner = quest == null ? "" : quest.chainOwnerId;
        String choice = label == null || label.isBlank() ? readableQuestOutcome(outcome) : label;
        return switch (owner == null ? "" : owner) {
            case "seraphine" -> seraphineQuestChoiceMemory(title, outcome, choice);
            case "maera" -> "Chose " + choice + " during " + title + "; Maera recorded which proof was protected, which witness was risked, and why.";
            case "cassia" -> "Chose " + choice + " during " + title + "; Cassia measured the choice by who had to stand behind it.";
            case "lyra" -> "Chose " + choice + " during " + title + "; Lyra remembers who the choice protected and who it could not.";
            case "samir" -> "Chose " + choice + " during " + title + "; Samir kept returning to the mercy or truth inside the decision.";
            case "aria" -> "Chose " + choice + " during " + title + "; Aria watched whether the player named the trap or stepped around it.";
            case "vesper" -> "Chose " + choice + " during " + title + "; Vesper remembers what happened to the road, grove, and spring afterward.";
            case "rafiq" -> "Chose " + choice + " during " + title + "; Rafiq noticed whether the truth survived style.";
            case "calder" -> "Chose " + choice + " during " + title + "; Calder remembers whether the decision could bear weight afterward.";
            default -> "Chose " + choice + " during " + title + ".";
        };
    }

    private String seraphineQuestChoiceMemory(String title, String outcome, String choice) {
        String detail = switch (outcome == null ? "" : outcome) {
            case "clause_copied" -> "Seraphine noticed that leverage came before theater.";
            case "public_record" -> "Seraphine noticed that public truth still needed exits for the vulnerable.";
            case "witness_first" -> "Seraphine noticed that the living witness came before the beautiful proof.";
            case "witness_protected" -> "Seraphine remembered the clerk being protected before his testimony was useful.";
            case "testimony_public" -> "Seraphine remembered the testimony being made too public to quietly erase.";
            case "leverage_traded" -> "Seraphine remembered the risk of making a frightened clerk useful.";
            case "survival_named" -> "Seraphine remembered the player naming desperation without calling it consent.";
            case "legal_lie_named" -> "Seraphine remembered the contract being named legal enough to wound and false enough to fight.";
            case "blame_signed" -> "Seraphine remembered the old shame being placed too near the people trapped by it.";
            case "ledger_published" -> "Seraphine remembered the contract burning while the ledger became public evidence.";
            case "names_reclaimed" -> "Seraphine remembered the records being kept where victims could reclaim their names.";
            case "safety_bargain" -> "Seraphine remembered safety being bought with leverage and counted the later cost.";
            case "records_burned" -> "Seraphine remembered the clean fire and the names that still needed proof.";
            case "refuge_ledger" -> "Seraphine remembered Oathstead's ledger desk being built for people leaving chains.";
            case "witness_bench" -> "Seraphine remembered witness days being placed before any new oath.";
            case "chosen_daily" -> "Seraphine remembered the promise being left open enough to choose again.";
            default -> "Seraphine noticed whether freedom was treated as a fact or a performance.";
        };
        return "Chose " + choice + " during " + title + "; " + detail;
    }

    private String normalizeQuestOutcome(String outcome) {
        if (outcome == null) {
            return "";
        }
        return outcome.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_\\-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .strip();
    }

    public String questBranchOutcome(Quest quest) {
        return quest == null ? "" : questBranchOutcomes.getOrDefault(quest.outcomeKey(), "");
    }

    public String questBranchOutcome(String outcomeKey) {
        return outcomeKey == null || outcomeKey.isBlank() ? "" : questBranchOutcomes.getOrDefault(outcomeKey, "");
    }

    public String readableQuestOutcome(String outcome) {
        if (outcome == null || outcome.isBlank()) {
            return "undecided";
        }
        String[] parts = outcome.replace('-', '_').split("_+");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1).toLowerCase(Locale.ROOT));
        }
        return words.isEmpty() ? outcome : String.join(" ", words);
    }

    private void applyRelationshipMilestoneEffect(String recruitId, String effect) {
        try {
            int threshold = Integer.parseInt(effect.substring("milestone:".length()));
            if (threshold <= 0) {
                return;
            }
            String key = relationshipMilestoneKey(recruitId, threshold);
            if (companionRelationshipMilestones.add(key)) {
                String label = relationshipMilestoneLabel(threshold);
                recordCompanionMemory(recruitId, "relationship_milestone", "Reached " + label + " with the player.");
                publishCompanionEvent("relationship_milestone:" + recruitId + ":" + threshold,
                        "Reached " + label + " with " + companionName(recruitId),
                        List.of("loyalty", "commitment"), 1, "relationship_milestone",
                        "Reached " + label + " with the player.", true);
                status = companionName(recruitId) + " will remember this " + label + ".";
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private String activeCompanionRecruitId() {
        Npc canonical = sourceNpcForState(activeNpc);
        if (canonical != null && canonical.recruitId() != null) {
            return canonical.recruitId();
        }
        return activeNpc != null && activeNpc.recruitId() != null ? activeNpc.recruitId() : "";
    }

    private String companionName(String recruitId) {
        GameData.RecruitSpec spec = GameData.RECRUITS.get(recruitId);
        return spec == null ? "Your companion" : spec.name();
    }

    private Npc canonicalCompanionNpc(Npc npc) {
        if (npc == null) {
            return null;
        }
        if (npc.recruitId() != null && !npc.recruitId().isBlank()) {
            for (Npc candidate : GameData.NPCS) {
                if (npc.recruitId().equals(candidate.recruitId())) {
                    return candidate;
                }
            }
        }
        for (Npc candidate : GameData.NPCS) {
            if (candidate.recruitId() != null && candidate.name().equals(npc.name())) {
                return candidate;
            }
        }
        return null;
    }

    private Npc sourceNpcForState(Npc npc) {
        Npc canonical = canonicalCompanionNpc(npc);
        if (canonical != null) {
            return canonical;
        }
        Npc questSource = questNpcSourceFor(npc);
        if (questSource != null) {
            return questSource;
        }
        Npc commuterSource = commuterSourceNpcFor(npc);
        if (commuterSource != null) {
            return commuterSource;
        }
        if (npc == null || !syntheticVillageAllyNpc(npc)) {
            return null;
        }
        Actor ally = allyByName(npc.name());
        return ally == null ? null : npcForAlly(ally);
    }

    private boolean syntheticVillageAllyNpc(Npc npc) {
        if (npc == null) {
            return false;
        }
        if (activePartyTalkActor != null && npc == activeNpc) {
            return true;
        }
        if (WorldMap.PLAYER_VILLAGE_ID.equals(npc.mapId())) {
            return true;
        }
        return npc.mapId() != null && npc.mapId().startsWith("house_" + WorldMap.PLAYER_VILLAGE_ID + "_");
    }

    private boolean specialCompanionNpc(Npc npc) {
        Npc canonical = sourceNpcForState(npc);
        if (canonical == null || canonical.recruitId() == null) {
            return false;
        }
        for (Quest quest : quests.values()) {
            if (canonical.recruitId().equals(quest.chainOwnerId) && quest.companionQuest()) {
                return true;
            }
        }
        return false;
    }

    private static boolean companionRelationshipKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = key.strip().toLowerCase();
        for (Npc npc : GameData.NPCS) {
            if (npc.recruitId() == null || npc.recruitId().isBlank() || !companionRecruitId(npc.recruitId())) {
                continue;
            }
            String npcKey = npc.mapId() + ":" + npc.name().strip().toLowerCase();
            if (npcKey.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static boolean companionRecruitId(String recruitId) {
        for (Quest quest : GameData.QUESTS.values()) {
            if (recruitId.equals(quest.chainOwnerId) && quest.companionQuest()) {
                return true;
            }
        }
        return false;
    }

    private String npcBiomeContext(Npc npc) {
        char npcTile = WorldMap.OVERWORLD_ID.equals(npc.mapId())
                ? world.tileAt(npc.mapId(), npc.x(), npc.y())
                : currentBiomeTile();
        return DialogueLibrary.biomeContext(npc, npcTile, currentBiomeTile());
    }

    private DialogueLibrary.DialogueSession startDialogueSessionFor(Npc npc) {
        Quest current = questForNpc(npc);
        return DialogueLibrary.startSession(
                npc,
                current,
                npcBiomeContext(npc),
                npcRelationship(npc),
                random,
                dialogueContextFor(npc),
                precedingQuestReport(npc, current)
        );
    }

    private String precedingQuestReport(Npc npc, Quest current) {
        Quest first = npc.questId() == null ? null : quests.get(npc.questId());
        if (current == null || first == null || current.chainOwnerId == null
                || !current.chainOwnerId.equals(first.chainOwnerId)) return "";
        return quests.values().stream().filter(q -> q.completed && current.id.equals(q.nextQuestId))
                .filter(q -> current.chainOwnerId.equals(q.chainOwnerId))
                .map(q -> QuestNarrative.clean(q.stages.getLast().completeDialog())).findFirst().orElse("");
    }

    private DialogueLibrary.DialogueContext dialogueContextFor(Npc npc) {
        String recruitId = companionRecruitIdForContext(npc);
        boolean recruited = !recruitId.isBlank() && isRecruited(recruitId);
        boolean romanced = !recruitId.isBlank() && isRomancedCompanion(recruitId);
        boolean married = !recruitId.isBlank() && isMarriedCompanion(recruitId);
        Actor ally = companionActorForContext(npc, recruitId);
        boolean stationed = ally != null && villageAllies.contains(ally.name);
        String buildingLabel = "";
        String roleLabel = "";
        if (stationed) {
            String buildingKey = buildingAssignmentForAlly(ally.name);
            CityBuilding building = buildingKey.isBlank() ? null : playerVillageBuildingByKey(buildingKey);
            if (building != null) {
                buildingLabel = VillageManager.buildingLabel(building.style());
            }
            String roleId = villageWorkerRoles.getOrDefault(ally.name, "idle");
            roleLabel = VillageManager.workerRole(roleId).label();
        }
        return new DialogueLibrary.DialogueContext(
                recruited,
                !recruited && companionRecruitmentEligible(recruitId, npc),
                stationed,
                romanced,
                married,
                buildingLabel,
                roleLabel,
                knownCompanionNamesForDialogue(),
                recentCompanionMemoryTexts(recruitId),
                sharedTableConversationContext(),
                sharedTableConversationLabel(),
                companionApprovalMotive(recruitId),
                companionEpilogueReady(),
                activeSoftRequestText(recruitId),
                relationshipMilestoneResolved(recruitId, 150),
                relationshipMilestoneResolved(recruitId, 180),
                relationshipMilestoneResolved(recruitId, 250),
                pendingRelationshipMilestone(recruitId, npcRelationship(npc)),
                relationshipMilestoneLabel(pendingRelationshipMilestone(recruitId, npcRelationship(npc))),
                companionDialogueTopicCountsFor(recruitId),
                questBranchOutcomes
        );
    }

    private Map<String, Integer> companionDialogueTopicCountsFor(String recruitId) {
        if (recruitId == null || recruitId.isBlank()) {
            return Map.of();
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        String prefix = recruitId + ":";
        for (var entry : companionDialogueTopicCounts.entrySet()) {
            if (entry.getKey().startsWith(prefix) && entry.getValue() > 0) {
                counts.put(entry.getKey().substring(prefix.length()), entry.getValue());
            }
        }
        return counts;
    }

    private boolean relationshipMilestoneResolved(String recruitId, int threshold) {
        return recruitId != null
                && !recruitId.isBlank()
                && companionRelationshipMilestones.contains(relationshipMilestoneKey(recruitId, threshold));
    }

    private boolean companionEpilogueReady() {
        Quest gateQuest = quests.get("ms_twelve_stones_gate");
        return demonQueenDefeated || gateQuest != null && gateQuest.completed;
    }

    private String companionApprovalMotive(String recruitId) {
        if (recruitId == null || recruitId.isBlank()) {
            return "trust";
        }
        return companionApprovalProfile(recruitId).motive();
    }

    private int pendingRelationshipMilestone(String recruitId, int relationship) {
        if (recruitId == null || recruitId.isBlank()) {
            return 0;
        }
        for (int threshold : new int[]{50, 100, 150, 180, 250}) {
            if (relationship >= threshold && !companionRelationshipMilestones.contains(relationshipMilestoneKey(recruitId, threshold))) {
                return threshold;
            }
        }
        return 0;
    }

    private String relationshipMilestoneLabel(int threshold) {
        return switch (threshold) {
            case 50 -> "where we stand";
            case 100 -> "what changed between us";
            case 150 -> "staying together by choice";
            case 180 -> "what this feeling is becoming";
            case 250 -> "the future";
            default -> "";
        };
    }

    private String relationshipMilestoneKey(String recruitId, int threshold) {
        return recruitId + ":" + threshold;
    }

    private boolean sharedTableConversationContext() {
        return !sharedTableConversationLabel().isBlank();
    }

    private String sharedTableConversationLabel() {
        if (WorldMap.PLAYER_VILLAGE_ID.equals(currentMapId) || currentMapId.startsWith("house_" + WorldMap.PLAYER_VILLAGE_ID + "_")) {
            return "Oathstead";
        }
        CityBuilding interiorBuilding = buildingForInteriorMap(currentMapId);
        if (socialRestBuilding(interiorBuilding)) {
            return generatedBuildingName(interiorBuilding);
        }
        String label = world.label(currentMapId);
        String lowerLabel = label.toLowerCase();
        if (lowerLabel.contains("inn") || lowerLabel.contains("tavern")) {
            return label;
        }
        if (isSettlementMap(currentMapId)) {
            TilePoint player = new TilePoint(playerX, playerY);
            CityBuilding nearest = null;
            int bestDistance = Integer.MAX_VALUE;
            for (CityBuilding building : world.cityBuildings(currentMapId)) {
                if (!socialRestBuilding(building)) {
                    continue;
                }
                int distance = distanceToBuilding(player, building);
                if (distance < bestDistance) {
                    nearest = building;
                    bestDistance = distance;
                }
            }
            if (nearest != null && bestDistance <= 6) {
                return generatedBuildingName(nearest);
            }
        }
        return "";
    }

    private boolean socialRestBuilding(CityBuilding building) {
        if (building == null) {
            return false;
        }
        String style = building.style();
        String key = building.key() == null ? "" : building.key().toLowerCase();
        String generated = generatedBuildingName(building).toLowerCase();
        return "inn".equals(style)
                || "restaurant".equals(style)
                || key.contains("inn")
                || key.contains("tavern")
                || generated.contains("inn")
                || generated.contains("tavern");
    }

    private String companionRecruitIdForContext(Npc npc) {
        Npc canonical = sourceNpcForState(npc);
        if (canonical != null && canonical.recruitId() != null) {
            return canonical.recruitId();
        }
        return npc != null && npc.recruitId() != null ? npc.recruitId() : "";
    }

    private Actor companionActorForContext(Npc npc, String recruitId) {
        if (recruitId != null && !recruitId.isBlank()) {
            GameData.RecruitSpec spec = GameData.RECRUITS.get(recruitId);
            if (spec != null) {
                Actor ally = allyByName(spec.name());
                if (ally != null) {
                    return ally;
                }
            }
        }
        return npc == null ? null : allyByName(npc.name());
    }

    private List<String> knownCompanionNamesForDialogue() {
        List<String> names = new ArrayList<>();
        for (Npc npc : GameData.NPCS) {
            if (npc.recruitId() == null || npc.recruitId().isBlank() || !companionRecruitId(npc.recruitId())) {
                continue;
            }
            boolean known = isRecruited(npc.recruitId())
                    || introducedNpcKeys.contains(npcRelationshipKey(npc))
                    || npcRelationships.containsKey(npcRelationshipKey(npc))
                    || companionQuestTouched(npc.recruitId());
            if (known && !names.contains(npc.name())) {
                names.add(npc.name());
            }
        }
        return names;
    }

    private boolean companionQuestTouched(String recruitId) {
        for (Quest quest : quests.values()) {
            if (recruitId.equals(quest.chainOwnerId) && (quest.accepted || quest.completed || quest.progress > 0)) {
                return true;
            }
        }
        return false;
    }

    public void recordCompanionMemory(String recruitId, String category, String text) {
        if (recruitId == null || recruitId.isBlank() || !GameData.RECRUITS.containsKey(recruitId)
                || text == null || text.isBlank()) {
            return;
        }
        String cleanedCategory = category == null || category.isBlank() ? "event" : category.strip();
        String cleanedText = text.replaceAll("\\s+", " ").strip();
        List<CompanionMemory> memories = companionMemoryJournal.computeIfAbsent(recruitId, ignored -> new ArrayList<>());
        if (!memories.isEmpty()) {
            CompanionMemory latest = memories.get(memories.size() - 1);
            if (latest.category().equals(cleanedCategory) && latest.text().equals(cleanedText)) {
                return;
            }
        }
        memories.add(new CompanionMemory(worldTick, cleanedCategory, cleanedText));
        while (memories.size() > MAX_COMPANION_MEMORIES) {
            memories.remove(0);
        }
    }

    public List<CompanionMemory> companionMemories(String recruitId) {
        return List.copyOf(companionMemoryJournal.getOrDefault(recruitId, List.of()));
    }

    public String latestCompanionMemoryText(String recruitId) {
        List<CompanionMemory> memories = companionMemoryJournal.getOrDefault(recruitId, List.of());
        return memories.isEmpty() ? "" : memories.get(memories.size() - 1).text();
    }

    private List<String> recentCompanionMemoryTexts(String recruitId) {
        if (recruitId == null || recruitId.isBlank()) {
            return List.of();
        }
        List<CompanionMemory> memories = companionMemoryJournal.getOrDefault(recruitId, List.of());
        if (memories.isEmpty()) {
            return List.of();
        }
        List<String> recent = new ArrayList<>();
        for (int i = memories.size() - 1; i >= 0 && recent.size() < 4; i--) {
            String text = memories.get(i).text();
            if (!text.isBlank() && !recent.contains(text)) {
                recent.add(text);
            }
        }
        return recent;
    }

    private void recordCompanionMemoryForNpc(Npc npc, String category, String text) {
        String recruitId = companionRecruitIdForContext(npc);
        if (!recruitId.isBlank()) {
            recordCompanionMemory(recruitId, category, text);
        }
    }

    private void recordCompanionMemoryForAlly(Actor ally, String category, String text) {
        String recruitId = recruitIdForAlly(ally);
        if (!recruitId.isBlank()) {
            recordCompanionMemory(recruitId, category, text);
        }
    }

    private void recordMainStoryMemory(String text) {
        for (String recruitId : recruitedIds) {
            recordCompanionMemory(recruitId, "main_story", text);
        }
    }

    private String activeSoftRequestText(String recruitId) {
        if (recruitId == null || recruitId.isBlank()) {
            return "";
        }
        SoftCompanionRequest request = softCompanionRequest(recruitId, companionSoftRequests.getOrDefault(recruitId, ""));
        if (request != null && fulfilledCompanionSoftRequests.contains(softRequestKey(recruitId, request.id()))) {
            return "";
        }
        return request == null ? "" : request.prompt();
    }

    private SoftCompanionRequest activeSoftRequest(String recruitId) {
        if (recruitId == null || recruitId.isBlank()) {
            return null;
        }
        String requestId = companionSoftRequests.getOrDefault(recruitId, "");
        SoftCompanionRequest active = softCompanionRequest(recruitId, requestId);
        if (active != null && !fulfilledCompanionSoftRequests.contains(softRequestKey(recruitId, active.id()))) {
            return active;
        }
        for (SoftCompanionRequest request : softCompanionRequestsFor(recruitId)) {
            if (!fulfilledCompanionSoftRequests.contains(softRequestKey(recruitId, request.id()))) {
                return request;
            }
        }
        return null;
    }

    private String acceptSoftCompanionRequest(String recruitId) {
        SoftCompanionRequest request = activeSoftRequest(recruitId);
        String companionName = companionName(recruitId);
        if (request == null) {
            return companionName + " has no more personal requests right now.";
        }
        String key = softRequestKey(recruitId, request.id());
        if (request.id().equals(companionSoftRequests.get(recruitId)) && !fulfilledCompanionSoftRequests.contains(key)) {
            return companionName + " knows you are still carrying that request: " + request.prompt();
        }
        companionSoftRequests.put(recruitId, request.id());
        recordCompanionMemory(recruitId, "request", request.acceptMemory());
        publishCompanionEvent("companion_request:" + recruitId + ":" + request.id(),
                "Accepted " + companionName + "'s request",
                List.of("loyalty", "commitment"), 1, "request", request.acceptMemory(), true);
        return companionName + " trusts you with a personal request: " + request.prompt();
    }

    private void fulfillSoftCompanionRequestsForEvent(List<String> eventTags, String detail) {
        if (eventTags == null || eventTags.isEmpty()) {
            return;
        }
        for (Actor ally : activeAllies()) {
            String recruitId = recruitIdForAlly(ally);
            if (recruitId.isBlank()) {
                continue;
            }
            SoftCompanionRequest request = activeSoftRequest(recruitId);
            if (request == null || !request.id().equals(companionSoftRequests.get(recruitId))) {
                continue;
            }
            String key = softRequestKey(recruitId, request.id());
            if (fulfilledCompanionSoftRequests.contains(key) || request.tags().stream().noneMatch(eventTags::contains)) {
                continue;
            }
            fulfilledCompanionSoftRequests.add(key);
            companionSoftRequests.remove(recruitId);
            recordCompanionMemory(recruitId, "request_fulfilled", request.fulfillMemory(detail));
            adjustNpcRelationshipDirect(npcForAlly(ally), 3);
            enqueueCompanionComment(new PendingCompanionComment(
                    ally.name,
                    ally.name + ": You remembered what I asked. That matters more than doing it perfectly.",
                    List.of("I meant to.", "You matter.", "Keep me honest."),
                    List.of(
                            "Affirms that you honored the request deliberately. Builds trust.",
                            "Makes the answer personal. Builds trust strongly.",
                            "Invites them to keep holding you accountable."
                    ),
                    List.of(1, 2, 1),
                    List.of("companion_request", "request_fulfilled", recruitId),
                    banterResponseLines(ally, List.of("companion_request", "request_fulfilled", recruitId), 2)
            ));
        }
    }

    private String softRequestKey(String recruitId, String requestId) {
        return recruitId + ":" + requestId;
    }

    private SoftCompanionRequest softCompanionRequest(String recruitId, String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        for (SoftCompanionRequest request : softCompanionRequestsFor(recruitId)) {
            if (request.id().equals(requestId)) {
                return request;
            }
        }
        return null;
    }

    private List<SoftCompanionRequest> softCompanionRequestsFor(String recruitId) {
        return switch (recruitId == null ? "" : recruitId) {
            case "seraphine" -> List.of(
                    softRequest("free_choice", "When power asks for obedience, check whether anyone is still free to refuse.",
                            List.of("freedom", "truth", "evidence"), "Accepted Seraphine's request to protect free choice.",
                            "Kept Seraphine's request by choosing evidence and freedom over easy obedience."));
            case "maera" -> List.of(
                    softRequest("keep_evidence", "When truth becomes inconvenient, do not make it lonely.",
                            List.of("evidence", "truth", "main_story", "knowledge"), "Accepted Maera's request to protect inconvenient truth.",
                            "Kept Maera's request by preserving evidence when the road wanted speed."));
            case "cassia" -> List.of(
                    softRequest("protect_first", "If glory and protection point in different directions, choose protection.",
                            List.of("protection", "duty", "oathstead", "combat"), "Accepted Cassia's request to choose protection first.",
                            "Kept Cassia's request by choosing protection over spectacle."));
            case "lyra" -> List.of(
                    softRequest("care_supplies", "Before we chase another danger, remember medicine, food, and rest are not optional.",
                            List.of("healing", "gather", "craft", "protection"), "Accepted Lyra's request to keep care practical.",
                            "Kept Lyra's request by making room for care before the next danger."));
            case "samir" -> List.of(
                    softRequest("honest_light", "When faith or victory sounds too clean, ask what it had to wash away.",
                            List.of("truth", "mercy", "holy", "main_story"), "Accepted Samir's request to keep the light honest.",
                            "Kept Samir's request by choosing honest light over easy certainty."));
            case "aria" -> List.of(
                    softRequest("watch_trails", "When everyone rushes, look for the trail they hope we miss.",
                            List.of("scouting", "caution", "survival", "gather"), "Accepted Aria's request to watch the overlooked paths.",
                            "Kept Aria's request by noticing the useful trail before rushing on."));
            case "vesper" -> List.of(
                    softRequest("leave_roots", "Take what is needed, but leave enough living for tomorrow.",
                            List.of("nature", "gather", "oathstead", "patience"), "Accepted Vesper's request to leave room for living things.",
                            "Kept Vesper's request by taking care without stripping tomorrow bare."));
            case "rafiq" -> List.of(
                    softRequest("second_chance", "When someone reaches for a second chance, make them earn it without making them beg.",
                            List.of("mercy", "courage", "loyalty", "combat"), "Accepted Rafiq's request to leave room for earned second chances.",
                            "Kept Rafiq's request by treating second chances as work, not charity."));
            case "calder" -> List.of(
                    softRequest("maintain_promises", "When something holds today, ask what keeps it holding tomorrow.",
                            List.of("craft", "oathstead", "build", "practical", "protection"), "Accepted Calder's request to maintain what matters.",
                            "Kept Calder's request by strengthening something before it failed."));
            default -> List.of();
        };
    }

    private SoftCompanionRequest softRequest(String id, String prompt, List<String> tags, String acceptMemory, String fulfillMemory) {
        return new SoftCompanionRequest(id, prompt, tags, acceptMemory, fulfillMemory);
    }

    public void openActiveShop() {
        if (mode != GameMode.DIALOG) {
            return;
        }
        if (activeShop == null) {
            CityBuilding assignedShop = activeNpcAssignedShopBuilding();
            if (assignedShop != null) {
                activeShop = shopForVillageBuilding(assignedShop);
            }
        }
        if (activeShop != null) {
            mode = GameMode.SHOP;
            status = activeShop.name() + ". Press 1-" + activeShop.availableStock(player.level).size() + " to buy, Esc to leave.";
        }
    }

    public boolean activeNpcCanOpenAssignedShop() {
        return activeNpcAssignedShopBuilding() != null;
    }

    private void openActiveShopDialogueShop() {
        CityBuilding building = activeShopDialogueBuilding();
        if (building == null) {
            clearActiveQuestDialogueLine();
            status = "That shop assignment is no longer active.";
            return;
        }
        activeShop = shopForVillageBuilding(building);
        activeQuestDialogueLine = "";
        activeShopDialogueBuildingKey = "";
        mode = GameMode.SHOP;
        status = activeShop.name() + ". Press 1-" + activeShop.availableStock(player.level).size() + " to buy, Esc to leave.";
    }

    public boolean handleActiveNpcQuestAction() {
        Quest quest = questForNpc(activeNpc);
        if (quest == null) {
            return false;
        }
        if (quest.accepted && !quest.ready()) {
            recordStoryInventoryObjective(quest);
            if (autoAdvanceReadyQuestStage(quest)) {
                return true;
            }
            if (quest.activeObjectiveKind().conversationObjective() && quest.activeObjectiveKind() != Quest.ObjectiveKind.CHOICE) {
                String note = recordConversationQuestObjective(quest, activeNpc);
                if (!note.isBlank()) {
                    status = note;
                    return true;
                }
            }
        }
        if (!quest.accepted) {
            if (!canAcceptActiveStoryQuest(quest)) {
                return true;
            }
            quest.accepted = true;
            invalidateQuestObjectiveCache();
            invalidateNpcListCache();
            recordNpcKnowledge(activeNpc, quest.companionQuest() ? 2 : 1);
            if (quest.companionQuest()) {
                recordCompanionMemoryForNpc(activeNpc, "quest_start", companionQuestStartMemory(quest));
            }
            publishCompanionEvent(
                    "quest_start:" + quest.id,
                    "Started " + quest.title,
                    quest.companionQuest()
                            ? List.of("commitment", "companion_quest", "duty", "truth", "evidence")
                            : quest.mainStoryQuest()
                            ? List.of("main_story", "courage", "duty", "truth", "evidence")
                            : List.of("helpful", "practical"),
                    0,
                    "",
                    "",
                    quest.companionQuest() || quest.mainStoryQuest()
            );
            activeQuestDialogueLine = questSpeakerLine(quest.activeStartDialog());
            activeShopDialogueBuildingKey = "";
            activeDialogueSession = startDialogueSessionFor(activeNpc);
            status = "Quest accepted: " + quest.title + ". " + quest.activeStartDialog();
            if ("ms_ember_socket_rite".equals(quest.id)) {
                String note = unlockRecipe("ember_socket_stone", activeNpc.name() + " teaches");
                if (!note.isBlank()) {
                    status += " " + note;
                }
            }
            if (triggerAcceptedRaidDefenseQuest(quest)) {
                return true;
            }
            return true;
        } else if (quest.ready()) {
            if (!quest.finalStage()) {
                String completedStage = quest.activeStage().title();
                String stageCompleteLine = quest.activeCompleteDialog();
                quest.advanceStage();
                clearQuestStageRuntime(quest);
                activeQuestDialogueLine = questSpeakerLine(stageCompleteLine.isBlank()
                        ? quest.activeStartDialog()
                        : stageCompleteLine + " " + quest.activeStartDialog());
                activeShopDialogueBuildingKey = "";
                activeDialogueSession = startDialogueSessionFor(activeNpc);
                String stageLabel = completedStage == null || completedStage.isBlank() ? "stage" : completedStage;
                if (quest.companionQuest()) {
                    recordCompanionMemoryForNpc(activeNpc, "quest_stage", companionQuestStageMemory(quest, completedStage));
                }
                status = "Quest stage complete: " + stageLabel + ". " + quest.activeStartDialog();
                return true;
            }
            Npc reportRecipient = questGiver(quest.id);
            if (reportRecipient != null && !activeNpcMatchesQuestGiver(quest)) {
                activeQuestDialogueLine = "Take this report to " + reportRecipient.name() + " in " + world.label(reportRecipient.mapId()) + ".";
                status = activeQuestDialogueLine;
                return true;
            }
            quest.completed = true;
            quest.observedStages.add(quest.activeStage().id());
            invalidateQuestObjectiveCache();
            invalidateNpcListCache();
            player.gold += quest.rewardGold;
            int questXp = (int) Math.round(quest.rewardXp * 0.80 * (1.0 + player.skillRank("hard_won_lessons") * 0.08));
            List<String> notes = player.gainXp(questXp);
            adjustNpcRelationship(activeNpc, quest.mainStoryQuest() ? 30 : quest.companionQuest() ? 18 : 10);
            recordNpcKnowledge(activeNpc, quest.companionQuest() ? 2 : 1);
            if (quest.companionQuest()) {
                recordCompanionMemoryForNpc(activeNpc, "quest_complete", companionQuestCompletionMemory(quest));
            } else if (quest.mainStoryQuest()) {
                recordMainStoryMemory("Saw the player complete " + quest.title + ".");
            }
            publishCompanionEvent(
                    "quest_complete:" + quest.id,
                    "Completed " + quest.title,
                    quest.companionQuest()
                            ? List.of("loyalty", "companion_quest", "duty", "truth", "evidence")
                            : quest.mainStoryQuest()
                            ? List.of("main_story", "courage", "protection", "truth", "evidence")
                            : List.of("helpful", "practical"),
                    quest.mainStoryQuest() || quest.companionQuest() ? 1 : 0,
                    "quest_witness",
                    "Saw the player complete " + quest.title + ".",
                    true
            );
            activeQuestDialogueLine = questSpeakerLine(quest.activeCompleteDialog());
            activeShopDialogueBuildingKey = "";
            activeDialogueSession = startDialogueSessionFor(activeNpc);
            status = "Quest complete: " + quest.title + ". " + quest.activeCompleteDialog();
            String stoneReward = GameData.mainStoryStoneReward(quest.id);
            if (!stoneReward.isBlank() && !player.hasItem(stoneReward)) {
                player.addItem(stoneReward, 1);
                status += " Gained " + GameData.itemName(stoneReward) + " (" + magicStoneCount() + "/"
                        + GameData.MAGIC_STONE_KEYS.size() + ").";
            }
            if (quest.companionQuest()) {
                Quest next = quest.nextQuestId == null ? null : quests.get(quest.nextQuestId);
                if (next != null && !next.completed) {
                    status += " " + activeNpc.name() + " has more to share.";
                }
                if (activeNpc.recruitId() != null && !isRecruited(activeNpc.recruitId())) {
                    status += companionRecruitmentEligible(activeNpc.recruitId(), activeNpc)
                            ? " You can ask " + activeNpc.name() + " to travel with you."
                            : " Relationship " + npcRelationship(activeNpc) + "/" + COMPANION_RECRUIT_RELATIONSHIP
                            + " or one completed companion chapter needed before " + activeNpc.name() + " can join.";
                }
            } else if (quest.mainStoryQuest()) {
                Quest next = quest.nextQuestId == null ? null : quests.get(quest.nextQuestId);
                if (next != null && !next.completed) {
                    status += " " + activeNpc.name() + " trusts you more; keep talking before asking for the next secret.";
                } else {
                    status += " The void portal waits in the world with " + magicStoneCount() + "/"
                            + GameData.MAGIC_STONE_KEYS.size() + " stones lit.";
                }
            } else {
                Quest next = quest.nextQuestId == null ? null : quests.get(quest.nextQuestId);
                if (next != null && !next.completed) {
                    status += " " + questFollowUpLine(next);
                }
                String recruitNote = recruitAlly(activeNpc.recruitId());
                if (recruitNote != null) {
                    status += " " + recruitNote;
                }
            }
            if (!notes.isEmpty()) {
                status += " " + notes.get(notes.size() - 1);
            }
            refreshPlayerVillageGrowth();
            questMonsterRuntime.remove(quest.id);
            invalidateQuestObjectiveCache();
            if (activeQuestMonster != null && quest.id.equals(activeQuestMonster.questId)) {
                activeQuestMonster = null;
            }
            return true;
        } else if (!quest.completed) {
            status = quest.title + ": " + quest.progress + "/" + quest.activeNeeded() + ". " + quest.activeProgressDialog();
            return true;
        } else {
            status = quest.title + " is already complete.";
            return false;
        }
    }

    private String questFollowUpLine(Quest next) {
        Npc giver = questGiver(next.id);
        if (giver == null) {
            return "A new lead opens: " + next.title + ".";
        }
        return "A new lead opens: speak with " + giver.name() + " in " + world.label(giver.mapId()) + ".";
    }

    private boolean autoAdvanceReadyQuestStage(Quest quest) {
        if (quest == null || quest.completed || !quest.ready() || quest.finalStage()) {
            return false;
        }
        String completedStage = quest.activeStage().title();
        String stageReadyLine = quest.activeReadyDialog();
        quest.advanceStage();
        clearQuestStageRuntime(quest);
        if (quest.companionQuest()) {
            Npc giver = questGiver(quest.id);
            if (giver != null) {
                recordCompanionMemoryForNpc(giver, "quest_stage", companionQuestStageMemory(quest, completedStage));
            }
        }
        if (activeNpc != null) {
            activeDialogueSession = startDialogueSessionFor(activeNpc);
            activeShopDialogueBuildingKey = "";
            activeQuestDialogueLine = activeNpcMatchesQuestGiver(quest)
                    ? questSpeakerLine(quest.activeStartDialog())
                    : "";
        }
        String stageLabel = completedStage == null || completedStage.isBlank() ? "stage" : completedStage;
        StringBuilder note = new StringBuilder("Quest stage complete: " + stageLabel + ".");
        if (stageReadyLine != null && !stageReadyLine.isBlank()) {
            note.append(" ").append(stageReadyLine);
        }
        if (!quest.activeStartDialog().isBlank()) {
            note.append(" ").append(quest.activeStartDialog());
        }
        status = note.toString();
        return true;
    }

    private boolean activeNpcMatchesQuestGiver(Quest quest) {
        if (quest == null || activeNpc == null) {
            return false;
        }
        Npc giver = questGiver(quest.id);
        return giver != null && npcRelationshipKey(giver).equals(npcRelationshipKey(activeNpc));
    }

    private void clearQuestStageRuntime(Quest quest) {
        if (quest == null) {
            return;
        }
        questMonsterRuntime.remove(quest.id);
        questNpcRuntime.remove(quest.id);
        invalidateQuestObjectiveCache();
        invalidateNpcListCache();
        activeQuestMonster = activeQuestMonster != null && quest.id.equals(activeQuestMonster.questId)
                ? null
                : activeQuestMonster;
    }

    private String recordConversationQuestObjectives(Npc npc) {
        if (npc == null) {
            return "";
        }
        String note = "";
        for (Quest quest : quests.values()) {
            if (!quest.accepted || quest.completed || quest.ready() || !quest.activeObjectiveKind().conversationObjective()) {
                continue;
            }
            String update = recordConversationQuestObjective(quest, npc);
            if (!update.isBlank()) {
                note = update;
            }
        }
        return note.isBlank() ? "" : " " + note;
    }

    private String recordConversationQuestObjective(Quest quest, Npc npc) {
        if (quest == null || npc == null || !quest.accepted || quest.completed || quest.ready()
                || quest.activeObjectiveKind() == Quest.ObjectiveKind.CHOICE || !conversationObjectiveMatches(quest, npc)) {
            return "";
        }
        String key = conversationObjectiveKey(quest, npc);
        if (!CompanionQuestContent.canHandOver(quest)) {
            return "The handover needs the requested quest supplies and preparation. " + quest.activeProgressDialog();
        }
        if (harvestedQuestResources.contains(key)) {
            return "";
        }
        int oldProgress = quest.progress;
        quest.recordConversation();
        if (quest.progress <= oldProgress) {
            return "";
        }
        harvestedQuestResources.add(key);
        invalidateQuestObjectiveCache();
        String note = quest.objectiveAction() + " objective updated: " + quest.title + " "
                + quest.progress + "/" + quest.activeNeeded() + ".";
        if (quest.ready()) {
            if (autoAdvanceReadyQuestStage(quest)) {
                return status;
            }
            note += " " + quest.activeReadyDialog();
        }
        return note;
    }

    private boolean conversationObjectiveMatches(Quest quest, Npc npc) {
        if (npc == null) return false;
        if (questForQuestNpc(npc) == quest) return true;
        if (quest.activeObjectiveKind() == Quest.ObjectiveKind.ASK_AROUND) {
            return conversationObjectiveMarkerNpcs(quest).stream()
                    .anyMatch(witness -> npcRelationshipKey(witness).equals(npcRelationshipKey(npc)));
        }
        if (targetNpcMatches(quest.activeTargetNpcId(), npc)) {
            return true;
        }
        if (quest.activeTargetNpcId() != null && !quest.activeTargetNpcId().isBlank()) return false;
        String npcName = normalizeQuestMatchText(npc.name());
        String npcRecruitId = normalizeQuestMatchText(npc.recruitId());
        String npcKey = normalizeQuestMatchText(npcRelationshipKey(npc));
        for (String candidate : List.of(quest.activeMonsterKey(), quest.activeObjectiveAsset(), quest.activeTarget())) {
            String expected = normalizeQuestMatchText(candidate);
            if (expected.isBlank()) {
                continue;
            }
            if (expected.equals(npcName) || expected.equals(npcRecruitId) || expected.equals(npcKey)) {
                return true;
            }
        }
        Npc giver = questGiver(quest.id);
        if (questTemporaryNpcObjective(quest) && quest.activeObjectiveKind() != Quest.ObjectiveKind.CHOICE) return false;
        return giver != null && npcRelationshipKey(giver).equals(npcRelationshipKey(npc));
    }

    private boolean targetNpcMatches(String targetNpcId, Npc npc) {
        String expected = normalizeQuestMatchText(targetNpcId);
        if (expected.isBlank() || npc == null) {
            return false;
        }
        return expected.equals(normalizeQuestMatchText(npc.name()))
                || expected.equals(normalizeQuestMatchText(npc.recruitId()))
                || expected.equals(normalizeQuestMatchText(npcRelationshipKey(npc)))
                || expected.equals(normalizeQuestMatchText(npc.mapId() + ":" + npc.name()));
    }

    private String conversationObjectiveKey(Quest quest, Npc npc) {
        String npcKey = npcRelationshipKey(npc);
        return "quest-conversation:" + questStageResourceId(quest) + ":" + npcKey;
    }

    private String normalizeQuestMatchText(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "")
                .strip();
    }

    private String questSpeakerLine(String line) {
        if (activeNpc == null) {
            return line == null ? "" : line.strip();
        }
        return stripSpeakerPrefix(line, activeNpc.name());
    }

    private boolean canAcceptActiveStoryQuest(Quest quest) {
        if (quest == null || !quest.mainStoryQuest() || activeNpc == null) {
            return true;
        }
        if (!mainStoryPrerequisiteComplete(quest)) {
            status = activeNpc.name() + " is not ready to reveal this part of the Twelve Socket Stones yet.";
            return false;
        }
        if ("ms_kingdoms_answer".equals(quest.id) && !hasAllMagicStonesExcept("stone_dawn")) {
            status = "Maelis needs the other eleven socket stones before the kingdoms can answer together.";
            return false;
        }
        if ("ms_twelve_stones_gate".equals(quest.id) && !hasAllMagicStones()) {
            status = "The Old Gate oath needs all twelve socket stones before Maelis will send you there.";
            return false;
        }
        return true;
    }

    private void recordStoryInventoryObjective(Quest quest) {
        String requiredItem = GameData.mainStoryRequiredItem(quest.id);
        if (requiredItem.isBlank() || !player.hasItem(requiredItem)) {
            return;
        }
        quest.progress = Math.max(quest.progress, quest.activeNeeded());
        invalidateQuestObjectiveCache();
    }

    public Quest questForNpc(Npc npc) {
        if (npc == null) {
            return null;
        }
        Quest questNpcQuest = questForQuestNpc(npc);
        if (questNpcQuest != null) {
            return questNpcQuest;
        }
        for (Quest active : quests.values()) {
            if (active.accepted && !active.completed && !active.ready()
                    && active.activeObjectiveKind().conversationObjective()
                    && active.activeObjectiveKind() != Quest.ObjectiveKind.CHOICE
                    && conversationObjectiveMatches(active, npc)) return active;
        }
        Quest authored = authoredQuestForNpc(npc);
        if (authored != null && !authored.completed) {
            if (!questPrerequisiteMet(authored)) {
                return null;
            }
            if (authored.mainStoryQuest() && !mainStoryAvailable(authored)) {
                return null;
            }
            return authored;
        }
        ensureWeeklyNpcQuests();
        String weeklyId = weeklyNpcQuestIds.get(npcRelationshipKey(npc));
        Quest weekly = weeklyId == null ? null : quests.get(weeklyId);
        return weekly != null && !weekly.completed ? weekly : null;
    }

    private Quest questForQuestNpc(Npc npc) {
        if (npc == null) {
            return null;
        }
        for (List<QuestNpcRuntime> runtimes : questNpcRuntime.values()) {
            for (QuestNpcRuntime runtime : runtimes) {
                if (!runtime.npc.equals(npc)) {
                    continue;
                }
                Quest quest = quests.get(runtime.questId);
                return quest != null && quest.accepted && !quest.completed ? quest : null;
            }
        }
        return null;
    }

    private Quest authoredQuestForNpc(Npc npc) {
        if (npc.questId() == null) {
            return null;
        }
        Quest quest = quests.get(npc.questId());
        while (quest != null && quest.completed && quest.nextQuestId != null && !quest.nextQuestId.isBlank()) {
            Quest next = quests.get(quest.nextQuestId);
            if (next == null || next.completed) {
                quest = next;
                continue;
            }
            return next;
        }
        return quest;
    }

    private boolean questPrerequisiteMet(Quest quest) {
        String predecessorId = questPredecessorId(quest == null ? "" : quest.id);
        if (predecessorId.isBlank()) {
            return true;
        }
        Quest predecessor = quests.get(predecessorId);
        return predecessor != null && predecessor.completed;
    }

    private String questPredecessorId(String questId) {
        if (questId == null || questId.isBlank()) {
            return "";
        }
        for (Quest candidate : quests.values()) {
            if (candidate.nextQuestId != null && questId.equals(candidate.nextQuestId)) {
                return candidate.id;
            }
        }
        return "";
    }

    private boolean mainStoryAvailable(Quest quest) {
        if (quest == null || !quest.mainStoryQuest() || quest.completed) {
            return true;
        }
        if (!mainStoryPrerequisiteComplete(quest)) {
            return false;
        }
        if ("ms_kingdoms_answer".equals(quest.id)) {
            return hasAllMagicStonesExcept("stone_dawn");
        }
        if ("ms_twelve_stones_gate".equals(quest.id)) {
            return hasAllMagicStones();
        }
        return true;
    }

    private boolean mainStoryPrerequisiteComplete(Quest quest) {
        String prerequisiteId = GameData.mainStoryPrerequisite(quest.id);
        if (prerequisiteId.isBlank()) {
            return true;
        }
        Quest prerequisite = quests.get(prerequisiteId);
        return prerequisite != null && prerequisite.completed;
    }

    public void ensureWeeklyNpcQuests() {
        int block = Math.floorDiv(worldTick, SIDE_QUEST_REFRESH_TICKS);
        if (block == weeklyQuestBlock) {
            return;
        }
        weeklyQuestBlock = block;
        weeklyNpcQuestIds.clear();
        Map<String, List<Npc>> candidatesByMap = new LinkedHashMap<>();
        for (Npc npc : weeklyQuestCandidates()) {
            if (npc.recruitId() != null) {
                continue;
            }
            Quest authored = authoredQuestForNpc(npc);
            if (authored != null && (authored.mainStoryQuest() || authored.companionQuest() || !authored.completed)) {
                continue;
            }
            candidatesByMap.computeIfAbsent(npc.mapId(), key -> new ArrayList<>()).add(npc);
        }
        for (List<Npc> candidates : candidatesByMap.values()) {
            // Keep local work available, but only half of eligible NPCs post each cycle.
            int count = (candidates.size() + 1) / 2;
            int offset = Math.floorMod(block, candidates.size());
            for (int i = 0; i < count; i++) {
                Npc npc = candidates.get((offset + i) % candidates.size());
                String id = weeklyQuestId(npc, block);
                quests.put(id, createWeeklyNpcQuest(npc, id, block));
                weeklyNpcQuestIds.put(npcRelationshipKey(npc), id);
            }
        }
        if (block > 0) {
            status = "Fresh side quests are posted across the settlements.";
        }
    }

    private List<Npc> weeklyQuestCandidates() {
        List<Npc> candidates = new ArrayList<>(GameData.NPCS);
        Set<String> seen = new HashSet<>();
        for (Npc npc : candidates) {
            seen.add(npcRelationshipKey(npc));
        }
        for (WorldMap.SettlementSite settlement : world.settlementSites()) {
            for (Npc npc : world.npcs(settlement.id())) {
                String key = npcRelationshipKey(npc);
                if (seen.add(key)) {
                    candidates.add(npc);
                }
            }
        }
        return candidates;
    }

    private String weeklyQuestId(Npc npc, int block) {
        return "weekly_" + block + "_" + npc.mapId().replace('-', '_') + "_"
                + npc.name().strip().toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private Quest createWeeklyNpcQuest(Npc npc, String id, int block) {
        return NpcQuestStories.refine(RegionalNpcQuests.create(npc, id, block, world), npc, world);
    }

    public String questGiverName(String questId) {
        Quest shared = quests.get(questId);
        if (shared != null && PartyDialogue.isShared(shared)) return PartyDialogue.pair(shared).names();
        Npc npc = questGiver(questId);
        return npc == null ? "the quest giver" : npc.name();
    }

    public String questReturnLocation(String questId) {
        Quest shared = quests.get(questId);
        if (shared != null && PartyDialogue.isShared(shared)) return "the traveling party";
        Npc npc = questGiver(questId);
        return npc == null ? "Unknown" : world.label(npc.mapId());
    }

    private Npc questGiver(String questId) {
        for (Npc npc : GameData.NPCS) {
            Quest authored = authoredQuestForNpc(npc);
            if (authored != null && questId.equals(authored.id)) {
                return npc;
            }
            if (questId.equals(weeklyNpcQuestIds.get(npcRelationshipKey(npc)))) {
                return npc;
            }
        }
        return null;
    }

    public void closeOverlay() {
        activeChest = null;
        if (mode == GameMode.DEFENSE) {
            closeDefenseRaid();
            return;
        }
        if (isMapEditorMap()) {
            exitMapEditor();
            return;
        }
        finishActiveNpcDialogue();
        activeNpc = null;
        activePartyTalkActor = null;
        activeShop = null;
        activeDialogueSession = null;
        activeDialogueVideo = null;
        activeRoamingEventPrompt = null;
        activeNpcIntroLine = "";
        activeQuestDialogueLine = "";
        activeShopDialogueBuildingKey = "";
        activeVillageBuilding = null;
        dialogIndex = 0;
        pendingVillageMoveSource = null;
        pendingVillageMoveMapId = null;
        mode = GameMode.EXPLORE;
    }

    public void startDefenseRaid() {
        if (mode != GameMode.EXPLORE && mode != GameMode.VILLAGE) {
            return;
        }
        if (!isSettlementMap(currentMapId)) {
            status = "Raid defenses must be staged inside a city, village, or Oathstead Camp.";
            return;
        }
        resetVillageInterface();
        Quest quest = activeDefenseRaidQuest();
        activeDefenseQuestId = quest == null ? "" : quest.id;
        defenseRaid = new CampDefenseMinigame(this);
        mode = GameMode.DEFENSE;
        status = quest == null
                ? "Raid warning: choose up to three learned abilities, then begin the defense."
                : quest.title + ": raid defense begins. Choose up to three learned abilities, then begin the defense.";
    }

    public void tickDefenseRaid() {
        if (mode != GameMode.DEFENSE || defenseRaid == null) {
            return;
        }
        defenseRaid.tick(this);
        if (defenseRaid.finished() && !defenseRaid.resolved()) {
            boolean victory = defenseRaid.victory();
            String reward = defenseRaid.resolveRewards(this);
            String questUpdate = victory ? recordDefenseRaidQuestProgress(activeDefenseQuestId) : "";
            status = defenseRaid.resultText() + (reward.isBlank() ? "" : " " + reward) + questUpdate;
        }
    }

    public void toggleDefenseRaidAbility(int index) {
        if (mode != GameMode.DEFENSE || defenseRaid == null) {
            return;
        }
        if (defenseRaid.toggleAbility(index)) {
            status = "Raid loadout updated.";
        }
    }

    public void chooseDefenseRaidLevelAbility(int index) {
        if (mode != GameMode.DEFENSE || defenseRaid == null) {
            return;
        }
        if (defenseRaid.chooseLevelAbility(index)) {
            status = "Raid level " + defenseRaid.raidLevel() + ": new technique added to the rotation.";
        }
    }

    public void beginDefenseRaid() {
        if (mode != GameMode.DEFENSE || defenseRaid == null) {
            return;
        }
        defenseRaid.begin(this);
    }

    public void moveDefensePlayer(double dx, double dy) {
        if (mode == GameMode.DEFENSE && defenseRaid != null) {
            defenseRaid.movePlayer(this, dx, dy);
        }
    }

    public void closeDefenseRaid() {
        if (defenseRaid != null && !defenseRaid.finished()) {
            status = "Raid drill abandoned.";
        }
        defenseRaid = null;
        activeDefenseQuestId = "";
        mode = GameMode.EXPLORE;
    }

    private Quest activeDefenseRaidQuest() {
        for (Quest quest : quests.values()) {
            if (quest.accepted
                    && !quest.completed
                    && !quest.ready()
                    && quest.activeObjectiveKind().defenseMinigameObjective()
                    && raidDefenseMapId(quest).equals(currentMapId)) {
                return quest;
            }
        }
        return null;
    }

    private String recordDefenseRaidQuestProgress(String questId) {
        if (questId == null || questId.isBlank()) {
            return "";
        }
        Quest quest = quests.get(questId);
        if (quest == null
                || quest.completed
                || quest.ready()
                || !quest.activeObjectiveKind().defenseMinigameObjective()) {
            return "";
        }
        int oldProgress = quest.progress;
        quest.progress = Math.min(quest.activeNeeded(), quest.progress + 1);
        if (quest.progress <= oldProgress) {
            return "";
        }
        invalidateQuestObjectiveCache();
        String update = " " + quest.title + ": " + quest.progress + "/" + quest.activeNeeded() + ".";
        if (quest.ready()) {
            if (autoAdvanceReadyQuestStage(quest)) {
                return " " + status;
            }
            update += " " + quest.activeReadyDialog();
        }
        return update;
    }

    private boolean triggerAcceptedRaidDefenseQuest(Quest quest) {
        if (quest == null
                || !quest.accepted
                || quest.completed
                || quest.ready()
                || !quest.activeObjectiveKind().defenseMinigameObjective()) {
            return false;
        }
        String mapId = raidDefenseMapId(quest);
        if (!world.hasMap(mapId) || !isSettlementMap(mapId)) {
            status = "Quest accepted: " + quest.title + ". The raid location is not available yet.";
            return false;
        }
        TilePoint marker = raidDefenseObjectivePoint(quest, mapId);
        TilePoint start = nearbyRaidDefenseStartPoint(mapId, marker);
        currentMapId = mapId;
        playerX = start.x();
        playerY = start.y();
        recordSettlementVisit(currentMapId);
        activeNpc = null;
        activePartyTalkActor = null;
        activeShop = null;
        activeDialogueSession = null;
        activeQuestDialogueLine = "";
        activeShopDialogueBuildingKey = "";
        dialogIndex = 0;
        mode = GameMode.EXPLORE;
        startDefenseRaid();
        return mode == GameMode.DEFENSE;
    }

    private void finishActiveNpcDialogue() {
        if (mode != GameMode.DIALOG || activeNpc == null || activeDialogueSession == null) {
            return;
        }
        String repeatKey = activeDialogueSession.lastRepeatKey();
        if (repeatKey == null || repeatKey.isBlank()) {
            return;
        }
        String recruitId = activeCompanionRecruitId();
        if (recruitId.isBlank()) {
            return;
        }
        String line = companionDialogueAfterLine(recruitId, repeatKey);
        if (line.isBlank()) {
            return;
        }
        Actor ally = companionActorForContext(activeNpc, recruitId);
        if (ally != null && activeAllies().contains(ally)) {
            enqueueCompanionComment(new PendingCompanionComment(
                    ally.name,
                    ally.name + ": " + line,
                    banterReplyOptions(ally, List.of("memory", "dialogue_after", recruitId), 1),
                    banterReplyTooltips(List.of("memory", "dialogue_after", recruitId), 1),
                    banterReplyDeltas(List.of("memory", "dialogue_after", recruitId), 1),
                    List.of("memory", "dialogue_after", recruitId),
                    banterResponseLines(ally, List.of("memory", "dialogue_after", recruitId), 1)
            ));
        } else {
            status = activeNpc.name() + ": " + line;
        }
    }

    private String companionDialogueAfterLine(String recruitId, String repeatKey) {
        return switch (recruitId == null ? "" : recruitId) {
            case "seraphine" -> switch (repeatKey) {
                case "romance", "future" -> "No hidden clause in that conversation. Terrifying, but clean.";
                case "trust", "values" -> "You keep asking where choice begins. I notice that more than I say.";
                case "memory", "outcome.detail" -> "Some choices only stay honest when someone keeps naming them.";
                default -> "The wording matters. It usually does.";
            };
            case "maera" -> switch (repeatKey) {
                case "memory", "outcome.detail" -> "That belongs in the record, but not where strangers can flatten it.";
                case "quest.clarify", "quest.personal" -> "The motive and the evidence are not separate. Remember that.";
                case "future" -> "I reserve the right to annotate that future heavily.";
                default -> "If the answer bothered you, good. Useful answers often do.";
            };
            case "cassia" -> switch (repeatKey) {
                case "quest.warning", "next" -> "Good questions. They keep people alive before courage gets dramatic.";
                case "trust", "feeling" -> "I am still standing. That is not the whole answer, but it is a true one.";
                case "home", "future" -> "Worth defending does not always mean easy to hold.";
                default -> "We named it. That makes it easier to carry cleanly.";
            };
            case "lyra" -> switch (repeatKey) {
                case "checkin", "feeling", "need" -> "Next time I say I am fine too quickly, you may look unconvinced.";
                case "memory" -> "Some memories need air before they heal.";
                case "future" -> "A future is care that survived long enough to rest.";
                default -> "Do not let the road make tenderness feel inefficient.";
            };
            case "samir" -> switch (repeatKey) {
                case "trust", "values" -> "Questions like that keep trust awake.";
                case "romance", "future" -> "Some light is warmer because no one ordered it to shine.";
                case "quest.clarify" -> "The shadow under the answer matters as much as the flame.";
                default -> "Carry the question gently. It may still be teaching us.";
            };
            case "aria" -> switch (repeatKey) {
                case "checkin", "feeling" -> "Do not get smug about asking the right question. Just... keep asking sometimes.";
                case "home", "romance", "future" -> "I still know the exits. That is why staying means anything.";
                case "quest.warning", "quest.practical" -> "Watch the second trail. The obvious one is rarely lonely.";
                default -> "If I sounded honest, blame the road. It was distracting me.";
            };
            case "vesper" -> switch (repeatKey) {
                case "memory" -> "Spoken roots keep growing after the conversation ends.";
                case "home", "future" -> "Home should grow around people, not over them.";
                case "need", "request" -> "Needs are safer once named, if no one grabs them too hard.";
                default -> "Let that answer sit in the soil before we pull at it again.";
            };
            case "rafiq" -> switch (repeatKey) {
                case "romance", "future" -> "I was almost sincere without injury. A historic achievement.";
                case "opinion" -> "My opinions are expensive. You are getting the troubling discount.";
                case "trust" -> "Trust remains a terrible plan. Naturally, I am considering it.";
                default -> "If that sounded honest, please remember I had many better jokes available.";
            };
            case "calder" -> switch (repeatKey) {
                case "need", "trust", "values" -> "Named weight is easier to brace.";
                case "home", "future" -> "A place worth keeping needs maintenance after the warm words.";
                case "quest.practical", "quest.warning" -> "Check the supports twice. Then move.";
                default -> "Good talk. Now we see whether it holds under weather.";
            };
            default -> "";
        };
    }

    public void gatherNearby() {
        if (mode != GameMode.EXPLORE) {
            return;
        }
        CraftingSystem.GatherCandidate candidate = crafting.findGatherTarget(world, currentMapId, playerX, playerY);
        status = crafting.beginGather(player, activeAllies(), candidate, random);
        trackPendingResourceNode(candidate);
    }

    private void trackPendingResourceNode(CraftingSystem.GatherCandidate candidate) {
        pendingResourceNodeMapId = "";
        pendingResourceNodeTile = null;
        pendingResourceNodeAsset = "";
        activeGatherMapId = "";
        activeGatherCandidate = null;
        if (crafting.active() && candidate != null) {
            activeGatherMapId = currentMapId;
            activeGatherCandidate = candidate;
            publishCompanionEvent("gather:" + candidate.label() + ":" + worldTick,
                    "Gathered " + candidate.label(),
                    gatherTags(candidate),
                    0,
                    "",
                    "",
                    false);
        }
        if (crafting.active() && shouldDepleteGatheredProp(candidate)) {
            pendingResourceNodeMapId = currentMapId;
            pendingResourceNodeTile = candidate.tile();
            pendingResourceNodeAsset = candidate.asset();
        }
    }

    private boolean shouldDepleteGatheredProp(CraftingSystem.GatherCandidate candidate) {
        if (candidate == null || candidate.terrain() != 'P' || candidate.asset().isBlank()) {
            return false;
        }
        String asset = candidate.asset().toLowerCase();
        return !asset.startsWith("interior_") && !"interior".equals(world.kind(currentMapId));
    }

    private List<String> gatherTags(CraftingSystem.GatherCandidate candidate) {
        if (candidate == null) {
            return List.of("gather", "survival", "practical");
        }
        String label = candidate.label().toLowerCase();
        List<String> tags = new ArrayList<>(List.of("gather", "survival", "practical"));
        if (label.contains("herb") || label.contains("flower") || label.contains("root") || label.contains("wood")
                || label.contains("mushroom") || label.contains("vegetable")) {
            tags.add("nature");
        }
        if (label.contains("ore") || label.contains("stone") || label.contains("coal")) {
            tags.add("craft");
        }
        if (label.contains("fish") || label.contains("meat") || label.contains("coconut")) {
            tags.add("protection");
        }
        return tags;
    }

    public void gatherNearby(int facingDx, int facingDy) {
        if (mode != GameMode.EXPLORE) {
            return;
        }
        CraftingSystem.GatherCandidate candidate = gatherTarget(facingDx, facingDy);
        if (candidate == null) {
            status = "Face a resource node, mountain, water, beach prop, or planter to gather.";
            return;
        }
        status = crafting.beginGather(player, activeAllies(), candidate, random);
        trackPendingResourceNode(candidate);
    }

    public void gatherAtTile(int x, int y) {
        if (mode != GameMode.EXPLORE) {
            return;
        }
        CraftingSystem.GatherCandidate candidate = crafting.findGatherTargetAt(world, currentMapId, x, y);
        if (candidate == null) {
            status = "Nothing useful to gather there.";
            return;
        }
        int distance = Math.abs(candidate.tile().x() - playerX) + Math.abs(candidate.tile().y() - playerY);
        if (distance > 1) {
            status = "Move closer to gather " + candidate.label() + ".";
            return;
        }
        status = crafting.beginGather(player, activeAllies(), candidate, random);
        trackPendingResourceNode(candidate);
    }

    public CraftingSystem.GatherCandidate gatherTargetAtTile(int x, int y) {
        if (mode != GameMode.EXPLORE) {
            return null;
        }
        return crafting.findGatherTargetAt(world, currentMapId, x, y);
    }

    public CraftingSystem.GatherCandidate gatherTarget(int facingDx, int facingDy) {
        if (mode != GameMode.EXPLORE) {
            return null;
        }
        return crafting.findGatherTarget(world, currentMapId, playerX, playerY, facingDx, facingDy);
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

    public WorldProp craftingStationAtTile(int x, int y) {
        for (WorldProp prop : world.props(currentMapId)) {
            CraftingSystem.Workstation workstation = CraftingSystem.workstationForAsset(prop.asset());
            if (workstation == null) {
                continue;
            }
            int[] footprint = CraftingSystem.workstationFootprint(prop.asset());
            if (x >= prop.x() && y >= prop.y() && x < prop.x() + footprint[0] && y < prop.y() + footprint[1]) {
                return prop;
            }
        }
        return null;
    }

    public CraftingSystem.Workstation workstationAtTile(int x, int y) {
        WorldProp prop = craftingStationAtTile(x, y);
        return prop == null ? null : CraftingSystem.workstationForAsset(prop.asset());
    }

    public void openCraftingAtTile(int x, int y) {
        if (mode != GameMode.EXPLORE) {
            return;
        }
        WorldProp station = craftingStationAtTile(x, y);
        CraftingSystem.Workstation workstation = station == null ? null : CraftingSystem.workstationForAsset(station.asset());
        if (workstation == null) {
            status = "There is no crafting station there.";
            return;
        }
        int[] footprint = CraftingSystem.workstationFootprint(station.asset());
        boolean nearby = false;
        for (int yy = station.y(); yy < station.y() + footprint[1] && !nearby; yy++) {
            for (int xx = station.x(); xx < station.x() + footprint[0]; xx++) {
                if (Math.abs(xx - playerX) + Math.abs(yy - playerY) <= 1) {
                    nearby = true;
                    break;
                }
            }
        }
        if (!nearby) {
            status = "Move closer to craft at " + workstation.label() + ".";
            return;
        }
        mode = GameMode.CRAFTING;
        status = "Crafting at " + workstation.label() + ".";
    }

    public List<CraftingSystem.Recipe> availableCraftingRecipes() {
        if (config.creativeCraftingMode) return CraftingSystem.sortRecipes(CraftingSystem.RECIPES);
        ensureStarterRecipeUnlocks();
        discoverRecipesFromInventory(false);
        return CraftingSystem.sortRecipes(CraftingSystem.recipesFor(currentWorkstation()).stream()
                .filter(recipe -> unlockedRecipeKeys.contains(recipe.key()))
                .toList());
    }

    public List<CraftingSystem.Recipe> learnedCraftingRecipes() {
        if (config.creativeCraftingMode) return CraftingSystem.sortRecipes(CraftingSystem.RECIPES);
        ensureStarterRecipeUnlocks();
        discoverRecipesFromInventory(false);
        return CraftingSystem.sortRecipes(CraftingSystem.RECIPES.stream()
                .filter(recipe -> unlockedRecipeKeys.contains(recipe.key()))
                .toList());
    }

    public void craftComponent(AssemblyCrafting.Slot slot, String materialKey) {
        startAssemblyTask(AssemblyCrafting.componentRecipe(player, slot, materialKey));
    }

    public void assembleEquipment(AssemblyCrafting.Blueprint blueprint, Map<AssemblyCrafting.Slot, String> parts) {
        startAssemblyTask(AssemblyCrafting.assemblyRecipe(player, blueprint, parts));
    }

    private void startAssemblyTask(CraftingSystem.Recipe recipe) {
        if (mode != GameMode.CRAFTING || crafting.active()) return;
        if (recipe == null) { status = "Prepare and select all required components first."; return; }
        if (config.creativeCraftingMode) { status = crafting.creativeCraft(player, recipe); return; }
        if (recipe.workstation() != currentWorkstation()) {
            status = "Move to " + recipe.workstation().label() + " for " + recipe.name() + ".";
            return;
        }
        status = crafting.beginCraft(player, recipe);
        if (crafting.active()) mode = GameMode.EXPLORE;
    }

    public void craftRecipe(String recipeKey) {
        if (mode != GameMode.CRAFTING && mode != GameMode.EXPLORE) {
            return;
        }
        ensureStarterRecipeUnlocks();
        CraftingSystem.Recipe recipe = CraftingSystem.recipeByKey(recipeKey);
        if (config.creativeCraftingMode) { status = crafting.creativeCraft(player, recipe); return; }
        if (recipe != null && !unlockedRecipeKeys.contains(recipe.key())) {
            status = "You have not learned " + recipe.name() + " yet.";
            return;
        }
        CraftingSystem.Workstation workstation = currentWorkstation();
        if (recipe == null || (recipe.workstation() != null && recipe.workstation() != workstation)) {
            status = "That recipe needs the right workstation.";
            return;
        }
        if (!stageWarehouseIngredientsForCraft(recipe)) {
            return;
        }
        status = crafting.beginCraft(player, recipe);
        if (crafting.active()) {
            publishCompanionEvent("craft:" + recipe.key() + ":" + worldTick,
                    "Started crafting " + recipe.name(),
                    craftTags(recipe),
                    0,
                    "",
                    "",
                    false);
            mode = GameMode.EXPLORE;
        }
    }

    private boolean stageWarehouseIngredientsForCraft(CraftingSystem.Recipe recipe) {
        for (Map.Entry<String, Integer> entry : recipe.cost().entrySet()) {
            int available = player.inventory.getOrDefault(entry.getKey(), 0) + villageStorage.getOrDefault(entry.getKey(), 0);
            if (available < entry.getValue()) {
                status = "Need " + CraftingSystem.costLabel(recipe.cost()) + " for " + recipe.name() + ".";
                return false;
            }
        }
        for (Map.Entry<String, Integer> entry : recipe.cost().entrySet()) {
            int missing = Math.max(0, entry.getValue() - player.inventory.getOrDefault(entry.getKey(), 0));
            if (missing <= 0) {
                continue;
            }
            int stored = villageStorage.getOrDefault(entry.getKey(), 0);
            int fromWarehouse = Math.min(stored, missing);
            if (fromWarehouse > 0) {
                updateVillageStorage(entry.getKey(), stored - fromWarehouse);
                player.addItem(entry.getKey(), fromWarehouse);
            }
        }
        return true;
    }

    private List<String> craftTags(CraftingSystem.Recipe recipe) {
        if (recipe == null) {
            return List.of("craft", "practical");
        }
        List<String> tags = new ArrayList<>(List.of("craft", "practical"));
        switch (recipe.category()) {
            case CONSUMABLE -> {
                tags.add("healing");
                tags.add("protection");
            }
            case ARMOR, TOOL -> {
                tags.add("protection");
                tags.add("duty");
            }
            case WEAPON -> tags.add("combat");
            case SEED -> tags.add("nature");
            case DECOR -> {
                tags.add("oathstead");
                tags.add("build");
            }
            default -> {
            }
        }
        return tags;
    }

    public void craftRecipeAt(int index) {
        List<CraftingSystem.Recipe> recipes = availableCraftingRecipes();
        if (index >= 0 && index < recipes.size()) {
            craftRecipe(recipes.get(index).key());
        }
    }

    public void ensureStarterRecipeUnlocks() {
        unlockedRecipeKeys.addAll(CraftingSystem.starterRecipeKeys());
    }

    public String unlockRecipe(String recipeKey, String source) {
        ensureStarterRecipeUnlocks();
        CraftingSystem.Recipe recipe = CraftingSystem.recipeByKey(recipeKey);
        if (recipe == null || unlockedRecipeKeys.contains(recipe.key())) {
            return "";
        }
        unlockedRecipeKeys.add(recipe.key());
        String prefix = source == null || source.isBlank() ? "Learned" : source;
        return prefix + " " + recipe.name() + ".";
    }

    private String discoverRecipesFromInventory(boolean describe) {
        ensureStarterRecipeUnlocks();
        List<String> learned = new ArrayList<>();
        for (String itemKey : new ArrayList<>(player.inventory.keySet())) {
            if (player.inventory.getOrDefault(itemKey, 0) <= 0) {
                continue;
            }
            for (CraftingSystem.Recipe recipe : CraftingSystem.recipesUsingIngredient(itemKey)) {
                if (unlockedRecipeKeys.add(recipe.key())) {
                    learned.add(recipe.name());
                }
            }
        }
        if (!describe || learned.isEmpty()) {
            return "";
        }
        int shown = Math.min(3, learned.size());
        String names = String.join(", ", learned.subList(0, shown));
        String extra = learned.size() > shown ? ", and " + (learned.size() - shown) + " more" : "";
        return learned.size() + " ingredient recipe" + (learned.size() == 1 ? "" : "s")
                + " unlocked: " + names + extra + ".";
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
        invalidateNpcListCache();
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
        invalidateNpcListCache();
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
        return WorldMap.PLAYER_VILLAGE_ID.equals(currentMapId)
                || currentMapId.startsWith("house_" + WorldMap.PLAYER_VILLAGE_ID + "_")
                || isMapEditorMap();
    }

    public boolean isManagedVillageMap() {
        return WorldMap.PLAYER_VILLAGE_ID.equals(currentMapId);
    }

    public boolean isManagedVillageInterior() {
        return (currentMapId.startsWith("house_" + WorldMap.PLAYER_VILLAGE_ID + "_") || isMapEditorMap())
                && "interior".equals(world.kind(currentMapId));
    }

    public boolean isMapEditorMap() {
        return world.isEditorMap(currentMapId);
    }

    public String selectedMapEditorKind() {
        return switch (Math.floorMod(mapEditorKindIndex, 3)) {
            case 0 -> "village";
            case 1 -> "city";
            default -> "interior";
        };
    }

    public String selectedMapEditorKindLabel() {
        return switch (selectedMapEditorKind()) {
            case "city" -> "City";
            case "interior" -> "Interior";
            default -> "Village";
        };
    }

    public int[] selectedMapEditorSize() {
        return switch (Math.floorMod(mapEditorSizeIndex, 4)) {
            case 0 -> new int[]{18, 14};
            case 1 -> new int[]{26, 18};
            case 2 -> new int[]{36, 24};
            default -> new int[]{48, 32};
        };
    }

    public String selectedMapEditorSizeLabel() {
        int[] size = selectedMapEditorSize();
        return size[0] + "x" + size[1];
    }

    public void previousMapEditorKind() {
        mapEditorKindIndex = Math.floorMod(mapEditorKindIndex - 1, 3);
        status = "Map editor type: " + selectedMapEditorKindLabel() + ".";
    }

    public void nextMapEditorKind() {
        mapEditorKindIndex = Math.floorMod(mapEditorKindIndex + 1, 3);
        status = "Map editor type: " + selectedMapEditorKindLabel() + ".";
    }

    public void previousMapEditorSize() {
        mapEditorSizeIndex = Math.floorMod(mapEditorSizeIndex - 1, 4);
        status = "Map editor size: " + selectedMapEditorSizeLabel() + ".";
    }

    public void nextMapEditorSize() {
        mapEditorSizeIndex = Math.floorMod(mapEditorSizeIndex + 1, 4);
        status = "Map editor size: " + selectedMapEditorSizeLabel() + ".";
    }

    public void enterMapEditor() {
        if (mode != GameMode.VILLAGE && mode != GameMode.EXPLORE) {
            return;
        }
        if (isMapEditorMap()) {
            status = "Already editing " + world.label(currentMapId) + ".";
            mode = GameMode.VILLAGE;
            return;
        }
        mapEditorReturnMapId = currentMapId;
        mapEditorReturnX = playerX;
        mapEditorReturnY = playerY;
        String kind = selectedMapEditorKind();
        int[] size = selectedMapEditorSize();
        String mapId = "editor_" + kind + "_" + player.name.toLowerCase().replaceAll("[^a-z0-9]+", "_") + "_" + (++mapEditorSerial);
        world.createEditorMap(mapId, "Custom " + selectedMapEditorKindLabel(), kind, size[0], size[1]);
        currentMapId = mapId;
        playerX = Math.max(1, size[0] / 2);
        playerY = Math.max(1, "interior".equals(kind) ? size[1] - 3 : size[1] / 2);
        resetVillageInterface();
        villageTab = "interior".equals(kind) ? 3 : 0;
        mode = GameMode.VILLAGE;
        status = "Map editor opened: " + selectedMapEditorKindLabel() + " " + size[0] + "x" + size[1] + ".";
    }

    public void exitMapEditor() {
        if (!isMapEditorMap()) {
            closeOverlay();
            return;
        }
        String editedLabel = world.label(currentMapId);
        currentMapId = mapEditorReturnMapId == null || mapEditorReturnMapId.isBlank() ? WorldMap.PLAYER_VILLAGE_ID : mapEditorReturnMapId;
        playerX = mapEditorReturnX;
        playerY = mapEditorReturnY;
        resetVillageInterface();
        mode = GameMode.EXPLORE;
        status = "Closed " + editedLabel + ". Returned to " + world.label(currentMapId) + ".";
    }

    public String exportMapEditorDesign() {
        String export = world.exportEditorMap(currentMapId);
        if (export.isBlank()) {
            status = "Open the map editor before exporting.";
            return "";
        }
        return export;
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
        VillageManager.PlaceableAsset option = VillageManager.interiorAsset(asset);
        selectedInteriorAsset = option.asset();
        if (!"All".equals(selectedInteriorAssetCategory)) {
            selectedInteriorAssetCategory = option.category();
        }
        setVillageEditAction("place");
    }

    public void selectInteriorAssetCategory(String category) {
        List<VillageManager.PlaceableAsset> options = VillageManager.interiorAssets(category);
        if (options.isEmpty()) {
            return;
        }
        selectedInteriorAssetCategory = VillageManager.interiorAssetCategories().contains(category) ? category : "All";
        selectedInteriorAsset = options.get(0).asset();
        setVillageEditAction("place");
    }

    public void handleVillageWorldClick(int x, int y) {
        if (mode != GameMode.VILLAGE) {
            return;
        }
        if (isMapEditorMap()) {
            handleMapEditorClick(x, y);
            return;
        }
        if (isManagedVillageInterior()) {
            if (villageTab == 1) {
                handleInteriorTileClick(x, y);
            } else {
                handleInteriorPlacementClick(x, y);
            }
            return;
        }
        if (villageTab == 3) {
            handleInteriorPlacementClick(x, y);
            return;
        }
        if (!isManagedVillageMap()) {
            status = "Village building changes must be made at Oathstead Camp.";
            return;
        }
        if ("delete".equals(villageEditAction)) {
            WorldProp prop = world.playerVillagePropAt(x, y);
            if (prop != null && world.removePlayerVillageProp(prop)) {
                status = "Village asset removed.";
                return;
            }
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

    private void handleVillageBuildingDecorationClick(int x, int y) {
        CityBuilding building = world.cityBuildingAt(WorldMap.PLAYER_VILLAGE_ID, x, y);
        if (building == null || !building.key().startsWith("player_")) {
            status = "Select one of your placed buildings to decorate.";
            return;
        }
        List<String> decor = villageBuildingDecorations.computeIfAbsent(building.key(), ignored -> new ArrayList<>());
        if ("delete".equals(villageEditAction)) {
            if (decor.isEmpty()) {
                status = VillageManager.buildingLabel(building.style()) + " has no interior decor to remove.";
                return;
            }
            String removed = decor.remove(decor.size() - 1);
            status = "Removed " + interiorDecorLabel(removed) + ". " + buildingInteriorSummary(building);
            return;
        }
        if ("move".equals(villageEditAction) || "upgrade".equals(villageEditAction)) {
            status = "Interior decor can be placed or deleted from the Inside tab.";
            return;
        }
        VillageManager.PlaceableAsset asset = VillageManager.interiorAsset(selectedInteriorAsset);
        decor.add(asset.asset());
        status = "Added " + asset.label() + " to " + VillageManager.buildingLabel(building.style()) + ". "
                + buildingInteriorSummary(building);
    }

    private void handleMapEditorClick(int x, int y) {
        String kind = world.kind(currentMapId);
        if ("interior".equals(kind)) {
            if (villageTab == 1) {
                handleMapEditorTileClick(x, y);
            } else if (villageTab == 3 || villageTab == 2) {
                handleMapEditorInteriorAssetClick(x, y);
            } else {
                status = "Interior maps use Tiles and Inside tools.";
            }
            return;
        }
        if (villageTab == 0) {
            handleMapEditorBuildingClick(x, y);
        } else if (villageTab == 1) {
            handleMapEditorTileClick(x, y);
        } else if (villageTab == 2) {
            handleMapEditorAssetClick(x, y);
        } else {
            status = "Use the Interior editor type for inside layouts.";
        }
    }

    private void handleMapEditorBuildingClick(int x, int y) {
        CityBuilding building = world.cityBuildingAt(currentMapId, x, y);
        if ("delete".equals(villageEditAction)) {
            status = world.removeEditorBuilding(currentMapId, building) ? "Editor building removed." : "No editor building there.";
            return;
        }
        if ("move".equals(villageEditAction)) {
            if (pendingVillageMoveSource == null) {
                if (building == null) {
                    status = "Select an editor building first.";
                    return;
                }
                pendingVillageMoveSource = building.anchor();
                pendingVillageMoveMapId = currentMapId;
                status = "Choose the new building location.";
                return;
            }
            CityBuilding source = currentMapId.equals(pendingVillageMoveMapId)
                    ? world.cityBuildingAt(currentMapId, pendingVillageMoveSource.x(), pendingVillageMoveSource.y())
                    : null;
            status = world.moveEditorBuilding(currentMapId, source, x, y) ? "Editor building moved." : "That building will not fit there.";
            pendingVillageMoveSource = null;
            pendingVillageMoveMapId = null;
            return;
        }
        if ("upgrade".equals(villageEditAction)) {
            status = "Editor buildings use their base form.";
            return;
        }
        VillageManager.BuildingPlan plan = VillageManager.buildingPlan(selectedVillageBuildingStyle);
        CityBuilding placed = world.placeEditorBuilding(currentMapId, plan.style(), x, y);
        status = placed == null ? "That building will not fit there." : "Placed " + plan.label() + ".";
    }

    private void handleMapEditorTileClick(int x, int y) {
        char tile = selectedVillageTile;
        if ("delete".equals(villageEditAction)) {
            tile = switch (world.kind(currentMapId)) {
                case "city" -> 'p';
                case "interior" -> 'g';
                default -> 'g';
            };
        }
        VillageManager.TilePlan plan = VillageManager.tilePlan(tile);
        status = world.setEditorTile(currentMapId, x, y, tile)
                ? ("interior".equals(world.kind(currentMapId)) ? "Interior tile set." : plan.label() + " set.")
                : "That tile cannot be changed.";
    }

    private void handleMapEditorAssetClick(int x, int y) {
        WorldProp prop = world.editorPropAt(currentMapId, x, y);
        if ("delete".equals(villageEditAction)) {
            status = world.removeEditorProp(currentMapId, prop) ? "Editor prop removed." : "No editor prop there.";
            return;
        }
        if ("move".equals(villageEditAction)) {
            if (pendingVillageMoveSource == null) {
                if (prop == null) {
                    status = "Select an editor prop first.";
                    return;
                }
                pendingVillageMoveSource = new TilePoint(x, y);
                pendingVillageMoveMapId = currentMapId;
                status = "Choose the new prop location.";
                return;
            }
            WorldProp source = currentMapId.equals(pendingVillageMoveMapId)
                    ? world.editorPropAt(currentMapId, pendingVillageMoveSource.x(), pendingVillageMoveSource.y())
                    : null;
            status = world.moveEditorProp(currentMapId, source, x, y) ? "Editor prop moved." : "That prop will not fit there.";
            pendingVillageMoveSource = null;
            pendingVillageMoveMapId = null;
            return;
        }
        VillageManager.PlaceableAsset asset = VillageManager.outdoorAsset(selectedVillageAsset);
        status = world.addEditorProp(currentMapId, x, y, asset.asset(), asset.size())
                ? asset.label() + " placed."
                : "That prop cannot be placed there.";
    }

    private void handleMapEditorInteriorAssetClick(int x, int y) {
        WorldProp prop = world.editorInteriorPropAt(currentMapId, x, y);
        if ("delete".equals(villageEditAction)) {
            status = world.removeEditorInteriorProp(currentMapId, prop) ? "Interior asset removed." : "No interior asset there.";
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
                    ? world.editorInteriorPropAt(currentMapId, pendingVillageMoveSource.x(), pendingVillageMoveSource.y())
                    : null;
            status = world.moveEditorInteriorProp(currentMapId, source, x, y) ? "Interior asset moved." : "That interior asset will not fit there.";
            pendingVillageMoveSource = null;
            pendingVillageMoveMapId = null;
            return;
        }
        VillageManager.PlaceableAsset asset = VillageManager.interiorAsset(selectedInteriorAsset);
        status = world.addEditorInteriorProp(currentMapId, x, y, asset.asset(), asset.size())
                ? asset.label() + " placed."
                : "That interior asset cannot be placed there.";
    }

    private void handleVillageBuildingClick(int x, int y) {
        CityBuilding building = world.cityBuildingAt(WorldMap.PLAYER_VILLAGE_ID, x, y);
        if ("delete".equals(villageEditAction)) {
            if (world.removePlayerVillageBuilding(building)) {
                villageBuildingAssignments.remove(building.key());
                villageBuildingDecorations.remove(building.key());
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
        if (placed != null) {
            publishCompanionEvent("oathstead_build:" + placed.key(), "Placed " + villageBuildingLabel(placed.style()),
                    List.of("oathstead", "build", "practical", "craft"), 1, "oathstead_build",
                    "Helped place " + villageBuildingLabel(placed.style()) + " at Oathstead.", true);
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
            publishCompanionEvent("oathstead_upgrade:" + building.key() + ":" + (level + 1),
                    "Upgraded " + plan.label(),
                    List.of("oathstead", "build", "practical", "craft"), 1, "oathstead_build",
                    "Helped upgrade " + plan.label() + " at Oathstead.", true);
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

    private void handleInteriorTileClick(int x, int y) {
        if (!isManagedVillageInterior()) {
            status = "Enter a village building to edit its interior tiles.";
            return;
        }
        if ("move".equals(villageEditAction)) {
            status = "Interior tiles can be placed or deleted directly.";
            return;
        }
        char tile = "delete".equals(villageEditAction) ? 'i' : selectedVillageTile;
        char resolved = tile == 'o' ? 'o' : 'i';
        VillageManager.TilePlan plan = VillageManager.tilePlan(resolved);
        if (!"delete".equals(villageEditAction) && !canAffordVillageCost(plan.cost())) {
            status = "You need " + VillageManager.costLabel(plan.cost()) + " for " + plan.label() + ".";
            return;
        }
        boolean placed = world.setPlayerInteriorTile(currentMapId, x, y, resolved);
        if (placed && !"delete".equals(villageEditAction)) {
            spendVillageCost(plan.cost());
        }
        status = placed ? plan.label() + " set at " + x + ", " + y + "." : "That interior tile cannot be changed.";
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
        if (mode == GameMode.SKILLS || mode == GameMode.PARTY) {
            closeOverlay();
        } else if (mode == GameMode.EXPLORE) {
            partyScreenIndex = Math.max(0, Math.min(partyScreenIndex, partyMembers().size() - 1));
            mode = GameMode.PARTY;
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
        this.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
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
        if (CraftingSystem.isRecipeBookItem(itemKey)) {
            String recipeKey = CraftingSystem.recipeKeyForBookItem(itemKey);
            String note = unlockRecipe(recipeKey, GameData.itemName(itemKey) + " teaches");
            status = note.isBlank()
                    ? "You already know the recipe in " + GameData.itemName(itemKey) + "."
                    : "Bought " + GameData.itemName(itemKey) + ". " + note;
            return;
        }
        player.addItem(itemKey, 1);
        status = "Bought " + GameData.itemName(itemKey) + ".";
    }

    public void sellShopItem(int index) {
        if (mode != GameMode.SHOP || activeShop == null || index < 0 || index >= player.inventory.size()) {
            return;
        }
        String itemKey = player.inventory.keySet().stream().toList().get(index);
        int value = Math.max(1, GameData.itemCost(itemKey) / 2);
        if (!player.consumeItem(itemKey)) {
            status = "No " + GameData.itemName(itemKey) + " to sell.";
            return;
        }
        player.gold += value;
        status = "Sold " + GameData.itemName(itemKey) + " for " + value + "g.";
    }

    public boolean isRecruited(String recruitId) {
        return recruitId != null && recruitedIds.contains(recruitId);
    }

    public List<Actor> activeAllies() {
        List<Actor> active = new ArrayList<>();
        for (String recruitId : recruitedIds) {
            GameData.RecruitSpec spec = GameData.RECRUITS.get(recruitId);
            if (spec == null) {
                continue;
            }
            Actor ally = allyByName(spec.name());
            if (ally != null && villageAllies.contains(ally.name)) {
                continue;
            }
            if (ally != null && !active.contains(ally)) {
                active.add(ally);
            }
            if (active.size() >= MAX_ACTIVE_COMPANIONS) {
                break;
            }
        }
        return active;
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
        invalidateNpcListCache();
        recordCompanionMemory(recruitId, "recruited", "Joined the player's companions.");
        publishCompanionEvent("recruited:" + recruitId, spec.name() + " joined the party",
                List.of("loyalty", "commitment", "companionship"), 1, "recruited",
                "Joined the player's companions.", true);
        triggerActionBanter(ally, companionRecruitmentBanter(recruitId, ally.name));
        return activeAllies().contains(ally)
                ? ally.name + " joins your party."
                : ally.name + " joins your companions at Oathstead. Active party is full ("
                + MAX_ACTIVE_COMPANIONS + "/" + MAX_ACTIVE_COMPANIONS + ").";
    }

    private String companionRecruitmentBanter(String recruitId, String name) {
        return switch (recruitId == null ? "" : recruitId) {
            case "seraphine" -> name + ": By choice, no clauses. Try not to make me regret the clean wording.";
            case "maera" -> name + ": I am coming. If the road lies, I reserve the right to annotate it violently.";
            case "cassia" -> name + ": I stand with you. Not by order. Remember that when the road gets loud.";
            case "lyra" -> name + ": I am with you. Someone has to keep mercy stocked and heroes hydrated.";
            case "samir" -> name + ": I walk beside you. Bring questions. I dislike lonely light.";
            case "aria" -> name + ": Fine. I am scouting with you. Step where I step unless I am showing off.";
            case "vesper" -> name + ": I am coming. If we pass something still alive, we stop long enough to notice.";
            case "rafiq" -> name + ": I join you under protest, excitement, and excellent lighting.";
            case "calder" -> name + ": I am with you. First rule: if the road cracks, we inspect before bragging.";
            default -> name + ": I am with you now.";
        };
    }

    public boolean canRecruitActiveNpcToVillage() {
        if (activeNpc == null) {
            return false;
        }
        if (allyByName(activeNpc.name()) != null && villageAllies.contains(activeNpc.name())) {
            return false;
        }
        return npcRelationship(activeNpc) >= villageRecruitThreshold(activeNpc);
    }

    public boolean canRecruitActiveNpcToParty() {
        return activeNpc != null
                && activeNpc.recruitId() != null
                && !isRecruited(activeNpc.recruitId())
                && companionRecruitmentEligible(activeNpc.recruitId(), activeNpc);
    }

    public void recruitActiveNpcToParty() {
        if ((mode != GameMode.DIALOG && mode != GameMode.SHOP) || activeNpc == null || activeNpc.recruitId() == null) {
            return;
        }
        if (!companionRecruitmentEligible(activeNpc.recruitId(), activeNpc)) {
            status = activeNpc.name() + " needs relationship " + COMPANION_RECRUIT_RELATIONSHIP
                    + " or one completed companion chapter to join the party. Current: " + npcRelationship(activeNpc) + ".";
            return;
        }
        String recruitNote = recruitAlly(activeNpc.recruitId());
        status = recruitNote == null ? activeNpc.name() + " cannot join right now." : recruitNote;
    }

    private boolean companionRecruitmentEligible(String recruitId, Npc npc) {
        if (recruitId == null || recruitId.isBlank() || isRecruited(recruitId)) {
            return false;
        }
        return npcRelationship(npc) >= COMPANION_RECRUIT_RELATIONSHIP
                || companionAnyQuestCompleted(recruitId);
    }

    private boolean companionAnyQuestCompleted(String recruitId) {
        if (recruitId == null || recruitId.isBlank()) {
            return false;
        }
        for (Quest quest : quests.values()) {
            if (recruitId.equals(quest.chainOwnerId) && quest.completed) {
                return true;
            }
        }
        return false;
    }

    private boolean companionChainComplete(String recruitId) {
        boolean found = false;
        for (Quest quest : quests.values()) {
            if (recruitId.equals(quest.chainOwnerId)) {
                found = true;
                if (!quest.completed) {
                    return false;
                }
            }
        }
        return found;
    }

    public int villageRecruitThreshold(Npc npc) {
        return specialCompanionNpc(npc) ? COMPANION_RECRUIT_RELATIONSHIP : VILLAGE_RECRUIT_RELATIONSHIP;
    }

    public void recruitActiveNpcToVillage() {
        if ((mode != GameMode.DIALOG && mode != GameMode.SHOP) || activeNpc == null) {
            return;
        }
        int threshold = villageRecruitThreshold(activeNpc);
        int relationship = npcRelationship(activeNpc);
        if (relationship < threshold) {
            status = activeNpc.name() + " needs relationship " + threshold + " to join Oathstead. Current: " + relationship + ".";
            return;
        }
        Actor ally = allyByName(activeNpc.name());
        if (ally == null && activeNpc.recruitId() != null && !isRecruited(activeNpc.recruitId())) {
            recruitAlly(activeNpc.recruitId());
            ally = allyByName(activeNpc.name());
            GameData.RecruitSpec spec = GameData.RECRUITS.get(activeNpc.recruitId());
            if (ally == null && spec != null) {
                ally = allyByName(spec.name());
            }
        }
        if (ally == null) {
            ally = createVillageActorFromNpc(activeNpc);
            allies.add(ally);
        }
        villageAllies.add(ally.name);
        villageWorkerRoles.putIfAbsent(ally.name, "idle");
        invalidateNpcListCache();
        status = ally.name + " agrees to settle at Oathstead.";
        refreshPlayerVillageGrowth();
    }

    private Actor createVillageActorFromNpc(Npc npc) {
        String className = villageClassForNpc(npc);
        int level = Math.max(1, Math.min(player.level, 1 + npcRelationship(npc) / 35));
        Actor actor = new Actor(npc.name(), npc.sprite(), className,
                42 + level * 3,
                className.equals("Mage") || className.equals("Cleric") || className.equals("Medic") ? 24 + level * 2 : 14 + level,
                8 + level,
                2 + level / 2);
        actor.abilities.addAll(GameData.classAbilities(className));
        actor.level = level;
        actor.healFull();
        actor.professionXp.putAll(npc.professionXp());
        syncSkillAbilities(actor);
        return actor;
    }

    private String villageClassForNpc(Npc npc) {
        String text = (npc.name() + " " + npc.sprite() + " " + String.join(" ", npc.dialog())).toLowerCase();
        if (text.contains("medic") || text.contains("healer") || text.contains("salve") || text.contains("clinic")) {
            return "Medic";
        }
        if (text.contains("scribe") || text.contains("archive") || text.contains("rune") || text.contains("spell")) {
            return "Mage";
        }
        if (text.contains("scout") || text.contains("guide") || text.contains("track") || text.contains("road")) {
            return "Scout";
        }
        if (text.contains("guard") || text.contains("captain") || text.contains("shield") || text.contains("wall")) {
            return "Guard";
        }
        if (text.contains("merchant") || text.contains("peddler") || text.contains("seller") || text.contains("market")) {
            return "Trader";
        }
        if (text.contains("farmer") || text.contains("baker") || text.contains("cook")) {
            return "Forager";
        }
        return "Worker";
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
        invalidateNpcListCache();
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
        invalidateNpcListCache();
        status = ally.name + " ends city attendance at Oathstead.";
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

    public void openActiveVillageBuildingShop() {
        if (activeVillageBuilding == null) {
            status = "Select a building first.";
            return;
        }
        String assigned = assignedAllyForBuilding(activeVillageBuilding.key());
        if (assigned.isBlank()) {
            status = "Assign a city-attendance worker before opening a shop.";
            return;
        }
        activeNpc = null;
        activeShop = shopForVillageBuilding(activeVillageBuilding);
        mode = GameMode.SHOP;
        status = activeShop.name() + " opened.";
    }

    private Shop shopForVillageBuilding(CityBuilding building) {
        List<String> stock = new ArrayList<>(baseBuildingShopStock(building.style()));
        int quality = buildingToolScore(building);
        List<String> upgrades = upgradedBuildingShopStock(building.style());
        for (int i = 0; i < Math.min(quality, upgrades.size()); i++) {
            String item = upgrades.get(i);
            if (!stock.contains(item)) {
                stock.add(item);
            }
        }
        return new Shop("village_" + building.key(), VillageManager.buildingLabel(building.style()) + " Shop", stock);
    }

    private List<String> baseBuildingShopStock(String style) {
        return switch (style) {
            case "blacksmith" -> List.of("iron_sword", "steel_sword", "riveted_mail", "guard_cuirass", "iron_kite_shield");
            case "shop" -> List.of("stone_axe", "woodcutter_axe", "river_buckler", "oaken_roundshield", "recipe_book_oak_bow");
            case "apothecary" -> List.of("potion_small", "potion_large", "ether", "guard_tonic", "recipe_book_apothecary_salve");
            case "inn" -> List.of("trail_rations", "potion_small", "ether", "recipe_book_camp_cookery");
            case "bakery" -> List.of("trail_rations", "potion_small", "recipe_book_camp_cookery");
            case "warehouse", "granary" -> List.of("trail_rations", "escape_scroll", "guard_tonic");
            case "forestry_hut" -> List.of("stone_axe", "woodcutter_axe", "oaken_roundshield", "recipe_book_oak_bow");
            case "mine" -> List.of("stone_pickaxe", "iron_pickaxe", "riveted_mail", "recipe_book_iron_mail");
            case "hunting_camp" -> List.of("potion_small", "scout_hood", "ranger_jerkin", "recipe_book_fisher_knots");
            case "farmstead", "garden" -> List.of("trail_rations", "potion_small", "recipe_book_camp_cookery");
            case "fishing_hut" -> List.of("trail_rations", "shell_lure", "recipe_book_fisher_knots");
            case "guild" -> List.of("ether", "archive_lens", "recipe_book_escape_scrolls");
            case "shrine" -> List.of("potion_small", "ether", "sunward_medallion");
            case "watchtower" -> List.of("iron_sword", "river_buckler", "guard_tonic");
            case "house", "row" -> List.of("potion_small", "trail_rations", "traveler_cloak");
            default -> List.of("potion_small", "trail_rations");
        };
    }

    private List<String> upgradedBuildingShopStock(String style) {
        return switch (style) {
            case "blacksmith" -> List.of("steel_bastion_plate", "mountain_plate", "emberforged_plate", "starforged_plate");
            case "shop" -> List.of("towerguard_shield", "frostguard_aegis", "heartstone_bulwark");
            case "apothecary" -> List.of("battle_kit", "phoenix_feather", "heartstone_locket");
            case "inn", "bakery" -> List.of("battle_kit", "phoenix_feather");
            case "warehouse", "granary" -> List.of("battle_kit", "escape_scroll", "royal_wardplate");
            case "forestry_hut" -> List.of("heartwood_vest", "thornwall_shield", "heartstone_bulwark");
            case "mine" -> List.of("steel_bastion_plate", "mountain_plate", "frostbound_plate");
            case "hunting_camp" -> List.of("stormhide_jacket", "thornsilk_armor", "obsidian_fang");
            case "farmstead", "garden" -> List.of("guard_tonic", "phoenix_feather");
            case "fishing_hut" -> List.of("marshrunner_mantle", "marshlight_seal");
            case "guild" -> List.of("starweave_robes", "starrelic_ring", "voidglass_staff");
            case "shrine" -> List.of("suncloth_mantle", "sunwarden_plate", "phoenix_crown_pin");
            case "watchtower" -> List.of("towerguard_shield", "royal_heater", "stormguard_plate");
            case "house", "row" -> List.of("river_pearl_charm", "thornroot_charm");
            default -> List.of("battle_kit");
        };
    }

    public String buildingInteriorSummary(CityBuilding building) {
        if (building == null) {
            return "No building selected.";
        }
        return "Decor " + buildingDecorCount(building)
                + " | Appeal +" + buildingAttractiveness(building)
                + " | Shop +" + buildingToolScore(building)
                + " | Revenue +" + buildingRevenueBonus(building) + "g/day";
    }

    public int buildingDecorCount(CityBuilding building) {
        return buildingDecorAssets(building).size();
    }

    public int buildingAttractiveness(CityBuilding building) {
        int score = 0;
        for (String asset : buildingDecorAssets(building)) {
            if (asset.contains("flower") || asset.contains("plant") || asset.contains("rug")
                    || asset.contains("aquarium") || asset.contains("vase") || asset.contains("window")) {
                score += 2;
            } else {
                score += 1;
            }
        }
        return score;
    }

    public int buildingToolScore(CityBuilding building) {
        int score = 0;
        for (String asset : buildingDecorAssets(building)) {
            if (asset.contains("anvil") || asset.contains("forge") || asset.contains("workbench")
                    || asset.contains("carpenter") || asset.contains("alchemy") || asset.contains("cooking")
                    || asset.contains("counter") || asset.contains("bookshelf") || asset.contains("shelf")) {
                score++;
            }
        }
        return score;
    }

    public int buildingRevenueBonus(CityBuilding building) {
        int beds = 0;
        int counters = 0;
        for (String asset : buildingDecorAssets(building)) {
            if (asset.contains("bed")) {
                beds++;
            }
            if (asset.contains("counter") || asset.contains("table") || asset.contains("bar")) {
                counters++;
            }
        }
        int styleBase = "inn".equals(building.style()) ? beds * 5 : counters * 2;
        return styleBase + buildingAttractiveness(building) / 2;
    }

    private String interiorDecorLabel(String asset) {
        return asset == null ? "decor" : asset.replace("interior_", "").replace('_', ' ');
    }

    private List<String> buildingDecorAssets(CityBuilding building) {
        if (building == null) {
            return List.of();
        }
        List<String> assets = new ArrayList<>(villageBuildingDecorations.getOrDefault(building.key(), List.of()));
        String mapId = "house_" + WorldMap.PLAYER_VILLAGE_ID + "_" + building.anchor().x() + "_" + building.anchor().y();
        for (WorldProp prop : world.playerInteriorProps().getOrDefault(mapId, List.of())) {
            assets.add(prop.asset());
        }
        return assets;
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
        invalidateNpcListCache();
        status = ally.name + " assigned to " + VillageManager.buildingLabel(activeVillageBuilding.style()) + ".";
        resetNpcRuntime();
        recordCompanionMemoryForAlly(ally, "oathstead_work", "Assigned to " + VillageManager.buildingLabel(activeVillageBuilding.style()) + " at Oathstead.");
        publishCompanionEvent("oathstead_assignment:" + ally.name, ally.name + " was assigned to "
                        + VillageManager.buildingLabel(activeVillageBuilding.style()),
                List.of("oathstead", "practical", "duty", "craft"), 1, "oathstead_work",
                "Assigned work at Oathstead.", true);
        triggerActionBanter(ally, ally.name + ": The " + VillageManager.buildingLabel(activeVillageBuilding.style()).toLowerCase()
                + " gives me work with edges. Good. I trust useful edges.");
    }

    public void clearActiveBuildingAssignment() {
        if (activeVillageBuilding == null) {
            return;
        }
        String removed = villageBuildingAssignments.remove(activeVillageBuilding.key());
        status = removed == null || removed.isBlank()
                ? "No one was assigned there."
                : removed + " is no longer assigned to that building.";
        invalidateNpcListCache();
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
        invalidateNpcListCache();
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
        Equipment equipment = GameData.equipment(itemKey);
        if (equipment != null) {
            status = player.equipItem(itemKey);
            return;
        }
        Item item = GameData.ITEMS.get(itemKey);
        if (item == null || !player.hasItem(itemKey)) {
            status = "No usable " + GameData.itemName(itemKey) + ".";
            return;
        }
        if ("escape_scroll".equals(itemKey)) {
            useEscapeScroll();
            return;
        }
        if (player.level < item.minLevel()) {
            status = item.name() + " requires level " + item.minLevel() + ".";
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

    private void useEscapeScroll() {
        if (mode == GameMode.BATTLE) {
            status = "Escape scrolls cannot be read in battle.";
            return;
        }
        if (!"dungeon".equals(world.kind(currentMapId))) {
            status = "The scroll only answers from inside a dungeon.";
            return;
        }
        WorldTransition escape = world.dungeonEscapeTransition(currentMapId);
        if (escape == null) {
            status = "The scroll flickers, but finds no way out.";
            return;
        }
        player.consumeItem("escape_scroll");
        battle = null;
        activeDungeonMonster = null;
        mode = GameMode.EXPLORE;
        applyTransition(escape);
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
        if (GameData.isEquipment(itemKey)) {
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
        if ("escape_scroll".equals(itemKey)) {
            useEscapeScroll();
            return;
        }
        if (actor.level < item.minLevel()) {
            status = item.name() + " requires level " + item.minLevel() + ".";
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
        Equipment item = GameData.equipment(itemKey);
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
        Equipment item = GameData.equipment(itemKey);
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
        boolean professionSkill = SkillTrees.isProfessionSkill(skillKey);
        if (professionSkill ? actor.professionSkillPoints <= 0 : actor.skillPoints <= 0) {
            status = professionSkill ? "No profession skill points available." : "No skill points available.";
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
        if (actor.level < node.levelRequirement()) {
            status = node.name() + " requires level " + node.levelRequirement() + ".";
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
        if (SkillTrees.isProfessionSkill(skillKey)) {
            actor.professionSkillPoints--;
        } else {
            actor.skillPoints--;
        }
        actor.skillAllocations.put(skillKey, actor.skillRank(skillKey) + 1);
        applySkillEffects(actor, node, 1);
        syncSkillAbilities(actor);
        status = actor.name + " learned " + node.name() + ".";
    }

    public void savePartyLoadoutPreset(String presetName) {
        Actor actor = loadoutScreenActor();
        if (actor != null && actor.saveAbilityLoadoutPreset(presetName)) {
            status = actor.name + " saved " + presetName + " loadout.";
        }
    }

    public void applyPartyLoadoutPreset(String presetName) {
        Actor actor = loadoutScreenActor();
        if (actor != null && actor.applyAbilityLoadoutPreset(presetName)) {
            status = actor.name + " equipped " + presetName + " loadout.";
        } else if (actor != null) {
            status = actor.name + " has no " + presetName + " loadout saved.";
        }
    }

    public void placePartyAbilityInLoadout(String abilityName, int slot) {
        Actor actor = loadoutScreenActor();
        if (actor == null || abilityName == null || abilityName.isBlank()) {
            return;
        }
        if (actor.placeAbilityInLoadout(abilityName, slot)) {
            status = actor.name + " prepared " + abilityName + ".";
        } else {
            status = actor.name + " does not know " + abilityName + ".";
        }
    }

    public void removePartyAbilityFromLoadout(String abilityName) {
        Actor actor = loadoutScreenActor();
        if (actor != null && actor.removeAbilityFromLoadout(abilityName)) {
            status = actor.name + " set aside " + abilityName + ".";
        }
    }

    private Actor loadoutScreenActor() {
        return mode == GameMode.SKILLS ? player : partyScreenActor();
    }

    public void allocateStat(Actor actor, String statKey) {
        if (actor == null || actor.statPoints <= 0) {
            return;
        }
        if (actor.allocateStat(statKey)) {
            status = actor.name + " improved " + statLabel(statKey) + ".";
        }
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
        int professionSpent = SkillTrees.professionSpent(actor.skillAllocations);
        actor.professionSkillPoints += professionSpent;
        actor.skillPoints += spent - professionSpent;
        actor.skillAllocations.clear();
        List<String> previousLoadout = List.copyOf(actor.activeAbilityNames);
        syncSkillAbilities(actor);
        int removed = Math.max(0, previousLoadout.size() - actor.activeAbilityNames.size());
        actor.sanitizeAbilityLoadoutPresets();
        status = actor.name + "'s skills reset for " + cost + " gold."
                + (removed > 0 ? " Removed " + removed + " unlearned loadout slot" + (removed == 1 ? "." : "s.") : "");
    }

    public boolean move(int dx, int dy) {
        if (mode == GameMode.DEFENSE) {
            return moveDuringDefense(dx, dy);
        }
        if (mode != GameMode.EXPLORE) {
            return false;
        }
        if (crafting.active()) {
            status = "Busy: " + crafting.progressLabel() + ".";
            return false;
        }
        return moveExploreTo(playerX + dx, playerY + dy);
    }

    /** Old saves may contain a position in a building whose footprint has since been corrected. */
    public void recoverBlockedSettlementPosition() {
        if (mode != GameMode.EXPLORE || !("city".equals(world.kind(currentMapId)) || "village".equals(world.kind(currentMapId)))
                || !world.cityBuildingBlocksMovementAt(currentMapId, playerX, playerY)
                || PropCollision.navigationAnchor(world, currentMapId, playerX, playerY) != null) return;
        for (int radius = 1; radius <= 8; radius++) {
            for (int dy = radius; dy >= -radius; dy--) for (int dx = -radius; dx <= radius; dx++) {
                if (Math.abs(dx) + Math.abs(dy) != radius) continue;
                int x = playerX + dx, y = playerY + dy;
                if (world.transitionAt(currentMapId, x, y) != null) continue;
                if (PropCollision.navigationAnchor(world, currentMapId, x, y) != null) {
                    playerX = x; playerY = y; return;
                }
            }
        }
    }

    public boolean moveFreeExploreTo(double centerX, double centerY) {
        var from = PropCollision.navigationAnchor(world, currentMapId, playerX, playerY);
        if (from == null) return false;
        return moveFreeExploreTo(from.x(), from.y(), centerX, centerY);
    }

    public boolean moveFreeExploreTo(double fromX, double fromY, double centerX, double centerY) {
        if (mode != GameMode.EXPLORE) {
            return false;
        }
        if (crafting.active()) {
            status = "Busy: " + crafting.progressLabel() + ".";
            return false;
        }
        int nx = (int) Math.floor(centerX);
        int ny = (int) Math.floor(centerY);
        if (!PropCollision.canTravel(world, currentMapId, fromX, fromY, centerX, centerY)) {
            // Enter authored doors when the player's leading edge reaches them, before its center clips the wall.
            if (Math.hypot(centerX - fromX, centerY - fromY) <= 0.35) {
                int touchX = (int) Math.floor(centerX + Math.signum(centerX - fromX) * PropCollision.PLAYER_RADIUS);
                int touchY = (int) Math.floor(centerY + Math.signum(centerY - fromY) * PropCollision.PLAYER_RADIUS);
                if (!world.isPassable(currentMapId, touchX, touchY)
                        && (world.transitionAt(currentMapId, touchX, touchY) != null
                        || world.cityBuildingEntryAt(currentMapId, touchX, touchY, playerX, playerY) != null)) {
                    return moveExploreTo(touchX, touchY);
                }
            }
            status = "Path blocked.";
            return false;
        }
        if (nx == playerX && ny == playerY) {
            return true;
        }
        return moveExploreTo(nx, ny, true);
    }

    private boolean moveExploreTo(int nx, int ny) {
        return moveExploreTo(nx, ny, false);
    }

    private boolean moveExploreTo(int nx, int ny, boolean continuous) {
        WorldTransition currentTransition = world.transitionAt(currentMapId, playerX, playerY);
        WorldTransition targetTransition = world.transitionAt(currentMapId, nx, ny);
        if (!continuous && Math.abs(nx - playerX) == 1 && Math.abs(ny - playerY) == 1
                && (!world.isPassable(currentMapId, nx, playerY)
                || !world.isPassable(currentMapId, playerX, ny))
                && targetTransition == null) {
            status = "Path blocked.";
            return false;
        }
        if (!continuous && !world.isPassable(currentMapId, nx, ny)) {
            if (targetTransition != null) {
                applyTransition(targetTransition);
                return true;
            }
            if (currentTransition != null) {
                applyTransition(currentTransition);
                return true;
            }
            CityBuilding building = world.cityBuildingEntryAt(currentMapId, nx, ny, playerX, playerY);
            if (building != null && isSettlementMap(currentMapId)) {
                enterBuildingAt(nx, ny);
                return true;
            }
            status = "Blocked by " + Terrain.name(world.tileAt(currentMapId, nx, ny)) + ".";
            return false;
        }
        if (!continuous) {
            var from = PropCollision.navigationAnchor(world, currentMapId, playerX, playerY);
            var to = PropCollision.navigationAnchor(world, currentMapId, nx, ny);
            if (from == null || to == null || !PropCollision.canTravel(world, currentMapId,
                    from.x(), from.y(), to.x(), to.y())) {
                status = "Path blocked.";
                return false;
            }
        }
        DungeonMonsterRuntime dungeonMonster = dungeonMonsterAt(currentMapId, nx, ny);
        if (dungeonMonster != null) {
            playerX = nx;
            playerY = ny;
            startDungeonMonsterBattle(dungeonMonster);
            return true;
        }
        QuestMonsterRuntime questMonster = questMonsterAt(currentMapId, nx, ny);
        if (questMonster != null) {
            playerX = nx;
            playerY = ny;
            startQuestMonsterBattle(questMonster);
            return true;
        }
        Npc npc = npcAt(currentMapId, nx, ny);
        if (npc != null && !residentsYieldToPlayer(currentMapId)) {
            status = npcDisplayName(npc) + " is there. Press E to talk.";
            return false;
        }
        QuestObjective blockingObjective = blockingQuestObjectiveAt(currentMapId, nx, ny);
        if (blockingObjective != null) {
            status = blockingObjective.target() + " is there. Press E to engage.";
            return false;
        }
        if (npc != null) yieldResidentToPlayer(npc, nx - playerX, ny - playerY);
        playerX = nx;
        playerY = ny;
        if (targetTransition != null) {
            applyTransition(targetTransition);
            return true;
        }
        finishExploreStep();
        return true;
    }

    private void finishExploreStep() {
        tickWorldAbilityTimers();
        String discovery = landmarkDiscoveryLog.discover(this);
        status = discovery.isBlank() ? world.describe(currentMapId, playerX, playerY) : discovery;
        maybeStartEncounter();
    }

    private boolean moveDuringDefense(int dx, int dy) {
        if (defenseRaid == null || defenseRaid.finished() || !defenseRaid.started()) {
            return false;
        }
        defenseRaid.movePlayer(this, dx, dy);
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
                String bonus = applyGatherWorldBonus();
                if (completePendingBookshelfReading()) {
                    activeGatherMapId = "";
                    activeGatherCandidate = null;
                    return;
                }
                depletePendingResourceNode();
                activeGatherMapId = "";
                activeGatherCandidate = null;
                String discovered = discoverRecipesFromInventory(true);
                status = discovered.isBlank() ? finished : finished + " " + discovered;
                if (!bonus.isBlank()) {
                    status += " " + bonus;
                }
            }
            return;
        }
        if (mode != GameMode.EXPLORE) {
            return;
        }
        ensureWeeklyNpcQuests();
        updateNpcMovement();
        updateDungeonMonsterMovement();
        updateQuestMonsterMovement();
        updateTravelBanter();
        updateAmbientTownConversation();
    }

    private void tickVillageProduction() {
        if (villageAllies.isEmpty() || villageStorageUsed() >= villageStorageCapacity()) {
            return;
        }
        Map<String, Integer> produced = new LinkedHashMap<>();
        Map<String, Integer> levelSummary = world.playerVillageBuildingRoleLevels();
        int goldRevenue = 0;
        for (Actor ally : stationedAllies()) {
            String buildingKey = buildingAssignmentForAlly(ally.name);
            CityBuilding assignedBuilding = buildingKey.isBlank() ? null : playerVillageBuildingByKey(buildingKey);
            if (assignedBuilding == null) {
                continue;
            }
            goldRevenue += Math.max(0, buildingRevenueBonus(assignedBuilding)) / 2;
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
        if (goldRevenue > 0) {
            player.gold += goldRevenue;
            status = (produced.isEmpty() ? "" : status + " ") + "Village businesses earned " + goldRevenue + "g.";
        }
    }

    private void updateTravelBanter() {
        if (mode != GameMode.EXPLORE) return;
        if (partyDialogue.owns(activeTravelBanter)) {
            if (!partyDialogue.valid(this)) {
                partyDialogue.cancel();
                activeTravelBanter = null;
            } else if (worldTick >= activeTravelBanter.expiresAtTick()) {
                // Expiring an offer never accepts it on the player's behalf.
                partyDialogue.cancel();
                activeTravelBanter = null;
                nextTravelBanterTick = worldTick + 1300;
            } else return;
        }
        if (activeTravelBanter != null && worldTick >= activeTravelBanter.expiresAtTick()) {
            activeTravelBanter = null;
            nextTravelBanterTick = worldTick + 480 + random.nextInt(900);
        }
        if (activeTravelBanter == null) {
            activeTravelBanter = partyDialogue.startPending(this);
            if (activeTravelBanter != null) return;
        }
        if (activeTravelBanter == null && !pendingCompanionComments.isEmpty()) {
            showPendingCompanionComment();
            return;
        }
        if (activeTravelBanter != null || worldTick < nextTravelBanterTick || activeAllies().isEmpty()) {
            return;
        }
        if (!WorldMap.OVERWORLD_ID.equals(currentMapId) || mode != GameMode.EXPLORE) {
            nextTravelBanterTick = worldTick + 360;
            return;
        }
        if (random.nextDouble() > 0.018) {
            return;
        }
        activeTravelBanter = partyDialogue.ambient(this);
        if (activeTravelBanter != null) {
            nextTravelBanterTick = worldTick + 1300;
            return;
        }
        Actor speaker = activeAllies().get(random.nextInt(activeAllies().size()));
        BanterDraft draft = travelBanterDraft(speaker);
        if (draft.line().isBlank() || recentlyUsedBanter(speaker.name, draft.key())) {
            nextTravelBanterTick = worldTick + 240 + random.nextInt(420);
            return;
        }
        activeTravelBanter = new TravelBanterPrompt(
                speaker.name,
                draft.line(),
                banterReplyOptions(speaker, draft.tags(), 1),
                banterReplyTooltips(draft.tags(), 1),
                banterReplyDeltas(draft.tags(), 1),
                draft.tags(),
                banterResponseLines(speaker, draft.tags(), 1),
                worldTick + TRAVEL_BANTER_DURATION_TICKS
        );
        markBanterUsed(speaker.name, draft.key());
        nextTravelBanterTick = worldTick + 1200 + random.nextInt(1600);
    }

    private void updateAmbientTownConversation() {
        if (activeTownConversation != null) {
            if (!currentMapId.equals(activeTownConversation.mapId()) || worldTick >= activeTownConversation.expiresAtTick()) {
                activeTownConversation = null;
                nextTownConversationTick = worldTick + 260 + random.nextInt(420);
            } else {
                return;
            }
        }
        if (activeTravelBanter != null || worldTick < nextTownConversationTick) {
            return;
        }
        String kind = world.kind(currentMapId);
        if (!"city".equals(kind) && !"village".equals(kind)) {
            nextTownConversationTick = worldTick + 300;
            return;
        }
        List<Npc> npcs = npcsForMap(currentMapId);
        if (npcs.size() < 2) {
            nextTownConversationTick = worldTick + 360;
            return;
        }
        List<NpcPair> pairs = nearbyConversationPairs(npcs);
        if (pairs.isEmpty() || random.nextDouble() > 0.065) {
            nextTownConversationTick = worldTick + 150 + random.nextInt(220);
            return;
        }
        NpcPair pair = pairs.get(random.nextInt(pairs.size()));
        int draftSeed = worldTick / 90 + pair.speaker().name().hashCode() * 31 + pair.listener().name().hashCode();
        AmbientTownConversationLibrary.Draft draft = AmbientTownConversationLibrary.draftFor(
                pair.speaker(),
                pair.listener(),
                dayPhaseLabel(),
                world.label(currentMapId),
                draftSeed
        );
        if (draft.speakerLine().isBlank() || draft.listenerLine().isBlank() || recentlyUsedTownConversation(draft.key())) {
            nextTownConversationTick = worldTick + 180 + random.nextInt(260);
            return;
        }
        activeTownConversation = new AmbientNpcConversation(
                currentMapId,
                npcRelationshipKey(pair.speaker()),
                npcRelationshipKey(pair.listener()),
                draft.speakerLine(),
                draft.listenerLine(),
                worldTick + TOWN_CONVERSATION_DURATION_TICKS + random.nextInt(70)
        );
        markTownConversationUsed(draft.key());
        nextTownConversationTick = worldTick + 520 + random.nextInt(760);
    }

    private List<NpcPair> nearbyConversationPairs(List<Npc> npcs) {
        List<NpcPair> pairs = new ArrayList<>();
        for (int i = 0; i < npcs.size(); i++) {
            Npc speaker = npcs.get(i);
            if (speaker == null || speaker.equals(activeNpc)) {
                continue;
            }
            TilePoint speakerPosition = npcPosition(speaker);
            for (int j = i + 1; j < npcs.size(); j++) {
                Npc listener = npcs.get(j);
                if (listener == null || listener.equals(activeNpc)) {
                    continue;
                }
                TilePoint listenerPosition = npcPosition(listener);
                int distance = Math.abs(speakerPosition.x() - listenerPosition.x()) + Math.abs(speakerPosition.y() - listenerPosition.y());
                if (distance == 0 || distance > 3) {
                    continue;
                }
                if (speakerPosition.x() <= listenerPosition.x()) {
                    pairs.add(new NpcPair(speaker, listener));
                } else {
                    pairs.add(new NpcPair(listener, speaker));
                }
            }
        }
        return pairs;
    }


    private boolean recentlyUsedTownConversation(String key) {
        cleanupRecentTownConversationKeys();
        return key != null && !key.isBlank() && recentTownConversationKeys.containsKey(key);
    }

    private void markTownConversationUsed(String key) {
        cleanupRecentTownConversationKeys();
        if (key == null || key.isBlank()) {
            return;
        }
        recentTownConversationKeys.put(key, worldTick);
        while (recentTownConversationKeys.size() > 24) {
            String oldest = recentTownConversationKeys.keySet().iterator().next();
            recentTownConversationKeys.remove(oldest);
        }
    }

    private void cleanupRecentTownConversationKeys() {
        recentTownConversationKeys.entrySet().removeIf(entry -> worldTick - entry.getValue() > 1800);
    }

    private BanterDraft travelBanterDraft(Actor speaker) {
        String recruitId = recruitIdForAlly(speaker);
        String className = speaker.className;
        String terrain = Terrain.name(world.tileAt(currentMapId, playerX, playerY));
        BanterDraft companionLine = companionExplorationBark(speaker, recruitId, terrain);
        if (!companionLine.line().isBlank()) {
            return companionLine;
        }
        List<String> tags = List.of("exploration", "road", "terrain:" + terrain.toLowerCase().replaceAll("[^a-z0-9]+", "_"));
        if (className.contains("Medic") || className.equals("Cleric") || className.equals("Grovekeeper")) {
            return new BanterDraft(speaker.name + ": This road is collecting injuries before we have earned them. I can smell trouble under the " + terrain.toLowerCase() + ".",
                    tagged(tags, "healing", "caution"), "travel:healer:" + terrain);
        }
        if (className.contains("Mage") || className.equals("Wildspeaker") || className.equals("Thornbinder")) {
            return new BanterDraft(speaker.name + ": The air changed. Not enough for a spell, enough for a warning.",
                    tagged(tags, "magic", "caution"), "travel:mage:" + terrain);
        }
        if (className.contains("Ranger") || className.equals("Scout") || className.equals("Dune Guide")) {
            return new BanterDraft(speaker.name + ": Tracks cross here twice. Someone doubled back, or wanted us to think they did.",
                    tagged(tags, "scouting", "caution"), "travel:scout:" + terrain);
        }
        if (className.contains("Rogue") || className.equals("Veilrunner") || className.equals("Nightblade")) {
            return new BanterDraft(speaker.name + ": If an ambush waits ahead, it has terrible manners. I would have chosen better cover.",
                    tagged(tags, "clever", "danger"), "travel:rogue:" + terrain);
        }
        return new BanterDraft(speaker.name + ": Good pace. Bad road. That usually means we are exactly where someone needs us.",
                tags, "travel:generic:" + terrain);
    }

    private BanterDraft companionExplorationBark(Actor speaker, String recruitId, String terrain) {
        if (recruitId == null || recruitId.isBlank()) {
            return new BanterDraft("", List.of(), "");
        }
        String ground = terrain.toLowerCase();
        List<String> tags = List.of("exploration", "road", "terrain:" + ground.replaceAll("[^a-z0-9]+", "_"), recruitId);
        return switch (recruitId) {
            case "seraphine" -> new BanterDraft(speaker.name + ": Roads like this always have terms. The trick is finding who wrote them before we step into the fee.",
                    tagged(tags, "clever", "freedom"), "travel:seraphine:" + ground);
            case "maera" -> new BanterDraft(speaker.name + ": The " + ground + " keeps better records than most archives. Less flattering, usually more accurate.",
                    tagged(tags, "knowledge", "evidence"), "travel:maera:" + ground);
            case "cassia" -> new BanterDraft(speaker.name + ": Watch the edges. Trouble prefers people who stare only at the road ahead.",
                    tagged(tags, "duty", "protection"), "travel:cassia:" + ground);
            case "lyra" -> new BanterDraft(speaker.name + ": If anyone starts limping, say it before pride turns a small hurt into a long one.",
                    tagged(tags, "healing", "care"), "travel:lyra:" + ground);
            case "samir" -> new BanterDraft(speaker.name + ": The light sits strangely here. Not wrong. Asking questions, maybe.",
                    tagged(tags, "holy", "truth"), "travel:samir:" + ground);
            case "aria" -> new BanterDraft(speaker.name + ": Tracks cross the " + ground + " and then pretend not to. Amateur lie. Useful lie.",
                    tagged(tags, "scouting", "caution"), "travel:aria:" + ground);
            case "vesper" -> new BanterDraft(speaker.name + ": Something living passed here recently. Even fear leaves roots if it stays long enough.",
                    tagged(tags, "nature", "patience"), "travel:vesper:" + ground);
            case "rafiq" -> new BanterDraft(speaker.name + ": Excellent terrain for a dramatic ambush. Poor terrain for my boots. The road has mixed priorities.",
                    tagged(tags, "style", "danger"), "travel:rafiq:" + ground);
            case "calder" -> new BanterDraft(speaker.name + ": Bad footing. Keep your weight honest and the road might return the favor.",
                    tagged(tags, "practical", "craft"), "travel:calder:" + ground);
            default -> new BanterDraft("", List.of(), "");
        };
    }

    private List<String> tagged(List<String> baseTags, String... extraTags) {
        List<String> tags = new ArrayList<>();
        if (baseTags != null) {
            for (String tag : baseTags) {
                addTag(tags, tag);
            }
        }
        if (extraTags != null) {
            for (String tag : extraTags) {
                addTag(tags, tag);
            }
        }
        return tags;
    }

    private void addTag(List<String> tags, String tag) {
        if (tag == null || tag.isBlank()) {
            return;
        }
        String normalized = tag.strip().toLowerCase(Locale.ROOT);
        if (!tags.contains(normalized)) {
            tags.add(normalized);
        }
    }

    private boolean recentlyUsedBanter(String speaker, String key) {
        cleanupRecentTravelBanterKeys();
        if (speaker == null || key == null || key.isBlank()) {
            return false;
        }
        return recentTravelBanterKeys.containsKey(speaker + "|" + key);
    }

    private void markBanterUsed(String speaker, String key) {
        cleanupRecentTravelBanterKeys();
        if (speaker == null || key == null || key.isBlank()) {
            return;
        }
        recentTravelBanterKeys.put(speaker + "|" + key, worldTick);
        while (recentTravelBanterKeys.size() > 36) {
            String oldest = recentTravelBanterKeys.keySet().iterator().next();
            recentTravelBanterKeys.remove(oldest);
        }
    }

    private void cleanupRecentTravelBanterKeys() {
        recentTravelBanterKeys.entrySet().removeIf(entry -> worldTick - entry.getValue() > TICKS_PER_GAME_DAY * 2);
    }

    private List<String> banterReplyOptions(Actor ally, List<String> tags, int approvalDelta) {
        String recruitId = recruitIdForAlly(ally);
        if (tags.contains("combat")) {
            if (tags.contains("hard") || tags.contains("reckless") || approvalDelta < 0) {
                return List.of(
                        "You are right. I pushed too hard.",
                        "We survived because you held.",
                        "Say what you saw."
                );
            }
            return List.of(
                    "You held the line with me.",
                    "Check everyone before we move.",
                    "Keep your eyes on the road."
            );
        }
        if (tags.contains("oathstead") || tags.contains("build")) {
            return List.of(
                    "I want this place to feel like yours too.",
                    "Tell me what it still needs.",
                    "Good. We keep building."
            );
        }
        if (tags.contains("companion_request")) {
            return List.of(
                    "That matters to you. I hear it.",
                    "Tell me exactly what you need from me.",
                    "Not now, but I have not forgotten."
            );
        }
        if (approvalDelta < 0) {
            return List.of(
                    "Tell me what sat wrong with you.",
                    "I made the call I could live with.",
                    "Not now."
            );
        }
        return switch (recruitId) {
            case "aria" -> List.of("You noticed something before I did.", "What did the road tell you?", "Stay sharp with me.");
            case "vesper" -> List.of("You heard something in this place.", "What should I leave undisturbed?", "We will move gently.");
            case "rafiq" -> List.of("That sounded almost sincere.", "Make the joke, then the truth.", "Try not to admire the danger.");
            case "calder" -> List.of("What would you check first?", "You make the road sound like a beam under strain.", "Practical answer, then.");
            case "seraphine" -> List.of("You think there is a price hidden here.", "Name the terms you see.", "I trust your suspicion.");
            case "lyra" -> List.of("You are watching everyone breathe again.", "Tell me who you are worried about.", "I will not make you carry that alone.");
            case "samir" -> List.of("You sound like the road asked a question.", "Tell me what the light is showing you.", "We can doubt and still move.");
            case "maera" -> List.of("You are reading the ground like a record.", "What detail would others miss?", "Put it in plain words for me.");
            case "cassia" -> List.of("You are measuring the edges again.", "What would break formation here?", "I will hold my side.");
            default -> List.of("I am listening.", "What did you notice?", "We keep moving.");
        };
    }

    private List<String> banterReplyTooltips(List<String> tags, int approvalDelta) {
        if (tags.contains("combat") || approvalDelta < 0) {
            return List.of(
                    "Own the risk and invite honesty. Improves trust.",
                    "Recognize their contribution. Improves trust.",
                    "Ask for a tactical read. Small trust gain."
            );
        }
        return List.of(
                "Warm, personal reply. Improves trust.",
                "Curious reply that asks for their perspective. Improves trust.",
                "Practical reply. Keeps momentum."
        );
    }

    private List<Integer> banterReplyDeltas(List<String> tags, int approvalDelta) {
        if (approvalDelta < 0) {
            return List.of(1, 0, -1);
        }
        if (tags.contains("combat") || tags.contains("companion_request")) {
            return List.of(1, 1, 0);
        }
        return List.of(1, 1, 0);
    }

    private List<String> banterResponseLines(Actor ally, List<String> tags, int approvalDelta) {
        List<Integer> deltas = banterReplyDeltas(tags, approvalDelta);
        List<String> responses = new ArrayList<>();
        for (int i = 0; i < deltas.size(); i++) {
            responses.add(banterResponseLine(ally, tags, i, deltas.get(i)));
        }
        return responses;
    }

    private String banterResponseLine(Actor ally, List<String> tags, int optionIndex, int delta) {
        if (ally == null) {
            return "";
        }
        String recruitId = recruitIdForAlly(ally);
        int relationship = npcRelationship(npcForAlly(ally));
        String tier = relationship >= 150 ? "high" : relationship >= 70 ? "mid" : "low";
        String memory = latestCompanionMemoryText(recruitId);
        if (tags.contains("combat")) {
            return companionCombatReplyLine(ally, recruitId, tier, optionIndex, memory);
        }
        if (tags.contains("oathstead") || tags.contains("build")) {
            return companionOathsteadReplyLine(ally, recruitId, tier, optionIndex);
        }
        if (delta < 0 || optionIndex == 2 && !tags.contains("exploration")) {
            return companionDistantReplyLine(ally, recruitId, tier);
        }
        if (optionIndex == 0 && !memory.isBlank() && relationship >= 70) {
            return companionMemoryAwareReplyLine(ally, recruitId, tier, memory);
        }
        return companionExplorationReplyLine(ally, recruitId, tier, optionIndex);
    }

    private String companionCombatReplyLine(Actor ally, String recruitId, String tier, int optionIndex, String memory) {
        if (optionIndex == 2) {
            return switch (recruitId) {
                case "aria" -> ally.name + ": I saw the flank open before the first shout. Next time, we move before they learn we noticed.";
                case "lyra" -> ally.name + ": I saw breathing turn ragged. That is the part victory songs always forget.";
                case "calder" -> ally.name + ": Left side nearly folded. We fix that before the next impact.";
                default -> ally.name + ": I saw enough to respect the danger. That is the useful answer.";
            };
        }
        if ("high".equals(tier) && !memory.isBlank()) {
            return ally.name + ": I will say it plainly because you have earned plain words. I remembered " + memory.toLowerCase(Locale.ROOT) + " while we fought.";
        }
        return switch (recruitId) {
            case "seraphine" -> ally.name + ": Then let us choose terms sooner next time. I dislike surviving by accident.";
            case "maera" -> ally.name + ": Good. We keep the lesson, not the panic.";
            case "cassia" -> ally.name + ": You held. I can work with someone who holds.";
            case "lyra" -> ally.name + ": Then let me count everyone twice, and we can call it victory.";
            case "samir" -> ally.name + ": Courage listens better when pride is quiet.";
            case "aria" -> ally.name + ": I noticed you listening. That matters more than the apology.";
            case "vesper" -> ally.name + ": Good. The damage needs witnesses before it becomes wisdom.";
            case "rafiq" -> ally.name + ": Excellent. We are both alive and almost mature about it.";
            case "calder" -> ally.name + ": Good. Inspection first, boasting never if we can help it.";
            default -> ally.name + ": Good. Then the next fight starts with cleaner eyes.";
        };
    }

    private String companionOathsteadReplyLine(Actor ally, String recruitId, String tier, int optionIndex) {
        if (optionIndex == 1) {
            return switch (recruitId) {
                case "calder" -> ally.name + ": Dry storage, better braces, and people who stop calling temporary repairs temporary after a year.";
                case "vesper" -> ally.name + ": A place where roots are not ripped up just because the road is impatient.";
                case "lyra" -> ally.name + ": More rest than pride allows, and a corner where fear can sit without being mocked.";
                default -> ally.name + ": It needs people who can argue and still return to the same hearth.";
            };
        }
        return "high".equals(tier)
                ? ally.name + ": Then I will let myself want that. Carefully. Wanting a home is not a small risk."
                : ally.name + ": Then keep making it honest. A place can lie as easily as a person.";
    }

    private String companionDistantReplyLine(Actor ally, String recruitId, String tier) {
        return switch (recruitId) {
            case "seraphine" -> ally.name + ": Convenient distance. I know the shape. We can return to it when you are less fond of it.";
            case "aria" -> ally.name + ": Fine. I know how to keep a thought packed for later.";
            case "vesper" -> ally.name + ": Then later. Roots can wait. They do not forget.";
            case "rafiq" -> ally.name + ": Ah, the tactical retreat from feelings. A classic maneuver.";
            case "calder" -> ally.name + ": Later, then. Weight does not vanish because we stop naming it.";
            default -> ally.name + ": Later, then. I heard the answer too.";
        };
    }

    private String companionMemoryAwareReplyLine(Actor ally, String recruitId, String tier, String memory) {
        String remembered = memory.length() > 110 ? memory.substring(0, 107) + "..." : memory;
        return switch (recruitId) {
            case "aria" -> ally.name + ": I remember that too. " + remembered + " I notice who comes back after the trail gets ugly.";
            case "vesper" -> ally.name + ": It is still growing in me. " + remembered + " Some things need time before they become trust.";
            case "rafiq" -> ally.name + ": Do not look so pleased. I remember it too: " + remembered;
            case "calder" -> ally.name + ": That stayed with me. " + remembered + " Some repairs begin after the work looks finished.";
            case "seraphine" -> ally.name + ": I remember the terms of that moment. " + remembered + " You did not spend trust cheaply.";
            case "lyra" -> ally.name + ": I remember. " + remembered + " Care becomes real when someone returns to it.";
            case "samir" -> ally.name + ": I have carried that quietly. " + remembered + " It still gives light from an unexpected angle.";
            case "maera" -> ally.name + ": I kept that in the margin. " + remembered + " It refused to become a simple note.";
            case "cassia" -> ally.name + ": I remember where you stood. " + remembered + " People reveal themselves under weight.";
            default -> ally.name + ": I remember that. " + remembered;
        };
    }

    private String companionExplorationReplyLine(Actor ally, String recruitId, String tier, int optionIndex) {
        if ("high".equals(tier) && optionIndex == 0) {
            return switch (recruitId) {
                case "aria" -> ally.name + ": You asking like that makes the road feel less like a thing I have to survive alone.";
                case "vesper" -> ally.name + ": Then walk quietly with me. I trust you more when you do not rush the answer.";
                case "rafiq" -> ally.name + ": Careful. If you keep listening, I may become sincere and ruin my reputation.";
                case "calder" -> ally.name + ": Good. I am tired of pretending practical things do not become personal.";
                default -> ally.name + ": Good. I have started believing you mean that.";
            };
        }
        if ("mid".equals(tier)) {
            return ally.name + ": Good. I can work with a question that makes room for an answer.";
        }
        return ally.name + ": Then listen with your feet too. Roads punish careless attention.";
    }

    private void recordBanterMemory(Actor ally, List<String> tags, int optionIndex) {
        String recruitId = recruitIdForAlly(ally);
        if (recruitId.isBlank()) {
            return;
        }
        String category = tags.contains("combat") ? "banter_combat" : tags.contains("oathstead") ? "banter_oathstead" : "banter";
        String text = optionIndex == 0
                ? "The player answered a passing comment with warmth instead of treating it as noise."
                : "The player asked for the companion's perspective during travel.";
        recordCompanionMemory(recruitId, category, text);
    }

    public TravelBanterPrompt activeTravelBanter() {
        return activeTravelBanter;
    }

    public AmbientNpcConversation activeTownConversation() {
        if (activeTownConversation == null || !currentMapId.equals(activeTownConversation.mapId())) {
            return null;
        }
        return activeTownConversation;
    }

    public String npcRenderKey(Npc npc) {
        return npcRelationshipKey(npc);
    }

    public void replyToTravelBanter(int optionIndex) {
        if (activeTravelBanter == null || optionIndex < 0 || optionIndex >= activeTravelBanter.options().size()) {
            return;
        }
        if (partyDialogue.owns(activeTravelBanter)) {
            activeTravelBanter = partyDialogue.reply(this, optionIndex);
            nextTravelBanterTick = worldTick + 1300;
            invalidateQuestObjectiveCache();
            return;
        }
        if (activeTravelBanter.tags().contains("banter_response")) {
            activeTravelBanter = null;
            nextTravelBanterTick = pendingCompanionComments.isEmpty()
                    ? worldTick + 900 + random.nextInt(1200)
                    : worldTick + 120;
            return;
        }
        String speaker = activeTravelBanter.speaker();
        Actor ally = allyByName(speaker);
        Npc npc = npcForAlly(ally);
        int delta = optionIndex < activeTravelBanter.optionRelationshipDeltas().size()
                ? activeTravelBanter.optionRelationshipDeltas().get(optionIndex)
                : 0;
        if (delta != 0) {
            adjustNpcRelationshipDirect(npc, delta);
        }
        List<String> responses = activeTravelBanter.optionResponseLines();
        String response = optionIndex < responses.size() ? responses.get(optionIndex) : "";
        if (response.isBlank() && ally != null) {
            response = banterResponseLine(ally, activeTravelBanter.tags(), optionIndex, delta);
        }
        if (!response.isBlank()) {
            activeTravelBanter = new TravelBanterPrompt(
                    speaker,
                    response,
                    List.of("We keep moving."),
                    List.of("Close the exchange and return to travel."),
                    List.of(0),
                    tagged(activeTravelBanter.tags(), "banter_response"),
                    List.of(""),
                    worldTick + TRAVEL_BANTER_DURATION_TICKS
            );
            if (ally != null && delta > 0) {
                recordBanterMemory(ally, activeTravelBanter.tags(), optionIndex);
            }
            nextTravelBanterTick = worldTick + 900 + random.nextInt(1200);
            return;
        }
        status = speaker + " lets the thought settle and keeps pace.";
        activeTravelBanter = null;
        if (!pendingCompanionComments.isEmpty()) {
            nextTravelBanterTick = worldTick + 120;
            return;
        }
        nextTravelBanterTick = worldTick + 900 + random.nextInt(1200);
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
        invalidateNpcListCache();
    }

    public void resetDungeonMonsterRuntime() {
        dungeonMonsterRuntime.clear();
        defeatedDungeonMonsters.clear();
        activeDungeonMonster = null;
    }

    public void resetQuestMonsterRuntime() {
        questMonsterRuntime.clear();
        questNpcRuntime.clear();
        activeQuestMonster = null;
        invalidateNpcListCache();
        invalidateQuestObjectiveCache();
    }

    private void invalidateNpcListCache() {
        npcListCache.clear();
        npcListCacheTick = -1;
    }

    private void invalidateQuestObjectiveCache() {
        activeQuestObjectiveCache = null;
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

    boolean residentsYieldToPlayer(String mapId) {
        String kind = world.kind(mapId);
        return "city".equals(kind) || "village".equals(kind) || "interior".equals(kind);
    }

    /** Friendly residents are soft obstacles. A safe sidestep is cosmetic, never required to pass. */
    private void yieldResidentToPlayer(Npc npc, int playerDx, int playerDy) {
        if (questNpc(npc)) return; // Scripted characters keep their authored position.
        NpcRuntime runtime = runtimeFor(npc);
        if (worldTick - runtime.moveStartTick < NpcMotion.MOVE_TICKS) return;
        int dx = Integer.compare(playerDx, 0), dy = Integer.compare(playerDy, 0);
        // Try sideways first so the resident does not keep retreating along the player's path.
        int[][] directions = {{-dy, dx}, {dy, -dx}, {-dx, -dy}, {dx, dy},
                {0, 1}, {1, 0}, {0, -1}, {-1, 0}};
        for (int[] direction : directions) {
            if (Math.abs(direction[0]) + Math.abs(direction[1]) != 1) continue;
            if (direction[0] * dx + direction[1] * dy > 0) continue;
            int x = runtime.x + direction[0], y = runtime.y + direction[1];
            if ((x == playerX && y == playerY) || !world.isPassable(npc.mapId(), x, y)
                    || world.transitionAt(npc.mapId(), x, y) != null
                    || blockingQuestObjectiveAt(npc.mapId(), x, y) != null
                    || npcAt(npc.mapId(), x, y) != null) continue;
            boolean reserved = false;
            for (Npc other : npcsForMap(npc.mapId())) {
                if (other.equals(npc) || questNpc(other)) continue;
                NpcRuntime otherRuntime = runtimeFor(other);
                if (worldTick - otherRuntime.moveStartTick < NpcMotion.MOVE_TICKS
                        && otherRuntime.fromX == x && otherRuntime.fromY == y) {
                    reserved = true;
                    break;
                }
            }
            if (reserved || !PropCollision.canTravel(world, npc.mapId(), runtime.x + 0.5, runtime.y + 0.5,
                    x + 0.5, y + 0.5)) continue;
            runtime.fromX = runtime.x;
            runtime.fromY = runtime.y;
            runtime.x = x;
            runtime.y = y;
            runtime.facingDx = direction[0];
            runtime.facingDy = direction[1];
            runtime.moveStartTick = worldTick;
            runtime.townPath = List.of();
            runtime.pathIndex = 0;
            runtime.routeDestination = null;
            runtime.blockedAttempts = 0;
            runtime.nextThinkTick = worldTick + NpcMotion.MOVE_TICKS + 90;
            return;
        }
        // In a narrow corridor the player can squeeze past without displacing anyone.
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

    private QuestMonsterRuntime questMonsterAt(String mapId, int x, int y) {
        if (!WorldMap.OVERWORLD_ID.equals(mapId)) {
            return null;
        }
        ensureActiveQuestMonsterRuntime();
        return questMonsterAtRuntime(x, y, null);
    }

    private QuestMonsterRuntime questMonsterAtRuntime(int x, int y, QuestMonsterRuntime ignored) {
        for (List<QuestMonsterRuntime> monsters : questMonsterRuntime.values()) {
            for (QuestMonsterRuntime monster : monsters) {
                if (monster != ignored && monster.x == x && monster.y == y) {
                    return monster;
                }
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
        List<WorldMap.DungeonEncounterSlot> planned = world.dungeonEncounterSlots(mapId);
        if (!planned.isEmpty()) {
            Random seeded = new Random(mapId.hashCode() * 31L + 1701L);
            List<DungeonMonsterRuntime> monsters = new ArrayList<>();
            int roamer = 0;
            for (WorldMap.DungeonEncounterSlot slot : planned) {
                String key = slot.boss()
                        ? chooseDungeonBoss(mapId)
                        : chooseDungeonRoamer(mapId, slot.role(), seeded);
                // Keep the legacy sequential roamer id shape so old defeated-monster saves remain useful.
                String id = slot.boss() ? mapId + ":boss" : mapId + ":" + roamer++;
                monsters.add(new DungeonMonsterRuntime(id,
                        GameData.MONSTERS.getOrDefault(key, GameData.MONSTERS.get("skeleton")),
                        slot.boss(), slot.x(), slot.y(), seeded.nextInt(80), slot.leash()));
            }
            return monsters;
        }
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
            monsters.add(new DungeonMonsterRuntime(mapId + ":boss", GameData.MONSTERS.getOrDefault(bossKey, GameData.MONSTERS.get("wraith")), true, bossPoint.x(), bossPoint.y(), seeded.nextInt(80), 6));
        }
        int count = Math.min(candidates.size(), dungeonRoamerCount(mapId));
        for (int i = 0; i < count; i++) {
            int index = seeded.nextInt(candidates.size());
            TilePoint point = candidates.remove(index);
            String key = chooseDungeonRoamer(mapId, seeded);
            monsters.add(new DungeonMonsterRuntime(mapId + ":" + i, GameData.MONSTERS.getOrDefault(key, GameData.MONSTERS.get("skeleton")), false, point.x(), point.y(), seeded.nextInt(80), 9));
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
        return tile == 'd' || tile == 'D' || tile == 'F' || tile == 'M' || tile == 'R' || tile == 'S' || tile == 'L'
                || tile == 'N' || tile == 'E' || tile == 'I' || tile == 'J' || tile == 'H' || tile == 'Q'
                || tile == '1' || tile == '2' || tile == '3' || tile == '4' || tile == '5' || tile == '6';
    }

    private int dungeonDepth(String mapId) {
        int underscore = mapId.lastIndexOf('_');
        if (underscore >= 0 && underscore < mapId.length() - 1) {
            try {
                return Math.max(1, Integer.parseInt(mapId.substring(underscore + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return mapId.contains("deep") ? 2 : 1;
    }

    private int dungeonRoamerCount(String mapId) {
        return switch (dungeonSpawnTheme(mapId)) {
            case "bandit", "goblin" -> 4 + Math.min(2, dungeonDepth(mapId));
            default -> 3 + Math.min(2, dungeonDepth(mapId));
        };
    }

    private String dungeonSpawnTheme(String mapId) {
        WorldMap.DungeonContext context = world.dungeonContext(mapId);
        if (context == null) return "crypt";
        return switch (context.theme()) {
            case "goblin_camp" -> "goblin";
            case "bandit_camp" -> "bandit";
            case "abandoned_castle" -> "castle";
            case "prison" -> "prison";
            case "sewer" -> "sewer";
            case "crypt" -> "crypt";
            case "cave" -> {
                yield switch (CavernStyle.biome(context.region(), context.exterior())) {
                    case "ice" -> "frost_cave";
                    case "moss" -> "mire_cave";
                    case "sand" -> "sand_cave";
                    default -> "cave";
                };
            }
            default -> "crypt";
        };
    }

    private String chooseDungeonBoss(String mapId) {
        int depth = dungeonDepth(mapId);
        return switch (dungeonSpawnTheme(mapId)) {
            case "goblin" -> "goblin_warlord";
            case "bandit" -> "bandit_captain";
            case "frost_cave" -> depth > 2 ? "mountain_drake" : "frost_troll";
            case "mire_cave" -> depth > 2 ? "marsh_drake" : "swamp_troll";
            case "sand_cave" -> depth > 2 ? "ash_scorpion" : "sand_stalker";
            case "cave" -> depth > 2 ? "mountain_drake" : "frost_troll";
            case "castle" -> depth > 2 ? "nameless_warden" : "void_knight";
            case "prison" -> depth > 2 ? "void_knight" : "bone_knight";
            case "sewer" -> depth > 2 ? "swamp_troll" : "bog_beast";
            case "crypt" -> depth > 2 ? "elder_wraith" : "bone_knight";
            default -> "bone_knight";
        };
    }

    private String chooseDungeonRoamer(String mapId, Random seeded) {
        int depth = dungeonDepth(mapId);
        List<String> pool = switch (dungeonSpawnTheme(mapId)) {
            case "goblin" -> List.of(
                    "goblin_scout", "goblin", "goblin_archer", "goblin_trapper", "goblin_skirmisher", "goblin_shaman");
            case "bandit" -> List.of(
                    "bandit_cutthroat", "bandit_cutthroat", "bandit_archer", "goblin_scout", "bandit_captain", "orc_raider", "wolf");
            case "frost_cave" -> depth > 1
                    ? List.of("crypt_bat", "spider", "frost_wolf", "snow_lynx", "frost_troll", "ice_golem", "mountain_drake", "bat", "bat")
                    : List.of("crypt_bat", "spider", "bat", "frost_wolf", "bandit_cutthroat", "bat");
            case "mire_cave" -> depth > 1
                    ? List.of("slime", "spider", "reed_serpent", "bog_beast", "river_eel", "marsh_drake", "swamp_troll", "bat")
                    : List.of("slime", "spider", "bat", "reed_serpent", "bog_beast", "river_eel");
            case "sand_cave" -> depth > 1
                    ? List.of("bat", "spider", "glass_scorpion", "sand_stalker", "ember_imp", "ash_scorpion", "bat")
                    : List.of("bat", "bat", "spider", "glass_scorpion", "sand_stalker");
            case "cave" -> depth > 1
                    ? List.of("crypt_bat", "spider", "slime", "orc_raider", "mountain_drake", "bat", "wolf")
                    : List.of("crypt_bat", "bat", "spider", "slime", "orc_raider", "wolf");
            case "castle" -> depth > 1
                    ? List.of("skeleton", "crypt_bat", "wraith", "bone_knight", "crypt_revenant", "void_knight", "elder_wraith", "spider")
                    : List.of("skeleton", "crypt_bat", "wraith", "bone_knight", "crypt_revenant", "spider");
            case "prison" -> depth > 1
                    ? List.of("skeleton", "bone_knight", "wraith", "crypt_revenant", "void_knight", "bandit_cutthroat")
                    : List.of("skeleton", "spider", "bone_knight", "wraith", "bandit_cutthroat", "bat");
            case "sewer" -> depth > 1
                    ? List.of("slime", "bat", "river_eel", "reed_serpent", "spider", "bog_beast", "swamp_troll")
                    : List.of("slime", "bat", "river_eel", "reed_serpent", "spider", "bog_beast");
            case "crypt" -> depth > 1
                    ? List.of("skeleton", "spider", "crypt_bat", "wraith", "bone_knight", "crypt_revenant", "elder_wraith")
                    : List.of("skeleton", "skeleton", "crypt_bat", "wraith", "bone_knight", "spider");
            default -> List.of("crypt_bat", "skeleton", "spider", "wraith");
        };
        return pool.get(seeded.nextInt(pool.size()));
    }

    private String chooseDungeonRoamer(String mapId, String roomRole, Random seeded) {
        String theme = dungeonSpawnTheme(mapId);
        List<String> rolePool = switch (roomRole) {
            case "guard" -> switch (theme) {
                case "goblin" -> List.of("goblin_scout", "goblin_archer", "goblin_skirmisher");
                case "bandit" -> List.of("bandit_cutthroat", "bandit_archer", "orc_raider");
                case "crypt", "castle", "prison" -> List.of("skeleton", "bone_knight", "crypt_revenant");
                default -> List.of();
            };
            case "shrine", "burial" -> switch (theme) {
                case "crypt", "castle", "prison" -> List.of("wraith", "skeleton", "crypt_bat", "crypt_revenant");
                case "goblin" -> List.of("goblin_shaman", "goblin_trapper");
                default -> List.of();
            };
            case "works" -> switch (theme) {
                case "sewer", "mire_cave" -> List.of("slime", "river_eel", "spider");
                case "goblin" -> List.of("goblin_trapper", "goblin_scout");
                case "bandit" -> List.of("bandit_archer", "bandit_cutthroat");
                default -> List.of();
            };
            default -> List.of();
        };
        return rolePool.isEmpty() || seeded.nextInt(5) == 0
                ? chooseDungeonRoamer(mapId, seeded) : rolePool.get(seeded.nextInt(rolePool.size()));
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
        battle = createBattle(specs, world.tileAt(currentMapId, monster.x, monster.y), world.kind(currentMapId));
        prepareBattleKnowledge(battle);
        mode = GameMode.BATTLE;
        status = monster.spec.name() + " attacks!";
    }

    private void startQuestMonsterBattle(QuestMonsterRuntime monster) {
        Quest quest = quests.get(monster.questId);
        List<GameData.MonsterSpec> specs = quest == null
                ? monsterSpecsForKeys(List.of(monster.spec.key()))
                : storyObjectiveMonsterSpecs(monster.spec.key());
        activeQuestMonster = monster;
        battle = createBattle(specs, world.tileAt(WorldMap.OVERWORLD_ID, monster.x, monster.y), world.kind(WorldMap.OVERWORLD_ID));
        prepareBattleKnowledge(battle);
        mode = GameMode.BATTLE;
        status = monster.spec.name() + " answers " + (quest == null ? "the hunt" : quest.title) + ".";
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

    private void clearDefeatedQuestMonster() {
        if (activeQuestMonster == null) {
            return;
        }
        List<QuestMonsterRuntime> monsters = questMonsterRuntime.get(activeQuestMonster.questId);
        if (monsters != null) {
            monsters.removeIf(monster -> monster.id.equals(activeQuestMonster.id));
        }
        activeQuestMonster = null;
        invalidateQuestObjectiveCache();
    }

    public void maybeStartEncounter() {
        char tile = world.tileAt(currentMapId, playerX, playerY);
        String kind = world.kind(currentMapId);
        if ("dungeon".equals(kind)) {
            return;
        }
        String locationKind = world.locationKindAt(currentMapId, playerX, playerY);
        boolean hostileSite = !locationMonsterPool(locationKind).isEmpty();
        if ((Terrain.connectingRoad(tile) && !hostileSite) || "city".equals(kind) || "village".equals(kind) || "interior".equals(kind)) {
            return;
        }
        double chance = tile == 'd' ? config.dungeonEntranceEncounterChance : config.wildEncounterChance;
        if (worldAbilityActive("encounter_ward")) {
            chance *= 0.35;
        }
        if (worldAbilityActive("encounter_lure")) {
            chance *= 2.25;
        }
        if (random.nextDouble() >= chance) {
            return;
        }
        char monsterTile = "dungeon".equals(kind) ? 'd' : tile;
        List<GameData.MonsterSpec> specs = chooseMonsterGroup(monsterTile, locationKind).stream()
                .map(key -> GameData.MONSTERS.getOrDefault(key, GameData.MONSTERS.get("slime")))
                .toList();
        battle = createBattle(specs, tile, kind);
        prepareBattleKnowledge(battle);
        mode = GameMode.BATTLE;
        status = "Battle!";
    }

    public List<QuestObjective> activeQuestObjectives() {
        if (activeQuestObjectiveCache != null) {
            return activeQuestObjectiveCache;
        }
        List<QuestObjective> objectives = new ArrayList<>();
        pruneQuestMonsterRuntime();
        for (Quest quest : quests.values()) {
            if (PartyDialogue.isShared(quest)) continue;
            if (!quest.accepted || quest.completed) {
                continue;
            }
            if (quest.ready()) {
                addQuestReturnObjective(objectives, quest);
                continue;
            }
            if (quest.activeObjectiveKind().conversationObjective()) {
                addConversationQuestObjectives(objectives, quest);
                continue;
            }
            if (quest.activeObjectiveKind().defenseMinigameObjective()) {
                String mapId = raidDefenseMapId(quest);
                TilePoint point = raidDefenseObjectivePoint(quest, mapId);
                objectives.add(questObjective(
                        quest,
                        quest.id,
                        quest.title,
                        quest.activeTarget(),
                        mapId,
                        point.x(),
                        point.y(),
                        quest.activeObjectiveAsset() == null || quest.activeObjectiveAsset().isBlank()
                                ? "icon_shield"
                                : quest.activeObjectiveAsset(),
                        quest.activeObjectiveKind(),
                        quest.activeMonsterKey(),
                        false,
                        false
                ));
                continue;
            }
            if (quest.activeObjectiveKind().combatObjective()) {
                for (QuestMonsterRuntime monster : questMonstersForQuest(quest)) {
                    objectives.add(questObjective(
                            quest,
                            quest.id,
                            quest.title,
                            quest.activeTarget(),
                            WorldMap.OVERWORLD_ID,
                            monster.x,
                            monster.y,
                            monster.spec.sprite(),
                            quest.activeObjectiveKind(),
                            monster.spec.key(),
                            true,
                            false
                    ));
                }
                continue;
            }
            if (!quest.hasWorldObjective()) {
                continue;
            }
            int remaining = Math.max(0, quest.activeNeeded() - quest.progress);
            Set<TilePoint> usedObjectivePoints = new HashSet<>();
            String asset = quest.activeObjectiveAsset() != null && !quest.activeObjectiveAsset().isBlank()
                    ? quest.activeObjectiveAsset()
                    : quest.activeMonsterKey() != null ? quest.activeMonsterKey() : "icon_chest";
            int added = 0;
            for (int variant = 0; variant < quest.activeNeeded() && added < remaining; variant++) {
                TilePoint point = uniqueQuestObjectivePoint(quest, variant, asset, usedObjectivePoints);
                if (questObjectiveSlotHandled(quest, point, asset)) {
                    continue;
                }
                objectives.add(questObjective(
                        quest,
                        quest.id,
                        quest.title,
                        quest.activeTarget(),
                        quest.activeObjectiveMapId(),
                        point.x(),
                        point.y(),
                        asset,
                        quest.activeObjectiveKind(),
                        quest.activeMonsterKey(),
                        quest.activeObjectiveKind().combatObjective(),
                        false
                ));
                added++;
            }
        }
        activeQuestObjectiveCache = List.copyOf(objectives);
        return activeQuestObjectiveCache;
    }

    private void addQuestReturnObjective(List<QuestObjective> objectives, Quest quest) {
        Npc giver = questGiver(quest.id);
        if (giver == null) {
            return;
        }
        TilePoint home = world.npcHome(giver);
        objectives.add(questObjective(
                quest,
                quest.id,
                "Return to " + world.label(giver.mapId()),
                giver.name(),
                giver.mapId(),
                home.x(),
                home.y(),
                giver.sprite(),
                Quest.ObjectiveKind.REPORT,
                quest.activeMonsterKey(),
                false,
                true
        ));
    }

    private void addConversationQuestObjectives(List<QuestObjective> objectives, Quest quest) {
        int remaining = Math.max(0, quest.activeNeeded() - quest.progress);
        if (remaining <= 0) {
            return;
        }
        int added = 0;
        for (Npc npc : conversationObjectiveMarkerNpcs(quest)) {
            if (added >= remaining) {
                break;
            }
            if (harvestedQuestResources.contains(conversationObjectiveKey(quest, npc))) {
                continue;
            }
            TilePoint home = world.npcHome(npc);
            objectives.add(questObjective(
                    quest,
                    quest.id,
                    quest.objectiveAction() + ": " + world.label(npc.mapId()),
                    npc.name(),
                    npc.mapId(),
                    home.x(),
                    home.y(),
                    npc.sprite(),
                    quest.activeObjectiveKind(),
                    quest.activeMonsterKey(),
                    false,
                    true
            ));
            added++;
        }
    }

    private List<Npc> conversationObjectiveMarkerNpcs(Quest quest) {
        if (quest == null) {
            return List.of();
        }
        if (questNeedsQuestNpc(quest)) {
            return questNpcsForQuest(quest).stream()
                    .map(runtime -> runtime.npc)
                    .toList();
        }
        Npc target = questTargetSourceNpc(quest);
        if (target != null) {
            return List.of(target);
        }
        if (quest.activeObjectiveKind() == Quest.ObjectiveKind.ASK_AROUND) {
            Npc giver = questGiver(quest.id);
            if (giver == null) {
                return List.of();
            }
            return GameData.NPCS.stream()
                    .filter(npc -> npc.mapId().equals(giver.mapId()))
                    .filter(npc -> !npcRelationshipKey(npc).equals(npcRelationshipKey(giver)))
                    .filter(npc -> npc.questId() == null)
                    .filter(npc -> !harvestedQuestResources.contains(conversationObjectiveKey(quest, npc)))
                    .limit(Math.max(1, quest.activeNeeded() - quest.progress))
                    .toList();
        }
        Npc giver = questGiver(quest.id);
        return giver == null ? List.of() : List.of(giver);
    }

    private QuestObjective questObjective(
            Quest quest,
            String questId,
            String title,
            String target,
            String mapId,
            int x,
            int y,
            String asset,
            Quest.ObjectiveKind kind,
            String monsterKey,
            boolean blocking,
            boolean markerOnly
    ) {
        WorldMap.AdventureMarker dungeon = markerOnly && quest != null && quest.ready() ? null : dungeonMarkerFor(quest);
        String markerLabel = questMarkerLabel(quest, title, target, mapId, dungeon);
        return new QuestObjective(
                questId,
                title,
                target,
                mapId,
                x,
                y,
                asset,
                kind,
                monsterKey,
                blocking,
                markerLabel,
                markerOnly,
                quest != null && quest.mainStoryQuest(),
                dungeon != null,
                dungeon == null ? -1 : dungeon.x(),
                dungeon == null ? -1 : dungeon.y(),
                quest == null ? "" : quest.activeStage().id()
        );
    }

    private String questMarkerLabel(Quest quest, String title, String target, String mapId, WorldMap.AdventureMarker dungeon) {
        StoryLocationCatalog.Place fixedPlace = quest == null ? null : StoryLocationCatalog.forQuest(quest.id);
        if (fixedPlace != null && fixedPlace.outdoorSite() && !quest.ready())
            return fixedPlace.name() + ": " + target;
        if (dungeon != null) {
            return title + ": " + dungeon.label();
        }
        String kind = mapId == null ? "" : world.kind(mapId);
        if ("city".equals(kind) || "village".equals(kind) || "interior".equals(kind)) {
            String sourceMap = "interior".equals(kind) ? sourceMapForInterior(mapId) : mapId;
            String place = sourceMap == null || sourceMap.isBlank() ? world.label(mapId) : world.label(sourceMap);
            if (quest != null && quest.ready()) {
                return "Return to " + place;
            }
            if (quest != null && quest.activeObjectiveKind().conversationObjective()) {
                return quest.objectiveAction() + ": " + target + " in " + place;
            }
            return title + ": " + place;
        }
        return title;
    }

    private WorldMap.AdventureMarker dungeonMarkerFor(Quest quest) {
        if (quest == null) {
            return null;
        }
        StoryLocationCatalog.Place fixedPlace = StoryLocationCatalog.forQuest(quest.id);
        if (fixedPlace != null && fixedPlace.outdoorSite()) {
            return world.adventureMarkers().stream().filter(m -> m.mapId().equals(fixedPlace.adventureId()))
                    .findFirst().orElse(null);
        }
        String mapId = quest.activeObjectiveMapId();
        if (mapId != null && "dungeon".equals(world.kind(mapId))) {
            TilePoint entrance = world.overworldEntranceFor(mapId);
            if (entrance != null) {
                return new WorldMap.AdventureMarker("dungeon", mapId, world.label(mapId), entrance.x(), entrance.y(), 1);
            }
        }
        String locationKind = quest.activeObjectiveLocationKind();
        if (dungeonLocationKind(locationKind)) {
            WorldMap.AdventureMarker marker = world.adventureMarker(locationKind, quest.activeObjectiveLocationIndex());
            if (marker != null) {
                return marker;
            }
        }
        String questText = normalizeQuestMatchText(quest.title + " " + quest.description + " " + quest.activeStartDialog());
        for (WorldMap.AdventureMarker marker : world.adventureMarkers()) {
            if (questText.contains(normalizeQuestMatchText(marker.label()))) {
                return marker;
            }
        }
        return null;
    }

    private boolean dungeonLocationKind(String locationKind) {
        if (locationKind == null) {
            return false;
        }
        return switch (locationKind) {
            case "cave", "crypt", "abandoned_castle", "prison", "sewer", "bandit_camp", "goblin_camp" -> true;
            default -> false;
        };
    }

    private boolean questObjectiveSlotHandled(Quest quest, TilePoint point, String asset) {
        return harvestedQuestResources.contains(resourceKey(questStageResourceId(quest), quest.activeObjectiveMapId(), point.x(), point.y(), asset));
    }

    private TilePoint uniqueQuestObjectivePoint(Quest quest, int variant, String asset, Set<TilePoint> used) {
        variant = MainStoryContent.objectiveVariant(quest, variant);
        if ("story".equals(quest.activeObjectiveLocationKind())) {
            TilePoint point = world.storyObjectivePoint(quest.activeObjectiveMapId(), quest.activeObjectiveLocationIndex() + variant);
            used.add(point);
            return point;
        }
        TilePoint first = null;
        for (int attempt = 0; attempt < 72; attempt++) {
            TilePoint candidate = world.objectivePoint(quest.activeObjectiveLocationKind(), quest.activeObjectiveLocationIndex(), variant + attempt, asset);
            candidate = RegionalNpcQuests.accessiblePoint(world, quest, candidate, used);
            if (first == null) {
                first = candidate;
            }
            if (used.add(candidate)) {
                return candidate;
            }
        }
        TilePoint fallback = first == null
                ? new TilePoint(playerX, playerY)
                : nearbyUniqueObjectivePoint(first, quest, used);
        used.add(fallback);
        return fallback;
    }

    private TilePoint nearbyUniqueObjectivePoint(TilePoint origin, Quest quest, Set<TilePoint> used) {
        for (int radius = 1; radius <= 6; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != radius) {
                        continue;
                    }
                    TilePoint candidate = new TilePoint(origin.x() + dx, origin.y() + dy);
                    if (used.contains(candidate)) {
                        continue;
                    }
                    if (quest.activeObjectiveLocationKind() != null
                            && !quest.activeObjectiveLocationKind().isBlank()
                            && !quest.activeObjectiveLocationKind().equals(world.locationKindAt(quest.activeObjectiveMapId(), candidate.x(), candidate.y()))) {
                        continue;
                    }
                    if (world.isPassable(quest.activeObjectiveMapId(), candidate.x(), candidate.y())) {
                        return candidate;
                    }
                }
            }
        }
        return origin;
    }

    private String raidDefenseMapId(Quest quest) {
        if (quest == null || quest.activeObjectiveMapId() == null || quest.activeObjectiveMapId().isBlank()) {
            return WorldMap.PLAYER_VILLAGE_ID;
        }
        String mapId = quest.activeObjectiveMapId();
        return isSettlementMap(mapId) ? mapId : WorldMap.PLAYER_VILLAGE_ID;
    }

    private TilePoint raidDefenseObjectivePoint(Quest quest, String mapId) {
        String asset = quest.activeObjectiveAsset();
        if (asset != null && !asset.isBlank()) {
            for (WorldProp prop : world.props(mapId)) {
                if (asset.equals(prop.asset())) {
                    return new TilePoint(prop.x(), prop.y());
                }
            }
        }
        int centerX = Math.max(1, world.width(mapId) / 2);
        int centerY = Math.max(1, world.height(mapId) / 2);
        for (int radius = 0; radius <= 12; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != radius) {
                        continue;
                    }
                    int x = centerX + dx;
                    int y = centerY + dy;
                    if (world.isPassable(mapId, x, y)) {
                        return new TilePoint(x, y);
                    }
                }
            }
        }
        return new TilePoint(Math.max(1, Math.min(world.width(mapId) - 2, centerX)),
                Math.max(1, Math.min(world.height(mapId) - 2, centerY)));
    }

    private TilePoint nearbyRaidDefenseStartPoint(String mapId, TilePoint marker) {
        if (marker == null) {
            return new TilePoint(Math.max(1, world.width(mapId) / 2), Math.max(1, world.height(mapId) / 2));
        }
        if (world.isPassable(mapId, marker.x(), marker.y())) {
            return marker;
        }
        for (int radius = 1; radius <= 6; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != radius) {
                        continue;
                    }
                    int x = marker.x() + dx;
                    int y = marker.y() + dy;
                    if (world.isPassable(mapId, x, y)) {
                        return new TilePoint(x, y);
                    }
                }
            }
        }
        return new TilePoint(Math.max(1, Math.min(world.width(mapId) - 2, marker.x())),
                Math.max(1, Math.min(world.height(mapId) - 2, marker.y())));
    }

    private void ensureActiveQuestMonsterRuntime() {
        pruneQuestMonsterRuntime();
        for (Quest quest : quests.values()) {
            if (quest.accepted && !quest.completed && !quest.ready() && quest.activeObjectiveKind().combatObjective()) {
                questMonstersForQuest(quest);
            }
        }
    }

    private void pruneQuestMonsterRuntime() {
        questMonsterRuntime.entrySet().removeIf(entry -> {
            Quest quest = quests.get(entry.getKey());
            if (quest == null || !quest.accepted || quest.completed || quest.ready() || !quest.activeObjectiveKind().combatObjective()) {
                return true;
            }
            String monsterKey = questMonsterKey(quest);
            entry.getValue().removeIf(monster -> !monster.spec.key().equals(monsterKey));
            int remaining = Math.max(0, quest.activeNeeded() - quest.progress);
            while (entry.getValue().size() > remaining) {
                QuestMonsterRuntime removed = entry.getValue().remove(entry.getValue().size() - 1);
                if (activeQuestMonster == removed) {
                    activeQuestMonster = null;
                }
            }
            return entry.getValue().isEmpty() && remaining <= 0;
        });
    }

    private List<QuestMonsterRuntime> questMonstersForQuest(Quest quest) {
        String monsterKey = questMonsterKey(quest);
        GameData.MonsterSpec spec = monsterKey == null ? null : GameData.MONSTERS.get(monsterKey);
        int remaining = Math.max(0, quest.activeNeeded() - quest.progress);
        if (spec == null || remaining <= 0) {
            questMonsterRuntime.remove(quest.id);
            invalidateQuestObjectiveCache();
            return List.of();
        }
        List<QuestMonsterRuntime> monsters = questMonsterRuntime.computeIfAbsent(quest.id, ignored -> new ArrayList<>());
        if (monsters.removeIf(monster -> !monster.spec.key().equals(spec.key()))) {
            invalidateQuestObjectiveCache();
        }
        while (monsters.size() > remaining) {
            QuestMonsterRuntime removed = monsters.remove(monsters.size() - 1);
            if (activeQuestMonster == removed) {
                activeQuestMonster = null;
            }
            invalidateQuestObjectiveCache();
        }
        while (monsters.size() < remaining) {
            int index = monsters.size();
            TilePoint point = questMonsterSpawnPoint(quest, spec, index, monsters);
            monsters.add(new QuestMonsterRuntime(quest.id, quest.id + ":" + index, spec, point.x(), point.y(), worldTick + 20 + random.nextInt(60)));
            invalidateQuestObjectiveCache();
        }
        return monsters;
    }

    private String questMonsterKey(Quest quest) {
        if (quest.activeMonsterKey() != null && !quest.activeMonsterKey().isBlank() && GameData.MONSTERS.containsKey(quest.activeMonsterKey())) {
            return quest.activeMonsterKey();
        }
        return GameData.monsterKeyForName(quest.activeTarget());
    }

    private TilePoint questMonsterSpawnPoint(Quest quest, GameData.MonsterSpec spec, int index, List<QuestMonsterRuntime> localMonsters) {
        Random seeded = new Random(quest.id.hashCode() * 1103515245L + index * 2654435761L + spec.key().hashCode());
        TilePoint center = questMonsterSpawnCenter(quest, index);
        int minRadius = quest.activeObjectiveLocationKind() == null || quest.activeObjectiveLocationKind().isBlank() ? 7 : 0;
        int maxRadius = quest.activeObjectiveLocationKind() == null || quest.activeObjectiveLocationKind().isBlank() ? 34 : 12;
        char[] preferred = questMonsterPreferredTiles(spec);
        List<TilePoint> preferredPoints = new ArrayList<>();
        List<TilePoint> fallbackPoints = new ArrayList<>();
        for (int dy = -maxRadius; dy <= maxRadius; dy++) {
            for (int dx = -maxRadius; dx <= maxRadius; dx++) {
                int distance = Math.abs(dx) + Math.abs(dy);
                if (distance < minRadius || distance > maxRadius) {
                    continue;
                }
                int x = center.x() + dx;
                int y = center.y() + dy;
                String locationKind = quest.activeObjectiveLocationKind();
                if (locationKind != null && locationKind.startsWith("place:")
                        && !world.campaignPlaceContains(locationKind.substring(6), x, y)) continue;
                if (!validQuestMonsterTile(x, y, localMonsters)) {
                    continue;
                }
                TilePoint point = new TilePoint(x, y);
                char tile = world.tileAt(WorldMap.OVERWORLD_ID, x, y);
                if (tileMatches(tile, preferred)) {
                    preferredPoints.add(point);
                } else {
                    fallbackPoints.add(point);
                }
            }
        }
        List<TilePoint> pool = preferredPoints.isEmpty() ? fallbackPoints : preferredPoints;
        if (pool.isEmpty()) {
            return center;
        }
        return pool.get(seeded.nextInt(pool.size()));
    }

    private TilePoint questMonsterSpawnCenter(Quest quest, int index) {
        if (quest.activeObjectiveLocationKind() != null && !quest.activeObjectiveLocationKind().isBlank()) {
            return world.objectivePoint(quest.activeObjectiveLocationKind(), quest.activeObjectiveLocationIndex(), index,
                    quest.activeObjectiveAsset() == null || quest.activeObjectiveAsset().isBlank() ? quest.activeMonsterKey() : quest.activeObjectiveAsset());
        }
        Npc giver = questGiver(quest.id);
        WorldMap.SettlementSite settlement = settlementForMap(giver == null ? "" : giver.mapId());
        if (settlement != null) {
            return new TilePoint(settlement.x(), settlement.y());
        }
        return new TilePoint(WorldMap.START_POSITION.x(), WorldMap.START_POSITION.y());
    }

    private WorldMap.SettlementSite settlementForMap(String mapId) {
        if (mapId == null || mapId.isBlank()) {
            return null;
        }
        String sourceMap = "interior".equals(world.kind(mapId)) ? sourceMapForInterior(mapId) : mapId;
        for (WorldMap.SettlementSite settlement : world.settlementSites()) {
            if (settlement.id().equals(sourceMap)) {
                return settlement;
            }
        }
        return null;
    }

    private boolean validQuestMonsterTile(int x, int y, List<QuestMonsterRuntime> localMonsters) {
        char tile = world.tileAt(WorldMap.OVERWORLD_ID, x, y);
        if (!world.isPassable(WorldMap.OVERWORLD_ID, x, y) || Terrain.connectingRoad(tile) || tile == 'c' || tile == 'u' || tile == 'A') {
            return false;
        }
        if (WorldMap.OVERWORLD_ID.equals(currentMapId) && x == playerX && y == playerY) {
            return false;
        }
        for (QuestMonsterRuntime monster : localMonsters) {
            if (monster.x == x && monster.y == y) {
                return false;
            }
        }
        for (List<QuestMonsterRuntime> monsters : questMonsterRuntime.values()) {
            for (QuestMonsterRuntime monster : monsters) {
                if (monster.x == x && monster.y == y) {
                    return false;
                }
            }
        }
        return true;
    }

    private char[] questMonsterPreferredTiles(GameData.MonsterSpec spec) {
        String key = spec.key();
        return switch (spec.species()) {
            case "bandit" -> new char[]{'g', 'f', 'b', 's'};
            case "bat", "undead" -> new char[]{'f', 'v', 'n', 'b'};
            case "beast" -> {
                if (key.contains("frost") || key.contains("snow")) {
                    yield new char[]{'n', 'q'};
                }
                if (key.contains("mountain") || key.contains("stoneback")) {
                    yield new char[]{'q', 'n', 'b'};
                }
                yield new char[]{'g', 'f'};
            }
            case "demon" -> new char[]{'b', 's', 'f'};
            case "dragon", "drake" -> key.contains("marsh") ? new char[]{'v'} : new char[]{'q', 'b', 's'};
            case "elemental" -> key.contains("ice") ? new char[]{'n', 'q'} : new char[]{'s', 'b'};
            case "giant" -> key.contains("fire") ? new char[]{'s', 'b'} : new char[]{'q', 'b', 'n'};
            case "goblin", "orc" -> new char[]{'g', 'f', 'b', 'v'};
            case "insect" -> key.contains("scorpion") ? new char[]{'s', 'b'} : new char[]{'f', 'v'};
            case "plant" -> new char[]{'f', 'g', 'v'};
            case "reptile" -> key.contains("sand") ? new char[]{'s', 'b', 'P'} : new char[]{'v', 'P'};
            case "slime", "troll" -> key.contains("frost") ? new char[]{'n', 'q'} : new char[]{'v', 'f'};
            default -> new char[]{'g', 'f', 'v', 'b', 's', 'n', 'q', 'P'};
        };
    }

    private boolean tileMatches(char tile, char[] preferred) {
        for (char candidate : preferred) {
            if (tile == candidate) {
                return true;
            }
        }
        return false;
    }

    private void ensureActiveQuestNpcRuntime() {
        pruneQuestNpcRuntime();
        for (Quest quest : quests.values()) {
            if (!questNeedsQuestNpc(quest)) {
                continue;
            }
            questNpcsForQuest(quest);
        }
    }

    private void pruneQuestNpcRuntime() {
        if (questNpcRuntime.entrySet().removeIf(entry -> {
            Quest quest = quests.get(entry.getKey());
            return !questNeedsQuestNpc(quest);
        })) {
            invalidateNpcListCache();
        }
    }

    private boolean questNeedsQuestNpc(Quest quest) {
        return quest != null
                && quest.accepted
                && !quest.completed
                && !quest.ready()
                && (questTemporaryNpcObjective(quest) || questRelocatesExistingNpc(quest));
    }

    private boolean questTemporaryNpcObjective(Quest quest) {
        if (quest == null || quest.activeTargetNpcId() != null && !quest.activeTargetNpcId().isBlank()) {
            return false;
        }
        return switch (quest.activeObjectiveKind()) {
            case RESCUE, TALK, DELIVER, ESCORT, CHOICE -> true;
            default -> false;
        };
    }

    private boolean questRelocatesExistingNpc(Quest quest) {
        if (quest == null || quest.activeTargetNpcId() == null || quest.activeTargetNpcId().isBlank()) {
            return false;
        }
        Npc source = questTargetSourceNpc(quest);
        if (source == null || quest.activeObjectiveLocationKind() == null || quest.activeObjectiveLocationKind().isBlank()) {
            return false;
        }
        String objectiveMapId = quest.activeObjectiveMapId() == null || quest.activeObjectiveMapId().isBlank()
                ? WorldMap.OVERWORLD_ID
                : quest.activeObjectiveMapId();
        return !objectiveMapId.equals(source.mapId()) || !quest.activeObjectiveLocationKind().isBlank();
    }

    private List<QuestNpcRuntime> questNpcsForQuest(Quest quest) {
        List<QuestNpcRuntime> npcs = questNpcRuntime.computeIfAbsent(quest.id, ignored -> new ArrayList<>());
        Npc source = questTargetSourceNpc(quest);
        boolean shouldRelocate = source != null && questRelocatesExistingNpc(quest);
        if (!shouldRelocate && !questTemporaryNpcObjective(quest)) {
            npcs.clear();
            invalidateNpcListCache();
            return List.of();
        }
        if (npcs.size() == 1 && (source == null || source.equals(npcs.get(0).sourceNpc))) {
            return npcs;
        }
        npcs.clear();
        TilePoint point = questNpcSpawnPoint(quest, source);
        String mapId = quest.activeObjectiveMapId() == null || quest.activeObjectiveMapId().isBlank()
                ? WorldMap.OVERWORLD_ID
                : quest.activeObjectiveMapId();
        Npc npc = source == null
                ? createTemporaryQuestNpc(quest, mapId, point)
                : relocateQuestNpc(source, quest, mapId, point);
        npcs.add(new QuestNpcRuntime(quest.id, quest.id + ":npc:0", npc, source));
        invalidateNpcListCache();
        return npcs;
    }

    private Npc createTemporaryQuestNpc(Quest quest, String mapId, TilePoint point) {
        String name = quest.activeTarget() == null || quest.activeTarget().isBlank() ? "Quest Witness" : quest.activeTarget();
        String sprite = quest.activeObjectiveAsset() != null && quest.activeObjectiveAsset().startsWith("npc_")
                ? quest.activeObjectiveAsset()
                : quest.activeObjectiveKind() == Quest.ObjectiveKind.RESCUE ? "npc_citizen_man" : "npc_citizen_woman";
        List<String> dialog = List.of(
                name + ": I was beginning to think the road had swallowed every decent person.",
                "Quest: " + quest.title + ".",
                "Lead: " + quest.activeProgressDialog()
        );
        return new Npc(mapId, name, sprite, point.x(), point.y(), dialog, null, null);
    }

    private Npc relocateQuestNpc(Npc source, Quest quest, String mapId, TilePoint point) {
        List<String> dialog = new ArrayList<>(source.dialog());
        dialog.add(0, source.name() + ": You found me. That means the trail was not as dead as I feared.");
        dialog.add("Quest: " + quest.title + ".");
        return new Npc(
                mapId,
                source.name(),
                source.sprite(),
                point.x(),
                point.y(),
                List.copyOf(dialog),
                source.questId(),
                source.shopId(),
                source.recruitId(),
                source.recruitCost(),
                source.professionXp(),
                source.job()
        );
    }

    private TilePoint questNpcSpawnPoint(Quest quest, Npc source) {
        String mapId = quest.activeObjectiveMapId() == null || quest.activeObjectiveMapId().isBlank()
                ? WorldMap.OVERWORLD_ID
                : quest.activeObjectiveMapId();
        TilePoint center = quest.activeObjectiveLocationKind() == null || quest.activeObjectiveLocationKind().isBlank()
                ? questMonsterSpawnCenter(quest, 0)
                : "story".equals(quest.activeObjectiveLocationKind())
                ? world.storyObjectivePoint(mapId, quest.activeObjectiveLocationIndex())
                : world.objectivePoint(quest.activeObjectiveLocationKind(), quest.activeObjectiveLocationIndex(), 0,
                quest.activeObjectiveAsset() == null || quest.activeObjectiveAsset().isBlank() ? source == null ? "" : source.sprite() : quest.activeObjectiveAsset());
        Random seeded = new Random(quest.id.hashCode() * 2862933555777941757L + (source == null ? 0 : source.name().hashCode()));
        for (int radius = 0; radius <= 8; radius++) {
            List<TilePoint> candidates = new ArrayList<>();
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != radius) {
                        continue;
                    }
                    int x = center.x() + dx;
                    int y = center.y() + dy;
                    if (validQuestNpcTile(mapId, x, y)) {
                        candidates.add(new TilePoint(x, y));
                    }
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(seeded.nextInt(candidates.size()));
            }
        }
        return center;
    }

    private boolean validQuestNpcTile(String mapId, int x, int y) {
        if (!world.isPassable(mapId, x, y)) {
            return false;
        }
        if (mapId.equals(currentMapId) && x == playerX && y == playerY) {
            return false;
        }
        for (List<QuestMonsterRuntime> monsters : questMonsterRuntime.values()) {
            for (QuestMonsterRuntime monster : monsters) {
                if (WorldMap.OVERWORLD_ID.equals(mapId) && monster.x == x && monster.y == y) {
                    return false;
                }
            }
        }
        return true;
    }

    private Npc questTargetSourceNpc(Quest quest) {
        if (quest == null || quest.activeTargetNpcId() == null || quest.activeTargetNpcId().isBlank()) {
            return null;
        }
        String expected = normalizeQuestMatchText(quest.activeTargetNpcId());
        for (Npc npc : GameData.NPCS) {
            if (expected.equals(normalizeQuestMatchText(npc.name()))
                    || expected.equals(normalizeQuestMatchText(npc.recruitId()))
                    || expected.equals(normalizeQuestMatchText(rawNpcStateKey(npc)))
                    || expected.equals(normalizeQuestMatchText(npc.mapId() + ":" + npc.name()))) {
                return npc;
            }
        }
        return null;
    }

    private boolean questNpc(Npc npc) {
        if (npc == null) {
            return false;
        }
        for (List<QuestNpcRuntime> runtimes : questNpcRuntime.values()) {
            for (QuestNpcRuntime runtime : runtimes) {
                if (runtime.npc.equals(npc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Npc questNpcSourceFor(Npc npc) {
        if (npc == null) {
            return null;
        }
        for (List<QuestNpcRuntime> runtimes : questNpcRuntime.values()) {
            for (QuestNpcRuntime runtime : runtimes) {
                if (runtime.npc.equals(npc)) {
                    return runtime.sourceNpc;
                }
            }
        }
        return null;
    }

    private boolean npcRelocatedByQuest(Npc npc) {
        if (npc == null) {
            return false;
        }
        ensureActiveQuestNpcRuntime();
        for (List<QuestNpcRuntime> runtimes : questNpcRuntime.values()) {
            for (QuestNpcRuntime runtime : runtimes) {
                if (npc.equals(runtime.sourceNpc)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<QuestInteractible> activeQuestInteractibles(String mapId) {
        List<QuestInteractible> interactibles = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (!quest.hasWorldObjective() || !quest.activeObjectiveKind().gatherObjective() || quest.activeObjectiveAsset() == null || quest.activeObjectiveAsset().isBlank()) {
                continue;
            }
            if (!quest.activeObjectiveMapId().equals(mapId)) {
                continue;
            }
            for (WorldProp prop : world.props(mapId)) {
                if (!quest.activeObjectiveAsset().equals(prop.asset())) {
                    continue;
                }
                String locationKind = world.locationKindAt(mapId, prop.x(), prop.y());
                if (quest.activeObjectiveLocationKind() != null && !quest.activeObjectiveLocationKind().equals(locationKind)) {
                    continue;
                }
                if (harvestedQuestResources.contains(resourceKey(questStageResourceId(quest), mapId, prop.x(), prop.y(), prop.asset()))) {
                    continue;
                }
                interactibles.add(new QuestInteractible(
                        quest.id,
                        quest.title,
                        quest.activeTarget(),
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

    public void battleHeavyAttack() {
        if (battle == null) {
            return;
        }
        battle.playerHeavyAttack();
        updateAfterBattleAction();
    }

    public void battleCleaveAttack() {
        if (battle == null) {
            return;
        }
        battle.playerCleaveAttack();
        updateAfterBattleAction();
    }

    public void battleDodge() {
        if (battle == null) {
            return;
        }
        battle.playerDodge();
        updateAfterBattleAction();
    }

    public void battleRun() {
        if (battle == null) {
            return;
        }
        battle.playerRun();
        updateAfterBattleAction();
    }

    public void battleAbility(int index) {
        if (battle == null) {
            return;
        }
        battle.useAbility(index);
        updateAfterBattleAction();
    }

    public void togglePartyAbilityLoadout(String abilityName) {
        Actor actor = partyScreenActor();
        if (actor == null || abilityName == null || abilityName.isBlank()) {
            return;
        }
        boolean wasActive = actor.isAbilityActive(abilityName);
        boolean changed = actor.toggleAbilityLoadout(abilityName);
        if (!changed) {
            status = actor.hasAbility(abilityName)
                    ? actor.name + "'s loadout is full."
                    : actor.name + " does not know " + abilityName + ".";
            return;
        }
        status = wasActive
                ? actor.name + " set aside " + abilityName + "."
                : actor.name + " prepared " + abilityName + ".";
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

    public void recordMonsterInsight(String monsterKey) {
        if (monsterKey == null || monsterKey.isBlank() || !GameData.MONSTERS.containsKey(monsterKey)) {
            return;
        }
        monsterInsights.merge(monsterKey, 1, Integer::sum);
    }

    public void recordMonsterVulnerability(String monsterKey, String damageType) {
        String type = GameData.normalizeDamageType(damageType);
        if (monsterKey == null || monsterKey.isBlank() || !GameData.MONSTERS.containsKey(monsterKey)
                || !GameData.DAMAGE_TYPES.contains(type)) {
            return;
        }
        monsterKnownVulnerabilities.computeIfAbsent(monsterKey, key -> new HashSet<>()).add(type);
    }

    public Set<String> knownMonsterVulnerabilities(String monsterKey) {
        Set<String> known = monsterKnownVulnerabilities.get(monsterKey);
        return known == null ? Set.of() : Set.copyOf(known);
    }

    public int monsterInsightCount(String monsterKey) {
        return Math.max(0, monsterInsights.getOrDefault(monsterKey, 0));
    }

    public int monsterInsightLevel(String monsterKey) {
        return GameData.monsterInsightLevel(monsterInsightCount(monsterKey), player.intelligence);
    }

    public int monsterInsightPercent(String monsterKey) {
        return GameData.monsterInsightPercent(monsterInsightCount(monsterKey), player.intelligence);
    }

    public void revive() {
        player.healFull();
        for (Actor ally : allies) {
            ally.healFull();
        }
        currentMapId = WorldMap.PLAYER_VILLAGE_ID;
        TilePoint respawn = world.playerVillageRespawnPoint();
        playerX = respawn.x();
        playerY = respawn.y();
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
        boolean villageChanged = refreshPlayerVillageGrowth();
        ensureSafeBattleReturn();
        if (!villageChanged) {
            status = world.describe(currentMapId, playerX, playerY);
        }
    }

    private void leaveEscapedBattle() {
        battle = null;
        activeDungeonMonster = null;
        activeQuestMonster = null;
        demonQueenBattleActive = false;
        mode = GameMode.EXPLORE;
        ensureSafeBattleReturn();
        invalidateQuestObjectiveCache();
        status = "Escaped from battle. " + world.describe(currentMapId, playerX, playerY);
    }

    private void ensureSafeBattleReturn() {
        TilePoint safe = world.safePassablePoint(currentMapId, playerX, playerY);
        playerX = safe.x();
        playerY = safe.y();
    }

    public void applyTransition(WorldTransition transition) {
        String previousMapId = currentMapId;
        currentMapId = transition.targetMapId();
        playerX = transition.targetX();
        playerY = transition.targetY();
        if (!previousMapId.equals(currentMapId)) {
            recordSettlementVisit(currentMapId);
        }
        regenerateResourceNodesOnTownEntry();
        status = transition.message();
    }

    public int settlementVisitCount(String mapId) {
        return settlementVisits.getOrDefault(mapId, 0);
    }

    public int settlementKnowledgeLevel(String mapId) {
        int visits = settlementVisitCount(mapId);
        int intelligence = Math.max(1, player.intelligence);
        if (visits <= 0) {
            return intelligence >= 10 ? 1 : 0;
        }
        return Math.min(5, 1 + visits / 2 + intelligence / 5);
    }

    public void recordSettlementVisit(String mapId) {
        if (mapId == null || !isSettlementMap(mapId)) {
            return;
        }
        settlementVisits.merge(mapId, 1, Integer::sum);
    }

    public String buildingKnowledgeKey(String mapId, CityBuilding building) {
        if (building == null) {
            return "";
        }
        String key = building.key() == null || building.key().isBlank()
                ? building.x1() + "_" + building.y1()
                : building.key();
        return mapId + ":" + key;
    }

    public int buildingKnowledgeCount(String mapId, CityBuilding building) {
        return buildingKnowledge.getOrDefault(buildingKnowledgeKey(mapId, building), 0);
    }

    public int buildingKnowledgeLevel(String mapId, CityBuilding building) {
        int count = buildingKnowledgeCount(mapId, building);
        int intelligence = Math.max(1, player.intelligence);
        if (count <= 0) {
            return intelligence >= 12 ? 1 : 0;
        }
        return Math.min(5, 1 + count / 2 + intelligence / 6);
    }

    public String generatedBuildingName(CityBuilding building) {
        String western = WesternReachFolklore.buildingName(currentMapId, building);
        if (!western.isEmpty()) return western;
        String local = HearthlandsFolklore.buildingName(currentMapId, building);
        if (!local.isEmpty()) return local;
        String regional = RegionalBuildingTypes.name(currentMapId, building);
        if (!regional.isEmpty()) return regional;
        if (building == null) {
            return "Building";
        }
        int seed = buildingNameSeed(building);
        String owner = BUILDING_OWNER_NAMES[Math.floorMod(seed, BUILDING_OWNER_NAMES.length)];
        String place = BUILDING_PLACE_ROOTS[Math.floorMod(seed / 5, BUILDING_PLACE_ROOTS.length)];
        String sign = BUILDING_SIGNS[Math.floorMod(seed / 11, BUILDING_SIGNS.length)];
        return switch (building.style()) {
            case "arena" -> switch (Math.floorMod(seed, 3)) {
                case 0 -> place + " Arena";
                case 1 -> "The " + sign + " Ring";
                default -> owner + "'s Trial Yard";
            };
            case "mage_tower" -> switch (Math.floorMod(seed, 3)) {
                case 0 -> place + " Spire";
                case 1 -> owner + "'s Star Tower";
                default -> "The " + sign + " Observatory";
            };
            case "bell_tower" -> switch (Math.floorMod(seed, 3)) {
                case 0 -> place + " Bell Tower";
                case 1 -> "The " + sign + " Belfry";
                default -> owner + "'s Watchbell";
            };
            case "sun_shrine", "shrine" -> switch (Math.floorMod(seed, 3)) {
                case 0 -> place + " Shrine";
                case 1 -> "The " + sign + " Chapel";
                default -> owner + "'s Votive House";
            };
            case "river_hall", "hall" -> switch (Math.floorMod(seed, 3)) {
                case 0 -> place + " Manor";
                case 1 -> "The " + sign + " Hall";
                default -> owner + "'s Meeting House";
            };
            case "barracks", "watchtower" -> switch (Math.floorMod(seed, 3)) {
                case 0 -> place + " Guardhouse";
                case 1 -> "The " + sign + " Watch";
                default -> owner + "'s Lookout";
            };
            case "warehouse", "granary" -> switch (Math.floorMod(seed, 3)) {
                case 0 -> place + " Storehouse";
                case 1 -> owner + "'s Warehouse";
                default -> "The " + sign + " Granary";
            };
            case "guild", "study" -> switch (Math.floorMod(seed, 3)) {
                case 0 -> place + " Study";
                case 1 -> "The " + sign + " Guildhall";
                default -> owner + "'s Archive";
            };
            case "inn" -> switch (Math.floorMod(seed, 4)) {
                case 0 -> "Traveller's Inn";
                case 1 -> "The " + sign + " Inn";
                case 2 -> place + " Rest";
                default -> owner + "'s Hearth";
            };
            case "blacksmith" -> switch (Math.floorMod(seed, 4)) {
                case 0 -> owner + "'s Forge";
                case 1 -> "The " + sign + " Smithy";
                case 2 -> place + " Ironworks";
                default -> owner + "'s Tool Yard";
            };
            case "bakery" -> switch (Math.floorMod(seed, 4)) {
                case 0 -> owner + "'s Bakery";
                case 1 -> "The " + sign + " Oven";
                case 2 -> place + " Bakehouse";
                default -> owner + "'s Bread Shop";
            };
            case "restaurant" -> switch (Math.floorMod(seed, 4)) {
                case 0 -> owner + "'s Kitchen";
                case 1 -> "The " + sign + " Table";
                case 2 -> place + " Eating House";
                default -> owner + "'s Cookshop";
            };
            case "apothecary" -> switch (Math.floorMod(seed, 4)) {
                case 0 -> owner + "'s Apothecary";
                case 1 -> "The " + sign + " Herbary";
                case 2 -> place + " Remedies";
                default -> owner + "'s Tonic House";
            };
            case "alchemist" -> switch (Math.floorMod(seed, 4)) {
                case 0 -> owner + "'s Alchemy House";
                case 1 -> "The " + sign + " Laboratory";
                case 2 -> place + " Essences";
                default -> owner + "'s Glassworks";
            };
            case "forestry_hut" -> switch (Math.floorMod(seed, 4)) {
                case 0 -> owner + "'s Timber Hut";
                case 1 -> "The " + sign + " Woodlot";
                case 2 -> place + " Foresters";
                default -> owner + "'s Saw Yard";
            };
            case "workshop" -> switch (Math.floorMod(seed, 4)) {
                case 0 -> owner + "'s Carpenter Shop";
                case 1 -> "The " + sign + " Workshop";
                case 2 -> place + " Joinery";
                default -> owner + "'s Repair Yard";
            };
            case "carpenter" -> switch (Math.floorMod(seed, 4)) {
                case 0 -> owner + "'s Carpenter Shop";
                case 1 -> "The " + sign + " Joinery";
                case 2 -> place + " Woodworks";
                default -> owner + "'s Saw Yard";
            };
            case "fishing_hut" -> switch (Math.floorMod(seed, 4)) {
                case 0 -> owner + "'s Fish Hut";
                case 1 -> "The " + sign + " Net House";
                case 2 -> place + " Fishery";
                default -> owner + "'s Dock Store";
            };
            case "shop", "mine" -> switch (Math.floorMod(seed, 4)) {
                case 0 -> owner + "'s Workshop";
                case 1 -> "The " + sign + " Shop";
                case 2 -> place + " Works";
                default -> owner + "'s Tradehouse";
            };
            case "row" -> switch (Math.floorMod(seed, 4)) {
                case 0 -> place + " Longhouse";
                case 1 -> owner + "'s Homestead";
                case 2 -> "The " + sign + " Row";
                default -> place + " Hearth";
            };
            case "house", "farmstead", "hunting_camp" -> switch (Math.floorMod(seed, 4)) {
                case 0 -> owner + "'s Homestead";
                case 1 -> place + " Cottage";
                case 2 -> "The " + sign + " House";
                default -> owner + "'s Yard";
            };
            case "garden" -> switch (Math.floorMod(seed, 3)) {
                case 0 -> owner + "'s Garden";
                case 1 -> place + " Green";
                default -> "The " + sign + " Plot";
            };
            default -> switch (Math.floorMod(seed, 3)) {
                case 0 -> place + " House";
                case 1 -> "The " + sign;
                default -> owner + "'s Place";
            };
        };
    }

    public String buildingTypeLabel(CityBuilding building) {
        if (building == null) {
            return "Building";
        }
        return switch (building.style()) {
            case "arena" -> "Arena";
            case "mage_tower" -> "Mage Tower";
            case "bell_tower" -> "Bell Tower";
            case "sun_shrine" -> "Sun Shrine";
            case "river_hall" -> "River Hall";
            case "hall" -> "Hall";
            case "barracks" -> "Barracks";
            case "warehouse" -> "Warehouse";
            case "guild" -> "Guildhall";
            case "inn" -> "Inn";
            case "shop" -> "Workshop";
            case "row" -> "Longhouse";
            case "house" -> "Homestead";
            case "granary" -> "Granary";
            case "watchtower" -> "Watchtower";
            case "shrine" -> "Shrine";
            case "garden" -> "Garden";
            case "blacksmith" -> "Blacksmith";
            case "bakery" -> "Bakery";
            case "apothecary" -> "Apothecary";
            case "restaurant" -> "Restaurant";
            case "alchemist" -> "Alchemist";
            case "workshop" -> "Workshop";
            case "carpenter" -> "Carpenter";
            case "fishing_hut" -> "Fishing Hut";
            case "forestry_hut" -> "Forestry Hut";
            case "mine" -> "Mine";
            case "farmstead" -> "Farmstead";
            case "hunting_camp" -> "Hunting Camp";
            default -> VillageManager.isManagedBuildingStyle(building.style())
                    ? VillageManager.buildingLabel(building.style())
                    : titleCase(building.style().replace("_", " "));
        };
    }

    private int buildingNameSeed(CityBuilding building) {
        int hash = 17;
        hash = hash * 31 + (building.key() == null ? 0 : building.key().hashCode());
        hash = hash * 31 + (building.style() == null ? 0 : building.style().hashCode());
        hash = hash * 31 + building.x1();
        hash = hash * 31 + building.y1();
        hash = hash * 31 + building.x2();
        hash = hash * 31 + building.y2();
        hash = hash * 31 + building.palette();
        return hash == Integer.MIN_VALUE ? 0 : Math.abs(hash);
    }

    public boolean gainBuildingKnowledge(String mapId, CityBuilding building, int amount, String source) {
        String message = gainBuildingKnowledgeMessage(mapId, building, amount, source);
        if (message.isBlank()) {
            status = "You already know as much as you can about " + generatedBuildingName(building) + ".";
            return false;
        }
        status = message;
        return true;
    }

    public boolean inspectBuilding(String mapId, CityBuilding building) {
        String message = gainBuildingKnowledgeMessage(mapId, building, 1, "Inspection");
        String name = generatedBuildingName(building);
        if (message.isBlank()) {
            status = "You inspect " + name + ", but nothing new stands out.";
            return false;
        }
        status = message;
        return true;
    }

    public boolean enterBuilding(CityBuilding building) {
        if (building == null || !isSettlementMap(currentMapId)) {
            return false;
        }
        for (TilePoint door : world.cityBuildingDoorTiles(building)) {
            if (playerX == door.x() && playerY == door.y() + 1) {
                enterBuildingAt(door.x(), door.y());
                return true;
            }
        }
        status = "Stand at the door to enter " + generatedBuildingName(building) + ".";
        return false;
    }

    private String gainBuildingKnowledgeMessage(String mapId, CityBuilding building, int amount, String source) {
        if (building == null || mapId == null || mapId.isBlank()) {
            return "";
        }
        String key = buildingKnowledgeKey(mapId, building);
        int previous = buildingKnowledge.getOrDefault(key, 0);
        int next = Math.min(6, previous + Math.max(1, amount));
        if (next <= previous) {
            return "";
        }
        buildingKnowledge.put(key, next);
        int xp = 1;
        List<String> notes = player.gainXp(xp);
        String message = source + " reveals more about " + generatedBuildingName(building) + ". +" + xp + " XP.";
        if (!notes.isEmpty()) {
            message += " " + String.join(" ", notes);
        }
        return message;
    }

    private CityBuilding activeNpcPlaceInquiryBuilding() {
        if (activeNpc == null) {
            return null;
        }
        String mapId = activeNpc.mapId();
        if ("interior".equals(world.kind(mapId))) {
            return buildingForInteriorMap(mapId);
        }
        if (!isSettlementMap(mapId)) {
            return null;
        }
        TilePoint position = npcPosition(activeNpc);
        CityBuilding best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (CityBuilding building : world.cityBuildings(mapId)) {
            int distance = distanceToBuilding(position, building);
            if (distance < bestDistance) {
                best = building;
                bestDistance = distance;
            }
        }
        return bestDistance <= 7 ? best : null;
    }

    private CityBuilding activeNpcAssignedShopBuilding() {
        if (activeNpc == null || activePartyTalkActor != null) {
            return null;
        }
        String buildingKey = buildingAssignmentForAlly(activeNpc.name());
        if (buildingKey.isBlank()) {
            return null;
        }
        CityBuilding building = playerVillageBuildingByKey(buildingKey);
        if (building == null || !activeNpc.name().equals(assignedAllyForBuilding(building.key()))) {
            return null;
        }
        return building;
    }

    private CityBuilding activeShopDialogueBuilding() {
        if (activeShopDialogueBuildingKey.isBlank() || activeNpc == null) {
            return null;
        }
        CityBuilding building = playerVillageBuildingByKey(activeShopDialogueBuildingKey);
        if (building == null || !activeNpc.name().equals(assignedAllyForBuilding(building.key()))) {
            return null;
        }
        return building;
    }

    private void askActiveNpcAboutPlace(CityBuilding building) {
        if (activeNpc == null || building == null) {
            return;
        }
        activeShopDialogueBuildingKey = "";
        String mapId = "interior".equals(world.kind(activeNpc.mapId()))
                ? sourceMapForInterior(activeNpc.mapId())
                : activeNpc.mapId();
        String message = gainBuildingKnowledgeMessage(mapId, building, 1, activeNpc.name());
        if (message.isBlank()) {
            status = activeNpc.name() + " has no new stories about " + generatedBuildingName(building) + ".";
        } else {
            status = message;
            adjustNpcRelationship(activeNpc, 1);
            recordNpcKnowledge(activeNpc, 1);
        }
        dialogIndex = 0;
    }

    private void askActiveNpcAboutAssignedShop(CityBuilding building) {
        if (activeNpc == null || building == null) {
            return;
        }
        activeShopDialogueBuildingKey = building.key();
        activeQuestDialogueLine = activeNpc.name() + ": " + assignedShopDialogue(building);
        status = activeNpc.name() + " talks through the " + VillageManager.buildingLabel(building.style()).toLowerCase() + " shop.";
        dialogIndex = 0;
    }

    private String assignedShopDialogue(CityBuilding building) {
        Shop shop = shopForVillageBuilding(building);
        int stockCount = shop.availableStock(player.level).size();
        String label = VillageManager.buildingLabel(building.style()).toLowerCase();
        String focus = switch (building.style()) {
            case "blacksmith" -> "I stock blades, shields, and plate. More forge tools inside let me offer stronger armor.";
            case "shop" -> "I handle carpentry goods: axes, shields, bows, and repair tools. Workbenches improve the shelf.";
            case "apothecary" -> "I sell herbs, tonics, potions, and field medicine. Alchemy tools help me mix better stock.";
            case "inn" -> "I sell meals, drink, and travel supplies. More beds inside mean more overnight gold for the village.";
            case "bakery" -> "I keep food and camp cookery ready. Ovens, counters, and tidy tables make the trade better.";
            case "warehouse", "granary" -> "I move stored goods and useful supplies. Better shelves and counters make storage work pay.";
            case "forestry_hut" -> "I sell axes, woodcraft, bows, and shields. Carpenter tools inside improve what I can carry.";
            case "mine" -> "I stock picks, mail, and mining gear. Proper tools inside help me bring in stronger equipment.";
            case "hunting_camp" -> "I trade hunting gear, leathers, and scouting supplies. Racks and worktables improve the stock.";
            case "farmstead", "garden" -> "I sell food, herbs, and simple supplies. Plants and tidy counters make people linger.";
            case "fishing_hut" -> "I sell food, lures, and waterside gear. Nets, counters, and worktables improve the trade.";
            case "guild" -> "I keep books, scrolls, lenses, and arcane gear. Shelves and study tools widen the selection.";
            case "shrine" -> "I offer tonics, charms, and restorative supplies. Sacred decor helps the place draw visitors.";
            case "watchtower" -> "I stock guard arms, shields, and patrol tonics. Weapon racks and tables improve the armory.";
            case "house", "row" -> "I run a small household trade. Attractive decor brings more customers through the door.";
            default -> "I keep a practical shelf for travelers. Useful tools and pleasant decor make it better.";
        };
        return focus + " Right now this " + label + " has " + stockCount + " shop lines and " + buildingInteriorSummary(building) + ".";
    }

    private CityBuilding buildingForInteriorMap(String mapId) {
        String sourceMapId = sourceMapForInterior(mapId);
        if (sourceMapId.isBlank()) {
            return null;
        }
        int[] anchor = interiorAnchor(mapId);
        return anchor == null ? null : world.cityBuildingAt(sourceMapId, anchor[0], anchor[1]);
    }

    private String sourceMapForInterior(String mapId) {
        if (mapId == null || !mapId.startsWith("house_")) {
            return "";
        }
        int last = mapId.lastIndexOf('_');
        if (last <= "house_".length()) {
            return "";
        }
        int previous = mapId.lastIndexOf('_', last - 1);
        if (previous <= "house_".length()) {
            return "";
        }
        return mapId.substring("house_".length(), previous);
    }

    private int[] interiorAnchor(String mapId) {
        try {
            int last = mapId.lastIndexOf('_');
            int previous = mapId.lastIndexOf('_', last - 1);
            return new int[]{Integer.parseInt(mapId.substring(previous + 1, last)), Integer.parseInt(mapId.substring(last + 1))};
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private int distanceToBuilding(TilePoint point, CityBuilding building) {
        int dx = point.x() < building.x1() ? building.x1() - point.x() : Math.max(0, point.x() - building.x2());
        int dy = point.y() < building.y1() ? building.y1() - point.y() : Math.max(0, point.y() - building.y2());
        return dx + dy;
    }

    private String titleCase(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                result.append(part.substring(1));
            }
        }
        return result.length() == 0 ? "Building" : result.toString();
    }

    private void regenerateResourceNodesOnTownEntry() {
        if (!isSettlementMap(currentMapId) || harvestedResourceNodes.isEmpty()) {
            return;
        }
        restoreHarvestedResourceNodes();
        harvestedResourceNodes.clear();
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
        CityBuilding building = world.cityBuildingEntryAt(currentMapId, wx, wy, playerX, playerY);
        String knowledgeMessage = building == null ? "" : gainBuildingKnowledgeMessage(currentMapId, building, 2, "Entering");
        String houseMapId = world.ensureHouseInterior(currentMapId, wx, wy, playerX, playerY);
        TilePoint entry = world.interiorEntryPoint(houseMapId);
        currentMapId = houseMapId;
        playerX = entry.x();
        playerY = entry.y();
        status = knowledgeMessage.isBlank() ? "You step inside." : "You step inside. " + knowledgeMessage;
    }

    private QuestObjective questObjectiveNearPlayer() {
        QuestObjective best = null;
        int bestScore = Integer.MAX_VALUE;
        for (QuestObjective objective : activeQuestObjectives()) {
            if (objective.markerOnly()) {
                continue;
            }
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
            int kindPriority = objective.kind().inspectObjective() ? -2 : objective.kind().gatherObjective() ? -1 : 0;
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
        if (quest == null || quest.completed || !quest.activeObjectiveKind().gatherObjective()) {
            return;
        }
        String resourceKey = resourceKey(questStageResourceId(quest), interactible.mapId(), interactible.x(), interactible.y(), interactible.asset());
        if (harvestedQuestResources.contains(resourceKey)) {
            status = "Already gathered from here.";
            return;
        }
        int oldProgress = quest.progress;
        quest.recordGather(interactible.target());
        if (quest.progress > oldProgress) {
            harvestedQuestResources.add(resourceKey);
            invalidateQuestObjectiveCache();
            status = "Gathered " + interactible.target() + ". " + quest.title + ": " + quest.progress + "/" + quest.activeNeeded() + ".";
            if (quest.ready()) {
                if (autoAdvanceReadyQuestStage(quest)) {
                    return;
                }
                status += " " + quest.activeReadyDialog();
            }
        }
    }

    private void interactQuestObjective(QuestObjective objective) {
        Quest quest = quests.get(objective.questId());
        if (quest == null || !quest.accepted || quest.completed || quest.ready()
                || !quest.activeStage().id().equals(objective.stageId())
                || quest.activeObjectiveKind() != objective.kind()) {
            return;
        }
        if (objective.kind().gatherObjective()) {
            recordMarkedObjective(quest, objective, "Gathered");
            return;
        }
        if (objective.kind().inspectObjective()) {
            recordMarkedObjective(quest, objective, objective.kind() == Quest.ObjectiveKind.SEARCH ? "Searched" : objective.kind() == Quest.ObjectiveKind.ESCORT ? "Reached" : "Inspected");
            return;
        }
        if (objective.kind().defenseMinigameObjective()) {
            startDefenseRaid();
            return;
        }
        QuestMonsterRuntime questMonster = questMonsterAtRuntime(objective.x(), objective.y(), null);
        if (questMonster != null) {
            startQuestMonsterBattle(questMonster);
            return;
        }
        String monsterKey = objective.monsterKey() == null || objective.monsterKey().isBlank() ? "goblin" : objective.monsterKey();
        List<GameData.MonsterSpec> specs = storyObjectiveMonsterSpecs(monsterKey);
        battle = createBattle(specs, world.tileAt(currentMapId, objective.x(), objective.y()), world.kind(currentMapId));
        prepareBattleKnowledge(battle);
        mode = GameMode.BATTLE;
        status = objective.target() + " answers the challenge.";
    }

    private List<GameData.MonsterSpec> storyObjectiveMonsterSpecs(String monsterKey) {
        return switch (monsterKey) {
            case "flame_herald" -> monsterSpecsForKeys(List.of("flame_herald", "ember_imp", "ember_imp"));
            case "frost_witch" -> monsterSpecsForKeys(List.of("frost_witch", "wraith", "ice_golem"));
            case "shadow_beast" -> monsterSpecsForKeys(List.of("shadow_beast", "wraith", "goblin_shaman"));
            case "void_knight" -> monsterSpecsForKeys(List.of("void_knight", "bone_knight"));
            case "kharvok_banner_bound" -> monsterSpecsForKeys(List.of("kharvok_banner_bound", "orc_raider", "orc_shieldbearer"));
            case "gate_ash_raider" -> monsterSpecsForKeys(List.of("gate_ash_raider", "orc_raider", "orc_shieldbearer"));
            case "velmora_bell_drowned" -> monsterSpecsForKeys(List.of("velmora_bell_drowned", "wraith", "skeleton"));
            case "rootmaw_stag" -> monsterSpecsForKeys(List.of("rootmaw_stag", "thornling", "bramble_boar"));
            case "masked_trail_hunter" -> monsterSpecsForKeys(List.of("masked_trail_hunter", "bandit_archer", "goblin_trapper"));
            case "briar_snare_beast" -> monsterSpecsForKeys(List.of("briar_snare_beast", "thornling", "bramble_boar"));
            case "nameless_warden" -> monsterSpecsForKeys(List.of("nameless_warden", "bone_knight", "skeleton"));
            case "oathbreaker_echo" -> monsterSpecsForKeys(List.of("oathbreaker_echo", "bone_knight", "wraith"));
            case "hailback_broodmother" -> monsterSpecsForKeys(List.of("hailback_broodmother", "spider", "frost_wolf"));
            case "sareth_cinder_knife" -> monsterSpecsForKeys(List.of("sareth_cinder_knife", "ember_imp", "ember_imp"));
            case "morvane_mercy_taker" -> monsterSpecsForKeys(List.of("morvane_mercy_taker", "void_knight", "ember_imp"));
            default -> monsterSpecsForKeys(List.of(monsterKey == null || monsterKey.isBlank() ? "goblin" : monsterKey));
        };
    }

    private void prepareBattleKnowledge(Battle battle) {
        if (battle == null) {
            return;
        }
        battle.setMonsterInsightCounts(monsterInsights);
        battle.setKnownMonsterVulnerabilities(monsterKnownVulnerabilities);
    }

    private Battle createBattle(List<GameData.MonsterSpec> specs, char tile, String kind) {
        Battle created = new Battle(player, activeAllies(), specs, random, tile, kind, config.monsterLevelScaling);
        created.setAnimationSpeed(config.combatAnimationSpeed);
        created.backdrop = BattleScenery.choose(world.area(currentMapId), playerX, playerY, tile, created.backdrop);
        if ("dungeon".equals(kind)) {
            created.backdrop = BattleScenery.dungeon(world.dungeonContext(currentMapId), created.backdrop);
        }
        applyWorldBattleAdvantage(created);
        configureBattleIntro(created, specs, tile, kind);
        return created;
    }

    private void configureBattleIntro(Battle battle, List<GameData.MonsterSpec> specs, char tile, String kind) {
        if (battle == null) {
            return;
        }
        int seed = battleIntroSeed(specs, tile, kind);
        battle.configureIntro(
                battleIntroStyleLabel(tile, kind),
                battleIntroEncounterLine(specs, tile, kind, seed),
                battleIntroPartyBark(specs, tile, kind, seed)
        );
    }

    private int battleIntroSeed(List<GameData.MonsterSpec> specs, char tile, String kind) {
        int seed = worldTick * 31 + playerX * 131 + playerY * 197 + tile * 17 + (kind == null ? 0 : kind.hashCode());
        if (specs != null) {
            for (GameData.MonsterSpec spec : specs) {
                if (spec != null) {
                    seed = seed * 31 + spec.key().hashCode();
                }
            }
        }
        return seed;
    }

    private String battleIntroStyleLabel(char tile, String kind) {
        if ("dungeon".equals(kind)) {
            return "Vault Awakening";
        }
        return switch (tile) {
            case 'f' -> "Oldwood Ambush";
            case 'v' -> "Fenwater Ambush";
            case 's' -> "Sunsteppe Clash";
            case 'b' -> "Badlands Clash";
            case 'n' -> "Frostfield Clash";
            case 'm', 'q' -> "Pass of Stone";
            case 'r', 'p', 'j', 'l', 'a' -> "Roadside Clash";
            default -> "Meadow Clash";
        };
    }

    private String battleIntroEncounterLine(List<GameData.MonsterSpec> specs, char tile, String kind, int seed) {
        String entrance = battleIntroEnemyEntrance(specs, tile, seed);
        if ("dungeon".equals(kind)) {
            return "Torchlight buckles against old stone as " + entrance + ".";
        }
        WeatherCondition weather = currentWeather();
        String weatherLead = switch (weather) {
            case RAIN -> "Rain needles across the ground as ";
            case STORM -> "Thunder rolls overhead as ";
            case FOG -> "Fog parts at the last second as ";
            case SNOW -> "Snow swirls around your footing as ";
            case BLIZZARD -> "White wind breaks apart and reveals ";
            case DUST -> "Dust sheets over the trail as ";
            case HEAT_HAZE -> "Heat shimmer blurs the distance until ";
            case CLOUDY -> "Cloudshadow sweeps low as ";
            case CLEAR -> switch (dayPhaseLabel()) {
                case "Night" -> "Night presses close as ";
                case "Dusk" -> "Dusk light catches steel and eyes as ";
                case "Dawn" -> "Dawn has barely opened when ";
                default -> "";
            };
        };
        if (!weatherLead.isBlank()) {
            return weatherLead + entrance + ".";
        }
        return switch (tile) {
            case 'f' -> "Branches shiver once, then " + entrance + ".";
            case 'v' -> "Reeds lean away from the path as " + entrance + ".";
            case 's', 'b' -> "Dust lifts off the hard ground as " + entrance + ".";
            case 'n' -> "Cold breath hangs in the air as " + entrance + ".";
            case 'm', 'q' -> "Loose stone skips downhill as " + entrance + ".";
            case 'r', 'p', 'j', 'l', 'a' -> "The road gives one empty breath before " + entrance + ".";
            default -> "Grass folds under sudden motion as " + entrance + ".";
        };
    }

    private String battleIntroEnemyEntrance(List<GameData.MonsterSpec> specs, char tile, int seed) {
        GameData.MonsterSpec primary = specs == null || specs.isEmpty() ? GameData.MONSTERS.get("slime") : specs.get(0);
        String species = primary == null ? "" : primary.species();
        boolean singular = specs == null || specs.size() <= 1;
        String location = switch (tile) {
            case 'f' -> "breaks from the tree line";
            case 'v' -> "surges out of the reeds";
            case 's', 'b' -> "cuts through the open dust";
            case 'n' -> "emerges through the pale wind";
            case 'm', 'q' -> "comes down the pass";
            case 'r', 'p', 'j', 'l', 'a' -> "steps into the road";
            default -> "closes out of the open ground";
        };
        if (singular) {
            String name = primary == null ? "Something hostile" : primary.name();
            return switch (species) {
                case "bandit", "orc" -> name + " steps into view";
                case "goblin" -> name + " darts into view";
                case "undead" -> name + " lurches into view";
                case "demon" -> name + " tears into view";
                case "elemental" -> name + " forms in the open";
                default -> name + " " + location;
            };
        }
        String group = battleIntroGroupLabel(specs);
        return switch (species) {
            case "bandit", "orc" -> group + " step into the road";
            case "goblin" -> group + " spill out shrieking";
            case "undead" -> group + " lurch into the open";
            case "demon" -> group + " tear through the stillness";
            case "elemental" -> group + " gather shape at once";
            default -> group + " " + location;
        };
    }

    private String battleIntroGroupLabel(List<GameData.MonsterSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return "enemies";
        }
        GameData.MonsterSpec primary = specs.get(0);
        String species = primary == null ? "" : primary.species();
        boolean sameName = specs.stream().filter(spec -> spec != null).map(GameData.MonsterSpec::name).distinct().count() == 1;
        if (sameName && primary != null) {
            return pluralizeMonsterName(primary.name()).toLowerCase(Locale.ROOT);
        }
        return switch (species) {
            case "bandit" -> "bandits";
            case "goblin" -> "goblins";
            case "undead" -> "the dead";
            case "orc" -> "orcs";
            case "demon" -> "demons";
            case "elemental" -> "elementals";
            default -> "a hunting pack";
        };
    }

    private String pluralizeMonsterName(String name) {
        if (name == null || name.isBlank()) {
            return "Enemies";
        }
        String[] parts = name.split("\\s+");
        String last = parts[parts.length - 1];
        String pluralLast = switch (last.toLowerCase(Locale.ROOT)) {
            case "wolf" -> "Wolves";
            case "knife" -> "Knives";
            case "life" -> "Lives";
            case "thief" -> "Thieves";
            default -> last.endsWith("y") && last.length() > 1
                    ? last.substring(0, last.length() - 1) + "ies"
                    : last.endsWith("s") ? last : last + "s";
        };
        parts[parts.length - 1] = pluralLast;
        return String.join(" ", parts);
    }

    private String battleIntroPartyBark(List<GameData.MonsterSpec> specs, char tile, String kind, int seed) {
        List<Actor> party = activeAllies();
        if (!party.isEmpty()) {
            Actor speaker = party.get(Math.floorMod(seed, party.size()));
            String recruitId = recruitIdForAlly(speaker);
            String line = recruitId == null || recruitId.isBlank()
                    ? genericBattleIntroBark(speaker.className, tile, kind)
                    : companionBattleIntroBark(speaker.name, recruitId, tile, kind);
            if (!line.isBlank()) {
                return line;
            }
        }
        return playerBattleIntroBark(tile, kind, specs);
    }

    private String companionBattleIntroBark(String speakerName, String recruitId, char tile, String kind) {
        return switch (recruitId) {
            case "seraphine" -> speakerName + ": They chose a messy opening. Good. I dislike tidy predators.";
            case "maera" -> speakerName + ": Remember the ground. The wrong footing ends more fights than brilliance.";
            case "cassia" -> speakerName + ": Hold the edge. Make them spend the first mistake.";
            case "lyra" -> speakerName + ": Fast hands, clean work, no heroic bleeding.";
            case "samir" -> speakerName + ": The stillness is lying. Break it.";
            case "aria" -> switch (tile) {
                case 'f' -> speakerName + ": Tree line to tree line. Do not give them the flank.";
                case 'm', 'q' -> speakerName + ": Bad footing. Pick the shot, then move.";
                case 'v' -> speakerName + ": Reeds hide bad angles. Keep them where we can count them.";
                default -> speakerName + ": Eyes up. They picked the ground; now we take it back.";
            };
            case "vesper" -> switch (tile) {
                case 'f' -> speakerName + ": The wood was warning us.";
                case 'v' -> speakerName + ": The fen is restless. End this before it swallows the noise.";
                case 'n' -> speakerName + ": Cold makes panic brittle. Use that.";
                default -> speakerName + ": The land felt them before we did.";
            };
            case "rafiq" -> speakerName + ": Dramatic entrance. Generous of them to announce where to lose.";
            case "calder" -> speakerName + ": Plant your feet. Let them waste the first rush.";
            default -> "";
        };
    }

    private String genericBattleIntroBark(String className, char tile, String kind) {
        if (className == null) {
            return "";
        }
        return switch (className) {
            case "Mage" -> "Mage: Keep them off my casting line.";
            case "Knight", "Ironwall", "Sunwarden" -> "Knight: Forward guard. We meet them here.";
            case "Ranger", "Scout" -> "Ranger: Clear lanes. I want the first mover.";
            case "Cleric", "Battle Medic", "Grovekeeper" -> "Cleric: Stay close. I can keep this from becoming worse.";
            case "Rogue", "Nightblade", "Veilrunner" -> "Rogue: Good. Close enough to punish.";
            default -> "";
        };
    }

    private String playerBattleIntroBark(char tile, String kind, List<GameData.MonsterSpec> specs) {
        String line = genericBattleIntroBark(player.className, tile, kind);
        return line.isBlank() ? player.className + ": We finish this fast." : line;
    }

    public List<WorldAbilityOption> worldAbilityOptions() {
        List<WorldAbilityOption> options = new ArrayList<>();
        for (Actor actor : partyMembers()) {
            for (Ability ability : actor.activeAbilities()) {
                String effect = worldEffectForAbility(ability);
                if (!effect.isBlank()) {
                    options.add(new WorldAbilityOption(actor, ability, effect,
                            worldAbilityLabel(effect), worldAbilityDescription(effect, actor, ability)));
                }
            }
        }
        return options;
    }

    public void useWorldAbility(int index) {
        if (mode != GameMode.EXPLORE) {
            return;
        }
        List<WorldAbilityOption> options = worldAbilityOptions();
        if (index < 0 || index >= options.size()) {
            return;
        }
        WorldAbilityOption option = options.get(index);
        Actor actor = option.actor();
        Ability ability = option.ability();
        if (actor.mp < ability.cost()) {
            status = actor.name + " lacks MP for " + ability.name() + ".";
            return;
        }
        actor.mp -= ability.cost();
        int duration = 36 + actor.level + actor.abilityScalingBonus(ability);
        if ("battle_advantage".equals(option.effectKey())) {
            duration = Math.max(18, duration / 2);
        }
        worldAbilityTimers.merge(option.effectKey(), duration, Math::max);
        status = actor.name + " used " + ability.name() + ": " + option.label() + ".";
    }

    public boolean worldAbilityActive(String effectKey) {
        return worldAbilityTimers.getOrDefault(effectKey, 0) > 0;
    }

    public int worldAbilityRemaining(String effectKey) {
        return Math.max(0, worldAbilityTimers.getOrDefault(effectKey, 0));
    }

    public String worldAbilityLabel(String effectKey) {
        return switch (effectKey) {
            case "travel_speed" -> "Swift Travel";
            case "gather_focus" -> "Gather Focus";
            case "encounter_ward" -> "Quiet Road";
            case "encounter_lure" -> "Challenge Call";
            case "battle_advantage" -> "Opening Advantage";
            default -> effectKey;
        };
    }

    public String worldAbilityDescription(String effectKey, Actor actor, Ability ability) {
        return switch (effectKey) {
            case "travel_speed" -> "Increases overland movement speed while the timer lasts.";
            case "gather_focus" -> "Adds extra gathered material after completed gathering actions.";
            case "encounter_ward" -> "Greatly reduces random encounter chance while travelling.";
            case "encounter_lure" -> "Raises random encounter chance for players hunting fights.";
            case "battle_advantage" -> "The next battle starts with party Haste and Shield.";
            default -> "World-use ability.";
        } + "\nCaster: " + actor.name + ". Cost: " + ability.cost() + " MP.";
    }

    public double travelSpeedWorldMultiplier() {
        return worldAbilityActive("travel_speed") ? 0.68 : 1.0;
    }

    private String worldEffectForAbility(Ability ability) {
        String name = ability.name().toLowerCase(Locale.ROOT);
        if (name.contains("grace") || name.contains("step") || name.contains("quick") || name.contains("tempo")
                || name.contains("path") || name.contains("pack") || name.contains("route") || name.contains("trail")) {
            return "travel_speed";
        }
        if (name.contains("smoke") || name.contains("veil") || name.contains("shadow") || name.contains("camouflage")
                || name.contains("snare") || name.contains("hidden") || name.contains("exit") || name.contains("prison")) {
            return "encounter_ward";
        }
        if (name.contains("war cry") || name.contains("banner") || name.contains("howl") || name.contains("herdcall")
                || name.contains("storm") || name.contains("flare") || name.contains("sunburst") || name.contains("challenge")
                || name.contains("judgment") || name.contains("verdict")) {
            return "encounter_lure";
        }
        if (ability.kind() == Ability.AbilityKind.HEAL || ability.kind() == Ability.AbilityKind.DEFEND
                || name.contains("hymn") || name.contains("memory") || name.contains("ward")
                || name.contains("blessing") || name.contains("aegis") || name.contains("guard")
                || name.contains("refuge") || name.contains("line anchor") || name.contains("halo")
                || name.contains("mend") || name.contains("salve") || name.contains("remedy")
                || name.contains("circle") || name.contains("shelter")) {
            return "battle_advantage";
        }
        if (name.contains("herb") || name.contains("green") || name.contains("vine") || name.contains("thorn")
                || name.contains("briar") || name.contains("root") || name.contains("petal")
                || name.contains("grove") || name.contains("wood") || name.contains("forage")
                || name.contains("harvest") || name.contains("flower")) {
            return "gather_focus";
        }
        return "";
    }

    private void tickWorldAbilityTimers() {
        for (String key : new ArrayList<>(worldAbilityTimers.keySet())) {
            int remaining = worldAbilityTimers.getOrDefault(key, 0) - 1;
            if (remaining <= 0) {
                worldAbilityTimers.remove(key);
            } else {
                worldAbilityTimers.put(key, remaining);
            }
        }
    }

    private String applyGatherWorldBonus() {
        if (!worldAbilityActive("gather_focus") || activeGatherCandidate == null) {
            return "";
        }
        Map<String, Integer> output = crafting.lastCompletedOutput();
        if (output.isEmpty()) {
            return "";
        }
        String key = output.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
        if (key.isBlank()) {
            return "";
        }
        int amount = Math.max(1, output.getOrDefault(key, 1) / 2);
        player.addItem(key, amount);
        return "Gather Focus: +" + amount + " " + GameData.itemName(key) + ".";
    }

    private void applyWorldBattleAdvantage(Battle battle) {
        if (battle == null || !worldAbilityActive("battle_advantage")) {
            return;
        }
        boolean strong = worldAbilityRemaining("battle_advantage") > 45;
        battle.grantPartyStartingAdvantage(strong);
        worldAbilityTimers.remove("battle_advantage");
    }

    private void updateAfterBattleAction() {
        if (battle != null) {
            absorbBattleVulnerabilityDiscoveries();
        }
        if (battle != null && battle.finished && battle.fled) {
            leaveEscapedBattle();
            return;
        }
        if (battle != null && battle.finished && battle.victory) {
            if (!battle.questRecorded) {
                partyDialogue.victory(this, battle, activeDungeonMonster != null
                        && activeDungeonMonster.boss && "dungeon".equals(world.kind(currentMapId)));
                for (String defeatedName : battle.defeatedMonsterNames()) {
                    recordQuestProgress(defeatedName);
                }
                for (String defeatedKey : battle.defeatedMonsterKeys()) {
                    recordMonsterInsight(defeatedKey);
                }
                battle.questRecorded = true;
            }
            if (!battle.lootGranted) {
                char terrain = world.tileAt(currentMapId, playerX, playerY);
                String loot = CraftingSystem.grantLoot(player, battle.monsterSpecs, random,
                        world.kind(currentMapId), terrain, dungeonLootTier(currentMapId));
                if (!loot.isBlank()) {
                    battle.addLog("Loot: " + loot + ".");
                }
                battle.lootGranted = true;
            }
            if (!battle.companionTrustGranted) {
                grantCompanionCombatTrust(battle);
                battle.companionTrustGranted = true;
            }
            clearDefeatedDungeonMonster();
            clearDefeatedQuestMonster();
            if (demonQueenBattleActive && (battle.defeatedMonsterKeys().contains("vaelthara")
                    || battle.defeatedMonsterKeys().contains("demon_queen"))) {
                Quest gateQuest = quests.get("ms_twelve_stones_gate");
                if (gateQuest != null) {
                    gateQuest.accepted = true;
                    gateQuest.progress = gateQuest.needed;
                    gateQuest.completed = true;
                    invalidateQuestObjectiveCache();
                }
                demonQueenDefeated = true;
                demonQueenBattleActive = false;
                status = "Vaelthara is defeated. Oathstead survives beyond her command. The damaged wards and the people bound to them still need an answer. Press Enter to continue.";
            } else {
                status = "Victory. Press Enter to continue.";
            }
        } else if (battle != null && battle.finished) {
            demonQueenBattleActive = false;
            status = "Defeated. Press R to revive.";
        } else if (battle != null && battle.isAnimating()) {
            String animationStatus = battle.animationStatus();
            status = animationStatus.isBlank() ? "Battle!" : animationStatus;
        } else if (battle != null) {
            Actor actor = battle.activeActor();
            status = actor == null ? "Battle!" : actor.name + "'s turn.";
        }
    }

    private int dungeonLootTier(String mapId) {
        if (mapId == null || !"dungeon".equals(world.kind(mapId))) {
            return 0;
        }
        int highestDigit = 0;
        for (int i = 0; i < mapId.length(); i++) {
            char ch = mapId.charAt(i);
            if (Character.isDigit(ch)) {
                highestDigit = Math.max(highestDigit, Character.digit(ch, 10));
            }
        }
        return Math.max(1, Math.min(4, highestDigit == 0 ? 2 : highestDigit));
    }

    private void absorbBattleVulnerabilityDiscoveries() {
        for (Map.Entry<String, List<String>> entry : battle.discoveredMonsterVulnerabilities().entrySet()) {
            for (String type : entry.getValue()) {
                recordMonsterVulnerability(entry.getKey(), type);
            }
        }
    }

    private void grantCompanionCombatTrust(Battle completedBattle) {
        List<Actor> companions = activeAllies();
        if (companions.isEmpty()) {
            return;
        }
        boolean questBattle = activeQuestMonster != null && questMonsterIsCompanionQuest(activeQuestMonster);
        boolean bossBattle = completedBattle.defeatedMonsterKeys().stream()
                .anyMatch(key -> key.contains("queen") || key.contains("boss") || key.equals("vaelthara"));
        boolean hardFight = player.hp <= Math.max(1, player.maxHp / 3)
                || companions.stream().anyMatch(ally -> ally.hp > 0 && ally.hp <= Math.max(1, ally.maxHp / 3))
                || completedBattle.enemies().size() >= 3;
        int delta = bossBattle || questBattle ? 3 : hardFight ? 2 : 1;
        List<String> names = new ArrayList<>();
        for (Actor ally : companions) {
            Npc npc = npcForAlly(ally);
            if (npc == null) {
                continue;
            }
            adjustNpcRelationshipDirect(npc, delta);
            recordCompanionMemoryForAlly(ally, bossBattle || questBattle ? "major_combat" : hardFight ? "hard_combat" : "combat",
                    combatMemoryText(completedBattle, bossBattle || questBattle, hardFight));
            names.add(ally.name);
        }
        if (!names.isEmpty()) {
            completedBattle.addLog("Trust +" + delta + ": " + String.join(", ", names) + ".");
            List<String> tags = new ArrayList<>(List.of("combat", "courage", "protection"));
            if (bossBattle || questBattle) {
                tags.add("main_story");
                tags.add("duty");
            }
            if (hardFight) {
                tags.add("reckless");
            }
            publishCompanionEvent(
                    "combat:" + worldTick,
                    bossBattle || questBattle ? "Survived a decisive fight" : hardFight ? "Survived a hard fight" : "Won a fight",
                    tags,
                    0,
                    "",
                    "",
                    bossBattle || questBattle || hardFight
            );
        }
    }

    private boolean questMonsterIsCompanionQuest(QuestMonsterRuntime monster) {
        if (monster == null) {
            return false;
        }
        Quest quest = quests.get(monster.questId);
        return quest != null && quest.companionQuest();
    }

    private String combatMemoryText(Battle completedBattle, boolean majorFight, boolean hardFight) {
        String foes = completedBattle.defeatedMonsterNames().isEmpty()
                ? "enemies"
                : String.join(", ", completedBattle.defeatedMonsterNames());
        if (majorFight) {
            return "Survived a decisive battle against " + foes + ".";
        }
        if (hardFight) {
            return "Survived a hard fight against " + foes + ".";
        }
        return "Fought beside the player against " + foes + ".";
    }

    private void adjustNpcRelationshipDirect(Npc npc, int delta) {
        if (npc == null || delta == 0) {
            return;
        }
        String key = npcRelationshipKey(npc);
        int previous = npcRelationships.getOrDefault(key, 0);
        int value = Math.max(MIN_NPC_RELATIONSHIP, Math.min(maxRelationshipForNpc(npc), previous + delta));
        npcRelationships.put(key, value);
        maybeQueueRelationshipMilestonePrompt(npc, previous, value);
    }

    private void maybeQueueRelationshipMilestonePrompt(Npc npc, int previous, int current) {
        if (current <= previous || !specialCompanionNpc(npc)) {
            return;
        }
        String recruitId = companionRecruitIdForContext(npc);
        int threshold = pendingRelationshipMilestone(recruitId, current);
        if (threshold <= 0 || previous >= threshold) {
            return;
        }
        Actor ally = companionActorForContext(npc, recruitId);
        if (ally == null || !activeAllies().contains(ally)) {
            return;
        }
        String label = relationshipMilestoneLabel(threshold);
        enqueueCompanionComment(new PendingCompanionComment(
                ally.name,
                ally.name + ": When we have a quiet moment, there is something I should say about this " + label + " between us.",
                List.of("I want to hear it from you.", "Say it when you are ready.", "We can keep it for a quieter road."),
                List.of(
                        "Warmly acknowledges the bond. Improves trust.",
                        "Gives the companion patience and agency.",
                        "Defers the scene without losing it."
                ),
                List.of(2, 1, 0),
                List.of("relationship", "milestone", "trust:" + threshold, recruitId),
                banterResponseLines(ally, List.of("relationship", "milestone", "trust:" + threshold, recruitId), 2)
        ));
    }

    private void publishCompanionEvent(
            String eventId,
            String detail,
            List<String> tags,
            int baseApproval,
            String memoryCategory,
            String memoryText,
            boolean important
    ) {
        partyDialogue.questEvent(this, eventId);
        if (activeAllies().isEmpty()) {
            return;
        }
        List<String> eventTags = tags == null ? List.of() : tags;
        if (eventId == null || !eventId.startsWith("companion_request:")) {
            fulfillSoftCompanionRequestsForEvent(eventTags, detail);
        }
        if (!important && random.nextDouble() > 0.35) {
            return;
        }
        List<CompanionEventReaction> reactions = new ArrayList<>();
        for (Actor ally : activeAllies()) {
            String recruitId = recruitIdForAlly(ally);
            if (recruitId.isBlank()) {
                continue;
            }
            int delta = companionApprovalDelta(recruitId, eventTags, baseApproval);
            Npc npc = npcForAlly(ally);
            if (delta != 0) {
                adjustNpcRelationshipDirect(npc, delta);
            }
            if (important && memoryText != null && !memoryText.isBlank()) {
                recordCompanionMemory(recruitId, memoryCategory, memoryText);
            }
            reactions.add(new CompanionEventReaction(ally, recruitId, delta));
        }
        if (reactions.isEmpty()) {
            return;
        }
        CompanionEventReaction speaker = reactions.stream()
                .max((left, right) -> Integer.compare(Math.abs(left.approvalDelta()), Math.abs(right.approvalDelta())))
                .orElse(reactions.get(0));
        String line = companionEventCommentLine(speaker.ally(), speaker.recruitId(), detail, eventTags, speaker.approvalDelta());
        List<String> promptTags = tagged(eventTags, "event", speaker.recruitId());
        enqueueCompanionComment(new PendingCompanionComment(
                speaker.ally().name,
                line,
                banterReplyOptions(speaker.ally(), promptTags, speaker.approvalDelta()),
                banterReplyTooltips(promptTags, speaker.approvalDelta()),
                banterReplyDeltas(promptTags, speaker.approvalDelta()),
                promptTags,
                banterResponseLines(speaker.ally(), promptTags, speaker.approvalDelta())
        ));
    }

    private int companionApprovalDelta(String recruitId, List<String> tags, int baseApproval) {
        CompanionApprovalProfile profile = companionApprovalProfile(recruitId);
        int score = baseApproval;
        for (String tag : tags) {
            if (profile.likes().contains(tag)) {
                score++;
            }
            if (profile.dislikes().contains(tag)) {
                score--;
            }
        }
        return Math.max(-2, Math.min(3, score));
    }

    private CompanionApprovalProfile companionApprovalProfile(String recruitId) {
        return switch (recruitId == null ? "" : recruitId) {
            case "seraphine" -> new CompanionApprovalProfile(
                    List.of("freedom", "clever", "evidence", "mercy", "oathstead"),
                    List.of("greed", "cruelty", "coercion", "waste"),
                    "choice");
            case "maera" -> new CompanionApprovalProfile(
                    List.of("evidence", "knowledge", "clever", "main_story", "truth"),
                    List.of("reckless", "ignorance", "secrecy"),
                    "truth");
            case "cassia" -> new CompanionApprovalProfile(
                    List.of("duty", "protection", "courage", "combat", "oathstead"),
                    List.of("reckless", "cruelty", "cowardice"),
                    "duty");
            case "lyra" -> new CompanionApprovalProfile(
                    List.of("mercy", "healing", "protection", "gather", "craft"),
                    List.of("cruelty", "reckless", "waste"),
                    "care");
            case "samir" -> new CompanionApprovalProfile(
                    List.of("mercy", "truth", "holy", "main_story", "oathstead"),
                    List.of("cruelty", "greed", "cynicism"),
                    "light");
            case "aria" -> new CompanionApprovalProfile(
                    List.of("caution", "scouting", "clever", "survival", "gather"),
                    List.of("reckless", "noise", "coercion"),
                    "watchfulness");
            case "vesper" -> new CompanionApprovalProfile(
                    List.of("nature", "mercy", "gather", "oathstead", "patience"),
                    List.of("waste", "cruelty", "greed"),
                    "living things");
            case "rafiq" -> new CompanionApprovalProfile(
                    List.of("courage", "clever", "mercy", "style", "combat"),
                    List.of("cowardice", "cruelty", "greed"),
                    "second chances");
            case "calder" -> new CompanionApprovalProfile(
                    List.of("craft", "oathstead", "duty", "practical", "protection"),
                    List.of("reckless", "waste", "glamour"),
                    "what holds");
            default -> new CompanionApprovalProfile(List.of("practical", "protection"), List.of("reckless", "cruelty"), "trust");
        };
    }

    private String companionEventCommentLine(Actor speaker, String recruitId, String detail, List<String> tags, int approvalDelta) {
        String first = speaker.name.split("\\s+")[0];
        String subject = detail == null || detail.isBlank() ? "that" : detail;
        boolean approving = approvalDelta >= 0;
        if (tags.contains("combat")) {
            if (approvalDelta < 0) {
                return companionCombatConcernLine(speaker, recruitId);
            }
            return switch (recruitId) {
                case "seraphine" -> speaker.name + ": You fought like someone refusing a contract written in blood. Effective. Messy. Satisfying.";
                case "maera" -> speaker.name + ": Combat is a poor argument, but that one produced a convincing conclusion.";
                case "cassia" -> speaker.name + ": You held the line. More importantly, you knew why it mattered.";
                case "lyra" -> speaker.name + ": Victory is acceptable. Breathing afterward is better. Let me count everyone twice.";
                case "samir" -> speaker.name + ": You did not let fear choose your hands. That is a kind of light.";
                case "aria" -> speaker.name + ": Clean enough. I saw two openings you missed and one you made on purpose. Progress.";
                case "vesper" -> speaker.name + ": The fight is over. Now we see what still lives around it.";
                case "rafiq" -> speaker.name + ": We survived with style adjacent to competence. I am calling it growth.";
                case "calder" -> speaker.name + ": You held under impact. Good. Now we check what cracked.";
                default -> speaker.name + ": You held when the fight tried to make you smaller. I noticed.";
            };
        }
        if (tags.contains("oathstead") || tags.contains("build")) {
            return switch (recruitId) {
                case "seraphine" -> speaker.name + ": Oathstead keeps becoming more like a choice people can survive. Dangerous precedent.";
                case "maera" -> speaker.name + ": A settlement is a record with roofs. This one is starting to cite its sources.";
                case "cassia" -> speaker.name + ": Better walls, better work, fewer frightened people. That is a plan I can stand behind.";
                case "lyra" -> speaker.name + ": Places heal too. Slowly, with timber, food, and people who come back.";
                case "samir" -> speaker.name + ": A hearth built honestly gives better light than any sermon.";
                case "aria" -> speaker.name + ": More corners to watch. More doors worth keeping open.";
                case "vesper" -> speaker.name + ": The ground is accepting us by inches. Do not rush it.";
                case "rafiq" -> speaker.name + ": Mud, labor, hope. Oathstead remains terribly unfashionable and annoyingly persuasive.";
                case "calder" -> speaker.name + ": Good. " + subject + " is the kind of work that keeps promises from falling over.";
                default -> speaker.name + ": Oathstead will remember " + subject.toLowerCase() + ".";
            };
        }
        if (tags.contains("gather") || tags.contains("craft")) {
            if (approvalDelta < 0) {
                return speaker.name + ": Useful, maybe. Wasteful, if we stop noticing what it cost.";
            }
            return switch (recruitId) {
                case "calder" -> speaker.name + ": Useful hands. Useful materials. That is how hope becomes something with nails in it.";
                case "vesper" -> speaker.name + ": Take what is needed, leave enough for tomorrow. That is the whole lesson, somehow.";
                case "lyra" -> speaker.name + ": Supplies first. Bravery works better when someone remembered bandages.";
                case "aria" -> speaker.name + ": Good. A road party that gathers well goes hungry less often.";
                default -> speaker.name + ": Practical work suits us better than speeches, " + first + " included.";
            };
        }
        if (tags.contains("main_story")) {
            return speaker.name + ": That choice moved more than our feet. Alderfall will feel it, whether it knows your name or not.";
        }
        if (tags.contains("commitment") || tags.contains("loyalty")) {
            return approving
                    ? speaker.name + ": Promises are heavier when spoken clearly. I respect that."
                    : speaker.name + ": Promises made too quickly become cages. Walk carefully.";
        }
        return approving
                ? speaker.name + ": " + subject + " tells me something about the kind of road you are choosing."
                : speaker.name + ": " + subject + " sits poorly with me. I am saying it now, before silence makes it worse.";
    }

    private String companionCombatConcernLine(Actor speaker, String recruitId) {
        return switch (recruitId == null ? "" : recruitId) {
            case "seraphine" -> speaker.name + ": We lived, but do not mistake survival for consent. Next time we choose the terms sooner.";
            case "maera" -> speaker.name + ": The conclusion stands. The method deserves several angry footnotes.";
            case "cassia" -> speaker.name + ": Victory with too many exposed backs is not courage. It is a lesson we paid for in breath.";
            case "lyra" -> speaker.name + ": I can mend wounds. I cannot mend the habit of collecting them for no reason.";
            case "samir" -> speaker.name + ": Fear did not win, but haste nearly did. Let courage keep its eyes open.";
            case "aria" -> speaker.name + ": We won after ignoring three exits and two warnings. I prefer victories with fewer invitations to disaster.";
            case "vesper" -> speaker.name + ": The fight ended. The damage is still speaking. Listen before we call this clean.";
            case "rafiq" -> speaker.name + ": Dramatic, yes. Sensible, barely. I object to becoming a cautionary ballad.";
            case "calder" -> speaker.name + ": A structure can stand and still be cracked. That fight was the same. We inspect before boasting.";
            default -> speaker.name + ": We lived. I would like the next victory to require less luck and fewer heroic assumptions.";
        };
    }

    private void enqueueCompanionComment(PendingCompanionComment comment) {
        if (comment == null || comment.line().isBlank()) {
            return;
        }
        if (!comment.tags().contains("milestone") && recentlyUsedBanter(comment.speaker(), comment.line())) {
            return;
        }
        markBanterUsed(comment.speaker(), comment.line());
        if (activeTravelBanter == null && mode == GameMode.EXPLORE) {
            activeTravelBanter = comment.toPrompt(worldTick + TRAVEL_COMMENT_DURATION_TICKS);
            nextTravelBanterTick = worldTick + 1300;
            return;
        }
        pendingCompanionComments.add(comment);
        while (pendingCompanionComments.size() > 4) {
            pendingCompanionComments.remove(0);
        }
    }

    private void showPendingCompanionComment() {
        if (pendingCompanionComments.isEmpty()) {
            return;
        }
        PendingCompanionComment comment = pendingCompanionComments.remove(0);
        activeTravelBanter = comment.toPrompt(worldTick + TRAVEL_COMMENT_DURATION_TICKS);
        nextTravelBanterTick = worldTick + 1300;
    }

    private List<String> postCombatTags(Actor speaker, boolean majorFight, boolean hardFight) {
        List<String> tags = new ArrayList<>(List.of("combat", recruitIdForAlly(speaker)));
        if (majorFight) {
            tags.add("major");
            tags.add("courage");
            tags.add("duty");
        }
        if (hardFight) {
            tags.add("hard");
            tags.add("reckless");
        }
        return tags;
    }

    private List<String> actionBanterTags(String line, Actor speaker) {
        List<String> tags = new ArrayList<>(List.of("action", recruitIdForAlly(speaker)));
        String text = line == null ? "" : line.toLowerCase(Locale.ROOT);
        if (text.contains("oathstead") || text.contains("working at") || text.contains("assigned")) {
            tags.add("oathstead");
            tags.add("build");
        }
        if (text.contains("joined") || text.contains("travel")) {
            tags.add("recruited");
            tags.add("loyalty");
        }
        if (text.contains("request")) {
            tags.add("companion_request");
        }
        return tags;
    }

    private void triggerPostCombatBanter(Actor speaker, boolean majorFight, boolean hardFight) {
        if (speaker == null) {
            return;
        }
        activeTravelBanter = new TravelBanterPrompt(
                speaker.name,
                postCombatBanterLine(speaker, majorFight, hardFight),
                banterReplyOptions(speaker, postCombatTags(speaker, majorFight, hardFight), hardFight ? -1 : 1),
                banterReplyTooltips(postCombatTags(speaker, majorFight, hardFight), hardFight ? -1 : 1),
                banterReplyDeltas(postCombatTags(speaker, majorFight, hardFight), hardFight ? -1 : 1),
                postCombatTags(speaker, majorFight, hardFight),
                banterResponseLines(speaker, postCombatTags(speaker, majorFight, hardFight), hardFight ? -1 : 1),
                worldTick + TRAVEL_COMMENT_DURATION_TICKS
        );
        markBanterUsed(speaker.name, "combat:" + (majorFight ? "major" : hardFight ? "hard" : "normal"));
        nextTravelBanterTick = worldTick + 1300;
    }

    private void triggerActionBanter(Actor speaker, String line) {
        if (speaker == null || line == null || line.isBlank()) {
            return;
        }
        enqueueCompanionComment(new PendingCompanionComment(
                speaker.name,
                line,
                banterReplyOptions(speaker, actionBanterTags(line, speaker), 1),
                banterReplyTooltips(actionBanterTags(line, speaker), 1),
                banterReplyDeltas(actionBanterTags(line, speaker), 1),
                actionBanterTags(line, speaker),
                banterResponseLines(speaker, actionBanterTags(line, speaker), 1)
        ));
    }

    private String postCombatBanterLine(Actor speaker, boolean majorFight, boolean hardFight) {
        String recruitId = recruitIdForAlly(speaker);
        if (!recruitId.isBlank()) {
            if (majorFight) {
                return companionEventCommentLine(
                        speaker,
                        recruitId,
                        "Survived a decisive battle.",
                        List.of("combat", "courage", "major"),
                        1
                );
            }
            if (hardFight) {
                return companionCombatConcernLine(speaker, recruitId);
            }
        }
        if (majorFight) {
            return speaker.name + ": That was not just a fight. That was the road deciding whether we meant what we promised.";
        }
        if (hardFight) {
            return speaker.name + ": Next time we survive by a wider margin. I am fond of breathing.";
        }
        if (speaker.className.contains("Medic") || speaker.className.equals("Battle Medic")) {
            return speaker.name + ": Victory is nicer when everyone keeps their blood on the inside.";
        }
        if (speaker.className.contains("Ranger") || speaker.className.equals("Scout")) {
            return speaker.name + ": Clean enough. Tracks say nothing else is rushing us yet.";
        }
        if (speaker.className.contains("Rogue") || speaker.className.equals("Veilrunner")) {
            return speaker.name + ": I have seen worse ambushes. I have also planned better ones.";
        }
        return speaker.name + ": You held. I noticed.";
    }

    private String resourceKey(String questId, String mapId, int x, int y, String asset) {
        return questId + ":" + mapId + ":" + x + ":" + y + ":" + asset;
    }

    private String questStageResourceId(Quest quest) {
        if (quest == null || !quest.stagedQuest()) {
            return quest == null ? "" : quest.id;
        }
        return quest.id + "@" + quest.activeStage().id();
    }

    private String resourceNodeKey(String mapId, int x, int y, String asset) {
        return resourceKey("resource", mapId, x, y, asset);
    }

    private void depletePendingResourceNode() {
        if (pendingResourceNodeTile == null || pendingResourceNodeMapId.isBlank() || pendingResourceNodeAsset.isBlank()) {
            return;
        }
        String key = resourceNodeKey(
                pendingResourceNodeMapId,
                pendingResourceNodeTile.x(),
                pendingResourceNodeTile.y(),
                pendingResourceNodeAsset
        );
        WorldProp removedProp = null;
        for (WorldProp prop : world.propsAt(pendingResourceNodeMapId, pendingResourceNodeTile.x(), pendingResourceNodeTile.y())) {
            if (pendingResourceNodeAsset.equals(prop.asset())) {
                removedProp = prop;
                removedResourceNodeProps.put(key, prop);
                break;
            }
        }
        if (world.removePropAt(pendingResourceNodeMapId, pendingResourceNodeTile.x(), pendingResourceNodeTile.y(), pendingResourceNodeAsset)) {
            harvestedResourceNodes.add(key);
            lastGatheredPropMapId = pendingResourceNodeMapId;
            lastGatheredProp = removedProp == null
                    ? new WorldProp(pendingResourceNodeTile.x(), pendingResourceNodeTile.y(), pendingResourceNodeAsset, 48)
                    : removedProp;
            lastGatheredPropWorldTick = worldTick;
        }
        pendingResourceNodeMapId = "";
        pendingResourceNodeTile = null;
        pendingResourceNodeAsset = "";
    }

    private boolean completePendingBookshelfReading() {
        if (pendingReadBookshelfTile == null || pendingReadBookshelfMapId.isBlank()) {
            return false;
        }
        String mapId = pendingReadBookshelfMapId;
        TilePoint tile = pendingReadBookshelfTile;
        pendingReadBookshelfMapId = "";
        pendingReadBookshelfTile = null;
        for (WorldProp prop : world.propsAt(mapId, tile.x(), tile.y())) {
            if ("interior_bookshelf".equals(prop.asset())) {
                studyBookshelf(prop, mapId);
                return true;
            }
        }
        status = "You finish reading, but the notes are no longer there.";
        return true;
    }

    public void restoreHarvestedResourceNodes() {
        for (Map.Entry<String, WorldProp> entry : removedResourceNodeProps.entrySet()) {
            String[] fields = entry.getKey().split(":", 5);
            if (fields.length != 5) {
                continue;
            }
            String mapId = fields[1];
            WorldProp removed = entry.getValue();
            boolean alreadyPresent = false;
            for (WorldProp prop : world.propsAt(mapId, removed.x(), removed.y())) {
                if (removed.asset().equals(prop.asset())) {
                    alreadyPresent = true;
                    break;
                }
            }
            if (!alreadyPresent) {
                world.restoreProp(mapId, removed);
            }
        }
        removedResourceNodeProps.clear();
    }

    public void applyHarvestedResourceNodes() {
        removedResourceNodeProps.clear();
        for (String key : harvestedResourceNodes) {
            String[] fields = key.split(":", 5);
            if (fields.length != 5) {
                continue;
            }
            try {
                String mapId = fields[1];
                int x = Integer.parseInt(fields[2]);
                int y = Integer.parseInt(fields[3]);
                String asset = fields[4];
                for (WorldProp prop : world.propsAt(mapId, x, y)) {
                    if (asset.equals(prop.asset())) {
                        removedResourceNodeProps.put(key, prop);
                        break;
                    }
                }
                world.removePropAt(mapId, x, y, asset);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void recordMarkedObjective(Quest quest, QuestObjective objective, String verb) {
        String resourceKey = resourceKey(questStageResourceId(quest), objective.mapId(), objective.x(), objective.y(), objective.asset());
        if (harvestedQuestResources.contains(resourceKey) || resourceAlreadyGatheredForQuest(quest, objective.mapId(), objective.x(), objective.y())) {
            status = "Already handled this objective.";
            return;
        }
        int oldProgress = quest.progress;
        if (objective.kind().gatherObjective()) {
            quest.recordGather(objective.target());
        } else if (objective.kind().inspectObjective()) {
            quest.recordVisit(objective.target());
        }
        if (quest.progress > oldProgress) {
            harvestedQuestResources.add(resourceKey);
            markGatheredResourcesAt(quest, objective.mapId(), objective.x(), objective.y());
            invalidateQuestObjectiveCache();
            status = verb + " " + objective.target() + ". " + quest.title + ": " + quest.progress + "/" + quest.activeNeeded() + ".";
            if (quest.ready()) {
                if (autoAdvanceReadyQuestStage(quest)) {
                    return;
                }
                status += " " + quest.activeReadyDialog();
            }
        }
    }

    private boolean resourceAlreadyGatheredForQuest(Quest quest, String mapId, int x, int y) {
        if (quest.activeObjectiveAsset() == null || quest.activeObjectiveAsset().isBlank()) {
            return false;
        }
        for (WorldProp prop : world.propsAt(mapId, x, y)) {
            if (quest.activeObjectiveAsset().equals(prop.asset())
                    && harvestedQuestResources.contains(resourceKey(questStageResourceId(quest), mapId, x, y, prop.asset()))) {
                return true;
            }
        }
        return false;
    }

    private void markGatheredResourcesAt(Quest quest, String mapId, int x, int y) {
        if (quest.activeObjectiveAsset() == null || quest.activeObjectiveAsset().isBlank()) {
            return;
        }
        for (WorldProp prop : world.propsAt(mapId, x, y)) {
            if (quest.activeObjectiveAsset().equals(prop.asset())) {
                harvestedQuestResources.add(resourceKey(questStageResourceId(quest), mapId, x, y, prop.asset()));
                invalidateQuestObjectiveCache();
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
                if (actor.abilities.size() >= Actor.MAX_ABILITIES) {
                    break;
                }
                actor.abilities.add(upgradedAbility(ability, actor.skillRank(node.id())));
            }
        }
        actor.sanitizeAbilityLoadout();
        actor.sanitizeAbilityLoadoutPresets();
    }

    private Ability upgradedAbility(Ability ability, int rank) {
        int extraRanks = Math.max(0, rank - 1);
        if (extraRanks <= 0) {
            return ability;
        }
        int powerGain = switch (ability.kind()) {
            case DAMAGE -> "all_enemies".equals(ability.target()) ? 6 : 9;
            case HEAL -> "party".equals(ability.target()) ? 5 : 7;
            case DEFEND -> 0;
        };
        int costGain = ability.kind() == Ability.AbilityKind.DEFEND ? 1 : 2;
        return ability.upgraded(extraRanks * powerGain, extraRanks * costGain);
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
                case "strength", "intelligence", "dexterity", "charisma", "constitution", "willpower" ->
                        actor.increaseStat(entry.getKey(), delta);
                default -> {
                }
            }
        }
        actor.hp = Math.max(1, Math.min(actor.maxHp, (int) Math.round(actor.maxHp * hpRatio)));
        actor.mp = Math.max(0, Math.min(actor.maxMp, (int) Math.round(actor.maxMp * mpRatio)));
    }

    private String statLabel(String statKey) {
        return switch (statKey) {
            case "strength" -> "Strength";
            case "intelligence" -> "Intelligence";
            case "dexterity" -> "Dexterity";
            case "charisma" -> "Charisma";
            case "constitution" -> "Constitution";
            case "willpower" -> "Willpower";
            default -> "stats";
        };
    }

    void rememberSharedQuest(PartyDialogue.Pair pair, String title) {
        String memory = "Defeated a dungeon leader with " + pair.names() + " during " + title + ".";
        recordCompanionMemory(pair.first(), "shared_quest", memory);
        recordCompanionMemory(pair.second(), "shared_quest", memory);
        invalidateQuestObjectiveCache();
    }

    private void recordQuestProgress(String defeatedTarget) {
        for (Quest quest : quests.values()) {
            if (PartyDialogue.isShared(quest)) continue;
            if ((quest.mainStoryQuest() || quest.companionQuest())
                    && quest.activeObjectiveLocationKind() != null && !quest.activeObjectiveLocationKind().isBlank()
                    && !combatEncounterMatchesQuest(quest)) {
                continue;
            }
            int oldProgress = quest.progress;
            quest.record(defeatedTarget);
            if (quest.progress > oldProgress) {
                invalidateQuestObjectiveCache();
                status = quest.title + ": " + quest.progress + "/" + quest.activeNeeded() + ".";
                if (quest.ready()) {
                    if (autoAdvanceReadyQuestStage(quest)) {
                        return;
                    }
                    status += " " + quest.activeReadyDialog();
                }
            }
        }
    }

    private boolean combatEncounterMatchesQuest(Quest quest) {
        if (activeQuestMonster != null && quest.id.equals(activeQuestMonster.questId)) return true;
        if (activeDungeonMonster == null || !"dungeon".equals(world.kind(currentMapId))) return false;
        WorldMap.AdventureMarker destination = dungeonMarkerFor(quest);
        if (destination != null && destination.mapId().equals(currentMapId)) return true;
        // A named campaign boss remains valid in its actual dungeon as well as its quest encounter.
        return quest.mainStoryQuest() && quest.activeNeeded() == 1 && activeDungeonMonster.boss
                && activeDungeonMonster.spec.key().equals(quest.activeMonsterKey());
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
        invalidateQuestObjectiveCache();
    }

    private boolean canDungeonMonsterMoveTo(DungeonMonsterRuntime monster, int x, int y) {
        if (!isDungeonFloor(world.tileAt(currentMapId, x, y))) {
            return false;
        }
        int roamRadius = monster.leash;
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

    private void updateQuestMonsterMovement() {
        ensureActiveQuestMonsterRuntime();
        for (List<QuestMonsterRuntime> monsters : questMonsterRuntime.values()) {
            for (QuestMonsterRuntime monster : monsters) {
                if (worldTick - monster.moveStartTick < QuestMonsterRuntime.MOVE_TICKS || worldTick < monster.nextThinkTick) {
                    continue;
                }
                monster.nextThinkTick = worldTick + 20 + random.nextInt(30);
                int distanceToPlayer = WorldMap.OVERWORLD_ID.equals(currentMapId)
                        ? Math.abs(playerX - monster.x) + Math.abs(playerY - monster.y)
                        : Integer.MAX_VALUE;
                if (distanceToPlayer > 5 && random.nextDouble() < 0.36) {
                    continue;
                }
                for (int[] direction : questMonsterDirections(monster, distanceToPlayer)) {
                    int nx = monster.x + direction[0];
                    int ny = monster.y + direction[1];
                    if (WorldMap.OVERWORLD_ID.equals(currentMapId) && nx == playerX && ny == playerY) {
                        moveQuestMonster(monster, nx, ny, direction);
                        startQuestMonsterBattle(monster);
                        return;
                    }
                    if (!canQuestMonsterMoveTo(monster, nx, ny)) {
                        continue;
                    }
                    moveQuestMonster(monster, nx, ny, direction);
                    break;
                }
            }
        }
    }

    private List<int[]> questMonsterDirections(QuestMonsterRuntime monster, int distanceToPlayer) {
        int[][] baseDirections = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        List<int[]> directions = new ArrayList<>();
        if (distanceToPlayer <= 6) {
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

    private void moveQuestMonster(QuestMonsterRuntime monster, int x, int y, int[] direction) {
        monster.fromX = monster.x;
        monster.fromY = monster.y;
        monster.x = x;
        monster.y = y;
        monster.facingDx = direction[0];
        monster.facingDy = direction[1];
        monster.moveStartTick = worldTick;
    }

    private boolean canQuestMonsterMoveTo(QuestMonsterRuntime monster, int x, int y) {
        Quest quest = quests.get(monster.questId);
        String place = quest == null ? "" : quest.activeObjectiveLocationKind();
        if (place != null && place.startsWith("place:") && !world.campaignPlaceContains(place.substring(6), x, y)) return false;
        char tile = world.tileAt(WorldMap.OVERWORLD_ID, x, y);
        if (!world.isPassable(WorldMap.OVERWORLD_ID, x, y) || Terrain.connectingRoad(tile) || tile == 'c' || tile == 'u' || tile == 'A') {
            return false;
        }
        if (Math.abs(x - monster.homeX) + Math.abs(y - monster.homeY) > 8) {
            return false;
        }
        if (!tileMatches(tile, questMonsterPreferredTiles(monster.spec))) {
            return false;
        }
        if (WorldMap.OVERWORLD_ID.equals(currentMapId) && x == playerX && y == playerY) {
            return false;
        }
        if (questMonsterAtRuntime(x, y, monster) != null) {
            return false;
        }
        return true;
    }

    private void updateNpcMovement() {
        WeatherCondition weather = currentWeather();
        int minutes = timeOfDayMinutes();
        int day = dayNumber();
        for (Npc npc : npcsForMap(currentMapId)) {
            if (questNpc(npc)) {
                continue;
            }
            NpcRuntime runtime = runtimeFor(npc);
            String mapKind = world.kind(currentMapId);
            if ("city".equals(mapKind) || "village".equals(mapKind)) {
                updateTownNpcMovement(npc, runtime, minutes, weather, day);
                continue;
            }
            AmbientNpcAi.Routine routine = AmbientNpcAi.routineFor(npc, world, currentMapId, minutes, weather, day);
            if (worldTick - runtime.moveStartTick < NpcMotion.MOVE_TICKS || worldTick < runtime.nextThinkTick) {
                continue;
            }
            int targetDistance = Math.abs(runtime.x - routine.target().x()) + Math.abs(runtime.y - routine.target().y());
            if (targetDistance <= routine.roamRadius() && random.nextDouble() < routine.idleChance()) {
                runtime.nextThinkTick = worldTick + routine.nextThinkDelay(random);
                continue;
            }
            boolean moved = false;
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
                runtime.nextThinkTick = worldTick + NpcMotion.MOVE_TICKS + npcMovePauseTicks(routine);
                moved = true;
                break;
            }
            if (!moved) {
                runtime.nextThinkTick = worldTick + routine.nextThinkDelay(random);
            }
        }
    }

    private void updateTownNpcMovement(Npc npc, NpcRuntime runtime, int minutes, WeatherCondition weather, int day) {
        if (worldTick - runtime.moveStartTick < NpcMotion.MOVE_TICKS) return;
        if (runtime.townRoutine == null || worldTick >= runtime.routineCheckTick) {
            AmbientNpcAi.Routine routine = AmbientNpcAi.routineFor(npc, world, npc.mapId(), minutes, weather, day);
            runtime.routineCheckTick = worldTick + 90;
            if (!routine.equals(runtime.townRoutine)) {
                boolean firstRoutine = runtime.townRoutine == null;
                runtime.townRoutine = routine;
                runtime.checkpoints = TownNpcNavigation.checkpoints(npc, world, routine);
                runtime.checkpointIndex = 0;
                runtime.townPath = List.of();
                runtime.pathIndex = 0;
                runtime.routeDestination = null;
                runtime.blockedAttempts = 0;
                if (!firstRoutine) runtime.nextThinkTick = worldTick;
            }
        }
        if (worldTick < runtime.nextThinkTick) return;
        AmbientNpcAi.Routine routine = runtime.townRoutine;
        TilePoint position = new TilePoint(runtime.x, runtime.y);
        TilePoint checkpoint = runtime.checkpoints.get(runtime.checkpointIndex);
        if (position.equals(checkpoint) || position.equals(runtime.routeDestination)) {
            runtime.checkpointIndex = (runtime.checkpointIndex + 1) % runtime.checkpoints.size();
            runtime.townPath = List.of();
            runtime.pathIndex = 0;
            runtime.routeDestination = null;
            runtime.blockedAttempts = 0;
            // Complete a leg before pausing; never randomly reverse direction during travel.
            runtime.nextThinkTick = worldTick + routine.nextThinkDelay(random);
            return;
        }
        Set<TilePoint> occupied = new HashSet<>();
        occupied.add(new TilePoint(playerX, playerY));
        for (Npc other : npcsForMap(npc.mapId())) {
            if (other.equals(npc)) continue;
            occupied.add(npcPosition(other));
            if (!questNpc(other)) {
                NpcRuntime otherRuntime = runtimeFor(other);
                if (worldTick - otherRuntime.moveStartTick < NpcMotion.MOVE_TICKS) {
                    occupied.add(new TilePoint(otherRuntime.fromX, otherRuntime.fromY));
                }
            }
        }
        java.util.function.BiPredicate<Integer, Integer> walkable = (x, y) ->
                world.isPassable(npc.mapId(), x, y) && !occupied.contains(new TilePoint(x, y))
                        && world.transitionAt(npc.mapId(), x, y) == null
                        && blockingQuestObjectiveAt(npc.mapId(), x, y) == null;
        if (runtime.pathIndex < runtime.townPath.size()) {
            TilePoint next = runtime.townPath.get(runtime.pathIndex);
            if (!walkable.test(next.x(), next.y())) {
                runtime.townPath = List.of();
                runtime.pathIndex = 0;
                // Briefly yield before replanning around another resident or the player.
                runtime.nextThinkTick = worldTick + 15 + random.nextInt(20);
                return;
            }
        }
        if (runtime.pathIndex >= runtime.townPath.size()) {
            runtime.townPath = TownNpcNavigation.path(world.width(npc.mapId()), world.height(npc.mapId()),
                    position, checkpoint, walkable, (x, y) -> Terrain.roadLike(world.tileAt(npc.mapId(), x, y)));
            runtime.pathIndex = 0;
            runtime.routeDestination = checkpoint;
            if (runtime.townPath.isEmpty() && runtime.blockedAttempts >= 2) {
                // A busy shop or inaccessible interaction spot should not strand its visitors.
                for (int radius = 1; radius <= 2 && runtime.townPath.isEmpty(); radius++) {
                    for (int[] direction : new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}}) {
                        TilePoint nearby = new TilePoint(checkpoint.x() + direction[0] * radius,
                                checkpoint.y() + direction[1] * radius);
                        if (!walkable.test(nearby.x(), nearby.y())) continue;
                        if (position.equals(nearby)) {
                            runtime.routeDestination = nearby;
                            runtime.nextThinkTick = worldTick + 1;
                            return;
                        }
                        runtime.townPath = TownNpcNavigation.path(world.width(npc.mapId()), world.height(npc.mapId()),
                                position, nearby, walkable, (x, y) -> Terrain.roadLike(world.tileAt(npc.mapId(), x, y)));
                        if (!runtime.townPath.isEmpty()) {
                            runtime.routeDestination = nearby;
                            break;
                        }
                    }
                }
            }
            if (runtime.townPath.isEmpty()) {
                runtime.routeDestination = null;
                if (++runtime.blockedAttempts >= 3) {
                    runtime.checkpointIndex = (runtime.checkpointIndex + 1) % runtime.checkpoints.size();
                    runtime.blockedAttempts = 0;
                }
                runtime.nextThinkTick = worldTick + routine.nextThinkDelay(random);
                return;
            }
        }
        TilePoint next = runtime.townPath.get(runtime.pathIndex++);
        runtime.fromX = runtime.x;
        runtime.fromY = runtime.y;
        runtime.facingDx = next.x() - runtime.x;
        runtime.facingDy = next.y() - runtime.y;
        runtime.x = next.x();
        runtime.y = next.y();
        runtime.moveStartTick = worldTick;
        runtime.blockedAttempts = 0;
        runtime.nextThinkTick = worldTick + NpcMotion.MOVE_TICKS;
    }

    private int npcMovePauseTicks(AmbientNpcAi.Routine routine) {
        int base = switch (routine.activity()) {
            case PATROLLING, GATHERING, FISHING -> 1;
            case OPENING_SHOP, ERRAND, RETURNING_HOME -> 2;
            case SOCIALIZING, WORKING, STUDYING -> 5;
            case SHELTERING, RESTING, SLEEPING -> 10;
        };
        return base + random.nextInt(base + 4);
    }

    private List<int[]> npcMoveDirections(NpcRuntime runtime, AmbientNpcAi.Routine routine) {
        int[][] baseDirections = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        List<int[]> directions = new ArrayList<>();
        int distance = Math.abs(runtime.x - routine.target().x()) + Math.abs(runtime.y - routine.target().y());
        if (distance > 0) {
            int dx = Integer.compare(routine.target().x(), runtime.x);
            int dy = Integer.compare(routine.target().y(), runtime.y);
            if (dx != 0 && dy != 0) {
                addNpcDirection(directions, dx, dy);
            }
            if (Math.abs(routine.target().x() - runtime.x) >= Math.abs(routine.target().y() - runtime.y)) {
                addNpcDirection(directions, dx, 0);
                addNpcDirection(directions, 0, dy);
            } else {
                addNpcDirection(directions, 0, dy);
                addNpcDirection(directions, dx, 0);
            }
            if (distance > routine.roamRadius() + 1) {
                addNpcDirection(directions, -dx, 0);
                addNpcDirection(directions, 0, -dy);
            }
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
        if (Math.abs(x - runtime.x) == 1 && Math.abs(y - runtime.y) == 1
                && !world.isPassable(npc.mapId(), x, runtime.y)
                && !world.isPassable(npc.mapId(), runtime.x, y)) {
            return false;
        }
        int currentHomeDistance = Math.abs(runtime.x - runtime.homeX) + Math.abs(runtime.y - runtime.homeY);
        int nextHomeDistance = Math.abs(x - runtime.homeX) + Math.abs(y - runtime.homeY);
        if (nextHomeDistance > maxDistanceFromHome && nextHomeDistance >= currentHomeDistance) {
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
        if (npcListCacheTick != worldTick) {
            npcListCache.clear();
            npcListCacheTick = worldTick;
        }
        List<Npc> cached = npcListCache.get(mapId);
        if (cached != null) {
            return cached;
        }
        List<Npc> npcs = new ArrayList<>();
        ensureActiveQuestNpcRuntime();
        for (Npc npc : GameData.NPCS) {
            if (npc.mapId().equals(mapId) && !recruitedCompanionNpc(npc) && !npcRelocatedByQuest(npc)
                    && commuterSourceVisible(npc, mapId)) {
                npcs.add(npc);
            }
        }
        for (Npc npc : world.npcs(mapId)) {
            if (commuterSourceVisible(npc, mapId)) {
                npcs.add(npc);
            }
        }
        addCommuterNpcs(npcs, mapId);
        if (WorldMap.PLAYER_VILLAGE_ID.equals(mapId)) {
            addAssignedVillageNpcs(npcs);
        } else if (mapId.startsWith("house_" + WorldMap.PLAYER_VILLAGE_ID + "_")) {
            addAssignedInteriorNpc(npcs, mapId);
        }
        addQuestNpcs(npcs, mapId);
        npcs.addAll(CompanionQuestContent.aftermath(quests, world, mapId));
        List<Npc> snapshot = List.copyOf(npcs);
        npcListCache.put(mapId, snapshot);
        return snapshot;
    }

    private void addQuestNpcs(List<Npc> npcs, String mapId) {
        for (List<QuestNpcRuntime> runtimes : questNpcRuntime.values()) {
            for (QuestNpcRuntime runtime : runtimes) {
                if (runtime.npc.mapId().equals(mapId)) {
                    npcs.add(runtime.npc);
                }
            }
        }
    }

    private boolean commuterSourceVisible(Npc npc, String mapId) {
        if (npc == null || npc.job() == null || !mapId.equals(npc.mapId())) {
            return true;
        }
        return commuterNpcForSource(npc) == null;
    }

    private void addCommuterNpcs(List<Npc> npcs, String mapId) {
        if (!WorldMap.OVERWORLD_ID.equals(mapId)) {
            return;
        }
        for (Npc source : commuterSourceNpcs()) {
            Npc commuter = commuterNpcForSource(source);
            if (commuter != null) {
                npcs.add(commuter);
            }
        }
    }

    private List<Npc> commuterSourceNpcs() {
        List<Npc> sources = new ArrayList<>();
        for (Npc npc : GameData.NPCS) {
            if (eligibleCommuterSourceNpc(npc)) {
                sources.add(npc);
            }
        }
        for (WorldMap.SettlementSite settlement : world.settlementSites()) {
            for (Npc npc : world.npcs(settlement.id())) {
                if (eligibleCommuterSourceNpc(npc)) {
                    sources.add(npc);
                }
            }
        }
        return sources;
    }

    private boolean eligibleCommuterSourceNpc(Npc npc) {
        return npc != null
                && npc.job() != null
                && npc.recruitId() == null
                && npc.questId() == null
                && npc.shopId() == null
                && !storyNpc(npc)
                && world.overworldEntranceFor(npc.mapId()) != null
                && !npcRelocatedByQuest(npc);
    }

    private boolean storyNpc(Npc npc) {
        return npc != null && npc.sprite() != null && npc.sprite().startsWith("npc_story_");
    }

    private Npc commuterNpcForSource(Npc source) {
        if (!eligibleCommuterSourceNpc(source) || !source.job().activeAt(timeOfDayMinutes())) {
            return null;
        }
        TilePoint workSite = commuterWorkSite(source);
        if (workSite == null) {
            return null;
        }
        List<String> dialog = new ArrayList<>(source.dialog());
        String place = world.label(source.mapId());
        dialog.add(source.name() + ": I am on the day shift outside " + place + ". I head back before dark.");
        dialog.add("Work: " + source.job().roleLabel() + " duty keeps me between the gate and the nearest good ground.");
        return new Npc(
                WorldMap.OVERWORLD_ID,
                source.name(),
                source.sprite(),
                workSite.x(),
                workSite.y(),
                dialog,
                source.questId(),
                source.shopId(),
                source.recruitId(),
                source.recruitCost(),
                source.professionXp(),
                source.job()
        );
    }

    private Npc commuterSourceNpcFor(Npc npc) {
        if (npc == null || npc.job() == null || !WorldMap.OVERWORLD_ID.equals(npc.mapId())) {
            return null;
        }
        TilePoint position = new TilePoint(npc.x(), npc.y());
        for (Npc source : commuterSourceNpcs()) {
            TilePoint workSite = commuterWorkSite(source);
            if (source.job() != null
                    && source.job().kind() == npc.job().kind()
                    && source.name().equals(npc.name())
                    && position.equals(workSite)) {
                return source;
            }
        }
        return null;
    }

    private TilePoint commuterWorkSite(Npc source) {
        String key = rawNpcStateKey(source) + ":" + source.job().kind();
        TilePoint cached = commuterWorkSiteCache.get(key);
        if (cached != null) {
            return cached;
        }
        TilePoint entrance = world.overworldEntranceFor(source.mapId());
        if (entrance == null) {
            return null;
        }
        int seed = Math.abs(key.hashCode());
        TilePoint site = commuterPropWorkSite(source, entrance, seed);
        if (site == null) {
            site = commuterLocationWorkSite(source, entrance, seed);
        }
        if (site == null) {
            site = closestPassableOverworldPoint(entrance, 6 + Math.floorMod(seed, 6), seed);
        }
        if (site != null) {
            commuterWorkSiteCache.put(key, site);
        }
        return site;
    }

    private TilePoint commuterPropWorkSite(Npc source, TilePoint entrance, int seed) {
        TilePoint best = null;
        int bestScore = Integer.MIN_VALUE;
        for (WorldProp prop : world.props(WorldMap.OVERWORLD_ID)) {
            if (!matchesCommuterProp(source.job().kind(), prop.asset())) {
                continue;
            }
            int distance = Math.abs(prop.x() - entrance.x()) + Math.abs(prop.y() - entrance.y());
            if (distance > commuterSearchRadius(source.job().kind())) {
                continue;
            }
            TilePoint site = commuterInteractionSpot(prop, seed);
            if (site == null) {
                continue;
            }
            int score = 220 - distance * 5
                    + commuterLocationBonus(source.job().kind(), world.locationKindAt(WorldMap.OVERWORLD_ID, prop.x(), prop.y()))
                    + Math.floorMod(seed + prop.asset().hashCode() + prop.x() * 13 + prop.y() * 29, 23);
            if (score > bestScore) {
                bestScore = score;
                best = site;
            }
        }
        return best;
    }

    private TilePoint commuterLocationWorkSite(Npc source, TilePoint entrance, int seed) {
        TilePoint best = null;
        int bestScore = Integer.MIN_VALUE;
        int radius = commuterSearchRadius(source.job().kind());
        for (int y = Math.max(1, entrance.y() - radius); y <= Math.min(world.height(WorldMap.OVERWORLD_ID) - 2, entrance.y() + radius); y++) {
            for (int x = Math.max(1, entrance.x() - radius); x <= Math.min(world.width(WorldMap.OVERWORLD_ID) - 2, entrance.x() + radius); x++) {
                if (!world.isPassable(WorldMap.OVERWORLD_ID, x, y)) {
                    continue;
                }
                String locationKind = world.locationKindAt(WorldMap.OVERWORLD_ID, x, y);
                int locationBonus = commuterLocationBonus(source.job().kind(), locationKind);
                if (locationBonus <= 0) {
                    continue;
                }
                int distance = Math.abs(x - entrance.x()) + Math.abs(y - entrance.y());
                int score = 150 + locationBonus - distance * 4 + Math.floorMod(seed + x * 17 + y * 31, 19);
                if (score > bestScore) {
                    bestScore = score;
                    best = new TilePoint(x, y);
                }
            }
        }
        return best;
    }

    private TilePoint commuterInteractionSpot(WorldProp prop, int seed) {
        int[][] offsets = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
        int start = Math.floorMod(seed + prop.x() * 7 + prop.y() * 11, offsets.length);
        for (int i = 0; i < offsets.length; i++) {
            int[] offset = offsets[(start + i) % offsets.length];
            int x = prop.x() + offset[0];
            int y = prop.y() + offset[1];
            if (world.isPassable(WorldMap.OVERWORLD_ID, x, y) && world.propAt(WorldMap.OVERWORLD_ID, x, y) == null) {
                return new TilePoint(x, y);
            }
        }
        return world.isPassable(WorldMap.OVERWORLD_ID, prop.x(), prop.y()) ? new TilePoint(prop.x(), prop.y()) : null;
    }

    private TilePoint closestPassableOverworldPoint(TilePoint center, int radius, int seed) {
        if (world.isPassable(WorldMap.OVERWORLD_ID, center.x(), center.y())) {
            return center;
        }
        int[][] ring = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
        int start = Math.floorMod(seed, ring.length);
        for (int r = 1; r <= radius; r++) {
            for (int i = 0; i < ring.length; i++) {
                int[] direction = ring[(start + i) % ring.length];
                for (int step = 1; step <= r; step++) {
                    int x = center.x() + direction[0] * step;
                    int y = center.y() + direction[1] * step;
                    if (world.isPassable(WorldMap.OVERWORLD_ID, x, y)) {
                        return new TilePoint(x, y);
                    }
                }
            }
        }
        return null;
    }

    private boolean matchesCommuterProp(NpcJob.Kind kind, String asset) {
        if (asset == null) {
            return false;
        }
        String lower = asset.toLowerCase(Locale.ROOT);
        return switch (kind) {
            case FARMER -> lower.contains("farmland_wheat") || lower.contains("hay") || lower.contains("tilled");
            case WOODCUTTER -> (lower.contains("tree") || lower.contains("log") || lower.contains("stump"))
                    && (lower.contains("harvestable") || lower.contains("fallen") || lower.contains("wood"));
            case HERBALIST -> lower.contains("herb") || lower.contains("flower") || lower.contains("mushroom") || lower.contains("root");
        };
    }

    private int commuterLocationBonus(NpcJob.Kind kind, String locationKind) {
        if (locationKind == null || locationKind.isBlank()) {
            return 0;
        }
        return switch (kind) {
            case FARMER -> "farmland".equals(locationKind) ? 60 : 0;
            case WOODCUTTER -> switch (locationKind) {
                case "hidden_grove", "forest_shrine" -> 45;
                default -> 0;
            };
            case HERBALIST -> switch (locationKind) {
                case "hidden_grove", "forest_shrine" -> 60;
                case "farmland" -> 24;
                default -> 0;
            };
        };
    }

    private int commuterSearchRadius(NpcJob.Kind kind) {
        return switch (kind) {
            case FARMER -> 44;
            case WOODCUTTER -> 52;
            case HERBALIST -> 48;
        };
    }

    private void addAssignedVillageNpcs(List<Npc> npcs) {
        if (!isDaytime()) {
            return;
        }
        List<Actor> traveling = activeAllies();
        for (Map.Entry<String, String> entry : villageBuildingAssignments.entrySet()) {
            CityBuilding building = playerVillageBuildingByKey(entry.getKey());
            Actor ally = allyByName(entry.getValue());
            if (building == null || ally == null || !villageAllies.contains(ally.name)) {
                continue;
            }
            if (traveling.contains(ally)) {
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
        if (activeAllies().contains(ally)) {
            return;
        }
        TilePoint home = world.interiorEntryPoint(mapId);
        npcs.add(assignedCompanionNpc(ally, mapId, home.x(), Math.max(1, home.y() - 2), building, true));
    }

    private Npc assignedCompanionNpc(Actor ally, String mapId, int x, int y, CityBuilding building, boolean inside) {
        String roleId = villageRoleFor(ally.name);
        VillageManager.WorkerRole role = VillageManager.workerRole(roleId);
        Npc sourceNpc = npcForAlly(ally);
        List<String> dialog = new ArrayList<>();
        if (sourceNpc != null) {
            dialog.addAll(sourceNpc.dialog());
        }
        dialog.add(ally.name + ": " + (inside ? "Off shift inside " : "Working at ")
                + VillageManager.buildingLabel(building.style()) + ". Role: " + role.label() + ".");
        dialog.add("Abilities: " + abilityPreview(ally));
        dialog.add("Skills: " + professionPreview(ally));
        dialog.add("Pack: " + actorInventoryPreview(ally));
        dialog.add("Work: " + role.description());
        return new Npc(
                mapId,
                ally.name,
                ally.sprite,
                x,
                y,
                dialog,
                sourceNpc == null ? null : sourceNpc.questId(),
                sourceNpc == null ? null : sourceNpc.shopId(),
                sourceNpc == null ? null : sourceNpc.recruitId(),
                0,
                ally.professionXp,
                null
        );
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
        return npcRuntime.computeIfAbsent(npc, ignored -> {
            TilePoint home = world.npcHome(npc);
            TilePoint start = world.isPassable(npc.mapId(), npc.x(), npc.y())
                    ? new TilePoint(npc.x(), npc.y())
                    : home;
            return new NpcRuntime(start.x(), start.y(), home.x(), home.y(), Math.max(0, random.nextInt(90)));
        });
    }

    private String cleanName(String name, int maxLength) {
        if (name == null) {
            return "";
        }
        String cleaned = name.replaceAll("\\s+", " ").stripLeading();
        return cleaned.substring(0, Math.min(maxLength, cleaned.length()));
    }

    /** Weak residents first: level gates may reduce variety, but never change a site's identity. */
    static List<String> locationMonsterPool(String kind) {
        if (kind == null) return List.of();
        return switch (kind) {
            case "goblin_camp" -> List.of("goblin_scout", "goblin_archer", "goblin_trapper", "goblin_skirmisher", "goblin_shaman");
            case "bandit_camp" -> List.of("bandit_cutthroat", "bandit_archer", "bandit_captain");
            case "graveyard", "crypt" -> List.of("skeleton", "crypt_bat", "wraith", "bone_knight", "crypt_revenant");
            case "cave", "cave_mouth" -> List.of("crypt_bat", "spider", "stoneback_goat", "mountain_drake");
            case "abandoned_castle" -> List.of("skeleton", "bandit_cutthroat", "bone_knight", "wraith");
            case "prison" -> List.of("skeleton", "bandit_cutthroat", "bone_knight");
            case "sewer" -> List.of("slime", "spider", "reed_serpent", "bog_beast");
            default -> List.of();
        };
    }

    private String chooseMonster(char tile, String locationKind) {
        List<String> pool = locationMonsterPool(locationKind);
        if (pool.isEmpty()) pool = switch (tile) {
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

    private List<String> chooseMonsterGroup(char tile, String locationKind) {
        int count = 1;
        double roll = random.nextDouble();
        if (player.level >= 9 && roll < config.threeMonsterChance * 0.33) {
            count = 5;
        } else if (player.level >= 6 && roll < config.threeMonsterChance * 0.66) {
            count = 4;
        } else if (roll < config.threeMonsterChance && player.level >= 3) {
            count = 3;
        } else if (roll < config.threeMonsterChance + config.twoMonsterChance) {
            count = 2;
        }
        count = Math.min(MAX_ENEMY_GROUP_SIZE, count);
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            keys.add(chooseMonster(tile, locationKind));
        }
        return keys;
    }

    private List<GameData.MonsterSpec> monsterSpecsForKeys(List<String> keys) {
        List<GameData.MonsterSpec> specs = new ArrayList<>();
        for (String key : keys) {
            GameData.MonsterSpec spec = GameData.MONSTERS.get(key);
            if (spec != null) {
                specs.add(spec);
            }
        }
        if (specs.isEmpty()) {
            return List.of(GameData.MONSTERS.get("slime"));
        }
        return specs.size() <= MAX_ENEMY_GROUP_SIZE ? specs : specs.subList(0, MAX_ENEMY_GROUP_SIZE);
    }

    public record NpcMotion(Npc npc, int x, int y, int fromX, int fromY, int moveStartTick, int facingDx, int facingDy) {
        public static final int MOVE_TICKS = 12;
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
            boolean blocking,
            String markerLabel,
            boolean markerOnly,
            boolean mainStory,
            boolean dungeonHazard,
            int worldMapX,
            int worldMapY,
            String stageId
    ) {
        public QuestObjective(
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
            this(questId, title, target, mapId, x, y, asset, kind, monsterKey, blocking,
                    title, false, false, false, -1, -1, "");
        }
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

    public record TravelBanterPrompt(
            String speaker,
            String line,
            List<String> options,
            List<String> optionTooltips,
            List<Integer> optionRelationshipDeltas,
            List<String> tags,
            List<String> optionResponseLines,
            int expiresAtTick
    ) {
        public TravelBanterPrompt(String speaker, String line, List<String> options, int expiresAtTick) {
            this(speaker, line, options, List.of(), List.of(), List.of(), List.of(), expiresAtTick);
        }

        public TravelBanterPrompt(
                String speaker,
                String line,
                List<String> options,
                List<String> optionTooltips,
                int expiresAtTick
        ) {
            this(speaker, line, options, optionTooltips, List.of(), List.of(), List.of(), expiresAtTick);
        }

        public TravelBanterPrompt(
                String speaker,
                String line,
                List<String> options,
                List<String> optionTooltips,
                List<Integer> optionRelationshipDeltas,
                int expiresAtTick
        ) {
            this(speaker, line, options, optionTooltips, optionRelationshipDeltas, List.of(), List.of(), expiresAtTick);
        }

        public TravelBanterPrompt {
            options = options == null ? List.of() : List.copyOf(options);
            optionTooltips = optionTooltips == null ? List.of() : List.copyOf(optionTooltips);
            optionRelationshipDeltas = optionRelationshipDeltas == null ? List.of() : List.copyOf(optionRelationshipDeltas);
            tags = tags == null ? List.of() : List.copyOf(tags);
            optionResponseLines = optionResponseLines == null ? List.of() : List.copyOf(optionResponseLines);
        }
    }

    public record AmbientNpcConversation(
            String mapId,
            String speakerNpcKey,
            String listenerNpcKey,
            String speakerLine,
            String listenerLine,
            int expiresAtTick
    ) {
    }

    public record CompanionMemory(
            int worldTick,
            String category,
            String text
    ) {
        public CompanionMemory {
            category = category == null || category.isBlank() ? "event" : category.strip();
            text = text == null ? "" : text.replaceAll("\\s+", " ").strip();
        }
    }

    private record CompanionApprovalProfile(List<String> likes, List<String> dislikes, String motive) {
        private CompanionApprovalProfile {
            likes = likes == null ? List.of() : List.copyOf(likes);
            dislikes = dislikes == null ? List.of() : List.copyOf(dislikes);
            motive = motive == null ? "trust" : motive.strip();
        }
    }

    private record SoftCompanionRequest(
            String id,
            String prompt,
            List<String> tags,
            String acceptMemory,
            String fulfillMemory
    ) {
        private SoftCompanionRequest {
            id = id == null ? "" : id.strip();
            prompt = prompt == null ? "" : prompt.replaceAll("\\s+", " ").strip();
            tags = tags == null ? List.of() : List.copyOf(tags);
            acceptMemory = acceptMemory == null ? "" : acceptMemory.replaceAll("\\s+", " ").strip();
            fulfillMemory = fulfillMemory == null ? "" : fulfillMemory.replaceAll("\\s+", " ").strip();
        }

        private String fulfillMemory(String detail) {
            String cleanedDetail = detail == null ? "" : detail.replaceAll("\\s+", " ").strip();
            return cleanedDetail.isBlank() ? fulfillMemory : fulfillMemory + " (" + cleanedDetail + ")";
        }
    }

    private record CompanionEventReaction(Actor ally, String recruitId, int approvalDelta) {
    }

    private record PendingCompanionComment(
            String speaker,
            String line,
            List<String> options,
            List<String> optionTooltips,
            List<Integer> optionRelationshipDeltas,
            List<String> tags,
            List<String> optionResponseLines
    ) {
        private PendingCompanionComment {
            speaker = speaker == null ? "" : speaker.strip();
            line = line == null ? "" : line.replaceAll("\\s+", " ").strip();
            options = options == null ? List.of() : List.copyOf(options);
            optionTooltips = optionTooltips == null ? List.of() : List.copyOf(optionTooltips);
            optionRelationshipDeltas = optionRelationshipDeltas == null ? List.of() : List.copyOf(optionRelationshipDeltas);
            tags = tags == null ? List.of() : List.copyOf(tags);
            optionResponseLines = optionResponseLines == null ? List.of() : List.copyOf(optionResponseLines);
        }

        private TravelBanterPrompt toPrompt(int expiresAtTick) {
            return new TravelBanterPrompt(speaker, line, options, optionTooltips, optionRelationshipDeltas, tags, optionResponseLines, expiresAtTick);
        }
    }

    private record BanterDraft(String line, List<String> tags, String key) {
        private BanterDraft {
            line = line == null ? "" : line.replaceAll("\\s+", " ").strip();
            tags = tags == null ? List.of() : List.copyOf(tags);
            key = key == null || key.isBlank() ? line : key.strip();
        }
    }

    private record NpcPair(Npc speaker, Npc listener) {
    }

    private static final class NpcRuntime {
        AmbientNpcAi.Routine townRoutine;
        int routineCheckTick;
        List<TilePoint> checkpoints = List.of();
        int checkpointIndex;
        List<TilePoint> townPath = List.of();
        TilePoint routeDestination;
        int pathIndex;
        int blockedAttempts;
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

        NpcRuntime(int x, int y, int homeX, int homeY, int nextThinkTick) {
            this.homeX = homeX;
            this.homeY = homeY;
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
        final int leash;
        int x;
        int y;
        int fromX;
        int fromY;
        int moveStartTick = -DungeonMonsterMotion.MOVE_TICKS;
        int facingDx;
        int facingDy = 1;
        int nextThinkTick;

        DungeonMonsterRuntime(String id, GameData.MonsterSpec spec, boolean boss, int x, int y, int nextThinkTick, int leash) {
            this.id = id;
            this.spec = spec;
            this.boss = boss;
            this.homeX = x;
            this.homeY = y;
            this.leash = Math.max(2, leash);
            this.x = x;
            this.y = y;
            this.fromX = x;
            this.fromY = y;
            this.nextThinkTick = nextThinkTick;
        }
    }

    private static final class QuestMonsterRuntime {
        static final int MOVE_TICKS = 18;
        final String questId;
        final String id;
        final GameData.MonsterSpec spec;
        final int homeX;
        final int homeY;
        int x;
        int y;
        int fromX;
        int fromY;
        int moveStartTick = -MOVE_TICKS;
        int facingDx;
        int facingDy = 1;
        int nextThinkTick;

        QuestMonsterRuntime(String questId, String id, GameData.MonsterSpec spec, int x, int y, int nextThinkTick) {
            this.questId = questId;
            this.id = id;
            this.spec = spec;
            this.homeX = x;
            this.homeY = y;
            this.x = x;
            this.y = y;
            this.fromX = x;
            this.fromY = y;
            this.nextThinkTick = nextThinkTick;
        }
    }

    private static final class QuestNpcRuntime {
        final String questId;
        final String id;
        final Npc npc;
        final Npc sourceNpc;

        QuestNpcRuntime(String questId, String id, Npc npc, Npc sourceNpc) {
            this.questId = questId;
            this.id = id;
            this.npc = npc;
            this.sourceNpc = sourceNpc;
        }
    }
}
