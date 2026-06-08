package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import com.alderfall.game.inventory.Equipment;
import com.alderfall.game.inventory.Item;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class CraftingSystem {
    private static final int[][] GATHER_DIRECTIONS = {
            {0, -1}, {1, 0}, {0, 1}, {-1, 0}, {0, 0}
    };

    public enum Workstation {
        ANVIL("Anvil"),
        CARPENTER("Carpenter's Table"),
        OVEN("Cooking Station"),
        ALCHEMY("Alchemy Station");

        private final String label;

        Workstation(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum RecipeCategory {
        CONSUMABLE("Consumables"),
        WEAPON("Weapons"),
        ARMOR("Armor"),
        TOOL("Tools"),
        SEED("Seeds"),
        ACCESSORY("Accessories"),
        MATERIAL("Materials"),
        DECOR("Decor");

        private final String label;

        RecipeCategory(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record ItemInfo(String name, String icon, String detail) {
    }

    public record Recipe(
            String key,
            String name,
            Workstation workstation,
            Map<String, Integer> cost,
            String resultKey,
            int resultAmount,
            int ticks,
            String profession,
            Map<String, Integer> professionRequirements,
            RecipeCategory category,
            String description
    ) {
    }

    public record GatherCandidate(String label, TilePoint tile, char terrain, String asset) {
        public GatherCandidate(String label, TilePoint tile, char terrain) {
            this(label, tile, terrain, "");
        }
    }

    private Map<String, Integer> lastCompletedOutput = Map.of();

    public static final Map<String, ItemInfo> CRAFTING_ITEMS = Map.ofEntries(
            Map.entry("wood", new ItemInfo("Wood", "material_wood", "Crafting ingredient from chopped trees.")),
            Map.entry("oak_wood", new ItemInfo("Oak Wood", "material_oak_wood", "Dense shield-ready timber from oak trees.")),
            Map.entry("birch_wood", new ItemInfo("Birch Wood", "material_birch_wood", "Pale flexible wood used for light handles and bows.")),
            Map.entry("pine_wood", new ItemInfo("Pine Wood", "material_pine_wood", "Resinous softwood for practical camp and tool work.")),
            Map.entry("willow_wood", new ItemInfo("Willow Wood", "material_willow_wood", "Supple wood valued for bows and binding frames.")),
            Map.entry("maple_wood", new ItemInfo("Maple Wood", "material_maple_wood", "Hard smooth-grained wood for fine hafts and staves.")),
            Map.entry("ash_wood", new ItemInfo("Ash Wood", "material_ash_wood", "Springy wood favored for bows, spears, and staff cores.")),
            Map.entry("elder_wood", new ItemInfo("Elder Wood", "material_elder_wood", "Old spell-friendly wood from elder trees.")),
            Map.entry("magic_wood", new ItemInfo("Magic Wood", "material_magic_wood", "Blue-glowing timber from enchanted trees.")),
            Map.entry("deadwood", new ItemInfo("Deadwood", "material_deadwood", "Dry brittle wood that chars into useful forge fuel.")),
            Map.entry("fruitwood", new ItemInfo("Fruitwood", "material_fruitwood", "Sweet-smelling wood from orchard trees.")),
            Map.entry("ironwood", new ItemInfo("Ironwood", "material_ironwood", "Heavy wood tough enough for armor plates.")),
            Map.entry("enchanted_bark", new ItemInfo("Enchanted Bark", "material_enchanted_bark", "Living bark peeled from old magical stumps.")),
            Map.entry("glowroot", new ItemInfo("Glowroot", "material_glowroot", "Luminous root fiber used in advanced alchemy and warding.")),
            Map.entry("stone", new ItemInfo("Stone", "material_stone", "Crafting ingredient chipped from mountain rock.")),
            Map.entry("iron_ore", new ItemInfo("Iron Ore", "material_iron_ore", "Metalworking ingredient mined from mountain seams.")),
            Map.entry("copper_ore", new ItemInfo("Copper Ore", "material_copper_ore", "Conductive ore used in fittings, rings, and storm gear.")),
            Map.entry("tin_ore", new ItemInfo("Tin Ore", "material_tin_ore", "Soft pale ore used in alloy work.")),
            Map.entry("silver_ore", new ItemInfo("Silver Ore", "material_silver_ore", "Bright ore prized for charms and warded weapons.")),
            Map.entry("gold_ore", new ItemInfo("Gold Ore", "material_gold_ore", "Soft precious ore for fine medallions and fittings.")),
            Map.entry("mithril_ore", new ItemInfo("Mithril Ore", "material_mithril_ore", "Light blue ore used in high-tier blades and armor.")),
            Map.entry("cobalt_ore", new ItemInfo("Cobalt Ore", "material_cobalt_ore", "Deep blue ore for cold and storm-tempered forgework.")),
            Map.entry("adamantite_ore", new ItemInfo("Adamantite Ore", "material_adamantite_ore", "Rare purple ore for near-mythic equipment.")),
            Map.entry("steel_scrap", new ItemInfo("Steel Scrap", "material_steel_scrap", "Recoverable steel from old battlefield deposits.")),
            Map.entry("wild_meat", new ItemInfo("Wild Meat", "material_wild_meat", "Cooking ingredient from hunted animals.")),
            Map.entry("wool", new ItemInfo("Wool", "material_wool", "Soft fiber from sheep and goats.")),
            Map.entry("skin", new ItemInfo("Skin", "material_skin", "Supple hide used for leatherwork.")),
            Map.entry("horn", new ItemInfo("Horn", "material_horn", "Hard animal horn for handles and fittings.")),
            Map.entry("scale", new ItemInfo("Scale", "material_scale", "Tough scale used in armor and charms.")),
            Map.entry("venom_sac", new ItemInfo("Venom Sac", "material_venom_sac", "Potent monster ingredient.")),
            Map.entry("bone", new ItemInfo("Bone", "material_bone", "Sturdy bone used in rough crafting.")),
            Map.entry("ember_shard", new ItemInfo("Ember Shard", "material_ember_shard", "Hot monster shard used in forgework.")),
            Map.entry("frost_shard", new ItemInfo("Frost Shard", "material_frost_shard", "Cold crystal from mountain creatures.")),
            Map.entry("herb_seed", new ItemInfo("Herb Seeds", "material_herb_seed", "Seeds for small planter pots and garden plots.")),
            Map.entry("herb_leaf", new ItemInfo("Herb Leaf", "material_herb_leaf", "Fresh herb used in cooking and alchemy.")),
            Map.entry("flower_blossom", new ItemInfo("Flower Blossom", "material_flower_blossom", "A fragrant reagent for calming tinctures.")),
            Map.entry("garden_vegetables", new ItemInfo("Garden Vegetables", "material_garden_vegetables", "Fresh produce from tended planters.")),
            Map.entry("flint", new ItemInfo("Flint", "material_flint", "Sharp stone for simple field tools.")),
            Map.entry("plant_fiber", new ItemInfo("Plant Fiber", "material_plant_fiber", "Tough cordage from grasses, reeds, and bark.")),
            Map.entry("clay", new ItemInfo("Clay", "material_clay", "Workable earth used in cooking and alchemy vessels.")),
            Map.entry("coal", new ItemInfo("Coal", "material_coal", "Hot-burning fuel for forgework and field fires.")),
            Map.entry("crystal_dust", new ItemInfo("Crystal Dust", "material_crystal_dust", "Powdered crystal used in refined recipes.")),
            Map.entry("stone_memory", new ItemInfo("Stone of Memory", "icon_potion_blue", "An old socket stone recovered from the sealed vault beneath Archive City. Its surface shifts with faint lines of script no one remembers writing.")),
            Map.entry("stone_iron", new ItemInfo("Stone of Iron", "icon_shield", "A heavy socket stone taken from Kharvok the Banner-Bound. It smells faintly of cold steel and battlefield smoke.")),
            Map.entry("stone_ember", new ItemInfo("Stone of Ember", "icon_potion_red", "A socket stone shaped through Sanctum ember rites. It remains warm even in darkness.")),
            Map.entry("stone_tides", new ItemInfo("Stone of Tides", "material_seashell", "A wet, dark stone from the depths of Miredepth Cave. If held near the ear, it sounds like a bell ringing underwater.")),
            Map.entry("stone_hunger", new ItemInfo("Stone of Hunger", "icon_sword", "A mud-caked socket stone recovered from goblin scavengers. It feels hollow, as if something inside it is waiting to be fed.")),
            Map.entry("stone_roots", new ItemInfo("Stone of Roots", "material_glowroot", "A green-black stone pulled from a corrupted orchard ward. Tiny root patterns move beneath its surface.")),
            Map.entry("stone_graves", new ItemInfo("Stone of Graves", "icon_potion_blue", "A pale socket stone from Stonegate Crypt. Names appear briefly on its surface, then fade before they can be read.")),
            Map.entry("stone_frost", new ItemInfo("Stone of Frost", "icon_potion_blue", "A blue-white socket stone found in the Hailback Broodmother's nest. Frost gathers on any container that holds it.")),
            Map.entry("stone_bells", new ItemInfo("Stone of Bells", "icon_chest", "A quiet stone hidden beneath an old bell foundation. Nearby metal hums softly in its presence.")),
            Map.entry("stone_ash", new ItemInfo("Stone of Ash", "icon_potion_red", "A blackened socket stone dropped by Sareth, the Cinder Knife. It leaves soot on the hand but never grows smaller.")),
            Map.entry("stone_oaths", new ItemInfo("Stone of Oaths", "icon_chest", "A socket stone formed at Oathstead's restored oath marker. It is plain compared to the others, but it feels steady in the hand.")),
            Map.entry("stone_dawn", new ItemInfo("Stone of Dawn", "icon_potion_red", "A socket stone formed from five oath fragments, one from each kingdom. At sunrise, it catches light even underground.")),
            Map.entry("mushroom_spores", new ItemInfo("Mushroom Spores", "material_mushroom_spores", "Fungal reagent gathered from forest and marsh growth.")),
            Map.entry("coconut", new ItemInfo("Coconut", "material_coconut", "Beach forage used for travel food and fresh water.")),
            Map.entry("raw_fish", new ItemInfo("Raw Fish", "material_wild_meat", "Fresh catch ready for a cooking fire.")),
            Map.entry("seashell", new ItemInfo("Seashell", "material_seashell", "Shoreline shell used for charms and coastal craft.")),
            Map.entry("palm_frond", new ItemInfo("Palm Frond", "material_palm_frond", "Broad beach leaf used for shade, thatch, and woven props.")),
            Map.entry("shell_lure", new ItemInfo("Shell Lure", "material_seashell", "A small shell charm used for coastal barter and later fishing work.")),
            Map.entry("village_prop_palm_shade", new ItemInfo("Palm Shade", "item_palm_shade", "Crafted beach prop for village decoration.")),
            Map.entry("village_prop_net_drying_rack", new ItemInfo("Net Drying Rack", "item_net_drying_rack", "Crafted beach prop for village decoration.")),
            Map.entry("stone_axe", new ItemInfo("Stone Axe", "icon_sword", "Tool: improves tree chopping before proper metal tools.")),
            Map.entry("stone_pickaxe", new ItemInfo("Stone Pickaxe", "icon_sword", "Tool: improves mountain mining.")),
            Map.entry("iron_pickaxe", new ItemInfo("Iron Pickaxe", "icon_sword", "Tool: greatly improves mountain mining."))
    );

    public static final Map<String, String> RECIPE_BOOKS = Map.ofEntries(
            Map.entry("recipe_book_camp_cookery", "coconut_rations"),
            Map.entry("recipe_book_fisher_knots", "shell_lure"),
            Map.entry("recipe_book_iron_blades", "iron_sword"),
            Map.entry("recipe_book_iron_mail", "iron_mail"),
            Map.entry("recipe_book_steel_plate", "steel_plate"),
            Map.entry("recipe_book_oak_bow", "oak_bow"),
            Map.entry("recipe_book_apothecary_salve", "herbal_salve"),
            Map.entry("recipe_book_escape_scrolls", "escape_scroll")
    );

    private static final Set<String> STARTER_RECIPE_KEYS = Set.of(
            "stone_axe",
            "stone_pickaxe",
            "seed_bundle",
            "campfire_coal"
    );

    public static final List<Recipe> RECIPES = List.of(
            recipe("stone_axe", "Stone Axe", null,
                    Map.of("stone", 2, "flint", 1, "plant_fiber", 1), "stone_axe", 1, 60,
                    "A crude field axe tied from stone and fiber."),
            recipe("stone_pickaxe", "Stone Pickaxe", null,
                    Map.of("stone", 3, "flint", 1, "plant_fiber", 1), "stone_pickaxe", 1, 75,
                    "A starter mining tool that can be made in the field."),
            recipe("seed_bundle", "Seed Bundle", null,
                    Map.of("herb_seed", 2, "plant_fiber", 1), "herb_seed", 4, 45,
                    "Sort gathered seeds into a better planting bundle."),
            recipe("campfire_coal", "Campfire Coal", null,
                    Map.of("wood", 2, "flint", 1), "coal", 1, 70,
                    "Char wood into rough field fuel."),
            recipe("deadwood_charcoal", "Deadwood Charcoal", null,
                    Map.of("deadwood", 2, "flint", 1), "coal", 3, 65,
                    Profession.WOODCUTTING.id(), Map.of(Profession.WOODCUTTING.id(), 1),
                    "Burn dry deadwood down into hotter forge fuel."),
            recipe("tin_copper_alloy", "Tin-Copper Alloy", Workstation.ANVIL,
                    Map.of("copper_ore", 2, "tin_ore", 1, "coal", 1), "steel_scrap", 1, 90,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 2, Profession.MINING.id(), 2),
                    "Work soft ores into repairable metal stock for simple fittings."),
            recipe("coconut_rations", "Coconut Rations", null,
                    Map.of("coconut", 2, "plant_fiber", 1), "trail_rations", 1, 80,
                    Profession.COOKING.id(), Map.of(Profession.COOKING.id(), 2, Profession.SURVIVAL.id(), 2),
                    "Pack coconut meat into travel food for hot shoreline roads."),
            recipe("cooked_fish", "Cooked Fish", Workstation.OVEN,
                    Map.of("raw_fish", 1), "cooked_meat", 1, 70,
                    Profession.COOKING.id(), Map.of(Profession.COOKING.id(), 1),
                    "A simple pan-fried catch that restores health."),
            recipe("shell_lure", "Shell Lure", null,
                    Map.of("seashell", 2, "plant_fiber", 1), "shell_lure", 1, 45,
                    Profession.WEAVING.id(), Map.of(Profession.FISHING.id(), 1, Profession.WEAVING.id(), 1),
                    "String shells into a tidy lure bundle for coastal barter and crafting."),
            recipe("palm_shade", "Palm Shade", Workstation.CARPENTER,
                    Map.of("palm_frond", 4, "wood", 2, "plant_fiber", 2), "village_prop_palm_shade", 1, 120,
                    Profession.WEAVING.id(), Map.of(Profession.WEAVING.id(), 2, Profession.WOODCUTTING.id(), 2),
                    "A woven shade canopy for a beach-style village corner."),
            recipe("net_drying_rack", "Net Drying Rack", Workstation.CARPENTER,
                    Map.of("wood", 3, "plant_fiber", 4, "seashell", 1), "village_prop_net_drying_rack", 1, 130,
                    Profession.WEAVING.id(), Map.of(Profession.WEAVING.id(), 2, Profession.FISHING.id(), 2),
                    "A simple rack for nets, ropes, and seaside supplies."),
            recipe("oaken_roundshield", "Oaken Roundshield", Workstation.CARPENTER,
                    Map.of("oak_wood", 4, "skin", 1, "plant_fiber", 2), "oaken_roundshield", 1, 120,
                    Profession.CRAFTING.id(), Map.of(Profession.WOODCUTTING.id(), 2, Profession.CRAFTING.id(), 2),
                    "A wooden shield bound with hide."),
            recipe("oak_bow", "Oak Bow", Workstation.CARPENTER,
                    Map.of("oak_wood", 5, "wool", 1, "plant_fiber", 2), "oak_bow", 1, 130,
                    Profession.WEAVING.id(), Map.of(Profession.WOODCUTTING.id(), 2, Profession.WEAVING.id(), 2),
                    "A simple bow strung with twisted wool cord."),
            recipe("ash_bow", "Ash Bow", Workstation.CARPENTER,
                    Map.of("ash_wood", 5, "willow_wood", 1, "plant_fiber", 2), "ash_bow", 1, 150,
                    Profession.WEAVING.id(), Map.of(Profession.WOODCUTTING.id(), 3, Profession.WEAVING.id(), 3),
                    "A springy bow carved from ash and backed with willow."),
            recipe("oakheart_staff", "Oakheart Staff", Workstation.CARPENTER,
                    Map.of("oak_wood", 3, "elder_wood", 1, "crystal_dust", 1), "oakheart_staff", 1, 125,
                    Profession.CRAFTING.id(), Map.of(Profession.WOODCUTTING.id(), 2, Profession.CRAFTING.id(), 2),
                    "A sturdy staff with an elderwood focus point."),
            recipe("ashwind_staff", "Ashwind Staff", Workstation.CARPENTER,
                    Map.of("ash_wood", 3, "birch_wood", 2, "plant_fiber", 2), "ashwind_staff", 1, 145,
                    Profession.CRAFTING.id(), Map.of(Profession.WOODCUTTING.id(), 3, Profession.CRAFTING.id(), 3),
                    "Light ash wood shaped into a quick spell focus."),
            recipe("frostroot_staff", "Frostroot Staff", Workstation.CARPENTER,
                    Map.of("glowroot", 2, "willow_wood", 2, "frost_shard", 1), "frostroot_staff", 1, 170,
                    Profession.CRAFTING.id(), Map.of(Profession.WOODCUTTING.id(), 4, Profession.CRAFTING.id(), 4),
                    "A pale root-bound focus that keeps a chill in the grain."),
            recipe("stormcall_staff", "Stormcall Staff", Workstation.CARPENTER,
                    Map.of("magic_wood", 3, "cobalt_ore", 2, "copper_ore", 2), "stormcall_staff", 1, 195,
                    Profession.CRAFTING.id(), Map.of(Profession.WOODCUTTING.id(), 5, Profession.MINING.id(), 4),
                    "A forked staff wired with copper and cobalt seams."),
            recipe("woodcutter_axe", "Woodcutter Axe", Workstation.ANVIL,
                    Map.of("wood", 2, "iron_ore", 2, "coal", 1), "woodcutter_axe", 1, 120,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 2, Profession.MINING.id(), 2),
                    "A practical axe that speeds tree chopping."),
            recipe("copper_signet", "Copper Signet", Workstation.ANVIL,
                    Map.of("copper_ore", 3, "coal", 1), "copper_signet", 1, 90,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 2, Profession.MINING.id(), 1),
                    "A simple ring stamped from heated copper."),
            recipe("iron_pickaxe", "Iron Pickaxe", Workstation.ANVIL,
                    Map.of("wood", 2, "iron_ore", 3, "coal", 1), "iron_pickaxe", 1, 140,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 3, Profession.MINING.id(), 2),
                    "A sturdy pickaxe for mining richer mountain seams."),
            recipe("iron_sword", "Iron Sword", Workstation.ANVIL,
                    Map.of("iron_ore", 4, "skin", 1, "coal", 2), "iron_sword", 1, 150,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 3, Profession.MINING.id(), 2),
                    "A dependable forged blade."),
            recipe("iron_mail", "Iron Mail", Workstation.ANVIL,
                    Map.of("iron_ore", 7, "skin", 2, "plant_fiber", 2, "coal", 2), "iron_mail", 1, 180,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 3, Profession.LEATHERWORKING.id(), 2),
                    "Protective mail rings over leather backing."),
            recipe("iron_kite_shield", "Iron Kite Shield", Workstation.ANVIL,
                    Map.of("iron_ore", 5, "oak_wood", 2, "coal", 2), "iron_kite_shield", 1, 180,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 4, Profession.MINING.id(), 3),
                    "A pointed iron shield built over a solid oak back."),
            recipe("steel_sword", "Steel Sword", Workstation.ANVIL,
                    Map.of("steel_scrap", 3, "iron_ore", 3, "coal", 3, "maple_wood", 1), "steel_sword", 1, 190,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 4, Profession.MINING.id(), 4),
                    "A sharper blade from recovered steel and fresh iron."),
            recipe("steel_plate", "Steel Plate", Workstation.ANVIL,
                    Map.of("steel_scrap", 5, "iron_ore", 6, "coal", 4, "skin", 2), "steel_plate", 1, 230,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 5, Profession.MINING.id(), 4),
                    "Heavy steel armor built around leather straps."),
            recipe("steel_greaves", "Steel Greaves", Workstation.ANVIL,
                    Map.of("steel_scrap", 3, "iron_ore", 4, "coal", 2, "skin", 1), "steel_greaves", 1, 185,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 4, Profession.MINING.id(), 4),
                    "Leg armor with a dependable steel bite."),
            recipe("silver_mace", "Silver Mace", Workstation.ANVIL,
                    Map.of("silver_ore", 3, "iron_ore", 3, "coal", 2, "maple_wood", 1), "silver_mace", 1, 190,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 4, Profession.MINING.id(), 4),
                    "A polished mace useful against warded foes."),
            recipe("steelcleaver", "Steelcleaver", Workstation.ANVIL,
                    Map.of("steel_scrap", 4, "iron_ore", 3, "coal", 3, "ash_wood", 1), "steelcleaver", 1, 210,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 5, Profession.MINING.id(), 4),
                    "A wide axe blade made for splitting armor."),
            recipe("stormguard_plate", "Stormguard Plate", Workstation.ANVIL,
                    Map.of("cobalt_ore", 4, "copper_ore", 4, "steel_scrap", 4, "coal", 3), "stormguard_plate", 1, 260,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 7, Profession.MINING.id(), 6),
                    "Heavy armor grounded with copper and cobalt channels."),
            recipe("frostvein_sword", "Frostvein Sword", Workstation.ANVIL,
                    Map.of("cobalt_ore", 3, "silver_ore", 2, "frost_shard", 2, "coal", 2), "frostvein_sword", 1, 230,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 6, Profession.MINING.id(), 5),
                    "Cold metal with pale blue seams."),
            recipe("starforged_sword", "Starforged Sword", Workstation.ANVIL,
                    Map.of("mithril_ore", 4, "adamantite_ore", 2, "silver_ore", 3, "magic_wood", 1), "starforged_sword", 1, 320,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 10, Profession.MINING.id(), 9),
                    "A silver star-metal blade with a singing edge."),
            recipe("starforged_plate", "Starforged Plate", Workstation.ANVIL,
                    Map.of("mithril_ore", 6, "adamantite_ore", 4, "cobalt_ore", 5, "glowroot", 2), "starforged_plate", 1, 390,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 12, Profession.MINING.id(), 10),
                    "A near-mythic suit of fallen star-metal."),
            recipe("heartwood_vest", "Heartwood Vest", Workstation.CARPENTER,
                    Map.of("ironwood", 4, "enchanted_bark", 3, "magic_wood", 2, "skin", 2), "heartwood_vest", 1, 280,
                    Profession.CRAFTING.id(), Map.of(Profession.WOODCUTTING.id(), 8, Profession.CRAFTING.id(), 7),
                    "Living wood plates over supple leather."),
            recipe("frostguard_aegis", "Frostguard Aegis", Workstation.ANVIL,
                    Map.of("iron_ore", 5, "frost_shard", 2, "skin", 1, "crystal_dust", 1), "frostguard_aegis", 1, 210,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 4, Profession.MINING.id(), 3),
                    "A northern shield chilled with shards from frostland hunts."),
            recipe("emberward_shield", "Emberward Shield", Workstation.ANVIL,
                    Map.of("iron_ore", 5, "ember_shard", 2, "scale", 1, "coal", 2), "emberward_shield", 1, 220,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 4, Profession.LEATHERWORKING.id(), 3),
                    "Heat-tempered warding gear from badlands monster glass."),
            recipe("marshreed_ward", "Marshreed Ward", Workstation.ANVIL,
                    Map.of("wood", 3, "scale", 2, "venom_sac", 1, "plant_fiber", 3), "marshreed_ward", 1, 230,
                    Profession.WEAVING.id(), Map.of(Profession.WEAVING.id(), 4, Profession.LEATHERWORKING.id(), 3),
                    "A damp-proof ward made from fen scales and reed binding."),
            recipe("thorncarver", "Thorncarver", Workstation.CARPENTER,
                    Map.of("wood", 4, "venom_sac", 2, "horn", 1, "flint", 1), "thorncarver", 1, 190,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 3, Profession.WOODCUTTING.id(), 3),
                    "A hooked briar blade built from forest monster trophies."),
            recipe("bonebound_shield", "Bonebound Shield", Workstation.CARPENTER,
                    Map.of("wood", 4, "bone", 3, "skin", 2, "plant_fiber", 2), "bonebound_shield", 1, 220,
                    Profession.LEATHERWORKING.id(), Map.of(Profession.LEATHERWORKING.id(), 3, Profession.CRAFTING.id(), 2),
                    "A grim ward lashed from dungeon bone and cured hide."),
            recipe("cooked_meat", "Cooked Meat", Workstation.OVEN,
                    Map.of("wild_meat", 1), "cooked_meat", 1, 80,
                    Profession.COOKING.id(), Map.of(Profession.COOKING.id(), 1),
                    "Simple food that restores health."),
            recipe("trail_rations", "Trail Rations", Workstation.OVEN,
                    Map.of("wild_meat", 2, "wool", 1), "trail_rations", 2, 110,
                    Profession.COOKING.id(), Map.of(Profession.COOKING.id(), 2, Profession.WEAVING.id(), 1),
                    "Packed food for rough travel."),
            recipe("vegetable_stew", "Vegetable Stew", Workstation.OVEN,
                    Map.of("garden_vegetables", 2, "herb_leaf", 1), "vegetable_stew", 1, 95,
                    Profession.COOKING.id(), Map.of(Profession.COOKING.id(), 2, Profession.SURVIVAL.id(), 2),
                    "A warm herb stew that restores health and a little focus."),
            recipe("herbed_rations", "Herbed Rations", Workstation.OVEN,
                    Map.of("wild_meat", 1, "garden_vegetables", 1, "herb_leaf", 1), "trail_rations", 2, 115,
                    Profession.COOKING.id(), Map.of(Profession.COOKING.id(), 3, Profession.SURVIVAL.id(), 2),
                    "Better travel food made from fresh garden ingredients."),
            recipe("herbal_salve", "Herbal Salve", Workstation.ALCHEMY,
                    Map.of("herb_leaf", 2, "flower_blossom", 1, "clay", 1), "herbal_salve", 1, 90,
                    "A gentle healing salve brewed from garden herbs."),
            recipe("focus_tea", "Focus Tea", Workstation.ALCHEMY,
                    Map.of("herb_leaf", 1, "flower_blossom", 2, "mushroom_spores", 1), "focus_tea", 1, 85,
                    "A calming infusion that restores a modest amount of MP."),
            recipe("escape_scroll", "Escape Scroll", Workstation.ALCHEMY,
                    Map.of("plant_fiber", 2, "crystal_dust", 1, "glowroot", 1), "escape_scroll", 1, 100,
                    "A one-use dungeon recall scroll that returns the reader to the surface."),
            recipe("potent_tonic", "Potent Tonic", Workstation.ALCHEMY,
                    Map.of("herb_leaf", 2, "venom_sac", 1, "crystal_dust", 1), "potion_large", 1, 130,
                    "A stronger healing draught refined at an alchemy station."),
            recipe("upgrade_weapon_uncommon", "Temper Emberglass Saber", Workstation.ANVIL,
                    Map.of("new_weapon_ashfall_cutlass", 1, "ember_shard", 2, "copper_ore", 3, "coal", 2),
                    "new_weapon_emberglass_saber__keen", 1, 190,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 4, Profession.MINING.id(), 3),
                    "Lift a common blade into an uncommon, affixed duelist weapon."),
            recipe("upgrade_weapon_rare", "Archive Thornstaff Binding", Workstation.CARPENTER,
                    Map.of("new_weapon_stormthread_wand", 1, "magic_wood", 3, "crystal_dust", 3, "silver_ore", 2),
                    "new_weapon_archive_thornstaff__arcane", 1, 260,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 7, Profession.WOODCUTTING.id(), 5),
                    "Bind an uncommon focus into a rare staff with a spell-damage affix."),
            recipe("upgrade_weapon_unique", "Bellringer Reforging", Workstation.ANVIL,
                    Map.of("new_weapon_lanternfall_bow", 1, "stone_bells", 1, "mithril_ore", 3, "crystal_dust", 4),
                    "new_weapon_the_bellringer", 1, 360,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 12, Profession.MINING.id(), 9),
                    "Reforge rare arms into a unique weapon with a real first-critical hook."),
            recipe("upgrade_weapon_legendary", "Dawn-Crown Brand Rite", Workstation.ANVIL,
                    Map.of("new_weapon_the_bellringer", 1, "stone_dawn", 1, "adamantite_ore", 4, "mithril_ore", 4),
                    "new_weapon_dawn_crown_brand", 1, 480,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 16, Profession.MINING.id(), 13),
                    "Crown a unique weapon into a legendary blade that can deny one lethal blow."),
            recipe("upgrade_armor_uncommon", "Briarwatch Helm Lining", Workstation.ANVIL,
                    Map.of("new_armor_mended_linen_hood", 1, "skin", 2, "iron_ore", 2, "herb_leaf", 2),
                    "new_armor_briarwatch_helm__stout", 1, 180,
                    Profession.LEATHERWORKING.id(), Map.of(Profession.LEATHERWORKING.id(), 4, Profession.CRAFTING.id(), 3),
                    "Rework common protection into uncommon affixed armor."),
            recipe("upgrade_armor_rare", "Lanternscale Coat Setting", Workstation.ANVIL,
                    Map.of("new_armor_cobalt_mail", 1, "scale", 3, "silver_ore", 2, "glowroot", 2),
                    "new_armor_lanternscale_coat__bulwark", 1, 290,
                    Profession.LEATHERWORKING.id(), Map.of(Profession.LEATHERWORKING.id(), 8, Profession.CRAFTING.id(), 6),
                    "Set rare scales into a defensive armor upgrade."),
            recipe("upgrade_armor_unique", "Lyra Mercywrap Stitching", Workstation.ALCHEMY,
                    Map.of("new_armor_nightwater_mantle", 1, "stone_roots", 1, "flower_blossom", 4, "crystal_dust", 4),
                    "new_armor_lyra_mercywraps", 1, 370,
                    Profession.LEATHERWORKING.id(), Map.of(Profession.LEATHERWORKING.id(), 12, Profession.CRAFTING.id(), 10),
                    "Make unique healer armor whose overheal becomes a ward."),
            recipe("upgrade_armor_legendary", "Worldroot Carapace Growth", Workstation.CARPENTER,
                    Map.of("new_armor_lyra_mercywraps", 1, "stone_roots", 1, "adamantite_ore", 3, "glowroot", 5),
                    "new_armor_worldroot_carapace", 1, 480,
                    Profession.LEATHERWORKING.id(), Map.of(Profession.LEATHERWORKING.id(), 16, Profession.WOODCUTTING.id(), 12),
                    "Grow unique armor into a legendary piece that rewards alternating spell and weapon damage."),
            recipe("upgrade_accessory_uncommon", "Emberglass Signet Setting", Workstation.ANVIL,
                    Map.of("new_accessory_tin_luck_ring", 1, "ember_shard", 1, "copper_ore", 2, "crystal_dust", 1),
                    "new_accessory_emberglass_signet__surgical", 1, 150,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 4, Profession.MINING.id(), 3),
                    "Upgrade a common ring into an uncommon affixed crit accessory."),
            recipe("upgrade_accessory_rare", "Archive Pageweight Inscription", Workstation.ALCHEMY,
                    Map.of("new_accessory_stormthread_torque", 1, "crystal_dust", 4, "glowroot", 2, "silver_ore", 2),
                    "new_accessory_archive_pageweight__focused", 1, 245,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 7, Profession.MINING.id(), 5),
                    "Inscribe a rare accessory with focused casting stats."),
            recipe("upgrade_accessory_unique", "Lyra Mercybell Casting", Workstation.ANVIL,
                    Map.of("new_accessory_nightwater_signet", 1, "stone_bells", 1, "mithril_ore", 3, "flower_blossom", 4),
                    "new_accessory_lyra_mercybell", 1, 345,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 12, Profession.MINING.id(), 9),
                    "Cast a unique ring that remembers one desperate victory."),
            recipe("upgrade_accessory_legendary", "Worldroot Seedstone Vow", Workstation.ALCHEMY,
                    Map.of("new_accessory_lyra_mercybell", 1, "stone_roots", 1, "adamantite_ore", 3, "crystal_dust", 6),
                    "new_accessory_worldroot_seedstone", 1, 460,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 16, Profession.MINING.id(), 12),
                    "Raise a unique accessory into legendary alternating-damage gear."),
            recipe("reforge_keen_weapon", "Keen Weapon Reforge", Workstation.ANVIL,
                    Map.of("new_weapon_cobalt_halberd", 1, "crystal_dust", 2, "iron_ore", 3, "ember_shard", 1),
                    "new_weapon_cobalt_halberd__keen", 1, 190,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 6, Profession.MINING.id(), 5),
                    "Reforge an uncommon weapon toward crit rate and crit damage."),
            recipe("reforge_surgical_weapon", "Surgical Weapon Reforge", Workstation.ANVIL,
                    Map.of("new_weapon_nightwater_trident", 1, "crystal_dust", 4, "silver_ore", 3, "venom_sac", 1),
                    "new_weapon_nightwater_trident__surgical", 1, 315,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 10, Profession.MINING.id(), 8),
                    "Tune a rare weapon for high precision and triage pressure."),
            recipe("reinforce_stout_armor", "Stout Armor Reinforcement", Workstation.ANVIL,
                    Map.of("new_armor_cobalt_mail", 1, "iron_ore", 4, "skin", 2, "coal", 2),
                    "new_armor_cobalt_mail__stout", 1, 205,
                    Profession.LEATHERWORKING.id(), Map.of(Profession.LEATHERWORKING.id(), 6, Profession.CRAFTING.id(), 5),
                    "Reinforce uncommon armor with HP and resistance bracing."),
            recipe("reinforce_bulwark_armor", "Bulwark Armor Reinforcement", Workstation.ANVIL,
                    Map.of("new_armor_hearthflame_cuirass", 1, "scale", 3, "mithril_ore", 2, "crystal_dust", 3),
                    "new_armor_hearthflame_cuirass__bulwark", 1, 335,
                    Profession.LEATHERWORKING.id(), Map.of(Profession.LEATHERWORKING.id(), 10, Profession.CRAFTING.id(), 8),
                    "Reinforce rare armor into a heavier defensive affix profile."),
            recipe("attune_focused_accessory", "Focused Accessory Attunement", Workstation.ALCHEMY,
                    Map.of("new_accessory_cobalt_lens", 1, "glowroot", 2, "crystal_dust", 3, "flower_blossom", 1),
                    "new_accessory_cobalt_lens__focused", 1, 185,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 6),
                    "Attune an uncommon accessory toward MP and spell damage."),
            recipe("attune_mending_accessory", "Mending Accessory Attunement", Workstation.ALCHEMY,
                    Map.of("new_accessory_hearthflame_chain", 1, "flower_blossom", 4, "glowroot", 2, "crystal_dust", 3),
                    "new_accessory_hearthflame_chain__mending", 1, 300,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 10),
                    "Attune a rare accessory toward healing power and recovery stats."),
            recipe("upgrade_potion_uncommon", "Emberwarm Elixir Batch", Workstation.ALCHEMY,
                    Map.of("new_potion_minor_redcap_draught", 1, "herb_leaf", 2, "ember_shard", 1),
                    "new_potion_emberwarm_elixir", 1, 115,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 3),
                    "Refine common medicine into an uncommon battle elixir."),
            recipe("upgrade_potion_rare", "Archive Ink Tonic Batch", Workstation.ALCHEMY,
                    Map.of("new_potion_stormbreath_tonic", 1, "crystal_dust", 3, "glowroot", 1, "flower_blossom", 2),
                    "new_potion_archive_ink_tonic", 1, 190,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 7),
                    "Refine uncommon medicine into a rare tonic."),
            recipe("upgrade_potion_unique", "Lyra Mercy Vial Batch", Workstation.ALCHEMY,
                    Map.of("new_potion_nightwater_draught", 1, "stone_bells", 1, "flower_blossom", 5, "crystal_dust", 3),
                    "new_potion_lyra_mercy_vial", 1, 300,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 12),
                    "Brew a unique potion that makes the first potion of battle stronger."),
            recipe("upgrade_potion_legendary", "Worldroot Panacea Batch", Workstation.ALCHEMY,
                    Map.of("new_potion_lyra_mercy_vial", 1, "stone_roots", 1, "glowroot", 5, "adamantite_ore", 2),
                    "new_potion_worldroot_panacea", 1, 430,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 16, Profession.MINING.id(), 10),
                    "Brew legendary medicine tied to alternating spell and weapon rhythm."),
            recipe("ember_socket_stone", "Stone of Ember", Workstation.ALCHEMY,
                    Map.of("ember_shard", 3, "crystal_dust", 2, "coal", 1), "stone_ember", 1, 170,
                    Profession.CRAFTING.id(), Map.of(Profession.CRAFTING.id(), 3, Profession.MINING.id(), 2),
                    "Solari's ember rite shapes heat, glass, and ash into a socket stone.")
    );

    private ActiveTask activeTask;

    public static String itemName(String key) {
        ItemInfo info = CRAFTING_ITEMS.get(key);
        return info == null ? null : info.name();
    }

    public static String itemIcon(String key) {
        ItemInfo info = CRAFTING_ITEMS.get(key);
        return info == null ? null : info.icon();
    }

    public static String itemDetail(String key) {
        ItemInfo info = CRAFTING_ITEMS.get(key);
        return info == null ? "" : info.detail();
    }

    public static boolean isCraftingOnlyItem(String key) {
        return CRAFTING_ITEMS.containsKey(key);
    }

    public static Workstation workstationForAsset(String asset) {
        return switch (asset) {
            case "interior_anvil", "interior_forge", "interior_anvil_tool_rack" -> Workstation.ANVIL;
            case "interior_carpenter_table", "interior_carpenter_workbench", "interior_sawhorse_planks" -> Workstation.CARPENTER;
            case "interior_oven", "interior_bakery_oven", "interior_bakery_counter", "interior_stove", "interior_hearth_pot",
                    "interior_cooking_station", "interior_cookpot_stand" -> Workstation.OVEN;
            case "interior_alchemy_station", "interior_mortar_pestle" -> Workstation.ALCHEMY;
            default -> null;
        };
    }

    public static int[] workstationFootprint(String asset) {
        return switch (asset) {
            case "interior_carpenter_table", "interior_carpenter_workbench", "interior_alchemy_station",
                    "interior_cooking_station", "interior_sawhorse_planks", "interior_bakery_counter" -> new int[]{2, 1};
            case "interior_bakery_oven", "interior_anvil_tool_rack" -> new int[]{1, 2};
            default -> new int[]{1, 1};
        };
    }

    public static List<Recipe> recipesFor(Workstation workstation) {
        return RECIPES.stream()
                .filter(recipe -> recipe.workstation() == null || recipe.workstation() == workstation)
                .toList();
    }

    public static List<Recipe> sortRecipes(List<Recipe> recipes) {
        return recipes.stream()
                .sorted(Comparator.comparingInt((Recipe recipe) -> recipe.category().ordinal())
                        .thenComparing(Recipe::name))
                .toList();
    }

    public static Set<String> starterRecipeKeys() {
        return STARTER_RECIPE_KEYS;
    }

    public static boolean isStarterRecipe(String recipeKey) {
        return STARTER_RECIPE_KEYS.contains(recipeKey);
    }

    public static String recipeKeyForBookItem(String itemKey) {
        return RECIPE_BOOKS.getOrDefault(itemKey, "");
    }

    public static boolean isRecipeBookItem(String itemKey) {
        return RECIPE_BOOKS.containsKey(itemKey);
    }

    public static List<Recipe> recipesUsingIngredient(String itemKey) {
        if (itemKey == null || itemKey.isBlank()) {
            return List.of();
        }
        return RECIPES.stream()
                .filter(recipe -> !isStarterRecipe(recipe.key()))
                .filter(recipe -> !isStoryLockedRecipe(recipe.key()))
                .filter(recipe -> recipe.cost().containsKey(itemKey))
                .toList();
    }

    public static List<Recipe> recipesFromBookcase(String mapId, int x, int y) {
        List<Recipe> readable = RECIPES.stream()
                .filter(recipe -> !isStarterRecipe(recipe.key()))
                .filter(recipe -> !isStoryLockedRecipe(recipe.key()))
                .toList();
        if (readable.isEmpty()) {
            return List.of();
        }
        int seed = mapId.hashCode() * 31 + x * 928371 + y * 364479;
        Recipe first = readable.get(Math.floorMod(seed, readable.size()));
        Recipe second = readable.get(Math.floorMod(seed / 7 + 11, readable.size()));
        if (first.key().equals(second.key())) {
            return List.of(first);
        }
        return List.of(first, second);
    }

    public static List<Recipe> recipesTaughtByNpc(Npc npc) {
        if (npc == null) {
            return List.of();
        }
        String text = (npc.name() + " " + npc.sprite() + " " + (npc.shopId() == null ? "" : npc.shopId())
                + " " + String.join(" ", npc.dialog())).toLowerCase();
        Set<RecipeCategory> categories = new HashSet<>();
        if (hasAny(text, "smith", "blacksmith", "forge", "armorer", "armourer", "weaponsmith", "quartermaster")) {
            categories.add(RecipeCategory.WEAPON);
            categories.add(RecipeCategory.ARMOR);
            categories.add(RecipeCategory.TOOL);
        }
        if (hasAny(text, "carpenter", "woodworker", "woodwright", "bowyer", "fletcher", "joiner")) {
            categories.add(RecipeCategory.WEAPON);
            categories.add(RecipeCategory.TOOL);
            categories.add(RecipeCategory.DECOR);
        }
        if (hasAny(text, "cook", "baker", "innkeeper", "tavern", "brewer", "chef")) {
            categories.add(RecipeCategory.CONSUMABLE);
        }
        if (hasAny(text, "alchemist", "apothecary", "herbalist", "healer", "physicker", "distiller")) {
            categories.add(RecipeCategory.CONSUMABLE);
            categories.add(RecipeCategory.MATERIAL);
        }
        if (hasAny(text, "farmer", "gardener", "seedkeeper", "seed keeper", "horticulturist", "beekeeper")) {
            categories.add(RecipeCategory.SEED);
            categories.add(RecipeCategory.CONSUMABLE);
        }
        if (hasAny(text, "tailor", "weaver", "leatherworker", "tanner", "cobbler", "seamstress")) {
            categories.add(RecipeCategory.ARMOR);
            categories.add(RecipeCategory.CONSUMABLE);
        }
        if (categories.isEmpty()) {
            return List.of();
        }
        return sortRecipes(RECIPES.stream()
                .filter(recipe -> !isStarterRecipe(recipe.key()))
                .filter(recipe -> !isStoryLockedRecipe(recipe.key()))
                .filter(recipe -> categories.contains(recipe.category()))
                .toList());
    }

    private static boolean isStoryLockedRecipe(String recipeKey) {
        return "ember_socket_stone".equals(recipeKey);
    }

    private static boolean hasAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public static Recipe recipeByKey(String key) {
        for (Recipe recipe : RECIPES) {
            if (recipe.key().equals(key)) {
                return recipe;
            }
        }
        return null;
    }

    public static String costLabel(Map<String, Integer> cost) {
        if (cost.isEmpty()) {
            return "free";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            parts.add(entry.getValue() + " " + GameData.itemName(entry.getKey()));
        }
        return String.join(", ", parts);
    }

    public static boolean canAfford(Actor actor, Map<String, Integer> cost) {
        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            if (actor.inventory.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public static boolean meetsProfessionRequirements(Actor actor, Recipe recipe) {
        for (Map.Entry<String, Integer> entry : recipe.professionRequirements().entrySet()) {
            if (actor.professionLevel(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public static boolean canCraft(Actor actor, Recipe recipe) {
        return canAfford(actor, recipe.cost()) && meetsProfessionRequirements(actor, recipe);
    }

    public static String professionRequirementLabel(Recipe recipe) {
        if (recipe.professionRequirements().isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : recipe.professionRequirements().entrySet()) {
            parts.add(Profession.label(entry.getKey()) + " " + entry.getValue());
        }
        return String.join(", ", parts);
    }

    public static String requirementLabel(Recipe recipe) {
        String materials = costLabel(recipe.cost());
        String professions = professionRequirementLabel(recipe);
        return professions.isBlank() ? materials : materials + "; " + professions;
    }

    public static String grantLoot(Actor actor, List<GameData.MonsterSpec> specs, Random random) {
        return grantLoot(actor, specs, random, "", 'g', 0);
    }

    public static String grantLoot(Actor actor, List<GameData.MonsterSpec> specs, Random random,
                                   String mapKind, char terrain, int dungeonTier) {
        Map<String, Integer> loot = new LinkedHashMap<>();
        for (GameData.MonsterSpec spec : specs) {
            rollMonsterLoot(spec.key(), random, loot);
        }
        String contextual = GameData.rollContextualLoot(mapKind, terrain, dungeonTier, specs, random);
        if (!contextual.isBlank()) {
            addLoot(loot, contextual, 1);
        }
        for (Map.Entry<String, Integer> entry : loot.entrySet()) {
            actor.addItem(entry.getKey(), entry.getValue());
        }
        return lootLabel(loot);
    }

    public boolean active() {
        return activeTask != null;
    }

    public double progress() {
        if (activeTask == null) {
            return 0.0;
        }
        return 1.0 - activeTask.remainingTicks / (double) Math.max(1, activeTask.totalTicks);
    }

    public String progressLabel() {
        if (activeTask == null) {
            return "";
        }
        int percent = (int) Math.round(progress() * 100.0);
        return activeTask.actionLabel + " " + Math.max(0, Math.min(100, percent)) + "%";
    }

    public int worldTickAdvance() {
        return activeTask == null ? 1 : activeTask.worldTickAdvance;
    }

    public Map<String, Integer> lastCompletedOutput() {
        return Map.copyOf(lastCompletedOutput);
    }

    public GatherCandidate findGatherTarget(WorldMap world, String mapId, int playerX, int playerY) {
        for (int[] direction : GATHER_DIRECTIONS) {
            GatherCandidate candidate = gatherCandidateAt(world, mapId, playerX + direction[0], playerY + direction[1]);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    public GatherCandidate findGatherTarget(WorldMap world, String mapId, int playerX, int playerY, int facingDx, int facingDy) {
        if (Math.abs(facingDx) + Math.abs(facingDy) != 1) {
            return findGatherTarget(world, mapId, playerX, playerY);
        }
        return gatherCandidateAt(world, mapId, playerX + facingDx, playerY + facingDy);
    }

    public GatherCandidate findGatherTargetAt(WorldMap world, String mapId, int x, int y) {
        return gatherCandidateAt(world, mapId, x, y);
    }

    private GatherCandidate gatherCandidateAt(WorldMap world, String mapId, int x, int y) {
        WorldProp prop = world.propAt(mapId, x, y);
        if (prop != null && isGatherableProp(prop.asset())) {
            return new GatherCandidate(propGatherLabel(prop.asset()), new TilePoint(x, y), 'P', prop.asset());
        }
        char tile = world.tileAt(mapId, x, y);
        if (tile == 'm' || tile == 'q') {
            return new GatherCandidate("mountain stone", new TilePoint(x, y), tile);
        }
        if (tile == 'w') {
            return new GatherCandidate("water", new TilePoint(x, y), tile);
        }
        return null;
    }

    public String beginGather(Actor actor, List<Actor> helpers, GatherCandidate candidate, Random random) {
        if (activeTask != null) {
            return "Already busy: " + activeTask.actionLabel + ".";
        }
        if (candidate == null) {
            return "Stand next to a resource node, mountain, water, beach prop, or planter to gather.";
        }
        Map<String, Integer> output = new LinkedHashMap<>();
        int ticks;
        String label;
        String profession;
        PropProfile profile = PropProfile.NONE;
        if (candidate.terrain() == 'f') {
            profession = Profession.WOODCUTTING.id();
            int skillLevel = effectiveProfessionLevel(actor, helpers, profession);
            boolean axe = actor.hasItem("woodcutter_axe") || "woodcutter_axe".equals(actor.equipment.get("weapon"));
            boolean stoneAxe = axe || actor.hasItem("stone_axe");
            ticks = axe ? 42 : stoneAxe ? 54 : 72;
            output.put("wood", (axe ? 3 : 2) + random.nextInt(2));
            if (stoneAxe && random.nextDouble() < 0.45) {
                output.put("plant_fiber", 1);
            }
            if (random.nextDouble() < 0.25) {
                output.put("skin", 1);
            }
            applyProfessionGatherBonuses(actor, output, profession, skillLevel, random);
            ticks = adjustedGatherTicks(ticks, skillLevel);
            label = axe ? "Chopping trees" : stoneAxe ? "Hewing branches" : "Gathering fallen wood";
        } else if (candidate.terrain() == 'm' || candidate.terrain() == 'q') {
            profession = Profession.MINING.id();
            int skillLevel = effectiveProfessionLevel(actor, helpers, profession);
            boolean ironPick = actor.hasItem("iron_pickaxe");
            boolean stonePick = ironPick || actor.hasItem("stone_pickaxe");
            ticks = ironPick ? 54 : stonePick ? 76 : 108;
            output.put("stone", (stonePick ? 2 : 1) + random.nextInt(2));
            double oreChance = ironPick ? 0.85 : stonePick ? 0.55 : 0.25;
            oreChance += (skillLevel - 1) * 0.035;
            if (random.nextDouble() < oreChance) {
                output.put("iron_ore", ironPick && random.nextBoolean() ? 2 : 1);
            }
            applyProfessionGatherBonuses(actor, output, profession, skillLevel, random);
            ticks = adjustedGatherTicks(ticks, skillLevel);
            label = stonePick ? "Mining mountain stone" : "Chipping loose stone";
        } else if (candidate.terrain() == 'w') {
            profession = Profession.FISHING.id();
            int skillLevel = effectiveProfessionLevel(actor, helpers, profession);
            boolean lure = actor.hasItem("shell_lure");
            ticks = lure ? 62 : 82;
            addLoot(output, "raw_fish", 1);
            if (random.nextDouble() < 0.25 + skillLevel * 0.035) {
                addLoot(output, random.nextBoolean() ? "seashell" : "plant_fiber", 1);
            }
            if (random.nextDouble() < 0.08 + skillLevel * 0.025) {
                addLoot(output, "raw_fish", 1);
            }
            applyProfessionGatherBonuses(actor, output, profession, skillLevel, random);
            ticks = adjustedGatherTicks(ticks, skillLevel);
            label = lure ? "Fishing with shell lure" : "Fishing";
        } else if (candidate.terrain() == 'P') {
            profile = propProfile(candidate.asset());
            profession = professionForProfile(profile);
            int skillLevel = effectiveProfessionLevel(actor, helpers, profession);
            ticks = adjustedGatherTicks(propGatherTicks(candidate.asset()), skillLevel);
            label = "Gathering from " + candidate.label();
            addPropGatherOutput(candidate.asset(), output, random);
            applyToolGatherBonuses(actor, candidate.asset(), output, profession, skillLevel, random);
            applyProfessionGatherBonuses(actor, output, profession, skillLevel, random);
        } else {
            return "Nothing useful to gather here.";
        }
        List<Actor> participants = gatherParticipants(actor, helpers);
        int xp = 8 + output.values().stream().mapToInt(Integer::intValue).sum() * 2;
        int characterXp = Math.max(1, (int) Math.round((4 + output.values().stream().mapToInt(Integer::intValue).sum()) * 0.80));
        activeTask = new ActiveTask(label, ticks, output, 6, profession, xp, characterXp, participants);
        return label + " near " + candidate.label() + "...";
    }

    private static boolean isGatherableProp(String asset) {
        return gatherablePropProfile(asset) != PropProfile.NONE;
    }

    public static boolean isDepletableResourceNode(String asset) {
        String lower = asset == null ? "" : asset.toLowerCase();
        if (lower.isBlank()) {
            return false;
        }
        return gatherPrimaryMaterialKey(asset) != null
                || lower.contains("beach_shells")
                || lower.contains("beach_coconuts")
                || lower.contains("driftwood")
                || lower.contains("herb_patch")
                || lower.contains("mushroom")
                || lower.contains("reeds")
                || lower.contains("cattails")
                || lower.contains("crystal_cluster");
    }

    private static String propGatherLabel(String asset) {
        String lower = asset.toLowerCase();
        String oreKey = typedOreKey(lower);
        if (oreKey != null) {
            return switch (oreKey) {
                case "iron_ore" -> "iron ore";
                case "copper_ore" -> "copper ore";
                case "tin_ore" -> "tin ore";
                case "silver_ore" -> "silver ore";
                case "gold_ore" -> "gold ore";
                case "mithril_ore" -> "mithril ore";
                case "cobalt_ore" -> "cobalt ore";
                case "adamantite_ore" -> "adamantite ore";
                case "steel_scrap" -> "steel scrap";
                case "crystal_dust" -> "crystals";
                case "coal" -> "coal";
                default -> "ore";
            };
        }
        String woodKey = typedWoodKey(lower);
        if (woodKey != null) {
            return switch (woodKey) {
                case "oak_wood" -> "oak wood";
                case "birch_wood" -> "birch wood";
                case "pine_wood" -> "pine wood";
                case "willow_wood" -> "willow wood";
                case "maple_wood" -> "maple wood";
                case "ash_wood" -> "ash wood";
                case "elder_wood" -> "elder wood";
                case "magic_wood" -> "magic wood";
                case "deadwood" -> "deadwood";
                case "fruitwood" -> "fruitwood";
                case "ironwood" -> "ironwood";
                case "enchanted_bark" -> "enchanted bark";
                case "glowroot" -> "glowroot";
                default -> "wood";
            };
        }
        if (lower.contains("seed_bowl")) {
            return "seed bowl";
        }
        if (lower.contains("planting_pot")) {
            return "planting pot";
        }
        if (lower.contains("ivy") || lower.contains("vine") || lower.contains("sapling")) {
            return "living plants";
        }
        if (lower.contains("sprout")) {
            return "sprout planter";
        }
        if (lower.contains("flower") || lower.contains("blossom") || lower.contains("daisy")) {
            return "flowers";
        }
        if (lower.contains("herb")) {
            return "herbs";
        }
        if (lower.contains("mushroom")) {
            return "mushrooms";
        }
        if (lower.contains("reeds") || lower.contains("cattails")) {
            return "reeds";
        }
        if (lower.contains("coconut")) {
            return "coconuts";
        }
        if (lower.contains("shell")) {
            return "shells";
        }
        if (lower.contains("palm")) {
            return "palm fronds";
        }
        if (lower.contains("driftwood")) {
            return "driftwood";
        }
        if (lower.contains("beach_grass") || lower.contains("dune")) {
            return "dune grass";
        }
        if (lower.contains("net")) {
            return "net rack";
        }
        if (lower.contains("rock") || lower.contains("stone") || lower.contains("pebble") || lower.contains("cairn")) {
            return "stones";
        }
        if (lower.contains("crystal")) {
            return "crystals";
        }
        if (lower.contains("tree") || lower.contains("pine") || lower.contains("log") || lower.contains("stump") || lower.contains("woodpile")) {
            return "wood";
        }
        if (lower.contains("crate") || lower.contains("barrel") || lower.contains("palisade")) {
            return "salvage";
        }
        if (lower.contains("bone") || lower.contains("skull")) {
            return "bones";
        }
        if (lower.contains("jar") || lower.contains("pot")) {
            return "clay vessels";
        }
        if (lower.contains("fire")) {
            return "ashes";
        }
        return "nearby growth";
    }

    private static int propGatherTicks(String asset) {
        return switch (propProfile(asset)) {
            case WOOD, PALM, DRIFTWOOD, SALVAGE -> 52;
            case STONE, CRYSTAL -> 58;
            case BONE, CLAY, FIRE -> 46;
            case PLANT, FLOWER, MUSHROOM, REED, FARM, COCONUT, SHELL -> 38;
            case NONE -> 44;
        };
    }

    private static void addPropGatherOutput(String asset, Map<String, Integer> output, Random random) {
        String lower = asset == null ? "" : asset.toLowerCase();
        if (addTypedWoodOutput(lower, output, random) || addTypedOreOutput(lower, output, random)) {
            return;
        }
        switch (propProfile(asset)) {
            case WOOD -> {
                addLoot(output, "wood", 1 + random.nextInt(2));
                if (random.nextDouble() < 0.55) {
                    addLoot(output, "plant_fiber", 1);
                }
                if (random.nextDouble() < 0.25) {
                    addLoot(output, "herb_seed", 1);
                }
            }
            case PALM -> {
                addLoot(output, "wood", 1);
                addLoot(output, "palm_frond", 1 + random.nextInt(2));
                if (random.nextDouble() < 0.5) {
                    addLoot(output, "coconut", 1);
                }
                if (random.nextDouble() < 0.45) {
                    addLoot(output, "plant_fiber", 1);
                }
            }
            case COCONUT -> {
                addLoot(output, "coconut", 1 + random.nextInt(2));
                if (random.nextDouble() < 0.35) {
                    addLoot(output, "palm_frond", 1);
                }
            }
            case SHELL -> {
                addLoot(output, "seashell", 1 + random.nextInt(3));
                if (random.nextDouble() < 0.28) {
                    addLoot(output, "flint", 1);
                }
            }
            case DRIFTWOOD -> {
                addLoot(output, "wood", 1 + random.nextInt(2));
                if (random.nextDouble() < 0.45) {
                    addLoot(output, "flint", 1);
                }
                if (random.nextDouble() < 0.35) {
                    addLoot(output, "seashell", 1);
                }
            }
            case SALVAGE -> {
                addLoot(output, "wood", 1);
                if (random.nextDouble() < 0.45) {
                    addLoot(output, "plant_fiber", 1);
                }
                if (random.nextDouble() < 0.35) {
                    addLoot(output, "coal", 1);
                }
            }
            case STONE -> {
                addLoot(output, "stone", 1 + random.nextInt(2));
                if (random.nextDouble() < 0.55) {
                    addLoot(output, "flint", 1);
                }
                if (random.nextDouble() < 0.2) {
                    addLoot(output, "iron_ore", 1);
                }
            }
            case CRYSTAL -> {
                addLoot(output, "crystal_dust", 1 + random.nextInt(2));
                if (random.nextDouble() < 0.35) {
                    addLoot(output, "frost_shard", 1);
                }
            }
            case PLANT -> {
                addLoot(output, "plant_fiber", 1 + random.nextInt(2));
                if (random.nextDouble() < 0.7) {
                    addLoot(output, "herb_seed", 1);
                }
                if (random.nextDouble() < 0.5) {
                    addLoot(output, "herb_leaf", 1);
                }
            }
            case FLOWER -> {
                addLoot(output, "flower_blossom", 1 + random.nextInt(2));
                if (random.nextDouble() < 0.65) {
                    addLoot(output, "herb_seed", 1);
                }
                if (random.nextDouble() < 0.35) {
                    addLoot(output, "herb_leaf", 1);
                }
            }
            case MUSHROOM -> {
                addLoot(output, "mushroom_spores", 1 + random.nextInt(2));
                if (random.nextDouble() < 0.4) {
                    addLoot(output, "herb_leaf", 1);
                }
            }
            case REED -> {
                addLoot(output, "plant_fiber", 2 + random.nextInt(2));
                if (random.nextDouble() < 0.45) {
                    addLoot(output, "herb_seed", 1);
                }
            }
            case FARM -> {
                addLoot(output, "garden_vegetables", 1 + random.nextInt(2));
                addLoot(output, "herb_seed", 1 + random.nextInt(2));
                if (random.nextDouble() < 0.5) {
                    addLoot(output, "plant_fiber", 1);
                }
            }
            case BONE -> {
                addLoot(output, "bone", 1 + random.nextInt(2));
                if (random.nextDouble() < 0.3) {
                    addLoot(output, "flint", 1);
                }
            }
            case CLAY -> {
                addLoot(output, "clay", 1 + random.nextInt(2));
                if (random.nextDouble() < 0.35) {
                    addLoot(output, "coal", 1);
                }
            }
            case FIRE -> {
                addLoot(output, "coal", 1 + random.nextInt(2));
                if (random.nextDouble() < 0.3) {
                    addLoot(output, "ember_shard", 1);
                }
            }
            case NONE -> {
            }
        }
    }

    private static boolean addTypedWoodOutput(String lower, Map<String, Integer> output, Random random) {
        String woodKey = typedWoodKey(lower);
        if (woodKey == null) {
            return false;
        }
        addLoot(output, woodKey, 1 + random.nextInt(2));
        if (lower.contains("ironwood") || lower.contains("magical") || lower.contains("glowing_root")) {
            addLoot(output, woodKey, 1);
        }
        if (random.nextDouble() < 0.45) {
            addLoot(output, "wood", 1);
        }
        if (lower.contains("fruit") && random.nextDouble() < 0.65) {
            addLoot(output, "garden_vegetables", 1);
        }
        if ((lower.contains("magical") || lower.contains("enchanted") || lower.contains("glowing_root"))
                && random.nextDouble() < 0.55) {
            addLoot(output, "crystal_dust", 1);
        }
        if (lower.contains("deadwood") && random.nextDouble() < 0.55) {
            addLoot(output, "coal", 1);
        }
        if (random.nextDouble() < 0.35) {
            addLoot(output, "plant_fiber", 1);
        }
        return true;
    }

    private static boolean addTypedOreOutput(String lower, Map<String, Integer> output, Random random) {
        String oreKey = typedOreKey(lower);
        if (oreKey == null) {
            return false;
        }
        int amount = switch (oreKey) {
            case "coal", "crystal_dust" -> 2 + random.nextInt(2);
            case "adamantite_ore", "mithril_ore" -> 1;
            default -> 1 + random.nextInt(2);
        };
        addLoot(output, oreKey, amount);
        if (!"coal".equals(oreKey) && random.nextDouble() < 0.65) {
            addLoot(output, "stone", 1);
        }
        if ("steel_scrap".equals(oreKey)) {
            if (random.nextDouble() < 0.45) {
                addLoot(output, "iron_ore", 1);
            }
            if (random.nextDouble() < 0.35) {
                addLoot(output, "coal", 1);
            }
        }
        if ("crystal_dust".equals(oreKey) && random.nextDouble() < 0.35) {
            addLoot(output, "frost_shard", 1);
        }
        return true;
    }

    private static String gatherPrimaryMaterialKey(String asset) {
        String lower = asset == null ? "" : asset.toLowerCase();
        String woodKey = typedWoodKey(lower);
        return woodKey == null ? typedOreKey(lower) : woodKey;
    }

    private static String typedWoodKey(String lower) {
        if (lower.contains("oak_harvestable")) {
            return "oak_wood";
        }
        if (lower.equals("deco_tree_oak")) {
            return "oak_wood";
        }
        if (lower.contains("birch")) {
            return "birch_wood";
        }
        if (lower.contains("pine_harvestable") || lower.equals("deco_tree_pine") || lower.equals("deco_tree_blue_pine")
                || lower.equals("deco_snow_pine") || lower.equals("deco_mountain_scrub_pine")
                || lower.equals("deco_pine_sapling") || lower.equals("deco_forest_pine_cluster")) {
            return "pine_wood";
        }
        if (lower.contains("willow")) {
            return "willow_wood";
        }
        if (lower.contains("maple")) {
            return "maple_wood";
        }
        if (lower.contains("ash_harvestable") || lower.contains("fallen_ash")) {
            return "ash_wood";
        }
        if (lower.contains("elder")) {
            return "elder_wood";
        }
        if (lower.contains("magical")) {
            return "magic_wood";
        }
        if (lower.contains("deadwood")) {
            return "deadwood";
        }
        if (lower.contains("fruit")) {
            return "fruitwood";
        }
        if (lower.contains("ironwood")) {
            return "ironwood";
        }
        if (lower.contains("enchanted_stump")) {
            return "enchanted_bark";
        }
        if (lower.contains("glowing_root")) {
            return "glowroot";
        }
        return null;
    }

    private static String typedOreKey(String lower) {
        if (lower.contains("iron_vein")) {
            return "iron_ore";
        }
        if (lower.contains("copper_vein")) {
            return "copper_ore";
        }
        if (lower.contains("coal_deposit")) {
            return "coal";
        }
        if (lower.contains("mithril_vein") || lower.contains("mythril_vein")) {
            return "mithril_ore";
        }
        if (lower.contains("cobalt_vein")) {
            return "cobalt_ore";
        }
        if (lower.contains("adamantite_vein")) {
            return "adamantite_ore";
        }
        if (lower.contains("tin_vein")) {
            return "tin_ore";
        }
        if (lower.contains("silver_vein")) {
            return "silver_ore";
        }
        if (lower.contains("gold_vein")) {
            return "gold_ore";
        }
        if (lower.contains("crystal_vein")) {
            return "crystal_dust";
        }
        if (lower.contains("steel_scrap")) {
            return "steel_scrap";
        }
        return null;
    }

    private static void applyToolGatherBonuses(Actor actor, String asset, Map<String, Integer> output,
                                               String profession, int skillLevel, Random random) {
        if (actor == null || asset == null || asset.isBlank()) {
            return;
        }
        String primary = gatherPrimaryMaterialKey(asset);
        if (primary == null) {
            return;
        }
        if (Profession.MINING.id().equals(profession)) {
            boolean ironPick = actor.hasItem("iron_pickaxe");
            boolean stonePick = ironPick || actor.hasItem("stone_pickaxe");
            if (ironPick) {
                addLoot(output, primary, 1);
                if (random.nextDouble() < 0.25 + skillLevel * 0.025) {
                    addLoot(output, "stone", 1);
                }
            } else if (stonePick && random.nextDouble() < 0.35 + skillLevel * 0.025) {
                addLoot(output, primary, 1);
            }
        } else if (Profession.WOODCUTTING.id().equals(profession)) {
            boolean axe = actor.hasItem("woodcutter_axe") || "woodcutter_axe".equals(actor.equipment.get("weapon"));
            boolean stoneAxe = axe || actor.hasItem("stone_axe");
            if (axe) {
                addLoot(output, primary, 1);
                if (random.nextDouble() < 0.35 + skillLevel * 0.02) {
                    addLoot(output, "plant_fiber", 1);
                }
            } else if (stoneAxe && random.nextDouble() < 0.35 + skillLevel * 0.02) {
                addLoot(output, primary, 1);
            }
        }
    }

    private static List<Actor> gatherParticipants(Actor actor, List<Actor> helpers) {
        List<Actor> participants = new ArrayList<>();
        participants.add(actor);
        if (helpers != null) {
            participants.addAll(helpers);
        }
        return participants;
    }

    private static int effectiveProfessionLevel(Actor actor, List<Actor> helpers, String profession) {
        int level = actor.professionLevel(profession) + actor.professionPracticeBonus(profession);
        if (helpers != null) {
            for (Actor helper : helpers) {
                int helperLevel = helper.professionLevel(profession) + helper.professionPracticeBonus(profession);
                level += Math.max(0, helperLevel - 1) / 2;
            }
        }
        return Math.max(1, Math.min(Profession.ABSOLUTE_MAX_LEVEL + 4, level));
    }

    private static int adjustedGatherTicks(int ticks, int skillLevel) {
        double multiplier = 1.0 - Math.min(0.45, Math.max(0, skillLevel - 1) * 0.045);
        return Math.max(18, (int) Math.round(ticks * multiplier));
    }

    private static void applyProfessionGatherBonuses(Actor actor, Map<String, Integer> output, String profession, int skillLevel, Random random) {
        int bonusRolls = Math.max(0, skillLevel - 1);
        for (int i = 0; i < bonusRolls; i++) {
            if (random.nextDouble() >= 0.16 + skillLevel * 0.018) {
                continue;
            }
            switch (profession) {
                case "woodcutting" -> addLoot(output, random.nextDouble() < 0.65 ? "wood" : "plant_fiber", 1);
                case "fishing" -> addLoot(output, random.nextDouble() < 0.70 ? "raw_fish" : "seashell", 1);
                case "mining" -> addLoot(output, random.nextDouble() < 0.55 ? "stone" : random.nextBoolean() ? "iron_ore" : "coal", 1);
                case "weaving" -> addLoot(output, random.nextDouble() < 0.70 ? "plant_fiber" : "wool", 1);
                case "leatherworking" -> addLoot(output, random.nextDouble() < 0.65 ? "skin" : "bone", 1);
                case "cooking" -> addLoot(output, random.nextDouble() < 0.60 ? "garden_vegetables" : "herb_leaf", 1);
                default -> addLoot(output, random.nextDouble() < 0.55 ? "herb_leaf" : "flint", 1);
            }
        }
        if (skillLevel >= 6 && random.nextDouble() < 0.10 + skillLevel * 0.01) {
            switch (profession) {
                case "mining" -> addLoot(output, "crystal_dust", 1);
                case "woodcutting" -> addLoot(output, "herb_seed", 1);
                case "fishing" -> addLoot(output, "shell_lure", 1);
                case "survival" -> addLoot(output, "mushroom_spores", 1);
                default -> {
                }
            }
        }
        applyUniqueProfessionPerks(actor, output, profession, random);
    }

    private static void applyUniqueProfessionPerks(Actor actor, Map<String, Integer> output, String profession, Random random) {
        if (actor == null) {
            return;
        }
        switch (profession) {
            case "woodcutting" -> {
                rollPerkLoot(actor, random, "resin_tapping", output, "plant_fiber", "herb_leaf");
                rollPerkLoot(actor, random, "heartwood_harvest", output, "wood", "herb_seed");
            }
            case "fishing" -> {
                rollPerkLoot(actor, random, "tide_reader", output, "seashell", "plant_fiber");
                rollPerkLoot(actor, random, "netcraft", output, "raw_fish", "raw_fish");
                rollPerkLoot(actor, random, "deepwater_bounty", output, "raw_fish", "shell_lure");
            }
            case "mining" -> {
                rollPerkLoot(actor, random, "seam_sense", output, "iron_ore", "coal");
                rollPerkLoot(actor, random, "blast_mining", output, "stone", "iron_ore");
                rollPerkLoot(actor, random, "gem_cutting", output, "crystal_dust", "iron_ore");
            }
            case "weaving" -> {
                rollPerkLoot(actor, random, "loom_logic", output, "plant_fiber", "wool");
                rollPerkLoot(actor, random, "dye_baths", output, "herb_leaf", "plant_fiber");
                rollPerkLoot(actor, random, "sailcloth_patterns", output, "wool", "plant_fiber");
            }
            case "leatherworking" -> {
                rollPerkLoot(actor, random, "tanner_path", output, "skin", "bone");
                rollPerkLoot(actor, random, "curing_racks", output, "skin", "skin");
                rollPerkLoot(actor, random, "reinforced_hide", output, "skin", "bone");
            }
            case "cooking" -> {
                rollPerkLoot(actor, random, "spice_blends", output, "herb_leaf", "garden_vegetables");
                rollPerkLoot(actor, random, "stockpot_rhythm", output, "garden_vegetables", "raw_fish");
                rollPerkLoot(actor, random, "feast_planning", output, "garden_vegetables", "herb_leaf");
            }
            default -> {
                rollPerkLoot(actor, random, "trailcraft_path", output, "flint", "herb_leaf");
                rollPerkLoot(actor, random, "weather_eye", output, "mushroom_spores", "herb_leaf");
                rollPerkLoot(actor, random, "snare_lines", output, "skin", "bone");
                rollPerkLoot(actor, random, "emergency_cache", output, "flint", "mushroom_spores");
            }
        }
    }

    private static void rollPerkLoot(Actor actor, Random random, String skill, Map<String, Integer> output, String common, String rare) {
        int rank = actor.skillRank(skill);
        if (rank <= 0) {
            return;
        }
        double chance = 0.12 + rank * 0.10;
        if (random.nextDouble() < chance) {
            addLoot(output, random.nextDouble() < 0.72 ? common : rare, 1);
        }
    }

    private static String professionForProfile(PropProfile profile) {
        return switch (profile) {
            case WOOD, PALM, DRIFTWOOD, SALVAGE -> Profession.WOODCUTTING.id();
            case STONE, CRYSTAL, CLAY, FIRE -> Profession.MINING.id();
            case SHELL, REED -> Profession.FISHING.id();
            case FARM, COCONUT -> Profession.COOKING.id();
            case BONE -> Profession.LEATHERWORKING.id();
            case PLANT, FLOWER, MUSHROOM -> Profession.SURVIVAL.id();
            case NONE -> Profession.SURVIVAL.id();
        };
    }

    private static PropProfile propProfile(String asset) {
        String lower = asset == null ? "" : asset.toLowerCase();
        if (lower.isBlank()) {
            return PropProfile.NONE;
        }
        if (lower.contains("coconut")) {
            return PropProfile.COCONUT;
        }
        if (lower.contains("shell")) {
            return PropProfile.SHELL;
        }
        if (lower.contains("driftwood")) {
            return PropProfile.DRIFTWOOD;
        }
        if (lower.contains("palm")) {
            return PropProfile.PALM;
        }
        if (lower.contains("camp_fire") || lower.contains("hearth") || lower.contains("fire")) {
            return PropProfile.FIRE;
        }
        if (lower.contains("crystal") || lower.contains("ice_crystals")) {
            return PropProfile.CRYSTAL;
        }
        if (typedOreKey(lower) != null) {
            return PropProfile.STONE;
        }
        if (lower.contains("bone") || lower.contains("skull")) {
            return PropProfile.BONE;
        }
        if (lower.contains("jar") || lower.contains("clay") || lower.contains("pottery")) {
            return PropProfile.CLAY;
        }
        if (lower.contains("wheat") || lower.contains("hay") || lower.contains("tilled") || lower.contains("garden_vegetables")) {
            return PropProfile.FARM;
        }
        if (lower.contains("mushroom")) {
            return PropProfile.MUSHROOM;
        }
        if (lower.contains("reed") || lower.contains("cattail") || lower.contains("water_lily") || lower.contains("duckweed")) {
            return PropProfile.REED;
        }
        if (lower.contains("flower") || lower.contains("blossom") || lower.contains("daisy") || lower.contains("clover")) {
            return PropProfile.FLOWER;
        }
        if (lower.contains("herb") || lower.contains("plant") || lower.contains("grass") || lower.contains("bush")
                || lower.contains("fern") || lower.contains("leaf") || lower.contains("cactus") || lower.contains("seed")
                || lower.contains("alpine_mix") || lower.contains("sapling") || lower.contains("ivy")
                || lower.contains("vine") || lower.contains("dune")) {
            return PropProfile.PLANT;
        }
        if (lower.contains("tree") || lower.contains("pine") || lower.contains("log") || lower.contains("stump")
                || lower.contains("roots") || lower.contains("woodpile")) {
            return PropProfile.WOOD;
        }
        if (lower.contains("crate") || lower.contains("barrel") || lower.contains("palisade") || lower.contains("signpost")
                || lower.contains("fence") || lower.contains("cart")) {
            return PropProfile.SALVAGE;
        }
        if (lower.contains("rock") || lower.contains("stone") || lower.contains("pebble") || lower.contains("cairn")
                || lower.contains("boulder") || lower.contains("tombstone")) {
            return PropProfile.STONE;
        }
        return PropProfile.NONE;
    }

    private static PropProfile gatherablePropProfile(String asset) {
        String lower = asset == null ? "" : asset.toLowerCase();
        if (lower.isBlank()) {
            return PropProfile.NONE;
        }
        if (typedOreKey(lower) != null) {
            return PropProfile.STONE;
        }
        if (typedWoodKey(lower) != null || lower.contains("harvestable") || isRegularTreeProp(lower)) {
            return PropProfile.WOOD;
        }
        if (lower.contains("beach_coconuts") || lower.contains("coconut")) {
            return PropProfile.COCONUT;
        }
        if (lower.contains("beach_shells") || lower.contains("shell")) {
            return PropProfile.SHELL;
        }
        if (lower.contains("driftwood")) {
            return PropProfile.DRIFTWOOD;
        }
        if (lower.contains("palm_frond") || lower.contains("palm_shade")) {
            return PropProfile.PALM;
        }
        if (lower.contains("crystal_cluster") || lower.contains("ice_crystals")) {
            return PropProfile.CRYSTAL;
        }
        if (lower.contains("mushroom")) {
            return PropProfile.MUSHROOM;
        }
        if (lower.contains("reeds") || lower.contains("cattails") || lower.contains("water_lily")) {
            return PropProfile.REED;
        }
        if (lower.contains("herb_patch") || lower.contains("herb_planter") || lower.contains("seed_bowl")
                || lower.contains("planting_pot") || lower.contains("sprout_planter")) {
            return PropProfile.PLANT;
        }
        if (lower.contains("flower") || lower.contains("blossom") || lower.contains("daisy")) {
            return PropProfile.FLOWER;
        }
        if (lower.contains("wheat") || lower.contains("hay") || lower.contains("tilled") || lower.contains("garden_vegetables")) {
            return PropProfile.FARM;
        }
        if (lower.contains("bone") || lower.contains("skull")) {
            return PropProfile.BONE;
        }
        if (lower.contains("clay") || lower.contains("pottery")) {
            return PropProfile.CLAY;
        }
        return PropProfile.NONE;
    }

    private static boolean isRegularTreeProp(String lower) {
        return lower.equals("deco_tree_oak")
                || lower.equals("deco_tree_round")
                || lower.equals("deco_tree_pine")
                || lower.equals("deco_tree_blue_pine")
                || lower.equals("deco_tree_young")
                || lower.equals("deco_snow_pine")
                || lower.equals("deco_mountain_scrub_pine")
                || lower.equals("deco_pine_sapling")
                || lower.equals("deco_forest_pine_cluster")
                || lower.equals("deco_forest_log")
                || lower.equals("deco_stump");
    }

    public String beginCraft(Actor actor, Recipe recipe) {
        if (activeTask != null) {
            return "Already busy: " + activeTask.actionLabel + ".";
        }
        if (recipe == null) {
            return "That recipe is not available here.";
        }
        if (!meetsProfessionRequirements(actor, recipe)) {
            return "Need " + professionRequirementLabel(recipe) + " for " + recipe.name() + ".";
        }
        if (!canAfford(actor, recipe.cost())) {
            return "Need " + costLabel(recipe.cost()) + " for " + recipe.name() + ".";
        }
        for (Map.Entry<String, Integer> entry : recipe.cost().entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                actor.consumeItem(entry.getKey());
            }
        }
        int skillLevel = actor.professionLevel(recipe.profession()) + actor.professionPracticeBonus(recipe.profession());
        int ticks = adjustedGatherTicks(recipe.ticks(), skillLevel);
        int xp = 10 + Math.max(1, recipe.cost().values().stream().mapToInt(Integer::intValue).sum()) * 3;
        int resultAmount = recipe.resultAmount() + craftedOutputBonus(actor, recipe);
        activeTask = new ActiveTask("Crafting " + recipe.name(), ticks, Map.of(recipe.resultKey(), resultAmount), 1,
                recipe.profession(), xp, 0, List.of(actor));
        return "Crafting " + recipe.name() + "...";
    }

    public String beginReading(String subject, int ticks) {
        if (activeTask != null) {
            return "Already busy: " + activeTask.actionLabel + ".";
        }
        String label = subject == null || subject.isBlank() ? "Reading" : "Reading " + subject;
        activeTask = new ActiveTask(label, Math.max(24, ticks), Map.of(), 5, "", 0, 0, List.of());
        return label + "...";
    }

    private static int craftedOutputBonus(Actor actor, Recipe recipe) {
        String profession = recipe.profession();
        int bonus = 0;
        if ("crafting".equals(profession)) {
            bonus += actor.skillRank("jig_templates") >= 2 ? 1 : 0;
            bonus += actor.skillRank("masterwork_fittings");
        } else if ("weaving".equals(profession)) {
            bonus += actor.skillRank("sailcloth_patterns");
        } else if ("leatherworking".equals(profession)) {
            bonus += actor.skillRank("saddle_stitch");
        } else if ("cooking".equals(profession)) {
            bonus += actor.skillRank("stockpot_rhythm") >= 2 ? 1 : 0;
            bonus += actor.skillRank("feast_planning");
        }
        return Math.max(0, bonus);
    }

    public String tick(Actor actor) {
        if (activeTask == null) {
            return "";
        }
        activeTask.remainingTicks--;
        if (activeTask.remainingTicks > 0) {
            return "";
        }
        Map<String, Integer> output = activeTask.output;
        lastCompletedOutput = Map.copyOf(output);
        String label = lootLabel(output);
        for (Map.Entry<String, Integer> entry : output.entrySet()) {
            actor.addItem(entry.getKey(), entry.getValue());
        }
        List<String> levelNotes = new ArrayList<>();
        List<String> characterNotes = new ArrayList<>();
        if (activeTask.profession != null && !activeTask.profession.isBlank()) {
            for (int i = 0; i < activeTask.participants.size(); i++) {
                Actor participant = activeTask.participants.get(i);
                int xp = i == 0 ? activeTask.professionXp : Math.max(1, activeTask.professionXp / 2);
                levelNotes.addAll(participant.gainProfessionXp(activeTask.profession, xp));
                if (activeTask.characterXp > 0) {
                    int characterXp = i == 0 ? activeTask.characterXp : Math.max(1, activeTask.characterXp / 2);
                    for (String note : participant.gainXp(characterXp)) {
                        characterNotes.add(participant.name + ": " + note);
                    }
                }
            }
        }
        int earnedCharacterXp = activeTask.characterXp;
        activeTask = null;
        String levels = levelNotes.isEmpty() ? "" : " Skills improved: " + String.join(", ", levelNotes) + ".";
        String regularXp = earnedCharacterXp <= 0 ? "" : " +" + earnedCharacterXp + " XP.";
        if (!characterNotes.isEmpty()) {
            regularXp += " " + String.join(" ", characterNotes);
        }
        return "Finished: +" + label + "." + regularXp + levels;
    }

    private static Recipe recipe(
            String key,
            String name,
            Workstation workstation,
            Map<String, Integer> cost,
            String resultKey,
            int resultAmount,
            int ticks,
            String description
    ) {
        return recipe(key, name, workstation, cost, resultKey, resultAmount, ticks,
                defaultRecipeProfession(workstation, cost), Map.of(), description);
    }

    private static Recipe recipe(
            String key,
            String name,
            Workstation workstation,
            Map<String, Integer> cost,
            String resultKey,
            int resultAmount,
            int ticks,
            String profession,
            Map<String, Integer> professionRequirements,
            String description
    ) {
        return new Recipe(key, name, workstation, cost, resultKey, resultAmount, ticks,
                profession, Map.copyOf(professionRequirements), categoryForRecipe(key, resultKey, cost), description);
    }

    private static RecipeCategory categoryForRecipe(String key, String resultKey, Map<String, Integer> cost) {
        if (resultKey == null) {
            return RecipeCategory.MATERIAL;
        }
        if (resultKey.contains("seed") || key.contains("seed")) {
            return RecipeCategory.SEED;
        }
        if (resultKey.startsWith("village_prop_")) {
            return RecipeCategory.DECOR;
        }
        if (resultKey.endsWith("_axe") || resultKey.endsWith("_pickaxe") || resultKey.contains("pickaxe")
                || resultKey.contains("woodcutter")) {
            return RecipeCategory.TOOL;
        }
        Item item = GameData.ITEMS.get(resultKey);
        if (item != null) {
            return RecipeCategory.CONSUMABLE;
        }
        Equipment equipment = GameData.equipment(resultKey);
        if (equipment != null) {
            String slot = equipment.slot();
            if ("weapon".equals(slot)) {
                return RecipeCategory.WEAPON;
            }
            if ("ring".equals(slot) || "necklace".equals(slot) || "belt".equals(slot)) {
                return RecipeCategory.ACCESSORY;
            }
            return RecipeCategory.ARMOR;
        }
        if (cost.containsKey("herb_leaf") || cost.containsKey("wild_meat") || cost.containsKey("raw_fish")
                || cost.containsKey("garden_vegetables") || cost.containsKey("coconut")) {
            return RecipeCategory.CONSUMABLE;
        }
        return RecipeCategory.MATERIAL;
    }

    private static String defaultRecipeProfession(Workstation workstation, Map<String, Integer> cost) {
        if (workstation == Workstation.OVEN) {
            return Profession.COOKING.id();
        }
        if (cost.containsKey("wool") || cost.containsKey("plant_fiber") || cost.containsKey("palm_frond")) {
            return Profession.WEAVING.id();
        }
        if (cost.containsKey("skin") || cost.containsKey("scale")) {
            return Profession.LEATHERWORKING.id();
        }
        return Profession.CRAFTING.id();
    }

    private static void rollMonsterLoot(String monsterKey, Random random, Map<String, Integer> loot) {
        switch (monsterKey) {
            case "sheep" -> addLoot(loot, "wool", 1 + random.nextInt(2));
            case "crystal_hare" -> {
                addLoot(loot, "skin", 1);
                if (random.nextDouble() < 0.55) {
                    addLoot(loot, "crystal_dust", 1);
                }
                if (random.nextDouble() < 0.35) {
                    addLoot(loot, "wild_meat", 1);
                }
            }
            case "mountain_goat", "stoneback_goat" -> {
                addLoot(loot, "wool", 1);
                if (random.nextDouble() < 0.65) {
                    addLoot(loot, "horn", 1);
                }
                if (random.nextDouble() < 0.45) {
                    addLoot(loot, "wild_meat", 1);
                }
                if ("stoneback_goat".equals(monsterKey) && random.nextDouble() < 0.45) {
                    addLoot(loot, "stone", 1);
                }
            }
            case "doe" -> {
                addLoot(loot, "skin", 1);
                if (random.nextDouble() < 0.55) {
                    addLoot(loot, "wild_meat", 1);
                }
            }
            case "stag", "moss_stag" -> {
                addLoot(loot, "skin", 1);
                addLoot(loot, "horn", 1);
                if (random.nextDouble() < 0.65) {
                    addLoot(loot, "wild_meat", 1);
                }
                if ("moss_stag".equals(monsterKey) && random.nextDouble() < 0.5) {
                    addLoot(loot, "herb_leaf", 1);
                }
            }
            case "bramble_boar" -> {
                addLoot(loot, "skin", 1);
                addLoot(loot, "wild_meat", 1);
                if (random.nextDouble() < 0.60) {
                    addLoot(loot, "horn", 1);
                }
                if (random.nextDouble() < 0.45) {
                    addLoot(loot, "plant_fiber", 1);
                }
            }
            case "snow_lynx" -> {
                addLoot(loot, "skin", 1);
                if (random.nextDouble() < 0.55) {
                    addLoot(loot, "wild_meat", 1);
                }
                if (random.nextDouble() < 0.55) {
                    addLoot(loot, "frost_shard", 1);
                }
            }
            case "wolf", "meadow_wolf", "frost_wolf" -> {
                addLoot(loot, "skin", 1);
                if (random.nextDouble() < 0.45) {
                    addLoot(loot, "wild_meat", 1);
                }
                if ("frost_wolf".equals(monsterKey)) {
                    addLoot(loot, "frost_shard", 1);
                }
            }
            case "spider", "thornling" -> {
                if (random.nextDouble() < 0.65) {
                    addLoot(loot, "venom_sac", 1);
                }
            }
            case "sand_stalker", "bog_beast", "glass_scorpion", "reed_serpent", "river_eel", "ash_scorpion",
                    "marsh_drake", "mountain_drake" -> {
                addLoot(loot, "scale", 1);
                if (random.nextDouble() < 0.5) {
                    addLoot(loot, "skin", 1);
                }
                if ("glass_scorpion".equals(monsterKey) || "ash_scorpion".equals(monsterKey)) {
                    addLoot(loot, "venom_sac", 1);
                }
                if ("ash_scorpion".equals(monsterKey) && random.nextDouble() < 0.45) {
                    addLoot(loot, "ember_shard", 1);
                }
                if ("reed_serpent".equals(monsterKey) || "river_eel".equals(monsterKey)) {
                    addLoot(loot, "venom_sac", 1);
                }
                if ("marsh_drake".equals(monsterKey) && random.nextDouble() < 0.55) {
                    addLoot(loot, "venom_sac", 1);
                }
                if ("mountain_drake".equals(monsterKey) && random.nextDouble() < 0.55) {
                    addLoot(loot, "stone", 1);
                }
            }
            case "red_dragon", "elder_dragon" -> {
                addLoot(loot, "scale", 2);
                addLoot(loot, "ember_shard", 1 + random.nextInt(2));
                if ("elder_dragon".equals(monsterKey)) {
                    addLoot(loot, "bone", 1);
                }
            }
            case "swamp_troll", "frost_troll" -> {
                addLoot(loot, "skin", 1);
                addLoot(loot, "bone", 1);
                if ("swamp_troll".equals(monsterKey) && random.nextDouble() < 0.55) {
                    addLoot(loot, "venom_sac", 1);
                }
                if ("frost_troll".equals(monsterKey)) {
                    addLoot(loot, "frost_shard", 1);
                }
            }
            case "hill_giant", "stone_giant", "fire_giant" -> {
                addLoot(loot, "bone", 1);
                addLoot(loot, "stone", "hill_giant".equals(monsterKey) ? 1 : 2);
                if (random.nextDouble() < 0.55) {
                    addLoot(loot, "iron_ore", 1);
                }
                if ("fire_giant".equals(monsterKey)) {
                    addLoot(loot, "ember_shard", 1);
                }
            }
            case "ember_tortoise" -> {
                addLoot(loot, "scale", 2);
                if (random.nextDouble() < 0.60) {
                    addLoot(loot, "ember_shard", 1);
                }
                if (random.nextDouble() < 0.35) {
                    addLoot(loot, "stone", 1);
                }
            }
            case "skeleton" -> addLoot(loot, "bone", 1 + random.nextInt(2));
            case "crypt_bat" -> {
                addLoot(loot, "skin", 1);
                if (random.nextDouble() < 0.45) {
                    addLoot(loot, "bone", 1);
                }
            }
            case "ember_imp" -> addLoot(loot, "ember_shard", 1);
            case "ice_golem" -> {
                addLoot(loot, "stone", 2);
                addLoot(loot, "frost_shard", 1);
            }
            case "goblin", "goblin_scout", "goblin_archer", "goblin_trapper", "goblin_skirmisher",
                    "goblin_shaman", "hobgoblin_guard", "goblin_warlord", "goblin_king",
                    "bandit_cutthroat", "bandit_archer", "bandit_captain",
                    "orc", "orc_raider", "orc_berserker", "orc_shaman", "orc_shieldbearer", "orc_champion" -> {
                if (random.nextDouble() < 0.7) {
                    addLoot(loot, "skin", 1);
                }
                if (random.nextDouble() < 0.35) {
                    addLoot(loot, "iron_ore", 1);
                }
            }
            default -> {
                if (random.nextDouble() < 0.35) {
                    addLoot(loot, "bone", 1);
                }
            }
        }
    }

    private static void addLoot(Map<String, Integer> loot, String key, int amount) {
        if (amount > 0) {
            loot.merge(key, amount, Integer::sum);
        }
    }

    private enum PropProfile {
        NONE,
        WOOD,
        PALM,
        COCONUT,
        SHELL,
        DRIFTWOOD,
        SALVAGE,
        STONE,
        CRYSTAL,
        PLANT,
        FLOWER,
        MUSHROOM,
        REED,
        FARM,
        BONE,
        CLAY,
        FIRE
    }

    private static String lootLabel(Map<String, Integer> loot) {
        if (loot.isEmpty()) {
            return "no crafting ingredients";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : loot.entrySet()) {
            parts.add(entry.getValue() + " " + GameData.itemName(entry.getKey()));
        }
        return String.join(", ", parts);
    }

    private static final class ActiveTask {
        final String actionLabel;
        final int totalTicks;
        final Map<String, Integer> output;
        final int worldTickAdvance;
        final String profession;
        final int professionXp;
        final int characterXp;
        final List<Actor> participants;
        int remainingTicks;

        ActiveTask(String actionLabel, int totalTicks, Map<String, Integer> output, int worldTickAdvance,
                   String profession, int professionXp, int characterXp, List<Actor> participants) {
            this.actionLabel = actionLabel;
            this.totalTicks = Math.max(1, totalTicks);
            this.remainingTicks = this.totalTicks;
            this.output = new LinkedHashMap<>(output);
            this.worldTickAdvance = Math.max(1, worldTickAdvance);
            this.profession = profession == null ? "" : profession;
            this.professionXp = Math.max(0, professionXp);
            this.characterXp = Math.max(0, characterXp);
            this.participants = List.copyOf(participants == null ? List.of() : participants);
        }
    }
}
