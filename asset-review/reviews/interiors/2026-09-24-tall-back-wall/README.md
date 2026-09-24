# Taller back wall prototype

Review status: prototype captured; awaiting visual review.

This capture shows the existing home interior with the renderer drawing the rear
wall face across two tile heights. The room grid, wall collision, floor positions,
doorway, furniture anchors, and save data are unchanged. The treatment applies to
rear-facing wall tiles only; side walls retain their existing continuous caps.

## Reproduce

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -OutputDirectory temp/interior-prototype/classes -IncludeTests -IncludeReviews
java '-Djava.awt.headless=true' -cp Java/temp/interior-prototype/classes com.alderfall.game.InteriorDesignPreview Java/temp/interior-prototype/preview home
```

The reviewed frame is `home-seed-0.png` (seed 0). Four home footprint seeds were
captured; the other frames remain disposable under `Java/temp/interior-prototype/`.

The four source sheets are in the user-provided `Input - uploads` folder. They
have not been installed as runtime assets: their labels, borders, and mixed-size
modules need an authored crop manifest before they can safely drive automatic
wall/floor joins. Next, map the seamless floor cells and wall run/corner/trim
pieces into named IDs, then compare the sheet-built preview with this taller
procedural wall treatment.
