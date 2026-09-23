# Overworld location blueprints

Regional dungeon placement, the full playable roster, and surface art review are documented in [Dungeon exteriors](dungeon-exteriors.md). `forSite` specializes the base blueprint with local terrain and dungeon intent; playable entrances maintain a minimum 28-tile separation.

Camps and dungeon approaches are assembled as complete authored instances rather than independent prop rolls.

`OverworldLocationBlueprints` defines, per location kind:

- a primary structure aligned to the gameplay entrance;
- an optional connected perimeter with a reserved south gate;
- fixed activity and landmark slots;
- low-priority side dressing and interior scatter.

`WorldMap.addBlueprintLocationProps` validates every slot against terrain, roads, transitions, existing props, and the entrance reserve. A primary adventure site already receives its structure on the dungeon tile, so blueprint instantiation skips only that duplicate slot. Non-primary story sites receive the same complete structure from the blueprint.

Each blueprint instance is mirrored deterministically from its location seed. This varies camp activity sides and ruin dressing without breaking the authored gate, perimeter, or structure relationships.

When an adventure site claims coordinates previously held by a generic procedural scaffold, the scaffold is replaced. This prevents two independently dressed patches—such as a graveyard and a fortress—from occupying the same site.

Every blueprint also contributes one continuous blended ground footprint. The layered terrain renderer replaces the old square dirt/path stamps with a feathered clearing and a worn route through its gate, while leaving gameplay terrain and movement unchanged.

The older structured generators remain as fallbacks for location kinds without a blueprint. To add a new ready-made camp or dungeon instance, add a `Blueprint` entry rather than another placement loop.

Fortified sites deliberately have no loose wall perimeter: `location_dungeon_fortress_gate_imagegen` contains connected thick walls, towers, and the open walkable gate in one correctly scaled asset. Camps and grave sites retain modular perimeter pieces because their palisades and fences already have directional variants.
