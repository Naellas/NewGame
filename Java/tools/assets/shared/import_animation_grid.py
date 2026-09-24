"""Import a transparent animation grid as a consistently scaled, bottom-aligned strip."""
from __future__ import annotations

import argparse
from pathlib import Path
from PIL import Image
from tools.project_paths import JAVA_ROOT
from tools.assets.shared.asset_paths import asset_file


def assemble(source: Image.Image, columns: int, rows: int, width: int, height: int, alpha_threshold: int = 2) -> Image.Image:
    if min(columns, rows, width, height) < 1 or min(width, height) < 5:
        raise ValueError("Positive grid counts and frame dimensions >= 5 required")
    source = source.convert("RGBA")
    cells = []
    for row in range(rows):
        for col in range(columns):
            cell = source.crop((col * source.width // columns, row * source.height // rows,
                                (col + 1) * source.width // columns, (row + 1) * source.height // rows))
            bounds = cell.getchannel("A").point(lambda a: 255 if a > alpha_threshold else 0).getbbox()
            if bounds is None:
                raise ValueError(f"Empty frame {len(cells)}")
            cells.append(cell.crop(bounds))
    # One scale for the entire cycle preserves relative growth and foam dissipation.
    scale = min((width - 4) / max(c.width for c in cells), (height - 4) / max(c.height for c in cells))
    strip = Image.new("RGBA", (width * len(cells), height))
    for index, cell in enumerate(cells):
        cell = cell.resize((max(1, round(cell.width * scale)), max(1, round(cell.height * scale))), Image.Resampling.LANCZOS)
        strip.alpha_composite(cell, (index * width + (width - cell.width) // 2, height - 2 - cell.height))
    return strip


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("output", help="Asset-relative PNG path (overwritten)")
    parser.add_argument("--columns", type=int, required=True)
    parser.add_argument("--rows", type=int, required=True)
    parser.add_argument("--width", type=int, default=192)
    parser.add_argument("--height", type=int, default=64)
    parser.add_argument("--alpha-threshold", type=int, default=2, help="Ignore near-invisible noise when locating content; retained pixels keep alpha")
    args = parser.parse_args()
    output = asset_file(JAVA_ROOT / "assets", args.output)
    with Image.open(args.source) as source:
        strip = assemble(source, args.columns, args.rows, args.width, args.height, args.alpha_threshold)
    output.parent.mkdir(parents=True, exist_ok=True)
    strip.save(output)
    output.with_suffix(".frames").write_text(str(args.columns * args.rows), encoding="utf-8")
    output.with_suffix(".framebounds").write_text(f"0 0 {args.width} {args.height}", encoding="utf-8")
    print(output)


if __name__ == "__main__":
    main()
