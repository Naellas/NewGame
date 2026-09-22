# Echoes of Alderfall - Java Port

This folder is the staged Java migration of the Python prototype.

The current port is intentionally dependency-free Java 21/Swing so it can compile on this machine without Maven, Gradle, or network downloads. PNG assets, gameplay config, and legacy asset pipeline tools are staged inside this Java project, so the game no longer needs a sibling `Python/` folder at runtime.

## Run

```powershell
.\scripts\run.ps1
```

## Build

```powershell
.\scripts\build.ps1
```

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

Build products under `out/` and `out-check/`, local saves, local settings, map-editor exports, compiled `.class` files, and Python `__pycache__` bytecode are ignored. From the repository root, existing tracked generated artifacts can be removed from the index without deleting local copies with:

```powershell
git rm -r --cached out Java/tools/__pycache__
```

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
- `diagnostics`: smoke tests, audit tools, debug metrics, and QA exports.
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
- Shop: click `Buy`, `Hire`, or `Leave Shop`; number keys still buy matching items
- Inventory: click item rows or use number keys
- Main menu: `Enter` new adventure, `L` load adventure, `S` settings
- Pause menu: `Esc` or `P` from gameplay, then use Save / Load, Settings, Main Menu, or Resume
- Save/load: `F5` / `F9`, or use the Save / Load menu
- Fullscreen: click `Fullscreen`, press `F11`, or press `Alt+Enter`
- Close overlay: `Esc`
- Enter settlement/dungeon/interior transition: move onto the entrance, or press `E` while standing on one

For a long-term 2D Java game, the recommended next engine step is libGDX once the gameplay systems are stable. Swing is useful here because it avoids setup friction while the port is young.

## Legacy Asset Tools

The former Python asset scripts are retained under `Java/tools/` for regeneration and extraction work. Run them from the `Java` folder so their relative `assets/...` paths resolve against `Java/assets`.
