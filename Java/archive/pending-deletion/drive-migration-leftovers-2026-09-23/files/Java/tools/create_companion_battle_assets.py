from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from PIL import Image

from asset_paths import companion_dir


NAMES = [
    "aria",
    "calder",
    "cassia",
    "lyra",
    "maera",
    "rafiq",
    "samir",
    "seraphine",
    "vesper",
]


def keyed_alpha(pixel: tuple[int, int, int, int], key_color: str) -> int:
    r, g, b, a = pixel
    if key_color == "magenta":
        magenta_score = min(r, b) - g
        if r > 178 and b > 178 and magenta_score > 72:
            return 0
        if r > 132 and b > 132 and magenta_score > 42:
            return max(0, min(a, (magenta_score - 42) * 4))
        return a

    green_score = g - max(r, b)
    if g > 178 and green_score > 72:
        return 0
    if g > 132 and green_score > 42:
        return max(0, min(a, (g - 132) * 2))
    return a


def crop_alpha(image: Image.Image) -> Image.Image:
    bbox = image.getchannel("A").getbbox()
    if bbox is None:
        return image
    pad = 8
    return image.crop((
        max(0, bbox[0] - pad),
        max(0, bbox[1] - pad),
        min(image.width, bbox[2] + pad),
        min(image.height, bbox[3] + pad),
    ))


def chroma_to_alpha(image: Image.Image, key_color: str) -> Image.Image:
    rgba = image.convert("RGBA")
    keyed = []
    for pixel in rgba.getdata():
        alpha = keyed_alpha(pixel, key_color)
        if alpha == 0:
            keyed.append((0, 0, 0, 0))
            continue
        r, g, b, _ = pixel
        if key_color == "magenta" and r > g and b > g:
            cap = g + 42
            r = min(r, cap)
            b = min(b, cap)
        elif key_color == "green" and g > r and g > b:
            g = min(g, int((r + b) / 2) + 24)
        keyed.append((r, g, b, alpha))
    rgba.putdata(keyed)
    return crop_alpha(rgba)


def normalize_height(image: Image.Image, target_h: int) -> Image.Image:
    if image.height <= 0:
        return image
    scale = target_h / image.height
    target_w = max(1, round(image.width * scale))
    return image.resize((target_w, target_h), Image.Resampling.LANCZOS)


def main() -> None:
    parser = argparse.ArgumentParser(description="Prepare generated companion combat sprites.")
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--assets-root", default=Path("assets"), type=Path)
    parser.add_argument("--name", required=True, choices=NAMES)
    parser.add_argument("--height", default=320, type=int)
    parser.add_argument("--key-color", choices=["green", "magenta"], default="green")
    args = parser.parse_args()

    source = args.source.resolve()
    out_dir = companion_dir(args.assets_root.resolve(), args.name)
    source_dir = out_dir / "sources"
    out_dir.mkdir(parents=True, exist_ok=True)
    source_dir.mkdir(parents=True, exist_ok=True)

    source_copy = source_dir / f"npc_{args.name}_battle_source.png"
    if source != source_copy.resolve():
        shutil.copy2(source, source_copy)
    sprite = chroma_to_alpha(Image.open(source).convert("RGBA"), args.key_color)
    sprite = normalize_height(sprite, args.height)
    output = out_dir / f"npc_{args.name}_battle_sprite.png"
    sprite.save(output)
    flat_output = args.assets_root.resolve() / "companions" / f"npc_{args.name}_battle_sprite.png"
    if flat_output != output:
        sprite.save(flat_output)
    print(f"Wrote {output}")


if __name__ == "__main__":
    main()
