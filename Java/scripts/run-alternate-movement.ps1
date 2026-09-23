param([switch]$PrintCommand)
$ErrorActionPreference = "Stop"
& "$PSScriptRoot/run.ps1" -Movement alternate -PrintCommand:$PrintCommand
