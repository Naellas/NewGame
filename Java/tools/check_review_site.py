"""Compatibility entrypoint; implementation: tools.checks.check_review_site."""
import sys
from pathlib import Path
import importlib
import runpy
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
if __name__ == "__main__":
    runpy.run_module("tools.checks.check_review_site", run_name="__main__")
else:
    sys.modules[__name__] = importlib.import_module("tools.checks.check_review_site")
