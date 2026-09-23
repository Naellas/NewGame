# Working rules for Echoes of Alderfall

The active game is the dependency-free Java 21/Swing project in `Java/`.
Read `Java/README.md` and the relevant topic documentation before changing behavior.
The target structure in `Java/docs/repository-organization-report.md` is a staged
proposal, not permission to perform a wholesale migration during unrelated work.

## Placement and ownership

- Reuse an existing implementation or tool before adding another. Search for the
  same responsibility first; parameterize variants rather than cloning launchers.
- Accepted game assets belong under `Java/assets/characters/<type>/<owner>`,
  `environments/<family>`, `effects`, `items`, `music`, or `sfx`; see
  `Java/assets/README.md`. Keep animation strips under their character owner or
  effect family. Do not recreate the retired flat player/monsters/city/biome roots.
  Put NEW original sheets, prompts, and import recipes in
  `art-source/<topic>/<batch>/`. Do not add source/sources folders inside runtime
  assets or extend the legacy root `assets/` tree.
- Keep animation PNGs, `.frames`, and `.framebounds` together. Preserve logical
  asset names and dynamic direction/action lookup. Identical bytes alone never
  prove that an asset name or metadata sidecar can be removed.
- Each new art batch needs a README recording original inputs, import command,
  output paths/IDs, and review status. Use dates/versions for source batches;
  avoid new runtime `_final`, `_new`, or `_backup` naming conventions.
- Put disposable captures, experiments, and alternate compilation output under
  ignored `Java/temp/<task>/`. Use the existing `Java/out/` for normal builds.
  Do not create more top-level `out-<task>` directories.
- Curated visual evidence belongs in
  `asset-review/reviews/<topic>/<YYYY-MM-DD>-<batch>/` with a README explaining
  reproduction and acceptance. Keep bulk intermediate frames in scratch space.
  Interactive review sites and their subsites belong in `Java/tools/reviews/`.
  The old `asset-review/characters/index.html` is only a bookmark redirect; do not
  recreate content beside it. Preserve archived candidates and restoration data
  under `Java/archive/pending-deletion/<batch>/` with an exact-file checksum
  manifest, original paths, reasons, and README. Do not permanently delete them
  as part of unrelated cleanup.
- Reusable tools belong in the subject folders under `Java/tools/assets/`,
  `audio/`, or `checks/`; Python tests belong in `Java/tools/tests/`. Register
  Python commands in `Java/tools/tool-index.json` and update `INDEX.md`.
  Prefer `tools/run.py NAME` to ad-hoc working-directory assumptions; share paths
  through `tools/project_paths.py` and asset routing through `asset_paths.py`.
  Launchers belong in `Java/scripts/`. Do not add more flat tool files or shims.
  Document each new tool in `Java/tools/README.md`: purpose, invocation, inputs,
  outputs, dependencies, and whether it overwrites files. One-off probes belong
  in scratch space. Use configurable paths, not personal generated-image paths.
- Keep existing Java package declarations when moving source unless a package
  refactor is explicitly part of the task. Directory names do not imply packages.
  Production belongs in `src/main/java`, assertions in `src/test/java`, exporters
  in `src/review/java`, and shared development-only support in `src/testSupport/java`.
  Main must compile without development source roots. Use `scripts/test.ps1` or
  the build switches; never move diagnostics back into main to fix a classpath.
- Keep local saves/settings private and ignored. Do not mass-ignore assets or
  review archives to conceal an organization problem.

## Completion

- Preserve unrelated modified/untracked work. Never bulk-delete duplicates or
  generated-looking directories without checking their purpose and contents.
- For retirement, inspect Git history, callers, documented commands, inputs, and
  replacement workflows. Old modification dates are candidate signals only.
  Archive confirmed retired tools with an original-path/hash manifest and a
  reason. Keep active regeneration dependencies even if they are old.
- Update consumers and relative links in the same change as any move.
- Run `powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/check.ps1`
  from the repository root. This runs policy tests, layout checks, review-link
  checks, tool-import/class-identity checks, and quarantine checksum validation.
  Resolve new findings. Python
  checkers can also run individually on non-Windows systems.
- For Java behavior changes, run the relevant build and diagnostics; for tooling,
  run affected tool tests. Do not regenerate the whole asset library just to test
  a small change. State validation and any remaining limitations in the handoff.
- Do not expand the hygiene baseline, weaken checks, or add ignores merely to
  make a task pass. Intentional policy changes must explain the reason in the
  change description. Existing baseline entries are migration debt, not examples
  to copy. Ordinary compliant work needs no extra permission ceremony.

Enforcement and limits: `Java/docs/agent-guidelines-and-enforcement.md`.
