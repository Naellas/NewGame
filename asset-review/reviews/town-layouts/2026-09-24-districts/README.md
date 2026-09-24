# Town districts and suburbs

Actual Swing game captures of the September 24 layout pass, at noon and 40% zoom.
Northwatch shows the civic/residential and craftsmen/market grouping, public-space
band, and the southern suburb beyond the inset wall. Moonspire shows the same
land-use plan with its scholarly program and regional vegetation.

Reproduce from Java/ with Java 21:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 -IncludeTests -OutputDirectory temp/town-layout/classes
java '-Djava.awt.headless=true' -cp temp/town-layout/classes com.alderfall.game.TownLayoutTest --render
```

The exporter writes fourteen views to temp/town-layout/review. Only the three
selected PNGs are retained here; these are unedited renders, not new game assets.

Review status: agent visually inspected Northwatch's public spaces and suburbs,
wall proportions, and Moonspire's regional treatment. User aesthetic acceptance
is pending. TownLayoutTest and RegionalSettlementTest passed three seeds;
TownArchitectureTest, RegionalBuildingsTest, BuildingCollisionTest and
TownNpcNavigationTest passed. Repository checks and the normal build passed.

Scope: seven named towns receive districts and suburbs. Capitals retain their
existing layouts; all city maps receive the wall-rendering scale adjustment.
Wall collision remains one tile, beneath a larger rendered silhouette.
