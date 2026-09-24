# Purpose-built trade interiors

Selected actual game-render captures: blacksmith, carpenter, bakery, alchemy,
study/library and general shop. Seed zero shows the core layout; blacksmith-1
shows the vestibule variant. Additional footprint captures remain in scratch.

Acceptance: joined storage fronts and worktops have continuous tops/bases and
outside end caps; shop contents match the profession. The forge has adjacent fuel,
water and anvil, separate finishing and service areas. Libraries have reading rows,
lectern, support pillars and a screened nook; rugs belong to seated customer areas.
All images use the real world renderer rather than composited concept art.

Reproduce from repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -IncludeReviews -OutputDirectory temp/trade-interiors/classes
Set-Location Java
foreach ($theme in @('blacksmith','carpenter','bakery','alchemy','study','shop')) {
    java '-Djava.awt.headless=true' -cp temp/trade-interiors/classes com.alderfall.game.InteriorDesignPreview temp/trade-interiors/captures $theme
}
```

The exporter writes four footprint seeds per theme. Curated captures here are
copied from Java/temp/trade-interiors/captures after inspection.
Original art, exact prompts and import commands are in the
[source batch](../../../../art-source/trade-interiors/2026-09-24-connected-workrooms/README.md).
Gameplay and connection limits are documented in
[trade interiors](../../../../Java/docs/trade-interiors.md).

Validation: 1,620 layout compositions (all floor reachable, residents/aisles clear,
no rejected furniture or navigation repair deletions), 324 furnishing plans,
54 generated interior placement checks, profession/crafting routing, 26 connected
families at three scales, and the existing furniture quest flow.
Repository policy tests pass; the top-level hygiene script still reports the
unrelated `.vscode/settings.json` root placement. Baselines/ignores are unchanged.
