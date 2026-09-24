# Placement rules and editor-style offsets

Actual GamePanel captures; agent visual review completed, user acceptance pending.
These supersede the earlier composed-streets bench placements.

- [Moonspire landmark](town_moonspire-landmark.png): the wall-adjacent bench is gone; seating faces open paving.
- [Briarbridge square](town_briarbridge-square-seating.png): seating belongs to the civic square.
- [Ironvale](town_ironvale-street-garden.png) and [Greyharbor](town_greyharbor-street-garden.png) street gardens: complete hedge rails enclose aligned trees.

Rules and limits: [town architecture](../../../../Java/docs/town-architecture.md).
The existing plot layout is retained. This is not a reconstruction of the user's
full enclosed house garden. Narrow plots are skipped rather than decorated anyway.

Build from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -OutputDirectory temp/town-placement/classes
```

Render from Java/ with Java 21:

```powershell
java -XX:TieredStopAtLevel=1 -XX:ActiveProcessorCount=2 '-Djava.awt.headless=true' -cp temp/town-placement/classes com.alderfall.game.TownLayoutTest --render
```

Output: Java/temp/town-layout/review. Selected files here are unmodified captures.
Validation: TownPlacementRulesTest, TownExteriorRoomsTest, TownLayoutTest,
RegionalSettlementTest --layout-only, TownNpcNavigationTest, EditorPropCollisionTest,
ConnectedBoundaryTest and production build. Full repository checks still report
pre-existing .vscode/settings.json; review/structure checks run separately.
