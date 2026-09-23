# Monster sprite review — 2026-09-22

Reviewed 58 unique static sprites used by 80 monster catalog entries, including story monsters, all nine goblin-family sprites, and all three bandit sprites. Replaced 27 static sprites and the skeleton attack strip. The other 31 static sprites and the spider strip artwork remain unchanged. No unused assets were moved in this pass.

## Repairs

- Restored opaque green skin, scales, and clothing where background removal had punched transparent holes through characters.
- Removed stray neighboring artwork, color spill, and cutout residue; restored clipped extremities and transparent margins.
- Brought the oldest goblin, orc, and bat closer to the newer sprites' palette, shading, and level of detail.
- Preserved role-specific weapons, shields, traps, spell effects, and intentional wing tears while making the goblin and bandit families more consistent.
- Rebuilt the skeleton's six attack poses to remove duplicated limbs and detached weapon fragments.
- Declared six frames explicitly for both attack strips. This also corrects the spider strip's previously incorrect inferred frame count at some display proportions.

The repaired static sprites are: goblin, orc, bat, ash_scorpion, hobgoblin_guard, orc_shieldbearer, stone_giant, elder_dragon, red_dragon, mountain_drake, shadow_beast, crypt_bat, goblin_scout, goblin_archer, goblin_trapper, goblin_skirmisher, goblin_shaman, goblin_warlord, goblin_king, bandit_cutthroat, bandit_archer, bandit_captain, orc_raider, orc_berserker, orc_shaman, swamp_troll, and marsh_drake.

Named bandits reuse the existing family artwork: masked_trail_hunter uses bandit_archer, contract_knives_agent uses bandit_cutthroat, and velvet_room_assassin uses bandit_captain. They receive the repaired sprites through those references.

## Animation support

AssetStore now accepts optional `.framebounds` metadata defining a shared viewport within each animation cell. The skeleton strip uses this to retain consistent scale and foot placement despite the generated strip's transparent padding. Missing or invalid metadata falls back to the full cell. Format documentation is in `Java/assets/characters/shared/animations/README.md`.

## Verification and evidence

- Full Java source and the three review diagnostics compiled with JDK 21.
- All 80 catalog entries resolve to 58 loadable, renderable static sprites.
- Both attack strips have six frames and render at three display proportions; four invalid viewport metadata cases pass fallback checks.
- Actual battle previews were captured for goblins, goblin leaders, bandits, and orcs.
- Hash checks confirm 28 byte-exact archived image originals, the accepted replacements, and 31 unchanged static sprites. See `verification.json`.

Start with [goblin and bandit family](goblin-bandit-family.png), [broader family comparison](family-consistency.png), and [attack animations](animation-runtime.png). `runtime-before-after-1.png` through `runtime-before-after-6.png` compare originals and repairs on dark/light backgrounds and at gameplay size. Battle captures are in `scenes/`.

Original files are under `before/Java/assets/`. Generation prompts, manifests, baseline inventories, diagnostic sources, and logs are retained here. Accepted source masters are under `Java/assets/source/monster-integrity-2026-09-22/` in the project root.
