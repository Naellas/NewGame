package com.alderfall.game.map;

import com.alderfall.game.TilePoint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;

/** Checks generated dressing against real floor plans and the asset files. */
public final class ModularDungeonAssetsTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of("assets/deco/dungeon_details");
        Set<String> names = new HashSet<>();
        try (var paths = Files.list(root)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".png")).toList()) {
                var image = ImageIO.read(path.toFile());
                require(image != null && image.getWidth() == 128 && image.getHeight() == 128, "Invalid image: " + path);
                require(image.getColorModel().hasAlpha() && (image.getRGB(0, 0) >>> 24) == 0, "Missing alpha: " + path);
                names.add(path.getFileName().toString().replace(".png", ""));
            }
        }
        require(names.size() == 20, "Expected twenty modular sprites");
        int floorsChecked = 0;
        for (String theme : List.of("cave", "crypt", "abandoned_castle", "bandit_camp", "goblin_camp")) {
            int newProps = 0;
            for (int seed = 0; seed < 12; seed++) {
                var profile = DungeonGenerator.profile("asset-test-" + theme, "Asset test", theme, "western", 'g', seed);
                for (int floor = 1; floor <= 3; floor++) {
                    var plan = DungeonGenerator.generate(profile, floor, 3);
                    for (var prop : plan.props()) {
                        if (!prop.asset().startsWith("dungeon_detail_")) continue;
                        require(names.contains(prop.asset()), "Unresolved sprite: " + prop.asset());
                        require(prop.size() <= 48, "Oversized dressing: " + prop.asset());
                        require(!plan.criticalRoute().contains(new TilePoint(prop.x(), prop.y())), "Dressing obstructs route");
                        newProps++;
                    }
                    floorsChecked++;
                }
            }
            require(newProps > 0, "No new dressing generated for " + theme);
            var blueprint = OverworldLocationBlueprints.forKind(theme);
            for (var slot : blueprint.slots()) for (String name : slot.assets()) {
                if (name.startsWith("dungeon_detail_")) require(names.contains(name), "Unresolved exterior sprite: " + name);
            }
        }
        System.out.println("ModularDungeonAssetsTest passed: 20 sprites, " + floorsChecked + " floors, matching sizes and clear routes");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
