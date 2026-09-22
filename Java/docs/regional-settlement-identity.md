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

Three new house sprites and a timber-walk texture were generated with the built-in imagegen tool. Their subdued materials, overhead view and south-facing doors match the existing game art. Most ordinary northern, southern and Fenland homes use them; some older houses and all specialised businesses and story buildings retain their established appearances. Source prompts and provenance are in [prompts.md](../assets/city/regional/prompts.md). The PNGs were copied unmodified, with scaling performed by the renderer.

## Generation and compatibility

- Regional changes run after story and abbey entrances are registered. Building keys and lots remain stable, and entrances, NPC anchors, landmarks, harvestable resources and important props are protected from new water.
- Water may replace open ground. A reachability pass restores crossings wherever new channels divide previously accessible land. Existing city wall fragments that cut off house approaches are opened with the smallest practical connection, preferring existing walkable ground.
- Plank and packed-earth roads use dedicated `Terrain.PLANK_ROAD` and `Terrain.PACKED_ROAD` values. They remain connected roads for pathfinding and frontage checks, distinct from decorative timber/earth courts. The renderer uses their terrain texture without the generic road-colour overlay.
- Ordinary vegetation changes by habitat. Commons connect to existing paths, and plants avoid worker and gathering positions. A missing stone-planter reference was replaced by an existing barrel planter so towns no longer show its fallback box.
- Maps regenerate from their existing seed. The save loader already moves an invalid saved position to nearby walkable ground; the regional test exercises this against Mireford's water tiles. No save format change is required. Restart the game to rebuild maps already held in memory.

## Validation and review

`RegionalSettlementTest` checks 24 settlements and 48 commons across seeds 0, 42 and 2026: asset availability and house transparency, accessible doors and directional exits, common features, worker targets, daily routines, regional waterways, deterministic regeneration and saved-position recovery. `--render` also captures nine representative settlements using the actual game renderer at noon. `--render-only` makes the captures alone.

Captures are under `asset-review/regions/` at the workspace root, including `city_highwall.png`, `city_sanctum.png`, `city_belltower.png`, `village_sunmere.png` and `village_mireford.png`.

The change is a regional settlement pass, not a replacement for every civic building, interior, enemy camp or quest. Existing main street layouts and building footprints remain; the different waterways, open spaces, ordinary homes and working commons supply the new regional character.
