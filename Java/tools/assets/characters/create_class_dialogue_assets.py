from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import argparse
import shutil
from pathlib import Path

from PIL import Image

from tools.assets.shared.asset_paths import player_dir


NAMES = ["knight", "mage", "ranger", "cleric", "rogue"]


from tools.assets.shared.image_ops import keyed_alpha


def crop_alpha(image: Image.Image) -> Image.Image:
    bbox = image.getchannel("A").getbbox()
    if bbox is None:
        return image
    pad = 10
    return image.crop((
        max(0, bbox[0] - pad),
        max(0, bbox[1] - pad),
        min(image.width, bbox[2] + pad),
        min(image.height, bbox[3] + pad),
    ))


def chroma_to_alpha(image: Image.Image) -> Image.Image:
    from tools.assets.shared import image_ops
    return image_ops.chroma_to_alpha(image, keyed_alpha=keyed_alpha, crop_alpha=crop_alpha)


from tools.assets.shared.image_ops import normalize_height


def main() -> None:
    parser = argparse.ArgumentParser(description="Prepare generated player class dialogue sprites.")
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--assets-root", default=Path("assets"), type=Path)
    parser.add_argument("--single-name", choices=NAMES, help="Import one full-body class image instead of slicing a sheet.")
    args = parser.parse_args()

    source = args.source.resolve()
    classes_dir = args.assets_root.resolve() / "characters/player" / "classes"
    classes_dir.mkdir(parents=True, exist_ok=True)
    source_dir = classes_dir / "sources"
    source_dir.mkdir(parents=True, exist_ok=True)

    if args.single_name:
        out_dir = player_dir(args.assets_root.resolve(), f"class_{args.single_name}")
        out_dir.mkdir(parents=True, exist_ok=True)
        class_source_dir = out_dir / "sources"
        class_source_dir.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, class_source_dir / f"class_{args.single_name}_dialogue_source.png")
        sprite = chroma_to_alpha(Image.open(source).convert("RGBA"))
        sprite = normalize_height(sprite, 1400)
        sprite.save(out_dir / f"class_{args.single_name}_dialogue_sprite.png")
        print(f"Wrote class_{args.single_name}_dialogue_sprite.png to {out_dir}")
        return

    shutil.copy2(source, source_dir / "class_dialogue_sprites_imagegen_source.png")

    sheet = Image.open(source).convert("RGBA")
    cell_w = sheet.width // len(NAMES)
    for index, name in enumerate(NAMES):
        cell = sheet.crop((index * cell_w, 0, (index + 1) * cell_w, sheet.height))
        sprite = chroma_to_alpha(cell)
        sprite = normalize_height(sprite, 1400)
        out_dir = player_dir(args.assets_root.resolve(), f"class_{name}")
        out_dir.mkdir(parents=True, exist_ok=True)
        sprite.save(out_dir / f"class_{name}_dialogue_sprite.png")

    print(f"Wrote {len(NAMES)} class dialogue sprites to class asset folders")


if __name__ == "__main__":
    main()
