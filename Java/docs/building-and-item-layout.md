# Building and item organization

Player construction art is separated under
`environments/settlements/player_village/buildings/<style>` at the user's request.
The 72 relocated PNGs retain their bytes and IDs. Historical placement aliases
resolve directly to the player-owned paths; NPC architecture remains under
`city/buildings/<region>`. See [settlement building](settlement-building.md).

Consolidated 96 building-related files
under environments/settlements/city/buildings by region. Shared buildings are
stored once. Sorted all 358 existing
item files by equipment slot, weapon type, material/consumable purpose, or atlas.
Filenames, PNG pixels, asset IDs and gameplay statistics are unchanged.

The exact old-to-new mapping is config/asset-placements.json. Importers and direct
review links use the new destinations. Structural checks reject missing canonical
files, reintroduced old paths, and new loose PNGs in the items root.

See [item categories](../assets/items/README.md) and
[building regions](../assets/environments/settlements/city/buildings/README.md).
