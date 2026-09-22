# Companion Dialogue and Story Quest Implementation Plan

Date: 2026-06-17

This document turns the recent dialogue-design discussion into an implementation brief for agents. The goal is not simply to make dialogue more poetic. The goal is to make companion conversations understandable, personal, quest-aware, and visibly connected to the world.

## Core Problem

Current dialogue can become too generic in two different ways:

- It can sound like shared template text no matter which NPC or companion is speaking.
- It can sound literary but unclear, using phrases such as "a warning that forgot how to be gentle" before the player understands the concrete event.

The fix is:

1. Put concrete story facts first.
2. Add emotional meaning second.
3. Tie every major companion line to a player action, world object, placed objective, named NPC, monster, or visible consequence.

## Writing Rule

Every important line should answer at least one of these:

- What is physically happening?
- Who is affected?
- What does the player need to do?
- Why does this matter emotionally, morally, or politically?

Use this pattern:

```text
Concrete fact. Consequence. Character meaning.
```

Good:

```text
Vesper: The roots are breaking the road because something under the old grove is awake. If we cut every root, we may stop the road damage and still kill the grove. I need you to inspect the black sap before we choose steel.
```

Avoid:

```text
Vesper: It is a warning that forgot how to be gentle.
```

That sentence can survive only after the concrete explanation, not instead of it.

## Design Goals

- Player options should feel contextual to the current companion, quest stage, objective, relationship level, and location.
- Companion dialogue should guide the player through quest objectives without becoming UI exposition.
- Every companion storyline should have visible world evidence: props, monsters, NPCs, locations, or altered spaces.
- Important quest objectives should not be generic "gather 3 things" unless the things themselves carry story meaning.
- Major companion quests should place bespoke story objects and encounters in the world.
- Companion memory callbacks should reference earlier choices in later conversations.

## Files Likely To Change

### Dialogue and Conversation

- `Java/src/main/java/com/alderfall/game/content/DialogueLibrary.java`
  - Companion voice lines.
  - Companion quest openings, support/challenge/warning responses.
  - NPC/player option labels.
  - Outcome memory lines.
  - Shared quest dialogue helpers.

- `Java/src/main/java/com/alderfall/game/ui/DialogueRenderer.java`
  - Intent color and labels for options.
  - Option preview display.
  - Any added iconography or intent label adjustments.

- `Java/src/main/java/com/alderfall/game/state/GameState.java`
  - Dialogue option intent metadata.
  - Quest state, quest completion, companion relationship, outcome memory, objective interaction.
  - Active quest objectives, quest NPCs, quest monsters, interactibles.

- `Java/src/main/java/com/alderfall/game/diagnostics/CompanionDialogueQaExport.java`
  - Add QA probes for new companion quest arcs.
  - Check vague options and repeated generic labels.
  - Sample relationship stages and quest stages.

### Quest Content and Data

- `Java/src/main/java/com/alderfall/game/content/GameData.java`
  - `QUESTS`
  - companion staged quests
  - `MONSTERS`
  - `MONSTER_LORE`
  - `NPCS`
  - `RECRUITS`
  - quest objective text, start/progress/ready/complete dialogue

- `Java/src/main/java/com/alderfall/game/entities/Quest.java`
  - Add fields only if existing quest stage/objective fields cannot express the new story requirements.
  - Prefer using existing objective kind/map/target/monster/item fields first.

- `Java/src/main/java/com/alderfall/game/entities/Npc.java`
  - Use existing fields where possible.
  - Extend only if quest-specific NPC metadata cannot be modeled through existing runtime quest NPC generation.

### World Placement, Props, Monsters, and Rendering

- `Java/src/main/java/com/alderfall/game/map/WorldMap.java`
  - World map sites, settlement maps, location sites, prop placement helpers.
  - Add stable placement rules for bespoke companion quest landmarks.

- `Java/src/main/java/com/alderfall/game/state/GameState.java`
  - `activeQuestObjectives`
  - `uniqueQuestObjectivePoint`
  - quest monster spawning/runtime
  - quest NPC spawning/runtime
  - quest interactibles
  - objective interaction and completion

