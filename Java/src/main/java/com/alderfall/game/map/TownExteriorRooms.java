package com.alderfall.game.map;

import com.alderfall.game.*;
import java.util.*;

/** Small outdoor rooms composed from touching modules, with open aisles rather than scattered props. */
final class TownExteriorRooms {
    private TownExteriorRooms() { }
    private record Piece(int x, int y, String asset) { }
    private record Yard(int x, int y, String[] shape) {
        int width() { return shape[0].length(); }
        int height() { return shape.length; }
    }
    private static final String[] L_COURT = {"######   ", "#....#   ", "#....####", "#.......#", "#.......#", "#.......#", "###ggg###"};
    private static final String[] STEPPED_COURT = {"  #####  ", "###...###", "#.......#", "#.......#", "#.......#", "###ggg###"};
    private static final String[] SMALL_COURT = {"#######", "#.....#", "#.....#", "#.....#", "##ggg##"};
    private static final String[] COMPACT_COURT = {"#####", "#...#", "#...#", "#...#", "#ggg#"};
    private static final String BENCH = "city_prop_refresh_bench";
    private static final String PLANTER = "city_prop_refresh_planter_herbs";
    private static final String STALL = "city_prop_refresh_stall_canvas";

    static void furnish(WorldMap world, MapArea area) {
        for (var landmark : List.copyOf(area.landmarks.entrySet())) {
            TilePoint c = landmark.getKey();
            if (landmark.getValue().equals("Market Square")) {
                removeCornerFurniture(area, c, STALL);
                for (int dy : new int[]{-3, 3}) for (int dx : new int[]{-5, 3}) {
                    run(world, area, c.x() + dx, c.y() + dy, 3, STALL);
                    int back = dy < 0 ? -2 : 2;
                    group(world, area, List.of(new Piece(c.x() + dx, c.y() + back, "city_prop_refresh_crate_closed"),
                            new Piece(c.x() + dx + 1, c.y() + back, TownGardenArt.localGoods(area.id)[0])));
                }
            } else if (landmark.getValue().equals("Public Garden")) {
                area.props.removeIf(p -> Math.abs(p.x() - c.x()) <= 6 && Math.abs(p.y() - c.y()) <= 3
                        && (p.asset().startsWith("deco_") && !p.asset().contains("harvestable")
                        || p.asset().equals("city_prop_stone_bench"))
                        && !area.landmarks.containsKey(new TilePoint(p.x(), p.y()))
                        && world.transitionAt(area.id, p.x(), p.y()) == null);
                int[][] vertices = {{-4,-3},{4,-3},{4,-2},{6,-2},{6,2},{4,2},{4,3},{-4,3},{-4,2},{-6,2},{-6,-2},{-4,-2}};
                List<TilePoint> border = new ArrayList<>();
                for (TilePoint p : outline(vertices)) {
                    if (Math.abs(p.y()) == 3 && Math.abs(p.x()) <= 1 || Math.abs(p.x()) == 6 && Math.abs(p.y()) <= 1) continue;
                    border.add(new TilePoint(c.x() + p.x(), c.y() + p.y()));
                }
                hedgeBorder(world, area, border);
                for (int dy : new int[]{-3, 3}) gate(world, area, c.x(), c.y() + dy, TownGardenArt.gardenGate(area.id));
                for (int dx : new int[]{-4, 3}) run(world, area, c.x() + dx, c.y() - 1, 2, BENCH);
                for (int dx : new int[]{-3, 3}) for (int dy : new int[]{-2, 2}) {
                    WorldProp tree = new WorldProp(c.x() + dx, c.y() + dy,
                            TownGardenArt.gardenPlant(area.id), 100, -1, dx < 0 ? 12 : -12, -12);
                    if (propFits(world, area, tree, false)) area.addProp(tree);
                }
            } else if (landmark.getValue().equals("Civic Plaza")) {
                removeCornerFurniture(area, c, "city_prop_stone_bench");
                for (int side : new int[]{-1, 1}) {
                    boolean seated = false;
                    for (int dy = -3; dy <= -1 && !seated; dy++)
                        for (int offset = 4; offset >= 2 && !seated; offset--)
                            seated = squareSeat(world, area, c.x() + (side < 0 ? -offset - 1 : offset), c.y() + dy);
                }
                fountain(world, area, c);
            }
        }
        landmarkGrounds(world, area);
        List<Yard> yards = new ArrayList<>();
        int[] districtYards = new int[5];
        List<CityBuilding> owners = new ArrayList<>(world.cityBuildings(area.id));
        owners.sort(Comparator.comparingInt(b -> b.key().equals("town_service_barn") ? 0 : specialized(b) ? 1 : 2));
        for (CityBuilding b : owners) {
            if (TownLayout.district(b) == 2) gardenFront(world, area, b);
            else {
                int district = TownLayout.district(b);
                if (!specialized(b) && districtYards[district] >= (district == 3 ? 3 : 2)) continue;
                Yard yard = findYard(world, area, b, yards);
                if (yard == null) continue;
                yards.add(yard);
                districtYards[district]++;
                furnishYard(world, area, yard, district, b);
            }
        }
        roadGroves(world, area);
        plantUnusedLots(world, area);
        // A clipped boundary must not leave an isolated hedge in an otherwise open lot.
        Set<WorldProp> isolated = new HashSet<>();
        for (WorldProp p : area.props)
            if (p.asset().startsWith("town_hedge_") && ConnectedBoundary.connections(world, area.id, p) == 0)
                isolated.add(p);
        area.props.removeIf(isolated::contains);
        area.markVisualChange();
    }

