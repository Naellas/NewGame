# Interior materials, placement and light

Interiors use the originating settlement's identity through `InteriorStyle`. The household accents follow the regional customs in `Story premise/world-framework.md`, with the seedhouse and guest-abbey context described in `hearthlands-worldbuilding.md` and `western-reach-folklore.md`.

| Region | Interior treatment |
| --- | --- |
| Hearthlands | Warm planks, herb stores, seed bowls on available surfaces |
| River and Thorn | Green-toned timber, ivy, journey-tree saplings, guest meals |
| Stormbound Holds | Dark timber walls and floors, winter linen, dried herbs, candles |
| Sunrealm | Pale warm planks and plaster, clay vessels, planted wall shelves |
| Belltower Fenlands | Shaded green timber, reed pots, dried herbs and mortar bowls |
| Archive households | Parquet, shelves, wide windows and desk candles |

These are furnishing and material variations on the existing building layouts, using existing pixel art. They do not introduce new cults or quest mechanics. The current mainland settlement catalog has no Lantern Isles settlement interiors; a future Isles profile should be authored against that content rather than treating the Fenlands as the Isles.

Furniture drawing and editor previews use the same top-left, multi-tile footprint as placement. Sprites preserve their proportions, tabletop details require a supporting surface, and collision reserves the full width of counters.

`InteriorLayout` composes the architecture, furnishing groups, circulation and resident positions as one plan. It replaces the old individual-prop scoring and density target. Homes have a private sleeping alcove, cooking wall, dining group, hearth seating and household work corner. Inns have a service area with staff behind the counter, dining groups, a hearth lounge, a welcome bench and two enclosed guest rooms. Taverns keep a private keeper's room and stores; shops, bakehouses, libraries and workshops each have their own work-to-service arrangement. A two-tile route stays clear from the threshold into the interior. Regional variations belong to these activities, including communal northern dining and local objects on household work surfaces.

All 17 building uses now have fuller activity groups: washstands and clothing storage in sleeping rooms, serving supplies near dining areas, tools and stock beside workstations, and reading materials on supported surfaces. Remembrance halls separate family records, public testimony, consultations and memorial offerings; granaries and smokehouses distinguish reserve bays from public tally and distribution areas. Additional furniture uses the existing asset catalog and retains each region's materials and customary objects.

Seeded footprints include the original shell, projecting entrance vestibules, and left or right annexes. Annex doors require clear space on the existing room's side; each annex has a closed perimeter, local window and lamp, and furnishings for its use. A bedroom extension becomes a dressing room; other annexes hold records, provisions or equipment. If neither doorway clearance nor room size permits the selected wing, the plan uses a vestibule. The centered southern exit is retained. Player-village buildings retain their original footprint and coordinates for saved construction edits.

Rug coverage is retained separately from furniture collision. Adjacent rug tiles share their field and draw borders only at the edge of the group, so a seating area has one continuous rug and furniture does not erase the material beneath it.

Side walls have continuous timber caps. Dark void replaces the repeated framed void asset, rear walls no longer repeat a second facade, door openings show the floor through their transparent pixels, and floor-side contact shading survives terrain chunk rendering.

Interior lights remain visible during daytime, windows contribute cooler daylight, and the player no longer emits an invisible indoor lantern. Light visibility is cached by room geometry and source position, with invalidation on wall edits and zoom. Solid walls stop spill; doorways transmit it. Furniture casts a short silhouette away from the strongest visible local emitter, clipped to floor space, with a separate contact shadow. This is a Java2D approximation; it does not simulate reflected light or accumulate shadows from every emitter. Quality settings still control caster shadows and glow.

Fire uses the flames already painted into the assets with small animated embers and warm flicker. The old triangle overlay has been removed. Cold preparation counters and fireflies no longer count as flame sources.

## Checks and captures

From `Java/`, compile all Java sources into a separate output folder, then run:

```powershell
$sources = @(rg --files src/main/java -g '*.java')
javac -d out-interior-check $sources
java '-Djava.awt.headless=true' -cp out-interior-check com.alderfall.game.InteriorPlacementTest
java '-Djava.awt.headless=true' -cp out-interior-check com.alderfall.game.InteriorLayoutTest
java '-Djava.awt.headless=true' -cp out-interior-check com.alderfall.game.InteriorFurnishingsTest
java '-Djava.awt.headless=true' -cp out-interior-check com.alderfall.game.RegionalBuildingsTest
java '-Djava.awt.headless=true' -cp out-interior-check com.alderfall.game.render.world.InteriorLightTest
java '-Djava.awt.headless=true' -cp out-interior-check com.alderfall.game.RenderCacheTest
java '-Djava.awt.headless=true' -cp out-interior-check com.alderfall.game.InteriorRenderTest exports/interiors --benchmark
java '-Djava.awt.headless=true' -cp out-interior-check com.alderfall.game.InteriorDesignPreview exports/interior-designs
```

Placement checks cover 54 generated interiors, three seeds and six regional styles. Light checks cover walls, open doors, cached visibility and invalidation at four tile scales. The render diagnostic captures six locations at noon and 23:00 through the actual game renderer. `--benchmark` measures 30 software-rendered frames after 10 warmup frames per scene; it does not measure display presentation or simulation. Captures live in `Java/exports/interiors/` and are ignored build/review output.

Composition checks cover 1,530 plans across 17 building uses, six regional styles and 15 seeds, including negative seeds. They flood the exterior with only the actual exits sealed to detect wall gaps, and verify that all walkable floor is reachable before navigation repair. They also reject omitted props, furniture extending outside its functional zone, blocked circulation or resident positions, beds outside private zones, unpaired dining benches, rug holes and any layout that navigation repair has to dismantle. The current sample produces 39 distinct outlines. `InteriorDesignPreview` captures all 17 uses at four footprint seeds through the actual world renderer with a viewport sized to show the shell, without the HUD; files are in `Java/exports/interior-designs/`. Pass a building theme as its second argument to capture just that use.

The September 22 validation passed the full compilation, both interior checks, render cache checks, and Western Reach checks. Broader validation encountered a road-sign objective count assertion in `SmokeTest` and an Elderford outdoor prop-count assertion in `HearthlandsWorldTest`; these suites were not green. Interior software-render medians in the local capture run were approximately 28–33 ms, with p95 approximately 37–48 ms.
