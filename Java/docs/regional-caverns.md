# Regional cavern appearance

Caves and goblin warrens use continuous ground and dark bedrock, with cliff faces only along exposed southern walls and narrow rocky lips along the other edges. Corner clipping, contact shadows, and gravel near walls replace the old full-cell wall repetition. Walkability, room layouts, stairs, encounters, and save formats are unchanged.

`CavernStyle` selects ice for Highwall/Northroad, moss for Belltower, sandstone for Sanctum, and dark stone elsewhere. Snow, swamp/water, and desert/badlands entrance terrain override the political region; temperate forest entrances use moss. Selection uses the existing dungeon context on every floor, not the site name: the currently placed Frosthollow site lies in Sanctum and therefore uses sandstone. Regional cave dressing uses existing roots, mossy rocks, ice crystals, lanterns and supplies.

## Resources and population

Six dedicated resource nodes and up to six small props populate room edges on each cavern/warren floor. Deposits avoid critical routes, stairs, existing props and encounter slots; neighboring props have a one-tile gap. Existing gathering, depletion, profession bonuses and town-entry resource renewal apply. No new item types or generated artwork are needed.

| Material theme | Resources |
| --- | --- |
| Ice | Froststeel, iron, crystal, coal, silver, copper; mithril replaces copper below floor two |
| Moss | Bog iron, mushrooms, ancient roots, copper, coal; verdant ore replaces blue mushrooms below floor one |
| Sand | Sunmetal, copper, rock salt, coal, iron; emberite replaces obsidian below floor one |
| Stone | Iron, copper, coal, pale mushrooms, tin; silver replaces crystal below floor one |

Cavern/warren room activity rises by eight percentage points, without altering boss formations or entry/exit safety. Additional encounters append to the existing slot list to preserve sequential IDs. A 600-floor comparison measured 4,123 -> 4,564 monster slots (+10.7%) and 8,900 -> 16,100 props (including resources). Bats, regional wildlife, and goblin raiders broaden the pools, with a 20% chance to draw from the full thematic pool in role-specialized cavern encounters. Sand caves use scorpions and sand stalkers instead of frost monsters.

`CavernEcologyTest` checks 640 floors for deterministic population, six nodes per floor, asset resolution and route/stair/encounter exclusions, and targets/harvests 30 nodes in actual world caverns. ResourceEconomyTest, DungeonGenerationTest, ModularDungeonAssetsTest and CavernTerrainTest pass. Updated captures show the new population.

The built-in imagegen tool produced a sixteen-cell atlas using the supplied sheets as visual references. Source and exact prompt: `assets/source/regional-caverns/terrain-atlas.png` and `prompt.md`. Runtime assets: `assets/terrain/cavern_{stone,ice,moss,sand}_{floor,gravel,face,roof}.png`. `tools/import_regional_caverns.ps1` reproducibly slices the source without recoloring it. The renderer mirrors ground/roof texture edges in world coordinates and uses the existing terrain chunk cache.

Validation: full Java compilation; DungeonGenerationTest across five seeds; ModularDungeonAssetsTest across 180 floors; RenderCacheTest; CavernTerrainTest across five sites and three zooms (cached/direct/scroll pixel equivalence and unchanged collision tiles). CavernPreview captures the actual game renderer. Review `../../asset-review/regional-caverns/index.html`.
