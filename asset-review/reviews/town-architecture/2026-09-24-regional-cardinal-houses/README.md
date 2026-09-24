# Cardinal houses across the town regions

Seven selected actual GamePanel captures, one per town, at noon and 100% zoom.
They show the new regional side-facing houses in their residential surroundings.
The full 21 directional views remain in Java/temp/town-layout/review; all 21
runtime sprites are also displayed first in the existing town architecture gallery.

New art: 18 imagegen sprites across Briarbridge, Ironvale, Moonspire, Reedwatch,
Embermarket and Greyharbor. Northwatch retains its previous three variants.
The sets cover river, northern, Hearthlands, Fenland, desert and coastal towns.
All doors face cardinal directions; north doors are on the far side behind roofs.
Named institutions retain dedicated art. Each town still uses south-facing homes.

From repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -OutputDirectory temp/regional-cardinal/classes
```

From Java/, with Java 21:

```powershell
java '-XX:TieredStopAtLevel=1' '-Djava.awt.headless=true' -cp temp/regional-cardinal/classes com.alderfall.game.CardinalBuildingTest
java '-XX:TieredStopAtLevel=1' '-Djava.awt.headless=true' -cp temp/regional-cardinal/classes com.alderfall.game.TownArchitectureTest
java '-XX:TieredStopAtLevel=1' '-Djava.awt.headless=true' -cp temp/regional-cardinal/classes com.alderfall.game.TownLayoutTest --render
```

The limited JIT tier avoids the intermittent native JVM crashes observed in prior
diagnostic runs; it changes no game configuration. RegionalSettlementTest
--layout-only, TownExteriorRoomsTest, BuildingCollisionTest and TownNpcNavigationTest
also cover street reachability, furnishings, solid bases and resident movement.
CardinalBuildingTest checks all seven towns over three seeds, regional RGBA art,
entry direction, road approaches, interior identity, three entry methods and exits.

Agent visual review completed; user aesthetic acceptance pending. Repository
check.ps1 stops at the unrelated .vscode/settings.json root-policy finding after
passing thirteen policy tests; the baseline and that file are unchanged.

[Originals, exact prompts, checksums and import recipe](../../../../art-source/towns/2026-09-24-regional-cardinal-houses/README.md).
[Interactive gallery](../../../../Java/tools/reviews/town-architecture/index.html).
