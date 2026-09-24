package com.alderfall.game;

import java.util.List;
import java.util.Map;

/** Cardinal direction is the direction a seated occupant faces. Legacy sprite IDs stay stable. */
public final class DirectionalSeating {
    private DirectionalSeating() { }
    public static final List<String> DIRECTIONS = List.of("north", "east", "south", "west");
    public record Family(String id, String label, boolean interior) {
        public String asset(String direction) {
            if (id.equals("interior_chair")) return "interior_chair_" + switch (direction) {
                case "north" -> "south"; case "south" -> "north"; case "east" -> "west"; default -> "east";
            };
            return id.equals("interior_chair_alt") ? "interior_chair_" + direction + "_alt" : id + "_" + direction;
        }
    }
    public static final List<Family> FAMILIES = List.of(
            new Family("interior_chair", "Wood Chair", true),
            new Family("interior_chair_alt", "Padded Chair", true),
            new Family("interior_armchair_green", "Green Armchair", true),
            new Family("interior_bench", "Backless Bench", true),
            new Family("village_prop_bench", "Rustic Bench", false),
            new Family("city_prop_refresh_bench", "Iron-frame Bench", false),
            new Family("city_prop_stone_bench", "Stone Bench", false));
    private static final Map<String, String> ALIASES = Map.of(
            "interior_armchair_green_south", "interior_armchair_green",
            "city_prop_refresh_bench_south", "city_prop_refresh_bench",
            "city_prop_stone_bench_south", "city_prop_stone_bench",
            "interior_bench_north", "interior_bench_h", "interior_bench_south", "interior_bench_h",
            "interior_bench_east", "interior_bench_v", "interior_bench_west", "interior_bench_v");
    public static String source(String asset) { return ALIASES.getOrDefault(asset, asset); }
    public static List<VillageManager.PlaceableAsset> addTo(List<VillageManager.PlaceableAsset> existing, boolean interior) {
        var result = new java.util.ArrayList<>(existing);
        for (Family family : FAMILIES) if (family.interior() == interior) for (String direction : DIRECTIONS) {
            String asset = family.asset(direction);
            result.removeIf(p -> p.asset().equals(asset));
            result.add(new VillageManager.PlaceableAsset(
                    asset, family.label() + " " + direction, 48,
                    VillageManager.VillageCost.of(5, Map.of(family.id().contains("stone") ? "stone" : "wood", 2)),
                    interior ? "Seating" : "Decor"));
        }
        return List.copyOf(result);
    }
}
