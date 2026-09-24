from __future__ import annotations

import argparse
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from asset_paths import animation_dir


ROOT = Path("assets")
DIRECTIONS = ("down_left", "down_right", "up_left", "up_right", "down", "left", "right", "up")
START_FRAMES = 6
STOP_FRAMES = 6
IDLE_FRAMES = 8
PREVIEW_OUT = ROOT / "source" / "movement_states" / "preview-character-movement-states.png"


def alpha_bbox(image: Image.Image) -> tuple[int, int, int, int] | None:
    return image.getchannel("A").getbbox()


def clamp(value: int, low: int, high: int) -> int:
    return max(low, min(high, value))


def clip_blit(out: Image.Image, source: Image.Image, dx: int, dy: int) -> None:
    src_left = max(0, -dx)
    src_top = max(0, -dy)
    src_right = min(source.width, out.width - dx)
    src_bottom = min(source.height, out.height - dy)
    if src_right <= src_left or src_bottom <= src_top:
        return
    piece = source.crop((src_left, src_top, src_right, src_bottom))
    out.alpha_composite(piece, (max(0, dx), max(0, dy)))


def band(image: Image.Image, top: int, bottom: int) -> Image.Image:
    out = Image.new("RGBA", image.size, (0, 0, 0, 0))
    top = clamp(top, 0, image.height)
    bottom = clamp(bottom, top, image.height)
    if bottom > top:
        out.alpha_composite(image.crop((0, top, image.width, bottom)), (0, top))
    return out


def horizontal_sign(direction: str) -> int:
    if "left" in direction:
        return -1
    if "right" in direction:
        return 1
    return 0


def subtle_pose(source: Image.Image, direction: str, frame: int, total_frames: int, intensity: float) -> Image.Image:
    image = source.convert("RGBA")
    box = alpha_bbox(image)
    if box is None:
        return Image.new("RGBA", image.size, (0, 0, 0, 0))

    left, top, right, bottom = box
    height = bottom - top
    shoulder = top + round(height * 0.36)
    hip = top + round(height * 0.62)
    knee = top + round(height * 0.80)

    phase = (frame / max(1, total_frames)) * math.tau
    sway = math.sin(phase)
    breath = math.sin(phase * 2.0)
    side = horizontal_sign(direction)

    if side != 0:
        upper_dx = round(side * sway * intensity)
        torso_dx = round(side * sway * intensity * 0.45)
        lower_dx = round(-side * sway * intensity * 0.35)
    else:
        upper_dx = round(-sway * intensity * 0.8)
        torso_dx = 0
        lower_dx = round(sway * intensity * 0.55)
    dy = -1 if breath > 0.48 else 0

    x_low = -left
    x_high = image.width - right
    y_low = -top
    y_high = image.height - bottom
    upper_dx = clamp(upper_dx, x_low, x_high)
    torso_dx = clamp(torso_dx, x_low, x_high)
    lower_dx = clamp(lower_dx, x_low, x_high)
    dy = clamp(dy, y_low, y_high)

    out = Image.new("RGBA", image.size, (0, 0, 0, 0))
    clip_blit(out, band(image, 0, shoulder + 2), upper_dx, dy)
    clip_blit(out, band(image, shoulder - 2, knee + 2), torso_dx, dy)
    clip_blit(out, band(image, hip - 2, image.height), lower_dx, 0)
    return out


def parse_directional_stem(stem: str) -> tuple[str, str] | None:
    if stem.endswith("_anim"):
        return None
    for direction in DIRECTIONS:
        suffix = "_" + direction
        if stem.endswith(suffix):
            return stem.removesuffix(suffix), direction
    return None


