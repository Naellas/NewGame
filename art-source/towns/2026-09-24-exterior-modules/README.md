# Exterior hedge and fence modules

Original inputs: built-in imagegen, September 24, 2026, requested for the town
exterior furnishing pass. `boundaries-original.png` is the first generation;
`boundaries-clean.png` is the selected background-extraction edit of that image.
Exact instructions are recorded in `prompts.txt`. No external reference art was
used. The generated RGBA alpha is preserved by import; hidden RGB background
colors are not opaque scenery. No color key, repainting or synthetic artwork is
introduced by the importer.

Import from repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File art-source/towns/2026-09-24-exterior-modules/import.ps1
```

Requires Windows PowerShell and System.Drawing. The recipe accepts `-Sheet` and
overwrites exactly eight PNGs beneath
`Java/assets/environments/settlements/city/props/garden/`. Explicit gutter cuts
account for the generated sheet's unequal object widths, rather than cropping
the artwork to an assumed equal grid.

Output IDs: `town_hedge_h`, `town_hedge_v`, `town_hedge_corner`, `town_hedge_end`,
`town_fence_h`, `town_fence_v`, `town_fence_corner`, `town_fence_gate`.
ConnectedBoundary composes the straight artwork around a shared ground anchor
for all sixteen cardinal connection masks, including corners and junctions.
Corner art is retained for authored placements. The existing interior furniture
implementation remains intact. The importer also accepts an optional `-Recipe`
JSON array of named source rectangles for subsequent garden feature batches;
in that mode it overwrites only the listed output IDs.

Review status: selected generated atlas and alpha inspected; TownExteriorRoomsTest
checks the eight RGBA assets, joined edges at three scales, generated density,
road reservations, and boundary/gateway collision across three seeds. Final game
captures are recorded under asset-review/reviews/town-layouts/2026-09-24-exterior-rooms.
User aesthetic acceptance is pending.
