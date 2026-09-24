# Echoes of Alderfall: folders and class responsibilities

Reviewed 2026-09-23 against the active D: working tree. Paths below are relative
to the repository, so this guide remains usable after relocation. The active game
is **Java/**, a dependency-free Java 21/Swing application built with PowerShell and
`javac`. This documents the current implementation, not a migration proposal.

Start with `Main` → `GameWindow` → `GamePanel`. The panel coordinates presentation
and input; `GameState` owns gameplay state; `WorldMap` owns the world and map rules;
`Battle` resolves combat. Renderers consume that state, `AssetCatalog` locates art,
`AssetStore` decodes/crops/scales it, and `SaveSystem` persists adventures.

## Repository and runtime folders

| Folder | Responsibility / usage |
| --- | --- |
| `Java/` | Active game, runtime assets, configuration, documentation and development tooling. |
| `Java/src/main/java/` | Production Java code. Every normal build includes this root. |
| `Java/src/test/java/` | Assertion-based diagnostics; compiled with `-IncludeTests`. |
| `Java/src/review/java/` | Visual exporters, audits and benchmarks; compiled with `-IncludeReviews`. |
| `Java/src/testSupport/java/` | Shared development-only support (`CharacterAnimationAudit`), included by either development switch. |
| `Java/assets/` | Accepted runtime PNGs, animation metadata, music and SFX. See the asset table below. |
| `Java/config/` | Gameplay tuning, asset routing/placement, permitted duplicate stems and hygiene baseline. `settings.properties` is private local configuration. |
| `Java/scripts/` | Build/run/test/check launchers and named development commands. |
| `Java/tools/assets/` | Reusable image import/extraction/generation tools grouped by characters, monsters, environments, items and shared helpers. |
| `Java/tools/audio/` | Audio preparation/generation tools. |
| `Java/tools/checks/` | Repository policy, structure and review/archive validation. |
| `Java/tools/tests/` | Python tool and policy tests. |
| `Java/tools/reviews/` | Interactive HTML review sites and related experimental studies. |
| `Java/docs/` | Architecture/content documentation, plans and audit reports. Plans are not automatically implemented behavior. |
| `Java/out/` | Normal compiled game output; generated, not source. |
| `Java/temp/<task>/` | Ignored temporary probes, alternate compilation and intermediate captures. |
| `Java/saves/` | Private local adventure saves. |
| `Java/exports/` | Existing generated exports; new disposable output belongs in `Java/temp/<task>/`. |
| `Java/archive/pending-deletion/<batch>/` | Quarantined retirement candidates with original paths, reasons and checksum manifests; not a runtime dependency. |
| `art-source/<topic>/<batch>/` | New originals, generation prompts, import recipes and provenance. |
| `asset-review/reviews/<topic>/<batch>/` | Curated evidence/captures with reproduction and acceptance README. |
| `asset-review/characters/index.html` | Legacy bookmark redirect to the current review site. |
| `Story premise/` | Narrative source/reference material. |
| `.github/workflows/` | CI workflow definitions, including repository hygiene. |
| Root `assets/`, `out/`, `saves/`, `temp/`; `Java/out-*` | Legacy/existing locations, not destinations for new work. Do not assume their contents are disposable without auditing callers/history. |

Root `AGENTS.md` defines placement and validation rules. Read
[source-root rules](../src/README.md), [tool commands](../tools/INDEX.md), and
[enforcement details](agent-guidelines-and-enforcement.md) before extending them.
Python tools run through `python Java/tools/run.py NAME [arguments]`; the registry
is `Java/tools/tool-index.json`, paths are shared through `project_paths.py` and
`tools/assets/shared/asset_paths.py`.

## Runtime assets

All paths in this table are under `Java/assets/`. Asset filenames are stable IDs;
changing storage or removing identical bytes can still break an ID or animation lookup.

| Subtree | Responsibility |
| --- | --- |
| `characters/player/` | Base player and class art; class-owned animations. |
| `characters/companions/` | Companion-owned sprites, portraits and animations. |
| `characters/npcs/` | Townsfolk, regional variants and story NPCs. |
| `characters/monsters/` | Monster art and animation strips. |
| `characters/shared/` | Existing cross-roster animation batches. |
| `environments/terrain/` | Common terrain, biome-specific surfaces and roads. |
| `environments/settlements/city/buildings/<region>/` | Building art for sun, river, north, freehold, fen, hearth and shared styles; distinct town IDs within the region. |
| `environments/settlements/city/{props,walls,terrain,landmarks,folklore,overworld}/` | Settlement decoration, wall/gate modules, surfaces, local identities and map icons. |
| `environments/settlements/player_village/` | Player settlement construction art. |
| `environments/locations/quest/` | Quest-location art. |
| `environments/interiors/props/` | Furniture, appliances/workstations and household decoration. |
| `environments/props/nature/` | Plants, ground detail and environmental props. |
| `environments/battle/` | Combat environments/backdrops. |
| `items/weapons/<type>/` | Swords, staves, bows and other weapon families. |
| `items/{armors,helmets,gloves,belts,leggings,boots,shoulders,shields,accessories}/` | Equipment grouped by wearable category. |
| `items/{consumables,materials,tools,placeables,icons,atlases}/` | Other item sprites and shared source atlases used at runtime. |
| `effects/{animations,weather}/` | Effect strips and weather art. |
| `music/`, `sfx/` | Runtime audio. |
| `source/` | Grandfathered originals only; new originals belong at root `art-source/`. |

Keep `.png`, `.frames` and `.framebounds` animation companions together. See
[asset conventions](../assets/README.md) and [building/item layout](building-and-item-layout.md).

## Where to change what

| Task | First places to inspect |
| --- | --- |
| Town building appearance or scale | `TownBuildingArt`, `AssetStore`, `GamePanel`; [town architecture](town-architecture.md). |
| Buildings, doors, walls, gates or blocked paths | `WorldMap`, `BuildingGeometry`, `PropCollision`, `RegionalSettlementGenerator`; rendering in `WorldRenderer`/`GamePanel`. |
| Regional town identity and building roles | `RegionalSettlementIdentity`, `RegionalBuildingTypes`, folklore content classes. |
| NPC routine/path or walking appearance | `AmbientNpcAi`, `TownNpcNavigation`, `LocomotionClock`, `WorldCharacterAnimation`, walking-motion classes. |
| Terrain, roads, water or lighting | Generators in `map/`; `LayeredTerrainRenderer`, `RoadSurface`, `WaterTileRenderer`, helpers in `render/world/`. |
| Item stats, loot, icons or crafting | `EquipmentCatalog`, `ItemCatalog`, `MaterialCatalog`; `ItemAppearance`; `CraftingSystem`, `CraftingCalculation`, `AssemblyCrafting`. |
| Quest or conversation | `Quest`, `GameState`, narrative/content classes and `DialogueLibrary`/named companion dialogue classes. |
| Combat rules versus combat appearance | Rules: `Battle`, ability/status models. Appearance: `BattleRenderer`, VFX classes, `BattleFormation`, `BattleScenery`. |
| Inventory/shop/menu screen | Relevant renderer in `ui/`; `GamePanel` and `GameKeyboardController` for integration/input. |
| Saves or progression | `SaveSystem`, `GameState`, `Actor`; verify existing-save restoration. |
| Asset import or repository organization | Existing command in `tools/INDEX.md`, shared routing helpers, `AGENTS.md` and checks. |

## Source folders and package boundaries

The following folders are under `Java/src/main/java/com/alderfall/game/`.
**A folder name does not necessarily name a Java package.** Much code was moved
into subject folders without changing `package com.alderfall.game;`. Other classes
use actual `camera`, `inventory`, `map`, `render`, `render.battle`, `render.world`
and `ui` packages. Check each declaration before changing imports or launching a
class. A duplicate fully qualified class remains a duplicate even in another folder.

`rendering/` contains many root-package presentation helpers; `render/` contains
extracted rendering packages. `worldmodel/` contains shared models/collision while
`map/` contains generation/map storage as well as map UI. `content/` defines much
authored data, whereas `state/` and `systems/` execute gameplay. These are useful
ownership conventions, not fully enforced architectural boundaries.

| Source folder | Files | Responsibility |
| --- | ---: | --- |
| `app/` | 6 | Application startup, Swing window/panel, keyboard routing, configuration and game modes. |
| `audio/` | 3 | Audio playback and selection of cues from gameplay state. |
| `camera/` | 3 | World camera modes, follow behavior and viewport data. |
| `combat/` | 18 | Turn-based combat rules, abilities/statuses, formations and battle presentation. |
| `content/` | 30 | Authored/static definitions: items, equipment, materials, quests, culture, places and regional identities. |
| `dialogues/` | 13 | Conversation library, companion-specific writing and ambient town exchanges. |
| `entities/` | 11 | Actors/NPCs, schedules, navigation and world-character animation. |
| `events/` | 1 | Gameplay workflows for interactive roaming events. |
| `inventory/` | 7 | Item/equipment models and inventory drag/drop support. |
| `map/` | 20 | World/map generation and storage, interiors, location dressing and world-map UI. |
| `minigames/` | 1 | Camp-defense gameplay. |
| `render/battle/` | 2 | Extracted combat visual-effect drawing. |
| `render/world/` | 6 | Extracted terrain details, atmosphere, lights and prop drawing. |
| `render/` | 1 | World-map terrain cache; extracted world and battle rendering packages below. |
| `rendering/` | 29 | Shared image loading/caching, terrain/building rendering, motion layers and render utilities. |
| `state/` | 1 | Central mutable gameplay state and orchestration. |
| `systems/` | 9 | Persistence, crafting/assembly, village management, chests, equipment hooks and weather. |
| `ui/` | 20 | Screen-specific renderers and interaction helpers; dialogue figure animation. |
| `uiwidgets/` | 2 | Small reusable button and tooltip data types. |
| `worldmodel/` | 18 | World records, terrain/settings identities, collision, pathfinding and narration. |

## Production class/file index

All **201 production Java source files**, grouped by their current folder.
Each row links to the source; classes, records and enums are included. Nested types
remain documented with their owning file rather than counted as separate files.

### app/

Application startup, Swing window/panel, keyboard routing, configuration and game modes.

| Source | Responsibility |
| --- | --- |
| [GameConfig](../src/main/java/com/alderfall/game/app/GameConfig.java) | Static dimensions/timing constants plus tunable encounter/audio settings and config persistence. |
| [GameKeyboardController](../src/main/java/com/alderfall/game/app/GameKeyboardController.java) | Routes keyboard presses, releases and typed text to the current game mode and UI; tracks held keys. |
| [GameMode](../src/main/java/com/alderfall/game/app/GameMode.java) | Enumerates UI/game modes such as menu, explore, battle, dialog, skills, inventory, village, shop, settings, and save menu. |
| [GamePanel](../src/main/java/com/alderfall/game/app/GamePanel.java) | Swing panel, fixed-step game loop, rendering, input handling, UI overlays, camera, animation visuals, audio triggers, and interaction hit zones. |
| [GameWindow](../src/main/java/com/alderfall/game/app/GameWindow.java) | Swing frame setup, fullscreen toggle, panel installation, and shutdown handling. |
| [Main](../src/main/java/com/alderfall/game/app/Main.java) | Program entry point. |

### audio/

Audio playback and selection of cues from gameplay state.

| Source | Responsibility |
| --- | --- |
| [GameAudioController](../src/main/java/com/alderfall/game/audio/GameAudioController.java) | Selects music for game modes/regions and triggers gameplay sound effects at configured volumes. |
| [MusicManager](../src/main/java/com/alderfall/game/audio/MusicManager.java) | Loads, loops, cross-selects, and shuts down music clips. |
| [SoundManager](../src/main/java/com/alderfall/game/audio/SoundManager.java) | Loads, plays, and volume-scales short sound effects. |

### camera/

World camera modes, follow behavior and viewport data.

| Source | Responsibility |
| --- | --- |
| [CameraController](../src/main/java/com/alderfall/game/camera/CameraController.java) | Updates follow/dead-zone camera movement, clamps its position and resets to the player. |
| [CameraMode](../src/main/java/com/alderfall/game/camera/CameraMode.java) | Camera behavior choices and configuration-key lookup. |
| [CameraView](../src/main/java/com/alderfall/game/camera/CameraView.java) | Camera x/y position shared with drawing code. |

### combat/

Turn-based combat rules, abilities/statuses, formations and battle presentation.

| Source | Responsibility |
| --- | --- |
| [Ability](../src/main/java/com/alderfall/game/combat/Ability.java) | Describes a player ability: name, power, MP cost, kind, and target profile. |
| [AbilityResolutionStep](../src/main/java/com/alderfall/game/combat/AbilityResolutionStep.java) | Ability-resolution snapshot: step index/count, source/target actors, visual mode, animation stage and progress. |
| [AbilityStatus](../src/main/java/com/alderfall/game/combat/AbilityStatus.java) | Describes a status effect that an ability may apply, including target and chance. |
| [Battle](../src/main/java/com/alderfall/game/combat/Battle.java) | Turn-based battle engine for party/enemy combat, statuses, target selection, rewards, logs, and animation timing. |
| [BattleActionAnimation](../src/main/java/com/alderfall/game/combat/BattleActionAnimation.java) | Tracks combat animation progression from windup through release and recovery. |
| [BattleActorPose](../src/main/java/com/alderfall/game/combat/BattleActorPose.java) | Stores combat render offsets, rotation, and scale for animated actors. |
| [BattleFormation](../src/main/java/com/alderfall/game/combat/BattleFormation.java) | Shared combat foot positions and actor sizes for drawing, hit targets and effects. |
| [BattleRenderer](../src/main/java/com/alderfall/game/combat/BattleRenderer.java) | Draws battle actors, poses and the combat presentation. |
| [BattleScenery](../src/main/java/com/alderfall/game/combat/BattleScenery.java) | Selects encounter backdrops from location identity without consuming combat randomness. |
| [ClassAbilityVfx](../src/main/java/com/alderfall/game/combat/ClassAbilityVfx.java) | Maps named player abilities to presentation profiles while preserving gameplay effect keys. |
| [EffectStack](../src/main/java/com/alderfall/game/combat/EffectStack.java) | Runtime stack of a status effect with duration and power. |
| [MonsterAbilities](../src/main/java/com/alderfall/game/combat/MonsterAbilities.java) | Registry and helper logic for monster-specific abilities. |
| [MonsterAbility](../src/main/java/com/alderfall/game/combat/MonsterAbility.java) | Defines enemy ability behavior, power, chance, target, cooldown, status payloads, and effect key. |
| [MonsterAbilityStatus](../src/main/java/com/alderfall/game/combat/MonsterAbilityStatus.java) | Status application payload for monster abilities. |
| [MonsterAbilityVfx](../src/main/java/com/alderfall/game/combat/MonsterAbilityVfx.java) | Enemy ability visual profiles and drawing, separate from damage/status rules. |
| [MonsterAttackAnimations](../src/main/java/com/alderfall/game/combat/MonsterAttackAnimations.java) | Selects authored enemy attack strips and supported attack cycles. |
| [StatusEffect](../src/main/java/com/alderfall/game/combat/StatusEffect.java) | Static status effect definition: name, key, duration, power, description, and affected stat/type. |
| [StatusEffects](../src/main/java/com/alderfall/game/combat/StatusEffects.java) | Registry and lookup helpers for status effect definitions. |

### content/

Authored/static definitions: items, equipment, materials, quests, culture, places and regional identities.

| Source | Responsibility |
| --- | --- |
| [AssetCatalog](../src/main/java/com/alderfall/game/content/AssetCatalog.java) | Caches asset path lookups and missing asset names across known asset folders. |
| [BuiltDungeonStyle](../src/main/java/com/alderfall/game/content/BuiltDungeonStyle.java) | Material, resource and prop choices for constructed dungeons. |
| [CavernStyle](../src/main/java/com/alderfall/game/content/CavernStyle.java) | Natural cavern biome/material identity shared by generation and rendering. |
| [CompanionQuestContent](../src/main/java/com/alderfall/game/content/CompanionQuestContent.java) | Companion quest segment content, cargo handovers, arrangements and aftermath. |
| [DialogOption](../src/main/java/com/alderfall/game/content/DialogOption.java) | Stores an immediate-mode dialog button label, action, colors, and enabled state. |
| [EquipmentCatalog](../src/main/java/com/alderfall/game/content/EquipmentCatalog.java) | Equipment definitions, affixes, names/icons/prices and contextual loot selection. |
| [FolkloreContent](../src/main/java/com/alderfall/game/content/FolkloreContent.java) | Local observations and environmental storytelling for the first outward journey. |
| [FurnitureQuestContent](../src/main/java/com/alderfall/game/content/FurnitureQuestContent.java) | Binds quest stages to existing furniture and their keepers; governs furniture interactions. |
| [GameData](../src/main/java/com/alderfall/game/content/GameData.java) | Static content registry for classes, monsters, NPCs, items, equipment, shops, quests, and lookup helpers. |
| [HearthlandsFolklore](../src/main/java/com/alderfall/game/content/HearthlandsFolklore.java) | Named buildings, descriptions, observations and conversations for Oathstead and neighboring Crownlands. |
| [InteriorFurnishings](../src/main/java/com/alderfall/game/content/InteriorFurnishings.java) | Shared furniture identities used by placement/editing, collision and rendering. |
| [ItemCatalog](../src/main/java/com/alderfall/game/content/ItemCatalog.java) | Builds the item definition registry. |
| [MainStoryContent](../src/main/java/com/alderfall/game/content/MainStoryContent.java) | Campaign prose, investigation context and optional conversations; conversation evidence does not itself award progress. |
| [MaterialCatalog](../src/main/java/com/alderfall/game/content/MaterialCatalog.java) | Material properties and stat contributions independent of equipment templates. |
| [NpcBackstories](../src/main/java/com/alderfall/game/content/NpcBackstories.java) | NPC introductions and personal disclosures gated by earned context. |
| [NpcIdentity](../src/main/java/com/alderfall/game/content/NpcIdentity.java) | Regional/role appearance and portrait selection separate from stable NPC IDs. |
| [NpcQuestStories](../src/main/java/com/alderfall/game/content/NpcQuestStories.java) | Authored local-request stories and trade-specific quest context. |
| [PartyDialogue](../src/main/java/com/alderfall/game/content/PartyDialogue.java) | Authored companion-pair scenes and shared-hunt dialogue/progression integration. |
| [Profession](../src/main/java/com/alderfall/game/content/Profession.java) | Enumerates companion/village professions and display data. |
| [Quest](../src/main/java/com/alderfall/game/content/Quest.java) | Quest definition, objective categories and mutable progress/completion state. |
| [QuestNarrative](../src/main/java/com/alderfall/game/content/QuestNarrative.java) | Quest journal/conversation wording, objectives, evidence and completion context. |
| [RegionalBuildingTypes](../src/main/java/com/alderfall/game/content/RegionalBuildingTypes.java) | Classifies working institutions on existing lots and supplies their names. |
| [RegionalNpcQuests](../src/main/java/com/alderfall/game/content/RegionalNpcQuests.java) | Generates trade-related local work near an NPC's settlement. |
| [RegionalSettlementIdentity](../src/main/java/com/alderfall/game/content/RegionalSettlementIdentity.java) | Explicit regional culture, town profiles, authored building roles, districts and settlement asset selection. |
| [SettlementSpriteScale](../src/main/java/com/alderfall/game/content/SettlementSpriteScale.java) | Reference sizes for city_prop_refresh props such as lamps, stalls and planters; not town-building sizing. |
| [Shop](../src/main/java/com/alderfall/game/content/Shop.java) | Shop identity, display name, and stock item keys. |
| [SkillNode](../src/main/java/com/alderfall/game/content/SkillNode.java) | Defines one skill tree node, ranks, coordinates, prerequisites, bonuses, unlocks, and class restrictions. |
| [SkillTrees](../src/main/java/com/alderfall/game/content/SkillTrees.java) | Static skill tree definitions and helper methods for skill unlock/progression data. |
| [StoryLocationCatalog](../src/main/java/com/alderfall/game/content/StoryLocationCatalog.java) | Stable campaign location identities shared by quests and world generation. |
| [WesternReachFolklore](../src/main/java/com/alderfall/game/content/WesternReachFolklore.java) | Western-region building identities, interiors, residents, observations and dialogue. |

### dialogues/

Conversation library, companion-specific writing and ambient town exchanges.

| Source | Responsibility |
| --- | --- |
| [AmbientTownConversationLibrary](../src/main/java/com/alderfall/game/dialogues/AmbientTownConversationLibrary.java) | Contextual resident-to-resident conversation drafts and speaker/listener lines. |
| [AriaDialogue](../src/main/java/com/alderfall/game/dialogues/AriaDialogue.java) | Aria-specific conversation voice, quest acceptance/clarification and contextual response text. |
| [CalderDialogue](../src/main/java/com/alderfall/game/dialogues/CalderDialogue.java) | Calder-specific conversation voice, quest acceptance/clarification and contextual response text. |
| [CassiaDialogue](../src/main/java/com/alderfall/game/dialogues/CassiaDialogue.java) | Cassia-specific conversation voice, quest acceptance/clarification and contextual response text. |
| [CompanionDialogueProfile](../src/main/java/com/alderfall/game/dialogues/CompanionDialogueProfile.java) | Companion conversation profile data. |
| [CompanionMemoryTone](../src/main/java/com/alderfall/game/dialogues/CompanionMemoryTone.java) | Wording for remembered companion experiences. |
| [DialogueLibrary](../src/main/java/com/alderfall/game/dialogues/DialogueLibrary.java) | Provides NPC dialog text and contextual conversation lines. |
| [LyraDialogue](../src/main/java/com/alderfall/game/dialogues/LyraDialogue.java) | Lyra-specific conversation voice, quest acceptance/clarification and contextual response text. |
| [MaeraDialogue](../src/main/java/com/alderfall/game/dialogues/MaeraDialogue.java) | Maera-specific conversation voice, quest acceptance/clarification and contextual response text. |
| [RafiqDialogue](../src/main/java/com/alderfall/game/dialogues/RafiqDialogue.java) | Rafiq-specific conversation voice, quest acceptance/clarification and contextual response text. |
| [SamirDialogue](../src/main/java/com/alderfall/game/dialogues/SamirDialogue.java) | Samir-specific conversation voice, quest acceptance/clarification and contextual response text. |
| [SeraphineDialogue](../src/main/java/com/alderfall/game/dialogues/SeraphineDialogue.java) | Seraphine-specific conversation voice, quest acceptance/clarification and contextual response text. |
| [VesperDialogue](../src/main/java/com/alderfall/game/dialogues/VesperDialogue.java) | Vesper-specific conversation voice, quest acceptance/clarification and contextual response text. |

### entities/

Actors/NPCs, schedules, navigation and world-character animation.

| Source | Responsibility |
| --- | --- |
| [Actor](../src/main/java/com/alderfall/game/entities/Actor.java) | Mutable combat/gameplay character model for player, allies, recruits, and monsters. Tracks stats, HP/MP, inventory, equipment, skills, abilities, XP, and helpers. |
| [AmbientNpcAi](../src/main/java/com/alderfall/game/entities/AmbientNpcAi.java) | Time-of-day routines, job behavior and target selection for ambient residents. |
| [CharacterMotion](../src/main/java/com/alderfall/game/entities/CharacterMotion.java) | Cached secondary motion for characters without authored action strips. |
| [CharacterWorldScale](../src/main/java/com/alderfall/game/entities/CharacterWorldScale.java) | Stores world-sprite dimensions, foot lift, and shadow width. |
| [DirectionalSprite](../src/main/java/com/alderfall/game/entities/DirectionalSprite.java) | Names the selected directional sprite and whether it should be flipped horizontally. |
| [FloatingText](../src/main/java/com/alderfall/game/entities/FloatingText.java) | Floating combat text instance with position, text, color, and lifespan. |
| [LocomotionClock](../src/main/java/com/alderfall/game/entities/LocomotionClock.java) | Distance-driven gait phase that continues across tile/direction changes and settles on a planted step. |
| [Npc](../src/main/java/com/alderfall/game/entities/Npc.java) | Defines NPC identity, position, sprite, shop/recruit/quest metadata, and dialog role. |
| [NpcJob](../src/main/java/com/alderfall/game/entities/NpcJob.java) | NPC work-role metadata, active times and common role factories. |
| [TownNpcNavigation](../src/main/java/com/alderfall/game/entities/TownNpcNavigation.java) | Cached resident street routes and checkpoints that avoid cutting building corners. |
| [WorldCharacterAnimation](../src/main/java/com/alderfall/game/entities/WorldCharacterAnimation.java) | World sprite selection/normalization and consistent ground/body alignment across actions and walking modes. |

### events/

Gameplay workflows for interactive roaming events.

| Source | Responsibility |
| --- | --- |
| [RoamingEventWorkflow](../src/main/java/com/alderfall/game/events/RoamingEventWorkflow.java) | Gameplay prompts and resolution for roaming events, including shrines, travelers, ambushes and traps. |

### inventory/

Item/equipment models and inventory drag/drop support.

| Source | Responsibility |
| --- | --- |
| [Equipment](../src/main/java/com/alderfall/game/inventory/Equipment.java) | Defines equippable item stats, slot, class restrictions, rarity, and description. |
| [InventoryDrag](../src/main/java/com/alderfall/game/inventory/InventoryDrag.java) | Tracks the dragged inventory item and its source slot. |
| [InventoryDragZone](../src/main/java/com/alderfall/game/inventory/InventoryDragZone.java) | Defines a clickable/draggable inventory source rectangle. |
| [InventoryDropKind](../src/main/java/com/alderfall/game/inventory/InventoryDropKind.java) | Categorizes inventory drop targets as slot, character, or pack. |
| [InventoryDropZone](../src/main/java/com/alderfall/game/inventory/InventoryDropZone.java) | Defines an inventory drop rectangle, kind, and slot. |
| [Item](../src/main/java/com/alderfall/game/inventory/Item.java) | Defines consumable/shop item metadata: key, display name, icon, cost, healing, and MP recovery. |
| [ItemRarity](../src/main/java/com/alderfall/game/inventory/ItemRarity.java) | Item rarity categories and display labels. |

### map/

World/map generation and storage, interiors, location dressing and world-map UI.

| Source | Responsibility |
| --- | --- |
| [DestinationApproaches](../src/main/java/com/alderfall/game/map/DestinationApproaches.java) | Trails and dressing leading to actual predefined destinations. |
| [DungeonExteriorCatalog](../src/main/java/com/alderfall/game/map/DungeonExteriorCatalog.java) | Surface entrance identity, climate and regional props independent of dungeon floor theme. |
| [DungeonGenerator](../src/main/java/com/alderfall/game/map/DungeonGenerator.java) | Seeded dungeon profiles, functional rooms, corridors, critical routes, props and encounters. |
| [FolklorePropGenerator](../src/main/java/com/alderfall/game/map/FolklorePropGenerator.java) | Deterministic, route-safe environmental story props for the initial folklore region. |
| [HearthlandsPropGenerator](../src/main/java/com/alderfall/game/map/HearthlandsPropGenerator.java) | Authored Hearthlands settlement/overworld anchors with road-safe placement. |
| [InteriorLayout](../src/main/java/com/alderfall/game/map/InteriorLayout.java) | Composes rooms, furniture groups, work positions and entrance space as one layout. |
| [MapArea](../src/main/java/com/alderfall/game/map/MapArea.java) | Holds one map grid, label, kind, props, and tile access/mutation helpers. |
| [NatureSiteGenerator](../src/main/java/com/alderfall/game/map/NatureSiteGenerator.java) | Sparse seeded nature vignettes and visual trails with protected-placement rules. |
| [OverworldLandmarkPropGenerator](../src/main/java/com/alderfall/game/map/OverworldLandmarkPropGenerator.java) | Discoverability props near settlements, landmarks and roads. |
| [OverworldLocationBlueprints](../src/main/java/com/alderfall/game/map/OverworldLocationBlueprints.java) | Reusable authored site layouts describing structures, perimeters and activity clusters; WorldMap validates placement. |
| [OverworldRoadPlanner](../src/main/java/com/alderfall/game/map/OverworldRoadPlanner.java) | Ground routing with straight bank-to-bank water crossings. |
| [RegionalSettlementGenerator](../src/main/java/com/alderfall/game/map/RegionalSettlementGenerator.java) | Regional surfaces, water, districts, commons, vegetation and door approaches after fixed entrances exist. |
| [SettlementDressing](../src/main/java/com/alderfall/game/map/SettlementDressing.java) | Final settlement composition, including street lighting after districts and portals exist. |
| [WesternAbbeyGrounds](../src/main/java/com/alderfall/game/map/WesternAbbeyGrounds.java) | Authored abbey garden/undercroft layouts, entrances and residents. |
| [WesternReachPropGenerator](../src/main/java/com/alderfall/game/map/WesternReachPropGenerator.java) | Deterministic, route-safe western regional story props. |
| [WorldMap](../src/main/java/com/alderfall/game/map/WorldMap.java) | World generation and map data for overworld, cities, villages, player camp, dungeons, interiors, adventure sites, location patches, props, transitions, and path/passability data. |
| [WorldMapLabels](../src/main/java/com/alderfall/game/map/WorldMapLabels.java) | Prioritizes and places map labels within a shared overlap budget. |
| [WorldMapOverlayRenderer](../src/main/java/com/alderfall/game/map/WorldMapOverlayRenderer.java) | Draws the interactive world-map overlay, site markers and labels at the current viewport. |
| [WorldMapViewport](../src/main/java/com/alderfall/game/map/WorldMapViewport.java) | World-map viewing bounds and viewport data. |
| [WorldTransition](../src/main/java/com/alderfall/game/map/WorldTransition.java) | Map transition target and display message. |

### minigames/

Camp-defense gameplay.

| Source | Responsibility |
| --- | --- |
| [CampDefenseMinigame](../src/main/java/com/alderfall/game/minigames/CampDefenseMinigame.java) | Camp-defense simulation: waves, enemies, player movement, abilities and level-up choices. |

### render/battle/

Extracted combat visual-effect drawing.

| Source | Responsibility |
| --- | --- |
| [AbilityImpactArt](../src/main/java/com/alderfall/game/render/battle/AbilityImpactArt.java) | Draws dedicated ability impact art and associated charge/projectile treatments. |
| [BattleVfxRenderer](../src/main/java/com/alderfall/game/render/battle/BattleVfxRenderer.java) | Draws timed combat effects and persistent status effects with independent lifetimes. |

### render/world/

Extracted terrain details, atmosphere, lights and prop drawing.

| Source | Responsibility |
| --- | --- |
| [InteriorLightField](../src/main/java/com/alderfall/game/render/world/InteriorLightField.java) | Caches interior light visibility; walls/void block transmission and doors allow it. |
| [TerrainFeatureRenderer](../src/main/java/com/alderfall/game/render/world/TerrainFeatureRenderer.java) | Terrain edges, surface seams and additional tile details. |
| [WorldAtmosphereRenderer](../src/main/java/com/alderfall/game/render/world/WorldAtmosphereRenderer.java) | Clouds, outdoor atmosphere and interior time overlays. |
| [WorldLight](../src/main/java/com/alderfall/game/render/world/WorldLight.java) | Dynamic world light data and shadow contribution helpers. |
| [WorldLightingRenderer](../src/main/java/com/alderfall/game/render/world/WorldLightingRenderer.java) | Dynamic world lighting and shadow drawing with performance controls. |
| [WorldPropRenderer](../src/main/java/com/alderfall/game/render/world/WorldPropRenderer.java) | Prop/furniture images, sizing, placement and gathered-prop visuals. |

### render/

World-map terrain cache; extracted world and battle rendering packages below.

| Source | Responsibility |
| --- | --- |
| [WorldMapTerrainCache](../src/main/java/com/alderfall/game/render/WorldMapTerrainCache.java) | Caches terrain imagery and cartographic symbols for the world-map overlay. |

### rendering/

Shared image loading/caching, terrain/building rendering, motion layers and render utilities.

| Source | Responsibility |
| --- | --- |
| [AlternateWalkingMotion](../src/main/java/com/alderfall/game/rendering/AlternateWalkingMotion.java) | Alternative cutout walking with independent feet and stable source artwork. |
| [AmbientMotionLayer](../src/main/java/com/alderfall/game/rendering/AmbientMotionLayer.java) | Small ambient particles such as leaves, with spawn/update/draw behavior. |
| [ArticulatedWalkingMotion](../src/main/java/com/alderfall/game/rendering/ArticulatedWalkingMotion.java) | Jointed walking variant with complete legs and an independent clothing layer. |
| [AssetStore](../src/main/java/com/alderfall/game/rendering/AssetStore.java) | Loads, scales, fits, crops, and caches image assets and animation frames. |
| [CavernTerrainRenderer](../src/main/java/com/alderfall/game/rendering/CavernTerrainRenderer.java) | Continuous cavern/constructed-dungeon terrain masses and exposed faces. |
| [ConnectedFurniture](../src/main/java/com/alderfall/game/rendering/ConnectedFurniture.java) | Computes left/right joins for horizontal furniture runs. |
| [DebugMetrics](../src/main/java/com/alderfall/game/rendering/DebugMetrics.java) | Small runtime timing helpers used by render metrics; belongs in production. |
| [GroundedArmRig](../src/main/java/com/alderfall/game/rendering/GroundedArmRig.java) | Jointed upper-arm, forearm and hand/held-object rendering for grounded motion. |
| [GroundedPartMasks](../src/main/java/com/alderfall/game/rendering/GroundedPartMasks.java) | Artwork-specific garment, equipment and carrying-pose ownership masks. |
| [GroundedWalkingMotion](../src/main/java/com/alderfall/game/rendering/GroundedWalkingMotion.java) | Grounded walking with articulated legs, planted feet, weight/spine motion and clothing handling. |
| [ItemAppearance](../src/main/java/com/alderfall/game/rendering/ItemAppearance.java) | Inventory/equipment appearance from atlas shapes, materials, fittings and ornament channels. |
| [LayeredTerrainRenderer](../src/main/java/com/alderfall/game/rendering/LayeredTerrainRenderer.java) | World-space surface/material composition for overworld and outdoor settlements, with caching. |
| [NpcChoreAnimationLayer](../src/main/java/com/alderfall/game/rendering/NpcChoreAnimationLayer.java) | Selects visible chore actions for NPCs on the active map. |
| [PixelMotionTween](../src/main/java/com/alderfall/game/rendering/PixelMotionTween.java) | Cached motion-based in-between frames preserving pixel edges. |
| [PropPlacement](../src/main/java/com/alderfall/game/rendering/PropPlacement.java) | Visual prop anchors and sub-tile offsets; retains gameplay ownership on the original tile. |
| [ReactivePropLayer](../src/main/java/com/alderfall/game/rendering/ReactivePropLayer.java) | Short-lived prop reaction pulses and particles. |
| [RenderBackBuffer](../src/main/java/com/alderfall/game/rendering/RenderBackBuffer.java) | Creates and maintains the render backing buffer, including volatile images. |
| [RenderMetrics](../src/main/java/com/alderfall/game/rendering/RenderMetrics.java) | Optional frame/section timing collection and performance reporting. |
| [RoadSurface](../src/main/java/com/alderfall/game/rendering/RoadSurface.java) | Shared road/paving material identity separate from gameplay terrain codes. |
| [RoamingWorldEventLayer](../src/main/java/com/alderfall/game/rendering/RoamingWorldEventLayer.java) | Spawns, updates, draws and locates interactive roaming world events. |
| [TerrainFootstepLayer](../src/main/java/com/alderfall/game/rendering/TerrainFootstepLayer.java) | Surface-dependent footstep particles/traces and their lifetimes. |
| [TextileMaterialSprites](../src/main/java/com/alderfall/game/rendering/TextileMaterialSprites.java) | Extracts individual textile inventory sprites from the shared atlas. |
| [TownBuildingArt](../src/main/java/com/alderfall/game/rendering/TownBuildingArt.java) | Selects the twelve town-specific building images per town and normalizes visible sprite size without changing footprints. |
| [WalkingArmRig](../src/main/java/com/alderfall/game/rendering/WalkingArmRig.java) | Jointed arm and held-object rendering for the articulated walking path. |
| [WalkingStride](../src/main/java/com/alderfall/game/rendering/WalkingStride.java) | Pixel-cutout leg articulation used by walking presentation. |
| [WaterTileRenderer](../src/main/java/com/alderfall/game/rendering/WaterTileRenderer.java) | Animated water tile detail: depth, currents, ripples, glints and waves. |
| [WeatherLayerCacheStore](../src/main/java/com/alderfall/game/rendering/WeatherLayerCacheStore.java) | Caches stable, scrolling and precipitation drawing layers and manages repaint decisions. |
| [WorldDepthRenderer](../src/main/java/com/alderfall/game/rendering/WorldDepthRenderer.java) | Orders scenery/characters by ground contact and adds local foreground cutaways. |
| [WorldRenderer](../src/main/java/com/alderfall/game/rendering/WorldRenderer.java) | World terrain pass, cached chunks, tile overlays, settlement walls and gates; coordinates terrain/prop/light drawing helpers. |

### state/

Central mutable gameplay state and orchestration.

| Source | Responsibility |
| --- | --- |
| [GameState](../src/main/java/com/alderfall/game/state/GameState.java) | Central mutable game state and gameplay controller for mode transitions, movement, quests, inventory, crafting, village, NPCs, battles, weather, and progression. |

### systems/

Persistence, crafting/assembly, village management, chests, equipment hooks and weather.

| Source | Responsibility |
| --- | --- |
| [AssemblyCrafting](../src/main/java/com/alderfall/game/systems/AssemblyCrafting.java) | Component-based equipment assembly, blueprints and versioned item keys preserving parts/quality through saves. |
| [ChestSystem](../src/main/java/com/alderfall/game/systems/ChestSystem.java) | Persistent chest contents and transfers; retains empty containers to prevent refilling. |
| [CraftingCalculation](../src/main/java/com/alderfall/game/systems/CraftingCalculation.java) | Shared deterministic crafting aptitude and quality calculations for results and previews. |
| [CraftingSystem](../src/main/java/com/alderfall/game/systems/CraftingSystem.java) | Defines workstations, item info, recipes, gather candidates, item costs, and crafting utilities. |
| [EquipmentEffectHooks](../src/main/java/com/alderfall/game/systems/EquipmentEffectHooks.java) | Shared identifiers for special equipment effects interpreted by gameplay/combat. |
| [SaveSystem](../src/main/java/com/alderfall/game/systems/SaveSystem.java) | Save/load persistence, save discovery, summaries, property serialization, and restoration helpers. |
| [VillageManager](../src/main/java/com/alderfall/game/systems/VillageManager.java) | Village construction data, building plans, costs, placeable assets, tile plans, worker roles, and settlement stages. |
| [WeatherController](../src/main/java/com/alderfall/game/systems/WeatherController.java) | Chooses/transitions weather and determines which maps use outdoor weather. |
| [WeatherSystem](../src/main/java/com/alderfall/game/systems/WeatherSystem.java) | Derived weather intensities, wind and water-wave behavior used by presentation. |

### ui/

Screen-specific renderers and interaction helpers; dialogue figure animation.

| Source | Responsibility |
| --- | --- |
| [AssemblyRenderer](../src/main/java/com/alderfall/game/ui/AssemblyRenderer.java) | Workshop UI for preparing components and assembling selected parts. |
| [CharacterRenderer](../src/main/java/com/alderfall/game/ui/CharacterRenderer.java) | Character/equipment/profession and skill-tree UI, including tabs, graph nodes and tooltips. |
| [DialogueAnimationLayers](../src/main/java/com/alderfall/game/ui/DialogueAnimationLayers.java) | Combines dialogue idle, gestures and bounded secondary motion without persisting animation state. |
| [DialogueBodyMotion](../src/main/java/com/alderfall/game/ui/DialogueBodyMotion.java) | Dialogue figure skeleton, parent pose transforms, collision shapes and planted-foot constraints. |
| [DialogueDeformationMesh](../src/main/java/com/alderfall/game/ui/DialogueDeformationMesh.java) | Textured deformation mesh drawing with Java2D interpolation. |
| [DialogueFigureAnimation](../src/main/java/com/alderfall/game/ui/DialogueFigureAnimation.java) | Facial poses, blinking, speech and foot-anchored idle for dialogue artwork. |
| [DialoguePartMask](../src/main/java/com/alderfall/game/ui/DialoguePartMask.java) | Feathered ownership masks for animated figure attachments. |
| [DialoguePoseTrack](../src/main/java/com/alderfall/game/ui/DialoguePoseTrack.java) | Timed dialogue rotation keyframes and pose interpolation. |
| [DialogueRenderer](../src/main/java/com/alderfall/game/ui/DialogueRenderer.java) | Dialogue screens, speech boxes, options and standing character artwork. |
| [DialogueRigProfile](../src/main/java/com/alderfall/game/ui/DialogueRigProfile.java) | Artwork-specific attachment/face coordinates in alpha-cropped figure space. |
| [InventoryRenderer](../src/main/java/com/alderfall/game/ui/InventoryRenderer.java) | Inventory/chest panels and recipe-crafting presentation and interaction helpers. |
| [ItemBrowser](../src/main/java/com/alderfall/game/ui/ItemBrowser.java) | Reusable item category/search/paging and input state for item collections. |
| [PartyPortraitRenderer](../src/main/java/com/alderfall/game/ui/PartyPortraitRenderer.java) | Shared party-card artwork framing and idle/speech animation. |
| [QuestLogRenderer](../src/main/java/com/alderfall/game/ui/QuestLogRenderer.java) | Quest list, details, dialogue timeline and journal tooltips. |
| [SaveMenuRenderer](../src/main/java/com/alderfall/game/ui/SaveMenuRenderer.java) | Save/load screens, folder/save selection, summaries and overwrite prompts. |
| [ShopRenderer](../src/main/java/com/alderfall/game/ui/ShopRenderer.java) | Shop stock/player-pack UI and trade input handling. |
| [SkillTab](../src/main/java/com/alderfall/game/ui/SkillTab.java) | Skill-screen tab choices and labels. |
| [TitleScene](../src/main/java/com/alderfall/game/ui/TitleScene.java) | Decorative title-stage drawing aligned with the menu viewport. |
| [VillageOverlayRenderer](../src/main/java/com/alderfall/game/ui/VillageOverlayRenderer.java) | Village management overlays and their UI controls. |
| [VillageSidebarRenderer](../src/main/java/com/alderfall/game/ui/VillageSidebarRenderer.java) | Village construction sidebar, search, lists, costs and building previews. |

### uiwidgets/

Small reusable button and tooltip data types.

| Source | Responsibility |
| --- | --- |
| [TooltipZone](../src/main/java/com/alderfall/game/uiwidgets/TooltipZone.java) | Screen rectangle with tooltip title/body. |
| [UiButton](../src/main/java/com/alderfall/game/uiwidgets/UiButton.java) | Immediate-mode UI button rectangle, label, and action. |

### worldmodel/

World records, terrain/settings identities, collision, pathfinding and narration.

| Source | Responsibility |
| --- | --- |
| [BuildingGeometry](../src/main/java/com/alderfall/game/worldmodel/BuildingGeometry.java) | Logical building solid bases and general display sizing/module rules; town-specific visual size is delegated elsewhere. |
| [CityBuilding](../src/main/java/com/alderfall/game/worldmodel/CityBuilding.java) | Stable building key, rectangular tile footprint, style and palette; provides dimensions, anchor and containment helpers. |
| [InteriorStyle](../src/main/java/com/alderfall/game/worldmodel/InteriorStyle.java) | Regional household materials/customs shared by interior generation and drawing. |
| [Kingdom](../src/main/java/com/alderfall/game/worldmodel/Kingdom.java) | Top-level kingdom metadata record used by world map/settlement features. |
| [LandmarkDiscoveryLog](../src/main/java/com/alderfall/game/worldmodel/LandmarkDiscoveryLog.java) | Discovery-log text for nearby named landmarks. |
| [NearbyPrompt](../src/main/java/com/alderfall/game/worldmodel/NearbyPrompt.java) | Stores nearby quest prompt data for world rendering. |
| [Pathfinder](../src/main/java/com/alderfall/game/worldmodel/Pathfinder.java) | Grid pathfinding, reachable target selection and movement costs/passability. |
| [PathNode](../src/main/java/com/alderfall/game/worldmodel/PathNode.java) | Priority queue node used by pathfinding. |
| [PlacementPulse](../src/main/java/com/alderfall/game/worldmodel/PlacementPulse.java) | Tracks short-lived map placement visual pulses. |
| [PropCollision](../src/main/java/com/alderfall/game/worldmodel/PropCollision.java) | World-space ground footprints and travel/navigation collision for props. |
| [RenderQuality](../src/main/java/com/alderfall/game/worldmodel/RenderQuality.java) | Rendering quality choices and effective weather-quality policy. |
| [SettlementSite](../src/main/java/com/alderfall/game/worldmodel/SettlementSite.java) | Top-level settlement site metadata record. |
| [Terrain](../src/main/java/com/alderfall/game/worldmodel/Terrain.java) | Terrain character mapping for labels, colors, assets, and passability groups. |
| [TilePoint](../src/main/java/com/alderfall/game/worldmodel/TilePoint.java) | Immutable map coordinate. |
| [TravelLogNarrator](../src/main/java/com/alderfall/game/worldmodel/TravelLogNarrator.java) | Travel and map-transition narration. |
| [WeatherCondition](../src/main/java/com/alderfall/game/worldmodel/WeatherCondition.java) | Enumerates weather states used by world simulation and rendering. |
| [WeatherQuality](../src/main/java/com/alderfall/game/worldmodel/WeatherQuality.java) | Weather quality settings and configuration-key lookup. |
| [WorldProp](../src/main/java/com/alderfall/game/worldmodel/WorldProp.java) | World/interior decoration or object with tile position, asset key, and size. |

## Diagnostics, review exporters and ownership limits

The source roots currently contain 201 production files, 81 test files, 31 review
files and one shared test-support file. This count excludes Java studies beside
HTML sites in `tools/reviews/`, which the review build can also include.

| Responsibility | Useful starting points |
| --- | --- |
| Broad behavior | `SmokeTest`. |
| Towns/buildings/gateways | `TownArchitectureTest`, `RegionalSettlementTest`, `RegionalBuildingsTest`, `BuildingCollisionTest`, `TownNpcNavigationTest`. |
| Movement/animation | `GroundedWalkingTest`, `WorldPoseTest`, `LocomotionTest`, `WalkAnimationSpeedTest`, `CharacterAnimationAudit` (shared support). |
| World generation | `WorldGenerationTest`, `DungeonGenerationTest`, `DestinationApproachesTest`, `SettlementCompositionTest`. |
| Items/crafting | `ItemAppearanceTest`, `AssemblyCraftingTest`, `CraftingCalculationTest`, `ChestSystemTest`. |
| Dialogue/quests | `MainStoryDialogueTest`, `CompanionQuestSegmentTest`, `QuestNarrativeTest`, `DialogueAnimationTest`. |
| Visual exports | `WorldGenerationPreview`, `DungeonPreview`, `InteriorDesignPreview`, `NpcVisualReview`, `CompanionDialogueQaExport`. |
| Performance/audits | `RenderBenchmark`, `ZoomRenderBenchmark`, `SkillTreeAudit`, `GameDiagnostics`. |

Browse [tests](../src/test/java/com/alderfall/game/),
[review exporters](../src/review/java/com/alderfall/game/) and
[shared support](../src/testSupport/java/com/alderfall/game/). Some diagnostics
can export screenshots; read their arguments before running them. Tests and
exporters must not become production dependencies.

The largest coordination files remain `GamePanel`, `GameState`, `WorldMap`,
`DialogueLibrary` and `GameData`. Their responsibilities are broad despite existing
extractions. Reuse an extracted owner before extending these files. Similar names
are not proof of duplication: for example `SettlementSpriteScale` sizes refreshed
props, `TownBuildingArt` sizes town buildings, and `BuildingGeometry` also governs
solid bases/general building rules. Likewise alternative walking implementations
are selectable rendering modes, not automatically retired copies.

Top-level `worldmodel/Kingdom.java` and `SettlementSite.java` coexist with nested
`WorldMap` types of the same simple names. Inspect the actual referenced type and
callers before consolidating. This guide is an ownership map, not proof that all
code is reachable or that similarly named classes are safe to merge.

## Build, run and checks

Run from repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/run.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/test.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/check.ps1
```

`test.ps1` defaults to `SmokeTest`, builds development sources under ignored
`Java/temp/tests/`, and isolates smoke-test saves. For a selected diagnostic:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/test.ps1 -Test com.alderfall.game.TownArchitectureTest
```

For exporters, compile with `-IncludeReviews -OutputDirectory temp/<task>/classes`;
use the class's declared package when launching it from `Java/`. Normal builds
compile only production sources to `Java/out/`. `check.ps1` validates repository
policy, tool registration/imports, class identities, asset ambiguity, HTML links
and quarantine checksums; it does not replace behavior tests or visual review.

Maintain this guide when adding/moving a source file or changing its responsibility.
See the [organization proposal](repository-organization-report.md) for staged
future work and [enforcement](agent-guidelines-and-enforcement.md) for check limits.
