package com.alderfall.game;

/** Workplace-specific production rules, shared by simulation and forecasts. */
public final class SettlementEconomy {
    private SettlementEconomy() {}
    public record Output(String resource, String rare, String profession, int base) {}
    public static Output output(String style) {
        return switch (style) {
            case "forestry_hut" -> new Output("wood", "plant_fiber", Profession.WOODCUTTING.id(), 2);
            case "mine" -> new Output("stone", "iron_ore", Profession.MINING.id(), 2);
            case "hunting_camp" -> new Output("wild_meat", "skin", Profession.SURVIVAL.id(), 1);
            case "fishing_hut" -> new Output("raw_fish", "bone", Profession.FISHING.id(), 2);
            case "farmstead", "granary" -> new Output("trail_rations", "herb_leaf", Profession.COOKING.id(), 2);
            case "bakery" -> new Output("vegetable_stew", "trail_rations", Profession.COOKING.id(), 1);
            case "apothecary" -> new Output("herbal_salve", "potion_small", Profession.ALCHEMY.id(), 1);
            case "garden" -> new Output("herb_leaf", "potion_small", Profession.ALCHEMY.id(), 2);
            case "blacksmith" -> new Output("iron_ingot", "steel_ingot", Profession.SMITHING.id(), 1);
            case "shop" -> new Output("wood", "iron_ore", Profession.CARPENTRY.id(), 2);
            case "warehouse" -> new Output("plant_fiber", "wood", Profession.SURVIVAL.id(), 1);
            default -> new Output("", "", Profession.CRAFTING.id(), 0);
        };
    }
    public static int housing(String style, int tier) {
        int level=Math.max(1,Math.min(6,tier));
        return switch(style) { case "house" -> level*2; case "row" -> level*4; default -> 0; };
    }
    public static int slots(String style,int tier) {
        return housing(style,tier)>0?0:1+(Math.max(1,Math.min(6,tier))-1)/2;
    }
    public static int skill(Actor worker, Output output) {
        return worker.professionLevel(output.profession())+worker.professionPracticeBonus(output.profession());
    }
    public static int amount(Output output,int tier,int skill,int tools) {
        return output.resource().isBlank()?0:output.base()+(tier-1)/2+Math.max(0,skill-1)/3+Math.min(3,tools/2);
    }
    public static double rareChance(int tier,int skill,int tools) {
        return Math.min(.65,.03+Math.max(0,skill-1)*.025+Math.max(0,tier-1)*.025+tools*.035);
    }
    public static String rareResource(String style,int tier,int skill,int tools) {
        if(tier>=3 && skill>=6 && tools>=2) return switch(style) {
            case "mine" -> "silver_ore";
            case "forestry_hut" -> "ironwood";
            case "blacksmith" -> "froststeel_ingot";
            case "apothecary" -> "new_potion_emberwarm_elixir";
            default -> output(style).rare();
        };
        return output(style).rare();
    }
    public static int income(String style,int tier) {
        if(housing(style,tier)>0)return 0;
        return switch(style) { case "inn", "guild", "watchtower", "shrine", "shop", "bakery", "apothecary" -> tier; default -> Math.max(0,tier-1); };
    }
}
