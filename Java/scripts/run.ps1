$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path out)) {
    & "$PSScriptRoot\build.ps1"
}

java -cp out com.alderfall.game.Main
