# Parallel Upgrade Prompts

Use this file to run several upgrade tasks in parallel with separate workers. Each worker should start from the current workspace, read the named files first, preserve uncommitted user changes, and keep edits inside its assigned ownership boundary.

Recommended baseline commands for workers that change Java code:

```powershell
powershell -ExecutionPolicy Bypass -File Java\scripts\build.ps1
powershell -ExecutionPolicy Bypass -File Java\scripts\smoke-test.ps1
```

If a worker only changes docs, it does not need to run the Java build unless it edits code-adjacent scripts or generated QA reports.

## Coordination Rules

- Do not revert unrelated changes.
- Avoid broad formatting churn.
- Prefer small, reviewable commits or patches.
- Keep `GamePanel` extraction workers on different render/UI surfaces.
- Run build and smoke test after Java changes.
- Update docs when behavior, commands, or file ownership changes.
- If two workers need the same file, the worker with the narrower task should go first.

## Worker 1 - Renderer Extraction Spike

```text
Upgrade the Java/Swing project by extracting a first small world-rendering slice from GamePanel into a package-private WorldRenderer helper.

Context:
- Project root: Java/
- Key files:
  - Java/src/main/java/com/alderfall/game/GamePanel.java
  - Java/src/main/java/com/alderfall/game/WorldMap.java
  - Java/src/main/java/com/alderfall/game/AssetStore.java
  - Java/docs/optimization-game-development-plan.md

Goal:
- Start Phase 4 from Java/docs/optimization-game-development-plan.md with the smallest safe extraction.
- Keep GamePanel as the coordinator.
- Move only pure world drawing helpers that do not require input state mutation.
- Preserve existing visuals and behavior.

Constraints:
- Do not attempt to move all world rendering at once.
- Do not change save format, assets, or gameplay rules.
- Do not refactor battle, inventory, village, or menu rendering in this task.
- Preserve all uncommitted user changes.

Suggested approach:
1. Read the world rendering section of GamePanel starting at drawWorld.
2. Identify a cohesive helper cluster with minimal field dependencies.
3. Create Java/src/main/java/com/alderfall/game/WorldRenderer.java.
4. Move that helper cluster and pass a compact context or explicit parameters.
5. Keep method names and draw order recognizable.
6. Run build and smoke test.

Acceptance:
- Java build passes.
- Smoke test passes.
- GamePanel is smaller.
- World drawing behavior is intended to be visually unchanged.
```

## Worker 2 - Battle Renderer Extraction Spike

```text
Upgrade the Java/Swing project by extracting a first small battle-rendering slice from GamePanel into a package-private BattleRenderer helper.

Context:
- Key files:
  - Java/src/main/java/com/alderfall/game/GamePanel.java
  - Java/src/main/java/com/alderfall/game/Battle.java
  - Java/src/main/java/com/alderfall/game/BattleActionAnimation.java
  - Java/src/main/java/com/alderfall/game/BattleActorPose.java

Goal:
- Reduce GamePanel presentation coupling by moving a cohesive, pure battle drawing helper group into BattleRenderer.
- Keep battle input, button registration, and state mutation in GamePanel for now.

Constraints:
- Do not alter battle rules, target selection, cooldowns, or status logic.
- Do not move UI hit-zone registration unless the extracted API is explicitly designed for it.
- Do not touch world rendering or inventory rendering.
- Preserve uncommitted user changes.

Suggested approach:
1. Read GamePanel battle drawing methods around drawBattle and battle effect helpers.
2. Pick a narrow helper cluster, such as battle backdrop/effect drawing or actor pose rendering.
3. Create Java/src/main/java/com/alderfall/game/BattleRenderer.java.
4. Pass AssetStore, animation time, and dimensions explicitly.
5. Keep GamePanel as the public coordinator.
6. Run build and smoke test.

Acceptance:
- Java build passes.
- Smoke test passes.
- No battle behavior changes.
- Extracted renderer has package-private visibility.
```

