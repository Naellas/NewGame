# Echoes of Alderfall: Narrative Rewrite Brief

Status: Authored target with an initial runtime implementation. See [Quest Narrative Implementation](../Java/docs/quest-narrative-implementation.md) for current playable coverage and remaining work. Samples below remain writing targets unless the implementation report says otherwise.

Main-story implementation checkpoint, 22 September 2026: the 26 existing main quests now have individual story conversations and reports. The northern chapter demonstrates an oath's ending overwritten on Kharvok's standard; the southern chapter lets the player discover a trapped fire guest, disconnect Ember, and open its outlet. These actions carry the premise into gameplay. [The main-story report](../Java/docs/main-story-rewrite-pass.md) states the exact contracts and limits; [the generated transcript](../Java/docs/main-story-dialogue.md) is the current playable wording. The Vaelthara war-brazier scene below remains a target; the existing Gate confrontation now states her coercive terms but offers no surrender branch.

Creative source: [Setting and Story Bible](world-framework.md). This brief replaces the earlier assumption that new folklore must fit the old explanations unchanged. It supplies new explanations, companion arcs, and sample conversations while preserving useful character identities and technical migration anchors.

Encounter companions: [Faith, Cults, Camps, and Dungeons](faith-cults-and-dungeons.md) and [Regional Bestiary and Bosses](regional-bestiary-and-bosses.md). Section 9 below defines the direct, evidence-aware dialogue style used across these stories.

Implementation follow-through: [Quest Objective Refinement Plan](../Java/docs/quest-objective-refinement-plan.md) defines completion rules, world placement, pilot quest revisions, save migration, and validation for turning this brief into playable objectives.

Player comprehension is an additional acceptance requirement: introduce unfamiliar people, objects, customs, and locations before relying on their names. Use the [fixed location library](location-library.md) and [dialogue clarity review](dialogue-clarity-and-place-review.md). “The page,” “the ancestors,” and “the sites” are not clear references unless the conversation has already identified which document, dead people, and destinations it means. Required destinations must be actual named game places, not an arbitrary camp index disguised by prose.

## 1. Scope and source of truth

Rewrite NPC greetings, local rumors, work conversations, quest offers, progress lines, completion reactions, companion personal conversations, companion quest chains, ambient conversations, travel banter, and regional responses to the main campaign. Update the opening and ending to match the revised premise.

For target fiction, the setting bible takes precedence over old dialogue. For current behavior, the code is the source of truth. A future implementation must reconcile the two explicitly; new text must not claim that an unimplemented action or consequence is available.

The existing [dialogue QA export](companion-dialogue-qa.md) is a historical sample, not current canon or proof that a new rewrite works. The [companion relevance rubric](../Java/docs/companion-quest-relevance-rubric.md) still applies: offers must explain why now, why this player, the cost of inaction, and where to begin.

Keep existing quest, NPC, recruit, item, and map identifiers where their meanings remain compatible. Renaming visible text does not require renaming an identifier. Changes to rewards, prerequisites, world placement, or choice outcomes require corresponding implementation and save compatibility review.

## 2. What changes in the existing story

| Current content | Target rewrite |
| --- | --- |
| Survive Vaelthara and gather stones to return to her. | Survive the breach, build Oathstead, discover how the network works, and gather the means and support to change its future. |
| Five kingdoms supply trust through regional errands. | Mainland signatories, independent Freeholds, and eastern communities contribute different knowledge and make concrete commitments. |
| Regional dialogue often repeats motifs of roads, records, and obligations. | Regional supernatural encounters, professions, beliefs, and personal concerns determine what a speaker talks about. |
| Several companion stories center on falsified records or concealed wrongdoing. | Preserve relevant wrongdoing but vary the dramatic action: rescue, exploration, defense, craft, ritual, pursuit, treatment, and negotiation. |
| Tides is awarded in the Fenlands. | Tides belongs to a new eastern crossing chapter; consolidate the Fenlands around Bells. |
| Calder is principally a bridge repair and responsibility story. | Keep that responsibility, add his Freehold origins and eastern training, and make a failed spirit crossing part of the engineering problem. |
| Existing ending resolves Vaelthara's defeat. | Add local aftermath and a meaningful decision about the network's future. |

Neither local spirits nor every companion antagonist become servants of Vaelthara. Her campaign exploits existing vulnerabilities, but people and other beings retain independent motives.

## 3. NPC writing specification

Runtime follow-through: [Physical places and conversations about people](../Java/docs/location-and-character-pass.md) implements generated campaign sites, paged quest conversations, twenty cast backgrounds and revised weekly requests. [Character introductions](../Java/docs/npc-background-conversations.md) and [local conversations](../Java/docs/local-quest-conversations.md) are generated from the current content.

Start with the incident and the person, not a quest title or an objective checklist. Let the player advance through several short passages before a response menu: introduce the speaker's work, explain what happened and how they learned it, then explain their personal stake and request. Use as many passages as the scene needs. Optional questions deepen or challenge that account instead of standing in for its missing introduction.

