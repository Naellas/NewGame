# Connected counters and shelves

Reviewed in-game renderer captures: [inn counters](inn.png) and [library shelves](study.png).
Status: visually checked for continuous horizontal runs, retained outside end caps,
and room circulation. These use the existing artwork through runtime sprite slicing;
there are no new source sheets or imported assets.

Reproduce from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -IncludeReviews -OutputDirectory temp/modular-furniture/classes
```

Then from `Java/`:

```powershell
java -Djava.awt.headless=true -cp temp/modular-furniture/classes com.alderfall.game.InteriorDesignPreview temp/modular-furniture/inn inn
java -Djava.awt.headless=true -cp temp/modular-furniture/classes com.alderfall.game.InteriorDesignPreview temp/modular-furniture/study study
java -Djava.awt.headless=true -cp temp/modular-furniture/classes com.alderfall.game.ConnectedFurnitureTest
```

The curated images are seed 0. Other seeds remain in ignored scratch space.
Existing local display settings can affect floor textiles and lighting.
Horizontal runs require the same furniture ID, same row, and touching footprints.
Corners, vertical runs and connections between different furniture styles are not implemented.
