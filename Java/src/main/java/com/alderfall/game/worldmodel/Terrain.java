package com.alderfall.game;

import java.awt.Color;
import java.util.Map;
import java.util.Set;

public final class Terrain {
    public static final char DIRT_ROAD = 'r';
    public static final char VILLAGE_ROAD = 'T';
    public static final char COBBLESTONE_ROAD = 'K';
    public static final char PLANK_ROAD = '7';
    public static final char PACKED_ROAD = '8';
    /** Passable opening through an authored settlement wall. */
    public static final char CITY_GATE = 'c';

    public static final Set<Character> PASSABLE = Set.of(
            'g', 'f', 's', 'n', 'v', 'b', 'P', 'q', 'A', 'B', 'r', 'T', 'K', CITY_GATE, 'u', 'd', 'p', 'j', 'l', 'a', 'y', 't', 'i', 'e', 'z',
            'C', 'G', 'V', 'U', 'D', 'F', 'M', 'R', 'S', 'L', 'N', 'E', 'I', 'J', 'H', 'Q',
            '1', '2', '3', '4', '5', '6', PLANK_ROAD, PACKED_ROAD
    );
    public static final Set<Character> ROAD_LIKE = Set.of(
            'r', 'T', 'K', 'q', 'B', CITY_GATE, 'p', 'j', 'l', 'a', 'C', 'G', 'V', 'U', PLANK_ROAD, PACKED_ROAD
    );
    public static final Set<Character> CONNECTING_ROAD = Set.of(
            'r', 'T', 'K', 'q', 'B', CITY_GATE, 'u', 'd', PLANK_ROAD, PACKED_ROAD
    );

    private static final Map<Character, String> NAMES = Map.ofEntries(
            Map.entry('g', "Meadow"),
            Map.entry('f', "Oldwood"),
            Map.entry('s', "Sunsteppe"),
            Map.entry('n', "Frostfield"),
            Map.entry('v', "Marsh"),
            Map.entry('b', "Badlands"),
            Map.entry('P', "Beach"),
            Map.entry('~', "Submerged Sand"),
            Map.entry('m', "Mountain"),
            Map.entry('q', "Mountain Pass"),
            Map.entry('A', "Farmland"),
            Map.entry('B', "Bridge"),
            Map.entry('w', "Water"),
            Map.entry('r', "Road"),
            Map.entry('T', "Unmaintained Road"),
            Map.entry('K', "Cobblestone Road"),
            Map.entry('c', "City Gate"),
            Map.entry('u', "Village"),
            Map.entry('d', "Dungeon"),
            Map.entry('p', "Stone Plaza"),
            Map.entry('j', "Residential Court"),
            Map.entry('l', "Side Street"),
            Map.entry('a', "Market Row"),
            Map.entry('y', "Garden Court"),
            Map.entry('C', "Herringbone Cobble"),
            Map.entry('G', "Brick Crosswalk"),
            Map.entry('V', "Packed Earth"),
            Map.entry('U', "Plank Walk"),
            Map.entry(PLANK_ROAD, "Plank Walk"),
            Map.entry(PACKED_ROAD, "Packed Earth"),
            Map.entry('D', "Mosaic Floor"),
            Map.entry('F', "Cracked Flagstone"),
            Map.entry('M', "Mossy Dungeon Floor"),
            Map.entry('R', "Rubble-Strewn Floor"),
            Map.entry('S', "Sigil Floor"),
            Map.entry('L', "Torchlit Flagstone"),
            Map.entry('N', "Natural Cavern Floor"),
            Map.entry('E', "Cave Rubble Floor"),
            Map.entry('I', "Iron Prison Floor"),
            Map.entry('J', "Sewer Walkway"),
            Map.entry('H', "Castle Flagstone"),
            Map.entry('Q', "Crypt Ossuary Floor"),
            Map.entry('1', "Castle Rubble Floor"),
            Map.entry('2', "Castle Ceremonial Floor"),
            Map.entry('3', "Castle Candlelit Floor"),
            Map.entry('4', "Castle Sigil Floor"),
            Map.entry('5', "Stairs Up"),
            Map.entry('6', "Stairs Down"),
            Map.entry('O', "Cavern Wall"),
            Map.entry('Y', "Cave Water"),
            Map.entry('W', "Sewer Channel"),
            Map.entry('X', "Castle Wall"),
            Map.entry('Z', "Void"),
            Map.entry('t', "Tower"),
            Map.entry('h', "Building"),
            Map.entry('x', "Wall"),
            Map.entry('i', "Interior Floor"),
            Map.entry('e', "Doorway"),
            Map.entry('z', "Rug"),
            Map.entry('k', "Furniture"),
            Map.entry('o', "Interior Wall")
    );

