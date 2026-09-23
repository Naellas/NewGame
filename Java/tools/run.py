"""Run a registered Python tool: python tools/run.py --list | NAME [arguments]."""
from pathlib import Path
import json
import os
import runpy
import sys

root = Path(__file__).resolve().parent
sys.path.insert(0, str(root.parent))
registry = json.loads((root / 'tool-index.json').read_text(encoding='utf-8'))
if len(sys.argv) < 2 or sys.argv[1] == '--list':
    for name, module in sorted(registry.items()):
        print(f'{name:48} {module}')
else:
    name = sys.argv.pop(1)
    if name not in registry:
        raise SystemExit(f'Unknown tool: {name}. Use --list.')
    os.chdir(root.parent)
    runpy.run_module(registry[name], run_name='__main__')
