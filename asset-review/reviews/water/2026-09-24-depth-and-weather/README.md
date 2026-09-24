# Water: depth and weather, 2026-09-24

- `weather-states.png`: runtime surface comparison, left-to-right shallow to deep.
- `weather-motion.gif`: eight seconds of calm, wind and storm surface motion.
- `snow-shore.png`: full game render showing depth shelves beside snowy banks.
- `wading.png`: full game render with the player in a walkable shelf.

Reproduction: build Java with `scripts/build.ps1 -IncludeReviews`, then run
`com.alderfall.game.WaterPreview temp/water/surface-frames` from Java with classpath
`out`. The 80 frames are spaced by two simulation ticks (100 ms). Encode in name
order at 100 ms/frame; the GIF playback boundary is a review reset, not an engine
animation loop. Fixed weather inputs isolate the surface layer; the strip does
not include the game's additional rain impact rings.

The shore capture uses `LayeredTerrainPreview --water temp/water/preview`.
The same `--water` command finds the nearest wading shelf to (125,99); the
current world selects (125,98).
Original art inputs: existing game assets; no new raster art was generated.

Review status: inspected surface strips and in-game shores. Depth, collision,
pathfinding, camera alignment and frame continuity have automated coverage.
Final subjective art approval remains with the user.
