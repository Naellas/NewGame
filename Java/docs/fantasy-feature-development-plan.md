# Echoes of Alderfall Fantasy Feature Development Plan

Generated: 2026-06-01

## Purpose

This plan expands Echoes of Alderfall from a strong Java/Swing RPG foundation into a more immersive fantasy adventure. It focuses on new features, player options, dialogue depth, quest arcs, asset generation, mobs, NPCs, and map expansion while preserving the current dependency-free Java 21/Swing setup.

The current codebase already supports:

- Class selection, skills, abilities, equipment, inventory, consumables, shops, crafting, and party members.
- A deterministic overworld with cities, villages, dungeons, interiors, adventure sites, location patches, weather, day/night, transitions, and props.
- Quests with defeat, gather, and visit objectives.
- NPC dialogue with personality, relationship changes, work topics, local rumors, and quest discussion.
- Village building, placement, workers, settlement stages, generated interiors, and save/load persistence.
- A Python asset pipeline under `Java/tools` for generating, slicing, normalizing, and importing PNG assets.

## Optimization Baseline To Preserve

The feature plan must be executed against the optimized codebase now present in the workspace. Future fantasy features should build on these changes instead of reintroducing older full-scan or UI-coupled patterns.

Current optimization foundations:

- `MapArea` owns prop storage through an indexed `props` list. Use `MapArea.addProp`, `removeProp`, `moveProp`, `propAt`, `propsAt`, and `propsInBounds` so the ordered draw list and tile index stay synchronized.
- `WorldMap.propAt`, `propsAt`, and `propsInBounds` delegate to the `MapArea` index. New feature code should call those APIs instead of scanning `world.props(mapId)` unless it truly needs every prop.
- `GamePanel.rebuildVisibleWorldProps(...)` uses `WorldMap.propsInBounds(...)` with a visibility margin. New rendering work must keep bounded prop queries intact.
- `Pathfinder` is already split out of `GamePanel`. New movement, click navigation, blocker, NPC, or quest-objective work should update `Pathfinder` or `GameState` rules rather than moving path logic back into `GamePanel`.
- `DebugMetrics` and `GameDiagnostics` exist for measuring config load, state creation, new-game setup, asset warmup, map count, prop count, and asset-cache summary.
- `AssetCatalog` now maintains indexed asset lookups, missing-asset tracking, duplicate stem counts, and cache summaries. New asset batches should preserve unique runtime asset stems where possible.
- `SmokeTest` already includes checks for the prop index, pathfinder behavior, overworld traversal, village placement, shops, recruits, quests, transitions, and interior placement.

Feature-development rule:

Every new content slice should keep these optimizations green. If a feature requires many new props, maps, NPCs, or assets, add the content through the optimized APIs first, then broaden diagnostics or smoke tests if the slice increases map density or pathing complexity.

## Creative Direction

Core fantasy promise:

The player is rebuilding Oathstead Camp into a living settlement while uncovering why Alderfall's old wards are failing. Each biome should feel like it has a local problem, a local magic, and a local cost.

Design pillars:

- Every location has a reason to exist beyond loot.
- NPCs remember tone, help, neglect, and faction choices.
- Quests should change places visually when completed.
- Mobs should belong to ecology, folklore, or faction, not only encounter tables.
- Asset batches should ship in themed sets so new content can be seen immediately.
- New systems should reuse existing quest, dialogue, village, battle, crafting, and map structures before adding larger abstractions.

## Phase 1 - Content Spine: Alderfall Rumor Board

Goal:

Add a repeatable, immersive way to discover quests without overloading individual NPCs.

Features:

- Add a notice board interaction in Oathstead Camp and major villages.
- Board entries surface nearby quests, biome rumors, bounty targets, and settlement requests.
- Board options:
  - "Local trouble" for nearby gather/visit quests.
  - "Dangerous marks" for named mob hunts.
  - "Trade needs" for crafting/village resource requests.
  - "Old stories" for lore hints that point toward ruins, caves, crypts, or castles.
- Add a small cooldown or rotation seed so rumors feel alive without needing live-service complexity.

Content:

- 20 short rumor entries split by biome: forest, marsh, desert, mountain, grave/crypt, road.
- 10 village request entries tied to Oathstead building plans and worker roles.
- 8 danger entries that name existing elite targets and future bosses.

Implementation touchpoints:

