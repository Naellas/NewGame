# Monster ability VFX review

Open `../index.html`, select a monster family and Combat, then choose an ability on each card. Ability VFX is enabled by default. Disable it for the original full-size sprite review. Pause, speed and frame scrubbing remain shared. The player-team target is on the left; self-buffs stay on the monster. Named variants sharing a sprite retain separate ability choices. Actual bosses also expose their phase abilities.

`ability-audit.tsv` records the catalog ability, original gameplay effect, selected artwork and contact/projectile/self presentation. Damage, status chances, target selection and sound keys are unchanged. Named examples include poison Bog Breath, bone Bone Rattle, web Web Spit, shield-ram Shield Slam, water Current Lash, lightning Numbing Coil, snare Pinning Shot, and impact Treeclub Smash. Regeneration uses healing artwork; defensive shells and wards stay on their caster.

All art reuses existing `Java/assets/effects/animations` strips. `MonsterAbilityVfx` defines mappings and draws deterministic charge, travel and collision frames in both battle and the preview export. Directional projectiles use the renderer's existing native-angle calibration. Contact attacks have no flying projectile. Buffs hold the monster ready pose.

From `Java/`, compile the game and run `com.alderfall.game.MonsterVfxReview`. This verifies each mapping through the real battle scheduler, checks effect assets and buff targeting, and exports `../monster-vfx.js`, the audit, verification result and 24-frame transparent VFX strips. Browser playback uses normalized wind-up/release/impact timing; battle retains the monster's own cast and travel durations.