## Worker 3 - Companion Dialogue QA Upgrade

```text
Upgrade the companion dialogue QA tooling so dialogue regressions are easier to detect.

Context:
- Key files:
  - Java/src/main/java/com/alderfall/game/CompanionDialogueQaExport.java
  - Java/scripts/dialogue-qa.ps1
  - Story premise/companion-dialogue-qa.md
  - Java/src/main/java/com/alderfall/game/DialogueLibrary.java
  - Java/src/main/java/com/alderfall/game/GameData.java

Goal:
- Add useful QA checks to the generated companion dialogue report.
- Keep the report readable for narrative review.

Useful checks:
- Repeated option labels within the same node.
- Empty or duplicate NPC lines in sampled paths.
- Missing expected trust snapshots.
- Quest-stage samples that fail to produce choices.
- Branches where the player path cannot be followed.

Constraints:
- Do not rewrite companion dialogue content unless fixing an obvious broken label or empty line.
- Do not change gameplay dialogue behavior outside QA/reporting unless required by a clear bug.
- Preserve current generated report format as much as possible.

Suggested approach:
1. Read CompanionDialogueQaExport and the generated report.
2. Add a "QA Findings" section per companion and a summary count near the top.
3. Make dialogue-qa.ps1 rebuild and regenerate the report.
4. Run dialogue-qa.ps1.
5. Run build and smoke test if Java code changes.

Acceptance:
- QA report regenerates successfully.
- Findings are deterministic.
- Build passes if Java code changed.
```

## Worker 4 - Static Content Catalog Split

```text
Upgrade maintainability by splitting one low-risk static content group out of GameData into a focused catalog class.

Context:
- Key files:
  - Java/src/main/java/com/alderfall/game/GameData.java
  - Java/src/main/java/com/alderfall/game/Item.java
  - Java/src/main/java/com/alderfall/game/Equipment.java
  - Java/src/main/java/com/alderfall/game/Shop.java

Goal:
- Reduce GameData size without changing runtime behavior.
- Choose one narrow content group first, preferably items, equipment, shops, or monster lore.

Constraints:
- Do not move all GameData content.
- Do not convert to JSON yet.
- Preserve public GameData APIs used by the rest of the code.
- Avoid touching DialogueLibrary in this task.

Suggested approach:
1. Identify one static map/list initializer that is self-contained.
2. Create a package-private catalog class, for example ItemCatalog or ShopCatalog.
3. Have GameData delegate to the new catalog while preserving existing constants/methods.
4. Add duplicate-key validation if easy and local.
5. Run build and smoke test.

Acceptance:
- GameData is smaller.
- Public behavior and names remain compatible.
- Java build and smoke test pass.
```

## Worker 5 - GameState Controller Extraction Spike

```text
Upgrade GameState maintainability by extracting one narrow controller from gameplay orchestration.

Context:
- Key files:
  - Java/src/main/java/com/alderfall/game/GameState.java
  - Java/src/main/java/com/alderfall/game/Quest.java
  - Java/src/main/java/com/alderfall/game/CraftingSystem.java
  - Java/src/main/java/com/alderfall/game/VillageManager.java

Goal:
- Extract one cohesive controller while keeping GameState as the facade used by GamePanel.
- Recommended first target: QuestController, InventoryController, or WeatherController.

Constraints:
- Do not change save/load format.
- Do not require GamePanel call-site rewrites beyond tiny facade-preserving adjustments.
- Do not combine this with renderer extraction.
- Preserve uncommitted user changes.

Suggested approach:
1. Search GameState for a cohesive method group.
2. Create a package-private controller class with explicit dependencies.
3. Move only methods whose ownership is clear.
4. Keep GameState public methods delegating to the controller.
5. Add or extend smoke-test coverage for the moved behavior.
6. Run build and smoke test.

Acceptance:
- GameState is smaller.
- Existing GamePanel calls still compile.
- Save/load behavior is unchanged.
- Java build and smoke test pass.
```

