# Dialogue animation framework

The companion/story renderer composes independent animation layers on the
existing painted cutouts. The world walking rig and the movement-physics study
have different poses and masks; dialogue profiles are calibrated separately.

## Layers and ownership

| Layer | Driver | Scope |
| --- | --- | --- |
| Breathing | Continuous clock, per-character phase and period | Small chest/shoulder motion; continues after text stops |
| Blinking | Continuous clock with staggered character phase | Eyes, independent of gestures and speech |
| Sway | Slow continuous clock | Whole-figure weight shift; local foot regions stay planted |
| Speech | Visible text snapshot | Mouth closes on narration, punctuation, completion and skip |
| Gestures | Interpolated speaking/listening keyframes | Neck and chest rotations, with no independent head translation |
| Hands | Damped gesture channel | Only explicitly mapped free hands; no guessed hidden hands or moving grips |
| Hair and cloth | Seven-node damped spring chains | Pinned roots, interpolated displacement, bounded calibrated regions |
| Loose equipment | Damped angular spring | Rotation around an authored attachment point |
| Supported soft tissue | Heavily damped, very small displacement | Explicit outfit opt-in; no default physics on armour |

`DialogueAnimationLayers` is the reusable driver. Its fixed 120 Hz simulation
step is separate from the render rate. It resets after a long gap, so reopening
a conversation cannot accumulate a large physics impulse. Repainting the same
timestamp does not advance the springs. The layer state is transient, bounded
to four recently used figure rigs, and is not stored in saves.

The hair/cloth solver follows the design of the movement-physics simulator:
seven coupled horizontal nodes, a pinned root, damping and continuous sampling
between nodes. Dialogue uses conservative authored masks and displacement
limits. Body and leg capsules constrain new horizontal node crossings in bind
space; existing painted overlaps remain intentional. The parent body transform
carries nodes and colliders together. This is not mesh self-collision, scenery
collision or hidden-surface synthesis. Larger gestures require new cutouts and
collision review rather than increasing the amplitudes blindly.

## Character calibration

`DialogueRigProfile` owns the art-specific data. All coordinates refer to the
alpha-cropped source at a normalized height of 1000. The renderer maps them to
the fitted sprite's actual origin and scale.

A lip profile has left corner, central seam and right corner coordinates. A
quadratic passes through all three points, preserving a sloped smile such as
Aria's. The opening and both painted lip borders use the same curve. This
replaces local dark-pixel fitting for calibrated characters, which could select
lip shading below the real seam and create an apparent second mouth.

Attachments specify a type, pivot, maximum motion, feather width and polygon.
`DialoguePartMask` rasterizes ownership once per fitted source. Overlaps have a
single owner, boundaries feather to zero, and each region has its own pivot.
Angular limits are degrees; supported-soft-tissue limits are normalized source
pixels. Hair/cloth tip displacement is additionally capped at 2.5 times the
profile limit, decreasing toward the pinned root.

Current enhanced profiles:

| Character | Calibrated attachments |
| --- | --- |
| Aria | Left hair, belt leaves, free left hand, separate supported left/right breast springs, long cape hems |
| Vesper | Right hair, loose feather edge, waist pendant, free right hand, separate supported left/right breast springs, long feathered mantle |
| Mage | Hanging focus, free right hand, cape edge and long robe panels; short hair follows the head |

The remaining figures retain breathing, blinking, sway, speech and head/chest
gestures. Their lip seam still uses the earlier estimate, and they receive no
guessed hand, breast or equipment physics. A new profile must be reviewed on its
actual artwork before enabling attachments. Plate-armoured characters have no
soft-tissue attachment by default. The hand holding a staff, bow or book must
not be marked as free; animating a grip requires mapping the held item with it.

## Rendering and game integration

`DialogueRenderer` passes one text-reveal snapshot to the speech and gesture
layers. It identifies the player as the listener and gives each figure the
direction of its partner. Idle channels use the continuous animation clock,
not the length of the spoken line.

