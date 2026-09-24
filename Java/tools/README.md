# Asset Tools

Start with the [canonical tool index](INDEX.md). Python tools are grouped into
`assets/characters`, `assets/environments`, `assets/monsters`, `assets/items`,
`assets/shared`, `audio`, `checks`, and `tests`. Use
`python Java/tools/run.py --list` from the repository root, then
`python Java/tools/run.py NAME [arguments]`. The runner uses the Java working
directory. Common old entrypoints remain compatibility shims.

The [asset layout](../assets/README.md) documents current destinations.
Three unused legacy tools are [archived](../archive/pending-deletion/retired-tools-2026-09-23/README.md).
Shared image helpers are in `assets/shared/image_ops.py`; importer-specific
keying, cropping, and resampling policies must remain explicit.

For Java diagnostics mentioned below, first build from `Java/` with
`powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 -IncludeTests -IncludeReviews -OutputDirectory temp/checks/classes`.
See [source-root instructions](../src/README.md).

`InteriorAssetReview` compares two sprite directories through the runtime
aspect-preserving furniture fitting path. From `Java/`, after the review build:
`java -Djava.awt.headless=true -cp temp/checks/classes com.alderfall.game.InteriorAssetReview BEFORE AFTER temp/interior-comparison`.
It requires Java 21 only, reads PNGs from BEFORE and matching logical IDs from
AFTER, and writes/overwrites `comparison-N.png` sheets in the output directory.
It never modifies input art. See the
[interior quality review](../../asset-review/reviews/interior-quality/2026-09-24-cutouts-detail/README.md).

## Interactive review sites

Java `RenderBenchmark` also accepts `SAMPLES dialogue [CAPTURE.png]` for a
1920x1080 Aria/Mage dialogue with the world visible. It uses the existing game
assets and configuration, requires the review build above, prints render timings,
and overwrites only the optional capture file. From `Java`, for example:
`java -Djava.awt.headless=true -cp temp/checks/classes com.alderfall.game.RenderBenchmark 60 dialogue temp/dialogue-mesh/game.png`.
Add JVM flag `-Dalderfall.dialogueDenseReference=true` to compare the dense
reference against the default mesh. These timings exclude game updates and
display presentation.

Open the [Preview Workshop](reviews/index.html) to search and switch between all
24 HTML previews: characters, movement, dialogue, world galleries, artwork, and
audio. It opens directly in a browser, needs no dependencies or server, reads the
existing preview pages, and writes no files. All related subsites and their exported
frames live in [reviews/](reviews/README.md). Java exporters run from `Java/` and
overwrite their named review outputs; browser checks require local Chrome and
write disposable screenshots under `Java/temp/review-checks/`. Runtime assets
stay under `Java/assets/`.

## Review and archive integrity

`python Java/tools/checks/check_review_site.py` from the repository root validates local
HTML links, retired Java exporter paths, and pending-deletion manifests/checksums.
It requires Python 3.10+, uses the standard library, and writes nothing. It skips
dynamic JavaScript URLs, which are exercised by the browser checks. No network
requests are made. Archive instructions are in `Java/archive/pending-deletion/`.

## Repository hygiene

`python Java/tools/checks/check_repository_hygiene.py` from the repository root checks
new layout violations against `Java/config/repository-hygiene-baseline.json`.
It reads Git's tracked/non-ignored file list, prints findings, and exits nonzero
on failure. It requires Python 3.10+ and Git, uses only the standard library,
and never writes or overwrites project files. The script also works by absolute
path from another working directory.

Tests: `python -m unittest discover -s Java/tools/tests -p "test_repository_*.py"`.
On Windows, `powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/check.ps1`
runs both checkers and their tests from any working directory when given the
script's absolute path.
See [agent rules and enforcement](../docs/agent-guidelines-and-enforcement.md).

Run tools from the `Java` project root:

```powershell
python tools/assets/regenerate_universal_assets.py --verify
```

`regenerate_universal_assets.py` is the preferred entrypoint for sheet cutouts. It routes sprites, props, items, monsters, NPCs, class models, directional sprites, and walk strips through `universal_cutout.py`, then can run the proof report.

Use individual extraction scripts only when iterating on one source sheet's grid, names, or sizing. New chroma/background removal behavior belongs in `universal_cutout.py`, not in sheet-specific scripts.

Some extraction and fidelity-refresh scripts require Pillow. The Java runtime does not use these scripts.

## Music

`python tools/audio/musicgen.py` renders the game's stereo soundtrack (NumPy required;
install with `python -m pip install -r tools/requirements.txt`). The Java game
plays the generated WAVs and needs no Python or synthesis library at runtime.

The score engine in `music_score.py` supplies synthesized string ensembles,
flute, oboe, horn, harp, lute, dulcimer, bells, bass and percussion. These are
procedural instrument approximations, not sampled orchestral recordings.
32-bar scores develop through statement, variation, a quieter section and a
fuller return, with stereo placement and room reflections that wrap at the loop.

Six `region_*` themes follow kingdom boundaries. Their `town_*` arrangements
carry into settlements and houses. Dungeon and battle cues take priority;
battles use a fast entrance. Regional pieces play their full arrangements
without the old 50-second ambient rotation. Brief border crossings are ignored.

