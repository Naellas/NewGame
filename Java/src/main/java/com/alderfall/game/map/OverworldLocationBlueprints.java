package com.alderfall.game.map;

import java.util.List;

/**
 * Authored, reusable overworld location instances. A blueprint describes a complete readable site:
 * its main structure, perimeter, fixed activity clusters, and low-priority dressing. WorldMap owns
 * collision and placement validation; these records only describe intent.
 */
final class OverworldLocationBlueprints {
    enum SlotRole { STRUCTURE, LANDMARK, DRESSING }

    record Slot(int dx, int dy, List<String> assets, SlotRole role, boolean allowEntranceReserve) {
        Slot(int dx, int dy, String asset, SlotRole role, boolean allowEntranceReserve) {
            this(dx, dy, List.of(asset), role, allowEntranceReserve);
        }
    }

    record Blueprint(
            String kind,
            String perimeterAsset,
            int perimeterChance,
            int perimeterSpacing,
            List<String> sideAssets,
            int sideChance,
            int sideSpacing,
            List<Slot> slots,
            List<String> scatterAssets,
            int scatterChance
    ) {
        boolean hasPerimeter() { return perimeterAsset != null && !perimeterAsset.isBlank(); }
        boolean hasSideDressing() { return sideAssets != null && !sideAssets.isEmpty() && sideChance > 0; }
        boolean hasScatter() { return scatterAssets != null && !scatterAssets.isEmpty() && scatterChance > 0; }
    }

    private OverworldLocationBlueprints() { }

    static Blueprint forSite(String kind, int seed, String intent, String climate) {
        Blueprint base = forKind(intent.equals("graveyard") ? "graveyard" : kind, seed);
        if (base == null) return null;
        List<String> regional = DungeonExteriorCatalog.regionalProps(climate);
        List<String> theme = switch (intent) {
            case "vampire_lair" -> List.of("dungeon_detail_vampire_coffin", "dungeon_detail_vampire_banner",
                    "dungeon_detail_vampire_gargoyle", "dungeon_detail_vampire_candelabrum");
            case "magic_tower" -> List.of("dungeon_prop_rune_pillar", "deco_imagen_crystal_cluster",
                    "dungeon_prop_castle_books", "dungeon_prop_castle_altar");
            case "castle" -> List.of("location_camp_crates", "dungeon_prop_castle_rubble",
                    "dungeon_prop_castle_statue", "location_dungeon_braziers");
            case "mine" -> List.of("location_camp_crates", "dungeon_detail_bandit_supply_sacks",
                    "dungeon_prop_lantern_stand", "dungeon_detail_cave_rock_cluster");
            default -> kind.equals("cave") ? regional : base.scatterAssets();
        };
        java.util.ArrayList<Slot> slots = new java.util.ArrayList<>();
        for (Slot slot : base.slots()) {
            if (slot.role() == SlotRole.STRUCTURE) {
                slots.add(new Slot(slot.dx(), slot.dy(), DungeonExteriorCatalog.entrance(kind, intent, climate),
                        slot.role(), slot.allowEntranceReserve()));
            } else if (kind.equals("cave") || kind.equals("abandoned_castle")) {
                slots.add(new Slot(slot.dx(), slot.dy(), theme.get(slots.size() % theme.size()),
                        slot.role(), slot.allowEntranceReserve()));
            } else slots.add(slot);
        }
        // Two peripheral habitat clusters frame the site without occupying its south approach.
        slots.add(new Slot(-4, -2, regional.get(0), SlotRole.DRESSING, false));
        slots.add(new Slot(4, -2, regional.get(2), SlotRole.DRESSING, false));
        java.util.ArrayList<String> sides = new java.util.ArrayList<>(regional);
        if (!kind.equals("cave")) sides.addAll(theme);
        return new Blueprint(kind, base.perimeterAsset(), base.perimeterChance(), base.perimeterSpacing(),
                sides, 42, 3, List.copyOf(slots), theme, base.scatterChance());
    }

    static Blueprint forKind(String kind) {
        return switch (kind) {
            case "goblin_camp" -> camp(kind, "location_goblin_hut", true);
            case "bandit_camp" -> camp(kind, "location_bandit_outpost", false);
            case "cave" -> new Blueprint(kind, null, 0, 0,
                    List.of("deco_rocks", "location_dungeon_rubble_cairn", "location_ruin_standing_stones"), 42, 3,
                    List.of(
                            slot(0, 0, "location_overgrown_cave_entrance", SlotRole.STRUCTURE, true),
                            slot(-3, 2, "location_dungeon_rubble_cairn", SlotRole.LANDMARK, false),
                            slot(-4, 3, "dungeon_detail_cave_rock_cluster", SlotRole.DRESSING, false),
                            slot(3, 2, "location_dungeon_collapsed_wall", SlotRole.LANDMARK, false),
                            slot(4, 3, "dungeon_prop_cave_torch", SlotRole.DRESSING, false),
                            slot(-1, -3, "location_graveyard_dead_stump", SlotRole.DRESSING, false)
                    ),
                    List.of("deco_rocks", "dungeon_detail_cave_rock_cluster", "deco_imagen_crystal_cluster",
                            "location_graveyard_skull_marker"), 18);
            case "crypt" -> graveSite(kind, "location_crypt_sarcophagus", true);
            case "graveyard" -> graveSite(kind, "location_dungeon_broken_altar", false);
            case "abandoned_castle" -> fortress(kind, false);
            case "prison" -> fortress(kind, true);
            case "sewer" -> new Blueprint(kind, null, 0, 0,
                    List.of("deco_soft_water_wet_stones", "deco_soft_water_reeds_gold"), 48, 2,
                    List.of(
                            slot(-2, 1, "dungeon_prop_lantern_stand", SlotRole.LANDMARK, true),
                            slot(2, 1, "deco_soft_water_reeds_gold", SlotRole.DRESSING, true),
                            slot(-3, 0, "deco_soft_water_wet_stones", SlotRole.DRESSING, false),
                            slot(3, 1, "deco_imagen_marsh_bubble_pool", SlotRole.DRESSING, false)
                    ),
                    List.of("deco_soft_water_wet_stones", "deco_soft_water_reeds_gold",
                            "location_dungeon_rubble_cairn", "dungeon_prop_lantern_stand"), 20);
            default -> null;
        };
    }

