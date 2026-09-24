# Regional cardinal houses

Eighteen new original sprites generated with the built-in imagegen tool on
September 24, 2026: west, east and north views for Briarbridge, Ironvale,
Moonspire, Reedwatch, Embermarket and Greyharbor. Northwatch reuses its existing
three cardinal views. Existing south-facing houses remain in every town.

Each `*-prompt.txt` is the exact generation prompt. Reference 1 was the matching
town's existing `town_<name>_house.png`; reference 2 was Northwatch's matching
cardinal sprite, used for projection and threshold placement. Both references
are under Java/assets/environments/settlements/city/buildings, in their regional
folders. Original generated RGBA files are preserved in `originals/`.

Architecture covers river-town plaster/timber and slate, northern granite/timber
and snowy slate, Hearthlands red-tiled scholarly homes, Fenland reed-roof stilt
homes, Sunrealm sandstone/parapets, and coastal timber/slate homes. Long rooflines
are horizontal and doors project directly west/east. North entrances are on the
far side, behind the roof. No diagonal or isometric rotation is used.

From repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File art-source/towns/2026-09-23-town-architecture/import.ps1 -BatchDirectory art-source/towns/2026-09-24-regional-cardinal-houses
```

Requires PowerShell. The existing importer copies original bytes, verifies source
SHA256 checksums in `manifest.json`, and refuses to overwrite different files.
Outputs: `town_<name>_house_<west|east|north>.png` under
`Java/assets/environments/settlements/city/buildings/<region>/`. Region folders
are river, north, hearth, fen, sun and freehold respectively. No pixels are
rotated, mirrored, recolored or resampled by import.

Review status: generated outputs inspected; in-game review and validation recorded
in the corresponding dated town-architecture review. User aesthetic acceptance
is pending. This batch adds residential variants; named institutions retain their
dedicated sprites.
