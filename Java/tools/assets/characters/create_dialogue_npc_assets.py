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

from tools.assets.shared.asset_paths import companion_dir


NAMES = [
    "aria",
    "seraphine",
    "maera",
    "cassia",
    "lyra",
    "samir",
    "vesper",
    "rafiq",
    "calder",
]


from tools.assets.shared.image_ops import keyed_alpha


def chroma_to_alpha(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    pixels = list(rgba.getdata())
    keyed = []
    for pixel in pixels:
        alpha = keyed_alpha(pixel)
        if alpha == 0:
            keyed.append((0, 0, 0, 0))
        else:
            r, g, b, a = pixel
            if g > r and g > b:
                g = min(g, int((r + b) / 2) + 24)
            keyed.append((r, g, b, alpha))
    rgba.putdata(keyed)
    return crop_alpha(rgba)


def crop_alpha(image: Image.Image) -> Image.Image:
    alpha = image.getchannel("A")
    bbox = alpha.getbbox()
    if bbox is None:
        return image
    pad = 10
    left = max(0, bbox[0] - pad)
    top = max(0, bbox[1] - pad)
    right = min(image.width, bbox[2] + pad)
    bottom = min(image.height, bbox[3] + pad)
    return image.crop((left, top, right, bottom))


from tools.assets.shared.image_ops import normalize_height


def main() -> None:
    parser = argparse.ArgumentParser(description="Slice generated companion dialogue sprite sheets.")
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--assets-root", default=Path("assets"), type=Path)
    parser.add_argument("--single-name", choices=NAMES, help="Process one full-image companion cutout instead of a 3x3 sheet.")
    args = parser.parse_args()

    source = args.source.resolve()
    assets_root = args.assets_root.resolve()
    out_dir = assets_root / "characters/companions"
    out_dir.mkdir(parents=True, exist_ok=True)
    sources_dir = out_dir / "sources"
    sources_dir.mkdir(parents=True, exist_ok=True)

    if args.single_name:
        npc_dir = companion_dir(assets_root, args.single_name)
        npc_dir.mkdir(parents=True, exist_ok=True)
        npc_sources = npc_dir / "sources"
        npc_sources.mkdir(parents=True, exist_ok=True)
        source_copy = npc_sources / f"npc_{args.single_name}_dialogue_source.png"
        shutil.copy2(source, source_copy)
        sprite = chroma_to_alpha(Image.open(source).convert("RGBA"))
        sprite = normalize_height(sprite, 1400)
        sprite.save(npc_dir / f"npc_{args.single_name}_dialogue_sprite.png")
        print(f"Wrote npc_{args.single_name}_dialogue_sprite.png to {npc_dir}")
        print(f"Copied source image to {source_copy}")
        return

    source_copy = out_dir / "special_recruit_dialogue_sprites_imagegen_source.png"
    shutil.copy2(source, source_copy)

    sheet = Image.open(source).convert("RGBA")
    cell_w = sheet.width // 3
    cell_h = sheet.height // 3
    for index, name in enumerate(NAMES):
        col = index % 3
        row = index // 3
        cell = sheet.crop((col * cell_w, row * cell_h, (col + 1) * cell_w, (row + 1) * cell_h))
        sprite = chroma_to_alpha(cell)
        sprite = normalize_height(sprite, 1100)
        npc_dir = companion_dir(assets_root, name)
        npc_dir.mkdir(parents=True, exist_ok=True)
        sprite.save(npc_dir / f"npc_{name}_dialogue_sprite.png")

    print(f"Wrote {len(NAMES)} dialogue sprites to companion asset folders")
    print(f"Copied source sheet to {source_copy}")


if __name__ == "__main__":
    main()
