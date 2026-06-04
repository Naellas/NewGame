# Maera Quill Companion Quest Overhaul

## Design Goal

Maera's questline should feel like scholarship becoming danger, then danger becoming a chosen public responsibility. The player should not simply collect forbidden papers for a clever archivist. They should learn why Maera distrusts official truth, why her mother's work matters, and why Oathstead becomes the first place where dangerous knowledge can be useful without being owned.

Core emotional question:
- Can Maera stop treating truth as armor long enough to let someone stand beside her?

Core world question:
- Who erased the old star-road from Crown history, and why do cultists and registrars both fear it?

Primary systems used:
- `QuestStage` internals with the existing `nextQuestId` outer chain.
- `SEARCH`, `GATHER`, `TALK`, `DEFEND`, `DEFEAT`, `CHOICE`, `VISIT`.
- Temporary quest NPCs for archive apprentices, registrars, and study witnesses.
- Branch outcome keys for later memory, banter, Oathstead, inn, tavern, and romance follow-up.

## Implemented Branch Outcome Keys

- `maera_first_truth`
  - Decides whether the first contradiction is preserved quietly, cited publicly, or used to bait denial.

- `maera_moving_map`
  - Frames the moving map as royal censorship, demon pressure, old protection, or several ugly truths at once.

- `maera_archive_warning`
  - Chooses how Maera defies the Archive's disciplinary warning.

- `maera_cult_pages`
  - Decides whether the stolen pages should be used to warn the Archive, hide the route, or bait the thieves.

- `maera_family_correction`
  - Names her mother's work as vindication, inheritance, warning, or unfinished collaboration.

- `maera_vault_truth`
  - Chooses how the forbidden truth leaves the vault: copied, memorized, smuggled, or declared openly.

- `maera_oathstead_archive`
  - Lets Maera decide what she builds at Oathstead: public archive, hidden copy, field map, or scandal with shelves.

These outcomes should influence tone and later callbacks rather than hard-locking content. Maera should remember whether the player values caution, public truth, tactical deception, or personal witness.

## Implemented Internal Stage Sequence

The outer chain remains `maera_chain_1` through `maera_chain_8`, but every chapter now has internal stages.

### `maera_chain_1`: A Footnote With Teeth
- Inspect The Censored Star Map: `SEARCH` Censored Star Map.
- Compare The Royal Route Record: `SEARCH` Royal Route Record.
- Ask The Apprentice: `TALK` with an archive apprentice witness.
- Choose The First Citation: `CHOICE` using `maera_first_truth`.

### `maera_chain_2`: Ink Under Moonlight
- Gather Moonwell Ink: `GATHER` Moonwell Ink.
- Gather Star Moth Wings: `GATHER` Star Moth Wings.
- Find Clean Parchment: `SEARCH` Clean Parchment.

### `maera_chain_3`: The Map That Moved
- Inspect The Star Device: `SEARCH` Rotating Star Device.
- Recover The Cracked Lens: `SEARCH` Old Observatory Lens.
- Open The Sealed Northern Chart: `SEARCH` Sealed Northern Chart.
- Choose The Interpretation: `CHOICE` using `maera_moving_map`.

### `maera_chain_4`: The Archivist's Warning
- Read The Disciplinary Notice: `SEARCH` Disciplinary Notice.
- Recover The Revoked Seal: `SEARCH` Revoked Research Seal.
- Question The Registrar: `TALK` with a registrar witness.
- Choose How To Defy The Warning: `CHOICE` using `maera_archive_warning`.

### `maera_chain_5`: Cultists in the Stacks
- Defend The Restricted Stacks: `DEFEND` against wraith stand-ins for cult pressure.
- Recover Stolen Star Pages: `SEARCH` Stolen Star Pages.
- Inspect The Broken Moon Seal: `SEARCH` Broken Moon Seal.
- Choose What The Cult Proof Means: `CHOICE` using `maera_cult_pages`.

### `maera_chain_6`: The Quill Family Correction
- Search The Quill Study: `SEARCH` Quill Family Study.
- Read Her Mother's Notes: `SEARCH` Mother's Route Notes.
- Ask The Old Study Keeper: `TALK` with a witness who protected the room.
- Choose The Family Truth: `CHOICE` using `maera_family_correction`.

### `maera_chain_7`: The Forbidden Map Vault
- Enter The Forbidden Map Vault: `SEARCH` Forbidden Map Vault.
- Break The Inkbound Warden: `DEFEAT` Elder Wraith stand-in for corrupted censorship magic.
- Read The Five-Kingdom Chart: `SEARCH` Five-Kingdom Star Chart.
- Choose How Truth Leaves The Vault: `CHOICE` using `maera_vault_truth`.

### `maera_chain_8`: Properly Footnoted Treason
- Inspect Oathstead's Map Table: `VISIT` Oathstead Map Table.
- Complete The Star-Route Map: `SEARCH` Completed Star-Route Map.
- Choose The Archive: `CHOICE` using `maera_oathstead_archive`.

## Dialogue Pacing Notes

Early Maera:
- Sharp, funny, and defensive.
- She should talk about evidence before vulnerability.
- The first chapters should make official records feel actively edited, not merely mistaken.

Middle Maera:
- The Archive conflict should reveal that Maera is not just rebellious; she has been professionally punished for being correct.
- The cultist chapter should raise the stakes from academic danger to practical danger.
- The family chapter should soften her without making her sentimental. Her mother is not a saint to worship; she is a scholar whose unfinished work still matters.

Late Maera:
- The vault should make censorship feel magical, political, and personal at once.
- The final Oathstead scene should turn Maera from a lone forbidden scholar into a founder of a living archive.
- She should still be dry and difficult, but the player should feel that her trust has become real.

## Repeatable Post-Quest Dialogue Hooks

Root topic:
- "About the map we completed..."

Possible lines:
- "The map table is uneven. I have decided this makes it democratic."
- "I used to think truth needed a locked drawer. Now I think it needs witnesses who can run."
- "Your choices are in the margins, whether you like it or not."
- "If anyone asks, Oathstead is not harboring treason. It is hosting an aggressively accurate geography project."

## Banter Hooks

Exploration:
- Near roads: Maera notices old route logic, missing signs, and places where maps were corrected badly.
- Near ruins or graveyards: she comments on what people chose to preserve and what they expected the dead to keep quiet.
- Near Oathstead: she treats the settlement as an argument that has started building itself walls.

Combat:
- Against wraiths: "Even censorship gets dramatic when left underground too long."
- Against cultists or corrupted forces: "Anyone stealing maps for worship has already failed geography."
- After hard fights: she admits the player is useful before burying the admission under sarcasm.

Quest outcome banter:
- Public-truth outcomes: "You prefer truth with witnesses. Dangerous. I am fond of it."
- Cautious outcomes: "You hid the proof to keep it alive. I am trying not to resent how sensible that was."
- Bait outcomes: "You made the liars deny the evidence loudly. Cruel, elegant, and almost scholarly."
- Family-correction outcome: "You did not turn my mother into a monument. Thank you. Do not make me say it twice."

## Next Content Pass

- Add Maera-specific remembered-outcome lines in personal talks and Oathstead conversations.
- Add inn/tavern quiet scenes where she lowers the sarcasm and talks about being believed.
- Add romance-stage variants where flirtation is built around trust, shared danger, and Maera letting the player read her unfinished notes.
- Consider unique Archive cultist and Inkbound Warden monster keys later; for now `wraith` and `elder_wraith` are implementation-safe stand-ins.
