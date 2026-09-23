# Character animation refresh

The v4 pass keeps the detailed pixel-art style. New bitmap assets were generated
with the built-in imagegen tool using existing character art as references.
All accepted sources and runtime assets are saved in this workspace.

## Size, standing and movement

All directional world sprites use a shared 192 x 144 canvas, 132-pixel body
height and 138-pixel ground baseline. The renderer reserves width for equipment
instead of shrinking side views to fit. Original adult player standing models
replace the previously oversized-headed replacements. Eight ordinary-town models
receive a smaller-head/body proportion correction; named adventurers retain their
existing proportions. NPC chore poses use the same viewport and display height.

Standing idle has 24 procedural breathing frames with planted feet, across all
47 character types. Stopping blends the current stride pose back to standing.

The five player classes and nine companions have 16 newly drawn right-facing
walking poses, interpolated into 32 runtime frames. Left views mirror those poses;
diagonals use the corresponding side. Front/back views and other NPC walking
retain native art with procedural leg articulation. These are not 32 separately
drawn poses for every direction. Feet alternate contact, knees lift on the return
step, and interpolation does not crossfade two translucent bodies.

Gameplay advances stride phase from actual interpolated world distance: one cycle
per 0.75 tile. Players, followers and NPCs share this policy. Blocked movement
cannot keep walking in place; teleports reset cadence. Extra tile-based bobbing
is disabled for these sprites.

## Combat and asset locations

Each of the fourteen party actors has 24 drawn poses for its primary attack,
bow shot or spell cast. Secondary actions retain procedural animation. Battle
timing plays anticipation, release and recovery once, with pose 15 aligned to
projectile launch. Sword/shield characters use one sword hand and one shield arm;
Maera supports her spellbook throughout casting.

- `pass-v4/sources/`: accepted combat and walking atlases for all fourteen actors.
- `pass-v4/prompts.json`: generation prompts and reference paths.
- `pass-v4/corrections.md`: targeted corrections and rejected-layout notes.
- `ready-v4/`: 256-pixel combat cells, first-pose models and 24-frame metadata.
- `walk-ready-v4/`: 192-pixel walking cells and 16-frame source metadata.
- Older v2/v3 folders remain available, but v4 party side-walk and combat assets
  take precedence at runtime.

From `Java/`, rebuild accepted assets and source review sheets with:

```powershell
java tools/assets/characters/ImportCharacterPass.java --complete
java tools/assets/characters/ReviewCharacterPass.java
```

The importer follows transparent gutters, removes chroma backgrounds and detached
annotations, preserves shared scale and aligns foot baselines. It requires all
28 source atlases in complete mode.

## Review and validation

Open `Java/tools/reviews/characters/index.html` from the repository root. It runs locally
without a server and supports standing, walking, combat, party/town/all filters,
eight directions, playback speed and frame scrubbing. World preview strips come
from the actual runtime renderer. `world-poses.png`, `player-proportions.png` and
`npc-proportions.png` compare standing and stride poses at consistent scale.

With compiled classes on the classpath, run these package entry points:

- `com.alderfall.game.WorldPoseTest`: equal-distance cadence, blocked movement,
  teleport reset, settling and visible planted-foot idle across 47 actor types.
- `com.alderfall.game.LocomotionTest`: 112 party directional cycles, fixed scale,
  clipping, distinct frames and repaint stability.
- `com.alderfall.game.WalkingStrideTest`: alternating foot lead, returning-foot
  clearance, level planted feet and rigid boots.
- `com.alderfall.game.CharacterAnimationTest`: fourteen 24-pose primary actions,
  transparency, caches, release timing and secondary-action coverage.
- `com.alderfall.game.CharacterAnimationAudit after`: regenerate 376 directional
  walking and idle review strips, roster data and the coverage audit.
- `com.alderfall.game.SmokeTest` and `com.alderfall.game.CombatGraphicsTest`:
  gameplay flow and combat rendering, including class spell-effect coverage.

The local browser check additionally verified image loading, live playback,
scrubbing/resume and all town-NPC directions in the review page. Procedural
front/back and town walking remain a different source-art quality tier from the
newly drawn party side strides.

