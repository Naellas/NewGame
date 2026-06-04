# Staged Quest Objective System

## Can This Concept Work?

Yes. The game can support quests like:

1. A quest giver admits a social problem, lie, debt, fear, or hidden motive.
2. The player completes a field objective.
3. The quest hands off to another NPC or location.
4. Dialogue offers a moral choice.
5. The outcome changes the next step, reward source, relationship, or memory.
6. The original situation changes when the player returns.
7. A rescue, confrontation, or reconciliation closes the arc.

The current game already supports simple chained quests through `nextQuestId`. The next step is to treat those chains as a single visible story arc with stage locks, handoff NPCs, dialogue choices, and branching outcomes.

## Current Runtime Foundation

The current code supports:
- Defeat objectives.
- Gather objectives.
- Visit/inspect objectives.
- Quest chains through `nextQuestId`.
- Companion quest chains.
- Stage-aware dialogue branches.
- Quest acceptance and turn-in through dialogue effects.

New runtime gate added:
- A quest that is the `nextQuestId` of another quest now stays hidden until the previous quest is completed.
- This means later stages can safely live on a different NPC in another town without appearing too early.

## Objective Types Implemented / To Author

The runtime now supports these `Quest.ObjectiveKind` values:
- `DEFEAT`: defeat hostile targets.
- `RESCUE`: defeat attackers framed as saving a person.
- `DEFEND`: defeat attackers framed as holding a place.
- `GATHER`: collect marked resources.
- `DELIVER`: complete a handoff through dialogue.
- `VISIT`: inspect marked world points.
- `SEARCH`: inspect marked clues with stronger mystery framing.
- `TALK`: speak to a specific NPC.
- `ASK_AROUND`: speak to multiple NPCs to collect leads.
- `REPORT`: return information through dialogue.
- `ESCORT`: reach marked route waypoints.
- `CHOICE`: resolve a moral/social decision through dialogue.

Authoring note:
- Combat objectives may use `target` for the story-facing person/place and `monsterKey` for the attackers.
- Conversation objectives can use `targetNpcId` for the intended speaker/recipient. They still fall back to matching by `target`, `objectiveAsset`, `monsterKey`, or the quest giver. `ASK_AROUND` can count different NPCs once each.
- `SEARCH` and `ESCORT` reuse marked world points; use distinctive `objectiveAsset` values so the player sees what they are interacting with.
- `branchOutcomeKey` lets multiple stages in one companion arc remember the same choice, such as truth, mercy, protection, accountability, confession, or refusal.

### Hunt
Player must defeat marked or roaming targets.

Useful for:
- Clearing threats.
- Recovering proof from dangerous creatures.
- Rescue setup where enemies surround an NPC.

### Gather
Player collects marked objects in the world.

Useful for:
- Medicine.
- Craft materials.
- Evidence.
- Supplies.
- False-proof quests where the item is not enough and dialogue reveals the truth.

### Inspect
Player visits or examines marked places.

Useful for:
- Tracks.
- Graves.
- Broken signs.
- Camps.
- Ruins.
- Witness locations.

### Deliver
Player brings items or proof to a specific NPC, not necessarily the original giver.

Needed for:
- Multi-town errands.
- Family or faction handoffs.
- “Tell the uncle / lie to the uncle” style choices.

### Talk
Player must speak with a target NPC and resolve a dialogue branch.

Needed for:
- Witness interviews.
- Confessions.
- Persuasion.
- Moral choices.
- Finding missing NPCs through friends.

### Search / Ask Around
Player must talk to one or more NPCs in a settlement to locate a person or lead.

Useful for:
- Missing people.
- Rumors.
- Consequence quests where the original giver has moved.
- Companion quests where trust reveals the real location.

### Rescue
Player finds an NPC in danger, wins a fight, then has an immediate follow-up dialogue.

Useful for:
- Emotional companion moments.
- Consequences of lies or cowardice.
- Turning a fetch quest into a character scene.

### Defend
Player survives waves or protects a place/NPC.

Useful for:
- Oathstead attacks.
- Tavern ambushes.
- Companion loyalty scenes.
- Settlement reputation.

### Choice / Moral Outcome
Player chooses a dialogue outcome that changes the branch.

