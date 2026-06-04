# Samir Dawn Companion Quest Overhaul

## Design Goal

Samir's questline should feel like faith learning to breathe through doubt. The player should not simply help a holy warrior cleanse shrines. They should witness how Sanctum used certainty as a leash, how Samir's family inherited sealed questions, and how Oathstead lets him keep a light that answers without demanding obedience.

Core emotional question:
- Can Samir trust his doubt as witness rather than treating it as a failure of faith?

Core world question:
- Who taught dawn-light to lie, and how much of Sanctum's obedience was built around hiding older witness?

Primary systems used:
- `QuestStage` internals with the existing `nextQuestId` outer chain.
- `SEARCH`, `GATHER`, `TALK`, `DEFEAT`, `CHOICE`, `VISIT`.
- Temporary quest NPCs for doubtful acolytes and shrine witnesses.
- Branch outcome keys for later memory, banter, Oathstead, inn, tavern, and romance follow-up.

## Implemented Branch Outcome Keys

- `samir_reliquary_truth`
  - Names the family reliquary as inheritance, warning, prison, or a question his family feared.

- `samir_forbidden_rite`
  - Chooses what the dawn rite asks: whether light lies, whether doubt is witness, whether faith can disobey, or whether Samir is afraid.

- `samir_questioned_light`
  - Frames doubt as warning, prayer, accusation, or the first honest light in the room.

- `samir_first_keeper`
  - Renames the first keeper as keeper, witness, rebel, or a faithful person punished for seeing clearly.

- `samir_missing_verse`
  - Decides how the censored hymn returns: quietly, publicly, as mourning, or as refusal.

- `samir_holy_anger`
  - Chooses what Samir's anger serves: protection, truth, mourning, or refusal to let harm call itself sacred.

- `samir_second_dawn`
  - Defines the second dawn as revealed truth, chosen faith, mercy after fire, or doubt carried openly.

- `samir_oathstead_light`
  - Lets Samir choose what he keeps at Oathstead: shrine, lantern road, doubtful chapel, or a place where faith answers questions.

These outcomes should influence tone and later callbacks rather than hard-locking content. Samir should remember whether the player respects doubt, public witness, restraint, anger pointed at harm, or faith that chooses rather than obeys.

## Implemented Internal Stage Sequence

The outer chain remains `samir_chain_1` through `samir_chain_8`, but every chapter now has internal stages.

### `samir_chain_1`: Cinders in the Reliquary
- Snuff The Reliquary Imps: `DEFEAT` Ember Imps.
- Inspect The Family Reliquary: `SEARCH` Family Reliquary.
- Read The Inner Seal: `SEARCH` Inner Reliquary Seal.
- Choose What The Seal Means: `CHOICE` using `samir_reliquary_truth`.

### `samir_chain_2`: Oil for the Dawn Lamp
- Gather Clean Lamp Oil: `GATHER` Clean Lamp Oil.
- Gather Ash Salt: `GATHER` Ash Salt.
- Gather Prayer Thread: `GATHER` Prayer Thread.
- Choose The Rite's Purpose: `CHOICE` using `samir_forbidden_rite`.

### `samir_chain_3`: Lantern Without Permission
- Inspect The Ward Seal: `SEARCH` Ward Seal.
- Trace The Backward Shadow: `SEARCH` Backward Shrine Shadow.
- Question The Doubtful Acolyte: `TALK` with a temporary acolyte witness.
- Choose How Doubt Speaks: `CHOICE` using `samir_questioned_light`.

### `samir_chain_4`: Ashes of the First Keeper
- Scatter The Ember Envoy: `DEFEAT` Ember Imps.
- Recover The Ancestor Record: `SEARCH` First Keeper Record.
- Inspect The Kneeling Mark: `SEARCH` Kneeling Mark.
- Choose The First Keeper's Name: `CHOICE` using `samir_first_keeper`.

