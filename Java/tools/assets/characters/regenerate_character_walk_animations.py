from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import argparse
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from tools.assets.shared.asset_paths import animation_dir


ROOT = Path("assets")
DIRECTIONS = ("down", "left", "right", "up")
FRAMES = 18
PREVIEW_OUT = ROOT / "source" / "walk18" / "preview-character-walk-contact-sheet.png"


def alpha_bbox(image: Image.Image) -> tuple[int, int, int, int] | None:
    return image.getchannel("A").getbbox()


def clamp(value: int, low: int, high: int) -> int:
    return max(low, min(high, value))


from tools.assets.shared.image_ops import clip_blit


def band(image: Image.Image, top: int, bottom: int) -> Image.Image:
    from tools.assets.shared import image_ops
    return image_ops.band(image, top, bottom, clamp=clamp)


def motion(direction: str, frame: int) -> tuple[int, int, int, int]:
    phase = (frame / FRAMES) * math.tau
    stride = math.sin(phase)
    lift = math.sin(phase * 2.0)
    bounce = -1 if lift > 0.35 else 0

    if direction in {"left", "right"}:
        lead = 1 if direction == "right" else -1
        torso_dx = round(lead * stride * 0.55)
        upper_dx = round(-lead * stride * 0.35)
        lower_dx = round(lead * stride * 1.15)
        return upper_dx, torso_dx, lower_dx, bounce

    sway = round(stride * 0.7)
    upper_dx = -sway
    torso_dx = 0
    lower_dx = sway
    return upper_dx, torso_dx, lower_dx, bounce


def pose_frame(source: Image.Image, direction: str, frame: int) -> Image.Image:
    image = source.convert("RGBA")
    box = alpha_bbox(image)
    if box is None:
        return Image.new("RGBA", image.size, (0, 0, 0, 0))

    left, top, right, bottom = box
    height = bottom - top
    shoulder = top + round(height * 0.38)
    hip = top + round(height * 0.63)
    knee = top + round(height * 0.80)

    upper_dx, torso_dx, lower_dx, dy = motion(direction, frame)
    x_low = -left
    x_high = image.width - right
    y_low = -top
    y_high = image.height - bottom
    dy = clamp(dy, y_low, y_high)
    upper_dx = clamp(upper_dx, x_low, x_high)
    torso_dx = clamp(torso_dx, x_low, x_high)
    lower_dx = clamp(lower_dx, x_low, x_high)

    out = Image.new("RGBA", image.size, (0, 0, 0, 0))
    clip_blit(out, band(image, 0, shoulder + 2), upper_dx, dy)
    clip_blit(out, band(image, shoulder - 2, knee + 2), torso_dx, dy)
    clip_blit(out, band(image, hip - 2, image.height), lower_dx, dy)

    # Keep the planted-foot frames grounded so the walk cycle does not appear to float.
    if frame % 9 == 0:
        clip_blit(out, band(image, knee, image.height), 0, 0)
    return out


def write_strip(source_path: Path, stem: str, direction: str, assets_root: Path, frames: int) -> Path:
    source = Image.open(source_path).convert("RGBA")
    strip = Image.new("RGBA", (source.width * frames, source.height), (0, 0, 0, 0))
    for index in range(frames):
        frame = pose_frame(source, direction, index)
        strip.alpha_composite(frame, (index * source.width, 0))

    anim_name = f"{stem}_{direction}_walk_anim"
    out_dir = animation_dir(assets_root, anim_name)
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / f"{anim_name}.png"
    strip.save(out_path)
    (out_dir / f"{anim_name}.frames").write_text(f"{frames}\n", encoding="utf-8")
    return out_path


