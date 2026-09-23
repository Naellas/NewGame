package com.alderfall.game.map;

import com.alderfall.game.TilePoint;
import com.alderfall.game.CavernStyle;
import com.alderfall.game.BuiltDungeonStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds a dungeon as a place with a history, rather than as independent tile noise.
 * A stable site profile carries exterior, region, folklore, and vertical direction into
 * every floor. Floors then receive functional rooms, a guaranteed critical route,
 * room-aware prop clusters, and encounter formations.
 */
final class DungeonGenerator {
    private static final int WIDTH = 36;
    private static final int HEIGHT = 26;

    enum Verticality { ASCENDING, DESCENDING }

    enum RoomRole {
        ENTRY, GUARD, HABITATION, WORKS, BURIAL, SHRINE, TREASURE, BOSS, EXIT
    }

    record SiteProfile(
            String siteId,
            String label,
            String theme,
            String region,
            char exterior,
            String folklore,
            Verticality verticality,
            int seed
    ) { }

    record Room(RoomRole role, int cx, int cy, int rx, int ry) {
        boolean contains(int x, int y) {
            return Math.abs(x - cx) <= rx && Math.abs(y - cy) <= ry;
        }
    }

    record PropSpec(int x, int y, String asset, int size) { }

    private record PropMotif(String anchor, String support, String light) {
        String asset(int index) {
            return index == 0 ? anchor : index == 1 ? support : light;
        }
    }

    record EncounterSpec(int x, int y, String role, int group, boolean boss, int leash) { }

    record FloorPlan(
            SiteProfile profile,
            int floor,
            int floors,
            char[][] tiles,
            TilePoint up,
            TilePoint down,
            List<Room> rooms,
            List<PropSpec> props,
            List<EncounterSpec> encounters,
            Set<TilePoint> criticalRoute
    ) { }

    private static final int[][][] TOPOLOGIES = {
            {{3, 12}, {8, 5}, {17, 5}, {27, 7}, {18, 12}, {8, 20}, {21, 20}, {32, 17}},
            {{5, 4}, {14, 5}, {25, 5}, {31, 11}, {27, 19}, {17, 20}, {8, 19}, {3, 13}},
            {{3, 19}, {8, 12}, {7, 5}, {17, 6}, {18, 14}, {28, 13}, {30, 5}, {32, 20}},
            {{32, 5}, {25, 5}, {18, 10}, {28, 18}, {18, 20}, {9, 19}, {7, 10}, {3, 5}},
            {{3, 6}, {10, 5}, {15, 12}, {9, 20}, {20, 20}, {27, 15}, {26, 6}, {33, 11}},
            {{32, 19}, {27, 12}, {29, 5}, {19, 6}, {17, 14}, {8, 13}, {6, 20}, {3, 8}}
    };

    private DungeonGenerator() { }

    static SiteProfile profile(String id, String label, String theme, String region, char exterior, int seed) {
        String folklore = folkloreFor(id, theme, region, exterior);
        Verticality verticality = switch (theme) {
            case "abandoned_castle", "bandit_camp" -> Verticality.ASCENDING;
            default -> Verticality.DESCENDING;
        };
        return new SiteProfile(id, label, theme, region, exterior, folklore, verticality,
                mix(seed, id.hashCode(), label.hashCode()));
    }

