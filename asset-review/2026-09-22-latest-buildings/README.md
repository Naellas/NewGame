# Latest building asset review — 2026-09-22

Reviewed the newest regional buildings, folklore buildings, settlement icons and city walls against their in-game use, with older buildings retained as visual comparisons. The initial inventory contains 168 PNGs, including source sheets and older fallbacks; 76 were recent runtime candidates. Ten new `*_gate_*_clean` runtime assets appeared during this review and were checked as well.

## Repairs

13 sprite designs regenerated, installed in 15 asset paths:

- Freehold rescue lodge: restored the turret finial and transparent clearance around roof edges; retained the entrance, stretcher, blankets, ropes, crates, barrel and windsock.
- Five regional vertical gates: removed detached neighboring wall fragments; kept open archways and regional stone, snow and vegetation details.
- Five regional end towers: removed unrelated top-edge fragments that distorted the loader's fitted bounds.
- Sun corner tower: removed the brown matte and restored the complete bottom connector.
- Sun horizontal gate: completed the right connector and removed the exterior/arch matte.
- The two newly introduced active Sun `*_clean` gate paths now use the repaired Sun images too. The other eight new clean gates were retained.

The renderer changed during the review to use clean gate aliases. The repaired original gate paths remain compatible fallbacks/editor assets. No recent unused/editor-only assets were moved out of the live asset directories.

All replaced originals are under `before/`, retaining their full original project paths. SHA-256 records are in `repair-manifest.json` and `active-gate-manifest.json`. Generated masters are also saved under `Java/assets/source/latest-building-integrity-2026-09-22/`. Generation and correction prompts are recorded in the JSON files here. Three gate attempts that introduced portcullises were corrected before installation.

## Visual evidence

- [Before/after sheet 1](runtime-before-after-1.png): original, repaired on dark, repaired on light, and 48 px fit, using the actual AssetStore.
- [Before/after sheet 2](runtime-before-after-2.png).
- [Rescue lodge in game](scenes/rescue-lodge.png).
- [Sun gates in game](scenes/sun-gate.png).
- `before-scenes/` preserves the initial game captures. Other concurrent terrain/renderer changes also affect the scene comparison; those changes are not attributed to this asset pass.
- The regional, overworld, building and wall contact sheets capture the wider audit. `clean-gates.png` records the ten additional gate aliases before replacing the two Sun aliases.

## Verification

- Full current Java source compilation passed with JDK 21.
- LatestBuildingsPreview passed: 13 repaired designs rendered through AssetStore; all 76 initial recent candidate stems resolve.
- RegionalSettlementTest passed: seeds 0, 42 and 2026, 24 settlements and 48 commons each; deterministic regeneration and saved-position recovery passed. This also validates the new clean gate assets.
- RegionalBuildingsTest passed: nine building types, 38 visitable buildings and 24 settlements for each of two seeds; furniture, resident routes, in-game naming, entry, return and save/load passed.
- Repaired PNGs have real alpha, clear visible silhouette margins and readable details at game scale. Background RGB can remain colored where alpha is zero; composited previews, rather than RGB-only previews, were used to assess cutouts. The Sun horizontal gate's stone alpha peaks at 254/255, visually opaque at runtime.
- Every installed replacement has a byte-exact archived original. No gameplay paths were removed.

## Remaining older findings

The June fallback building set still includes green-tinted chimney smoke on some upgrades, white negative-space remnants on granary/watchtower variants, and visibly cropped lower floors on some base buildings. These are older assets, recorded for a separate targeted pass; they were not silently marked clean by this latest-assets review. The newest regional/folklore buildings and overworld icons otherwise showed no obvious missing structural features in this review.
