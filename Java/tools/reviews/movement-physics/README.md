# Unified movement physics simulator

Open `../characters/index.html#movement-physics` or this folder's `index.html` directly. No server required.

The simulator brings the grounded and cloth studies onto one 32-frame timeline. It compares actual runtime grounded frames against the revised review-only rig with cape/hair physics. Character, cadence, slow motion, pause and scrub are shared. Skeleton, planted-foot markers, leg capsules and cloth/hair nodes can be inspected independently. The game baseline keeps its own joint metadata so overlays match the appropriate rig.

Coverage: Mage, Seraphine and Knight, facing right, at three cadence preferences. 0.5x is limited to 0.9x sideways, matching the game. Ground distance per cycle changes with cadence, keeping travel speed constant. Turning, other directions, scenery and full cloth meshes are outside this study.

The revised rig samples robe rows continuously between spring-driven edges rather than separating the robe down its centre. Seraphine's static source leg pixels are excluded from the clothing layer, with tapered thigh/calf contours. StudyArmRig reduces arm swing and wrist flutter while preserving wrist parenting. These refinements are preview-only; game code is unchanged by this merge.

Data and strips are exported by `../cloth-physics/source/ExportClothStudy.java`, using `ClothMotionStudy` and `StudyArmRig` plus the game classes. Compile those sources with Java 21 into a separate output directory, then run `com.alderfall.game.ExportClothStudy` from `Java/`. The exporter verifies pinned roots, finite nodes, loop continuity and leg-capsule contact for 288 poses. This page reuses its `data.js` and frames rather than duplicating assets.

Browser smoke check: `python Java/tools/reviews/grounded-movement/source/check_preview.py` from the workspace root. Checks all nine actor/cadence combinations, overlays, scrubbing, corrected rig retained with physics off and fixed-frame toggle round trips, old deep links, dialogue tab navigation, mobile layout and local-file loading.

Older standalone studies remain linked. Both former tab hashes (`#movement-study`, `#cloth-physics`) open the unified simulator. The dialogue tab is preserved.

Seraphine side-view correction: legs use a calibrated pelvis anchor independent of the ponytail-biased head centroid; thigh/shin sampling follows that anchor. Reduced excess knee bend and swing lift retain the same planted-foot travel. `SeraphinePlacementReview` checks 96 poses across the three cadences and exports a cycle contact sheet (`seraphine-after.png`).

Physics and rig selection are independent: the left panel always shows the game baseline, and the right always uses the revised rig. The physics checkbox switches between revised-rig strips with and without cloth/hair simulation. The exporter checks that both variants have identical legs and skeleton joints across all 288 poses. Guide toggles do not select a different sprite or advance paused playback.

Initial leg-volume prototype (Seraphine): the previous seven-pixel rectangular strips and darkened edges removed thigh/calf contours. The study now uses separate row profiles (11?12 px thigh, 8 px knee, 10 px calf, 6 px ankle), samples the source while excluding palm/cape pixels, and reduces far-leg darkening from 22% to 10%. Hip/foot trajectories are unchanged. The collision envelope is widened to match. `seraphine-volume-comparison.png` preserves before/after poses. For a broader pass, calibrate each actor?s silhouettes and visible source limb regions; do not apply one width multiplier to everyone. Occluded anatomy still needs authored cutouts for accurate costume details.

## Reusable profile preparation

MovementLimbProfile now owns contour landmarks, material/occlusion strategy, pelvis anchor, shading, reach and lift for three exact sprite/direction pilots: Seraphine, Knight and Mage facing right. MovementProfileReview validates 288 poses and exports profile-pilots.png, profile-coverage.csv and profile-coverage.js. The inventory covers 127 humanoid actors and 1,016 direction cases; three are pilots and the remainder await calibration. Unknown profiles keep the prior renderer. See rollout-plan.md for batches and runtime acceptance criteria. The simulator links both the cutout sheet and inventory.

## Full-roster calibration pass

See calibration/index.html for all 127 actors, 564 measured source profiles, 1,016 movement directions and 97,536 checked poses. The final geometry and physics checks pass for every case. The review retains 452 missing-diagonal-art flags and per-source visible-landmark evidence. Candidate profiles remain confined to the review pipeline; the game has not been integrated. See calibration/README.md for fixes, limitations and targeted reruns.