- `Java/src/main/java/com/alderfall/game/render/world/WorldPropRenderer.java`
  - Rendering behavior for new props.
  - Glow, wind, soft-ground, readable, fire, magic, or resource animation classification.

- `Java/src/main/java/com/alderfall/game/rendering/WorldRenderer.java`
  - Ground prop visibility and layering if new props need special treatment.

- `Java/src/main/java/com/alderfall/game/render/world/WorldLightingRenderer.java`
  - Add light behavior for magical props, ward stones, corrupted roots, lanterns, shrine markers, etc.

- `Java/src/main/java/com/alderfall/game/render/world/WorldPropRenderer.java`
  - Add prop sizing/offsets if generated assets are not standard tile size.

- `Java/assets/...`
  - New companion quest props, monster sprites, NPC sprites, and location art.

### Tests and Diagnostics

- `Java/src/main/java/com/alderfall/game/diagnostics/SmokeTest.java`
  - Add checks that bespoke quest objectives spawn on reachable/passable tiles.
  - Add checks that required props exist near the intended settlement/location.
  - Add checks that quest monsters and NPCs spawn with valid specs.

- `Java/src/main/java/com/alderfall/game/diagnostics/CompanionDialogueQaExport.java`
  - Add content QA: vague metaphor detection, repeated option detection, missing concrete objective clue detection.

## Dialogue Implementation Rules

### Concrete Before Poetic

Every companion quest beat should follow:

```text
NPC: [Concrete fact]. [What it means]. [What the player should do or decide].
```

Example:

```text
Vesper: The root breaks are all leading back to my family grove. My family sealed that grove and told Snowrest it was safe. If the sap has turned black, the seal is poisoning the road from below.
```

### Player Options Should Be Stances

Do not use generic options like:

```text
Tell me more.
I will help.
Why does this matter?
```

Prefer contextual stances:

```text
Show me where the sap first turned black.
If people are in danger, I choose them over the grove.
Your family lied. I need to know what they buried.
We inspect first. Steel only where the rot has already won.
```

### Companion Replies Must React To The Chosen Stance

If the player chooses a pragmatic or harsh option, the reply should not be the same as the warm option.

Example:

```text
Player: If people are in danger, I choose them over the grove.

Vesper: Good. Say it plainly. I may hate the choice when we reach it, but I would rather walk beside someone honest about the knife in their hand.
```

### Quest Guidance Should Be In-World

Instead of:

```text
Inspect 3 root breaks near Snowrest.
```

Use:

```text
Vesper: Start with the three places where the road buckled: the sled rut, the old cairn, and the frozen culvert. Clear sap means the root can be calmed. Black sap means we cut that branch only.
```

The UI can still track the objective, but the dialogue should explain it as a real action.

## Quest Objective Design Rules

Each companion quest stage should define:

- Objective kind: `DEFEAT`, `RESCUE`, `DEFEND`, `GATHER`, `DELIVER`, `VISIT`, `SEARCH`, `TALK`, `ASK_AROUND`, `REPORT`, `ESCORT`, `CHOICE`, or `RAID_DEFENSE`.
- Physical place: map id, settlement, world location, or generated site.
- Story object: prop, monster, NPC, or readable evidence.
- Player action: inspect, calm, cut, rescue, question, deliver, defend, choose.
- Consequence: what changes after completion.
- Memory key: outcome referenced later by the companion.

For each objective, answer:

```text
Why is this object/person/monster here?
Why does the companion care?
What should the player learn by interacting with it?
What changes after the player finishes?
```

## World Placement Rules

Quest objectives should not appear anywhere valid by tile alone. They should appear where the story says they belong.

### Placement Requirements

- Root/grove content belongs near forest, mountain, Snowrest, or old grove sites.
- Archive/star-map content belongs in archive maps, ruins, grave sites, or high places.
- Guard/war content belongs near gates, roads, watch posts, camps, or battle scars.
- Healer/plague content belongs near clinics, wells, camps, marsh pools, or crowded settlements.
- Merchant/debt content belongs near trade roads, caravans, inns, markets, or ambush sites.
- Bridge/craft content belongs near rivers, ravines, village infrastructure, or construction props.

### Implementation Notes

