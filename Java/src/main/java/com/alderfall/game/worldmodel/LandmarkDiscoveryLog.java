package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class LandmarkDiscoveryLog {
    private static final int DISCOVERY_RADIUS = 4;

    private final Set<String> discoveredKeys = new HashSet<>();

    String discover(GameState state) {
        if (state == null) {
            return "";
        }
        var area = state.world.area(state.currentMapId);
        if (area != null) {
            for (RegionalSettlementIdentity.District district : area.districts) {
                if (manhattan(state.playerX, state.playerY, district.center().x(), district.center().y()) <= 3
                        && discoveredKeys.add(state.currentMapId + ":district:" + district.name())) {
                    return district.name() + ". " + district.dialogue().get(0);
                }
            }
        }
        List<WorldProp> nearby = state.world.propsInBounds(
                state.currentMapId,
                state.playerX - DISCOVERY_RADIUS,
                state.playerY - DISCOVERY_RADIUS,
                state.playerX + DISCOVERY_RADIUS + 1,
                state.playerY + DISCOVERY_RADIUS + 1
        ).stream()
                .filter(prop -> !WesternReachFolklore.observation(state.currentMapId, prop.asset()).isBlank()
                        || !HearthlandsFolklore.observation(state.currentMapId, prop).isBlank()
                        || (WorldMap.OVERWORLD_ID.equals(state.currentMapId) && isDiscoverable(prop)))
                .filter(prop -> manhattan(state.playerX, state.playerY, prop.x(), prop.y()) <= DISCOVERY_RADIUS)
                .sorted(Comparator
                        .comparingInt((WorldProp prop) -> manhattan(state.playerX, state.playerY, prop.x(), prop.y()))
                        .thenComparingInt(WorldProp::y)
                        .thenComparingInt(WorldProp::x)
                        .thenComparing(WorldProp::asset))
                .toList();
        for (WorldProp prop : nearby) {
            String folklore = WesternReachFolklore.observation(state.currentMapId, prop.asset());
            if (folklore.isBlank()) folklore = HearthlandsFolklore.observation(state.currentMapId, prop);
            if (!folklore.isBlank() && discoveredKeys.add(state.currentMapId + ":folklore:" + prop.asset())) {
                return folklore;
            }
            if (!folklore.isBlank()) {
                continue;
            }
            String key = state.currentMapId + ":" + prop.x() + ":" + prop.y() + ":" + prop.asset();
            if (discoveredKeys.add(key)) {
                return lineFor(state, prop);
            }
        }
        return "";
    }

    private String lineFor(GameState state, WorldProp prop) {
        String asset = prop.asset();
        String place = placeNameNear(state, prop.x(), prop.y());
        if (asset.contains("camp_fire")) {
            return place.isBlank()
                    ? "Smoke threads upward from a campfire just beyond the road."
                    : "Smoke from " + place + " lifts in a thin, watchful column.";
        }
        if (asset.contains("road_camp")) {
            return place.isBlank()
                    ? "A rough camp shows through the grass, close enough to make the road feel watched."
                    : "Canvas and ash mark the edge of " + place + ".";
        }
        if (asset.contains("signpost")) {
            return place.isBlank()
                    ? "A weathered signpost leans toward a half-forgotten turn."
                    : "A weathered signpost points toward " + place + ".";
        }
        if (asset.contains("milestone")) {
            return place.isBlank()
                    ? "An old milestone rises from the weeds, its numbers almost gone."
                    : "A worn milestone names the road to " + place + ".";
        }
        if (asset.contains("shrine") || asset.contains("rune")) {
            return place.isBlank()
                    ? "A low shrine-glow pulses between stone and root."
                    : "A green shrine-glow marks the outskirts of " + place + ".";
        }
        if (asset.contains("cairn") || asset.contains("stone_stack") || asset.contains("standing_stones")) {
            return place.isBlank()
                    ? "Old stones stand where someone needed the road to remember."
                    : "Old stones mark the approach to " + place + ".";
        }
        if (asset.contains("elder")) {
            return place.isBlank()
                    ? "An elder tree stands apart, too deliberate to be ordinary."
                    : "An elder tree watches the road near " + place + ".";
        }
        return place.isBlank()
                ? "A landmark breaks the pattern of the wilds."
                : "The shape of " + place + " resolves through the trees.";
    }

    private String placeNameNear(GameState state, int x, int y) {
        for (int radius = 0; radius <= 7; radius++) {
            for (int yy = y - radius; yy <= y + radius; yy++) {
                for (int xx = x - radius; xx <= x + radius; xx++) {
                    if (Math.abs(xx - x) + Math.abs(yy - y) > radius) {
                        continue;
                    }
                    String landmark = state.world.landmarkAt(state.currentMapId, xx, yy);
                    if (landmark != null && !landmark.isBlank()) {
                        return landmark;
                    }
                }
            }
        }
        return "";
    }

    private boolean isDiscoverable(WorldProp prop) {
        if (prop == null || prop.asset() == null) {
            return false;
        }
        String asset = prop.asset();
        return asset.equals("deco_imagen_signpost")
                || asset.equals("deco_imagen_milestone")
                || asset.equals("deco_imagen_road_camp")
                || asset.equals("deco_imagen_shrine_stone")
                || asset.equals("deco_forest_shrine_stone")
                || asset.equals("deco_imagen_green_rune_stone")
                || asset.equals("deco_imagen_tundra_rune_stone")
                || asset.equals("deco_imagen_stone_stack")
                || asset.equals("deco_mountain_cairn")
                || asset.equals("deco_mountain_pass_way_cairn")
                || asset.equals("location_camp_fire")
                || asset.equals("location_ruin_standing_stones")
                || asset.equals("deco_tree_elder_harvestable");
    }

    private static int manhattan(int ax, int ay, int bx, int by) {
        return Math.abs(ax - bx) + Math.abs(ay - by);
    }
}
