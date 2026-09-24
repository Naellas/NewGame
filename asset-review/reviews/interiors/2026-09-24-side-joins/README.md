# Slim side walls and continuous junctions

Status: visually reviewed in renderer captures; user acceptance pending.

- `home.png`: side rails use one-sixth of a tile, with matching single posts at
  corners and T-junctions. Header/dado bands are excluded from repeated shafts.
  Detached shadows at the old full-cell boundary are removed.
- `cutaway.png`: the same layout with the player behind the sleeping-room wall,
  showing that furniture occlusion and the opacity bubble remain available.

Movement, saved wall coordinates and furniture placement still use the existing
tile grid. Narrower visuals do not add traversable slivers beside wall cells.
This change reuses the supplied architectural pieces; no new asset import.

## Reproduce

From `Java/`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 -OutputDirectory temp/interior-side-joins/classes -IncludeTests -IncludeReviews
java '-Djava.awt.headless=true' -cp temp/interior-side-joins/classes com.alderfall.game.InteriorDesignPreview temp/interior-side-joins/home home
java '-Djava.awt.headless=true' -cp temp/interior-side-joins/classes com.alderfall.game.InteriorDesignPreview temp/interior-side-joins/cutaway home 3,4
```

The two retained images use seed 0. Additional seeds remain in ignored scratch.
ConnectedInteriorWallsTest checks exact seam pixels across corner, T and cross
junctions at 24, 48 and 73 pixels, plus wall mounts, furniture occlusion, player
cutaways and terrain cache boundaries. It passed, as did WorldDepthRenderTest and
RenderCacheTest. The full repository check remains blocked by the existing
`.vscode/settings.json` and four original uploads; the baseline was not expanded.
