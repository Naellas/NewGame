from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from PIL import Image

from asset_paths import animation_dir, story_npc_dir


NAMES = [
    "maelis",
    "selene",
    "odrick",
    "solari",
    "ysra",
    "mirella",
    "elder_rowan",
    "gravekeeper_hollis",
    "captain_elric_snowrest",
    "bellwright_nessa",
    "ash_scribe_damar",
    "vaelthara",
]

DETECTED_SHEET_NAMES = [
    "maelis",
    "selene",
    "odrick",
    "solari",
    "ysra",
    "mirella",
    "elder_rowan",
    "gravekeeper_hollis",
    "captain_elric_snowrest",
    "bellwright_nessa",
    "vaelthara",
]


def keyed_alpha(pixel: tuple[int, int, int, int]) -> int:
    r, g, b, a = pixel
    if r > 198 and g > 198 and b > 198 and max(r, g, b) - min(r, g, b) < 18:
        return 0
    green_score = g - max(r, b)
    if g > 178 and green_score > 72:
        return 0
    if g > 132 and green_score > 42:
        return max(0, min(a, (g - 132) * 2))
    return a


def crop_alpha(image: Image.Image, pad: int = 10) -> Image.Image:
    bbox = image.getchannel("A").getbbox()
    if bbox is None:
        return image
    return image.crop((
        max(0, bbox[0] - pad),
        max(0, bbox[1] - pad),
        min(image.width, bbox[2] + pad),
        min(image.height, bbox[3] + pad),
    ))


