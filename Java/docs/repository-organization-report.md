# Repository organization and consolidation proposal

Audit date: 2026-09-23. Status: proposal; no existing assets, scripts, or source files moved or deleted.

## Recommendation

Keep `Java/` as the active project and preserve existing runtime asset names initially. Separate production assets, original artwork, review evidence, development tools, and disposable output. Consolidate shared tool infrastructure before moving large asset trees. A wholesale rename would currently risk changing asset lookup and breaking relative paths.

The main problem is unclear ownership and lifecycle, rather than simply too many files. The repository already has useful building blocks: an asset path helper, a preferred regeneration entrypoint, ignore rules, and a checksum-backed cleanup archive. Extend these instead of creating another parallel system.

## Current inventory

Counts include files physically present, including untracked additions and ignored files where applicable. Sizes are logical file sizes, not OneDrive allocation or Git history size. This is a working-tree snapshot.

| Area | Files | MiB | Finding |
| --- | ---: | ---: | --- |
| `Java/assets/` | 4,781 | 1,981.1 | Runtime images, source sheets, prompts, and previews coexist |
| `Java/assets/source/` (included above) | 454 | 574.3 | Explicit source archive, but not the only source location |
| Root `assets/` | 16 | 26.6 | Mage movement source images outside the documented Java asset home |
| `asset-review/` | 7,940 | 1,334.6 | Cleanup quarantine, source masters, previews, and experiments share one namespace |
| `Java/tools/` | 95 | 0.6 | Top-level files include 73 Python, 13 Java, and 5 PowerShell tools |
| `Java/docs/` | 47 | 0.4 | Plans, reference docs, and HTML review pages mixed together |
| `Java/src/` | 295 | 5.5 | Includes 94 Java diagnostic classes under production source |
| `Java/out*` directories | 16,176 | 189.3 | 34 separate build-output directories |
| Root `temp/` and `Java/temp/` | 2,786 | 71.0 | Scratch work; inspect before treating everything as disposable |
| `Java/archive/` | 5 | <0.1 | README and placeholders; not an established populated archive |

At audit time, `Java/assets` had 989 untracked files and 71 modified files; `Java/src` had 67 untracked and 37 modified files. Preserve and record this work before any reorganization. The two newest commits are dated September 22–23; findings cover the current working tree, not only committed additions.

Large review areas include `dialogue-motion` (450.5 MiB), `monster-attacks-2026-09-23` (188.7 MiB), and `characters` (2,600 files, 105.7 MiB). Their size makes them retention-policy candidates, not evidence that they are unnecessary.

## Structural problems and evidence

1. **Source artwork has several homes.** Examples: root `assets/source`, `Java/assets/source`, `Java/assets/companions/*/sources`, and `Java/assets/animations/character-refresh/pass-v4/sources`. `ImportCharacterPass.java` and `ReviewCharacterPass.java` hard-code the last location.
2. **Runtime discovery can include development images.** `AssetCatalog.java` recursively indexes PNGs in named runtime folders, including `animations` and `companions`; it does not exclude nested `sources` folders. Top-level `Java/assets/source` is not in that list. Moving all sources into one location outside runtime assets would make this distinction explicit.
3. **Asset identity depends on filenames and traversal.** `AssetCatalog` uses the PNG stem and `putIfAbsent`; duplicate names within recursive discovery do not have an explicit sorted precedence. There are 93 repeated PNG basenames across all of `Java/assets`, including source material; this is not a count of actual runtime collisions. Python `find_asset` returns the first sorted recursive match, so Python and Java can select differently.
4. **Review and quarantine have different responsibilities.** `asset-review/README.md`, `manifest.json`, `files/`, and `restore.ps1` form an existing restoration workflow for 437 candidates. New visual review batches sit alongside them. Preserve that restoration bundle intact until its paths and checksums are explicitly migrated.
5. **Tool placement does not explain execution.** Some Java tools are standalone importers; others, such as `DestinationApproachReview`, belong to `com.alderfall.game` and depend on compiled game classes. Moving them into one directory does not make their launch commands interchangeable.
6. **Source folders and Java packages are not equivalent.** Many responsibility folders intentionally retain the root package for package-private access. `render` and `rendering`, or `ui` and `uiwidgets`, cannot safely be merged by renaming folders alone.
7. **Current build includes development diagnostics.** `scripts/build.ps1` recursively compiles all Java under `src/main/java`, including tests and review exporters. There is no separate test/review source build in that script.
8. **Path conventions are fragile.** Many tools expect the working directory to be `Java`; `regenerate_universal_assets.py` derives its root from `__file__.parents[1]`; `process_walk15_animations.py` also knows a machine-local generated-image directory, with a local-source fallback. Reparenting tools needs path and import updates.