- Prefer deterministic placement based on quest id and stage so saves remain stable.
- Use existing `GameState.activeQuestObjectives` and `uniqueQuestObjectivePoint` logic where possible.
- If a quest needs a named location, add a location site or settlement/world prop anchor in `WorldMap`.
- If using props as objectives, ensure `activeQuestInteractibles` can identify them and `interactQuestObjective` or `interactQuestInteractible` can progress the quest.
- Add smoke checks for reachability and no overlap with blocked tiles.

## Asset and Entity Generation Rules

Unique companion quest assets are encouraged when they clarify the story. They should be generated for story function, not decoration.

### Unique Props

Each major companion arc should get at least 2-4 bespoke props.

Examples:

- `quest_vesper_black_sap_root`
- `quest_vesper_sealed_grove_stone`
- `quest_maera_broken_star_map`
- `quest_cassia_burned_gate_banner`
- `quest_lyra_false_medicine_crate`
- `quest_aria_marked_briar_snare`
- `quest_rafiq_debt_ledger`
- `quest_calder_cracked_bridge_pylon`

Prop requirements:

- readable silhouette at game scale
- clear story purpose
- asset key included in `GameData` or placement code
- render classification in `WorldPropRenderer` if glowing, soft-ground, readable, animated, tall, or offset
- lighting classification in `WorldLightingRenderer` if magical/fire/lamp-like

### Unique Monsters

Use unique monsters when the enemy represents the companion's story, not just combat filler.

Examples:

- Vesper: `black_sap_rootbound`, `winter_grove_warden`
- Maera: `inkbound_scholar`, `starless_witness`
- Cassia: `oathbreaker_echo`, `gate_ash_raider`
- Lyra: `fever_wraith`, `plague_mummer`
- Aria: `briar_snare_beast`, `masked_trail_hunter`
- Rafiq: `silk_debt_duelist`, `ledgerbound_cutthroat`
- Calder: `river_pylon_thing`, `mudfoundation_brute`
- Seraphine: `contract_knives_agent`, `velvet_room_assassin`

Monster requirements:

- Add `MonsterSpec` in `GameData.MONSTERS`.
- Add lore in `GameData.MONSTER_LORE`.
- Ensure battle creation can resolve the monster key.
- If monster is a quest overworld threat, use quest monster runtime in `GameState`.
- If monster appears in a dungeon/location, ensure dungeon spawn logic accepts the key.

### Unique NPCs

Use unique NPCs when the story needs testimony, accusation, rescue, or a moral mirror.

Examples:

- Vesper: Snowrest elder who remembers the grove sealing.
- Maera: archive censor who altered a star-route record.
- Cassia: surviving gate recruit who contradicts official reports.
- Lyra: patient harmed by false medicine.
- Aria: missing-sister witness who knows one false trail was planted.
- Rafiq: creditor's messenger with a half-true debt record.
- Calder: bridge worker who signed a false inspection.
- Seraphine: contract witness from Riverside court.

NPC requirements:

- Prefer quest runtime NPCs when they exist only for a quest stage.
- Use `GameData.NPCS` only for persistent settlement NPCs.
- Quest NPC dialogue should use concrete testimony and avoid generic rumor phrasing.

## Companion Arc Templates

Each companion should have a 3-5 quest arc:

1. Public problem
2. Personal connection
3. Contradiction or revealed lie
4. Player choice
5. Oathstead/home/world consequence

Each stage should include:

- Start dialogue
- Clarifying question
- Practical objective explanation
- Companion vulnerability line
- Warning line
- Completion line
- Outcome memory line

## Companion Arc Directions

### Vesper Snowroot

Theme: survival, buried truth, inherited silence, choosing spring without hiding winter.

Arc:

1. Road roots are breaking through near Snowrest.
2. Roots trace back to the Snowroot family grove.
3. Family sealed a corrupted spring and lied that the grove was protected.
4. Player chooses purify, reseal truthfully, or redirect at risk.
5. Oathstead receives a cutting or memorial depending on outcome.

Concrete objective examples:

- Inspect black-sap root props at road breaks.
- Question Snowrest elder.
- Defeat rootbound guardian only if corruption spreads.
- Search sealed grove stones.
- Choose what happens to the buried spring.

