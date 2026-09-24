# Purpose floors and wall decor — 2026-09-24

Curated captures from the actual game renderer. No live saves were edited.

- `blacksmith-1.png`: masonry forge/finishing areas and equipment pads; wood in customer areas; two joined oak casements.
- `home-1.png`: wooden bedroom; stone kitchen/hearth; harvest painting and new oak windows.
- `wall-art.png`: three joined casements, Gothic tracery, river and harvest paintings together on a two-tile rear wall.

Browse the [interactive comparison](../../../../Java/tools/reviews/interiors/flooring-and-decor.html).
Originals, prompts and crop recipe are in
[the art batch](../../../../art-source/interiors/2026-09-24-windows-and-paintings/README.md).

## Reproduction

From `Java/`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 -OutputDirectory temp/room-floors-wall-art/classes -IncludeTests -IncludeReviews
java '-Djava.awt.headless=true' -cp temp/room-floors-wall-art/classes com.alderfall.game.InteriorDesignPreview temp/room-floors-wall-art/captures blacksmith
java '-Djava.awt.headless=true' -cp temp/room-floors-wall-art/classes com.alderfall.game.InteriorDesignPreview temp/room-floors-wall-art/captures home
java '-Djava.awt.headless=true' -cp temp/room-floors-wall-art/classes com.alderfall.game.InteriorDesignPreview temp/room-floors-wall-art/captures wall-art
```

The home/workshop captures use footprint seed 1. The wall-art mode uses a small
review-only furnished room and the standard wall-placement API.

## Review and checks

Visually inspected: floors align with work/household zones, side cutaways continue
their neighboring finish, painting alpha is clean, window banks share a continuous
lintel/sill and individual artwork remains readable. Existing foreground walls
and player opacity bubble remain in use. User acceptance is pending.

Build and diagnostics passed: InteriorFlooringTest, ConnectedInteriorWallsTest,
ConnectedFurnitureTest, InteriorLayoutTest (1,620 plans), InteriorPlacementTest
(54 interiors), InteriorFurnishingsTest (79 assets / 324 plans), RenderCacheTest.
Architecture importer tests, review links and repository structure checks passed.
The repository-wide check remains blocked by
the existing five root-policy findings: `.vscode/settings.json` and four user PNGs
in `Input - uploads`; no baseline or uploads were changed.

Scope: room-purpose finishes are reconstructed for generated houses. Custom
equipment adds removable stone pads. There is no new manual floor-paint UI or
serialized purpose-zone format for externally authored maps. The Gothic window
is standalone; only identical oak casements form horizontal banks.
