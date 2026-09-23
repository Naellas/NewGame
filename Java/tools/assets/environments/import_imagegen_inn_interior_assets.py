from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import json
from pathlib import Path

from PIL import Image

from tools.assets.shared.universal_cutout import CutoutSettings, quality_report, universal_cutout


ROOT = JAVA_ROOT
SOURCE = ROOT / "assets" / "source" / "imagegen-inn-interior-props-horizontal-sheet.png"
OUT = ROOT / "assets" / "environments/interiors" / "props" / "specialized"

HORIZONTAL = (96, 48)
VERTICAL = (48, 96)

ASSETS = [
    ("interior_carpenter_workbench", HORIZONTAL),
    ("interior_bakery_counter", HORIZONTAL),
    ("interior_tavern_counter", HORIZONTAL),
    ("interior_metal_crate", HORIZONTAL),
    ("interior_long_table_benches", HORIZONTAL),
    ("interior_sawhorse_planks", HORIZONTAL),
    ("interior_storage_counter", HORIZONTAL),
    ("interior_low_cupboard", HORIZONTAL),
    ("interior_bakery_oven", VERTICAL),
    ("interior_resident_bed", VERTICAL),
    ("interior_traveler_trunk", VERTICAL),
    ("interior_herb_drying_rack_v", VERTICAL),
    ("interior_linen_shelf", VERTICAL),
    ("interior_anvil_tool_rack", VERTICAL),
    ("interior_grain_sacks_v", VERTICAL),
    ("interior_inn_screen_chest", VERTICAL),
]


from tools.assets.shared.image_ops import fit_footprint_nearest as fit_to_footprint


def main() -> None:
    if not SOURCE.exists():
        raise FileNotFoundError(f"Missing modular imagegen sheet: {SOURCE}")
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
    report_path = ROOT / "assets" / "source" / "imagegen-inn-interior-props-horizontal-sheet.report.json"
    report_path.write_text(json.dumps(report, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
