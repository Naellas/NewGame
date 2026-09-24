package com.alderfall.game.map;

import com.alderfall.game.*;
import java.util.*;

/** Reserves district blocks and public space before streets and regional dressing are generated. */
final class TownLayout {
    private TownLayout() { }
    record Plan(List<CityBuilding> buildings, int height, int promenade) { }

    static int district(CityBuilding b) {
        String key = b.key();
        if (key.contains("inner_west_row") || key.contains("inner_east_row")) return 0;
        if (key.contains("stacks") || key.contains("scriptorium") || key.contains("study")
                || key.contains("illumin") || key.contains("mage") || key.contains("reading")) return 1;
        return switch (b.style()) {
            case "house", "row" -> 2;
            case "blacksmith", "carpenter", "workshop", "fishing_hut", "warehouse" -> 3;
            case "inn", "shop", "alchemist", "apothecary", "market" -> 4;
            default -> 0;
        };
    }

    static Plan arrange(String mapId, List<CityBuilding> source) {
        List<List<CityBuilding>> groups = new ArrayList<>();
        for (int i = 0; i < 5; i++) groups.add(new ArrayList<>());
        for (CityBuilding b : source) groups.get(district(b)).add(b);
        groups.get(0).addAll(groups.get(1));
        groups.get(1).clear();
        int upperRows = Math.max(2, (Math.max(groups.get(0).size(), groups.get(2).size()) + 2) / 3);
        int promenade = 6 + upperRows * 7;
        int lowerRows = Math.max(2, (Math.max(groups.get(3).size(), groups.get(4).size()) + 2) / 3);
        List<CityBuilding> result = new ArrayList<>();
        for (int district = 0; district < 5; district++) {
            List<CityBuilding> group = groups.get(district);
            group.sort(Comparator.<CityBuilding>comparingInt(b -> TownBuildingArt.landmark(mapId, b) ? -1 : district(b))
                    .thenComparing(CityBuilding::style).thenComparing(CityBuilding::key));
            int columns = 3;
            int originX = district == 0 || district == 3 ? 5 : 34;
            int originY = district < 3 ? 6 : promenade + 10;
            for (int i = 0; i < group.size(); i++) {
                CityBuilding b = group.get(i);
                int x = originX + i % columns * 7;
                int y = originY + i / columns * 7;
                // Town art depicts one roughly four-tile building, even for inherited mansion lots.
                result.add(new CityBuilding(b.key(), x, y, x + (TownBuildingArt.landmark(mapId, b) ? 6 : Math.min(5, b.width())) - 1,
                        y + Math.min(3, b.depth()) - 1, b.style(), b.palette()));
            }
        }
        int height = promenade + 10 + lowerRows * 7 + 5 + 16;
        List<CityBuilding> padded = new ArrayList<>();
        int suburban = 0;
        int directionalHomes = 0;
        for (CityBuilding b : result) {
            int x = b.x1() + 8, y = b.y1() + 8;
            if (district(b) == 2 && suburban < 2) {
                x = 22 + suburban++ * 22;
                y = height - 7;
            }
            CityBuilding.Facing facing = CityBuilding.Facing.SOUTH;
            if (mapId.equals("town_northwatch")) facing = switch (b.key()) {
                case "east_lower_row_part_a" -> CityBuilding.Facing.EAST;
                case "east_lower_row_part_b" -> CityBuilding.Facing.WEST;
                case "west_lower_row_part_b" -> CityBuilding.Facing.NORTH;
                default -> CityBuilding.Facing.SOUTH;
            };
            else if (district(b) == 2 && y < height - 11 && directionalHomes < 3
                    && TownBuildingArt.asset(mapId, b).equals(mapId + "_house")) {
                facing = new CityBuilding.Facing[]{CityBuilding.Facing.EAST,
                        CityBuilding.Facing.WEST, CityBuilding.Facing.NORTH}[directionalHomes++];
            }
            int width = facing.dx != 0 ? Math.max(4, b.width()) : b.width();
            int depth = facing == CityBuilding.Facing.SOUTH ? b.depth() : Math.max(3, b.depth());
            padded.add(new CityBuilding(b.key(), x, y, x + width - 1, y + depth - 1,
                    b.style(), b.palette(), facing));
        }
        // Farm services fill the lower-quarter gap, near the south gate.
        // Its new key/anchor cannot change the retained interior IDs of existing homes.
        padded.add(new CityBuilding("town_service_barn", 34, height - 24, 38, height - 22, "warehouse", 0));
        // Infill the central residential gaps and southgate suburb without enlarging the town.
        // Stable keys give each dwelling its own ordinary, accessible house interior.
        for (int i = 0; i < 4; i++) {
            int x = i == 2 ? 12 : 34;
            int y = i < 2 ? 14 + i * 7 : height - 7;
            String style = mapId.equals("town_moonspire") && i == 0 ? "alchemist" : "house";
            padded.add(new CityBuilding("town_infill_home_" + i, x, y, x + 3, y + 2, style, i % 3));
        }
        return new Plan(padded, height, promenade);
    }

