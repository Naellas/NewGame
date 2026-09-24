# Six settlement tiers — 2026-09-24

Original inputs: `buildings.png` (transparent six-column/three-row building
atlas) and `grounds.png` (opaque three-column/two-row terrain atlas), generated
with the built-in image generation tool. Full prompts are in
`buildings-prompt.txt` and `grounds-prompt.txt`.

Reproduce from the repository root:

```powershell
python art-source/settlement-growth/2026-09-24-six-tiers/import_recipe.py
```

Requires Pillow and imports the existing village atlas crop helper. This is a
fixed recipe for these two original sheets. It overwrites only its 24 named
runtime outputs. Building row gutters are recorded in the recipe to preserve
the tall civic roofs; true alpha is retained, subjects fit into 256px canvases.
Ground textures are 192px squares.

Outputs:

- `Java/assets/environments/settlements/player_village/buildings/shared/settlement_residence_tier1.png` through `tier6.png`.
- Same folder: `settlement_workshop_tier1.png` through `tier6.png`.
- Same folder: `settlement_civic_tier1.png` through `tier6.png`.
- `Java/assets/environments/terrain/common/settlement_ground_tier1.png` through `tier6.png`.

Columns/tiers are Camp, Small Village, Village, Small Town, Large Town,
Metropolis. This batch supplies the residential, workshop and hall families.
The later specialist batch supplies distinct six-tier art for the other 15 types. Ground textures are manually paintable at any stage.

Review status: atlases inspected, runtime assets imported, lookup and real
panel rendering checked. Generated paving has visible repeating patterns;
perfect edge matching is not guaranteed. No legacy source or runtime assets
were removed. See `Java/docs/settlement-building.md` for controls and diagnostics.
