from __future__ import annotations

from pathlib import Path


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
            return assets_root / "player" / "classes" / parts[1]
    return assets_root / "player" / "base"


def companion_dir(assets_root: Path, name_or_stem: str) -> Path:
    name = name_or_stem.removeprefix("npc_").split("_", 1)[0]
    return assets_root / "companions" / name


def story_npc_dir(assets_root: Path, name_or_stem: str) -> Path:
    name = name_or_stem.removeprefix("npc_story_")
    for story_name in STORY_NPCS:
        if name == story_name or name.startswith(story_name + "_"):
            return assets_root / "story" / "npcs" / story_name
    return assets_root / "story" / "npcs" / name


def character_dir(assets_root: Path, stem: str, default: Path | None = None) -> Path:
    if stem.startswith("class_") or stem.startswith("player"):
        return player_dir(assets_root, stem)
    if stem.startswith("npc_story_"):
        return story_npc_dir(assets_root, stem)
    if stem.startswith("npc_"):
        parts = stem.split("_")
        if len(parts) >= 2 and parts[1] in COMPANIONS:
            return companion_dir(assets_root, stem)
        return assets_root / "npcs"
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
        return assets_root / "npcs" / "animations"
    if stem.endswith("_attack_anim"):
        return assets_root / "monsters" / "animations"
    if stem.startswith("fx_"):
        return assets_root / "effects" / "animations"
    return assets_root / "animations"


def find_asset(assets_root: Path, filename: str) -> Path:
    direct = assets_root / filename
    if direct.exists():
        return direct
    matches = sorted(path for path in assets_root.rglob(filename) if path.is_file())
    if not matches:
        raise FileNotFoundError(f"Missing asset {filename} under {assets_root}")
    return matches[0]
