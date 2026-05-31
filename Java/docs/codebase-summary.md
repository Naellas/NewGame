# Echoes of Alderfall Java Codebase Summary

Generated: 2026-05-31

This document summarizes the current Java port, the recent performance and organization changes, and the role of every Java source file under `Java/src/main/java/com/alderfall/game`.

## Recent Optimization And Refactor Changes

The latest optimization pass preserved the current feature set while making the code easier to maintain and reducing repeated work in hot paths.

- Split small `GamePanel` helper types into separate package-private `.java` files:
  `BattleActorPose`, `CharacterWorldScale`, `DialogOption`, `DirectionalSprite`, `InventoryDrag`, `InventoryDragZone`, `InventoryDropKind`, `InventoryDropZone`, `NearbyPrompt`, `PathNode`, `PlacementPulse`, `TooltipZone`, and `WorldLight`.
- Added `AssetCatalog` to cache asset path lookups and remember missing assets, reducing repeated `Files.exists(...)` checks when rendering or probing sprites.
- Updated `AssetStore` to cache decoded source images separately from scaled images. This avoids re-reading the same PNG source when the same asset is requested at multiple sizes.
- Added animation metadata caching in `AssetStore`, including negative caching for missing `.frames` files.
- Reduced per-frame allocation in `GamePanel` by reusing the visible world prop list and lifting the world-prop draw ordering comparator into a static field.
- Verified the refactor with:
  - `powershell -ExecutionPolicy Bypass -File Java\scripts\build.ps1`
  - `java -cp Java\out com.alderfall.game.SmokeTest`

## Project Shape

The Java port is a dependency-free Java 21/Swing game. The project uses plain `javac` scripts instead of Maven or Gradle, and all runtime assets are staged under `Java/assets`.

Primary runtime flow:

1. `Main` starts the Swing UI.
2. `GameWindow` owns the application frame and fullscreen handling.
3. `GamePanel` owns the fixed-step timer, rendering, input, UI overlays, and sound/music triggers.
4. `GameState` owns game rules, current mode, player progression, quests, inventory, village state, map transitions, crafting, and battle entry/exit.
5. `WorldMap` owns map generation, map data, passability, transitions, settlements, dungeons, city/interior/village layouts, and generated props.
6. `Battle` owns turn-based combat resolution.
7. `AssetStore` and `AssetCatalog` load, locate, scale, crop, and cache image assets.
8. `SaveSystem` persists named adventures under `Java/saves`.

## Main Systems

### Application And Rendering

- `Main` launches the game on the Swing event thread.
- `GameWindow` creates the window, installs `GamePanel`, and toggles fullscreen.
- `GamePanel` renders all game screens: title, class select, story intro, world, battle, dialog, quests, skills, inventory, crafting, party, village, settlement board, shop, world map, pause, settings, and save/load.
- `UiButton`, `TooltipZone`, `DialogOption`, and drag/drop helper records support immediate-mode UI interaction.

### Game State And Rules

- `GameState` is the central gameplay state machine. It tracks mode, world position, player, allies, quests, inventory, equipment, skills, crafting, shops, NPC dialog, settlement/village actions, battle lifecycle, weather, day/night, and save/load UI state.
- `GameMode` enumerates every high-level game screen and overlay state.
- `GameConfig` stores constants and tunable gameplay settings loaded from `Java/config/gameplay.json` plus local settings.
- `GameData` provides static content: player classes, monsters, NPCs, items, shops, quests, recruit specs, and supporting lookup helpers.

### World And Maps

- `WorldMap` builds the overworld and submaps. It generates terrain, roads, rivers, shorelines, settlements, dungeons, city/village layouts, interiors, props, locations, transitions, and passability.
- `MapArea` stores a single map grid and related per-map data.
- `Terrain` maps terrain characters to colors, labels, asset names, and passability meaning.
- `WorldTransition`, `WorldProp`, `TilePoint`, `CityBuilding`, `Kingdom`, and `SettlementSite` are compact records used by world generation and gameplay.

### Combat

- `Battle` owns party/enemy turn flow, target selection, abilities, item use, guard behavior, monster AI, status effects, XP/gold rewards, floating combat text, and action animation state.
- `BattleActionAnimation` tracks attack/cast/projectile/effect stages.
- `BattleActorPose` stores render pose offsets for combat animation.
- `Actor`, `Ability`, `AbilityStatus`, `MonsterAbility`, `MonsterAbilityStatus`, `EffectStack`, `StatusEffect`, `StatusEffects`, and `FloatingText` support combat stats and effects.

