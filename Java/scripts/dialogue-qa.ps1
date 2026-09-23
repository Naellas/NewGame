$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$relativeOutput = 'temp/dialogue-qa/' + [Guid]::NewGuid().ToString('N')
$qaOut = Join-Path $root $relativeOutput
& "$PSScriptRoot/build.ps1" -IncludeReviews -OutputDirectory $relativeOutput
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$output = Join-Path (Split-Path -Parent $root) "Story premise\companion-dialogue-qa.md"
java -cp $qaOut com.alderfall.game.CompanionDialogueQaExport $output
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
