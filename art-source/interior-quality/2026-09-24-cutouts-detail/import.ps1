param([switch]$RestoreOriginals)
$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$entries = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'inputs-manifest.json') -Raw | ConvertFrom-Json
foreach ($entry in $entries) {
    $source = Join-Path $PSScriptRoot ($entry.id + '.png')
    if ($RestoreOriginals) { $source = Join-Path $PSScriptRoot $entry.input }
    if (!(Test-Path -LiteralPath $source)) { throw "Missing source: $source" }
    $target = [IO.Path]::GetFullPath((Join-Path $repo $entry.asset_file))
    if (!$target.StartsWith((Join-Path $repo 'Java/assets/environments/interiors') + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw "Invalid destination: $target" }
}
foreach ($entry in $entries) {
    $source = Join-Path $PSScriptRoot ($entry.id + '.png')
    if ($RestoreOriginals) { $source = Join-Path $PSScriptRoot $entry.input }
    Copy-Item -LiteralPath $source -Destination (Join-Path $repo $entry.asset_file)
}
Write-Output "Imported $($entries.Count) interior sprites. Original IDs and paths preserved."
