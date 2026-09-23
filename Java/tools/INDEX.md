# Tool index

Run Python commands from any directory with `python <path-to-Java>/tools/run.py NAME [arguments]`.
The runner selects `Java/` as its working directory; relative inputs are interpreted there.
Use `--list` to list names. Importers/generators may overwrite their named outputs: inspect `--help` or source before regenerating.

| Command | Canonical implementation |
| --- | --- |
| `asset_paths` | [assets/shared/asset_paths.py](assets/shared/asset_paths.py) |
| `audit_flora_edges` | [checks/audit_flora_edges.py](checks/audit_flora_edges.py) |
| `audit_resource_art` | [checks/audit_resource_art.py](checks/audit_resource_art.py) |
| `check_repository_hygiene` | [checks/check_repository_hygiene.py](checks/check_repository_hygiene.py) |
| `check_review_site` | [checks/check_review_site.py](checks/check_review_site.py) |
| `check_structure` | [checks/check_structure.py](checks/check_structure.py) |
| `create_class_dialogue_assets` | [assets/characters/create_class_dialogue_assets.py](assets/characters/create_class_dialogue_assets.py) |
| `create_companion_battle_assets` | [assets/characters/create_companion_battle_assets.py](assets/characters/create_companion_battle_assets.py) |
| `create_dialogue_npc_assets` | [assets/characters/create_dialogue_npc_assets.py](assets/characters/create_dialogue_npc_assets.py) |
| `create_special_npc_assets` | [assets/characters/create_special_npc_assets.py](assets/characters/create_special_npc_assets.py) |
| `create_story_dialogue_npc_assets` | [assets/characters/create_story_dialogue_npc_assets.py](assets/characters/create_story_dialogue_npc_assets.py) |
| `extract_beach_assets` | [assets/environments/extract_beach_assets.py](assets/environments/extract_beach_assets.py) |
| `extract_directional_character_assets` | [assets/characters/extract_directional_character_assets.py](assets/characters/extract_directional_character_assets.py) |
| `extract_goblin_monster_assets` | [assets/monsters/extract_goblin_monster_assets.py](assets/monsters/extract_goblin_monster_assets.py) |
| `extract_interior_assets` | [assets/environments/extract_interior_assets.py](assets/environments/extract_interior_assets.py) |
| `extract_interior_extra_props` | [assets/environments/extract_interior_extra_props.py](assets/environments/extract_interior_extra_props.py) |
| `extract_interior_seamless_tiles` | [assets/environments/extract_interior_seamless_tiles.py](assets/environments/extract_interior_seamless_tiles.py) |
| `extract_interior_tiles` | [assets/environments/extract_interior_tiles.py](assets/environments/extract_interior_tiles.py) |
| `extract_interior_wall_decor` | [assets/environments/extract_interior_wall_decor.py](assets/environments/extract_interior_wall_decor.py) |
| `extract_interior_wall_orientations` | [assets/environments/extract_interior_wall_orientations.py](assets/environments/extract_interior_wall_orientations.py) |
| `extract_item_icon_sheets` | [assets/items/extract_item_icon_sheets.py](assets/items/extract_item_icon_sheets.py) |
| `extract_location_assets` | [assets/environments/extract_location_assets.py](assets/environments/extract_location_assets.py) |
| `extract_mage_directional_assets` | [assets/characters/extract_mage_directional_assets.py](assets/characters/extract_mage_directional_assets.py) |
| `extract_monster_expansion_assets` | [assets/monsters/extract_monster_expansion_assets.py](assets/monsters/extract_monster_expansion_assets.py) |
| `extract_new_npc_class_assets` | [assets/characters/extract_new_npc_class_assets.py](assets/characters/extract_new_npc_class_assets.py) |
| `extract_npc_variation_assets` | [assets/characters/extract_npc_variation_assets.py](assets/characters/extract_npc_variation_assets.py) |
| `extract_player_village_growth_assets` | [assets/environments/extract_player_village_growth_assets.py](assets/environments/extract_player_village_growth_assets.py) |
| `extract_sheet_assets` | [assets/extract_sheet_assets.py](assets/extract_sheet_assets.py) |
| `generate_assets` | [assets/generate_assets.py](assets/generate_assets.py) |
| `generate_biome_decorations` | [assets/environments/generate_biome_decorations.py](assets/environments/generate_biome_decorations.py) |
| `generate_character_movement_state_animations` | [assets/characters/generate_character_movement_state_animations.py](assets/characters/generate_character_movement_state_animations.py) |
| `generate_cloud_assets` | [assets/environments/generate_cloud_assets.py](assets/environments/generate_cloud_assets.py) |
| `generate_dungeon_tiles` | [assets/environments/generate_dungeon_tiles.py](assets/environments/generate_dungeon_tiles.py) |
| `generate_field_connectors` | [assets/environments/generate_field_connectors.py](assets/environments/generate_field_connectors.py) |
| `generate_forest_mesh_assets` | [assets/environments/generate_forest_mesh_assets.py](assets/environments/generate_forest_mesh_assets.py) |
| `generate_monster_assets` | [assets/monsters/generate_monster_assets.py](assets/monsters/generate_monster_assets.py) |
| `generate_npc_assets` | [assets/characters/generate_npc_assets.py](assets/characters/generate_npc_assets.py) |
| `generate_overworld_fidelity_assets` | [assets/environments/generate_overworld_fidelity_assets.py](assets/environments/generate_overworld_fidelity_assets.py) |
| `generate_overworld_settlement_assets` | [assets/environments/generate_overworld_settlement_assets.py](assets/environments/generate_overworld_settlement_assets.py) |
| `generate_player_model_assets` | [assets/environments/generate_player_model_assets.py](assets/environments/generate_player_model_assets.py) |
| `generate_quest_objective_assets` | [assets/environments/generate_quest_objective_assets.py](assets/environments/generate_quest_objective_assets.py) |
| `generate_road_autotiles` | [assets/environments/generate_road_autotiles.py](assets/environments/generate_road_autotiles.py) |
| `generate_settlement_scene_assets` | [assets/environments/generate_settlement_scene_assets.py](assets/environments/generate_settlement_scene_assets.py) |
| `generate_village_management_assets` | [assets/environments/generate_village_management_assets.py](assets/environments/generate_village_management_assets.py) |
| `import_city_refinement_assets` | [assets/environments/import_city_refinement_assets.py](assets/environments/import_city_refinement_assets.py) |
| `import_dungeon_overworld_props` | [assets/environments/import_dungeon_overworld_props.py](assets/environments/import_dungeon_overworld_props.py) |
| `import_household_expansion` | [assets/environments/import_household_expansion.py](assets/environments/import_household_expansion.py) |
| `import_imagegen_action_strip` | [assets/characters/import_imagegen_action_strip.py](assets/characters/import_imagegen_action_strip.py) |
| `import_imagegen_castle_dungeon_assets` | [assets/environments/import_imagegen_castle_dungeon_assets.py](assets/environments/import_imagegen_castle_dungeon_assets.py) |
| `import_imagegen_cave_dungeon_tiles` | [assets/environments/import_imagegen_cave_dungeon_tiles.py](assets/environments/import_imagegen_cave_dungeon_tiles.py) |
| `import_imagegen_companion_directionals` | [assets/characters/import_imagegen_companion_directionals.py](assets/characters/import_imagegen_companion_directionals.py) |
| `import_imagegen_companion_walk_cycles` | [assets/characters/import_imagegen_companion_walk_cycles.py](assets/characters/import_imagegen_companion_walk_cycles.py) |
| `import_imagegen_inn_interior_assets` | [assets/environments/import_imagegen_inn_interior_assets.py](assets/environments/import_imagegen_inn_interior_assets.py) |
| `import_imagegen_modular_seating_assets` | [assets/environments/import_imagegen_modular_seating_assets.py](assets/environments/import_imagegen_modular_seating_assets.py) |
| `import_imagegen_npc_walk_cycles` | [assets/characters/import_imagegen_npc_walk_cycles.py](assets/characters/import_imagegen_npc_walk_cycles.py) |
| `import_town_portals` | [assets/environments/import_town_portals.py](assets/environments/import_town_portals.py) |
| `import_water_imagegen_props` | [assets/environments/import_water_imagegen_props.py](assets/environments/import_water_imagegen_props.py) |
| `make_directional_reference_sheets` | [assets/characters/make_directional_reference_sheets.py](assets/characters/make_directional_reference_sheets.py) |
| `music_score` | [audio/music_score.py](audio/music_score.py) |
| `musicgen` | [audio/musicgen.py](audio/musicgen.py) |
| `normalize_npc_portraits` | [assets/characters/normalize_npc_portraits.py](assets/characters/normalize_npc_portraits.py) |
| `process_mage_cast_animation_sheet` | [assets/characters/process_mage_cast_animation_sheet.py](assets/characters/process_mage_cast_animation_sheet.py) |
| `process_walk15_animations` | [assets/characters/process_walk15_animations.py](assets/characters/process_walk15_animations.py) |
| `recolor_mage_down_blue_eyes` | [assets/characters/recolor_mage_down_blue_eyes.py](assets/characters/recolor_mage_down_blue_eyes.py) |
| `regenerate_character_walk_animations` | [assets/characters/regenerate_character_walk_animations.py](assets/characters/regenerate_character_walk_animations.py) |
| `regenerate_universal_assets` | [assets/regenerate_universal_assets.py](assets/regenerate_universal_assets.py) |
| `slice_imagegen_village_assets` | [assets/environments/slice_imagegen_village_assets.py](assets/environments/slice_imagegen_village_assets.py) |
| `slice_resource_props` | [assets/items/slice_resource_props.py](assets/items/slice_resource_props.py) |
| `test_asset_helpers` | [tests/test_asset_helpers.py](tests/test_asset_helpers.py) |
| `test_music_score` | [tests/test_music_score.py](tests/test_music_score.py) |
| `test_repository_hygiene` | [tests/test_repository_hygiene.py](tests/test_repository_hygiene.py) |
| `test_repository_reviews` | [tests/test_repository_reviews.py](tests/test_repository_reviews.py) |
| `test_universal_cutout` | [tests/test_universal_cutout.py](tests/test_universal_cutout.py) |
| `test_universal_cutout_real_sheets` | [tests/test_universal_cutout_real_sheets.py](tests/test_universal_cutout_real_sheets.py) |
| `universal_cutout` | [assets/shared/universal_cutout.py](assets/shared/universal_cutout.py) |
| `verify_universal_cutout_assets` | [checks/verify_universal_cutout_assets.py](checks/verify_universal_cutout_assets.py) |