    static FloorPlan generate(SiteProfile profile, int floor, int floors) {
        int salt = mix(profile.seed(), floor * 3571, floors * 101);
        int topologyIndex = Math.floorMod(salt + floor * 3, TOPOLOGIES.length);
        int[][] points = TOPOLOGIES[topologyIndex];
        char voidTile = switch (profile.theme()) {
            case "cave", "abandoned_castle" -> 'Z';
            default -> 'x';
        };
        char base = baseFloor(profile.theme());
        char[][] grid = filled(WIDTH, HEIGHT, voidTile);
        Set<TilePoint> route = new LinkedHashSet<>();
        List<Room> rooms = new ArrayList<>();
        List<RoomRole> roles = rolesFor(profile, floor, floors, salt);

        for (int i = 0; i < points.length; i++) {
            int[] point = points[i];
            int rx = (i == 0 || i == points.length - 1) ? 2 : 2 + Math.floorMod(mix(salt, i, 11), 3);
            int ry = (i == 0 || i == points.length - 1) ? 2 : 2 + Math.floorMod(mix(salt, i, 23), 2);
            Room room = new Room(roles.get(i), point[0], point[1], rx, ry);
            rooms.add(room);
            carveRoom(grid, room, base, profile.theme().equals("cave"), salt + i * 43);
            if (i > 0) {
                carveCorridor(grid, rooms.get(i - 1), room, base, route, salt + i * 71);
            }
        }
        // A loop makes navigation less linear while preserving the readable main route.
        int loopFrom = 1 + Math.floorMod(salt, 2);
        int loopTo = 5 + Math.floorMod(salt / 7, 2);
        carveCorridor(grid, rooms.get(loopFrom), rooms.get(loopTo), base, route, salt + 997);

        for (Room room : rooms) {
            paintRoomIdentity(grid, room, profile, floor, floors, salt);
        }
        blendExteriorInfluence(grid, profile, floor, route, salt);
        placeFeatureTiles(grid, profile, floor, floors, rooms);
        TilePoint up = new TilePoint(rooms.get(0).cx(), rooms.get(0).cy());
        TilePoint down = floor < floors
                ? new TilePoint(rooms.get(rooms.size() - 1).cx(), rooms.get(rooms.size() - 1).cy())
                : null;
        reserveApproach(route, up);
        if (down != null) reserveApproach(route, down);
        if (profile.theme().equals("abandoned_castle")) {
            grid[up.y()][up.x()] = '5';
            if (down != null) grid[down.y()][down.x()] = '6';
        }
        addBoundaryWalls(grid, profile.theme());

        List<PropSpec> props = buildProps(profile, floor, floors, rooms, route, salt);
        Set<TilePoint> occupied = new HashSet<>();
        for (PropSpec prop : props) occupied.add(new TilePoint(prop.x(), prop.y()));
        List<EncounterSpec> encounters = buildEncounters(profile, floor, floors, rooms, route, occupied, grid, salt);
        if (CavernStyle.natural(profile.theme()) || BuiltDungeonStyle.supports(profile.theme())) {
            addCavernResources(profile, floor, rooms, route, props, encounters, grid, salt);
        }
        return new FloorPlan(profile, floor, floors, grid, up, down, List.copyOf(rooms),
                List.copyOf(props), List.copyOf(encounters), Set.copyOf(route));
    }

    static String floorLabel(SiteProfile profile, int floor, int floors) {
        String stage = floor == 1 ? upperLabel(profile) : floor == floors ? terminalLabel(profile) : middleLabel(profile, floor);
        return profile.label() + " - " + stage;
    }

    private static String folkloreFor(String id, String theme, String region, char exterior) {
        if (id.contains("stonegate")) return "witnessed-names";
        if (id.contains("miredepth") || id.contains("belltower")) return "answered-bells";
        if (id.contains("frosthollow")) return "winter-remembrance";
        if (id.contains("blackvault")) return "broken-oaths";
        if (id.contains("ironbarrow")) return "unending-vigil";
        if (id.contains("cairnspire")) return "mountain-cairns";
        if (id.contains("stormfen")) return "storm-and-tide";
        if (id.contains("redcap")) return "antlers-due";
        if (id.contains("crowhook") || id.contains("greyhook")) return "stolen-road-oaths";
        if (exterior == 'n' || exterior == 'q') return "winter-remembrance";
        if (exterior == 'f' || exterior == 'w') return "answered-bells";
        return switch (region) {
            case "sanctum" -> "guest-flame";
            case "highwall", "northroad" -> "witnessed-names";
            case "belltower" -> "answered-bells";
            case "riverside" -> "guest-road";
            default -> theme.equals("crypt") ? "witnessed-names" : "broken-oaths";
        };
    }

    private static List<RoomRole> rolesFor(SiteProfile profile, int floor, int floors, int salt) {
        RoomRole[] middle = switch (profile.theme()) {
            case "cave" -> new RoomRole[]{RoomRole.GUARD, RoomRole.WORKS, RoomRole.SHRINE, RoomRole.HABITATION, RoomRole.TREASURE, RoomRole.BURIAL};
            case "crypt" -> new RoomRole[]{RoomRole.GUARD, RoomRole.BURIAL, RoomRole.SHRINE, RoomRole.BURIAL, RoomRole.TREASURE, RoomRole.WORKS};
            case "prison" -> new RoomRole[]{RoomRole.GUARD, RoomRole.HABITATION, RoomRole.WORKS, RoomRole.BURIAL, RoomRole.GUARD, RoomRole.TREASURE};
            case "sewer" -> new RoomRole[]{RoomRole.WORKS, RoomRole.GUARD, RoomRole.HABITATION, RoomRole.SHRINE, RoomRole.WORKS, RoomRole.TREASURE};
            case "goblin_camp", "bandit_camp" -> new RoomRole[]{RoomRole.GUARD, RoomRole.HABITATION, RoomRole.WORKS, RoomRole.TREASURE, RoomRole.GUARD, RoomRole.SHRINE};
            default -> new RoomRole[]{RoomRole.GUARD, RoomRole.HABITATION, RoomRole.WORKS, RoomRole.SHRINE, RoomRole.BURIAL, RoomRole.TREASURE};
        };
        List<RoomRole> result = new ArrayList<>();
        result.add(RoomRole.ENTRY);
        int rotation = Math.floorMod(salt + floor, middle.length);
        for (int i = 0; i < middle.length; i++) result.add(middle[(i + rotation) % middle.length]);
        result.add(floor == floors ? RoomRole.BOSS : RoomRole.EXIT);
        return result;
    }

