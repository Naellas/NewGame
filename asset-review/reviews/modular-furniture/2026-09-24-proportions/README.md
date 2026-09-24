# Furniture proportions

`blacksmith.png` is an actual game-render capture after removing horizontal
rescaling of connected furniture bodies. Joined variants copy the same fitted
body pixels as standalone variants and mirror edge patches into the join margins.
This removes the width inflation caused by stretching a cropped sprite. Thin-strip
repetition and single-column extrusion were rejected during visual review because
they created ribbed hinges or smeared posts. Generic mirroring can still repeat
small edge details; authored connector art would improve those specific seams.

Reproduce from repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -IncludeReviews -OutputDirectory temp/furniture-proportions/classes
Set-Location Java
java '-Djava.awt.headless=true' -cp temp/furniture-proportions/classes com.alderfall.game.ConnectedFurnitureTest
java '-Djava.awt.headless=true' -cp temp/furniture-proportions/classes com.alderfall.game.InteriorDesignPreview temp/furniture-proportions/captures blacksmith
```

Validation: build, ConnectedFurnitureTest and ModularFurnitureTest passed.
ConnectedFurnitureTest checks central body pixels against standalone images for
26 families, all joined masks and three scales, plus edge coverage and removal.
Review links and structure checks passed. The full policy script reports unrelated
root-layout findings in `.vscode/settings.json` and four `Input - uploads` PNGs;
these files were left untouched and no baseline or ignore rule changed.
