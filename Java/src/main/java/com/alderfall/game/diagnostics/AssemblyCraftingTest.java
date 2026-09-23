package com.alderfall.game;

import com.alderfall.game.inventory.Equipment;
import com.alderfall.game.inventory.ItemRarity;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import static com.alderfall.game.AssemblyCrafting.Slot.*;
import static com.alderfall.game.AssemblyCrafting.Quality.*;

public final class AssemblyCraftingTest {
    public static void main(String[] args) throws Exception {
        Actor novice = actor(1), master = actor(10);
        Equipment copper = sword(novice, "copper_ingot", STANDARD, null);
        Equipment mithril = sword(novice, "mithril_ingot", STANDARD, null);
        require(mithril.attackBonus() > copper.attackBonus(), "Material strength must survive identical assembly");
        require(mithril.minLevel() > copper.minLevel(), "Advanced material equip requirement");
        Equipment legendary = sword(master, "copper_ingot", LEGENDARY, null);
        require(legendary.rarity() == ItemRarity.LEGENDARY && legendary.attackBonus() > copper.attackBonus(), "Skill upgrades rarity and stats");
        require(sword(master, "copper_ingot", STANDARD, null).name().startsWith(STANDARD.label), "Weak parts limit craftsmanship");
        require(sword(novice, "copper_ingot", LEGENDARY, null).name().startsWith(STANDARD.label), "Assembly skill limits craftsmanship");
        require(sword(master, "copper_ingot", MASTERWORK, null).name().startsWith("Masterwork"), "Masterwork retained");
        Equipment venom = sword(novice, "copper_ingot", STANDARD, part(ORNAMENT, "venom_sac", STANDARD));
        require(venom.critChanceBonus() > copper.critChanceBonus(), "Monster drop contributes real combat stats");
        require(AssemblyCrafting.componentRecipe(novice, BLADE, "wood") == null, "Reject wrong material family");
        require(AssemblyCrafting.componentRecipe(novice, BLADE, "copper_ore") == null, "Refine ore before component work");
        require(AssemblyCrafting.assemblyRecipe(master, AssemblyCrafting.blueprint("staff"),
                Map.of(SHAFT, part(SHAFT, "wood", STANDARD))) == null, "Staff requires gem");
        require(AssemblyCrafting.assemblyRecipe(master, AssemblyCrafting.blueprint("sword"),
                Map.of(BLADE, part(SHAFT, "wood", STANDARD), HILT, part(HILT, "wood", STANDARD))) == null, "Wrong component slot rejected");

        CraftingSystem crafting = new CraftingSystem();
        CraftingSystem.Recipe locked = AssemblyCrafting.componentRecipe(novice, BLADE, "mithril_ingot");
        novice.addItem("mithril_ingot", 3);
        Map<String, Integer> before = Map.copyOf(novice.inventory);
        crafting.beginCraft(novice, locked);
        require(!crafting.active() && novice.inventory.equals(before), "Skill failure must consume nothing");
        CraftingSystem.Recipe blade = AssemblyCrafting.componentRecipe(master, BLADE, "copper_ingot");
        crafting.beginCraft(master, blade);
        require(!crafting.active(), "Missing material rejection");
        master.addItem("copper_ingot", 3);
        int xp = master.professionXp.get(Profession.SMITHING.id());
        master.skillAllocations.put("masterwork_fittings", 1);
        require(crafting.beginCraft(master, blade).startsWith("Crafting"), "Start component work");
        require(!master.hasItem("copper_ingot") && !master.hasItem(blade.resultKey()), "Consume on start, output on completion");
        crafting.beginCraft(master, blade);
        finish(crafting, master);
        require(master.inventory.get(blade.resultKey()) == 1, "No duplicate components from busy calls or batch bonuses");
        require(master.professionXp.get(Profession.SMITHING.id()) > xp, "Smithing XP earned");
        String hilt = part(HILT, "wood", LEGENDARY);
        master.addItem(hilt, 1);
        CraftingSystem.Recipe assembly = AssemblyCrafting.assemblyRecipe(master, AssemblyCrafting.blueprint("sword"), Map.of(BLADE, blade.resultKey(), HILT, hilt));
        require(crafting.beginCraft(master, assembly).startsWith("Crafting"), "Start assembly");
        finish(crafting, master);
        require(master.inventory.get(assembly.resultKey()) == 1 && !master.hasItem(hilt) && !master.hasItem(blade.resultKey()), "Assembly consumes exactly its selected parts");
        require(GameData.equipment(assembly.resultKey()).equals(legendary), "Preview equals completed output");
        master.equipment.put("weapon", assembly.resultKey());
        master.addItem(part(RUNE, "ember_shard", RARE), 2);
        roundTrip(master);

        for (AssemblyCrafting.Blueprint b : AssemblyCrafting.BLUEPRINTS) {
            Map<AssemblyCrafting.Slot, String> parts = new EnumMap<>(AssemblyCrafting.Slot.class);
            for (AssemblyCrafting.Slot slot : b.slots()) if (!slot.optional())
                parts.put(slot, part(slot, AssemblyCrafting.materials(slot).get(0).key(), FINE));
            CraftingSystem.Recipe recipe = AssemblyCrafting.assemblyRecipe(master, b, parts);
            require(recipe != null && GameData.equipment(recipe.resultKey()) != null, "Blueprint coverage: " + b.id());
        }
        for (String equipmentSlot : GameData.EQUIPMENT_SLOTS)
            require(AssemblyCrafting.BLUEPRINTS.stream().anyMatch(b -> b.equipmentSlot().equals(equipmentSlot)),
                    "Crafting must cover equipment slot: " + equipmentSlot);
        for (Profession p : Profession.ALL) require(!SkillTrees.professionSkillTree(p.id()).isEmpty(), "Profession tree: " + p.id());
        for (MaterialCatalog.Material m : MaterialCatalog.all()) {
            require(CraftingSystem.CRAFTING_ITEMS.containsKey(m.key()), "Material must exist in game: " + m.key());
            require(!m.stats().summary().isEmpty(), "Material properties: " + m.key());
        }
        for (String invalid : List.of("gear1~sword", "gear1~sword~INVALID~x", "gear1~sword~LEGENDARY~copper_ingot~STANDARD+wood~STANDARD+-+-", "part1~BLADE~bad~STANDARD")) {
            require(GameData.equipment(invalid) == null && AssemblyCrafting.component(invalid) == null, "Malformed key rejection");
        }
        System.out.println("Assembly crafting passed: material stats, quality, slots, costs, XP, all blueprints and save round-trip.");
        System.out.println("Copper ATK " + copper.attackBonus() + "; mithril ATK " + mithril.attackBonus()
                + "; legendary copper ATK " + legendary.attackBonus());
    }
    private static Equipment sword(Actor actor, String metal, AssemblyCrafting.Quality q, String ornament) {
        Map<AssemblyCrafting.Slot, String> parts = new EnumMap<>(AssemblyCrafting.Slot.class);
        parts.put(BLADE, part(BLADE, metal, q)); parts.put(HILT, part(HILT, "wood", q));
        if (ornament != null) parts.put(ORNAMENT, ornament);
        return GameData.equipment(AssemblyCrafting.assemblyRecipe(actor, AssemblyCrafting.blueprint("sword"), parts).resultKey());
    }
    private static String part(AssemblyCrafting.Slot slot, String material, AssemblyCrafting.Quality q) {
        return new AssemblyCrafting.Component(slot, MaterialCatalog.get(material), q).key();
    }
    private static Actor actor(int level) {
        Actor actor = new Actor("Crafter", "player", "Warrior", 100, 30, 8, 5);
        actor.inventory.clear();
        for (Profession p : Profession.ALL) actor.professionXp.put(p.id(), Profession.xpForLevel(level));
        return actor;
    }
    private static void finish(CraftingSystem crafting, Actor actor) {
        for (int tick = 0; tick < 1000 && crafting.active(); tick++) crafting.tick(actor);
        require(!crafting.active(), "Timed task completion");
    }
    private static void roundTrip(Actor source) throws Exception {
        SaveSystem save = new SaveSystem(Path.of("out", "assembly-test"));
        Actor loaded = actor(1);
        for (String field : List.of("Inventory", "Equipment", "ProfessionXp")) {
            Method write = SaveSystem.class.getDeclaredMethod("write" + field, Actor.class);
            Method read = SaveSystem.class.getDeclaredMethod("read" + field, String.class, Actor.class);
            write.setAccessible(true); read.setAccessible(true);
            read.invoke(save, (String) write.invoke(save, source), loaded);
        }
        require(source.inventory.equals(loaded.inventory), "Saved material and component identity");
        require(source.equipment.equals(loaded.equipment), "Saved equipment identity");
        require(source.professionXp.equals(loaded.professionXp), "Saved specialist experience");
        require(GameData.equipment(loaded.equipment.get("weapon")).equals(GameData.equipment(source.equipment.get("weapon"))), "Saved quality and stats");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
