# Merchant stock

`Shop.baseStock` and `Shop.upgradedStock` define staples shared by town merchants
and assigned village shops. Furnishings add upgraded staples to the next delivery.
`MerchantStock` expands the pool from the full equipment, consumable, material,
and curated recipe-book catalogues, retaining only goods appropriate to the trade.
Equipment, consumables and materials respect their level requirements.

- Blacksmiths sell weapons, shields, and armor.
- Carpenters sell bows, wooden shields, axes, timber, and woodcraft recipes.
  Existing `wood` is the processed lumber item; no separate plank item is added.
- Herbalists sell potions, salves, herbs, seeds, and other alchemy ingredients.
- Farmers sell produce, seeds, fiber, wool, and food. Grain quest objects remain
  quest objectives, rather than purchasable shortcuts.
- Bakers and innkeepers sell food and cooking supplies.
- Forestry workers sell timber and axes; mines offer ores and picks; fishers
  offer fish, lures, and fishing recipes.
- Furriers sell cloth, leather, and clothing; shrine vendors sell charms and
  remedies. Regional granaries, reedworks, smokehouses, and other working
  institutions have corresponding trade profiles.

Generated interior merchants receive explicit shop IDs. `Shop.forNpc` uses a
worker's `NpcJob` before a market ID, including when that worker commutes. Generic
markets and expedition quartermasters retain their mixed catalogues. Customers
and apprentices do not acquire shops from their dialogue text.

Materials now cost `4 * tier * tier` gold using `MaterialCatalog`; iron picks cost
60 gold and shell lures 12. Existing item/equipment prices take precedence.
Selling still uses half the item price, with a one-gold minimum, and all shops
continue to accept player goods. Player sales do not add unrelated goods to the
merchant's sale catalogue.

## Regions and variants

The settlement climate uses `InteriorStyle`, including the parent settlement for
interiors and the nearest settlement for overworld workers and Oathstead. Northern
freeholds such as Northwatch retain their cold climate.

- Sunrealm: sunmetal, emberite, obsidian, palm, coconut, and sun/ember/dune gear.
- North: froststeel, cobalt, mithril, and frost/rime/snow goods.
- Fenlands: bog iron, cypress, and marsh/reed goods.
- Hearthlands, river and Archive: verdant materials, ancient or magical wood,
  enchanted bark, and glowroot.

These specialties have **no imports** outside their home climates. The check
applies to ores, ingots, named equipment, potions, affixed equipment, and every
material embedded in assembled equipment keys. Common goods remain widespread.

Deliveries keep eligible staples and independently select 45% of the wider
candidate pool. Existing affix rolls provide enchanted variants. All 18 assembly
blueprints and five quality grades can contribute valid combinations of local
materials; combinations rotate rather than enumerating every possible crafted
item in one shop. Unique equipment stays out of repeatable merchant stock.

## Quantities and restocking

Merchants carry one of each equipment item, 4–12 of each material, and 2–6 of each
other item. Purchases reduce quantities; sold-out tiles remain visible with stable
keyboard indices. The shop header shows the next delivery day.

The default interval is **three in-game days**. Change it under **Settings →
Gameplay → Merchants → Restock every**, from 1 to 30 days, or set
`shop_restock_days` in `config/gameplay.json`. Saved local settings override the
JSON default. Changing the interval recalculates the due date from the last
delivery; reducing it can make a delivery immediately due.

`ShopInventory` stores deliveries per canonical NPC or assigned village building.
Reopening, leveling up, commuting, and saving/loading do not reroll deliveries.
Restocking happens lazily on the first browse/purchase at or after the due day.
Skipping several days preserves the delivery cadence. `SaveSystem` persists
quantities (including zero) and last-delivery days; older saves start with fresh
stock on first use. Starting a new adventure clears merchant inventories.

Validation: `scripts/test.ps1 -Test com.alderfall.game.ShopStockTest` checks stock
identities/prices, trade boundaries through upgrades, generated merchant routing,
and purchases through worker dialogue. `MerchantRestockTest` covers regional pools,
every assembly family/quality, independent depletion, exact and skipped-day
restocks, settings persistence, and actual save/load. `ItemBrowserTest` covers
the trade UI; `TradeInteriorTest` covers trade interior routing.
