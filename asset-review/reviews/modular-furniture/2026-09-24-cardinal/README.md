# Modular furniture review

`seating-directions.png`: all seven models, columns north/east/south/west.
`market-and-fences.png`: actual Swing world renderer with four connected market
colors, village/farm/iron fence enclosures and cardinal armchairs.

Reproduce from repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -IncludeReviews -OutputDirectory temp/modular-marketplace/classes
Set-Location Java
java -Djava.awt.headless=true -cp temp/modular-marketplace/classes com.alderfall.game.ModularFurniturePreview
```

Exporter output is scratch under Java/temp/modular-marketplace/captures. These two
selected captures are curated evidence. Acceptance: cardinal seating directions,
transparent backgrounds, joined counter/canopy seams, same-material fence joins.
Generated originals and prompts are in
[the source batch](../../../../art-source/modular-marketplace/2026-09-24-cardinal-furniture/README.md).

Validation: Java build, ModularFurnitureTest, ConnectedFurnitureTest and
FurnitureQuestTest. Review-link/checksum and tool/class structure checks passed.
The root check script reports the unrelated existing `.vscode/settings.json` as
an unapproved root file; no baseline or ignore rules were changed.
