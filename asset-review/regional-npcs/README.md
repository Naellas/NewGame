# Regional NPC pass

Town appearances resolve separately from stable NPC identity. Names, source sprite IDs, relationship keys, recruitment, and existing quest links remain intact.

- 80 regional character looks: male and female farmers, miners, traders, artisans, scholars, guards, healers, and magistrates across temperate, northern, desert, marsh, and highland cultures. Each has a chest-up portrait and four directional world poses.
- 39 additional portraits based on the existing named-character sprites. Dialogue cards use explicit portrait assets; party portrait rendering also prefers them. Full-height companion/story dialogue scenes keep their existing staging.
- Greyharbor uses coastal marsh clothing; other freehold settlements use highland clothing. House interiors inherit their parent settlement's culture. Recruitable/story characters retain established outfits; bakers and hosts retain kitchen outfits.
- Weekly quests use occupational pools and the nearest matching site. Farmers collect farm supplies, miners collect iron/coal samples, traders recover cargo or remove bandits, artisans recover workshop supplies, scholars inspect records, guards clear patrol routes, healers gather herbs, and magistrates investigate evidence. Cooks collect grain orders.
- Miner Dorran in Cairnvale offers **Iron for the Winter Tools**. Magistrate Halven in Briarbridge offers the two-stage **A Borrowed Signet** investigation. Both have authored introductions and backgrounds.
- New material-gathering objectives use accessible ground instead of impassable scenery. Fresh weekly offers use the occupational pools; existing saved quest state is not rewritten by the appearance system.

## Review

- [Portraits and world poses](regional-portrait-review.png)
- [Miner dialogue in game](miner-dorran-dialogue.png)
- [Magistrate dialogue in game](magistrate-halven-dialogue.png)
- [Farmer dialogue in game](farmer-joss-dialogue.png)
- [Desert trader dialogue in game](spice-peddler-rafi-dialogue.png)
- [Northern farmer dialogue in game](goatkeeper-una-dialogue.png)
- [Conversation regression transcript](conversations.md)

## Art provenance and reproduction

Art was generated with the built-in `image_gen` tool. [Exact prompt set](../../Java/assets/source/regional-npcs/prompts.json), source sheets, and the named-character reference sheet are retained in `Java/assets/source/regional-npcs/`.

Runtime files live in `Java/assets/characters/npcs/townsfolk/regional/` and `Java/assets/characters/npcs/townsfolk/portraits/`. `Java/tools/assets/characters/ImportRegionalNpcs.java` extracts the reviewed portraits and directional poses; `Java/tools/assets/characters/NamedNpcPortraits.java` creates the identity reference and imports the named portrait atlas. World poses use the existing runtime animation system, not newly authored walk-cycle atlases.

From `Java/`, compile the game normally and run `RegionalNpcTest`, `NpcQuestStoryTest`, `QuestNarrativeTest`, `TownNpcNavigationTest`, and `SmokeTest` in package `com.alderfall.game`. `NpcVisualReview` regenerates the actual dialogue screenshots. The regional test verifies assets, occupational requests, culture inheritance, and interaction/turn-in for both new quests.

Validation: normal Java build passed; regional audit passed for 202 residents and 808 requests, with both new quests completed; 2,930 NPC conversation checks and 2,052 quest narrative checks passed; town navigation passed 999 simulated steps; smoke test passed. Screenshots were reviewed at 1440x900.
