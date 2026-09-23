# Continuous overworld terrain

The gameplay grid still controls movement and encounters. Its visual surface now uses continuous material coverage in world coordinates, with small deterministic distortions to break up the square outlines. Only coverage is feathered; the underlying textures remain sharp.

`LayeredTerrainRenderer` combines neighboring terrain materials, adds a narrow wet bank and shallow-water tint, and supplies a matching clip for animated water. Pixels depend on world position, so camera movement, tile draw order, and terrain chunk boundaries cannot shift the pattern. Existing textures are reused; this pass needs no new raster assets.

Goblin and bandit camp regions contribute one irregular dirt clearing with a worn central track. Ground stamps, redundant paving props, and road overlays inside the clearing yield to that shared surface. Other locations retain their existing props. Mountains, upright structures, and the wider road network still use their existing renderers; arbitrary rotation of upright wall sprites is not part of this change.

Composed tiles use a bounded cache and feed the existing terrain chunk cache. Uniform materials have a fast path, and fully wet tiles bypass the complex shoreline clip. Terrain edits in the surrounding neighborhood invalidate cached surfaces and chunks.

Validation from `Java/`:

```powershell
java '-Djava.awt.headless=true' -cp out com.alderfall.game.LayeredTerrainTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.LayeredTerrainPreview
java '-Djava.awt.headless=true' -cp out com.alderfall.game.RenderCacheTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.BridgeRenderingTest
java -cp temp/checks/classes com.alderfall.game.map.WorldGenerationTest
```

The layered test checks water masks at four zoom levels, opaque terrain coverage, tile-center consistency with gameplay, cache reuse, neighbor-edit invalidation, exact pixel agreement between direct/chunk rendering and scrolled views, and unchanged gameplay tiles. The preview writes actual game screenshots of shoreline, snow boundary, and camp to `exports/layered-terrain/`. Preview timings include PNG encoding and are not steady-state frame timings.