- `GameState`: board interaction state, available entries, acceptance flow.
- `GamePanel`: board overlay drawing and mouse/keyboard options.
- `GameData`: static board entries at first, later extract to a catalog class or JSON.
- `WorldMap`: connect board props to interactions in villages and player settlement through `MapArea.addProp` and indexed prop lookups.
- `SaveSystem`: save accepted board-generated quests if they become persistent.

Assets to generate:

- `village_prop_notice_board_upgraded`
- `village_prop_wanted_scrolls`
- `village_prop_weathered_map_table`
- 3 small parchment UI backgrounds for board entry categories.

Acceptance criteria:

- Player can inspect a board, select an entry, and receive a quest or map hint.
- Entries are biome-aware and do not repeat obviously in the same session.
- Build and smoke test pass.

## Phase 2 - Dialogue Depth: Relationship And Memory

Goal:

Make conversations feel less like menu branches and more like relationships.

Features:

- Extend NPC relationship memory from current dialogue deltas into visible relationship states: wary, neutral, friendly, trusted.
- Add relationship-gated choices:
  - Friendly NPCs offer extra rumors or lower shop prices.
  - Trusted NPCs reveal personal quests or recruit discounts.
  - Wary NPCs shorten answers until helped through a quest or respectful response.
- Add a "Follow up" dialogue option after completing a quest for the NPC.
- Add one personal secret, fear, or ambition per important named NPC.
- Add "Ask about Oathstead" option for recruitable NPCs and village workers.

Dialogue packs:

- Marla: medicine, grief, practical hope.
- Captain Torin: duty, border warnings, cost of command.
- Archivist Ren: missing records, old wards, forbidden maps.
- Mira Sunwarden: lantern magic, protection, faith under pressure.
- Vexa: wild magic, thorns, distrust of towns.
- Nyx/Sable: secrets, contracts, quiet exits.
- Imani/Asta/Lin/Vell: biome guardianship and local omens.

Implementation touchpoints:

- `DialogueLibrary`: add memory-aware nodes and reusable relationship-gated choice helpers.
- `GameState`: persist relationship changes per NPC key if not already fully persisted.
- `Npc`: keep current record shape if possible; derive identity from map/name/sprite.
- `SaveSystem`: include relationship map and completed personal flags.

Assets to generate:

- 8 NPC portrait busts or half-body dialogue cut-ins.
- 4 expression overlays per main companion: neutral, pleased, worried, stern.
- Optional small relationship icons for the dialogue UI.

Acceptance criteria:

- At least 6 important NPCs have relationship-gated lines.
- Completing a quest unlocks a follow-up line for its giver.
- Relationship state is saved and restored.

## Phase 3 - Quest Arc: The Wards Beneath Alderfall

Goal:

Add a main fantasy arc that links existing dungeons, locations, and settlement growth.

Arc summary:

Old wardstones under Alderfall are cracking. Each biome has a damaged ward tied to a local faction, monster ecology, and dungeon. Restoring the wards explains why Oathstead matters: the player settlement becomes the new anchor point.

Quest chain:

1. "Ashes Under Oathstead"
   - Visit objective at a newly revealed buried wardstone near Oathstead.
   - Introduces Archivist Ren and a camp ritual table.
2. "Three Names Missing"
   - Visit/gather objective in graveyard patches: tomb rubbings, broken charms, faded names.
   - Unlocks ward lore in Archive City.
3. "Lanterns for the Mire"
   - Gather marsh reeds, luminous lilies, and ward oil.
   - Defeat a marsh elite guarding a drowned shrine.
4. "Cairns That Walk"
   - Mountain route: inspect cairns, defeat an elemental guardian, retrieve frost-marked stone.
5. "The Sunken Toll"
   - Desert route: recover caravan seals, investigate glass tracks, fight a mirage-touched creature.
6. "Blackvault's Broken Key"
   - Dungeon crawl through Blackvault Ruins with a new boss and locked chamber prop.
7. "Oathstead's First Bell"
   - Settlement milestone quest: build a bell tower or ward hall, assign workers, perform ritual.
8. "The Root Below The Road"
   - Finale: multi-stage boss encounter beneath Oathstead, with allies joining based on relationship and settlement stage.

New quest objective needs:

- Multi-step quest state, or a chain of linked current `Quest` objects.
- Optional prerequisites: completed quest ids, settlement stage, player level, relationship threshold.
- Quest rewards beyond gold/XP: item unlocks, building unlocks, recruit availability, village stage progress.

Implementation touchpoints:

- Short term: represent the arc as linked quest ids in `GameData`.
- Medium term: add `prerequisiteQuestIds`, `unlockQuestIds`, and `rewardUnlocks` to `Quest`.
- `GameState`: quest unlock checks after completion and settlement changes.
- `WorldMap`: add or reveal objective sites near existing adventure sites.
- `SaveSystem`: persist unlocked quests and arc flags.

Assets to generate:

- `location_oathstead_buried_wardstone`
- `location_wardstone_cracked`
- `location_wardstone_restored`
- `location_ritual_bell`
- `interior_ward_hall_altar`
- `fx_ward_restore_anim`
- `fx_ward_crack_anim`

Acceptance criteria:

- The first 3 quests can be accepted, progressed, completed, and saved.
- Quest completion changes at least one world prop from cracked to restored.
- The arc unlocks one visible Oathstead building or village option.

## Phase 4 - Map Expansion: Named Biome Frontiers

Goal:

Make exploration feel broader without replacing the existing 300x300 overworld.

New explorable regions:

- Moonfen Reedmaze: marsh submap with plank paths, drowned lights, reed serpent encounters, and hidden healer NPC.
- Glasswind Expanse: desert submap with mirage paths, caravan wrecks, glass scorpion nests, and shade wells.
- Frosthollow Upper Pass: mountain/tundra submap with cairn routes, avalanches as blockers, frost creatures, and a hermit forge.
- Elderbough Deepwood: forest submap with living roots, old groves, thornling packs, and druidic shrines.
- Blackvault Lower Halls: dungeon expansion below existing Blackvault Ruins.

World structure:

- Keep the overworld stable.
- Add transitions from existing adventure sites into new submaps.
- Start each region as a deterministic `MapArea` with hand-authored landmarks and procedural decoration.
- Add local encounter tables per region instead of using only broad biome tables.

Implementation touchpoints:

- `WorldMap`: add map builder methods for each frontier, transitions, passability, props, and landmarks.
- `GameState`: route biome context and encounter tables by map id/kind.
- `Terrain`: add any new terrain chars only if existing chars cannot represent the area.
- `GamePanel`: verify world map overlay labels and transition prompts.
- `MapArea`: add all region props through `addProp`; use `propsInBounds` for any viewport or local-density queries.
- `Pathfinder`: update passability/blocker logic here if new region hazards affect click navigation.

Assets to generate:

- Terrain variants:
  - marsh dark water, marsh plank junctions, reed wall, luminous pool.
  - glass sand, cracked salt, mirage shimmer overlay.
  - snow cliff, icy path, avalanche rubble.
  - ancient root floor, moss ruin, canopy shadow.
- Props:
  - reed gate, drowned lantern, shade well, caravan ribs, frost cairn, avalanche marker, elder root arch, shrine stones.
- Transitions:
  - cave descent, ruin stair, root tunnel, mirage gate.

Acceptance criteria:

- Each frontier has an entrance, exit, local label, at least 3 landmark props, and at least 1 quest hook.
- Pathfinding and passability work in each new map.
- Save/load restores player position inside each new map.

## Phase 5 - Mobs, Bosses, And Encounter Identity

Goal:

Make enemies feel like local fantasy threats with behaviors, drops, and quest roles.

New mob families:

- Ward-touched constructs:
  - Cracked Wardling: low-level elemental scout.
  - Runebound Sentinel: defensive construct with guard/status ability.
  - Bellstone Colossus: boss for Oathstead finale.
- Marsh spirits:
  - Wicklight: fragile caster that blinds or drains MP.
  - Drowned Reedling: plant/spirit hybrid with poison or root.
  - Mire Choir: elite multi-target sonic caster.
- Desert mirage beasts:
  - Glass Moth: evasive creature with shimmer defense.
  - Saltshade: stealth attacker with delayed strike.
  - Mirage Drake: boss with fire and illusion.
- Mountain oathbreakers:
  - Frostbound Miner: undead worker with armor.
  - Cairn Watcher: stone elemental with slow heavy hits.
  - Whiteout Hag: caster boss with frost and silence.
