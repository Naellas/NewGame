# Retired village and shallow-water textures

Archived on 2026-09-22 at 05:13:30 local time. Originals were moved without modifying their pixels. `manifest.json` records their original locations and SHA-256 hashes.

| Asset | Reason | Replacement |
| --- | --- | --- |
| `submerged_sand.png` | Baked sand/caustic shape reads as a separate circular tile in shallow water. | Continuous water texture and a restrained shallow-water tint in `LayeredTerrainRenderer`. |
| `submerged_sand_variant_1.png` | Alternate stamp has the same discontinuity. | Same continuous shallow-water material. |
| `village_packed_earth.png` | Repeating black tracks and hard square borders conflict with natural village ground. | Biome-tinted earth/gravel with continuous edge coverage. |

`Terrain` now points to water/road fallback assets for menus and other map renderers. The extraction/generation scripts no longer install these retired images into the runtime asset directory. Connected plank walks, paving used elsewhere, and building sprites remain active; this review concerns the specific incompatible ground textures.