### Progression, Skills, Crafting, Village

- `SkillTrees` and `SkillNode` define class/common skill trees, prerequisites, ranks, bonuses, and ability unlocks.
- `CraftingSystem` defines workstations, material/item data, recipes, gather candidates, and crafting cost logic.
- `VillageManager` defines village buildings, costs, placeable assets, tile plans, worker roles, and settlement growth stages.
- `Profession` maps recruit/worker profession roles and labels.

### Assets And Audio

- `AssetCatalog` caches path resolution for PNG assets.
- `AssetStore` loads source images, scales/crops/fits assets, reads animation frame metadata, caches scaled images, and generates fallback placeholder images.
- `MusicManager` plays looping zone/battle/town music.
- `SoundManager` loads and plays SFX clips.

### Persistence And Tests

- `SaveSystem` serializes and restores game state, map/village/interior state, party, inventory, skills, quests, settings, and save summaries.
- `SmokeTest` exercises core state transitions and game rules without launching the full UI.

## Top-Level Java Source Index

| File | Type | Purpose |
| --- | --- | --- |
| `Ability.java` | public record `Ability`; nested enum `AbilityKind` | Describes a player ability: name, power, MP cost, kind, and target profile. |
| `AbilityStatus.java` | public record `AbilityStatus` | Describes a status effect that an ability may apply, including target and chance. |
| `Actor.java` | public final class `Actor` | Mutable combat/gameplay character model for player, allies, recruits, and monsters. Tracks stats, HP/MP, inventory, equipment, skills, abilities, XP, and helpers. |
| `AssetCatalog.java` | package-private final class `AssetCatalog` | Caches asset path lookups and missing asset names across known asset folders. |
| `AssetStore.java` | public final class `AssetStore` | Loads, scales, fits, crops, and caches image assets and animation frames. |
| `Battle.java` | public final class `Battle` | Turn-based battle engine for party/enemy combat, statuses, target selection, rewards, logs, and animation timing. |
| `BattleActionAnimation.java` | public final class `BattleActionAnimation`; nested enum `Stage` | Tracks combat animation progression from windup through release and recovery. |
| `BattleActorPose.java` | package-private record `BattleActorPose` | Stores combat render offsets, rotation, and scale for animated actors. |
| `CharacterWorldScale.java` | package-private record `CharacterWorldScale` | Stores world-sprite dimensions, foot lift, and shadow width. |
| `CityBuilding.java` | public record `CityBuilding` | Represents a building footprint, style, label, interior metadata, and optional source key. |
| `CraftingSystem.java` | public final class `CraftingSystem`; nested enum/records | Defines workstations, item info, recipes, gather candidates, item costs, and crafting utilities. |
| `DialogOption.java` | package-private record `DialogOption` | Stores an immediate-mode dialog button label, action, colors, and enabled state. |
| `DialogueLibrary.java` | public final class `DialogueLibrary` | Provides NPC dialog text and contextual conversation lines. |
| `DirectionalSprite.java` | package-private record `DirectionalSprite` | Names the selected directional sprite and whether it should be flipped horizontally. |
| `EffectStack.java` | public final class `EffectStack` | Runtime stack of a status effect with duration and power. |
| `Equipment.java` | public record `Equipment` | Defines equippable item stats, slot, class restrictions, rarity, and description. |
| `FloatingText.java` | public final class `FloatingText` | Floating combat text instance with position, text, color, and lifespan. |
| `GameConfig.java` | public final class `GameConfig` | Static dimensions/timing constants plus tunable encounter/audio settings and config persistence. |
| `GameData.java` | public final class `GameData`; nested records `MonsterSpec`, `RecruitSpec` | Static content registry for classes, monsters, NPCs, items, equipment, shops, quests, and lookup helpers. |
| `GameMode.java` | public enum `GameMode` | Enumerates UI/game modes such as menu, explore, battle, dialog, skills, inventory, village, shop, settings, and save menu. |
| `GamePanel.java` | public final class `GamePanel` | Swing panel, fixed-step game loop, rendering, input handling, UI overlays, camera, animation visuals, audio triggers, and interaction hit zones. |
| `GameState.java` | public final class `GameState`; nested records `NpcMotion`, `QuestObjective`, `QuestInteractible`, `VillageRecruitOption` | Central mutable game state and gameplay controller for mode transitions, movement, quests, inventory, crafting, village, NPCs, battles, weather, and progression. |
| `GameWindow.java` | public final class `GameWindow` | Swing frame setup, fullscreen toggle, panel installation, and shutdown handling. |
| `InventoryDrag.java` | package-private record `InventoryDrag` | Tracks the dragged inventory item and its source slot. |
| `InventoryDragZone.java` | package-private record `InventoryDragZone` | Defines a clickable/draggable inventory source rectangle. |
| `InventoryDropKind.java` | package-private enum `InventoryDropKind` | Categorizes inventory drop targets as slot, character, or pack. |
| `InventoryDropZone.java` | package-private record `InventoryDropZone` | Defines an inventory drop rectangle, kind, and slot. |
| `Item.java` | public record `Item` | Defines consumable/shop item metadata: key, display name, icon, cost, healing, and MP recovery. |
| `Kingdom.java` | public record `Kingdom` | Top-level kingdom metadata record used by world map/settlement features. |
| `Main.java` | public final class `Main` | Program entry point. |
| `MapArea.java` | public final class `MapArea` | Holds one map grid, label, kind, props, and tile access/mutation helpers. |
| `MonsterAbilities.java` | public final class `MonsterAbilities` | Registry and helper logic for monster-specific abilities. |
| `MonsterAbility.java` | public record `MonsterAbility`; nested enum `Kind` | Defines enemy ability behavior, power, chance, target, cooldown, status payloads, and effect key. |
| `MonsterAbilityStatus.java` | public record `MonsterAbilityStatus` | Status application payload for monster abilities. |
| `MusicManager.java` | public final class `MusicManager` | Loads, loops, cross-selects, and shuts down music clips. |
| `NearbyPrompt.java` | package-private record `NearbyPrompt` | Stores nearby quest prompt data for world rendering. |
| `Npc.java` | public record `Npc` | Defines NPC identity, position, sprite, shop/recruit/quest metadata, and dialog role. |
| `PathNode.java` | package-private record `PathNode` | Priority queue node used by pathfinding. |
| `PlacementPulse.java` | package-private record `PlacementPulse` | Tracks short-lived map placement visual pulses. |
| `Profession.java` | public enum `Profession` | Enumerates companion/village professions and display data. |
| `Quest.java` | public final class `Quest`; nested enum `ObjectiveKind` | Quest definition and runtime progress state for gather, visit, and defeat objectives. |
| `SaveSystem.java` | public final class `SaveSystem`; nested record `SaveSummary` | Save/load persistence, save discovery, summaries, property serialization, and restoration helpers. |
| `SettlementSite.java` | public record `SettlementSite` | Top-level settlement site metadata record. |
| `Shop.java` | public record `Shop` | Shop identity, display name, and stock item keys. |
| `SkillNode.java` | public record `SkillNode` | Defines one skill tree node, ranks, coordinates, prerequisites, bonuses, unlocks, and class restrictions. |
| `SkillTrees.java` | public final class `SkillTrees` | Static skill tree definitions and helper methods for skill unlock/progression data. |
| `SmokeTest.java` | public final class `SmokeTest` | Headless behavior smoke tests for important state/gameplay flows. |
| `SoundManager.java` | public final class `SoundManager` | Loads, plays, and volume-scales short sound effects. |
| `StatusEffect.java` | public record `StatusEffect` | Static status effect definition: name, key, duration, power, description, and affected stat/type. |
| `StatusEffects.java` | public final class `StatusEffects` | Registry and lookup helpers for status effect definitions. |
| `Terrain.java` | public final class `Terrain` | Terrain character mapping for labels, colors, assets, and passability groups. |
| `TilePoint.java` | public record `TilePoint` | Immutable map coordinate. |
| `TooltipZone.java` | package-private record `TooltipZone` | Screen rectangle with tooltip title/body. |
| `UiButton.java` | public record `UiButton` | Immediate-mode UI button rectangle, label, and action. |
| `VillageManager.java` | public final class `VillageManager`; nested records | Village construction data, building plans, costs, placeable assets, tile plans, worker roles, and settlement stages. |
| `WeatherCondition.java` | public enum `WeatherCondition` | Enumerates weather states used by world simulation and rendering. |
| `WorldLight.java` | package-private final class `WorldLight` | Dynamic world light data and shadow contribution helpers. |
| `WorldMap.java` | public final class `WorldMap`; nested records | World generation and map data for overworld, cities, villages, player camp, dungeons, interiors, adventure sites, location patches, props, transitions, and path/passability data. |
| `WorldProp.java` | public record `WorldProp` | World/interior decoration or object with tile position, asset key, and size. |
| `WorldTransition.java` | public record `WorldTransition` | Map transition target and display message. |

