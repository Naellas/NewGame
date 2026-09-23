param([switch]$ValidateOnly)
$ErrorActionPreference = 'Stop'
$reviewRoot = [IO.Path]::GetFullPath($PSScriptRoot)
$workspaceRoot = (Resolve-Path -LiteralPath (Join-Path $reviewRoot '../../../..')).Path
$assetsRoot = [IO.Path]::GetFullPath((Join-Path $workspaceRoot 'Java/assets'))
$filesRoot = [IO.Path]::GetFullPath((Join-Path $reviewRoot 'files'))
$entries = Get-Content -Raw -LiteralPath (Join-Path $reviewRoot 'manifest.json') | ConvertFrom-Json
$layout = Get-Content -Raw -LiteralPath (Join-Path $workspaceRoot 'Java/config/asset-layout.json') | ConvertFrom-Json
$moves = foreach ($entry in $entries) {
    $source = [IO.Path]::GetFullPath((Join-Path $filesRoot $entry.path))
    $restorePath = $entry.path
    if ($restorePath.StartsWith('Java/assets/')) {
        $segments = $restorePath.Substring(12).Split('/', 2)
        $family = $layout.legacy_families.PSObject.Properties[$segments[0]]
        if ($null -ne $family) { $restorePath = 'Java/assets/' + $family.Value + '/' + $segments[1] }
    }
    $destination = [IO.Path]::GetFullPath((Join-Path $workspaceRoot $restorePath))
    if (-not $source.StartsWith($filesRoot + '\', [StringComparison]::OrdinalIgnoreCase) -or
        -not $destination.StartsWith($assetsRoot + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw "Invalid manifest path: $($entry.path)"
    }
    if (Test-Path -LiteralPath $destination) { throw "Refusing to overwrite: $destination" }
    if ((Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash -ne $entry.sha256) {
        throw "Review file has changed: $source"
    }
    [PSCustomObject]@{ Source = $source; Destination = $destination }
}
if ($ValidateOnly) {
    Write-Output "Validated $($moves.Count) archived assets; no files restored."
    exit 0
}
foreach ($move in $moves) {
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $move.Destination) | Out-Null
    Move-Item -LiteralPath $move.Source -Destination $move.Destination
}
Write-Output "Restored $($moves.Count) assets."
