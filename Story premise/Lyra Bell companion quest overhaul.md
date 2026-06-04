# Lyra Bell Companion Quest Overhaul

## Design Goal

Lyra's questline should feel like practical mercy under pressure, not a healer sending the player on errands. The player should learn that Lyra values supplies before speeches, names before rumors, and systems of care before heroic rescue. Her final Oathstead scene should make home and movement compatible: a clinic can stand somewhere and still reach the road.

Core emotional question:
- Can Lyra forgive herself for surviving impossible triage without becoming careless with the next patient?

Core world question:
- Who turned medicine into harm, and how can Belltower's roads and bells carry care instead of fear?

Primary systems used:
- `QuestStage` internals with the existing `nextQuestId` outer chain.
- `SEARCH`, `GATHER`, `TALK`, `DEFEND`, `DEFEAT`, `DELIVER`, `CHOICE`, `VISIT`.
- Temporary quest NPCs for shivering patients and clinic witnesses.
- Branch outcome keys for later memory, banter, Oathstead, inn, tavern, and romance follow-up.

## Implemented Branch Outcome Keys

- `lyra_first_triage`
  - Decides what the first roadside clinic rule prioritizes: fever, bleeding, fear, or the quiet patient no one sees.

- `lyra_names_on_cot`
  - Chooses what remembered patients need from the living: witness, warning, justice, or care first.

- `lyra_false_medicine`
  - Decides how Lyra responds to sabotaged medicine: warn clinics, hunt the brewer, protect patients, or trace the buyer.

- `lyra_antidote_rule`
  - Defines how the antidote is handled: freely given, tracked, reserved for relapse, or taught to others.

- `lyra_first_patient`
  - Names what Lyra's first lost patient teaches: disobey sooner, record every name, save who can be saved, or never let distance decide care.

- `lyra_moving_care`
  - Chooses the traveling clinic route: worst wounds, forgotten places, warning bells, or places no one else goes.

- `lyra_fever_cure`
  - Decides how the cure travels through the world: bell towers, road clinics, village shelves, or trained hands.

- `lyra_oathstead_clinic`
  - Lets Lyra define Oathstead's clinic as a road clinic, village infirmary, bell-route aid network, or proactive care system.

These outcomes should influence tone and later callbacks rather than hard-locking content. Lyra should remember whether the player prioritizes urgency, patient dignity, shared knowledge, prevention, or care for forgotten places.

## Implemented Internal Stage Sequence

The outer chain remains `lyra_chain_1` through `lyra_chain_8`, but every chapter now has internal stages.

### `lyra_chain_1`: The Roadside Clinic
- Gather Clean Bandages: `GATHER` Clean Bandages.
- Gather Fever Reed: `GATHER` Fever Reed.
- Fill Water Skins: `GATHER` Clean Water Skins.
- Choose The First Triage: `CHOICE` using `lyra_first_triage`.

### `lyra_chain_2`: Names on the Cot
- Read The Patient Ledger: `SEARCH` Patient Ledger.
- Inspect The Empty Shelf: `SEARCH` Empty Medicine Shelf.
- Inspect The Bloodied Cloak: `SEARCH` Bloodied Travel Cloak.
- Choose How Names Are Kept: `CHOICE` using `lyra_names_on_cot`.

### `lyra_chain_3`: Medicine That Lied
- Inspect Altered Clinic Records: `SEARCH` Altered Clinic Records.
- Inspect The Marked Bell Winch: `SEARCH` Marked Bell Winch.
- Question The Shivering Patient: `TALK` with a temporary patient witness.
- Choose The Clinic Response: `CHOICE` using `lyra_false_medicine`.

### `lyra_chain_4`: The Bitter Vial
- Break The False Brewer: `DEFEAT` Goblin Shaman stand-in.
- Recover The Bitter Vial: `SEARCH` Bitter Vial.
- Gather Antidote Roots: `GATHER` Antidote Roots.
- Choose The Antidote Rule: `CHOICE` using `lyra_antidote_rule`.