## Worker 6 - Repository Hygiene

```text
Upgrade repository hygiene by documenting and fixing generated-artifact tracking.

Context:
- Key files:
  - Java/.gitignore
  - README.md
  - Java/README.md
  - tracked root out/ class files
  - tracked Java/tools/__pycache__ pyc files

Goal:
- Add ignore coverage for root build output and Python cache files.
- Prepare a safe cleanup plan for tracked generated artifacts.

Constraints:
- Do not delete tracked files unless explicitly requested by the user.
- Do not remove source assets under Java/assets/source.
- Do not change build output paths unless the build scripts require it.

Suggested approach:
1. Add a root .gitignore covering /out/, __pycache__/, *.pyc, Java/saves/, Java/config/settings.properties, and local exports.
2. Keep Java/.gitignore or align it with the root ignore file.
3. Document the exact follow-up command needed to stop tracking generated artifacts, but do not run destructive cleanup.
4. Run git status and report the changed files.

Acceptance:
- Ignore rules cover generated build/cache outputs.
- No source assets are removed.
- The user has a clear cleanup command for a later intentional step.
```

## Worker 7 - Focused Smoke Test Expansion

```text
Upgrade regression coverage by adding focused headless tests to SmokeTest.

Context:
- Key files:
  - Java/src/main/java/com/alderfall/game/SmokeTest.java
  - Java/src/main/java/com/alderfall/game/GameState.java
  - Java/src/main/java/com/alderfall/game/Battle.java
  - Java/src/main/java/com/alderfall/game/SaveSystem.java

Goal:
- Add high-signal tests for one risky gameplay surface.
- Recommended first targets:
  - shop buy/sell behavior
  - battle ally target/item behavior
  - save/load compatibility for new companion/dialogue state
  - quest objective stage progression

Constraints:
- Do not turn SmokeTest into a full test framework.
- Keep tests deterministic and headless.
- Avoid changing gameplay behavior unless a test exposes a clear bug.

Suggested approach:
1. Pick one target area.
2. Add small helper methods inside SmokeTest.
3. Test behavior through public or package-level APIs already used by the game.
4. Keep failure messages specific.
5. Run build and smoke test.

Acceptance:
- SmokeTest covers one additional risky workflow.
- Test failure messages identify the broken behavior.
- Java build and smoke test pass.
```

## Worker 8 - Upgrade Roadmap Refresh

```text
Refresh project docs so the upgrade roadmap matches the current codebase.

Context:
- Key files:
  - Java/docs/optimization-game-development-plan.md
  - Java/docs/migration-plan.md
  - Java/docs/codebase-summary.md
  - Java/README.md

Goal:
- Bring docs up to date with the current source shape.
- Note that GamePanel is now over 18k lines and should be the primary extraction target.
- Document completed optimization phases and current next steps.

Constraints:
- Docs only.
- Do not edit Java source.
- Do not claim a task is complete unless verified in code.

Suggested approach:
1. Recount current large source files.
2. Update stale size estimates.
3. Update the next recommended tasks.
4. Add a short section pointing to this parallel prompt file.

Acceptance:
- Docs reflect current project state.
- Future workers can choose the next task without rediscovering basics.
```

## Suggested Parallel Schedule

Run these together first because they touch mostly separate surfaces:

1. Worker 3 - Companion Dialogue QA Upgrade
2. Worker 6 - Repository Hygiene
3. Worker 7 - Focused Smoke Test Expansion
4. Worker 8 - Upgrade Roadmap Refresh

Run only one of these at a time unless workers coordinate patches carefully:

1. Worker 1 - Renderer Extraction Spike
2. Worker 2 - Battle Renderer Extraction Spike
3. Worker 5 - GameState Controller Extraction Spike

Run after the docs and tests are stable:

1. Worker 4 - Static Content Catalog Split

