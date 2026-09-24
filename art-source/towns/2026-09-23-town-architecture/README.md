# Town architecture - 2026-09-23

Fourteen sprites generated with the built-in image_gen tool: seven town civic
landmarks and seven residential styles across six regions. manifest.json records
the exact prompts, original files, destinations, image dimensions and SHA-256.
Original PNG alpha is preserved byte-for-byte; no resizing or chroma keying.

Import/reproduce from the repository root:

```powershell
& ./art-source/towns/2026-09-23-town-architecture/import.ps1
```

The import copies originals to the listed runtime destinations. It skips identical
existing files and refuses to overwrite different content. No dependencies beyond
PowerShell. Originals are source material; only runtime copies are game assets.

Town-specific sprite selection lives in RegionalSettlementIdentity.townBuildingAsset
and is called by both GamePanel building render paths. Existing geometry uses the
unchanged buildingAsset method. Lots, door coordinates, interiors, managed-village
upgrades, institution sprites and save keys are preserved. Earlier shared art
remains in use outside the selected towns.

Review: Java/tools/reviews/town-architecture/index.html. Generation outputs were
visually inspected for style, isolated silhouettes, entrance placement and alpha.
TownArchitectureTest checks all assignments, image alpha and renderer selection;
its --render option produces actual game captures for both art types in each town.
