# Masonry backs and timber floor borders

The September 24 follow-up uses the existing supplied tile art. No new generated
art or import step is required. Review through the
[flooring gallery](../../../../Java/tools/reviews/interiors/flooring-and-decor.html).

- `blacksmith-1.png`: masonry behind stone forge/anvil/finishing areas, original
  plaster behind wooden service areas, dressed masonry ends and timber floor edging.
- `home-1.png`: masonry kitchen/hearth sections; wooden bedroom floor and its
  original rear wall; a timber border around the stone work surfaces.

From `Java/`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 -OutputDirectory temp/room-wall-finishes/classes -IncludeTests -IncludeReviews
java '-Djava.awt.headless=true' -cp temp/room-wall-finishes/classes com.alderfall.game.InteriorDesignPreview temp/room-wall-finishes/captures blacksmith
java '-Djava.awt.headless=true' -cp temp/room-wall-finishes/classes com.alderfall.game.InteriorDesignPreview temp/room-wall-finishes/captures home
```

Copy only seed 1 into this review folder; remaining captures stay in scratch.
Visually inspected full game renders. User acceptance pending.

Build, InteriorFlooringTest, ConnectedInteriorWallsTest, InteriorPlacementTest and
RenderCacheTest passed. Tests cover four border directions at 24/48/73 px,
absence of internal or wall-edge strips, masonry selection/restoration after
equipment edits, mixed-material terrain chunks, side junctions and occlusion.
The full repository check still flags the five pre-existing unapproved root
files (`.vscode/settings.json` and four PNGs in `Input - uploads`).

Finishes are derived automatically. This does not add manual wall-material tools.
Borders stay inside stone floor tiles, and existing rugs may cover them. Side
rails keep the original structural material; the visible rear faces change.
