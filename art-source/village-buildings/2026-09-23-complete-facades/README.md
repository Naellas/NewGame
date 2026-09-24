# Complete village building facades

Generated on 2026-09-23 using the built-in image generation tool, one edit per
building. The user reported the workshop, cottage, and red-roofed longhouse
cut off at ground level. Inspection confirmed the runtime PNGs themselves
ended through the lower facade and doors.

## Inputs and prompts

`inputs/` preserves the three original runtime PNGs used as edit targets.
`generated/` contains the unmodified generated RGBA PNGs. Exact requests are
recorded in [prompts.md](prompts.md). The edits preserve each building's roof,
materials, decorative identity, and frontal view while reconstructing the
missing doors, thresholds, posts, and foundations.

## Import and output IDs

Run from the repository root in PowerShell (overwrites only these three assets):

```powershell
$batch = 'art-source/village-buildings/2026-09-23-complete-facades'
$runtime = 'Java/assets/environments/settlements/city/buildings/village'
foreach ($id in @('village_building_workshop', 'imagegen_city_cottage', 'imagegen_city_row')) {
    Copy-Item -LiteralPath "$batch/generated/$id.png" -Destination "$runtime/$id.png"
}
```

No pixel processing or background removal is required. The renderer fits the
sprites to its existing building dimensions. Runtime filenames and consumers
remain the same. The historical `slice_imagegen_village_assets` command still
reconstructs the older sheet assets; reapply this import after using it.

## Review and validation

Visually reviewed all three generated images: full doors and foundations are
present, and no visible building geometry touches the canvas edge. Accepted as
runtime replacements; in-game visual confirmation remains pending.

Pillow verified all PNG streams, RGBA mode and alpha range 0–255. Visible bounds
(alpha > 32), in left/top/right/bottom pixel coordinates:

| Asset ID | Canvas | Visible bounds |
| --- | --- | --- |
| village_building_workshop | 1279 x 1230 | 136, 105, 1133, 1121 |
| imagegen_city_cottage | 1203 x 1308 | 174, 101, 1031, 1202 |
| imagegen_city_row | 1329 x 1183 | 85, 36, 1241, 1128 |

The cottage and longhouse have negligible alpha-1 pixels at the canvas edge;
generated alpha is preserved verbatim. The visible silhouettes have clear
margins. No Java behavior changed.

Validation: `powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/check.ps1`
passed all 13 policy tests, hygiene, review-link/archive-integrity, and structure
checks, with 3,483 runtime asset IDs retained.
