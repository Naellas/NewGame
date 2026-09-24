# Cardinal Northwatch houses

Generated with the built-in imagegen tool on September 24, 2026. Material reference:
`Java/assets/environments/settlements/city/buildings/freehold/town_northwatch_house.png`.
Three independent sprites supply west, east and north; the existing south sprite
is retained. Exact prompts: `west_cardinal-prompt.txt`, `east_cardinal-prompt.txt`,
and `north-prompt.txt`. Original PNGs are in `originals/`.

Selected side views have horizontal ridges and edge-on entrances. Earlier diagonal
east and front-left west candidates were rejected before import. The north view
shows the back wall and far-side entrance roof; its doorway is hidden by the roof.

From repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File art-source/towns/2026-09-23-town-architecture/import.ps1 -BatchDirectory art-source/towns/2026-09-24-cardinal-houses
```

The existing importer now accepts an optional batch directory. Requires PowerShell;
copies original RGBA bytes using `manifest.json` source checksums, verifies identical
existing files, and refuses to overwrite different assets. No rotation, mirroring,
repainting or resampling. Outputs in
`Java/assets/environments/settlements/city/buildings/freehold/`:
`town_northwatch_house_west.png`, `town_northwatch_house_east.png`,
`town_northwatch_house_north.png`.

Review: agent inspected generated art and game captures; user aesthetic acceptance
pending. This first family is assigned to three Northwatch homes; other families
retain existing artwork. CardinalBuildingTest covers entry and exit behavior.
