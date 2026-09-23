from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import argparse
from pathlib import Path

from PIL import Image

from tools.assets.shared.asset_paths import animation_dir, companion_dir
from tools.assets.shared.universal_cutout import CutoutSettings, fit, universal_cutout


NAMES = [
    "npc_seraphine",
    "npc_maera",
    "npc_cassia",
    "npc_lyra",
    "npc_samir",
    "npc_aria",
    "npc_vesper",
    "npc_rafiq",
    "npc_calder",
]


def cut_cell(sheet: Image.Image, index: int) -> Image.Image:
    left = round(index * sheet.width / len(NAMES))
    right = round((index + 1) * sheet.width / len(NAMES))
    cell = sheet.crop((left, 0, right, sheet.height))
    return universal_cutout(
        cell,
        CutoutSettings(
            mode="green",
            padding=8,
            global_key=True,
            stray_max_gap=24,
            drop_edge_strays=True,
            drop_small_green_matte=True,
            spill_passes=5,
        ),
    )


def save_directionals(sprite: Image.Image, name: str, out_dir: Path) -> None:
    target_dir = companion_dir(out_dir.parent, name)
    target_dir.mkdir(parents=True, exist_ok=True)
    target_anim_dir = animation_dir(out_dir.parent, f"{name}_model_down_walk_anim")
    target_anim_dir.mkdir(parents=True, exist_ok=True)
    model = fit(sprite, 96, 128, bottom_align=True, margin=4)
    model.save(target_dir / f"{name}_model.png")
    fit(sprite, 96, 96, bottom_align=False, margin=6).save(target_dir / f"{name}.png")

    down = model
    up = muted_copy(model)
    left = model.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    right = model
    directions = {
        "down": down,
        "up": up,
        "left": left,
        "right": right,
    }
    for direction, image in directions.items():
        image.save(target_dir / f"{name}_model_{direction}.png")
        make_walk_strip(image, direction).save(target_anim_dir / f"{name}_model_{direction}_walk_anim.png")
        (target_anim_dir / f"{name}_model_{direction}_walk_anim.frames").write_text("4", encoding="utf-8")


def muted_copy(image: Image.Image) -> Image.Image:
    out = image.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a > 0:
                px[x, y] = (max(0, r - 10), max(0, g - 10), max(0, b - 6), a)
    return out


def make_walk_strip(image: Image.Image, direction: str) -> Image.Image:
    frame_w, frame_h = image.size
    strip = Image.new("RGBA", (frame_w * 4, frame_h), (0, 0, 0, 0))
    offsets = [0, -2, 0, 2] if direction in {"left", "right"} else [0, 1, 0, -1]
    lifts = [0, -2, 0, -1]
    for frame, offset in enumerate(offsets):
        canvas = Image.new("RGBA", image.size, (0, 0, 0, 0))
        dx = max(-2, min(2, offset if direction in {"left", "right"} else 0))
        dy = max(-2, min(2, lifts[frame]))
        canvas.alpha_composite(image, (dx, dy))
        strip.alpha_composite(canvas, (frame * frame_w, 0))
    return strip


def main() -> None:
    parser = argparse.ArgumentParser(description="Import generated special recruit NPC sheet.")
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--out", default=Path("assets/characters/companions"), type=Path)
    args = parser.parse_args()

    args.out.mkdir(parents=True, exist_ok=True)
    source_dir = args.out / "sources"
    source_dir.mkdir(parents=True, exist_ok=True)
    source_copy = source_dir / "special_recruit_npcs_imagegen_source.png"
    sheet = Image.open(args.source).convert("RGBA")
    sheet.save(source_copy)
    for index, name in enumerate(NAMES):
        save_directionals(cut_cell(sheet, index), name, args.out)
    print(f"Wrote {len(NAMES)} special NPC asset sets to {args.out}")


if __name__ == "__main__":
    main()
