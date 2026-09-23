from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[2]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import json
from dataclasses import asdict
from pathlib import Path
from shutil import copy2

from PIL import Image, ImageDraw, ImageFont

from tools.assets.shared.universal_cutout import (
    CutoutSettings,
    alpha_components,
    is_background_pixel,
    quality_report,
    sample_background_palette,
    trim_alpha,
    universal_cutout,
)


ROOT = JAVA_ROOT
SOURCE_ROOT = ROOT / "assets" / "source"
TEST_ROOT = ROOT / "temp" / "cutout_tests"
SOURCE_DIR = TEST_ROOT / "source_sheets"
RESULT_DIR = TEST_ROOT / "end_results" / "real_sheets"
REPORT_PATH = RESULT_DIR / "real_sheet_cutout_report.json"
CONTACT_SHEET = RESULT_DIR / "real_sheet_contact_sheet.png"


SHEETS = [
    {
        "label": "village_buildings_lvl2",
        "source": "imagegen-village-buildings-lvl2.png",
        "cols": 6,
        "rows": 3,
        "inner": 0,
        "bleed": 64,
        "settings": CutoutSettings(
            mode="green",
            padding=10,
            global_key=True,
            stray_max_gap=24,
            drop_edge_strays=True,
            drop_above_strays=True,
            drop_below_strays=True,
            drop_small_green_matte=True,
        ),
    },
    {
        "label": "village_buildings_lvl3",
        "source": "imagegen-village-buildings-lvl3.png",
        "cols": 6,
        "rows": 3,
        "inner": 0,
        "bleed": 64,
        "settings": CutoutSettings(
            mode="green",
            padding=10,
            global_key=True,
            stray_max_gap=24,
            drop_edge_strays=True,
            drop_above_strays=True,
            drop_below_strays=True,
            drop_small_green_matte=True,
        ),
    },
    {
        "label": "new_npc_class_models",
        "source": "imagegen-new-npc-class-models.png",
        "cols": 5,
        "rows": 2,
        "inner": 4,
        "bleed": 48,
        "settings": CutoutSettings(
            mode="magenta",
            padding=12,
            stray_max_gap=96,
            drop_edge_strays=True,
            drop_above_strays=True,
            drop_below_strays=True,
        ),
    },
    {
        "label": "mage_directional_model",
        "source": "imagegen-mage-directional-model-sheet.png",
        "cols": 4,
        "rows": 1,
        "inner": 0,
        "bleed": 72,
        "settings": CutoutSettings(
            mode="magenta",
            padding=12,
            stray_max_gap=64,
            drop_edge_strays=True,
            drop_above_strays=True,
            drop_below_strays=True,
        ),
    },
    {
        "label": "resource_props",
        "source": "imagegen-resource-props-sheet.png",
        "cols": 5,
        "rows": 5,
        "inner": 0,
        "bleed_bottom": 48,
        "settings": CutoutSettings(
            mode="magenta",
            padding=10,
            stray_max_gap=48,
            drop_above_strays=True,
            above_stray_gap=16,
            drop_below_strays=True,
            below_stray_gap=16,
        ),
    },
    {
        "label": "goblin_monsters",
        "source": "imagegen-goblin-monster-sheet.png",
        "cols": 4,
        "rows": 2,
        "inner": 0,
        "bleed": 96,
        "settings": CutoutSettings(
            mode="green",
            padding=12,
            global_key=True,
            stray_max_gap=96,
            drop_edge_strays=True,
            drop_above_strays=True,
            drop_below_strays=True,
            drop_small_green_matte=True,
        ),
    },
    {
        "label": "monster_expansion",
        "source": "imagegen-monster-expansion-sheet.png",
        "cols": 4,
        "rows": 4,
        "inner": 0,
        "bleed": 96,
        "settings": CutoutSettings(
            mode="green",
            padding=12,
            global_key=True,
            stray_max_gap=96,
            drop_edge_strays=True,
            drop_above_strays=True,
            drop_below_strays=True,
            drop_small_green_matte=True,
        ),
    },
]


COMPONENT_SHEETS = [
    {
        "label": "village_tilesheet_nosnow",
        "source": "imagegen-village-tilesheet-nosnow.png",
        "settings": CutoutSettings(
            mode="dark",
            padding=6,
            global_key=False,
            clear_strays=False,
            clean_spill=False,
            trim=False,
        ),
        "min_pixels": 80,
        "max_outputs": 140,
    },
]


def crop_cell(
    sheet: Image.Image,
    col: int,
    row: int,
    cols: int,
    rows: int,
    inner: int,
    bleed: int = 0,
    bleed_bottom: int = 0,
) -> Image.Image:
    x1 = round(col * sheet.width / cols) + inner - bleed
    y1 = round(row * sheet.height / rows) + inner - bleed
    x2 = round((col + 1) * sheet.width / cols) - inner + bleed
    y2 = round((row + 1) * sheet.height / rows) - inner + max(bleed, bleed_bottom)
    return sheet.crop((max(0, x1), max(0, y1), min(sheet.width, x2), min(sheet.height, y2)))


