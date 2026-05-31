package com.alderfall.game;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;

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
        Battle statusBattle = new Battle(state.player, GameData.MONSTERS.get("skeleton"), state.random);
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
        assertOverworldTraversal(state.world);
        if (!state.currentMapId.equals(WorldMap.PLAYER_VILLAGE_ID)) {
            throw new IllegalStateException("Expected new adventure to start in the player camp.");
        }
        if (!state.world.playerVillageBuildings().isEmpty()) {
            throw new IllegalStateException("Player camp should start without buildings.");
        }
        assertCleanVillageGround(state.world, WorldMap.PLAYER_VILLAGE_ID, true);
        assertCleanVillageGround(state.world, "village_oakhaven", false);
        assertNoBuildingBaseTiles(state.world, "city_riverside");
        state.currentMapId = "village_oakhaven";
        state.playerX = 13;
        state.playerY = 8;
        state.interact();
        if (state.mode != GameMode.DIALOG) {
            throw new IllegalStateException("Expected Oakhaven NPC dialog.");
        }
        while (state.mode == GameMode.DIALOG && !state.quests.get("bread_for_road").accepted) {
            state.advanceDialog();
        }
        if (!state.quests.get("bread_for_road").accepted) {
            throw new IllegalStateException("Quest accept button path failed.");
        }
        state.openActiveShop();
        if (state.mode != GameMode.SHOP) {
            throw new IllegalStateException("Expected Oakhaven shop.");
        }
        int potionsBefore = state.player.inventory.getOrDefault("potion_small", 0);
        state.buyShopItem(0);
        if (state.player.inventory.getOrDefault("potion_small", 0) <= potionsBefore) {
            throw new IllegalStateException("Shop purchase failed.");
        }
        state.closeOverlay();
        state.player.gold = 100;
        state.playerX = 16;
        state.playerY = 9;
        state.interact();
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
        state.closeOverlay();
        state.currentMapId = WorldMap.PLAYER_VILLAGE_ID;
        state.currentMapId = "city_riverside";
        state.playerX = 17;
        state.playerY = 11;
        state.interact();
        Quest quest = state.quests.get("slime_help");
        int dialogGuard = 0;
        while (state.mode == GameMode.DIALOG && quest != null && !quest.accepted && dialogGuard++ < 12) {
            state.advanceDialog();
        }
        if (quest == null || !quest.accepted) {
            throw new IllegalStateException("Quest acceptance failed.");
        }
        quest.progress = quest.needed;
        state.currentMapId = "city_riverside";
        state.playerX = 17;
        state.playerY = 11;
        state.closeOverlay();
        state.interact();
        dialogGuard = 0;
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
        assertSettlementExits(state, WorldMap.PLAYER_VILLAGE_ID, WorldMap.START_POSITION.x(), WorldMap.START_POSITION.y(), 14, 26, 10);
        assertSettlementExits(state, "village_oakhaven", WorldMap.OAKHAVEN_POSITION.x(), WorldMap.OAKHAVEN_POSITION.y(), 14, 26, 10);
        assertSettlementExits(state, "city_riverside", 82, 105, 17, 26, 12);
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
        state.allies.get(0).hp = 7;
        state.player.level = 6;
        state.player.xp = 11;
        state.player.gold = 123;
        state.player.attack = 17;
        state.player.defense = 5;
        SaveSystem saves = new SaveSystem(javaRoot);
        saves.save(state);
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
        assertExit(state, mapId, 34, horizontalGateY, 1, 0, overworldX + 3, overworldY);
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
}
