package com.alderfall.game;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VillageManager {
    public static final boolean FREE_BUILD_MODE = true;

    private VillageManager() {
    }

    public record VillageCost(int gold, Map<String, Integer> items) {
        public static VillageCost free() {
            return new VillageCost(0, Map.of());
        }

        public static VillageCost of(int gold, Map<String, Integer> items) {
            return new VillageCost(gold, Map.copyOf(items));
        }

        public boolean isFree() {
            return gold <= 0 && items.isEmpty();
        }
    }

    public record BuildingPlan(
            String style,
            String label,
            int width,
            int depth,
            VillageCost cost,
            List<String> sprites,
            String description,
            int maxLevel,
            int storageCapacity,
            String workerRole,
            Map<Integer, VillageCost> upgradeCosts
    ) {
        public int[] size() {
            return new int[]{width, depth};
        }
    }

    public record PlaceableAsset(
            String asset,
            String label,
            int size,
            VillageCost cost,
            String category
    ) {
        public PlaceableAsset(String asset, String label, int size, VillageCost cost) {
            this(asset, label, size, cost, "Decor");
        }
    }

    public record TilePlan(
            char tile,
            String label,
            VillageCost cost,
            String description
    ) {
    }

    public record WorkerRole(
            String id,
            String label,
            String resource,
            int baseAmount,
            String requiredBuildingStyle,
            char preferredTile,
            String profession,
            String description
    ) {
    }

    public record SettlementStage(
            int stage,
            String title,
            String kind,
            int requiredPlayerLevel,
            int requiredBuildings,
            int requiredAllies,
            int requiredCompletedQuests,
            int requiredStorageUsed,
            int requiredDevelopedTiles,
            String asset,
            String description
    ) {
    }

    private static final List<SettlementStage> SETTLEMENT_STAGES = List.of(
            stage(1, "Ember Camp", "Camp", 1, 0, 0, 0, 0, 0, "player_village_stage_01",
                    "A fire, a tent, and enough resolve to call the place yours."),
            stage(2, "Surveyed Camp", "Camp", 2, 1, 0, 0, 0, 4, "player_village_stage_02",
                    "A second shelter and marked plots turn the camp into a plan."),
            stage(3, "Palisade Camp", "Camp", 3, 2, 1, 1, 5, 8, "player_village_stage_03",
                    "Stakes, stores, and a posted board give the camp a public heart."),
            stage(4, "First Homesteads", "Homestead", 4, 3, 1, 2, 10, 14, "player_village_stage_04",
                    "Permanent homes begin replacing canvas and rough bedrolls."),
            stage(5, "Oathstead Hamlet", "Hamlet", 5, 5, 2, 3, 20, 22, "player_village_stage_05",
                    "Gardens, wells, and cottages make Oathstead feel lived in."),
            stage(6, "Working Village", "Village", 7, 7, 2, 4, 30, 32, "player_village_stage_06",
                    "Work yards and storage sheds let the settlement support itself."),
            stage(7, "Market Village", "Village", 9, 9, 3, 5, 45, 44, "player_village_stage_07",
                    "A hall, watch post, and market space draw people to stay."),
            stage(8, "Walled Township", "Township", 11, 11, 4, 7, 60, 58, "player_village_stage_08",
                    "Walls and denser streets mark Oathstead as a true township."),
            stage(9, "Prosperous Town", "Town", 14, 14, 5, 9, 80, 74, "player_village_stage_09",
                    "Stonework, towers, and paved squares give the town weight."),
            stage(10, "Oathstead Town", "Town", 16, 16, 6, 12, 100, 92, "player_village_stage_10",
                    "A fortified town now anchors the road instead of hiding from it.")
    );

    private static final List<BuildingPlan> BUILDINGS = List.of(
            building("house", "Cottage", 3, 3, VillageCost.of(20, Map.of("wood", 2)),
                    List.of("imagegen_city_cottage", "city_building_town_gabled", "city_building_town_small", "city_building_town_tall"),
                    "Housing for future villagers and companions.", 3, 0, "",
                    Map.of(2, VillageCost.of(35, Map.of("wood", 3, "stone", 1)),
                            3, VillageCost.of(60, Map.of("wood", 5, "stone", 3)))),
            building("warehouse", "Warehouse", 5, 4, VillageCost.of(35, Map.of("wood", 6, "stone", 3)),
                    List.of("village_building_warehouse", "imagegen_city_warehouse", "village_building_granary"),
                    "Stores gathered village materials for construction and crafting.", 3, 80, "hauler",
                    Map.of(2, VillageCost.of(45, Map.of("wood", 5, "stone", 5)),
                            3, VillageCost.of(80, Map.of("wood", 8, "stone", 8, "iron_ore", 2)))),
            building("forestry_hut", "Forestry Hut", 4, 3, VillageCost.of(25, Map.of("wood", 5)),
                    List.of("village_building_forestry_hut", "village_building_workshop"),
                    "Unlocks steady wood gathering for assigned foresters.", 3, 0, "forester",
                    Map.of(2, VillageCost.of(40, Map.of("wood", 6, "stone", 2)),
                            3, VillageCost.of(70, Map.of("wood", 9, "iron_ore", 2)))),
            building("mine", "Mine", 4, 4, VillageCost.of(30, Map.of("wood", 4, "stone", 6)),
                    List.of("village_building_mine", "city_building_stone_shop"),
                    "Unlocks stone and ore work for assigned miners.", 3, 0, "miner",
                    Map.of(2, VillageCost.of(50, Map.of("wood", 4, "stone", 8)),
                            3, VillageCost.of(90, Map.of("stone", 10, "iron_ore", 4)))),
            building("hunting_camp", "Hunting Camp", 4, 3, VillageCost.of(25, Map.of("wood", 4, "skin", 1)),
                    List.of("village_building_hunting_camp", "location_camp_tent"),
                    "Unlocks meat, hide, bone, and horn gathering for hunters.", 3, 0, "hunter",
                    Map.of(2, VillageCost.of(45, Map.of("wood", 5, "skin", 2)),
                            3, VillageCost.of(75, Map.of("wood", 8, "bone", 3)))),
            building("farmstead", "Farmstead", 4, 3, VillageCost.of(25, Map.of("wood", 3, "stone", 1)),
                    List.of("village_building_farmstead", "village_building_garden"),
                    "Improves food yields from assigned farmers and nearby fields.", 3, 0, "farmer",
                    Map.of(2, VillageCost.of(40, Map.of("wood", 4, "stone", 2)),
                            3, VillageCost.of(70, Map.of("wood", 6, "stone", 4)))),
            building("shop", "Workshop", 4, 3, VillageCost.of(35, Map.of("wood", 4, "stone", 2)),
                    List.of("village_building_workshop", "imagegen_city_workshop", "city_building_town_shop"),
                    "A crafting yard for tools, repairs, and future production chains.", 3, 0, "builder",
                    Map.of(2, VillageCost.of(50, Map.of("wood", 5, "stone", 4)),
                            3, VillageCost.of(90, Map.of("wood", 7, "stone", 6, "iron_ore", 3)))),
            building("inn", "Hall", 5, 4, VillageCost.of(45, Map.of("wood", 5, "stone", 2)),
                    List.of("imagegen_city_hall", "city_building_town_gabled", "city_building_town_shop"),
                    "A gathering hall that supports larger village crews.", 3, 0, "",
                    Map.of(2, VillageCost.of(60, Map.of("wood", 6, "stone", 4)),
                            3, VillageCost.of(100, Map.of("wood", 10, "stone", 6)))),
            building("guild", "Study", 4, 3, VillageCost.of(40, Map.of("wood", 3, "stone", 4)),
                    List.of("imagegen_city_study", "city_building_stone_shop", "city_building_stone_hall"),
                    "Planning space for future research and specialist upgrades.", 3, 0, "",
                    Map.of(2, VillageCost.of(60, Map.of("stone", 6, "iron_ore", 1)),
                            3, VillageCost.of(110, Map.of("stone", 9, "iron_ore", 3)))),
            building("row", "Longhouse", 5, 3, VillageCost.of(30, Map.of("wood", 5)),
                    List.of("imagegen_city_row", "city_building_town_narrow", "city_building_town_gabled"),
                    "Compact housing for a growing camp.", 3, 0, "",
                    Map.of(2, VillageCost.of(45, Map.of("wood", 6, "stone", 1)),
                            3, VillageCost.of(75, Map.of("wood", 9, "stone", 3)))),
            building("granary", "Granary", 3, 3, VillageCost.of(30, Map.of("wood", 4)),
                    List.of("village_building_granary", "city_building_house_wide"),
                    "Adds modest food-oriented storage and keeps crops dry.", 3, 35, "farmer",
                    Map.of(2, VillageCost.of(45, Map.of("wood", 5, "stone", 2)),
                            3, VillageCost.of(75, Map.of("wood", 8, "stone", 4)))),
            building("watchtower", "Watchtower", 2, 3, VillageCost.of(35, Map.of("wood", 4, "stone", 2)),
                    List.of("village_building_watchtower", "city_building_stone_tower"),
                    "A defensive lookout for later threats and patrol work.", 3, 0, "",
                    Map.of(2, VillageCost.of(45, Map.of("wood", 4, "stone", 4)),
                            3, VillageCost.of(85, Map.of("stone", 7, "iron_ore", 2)))),
            building("shrine", "Shrine", 3, 2, VillageCost.of(30, Map.of("stone", 3)),
                    List.of("village_building_shrine", "city_building_stone_hall"),
                    "A morale structure for future blessings and recovery bonuses.", 3, 0, "",
                    Map.of(2, VillageCost.of(45, Map.of("stone", 5)),
                            3, VillageCost.of(85, Map.of("stone", 7, "ember_shard", 1)))),
            building("garden", "Garden", 4, 2, VillageCost.of(20, Map.of("wood", 2)),
                    List.of("village_building_garden", "city_building_house_gabled"),
                    "A small tended plot that helps farmers produce more food.", 3, 0, "farmer",
                    Map.of(2, VillageCost.of(35, Map.of("wood", 3, "stone", 1)),
                            3, VillageCost.of(60, Map.of("wood", 5, "stone", 2)))),
            building("blacksmith", "Blacksmith", 4, 3, VillageCost.of(45, Map.of("wood", 4, "stone", 5, "iron_ore", 2)),
                    List.of("village_building_blacksmith"),
                    "Turns gathered ore into tools and future gear upgrades.", 3, 0, "builder",
                    Map.of(2, VillageCost.of(65, Map.of("wood", 5, "stone", 6, "iron_ore", 3)),
                            3, VillageCost.of(110, Map.of("stone", 8, "iron_ore", 6, "ember_shard", 1)))),
            building("bakery", "Bakery", 3, 3, VillageCost.of(32, Map.of("wood", 3, "stone", 2, "trail_rations", 1)),
                    List.of("village_building_bakery"),
                    "A food workshop that helps farmers turn harvests into supplies.", 3, 20, "farmer",
                    Map.of(2, VillageCost.of(48, Map.of("wood", 4, "stone", 3)),
                            3, VillageCost.of(78, Map.of("wood", 6, "stone", 5)))),
            building("apothecary", "Apothecary", 3, 3, VillageCost.of(38, Map.of("wood", 3, "stone", 2, "herb_leaf", 2)),
                    List.of("village_building_apothecary"),
                    "A herb shop for future medicine, poultices, and recovery chains.", 3, 0, "",
                    Map.of(2, VillageCost.of(55, Map.of("wood", 4, "stone", 3, "herb_leaf", 3)),
                            3, VillageCost.of(90, Map.of("stone", 5, "herb_leaf", 6)))),
            building("fishing_hut", "Fishing Hut", 3, 3, VillageCost.of(28, Map.of("wood", 4, "skin", 1)),
                    List.of("village_building_fishing_hut"),
                    "A small waterside trade hut that supports food gathering.", 3, 0, "fisher",
                    Map.of(2, VillageCost.of(44, Map.of("wood", 5, "skin", 2)),
                            3, VillageCost.of(72, Map.of("wood", 7, "bone", 2))))
    );

    private static final List<PlaceableAsset> OUTDOOR_ASSETS = List.of(
            new PlaceableAsset("city_prop_flower_pot", "Flower Pot", 40, VillageCost.free(), "Decor"),
            new PlaceableAsset("deco_flowers", "Flowers", 34, VillageCost.free(), "Decor"),
            new PlaceableAsset("city_lantern", "Lantern", 38, VillageCost.of(8, Map.of("iron_ore", 1)), "Decor"),
            new PlaceableAsset("village_prop_well", "Well", 46, VillageCost.of(10, Map.of("stone", 2)), "Decor"),
            new PlaceableAsset("village_prop_notice_board", "Notice Board", 42, VillageCost.of(0, Map.of("wood", 1)), "Decor"),
            new PlaceableAsset("village_prop_bench", "Bench", 38, VillageCost.of(0, Map.of("wood", 1)), "Decor"),
            new PlaceableAsset("village_prop_wash_line", "Wash Line", 48, VillageCost.of(0, Map.of("wood", 1, "plant_fiber", 1)), "Decor"),
            new PlaceableAsset("village_prop_clay_oven", "Clay Oven", 44, VillageCost.of(8, Map.of("clay", 2, "stone", 1)), "Decor"),
            new PlaceableAsset("deco_tree_oak", "Oak Tree", 58, VillageCost.of(0, Map.of("wood", 2)), "Trees"),
            new PlaceableAsset("deco_tree_pine", "Pine Tree", 58, VillageCost.of(0, Map.of("wood", 2)), "Trees"),
            new PlaceableAsset("deco_tree_round", "Round Tree", 56, VillageCost.of(0, Map.of("wood", 2)), "Trees"),
            new PlaceableAsset("deco_tree_blue_pine", "Blue Pine", 60, VillageCost.of(0, Map.of("wood", 2)), "Trees"),
            new PlaceableAsset("deco_tree_young", "Young Tree", 48, VillageCost.of(0, Map.of("wood", 1)), "Trees"),
            new PlaceableAsset("deco_pine_sapling", "Pine Sapling", 42, VillageCost.of(0, Map.of("wood", 1)), "Trees"),
            new PlaceableAsset("deco_forest_broadleaf_cluster", "Broadleaf Cluster", 64, VillageCost.of(0, Map.of("wood", 3)), "Trees"),
            new PlaceableAsset("deco_forest_pine_cluster", "Pine Cluster", 64, VillageCost.of(0, Map.of("wood", 3)), "Trees"),
            new PlaceableAsset("deco_forest_mixed_cluster", "Mixed Tree Cluster", 64, VillageCost.of(0, Map.of("wood", 3)), "Trees"),
            new PlaceableAsset("deco_forest_log", "Fallen Log", 48, VillageCost.of(0, Map.of("wood", 1)), "Trees"),
            new PlaceableAsset("deco_stump", "Stump", 38, VillageCost.of(0, Map.of("wood", 1)), "Trees"),
            new PlaceableAsset("deco_bush", "Bush", 38, VillageCost.of(0, Map.of("plant_fiber", 1)), "Plants"),
            new PlaceableAsset("deco_grass_clump", "Grass Clump", 34, VillageCost.of(0, Map.of("plant_fiber", 1)), "Plants"),
            new PlaceableAsset("deco_grass_wildflowers", "Wildflowers", 34, VillageCost.of(0, Map.of("flower_blossom", 1)), "Plants"),
            new PlaceableAsset("deco_grass_herb_patch", "Herb Patch", 34, VillageCost.of(0, Map.of("herb_leaf", 1)), "Plants"),
            new PlaceableAsset("deco_soft_daisy_patch", "Daisy Patch", 34, VillageCost.of(0, Map.of("flower_blossom", 1)), "Plants"),
            new PlaceableAsset("deco_soft_purple_flowers", "Purple Flowers", 34, VillageCost.of(0, Map.of("flower_blossom", 1)), "Plants"),
            new PlaceableAsset("deco_soft_yellow_flowers", "Yellow Flowers", 34, VillageCost.of(0, Map.of("flower_blossom", 1)), "Plants"),
            new PlaceableAsset("deco_soft_dense_leafy_plant", "Leafy Plant", 38, VillageCost.of(0, Map.of("plant_fiber", 1)), "Plants"),
            new PlaceableAsset("deco_soft_dense_tall_grass", "Tall Grass", 38, VillageCost.of(0, Map.of("plant_fiber", 1)), "Plants"),
            new PlaceableAsset("deco_forest_fern", "Fern", 36, VillageCost.of(0, Map.of("plant_fiber", 1)), "Plants"),
            new PlaceableAsset("deco_mushrooms", "Mushrooms", 34, VillageCost.of(0, Map.of("mushroom_spores", 1)), "Plants"),
            new PlaceableAsset("deco_forest_mushrooms", "Forest Mushrooms", 36, VillageCost.of(0, Map.of("mushroom_spores", 1)), "Plants"),
            new PlaceableAsset("deco_rocks", "Rocks", 38, VillageCost.of(0, Map.of("stone", 1)), "Rocks"),
            new PlaceableAsset("deco_soft_mossy_boulder", "Mossy Boulder", 44, VillageCost.of(0, Map.of("stone", 2)), "Rocks"),
            new PlaceableAsset("deco_soft_mossy_rock", "Mossy Rock", 38, VillageCost.of(0, Map.of("stone", 1)), "Rocks"),
            new PlaceableAsset("deco_soft_flat_stones", "Flat Stones", 36, VillageCost.of(0, Map.of("stone", 1)), "Rocks"),
            new PlaceableAsset("deco_grass_stone_stack", "Stone Stack", 40, VillageCost.of(0, Map.of("stone", 2)), "Rocks"),
            new PlaceableAsset("deco_mountain_cairn", "Cairn", 40, VillageCost.of(0, Map.of("stone", 2)), "Rocks"),
            new PlaceableAsset("deco_mountain_crystal_cluster", "Crystal Cluster", 42, VillageCost.of(0, Map.of("crystal_dust", 1)), "Rocks"),
            new PlaceableAsset("deco_grass_pond", "Grass Pond", 48, VillageCost.free(), "Water"),
            new PlaceableAsset("deco_imagen_mossy_pond", "Mossy Pond", 48, VillageCost.free(), "Water"),
            new PlaceableAsset("deco_reeds", "Reeds", 38, VillageCost.of(0, Map.of("plant_fiber", 1)), "Water"),
            new PlaceableAsset("deco_water_lilies", "Water Lilies", 38, VillageCost.of(0, Map.of("flower_blossom", 1)), "Water"),
            new PlaceableAsset("deco_water_reed_islet", "Reed Islet", 42, VillageCost.of(0, Map.of("plant_fiber", 1)), "Water"),
            new PlaceableAsset("deco_soft_water_lily_white", "White Lily", 34, VillageCost.of(0, Map.of("flower_blossom", 1)), "Water"),
            new PlaceableAsset("deco_soft_water_cattails", "Cattails", 38, VillageCost.of(0, Map.of("plant_fiber", 1)), "Water"),
            new PlaceableAsset("deco_water_driftwood", "Water Driftwood", 38, VillageCost.of(0, Map.of("wood", 1)), "Water"),
            new PlaceableAsset("deco_soft_water_wet_stones", "Wet Stones", 38, VillageCost.of(0, Map.of("stone", 1)), "Water"),
            new PlaceableAsset("deco_marsh_lily_pool", "Lily Pool", 46, VillageCost.free(), "Water"),
            new PlaceableAsset("deco_cactus", "Cactus", 40, VillageCost.of(0, Map.of("plant_fiber", 1)), "Desert"),
            new PlaceableAsset("deco_desert_blooming_cactus", "Blooming Cactus", 40, VillageCost.of(0, Map.of("flower_blossom", 1)), "Desert"),
            new PlaceableAsset("deco_desert_rocks", "Desert Rocks", 38, VillageCost.of(0, Map.of("stone", 1)), "Desert"),
            new PlaceableAsset("deco_desert_oasis_pool", "Oasis Pool", 48, VillageCost.free(), "Desert"),
            new PlaceableAsset("deco_badlands_skull_marker", "Skull Marker", 38, VillageCost.of(0, Map.of("bone", 1)), "Desert"),
            new PlaceableAsset("deco_badlands_totem_stones", "Totem Stones", 42, VillageCost.of(0, Map.of("stone", 2)), "Desert"),
            new PlaceableAsset("deco_badlands_red_spire", "Red Spire", 44, VillageCost.of(0, Map.of("stone", 2)), "Desert"),
            new PlaceableAsset("deco_snow_pine", "Snow Pine", 58, VillageCost.of(0, Map.of("wood", 2)), "Snow"),
            new PlaceableAsset("deco_snow_mound", "Snow Mound", 38, VillageCost.free(), "Snow"),
            new PlaceableAsset("deco_tundra_frost_bush", "Frost Bush", 38, VillageCost.of(0, Map.of("plant_fiber", 1)), "Snow"),
            new PlaceableAsset("deco_tundra_ice_crystals", "Ice Crystals", 40, VillageCost.of(0, Map.of("frost_shard", 1)), "Snow"),
            new PlaceableAsset("deco_tundra_rocks", "Tundra Rocks", 38, VillageCost.of(0, Map.of("stone", 1)), "Snow"),
            new PlaceableAsset("deco_tundra_rune_stone", "Rune Stone", 40, VillageCost.of(0, Map.of("stone", 2)), "Snow"),
            new PlaceableAsset("city_prop_market_green", "Market Stall", 44, VillageCost.of(8, Map.of("wood", 1)), "Trade"),
            new PlaceableAsset("city_prop_crate", "Crate", 38, VillageCost.of(0, Map.of("wood", 1)), "Supplies"),
            new PlaceableAsset("city_prop_barrel", "Barrel", 38, VillageCost.of(0, Map.of("wood", 1)), "Supplies"),
            new PlaceableAsset("location_camp_crates", "Supply Crates", 38, VillageCost.of(0, Map.of("wood", 1)), "Supplies"),
            new PlaceableAsset("village_prop_grain_sacks", "Grain Sacks", 40, VillageCost.free(), "Supplies"),
            new PlaceableAsset("village_prop_seedling_tray", "Seedling Tray", 38, VillageCost.of(0, Map.of("wood", 1, "herb_seed", 1)), "Supplies"),
            new PlaceableAsset("village_prop_tool_rack", "Tool Rack", 42, VillageCost.of(0, Map.of("wood", 1)), "Work"),
            new PlaceableAsset("village_prop_training_dummy", "Training Dummy", 44, VillageCost.of(5, Map.of("wood", 1, "skin", 1)), "Work"),
            new PlaceableAsset("village_prop_anvil_stump", "Anvil Stump", 42, VillageCost.of(4, Map.of("wood", 1, "iron_ore", 1)), "Work"),
            new PlaceableAsset("village_prop_sawhorse", "Sawhorse", 42, VillageCost.of(0, Map.of("wood", 1)), "Work"),
            new PlaceableAsset("village_prop_woodpile", "Woodpile", 40, VillageCost.free(), "Forestry"),
            new PlaceableAsset("village_prop_log_stack", "Log Stack", 42, VillageCost.free(), "Forestry"),
            new PlaceableAsset("village_prop_chopping_block", "Chop Block", 40, VillageCost.free(), "Forestry"),
            new PlaceableAsset("village_prop_ore_cart", "Ore Cart", 42, VillageCost.of(8, Map.of("wood", 1, "iron_ore", 1)), "Mine"),
            new PlaceableAsset("village_prop_ore_pile", "Ore Pile", 40, VillageCost.free(), "Mine"),
            new PlaceableAsset("village_prop_mine_lantern", "Mine Lantern", 38, VillageCost.of(5, Map.of("iron_ore", 1)), "Mine"),
            new PlaceableAsset("village_prop_hay_stack", "Hay Stack", 40, VillageCost.free(), "Farm"),
            new PlaceableAsset("village_prop_hay_bales", "Hay Bales", 40, VillageCost.free(), "Farm"),
            new PlaceableAsset("village_prop_produce_basket", "Produce Basket", 38, VillageCost.free(), "Farm"),
            new PlaceableAsset("village_prop_water_trough", "Water Trough", 42, VillageCost.of(0, Map.of("wood", 1)), "Farm"),
            new PlaceableAsset("village_prop_fence_segment", "Fence Segment", 40, VillageCost.of(0, Map.of("wood", 1)), "Farm"),
            new PlaceableAsset("village_prop_farm_tools", "Farm Tools", 40, VillageCost.of(0, Map.of("wood", 1)), "Farm"),
            new PlaceableAsset("village_prop_beehive", "Beehive", 38, VillageCost.of(5, Map.of("wood", 1)), "Farm"),
            new PlaceableAsset("village_prop_chicken_coop", "Chicken Coop", 44, VillageCost.of(5, Map.of("wood", 2)), "Farm"),
            new PlaceableAsset("village_prop_compost_bin", "Compost Bin", 40, VillageCost.of(0, Map.of("wood", 1, "plant_fiber", 1)), "Farm"),
            new PlaceableAsset("village_prop_drying_rack", "Drying Rack", 42, VillageCost.of(5, Map.of("wood", 1, "skin", 1)), "Hunting"),
            new PlaceableAsset("village_prop_fish_rack", "Fish Rack", 42, VillageCost.of(5, Map.of("wood", 1, "bone", 1)), "Hunting"),
            new PlaceableAsset("village_prop_herb_barrel", "Herb Barrel", 42, VillageCost.of(4, Map.of("wood", 1, "herb_leaf", 1)), "Trade"),
            new PlaceableAsset("village_prop_palm_shade", "Palm Shade", 48, VillageCost.of(4, Map.of("palm_frond", 3, "wood", 1)), "Beach"),
            new PlaceableAsset("village_prop_net_drying_rack", "Net Drying Rack", 48, VillageCost.of(4, Map.of("plant_fiber", 3, "wood", 1)), "Beach"),
            new PlaceableAsset("deco_beach_coconuts", "Coconuts", 34, VillageCost.of(0, Map.of("coconut", 1)), "Beach"),
            new PlaceableAsset("deco_beach_shells", "Shells", 32, VillageCost.of(0, Map.of("seashell", 1)), "Beach"),
            new PlaceableAsset("deco_beach_driftwood", "Driftwood", 38, VillageCost.of(0, Map.of("wood", 1)), "Beach"),
            new PlaceableAsset("deco_beach_grass", "Dune Grass", 34, VillageCost.of(0, Map.of("plant_fiber", 1)), "Beach"),
            new PlaceableAsset("location_camp_fire", "Campfire", 38, VillageCost.free(), "Camp"),
            new PlaceableAsset("player_village_quest_board", "Quest Board", 42, VillageCost.free(), "Camp"),
            new PlaceableAsset("location_camp_tent", "Tent", 46, VillageCost.of(0, Map.of("skin", 1)), "Camp")
    );

    private static final List<PlaceableAsset> INTERIOR_ASSETS = List.of(
            new PlaceableAsset("interior_bed_vertical", "Bed", 48, VillageCost.free()),
            new PlaceableAsset("interior_round_table", "Round Table", 48, VillageCost.free()),
            new PlaceableAsset("interior_side_table", "Side Table", 48, VillageCost.free()),
            new PlaceableAsset("interior_tabletop_place_setting", "Place Setting", 48, VillageCost.free()),
            new PlaceableAsset("interior_tabletop_meal", "Meal Setting", 48, VillageCost.free()),
            new PlaceableAsset("interior_tabletop_candle", "Candle Centerpiece", 48, VillageCost.free()),
            new PlaceableAsset("interior_flower_vase", "Flower Vase", 48, VillageCost.free()),
            new PlaceableAsset("interior_herb_pot", "Herb Pot", 48, VillageCost.free()),
            new PlaceableAsset("interior_flower_pot", "Flower Pot", 48, VillageCost.free()),
            new PlaceableAsset("interior_floor_leafy_plant", "Leafy Plant", 48, VillageCost.free()),
            new PlaceableAsset("interior_floor_sapling_pot", "Sapling Pot", 48, VillageCost.free()),
            new PlaceableAsset("interior_floor_bushy_planter", "Bushy Planter", 48, VillageCost.free()),
            new PlaceableAsset("interior_floor_reed_pot", "Reed Pot", 48, VillageCost.free()),
            new PlaceableAsset("interior_floor_flower_planter", "Flower Planter", 48, VillageCost.free()),
            new PlaceableAsset("interior_seed_bowl", "Seed Bowl", 48, VillageCost.free()),
            new PlaceableAsset("interior_planting_pot", "Planting Pot", 48, VillageCost.free()),
            new PlaceableAsset("interior_sprout_planter", "Sprout Planter", 48, VillageCost.free()),
            new PlaceableAsset("interior_herb_planter", "Herb Planter", 48, VillageCost.free()),
            new PlaceableAsset("interior_wall_flower_pot", "Wall Flower Pot", 48, VillageCost.free()),
            new PlaceableAsset("interior_wall_ivy_planter", "Wall Ivy Planter", 48, VillageCost.free()),
            new PlaceableAsset("interior_wall_sconce_lamp", "Wall Sconce", 48, VillageCost.free()),
            new PlaceableAsset("interior_wall_window_small", "Small Window", 48, VillageCost.free()),
            new PlaceableAsset("interior_wall_window_wide", "Wide Window", 48, VillageCost.free()),
            new PlaceableAsset("interior_wall_plant_shelf", "Plant Shelf", 48, VillageCost.free()),
            new PlaceableAsset("interior_wall_herb_rack", "Wall Herb Rack", 48, VillageCost.free()),
            new PlaceableAsset("interior_wall_crystal_ornament", "Wall Crystal", 48, VillageCost.free()),
            new PlaceableAsset("interior_vine_trellis", "Vine Trellis", 48, VillageCost.free()),
            new PlaceableAsset("interior_rug_runner", "Rug Runner", 48, VillageCost.free()),
            new PlaceableAsset("interior_aquarium_table", "Aquarium Table", 48, VillageCost.free()),
            new PlaceableAsset("interior_bookshelf", "Bookshelf", 48, VillageCost.free()),
            new PlaceableAsset("interior_crates", "Crates", 48, VillageCost.free()),
            new PlaceableAsset("interior_anvil", "Anvil", 48, VillageCost.free()),
            new PlaceableAsset("interior_forge", "Forge", 48, VillageCost.free()),
            new PlaceableAsset("interior_carpenter_table", "Carpenter Table", 48, VillageCost.free()),
            new PlaceableAsset("interior_alchemy_station", "Alchemy Station", 48, VillageCost.free()),
            new PlaceableAsset("interior_mortar_pestle", "Mortar & Pestle", 48, VillageCost.free()),
            new PlaceableAsset("interior_cooking_station", "Cooking Station", 48, VillageCost.free()),
            new PlaceableAsset("interior_cookpot_stand", "Cookpot Stand", 48, VillageCost.free()),
            new PlaceableAsset("interior_herb_drying_rack", "Herb Drying Rack", 48, VillageCost.free()),
            new PlaceableAsset("interior_oven", "Oven", 48, VillageCost.free()),
            new PlaceableAsset("interior_stove", "Stove", 48, VillageCost.free()),
            new PlaceableAsset("interior_shop_counter", "Counter", 48, VillageCost.free()),
            new PlaceableAsset("interior_tavern_bar", "Tavern Bar", 48, VillageCost.free()),
            new PlaceableAsset("interior_hearth_pot", "Hearth Pot", 48, VillageCost.free())
    );

    private static final List<TilePlan> TILES = List.of(
            new TilePlan('A', "Farmland", VillageCost.of(0, Map.of("wood", 1)), "Tilled ground that improves farmer output."),
            new TilePlan('g', "Grass", VillageCost.free(), "Open meadow tiles for paths, yards, and future builds."),
            new TilePlan('f', "Forest", VillageCost.of(0, Map.of("wood", 1)), "Managed tree growth for forester output."),
            new TilePlan('P', "Beach", VillageCost.of(0, Map.of("seashell", 1)), "Warm sand tiles for shore paths and beach props."),
            new TilePlan('r', "Dirt Road", VillageCost.of(0, Map.of("stone", 1)), "Simple village road for layout and access."),
            new TilePlan('q', "Gravel", VillageCost.of(0, Map.of("stone", 1)), "Packed gravel for mine yards, camp edges, and rough paths."),
            new TilePlan('V', "Packed Earth", VillageCost.free(), "Warm worn ground for yards, commons, and work areas."),
            new TilePlan('U', "Plank Walk", VillageCost.of(0, Map.of("wood", 1)), "Raised plank paths for damp village edges and tidy crossings."),
            new TilePlan('p', "Pavement", VillageCost.of(0, Map.of("stone", 2)), "Stone pavement for plazas and building fronts."),
            new TilePlan('l', "Side Street", VillageCost.of(0, Map.of("stone", 2)), "Narrow cobbled lane for alleys and service roads."),
            new TilePlan('j', "Court", VillageCost.of(0, Map.of("stone", 2)), "Residential court paving for cottage clusters."),
            new TilePlan('a', "Market Paving", VillageCost.of(0, Map.of("stone", 2, "wood", 1)), "Warmer paving for stalls, workshops, and trade corners."),
            new TilePlan('y', "Garden Court", VillageCost.of(0, Map.of("wood", 1)), "Tidy green court tile for gardens and soft village squares.")
    );

    private static final List<WorkerRole> WORKER_ROLES = List.of(
            new WorkerRole("idle", "Idle", "", 0, "", 'g', "", "No production assignment."),
            new WorkerRole("farmer", "Farmer", "trail_rations", 1, "farmstead", 'A', Profession.COOKING.id(), "Turns field work into village food stores."),
            new WorkerRole("forester", "Forester", "wood", 2, "forestry_hut", 'f', Profession.WOODCUTTING.id(), "Cuts and seasons wood from managed forest tiles."),
            new WorkerRole("miner", "Miner", "stone", 2, "mine", 'q', Profession.MINING.id(), "Mines stone with a chance to uncover iron ore."),
            new WorkerRole("hunter", "Hunter", "wild_meat", 1, "hunting_camp", 'f', Profession.SURVIVAL.id(), "Brings back meat, hide, bone, and horn."),
            new WorkerRole("fisher", "Fisher", "raw_fish", 1, "fishing_hut", 'w', Profession.FISHING.id(), "Works nets and lines for fish and shoreline supplies."),
            new WorkerRole("hauler", "Hauler", "wood", 1, "warehouse", 'g', Profession.SURVIVAL.id(), "Improves storage throughput and keeps supplies moving."),
            new WorkerRole("builder", "Builder", "stone", 1, "shop", 'g', Profession.CRAFTING.id(), "Slowly gathers construction surplus for upgrades.")
    );

    private static final Map<String, BuildingPlan> BUILDING_BY_STYLE = indexBuildings();
    private static final Map<String, PlaceableAsset> OUTDOOR_BY_ASSET = indexAssets(OUTDOOR_ASSETS);
    private static final Map<String, PlaceableAsset> INTERIOR_BY_ASSET = indexAssets(INTERIOR_ASSETS);
    private static final Map<Character, TilePlan> TILE_BY_CHAR = indexTiles();
    private static final Map<String, WorkerRole> ROLE_BY_ID = indexRoles();

    public static List<BuildingPlan> buildingPlans() {
        return BUILDINGS;
    }

    public static List<PlaceableAsset> outdoorAssets() {
        return OUTDOOR_ASSETS;
    }

    public static List<String> outdoorAssetCategories() {
        Map<String, Boolean> categories = new LinkedHashMap<>();
        for (PlaceableAsset asset : OUTDOOR_ASSETS) {
            categories.put(asset.category(), true);
        }
        return List.copyOf(categories.keySet());
    }

    public static List<PlaceableAsset> outdoorAssets(String category) {
        String selected = outdoorAssetCategories().contains(category) ? category : OUTDOOR_ASSETS.get(0).category();
        return OUTDOOR_ASSETS.stream()
                .filter(asset -> asset.category().equals(selected))
                .toList();
    }

    public static List<PlaceableAsset> interiorAssets() {
        return INTERIOR_ASSETS;
    }

    public static List<TilePlan> tilePlans() {
        return TILES;
    }

    public static List<WorkerRole> workerRoles() {
        return WORKER_ROLES;
    }

    public static List<SettlementStage> settlementStages() {
        return SETTLEMENT_STAGES;
    }

    public static SettlementStage settlementStage(int stage) {
        int index = Math.max(1, Math.min(stage, SETTLEMENT_STAGES.size())) - 1;
        return SETTLEMENT_STAGES.get(index);
    }

    public static SettlementStage nextSettlementStage(int stage) {
        return stage >= SETTLEMENT_STAGES.size() ? null : settlementStage(stage + 1);
    }

    public static int settlementStageFor(
            int playerLevel,
            int buildings,
            int allies,
            int completedQuests,
            int storageUsed,
            int developedTiles
    ) {
        int result = 1;
        for (SettlementStage stage : SETTLEMENT_STAGES) {
            if (playerLevel >= stage.requiredPlayerLevel()
                    && buildings >= stage.requiredBuildings()
                    && allies >= stage.requiredAllies()
                    && completedQuests >= stage.requiredCompletedQuests()
                    && storageUsed >= stage.requiredStorageUsed()
                    && developedTiles >= stage.requiredDevelopedTiles()) {
                result = stage.stage();
            }
        }
        return result;
    }

    public static BuildingPlan buildingPlan(String style) {
        return BUILDING_BY_STYLE.getOrDefault(style, BUILDING_BY_STYLE.get("house"));
    }

    public static boolean isManagedBuildingStyle(String style) {
        return BUILDING_BY_STYLE.containsKey(style);
    }

    public static PlaceableAsset outdoorAsset(String asset) {
        return OUTDOOR_BY_ASSET.getOrDefault(asset, OUTDOOR_ASSETS.get(0));
    }

    public static PlaceableAsset interiorAsset(String asset) {
        return INTERIOR_BY_ASSET.getOrDefault(asset, INTERIOR_ASSETS.get(0));
    }

    public static TilePlan tilePlan(char tile) {
        return TILE_BY_CHAR.getOrDefault(tile, TILE_BY_CHAR.get('g'));
    }

    public static WorkerRole workerRole(String roleId) {
        return ROLE_BY_ID.getOrDefault(roleId, ROLE_BY_ID.get("idle"));
    }

    public static String buildingLabel(String style) {
        return buildingPlan(style).label();
    }

    public static int[] buildingSize(String style) {
        return buildingPlan(style).size();
    }

    public static int outdoorAssetSize(String asset) {
        return outdoorAsset(asset).size();
    }

    public static int interiorAssetSize(String asset) {
        return interiorAsset(asset).size();
    }

    public static List<String> buildingSprites(String style) {
        return buildingPlan(style).sprites();
    }

    public static String primaryBuildingSprite(String style) {
        List<String> sprites = buildingSprites(style);
        return sprites.isEmpty() ? "" : sprites.get(0);
    }

    public static String buildingLevelSprite(String style, int level) {
        String primary = primaryBuildingSprite(style);
        if (primary.isBlank() || level <= 1) {
            return primary;
        }
        return primary + "_lvl" + Math.max(2, Math.min(3, level));
    }

    public static boolean canAfford(Actor actor, VillageCost cost) {
        if (FREE_BUILD_MODE) {
            return true;
        }
        if (actor == null || cost == null) {
            return false;
        }
        if (actor.gold < cost.gold()) {
            return false;
        }
        for (Map.Entry<String, Integer> entry : cost.items().entrySet()) {
            if (actor.inventory.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public static VillageCost upgradeCost(String style, int currentLevel) {
        BuildingPlan plan = buildingPlan(style);
        if (currentLevel >= plan.maxLevel()) {
            return null;
        }
        return plan.upgradeCosts().getOrDefault(currentLevel + 1, VillageCost.free());
    }

    public static int storageCapacity(String style, int level) {
        BuildingPlan plan = buildingPlan(style);
        return Math.max(0, plan.storageCapacity()) * Math.max(1, level);
    }

    public static int productionLevelBonus(String roleId, Map<String, Integer> buildingLevels) {
        WorkerRole role = workerRole(roleId);
        if (role.requiredBuildingStyle().isBlank()) {
            return 0;
        }
        int best = 0;
        for (Map.Entry<String, Integer> entry : buildingLevels.entrySet()) {
            if (entry.getKey().startsWith(role.requiredBuildingStyle() + ":")) {
                best = Math.max(best, entry.getValue());
            }
        }
        return Math.max(0, best - 1);
    }

    public static void spend(Actor actor, VillageCost cost) {
        if (FREE_BUILD_MODE || actor == null || cost == null || cost.isFree()) {
            return;
        }
        actor.gold -= cost.gold();
        for (Map.Entry<String, Integer> entry : cost.items().entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                actor.consumeItem(entry.getKey());
            }
        }
    }

    public static String costLabel(VillageCost cost) {
        if (cost == null || cost.isFree()) {
            return "Free";
        }
        StringBuilder label = new StringBuilder();
        if (cost.gold() > 0) {
            label.append(cost.gold()).append("g");
        }
        for (Map.Entry<String, Integer> entry : cost.items().entrySet()) {
            if (!label.isEmpty()) {
                label.append(", ");
            }
            label.append(entry.getValue()).append(" ").append(GameData.itemName(entry.getKey()));
        }
        return label.toString();
    }

    private static Map<String, BuildingPlan> indexBuildings() {
        Map<String, BuildingPlan> result = new LinkedHashMap<>();
        for (BuildingPlan building : BUILDINGS) {
            result.put(building.style(), building);
        }
        return Map.copyOf(result);
    }

    private static Map<Character, TilePlan> indexTiles() {
        Map<Character, TilePlan> result = new LinkedHashMap<>();
        for (TilePlan tile : TILES) {
            result.put(tile.tile(), tile);
        }
        return Map.copyOf(result);
    }

    private static Map<String, WorkerRole> indexRoles() {
        Map<String, WorkerRole> result = new LinkedHashMap<>();
        for (WorkerRole role : WORKER_ROLES) {
            result.put(role.id(), role);
        }
        return Map.copyOf(result);
    }

    private static Map<String, PlaceableAsset> indexAssets(List<PlaceableAsset> assets) {
        Map<String, PlaceableAsset> result = new LinkedHashMap<>();
        for (PlaceableAsset asset : assets) {
            result.put(asset.asset(), asset);
        }
        return Map.copyOf(result);
    }

    private static BuildingPlan building(
            String style,
            String label,
            int width,
            int depth,
            VillageCost cost,
            List<String> sprites,
            String description,
            int maxLevel,
            int storageCapacity,
            String workerRole,
            Map<Integer, VillageCost> upgradeCosts
    ) {
        return new BuildingPlan(style, label, width, depth, cost, sprites, description, maxLevel, storageCapacity,
                workerRole, Map.copyOf(upgradeCosts));
    }

    private static SettlementStage stage(
            int stage,
            String title,
            String kind,
            int requiredPlayerLevel,
            int requiredBuildings,
            int requiredAllies,
            int requiredCompletedQuests,
            int requiredStorageUsed,
            int requiredDevelopedTiles,
            String asset,
            String description
    ) {
        return new SettlementStage(stage, title, kind, requiredPlayerLevel, requiredBuildings, requiredAllies,
                requiredCompletedQuests, requiredStorageUsed, requiredDevelopedTiles, asset, description);
    }
}