Examples:
- Tell the truth.
- Lie to protect someone.
- Demand payment.
- Refuse payment.
- Encourage confession.
- Expose the cowardice.
- Give the reward to the injured NPC.

## Stage Structure

Each stage should have:
- Stage id.
- Objective type.
- Objective target.
- Target NPC or location.
- Dialogue at offer.
- Dialogue while active.
- Dialogue when ready.
- Dialogue after completion.
- Optional branch choices.
- Optional relationship/memory effects.
- Optional next stage id.

Suggested stage states:
- Locked.
- Offered.
- Accepted.
- Active.
- Ready to report.
- Completed.
- Branched.
- Failed or resolved differently.

## Example Quest Shape: Not A Brave Hunter

This is inspired by the broad example, but not a direct copy.

### Stage 1: The Borrowed Reputation
An anxious youth asks for help preserving a reputation they did not earn.

Objective:
- Hunt wolves or gather proof from wolf territory.

Dialogue:
- Player can ask why the lie matters.
- NPC admits they wanted family respect.
- Player can accept warmly, coldly, or challenge the lie.

### Stage 2: The Relative In Another Town
The player brings proof to the relative.

Objective:
- Deliver proof / talk to target NPC.

Dialogue choice:
- Tell the truth about the youth.
- Maintain the lie.
- Tell a partial truth: the youth was afraid but trying to change.

Effects:
- Truth: relative respects honesty, youth may feel betrayed.
- Lie: youth is grateful, relative remains deceived.
- Partial truth: best trust outcome if relationship/charisma is high enough.

### Stage 3: The Missing Youth
Returning to the first town, the youth is gone.

Objective:
- Ask around.

NPC friend says the youth went to prove themselves.

### Stage 4: The Real Hunt
Player finds the youth cornered by wolves.

Objective:
- Rescue/defeat enemies.

### Stage 5: The Reckoning
After combat, dialogue starts with the youth.

Choices:
- “You do not have to become the lie.”
- “You nearly died for pride.”
- “Tell your family yourself.”
- “I will keep your secret, but you owe yourself better.”

Outcome:
- Reward can come from the youth, relative, or both.
- Relationship changes depending on honesty and mercy.
- A memory is recorded for companions who witnessed it.

## Companion Story Use

This structure is especially good for companion quests.

Companion stages can become:
- Personal request.
- Field proof.
- Handoff to someone from their past.
- Moral choice where the companion reacts.
- Consequence return.
- Rescue or confrontation.
- Trust milestone scene.

Examples:

### Aria
Theme: staying without being trapped.

Stages:
- Find false trail marks.
- Ask a road contact about her sister.
- Choose whether to reveal a painful truth to Aria immediately.
- Rescue the contact from an ambush.
- Aria decides whether to keep chasing or return to Oathstead.

### Seraphine
Theme: freedom without ownership.

Stages:
- Recover a contract ledger.
- Deliver it to a former client.
- Choose whether to expose names publicly or protect vulnerable signers.
- Face an enforcer.
- Seraphine reacts to how the player handles power.

### Cassia
Theme: duty without obedience.

Stages:
- Recover a broken order seal.
- Question an old officer.
- Choose whether to report the officer or give them a chance to testify.
- Defend a road gate.
- Cassia decides what loyalty means now.

## Implementation Recommendation

Phase 1:
- Use existing `nextQuestId` chains as staged quest arcs.
- Hide later stages until predecessor quest is completed.
- Add better follow-up hints when a stage unlocks another NPC.

Phase 2:
- Implemented: expanded objective kinds for combat, marked-world, and conversation stages.
- Implemented: explicit `targetNpcId` and `branchOutcomeKey` fields on quests.
- Implemented: dialogue choices can record saved quest outcomes through `quest:outcome:<questId>:<outcome>`.
- Next: author companion-specific choice text and branch reactions for later quest stages.

Phase 3:
- Add a `QuestArc` wrapper so the quest log shows one story with multiple completed/active stages.
- Store branch outcomes and companion reactions.
- Let companion banter reference quest choices later.

Phase 4:
- Author 2-3 staged side quests and one companion staged quest using the system.
