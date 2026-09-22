# Quest Objective Refinement Plan

Status: Design plan with an initial runtime implementation. See [Quest Narrative Implementation](quest-narrative-implementation.md) for implemented quests, save migration, verification, and remaining work. The source-review findings below describe the pre-implementation baseline.

Main-story checkpoint, 22 September 2026: all 26 current campaign quests now have authored premise-led conversations. Watchtower Without Bells, Frosthollow Standard, Shrine Without Shadow, and The Ember Socket Rite also have revised action sequences: distinct findings, post-battle inspection, and an ordered disconnect/release ritual. See [the implementation contracts](main-story-rewrite-pass.md) and [actual runtime dialogue](main-story-dialogue.md). This extends workstream E without declaring the later eastern campaign or new boss designs implemented.

## 1. Intended result

Make every objective describe an action the player can actually perform, at a believable place, for a reason connected to their story. Completion must prove the promised action happened. Dialogue, journal text, world evidence, rewards, and aftermath must agree about that result.

The writing direction is direct language with fantasy in the situation itself: recover the ward fragment to help protect Oathstead; reopen a channel to release a trapped fire-being; inspect the gates blocked by a river serpent. Complexity comes from evidence, preparation, competing needs, and consequences rather than vague wording or extra counters.

Creative references:

- [Narrative Rewrite Brief](../../Story%20premise/narrative-rewrite-brief.md), especially sections 3, 4, and 9.
- [Setting and Story Bible](../../Story%20premise/world-framework.md).
- [Faith, Cults, Camps, and Dungeons](../../Story%20premise/faith-cults-and-dungeons.md).
- [Regional Bestiary and Bosses](../../Story%20premise/regional-bestiary-and-bosses.md).
- [Companion Quest Relevance Rubric](companion-quest-relevance-rubric.md).

This plan refines how those stories become playable objectives. It does not require every simple errand to become a branching investigation or every existing enemy to become a boss.

## 2. Findings from the current implementation

These are source-review findings, not results from a runtime playthrough. Verify each affected route during its implementation batch.

| Priority | Observed implementation | Consequence and required refinement |
| --- | --- | --- |
| P0 | Starting an NPC interaction calls `recordConversationQuestObjectives`. Conversation kinds include `TALK`, `REPORT`, `DELIVER`, and `CHOICE`; `recordConversation` increments their counters without distinguishing the content of a reply. | Opening a matching conversation can stand in for giving a report or making a decision. Require a validated content event for substantive reports, testimony, delivery, and choices. |
| P0 | `conversationObjectiveMatches` accepts broad matching and can fall back to the quest giver. `ASK_AROUND` can accept NPCs other than the giver without a specific witness set. | Speaking to an unrelated person must not establish a named witness's account. Define eligible sources and stable participant identities per objective. |
| P0 | `Quest.record` advances combat objectives by matching a defeated target; `RESCUE` and `DEFEND` are classified as combat objectives. | A matched kill alone cannot prove that a captive is safe or a defended object survived. Add explicit outcome conditions, or describe the narrower action honestly. |
| P0 | `ESCORT` uses inspect-style handling and records arrival at a marked objective. | Do not promise a moving, surviving follower on this basis. Implement the journey requirements or author the objective as reaching and securing a destination. |
| P0 | Saves encode quest progress with `stageIndex`; handled resource keys for staged quests include that numeric index and coordinates/assets. | Inserting stages or moving evidence can reinterpret old progress or permit duplicates. Add content-version migration and stable stage/objective identities before broad restructuring. |
| P1 | `ms_wake_ashes` asks for two inspections using one target, a farmland location, and a scarecrow asset. `ms_road_dust` mentions ash, fragments, and a banner while counting three units of Demon Ash. | Replace proxy counts with distinct evidence and place it at the actual shrine scene. Do not claim a banner was recovered because ash was gathered. |
| P1 | Archive quests refer to city records and a vault but are configured through generic graveyard locations and props. The main-story helper normally targets the overworld. | Bind stages to actual archive and vault spaces. A readable name is insufficient if the player performs the action elsewhere. |
| P1 | `ms_stolen_index` describes recovering pages while its primary objective is defeating eight Bandit Cutthroats. | Recover the pages through a placed cache or a specifically attributed drop. Combat can open access without substituting for recovery. |
| P1 | Vesper's opening uses specific stage names but generic graveyard location parameters for Snowrest evidence. | Preserve the staged conversation improvements while placing distinct discoveries along the actual Snowrest road and grove. |
| P1 | Progress is mainly a single counter for the current stage; marked-object deduplication is tied to resource keys. | Distinct clues, alternative solutions, and repeatable inspections need semantic identities and clear completion rules. A repeated inspection can show information again without adding progress. |

