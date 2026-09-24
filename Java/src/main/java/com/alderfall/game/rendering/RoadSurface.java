package com.alderfall.game;

import com.alderfall.game.map.WorldMap;

/** Shared road materials; private material codes never change gameplay tiles. */
final class RoadSurface {
    private static final String GROUNDS = "gsnvb";
    private static final int BASE = 0xe000;
    private static final int PAVING_OFFSET = 64;
    private RoadSurface() { }

    static boolean isRoad(char tile) { return "rTK8Vq".indexOf(tile) >= 0; }

    static char groundAt(WorldMap world, String mapId, int x, int y) {
        int[] scores = new int[GROUNDS.length()];
        for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) {
            char tile = world.tileAt(mapId, x + dx, y + dy);
            char ground = switch (tile) {
                case 'f' -> 'g';
                case 'P' -> 's';
                case 'm', 'q' -> 'b';
                default -> tile;
            };
            int index = GROUNDS.indexOf(ground);
            if (index >= 0) scores[index] += 4 - Math.max(Math.abs(dx), Math.abs(dy));
        }
        int best = 0;
        for (int i = 1; i < scores.length; i++) if (scores[i] > scores[best]) best = i;
        return GROUNDS.charAt(best);
    }

    static char material(char tile, char ground) {
        int biome = Math.max(0, GROUNDS.indexOf(ground));
        return (char) (BASE + biome + (tile == 'K' && ground != 's' ? 16 : 0));
    }
    static char townMaterial(char tile, char ground) {
        return (char) (BASE + Math.max(0, GROUNDS.indexOf(ground)) + (tile == 'K' ? 16 : tile == 'T' ? 32 : 0));
    }
    static boolean worn(char material) { return material >= BASE + 32 && material < BASE + 48; }
    static boolean isMaterial(char material) { return material >= BASE && material < BASE + 48; }
    static char ground(char material) { return GROUNDS.charAt((material - BASE) % 16); }
    static boolean stone(char material) { return material >= BASE + 16; }
    static char paving(char roadMaterial) { return (char) (roadMaterial + PAVING_OFFSET); }
    static boolean isPaving(char material) { return material >= BASE + PAVING_OFFSET && material < BASE + PAVING_OFFSET + 48; }
    static char pavingGround(char material) { return ground((char) (material - PAVING_OFFSET)); }
}
