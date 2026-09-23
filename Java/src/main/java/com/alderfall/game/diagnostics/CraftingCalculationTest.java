package com.alderfall.game;

import java.util.List;
import java.util.Map;
import static com.alderfall.game.AssemblyCrafting.Quality.*;
import static com.alderfall.game.AssemblyCrafting.Slot.*;

public final class CraftingCalculationTest {
    public static void main(String[] args) {
        Actor actor = new Actor("Artisan", "player", "Warrior", 100, 30, 8, 5);
        for (Profession p : Profession.ALL) actor.professionXp.put(p.id(), Profession.xpForLevel(4));
        actor.strength = 20; actor.constitution = 10;
        var smith = CraftingCalculation.calculate(actor, Profession.SMITHING, 1);
        require(smith.score() == 56 && smith.quality() == MASTERWORK, "Smithing: 40 + 12 + 4 = 56");
        require(CraftingCalculation.calculate(actor, Profession.ARMORCRAFT, 1).score() == 56, "Armorcraft uses STR/CON");
        actor.intelligence = 100; actor.dexterity = 100;
        require(CraftingCalculation.calculate(actor, Profession.SMITHING, 1).score() == 56, "Smith ignores unrelated attributes");
        var difficult = CraftingCalculation.calculate(actor, Profession.SMITHING, 3);
        require(difficult.score() == 48 && difficult.quality() == FINE, "Tier difficulty changes quality: 56 - 8 = 48");
        actor.strength = 1; actor.constitution = 1;
        require(CraftingCalculation.calculate(actor, Profession.SMITHING, 1).quality() == FINE, "Attributes can cross quality thresholds");
        actor.skillAllocations.put("smithing_training", 1);
        require(CraftingCalculation.calculate(actor, Profession.SMITHING, 1).score() == 51, "Training adds ten score per effective level");
        actor.skillAllocations.clear();
        actor.dexterity = 20; actor.intelligence = 10; actor.strength = 10; actor.constitution = 10;
        for (Profession p : List.of(Profession.CARPENTRY, Profession.TAILORING, Profession.JEWELLERY, Profession.LEATHERWORKING))
            require(CraftingCalculation.calculate(actor, p, 1).score() == 56, "Trade-specific attributes: " + p);
        actor.intelligence = 20; actor.willpower = 10;
        require(CraftingCalculation.calculate(actor, Profession.ALCHEMY, 1).score() == 56, "Alchemy uses INT/WIL");
        actor.intelligence = 10; actor.willpower = 10;
        actor.professionXp.put(Profession.ALCHEMY.id(), Profession.xpForLevel(2));
        require(CraftingCalculation.calculate(actor, Profession.ALCHEMY, 1).quality() == FINE, "Exact 30-point threshold included");
        actor.willpower = 9;
        require(CraftingCalculation.calculate(actor, Profession.ALCHEMY, 1).quality() == STANDARD, "29.6 does not round up into higher quality");
        actor.strength = 100; actor.constitution = 100;
        actor.professionXp.put(Profession.SMITHING.id(), 0);
        var capped = CraftingCalculation.calculate(actor, Profession.SMITHING, 1);
        require(capped.score() == 40 && capped.aptitude().bonus() == 30, "Attribute contribution capped");
        require(!CraftingSystem.meetsProfessionRequirements(actor, AssemblyCrafting.componentRecipe(actor, BLADE, "mithril_ingot")), "Attributes cannot bypass profession requirements");

        for (String key : List.of("obsidian_shard", "obsidian_glass", "raw_amber", "polished_amber", "rock_salt", "refined_salt", "cypress_wood"))
            require(MaterialCatalog.get(key) != null && !MaterialCatalog.get(key).stats().summary().isEmpty(), "New material properties: " + key);
        Map<AssemblyCrafting.Slot, String> usable = Map.of(BLADE, "obsidian_glass", SHAFT, "cypress_wood", GEM, "polished_amber", RUNE, "refined_salt");
        for (var entry : usable.entrySet()) {
            var recipe = AssemblyCrafting.componentRecipe(actor, entry.getKey(), entry.getValue());
            require(recipe != null, "New material in component recipe: " + entry);
            require(AssemblyCrafting.materials(entry.getKey()).stream().anyMatch(m -> m.key().equals(entry.getValue())), "New material in picker");
            require(AssemblyCrafting.component(recipe.resultKey()).quality() == CraftingCalculation.calculate(actor,
                    entry.getKey().profession, MaterialCatalog.get(entry.getValue()).tier()).quality(), "Preview formula equals output");
        }
        require(AssemblyCrafting.componentRecipe(actor, GEM, "raw_amber") == null, "Amber must be polished");
        require(AssemblyCrafting.componentRecipe(actor, RUNE, "rock_salt") == null, "Salt must be refined");
        require(AssemblyCrafting.componentRecipe(actor, BLADE, "obsidian_shard") == null, "Obsidian blade must be honed");
        for (String key : CraftingSystem.CRAFTING_ITEMS.keySet()) {
            boolean product = key.startsWith("stone_") && !key.equals("stone") || key.startsWith("village_prop_")
                    || key.equals("iron_pickaxe") || key.equals("shell_lure");
            if (!product) require(MaterialCatalog.get(key) != null, "Unregistered crafting material: " + key);
        }
        for (String process : List.of("process_obsidian", "process_amber", "process_salt")) {
            var recipe = CraftingSystem.recipeByKey(process);
            require(recipe != null && MaterialCatalog.get(recipe.resultKey()) != null, "Refinement path exists: " + process);
        }
        String legacy = "gear1~sword~MASTERWORK~copper_ingot~MASTERWORK+wood~MASTERWORK+-+-";
        require(GameData.equipment(legacy) != null, "Existing saved quality remains readable");
        System.out.println("Crafting calculation passed: job attributes, exact scores, thresholds, caps, material coverage and refinement.");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
