# Dialogue clarity and place review

The reader is a new player, not an author who has read the setting bible. A technically cautious answer can still be incomprehensible. The story must introduce its people, objects, customs, and destinations before relying on them.

The [location library](location-library.md) is the shared reference for the current main campaign. It is generated from the runtime `StoryLocationCatalog`, which also binds quest destinations and registers persistent map labels. The [campaign transcript](../Java/docs/main-story-dialogue.md) contains the current dialogue rather than aspirational samples.

## Rules for every conversation

1. **Introduce the subject.** Say “the missing leaf titled *Old Oath Vault: Keeper's Instructions*” before calling it “the page.” Explain that it describes the protective seal around the Stone of Memory beneath Archive City.
2. **Identify people by a useful role.** “The families who tend the graves of Highwall's dead roadwatchmen” introduces cairn keepers. Their ceremonial title can follow. A new player should not need a glossary to understand the assignment.
3. **Explain the custom through an action.** State who rings the bell, what two short strokes mean, and who relies on hearing them. Explain the belief about dead watchmen separately from the living soldiers' signaling practice.
4. **Give a named destination and recognizable object.** “Highwall Cairn Watch, southwest of Highwall; inspect the Split Signal Bell” is a direction. “Inspect the sites” is only useful after those sites have been identified.
5. **Make the source available or attributable.** The ransom demand is in the inspected builders' index. Solari can explain the rite he was taught. An unexplained “report” should not magically supply everything the plot needs.
6. **Explain the player's reason to act.** Recovering the instructions lets Selene examine Memory safely, which may help investigate the protection Oathstead shares with the burned shrine. The document is not important merely because it is missing.
7. **Let uncertainty answer a real question.** Admit what is unknown when it matters. Do not replace an explanation with repeated warnings that something is “only a lead” or “not proof.”
8. **Keep discoveries in order.** A player who has disconnected Ember still has a captive spirit to release. A player who has seen the spirit leave should receive an answer about the lost source of heat, not another suggestion that the vessel might be a prison.
9. **Keep one place and object consistent across a chain.** The fire vessel examined at Sunken Guest Shrine is connected to Ember's cradle in the workshop behind that shrine. The later release concerns that same captive spirit, not an unexplained second vessel in another town.
10. **Do not conceal author-only exposition in a player reply.** Before discovering Vaelthara's captivity, the player should not spontaneously ask whether she was used as a waste reservoir. Damar's conversation now discusses Blackvault's observable storage problem.

