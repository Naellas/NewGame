package com.alderfall.game;

import com.alderfall.game.map.MapArea;
import com.alderfall.game.map.WorldMap;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

/** Biome selection and the assets it actually returns, including dungeon theme precedence. */
public final class BattleSceneryTest {
    public static void main(String[] args) throws Exception {
        Set<String> selected = new HashSet<>();
        Set<String> caves = new HashSet<>();
        for (char biome : "gfsnvbPm".toCharArray()) {
            char[][] tiles = new char[11][11];
            for (char[] row : tiles) Arrays.fill(row, biome);
            MapArea area = new MapArea("biome", "Biome", "overworld", tiles);
            String ground = BattleScenery.outdoor(biome);
            selected.add(ground);
            String cave = BattleScenery.dungeon(context("cave", biome), "missing");
            caves.add(cave);
            selected.add(cave);
            for (char road : "rTK78B".toCharArray()) {
                area.setTile(5, 5, road);
                String roadside = BattleScenery.choose(area, 5, 5, road, "missing");
                selected.add(roadside);
                require(roadside.equals(ground) || (biome == 'g' && roadside.equals("battle_meadow_hills_backdrop"))
                        || (biome == 'f' && roadside.equals("battle_forest_edge_backdrop")), "Road biome lost: " + biome);
            }
            require(BattleScenery.dungeon(context("crypt", biome), "missing").equals("battle_dungeon_crypt_backdrop"), "Crypt theme must override biome");
            require(BattleScenery.dungeon(context("sewer", biome), "missing").equals("battle_dungeon_sewer_backdrop"), "Sewer theme must override biome");
            require(BattleScenery.dungeon(context("abandoned_castle", biome), "missing").equals("battle_dungeon_hall_backdrop"), "Castle theme must override biome");
        }
        require(caves.size() == 8, "All eight exterior biomes need distinct caverns");
        require(BattleScenery.dungeon(null, "fallback").equals("fallback"), "Unknown dungeon fallback");
        selected.add("battle_dungeon_hall_backdrop");
        selected.add("battle_dungeon_crypt_backdrop");
        selected.add("battle_dungeon_sewer_backdrop");
        AssetStore assets = new AssetStore(Path.of("assets"));
        for (String name : selected) {
            require(assets.hasSprite(name), "Missing registered asset: " + name);
            var image = ImageIO.read(Path.of("assets/battle", name + ".png").toFile());
            require(image != null && image.getWidth() >= 1200 && image.getHeight() >= 700, "Invalid environment: " + name);
            require(assets.cover(name, 1536, 1080).getWidth() == 1536, "Backdrop cover rendering failed");
        }
        System.out.println("Battle scenery checks passed: " + selected.size() + " reachable backgrounds, 8 distinct cavern biomes.");
    }

    private static WorldMap.DungeonContext context(String theme, char biome) {
        return new WorldMap.DungeonContext(theme, "test", "test", biome, "descending", 1, 3);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
