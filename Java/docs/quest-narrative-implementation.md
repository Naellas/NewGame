# Quest narrative implementation

Implementation checkpoint: 22 September 2026. This records the playable changes made from the [rewrite brief](../../Story%20premise/narrative-rewrite-brief.md) and [objective refinement plan](quest-objective-refinement-plan.md). It is not a claim that the entire setting rewrite is finished.

The second pass implements five companion segments. See [Companion Care, Witnesses, and Deliveries](companion-quest-segments.md) for their actions, migration policy, and verification.

The main-story pass now authors all 26 campaign quests, adds connected questions and evidence-gated follow-ups, replaces the main cast's surrounding conversations, and revises four northern/southern objective sequences. See [Main Story: the Ward Network and Oathstead](main-story-rewrite-pass.md) and the [runtime transcript](main-story-dialogue.md).

The comprehension review introduces a [35-place location library](../../Story%20premise/location-library.md). All 19 outdoor main quests bind to 18 named campaign sites, now built with actual surface layouts and permanent world-map markers. NPCs explain unfamiliar objects and customs, and their directions use the same names as the markers. Crowhook reuses its actual camp entrance; Kharvok's northern chapter takes place at Banner Cairn.

The [location and character pass](location-and-character-pass.md) adds multiple passages before response menus, rewrites the 16 weekly scenarios and Joss's hay request, and supplies first-meeting backgrounds for all nine companions and eleven main-story quest givers. It replaces the shared task-checklist opening with the incident, occupation, personal concern and reason for asking this player. Only recorded observations and completed predecessor quests supply recaps.

The [whole-catalog location audit](quest-location-audit.md) inventories all 134 quests / 355 stages, identifying 84 companion/side quests with 202 remaining generic indexed bindings. The [clarity review](../../Story%20premise/dialogue-clarity-and-place-review.md) records the resulting problems for all nine companion arcs. This does not claim those later arcs have already been rewritten or relocated.

## Implemented content

| Area | Current behavior |
| --- | --- |
| Opening | The player survives the shrine breach, receives shelter at Oathstead, and investigates whether the camp's protection shares the shrine's weakness. The introduction does not assert Vaelthara's private motive. |
| Main story conversations | All 26 existing main quests now have individual openings, stakes, uncertainties, completion reactions, and connected optional exchanges. Regional folklore explains each stone's purpose and connects it to the player's home and the ward network. Newly observed discoveries unlock specific questions. |
| Northern main chapter | Three distinct observations replace the signal survey counter. After defeating Kharvok, the player must read his fallen standard to establish how a seasonal oath became service until release by its commander. |
| Southern main chapter | Inspect the guest cup, altered welcome, and blocked outlet. At the forge, disconnect Ember before opening the captive fire's outlet. Dialogue preserves the difference between discovering confinement, stopping extraction, releasing one guest, and repairing the road wards. |
| Shrine and Archive investigation | `ms_wake_ashes`, `ms_road_dust`, `ms_names_dust`, `ms_stolen_index`, and `ms_first_socket` have distinct evidence stages. Clearing Crowhook guards does not recover the missing page: the player must search its cache. |
| Investigation spaces | The burned shrine is accessible from Oathstead. The Old Oath Vault is accessible from Archive City. Evidence uses reachable local positions. Oathstead's shrine entrance and return connection survive settlement expansion and save loading. |
| Mainland commitments | `ms_kingdoms_answer` requires explicit exchanges with Mirella, Odrick, Selene, Ysra, and Solari. Each states a specific commitment and its limits. Visiting anonymous markers no longer stands in for their consent. |
| All nine companions | Quest conversations separate purpose, known findings, next action, uncertainty, and decisions. Authored choices remain available without persuasion or approval requirements. Asking about evidence gives no relationship points. Earlier recorded findings can be reopened individually. Revised segments retain previously recorded choices without awarding their effects again. |
| Vesper | The first two investigations place their physical evidence on Snowrest's village paths. The elder's testimony comes from Goatkeeper Una, through an explicit exchange; speaking to Vesper does not substitute for Una's account. |
| Side quests | Orren's ledger is recovered from an actual cache. Cal's two skull charms have separate inspections and findings. Wolf and goat requests describe the existing combat work without inventing delivered pelts or repaired bell posts. |

