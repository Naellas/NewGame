from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

from pathlib import Path, PurePosixPath
import json

_FAMILIES = json.loads((JAVA_ROOT / 'config/asset-layout.json').read_text(encoding='utf-8'))['legacy_families']


def asset_relative(path: str) -> str:
    """Map a historical asset-relative family/path to its canonical subtree."""
    relative = PurePosixPath(path.replace('\\', '/'))
    if relative.is_absolute() or '..' in relative.parts or ':' in path:
        raise ValueError(f'Expected an asset-relative path: {path}')
    parts = relative.parts
    if not parts:
        return '.'
    return str(PurePosixPath(_FAMILIES.get(parts[0], parts[0]), *parts[1:]))


def family_dir(assets_root: Path, family: str) -> Path:
    return assets_root / asset_relative(family)


PLAYER_CLASSES = ("knight", "mage", "ranger", "cleric", "rogue")
COMPANIONS = ("aria", "calder", "cassia", "lyra", "maera", "rafiq", "samir", "seraphine", "vesper")
STORY_NPCS = (
    "captain_elric_snowrest",
    "gravekeeper_hollis",
    "ash_scribe_damar",
    "bellwright_nessa",
    "elder_rowan",
    "vaelthara",
    "maelis",
    "mirella",
    "odrick",
    "selene",
    "solari",
    "ysra",
)


def player_dir(assets_root: Path, stem: str) -> Path:
    if stem.startswith("class_"):
        parts = stem.split("_")
        if len(parts) >= 2 and parts[1] in PLAYER_CLASSES:
            return assets_root / "characters/player" / "classes" / parts[1]
    return assets_root / "characters/player" / "base"


def companion_dir(assets_root: Path, name_or_stem: str) -> Path:
    name = name_or_stem.removeprefix("npc_").split("_", 1)[0]
    return assets_root / "characters/companions" / name


def story_npc_dir(assets_root: Path, name_or_stem: str) -> Path:
    name = name_or_stem.removeprefix("npc_story_")
    for story_name in STORY_NPCS:
        if name == story_name or name.startswith(story_name + "_"):
            return assets_root / "characters/npcs/story" / "npcs" / story_name
    return assets_root / "characters/npcs/story" / "npcs" / name


def character_dir(assets_root: Path, stem: str, default: Path | None = None) -> Path:
    if stem.startswith("class_") or stem.startswith("player"):
        return player_dir(assets_root, stem)
    if stem.startswith("npc_story_"):
        return story_npc_dir(assets_root, stem)
    if stem.startswith("npc_"):
        parts = stem.split("_")
        if len(parts) >= 2 and parts[1] in COMPANIONS:
            return companion_dir(assets_root, stem)
        return assets_root / "characters/npcs/townsfolk"
    return default if default is not None else assets_root


def animation_dir(assets_root: Path, stem: str) -> Path:
    if stem.startswith("class_") or stem.startswith("player"):
        return player_dir(assets_root, stem) / "animations"
    if stem.startswith("npc_story_"):
        return story_npc_dir(assets_root, stem) / "animations"
    if stem.startswith("npc_"):
        parts = stem.split("_")
        if len(parts) >= 2 and parts[1] in COMPANIONS:
            return companion_dir(assets_root, stem) / "animations"
        return assets_root / "characters/npcs/townsfolk" / "animations"
    if stem.endswith("_attack_anim"):
        return assets_root / "characters/monsters" / "animations"
    if stem.startswith("fx_"):
        return assets_root / "effects" / "animations"
    return assets_root / "characters/shared/animations"


def find_asset(assets_root: Path, filename: str) -> Path:
    direct = assets_root / filename
    if direct.exists():
        return direct
    def historical_order(path: Path) -> tuple[str, str]:
        relative = path.relative_to(assets_root).as_posix()
        # Preserve importer selection when a source sheet and runtime sprite share
        # a basename. Reparenting families must not silently change regeneration.
        for old, new in sorted(_FAMILIES.items(), key=lambda item: -len(item[1])):
            if relative.startswith(new + '/'):
                return old + relative[len(new):], relative
        return relative, relative

    matches = sorted((path for path in assets_root.rglob(filename) if path.is_file()), key=historical_order)
    if not matches:
        raise FileNotFoundError(f"Missing asset {filename} under {assets_root}")
    return matches[0]
