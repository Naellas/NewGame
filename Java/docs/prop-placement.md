# Prop anchors within tiles

Overworld natural props now use deterministic ground anchors inside their owning tile. `PropPlacement` defines the visual policy; `WorldProp` still stores the original tile, asset, and authored size, preserving existing saves, gathering, and navigation.

Trees use a restricted root footprint and five scale multipliers from 0.84 to 1.08. Rocks and fallen wood use 0.82–1.14; small plants use 0.76–1.12; camp crates use 0.92–1.04. Positions vary continuously, while the five size choices limit sprite and shadow cache growth. Asset and world coordinates determine the variation, so scrolling, reloading, zooming, and gathering other props cannot reroll a scene.

Neighboring roads, settlement entrances, and land/water boundaries narrow the placement area. Anchors remain inside their original tile. Canopies can overlap neighboring tiles; this is visual placement, not a replacement for collision footprints. Authored landmarks, elder trees, connected fences/walls, and settlement/interior/editor props retain deliberate placement.

Ground cover draws before upright props. Upright props sort by their actual foot position with deterministic tie-breakers. Rendering, shadows, falling-tree origins, gathering tool hits, resource highlight rings, reactive vegetation pulses, and light offsets use the shared placement. Click-to-gather still selects the owning gameplay tile.

Run from `Java/`:

```powershell
java '-Djava.awt.headless=true' -cp out com.alderfall.game.PropPlacementTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.LayeredTerrainPreview --preview exports/prop-placement
```

The test checks all generated prop anchors and scale ranges, recreated-record determinism, fixed landmarks, road clearance, four zoom levels, camera translation, and identical pixels when prop input order is reversed. The preview writes shoreline, snow-edge, and camp screenshots using the actual game renderer.
