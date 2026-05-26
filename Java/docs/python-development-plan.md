# Archived Python Development Plan

This is the former Python prototype plan, copied here so the top-level `Python/` folder can be removed without losing migration context. Treat it as historical reference; current Java work is tracked in `docs/migration-plan.md`.

# Echoes of Alderfall Development Plan

This plan starts from the current project shape after removing obvious generated clutter:

- Removed temporary city extraction folders: `assets/city/split_try/`, `assets/city/debug_components/`.
- Removed local Python cache folders outside `.venv/`.
- Moved `prop-contact-sheet.png` to `assets/source/prop-contact-sheet.png`.
- Updated `.gitignore` for local saves, virtualenvs, caches, and regenerated debug outputs.

## Current Structure

Core runtime:

- `main.py` - thin launcher.
- `src/game/app.py` - main Tkinter application, screens, input, overlays, battle UI, and many rendering helpers.
- `src/game/renderer.py` - world renderer and cached render modes.
- `src/game/assets.py` - asset lookup, image fitting, variant tinting, backdrop handling, and fallbacks.
- `src/game/world.py` - procedural world generation, map transitions, POIs, city/village/dungeon layouts.
- `src/game/battle.py` - battle loop, actor turns, abilities, item use, rewards, combat effects.
- `src/game/entities.py` - actor, NPC, quest, inventory, leveling, and equipment state.
- `src/game/data.py` - classes, quests, items, shops, story beats, recruit data.
- `src/game/equipment.py` - equipment definitions and derived item behavior.
- `src/game/skill_tree.py` - skill nodes, ranks, unlocks, and respec cost.
- `src/game/effects.py` - status effects, icon labels, and ability hinting.
- `src/game/decorations.py` - biome decoration catalogs and sizing rules.
- `src/game/save_system.py` - save/load serialization and save summaries.
- `src/game/config.py` and `config/gameplay.json` - runtime defaults and tunable gameplay/graphics config.
- `src/game/monsters/catalog.py` and `src/game/monsters/abilities.py` - monster stats and enemy ability definitions.

Assets and tooling:

- `assets/source/` - source sheets, previews, and contact/reference images.
- `assets/terrain/`, `assets/road/`, `assets/deco/`, `assets/city/`, `assets/battle/`, `assets/player/`, `assets/npcs/`, `assets/monsters/` - runtime image categories.
- `assets/city/split/` - generated city tilesheet split output. Keep for now until city rendering is fully audited.
- `assets/terrain/before_fidelity_refresh/` - backup terrain set. Keep for now as rollback material, but archive or remove once the refreshed terrain is accepted.
- `tools/*.py` - asset generation and extraction scripts.
- `saves/*.json` - local runtime saves, ignored by git.

## Near-Term Priorities

1. Establish a safety net before deeper refactors.

   Files to add or update:

   - `tests/test_save_system.py`
   - `tests/test_config.py`
   - `tests/test_world_generation.py`
   - `src/game/save_system.py`
   - `src/game/config.py`
   - `src/game/world.py`
   - `README.md`

   Points to expand:

   - Add save/load round-trip tests for player state, inventory, equipment, skill allocations, quest state, current map, and world seed.
   - Add config tests for bad JSON, missing keys, chance clamping, and graphics mode validation.
   - Add deterministic world generation checks for the default seed and a nonzero seed.
   - Add a simple `python -m compileall src tools main.py` verification step to the README.

2. Reduce the size and responsibility of `src/game/app.py`.

   Files to create or change:

   - `src/game/app.py`
   - `src/game/ui/__init__.py`
   - `src/game/ui/widgets.py`
   - `src/game/ui/screens.py`
   - `src/game/ui/overlays.py`
   - `src/game/input.py`
   - `src/game/state.py`

   Points to expand:

   - Move button, hover zone, tooltip, text wrapping, and shared UI drawing helpers into `src/game/ui/widgets.py`.
   - Move class select, save select, quest log, shop, inventory, skill tree, and dialog overlays into `src/game/ui/screens.py` or `src/game/ui/overlays.py`.
   - Keep `GameApp` as the orchestrator while shrinking direct rendering/UI methods gradually.
   - Preserve existing behavior first; avoid visual redesign during this pass.