A first meeting must not presume friendship or shared history. Give a reason this NPC would ask an unfamiliar traveler for help, and let the player question it. Public background can be volunteered; intimate admissions require a relationship or shared work appropriate to the character. Preserve that distinction on return visits. Between tasks, recall the particular completed action or discovery that caused the next request; never import an unplayed predecessor's findings. Reading, accepting, promising and completing remain separate actions.

Before writing a named NPC, record their home, current community, work, immediate need, belief, private preference, important relationship, and knowledge limits. A displaced northern baker living in Sanctum should have a different outlook from a local temple guard. Do not select a complete personality from region alone.

Each recurring NPC needs these authored situations:

| Situation | Content requirement |
| --- | --- |
| First meeting | Identify the present activity or concern; give the player a reason to engage. |
| Ordinary return | Remember a relevant local fact without retelling the introduction. |
| Work or daily life | Show expertise and an ordinary personal preference. |
| Local belief | Describe a practice or encounter from the speaker's perspective; leave room for doubt. |
| Rumor | Say how the speaker heard it and distinguish hearsay from observation. |
| Quest offer | Establish stakes in short steps, then name a reachable first action. |
| Progress | Respond to the actual objective state and provide useful guidance. |
| Resolution | Recognize the actual outcome and its local effect. |
| Relationship change | Change what the speaker shares or requests, not merely the warmth of adjectives. |
| Campaign change | React only to events the speaker could plausibly know. |

A short-lived ambient resident does not need a full named-character tree. Give ambient lines location and event context, and ensure the listener responds to the speaker's actual subject. A job-based fallback can remain for coverage, but cannot supply all regional identity.

### Regional conversations: target examples

These are original samples, not implemented nodes. They demonstrate everyday voices as well as fantasy.

**North — a net mender at a harbor:**

> Net mender: The ship above the headland has lowered its anchor again. Third night now.
>
> Player: Is it dangerous?
>
> Net mender: Not yet. My father used to leave the shore empty when one stopped. The new harbor master has put a watchfire under it.
>
> Player: What do you need?
>
> Net mender: Someone to ask him why. I need this net finished before the tide turns.

**South — a water engineer and a shrine attendant:**

> Engineer: The west channel is clear. The water still won't pass the shrine.
>
> Attendant: I moved the vessel yesterday. The builders needed the alcove.
>
> Engineer: Then show me where you put it. If that fixes the flow, I can get the gardens watered before noon.

**West — a ferryman:**

> Ferryman: Two banks, two fares. If you see a third bank in the water, stay in the boat.
>
> Player: Have you ever landed there?
>
> Ferryman: Once. Beautiful pears. I came home in the wrong month.
>
> Player: And you still work here?
>
> Ferryman: My mother owns the boat. She's harder to bargain with than anything across the river.

**Center — a baker and an apprentice:**

> Apprentice: The oven's whistling again.
>
> Baker: Then your first loaf is burning.
>
> Apprentice: It whistles when you bake, too.
>
> Baker: Yes. Take the loaf out.

**Fenlands — a boat gardener:**

> Gardener: Hold that rope. The onions are leaving.
>
> Player: Is the current always this strong?
>
> Gardener: Only when the south bell misses its morning call. Today the bellkeeper's getting married. I told them to appoint a substitute.

**East — a sail repairer:**

> Sail repairer: This patch keeps pulling toward a harbor that burned before I was born.
>
> Player: Will you cut it out?
>
> Sail repairer: Perhaps. First I'd like to know who taught the cloth the way home.

Avoid making every line a proverb. Use concrete nouns and verbs, interruptions, humor, short answers, and the occasional direct statement of uncertainty. A scholarly speaker can be elaborate; a tired worker need not perform the setting's themes in every sentence.

## 4. Main-story conversation samples

### Maelis: the opening at Oathstead

> Maelis: Sit down. Your sleeve is burned through. Did anyone else leave the shrine?
>
> Player: Vaelthara let me go.
>
> Maelis: Then she wanted someone to tell us. We can decide what to do with her message after I stop the bleeding.
>
> Player: You believe me?
>
> Maelis: I believe you're hurt. Tell me what you saw, in order.

A later short beat gives the first objective:

> Maelis: Two scouts found the road empty. Take one back to the shrine and show them where the protection broke. Bring back a piece of the stone if it's safe to touch.

The final implementation must either supply the scout or change that line to match the supported objective. The example establishes the intended scene, not a license to promise absent companions.

### Selene: finding a way to defend Oathstead

**Scene:** In Act II, the player brings a fragment from the destroyed road shrine to Archive City. Selene has compared its marks with a surviving maintenance record. The player wants to know whether Oathstead can withstand the same attack. This is a proposed replacement scene; the fragment, record, and following objective must exist in the implemented quest.