    private static void carveRoom(char[][] grid, Room room, char floor, boolean organic, int salt) {
        for (int y = room.cy() - room.ry(); y <= room.cy() + room.ry(); y++) {
            for (int x = room.cx() - room.rx(); x <= room.cx() + room.rx(); x++) {
                if (x <= 0 || y <= 0 || x >= WIDTH - 1 || y >= HEIGHT - 1) continue;
                if (organic && Math.abs(x - room.cx()) == room.rx() && Math.abs(y - room.cy()) == room.ry()
                        && Math.floorMod(mix(x, y, salt), 100) < 62) continue;
                grid[y][x] = floor;
            }
        }
    }

    private static void carveCorridor(char[][] grid, Room from, Room to, char floor, Set<TilePoint> route, int salt) {
        int x = from.cx(), y = from.cy();
        boolean horizontalFirst = (salt & 1) == 0;
        while (x != to.cx() || y != to.cy()) {
            carveCorridorTile(grid, x, y, floor, route);
            if ((horizontalFirst && x != to.cx()) || y == to.cy()) x += Integer.compare(to.cx(), x);
            else y += Integer.compare(to.cy(), y);
        }
        carveCorridorTile(grid, x, y, floor, route);
    }

    private static void carveCorridorTile(char[][] grid, int x, int y, char floor, Set<TilePoint> route) {
        if (x <= 0 || y <= 0 || x >= WIDTH - 1 || y >= HEIGHT - 1) return;
        grid[y][x] = floor;
        route.add(new TilePoint(x, y));
        // Two-wide passages prevent single-file snagging and read cleanly at game scale.
        if (x + 1 < WIDTH - 1) grid[y][x + 1] = floor;
        if (y + 1 < HEIGHT - 1) grid[y + 1][x] = floor;
    }

    private static void paintRoomIdentity(char[][] grid, Room room, SiteProfile profile, int floor, int floors, int salt) {
        char accent = accentTile(profile.theme(), room.role(), floor == floors);
        if (accent == baseFloor(profile.theme())) return;
        for (int y = room.cy() - Math.max(1, room.ry() - 1); y <= room.cy() + Math.max(1, room.ry() - 1); y++) {
            for (int x = room.cx() - Math.max(1, room.rx() - 1); x <= room.cx() + Math.max(1, room.rx() - 1); x++) {
                if (x <= 0 || y <= 0 || x >= WIDTH - 1 || y >= HEIGHT - 1) continue;
                double nx = Math.abs(x - room.cx()) / (double) Math.max(1, room.rx());
                double ny = Math.abs(y - room.cy()) / (double) Math.max(1, room.ry());
                double radius = Math.max(nx, ny);
                int coverage = (int) Math.round((1.12 - radius) * 92)
                        + Math.floorMod(mix(x / 2, y / 2, salt), 31) - 15;
                if (grid[y][x] == baseFloor(profile.theme())
                        && Math.floorMod(mix(x, y, salt + 41), 100) < coverage) grid[y][x] = accent;
            }
        }
    }

    /** Surface conditions leak inward in coarse, connected patches instead of rectangular carpets. */
    private static void blendExteriorInfluence(char[][] grid, SiteProfile profile, int floor,
                                               Set<TilePoint> route, int salt) {
        char base = baseFloor(profile.theme());
        char influence;
        int chance;
        if (profile.folklore().contains("bell") || profile.folklore().contains("storm")
                || profile.exterior() == 'f' || profile.exterior() == 'w') {
            influence = profile.theme().equals("sewer") ? 'J' : 'M';
            chance = 18 + floor * 2;
        } else if (profile.folklore().contains("winter") || profile.folklore().contains("cairn")
                || profile.exterior() == 'n' || profile.exterior() == 'q') {
            influence = profile.theme().equals("cave") ? 'E' : 'F';
            chance = 13;
        } else if (profile.region().equals("sanctum") || profile.exterior() == 's' || profile.exterior() == 'b') {
            influence = profile.theme().equals("goblin_camp") ? 'R' : 'F';
            chance = 12 + floor;
        } else if (profile.region().equals("crownlands") || profile.exterior() == 'g') {
            influence = 'M';
            chance = 10;
        } else {
            return;
        }
        if (influence == base) return;
        char[][] source = copy(grid);
        for (int y = 2; y < HEIGHT - 2; y++) {
            for (int x = 2; x < WIDTH - 2; x++) {
                if (source[y][x] != base || route.contains(new TilePoint(x, y))) continue;
                int field = Math.floorMod(mix(x / 3, y / 3, salt + profile.exterior() * 17), 100);
                int detail = Math.floorMod(mix(x, y, salt + floor * 131), 17) - 8;
                if (field < chance + detail && adjacentFloor(source, x, y) >= 2) grid[y][x] = influence;
            }
        }
    }