- Deepwood fae/plant threats:
  - Root Imp: fast nuisance attacker.
  - Briar Knight: armored plant warrior.
  - Elderbough Heart: boss that summons thorns.

Combat feature upgrades:

- Add enemy ability tags visible in battle log: poison, burn, frost, ward, blind, silence, root.
- Add drops linked to crafting and village upgrades.
- Add rare named variants with stronger stats and unique rewards.
- Add simple boss phases at HP thresholds using current battle turn structure before building a full phase engine.

Implementation touchpoints:

- `GameData.MONSTERS`: add specs.
- `MonsterAbilities`: add ability definitions and assignment rules.
- `Battle`: support any new status behavior or boss threshold callbacks.
- `CraftingSystem`: add drops as recipe ingredients.
- `AssetStore`: ensure animations can be found by sprite key.

Assets to generate:

- 20 monster sprites, prioritized as 4-direction or battle-facing first.
- 8 attack animations:
  - ward pulse, reed lash, mirage flare, frost breath, thorn summon, bell shockwave, shadow blink, poison spit.
- 5 boss portraits or large battle sprites.

Acceptance criteria:

- At least 10 new mobs appear in correct maps/biomes.
- At least 4 new enemy abilities are visible in combat logs.
- At least 3 drops are used in recipes or village upgrades.

## Phase 6 - NPCs, Companions, And Settlement Life

Goal:

Make Oathstead and the frontier towns feel inhabited by people with jobs, desires, and conflicts.

New NPCs:

- Ser Caldus Bellwright: ward-bell craftsman; unlocks bell tower/ward hall.
- Nera Reed-Sister: marsh healer; recruitable Grovekeeper variant.
- Thane Varric Snowmend: mountain smith; sells frost gear and unlocks forge upgrades.
- Zahir of the Empty Canteen: desert guide; teaches mirage-safe routes.
- Maelin Rootscribe: deepwood scholar; links forest magic to the main arc.
- Sister Ivara: Archive City ward-priest; interprets restored ward names.
- Tallow: nervous camp cook; settlement comfort quests.
- Brin and Wessa: sibling haulers; worker/tutorial quest for production assignments.

Companion options:

- Bellwright Defender: tank/support with ward abilities.
- Reed-Sister: healer/status cleanse.
- Mirage Guide: rogue/scout with evasion and reveal.
- Rootscribe: mage/support with thorn and lore bonuses.

Settlement life features:

- Ambient villagers walk to workstations by role.
- Small daily lines change based on weather, stage, and recent quests.
- Workers occasionally request supplies for a temporary production bonus.
- Buildings at higher levels add visible prop upgrades.

Implementation touchpoints:

- `GameData.NPCS` and `RECRUITS`: add NPCs and recruit specs.
- `VillageManager`: add building plans and worker role hooks as needed.
- `WorldMap`: place new NPCs and stage-based props.
- `DialogueLibrary`: personal lines and Oathstead-specific discussion.
- `GameState`: worker request generation and turn-in flow.

Assets to generate:

- 8 NPC directional sprites with walk animations.
- 4 recruit battle sprites or directional animation sets.
- 12 settlement life props: laundry, stew pot, ledger desk, ward bell frame, apprentices' tools, drying herbs, practice circle.

Acceptance criteria:

- At least 4 new named NPCs appear in game and have unique dialogue.
- At least 2 new recruitable companions can join through quest/reputation/hire flow.
- Settlement stage visually changes one building or prop cluster.

## Phase 7 - Player Options And Immersion Settings

Goal:

Give players more ways to shape their fantasy role and tune the experience.

Gameplay options:

- Dialogue tone preference:
  - Direct, gentle, joking, cautious.
  - Used only to preselect or highlight choices, not to auto-play conversations.
- Quest guidance:
  - Full markers, subtle hints, or no markers.
- Encounter pace:
  - Relaxed, standard, dangerous.
- Weather intensity:
  - Light, standard, dramatic.
- Text speed:
  - Instant, quick, standard.

Roleplay options:

- Player origin:
  - Roadwarden, failed apprentice, deserter, hedge healer, ruined noble.
- Origin gives:
  - One starting line in intro.
  - One minor item.
  - A small relationship modifier with one NPC personality type.
- Camp banner choice:
  - Stag, lantern, thorn, bell, river.
  - Changes Oathstead banner props and save summary flavor.

Implementation touchpoints:

- `GameConfig`: settings persistence.
- `GameState`: origin/banner flags and intro text.
- `GamePanel`: settings and new adventure UI.
- `SaveSystem`: persist origin, banner, marker preferences.

Assets to generate:

- 5 banner props and small UI icons.
- 5 origin icons.
- Optional title/new-adventure parchment background refresh.

Acceptance criteria:

- Player can select origin and banner during new adventure.
- Settings persist and affect markers/weather/dialogue presentation.
- Existing saves load with sensible defaults.

## Phase 8 - Asset Generation Pipeline

Goal:

Make generated content consistent, repeatable, and easy to review.

Workflow:

1. Create a content batch file before generating assets:
   - asset key
   - target folder
   - intended size
   - style notes
   - map/quest/NPC owner
2. Generate raw sheets or images.
3. Slice/normalize with `Java/tools`.
4. Run fringe cleanup where needed.
5. Add assets under the closest existing folder:
   - character animation subfolders such as `Java/assets/player/classes/mage/animations`,
     `Java/assets/companions/aria/animations`, or `Java/assets/npcs/animations`
   - `Java/assets/effects`
   - `Java/assets/interiors`
   - `Java/assets/locations`
   - biome folders such as `forest`, `desert`, `tundra`, `water`, `terrain`
6. Add asset keys to the relevant content registry.
7. Verify in-game at 1x and current zoom levels.

Suggested batch order:

- Batch A: wardstone props, board props, Oathstead banners, ritual effects.
- Batch B: 8 NPCs and 4 recruit animation sets.
- Batch C: frontier terrain and landmark props.
- Batch D: 10 common mobs and 4 combat effects.
- Batch E: bosses, boss effects, upgraded buildings, finale props.

Quality rules:

- Keep pixel scale compatible with existing assets.
- Prefer transparent PNGs for props, sprites, and effects.
- Name assets by content role, not by generation source.
- Include fallback-safe names so `AssetStore` resolves them consistently.
- Keep runtime asset stems unique where possible so `AssetCatalog` duplicate-stem diagnostics stay useful.
- After each asset batch, run diagnostics and review `assetCache` for missing or duplicate stems.
- Add before/after preview assets only in a temporary review folder or with clear `_preview` suffixes.

## Phase 9 - Data And Code Organization

Goal:

Keep content growth from turning `GameData`, `WorldMap`, and `GamePanel` into harder-to-change files.

Recommended sequence:

1. Add new content in existing registries for the first vertical slice.
2. Extract focused catalog classes once patterns settle:
   - `QuestCatalog`
   - `NpcCatalog`
   - `MonsterCatalog`
   - `BoardCatalog`
   - `RegionCatalog`
3. Add small tests or smoke-test checks for:
   - quest unlock flow
   - objective completion
   - relationship persistence
   - new map transitions
   - new monster ability lookup
4. Move stable content groups to JSON only after the Java catalog shape is clear.

Avoid early:

- Full renderer migration.
- Large save-format rewrite without migration.
- Replacing quest and dialogue systems before proving the new content needs it.
- Generating a huge asset batch before one region is playable.

## Vertical Slice Recommendation

Build this first:

1. Add Notice Board in Oathstead Camp.
2. Add first arc quest, "Ashes Under Oathstead."
3. Add cracked wardstone prop near camp.
4. Add Ser Caldus Bellwright NPC.
5. Add one new mob, Cracked Wardling.
6. Add one new battle effect, ward pulse.
7. Add one settlement unlock, Ward Bell Frame.
8. Add two relationship-gated dialogue lines for Caldus and Ren.
9. Add save/load coverage for the new quest and any new flags.

This slice proves the core expansion loop:

discover rumor -> talk to NPC -> inspect world object -> fight themed mob -> complete quest -> world/settlement changes -> relationship deepens.

## Step-By-Step Execution Plan

Use this section as the practical build order. Complete one step, verify it, then move to the next. The goal is to keep every change playable instead of accumulating a huge content branch that cannot be tested until the end.

This execution plan assumes the optimized systems listed in **Optimization Baseline To Preserve** are already present. Do not repeat completed optimization tasks such as extracting `Pathfinder`, adding `MapArea` prop indexing, or replacing visible prop scans.

### Step 0 - Baseline The Current Game

Do this before changing gameplay code:

1. Run the build.

   ```powershell
   powershell -ExecutionPolicy Bypass -File Java\scripts\build.ps1
   ```

2. Run the smoke test.

   ```powershell
   powershell -ExecutionPolicy Bypass -File Java\scripts\smoke-test.ps1
   ```

3. Run the diagnostics entry point after build output exists.

   ```powershell
   java -cp Java\out com.alderfall.game.GameDiagnostics
   ```

4. Record these diagnostics before adding a content slice:
   - `stateCreate`
   - `newGameSetup`
   - `assetWarmup`
   - `maps`
   - `props`
   - `assetCache`

5. Start the game and manually check:
   - new adventure
   - movement
   - talk/interact
   - quest log
   - battle
   - save/load
   - village screen

6. Record any existing bug separately so new feature work does not inherit unclear failures.

Exit criteria:

- Build passes.
- Smoke test passes.
- Diagnostics run and produce map/prop/asset-cache counts.
- You know whether the current game has any unrelated broken behavior.

### Step 1 - Add Content Keys And Asset Names First

Create the names before creating the systems. This keeps later code changes concrete.

1. Decide the first vertical slice content:
   - Quest: `ashes_under_oathstead`
   - NPC: Ser Caldus Bellwright
   - Mob: Cracked Wardling
   - Prop: `location_oathstead_buried_wardstone`
   - Settlement prop: `village_prop_ward_bell_frame`
   - Effect: `fx_ward_pulse`

2. Add placeholder notes to the plan or a temporary content checklist:
   - display name
   - asset key
   - target folder
   - first map or NPC owner
   - completion reward

3. Confirm asset naming matches existing conventions:
   - location props use `location_*`
   - village props use `village_prop_*`
   - effects use `fx_*`
   - NPC sprites use `npc_*`
   - monsters use short combat sprite keys, then animation keys if needed.

Exit criteria:

- Every first-slice feature has a stable key.
- No code has to guess later names.

### Step 2 - Generate Or Stub The First Asset Batch

Start with the minimum visible assets needed for the vertical slice.

1. Create or generate:
   - `location_oathstead_buried_wardstone.png`
   - `location_wardstone_cracked.png`
   - `location_wardstone_restored.png`
   - `village_prop_ward_bell_frame.png`
   - `fx_ward_pulse.png` or `fx_ward_pulse_anim.png`
   - `npc_caldus_bellwright.png`
   - `cracked_wardling.png`

2. Place assets in likely folders:
   - location props: `Java/assets/locations` if present, otherwise the closest existing location/props folder.
   - village props: `Java/assets/village` or existing village prop folder.
   - effects: `Java/assets/effects`.
   - NPC/monster animation sheets: the owner domain's `animations` folder.

3. If generated sheets need slicing, run the relevant existing tool from `Java/`:

   ```powershell
   python tools/regenerate_universal_assets.py --verify
   ```

4. Keep any rough preview files clearly named with `_preview` or outside runtime folders.

5. Run diagnostics and check the asset cache summary after adding runtime assets:

   ```powershell
   java -cp Java\out com.alderfall.game.GameDiagnostics
   ```

6. If `duplicateStems` increases, confirm the duplicate is intentional. If `missing` increases after visiting the new scene, add or rename the missing asset.

Exit criteria:

- Runtime assets exist with final keys.
- The game can fall back gracefully if an animation sheet is not ready yet.
- Asset diagnostics do not show accidental duplicate stems or avoidable missing assets.

### Step 3 - Add The First Quest Data

Use the current `Quest` model before designing a larger quest system.

1. Add `ashes_under_oathstead` to `GameData.QUESTS`.

2. Make it a `VISIT` quest first:
   - target: "Buried Wardstone"
   - needed: 1
   - objective location kind: a new or existing Oathstead/camp-adjacent location kind
   - objective asset: `location_oathstead_buried_wardstone`
   - reward: modest gold/XP plus future unlock flag later.

3. Add four quest lines:
   - start dialog
   - progress dialog
   - ready dialog
   - complete dialog

4. Build and smoke test.

Exit criteria:

- Quest compiles.
- Quest can be accepted by an NPC or test hook.
- Quest appears correctly in the quest log.

### Step 4 - Place The Quest Giver

Add Ser Caldus as a real NPC before adding the notice board.

1. Add Caldus to the relevant NPC list in `GameData`.

