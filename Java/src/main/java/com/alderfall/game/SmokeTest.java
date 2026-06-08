package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import com.alderfall.game.map.MapArea;
import com.alderfall.game.map.WorldTransition;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class SmokeTest {
    private SmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path javaRoot = Path.of("").toAbsolutePath().normalize();

        GameConfig config = GameConfig.load(javaRoot);
        GameState state = new GameState(config);
        state.chooseClass("Mage");
        while (state.mode == GameMode.STORY_INTRO) {
            state.advanceStoryIntro();
        }
        if (state.player.equippedItem("weapon") == null) {
            throw new IllegalStateException("Starter equipment was not equipped.");
        }
        int attackBeforeGear = state.player.attack;
        state.player.addItem("iron_sword", 1);
        state.useItem("iron_sword");
        if (state.player.attack <= attackBeforeGear) {
            throw new IllegalStateException("Equipment bonuses did not apply.");
        }
        state.player.skillPoints = 2;
        int mpBeforeSkill = state.player.maxMp;
        state.allocateSkill("battle_focus");
        state.allocateSkill("channeling");
        if (state.player.maxMp <= mpBeforeSkill || state.player.skillRank("channeling") != 1) {
            throw new IllegalStateException("Skill allocation failed.");
        }
        Actor professionTester = GameData.createPlayer("Mage");
        professionTester.level = 3;
        professionTester.professionSkillPoints = 3;
        int miningCapBefore = professionTester.professionLevelCap(Profession.MINING.id());
        state.allocateSkill(professionTester, "trade_foundations");
        state.allocateSkill(professionTester, "trade_foundations");
        state.allocateSkill(professionTester, "prospector_path");
        if (professionTester.professionLevelCap(Profession.MINING.id()) <= miningCapBefore
                || professionTester.professionPracticeBonus(Profession.MINING.id()) <= 0) {
            throw new IllegalStateException("Profession skill branch did not improve cap and output bonus.");
        }
        assertResourceNodeGathering(state);
        assertOreNodesAreAccessibleAroundMountains(state.world);
        Battle statusBattle = new Battle(state.player, GameData.MONSTERS.get("skeleton"), state.random);
        statusBattle.enemy.maxHp = 999;
        statusBattle.enemy.hp = 999;
        statusBattle.useAbility(1);
        drainBattle(statusBattle);
        boolean weakApplied = statusBattle.statusesFor(statusBattle.enemy).stream()
                .anyMatch(stack -> stack.effect.key().equals("weak"));
        if (!weakApplied) {
            throw new IllegalStateException("Ability status effect did not apply.");
        }
        Actor targetTester = GameData.createPlayer("Knight");
        targetTester.attack = 100;
        Battle targetBattle = new Battle(
                targetTester,
                List.of(),
                List.of(GameData.MONSTERS.get("slime"), GameData.MONSTERS.get("bat")),
                new Random(2),
                'g',
                "overworld"
        );
        targetBattle.enemies().get(0).hp = 20;
        targetBattle.enemies().get(1).hp = 1;
        targetBattle.selectEnemy(1);
        targetBattle.playerAttack();
        drainBattle(targetBattle);
        if (targetBattle.enemies().get(0).hp != 20 || targetBattle.enemies().get(1).alive()) {
            throw new IllegalStateException("Selected enemy target was not used.");
        }
        Actor groupTester = GameData.createPlayer("Knight");
        groupTester.attack = 100;
        Battle groupBattle = new Battle(
                groupTester,
                List.of(),
                List.of(GameData.MONSTERS.get("slime"), GameData.MONSTERS.get("bat")),
                new Random(1),
                'g',
                "overworld"
        );
        for (Actor foe : groupBattle.enemies()) {
            foe.hp = 1;
        }
        groupBattle.playerAttack();
        drainBattle(groupBattle);
        groupBattle.playerAttack();
        drainBattle(groupBattle);
        if (!groupBattle.finished || !groupBattle.victory || groupBattle.defeatedMonsterNames().size() != 2) {
            throw new IllegalStateException("Multi-opponent battle did not resolve all enemies.");
        }
        if (!state.world.isPassable(WorldMap.START_POSITION.x(), WorldMap.START_POSITION.y())) {
            throw new IllegalStateException("Start position is not passable.");
        }
        assertTravelLogNarration(state);
        assertLandmarkDiscoveryLog(state);
        assertMapAreaPropIndex();
        assertPathfinder(state);
        assertOverworldTraversal(state.world);
        assertOverworldDiscoverabilityProps(state.world);
        assertSettlementGateRoads(state.world);
        assertRoadMaterialsGenerated(state.world);
        assertWeatherControllerFacade(state);
        if (!state.currentMapId.equals(WorldMap.PLAYER_VILLAGE_ID)) {
            throw new IllegalStateException("Expected new adventure to start in the player camp.");
        }
        if (!state.world.playerVillageBuildings().isEmpty()) {
            throw new IllegalStateException("Player camp should start without buildings.");
        }
        assertCleanVillageGround(state.world, WorldMap.PLAYER_VILLAGE_ID, true);
        assertCleanVillageGround(state.world, "village_oakhaven", false);
        assertNoBuildingBaseTiles(state.world, "city_riverside");
        assertReviveRespawnsAtOathsteadCamp(state);
        state.currentMapId = WorldMap.PLAYER_VILLAGE_ID;
        state.playerX = 14;
        state.playerY = 17;
        assertTownPortalFastTravel(state);
        assertCompanionNpcVisibility(state);
        assertSettlementWeeklyQuestCoverage(state);
        state.currentMapId = "village_oakhaven";
        state.playerX = 13;
        state.playerY = 8;
        state.interact();
        if (state.mode != GameMode.DIALOG) {
            throw new IllegalStateException("Expected Oakhaven NPC dialog.");
        }
        acceptActiveQuestThroughDialogue(state, "bread_for_road");
        if (!state.quests.get("bread_for_road").accepted) {
            throw new IllegalStateException("Quest dialogue acceptance path failed.");
        }
        state.openActiveShop();
        if (state.mode != GameMode.SHOP) {
            throw new IllegalStateException("Expected Oakhaven shop.");
        }
        assertShopTransactions(state);
        state.closeOverlay();
        state.player.gold = 100;
        talkToNamedNpc(state, "village_oakhaven", "Bran");
        if (state.mode != GameMode.DIALOG) {
            throw new IllegalStateException("Expected Bran recruit dialog.");
        }
        state.advanceDialog();
        state.advanceDialog();
        state.hireActiveRecruit();
        if (state.allies.isEmpty() || !state.recruitedIds.contains("bran")) {
            throw new IllegalStateException("Recruit hire failed.");
        }
        state.closeOverlay();
        state.currentMapId = WorldMap.PLAYER_VILLAGE_ID;
        state.playerX = 14;
        state.playerY = 17;
        state.toggleVillage();
        state.stationAlly("Bran");
        if (!state.activeAllies().isEmpty() || state.stationedAllies().isEmpty()) {
            throw new IllegalStateException("Village ally stationing failed.");
        }
        state.recallAlly("Bran");
        if (state.activeAllies().isEmpty() || !state.stationedAllies().isEmpty()) {
            throw new IllegalStateException("Village ally recall failed.");
        }
        state.recruitAlly("seraphine");
        state.recruitAlly("maera");
        state.recruitAlly("aria");
        if (state.activeAllies().size() != 3) {
            throw new IllegalStateException("Active companion cap failed.");
        }
        Battle cappedBattle = new Battle(
                state.player,
                state.activeAllies(),
                List.of(
                        GameData.MONSTERS.get("slime"),
                        GameData.MONSTERS.get("skeleton"),
                        GameData.MONSTERS.get("spider"),
                        GameData.MONSTERS.get("wraith"),
                        GameData.MONSTERS.get("goblin"),
                        GameData.MONSTERS.get("orc_raider")
                ),
                state.random,
                'g',
                "overworld"
        );
        if (cappedBattle.partyMembers().size() != 4 || cappedBattle.enemies().size() != 5) {
            throw new IllegalStateException("Battle party/enemy caps failed.");
        }
        state.setVillageTab(2);
        state.setVillageEditAction("move");
        state.handleVillageWorldClick(15, 13);
        state.handleVillageWorldClick(20, 13);
        if (state.world.playerVillagePropAt(20, 13) == null
                || !"location_camp_fire".equals(state.world.playerVillagePropAt(20, 13).asset())) {
            throw new IllegalStateException("Default camp prop move failed.");
        }
        state.setVillageEditAction("delete");
        state.handleVillageWorldClick(18, 14);
        if (state.world.playerVillagePropAt(18, 14) != null) {
            throw new IllegalStateException("Default camp prop delete failed.");
        }
        state.setVillageTab(0);
        state.player.addItem("wood", 20);
        state.player.addItem("stone", 12);
        state.player.addItem("skin", 3);
        state.selectVillageBuildingStyle("house");
        state.handleVillageWorldClick(25, 22);
        if (state.world.cityBuildingAt(WorldMap.PLAYER_VILLAGE_ID, 25, 22) == null) {
            throw new IllegalStateException("Village building placement failed.");
        }
        state.selectVillageBuildingStyle("granary");
        state.handleVillageWorldClick(29, 20);
        CityBuilding granary = state.world.cityBuildingAt(WorldMap.PLAYER_VILLAGE_ID, 29, 20);
        if (granary == null || !"granary".equals(granary.style())) {
            throw new IllegalStateException("New village building catalog placement failed.");
        }
        CityBuilding workshop = null;
        for (int y = 18; y < 34 && workshop == null; y++) {
            for (int x = 22; x < 46 && workshop == null; x++) {
                if (state.world.canPlacePlayerVillageBuilding(x, y, 4, 3, null)) {
                    workshop = state.world.placePlayerVillageBuilding("shop", x, y);
                }
            }
        }
        if (workshop == null) {
            throw new IllegalStateException("Workshop placement for collision test failed.");
        }
        if (!state.world.isPassable(WorldMap.PLAYER_VILLAGE_ID, workshop.x1(), workshop.y1() + 1)) {
            throw new IllegalStateException("Village building side padding should be walkable.");
        }
        if (state.world.isPassable(WorldMap.PLAYER_VILLAGE_ID, workshop.x1() + 1, workshop.y1() + 1)) {
            throw new IllegalStateException("Village building core should block movement.");
        }
        TilePoint workshopDoor = state.world.cityBuildingDoorTiles(workshop).get(0);
        if (state.world.isPassable(WorldMap.PLAYER_VILLAGE_ID, workshopDoor.x(), workshopDoor.y())) {
            throw new IllegalStateException("Village building door tile should stay blocked for entry.");
        }
        Npc originalBran = findNpc("village_oakhaven", "Bran");
        String originalBranKey = "village_oakhaven:bran";
        String assignedBranKey = WorldMap.PLAYER_VILLAGE_ID + ":bran";
        state.npcRelationships.put(originalBranKey, 37);
        state.npcKnowledge.put(originalBranKey, 4);
        state.introducedNpcKeys.add(originalBranKey);
        state.npcRelationships.put(assignedBranKey, 9);
        state.npcKnowledge.put(assignedBranKey, 2);
        state.stationAlly("Bran");
        state.openBuildingAssignment(workshop);
        state.assignAllyToActiveBuilding("Bran");
        state.closeOverlay();
        state.mode = GameMode.EXPLORE;
        state.currentMapId = WorldMap.PLAYER_VILLAGE_ID;
        state.worldTick = 2000;
        state.resetNpcRuntime();
        Npc assignedBran = null;
        for (GameState.NpcMotion motion : state.npcMotionsForMap(WorldMap.PLAYER_VILLAGE_ID)) {
            if ("Bran".equals(motion.npc().name())) {
                assignedBran = motion.npc();
                break;
            }
        }
        if (assignedBran == null) {
            throw new IllegalStateException("Assigned Bran did not appear in the player village.");
        }
        TilePoint assignedBranPosition = state.npcPosition(assignedBran);
        boolean talkedToAssignedBran = false;
        int[][] branTalkPositions = {{0, 1}, {-1, 0}, {1, 0}, {0, -1}};
        for (int[] position : branTalkPositions) {
            int x = assignedBranPosition.x() + position[0];
            int y = assignedBranPosition.y() + position[1];
            if (!state.world.isPassable(WorldMap.PLAYER_VILLAGE_ID, x, y)) {
                continue;
            }
            state.playerX = x;
            state.playerY = y;
            if (state.talkToNpc(assignedBran)) {
                talkedToAssignedBran = true;
                break;
            }
        }
        if (!talkedToAssignedBran) {
            throw new IllegalStateException("Could not talk to assigned Bran.");
        }
        if (state.activeNpcIntroductionPending()) {
            throw new IllegalStateException("Assigned Bran was treated as a stranger.");
        }
        if (state.npcRelationship(assignedBran) != 37 || state.npcRelationship(originalBran) != 37
                || state.npcKnowledgeCount(assignedBran) != 4 || state.npcKnowledgeCount(originalBran) != 4) {
            throw new IllegalStateException("Assigned Bran did not retain original NPC relationship or knowledge state.");
        }
        if (state.npcRelationships.containsKey(assignedBranKey) || state.npcKnowledge.containsKey(assignedBranKey)) {
            throw new IllegalStateException("Assigned Bran kept a temporary village NPC state key.");
        }
        if (!state.activeNpcCanOpenAssignedShop()) {
            throw new IllegalStateException("Assigned Bran could not open his building shop from dialogue.");
        }
        state.openActiveShop();
        if (state.mode != GameMode.SHOP) {
            throw new IllegalStateException("Assigned Bran dialogue did not open the building shop.");
        }
        state.closeOverlay();
        state.toggleVillage();
        state.setVillageTab(2);
        state.selectVillageAsset("village_prop_well");
        state.handleVillageWorldClick(32, 22);
        if (state.world.playerVillageProps().stream().noneMatch(prop -> prop.asset().equals("village_prop_well"))) {
            throw new IllegalStateException("Village asset placement failed.");
        }
        state.closeOverlay();
        state.playerX = 26;
        state.playerY = 25;
        state.move(0, -1);
        if (!state.currentMapId.startsWith("house_" + WorldMap.PLAYER_VILLAGE_ID)) {
            throw new IllegalStateException("Expected player village building interior.");
        }
        state.toggleVillage();
        state.setVillageTab(3);
        state.selectInteriorAsset("interior_bookshelf");
        state.handleVillageWorldClick(8, 4);
        if (state.world.playerInteriorProps().isEmpty()) {
            throw new IllegalStateException("Village interior asset placement failed.");
        }
        String interiorMapId = state.currentMapId;
        String interiorGatherAsset = "interior_herb_planter";
        WorldProp interiorGatherProp = null;
        int interiorGatherPlayerX = 0;
        int interiorGatherPlayerY = 0;
        for (int y = 2; y < state.world.height(interiorMapId) - 2 && interiorGatherProp == null; y++) {
            for (int x = 2; x < state.world.width(interiorMapId) - 2; x++) {
                if (!state.world.canPlacePlayerInteriorProp(interiorMapId, x, y, interiorGatherAsset)) {
                    continue;
                }
                int[][] positions = {
                        {x, y + 1},
                        {x, y - 1},
                        {x + 1, y},
                        {x - 1, y}
                };
                for (int[] position : positions) {
                    if (state.world.isPassable(interiorMapId, position[0], position[1])) {
                        if (!state.world.addPlayerInteriorProp(interiorMapId, x, y, interiorGatherAsset, 48)) {
                            throw new IllegalStateException("Interior gather prop placement failed.");
                        }
                        interiorGatherProp = new WorldProp(x, y, interiorGatherAsset, 48);
                        interiorGatherPlayerX = position[0];
                        interiorGatherPlayerY = position[1];
                        break;
                    }
                }
                if (interiorGatherProp != null) {
                    break;
                }
            }
        }
        if (interiorGatherProp == null) {
            throw new IllegalStateException("Expected room for an interior gather prop regression.");
        }
        state.closeOverlay();
        state.mode = GameMode.EXPLORE;
        state.currentMapId = interiorMapId;
        state.playerX = interiorGatherPlayerX;
        state.playerY = interiorGatherPlayerY;
        int interiorInventoryBefore = state.player.inventory.values().stream().mapToInt(Integer::intValue).sum();
        state.gatherAtTile(interiorGatherProp.x(), interiorGatherProp.y());
        if (!state.crafting.active()) {
            throw new IllegalStateException("Interior gather prop did not start gathering: " + state.status);
        }
        while (state.crafting.active()) {
            state.tickWorld();
        }
        int interiorInventoryAfter = state.player.inventory.values().stream().mapToInt(Integer::intValue).sum();
        boolean interiorPropStillPresent = state.world.propsAt(interiorMapId, interiorGatherProp.x(), interiorGatherProp.y()).stream()
                .anyMatch(prop -> prop.asset().equals(interiorGatherAsset));
        if (interiorInventoryAfter <= interiorInventoryBefore || !interiorPropStillPresent) {
            throw new IllegalStateException("Interior props should grant gathering output without disappearing.");
        }
        state.closeOverlay();
        state.currentMapId = WorldMap.PLAYER_VILLAGE_ID;
        state.currentMapId = "city_riverside";
        state.playerX = 17;
        state.playerY = 11;
        state.interact();
        Quest quest = state.quests.get("slime_help");
        acceptActiveQuestThroughDialogue(state, "slime_help");
        if (quest == null || !quest.accepted) {
            throw new IllegalStateException("Quest acceptance failed.");
        }
        quest.progress = quest.activeNeeded();
        state.currentMapId = "city_riverside";
        state.playerX = 17;
        state.playerY = 11;
        state.closeOverlay();
        state.interact();
        int dialogGuard = 0;
        while (state.mode == GameMode.DIALOG && !quest.completed && dialogGuard++ < 12) {
            state.advanceDialog();
        }
        if (!quest.completed) {
            throw new IllegalStateException("Quest return turn-in failed.");
        }
        state.closeOverlay();
        state.currentMapId = WorldMap.OVERWORLD_ID;
        state.playerX = 82;
        state.playerY = 105;
        state.interact();
        if (!state.currentMapId.equals("city_riverside")) {
            throw new IllegalStateException("Expected transition into Riverside.");
        }
        assertCurrentPlayerVillageExits(state);
        assertSettlementExits(state, "village_oakhaven", WorldMap.OAKHAVEN_POSITION.x(), WorldMap.OAKHAVEN_POSITION.y(), 14, 26, 10);
        assertSettlementExits(state, "city_riverside", 82, 105, 17, state.world.height("city_riverside") - 2, 12);
        if (state.world.width("city_riverside") <= 36 || state.world.height("town_moonspire") <= 28) {
            throw new IllegalStateException("Expected cities and towns to use varied larger footprints.");
        }
        if (state.world.tileAt("town_moonspire", state.world.width("town_moonspire") - 3, 1) != 'x') {
            throw new IllegalStateException("Expected Moonspire to have an irregular city footprint.");
        }
        assertGrownPlayerVillageExits(config);
        state.currentMapId = "city_riverside";
        state.playerX = 8;
        state.playerY = 5;
        state.interact();
        if (state.currentMapId.startsWith("house_city_riverside")) {
            throw new IllegalStateException("Expected side walls not to enter city buildings.");
        }
        state.playerX = 5;
        state.playerY = 6;
        state.move(0, -1);
        if (!state.currentMapId.startsWith("house_city_riverside")) {
            throw new IllegalStateException("Expected movement through a visible door to enter a house interior.");
        }
        state.currentMapId = "city_riverside";
        state.playerX = 5;
        state.playerY = 6;
        state.interact();
        if (!state.currentMapId.startsWith("house_city_riverside")) {
            throw new IllegalStateException("Expected generated house interior.");
        }
        assertInteriorNavigable(state.world, state.currentMapId);
        state.allies.get(0).hp = 7;
        state.player.level = 6;
        state.player.xp = 11;
        state.player.gold = 123;
        state.player.attack = 17;
        state.player.defense = 5;
        SaveSystem saves = new SaveSystem(javaRoot);
        saves.save(state);
        String savedId = state.currentSaveId;
        GameState loaded = new GameState(config);
        if (!saves.load(loaded)) {
            throw new IllegalStateException("Save did not load.");
        }
        if (!loaded.player.className.equals("Mage")) {
            throw new IllegalStateException("Player class did not round-trip.");
        }
        if (!loaded.quests.get("slime_help").accepted) {
            throw new IllegalStateException("Quest state did not round-trip.");
        }
        if (!"iron_sword".equals(loaded.player.equipment.get("weapon"))) {
            throw new IllegalStateException("Equipment did not round-trip.");
        }
        if (loaded.player.level != 6 || loaded.player.xp != 11 || loaded.player.gold != 123) {
            throw new IllegalStateException("Player level, XP, or gold did not round-trip.");
        }
        if (loaded.player.attack != 17 || loaded.player.defense != 5) {
            throw new IllegalStateException("Player combat stats did not round-trip.");
        }
        if (loaded.allies.isEmpty() || !loaded.recruitedIds.contains("bran")) {
            throw new IllegalStateException("Recruit state did not round-trip.");
        }
        if (loaded.allies.get(0).hp != 7) {
            throw new IllegalStateException("Ally actor state did not round-trip.");
        }
        if (loaded.world.playerVillageBuildings().isEmpty() || loaded.world.playerVillageProps().isEmpty()
                || loaded.world.playerInteriorProps().isEmpty()) {
            throw new IllegalStateException("Village customizations did not round-trip.");
        }
        if (loaded.player.skillRank("channeling") != 1 || loaded.player.maxMp <= mpBeforeSkill) {
            throw new IllegalStateException("Skills did not round-trip.");
        }
        if (!loaded.currentMapId.equals("city_riverside") || loaded.playerX != 5 || loaded.playerY != 6) {
            throw new IllegalStateException("House save did not restore to the exterior entry.");
        }
        if (!loaded.world.isPassable(loaded.currentMapId, loaded.playerX, loaded.playerY)) {
            throw new IllegalStateException("Loaded position is not passable.");
        }
        GameState imported = new GameState(config);
        if (!saves.importCharacter(imported, savedId)) {
            throw new IllegalStateException("Character import did not load.");
        }
        Quest importedQuest = imported.quests.get("slime_help");
        if (importedQuest == null || importedQuest.accepted || importedQuest.completed || importedQuest.progress != 0) {
            throw new IllegalStateException("Imported character retained quest progress.");
        }
        if (!"iron_sword".equals(imported.player.equipment.get("weapon"))
                || imported.player.level != 6 || imported.player.xp != 11 || imported.player.gold != 123
                || imported.player.attack != 17 || imported.player.defense != 5) {
            throw new IllegalStateException("Imported character did not preserve player state.");
        }
        if (imported.allies.isEmpty() || !imported.recruitedIds.contains("bran") || imported.allies.get(0).hp != 7) {
            throw new IllegalStateException("Imported character did not preserve party state.");
        }
        if (imported.world.playerVillageStage() != 1 || !imported.world.playerVillageBuildings().isEmpty()
                || !imported.world.playerVillageProps().isEmpty() || !imported.world.playerInteriorProps().isEmpty()
                || !imported.villageStorage.isEmpty() || !imported.villageAllies.isEmpty()
                || !imported.villageWorkerRoles.isEmpty() || !imported.villageBuildingAssignments.isEmpty()) {
            throw new IllegalStateException("Imported character retained village progress.");
        }
        if (!imported.currentMapId.equals(WorldMap.PLAYER_VILLAGE_ID) || imported.playerX != 14 || imported.playerY != 17
                || imported.worldTick != 0 || imported.mode != GameMode.STORY_INTRO) {
            throw new IllegalStateException("Imported character did not start a fresh adventure.");
        }

        GameState objectiveState = new GameState(config);
        objectiveState.chooseClass("Knight");
        while (objectiveState.mode == GameMode.STORY_INTRO) {
            objectiveState.advanceStoryIntro();
        }
        Quest bread = objectiveState.quests.get("bread_for_road");
        bread.accepted = true;
        GameState.QuestObjective wheat = objectiveState.activeQuestObjectives().stream()
                .filter(objective -> objective.questId().equals("bread_for_road"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Expected active wheat objective."));
        objectiveState.currentMapId = wheat.mapId();
        objectiveState.playerX = wheat.x();
        objectiveState.playerY = wheat.y();
        objectiveState.interact();
        if (bread.progress != 1) {
            throw new IllegalStateException("Gather objective did not advance.");
        }

        Quest scarecrow = objectiveState.quests.get("scarecrow_watch");
        scarecrow.accepted = true;
        GameState.QuestObjective scarecrowObjective = objectiveState.activeQuestObjectives().stream()
                .filter(objective -> objective.questId().equals("scarecrow_watch"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Expected active scarecrow objective."));
        if (objectiveState.world.propsAt(scarecrowObjective.mapId(), scarecrowObjective.x(), scarecrowObjective.y()).stream()
                .noneMatch(prop -> prop.asset().equals("location_farmland_scarecrow"))) {
            throw new IllegalStateException("Scarecrow objective was not placed on a scarecrow prop.");
        }
        objectiveState.currentMapId = scarecrowObjective.mapId();
        objectiveState.playerX = scarecrowObjective.x();
        objectiveState.playerY = scarecrowObjective.y();
        objectiveState.interact();
        if (!scarecrow.ready()) {
            throw new IllegalStateException("Visit objective did not advance.");
        }

        Quest roadSigns = objectiveState.quests.get("aria_chain_2");
        roadSigns.accepted = true;
        boolean collectedOutOfOrderRoadSign = false;
        while (!roadSigns.ready()) {
            List<GameState.QuestObjective> signObjectives = objectiveState.activeQuestObjectives().stream()
                    .filter(objective -> objective.questId().equals("aria_chain_2"))
                    .toList();
            if (signObjectives.size() != roadSigns.needed - roadSigns.progress) {
                throw new IllegalStateException("Road sign objective count did not match remaining progress.");
            }
            Set<TilePoint> signTiles = new HashSet<>();
            for (GameState.QuestObjective objective : signObjectives) {
                if (!"quest_broken_road_signs".equals(objective.asset())) {
                    throw new IllegalStateException("Road sign objective used wrong asset: " + objective.asset());
                }
                if (!signTiles.add(new TilePoint(objective.x(), objective.y()))) {
                    throw new IllegalStateException("Road sign objectives stacked on one tile.");
                }
            }
            GameState.QuestObjective sign = !collectedOutOfOrderRoadSign && signObjectives.size() > 1
                    ? signObjectives.get(signObjectives.size() - 1)
                    : signObjectives.get(0);
            collectedOutOfOrderRoadSign = true;
            objectiveState.currentMapId = sign.mapId();
            objectiveState.playerX = sign.x();
            objectiveState.playerY = sign.y();
            objectiveState.interact();
        }
        if (roadSigns.progress != roadSigns.needed) {
            throw new IllegalStateException("Road sign objective did not complete.");
        }

        Quest crown = objectiveState.quests.get("goblin_crown");
        crown.accepted = true;
        GameState.QuestObjective king = objectiveState.activeQuestObjectives().stream()
                .filter(objective -> objective.questId().equals("goblin_crown"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Expected active Goblin King objective."));
        objectiveState.currentMapId = king.mapId();
        objectiveState.playerX = king.x() + 1;
        objectiveState.playerY = king.y();
        objectiveState.player.attack = 999;
        objectiveState.interact();
        if (objectiveState.mode != GameMode.BATTLE || objectiveState.battle == null
                || !objectiveState.battle.monsterSpecs.get(0).key().equals("goblin_king")) {
            throw new IllegalStateException("Goblin King objective did not start the boss battle.");
        }
        objectiveState.battleAttack();
        drainBattle(objectiveState);
        if (!crown.ready()) {
            throw new IllegalStateException("Goblin King battle did not advance quest progress.");
        }
        System.out.println("Smoke test passed. " + loaded.player.className + " at " + loaded.playerX + "," + loaded.playerY);
    }

    private static void drainBattle(Battle battle) {
        for (int i = 0; i < 500 && battle.isAnimating(); i++) {
            battle.tick();
        }
        if (battle.isAnimating()) {
            throw new IllegalStateException("Battle animation did not complete.");
        }
    }

    private static void drainBattle(GameState state) {
        for (int i = 0; i < 500 && state.battle != null && state.battle.isAnimating(); i++) {
            state.tickBattle();
        }
        if (state.battle != null && state.battle.isAnimating()) {
            throw new IllegalStateException("Battle animation did not complete.");
        }
    }

    private static void assertWeatherControllerFacade(GameState state) {
        String oldMapId = state.currentMapId;
        int oldX = state.playerX;
        int oldY = state.playerY;
        int oldTick = state.worldTick;
        try {
            state.currentMapId = WorldMap.PLAYER_VILLAGE_ID;
            state.playerX = 14;
            state.playerY = 17;
            state.worldTick = GameState.TICKS_PER_GAME_DAY + 345;
            WeatherCondition outdoorWeather = state.currentWeather();
            if (!state.weatherLabel().equals(outdoorWeather.label())) {
                throw new IllegalStateException("Weather label did not match current weather.");
            }
            double windStrength = state.windStrength();
            if (windStrength < 0.0 || windStrength > 1.0 || state.windLabel().isBlank()) {
                throw new IllegalStateException("Weather wind facade returned invalid values.");
            }

            List<CityBuilding> buildings = state.world.cityBuildings("village_oakhaven");
            if (buildings.isEmpty()) {
                throw new IllegalStateException("Expected Oakhaven to have at least one building for weather coverage.");
            }
            CityBuilding building = buildings.get(0);
            state.currentMapId = state.world.ensureHouseInterior("village_oakhaven", building.x1(), building.y1(), 13, 8);
            if (state.currentWeather() != WeatherCondition.CLEAR) {
                throw new IllegalStateException("Indoor weather should remain clear.");
            }
        } finally {
            state.currentMapId = oldMapId;
            state.playerX = oldX;
            state.playerY = oldY;
            state.worldTick = oldTick;
        }
    }

    private static void talkToNamedNpc(GameState state, String mapId, String npcName) {
        Npc target = findNpc(mapId, npcName);
        state.currentMapId = mapId;
        TilePoint position = state.npcPosition(target);
        int[][] candidates = {{0, 1}, {-1, 0}, {1, 0}, {0, -1}};
        for (int[] candidate : candidates) {
            int x = position.x() + candidate[0];
            int y = position.y() + candidate[1];
            if (!state.world.isPassable(mapId, x, y)) {
                continue;
            }
            state.playerX = x;
            state.playerY = y;
            if (state.talkToNpc(target)) {
                return;
            }
        }
        throw new IllegalStateException("Could not reach smoke-test NPC: " + npcName);
    }

    private static Npc findNpc(String mapId, String npcName) {
        for (Npc npc : GameData.NPCS) {
            if (npc.mapId().equals(mapId) && npc.name().equals(npcName)) {
                return npc;
            }
        }
        throw new IllegalStateException("Missing smoke-test NPC: " + npcName);
    }

    private static void assertSettlementExits(
            GameState state,
            String mapId,
            int overworldX,
            int overworldY,
            int verticalGateX,
            int southStartY,
            int horizontalGateY
    ) {
        assertExit(state, mapId, verticalGateX, 1, 0, -1, overworldX, overworldY - 3);
        assertExit(state, mapId, verticalGateX, southStartY, 0, 1, overworldX, overworldY + 3);
        assertExit(state, mapId, 1, horizontalGateY, -1, 0, overworldX - 3, overworldY);
        assertExit(state, mapId, state.world.width(mapId) - 2, horizontalGateY, 1, 0, overworldX + 3, overworldY);
    }

    private static void assertCurrentPlayerVillageExits(GameState state) {
        int offset = (Math.max(1, state.world.playerVillageStage()) - 1) * 10;
        int eastX = state.world.width(WorldMap.PLAYER_VILLAGE_ID) - 1;
        int southY = state.world.height(WorldMap.PLAYER_VILLAGE_ID) - 1;
        assertExit(state, WorldMap.PLAYER_VILLAGE_ID, 14 + offset, 1, 0, -1,
                WorldMap.START_POSITION.x(), WorldMap.START_POSITION.y() - 3);
        assertExit(state, WorldMap.PLAYER_VILLAGE_ID, 14 + offset, southY - 1, 0, 1,
                WorldMap.START_POSITION.x(), WorldMap.START_POSITION.y() + 3);
        assertExit(state, WorldMap.PLAYER_VILLAGE_ID, 1, 10 + offset, -1, 0,
                WorldMap.START_POSITION.x() - 3, WorldMap.START_POSITION.y());
        assertExit(state, WorldMap.PLAYER_VILLAGE_ID, eastX - 1, 10 + offset, 1, 0,
                WorldMap.START_POSITION.x() + 3, WorldMap.START_POSITION.y());
    }

    private static void assertGrownPlayerVillageExits(GameConfig config) {
        GameState state = new GameState(config);
        state.chooseClass("Mage");
        while (state.mode == GameMode.STORY_INTRO) {
            state.advanceStoryIntro();
        }
        state.world.setPlayerVillageStage(3);
        int offset = 20;
        int eastX = state.world.width(WorldMap.PLAYER_VILLAGE_ID) - 1;
        int southY = state.world.height(WorldMap.PLAYER_VILLAGE_ID) - 1;
        assertExit(state, WorldMap.PLAYER_VILLAGE_ID, 14 + offset, 1, 0, -1,
                WorldMap.START_POSITION.x(), WorldMap.START_POSITION.y() - 3);
        assertExit(state, WorldMap.PLAYER_VILLAGE_ID, 14 + offset, southY - 1, 0, 1,
                WorldMap.START_POSITION.x(), WorldMap.START_POSITION.y() + 3);
        assertExit(state, WorldMap.PLAYER_VILLAGE_ID, 1, 10 + offset, -1, 0,
                WorldMap.START_POSITION.x() - 3, WorldMap.START_POSITION.y());
        assertExit(state, WorldMap.PLAYER_VILLAGE_ID, eastX - 1, 10 + offset, 1, 0,
                WorldMap.START_POSITION.x() + 3, WorldMap.START_POSITION.y());
        assertExit(state, WorldMap.PLAYER_VILLAGE_ID, eastX, 10 + offset, 1, 0,
                WorldMap.START_POSITION.x() + 3, WorldMap.START_POSITION.y());
        assertPlayerVillageEntry(state, 0, -2, 14 + offset, 1);
        assertPlayerVillageEntry(state, 0, 2, 14 + offset, southY - 1);
        assertPlayerVillageEntry(state, -2, 0, 1, 10 + offset);
        assertPlayerVillageEntry(state, 2, 0, eastX - 1, 10 + offset);
        assertPlayerVillageEntry(state, 0, 0, 14 + offset, 1);
        if (state.world.props(WorldMap.PLAYER_VILLAGE_ID).stream()
                .noneMatch(prop -> prop.asset().equals("player_village_exit_marker"))) {
            throw new IllegalStateException("Grown player village exit markers were not added.");
        }
    }

    private static void assertReviveRespawnsAtOathsteadCamp(GameState state) {
        state.revive();
        if (!WorldMap.PLAYER_VILLAGE_ID.equals(state.currentMapId)) {
            throw new IllegalStateException("Revive should respawn at Oathstead Camp.");
        }
        if (!"village".equals(state.world.kind(state.currentMapId))) {
            throw new IllegalStateException("Oathstead revive target should be the camp map.");
        }
        if (!state.world.isPassable(state.currentMapId, state.playerX, state.playerY)) {
            throw new IllegalStateException("Oathstead revive target is not walkable.");
        }
        if (state.world.cityBuildingAt(state.currentMapId, state.playerX, state.playerY) != null) {
            throw new IllegalStateException("Oathstead revive target is on a building.");
        }
    }

    private static void assertPlayerVillageEntry(GameState state, int dx, int dy, int expectedX, int expectedY) {
        WorldTransition transition = state.world.transitionAt(
                WorldMap.OVERWORLD_ID,
                WorldMap.START_POSITION.x() + dx,
                WorldMap.START_POSITION.y() + dy
        );
        if (transition == null || !WorldMap.PLAYER_VILLAGE_ID.equals(transition.targetMapId())
                || transition.targetX() != expectedX || transition.targetY() != expectedY) {
            throw new IllegalStateException(
                    "Expected player village entry to " + expectedX + "," + expectedY + " but got "
                            + (transition == null ? "none" : transition.targetMapId() + " "
                            + transition.targetX() + "," + transition.targetY()) + "."
            );
        }
    }

    private static void assertExit(
            GameState state,
            String mapId,
            int startX,
            int startY,
            int dx,
            int dy,
            int expectedX,
            int expectedY
    ) {
        state.currentMapId = mapId;
        state.playerX = startX;
        state.playerY = startY;
        state.mode = GameMode.EXPLORE;
        if (!state.move(dx, dy)) {
            throw new IllegalStateException("Expected " + mapId + " exit movement from " + startX + "," + startY + ".");
        }
        if (!WorldMap.OVERWORLD_ID.equals(state.currentMapId) || state.playerX != expectedX || state.playerY != expectedY) {
            throw new IllegalStateException(
                    "Expected " + mapId + " exit to " + expectedX + "," + expectedY
                            + " but got " + state.currentMapId + " " + state.playerX + "," + state.playerY + "."
            );
        }
    }

    private static void assertOverworldTraversal(WorldMap world) {
        int bridges = 0;
        int passes = 0;
        for (int y = 0; y < WorldMap.ROWS; y++) {
            for (int x = 0; x < WorldMap.COLS; x++) {
                char tile = world.rawOverworldTileAt(x, y);
                if (tile == 'B') {
                    bridges++;
                } else if (tile == 'q') {
                    passes++;
                }
            }
        }
        if (bridges < 6) {
            throw new IllegalStateException("Expected generated bridge crossings, found " + bridges + ".");
        }
        if (passes < 20) {
            throw new IllegalStateException("Expected traversible mountain passes, found " + passes + ".");
        }
        int longestBridgeSpan = longestBridgeSpan(world);
        if (longestBridgeSpan > 8) {
            throw new IllegalStateException("Expected bridge spans to be broken by landings, longest span was " + longestBridgeSpan + ".");
        }
        TilePoint[] anchors = {
                WorldMap.START_POSITION,
                new TilePoint(82, 105), new TilePoint(152, 145), new TilePoint(205, 78),
                new TilePoint(228, 185), new TilePoint(150, 230), new TilePoint(83, 62),
                new TilePoint(102, 245), new TilePoint(240, 153), new TilePoint(196, 62),
                new TilePoint(255, 177), new TilePoint(72, 218), new TilePoint(194, 235)
        };
        for (TilePoint target : anchors) {
            if (!canReach(world, WorldMap.START_POSITION, target)) {
                throw new IllegalStateException("Expected overworld route to " + target.x() + "," + target.y() + ".");
            }
        }
    }

    private static int longestBridgeSpan(WorldMap world) {
        int longest = 0;
        for (int y = 0; y < WorldMap.ROWS; y++) {
            int run = 0;
            for (int x = 0; x < WorldMap.COLS; x++) {
                if (world.rawOverworldTileAt(x, y) == 'B') {
                    run++;
                    longest = Math.max(longest, run);
                } else {
                    run = 0;
                }
            }
        }
        for (int x = 0; x < WorldMap.COLS; x++) {
            int run = 0;
            for (int y = 0; y < WorldMap.ROWS; y++) {
                if (world.rawOverworldTileAt(x, y) == 'B') {
                    run++;
                    longest = Math.max(longest, run);
                } else {
                    run = 0;
                }
            }
        }
        return longest;
    }

    private static void assertSettlementGateRoads(WorldMap world) {
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (WorldMap.SettlementSite settlement : world.settlementSites()) {
            for (int[] dir : dirs) {
                for (int distance = 0; distance <= 3; distance++) {
                    int x = settlement.x() + dir[0] * distance;
                    int y = settlement.y() + dir[1] * distance;
                    char tile = world.tileAt(WorldMap.OVERWORLD_ID, x, y);
                    if (!isOverworldRoad(tile)) {
                        throw new IllegalStateException("Settlement gate road disconnected for "
                                + settlement.label() + " at " + x + "," + y + ": " + Terrain.name(tile));
                    }
                }
            }
        }
    }

    private static void assertRoadMaterialsGenerated(WorldMap world) {
        assertNoSettlementRoadTiles(world, WorldMap.PLAYER_VILLAGE_ID);
        int oakhavenWorn = countTiles(world, "village_oakhaven", Terrain.VILLAGE_ROAD);
        int oakhavenRoad = countTiles(world, "village_oakhaven", Terrain.DIRT_ROAD);
        if (oakhavenWorn > 0 || oakhavenRoad < 60) {
            throw new IllegalStateException("Oakhaven should generate normal village roads without blocky worn road segments. "
                    + "Worn=" + oakhavenWorn + " dirt=" + oakhavenRoad + ".");
        }
        if (countTiles(world, "city_riverside", Terrain.COBBLESTONE_ROAD) < 60) {
            throw new IllegalStateException("Riverside did not generate cobblestone city roads.");
        }
        int overworldCobble = countTiles(world, WorldMap.OVERWORLD_ID, Terrain.COBBLESTONE_ROAD);
        int overworldRoad = countTiles(world, WorldMap.OVERWORLD_ID, Terrain.DIRT_ROAD);
        if (overworldCobble < 100 || overworldRoad < 100) {
            throw new IllegalStateException("Overworld city approach roads did not generate mixed cobblestone segments.");
        }
    }

    private static void assertOverworldDiscoverabilityProps(WorldMap world) {
        int settlementHooks = 0;
        for (WorldMap.SettlementSite settlement : world.settlementSites()) {
            if (hasNearbyDiscoverabilityProp(world, settlement.x(), settlement.y(), 10)) {
                settlementHooks++;
            }
        }
        if (settlementHooks < world.settlementSites().size()) {
            throw new IllegalStateException("Not every settlement received an overworld discoverability prop.");
        }

        int campHooks = 0;
        for (WorldMap.LocationSite camp : world.locationSites("goblin_camp")) {
            if (hasNearbyProp(world, camp.x(), camp.y(), 7, Set.of("location_camp_fire", "deco_imagen_road_camp"))) {
                campHooks++;
            }
        }
        if (campHooks < 4) {
            throw new IllegalStateException("Raider camps did not receive enough visible campfire or camp markers.");
        }

        boolean hasGlowHook = world.props(WorldMap.OVERWORLD_ID).stream()
                .anyMatch(prop -> prop.asset().contains("shrine") || prop.asset().contains("rune"));
        if (!hasGlowHook) {
            throw new IllegalStateException("Overworld discoverability pass did not add any shrine or rune glows.");
        }
    }

    private static boolean hasNearbyDiscoverabilityProp(WorldMap world, int x, int y, int radius) {
        for (WorldProp prop : world.propsInBounds(WorldMap.OVERWORLD_ID, x - radius, y - radius, x + radius + 1, y + radius + 1)) {
            if (Math.abs(prop.x() - x) + Math.abs(prop.y() - y) <= radius && isDiscoverabilityProp(prop.asset())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNearbyProp(WorldMap world, int x, int y, int radius, Set<String> assets) {
        for (WorldProp prop : world.propsInBounds(WorldMap.OVERWORLD_ID, x - radius, y - radius, x + radius + 1, y + radius + 1)) {
            if (Math.abs(prop.x() - x) + Math.abs(prop.y() - y) <= radius && assets.contains(prop.asset())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDiscoverabilityProp(String asset) {
        return asset != null && (asset.equals("deco_imagen_signpost")
                || asset.equals("deco_imagen_milestone")
                || asset.equals("deco_imagen_road_camp")
                || asset.equals("deco_imagen_shrine_stone")
                || asset.equals("deco_forest_shrine_stone")
                || asset.equals("deco_imagen_green_rune_stone")
                || asset.equals("deco_imagen_tundra_rune_stone")
                || asset.equals("deco_mountain_cairn")
                || asset.equals("deco_mountain_pass_way_cairn")
                || asset.equals("location_camp_fire")
                || asset.equals("location_ruin_standing_stones")
                || asset.equals("deco_tree_elder_harvestable"));
    }

    private static void assertTravelLogNarration(GameState state) {
        String road = TravelLogNarrator.narrate(state, "Road");
        if ("Road".equals(road) || road.isBlank()) {
            throw new IllegalStateException("Travel log did not narrate raw road terrain.");
        }
        String meadow = TravelLogNarrator.narrate(state, "Meadow");
        if ("Meadow".equals(meadow) || meadow.isBlank()) {
            throw new IllegalStateException("Travel log did not narrate raw meadow terrain.");
        }
        String leave = TravelLogNarrator.narrate(state, "You leave Oakhaven Village.");
        if (!leave.startsWith("You leave Oakhaven Village;")) {
            throw new IllegalStateException("Travel log did not narrate settlement exits.");
        }
        String ordinary = TravelLogNarrator.narrate(state, "Saved Test.");
        if (!"Saved Test.".equals(ordinary)) {
            throw new IllegalStateException("Travel log changed an ordinary status message.");
        }
    }

    private static void assertLandmarkDiscoveryLog(GameState state) {
        String oldMapId = state.currentMapId;
        int oldX = state.playerX;
        int oldY = state.playerY;
        try {
            WorldProp target = state.world.props(WorldMap.OVERWORLD_ID).stream()
                    .filter(prop -> isDiscoverabilityProp(prop.asset()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Expected at least one discoverable landmark prop."));
            state.currentMapId = WorldMap.OVERWORLD_ID;
            state.playerX = target.x();
            state.playerY = target.y();
            LandmarkDiscoveryLog log = new LandmarkDiscoveryLog();
            int nearbyDiscoverables = (int) state.world.propsInBounds(
                    WorldMap.OVERWORLD_ID,
                    target.x() - 4,
                    target.y() - 4,
                    target.x() + 5,
                    target.y() + 5
            ).stream()
                    .filter(prop -> isDiscoverabilityProp(prop.asset()))
                    .filter(prop -> Math.abs(prop.x() - target.x()) + Math.abs(prop.y() - target.y()) <= 4)
                    .count();
            String first = log.discover(state);
            if (first.isBlank() || first.equals(Terrain.name(state.world.tileAt(WorldMap.OVERWORLD_ID, target.x(), target.y())))) {
                throw new IllegalStateException("Landmark discovery did not emit narrative text.");
            }
            for (int i = 1; i < nearbyDiscoverables; i++) {
                if (log.discover(state).isBlank()) {
                    throw new IllegalStateException("Landmark discovery forgot a nearby landmark before exhausting the set.");
                }
            }
            if (!log.discover(state).isBlank()) {
                throw new IllegalStateException("Landmark discovery repeated an already discovered nearby set.");
            }
        } finally {
            state.currentMapId = oldMapId;
            state.playerX = oldX;
            state.playerY = oldY;
        }
    }

    private static void assertNoSettlementRoadTiles(WorldMap world, String mapId) {
        int roads = countTiles(world, mapId, Terrain.DIRT_ROAD)
                + countTiles(world, mapId, Terrain.VILLAGE_ROAD)
                + countTiles(world, mapId, Terrain.COBBLESTONE_ROAD);
        if (roads > 0) {
            throw new IllegalStateException(mapId + " should not generate settlement road-grid tiles.");
        }
    }

    private static int countTiles(WorldMap world, String mapId, char tile) {
        int count = 0;
        for (int y = 0; y < world.height(mapId); y++) {
            for (int x = 0; x < world.width(mapId); x++) {
                if (world.tileAt(mapId, x, y) == tile) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean isOverworldRoad(char tile) {
        return Terrain.connectingRoad(tile);
    }

    private static void assertMapAreaPropIndex() {
        MapArea area = new MapArea("index_test", "Index Test", "test", new char[][]{
                {'g', 'g', 'g'},
                {'g', 'g', 'g'},
                {'g', 'g', 'g'}
        });
        WorldProp first = new WorldProp(1, 1, "first", 16);
        WorldProp second = new WorldProp(1, 1, "second", 20);
        WorldProp third = new WorldProp(2, 1, "third", 24);
        area.addProp(first);
        area.props.add(second);
        area.props.add(third);
        if (!area.propAt(1, 1).equals(second) || area.propsAt(1, 1).size() != 2) {
            throw new IllegalStateException("MapArea prop index did not preserve tile ordering.");
        }
        if (area.propsInBounds(1, 1, 2, 2).size() != 2) {
            throw new IllegalStateException("MapArea bounded prop query missed indexed props.");
        }
        area.removeProp(second);
        if (!area.propAt(1, 1).equals(first) || area.propsAt(1, 1).size() != 1) {
            throw new IllegalStateException("MapArea prop index did not update after removal.");
        }
        WorldProp moved = area.moveProp(first, 0, 1);
        if (moved == null || area.propAt(1, 1) != null || !area.propAt(0, 1).equals(moved)) {
            throw new IllegalStateException("MapArea prop index did not update after move.");
        }
        area.props.set(0, new WorldProp(0, 0, "replacement", 18));
        if (area.propAt(1, 1) != null || area.propAt(0, 0) == null) {
            throw new IllegalStateException("MapArea prop index did not update after replacement.");
        }
        area.props.clear();
        if (!area.propsAt(0, 0).isEmpty() || !area.propsInBounds(0, 0, 3, 3).isEmpty()) {
            throw new IllegalStateException("MapArea prop index did not clear.");
        }
    }

    private static void assertOreNodesAreAccessibleAroundMountains(WorldMap world) {
        int oreNodes = 0;
        for (WorldProp prop : world.props(WorldMap.OVERWORLD_ID)) {
            if (!isOreSmokeAsset(prop.asset())) {
                continue;
            }
            oreNodes++;
            char tile = world.tileAt(WorldMap.OVERWORLD_ID, prop.x(), prop.y());
            if (!Terrain.passable(tile) || tile == 'm') {
                throw new IllegalStateException("Ore node generated on impassable terrain at "
                        + prop.x() + "," + prop.y() + " tile=" + tile);
            }
            int mountainDistance = distanceToOverworldTile(world, prop.x(), prop.y(), 3, 'm');
            if (tile != 'q' && mountainDistance > 2) {
                throw new IllegalStateException("Ore node generated too far from mountains at "
                        + prop.x() + "," + prop.y());
            }
        }
        if (oreNodes == 0) {
            throw new IllegalStateException("Expected at least one accessible ore node around mountains.");
        }
    }

    private static boolean isOreSmokeAsset(String asset) {
        return asset != null && asset.startsWith("deco_ore_");
    }

    private static int distanceToOverworldTile(WorldMap world, int x, int y, int radius, char wanted) {
        int best = radius + 1;
        for (int oy = -radius; oy <= radius; oy++) {
            for (int ox = -radius; ox <= radius; ox++) {
                int distance = Math.abs(ox) + Math.abs(oy);
                if (distance >= best) {
                    continue;
                }
                int tx = x + ox;
                int ty = y + oy;
                if (tx >= 0 && ty >= 0 && tx < WorldMap.COLS && ty < WorldMap.ROWS
                        && world.tileAt(WorldMap.OVERWORLD_ID, tx, ty) == wanted) {
                    best = distance;
                }
            }
        }
        return best;
    }

    private static void assertResourceNodeGathering(GameState state) {
        String previousMapId = state.currentMapId;
        int previousX = state.playerX;
        int previousY = state.playerY;
        state.currentMapId = WorldMap.OVERWORLD_ID;
        boolean checkedEmptyForest = false;
        for (int y = 1; y < state.world.height(WorldMap.OVERWORLD_ID) - 1 && !checkedEmptyForest; y++) {
            for (int x = 1; x < state.world.width(WorldMap.OVERWORLD_ID) - 1; x++) {
                if (state.world.tileAt(WorldMap.OVERWORLD_ID, x, y) == 'f'
                        && state.world.propAt(WorldMap.OVERWORLD_ID, x, y) == null) {
                    if (state.gatherTargetAtTile(x, y) != null) {
                        throw new IllegalStateException("Empty forest terrain should not be directly gatherable.");
                    }
                    checkedEmptyForest = true;
                    break;
                }
            }
        }
        if (!checkedEmptyForest) {
            throw new IllegalStateException("Expected an empty forest tile for gathering regression coverage.");
        }
        boolean checkedMushroom = false;
        for (WorldProp prop : state.world.props(WorldMap.OVERWORLD_ID)) {
            if (!prop.asset().toLowerCase().contains("mushroom")) {
                continue;
            }
            WorldProp topProp = state.world.propAt(WorldMap.OVERWORLD_ID, prop.x(), prop.y());
            if (topProp == null || !topProp.asset().equals(prop.asset())) {
                continue;
            }
            CraftingSystem.GatherCandidate candidate = state.gatherTargetAtTile(prop.x(), prop.y());
            if (candidate == null) {
                throw new IllegalStateException("Expected mushroom props to be gatherable.");
            }
            String label = candidate.label().toLowerCase();
            if (!label.contains("mushroom") || label.contains("ore")) {
                throw new IllegalStateException("Mushroom prop was misidentified as: " + candidate.label());
            }
            checkedMushroom = true;
            break;
        }
        if (!checkedMushroom) {
            throw new IllegalStateException("Expected at least one top-level mushroom prop for gathering regression coverage.");
        }
        WorldProp regularTree = null;
        int treePlayerX = 0;
        int treePlayerY = 0;
        for (WorldProp prop : state.world.props(WorldMap.OVERWORLD_ID)) {
            if (!isRegularTreeSmokeAsset(prop.asset())) {
                continue;
            }
            WorldProp topProp = state.world.propAt(WorldMap.OVERWORLD_ID, prop.x(), prop.y());
            if (topProp == null || !topProp.asset().equals(prop.asset())) {
                continue;
            }
            int[][] positions = {
                    {prop.x(), prop.y() - 1},
                    {prop.x() + 1, prop.y()},
                    {prop.x(), prop.y() + 1},
                    {prop.x() - 1, prop.y()}
            };
            for (int[] position : positions) {
                if (state.world.isPassable(WorldMap.OVERWORLD_ID, position[0], position[1])) {
                    regularTree = prop;
                    treePlayerX = position[0];
                    treePlayerY = position[1];
                    break;
                }
            }
            if (regularTree != null) {
                break;
            }
        }
        if (regularTree == null) {
            throw new IllegalStateException("Expected at least one gatherable regular tree prop in the overworld.");
        }
        CraftingSystem.GatherCandidate treeCandidate = state.gatherTargetAtTile(regularTree.x(), regularTree.y());
        if (treeCandidate == null || !treeCandidate.asset().equals(regularTree.asset())) {
            throw new IllegalStateException("Regular tree prop should be directly gatherable.");
        }
        state.playerX = treePlayerX;
        state.playerY = treePlayerY;
        state.player.addItem("stone_axe", 1);
        int treeInventoryBefore = state.player.inventory.values().stream().mapToInt(Integer::intValue).sum();
        state.gatherAtTile(regularTree.x(), regularTree.y());
        if (!state.crafting.active()) {
            throw new IllegalStateException("Regular tree did not start gathering: " + state.status);
        }
        while (state.crafting.active()) {
            state.tickWorld();
        }
        int treeInventoryAfter = state.player.inventory.values().stream().mapToInt(Integer::intValue).sum();
        String regularTreeAsset = regularTree.asset();
        boolean treeStillPresent = state.world.propsAt(WorldMap.OVERWORLD_ID, regularTree.x(), regularTree.y()).stream()
                .anyMatch(prop -> prop.asset().equals(regularTreeAsset));
        if (treeInventoryAfter <= treeInventoryBefore || treeStillPresent) {
            throw new IllegalStateException("Regular tree gathering should add materials and remove the tree prop.");
        }
        WorldProp target = null;
        int playerX = 0;
        int playerY = 0;
        for (WorldProp prop : state.world.props(WorldMap.OVERWORLD_ID)) {
            if (!CraftingSystem.isDepletableResourceNode(prop.asset())) {
                continue;
            }
            WorldProp topProp = state.world.propAt(WorldMap.OVERWORLD_ID, prop.x(), prop.y());
            if (topProp == null || !topProp.asset().equals(prop.asset())) {
                continue;
            }
            int[][] positions = {
                    {prop.x(), prop.y()},
                    {prop.x(), prop.y() - 1},
                    {prop.x() + 1, prop.y()},
                    {prop.x(), prop.y() + 1},
                    {prop.x() - 1, prop.y()}
            };
            for (int[] position : positions) {
                if (state.world.isPassable(WorldMap.OVERWORLD_ID, position[0], position[1])) {
                    CraftingSystem.GatherCandidate candidate = state.crafting.findGatherTarget(
                            state.world,
                            WorldMap.OVERWORLD_ID,
                            position[0],
                            position[1]
                    );
                    if (candidate != null
                            && candidate.tile().x() == prop.x()
                            && candidate.tile().y() == prop.y()
                            && candidate.asset().equals(prop.asset())) {
                        target = prop;
                        playerX = position[0];
                        playerY = position[1];
                        break;
                    }
                }
            }
            if (target != null) {
                break;
            }
        }
        if (target == null) {
            throw new IllegalStateException("Expected at least one gatherable random resource node in the overworld.");
        }
        state.currentMapId = WorldMap.OVERWORLD_ID;
        state.playerX = playerX;
        state.playerY = playerY;
        state.player.addItem("iron_pickaxe", 1);
        state.player.addItem("stone_axe", 1);
        int inventoryBefore = state.player.inventory.values().stream().mapToInt(Integer::intValue).sum();
        state.gatherNearby();
        if (!state.crafting.active()) {
            throw new IllegalStateException("Resource node did not start gathering: " + state.status);
        }
        while (state.crafting.active()) {
            state.tickWorld();
        }
        int inventoryAfter = state.player.inventory.values().stream().mapToInt(Integer::intValue).sum();
        if (inventoryAfter <= inventoryBefore) {
            throw new IllegalStateException("Resource node gathering did not add materials.");
        }
        String targetAsset = target.asset();
        boolean stillPresent = state.world.propsAt(WorldMap.OVERWORLD_ID, target.x(), target.y()).stream()
                .anyMatch(prop -> prop.asset().equals(targetAsset));
        if (stillPresent || state.harvestedResourceNodes.isEmpty()) {
            throw new IllegalStateException("Resource node was not depleted after gathering.");
        }
        state.currentMapId = WorldMap.OVERWORLD_ID;
        state.playerX = 82;
        state.playerY = 105;
        state.interact();
        boolean regenerated = state.world.propsAt(WorldMap.OVERWORLD_ID, target.x(), target.y()).stream()
                .anyMatch(prop -> prop.asset().equals(targetAsset));
        if (!state.currentMapId.equals("city_riverside") || !regenerated || !state.harvestedResourceNodes.isEmpty()) {
            throw new IllegalStateException("Resource nodes should regenerate when entering a settlement.");
        }
        state.currentMapId = previousMapId;
        state.playerX = previousX;
        state.playerY = previousY;
    }

    private static boolean isRegularTreeSmokeAsset(String asset) {
        return asset.equals("deco_tree_oak")
                || asset.equals("deco_tree_round")
                || asset.equals("deco_tree_pine")
                || asset.equals("deco_tree_blue_pine")
                || asset.equals("deco_tree_young")
                || asset.equals("deco_snow_pine")
                || asset.equals("deco_mountain_scrub_pine");
    }

    private static void assertPathfinder(GameState state) {
        TilePoint start = new TilePoint(state.playerX, state.playerY);
        TilePoint target = null;
        List<TilePoint> path = List.of();
        for (int y = 1; y < state.world.height(state.currentMapId) - 1 && target == null; y++) {
            for (int x = 1; x < state.world.width(state.currentMapId) - 1; x++) {
                if (Math.abs(x - start.x()) + Math.abs(y - start.y()) < 6 || !Pathfinder.walkable(state, x, y)) {
                    continue;
                }
                List<TilePoint> candidatePath = Pathfinder.findPath(state, start, new TilePoint(x, y));
                if (!candidatePath.isEmpty()) {
                    target = new TilePoint(x, y);
                    path = candidatePath;
                    break;
                }
            }
        }
        if (target == null) {
            throw new IllegalStateException("Pathfinder could not find a reachable smoke-test target.");
        }
        if (!path.get(path.size() - 1).equals(target)) {
            throw new IllegalStateException("Pathfinder path did not end at the requested target.");
        }
        TilePoint nearest = Pathfinder.nearestTarget(state, target.x(), target.y());
        if (!target.equals(nearest)) {
            throw new IllegalStateException("Pathfinder nearest target changed a walkable target.");
        }
        if (Pathfinder.findPath(state, start, new TilePoint(-1, -1)).size() != 0) {
            throw new IllegalStateException("Pathfinder returned a path to an out-of-bounds target.");
        }
    }

    private static void assertCleanVillageGround(WorldMap world, String mapId, boolean playerCamp) {
        int buildingBaseTiles = 0;
        int packedEarthTiles = 0;
        int plankWalkTiles = 0;
        for (int y = 0; y < world.height(mapId); y++) {
            for (int x = 0; x < world.width(mapId); x++) {
                char tile = world.tileAt(mapId, x, y);
                if (tile == 'h') {
                    buildingBaseTiles++;
                } else if (tile == 'V') {
                    packedEarthTiles++;
                } else if (tile == 'U') {
                    plankWalkTiles++;
                }
            }
        }
        if (buildingBaseTiles > 0) {
            throw new IllegalStateException(mapId + " should not stamp building background tiles.");
        }
        if (packedEarthTiles > 0) {
            throw new IllegalStateException(mapId + " should not use packed-earth slabs.");
        }
        if (playerCamp && plankWalkTiles > 0) {
            throw new IllegalStateException(mapId + " should not use random plank walkway strips.");
        }
    }

    private static void assertNoBuildingBaseTiles(WorldMap world, String mapId) {
        for (int y = 0; y < world.height(mapId); y++) {
            for (int x = 0; x < world.width(mapId); x++) {
                if (world.tileAt(mapId, x, y) == 'h') {
                    throw new IllegalStateException(mapId + " should not stamp building background tiles.");
                }
            }
        }
    }

    private static void assertTownPortalFastTravel(GameState state) {
        TilePoint portalPoint = state.world.townPortalPoint("village_oakhaven");
        if (portalPoint == null) {
            throw new IllegalStateException("Expected Oakhaven to have a town portal.");
        }
        assertTownPortalPlacement(state.world, "village_oakhaven", portalPoint);
        TilePoint portal = state.world.townPortalArrival("village_oakhaven");
        if (portal == null) {
            throw new IllegalStateException("Expected Oakhaven portal to have a walkable arrival tile.");
        }
        state.currentMapId = "village_oakhaven";
        state.playerX = portal.x();
        state.playerY = portal.y();
        state.player.gold = 100;
        state.interact();
        if (state.mode != GameMode.FAST_TRAVEL) {
            throw new IllegalStateException("Expected town portal interaction to open fast travel.");
        }
        List<GameState.FastTravelDestination> destinations = state.fastTravelDestinations();
        int targetIndex = -1;
        for (int i = 0; i < destinations.size(); i++) {
            if ("city_riverside".equals(destinations.get(i).mapId())) {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex < 0) {
            throw new IllegalStateException("Expected Riverside to be a portal destination.");
        }
        int cost = destinations.get(targetIndex).cost();
        state.fastTravelTo(targetIndex);
        if (!"city_riverside".equals(state.currentMapId)) {
            throw new IllegalStateException("Portal fast travel did not move to Riverside.");
        }
        if (state.player.gold != 100 - cost) {
            throw new IllegalStateException("Portal fast travel did not charge gold.");
        }
    }

    private static void assertCompanionNpcVisibility(GameState state) {
        for (Npc npc : GameData.NPCS) {
            if (npc.recruitId() == null || npc.recruitCost() > 0) {
                continue;
            }
            if (!state.world.hasMap(npc.mapId())) {
                throw new IllegalStateException("Recruitable NPC map does not exist: " + npc.name() + " -> " + npc.mapId());
            }
            GameState.NpcMotion motion = state.npcMotionsForMap(npc.mapId()).stream()
                    .filter(candidate -> candidate.npc().equals(npc))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Recruitable NPC is not listed on their map: " + npc.name()
                            + " -> " + npc.mapId()));
            if (!state.world.isPassable(npc.mapId(), motion.x(), motion.y())) {
                throw new IllegalStateException("Recruitable NPC spawned on blocked tile: " + npc.name()
                        + " at " + npc.mapId() + " " + motion.x() + "," + motion.y());
            }
        }
    }

    private static void assertSettlementWeeklyQuestCoverage(GameState state) {
        state.ensureWeeklyNpcQuests();
        for (WorldMap.SettlementSite settlement : state.world.settlementSites()) {
            if (WorldMap.PLAYER_VILLAGE_ID.equals(settlement.id())) {
                continue;
            }
            List<GameState.NpcMotion> motions = state.npcMotionsForMap(settlement.id());
            if (motions.isEmpty()) {
                throw new IllegalStateException("Settlement has no exterior NPCs: " + settlement.id());
            }
            for (GameState.NpcMotion motion : motions) {
                if (placeholderNpcName(motion.npc().name())) {
                    throw new IllegalStateException("Settlement generated placeholder NPC name: "
                            + motion.npc().name() + " in " + settlement.id());
                }
            }
            boolean hasQuestGiver = motions.stream().anyMatch(motion -> state.questForNpc(motion.npc()) != null);
            if (!hasQuestGiver) {
                throw new IllegalStateException("Settlement has no visible quest giver: " + settlement.id());
            }
        }
    }

    private static boolean placeholderNpcName(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        if (Set.of("Townsperson", "Householder", "Resident", "Townfolk").contains(name)) {
            return true;
        }
        return name.matches("(Street Guide|Local Clerk|Doorward|Town Crier|Guard) \\d+");
    }

    private static void acceptActiveQuestThroughDialogue(GameState state, String questId) {
        Quest quest = state.quests.get(questId);
        int guard = 0;
        while (state.mode == GameMode.DIALOG && quest != null && !quest.accepted && guard++ < 18) {
            List<String> options = state.activeNpcDialogOptions();
            int index = preferredQuestDialogueOption(options);
            if (index < 0) {
                throw new IllegalStateException("No dialogue option could lead to accepting " + questId + ": " + options);
            }
            state.selectDialogOption(index);
        }
    }

    private static int preferredQuestDialogueOption(List<String> options) {
        String[] priorities = {
                "I will handle it",
                "What work needs doing?",
                "What work do you do here?",
                "What exactly do you need?",
                "I will help",
                "I will follow",
                "I will stand",
                "I know the road",
                "I will show up",
                "Point me at",
                "Show me where",
                "How can I help?",
                "I need more context first."
        };
        for (String priority : priorities) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).equals(priority) || options.get(i).startsWith(priority)) {
                    return i;
                }
            }
        }
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).startsWith("What work do you do here?")) {
                return i;
            }
        }
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            if (option.startsWith("Tell me about ") && !option.startsWith("Tell me about yourself")) {
                return i;
            }
        }
        return options.isEmpty() ? -1 : 0;
    }

    private static void assertShopTransactions(GameState state) {
        if (state.mode != GameMode.SHOP || state.activeShop == null) {
            throw new IllegalStateException("Shop transaction test requires an active shop.");
        }
        List<String> stock = state.activeShop.availableStock(state.player.level);
        String itemKey = stock.stream()
                .filter(key -> !CraftingSystem.isRecipeBookItem(key))
                .filter(key -> GameData.itemCost(key) > 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Active shop has no purchasable inventory item."));
        int stockIndex = stock.indexOf(itemKey);
        int itemCost = GameData.itemCost(itemKey);
        int initialCount = state.player.inventory.getOrDefault(itemKey, 0);

        state.player.gold = itemCost - 1;
        state.buyShopItem(stockIndex);
        if (state.player.gold != itemCost - 1 || state.player.inventory.getOrDefault(itemKey, 0) != initialCount) {
            throw new IllegalStateException("Shop allowed purchase without enough gold for " + itemKey + ".");
        }

        int purchaseGold = itemCost + 17;
        state.player.gold = purchaseGold;
        state.buyShopItem(stockIndex);
        if (state.player.gold != purchaseGold - itemCost
                || state.player.inventory.getOrDefault(itemKey, 0) != initialCount + 1) {
            throw new IllegalStateException("Shop purchase did not debit gold and add exactly one " + itemKey + ".");
        }

        int sellIndex = state.player.inventory.keySet().stream().toList().indexOf(itemKey);
        if (sellIndex < 0) {
            throw new IllegalStateException("Purchased item was not available for sale: " + itemKey + ".");
        }
        int sellGold = state.player.gold;
        state.sellShopItem(sellIndex);
        int expectedSaleValue = Math.max(1, itemCost / 2);
        if (state.player.gold != sellGold + expectedSaleValue
                || state.player.inventory.getOrDefault(itemKey, 0) != initialCount) {
            throw new IllegalStateException("Shop sale did not refund half value and remove one " + itemKey + ".");
        }
    }

    private static void assertTownPortalPlacement(WorldMap world, String mapId, TilePoint portal) {
        for (int y = portal.y() - 1; y <= portal.y() + 1; y++) {
            for (int x = portal.x() - 1; x <= portal.x() + 1; x++) {
                if (!Terrain.passable(world.tileAt(mapId, x, y))) {
                    throw new IllegalStateException("Town portal footprint is not passable at " + mapId + ":" + x + "," + y);
                }
                if (world.cityBuildingAt(mapId, x, y) != null) {
                    throw new IllegalStateException("Town portal overlaps a building at " + mapId + ":" + x + "," + y);
                }
                if (world.transitionAt(mapId, x, y) != null) {
                    throw new IllegalStateException("Town portal overlaps a map transition at " + mapId + ":" + x + "," + y);
                }
            }
        }
    }

    private static boolean canReach(WorldMap world, TilePoint start, TilePoint target) {
        boolean[][] seen = new boolean[WorldMap.ROWS][WorldMap.COLS];
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        queue.add(start);
        seen[start.y()][start.x()] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            TilePoint current = queue.removeFirst();
            if (current.equals(target)) {
                return true;
            }
            for (int[] dir : dirs) {
                int nx = current.x() + dir[0];
                int ny = current.y() + dir[1];
                if (nx < 0 || ny < 0 || nx >= WorldMap.COLS || ny >= WorldMap.ROWS || seen[ny][nx] || !world.isPassable(nx, ny)) {
                    continue;
                }
                seen[ny][nx] = true;
                queue.addLast(new TilePoint(nx, ny));
            }
        }
        return false;
    }

    private static void assertInteriorNavigable(WorldMap world, String mapId) {
        TilePoint start = world.interiorEntryPoint(mapId);
        if (!world.isPassable(mapId, start.x(), start.y())) {
            throw new IllegalStateException("Interior entry is not passable at " + mapId + ":" + start.x() + "," + start.y());
        }
        boolean[][] seen = new boolean[world.height(mapId)][world.width(mapId)];
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        queue.add(start);
        seen[start.y()][start.x()] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            TilePoint current = queue.removeFirst();
            for (int[] dir : dirs) {
                int nx = current.x() + dir[0];
                int ny = current.y() + dir[1];
                if (nx < 0 || ny < 0 || nx >= world.width(mapId) || ny >= world.height(mapId)
                        || seen[ny][nx] || !world.isPassable(mapId, nx, ny)) {
                    continue;
                }
                seen[ny][nx] = true;
                queue.addLast(new TilePoint(nx, ny));
            }
        }
        for (int y = 1; y < world.height(mapId) - 1; y++) {
            for (int x = 1; x < world.width(mapId) - 1; x++) {
                if (world.isPassable(mapId, x, y) && !seen[y][x]) {
                    throw new IllegalStateException("Interior passable tile is unreachable at " + mapId + ":" + x + "," + y);
                }
            }
        }
    }
}