    /**
     * Landmark materials stay sparse: warm light marks a shrine or working room and the terminal
     * ward marks the objective. This makes those images useful navigation cues rather than visibly
     * repeated tiles.
     */
    private static void placeFeatureTiles(char[][] grid, SiteProfile profile, int floor, int floors,
                                          List<Room> rooms) {
        Room lightLandmark = rooms.stream().filter(room -> room.role() == RoomRole.SHRINE).findFirst()
                .orElseGet(() -> rooms.stream().filter(room -> room.role() == RoomRole.WORKS).findFirst()
                        .orElseGet(() -> rooms.stream().filter(room -> room.role() == RoomRole.GUARD).findFirst()
                                .orElse(rooms.get(1))));
        for (Room room : rooms) {
            if (room == lightLandmark && isFloor(grid[room.cy()][room.cx()])) {
                grid[room.cy()][room.cx()] = 'L';
            }
            if (floor == floors && room.role() == RoomRole.BOSS
                    && !profile.theme().equals("abandoned_castle")
                    && isFloor(grid[room.cy()][room.cx()])) {
                grid[room.cy()][room.cx()] = 'S';
            }
        }
    }

    private static int adjacentFloor(char[][] grid, int x, int y) {
        int count = 0;
        if (isFloor(grid[y - 1][x])) count++;
        if (isFloor(grid[y + 1][x])) count++;
        if (isFloor(grid[y][x - 1])) count++;
        if (isFloor(grid[y][x + 1])) count++;
        return count;
    }

    private static char accentTile(String theme, RoomRole role, boolean deepest) {
        return switch (theme) {
            case "cave" -> role == RoomRole.BOSS || role == RoomRole.WORKS ? 'E' : 'N';
            case "crypt" -> role == RoomRole.SHRINE ? 'D' : role == RoomRole.BOSS || role == RoomRole.BURIAL ? 'F' : role == RoomRole.HABITATION ? 'M' : 'Q';
            case "abandoned_castle" -> role == RoomRole.BOSS ? '4' : role == RoomRole.SHRINE ? '3' : role == RoomRole.BURIAL ? '1' : role == RoomRole.GUARD ? '2' : 'H';
            case "prison" -> role == RoomRole.BOSS ? 'F' : role == RoomRole.BURIAL ? 'R' : 'I';
            case "sewer" -> role == RoomRole.BOSS ? 'F' : role == RoomRole.HABITATION ? 'M' : 'J';
            case "goblin_camp" -> role == RoomRole.BOSS ? 'F' : role == RoomRole.HABITATION ? 'M' : 'R';
            case "bandit_camp" -> role == RoomRole.BOSS ? '4' : role == RoomRole.GUARD ? 'I' : 'H';
            default -> deepest && role == RoomRole.BOSS ? 'F' : 'd';
        };
    }

