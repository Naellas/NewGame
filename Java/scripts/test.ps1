param([string[]]$Test = @('com.alderfall.game.SmokeTest'))
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$output = 'temp/tests/' + [Guid]::NewGuid().ToString('N')
Push-Location $root
try {
    & "$PSScriptRoot/build.ps1" -IncludeTests -OutputDirectory $output
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $classpath = Join-Path $root $output
    foreach ($class in $Test) {
        if ($class -notmatch '^com\.alderfall\.game(?:\.[A-Za-z_][A-Za-z0-9_]*)+$') { throw "Invalid test class: $class" }
        if ($class -eq 'com.alderfall.game.SmokeTest') {
            # SmokeTest exercises saves; keep them separate from local play progress.
            $working = Join-Path $classpath 'working'
            New-Item -ItemType Directory -Path (Join-Path $working 'config') -Force | Out-Null
            Copy-Item -LiteralPath (Join-Path $root 'config/gameplay.json') -Destination (Join-Path $working 'config/gameplay.json')
            New-Item -ItemType Junction -Path (Join-Path $working 'assets') -Target (Join-Path $root 'assets') | Out-Null
            Push-Location $working
            try {
                & java '-Djava.awt.headless=true' -cp $classpath $class
                if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
            } finally { Pop-Location }
        } else {
            & java '-Djava.awt.headless=true' -cp $classpath $class
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        }
    }
} finally { Pop-Location }
