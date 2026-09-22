$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

& "$PSScriptRoot\build.ps1"

java -Dalderfall.renderMetrics=true -Dalderfall.renderMetricsEvery=120 -cp out com.alderfall.game.Main
