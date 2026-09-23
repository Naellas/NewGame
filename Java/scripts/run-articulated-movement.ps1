param([switch]$PrintCommand)
$ErrorActionPreference = "Stop"
& "$PSScriptRoot/run.ps1" -Movement articulated -PrintCommand:$PrintCommand
