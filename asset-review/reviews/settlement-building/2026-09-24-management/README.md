# Settlement management revision

Actual game captures from `SettlementPanelTest`, at 1920x1080 with creative
construction enabled only in the test fixture:

- `catalogue.png`: spacious building catalogue and category tabs.
- `build.png`: quick placement sidebar with revised costs.
- `workers.png`: workplace selection, housing status and relevant skill.
- `housing.png`: resident and bed capacity panel.
- `manage.png`: upgrades, daily forecasts and production summary.
- `building.png`: six-stage comparison and resident/staff details.
- `board.png`: new resized town-board sprite and town growth interface.
- `terrain.png`: retained terrain editing controls.

Reproduction from repository root:

```powershell
$env:JAVA_TOOL_OPTIONS='-XX:TieredStopAtLevel=1'
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -OutputDirectory temp/settlement-identity/classes
Set-Location Java
java -Djava.awt.headless=true -cp temp/settlement-identity/classes com.alderfall.game.SettlementPanelTest
```

Use Java 21. Test captures are written under `Java/temp/settlement-identity`.
Review accepts the catalogue hierarchy, clearer worker assignment, housing
capacity and clean town-board silhouette. Economy tests cover housing loss,
multiple worker slots, skill/equipment effects, rarer output, increasing costs,
and save/load of assignments and prop offsets.

The cache test compares locally edited terrain against a fresh render exactly.
In the test scene, a new building rebuilt 33/48 visible chunks (about 130 ms);
an upgrade and ordinary prop placement rebuilt zero terrain chunks. This is a
controlled rendering measurement, not an end-to-end frame-time guarantee.
Map expansion still rebuilds the view after coordinates change.

Production compilation and SettlementEconomyTest, SettlementCacheTest,
SettlementPanelTest, EditorResponsivenessTest and WorldElevationTest pass.
Structure and review-link checks pass. The required repository check passes
13 policy tests, then stops on the pre-existing `.vscode/settings.json`
unapproved-root finding. No ignore or baseline was changed.

[Board sources, prompt and import recipe](../../../../art-source/settlement-management/2026-09-24-town-board/README.md)
and [gameplay documentation](../../../../Java/docs/settlement-building.md).