Avoid vague lines:

- "The roots are warning us."

Use:

- "The roots broke the road in three places, all pointing back to my family grove. If the sap is black, the seal under that grove is poisoning them."

### Maera Quill

Theme: truth, dangerous knowledge, archives as power.

Arc:

1. A star-route map contradicts official records.
2. Archive pages were edited to hide a route used by enemies.
3. Maera must decide whether truth belongs public, sealed, or weaponized.
4. Player gathers map fragments, questions archivists, searches ruins.
5. Oathstead gets an observatory/archive table if truth is preserved.

Concrete objective examples:

- Search `quest_maera_broken_star_map` props.
- Talk to archive censor NPC.
- Defeat inkbound scholar or starless witness.
- Report truth to Maera or public archive.

### Cassia Flint

Theme: duty, guilt, command, the difference between holding a gate and telling the truth.

Arc:

1. Old gate reports do not match survivor accounts.
2. Cassia learns a command decision sacrificed people unnecessarily.
3. Player investigates burned banners, gate ruins, and survivor testimony.
4. Choice: expose command, protect morale, or force public accountability.
5. Oathstead gains a watch post memorial or tactical training yard.

Concrete objective examples:

- Visit burned gate props.
- Talk to surviving recruit.
- Defend a gate during a raid objective.
- Defeat oathbreaker echo.

### Lyra Bell

Theme: care, triage, false medicine, who gets saved first.

Arc:

1. A sickness pattern repeats across towns.
2. False medicine crates are spreading harm.
3. Lyra must confront a healer/order/vendor who chose reputation over patients.
4. Player gathers samples, rescues patients, questions witnesses.
5. Oathstead clinic changes based on mercy/accountability choice.

Concrete objective examples:

- Gather medicine crate props.
- Rescue fever patient NPC.
- Defeat fever wraith/plague mummer if illness manifests.
- Choose public accusation, quiet cure, or forced restitution.

### Aria Foxglove

Theme: false trails, family disappearance, survival through being underestimated.

Arc:

1. An ambush site contains signs too neat to be real.
2. Aria finds evidence someone planted trails about her sister.
3. Player follows marked briars, questions road witnesses, disarms snares.
4. Choice: pursue truth, protect surviving witness, or set a counter-ambush.
5. Oathstead gets road scouts or hidden trail markers.

Concrete objective examples:

- Search marked briar props.
- Escort witness NPC.
- Defeat masked trail hunter.
- Choose how to handle the false trail.

### Rafiq Glass

Theme: debt, charm as defense, whether a person can stop fleeing their own story.

Arc:

1. A creditor's marker follows Rafiq into safe towns.
2. A debt ledger contains lies mixed with real guilt.
3. Player collects ledger pages and duels or negotiates with debt agents.
4. Choice: pay, expose, duel, or burn the debt record.
5. Oathstead gets trade consequences, dueling yard, or debt shelter memory.

Concrete objective examples:

- Deliver or steal debt ledger prop.
- Talk to creditor messenger NPC.
- Defeat silk-debt duelist.
- Choose what happens to the ledger.

### Calder Reed

Theme: repair, foundations, guilt over a failed bridge or missed rescue.

Arc:

1. A bridge or foundation fails in a way that looks natural but is not.
2. Calder finds old inspection marks tied to a loss.
3. Player gathers pylon evidence, rescues trapped workers, defeats river/mud threat.
4. Choice: rebuild fast, rebuild honestly, or expose the false inspection first.
5. Oathstead gains bridgework, mill, or workshop consequence.

Concrete objective examples:

- Inspect cracked pylon props.
- Rescue trapped worker NPC.
- Defend repair site.
- Defeat river pylon monster.

### Seraphine Vale

Theme: contracts, beauty as armor, who owns a person through paper.

Arc:

1. A contract from Riverside resurfaces.
2. Seraphine knows the contract is legal but false in spirit.
3. Player searches court rooms, talks to witnesses, steals or exposes clauses.
4. Choice: destroy contract, use it against its owner, or free others publicly.
5. Oathstead gets court/ledger consequence or social refuge.

Concrete objective examples:

- Search contract knife/ledger props.
- Talk to contract witness NPC.
- Defeat velvet-room assassin.
- Report or expose contract truth.