## Nested Types Worth Knowing

Some files contain nested types that are important for understanding the data model:

- `Ability.AbilityKind`: attack, heal, guard, and related player ability behavior categories.
- `BattleActionAnimation.Stage`: combat animation lifecycle stage.
- `CraftingSystem.Workstation`: crafting station categories.
- `CraftingSystem.ItemInfo`: material/item display metadata.
- `CraftingSystem.Recipe`: craftable output, workstation, costs, and requirements.
- `CraftingSystem.GatherCandidate`: possible gather target near the player.
- `GameData.MonsterSpec`: static monster stats and reward data.
- `GameData.RecruitSpec`: static recruit/companion data.
- `GameState.NpcMotion`: NPC movement interpolation data.
- `GameState.QuestObjective`: active quest target projected into the world.
- `GameState.QuestInteractible`: active interactible quest object.
- `GameState.VillageRecruitOption`: hireable settlement companion option.
- `MonsterAbility.Kind`: enemy ability behavior category.
- `Quest.ObjectiveKind`: gather, visit, or defeat.
- `SaveSystem.SaveSummary`: metadata displayed in the save/load menu.
- `VillageManager.VillageCost`: gold and item cost bundle.
- `VillageManager.BuildingPlan`: village building definition and requirements.
- `VillageManager.PlaceableAsset`: placeable building/prop entry.
- `VillageManager.TilePlan`: placeable terrain/tile entry.
- `VillageManager.WorkerRole`: village worker role metadata and bonuses.
- `VillageManager.SettlementStage`: growth-stage thresholds and descriptions.
- `WorldMap.LocationSite`: visible named location metadata.
- `WorldMap.Kingdom`: generated kingdom region metadata.
- `WorldMap.SettlementSite`: generated settlement metadata.

