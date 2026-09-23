# Row building footprint correction

Row buildings could draw several adjoining modules but derive collision from their door count. A five-tile row can have three visible modules and only one logical entrance, leaving the outer walls passable.

`BuildingGeometry.moduleCount` and `moduleVisualBounds` now supply the same module count, sizes, centers and jitter to drawing and collision. Each visible section has a solid base. Ordinary bases cover 90% of the sprite width, including walls extending beyond the lot; towers retain a narrower base. Wall depth now uses 80% of sprite height (65% for towers), bounded by lot depth. This corrects the subsequent upper-wall phasing report; the former 42% strip was too shallow. Collision queries include overhanging sides. Front-door interaction remains intact, and old saved positions inside corrected walls relocate to nearby free ground.

Validation: `BuildingCollisionTest` checks 77 buildings, continuous swept movement, side/back clearance and entry through front doors. Its row regression samples the entire five-tile frontage, including the previously unblocked outer sections. `SubtileCollisionTest` and `SmokeTest` pass. `RowHitboxReview` exercises the real GamePanel hover geometry at 75%, 100% and 150% zoom.

[Corrected row with footprint overlay](row-fixed.png). Roofs can still obscure a player standing behind a building; the solid ground base covers its walls.