Use `--only region_sanctum battle_boss --out assets/music/audition` to render
selected scores for review, `--bars 8` for shorter sketches, or `--legacy` for
the original mono renderer. Existing `--sample-pack` and `--inspired-pack`
options retain their original audition engine. `--list` lists all presets.
Generation is deterministic. A partial render's README lists only that batch.

Validate audio with `python -m unittest discover -s tools/tests -p test_music_score.py`.
After compiling Java, run `java -cp temp/checks/classes com.alderfall.game.MusicRegionTest` from
the Java directory to check regional, settlement, interior and dungeon selection.

### Combat and dungeon scores

The encounter library contains 21 arrangements: four regular battle tracks,
three elite tracks, four boss tracks, and ten dungeon tracks. Caves, crypts,
castles, prisons and bandit strongholds each have an exploration theme and a
more tense theme for the last two floors. Ascending dungeons use the same
progression toward the final floor. These full pieces loop without timed rotation.

Combat selection uses `Battle.isEliteEnemy` and `Battle.isBossEnemy`; group size
does not classify an encounter as a boss. Each tier cycles through its own pool
without consecutive repeats. The cue remains fixed for the battle, including
enemy deaths, boss phase changes, and pause/settings menus. Selection does not
consume gameplay randomness. Pool positions reset when the audio controller is recreated.

`ENCOUNTER_SCORES` in `music_score.py` defines each piece's harmony source,
tempo, melodic motif, instruments and rhythmic pattern. New cello, organ and
choir voices complement the existing palette. Elite arrangements add bowed
rhythmic figures; bosses add sustained choir layers. The sounds remain synthesized.

Open `assets/music/encounter-music.html` to audition and filter the 21 scores.
The generator rebuilds this local listening page alongside the WAV files.
Run `java -cp temp/checks/classes com.alderfall.game.CombatMusicTest` after compiling to verify
tier classification, track coverage, repeat avoidance and stable encounter cues.

Town architecture: [regional sprite and in-game review](reviews/town-architecture/index.html).


### Water surface review (Java)

`com.alderfall.game.WaterPreview` compares calm, wind and storm surface motion
using the runtime renderer. Build with `scripts/build.ps1 -IncludeReviews`, then
from Java run `java -Djava.awt.headless=true -cp out com.alderfall.game.WaterPreview temp/water/surface-frames`.
Input: existing game configuration and fixed weather/depth review values.
Output: 80 PNG frames (10 fps), overwriting matching filenames in the supplied
directory. Dependencies: Java 21 standard libraries only. See
[water documentation](../docs/water.md) for gameplay checks and curated evidence.

### Transparent animation grids

`python tools/run.py import_animation_grid SOURCE ASSET_RELATIVE_OUTPUT --columns 4 --rows 3 --width 192 --height 64`
imports an RGBA grid into a horizontal animation strip. It uses shared project
paths and asset routing, one common scale, and bottom registration. Alpha above
`--alpha-threshold` (default 2/255) determines content bounds; retained alpha is
preserved. Outputs are PNG plus `.frames` and `.framebounds` beside it; matching
files are overwritten. Dependencies: Python and Pillow. Source may be anywhere;
output must be asset-relative. Used by the [wave batch](../../art-source/water/2026-09-24-wave-crests/README.md).
Tests: from Java, `python -m unittest discover -s tools/tests -p test_animation_grid.py`.

## Original spell and footstep samples

`python tools/run.py sfxgen [--output PATH]` synthesizes 144 deterministic WAVs:
elemental flight/impact, weapon and monster actions, support magic, and eight
soft footstep variants per surface.
Inputs are the named recipes/seeds in `audio/sfxgen.py`; no external files or
packages are required. It reuses the PCM writer in `audio/musicgen.py` and defaults
to overwriting only its named files under `assets/sfx`. Use `--output temp/audio`
for experiments. Listen in [audio review](reviews/audio/index.html). Tests:
`python -m unittest tools.tests.test_sfxgen` from Java.

## Interior architecture crop recipes

`extract_interior_seamless_tiles --recipe <recipe.json> [--output-root <assets-root>]`
also imports the generated window/painting batch at
`../art-source/interiors/2026-09-24-windows-and-paintings/recipe.json`, preserving
alpha while cropping and resizing to the four declared wall-decor runtime paths.
The same command
imports explicit floor, wall face, cap, post and baseboard rectangles from hashed
original atlases. Run with `python Java/tools/run.py extract_interior_seamless_tiles`
from the repository root; recipe paths are interpreted from `Java/`. Python and
Pillow are required. The recipe supplies source paths relative to itself,
SHA-256 checksums, crop rectangles, optional 90-degree rotation, output sizes and
canonical `asset_file` paths. It overwrites only the recipe's named PNG outputs;
source images are never overwritten. Without `--recipe` the existing legacy
sheet import remains available.

Window crops can set `clear_navy_background` to remove border-connected dark navy
atlas pixels while preserving enclosed dark details. `transparent_padding` adds
an inset transparent margin within the requested output size. Both options are
off by default; architectural material crops remain opaque.

The [September modular-material batch](../../art-source/interiors/2026-09-24-modular-materials/README.md)
records the current command, inputs, 24 outputs and review status. Use a scratch
`--output-root temp/<task>/assets` to inspect regenerated output before replacing
runtime art. Import checks: `python -m unittest tools.tests.test_interior_architecture_import`
from `Java/`.
