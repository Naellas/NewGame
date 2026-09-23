# Full-roster movement calibration

Open index.html locally or through the simulator link. It filters by failed checks and missing directional art and shows every actor's source plus contact/passing poses in eight movement directions.

## What is calibrated

CalibrateMovementRoster measures each available normalized source independently. It records a robust pelvis estimate from torso rows excluding known cape/equipment masks; thigh, knee, calf and ankle row spans; a material category; source hash; and visible-landmark evidence. The three manually tuned pilots remain overrides. measured-profiles.csv stores the resulting values and evidence. These are review candidates, not runtime defaults.

All 127 humanoid actors and 1,016 movement-direction cases are exercised at 0.5x, 1x and 1.5x preference. The actual sideways minimum remains 0.9x. Missing diagonal sources use the existing side art with diagonal leg trajectories, explicitly tagged as fallbacks. This does not invent a diagonal cutout or certify its visual accuracy.

## Checks and fixes

Every case checks 32 frames at each cadence: finite skeleton joints, limb geometry, continuous opaque thigh/shin cross sections, collision-envelope width, planted-foot displacement in both axes, and frame-edge clipping. Physics is checked separately for capsule clearance, pinned roots, repeatable loop boundaries and invariant body joints when disabled. A successful check is numerical evidence, not visual approval.

The first sweep exposed one Cleric right-view clipping case and 72 cloth-loop discontinuities. Cleric right has no visible staff: gold-trim detection was mistaking dress pixels for the held staff. That source is now excluded from staff extraction in the study. Cloth collision paths now use an outward-only cyclic clearance envelope to prevent sudden impact displacement while preserving capsule clearance. Candidate boot selection prioritizes the pelvis rather than a wider nearby prop. Seraphine hair extraction is restricted to its calibrated right-view mask.

## Integration gate

Do not integrate inferred anatomy as if it were authored anatomy. Some contour landmarks are hidden by robes, armor, hands or equipment. The evidence column records this. Existing generic part masks are not complete per-actor segmentation. Native diagonal sources still require limb-depth review, while 452 missing-diagonal cases require an explicit art/fallback decision. In particular inspect carrying poses, Calder's hammer, long hems, ponytails, and bow/staff grips at gameplay size and enlarged.

The current game renderer and assets remain unchanged by this calibration pass. Promote only visually reviewed actor/direction profiles in a subsequent runtime change, with fallbacks preserved and in-game turning/stopping/cadence tests.

## Reproduce

Compile game Java sources plus ../cloth-physics/source/*.java into a separate Java 21 output folder. Run com.alderfall.game.CalibrateMovementRoster from Java/. It writes measured-profiles.csv, checks.csv, results.js, and one eight-direction sheet per actor. Run ExportClothStudy and MovementProfileReview to refresh the interactive pilots. Run the existing check_preview.py browser check from the workspace root.

## Final numerical results

564 native source profiles measured; 127 actors across 1,016 direction cases; all 97,536 geometry poses passed and all 1,016 three-cadence physics cases passed. 452 diagonal cases retain side-art fallback. Targeted reruns replaced 24 cases for Calder, Ranger and Rogue after source ownership and rear-cape depth corrections. The browser check passes inventory filters, mobile layout, local-file previews and independent simulator toggles. These numerical passes do not remove the visual/artwork integration gate above.

For changes isolated to selected actors, pass actor IDs to CalibrateMovementRoster. It writes a targeted/ report. After reviewing it, run merge-targeted.py to replace only those actors in the full audit.
