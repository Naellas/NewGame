# Continuous ground-cover patches

Reviewed actual Swing captures at 150% zoom:

- [Snow and tundra](groundcover-n.png), world position 262,37.
- [Meadow](groundcover-g.png), world position 182,322.

Accepted: irregularly spaced modules follow shared patches with full centers,
smaller edge clumps and open areas. Existing eight-sprite biome families supply
the modules. No new raster asset was generated. Larger plants keep their existing
placement; this pass changes the small visual-only ground cover.

Reproduce from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -IncludeReviews -OutputDirectory temp/ground-cover/classes
Set-Location Java
java '-Djava.awt.headless=true' -cp temp/ground-cover/classes com.alderfall.game.NatureSpaceReview temp/ground-cover/captures groundcover
```

The review chooses the most densely populated sample of snow and meadow to make
repetition easy to inspect. Normal world generation supplies all objects. It does
not write saves. The shared field is deterministic and evaluated at each module's
actual anchor; changes become visible when a world is reconstructed.

Validation passed: PropPlacementTest, GroundDetailBatchTest, NatureSpacesTest,
SubtileCollisionTest and RenderCacheTest. Coverage includes continuous density
across tile/cell boundaries, varied sub-tile placement, stable roots when changing
sprite variants, three zooms, three camera offsets, exact direct/cached pixel
agreement, cache removal invalidation, and non-blocking ground details.
