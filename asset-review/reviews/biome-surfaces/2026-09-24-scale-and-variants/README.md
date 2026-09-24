# Biome surfaces and vegetation scale

Accepted renderer captures:

- [All eight biome surfaces](biome-surfaces.png): meadow, forest, tundra, desert,
  badlands, marsh, beach and mountain ground. Each panel spans eight by five tiles.
- [Meadow vegetation](groundcover-g.png): mixed small and large modules at 150% zoom.
- [Tundra vegetation](groundcover-n.png): sparse accents and larger frost-grass clusters.

The surface variants are runtime transformations of existing textures, not new
imagegen assets. Original, lighter/drier and darker/weathered palettes combine
with independent texture offsets and orientation per mesh corner. Small seamless
swatches wrap normally; large desert sheets retain mirrored wrapping. A continuous
world-space warp and shared interpolation prevent visible gameplay-tile seams.
Original texture IDs and source files are preserved. Upright props and roads keep
their own renderer; water and shallows retain their existing surface sampling.

Vegetation module scale uses five bounded buckets: .58, .78, 1.00, 1.24 and 1.52.
The largest bucket occurs for about 12% of candidates. Roots remain stable.

Reproduce from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -IncludeReviews -OutputDirectory temp/biome-variation/classes
Set-Location Java
java '-Djava.awt.headless=true' -cp temp/biome-variation/classes com.alderfall.game.LayeredTerrainPreview --biomes temp/biome-variation/captures
java '-Djava.awt.headless=true' -cp temp/biome-variation/classes com.alderfall.game.NatureSpaceReview temp/biome-variation/captures groundcover
```

Passed: build, NaturalTerrainVariantTest, LayeredTerrainTest,
GroundDetailBatchTest, PropPlacementTest, RenderCacheTest and repository checks.
These check asset availability, scale range, stable roots, seam differences,
water masks, opaque pixels, chunk/direct agreement, zoom/scroll stability,
cache invalidation.

Known existing limitation: VillageTerrainTest fails its direct/cached pixel
comparison. Reproduced in both temp/ground-cover/classes (before this change,
first mismatch 0,0) and temp/biome-variation/classes (1,0). Its assertions have
not been weakened or suppressed.

Placement follows repository policy: production changes are under
Java/src/main/java/com/alderfall/game/rendering; tests under src/test/java;
the existing exporter under src/review/java; scratch outputs under Java/temp;
curated captures here. No new runtime PNGs or source art batch is required.
