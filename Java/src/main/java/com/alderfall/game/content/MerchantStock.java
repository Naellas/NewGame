package com.alderfall.game;

import com.alderfall.game.inventory.ItemRarity;
import java.util.*;

/** Trade candidate pools cover catalogued variants; local climate limits their distribution. */
public final class MerchantStock {
    private MerchantStock() {}

    public static String trade(String id) {
        return switch (id) {
            case "shop", "workshop", "forestry_hut", "reedworks" -> "carpenter";
            case "alchemist", "alchemy" -> "apothecary";
            case "garden", "granary" -> "farmstead";
            case "tavern", "smokehouse", "caravanserai" -> "inn";
            case "barracks", "watchtower", "highwall", "rescue_lodge" -> "quartermaster";
            case "riverside", "crypt_vendor" -> "general";
            case "mage_tower", "study" -> "guild";
            default -> id;
        };
    }

    public static List<String> candidates(Shop shop) {
        Set<String> keys = new TreeSet<>(shop.stock());
        keys.addAll(GameData.EQUIPMENT.keySet());
        keys.addAll(GameData.ITEMS.keySet());
        MaterialCatalog.all().forEach(m -> keys.add(m.key()));
        CraftingSystem.RECIPE_BOOKS.keySet().forEach(keys::add);
        for (String vendor : List.of("riverside", "highwall", "crypt_vendor"))
            keys.addAll(AssemblyCrafting.vendorStock(vendor));
        return keys.stream().filter(k -> GameData.itemCost(k) > 0)
                .filter(k -> accepts(shop.trade(), k, shop.stock().contains(k))).toList();
    }

    public static boolean accepts(String tradeId, String key, boolean staple) {
        String trade = trade(tradeId);
        var gear = GameData.equipment(key);
        if (gear != null) {
            // Named unique rewards do not become repeatable shop purchases.
            if (gear.rarity() == ItemRarity.UNIQUE) return false;
            if (key.startsWith("gear1~")) {
                var blueprint = AssemblyCrafting.blueprint(key.split("~", 3)[1]);
                return switch (trade) {
                    case "blacksmith" -> blueprint.profession() == Profession.SMITHING || blueprint.profession() == Profession.ARMORCRAFT;
                    case "carpenter" -> blueprint.profession() == Profession.CARPENTRY;
                    case "tailor" -> blueprint.profession() == Profession.TAILORING || blueprint.profession() == Profession.LEATHERWORKING;
                    case "hunting_camp" -> blueprint.id().equals("bow") || blueprint.profession() == Profession.LEATHERWORKING;
                    case "guild" -> blueprint.id().equals("staff") || blueprint.id().equals("robe") || blueprint.profession() == Profession.JEWELLERY;
                    case "shrine" -> blueprint.profession() == Profession.JEWELLERY;
                    case "general", "quartermaster" -> true;
                    default -> false;
                };
            }
            boolean weapon = gear.slot().equals("weapon");
            boolean bow = key.contains("bow");
            boolean staff = contains(key, "staff", "wand", "scepter", "crook");
            boolean woodenShield = key.contains("shield") && contains(key, "oak", "thorn", "reed", "wood");
            return switch (trade) {
                case "blacksmith" -> !bow && !staff && !contains(key, "robe", "cloth", "silk", "leather", "jacket", "jerkin", "cloak", "mantle")
                        && (weapon || Set.of("shield", "head", "helmet", "chestpiece", "gloves", "boots", "leggings", "pauldrons", "shoulders", "belt").contains(gear.slot()));
                case "carpenter" -> bow || staff || woodenShield || key.equals("woodcutter_axe");
                case "guild" -> staff || contains(key, "robe", "archive", "lens", "ring");
                case "shrine" -> !weapon && contains(key, "charm", "medallion", "pendant", "rosary", "prayer");
                case "tailor", "hunting_camp" -> bow && trade.equals("hunting_camp")
                        || !weapon && contains(key, "leather", "hide", "cloth", "wool", "felt", "linen", "jerkin", "cloak", "jacket", "hood", "mantle");
                case "general", "quartermaster" -> true;
                default -> staple && !Set.of("farmstead", "apothecary", "bakery", "inn", "mine", "warehouse").contains(trade);
            };
        }
        var material = MaterialCatalog.get(key);
        if (material != null) {
            return switch (trade) {
                case "blacksmith" -> material.family() == MaterialCatalog.Family.METAL && key.endsWith("_ingot") || key.equals("coal");
                case "mine" -> key.endsWith("_ore") || Set.of("coal", "stone", "flint", "raw_amber", "obsidian_shard", "rock_salt", "steel_scrap").contains(key);
                case "carpenter" -> material.family() == MaterialCatalog.Family.WOOD || Set.of("plant_fiber", "skin", "polished_amber").contains(key);
                case "apothecary" -> material.family() == MaterialCatalog.Family.REAGENT
                        || Set.of("herb_seed", "crystal_dust", "venom_sac", "ember_shard", "frost_shard").contains(key);
                case "farmstead" -> Set.of("garden_vegetables", "herb_seed", "herb_leaf", "plant_fiber", "wool", "fruitwood", "flower_blossom", "coconut").contains(key);
                case "inn", "bakery" -> Set.of("garden_vegetables", "wild_meat", "raw_fish", "herb_leaf", "refined_salt", "coconut").contains(key);
                case "tailor", "hunting_camp" -> material.family() == MaterialCatalog.Family.CLOTH || material.family() == MaterialCatalog.Family.HIDE || key.equals("wild_meat");
                case "fishing_hut" -> Set.of("raw_fish", "seashell", "plant_fiber", "refined_salt").contains(key);
                case "guild", "shrine" -> material.family() == MaterialCatalog.Family.GEM;
                case "general", "warehouse" -> true;
                default -> staple;
            };
        }
        if (GameData.ITEMS.containsKey(key) && !CraftingSystem.isRecipeBookItem(key)) {
            boolean food = contains(key, "rations", "meat", "stew", "broth", "tea");
            return switch (trade) {
                case "apothecary" -> !food || key.equals("focus_tea");
                case "inn", "bakery", "farmstead", "fishing_hut" -> food;
                case "general", "quartermaster" -> true;
                default -> staple;
            };
        }
        // Books and specialized tools retain their explicitly curated trade placement.
        return staple;
    }

    public static boolean local(String key, InteriorStyle region) {
        // Applies to raw/refined resources and to material names embedded in assembled gear.
        if (contains(key, "sun", "ember", "palm_", "coconut", "obsidian", "dune")
                && region != InteriorStyle.SUNREALM) return false;
        if (contains(key, "frost", "rime", "snow", "cobalt", "mithril")
                && region != InteriorStyle.STORMBOUND) return false;
        if (contains(key, "bog_iron", "cypress", "marsh", "mire", "bogsong", "reed")
                && region != InteriorStyle.FENLANDS) return false;
        if (contains(key, "verdant", "ancient_wood", "magic_wood", "enchanted_bark", "glowroot")
                && region != InteriorStyle.HEARTHLANDS && region != InteriorStyle.THORNMERE && region != InteriorStyle.ARCHIVE) return false;
        return true;
    }

    private static boolean contains(String key, String... words) {
        for (String word : words) if (key.contains(word)) return true;
        return false;
    }
}