    private static boolean specialized(CityBuilding b) {
        return b.key().equals("town_service_barn") || Set.of("blacksmith", "alchemist", "apothecary").contains(b.style());
    }

    private static void landmarkGrounds(WorldMap world, MapArea area) {
        for (CityBuilding b : world.cityBuildings(area.id)) {
            if (!TownBuildingArt.landmark(area.id, b)) continue;
            TilePoint door = world.cityBuildingDoorTiles(b).getFirst();
            char paving = area.id.equals("town_reedwatch") || area.id.equals("town_greyharbor") ? 'U'
                    : area.id.equals("town_embermarket") ? 'V' : 'p';
            for (int y = b.y2() + 1; y <= b.y2() + 4; y++) for (int x = b.x1() - 1; x <= b.x2() + 1; x++) {
                if (world.cityBuildingAt(area.id, x, y) == null && world.isPassable(area.id, x, y)
                        && !Terrain.connectingRoad(area.tileAt(x, y)) && world.transitionAt(area.id, x, y) == null)
                    area.tiles[y][x] = paving;
            }
            String label = switch (area.id) {
                case "town_briarbridge" -> "Charter Hall Terrace";
                case "town_ironvale" -> "Forge Keep Muster Court";
                case "town_moonspire" -> "Survey Tower Botanical Court";
                case "town_reedwatch" -> "Listening Tower Water Garden";
                case "town_embermarket" -> "Sun Court Reflecting Garden";
                case "town_northwatch" -> "Signal Tower Rescue Court";
                default -> "Storm Beacon Mariners' Walk";
            };
            area.landmarks.put(b.outside(door, 0, 2), label);
            // Compose each wing as a complete planted seat, preserving the central approach.
            for (int side : new int[]{-1, 1}) {
                boolean placed = false;
                for (int depth = 2; depth <= 3 && !placed; depth++) {
                    for (int offset = 2; offset <= 5 && !placed; offset++) {
                        int x = side < 0 ? door.x() - offset - 1 : door.x() + offset;
                        placed = squareSeat(world, area, x, b.y2() + depth);
                    }
                }
            }

        }
    }

    /** South-facing bench art must face an open paved court, never a wall or a grass scrap. */
    private static boolean squareSeat(WorldMap world, MapArea area, int x, int y) {
        if (!seatContext(area, x, y)) return false;
        for (int dx = 0; dx < 2; dx++) {
            if (!free(world, area, x + dx, y)) return false;
            for (int dy = 1; dy <= 2; dy++)
                if (!world.isPassable(area.id, x + dx, y + dy)
                        || world.cityBuildingAt(area.id, x + dx, y + dy) != null
                        || world.transitionAt(area.id, x + dx, y + dy) != null) return false;
        }
        // Sit slightly back from the walking apron, like an editor-authored placement.
        for (int dx = 0; dx < 2; dx++) area.addProp(new WorldProp(x + dx, y, BENCH, 48, -1, 0, -12));
        return true;
    }

    static boolean seatContext(MapArea area, int x, int y) {
        if (!wallClear(area, x, y, 2, 3)) return false;
        for (int dx = 0; dx < 2; dx++) {
            if (!paving(area.tileAt(x + dx, y)) || Terrain.connectingRoad(area.tileAt(x + dx, y))) return false;
            for (int dy = 1; dy <= 2; dy++)
                if (!paving(area.tileAt(x + dx, y + dy)) || area.propAt(x + dx, y + dy) != null) return false;
        }
        return true;
    }