2. Place him in Oathstead Camp if player-settlement NPC placement supports it; otherwise place him in the nearest stable city/village for the first slice.

3. Assign:
   - sprite: `npc_caldus_bellwright`
   - quest id: `ashes_under_oathstead`
   - shop id: none for now
   - recruit id: none for now
   - two normal dialogue lines.

4. Add Caldus-specific personality/dialogue in `DialogueLibrary` only if the generic dialogue feels too thin.

5. Build, run, and talk to Caldus.

Exit criteria:

- Caldus appears on the map.
- Player can talk to him.
- Player can accept the quest from him.

### Step 5 - Add The World Objective

Make the wardstone a real interactible objective.

1. Add or reuse a location patch/objective site near Oathstead in `WorldMap`.

2. Ensure `GameState` can resolve the quest objective to a tile and prop.

3. Place the `location_oathstead_buried_wardstone` prop through `MapArea.addProp(...)`, not by mutating backing collections directly.

4. On interaction, call the existing visit-objective progress path.

5. Add a completion visual change:
   - before completion: cracked/buried wardstone
   - after completion: restored or revealed wardstone

6. Save before interacting, load, and confirm the objective is still present.

7. Interact, complete objective, save, load, and confirm completed state stays completed.

8. Add or update smoke-test coverage if the new objective introduces a new location kind, blocker behavior, or persistent world-state flag.

Exit criteria:

- Player can find and inspect the wardstone.
- Quest progress reaches ready state.
- Save/load preserves objective state.
- Prop lookup for the objective uses `WorldMap.propAt`, `propsAt`, or `propsInBounds`, not a full-map scan.

### Step 6 - Add The First Mob And Encounter Hook

Add Cracked Wardling after the visit objective works.

1. Add a monster spec in `GameData.MONSTERS`.

2. Add one ability in `MonsterAbilities`:
   - name: Ward Pulse
   - effect key: `fx_ward_pulse`
   - behavior: low damage or defense debuff.

3. Add it to the appropriate encounter table or quest-specific encounter trigger.

4. If the current quest remains `VISIT`, create a second linked quest or board bounty that defeats one Cracked Wardling.

5. Build, run, and fight the mob.

Exit criteria:

- Cracked Wardling appears only in intended areas or quest context.
- Its ability appears in the battle log.
- Combat completes and rewards correctly.

### Step 7 - Add Notice Board Discovery

Only add the board after one quest and one NPC work.

1. Reuse the existing `player_village_quest_board` prop in Oathstead Camp before adding another board asset.

2. Add a simple board entry catalog:
   - id
   - title
   - category
   - body text
   - optional quest id
   - optional required settlement stage
   - optional required completed quest.

3. Start with three entries:
   - "Ashes Under Oathstead"
   - "Strange Pulse Near Camp"
   - "Supplies For The Bell Frame"

4. Add a board overlay in `GamePanel`.

5. Add board state and selection handling in `GameState`.

6. Use the board to point the player to Caldus or directly accept a small quest.

7. Keep board proximity checks on indexed prop APIs. `settlementBoardNearPlayer()` can continue using the current prop API, but new general board checks should not scan every prop on dense maps.

8. Build and smoke test.

Exit criteria:

- Player can open the board.
- Board lists readable entries.
- At least one entry starts or points to the first quest.

### Step 8 - Add Settlement Unlock

Tie quest completion to Oathstead growth.

1. Add `village_prop_ward_bell_frame` to `VillageManager.OUTDOOR_ASSETS`.

2. Add a new building or upgrade only if the prop is not enough:
   - Ward Hall
   - Bell Tower
   - Shrine Court.

3. After completing `ashes_under_oathstead`, unlock the prop/building.

4. If the current data model cannot lock assets, add a small unlock check before display.

5. Place the prop in the village and save/load.

Exit criteria:

- Completing the quest visibly unlocks something in Oathstead.
- The unlock is saved.
- Existing village placement still works.

### Step 9 - Add Relationship Follow-Up

Make the content feel personal.

1. Add a post-completion dialogue line for Caldus.

2. Add one friendly/trusted line for Archivist Ren about the wardstone.

3. Confirm relationship changes from dialogue choices are persisted.

4. Add one practical reward for good relationship if easy:
   - discount
   - extra rumor
   - companion hint
   - small item gift.

Exit criteria:

