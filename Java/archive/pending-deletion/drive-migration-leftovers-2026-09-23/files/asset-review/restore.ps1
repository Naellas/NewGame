$ErrorActionPreference = 'Stop'
$reviewRoot = [IO.Path]::GetFullPath($PSScriptRoot)
$workspaceRoot = Split-Path -Parent $reviewRoot
$assetsRoot = [IO.Path]::GetFullPath((Join-Path $workspaceRoot 'Java/assets'))
$filesRoot = [IO.Path]::GetFullPath((Join-Path $reviewRoot 'files'))
$entries = Get-Content -Raw -LiteralPath (Join-Path $reviewRoot 'manifest.json') | ConvertFrom-Json
$moves = foreach ($entry in $entries) {
    $source = [IO.Path]::GetFullPath((Join-Path $filesRoot $entry.path))
    $destination = [IO.Path]::GetFullPath((Join-Path $workspaceRoot $entry.path))
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
foreach ($move in $moves) {
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $move.Destination) | Out-Null
    Move-Item -LiteralPath $move.Source -Destination $move.Destination
}
Write-Output "Restored $($moves.Count) assets."
