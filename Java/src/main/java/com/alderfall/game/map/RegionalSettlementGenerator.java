package com.alderfall.game.map;

import com.alderfall.game.*;
import com.alderfall.game.RegionalSettlementIdentity.District;
import com.alderfall.game.RegionalSettlementIdentity.Region;
import java.util.*;

/** Regional land use, hydrology and working commons, applied after fixed entrances are known. */
final class RegionalSettlementGenerator {
    private static final int[][] STEPS = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};

    static List<Npc> populate(WorldMap world, MapArea area) {
        Region region = RegionalSettlementIdentity.region(area.id);
        if (region == Region.NONE) return List.of();
        // Villages can still inherit an obsolete blocker. Authored city rings are never carved here.
        if (!"city".equals(area.kind)) {
            for (CityBuilding building : world.cityBuildings(area.id)) for (TilePoint door : world.cityBuildingDoorTiles(building)) {
                int x = door.x(), y = door.y() + 1;
                if (area.tileAt(x, y) == 'x' && world.cityBuildingAt(area.id, x, y) == null) area.tiles[y][x] = 'K';
            }
        }
        connectDoorApproaches(world, area);
        Set<TilePoint> before = reachable(world, area, entrance(world, area));
        if (before.isEmpty()) return List.of();
        boolean[][] protectedTiles = protectedTiles(world, area);
        List<TilePoint> centers = centers(world, area, before, protectedTiles);
        paintSurfaces(world, area, region);
        paintFunctionalDistricts(world, area, region, centers, protectedTiles);
        Set<TilePoint> water = new HashSet<>();
        if (!centers.isEmpty()) paintWater(area, region, centers.get(0), protectedTiles, water);
        restoreAccess(world, area, before, water);
        adaptVegetation(area, region);
        List<Npc> residents = new ArrayList<>();
        for (int index = 0; index < centers.size(); index++) {
            TilePoint center = centers.get(index);
            clearPlants(area, center, 2);
            char court = region == Region.FEN ? 'U' : region == Region.SUN ? 'V' : 'p';
            for (int[] step : STEPS) {
                int x = center.x() + step[0], y = center.y() + step[1];
                if (world.isPassable(area.id, x, y)
                        && !Terrain.connectingRoad(area.tileAt(x, y))) area.tiles[y][x] = court;
            }
            area.tiles[center.y()][center.x()] = court;
            connectCommon(world, area, center, region);
            String[] props = props(region, index);
            place(world, area, center.x(), center.y(), props[0], 56);
            place(world, area, center.x() - 2, center.y(), props[1], 40);
            place(world, area, center.x() + 2, center.y(), props[2], 40);
            TilePoint work = freeNear(world, area, center, 1);
            TilePoint gathering = index == 0 ? freeNear(world, area, center, 3) : area.districts.get(0).gathering();
            String townName = RegionalSettlementIdentity.townDistrictName(area.id, index);
            String name = townName == null ? RegionalSettlementIdentity.districtName(region, index) : townName;
            String keeper = RegionalSettlementIdentity.role(region) + " " + (index == 0 ? "Eren" : "Tala");
            District district = new District(name, center, work, gathering, keeper,
                    RegionalSettlementIdentity.dialogue(region, index));
            area.districts.add(district);
            protectedTiles[work.y()][work.x()] = true;
            protectedTiles[gathering.y()][gathering.x()] = true;
            area.landmarks.put(center, name);
            residents.add(new Npc(area.id, keeper, index == 0 ? "npc_citizen_man" : "npc_citizen_woman",
                    work.x(), work.y(), district.dialogue(), null, null));
            plantDistrict(world, area, region, center, protectedTiles);
        }
        clearRegionalRoofClutter(world, area);
        return residents;
    }

    private static void clearRegionalRoofClutter(WorldMap world, MapArea area) {
        for (CityBuilding building : world.cityBuildings(area.id)) {
            RegionalBuildingTypes.Type type = RegionalBuildingTypes.type(area.id, building);
            boolean village = "village".equals(area.kind);
            if (type == null && !village) continue;
            int rows = type == null ? 1 : switch (type) {
                case REMEMBRANCE_HALL, BELLHOUSE -> 3;
                case CARAVANSERAI, RESCUE_LODGE, GRANARY -> 2;
                default -> 1;
            };
            // Older decorative yards were composed for short cottages. Keep their props off taller roofs.
            area.props.removeIf(p -> p.x() >= building.x1() && p.x() <= building.x2()
                    && (village ? p.y() <= building.y2() : p.y() < building.y1()) && p.y() >= building.y1() - rows
                    && !area.landmarks.containsKey(new TilePoint(p.x(), p.y()))
                    && world.transitionAt(area.id, p.x(), p.y()) == null
                    && !p.asset().contains("fence") && !p.asset().contains("palisade")
                    && (plant(p) || village && p.asset().startsWith("deco_")
                    || p.asset().startsWith("city_prop_") || p.asset().startsWith("village_prop_")));
        }
    }

    private static void connectDoorApproaches(WorldMap world, MapArea area) {
        boolean mayCarveLegacyWall = !"city".equals(area.kind);
        Set<TilePoint> reached = reachable(world, area, entrance(world, area));
        for (CityBuilding building : world.cityBuildings(area.id)) {
            List<TilePoint> approaches = world.cityBuildingDoorTiles(building).stream()
                    .map(p -> new TilePoint(p.x(), p.y() + 1)).toList();
            if (approaches.stream().anyMatch(reached::contains)) continue;
            record Step(TilePoint point, int cost) { }
            PriorityQueue<Step> queue = new PriorityQueue<>(Comparator.comparingInt(Step::cost)
                    .thenComparingInt(s -> s.point().y()).thenComparingInt(s -> s.point().x()));
            Map<TilePoint, Integer> costs = new HashMap<>();
            Map<TilePoint, TilePoint> previous = new HashMap<>();
            for (TilePoint start : approaches) if (world.isPassable(area.id, start.x(), start.y())) {
                queue.add(new Step(start, 0)); costs.put(start, 0); previous.put(start, null);
            }
            TilePoint end = null;
            while (!queue.isEmpty()) {
                Step step = queue.remove();
                TilePoint p = step.point();
                if (step.cost() != costs.get(p)) continue;
                if (reached.contains(p)) { end = p; break; }
                for (int[] dir : STEPS) {
                    TilePoint n = new TilePoint(p.x() + dir[0], p.y() + dir[1]);
                    if (n.x() < 0 || n.y() < 0 || n.x() >= area.width() || n.y() >= area.height()) continue;
                    boolean wall = mayCarveLegacyWall && area.tileAt(n.x(), n.y()) == 'x'
                            && world.cityBuildingAt(area.id, n.x(), n.y()) == null && area.propAt(n.x(), n.y()) == null;
                    if (!wall && !world.isPassable(area.id, n.x(), n.y())) continue;
                    int cost = step.cost() + (wall ? 20 : 1);
                    if (cost >= costs.getOrDefault(n, Integer.MAX_VALUE)) continue;
                    costs.put(n, cost); previous.put(n, p); queue.add(new Step(n, cost));
                }
            }
            for (TilePoint p = end; p != null; p = previous.get(p)) {
                if (mayCarveLegacyWall && area.tileAt(p.x(), p.y()) == 'x') area.tiles[p.y()][p.x()] = 'K';
            }
            reached = reachable(world, area, entrance(world, area));
        }
    }

    private static void paintSurfaces(WorldMap world, MapArea area, Region region) {
        double phase = Math.floorMod(area.id.hashCode(), 31) / 5.0;
        boolean harbor = area.id.equals("town_greyharbor");
        boolean village = "village".equals(area.kind);
        for (int y = 1; y < area.height() - 1; y++) {
            for (int x = 1; x < area.width() - 1; x++) {
                char tile = area.tileAt(x, y);
                if (!Terrain.passable(tile) || "Buedt".indexOf(tile) >= 0
                        || world.cityBuildingAt(area.id, x, y) != null) continue;
                if (tile == Terrain.CITY_GATE) continue;
                if (Terrain.connectingRoad(tile) || tile == 'U') {
                    if (region == Region.FEN && tile != 'B') area.tiles[y][x] = Terrain.PLANK_ROAD;
                    else if (region == Region.SUN && (village || x != 16 && x != 17 && y != 11 && y != 12)) area.tiles[y][x] = Terrain.PACKED_ROAD;
                    continue;
                }
                double patch = Math.sin(x / 5.0 + phase) + Math.cos(y / 6.0 - phase)
                        + Math.sin((x + y) / 9.0 + phase) * 0.5;
                boolean front = world.cityBuildingAt(area.id, x, y - 1) != null;
                boolean civicCore = village ? Math.pow((x - 14) / 3.2, 2) + Math.pow((y - 10) / 2.4, 2) < 1
                        : Math.abs(x - 17) + Math.abs(y - 12) < 6;
                area.tiles[y][x] = switch (region) {
                    case FEN -> front || civicCore ? 'U' : 'v';
                    case SUN -> front || civicCore ? 'V' : patch > 1.6 ? 'b' : 's';
                    case NORTH -> front || civicCore ? 'p' : 'n';
                    case FREEHOLDS -> front || civicCore ? (harbor ? 'U' : 'p') : harbor ? 'g' : 'n';
                    case RIVER, HEARTH -> front || civicCore ? 'p' : 'g';
                    default -> tile;
                };
            }
        }
    }

    /** Irregular material fields make the two named quarters readable without fencing them into boxes. */
    private static void paintFunctionalDistricts(WorldMap world, MapArea area, Region region,
                                                 List<TilePoint> centers, boolean[][] protectedTiles) {
        if (!area.id.startsWith("town_")) return;
        for (int index = 0; index < centers.size(); index++) {
            TilePoint center = centers.get(index);
            char material = districtMaterial(region, index);
            for (int y = center.y() - 5; y <= center.y() + 5; y++) {
                for (int x = center.x() - 6; x <= center.x() + 6; x++) {
                    if (x <= 1 || y <= 1 || x >= area.width() - 2 || y >= area.height() - 2
                            || protectedTiles[y][x] || world.cityBuildingAt(area.id, x, y) != null) continue;
                    char tile = area.tileAt(x, y);
                    if (!Terrain.passable(tile) || Terrain.connectingRoad(tile) || tile == 'w' || tile == '~') continue;
                    double distance = Math.pow((x - center.x()) / 6.0, 2) + Math.pow((y - center.y()) / 5.0, 2);
                    int edge = Math.floorMod(x * 37 + y * 19 + area.id.hashCode() + index * 101, 100);
                    if (distance < 0.52 && edge < 58 || distance < 1.0 && edge < 26) {
                        area.tiles[y][x] = material;
                    }
                }
            }
        }
    }

    private static char districtMaterial(Region region, int index) {
        return switch (region) {
            case FEN -> index == 0 ? 'U' : 'y';
            case SUN -> index == 0 ? 'V' : 'b';
            case NORTH -> index == 0 ? 'p' : 'y';
            case FREEHOLDS -> index == 0 ? 'p' : 'y';
            case HEARTH -> index == 0 ? 'p' : 'y';
            case RIVER -> index == 0 ? 'a' : 'y';
            default -> index == 0 ? 'C' : 'p';
        };
    }

    private static void paintWater(MapArea area, Region region, TilePoint center,
                                   boolean[][] protectedTiles, Set<TilePoint> water) {
        boolean harbor = area.id.equals("town_greyharbor");
        double phase = Math.floorMod(area.id.hashCode(), 17) / 4.0;
        for (int y = 2; y < area.height() - 2; y++) {
            for (int x = 2; x < area.width() - 2; x++) {
                if (protectedTiles[y][x] || !Terrain.passable(area.tileAt(x, y))) continue;
                double dx = x - (center.x() + 3), dy = y - center.y();
                boolean wet = switch (region) {
                    case FEN -> dx * dx / 49.0 + dy * dy / 20.0 < 1.0
                            || Math.abs(x - (area.width() - 6 + Math.sin(y / 5.0 + phase) * 2)) < 1.5;
                    case SUN -> Math.pow(x - (center.x() - 3), 2) / 7.0 + Math.pow(y - (center.y() - 2), 2) / 3.0 < 1.0
                            || (x == center.x() - 3 && y >= center.y() - 2 && y <= center.y() + 4);
                    case RIVER -> Math.abs(y - (area.height() - 6 + Math.sin(x / 6.0 + phase) * 2)) < 1.4;
                    case FREEHOLDS -> harbor && x >= area.width() - 6 + Math.sin(y / 5.0 + phase);
                    default -> false;
                };
                if (!wet || Math.abs(x - center.x()) + Math.abs(y - center.y()) <= 2) continue;
                char tile = area.tileAt(x, y);
                if (tile == Terrain.CITY_GATE) continue;
                if (Terrain.connectingRoad(tile) || tile == 'U' || tile == 'V') {
                    area.tiles[y][x] = Terrain.connectingRoad(tile) ? Terrain.PLANK_ROAD : 'U';
                } else {
                    area.tiles[y][x] = 'w';
                    water.add(new TilePoint(x, y));
                }
            }
        }
        ensureRegionalWaterFootprint(area, region, center, protectedTiles, water, 5);
        area.props.removeIf(p -> water.contains(new TilePoint(p.x(), p.y())) && plant(p));
        if (region == Region.SUN) {
            for (int y = 2; y < area.height() - 2; y++) for (int x = 2; x < area.width() - 2; x++) {
                if (!protectedTiles[y][x] && area.tileAt(x, y) == 's' && nearWater(area, x, y, 2)) area.tiles[y][x] = 'g';
            }
        }
    }

    private static void ensureRegionalWaterFootprint(MapArea area, Region region, TilePoint center,
                                                     boolean[][] protectedTiles, Set<TilePoint> water,
                                                     int minimum) {
        if (region != Region.FEN && region != Region.SUN && region != Region.RIVER) return;
        int existing = 0;
        for (char[] row : area.tiles) for (char tile : row) if (tile == 'w') existing++;
        for (int radius = 2; existing < minimum && radius <= 9; radius++) {
            for (int y = center.y() - radius; y <= center.y() + radius && existing < minimum; y++) {
                for (int x = center.x() - radius; x <= center.x() + radius && existing < minimum; x++) {
                    if (x <= 1 || y <= 1 || x >= area.width() - 2 || y >= area.height() - 2
                            || Math.abs(x - center.x()) + Math.abs(y - center.y()) != radius
                            || protectedTiles[y][x]) continue;
                    char tile = area.tileAt(x, y);
                    if (!Terrain.passable(tile) || Terrain.connectingRoad(tile) || tile == 'U' || tile == 'V') continue;
                    area.tiles[y][x] = 'w';
                    water.add(new TilePoint(x, y));
                    existing++;
                }
            }
        }
    }

    private static boolean[][] protectedTiles(WorldMap world, MapArea area) {
        boolean[][] result = new boolean[area.height()][area.width()];
        for (int y = 0; y < area.height(); y++) for (int x = 0; x < area.width(); x++) {
            result[y][x] = x < 2 || y < 2 || x >= area.width() - 2 || y >= area.height() - 2
                    || world.cityBuildingAt(area.id, x, y) != null
                    || world.cityBuildingAt(area.id, x, y - 1) != null
                    || area.landmarks.containsKey(new TilePoint(x, y))
                    || world.transitionAt(area.id, x, y) != null
                    || area.propsAt(x, y).stream().anyMatch(p -> !plant(p));
        }
        List<Npc> npcs = new ArrayList<>(world.npcs(area.id));
        npcs.addAll(GameData.NPCS.stream().filter(n -> n.mapId().equals(area.id)).toList());
        for (Npc npc : npcs) for (int y = npc.y() - 1; y <= npc.y() + 1; y++) for (int x = npc.x() - 1; x <= npc.x() + 1; x++) {
            if (x >= 0 && y >= 0 && x < area.width() && y < area.height()) result[y][x] = true;
        }
        return result;
    }

    private static List<TilePoint> centers(WorldMap world, MapArea area, Set<TilePoint> accessible, boolean[][] protectedTiles) {
        List<TilePoint> result = new ArrayList<>();
        int salt = Math.floorMod(area.id.hashCode(), 7);
        for (int index = 0; index < 2; index++) {
            TilePoint best = null;
            int scoreBest = Integer.MIN_VALUE;
            TilePoint townAnchor = townDistrictAnchor(world, area, index);
            int preferredX = townAnchor == null
                    ? (index == 0 ? area.width() * (3 + salt % 3) / 8 : area.width() / 4)
                    : townAnchor.x();
            int preferredY = townAnchor == null
                    ? (index == 0 ? area.height() - 7 : area.height() / 2)
                    : townAnchor.y();
            for (int y = 4; y < area.height() - 4; y++) for (int x = 4; x < area.width() - 4; x++) {
                TilePoint p = new TilePoint(x, y);
                if (!accessible.contains(p) || protectedTiles[y][x] || Terrain.ROAD_LIKE.contains(area.tileAt(x, y))) continue;
                if (!result.isEmpty() && distance(p, result.get(0)) < 10) continue;
                boolean clear = true;
                int open = 0;
                for (int yy = y - 3; yy <= y + 3; yy++) for (int xx = x - 3; xx <= x + 3; xx++) {
                    if (!protectedTiles[yy][xx] && Terrain.passable(area.tileAt(xx, yy))) open++;
                    if (Math.abs(xx - x) <= 1 && Math.abs(yy - y) <= 1
                            && (protectedTiles[yy][xx] || !world.isPassable(area.id, xx, yy))) clear = false;
                }
                int score = open * 3 - Math.abs(x - preferredX) * 2 - Math.abs(y - preferredY) * 2;
                if (clear && score > scoreBest) { best = p; scoreBest = score; }
            }
            if (best != null) {
                result.add(best);
                for (int y = best.y() - 1; y <= best.y() + 1; y++) for (int x = best.x() - 1; x <= best.x() + 1; x++) protectedTiles[y][x] = true;
            }
        }
        return result;
    }

    private static TilePoint townDistrictAnchor(WorldMap world, MapArea area, int index) {
        RegionalSettlementIdentity.TownProfile profile = RegionalSettlementIdentity.townProfile(area.id);
        if (profile == null || profile.buildings().isEmpty()) return null;
        int specIndex = index == 0 ? 0 : profile.buildings().size() - 1;
        String key = profile.buildings().get(specIndex).key();
        CityBuilding anchor = world.cityBuildings(area.id).stream()
                .filter(building -> building.key().equals(key) || building.key().startsWith(key + "_part_"))
                .findFirst().orElse(null);
        if (anchor == null) return null;
        return new TilePoint(anchor.x1() + anchor.width() / 2,
                Math.min(area.height() - 5, anchor.y2() + 3));
    }

    private static void adaptVegetation(MapArea area, Region region) {
        for (int i = 0; i < area.props.size(); i++) {
            WorldProp p = area.props.get(i);
            if (!plant(p) || !p.asset().contains("tree") || p.asset().contains("harvestable")) continue;
            String replacement = switch (region) {
                case NORTH -> "deco_tree_pine";
                case FREEHOLDS -> area.id.equals("town_greyharbor") ? "deco_mountain_scrub_pine" : "deco_tree_pine";
                case SUN -> nearWater(area, p.x(), p.y(), 3) ? "deco_beach_palm" : "deco_dry_grass";
                case FEN -> "deco_reeds";
                default -> p.asset();
            };
            area.props.set(i, new WorldProp(p.x(), p.y(), replacement,
                    replacement.contains("grass") || replacement.contains("reeds") ? 40 : p.size()));
        }
    }

    private static void plantDistrict(WorldMap world, MapArea area, Region region, TilePoint center, boolean[][] protectedTiles) {
        for (int y = center.y() - 5; y <= center.y() + 5; y++) for (int x = center.x() - 6; x <= center.x() + 6; x++) {
            if (x < 3 || y < 3 || x >= area.width() - 3 || y >= area.height() - 3 || protectedTiles[y][x]
                    || Terrain.ROAD_LIKE.contains(area.tileAt(x, y))
                    || Math.abs(x - center.x()) + Math.abs(y - center.y()) < 3
                    || Math.floorMod(x * 17 + y * 31 + area.id.hashCode(), 9) != 0) continue;
            boolean water = nearWater(area, x, y, 2);
            String asset = switch (region) {
                case SUN -> water ? "deco_beach_palm" : "deco_dry_grass";
                case FEN -> water ? "deco_reeds" : "village_prop_seedling_tray";
                case NORTH, FREEHOLDS -> "deco_tree_pine";
                default -> "deco_tree_oak";
            };
            place(world, area, x, y, asset, asset.contains("tree") || asset.contains("palm") ? 70 : 36);
        }
    }

    private static void connectCommon(WorldMap world, MapArea area, TilePoint center, Region region) {
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        Map<TilePoint, TilePoint> previous = new HashMap<>();
        queue.add(center); previous.put(center, null);
        TilePoint end = null;
        while (!queue.isEmpty()) {
            TilePoint p = queue.removeFirst();
            char tile = area.tileAt(p.x(), p.y());
            if (distance(p, center) > 2 && (Terrain.connectingRoad(tile) || tile == 'U')) { end = p; break; }
            for (int[] step : STEPS) {
                TilePoint n = new TilePoint(p.x() + step[0], p.y() + step[1]);
                if (previous.containsKey(n) || !world.isPassable(area.id, n.x(), n.y())
                        || world.cityBuildingAt(area.id, n.x(), n.y()) != null
                        || area.propsAt(n.x(), n.y()).stream().anyMatch(prop -> !plant(prop))) continue;
                previous.put(n, p); queue.add(n);
            }
        }
        for (TilePoint p = end; p != null && !p.equals(center); p = previous.get(p)) {
            if (world.transitionAt(area.id, p.x(), p.y()) != null || Terrain.connectingRoad(area.tileAt(p.x(), p.y()))) continue;
            area.tiles[p.y()][p.x()] = region == Region.FEN ? 'U'
                    : region == Region.SUN ? 'V' : Terrain.DIRT_ROAD;
            TilePoint pathTile = p;
            area.props.removeIf(prop -> prop.x() == pathTile.x() && prop.y() == pathTile.y() && plant(prop));
        }
    }

    private static String[] props(Region region, int index) {
        return switch (region) {
            case HEARTH -> index == 0 ? new String[]{"village_prop_clay_oven", "village_prop_seedling_tray", "village_prop_farm_tools"}
                    : new String[]{"village_prop_beehive", "village_prop_produce_basket", "village_prop_bench"};
            case RIVER -> index == 0 ? new String[]{"deco_crossing_charter_marker", "city_prop_cart", "village_prop_produce_basket"}
                    : new String[]{"village_prop_beehive", "village_prop_produce_basket", "village_prop_bench"};
            case NORTH -> index == 0 ? new String[]{"village_prop_woodpile", "village_prop_grain_sacks", "village_prop_tool_rack"}
                    : new String[]{"deco_imagen_tundra_rune_stone", "deco_mountain_cairn", "village_prop_bench"};
            case SUN -> index == 0 ? new String[]{"village_prop_well", "village_prop_palm_shade", "village_prop_water_trough"}
                    : new String[]{"village_prop_water_trough", "village_prop_seedling_tray", "village_prop_farm_tools"};
            case FEN -> index == 0 ? new String[]{"deco_crossing_flood_bell", "village_prop_fish_rack", "village_prop_seedling_tray"}
                    : new String[]{"village_prop_seedling_tray", "village_prop_water_trough", "village_prop_farm_tools"};
            case FREEHOLDS -> index == 0 ? new String[]{"village_prop_woodpile", "location_camp_crates", "village_prop_tool_rack"}
                    : new String[]{"deco_mountain_cairn", "deco_imagen_tundra_rune_stone", "village_prop_bench"};
            default -> new String[]{"village_prop_bench", "village_prop_bench", "village_prop_bench"};
        };
    }

    private static void place(WorldMap world, MapArea area, int x, int y, String asset, int size) {
        if (!world.isPassable(area.id, x, y) || world.cityBuildingAt(area.id, x, y) != null
                || world.transitionAt(area.id, x, y) != null || Terrain.connectingRoad(area.tileAt(x, y))
                || area.propsAt(x, y).stream().anyMatch(p -> !plant(p))) return;
        area.props.removeIf(p -> p.x() == x && p.y() == y && plant(p));
        area.addProp(new WorldProp(x, y, asset, size));
    }

    private static TilePoint freeNear(WorldMap world, MapArea area, TilePoint p, int preferred) {
        for (int radius = preferred; radius <= preferred + 5; radius++) {
            for (int y = p.y() - radius; y <= p.y() + radius; y++) for (int x = p.x() - radius; x <= p.x() + radius; x++) {
                if (Math.abs(x - p.x()) + Math.abs(y - p.y()) == radius && world.isPassable(area.id, x, y)
                        && area.propAt(x, y) == null && world.transitionAt(area.id, x, y) == null) return new TilePoint(x, y);
            }
        }
        return p;
    }

    private static boolean plant(WorldProp p) {
        String a = p.asset();
        return !a.contains("harvestable") && (a.startsWith("deco_soft_") || a.startsWith("town_park_accent_")
                || a.startsWith("deco_") && (a.contains("grass") || a.contains("flower") || a.contains("tree")
                || a.contains("bush") || a.contains("pebble") || a.contains("clover") || a.contains("leaf") || a.contains("leaves")));
    }

    private static void clearPlants(MapArea area, TilePoint p, int radius) {
        area.props.removeIf(prop -> plant(prop) && Math.abs(prop.x() - p.x()) <= radius
                && prop.y() >= p.y() - radius - 1 && prop.y() <= p.y() + 1);
    }

    private static boolean nearWater(MapArea area, int x, int y, int radius) {
        for (int yy = y - radius; yy <= y + radius; yy++) for (int xx = x - radius; xx <= x + radius; xx++) {
            if (area.tileAt(xx, yy) == 'w') return true;
        }
        return false;
    }

    private static TilePoint entrance(WorldMap world, MapArea area) {
        for (int y = 0; y < area.height(); y++) for (int x = 0; x < area.width(); x++) {
            if (world.transitionAt(area.id, x, y) != null && world.isPassable(area.id, x, y)) return new TilePoint(x, y);
        }
        return null;
    }

    private static Set<TilePoint> reachable(WorldMap world, MapArea area, TilePoint start) {
        Set<TilePoint> result = new HashSet<>();
        if (start == null) return result;
        ArrayDeque<TilePoint> queue = new ArrayDeque<>(); queue.add(start); result.add(start);
        while (!queue.isEmpty()) {
            TilePoint p = queue.removeFirst();
            for (int[] s : STEPS) {
                TilePoint n = new TilePoint(p.x() + s[0], p.y() + s[1]);
                if (world.isPassable(area.id, n.x(), n.y()) && result.add(n)) queue.add(n);
            }
        }
        return result;
    }

    private static void restoreAccess(WorldMap world, MapArea area, Set<TilePoint> before, Set<TilePoint> water) {
        TilePoint start = entrance(world, area);
        // New channels may divide a yard. Restore only the crossings needed by the old reachable ground.
        for (int attempt = 0; attempt <= water.size(); attempt++) {
            Set<TilePoint> reached = reachable(world, area, start);
            TilePoint target = before.stream().filter(p -> world.isPassable(area.id, p.x(), p.y()) && !reached.contains(p))
                    .min(Comparator.comparingInt(TilePoint::y).thenComparingInt(TilePoint::x)).orElse(null);
            if (target == null) return;
            ArrayDeque<TilePoint> queue = new ArrayDeque<>();
            Map<TilePoint, TilePoint> previous = new HashMap<>();
            queue.add(target); previous.put(target, null);
            TilePoint end = null;
            while (!queue.isEmpty() && end == null) {
                TilePoint p = queue.removeFirst();
                for (int[] s : STEPS) {
                    TilePoint n = new TilePoint(p.x() + s[0], p.y() + s[1]);
                    if (previous.containsKey(n) || !before.contains(n)) continue;
                    if (!world.isPassable(area.id, n.x(), n.y()) && !water.contains(n)) continue;
                    previous.put(n, p); queue.add(n);
                    if (reached.contains(n)) { end = n; break; }
                }
            }
            if (end == null) throw new IllegalStateException("Regional channel cannot restore access: " + area.id);
            for (TilePoint p = end; p != null; p = previous.get(p)) {
                if (water.contains(p)) area.tiles[p.y()][p.x()] = Terrain.PLANK_ROAD;
            }
        }
        throw new IllegalStateException("Regional channel restoration did not converge: " + area.id);
    }

    private static int distance(TilePoint a, TilePoint b) { return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y()); }
}