    private static boolean paving(char tile) {
        return Terrain.connectingRoad(tile) || "pjalCGUV".indexOf(tile) >= 0;
    }

    /** Leave two ground tiles around walls: their rendered towers overhang their collision tile. */
    static boolean wallClear(MapArea area, int x, int y, int width, int height) {
        for (int py = y - 2; py < y + height + 2; py++)
            for (int px = x - 2; px < x + width + 2; px++)
                if ("xcht".indexOf(area.tileAt(px, py)) >= 0) return false;
        return true;
    }

    /** Keep only connected border stretches of at least three modules; gaps remain real entrances. */
    private static void hedgeBorder(WorldMap world, MapArea area, List<TilePoint> planned) {
        Set<TilePoint> available = new LinkedHashSet<>();
        for (TilePoint p : planned) if (free(world, area, p.x(), p.y())) available.add(p);
        for (List<TilePoint> stretch : hedgeStretches(available))
            group(world, area, stretch.stream().map(p -> new Piece(p.x(), p.y(), "town_hedge_h")).toList());
    }

    static List<List<TilePoint>> hedgeStretches(Set<TilePoint> available) {
        Set<TilePoint> remaining = new LinkedHashSet<>(available);
        List<List<TilePoint>> result = new ArrayList<>();
        while (!remaining.isEmpty()) {
            List<TilePoint> stretch = new ArrayList<>();
            ArrayDeque<TilePoint> queue = new ArrayDeque<>();
            TilePoint start = remaining.iterator().next(); remaining.remove(start); queue.add(start);
            while (!queue.isEmpty()) {
                TilePoint p = queue.removeFirst(); stretch.add(p);
                for (int[] d : new int[][]{{0,-1},{1,0},{0,1},{-1,0}}) {
                    TilePoint n = new TilePoint(p.x() + d[0], p.y() + d[1]);
                    if (remaining.remove(n)) queue.add(n);
                }
            }
            if (stretch.size() >= 3) result.add(stretch);
        }
        return result;
    }

    /** Long planted strips follow roads on two sides, rather than filling arbitrary unused tiles. */
    private static void roadGroves(WorldMap world, MapArea area) {
        List<TilePoint> planted = new ArrayList<>();
        for (int[] size : new int[][]{{3,5},{5,3}})
            for (int y = 13; y < area.height() - 12 - size[1]; y++)
                for (int x = 13; x < area.width() - 12 - size[0]; x++) {
                    if (planted.size() >= 6) return;
                    int width = size[0], height = size[1];
                    TilePoint c = new TilePoint(x + width / 2, y + height / 2);
                    if (planted.stream().anyMatch(p -> Math.abs(p.x()-c.x()) + Math.abs(p.y()-c.y()) < 10)
                            || !groveContext(area, x, y, width, height)) continue;
                    boolean clear = true;
                    // A walkable rim prevents the hedges from becoming a barrier across a gap.
                    for (int py = y - 1; py <= y + height && clear; py++)
                        for (int px = x - 1; px <= x + width; px++) {
                            boolean inside = px >= x && px < x + width && py >= y && py < y + height;
                            if (inside ? !free(world, area, px, py, true)
                                    : !world.isPassable(area.id, px, py) || area.propAt(px, py) != null
                                    || world.cityBuildingAt(area.id, px, py) != null) { clear = false; break; }
                        }
                    if (!clear) continue;
                    List<WorldProp> composition = new ArrayList<>();
                    // Each complete rail shares one offset, preserving its joined seams.
                    for (int i = 0; i < Math.max(width, height); i++) for (int side : new int[]{0,2}) {
                        int px = width == 3 ? x + side : x + i;
                        int py = width == 3 ? y + i : y + side;
                        int outward = side == 0 ? -12 : 12;
                        composition.add(new WorldProp(px, py, "town_hedge_h", 48, -1,
                                width == 3 ? outward : 0, width == 3 ? 0 : outward));
                    }
                    for (int i : new int[]{1,3}) {
                        int px = width == 3 ? x + 1 : x + i;
                        int py = width == 3 ? y + i : y + 1;
                        composition.add(new WorldProp(px, py, TownGardenArt.gardenPlant(area.id), 110, -1, 0, -12));
                    }
                    if (composition.stream().anyMatch(p -> !propFits(world, area, p, true))) continue;
                    final int left = x, top = y;
                    area.props.removeIf(p -> p.x() >= left && p.x() < left + width
                            && p.y() >= top && p.y() < top + height && replaceablePlant(p));
                    composition.forEach(area::addProp);
                    for (int py = y; py < y + height; py++) for (int px = x; px < x + width; px++)
                        area.tiles[py][px] = TownGardenArt.gardenGround(area.id);
                    area.landmarks.put(c, "Street Grove");
                    planted.add(c);
                }
    }

