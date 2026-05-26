# Echoes of Alderfall - Java Port

This folder is the staged Java migration of the Python prototype.

The current port is intentionally dependency-free Java 21/Swing so it can compile on this machine without Maven, Gradle, or network downloads. PNG assets are migrated into `assets/` inside this Java project.

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
- Java-side save/load under `Java/saves/save.properties`.

## Migration Direction

This is not a one-file translation of the Python `app.py`. The Java port is split into smaller systems from the start:

- `GameState` owns gameplay state.
- `WorldMap` owns world generation and passability.
- `MapArea`, `WorldTransition`, and `WorldProp` separate map data, movement links, and generated scenery.
- `AssetStore` owns image lookup and caching.
- `GamePanel` owns Swing rendering and input.
- `Battle` owns the initial combat loop.
- `SaveSystem` owns Java save persistence.

## Controls

- Move: click an adjacent map tile, use sidebar movement buttons, or use `WASD`/arrow keys
- Talk/interact: click `Talk / Enter`, click your tile, or press `E`
- Quest log: click `Quest Log` or press `Q`
- World map: click `World Map` or press `M`
- Skills: click `Skills` or press `K`
- Inventory: click `Inventory` or press `I`
- Zoom: click `-`/`+` or use mouse wheel
- Battle: click the action bar, or use `A`/`Space` attack, `1`-`3` abilities, `H` potion, `J` ether
- Dialog: click `Continue`/`Open Shop`, or press `Enter`/`E`
- Shop: click `Buy`, `Hire`, or `Leave Shop`; number keys still buy matching items
- Inventory: click item rows or use number keys
- Save/load: `F5` / `F9`
- Close overlay: `Esc`
- Enter settlement/dungeon/interior transition: move onto the entrance, or press `E` while standing on one

For a long-term 2D Java game, the recommended next engine step is libGDX once the gameplay systems are stable. Swing is useful here because it avoids setup friction while the port is young.
