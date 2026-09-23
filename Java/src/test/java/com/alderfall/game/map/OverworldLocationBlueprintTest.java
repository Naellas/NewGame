package com.alderfall.game.map;

import java.util.List;

public final class OverworldLocationBlueprintTest {
    private OverworldLocationBlueprintTest() { }

    public static void main(String[] args) {
        for (String kind : List.of("goblin_camp", "bandit_camp", "cave", "crypt", "graveyard",
                "abandoned_castle", "prison", "sewer")) {
            OverworldLocationBlueprints.Blueprint blueprint = OverworldLocationBlueprints.forKind(kind);
            require(blueprint != null, "missing blueprint for " + kind);
            require(!blueprint.slots().isEmpty(), "blueprint has no authored slots: " + kind);
            long structures = blueprint.slots().stream()
                    .filter(slot -> slot.role() == OverworldLocationBlueprints.SlotRole.STRUCTURE).count();
            if (!kind.equals("crypt") && !kind.equals("graveyard") && !kind.equals("sewer")) {
                require(structures == 1, "expected exactly one entrance structure for " + kind);
            }
            for (OverworldLocationBlueprints.Slot slot : blueprint.slots()) {
                require(!slot.assets().isEmpty(), "empty asset choice in " + kind);
                require(Math.abs(slot.dx()) <= 4 && Math.abs(slot.dy()) <= 4,
                        "slot outside minimum supported footprint in " + kind);
            }
        }
        var fortress = OverworldLocationBlueprints.forKind("abandoned_castle");
        require(!fortress.hasPerimeter(), "fortress must use its connected wall asset, not loose edge walls");
        require(fortress.slots().stream().anyMatch(slot ->
                        slot.assets().contains("location_dungeon_fortress_gate_imagegen")),
                "fortress gate asset missing");
        var mirrored = OverworldLocationBlueprints.forKind("goblin_camp", 1);
        var normal = OverworldLocationBlueprints.forKind("goblin_camp", 0);
        require(normal.slots().get(1).dx() == -mirrored.slots().get(1).dx(),
                "seeded blueprint mirroring did not transform authored slots");
        WorldMap world = new WorldMap(0L);
        for (WorldMap.AdventureMarker marker : world.adventureMarkers()) {
            long sitesAtEntrance = world.locationSites(marker.kind()).stream()
                    .filter(site -> site.x() == marker.x() && site.y() == marker.y()).count();
            require(sitesAtEntrance == 1, "stacked location patches at " + marker.label());
        }
        System.out.println("OverworldLocationBlueprintTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