## Redundant files: verified findings

The companion [duplicate inventory](repository-duplicate-inventory.csv) records every nonempty exact duplicate group found using SHA-256, across tracked and non-ignored untracked files. Git internals, ignored build output, and ignored scratch data were excluded. File-size grouping preceded hashing. Empty placeholders were excluded. No image similarity or perceptual comparison was performed.

**839 groups contain 2,054 extra copies, totaling approximately 134.7 MiB of repeated bytes.** Of these, 825 groups are PNGs (1,204 extra copies, 134.63 MiB); 9 groups are `.frames` sidecars (844 extra copies). No byte-identical `.java`, `.py`, `.ps1`, `.md`, or `.html` files were found. These numbers are an upper bound on repeated storage, not an approved deletion list.

| Candidate | Evidence | Proposed treatment |
| --- | --- | --- |
| `Java/assets/city/walls/city_wall_sun_gate_horizontal.png` and `_horizontal_clean.png` | Exact same bytes; 1.70 MiB per image | Choose canonical ID only after checking dynamic wall-name construction and external references; alias or update consumers before removing one |
| Equivalent `city_wall_sun_gate_vertical.png` and `_vertical_clean.png` | Exact same bytes; 0.74 MiB per image | Same treatment; together the two pairs represent about 2.44 MiB of extra runtime copies |
| `Java/assets/companions/aria/sources/npc_aria_battle_source.png` and `Java/assets/companions/sources/npc_aria_battle_source.png` | Exact same source image; about 1.51 MiB | One canonical source path; update importers and provenance records |
| `Java/assets/source/npc-refresh-2026-09-22/{orin,citizen_woman,blacksmith,merchant,bartender,citizen_man}.png` and matching `asset-review/2026-09-22-npcs/masters/` files | Six exact source/master pairs | Let review pages reference canonical originals, unless the review intentionally preserves a historical snapshot |
| Runtime images duplicated in `source/*integrity-2026-09-22/` | Examples: `goblin_king.png`, `mountain_drake.png`, `city_building_town_manor.png` | Record whether these are rollback snapshots before consolidation; original and accepted output are separate lifecycle roles even when currently identical |
| Directional animations with identical content | Examples: knight up/up-left/up-right walk strips; mage up-left/up-right strips | Candidate for explicit aliases, not direct deletion: distinct names may be required by direction-based lookup |
| Repeated `.frames` contents | Small files often contain only the same integer | Keep sidecars beside their strips; same content does not make their per-asset role redundant |
| 34 `Java/out*` folders | Generated build locations already covered by ignore rules | Rebuild reproducible outputs into one standard build tree; inspect any baseline-named directories for unique source/evidence first |
| Root and Java `.gitignore` | Substantial overlapping patterns, but not identical | Prefer root ownership for this repository; preserve unique rules such as `Java/Story premise/` if still intentional |

The 16 root `assets/source` files did **not** match other files in this exact-byte scan. They are misplaced source candidates, not verified redundant copies.

An exploratory filter found 184 PNG duplicate groups with multiple copies outside obvious source/character-refresh paths in `Java/assets`. That filter is only a triage aid: it does not establish which files are loaded or can be removed. The CSV labels likewise classify paths, not runtime reachability.

## Files and tools that could be merged

