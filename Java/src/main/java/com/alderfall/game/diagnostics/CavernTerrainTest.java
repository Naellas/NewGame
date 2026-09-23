package com.alderfall.game;

import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

/** Guards material selection and camera/chunk seams without changing dungeon gameplay. */
public final class CavernTerrainTest {
    public static void main(String[] args) throws Exception {
        require(CavernStyle.biome("highwall", 'g').equals("ice"), "Northern cavern");
        require(CavernStyle.biome("northroad", 'm').equals("ice"), "Northern mine");
        require(CavernStyle.biome("crownlands", 'v').equals("moss"), "Swamp entrance");
        require(CavernStyle.biome("belltower", 'g').equals("moss"), "Fen cavern");
        require(CavernStyle.biome("sanctum", 'g').equals("sand"), "Sunrealm cavern");
        require(CavernStyle.biome("crownlands", 'm').equals("stone"), "Temperate bedrock");
        require(CavernStyle.biome("sanctum", 'n').equals("ice"), "Snow overrides political region");
        for (String biome : List.of("stone", "ice", "moss", "sand")) {
            for (String role : List.of("floor", "gravel", "face", "roof")) {
                var image = ImageIO.read(Path.of("assets/terrain/cavern_" + biome + "_" + role + ".png").toFile());
                require(image != null && image.getWidth() >= 256 && image.getHeight() >= 256, "Missing terrain master");
            }
            for (String detail : List.of("dungeon_detail_cave_mineral_cluster", "dungeon_detail_cave_stalagmites")) {
                String asset = CavernStyle.detail(biome, detail);
                try (var files = Files.walk(Path.of("assets"))) {
                    require(files.anyMatch(p -> p.getFileName().toString().equals(asset + ".png")), "Missing regional detail " + asset);
                }
            }
        }
        GameState state = new GameState(GameConfig.load(Path.of("").toAbsolutePath().normalize()));
        AssetStore assets = new AssetStore(Path.of("assets"));
        var painter = (WorldRenderer.TerrainPainter) Proxy.newProxyInstance(
                WorldRenderer.TerrainPainter.class.getClassLoader(), new Class<?>[]{WorldRenderer.TerrainPainter.class},
                (proxy, method, values) -> method.getName().equals("visibleTerrainTile") ? values[0] : null);
        WorldRenderer renderer = new WorldRenderer(assets, painter, null, null);
        int checked = 0;
        for (var marker : state.world.adventureMarkers()) {
            if (!CavernTerrainRenderer.supports(state.world.dungeonContext(marker.mapId()))) continue;
            for (int floor : new int[]{1,3}) {
            state.currentMapId = marker.mapId().substring(0,marker.mapId().lastIndexOf('_')+1)+floor;
            var area = state.world.area(state.currentMapId);
            char[][] before = Arrays.stream(area.tiles).map(char[]::clone).toArray(char[][]::new);
            for (int size : new int[]{24, 48, 72}) {
                var direct = render(renderer, state, false, 5, 4, size);
                compare(direct, render(renderer, state, true, 5, 4, size));
                var scroll = render(renderer, state, true, 6, 5, size);
                compare(direct.getSubimage(size, size, 16*size, 12*size), scroll.getSubimage(0, 0, 16*size, 12*size));
            }
            require(Arrays.deepEquals(before, area.tiles), "Renderer changed collision tiles");
            checked++;
            }
        }
        require(checked > 0, "No cavern sites checked");
        System.out.println("Cavern terrain passed: " + checked + " sites, three zooms, cache/scroll equivalence, materials and unchanged collision.");
    }

    private static BufferedImage render(WorldRenderer renderer, GameState state, boolean cached, int x, int y, int size) {
        var image = new BufferedImage(18*size, 14*size, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        var context = new WorldRenderer.TerrainContext(state, x, y, 18, 14,
                state.world.width(state.currentMapId), state.world.height(state.currentMapId), size, size*100/48, "dungeon");
        if (cached) renderer.drawCachedTerrainBase(g, context); else renderer.drawTerrainBase(g, context);
        g.dispose();
        return image;
    }

    private static void compare(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) for (int x = 0; x < a.getWidth(); x++)
            require(a.getRGB(x,y) == b.getRGB(x,y), "Camera/chunk seam at " + x + "," + y);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
