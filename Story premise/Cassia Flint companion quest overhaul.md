# Cassia Flint Companion Quest Overhaul

## Design Goal

Cassia's questline should feel like a soldier learning that duty without judgment becomes another kind of cowardice. The player should not simply clear roads and prove strength. They should inspect what Highwall chose to remember, recover what command buried, and help Cassia decide what kind of gate she is willing to stand at again.

Core emotional question:
- Can Cassia carry guilt honestly without letting it become her only order?

Core world question:
- Who turned the Highwall gate into a scapegoat story, and what does a true defensive duty look like when civilians are behind the wall?

Primary systems used:
- `QuestStage` internals with the existing `nextQuestId` outer chain.
- `SEARCH`, `GATHER`, `TALK`, `DEFEND`, `DEFEAT`, `CHOICE`, `VISIT`.
- Temporary quest NPCs for Varran and gate witnesses.
- Branch outcome keys for later memory, banter, Oathstead, inn, tavern, and romance follow-up.

## Implemented Branch Outcome Keys

- `cassia_gate_memory`
  - Names Highwall's gate as duty, guilt, cowardice, or an old wound still giving orders.

- `cassia_frost_road`
  - Decides what the road test proves: discipline, mercy, speed, or suspicion about the turned warning marker.

- `cassia_survivor_truth`
  - Chooses what the survivor deserved: apology, record, restitution, or a promise the gate will not close again.

- `cassia_blue_steel_orders`
  - Frames orders as binding, accusing, requiring judgment, or as hiding places for cowards.

- `cassia_varran_truth`
  - Decides how Varran should be answered: exposure, confession, record-first justice, or facing survivors.

- `cassia_old_gate`
  - Defines the standing order at Old Flint Gate when history tries to repeat itself.

- `cassia_ironwall_trial`
  - Chooses what the public record says about Varran, Cassia, Highwall, and the abandoned civilians.

- `cassia_oathstead_duty`
  - Lets Cassia choose her Oathstead role: gate captain, shield trainer, witness, or guard who questions orders.

These outcomes should influence tone and later callbacks rather than hard-locking content. Cassia should remember whether the player values discipline, mercy, public truth, repair, or judgment over obedience.

## Implemented Internal Stage Sequence

The outer chain remains `cassia_chain_1` through `cassia_chain_8`, but every chapter now has internal stages.

### `cassia_chain_1`: The Woman at the Gate
- Inspect The Dented Shield: `SEARCH` Highwall Gate Shield.
- Test The Old Gate Winch: `SEARCH` Old Gate Winch.
- Read The Memorial Plaque: `SEARCH` Highwall Memorial Plaque.
- Name What The Gate Kept: `CHOICE` using `cassia_gate_memory`.

### `cassia_chain_2`: Frost Road Discipline
- Break The Wolf Pack: `DEFEAT` Frost Wolves.
- Push Back The Raiders: `DEFEAT` Orc Raiders.
- Inspect The Road Marker: `SEARCH` Frost Road Marker.
- Choose The Road Lesson: `CHOICE` using `cassia_frost_road`.

### `cassia_chain_3`: The Survivor's Complaint
- Visit The Survivor's House: `VISIT` Survivor's House.
- Inspect The Burned Token: `SEARCH` Burned Family Token.
- Read The Complaint Letter: `SEARCH` Survivor's Complaint.
- Choose What The Survivor Deserved: `CHOICE` using `cassia_survivor_truth`.

### `cassia_chain_4`: Orders in Blue Steel
- Recover Torn Command Orders: `GATHER` Torn Command Orders.
- Inspect The Command Desk: `SEARCH` Old Command Desk.
- Inspect The Rusted Signal Horn: `SEARCH` Rusted Signal Horn.
- Choose What Orders Mean: `CHOICE` using `cassia_blue_steel_orders`.

### `cassia_chain_5`: The Captain Who Lied
- Question Retired Captain Varran: `TALK` with a temporary Varran stand-in.
- Search Varran's Campaign Chest: `SEARCH` Campaign Chest.
- Read The Hidden Order Copy: `SEARCH` Hidden Order Copy.
- Choose How Varran Is Answered: `CHOICE` using `cassia_varran_truth`.

### `cassia_chain_6`: Raiders at Old Flint Gate
- Break The Raiders: `DEFEND` against Orc Raiders.
- Hold Against The Brutes: `DEFEAT` Orc Brutes.
- Inspect The Broken Chain: `SEARCH` Broken Gate Chain.
- Choose The New Gate Order: `CHOICE` using `cassia_old_gate`.

### `cassia_chain_7`: The Ironwall Trial
- Enter The Trial Yard: `VISIT` Highwall Trial Yard.
- Inspect The Shield Rack: `SEARCH` Trial Shield Rack.
- Defeat Varran's Champion: `DEFEAT` Orc Champion stand-in.
- Choose The Public Record: `CHOICE` using `cassia_ironwall_trial`.

### `cassia_chain_8`: Gate Open, Shield Raised
- Inspect Oathstead's Gate: `VISIT` Oathstead Gate.
- Inspect The Repaired Shield: `SEARCH` Cassia's Repaired Shield.
- Choose The Duty: `CHOICE` using `cassia_oathstead_duty`.

## Dialogue Pacing Notes

Early Cassia:
- Blunt, disciplined, and resistant to comfort.
- She should demand facts before sympathy.
- The gate should feel like a place that shaped her, not only a backstory detail.

Middle Cassia:
- The survivor evidence should make clear that Cassia did close the gate and that context does not erase harm.
- The orders should reveal Varran's cowardice without turning Cassia into someone blameless.
- The player choices should let Cassia distinguish guilt, justice, restitution, and revenge.

Late Cassia:
- Old Flint Gate should replay the central wound under active pressure, giving Cassia a new choice instead of a speech.
- The Ironwall Trial should make public truth heavy and specific.
- The final Oathstead scene should make her chosen duty feel practical: a gate that opens when it should, a shield that remembers its dents, and work she judged for herself.

## Repeatable Post-Quest Dialogue Hooks

Root topic:
- "About the gate you chose..."

Possible lines:
- "Oathstead's gate sticks in rain. I prefer honest flaws."
- "The dead do not need me to suffer loudly. They need me to keep the next gate open."
- "Your choice is still in the standing order. I read it when my temper wants simpler work."
- "I used to think obedience made a wall strong. Judgment does. Obedience only makes it quiet."

## Banter Hooks

Exploration:
- Near roads: Cassia judges sight lines, warning markers, and whether travelers have somewhere to retreat.
- Near cities or walls: she comments on gates as promises rather than decorations.
- Near Oathstead: she notices small structural faults and treats repair as care.

Combat:
- Against wolves: "Hold the line. Fear is faster when you turn your back."
- Against raiders: "They expect panic. Deny them the pleasure."
- Against armored enemies: "Armor protects the body. It does not make the argument better."

Quest outcome banter:
- Mercy outcomes: "You remembered the people behind the order. That matters."
- Public-record outcomes: "A clean record is not a kind one. It is better than kind."
- Judgment-over-orders outcomes: "You gave me an order that expects me to think. Dangerous. Correct."
- Restitution outcomes: "Apology without repair is only breath. You knew that."

## Next Content Pass

- Add Cassia-specific remembered-outcome lines in personal talks and Oathstead conversations.
- Add inn/tavern quiet scenes where Cassia talks about sleeping near a gate that opens.
- Add romance-stage variants where closeness grows from trust under pressure, not softening her into someone else.
- Consider unique Highwall soldier and Varran's Champion monster keys later; for now `orc_raider`, `orc`, and `orc_champion` are implementation-safe stand-ins.
