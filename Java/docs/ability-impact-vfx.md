# Ability impact artwork

## Class ability expansion

Settings → Combat includes an animation speed slider from 0.5× to 3× in 0.1× steps. The saved `combatAnimationSpeed` preference defaults to 1× and applies to the next action; cast, travel, and impact durations scale together while collision-triggered damage stays synchronized. An action already playing retains its timing.

The class atlas set uses 19 sheets, with a separate cell for each of 365 named abilities and class actions. The ten retained dedicated impacts below remain separate. The completed continuation adds seven sheets (137 cells): Stonebreaker, Nightblade, Grovekeeper, common abilities, and three sheets covering basic/heavy/cleave/guard/dodge actions for all 15 classes.

These new PNGs are stored in `assets/effects/fx_class_{stonebreaker,nightblade,grovekeeper,common,attacks_1,attacks_2,attacks_3}_atlas_v3.png`. They were generated using the built-in imagegen tool; exact base prompts and the cell mapping are in `assets/source/class-vfx/`. Each base prompt used this additional production instruction: "Production requirement: preserve the exact specified grid including blank cells; fully transparent alpha background. Textured hand-painted pixel art matching an earthy fantasy RPG. Every ability has a unique readable silhouette."

Effects appear progressively after collision: stone and roots emerge in staggered columns, shadow ribbons gather before sweeping away, healing flows upward, and wards assemble in sections. Fire grows from a compact burst into dispersing embers. These are renderer-driven animations of generated artwork, not pre-rendered animation frames. Area impacts use the affected formation bounds, and restorative effects play on individual recipients.

`CombatGraphicsTest` checks sheet availability, unique occupied cells, alpha transparency, class/skill-tree coverage, gradual appearance and dissipation. Its `exports/combat-graphics/class-ability-process.gif` shows Worldsplitter, Primeval Bloom, Moonless Verdict, and Signal Flare in motion; `class-process-phase-*.png` captures intermediate frames.

Generated with the built-in imagegen tool. Production PNGs live in `assets/effects/fx_impact_*_v2.png`; the complete prompts are preserved in `assets/source/vfx-impact-prompts/`. Generated alpha is preserved. Existing projectile artwork is separate from these new collision assets.

| Ability / state | New artwork |
| --- | --- |
| Root Memory | Amber ancestral root cradle, teal sap and ivory leaves |
| Mend | Rising golden ribbons and luminous stitching |
| Grove Hymn | Fern wreath, petals and ascending pollen |
| Seraphic Hymn | Textured ivory feather wings and a gold halo |
| Thorn Lash | Jagged wood tendrils and broken earth |
| Briar Tempest | Wide bramble eruption |
| Glacier Prison | Chipped ice eruption and snow |
| Ley Detonation | Fractured purple rune stone and arcane filaments |
| Inferno Script | Broad flame crown and embers |
| Firebolt | Irregular textured flame explosion |
| Frozen | Dedicated hollow frost shell, independent of attack artwork |

The later artwork uses `bramble_boar.png` and `fx_firebolt_unique.png` as style references: textured pixel clusters, earthy shadows, irregular silhouettes and selective highlights. This replaces the flat procedural Firebolt burst and frozen polygon. Each listed impact has a distinct file; no new effect borrows another ability's impact image.

`AbilityImpactArt` renders phased animation from the generated textures. Staggered vertical sections reveal roots and ice from the ground. Healing bands gather at the feet, flow upward and drift away. Fire and arcane textures assemble rapidly, then separate into independently moving fragments; ice breaks apart after growing. Pixel motes continue moving through the tail of each impact. The frozen status shell grows onto a newly hit target. Raster cache sizes remain fixed. Area art spans the affected formation; multi-projectile fallback hits are enlarged independently at each actual collision. No area damage or additional targets are introduced by the visual changes.

The animation is assembled by the renderer from each impact's source artwork, rather than a hand-drawn sprite sheet. Impact phases last roughly 0.7â€“1 second. `exports/combat-graphics/ability-process.gif` is a slowed four-effect motion review rendered by the same code used in game.

Healing now preserves its configured visual identity and exposes every recipient to the animation. Party healing resolves once at the shared collision, rather than once per visual target. Impact durations allow the new art to remain readable. Frozen retains its existing weakening semantics.

Validation: compile all Java sources, run `CombatGraphicsTest` and `SmokeTest`. Combat graphics diagnostics cover collision timing, party healing recipients and single resolution, status expiry/cleansing/death, distinct transparent assets, and formation-wide sizing. Captures are written to `exports/combat-graphics/impact-*.png`.

