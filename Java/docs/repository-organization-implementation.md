# Organization implementation — 2026-09-23

## Installed layout

The character review entrypoint is now
[`Java/tools/reviews/characters/index.html`](../tools/reviews/characters/index.html).
All six connected site folders moved together: characters, movement-physics
(including calibration), cloth-physics, grounded-movement, dialogue-motion, and
monster-attacks-2026-09-23. Their pages, JavaScript, frames, evidence, and study
source files remain together. The previous character URL is a small redirect
that preserves query parameters and tab hashes.

Updated Java exporters, browser scripts, documentation, and asset URLs to the
new locations. New browser-check screenshots go to `Java/temp/review-checks/`.
Normal game builds exclude the moved study sources. `build.ps1 -IncludeReviews
-OutputDirectory temp/<task>/classes` compiles game and study sources together
for deliberate regeneration; the existing package-private access is preserved.

The existing 437-file quarantine moved from `asset-review/files` to
`Java/archive/pending-deletion/legacy-assets/files`. Its manifest, original
verification record, README, and restore script moved with it. No candidate was
permanently deleted, and no additional runtime asset was retired. The restore
script now resolves the new location and supports `-ValidateOnly`.

`scripts/run.ps1` now owns movement/profile options and build error handling.
The five existing mode/profile entrypoints are compatibility wrappers. Their
original JVM flags and the default game command are preserved; `-PrintCommand`
allows checking these without launching the game.

## Rules and checks

`AGENTS.md` now describes the installed review and quarantine locations. The
hygiene checker rejects newly recreated review content under the retired paths;
only the old bookmark redirect is permitted. The exact-path legacy baseline was
not expanded: 731 pre-existing exceptions remain.

`Java/scripts/check.ps1` runs 11 policy tests and both read-only checkers:

- Layout and tracked/local-output rules.
- Literal HTML links and outdated Java review-export paths.
- Archive manifest coverage, path safety, byte sizes, and SHA-256 checksums.

The GitHub Actions workflow runs the same Python checks on pushes and pull
requests once these changes are committed and pushed. Branch-required checks
and reviewer rules are repository settings; they have not been changed here.
Local instructions and CI files alone cannot prevent a user with bypass rights
from overriding policy.

## Validation

- Java sources compiled with `--release 21` into isolated scratch output.
- Game and all 11 optional review source classes compiled together successfully.
- `SmokeTest` passed using isolated saves/config and the existing runtime assets.
- `GroundedWalkingTest`: 336 actor/direction/cadence combinations passed.
- `WalkAnimationSpeedTest` and `DialogueAnimationTest` passed (26 figures).
- `MonsterAttackAnimationTest`: 58 strips, 696 frames, 80 catalog entries passed;
  it successfully regenerated evidence into the relocated monster review folder.
- Headless Chrome checks passed for local-file review loading, all party combat
  sheets, five party motion modes, 58 monster sheets, old-URL redirection, movement
  controls, dialogue embedding, calibration, and mobile layouts.
- All 437 archived assets passed checksum and restore-path validation without
  restoring any files.
- All 11 repository tests, layout checks, and review/archive integrity checks passed.

A before/after SHA-256 comparison recorded no removed files or changed runtime
artwork/configuration/gameplay Java code. Within the asset tree, only the
character-refresh README link changed. The 12 changed diagnostic Java files
redirect review output paths; gameplay implementations were not edited.

The pre-existing normal build refuses to recursively clear `Java/out` because
it is marked as a redirected/reparse directory in this OneDrive workspace.
That guard remains intact. The new `-OutputDirectory temp/<task>/classes`
option allowed verification without touching that directory. Use a fresh task
directory if OneDrive marks previous output as a reparse directory too.

## Remaining migration work

The original organization report is a historical inventory and broader staged
proposal, not the installed filesystem map. Its duplicate CSV retains audit-time
paths. Source-art separation, semantic importer/helper consolidation, runtime
asset aliases, and production/test source-root separation still need individual
consumer-aware migrations. This implementation does not infer that duplicate
hashes make active assets safe to delete. The installed checks and site/archive
layout provide the foundation for those migrations without changing gameplay.
