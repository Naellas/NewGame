from __future__ import annotations

import json
from dataclasses import asdict
from pathlib import Path

from PIL import Image, ImageDraw

from universal_cutout import CutoutSettings, quality_report, universal_cutout


ROOT = Path(__file__).resolve().parents[1]
TEST_ROOT = ROOT / "assets" / "cutout_tests"
SOURCE_DIR = TEST_ROOT / "source_sheets"
RESULT_DIR = TEST_ROOT / "end_results"
SOURCE_SHEET = SOURCE_DIR / "universal_cutout_test_sheet.png"
REPORT_PATH = RESULT_DIR / "universal_cutout_report.json"
CELL = 128


CASES = [
    ("magenta", (255, 0, 255, 255), (34, 25, 98, 111)),
    ("green", (0, 235, 28, 255), (28, 18, 102, 108)),
    ("dark", (8, 18, 30, 255), (26, 26, 104, 104)),
    ("light", (235, 236, 235, 255), (24, 24, 105, 106)),
    ("paper", (229, 217, 194, 255), (23, 23, 104, 109)),
]


def draw_case(draw: ImageDraw.ImageDraw, index: int, bg: tuple[int, int, int, int]) -> None:
    ox = index * CELL
    draw.rectangle((ox, 0, ox + CELL - 1, CELL - 1), fill=bg)
    if index == 4:
        for y in range(0, CELL, 7):
            color = (225 + (y % 3), 213 + (y % 5), 190 + (y % 4), 255)
            draw.line((ox, y, ox + CELL, y), fill=color)
    # A keyed halo exercises bleed cleanup without changing the expected object bounds.
    halo = bg
    draw.ellipse((ox + 27, 21, ox + 99, 113), fill=halo)
    draw.ellipse((ox + 34, 28, ox + 92, 106), fill=(82, 99, 139, 255))
    draw.rectangle((ox + 46, 76, ox + 82, 111), fill=(122, 82, 58, 255))
    draw.polygon(
        [(ox + 64, 14), (ox + 78, 35), (ox + 50, 35)],
        fill=(218, 184, 83, 255),
    )
    draw.rectangle((ox + 94, 54, ox + 104, 58), fill=(196, 205, 211, 255))
    draw.rectangle((ox + 101, 49, ox + 106, 63), fill=(196, 205, 211, 255))
    draw.rectangle((ox + 22, 86, ox + 31, 97), fill=(70, 132, 91, 255))
    draw.point((ox + 6, 7), fill=(190, 40, 210, 255))
    draw.point((ox + 119, 118), fill=(190, 40, 210, 255))


def build_source_sheet() -> Image.Image:
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)
    sheet = Image.new("RGBA", (CELL * len(CASES), CELL), (0, 0, 0, 0))
    draw = ImageDraw.Draw(sheet)
    for index, (_, bg, _) in enumerate(CASES):
        draw_case(draw, index, bg)
    sheet.save(SOURCE_SHEET)
    return sheet


def assert_contains(actual: tuple[int, int, int, int] | None, expected: tuple[int, int, int, int], label: str) -> None:
    if actual is None:
        raise AssertionError(f"{label}: no visible pixels after cutout")
    left, top, right, bottom = actual
    exp_left, exp_top, exp_right, exp_bottom = expected
    if left > exp_left or top > exp_top or right < exp_right or bottom < exp_bottom:
        raise AssertionError(f"{label}: cutout bbox {actual} clipped expected object bbox {expected}")


def main() -> None:
    RESULT_DIR.mkdir(parents=True, exist_ok=True)
    sheet = build_source_sheet()
    reports = []
    for index, (mode, _, expected_bbox) in enumerate(CASES):
        cell = sheet.crop((index * CELL, 0, (index + 1) * CELL, CELL))
        full = universal_cutout(
            cell,
            CutoutSettings(mode=mode, trim=False, padding=8, stray_max_gap=20),
        )
        full_report = quality_report(full, mode)
        assert full_report.passed, f"{mode}: background bleed remained on sprite edge"
        assert_contains(full_report.visible_bbox, expected_bbox, mode)

        trimmed = universal_cutout(
            cell,
            CutoutSettings(mode=mode, trim=True, padding=8, stray_max_gap=20),
        )
        trimmed_path = RESULT_DIR / f"{mode}_cutout.png"
        trimmed.save(trimmed_path)
        trimmed_report = quality_report(trimmed, mode)
        assert trimmed_report.passed, f"{mode}: trimmed result has background bleed"
        reports.append({
            "case": mode,
            "path": trimmed_path.as_posix(),
            "full": asdict(full_report) | {"passed": full_report.passed},
            "trimmed": asdict(trimmed_report) | {"passed": trimmed_report.passed},
        })

    REPORT_PATH.write_text(json.dumps(reports, indent=2), encoding="utf-8")
    print(f"Wrote test source sheet to {SOURCE_SHEET}")
    print(f"Wrote {len(reports)} cutout results to {RESULT_DIR}")
    print(f"Wrote report to {REPORT_PATH}")


if __name__ == "__main__":
    main()
