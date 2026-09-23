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

## Interactive review sites

Open [characters/index.html](reviews/characters/index.html) for character/monster
animations, movement, and dialogue. All related subsites and their exported
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