> Selene: The marks on your fragment match the wards beneath Oathstead. If Vaelthara broke this shrine, the camp may be vulnerable too.
>
> Player: Can you repair them?
>
> Selene: I can repair the stone. I don't yet know how she broke its protection. We need the instructions the ward builders left behind.
>
> Player: You don't have them?
>
> Selene: Only part of them. This record describes a northern hall, a southern fire vessel, and a crossing beyond the eastern sea. The Archive kept the northern instructions. The others are missing.
>
> Player: Then give me the northern ones. People are still sleeping behind that palisade.
>
> Selene: They're in the Old Oath Vault below us. The ward on its door stopped recognizing the archivists three days ago. I can take you to the entrance. We'll have to find out what's keeping it shut.
>
> Player: And if we get inside?
>
> Selene: We look for the builders' instructions and the Stone of Memory. The stone preserves the old ward agreements. Between them, we may learn how to protect the camp and reach Vaelthara.

**Result:** opens the vault investigation. Selene promises evidence and assistance, not a guaranteed repair. The missing eastern and southern records become relevant because the player needs their knowledge. The seven-delegation carving can appear later as corroborating evidence; its political significance need not carry this first conversation.

**Optional follow-up, after examining the altered carving:**

> Player: Why would someone cut the eastern builders out of the record?
>
> Selene: I don't know. But somebody went to the trouble of removing their names and leaving their work in place. If their descendants still maintain the crossing, they may have the instructions we're missing.

### Vaelthara: surrender the stones or defend the camp

**Scene:** After the player has helped defend Oathstead and recovered several stones, Vaelthara speaks through a captured war brazier. Maelis and the player are present. The threat and offer are about this settlement and this campaign. This proposed contact method requires an actual scene and object; it is not a generic greeting that can fire at any point.

> Vaelthara: Your camp held against Morvane. Now half the road villages are looking to you for protection. Can you feed them when winter comes?
>
> Player: You burned the shrine that protected their road. Don't pretend you're here to help them.
>
> Vaelthara: I am here to offer terms. Bring me the stones. Maelis opens Oathstead's gates to my soldiers. In return, I send grain and place the camp under my wards.
>
> Player: And you command it.
>
> Vaelthara: Yes. No more attacks on my patrols. No more opening sealed places behind my back. You can stay and help your people, or leave them in my care.
>
> Player: Can they leave with me?
>
> Vaelthara: Once they swear themselves to my protection, they stay until I release them. I will not have you raising another army outside my walls.
>
> Maelis: They came here because they had nowhere safe to go. That doesn't make them yours.
>
> Vaelthara: Then keep them alive without me. My next army will not turn back at a wooden gate.

**Player responses must have explicit scope:**

- **Refuse:** "You aren't getting the stones. We'll defend the camp." End the audience and unlock a practical defense discussion with Maelis.
- **Question the offer:** "Your wards failed at the shrine. Why should yours hold here?" Vaelthara answers that she controls the breach and would redirect her protection to the camp. Treat this as her claim until independently demonstrated; asking does not accept her terms.
- **Take time:** "I'm not deciding for everyone here." End without surrender and open discussion with residents. Do not invent an invisible deadline.

Do not offer a working surrender branch until the game can support its consequences. Vaelthara can demand surrender in the story without the dialogue menu falsely promising it as a playable campaign path.

**Result:** the player understands what Vaelthara wants, what she offers, who would pay the price, and why the stones matter to her. Maelis follows with concrete defense needs appropriate to the camp's actual state. The exchange adds pressure to the next campaign step rather than pausing it for an abstract debate.

## 5. Companion rewrite packets

Each numbered sequence below maps to the corresponding existing `<recruit>_chain_1` through `_chain_8` identity. These are eight proposed chapter treatments, not final replacement objective arrays. Preserve an ID only after checking that its saved stages can be migrated safely.

Each companion also needs ordinary conversations, exploration reactions, friendship scenes, optional romance, disagreements, and aftermath. Their quest is one part of their life.

### Aria Foxglove — The Hunt Beyond the Hedge

**Keep:** missing sister, sharp observation, distrust of arranged trails, desire to choose where she belongs.

**Rebuild:** her sister entered the Briar Courts to escape a human pursuer and accepted service she cannot easily leave. A court huntsman plants misleading signs to keep Aria outside its territory. The sister is alive and has made decisions Aria dislikes; this is the target story truth, not an already established outcome.

1. Examine a ribbon tied above tracks that end at an unbroken hedge.
2. Protect a charcoal burner who saw the sister enter a road visible only at dusk.
3. Learn and test a countermark that holds the hidden path open.
4. Divert an otherworldly hunt long enough to reach its resting place.
5. Establish safe return marks with Oathstead's scouts before crossing farther.
6. Learn the sister's terms of service and the protection she obtained from them.
7. Meet her, confront the huntsman, and pursue a release, a limited renegotiation, or her chosen continued service with safeguards.
8. Aria decides how to maintain contact and builds a roadwatch that helps travelers without deciding their destinations.

**Voice:** observant, quick, often brief; more comfortable showing a clue than explaining a feeling. She enjoys improvised games of tracking and can be playfully competitive.

