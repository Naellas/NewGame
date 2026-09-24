# Composed street gardens

Actual GamePanel captures, inspected by the agent; user acceptance pending.
Briarbridge shows compact street seating with regional trees and herb planters.
Moonspire shows the same composition at the edge of its landmark forecourt.
This is a street-furnishing pass. It does not reproduce the reference image's
large enclosed landmark grounds; several existing forecourts lack clear space.

Reproduce from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -OutputDirectory temp/town-composition/classes
```

From Java/, using Java 21:

```powershell
java -XX:TieredStopAtLevel=1 -XX:ActiveProcessorCount=2 '-Djava.awt.headless=true' -cp temp/town-composition/classes com.alderfall.game.TownLayoutTest --render
```

Captures are written to temp/town-layout/review/*-street-garden.png. All seven
towns receive composed planted seating; the larger variant has a hedge back.
Whole footprints must pass existing road, entrance, worker and prop reservations.

Validation: production and diagnostic builds; TownExteriorRoomsTest,
TownLayoutTest, RegionalSettlementTest --layout-only, TownNpcNavigationTest.
Repository policy tests, review links/archive checksums and structure checks pass.
The full check.ps1 entrypoint stops on pre-existing .vscode/settings.json.
