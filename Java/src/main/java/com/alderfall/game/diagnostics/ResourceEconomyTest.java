package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.Path;
import java.util.*;

/** Resource placement, real gathering output, and refining integration checks. */
public final class ResourceEconomyTest {
    public static void main(String[] args) {
        boolean checkArt = !List.of(args).contains("--skip-art");
        AssetStore assets = new AssetStore(Path.of("assets"));
        Map<String, Character> regional = Map.of("bog_iron", 'v', "froststeel", 'n',
                "sunmetal", 's', "emberite", 'b', "verdant", 'f');
        Map<String, String> drops = new LinkedHashMap<>();
        for (String metal : List.of("iron", "copper", "tin", "silver", "gold", "mithril", "cobalt", "adamantite")) {
            drops.put("deco_ore_" + metal + "_vein", metal + "_ore");
        }
        regional.keySet().forEach(m -> drops.put("deco_ore_" + m + "_vein", m + "_ore"));
        drops.put("deco_ore_obsidian_vein", "obsidian_shard");
        drops.put("deco_ore_amber_vein", "raw_amber");
        drops.put("deco_ore_rock_salt_vein", "rock_salt");
        drops.put("deco_tree_cypress_harvestable", "cypress_wood");
        drops.put("deco_ore_coal_deposit", "coal");
        drops.put("deco_ore_crystal_vein", "crystal_dust");
        drops.put("deco_ore_steel_scrap", "steel_scrap");
        for (String wood : List.of("oak", "birch", "pine", "willow", "maple", "ash", "elder")) {
            drops.put("deco_tree_" + wood + "_harvestable", wood + "_wood");
        }
        drops.put("deco_tree_magical_harvestable", "magic_wood");
        drops.put("deco_forest_ancient_roots", "ancient_wood");
        drops.put("deco_beach_palm", "palm_wood");
        drops.put("deco_beach_palm_cluster", "palm_wood");
        drops.put("deco_tree_enchanted_stump", "enchanted_bark");
        drops.put("deco_tree_glowing_root_cluster", "glowroot");
        drops.put("deco_tree_deadwood_harvestable", "deadwood");
        drops.put("deco_tree_fruit_harvestable", "fruitwood");
        drops.put("deco_wood_ironwood_log_pile", "ironwood");
        for (var entry : drops.entrySet()) {
            if (checkArt) require(assets.hasSprite(entry.getKey()), "Missing node art: " + entry.getKey());
            require(CraftingSystem.isDepletableResourceNode(entry.getKey()), "Not depletable: " + entry.getKey());
            for (int seed = 0; seed < 12; seed++) {
                Actor actor = actor();
                CraftingSystem crafting = new CraftingSystem();
                crafting.beginGather(actor, List.of(), new CraftingSystem.GatherCandidate("test",
                        new TilePoint(0, 0), 'P', entry.getKey()), new Random(seed));
                finish(crafting, actor);
                require(crafting.lastCompletedOutput().getOrDefault(entry.getValue(), 0) > 0,
                        "Incorrect drop: " + entry);
                for (String item : crafting.lastCompletedOutput().keySet()) {
                    require(CraftingSystem.CRAFTING_ITEMS.containsKey(item), "Unknown drop: " + item);
                }
            }
        }
        Set<String> recipeKeys = new HashSet<>();
        for (CraftingSystem.Recipe recipe : CraftingSystem.RECIPES) {
            require(recipeKeys.add(recipe.key()), "Duplicate recipe: " + recipe.key());
            if (!(recipe.key().startsWith("smelt_") || recipe.key().startsWith("forge_")
                    || recipe.key().startsWith("saw_") || recipe.key().equals("ancient_staff")
                    || recipe.key().startsWith("process_") || recipe.key().startsWith("resource_")
                    || recipe.key().endsWith("_refined")
                    || recipe.key().equals("bronze_pickaxe") || recipe.key().equals("recycle_steel"))) continue;
            Actor actor = actor();
            for (Profession profession : Profession.ALL) actor.professionXp.put(profession.id(), 100000);
            // Exercise late-game recipes with a trained artisan, without batch-output bonuses.
            actor.skillAllocations.put("trade_foundations", 2);
            actor.skillAllocations.put("master_of_trades", 2);
            actor.skillAllocations.put("artisan_path", 2);
            recipe.cost().forEach((key, amount) -> {
                require(knownItem(key), "Unknown recipe ingredient: " + key);
                actor.addItem(key, amount);
            });
            require(knownItem(recipe.resultKey()), "Unknown recipe result: " + recipe.resultKey());
            CraftingSystem crafting = new CraftingSystem();
            require(crafting.beginCraft(actor, recipe).startsWith("Crafting"), "Cannot craft " + recipe.key());
            finish(crafting, actor);
            require(actor.inventory.getOrDefault(recipe.resultKey(), 0) == recipe.resultAmount(),
                    "Incorrect craft output amount: " + recipe.key());
            recipe.cost().keySet().stream().filter(k -> !k.equals(recipe.resultKey())).forEach(k ->
                    require(!actor.hasItem(k), "Ingredient not consumed: " + recipe.key() + ": " + k));
        }
        for (var entry : CraftingSystem.CRAFTING_ITEMS.entrySet()) {
            if (checkArt) require(assets.hasSprite(entry.getValue().icon()), "Missing icon: " + entry.getKey());
            if (entry.getKey().endsWith("_ingot")) require(CraftingSystem.RECIPES.stream()
                    .anyMatch(r -> r.cost().containsKey(entry.getKey())), "Unused ingot: " + entry.getKey());
        }
        for (String key : List.of("obsidian_shard", "obsidian_glass", "raw_amber", "polished_amber",
                "rock_salt", "refined_salt", "cypress_wood")) {
            require(!CraftingSystem.recipesUsingIngredient(key).isEmpty(), "No discoverable use: " + key);
        }
        Map<String, Integer> counts = new TreeMap<>();
        for (long seed : new long[]{0, 42, -17}) {
            WorldMap world = new WorldMap(seed);
            CraftingSystem crafting = new CraftingSystem();
            for (WorldProp prop : world.props(WorldMap.OVERWORLD_ID)) {
                if (prop.asset().equals("deco_tree_cypress_harvestable")) {
                    require(world.tileAt(WorldMap.OVERWORLD_ID, prop.x(), prop.y()) == 'v', "Cypress outside marsh");
                    require(nearWater(world, prop), "Cypress too far from marsh water");
                    counts.merge(prop.asset(), 1, Integer::sum);
                }
                if (!prop.asset().startsWith("deco_ore_")) continue;
                char tile = world.tileAt(WorldMap.OVERWORLD_ID, prop.x(), prop.y());
                require(Terrain.passable(tile), "Inaccessible ore: " + prop);
                CraftingSystem.GatherCandidate target = crafting.findGatherTargetAt(world,
                        WorldMap.OVERWORLD_ID, prop.x(), prop.y());
                require(target != null && target.asset().equals(prop.asset()),
                        "Ore cannot be targeted or is obscured by another resource: " + prop);
                counts.merge(prop.asset(), 1, Integer::sum);
                String metal = prop.asset().substring(9).replace("_vein", "");
                if (regional.containsKey(metal)) require(tile == regional.get(metal), "Wrong biome: " + prop);
                else if (metal.equals("obsidian")) require(tile == 'b', "Obsidian outside badlands");
                else if (metal.equals("amber")) require(tile == 'f', "Amber outside forest");
                else if (metal.equals("rock_salt")) require(tile == 'P' || tile == 's', "Salt outside coast/desert");
                else require(tile == 'q' || nearMountain(world, prop), "Mountain ore outside foothills: " + prop);
            }
            System.out.println("Resource placement passed for seed " + seed);
        }
        drops.keySet().stream().filter(k -> k.startsWith("deco_ore_")).forEach(k ->
                require(counts.getOrDefault(k, 0) > 0, "Ore never generated: " + k));
        require(counts.getOrDefault("deco_tree_cypress_harvestable", 0) > 0, "Cypress never generated");
        System.out.println("Resource economy passed: " + counts);
    }

    private static boolean nearMountain(WorldMap world, WorldProp prop) {
        for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
            if (Math.abs(dx) + Math.abs(dy) <= 2
                    && world.tileAt(WorldMap.OVERWORLD_ID, prop.x() + dx, prop.y() + dy) == 'm') return true;
        }
        return false;
    }

    private static boolean nearWater(WorldMap world, WorldProp prop) {
        for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
            char tile = world.tileAt(WorldMap.OVERWORLD_ID, prop.x() + dx, prop.y() + dy);
            if (Math.abs(dx) + Math.abs(dy) <= 2 && (tile == 'w' || tile == '~')) return true;
        }
        return false;
    }

    private static boolean knownItem(String key) {
        return CraftingSystem.CRAFTING_ITEMS.containsKey(key) || GameData.ITEMS.containsKey(key)
                || GameData.equipment(key) != null;
    }

    private static Actor actor() { return new Actor("Tester", "player", "Warrior", 100, 30, 8, 5); }
    private static void finish(CraftingSystem crafting, Actor actor) {
        for (int tick = 0; tick < 1000; tick++) crafting.tick(actor);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