    static boolean groveContext(MapArea area, int x, int y, int width, int height) {
        if (!wallClear(area, x, y, width, height)) return false;
        if (area.landmarks.entrySet().stream().anyMatch(e ->
                Set.of("Public Garden", "Civic Plaza", "Market Square").contains(e.getValue())
                && x <= e.getKey().x() + 7 && x + width > e.getKey().x() - 7
                && y <= e.getKey().y() + 4 && y + height > e.getKey().y() - 4)) return false;
        for (int py = y; py < y + height; py++) for (int px = x; px < x + width; px++)
            if ("gfnsvby".indexOf(area.tileAt(px, py)) < 0) return false;
        // Opposite paved edges must each have a continuous three-tile frontage.
        return roadEdge(area, x, y, width, height, -1, 0) && roadEdge(area, x, y, width, height, 1, 0)
                || roadEdge(area, x, y, width, height, 0, -1) && roadEdge(area, x, y, width, height, 0, 1);
    }

    private static boolean roadEdge(MapArea area, int x, int y, int width, int height, int dx, int dy) {
        for (int distance = 1; distance <= 3; distance++) {
            int run = 0;
            for (int i = 0; i < (dx == 0 ? width : height); i++) {
                int px = dx == 0 ? x + i : dx < 0 ? x - distance : x + width - 1 + distance;
                int py = dy == 0 ? y + i : dy < 0 ? y - distance : y + height - 1 + distance;
                if (paving(area.tileAt(px, py))) { if (++run >= 3) return true; }
                else run = 0;
            }
        }
        return false;
    }

    /** Validate the shifted solid footprint, not just the prop's integer ownership tile. */
    private static boolean propFits(WorldMap world, MapArea area, WorldProp prop, boolean replant) {
        if (!free(world, area, prop.x(), prop.y(), replant)) return false;
        var footprint = PropCollision.footprint(world, area.id, prop);
        if (footprint == null) return true;
        for (int y = (int)Math.floor(footprint.y); y < Math.ceil(footprint.getMaxY()); y++)
            for (int x = (int)Math.floor(footprint.x); x < Math.ceil(footprint.getMaxX()); x++)
                if (!free(world, area, x, y, replant)) return false;
        return area.propsInBounds(prop.x()-3, prop.y()-3, prop.x()+4, prop.y()+4).stream().noneMatch(p -> {
            if (replant && replaceablePlant(p)) return false;
            var other = PropCollision.footprint(world, area.id, p);
            return other != null && other.intersects(footprint);
        });
    }

    private static boolean replaceablePlant(WorldProp p) {
        return p.asset().startsWith("deco_") && !p.asset().contains("harvestable")
                && (p.asset().contains("tree") || p.asset().contains("grass") || p.asset().contains("bush"));
    }

    /** Allocate road-shaped parcels before choosing any of their furnishings. */
    private static void plantUnusedLots(WorldMap world, MapArea area) {
        Set<TilePoint> land = new LinkedHashSet<>(), streetEdges = new HashSet<>();
        List<CityBuilding> buildings = world.cityBuildings(area.id);
        for (int y = 12; y < area.height() - 4; y++) for (int x = 12; x < area.width() - 12; x++) {
            if ("gfnsvby".indexOf(area.tileAt(x,y)) < 0 || !wallClear(area,x,y,1,1)
                    || !free(world,area,x,y,true)) continue;
            TilePoint p = new TilePoint(x,y);
            if (area.landmarks.entrySet().stream().anyMatch(e ->
                    Set.of("Public Garden", "Civic Plaza", "Market Square").contains(e.getValue())
                    && Math.abs(e.getKey().x()-p.x()) <= 8 && Math.abs(e.getKey().y()-p.y()) <= 5)) continue;
            if (buildings.stream().noneMatch(owner -> Math.abs(p.x()-(owner.x1()+owner.x2())/2)
                    + Math.abs(p.y()-owner.y2()) <= 12)) continue;
            land.add(p);
            for (int[] d : TownLandPlots.STEPS) {
                int px=x+d[0], py=y+d[1];
                if (paving(area.tileAt(px,py)) && world.isPassable(area.id,px,py)
                        && area.propAt(px,py)==null && world.transitionAt(area.id,px,py)==null) streetEdges.add(p);
            }
        }
        List<TownLandPlots.Plot> plots = TownLandPlots.plan(land, buildings, streetEdges);
        area.setTownPlots(plots);
        for (TownLandPlots.Plot plot : plots) furnishPlot(world,area,plot);
    }

