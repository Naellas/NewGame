$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (Test-Path out) {
    Remove-Item out -Recurse -Force
}

New-Item -ItemType Directory -Force -Path out | Out-Null
$sources = Get-ChildItem -Recurse -Filter *.java src\main\java | ForEach-Object { $_.FullName }
javac -d out $sources
Write-Host "Built Java classes into $root\out"
