from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import argparse
import shutil
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFont

from tools.assets.shared.asset_paths import COMPANIONS, animation_dir
from tools.assets.shared.universal_cutout import CutoutSettings, fit, universal_cutout


ROOT = Path("assets")
SOURCE_OUT = ROOT / "source" / "imagegen_companion_walk_cycles"
PREVIEW_OUT = SOURCE_OUT / "preview-imported-companion-walk-cycles.png"
DIRECTIONS = ("down", "right", "left", "up")
FRAMES = 8


def alpha_bbox(image: Image.Image) -> tuple[int, int, int, int] | None:
    return image.getchannel("A").getbbox()


def trim(image: Image.Image) -> Image.Image:
    box = alpha_bbox(image)
    return image.crop(box) if box else image


from tools.assets.shared.image_ops import cutout


def bounds(length: int, count: int) -> list[tuple[int, int]]:
    return [
        (round(index * length / count), round((index + 1) * length / count))
        for index in range(count)
    ]


def write_walk_strip(assets_root: Path, name: str, direction: str, frames: list[Image.Image]) -> Path:
    frame_w, frame_h = 96, 128
    strip = Image.new("RGBA", (frame_w * len(frames), frame_h), (0, 0, 0, 0))
    for index, frame in enumerate(frames):
        strip.alpha_composite(frame, (index * frame_w, 0))

    anim_name = f"npc_{name}_model_{direction}_walk_anim"
    out_dir = animation_dir(assets_root, anim_name)
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / f"{anim_name}.png"
    strip.save(out_path)
    (out_dir / f"{anim_name}.frames").write_text(f"{len(frames)}\n", encoding="utf-8")
    return out_path


def import_sheet(assets_root: Path, name: str, source: Path) -> list[Path]:
    SOURCE_OUT.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, SOURCE_OUT / f"npc_{name}_walk_cycle_imagegen_source.png")

    sheet = Image.open(source).convert("RGBA")
    xs = bounds(sheet.width, FRAMES)
    ys = bounds(sheet.height, len(DIRECTIONS))
    outputs: list[Path] = []

    for row, direction in enumerate(DIRECTIONS):
        frames: list[Image.Image] = []
        top, bottom = ys[row]
        for left, right in xs:
            cell = sheet.crop((left, top, right, bottom))
            sprite = fit(trim(cutout(cell)), 96, 128, bottom_align=True, margin=4)
            frames.append(sprite)
        outputs.append(write_walk_strip(assets_root, name, direction, frames))
    return outputs


def parse_sources(values: list[str]) -> dict[str, Path]:
    out: dict[str, Path] = {}
    for value in values:
        if "=" not in value:
            raise SystemExit(f"Expected NAME=PATH source argument, got {value!r}")
        name, path = value.split("=", 1)
        out[name.strip().removeprefix("npc_")] = Path(path)
    return out


def preview_frame(path: Path, frame: int) -> Image.Image:
    sheet = Image.open(path).convert("RGBA")
    frames = int(path.with_suffix(".frames").read_text(encoding="utf-8").strip())
    frame_w = sheet.width // frames
    return sheet.crop((frame * frame_w, 0, (frame + 1) * frame_w, sheet.height))


def render_preview(paths: list[Path], out_path: Path) -> None:
    from tools.assets.shared import image_ops
    return image_ops.render_preview(paths, out_path, trim=trim, preview_frame=preview_frame)


def audit(paths: list[Path]) -> list[str]:
    issues: list[str] = []
    for path in paths:
        image = Image.open(path).convert("RGBA")
        frames = int(path.with_suffix(".frames").read_text(encoding="utf-8").strip())
        if frames != FRAMES:
            issues.append(f"{path}: expected {FRAMES} frames, got {frames}")
        if image.size != (96 * frames, 128):
            issues.append(f"{path}: expected {(96 * frames, 128)}, got {image.size}")
            continue
        previous: Image.Image | None = None
        changed_pairs = 0
        for index in range(frames):
            frame = image.crop((index * 96, 0, (index + 1) * 96, 128))
            if alpha_bbox(frame) is None:
                issues.append(f"{path}: blank frame {index + 1}")
            if previous is not None and ImageChops.difference(previous, frame).getbbox() is not None:
                changed_pairs += 1
            previous = frame
        if changed_pairs < frames // 2:
            issues.append(f"{path}: frames do not vary enough")
    return issues


def main() -> None:
    parser = argparse.ArgumentParser(description="Import imagegen 4x8 companion walk-cycle sheets.")
    parser.add_argument("--assets-root", default=ROOT, type=Path)
    parser.add_argument("--source", action="append", default=[], help="Companion source as name=path. Repeat for each companion.")
    parser.add_argument("--no-preview", action="store_true")
    args = parser.parse_args()

    sources = parse_sources(args.source)
    missing = [name for name in COMPANIONS if name not in sources]
    if missing:
        raise SystemExit(f"Missing imagegen walk-cycle sources for: {', '.join(missing)}")

    outputs: list[Path] = []
    for name in COMPANIONS:
        outputs.extend(import_sheet(args.assets_root, name, sources[name]))

    issues = audit(outputs)
    if not args.no_preview:
        render_preview(outputs, PREVIEW_OUT)

    print(f"Imported {len(outputs)} imagegen-authored companion walk strips")
    if not args.no_preview:
        print(f"Wrote preview to {PREVIEW_OUT}")
    if issues:
        print("Audit issues:")
        for issue in issues:
            print(f"- {issue}")
        raise SystemExit(1)
    print("Audit passed: no blank frames, fixed 96x128 frame slots, and frames vary")


if __name__ == "__main__":
    main()