    private static void furnishPlot(WorldMap world, MapArea area, TownLandPlots.Plot plot) {
        boolean trade = plot.use() == TownLandPlots.Use.TRADE_YARD;
        boolean herbs = plot.use() == TownLandPlots.Use.HERB_GARDEN;
        boolean cold = TownGardenArt.gardenGround(area.id) == 'n';
        Set<TilePoint> aisle = new HashSet<>(plot.path());
        area.props.removeIf(p -> plot.cells().contains(new TilePoint(p.x(),p.y())) && replaceablePlant(p));
        for (TilePoint p : plot.cells()) area.tiles[p.y()][p.x()] = trade || aisle.contains(p)
                ? TownGardenArt.courtGround(area.id) : TownGardenArt.gardenGround(area.id);
        // Place a single contour on half-tile vertices, inset from roads and neighboring parcels.
        Set<TilePoint> boundary = new LinkedHashSet<>();
        String edgeAsset = trade ? "town_fence_h" : "town_hedge_h";
        for (TilePoint p : TownLandPlots.borderVertices(plot.cells())) {
            if (plot.path().stream().anyMatch(q -> Math.abs(p.x()-q.x())+Math.abs(p.y()-q.y()) <= 2)) continue;
            WorldProp edge = new WorldProp(p.x(),p.y(),edgeAsset,48,-1,-24,-24);
            if (propFits(world,area,edge,false)) boundary.add(p);
        }
        for (List<TilePoint> stretch : hedgeStretches(boundary))
            for (TilePoint p : stretch) area.addProp(new WorldProp(p.x(),p.y(),edgeAsset,48,-1,-24,-24));
        List<TilePoint> centers = plot.cells().stream()
                .filter(p -> TownLandPlots.interior(plot.cells(),p) == 9 && !aisle.contains(p))
                .sorted(TownLandPlots.ORDER).toList();
        List<TilePoint> occupied = new ArrayList<>();
        for (TilePoint p : centers) {
            if (occupied.size() >= 5 || occupied.stream().anyMatch(q -> Math.abs(p.x()-q.x())+Math.abs(p.y()-q.y()) < 3)) continue;
            String asset = trade ? TownGardenArt.localGoods(area.id)[occupied.size()%2]
                    : herbs ? "village_prop_seedling_tray"
                    : plot.use() == TownLandPlots.Use.HOUSE_GARDEN && !cold
                        && !area.id.equals("town_embermarket") && !area.id.equals("town_reedwatch")
                        ? "deco_tree_fruit_harvestable" : TownGardenArt.gardenPlant(area.id);
            WorldProp prop = new WorldProp(p.x(),p.y(),asset,trade || herbs ? 42 : 100,-1,0,-12);
            if (!propFits(world,area,prop,false)) continue;
            area.addProp(prop); occupied.add(p);
            if (herbs && !cold) area.tiles[p.y()][p.x()] = 'A';
        }
        // Low planted beds follow the irregular contour without blocking the access spine.
        if (!trade) for (TilePoint p : plot.cells().stream().sorted(TownLandPlots.ORDER).toList()) {
            if (TownLandPlots.interior(plot.cells(),p) < 5 || aisle.contains(p)
                    || area.propAt(p.x(),p.y()) != null || (p.x()+p.y())%3 != 0) continue;
            String asset = cold ? "deco_tundra_frost_bush" : area.id.equals("town_reedwatch")
                    ? "deco_marsh_sedge_clump" : herbs ? "village_prop_seedling_tray" : "deco_grass_wildflowers";
            if (free(world,area,p.x(),p.y())) area.addProp(new WorldProp(p.x(),p.y(),asset,32,-1,0,-12));
        }
        String name = switch (plot.use()) {
            case HOUSE_GARDEN -> "Household Garden";
            case HERB_GARDEN -> "Herb Plot";
            case CIVIC_GARDEN -> "Quiet Garden";
            case TRADE_YARD -> "Trade Yard";
        };
        area.landmarks.put(plot.entrance(),name);
    }

