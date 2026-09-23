package com.alderfall.game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class CreativeCraftingTest {
    public static void main(String[] args) throws Exception {
        GameConfig config = GameConfig.load(Path.of("."));
        require(!config.creativeCraftingMode, "Creative crafting defaults off");
        Path testRoot = Files.createTempDirectory(Path.of("../temp"), "creative-crafting-");
        config.creativeCraftingMode = true; config.save(testRoot);
        require(GameConfig.loadWithSettings(testRoot).creativeCraftingMode, "Setting persists");
        config.creativeCraftingMode = false; config.save(testRoot);
        require(!GameConfig.loadWithSettings(testRoot).creativeCraftingMode, "Off setting persists");
        GameState state = new GameState(config);
        state.chooseClass("Knight"); state.mode = GameMode.CRAFTING;
        state.player.inventory.clear(); state.player.addItem("copper_ingot", 1);
        var beforeXp = Map.copyOf(state.player.professionXp);
        int characterXp = state.player.xp;
        var blade = AssemblyCrafting.componentRecipe(state.player, AssemblyCrafting.Slot.BLADE, "mithril_ingot");
        state.craftComponent(AssemblyCrafting.Slot.BLADE, "mithril_ingot");
        require(!state.player.hasItem(blade.resultKey()), "Normal crafting retains restrictions");
        config.creativeCraftingMode = true;
        state.craftComponent(AssemblyCrafting.Slot.BLADE, "mithril_ingot");
        require(state.player.hasItem(blade.resultKey()) && !state.crafting.active(), "Creative component is instant despite missing skill/material/station");
        var hilt = AssemblyCrafting.componentRecipe(state.player, AssemblyCrafting.Slot.HILT, "wood");
        state.craftComponent(AssemblyCrafting.Slot.HILT, "wood");
        var parts = Map.of(AssemblyCrafting.Slot.BLADE, blade.resultKey(), AssemblyCrafting.Slot.HILT, hilt.resultKey());
        var recipe = AssemblyCrafting.assemblyRecipe(state.player, AssemblyCrafting.blueprint("sword"), parts);
        state.assembleEquipment(AssemblyCrafting.blueprint("sword"), parts);
        require(state.player.hasItem(recipe.resultKey()), "Creative output matches real quality preview");
        require(state.player.hasItem(blade.resultKey()) && state.player.hasItem(hilt.resultKey())
                && state.player.inventory.get("copper_ingot") == 1, "No resources or components consumed");
        require(beforeXp.equals(state.player.professionXp) && state.player.xp == characterXp, "No XP awarded");
        require(state.mode == GameMode.CRAFTING, "Workshop stays open for testing");
        require(state.learnedCraftingRecipes().size() == CraftingSystem.RECIPES.size(), "All legacy recipes available for testing");
        state.craftRecipe("process_obsidian");
        require(state.player.hasItem("obsidian_glass"), "Legacy/refining recipes support creative crafting");
        config.creativeCraftingMode = false;
        state.craftComponent(AssemblyCrafting.Slot.BLADE, "mithril_ingot");
        require(state.player.inventory.get(blade.resultKey()) == 1, "Turning off restores restrictions");
        System.out.println("Creative crafting passed: settings persistence, all crafting paths, zero costs/XP and normal-mode restoration.");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
