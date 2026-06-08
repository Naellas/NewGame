# Echoes of Alderfall Optimization And Development Plan

Generated: 2026-06-01

## Purpose

This report turns the current optimization goal into an actionable backlog:

- Improve runtime performance without reducing graphical fidelity.
- Keep current gameplay features intact.
- Split oversized classes only where it creates clearer ownership or removes repeated work.
- Preserve the current dependency-free Java 21/Swing setup until the gameplay layer is stable enough for a renderer migration.

## Current Baseline

The active game is the Java/Swing port in `Java/`.

Verification on 2026-06-01:

- `powershell -ExecutionPolicy Bypass -File Java\scripts\build.ps1` passed.
- `powershell -ExecutionPolicy Bypass -File Java\scripts\smoke-test.ps1` passed.
- Smoke test output: `Smoke test passed. Mage at 5,6`.

Important workspace note:

- The worktree already contains many uncommitted user changes, especially moved/changed assets and changed core Java files.
- Optimization work should avoid reverting or reorganizing those asset changes unless explicitly requested.

## Current Architecture

Runtime flow:

1. `Main` starts the application.
2. `GameWindow` owns the Swing frame and fullscreen behavior.
3. `GamePanel` owns the fixed-step loop, rendering, input, UI overlays, audio cues, pathing UI, and presentation state.
4. `GameState` owns gameplay state, mode transitions, movement, quests, inventory, party, village, crafting, weather, and battle orchestration.
5. `WorldMap` owns map generation, terrain grids, transitions, props, interiors, village edits, passability, and world lookup helpers.
6. `Battle` owns turn-based combat behavior.
7. `AssetStore` and `AssetCatalog` own image lookup, scaling, fitting, cropping, and cache behavior.
8. `SaveSystem` persists named adventures under `Java/saves`.

Largest Java files by rough source size:

| File | Lines | Risk |
| --- | ---: | --- |
| `GamePanel.java` | ~9,700+ | Very high presentation coupling; best split target. |
| `WorldMap.java` | ~4,000+ | High generation and lookup coupling; best data-index target. |
| `GameState.java` | ~2,900+ | High gameplay orchestration coupling; split carefully after tests. |
| `CraftingSystem.java` | ~1,300+ | Content-heavy; should move toward data definitions later. |
| `Battle.java` | ~1,100+ | Focused but large; optimize after UI split. |
| `GameData.java` | ~1,000+ | Static content registry; JSON/data extraction candidate. |
| `SaveSystem.java` | ~1,000+ | Sensitive persistence surface; refactor late and test heavily. |

## What Is Already Optimized

The existing codebase already includes a previous optimization pass:

- Small `GamePanel` helper records were split into separate package-private files.
- `AssetCatalog` caches successful and missing asset path lookups.
- `AssetStore` caches decoded source images separately from scaled images.
- Animation frame metadata is cached.
- `GamePanel` reuses visible prop lists and uses a static prop draw comparator.

These are useful foundations. The next pass should avoid redoing them and should instead focus on spatial indexing, render organization, and stable subsystem boundaries.

## Performance Findings

### 1. Prop Lookup Is Still Linear

`WorldMap.propAt(...)` and `WorldMap.propsAt(...)` scan `MapArea.props` every time.

Current impact:

- Fine for small maps.
- Cost grows as village/interior/world decoration density grows.
- Tile interactions, placement checks, and render-adjacent logic can repeat this work.

Recommended direction:

- Add a prop index to `MapArea`, keyed by `TilePoint`.
- Keep the list for draw ordering and serialization compatibility.
- Route add/remove/move prop operations through small `MapArea` methods so list and index stay synchronized.

Expected benefit:

- Convert common prop lookup from O(prop count) to O(1) or near O(1).
- Reduce frame-time spikes on dense maps without changing visuals.

### 2. Visible Prop Rebuild Scans Every Prop On The Current Map

`GamePanel.rebuildVisibleWorldProps(...)` still loops over every prop for the active map each frame.

