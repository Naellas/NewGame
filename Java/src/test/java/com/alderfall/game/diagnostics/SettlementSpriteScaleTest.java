package com.alderfall.game;

import com.alderfall.game.render.world.WorldPropRenderer;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Asset integration checks: alpha, scale hierarchy, zoom and road-facing lanterns. */
public final class SettlementSpriteScaleTest {
    public static void main(String[] args) throws Exception {
        var state = new GameState(GameConfig.load(Path.of("").toAbsolutePath()));
        var assets = new AssetStore(Path.of("assets"));
        var renderer = new WorldPropRenderer(assets, state, null);
        int count = 0;
        try (var files = Files.list(Path.of("assets/environments/settlements/city/props/refresh"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".png")).toList()) {
                String asset = file.getFileName().toString().replace(".png", "");
                var image = ImageIO.read(file.toFile());
                require(assets.hasSprite(asset), "Not indexed: " + asset);
                require(image.getColorModel().hasAlpha(), "Opaque cutout: " + asset);
                require((image.getRGB(0, 0) >>> 24) < 24, "Opaque corner: " + asset);
                int size = SettlementSpriteScale.size(asset);
                if (asset.contains("crate") || asset.contains("basket")) require(size <= 32, "Oversized cargo");
                if (asset.contains("lamp")) require(size >= 80 && size <= 100, "Lamp is not above head height");
                if (asset.contains("stall")) require(size >= 70 && size <= 90, "Stall canopy scale");
                for (int tile : new int[]{36, 48, 72}) {
                    int rendered = renderer.propRenderSize(asset, size, tile);
                    require(Math.abs(rendered / (double)tile - size / (double)GameConfig.TILE) < .03,
                            "Zoom changed physical scale: " + asset);
                }
                count++;
            }
        }
        for (String id : new String[]{"city_sanctum", "city_highwall", "town_briarbridge", "village_mireford"}) {
            var area = state.world.area(id);
            for (var prop : area.props) {
                if (prop.asset().equals("city_prop_refresh_lamp_right"))
                    require(Terrain.connectingRoad(area.tileAt(prop.x()+1, prop.y())), "Lamp points away from road");
                if (prop.asset().equals("city_prop_refresh_lamp_left"))
                    require(Terrain.connectingRoad(area.tileAt(prop.x()-1, prop.y())), "Lamp points away from road");
            }
        }
        require(count == 21, "Missing imported sprites");
        System.out.println("21 transparent sprites: scale hierarchy, three zooms, and road-facing lamps passed.");
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