def strict_residual_key_pixels(image: Image.Image, mode: str) -> int:
    if mode == "dark":
        return 0
    out = image.convert("RGBA")
    px = out.load()
    count = 0
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a <= 8:
                continue
            if mode == "green" and g > 220 and r < 70 and b < 90:
                count += 1
            elif mode == "magenta" and r > 220 and b > 210 and g < 80:
                count += 1
            elif mode == "dark" and r < 18 and g < 34 and b < 48:
                count += 1
            elif mode == "light" and r > 230 and g > 230 and b > 230 and max(r, g, b) - min(r, g, b) < 14:
                count += 1
    return count


def side_or_top_edge_alpha_pixels(image: Image.Image) -> int:
    out = image.convert("RGBA")
    px = out.load()
    count = 0
    for x in range(out.width):
        if px[x, 0][3] > 8:
            count += 1
    for y in range(1, out.height):
        if px[0, y][3] > 8:
            count += 1
        if px[out.width - 1, y][3] > 8:
            count += 1
    return count


def background_like_alpha_pixels(image: Image.Image, mode: str) -> int:
    out = image.convert("RGBA")
    px = out.load()
    samples = sample_background_palette(out, mode)
    count = 0
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a > 8 and is_background_pixel((r, g, b), mode, samples):
                count += 1
    return count


