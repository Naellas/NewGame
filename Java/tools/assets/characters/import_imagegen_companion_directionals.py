from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import argparse
import shutil
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from tools.assets.shared.asset_paths import COMPANIONS
from tools.assets.shared.universal_cutout import CutoutSettings, fit, universal_cutout


ROOT = Path("assets")
SOURCE_OUT = ROOT / "source" / "imagegen_companion_directionals"
PREVIEW_OUT = SOURCE_OUT / "preview-imported-companion-directionals.png"
SLOT_TO_DIRECTION = ("down", "right", "left", "up")


def alpha_bbox(image: Image.Image) -> tuple[int, int, int, int] | None:
    return image.getchannel("A").getbbox()


def trim(image: Image.Image) -> Image.Image:
    box = alpha_bbox(image)
    return image.crop(box) if box else image


def cutout(image: Image.Image) -> Image.Image:
    return universal_cutout(
        image,
        CutoutSettings(
            mode="magenta",
            padding=4,
            trim=False,
            global_key=True,
            stray_max_gap=28,
            drop_edge_strays=True,
            spill_passes=6,
        ),
    )


def slot_bounds(width: int, slots: int = 4) -> list[tuple[int, int]]:
    return [
        (round(index * width / slots), round((index + 1) * width / slots))
        for index in range(slots)
    ]


def companion_dir(assets_root: Path, name: str) -> Path:
    return assets_root / "characters/companions" / name


def import_sheet(assets_root: Path, name: str, source: Path) -> list[Path]:
    source_out = SOURCE_OUT / f"npc_{name}_directionals_imagegen_source.png"
    SOURCE_OUT.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, source_out)

    sheet = Image.open(source).convert("RGBA")
    target_dir = companion_dir(assets_root, name)
    target_dir.mkdir(parents=True, exist_ok=True)
    outputs: list[Path] = []

    for index, (left, right) in enumerate(slot_bounds(sheet.width)):
        direction = SLOT_TO_DIRECTION[index]
        cell = sheet.crop((left, 0, right, sheet.height))
        sprite = fit(trim(cutout(cell)), 96, 128, bottom_align=True, margin=4)
        out_path = target_dir / f"npc_{name}_model_{direction}.png"
        sprite.save(out_path)
        outputs.append(out_path)

    # Keep the base model in sync with the imagegen-authored front view.
    shutil.copy2(target_dir / f"npc_{name}_model_down.png", target_dir / f"npc_{name}_model.png")
    fit(Image.open(target_dir / f"npc_{name}_model_down.png").convert("RGBA"), 96, 96, bottom_align=False, margin=4).save(
        target_dir / f"npc_{name}.png"
    )
    return outputs


def render_preview(assets_root: Path, names: list[str], out_path: Path) -> None:
    cell_w = 104
    cell_h = 138
    label_h = 15
    columns = 4
    sheet = Image.new("RGBA", (columns * cell_w, len(names) * (cell_h + label_h)), (25, 27, 32, 255))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("arial.ttf", 9)
    except OSError:
        font = ImageFont.load_default()

    for row, name in enumerate(names):
        root = companion_dir(assets_root, name)
        for col, direction in enumerate(("down", "left", "right", "up")):
            path = root / f"npc_{name}_model_{direction}.png"
            image = trim(Image.open(path).convert("RGBA"))
            x = col * cell_w + (cell_w - image.width) // 2
            y = row * (cell_h + label_h) + cell_h - image.height
            sheet.alpha_composite(image, (x, y))
            draw.text(
                (col * cell_w + 4, row * (cell_h + label_h) + cell_h + 1),
                f"{name} {direction}",
                fill=(236, 234, 220),
                font=font,
            )

    out_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out_path)


def parse_sources(values: list[str]) -> dict[str, Path]:
    out: dict[str, Path] = {}
    for value in values:
        if "=" not in value:
            raise SystemExit(f"Expected NAME=PATH source argument, got {value!r}")
        name, path = value.split("=", 1)
        name = name.strip().removeprefix("npc_")
        out[name] = Path(path)
    return out


def main() -> None:
    parser = argparse.ArgumentParser(description="Import imagegen companion 4-direction sheets into model sprites.")
    parser.add_argument("--assets-root", default=ROOT, type=Path)
    parser.add_argument("--source", action="append", default=[], help="Companion source as name=path. Repeat for each companion.")
    parser.add_argument("--no-preview", action="store_true")
    args = parser.parse_args()

    sources = parse_sources(args.source)
    names = [name for name in COMPANIONS if name in sources]
    missing = [name for name in COMPANIONS if name not in sources]
    if missing:
        raise SystemExit(f"Missing imagegen sources for: {', '.join(missing)}")

    outputs: list[Path] = []
    for name in names:
        outputs.extend(import_sheet(args.assets_root, name, sources[name]))

    if not args.no_preview:
        render_preview(args.assets_root, names, PREVIEW_OUT)

    print(f"Imported {len(outputs)} imagegen-authored companion directionals")
    if not args.no_preview:
        print(f"Wrote preview to {PREVIEW_OUT}")


if __name__ == "__main__":
    main()