> Aria: That's her knot. But the branch grew through it years ago. She vanished last spring.
>
> Player: Could someone be copying her?
>
> Aria: Yes. Or the hedge has kept her longer than we have. I want to know which before I step through.

**Lasting choice:** her sister's destination changes later letters and conversations. Aria can disagree with the player without the story declaring either sibling an object to recover.

### Seraphine Vale — Guests of the Thorn Hall

**Keep:** court experience, inherited exploitation, guarded intimacy, eventual protection for vulnerable people.

**Rebuild:** the Vale family served as living guarantees in a compact with a Briar Court. A dead baron's steward keeps renewing invitations that prolong their service. The relevant document has magical force, but dinner etiquette, witnesses, and access to the hall matter as much as reading it.

1. Discover a new invitation addressed to a deceased member of her family.
2. Find a former servant who remembers the original host's promise.
3. Enter a court supper and learn how guests become household retainers.
4. Retrieve a stolen guest token from human accomplices on the river.
5. Visit the Vale home and confront her family's participation in choosing substitutes.
6. Meet the dead baron, who has also become trapped in the household he enriched.
7. Defeat or outmaneuver the Red Notary and resolve the binding without quietly naming a replacement victim.
8. Establish an Oathstead refuge with practical limits and a policy for handling supernatural invitations.

**Voice:** socially perceptive, controlled, capable of sincere warmth. She enjoys music, clothes, and hosting; pleasure is not always a disguise.

> Seraphine: Keep the invitation. Give them your name only when I do.
>
> Player: What happens if I answer first?
>
> Seraphine: They'll set a place for you. We need to learn whether they're inviting you to supper or hiring you for the next hundred years.

**Lasting choice:** public exposure and a negotiated release produce different political reactions. The released servants have independent wishes about where to live.

### Maera Quill — An Atlas of Missing Roads

**Keep:** censored research, her mother's work, curiosity, evidence, and a place for scholarship at Oathstead.

**Rebuild:** the lost routes connect genuinely extraordinary places. Eastern navigation and northern sky observation can correct the Archive's account. Maera learns that a written map is one kind of knowledge among several.

1. Find a star position inconsistent with a supposedly complete royal route chart.
2. Restore the chart with materials obtained from a living observatory, not unrelated gathering sites.
3. Follow a moving road and record what remains stable when the landscape changes.
4. Confront her former supervisor about the research ban.
5. Protect witnesses and charts during a theft by agents seeking network access.
6. Recover her mother's annotations, including an acknowledgment of an eastern navigator omitted from the published work.
7. Visit the Isles with local guidance and verify the missing crossing rather than declaring the chart correct on arrival.
8. Open an Oathstead map room and choose how to share routes whose publication can expose vulnerable places.

**Voice:** enthusiastic and specific, capable of losing track of a meal while testing an idea. Her humor comes from observation rather than contempt for everyone else.

> Maera: The island is on the chart. The chart is older than the eruption that made it.
>
> Player: A prediction?
>
> Maera: Perhaps. Or this isn't a map of the year we thought it was. I need another observation.

**Lasting choice:** public routes improve access but require protections; restricted routes preserve safety but invite accusations of repeating the Archive's secrecy. No path requires Maera's recruitment to unlock the main campaign voyage.

### Cassia Flint — Those Outside the Gate

**Keep:** a fatal gate decision, Captain Varran's responsibility, Cassia's own participation, and her struggle with obedience.

**Rebuild:** the dead from the abandoned convoy now return along the Wake Road. Some demand acknowledgment; others have been bound to Kharvok's banner. Cassia must protect the living without treating the dead as a single hostile force.

1. Find a frost-covered shield outside Highwall bearing a survivor's family mark.
2. Escort a vulnerable convoy through the route her old order abandoned.
3. Hear survivors before confronting the returning dead.
4. Recover the actual order and the signal that made Cassia close the gate.
5. Confront Varran before a local assembly with both living and ancestral witnesses.
6. Defend a gate during a new attack while keeping an evacuation route open.
7. Break the banner's hold over the ancestral company; let accountability follow even if Varran dies in the fighting.
8. Establish an Oathstead defense plan with a clear rescue duty and a way for subordinates to challenge an order.

**Voice:** concise in danger, slower with personal matters; enjoys board games and teaching practical skills.

> Cassia: The dead are carrying our old shields. Don't shoot until we know who's moving them.
>
> Player: You recognize one?
>
> Cassia: I recognize six. They were outside when I closed the gate.

**Lasting choice:** punishment, restitution, and public responsibility affect survivor reactions. A confession does not automatically earn forgiveness.

### Vesper Snowroot — The Spring Beneath Winter

**Keep:** Snowrest, black sap, a sealed family grove, the trapped spring, and a living cutting at Oathstead.

**Rebuild:** the grove is a refuge tended jointly by people and a winter-being. Her family froze its living spring to halt a spreading blight. The failing seal now traps healthy growth and spreads the corruption it contained.

