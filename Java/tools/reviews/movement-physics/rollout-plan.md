# Movement profile rollout preparation

The review renderer now uses MovementLimbProfile, an explicit sprite-and-direction registry. It separates contour, palette/occlusion strategy, pelvis anchor, reach, foot lift and far-leg shading from the animation clock. Missing profiles retain the prior renderer. No automatic promotion of one direction to every view.

## Pilot evidence

- Seraphine: exposed thigh, calf taper, ponytail-biased pelvis corrected; corrected joints retained with physics disabled.
- Knight: broader armored contours; hidden thigh samples visible armor instead of red tabard pixels.
- Mage: under-robe limb palette comes from visible footwear; the continuous robe remains a separate surface.

These are three right-facing pilots, not approval of other directions. See profile-pilots.png for source, extracted layers, and four walking phases. profile-coverage.csv inventories every actor and direction, including missing sources requiring fallback review.

## Next calibration batches

1. Exposed/slender limbs: Seraphine, Rafiq, Aria. Check near/far thigh identity, skin-to-boot boundary, and pelvis alignment.
2. Armor and equipment: Knight, Cassia, Samir, Calder. Check armor volume, knee overlap, and held equipment masks.
3. Robes and coats: Mage, Cleric, Lyra, Maera. Check hem continuity, hidden-leg palettes, and boots at contact.
4. Capes and ordinary clothing: Ranger, Rogue, Vesper and civilian NPCs. Check leg/cape ownership and rear-arm visibility.
5. Carrying NPCs: baker, bartender, blacksmith. Preserve intact grips and props while calibrating legs independently.

Within each batch: standing source -> side profile -> opposite side -> front/back -> four diagonals. Calibrate source masks separately where artwork differs. Review at gameplay size and enlarged, with guides off and on at the same paused frame. Do not mark a direction ready from a mirrored assumption.

## Required evidence before runtime adoption

- Source mask excludes hands, cape, equipment and opposite limbs. Anatomy hidden in the source may require authored near/far cutouts; repeated palette pixels are a prototype fallback, not recovered detail.
- Thigh, knee, calf and ankle retain meaningful cross sections. Joint overlaps stay connected across all 32 frames. Skeleton landmarks and source silhouettes agree.
- Foot contact follows traversed distance at all supported cadence settings; turning, diagonal travel, stopping, and idle must be checked in game.
- Collider width follows the visible contour. Current capsules are conservative envelopes, not a fitted cloth mesh.
- Body pose is identical with cloth physics on/off. Guide controls never alter simulation or select another rig.
- Keep the original renderer and an explicit fallback. Add runtime profiles by reviewed actor/direction batch, with bounded frame caching and actual game render checks.

## Reproduction

Compile game sources plus Java/tools/reviews/cloth-physics/source/*.java with Java 21 into a separate output folder. From Java/, run ExportClothStudy and MovementProfileReview. Run python Java/tools/reviews/grounded-movement/source/check_preview.py from the workspace root.

Current checks cover 288 pilot poses: connected texture rows, contour/collider bounds, planted-foot travel, capsule contact, loop continuity and physics-independent joints. Visual correctness of authored masks and diagonals still needs review before the large pass.
