# Grounded cutout review

The after sheets compare each source drawing with grounded frames 0, 8 and 16
in all four diagonal travel directions. Town NPCs without diagonal artwork use
the same side-art fallback as the game, with diagonal movement retained by the
rig. Ranger and Calder before sheets preserve the original defects.

This pass keeps the distance/stance timing and body weight transfer. It changes:
- visible-boot selection instead of sampling the gap between the source feet;
- narrower projected knee bends and wider leg separation in quarter views;
- coverage below the belt, whole aprons/dresses and Ranger/Rogue rear cloaks;
- stable carrying poses for baker, bartender and blacksmith;
- Calder's diagonal hammer/shaft ownership, excluded from leg texture sampling;
- matching gallery/game fallback directions for NPCs with cardinal-only art.

The result still uses cutouts from flat artwork. Occluded limb surfaces are
reconstructed, and other actor-specific masks may warrant a separate review.

Run `GroundedCutoutTest`, `GroundedWalkingTest`, `GroundedGameRenderTest`,
`GroundedCutoutReview after` and `CharacterAnimationAudit grounded` from Java
with the compiled game on the classpath to repeat the checks and exports.