1. Inspect a thawed root circle and an impossible flowering beneath fresh snow.
2. Follow black sap from broken roads toward the old grove.
3. Help displaced animals and learn where the poison is spreading.
4. Enter the sealed grove and meet a guardian that still recognizes Vesper's family.
5. Recover the original keeper's account and hear the winter-being's different memory.
6. Clear a cave-root conduit while arranging protection for the village downstream.
7. Separate the blight from the living spring; choose between a slow managed thaw and a faster release with evacuation and material loss.
8. Bring a viable cutting to Oathstead, test its needs, and arrange care that does not depend on Vesper being permanently present.

**Voice:** precise about living things, emotionally guarded about family; takes real delight in unfamiliar plants and animals.

> Vesper: This branch is healthy. The black sap comes from underneath it.
>
> Player: So we leave the tree standing?
>
> Vesper: For now. Cut it down and the roots will still carry poison into the stream. Help me find where they enter the cave.

**Lasting choice:** the village's recovery, the grove's character, and seasonal imagery reflect the chosen thaw. Neither outcome restores everything immediately.

### Samir Dawn — The Guest Inside the Flame

**Keep:** family reliquary, censored hymn, grief for his mother, and faith that can survive questioning.

**Rebuild:** a member of the Unbanked Flame once agreed to shelter with his family. Its vessel was later altered so it could not leave. Samir's mother discovered this and tried to restore the original relationship. The family's public honor conceals that conflict.

1. Hear a second voice from the reliquary after its damaged seal flares.
2. Prepare a safe audience with the vessel using appropriate oil, cooling, and witnesses.
3. Speak to its inhabitant and test a claim about the seal without immediately accepting every demand.
4. Learn how the first keeper's welcome became compulsory service.
5. Recover the missing hymn verse and his mother's instructions.
6. Visit the ash field where her attempted release failed and confront a hostile remnant of the binding.
7. Defeat the false dawn's herald and offer the fire-being a genuine choice to leave or enter a new agreement.
8. Establish an Oathstead light maintained through knowledge and voluntary help, including a practical alternative if the being departs.

**Voice:** sincere, thoughtful, sometimes unexpectedly funny; interested in song and teaching, not permanently solemn.

> Samir: It answered in my mother's voice. Then it asked me to open the lid.
>
> Player: Do you think it's her?
>
> Samir: No. But she taught it a song no one else in the temple knew. I want to hear the rest before we decide what it is.

**Lasting choice:** the being's freedom is genuine; a new relationship cannot be demanded as the reward for releasing it. Samir's faith remains his decision.

### Rafiq Glass — The Man in the Wrong Reflection

**Keep:** public performance, a rigged fatal duel, his sister, the glassmaker, Nadim, and a repaired blade or charm.

**Rebuild:** a stolen reflection copied Rafiq's movements during the duel and struck when he fled. He still concealed what he knew and left others to carry the consequences. The reflection is becoming a person with its own fear of destruction.

1. Notice that a duel poster's enchanted image moves a moment before he does.
2. Help the affected household secure water and protection before asking for testimony.
3. Revisit the dueling circle and reproduce the discrepancy in its mirrorwork.
4. Defend the glassmaker's workshop against people trying to destroy the evidence.
5. Discover how his bargain for his sister's safety enabled the theft of his reflection.
6. Reforge his blade with the glassmaker and agree on a way to distinguish bodies from reflections in the final circle.
7. Face Nadim and the double; choose whether to reintegrate a willing reflection or give it a separate existence under negotiated safeguards.
8. Return to those harmed, make restitution, and stage an Oathstead performance that no longer depends on a false heroic account.

**Voice:** charming and theatrical in public, quieter when he stops managing an audience. He enjoys performance for its own sake.

> Rafiq: Watch the mirror. I haven't drawn my sword.
>
> Player: Your reflection has.
>
> Rafiq: Yes. I'd hoped it had taken up something less dangerous since we parted.

**Lasting choice:** the double's fate changes later scenes, while restitution remains necessary in either outcome.

### Lyra Bell — The Patients Beneath the Water

**Keep:** repeated fever, mislabeled supplies, a patient she lost, traveling care, and an Oathstead clinic.

**Rebuild:** contaminated enchanted bell-rope carries both a physical sickness and the distress of people trapped in a drowned infirmary. A supplier hid contamination, but exposing them alone cannot treat patients or settle the haunting.

1. Compare cases and identify contact with fibers from the same bell-rope shipment.
2. Trace clinic labels and secure uncontaminated supplies.
3. Treat an endangered patient before taking the evidence farther.
4. Confront the supplier and arrange immediate replacement materials regardless of the later legal outcome.
5. Return to the place where Lyra followed a triage order she now questions; distinguish what she knew then from what she knows now.
6. Build a mobile treatment kit and reach a village cut off by flood.
7. Enter the drowned infirmary with bellkeepers, contain the physical source, and release the trapped call for help.
8. Establish an Oathstead clinic with trained helpers, clean supply practices, and a remembrance chosen by surviving families.

**Voice:** attentive, unembellished while working, capable of impatience and humor; has hobbies and wants rest without having to justify it.

