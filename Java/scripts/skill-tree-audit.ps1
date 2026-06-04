$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

& "$PSScriptRoot\build.ps1"
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

java -cp (Join-Path $root "out") com.alderfall.game.SkillTreeAudit
exit $LASTEXITCODE
