"""Read-only repository policy check. Run from any directory; standard library only."""
from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[2]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import json
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys

ROOT = REPO_ROOT
BASELINE = ROOT / 'Java/config/repository-hygiene-baseline.json'
ALLOWED_ROOTS = {'Java', 'Story premise', 'asset-review', 'art-source', '.github'}
ALLOWED_ROOT_FILES = {'README.md', 'AGENTS.md', '.gitignore', '.gitattributes'}
MOVED_REVIEWS = {'characters', 'movement-physics', 'cloth-physics',
                 'grounded-movement', 'dialogue-motion', 'monster-attacks-2026-09-23'}
TOP_LEVEL_TOOLS = {'__init__.py', 'project_paths.py', 'run.py', 'asset_paths.py',
                   'universal_cutout.py', 'musicgen.py', 'regenerate_universal_assets.py',
                   'check_repository_hygiene.py', 'check_review_site.py'}
LEGACY_ASSET_FAMILIES = set(json.loads((ROOT / 'Java/config/asset-layout.json').read_text(encoding='utf-8'))['legacy_families'])


def repository_paths(root: Path) -> list[str]:
    result = subprocess.run(
        ['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'],
        cwd=root, check=True, capture_output=True,
    )
    return sorted({p for p in result.stdout.decode('utf-8').split('\0')
                   if p and (root / p).is_file()})


def violations(paths: list[str]) -> set[str]:
    findings = set()
    for path in paths:
        p = PurePosixPath(path)
        parts = p.parts
        if path.startswith('Java/src/main/java/') and (
                p.stem.endswith(('Test', 'Preview', 'Review', 'Benchmark', 'Audit', 'QaExport'))
                or 'diagnostics' in parts):
            findings.add('diagnostic-in-production:' + path)
        if len(parts) == 3 and parts[:2] == ('Java', 'tools') and p.suffix in {'.py', '.java', '.ps1'} and p.name not in TOP_LEVEL_TOOLS:
            findings.add('ungrouped-tool:' + path)
        if len(parts) > 2 and parts[:2] == ('Java', 'assets') and parts[2] in LEGACY_ASSET_FAMILIES:
            findings.add('retired-asset-family:' + path)
        if len(parts) > 1 and parts[0] == 'asset-review' and parts[1] in MOVED_REVIEWS:
            if path != 'asset-review/characters/index.html':
                findings.add('review-outside-tools:' + path)
        if (len(parts) == 1 and path not in ALLOWED_ROOT_FILES) or (
                len(parts) > 1 and parts[0] not in ALLOWED_ROOTS):
            findings.add('unapproved-root:' + path)
        if p.suffix.lower() in {'.class', '.pyc', '.jfr'} or '__pycache__' in parts:
            findings.add('generated-file:' + path)
        if re.match(r'^(?:Java/)?(?:out(?:-[^/]+)?|temp|saves|exports|build)/', path):
            findings.add('local-output:' + path)
        if path == 'Java/config/settings.properties':
            findings.add('local-settings:' + path)
        if path.startswith('Java/assets/') and (
                {'source', 'sources', 'character-refresh'} & set(parts[2:])):
            findings.add('source-in-runtime:' + path)
    return findings


def main() -> int:
    try:
        baseline = json.loads(BASELINE.read_text(encoding='utf-8'))
        if baseline.get('version') != 1 or not isinstance(baseline.get('exceptions'), list):
            raise ValueError('Unsupported or malformed baseline')
        allowed = set(baseline['exceptions'])
        current = violations(repository_paths(ROOT))
    except (OSError, ValueError, TypeError, subprocess.CalledProcessError) as exc:
        print(f'Hygiene check could not run: {exc}', file=sys.stderr)
        return 2
    new = sorted(current - allowed)
    if new:
        print('\n'.join(new))
        print(f'FAIL: {len(new)} new violation(s). See AGENTS.md; do not auto-expand the baseline.')
        return 1
    print(f'PASS: no new hygiene violations; {len(current & allowed)} existing exceptions remain.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