def chroma_to_alpha(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    keyed = []
    for pixel in rgba.getdata():
        alpha = keyed_alpha(pixel)
        if alpha == 0:
            keyed.append((0, 0, 0, 0))
            continue
        r, g, b, _ = pixel
        if g > r and g > b:
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


def center_on_canvas(image: Image.Image, width: int, height: int, y_offset: int = 0) -> Image.Image:
    canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    x = (width - image.width) // 2
    y = max(0, height - image.height - y_offset)
    canvas.alpha_composite(image, (x, y))
    return canvas


def model_from_dialogue(dialogue: Image.Image) -> Image.Image:
    cropped = crop_alpha(dialogue, 4)
    sprite = normalize_height(cropped, 96)
    if sprite.width > 64:
        sprite = sprite.resize((64, max(1, round(sprite.height * 64 / sprite.width))), Image.Resampling.LANCZOS)
    return center_on_canvas(sprite, 64, 96)


def tint_direction(image: Image.Image, direction: str) -> Image.Image:
    rgba = image.copy()
    pixels = []
    shifts = {
        "down": (0, 0, 0),
        "up": (-12, -8, 10),
        "left": (-5, -2, 6),
        "right": (6, 2, -4),
    }
    dr, dg, db = shifts[direction]
    for r, g, b, a in rgba.getdata():
        if a == 0:
            pixels.append((0, 0, 0, 0))
        else:
            pixels.append((max(0, min(255, r + dr)), max(0, min(255, g + dg)), max(0, min(255, b + db)), a))
    rgba.putdata(pixels)
    return rgba


def walk_sheet(base: Image.Image, direction: str) -> Image.Image:
    frame_w, frame_h = base.size
    sheet = Image.new("RGBA", (frame_w * 4, frame_h), (0, 0, 0, 0))
    offsets = [0, -3, 0, 3]
    for index, offset in enumerate(offsets):
        frame = Image.new("RGBA", (frame_w, frame_h), (0, 0, 0, 0))
        frame.alpha_composite(base, (offset if direction in {"left", "right"} else 0, -abs(offset)))
        sheet.alpha_composite(frame, (index * frame_w, 0))
    return sheet


def component_boxes(image: Image.Image) -> list[tuple[int, int, int, int]]:
    alpha = image.getchannel("A")
    width, height = image.size
    seen: set[tuple[int, int]] = set()
    boxes = []
    pixels = alpha.load()
    for y in range(height):
        for x in range(width):
            if (x, y) in seen or pixels[x, y] <= 20:
                continue
            stack = [(x, y)]
            seen.add((x, y))
            min_x = max_x = x
            min_y = max_y = y
            count = 0
            while stack:
                px, py = stack.pop()
                count += 1
                min_x = min(min_x, px)
                max_x = max(max_x, px)
                min_y = min(min_y, py)
                max_y = max(max_y, py)
                for nx, ny in ((px + 1, py), (px - 1, py), (px, py + 1), (px, py - 1)):
                    if nx < 0 or ny < 0 or nx >= width or ny >= height or (nx, ny) in seen:
                        continue
                    if pixels[nx, ny] <= 20:
                        continue
                    seen.add((nx, ny))
                    stack.append((nx, ny))
            if count > 2000 and max_x - min_x > 28 and max_y - min_y > 48:
                boxes.append((max(0, min_x - 8), max(0, min_y - 8), min(width, max_x + 9), min(height, max_y + 9)))
    boxes.sort(key=lambda box: ((box[1] + box[3]) // 2, (box[0] + box[2]) // 2))
    if len(boxes) <= 1:
        return boxes
    rows: list[list[tuple[int, int, int, int]]] = []
    for box in boxes:
        center_y = (box[1] + box[3]) // 2
        for row in rows:
            row_center = sum((b[1] + b[3]) // 2 for b in row) / len(row)
            if abs(center_y - row_center) < 160:
                row.append(box)
                break
        else:
            rows.append([box])
    sorted_boxes: list[tuple[int, int, int, int]] = []
    for row in sorted(rows, key=lambda row: sum((b[1] + b[3]) // 2 for b in row) / len(row)):
        sorted_boxes.extend(sorted(row, key=lambda box: (box[0] + box[2]) // 2))
    return sorted_boxes


def write_assets(assets_root: Path, name: str, source_image: Image.Image) -> None:
    npc_dir = story_npc_dir(assets_root, name)
    npc_dir.mkdir(parents=True, exist_ok=True)
    anim_dir = animation_dir(assets_root, f"npc_story_{name}_model_down_walk_anim")
    anim_dir.mkdir(parents=True, exist_ok=True)
    dialogue = normalize_height(chroma_to_alpha(source_image), 1400)
    dialogue.save(npc_dir / f"npc_story_{name}_dialogue_sprite.png")

    base_model = model_from_dialogue(dialogue)
    base_model.save(npc_dir / f"npc_story_{name}.png")
    base_model.save(npc_dir / f"npc_story_{name}_model.png")
    for direction in ("down", "left", "right", "up"):
        directional = tint_direction(base_model, direction)
        directional.save(npc_dir / f"npc_story_{name}_model_{direction}.png")
        walk = walk_sheet(directional, direction)
        walk.save(anim_dir / f"npc_story_{name}_model_{direction}_walk_anim.png")
        (anim_dir / f"npc_story_{name}_model_{direction}_walk_anim.frames").write_text("4\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="Slice generated main-story NPC dialogue and model sprites.")
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--assets-root", default=Path("assets"), type=Path)
    parser.add_argument("--detected-sheet", action="store_true", help="Detect separated full-body cutouts instead of slicing a 4x3 grid.")
    parser.add_argument("--single-name", choices=NAMES, help="Import one full-body story character image.")
    args = parser.parse_args()

    source = args.source.resolve()
    out_dir = args.assets_root.resolve() / "story" / "npcs"
    out_dir.mkdir(parents=True, exist_ok=True)
    sources_dir = args.assets_root.resolve() / "story" / "sources"
    sources_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, sources_dir / ("main_story_dialogue_sprites_imagegen_source.png"
                                       if not args.single_name else f"npc_story_{args.single_name}_source.png"))

    if args.single_name:
        write_assets(args.assets_root.resolve(), args.single_name, Image.open(source).convert("RGBA"))
        print(f"Wrote story assets for {args.single_name} to {out_dir}")
        return

    if args.detected_sheet:
        keyed = chroma_to_alpha(Image.open(source).convert("RGBA"))
        boxes = component_boxes(keyed)
        if len(boxes) < len(DETECTED_SHEET_NAMES):
            raise RuntimeError(f"Detected {len(boxes)} characters; expected at least {len(DETECTED_SHEET_NAMES)}.")
        for name, box in zip(DETECTED_SHEET_NAMES, boxes):
            write_assets(args.assets_root.resolve(), name, keyed.crop(box))
        print(f"Wrote {len(DETECTED_SHEET_NAMES)} detected full-body story sprites to {out_dir}")
        return

    sheet = Image.open(source).convert("RGBA")
    cell_w = sheet.width // 4
    cell_h = sheet.height // 3
    for index, name in enumerate(NAMES):
        col = index % 4
        row = index // 4
        cell = sheet.crop((col * cell_w, row * cell_h, (col + 1) * cell_w, (row + 1) * cell_h))
        write_assets(args.assets_root.resolve(), name, cell)

    print(f"Wrote {len(NAMES)} story dialogue sprites and model animation sets to {out_dir}")


if __name__ == "__main__":
    main()
