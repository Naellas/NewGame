$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

& "$PSScriptRoot\build.ps1"
$pythonRoot = Join-Path (Split-Path -Parent $root) "Python"
java -cp out com.alderfall.game.SmokeTest $pythonRoot
