# Dialogue framerate investigation — 2026-09-23

## Mesh remediation

The default renderer now uses an adaptive deformation mesh and Java2D cubic
texture patches. It retains the existing skeleton, physics, source resolution
and animation clock. Facial-only configurations bypass body deformation, and
party portraits only deform their visible viewport. The original dense path
remains available with `-Dalderfall.dialogueDenseReference=true` for comparison.

Full-scene benchmark after mandatory attachment-region refinement:

| Renderer | Mean | Median | p95 |
| --- | ---: | ---: | ---: |
| Mesh | 60.11 ms | 58.14 ms | 70.72 ms |
| Dense reference | 102.23 ms | 99.83 ms | 119.54 ms |

This measured 1920x1080 panel painting on the Swing event thread, with Aria,
Mage, the world and dialogue UI visible: 120 warm-up paints, 60 samples each.
The world simulation timer was stopped for repeatability. These timings exclude
updates and display presentation; they are not delivered in-game FPS. The mesh
reduces mean render time by about 41%, but still exceeds the 50 ms timer budget.
Java2D native texture filtering is the next major cost; this does not establish
a 60 FPS renderer. No texture downsampling or animation-rate reduction was used.

`DialogueMeshTest` compares 12 poses of each calibrated character against the
dense reference, checks portrait/full-frame cache transitions, and detects opacity
seams. Mean RGB errors on opaque reference pixels were 0.228 (Aria), 0.265
(Vesper), and 1.665 (Mage), on a 0–255 scale; no tested opaque pixels fell below
alpha 240. Full-figure and in-game captures were also visually inspected.

Reproduce after building with `-IncludeReviews` into a task directory, from `Java`:

```powershell
java '-Djava.awt.headless=true' -cp temp/dialogue-mesh/final com.alderfall.game.RenderBenchmark 60 dialogue temp/dialogue-mesh/game-mesh.png
java '-Djava.awt.headless=true' '-Dalderfall.dialogueDenseReference=true' -cp temp/dialogue-mesh/final com.alderfall.game.RenderBenchmark 60 dialogue temp/dialogue-mesh/game-dense.png
```

## Original diagnosis

The principal regression is the full-body CPU image deformation added to
`DialogueFigureAnimation`, not the spring/node/collider simulation. Both large
figures are reconstructed on every repaint, on Swing's event dispatch thread.
No production behavior was changed during this investigation.

## Measurements

Isolated headless rendering of Aria and Mage together, using the production
renderer and fitted assets. Each case has 60 warm-up frames and 100 measured
frames, with a simulated 17 ms animation step. The destination is an ARGB image;
world rendering, dialogue text, game updates and display presentation are excluded.
These are local CPU timings, not measured in-game FPS.

| Configuration | Pair at 480×800 each, mean ms | Pair at 510×1000 each, mean ms |
| --- | ---: | ---: |
| Static source images | 0.88 | 1.25 |
| Standard animation | 56.77 | 75.37 |
| Secondary physics disabled, same body/hand rig | 57.01 | 75.65 |
| Reduced motion | 34.35 | 46.64 |
| All body/idle channels disabled, mouth still driven | 22.83 | 30.36 |
| Same cached animation timestamp/pose | 0.86 | 1.21 |

Standard animation at 510×1000 had a median of 74.73 ms and p95 of 78.34 ms.
A separate idle-only run, with mouth closed and no speaking/listening gestures,
took 77.07 ms per pair: the cost continues after text finishes.
The isolated Aria physics update, including copied node arrays, averaged
0.0011 ms over 10,000 updates.

A separate diagnostic thread sampled the rendering thread's stack every 5 ms.
It recorded 1,874 top-frame samples in `sampleSharp` and 161 in `articulate`;
the next most common method was Java2D `MaskBlit` at 45. This sampler adds some
overhead and includes startup; the table above was measured without it.

## Cause and consequences

- `DialogueRenderer.drawCompanionDialog` draws both figures at up to 540 pixels
  wide, with height derived from the window. A 1080-high view requests 1000-high
  fitted images after the figure margin.
- `DialogueFigureAnimation.draw` invalidates the rendered result whenever
  `timeMs != rig.frame`. Ordinary consecutive repaints therefore recompute it,
  even when speech has ended. Cached facial poses do not cache body deformation.
- `articulate` traverses the full figure's active image spans, calculates body,
  head, breathing and attachment coordinates, then invokes `sampleSharp`.
  Its cubic reconstruction reads up to 16 source pixels for each output pixel.
  Whole-body sway made this expensive operation cover most of the visible model.
- Disabling body channels does not bypass the coordinate-calculation loop.
  This explains the remaining 30 ms in the all-body-channels-disabled case.
- `GamePanel.paintComponent` draws the world before the dialogue. This is
  additional cost, not included in the measurements. Its timer and painting run
  on the same Swing event thread, so long paints also delay updates and input.
- `GameConfig.FPS_MS` is 50: the nominal timer target is already 20 Hz. A 75 ms
  character pass alone exceeds that budget and implies a roughly 13 FPS ceiling
  before the other work. Timer scheduling can make actual throughput lower.
- Initial rig creation also builds per-pixel masks and foot/support data. First
  pair draws in this experiment ranged around 113–314 ms for standard/no-physics
  cases, including JIT effects. This contributes an opening hitch but does not
  explain the sustained cost after warm-up.
- `AssetStore.spriteFit` returned the identical cached image on repeated calls;
  repeated asset fitting/loading is not needed to reproduce the sustained drop.
- Party `drawPortrait` currently crops only after rendering the full source rig,
  so a small visible portrait can still incur full-figure deformation work.

## Recommended correction

Keep the simulation and original texture resolution. First add a true bypass
when no body deformation is active and restrict party rendering to its crop.
For the normal animated case, replace the dense per-pixel coordinate solver and
Java cubic reconstruction with a coarser deformation mesh or cached transforms
rendered by an efficient image path. Small mouth/eye regions can update separately.
Measure that replacement with two characters and the world visible, and review
sharpness, attachment boundaries and foot pins before adopting it. Merely
disabling physics or increasing the timer rate will not fix this bottleneck.

## Reproduction and validation

The one-off probe and compiled classes are under ignored
`Java/temp/dialogue-profile/`; `DialogueProfile.java` contains the cases and the
optional `profile` argument enables stack sampling. `sources.txt` contains the
production sources plus that probe. From `Java`:

```powershell
javac -d temp/dialogue-profile/classes '@temp/dialogue-profile/sources.txt'
java '-Djava.awt.headless=true' -cp temp/dialogue-profile/classes DialogueProfile
java '-Djava.awt.headless=true' -cp temp/dialogue-profile/classes DialogueProfile profile
```

The production-only build passed. Repository policy tests passed (13 tests),
as did review-link/checksum and structure/import/asset-identity checks.
`scripts/check.ps1` stops at an existing layout violation:
`review-outside-tools:asset-review/dialogue-motion/index.html`. That staged file
contains only `v`; it was not changed by this investigation. The active review
site is under `Java/tools/reviews/dialogue-motion/`.


## D-drive working-copy transfer

The mesh and portrait/banter changes were merged into `D:/Codex python/Codex python game/Java` and its normal `out` was rebuilt. D-specific building-rendering changes were preserved. Portrait HUD, mesh, animation, pose, physics and party dialogue checks passed against this normal output. The full repository checks also passed in D; the earlier legacy-path violation recorded above belonged to the old OneDrive copy. Earlier performance numbers describe the original measurement scene, not a new D-scene benchmark.
