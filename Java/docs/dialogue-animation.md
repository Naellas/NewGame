# Dialogue figure animation

See [Dialogue animation framework](dialogue-animation-framework.md) for layer
ownership, profile calibration, secondary physics, settings and rollout scope.

Companion and story conversations animate the large standing character cutouts
in `DialogueRenderer`. Both the NPC and player breathe and blink independently;
only the NPC moves their mouth as their speech is revealed. Narration, whitespace,
punctuation, completed lines and instantly revealed lines keep the mouth closed.
The small ordinary NPC cards and travel banter UI retain their existing rendering.

`DialogueFigureAnimation` uses a monotonic clock for a subtle 4.2-second breath,
a 170 ms blink with character-specific timing, and three mouth poses. Breathing
is anchored at the bottom of the figure. Facial landmarks are calibrated for
the 26 current dialogue cutouts, in alpha-cropped coordinates normalized to a
height of 1000. New or replaced artwork needs its own landmark review; unknown
sprites retain their original artwork without guessed facial features.

Lip poses separate the painted upper and lower lips, preserving their color,
corners and shading. A narrow opening follows a smooth line fitted to the
original mouth seam, with a shadow sampled from that seam. Aria, Vesper and
Mage now use three explicit seam anchors; other figures retain estimated seams.
It is not an oval
stamped over the face. Two opening amounts alternate with the closed source.
Blinking uses sampled eyelid colors. The original images are never modified.
A bounded cache holds the derived poses, and the same reveal snapshot drives
both visible text and the speaking pose.

Speaking also drives restrained head nods, neck tilts and shoulder gestures.
The listener occasionally turns inward toward the speaker; the speaker glances
toward their partner. Motion eases into and out of speech independently of each
mouth closure. Interpolated head poses approximate an inward turn; it is not a
3D rotation or a new profile view. The whole body sways around the ground root while local foot regions stay planted. Aria, Vesper and Mage have calibrated free-hand gestures
and bounded secondary attachments. Additional arm gestures need their own
dialogue-art masks; the world walking masks cannot be reused at these poses.

`DialoguePoseTrack` supplies timed anticipation, nod, glance and recovery
keyframes. These are rotation tracks: head position offsets are zero. The chest
pivots at the waist and the head inherits that motion before rotating at the
neck, so gestures follow attached arcs rather than sliding sideways. Cubic
interpolation evaluates their transforms at every repaint;
there is no 50 ms pose hold or image crossfade. The face follows one rigid
rotation about the neck, feathered into surrounding hair and clothing. Speech
and listening transitions blend over 220 ms. Four recently used rigs are kept.

Breathing and a slow sway continue independently of speech, with distinct
character phases. Hair/cloth attachments use pinned seven-node spring chains,
following the movement-physics simulator's approach. Loose equipment and
supported chest regions use bounded damped springs; uncalibrated and rigid
outfits get no guessed secondary motion. See the framework guide for limits.

Breathing is local to the upper body; the complete figure is no longer scaled
and resampled every repaint. Local foot regions are copied unchanged, while lower-body weight shift and
calibrated garment chains extend motion through the full figure. Composed cubic
sampling preserves detail without crossfading complete images. The selected
party portrait crops the same rig; full dialogues retain the entire figure.

From `Java`, after compiling:

```powershell
java '-Djava.awt.headless=true' -cp out com.alderfall.game.DialogueAnimationTest
```

This checks speech gating, blink timing, and source image integrity, and exports
`temp/dialogue-animation-review.png` with neutral, closed-eye and open-mouth
views for the roster. Existing `MainStoryDialogueTest` and `PartyDialogueTest`
cover conversation progression.

`DialoguePoseTrackTest` checks keyframe continuity, frame-rate independence,
motion between consecutive repaints, and transitions back to rest.

`DialogueMotionReview` exports 96 actual runtime frames per figure at 24 fps to
`Java/tools/reviews/dialogue-motion/`. Open its `index.html` directly, or follow the
link from the main character gallery. It includes face close-ups, full figures,
original-art comparison, slow playback, alignment guides and frame stepping.
The exporter verifies source integrity, upper- and lower-body movement and
pixel-identical foot regions on every frame. Player figures demonstrate listening;
NPC figures demonstrate speaking, listening and settling.
Full-figure frames retain their native 480x800 resolution, matching the source
comparison; they are no longer reduced to half resolution and enlarged again.