### `samir_chain_5`: The Hymn That Lied
- Read The Censored Dawn Hymn: `SEARCH` Censored Dawn Hymn.
- Inspect The Cracked Sun Tile: `SEARCH` Cracked Sun Tile.
- Inspect The Family Prayer Chain: `SEARCH` Family Prayer Chain.
- Choose The Missing Verse: `CHOICE` using `samir_missing_verse`.

### `samir_chain_6`: Prayer in the Ash Field
- Enter The Ash Field: `VISIT` Ash Field.
- Free The Prayer Chain: `DEFEAT` Ash Wraith stand-in.
- Recover His Mother's Chain: `SEARCH` Mother's Prayer Chain.
- Choose What Anger Serves: `CHOICE` using `samir_holy_anger`.

### `samir_chain_7`: Dawn Chosen Twice
- Enter The False Dawn Altar: `VISIT` False Dawn Altar.
- Inspect The Inverted Sun Symbol: `SEARCH` Inverted Sun Symbol.
- Break The Flame Herald: `DEFEAT` Flame Herald.
- Choose The Second Dawn: `CHOICE` using `samir_second_dawn`.

### `samir_chain_8`: A Lantern at Oathstead
- Inspect Oathstead's Oath Marker: `VISIT` Oathstead Oath Marker.
- Place The Dawn Lantern: `SEARCH` Oathstead Dawn Lantern.
- Read The Repaired Oath Tablet: `SEARCH` Repaired Oath Tablet.
- Choose The Light That Stays: `CHOICE` using `samir_oathstead_light`.

## Dialogue Pacing Notes

Early Samir:
- Calm, precise, and quietly wounded by inherited obedience.
- He should not reject faith. He rejects certainty used to silence witness.
- The first chapters should make family, ritual, and doubt feel intertwined rather than separate lore facts.

Middle Samir:
- The ward seal and first keeper chapters should prove that bad light can imitate holiness.
- The hymn and prayer-chain chapters should make his mother's grief active and meaningful.
- Player choices should let Samir distinguish doubt, accusation, mourning, refusal, and honest faith.

Late Samir:
- The ash field should make anger morally useful without making anger the whole answer.
- The false dawn fight should reject light as command and recover light as revelation.
- The final Oathstead scene should make his lantern a living practice: small, steady, portable, and unowned.

## Repeatable Post-Quest Dialogue Hooks

Root topic:
- "About the lantern at Oathstead..."

Possible lines:
- "The lantern burns lower than Sanctum would approve. Travelers can still see it. That is enough."
- "I thought doubt was a crack in faith. It seems to be where the air enters."
- "Your answer is still in the repaired tablet. I read it when certainty starts sounding too comfortable."
- "Oathstead does not make me kneel before the light. That is why I can pray here."

## Banter Hooks

Exploration:
- Near shrines: Samir notices whether the place reveals truth or only demands posture.
- Near roads at dawn: he comments on light as guidance rather than command.
- Near Oathstead: he treats ordinary lamps, markers, and shared meals as small forms of chosen faith.

Combat:
- Against ember enemies: "Fire is honest when it burns. Make it answer honestly."
- Against wraiths: "Do not hate the grief it wears. Free it."
- Against false holy enemies: "Brightness is not proof. Stand where it must reveal you."

Quest outcome banter:
- Doubt-as-witness outcomes: "You did not hurry to cure my doubt. I noticed."
- Public-hymn outcomes: "A missing verse sung aloud is not merely music. It is repair."
- Holy-anger outcomes: "You let anger point at harm instead of becoming worship. That matters."
- Chosen-faith outcomes: "You understood that dawn chosen freely is brighter than dawn assigned."

## Next Content Pass

- Add Samir-specific remembered-outcome lines in personal talks and Oathstead conversations.
- Add inn/tavern quiet scenes where he discusses rest, prayer, and ordinary light without ceremony.
- Add romance-stage variants where intimacy is framed as being witnessed without being commanded.
- Consider unique Ash Wraith and false dawn priest variants later; for now `ember_imp`, `ash_wraith`, and `flame_herald` are implementation-safe stand-ins if the current monster registry supports them.