These supplement, rather than replace, the [rewrite brief's](narrative-rewrite-brief.md) rules for branching, evidence, promises, stale actions, saves, rewards, and required alternative routes.

## Terms a speaker must explain on first use

| Term | Plain introduction | Relevant speaker |
| --- | --- | --- |
| Ward | A protective enchantment maintained through a carved stone, vessel, bell, or another physical object. | Maelis / Selene; every main quest also permits the player to ask. |
| Ward network | Connected local protections whose twelve stones coordinate different functions. A failure may affect more than one settlement. | Selene, after comparing the shrine records and vault. |
| Keeper's instructions | A titled Archive document describing the Old Oath Vault's seal and Memory pedestal. | Selene. |
| Cairn | A stone burial mound; Highwall Cairn Watch stands beside the graves of former roadwatchmen. | Odrick. |
| Cairn keepers | The families maintaining those burial mounds and remembering the dead watchmen. | Odrick. |
| Watch oath | The stated duty of the named watchmen, with an ending at the spring thaw. Kharvok's alteration is a discovery, not an opening assumption. | Odrick, after the inscriptions are read. |
| Fire guest | A fire spirit invited into a vessel to provide warmth and protective power, with permission to leave. | Solari. |
| Transfer cradle | The holder that draws power through Ember while the stone is seated; disconnecting it precedes opening the spirit's exit. | Solari / forge directions. |
| Deep Listeners | Water spirits described in ferry families' stories; the belief does not identify whoever is sounding an unattended bell. | Ysra. |
| Briar Courts | Supernatural households associated with western forest paths, distinct from the human authorities governing river bridges. | Mirella. |
| Rootmaw | The stag that guarded Oakhaven's orchard and now attacks its workers. | Rowan. |
| Hailback Broodmother | A giant mountain spider blocking Snowrest's supply road. | Elric. |
| Ash / spent power | Magical force left after a ward has stopped an attack; Blackvault stored it, and the Ash stone provided a way to release it. | Damar. |

## Changes made during this review

| Conversation / location issue | Implemented correction |
| --- | --- |
| Crowhook's “page” had no clear identity or purpose. | Name the keeper's document, explain the seal it describes, and put the ransom demand inside the index the player actually inspects. |
| Crowhook was selected through a generic camp index that could refer to Greyhook. | Bind its quest to the catalog's Crowhook place and existing Crowhook adventure entrance. |
| “Ancestors,” “keepers,” “same signals,” and “sites” depended on unstated lore. | Introduce dead roadwatchmen, their surviving families, the two-short/three-slow bell code, and three individually named objects at Highwall Cairn Watch. |
| Kharvok's northern story pointed to the geographically southern Frosthollow cave. | Place the main encounter and banner inspection at Banner Cairn north of Highwall. Rename the visible quest to *The Stolen Watch Oath*; retain the old quest ID for saves. Frosthollow remains an existing location for other content. |
| Shrine investigation and fire release appeared to involve unrelated vessels. | Establish Sunken Shrine Forge as the workshop behind the investigated shrine and explain its connection to the same guest cup. |
| Many regional objectives meant an unnamed camp, farm, or graveyard index. | Bind all 19 outdoor main quests to 18 named, persistent campaign sites. Existing cities and shrine/vault interiors cover the remaining main quests. |
| Main NPC ambient branches contradicted or skipped the investigation. | Replace their greetings and personal conversation paths with grounded dialogue; keep unknown discoveries behind observation gates. |
| The coalition assumed the player recognized five names. | Introduce each representative's role, city, and requested contribution in Maelis's offer. |

## Coverage and remaining review

The current runtime catalog contains 35 places: campaign sites and existing supporting settlements. All 26 main quests have a named destination. The 18 outdoor sites now have actual generated surface layouts and permanent world-map icons, labels and descriptions. Existing dungeon entrances are reused; the shrine and forge share a compound. See [the implementation report](../Java/docs/location-and-character-pass.md) for templates, access checks and limits.

The same pass replaces the shared task-card dialogue opening with paced conversations about the incident and speaker. [Nine companion and eleven main-cast backgrounds](../Java/docs/npc-background-conversations.md) explain work, home and the reason to ask an unfamiliar player for help. [Weekly requests and Joss's hay promise](../Java/docs/local-quest-conversations.md) give the smaller encounters a source, personal stake and coherent follow-ups. This improves entry and continuity without claiming every older optional branch or later quest has completed its editorial review.

The [whole-catalog location audit](../Java/docs/quest-location-audit.md) checks all **134 quests and 355 stages**. It finds **84 remaining companion/side quests with 202 indexed location bindings**. This is a mechanical inventory of every binding, distinct from the detailed main-dialogue review. It exposes the same problem beyond the user's examples:

| Story | Existing mismatch | Place decision required before rewriting directions |
| --- | --- | --- |
| Aria | Her sister's trail jumps among unrelated numbered fields; a trapped scout is also a field objective. | A connected western trail with named junctions, a real scout location, and an explicit route from Oakhaven. |
| Calder | A bridge, its workshop, and a flood memorial are represented by fields and a graveyard. | A named river crossing, repair yard, and ruined predecessor crossing with clear relationships. |
| Cassia | Highwall's gate, command desk, retired captain, and trial yard use graveyard/camp indices. | Distinct Highwall gate grounds, command room, captain's residence, and training yard. |
| Lyra | A patient cot, well, supplier, and old clinic are placed at generic fields or a graveyard. | Named care locations in Mireford and an identifiable supplier premises; retain the already explicit Eda/Sen treatment and delivery stages. |
| Maera | Restricted Archive stacks are a goblin-camp encounter; the Quill family study is a graveyard. | Actual Archive rooms, a study with an entrance, and a separately identified map vault. |
| Rafiq | His old dueling yard, a glassmaker's house, and a glass shrine have unrelated indexed sites. | A southern dueling ground, glassmaking household, and shrine whose relationship to the duel is introduced. |
| Samir | The family reliquary and prayer sites repeatedly mean a goblin-camp index. | A named family shrine, lamp workshop, and changed altar; distinguish Samir's family story from Solari's guest-vessel case. |
| Seraphine | A Riverside counting room, warehouse, estate, and hidden archive use fields, camps, and graves. | Identify which building belongs to the Vale family, which stores the contracts, and where the notary can actually be confronted. |
| Vesper | The Snowroot gate, keeper's cave, conduit, and spring use several unrelated graves/camps. | A geographically coherent grove, cave, and spring route; do not claim Frosthollow is nearby without reconciling its actual southern position. |

These are unresolved authoring decisions, not new implemented places. They are recorded here to prevent a cosmetic rename from disguising the problem. The full audit lists individual quest and stage IDs for the subsequent changes.

The main campaign conversation graph and its eleven primary speakers have been reviewed in this pass. The previously revised companion chapters already have named witnesses and explicit handovers, but later companion chapters and the wider side-quest catalog still contain generic site bindings and require individual authoring. Do not treat their old references to Thorn Hall, future eastern crossings, or unnamed rescue locations as proof that those destinations are implemented. Add required places to the runtime catalog before writing instructions that send the player there.

When adding a place, provide its fixed identity, region, actual map or world anchor, route, inhabitants, recognizable objects, and quest bindings. When adding dialogue, read it without the setting bible open: the introductory exchange must supply the knowledge needed to understand the next question.
