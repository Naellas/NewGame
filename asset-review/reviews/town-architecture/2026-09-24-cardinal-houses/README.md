# Cardinal Northwatch residences

Actual unedited GamePanel captures at 100% zoom, noon, high quality. East/west
houses have horizontal rooflines and entrances projecting directly sideways.
The north view shows a rear elevation with an approach to the hidden far doorway.
Existing south-facing art is unchanged. Agent visual review completed; user
aesthetic acceptance pending. This batch covers one house family, not every town.

Reproduce from repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -OutputDirectory temp/cardinal-houses/classes
```

From Java/ with Java 21:

```powershell
java '-Djava.awt.headless=true' -cp temp/cardinal-houses/classes com.alderfall.game.CardinalBuildingTest
java '-Djava.awt.headless=true' -cp temp/cardinal-houses/classes com.alderfall.game.RegionalSettlementTest --layout-only
java '-Djava.awt.headless=true' -cp temp/cardinal-houses/classes com.alderfall.game.TownLayoutTest --render
```

Exporter writes to Java/temp/town-layout/review; three selected captures are here.
Validation passed: three seeds, directional approaches/road connections, solid
collision and transparent sprites, wrong-side entry rejection, stable interior
identity, explicit interaction, keyboard interaction, continuous walking entry
and return positions. Town architecture sizing/selection, exterior furnishing,
building collision and NPC navigation regressions passed; normal build passed.
RegionalBuildingsTest also passed with `-XX:TieredStopAtLevel=1` after its first
run hit a native HotSpot access violation in java.util.stream. The crash log is
retained in Java/temp/cardinal-houses; the other diagnostics used default Java 21.

Repository policy unit tests and remaining structure/review checks passed.
Full check.ps1 stops on the unrelated .vscode/settings.json root-policy finding;
that file and the baseline were left unchanged.

[Source images, exact prompts and checksum import](../../../../art-source/towns/2026-09-24-cardinal-houses/README.md).
