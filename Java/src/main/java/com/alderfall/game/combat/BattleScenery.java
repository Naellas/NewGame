package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import com.alderfall.game.map.WorldMap;

/** Select once per encounter, without consuming the combat random stream. */
final class BattleScenery {
    static String dungeon(WorldMap.DungeonContext context, String fallback) {
        if (context == null) return fallback;
        return switch (context.theme()) {
            case "cave" -> cavern(context.exterior());
            case "crypt", "graveyard" -> "battle_dungeon_crypt_backdrop";
            case "sewer" -> "battle_dungeon_sewer_backdrop";
            default -> "battle_dungeon_hall_backdrop";
        };
    }

    static String cavern(char biome) {
        return switch (biome) {
            case 'f' -> "battle_cavern_forest_backdrop";
            case 's' -> "battle_cavern_desert_backdrop";
            case 'n' -> "battle_cavern_snow_backdrop";
            case 'v' -> "battle_cavern_marsh_backdrop";
            case 'b' -> "battle_cavern_badlands_backdrop";
            case 'P', '~', 'w' -> "battle_cavern_coast_backdrop";
            case 'm', 'q' -> "battle_cavern_mountain_backdrop";
            default -> "battle_dungeon_cavern_backdrop";
        };
    }

    static String outdoor(char biome) {
        return switch (biome) {
            case 'f' -> "battle_forest_backdrop";
            case 's' -> "battle_desert_backdrop";
            case 'b' -> "battle_badlands_backdrop";
            case 'n' -> "battle_snow_backdrop";
            case 'v', 'w' -> "battle_marsh_backdrop";
            case 'P', '~' -> "battle_beach_backdrop";
            case 'q', 'm' -> "battle_mountain_backdrop";
            case 'd' -> "battle_dungeon_cavern_backdrop";
            default -> "battle_plains_backdrop";
        };
    }

    static String choose(MapArea area, int x, int y, char terrain, String fallback) {
        if (area == null || !"overworld".equals(area.kind)) return fallback;
        if ("rTK78B".indexOf(terrain) >= 0) {
            terrain = roadsideBiome(area, x, y);
            fallback = outdoor(terrain);
        }
        boolean coast = terrain == 'P';
        boolean mountains = false;
        boolean forest = false;
        boolean meadow = false;
        for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
            if (Math.abs(dx) + Math.abs(dy) > 2) continue;
            int nx = x + dx, ny = y + dy;
            if (nx < 0 || ny < 0 || nx >= area.width() || ny >= area.height()) continue;
            char tile = area.tileAt(nx, ny);
            coast |= tile == 'P' || tile == '~';
            mountains |= tile == 'm' || tile == 'q';
            forest |= tile == 'f';
            meadow |= tile == 'g' || tile == 'A';
        }
        if (terrain == 'P' || terrain == '~') return "battle_beach_backdrop";
        if (coast && "gAr".indexOf(terrain) >= 0) return "battle_coastal_meadow_backdrop";
        if (coast && terrain == 's') return "battle_beach_backdrop";
        if (mountains && (terrain == 's' || terrain == 'b')) return "battle_desert_foothills_backdrop";
        if ((forest && "gAr".indexOf(terrain) >= 0) || (terrain == 'f' && meadow))
            return "battle_forest_edge_backdrop";
        // Broad location patches, stable across saves and repeated encounters.
        int variant = Math.floorMod((x / 5) * 31 + (y / 5) * 17 + area.id.hashCode(), 3);
        if (terrain == 'f' && variant == 1) return "battle_forest_edge_backdrop";
        if ("gAr".indexOf(terrain) >= 0 && variant != 0) return "battle_meadow_hills_backdrop";
        return fallback;
    }

    private static char roadsideBiome(MapArea area, int x, int y) {
        String biomes = "gfsnbvPmAq";
        int[] weights = new int[biomes.length()];
        for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) {
            int distance = Math.abs(dx) + Math.abs(dy);
            int nx = x + dx, ny = y + dy;
            if (distance > 3 || nx < 0 || ny < 0 || nx >= area.width() || ny >= area.height()) continue;
            int biome = biomes.indexOf(area.tileAt(nx, ny));
            if (biome >= 0) weights[biome] += 4 - distance;
        }
        int best = 0;
        for (int i = 1; i < weights.length; i++) if (weights[i] > weights[best]) best = i;
        return biomes.charAt(best);
    }
}