Existing useful foundations include authored stage IDs, stage dialogue, quest branch outcomes, companion memories, runtime objective placement, and save serialization for choices. Reuse these foundations; extend only the semantics needed for the next complete quest segment.

## 3. Authoring contract for every refined objective

Before implementation, complete this compact record. It is a design template, not a proposed file format or requirement for a new scripting language.

| Field | Required content |
| --- | --- |
| Identity | Stable quest, stage, and objective ID; current source entry being replaced. |
| Why now | A concrete event that creates the need. No invented countdown unless a timer actually exists. |
| Player stake | Who is affected and why the player has a reason to act. |
| Player role | Investigator, protector, builder, negotiator, witness, explorer, hunter, or another specific role. |
| Instruction | Verb + specific target + recognizable place; add a short purpose when useful. |
| Starting knowledge | Which facts the player must know; how they can learn them. |
| World binding | Map/site and identifiable object, NPC, encounter, or mechanism; required approach and exit. |
| Completion evidence | The exact event or state that proves the action, including scope and duplicate handling. |
| Knowledge gained | Observed fact, source, and limits; separate it from an NPC's interpretation. |
| Dependencies | Required predecessors, optional preparations, and any alternative completion paths. |
| Choice and failure | Supported outcomes, reversibility, blocked-state explanation, and recovery path. |
| Presentation | Offer, active instruction, partial-progress response, result, and return dialogue. |
| Consequence | Immediate state change plus any visible or conversational aftermath. |
| Persistence | What survives leaving, saving, returning, and upgrading from old content. |

Every required action must be possible without a particular optional companion, romance, class, or rare consumable. Companions can supply insight, assistance, and personal stakes; provide another source for essential information.

### Example objective card

```text
Proposed objective ID: shrine.ward_fragment
Current quest anchor: ms_road_dust
Why now: The shrine failed during Vaelthara's attack.
Player stake: Oathstead may use the same protection.
Instruction: Recover the carved ward fragment from the burned road shrine.
World binding: Authored shrine rubble, with an accessible interaction point.
Completion: That specific fragment is secured once; unrelated ash does not count.
Observation: Marks remain readable on the inner face.
Unknown: Why the protection failed; whether Oathstead's ward can be repaired.
Next action: Show the fragment to Selene in Archive City.
Disclosure: Selene learns about it only during the inspection conversation.
Persistence: The fragment and recorded recovery survive save/load.
```

A key item can remain in inventory or become a recorded quest possession. Decide explicitly; a progress counter is not a substitute for either. Do not let selling or discarding the only fragment silently block the campaign.

## 4. Objective semantics to establish first

| Objective family | Completion must mean | Useful application |
| --- | --- | --- |
| Inspect/search | The identified source was inspected and its specific result recorded. | Observe inward-burning flame; inspect the broken sluice chain. |
| Recover/gather | The required unique object or legitimate quantity was acquired. | Recover a particular page; collect enough timber for a defined repair. |
| Deliver | The correct recipient accepted the required possession or quantity, with any consumption applied once. | Hand recovered evidence to an archivist; supply a clinic. |
| Talk/ask/report | The eligible exchange or disclosure occurred, not merely a greeting. | Ask a survivor about the mine diversion; report the discovered exit. |
| Choose | The player explicitly committed to a valid option and its outcome was recorded once. | Choose a thaw plan after learning its consequences. |
| Defeat | The relevant encounter target was defeated under the required scope. | Stop the named gate guards, not unrelated enemies elsewhere. |
| Rescue | A specified person was freed and reached the authored safe state. | Release a captive and secure their exit; track whether further travel is needed. |
| Defend | The threat ended and the protected person, site, or object met survival conditions. | Keep a rescue route open through an assault. |
| Escort | The named traveler completed a supported journey or traversal sequence. | Cross the opened path together; arrival by the player alone does not count. |
| Repair/operate/perform rite | A validated interaction changed a mechanism or ritual state after its requirements were met. | Restore cooling and test flow; vent the binding through a demonstrated sequence. |
| Resolve threat | One of a defined set of actual encounter outcomes occurred. | Kill Rootmaw or complete supported restoration. |

