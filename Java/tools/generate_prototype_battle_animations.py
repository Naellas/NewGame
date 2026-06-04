from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from PIL import Image

from asset_paths import animation_dir, find_asset


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "assets"
FRAMES = 6


@dataclass(frozen=True)
class Part:
    name: str
    rect: tuple[float, float, float, float]
    pivot: tuple[float, float]
    angles: tuple[float, ...]
    offsets: tuple[tuple[int, int], ...] = ((0, 0),) * FRAMES


@dataclass(frozen=True)
class StripSpec:
    source: str
    output: str
    parts: tuple[Part, ...]
    body_offsets: tuple[tuple[int, int], ...] = ((0, 0),) * FRAMES
    body_scales: tuple[tuple[float, float], ...] = ((1.0, 1.0),) * FRAMES
    erase_parts: bool = False


def main() -> None:
    specs = [
        knight_attack(),
        mage_cast(),
        ranger_shoot(),
        cleric_cast(),
        rogue_attack(),
        skeleton_attack(),
        spider_attack(),
    ]
    for spec in specs:
        output = write_strip(spec)
        print(f"Wrote {output}")


def knight_attack() -> StripSpec:
    return StripSpec(
        source="class_knight_model.png",
        output="class_knight_model_attack_anim.png",
        body_offsets=((0, 0), (-4, 1), (-8, 2), (14, -2), (8, 0), (0, 0)),
        body_scales=((1.0, 1.0), (1.02, 0.99), (1.03, 0.98), (1.04, 0.98), (1.01, 1.0), (1.0, 1.0)),
        parts=(
            Part("weapon_arm", (0.12, 0.20, 0.44, 0.74), (0.32, 0.30), (-26, -42, -60, 30, 16, 0)),
            Part("shield_arm", (0.52, 0.22, 0.86, 0.66), (0.62, 0.31), (8, 16, 20, -10, -4, 0)),
        ),
    )


def mage_cast() -> StripSpec:
    return StripSpec(
        source="class_mage_model.png",
        output="class_mage_model_cast_anim.png",
        body_offsets=((0, 0), (0, -3), (1, -7), (-1, -9), (0, -5), (0, 0)),
        body_scales=((1.0, 1.0), (0.99, 1.02), (0.98, 1.04), (0.99, 1.03), (1.0, 1.01), (1.0, 1.0)),
        parts=(
            Part("staff_arm", (0.48, 0.18, 0.86, 0.72), (0.58, 0.28), (-8, -22, -38, -28, -12, 0)),
            Part("off_hand", (0.12, 0.22, 0.43, 0.62), (0.34, 0.31), (10, 25, 38, 22, 8, 0)),
        ),
    )


def ranger_shoot() -> StripSpec:
    return StripSpec(
        source="class_ranger_model.png",
        output="class_ranger_model_shoot_anim.png",
        body_offsets=((0, 0), (-4, 0), (-9, 1), (-12, 1), (8, -1), (0, 0)),
        body_scales=((1.0, 1.0), (0.99, 1.01), (0.98, 1.02), (0.98, 1.02), (1.02, 0.99), (1.0, 1.0)),
        parts=(
            Part("bow_arm", (0.48, 0.20, 0.88, 0.72), (0.58, 0.30), (-8, -18, -28, -30, -10, 0)),
            Part("draw_arm", (0.10, 0.20, 0.48, 0.68), (0.36, 0.30), (4, 18, 32, 36, -8, 0)),
        ),
    )


def cleric_cast() -> StripSpec:
    return StripSpec(
        source="class_cleric_model.png",
        output="class_cleric_model_cast_anim.png",
        body_offsets=((0, 0), (0, -2), (0, -5), (0, -7), (0, -3), (0, 0)),
        body_scales=((1.0, 1.0), (0.99, 1.02), (0.99, 1.03), (1.0, 1.03), (1.0, 1.01), (1.0, 1.0)),
        parts=(
            Part("blessing_hand", (0.12, 0.20, 0.48, 0.66), (0.34, 0.30), (8, 22, 36, 34, 16, 0)),
            Part("ward_hand", (0.50, 0.22, 0.86, 0.68), (0.60, 0.31), (-8, -20, -34, -28, -12, 0)),
        ),
    )


def rogue_attack() -> StripSpec:
    return StripSpec(
        source="class_rogue_model.png",
        output="class_rogue_model_attack_anim.png",
        body_offsets=((0, 0), (-5, 1), (-10, 1), (18, -2), (7, 0), (0, 0)),
        body_scales=((1.0, 1.0), (1.02, 0.99), (1.04, 0.98), (1.04, 0.98), (1.01, 1.0), (1.0, 1.0)),
        parts=(
            Part("knife_arm", (0.50, 0.18, 0.88, 0.70), (0.60, 0.29), (-12, -34, -50, 34, 14, 0)),
            Part("balance_arm", (0.10, 0.22, 0.44, 0.66), (0.34, 0.32), (10, 18, 24, -16, -6, 0)),
        ),
    )