    static void mark(MapArea area, Plan plan) {
        String civic = switch (area.id) {
            case "town_ironvale", "town_northwatch" -> "Government District · Watch Court";
            case "town_embermarket" -> "Government District · Water Court";
            default -> "Government District · Civic Court";
        };
        area.landmarks.put(new TilePoint(12, 4), civic);
        area.landmarks.put(new TilePoint(42, 4), "Residential District");
        area.landmarks.put(new TilePoint(15, plan.promenade() + 8), "Craftsmen's District");
        area.landmarks.put(new TilePoint(42, plan.promenade() + 8), "Market District");
        area.landmarks.put(new TilePoint(29, plan.promenade() + 3), "Civic Plaza");
        area.landmarks.put(new TilePoint(11, plan.promenade() + 3), "Public Garden");
        area.landmarks.put(new TilePoint(47, plan.promenade() + 3), "Market Square");
        var marks = new HashMap<>(area.landmarks);
        area.landmarks.clear();
        marks.forEach((p, name) -> area.landmarks.put(new TilePoint(p.x() + 8, p.y() + 8), name));
        plan.buildings().stream().filter(b -> district(b) == 1).findFirst().ifPresent(b ->
                area.landmarks.put(new TilePoint(b.x1(), b.y1() - 2), "Scholars' District"));
        area.landmarks.put(new TilePoint(36, plan.height() - 5), "Southgate Suburbs");
    }

    static void finish(WorldMap world, MapArea area) {
        if (!area.id.startsWith("town_")) return;
        for (var entry : List.copyOf(area.landmarks.entrySet())) {
            String name = entry.getValue();
            if (!Set.of("Civic Plaza", "Public Garden", "Market Square").contains(name)) continue;
            TilePoint center = entry.getKey();
            boolean garden = name.equals("Public Garden");
            if (!garden) {
                area.props.removeIf(p -> Math.abs(p.x() - center.x()) <= 6 && Math.abs(p.y() - center.y()) <= 3
                        && p.asset().startsWith("deco_") && !p.asset().contains("harvestable")
                        && !area.landmarks.containsKey(new TilePoint(p.x(), p.y()))
                        && world.transitionAt(area.id, p.x(), p.y()) == null);
            }
            for (int y = center.y() - 3; y <= center.y() + 3; y++) {
                for (int x = center.x() - 6; x <= center.x() + 6; x++) {
                    // Furniture and the landmark itself should not leave holes in the paved court.
                    if (!garden && Terrain.passable(area.tileAt(x, y))
                            && !Terrain.connectingRoad(area.tileAt(x, y))
                            && world.cityBuildingAt(area.id, x, y) == null
                            && world.transitionAt(area.id, x, y) == null) area.tiles[y][x] = 'p';
                    if (!free(world, area, x, y)) continue;
                    boolean path = x == center.x() || y == center.y();
                    area.tiles[y][x] = garden && !path ? TownGardenArt.gardenGround(area.id) : 'p';
                    if (Math.abs(y - center.y()) == 3 && Math.abs(x - center.x()) == 4) {
                        String asset = garden ? TownGardenArt.gardenPlant(area.id)
                                : name.equals("Market Square") ? "city_prop_refresh_stall_canvas" : "city_prop_stone_bench";
                        area.addProp(new WorldProp(x, y, asset, garden ? 76 : 52));
                    } else if (garden && Math.abs(x - center.x()) == 3 && y == center.y() + 2) {
                        area.tiles[y][x] = 'p';
                        area.addProp(new WorldProp(x, y, "city_prop_stone_bench", 40));
                    }
                }
            }
        }
        for (CityBuilding b : world.cityBuildings(area.id)) {
            if (district(b) != 2 || b.facing() != CityBuilding.Facing.SOUTH) continue;
            Set<Integer> doors = new HashSet<>();
            for (TilePoint door : world.cityBuildingDoorTiles(b)) doors.add(door.x());
            for (int y = b.y2() + 2; y <= b.y2() + 3; y++) for (int x = b.x1(); x <= b.x2(); x++) {
                if (doors.contains(x) || !free(world, area, x, y)) continue;
                area.tiles[y][x] = TownGardenArt.gardenGround(area.id);
                if (y == b.y2() + 3 && (x == b.x1() || x == b.x2()))
                    area.addProp(new WorldProp(x, y, "city_prop_refresh_planter_pink", 34));
            }
        }
        TownExteriorRooms.furnish(world, area);
        TownStreets.finish(world, area);
    }

    private static boolean free(WorldMap world, MapArea area, int x, int y) {
        return x > 2 && y > 2 && x < area.width() - 3 && y < area.height() - 3
                && world.isPassable(area.id, x, y) && !Terrain.connectingRoad(area.tileAt(x, y))
                && world.cityBuildingAt(area.id, x, y) == null
                && world.transitionAt(area.id, x, y) == null && area.propAt(x, y) == null
                && area.districts.stream().noneMatch(d -> d.work().equals(new TilePoint(x, y))
                        || d.gathering().equals(new TilePoint(x, y)))
                && !area.landmarks.containsKey(new TilePoint(x, y));
    }
}