    private static List<PropSpec> buildProps(SiteProfile profile, int floor, int floors, List<Room> rooms,
                                              Set<TilePoint> route, int salt) {
        List<PropSpec> result = new ArrayList<>();
        Set<TilePoint> occupied = new HashSet<>();
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            List<TilePoint> candidates = edgeCandidates(room, salt + i * 83);
            PropMotif motif = motifFor(profile, room.role(), salt + i * 109);
            int desired = switch (room.role()) {
                case ENTRY, EXIT -> 1;
                case BOSS -> 3;
                case SHRINE, HABITATION, WORKS, BURIAL -> 2;
                default -> 1 + Math.floorMod(salt + i, 2);
            };
            int placed = 0;
            for (TilePoint point : candidates) {
                if (placed >= desired || route.contains(point) || occupied.contains(point)) continue;
                if (nearStair(point, rooms.get(0)) || (floor < floors && nearStair(point, rooms.get(rooms.size() - 1)))) continue;
                String asset = room.role() == RoomRole.TREASURE && placed == 0
                        ? "dungeon_prop_castle_chest" : motif.asset(placed);
                if (profile.theme().equals("cave")) {
                    asset = CavernStyle.detail(CavernStyle.biome(profile.region(), profile.exterior()), asset);
                }
                if (BuiltDungeonStyle.supports(profile.theme())) asset = BuiltDungeonStyle.propAsset(asset);
                result.add(new PropSpec(point.x(), point.y(), asset, propSize(asset)));
                occupied.add(point);
                placed++;
            }
        }
        // Stairs are a navigation landmark, kept visually isolated from dressing.
        boolean climbsForward = profile.verticality() == Verticality.ASCENDING;
        String returnStair = climbsForward ? "dungeon_prop_stairs_down" : "dungeon_prop_stairs_up";
        String forwardStair = climbsForward ? "dungeon_prop_stairs_up" : "dungeon_prop_stairs_down";
        Room entry = rooms.get(0);
        result.add(new PropSpec(entry.cx(), entry.cy(), returnStair, 58));
        if (floor < floors) {
            Room exit = rooms.get(rooms.size() - 1);
            result.add(new PropSpec(exit.cx(), exit.cy(), forwardStair, 60));
        }
        return result;
    }

    private static List<TilePoint> edgeCandidates(Room room, int salt) {
        List<TilePoint> points = new ArrayList<>(List.of(
                new TilePoint(room.cx() - room.rx() + 1, room.cy() - room.ry() + 1),
                new TilePoint(room.cx() + room.rx() - 1, room.cy() - room.ry() + 1),
                new TilePoint(room.cx() + room.rx() - 1, room.cy() + room.ry() - 1),
                new TilePoint(room.cx() - room.rx() + 1, room.cy() + room.ry() - 1),
                new TilePoint(room.cx(), room.cy() - room.ry() + 1),
                new TilePoint(room.cx(), room.cy() + room.ry() - 1)
        ));
        int rotation = Math.floorMod(salt, points.size());
        List<TilePoint> rotated = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) rotated.add(points.get((i + rotation) % points.size()));
        return rotated;
    }

    private static PropMotif motifFor(SiteProfile profile, RoomRole role, int salt) {
        String[] functional = switch (role) {
            case ENTRY, EXIT -> new String[]{"dungeon_prop_lantern_stand", "location_dungeon_braziers", "dungeon_prop_castle_candles"};
            case GUARD -> new String[]{"dungeon_prop_chain_stand", "dungeon_prop_castle_iron_gate", "dungeon_prop_lantern_stand", "location_dungeon_rubble_cairn"};
            case HABITATION -> new String[]{"dungeon_prop_cave_bedroll", "location_camp_crates", "location_camp_fire", "dungeon_prop_castle_books"};
            case WORKS -> new String[]{"dungeon_prop_cave_crates", "dungeon_prop_relic_crate", "dungeon_prop_castle_books", "dungeon_prop_chain_stand"};
            case BURIAL -> new String[]{"location_crypt_sarcophagus", "location_dungeon_grave_slabs", "dungeon_prop_castle_bones", "dungeon_prop_castle_sarcophagus"};
            case SHRINE -> new String[]{"dungeon_prop_rune_pillar", "location_dungeon_broken_altar", "location_dungeon_braziers", "dungeon_prop_castle_altar"};
            case TREASURE -> new String[]{"dungeon_prop_castle_chest", "dungeon_prop_relic_crate", "dungeon_prop_cave_crates"};
            case BOSS -> new String[]{"dungeon_prop_castle_altar", "dungeon_prop_rune_pillar", "location_dungeon_braziers", "dungeon_prop_castle_statue"};
        };
        String[] thematic = thematicProps(profile);
        String detail = modularDetail(profile.theme(), role, salt);
        String[] lights = switch (profile.folklore()) {
            case "answered-bells", "storm-and-tide" -> new String[]{"dungeon_prop_lantern_stand", "dungeon_prop_cave_torch"};
            case "guest-flame" -> new String[]{"location_dungeon_braziers", "dungeon_prop_castle_candles"};
            default -> profile.theme().equals("cave")
                    ? new String[]{"dungeon_prop_cave_torch", "dungeon_prop_cave_crystal"}
                    : new String[]{"dungeon_prop_castle_candles", "dungeon_prop_lantern_stand"};
        };
        String anchor = functional[Math.floorMod(salt, functional.length)];
        String support = detail != null ? detail : thematic[Math.floorMod(salt / 7 + role.ordinal(), thematic.length)];
        String light = lights[Math.floorMod(salt / 17, lights.length)];
        return new PropMotif(anchor, support, light);
    }

    private static String modularDetail(String theme, RoomRole role, int salt) {
        return switch (theme) {
            case "cave" -> (salt & 1) == 0
                    ? "dungeon_detail_cave_stalagmites" : "dungeon_detail_cave_mineral_cluster";
            case "crypt" -> switch (role) {
                case BURIAL -> "dungeon_detail_crypt_ossuary";
                case SHRINE, BOSS -> "dungeon_detail_crypt_offering";
                default -> "dungeon_detail_crypt_urn";
            };
            case "abandoned_castle", "vampire_castle" -> switch (role) {
                case BURIAL -> "dungeon_detail_vampire_coffin";
                case GUARD, BOSS -> "dungeon_detail_vampire_banner";
                default -> "dungeon_detail_vampire_candelabrum";
            };
            case "bandit_camp" -> switch (role) {
                case HABITATION -> "dungeon_detail_bandit_bedroll";
                case GUARD, WORKS, BOSS -> "dungeon_detail_bandit_weapon_rack";
                default -> "dungeon_detail_bandit_supply_sacks";
            };
            case "goblin_camp" -> switch (role) {
                case HABITATION -> "dungeon_detail_goblin_cookpot";
                case SHRINE, BOSS -> "dungeon_detail_goblin_totem";
                default -> "dungeon_detail_goblin_scrap_heap";
            };
            default -> null;
        };
    }

    private static String[] thematicProps(SiteProfile profile) {
        if (profile.folklore().contains("bell") || profile.folklore().contains("storm")) {
            return new String[]{"dungeon_prop_lantern_stand", "deco_soft_water_wet_stones", "deco_soft_water_reeds_gold", "deco_imagen_marsh_bubble_pool"};
        }
        if (profile.folklore().contains("winter") || profile.folklore().contains("cairn")) {
            return new String[]{"dungeon_prop_cave_crystal", "location_dungeon_rubble_cairn", "deco_imagen_flat_stone_stack", "dungeon_prop_rune_pillar"};
        }
        if (profile.folklore().contains("guest-flame")) {
            return new String[]{"location_dungeon_braziers", "dungeon_prop_castle_urns", "dungeon_prop_castle_candles", "dungeon_prop_castle_altar"};
        }
        if (profile.folklore().contains("names") || profile.folklore().contains("oath")) {
            return new String[]{"dungeon_prop_rune_pillar", "location_dungeon_grave_slabs", "dungeon_prop_castle_books", "location_dungeon_broken_altar"};
        }
        return switch (profile.theme()) {
            case "cave" -> new String[]{"dungeon_prop_cave_crystal", "dungeon_prop_cave_torch", "deco_rocks"};
            case "goblin_camp", "bandit_camp" -> new String[]{"location_camp_crates", "dungeon_prop_cave_bedroll", "dungeon_prop_chain_stand"};
            default -> new String[]{"dungeon_prop_lantern_stand", "dungeon_prop_relic_crate", "location_dungeon_rubble_cairn"};
        };
    }

    private static List<EncounterSpec> buildEncounters(SiteProfile profile, int floor, int floors, List<Room> rooms,
                                                        Set<TilePoint> route, Set<TilePoint> props, char[][] grid, int salt) {
        List<EncounterSpec> result = new ArrayList<>();
        int group = 0;
        for (int i = 1; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            if (room.role() == RoomRole.EXIT || room.role() == RoomRole.TREASURE) continue;
            if (room.role() == RoomRole.BOSS) {
                addEncounter(result, room.cx(), room.cy(), room.role(), group++, true, 5, props, grid);
                addEncounter(result, room.cx() - 2, room.cy() + 1, RoomRole.GUARD, group, false, 4, props, grid);
                addEncounter(result, room.cx() + 2, room.cy() + 1, RoomRole.GUARD, group, false, 4, props, grid);
                continue;
            }
            boolean active = Math.floorMod(mix(salt, i, floor), 100) < 48 + floor * 8;
            if (!active) continue;
            int dx = room.rx() >= room.ry() ? 1 : 0;
            int dy = dx == 0 ? 1 : 0;
            addEncounter(result, room.cx() - dx, room.cy() - dy, room.role(), group, false, 3, props, grid);
            if (room.role() == RoomRole.GUARD || room.role() == RoomRole.HABITATION || floor > 1) {
                addEncounter(result, room.cx() + dx, room.cy() + dy, room.role(), group, false, 3, props, grid);
            }
            group++;
        }
        // Every non-terminal floor still teaches the room/encounter grammar with
        // at least one visible pair; seeded variation changes which other rooms are occupied.
        for (int i = 1; result.size() < 2 && i < rooms.size() - 1; i++) {
            Room room = rooms.get(i);
            addEncounter(result, room.cx(), room.cy(), room.role(), group++, false, 3, props, grid);
        }
        // Append newly active rooms so existing sequential monster IDs retain their original slots.
        if (CavernStyle.natural(profile.theme()) || BuiltDungeonStyle.supports(profile.theme())) {
            for (int i = 1; i < rooms.size(); i++) {
                Room room = rooms.get(i);
                if (room.role() == RoomRole.EXIT || room.role() == RoomRole.TREASURE || room.role() == RoomRole.BOSS) continue;
                int roll = Math.floorMod(mix(salt, i, floor), 100);
                int threshold = 48 + floor * 8;
                if (roll < threshold || roll >= threshold + 8) continue;
                int dx = room.rx() >= room.ry() ? 1 : 0;
                int dy = dx == 0 ? 1 : 0;
                addEncounter(result, room.cx()-dx, room.cy()-dy, room.role(), group, false, 3, props, grid);
                if (room.role() == RoomRole.GUARD || room.role() == RoomRole.HABITATION || floor > 1)
                    addEncounter(result, room.cx()+dx, room.cy()+dy, room.role(), group, false, 3, props, grid);
                group++;
            }
        }
        return result;
    }

    /** Small deposits sit inside reachable room edges, never on stairs, routes, or monster slots. */
    private static void addCavernResources(SiteProfile profile, int floor, List<Room> rooms,
                                          Set<TilePoint> route, List<PropSpec> props,
                                          List<EncounterSpec> encounters, char[][] grid, int salt) {
        Set<TilePoint> occupied = new HashSet<>();
        props.forEach(p -> occupied.add(new TilePoint(p.x(), p.y())));
        encounters.forEach(e -> occupied.add(new TilePoint(e.x(), e.y())));
        String biome = CavernStyle.biome(profile.region(), profile.exterior());
        boolean built = BuiltDungeonStyle.supports(profile.theme());
        List<String> resources = built ? BuiltDungeonStyle.resources(profile.theme(),profile.region(),profile.exterior(),floor)
                : CavernStyle.resources(biome, floor);
        List<String> dressing = built ? BuiltDungeonStyle.dressing(profile.theme(),floor) : CavernStyle.dressing(biome, profile.theme());
        int resourceIndex = Math.floorMod(salt, resources.size());
        for (int i = 1; i < rooms.size()-1; i++) {
            Room room = rooms.get(i);
            List<TilePoint> candidates = new ArrayList<>();
            for (int y = Math.max(1, room.cy()-room.ry()); y <= Math.min(HEIGHT-2, room.cy()+room.ry()); y++) {
                for (int x = Math.max(1, room.cx()-room.rx()); x <= Math.min(WIDTH-2, room.cx()+room.rx()); x++) {
                    TilePoint point = new TilePoint(x,y);
                    if (!isFloor(grid[y][x]) || route.contains(point) || occupied.contains(point)) continue;
                    if (Math.abs(x-rooms.get(0).cx()) + Math.abs(y-rooms.get(0).cy()) <= 2
                            || Math.abs(x-rooms.get(rooms.size()-1).cx()) + Math.abs(y-rooms.get(rooms.size()-1).cy()) <= 2) continue;
                    if (Math.abs(x-room.cx()) < room.rx()-1 && Math.abs(y-room.cy()) < room.ry()-1) continue;
                    candidates.add(point);
                }
            }
            // Prefer actual rock edges, then deterministically vary positions within that band.
            candidates.sort(java.util.Comparator.<TilePoint>comparingInt(p -> adjacentFloor(grid,p.x(),p.y()))
                    .thenComparingInt(p -> mix(p.x(),p.y(),salt + room.cx()*31)));
            int placed = 0;
            for (TilePoint point : candidates) {
                if (placed >= 2) break;
                if (occupied.stream().anyMatch(p -> Math.abs(p.x()-point.x()) + Math.abs(p.y()-point.y()) <= 1)) continue;
                String asset = placed == 0 ? resources.get(resourceIndex++ % resources.size())
                        : dressing.get(Math.floorMod(salt+i, dressing.size()));
                props.add(new PropSpec(point.x(), point.y(), asset, placed == 0 ? 40 : 36));
                occupied.add(point);
                placed++;
            }
        }
    }

    private static void addEncounter(List<EncounterSpec> result, int x, int y, RoomRole role, int group,
                                     boolean boss, int leash, Set<TilePoint> props, char[][] grid) {
        TilePoint point = new TilePoint(x, y);
        if (x <= 0 || y <= 0 || x >= WIDTH - 1 || y >= HEIGHT - 1 || props.contains(point)
                || !isFloor(grid[y][x]) || result.stream().anyMatch(existing -> existing.x() == x && existing.y() == y)) return;
        result.add(new EncounterSpec(x, y, role.name().toLowerCase(), group, boss, leash));
    }

    private static boolean nearStair(TilePoint point, Room stairRoom) {
        return Math.abs(point.x() - stairRoom.cx()) + Math.abs(point.y() - stairRoom.cy()) <= 1;
    }

    private static void reserveApproach(Set<TilePoint> route, TilePoint stair) {
        route.add(stair);
        route.add(new TilePoint(Math.min(WIDTH - 2, stair.x() + 1), stair.y()));
        route.add(new TilePoint(stair.x(), Math.min(HEIGHT - 2, stair.y() + 1)));
    }

    private static void addBoundaryWalls(char[][] grid, String theme) {
        if (!theme.equals("cave") && !theme.equals("abandoned_castle")) return;
        char wall = theme.equals("cave") ? 'O' : 'X';
        for (int y = 1; y < HEIGHT - 1; y++) {
            for (int x = 1; x < WIDTH - 1; x++) {
                if (grid[y][x] != 'Z') continue;
                for (int oy = -1; oy <= 1; oy++) {
                    for (int ox = -1; ox <= 1; ox++) {
                        if (isFloor(grid[y + oy][x + ox])) grid[y][x] = wall;
                    }
                }
            }
        }
    }

    private static boolean isFloor(char tile) {
        return switch (tile) {
            case 'd', 'D', 'F', 'M', 'R', 'S', 'L', 'N', 'E', 'I', 'J', 'H', 'Q', '1', '2', '3', '4', '5', '6' -> true;
            default -> false;
        };
    }

    private static char baseFloor(String theme) {
        return switch (theme) {
            case "cave" -> 'N';
            case "crypt" -> 'Q';
            case "abandoned_castle", "bandit_camp" -> 'H';
            case "prison" -> 'I';
            case "sewer" -> 'J';
            case "goblin_camp" -> 'R';
            default -> 'd';
        };
    }

    private static int propSize(String asset) {
        return switch (asset) {
            case "location_dungeon_collapsed_wall" -> 54;
            case "location_dungeon_broken_altar", "location_crypt_sarcophagus", "dungeon_prop_castle_altar",
                    "dungeon_prop_castle_sarcophagus", "dungeon_prop_castle_statue", "dungeon_prop_castle_iron_gate" -> 50;
            case "location_dungeon_braziers" -> 48;
            case "location_dungeon_grave_slabs", "dungeon_prop_cave_crystal", "dungeon_prop_cave_torch",
                    "dungeon_prop_cave_crates", "dungeon_prop_cave_bedroll" -> 44;
            default -> 42;
        };
    }

    private static String upperLabel(SiteProfile profile) {
        return switch (profile.theme()) {
            case "cave" -> "Threshold Caverns";
            case "crypt" -> "Processional Halls";
            case "abandoned_castle" -> "Lower Ward";
            case "prison" -> "Intake Blocks";
            case "sewer" -> "Service Channels";
            case "goblin_camp" -> "Root Warren";
            case "bandit_camp" -> "Gate Stores";
            default -> "Threshold";
        };
    }

    private static String middleLabel(SiteProfile profile, int floor) {
        return switch (profile.theme()) {
            case "cave" -> profile.folklore().contains("bell") ? "Echo Galleries" : "Working Galleries";
            case "crypt" -> profile.folklore().contains("names") ? "Hall of Witnesses" : "Ossuary " + floor;
            case "abandoned_castle" -> "Broken Great Hall";
            case "prison" -> "Iron Gallery";
            case "sewer" -> "Bell Cistern " + floor;
            case "goblin_camp" -> "Occupied Cellars";
            case "bandit_camp" -> "Contraband Floor";
            default -> "Level " + floor;
        };
    }

    private static String terminalLabel(SiteProfile profile) {
        return switch (profile.theme()) {
            case "cave" -> "Deep Grotto";
            case "crypt" -> "Named Tomb";
            case "abandoned_castle" -> "Crown Tower";
            case "prison" -> "Oubliette";
            case "sewer" -> "Drowned Sump";
            case "goblin_camp" -> "Warlord's Den";
            case "bandit_camp" -> "Captain's Loft";
            default -> "Inner Sanctum";
        };
    }

    private static char[][] filled(int width, int height, char tile) {
        char[][] grid = new char[height][width];
        for (char[] row : grid) Arrays.fill(row, tile);
        return grid;
    }

    private static char[][] copy(char[][] source) {
        char[][] result = new char[source.length][];
        for (int y = 0; y < source.length; y++) result[y] = source[y].clone();
        return result;
    }

    private static int mix(int a, int b, int c) {
        int h = a * 0x45d9f3b + b * 0x119de1f3 + c * 0x3449d;
        h ^= h >>> 16;
        h *= 0x45d9f3b;
        return h ^ (h >>> 16);
    }
}
