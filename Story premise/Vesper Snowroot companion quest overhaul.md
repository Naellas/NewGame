# Vesper Snowroot Companion Quest Overhaul

## Design Goal

Vesper's questline should feel like winter care becoming ecological grief, then grief becoming chosen growth. The player should not simply gather herbs for a druid. They should learn why Vesper distrusts dramatic omens, why she starts with practical warmth, and why spring can be both healing and dangerous.

Core emotional question:
- Can Vesper let something wounded grow again without forcing it to become proof that the pain was worthwhile?

Core world question:
- Why did the Snowroot grove stop answering, and what was sealed beneath the winter shrine?

Primary systems used:
- `QuestStage` internals with the existing `nextQuestId` outer chain.
- `SEARCH`, `GATHER`, `TALK`, `DEFEAT`, `CHOICE`, `VISIT`.
- Temporary quest NPCs for the feverish child and old hermit druid.
- Branch outcome keys for later memory, banter, Oathstead, inn, tavern, and romance follow-up.

## Implemented Branch Outcome Keys

- `vesper_first_root`
  - Names the waking root as healing, hunger, warning, or grief.

- `vesper_practical_care`
  - Prioritizes chasing signs, protecting the sick, or waking the grove.

- `vesper_family_grove`
  - Names the old seal as protection, fear, sacrifice, or a mistake that saved lives.

- `vesper_buried_spring`
  - Judges the old druid's burial of spring as mercy, cowardice, sacrifice, or fear with good intentions.

- `vesper_spring_return`
  - Decides whether spring returns quickly, carefully, publicly, or only with the roots' consent.

- `vesper_oathstead_growth`
  - Lets Vesper choose staying, wandering, tending, or admitting Oathstead has become living ground.

These outcomes should influence tone and later callbacks rather than hard-locking content. Vesper should remember whether the player rushes growth, protects living things, or respects recovery as a process.

## Implemented Internal Stage Sequence

The outer chain remains `vesper_chain_1` through `vesper_chain_8`, but every chapter now has internal stages.

### `vesper_chain_1`: The Green Under White
- Read The Root Circle: `SEARCH` Snowroot Circle.
- Inspect The Frozen Seed Bowl: `SEARCH` Frozen Seed Bowl.
- Name The Waking: `CHOICE` using `vesper_first_root`.

### `vesper_chain_2`: Firewood and Feverroot
- Gather Firewood: `GATHER` Firewood Bundles.
- Gather Feverroot: `GATHER` Feverroot And Winter Moss.
- Check The Feverish Child: `TALK` with a temporary child/witness NPC.
- Choose The Care: `CHOICE` using `vesper_practical_care`.

### `vesper_chain_3`: The Frozen Burrow
- Drive Off The Wolves: `DEFEAT` Frost Wolves.
- Inspect The Burrows: `SEARCH` Frozen Burrows.
- Collect Black Root Threads: `GATHER` Black Root Threads.

### `vesper_chain_4`: Snowroot Memory
- Find The Family Grove: `SEARCH` Snowroot Grove.
- Read The Carved Names: `SEARCH` Carved Family Names.
- Recover The Green Charm: `SEARCH` Buried Green Charm.
- Choose What The Grove Kept: `CHOICE` using `vesper_family_grove`.

### `vesper_chain_5`: The Druid Who Buried Spring
- Find The Hermit Cave: `SEARCH` Hermit Cave.
- Question The Old Druid: `TALK` with a temporary hermit druid.
- Open The Seed Chest: `SEARCH` Sealed Seed Chest.
- Judge The Burial: `CHOICE` using `vesper_buried_spring`.

### `vesper_chain_6`: Frosthollow Roots
- Clear The Root Nest: `DEFEAT` Cave Spiders.
- Gather Frozen Root Shards: `GATHER` Frozen Root Shards.
- Read The Cave Root Wall: `SEARCH` Cave Root Wall.

### `vesper_chain_7`: The Buried Spring
- Find The Sealed Spring Pool: `SEARCH` Sealed Spring Pool.
- Break The Winterroot Hollow: `DEFEAT` Ice Golem stand-in for the guardian.
- Find The First Green Shoot: `SEARCH` First Green Shoot.
- Choose How Spring Returns: `CHOICE` using `vesper_spring_return`.

### `vesper_chain_8`: Snowroot Blooms
- Read Oathstead's Soil: `VISIT` Oathstead Garden Soil.
- Plant The Snowroot Cutting: `SEARCH` Snowroot Cutting.
- Choose The Bloom: `CHOICE` using `vesper_oathstead_growth`.

## Dialogue Pacing Notes

Early Vesper:
- Practical before mystical.
- She should ask for firewood, feverroot, and care before omen interpretation.
- Her warmth should feel grounded, not naive.

Middle Vesper:
- The family grove should reveal that her guilt was built on incomplete truth.
- She should resist both blame without evidence and comfort without honesty.
- The old druid choice should be morally difficult: the seal may have saved lives and still caused harm.

Late Vesper:
- The buried spring is not simply "saved." It must be allowed to return carefully.
- Vesper's final Oathstead scene should frame home as soil that asks and answers, not a cage.

## Repeatable Post-Quest Dialogue Hooks

Root topic:
- "About the bloom we chose..."

Possible lines:
- "The cutting is alive. It sulks in cold wind, which is how I know it belongs here."
- "I used to think spring meant the end of grief. Now I think it means grief has somewhere to put its hands."
- "Oathstead's soil is tired, but it answers. I understand that more than I expected."
- "Do not rush green things. They remember every hand that pulled at them before they were ready."

## Banter Hooks

Exploration:
- In snow or mountains: Vesper comments on quiet survival under harsh weather.
- Near farms or gardens: she notices whether growth is being asked from the soil or demanded from it.
- After gathering: she praises taking what is needed and leaving tomorrow alive.

Combat:
- Against beasts: "They are frightened, not evil. Survive them; do not hate them."
- Against spiders: "The nest is guarding hunger. That is different from guarding home."
- Against ice/elementals: "Old fear makes strong walls. Strong is not the same as right."

Quest outcome banter:
- Care-first outcome: "You remembered the sick before the sign. The land noticed."
- Cautious-growth outcome: "You did not force spring to perform recovery. Good."
- Public-return outcome: "Hope seen openly becomes a promise. Handle that carefully."

## Next Content Pass

- Add Vesper-specific remembered-outcome lines in personal talks and Oathstead conversations.
- Add inn/tavern quiet scenes where she compares warmth, shelter, and chosen company.
- Add romance-stage variants about growing beside someone without being tended like a fragile thing.
- Consider a unique Winterroot Hollow monster key later; for now `ice_golem` is used as an implementation-safe stand-in.
