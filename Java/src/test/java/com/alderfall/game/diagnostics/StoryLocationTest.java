package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Checks the catalog against real maps and the terrain reachable from Oathstead. */
public final class StoryLocationTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Set<String> identities = new HashSet<>();
        Set<String> questBindings = new HashSet<>();
        for (var place : StoryLocationCatalog.PLACES) {
            check(identities.add(place.id()), "Duplicate place identity: " + place.id());
            for (String quest : place.quests()) check(questBindings.add(quest), "Ambiguous quest destination: " + quest);
        }
        check(questBindings.equals(GameData.MAIN_STORY_QUEST_IDS), "Campaign has an unnamed or missing destination");
        WorldMap reference = null;
        for (long seed : new long[]{0, 1, 42, 1024, 2026, -17}) {
            System.out.println("Checking campaign sites for seed " + seed);
            WorldMap world = new WorldMap(seed);
            if (seed == 0) reference = world;
            Set<TilePoint> reachable = reachable(world);
            check(world.campaignMarkers().size() == 18, "Missing permanent map icons");
            for (var place : StoryLocationCatalog.PLACES) {
                check(world.hasMap(place.mapId()), "Catalog points to a missing map: " + place.name());
                if (!place.outdoorSite()) continue;
                check(world.area("overworld").landmarks.containsValue(place.name()), "Missing persistent map label: " + place.name());
                var marker = world.campaignMarkers().stream().filter(m -> m.id().equals(place.id())).findFirst().orElseThrow();
                check(marker.kind().equals(place.template()), "Wrong generator template: " + place.name());
                var props = world.area("overworld").propsInBounds(marker.x() - marker.radiusX(), marker.y() - marker.radiusY(),
                        marker.x() + marker.radiusX(), marker.y() + marker.radiusY());
                check(props.size() >= 5, "Place has no actual layout: " + place.name());
                String signature = switch (place.template()) {
                    case "bandit_camp", "goblin_camp", "caravan_halt", "tollhouse" -> "location_camp_crates";
                    case "guest_shrine" -> "village_prop_anvil_stump";
                    case "bell_landing", "ruined_watchpost" -> "deco_crossing_flood_bell";
                    case "reed_beds" -> "deco_soft_water_reeds_gold";
                    case "orchard" -> "deco_tree_fruit_harvestable";
                    case "graveyard", "crypt" -> "location_graveyard_tombstones";
                    default -> "";
                };
                check(signature.isEmpty() || props.stream().anyMatch(p -> p.asset().equals(signature)), "Missing " + signature + " at " + place.name());
                if (!place.adventureId().isBlank()) {
                    var entrance = world.adventureMarkers().stream().filter(a -> a.mapId().equals(place.adventureId())).findFirst().orElseThrow();
                    check(marker.x() == entrance.x() && marker.y() == entrance.y(), "Duplicate/moved adventure entrance: " + place.name());
                    check(world.transitionAt("overworld", marker.x(), marker.y()) != null, "Entrance no longer enters a dungeon");
                }
                Set<TilePoint> unique = new HashSet<>();
                for (int variant = 0; variant < 8; variant++) {
                    TilePoint point = world.campaignPlacePoint(place.id(), variant);
                    check(unique.add(point), "Overlapping inspection points: " + place.name());
                    check(world.campaignPlaceContains(place.id(), point.x(), point.y()), "Objective left its site: " + place.name());
                    check(world.isPassable("overworld", point.x(), point.y()), "Blocked objective: " + place.name());
                    check(world.transitionAt("overworld", point.x(), point.y()) == null, "Objective overlaps an entrance: " + place.name());
                    check(reachable.contains(point), "No walkable route from Oathstead: " + place.name() + " seed=" + seed);
                }
                for (String id : place.quests()) {
                    Quest q = GameData.QUESTS.get(id);
                    for (var stage : q.stages) {
                        check(stage.objectiveLocationKind().equals("place:" + place.id()), "Quest still uses a generic location index: " + id);
                        check(stage.progressDialog().contains(place.name()), "Instructions omit the named destination: " + id);
                    }
                }
            }
            for (var a : world.campaignMarkers()) for (var b : world.campaignMarkers()) {
                // The forge is a wing of the shrine's footprint, not a second six-tile-radius building.
                if (a.id().equals(b.id()) || a.id().equals("sanctum_forge") || b.id().equals("sanctum_forge")) continue;
                check(Math.abs(a.x() - b.x()) > a.radiusX() + b.radiusX()
                                || Math.abs(a.y() - b.y()) > a.radiusY() + b.radiusY(),
                        "Named sites overlap: " + a.label() + " / " + b.label());
            }
        }
        WorldMap repeat = new WorldMap(0);
        check(reference.campaignMarkers().equals(repeat.campaignMarkers()), "Map labels move when regenerating a save");
        check(reference.props("overworld").equals(repeat.props("overworld")), "Site furniture changes when regenerating a save");
        export(Path.of(args.length == 0 ? "out-story-refinement/location-library.md" : args[0]), reference);
        System.out.println("Story location checks passed: " + checks);
    }

    private static Set<TilePoint> reachable(WorldMap world) {
        Set<TilePoint> seen = new HashSet<>();
        ArrayDeque<TilePoint> pending = new ArrayDeque<>();
        pending.add(WorldMap.START_POSITION);
        seen.add(WorldMap.START_POSITION);
        while (!pending.isEmpty()) {
            TilePoint p = pending.removeFirst();
            for (int[] direction : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                TilePoint next = new TilePoint(p.x() + direction[0], p.y() + direction[1]);
                if (next.x() >= 0 && next.y() >= 0 && next.x() < WorldMap.COLS && next.y() < WorldMap.ROWS
                        && !seen.contains(next) && world.isPassable("overworld", next.x(), next.y())) {
                    seen.add(next);
                    pending.add(next);
                }
            }
        }
        return seen;
    }

    private static void export(Path path, WorldMap reference) throws Exception {
        StringBuilder out = new StringBuilder("# Alderfall location library\n\n"
                + "Generated from `StoryLocationCatalog` by `StoryLocationTest`. This is the current location reference for the 26 main quests "
                + "and their supporting settlements. Use these names in offers, journal entries, directions, and map labels. "
                + "A location's identity and regional anchor are fixed; its exact walkable tile can adjust to generated terrain.\n\n"
                + "The 18 outdoor destinations have generated surface layouts, road approaches, permanent world-map icons, hover descriptions, and named quest interactions. "
                + "Map labels avoid one another; zoom or hover over an icon to read a crowded place's name. "
                + "Camp, graveyard, and watchpost layouts reuse the existing location templates. Orchard, reed-bed, landing, caravan, tollhouse, and guest-shrine layouts use their placement rules and existing assets. "
                + "The shrine and forge occupy separate wings of one compound. Only locations described as existing entrances have a separate accessible map. "
                + "The existing shrine/vault interiors and settlement maps remain in use.\n\n"
                + "Read with [the dialogue clarity rules](dialogue-clarity-and-place-review.md), "
                + "[the story bible](world-framework.md), and [the actual campaign dialogue](../Java/docs/main-story-dialogue.md).\n\n");
        for (var place : StoryLocationCatalog.PLACES) {
            out.append("## ").append(place.name()).append("\n\n")
                    .append("Identity: `").append(place.id()).append("`. Region: ").append(place.region()).append(".\n\n")
                    .append("**What it is:** ").append(place.purpose()).append("\n\n")
                    .append("**Who matters here:** ").append(place.people()).append("\n\n")
                    .append("**How the player finds it:** ").append(place.route()).append("\n\n")
                    .append("**What the player can recognize:** ").append(place.landmark()).append("\n\n")
                    .append("Game map: `").append(place.mapId()).append("`.");
            if (place.outdoorSite()) out.append(" Preferred world anchor: (").append(place.x()).append(", ").append(place.y()).append(").");
            if (place.outdoorSite()) {
                var marker = reference.campaignMarkers().stream().filter(m -> m.id().equals(place.id())).findFirst().orElseThrow();
                out.append(" Layout: `").append(place.template()).append("`. Seed-0 map marker: (")
                        .append(marker.x()).append(", ").append(marker.y()).append(").");
            }
            if (!place.adventureId().isBlank()) out.append(" Existing adventure entrance: `").append(place.adventureId()).append("`.");
            if (!place.quests().isEmpty()) out.append(" Quest bindings: ").append(String.join(", ", place.quests()));
            out.append("\n\n");
        }
        out.append("## Referenced places beyond this catalog\n\n"
                + "The Lantern Isles, individual Briar Court halls such as Thorn Hall, the complete Hollow Throne exploration map, "
                + "and future folklore-boss lairs are setting references or design targets. Do not give directions into them as if their proposed "
                + "interiors and encounters already exist. The current Old Gate starts Vaelthara's battle directly. "
                + "Later companion objectives still using numbered generic sites require individual migration to named places; this catalog does not silently relocate those stages.\n");
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.writeString(path, out);
    }

    private static void check(boolean pass, String message) {
        checks++;
        if (!pass) throw new AssertionError(message);
    }
}
