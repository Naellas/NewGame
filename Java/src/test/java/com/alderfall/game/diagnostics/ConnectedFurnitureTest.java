package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.Path;
import java.util.List;

public final class ConnectedFurnitureTest {
    public static void main(String[] args) {
        WorldMap world = new WorldMap(42);
        AssetStore assets = new AssetStore(Path.of("assets"));
        ConnectedFurniture.Sprites sprites = new ConnectedFurniture.Sprites();
        for (String asset : List.of("interior_storage_counter", "interior_shop_counter",
                "interior_bakery_counter", "interior_tavern_counter", "interior_tavern_bar",
                "interior_bookshelf", "interior_pantry_shelf", "interior_tools_shelf", "interior_supplies_shelf",
                "interior_joinery_storage", "interior_bakehouse_storage", "interior_apothecary_storage", "interior_archive_storage", "interior_smith_storage",
                "interior_joinery_worktop", "interior_bakehouse_worktop", "interior_apothecary_worktop", "interior_archive_worktop", "interior_smith_worktop",
                "interior_carpenter_workbench", "interior_carpenter_table", "interior_alchemy_station", "interior_cooking_station",
                "interior_study_desk_h", "interior_low_cupboard", "interior_crockery_cupboard")) {
            String id = world.createEditorMap("editor_connected", "Connected furniture", "interior", 20, 14);
            var area = world.area(id);
            for (int y = 1; y < 13; y++) for (int x = 1; x < 19; x++) area.tiles[y][x] = 'i';
            int width = WorldMap.interiorVisualFootprint(asset)[0];
            for (int i = 0; i < 3; i++) {
                require(world.addEditorInteriorProp(id, 3 + i * width, 5, asset, 48), "Placement " + asset);
            }
            WorldProp left = world.editorInteriorPropAt(id, 3, 5);
            WorldProp middle = world.editorInteriorPropAt(id, 3 + width, 5);
            WorldProp right = world.editorInteriorPropAt(id, 3 + width * 2, 5);
            require(ConnectedFurniture.connections(left, area.props) == 2, "Left cap");
            require(ConnectedFurniture.connections(middle, area.props) == 3, "Middle");
            require(ConnectedFurniture.connections(right, area.props) == 1, "Right cap");
            require(world.removeEditorInteriorProp(id, middle), "Remove middle");
            require(ConnectedFurniture.connections(left, area.props) == 0, "Restore left standalone");
            require(ConnectedFurniture.connections(right, area.props) == 0, "Do not bridge gap");
            require(world.isPassable(id, 3 + width, 5), "Clear removed collision");
            require(ConnectedFurniture.connections(left, List.of(left,
                    new WorldProp(3 + width, 6, asset, 48),
                    new WorldProp(3 + width, 5, "interior_stool", 48))) == 0, "Reject mismatched neighbors");
            for (int tile : new int[]{32, 48, 72}) for (int mask = 0; mask < 4; mask++) {
                var image = sprites.image(assets, asset, width * tile, tile * 2, mask);
                require(image.getWidth() == width * tile && image.getHeight() == tile * 2, "Sprite dimensions");
                require(image == sprites.image(assets, asset, width * tile, tile * 2, mask), "Cached variant");
                if (mask == 0) continue;
                var standalone = assets.spriteFit(asset, width * tile, tile * 2);
                // Adding neighbors must not change central detail size, shape or position.
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = image.getWidth() / 3; x < image.getWidth() * 2 / 3; x++) {
                        require(image.getRGB(x, y) == standalone.getRGB(x, y),
                                "Joining distorts the furniture body: " + asset + " mask=" + mask);
                    }
                }
                for (int edge : new int[]{0, image.getWidth() - 1}) {
                    if ((mask & (edge == 0 ? 1 : 2)) == 0) continue;
                    int ink = 0;
                    for (int y = 0; y < image.getHeight(); y++) if ((image.getRGB(edge, y) >>> 24) >= 16) ink++;
                    require(ink > tile / 4, "Connected edge remains empty: " + asset);
                }
            }
        }
        System.out.println("Connected furniture passed: 26 families, editor removal, collision, four variants and opaque joins at three scales.");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