The last two rows describe design semantics, not currently existing enum values. Prefer explicit interaction events and outcome checks over adding an enum name without behavior. Existing `SEARCH` or `CHOICE` stages can host a small interaction when they validate its actual effects; they cannot stand in for work that never happens.

Implement an event-driven distinction between meeting an NPC and completing an exchange. A valid dialogue event identifies its quest/objective, speaker, content, and any required disclosed evidence. A choice handler checks acceptance, active stage, allowed option, and current prerequisites before applying it. Old menus must not execute stale actions after the world changes.

Deliveries validate inventory and recipient atomically with recording completion. Rescue, defense, and threat resolution use explicit outcomes. Keep generic kill-count jobs where a bounty really asks for a count; do not silently change all combat progression globally.

## 5. Refine the quest graph and world placement together

Start with an inventory of main quests, companion chains, and recurring side quests. For each stage, record current ID, objective kind, target, count, map/location parameters, actual placement, prerequisites, outcome key, reward, next quest, and related dialogue. Add its target narrative role and classify it:

- **Keep:** already clear and supported.
- **Rewrite:** wording or local context needs improvement while behavior remains valid.
- **Rebind:** named place or object does not match the actual world target.
- **Split:** one count stands in for materially different actions or discoveries.
- **Combine:** repeated errands add no new information or decision.
- **Implement:** the promised action or alternative resolution needs new behavior.
- **Retire:** the stage has no necessary role in the target story; define what replaces its progression function.

Represent required sequences as dependencies. Use distinct facts for parallel clues; require all necessary facts rather than "any three objects." Represent alternatives explicitly so a resolved noncombat path closes a combat objective. Do not make the player perform both alternatives or leave an impossible kill marker after successful restoration.

Avoid forcing a full generic graph engine into the first pass. Existing linear stages plus stable facts and explicit outcome conditions can cover many quests. Introduce a limited required-set or alternative-set representation only when the pilot needs it and add validation for cycles, unreachable stages, and missing exits.

Bind story-critical objectives to stable named anchors within authored sites. Coordinates may be resolved from those anchors, but they are not the object's identity. Validate reachability from the entry and availability under relevant world states. Procedural placement remains useful for repeatable gathering and minor work; it must not move the Archive's only record into a random graveyard.

Define early discovery deliberately. A persistent clue found before accepting the quest can satisfy later knowledge requirements; delivering it, making a promise, or earning a reward still requires its own valid event. A player should not need to inspect the same unique fact again merely because they learned it before the offer.

## 6. First implementation batch: shrine to Archive

This batch proves the full chain from instruction to evidence, disclosure, next action, and save/load. Preserve old quest IDs where possible, but treat new stage identities below as proposals.

| Current anchor | Target refinement | Evidence and visible result |
| --- | --- | --- |
| `ms_wake_ashes` | Inspect the burned shrine, then examine the ward socket as distinct sources. | Record destruction and a broken ward separately. Maelis acknowledges what the player actually observed. |
| `ms_road_dust` | Secure the readable fragment; collect ash as supporting evidence. Keep the banner only if its ownership matters later. | Different sources yield different facts. Do not claim to have proved Vaelthara's entire strategy from a scorch mark. |
| `ms_oathstead_stand` | Secure a specific threatened supply approach, then confirm supplies reached the camp. | A local encounter resolves the immediate road threat; a real handover or arrival supports the supply claim. If arrival is not implemented, limit the objective and response to clearing the route. |
| `ms_names_dust` | Show Selene the fragment; compare it with the accessible maintenance record in Archive City. | The comparison establishes the connection to Oathstead and identifies missing instructions. Opening Selene's greeting does not complete the comparison. |
| `ms_stolen_index` | Learn where the stolen index went and recover the page from Crowhook's cache. | Combat, stealth, or negotiation is offered only when supported; the recovered page, not eight unrelated kills, completes recovery. |
| `ms_first_socket` | Reach the actual Old Oath Vault, inspect its blocking ward, use the recovered instructions, and retrieve Memory. | Entry and retrieval happen in the vault. The existing Memory reward is assigned once through the chosen acquisition model. |

### Resolve the ordering in the Selene scene

The rewrite sample introduces the vault early; the current quest graph places the stolen index before the first socket. Keep that sequence for the first batch. Selene can show the player the blocked entrance, then explain that the missing index identifies how to open it. Return with the page before entering. Do not promise immediate access in one conversation while an unrelated prerequisite prevents it.

