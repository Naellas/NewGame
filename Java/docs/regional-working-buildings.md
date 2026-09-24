# Regional working buildings

Nine building types now occupy 38 stable lots across the 24 NPC settlements. They implement the everyday institutions described by `Story premise/world-framework.md`: shared food reserves, winter remembrance and hospitality, water accounting, crossing obligations, flood knowledge and freehold rescue crews.

| Type | Region | Where to find it |
| --- | --- | --- |
| Common Granary | Hearthlands | Oakhaven, Elderford, Archive City, Moonspire |
| Ferry Lodge | Western Reach | Riverside, Briarbridge, Foxbarrow |
| Hall of Names | Stormbound Holds | Highwall, Ironvale, Snowrest, Pineward |
| Winter Smokehouse | Stormbound Holds | Highwall, Ironvale, Snowrest, Pineward |
| Public Cistern House | Sunrealm | Sanctum, Embermarket, Dunewick, Sunmere, Redcairn |
| Roadside Caravanserai | Sunrealm | Sanctum, Embermarket, Dunewick, Sunmere, Redcairn |
| Flood Bellhouse | Fenlands | Belltower, Reedwatch, Mireford, Glimmerfen, Stormfen |
| Reedworkers’ House | Fenlands | Belltower, Reedwatch, Mireford, Glimmerfen, Stormfen |
| Freehold Rescue Lodge | Northroad Freeholds | Northwatch, Cairnvale, Greyharbor |

## In play

Each building has a new transparent exterior sprite, a settlement-specific name, matching entrance and inspection text, an enterable furnished interior, and two residents with local dialogue. Granaries separate seed and food reserves; smokehouses have warming and drying bays; halls of names hold records and hearing benches; cistern houses have water stores and allocation desks. Ferry lodges provide waiting and dispatch areas, Fenland workshops handle reeds or warning bells, caravanserais contain guest rooms, and rescue lodges combine crew stores with sheltered bunks.

These are explorable institutions and conversations. No new production economy, ferry transport or paid lodging service is introduced by this pass.

Ordinary housing remains elsewhere in each settlement. Existing businesses, companion destinations, the Guest Abbey, Charter Hall, Orchard House and seedhouses retain their identities. Oathstead's player-managed buildings are unchanged.

## Implementation

- `RegionalBuildingTypes` defines the nine types, their cultural roles and their stable assignments. Primary lots are `inner_west_row` in cities/towns and `east_cottage` in villages; second types use `inner_east_row` and `south_cottage`. The northern, southern and Fenland regions receive two types per settlement; the other regions receive one.
- `RegionalSettlementIdentity.buildingAsset` routes those lots to the new art before selecting ordinary regional houses.
- `WorldMap.ensureHouseInterior` selects the named room plan and keeper dialogue. `InteriorLayout` provides storage, public-hearing, dispatch, water, craft and rescue plans; the caravanserai uses the existing guest-room composition.
- Building prompts, generated names and all levels of building inspection use the same type metadata. Exterior inquiry NPCs at these doors explain the institution.
- Tall buildings get appropriate rendering dimensions. Generic decoration behind their lots is cleared where it would otherwise render on their roofs; marked landmarks, transitions and blocking fences are preserved.
- Lot keys, coordinates and interior IDs are unchanged. No save schema changes are needed. Interior saves use the existing policy of resuming outside the door; re-entering regenerates the corresponding regional interior. Restart the game to rebuild an already-loaded world.

## Art and review

All nine assets live in `Java/assets/environments/settlements/city/buildings/<region>/`, with filenames beginning `regional_` as listed in [building-prompts.md](../assets/environments/settlements/city/buildings/building-prompts.md). They were generated with the built-in imagegen tool and copied unmodified, preserving alpha. Runtime rendering supplies scaling and lighting. The existing regional house sprites remain in use.

Open [the illustrated guide](../../asset-review/regional-buildings/index.html) to compare all nine sprites and their actual in-game exterior/interior captures. Captures are under `asset-review/regional-buildings/` at the workspace root.

## Validation

`RegionalBuildingsTest` checks all 38 buildings across seeds 0 and 42, including sprite availability/transparency, routing, named interiors, every planned furniture placement, keeper positions and reachable exits. It also exercises actual game entry/return and save/load for each of the nine types. `--render` writes eighteen game captures.

Existing smoke, regional-settlement and interior-layout checks remain applicable. Test saves are isolated under `Java/out-regional-buildings-check/save-check-*/`; player saves are not used.

