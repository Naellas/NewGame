# Source, tool, and asset organization — 2026-09-23

This pass implements the next cleanup batch without changing gameplay logic or
regenerating accepted art. See [source roots](../src/README.md),
[runtime asset layout](../assets/README.md), and [tool index](../tools/INDEX.md).

## Changes

- 196 Java files remain in main; 76 tests and 31 review/audit/export files now
  have separate roots. One shared animation-audit class is in `testSupport`.
  Eleven study-source files remain with the review sites and are compiled only
  for review builds. Existing package declarations were preserved.
- Python tools are grouped under `assets/{characters,environments,monsters,items,shared}`,
  `audio`, `checks`, and `tests`. Java importers and PowerShell art tools are grouped
  beside their subjects. Five game-dependent Java exporters moved into `src/review`.
- `tools/run.py` lists and runs registered Python commands from the Java working
  directory. Six established flat entrypoints remain as small compatibility
  shims, not duplicate implementations. Shared project-root discovery replaces
  assumptions about a script's depth.
- Fourteen duplicate helper groups were consolidated across 19 tools: 33 former
  definitions now delegate to shared mechanics. Keying policies, trimming
  callbacks, sizing, margins, and resampling choices remain distinct where needed.
- Runtime assets now use character-owner and environment-family subtrees,
  retaining existing class, companion, NPC-region, building, and animation folders.
  Consumers, exporters, browser paths, manifests, and archive restore destinations
  follow the new layout. No asset filenames or sidecar contents were changed.

## Tool retirement

Three tools moved unchanged into
[`archive/pending-deletion/retired-tools-2026-09-23`](../archive/pending-deletion/retired-tools-2026-09-23/README.md):

| Tool | Last committed edit | Evidence |
| --- | --- | --- |
| `extract_city_tilesheet.py` | 2026-05-26 | No active callers found; old grid-slice workflow, whose unused results were already quarantined |
| `extract_city_key_assets.py` | 2026-05-26 | No active callers found; current city imports/refinement have their own tooling |
| `generate_prototype_battle_animations.py` | 2026-06-04 | No active callers found; six-frame prototypes superseded by authored import workflows |

The manifest records original paths, dates, reasons, sizes, and hashes. No files
were permanently deleted. Older regeneration dependencies, `asset_paths`, and
`universal_cutout` remain active: age did not establish redundancy. Absence of
repository callers cannot establish that a human never used a manual command;
recoverable retirement preserves that option.

## Verification and safeguards

- Main-only and combined development builds compile with Java 21 source compatibility.
- Every one of the 3,483 pre-migration asset IDs resolved to the identical image
  hash afterward; no ID was added or lost by the directory migration.
- All 4,701 asset binaries and animation sidecars retained their bytes. All 3,738
  Python basename lookups retained their selected image hashes; shared lookup
  code preserves historical precedence across the renamed family directories.
- All 33 former helper implementations matched the consolidated versions on
  representative pixels, transparent inputs, clipping, geometry, and previews.
- Focused image/path tests and existing cutout/music tests cover the refactored tools.
- Runtime smoke, character/monster animation, grounded movement, and dialogue
  checks plus browser review checks verify the relocated consumers.
- Structural checks reject diagnostics in main, new ungrouped tools, legacy
  asset-family recreation, missing internal tool imports, duplicate Java class
  identities, and new ambiguous runtime asset stems. Archive hashes remain checked.

The 731 existing hygiene exceptions were translated to their new paths, not
expanded. New source art still belongs outside runtime assets. The six existing
duplicate stems are a bounded legacy exception, not a license to add more.

Normal `Java/out` cleanup rejects redirected directories while allowing recognized
OneDrive cloud placeholders. The guard checks ancestors and descendants before deletion.
Tests use fresh scratch output; isolated builds also remain available through
`-OutputDirectory`. Local checks/workflow files are implemented; remote required
status checks still depend on the repository's branch settings.
