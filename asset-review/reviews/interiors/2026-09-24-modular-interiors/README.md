# Modular interior materials and room graphs

Review status: implementation and captures complete; player visual acceptance pending.

This review pairs a seed with an open hall (`home-open-hall.png`) against a
partitioned home plan (`home-partitioned.png`), then shows a partitioned trade
interior (`blacksmith-partitioned.png`). The continuous timber side rails, joined
wall faces, trim, baseboards, posts and floor material come from the uploaded
interior sheets. The back wall is two tiles high. Seeded partitions divide rooms
with connected openings, while the original functional furnishing groups remain.

## Reproduce

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -OutputDirectory temp/modular-interiors/classes -IncludeTests -IncludeReviews
java '-Djava.awt.headless=true' -cp Java/temp/modular-interiors/classes com.alderfall.game.InteriorDesignPreview Java/temp/modular-interiors/preview-home home
java '-Djava.awt.headless=true' -cp Java/temp/modular-interiors/classes com.alderfall.game.InteriorDesignPreview Java/temp/modular-interiors/preview-blacksmith blacksmith
```

The previews capture seeds 0 through 3. Only the three named review frames are
curated here; the remaining generated frames stay in ignored scratch space.

The source crop recipe and original inputs are documented in
[`art-source/interiors/2026-09-24-modular-materials`](../../../../art-source/interiors/2026-09-24-modular-materials/README.md).
Run the layout diagnostic to check room connectivity and furnishing groups, and
`ConnectedInteriorWallsTest` to check material seams, joined doors and chunk-edge
rendering. The screenshots are visual evidence, not a claim that the art direction
has final acceptance.
