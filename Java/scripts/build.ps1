$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (Test-Path out) {
    Remove-Item out -Recurse -Force
}

New-Item -ItemType Directory -Force -Path out | Out-Null
$sources = Get-ChildItem -Path src\main\java -Recurse -Filter *.java | Sort-Object FullName | ForEach-Object { $_.FullName }
javac -d out $sources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
Write-Host "Built Java classes into $root\out"
