package com.alderfall.game;

import java.nio.file.Path;
import java.util.*;

/** Checks actual loot, recipe execution, assembly compatibility and atlas rendering. */
public final class TextileMaterialsTest {
    public static void main(String[] args) {
        Actor actor = new Actor("Textile tester", "player", "Warrior", 100, 30, 8, 5);
        for (Profession profession : Profession.values())
            actor.professionXp.put(profession.id(), Profession.xpForLevel(10));
        for (String mob : List.of("spider", "bramble_boar", "swamp_troll", "frost_troll")) {
            actor.inventory.clear();
            require(GameData.MONSTERS.containsKey(mob), "World monster exists: " + mob);
            CraftingSystem.grantLoot(actor, List.of(GameData.MONSTERS.get(mob)), new Random(42));
            require(actor.hasItem(mob.equals("spider") ? "spider_silk" : "thick_hide"), "Actual loot: " + mob);
        }
        AssetStore assets = new AssetStore(Path.of("assets"));
        Set<Integer> spriteHashes = new HashSet<>();
        for (String icon : TextileMaterialSprites.NAMES) {
            String key = icon.substring("material_".length());
            var material = MaterialCatalog.get(key);
            require(material != null && !material.stats().summary().isEmpty(), "Stats: " + key);
            var slot = material.family() == MaterialCatalog.Family.CLOTH
                    ? AssemblyCrafting.Slot.FABRIC : AssemblyCrafting.Slot.LEATHER;
            require(AssemblyCrafting.componentRecipe(actor, slot, key) != null, "Assembly: " + key);
            require(assets.hasSprite(icon), "Sprite: " + key);
            var sprite = assets.sprite(icon, 64);
            int[] pixels = sprite.getRGB(0, 0, 64, 64, null, 0, 64);
            require(Arrays.stream(pixels).anyMatch(p -> (p >>> 24) == 0), "Transparent background: " + key);
            require(Arrays.stream(pixels).filter(p -> (p >>> 24) > 128).count() > 200, "Visible sprite: " + key);
            require(spriteHashes.add(Arrays.hashCode(pixels)), "Distinct art: " + key);
            if (key.equals("spider_silk") || key.equals("thick_hide")) continue;
            var recipe = CraftingSystem.RECIPES.stream().filter(r -> r.resultKey().equals(key)).findFirst().orElseThrow();
            String profession = material.family() == MaterialCatalog.Family.CLOTH
                    ? Profession.TAILORING.id() : Profession.LEATHERWORKING.id();
            require(recipe.profession().equals(profession), "Profession: " + key);
            require(recipe.professionRequirements().get(profession) == material.requiredSkill(), "Tier gate: " + key);
            actor.inventory.clear();
            for (var ingredient : recipe.cost().entrySet()) {
                require(MaterialCatalog.get(ingredient.getKey()) != null, "Registered ingredient");
                require(CraftingSystem.recipesUsingIngredient(ingredient.getKey()).contains(recipe), "Discoverable recipe");
                actor.addItem(ingredient.getKey(), ingredient.getValue());
            }
            int xp = actor.professionXp.get(profession);
            CraftingSystem crafting = new CraftingSystem();
            require(crafting.beginCraft(actor, recipe).startsWith("Crafting"), "Begin: " + key);
            for (int tick = 0; tick < 1000 && crafting.active(); tick++) crafting.tick(actor);
            require(!crafting.active() && actor.hasItem(key), "Finished material: " + key);
            require(actor.professionXp.get(profession) > xp, "Profession XP: " + key);
            for (String ingredient : recipe.cost().keySet())
                require(!actor.hasItem(ingredient), "Ingredient consumed: " + ingredient);
        }
        System.out.println("Textile materials passed: real drops, ten refinement recipes, profession XP/gates, assembly and twelve unique transparent sprites.");
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
