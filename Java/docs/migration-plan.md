# Java Migration Plan

Last updated: 2026-05-26

## Current Migration Snapshot

The Java port has moved beyond the initial spine. It now has a broad Swing-based playable slice, with several newer systems in active integration.

Current build note:

- `.\scripts\build.ps1` compiles successfully when run with a per-process PowerShell execution-policy bypass on this machine. Use `powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1` if direct script execution is blocked.

## Completed Or Mostly Completed

The first pass creates a runnable Java spine instead of a literal line-by-line rewrite:

- Core config constants and gameplay chances loaded from the Python config shape.
- Terrain/passability names and asset names.
- Player classes, abilities, actor stats, enemy catalog, XP, gold, and level-ups.
- Overworld generation with landmarks, roads, settlements, dungeons, and biome painting.
- Generated city, village, dungeon, and house-interior map areas.
- Map transitions for settlements, dungeons, exits, and Stonegate depth changes.
- Deterministic prop generation for overworld and interior maps.
- Terrain variants, natural edge blending, connected road rendering, settlement overlays, cloud layer, mini-map, zoom controls, and mouse-first sidebar UI.
- Asset loading from copied Java-side assets, with runtime folders mirroring the Python asset categories.
- Exploration, encounters, class selection, keyboard movement, clickable adjacent-tile movement, and map interaction.
- Core NPC data placed in cities and villages.
- Dialog, quest acceptance, progress tracking, completion rewards, quest log, and quest persistence.
- Consumable items, inventory, basic shops, equipment catalog, and shop purchases.
- Recruit data, hire/quest reward recruitment hooks, party display, and recruit save/load.
- Java-side save/load under `Java/saves/save.properties` for class, map, position, player stats, inventory, quest state, recruits, and zoom.
- Build, run, and smoke-test scripts.

## Currently Being Worked On

These areas have visible Java code, but are not yet done enough to mark stable:

- Battle expansion: backdrop selection, battle action bar, item use in battle, floating text, hit flashes, shake/lunge effects, status effects, enemy special abilities, cooldowns, buffs, debuffs, and direct-damage-over-time effects are present.
- Party battle support: recruits can be added to `GameState`, saved, shown in the sidebar, passed into battle, and commanded on their own party turns.
- Skill trees: common and class-specific trees, skill points, allocation checks, respec cost, stat effects, and ability unlock synchronization are present in state/data, but the skills overlay and input path are not wired in `GamePanel`, and save/load does not yet persist skill points or allocations.
- Equipment: equipment definitions and actor equip/unequip behavior exist, shops can stock equipment, and inventory can equip items. The inventory UI still treats entries mostly as consumables, shops only render item rows cleanly for consumables, equipped slots are not surfaced, and save/load does not persist equipped gear.
- Status effects: status data, effect stacks, ability status hints, and enemy status abilities exist. This is still mid-port until the battle UI renders statuses clearly and the balancing pass is complete.
- Richer battle controls: the clickable action bar is present for active party actors, with keyboard fallback still limited to the first three abilities.

## Partially Completed Feature Areas

- Recruits and party members: data, hiring, quest joins, sidebar display, persistence, and commanded party battle turns are present; targeting depth and balance still need Python parity.
- City building doors and interiors: generated house interiors and city door markers exist; richer, Python-parity population and interior interaction remain.
- Prop clustering and location patches: deterministic props are present across maps; more specific farmland, camp, graveyard, and location patches still need parity work.
- Equipment/inventory: data model and equip logic are present; UI, persistence, shop display, and battle-use rules need completion.
- Skill tree: data model and allocation/respec state logic are present; UI and persistence need completion.
- Status effects and battle AI: effect model and enemy special move data are present; final UI, party interaction, and balancing remain.
- Asset variant/tint/cutout pipeline: Java consumes copied runtime assets and variants; Python extraction/generation tools remain the source of truth.

## Recommended Library Path

Short term:

- Keep Java 21 plus Swing for bootstrapping.
- Keep asset reuse from `../Python/assets` to avoid duplicating hundreds of PNGs.
- Keep tools in Python until the runtime migration is further along.

Medium term:

- Add Gradle once dependency management is needed.
- Move rendering/input/audio to libGDX.
- Keep game state, combat, world, save, and content APIs plain Java so they are not coupled to the renderer.
- Expand `MapArea` content before moving to a heavier renderer so the engine swap does not blur feature work with framework work.

Later:

- Move authorable content to JSON.
- Add schema validation for classes, items, monsters, and quests.
- Port the Python asset extraction/generation tools only if they still matter after the art pipeline settles.

## Python Feature Areas Still To Port

- Expand party-member battle targeting, balancing, and UI polish.
- More faithful city building doors and fully populated house interiors.
- More detailed prop clustering and location patches such as farmland, camps, and graveyards.
- Richer dialog/shop controls, including equipment listings and sell/compare/equip flows.
- Full skill tree overlay, skill allocation input, respec UI, and skill persistence.
- Equipment slot UI, unequip controls, equipment persistence, and shop equipment display.
- Full Python battle AI parity, party targeting, status readability, and visual effects.
- Music playback and transitions.
- Full asset variant/tint/cutout pipeline.
- Settings screen and render-mode controls.

## Next Stabilization Slice

1. Run `powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1` after each battle/party/status change.
2. Decide how deep the next party battle slice should go: passive allies, automatic ally turns, or full ally command selection.
3. Wire the skills overlay into the sidebar/keyboard path and persist `skillPoints` plus `skillAllocations`.
4. Surface equipped weapon/armor/accessory slots in inventory and persist equipped gear.
5. Update the smoke test to cover one recruited ally, one equipment purchase/equip, and one skill allocation once those paths compile.
