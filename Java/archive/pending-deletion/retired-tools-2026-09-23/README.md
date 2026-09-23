# Retired legacy tools — 2026-09-23

Three source tools are preserved byte-for-byte under `files/Java/tools/`.
`manifest.json` records original paths, last committed modification dates,
retirement reasons, byte sizes, and SHA-256 hashes.

- `extract_city_tilesheet.py` (2026-05-26): manual grid-slice extraction. No active
  callers were found; the earlier cleanup already quarantined unused grid slices.
- `extract_city_key_assets.py` (2026-05-26): manual heuristic extraction from the
  old city sheet. Current refinement/import tools cover the active workflow.
- `generate_prototype_battle_animations.py` (2026-06-04): old six-frame procedural
  attacks. Authored character/monster importers now drive the active workflow.

Age was a candidate filter, not the retirement criterion. Older shared helpers
and regeneration dependencies remain active. No runtime artwork was deleted or
regenerated. Original imports/paths in these snapshots are intentionally frozen;
review and adapt them to the current tool/asset layout before historical reuse.
Restore a reviewed file from `files/<original path>` only if the destination is
absent, and verify its manifest checksum first. Nothing is purged automatically.
