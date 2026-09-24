# Generated wave animation review

`weather-motion.gif` compares eight seconds of calm, wind and storm surface
motion at 10 fps. `weather-states.png` captures the same comparison at two seconds.
The playback boundary resets the review; the renderer's individual waves fade
out before their own lifecycle repeats. Depth colors are unchanged.

Reproduce from Java after `scripts/build.ps1 -IncludeReviews`:

```powershell
java '-Djava.awt.headless=true' -cp out com.alderfall.game.WaterPreview temp/water-waves/frames
```

Encode the 80 PNG frames in name order with 100 ms duration. Fixed wave strengths
are 0.18 / 0.58 / 0.95; the storm row also sets storm intensity to 1. This isolates
the runtime water layer. Rain impacts and scenery are not included in the strip.

Review status: inspected source, runtime alpha and comparison frames. Automated
checks cover asset loading, camera alignment, frame continuity including cycle
boundaries, gradual weather changes, and the existing depth/movement behavior.

[Original sheets, prompts and import recipe](../../../../art-source/water/2026-09-24-wave-crests/README.md).
