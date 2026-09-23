$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$output = "temp/skill-audit/" + [Guid]::NewGuid().ToString("N")
& "$PSScriptRoot\build.ps1" -IncludeReviews -OutputDirectory $output
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

java -cp (Join-Path $root $output) com.alderfall.game.SkillTreeAudit
exit $LASTEXITCODE
