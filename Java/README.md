# Echoes of Alderfall - Java Port

This folder is the staged Java migration of the Python prototype.

The current port is intentionally dependency-free Java 21/Swing so it can compile on this machine without Maven, Gradle, or network downloads. PNG assets, gameplay config, and legacy asset pipeline tools are staged inside this Java project, so the game no longer needs a sibling `Python/` folder at runtime.

## Run

```powershell
.\scripts\run.ps1
```

Select a retained walking renderer with `scripts/run.ps1 -Movement original`,
`alternate`, `articulated`, or `grounded`. Add `-Profile` for rendering metrics.
The existing per-mode scripts delegate to this shared launcher. `-PrintCommand`
prints the Java command without building or opening the game.

Developer review: [characters and related subsites](tools/reviews/characters/index.html).
Repository checks: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check.ps1`.

## Character movement

Grounded walking is the default for players, companions and world NPCs. It uses
32-frame, distance-driven strides in eight directions, jointed hands/spine/head,
separate cloth, and a short transition back to standing. The existing Gameplay
walking-animation speed control remains at 1x by default. Character scale and
cadence are passed to the rig so foot contact follows world travel.

Previous renderers are preserved: `scripts/run-articulated-movement.ps1`,
`scripts/run-alternate-movement.ps1`, and `scripts/run-original-movement.ps1`.
`scripts/run-grounded-movement.ps1` explicitly selects the new default.
The character review page offers all four walks alongside the original study.

The full roster uses a shared rig. The study's detailed hand/cape masks apply to
its reviewed right-facing source poses; other outfits/directions use the general
part extraction and can still benefit from individual art cleanup. This system
covers humanoid world characters; monster and combat animations keep their own
renderers.

Validation: `GroundedWalkingTest`, `GroundedGameRenderTest`,
`WalkAnimationSpeedTest`, `WorldPoseTest`, and `CharacterAnimationAudit grounded`.

## Build

```powershell
.\scripts\build.ps1
```

For an isolated verification build, use `scripts/build.ps1 -OutputDirectory
temp/<task>/classes`. Output is restricted to the normal `out` or a task path
beneath `temp`; redirected output is never recursively cleared. Recognized
OneDrive cloud placeholders are supported. Junctions, symbolic links, and unknown
reparse points in the output tree or its ancestors stop the build before cleanup.

## Smoke Test

```powershell
.\scripts\smoke-test.ps1
```

## Rendering Performance

Run a repeatable software rendering benchmark and rendering cache checks from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Java\scripts\benchmark-render.ps1
```

The benchmark warms up each scene for 120 frames, then measures 240 frames at High render quality and 100% zoom in the starting village and overworld. Use `-Samples 480` for a longer run. It reports mean, median, and p95 frame times. These measurements exclude simulation updates and GPU/display presentation, so they are not an in-game FPS measurement. Other local settings remain in effect.

The renderer caches the viewport vignette until its size or day/night colors change, uses opaque terrain images where all pixels are opaque, and uses an opaque software back buffer. Transparent terrain retains its alpha channel. Rendering and gameplay updates stay on the Swing event thread.

For live profiling, `scripts/run-profile.ps1` enables average and maximum timings, including simulation updates, terrain, scenery, props, and lighting. Maximum timings help expose loading or cache rebuild stalls that averages can hide.

## Generated Files

Build products under `out/` and `out-*/`, local saves, local settings, map-editor exports, `temp/` scratch files, compiled `.class` files, Python `__pycache__` bytecode, and JVM profiling/crash output are ignored.

Build output and caches can be deleted when the game and development tools are closed; build scripts recreate them. Deleting `saves/` removes local play progress. Review exports and scratch files before clearing them. Keep source, assets, tools, and configuration defaults.

## Current Scope

- Java window and fixed-step Swing game loop.
- Class selection for Knight, Mage, and Ranger.
- Deterministic 300x300 overworld inspired by the Python map generator.
- Multi-map world structure with generated city, village, dungeon, and house-interior maps.
- Overworld settlement/dungeon transitions, plus city/village exits and Stonegate deep-vault stairs.
- Deterministic terrain prop placement for biomes, roads, cities, villages, dungeons, and interiors.
- Terrain variants, blended overworld edges, connected road overlays, settlement overlays, and drifting clouds.
- Mouse-first sidebar controls, clickable movement/action buttons, zoom controls, and world map overlay.
- Migrated PNG assets loaded from `Java/assets` through a Java `AssetStore`.
- Keyboard exploration with camera-follow rendering.
- Encounter and battle loop with attacks, abilities, status effects, XP, gold, level-ups, defeat, and revive.
- Overworld NPCs adapted from the Python city/village NPC list.
- Dialog, quest acceptance, quest progress, quest rewards, and quest log.
- Recruitable quest allies and hireable village companions with party battle turns.
- Consumable inventory, equipment slots, quick item use, shops, and shop purchases.
- Migrated common/class skill trees with skill points, stat bonuses, ability unlocks, and respec.
- Java-side named adventure save/load slots under `Java/saves/`.
- Gameplay tuning loaded from `Java/config/gameplay.json`.

