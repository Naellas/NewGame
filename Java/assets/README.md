# Runtime asset layout

Asset filenames are stable game IDs. This organization changes storage paths,
not identities, pixels, animation timing, or sidecar contents.

```text
characters/
  player/base/
  player/classes/<class>/animations/
  companions/<companion>/animations/
  npcs/townsfolk/                 # common roles, portraits, regional variants
  npcs/townsfolk/animations/
  npcs/townsfolk/regional/
  npcs/story/npcs/<character>/animations/
  monsters/animations/
  shared/animations/              # legacy cross-roster character-refresh batches
environments/
  terrain/common/
  terrain/biomes/<biome>/
  terrain/roads/
  settlements/city/
    buildings/, props/, walls/, terrain/, landmarks/
    regional/, folklore/, overworld/
  settlements/player_village/
  locations/quest/
  interiors/props/                # furniture, workstations, household decoration
  props/nature/                   # flora, ground detail, dungeon decoration
  battle/
effects/animations/
effects/weather/
items/
music/
sfx/
source/                          # grandfathered originals; new originals use art-source/
```

Use character type and owner for character art; keep animations and their
`.frames`/`.framebounds` beside that owner. Use environment/location for terrain
and buildings. Regional variation belongs below its family, rather than copying
one shared image into multiple regional folders. Appliances/workstations belong
with interior props; an appliance-specific split can be added when importers
route to it consistently. Item materials/tiers remain item metadata rather than
duplicated files by region.

`config/asset-layout.json` (relative to `Java/`) records old family -> current
subtree mappings. Python tools share `tools/assets/shared/asset_paths.py`.
`AssetCatalog` scans explicit runtime families in the original precedence order.
All 3,483 catalog IDs were compared before/after migration and resolved to the
same SHA-256 image contents. Six pre-existing duplicate stems are grandfathered
by exact paths in `config/asset-duplicate-stems.json`; new ambiguities fail checks.

Do not rename IDs or split shared animations without updating their consumers.
The legacy cross-roster batches still contain source material; moving that out
requires a separate source/provenance migration. New original art and prompts
belong in repository-root `art-source/<topic>/<batch>/`, outside runtime lookup.
