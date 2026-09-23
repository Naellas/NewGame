# Constructed dungeon appearance and population

Crypts, prisons, sewers, bandit outposts and ruined castles now share the cavern pass's continuous terrain rendering and protected population placement. Architecture remains square, with narrow side/rear lips and exposed southern masonry faces. Wall mass is darkened, floors use world-aligned mirrored textures, and sewer stone receives a damp green tint. Collision, routes, stairs and boss formations remain intact.

| Dungeon | Architecture and props | Harvestable contents |
| --- | --- | --- |
| Crypt | Burial niches, ossuaries, urns, grave markers, offerings | Scrap, silver, coal, pale fungi, crystal and regional ore |
| Prison | Reinforced masonry, chains, bedrolls, supplies and lanterns | Scrap, copper, coal, tin, pale fungi and regional ore |
| Bandit outpost | Reinforced masonry, weapon racks, barricades and supplies | Scrap, copper, coal, tin, pale fungi and regional ore |
| Sewer | Damp masonry, roots, wet rocks, crates and lanterns | Scrap, mushrooms, bog iron, copper and coal |
| Ruined castle floors 1–2 | Gothic walls, red banners, coffins, gargoyles and candelabra | Scrap, silver, crystal, coal, copper and regional ore |
| Ruined castle floors 3+ | Arcane violet masonry, rune pillars and crystal clusters | Scrap, silver, crystal, coal, mithril and regional ore |

Six deposits and up to six additional small props are placed at room edges. Existing props, monster slots, stair approaches and critical routes are excluded. Deposits use existing gathering, depletion and profession rules. Previously generated raw cave prop tiles used inside constructed dungeons are replaced with transparent sprites.

Normal room activation increases by eight percentage points. New encounters append after the original slots, preserving existing sequential spawn IDs. Monster pools add appropriate bats, spiders, wildlife and raiders; role-specific encounters have a 20% opportunity to draw from the broader thematic pool. No new named dungeon type is introduced: the arcane style is applied to existing deep castle floors.

## Artwork

The built-in imagegen tool generated sixteen texture masters from the user's five visual references. Source: `assets/source/built-dungeons/terrain-atlas.png`. Exact prompt: `assets/source/built-dungeons/prompt.md`. Runtime assets: `assets/terrain/masonry_{prison,crypt,gothic,arcane}_{floor,gravel,face,roof}.png`. Reimport with `tools/import_built_dungeons.ps1`; this removes atlas gutters and the roof-cell's decorative cap before extraction. Props and harvestable node art reuse existing assets.

## Validation

Full Java 21 compilation passes. BuiltDungeonEcologyTest checks 400 floors and 2,400 deposits, plus targeting and harvesting 36 nodes in actual world dungeons and broader monster selection. DungeonGenerationTest passes five world seeds; ModularDungeonAssetsTest passes 180 floors; CavernEcologyTest passes 640 floors. CavernTerrainTest now covers eleven sites on floors one and three, across three zoom levels, with cached/direct and camera-scroll pixel equivalence and unchanged collision. An intermediate JDK 26 compiler crashed internally; the supported Java 21 toolchain completed successfully.

Actual game renderer captures: `../../asset-review/built-dungeons/index.html`. Regenerate with `com.alderfall.game.BuiltDungeonPreview` from the Java project directory.
