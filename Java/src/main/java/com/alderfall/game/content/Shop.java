package com.alderfall.game;

import com.alderfall.game.inventory.Equipment;
import java.util.List;

public record Shop(String id, String name, List<String> stock, String trade) {
    public Shop(String id, String name, List<String> stock) {
        this(id, name, stock, id);
    }
    /** Jobs are authoritative, including when a worker commutes to the overworld. */
    public static Shop forNpc(Npc npc) {
        if (npc == null) return null;
        if (npc.job() != null) {
            String trade = switch (npc.job().kind()) {
                case FARMER -> "farmstead";
                case HERBALIST -> "apothecary";
                case WOODCUTTER -> "forestry_hut";
            };
            return new Shop(trade, npc.name() + "'s Goods", baseStock(trade), trade);
        }
        return npc.shopId() == null ? null : GameData.SHOPS.get(npc.shopId());
    }

    public static Shop forTrade(String trade, String name) {
        return new Shop(trade, name, baseStock(trade));
    }

    public static String regionalTrade(String theme) {
        return switch (theme) {
            case "granary" -> "farmstead";
            case "smokehouse", "caravanserai" -> "inn";
            case "reedworks", "bellhouse" -> "carpenter";
            case "ferry_lodge" -> "fishing_hut";
            case "rescue_lodge" -> "highwall";
            case "cistern_house" -> "apothecary";
            default -> null;
        };
    }

    public static List<String> baseStock(String style) {
        return switch (style) {
            case "blacksmith" -> List.of("iron_sword", "steel_sword", "riveted_mail", "guard_cuirass", "iron_kite_shield");
            case "shop", "carpenter", "workshop" -> List.of("wood", "oak_wood", "birch_wood", "pine_wood", "willow_wood", "plant_fiber", "new_weapon_willow_shortbow", "oaken_roundshield", "woodcutter_axe", "recipe_book_oak_bow");
            case "apothecary", "alchemist", "alchemy" -> List.of("herb_leaf", "herb_seed", "flower_blossom", "mushroom_spores", "herbal_salve", "focus_tea", "potion_small", "potion_large", "ether", "guard_tonic", "recipe_book_apothecary_salve");
            case "inn", "tavern" -> List.of("trail_rations", "vegetable_stew", "cooked_meat", "focus_tea", "recipe_book_camp_cookery");
            case "bakery" -> List.of("trail_rations", "garden_vegetables", "herb_leaf", "recipe_book_camp_cookery");
            case "warehouse" -> List.of("wood", "stone", "plant_fiber", "clay", "trail_rations");
            case "forestry_hut" -> List.of("wood", "oak_wood", "birch_wood", "pine_wood", "woodcutter_axe", "recipe_book_oak_bow");
            case "mine" -> List.of("stone", "iron_ore", "copper_ore", "tin_ore", "coal", "iron_pickaxe");
            case "hunting_camp" -> List.of("potion_small", "scout_hood", "ranger_jerkin", "recipe_book_fisher_knots");
            case "farmstead", "garden", "granary" -> List.of("garden_vegetables", "herb_seed", "plant_fiber", "wool", "trail_rations", "recipe_book_camp_cookery");
            case "fishing_hut" -> List.of("raw_fish", "cooked_meat", "shell_lure", "plant_fiber", "recipe_book_fisher_knots");
            case "tailor" -> List.of("wool", "plant_fiber", "skin", "scout_leathers", "traveler_cloak");
            case "guild" -> List.of("ether", "archive_lens", "recipe_book_escape_scrolls");
            case "shrine" -> List.of("potion_small", "ether", "sunward_medallion");
            case "watchtower" -> List.of("iron_sword", "river_buckler", "guard_tonic");
            case "house", "row" -> List.of("potion_small", "trail_rations", "traveler_cloak");
            default -> List.of("potion_small", "trail_rations");
        };
    }

    public static List<String> upgradedStock(String style) {
        return switch (style) {
            case "blacksmith" -> List.of("steel_bastion_plate", "mountain_plate", "emberforged_plate", "starforged_plate");
            case "shop", "carpenter", "workshop" -> List.of("maple_wood", "ash_wood", "elder_wood", "ironwood", "thornwall_shield");
            case "apothecary", "alchemist", "alchemy" -> List.of("battle_kit", "new_potion_moonmilk_salve", "glowroot");
            case "inn", "tavern", "bakery" -> List.of("wild_meat", "refined_salt");
            case "warehouse" -> List.of("iron_ingot", "copper_ingot", "refined_salt");
            case "forestry_hut" -> List.of("maple_wood", "ash_wood", "ironwood");
            case "mine" -> List.of("silver_ore", "gold_ore", "cobalt_ore");
            case "hunting_camp" -> List.of("stormhide_jacket", "thornsilk_armor", "obsidian_fang");
            case "farmstead", "garden", "granary" -> List.of("herb_leaf", "flower_blossom", "fruitwood");
            case "fishing_hut" -> List.of("marshrunner_mantle", "marshlight_seal");
            case "tailor" -> List.of("wool_cloth", "silk_cloth", "hardened_leather");
            case "guild" -> List.of("starweave_robes", "starrelic_ring", "voidglass_staff");
            case "shrine" -> List.of("suncloth_mantle", "sunwarden_plate", "phoenix_crown_pin");
            case "watchtower" -> List.of("towerguard_shield", "royal_heater", "stormguard_plate");
            case "house", "row" -> List.of("river_pearl_charm", "thornroot_charm");
            default -> List.of("battle_kit");
        };
    }

    public List<String> availableStock(int level) {
        return stock.stream()
                .filter(key -> availableAt(key, level))
                .toList();
    }

    public static boolean availableAt(String key, int level) {
        Equipment equipment = GameData.equipment(key);
        if (equipment != null) return equipment.isAvailableAt(level);
        var item = GameData.ITEMS.get(key);
        if (item != null) return level >= item.minLevel();
        var material = MaterialCatalog.get(key);
        return material == null || level >= material.level();
    }
}