    /** Deterministic instance transform: keeps authored relationships while avoiding cloned-looking sites. */
    static Blueprint forKind(String kind, int seed) {
        Blueprint blueprint = forKind(kind);
        if (blueprint == null || Math.floorMod(seed, 2) == 0) {
            return blueprint;
        }
        List<Slot> mirrored = blueprint.slots().stream()
                .map(slot -> new Slot(-slot.dx(), slot.dy(), slot.assets(), slot.role(), slot.allowEntranceReserve()))
                .toList();
        return new Blueprint(blueprint.kind(), blueprint.perimeterAsset(), blueprint.perimeterChance(),
                blueprint.perimeterSpacing(), blueprint.sideAssets(), blueprint.sideChance(), blueprint.sideSpacing(),
                mirrored, blueprint.scatterAssets(), blueprint.scatterChance());
    }

    private static Blueprint camp(String kind, String structure, boolean goblin) {
        return new Blueprint(kind, "location_camp_palisade", 72, 2,
                List.of("location_camp_crates", "location_camp_tent", "deco_imagen_road_camp"), 48, 3,
                List.of(
                        slot(0, 0, structure, SlotRole.STRUCTURE, true),
                        slot(-1, 2, "location_camp_fire", SlotRole.LANDMARK, false),
                        slot(-2, 3, goblin ? "dungeon_detail_goblin_scrap_heap" : "dungeon_detail_bandit_supply_sacks", SlotRole.DRESSING, false),
                        slot(2, 3, goblin ? "dungeon_detail_goblin_spike_fence" : "dungeon_detail_bandit_barricade", SlotRole.DRESSING, false),
                        slot(3, -1, "location_camp_tent", SlotRole.LANDMARK, false),
                        slot(3, 0, "location_camp_crates", SlotRole.DRESSING, false),
                        slot(-3, -1, "location_camp_tent", SlotRole.LANDMARK, false),
                        slot(-2, -1, goblin ? "dungeon_detail_goblin_totem" : "dungeon_detail_bandit_weapon_rack",
                                SlotRole.DRESSING, false)
                ),
                List.of("location_camp_crates", "location_camp_fire",
                        goblin ? "dungeon_detail_goblin_cookpot" : "dungeon_detail_bandit_bedroll",
                        "dungeon_prop_chain_stand"), 18);
    }

    private static Blueprint graveSite(String kind, String altar, boolean crypt) {
        return new Blueprint(kind, "location_graveyard_iron_fence", 78, 1,
                List.of("location_graveyard_tombstones", "location_dungeon_grave_slabs",
                        "location_graveyard_dead_stump", "location_dungeon_rubble_cairn"), 44, 3,
                List.of(
                        slot(-2, 1, "location_dungeon_braziers", SlotRole.LANDMARK, true),
                        slot(2, 1, "location_dungeon_braziers", SlotRole.LANDMARK, true),
                        slot(0, -2, altar, SlotRole.LANDMARK, false),
                        slot(-3, -1, "dungeon_detail_crypt_grave_marker", SlotRole.DRESSING, false),
                        slot(-2, 0, "location_dungeon_grave_slabs", SlotRole.DRESSING, false),
                        slot(3, 0, crypt ? "dungeon_detail_crypt_urn" : "location_graveyard_skull_marker",
                                SlotRole.DRESSING, false)
                ),
                List.of("location_graveyard_tombstones", "location_graveyard_skull_marker",
                        "location_dungeon_grave_slabs", "location_dungeon_rubble_cairn"), 20);
    }

    private static Blueprint fortress(String kind, boolean prison) {
        return new Blueprint(kind, null, 0, 0,
                List.of("location_dungeon_rubble_cairn", "deco_imagen_flat_stone_stack",
                        prison ? "dungeon_prop_chain_stand" : "dungeon_prop_castle_rubble"), 32, 3,
                List.of(
                        slot(0, 0, "location_dungeon_fortress_gate_imagegen", SlotRole.STRUCTURE, true),
                        slot(-3, 2, "location_dungeon_rubble_cairn", SlotRole.LANDMARK, false),
                        slot(-4, 3, prison ? "dungeon_prop_lantern_stand" : "dungeon_detail_vampire_gargoyle", SlotRole.DRESSING, false),
                        slot(3, 2, "location_dungeon_rubble_cairn", SlotRole.LANDMARK, false),
                        slot(4, 3, prison ? "dungeon_prop_chain_stand" : "dungeon_detail_vampire_banner",
                                SlotRole.DRESSING, false)
                ),
                List.of("location_dungeon_rubble_cairn", "location_dungeon_grave_slabs",
                        prison ? "dungeon_prop_chain_stand" : "dungeon_prop_castle_rubble"), 14);
    }

    private static Slot slot(int dx, int dy, String asset, SlotRole role, boolean allowEntranceReserve) {
        return new Slot(dx, dy, asset, role, allowEntranceReserve);
    }
}
