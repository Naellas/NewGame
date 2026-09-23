# Supplied household and warehouse furniture

The three supplied sheets are preserved under `assets/source/household-expansion/`.
`tools/assets/environments/import_household_expansion.py` extracts 216 named transparent candidates with
authored crop bounds, conservative black-matte removal, and neighboring-fragment
cleanup. The household sheet's original alpha is retained. No artwork is synthesized.

The runtime selection contains **51 new furnishings and four replacements**.
Replacements preserve `interior_bookshelf`, `interior_crates`, `interior_barrels`,
and `interior_low_cupboard`, so existing editor choices and saved identifiers keep
working. The other candidates are kept outside the runtime asset index, in the
review archive. In particular, existing directional chairs, modular tables and
benches, continuous rugs, round table, bright plants, herb rack, kitchen station,
and candle centerpiece were retained after game-scale comparison.

Open `../../asset-review/household-expansion/index.html` for the searchable comparison
gallery. `manifest.json` records each crop, selection decision, and runtime ID.
The gallery includes the original versions of replaced art and the existing art
kept during review. All unused directional views remain named candidates rather
than redundant entries in the village editor.

## Placement

`InteriorFurnishings` shares names, editor categories, costs, collision dimensions,
and drawing height across the editor, map placement, and renderer. Tall storage
uses a compact floor footprint and taller drawing bounds. Wall decorations require
an exposed horizontal wall. Tabletop details require a supported surface and reject
stacked duplicates; decorated cooking counters are not treated as empty surfaces.
The renderer anchors details within the supporting sprite's visible tabletop,
including cabinets whose image is narrower than their reserved tile footprint.

`InteriorLayout` retains its activity groups, two-tile circulation spine, and resident
positions. Deterministic furnishing substitutions and authored additions supply:

- Inns: wardrobes, washstands, sofas, firewood baskets, and fireplaces.
- Shops: pantry/crockery/supply shelves, produce stock, and wall parcel shelves.
- Workshops: tool storage, coal stores, ladders, and tool shelves on walls.
- Granaries and smokehouses: appropriate food crates, pallets, and a delivery cart.
- Homes and studies: drawers, wall pictures or books, and supported writing details.

Every new item is available in the village/interior editor; automatic layouts use
the subset appropriate to their activities. Stocked shelves and food crates are
decorative furnishings, not new inventory containers or production mechanics.
Newly generated interiors use the revised plans; an already-open cached interior
is not forcibly rebuilt during a running game session.

## Reproduce and verify

From `Java/`, run `python tools/assets/environments/import_household_expansion.py` to regenerate the
review archive, or add `--install` to apply the reviewed runtime selection. Original
replacement backups in the review archive are preserved on repeat runs.

Compile the Java sources, then run:

```powershell
java '-Djava.awt.headless=true' -cp out-asset-import-check com.alderfall.game.InteriorFurnishingsTest
java '-Djava.awt.headless=true' -cp out-asset-import-check com.alderfall.game.InteriorLayoutTest
java '-Djava.awt.headless=true' -cp out-asset-import-check com.alderfall.game.InteriorPlacementTest
java '-Djava.awt.headless=true' -cp out-asset-import-check com.alderfall.game.render.world.InteriorLightTest
java '-Djava.awt.headless=true' -cp out-asset-import-check com.alderfall.game.RenderCacheTest
java '-Djava.awt.headless=true' -cp out-asset-import-check com.alderfall.game.InteriorRenderTest ../asset-review/household-expansion/interiors
```

The furnishing diagnostic exercises all 51 new assets through real editor placement,
collision, hit-testing, movement rollback, removal, categories, and export. It checks
306 deterministic plans across 17 uses and six regional styles, plus tabletop bounds
and camera translation at four tile scales. The existing composition suite covers
288 plans, and generated-world placement covers 54 interiors. Day/night captures
use the actual renderer in six settlements.
