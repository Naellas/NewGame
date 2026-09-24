# Village editor placement tools

`editor-menu.png` is an actual 1920x1080 game render from
`VillageEditorToolsTest`. It shows the Props sidebar and the new right-click
menu with grid snap and placement collision disabled. Object actions are
disabled in this capture because the cursor targets empty ground.

Reproduce from the repository root using Java 21:

```powershell
$env:JAVA_TOOL_OPTIONS='-XX:TieredStopAtLevel=1'
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -OutputDirectory temp/settlement-identity/classes
Set-Location Java
java -Djava.awt.headless=true -cp temp/settlement-identity/classes com.alderfall.game.VillageEditorToolsTest
```

The test verifies sub-tile placement, collision-on rejection, collision-off
stacking, nudges across tile boundaries, exact-object movement/removal,
interior stacking after save/load, surviving furniture collision, and menu
toggle actions. Captures are written to `Java/temp/settlement-identity`.

Visual review accepted the menu placement, readable labels and disabled-action
states. Production compilation, settlement panel checks, structure and review
links pass. Repository policy tests pass; the hygiene check flags unrelated
`.vscode/settings.json` and four images in `Input - uploads`. No baseline or
ignore rules were changed.

[Controls and limits](../../../../Java/docs/settlement-building.md).
