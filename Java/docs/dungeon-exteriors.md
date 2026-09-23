# Regional overworld dungeon locations

This pass changes the **overworld surroundings**, entrances, site placement and surface materials. Existing dungeon themes, floor generation, encounters, loot and stair/return mechanics are reused. Vampire lairs and magic towers are surface identities backed by the existing ascending `abandoned_castle` dungeon theme; they do not introduce a separate encounter system.

## Placement rules

- Every playable dungeon entrance is at least **28 tiles** from every other playable dungeon entrance (Euclidean distance). A typical site has a 6-by-5 tile radius, leaving substantial natural ground between neighboring sites.
- New sites search deterministically outward from their regional anchor, staying in that kingdom and requiring their allowed local biome. The search never relaxes spacing or biome requirements. Failure to place a required new site is reported rather than silently stacking locations.
- Existing named sites keep their IDs and registration order. If spacing or land constraints require relocation, the existing resolver searches within the original kingdom. New sites append after the original roster.
- Sites retain settlement clearance, avoid competing location patches, and require a dry building base and landing. A narrow passable approach connects to the existing road network. Entry, stairs, return and escape transitions use the existing mechanics.
- Terrain is sampled around the entrance, ignoring the dungeon marker and roads. Actual terrain controls the exterior palette; a suggestive name does not force snow into southern mountains.

## Playable roster

Coordinates below are regional anchors for new sites, not guaranteed final positions. Seed, terrain, neighboring sites and settlement clearance determine final placement. The world map displays the placed locations.

| Location | Intent | Surface identity / context | Existing floor theme |
| --- | --- | --- | --- |
| Stonegate Crypt | Ancient burial vault | Stone stairs, grave slabs, urns, iron enclosure and braziers; regional northern foothill dressing | crypt |
| Miredepth Cave | Wetland cavern | Root-covered mouth, twisted roots, wet rocks and reeds | cave |
| Frosthollow Cave | Southern mountain cavern | Local rock or sandstone, cairns and expedition supplies; snow only if local terrain is snowy | cave |
| Blackvault Ruins | Broken-oath ruined castle | Connected fortress gate, fallen masonry, statues and ruined courtyard | abandoned_castle |
| Ironbarrow Prison | Abandoned prison | Fortress walls, chains, lanterns and supply debris | prison |
| Belltower Sluice | Floodworks / sewer | Stair access, wet stones, reeds and work lamps | sewer |
| Cairnspire Mine | Mountain mining galleries | Rocky mouth, crates, sacks, lanterns and regional mountain or snow props | cave |
| Greyhook Outpost | Raider stronghold | Palisade, tents, weapons, supplies and campfire | bandit_camp |
| Stormfen Sinkhole | Marsh sinkhole | Overgrown entrance and wetland roots/reeds | cave |
| Redcap Goblin Camp | Goblin encampment | Huts, scrap, totems, cookpots and spike defenses | goblin_camp |
| Crowhook Bandit Camp | Roadside raider camp | Barricades, weapon racks, bedrolls and stolen supplies | bandit_camp |
| **Sunscar Caverns** (117,292) | Desert caverns | Sanctum sand/badlands; sandstone sun-carved entrance, dry rocks and bleached bones | cave |
| **Rimejaw Cavern** (292,42) | Frozen cavern | Northroad snow; icicles, frost crystals, snowy rocks and pines | cave |
| **Briarheart Hollow** (62,188) | Forest cavern | Riverside forest; root-covered entrance, moss rocks, ancient roots and ferns | cave |
| **Roseveil Vampire Lair** (207,127) | Vampire estate | Highwall woodland; red banners, gothic gatehouse, gargoyles, coffins and candelabra | abandoned_castle |
| **Lastwatch Graveyard** (165,79) | Haunted cemetery and crypts | Highwall foothills; iron enclosure, ordered graves, altar, skull markers and burial stone | crypt |
| **Duneshade Bandit Camp** (143,265) | Desert caravan raiders | Sanctum dry terrain; tents, palisades, weapon racks and provisions amid sand and bones | bandit_camp |
| **Thornwall Keep** (90,180) | Ruined frontier castle | Crownlands woodland/rock; heavy gate, rubble, guard statues and regional vegetation | abandoned_castle |
| **Starfall Observatory** (225,112) | Magic tower | Highwall forest/foothills; celestial blue tower, runestones, crystals, books and ritual altar | abandoned_castle |

## Surroundings and artwork

`DungeonExteriorCatalog` owns the new roster, intent, spacing threshold, entrance selection and regional palettes. `OverworldLocationBlueprints.forSite` combines an authored layout with that intent and climate. Camps retain their palisades and activity clusters; graveyards retain their enclosure and burial rows. Ordinary castles no longer inherit vampire banners by default. Arcane and vampire sites have distinct prop sets.

The layered renderer feathers clearings into the surrounding terrain. Snow, sand and marsh retain most of their original material; burial grounds receive muted stone, vampire courtyards a subdued burgundy tint, and arcane courts blue-grey gravel. The old five-tile-wide rectangular road landing is replaced by a narrow entrance route. Core scenery is reserved before placement, keeping tree crowns out of entrance silhouettes. Generic discoverability props no longer add unrelated runestones to playable dungeons.

The user's graveyard, gothic, cave, arcane and fortress sheets supplied the visual direction. Existing runtime props and entrances are reused. Four new transparent entrance sprites were generated with the built-in imagegen tool and copied into `assets/environments/locations/location_exterior_{sand,ice,vampire,arcane}_entrance.png`; these are new interpretations, not direct extractions from the attached sheets. Their original generated alpha is preserved.

## Review and validation

Actual game-renderer captures: [surface gallery](../../asset-review/dungeon-exteriors/index.html). Regenerate from `Java` with `com.alderfall.game.DungeonExteriorPreview`.

Java 21 compilation and these diagnostics cover the change:

- `DungeonExteriorTest`: five seeds; required new sites, all-pairs dungeon spacing, intended kingdom and biome, real PNG alpha, referenced assets, surface access, entry/return transitions and deterministic regeneration.
- `DungeonGenerationTest`: five seeds; floor connectivity, stairs, encounters and context.
- `WorldGenerationTest`: five seeds; destination reachability and crossings.
- `OverworldLocationBlueprintTest`: authored layout integrity and one patch per entrance.
- `LayeredTerrainTest`: zoom, scrolling, cache equivalence and unchanged gameplay during rendering.
