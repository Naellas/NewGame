# Aria Foxglove Companion Quest Overhaul

## Design Goal

Aria's questline should feel like a road story about trust, pride, skill, and the cost of being underestimated. The player should not simply collect scout objects for her. They should learn how Aria survives, why she resists being helped, and whether the player respects her independence or turns care into control.

Core emotional question:
- Can Aria let someone walk beside her without feeling owned, rescued, or made smaller?

Core world question:
- Who has been redirecting Oakhaven roads, and what happened to the scout who disappeared while investigating it?

Primary systems used:
- Staged companion quest helpers.
- `SEARCH`, `GATHER`, `DEFEND`, `ASK_AROUND`, `TALK`, `RESCUE`, `CHOICE`, `VISIT`.
- Quest NPC relocation for missing scouts and witnesses.
- `branchOutcomeKey` for remembered player choices.
- Repeatable post-quest dialogue that reacts to trust, romance, and final branch outcome.

## Branch Outcome Keys

Current implemented keys across Aria's staged chapters:

- `aria_ambush_lesson`
  - truth
  - mercy
  - accountability
  - protect

- `aria_mother_truth`
  - truth
  - mercy
  - accountability
  - protect

- `aria_crossing_truth`
  - truth
  - mercy
  - accountability
  - protect

- `aria_oathstead_future`
  - truth
  - mercy
  - accountability
  - protect

Older design keys that can still inspire later companion-specific choice text:

- `aria_help_style`
  - `respect_independence`
  - `protective`
  - `rushed`

- `aria_missing_scout_truth`
  - `truth_plain`
  - `soften_truth`
  - `withhold_until_ready`

- `aria_false_scouts`
  - `expose`
  - `mercy`
  - `ambush`

- `aria_final_road`
  - `shared_path`
  - `independence`
  - `home`

These outcomes should influence later dialogue, not necessarily lock content. The goal is memory, not punishment.

## Implemented Internal Stage Sequence

The outer `nextQuestId` chain remains `aria_chain_1` through `aria_chain_8`, but each chapter now has internal stages. This gives Aria pacing inside each chapter without exploding the quest log into many separate quests.

### `aria_chain_1`: Road Scout's Warning
- Read The Road: `SEARCH` Roadwatch Warning Marks.
- Ask The Locals: `ASK_AROUND` Oakhaven Road Rumors.
- Lift The Snares: `GATHER` Snare Cord.

### `aria_chain_2`: Missing Road Signs
- Recover The Signs: `GATHER` Broken Road Signs.
- Follow The Scratches: `SEARCH` Scratched Tree Marks.
- Find The Lost Carter: `TALK` with a temporary road NPC.

### `aria_chain_3`: Underestimated on Purpose
- Speak With The Bait: `TALK` with a temporary frightened NPC.
- Break The Ambush: `DEFEND` against false scout pressure.
- Choose The Lesson: `CHOICE` using `aria_ambush_lesson`.

### `aria_chain_4`: The Foxglove Trail
- Read The Foxglove: `SEARCH` Foxglove Trail Markers.
- Open The Scout Cache: `SEARCH` Old Scout Cache.
- Question The Woodcutter: `TALK` with a temporary Oakhaven witness.

### `aria_chain_5`: The Scout Who Did Not Return
- Search The Camp: `SEARCH` Abandoned Scout Camp.
- Recover The Route Map: `SEARCH` Unfinished Route Map.
- Name The Hope: `CHOICE` using `aria_mother_truth`.

### `aria_chain_6`: Bandits in Scout Green
- Separate Rumor From Uniform: `ASK_AROUND` False Scout Rumors.
- Break The False Patrol: `DEFEND` against Bandit Archers.
- Recover The Token: `SEARCH` Route Token.

### `aria_chain_7`: Rootmaw Crossing
- Read The Final Marker: `SEARCH` Final Foxglove Marker.
- Free The Trapped Scout: `RESCUE` a temporary trapped scout.
- Face Rootmaw: `DEFEAT` Rootmaw Stag.
- Choose What The End Means: `CHOICE` using `aria_crossing_truth`.

### `aria_chain_8`: A Road With My Name On It
- Draft Oathstead's Road: `SEARCH` Oathstead Road Map.
- Plant The Foxglove: `VISIT` Foxglove By The Gate.
- Choose The Road Ahead: `CHOICE` using `aria_oathstead_future`.

