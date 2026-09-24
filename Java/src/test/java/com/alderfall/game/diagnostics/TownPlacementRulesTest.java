package com.alderfall.game.map;

import com.alderfall.game.TilePoint;
import com.alderfall.game.WorldProp;
import java.util.*;

/** Small counterexamples for placement intent, independent of the generated town seed. */
public final class TownPlacementRulesTest {
    public static void main(String[] args) {
        MapArea square = area('p');
        require(TownExteriorRooms.seatContext(square, 10, 10), "Open square rejected");
        square.tiles[10][8] = 'x';
        require(!TownExteriorRooms.seatContext(square, 10, 10), "Bench touches the wall silhouette");
        square.tiles[10][8] = 'p';
        square.tiles[12][11] = 'g';
        require(!TownExteriorRooms.seatContext(square, 10, 10), "Bench faces unpaved verge");
        square.tiles[12][11] = 'p';
        square.addProp(new WorldProp(11, 11, "village_prop_well", 48));
        require(!TownExteriorRooms.seatContext(square, 10, 10), "Bench faces an occupied apron");

        Set<TilePoint> border = new LinkedHashSet<>();
        for (int x = 0; x <= 7; x++) border.add(new TilePoint(x, 0));
        border.remove(new TilePoint(2, 0)); // A road interrupts the border, leaving a two-piece fragment.
        var stretches = TownExteriorRooms.hedgeStretches(border);
        require(stretches.size() == 1 && stretches.getFirst().size() == 5, "Fragment retained or road bridged");
        require(!stretches.getFirst().contains(new TilePoint(2, 0)), "Hedge closes an entrance");
        var corner = Set.of(new TilePoint(0, 0), new TilePoint(1, 0), new TilePoint(1, 1));
        require(TownExteriorRooms.hedgeStretches(corner).size() == 1, "Connected hedge corner lost");

        MapArea gap = area('g');
        for (int y = 9; y <= 15; y++) { gap.tiles[y][8] = 'K'; gap.tiles[y][14] = 'K'; }
        require(TownExteriorRooms.groveContext(gap, 10, 10, 3, 5), "Green strip between roads rejected");
        for (int y = 9; y <= 15; y++) gap.tiles[y][14] = 'g';
        require(!TownExteriorRooms.groveContext(gap, 10, 10, 3, 5), "Single roadside mistaken for a median");
        for (int y = 9; y <= 15; y++) gap.tiles[y][14] = 'K';
        gap.tiles[12][11] = 'K';
        require(!TownExteriorRooms.groveContext(gap, 10, 10, 3, 5), "Grove consumes a crossing");
        gap.tiles[12][11] = 'g';
        gap.landmarks.put(new TilePoint(11, 12), "Public Garden");
        require(!TownExteriorRooms.groveContext(gap, 10, 10, 3, 5), "Grove consumes a reserved garden");
        System.out.println("TownPlacementRulesTest passed: seating context, hedge continuity and road-bounded groves.");
    }

    private static MapArea area(char terrain) {
        char[][] tiles = new char[30][30];
        for (char[] row : tiles) Arrays.fill(row, terrain);
        return new MapArea("town_rules", "Placement rules", "town", tiles);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
