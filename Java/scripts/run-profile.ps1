param([switch]$PrintCommand)
$ErrorActionPreference = "Stop"
& "$PSScriptRoot/run.ps1" -Profile -PrintCommand:$PrintCommand
