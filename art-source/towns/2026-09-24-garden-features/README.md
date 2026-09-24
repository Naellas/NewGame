# Garden entrances and regional fountains

Original input: `gates-and-fountains.png`, generated with the built-in imagegen
tool on September 24, 2026. The exact prompt is in `prompt.txt`; no external
reference art was used. Import preserves the generated RGBA pixels, trimming
transparent padding without repainting or color keying.

From the repository root (Windows PowerShell and System.Drawing):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File art-source/towns/2026-09-24-exterior-modules/import.ps1 -Sheet art-source/towns/2026-09-24-garden-features/gates-and-fountains.png -Recipe art-source/towns/2026-09-24-garden-features/import.json
```

The recipe overwrites six PNGs in
`Java/assets/environments/settlements/city/props/garden/`:
`town_garden_gate_arbor`, `town_garden_gate_iron`, `town_garden_gate_hedge`,
`town_fountain_civic`, `town_fountain_north`, `town_fountain_sun`.
Gate sprites span three walkable cells; fountain basins occupy two by two cells.
These are static sprites, without new water animation.

Review status: atlas and actual renderer inspected by agent; user aesthetic
acceptance pending. Collision and connection diagnostics are described in
[the review](../../../asset-review/reviews/town-layouts/2026-09-24-connected-gardens/README.md).
