# Wall placement, windows and cutaways

Status: implementation checked and captures visually reviewed; user acceptance pending.

- `home-hidden.png`: the sleeping-room partition hides the washstand. Leaded windows,
  painting, herb rack and lamps sit on the taller plaster face. Corners finish flush
  with the side rails.
- `home-cutaway.png`: the same layout, with the player behind the partition. The
  feathered wall cutaway reveals the floor and washstand near the player.
- `archive-windows.png`: a curtained window imported from the paneled source sheet,
  mounted above the Archive study shelves.

## Reproduce

From `Java/`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 -OutputDirectory temp/interior-wall-placement/classes -IncludeTests -IncludeReviews
java '-Djava.awt.headless=true' -cp temp/interior-wall-placement/classes com.alderfall.game.InteriorDesignPreview temp/interior-wall-placement/captures home
java '-Djava.awt.headless=true' -cp temp/interior-wall-placement/classes com.alderfall.game.InteriorDesignPreview temp/interior-wall-placement/cutaway home 3,4
java '-Djava.awt.headless=true' -cp temp/interior-wall-placement/classes com.alderfall.game.InteriorDesignPreview temp/interior-wall-placement/captures study
```

These frames are seed 0. The optional third preview argument is the player's
tile coordinate `x,y`; without it, the player starts at the entry.
The remaining seeds stay in ignored scratch space.

The [source recipe](../../../../art-source/interiors/2026-09-24-modular-materials/README.md)
preserves both window inputs and exact import instructions. Saved furniture/window
anchors and collision tiles retain their format. No furniture is relocated to
conceal overlap; foreground walls now occlude it.

Validation: ConnectedInteriorWallsTest (pixel occlusion, cutaway reveal, mount
coordinates, window fit, corner joins and cache boundaries), WorldDepthRenderTest,
InteriorFurnishingsTest, InteriorPlacementTest, InteriorLayoutTest, InteriorLightTest,
RenderCacheTest, and both architecture importer tests passed. The full repository
check still reports the pre-existing five root hygiene findings: `.vscode/settings.json`
and four originals in `Input - uploads`. The policy baseline was not changed.
