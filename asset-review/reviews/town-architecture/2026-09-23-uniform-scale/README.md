# Uniform town building scale and gateway alignment

112 actual Swing game captures: one per town-specific sprite (84) and one per town
gateway (28), rendered at daytime and 80% zoom. Review through
[the town gallery](../../../../Java/tools/reviews/town-architecture/index.html).

Reproduce from Java/ using Java 21:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 -IncludeTests -OutputDirectory temp/uniform-buildings/classes
java -Djava.awt.headless=true -cp temp/uniform-buildings/classes com.alderfall.game.TownArchitectureTest --render
```

Acceptance: common visible bounding-box area across town buildings, preserved
sprite proportions and door anchoring; no shrinking whole buildings to fit narrow
lots. Reedwatch east gate sits on the east-west road at (34,12). Tall/narrow art
retains its silhouette. Roof edges may overhang narrow legacy footprints, whose
collision and entrances remain unchanged. Prior variety captures are retained
in the sibling 2026-09-23-variety directory for comparison.

Automated checks cover all 84 sprites, institutions, PNG padding and resolution,
zoom scaling, lot independence, three layout seeds, continuous walls and gates,
building entrances, collision, NPC navigation and saved-position recovery.
Representative Reedwatch, Briarbridge and Moonspire frames were visually reviewed.
