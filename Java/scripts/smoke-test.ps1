$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

& "$PSScriptRoot\test.ps1" -Test com.alderfall.game.SmokeTest
exit $LASTEXITCODE
