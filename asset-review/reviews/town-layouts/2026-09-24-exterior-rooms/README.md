# Furnished outdoor rooms and stepped town outlines

Actual game-renderer captures of Northwatch's public spaces, northern wall court,
southern suburbs, and Reedwatch's eastern wall bay. These are unedited renders at
noon and 40% zoom. The wall scale from the previous review is preserved.

Reproduce from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -OutputDirectory temp/town-exterior/classes
```

Then from Java/, using Java 21:

```powershell
java '-Djava.awt.headless=true' -cp temp/town-exterior/classes com.alderfall.game.TownLayoutTest --render
java '-Djava.awt.headless=true' -cp temp/town-exterior/classes com.alderfall.game.TownExteriorRoomsTest
java '-Djava.awt.headless=true' -cp temp/town-exterior/classes com.alderfall.game.RegionalSettlementTest --layout-only
```

The exporter writes 21 views to Java/temp/town-layout/review. Four selected views
are kept here; bulk captures stay in scratch space. Existing local lighting and
display settings can affect the result.

Agent review: joined stalls and stock rows, joined benches and planters, hedge
borders with path/door openings, fenced yards, and the eight-corner wall outline
were inspected in the actual renderer. Regional reachability and perimeter checks
passed three seeds, as did generated exterior density/collision checks. Interior
ConnectedFurnitureTest, BuildingCollisionTest and TownNpcNavigationTest passed.
User aesthetic acceptance is pending. Automatic vertical/four-way furniture joins
remain outside this pass; yard recipes place vertical boundary pieces explicitly.

Source provenance and import:
[imagegen hedge/fence batch](../../../../art-source/towns/2026-09-24-exterior-modules/README.md).

Repository validation: policy unit tests, review links/checksums, tool imports,
class identities and asset catalog checks passed. The full check.ps1 run stopped
at the unrelated new root `.vscode/settings.json` file; that file was left intact
and the remaining checkers were run separately. No hygiene baseline was expanded.
