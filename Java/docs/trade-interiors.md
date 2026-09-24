# Connected trade interiors

Blacksmiths, carpenters, bakeries, alchemy shops, libraries/studies and general
shops now have composed production, storage and customer areas. These plans replace
the scattered household-detail additions for those uses. Other building plans,
regional wall/floor materials and existing player-village footprint rules remain.

- Blacksmith: forge, adjacent fuel and quench water, anvil and stock, finishing
  benches, finished goods and an order desk. Partitions separate work areas.
- Joinery: continuous tool cupboards and assembly benches, timber/jigs, service
  worktop and orders. Pillars mark the service threshold.
- Bakery: oven and flour store, mixing/proving bench, bread service, reserve
  ingredients and a small carpeted dining group.
- Alchemy: reagent fronts, distillation and bottling, washing, a dispensary and
  screened consultation seating.
- Library: continuous book/scroll fronts, shared reading tables and chairs,
  lectern, timber arcade, copying desks and screened reading nook.
- General shop: stock wall, service run, household-goods cupboards, packing and
  carpeted waiting area.

Alchemy/apothecary buildings have their own `alchemy` theme. Profession-specific
styles are resolved before generic market/shop names, so a market-named bakery
gets a bakery interior. Libraries and archives resolve to the study plan.

The 13 new Imagegen assets are available through the existing interior placement
catalog. Each storage/worktop bay occupies two tiles. Matching bays placed on the
same row with touching footprints join automatically; they need no manual variant
selection. Four cached variants preserve outside caps and remove inner posts.
Removing a bay restores the exposed ends. Connections currently form horizontal
runs, not L-shaped or north/south runs. Different-height furniture does not join.

The new smith, joinery, bakery and alchemy worktops expose the corresponding
crafting station. Crafting reach uses their complete two-tile footprint. Library
worktops are reading furniture. The forge hearth participates in existing fire
lighting and ember rendering. Carpets use connected rug coverage independent of
furniture collision; screen panels and architectural walls use existing placement.

Originals, prompts and import recipe:
`art-source/trade-interiors/2026-09-24-connected-workrooms` at repository root.
No new save format or destructive migration is needed; restart the game to rebuild
generated interiors with the new layouts. Player-authored furniture remains governed
by the existing edit/placement rules.

Validation: InteriorLayoutTest (1,620 plans), InteriorFurnishingsTest (324 plans,
catalog, alpha, editor movement and export), InteriorPlacementTest,
ConnectedFurnitureTest (26 families, joins/removal at three scales),
TradeInteriorTest (theme routing, authored runs and crafting), FurnitureQuestTest.
See the [review captures](../../asset-review/reviews/trade-interiors/2026-09-24-workrooms/README.md).
