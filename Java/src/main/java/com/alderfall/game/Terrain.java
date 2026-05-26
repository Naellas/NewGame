package com.alderfall.game;

import java.awt.Color;
import java.util.Map;
import java.util.Set;

public final class Terrain {
    public static final Set<Character> PASSABLE = Set.of(
            'g', 'f', 's', 'n', 'v', 'b', 'q', 'r', 'c', 'u', 'd', 'p', 'j', 'l', 'a', 'y', 't', 'i', 'e', 'z'
    );

    private static final Map<Character, String> NAMES = Map.ofEntries(
            Map.entry('g', "Meadow"),
            Map.entry('f', "Oldwood"),
            Map.entry('s', "Sunsteppe"),
            Map.entry('n', "Frostfield"),
            Map.entry('v', "Marsh"),
            Map.entry('b', "Badlands"),
            Map.entry('m', "Mountain"),
            Map.entry('q', "Mountain Pass"),
            Map.entry('w', "Water"),
            Map.entry('r', "Road"),
            Map.entry('c', "City Gate"),
            Map.entry('u', "Village"),
            Map.entry('d', "Dungeon"),
            Map.entry('p', "Stone Plaza"),
            Map.entry('j', "Residential Court"),
            Map.entry('l', "Side Street"),
            Map.entry('a', "Market Row"),
            Map.entry('y', "Garden Court"),
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
            Map.entry('s', "desert"),
            Map.entry('n', "tundra"),
            Map.entry('v', "marsh"),
            Map.entry('b', "badlands"),
            Map.entry('m', "mountain_massif_tile"),
            Map.entry('q', "mountain_massif_tile"),
            Map.entry('w', "water"),
            Map.entry('r', "road"),
            Map.entry('c', "city"),
            Map.entry('u', "village"),
            Map.entry('d', "dungeon_floor"),
            Map.entry('p', "city_cobble"),
            Map.entry('j', "city_cobble"),
            Map.entry('l', "city_cobble"),
            Map.entry('a', "city_cobble"),
            Map.entry('y', "grass"),
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
            Map.entry('m', new Color(125, 133, 143)),
            Map.entry('q', new Color(119, 113, 104)),
            Map.entry('w', new Color(46, 135, 189)),
            Map.entry('r', new Color(217, 179, 111)),
            Map.entry('c', new Color(215, 191, 130)),
            Map.entry('u', new Color(185, 173, 118)),
            Map.entry('d', new Color(59, 51, 72)),
            Map.entry('p', new Color(176, 166, 145)),
            Map.entry('j', new Color(151, 145, 132)),
            Map.entry('l', new Color(125, 133, 128)),
            Map.entry('a', new Color(191, 143, 97)),
            Map.entry('y', new Color(118, 168, 100)),
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
}
