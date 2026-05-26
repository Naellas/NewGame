package com.alderfall.game;

import java.nio.file.Path;
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
        groupBattle.playerAttack();
        if (!groupBattle.finished || !groupBattle.victory || groupBattle.defeatedMonsterNames().size() != 2) {
            throw new IllegalStateException("Multi-opponent battle did not resolve all enemies.");
        }
        if (!state.world.isPassable(WorldMap.START_POSITION.x(), WorldMap.START_POSITION.y())) {
            throw new IllegalStateException("Start position is not passable.");
        }
        state.interact();
        if (!state.currentMapId.equals("village_oakhaven")) {
            throw new IllegalStateException("Expected transition into Oakhaven.");
        }
        state.playerX = 13;
        state.playerY = 8;
        state.interact();
        if (state.mode != GameMode.DIALOG) {
            throw new IllegalStateException("Expected Oakhaven NPC dialog.");
        }
        state.advanceDialog();
        state.advanceDialog();
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
        state.currentMapId = "city_riverside";
        state.playerX = 17;
        state.playerY = 11;
        state.interact();
        state.advanceDialog();
        state.advanceDialog();
        state.advanceDialog();
        Quest quest = state.quests.get("slime_help");
        if (quest == null || !quest.accepted) {
            throw new IllegalStateException("Quest acceptance failed.");
        }
        state.closeOverlay();
        state.currentMapId = WorldMap.OVERWORLD_ID;
        state.playerX = 82;
        state.playerY = 105;
        state.interact();
        if (!state.currentMapId.equals("city_riverside")) {
            throw new IllegalStateException("Expected transition into Riverside.");
        }
        assertSettlementExits(state, "village_oakhaven", 112, 158, 14, 26, 10);
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
        if (!crown.ready()) {
            throw new IllegalStateException("Goblin King battle did not advance quest progress.");
        }
        System.out.println("Smoke test passed. " + loaded.player.className + " at " + loaded.playerX + "," + loaded.playerY);
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
}
