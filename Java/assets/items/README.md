# Item assets

Equipment is grouped by type. Armor slots follow EquipmentCatalog, so hoods and
veils used in the helmet slot live in helmets; torso armor and robes live in armors.
Named weapon sprites use their described type. Visual inspection identifies
The Bellringer as a hammer (maces) and The Twelfth Silence as a sword.

Shared fallback icons live in icons. Material resources, consumables, gathering
tools and placeable icons have separate folders. Existing composite sheets and
their prompt notes live in atlases; new original source art belongs in art-source.

Names remain runtime IDs. config/asset-placements.json records moved paths;
Python importers use tools.assets.shared.asset_paths.asset_file to resolve them.
New art should be written directly to its category; never recreate items/generated.
