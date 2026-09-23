$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Push-Location $workspace
try {
    python -m unittest discover -s Java/tools/tests -p 'test_repository_*.py'
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    python Java/tools/checks/check_repository_hygiene.py
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    python Java/tools/checks/check_review_site.py
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    python Java/tools/checks/check_structure.py
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally { Pop-Location }
