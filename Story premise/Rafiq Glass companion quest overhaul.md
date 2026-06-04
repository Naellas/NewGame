# Rafiq Glass Companion Quest Overhaul

## Design Goal

Rafiq's questline should feel like charm being slowly forced to tell the truth. The player should not simply clear his debt or win a duel for him. They should see how he uses performance to survive shame, why the old duel was rigged, what his debt was really buying, and how Oathstead becomes a place where a second chance is built instead of granted.

Core emotional question:
- Can Rafiq stop making himself entertaining long enough to become trustworthy?

Core world question:
- Who rigged the Glassstep duel, who profited from Rafiq's disgrace, and what did his debt actually protect?

Primary systems used:
- `QuestStage` internals with the existing `nextQuestId` outer chain.
- `SEARCH`, `GATHER`, `DELIVER`, `DEFEND`, `DEFEAT`, `CHOICE`, `VISIT`.
- Branch outcome keys for later memory, banter, Oathstead, inn, tavern, and romance follow-up.

## Implemented Branch Outcome Keys

- `rafiq_first_debt`
  - Names the opening debt as debt, trap, cowardice, or Rafiq running out of jokes.

- `rafiq_water_witnesses`
  - Chooses what Rafiq owes Dunewick next: labor, truth, repayment, or useful silence.

- `rafiq_rigged_duel`
  - Frames his flight as cowardice, refusal, survival, or a mistake inside a trap.

- `rafiq_embermarket_contracts`
  - Decides how the contracts are used: expose the patron, protect witnesses, bait the buyer, or pay debt with truth.

- `rafiq_glass_debt`
  - Names the sister debt as sacrifice, foolishness, love with bad accounting, or a debt no one should have been forced to pay.

- `rafiq_second_chance`
  - Chooses what the repaired blade is for: clearing his name, freeing his sister, facing Nadim, or becoming useful before forgiven.

- `rafiq_glassstep_duel`
  - Decides what the witness stone records: Nadim's guilt, the rigged duel, Rafiq's running, or the full arrangement.

- `rafiq_oathstead_chance`
  - Lets Rafiq choose his Oathstead role: dueling teacher, debt witness, blade at the gate, or a man who stays before he deserves it.

These outcomes should influence tone and later callbacks rather than hard-locking content. Rafiq should remember whether the player values honesty, useful repair, public truth, protected witnesses, or second chances earned by staying.

## Implemented Internal Stage Sequence

The outer chain remains `rafiq_chain_1` through `rafiq_chain_8`, but every chapter now has internal stages.

### `rafiq_chain_1`: The Duelist in Debt
- Inspect The Duel Notice: `SEARCH` Duel Notice.
- Inspect The Cracked Glass Token: `SEARCH` Cracked Glass Token.
- Read The Unpaid Debt Mark: `SEARCH` Unpaid Debt Mark.
- Choose How The Debt Is Named: `CHOICE` using `rafiq_first_debt`.

### `rafiq_chain_2`: Water and Witnesses
- Gather Water Skins: `GATHER` Water Skins.
- Gather Glassstep Herbs: `GATHER` Glassstep Herbs.
- Deliver Supplies To The Elders: `DELIVER` Dunewick supplies.
- Choose The Apology's Shape: `CHOICE` using `rafiq_water_witnesses`.

### `rafiq_chain_3`: The Duel He Ran From
- Visit The Old Dueling Yard: `VISIT` Old Dueling Yard.
- Inspect The Broken Blade Rack: `SEARCH` Broken Blade Rack.
- Read The Blood-Marked Sand: `SEARCH` Blood-Marked Sand.
- Choose What Running Meant: `CHOICE` using `rafiq_rigged_duel`.

### `rafiq_chain_4`: Blades at Embermarket
- Break The Hired Blades: `DEFEAT` Hired Duelists.
- Recover Duel Contracts: `SEARCH` Duel Contracts.
- Find The Purple Sash Token: `SEARCH` Purple Sash Token.
- Choose How To Use The Contracts: `CHOICE` using `rafiq_embermarket_contracts`.