    private static final Map<Character, String> ASSETS = Map.ofEntries(
            Map.entry('g', "grass"),
            Map.entry('f', "forest"),
            Map.entry('s', "desert_sand_wind"),
            Map.entry('n', "tundra"),
            Map.entry('v', "marsh"),
            Map.entry('b', "badlands"),
            Map.entry('P', "beach"),
            Map.entry('~', "water"),
            Map.entry('m', "mountain_massif_tile"),
            Map.entry('q', "mountain_pass"),
            Map.entry('A', "location_farmland_tilled"),
            Map.entry('B', "water"),
            Map.entry('w', "water"),
            Map.entry('r', "road"),
            Map.entry('T', "road_unmaintained"),
            Map.entry('K', "road_cobblestone"),
            Map.entry('c', "city"),
            Map.entry('u', "village"),
            Map.entry('d', "dungeon_floor"),
            Map.entry('p', "city_cobble"),
            Map.entry('j', "city_cobble"),
            Map.entry('l', "city_cobble"),
            Map.entry('a', "city_cobble"),
            Map.entry('y', "grass"),
            Map.entry('C', "town_herringbone_cobble"),
            Map.entry('G', "town_brick_crosswalk"),
            Map.entry('V', "road"),
            Map.entry('U', "village_plank_walk"),
            Map.entry(PLANK_ROAD, "village_plank_walk"),
            Map.entry(PACKED_ROAD, "road"),
            Map.entry('D', "dungeon_mosaic_floor"),
            Map.entry('F', "dungeon_cracked_flagstone"),
            Map.entry('M', "dungeon_moss_floor"),
            Map.entry('R', "dungeon_rubble_floor"),
            Map.entry('S', "dungeon_boss_sigil_floor"),
            Map.entry('L', "dungeon_torch_floor"),
            Map.entry('N', "dungeon_cave_floor"),
            Map.entry('E', "dungeon_cave_rubble_floor"),
            Map.entry('I', "dungeon_prison_floor"),
            Map.entry('J', "dungeon_sewer_walkway"),
            Map.entry('H', "dungeon_castle_floor"),
            Map.entry('Q', "dungeon_crypt_floor"),
            Map.entry('1', "dungeon_castle_floor_variant_2"),
            Map.entry('2', "dungeon_castle_ceremonial_floor"),
            Map.entry('3', "dungeon_castle_candle_floor"),
            Map.entry('4', "dungeon_castle_boss_sigil"),
            Map.entry('5', "dungeon_castle_stair_up"),
            Map.entry('6', "dungeon_castle_stair_down"),
            Map.entry('O', "dungeon_cave_wall"),
            Map.entry('Y', "dungeon_cave_water"),
            Map.entry('W', "dungeon_sewer_water"),
            Map.entry('X', "dungeon_castle_wall"),
            Map.entry('Z', "dungeon_void"),
            Map.entry('t', "city_cobble"),
            Map.entry('h', "city"),
            Map.entry('x', "dungeon_wall"),
            Map.entry('i', "city_cobble"),
            Map.entry('e', "road"),
            Map.entry('z', "road"),
            Map.entry('k', "city_prop_crate"),
            Map.entry('o', "dungeon_wall")
    );

