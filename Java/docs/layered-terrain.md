# Continuous overworld terrain

The gameplay grid still controls movement and encounters. Its visual surface now uses continuous material coverage in world coordinates, with small deterministic distortions to break up the square outlines. Only coverage is feathered; the underlying textures remain sharp.

`LayeredTerrainRenderer` combines neighboring terrain materials, adds a narrow wet bank and shallow-water tint, and supplies a matching clip for animated water. Pixels depend on world position, so camera movement, tile draw order, and terrain chunk boundaries cannot shift the pattern. Existing textures are reused; this pass needs no new raster assets.

Goblin and bandit camp regions contribute one irregular dirt clearing with a worn central track. Ground stamps, redundant paving props, and road overlays inside the clearing yield to that shared surface. Other locations retain their existing props. Mountains, upright structures, and the wider road network still use their existing renderers; arbitrary rotation of upright wall sprites is not part of this change.

Composed tiles use a bounded cache and feed the existing terrain chunk cache. Uniform materials have a fast path, and fully wet tiles bypass the complex shoreline clip. Terrain edits in the surrounding neighborhood invalidate cached surfaces and chunks.

Validation from `Java/`:

```powershell
java '-Djava.awt.headless=true' -cp out com.alderfall.game.LayeredTerrainTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.LayeredTerrainPreview
java '-Djava.awt.headless=true' -cp out com.alderfall.game.RenderCacheTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.BridgeRenderingTest
java -cp temp/checks/classes com.alderfall.game.map.WorldGenerationTest
```

The layered test checks water masks at four zoom levels, opaque terrain coverage, tile-center consistency with gameplay, cache reuse, neighbor-edit invalidation, exact pixel agreement between direct/chunk rendering and scrolled views, and unchanged gameplay tiles. The preview writes actual game screenshots of shoreline, snow boundary, and camp to `exports/layered-terrain/`. Preview timings include PNG encoding and are not steady-state frame timings.

## Modular grass and ground cover

Small ground sprites now use `GroundCoverPattern`: a shared, warped patch field
sampled at each sprite's actual world position. Neighboring tiles share dense
centers and sparse edges. Existing eight-sprite biome families form the modules;
center modules grow larger and edge modules taper down. Low background density
leaves visible open ground between patches.

`visualSlot` remains the stable candidate ID for compatibility, but no longer
means a fixed quarter-tile position. Anchors span the tile with deterministic
variation independent of the chosen sprite. Crowded candidates within an owner
tile are skipped. Road/biome buffers, authored locations, player construction
and collision behavior retain their existing rules. Generated cover is rebuilt
with the world; this does not rewrite saves.

Ground details still bake into the terrain chunk cache. The one-tile owner margin
covers cross-boundary sprites, and world-relative anchors prevent scrolling or
zoom from moving them. No per-frame coverage sampling or new bitmap assets.

Validation: `PropPlacementTest`, `GroundDetailBatchTest`, `NatureSpacesTest`,
`SubtileCollisionTest`, `RenderCacheTest` and repository checks.
[Reviewed snow and meadow captures](../../asset-review/reviews/ground-cover/2026-09-24-continuous-patches/README.md).

## Biome surface variants and scale range

`GroundCoverPattern` assigns each small vegetation module one of five stable
scales: 0.58, 0.78, 1.00, 1.24 or 1.52. Common sizes dominate and the largest
bucket has a 12% candidate probability. The root does not move when scale or
artwork changes. The existing one-tile chunk margin still contains the largest
module (28 logical pixels times 1.52, less than a 48-pixel tile).

The terrain compositor now derives three cached palette styles per authored
source: original, lighter/drier and darker/weathered. Meadow, forest and tundra
have 24 combinations each; marsh, badlands and mountain ground have 12; beach has
6; desert has 3. Colors are biome-specific, so snow is never tinted olive.
Every mesh corner samples its texture at a different stable phase/orientation.
A continuous coordinate warp breaks rigid repetition. Interpolation in world
space blends adjacent styles, independently of gameplay tiles and cache chunks.
Water, roads and upright mountain scenery retain their dedicated behavior.

This is a runtime material change, not a new set of authored PNGs. Existing
texture names and paths are preserved; no new sources or assets are scattered
into legacy directories. New review captures use the prescribed review tree.
[Eight-biome and vegetation evidence](../../asset-review/reviews/biome-surfaces/2026-09-24-scale-and-variants/README.md).

## Water depth and weather

Outdoor water now uses shore-derived walkable shallows and wading shelves,
continuous bed color and weather-driven wavelets. See [water](water.md) for
movement rules, renderer details, limitations and visual review.
