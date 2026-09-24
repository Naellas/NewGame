# Modular furniture

Village placement offers seven chair/bench families in all four cardinal directions.
North means the occupant faces up, east right, south down, west left. Legacy IDs
are preserved; `DirectionalSeating` resolves older reversed naming and symmetric
backless bench variants. Interior furniture appears under Seating, outdoor benches
under Decor.

Decor also includes four modular market bays: canvas, blue, green and gold. Place
them on the same row, two tiles apart, to connect their counters and canopies.
Joined sides remove their end posts; exposed ends retain them. Different canopy
colors can join. Each bay occupies two tiles for placement, selection and collision.
These are village/editor placement assets; existing authored town stalls remain.

Village, farm and iron fences connect to cardinal neighbors of the same material,
including ends, corners, tees and crosses. Removing or moving a neighbor updates
the rendered junction automatically. Different materials retain separate ends.

Implementation: `ModularMarket`, `ModularFences`, `ConnectedFurniture`,
`DirectionalSeating`, `WorldPropRenderer`, `PropCollision` and WorldMap placement.
Runtime variants are bounded renderer caches.
Joined furniture preserves the aspect-fitted body at its standalone scale. Only
the inside edge margins are filled with mirrored texture patches; exposed outer
caps keep their positions. No cropped body is stretched across the footprint.
For art with detailed objects close to the joins, authored connector strips remain
a possible refinement: the generic mirrored margins can repeat small edge details.

Original art and import records:
`art-source/modular-marketplace/2026-09-24-cardinal-furniture` at repository root.

Build with `scripts/build.ps1 -IncludeTests -IncludeReviews`. From Java, run
`com.alderfall.game.ModularFurnitureTest`, `ConnectedFurnitureTest` and
`FurnitureQuestTest` with the build output as the Java classpath.
`com.alderfall.game.ModularFurniturePreview` exports both review images to
`temp/modular-marketplace/captures` by default.
