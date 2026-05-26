# Echoes of Alderfall

A modular Python fantasy RPG prototype with a large scrollable world map, cities, quest lines, shops, tiered random encounters, class progression, and animated battles using generated sprite assets.

## Run

```powershell
python main.py
```

No third-party packages are required. The game uses `tkinter`, which is included with standard Python installs on Windows.

## Gameplay Config

Monster encounter rates can be tuned in `config/gameplay.json`. Values are chances from `0.0` to `1.0` checked when entering a tile:

- `monster_spawn_chances.wild`: normal wilderness battles
- `monster_spawn_chances.dungeon`: battles inside dungeon maps
- `monster_spawn_chances.dungeon_entrance`: battles on overworld dungeon entrance tiles
- `monster_group_chances.two_monsters`: chance that eligible encounters include two monsters
- `monster_group_chances.three_monsters`: chance that eligible encounters include three monsters

## Controls

- Mouse-first gameplay:
- Click class cards and `Start Adventure` to begin
- Click map tiles or sidebar movement buttons to move
- Press `F11`, `Alt+Enter`, or `Ctrl+F` to toggle fullscreen
- Press `Escape` while exploring to leave fullscreen
- The map camera scrolls as your character travels across the larger world
- Click `Quest Log` in the sidebar to view active quests
- Some named NPCs can join your party after quest turn-ins, while mercenaries can be hired with gold
- Click `Skills` in the sidebar or press `K` to spend level-up skill points
- Click `Save` and `Load` in the sidebar to persist or restore your adventure
- Press `F5` to save and `F9` to load
- From the class screen, click `Load Game` or press `L` if a save exists
- Enter shop NPCs to buy consumables (potions/ether/kit items)
- In battle, each party member and enemy acts on their own turn; click `Attack`, `Defend`, item, or ability buttons for the active party member
- Up to 6 abilities can appear in combat as your class and skill tree grow
- Use consumables in battle with `Potion` and `Ether` buttons
- Battle scenes now change based on terrain (meadow/forest/mountain/water/city/dungeon)
- Abilities trigger themed combat effects (fire/frost/poison/volley/shield/heal/item sparks)
- On battle end, click `Continue` or `Revive`
- Keyboard fallback still works for movement and battle shortcuts (`Space` attack, `D` defend, `1`-`6` abilities, `7` potion, `8` ether)

## Project Structure

- `main.py`: thin launcher
- `src/game/app.py`: Tkinter app, mouse/keyboard input handling, rendering
- `src/game/world.py`: large world map, terrain, expanded NPC dialogue, tiered encounters
- `src/game/entities.py`: player/NPC/quest models and improved XP leveling
- `src/game/battle.py`: battle turns, defend logic, monster ability AI, lunge/flash/fade combat animation, rewards, item use
- `src/game/data.py`: classes, unlockable abilities, quests, item and shop definitions
- `src/game/monsters/`: monster stat catalog and monster ability definitions
- `src/game/assets.py`: image loading with seamless terrain crop + terrain variation tinting
- `tools/generate_assets.py`: legacy deterministic PNG sprite/tile generator
- `tools/extract_sheet_assets.py`: extracts game sprites from generated concept sheets
- `assets/`: categorized sprites, tiles, backdrops, decorations, and source sheets

## Architecture Summary

- **Launcher & App:** `main.py` calls into `src/game/app.py` which runs the Tkinter loop, manages global game state, input handling, UI layout, camera, and orchestrates rendering, world, battle and save systems.
- **Rendering:** `src/game/renderer.py` performs layered rendering with three modes (`fast`, `balanced`, `full`) and caches static layers to improve performance.
- **World generation:** `src/game/world.py` contains a seeded, deterministic procedural generator for the overworld and maps, plus POIs (cities, villages, dungeons) and biome painting.
- **Assets & Sprites:** `src/game/assets.py` loads and caches PNGs from `assets/`, performs resizing, tinting, variants and creates animated poses/backdrops.
- **Gameplay & Data:** `src/game/data.py`, `src/game/monsters/`, `src/game/entities.py`, and `src/game/battle.py` define classes, monsters, items, quests, actors, abilities, and combat mechanics.
- **Persistence:** `src/game/save_system.py` serializes/deserializes game state (player, allies, world position & seed, quests) to JSON under `saves/` with versioning.

## Notable Implementation Details

- Procedural world is seed-based and reproducible; the seed is stored in saves so maps reload identically.
- `AssetStore` supports variant shifts and class tints to reduce asset duplication while enabling visual variety.
- Renderer caches static world layers in `balanced` mode and recomputes dynamic layers each frame (actors, clouds, effects).

## How to Run

Use the project launcher:

```powershell
python main.py
```

No external dependencies are required beyond Python's standard library (uses `tkinter`).

## Quick Next Steps / Recommendations

- Run the game and exercise save/load, map transitions and a few battles to validate runtime behavior.
- Profile `render_mode` choices (`fast`/`balanced`/`full`) to find a good default for your target hardware; heavy image ops in `AssetStore` are common hotspots.
- Add a small automated test for save serialization/deserialization to prevent regressions when changing `Actor` or save structure.

If you want, I can run the game, add tests for save/load, or produce a small architecture diagram. Reply with which you'd like next.

## Planned Improvements

1. Improve rendering performance: profile `AssetStore` hot paths and optimize image ops; consider incremental streaming/caching.
2. Asset pipeline: automate regenerating and extracting assets, add a reproducible build step for spritesheets.
3. Save robustness: add unit tests for save/load serialization, migration handling, and backup/restore flows.
4. UI polish: refine sidebar layout, responsive scaling, and improve keyboard accessibility for menus and battle controls.
5. Battle depth: expand AI tactics, more ability interactions (status effects, combos), and terrain-based modifiers.
6. Content expansion: add more monsters, quests, village/city interiors, and recruitable NPC classes.
7. Tooling: add a simple test harness and CI checks (linting, save round-trip tests) to prevent regressions.
8. Performance modes: expose render-mode presets and an in-game profiler overlay for testing on target machines.
9. Localization & data-driven text: separate strings into resource files to enable translations and easy content tweaks.
10. Packaging: provide a Windows executable build and a minimal installer for easy distribution.
11. Introduce status icons and their duration
12. Visual overhaul - more assets, tiles, road improvements
13. Visual overhaul - rendering improvements to generate objects in logical clusters and locations

## Asset Notes

Source sheets:

```powershell
assets/source/rpg_asset_sheet_concept.png
assets/source/monster_sheet_extra.png
assets/source/monster_sheet_new.png
```

To regenerate extracted in-game assets from those sheets:

```powershell
python tools/extract_sheet_assets.py
```