def checker(size: tuple[int, int]) -> Image.Image:
    image = Image.new("RGBA", size, (36, 40, 48, 255))
    draw = ImageDraw.Draw(image)
    step = 10
    for y in range(0, size[1], step):
        for x in range(0, size[0], step):
            if (x // step + y // step) % 2 == 0:
                draw.rectangle((x, y, x + step - 1, y + step - 1), fill=(56, 60, 70, 255))
    return image


def render_contact_sheet(paths: list[tuple[str, Path]]) -> None:
    cell_w = 172
    cell_h = 170
    label_h = 18
    columns = 6
    rows = (len(paths) + columns - 1) // columns
    sheet = checker((columns * cell_w, rows * (cell_h + label_h)))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("arial.ttf", 10)
    except OSError:
        font = ImageFont.load_default()
    for index, (label, path) in enumerate(paths):
        image = Image.open(path).convert("RGBA")
        scale = min((cell_w - 16) / image.width, (cell_h - 12) / image.height)
        preview = image.resize((max(1, round(image.width * scale)), max(1, round(image.height * scale))), Image.Resampling.NEAREST)
        col = index % columns
        row = index // columns
        x = col * cell_w + (cell_w - preview.width) // 2
        y = row * (cell_h + label_h) + cell_h - preview.height - 4
        sheet.alpha_composite(preview, (x, y))
        draw.text((col * cell_w + 4, row * (cell_h + label_h) + cell_h + 1), label[:28], fill=(238, 238, 230), font=font)
    CONTACT_SHEET.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(CONTACT_SHEET)


def component_output(image: Image.Image, component: list[tuple[int, int]], padding: int) -> Image.Image:
    bbox = (
        max(0, min(x for x, _ in component) - padding),
        max(0, min(y for _, y in component) - padding),
        min(image.width, max(x for x, _ in component) + 1 + padding),
        min(image.height, max(y for _, y in component) + 1 + padding),
    )
    source = image.convert("RGBA")
    src = source.load()
    out = Image.new("RGBA", (bbox[2] - bbox[0], bbox[3] - bbox[1]), (0, 0, 0, 0))
    dst = out.load()
    for x, y in component:
        dst[x - bbox[0], y - bbox[1]] = src[x, y]
    return out


def main() -> None:
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)
    if RESULT_DIR.exists():
        for path in RESULT_DIR.rglob("*"):
            if path.is_file():
                path.unlink()
    RESULT_DIR.mkdir(parents=True, exist_ok=True)
    reports = []
    contact_paths: list[tuple[str, Path]] = []

    for sheet_info in SHEETS:
        label = sheet_info["label"]
        source_path = SOURCE_ROOT / sheet_info["source"]
        if not source_path.exists():
            raise FileNotFoundError(source_path)
        copied_source = SOURCE_DIR / f"real_{source_path.name}"
        copy2(source_path, copied_source)
        source = Image.open(source_path).convert("RGBA")
        out_dir = RESULT_DIR / label
        out_dir.mkdir(parents=True, exist_ok=True)
        cols = int(sheet_info["cols"])
        rows = int(sheet_info["rows"])
        inner = int(sheet_info["inner"])
        bleed = int(sheet_info.get("bleed", 0))
        bleed_bottom = int(sheet_info.get("bleed_bottom", 0))
        settings = sheet_info["settings"]
        assert isinstance(settings, CutoutSettings)
        for index in range(cols * rows):
            col = index % cols
            row = index // cols
            cell = crop_cell(source, col, row, cols, rows, inner, bleed, bleed_bottom)
            full = universal_cutout(cell, CutoutSettings(**(asdict(settings) | {"trim": False})))
            cutout = universal_cutout(cell, settings)
            out_path = out_dir / f"{label}_{index:02d}.png"
            cutout.save(out_path)
            report = quality_report(cutout, settings.mode)
            residual_key = strict_residual_key_pixels(cutout, settings.mode)
            background_like = background_like_alpha_pixels(cutout, settings.mode)
            crop_edge_alpha = side_or_top_edge_alpha_pixels(full)
            warnings = []
            if report.edge_background_pixels:
                warnings.append("edge colors resemble the configured background key")
            if crop_edge_alpha:
                warnings.append("visible pixels touch the source cell side/top edge")
            if background_like > max(16, report.visible_pixels // 300):
                warnings.append("natural colors resemble the sampled background key")
            passed = report.visible_pixels > 0 and residual_key == 0
            reports.append({
                "sheet": label,
                "index": index,
                "path": out_path.as_posix(),
                "copied_source": copied_source.as_posix(),
                **asdict(report),
                "strict_residual_key_pixels": residual_key,
                "background_like_visible_pixels": background_like,
                "full_cell_edge_alpha_pixels": crop_edge_alpha,
                "warnings": warnings,
                "passed": passed,
            })
            contact_paths.append((f"{label}_{index:02d}", out_path))

    for sheet_info in COMPONENT_SHEETS:
        label = sheet_info["label"]
        source_path = SOURCE_ROOT / sheet_info["source"]
        if not source_path.exists():
            raise FileNotFoundError(source_path)
        copied_source = SOURCE_DIR / f"real_{source_path.name}"
        copy2(source_path, copied_source)
        source = Image.open(source_path).convert("RGBA")
        settings = sheet_info["settings"]
        assert isinstance(settings, CutoutSettings)
        keyed = universal_cutout(source, settings)
        components = [
            component
            for component in alpha_components(keyed, threshold=8)
            if len(component) >= int(sheet_info["min_pixels"])
        ]
        components.sort(key=lambda comp: (min(y for _, y in comp), min(x for x, _ in comp)))
        components = components[: int(sheet_info["max_outputs"])]
        out_dir = RESULT_DIR / label
        out_dir.mkdir(parents=True, exist_ok=True)
        for index, component in enumerate(components):
            cutout = component_output(keyed, component, padding=6)
            out_path = out_dir / f"{label}_{index:03d}.png"
            cutout.save(out_path)
            report = quality_report(cutout, settings.mode)
            residual_key = strict_residual_key_pixels(cutout, settings.mode)
            background_like = background_like_alpha_pixels(cutout, settings.mode)
            warnings = []
            if report.edge_background_pixels:
                warnings.append("edge colors resemble the configured background key")
            if background_like > max(16, report.visible_pixels // 300):
                warnings.append("natural colors resemble the sampled background key")
            passed = report.visible_pixels > 0 and residual_key == 0
            reports.append({
                "sheet": label,
                "index": index,
                "path": out_path.as_posix(),
                "copied_source": copied_source.as_posix(),
                **asdict(report),
                "strict_residual_key_pixels": residual_key,
                "background_like_visible_pixels": background_like,
                "full_cell_edge_alpha_pixels": 0,
                "warnings": warnings,
                "passed": passed,
            })
            contact_paths.append((f"{label}_{index:03d}", out_path))

    REPORT_PATH.write_text(json.dumps(reports, indent=2), encoding="utf-8")
    render_contact_sheet(contact_paths)
    failed = [report for report in reports if not report["passed"]]
    print(f"Copied {len(SHEETS) + len(COMPONENT_SHEETS)} real source sheets to {SOURCE_DIR}")
    print(f"Wrote {len(reports)} real-sheet cutouts to {RESULT_DIR}")
    print(f"Wrote contact sheet to {CONTACT_SHEET}")
    print(f"Wrote report to {REPORT_PATH}")
    if failed:
        for report in failed[:12]:
            print(
                "FAILED "
                f"{report['sheet']}[{report['index']}]: "
                f"edge_bg={report['edge_background_pixels']} "
                f"strict_key={report['strict_residual_key_pixels']} "
                f"bg_like={report['background_like_visible_pixels']} "
                f"crop_edge={report['full_cell_edge_alpha_pixels']}"
            )
        raise SystemExit(f"{len(failed)} real-sheet cutout checks failed.")


if __name__ == "__main__":
    main()