| Priority | Current files | Proposed consolidation | Keep distinct |
| --- | --- | --- | --- |
| High | `scripts/run-{original,alternate,articulated,grounded}-movement.ps1`, `run.ps1`, `run-profile.ps1` | One `run.ps1 -Movement <style> -Profile` entrypoint with shared build/error handling; old launchers can temporarily delegate | Preserve defaults and the two existing JVM-property conventions until mapped and verified |
| High | `tools/ImportCharacterSheets.java`, `ImportCharacterPass.java` | Shared roster, output paths, chroma cleanup, baseline placement, and strip/metadata writer; selectable import profiles | 3x2, expanded, 24-pose combat, and 16-pose walk layouts have different semantics; do not flatten to one grid |
| High | `generate_character_movement_state_animations.py`, `regenerate_character_walk_animations.py` | Extract shared clipping, roster/direction traversal, preview construction, and strip/metadata helpers | Walking generation and idle/start/stop transitions remain separate pipeline stages |
| Medium | New Java importers and existing Python cutout tools | Common documented input/output contract and manifest; keep `regenerate_universal_assets.py` as the entrypoint for its supported sheet cutouts | Language implementations and different extraction algorithms; `ImportMonsterAttacks` uses component/pose detection, not a generic fixed-grid crop |
| Medium | Java reviews in `tools/` and `src/main/.../diagnostics/` | Shared review harness for output path, deterministic scene setup, capture, and index generation | Feature-specific assertions and scenes |
| Medium | `Java/docs` HTML galleries, review pages, music audition page | One discoverable review index with links; consolidate duplicated gallery scaffolding when maintaining it | Audio audition behavior versus image/animation review behavior |
| Low | Large Java README with accumulating feature notes | Concise setup/run guide plus links to topic documentation | Preserve the specific commands and limitations in topic docs |

These are code-reviewed consolidation candidates, not claims of semantic equivalence. For example, `DestinationApproachReview.java` captures actual GamePanel scenes, while `DestinationApproachesTest.java` asserts generated route properties; keep both. Similarly, movement renderers are selectable implementations, not redundant merely because all animate walking. Keep `musicgen.py` and `music_score.py` as CLI and score-engine layers. Retain specialized tests rather than merging them into a single large test.

## Proposed structure

Use this as a staged target. Keep the existing runtime asset subfolders during the first migration; reorganizing every biome/character path at once adds little immediate value.

```text
README.md                         # project entrypoint and navigation
.gitignore                        # repository-wide generated/local rules
Story premise/                    # preserve existing story links initially
art-source/                       # canonical originals; outside runtime discovery
  characters/<actor>/<batch>/
  monsters/<monster>/<batch>/
  environments/<theme>/<batch>/
  effects/<batch>/
  manifests/                      # source -> tool/profile -> output mapping
asset-review/
  README.md                       # index and review/retention policy
  files/, manifest.json, restore.ps1, verification.json
                                  # existing quarantine bundle, initially unchanged
  reviews/<topic>/<YYYY-MM-DD>-<batch>/
                                  # curated evidence, notes, acceptance status
Java/
  README.md
  assets/                         # accepted runtime assets and required sidecars
    player/, companions/, npcs/, story/, monsters/
    terrain/, city/, interiors/, deco/, ...
    music/, sfx/, effects/
  config/                         # defaults tracked; personal settings ignored
  src/main/java/                  # game code
  src/test/java/                  # assertion-based diagnostics
  src/review/java/                # scene exporters and visual review programs
  tools/
    README.md                     # command, inputs, outputs, dependencies, status
    assets/                       # import/extract/generate commands
    audio/                        # musicgen and score engine
    lib/                          # shared Python helpers; explicit import strategy
    tests/                        # Python tool tests
  scripts/
    build.ps1, run.ps1, test.ps1, review.ps1, benchmark-render.ps1
  docs/
    README.md
    architecture/
    features/
    plans/                        # active/completed status in each plan
    asset-pipeline/
  build/                          # ignored classes/main, classes/test, classes/review
  temp/                           # ignored disposable previews and scratch
  saves/, exports/                # retain local-state handling
```

The source-root split does not require Maven or Gradle. Extend the current PowerShell/javac approach with separate source lists and classpaths. Tests needing package-private game access can retain `com.alderfall.game` while living in a separate source root. Keep compatibility launchers during the transition.

Do not add an additional archive for each tool or feature. Use the existing quarantine bundle for its existing purpose; retire reproducible tool revisions through Git history after a checkpoint. Preserve unique original artwork and useful historical review evidence explicitly.

## Mapping and migration sequence