## Migration Direction

This is not a one-file translation of the Python `app.py`. The Java port is split into smaller systems from the start:

- `GameState` owns gameplay state.
- `WorldMap` owns world generation and passability.
- `MapArea`, `WorldTransition`, and `WorldProp` separate map data, movement links, and generated scenery.
- `AssetStore` owns image lookup and caching.
- `GamePanel` owns Swing rendering and input.
- `Battle` owns the initial combat loop.
- `SaveSystem` owns Java save persistence.

## Source Organization

The remaining `com.alderfall.game` classes are grouped into responsibility folders while keeping the root package declaration intact for now. This preserves package-private collaboration during the migration while making the source tree easier to scan:

- `app`: window, panel, input, game mode, and launch configuration.
- `audio`: music, sound, and game audio coordination.
- `combat`: battle flow, abilities, combat poses, and status effects.
- `content`: catalogs, dialogue, quests, shops, professions, and skill tree definitions.
- `entities`: actors, NPC behavior, directional sprites, and character presentation data.
- `systems`: save/load, weather, crafting, equipment hooks, and village management.
- `worldmodel`: terrain, pathfinding, landmarks, settlements, prompts, and world props.
- `rendering`: root-package render helpers, asset loading, metrics, weather cache, and world rendering.
- Test, audit, and review programs now live in separate source roots; see [src/README.md](src/README.md). Runtime `DebugMetrics` remains with rendering helpers.
- `uiwidgets`: root-package UI helper widgets.
- `state`: shared game state.
- `minigames`: standalone minigame flows.

## Controls

- Move: click a visible map tile to path toward it, or use `WASD`/arrow keys
- Talk/interact: click `Talk / Enter`, click your tile, or press `E`
- Quest log: click `Quest Log` or press `Q`
- World map: click `World Map` or press `M`
- Village: click `Village` or press `V` while in Oathstead Camp or one of its managed interiors
- Skills: click `Skills` or press `K`
- Inventory: click `Inventory` or press `I`
- Zoom: click `-`/`+` or use mouse wheel
- Battle: click the action bar, or use `A`/`Space` attack, `R` run, `1`-`0` abilities, `H` use item
- Dialog: click numbered conversation options, press `1`-`9`, or press `Enter`/`E` to advance
- Shop: drag item tiles between stock and your pack to buy/sell one, or click a tile; `1`-`9` buy visible stock. Each grid scrolls independently under the pointer.
- Inventory: click tiles or use `1`-`9` for visible items; drag items to equipment slots or the character, and drag equipped gear back to the pack.
- Inventory and shops: use icon category tabs (All, Materials, Weapons, Armor, Consumables, Accessories, Misc). Click search to type an item name; matching tiles glow and search switches categories when needed. Clear with `x`; `Enter` or `Esc` leaves the search field.
- Main menu: `Enter` new adventure, `L` load adventure, `S` settings
- Pause menu: `Esc` or `P` from gameplay, then use Save / Load, Settings, Main Menu, or Resume
- Save/load: `F5` / `F9`, or use the Save / Load menu
- Fullscreen: click `Fullscreen`, press `F11`, or press `Alt+Enter`
- Close overlay: `Esc`
- Enter settlement/dungeon/interior transition: move onto the entrance, or press `E` while standing on one

For a long-term 2D Java game, the recommended next engine step is libGDX once the gameplay systems are stable. Swing is useful here because it avoids setup friction while the port is young.

## Asset Tools

Python importers and generators are grouped by subject under `tools/assets/`; audio, checks, and tests have their own folders. Use `python tools/run.py --list` from Java (or `python Java/tools/run.py --list` from the repository root). The runner selects the Java working directory. See [tools/INDEX.md](tools/INDEX.md) for canonical paths and archived-tool status.

## Lootable chests

Stand beside a chest and press `E` (or click a nearby chest) to open the chest and shared pack together. Use **Take 1**, **Store 1**, or **Stack** to transfer items, and **Take all** to collect everything. Both lists have page controls. Close with `Esc`. Equipped gear must be unequipped before storing it.

Generated treasure rooms contain chests with randomized materials, weapons, and potions. Interior ironbound and screen chests also work as containers. Chest contents persist in saves, including empty chests and items you deposit. Older saves receive loot when each chest is first opened.

### Slow sideways cadence and cloth preview

Grounded walking now limits sideways cadence to at least 0.9x when a slower
preference would overextend the planted legs. The saved slider value remains a
preference; travel speed and the 1x default do not change. The same calculation
is used by the distance clock and rendered rig, including diagonal fallbacks.
Legacy movement modes retain their original timing.

The character review page has a **Cloth & hair physics** tab with a switchable
spring/capsule experiment. This secondary-motion simulation is preview-only.
