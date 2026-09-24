package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;

/** Generated boundary alpha, modular runs, circulation reservations and physical garden edges. */
public final class TownExteriorRoomsTest {
    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        AssetCatalog catalog = new AssetCatalog(Path.of("assets"));
        for (String asset : List.of("town_hedge_h", "town_hedge_v", "town_hedge_corner", "town_hedge_end",
                "town_fence_h", "town_fence_v", "town_fence_corner", "town_fence_gate")) {
            var image = ImageIO.read(catalog.findAsset(asset).toFile());
            require(image.getColorModel().hasAlpha(), "Missing alpha " + asset);
            require((image.getRGB(0, 0) >>> 24) < 8, "Opaque atlas gutter " + asset);
        }
        for (String asset : List.of("town_hedge_h", "town_fence_h", "city_prop_refresh_bench",
                "city_prop_refresh_planter_herbs", "city_prop_refresh_stall_canvas")) {
            var run = List.of(new WorldProp(5, 5, asset, 48), new WorldProp(6, 5, asset, 48), new WorldProp(7, 5, asset, 48));
            require(ConnectedFurniture.connections(run.get(0), run) == 2, "Missing left end " + asset);
            require(ConnectedFurniture.connections(run.get(1), run) == 3, "Broken middle " + asset);
            require(ConnectedFurniture.connections(run.get(2), run) == 1, "Missing right end " + asset);
            require(ConnectedFurniture.connections(run.get(0), List.of(run.get(0), run.get(2))) == 0, "Bridged gap " + asset);
            var sprites = new ConnectedFurniture.Sprites();
            for (int size : new int[]{24, 48, 72}) {
                var joined = sprites.image(assets, asset, size, size, 3);
                for (int x : new int[]{0, size - 1}) {
                    int ink = 0;
                    for (int y = 0; y < size; y++) if ((joined.getRGB(x, y) >>> 24) > 16) ink++;
                    require(ink >= size / 8, "Empty joined edge " + asset);
                }
            }
        }
        for (long seed : new long[]{0, 42, 2026}) {
            WorldMap world = new WorldMap(seed);
            int groveCount = 0;
            for (var site : world.settlementSites()) {
                if (!site.id().startsWith("town_")) continue;
                var area = world.area(site.id());
                boolean cold = Set.of("town_ironvale", "town_northwatch").contains(site.id());
                if (!cold) {
                    for (char[] row : area.tiles) for (char tile : row)
                        require(tile != 'n', "Snow terrain in temperate/warm town " + site.id());
                    require(area.props.stream().noneMatch(p -> p.asset().equals("town_fountain_north")
                            || p.asset().contains("frost") || p.asset().contains("snow")),
                            "Cold-climate dressing in " + site.id());
                }
                long planting = area.props.stream().filter(p -> p.asset().equals("deco_tree_fruit_harvestable")
                        || p.asset().equals("deco_tundra_frost_bush") || p.asset().equals("deco_marsh_sedge_clump")
                        || p.asset().equals("village_prop_seedling_tray")).count();
                require(planting >= 8, "Missing planted pockets " + site.id() + ": " + planting);
                for (WorldProp p : area.props) {
                    require(catalog.findAsset(p.asset()) != null, "Missing town artwork " + p.asset());
                    if (p.asset().startsWith("town_hedge_"))
                        require(ConnectedBoundary.connections(world, site.id(), p) != 0, "Isolated hedge " + site.id() + " " + p);
                }
                var plaza = area.landmarks.entrySet().stream().filter(e -> e.getValue().equals("Civic Plaza"))
                        .findFirst().orElseThrow().getKey();
                long plazaSeats = area.props.stream().filter(p -> p.asset().equals("city_prop_refresh_bench")
                        && Math.abs(p.x() - plaza.x()) <= 6 && Math.abs(p.y() - plaza.y()) <= 3).count();
                require(plazaSeats >= 2, "No seating faces the civic square " + site.id());
                for (WorldProp seat : area.props) {
                    if (!seat.asset().equals("city_prop_refresh_bench") || Math.abs(seat.x() - plaza.x()) > 6
                            || Math.abs(seat.y() - plaza.y()) > 3) continue;
                    for (int depth = 1; depth <= 2; depth++) {
                        int x = seat.x(), y = seat.y() + depth;
                        require(world.isPassable(site.id(), x, y) && area.propAt(x, y) == null,
                                "Blocked seating apron " + site.id() + " " + seat);
                        require(Terrain.ROAD_LIKE.contains(area.tileAt(x, y)), "Seat faces lawn " + seat);
                    }
                }
                for (var entry : area.landmarks.entrySet()) {
                    if (!entry.getValue().equals("Street Grove")) continue;
                    groveCount++;
                    var c = entry.getKey();
                    long trees = area.props.stream().filter(p -> p.asset().equals(TownGardenArt.gardenPlant(site.id()))
                            && Math.abs(p.x() - c.x()) <= 1 && Math.abs(p.y() - c.y()) <= 1).count();
                    require(trees == 2, "Incomplete tree patch " + site.id() + " " + c);
                    long hedges = area.props.stream().filter(p -> p.asset().startsWith("town_hedge_")
                            && Math.abs(p.x() - c.x()) <= 2 && Math.abs(p.y() - c.y()) <= 2).count();
                    require(hedges == 10, "Broken grove border " + site.id() + " " + c);
                }
                int boundaries = 0, joins = 0;
                Set<String> families = new HashSet<>();
                for (WorldProp p : area.props) {
                    if (ConnectedFurniture.connections(p, area.props) != 0) { joins++; families.add(p.asset()); }
                    if (!ConnectedFurniture.boundary(p.asset())) continue;
                    boundaries++;
                    require(TownGardenArt.gate(p.asset()) || !Terrain.connectingRoad(area.tileAt(p.x(), p.y())), "Boundary on a road " + site.id());
                    if (p.offsetX() == 0 && p.offsetY() == 0)
                        require(world.isGroundPassable(site.id(), p.x(), p.y()) == TownGardenArt.gate(p.asset()),
                                "Boundary/gateway collision " + p);
                    else {
                        var footprint = PropCollision.footprint(world, site.id(), p);
                        require(footprint != null && !PropCollision.clear(world, site.id(), footprint.getCenterX(), footprint.getCenterY()),
                                "Shifted boundary lost collision " + p);
                        for (int y = (int)Math.floor(footprint.y); y < Math.ceil(footprint.getMaxY()); y++)
                            for (int x = (int)Math.floor(footprint.x); x < Math.ceil(footprint.getMaxX()); x++)
                                require(!Terrain.connectingRoad(area.tileAt(x,y)), "Shifted hedge blocks a road " + p);
                    }
                }
                require(boundaries >= 12 && joins >= 12 && families.size() >= 3,
                        "Under-furnished " + site.id() + ": boundaries=" + boundaries + " joins=" + joins + " families=" + families);
                System.out.println(seed + " " + site.id() + ": " + boundaries + " boundary modules, " + joins + " joined pieces");
            }
            require(groveCount >= 6, "Road-grove rules placed no useful network: " + groveCount);
        }
        System.out.println("TownExteriorRoomsTest passed.");
    }
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
