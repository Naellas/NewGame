"""Validate tool registration and prevent new ambiguous runtime asset IDs."""
from __future__ import annotations

import sys
from pathlib import Path
if not __package__:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
import ast
from collections import defaultdict
import json
import re
from tools.project_paths import JAVA_ROOT


def duplicate_errors(actual: dict[str, set[str]], allowed: dict[str, list[str]]) -> list[str]:
    return [f'Ambiguous asset ID {stem}: {sorted(paths)}'
            for stem, paths in actual.items()
            if len(paths) > 1 and not paths.issubset(set(allowed.get(stem, [])))]


def main() -> int:
    errors = []
    classes = defaultdict(list)
    for tree in (JAVA_ROOT / 'src', JAVA_ROOT / 'tools/reviews'):
        for path in tree.rglob('*.java'):
            match = re.search(r'^package\s+([^;]+);', path.read_text(encoding='utf-8-sig'), re.MULTILINE)
            if match:
                classes[match.group(1) + '.' + path.stem].append(path.relative_to(JAVA_ROOT).as_posix())
    errors.extend(f'Duplicate Java class {name}: {paths}' for name, paths in classes.items() if len(paths) > 1)
    index = json.loads((JAVA_ROOT / 'tools/tool-index.json').read_text(encoding='utf-8'))
    for name, module in index.items():
        path = JAVA_ROOT.joinpath(*module.split('.')).with_suffix('.py')
        if not path.is_file():
            errors.append(f'Missing registered tool {name}: {path}')
    for path in (JAVA_ROOT / 'tools').rglob('*.py'):
        # A missing internal import is caught without importing/running generators.
        if '__pycache__' in path.parts:
            continue
        for node in ast.walk(ast.parse(path.read_text(encoding='utf-8-sig'))):
            if isinstance(node, ast.ImportFrom) and node.module and node.module.startswith('tools.'):
                target = JAVA_ROOT.joinpath(*node.module.split('.'))
                if not target.with_suffix('.py').is_file() and not (target / '__init__.py').is_file():
                    errors.append(f'{path.relative_to(JAVA_ROOT)}: missing import {node.module}')
                elif target.is_dir():
                    for alias in node.names:
                        if alias.name != '*' and not (target / (alias.name + '.py')).exists() and not (target / alias.name).is_dir():
                            errors.append(f'{path.relative_to(JAVA_ROOT)}: missing module {node.module}.{alias.name}')
    catalog = (JAVA_ROOT / 'src/main/java/com/alderfall/game/content/AssetCatalog.java').read_text(encoding='utf-8')
    folders = re.findall(r'"([^"]+)"', catalog.split('FOLDERS = {', 1)[1].split('};', 1)[0])
    assets = JAVA_ROOT / 'assets'
    stems = defaultdict(set)
    files = list(assets.glob('*.png'))
    for folder in folders:
        files.extend((assets / folder).rglob('*.png'))
    for path in files:
        stems[path.stem].add(path.relative_to(assets).as_posix())
    allowed = json.loads((JAVA_ROOT / 'config/asset-duplicate-stems.json').read_text(encoding='utf-8'))['duplicates']
    errors.extend(duplicate_errors(stems, allowed))
    if errors:
        print('\n'.join(errors))
        return 1
    print(f'PASS: {len(index)} registered Python tools, internal imports, and {len(stems)} runtime asset IDs.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
