param([string]$BatchDirectory = $PSScriptRoot)
$ErrorActionPreference = 'Stop'
$batch = (Resolve-Path -LiteralPath $BatchDirectory).Path
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../../..')).Path
$entries = Get-Content -LiteralPath (Join-Path $batch 'manifest.json') -Raw | ConvertFrom-Json
foreach ($entry in $entries) {
    $source = [IO.Path]::GetFullPath((Join-Path $batch $entry.source))
    $target = [IO.Path]::GetFullPath((Join-Path $root $entry.destination))
    if (-not $source.StartsWith($batch + '\', [StringComparison]::OrdinalIgnoreCase) -or
        -not $target.StartsWith((Join-Path $root 'Java/assets/environments/settlements/city/buildings') + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe import path.' }
    if ((Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash -ne $entry.sha256) { throw "Source checksum mismatch: $source" }
    if (Test-Path -LiteralPath $target) {
        if ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -ne $entry.sha256) { throw "Different asset exists: $target" }
        continue
    }
    New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
    Copy-Item -LiteralPath $source -Destination $target
}
Write-Output "Imported/verified $($entries.Count) town sprites."
