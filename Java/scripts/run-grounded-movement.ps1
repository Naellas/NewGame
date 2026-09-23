param([switch]$PrintCommand)
$ErrorActionPreference = "Stop"
& "$PSScriptRoot/run.ps1" -Movement grounded -PrintCommand:$PrintCommand
