# Raised-ground experiment — 2026-09-24

Status: prototype for user review, not accepted for generated adventure maps.

Historical first version: these captures are retained for comparison. The current
exporter now draws the [rounded-bank revision](../2026-09-24-organic-banks/README.md);
the commands below describe the original capture workflow and no longer reproduce
the square silhouette with the current source.

Open [the review page](../../../../Java/tools/reviews/elevation/index.html).
`plateau.png` shows the landscape; `route.png` shows the route and character on
the upper terrace; `ascent.gif` loops a climb through both ramps at 10 fps.
The GIF is half resolution; stills retain the full 1056 by 900 capture.

## Reproduction

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -OutputDirectory temp/elevation/classes -IncludeTests -IncludeReviews
```

Then from `Java/`:

```powershell
java '-Djava.awt.headless=true' -cp temp/elevation/classes com.alderfall.game.ElevationStudyTest
java '-Djava.awt.headless=true' -cp temp/elevation/classes com.alderfall.game.ElevationPreview ../asset-review/reviews/elevation/2026-09-24-plateau
java -cp temp/elevation/classes com.alderfall.game.ElevationPreview --play
```

The exporter overwrites the three named images in its output directory. Use
`temp/elevation/preview` for disposable renders. No save/settings files are written.
Requires Java 21 and existing runtime game assets; no added libraries or generated art.

## What this tests

The actual `WorldRenderer` produces the grass and earth-path material. The review
renderer projects those tiles using a development-only height field, reuses
`cavern_moss_face` for cliffs, and draws existing knight, oak, flower and rock
sprites at their surface heights. No runtime assets were added or altered.

Three levels (0, 1, 2), 30 pixels of rise per level at 48 pixels per tile.
North-facing ramps interpolate continuously within their tile. Their sides block
entry. A small breadth-first path search uses the same edge-height checks as
keyboard movement. Picking checks visible surfaces in reverse drawing order,
including rejection of clicks on exposed cliff faces.

The standalone Swing window supports click-to-walk and cardinal keyboard steps.
It deliberately has no combat, NPCs, prop collision, saves, water, full walking
animation or integration with adventure pathfinding. The rock face tiling and
straight boundaries are provisional. Integrating this into gameplay still needs
the shared collision, actor drawing, camera/picking, cache and world-generation
systems to consume height consistently.

## Validation

Review/test build and `ElevationStudyTest` pass. Tests cover blocked cliff and
ramp-side travel, both required ramps, reverse travel, continuous ramp endpoints,
surface picking, hidden lower ground and cliff-face click rejection. Both PNGs
were visually inspected.
The review-link/archive checker and structure/import checker also pass.

The repository check's 13 policy tests pass, but its hygiene stage reports the
unrelated untracked `.vscode/settings.json` as `unapproved-root`. This task does
not modify that file or the hygiene baseline.
