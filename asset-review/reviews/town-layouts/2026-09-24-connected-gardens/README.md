# Connected garden boundaries and fountains

Unedited actual Swing renderer captures at noon. Northwatch's wall-bay view
shows an L-shaped reading court, connected hedge corners and open iron gates;
the detail views show northern granite and Sunrealm sandstone fountains.
Overview zoom is 40%; detail zoom is 100%. Local display settings can affect output.

Reproduce from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -OutputDirectory temp/town-gardens/classes
```

Then from Java/, using Java 21:

```powershell
java '-Djava.awt.headless=true' -cp temp/town-gardens/classes com.alderfall.game.TownLayoutTest --render
java '-Djava.awt.headless=true' -cp temp/town-gardens/classes com.alderfall.game.ConnectedBoundaryTest
java '-Djava.awt.headless=true' -cp temp/town-gardens/classes com.alderfall.game.TownExteriorRoomsTest
java '-Djava.awt.headless=true' -cp temp/town-gardens/classes com.alderfall.game.RegionalSettlementTest --layout-only
```

Bulk captures remain in Java/temp/town-layout/review. Three selected frames are
retained here. Agent inspected the actual renderer; user aesthetic acceptance
is pending. Fountains are static art. Gates are permanently open passage props.

Validation passed: sixteen cardinal boundary masks at three scales, mixed
horizontal/vertical IDs, removal updates, four gate styles and fountain basin
collision; furnishing reservations and town spacing over three seeds; regional
reachability and saved-position recovery. ConnectedFurnitureTest,
BuildingCollisionTest and TownNpcNavigationTest also passed. Normal main build
passed. Repository check.ps1 passed its thirteen policy tests but stopped on the
unrelated root .vscode/settings.json violation, which was left intact.

Sources:
[boundary modules](../../../../art-source/towns/2026-09-24-exterior-modules/README.md),
[imagegen entrances and fountains](../../../../art-source/towns/2026-09-24-garden-features/README.md).
