package com.alderfall.game.map;

import com.alderfall.game.InteriorStyle;
import com.alderfall.game.TilePoint;
import com.alderfall.game.WorldProp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Composed rooms: architecture, furniture groups and working positions are one plan. */
public final class InteriorLayout {
    public record Zone(String purpose, int x, int y, int width, int height) {
        public boolean contains(int px, int py) {
            return px >= x && py >= y && px < x + width && py < y + height;
        }
    }

    private final char[][] tiles;
    private final List<WorldProp> props = new ArrayList<>();
    private final List<TilePoint> residents = new ArrayList<>();
    private final List<Zone> zones = new ArrayList<>();
    private final Set<TilePoint> circulation = new LinkedHashSet<>();
    private final InteriorStyle style;
    private final int width, height;

    public static InteriorLayout compose(String theme, int seed, InteriorStyle style) {
        int width = switch (theme) {
            case "blacksmith" -> 20; case "carpenter" -> 21;
            case "bakery", "shop" -> 22; case "inn" -> 25;
            case "tavern" -> 24; case "study" -> 23;
            case "granary", "smokehouse", "ferry_lodge", "remembrance_hall", "cistern_house", "bellhouse", "reedworks" -> 22;
            case "caravanserai", "rescue_lodge" -> 25;
            default -> Math.floorMod(seed, 3) == 0 ? 16 : 17;
        };
        int height = switch (theme) {
            case "inn", "tavern" -> 15; case "shop" -> 13;
            case "blacksmith", "carpenter", "bakery", "study" -> 14;
            case "granary", "smokehouse", "ferry_lodge", "remembrance_hall", "cistern_house", "bellhouse", "reedworks" -> 14;
            case "caravanserai", "rescue_lodge" -> 15;
            default -> Math.floorMod(seed / 7, 2) == 0 ? 11 : 12;
        };
        InteriorLayout plan = new InteriorLayout(width, height, style);
        switch (theme) {
            case "inn", "tavern" -> plan.inn(theme.equals("inn"));
            case "bakery" -> plan.bakery();
            case "shop" -> plan.shop();
            case "study" -> plan.study();
            case "blacksmith", "carpenter" -> plan.workshop(theme.equals("blacksmith"));
            case "granary", "smokehouse" -> plan.provisionHouse(theme.equals("smokehouse"));
            case "ferry_lodge" -> plan.ferryLodge();
            case "remembrance_hall" -> plan.remembranceHall();
            case "cistern_house" -> plan.cisternHouse();
            case "bellhouse", "reedworks" -> plan.marshWorkshop(theme.equals("bellhouse"));
            case "caravanserai" -> plan.inn(true);
            case "rescue_lodge" -> plan.rescueLodge();
            default -> plan.home();
        }
        return plan;
    }

