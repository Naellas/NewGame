param([ValidateRange(1, 100000)][int]$Samples = 240)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$relativeOutput = 'temp/benchmarks/' + [Guid]::NewGuid().ToString('N')
$buildOutput = Join-Path $projectRoot $relativeOutput
& "$PSScriptRoot/build.ps1" -IncludeTests -IncludeReviews -OutputDirectory $relativeOutput
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

java "-Duser.dir=$projectRoot" "-Djava.awt.headless=true" -cp $buildOutput com.alderfall.game.RenderCacheTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
java "-Duser.dir=$projectRoot" "-Djava.awt.headless=true" -cp $buildOutput com.alderfall.game.RenderBenchmark $Samples
exit $LASTEXITCODE