> Lyra: The fever fell when we cut the rope from the cot. Don't touch the loose fibers.
>
> Player: A curse?
>
> Lyra: Something in the fibers, at least. Bring clean cloth. We can name it once he's breathing comfortably.

**Lasting choice:** how resources and responsibility are distributed changes care in different settlements. Avoid an arbitrary choice that withholds a readily available cure merely to manufacture drama.

### Calder Reed — A Bridge for Both Shores

**Keep:** skilled repair, a previous collapse, compromised materials, accepted responsibility, and building at Oathstead.

**Rebuild:** Calder learned harbor construction in the Lantern Isles. His failed mainland design used a crossing pattern whose local spirit passage he had not understood. Officials denied proper materials; he approved the structure anyway. Both material weakness and interrupted passage contributed to the disaster.

1. Inspect a span whose physical cracks cannot explain footsteps heard beneath it.
2. Stabilize it with suitable materials before investigating its supernatural load.
3. Test it during ordinary traffic and during the hour when invisible travelers arrive.
4. Trace reused causeway stones to an older crossing and identify displaced markers.
5. Return to the flood bridge and hear the accounts of those who lost people there.
6. Revisit his eastern teacher with local crossing keepers and distinguish design error from official neglect.
7. Build and defend a solution: a shared span with maintained crossing hours, or separate routes requiring more land and material.
8. Train Oathstead builders and provide a maintenance plan whose knowledge survives his departure.

**Voice:** patient when explaining a craft, stubborn about shortcuts, capable of admitting ignorance. Enjoys making small toys and useful gifts.

> Calder: The posts are holding. Something is pushing between them every evening.
>
> Player: Can you build it stronger?
>
> Calder: Yes. But first I want to know whether I put a bridge across someone's road.

**Lasting choice:** the design changes traffic, maintenance, and relations with the crossing's inhabitants. Eastern expertise is necessary, but local people also supply knowledge his teacher cannot.

## 6. Relationships and smaller recruits

For each principal companion, write at least one scene of competence, pleasure, disagreement, vulnerability, and ordinary shared activity. Friendship and optional romance should branch from the person's situation rather than reuse the same declaration with regional nouns substituted.

At low trust, a companion can give precise practical information without revealing private history. At higher trust, they may admit uncertainty, ask for advice, or challenge the player more directly. After a quest, refer to the actual decision and affected people. Do not promise marriage, departure, a new building, or an NPC's survival unless the relevant state exists.

The remaining recruits need an inventory pass. Retain their gameplay roles where useful, establish individual origins and motivations, and rewrite recruitment lines against their actual locations. Do not give every recruit an eight-chapter arc merely to make them fit the setting.

Avoid merging similarly named people accidentally: recruit Rowan and Elder Rowan, or Peddler Nessa and Bellwright Nessa, need identity checks before assigning new biographies.

## 7. Implementation map

These paths were checked against the current workspace. Older planning notes refer to `content/DialogueLibrary.java`; the live dialogue library is now under `dialogues/`.

| Surface | Current location | Required work |
| --- | --- | --- |
| Main and companion quests, NPC lines, recruits, stone rewards | `Java/src/main/java/com/alderfall/game/content/GameData.java` | Rewrite descriptions and stages together; keep objectives, targets, clues, rewards, and consequences consistent. |
| Named conversations and shared dialogue logic | `Java/src/main/java/com/alderfall/game/dialogues/DialogueLibrary.java` | Rewrite branching responses; prevent generic templates from erasing individual voice. |
| Nine companion voice modules | `Java/src/main/java/com/alderfall/game/dialogues/*Dialogue.java` | Update profiles, personal history, stage-specific reactions, memory, friendship, and romance lines. |
| Ambient exchanges | `Java/src/main/java/com/alderfall/game/dialogues/AmbientTownConversationLibrary.java` | Add region, settlement, and known-event context while retaining useful role fallbacks. |
| Opening, story gates, conversation state, travel comments, victory text | `Java/src/main/java/com/alderfall/game/state/GameState.java` | Audit story-bearing strings and conditions; coordinate new eastern progression and actual endings. |
| Kingdoms, sites, settlement inhabitants, and world evidence | `Java/src/main/java/com/alderfall/game/map/WorldMap.java` | Place the authored scenes and add the eastern route; review local names, generated NPCs, and encounter context. |
| Save persistence | `Java/src/main/java/com/alderfall/game/systems/SaveSystem.java` | Migrate changed quest stages and new choices rather than silently reinterpreting old progress. |
| Dialogue export and gameplay diagnostics | `Java/src/main/java/com/alderfall/game/diagnostics/` | Regenerate conversation samples and test the changed story routes and world objectives. |

Before implementation, inventory additional story strings with searches for character names, quest IDs, kingdom names, and old premise phrases. This table identifies major surfaces, not every source of player-facing text.

### Coordinated campaign changes