Current impact:

- Works today, but scales poorly with high-fidelity decoration.
- This becomes more expensive as village customization and location-detail passes add more props.

Recommended direction:

- Add a spatial query method such as `WorldMap.propsInBounds(mapId, minX, minY, maxX, maxY)`.
- Back it with the same `MapArea` prop index, or a coarse chunk index if needed.
- Keep draw order identical by sorting only the returned visible set.

Expected benefit:

- Keep graphical fidelity while reducing per-frame iteration.

### 3. GamePanel Is Still Doing Too Much

`GamePanel` currently contains world rendering, battle rendering, inventory UI, village UI, shop UI, world map UI, save/load UI, input routing, audio cue logic, camera logic, pathfinding, and drawing helpers.

Current impact:

- Optimizations are risky because unrelated UI/render concerns are interleaved.
- Small changes can accidentally affect multiple modes.
- Compile remains fast, but cognitive load is high.

Recommended direction:

- Split by stable presentation surfaces, keeping `GamePanel` as coordinator:
  - `WorldRenderer`
  - `BattleRenderer`
  - `InventoryRenderer`
  - `VillageRenderer`
  - `MenuRenderer` or `OverlayRenderer`
  - `InputRouter`
  - `Pathfinder`
  - `AudioCueController`

Expected benefit:

- Faster, safer future optimization.
- Easier graphical regression checks because each surface has clearer ownership.

### 4. Pathfinding Lives Inside The UI Class

`GamePanel` owns path target selection and A* pathfinding.

Current impact:

- Logic is hard to test without UI.
- Future movement features will pull more gameplay logic into presentation code.

Recommended direction:

- Extract a package-private `Pathfinder` service.
- Keep path rendering state in `GamePanel`, but move path computation out.
- Add smoke-test coverage for simple passable, blocked, and nearest-target cases.

Expected benefit:

- No visual change.
- Lower risk when optimizing movement and click navigation.

### 5. AssetCatalog Uses Recursive Search Per New Asset Name

`AssetCatalog` caches results, which is good, but first lookup can still walk asset folders recursively.

Current impact:

- Startup and first-use costs can rise as asset folders grow.
- Missing asset lookups are cached after first miss.

Recommended direction:

- Build a one-time manifest map from asset stem to path at startup, or generate a static asset manifest from tools.
- Keep fallback behavior for development assets.

Expected benefit:

- Faster first-use rendering and fewer filesystem walks.
- Better missing-asset diagnostics.

### 6. Static Content Is Code-Heavy

`GameData`, `SkillTrees`, `CraftingSystem`, and `VillageManager` contain large static definitions.

Current impact:

- Adding content requires recompilation.
- Balancing and data review are harder.
- Large static registries add friction to code optimization work.

Recommended direction:

- Do not move everything to JSON immediately.
- First extract data builders into focused catalog classes.
- Later move stable content groups to JSON with schema validation.

Expected benefit:

- Smaller code files and clearer content ownership.
- Safer gameplay tuning without renderer changes.

## Refactor Boundaries To Preserve

Do preserve:

- Current visual output and asset names.
- `GamePanel` public role as the Swing panel.
- `GameState` as the gameplay coordinator until smoke-test coverage is stronger.
- `WorldMap` public API where saves and UI already depend on it.
- `SaveSystem` property format unless a migration step is written.
- Java 21/Swing dependency-free build scripts.

Avoid during optimization:

- Renderer migration and gameplay refactor in the same task.
- Asset compression or palette changes unless the user explicitly asks.
- Large save-format rewrites before save/load regression tests exist.
- Moving all static content to JSON in one pass.

## Executable Task Backlog

### Phase 0 - Guardrails And Measurements

