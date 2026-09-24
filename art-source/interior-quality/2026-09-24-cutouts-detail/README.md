# Interior sprite quality refresh — 2026-09-24

This batch reviews the 214 PNGs in `Java/assets/environments/interiors` and
replaces 44 older furniture/prop images selected for coarse source detail,
detached sprite-sheet fragments, or inconsistent cutouts. It does not replace
the whole game's art library. Newer household cupboards, trade modules and
architectural modules were retained.

## Inputs and generation

`inputs-manifest.json` records every original logical ID, exact runtime path,
original SHA-256, preserved input, and subject. `inputs/` holds byte-identical
originals. `manifest.json` records exact prompts, generated originals, output
checksums, alpha-edge metrics and review status. The PNGs beside this README
are the generated source assets. Generation uses the built-in `image_gen` tool,
one call per object, with actual transparent backgrounds. No sharpening,
recoloring, chroma-key removal or painted background extraction is applied.

`plan.json` and `extra-plan.json` retain subject/orientation instructions.
Legacy wooden chair IDs have historical orientation conventions; these are
preserved. Alternate chairs follow their existing cardinal directions.

## Import and restore

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File art-source/interior-quality/2026-09-24-cutouts-detail/import.ps1
```

This copies the 44 source PNGs to the exact existing `asset_file` locations in
`inputs-manifest.json`, overwriting only those images. All logical IDs remain
unchanged. Add `-RestoreOriginals` to restore the preserved inputs. The script
checks all sources and destinations before copying. No external dependencies.

## Review

`audit.csv` records the full interior inventory and this batch's decisions.
Purple crystals, cloth and flowers are intentional: the magenta-edge heuristic
is a review aid, not an automatic recoloring rule. Legacy wall-front/corner/door
sprites have remaining edge fringe; they are retained for compatibility and
are not the current modular wall renderer's assets. Retaining an asset is not
a claim that every historical sprite is equally detailed.

The renderer already uses nearest-neighbor fitting for interior art. This batch
changes source artwork, not world footprints or the connected-furniture join
algorithm. Final comparisons and scene captures are documented in
[the review folder](../../../asset-review/reviews/interior-quality/2026-09-24-cutouts-detail/README.md).
