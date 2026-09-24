param(
    [ValidateSet('default', 'original', 'alternate', 'articulated', 'grounded')]
    [string]$Movement = 'default',
    [switch]$Profile,
    [switch]$Editor,
    [switch]$Launcher,
    [string]$Map,
    [switch]$ConservativeJit,
    [switch]$PrintCommand
)
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$mapPath = $null
if ($Map) { $mapPath = (Resolve-Path -LiteralPath $Map).Path }
$javaArguments = @()
if ($ConservativeJit) { $javaArguments += '-XX:TieredStopAtLevel=1' }
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
if ($Editor) { $javaArguments += '--editor' }
elseif ($mapPath) { $javaArguments += '--playtest' }
elseif ($Launcher) { $javaArguments += '--launcher' }
if ($mapPath) { $javaArguments += $mapPath }
if ($PrintCommand) {
    $printedArguments = $javaArguments | ForEach-Object {
        if ($_ -match '\s') { "'" + $_.Replace("'", "''") + "'" } else { $_ }
    }
    Write-Output ('java ' + ($printedArguments -join ' '))
    exit 0
}
Push-Location $root
try {
    & "$PSScriptRoot\build.ps1" -ConservativeJit:$ConservativeJit
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & java @javaArguments
    exit $LASTEXITCODE
} finally { Pop-Location }