    private static void removeCornerFurniture(MapArea area, TilePoint c, String asset) {
        area.props.removeIf(p -> p.asset().equals(asset) && Math.abs(p.x() - c.x()) <= 6
                && Math.abs(p.y() - c.y()) == 3);
    }

    private static void gardenFront(WorldMap world, MapArea area, CityBuilding b) {
        if (b.facing() != CityBuilding.Facing.SOUTH) {
            TilePoint door = world.cityBuildingDoorTiles(b).getFirst();
            for (int side : new int[]{-2, 2}) {
                TilePoint p = b.outside(door, side, 2);
                group(world, area, List.of(new Piece(p.x(), p.y(), PLANTER)));
            }
            return;
        }
        Set<Integer> doors = new HashSet<>();
        for (TilePoint p : world.cityBuildingDoorTiles(b)) {
            doors.add(p.x() - 1); doors.add(p.x()); doors.add(p.x() + 1);
        }
        int y = b.y2() + 3;
        area.props.removeIf(p -> p.y() == y && p.x() >= b.x1() && p.x() <= b.x2()
                && p.asset().equals("city_prop_refresh_planter_pink") && !doors.contains(p.x()));
        List<TilePoint> border = new ArrayList<>();
        for (int x = b.x1() - 1; x <= b.x2() + 1; x++)
            if (!doors.contains(x)) border.add(new TilePoint(x, y));
        for (int x : new int[]{b.x1() - 1, b.x2() + 1})
            border.add(new TilePoint(x, b.y2() + 2));
        hedgeBorder(world, area, border);
        TilePoint entrance = world.cityBuildingDoorTiles(b).get(0);
        gate(world, area, entrance.x(), y, TownGardenArt.gardenGate(area.id));
        // A short continuous planter bed beside the house, separate from its entrance path.
        run(world, area, b.x2() + 2, b.y2() + 2, 2, PLANTER);
    }

    private static Yard findYard(WorldMap world, MapArea area, CityBuilding b, List<Yard> used) {
        List<String[]> shapes = specialized(b) ? List.of(COMPACT_COURT, SMALL_COURT, L_COURT, STEPPED_COURT)
                : List.of(L_COURT, STEPPED_COURT, SMALL_COURT, COMPACT_COURT);
        for (String[] shape : shapes) {
            List<Yard> candidates = new ArrayList<>();
            for (int y = b.y1() - 9; y <= b.y2() + 9; y += 2)
                for (int x = b.x1() - 9; x <= b.x2() + 9; x += 2) candidates.add(new Yard(x, y, shape));
            candidates.sort(Comparator.comparingInt(p -> Math.abs(p.x() + p.width() / 2 - (b.x1() + b.x2()) / 2)
                    + Math.abs(p.y() + p.height() / 2 - b.y2())));
            for (Yard p : candidates) {
                int exitX = p.x() + p.width() / 2, exitY = p.y() + p.height();
                if (!world.isPassable(area.id, exitX, exitY) || area.propAt(exitX, exitY) != null
                        || world.cityBuildingAt(area.id, exitX, exitY) != null) continue;
                if (used.stream().anyMatch(o -> p.x() <= o.x() + o.width() && p.x() + p.width() >= o.x()
                        && p.y() <= o.y() + o.height() && p.y() + p.height() >= o.y())) continue;
                if (area.landmarks.entrySet().stream().anyMatch(e -> Set.of("Public Garden", "Civic Plaza", "Market Square").contains(e.getValue())
                        && p.x() <= e.getKey().x() + 7 && p.x() + p.width() >= e.getKey().x() - 7
                        && p.y() <= e.getKey().y() + 4 && p.y() + p.height() >= e.getKey().y() - 4)) continue;
                boolean clear = true;
                int margin = specialized(b) ? 0 : 1;
                for (int y = -margin; y < p.height() + margin && clear; y++) for (int x = -margin; x < p.width() + margin; x++) {
                    if (!free(world, area, p.x() + x, p.y() + y)) { clear = false; break; }
                }
                if (clear) return p;
            }
        }
        return null;
    }

    private static Set<TilePoint> outline(int[][] vertices) {
        Set<TilePoint> points = new LinkedHashSet<>();
        for (int i = 0; i < vertices.length; i++) {
            int[] a = vertices[i], b = vertices[(i + 1) % vertices.length];
            int x = a[0], y = a[1];
            while (x != b[0] || y != b[1]) {
                points.add(new TilePoint(x, y)); x += Integer.signum(b[0] - x); y += Integer.signum(b[1] - y);
            }
        }
        return points;
    }

