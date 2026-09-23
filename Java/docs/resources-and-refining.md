# Resources and refining

Mountain veins spawn on passable foothills and mountain passes. Iron, copper,
coal, and tin are common; silver, gold, cobalt, mithril, adamantite, crystal,
and steel salvage have lower selection weights. Cold mountain edges favor cobalt
and mithril. Coal drops fuel, crystal veins drop crystal dust, and salvage drops
steel scrap rather than a fictitious steel ore.

Regional deposits occupy existing resource-node slots, preserving overall density:

| Biome | Ore | Use after smelting |
| --- | --- | --- |
| Marsh | Bog iron | Iron kite shield |
| Tundra | Froststeel | Frostguard aegis |
| Desert | Sunmetal | Sunward medallion |
| Badlands | Emberite | Emberward shield |
| Dense forest | Verdant | Silver mace |

Marsh nodes have a 35% regional-ore selection chance; tundra, desert, and
badlands nodes have 25%; dense forest nodes have 12%. These are percentages
of resource-node selections, not percentages of all terrain tiles.

All thirteen metal ores can be smelted at an anvil: two ore and one coal produce
one ingot. Copper and tin alloy into bronze; iron and coal produce steel;
steel scrap can also be recycled. Refined alternatives to existing anvil
equipment recipes use ingots. Existing raw-ore recipes and their keys remain
available. Bronze makes a pickaxe; gold makes a sunward medallion. Smiths teach
material recipes, and ingredient discovery also uses the existing recipe system.

Named trees drop their matching timber, including oak, birch, pine, willow,
maple, ash, elder, magical, fruit, and dead trees. Existing enchanted stumps,
glowing roots, and ironwood piles retain their specialized drops. Ancient forest
roots now yield ancient wood, and coastal palms yield palm wood plus fronds
and occasional coconuts. Ancient wood and elder can make an oakheart staff.
Carpenter recipes saw typed timber into general-purpose wood, and carpenters
teach material recipes. Deadwood retains its charcoal recipe.

Additional gatherable materials:

| Biome | Node | Drops | Processing and use |
| --- | --- | --- | --- |
| Badlands | Obsidian outcrop | Obsidian shards | Hone at an anvil; forge Obsidian Fang with steel and cypress |
| Forest | Amber-bearing deposit | Raw amber | Polish at a carpenter; set into a Marshlight Seal |
| Beach / desert | Halite deposit | Rock salt | Refine at a cooking station; preserve fish or meat into rations |
| Marsh beside water | Cypress tree | Cypress wood | Saw into lumber, build net racks, or use for weapon grips |

These use the remaining regional resource slots: obsidian and desert salt 18%,
forest amber 12%, coastal salt 22% near water or 10% on dry beach. Cypress
occupies a 25% selection band on marsh tiles within two tiles of water, subject
to existing tree-spacing rules. Amber-seal crafting requires Crafting 4;
Obsidian Fang requires Crafting 6 and Mining 4.

Artwork is transparent, painted fantasy game art generated with the built-in
image tool. The approved original verdant, emberite, sunmetal, and froststeel
nodes define the style: natural faceted shapes and readable illustrated detail.
All five regional metal veins have dedicated PNGs and distinct raw-ore icons.
All fifteen ingots have bar-shaped icons. Ancient, palm, and cypress timber
have log icons, and the three new nonmetal deposits have dedicated sprites.
Raw nonmetal inventory drops share their corresponding deposit artwork;
processed materials have separate icons.

Runtime PNGs use 96x96 inventory icons, 192x192 deposits, and a 256x256 tree,
with the game's normal smooth scaling. `tools/assets/items/import_resource_art.ps1` exports the
generated alpha PNGs at these dimensions. Final prompts are recorded in
`assets/source/resource-art-prompts.json`; per-asset provenance is under
`assets/source/resources-painted/`. The original five regional deposits are
preserved and resized from their approved sources. Rejected realistic and
coarse pixel drafts are not used by the game.

`ResourceEconomyTest` checks assets, guaranteed primary drops over multiple RNG
seeds, depletion eligibility, refining outputs, ingot uses, targetability, biome
placement, and availability of every ore across three generated worlds.
