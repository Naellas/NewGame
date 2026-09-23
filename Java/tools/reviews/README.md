# Interactive review tools

Open [characters/index.html](characters/index.html) directly in a browser. It is
the main review entrypoint; no server or game changes are required.

- [Movement simulator](movement-physics/index.html), including its full-roster calibration subsite.
- [Dialogue animation](dialogue-motion/index.html).
- [Grounded movement study](grounded-movement/index.html).
- [Cloth and hair study](cloth-physics/index.html).
- [Monster attack gallery](monster-attacks-2026-09-23/index.html).

All six site directories moved together from root `asset-review/`. Their
HTML/JavaScript, exported frames, contact sheets, notes, and source utilities
stay together. Runtime sheets are read from `Java/assets` by relative URL;
the game does not depend on these sites. Do not copy runtime artwork here.

Java review exporters run with `Java/` as their working directory and now write
to `tools/reviews/<topic>/`. Compile the optional study sources together with the
game (they share its package) into a separate output directory, from `Java/`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 -IncludeReviews -OutputDirectory temp/review-build/classes
```

Run the desired exporter, for example `java -cp temp/review-build/classes
com.alderfall.game.ExportClothStudy`, only when intending to regenerate its
review outputs. Normal game builds do not include these study sources.
Browser checks run from any working directory:

```powershell
python Java/tools/reviews/grounded-movement/source/check_preview.py
python Java/tools/reviews/dialogue-motion/check_preview.py
```

Those checks use local Chrome on Windows and write screenshots under
`Java/temp/review-checks/`.
Use the standard-library `check_review_site.py` for read-only link validation.
New temporary captures belong in `Java/temp/`; preserve these migrated evidence
files until a specific review determines which can be retired.