    private static void furnishYard(WorldMap world, MapArea area, Yard yard, int district, CityBuilding owner) {
        boolean civic = district < 2;
        boolean pasture = owner.key().equals("town_service_barn");
        boolean smith = owner.style().equals("blacksmith");
        boolean herbs = owner.style().equals("alchemist") || owner.style().equals("apothecary");
        List<Piece> pieces = new ArrayList<>();
        TilePoint entry = null;
        for (int y = 0; y < yard.height(); y++) for (int x = 0; x < yard.width(); x++) {
            char cell = yard.shape()[y].charAt(x);
            if (cell == '#') pieces.add(new Piece(yard.x() + x, yard.y() + y, civic ? "town_hedge_h" : "town_fence_h"));
            if (cell == 'g' && x > 0 && yard.shape()[y].charAt(x - 1) == 'g' && yard.shape()[y].charAt(x + 1) == 'g') {
                entry = new TilePoint(yard.x() + x, yard.y() + y);
                pieces.add(new Piece(entry.x(), entry.y(), civic ? TownGardenArt.gardenGate(area.id)
                        : district == 3 ? "town_fence_gate" : "town_garden_gate_arbor"));
            }
        }
        for (int x = 2; x <= 3; x++) if (yard.shape()[1].charAt(x) == '.')
            pieces.add(new Piece(yard.x() + x, yard.y() + 1, pasture
                    ? (x == 2 ? "village_prop_hay_bales" : "village_prop_water_trough")
                    : smith ? (x == 2 ? "interior_forge" : "village_prop_anvil_stump")
                    : herbs ? (x == 2 ? PLANTER : "village_prop_herb_barrel") : district == 3
                    ? TownGardenArt.localGoods(area.id)[x - 2] : PLANTER));
        int seatY = yard.height() - 3;
        for (int x = 2; x <= 3; x++) if (!pasture && yard.shape()[seatY].charAt(x) == '.')
            pieces.add(new Piece(yard.x() + x, yard.y() + seatY,
                    pasture ? "village_prop_hay_stack" : herbs ? PLANTER : smith ? STALL : BENCH));
        // Furnish the wider arm while keeping the middle entrance aisle clear.
        if (yard.width() >= 9) {
            if (civic && yard.height() >= 7) {
                pieces.add(new Piece(yard.x() + 6, yard.y() + 3, TownGardenArt.fountainFor(area.id)));
            } else {
                pieces.add(new Piece(yard.x() + 6, yard.y() + 2, smith ? STALL : herbs ? "village_prop_drying_rack" : district == 3
                        ? "village_prop_tool_rack" : "city_prop_refresh_crate_closed"));
                pieces.add(new Piece(yard.x() + 7, yard.y() + 3, district == 3
                        ? "village_prop_sawhorse" : PLANTER));
            }
        }
        if (!group(world, area, pieces)) return;
        for (int y = 0; y < yard.height(); y++) for (int x = 0; x < yard.width(); x++)
            if (yard.shape()[y].charAt(x) != ' ') area.tiles[yard.y() + y][yard.x() + x] = pasture
                    ? TownGardenArt.gardenGround(area.id) : herbs && yard.shape()[y].charAt(x) == '.' ? 'A'
                    : district == 3 && TownGardenArt.courtGround(area.id) == 'p' ? 'C' : TownGardenArt.courtGround(area.id);
        if (specialized(owner)) area.landmarks.put(new TilePoint(yard.x() + 2, yard.y() + 2),
                pasture ? "Barn Paddock" : smith ? "Smith's Working Yard" : "Apothecary Herb Garden");
        if (entry != null) connectYard(world, area, new TilePoint(entry.x(), entry.y() + 1));
    }

    private static void connectYard(WorldMap world, MapArea area, TilePoint start) {
        if (!world.isPassable(area.id, start.x(), start.y())) return;
        Map<TilePoint, TilePoint> previous = new HashMap<>();
        ArrayDeque<TilePoint> queue = new ArrayDeque<>();
        previous.put(start, null); queue.add(start);
        while (!queue.isEmpty() && previous.size() < 1024) {
            TilePoint p = queue.removeFirst();
            if (Terrain.connectingRoad(area.tileAt(p.x(), p.y()))) {
                for (TilePoint walk = previous.get(p); walk != null; walk = previous.get(walk))
                    area.tiles[walk.y()][walk.x()] = Terrain.PACKED_ROAD;
                return;
            }
            for (int[] d : new int[][]{{0,1},{-1,0},{1,0},{0,-1}}) {
                TilePoint n = new TilePoint(p.x() + d[0], p.y() + d[1]);
                if (previous.containsKey(n) || !world.isPassable(area.id, n.x(), n.y())
                        || area.propAt(n.x(), n.y()) != null || world.cityBuildingAt(area.id, n.x(), n.y()) != null
                        || world.transitionAt(area.id, n.x(), n.y()) != null || area.landmarks.containsKey(n)) continue;
                previous.put(n, p); queue.addLast(n);
            }
        }
    }

