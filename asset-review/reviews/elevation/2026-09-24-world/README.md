# Geographic elevation ? 2026-09-24

Status: integrated into generated outdoor gameplay; visuals supplied for user
review. [Gallery](../../../../Java/tools/reviews/elevation/index.html).
[Scope and validation](../../../../Java/docs/world-elevation.md).

Original inputs: existing game terrain, props, characters and regional generation.
Bank pixels are drawn by ElevationMaterial. No image-generation prompts, new
source bitmaps or asset import steps. The material sheet uses the live renderer's
material code; all location screenshots use the actual GamePanel renderer.

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -OutputDirectory temp/world-elevation/classes -IncludeTests -IncludeReviews
```

Then from Java/:

```powershell
java '-Djava.awt.headless=true' -cp temp/world-elevation/classes com.alderfall.game.WorldElevationTest
java '-Djava.awt.headless=true' -cp temp/world-elevation/classes com.alderfall.game.WorldElevationPreview ../asset-review/reviews/elevation/2026-09-24-world
```

The exporter overwrites meadow.png, forest.png, mountains.png, mountain-route.png, frost.png,
sandstone.png, badlands.png, fen.png, coast.png, city_highwall.png,
city_sanctum.png, city_belltower.png, materials.png and locations.txt in its output
folder. The text file records selected biome coordinates and regions. Captures
use High quality, 125% zoom, near noon and current local presentation settings.
Use temp/world-elevation/preview for disposable output. Requires Java 21 and
existing assets; no saves/settings/runtime asset files are written.

Earlier isolated prototypes remain in their original dated batches.

Mountain refinement: broad coherent slopes replace local wave peaks; generated
ramps join existing passes to upper terraces. The route capture and locations
file record a complete example. All planned route edges pass collision tests
across seeds 0, 42 and 2026.
