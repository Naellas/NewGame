package com.alderfall.game.map;

import com.alderfall.game.TilePoint;
import com.alderfall.game.WorldProp;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Regression checks for contextual multi-storey dungeons, dressing, and encounters. */
public final class DungeonGenerationTest {
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static void main(String[] args) throws Exception {
        checkNavigationAssets();
        checkDungeonFloorAssets();
        for (long seed : new long[]{0, 1, 42, 2026, -17}) {
            checkWorld(new WorldMap(seed), seed);
        }
    }

    private static void checkNavigationAssets() throws Exception {
        for (String name : List.of("dungeon_prop_stairs_up", "dungeon_prop_stairs_down")) {
            Path path = Path.of("assets", "deco", name + ".png");
            require(Files.isRegularFile(path), "Missing generated navigation asset " + path);
            BufferedImage image = ImageIO.read(path.toFile());
            require(image != null && image.getColorModel().hasAlpha(), "Navigation asset needs alpha: " + name);
            require((image.getRGB(0, 0) >>> 24) == 0, "Navigation asset corner is not transparent: " + name);
        }
    }

    private static void checkDungeonFloorAssets() throws Exception {
        for (String name : List.of("dungeon_crypt_floor", "dungeon_moss_floor", "dungeon_rubble_floor",
                "dungeon_torch_floor", "dungeon_boss_sigil_floor")) {
            Path path = Path.of("assets", "terrain", name + ".png");
            require(Files.isRegularFile(path), "Missing reviewed dungeon floor " + path);
            BufferedImage image = ImageIO.read(path.toFile());
            require(image != null && image.getWidth() == 48 && image.getHeight() == 48,
                    "Reviewed dungeon floor must be native 48x48: " + name);
            for (int y = 0; y < image.getHeight(); y++) {
                require(image.getRGB(0, y) == image.getRGB(image.getWidth() - 1, y),
                        "Horizontal seam in " + name + " at row " + y);
            }
            for (int x = 0; x < image.getWidth(); x++) {
                require(image.getRGB(x, 0) == image.getRGB(x, image.getHeight() - 1),
                        "Vertical seam in " + name + " at column " + x);
            }
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                require((argb >>> 24) == 255, "Transparent pixel in reviewed dungeon floor " + name);
                int red = (argb >>> 16) & 255;
                int green = (argb >>> 8) & 255;
                int blue = argb & 255;
                if (x == 0 || y == 0 || x == image.getWidth() - 1 || y == image.getHeight() - 1) {
                    require(Math.min(red, Math.min(green, blue)) < 225,
                            "White/checker edge artifact in reviewed dungeon floor " + name);
                }
            }
        }
    }

    private static void checkWorld(WorldMap world, long seed) {
        boolean foundAscending = false;
        boolean foundDescending = false;
        boolean foundSurfaceBlend = false;
        Set<Integer> siteSignatures = new HashSet<>();
        for (WorldMap.AdventureMarker marker : world.adventureMarkers()) {
            int floors = Math.max(3, Math.min(4, marker.depth() + 2));
            Set<Integer> floorSignatures = new HashSet<>();
            for (int floor = 1; floor <= floors; floor++) {
                String mapId = floorId(marker.mapId(), floor);
                WorldMap.DungeonContext context = world.dungeonContext(mapId);
                require(context != null, "Missing context for " + mapId);
                require(context.floor() == floor && context.floors() == floors, "Wrong storey metadata for " + mapId);
                require(context.region().equals(world.kingdomAt(marker.x(), marker.y()).id()), "Exterior region not carried into " + mapId);
                require(!context.folklore().isBlank(), "Missing folklore identity for " + mapId);
                foundAscending |= context.verticality().equals("ascending");
                foundDescending |= context.verticality().equals("descending");

                MapArea area = world.area(mapId);
                require(area != null && area.width() == 36 && area.height() == 26, "Wrong dungeon dimensions for " + mapId);
                List<TilePoint> transitions = transitionTiles(world, mapId, area);
                require(!transitions.isEmpty(), "Floor has no stair transition: " + mapId);
                Set<TilePoint> reached = flood(world, mapId, transitions.get(0));
                int floorTiles = countFloorTiles(area);
                require(reached.size() == floorTiles, "Disconnected floor in " + mapId + ": " + reached.size() + "/" + floorTiles);
                for (TilePoint transition : transitions) require(reached.contains(transition), "Unreachable stair in " + mapId);
                require(countTile(area, 'L') >= 1, "Floor has no functional light landmark: " + mapId);
                int sigils = countTile(area, 'S');
                require(sigils == (floor == floors && !context.theme().equals("abandoned_castle") ? 1 : 0),
                        "Terminal sigil landmark is misplaced in " + mapId);
                foundSurfaceBlend |= countTile(area, 'M') > 0;

                List<WorldProp> props = world.props(mapId);
                int propLimit = com.alderfall.game.CavernStyle.natural(context.theme())
                        || com.alderfall.game.BuiltDungeonStyle.supports(context.theme()) ? 40 : 28;
                require(props.size() >= 9 && props.size() <= propLimit, "Uncontrolled prop density in " + mapId + ": " + props.size());
                Set<TilePoint> propTiles = new HashSet<>();
                int stairProps = 0;
                for (WorldProp prop : props) {
                    require(propTiles.add(new TilePoint(prop.x(), prop.y())), "Stacked props in " + mapId + " at " + prop.x() + "," + prop.y());
                    if (prop.asset().startsWith("dungeon_prop_stairs_")) stairProps++;
                }
                require(stairProps == transitions.size(), "Stair art and transitions disagree in " + mapId);

                List<WorldMap.DungeonEncounterSlot> encounters = world.dungeonEncounterSlots(mapId);
                require(encounters.size() >= 2, "No encounter formation in " + mapId);
                long bosses = encounters.stream().filter(WorldMap.DungeonEncounterSlot::boss).count();
                require(bosses == (floor == floors ? 1 : 0), "Boss must occur only on terminal floor: " + mapId);
                for (WorldMap.DungeonEncounterSlot encounter : encounters) {
                    TilePoint point = new TilePoint(encounter.x(), encounter.y());
                    require(reached.contains(point), "Encounter outside navigable space in " + mapId);
                    require(world.transitionAt(mapId, point.x(), point.y()) == null, "Encounter blocks stairs in " + mapId);
                    require(world.propsAt(mapId, point.x(), point.y()).isEmpty(), "Encounter overlaps prop in " + mapId);
                }
                floorSignatures.add(tileSignature(area));
            }
            require(floorSignatures.size() == floors, "Repeated floor layout in " + marker.label());
            siteSignatures.add(floorSignatures.hashCode());
        }
        require(foundAscending && foundDescending, "Dungeon set must support both ascent and descent");
        require(foundSurfaceBlend, "Dungeon set must carry at least one exterior material inward");
        require(siteSignatures.size() >= world.adventureMarkers().size() - 1, "Too many sites share the same layout sequence");
        System.out.println("Dungeon generation passed for seed " + seed + " (" + siteSignatures.size() + " site layouts)");
    }

    private static List<TilePoint> transitionTiles(WorldMap world, String mapId, MapArea area) {
        List<TilePoint> result = new ArrayList<>();
        for (int y = 0; y < area.height(); y++) for (int x = 0; x < area.width(); x++) {
            if (world.transitionAt(mapId, x, y) != null) result.add(new TilePoint(x, y));
        }
        return result;
    }

    private static Set<TilePoint> flood(WorldMap world, String mapId, TilePoint start) {
        Set<TilePoint> reached = new HashSet<>();
        ArrayDeque<TilePoint> pending = new ArrayDeque<>();
        reached.add(start);
        pending.add(start);
        while (!pending.isEmpty()) {
            TilePoint point = pending.removeFirst();
            for (int[] direction : DIRECTIONS) {
                TilePoint next = new TilePoint(point.x() + direction[0], point.y() + direction[1]);
                if (reached.add(next) && world.isPassable(mapId, next.x(), next.y())) pending.addLast(next);
            }
        }
        reached.removeIf(point -> !world.isPassable(mapId, point.x(), point.y()));
        return reached;
    }

    private static int countFloorTiles(MapArea area) {
        int count = 0;
        for (int y = 0; y < area.height(); y++) for (int x = 0; x < area.width(); x++) {
            if (DungeonGeneratorTestFloor.isFloor(area.tileAt(x, y))) count++;
        }
        return count;
    }

    private static int countTile(MapArea area, char tile) {
        int count = 0;
        for (int y = 0; y < area.height(); y++) for (int x = 0; x < area.width(); x++) {
            if (area.tileAt(x, y) == tile) count++;
        }
        return count;
    }

    private static int tileSignature(MapArea area) {
        int result = 1;
        for (char[] row : area.tiles) for (char tile : row) result = 31 * result + tile;
        return result;
    }

    private static String floorId(String firstFloor, int floor) {
        return firstFloor.replaceFirst("_\\d+$", "_" + floor);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class DungeonGeneratorTestFloor {
        static boolean isFloor(char tile) {
            return switch (tile) {
                case 'd', 'D', 'F', 'M', 'R', 'S', 'L', 'N', 'E', 'I', 'J', 'H', 'Q',
                        '1', '2', '3', '4', '5', '6' -> true;
                default -> false;
            };
        }
    }
}
