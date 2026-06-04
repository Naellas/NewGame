# Calder Reed Companion Quest Overhaul

## Design Goal

Calder's questline should feel like craft becoming emotional honesty. The player should not simply gather bridge materials for a blunt builder. They should learn how Calder reads structural failure, why he distrusts pretty explanations, and how his lost bridge left him carrying blame that needs to be set correctly before repair can hold.

Core emotional question:
- Can Calder accept responsibility without making himself the only support stone under every failure?

Core world question:
- Why did the flood bridge fail, who ignored the warnings, and what does Oathstead need to become load-bearing?

Primary systems used:
- `QuestStage` internals with the existing `nextQuestId` outer chain.
- `SEARCH`, `GATHER`, `DEFEND`, `DEFEAT`, `CHOICE`, `VISIT`.
- Branch outcome keys for later memory, banter, Oathstead, inn, tavern, and romance follow-up.

## Implemented Branch Outcome Keys

- `calder_bridge_warning`
  - Names the first bridge problem as neglect, bad craft, short coin, or ignored maintenance.

- `calder_tools_before_talk`
  - Decides the repair order: brace first, replace first, lighten the load, or close the bridge.

- `calder_weight_test`
  - Defines the maintenance rule: inspect after flood, limit loads, train locals, or write unavoidable warnings.

- `calder_causeway_stones`
  - Chooses where the old causeway stones go: bridge footings, flood wall, road anchors, or Oathstead repairs.

- `calder_lost_bridge`
  - Frames the collapsed bridge as Calder's fault, bad material, ignored warning, or communal need pretending risk vanished.

- `calder_bad_mortar`
  - Chooses what gets repaired first: public record, bridge fund, families owed, or Calder carrying all weight alone.

- `calder_stonebreaker_span`
  - Decides what the defended span proves: good craft, shared weight, second repair, or useful fear.

- `calder_oathstead_work`
  - Lets Calder choose what he builds at Oathstead: gate, bridge, workshop, or people who check each other's weight.

These outcomes should influence tone and later callbacks rather than hard-locking content. Calder should remember whether the player values practical work, public accountability, shared responsibility, prevention, or honest repair over easy absolution.

## Implemented Internal Stage Sequence

The outer chain remains `calder_chain_1` through `calder_chain_8`, but every chapter now has internal stages.

### `calder_chain_1`: The Bridge That Complained
- Inspect The Cracked Beam: `SEARCH` Cracked Bridge Beam.
- Check The Loose Footing: `SEARCH` Loose Stone Footing.
- Read The Mudline: `SEARCH` Bridge Mudline.
- Choose What The Bridge Says: `CHOICE` using `calder_bridge_warning`.

### `calder_chain_2`: Tools Before Talk
- Gather Straight Timber: `GATHER` Straight Timber.
- Gather Iron Nails: `GATHER` Iron Nails.
- Gather Rope Coils: `GATHER` Rope Coils.
- Choose The Work Order: `CHOICE` using `calder_tools_before_talk`.

### `calder_chain_3`: The Weight Test
- Inspect The Repaired Beam: `SEARCH` Repaired Bridge Beam.
- Check The Rope Supports: `SEARCH` Rope Supports.
- Inspect The Stone Anchors: `SEARCH` Stone Anchors.
- Choose The Maintenance Rule: `CHOICE` using `calder_weight_test`.

### `calder_chain_4`: Stones From the Old Causeway
- Gather Causeway Stones: `GATHER` Causeway Stones.
- Clear The Marsh Road: `DEFEAT` Bog Beasts.
- Inspect The Old Foundation: `SEARCH` Old Causeway Foundation.
- Choose How Old Stone Is Used: `CHOICE` using `calder_causeway_stones`.

### `calder_chain_5`: The Bridge He Lost
- Visit The Collapsed Flood Bridge: `VISIT` Collapsed Flood Bridge.
- Inspect The Broken Support Stone: `SEARCH` Broken Support Stone.
- Read The Memorial Plank: `SEARCH` Memorial Plank.
- Choose What Blame Carries: `CHOICE` using `calder_lost_bridge`.

### `calder_chain_6`: Blame Has Bad Mortar
- Inspect The Flood Repair Ledger: `SEARCH` Flood Repair Ledger.
- Find The Missing Supply Entry: `SEARCH` Missing Supply Entry.
- Open The Sealed Complaint: `SEARCH` Sealed Village Complaint.
- Choose The Repair Of Blame: `CHOICE` using `calder_bad_mortar`.

### `calder_chain_7`: The Stonebreaker's Span
- Inspect The Damaged Span: `VISIT` Damaged Causeway Span.
- Drive Off The Bog Beasts: `DEFEND` the span.
- Check The Central Support: `SEARCH` Restored Central Support.
- Choose What The Span Proves: `CHOICE` using `calder_stonebreaker_span`.

### `calder_chain_8`: What Holds
- Inspect The Reinforced Gate: `VISIT` Oathstead Reinforced Gate.
- Inspect The New Bridge Plank: `SEARCH` Oathstead Bridge Plank.
- Choose What Holds At Oathstead: `CHOICE` using `calder_oathstead_work`.

## Dialogue Pacing Notes

Early Calder:
- Blunt, practical, and almost aggressively allergic to empty comfort.
- He should teach the player to read structures as evidence.
- His care should appear through repair work before it appears through confession.

Middle Calder:
- The lost bridge should not make him innocent, but it should make the failure legible.
- The records chapter should separate public accountability from private self-punishment.
- Player choices should help set blame correctly so repair has somewhere true to rest.

Late Calder:
- The defended span should prove repair under pressure, not just in theory.
- Calder's final Oathstead scene should make the settlement feel worth building because it admits it leans.
- His trust should feel like being allowed to help carry weight, not being praised.

## Repeatable Post-Quest Dialogue Hooks

Root topic:
- "About what holds..."

Possible lines:
- "Gate still leans. Less badly. That is progress, not poetry."
- "I checked the plank this morning. Held under two buckets, one child, and a terrible idea."
- "You set blame where it belonged. Harder than stonework. Less satisfying sound."
- "Oathstead is not strong because it stands. It is strong because people notice when it starts failing."

## Banter Hooks

Exploration:
- Near bridges or roads: Calder comments on load, shortcuts, and hidden failure points.
- Near villages: he notices whether maintenance is being done before disaster forces it.
- Near Oathstead: he treats every rough repair as a promise that needs checking.

Combat:
- Against bog beasts: "Teeth off the supports. Then teeth off everything else."
- Against heavy enemies: "Big things fall if the footing lies."
- After hard fights: "Still standing. Check why before celebrating."

Quest outcome banter:
- Public-record outcomes: "Truth written down is a brace. Not enough alone. Necessary."
- Families-owed outcomes: "Repair the living too. Hard work. Correct work."
- Shared-weight outcomes: "You noticed one person should not be the whole support. Good eye."
- Maintenance outcomes: "You chose checking after the flood. Boring answer. Best one."

## Next Content Pass

- Add Calder-specific remembered-outcome lines in personal talks and Oathstead conversations.
- Add inn/tavern quiet scenes where he admits rest is also maintenance.
- Add romance-stage variants where closeness is framed as shared weight and practical trust.
- Consider a unique Mudjaw Breaker monster key later; for now `bog_beast` is an implementation-safe stand-in.