`DialogueBodyMotion` defines the ground/pelvis/chest/neck/head hierarchy,
foot nodes and footprint constraints. Full companion/story dialogues draw the
whole rig. `drawPortrait` crops the same animated source to head and shoulders,
and accepts the same speech/listening inputs for party banter. `PartyPortraitRenderer`
uses the actual dialogue sprite for the travel sidebar and party-screen portraits,
bypassing `AssetStore.portrait`'s preference for a separate legacy portrait asset.
Sprites without dialogue artwork retain the existing static fallback.

Breathing, blinking and gentle motion continue while party cards are visible.
Banter gives the matching speaker a bounded mouth/gesture window (1.5–12 seconds,
based on line length), then returns to idle while response options remain visible.
The banter text remains fully displayed; this is reading-duration animation, not
text-reveal synchronization or audio lip sync. Animation instances are bounded
and only the portrait viewport is deformed, using a cached 384x768 source fit.

Travel prompts render after the sidebar and point sideways to the speaking
actor's card. The card is highlighted, the sidebar follows its page, and paging
is temporarily disabled while that actor's prompt is active. Reply actions and
tooltips retain the existing banter logic. Prompts are hidden when their portrait
is absent, the UI is hidden, or a blocking gameplay/menu mode is displayed.
Bubble placement uses logical UI coordinates so resizing does not stretch the
tail away from the card. `PartyPortraitHudTest` checks art selection, both speaker
anchors, compact-HUD paging, response hit targets/actions, and prompt cleanup.

The existing source images remain unchanged. Mouth/blink poses are cached.
Body, head and attachment displacements are composed at adaptive mesh vertices.
Java2D textures the resulting affine patches with native cubic interpolation;
nearly affine quads use one draw, curved cells use two triangles. Cells subdivide
where sampled mapping error exceeds 0.1 source pixel (subject to a minimum cell
size), with mandatory finer coverage over calibrated attachment regions.
This avoids a succession of whole-image transforms. Pixels outside the
affected region are copied directly; authored foot regions remain pixel-identical.
The lower body follows the ground-root weight shift, with a smooth falloff
into each foot pin. Long garment chains move independently of the stance.
The preview also retains the native 480x800 figure resolution.

The renderer skips deformation entirely when all body channels are disabled.
Portraits only deform the visible crop; changing the viewport invalidates the
rendered cache. Face poses retain their existing local eye/lip edits and cache.
Both the native source texture resolution and per-repaint motion are preserved.

The full-scene headless benchmark measured 60.11 ms per frame with the mesh
versus 102.23 ms with the dense reference (1920x1080, world and dialogue UI,
120 warm-up frames, 60 samples). This is about 41% less render time, but still
above the game's 50 ms timer budget before updates/display presentation.
The dense implementation is available for diagnostics with JVM flag
`-Dalderfall.dialogueDenseReference=true`; the mesh is the default.

The [framerate investigation and remediation](dialogue-animation-performance.md)
record the original bottleneck, comparisons and remaining performance limits.

Use `DialogueAnimationLayers.Settings` when constructing an animation instance
to enable or disable channels. `STANDARD` enables the calibrated layers;
`REDUCED` retains breathing, blinking and speech while disabling sway, gesture
tracks, hands and secondary motion. The default constructor also accepts JVM
flag `-Dalderfall.dialogueReducedMotion=true`. No gameplay or save data depends
on these presentation settings.

## Review and validation

Open `Java/tools/reviews/characters/index.html#dialogue-motion`. **Source rig guides**
shows lip anchors, attachment boundaries and pivots on the original artwork.
**Secondary physics** compares the same pose/hand rig with and without the
secondary solver. It retains the selected frame. Characters without enhanced
profiles use the same frames for either setting.

The four-second preview samples 96 actual runtime frames at 24 fps; runtime
pose interpolation runs on every repaint. It is a sample, not a seamless
physics loop. Use slow playback to inspect lip borders, wrists, hair edges and
equipment roots; keep a full-figure view for foot contact.

Tests: `DialogueMeshTest`, `DialogueAnimationLayersTest`, `DialoguePoseTrackTest`,
`DialogueAnimationTest`, and the `DialogueMotionReview` exporter. They check
lip-curve anchors, continuous idle, bounded springs, pinned chain roots,
fixed-step stability, reduced motion, unmodified source pixels, planted feet
and stable repeated repaints. The browser smoke test checks local-file loading,
the merged tab, frame controls, physics toggling and mobile layout.
