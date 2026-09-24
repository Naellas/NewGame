package com.alderfall.game;

import com.alderfall.game.map.*;
import java.nio.file.Path;
import java.util.List;

/** Purpose finishes, equipment pads and joined wall-window regressions. */
public final class InteriorFlooringTest {
    public static void main(String[] args) {
        WorldMap world = new WorldMap(42);
        for (String theme : List.of("home", "blacksmith", "bakery", "alchemy")) {
            InteriorLayout plan = InteriorLayout.compose(theme, 1, InteriorStyle.HEARTHLANDS);
            String id = world.createEditorMap("editor_floor", theme, "interior", plan.tiles()[0].length, plan.tiles().length);
            MapArea area = world.area(id);
            for (int y = 0; y < area.height(); y++) area.tiles[y] = plan.tiles()[y].clone();
            InteriorFlooring.apply(area, plan);
            require(area.interiorFloorMaterials.containsValue("stone"), theme + " working floor");
            if (theme.equals("home")) {
                require(area.interiorFloorMaterials.containsValue("wood"), "Wooden bedroom");
                var p = area.interiorFloorMaterials.entrySet().stream().filter(e -> e.getValue().equals("wood")).findFirst().orElseThrow().getKey();
                require(InteriorFlooring.material(world, id, p.x(), p.y(), "stone").equals("timber"), "Bedroom overrides masonry shell");
            }
            var snapshot = java.util.Map.copyOf(area.interiorFloorMaterials);
            InteriorFlooring.apply(area, plan);
            require(snapshot.equals(area.interiorFloorMaterials), "Deterministic reconstruction");
        }
        String id = world.createEditorMap("editor_floor_equipment", "Equipment", "interior", 14, 12);
        MapArea area = world.area(id);
        for (char[] row : area.tiles) java.util.Arrays.fill(row, 'i');
        EditorTerrainRevisions revisions = new EditorTerrainRevisions();
        revisions.prepare(area, true);
        long before = revisions.revision(area, 5, 5, 1, 1, 0);
        WorldProp anvil = new WorldProp(5, 5, "interior_anvil", 48);
        area.props.add(anvil);
        require(InteriorFlooring.material(world, id, 5, 5, "timber").equals("stone"), "Placed anvil creates pad");
        revisions.prepare(area, true);
        require(revisions.revision(area, 5, 5, 1, 1, 0) != before, "Pad invalidates editor terrain");
        area.tiles[5][6] = 'o';
        require(InteriorFlooring.material(world, id, 7, 5, "timber").equals("timber"), "Pad stays behind wall");
        area.props.remove(anvil);
        require(InteriorFlooring.material(world, id, 5, 5, "timber").equals("timber"), "Removal restores floor");

        for (int x = 2; x < 10; x++) area.tiles[2][x] = 'o';
        area.interiorFloorMaterials.put(new TilePoint(3, 3), "stone");
        require(com.alderfall.game.render.world.ConnectedInteriorWalls.wallMaterial(world, id, 3, 2).equals("stone"), "Rear wall follows stone floor");
        require(com.alderfall.game.render.world.ConnectedInteriorWalls.wallMaterial(world, id, 4, 2).equals("timber"), "Adjacent rear wall retains timber");
        area.interiorFloorMaterials.clear();
        WorldProp stove = new WorldProp(3, 3, "interior_stove", 48);
        area.props.add(stove);
        require(com.alderfall.game.render.world.ConnectedInteriorWalls.wallMaterial(world, id, 3, 2).equals("stone"), "Equipment pad backs onto masonry");
        area.props.remove(stove);
        require(com.alderfall.game.render.world.ConnectedInteriorWalls.wallMaterial(world, id, 3, 2).equals("timber"), "Removing equipment restores rear wall");

        String window = "interior_wall_window_oak_segment";
        for (int x = 2; x < 10; x++) area.tiles[2][x] = 'o';
        for (int x = 3; x <= 5; x++) require(world.addEditorInteriorProp(id, x, 2, window, 48), "Window placement");
        var middle = world.editorInteriorPropAt(id, 4, 2);
        require(ConnectedFurniture.connections(middle, area.props) == 3, "Window bank joins both sides");
        require(ConnectedFurniture.connections(middle, List.of(new WorldProp(3, 2, window, 48, -1, 0, 2))) == 0, "Offset window does not join");
        require(world.removeEditorInteriorProp(id, middle), "Remove window segment");
        require(ConnectedFurniture.connections(world.editorInteriorPropAt(id, 3, 2), area.props) == 0, "Gap restores outer frame");
        AssetStore assets = new AssetStore(Path.of("assets"));
        for (int size : new int[]{24, 48, 73}) {
            var border = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var g = border.createGraphics();
            com.alderfall.game.render.world.ConnectedInteriorWalls.floorBorders(g, assets, world, id, 8, 8, 0, 0, size, "stone");
            g.dispose();
            require((border.getRGB(size / 2, size / 2) >>> 24) == 0, "Border leaves central flooring clear");
            for (int[] p : new int[][]{{size / 2, 0}, {size - 1, size / 2}, {size / 2, size - 1}, {0, size / 2}})
                require((border.getRGB(p[0], p[1]) >>> 24) > 0, "Border covers each material edge");
            area.interiorFloorMaterials.put(new TilePoint(9, 8), "stone");
            area.tiles[8][7] = 'o';
            border = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            g = border.createGraphics();
            com.alderfall.game.render.world.ConnectedInteriorWalls.floorBorders(g, assets, world, id, 8, 8, 0, 0, size, "stone");
            g.dispose();
            require((border.getRGB(size - 1, size / 2) >>> 24) == 0, "No border inside matching stone");
            require((border.getRGB(0, size / 2) >>> 24) == 0, "No floor border against wall");
            area.interiorFloorMaterials.clear();
            area.tiles[8][7] = 'i';
        }
        var sprites = new ConnectedFurniture.Sprites();
        for (int size : new int[]{32, 48, 72}) for (int mask = 1; mask < 4; mask++) {
            var image = sprites.image(assets, window, size, size * 3 / 2, mask);
            for (int edge : new int[]{0, size - 1}) if ((mask & (edge == 0 ? 1 : 2)) != 0) {
                int ink = 0;
                for (int y = 0; y < image.getHeight(); y++) if ((image.getRGB(edge, y) >>> 24) >= 16) ink++;
                require(ink > size / 2, "Joined window has continuous glazing/frame");
            }
        }
        System.out.println("Room floor purposes, equipment pads, terrain invalidation and connected wall windows passed.");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
