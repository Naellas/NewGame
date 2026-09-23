param(
    [ValidateSet('default', 'original', 'alternate', 'articulated', 'grounded')]
    [string]$Movement = 'default',
    [switch]$Profile,
    [switch]$PrintCommand
)
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$javaArguments = @()
switch ($Movement) {
    'original' { $javaArguments += '-Dalderfall.alternateMovement=false' }
    'alternate' { $javaArguments += '-Dalderfall.alternateMovement=true' }
    'articulated' { $javaArguments += '-Dalderfall.movementStyle=articulated' }
    'grounded' { $javaArguments += '-Dalderfall.movementStyle=grounded' }
}
if ($Profile) {
    $javaArguments += '-Dalderfall.renderMetrics=true', '-Dalderfall.renderMetricsEvery=120'
}
$javaArguments += '-cp', 'out', 'com.alderfall.game.Main'
if ($PrintCommand) {
    Write-Output ('java ' + ($javaArguments -join ' '))
    exit 0
}
Push-Location $root
try {
    & "$PSScriptRoot\build.ps1"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & java @javaArguments
    exit $LASTEXITCODE
} finally { Pop-Location }
