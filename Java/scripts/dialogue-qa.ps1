$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$qaOut = Join-Path $root "out-check\dialogue-qa"
if (Test-Path $qaOut) {
    Remove-Item $qaOut -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $qaOut | Out-Null

javac -d $qaOut -sourcepath src\main\java src\main\java\com\alderfall\game\CompanionDialogueQaExport.java
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$output = Join-Path (Split-Path -Parent $root) "Story premise\companion-dialogue-qa.md"
java -cp $qaOut com.alderfall.game.CompanionDialogueQaExport $output
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
