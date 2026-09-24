# Settlement progression and management review

Captured 2026-09-24 from `SettlementPanelTest` at 1920x1080. These are actual
Swing game renders, with free-building enabled in the isolated test fixture.
The fixture intentionally has no stationed companions.

- `manage.png`: category filters, town milestones, building upgrade card.
- `building.png`: six-stage comparison, next improvement and staffing panel.
- `build.png`: building catalogue.
- `terrain.png`: terrain and elevation controls.

Reproduce from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -OutputDirectory temp/settlement-identity/classes
Set-Location Java
java -XX:TieredStopAtLevel=1 -Djava.awt.headless=true -cp temp/settlement-identity/classes com.alderfall.game.SettlementPanelTest
```

Use Java 21. Captures are written under `Java/temp/settlement-identity/`.
Visual review accepted the panel hierarchy and clear six-stage material changes.
Automated tests passed for category filtering, both upgrade actions, returning
to Manage, terrain buttons, all 108 distinct building assets, tier economics,
terrain protection, expansion and save/load. Production compilation passed.
All 72 relocated asset checksums match their originals.

Structure and review-link checks passed. The required `check.ps1` run passed
13 policy tests but stopped on the existing unapproved `.vscode/settings.json`
root file; no baseline was changed.

[Full building comparison](../../../../Java/tools/reviews/player-settlement/index.html)
and [source sheets, exact prompts and import recipe](../../../../art-source/settlement-growth/2026-09-24-specialist-tiers/README.md).