3. Make the asset pipeline reproducible.

   Files to add or update:

   - `src/game/assets.py`
   - `tools/extract_sheet_assets.py`
   - `tools/extract_city_tilesheet.py`
   - `tools/extract_city_key_assets.py`
   - `tools/generate_overworld_fidelity_assets.py`
   - `tools/generate_biome_decorations.py`
   - `assets/source/`
   - `docs/asset-pipeline.md`

   Points to expand:

   - Document which tools produce each runtime asset folder.
   - Add a manifest such as `assets/source/asset-manifest.json` that maps source sheets to generated outputs.
   - Decide whether `assets/city/split/` is runtime content or only extraction output.
   - Archive or remove `assets/terrain/before_fidelity_refresh/` after visual approval.
   - Keep preview/contact sheets under `assets/source/` only.

4. Improve rendering performance without changing gameplay.

   Files to change:

   - `src/game/renderer.py`
   - `src/game/assets.py`
   - `src/game/app.py`
   - `src/game/config.py`
   - `config/gameplay.json`

   Points to expand:

   - Profile `AssetStore.get`, `get_fit`, `get_backdrop_crop`, and city cutout processing.
   - Cache expensive fitted city/building images by stable size and variant.
   - Add a lightweight debug overlay for frame time, render mode, cache size, and visible tile count.
   - Keep `fast`, `balanced`, and `full` render modes, but define expected tradeoffs in docs.

5. Deepen combat and status readability.

   Files to change:

   - `src/game/battle.py`
   - `src/game/effects.py`
   - `src/game/app.py`
   - `src/game/data.py`
   - `src/game/monsters/abilities.py`
   - `src/game/monsters/catalog.py`

   Points to expand:

   - Expand status icons with remaining duration and stack count.
   - Add terrain-based modifiers only after battle tests exist.
   - Add monster AI priorities for healing, finishing attacks, buffs, and multi-target abilities.
   - Move battle UI drawing out of `app.py` once behavior is covered by tests.

6. Make content easier to extend.

   Files to change or split:

   - `src/game/data.py`
   - `src/game/world.py`
   - `src/game/decorations.py`
   - `src/game/equipment.py`
   - `src/game/monsters/catalog.py`
   - `content/classes.json`
   - `content/items.json`
   - `content/quests.json`
   - `content/shops.json`
   - `content/monsters.json`

   Points to expand:

   - Move static content out of Python only after tests can verify loading and defaults.
   - Keep generated/procedural behavior in Python; move authorable content into JSON.
   - Add schema-style validation helpers with clear fallback errors.
   - Add a short content authoring guide in `docs/content-authoring.md`.

7. Package and release the prototype cleanly.

   Files to add or update:

   - `README.md`
   - `.gitignore`
   - `pyproject.toml`
   - `requirements.txt` or documented standard-library-only policy
   - `tools/build_windows.ps1`
   - `docs/release-checklist.md`

   Points to expand:

   - Decide whether Pillow is a development-only dependency for tools or a runtime dependency.
   - Add a repeatable Windows build path after the file structure stabilizes.
   - Keep local saves out of release bundles unless exporting sample saves intentionally.
   - Add a pre-release checklist: compile, smoke run, save/load, battle, map transitions, asset regeneration notes.

## Proposed File Structure Direction

Target structure after staged refactors:

```text
.
├── assets/
│   ├── source/
│   ├── terrain/
│   ├── road/
│   ├── deco/
│   ├── city/
│   ├── battle/
│   ├── player/
│   ├── npcs/
│   └── monsters/
├── config/
├── content/
├── docs/
├── saves/
├── src/
│   └── game/
│       ├── ui/
│       ├── monsters/
│       ├── app.py
│       ├── renderer.py
│       ├── assets.py
│       ├── world.py
│       ├── battle.py
│       ├── entities.py
│       ├── save_system.py
│       └── ...
├── tests/
├── tools/
├── main.py
└── README.md
```

## Cleanup Watchlist

Review these before committing or packaging:

- `assets/city/split/` - generated split tiles; verify whether runtime needs them.
- `assets/terrain/before_fidelity_refresh/` - rollback backup; archive after acceptance.
- `assets/source/preview-*.png` - useful locally, ignored by git by default.
- `assets/source/prop-contact-sheet.png` - reference/contact sheet, keep only if useful for asset review.
- `tools/generate_assets.py` - marked legacy in README; keep until the new asset manifest replaces it.
- `saves/*.json` - local state only, ignored by git.

## Recommended First Implementation Slice

1. Add tests for `save_system.py`, `config.py`, and deterministic world seed behavior.
2. Extract shared UI helpers from `src/game/app.py` into `src/game/ui/widgets.py`.
3. Document the asset pipeline in `docs/asset-pipeline.md`.
4. Audit `assets/city/split/` and `assets/terrain/before_fidelity_refresh/`.
5. Add a basic performance overlay controlled by `config/gameplay.json`.
