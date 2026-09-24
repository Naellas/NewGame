from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

from universal_cutout import CutoutSettings, quality_report, universal_cutout


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets" / "source" / "imagegen-interior-modular-seating-sheet.png"
OUT = ROOT / "assets" / "interiors" / "props" / "modular_seating"

TILE = (48, 48)
HORIZONTAL = (96, 48)
VERTICAL = (48, 96)

ASSETS = [
    ("interior_table_h_left", HORIZONTAL),
    ("interior_table_h_middle", HORIZONTAL),
    ("interior_table_h_right", HORIZONTAL),
    ("interior_bench_h", HORIZONTAL),
    ("interior_banquet_table_h", HORIZONTAL),
    ("interior_stool_table_h", HORIZONTAL),
    ("interior_study_desk_h", HORIZONTAL),
    ("interior_counter_corner_h", HORIZONTAL),
    ("interior_table_v_top", VERTICAL),
    ("interior_table_v_middle", VERTICAL),
    ("interior_table_v_bottom", VERTICAL),
    ("interior_bench_v", VERTICAL),
    ("interior_chair_north_alt", TILE),
    ("interior_chair_south_alt", TILE),
    ("interior_chair_east_alt", TILE),
    ("interior_chair_west_alt", TILE),
]


def fit_to_footprint(image: Image.Image, width: int, height: int) -> Image.Image:
    bbox = image.getchannel("A").getbbox()
    if bbox is None:
        return Image.new("RGBA", (width, height), (0, 0, 0, 0))
    trimmed = image.crop((
        max(0, bbox[0] - 3),
        max(0, bbox[1] - 3),
        min(image.width, bbox[2] + 3),
        min(image.height, bbox[3] + 3),
    ))
    margin = 4
    max_w = width - margin * 2
    max_h = height - margin * 2
    scale = min(max_w / trimmed.width, max_h / trimmed.height)
    draw_w = max(1, int(round(trimmed.width * scale)))
    draw_h = max(1, int(round(trimmed.height * scale)))
    resized = trimmed.resize((draw_w, draw_h), Image.Resampling.NEAREST)
    canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    canvas.alpha_composite(resized, ((width - draw_w) // 2, height - draw_h - margin))
    return canvas


def main() -> None:
    if not SOURCE.exists():
        raise FileNotFoundError(f"Missing modular seating imagegen sheet: {SOURCE}")
    OUT.mkdir(parents=True, exist_ok=True)
    source = Image.open(SOURCE).convert("RGBA")
    settings = CutoutSettings(
        mode="green",
        padding=4,
        square=False,
        connected=True,
        global_key=True,
        drop_edge_strays=True,
        edge_stray_margin=2,
        drop_small_green_matte=True,
        small_green_matte_max_pixels=96,
    )
    x_edges = [round(source.width * i / 4) for i in range(5)]
    y_edges = [round(source.height * i / 4) for i in range(5)]
    report = []
    for index, (name, footprint) in enumerate(ASSETS):
        col = index % 4
        row = index // 4
        cell = source.crop((x_edges[col], y_edges[row], x_edges[col + 1], y_edges[row + 1]))
        cutout = universal_cutout(cell, settings)
        fitted = fit_to_footprint(cutout, footprint[0], footprint[1])
        out_path = OUT / f"{name}.png"
        fitted.save(out_path)
        result = quality_report(fitted, "green")
        report.append({
            "name": name,
            "path": out_path.relative_to(ROOT).as_posix(),
            "footprint": footprint,
            "cell": [col, row],
            "passed": result.passed,
            "output_size": result.output_size,
            "visible_pixels": result.visible_pixels,
            "edge_background_pixels": result.edge_background_pixels,
            "components": result.components,
        })
        if not result.passed:
            raise RuntimeError(f"Cutout quality check failed for {name}: {result}")
    report_path = ROOT / "assets" / "source" / "imagegen-interior-modular-seating-sheet.report.json"
    report_path.write_text(json.dumps(report, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