`WorldMap` also contains private implementation records such as location patches, adventure sites, and weighted decoration options. They are generation internals rather than external model types.

## Current Large Files And Ownership

The most complex files are still the central systems:

- `GamePanel.java`: rendering, UI, input, camera, and presentation logic.
- `WorldMap.java`: procedural generation, map construction, props, interiors, transitions, and passability.
- `GameState.java`: runtime state machine and gameplay orchestration.
- `GameData.java`: static content registry.
- `CraftingSystem.java`, `Battle.java`, `SaveSystem.java`, `VillageManager.java`, and `SkillTrees.java`: major gameplay subsystems.

The extraction performed in this pass moved small helper types out of `GamePanel`, but `GamePanel` still remains the primary presentation layer. Future productive splits would likely target large drawing regions, for example battle rendering, village UI, inventory UI, and world rendering helpers, while keeping `GamePanel` as the coordinator.

## Runtime Data And Assets

- `Java/assets`: PNG sprites, terrain, UI-relevant images, VFX, SFX, and music.
- `Java/config/gameplay.json`: gameplay tuning loaded by `GameConfig`.
- `Java/saves`: named save files written by `SaveSystem`.
- `Java/tools`: Python asset-generation/extraction tools retained for regeneration workflows.
- `Java/scripts`: build, run, and smoke-test PowerShell scripts.

## Build And Verification

Use the existing scripts from the `Java` folder:

```powershell
.\scripts\build.ps1
.\scripts\smoke-test.ps1
.\scripts\run.ps1
```

If local PowerShell execution policy blocks scripts, use:

```powershell
powershell -ExecutionPolicy Bypass -File Java\scripts\build.ps1
java -cp Java\out com.alderfall.game.SmokeTest
```

The optimization/refactor pass was verified with a clean Java compile and the headless smoke test.
