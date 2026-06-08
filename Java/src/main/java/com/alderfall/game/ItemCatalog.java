package com.alderfall.game;

import com.alderfall.game.inventory.Item;
import com.alderfall.game.inventory.ItemRarity;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ItemCatalog {
    private static final Map<String, Item> ITEMS = build(List.of(
            Map.entry("potion_small", new Item("potion_small", "Small Potion", "icon_potion_red", 18, 28, 0)),
            Map.entry("potion_large", new Item("potion_large", "Large Potion", "icon_potion_green", 42, 60, 0)),
            Map.entry("ether", new Item("ether", "Ether", "icon_potion_blue", 28, 0, 22)),
            Map.entry("guard_tonic", new Item("guard_tonic", "Guard Tonic", "icon_shield", 35, 16, 10)),
            Map.entry("battle_kit", new Item("battle_kit", "Battle Kit", "icon_sword", 55, 36, 16)),
            Map.entry("cooked_meat", new Item("cooked_meat", "Cooked Meat", "icon_potion_red", 16, 22, 0)),
            Map.entry("trail_rations", new Item("trail_rations", "Trail Rations", "icon_potion_green", 34, 42, 8)),
            Map.entry("vegetable_stew", new Item("vegetable_stew", "Vegetable Stew", "icon_potion_green", 24, 30, 8)),
            Map.entry("herbal_salve", new Item("herbal_salve", "Herbal Salve", "icon_potion_green", 24, 34, 0)),
            Map.entry("focus_tea", new Item("focus_tea", "Focus Tea", "icon_potion_blue", 22, 8, 18)),
            Map.entry("escape_scroll", new Item("escape_scroll", "Escape Scroll", "icon_potion_blue", 48, 0, 0)),
            Map.entry("recipe_book_camp_cookery", new Item("recipe_book_camp_cookery", "Camp Cookery Notes", "icon_potion_green", 34, 0, 0)),
            Map.entry("recipe_book_fisher_knots", new Item("recipe_book_fisher_knots", "Fisher's Knotbook", "icon_chest", 30, 0, 0)),
            Map.entry("recipe_book_iron_blades", new Item("recipe_book_iron_blades", "Iron Blade Pattern", "icon_sword", 58, 0, 0)),
            Map.entry("recipe_book_iron_mail", new Item("recipe_book_iron_mail", "Mail Ring Pattern", "icon_shield", 64, 0, 0)),
            Map.entry("recipe_book_steel_plate", new Item("recipe_book_steel_plate", "Steel Plate Pattern", "icon_shield", 110, 0, 0)),
            Map.entry("recipe_book_oak_bow", new Item("recipe_book_oak_bow", "Oak Bow Pattern", "icon_sword", 48, 0, 0)),
            Map.entry("recipe_book_apothecary_salve", new Item("recipe_book_apothecary_salve", "Apothecary Salve Page", "icon_potion_green", 46, 0, 0)),
            Map.entry("recipe_book_escape_scrolls", new Item("recipe_book_escape_scrolls", "Escape Scroll Primer", "icon_potion_blue", 72, 0, 0)),
            Map.entry("new_potion_minor_redcap_draught", new Item("new_potion_minor_redcap_draught", "Minor Redcap Draught", "new_potion_minor_redcap_draught", 14, 28, 9, ItemRarity.COMMON, 1, "Simple field recovery with no special rider.")),
            Map.entry("new_potion_clearwater_flask", new Item("new_potion_clearwater_flask", "Clearwater Flask", "new_potion_clearwater_flask", 24, 21, 12, ItemRarity.COMMON, 1, "Simple field recovery with no special rider.")),
            Map.entry("new_potion_mint_ether_sip", new Item("new_potion_mint_ether_sip", "Mint Ether Sip", "new_potion_mint_ether_sip", 34, 8, 14, ItemRarity.COMMON, 2, "Simple field recovery with no special rider.")),
            Map.entry("new_potion_bandage_balm", new Item("new_potion_bandage_balm", "Bandage Balm", "new_potion_bandage_balm", 44, 26, 0, ItemRarity.COMMON, 2, "Simple field recovery with no special rider.")),
            Map.entry("new_potion_salted_travel_broth", new Item("new_potion_salted_travel_broth", "Salted Travel Broth", "new_potion_salted_travel_broth", 54, 29, 0, ItemRarity.COMMON, 3, "Simple field recovery with no special rider.")),
            Map.entry("new_potion_copperleaf_tonic", new Item("new_potion_copperleaf_tonic", "Copperleaf Tonic", "new_potion_copperleaf_tonic", 64, 32, 20, ItemRarity.COMMON, 3, "Simple field recovery with no special rider.")),
            Map.entry("new_potion_blue_candle_tea", new Item("new_potion_blue_candle_tea", "Blue Candle Tea", "new_potion_blue_candle_tea", 74, 12, 21, ItemRarity.COMMON, 4, "Simple field recovery with no special rider.")),
            Map.entry("new_potion_thornbite_antidote", new Item("new_potion_thornbite_antidote", "Thornbite Antidote", "new_potion_thornbite_antidote", 84, 55, 4, ItemRarity.COMMON, 4, "Simple field recovery with no special rider.")),
            Map.entry("new_potion_emberwarm_elixir", new Item("new_potion_emberwarm_elixir", "Emberwarm Elixir", "new_potion_emberwarm_elixir", 127, 55, 0, ItemRarity.UNCOMMON, 5, "Reliable recovery brewed for longer routes.")),
            Map.entry("new_potion_frostwake_cordial", new Item("new_potion_frostwake_cordial", "Frostwake Cordial", "new_potion_frostwake_cordial", 140, 58, 37, ItemRarity.UNCOMMON, 5, "Reliable recovery brewed for longer routes.")),
            Map.entry("new_potion_stormbreath_tonic", new Item("new_potion_stormbreath_tonic", "Stormbreath Tonic", "new_potion_stormbreath_tonic", 154, 22, 39, ItemRarity.UNCOMMON, 6, "Reliable recovery brewed for longer routes.")),
            Map.entry("new_potion_moonmilk_salve", new Item("new_potion_moonmilk_salve", "Moonmilk Salve", "new_potion_moonmilk_salve", 167, 66, 0, ItemRarity.UNCOMMON, 6, "Reliable recovery brewed for longer routes.")),
            Map.entry("new_potion_oakskin_decoction", new Item("new_potion_oakskin_decoction", "Oakskin Decoction", "new_potion_oakskin_decoction", 181, 70, 0, ItemRarity.UNCOMMON, 7, "Reliable recovery brewed for longer routes.")),
            Map.entry("new_potion_sunspoke_remedy", new Item("new_potion_sunspoke_remedy", "Sunspoke Remedy", "new_potion_sunspoke_remedy", 194, 73, 47, ItemRarity.UNCOMMON, 7, "Reliable recovery brewed for longer routes.")),
            Map.entry("new_potion_gravesalt_cleanser", new Item("new_potion_gravesalt_cleanser", "Gravesalt Cleanser", "new_potion_gravesalt_cleanser", 208, 51, 53, ItemRarity.UNCOMMON, 8, "Reliable recovery brewed for longer routes.")),
            Map.entry("new_potion_duskstep_smoke_vial", new Item("new_potion_duskstep_smoke_vial", "Duskstep Smoke Vial", "new_potion_duskstep_smoke_vial", 221, 81, 27, ItemRarity.UNCOMMON, 8, "Reliable recovery brewed for longer routes.")),
            Map.entry("new_potion_cobalt_focus_phial", new Item("new_potion_cobalt_focus_phial", "Cobalt Focus Phial", "new_potion_cobalt_focus_phial", 235, 85, 0, ItemRarity.UNCOMMON, 9, "Reliable recovery brewed for longer routes.")),
            Map.entry("new_potion_redreef_brine", new Item("new_potion_redreef_brine", "Redreef Brine", "new_potion_redreef_brine", 248, 89, 57, ItemRarity.UNCOMMON, 9, "Reliable recovery brewed for longer routes.")),
            Map.entry("new_potion_archive_ink_tonic", new Item("new_potion_archive_ink_tonic", "Archive Ink Tonic", "new_potion_archive_ink_tonic", 349, 43, 80, ItemRarity.RARE, 10, "Potent recovery that supports difficult encounters.")),
            Map.entry("new_potion_rimehook_liniment", new Item("new_potion_rimehook_liniment", "Rimehook Liniment", "new_potion_rimehook_liniment", 367, 128, 0, ItemRarity.RARE, 10, "Potent recovery that supports difficult encounters.")),
            Map.entry("new_potion_lanternfall_restorative", new Item("new_potion_lanternfall_restorative", "Lanternfall Restorative", "new_potion_lanternfall_restorative", 385, 133, 45, ItemRarity.RARE, 11, "Potent recovery that supports difficult encounters.")),
            Map.entry("new_potion_widowglass_antivenom", new Item("new_potion_widowglass_antivenom", "Widowglass Antivenom", "new_potion_widowglass_antivenom", 403, 169, 94, ItemRarity.RARE, 11, "Potent recovery that supports difficult encounters.")),
            Map.entry("new_potion_thunderroot_charge", new Item("new_potion_thunderroot_charge", "Thunderroot Charge", "new_potion_thunderroot_charge", 421, 50, 93, ItemRarity.RARE, 12, "Potent recovery that supports difficult encounters.")),
            Map.entry("new_potion_mirechant_poultice", new Item("new_potion_mirechant_poultice", "Mirechant Poultice", "new_potion_mirechant_poultice", 439, 148, 0, ItemRarity.RARE, 12, "Potent recovery that supports difficult encounters.")),
            Map.entry("new_potion_kingsmark_battle_kit", new Item("new_potion_kingsmark_battle_kit", "Kingsmark Battle Kit", "new_potion_kingsmark_battle_kit", 457, 153, 0, ItemRarity.RARE, 13, "Potent recovery that supports difficult encounters.")),
            Map.entry("new_potion_hearthflame_reviver", new Item("new_potion_hearthflame_reviver", "Hearthflame Reviver", "new_potion_hearthflame_reviver", 475, 158, 104, ItemRarity.RARE, 13, "Potent recovery that supports difficult encounters.")),
            Map.entry("new_potion_starshard_ether", new Item("new_potion_starshard_ether", "Starshard Ether", "new_potion_starshard_ether", 493, 58, 107, ItemRarity.RARE, 14, "Potent recovery that supports difficult encounters.")),
            Map.entry("new_potion_nightwater_draught", new Item("new_potion_nightwater_draught", "Nightwater Draught", "new_potion_nightwater_draught", 511, 168, 0, ItemRarity.RARE, 14, "Potent recovery that supports difficult encounters.")),
            Map.entry("new_potion_bellringer_incense", new Item("new_potion_bellringer_incense", "Bellringer Incense", "new_potion_bellringer_incense", 691, 265, 4, ItemRarity.UNIQUE, 15, "First critical strike each battle hits harder.")),
            Map.entry("new_potion_glass_choir_serum", new Item("new_potion_glass_choir_serum", "Glass Choir Serum", "new_potion_glass_choir_serum", 714, 233, 153, ItemRarity.UNIQUE, 16, "Damaging spells leave a faint echo in the target.")),
            Map.entry("new_potion_oathroot_fortifier", new Item("new_potion_oathroot_fortifier", "Oathroot Fortifier", "new_potion_oathroot_fortifier", 738, 85, 157, ItemRarity.UNIQUE, 17, "Defending steadies nearby allies.")),
            Map.entry("new_potion_vesper_thorn_salve", new Item("new_potion_vesper_thorn_salve", "Vesper Thorn Salve", "new_potion_vesper_thorn_salve", 761, 246, 0, ItemRarity.UNIQUE, 18, "Healing above the cap becomes a small ward.")),
            Map.entry("new_potion_samir_sunbrew", new Item("new_potion_samir_sunbrew", "Samir Sunbrew", "new_potion_samir_sunbrew", 785, 253, 0, ItemRarity.UNIQUE, 19, "Basic attacks bite deeper against weakened foes.")),
            Map.entry("new_potion_cassia_gate_tonic", new Item("new_potion_cassia_gate_tonic", "Cassia Gate Tonic", "new_potion_cassia_gate_tonic", 808, 259, 171, ItemRarity.UNIQUE, 20, "Starting a turn wounded sharpens focus.")),
            Map.entry("new_potion_maera_margin_tea", new Item("new_potion_maera_margin_tea", "Maera Margin Tea", "new_potion_maera_margin_tea", 832, 94, 175, ItemRarity.UNIQUE, 21, "Battlefield movement feels lighter after every cast.")),
            Map.entry("new_potion_lyra_mercy_vial", new Item("new_potion_lyra_mercy_vial", "Lyra Mercy Vial", "new_potion_lyra_mercy_vial", 855, 318, 98, ItemRarity.UNIQUE, 22, "The first potion used each battle feels stronger.")),
            Map.entry("new_potion_dawn_crown_elixir", new Item("new_potion_dawn_crown_elixir", "Dawn-Crown Elixir", "new_potion_dawn_crown_elixir", 1122, 356, 0, ItemRarity.LEGENDARY, 20, "Legendary property: once per battle, a lethal blow should leave its bearer at one breath when this hook is expanded.")),
            Map.entry("new_potion_worldroot_panacea", new Item("new_potion_worldroot_panacea", "Worldroot Panacea", "new_potion_worldroot_panacea", 1152, 365, 241, ItemRarity.LEGENDARY, 22, "Legendary property: spell and weapon damage should alternate empowering each other in a future combat pass.")),
            Map.entry("new_potion_alderfall_star_phial", new Item("new_potion_alderfall_star_phial", "Alderfall Star Phial", "new_potion_alderfall_star_phial", 1182, 132, 247, ItemRarity.LEGENDARY, 24, "Legendary property: overhealing should create a party-wide shield pulse when this hook is expanded.")),
            Map.entry("new_potion_twelfth_silence_flask", new Item("new_potion_twelfth_silence_flask", "Twelfth Silence Flask", "new_potion_twelfth_silence_flask", 1212, 382, 0, ItemRarity.LEGENDARY, 26, "Legendary property: critical hits should briefly slow the enemy turn order when initiative exists."))
    ));

    private ItemCatalog() {
    }

    static Map<String, Item> items() {
        return ITEMS;
    }

    private static Map<String, Item> build(List<Map.Entry<String, Item>> entries) {
        LinkedHashMap<String, Item> items = new LinkedHashMap<>();
        for (Map.Entry<String, Item> entry : entries) {
            Item previous = items.put(entry.getKey(), entry.getValue());
            if (previous != null) {
                throw new IllegalStateException("Duplicate item key: " + entry.getKey());
            }
        }
        return Collections.unmodifiableMap(items);
    }
}