## Stage 1: Road Scout's Warning

Current title can remain.

Objective:
- `SEARCH`

Quest function:
- `companionStageSearch`

Setup:
- Aria notices the player almost missed roadwatch marks near Oakhaven.
- She frames the task as practical, not personal.
- The player searches snares, warning cuts, and false trail scratches.

Emotional beat:
- Aria is guarded and dry. She tests whether the player listens before acting.

Player branches:
- "I will follow your lead."
  - Outcome: `aria_help_style = respect_independence`
  - Aria approves because the player respects expertise.
- "I will make sure nothing reaches you."
  - Outcome: `aria_help_style = protective`
  - Aria is wary. Care is welcome, ownership is not.
- "Point me at the problem."
  - Outcome: `aria_help_style = rushed`
  - Aria notes impatience.

Sample Aria line:
- "Roads warn people who bother to learn their language. Most deaths begin as missed grammar."

## Stage 2: Missing Road Signs

Objective:
- `GATHER`

Quest function:
- Existing gather pattern or a future `companionStageGather`.

Setup:
- Broken signs are being moved to guide travelers into ambush routes.
- Aria asks the player to recover signs and compare scratch marks.

World relevance:
- Oakhaven trade routes become less safe when signposts are altered.
- This ties Aria's personal scout work to village survival.

Player branches:
- Ask why someone would move signs instead of attacking openly.
- Ask whether Aria has seen this tactic before.
- Challenge whether road signs matter with demons returning.

Aria response:
- "A demon queen burns kingdoms. A moved sign kills one family quietly. I am allowed to care about both."

## Stage 3: Underestimated on Purpose

Objective:
- `DEFEND`

Quest function:
- `companionStageDefend`

Setup:
- False scouts test the road, expecting Aria to be bait or a soft target.
- The player and Aria defend travelers at a marked road bend.

Combat framing:
- `target`: "Oakhaven road bend"
- `monsterKey`: bandit or goblin scout style enemy.
- The objective text should be about protecting travelers, not simply killing enemies.

Branch use:
- If `aria_help_style = respect_independence`, Aria says the player is learning to cover angles without crowding her.
- If `protective`, she says: "You keep stepping between me and the work. Try standing beside me instead."
- If `rushed`, she says: "Fast is useful. Accurate is rarer."

## Stage 4: The Foxglove Trail

Objective:
- `ASK_AROUND` followed by staged lead, or `SEARCH` if kept to one stage.

Quest function:
- `companionStageTalk` or `companionStageSearch`.

Setup:
- Aria recognizes an old foxglove trail mark linked to her mother's route network.
- The player asks Oakhaven locals about old scout routes.

NPC targets:
- Existing Oakhaven NPCs can provide rumor fragments.
- No NPC relocation needed yet.

Meaning:
- Aria's mother was not simply missing. She was following a manipulated trail.

Sample Aria line:
- "My mother used foxglove because it looked delicate. She liked warnings that laughed at people who underestimated them."

## Stage 5: The Scout Who Did Not Return

Objective:
- `TALK` with relocated existing NPC or temporary quest NPC.

Quest function:
- `companionStageTalk`

Quest NPC relocation:
- Move an existing Oakhaven NPC such as Finch, Hedgewise Lin, or a future "Old Scout Tavin" from village to overworld while active.
- `targetNpcId` should identify the NPC.
- `objectiveLocationKind`: farmland or forest-like route marker area.

Setup:
- The original quest giver or witness is absent from town.
- Aria realizes someone who knew her mother's route went looking alone.
- Player finds them injured or hiding on the road.

Important logic:
- The NPC must disappear from Oakhaven while the quest is active.
- When found, dialogue should route to this quest stage.

Player branches:
- Tell Aria immediately what the witness said.
- Soften the truth.
- Ask the witness to speak to Aria directly.

Potential outcome:
- `aria_missing_scout_truth`

## Stage 6: Bandits in Scout Green

Objective:
- `RESCUE`

Quest function:
- `companionStageRescue`

Setup:
- Bandits are wearing scout colors to exploit trust on the roads.
- A witness or young scout is surrounded.
- The player rescues them and learns the false scouts used Aria's mother's route token.

Combat framing:
- `target`: "captured scout witness"
- `targetNpcId`: relocated or temporary witness.
- `monsterKey`: bandit archer or bandit cutthroat.

