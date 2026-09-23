package com.alderfall.game;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Curated supplied furniture: one identity shared by editor, collision and drawing. */
public final class InteriorFurnishings {
    private InteriorFurnishings() {}
    public enum Placement { FLOOR, SURFACE, WALL, TABLETOP }
    public record Furnishing(String asset, String label, String category, Placement placement,
                             int width, int depth, float drawHeight) {
        public VillageManager.PlaceableAsset placeable() {
            String material = asset.contains("produce_") ? "trail_rations"
                    : asset.contains("ironbound") ? "iron_ore"
                    : asset.contains("fireplace") || asset.contains("pottery") || asset.contains("jug") ? "stone"
                    : "wood";
            int count = placement == Placement.TABLETOP ? 1 : width + depth;
            return new VillageManager.PlaceableAsset(asset, label, 48,
                    VillageManager.VillageCost.of(count * 3, Map.of(material, count)), category);
        }
    }
    private static final List<Furnishing> ALL = List.of(
            new Furnishing("interior_sofa_red", "Red Sofa", "Seating", Placement.FLOOR, 2, 1, 1.2f),
            new Furnishing("interior_armchair_green", "Green Armchair", "Seating", Placement.FLOOR, 1, 1, 1.2f),
            new Furnishing("interior_stool", "Stool", "Seating", Placement.FLOOR, 1, 1, 0.7f),
            new Furnishing("interior_chest_of_drawers", "Chest Of Drawers", "Storage", Placement.SURFACE, 1, 1, 1.1f),
            new Furnishing("interior_wardrobe", "Wardrobe", "Storage", Placement.FLOOR, 1, 1, 1.65f),
            new Furnishing("interior_crockery_cupboard", "Crockery Cupboard", "Storage", Placement.FLOOR, 1, 1, 1.5f),
            new Furnishing("interior_wall_books", "Books", "Wall-mounted", Placement.WALL, 2, 1, 1f),
            new Furnishing("interior_fireplace", "Fireplace", "Workstations", Placement.FLOOR, 1, 1, 1.3f),
            new Furnishing("interior_firewood_basket", "Firewood Basket", "Storage", Placement.FLOOR, 1, 1, 0.8f),
            new Furnishing("interior_washstand", "Washstand", "Workstations", Placement.FLOOR, 1, 1, 1.2f),
            new Furnishing("interior_plate_rack", "Plate Rack", "Storage", Placement.FLOOR, 1, 1, 1f),
            new Furnishing("interior_room_screen", "Room Screen", "Misc", Placement.FLOOR, 2, 1, 1.3f),
            new Furnishing("interior_coat_stand", "Coat Stand", "Misc", Placement.FLOOR, 1, 1, 1.35f),
            new Furnishing("interior_wall_banner_lion", "Banner Lion", "Wall-mounted", Placement.WALL, 1, 1, 1f),
            new Furnishing("interior_wall_landscape", "Landscape", "Wall-mounted", Placement.WALL, 2, 1, 1f),
            new Furnishing("interior_wall_flower_painting", "Flower Painting", "Wall-mounted", Placement.WALL, 1, 1, 1f),
            new Furnishing("interior_wall_mirror", "Mirror", "Wall-mounted", Placement.WALL, 1, 1, 1f),
            new Furnishing("interior_wall_clock", "Clock", "Wall-mounted", Placement.WALL, 1, 1, 1f),
            new Furnishing("interior_wall_antlers", "Antlers", "Wall-mounted", Placement.WALL, 1, 1, 1f),
            new Furnishing("interior_tabletop_books", "Books", "Decorations", Placement.TABLETOP, 1, 1, 0.45f),
            new Furnishing("interior_tabletop_scrolls", "Scrolls", "Decorations", Placement.TABLETOP, 1, 1, 0.4f),
            new Furnishing("interior_tabletop_inkwell", "Inkwell", "Decorations", Placement.TABLETOP, 1, 1, 0.45f),
            new Furnishing("interior_tabletop_tankard", "Tankard", "Decorations", Placement.TABLETOP, 1, 1, 0.35f),
            new Furnishing("interior_tabletop_jug", "Jug", "Decorations", Placement.TABLETOP, 1, 1, 0.45f),
            new Furnishing("interior_tabletop_wine", "Wine", "Decorations", Placement.TABLETOP, 1, 1, 0.5f),
            new Furnishing("interior_tabletop_fruit", "Fruit", "Decorations", Placement.TABLETOP, 1, 1, 0.4f),
            new Furnishing("interior_produce_apples", "Apple Crate", "Storage", Placement.FLOOR, 1, 1, 0.8f),
            new Furnishing("interior_produce_cabbages", "Cabbage Crate", "Storage", Placement.FLOOR, 1, 1, 0.8f),
            new Furnishing("interior_produce_corn", "Corn Crate", "Storage", Placement.FLOOR, 1, 1, 0.8f),
            new Furnishing("interior_produce_fish", "Fish Crate", "Storage", Placement.FLOOR, 1, 1, 0.8f),
            new Furnishing("interior_produce_potatoes", "Potato Crate", "Storage", Placement.FLOOR, 1, 1, 0.8f),
            new Furnishing("interior_bottle_crate", "Bottle Crate", "Storage", Placement.FLOOR, 1, 1, 0.8f),
            new Furnishing("interior_coal_crate", "Coal Crate", "Storage", Placement.FLOOR, 1, 1, 0.7f),
            new Furnishing("interior_supplies_shelf", "Supplies Shelf", "Storage", Placement.FLOOR, 1, 1, 1.5f),
            new Furnishing("interior_tools_shelf", "Tools Shelf", "Storage", Placement.FLOOR, 1, 1, 1.5f),
            new Furnishing("interior_pantry_shelf", "Pantry Shelf", "Storage", Placement.FLOOR, 1, 1, 1.5f),
            new Furnishing("interior_wall_tools", "Tools", "Wall-mounted", Placement.WALL, 2, 1, 1f),
            new Furnishing("interior_wall_parcels", "Parcels", "Wall-mounted", Placement.WALL, 2, 1, 1f),
            new Furnishing("interior_pallet", "Pallet", "Storage", Placement.FLOOR, 2, 1, 0.65f),
            new Furnishing("interior_tied_bales", "Tied Supply Bales", "Storage", Placement.FLOOR, 1, 1, 0.85f),
            new Furnishing("interior_pottery_cluster", "Pottery Cluster", "Storage", Placement.FLOOR, 1, 1, 1f),
            new Furnishing("interior_basket_empty", "Empty Basket", "Storage", Placement.FLOOR, 1, 1, 0.65f),
            new Furnishing("interior_basket_demijohn", "Wicker Demijohn", "Storage", Placement.FLOOR, 1, 1, 0.8f),
            new Furnishing("interior_bucket", "Bucket", "Misc", Placement.FLOOR, 1, 1, 0.7f),
            new Furnishing("interior_broom", "Broom", "Misc", Placement.FLOOR, 1, 1, 1.3f),
            new Furnishing("interior_stepladder", "Stepladder", "Workstations", Placement.FLOOR, 1, 1, 1.2f),
            new Furnishing("interior_cask_on_side", "Cask On Side", "Storage", Placement.FLOOR, 1, 1, 0.75f),
            new Furnishing("interior_ironbound_chest", "Ironbound Chest", "Storage", Placement.FLOOR, 1, 1, 0.9f),
            new Furnishing("interior_open_storage_bin", "Open Storage Bin", "Storage", Placement.FLOOR, 1, 1, 0.8f),
            new Furnishing("interior_handcart", "Handcart", "Workstations", Placement.FLOOR, 2, 1, 1f),
            new Furnishing("interior_leaning_ladder", "Leaning Ladder", "Workstations", Placement.FLOOR, 1, 1, 1.6f)
    );
    private static final Map<String, Furnishing> BY_ASSET = ALL.stream().collect(
            Collectors.toUnmodifiableMap(Furnishing::asset, Function.identity()));
    public static List<Furnishing> all() { return ALL; }
    public static Furnishing find(String asset) { return asset == null ? null : BY_ASSET.get(asset); }
    public static boolean is(String asset, Placement placement) {
        Furnishing item = find(asset);
        return item != null && item.placement() == placement;
    }
}