- Completed quest changes at least one NPC conversation.
- Friendly/trusted state produces a visibly different line or option.

### Step 10 - Expand Into The First Frontier Map

After the first slice works, expand one region, not all at once.

Recommended first frontier:

- Elderbough Deepwood if you want magical forest/fantasy tone first.
- Moonfen Reedmaze if you want mystery and atmosphere first.

Execution:

1. Add a deterministic `MapArea` builder in `WorldMap`.

2. Add transition from an existing overworld location.

3. Add local props with `MapArea.addProp(...)` and landmarks through the map area's landmark map.

4. Add one NPC, one local quest, and two local mobs.

5. Add local encounter rules.

6. Save/load inside the new map.

7. Run diagnostics and compare `maps`, `props`, and `newGameSetup` against the Step 0 baseline.

8. If the new region substantially increases prop count, manually verify viewport rendering still uses `propsInBounds` and remains responsive.

Exit criteria:

- The new map is reachable, readable, and escapable.
- It contains one complete quest loop.
- It has distinct local assets and enemies.
- Diagnostics show expected map/prop growth without an unexplained startup spike.

### Step 11 - Repeat Content Packs In Small Loops

For every later phase, use this order:

1. Define keys.
2. Generate/stub assets.
3. Add static data.
4. Place content in maps through optimized map/prop APIs.
5. Add dialogue.
6. Add quest/reward.
7. Add save/load fields if needed.
8. Add or update diagnostics/smoke checks for new density, pathing, or persistence.
9. Build.
10. Smoke test.
11. Run diagnostics.
12. Manual playtest.

Do not start the next pack until the current pack is playable.

### Step 12 - Refactor Only After Patterns Repeat

Some optimization refactors are already complete. Do not redo them:

- `Pathfinder` is already extracted.
- `MapArea` already owns the prop index.
- `WorldMap.propsInBounds(...)` already exists.
- `DebugMetrics` and `GameDiagnostics` already exist.

Wait until at least two content packs are implemented before extracting additional content or renderer classes.

1. If `GameData` becomes hard to scan, extract:
   - `QuestCatalog`
   - `NpcCatalog`
   - `MonsterCatalog`
   - `BoardCatalog`

2. If `WorldMap` gains too many region builders, extract:
   - `RegionBuilder`
   - `AdventureSiteBuilder`
   - `LocationPatchCatalog`

3. If `GamePanel` board/dialogue UI grows too large, extract:
   - `BoardRenderer`
   - `DialogueRenderer`
   - `QuestLogRenderer`

4. If new feature work changes movement blockers, update `Pathfinder` and add smoke-test coverage there rather than moving pathing back into `GamePanel`.

5. After each extraction, run build, smoke test, and diagnostics before adding more content.

Exit criteria:

- Refactors do not change gameplay.
- New content remains easier to add after the extraction.

## Backlog Summary

High priority:

- Notice board and rumor entries.
- Main ward quest chain, first 3 quests.
- Relationship memory and post-quest dialogue.
- Oathstead wardstone and bell assets.
- First frontier map: Moonfen Reedmaze or Elderbough Deepwood.
- First 10 new mobs and 4 enemy abilities.

Medium priority:

- Player origin and banner options.
- Settlement life requests and worker flavor lines.
- Additional companions and recruit quests.
- Boss phase support.
- Catalog extraction from `GameData`.

Later:

- Full JSON data migration.
- Larger renderer migration to libGDX.
- Procedural dialogue grammar beyond curated line pools.
- Multi-floor dungeon scripting and complex puzzles.

## Verification Checklist

For every feature/content batch:

- Build: `powershell -ExecutionPolicy Bypass -File Java\scripts\build.ps1`
- Smoke test: `powershell -ExecutionPolicy Bypass -File Java\scripts\smoke-test.ps1`
- Diagnostics: `java -cp Java\out com.alderfall.game.GameDiagnostics`
- Manual checks:
  - new map entrance/exit
  - quest accept/progress/complete/reward
  - save/load before and after quest completion
  - dialogue options with at least two relationship states
  - battle with each new mob ability
  - asset rendering at normal and zoomed views
  - no missing asset placeholders in intended scenes
  - no direct full-map prop scans added for viewport, interaction, or placement logic
  - pathfinding still routes around NPCs and blocking quest objectives
  - diagnostics map/prop/cache counts changed only for expected reasons
