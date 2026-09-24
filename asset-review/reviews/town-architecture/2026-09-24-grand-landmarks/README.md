# Grand landmark review

Actual GamePanel close-ups, generated with TownLayoutTest --render from Java.
Build: scripts/build.ps1 -IncludeTests -OutputDirectory temp/town-landmarks/classes.
Run: java -Djava.awt.headless=true -cp temp/town-landmarks/classes com.alderfall.game.TownLayoutTest --render.
The diagnostic writes all seven towns to temp/town-layout/review; selected
landmark close-ups are retained here after visual inspection.

Scope: three new grand institutions, a botanical glasshouse, larger landmark
scale across seven towns, wider frontage, decorated regional forecourts and two
regional village reuses. Reflecting ponds were deferred because clear basins and
banks did not fit without compromising movement. Existing fountains remain.

Validation: TownArchitectureTest, TownLayoutTest, RegionalSettlementTest,
RegionalBuildingsTest, BuildingCollisionTest, TownExteriorRoomsTest and
TownNpcNavigationTest. Production build passes. Full repository check stops on
existing .vscode/settings.json; policy tests pass. Review-link and structure
checks run separately. The local JVM is intermittently unstable; successful
retries use process-local JIT settings, without altering game launch options.

Agent reviewed; user acceptance pending.
