# Town density review

Actual GamePanel captures from TownLayoutTest --render, daytime Briarbridge.
Reviewed for added regional homes, market/garden foot traffic, open circulation
and suburban housing. Four homes and twelve street residents are added per town.
Existing barn paddocks, specialty yards, public spaces and fountains are retained.

Reproduce from Java: scripts/build.ps1 -IncludeTests -OutputDirectory temp/town-density/classes,
then java -Djava.awt.headless=true -cp temp/town-density/classes com.alderfall.game.TownLayoutTest --render.
All seven town captures are produced in temp/town-layout/review; only these two
representative views are curated here. No new artwork was generated.

Validation: production build, TownLayoutTest (three seeds plus renderer),
RegionalSettlementTest --layout-only, TownExteriorRoomsTest and
TownNpcNavigationTest. Repository policy tests pass; the umbrella check stops
at the existing unapproved-root .vscode/settings.json. Review links and structure
checks pass individually. The local JVM required process-local JIT retries;
launch settings were not changed.
