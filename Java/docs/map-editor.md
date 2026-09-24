# External location workshop

The workshop is a separate Java 21/Swing desktop editor. It shares the game's
terrain, building, prop, depth-order and collision implementations. It does not
require a running adventure, a character save, Python, or downloaded libraries.

Double-click `Java/scripts/launch.cmd` to open the shared start screen, then choose
**Play Game** or **Map Editor**. Only the selected mode initializes its world;
the start screen closes once that window is ready. `run-game.cmd` opens the game
directly and `run-map-editor.cmd` opens the editor directly. All three use the
same build/run scripts. From PowerShell, `run.ps1 -Launcher` selects the start screen.

Double-click `Java/scripts/run-map-editor.cmd` in File Explorer, or run it from
the repository root:

```powershell
.\Java\scripts\run-map-editor.cmd
```

The dedicated launcher delegates to `run.ps1`, builds the editor, and enables the
conservative-JIT workaround described below. It keeps the console open on failure
so the error can be read. Extra options are forwarded, for example
`run-map-editor.cmd -Map <file>` or `run-map-editor.cmd -PrintCommand`.

The equivalent shared launcher without the workaround is:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/run.ps1 -Editor
```

Open an existing document directly, or import it into a new game playtest:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/run.ps1 -Editor -Map Java/exports/map-editor/maps/example.aldermap
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/run.ps1 -Map Java/exports/map-editor/maps/example.aldermap
```

`-Map` is resolved relative to the calling directory. The shared launcher builds
normal production sources before launching. `-PrintCommand` prints the invocation.

