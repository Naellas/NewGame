from __future__ import annotations

import argparse
import os
from collections.abc import Callable
from dataclasses import dataclass
from pathlib import Path

import extract_beach_assets
import extract_directional_character_assets
import extract_goblin_monster_assets
import extract_item_icon_sheets
import extract_mage_directional_assets
import extract_monster_expansion_assets
import extract_new_npc_class_assets
import extract_npc_variation_assets
import generate_monster_assets
import generate_npc_assets
import generate_player_model_assets
import process_mage_cast_animation_sheet
import process_walk15_animations
import slice_imagegen_village_assets
import slice_resource_props
import verify_universal_cutout_assets


ROOT = Path(__file__).resolve().parents[1]


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