    private static void gate(WorldMap world, MapArea area, int x, int y, String asset) {
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) {
            int px = x + dx, py = y + dy;
            TilePoint p = new TilePoint(px, py);
            if (!world.isPassable(area.id, px, py) || area.propAt(px, py) != null
                    || world.cityBuildingAt(area.id, px, py) != null || world.transitionAt(area.id, px, py) != null
                    || area.landmarks.containsKey(p) || area.districts.stream().anyMatch(d -> d.work().equals(p) || d.gathering().equals(p))) return;
        }
        area.addProp(new WorldProp(x, y, asset, 144));
    }

    private static void fountain(WorldMap world, MapArea area, TilePoint center) {
        // Prefer a generous apron; denser quarters may only have the actual three-tile footprint clear.
        for (int extent : new int[]{2, 1})
        for (int radius = 1; radius <= 6; radius++) for (int dy = -radius; dy <= radius; dy++)
            for (int dx = -radius; dx <= radius; dx++) {
                if (Math.abs(dx) + Math.abs(dy) != radius) continue;
                int x = center.x() + dx, y = center.y() + dy;
                boolean clear = true;
                for (int oy = -1; oy <= extent && clear; oy++) for (int ox = -1; ox <= extent; ox++)
                    if (!free(world, area, x + ox, y + oy)) { clear = false; break; }
                if (!clear) continue;
                area.addProp(new WorldProp(x, y, TownGardenArt.fountainFor(area.id), 144));
                for (int oy = -1; oy <= extent; oy++) for (int ox = -1; ox <= extent; ox++) area.tiles[y + oy][x + ox] = 'p';
                return;
            }
    }

    private static void run(WorldMap world, MapArea area, int x, int y, int length, String asset) {
        List<Piece> pieces = new ArrayList<>();
        for (int i = 0; i < length; i++) pieces.add(new Piece(x + i, y, asset));
        group(world, area, pieces);
    }

    private static boolean group(WorldMap world, MapArea area, List<Piece> pieces) {
        if (pieces.stream().anyMatch(p -> !free(world, area, p.x(), p.y()))) return false;
        for (Piece p : pieces) area.addProp(new WorldProp(p.x(), p.y(), p.asset(), 48));
        return true;
    }

    private static boolean free(WorldMap world, MapArea area, int x, int y) {
        return free(world, area, x, y, false);
    }

    private static boolean free(WorldMap world, MapArea area, int x, int y, boolean replant) {
        TilePoint point = new TilePoint(x, y);
        if (x < 3 || y < 3 || x >= area.width() - 3 || y >= area.height() - 3
                || !world.isPassable(area.id, x, y) || Terrain.connectingRoad(area.tileAt(x, y))
                || area.propsAt(x, y).stream().anyMatch(p -> !replant || !replaceablePlant(p)) || area.landmarks.containsKey(point)
                || world.transitionAt(area.id, x, y) != null) return false;
        if (area.landmarks.entrySet().stream().anyMatch(e -> e.getValue().equals("Street Grove")
                && Math.abs(e.getKey().x() - x) <= 4 && Math.abs(e.getKey().y() - y) <= 4)) return false;
        if (area.districts.stream().anyMatch(d -> d.work().equals(point) || d.gathering().equals(point)
                || d.center().equals(point))) return false;
        if (world.npcs(area.id).stream().anyMatch(n -> n.x() == x && n.y() == y)) return false;
        for (CityBuilding b : world.cityBuildings(area.id)) {
            if (x >= b.x1() - 1 && x <= b.x2() + 1 && y >= b.y1() - 2 && y <= b.y2() + 1) return false;
            for (TilePoint door : world.cityBuildingDoorTiles(b))
                if (b.approachContains(door, x, y, 3)) return false;
        }
        return area.propsInBounds(x - 2, y - 2, x + 3, y + 3).stream()
                .noneMatch(p -> p.asset().startsWith("town_portal_"));
    }
}