| Phase | Concrete work | Completion gate |
| --- | --- | --- |
| 1: Record and index | Checkpoint current modified/untracked project work; add tools and review indexes; label tools active, experiment, or retired candidate | Every new import/review command has documented working directory, dependencies, inputs, outputs, and overwrite behavior |
| 2: Standardize entrypoints | Parameterize run commands; introduce shared root/output resolution and tool helpers | Existing launch modes still select the same implementation; tools run from documented entrypoints |
| 3: Separate source art | Move root `assets/source`, `Java/assets/source`, nested `sources`, and character-refresh originals into `art-source`; classify mixed folders file by file | Update Java/Python importers, docs, HTML links, and provenance manifests; compare asset-ID resolution before/after |
| 4: Separate diagnostics | Move assertion tests to `src/test/java`, visual exporters to `src/review/java`; inspect hybrid tests individually | Main build excludes diagnostic code; test/review commands compile with main output and preserve package access |
| 5: Consolidate duplicates | Review CSV groups, retain required logical IDs through explicit aliases or updated consumers; consolidate source masters | Missing-asset count does not increase; animation metadata and directional/action selection still resolve |
| 6: Normalize output and docs | Switch to `Java/build`, add ignore rule, update classpaths; group docs and curated review batches; retire obsolete output directories after inspection | Root and Java launch commands, local HTML links, review regeneration, and quarantine restoration path checks remain valid |

Each phase should be a separate reviewable change. Do not combine package renames, visual replacements, asset deletions, and folder moves in one change.

Specific mapping decisions:

- `Java/assets/animations/character-refresh/.../sources` -> `art-source/characters/<actor>/<batch>/`; review HTML/contact sheets -> review batches; retain any actually used strips in their runtime family.
- `Java/assets/source/monster-attacks-2026-09-23` -> `art-source/monsters/<monster>/2026-09-23/`, with `ImportMonsterAttacks` input configurable instead of hard-coded.
- `Java/tools/*Review.java` -> `src/review/java` when game-dependent; standalone art contact-sheet generators can stay with asset tools.
- `Java/assets/battle/painted-background-prompts.md` -> the corresponding source batch; accepted backdrop PNG stays in `assets/battle`.
- Current review directories -> `asset-review/reviews/<topic>/<batch>` only when their generator output paths and all relative HTML references are updated together. Keep old quarantine paths stable initially.
- Root `assets/` can disappear only after all 16 unique sources have been relocated and their consumers checked.

## Rules to prevent recurrence

- Every import batch records source paths/hashes, tool and profile, output IDs/paths, sidecars, review link, and acceptance status. Put dates/iterations in source/review batch names; preserve established runtime IDs during cleanup.
- Treat `.png`, `.frames`, and optional `.framebounds` as one animation unit. A future manifest may replace sidecars only with a corresponding loader migration.
- New source art never goes under a recursively indexed runtime folder. Accepted generated assets remain tracked unless a separate reproducible distribution policy is introduced.
- Bulk regenerable review frames go to ignored scratch output; retain selected evidence, the generator command, and relevant parameters in tracked review batches. Do not blanket-ignore the existing `asset-review/` archive.
- Use explicit tool input/output arguments and one root-resolution convention. Avoid machine-specific generated-image paths as the primary source.
- Give duplicate runtime stems an explicit validation failure or deliberate alias, rather than relying on traversal order. Make Python asset lookup report ambiguity too.
- Review retirement separately from duplicate detection. Dynamic names, config, saves, exports, fallback logic, HTML, and import recipes can all be consumers.

## Validation for implementation

Before moves, capture selected asset ID -> path -> hash mappings and current diagnostic results. After each phase, build with Java 21 and run the existing smoke test plus checks for affected areas. For animation changes, include `MonsterAttackAnimationTest`, `CharacterAnimationTest`, `GroundedWalkingTest`, and applicable dialogue checks; verify sidecar frame counts/bounds and representative directional poses. For tooling, retain universal-cutout and music-score tests where affected. Review relative links and the quarantine restore script without executing a restore over the working tree.

This report used filesystem inventory, Git status/history, SHA-256 content comparison, and targeted source/documentation inspection. It did not run the game, regenerate art, perform a full reachability analysis, or prove that every duplicate is unused. No build/tests were needed for these documentation-only deliverables. The next best implementation step is phases 1–2, followed by source-art separation; disk savings are secondary to making ownership and lookup predictable.