- [x] Add a small performance note to `SmokeTest` or a separate headless diagnostic that records world-generation time, map count, prop count, and asset-cache counts.
- [x] Add a `DebugMetrics` package-private helper for optional timing counters in render/update hot paths.
- [x] Add a manual visual checklist covering world view, battle, inventory, village, shop, map, save/load, weather, lighting, and animations.
- [x] Keep build verification as `powershell -ExecutionPolicy Bypass -File Java\scripts\build.ps1`.
- [x] Keep behavior verification as `powershell -ExecutionPolicy Bypass -File Java\scripts\smoke-test.ps1`.

Acceptance criteria:

- Build and smoke test pass.
- No visual or gameplay behavior is intentionally changed.
- A future optimization can be compared against baseline counts/timings.

Manual visual checklist for any renderer-affecting change:

- World exploration: terrain, roads, water animation, city/village overlays, props, player/NPC sprites, quest markers, clouds, weather, and lighting.
- Battle: backdrop, party/enemy sprites, action buttons, targeting, status display, floating text, hit flashes, and victory/defeat flow.
- Inventory and party: pack grid, drag/drop, equipment slots, tooltips, actor selector, and item use.
- Village: sidebar tabs, building/prop/tile placement previews, assignment panel, storage summary, and interior decoration.
- Shop and dialog: portrait cards, buy/hire actions, quest dialog options, and close/return behavior.
- Map and menus: world map markers, pause/settings, save/load, fullscreen, zoom, and keyboard shortcuts.

### Phase 1 - MapArea Prop Index

- [x] Add prop-management methods to `MapArea`: `addProp`, `removeProp`, `moveProp`, `propAt`, `propsAt`, and `propsInBounds`.
- [x] Maintain both the ordered `props` list and a `Map<TilePoint, List<WorldProp>>` index.
- [x] Replace direct `area.props.add(...)` and `area.props.remove(...)` call sites with `MapArea` methods.
- [x] Keep `props(String mapId)` returning the ordered list for rendering and compatibility.
- [x] Update `WorldMap.propAt(...)` and `WorldMap.propsAt(...)` to delegate to the index.
- [x] Add smoke-test checks for prop placement, movement, removal, and indexed lookup.

Acceptance criteria:

- All existing features behave the same.
- Dense prop lookup does not scan the full prop list.
- Build and smoke test pass.

### Phase 2 - Visible Prop Query

- [x] Add `WorldMap.propsInBounds(mapId, minX, minY, maxX, maxY)`.
- [x] Replace `GamePanel.rebuildVisibleWorldProps(...)` full-map scan with the bounded query.
- [x] Keep the current visibility margin and draw-order sort.
- [x] Verify dense village/interior scenes retain identical prop layering.

Acceptance criteria:

- Same visual draw order.
- Lower per-frame prop iteration on dense maps.
- Build and smoke test pass.

### Phase 3 - Extract Pathfinder

- [x] Create package-private `Pathfinder`.
- [x] Move `nearestPathTarget`, `findPlayerPath`, `heuristic`, and `walkableForPath` logic out of `GamePanel`.
- [x] Give `Pathfinder` only the world/passability inputs it needs.
- [x] Keep player path state and rendering in `GamePanel`.
- [x] Add smoke-test coverage for reachable, blocked, and nearest-walkable path targets.

Acceptance criteria:

- Click movement remains unchanged.
- Path computation is testable without Swing rendering.
- Build and smoke test pass.

### Phase 4 - Split World Rendering

- [x] Create `WorldRenderer` as a package-private class.
- [ ] Move pure world drawing helpers from `GamePanel` into `WorldRenderer`.
  - Started with the base terrain tile pass; props, actors, weather, lighting, overlays, and input-owned UI remain coordinated by `GamePanel`.
- [ ] Keep shared UI state, buttons, and input in `GamePanel`.
- [ ] Pass a compact render context instead of exposing all `GamePanel` fields.
- [ ] Preserve terrain, water, roads, city/village overlays, lights, weather, props, NPCs, player, quest markers, and clouds.

Acceptance criteria:

- World screen is visually unchanged.
- `GamePanel` becomes smaller without changing mode behavior.
- Build and smoke test pass.
- Manual visual checklist passes for world exploration.

