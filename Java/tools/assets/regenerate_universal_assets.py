from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[2]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import argparse
import os
from collections.abc import Callable
from dataclasses import dataclass
from pathlib import Path

from tools.assets.environments import extract_beach_assets
from tools.assets.characters import extract_directional_character_assets
from tools.assets.monsters import extract_goblin_monster_assets
from tools.assets.items import extract_item_icon_sheets
from tools.assets.characters import extract_mage_directional_assets
from tools.assets.monsters import extract_monster_expansion_assets
from tools.assets.characters import extract_new_npc_class_assets
from tools.assets.characters import extract_npc_variation_assets
from tools.assets.monsters import generate_monster_assets
from tools.assets.characters import generate_npc_assets
from tools.assets.environments import generate_player_model_assets
from tools.assets.characters import process_mage_cast_animation_sheet
from tools.assets.characters import process_walk15_animations
from tools.assets.environments import slice_imagegen_village_assets
from tools.assets.items import slice_resource_props
from tools.checks import verify_universal_cutout_assets


ROOT = JAVA_ROOT


@dataclass(frozen=True)
class Step:
    name: str
    run: Callable[[], None]
    optional_sources: tuple[Path, ...] = ()


def mage_cast_sources() -> tuple[Path, ...]:
    return (
        ROOT / "assets" / "source" / "imagegen-mage-cast-sheet-10.png",
        ROOT / "assets" / "source" / "imagegen-mage-cast-sheet.png",
    )


def steps(include_walk: bool) -> list[Step]:
    items = [
        Step("beach assets", extract_beach_assets.main),
        Step("item icon sheets", extract_item_icon_sheets.main),
        Step("base NPC models", generate_npc_assets.main),
        Step("NPC variations", extract_npc_variation_assets.main),
        Step("player/class models", generate_player_model_assets.main),
        Step("base monsters", generate_monster_assets.main),
        Step("directional characters", extract_directional_character_assets.main),
        Step("village buildings and props", slice_imagegen_village_assets.main),
        Step("resource props", slice_resource_props.main),
        Step("goblin monsters", extract_goblin_monster_assets.extract),
        Step("monster expansion", extract_monster_expansion_assets.main),
        Step("new NPC classes", extract_new_npc_class_assets.main),
        Step("mage directional sprites", extract_mage_directional_assets.main),
        Step("mage cast animation", process_mage_cast_animation_sheet.main, mage_cast_sources()),
    ]
    if include_walk:
        items.insert(7, Step("walk animations", process_walk15_animations.main))
    return items


def should_skip(step: Step) -> bool:
    return bool(step.optional_sources) and not any(path.exists() for path in step.optional_sources)


def main() -> None:
    parser = argparse.ArgumentParser(description="Regenerate project assets through the universal cutout pipeline.")
    parser.add_argument("--skip-walk", action="store_true", help="Skip the slower 108-strip walk animation regeneration.")
    parser.add_argument("--verify", action="store_true", help="Run the asset replacement proof after regeneration.")
    args = parser.parse_args()

    os.chdir(ROOT)
    for step in steps(include_walk=not args.skip_walk):
        if should_skip(step):
            sources = ", ".join(path.relative_to(ROOT).as_posix() for path in step.optional_sources)
            print(f"Skipped {step.name}: missing optional source ({sources})")
            continue
        print(f"Regenerating {step.name}...")
        step.run()

    if args.verify:
        print("Verifying regenerated assets...")
        verify_universal_cutout_assets.main()


if __name__ == "__main__":
    main()
