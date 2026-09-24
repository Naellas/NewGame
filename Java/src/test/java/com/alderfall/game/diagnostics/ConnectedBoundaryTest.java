package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;

/** Every cardinal topology, orientation-independent joins, opening collision and fountain footprints. */
public final class ConnectedBoundaryTest {
    public static void main(String[] args) {
        WorldMap world = new WorldMap(42);
        String id = world.createEditorMap("editor_boundary", "Boundary test", "village", 24, 24);
        var area = world.area(id);
        for (char[] row : area.tiles) Arrays.fill(row, 'g');
        AssetStore assets = new AssetStore(Path.of("assets"));
        var sprites = new ConnectedBoundary.Sprites();
        int[][] directions = {{0,-1,1},{0,1,2},{-1,0,4},{1,0,8}};
        for (String family : List.of("town_hedge", "town_fence")) for (int mask = 0; mask < 16; mask++) {
            area.props.clear();
            WorldProp center = new WorldProp(10, 10, family + "_h", 48);
            area.addProp(center);
            for (int[] d : directions) if ((mask & d[2]) != 0)
                area.addProp(new WorldProp(10 + d[0], 10 + d[1], family + "_v", 48));
            require(ConnectedBoundary.connections(world, id, center) == mask, "Wrong topology " + family + " " + mask);
            for (int tile : new int[]{24,48,72}) {
                BufferedImage image = sprites.image(assets, family + "_h", tile, mask);
                require(image.getWidth() == tile && image.getHeight() == tile * 2, "Unstable module anchor");
                if ((mask & 4) != 0) require(edgeInk(image, 0) >= tile / 8, "West seam " + family);
                if ((mask & 8) != 0) require(edgeInk(image, tile - 1) >= tile / 8, "East seam " + family);
                if ((mask & 1) != 0) require(rowInk(image, tile) >= 2, "North seam " + family);
                if ((mask & 2) != 0) require(rowInk(image, tile * 2 - 1) >= 1, "South seam " + family);
            }
            if (mask != 0) {
                WorldProp removed = area.props.get(area.props.size() - 1);
                area.removeProp(removed);
                require(ConnectedBoundary.connections(world, id, center) != mask, "Stale connections after removal");
            }
        }
        for (String gate : List.of("town_fence_gate", "town_garden_gate_arbor", "town_garden_gate_iron", "town_garden_gate_hedge")) {
            area.props.clear();
            area.addProp(new WorldProp(10, 10, gate, 144));
            WorldProp left = new WorldProp(8, 10, "town_hedge_h", 48);
            area.addProp(left);
            require(ConnectedBoundary.connections(world, id, left) == 8, "Gate end does not meet boundary " + gate);
            for (int x = 9; x <= 11; x++) require(world.isPassable(id, x, 10), "Gate passage blocked " + gate);
        }
        for (String fountain : List.of("town_fountain_civic", "town_fountain_north", "town_fountain_sun")) {
            area.props.clear();
            area.addProp(new WorldProp(10, 10, fountain, 144));
            for (int y = 10; y <= 11; y++) for (int x = 10; x <= 11; x++)
                require(!world.isPassable(id, x, y), "Incomplete basin collision");
            require(world.isPassable(id, 9, 10) && world.isPassable(id, 12, 10), "Basin blocks its walkway");
        }
        for (var site : world.settlementSites()) if (site.id().startsWith("town_")) {
            long fountains = world.props(site.id()).stream().filter(p -> TownGardenArt.fountain(p.asset())).count();
            long gates = world.props(site.id()).stream().filter(p -> p.asset().startsWith("town_garden_gate_")).count();
            require(fountains >= 1 && gates >= 1, "Missing garden features " + site.id() + ": " + fountains + "/" + gates);
            System.out.println(site.id() + ": " + fountains + " fountains, " + gates + " garden entrances");
        }
        System.out.println("ConnectedBoundaryTest passed: all 16 masks, three scales, four gates and basin collision.");
    }
    private static int edgeInk(BufferedImage image, int x) {
        int count = 0; for (int y = 0; y < image.getHeight(); y++) if ((image.getRGB(x, y) >>> 24) > 16) count++; return count;
    }
    private static int rowInk(BufferedImage image, int y) {
        int count = 0; for (int x = 0; x < image.getWidth(); x++) if ((image.getRGB(x, y) >>> 24) > 16) count++; return count;
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