### Phase 5 - Split Major UI Renderers

- [ ] Extract `BattleRenderer`.
- [ ] Extract `InventoryRenderer`.
- [ ] Extract `VillageRenderer`.
- [ ] Extract `ShopRenderer`.
- [ ] Extract `WorldMapRenderer`.
- [ ] Extract shared drawing helpers only after duplication appears.

Acceptance criteria:

- Battle, inventory, village, shop, and world map visuals remain unchanged.
- Input hit-zone registration still works.
- Build and smoke test pass after each extraction, not only at the end.

### Phase 6 - Asset Lookup Manifest

- [x] Add one-time asset index construction to `AssetCatalog`.
- [x] Preserve preferred asset overrides.
- [x] Preserve missing-asset fallback behavior.
- [x] Add a debug summary for duplicate asset stems and missing lookups.
- [x] Consider a generated manifest file later, but keep runtime discovery first.

Acceptance criteria:

- First-use asset lookup avoids repeated recursive filesystem walks.
- Existing assets resolve the same way.
- Missing assets still fall back without crashing.
- Build and smoke test pass.

### Phase 7 - GameState Subsystem Extraction

- [ ] Extract `VillageController` from village edit, worker, assignment, and production logic.
- [ ] Extract `InventoryController` from item use, equip, unequip, and party inventory logic.
- [ ] Extract `QuestController` from quest progress, objectives, interactibles, and reward logic.
- [ ] Extract `WeatherController` from day/night, weather, wind, and biome context.
- [ ] Keep `GameState` as the facade used by `GamePanel` until callers are stable.

Acceptance criteria:

- Existing save/load and UI calls continue through `GameState`.
- More behavior can be smoke-tested without rendering.
- Build and smoke test pass after each controller extraction.

### Phase 8 - Content Catalog Cleanup

- [ ] Split `GameData` into focused catalogs: player classes, enemies, NPCs, items, equipment, shops, quests, recruits.
- [ ] Split `CraftingSystem` data from crafting rules.
- [ ] Split `VillageManager` data from village placement rules.
- [ ] Move only stable, low-risk data to JSON after Java-side catalog classes are clean.
- [ ] Add validation for required fields and duplicate IDs before JSON becomes authoritative.

Acceptance criteria:

- Gameplay data is easier to review.
- Build and smoke test pass.
- No save compatibility break.

### Phase 9 - Optional Renderer Roadmap

- [ ] Keep Swing until the gameplay model is more modular.
- [ ] Add Gradle only when dependency management becomes necessary.
- [ ] Evaluate libGDX after renderer-independent systems are extracted.
- [ ] Treat renderer migration as a separate milestone, not a performance refactor.

Acceptance criteria:

- No engine migration starts until phases 1-7 are stable or explicitly reprioritized.

## Suggested Execution Order

Recommended immediate order:

1. Phase 0: add measurements and visual checklist.
2. Phase 1: add `MapArea` prop index.
3. Phase 2: use bounded prop queries in rendering.
4. Phase 3: extract pathfinding from `GamePanel`.
5. Phase 4: extract world rendering.

This order gives the best blend of performance improvement and refactor safety. It reduces repeated work first, then reduces class size once the hot data path is healthier.

## Definition Of Done For Optimization Work

Every optimization task should meet these conditions:

- `Java\scripts\build.ps1` passes through PowerShell execution-policy bypass.
- `Java\scripts\smoke-test.ps1` passes.
- Current graphical fidelity is preserved.
- Existing controls and UI modes remain available.
- No unrelated asset changes are reverted.
- Any file split keeps package-private visibility where possible.
- Any save-affecting change includes a compatibility note and test.

## Next Codex Task Prompt

Use this prompt to start implementation:

```text
Implement Phase 1 from Java/docs/optimization-game-development-plan.md. Preserve functionality and graphical fidelity. Do not revert existing uncommitted asset changes. Run build and smoke test after the change.
```