### `rafiq_chain_5`: The Glass Debt
- Visit The Glassmaker's House: `VISIT` Glassmaker's House.
- Inspect The Unpaid Glass Order: `SEARCH` Unpaid Glass Order.
- Inspect The Cracked Mirror: `SEARCH` Cracked Mirror.
- Choose What The Debt Was For: `CHOICE` using `rafiq_glass_debt`.

### `rafiq_chain_6`: Second Chance, Sharp Edge
- Gather Glasssteel Shards: `GATHER` Glasssteel Shards.
- Clear The Badlands Road: `DEFEND` against raiders.
- Repair The Curved Blade: `SEARCH` Repaired Curved Blade.
- Choose The Second Chance: `CHOICE` using `rafiq_second_chance`.

### `rafiq_chain_7`: The Glassstep Duel
- Visit The Glass Shrine: `VISIT` Glass Shrine Dueling Circle.
- Inspect The Circle Of Glass: `SEARCH` Circle Of Glass.
- Defeat Nadim's Champion: `DEFEAT` Bandit Captain stand-in.
- Choose The Witness Stone Record: `CHOICE` using `rafiq_glassstep_duel`.

### `rafiq_chain_8`: Glass Remembers Light
- Inspect The Repaired Blade: `SEARCH` Repaired Curved Blade.
- Place The Glass Charm: `VISIT` Repaired Glass Charm.
- Choose The Second Chance Built Here: `CHOICE` using `rafiq_oathstead_chance`.

## Dialogue Pacing Notes

Early Rafiq:
- Charming, evasive, funny, and clearly using jokes as armor.
- The debt should feel public and humiliating before it becomes sympathetic.
- Player choices should be allowed to call him out without making him collapse into self-pity.

Middle Rafiq:
- The old duel should prove he ran from a trap, but not erase the harm caused after he ran.
- The contracts should reveal that his disgrace is still profitable to someone.
- The sister debt should make his selfish persona crack without turning him into a saint.

Late Rafiq:
- The repaired blade should be earned through useful action, not symbolism alone.
- The final duel should be less about revenge and more about recording truth cleanly.
- The Oathstead scene should let him choose staying as an intention, not a reward he believes he deserves.

## Repeatable Post-Quest Dialogue Hooks

Root topic:
- "About the glass charm..."

Possible lines:
- "It catches the light badly from one angle. I have decided that is moral complexity."
- "I used to think a second chance arrived with applause. Apparently it arrives with chores."
- "Your choice is still in the witness stone. I pretend not to care. I am very convincing."
- "Oathstead has not forgiven me. Good. Forgiveness should not be rushed by a pretty face."

## Banter Hooks

Exploration:
- Near markets: Rafiq notices debt marks, bad contracts, and people performing confidence while cornered.
- Near badlands or glass fields: he comments on reflection, footing, and how sharp beauty can be.
- Near Oathstead: he jokes about the mud before admitting honest ground is useful.

Combat:
- Against bandits: "Paid blades always look surprised when consequences arrive unpaid."
- Against raiders: "Axes near glasssteel. Truly, civilization is fragile."
- After difficult fights: "I looked excellent. You looked useful. A devastating combination."

Quest outcome banter:
- Truth-first outcomes: "You made the truth less flattering and more useful. Annoying. Correct."
- Witness-protection outcomes: "You protected the mouths before asking them to speak. Good instinct."
- Sister-debt outcomes: "You did not make me noble. Thank you. It would have ruined my tailoring."
- Stay-before-deserve outcomes: "You told me to stay before I deserved it. I am still deciding whether that was mercy or strategy."

## Next Content Pass

- Add Rafiq-specific remembered-outcome lines in personal talks and Oathstead conversations.
- Add inn/tavern quiet scenes where he lets the charm drop and talks about debt without performing.
- Add romance-stage variants where flirting remains playful but starts carrying real vulnerability.
- Consider unique Nadim and hired duelist monster keys later; for now `bandit_cutthroat`, `bandit_captain`, and `orc_raider` are implementation-safe stand-ins.