### Journal and conversation example

**Active objective:** "Show the shrine fragment to Selene in Archive City. She may be able to compare it with Oathstead's ward records."

**Before handover:**

> Selene: You said you brought part of the broken ward. Let me see the carved side.

**After an explicit inspect-fragment reply, with possession validated:**

> Selene: These marks match the protection beneath Oathstead. I need the builders' instructions before I can tell you how to repair it.

**Next objective:** "Examine the maintenance record on Selene's reading table."

The player sees what caused the update. A return greeting later refers to the current investigation, not the original request for the fragment.

**Batch completion:** the player can travel from the opening to the Memory acquisition through correctly placed objectives, with no generic proxies, fabricated discoveries, or duplicated stone rewards. This batch does not wait for eastern maps or a new boss system.

## 7. Companion pilot and wider rewrite

Use **Vesper's first two chapters** as the companion pilot. They already provide staged investigation and have a direct connection between a person, a place, and visible harm.

### Vesper pilot

| Beat | Player action | Fact or consequence |
| --- | --- | --- |
| Hook | Speak to Vesper about the thaw near Snowrest. | Learn why unusual growth might endanger the road. |
| Inspect | Examine the actual root circle and the damaged road edge. | Distinguish healthy growth from black-sap damage. |
| Compare | Inspect the frozen culvert or a second independent sign along the route. | Establish where the contamination travels; do not infer it from a count alone. |
| Discuss | Give Vesper the specific findings and ask about the sealed grove. | Her account becomes more specific and can be challenged with actual evidence. |
| Act | Clear a supported obstruction or perform a supported test on the damaged root. | A road segment or investigation state changes. Do not offer a full thaw choice before that chapter exists. |
| Follow up | Review what the action changed. | Unlock the next grove lead and preserve the observations for later conversations. |

Rewrite `vesper_chain_1` and `vesper_chain_2` as needed, including their stage IDs and migration. Do not enforce the old number of stages if a beat is redundant. Do not convert chapter-one observations into a premature solution to her eight-chapter story.

### Remaining companion passes

| Companion | Objective emphasis | Specific substitution to avoid |
| --- | --- | --- |
| Aria | Compare real and planted trails, find a named witness, reach the sister, honor her actual decision. | Collecting arbitrary ribbons cannot prove the sister's location or consent. |
| Seraphine | Learn the invitation terms, gain supported access, identify a binding, release its actual subjects. | Reading a contract does not itself free everyone named in it. |
| Maera | Observe, reconstruct, verify a route, choose who receives proven knowledge. | Counting map pieces cannot establish that an unseen destination exists or is safe. |
| Cassia | Inspect orders, hear survivors, maintain a rescue route, resolve accountability. | Winning a battle alone cannot prove that a convoy survived. |
| Lyra | Compare cases, trace a source, obtain clean supplies, treat and verify an outcome. | Gathering herbs cannot mean a patient was cured without treatment. |
| Samir | Hear the vessel, test the binding, prepare a safe release, record the being's actual response. | Selecting "free it" cannot complete a release while the binding remains active. |
| Rafiq | Reproduce the reflection discrepancy, protect evidence, resolve the duel and restitution. | Defeating a rival cannot automatically settle the injured family's loss. |
| Calder | Inspect structural failure, obtain usable materials, repair, load-test, and maintain a crossing. | Reaching a bridge marker cannot prove it can carry a wagon. |

Each pass includes contextual NPC dialogue, companion reactions, journal transitions, and one aftermath observation. Limit full branches to decisions that the game can represent and support.

## 8. Apply the same rules to temples, camps, and bosses

### Temple and camp investigation

Use **The Guest Below the Flame** as the next systems-heavy integration batch after the opening and Vesper pilots. Its dependency chain is:

```text
Inspect failing ward -> investigate camp -> obtain evidence of confinement
-> inspect the vessel and learn the actual extraction mechanism
-> prepare a supported remedy -> resolve confrontation -> verify survivors and ward state
```

Evidence can come from Tarek or independent records. Neither a generic `ASK_AROUND` counter nor high trust supplies testimony he has not given. A secured exit must exist before a rescue uses it; accepting a rescue plan does not set the survivor to safe.

Repairing cooling, evacuating people, and obtaining the guest's agreement are separate facts. The voluntary-ward outcome requires all its actual prerequisites; stabilization leaves the release unresolved. A final report explains the selected outcome and any remaining work.