The current `MAIN_STORY_STONE_REWARDS` gives `stone_tides` for `ms_miredepth_below`, `stone_bells` for `ms_missing_bell_rope`, `stone_oaths` for `ms_camp_defending`, and `stone_dawn` for `ms_kingdoms_answer`.

The new eastern Tides chapter requires a new quest chain and reward source. Rewrite the two Fenland threads as one coherent Bells arc, retaining any side objectives that still serve it. Ensure that each stone has a deliberate acquisition path and that removing the old Tides reward cannot strand an existing save.

The current state logic requires the other eleven stones before the alliance quest completes. Preserve that dependency unless the campaign design explicitly changes it. Add eastern and Freehold participation to the alliance objectives and dialogue together; changing the phrase “five kingdoms” alone is insufficient.

Companion choices described in this brief will require persistent outcome state where it is absent. If a chapter cannot support its target choice yet, keep its implementation pending or author an explicitly linear interim version. Do not show a choice whose outcomes are identical while claiming a lasting consequence.

## 8. Delivery sequence and acceptance

1. Record the existing main-story graph, all nine companion chains, authored NPC roots, and supported outcome states before changing content.
2. Implement one complete opening segment: Maelis, the shrine investigation, Oathstead reactions, quest log, world evidence, and return dialogue.
3. Implement a complete regional segment with one companion, one recurring NPC, an ambient exchange, and a visible resolution. Vesper's existing grove assets make her a practical first candidate; confirm actual asset and objective coverage first.
4. Rewrite remaining regional segments and companions against the same setting. Add eastern maps and the Tides chapter as a complete playable route.
5. Update stone allocation, alliance conditions, the Hollow Throne confrontation, and aftermath together.
6. Audit friendship, romance, post-quest scenes, travel banter, minor recruits, and generated inhabitants for old-setting leftovers.

For each implemented segment, verify:

- Every promised objective exists at a reachable location and uses the correct target.
- The offer, active stages, return conversation, journal, and aftermath agree on what happened.
- Each player reply receives a relevant response rather than a generic acknowledgment.
- The companion has a specific stake and the player a clear role.
- Region-specific supernatural rules are discoverable before the player must rely on them.
- Local residents have voices and concerns beyond explaining mythology.
- Optional companions and romance are not hidden main-story requirements.
- Save/load preserves implemented choices and safely handles affected old progress.
- The twelve stones remain obtainable exactly as intended, and the final gate can be reached from a new game and relevant migrated saves.

Run the repository's applicable build, smoke tests, and dialogue diagnostics for runtime changes. Read the exported conversations as scenes: structural checks cannot establish that a character sounds distinct or that a story makes sense.

This document is ready to guide that implementation. Its new locations, branches, and consequences remain design work until the matching gameplay is built and verified.

## 9. Deeper conversations with a clear subject

Depth comes from people responding to a changing situation, not from making every sentence longer or more cryptic. Use direct speech. Keep the fantasy in the actual subject: a giant blocking the pass, a spirit inside a ward vessel, a serpent beneath the sluices, or the dead approaching a gate.

Before writing a scene, fill in five sentences:

1. The player is here because **[an event they experienced or an objective they accepted]**.
2. This speaker wants **[a specific result]** and fears **[a specific consequence]**.
3. They know **[facts with sources]**, believe **[interpretations]**, and conceal **[something, if relevant]**.
4. The conversation can change **[the plan, available evidence, access, commitment, or relationship]**.
5. After leaving, the player can **[take a named action or make a clear decision]**.

If these cannot be answered, develop the scene's purpose before polishing its lines. An ordinary social scene can aim at understanding a companion or choosing a shared activity; it does not need a quest reward to matter.

### Plain language, substantial content

Prefer "The serpent is blocking the floodgates. Mireford will flood if we can't open them" to "The river remembers what we buried." A poetic line can follow a concrete discovery, but must not replace its explanation.

Do not force the player into an abstract question such as "Does that prove history is false?" when their immediate concern is a threatened settlement. Let them ask "Will that repair our ward?", "Who has the missing page?", or "Can we get there before the pass freezes?"

Keep most speech bubbles to one or two related sentences. A complex conversation can unfold over many short exchanges. Introduce unfamiliar names when they become useful, and let the player ask what a term means without being mocked or lectured.

NPCs answer the question just asked before changing the subject. When they evade, the player can notice and challenge that evasion. A reply to an accusation should not be the same line as a reply to an offer of help.

### Conversation progression

| Beat | Job | Example from the vessel investigation |
| --- | --- | --- |
| Situation | Connect to the player's present evidence. | "You found the locked departure gate. Tarek works on the transport roster." |
| Account | Let the NPC explain what they claim happened. | Tarek says the pilgrims were kept inside because of illness. |
| Challenge | Ask for specifics or present relevant evidence. | The player shows that confinement began before the first recorded illness. |
| Personal stake | Explain why the NPC is resisting or helping. | Tarek admits his sister is still inside and fears retaliation. |
| Options | Establish actions with distinct costs or limitations. | Secure a route, investigate independently, or make an accusation now. |
| Commitment | State exactly what the player agrees to do. | Protect the sister's exit before requesting public testimony. |
| Return | Respond to the action's actual outcome. | A safe sister lets Tarek speak openly; a blocked route produces a revised plan. |