def skeleton_attack() -> StripSpec:
    return StripSpec(
        source="skeleton.png",
        output="skeleton_attack_anim.png",
        body_offsets=((0, 0), (-3, 1), (-8, 2), (18, -1), (7, 0), (0, 0)),
        body_scales=((1.0, 1.0), (1.02, 0.99), (1.03, 0.98), (1.04, 0.98), (1.01, 1.0), (1.0, 1.0)),
        parts=(
            Part("sword_arm", (0.08, 0.10, 0.46, 0.72), (0.32, 0.26), (-30, -54, -70, 36, 16, 0)),
            Part("shield_arm", (0.52, 0.18, 0.92, 0.70), (0.62, 0.30), (6, 12, 18, -10, -4, 0)),
        ),
    )


def spider_attack() -> StripSpec:
    return StripSpec(
        source="spider.png",
        output="spider_attack_anim.png",
        body_offsets=((0, 0), (-3, 0), (-6, 1), (10, -1), (4, 0), (0, 0)),
        body_scales=((1.0, 1.0), (1.02, 0.99), (1.04, 0.98), (1.02, 0.99), (1.0, 1.0), (1.0, 1.0)),
        parts=(
            Part("front_legs", (0.05, 0.40, 0.38, 0.96), (0.34, 0.58), (18, 32, 44, -24, -10, 0), ((0, 0), (-2, 0), (-4, 0), (8, -1), (3, 0), (0, 0))),
            Part("rear_legs", (0.62, 0.36, 0.98, 0.96), (0.66, 0.58), (-16, -28, -38, 22, 10, 0), ((0, 0), (2, 0), (4, 0), (-6, -1), (-2, 0), (0, 0))),
        ),
    )


def write_strip(spec: StripSpec) -> Path:
    source = Image.open(asset_source(spec.source)).convert("RGBA")
    frames = [render_frame(source, spec, frame) for frame in range(FRAMES)]
    sheet = Image.new("RGBA", (source.width * FRAMES, source.height), (0, 0, 0, 0))
    for i, frame in enumerate(frames):
        sheet.alpha_composite(frame, (i * source.width, 0))
    out_dir = animation_dir(ASSETS, Path(spec.output).stem)
    out_dir.mkdir(parents=True, exist_ok=True)
    output = out_dir / spec.output
    sheet.save(output)
    return output


def asset_source(relative_path: str) -> Path:
    path = ASSETS / relative_path
    if path.exists():
        return path
    return find_asset(ASSETS, Path(relative_path).name)


def render_frame(source: Image.Image, spec: StripSpec, frame: int) -> Image.Image:
    base = source.copy()
    parts = []
    for part in spec.parts:
        box = scaled_rect(source.size, part.rect)
        parts.append((part, box, source.crop(box)))
        if spec.erase_parts:
            erase_rect(base, box)

    scaled = scale_body(base, spec.body_scales[frame])
    canvas = Image.new("RGBA", source.size, (0, 0, 0, 0))
    body_offset = spec.body_offsets[frame]
    canvas.alpha_composite(scaled, centered_offset(source.size, scaled.size, body_offset))

    for part, box, part_image in parts:
        layer = Image.new("RGBA", source.size, (0, 0, 0, 0))
        offset = part.offsets[frame]
        layer.alpha_composite(part_image, (box[0] + offset[0], box[1] + offset[1]))
        pivot = (int(source.width * part.pivot[0]) + offset[0], int(source.height * part.pivot[1]) + offset[1])
        rotated = layer.rotate(part.angles[frame], resample=Image.Resampling.BICUBIC, center=pivot)
        canvas.alpha_composite(rotated)
    return canvas


def scale_body(image: Image.Image, scale: tuple[float, float]) -> Image.Image:
    width = max(1, round(image.width * scale[0]))
    height = max(1, round(image.height * scale[1]))
    return image.resize((width, height), Image.Resampling.BICUBIC)


def centered_offset(canvas_size: tuple[int, int], image_size: tuple[int, int], offset: tuple[int, int]) -> tuple[int, int]:
    return (
        (canvas_size[0] - image_size[0]) // 2 + offset[0],
        canvas_size[1] - image_size[1] + offset[1],
    )


def scaled_rect(size: tuple[int, int], rect: tuple[float, float, float, float]) -> tuple[int, int, int, int]:
    width, height = size
    x1 = max(0, min(width - 1, round(width * rect[0])))
    y1 = max(0, min(height - 1, round(height * rect[1])))
    x2 = max(x1 + 1, min(width, round(width * rect[2])))
    y2 = max(y1 + 1, min(height, round(height * rect[3])))
    return x1, y1, x2, y2


def erase_rect(image: Image.Image, box: tuple[int, int, int, int]) -> None:
    clear = Image.new("RGBA", (box[2] - box[0], box[3] - box[1]), (0, 0, 0, 0))
    image.paste(clear, box)


if __name__ == "__main__":
    main()