### `lyra_chain_5`: The Patient She Lost
- Inspect The Old Clinic Cot: `VISIT` Old Clinic Cot.
- Read The Bell Tag: `SEARCH` Old Bell Tag.
- Inspect The Travel Cloak: `SEARCH` Lost Patient's Cloak.
- Choose What Failure Teaches: `CHOICE` using `lyra_first_patient`.

### `lyra_chain_6`: Care That Moves
- Gather Traveling Clinic Supplies: `GATHER` Travel Clinic Supplies.
- Clear The Patient Road: `DEFEND` against Goblin Raider stand-ins.
- Deliver The Field Kit: `DELIVER` Field Clinic Kit to Lyra.
- Choose The Traveling Rule: `CHOICE` using `lyra_moving_care`.

### `lyra_chain_7`: The Fever Nest
- Clear The Poisoned Nest: `DEFEAT` Cave Spiders.
- Gather Poisoned Rope Silk: `GATHER` Poisoned Rope Silk.
- Inspect The Infected Shrine Bell: `SEARCH` Infected Shrine Bell.
- Choose How The Cure Travels: `CHOICE` using `lyra_fever_cure`.

### `lyra_chain_8`: Mercy That Travels
- Inspect Oathstead's Clinic Cot: `VISIT` Oathstead Clinic Cot.
- Stock The Medicine Shelf: `SEARCH` Oathstead Medicine Shelf.
- Choose The Clinic's Promise: `CHOICE` using `lyra_oathstead_clinic`.

## Dialogue Pacing Notes

Early Lyra:
- Practical before tender.
- She should immediately turn concern into useful work.
- Her kindness should feel strict because patients do not survive vague goodwill.

Middle Lyra:
- The patient ledger and false medicine stages should make care feel vulnerable to systems, not just monsters.
- The first lost patient should reveal Lyra's guilt without making her helpless or sentimental.
- Player choices should separate triage, witness, justice, prevention, and shared medical knowledge.

Late Lyra:
- Care That Moves should show Lyra building a method, not only a bag.
- The Fever Nest should transform the threat from individual sickness into a route-borne harm that needs a route-borne cure.
- The final Oathstead scene should frame the settlement as a standing base for traveling mercy.

## Repeatable Post-Quest Dialogue Hooks

Root topic:
- "About the clinic we built..."

Possible lines:
- "The shelf is short on needle oil and patience. We can make more of one."
- "Oathstead is not safe. That is why a clinic belongs here."
- "I used to think stopping meant someone on the road was dying without me. Now I think standing somewhere can send help farther."
- "Your triage rule is still written inside the kit. I pretend not to look at it when I am tired."

## Banter Hooks

Exploration:
- Near roads: Lyra notices whether injured travelers could reach shelter before nightfall.
- Near villages: she checks wells, shelves, and who looks too tired to ask for help.
- Near inns or taverns: she comments on rest as medicine, especially for companions who pretend they do not need it.

Combat:
- After hard fights: "Stand still. Bleeding is not a personality trait."
- Against spiders: "Silk, venom, fever. Wonderful. Nature has discovered paperwork."
- Against shamans or curse users: "Bad medicine is still bad if someone chants over it."

Quest outcome banter:
- Prevention outcomes: "You chose the route before the emergency. That saves people who never learn your name."
- Shared-recipe outcomes: "Teaching the cure means trusting tired hands. Good. Tired hands do most of the work."
- Forgotten-places outcomes: "You remembered the places without bells. I will not forget that."
- Quiet-patient outcomes: "You looked for the one no one heard. That is medicine."

## Next Content Pass

- Add Lyra-specific remembered-outcome lines in personal talks and Oathstead conversations.
- Add inn/tavern quiet scenes where she lets herself rest without calling it laziness.
- Add romance-stage variants where care is mutual rather than the player becoming another patient.
- Consider unique false-medicine brewer and fever nest monster keys later; for now `goblin_shaman`, `goblin`, and `spider` are implementation-safe stand-ins.
