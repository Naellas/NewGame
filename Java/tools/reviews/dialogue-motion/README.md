# Dialogue motion review

Framework details and calibration instructions:
[`Java/docs/dialogue-animation-framework.md`](../../../docs/dialogue-animation-framework.md).

Open the **Dialogue animation** tab in `../characters/index.html`, or use the
direct `#dialogue-motion` link. The embedded preview loads on first use, adapts
its height to face/full-figure views and pauses while another tab is active.
`index.html` also remains available as a standalone preview.
The preview uses exported production Java frames, not a separate browser rig.
Frames now use the adaptive mesh renderer with native cubic filtering and the same
full-resolution textures and physics. See the [performance report](../../../docs/dialogue-animation-performance.md).

Coverage: all 26 dialogue cutouts, 96 frames each at 24 fps. Face and full-figure views
compare the original artwork with speaking/listening motion. Slow playback,
frame stepping and alignment guides expose face drift and equipment distortion.
Players listen; NPCs speak, listen, then settle. The four-second sample restarts
at its end, so its loop boundary is not a continuous conversation transition.

The mouth separates the painted lips around a visible opening that follows the
original lip seam. Timed head poses use cubic interpolation on every in-game
repaint, without crossfading face images. The head rotates together rather
than stretching its cheeks around a stationary edge. Head keyframes have no
independent position offsets: waist/chest and neck pivots produce attached
rotational gestures, replacing the sideways drift. Articulation covers head,
neck and shoulder motion and an inward attention turn. It does not provide
phoneme-accurate lip sync or profile-view rotation. Aria, Vesper and Mage now
have explicitly mapped free hands, loose equipment and hair/clothing regions.
The rest of the roster retains the base idle/speech rig pending calibration.
World-sprite limb masks are not applied to these differently posed cutouts.
Full-figure exports use native 480x800 frames; the earlier half-resolution
downsample/upscale has been removed. Breathing no longer rescales the whole
character, preserving sharp weapons, clothes and hair outside the moving area.

Breathing, blinking and gentle sway run in parallel with speech. The new
**Secondary physics** checkbox compares the same rig with and without spring
motion, retaining the selected frame. Hair/cloth use pinned seven-node chains
like the unified movement-physics study; secondary chest displacement is
subtle, damped and enabled only for calibrated supported outfits. **Source rig
guides** displays the authored lip anchors, attachment polygons and pivots.
The full-body view is the default. Local foot pins keep contact fixed while the
pelvis/chest/neck/head hierarchy carries sway through the figure. Long garment
chains use body/leg capsule limits; source guides display those capsules and foot
regions. Aria and Vesper have separate left/right breast springs with outfit
support limits. There is no general mesh collision solver or automatic hidden-limb
extraction. The party detail portrait crops this same runtime rig.

Rebuild from `Java`, after compiling the game sources:

```powershell
java '-Djava.awt.headless=true' -cp out com.alderfall.game.DialogueAnimationTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.DialoguePoseTrackTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.DialogueAnimationLayersTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.DialogueMotionReview
```

The export checks unchanged source pixels, pixel-identical foot contact on every frame, lower-body motion, upper-body movement, and stable repeated repaints. It reports renderer
timing including cold pose caches, not complete in-game frame times.

From the repository root, `python Java/tools/reviews/dialogue-motion/check_preview.py`
checks all characters, local-file image loading, face/body switching, stepping,
scrubbing, playback and mobile layout using installed headless Chrome.
