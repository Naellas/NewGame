# Water depth and weather

Outdoor water now uses continuous, world-aligned bed variation and small curved
wavelets. The animated layer has independent slow phases rather than a short,
restarting sprite cycle. Texture detail and depth color are cached with terrain;
wave paths and generated crests retain subpixel movement and cross tile/chunk boundaries. Existing
water asset names remain available for legacy and dungeon consumers.

## Depth and travel

`WorldMap.waterDepth` is the shared source for movement, paths, labels and color.
Existing outdoor maps and saves derive their depths from natural banks:

- Shallow: the first bank-adjacent water tile, or authored `~` water; walkable,
  movement duration 1.2x dry ground.
- Waist-deep: the next shelf; walkable, movement duration 1.65x dry ground.
- Deep: the interior beyond those shelves; blocks walking.

Diagonal bank corners use squared distance: up to 2 for shallow, up to 5 for
wading. Bridges, walls and buildings do not count as natural banks. Narrow
streams can therefore be crossed on foot; broad lakes retain blocked interiors.
Collision still respects props and buildings. Paths account for wading cost.
Players, followers and walking NPCs sink into the surface and gain ripple rings.
Depth color interpolates between tile centers, while collision retains the
existing gameplay grid. This is a first shore-derived depth model, without a
saved bathymetry map, swimming, or weather-dependent water levels. Dungeon water
retains its existing blocking behavior.

## Weather

The existing weather transition supplies wave strength, wind direction, rain and
storm intensity. Calm water has subdued wavelets; wind increases displacement
and crest curvature; storms add fading whitecaps. Existing rain impact rings
remain in `TerrainFeatureRenderer`. Quality settings reduce secondary wave
layers but retain the same animation clock (`GameConfig.FPS_MS`) and depth rules.
Snow does not freeze lakes or change their walkability.

## Validation and review

Build with `scripts/build.ps1 -IncludeTests -IncludeReviews`, then from `Java/`:

```powershell
java '-Djava.awt.headless=true' -cp out com.alderfall.game.WaterDepthTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.LayeredTerrainTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.BridgeRenderingTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.SubtileCollisionTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.RenderCacheTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.WaterPreview temp/water/surface-frames
```

`WaterPreview` writes 80 PNG frames at 10 fps, overwriting matching names in the
chosen output directory. It uses only Java standard libraries and the existing
runtime renderer. Its three rows isolate surface motion at fixed calm, wind and
storm inputs; rain impacts, props and characters are excluded from that strip.

[Visual review and reproduction](../../asset-review/reviews/water/2026-09-24-depth-and-weather/README.md).

## Generated crest cycles

The surface now includes a generated twelve-pose crest strip in
`assets/effects/weather/water_crest_cycle_anim.png`, loaded through AssetStore
with matching frame metadata. The original base water palette is unchanged.
Three world-aligned populations layer small unbroken swells, rolling wind waves,
and larger foaming crests. Each travels and fades over its own cycle. Weather
smoothly weights populations, so changing intensity cannot reset their clocks.
Adjacent authored poses crossfade to avoid a stepped slideshow; lifecycle fade
hides the end-to-start discontinuity. Quality settings reduce storm populations
on low-spec while keeping frame time consistent.

[Source and prompts](../../art-source/water/2026-09-24-wave-crests/README.md).
[Animated comparison](../../asset-review/reviews/water/2026-09-24-generated-crests/README.md).

## Swamp pools and banks

Marsh generation adds small organic `~` pools before roads, settlements and
location layouts. These remain shallow and wadeable. Cypress and willow trees
now participate in the normal marsh decoration palette, away from roads; low
companion placements stay low. Soft wetland vegetation is denser, and both `w`
and `~` count as water for the main bank placement rules.

Outdoor water near marsh tiles blends toward a peat olive hue, interpolated in
world coordinates and cached with the existing terrain revision. Open water
away from marsh retains its blue palette. Depth, waves and collision are unchanged.
Marsh floors also mix irregular peat-brown mud into the existing surface texture.
Generated sedge clumps supplement the existing reeds and cattails.

Validation: `SwampEnvironmentTest`, `WorldGenerationTest`, and the existing
water, terrain, bridge, collision and cache diagnostics. Render with
`LayeredTerrainPreview --swamp temp/swamp/render` from Java after a build with
`-IncludeTests -IncludeReviews -OutputDirectory temp/swamp/classes`.

[Source, prompt and import recipe](../../art-source/swamp/2026-09-24-wetland-flora/README.md).
[In-game visual review](../../asset-review/reviews/swamp/2026-09-24-wetland/README.md).
