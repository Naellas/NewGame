# Java Migration Plan

Last updated: 2026-05-26

## Current Migration Snapshot

The Java port has moved beyond the initial spine. It now has a broad Swing-based playable slice, copied runtime assets, local gameplay config, and the former Python asset tools staged inside the Java project.

Current build note:

- `.\scripts\build.ps1` compiles successfully when run with a per-process PowerShell execution-policy bypass on this machine. Use `powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1` if direct script execution is blocked.

## Completed Or Mostly Completed

The first pass creates a runnable Java spine instead of a literal line-by-line rewrite:

- Core config constants and gameplay chances loaded from `Java/config/gameplay.json`.
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
- Dialog, quest acceptance, objective markers, progress tracking, completion rewards, quest log, and quest persistence.
- Consumable items, inventory, basic shops, equipment catalog, shop purchases, equipment slots, unequip controls, and equipment persistence.
- Recruit data, hire/quest reward recruitment hooks, party display, and recruit save/load.
- Skill tree overlay, allocation, respec, stat effects, ability unlocks, and skill persistence.
- Settings screen for encounter rates and audio volume, persisted locally.
- Java-side save/load under `Java/saves/save.properties` for class, map, position, player stats, inventory, equipment, quest state, recruits, skills, and zoom.
- Java-side gameplay config under `Java/config/gameplay.json`, with local settings saved to ignored `Java/config/settings.properties`.
- Python asset-generation and extraction tools copied to `Java/tools/` for historical regeneration work after the old Python project folder is removed.
- Build, run, and smoke-test scripts.

## Currently Being Worked On

These areas have visible Java code, but are not yet done enough to mark stable:

- Battle expansion: backdrop selection, battle action bar, item use in battle, floating text, hit flashes, shake/lunge effects, status effects, enemy special abilities, cooldowns, buffs, debuffs, and direct-damage-over-time effects are present.
- Party battle support: recruits can be added to `GameState`, saved, shown in the sidebar, passed into battle, and commanded on their own party turns.
- Skill trees: common and class-specific trees, skill points, allocation checks, respec cost, stat effects, ability unlock synchronization, UI, and persistence are present; balance and presentation still need playtesting.
- Equipment: equipment definitions, actor equip/unequip behavior, shop stocking, inventory slots, equipment rows, and persistence are present; sell/compare flow and deeper battle-use rules remain.
- Status effects: status data, effect stacks, ability status hints, and enemy status abilities exist. This is still mid-port until the battle UI renders statuses clearly and the balancing pass is complete.
- Richer battle controls: the clickable action bar is present for active party actors, with keyboard fallback still limited compared with the mouse UI.

## Partially Completed Feature Areas

- Recruits and party members: data, hiring, quest joins, sidebar display, persistence, and commanded party battle turns are present; targeting depth and balance still need Python parity.
- City building doors and interiors: generated house interiors and city door markers exist; richer, Python-parity population and interior interaction remain.
- Prop clustering and location patches: deterministic props are present across maps; more specific farmland, camp, graveyard, and location patches still need parity work.
- Equipment/inventory: data model, equip/unequip UI, shop display, and persistence are present; sell/compare and battle item targeting need completion.
- Skill tree: data model, allocation/respec UI, and persistence are present; tuning and clearer node affordances remain.
- Status effects and battle AI: effect model and enemy special move data are present; final UI, party interaction, and balancing remain.
- Asset variant/tint/cutout pipeline: Java consumes copied runtime assets and variants; the copied scripts in `Java/tools/` are the retained source for regeneration work.

## Python Folder Removal Readiness

The Java runtime is prepared to run without a sibling `Python/` folder:

- Runtime assets from `Python/assets/` are present under `Java/assets/`.
- `Java/config/gameplay.json` now carries the gameplay tuning values that used to be read from `Python/config/gameplay.json`.
- `Java/tools/` contains the Python asset extraction/generation scripts that were previously under `Python/tools/`.
- `Java/docs/python-development-plan.md` preserves the old Python project development plan for reference.
- `Java/scripts/run.ps1`, `Java/scripts/smoke-test.ps1`, `Main`, `GameWindow`, `GamePanel`, and `SmokeTest` no longer pass or resolve a Python project path.

Do not remove these local-only Python artifacts into Java unless there is a specific reason:

- `Python/.venv/` and `Python/src/**/__pycache__/`: disposable environment/cache data.
- `Python/saves/*.json`: local prototype saves; Java uses `Java/saves/*.properties`.
- `Python/src/` and `Python/main.py`: superseded runtime implementation, retained only until final deletion review.

## Recommended Library Path

Short term:

- Keep Java 21 plus Swing for bootstrapping.
- Treat `Java/assets` as the runtime asset root.
- Keep the copied Python scripts in `Java/tools` only as optional asset pipeline utilities; the Java runtime should not depend on them.

Medium term:

- Add Gradle once dependency management is needed.
- Move rendering/input/audio to libGDX.
- Keep game state, combat, world, save, and content APIs plain Java so they are not coupled to the renderer.
- Expand `MapArea` content before moving to a heavier renderer so the engine swap does not blur feature work with framework work.

Later:

- Move authorable content to JSON.
- Add schema validation for classes, items, monsters, and quests.
- Replace or retire the Python asset extraction/generation tools after the art pipeline settles.

## Feature Parity Work Still To Port Or Validate

- Expand party-member battle targeting, balancing, and UI polish.
- More faithful city building doors and fully populated house interiors.
- More detailed prop clustering and location patches such as farmland, camps, and graveyards.
- Richer dialog/shop controls, including sell/compare flows.
- Skill tree balance pass and clearer node affordances.
- Deeper equipment battle-use rules and item targeting.
- Full Python battle AI parity, party targeting, status readability, and visual effects.
- Music playback and transitions.
- Full asset variant/tint/cutout pipeline.
- Render-mode controls.

## Next Stabilization Slice

1. Run `powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1` after each battle/party/status change.
2. Do one manual run from `Java/` after removing or renaming the sibling `Python/` folder to confirm there are no hidden workspace assumptions.
3. Decide how deep the next party battle slice should go: smarter ally targeting, automatic ally turns, or full ally command selection.
4. Add sell/compare shop flows and any missing battle item targeting rules.
5. Add an asset-pipeline manifest before retiring or rewriting the copied Python scripts.