def read_frame_count(path: Path, frame_width_hint: int) -> int:
    meta = path.with_suffix(".frames")
    if meta.exists():
        text = meta.read_text(encoding="utf-8").strip()
        if text.isdigit():
            return max(1, int(text))
    sheet = Image.open(path).convert("RGBA")
    if frame_width_hint > 0 and sheet.width % frame_width_hint == 0:
        return max(1, sheet.width // frame_width_hint)
    return max(1, sheet.width // max(1, sheet.height))


def read_walk_frames(source_path: Path, walk_path: Path) -> list[Image.Image]:
    source = Image.open(source_path).convert("RGBA")
    sheet = Image.open(walk_path).convert("RGBA")
    frame_count = read_frame_count(walk_path, source.width)
    frame_width = max(1, sheet.width // max(1, frame_count))
    frames: list[Image.Image] = []
    for index in range(frame_count):
        x = index * frame_width
        frame = sheet.crop((x, 0, min(sheet.width, x + frame_width), sheet.height))
        if frame.width != source.width or frame.height != source.height:
            frame = frame.resize((source.width, source.height), Image.Resampling.NEAREST)
        frames.append(frame)
    return frames


def sample_walk_indices(frame_count: int, samples: int, start: bool) -> list[int]:
    if frame_count <= 0:
        return [0] * samples
    if frame_count <= samples:
        indices = list(range(frame_count))
        if not start:
            indices.reverse()
        while len(indices) < samples:
            indices.append(indices[-1])
        return indices
    if start:
        low = 0
        high = max(0, frame_count // 2)
    else:
        low = max(0, frame_count // 2 - 1)
        high = frame_count - 1
    if samples == 1:
        return [round((low + high) / 2)]
    return [round(low + (high - low) * index / (samples - 1)) for index in range(samples)]


def compose_strip(frames: list[Image.Image]) -> Image.Image:
    width = frames[0].width
    height = frames[0].height
    strip = Image.new("RGBA", (width * len(frames), height), (0, 0, 0, 0))
    for index, frame in enumerate(frames):
        strip.alpha_composite(frame, (index * width, 0))
    return strip


def write_strip(assets_root: Path, stem: str, direction: str, action: str, frames: list[Image.Image]) -> Path:
    out_dir = animation_dir(assets_root, stem)
    out_dir.mkdir(parents=True, exist_ok=True)
    name = f"{stem}_{direction}_{action}_anim"
    out_path = out_dir / f"{name}.png"
    compose_strip(frames).save(out_path)
    out_path.with_suffix(".frames").write_text(f"{len(frames)}\n", encoding="utf-8")
    return out_path


def build_transition_frames(source: Image.Image, walk_frames: list[Image.Image], direction: str, start: bool) -> list[Image.Image]:
    sample_count = 4
    walk_indices = sample_walk_indices(len(walk_frames), sample_count, start=start)
    settle = [subtle_pose(source, direction, 0 if start else 2, 4, 1.0), subtle_pose(source, direction, 1 if start else 3, 4, 1.0)]
    walk_selection = [walk_frames[index] for index in walk_indices]
    return settle + walk_selection if start else walk_selection + settle


def build_idle_frames(source: Image.Image, direction: str) -> list[Image.Image]:
    return [subtle_pose(source, direction, index, IDLE_FRAMES, 0.9) for index in range(IDLE_FRAMES)]


def discover_models(assets_root: Path) -> list[tuple[str, dict[str, Path]]]:
    roots = [
        assets_root / "player" / "base",
        assets_root / "player" / "classes",
        assets_root / "companions",
    ]
    discovered: dict[str, dict[str, Path]] = {}
    for root in roots:
        if not root.exists():
            continue
        for path in root.rglob("*.png"):
            parsed = parse_directional_stem(path.stem)
            if parsed is None:
                continue
            stem, direction = parsed
            discovered.setdefault(stem, {})[direction] = path
    return sorted((stem, directions) for stem, directions in discovered.items() if directions)


def render_preview(paths: list[Path], out_path: Path) -> None:
    if not paths:
        return
    cell_w = 112
    cell_h = 116
    label_h = 18
    columns = 6
    rows = math.ceil(len(paths) / columns)
    sheet = Image.new("RGBA", (columns * cell_w, rows * (cell_h + label_h)), (28, 30, 36, 255))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("arial.ttf", 10)
    except OSError:
        font = ImageFont.load_default()

    for index, path in enumerate(paths):
        frame_count = read_frame_count(path, 0)
        image = Image.open(path).convert("RGBA")
        frame_w = max(1, image.width // max(1, frame_count))
        frame = image.crop((0, 0, frame_w, image.height))
        box = alpha_bbox(frame)
        if box is not None:
            frame = frame.crop(box)
        scale = min((cell_w - 12) / max(1, frame.width), (cell_h - 12) / max(1, frame.height), 1.0)
        frame = frame.resize((max(1, round(frame.width * scale)), max(1, round(frame.height * scale))), Image.Resampling.NEAREST)
        col = index % columns
        row = index // columns
        x = col * cell_w + (cell_w - frame.width) // 2
        y = row * (cell_h + label_h) + cell_h - frame.height
        sheet.alpha_composite(frame, (x, y))
        draw.text((col * cell_w + 4, row * (cell_h + label_h) + cell_h + 2), path.stem[:22], fill=(236, 234, 220), font=font)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out_path)


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate directional start, stop, and idle strips for player and companion movement.")
    parser.add_argument("--assets-root", default=ROOT, type=Path)
    parser.add_argument("--no-preview", action="store_true")
    args = parser.parse_args()

    outputs: list[Path] = []
    for stem, directions in discover_models(args.assets_root):
        for direction, source_path in sorted(directions.items()):
            walk_path = animation_dir(args.assets_root, stem) / f"{stem}_{direction}_walk_anim.png"
            if not walk_path.exists():
                continue
            source = Image.open(source_path).convert("RGBA")
            walk_frames = read_walk_frames(source_path, walk_path)
            outputs.append(write_strip(args.assets_root, stem, direction, "start_walk",
                                       build_transition_frames(source, walk_frames, direction, start=True)))
            outputs.append(write_strip(args.assets_root, stem, direction, "stop_walk",
                                       build_transition_frames(source, walk_frames, direction, start=False)))
            outputs.append(write_strip(args.assets_root, stem, direction, "idle",
                                       build_idle_frames(source, direction)))

    preview_out = args.assets_root / "source" / "movement_states" / PREVIEW_OUT.name
    if not args.no_preview:
        render_preview(outputs, preview_out)

    print(f"Wrote {len(outputs)} movement-state strips")
    if not args.no_preview:
        print(f"Wrote preview to {preview_out}")


if __name__ == "__main__":
    main()
