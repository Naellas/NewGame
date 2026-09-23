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

from tools.assets.shared.asset_paths import animation_dir
from tools.assets.shared.universal_cutout import CutoutSettings, fit, universal_cutout


ROOT = Path("assets")
SOURCE_OUT = ROOT / "source" / "imagegen_npc_walk_cycles"
PREVIEW_OUT = SOURCE_OUT / "preview-imported-npc-walk-cycles.png"
DIRECTIONS = ("down", "right", "left", "up")
FRAMES = 8
MODEL_W = 96
MODEL_H = 128


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


def write_walk_strip(assets_root: Path, stem: str, direction: str, frames: list[Image.Image]) -> Path:
    strip = Image.new("RGBA", (MODEL_W * len(frames), MODEL_H), (0, 0, 0, 0))
    for index, frame in enumerate(frames):
        strip.alpha_composite(frame, (index * MODEL_W, 0))

    anim_name = f"{stem}_{direction}_walk_anim"
    out_dir = animation_dir(assets_root, anim_name)
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / f"{anim_name}.png"
    save_png(strip, out_path)
    (out_dir / f"{anim_name}.frames").write_text(f"{len(frames)}\n", encoding="utf-8")
    return out_path


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_name(f"{path.stem}.tmp{path.suffix}")
    image.save(temp)
    if path.exists():
        path.unlink()
    temp.replace(path)


def import_sheet(assets_root: Path, stem: str, source: Path) -> list[Path]:
    SOURCE_OUT.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, SOURCE_OUT / f"{stem}_walk_cycle_imagegen_source.png")

    sheet = Image.open(source).convert("RGBA")
    xs = bounds(sheet.width, FRAMES)
    ys = bounds(sheet.height, len(DIRECTIONS))
    npc_dir = assets_root / "characters/npcs/townsfolk"
    outputs: list[Path] = []

    first_down: Image.Image | None = None
    for row, direction in enumerate(DIRECTIONS):
        top, bottom = ys[row]
        frames: list[Image.Image] = []
        for left, right in xs:
            cell = sheet.crop((left, top, right, bottom))
            sprite = fit(trim(cutout(cell)), MODEL_W, MODEL_H, bottom_align=True, margin=4)
            frames.append(sprite)
        save_png(frames[0], npc_dir / f"{stem}_{direction}.png")
        if direction == "down":
            first_down = frames[0]
        outputs.append(write_walk_strip(assets_root, stem, direction, frames))

    if first_down is not None:
        save_png(first_down, npc_dir / f"{stem}.png")
        save_png(fit(first_down, 96, 96, bottom_align=False, margin=4), npc_dir / f"{stem.removesuffix('_model')}.png")
    return outputs


def parse_sources(values: list[str]) -> dict[str, Path]:
    out: dict[str, Path] = {}
    for value in values:
        if "=" not in value:
            raise SystemExit(f"Expected STEM=PATH source argument, got {value!r}")
        stem, path = value.split("=", 1)
        stem = stem.strip()
        if not stem.endswith("_model"):
            stem = f"npc_{stem.removeprefix('npc_')}_model"
        out[stem] = Path(path)
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
        if image.size != (MODEL_W * frames, MODEL_H):
            issues.append(f"{path}: expected {(MODEL_W * frames, MODEL_H)}, got {image.size}")
            continue
        varied = 0
        for index in range(frames):
            frame = image.crop((index * MODEL_W, 0, (index + 1) * MODEL_W, MODEL_H))
            if alpha_bbox(frame) is None:
                issues.append(f"{path}: blank frame {index + 1}")
            if index < frames - 1:
                next_frame = image.crop(((index + 1) * MODEL_W, 0, (index + 2) * MODEL_W, MODEL_H))
                if ImageChops.difference(frame, next_frame).getbbox() is not None:
                    varied += 1
        if varied < 6:
            issues.append(f"{path}: frames do not vary enough")
    return issues


def discover_stems(assets_root: Path) -> list[str]:
    return sorted(path.stem.removesuffix("_down") for path in (assets_root / "characters/npcs/townsfolk").glob("*_model_down.png"))


def main() -> None:
    parser = argparse.ArgumentParser(description="Import imagegen 4x8 regular NPC walk-cycle sheets.")
    parser.add_argument("--assets-root", default=ROOT, type=Path)
    parser.add_argument("--source", action="append", default=[], help="NPC source as stem=path. Repeat for each NPC.")
    parser.add_argument("--no-preview", action="store_true")
    args = parser.parse_args()

    sources = parse_sources(args.source)
    stems = discover_stems(args.assets_root)
    missing = [stem for stem in stems if stem not in sources]
    if missing:
        raise SystemExit(f"Missing imagegen walk-cycle sources for: {', '.join(missing)}")

    outputs: list[Path] = []
    for stem in stems:
        outputs.extend(import_sheet(args.assets_root, stem, sources[stem]))

    issues = audit(outputs)
    if not args.no_preview:
        render_preview(outputs, PREVIEW_OUT)

    print(f"Imported {len(outputs)} imagegen-authored NPC walk strips")
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
