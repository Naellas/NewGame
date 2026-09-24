# Rounded terraces and soil banks — 2026-09-24

Status: revised prototype for user review. Not enabled in the adventure world.

Historical capture: the current exporter also includes the later
[walkable small steps](../2026-09-24-small-steps/README.md). The existing images
here are retained for comparison; re-export to the current batch instead.

[Open the review page](../../../../Java/tools/reviews/elevation/index.html).
It includes the landscape, animated ascent, route and previous version for comparison.
The original square-edged captures remain in `../2026-09-24-plateau/`.

## Changes

Continuous curved outlines replace individual raised tile rectangles. Surface
projection, actor elevation, movement checks and picking consume the same contour.
The contour follows irregular broad lobes, with narrow straight joins at the two
ramps. The terrain renderer caches its projected ground and depth buffer; that
buffer also hides sprites behind banks when appropriate.

Procedural warm soil, small embedded stones, muted strata, a broken grass lip
and contact shading replace the repeated dark cavern rock sprite. These are Java
renderer effects, not new bitmap assets. Original inputs are the existing game
grass/road material and knight, oak, flower and rock sprites, loaded by AssetStore.
There is no image-generation prompt or art import step.

## Reproduction

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -OutputDirectory temp/elevation/classes -IncludeTests -IncludeReviews
```

Then from `Java/`:

```powershell
java '-Djava.awt.headless=true' -cp temp/elevation/classes com.alderfall.game.ElevationStudyTest
java '-Djava.awt.headless=true' -cp temp/elevation/classes com.alderfall.game.ElevationPreview ../asset-review/reviews/elevation/2026-09-24-organic-banks
java -cp temp/elevation/classes com.alderfall.game.ElevationPreview --play
```

The exporter overwrites `plateau.png`, `route.png`, and `ascent.gif` in the chosen
directory. Stills are 1056 by 900; the animated GIF is 528 by 450 at 10 fps.
Use `temp/elevation/organic` for disposable captures. Requires Java 21 and existing
game assets. No saves, settings or runtime assets are modified.

## Scope and checks

Build and elevation tests pass, including both ramps, blocked cliff crossings,
reverse travel, continuous ascent, surface/face picking and sub-tile contours.
Every permitted cardinal navigation edge is additionally checked at 256 points
for height discontinuities. Stills were visually inspected.
Review-link/archive and structure/import checks pass. The full repository check
passes its 13 policy tests, then stops on the pre-existing untracked
`.vscode/settings.json` (`unapproved-root`); that file and the baseline are unchanged.

This is still the standalone elevation study. Props are decorative, the actor
uses a standing pose, navigation goes between tile centers, and there is no
combat, NPC, water, save or adventure-world integration. Bank texture and contour
style remain open for art review.
