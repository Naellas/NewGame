package com.alderfall.game;

import com.alderfall.game.inventory.Equipment;
import com.alderfall.game.inventory.Item;
import com.alderfall.game.inventory.ItemRarity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

final class EquipmentCatalog {
    private static final Map<String, Item> ITEMS = ItemCatalog.items();

    private static final String AFFIX_SEPARATOR = "__";
    private record GearAffix(
            String key,
            String prefix,
            String suffix,
            ItemRarity minRarity,
            int attackBonus,
            int defenseBonus,
            int hpBonus,
            int mpBonus,
            int strengthBonus,
            int intelligenceBonus,
            int dexterityBonus,
            int charismaBonus,
            int constitutionBonus,
            int willpowerBonus,
            int spellDamageBonus,
            int critChanceBonus,
            int critDamageBonus,
            int healingPowerBonus,
            int damageReductionBonus,
            int costPercent,
            String description
    ) {
        String displayName(String baseName) {
            if (!prefix.isBlank()) {
                return prefix + " " + baseName;
            }
            return baseName + " " + suffix;
        }
    }

    private static final Map<String, GearAffix> GEAR_AFFIXES = Map.ofEntries(
            affix("keen", "Keen", "", ItemRarity.COMMON, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 2, 5, 0, 0, 18, "Sharper balance improves crit rate and crit damage."),
            affix("stout", "Stout", "", ItemRarity.COMMON, 0, 1, 8, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 16, "Extra bracing improves HP and damage resistance."),
            affix("focused", "Focused", "", ItemRarity.UNCOMMON, 0, 0, 0, 6, 0, 1, 0, 0, 0, 1, 3, 0, 0, 0, 0, 22, "A steadier channel improves MP and spell damage."),
            affix("mending", "Mending", "", ItemRarity.UNCOMMON, 0, 0, 6, 4, 0, 0, 0, 1, 0, 1, 0, 0, 0, 4, 0, 22, "A soft ward improves recovery and healing power."),
            affix("swift", "Swift", "", ItemRarity.UNCOMMON, 1, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 3, 3, 0, 0, 24, "Light fittings improve dexterity and critical tempo."),
            affix("bulwark", "", "of the Bulwark", ItemRarity.RARE, 0, 3, 18, 0, 1, 0, 0, 0, 2, 1, 0, 0, 0, 0, 3, 35, "Heavy reinforcement improves defense and resistance."),
            affix("arcane", "", "of the Arcane", ItemRarity.RARE, 0, 0, 0, 12, 0, 2, 0, 0, 0, 2, 7, 0, 0, 0, 0, 38, "Ley-cut fittings strongly improve spell damage."),
            affix("surgical", "Surgical", "", ItemRarity.RARE, 2, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 5, 10, 2, 0, 40, "Precise weighting improves criticals and triage work.")
    );
    private static final List<String> BOSS_LOOT_DAWN = List.of(
            "new_weapon_dawn_crown_brand", "new_armor_dawn_crown_regalia",
            "new_accessory_dawn_crown_phoenix_pin", "new_potion_dawn_crown_elixir"
    );
    private static final List<String> BOSS_LOOT_WORLDROOT = List.of(
            "new_weapon_worldroot_scythe", "new_armor_worldroot_carapace",
            "new_accessory_worldroot_seedstone", "new_potion_worldroot_panacea"
    );
    private static final List<String> BOSS_LOOT_TWELFTH = List.of(
            "new_weapon_the_twelfth_silence", "new_armor_mantle_of_the_twelfth",
            "new_accessory_seal_of_the_twelfth", "new_potion_twelfth_silence_flask"
    );
    private static final List<String> BOSS_LOOT_GLASS = List.of(
            "new_weapon_glass_choir_scepter", "new_armor_glass_choir_robes",
            "new_accessory_glass_choir_halo", "new_potion_glass_choir_serum"
    );
    private static final List<String> BOSS_LOOT_THORN = List.of(
            "new_weapon_vesper_thornbow", "new_armor_vesper_thornveil",
            "new_accessory_vesper_thornheart", "new_potion_vesper_thorn_salve"
    );
    private static final List<String> BOSS_LOOT_OATH = List.of(
            "new_weapon_oathroot_cleaver", "new_armor_oathroot_pauldrons",
            "new_accessory_oathroot_promise_band", "new_potion_oathroot_fortifier"
    );
    private static final List<String> LOW_BOSS_LOOT = List.of(
            "new_weapon_the_bellringer", "new_armor_bellringer_plate",
            "new_accessory_the_bellringer_clapper", "new_potion_bellringer_incense",
            "new_weapon_glass_choir_scepter", "new_armor_glass_choir_robes"
    );
    private static final List<String> HIGH_BOSS_LOOT = List.of(
            "new_weapon_dawn_crown_brand", "new_weapon_alderfall_starcutter",
            "new_armor_dawn_crown_regalia", "new_accessory_alderfall_starrelic",
            "new_potion_dawn_crown_elixir", "new_potion_worldroot_panacea"
    );
    private static final List<String> DUNGEON_LOW_LOOT = List.of(
            "new_weapon_gravesalt_flail", "new_weapon_archive_thornstaff",
            "new_armor_gravesalt_cowl", "new_accessory_archive_pageweight",
            "new_potion_archive_ink_tonic", "new_potion_gravesalt_cleanser"
    );
    private static final List<String> DUNGEON_HIGH_LOOT = List.of(
            "new_weapon_nightwater_trident", "new_weapon_glass_choir_scepter",
            "new_armor_nightwater_mantle", "new_accessory_nightwater_signet",
            "new_potion_nightwater_draught", "new_potion_glass_choir_serum"
    );
    public static final Map<String, Equipment> EQUIPMENT = Map.ofEntries(
            equipment("rusty_sword", "Rusty Sword", "weapon", "icon_sword", 1, 0, 0, 0, 8, "Old steel with one useful edge"),
            equipment("iron_sword", "Iron Sword", "weapon", "icon_sword", 3, 0, 0, 0, 25, "A sturdy blade of forged iron"),
            equipment("steel_sword", "Steel Sword", "weapon", "icon_sword", 6, 0, 0, 0, 60, "A keen edge of tempered steel"),
            equipment("enchanted_blade", "Enchanted Blade", "weapon", "icon_sword", 10, 0, 0, 4, 120, "A blade infused with ancient magic"),
            equipment("stormbrand", "Stormbrand", "weapon", "icon_sword", 13, 0, 0, 6, 190, "A bright blade that hums before rain"),
            equipment("hunter_knife", "Hunter Knife", "weapon", "icon_sword", 3, 1, 0, 0, 28, "Fast, balanced, and easy to carry"),
            equipment("oak_bow", "Oak Bow", "weapon", "icon_sword", 4, 0, 0, 0, 30, "A reliable ranger weapon"),
            equipment("ash_bow", "Ash Bow", "weapon", "icon_sword", 7, 0, 0, 0, 65, "Carved from hardened ash wood"),
            equipment("storm_bow", "Storm Bow", "weapon", "icon_sword", 11, 0, 0, 3, 145, "Its string snaps like distant thunder"),
            equipment("shadow_dagger", "Shadow Dagger", "weapon", "icon_sword", 8, 1, 0, 4, 115, "A quiet blade that vanishes in poor light"),
            equipment("silver_mace", "Silver Mace", "weapon", "icon_shield", 5, 2, 4, 8, 110, "A polished mace used by road chaplains"),
            equipment("flame_staff", "Flame Staff", "weapon", "icon_potion_blue", 2, 0, 0, 8, 55, "Crackling with fire magic"),
            equipment("frost_staff", "Frost Staff", "weapon", "icon_potion_blue", 3, 0, 0, 10, 75, "Radiates an icy chill"),
            equipment("star_staff", "Star Staff", "weapon", "icon_potion_blue", 5, 0, 0, 16, 160, "A focus for patient, precise spellwork"),
            equipment("traveler_cloak", "Traveler Cloak", "chestpiece", "icon_chest", 0, 1, 4, 0, 15, "Weathered cloth with hidden stitching"),
            equipment("leather_vest", "Leather Vest", "chestpiece", "icon_chest", 0, 2, 0, 0, 20, "Light and flexible protection"),
            equipment("iron_mail", "Iron Mail", "chestpiece", "icon_chest", 0, 4, 6, 0, 50, "Protective iron plating"),
            equipment("steel_plate", "Steel Plate", "chestpiece", "icon_chest", 0, 6, 12, 0, 100, "Heavy steel armor"),
            equipment("warden_plate", "Warden Plate", "chestpiece", "icon_shield", 0, 8, 18, 0, 170, "Built to hold a line when the line breaks"),
            equipment("mage_robes", "Mage Robes", "chestpiece", "icon_potion_blue", 0, 1, 0, 6, 40, "Imbued with magical resonance"),
            equipment("silkweave_robes", "Silkweave Robes", "chestpiece", "icon_potion_blue", 0, 3, 0, 12, 105, "Soft robes threaded with warding sigils"),
            equipment("ranger_coat", "Ranger Coat", "chestpiece", "icon_chest", 0, 4, 8, 3, 90, "Quiet leather reinforced for long roads"),
            equipment("acolyte_mantle", "Acolyte Mantle", "chestpiece", "icon_potion_blue", 0, 3, 10, 8, 95, "Layered cloth bright with stitched wards"),
            equipment("night_leathers", "Night Leathers", "chestpiece", "icon_chest", 2, 3, 6, 4, 100, "Soft black leather cut for quick movement"),
            equipment("scout_hood", "Scout Hood", "helmet", "icon_chest", 0, 1, 3, 2, 32, "A hood with a weatherproof brow"),
            equipment("iron_helm", "Iron Helm", "helmet", "icon_shield", 0, 3, 5, 0, 58, "Plain iron protection for plain bad days"),
            equipment("warded_pauldrons", "Warded Pauldrons", "pauldrons", "icon_shield", 0, 3, 6, 3, 80, "Shoulder plates etched with small warding marks"),
            equipment("duelist_gloves", "Duelist Gloves", "gloves", "icon_sword", 2, 1, 0, 2, 56, "Grip-stiffened gloves for quick work"),
            equipment("guard_gauntlets", "Guard Gauntlets", "gloves", "icon_shield", 1, 3, 4, 0, 74, "Heavy gloves with plated knuckles"),
            equipment("road_belt", "Road Belt", "belt", "icon_chest", 0, 1, 5, 3, 42, "Pouches, buckles, and room for one more useful thing"),
            equipment("focus_sash", "Focus Sash", "belt", "icon_potion_blue", 0, 0, 0, 8, 78, "A sash knotted around a cool blue charm"),
            equipment("trail_leggings", "Trail Leggings", "leggings", "icon_chest", 0, 2, 5, 1, 48, "Reinforced cloth for thorny roads"),
            equipment("steel_greaves", "Steel Greaves", "leggings", "icon_shield", 0, 4, 8, 0, 92, "Leg guards that make retreat sound expensive"),
            equipment("softstep_boots", "Softstep Boots", "boots", "icon_chest", 1, 1, 2, 4, 64, "Quiet soles with a ranger's stitch"),
            equipment("iron_boots", "Iron Boots", "boots", "icon_shield", 0, 3, 6, 0, 70, "Not graceful, but dependable"),
            equipment("iron_ring", "Iron Ring", "ring", "icon_shield", 1, 1, 0, 0, 30, "A simple band of iron"),
            equipment("vitality_charm", "Vitality Charm", "necklace", "icon_potion_red", 0, 0, 8, 0, 45, "Increases maximum health"),
            equipment("mana_stone", "Mana Stone", "necklace", "icon_potion_blue", 0, 0, 0, 6, 50, "Resonates with magical energy"),
            equipment("warrior_brooch", "Warrior Brooch", "necklace", "icon_sword", 3, 0, 4, 0, 70, "A brass pin worn by old captains"),
            equipment("warding_seal", "Warding Seal", "ring", "icon_shield", 0, 3, 0, 4, 75, "A small charm etched with defensive runes"),
            equipment("phoenix_feather", "Phoenix Feather", "necklace", "icon_potion_red", 0, 0, 14, 8, 130, "Warm to the touch and difficult to look away from"),
            equipment("rustbit_saber", "Rustbit Saber", "weapon", "rustbit_saber", 2, 0, 0, 0, 1, 3, 18, "A cheap saber with a stubborn edge"),
            equipment("riverguard_blade", "Riverguard Blade", "weapon", "riverguard_blade", 4, 0, 2, 0, 2, 4, 36, "Militia steel balanced for wet roads"),
            equipment("cinderedge", "Cinderedge", "weapon", "cinderedge", 6, 0, 0, 2, 3, 5, 58, "A blackened blade that holds heat"),
            equipment("frostvein_sword", "Frostvein Sword", "weapon", "frostvein_sword", 7, 1, 0, 2, 4, 6, 78, "Cold metal with pale blue seams"),
            equipment("thorncarver", "Thorncarver", "weapon", "thorncarver", 8, 1, 4, 0, 5, 7, 98, "A hooked sword made for cutting briars and armor straps"),
            equipment("moonlit_rapier", "Moonlit Rapier", "weapon", "moonlit_rapier", 9, 1, 0, 4, 6, 8, 124, "A narrow blade that flashes in dim light"),
            equipment("sunfall_longsword", "Sunfall Longsword", "weapon", "sunfall_longsword", 10, 2, 5, 0, 7, 9, 150, "Bright steel carried by roadwardens"),
            equipment("stormglass_blade", "Stormglass Blade", "weapon", "stormglass_blade", 12, 0, 0, 6, 8, 10, 188, "A glassy edge that hums in rain"),
            equipment("duskwake_sword", "Duskwake Sword", "weapon", "duskwake_sword", 13, 1, 4, 4, 9, 11, 225, "Shadowed steel with a quiet grip"),
            equipment("kingsroad_claymore", "Kingsroad Claymore", "weapon", "kingsroad_claymore", 15, 2, 8, 0, 10, 12, 270, "A broad sword fit for clearing a line"),
            equipment("marshlight_cutlass", "Marshlight Cutlass", "weapon", "marshlight_cutlass", 16, 2, 0, 6, 11, 13, 322, "Curved steel with a pale marsh glow"),
            equipment("obsidian_fang", "Obsidian Fang", "weapon", "obsidian_fang", 18, 1, 0, 8, 12, 14, 390, "A dark volcanic blade that drinks torchlight"),
            equipment("starforged_sword", "Starforged Sword", "weapon", "starforged_sword", 20, 2, 8, 8, 13, 16, 480, "A silver star-metal blade with a singing edge"),
            equipment("heartstone_blade", "Heartstone Blade", "weapon", "heartstone_blade", 23, 3, 12, 8, 15, 18, 610, "Forged around a warm red shard"),
            equipment("oathbreaker_edge", "Oathbreaker Edge", "weapon", "oathbreaker_edge", 26, 2, 10, 12, 18, 99, 760, "A forbidden blade made for ending old promises"),
            equipment("oakheart_staff", "Oakheart Staff", "weapon", "oakheart_staff", 1, 0, 0, 5, 1, 3, 20, "A simple oak focus for first spells"),
            equipment("ashwind_staff", "Ashwind Staff", "weapon", "ashwind_staff", 2, 0, 0, 8, 2, 4, 42, "Light ash wood carved with wind marks"),
            equipment("embercore_staff", "Embercore Staff", "weapon", "embercore_staff", 3, 0, 0, 11, 3, 5, 66, "A warm focus with a coal-bright heart"),
            equipment("frostroot_staff", "Frostroot Staff", "weapon", "frostroot_staff", 3, 1, 0, 13, 4, 6, 86, "Pale rootwood that never thaws"),
            equipment("stormcall_staff", "Stormcall Staff", "weapon", "stormcall_staff", 4, 0, 0, 16, 5, 7, 112, "A forked staff that smells of rain"),
            equipment("moonwell_staff", "Moonwell Staff", "weapon", "moonwell_staff", 4, 0, 4, 18, 6, 8, 140, "A silver focus used for patient healing rites"),
            equipment("sunspire_staff", "Sunspire Staff", "weapon", "sunspire_staff", 5, 1, 6, 20, 7, 9, 170, "Brightly capped and warm in the hand"),
            equipment("thornbinder_staff", "Thornbinder Staff", "weapon", "thornbinder_staff", 6, 1, 8, 21, 8, 10, 205, "Wrapped in living vine and careful knots"),
            equipment("bogsong_staff", "Bogsong Staff", "weapon", "bogsong_staff", 6, 2, 10, 23, 9, 11, 245, "A reed-bound focus that hums near water"),
            equipment("glassdune_staff", "Glassdune Staff", "weapon", "glassdune_staff", 7, 0, 0, 27, 10, 12, 292, "Desert glass set in polished darkwood"),
            equipment("archive_scepter", "Archive Scepter", "weapon", "archive_scepter", 8, 1, 0, 30, 11, 13, 350, "A scholar's focus with exacting sigils"),
            equipment("starfall_staff", "Starfall Staff", "weapon", "starfall_staff", 9, 1, 0, 34, 12, 14, 420, "A staff topped with cold starlight"),
            equipment("heartstone_crook", "Heartstone Crook", "weapon", "heartstone_crook", 10, 2, 16, 34, 13, 16, 510, "A healer's crook set with a red shard"),
            equipment("voidglass_staff", "Voidglass Staff", "weapon", "voidglass_staff", 12, 1, 0, 40, 15, 18, 645, "Black glass bends light around its crown"),
            equipment("dawnweave_staff", "Dawnweave Staff", "weapon", "dawnweave_staff", 13, 2, 10, 44, 18, 99, 790, "A radiant focus woven with gold thread"),
            equipment("woodcutter_axe", "Woodcutter Axe", "weapon", "woodcutter_axe", 3, 0, 2, 0, 1, 3, 22, "A practical axe with battlefield ambitions"),
            equipment("raider_hatchet", "Raider Hatchet", "weapon", "raider_hatchet", 5, 0, 0, 0, 2, 4, 40, "Fast and ugly in close quarters"),
            equipment("ironbeard_axe", "Ironbeard Axe", "weapon", "ironbeard_axe", 7, 1, 0, 0, 3, 5, 68, "An iron axe with a heavy lower bite"),
            equipment("steelcleaver", "Steelcleaver", "weapon", "steelcleaver", 9, 1, 4, 0, 4, 6, 92, "A wide blade made for splitting armor"),
            equipment("frosthew_axe", "Frosthew Axe", "weapon", "frosthew_axe", 10, 1, 0, 3, 5, 7, 118, "A cold axe from the northern pass"),
            equipment("embermaul_axe", "Embermaul Axe", "weapon", "embermaul_axe", 12, 0, 6, 2, 6, 8, 148, "Its head glows at the cutting edge"),
            equipment("stormsplitter", "Stormsplitter", "weapon", "stormsplitter", 13, 1, 0, 5, 7, 9, 180, "Built to crack shields like thunder"),
            equipment("thornbite_axe", "Thornbite Axe", "weapon", "thornbite_axe", 14, 2, 8, 0, 8, 10, 216, "A hooked axe wrapped in barbed vine"),
            equipment("bonehook_axe", "Bonehook Axe", "weapon", "bonehook_axe", 16, 1, 10, 0, 9, 11, 260, "A grim hook for dragging foes off balance"),
            equipment("obsidian_chopper", "Obsidian Chopper", "weapon", "obsidian_chopper", 18, 0, 0, 6, 10, 12, 315, "Volcanic glass honed to a cruel shine"),
            equipment("sunward_axe", "Sunward Axe", "weapon", "sunward_axe", 19, 2, 8, 4, 11, 13, 372, "A bright axe carried by temple guards"),
            equipment("marshreaper", "Marshreaper", "weapon", "marshreaper", 21, 2, 12, 2, 12, 14, 445, "A long axe made for cutting reeds and monsters"),
            equipment("mountainfall_axe", "Mountainfall Axe", "weapon", "mountainfall_axe", 24, 3, 16, 0, 13, 16, 540, "Heavy enough to make stone listen"),
            equipment("royal_halberd_axe", "Royal Halberd Axe", "weapon", "royal_halberd_axe", 27, 3, 12, 4, 15, 18, 680, "A ceremonial weapon that forgot to be harmless"),
            equipment("starbreaker_axe", "Starbreaker Axe", "weapon", "starbreaker_axe", 31, 2, 18, 8, 18, 99, 840, "A massive axe with a star-metal crescent"),
            equipment("river_buckler", "River Buckler", "shield", "river_buckler", 0, 2, 3, 0, 1, 3, 18, "A small shield with a water-dark rim"),
            equipment("oaken_roundshield", "Oaken Roundshield", "shield", "oaken_roundshield", 0, 3, 6, 0, 2, 4, 34, "Layered oak bound in iron"),
            equipment("iron_kite_shield", "Iron Kite Shield", "shield", "iron_kite_shield", 0, 5, 8, 0, 3, 5, 60, "A pointed shield for steady advances"),
            equipment("towerguard_shield", "Towerguard Shield", "shield", "towerguard_shield", 0, 7, 14, 0, 4, 6, 88, "Tall enough to hide behind properly"),
            equipment("frostguard_aegis", "Frostguard Aegis", "shield", "frostguard_aegis", 0, 8, 16, 3, 5, 7, 118, "Cold steel that numbs incoming blows"),
            equipment("emberward_shield", "Emberward Shield", "shield", "emberward_shield", 1, 9, 14, 4, 6, 8, 150, "A shield warm enough to dry rain"),
            equipment("stormwall_shield", "Stormwall Shield", "shield", "stormwall_shield", 0, 11, 18, 5, 7, 9, 184, "It rattles before lightning strikes"),
            equipment("suncrest_shield", "Suncrest Shield", "shield", "suncrest_shield", 1, 12, 22, 4, 8, 10, 222, "A bright crest painted over layered steel"),
            equipment("shadowglass_shield", "Shadowglass Shield", "shield", "shadowglass_shield", 2, 12, 16, 7, 9, 11, 270, "Dark glass backed by black iron"),
            equipment("thornwall_shield", "Thornwall Shield", "shield", "thornwall_shield", 1, 14, 26, 2, 10, 12, 325, "A living shield that grips the arm"),
            equipment("marshreed_ward", "Marshreed Ward", "shield", "marshreed_ward", 0, 15, 30, 5, 11, 13, 386, "Braided reed over hidden steel ribs"),
            equipment("bonebound_shield", "Bonebound Shield", "shield", "bonebound_shield", 2, 16, 28, 4, 12, 14, 460, "Bone plates lashed to a dark core"),
            equipment("royal_heater", "Royal Heater", "shield", "royal_heater", 1, 18, 34, 5, 13, 16, 560, "A polished heater shield with old heraldry"),
            equipment("starforged_aegis", "Starforged Aegis", "shield", "starforged_aegis", 2, 21, 38, 8, 15, 18, 710, "A shield hammered from bright fallen metal"),
            equipment("heartstone_bulwark", "Heartstone Bulwark", "shield", "heartstone_bulwark", 3, 24, 46, 8, 18, 99, 880, "A heavy ward built around a red shard"),
            equipment("padded_gambeson", "Padded Gambeson", "chestpiece", "padded_gambeson", 0, 2, 6, 0, 1, 3, 20, "Quilted cloth with honest stitching"),
            equipment("scout_leathers", "Scout Leathers", "chestpiece", "scout_leathers", 1, 3, 7, 1, 2, 4, 38, "Quiet leather with travel cuts"),
            equipment("ranger_jerkin", "Ranger Jerkin", "chestpiece", "ranger_jerkin", 1, 4, 10, 2, 3, 5, 64, "A reinforced jerkin for long patrols"),
            equipment("duelist_jacket", "Duelist Jacket", "chestpiece", "duelist_jacket", 2, 4, 8, 3, 4, 6, 88, "Cut short for fast footwork"),
            equipment("nightweave_coat", "Nightweave Coat", "chestpiece", "nightweave_coat", 2, 5, 10, 5, 5, 7, 115, "Black leather that swallows torchlight"),
            equipment("marshrunner_mantle", "Marshrunner Mantle", "chestpiece", "marshrunner_mantle", 1, 6, 14, 4, 6, 8, 145, "Waxed cloth that shrugs off damp"),
            equipment("dunewrap_vest", "Dunewrap Vest", "chestpiece", "dunewrap_vest", 2, 6, 12, 6, 7, 9, 176, "Layered desert cloth with hidden plates"),
            equipment("frostleaf_cloak", "Frostleaf Cloak", "chestpiece", "frostleaf_cloak", 1, 7, 16, 7, 8, 10, 212, "A pale cloak that keeps warmth close"),
            equipment("thornsilk_armor", "Thornsilk Armor", "chestpiece", "thornsilk_armor", 3, 7, 18, 5, 9, 11, 255, "Soft armor threaded with green briar silk"),
            equipment("stormhide_jacket", "Stormhide Jacket", "chestpiece", "stormhide_jacket", 2, 8, 16, 9, 10, 12, 306, "Oiled hide that crackles under pressure"),
            equipment("suncloth_mantle", "Suncloth Mantle", "chestpiece", "suncloth_mantle", 1, 9, 22, 8, 11, 13, 365, "Gold-threaded cloth used by healers"),
            equipment("archive_mantle", "Archive Mantle", "chestpiece", "archive_mantle", 0, 9, 14, 14, 12, 14, 430, "A scholar's mantle lined with ward notes"),
            equipment("starweave_robes", "Starweave Robes", "chestpiece", "starweave_robes", 2, 10, 18, 16, 13, 16, 520, "Robes stitched with cold stars"),
            equipment("moonwater_coat", "Moonwater Coat", "chestpiece", "moonwater_coat", 3, 11, 24, 14, 15, 18, 650, "A flowing coat that glimmers like deep water"),
            equipment("heartwood_vest", "Heartwood Vest", "chestpiece", "heartwood_vest", 4, 13, 30, 12, 18, 99, 800, "Living wood plates over supple leather"),
            equipment("riveted_mail", "Riveted Mail", "chestpiece", "riveted_mail", 0, 5, 12, 0, 1, 3, 34, "Basic mail rings on a padded backing"),
            equipment("guard_cuirass", "Guard Cuirass", "chestpiece", "guard_cuirass", 0, 7, 18, 0, 2, 4, 60, "A plain cuirass used by gate guards"),
            equipment("steel_bastion_plate", "Steel Bastion Plate", "chestpiece", "steel_bastion_plate", 0, 9, 24, 0, 3, 5, 92, "Heavy steel with a thick breastplate"),
            equipment("mountain_plate", "Mountain Plate", "chestpiece", "mountain_plate", 0, 11, 30, 0, 4, 6, 130, "Dense armor from cold mountain forges"),
            equipment("boneguard_plate", "Boneguard Plate", "chestpiece", "boneguard_plate", 1, 12, 32, 0, 5, 7, 168, "Bone plates over black mail"),
            equipment("obsidian_plate", "Obsidian Plate", "chestpiece", "obsidian_plate", 1, 13, 28, 4, 6, 8, 210, "Glossy plate with a volcanic bite"),
            equipment("frostbound_plate", "Frostbound Plate", "chestpiece", "frostbound_plate", 0, 15, 38, 4, 7, 9, 256, "Blue-white metal that resists shock"),
            equipment("emberforged_plate", "Emberforged Plate", "chestpiece", "emberforged_plate", 2, 15, 36, 5, 8, 10, 310, "Red seams glow between heavy plates"),
            equipment("stormguard_plate", "Stormguard Plate", "chestpiece", "stormguard_plate", 1, 17, 42, 6, 9, 11, 370, "Armor grounded with copper channels"),
            equipment("sunwarden_plate", "Sunwarden Plate", "chestpiece", "sunwarden_plate", 1, 18, 48, 6, 10, 12, 440, "Bright armor made for holding sacred doors"),
            equipment("shadowplate_harness", "Shadowplate Harness", "chestpiece", "shadowplate_harness", 3, 18, 38, 9, 11, 13, 520, "Black plate with silent leather joins"),
            equipment("thornplate_mail", "Thornplate Mail", "chestpiece", "thornplate_mail", 2, 20, 52, 5, 12, 14, 610, "Layered plates shaped like overlapping leaves"),
            equipment("marshbulwark_plate", "Marshbulwark Plate", "chestpiece", "marshbulwark_plate", 1, 21, 58, 7, 13, 16, 730, "Heavy armor sealed against water and rot"),
            equipment("royal_wardplate", "Royal Wardplate", "chestpiece", "royal_wardplate", 2, 24, 66, 8, 15, 18, 910, "Old royal plate rebuilt with modern wards"),
            equipment("starforged_plate", "Starforged Plate", "chestpiece", "starforged_plate", 3, 28, 78, 10, 18, 99, 1120, "A near-mythic suit of fallen star-metal"),
            equipment("copper_signet", "Copper Signet", "ring", "copper_signet", 1, 1, 2, 0, 1, 3, 18, "A simple ring stamped with a road mark"),
            equipment("river_pearl_charm", "River Pearl Charm", "necklace", "river_pearl_charm", 0, 1, 5, 3, 2, 4, 36, "A small pearl tied on blue cord"),
            equipment("emberglass_ring", "Emberglass Ring", "ring", "emberglass_ring", 2, 0, 0, 5, 3, 5, 62, "A red glass ring that never cools"),
            equipment("frostdrop_pendant", "Frostdrop Pendant", "necklace", "frostdrop_pendant", 0, 2, 6, 5, 4, 6, 84, "A pendant beaded with permanent frost"),
            equipment("stormbead_brooch", "Stormbead Brooch", "necklace", "stormbead_brooch", 2, 1, 0, 7, 5, 7, 110, "A copper brooch that clicks before storms"),
            equipment("sunward_medallion", "Sunward Medallion", "necklace", "sunward_medallion", 1, 2, 10, 6, 6, 8, 140, "A warm medallion favored by field healers"),
            equipment("moonthread_necklace", "Moonthread Necklace", "necklace", "moonthread_necklace", 0, 1, 6, 12, 7, 9, 172, "Silver thread knotted around pale gems"),
            equipment("thornroot_charm", "Thornroot Charm", "belt", "thornroot_charm", 2, 2, 12, 4, 8, 10, 208, "A knotted charm that smells of green earth"),
            equipment("marshlight_seal", "Marshlight Seal", "ring", "marshlight_seal", 1, 3, 14, 6, 9, 11, 250, "A seal ring lit like a distant lantern"),
            equipment("dunestar_talisman", "Dunestar Talisman", "necklace", "dunestar_talisman", 3, 1, 0, 12, 10, 12, 300, "A desert charm cut from pale glass"),
            equipment("archive_lens", "Archive Lens", "necklace", "archive_lens", 1, 2, 0, 18, 11, 13, 360, "A polished lens in a brass setting"),
            equipment("shadowband", "Shadowband", "ring", "shadowband", 4, 2, 6, 10, 12, 14, 430, "A dark ring that blurs at the edges"),
            equipment("heartstone_locket", "Heartstone Locket", "necklace", "heartstone_locket", 1, 3, 26, 12, 13, 16, 525, "A red locket with a steady heartbeat"),
            equipment("starrelic_ring", "Starrelic Ring", "ring", "starrelic_ring", 4, 3, 10, 18, 15, 18, 660, "A ring of silver metal and impossible light"),
            equipment("phoenix_crown_pin", "Phoenix Crown Pin", "necklace", "phoenix_crown_pin", 5, 4, 30, 16, 18, 99, 820, "A bright pin shaped like a rising flame"),
            equipment("new_weapon_ashfall_cutlass", "Ashfall Cutlass", "weapon", "new_weapon_ashfall_cutlass", 2, 0, 4, 0, 1, 5, 18, ItemRarity.COMMON, 0, 2, 4, 0, 0, "", "Common weapon. Requires level 1. Balanced for a distinct combat role."),
            equipment("new_weapon_briarhook_dagger", "Briarhook Dagger", "weapon", "new_weapon_briarhook_dagger", 3, 0, 0, 0, 1, 5, 37, ItemRarity.COMMON, 0, 2, 4, 0, 0, "", "Common weapon. Requires level 1. Balanced for a distinct combat role."),
            equipment("new_weapon_militia_pike", "Militia Pike", "weapon", "new_weapon_militia_pike", 4, 0, 0, 0, 2, 6, 56, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common weapon. Requires level 2. Balanced for a distinct combat role."),
            equipment("new_weapon_willow_shortbow", "Willow Shortbow", "weapon", "new_weapon_willow_shortbow", 5, 0, 0, 0, 2, 6, 75, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common weapon. Requires level 2. Balanced for a distinct combat role."),
            equipment("new_weapon_apprentice_wand", "Apprentice Wand", "weapon", "new_weapon_apprentice_wand", 2, 0, 8, 4, 3, 7, 94, ItemRarity.COMMON, 3, 0, 0, 0, 0, "", "Common weapon. Requires level 3. Balanced for a distinct combat role."),
            equipment("new_weapon_copper_warpick", "Copper Warpick", "weapon", "new_weapon_copper_warpick", 6, 0, 0, 0, 3, 7, 113, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common weapon. Requires level 3. Balanced for a distinct combat role."),
            equipment("new_weapon_roadwatch_mace", "Roadwatch Mace", "weapon", "new_weapon_roadwatch_mace", 7, 0, 0, 0, 4, 8, 132, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common weapon. Requires level 4. Balanced for a distinct combat role."),
            equipment("new_weapon_field_sickle", "Field Sickle", "weapon", "new_weapon_field_sickle", 8, 0, 0, 0, 4, 8, 151, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common weapon. Requires level 4. Balanced for a distinct combat role."),
            equipment("new_weapon_emberglass_saber", "Emberglass Saber", "weapon", "new_weapon_emberglass_saber", 12, 0, 12, 0, 5, 9, 230, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon weapon. Requires level 5. Balanced for a distinct combat role."),
            equipment("new_weapon_frostpine_bow", "Frostpine Bow", "weapon", "new_weapon_frostpine_bow", 13, 0, 0, 0, 5, 9, 255, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon weapon. Requires level 5. Balanced for a distinct combat role."),
            equipment("new_weapon_stormthread_wand", "Stormthread Wand", "weapon", "new_weapon_stormthread_wand", 7, 1, 0, 7, 6, 10, 281, ItemRarity.UNCOMMON, 5, 0, 0, 0, 0, "", "Uncommon weapon. Requires level 6. Balanced for a distinct combat role."),
            equipment("new_weapon_moonlit_dirk", "Moonlit Dirk", "weapon", "new_weapon_moonlit_dirk", 15, 0, 0, 0, 6, 10, 306, ItemRarity.UNCOMMON, 0, 3, 6, 0, 0, "", "Uncommon weapon. Requires level 6. Balanced for a distinct combat role."),
            equipment("new_weapon_oakwarden_spear", "Oakwarden Spear", "weapon", "new_weapon_oakwarden_spear", 16, 0, 16, 0, 7, 11, 332, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon weapon. Requires level 7. Balanced for a distinct combat role."),
            equipment("new_weapon_sunspoke_mace", "Sunspoke Mace", "weapon", "new_weapon_sunspoke_mace", 18, 0, 0, 0, 7, 11, 358, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon weapon. Requires level 7. Balanced for a distinct combat role."),
            equipment("new_weapon_gravesalt_flail", "Gravesalt Flail", "weapon", "new_weapon_gravesalt_flail", 19, 0, 0, 0, 8, 12, 383, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon weapon. Requires level 8. Balanced for a distinct combat role."),
            equipment("new_weapon_duskneedle_rapier", "Duskneedle Rapier", "weapon", "new_weapon_duskneedle_rapier", 20, 1, 0, 0, 8, 12, 409, ItemRarity.UNCOMMON, 0, 3, 7, 0, 0, "", "Uncommon weapon. Requires level 8. Balanced for a distinct combat role."),
            equipment("new_weapon_cobalt_halberd", "Cobalt Halberd", "weapon", "new_weapon_cobalt_halberd", 21, 0, 20, 0, 9, 13, 435, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon weapon. Requires level 9. Balanced for a distinct combat role."),
            equipment("new_weapon_redreef_axe", "Redreef Axe", "weapon", "new_weapon_redreef_axe", 22, 0, 0, 0, 9, 13, 460, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon weapon. Requires level 9. Balanced for a distinct combat role."),
            equipment("new_weapon_archive_thornstaff", "Archive Thornstaff", "weapon", "new_weapon_archive_thornstaff", 15, 0, 0, 11, 10, 16, 648, ItemRarity.RARE, 8, 0, 0, 0, 0, "", "Rare weapon. Requires level 10. Balanced for a distinct combat role."),
            equipment("new_weapon_rimehook_blade", "Rimehook Blade", "weapon", "new_weapon_rimehook_blade", 33, 0, 0, 0, 10, 16, 682, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare weapon. Requires level 10. Balanced for a distinct combat role."),
            equipment("new_weapon_lanternfall_bow", "Lanternfall Bow", "weapon", "new_weapon_lanternfall_bow", 34, 2, 24, 0, 11, 17, 716, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare weapon. Requires level 11. Balanced for a distinct combat role."),
            equipment("new_weapon_widowglass_kris", "Widowglass Kris", "weapon", "new_weapon_widowglass_kris", 36, 0, 0, 0, 11, 17, 751, ItemRarity.RARE, 0, 4, 9, 0, 0, "", "Rare weapon. Requires level 11. Balanced for a distinct combat role."),
            equipment("new_weapon_thunderroot_maul", "Thunderroot Maul", "weapon", "new_weapon_thunderroot_maul", 37, 0, 0, 0, 12, 18, 785, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare weapon. Requires level 12. Balanced for a distinct combat role."),
            equipment("new_weapon_mirechant_staff", "Mirechant Staff", "weapon", "new_weapon_mirechant_staff", 19, 0, 0, 13, 12, 18, 819, ItemRarity.RARE, 9, 0, 0, 0, 0, "", "Rare weapon. Requires level 12. Balanced for a distinct combat role."),
            equipment("new_weapon_kingsmark_glaive", "Kingsmark Glaive", "weapon", "new_weapon_kingsmark_glaive", 40, 0, 28, 0, 13, 19, 853, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare weapon. Requires level 13. Balanced for a distinct combat role."),
            equipment("new_weapon_hearthflame_falchion", "Hearthflame Falchion", "weapon", "new_weapon_hearthflame_falchion", 42, 3, 0, 0, 13, 19, 887, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare weapon. Requires level 13. Balanced for a distinct combat role."),
            equipment("new_weapon_starshard_wand", "Starshard Wand", "weapon", "new_weapon_starshard_wand", 21, 0, 0, 15, 14, 20, 922, ItemRarity.RARE, 10, 0, 0, 0, 0, "", "Rare weapon. Requires level 14. Balanced for a distinct combat role."),
            equipment("new_weapon_nightwater_trident", "Nightwater Trident", "weapon", "new_weapon_nightwater_trident", 45, 0, 0, 0, 14, 20, 956, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare weapon. Requires level 14. Balanced for a distinct combat role."),
            equipment("new_weapon_the_bellringer", "The Bellringer", "weapon", "new_weapon_the_bellringer", 61, 0, 32, 0, 15, 99, 1292, ItemRarity.UNIQUE, 0, 2, 0, 0, 0, "First critical strike each battle hits harder.", "Unique weapon. Requires level 15. First critical strike each battle hits harder."),
            equipment("new_weapon_glass_choir_scepter", "Glass Choir Scepter", "weapon", "new_weapon_glass_choir_scepter", 31, 0, 0, 16, 16, 99, 1337, ItemRarity.UNIQUE, 13, 2, 0, 0, 0, "Damaging spells leave a faint echo in the target.", "Unique weapon. Requires level 16. Damaging spells leave a faint echo in the target."),
            equipment("new_weapon_oathroot_cleaver", "Oathroot Cleaver", "weapon", "new_weapon_oathroot_cleaver", 65, 3, 0, 0, 17, 99, 1382, ItemRarity.UNIQUE, 0, 2, 0, 0, 0, "Defending steadies nearby allies.", "Unique weapon. Requires level 17. Defending steadies nearby allies."),
            equipment("new_weapon_vesper_thornbow", "Vesper Thornbow", "weapon", "new_weapon_vesper_thornbow", 67, 0, 0, 0, 18, 99, 1426, ItemRarity.UNIQUE, 0, 2, 0, 0, 0, "Healing above the cap becomes a small ward.", "Unique weapon. Requires level 18. Healing above the cap becomes a small ward."),
            equipment("new_weapon_samir_sunlance", "Samir Sunlance", "weapon", "new_weapon_samir_sunlance", 69, 0, 36, 0, 19, 99, 1471, ItemRarity.UNIQUE, 0, 2, 0, 0, 0, "Basic attacks bite deeper against weakened foes.", "Unique weapon. Requires level 19. Basic attacks bite deeper against weakened foes."),
            equipment("new_weapon_cassia_gatehammer", "Cassia Gatehammer", "weapon", "new_weapon_cassia_gatehammer", 71, 0, 0, 0, 20, 99, 1516, ItemRarity.UNIQUE, 0, 2, 0, 0, 0, "Starting a turn wounded sharpens focus.", "Unique weapon. Requires level 20. Starting a turn wounded sharpens focus."),
            equipment("new_weapon_maera_marginblade", "Maera Marginblade", "weapon", "new_weapon_maera_marginblade", 73, 0, 0, 0, 21, 99, 1560, ItemRarity.UNIQUE, 0, 2, 0, 0, 0, "Battlefield movement feels lighter after every cast.", "Unique weapon. Requires level 21. Battlefield movement feels lighter after every cast."),
            equipment("new_weapon_lyra_mercy_staff", "Lyra Mercy Staff", "weapon", "new_weapon_lyra_mercy_staff", 37, 4, 0, 19, 22, 99, 1605, ItemRarity.UNIQUE, 15, 2, 0, 0, 0, "The first potion used each battle feels stronger.", "Unique weapon. Requires level 22. The first potion used each battle feels stronger."),
            equipment("new_weapon_dawn_crown_brand", "Dawn-Crown Brand", "weapon", "new_weapon_dawn_crown_brand", 98, 0, 40, 0, 20, 99, 2106, ItemRarity.LEGENDARY, 0, 2, 0, 0, 0, "Legendary property: once per battle, a lethal blow should leave its bearer at one breath when this hook is expanded.", "Legendary weapon. Requires level 20. Legendary property: once per battle, a lethal blow should leave its bearer at one breath when this hook is expanded."),
            equipment("new_weapon_worldroot_scythe", "Worldroot Scythe", "weapon", "new_weapon_worldroot_scythe", 100, 0, 0, 0, 22, 99, 2163, ItemRarity.LEGENDARY, 0, 2, 0, 0, 0, "Legendary property: spell and weapon damage should alternate empowering each other in a future combat pass.", "Legendary weapon. Requires level 22. Legendary property: spell and weapon damage should alternate empowering each other in a future combat pass."),
            equipment("new_weapon_alderfall_starcutter", "Alderfall Starcutter", "weapon", "new_weapon_alderfall_starcutter", 103, 0, 0, 0, 24, 99, 2220, ItemRarity.LEGENDARY, 0, 2, 0, 0, 0, "Legendary property: overhealing should create a party-wide shield pulse when this hook is expanded.", "Legendary weapon. Requires level 24. Legendary property: overhealing should create a party-wide shield pulse when this hook is expanded."),
            equipment("new_weapon_the_twelfth_silence", "The Twelfth Silence", "weapon", "new_weapon_the_twelfth_silence", 52, 0, 0, 21, 26, 99, 2277, ItemRarity.LEGENDARY, 17, 2, 0, 0, 0, "Legendary property: critical hits should briefly slow the enemy turn order when initiative exists.", "Legendary weapon. Requires level 26. Legendary property: critical hits should briefly slow the enemy turn order when initiative exists."),
            equipment("new_armor_mended_linen_hood", "Mended Linen Hood", "helmet", "new_armor_mended_linen_hood", 0, 1, 4, 3, 1, 5, 26, ItemRarity.COMMON, 1, 0, 0, 0, 0, "", "Common armor. Requires level 1. Made to support a specific defensive style."),
            equipment("new_armor_roadmender_gloves", "Roadmender Gloves", "gloves", "new_armor_roadmender_gloves", 1, 2, 3, 0, 1, 5, 45, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common armor. Requires level 1. Made to support a specific defensive style."),
            equipment("new_armor_copper_thread_belt", "Copper-Thread Belt", "belt", "new_armor_copper_thread_belt", 1, 2, 4, 0, 2, 6, 64, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common armor. Requires level 2. Made to support a specific defensive style."),
            equipment("new_armor_patchwork_greaves", "Patchwork Greaves", "leggings", "new_armor_patchwork_greaves", 0, 3, 9, 0, 2, 6, 83, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common armor. Requires level 2. Made to support a specific defensive style."),
            equipment("new_armor_ashguard_boots", "Ashguard Boots", "boots", "new_armor_ashguard_boots", 0, 3, 11, 0, 3, 7, 102, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common armor. Requires level 3. Made to support a specific defensive style."),
            equipment("new_armor_willowhide_jerkin", "Willowhide Jerkin", "chestpiece", "new_armor_willowhide_jerkin", 0, 4, 13, 0, 3, 7, 121, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common armor. Requires level 3. Made to support a specific defensive style."),
            equipment("new_armor_tin_pauldrons", "Tin Pauldrons", "pauldrons", "new_armor_tin_pauldrons", 0, 4, 8, 0, 4, 8, 140, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common armor. Requires level 4. Made to support a specific defensive style."),
            equipment("new_armor_raincloak_mantle", "Raincloak Mantle", "chestpiece", "new_armor_raincloak_mantle", 0, 5, 17, 6, 4, 8, 159, ItemRarity.COMMON, 2, 0, 0, 0, 0, "", "Common armor. Requires level 4. Made to support a specific defensive style."),
            equipment("new_armor_briarwatch_helm", "Briarwatch Helm", "helmet", "new_armor_briarwatch_helm", 0, 7, 25, 0, 5, 9, 238, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon armor. Requires level 5. Made to support a specific defensive style."),
            equipment("new_armor_embersewn_gloves", "Embersewn Gloves", "gloves", "new_armor_embersewn_gloves", 1, 8, 11, 0, 5, 9, 263, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon armor. Requires level 5. Made to support a specific defensive style."),
            equipment("new_armor_frostfelt_sash", "Frostfelt Sash", "belt", "new_armor_frostfelt_sash", 2, 8, 12, 0, 6, 10, 289, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon armor. Requires level 6. Made to support a specific defensive style."),
            equipment("new_armor_stormhide_boots", "Stormhide Boots", "boots", "new_armor_stormhide_boots", 0, 9, 32, 0, 6, 10, 314, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon armor. Requires level 6. Made to support a specific defensive style."),
            equipment("new_armor_moonbuckled_leggings", "Moonbuckled Leggings", "leggings", "new_armor_moonbuckled_leggings", 0, 10, 35, 0, 7, 11, 340, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon armor. Requires level 7. Made to support a specific defensive style."),
            equipment("new_armor_oaken_pauldrons", "Oaken Pauldrons", "pauldrons", "new_armor_oaken_pauldrons", 0, 10, 15, 0, 7, 11, 366, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon armor. Requires level 7. Made to support a specific defensive style."),
            equipment("new_armor_sunwashed_robe", "Sunwashed Robe", "chestpiece", "new_armor_sunwashed_robe", 0, 11, 39, 10, 8, 12, 391, ItemRarity.UNCOMMON, 3, 0, 0, 5, 0, "", "Uncommon armor. Requires level 8. Made to support a specific defensive style."),
            equipment("new_armor_gravesalt_cowl", "Gravesalt Cowl", "helmet", "new_armor_gravesalt_cowl", 0, 12, 42, 10, 8, 12, 417, ItemRarity.UNCOMMON, 4, 0, 0, 0, 0, "", "Uncommon armor. Requires level 8. Made to support a specific defensive style."),
            equipment("new_armor_cobalt_mail", "Cobalt Mail", "chestpiece", "new_armor_cobalt_mail", 0, 13, 44, 0, 9, 13, 443, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon armor. Requires level 9. Made to support a specific defensive style."),
            equipment("new_armor_redreef_waders", "Redreef Waders", "boots", "new_armor_redreef_waders", 0, 13, 47, 0, 9, 13, 468, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon armor. Requires level 9. Made to support a specific defensive style."),
            equipment("new_armor_archive_shoulderplates", "Archive Shoulderplates", "pauldrons", "new_armor_archive_shoulderplates", 0, 19, 20, 0, 10, 16, 656, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare armor. Requires level 10. Made to support a specific defensive style."),
            equipment("new_armor_rimeguard_helm", "Rimeguard Helm", "helmet", "new_armor_rimeguard_helm", 0, 20, 69, 0, 10, 16, 690, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare armor. Requires level 10. Made to support a specific defensive style."),
            equipment("new_armor_lanternscale_coat", "Lanternscale Coat", "chestpiece", "new_armor_lanternscale_coat", 0, 21, 72, 0, 11, 17, 724, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare armor. Requires level 11. Made to support a specific defensive style."),
            equipment("new_armor_widowglass_gauntlets", "Widowglass Gauntlets", "gloves", "new_armor_widowglass_gauntlets", 3, 21, 23, 0, 11, 17, 759, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare armor. Requires level 11. Made to support a specific defensive style."),
            equipment("new_armor_thunderroot_girdle", "Thunderroot Girdle", "belt", "new_armor_thunderroot_girdle", 3, 22, 24, 0, 12, 18, 793, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare armor. Requires level 12. Made to support a specific defensive style."),
            equipment("new_armor_mirechant_waders", "Mirechant Waders", "leggings", "new_armor_mirechant_waders", 0, 23, 82, 0, 12, 18, 827, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare armor. Requires level 12. Made to support a specific defensive style."),
            equipment("new_armor_kingsmark_sabatons", "Kingsmark Sabatons", "boots", "new_armor_kingsmark_sabatons", 0, 24, 85, 0, 13, 19, 861, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare armor. Requires level 13. Made to support a specific defensive style."),
            equipment("new_armor_hearthflame_cuirass", "Hearthflame Cuirass", "chestpiece", "new_armor_hearthflame_cuirass", 0, 25, 88, 0, 13, 19, 895, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare armor. Requires level 13. Made to support a specific defensive style."),
            equipment("new_armor_starshard_hood", "Starshard Hood", "helmet", "new_armor_starshard_hood", 0, 26, 91, 16, 14, 20, 930, ItemRarity.RARE, 6, 0, 0, 0, 0, "", "Rare armor. Requires level 14. Made to support a specific defensive style."),
            equipment("new_armor_nightwater_mantle", "Nightwater Mantle", "chestpiece", "new_armor_nightwater_mantle", 0, 27, 95, 16, 14, 20, 964, ItemRarity.RARE, 6, 0, 0, 0, 0, "", "Rare armor. Requires level 14. Made to support a specific defensive style."),
            equipment("new_armor_bellringer_plate", "Bellringer Plate", "chestpiece", "new_armor_bellringer_plate", 0, 37, 128, 0, 15, 99, 1300, ItemRarity.UNIQUE, 0, 0, 0, 0, 2, "Critical healing has a chance to ripple to another ally.", "Unique armor. Requires level 15. Critical healing has a chance to ripple to another ally."),
            equipment("new_armor_glass_choir_robes", "Glass Choir Robes", "chestpiece", "new_armor_glass_choir_robes", 0, 38, 132, 17, 16, 99, 1345, ItemRarity.UNIQUE, 6, 0, 0, 0, 2, "Damage taken while guarding is partly returned as resolve.", "Unique armor. Requires level 16. Damage taken while guarding is partly returned as resolve."),
            equipment("new_armor_oathroot_pauldrons", "Oathroot Pauldrons", "pauldrons", "new_armor_oathroot_pauldrons", 0, 39, 32, 0, 17, 99, 1390, ItemRarity.UNIQUE, 0, 0, 0, 0, 3, "Attacks against vulnerable foes gain extra crit chance.", "Unique armor. Requires level 17. Attacks against vulnerable foes gain extra crit chance."),
            equipment("new_armor_vesper_thornveil", "Vesper Thornveil", "helmet", "new_armor_vesper_thornveil", 0, 40, 141, 18, 18, 99, 1434, ItemRarity.UNIQUE, 7, 0, 0, 0, 3, "The item remembers one desperate victory and answers in kind.", "Unique armor. Requires level 18. The item remembers one desperate victory and answers in kind."),
            equipment("new_armor_samir_sunmail", "Samir Sunmail", "chestpiece", "new_armor_samir_sunmail", 0, 41, 145, 0, 19, 99, 1479, ItemRarity.UNIQUE, 0, 0, 0, 10, 4, "First critical strike each battle hits harder.", "Unique armor. Requires level 19. First critical strike each battle hits harder."),
            equipment("new_armor_cassia_gateplate", "Cassia Gateplate", "chestpiece", "new_armor_cassia_gateplate", 0, 43, 149, 0, 20, 99, 1524, ItemRarity.UNIQUE, 0, 0, 0, 0, 4, "Damaging spells leave a faint echo in the target.", "Unique armor. Requires level 20. Damaging spells leave a faint echo in the target."),
            equipment("new_armor_maera_marginsash", "Maera Marginsash", "belt", "new_armor_maera_marginsash", 4, 44, 36, 20, 21, 99, 1568, ItemRarity.UNIQUE, 7, 0, 0, 0, 5, "Defending steadies nearby allies.", "Unique armor. Requires level 21. Defending steadies nearby allies."),
            equipment("new_armor_lyra_mercywraps", "Lyra Mercywraps", "gloves", "new_armor_lyra_mercywraps", 4, 45, 37, 0, 22, 99, 1613, ItemRarity.UNIQUE, 0, 0, 0, 10, 5, "Healing above the cap becomes a small ward.", "Unique armor. Requires level 22. Healing above the cap becomes a small ward."),
            equipment("new_armor_dawn_crown_regalia", "Dawn-Crown Regalia", "chestpiece", "new_armor_dawn_crown_regalia", 0, 59, 206, 0, 20, 99, 2114, ItemRarity.LEGENDARY, 0, 0, 0, 11, 6, "Legendary property: once per battle, a lethal blow should leave its bearer at one breath when this hook is expanded.", "Legendary armor. Requires level 20. Legendary property: once per battle, a lethal blow should leave its bearer at one breath when this hook is expanded."),
            equipment("new_armor_worldroot_carapace", "Worldroot Carapace", "chestpiece", "new_armor_worldroot_carapace", 0, 61, 212, 0, 22, 99, 2171, ItemRarity.LEGENDARY, 0, 0, 0, 0, 6, "Legendary property: spell and weapon damage should alternate empowering each other in a future combat pass.", "Legendary armor. Requires level 22. Legendary property: spell and weapon damage should alternate empowering each other in a future combat pass."),
            equipment("new_armor_alderfall_starplate", "Alderfall Starplate", "chestpiece", "new_armor_alderfall_starplate", 0, 62, 217, 0, 24, 99, 2228, ItemRarity.LEGENDARY, 0, 0, 0, 0, 7, "Legendary property: overhealing should create a party-wide shield pulse when this hook is expanded.", "Legendary armor. Requires level 24. Legendary property: overhealing should create a party-wide shield pulse when this hook is expanded."),
            equipment("new_armor_mantle_of_the_twelfth", "Mantle of the Twelfth", "chestpiece", "new_armor_mantle_of_the_twelfth", 0, 64, 223, 22, 26, 99, 2285, ItemRarity.LEGENDARY, 8, 0, 0, 0, 7, "Legendary property: critical hits should briefly slow the enemy turn order when initiative exists.", "Legendary armor. Requires level 26. Legendary property: critical hits should briefly slow the enemy turn order when initiative exists."),
            equipment("new_accessory_tin_luck_ring", "Tin Luck Ring", "ring", "new_accessory_tin_luck_ring", 0, 0, 4, 2, 1, 5, 22, ItemRarity.COMMON, 0, 1, 3, 0, 0, "", "Common accessory. Requires level 1. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_ash_cord_charm", "Ash Cord Charm", "necklace", "new_accessory_ash_cord_charm", 0, 0, 0, 2, 1, 5, 41, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common accessory. Requires level 1. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_riverglass_bead", "Riverglass Bead", "necklace", "new_accessory_riverglass_bead", 0, 0, 0, 3, 2, 6, 60, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common accessory. Requires level 2. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_copper_prayer_loop", "Copper Prayer Loop", "ring", "new_accessory_copper_prayer_loop", 0, 0, 7, 3, 2, 6, 79, ItemRarity.COMMON, 0, 0, 0, 2, 0, "", "Common accessory. Requires level 2. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_wayfarer_buckle", "Wayfarer Buckle", "belt", "new_accessory_wayfarer_buckle", 0, 1, 0, 4, 3, 7, 98, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common accessory. Requires level 3. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_willowseed_pendant", "Willowseed Pendant", "ring", "new_accessory_willowseed_pendant", 0, 0, 0, 4, 3, 7, 117, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common accessory. Requires level 3. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_small_storm_brooch", "Small Storm Brooch", "necklace", "new_accessory_small_storm_brooch", 0, 0, 10, 5, 4, 8, 136, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common accessory. Requires level 4. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_moonmilk_pearl", "Moonmilk Pearl", "necklace", "new_accessory_moonmilk_pearl", 0, 0, 0, 5, 4, 8, 155, ItemRarity.COMMON, 0, 0, 0, 0, 0, "", "Common accessory. Requires level 4. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_emberglass_signet", "Emberglass Signet", "ring", "new_accessory_emberglass_signet", 0, 0, 0, 6, 5, 9, 234, ItemRarity.UNCOMMON, 0, 2, 4, 0, 0, "", "Uncommon accessory. Requires level 5. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_frostpine_talisman", "Frostpine Talisman", "belt", "new_accessory_frostpine_talisman", 0, 1, 13, 6, 5, 9, 259, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon accessory. Requires level 5. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_stormthread_torque", "Stormthread Torque", "ring", "new_accessory_stormthread_torque", 0, 0, 0, 7, 6, 10, 285, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon accessory. Requires level 6. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_moonlit_ear_cuff", "Moonlit Ear Cuff", "necklace", "new_accessory_moonlit_ear_cuff", 0, 0, 0, 7, 6, 10, 310, ItemRarity.UNCOMMON, 0, 2, 5, 0, 0, "", "Uncommon accessory. Requires level 6. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_oakwarden_knot", "Oakwarden Knot", "necklace", "new_accessory_oakwarden_knot", 0, 0, 16, 8, 7, 11, 336, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon accessory. Requires level 7. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_sunspoke_medal", "Sunspoke Medal", "ring", "new_accessory_sunspoke_medal", 0, 0, 0, 8, 7, 11, 362, ItemRarity.UNCOMMON, 0, 0, 0, 4, 0, "", "Uncommon accessory. Requires level 7. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_gravesalt_rosary", "Gravesalt Rosary", "belt", "new_accessory_gravesalt_rosary", 0, 2, 0, 9, 8, 12, 387, ItemRarity.UNCOMMON, 0, 0, 0, 4, 0, "", "Uncommon accessory. Requires level 8. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_duskneedle_band", "Duskneedle Band", "ring", "new_accessory_duskneedle_band", 0, 0, 19, 9, 8, 12, 413, ItemRarity.UNCOMMON, 0, 2, 6, 0, 0, "", "Uncommon accessory. Requires level 8. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_cobalt_lens", "Cobalt Lens", "necklace", "new_accessory_cobalt_lens", 0, 0, 0, 10, 9, 13, 439, ItemRarity.UNCOMMON, 6, 0, 0, 0, 0, "", "Uncommon accessory. Requires level 9. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_redreef_shell_charm", "Redreef Shell Charm", "necklace", "new_accessory_redreef_shell_charm", 0, 0, 0, 10, 9, 13, 464, ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, "", "Uncommon accessory. Requires level 9. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_archive_pageweight", "Archive Pageweight", "ring", "new_accessory_archive_pageweight", 0, 0, 22, 11, 10, 16, 652, ItemRarity.RARE, 6, 0, 0, 0, 0, "", "Rare accessory. Requires level 10. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_rimehook_locket", "Rimehook Locket", "belt", "new_accessory_rimehook_locket", 0, 2, 0, 11, 10, 16, 686, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare accessory. Requires level 10. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_lanternfall_reliquary", "Lanternfall Reliquary", "ring", "new_accessory_lanternfall_reliquary", 0, 0, 0, 12, 11, 17, 720, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare accessory. Requires level 11. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_widowglass_ring", "Widowglass Ring", "necklace", "new_accessory_widowglass_ring", 0, 0, 25, 12, 11, 17, 755, ItemRarity.RARE, 0, 3, 7, 0, 0, "", "Rare accessory. Requires level 11. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_thunderroot_idol", "Thunderroot Idol", "necklace", "new_accessory_thunderroot_idol", 0, 0, 0, 13, 12, 18, 789, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare accessory. Requires level 12. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_mirechant_seal", "Mirechant Seal", "ring", "new_accessory_mirechant_seal", 0, 0, 0, 13, 12, 18, 823, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare accessory. Requires level 12. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_kingsmark_circlet", "Kingsmark Circlet", "belt", "new_accessory_kingsmark_circlet", 0, 3, 28, 14, 13, 19, 857, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare accessory. Requires level 13. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_hearthflame_chain", "Hearthflame Chain", "ring", "new_accessory_hearthflame_chain", 0, 0, 0, 14, 13, 19, 891, ItemRarity.RARE, 0, 0, 0, 0, 0, "", "Rare accessory. Requires level 13. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_starshard_astrolabe", "Starshard Astrolabe", "necklace", "new_accessory_starshard_astrolabe", 0, 0, 0, 15, 14, 20, 926, ItemRarity.RARE, 8, 0, 0, 0, 0, "", "Rare accessory. Requires level 14. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_nightwater_signet", "Nightwater Signet", "necklace", "new_accessory_nightwater_signet", 0, 0, 31, 15, 14, 20, 960, ItemRarity.RARE, 0, 4, 8, 0, 0, "", "Rare accessory. Requires level 14. A small relic tuned for a clear stat identity."),
            equipment("new_accessory_the_bellringer_clapper", "The Bellringer Clapper", "ring", "new_accessory_the_bellringer_clapper", 0, 0, 0, 16, 15, 99, 1296, ItemRarity.UNIQUE, 1, 5, 8, 0, 1, "Basic attacks bite deeper against weakened foes.", "Unique accessory. Requires level 15. Basic attacks bite deeper against weakened foes."),
            equipment("new_accessory_glass_choir_halo", "Glass Choir Halo", "belt", "new_accessory_glass_choir_halo", 0, 3, 0, 16, 16, 99, 1341, ItemRarity.UNIQUE, 10, 1, 0, 0, 1, "Starting a turn wounded sharpens focus.", "Unique accessory. Requires level 16. Starting a turn wounded sharpens focus."),
            equipment("new_accessory_oathroot_promise_band", "Oathroot Promise Band", "ring", "new_accessory_oathroot_promise_band", 0, 0, 34, 17, 17, 99, 1386, ItemRarity.UNIQUE, 1, 5, 9, 0, 1, "Battlefield movement feels lighter after every cast.", "Unique accessory. Requires level 17. Battlefield movement feels lighter after every cast."),
            equipment("new_accessory_vesper_thornheart", "Vesper Thornheart", "necklace", "new_accessory_vesper_thornheart", 0, 0, 0, 17, 18, 99, 1430, ItemRarity.UNIQUE, 1, 1, 0, 0, 2, "The first potion used each battle feels stronger.", "Unique accessory. Requires level 18. The first potion used each battle feels stronger."),
            equipment("new_accessory_samir_sun_index", "Samir Sun-Index", "necklace", "new_accessory_samir_sun_index", 0, 0, 0, 18, 19, 99, 1475, ItemRarity.UNIQUE, 1, 1, 0, 8, 2, "Critical healing has a chance to ripple to another ally.", "Unique accessory. Requires level 19. Critical healing has a chance to ripple to another ally."),
            equipment("new_accessory_cassia_gate_sigil", "Cassia Gate Sigil", "ring", "new_accessory_cassia_gate_sigil", 0, 0, 37, 18, 20, 99, 1520, ItemRarity.UNIQUE, 1, 1, 0, 0, 2, "Damage taken while guarding is partly returned as resolve.", "Unique accessory. Requires level 20. Damage taken while guarding is partly returned as resolve."),
            equipment("new_accessory_maera_margin_lens", "Maera Margin Lens", "belt", "new_accessory_maera_margin_lens", 0, 3, 0, 19, 21, 99, 1564, ItemRarity.UNIQUE, 11, 1, 0, 0, 3, "Attacks against vulnerable foes gain extra crit chance.", "Unique accessory. Requires level 21. Attacks against vulnerable foes gain extra crit chance."),
            equipment("new_accessory_lyra_mercybell", "Lyra Mercybell", "ring", "new_accessory_lyra_mercybell", 0, 0, 0, 19, 22, 99, 1609, ItemRarity.UNIQUE, 1, 1, 0, 9, 3, "The item remembers one desperate victory and answers in kind.", "Unique accessory. Requires level 22. The item remembers one desperate victory and answers in kind."),
            equipment("new_accessory_dawn_crown_phoenix_pin", "Dawn-Crown Phoenix Pin", "necklace", "new_accessory_dawn_crown_phoenix_pin", 0, 0, 40, 20, 20, 99, 2110, ItemRarity.LEGENDARY, 1, 1, 0, 0, 3, "Legendary property: once per battle, a lethal blow should leave its bearer at one breath when this hook is expanded.", "Legendary accessory. Requires level 20. Legendary property: once per battle, a lethal blow should leave its bearer at one breath when this hook is expanded."),
            equipment("new_accessory_worldroot_seedstone", "Worldroot Seedstone", "necklace", "new_accessory_worldroot_seedstone", 0, 0, 0, 20, 22, 99, 2167, ItemRarity.LEGENDARY, 1, 1, 0, 0, 4, "Legendary property: spell and weapon damage should alternate empowering each other in a future combat pass.", "Legendary accessory. Requires level 22. Legendary property: spell and weapon damage should alternate empowering each other in a future combat pass."),
            equipment("new_accessory_alderfall_starrelic", "Alderfall Starrelic", "ring", "new_accessory_alderfall_starrelic", 0, 0, 0, 21, 24, 99, 2224, ItemRarity.LEGENDARY, 12, 1, 0, 0, 4, "Legendary property: overhealing should create a party-wide shield pulse when this hook is expanded.", "Legendary accessory. Requires level 24. Legendary property: overhealing should create a party-wide shield pulse when this hook is expanded."),
            equipment("new_accessory_seal_of_the_twelfth", "Seal of the Twelfth", "belt", "new_accessory_seal_of_the_twelfth", 0, 4, 43, 21, 26, 99, 2281, ItemRarity.LEGENDARY, 1, 1, 0, 0, 4, "Legendary property: critical hits should briefly slow the enemy turn order when initiative exists.", "Legendary accessory. Requires level 26. Legendary property: critical hits should briefly slow the enemy turn order when initiative exists.")
    );


    private EquipmentCatalog() {
    }

    static Map<String, Equipment> equipmentItems() {
        return EQUIPMENT;
    }

    public static String itemName(String key) {
        Item item = ITEMS.get(key);
        if (item != null) {
            return item.name();
        }
        Equipment equipment = equipment(key);
        if (equipment != null) {
            return equipment.name();
        }
        String craftingName = CraftingSystem.itemName(key);
        return craftingName == null ? key : craftingName;
    }

    public static String itemIcon(String key) {
        Item item = ITEMS.get(key);
        if (item != null) {
            return item.icon();
        }
        Equipment equipment = equipment(key);
        if (equipment != null) {
            return ItemAppearance.icon(equipment);
        }
        String craftingIcon = CraftingSystem.itemIcon(key);
        return craftingIcon == null ? "icon_chest" : craftingIcon;
    }

    public static int itemCost(String key) {
        Item item = ITEMS.get(key);
        if (item != null) {
            return item.cost();
        }
        Equipment equipment = equipment(key);
        return equipment == null ? 0 : equipment.cost();
    }

    public static boolean isEquipment(String key) {
        return equipment(key) != null;
    }

    public static Equipment equipment(String key) {
        if (key != null && key.startsWith("gear1~")) return AssemblyCrafting.equipment(key);
        Equipment direct = EQUIPMENT.get(key);
        if (direct != null) {
            return direct;
        }
        if (key == null || !key.contains(AFFIX_SEPARATOR)) {
            return null;
        }
        String[] parts = key.split(AFFIX_SEPARATOR, 2);
        if (parts.length != 2) {
            return null;
        }
        Equipment base = EQUIPMENT.get(parts[0]);
        GearAffix affix = GEAR_AFFIXES.get(parts[1]);
        if (base == null || affix == null || base.rarity().ordinal() >= ItemRarity.UNIQUE.ordinal()
                || base.rarity().ordinal() < affix.minRarity().ordinal()) {
            return null;
        }
        String effect = base.uniqueEffect();
        String affixEffect = "Affix: " + affix.description();
        effect = effect == null || effect.isBlank() ? affixEffect : effect + " " + affixEffect;
        return new Equipment(
                key,
                affix.displayName(base.name()),
                base.slot(),
                base.icon(),
                base.rarity(),
                base.attackBonus() + affix.attackBonus(),
                base.defenseBonus() + affix.defenseBonus(),
                base.hpBonus() + affix.hpBonus(),
                base.mpBonus() + affix.mpBonus(),
                base.strengthBonus() + affix.strengthBonus(),
                base.intelligenceBonus() + affix.intelligenceBonus(),
                base.dexterityBonus() + affix.dexterityBonus(),
                base.charismaBonus() + affix.charismaBonus(),
                base.constitutionBonus() + affix.constitutionBonus(),
                base.willpowerBonus() + affix.willpowerBonus(),
                base.spellDamageBonus() + affix.spellDamageBonus(),
                base.critChanceBonus() + affix.critChanceBonus(),
                base.critDamageBonus() + affix.critDamageBonus(),
                base.healingPowerBonus() + affix.healingPowerBonus(),
                base.damageReductionBonus() + affix.damageReductionBonus(),
                base.minLevel(),
                base.maxLevel(),
                Math.max(1, base.cost() * (100 + affix.costPercent()) / 100),
                effect,
                base.description() + " " + affix.description()
        );
    }

    public static String rollAffixedKey(String baseKey, Random random) {
        Equipment base = EQUIPMENT.get(baseKey);
        if (base == null || base.rarity().ordinal() >= ItemRarity.UNIQUE.ordinal()) {
            return baseKey;
        }
        List<GearAffix> pool = GEAR_AFFIXES.values().stream()
                .filter(affix -> base.rarity().ordinal() >= affix.minRarity().ordinal())
                .toList();
        if (pool.isEmpty() || random.nextDouble() > affixChance(base.rarity())) {
            return baseKey;
        }
        GearAffix affix = pool.get(random.nextInt(pool.size()));
        return baseKey + AFFIX_SEPARATOR + affix.key();
    }

    private static double affixChance(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0.18;
            case UNCOMMON -> 0.32;
            case RARE -> 0.46;
            case UNIQUE, LEGENDARY -> 0.0;
        };
    }

    public static String rollContextualLoot(String mapKind, char terrain, int dungeonTier, List<GameData.MonsterSpec> specs, Random random) {
        if (random == null || specs == null || specs.isEmpty()) {
            return "";
        }
        boolean boss = specs.stream().anyMatch(EquipmentCatalog::isBossMonster);
        double dropChance = boss ? 0.72 : "dungeon".equals(mapKind) ? 0.34 + dungeonTier * 0.04 : 0.22;
        if (random.nextDouble() > Math.min(0.82, dropChance)) {
            return "";
        }
        List<String> pool = lootPoolFor(mapKind, terrain, dungeonTier, specs, boss);
        if (pool.isEmpty()) {
            return "";
        }
        String key = pool.get(random.nextInt(pool.size()));
        return EQUIPMENT.containsKey(key) ? rollAffixedKey(key, random) : key;
    }

    private static boolean isBossMonster(GameData.MonsterSpec spec) {
        String text = (spec.key() + " " + spec.name()).toLowerCase(Locale.ROOT);
        return text.contains("king") || text.contains("queen") || text.contains("dragon")
                || text.contains("broodmother") || text.contains("revenant") || text.contains("giant")
                || text.contains("vaelthara");
    }

    private static List<String> lootPoolFor(String mapKind, char terrain, int dungeonTier, List<GameData.MonsterSpec> specs, boolean boss) {
        if (boss) {
            return bossLootPoolFor(specs, dungeonTier);
        }
        if ("dungeon".equals(mapKind)) {
            return dungeonTier >= 3 ? DUNGEON_HIGH_LOOT : DUNGEON_LOW_LOOT;
        }
        return switch (terrain) {
            case 'f' -> List.of("new_weapon_briarhook_dagger", "new_weapon_oakwarden_spear",
                    "new_armor_willowhide_jerkin", "new_accessory_oakwarden_knot",
                    "new_potion_oakskin_decoction", "new_potion_vesper_thorn_salve");
            case 's', 'b' -> List.of("new_weapon_redreef_axe", "new_armor_redreef_waders",
                    "new_accessory_redreef_shell_charm", "new_potion_redreef_brine",
                    "new_weapon_hearthflame_falchion");
            case 'n' -> List.of("new_weapon_frostpine_bow", "new_weapon_rimehook_blade",
                    "new_armor_rimeguard_helm", "new_accessory_rimehook_locket",
                    "new_potion_frostwake_cordial", "new_potion_rimehook_liniment");
            case 'm', 'q' -> List.of("new_weapon_cobalt_halberd", "new_weapon_thunderroot_maul",
                    "new_armor_cobalt_mail", "new_accessory_cobalt_lens",
                    "new_potion_cobalt_focus_phial", "new_potion_thunderroot_charge");
            case 'w', '~' -> List.of("new_weapon_nightwater_trident", "new_armor_mirechant_waders",
                    "new_accessory_mirechant_seal", "new_potion_mirechant_poultice",
                    "new_potion_clearwater_flask");
            default -> List.of("new_weapon_ashfall_cutlass", "new_weapon_militia_pike",
                    "new_armor_ashguard_boots", "new_accessory_tin_luck_ring",
                    "new_potion_minor_redcap_draught", "new_potion_copperleaf_tonic");
        };
    }

    private static List<String> bossLootPoolFor(List<GameData.MonsterSpec> specs, int dungeonTier) {
        List<String> pool = new ArrayList<>(dungeonTier >= 3 ? HIGH_BOSS_LOOT : LOW_BOSS_LOOT);
        for (GameData.MonsterSpec spec : specs) {
            String text = (spec.key() + " " + spec.name()).toLowerCase(Locale.ROOT);
            if (text.contains("dragon")) {
                pool.addAll(BOSS_LOOT_DAWN);
            }
            if (text.contains("vaelthara")) {
                pool.addAll(BOSS_LOOT_TWELFTH);
            }
            if (text.contains("revenant") || text.contains("skeleton")) {
                pool.addAll(BOSS_LOOT_GLASS);
            }
            if (text.contains("broodmother") || text.contains("spider") || text.contains("thorn")) {
                pool.addAll(BOSS_LOOT_THORN);
            }
            if (text.contains("giant") || text.contains("orc") || text.contains("king") || text.contains("queen")) {
                pool.addAll(BOSS_LOOT_OATH);
            }
            if (text.contains("worldroot") || text.contains("root")) {
                pool.addAll(BOSS_LOOT_WORLDROOT);
            }
        }
        return List.copyOf(pool);
    }

    private static Map.Entry<String, Equipment> equipment(
            String key,
            String name,
            String slot,
            String icon,
            int attackBonus,
            int defenseBonus,
            int hpBonus,
            int mpBonus,
            int cost,
            String description
    ) {
        return equipment(key, name, slot, icon, attackBonus, defenseBonus, hpBonus, mpBonus, 1, 99, cost, description);
    }

    private static Map.Entry<String, GearAffix> affix(
            String key,
            String prefix,
            String suffix,
            ItemRarity minRarity,
            int attackBonus,
            int defenseBonus,
            int hpBonus,
            int mpBonus,
            int strengthBonus,
            int intelligenceBonus,
            int dexterityBonus,
            int charismaBonus,
            int constitutionBonus,
            int willpowerBonus,
            int spellDamageBonus,
            int critChanceBonus,
            int critDamageBonus,
            int healingPowerBonus,
            int damageReductionBonus,
            int costPercent,
            String description
    ) {
        return Map.entry(key, new GearAffix(key, prefix, suffix, minRarity,
                attackBonus, defenseBonus, hpBonus, mpBonus,
                strengthBonus, intelligenceBonus, dexterityBonus, charismaBonus, constitutionBonus, willpowerBonus,
                spellDamageBonus, critChanceBonus, critDamageBonus, healingPowerBonus, damageReductionBonus,
                costPercent, description));
    }

    private static Map.Entry<String, Equipment> equipment(
            String key,
            String name,
            String slot,
            String icon,
            int attackBonus,
            int defenseBonus,
            int hpBonus,
            int mpBonus,
            int minLevel,
            int maxLevel,
            int cost,
            String description
    ) {
        return Map.entry(key, new Equipment(
                key,
                name,
                slot,
                icon,
                inferredRarity(key, name, minLevel, maxLevel, cost),
                attackBonus,
                defenseBonus,
                hpBonus,
                mpBonus,
                inferredStrengthBonus(key, name, slot, attackBonus, defenseBonus, hpBonus, mpBonus),
                inferredIntelligenceBonus(key, name, slot, attackBonus, defenseBonus, hpBonus, mpBonus),
                inferredDexterityBonus(key, name, slot, attackBonus, defenseBonus, hpBonus, mpBonus),
                inferredCharismaBonus(key, name, slot, attackBonus, defenseBonus, hpBonus, mpBonus),
                inferredConstitutionBonus(key, name, slot, attackBonus, defenseBonus, hpBonus, mpBonus),
                inferredWillpowerBonus(key, name, slot, attackBonus, defenseBonus, hpBonus, mpBonus),
                0,
                0,
                0,
                0,
                0,
                minLevel,
                maxLevel,
                cost,
                "",
                description
        ));
    }

    private static Map.Entry<String, Equipment> equipment(
            String key,
            String name,
            String slot,
            String icon,
            int attackBonus,
            int defenseBonus,
            int hpBonus,
            int mpBonus,
            int minLevel,
            int maxLevel,
            int cost,
            ItemRarity rarity,
            int spellDamageBonus,
            int critChanceBonus,
            int critDamageBonus,
            int healingPowerBonus,
            int damageReductionBonus,
            String uniqueEffect,
            String description
    ) {
        return Map.entry(key, new Equipment(
                key,
                name,
                slot,
                icon,
                rarity,
                attackBonus,
                defenseBonus,
                hpBonus,
                mpBonus,
                inferredStrengthBonus(key, name, slot, attackBonus, defenseBonus, hpBonus, mpBonus),
                inferredIntelligenceBonus(key, name, slot, attackBonus, defenseBonus, hpBonus, mpBonus),
                inferredDexterityBonus(key, name, slot, attackBonus, defenseBonus, hpBonus, mpBonus),
                inferredCharismaBonus(key, name, slot, attackBonus, defenseBonus, hpBonus, mpBonus),
                inferredConstitutionBonus(key, name, slot, attackBonus, defenseBonus, hpBonus, mpBonus),
                inferredWillpowerBonus(key, name, slot, attackBonus, defenseBonus, hpBonus, mpBonus),
                spellDamageBonus,
                critChanceBonus,
                critDamageBonus,
                healingPowerBonus,
                damageReductionBonus,
                minLevel,
                maxLevel,
                cost,
                uniqueEffect,
                description
        ));
    }

    private static ItemRarity inferredRarity(String key, String name, int minLevel, int maxLevel, int cost) {
        String text = (key + " " + name).toLowerCase(Locale.ROOT);
        if ((maxLevel >= 99 && minLevel >= 18) || minLevel >= 18 || cost >= 750 || text.contains("phoenix") || text.contains("oathbreaker")) {
            return ItemRarity.LEGENDARY;
        }
        if (minLevel >= 13 || cost >= 430 || text.contains("heartstone") || text.contains("starrelic")) {
            return ItemRarity.UNIQUE;
        }
        if (minLevel >= 8 || cost >= 160) {
            return ItemRarity.RARE;
        }
        if (minLevel >= 3 || cost >= 45) {
            return ItemRarity.UNCOMMON;
        }
        return ItemRarity.COMMON;
    }

    private static int inferredStrengthBonus(String key, String name, String slot, int attackBonus, int defenseBonus, int hpBonus, int mpBonus) {
        String text = (key + " " + name + " " + slot).toLowerCase();
        int bonus = 0;
        if (slot.equals("weapon") && !isFocusWeapon(text)) {
            bonus += Math.max(0, attackBonus / 4);
        }
        if (slot.equals("shield") || text.contains("plate") || text.contains("mail") || text.contains("axe")) {
            bonus += Math.max(0, defenseBonus / 5) + Math.max(0, hpBonus / 18);
        }
        return bonus;
    }

    private static int inferredIntelligenceBonus(String key, String name, String slot, int attackBonus, int defenseBonus, int hpBonus, int mpBonus) {
        String text = (key + " " + name + " " + slot).toLowerCase();
        int bonus = 0;
        if (isFocusWeapon(text) || text.contains("robe") || text.contains("scepter") || text.contains("archive")) {
            bonus += Math.max(1, mpBonus / 6);
        } else {
            bonus += Math.max(0, mpBonus / 12);
        }
        return bonus;
    }

    private static int inferredDexterityBonus(String key, String name, String slot, int attackBonus, int defenseBonus, int hpBonus, int mpBonus) {
        String text = (key + " " + name + " " + slot).toLowerCase();
        int bonus = 0;
        if (text.contains("bow") || text.contains("dagger") || text.contains("rapier") || text.contains("knife")
                || text.contains("scout") || text.contains("softstep") || text.contains("duelist")
                || text.contains("leather") || text.contains("runner")) {
            bonus += Math.max(1, attackBonus / 4 + defenseBonus / 7);
        }
        if (slot.equals("boots") || slot.equals("gloves")) {
            bonus += Math.max(0, attackBonus / 3);
        }
        return bonus;
    }

    private static int inferredCharismaBonus(String key, String name, String slot, int attackBonus, int defenseBonus, int hpBonus, int mpBonus) {
        String text = (key + " " + name + " " + slot).toLowerCase();
        if (slot.equals("necklace") || slot.equals("ring") || text.contains("sun") || text.contains("royal")
                || text.contains("acolyte") || text.contains("mantle") || text.contains("brooch")) {
            return Math.max(1, (mpBonus + hpBonus) / 16);
        }
        return 0;
    }

    private static int inferredConstitutionBonus(String key, String name, String slot, int attackBonus, int defenseBonus, int hpBonus, int mpBonus) {
        String text = (key + " " + name + " " + slot).toLowerCase();
        int bonus = Math.max(0, hpBonus / 10);
        if (slot.equals("shield") || text.contains("plate") || text.contains("mail") || text.contains("helm")
                || text.contains("guard") || text.contains("vitality") || text.contains("heart")) {
            bonus += Math.max(1, defenseBonus / 6 + hpBonus / 18);
        }
        return bonus;
    }

    private static int inferredWillpowerBonus(String key, String name, String slot, int attackBonus, int defenseBonus, int hpBonus, int mpBonus) {
        String text = (key + " " + name + " " + slot).toLowerCase();
        int bonus = Math.max(0, defenseBonus / 5);
        if (slot.equals("shield") || text.contains("ward") || text.contains("aegis") || text.contains("charm")
                || text.contains("seal") || text.contains("heart") || text.contains("frost")) {
            bonus += Math.max(1, (defenseBonus + mpBonus) / 10);
        }
        return bonus;
    }

    private static boolean isFocusWeapon(String text) {
        return text.contains("staff") || text.contains("scepter") || text.contains("crook");
    }

}
