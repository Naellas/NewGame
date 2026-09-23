# Subtile scenery review

Real overworld captures at noon and 100% zoom, with denser clustered flora and regenerated transparent sprites.

- [Meadow](meadow.png): tile 330,188, with 236 small details in the sampled 15×11-tile neighborhood.
- [Forest](forest.png): tile 207,107, with 134 small details in the sampled neighborhood.
- [Meadow footprints](meadow-footprints.png) and [forest footprints](forest-footprints.png): orange rectangles mark solid prop bases. Player clearance extends 0.14 tiles around the player's ground point.

The pictures intentionally show densely decorated examples. Lower-density patches retain empty slots and tiles. Tiny plants are passable; trees, ore, rocks and dense bushes use ground footprints.

Static details share the existing 4×4-tile terrain cache, including the new denser patches. These captures intentionally select dense areas; their coordinates differ from the original grouping benchmark and should not be used as a direct performance comparison with those earlier scenes.

See [implementation and validation notes](../../Java/docs/subtile-placement.md).
