from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from PIL import Image, ImageChops

from asset_paths import animation_dir
from universal_cutout import CutoutSettings, fit, trim_alpha, universal_cutout


ROOT = Path("assets")
SOURCE_OUT = ROOT / "source" / "imagegen_action_strips"


def bounds(length: int, count: int) -> list[tuple[int, int]]:
    return [
        (round(index * length / count), round((index + 1) * length / count))
        for index in range(count)
    ]


def cutout(image: Image.Image) -> Image.Image:
    return universal_cutout(
        image,
        CutoutSettings(
            mode="green",
            padding=4,
            trim=False,
            global_key=True,
            stray_max_gap=36,
            drop_edge_strays=True,
            spill_passes=6,
        ),
    )


def alpha_bbox(image: Image.Image) -> tuple[int, int, int, int] | None:
    return image.getchannel("A").getbbox()


def shared_layout(
    frames: list[Image.Image],
    frame_width: int,
    frame_height: int,
    margin: int,
    anchor_reference: Path | None,
) -> list[Image.Image]:
    trimmed_frames = [trim_alpha(frame.convert("RGBA"), 0) for frame in frames]
    target_anchor_x = frame_width // 2
    target_bottom_y = frame_height - margin
    target_max_width = frame_width - margin * 2
    target_max_height = frame_height - margin * 2

    if anchor_reference is not None:
        reference_canvas = fit(
            Image.open(anchor_reference).convert("RGBA"),
            frame_width,
            frame_height,
            bottom_align=True,
            margin=margin,
        )
        bbox = alpha_bbox(reference_canvas)
        if bbox is not None:
            left, top, right, bottom = bbox
            target_anchor_x = (left + right) // 2
            target_bottom_y = bottom
            target_max_height = bottom - top

    max_source_width = max(frame.width for frame in trimmed_frames)
    max_source_height = max(frame.height for frame in trimmed_frames)
    scale = min(
        target_max_width / max(1, max_source_width),
        target_max_height / max(1, max_source_height),
    )

    laid_out: list[Image.Image] = []
    for frame in trimmed_frames:
        draw_width = max(1, int(round(frame.width * scale)))
        draw_height = max(1, int(round(frame.height * scale)))
        resized = frame.resize((draw_width, draw_height), Image.Resampling.NEAREST)
        canvas = Image.new("RGBA", (frame_width, frame_height), (0, 0, 0, 0))
        draw_x = target_anchor_x - draw_width // 2
        draw_y = target_bottom_y - draw_height
        canvas.alpha_composite(resized, (draw_x, draw_y))
        laid_out.append(canvas)
    return laid_out


def audit_frames(frames: list[Image.Image], label: str) -> None:
    blanks = [index + 1 for index, frame in enumerate(frames) if alpha_bbox(frame) is None]
    if blanks:
        raise ValueError(f"{label}: blank frames detected at {blanks}")
    changed_pairs = 0
    previous: Image.Image | None = None
    for frame in frames:
        if previous is not None and ImageChops.difference(previous, frame).getbbox() is not None:
            changed_pairs += 1
        previous = frame
    if len(frames) > 1 and changed_pairs < max(1, len(frames) // 2):
        raise ValueError(f"{label}: frames do not vary enough to read as animation")


def import_strip(
    assets_root: Path,
    stem: str,
    direction: str,
    action: str,
    source: Path,
    frames: int,
    frame_width: int,
    frame_height: int,
    margin: int,
    anchor_reference: Path | None,
    lock_first: Path | None,
    lock_last: Path | None,
) -> Path:
    SOURCE_OUT.mkdir(parents=True, exist_ok=True)
    copied_source = SOURCE_OUT / f"{stem}_{direction}_{action}_imagegen_source{source.suffix.lower()}"
    shutil.copy2(source, copied_source)

    sheet = Image.open(source).convert("RGBA")
    xs = bounds(sheet.width, frames)
    imported_frames: list[Image.Image] = []
    for left, right in xs:
        cell = sheet.crop((left, 0, right, sheet.height))
        imported_frames.append(cutout(cell))

    imported_frames = shared_layout(imported_frames, frame_width, frame_height, margin, anchor_reference)

    if lock_first is not None:
        imported_frames[0] = fit(Image.open(lock_first).convert("RGBA"), frame_width, frame_height, bottom_align=True, margin=margin)
    if lock_last is not None:
        imported_frames[-1] = fit(Image.open(lock_last).convert("RGBA"), frame_width, frame_height, bottom_align=True, margin=margin)

    audit_frames(imported_frames, f"{stem}_{direction}_{action}")

    strip = Image.new("RGBA", (frame_width * frames, frame_height), (0, 0, 0, 0))
    for index, frame in enumerate(imported_frames):
        strip.alpha_composite(frame, (index * frame_width, 0))

    anim_name = f"{stem}_{direction}_{action}_anim"
    out_dir = animation_dir(assets_root, anim_name)
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / f"{anim_name}.png"
    strip.save(out_path)
    out_path.with_suffix(".frames").write_text(f"{frames}\n", encoding="utf-8")
    return out_path


def main() -> None:
    parser = argparse.ArgumentParser(description="Import a single-row imagegen sprite strip into game animation assets.")
    parser.add_argument("--assets-root", default=ROOT, type=Path)
    parser.add_argument("--stem", required=True)
    parser.add_argument("--direction", required=True)
    parser.add_argument("--action", required=True)
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--frames", required=True, type=int)
    parser.add_argument("--frame-width", type=int, required=True)
    parser.add_argument("--frame-height", type=int, required=True)
    parser.add_argument("--margin", type=int, default=8)
    parser.add_argument("--anchor-reference", type=Path)
    parser.add_argument("--lock-first", type=Path)
    parser.add_argument("--lock-last", type=Path)
    args = parser.parse_args()

    out = import_strip(
        assets_root=args.assets_root,
        stem=args.stem,
        direction=args.direction,
        action=args.action,
        source=args.source,
        frames=args.frames,
        frame_width=args.frame_width,
        frame_height=args.frame_height,
        margin=args.margin,
        anchor_reference=args.anchor_reference,
        lock_first=args.lock_first,
        lock_last=args.lock_last,
    )
    print(f"Imported strip to {out}")


if __name__ == "__main__":
    main()