### Beast and boss objectives

| Encounter | Objective chain | Completion boundary |
| --- | --- | --- |
| Rootmaw | Inspect orchard damage -> learn the fragment's role -> prepare restoration if desired -> resolve the guardian -> verify orchard state. | Defeat or supported restoration, then one Roots reward through the existing campaign arc. |
| Hrold | Meet the stranded supply crew -> inspect the mine diversion -> prepare repair or assault -> stop attacks -> verify passage. | A truce requires the promised repair and withdrawal; reducing boss health alone does not establish either. |
| Whitewing | Trace cargo theft -> find the egg -> reach the nest -> complete return and retreat, or defeat the roc. | Acquiring the egg is preparation, not the end of caravan attacks. |
| Eight-Flood Serpent | Inspect sluices -> verify a possible outlet -> prepare the gates -> kill or drive out the serpent -> reach the crossing. | Removing the serpent opens the next objective. Tides is awarded by its later crossing resolution, not duplicated as boss loot. |

These encounters depend on their actual maps, battle actions, and alternative outcomes. Write interim objectives around implemented actions rather than shipping the full design's promises before its mechanics exist. The eastern chapter remains a later campaign migration, not a prerequisite for fixing current objectives.

## 9. Persistence, world state, and compatibility

Before changing stage order, define a content version and a migration table per affected quest. A stable quest ID by itself does not make changed stage semantics compatible.

Persist semantic facts, completed objective IDs, choices, and reward claims independently of display labels and prop coordinates. Retain the current branch-outcome infrastructure where suitable. Keep fact provenance and NPC disclosure separate from aggregate trust or rendered memory sentences.

| Existing save condition | Migration policy |
| --- | --- |
| Quest not accepted | Start the new version normally; retain legitimate independently discovered facts where compatible. |
| Unchanged stage and meaning | Map to the stable stage identity and preserve valid progress. |
| Partial counter replaced by distinct clues | Preserve only facts that old state can substantiate. Restart the affected discovery segment with an explicit journal note; do not invent which clue was found. |
| Choice already recorded | Preserve its meaning or explicitly map it to an equivalent outcome; do not silently change a rescue into a death or a refusal into consent. |
| Quest completed or campaign stone already granted | Preserve completion, inventory, and reward claims. Do not demand replay merely to conform old history to the new script. |
| Objective moved or removed | Resolve the new anchor by identity; retain completion if the action is semantically the same. |

Migrations must be repeat-safe. Keep a backup or preserve the previous save until the upgraded file is successfully written. Validate the earliest reachable unfinished objective after migration.

Later, when Tides moves from the Fenlands to the east, existing ownership counts as ownership. Do not remove the stone or award another. Gate the new introductory content separately where needed, and keep a valid route to the final alliance and Gate. Preserve the eleven-before-Dawn and twelve-at-the-Gate checks unless a separately documented campaign change replaces them.

For new outcomes, use one mutually exclusive result value where appropriate. Record a plan separately from its completion. World changes derive from validated outcomes: an opened gate, safe patient, restored shrine, or released beast must remain consistent after leaving and reloading.

## 10. Delivery order and implementation boundaries

| Batch | Deliverable | Dependencies | Exit condition |
| --- | --- | --- | --- |
| A — Inventory | Current quest graph, objective inventory, mismatch classification, and migration examples. | Source and representative saves. | Every stage in the first two pilots has an identified purpose and binding. |
| B — Completion rules | Explicit conversation/choice events, scoped targets, stable objective identities, and migration support required by the first pilot. | A. | A greeting, unrelated kill, repeated clue, or stale menu cannot complete the wrong action. |
| C — Shrine to Archive | Complete opening investigation, supply-route beat, city evidence, stolen index, and Memory acquisition. | B and authored site placements. | Playable end-to-end with consistent journal and dialogue, including migrated progress. |
| D — Vesper pilot | Refined first two chapters and their return conversations. | B; Snowrest site bindings. | Distinct observations drive later dialogue and the next chapter without false discoveries. |
| E — Existing content passes | Remaining main and companion objectives, minor quests, and required rescue/delivery/repair behavior. | Proven C/D patterns; expand B only as needed. | Each shipped quest segment meets the objective contract and supports existing saves. |
| F — Regional integrations | Guest Below the Flame, Rootmaw alternatives, then northern and southern boss segments. | Actual encounter mechanics and maps. | Preparation, confrontation, and aftermath form complete supported routes. |
| G — Eastern campaign migration | Serpent, crossing, Tides reassignment, expanded alliance, final-gate audit. | Eastern content and migration coverage. | The complete twelve-stone campaign remains finishable in new and supported old saves. |

