# Swamp wetland review ? 2026-09-24

In-game renders: [inland pools and canopy](swamp.png), [wet banks and reeds](swamp-banks.png).

Reproduce from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -IncludeReviews -OutputDirectory temp/swamp/classes
cd Java
java -Djava.awt.headless=true -cp temp/swamp/classes com.alderfall.game.LayeredTerrainPreview --swamp temp/swamp/render
```

The existing preview exporter uses a fresh world, noon, High quality, 100% zoom,
and cameras at (229,175) and (235,163). Matching scratch captures are overwritten.

Reviewed: olive shallow pools and wet margins blending into blue open water;
peat mud patches; cypress and willow canopy; new broad cattail/sedge clumps;
walk-through low vegetation; clear road surfaces. Shared supply-cache ID now
uses detailed camp supplies; rejected block artwork is archived with checksums.
These renders are accepted for integration, with final aesthetic review by the player pending.

Source and complete prompt: [flora batch](../../../../art-source/swamp/2026-09-24-wetland-flora/README.md).

Validation: production and diagnostic builds; SwampEnvironmentTest,
WaterDepthTest, LayeredTerrainTest, BridgeRenderingTest, SubtileCollisionTest,
RenderCacheTest, PropPlacementTest; WorldGenerationTest for seeds 0, 1, 42,
2026 and -17 (repeatability and destination access). The default test world
contains 14,869 marsh tiles, 322 shallow tiles, 864 cypress/willow props on
marsh, and 840 new sedge props. Counts describe the resulting world, not deltas.

One parallel test run encountered a transient IncompatibleClassChangeError in
TownExteriorRooms.free during world initialization; the same collision test
passed before and on immediate rerun. No town code was changed for this task.
Repository policy tests, review links/archive checksums, and structural checks
pass. The combined check.ps1 stops on pre-existing unapproved-root:
.vscode/settings.json; that unrelated user file and the baseline were preserved.

Existing saved or editor-authored terrain is not forcibly rewritten to add pools.
The water palette and replacement supply artwork apply when rendered; new
world generation supplies the new pond and decoration layout.
