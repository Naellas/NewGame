package com.alderfall.game;

import java.nio.file.Path;

public final class SmokeTest {
    private SmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path javaRoot = Path.of("").toAbsolutePath().normalize();
        Path pythonRoot = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : javaRoot.getParent().resolve("Python").normalize();

        GameConfig config = GameConfig.load(pythonRoot);
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
        state.playerX = 8;
        state.playerY = 5;
        state.interact();
        if (state.currentMapId.startsWith("house_city_riverside")) {
            throw new IllegalStateException("Expected side walls not to enter city buildings.");
        }
        state.playerX = 5;
        state.playerY = 6;
        state.interact();
        if (!state.currentMapId.startsWith("house_city_riverside")) {
            throw new IllegalStateException("Expected generated house interior.");
        }
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
        if (loaded.allies.isEmpty() || !loaded.recruitedIds.contains("bran")) {
            throw new IllegalStateException("Recruit state did not round-trip.");
        }
        if (loaded.player.skillRank("channeling") != 1 || loaded.player.maxMp <= mpBeforeSkill) {
            throw new IllegalStateException("Skills did not round-trip.");
        }
        System.out.println("Smoke test passed. " + loaded.player.className + " at " + loaded.playerX + "," + loaded.playerY);
    }
}
