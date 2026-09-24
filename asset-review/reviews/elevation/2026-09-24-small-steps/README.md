# Intermediate heights and walkable steps — 2026-09-24

Status: standalone prototype for user review, not enabled in the adventure world.

[Open the review page](../../../../Java/tools/reviews/elevation/index.html).
The landscape, close-up, animated climb and route demonstrate a new stepped
approach along the southern bank. Earlier captures remain in their original batches.

## Behavior

Terrain now includes 0, 0.25, 0.5, 0.75, 1 and 2 height levels. Each shallow riser
is one quarter of a normal cliff (7.5 pixels at the current 48-pixel tile scale).
Three curved shelves create four small climbs to the first terrace. Both existing
ramps remain available; the animation takes the new steps, then the upper ramp.

Keyboard and click-to-walk share a maximum individual step height of 0.25.
Both upward and downward crossings are allowed within that limit; larger
discontinuities still block walking. Sampling checks each intervening riser,
rather than only comparing the start and destination. This lets a sequence of
small steps climb a full level without permitting a single full-height jump.
The actor uses a short smooth vertical transition over north/south risers.

Grass, road, tree, knight, rock and flower inputs are existing game assets.
Bank shading is procedural Java rendering. No generated bitmap art, prompts,
asset imports or runtime asset modifications are involved.

## Reproduction

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -OutputDirectory temp/elevation/classes -IncludeTests -IncludeReviews
```

From `Java/`:

```powershell
java '-Djava.awt.headless=true' -cp temp/elevation/classes com.alderfall.game.ElevationStudyTest
java '-Djava.awt.headless=true' -cp temp/elevation/classes com.alderfall.game.ElevationPreview ../asset-review/reviews/elevation/2026-09-24-small-steps
java -cp temp/elevation/classes com.alderfall.game.ElevationPreview --play
```

The exporter overwrites `plateau.png`, `route.png`, `steps.png` and `ascent.gif`
in its output folder. Stills are 1056 by 900, the close-up is 800 by 490, and the
GIF is 528 by 450 at 10 fps. Use `temp/elevation/steps` for scratch output.
Requires Java 21 and the existing game assets. No saves/settings are written.

## Validation and limits

Build and elevation tests pass: fractional shelves, a direct step route in both
directions, smooth vertical foot motion, blocked tall cliffs, retained ramp
access, picking and dense checks of every allowed navigation edge. Landscape
and close-up captures were visually inspected.
Review-link/archive and structure/import checks pass. The full repository check
passes its 13 policy tests, then stops at the existing `.vscode/settings.json`
layout violation; that file and the hygiene baseline remain untouched.

This remains a development preview. Props are decorative, the actor uses a
standing pose, and navigation uses tile centers. No NPC, save, combat or
adventure-world integration is included. Fractional heights and the traversal
limit are authoring constants, not a new player settings slider.
