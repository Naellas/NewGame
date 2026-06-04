# Seraphine Vale Companion Quest Overhaul

## Design Goal

Seraphine's questline should feel like a paper trail that becomes a personal reckoning. The player should not simply collect ledgers for her. They should learn why she distrusts promises, why freedom matters to her more than comfort, and how a chosen oath can differ from ownership.

Core emotional question:
- Can Seraphine accept a promise without feeling bought, trapped, or renamed as debt?

Core world question:
- Who kept dead Riverside contracts alive, and how far did the false debt network reach?

Primary systems used:
- `QuestStage` internals with the existing `nextQuestId` outer chain.
- `SEARCH`, `GATHER`, `ASK_AROUND`, `TALK`, `DEFEND`, `DEFEAT`, `CHOICE`, `VISIT`.
- Temporary quest NPCs for clerks and witnesses.
- Branch outcomes for later dialogue, memory, banter, and Oathstead follow-up.

## Implemented Branch Outcome Keys

- `seraphine_first_lie`
  - Quiet exposure, righteous exposure, or theatrical exposure.

- `seraphine_clerk_truth`
  - Expose the clerk, hide the clerk, or make the testimony public enough to protect him.

- `seraphine_family_debt`
  - Frame Seraphine's father as foolish, desperate, brave, or complicated.

- `seraphine_red_notary`
  - Burn records, publish records, or preserve them for victims reclaiming names.

- `seraphine_oathstead_promise`
  - Leave, stay, help, or admit that Oathstead has become a chosen promise.

These outcomes should influence later lines without hard-locking content. Seraphine should remember tone and principle more than mechanical correctness.

## Implemented Internal Stage Sequence

The outer chain remains `seraphine_chain_1` through `seraphine_chain_8`, but every chapter now has internal stages.

### `seraphine_chain_1`: A Signature in Red
- Read The Toll Ledger: `SEARCH` Suspicious Toll Ledger.
- Find The Red Seal: `SEARCH` Wine-Stained Noble Seal.
- Choose The First Lie: `CHOICE` using `seraphine_first_lie`.

### `seraphine_chain_2`: Bridge Toll Lies
- Gather Forged Notes: `GATHER` Forged Toll Notes.
- Inspect Empty Crates: `SEARCH` Empty Supply Crates.
- Ask Bridge Witnesses: `ASK_AROUND` Bridge Toll Witnesses.

### `seraphine_chain_3`: The Clerk Who Vanished
- Search The Clerk's Room: `SEARCH` Missing Clerk's Room.
- Find The Hidden Clerk: `TALK` with a temporary hidden clerk.
- Decide The Clerk's Protection: `CHOICE` using `seraphine_clerk_truth`.

### `seraphine_chain_4`: Knives in the Warehouse
- Break The Warehouse Door: `DEFEND` against warehouse cutthroats.
- Recover Torn Pages: `SEARCH` Torn Contract Pages.
- Take The Black Ledger Key: `SEARCH` Black Ledger Key.

### `seraphine_chain_5`: Vale Was Never Free
- Enter The Vale Counting Room: `SEARCH` Vale Counting Room.
- Read The Protection Contract: `SEARCH` Old Protection Contract.
- Recover The Broken Signet: `SEARCH` Broken Vale Signet.
- Choose What Vale Means: `CHOICE` using `seraphine_family_debt`.

### `seraphine_chain_6`: The Dead Baron's Estate
- Find The Legal Shell: `SEARCH` Dead Baron's Estate.
- Clear The Estate Knives: `DEFEND` against estate cutthroats.
- Open The Contract Cabinet: `SEARCH` Sealed Contract Cabinet.

### `seraphine_chain_7`: The Red Notary
- Enter The Hidden Archive: `SEARCH` Hidden Bridge Archive.
- Break The Red Notary: `DEFEAT` Void Knight stand-in for the Red Notary.
- Open The Blood-Sealed Lockbox: `SEARCH` Blood-Sealed Lockbox.
- Choose What Freedom Costs: `CHOICE` using `seraphine_red_notary`.

### `seraphine_chain_8`: No One Owns Vale
- Walk The Oath Marker: `VISIT` Oathstead Oath Marker.
- Inspect The Burned Contract: `SEARCH` Burned Contract Remains.
- Choose The Promise: `CHOICE` using `seraphine_oathstead_promise`.

## Dialogue Pacing Notes

Early Seraphine:
- Dry, slippery, and allergic to gratitude.
- She treats truth as leverage because she has rarely seen it used as mercy.

Middle Seraphine:
- More personal when Vale appears in the records.
- She should resist pity more sharply than disagreement.
- The player earns trust by respecting her agency, not by promising protection.

Late Seraphine:
- The Red Notary reframes debt as a demonic philosophy: every name owes something.
- Seraphine's victory should not be "no promises." It should be "only promises I choose."

## Repeatable Post-Quest Dialogue Hooks

Root topic:
- "About the promise you chose..."

Possible lines:
- "Freedom is not silence after the chain breaks. It is deciding what sound your own steps make."
- "I still read every promise twice. Habit. But I no longer assume the second reading will betray me."
- "Oathstead has no hidden clause yet. I check regularly, for civic health."
- "No one owns Vale. Some days I believe that cleanly. Some days I say it until the room does."

## Banter Hooks

Exploration:
- Near roads or bridges: Seraphine comments on tolls, ownership, and who profits from delay.
- Near towns: she notices which doors are built to welcome and which are built to count.
- After search objectives: she praises evidence that embarrasses powerful people.

Combat:
- Against bandits: "Hired knives are just contracts with worse handwriting."
- Against demons: "It wants obedience to sound inevitable. I object on stylistic grounds."
- After a hard fight: "We survived. I will be smug once my ribs stop itemizing complaints."

Quest outcome banter:
- Truth/public testimony: "Public truth is dangerous. Useful things often are."
- Mercy/protection: "Mercy is not the same as letting someone escape the invoice."
- Accountability: "Consequences are just signatures nobody expected to read aloud."

## Next Content Pass

- Add Seraphine-specific remembered-outcome dialogue in personal talks.
- Let Oathstead, inns, and taverns unlock a quieter post-quest conversation about chosen promises.
- Add romance-stage variants that frame love as consent renewed, not a bond imposed.
- Consider a unique Red Notary monster key later; for now `void_knight` is used as the implementation-safe stand-in.