    private static final Map<Character, Color> COLORS = Map.ofEntries(
            Map.entry('g', new Color(95, 159, 74)),
            Map.entry('f', new Color(35, 92, 54)),
            Map.entry('s', new Color(202, 161, 93)),
            Map.entry('n', new Color(220, 232, 240)),
            Map.entry('v', new Color(64, 107, 85)),
            Map.entry('b', new Color(143, 111, 85)),
            Map.entry('P', new Color(222, 184, 102)),
            Map.entry('~', new Color(116, 187, 183)),
            Map.entry('m', new Color(125, 133, 143)),
            Map.entry('q', new Color(119, 113, 104)),
            Map.entry('A', new Color(151, 116, 74)),
            Map.entry('B', new Color(172, 123, 67)),
            Map.entry('w', new Color(46, 135, 189)),
            Map.entry('r', new Color(217, 179, 111)),
            Map.entry('T', new Color(156, 118, 70)),
            Map.entry('K', new Color(139, 132, 116)),
            Map.entry('c', new Color(215, 191, 130)),
            Map.entry('u', new Color(185, 173, 118)),
            Map.entry('d', new Color(59, 51, 72)),
            Map.entry('p', new Color(176, 166, 145)),
            Map.entry('j', new Color(151, 145, 132)),
            Map.entry('l', new Color(125, 133, 128)),
            Map.entry('a', new Color(191, 143, 97)),
            Map.entry('y', new Color(118, 168, 100)),
            Map.entry('C', new Color(142, 135, 122)),
            Map.entry('G', new Color(121, 91, 75)),
            Map.entry('V', new Color(138, 105, 65)),
            Map.entry('U', new Color(111, 82, 48)),
            Map.entry(PLANK_ROAD, new Color(111, 82, 48)),
            Map.entry(PACKED_ROAD, new Color(138, 105, 65)),
            Map.entry('D', new Color(64, 58, 72)),
            Map.entry('F', new Color(58, 57, 62)),
            Map.entry('M', new Color(51, 68, 61)),
            Map.entry('R', new Color(54, 52, 59)),
            Map.entry('S', new Color(70, 61, 80)),
            Map.entry('L', new Color(93, 70, 50)),
            Map.entry('N', new Color(55, 58, 62)),
            Map.entry('E', new Color(63, 55, 43)),
            Map.entry('I', new Color(50, 51, 58)),
            Map.entry('J', new Color(48, 60, 55)),
            Map.entry('H', new Color(76, 72, 76)),
            Map.entry('Q', new Color(63, 58, 65)),
            Map.entry('1', new Color(73, 69, 64)),
            Map.entry('2', new Color(55, 58, 67)),
            Map.entry('3', new Color(96, 75, 48)),
            Map.entry('4', new Color(58, 59, 75)),
            Map.entry('5', new Color(69, 71, 77)),
            Map.entry('6', new Color(51, 53, 60)),
            Map.entry('O', new Color(37, 39, 42)),
            Map.entry('Y', new Color(35, 87, 111)),
            Map.entry('W', new Color(43, 78, 64)),
            Map.entry('X', new Color(31, 34, 41)),
            Map.entry('Z', new Color(4, 4, 5)),
            Map.entry('t', new Color(121, 115, 112)),
            Map.entry('h', new Color(154, 98, 62)),
            Map.entry('x', new Color(84, 82, 90)),
            Map.entry('i', new Color(138, 98, 65)),
            Map.entry('e', new Color(91, 54, 36)),
            Map.entry('z', new Color(155, 77, 68)),
            Map.entry('k', new Color(111, 74, 45)),
            Map.entry('o', new Color(111, 87, 64))
    );

    private Terrain() {
    }

    public static String name(char tile) {
        return NAMES.getOrDefault(tile, "Unknown");
    }

    public static String assetName(char tile) {
        return ASSETS.getOrDefault(tile, "grass");
    }

    public static Color color(char tile) {
        return COLORS.getOrDefault(tile, Color.MAGENTA);
    }

    public static boolean passable(char tile) {
        return PASSABLE.contains(tile);
    }

    public static boolean roadLike(char tile) {
        return ROAD_LIKE.contains(tile);
    }

    public static boolean connectingRoad(char tile) {
        return CONNECTING_ROAD.contains(tile);
    }

    public static boolean texturedRoad(char tile) {
        return tile == DIRT_ROAD || tile == VILLAGE_ROAD || tile == COBBLESTONE_ROAD || tile == 'q';
    }
}
