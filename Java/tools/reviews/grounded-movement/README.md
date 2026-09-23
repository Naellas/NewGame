The grounded system has now been integrated into the game as the default for
humanoid world characters. Use the character gallery's **Walking - grounded
(current)** option for runtime coverage. The files in this folder preserve the
original three-character study and its earlier comparison baseline; the
review-only statements below describe that historical prototype.

# Grounded movement illustration

Open the **Grounded movement study** tab in `../characters/index.html`, or open
`index.html` locally. The tab can be linked directly with `#movement-study`. It needs no server. `mage-comparison.gif` is a shorter,
non-interactive comparison.

This is a review artifact only: no production movement code, settings or defaults
were changed. The left panel uses actual current runtime frames. The right uses
an isolated copy of the renderer in `source/GroundedMotionConcept.java`.

The concept introduces a 60% stance period with brief double support, constant
world-space foot contact during stance, step length adjusted to cadence, a
length-constrained knee solution, weight transfer and a small additional cloth
phase delay. Existing artwork and approximate limb masks remain in use.

Both panels have the same ground speed. Cadence choices are 0.85x, 1x and 1.2x;
playback speed slows the whole comparison. Contact guides expose sliding rather
than concealing it. Frame stepping and a full skeleton overlay are available. Green shows the spine
and head hierarchy; pink shows wrists and hands; amber/blue shows the legs.
The current panel retains its original leg-only overlay.

The distance reference is the current 0.75-tile stride, 48-pixel tiles and
66-pixel world body viewport mapped to the 144-pixel animation canvas. Other
render scales must be derived from their actual world-to-screen transform.
The default presentation is slowed for visual inspection, not a timing capture
of a live game session.

This demonstrates continuous rightward walking. Stops, slopes, direction changes,
collisions, uneven terrain and final skinning/cloth physics are not demonstrated.
Those would need their own implementation and validation if this direction is
chosen. Automated export checks verify constant contact X throughout each stance.

The proposed upper body uses its own `ConceptArmRig`: pelvis -> chest -> neck ->
head, with shoulder -> elbow -> wrist -> hand chains inheriting chest motion.
The chest rotates slightly without scaling; the head counter-rotates to stay
steady. Cloth retains its separate delayed motion. The Mage side-view source
has calibrated shoulder/wrist positions and a complete hand mask; its hidden far
arm is not extracted from front clothing. This corrects the duplicate hand in
the study. These are approximate source-art masks, not a finished rig for every
actor and direction; the study covers Mage, Seraphine and Knight walking right.

Export checks also verify constant upper-body bone lengths across all frames
and exclusive ownership of the Mage's finger/knuckle source region. Browser
checks cover the embedded tab, keyboard navigation, all actors/cadences,
frame scrubbing, skeleton controls, stable iframe sizing and mobile layout.
Run `python source/check_preview.py` on Windows with Chrome to repeat them.

Rebuild by compiling the Java files in `source/` alongside the main game
sources into a separate output directory, then run
`com.alderfall.game.ExportMovementConcept` from `Java/`. These prototype sources
are intentionally outside the game's source tree.


Cutout refinement
-----------------
`ConceptPartMasks` gives the Knight and Seraphine separate, continuous cape
layers in the calibrated right-facing sources. These layers render behind the
legs and stay attached to the upper body, rather than passing through the
original generic leg-removal mask. Arm extraction excludes the cape. Both
actors have adjusted shoulder/elbow/wrist positions, and Seraphine's hand mask
excludes the exposed thigh beside it. Small elbow/wrist underlaps close rotation
seams without duplicating the whole hand. Hidden far arms stay occluded.

`cutout-review.png` compares source artwork with two phases of the revised
animation. Export checks require a connected main cape throughout every frame
at all three cadences. These masks are calibrated for these study sprites;
additional actors and directions still need their own source-art review before
a full game rollout.
