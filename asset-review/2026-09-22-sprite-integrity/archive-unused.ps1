$ErrorActionPreference = 'Stop'
$workspaceRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$assetRoot = [IO.Path]::GetFullPath((Join-Path $workspaceRoot 'Java/assets'))
$reviewRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'unused'))
$records = Get-Content -Raw (Join-Path $PSScriptRoot 'unused-manifest.json') | ConvertFrom-Json
foreach ($entry in $records) {
    if (-not $entry.original_modified -or [datetime]$entry.original_modified -ge [datetime]'2026-05-27') { throw 'Archive age guard: recent or undated assets must remain intact' }
    $from = [IO.Path]::GetFullPath((Join-Path $workspaceRoot $entry.path))
    $to = [IO.Path]::GetFullPath((Join-Path $workspaceRoot $entry.review_path))
    if (-not $from.StartsWith($assetRoot + [IO.Path]::DirectorySeparatorChar) -or -not $to.StartsWith($reviewRoot + [IO.Path]::DirectorySeparatorChar)) { throw 'Move escapes checked directories' }
    if (Test-Path -LiteralPath $to) {
        if ((Test-Path -LiteralPath $from) -or (Get-FileHash -LiteralPath $to -Algorithm SHA256).Hash -ne $entry.sha256) { throw "Archive conflict: $from" }
        continue
    }
    if ((Get-FileHash -LiteralPath $from -Algorithm SHA256).Hash -ne $entry.sha256) { throw 'Source changed since audit' }
    New-Item -ItemType Directory -Force ([IO.Path]::GetDirectoryName($to)) | Out-Null
    Move-Item -LiteralPath $from -Destination $to
    Write-Output "Archived $($entry.path)"
}