## Movement selection

The current default uses two complete jointed legs with fixed thigh/shin lengths,
forward-bending knees and separate clothing motion. The anatomical hip stays at
55% of body height regardless of robe length. Front/back feet stay in separate
lanes; side views use stable near/far layers with a darker rear leg. Hidden limb
surfaces reuse the standing sprite's visible leg/boot texture. This reconstruction
is approximate; it is not a separately drawn anatomical rig for every character.

The former cutout split a standing image into left/right sections and compressed
those sections vertically to lift a foot. That could shorten legs, split a robe
or lose the hidden leg entirely. The new walk does not interpolate between sprite
drawings or compress a leg to produce its return step. Interpolation remains only
in the short transition back to standing.

All three implementations and their source assets remain available:

- `Java/scripts/run.ps1`: current jointed movement (also the default for Main).
- `Java/scripts/run-articulated-movement.ps1`: explicitly select the current mode.
- `Java/scripts/run-alternate-movement.ps1`: previous cutout movement.
- `Java/scripts/run-original-movement.ps1`: original drawn/interpolated movement.

`-Dalderfall.movementStyle=articulated|alternate|original` selects a mode for that
session without changing saves. The older alternateMovement true/false flag is
still accepted when movementStyle is absent. All modes retain distance-driven
cadence, standing idle and their own transition poses/caches.

In the review page select **Walking - jointed legs (current)**. The previous
cutout and original walking options remain beside it. Runtime strips are in
`walk_articulated/`, `walk_alternate/` and `walk/`, respectively, across 47 actors
and eight directions. `articulated-review.png` focuses on Mage, Cleric, Seraphine,
Knight, Lyra and Samir.

`com.alderfall.game.ArticulatedWalkingTest` checks invariant segment lengths,
forward knee bending, independent return lifts, separate frontal foot lanes and
settling back to the unchanged standing pose. The previous AlternateWalkingTest
remains available. Regenerate current preview strips using
`com.alderfall.game.CharacterAnimationAudit articulated`.

Standing direction corrections: Rafiq, Samir and Vesper had reversed left/right
labels. Runtime source mapping corrects those poses independently of the already
correct v4 walking strips. Rafiq down-right and Vesper up-right use mirrored
left counterparts. CompanionFacingReview rebuilds the affected idle and both
walking previews and writes companion-facing.png for visual direction review.

## Arm skeleton and diagonal locomotion

The current movement now includes rigid upper-arm, forearm and hand layers with
shoulder/elbow/wrist joints. Arms counter-swing against their corresponding legs.
Hand pixels rotate together; explicit grip regions cover Rogue's knife, Calder's
hammer, Samir's lantern and detected Cleric staves. Bow-carrying front/diagonal
poses use a smaller swing. The underlying flat art still requires approximate
part masks and reconstruction of body areas hidden by arms; these are not fully
hand-authored layered models.

The old waist/hand displacement has been removed. Chest and abdomen retain their
width, with only whole-body vertical bob; cloth sway begins below 60% body height.
Source parts and joint lengths are constant through the loop.

Diagonal stance phases now move each planted foot backwards along both components
of the travel direction. Reviewed rear diagonals for the five player classes, Aria and
Cassia had reversed labels. Several front-right poses also faced left; runtime
mapping corrects them without editing the source files. Both older walking modes
remain available; the current default is still the articulated mode.

ArmSkeletonTest validates fixed arm lengths, wrist continuity across the loop,
arm/leg counter-swing, rigid abdomen pixels and diagonal stance direction. It
exports arm-rig-review.png (rendered poses beside joint overlays) and
diagonal-facing-review.png. The preview links to both sheets.

## In-game walking speed control

Settings > Gameplay > Walking Animations includes a 0.5x-2x cadence slider
and a Reset button. The default remains 1x. Changes apply on the next movement
sample to players, companions and NPCs without resetting stride phase. The
walkAnimationSpeed setting persists in config/settings.properties. Travel speed,
combat timing and standing idle are independent. WalkAnimationSpeedTest covers
cadence scaling, live changes, blocked/stopped behavior and settings persistence.
