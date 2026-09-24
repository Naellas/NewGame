# Oathstead town board

Generated with the built-in image generator using the exact adjacent `prompt.txt`.
`original.png` is the full transparent output. `previous-board.png` preserves the
runtime art being replaced. The user requested a new, resized town board.

From repository root:

```powershell
python art-source/settlement-management/2026-09-24-town-board/import_recipe.py
```

Requires Pillow. Overwrites only
`Java/assets/environments/settlements/player_village/player_village_quest_board.png`.
The import trims transparent margins, resizes to fit 176 pixels, and grounds the
sprite on a padded 192x192 transparent canvas. Stable asset ID preserves quest
and save references. World placement is 56 reference pixels; board UI fits it
to a 128x104 preview.

Review: inspected the source and actual game renders. The sprite has a complete
roof, posts and notices, with no detached fragments above the canopy.
See [settlement management evidence](../../../asset-review/reviews/settlement-building/2026-09-24-management/README.md).
