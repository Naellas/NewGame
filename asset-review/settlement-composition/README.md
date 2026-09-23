# Settlement composition and collision review

The supplied scenes showed oversized repeated red market props, competing copies of grand buildings, lantern scatter, tile-shaped road joints and narrow building collision. This pass changes the generated settlement rules and reuses existing assets; no new image generation was required.

- Prop selection uses deterministic weighted variation with a local repetition penalty, rather than always picking the highest score. A final dressing pass turns repeated market displays into a mixture of stalls, carts, sacks, barrels and kiosks, and varies repeated barrel planters. Nearby groups are limited to two members of the same palette.
- Street lighting is generated separately: junctions first, then corners and six-tile intervals along roads, alternating candidate sides. Lights sit beside roads, at least five tiles apart, outside door approaches, portal clearances and NPC working/gathering positions. Automatic paired facade lanterns are reduced to occasional single lamps.
- Small containers, baskets and planting trays no longer receive the blanket outdoor size boost. Their maximum display size is 0.72 tile; ordinary stalls are capped at 1.05 tiles. Player-built settlements retain their placement and sizing behavior.
- Ordinary building variants rotate in spatial order by style; adjoining sections of a row retain a coherent facade. Regional dwellings occupy half rather than three quarters of eligible ordinary lots. One principal building receives the grand regional silhouette; secondary civic buildings use ordinary alternatives. Small lots no longer receive grand themed architecture, towers are modestly reduced, and civic-hall sizing has more headroom. Rendering and collision share sizing/layout functions.
- Cities were drawing legacy road rectangles and seam strips over the continuous terrain. Those duplicate layers are disabled; bridges remain. Short entrance paths now connect to adjacent paved courts, including single road nodes that were previously suppressed. No gameplay road tiles are removed.
- Building collision now includes the upper wall body and walls protruding past the nominal lot. The previous footprint-depth cap left passable space inside the visible building. Existing saved positions inside newly solid walls recover to nearby free ground. A compact-common fallback preserves regional public spaces in dense villages after expanding the collision footprints.

Representative red-market counts changed from 11 to 1 in Sanctum, 7 to 1 in Embermarket, 3 to 0 in Highwall and 8 to 1 in Briarbridge. Lantern count is governed by street coverage and spacing, rather than an overall density reduction.

Review images:

| Settlement | Before | After |
| --- | --- | --- |
| Sanctum | [before](city_sanctum-before.png) | [after](city_sanctum-after.png) |
| Embermarket | [before](town_embermarket-before.png) | [after](town_embermarket-after.png) |
| Highwall | [before](city_highwall-before.png) | [after](city_highwall-after.png) |
| Briarbridge | [before](town_briarbridge-before.png) | [after](town_briarbridge-after.png) |

`Java/tools/SettlementCompositionReview.java` exports actual GamePanel scenes and prop counts. `RowHitboxReview` exports the [corrected row footprint](../building-hitboxes/row-fixed.png) and checks real hover geometry at three zooms.

Validation: `SettlementCompositionTest` checks lamp spacing/entrance clearance, repeat limits, asset availability, player settlement preservation, actual movement into an upper wall and recovery of an old position. `BuildingCollisionTest` exercises 77 buildings and door entry. `RegionalSettlementTest` verifies 24 settlements across three seeds, including two commons per settlement, institutional access and exits. `RoadSurfaceTest` includes a regression against the legacy city overlay; layered-terrain and ground-detail tests cover cache seams, zooms and invalidation. Subtile collision and startup smoke checks also run.