    private InteriorLayout(int width, int height, InteriorStyle style) {
        this.width = width; this.height = height; this.style = style;
        tiles = new char[height][width];
        for (char[] row : tiles) Arrays.fill(row, 'x');
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                tiles[y][x] = x == 1 || x == width - 2 || y == 1 || y == height - 2 ? 'o' : 'i';
            }
        }
        int entry = width / 2 - 1;
        tiles[height - 2][entry] = tiles[height - 2][entry + 1] = 'e';
        // A two-tile spine connects the threshold, public room and back of house.
        for (int y = 2; y <= height - 2; y++) {
            circulation.add(new TilePoint(entry, y));
            circulation.add(new TilePoint(entry + 1, y));
        }
    }

    private void home() {
        zone("sleeping", 2, 2, 4, 3);
        wallV(6, 1, 5, -1);
        wallH(1, 6, 5, 4);
        put(2, 2, "resident_bed"); put(4, 2, "traveler_trunk");
        lamp(2, 1); put(4, 1, "wall_window_small");
        zone("cooking", width - 7, 2, 5, 2);
        put(width - 7, 2, "stove"); put(width - 5, 2, "cooking_station");
        put(width - 7, 1, "wall_herb_rack");
        zone("dining", width - 7, 4, 4, 3);
        roundTable(width - 6, 5, true);
        put(width - 4, 1, "wall_window_small");
        zone("hearth", 2, height - 5, 4, 3);
        put(2, height - 4, "hearth_pot");
        rug(3, height - 5, 5, height - 3);
        put(4, height - 4, "side_table"); put(4, height - 4, "tabletop_candle");
        put(5, height - 4, "chair_east"); put(4, height - 3, "chair_south");
        zone("household_work", width - 6, height - 4, 4, 2);
        regionalWork(width - 6, height - 4);
        lamp(width - 3, 1);
        residents.add(new TilePoint(width - 4, 4));
        residents.add(new TilePoint(3, height - 3));
    }

    private void inn(boolean guestHouse) {
        int partition = width - 7;
        wallV(partition, 1, height - 2, 5);
        tiles[11][partition] = 'e';
        wallH(partition, width - 2, 7, -1);
        bedroom(partition + 1, 2, guestHouse ? "guest_room" : "keeper_room");
        if (guestHouse) bedroom(partition + 1, 8, "guest_room");
        else {
            zone("stores", partition + 1, 8, 4, 5);
            put(partition + 2, 8, "barrels"); put(partition + 4, 8, "grain_sacks_v");
            put(partition + 2, 11, "linen_shelf");
        }
        zone("kitchen", 2, 2, 8, 4);
        put(3, 2, "oven"); put(5, 2, "cooking_station");
        put(8, 2, "barrels"); put(9, 2, "grain_sacks_v");
        put(5, 1, "wall_herb_rack"); lamp(3, 1);
        for (int x : new int[]{3, 5, 7}) put(x, 5, "storage_counter");
        zone("dining", 2, 7, 8, 3);
        if (style == InteriorStyle.STORMBOUND) {
            // Winter hospitality is a communal table beside the service area.
            banquet(3, 8); banquet(5, 8);
        } else {
            banquet(3, 8); banquet(7, 8);
        }
        zone("hearth", 13, 6, partition - 13, 5);
        put(16, 8, "hearth_pot");
        rug(13, 8, 15, 10);
        put(14, 9, "round_table"); put(14, 9, "tabletop_candle");
        put(13, 9, "chair_west"); put(15, 9, "chair_east");
        zone("welcome", 2, 11, 7, 2);
        regionalWork(3, 12);
        put(7, 12, "bench_h");
        zone("pantry", 13, 2, partition - 13, 3);
        put(13, 2, "storage_counter"); put(16, 2, "barrels");
        put(13, 1, "wall_window_wide"); lamp(16, 1);
        residents.add(new TilePoint(6, 4)); // Host behind the working bar.
        residents.add(new TilePoint(partition - 1, 11)); // Traveler near the guest-room doors.
        residents.add(new TilePoint(9, 10)); // Tablehand beside the dining aisle.
    }

    private void bedroom(int x, int y, String role) {
        zone(role, x, y, 4, 5);
        put(x + 1, y, "resident_bed"); put(x + 3, y, "traveler_trunk");
        put(x + 3, y + 3, "linen_shelf");
        rug(x, y + 2, x + 2, y + 3);
        lamp(x + 1, y - 1);
    }

    private void bakery() {
        zone("bakehouse", 2, 2, 7, 5);
        put(3, 2, "bakery_oven"); put(5, 2, "bakery_counter");
        put(8, 2, "grain_sacks_v"); put(5, 4, "cooking_station");
        for (int x : new int[]{3, 5, 7}) put(x, 6, "bakery_counter");
        zone("dining", 3, 8, 5, 3); banquet(4, 9);
        zone("seed_stores", 14, 2, 6, 4);
        put(14, 2, "grain_sacks_v"); put(17, 2, "storage_counter");
        put(17, 2, "seed_bowl"); put(19, 2, "barrels");
        zone("household_work", 14, 8, 6, 3); regionalWork(15, 9);
        put(19, 8, "herb_drying_rack_v");
        backWall(5, 14);
        residents.add(new TilePoint(7, 5)); residents.add(new TilePoint(7, 7));
    }

    private void shop() {
        zone("stockroom", 2, 2, 7, 4);
        put(3, 2, "bookshelf"); put(5, 2, "storage_counter"); put(8, 2, "crates");
        for (int x : new int[]{3, 5, 7}) put(x, 5, "shop_counter");
        zone("display", 14, 2, 6, 5);
        put(14, 2, "bookshelf"); put(17, 2, "bookshelf");
        put(14, 5, "low_cupboard"); put(18, 5, "barrels");
        zone("waiting", 2, 8, 6, 2); put(3, 9, "bench_h");
        put(6, 9, "floor_leafy_plant");
        zone("household_work", 14, 8, 5, 3); regionalWork(15, 8);
        backWall(5, 14);
        residents.add(new TilePoint(6, 4));
    }

    private void study() {
        zone("library", 2, 2, 7, 3);
        for (int x : new int[]{3, 5, 7}) put(x, 2, "bookshelf");
        zone("reading", 3, 5, 5, 6);
        put(4, 6, "study_desk_h"); put(4, 7, "chair_south");
        put(4, 9, "study_desk_h"); put(4, 10, "chair_south");
        put(4, 6, "tabletop_candle"); put(4, 9, "tabletop_candle");
        zone("records", 14, 2, 7, 3);
        put(15, 2, "bookshelf"); put(18, 2, "bookshelf"); put(20, 2, "traveler_trunk");
        zone("scribe", 14, 6, 6, 4);
        rug(14, 6, 17, 9);
        put(15, 7, "study_desk_h"); put(15, 8, "chair_south");
        put(19, 7, "low_cupboard"); put(19, 7, "mortar_pestle");
        backWall(4, 15);
        residents.add(new TilePoint(17, 8));
    }

    private void workshop(boolean smith) {
        zone("workshop", 2, 2, 6, 8);
        put(3, 2, smith ? "forge" : "crates");
        put(6, 2, smith ? "anvil_tool_rack" : "low_cupboard");
        put(3, 5, smith ? "anvil" : "carpenter_workbench");
        put(6, 5, smith ? "metal_crate" : "sawhorse_planks");
        put(3, 8, smith ? "storage_counter" : "carpenter_workbench");
        zone("service", 12, 2, width - 14, 5);
        put(12, 2, smith ? "metal_crate" : "crates");
        put(16, 2, smith ? "anvil_tool_rack" : "sawhorse_planks");
        put(12, 6, "shop_counter"); put(14, 6, "shop_counter");
        zone("stores", 12, 8, width - 14, 3);
        put(13, 9, smith ? "metal_crate" : "sawhorse_planks");
        put(16, 9, "barrels");
        backWall(4, 12);
        residents.add(new TilePoint(smith ? 4 : 5, 5)); residents.add(new TilePoint(13, 5));
    }

    private void provisionHouse(boolean smoke) {
        zone(smoke ? "warming_bay" : "grain_reserve", 2, 2, 7, 5);
        put(3, 2, smoke ? "stove" : "grain_sacks_v");
        put(6, 2, smoke ? "herb_drying_rack_v" : "grain_sacks_v");
        put(3, 5, smoke ? "cooking_station" : "barrels");
        put(7, 5, "crates");
        zone(smoke ? "drying_bay" : "seed_reserve", 13, 2, 7, 5);
        put(14, 2, smoke ? "herb_drying_rack_v" : "grain_sacks_v");
        put(17, 2, smoke ? "herb_drying_rack_v" : "barrels");
        put(14, 5, "storage_counter"); put(14, 5, smoke ? "mortar_pestle" : "seed_bowl");
        put(18, 5, "low_cupboard");
        zone("tally_desk", 2, 8, 6, 3);
        put(3, 9, "study_desk_h"); put(3, 10, "chair_south");
        zone("distribution", 13, 8, 7, 3);
        put(14, 9, "storage_counter"); put(17, 10, "bench_h");
        backWall(6, 14);
        residents.add(new TilePoint(6, 9)); residents.add(new TilePoint(15, 8));
    }

    private void ferryLodge() {
        zone("dispatch", 2, 2, 7, 4);
        put(3, 2, "bookshelf"); put(3, 5, "study_desk_h");
        zone("equipment", 13, 2, 7, 4);
        put(14, 2, "barrels"); put(17, 2, "crates"); put(14, 5, "carpenter_workbench");
        zone("waiting", 2, 8, 7, 3);
        put(3, 8, "bench_h"); put(6, 10, "bench_h");
        zone("shelter", 13, 8, 7, 3);
        roundTable(15, 9, false); put(19, 9, "hearth_pot");
        backWall(4, 14);
        residents.add(new TilePoint(6, 5)); residents.add(new TilePoint(7, 8));
    }

    private void remembranceHall() {
        zone("family_records", 2, 2, 7, 3);
        for (int x : new int[]{3, 5, 7}) put(x, 2, "bookshelf");
        zone("public_hearing", 2, 6, 7, 5);
        put(3, 7, "bench_h"); put(6, 10, "bench_h");
        zone("reading", 13, 2, 7, 5);
        put(15, 4, "study_desk_h"); put(15, 4, "tabletop_candle");
        put(15, 5, "chair_south");
        zone("memorial", 13, 8, 7, 3);
        rug(13, 8, 18, 10);
        put(14, 9, "low_cupboard"); put(14, 9, "tabletop_candle");
        put(18, 9, "traveler_trunk");
        backWall(4, 15);
        residents.add(new TilePoint(17, 5)); residents.add(new TilePoint(7, 7));
    }

    private void cisternHouse() {
        zone("covered_water", 2, 2, 7, 5);
        props.add(new WorldProp(3, 3, "village_prop_water_trough", 48));
        props.add(new WorldProp(6, 5, "village_prop_water_trough", 48));
        put(7, 2, "barrels");
        zone("allocation_records", 13, 2, 7, 5);
        put(14, 2, "bookshelf"); put(15, 5, "study_desk_h");
        zone("first_cup", 2, 8, 7, 3);
        put(3, 9, "storage_counter"); put(3, 9, "tabletop_meal"); put(6, 10, "bench_h");
        zone("channel_maintenance", 13, 8, 7, 3);
        put(14, 9, "carpenter_workbench"); put(18, 9, "crates");
        backWall(4, 15);
        residents.add(new TilePoint(6, 8)); residents.add(new TilePoint(17, 5));
    }

    private void marshWorkshop(boolean bells) {
        zone(bells ? "warning_bell" : "reed_cutting", 2, 2, 7, 5);
        if (bells) props.add(new WorldProp(3, 3, "deco_crossing_flood_bell", 48));
        else { put(3, 2, "floor_reed_pot"); put(6, 2, "floor_reed_pot"); }
        put(3, 6, "carpenter_workbench");
        zone(bells ? "flood_records" : "drying_bundles", 13, 2, 7, 5);
        put(14, 2, bells ? "bookshelf" : "herb_drying_rack_v");
        put(17, 2, bells ? "bookshelf" : "floor_reed_pot");
        put(14, 5, bells ? "study_desk_h" : "sawhorse_planks");
        zone("repairs", 2, 8, 7, 3);
        put(3, 9, "carpenter_workbench"); put(7, 9, "crates");
        zone("finished_work", 13, 8, 7, 3);
        put(14, 9, "storage_counter"); put(18, 9, "barrels");
        backWall(4, 14);
        residents.add(new TilePoint(6, 6)); residents.add(new TilePoint(16, 8));
    }

    private void rescueLodge() {
        int partition = width - 7;
        wallV(partition, 1, height - 2, 5);
        tiles[11][partition] = 'e';
        wallH(partition, width - 2, 7, -1);
        bedroom(partition + 1, 2, "rescue_bunk"); bedroom(partition + 1, 8, "guest_bunk");
        zone("dispatch", 2, 2, 7, 4);
        put(3, 2, "bookshelf"); put(3, 5, "study_desk_h");
        zone("rescue_stores", 13, 2, 4, 4);
        put(13, 2, "linen_shelf"); put(16, 2, "traveler_trunk"); put(14, 5, "crates");
        zone("crew_table", 2, 7, 7, 4); banquet(4, 9);
        zone("warming", 13, 8, 4, 4); put(15, 9, "hearth_pot");
        backWall(4, 13);
        residents.add(new TilePoint(6, 5)); residents.add(new TilePoint(16, 11));
    }

    private void regionalWork(int x, int y) {
        put(x, y, "low_cupboard");
        props.add(new WorldProp(x, y, style.tableDetail, 48));
        String accent = switch (style) {
            case STORMBOUND -> "traveler_trunk";
            case SUNREALM -> "planting_pot";
            case FENLANDS -> "floor_reed_pot";
            case THORNMERE -> "floor_sapling_pot";
            case ARCHIVE -> "bookshelf";
            default -> "herb_planter";
        };
        // Tall winter luggage sits beside the sleeping storage instead of the threshold.
        if (style == InteriorStyle.STORMBOUND && y + 2 > height - 2) accent = "barrels";
        put(x + 2, y, accent);
    }

    private void backWall(int workX, int windowX) {
        lamp(2, 1); lamp(width - 3, 1);
        props.add(new WorldProp(workX, 1, style.wallAccent, 48));
        put(windowX, 1, "wall_window_wide");
    }

    private void banquet(int x, int y) {
        put(x, y, "banquet_table_h");
        put(x, y - 1, "bench_h"); put(x, y + 1, "bench_h");
    }

    private void roundTable(int x, int y, boolean full) {
        rug(x - 1, y - 1, x + 1, y + 1);
        put(x, y, "round_table"); put(x, y, "tabletop_meal");
        put(x - 1, y, "chair_west"); put(x + 1, y, "chair_east");
        if (full) { put(x, y - 1, "chair_north"); put(x, y + 1, "chair_south"); }
    }

    private void zone(String role, int x, int y, int w, int h) { zones.add(new Zone(role, x, y, w, h)); }
    private void put(int x, int y, String asset) { props.add(new WorldProp(x, y, "interior_" + asset, 48)); }
    private void lamp(int x, int y) { put(x, y, "wall_sconce_lamp"); }
    private void rug(int x1, int y1, int x2, int y2) {
        for (int y = y1; y <= y2; y++) for (int x = x1; x <= x2; x++) {
            if (tiles[y][x] == 'i' && !circulation.contains(new TilePoint(x, y))) tiles[y][x] = 'z';
        }
    }
    private void wallV(int x, int y1, int y2, int door) {
        for (int y = y1; y <= y2; y++) tiles[y][x] = y == door ? 'e' : 'o';
    }
    private void wallH(int x1, int x2, int y, int door) {
        for (int x = x1; x <= x2; x++) tiles[y][x] = door >= 0 && (x == door || x == door + 1) ? 'e' : 'o';
    }

    public char[][] tiles() { return tiles; }
    public List<WorldProp> props() { return List.copyOf(props); }
    public List<TilePoint> residents() { return List.copyOf(residents); }
    public List<Zone> zones() { return List.copyOf(zones); }
    public Set<TilePoint> circulation() { return Set.copyOf(circulation); }
}