These are authoring beats, not mandatory menu headings. A scene may skip beats it does not need, and returning to a topic should resume its present situation rather than replay the introduction.

### Evidence, belief, and disclosure

Keep three distinct questions in state: what is true in the world, what the player has learned, and what a particular NPC has learned. High trust can encourage disclosure; it cannot create knowledge the NPC lacks. A journal entry reporting a rumor is not proof that the rumor is true.

Required player options should use an observation the player actually made. If the player has not inspected the return channel, show "Where does the cooling water go?" rather than "You blocked the cooling channel." When new evidence contradicts an earlier claim, provide a specific correction or denial; do not silently replace the NPC's account.

Track stable fact and outcome identifiers. Rendered memory prose is useful for presentation, but cannot safely determine whether a witness survived or a promise was fulfilled. The current dialogue context and saved quest outcomes provide a foundation; per-NPC evidence disclosure remains proposed work.

### Branching scene: Tarek at the caravan enclosure

This is the camp from [The Guest Below the Flame](faith-cults-and-dungeons.md). The player has reached the gate. Tarek has seen the player approach; he has not magically learned every clue they found elsewhere.

**Root:**

> Tarek: The prior closed the camp. If you're bringing medicine, leave it here.

**Without evidence:**

> Player: Why can't the pilgrims leave?
>
> Tarek: We're told the sickness spreads. That's what I was told to write on the gate notices.
>
> Player: Who examined them?
>
> Tarek: The vessel attendants. I carry their orders. I don't treat patients.

**With the dated departure register inspected:**

> Player: The register says you stopped departures four days before anyone fell ill.
>
> Tarek: Keep your voice down. That first order wasn't about sickness.
>
> Player: What was it about?
>
> Tarek: They needed people for the night rite. I was told anyone could refuse. Then I watched them take a man back inside when he tried to leave.

The register establishes the timing discrepancy; it does not itself prove extraction. Tarek supplies witnessed evidence of confinement. Only the lower shrine demonstrates how the rite drains participants.

**After the admission, ask why he stayed:**

> Player: You kept writing their orders after that?
>
> Tarek: Yes. My sister was already in the ward hall. I thought if I kept my job, I could get her out. I haven't managed it.

**Choose a practical approach:**

- **Secure the service exit:** "Show me the route the kitchen workers use. We'll get your sister out before you testify." Tarek describes the gate and warns that it is watched. This records a plan, not a completed rescue or guaranteed safety.
- **Investigate without his testimony:** "I won't name you. Tell me where the attendants keep the old orders." He gives a location. It leads to independent evidence and does not require trusting his whole account.
- **Confront him publicly:** "You helped keep them here. The pilgrims need to hear what you just told me." He warns that guards will hear too. A second explicit choice confirms the public accusation before changing camp alert state.
- **Leave the conversation:** "I need to check something first." The player can return with new evidence; leaving does not erase the admission.

**Return after a verified rescue:**

> Player: Your sister is at the guest court. Nima saw her inside.
>
> Tarek: Then I'll speak to Solari. I wrote the notices, and I knew they were false. I want that in the account too.

**Return while the exit is still blocked:**

> Tarek: The guards are still at the service gate. I can't bring her through while they're there.
>
> Player: Then we need another way. Who holds the keys to the supply stair?

Only offer the supply-stair branch if that route exists. If the sister died, Tarek's response names the actual loss and the player's known actions; it must not reuse the rescue line or presume blame for events the player did not cause.

### Companions in conversations

A companion contributes knowledge, stakes, or disagreement; they do not repeat the last NPC line in a different metaphor. Samir can recognize a family rite, while Nima understands a cooling mechanism he does not. Both can be necessary to the interpretation without either being required in the party.

Let the player respond to a companion's objection. After the meeting, a private exchange can distinguish "I agreed with your plan" from "I disliked your threat but understand why you used it." Approval changes must follow the actual action and the companion's stated values. Asking a careful question should not automatically earn moral credit.

### Coherence checks

- Can the reader name the conversation's subject and its connection to the player's current situation?
- Does each claim have a plausible source, and does the NPC distinguish certainty from belief?
- Do pronouns and phrases such as "the old agreement" identify something already introduced?
- Does every branch answer the selected reply and preserve relevant earlier admissions?
- Are a proposed plan, a promise, and a completed action kept separate?
- Do changed circumstances invalidate stale options before their effects execute?
- Can the player leave, save, return, and resume without losing decisive facts or collecting duplicate rewards?
- Do failed persuasion and absent companions leave another supported route through required content?

For a scene with several conditions, review a small state table and representative paths rather than writing every combination as a new speech. Prioritize resolved outcomes first, urgent current events second, active investigations third, and ordinary greetings last. Re-evaluate state after effects; a conversation built before a rescue must not continue offering to rescue the person afterward.
