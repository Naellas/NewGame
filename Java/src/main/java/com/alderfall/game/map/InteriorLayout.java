package com.alderfall.game.map;

import com.alderfall.game.InteriorStyle;
import com.alderfall.game.InteriorFurnishings;
import com.alderfall.game.TilePoint;
import com.alderfall.game.WorldProp;
import java.util.ArrayDeque;
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
        return compose(theme, seed, style, true);
    }

    public static InteriorLayout compose(String theme, int seed, InteriorStyle style, boolean variedFootprint) {
        int width = switch (theme) {
            case "blacksmith" -> 20; case "carpenter" -> 21;
            case "bakery", "shop", "alchemy" -> 22; case "inn" -> 25;
            case "tavern" -> 24; case "study" -> 23;
            case "granary", "smokehouse", "ferry_lodge", "remembrance_hall", "cistern_house", "bellhouse", "reedworks" -> 22;
            case "caravanserai", "rescue_lodge" -> 25;
            default -> Math.floorMod(seed, 3) == 0 ? 16 : 17;
        };
        int height = switch (theme) {
            case "inn", "tavern" -> 15; case "shop" -> 13;
            case "blacksmith", "carpenter", "bakery", "study", "alchemy" -> 14;
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
            case "alchemy" -> plan.alchemy();
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
        if (!Set.of("blacksmith", "carpenter", "bakery", "study", "alchemy", "shop").contains(theme)) {
            plan.householdDetails(theme, seed);
            plan.completeActivityGroups(theme);
        }
        if (variedFootprint) plan = plan.withFootprint(theme, seed);
        plan.addRoomGraph(seed);
        return plan;
    }

    public static final List<String> THEMES = List.of("home", "inn", "tavern", "bakery", "shop", "study", "alchemy",
            "blacksmith", "carpenter", "granary", "smokehouse", "ferry_lodge", "remembrance_hall",
            "cistern_house", "bellhouse", "reedworks", "caravanserai", "rescue_lodge");

    /** Additional objects belong to a named activity, never to a room-wide density quota. */
    private void completeActivityGroups(String theme) {
        switch (theme) {
            case "home" -> {
                put(2, 4, "washstand"); put(5, 4, "coat_stand");
                put(width - 3, 2, "pantry_shelf");
                put(2, height - 3, "firewood_basket"); put(width - 3, height - 3, "pottery_cluster");
            }
            case "inn", "tavern", "caravanserai" -> {
                put(2, 2, "pantry_shelf"); put(2, 3, "plate_rack");
                put(8, 3, "bottle_crate"); put(9, 5, "cask_on_side");
                zone("table_service", 2, 10, 3, 1); put(2, 10, "basket_empty"); put(3, 10, "plate_rack");
                put(2, 11, "coat_stand"); put(2, 12, "broom"); put(9, 12, "floor_leafy_plant");
                put(15, 4, "crockery_cupboard"); put(16, 3, "bottle_crate"); put(13, 4, "pantry_shelf");
                int bedroomX = width - 6;
                put(bedroomX, 2, "coat_stand"); put(bedroomX, 6, "stool");
                put(bedroomX + 1, 6, "ironbound_chest");
                if (!theme.equals("tavern")) {
                    put(bedroomX, 8, "coat_stand"); put(bedroomX, 12, "stool");
                    put(bedroomX + 1, 12, "ironbound_chest");
                } else { put(bedroomX + 1, 10, "bottle_crate"); put(bedroomX + 3, 11, "supplies_shelf"); }
            }
            case "study" -> {
                put(3, 4, "bookshelf"); put(7, 4, "bookshelf");
                zone("reading_tables", 7, 5, 2, 6);
                for (int y : new int[]{6, 9}) {
                    put(7, y, "study_desk_h"); put(7, y + 1, "chair_south"); put(7, y, "tabletop_books");
                }
                put(15, 4, "ironbound_chest"); put(19, 4, "ironbound_chest");
                zone("archive_screen", 14, 5, 2, 1); put(14, 5, "room_screen");
                put(19, 9, "armchair_green"); put(19, 10, "floor_leafy_plant");
            }
            case "remembrance_hall" -> {
                // Hearing benches face a common records wall; a screen shelters the family memorial.
                props.removeIf(p -> p.asset().equals("interior_bench_h"));
                for (int y : new int[]{7, 9}) for (int x : new int[]{3, 6}) put(x, y, "bench_h");
                put(3, 4, "armchair_green"); put(4, 4, "side_table"); put(4, 4, "tabletop_books");
                put(18, 2, "bookshelf"); put(18, 5, "chest_of_drawers"); put(18, 5, "tabletop_inkwell");
                put(13, 4, "armchair_green"); put(13, 3, "side_table"); put(13, 3, "tabletop_scrolls");
                // Records, public testimony and private remembrance have distinct thresholds.
                wallH(1, 8, 5, 6); lamp(3, 5);
                wallV(12, 1, height - 2, 5); tiles[10][12] = 'e';
                wallH(12, width - 2, 7, 16); lamp(14, 7);
                put(4, 6, "study_desk_h"); put(4, 6, "tabletop_scrolls");
                rug(3, 6, 7, 9);
                put(13, 9, "floor_flower_planter"); put(19, 9, "floor_flower_planter");
                put(16, 9, "side_table"); put(16, 9, "tabletop_books");
                put(2, 10, "coat_stand");
            }
            case "bakery" -> {
                put(2, 2, "pantry_shelf"); put(2, 5, "firewood_basket");
                put(7, 4, "produce_corn"); put(8, 4, "basket_empty");
                put(16, 3, "produce_apples"); put(18, 4, "pottery_cluster");
                put(14, 4, "open_storage_bin"); put(19, 4, "pantry_shelf");
                put(14, 9, "bucket"); put(18, 10, "broom");
            }
            case "shop" -> {
                put(2, 2, "supplies_shelf"); put(8, 3, "open_storage_bin");
                put(14, 3, "produce_apples"); put(17, 3, "produce_potatoes");
                put(17, 5, "basket_demijohn"); put(19, 3, "pottery_cluster");
                put(14, 8, "bottle_crate"); put(18, 9, "handcart");
                put(2, 9, "coat_stand");
            }
            case "blacksmith", "carpenter" -> {
                boolean smith = theme.equals("blacksmith");
                put(2, 4, smith ? "bucket" : "leaning_ladder");
                put(7, 4, "tools_shelf"); put(2, 7, smith ? "coal_crate" : "tied_bales");
                put(7, 9, "stool"); put(12, 4, "tools_shelf");
                put(16, 4, smith ? "ironbound_chest" : "open_storage_bin");
                put(12, 10, "bucket"); put(17, 10, "broom");
            }
            case "granary", "smokehouse" -> {
                boolean smoke = theme.equals("smokehouse");
                put(2, 2, smoke ? "firewood_basket" : "open_storage_bin");
                put(5, 2, smoke ? "bottle_crate" : "produce_corn");
                put(8, 3, "supplies_shelf"); put(5, 5, smoke ? "produce_fish" : "tied_bales");
                put(19, 2, "pantry_shelf"); put(17, 5, "basket_empty");
                put(3, 9, "tabletop_scrolls"); put(7, 10, "broom"); put(19, 10, "bucket");
                put(4, 2, smoke ? "bottle_crate" : "grain_sacks_v");
                put(16, 2, smoke ? "herb_drying_rack_v" : "grain_sacks_v");
                put(8, 5, smoke ? "bottle_crate" : "produce_potatoes");
                // Keep the reserve bays behind the public tally and distribution counters.
                wallH(1, 8, 7, 6); wallH(13, width - 2, 7, 16);
                lamp(3, 7); lamp(18, 7);
            }
            case "ferry_lodge" -> {
                put(6, 2, "supplies_shelf"); put(3, 5, "tabletop_scrolls"); put(3, 4, "stool");
                put(19, 2, "tools_shelf"); put(18, 5, "bucket"); put(17, 4, "tied_bales");
                put(2, 9, "coat_stand"); put(8, 10, "basket_demijohn");
                put(17, 8, "sofa_red"); put(19, 10, "firewood_basket");
            }
            case "cistern_house" -> {
                put(2, 2, "bucket"); put(4, 3, "pottery_cluster"); put(7, 4, "basket_demijohn");
                put(18, 2, "supplies_shelf"); put(15, 5, "tabletop_scrolls"); put(15, 4, "stool");
                put(2, 10, "pottery_cluster"); put(7, 8, "cask_on_side");
                put(19, 8, "tools_shelf"); put(18, 10, "broom");
            }
            case "bellhouse", "reedworks" -> {
                put(2, 2, "tools_shelf"); put(7, 4, "supplies_shelf");
                put(3, 5, "stool"); put(19, 2, "supplies_shelf");
                put(17, 4, "tied_bales"); put(18, 5, "bucket");
                put(2, 10, "broom"); put(5, 10, "open_storage_bin"); put(19, 9, "pottery_cluster");
            }
            case "rescue_lodge" -> {
                put(6, 2, "supplies_shelf"); put(3, 5, "tabletop_scrolls");
                put(7, 4, "coat_stand"); put(16, 4, "washstand"); put(13, 4, "tied_bales");
                put(13, 9, "armchair_green"); put(13, 10, "side_table"); put(13, 10, "tabletop_jug");
                put(15, 10, "firewood_basket"); put(7, 10, "bottle_crate");
            }
            default -> { }
        }
    }

    /** Extend the shell as a whole, keeping the main rooms and the centered exit intact. */
    private InteriorLayout withFootprint(String theme, int seed) {
        int variant = Math.floorMod(seed, 4);
        if (variant == 0) return this;
        if (variant == 1) return vestibule();
        boolean left = variant == 2;
        int accessX = left ? 2 : width - 3;
        for (int portal = 3; portal <= height - 5; portal++) {
            if (!clearFloor(accessX, portal) || !clearFloor(accessX, portal + 1)) continue;
            int wallX = left ? 1 : width - 2;
            if (tiles[portal][wallX] != 'o' || tiles[portal + 1][wallX] != 'o') continue;
            InteriorLayout extended = copyInto(width + 8, height, 4);
            int outerX = left ? 1 : extended.width - 2;
            int sharedX = wallX + 4;
            int x1 = Math.min(outerX, sharedX), x2 = Math.max(outerX, sharedX);
            int top = portal - 2, bottom = portal + 3;
            for (int y = top; y <= bottom; y++) for (int x = x1; x <= x2; x++) {
                // Keep the shared wall and cut only the deliberate doorway through it.
                if (x == sharedX) continue;
                extended.tiles[y][x] = x == outerX || y == top || y == bottom ? 'o' : 'i';
            }
            extended.tiles[portal][sharedX] = 'e';
            extended.circulation.add(new TilePoint(sharedX, portal));
            for (int y = portal; y <= portal + 1; y++) {
                for (int x = x1 + 1; x < x2; x++) extended.circulation.add(new TilePoint(x, y));
                extended.circulation.add(new TilePoint(accessX + 4, y));
            }
            int doorwayY = portal;
            boolean privateRoom = zones.stream().anyMatch(z -> (z.purpose().contains("room")
                    || z.purpose().contains("bunk") || z.purpose().equals("sleeping")) && z.contains(accessX, doorwayY));
            extended.furnishAnnex(theme, x1 + 1, top + 1, privateRoom);
            return extended;
        }
        return vestibule();
    }

    private boolean clearFloor(int x, int y) {
        if (tiles[y][x] != 'i' && tiles[y][x] != 'z') return false;
        for (WorldProp prop : props) {
            int[] footprint = WorldMap.interiorVisualFootprint(prop.asset());
            if (x >= prop.x() && x < prop.x() + footprint[0]
                    && y >= prop.y() && y < prop.y() + footprint[1]) return false;
        }
        return residents.stream().noneMatch(p -> p.x() == x && p.y() == y);
    }

    private InteriorLayout copyInto(int newWidth, int newHeight, int offsetX) {
        InteriorLayout result = new InteriorLayout(newWidth, newHeight, style);
        for (char[] row : result.tiles) Arrays.fill(row, 'x');
        for (int y = 0; y < height; y++) System.arraycopy(tiles[y], 0, result.tiles[y], offsetX, width);
        for (WorldProp p : props) result.props.add(new WorldProp(p.x() + offsetX, p.y(), p.asset(), p.size()));
        for (TilePoint p : residents) result.residents.add(new TilePoint(p.x() + offsetX, p.y()));
        for (Zone z : zones) result.zone(z.purpose(), z.x() + offsetX, z.y(), z.width(), z.height());
        result.circulation.clear();
        for (TilePoint p : circulation) result.circulation.add(new TilePoint(p.x() + offsetX, p.y()));
        return result;
    }

    private InteriorLayout vestibule() {
        InteriorLayout result = copyInto(width, height + 2, 0);
        int entry = width / 2 - 1;
        for (int y = height - 1; y <= height; y++) {
            for (int x = entry - 3; x <= entry + 4; x++) {
                result.tiles[y][x] = x == entry - 3 || x == entry + 4 || y == height ? 'o' : 'i';
            }
            result.tiles[y][entry] = result.tiles[y][entry + 1] = y == height ? 'e' : 'i';
            result.circulation.add(new TilePoint(entry, y));
            result.circulation.add(new TilePoint(entry + 1, y));
        }
        result.zone("entrance_vestibule", entry - 2, height - 1, 6, 1);
        result.put(entry - 2, height - 1, "coat_stand");
        result.put(entry + 3, height - 1, "washstand");
        return result;
    }

    private void furnishAnnex(String theme, int x, int y, boolean privateRoom) {
        String role;
        String first;
        String second;
        String detail;
        switch (theme) {
            case "alchemy" -> {
                role = "herb_reserve_annex"; first = "apothecary_storage"; second = "bottle_crate"; detail = "mortar_pestle";
            }
            case "study", "remembrance_hall", "bellhouse" -> {
                role = "records_annex"; first = "bookshelf"; second = "ironbound_chest"; detail = "tabletop_scrolls";
            }
            case "blacksmith", "carpenter", "reedworks", "ferry_lodge" -> {
                role = "equipment_annex"; first = "tools_shelf"; second = "open_storage_bin"; detail = "tabletop_tankard";
            }
            case "shop", "bakery", "granary", "smokehouse", "inn", "tavern", "caravanserai" -> {
                role = "pantry_annex"; first = "pantry_shelf"; second = "basket_demijohn"; detail = "tabletop_jug";
            }
            case "cistern_house" -> {
                role = "water_vessels_annex"; first = "bucket"; second = "pottery_cluster"; detail = "tabletop_jug";
            }
            case "rescue_lodge" -> {
                role = "rescue_supplies_annex"; first = "supplies_shelf"; second = "tied_bales"; detail = "tabletop_jug";
            }
            default -> {
                role = "household_annex"; first = "crockery_cupboard"; second = "wardrobe"; detail = "tabletop_books";
            }
        }
        if (privateRoom) {
            role = "dressing_annex"; first = "wardrobe"; second = "washstand"; detail = "tabletop_books";
        }
        zone(role, x, y, 3, 4);
        put(x, y, first); put(x + 2, y, second);
        put(x, y + 3, "low_cupboard"); put(x, y + 3, detail);
        put(x + 2, y + 3, style == InteriorStyle.FENLANDS ? "floor_reed_pot" : "pottery_cluster");
        lamp(x, y - 1); put(x + 1, y - 1, "wall_window_wide");
    }

    /**
     * Add room-to-room boundaries as connected runs, leaving authored furniture,
     * residents, and the main circulation spine intact. Seeded layouts can read as
     * one open hall, two linked rooms, or a small dungeon-like room graph.
     */
    private void addRoomGraph(int seed) {
        int variant = Math.floorMod(seed, 3);
        if (variant == 0) return;
        int entry = width / 2 - 1;
        int preferred = Math.max(3, height / 2 + Math.floorMod(seed, 3) - 1);
        int partitionY = -1;
        char[][] beforePartition = copyTiles();
        for (int offset = 0; offset < height; offset++) {
            int y = 2 + Math.floorMod(preferred - 2 + offset, height - 3);
            if (y >= height - 2 || !clearHorizontalPartition(y, entry)) continue;
            for (int x = 1; x < width - 1; x++) {
                if (x == entry || x == entry + 1 || tiles[y][x] == 'e') {
                    tiles[y][x] = 'e';
                } else {
                    tiles[y][x] = 'o';
                }
            }
            partitionY = y;
            break;
        }
        if (partitionY < 0) return;
        if (!allFloorTilesReachable()) {
            restoreTiles(beforePartition);
            return;
        }
        if (variant == 1) return;

        // A perpendicular upper branch forms a T junction when the selected run
        // is clear. This changes the room graph while preserving a doorway through
        // both connected boundaries.
        for (int offset = 0; offset < width; offset++) {
            int x = 2 + Math.floorMod(Math.floorMod(seed / 3, width - 4) + offset, width - 4);
            if (x == entry || x == entry + 1) continue;
            int doorY = Math.max(3, partitionY / 2);
            if (!clearVerticalPartition(x, partitionY, doorY)) continue;
            char[][] beforeBranch = copyTiles();
            for (int y = 2; y < partitionY; y++) {
                if (y == doorY || tiles[y][x] == 'e') tiles[y][x] = 'e';
                else tiles[y][x] = 'o';
            }
            if (!allFloorTilesReachable()) restoreTiles(beforeBranch);
            break;
        }
    }

    private char[][] copyTiles() {
        char[][] copy = new char[height][];
        for (int y = 0; y < height; y++) copy[y] = tiles[y].clone();
        return copy;
    }

    private void restoreTiles(char[][] source) {
        for (int y = 0; y < height; y++) System.arraycopy(source[y], 0, tiles[y], 0, width);
    }

    private boolean allFloorTilesReachable() {
        int entry = width / 2 - 1;
        boolean[][] reached = new boolean[height][width];
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        reached[height - 2][entry] = true;
        queue.add(new TilePoint(entry, height - 2));
        while (!queue.isEmpty()) {
            TilePoint current = queue.removeFirst();
            for (int[] direction : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int x = current.x() + direction[0], y = current.y() + direction[1];
                if (x < 0 || y < 0 || x >= width || y >= height || reached[y][x]) continue;
                if (tiles[y][x] == 'x' || tiles[y][x] == 'o') continue;
                reached[y][x] = true;
                queue.addLast(new TilePoint(x, y));
            }
        }
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (tiles[y][x] != 'x' && tiles[y][x] != 'o' && !reached[y][x]) return false;
        }
        return true;
    }

    private boolean clearHorizontalPartition(int y, int entry) {
        for (int x = 1; x < width - 1; x++) {
            if (x == entry || x == entry + 1) continue;
            if (occupiedByPlannedObject(x, y) || residents.contains(new TilePoint(x, y))) return false;
            if (tiles[y][x] != 'i' && tiles[y][x] != 'z') return false;
        }
        return true;
    }

    private boolean clearVerticalPartition(int x, int endY, int doorY) {
        for (int y = 2; y < endY; y++) {
            if (y == doorY) continue;
            if (occupiedByPlannedObject(x, y) || residents.contains(new TilePoint(x, y))) return false;
            if (tiles[y][x] != 'i' && tiles[y][x] != 'z') return false;
            if (circulation.contains(new TilePoint(x, y))) return false;
        }
        return true;
    }

    private boolean occupiedByPlannedObject(int x, int y) {
        for (WorldProp prop : props) {
            int[] footprint = WorldMap.interiorVisualFootprint(prop.asset());
            if (x >= prop.x() && x < prop.x() + footprint[0]
                    && y >= prop.y() && y < prop.y() + footprint[1]) return true;
        }
        return false;
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
        zone("welcome", 2, 11, 8, 2);
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

    /** Purpose-built connected runs; aisles separate production, stock and customers. */
    private void run(int x, int y, int bays, String asset) {
        int step = WorldMap.interiorVisualFootprint("interior_" + asset)[0];
        for (int i = 0; i < bays; i++) put(x + i * step, y, asset);
    }

    private void tradeWall(String accent, int windowX) {
        lamp(2, 1); lamp(width - 3, 1);
        put(4, 1, accent); put(windowX, 1, "wall_window_wide");
    }

    private void bakery() {
        zone("oven_and_flour", 2, 2, 7, 3);
        put(2, 2, "bakery_oven"); put(2, 4, "firewood_basket");
        run(4, 2, 2, "bakehouse_storage"); put(8, 2, "grain_sacks_v");
        zone("mixing_and_proving", 2, 5, 7, 2);
        run(3, 5, 3, "bakehouse_worktop");
        put(2, 5, "bucket"); put(2, 6, "broom");
        zone("bread_service", 13, 2, 7, 6);
        run(13, 2, 3, "bakehouse_storage");
        run(13, 6, 3, "bakery_counter"); put(19, 4, "basket_empty");
        zone("bakers_meal", 2, 8, 7, 3); banquet(4, 9);
        rug(3, 8, 6, 10); put(7, 9, "crockery_cupboard");
        zone("reserve_ingredients", 13, 9, 7, 2);
        run(13, 9, 2, "low_cupboard");
        put(17, 9, "produce_apples"); put(18, 9, "produce_corn");
        tradeWall("wall_herb_rack", 14);
        residents.add(new TilePoint(6, 4)); residents.add(new TilePoint(15, 5));
    }

    private void shop() {
        zone("stock_wall", 2, 2, 7, 3);
        run(3, 2, 5, "supplies_shelf");
        zone("service_counter", 2, 5, 7, 2); run(3, 5, 3, "shop_counter");
        zone("household_goods", 13, 2, 7, 5);
        run(13, 2, 3, "crockery_cupboard"); run(16, 2, 3, "pantry_shelf");
        run(13, 5, 3, "low_cupboard");
        put(13, 5, "tabletop_jug"); put(17, 5, "tabletop_fruit");
        zone("packing", 13, 8, 7, 2);
        run(13, 8, 2, "storage_counter"); put(18, 8, "handcart");
        zone("waiting", 2, 8, 7, 2); put(3, 9, "bench_h"); put(6, 9, "floor_leafy_plant");
        rug(2, 8, 7, 9); put(7, 8, "coat_stand");
        tradeWall("wall_parcels", 14);
        residents.add(new TilePoint(6, 4));
    }

    private void study() {
        zone("open_library", 2, 2, 7, 3);
        run(2, 2, 3, "archive_storage"); put(8, 2, "stepladder");
        put(8, 4, "archive_lectern");
        zone("library_arcade", 9, 3, 1, 7);
        put(9, 4, "oak_support_pillar"); put(9, 9, "oak_support_pillar");
        zone("reading_tables", 2, 5, 7, 6);
        for (int y : new int[]{5, 8}) {
            run(3, y, 3, "archive_worktop");
            for (int x : new int[]{3, 5, 7}) put(x, y + 1, "chair_south");
        }
        zone("catalogue_and_records", 13, 2, 8, 3);
        run(13, 2, 3, "archive_storage"); put(19, 2, "ironbound_chest");
        zone("binding_and_copying", 13, 5, 8, 6);
        run(13, 6, 3, "archive_worktop");
        put(14, 7, "chair_south"); put(18, 7, "chair_south");
        run(13, 9, 2, "low_cupboard"); put(13, 9, "tabletop_scrolls");
        put(18, 9, "armchair_green"); put(19, 9, "side_table"); put(19, 9, "tabletop_books");
        rug(17, 8, 20, 10);
        rug(2, 5, 8, 6); rug(2, 8, 8, 9);
        put(13, 8, "room_screen");
        tradeWall("wall_books", 14);
        residents.add(new TilePoint(16, 7));
    }

    private void alchemy() {
        zone("reagents", 2, 2, 7, 3);
        run(2, 2, 3, "apothecary_storage"); put(8, 2, "herb_drying_rack_v");
        zone("distillation", 2, 5, 7, 2);
        run(3, 5, 3, "apothecary_worktop"); put(2, 5, "bucket");
        zone("bottling_and_wash", 2, 8, 7, 3);
        run(3, 8, 2, "apothecary_worktop"); put(7, 8, "washstand");
        put(3, 10, "bottle_crate"); put(4, 10, "bottle_crate");
        zone("dispensary", 13, 2, 7, 5);
        run(13, 2, 3, "apothecary_storage"); run(13, 6, 3, "apothecary_worktop");
        zone("consultation", 13, 8, 7, 3);
        put(14, 9, "armchair_green"); put(15, 9, "side_table"); put(15, 9, "tabletop_books");
        put(17, 9, "floor_leafy_plant"); put(18, 9, "crockery_cupboard");
        rug(13, 8, 17, 10);
        put(13, 8, "room_screen"); put(16, 10, "stool");
        tradeWall("wall_herb_rack", 14);
        residents.add(new TilePoint(6, 4)); residents.add(new TilePoint(15, 5));
    }

    private void workshop(boolean smith) {
        zone(smith ? "forge_and_quench" : "cutting_and_assembly", 2, 2, 6, 5);
        if (smith) {
            put(2, 2, "smith_hearth");
            put(2, 3, "coal_crate");
            run(4, 2, 2, "smith_storage");
            put(3, 4, "anvil"); put(2, 4, "bucket"); put(6, 4, "anvil_tool_rack");
            put(2, 6, "metal_crate"); put(5, 6, "metal_crate");
        } else {
            run(2, 2, 3, "joinery_storage");
            run(2, 5, 3, "joinery_worktop");
        }
        zone(smith ? "finishing_bench" : "timber_and_jigs", 2, 8, 6, 3);
        run(2, 8, 3, smith ? "smith_worktop" : "joinery_worktop");
        put(2, 10, smith ? "ironbound_chest" : "sawhorse_planks");
        put(6, 10, "open_storage_bin");
        zone("finished_goods", 12, 2, width - 14, 4);
        run(12, 2, 2, smith ? "smith_storage" : "joinery_storage");
        put(16, 2, smith ? "anvil_tool_rack" : "leaning_ladder");
        put(16, 4, "ironbound_chest");
        zone("customer_counter", 12, 6, width - 14, 1);
        run(12, 6, 2, smith ? "smith_worktop" : "joinery_worktop");
        zone("dispatch_and_orders", 12, 8, width - 14, 3);
        run(12, 9, 2, "low_cupboard"); put(12, 9, "tabletop_scrolls");
        put(16, 9, "stool"); put(16, 10, "broom");
        rug(12, 9, 16, 10);
        zone("service_threshold", 11, 7, 1, 1); put(11, 7, "oak_support_pillar");
        // Separate noisy production from finishing and the order desk, preserving open thresholds.
        wallH(1, 8, 7, 7); lamp(3, 7);
        wallH(11, width - 2, 8, 16); lamp(13, 8);
        tradeWall("wall_tools", 12);
        residents.add(new TilePoint(4, 4)); residents.add(new TilePoint(13, 5));
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
        for (int x : new int[]{3, 4, 5}) put(x, 2, "bookshelf");
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
        residents.add(new TilePoint(17, 5)); residents.add(new TilePoint(8, 8));
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

    /** Furnish the existing activity groups without moving their circulation or residents. */
    private void householdDetails(String theme, int seed) {
        for (int i = 0; i < props.size(); i++) {
            WorldProp prop = props.get(i);
            String asset = prop.asset();
            String replacement = null;
            int variant = Math.floorMod(seed + prop.x() * 17 + prop.y() * 31, 3);
            if (asset.equals("interior_crates")) {
                replacement = switch (theme) {
                    case "shop" -> new String[]{"produce_apples", "produce_cabbages", "produce_potatoes"}[variant];
                    case "granary", "bakery" -> "produce_corn";
                    case "smokehouse" -> "produce_fish";
                    case "blacksmith" -> "coal_crate";
                    case "carpenter", "bellhouse" -> "tools_shelf";
                    case "reedworks", "rescue_lodge" -> "tied_bales";
                    case "ferry_lodge", "cistern_house" -> "bucket";
                    default -> "supplies_shelf";
                };
            } else if (asset.equals("interior_bookshelf") && theme.equals("shop")) {
                replacement = variant == 0 ? "crockery_cupboard" : variant == 1 ? "pantry_shelf" : "supplies_shelf";
            } else if (asset.equals("interior_barrels") && theme.equals("blacksmith")) {
                replacement = "coal_crate";
            } else if (asset.equals("interior_hearth_pot") && !theme.equals("smokehouse")) {
                replacement = "fireplace";
            } else if (asset.equals("interior_traveler_trunk") && (theme.equals("home") || theme.equals("inn"))) {
                replacement = "wardrobe";
            } else if (asset.equals("interior_linen_shelf") && (theme.equals("inn") || theme.equals("caravanserai"))) {
                replacement = "washstand";
            } else if (asset.equals("interior_tabletop_candle") && theme.equals("study") && prop.y() == 9) {
                replacement = "tabletop_books";
            }
            if (replacement != null) props.set(i, new WorldProp(prop.x(), prop.y(), "interior_" + replacement, prop.size()));
        }

        if (theme.equals("inn") || theme.equals("tavern") || theme.equals("caravanserai")) {
            put(13, 7, "sofa_red");
            put(15, 7, "firewood_basket");
        }
        switch (theme) {
            case "home" -> put(5, 2, "chest_of_drawers");
            case "study" -> {
                put(18, 4, "chest_of_drawers"); put(18, 4, "tabletop_inkwell");
            }
            case "granary" -> {
                put(18, 8, "handcart"); put(4, 8, "pallet");
            }
            case "carpenter" -> put(18, 9, "leaning_ladder");
            case "blacksmith" -> put(6, 8, "coal_crate");
            default -> { }
        }

        // Wall accents occupy an exposed horizontal wall, never a window or lamp.
        String wall = switch (theme) {
            case "study", "remembrance_hall" -> "wall_books";
            case "shop", "granary" -> "wall_parcels";
            case "blacksmith", "carpenter", "ferry_lodge", "bellhouse", "reedworks" -> "wall_tools";
            case "rescue_lodge" -> "wall_antlers";
            default -> style == InteriorStyle.ARCHIVE ? "wall_clock" : "wall_landscape";
        };
        InteriorFurnishings.Furnishing decoration = InteriorFurnishings.find("interior_" + wall);
        for (int x = 3; x < width - 3 - decoration.width(); x++) {
            boolean clear = true;
            for (int xx = x; xx < x + decoration.width(); xx++) {
                if (tiles[1][xx] != 'o' || tiles[2][xx] == 'o') clear = false;
                for (WorldProp prop : props) {
                    int w = prop.asset().contains("wide") || prop.asset().contains("shelf")
                            || prop.asset().equals("interior_wall_herb_rack") ? 2 : 1;
                    InteriorFurnishings.Furnishing f = InteriorFurnishings.find(prop.asset());
                    if (f != null) w = f.width();
                    if (prop.y() == 1 && prop.asset().startsWith("interior_wall_")
                            && xx >= prop.x() && xx < prop.x() + w) clear = false;
                }
            }
            if (clear) { put(x, 1, wall); break; }
        }

        // One detail per otherwise bare work surface; decorated counters are excluded.
        for (WorldProp surface : List.copyOf(props)) {
            if (!surface.asset().equals("interior_low_cupboard")
                    && !surface.asset().equals("interior_storage_counter")) continue;
            boolean occupied = props.stream().anyMatch(p -> p != surface && p.x() == surface.x() && p.y() == surface.y());
            if (occupied) continue;
            String detail = switch (theme) {
                case "study", "remembrance_hall", "bellhouse" -> "tabletop_inkwell";
                case "inn", "tavern", "caravanserai" -> "tabletop_wine";
                case "shop", "granary", "bakery" -> "tabletop_fruit";
                case "cistern_house" -> "tabletop_jug";
                default -> "tabletop_tankard";
            };
            put(surface.x(), surface.y(), detail);
            break;
        }
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
    private void put(int x, int y, String asset) {
        if (asset.equals("wall_window_small")) asset = "wall_window_leaded";
        if (asset.equals("wall_window_wide") && style == InteriorStyle.ARCHIVE) asset = "wall_window_curtained";
        props.add(new WorldProp(x, y, "interior_" + asset, 48));
    }
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