## Java and PowerShell asset tools

Run from `Java/`. Java importers use `java tools/assets/<family>/<Name>.java [arguments]`; PowerShell tools use `powershell -NoProfile -ExecutionPolicy Bypass -File tools/assets/<family>/<name>.ps1 [arguments]`.

| Tool | Location |
| --- | --- |
| `ImportCharacterPass.java` | [assets/characters/ImportCharacterPass.java](assets/characters/ImportCharacterPass.java) |
| `ImportCharacterSheets.java` | [assets/characters/ImportCharacterSheets.java](assets/characters/ImportCharacterSheets.java) |
| `ImportRegionalNpcs.java` | [assets/characters/ImportRegionalNpcs.java](assets/characters/ImportRegionalNpcs.java) |
| `NamedNpcPortraits.java` | [assets/characters/NamedNpcPortraits.java](assets/characters/NamedNpcPortraits.java) |
| `ReviewCharacterPass.java` | [assets/characters/ReviewCharacterPass.java](assets/characters/ReviewCharacterPass.java) |
| `import_built_dungeons.ps1` | [assets/environments/import_built_dungeons.ps1](assets/environments/import_built_dungeons.ps1) |
| `import_dungeon_floor_replacements.ps1` | [assets/environments/import_dungeon_floor_replacements.ps1](assets/environments/import_dungeon_floor_replacements.ps1) |
| `import_modular_dungeon_assets.ps1` | [assets/environments/import_modular_dungeon_assets.ps1](assets/environments/import_modular_dungeon_assets.ps1) |
| `import_regional_caverns.ps1` | [assets/environments/import_regional_caverns.ps1](assets/environments/import_regional_caverns.ps1) |
| `ImportCleanFlora.java` | [assets/environments/ImportCleanFlora.java](assets/environments/ImportCleanFlora.java) |
| `ImportNatureSheets.java` | [assets/environments/ImportNatureSheets.java](assets/environments/ImportNatureSheets.java) |
| `ImportSettlementRefresh.java` | [assets/environments/ImportSettlementRefresh.java](assets/environments/ImportSettlementRefresh.java) |
| `import_resource_art.ps1` | [assets/items/import_resource_art.ps1](assets/items/import_resource_art.ps1) |
| `ImportMonsterAttacks.java` | [assets/monsters/ImportMonsterAttacks.java](assets/monsters/ImportMonsterAttacks.java) |

## Status and dependencies

All listed tools are active or retained manual regeneration utilities. Java requires a JDK supporting Java 21; Python asset tools use Pillow, music generation uses NumPy; see `requirements.txt`. Repository/structure checks use only the standard library plus Git. Java tools have no new third-party runtime dependency.

Three historical tools are [archived](../archive/pending-deletion/retired-tools-2026-09-23/README.md). Age alone does not establish that an active regeneration dependency is obsolete. No automatic age-based retirement is configured.

Game-dependent exporters are now in [src/review](../src/README.md). Interactive sites remain in [reviews](reviews/README.md).
