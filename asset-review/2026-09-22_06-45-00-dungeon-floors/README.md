# Dungeon floor asset review

Reviewed on 2026-09-22. The five retired files are preserved under their original names; the
reviewed replacements are copied beside them with a `replacement_` prefix.

Four old 264 px images had opaque white/checkerboard margins baked into the pixels. Because those
pixels were not transparency, border removal and terrain blending could not hide them. Repeating the
assets produced the bright grid visible in dungeon rooms. The 48 px crypt base passed the technical
checks but failed the visual review because its bright dash motif overwhelmed every room when tiled.

| Asset | Review result | Replacement use |
| --- | --- | --- |
| `dungeon_crypt_floor.png` | Rejected: high-contrast dash motif produces obvious wallpaper repetition. | Quiet irregular charcoal crypt masonry that leaves props and routes legible. |
| `dungeon_moss_floor.png` | Rejected: white opaque perimeter and non-seamless inset tile. | Full-bleed damp/moss material for wet folklore and surface-biome ingress. |
| `dungeon_rubble_floor.png` | Rejected: white opaque perimeter and framed rubble patch. | Seamless walkable damage for burial, prison, and goblin spaces. |
| `dungeon_torch_floor.png` | Rejected: checker/white perimeter and isolated lighting stamp. | A sparse one-tile functional/wayfinding landmark, not a repeated room fill. |
| `dungeon_boss_sigil_floor.png` | Rejected: white opaque perimeter and oversized framed set piece. | One terminal objective landmark on the final storey. |

All replacements are native 48x48, fully opaque, and have identical opposite edge pixels. The
runtime regression test also rejects transparency, bright white/checker artifacts, or broken seams.
The generated masters and exact prompt specification live in
`Java/assets/source/dungeon-floor-replacements`; the deterministic importer lives at
`Java/tools/assets/environments/import_dungeon_floor_replacements.ps1`.