## Runtime rules

- Opening dialogue does not give testimony, make a choice, or complete a report. A matching reply is required.
- Named contacts and temporary quest witnesses are validated. Multi-witness investigations exclude people already interviewed.
- Conversation sessions capture their quest state. If that state changes, the player must review the refreshed choices before an old option can execute. World objective interactions also validate the stable stage ID.
- Main and companion combat objectives with an authored location accept their marked encounter or corresponding dungeon. Named campaign bosses also count in their actual dungeon. An unrelated enemy of the same type cannot satisfy them.
- Required main-story progression uses completed prerequisites and the required stones. Personal approval is no longer an additional gate to essential campaign information.
- Findings are stored as observed stage IDs and shown in conversations and the journal. Collecting a fragment records a quest possession; it is not a sellable inventory item.
- Existing reward handling remains one-time. Reports and decisions retain separate events and saved state.

## Save compatibility

New saves retain the numeric stage index for compatibility and add a stable stage ID, content revision, and observed stage IDs. Compatible legacy stages retain their progress; their handled-object keys are translated to stable stage IDs.

An unfinished quest whose objective structure changed restarts its evidence sequence while remaining accepted. Loading explains which investigations need to be checked again. Completed quests and their rewards remain completed; the migration does not invent detailed observations for old generic counters. The second pass also persists quest-bound supply quantities. Old companion decisions remain available as a single explicit confirmation when the revised evidence sequence reaches its decision stage.

The location pass assigns revision 4 to the 19 relocated outdoor main quests. Older unfinished versions restart at their newly named sites while staying accepted. Completed versions retain rewards and do not acquire invented new field notes. The other seven main quests keep their previous revisions.

## Verification

From `Java/`, compile sources with Java 21 and run `com.alderfall.game.QuestNarrativeTest`. Coverage includes unknown-fact disclosure, stale evidence, encounter attribution, all companion decision stages, five distinct mainland commitments, multiple witnesses, reachable investigation anchors, settlement growth, save/load, legacy migration, and reward replay after loading.

The companion dialogue QA export checks sampled branches at several relationship and quest states. This is a structural check, not a substitute for reviewing every authored conversation.

After the second pass, the Java 21 narrative regression suite passes 2,017 assertions, and the sampled dialogue export reports zero findings. The new segment suite covers the five rewritten chapters end-to-end, including public world interactions and save/load. `FolkloreWorldTest` passed for three seeds in the first pass.

The smoke fixture for Aria's second chapter now accepts the quest through the normal action, invalidating the objective cache correctly. The second-pass `SmokeTest` run stopped earlier at `assertVillageBuildingRoadConnections`: Dunewick reported zero connected building roads out of five. The newer location pass adds campaign landmarks but does not change settlement building-road generation.

An additional run with the system Java 26 runtime crashed in HotSpot's C2 compiler. The final compilation and narrative checks completed successfully with the installed Java 21 toolchain.

Current location/clarity checks with Java 21: main-story dialogue and integration suite 1,461 checks; narrative regression suite 2,037 assertions; companion segment suite 257 checks; location catalog 1,679 checks across three world seeds. The companion dialogue export reports zero sampled structural findings, and `FolkloreWorldTest` passes all three seeds. The latest broader `SmokeTest` still stops at village building-road connectivity, now reporting Snowrest with 6 of 8 building roads connected. The full smoke suite is not green.

## Remaining rewrite work

The implemented passes do not yet add the proposed eastern chapter, move the Stone of Tides, replace bosses with the new folklore designs, or implement the network's final political choice. Existing boss rewards and campaign prerequisites remain the current game's structure. The Gate demand and victory text now fit the revised premise without claiming that defeating Vaelthara has repaired every binding.

The shared conversation rules cover every companion. The second pass individually rewrites Seraphine 2, Aria 2, Lyra 3 and 6, and Rafiq 2. Other later companion stages, personal conversations, ambient lines, and banter still need individual authoring. Remaining legacy rescue, defense, ritual, and repair objectives need their own completion contracts, including Aria's later trapped-scout segment. Main-story destinations are now named, with additional northern and southern action revisions. Bespoke interiors, deeper western/Fenland actions, and named bindings for the remaining companion/side stages are still work to do.