Branch:
- `aria_false_scouts`
  - `expose`: publish what the false scouts did.
  - `mercy`: spare one frightened recruit who was coerced.
  - `ambush`: use the false route to trap the leaders.

Aria reaction examples:
- Expose: "Good. Let every road know their names."
- Mercy: "I hate that you may be right. I hate more that I wanted not to be."
- Ambush: "That is my kind of ugly. Useful, if we do not start liking it."

## Stage 7: Rootmaw Crossing

Objective:
- `CHOICE` plus optional boss fight or `RESCUE`/`DEFEND` if split into two stages.

Quest function:
- `companionStageChoice`

Setup:
- The final foxglove marker leads to Rootmaw Crossing.
- Aria learns her mother survived longer than believed, but chose to stay behind to keep a corrupted guardian away from the road.
- The player helps Aria decide how to carry that truth.

Choice outcome:
- `aria_missing_scout_truth`
  - `truth_plain`: "She chose the road and paid for it."
  - `soften_truth`: "She saved people, and the details can wait."
  - `withhold_until_ready`: "Aria deserves to ask for the truth when she can stand inside it."

Aria should not be treated as fragile. Even if the player softens the truth, she should notice.

Sample Aria line:
- "Do not wrap the truth in wool and call it kindness. If it cuts, hand it to me hilt first."

## Stage 8: A Road With My Name On It

Objective:
- `VISIT` at Oathstead or `ESCORT`/`VISIT` along the road to Oathstead.

Quest function:
- `companionStageOathstead`

Setup:
- Aria plants foxglove near Oathstead's gate or road marker.
- She chooses what kind of scout she wants to be now.

Final branch:
- `aria_final_road`
  - `shared_path`: Aria accepts that walking beside someone is not the same as surrendering.
  - `independence`: Aria defines the road as hers, and the player respects that.
  - `home`: Aria admits Oathstead can be a place she returns to.

Trust variants:
- Low trust: Aria thanks the player with restraint.
- Medium trust: Aria admits the player listened better than expected.
- High trust: Aria says the player became part of the road's answer.
- Romance: Aria frames love as a shared route, not a leash.

Sample final line:
- "I used to think a road with my name on it meant no one could follow. I was wrong. It means I get to choose who does."

## Repeatable Post-Quest Dialogue

Root topic:
- "About the road we chose..."

Lines by `aria_final_road`:

`shared_path`:
- "I still check behind us. Habit. But I no longer hate finding your footprints there."

`independence`:
- "You let me keep my road. That mattered more than another promise to protect me."

`home`:
- "Oathstead is not quiet enough to be safe. Maybe that is why I believe in it."

Lines by earlier truth branch:

`truth_plain`:
- "You handed me the truth clean. I respect that. I did not enjoy it, but respect and comfort rarely arrive together."

`soften_truth`:
- "You softened it. I noticed. I am still deciding whether to thank you or be angry."

`withhold_until_ready`:
- "You waited until I asked. That is a dangerous kind of kindness. The useful kind."

## Banter Hooks

Exploration:
- Forest or farmland: Aria comments on bad trail logic, moved signs, old foxglove marks.
- Road near Oathstead: Aria comments on whether the road feels like return or duty.
- After finding hidden objectives: Aria praises observation over speed.

Combat:
- Against bandits: "Scout colors on cowards. I am taking that personally."
- Against beasts: "Do not chase the first lunge. Watch the second."
- After a hard fight: "You kept your feet. I noticed."

Quest outcome banter:
- Truth outcome: "You do not flinch from hard answers. Try not to become proud of that."
- Mercy outcome: "Mercy is harder when the target deserves anger. Annoying, but true."
- Accountability outcome: "Consequences are just tracks. Follow them far enough and someone always claims they meant well."

## Implementation Notes

Implemented first pass:
- `QuestStage` internals are now in code.
- Aria's eight chapter ids and `nextQuestId` chain remain stable.
- Each Aria chapter has 3-4 internal stages except chapter 7, which has 4.
- Temporary quest NPC stages are used for witnesses and vulnerable road NPCs.
- Choice stages use implemented branch outcome keys and can be expanded later with Aria-specific consequence lines.
- Rewards stay close to the original Aria chain to avoid progression disruption.

Next content pass:
- Add Aria-specific dialogue reactions for each remembered outcome key.
- Add one or two relocated named Oakhaven NPCs once the NPC cast has stable names.
- Add Oathstead-specific objective placement when player-village location patches become available.