## Dialogue QA Additions

Add automated or semi-automated checks in `CompanionDialogueQaExport.java`:

- Flag major companion quest lines under 80 characters that contain metaphor but no concrete noun from the quest.
- Flag repeated option labels across different NPCs where role/biome/quest should vary.
- Flag objective dialogue that lacks a place clue.
- Flag objective dialogue that lacks an action verb.
- Flag companion completion lines that do not reference the player action or outcome.

Recommended review checklist:

```text
Can the player explain what is happening after one exchange?
Can the player explain where to go?
Can the player explain why the companion cares?
Does the objective have a visible world object/person/monster?
Does the completion line name what changed?
Does a later line remember the choice?
```

## Implementation Phases

### Phase 1: Story Specification

For each companion, create a small table:

```text
quest id
stage title
objective kind
map/location
prop/NPC/monster keys
start line
practical guidance line
warning line
completion line
memory key
choice outcomes
```

Do not implement dialogue until this table exists.

### Phase 2: Content Data

Update `GameData.java`:

- Add or rewrite companion staged quests.
- Add unique monster specs/lore.
- Add persistent NPCs only where needed.
- Add objective target names that are concrete and readable.

### Phase 3: Objective Placement

Update `GameState.java` and `WorldMap.java`:

- Place bespoke objectives near story-appropriate sites.
- Ensure active objectives produce unique props/NPCs/monsters.
- Ensure objectives are reachable and cannot spawn inside blocked spaces.
- Ensure return objectives route back to the correct companion/NPC.

### Phase 4: Dialogue Rewrite

Update `DialogueLibrary.java`:

- Rewrite companion quest openings.
- Add concrete/practical guidance lines.
- Add relationship-aware follow-ups.
- Add memory callbacks for completed choices.
- Keep option intent labels/color support intact.

### Phase 5: Assets

Generate or add assets for props, monsters, and NPCs:

- prop sprites in `Java/assets/deco` or relevant asset folder
- monster sprites in the existing monster asset location
- source images in `Java/assets/source` if generated
- renderer classifications if size/glow/animation differs from defaults

### Phase 6: Verification

Run:

```powershell
$out = Join-Path (Get-Location) 'Java\out-codex-dialogue-check'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$sources = Get-ChildItem -Path 'Java\src\main\java' -Recurse -Filter *.java | Sort-Object FullName | ForEach-Object { $_.FullName }
javac -d $out $sources
java -cp Java\out-codex-dialogue-check com.alderfall.game.CompanionDialogueQaExport Java\out-codex-dialogue-check\companion-dialogue-qa.md
java -cp Java\out-codex-dialogue-check com.alderfall.game.diagnostics.SmokeTest
git diff --check
```

Expected:

- compile passes
- dialogue QA has `Total findings: 0`
- smoke test passes
- no whitespace errors

Remove temporary output after verification:

```powershell
Remove-Item -LiteralPath 'Java\out-codex-dialogue-check' -Recurse -Force
```

## Example Fully Improved Beat

```text
Vesper: Do not cut those roots yet.

Player:
1. They tore open the road.
2. Then tell me what they are.
3. People could have died here.
4. I am cutting them before they spread.

Player chooses: Then tell me what they are.

Vesper: They are from the old Snowroot grove. My family sealed that grove when I was a child and told everyone the ward was safe. It was not. Something under it is awake now, and the roots are breaking through the road because they have nowhere else to go.

Player:
1. What is buried under the grove?
2. How do we stop it hurting people?
3. Why did your family lie?
4. If the roots keep spreading, I cut them.

Player chooses: How do we stop it hurting people?

Vesper: First, we check three root breaks along the road: the sled rut, the old cairn, and the frozen culvert. Clear sap means I can calm them. Black sap means the corruption has reached that far, and we cut only that branch. Not the whole root.

Player:
1. I will inspect the root breaks first.
2. I will trust your judgment.
3. If people are in danger, I choose them over the grove.

Vesper response to 1:
Good. Careful hands first. Steel only where the rot has already won.
```

This is the target tone: clear enough to guide play, specific enough to belong to the world, and personal enough to matter.