If the installed JVM crashes in native compiled code or `javac` reports an internal
compiler exception, use `-ConservativeJit` with the launcher. This optional flag
sets `TieredStopAtLevel=1` for both compilation and the application; it may reduce
peak performance. It is not enabled for normal launches. Such intermittent crashes
were observed with the local Oracle 21.0.10 JVM during validation; their underlying
cause has not been diagnosed.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/run.ps1 -Editor -ConservativeJit
```

## Authoring a location

1. Choose **New**. Select city, village, dungeon, or interior, dimensions up to
   160 × 160, and a numeric seed. Enable the starting layout for street blocks,
   city walls/gates and building lots, or connected dungeon chambers/corridors.
   Interior starts are enclosed blank rooms. The same seed/settings recreate the
   same starting layout. Generating creates a new document, with an unsaved-change
   prompt before replacing an edited document.
2. Search the **Asset library** by name, logical asset ID, category or source path.
   **All assets** is selected initially. Every image ID exposed by the runtime
   catalogue is available, along with the village system's visual aliases.
   Categories cover all regional building tilesets, flora/nature by biome, fauna
   and monsters, player characters, companions, NPCs, furniture, items, effects,
   terrain images, backgrounds, animation poses and raw atlases. No image is
   excluded by its name prefix. Thumbnails load for visible rows rather than
   decoding the whole library at startup.
   **Buildings** entries create functional lots with generated game interiors.
   Raw library sprites are scenery; placing a building-shaped sprite does not
   create an entrance or building footprint.
   Animals and characters are visual placements without AI or NPC interactions.
   Animation strips use a static first pose for thumbnails, placement and gameplay
   rendering; frame counts come from existing `.frames` metadata, with square-frame
   inference for strips without metadata. Atlases without frame metadata remain
   complete sheet images. **Terrain paint** edits tile materials; **Terrain sprites**
   places their artwork as props. Audio tracks and sound effects are not visual
   placements, and arbitrary procedural equipment recolor combinations are not
   enumerated as separate sprite files.
3. Select terrain, then use **Paint**, **Fill**, **Rectangle**, or **Line**.
   Paint/Line use the brush width; rectangle uses its drag bounds. Fill replaces
   the connected material. Roads, water, walls, dungeon floors and interior floors
   are all available. Paint replaces terrain; it does not erase props or lots.
4. **Place** adds a prop or building. Choose building facing or prop size in the
   inspector. **Select** an object at its anchor/lot and drag to move it. Apply
   size/facing to a selected object using the inspector button. **Erase** removes
   the topmost prop, then building, then landmark. Building overlap and map-edge
   violations are rejected; scenery can overlap intentionally.
5. **Spawn** places the playtest start; **Landmark** writes the inspector's label.
   Use the collision layer to see tiles blocked by the game's movement rules.
   **Validate** checks spawn, missing prop assets, connected walkable regions and
   building entrance access. Disconnected areas are reported rather than erased.
6. **Save** writes a reusable whole map. **Playtest** / F5 opens a separate game
   window with a detached copy and a character named Workshop Playtest. Buildings
   use the existing generated-interior system. Close that window to keep editing;
   playtest changes are not copied into the editor document. Starting a playtest
   does not write an adventure save. The normal game's save actions remain available,
   but adventure saves do not embed workshop map data; reload the `.aldermap` to
   playtest that location again.

## Prefabs and controls

With **Select**, drag empty terrain to select a rectangular region, or use
Ctrl+A for the whole map. Ctrl+C copies its terrain, props, entire building lots,
and landmarks. A selection cutting through a building is rejected. Ctrl+V enters
**Stamp** mode. Click to stamp repeatedly, and use Ctrl+R to rotate the stamp
clockwise. Disable **Stamp terrain** to place only its objects and landmarks.
Stamps must fit entirely in the destination; overlapping building lots reject the
whole operation without changing terrain or props. The destination spawn is kept.

**Prefabs → Save selection as prefab** writes a selection to disk.
**Load prefab** loads it for stamping; whole-map documents can also be stamps.
Rotation transforms tile coordinates, lot dimensions, building facing and markers.
Prop artwork keeps its authored orientation; select a directional asset variant
explicitly when needed. Functional building prefabs require city/village maps.

| Action | Control |
| --- | --- |
| Use active tool | Left click / drag |
| Pan | Right or middle drag |
| Zoom about cursor | Mouse wheel |
| Fit map | Fit button |
| Navigate overview | Click minimap |
| Undo / redo | Ctrl+Z / Ctrl+Y |
| Save / save as | Ctrl+S / Ctrl+Shift+S |
| Clear selection / return to Select | Esc with canvas focused |
| Delete selected objects | Delete |

History retains 60 gestures; a continuous paint stroke is one undo step. Saving
creates a content checkpoint, so undoing to the saved state clears the modified
indicator. Closing or opening another map prompts if content differs from that
checkpoint. Layer checkboxes control visibility; hidden layers are skipped by
direct placement, painting, selection and erasing. Region copy/delete and stamps
operate on the full document.

## Files and scope

Default locations are ignored, private authoring outputs:

- `Java/exports/map-editor/maps/*.aldermap`
- `Java/exports/map-editor/prefabs/*.aldermap`

The version-2 format is UTF-8 Java properties with `format=alderfall-map`, kind,
dimensions, tile rows, spawn, seed, prefab flag, props (including visual slots and offsets),
buildings (including keys, palette and facing), and landmark labels. Writes use a
sibling temporary file and atomic replacement when supported. Readers validate
dimensions, coordinates, records, tile codes, and versions before replacing the
current document. Village editor `.txt` exports are readable; those older files
have no authored spawn and default to the map center, which may need adjustment.
Runtime asset IDs are preserved; images are not copied into documents.

This iteration focuses on layout authoring. It does not implement scripted
encounters, custom NPCs, authored links between documents, hand-authored building
interior links, or campaign placement. Dungeon playtests retain the game's normal
procedural encounter behavior. Landmarks are labels, not encounter/quest triggers.
Collision follows existing asset/terrain rules, not custom collision polygons.
The editor view is a static authoring view without gameplay actors, weather or
day/night effects; playtesting supplies the complete gameplay presentation.

## Implementation and validation

`editor/` contains the detached model, versioned IO, bounded history, seeded layout
generators, searchable palette, desktop shell and canvas. `GamePanel` exposes a
small rendering/playtest bridge; `WorldMap.installEditorMap` confines installations
to `editor_` IDs and invalidates terrain/building caches. Neither layer depends on
test/review sources. The existing in-game village editor remains available.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/test.ps1 -Test com.alderfall.game.MapEditorTest
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -OutputDirectory temp/map-editor/checks
```

From `Java/`, on a desktop:

```powershell
java '-Djava.awt.headless=false' -cp temp/map-editor/checks com.alderfall.game.editor.MapEditorInteractionTest
```

The headless tests cover lossless roundtrips, legacy import, malformed inputs,
rotation, atomic stamping, history checkpoints, deterministic layouts, connected
dungeon rooms, shared rendering and game import. The desktop test dispatches
Swing mouse gestures to an undisplayed window and checks paint, undo, rectangle,
spawn, object drag, selection, prefab stamping and erase. Disposable captures go
under `Java/temp/map-editor/tests/`.

Validation on 2026-09-24: production and test builds passed. The native Swing
interaction suite passed with the default JVM. The complete model/render/game
import suite passed with `-XX:TieredStopAtLevel=1` after a native JVM crash in the
default mode. The existing `VillageTerrainTest` reports a seam/material mismatch
at `(1,0)`; the same failure was reproduced using isolated copies of WorldMap and
MapArea with this editor's additions removed. Repository policy tests, review
links/archive checks and structure checks passed. The full check script stops on
the existing unapproved `.vscode/settings.json`; no baseline or ignores were changed.

The complete-library extension adds `EditorAssetLibraryTest` (runtime catalogue
coverage, all regional families, lossless sprite-ID persistence, animation metadata
and identical editor/game placement poses), `LauncherTest` (mode routing, loading
guard and screen rendering), and global-search checks in `MapEditorInteractionTest`.
These tests use the same development build switches above. Run the first two with
`java '-XX:TieredStopAtLevel=1' '-Djava.awt.headless=true' -cp <classes>` followed by
`com.alderfall.game.EditorAssetLibraryTest` or `com.alderfall.game.LauncherTest`.

## Responsive editing and asset browsing

Editor rendering now retains unchanged terrain chunks across edits. Brush moves
batch all interpolated cells into one map update per mouse event. Ordinary sprite
placement does not rebuild terrain; terrain and ground-cover patches invalidate
only nearby chunks, including blend and cross-boundary sprite margins.

The library offers A?Z, Z?A, newest/oldest modification time, and category sorting.
Use the category and subcategory dropdowns together with source-path/name search.
Rows and the inspector show the source image file's last-modified time in the local
timezone, not its creation date or import history. Metadata is sampled when the
editor opens; generated aliases use their source file when it is available.

## Bulk placement and offsets

- **Drag behavior:** COPIES paints spaced copies, OFFSET places one prop and lets
  you drag it smoothly in any direction, and SINGLE places only on click. In
  Select mode, OFFSET moves selected props with sub-tile precision. Movement
  carries across tile boundaries. Each gesture is one undo.
- Ctrl/Shift-select multiple library assets and drag them onto the canvas to place
  a row. Building lots still require space and a compatible city/village map.
- **Select:** drag empty terrain to select a rectangle, then drag inside it to move
  its props and fully enclosed buildings together. Terrain stays in place.
- **Visual offset X/Y:** reference pixels, with 48 pixels equal to one tile at any
  zoom. Quarter-anchor buttons set NW/NE/SW/SE root positions. Use **Apply size /
  offset / facing** on a selected prop, or **Alt+arrow keys** to nudge it by one
  reference pixel. Offsets apply to new copies and survive undo, maps, prefabs,
  rotation and playtesting. Natural solid footprints follow offsets and size;
  functional building lots and interior furniture tile collision remain grid-based.
  Use building sprites for freely offset visual buildings.
- **PATCH:** select small grass/flowers/ferns/ground-cover assets (Ctrl-select to
  mix), choose brush diameter, density and variation, then drag. It paints a round,
  feathered patch with up to four anchors per tile and shows the 2?2 divisions.
  Ground protection skips non-natural terrain, buildings, landmarks and ordinary
  objects. Clear that checkbox to paint those surfaces deliberately.
- Patches reuse the baked ground-detail renderer. Authored slots 128?131 have fixed
  quarter-tile roots; generated cover keeps its existing irregular anchors.
  Size is capped at 28 reference pixels and offsets at ?12 for bounded overlap;
  variation adds deterministic size/jitter. Repainting a slot replaces its plant.
  **Shift+Erase** removes all authored patch slots from each visited tile.

Map format version 2 adds two prop fields: `x,y,asset,size,visualSlot,offsetX,offsetY`.
Version 1 and legacy village files still load with zero offsets. Older editors
cannot read version 2 files.

## Windows executable

`Java/exports/windows-launcher/Alderfall.exe` opens the shared mode-selection screen.
`scripts/build-launcher.ps1` builds this small Windows entry point with the installed
.NET Framework compiler, overwriting that executable only. No downloads or asset
copies are required. It delegates to `run.ps1 -Launcher -ConservativeJit` and displays
startup errors in a dialog. It needs the project folder and installed JDK 21; keep
it below `Java/`. `Alderfall.exe --check` checks project discovery without opening UI.

Additional diagnostics: `com.alderfall.game.editor.EditorResponsivenessTest` and
`com.alderfall.game.editor.MapEditorInteractionTest` in a test build. The former
compares incremental rendering against a fresh renderer and checks cache reuse,
patch density/deduplication, offset persistence, prefab rotation and sorting. The
native Swing test covers repeated placement, spacing, group movement, nudging,
undo/redo and category filtering.

## Selection, radial menu and collision footprints

Right-click without dragging opens the circular tool menu: Select, Offset drag,
Draw copies, Delete/Erase, Undo, Redo, Patch and Single. Translucent wheel segments
blend over the map; hovering highlights a segment with a gold fill and outline.
Left-click a segment to activate it and close the wheel. Right-click again, press
Escape, or left-click the center/outside the ring to dismiss without an action.
Arrow keys navigate the segments and Enter chooses. Right-drag still pans when
the wheel is closed. The overlay consumes clicks so dismissing cannot place a prop.
The menu retains an existing selection; with none, it selects an object under the
pointer first. Delete acts on the selection, or switches to Erase if none exists.

Shift-click adds/removes objects. Drag empty ground for a region. Delete/Backspace
while the canvas is focused removes the selected objects in one undo operation;
a Delete button is also in the inspector. Size, offsets and building facing can
apply immediately, or disable that checkbox and use Apply. Changing a size does
not overwrite offsets. Multi-prop offset edits, nudges and offset drags move the
selection together and preserve relative spacing. Building lots retain grid moves.

The Collision overlay now draws orange prop footprints as well as blocked tiles.
Trees, rocks/ore and dense bushes use family-based ground rectangles, not the full
sprite silhouette. Their footprints scale with prop size beyond the old 2x cap,
follow placement offsets, and participate in swept movement during playtesting.
Canopies and small grass/flower patches stay passable. Fences, market modules and
fountains retain their existing fixed structural dimensions, but offsetting their
artwork also moves their solid footprint. Decorative sprites/characters without
existing collision rules remain visual-only; this does not infer new blockers
from every PNG's alpha channel.

Validation includes `EditorPropCollisionTest` for size/offset alignment and movement,
and expanded `MapEditorInteractionTest` coverage for offset gestures, live property
editing, Shift-selection, bulk deletion, relative group offsets and radial actions.

## Loading feedback

Game startup, workshop startup and map playtesting show the same loading screen.
Actual progress messages cover terrain/world generation, towns/interiors, scenery,
elevation, asset indexing, library categories, file dates and the initial map.
An indeterminate progress indicator and elapsed clock keep moving while background
preparation runs; there is no estimated percentage or artificial delay. Swing
windows and render/input controllers are installed on the event thread afterward.

Choosing an adventure save uses the same screen with restoration stages for character
and storage, settlement customization, quests, companions, arrival and encounters.
The current simulation pauses while a separate state is prepared. Success replaces
the game panel and shuts down the old one; failures retain the old adventure and
resume its timer. Fullscreen controls and workshop playtest cleanup follow the new
panel. Existing save data formats are unchanged. Other documents cannot be edited
while a loading dialog is active.

`LoadingScreenTest` verifies a live Swing timer during background work, installation
on the event thread, failure cleanup, actual stage reporting, isolated save restore
and game-panel replacement. Test saves and captures stay under `Java/temp/loading/`.
