# Equipment appearance

`GameData.itemIcon` resolves equipment into a cached material appearance. Every UI that uses this shared lookup (inventory, equipment slots, shops, recipes, drag previews and tooltips) receives the same icon.

The cutout atlases `assets/items/equipment_weapons_tiers.png` and `assets/items/equipment_gear_tiers.png` contain ten shapes: sword, dagger, axe, spear, staff, bow, mail, robe, leather armor and ring. Each shape has five distinct silhouettes: Common, Uncommon, Rare, Masterwork and Legendary. The asset store isolates each atlas cell into a transparent item sprite and caches it before the UI scales it. Neutral pixels receive the primary material palette, red pixels receive the secondary component palette, and gold pixels receive the ornament palette. Original shading and alpha are preserved.

Assembly items use their existing versioned component keys; no save migration or stat changes are needed. Existing `STANDARD` and `FINE` keys now display as Common and Uncommon, while retaining their stored key names. Crafted quality selects the silhouette and material selects its colors, so a Common mithril sword stays simple but looks mithril. The first two required components control body and fittings, and the optional ornament controls trim. Optional runes currently affect stats, not a separate visual channel. Uniques and equipment with a bespoke special effect keep their original artwork. Recognizable legacy equipment uses its family and named material; equipment outside these ten shapes retains its existing icon. Consumables and raw materials retain their artwork.

`ItemAppearance` owns palette and shape selection. The renderer isolates each atlas object in an expanded cell to avoid neighboring fragments and fits it inside a padded square. `AssetStore` caches composed sources and scaled icons. New templates can be added by extending the atlas and shape mapping; a new material normally only needs a palette entry.

Riverside Market sells Common and Uncommon assembled gear, Highwall Quartermaster sells Rare and Masterwork gear, and the Crypt Provisioner sells Legendary gear. Generation prompts and tool provenance: `assets/items/equipment-material-prompts.md`.

Validation (from `Java`, after compiling):

```powershell
java '-Djava.awt.headless=true' -cp out com.alderfall.game.ItemAppearanceTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.AssemblyCraftingTest
java '-Djava.awt.headless=true' -cp out com.alderfall.game.ItemBrowserTest
```

The appearance test checks all blueprints and qualities, independent main/fitting recoloring, transparent backgrounds, unique preservation and caching. It writes `asset-review/item-appearance/material-quality-preview.png` at the repository root. Assembly checks cover the existing save round-trip.
