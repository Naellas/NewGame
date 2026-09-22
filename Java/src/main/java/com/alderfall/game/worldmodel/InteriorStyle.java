package com.alderfall.game;

import java.awt.Color;
import java.util.List;

/** Household materials and everyday customs, shared by generation and rendering. */
public enum InteriorStyle {
    HEARTHLANDS("Hearthlands", "interior_floor_blend", new Color(96, 59, 35, 12),
            "interior_wall_herb_rack", "interior_herb_planter", "interior_seed_bowl"),
    THORNMERE("River and Thorn", "interior_floor_blend", new Color(48, 76, 49, 25),
            "interior_wall_ivy_planter", "interior_floor_sapling_pot", "interior_tabletop_meal"),
    STORMBOUND("Stormbound Holds", "interior_floor_blend", new Color(28, 36, 49, 85),
            "interior_wall_herb_rack", "interior_linen_shelf", "interior_tabletop_candle"),
    SUNREALM("Sunrealm", "interior_floor_blend_alt", new Color(224, 180, 109, 33),
            "interior_wall_plant_shelf", "interior_planting_pot", "interior_flower_vase"),
    FENLANDS("Belltower Fenlands", "interior_floor_blend", new Color(28, 57, 53, 78),
            "interior_wall_herb_rack", "interior_floor_reed_pot", "interior_mortar_pestle"),
    ARCHIVE("Archive households", "interior_floor_study", new Color(73, 62, 95, 16),
            "interior_wall_window_wide", "interior_bookshelf", "interior_tabletop_candle");

    public final String label;
    public final String floor;
    public final Color materialTint;
    public final String wallAccent;
    public final String furnishing;
    public final String tableDetail;

    InteriorStyle(String label, String floor, Color tint, String wall, String furnishing, String detail) {
        this.label = label; this.floor = floor; this.materialTint = tint;
        this.wallAccent = wall; this.furnishing = furnishing; this.tableDetail = detail;
    }

    public static InteriorStyle forMap(String mapId) {
        if (belongsTo(mapId, "highwall", "ironvale", "northwatch", "snowrest", "pineward", "cairnvale")) return STORMBOUND;
        if (belongsTo(mapId, "sanctum", "embermarket", "dunewick", "sunmere", "redcairn")) return SUNREALM;
        if (belongsTo(mapId, "belltower", "reedwatch", "greyharbor", "mireford", "glimmerfen", "stormfen")) return FENLANDS;
        if (belongsTo(mapId, "riverside", "briarbridge", "foxbarrow")) return THORNMERE;
        if (belongsTo(mapId, "archive", "moonspire")) return ARCHIVE;
        return HEARTHLANDS;
    }

    private static boolean belongsTo(String id, String... settlements) {
        for (String name : settlements) {
            for (String kind : List.of("city_", "town_", "village_")) {
                String source = kind + name;
                if (id.equals(source) || id.startsWith("house_" + source + "_")) return true;
            }
        }
        return false;
    }
}
