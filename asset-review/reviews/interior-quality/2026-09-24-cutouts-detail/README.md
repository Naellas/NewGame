# Interior asset quality review

Scope: 214 interior PNGs inspected, 44 older props selected for replacement.
`inventory-0.png` through `inventory-5.png` preserve the inspected library before
this refresh; `comparison-N.png` sheets show selected originals and replacements.
The source batch preserves originals, prompts, exact runtime destinations and
checksums in [its manifest and README](../../../../art-source/interior-quality/2026-09-24-cutouts-detail/README.md).

## Reproduce

From the repository root, build the review/test classes:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -IncludeReviews -OutputDirectory temp/interior-art-audit/classes
```

Then from `Java/`:

```powershell
java '-Djava.awt.headless=true' -cp temp/interior-art-audit/classes com.alderfall.game.InteriorAssetReview ../art-source/interior-quality/2026-09-24-cutouts-detail/inputs assets temp/interior-art-audit/comparisons
java '-Djava.awt.headless=true' -cp temp/interior-art-audit/classes com.alderfall.game.InteriorDesignPreview temp/interior-art-audit/captures home
```

The read-only comparison exporter uses the real `AssetStore.spriteFit` path,
identical 160x112 fitting bounds, nearest-neighbor sampling, and a contrasting
background. It accepts arbitrary before/after asset directories and output
directory. The source images are not filtered or edited by the exporter.

## Acceptance criteria

- Whole objects with transparent corners; no painted checkerboard or magenta key.
- No detached fragments inherited from the old sprite sheets.
- Readable wood, metal and fabric details at game-sized display bounds.
- Cardinal chair orientation preserved, including legacy direction conventions.
- Original IDs, paths, collision footprints and connected-module behavior retained.

The full inventory is in the source batch's `audit.csv`. Intentional purple
crystal edges are not contamination. Some legacy architecture sprites still
have edge fringe; the current renderer uses the newer modular wall art. This
review does not claim that every historical or non-interior game asset has
been regenerated.

## Results

Accepted and imported all 44 replacements. All three comparison sheets were
visually inspected. `home-0.png` and `bakery-0.png` are accepted captures through
the actual world renderer; four seeds per theme were exported to scratch.
The initial overly narrow runner candidate was rejected and regenerated; it
remains in the source batch for provenance.

All replacements have alpha backgrounds. Corner alpha is zero except one
herb-planter corner at 1/255 (visually transparent). The trunk's preview glow
is stored in transparent RGB and does not appear in the runtime comparison.
The edge-color audit flags nine intentional purple crystal pixels and zero
magenta edge pixels in the other 43 replacements.

Validation: Java review/test build passed; ConnectedFurnitureTest passed for
26 families at three scales; ModularFurnitureTest passed for 28 cardinal
seating choices and 48 fence masks; InteriorFurnishingsTest passed for 74
editor assets and 324 plans. Review-link/archive and structure checks passed.
InteriorPlacementTest passed for 54 interiors across three seeds and six styles.
The required root check passed 13 policy tests, then stopped on five existing
unapproved-root findings: `.vscode/settings.json` and four files in
`Input - uploads/`. No baseline or ignore changes were made.
