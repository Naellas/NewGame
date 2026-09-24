param(
    [string]$OutputDirectory = 'out',
    [switch]$IncludeTests,
    [switch]$IncludeReviews,
    [switch]$ConservativeJit
)
$ErrorActionPreference = "Stop"

# Honor an explicit JDK; otherwise prefer the project's installed Java 21 over
# Oracle's PATH shim, which may select a newer compiler. Keep this process-local.
$jdkHome = $env:JAVA_HOME
if (-not $jdkHome) {
    $jdkHome = Get-ChildItem -Path (Join-Path $env:ProgramFiles 'Java/jdk-21*'),
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium/jdk-21*') -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if ($jdkHome) {
    $jdkBin = Join-Path $jdkHome 'bin'
    if (-not (Test-Path -LiteralPath (Join-Path $jdkBin 'javac.exe')) -or
        -not (Test-Path -LiteralPath (Join-Path $jdkBin 'java.exe'))) {
        throw "JAVA_HOME must point to a JDK containing java.exe and javac.exe: $jdkHome"
    }
    $env:PATH = $jdkBin + [IO.Path]::PathSeparator + $env:PATH
}


$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$outputPath = [System.IO.Path]::GetFullPath((Join-Path $root $OutputDirectory))
$defaultOutput = [System.IO.Path]::GetFullPath((Join-Path $root 'out'))
$scratchRoot = [System.IO.Path]::GetFullPath((Join-Path $root 'temp'))
if ($outputPath -ne $defaultOutput -and
    -not $outputPath.StartsWith($scratchRoot + '\', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Build output must be out or a task directory beneath Java/temp.'
}
if (($IncludeReviews -or $IncludeTests) -and $outputPath -eq $defaultOutput) {
    throw 'Tests and review sources must be compiled to a task directory beneath Java/temp.'
}

function Assert-SafeBuildItem([System.IO.FileSystemInfo]$Item) {
    if ($Item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) {
        # OneDrive Files On-Demand uses CLOUD/CLOUD_1..F tags, which do not
        # redirect paths. Allow only those tags; unknown tags fail closed.
        if ($Item.LinkType -or $Item.Target) {
            throw "Refusing to clear a redirected build output: $($Item.FullName)"
        }
        $tagInfo = & fsutil.exe reparsepoint query $Item.FullName 2>&1
        $tagExitCode = $LASTEXITCODE
        $tag = [regex]::Match(($tagInfo -join "`n"), '0x[0-9a-fA-F]{8}').Value
        if ($tagExitCode -ne 0 -or $tag -notmatch '^0x9000[0-9a-f]01a$') {
            throw "Refusing to clear an unrecognized reparse point: $($Item.FullName)"
        }
    }
}

# Check ancestors too: lexical containment alone does not protect a task path
# reached through a junction. Include the workspace and its parent directories.
$ancestor = $outputPath
while ($ancestor) {
    if (Test-Path -LiteralPath $ancestor) {
        Assert-SafeBuildItem (Get-Item -LiteralPath $ancestor -Force)
    }
    $ancestor = Split-Path -Parent $ancestor
}
if (Test-Path -LiteralPath $outputPath) {
    $resolvedOutput = (Resolve-Path -LiteralPath $outputPath).Path
    if ($resolvedOutput -ne $outputPath) {
        throw "Refusing to clear a redirected build output: $outputPath"
    }
    # Preflight the entire tree before deleting anything. Inspect each directory
    # before descending so a nested junction cannot lead enumeration outside it.
    $pending = [System.Collections.Generic.Stack[string]]::new()
    $pending.Push($resolvedOutput)
    while ($pending.Count -gt 0) {
        foreach ($item in Get-ChildItem -LiteralPath $pending.Pop() -Force) {
            Assert-SafeBuildItem $item
            if ($item.PSIsContainer) { $pending.Push($item.FullName) }
        }
    }
    Remove-Item -LiteralPath $resolvedOutput -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $outputPath | Out-Null
$sources = Get-ChildItem -Path src\main\java -Recurse -Filter *.java | Sort-Object FullName | ForEach-Object { $_.FullName }
if ($IncludeTests -or $IncludeReviews) {
    $sources += Get-ChildItem -Path src\testSupport\java -Recurse -Filter *.java | Sort-Object FullName | ForEach-Object { $_.FullName }
}
if ($IncludeTests) {
    $sources += Get-ChildItem -Path src\test\java -Recurse -Filter *.java | Sort-Object FullName | ForEach-Object { $_.FullName }
}
if ($IncludeReviews) {
    $sources += Get-ChildItem -Path src\review\java -Recurse -Filter *.java | Sort-Object FullName | ForEach-Object { $_.FullName }
    $sources += Get-ChildItem -Path tools\reviews -Recurse -Filter *.java | Sort-Object FullName | ForEach-Object { $_.FullName }
}
# An argument file avoids Windows' command-length limit as the source tree grows.
$sourceList = Join-Path $outputPath 'sources.args'
$sourceLines = $sources | ForEach-Object { '"' + $_.Replace('\', '/') + '"' }
[System.IO.File]::WriteAllLines($sourceList, [string[]]$sourceLines, [System.Text.UTF8Encoding]::new($false))
$compilerArguments = @()
if ($ConservativeJit) { $compilerArguments += '-J-XX:TieredStopAtLevel=1' }
javac @compilerArguments --release 21 -encoding UTF-8 -d $outputPath "@$sourceList"
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
Write-Host "Built Java classes into $outputPath"
