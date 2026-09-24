# Regional settlement identity

This pass changes the ground, waterways, domestic architecture and daily activity of all 24 existing NPC settlements. It follows the Hearthlands, Stormbound Holds, Sunrealm, Western Reach, Veiled Fenlands and Northroad Freeholds cultures in `Story premise/world-framework.md`. Oathstead's player-managed plots remain outside this generator.

| Region | Visible land use | Local commons and customs |
| --- | --- | --- |
| Hearthlands | Grass and orchard ground around existing streets, shared ovens, seed trays and tool stores | Seed Commons and Orchard Walk; shared stores and seasonal tending |
| Western Reach | River commons, waterside crossings, orchard ground and carriers' carts | River Common and Orchard Landing; crossing duties, freely given food and formal woodland invitations |
| Stormbound Holds | Low timber/turf homes, snow, pine shelter and cleared yards | Winter Stores Yard and Names Cairn; rescue supplies, guest blankets and remembrance |
| Sunrealm | Pale courtyard homes, small water basins and irrigation runs, planted soil near water | Guest Water Court and Irrigators' Garden; first-cup hospitality and water maintenance |
| Veiled Fenlands | Reed-roof stilt homes, wet ground, channels and timber walks | Listening Landing and Reed Garden; flood warnings, garden repairs and the Deep Listeners |
| Northroad Freeholds | Snowbound working yards inland; timber walks and a water edge in Greyharbor | Rescue Yard and Crew Cairns; council accountability and remembrance of crews |

Each settlement has two commons selected from accessible open ground. Their positions respond to the existing lots, important props and paths. Common workers have local dialogue, daytime work targets, evening gathering points and a southern midday rest. Severe-weather shelter behaviour still takes precedence. Approaching a common adds a one-time local observation to the travel log; settlement arrivals describe regional materials and land use.

Three new house sprites and a timber-walk texture were generated with the built-in imagegen tool. Their subdued materials, overhead view and south-facing doors match the existing game art. Most ordinary northern, southern and Fenland homes use them; some older houses and all specialised businesses and story buildings retain their established appearances. Source prompts and provenance are in [prompts.md](../assets/environments/settlements/city/buildings/regional-prompts.md). The PNGs were copied unmodified, with scaling performed by the renderer.

The overworld now uses the same authored-cutout approach as Archive City. Briarbridge, Ironvale, Reedwatch, Embermarket, Northwatch and Greyharbor each have a unique town silhouette. The twelve named villages resolve through six cultural silhouettes: Hearth, River, North, Sun, Fen and Freeholds. This keeps settlements within one region visually related while their interiors, names and story content remain individual. The exact asset mapping lives in `RegionalSettlementIdentity.overworldAsset`, and generation provenance is in [settlement-overworld-prompts.md](../assets/environments/settlements/city/overworld/settlement-overworld-prompts.md).

## Town interior programs

The September 24 [district layout](town-architecture.md#districts-public-space-and-suburbs-2026-09-24)
now relocates the seven towns' lots into functional quarters, reserves public
squares and gardens, and adds suburbs outside an inset wall ring. The stable-lot
descriptions below document the earlier regional pass; town keys and original
interior identities remain stable, but exterior coordinates and footprints change.

Towns no longer inherit only the capital's generic building mix. Each has a folklore-led civic program and two named quarters. Required institutions retain stable lot keys so their doors, generated interiors and saves remain compatible.

| Town | Named quarters | Required civic and working buildings |
| --- | --- | --- |
| Briarbridge | Charter Quay; Abbey Orchards | Bridge Court, Guest Abbey, Charter Granary, Ferry Lodge, Millwheel Workshop |
| Ironvale | Forge Ward; Names Court | Forge Keep, North Armory, Hall of Names, Winter Smokehouse, Pass Barracks |
| Moonspire | Survey Close; Seed Ledger Ward | Survey Tower, Map Stacks, Common Granary, Public Scriptorium, Illuminators' Workshop |
| Reedwatch | Bell Landing; Reedwright Walk | Listening Tower, Bellwrights' Workshop, Flood Bellhouse, Reedworkers' House, Eelers' Landing |
| Embermarket | First-Cup Court; Cistern Ward | Sun Court, Water Ledger House, Cistern House, Caravanserai, Irrigators' Apothecary |
| Northwatch | Signal Yard; Rescue Close | Signal Tower, Ropewrights' Shed, Rescue Lodge, Winter Rescue Stores, Stranded Travelers' Hall |
| Greyharbor | Beacon Quay; Returned Crews Walk | Storm Beacon, Netmakers' Loft, Rescue Lodge, Raised Storm Stores, Returned Crews House |

The interior generator removes the inherited rectangular road grid, joins four gates to a civic hub, branches lanes toward the named quarters and repairs any detached door-road component. Building footprints receive culture-appropriate stone, packed-earth or timber footings with softened sides and broken aprons; this prevents transparent building margins from exposing water or dungeon-wall tiles without creating rectangular paving mats. Functional districts use sparse, irregular material fields around their institutions rather than hard walls.

Town walls now stop at the outer map boundary. All interior wall tiles, including the old solid footprint notches, become regional ground so the expanded map reads as usable outskirts rather than a dark wall block. Capitals may retain narrow authored defensive lines, but their former solid corner notches are also regional outskirts; fortification comes from gates and landmark buildings rather than filled rectangles.

## Generation and compatibility

- Regional changes run after story and abbey entrances are registered. Building keys and lots remain stable, and entrances, NPC anchors, landmarks, harvestable resources and important props are protected from new water.
- Water may replace open ground. A reachability pass restores crossings wherever new channels divide previously accessible land. Existing city wall fragments that cut off house approaches are opened with the smallest practical connection, preferring existing walkable ground.
- Plank and packed-earth roads use dedicated `Terrain.PLANK_ROAD` and `Terrain.PACKED_ROAD` values. They remain connected roads for pathfinding and frontage checks, distinct from decorative timber/earth courts. The renderer uses their terrain texture without the generic road-colour overlay.
- Ordinary vegetation changes by habitat. Commons connect to existing paths, and plants avoid worker and gathering positions. A missing stone-planter reference was replaced by an existing barrel planter so towns no longer show its fallback box.
- Maps regenerate from their existing seed. The save loader already moves an invalid saved position to nearby walkable ground; the regional test exercises this against Mireford's water tiles. No save format change is required. Restart the game to rebuild maps already held in memory.

## Validation and review

`RegionalSettlementTest` checks 24 settlements and 48 commons across seeds 0, 42 and 2026: asset availability and house transparency, accessible doors and directional exits, common features, worker targets, daily routines, regional waterways, deterministic regeneration and saved-position recovery. Town checks additionally enforce unique building and district programs, safe foundations, absence of orphan interior walls, gate-connected institution streets and a limit on uninterrupted straight roads. `--layout-only` runs the structural checks independently from the asset catalogue. `--render` also captures sixteen representative settlements using the actual game renderer at noon. `--render-only` makes the captures alone.

Captures are under `asset-review/regions/` at the workspace root. They include all five capitals, all seven towns and four representative villages.

Overworld silhouette captures are under `asset-review/settlement-overworld/`. They include all six unique towns and one representative village from every regional family, rendered through the actual camera, lighting, weather and road layers.

The change is a regional settlement pass, not a replacement for every individual building interior, enemy camp or quest. Stable building lots remain, while the street graph, foundations, district surfaces, waterways, open spaces, ordinary homes and working commons are rebuilt into the regional character.
