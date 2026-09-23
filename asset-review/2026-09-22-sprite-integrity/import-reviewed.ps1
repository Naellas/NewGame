$ErrorActionPreference = 'Stop'
$reviewRoot = $PSScriptRoot
$workspaceRoot = [IO.Path]::GetFullPath((Join-Path $reviewRoot '../..'))
$jobs = Get-Content -Raw (Join-Path $reviewRoot 'replacements.json') | ConvertFrom-Json
$inventory = Get-Content -Raw (Join-Path $reviewRoot 'inventory.json') | ConvertFrom-Json
foreach ($job in $jobs) {
    if ($job.archived_to) { continue }
    $sourcePath = [IO.Path]::GetFullPath((Join-Path $workspaceRoot $job.source))
    $destination = [IO.Path]::GetFullPath((Join-Path $workspaceRoot $job.target))
    $assetRoot = [IO.Path]::GetFullPath((Join-Path $workspaceRoot 'Java/assets'))
    if (-not $destination.StartsWith($assetRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Target outside assets' }
    $backup = Join-Path (Join-Path $reviewRoot 'before') $job.target
    $expected = ($inventory | Where-Object { $_.path -eq $job.target }).sha256
    if (-not $expected) { throw "Missing baseline: $($job.target)" }
    if (Test-Path -LiteralPath $backup) {
        if ((Get-FileHash -LiteralPath $backup -Algorithm SHA256).Hash -ne $expected) { throw "Backup mismatch: $backup" }
        $current = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash
        $generated = (Get-FileHash -LiteralPath $sourcePath -Algorithm SHA256).Hash
        if ($current -ne $expected -and $current -ne $generated) { throw "Asset changed since review: $destination" }
    } else {
        if ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -ne $expected) { throw "Asset changed since review: $destination" }
        New-Item -ItemType Directory -Force ([IO.Path]::GetDirectoryName($backup)) | Out-Null
        Copy-Item -LiteralPath $destination -Destination $backup
    }
    Copy-Item -LiteralPath $sourcePath -Destination $destination
    Write-Output "Imported $($job.target)"
}
