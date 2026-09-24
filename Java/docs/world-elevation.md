# Geographic elevation

Generated outdoor maps now use a deterministic terrain-height field: the
overworld, villages and cities. Interior/cavern floors and authored editor maps
keep their existing geometry. Restart the game to rebuild cached maps. No
save-format migration or asset regeneration is needed.

## Geography and travel

Height follows the seed, biome, political region, water proximity and authored
infrastructure. Meadows have rolling shelves, forests stronger rooted banks,
northern regions higher ridges, and Sunrealm badlands pronounced terraces.
Fenlands have occasional low hummocks; wet ground and beaches stay low. Normal
height increments are one quarter level, using interpolated contours.

Generated mountains form broad connected terraces, with their shape derived
from the mountain range rather than independent wave peaks. The outer foothills
and middle shelves are walkable. Full-height scarps and steep summit cores remain
blocked; sparse peak silhouettes mark those cores. Later authored mountain/wall
tiles do not automatically become foothills.

Deliberately planned hiking ramps connect existing roads and passes to upper
terraces. Routes avoid water, solid scenery and building collision; their grade
blends continuously into the stepped ground. Tests verify every route segment
with actual swept collision in both directions. Seeds 0, 42 and 2026 generate
22, 16 and 19 clear mountain routes respectively. Their layout is fixed before
save restoration or harvesting, so clearing scenery cannot move an established
ramp out from under a character. These are generated paths, not
changes to existing road destinations or save IDs.

Roads, passes, bridges, doors, building foundations, map edges and authored
interaction aprons retain level ground. Terrain grades down toward them through
small steps. Rivers/lakes retain their original datum. This does not introduce
waterfalls, elevated lakes or stacked bridge/tunnel geometry.

`PropCollision.canTravel` checks every crossed elevation interval alongside its
existing swept collision. Quarter-height steps work in both directions; larger
jumps block travel. Player free movement, click paths, NPCs and followers using
that shared collision path share the rule. Existing passable cardinal ground
connections and generated destination connectivity are checked by diagnostics.

## Rendering and bank materials

Runtime uses **ground-plane contour relief**: a bank face occupies a short band
below its height boundary. Actors, camera coordinates and interaction boxes
retain their established ground anchors. It does not apply the isolated study's
whole-surface vertical projection to gameplay. This keeps roads, water masks,
buildings, shadows and clicks aligned; traversal uses the shared height field.

Faces have world-aligned procedural grain, strata, fractures, roots, caps and
contact shadows. They are baked into the existing terrain cache, with a halo
that continues faces across tiles and chunks. No new runtime PNGs are required.

| Location | Bank treatment |
| --- | --- |
| Meadow/farmland | Warm earth with turf lip |
| Woodland | Dark rooted soil |
| Temperate mountains/passes | Fractured granite |
| Northern mountains/snow | Cold rock with broken frost cap |
| Sand/Sunrealm mountains | Layered sandstone |
| Badlands | Red shale |
| Marsh | Wet peat |
| Beach | Pale coastal sediment/stone |
| Near paved city approaches | Dressed retaining masonry |

Mountain climate uses the actual political region; other surfaces select their
physical biome material. Settlement relief is gentler and reserves foundations.

## Implementation and review

`TerrainElevation` owns the seed field and visual-revision cache. `WorldMap`
exposes height and foothill passability/labels. `TerrainReliefRenderer` runs in
`LayeredTerrainRenderer`; `ElevationMaterial` supplies bank pixels. The earlier
`ElevationStudy` remains a development fixture sharing the quarter-step limit.

[Regional gallery](../tools/reviews/elevation/index.html).
[Reproduction](../../asset-review/reviews/elevation/2026-09-24-world/README.md).
For development comparisons, `-Dalderfall.disableElevation=true` disables the
field/foothill expansion at JVM startup. This is not a saved player setting.

## Validation

- WorldElevationTest: three seeds, repeatability, coast/bridge datum, protected
  roads/foundations, shelves, rims/cores, existing ground edges, actual mountain-ramp collision and edit invalidation.
- WorldGenerationTest: destination connectivity across five seeds.
- SubtileCollisionTest, WaterDepthTest, LayeredTerrainTest, RenderCacheTest:
  collision, immersion, four zooms, cache/chunk equivalence and edits.
- TownNpcNavigationTest: 1,010 simulated safe NPC steps.

RegionalSettlementTest --layout-only stops at Briarbridge wall `(50,61)`
(degree 1, neighbors `x8gg`). It fails identically with elevation disabled.
Repository hygiene reports the existing `.vscode/settings.json` violation.
Neither baseline was weakened.

A warmed 60-frame software benchmark of the refined terrain measured overworld
mean 55.30 ms, median 54.91 ms and p95 60.37 ms; village mean 19.36 ms, median
18.77 ms and p95 22.51 ms. The earlier elevation-disabled comparison measured
66.27 / 64.98 / 79.95 ms for the overworld. These short runs are diagnostic,
exclude display/GPU timing and cold cache generation, and do not establish a
precise performance bound.