Ship coherent quest segments. Do not merge a new objective label in one batch while deferring the only valid completion event or required location to a later batch.

### Source touchpoints

- [Quest model](../src/main/java/com/alderfall/game/content/Quest.java): objective semantics, stable IDs, completion and stage progression.
- [GameData](../src/main/java/com/alderfall/game/content/GameData.java): authored content, prerequisites, rewards, and legacy-to-new mappings.
- [GameState](../src/main/java/com/alderfall/game/state/GameState.java): interaction events, eligibility, evidence, dialogue effects, world outcomes, and reward application.
- [DialogueLibrary](../src/main/java/com/alderfall/game/dialogues/DialogueLibrary.java) and companion modules: factual gates, explicit commitments, disclosure, and updated return lines.
- [WorldMap](../src/main/java/com/alderfall/game/map/WorldMap.java): actual sites, anchors, transitions, reachable interaction points, and visible aftermath.
- [SaveSystem](../src/main/java/com/alderfall/game/systems/SaveSystem.java): versioning, semantic state persistence, and migration.
- [Diagnostics](../src/main/java/com/alderfall/game/diagnostics/): focused regression coverage and exported conversation review.

Preserve current prop indexing, bounded placement queries, and pathfinding separation. Build content through existing APIs; this plan does not justify a broad rendering or engine rewrite.

## 11. Verification and completion criteria

### Focused behavioral checks

| Scenario | Expected result |
| --- | --- |
| Open the target NPC's greeting without giving a report. | Report and choice stages remain unfinished. |
| Ask an unrelated resident about a named witness event. | No testimony fact or quest credit is created. |
| Inspect one clue repeatedly. | Information can be reread; distinct-clue progress advances once. |
| Find a persistent clue before accepting its quest. | Later investigation recognizes the legitimate fact without granting premature rewards. |
| Kill the same species away from a named rescue encounter. | The rescue remains unresolved. |
| Choose a plan, then lose a required resource before executing it. | Execution revalidates requirements and explains what is missing. |
| Deliver the wrong item, too few items, or to the wrong NPC. | No delivery completion or unintended consumption. |
| Reach an escort endpoint without the traveler. | No escort completion when an actual escort is promised. |
| Resolve a boss through a supported alternative. | Remaining kill objectives close correctly; encounter and quest rewards apply once. |
| Save after evidence, a choice, or a partial repair; reload. | Facts, commitments, world effects, and current instructions remain consistent. |
| Load a save from before stages were added or moved. | Migration preserves justified progress and produces a reachable next step. |
| Finish a segment without its optional companion. | Required evidence and progression remain available; companion-specific lines do not fire. |
| Revisit completed content. | Correct aftermath appears, with no repeat rewards or resurrected obsolete objectives. |

Use focused tests for the changed completion and migration rules, plus the existing broader diagnostics. Do not create tests that merely assert the presence of rewritten strings.

Run the repository build/smoke and dialogue QA entry points for runtime batches. From the repository root, the current scripts are `Java/scripts/smoke-test.ps1` and `Java/scripts/dialogue-qa.ps1`; the smoke script invokes the build. Ensure native compiler failures are surfaced before trusting a smoke result. The dialogue QA script overwrites the conversation export in `Story premise`, so review that generated change with the content batch.

Structural QA is necessary but cannot judge causal clarity. Read complete exported paths: offer, first discovery, partial return, new evidence, decision, action, and aftermath. Play the corresponding world route to verify that the described objects and consequences are actually there.

### Definition of done for a refined segment

- Every required objective names an understandable action and a real reachable target.
- Every completion condition proves the action the journal and dialogue claim occurred.
- The player knows why the action matters and what the result allows them to do next.
- Distinct discoveries remain distinct; repeated actions cannot fabricate missing evidence.
- Preparations, promises, decisions, and completed outcomes remain separate.
- Alternative solutions and failures have supported consequences and a valid next step.
- New-game and supported migrated-save routes work without losing stones or duplicating rewards.
- NPC knowledge, companion reactions, site state, and journal text agree after save/load.

The first milestone is a reliable, concrete journey from the burned shrine to the Stone of Memory, followed by Vesper's first investigation. That establishes the objective and conversation behavior needed for the larger regional rewrite.
