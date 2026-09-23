"""Stable filesystem anchors shared by development tools."""
from pathlib import Path

JAVA_ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = JAVA_ROOT.parent
