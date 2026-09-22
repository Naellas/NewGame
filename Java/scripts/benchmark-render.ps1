param([ValidateRange(1, 100000)][int]$Samples = 240)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$buildOutput = Join-Path $projectRoot "out-check\render-benchmark"
New-Item -ItemType Directory -Force -Path $buildOutput | Out-Null
$javaSources = Get-ChildItem -LiteralPath (Join-Path $projectRoot "src\main\java") -Recurse -Filter *.java |
    Sort-Object FullName | ForEach-Object { $_.FullName }
javac -d $buildOutput $javaSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

java "-Duser.dir=$projectRoot" "-Djava.awt.headless=true" -cp $buildOutput com.alderfall.game.RenderCacheTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
java "-Duser.dir=$projectRoot" "-Djava.awt.headless=true" -cp $buildOutput com.alderfall.game.RenderBenchmark $Samples
exit $LASTEXITCODE