def discover_models(assets_root: Path) -> list[tuple[str, dict[str, Path]]]:
    candidates: dict[str, dict[str, Path]] = {}
    search_roots = [
        assets_root / "characters/npcs/townsfolk",
        assets_root / "characters/companions",
        assets_root / "characters/npcs/story" / "npcs",
    ]
    for root in search_roots:
        if not root.exists():
            continue
        for path in root.rglob("*_model_*.png"):
            name = path.stem
            for direction in DIRECTIONS:
                suffix = f"_model_{direction}"
                if name.endswith(suffix):
                    stem = name.removesuffix(f"_{direction}")
                    candidates.setdefault(stem, {})[direction] = path
                    break
    return sorted(
        (stem, directions)
        for stem, directions in candidates.items()
        if all(direction in directions for direction in DIRECTIONS)
    )


def preview_frame(path: Path, frames: int, index: int = 4) -> Image.Image:
    sheet = Image.open(path).convert("RGBA")
    frame_w = sheet.width // frames
    return sheet.crop((index * frame_w, 0, (index + 1) * frame_w, sheet.height))


def trim(image: Image.Image) -> Image.Image:
    box = alpha_bbox(image)
    if box is None:
        return image
    return image.crop(box)


def render_preview(paths: list[Path], frames: int, out_path: Path) -> None:
    cell_w = 118
    cell_h = 132
    label_h = 18
    columns = 8
    rows = math.ceil(len(paths) / columns)
    sheet = Image.new("RGBA", (columns * cell_w, rows * (cell_h + label_h)), (28, 30, 36, 255))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("arial.ttf", 10)
    except OSError:
        font = ImageFont.load_default()

    for index, path in enumerate(paths):
        frame = trim(preview_frame(path, frames))
        if frame.width > 0 and frame.height > 0:
            scale = min((cell_w - 12) / frame.width, (cell_h - 12) / frame.height, 1.0)
            frame = frame.resize(
                (max(1, round(frame.width * scale)), max(1, round(frame.height * scale))),
                Image.Resampling.NEAREST,
            )
        col = index % columns
        row = index // columns
        x = col * cell_w + (cell_w - frame.width) // 2
        y = row * (cell_h + label_h) + cell_h - frame.height
        sheet.alpha_composite(frame, (x, y))
        label = path.stem.removesuffix("_walk_anim")
        draw.text((col * cell_w + 4, row * (cell_h + label_h) + cell_h + 2), label[:22], fill=(236, 234, 220), font=font)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out_path)


def audit(paths: list[Path], frames: int) -> list[str]:
    issues: list[str] = []
    for path in paths:
        sheet = Image.open(path).convert("RGBA")
        if sheet.width % frames != 0:
            issues.append(f"{path}: width {sheet.width} is not divisible by {frames}")
            continue
        frame_w = sheet.width // frames
        if frame_w <= 0:
            issues.append(f"{path}: invalid frame width")
            continue
        blank = 0
        for index in range(frames):
            frame = sheet.crop((index * frame_w, 0, (index + 1) * frame_w, sheet.height))
            if alpha_bbox(frame) is None:
                blank += 1
        if blank:
            issues.append(f"{path}: {blank} blank frames")
    return issues


def main() -> None:
    parser = argparse.ArgumentParser(description="Regenerate fixed-size 18-frame walk strips for NPCs and companions.")
    parser.add_argument("--assets-root", default=ROOT, type=Path)
    parser.add_argument("--frames", default=FRAMES, type=int)
    parser.add_argument("--no-preview", action="store_true")
    args = parser.parse_args()

    models = discover_models(args.assets_root)
    outputs: list[Path] = []
    for stem, directions in models:
        for direction in DIRECTIONS:
            outputs.append(write_strip(directions[direction], stem, direction, args.assets_root, args.frames))

    issues = audit(outputs, args.frames)
    if not args.no_preview:
        render_preview(outputs, args.frames, PREVIEW_OUT)

    print(f"Wrote {len(outputs)} walk animation strips from {len(models)} character models")
    if not args.no_preview:
        print(f"Wrote preview to {PREVIEW_OUT}")
    if issues:
        print("Audit issues:")
        for issue in issues:
            print(f"- {issue}")
        raise SystemExit(1)
    print("Audit passed: no blank frames and every strip has fixed-width frame slots")


if __name__ == "__main__":
    main()
