# Cardinal furniture and modular marketplace — 2026-09-24

Generated with the built-in Imagegen tool. `manifest.json` records exact prompts,
original generation locations, preserved source filenames and runtime `asset_file`
destinations. `inputs/` preserves the four existing furniture references. The
market stall was generated from its prompt. `profile-corrections.json` records
the strict armchair profile corrections; rejected angled candidates remain here
for provenance and are not loaded by the game. `bench-cleanup.json` records the
additional transparent south-facing rustic bench and its destination.

Import from repository root (byte-for-byte PNG copy, alpha retained):

```powershell
$batch = 'art-source/modular-marketplace/2026-09-24-cardinal-furniture'
foreach ($entry in (Get-Content "$batch/manifest.json" -Raw | ConvertFrom-Json)) {
    Copy-Item -LiteralPath "$batch/$($entry.source)" -Destination $entry.asset_file
}
$entry = Get-Content "$batch/bench-cleanup.json" -Raw | ConvertFrom-Json
Copy-Item -LiteralPath "$batch/$($entry.source)" -Destination $entry.asset_file
```

Runtime originals live under environments/interiors/props/modular_seating and
environments/settlements/city/props/{seating,marketplace}. No runtime source folder.
Directions mean the direction the seated occupant faces. Historical source files
named `*_south.png` in this batch are rear views imported as `*_north` according to
the manifest. Existing legacy IDs remain valid. Symmetric backless benches reuse
their horizontal or vertical model. Canopy blue, green and gold are cached runtime
tints of the canvas model; connected end/middle variants are composed at runtime.

Review: accepted cardinal silhouettes and transparency at game scale, including
the corrected east/west armchairs and rustic south bench. See
[review evidence](../../../asset-review/reviews/modular-furniture/2026-09-24-cardinal/README.md).
