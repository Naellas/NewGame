# Quarter-tile scenery and ground collision

Generated meadow and forest gaps can contain up to four small plant details per gameplay tile. Placement uses a deterministic 2×2 grid with small offsets and overlapping rounded patches. Patch centers have much denser flora, tapering to sparse growth toward the edges. Each patch favors one plant family so flowers, ferns and mushrooms form recognizable groups. Roads, terrain edges, landmarks and existing prop tiles are excluded. Existing assets are reused at smaller sizes.

`WorldProp.visualSlot` is `-1` for ordinary props and `0–3` for these generated visual details. The four-argument constructor remains available. Generated details are reconstructed with the world; player village and interior save formats are unchanged. They are excluded from primary prop selection and gathering.

Static details are baked into the existing 4×4-tile terrain chunks using cached source sprites. Warm frames do not sort, animate or draw each plant separately. Adjacent owner tiles are included during composition so sprites crossing chunk boundaries remain seamless. Adding, removing, moving or clearing details invalidates the terrain cache through the map's visual revision. Replacing the world also clears the chunk cache. Ordinary prop changes do not invalidate this static layer; trees and resources retain individual rendering and collision.

`PropCollision` derives natural solid footprints from the same `PropPlacement` anchor and scale used by rendering. Trees block at their trunks, rocks and ore at their bases, and dense bushes at their ground footprint. Small ground plants remain passable. Existing wall, fence, building and terrain collision remains in force. Footprint sizes are family-based approximations, independent of zoom and camera.

Exploration movement sweeps a player box with a radius of 0.14 tiles against those footprints and terrain. The player sprite, shadow and travel effects are aligned to the movement ground point. Authored doorway transitions still activate when the leading edge reaches the doorway. Removal and harvesting update collision immediately because the checks use the current prop index.

Pathfinding chooses a clear center or quarter-tile anchor for each tile and checks the swept connection between anchors. Diagonal corner cutting is prevented. A small local quarter-tile search joins click routes from the player's actual fractional position. The global search retains one anchor per gameplay tile, so it can be conservative where multiple disconnected gaps occupy one tile. This is not a replacement of the world, encounters or save coordinates with a higher-resolution grid. NPC schedules and party follower navigation retain their existing systems.

Validation: `SubtileCollisionTest`, `GroundDetailBatchTest`, `PropPlacementTest`, and `SmokeTest` pass. The batch test compares direct and cached pixels at three zooms and camera offsets, and verifies removal invalidation. `VillageTerrainTest` reports a cached/direct terrain pixel mismatch at (0,0); the same failure occurs in a separate build with quarter-tile generation disabled. Run diagnostics from `Java/` after building. Visual review images are in `asset-review/subtile-placement/` at the repository root; orange rectangles show prop ground footprints, without the additional player clearance margin.

Editor-authored natural props now retain linear footprint scaling above 2x and
apply reference-pixel offsets to their ground bases. Editor collision searches
include the full supported 512px size/offset range. Fixed structural prop footprints
also follow offsets; unshifted game structures preserve their grid rules. The editor
Collision overlay exposes these footprints for review. See `EditorPropCollisionTest`.
