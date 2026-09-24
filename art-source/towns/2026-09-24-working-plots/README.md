# Working town plots: barns and stables

Three cardinal south-facing barn/stable sprites generated with the built-in imagegen tool.
Temperate oak/slate is shared by Briarbridge, Moonspire, Reedwatch and Greyharbor;
northern granite/pine/snow by Ironvale and Northwatch; sandstone/reed by Embermarket.
Exact prompts and original imagegen paths are in generation.json and prompt files.
Original RGBA files are preserved byte-for-byte. Alpha edit candidates are retained
under candidates; original alpha was verified and originals selected for runtime.

Import from repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File art-source/towns/2026-09-23-town-architecture/import.ps1 -BatchDirectory art-source/towns/2026-09-24-working-plots
```

The checksum manifest records all three output paths and source inputs. The shared
importer only copies verified original bytes; no raster postprocessing is needed.
One new barn in each town uses feed-storage interiors and a fenced paddock.
Review: agent inspected; user acceptance pending. See regional-planting town review.
