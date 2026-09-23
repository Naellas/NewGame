from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import sys
from pathlib import Path

from PIL import Image

ROOT = JAVA_ROOT
ASSET_DIR = ROOT / "assets" / "environments/settlements/player_village"
SOURCE_DIR = ASSET_DIR / "source"


def remove_green_key(img: Image.Image) -> Image.Image:
    out = img.convert("RGBA")
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if g > 170 and r < 90 and b < 90:
                px[x, y] = (r, g, b, 0)
    return out


def stage_bounds(sheet: Image.Image, count: int, pad: int = 12) -> list[tuple[int, int, int, int]]:
    alpha = sheet.getchannel("A")
    columns: list[int] = []
    for x in range(sheet.width):
        if alpha.crop((x, 0, x + 1, sheet.height)).getbbox() is not None:
            columns.append(x)

    groups: list[tuple[int, int]] = []
    if columns:
        start = previous = columns[0]
        for x in columns[1:]:
            if x == previous + 1:
                previous = x
                continue
            groups.append((start, previous + 1))
            start = previous = x
        groups.append((start, previous + 1))

    if len(groups) != count:
        raise ValueError(f"Expected {count} stage clusters, found {len(groups)}")

    boxes: list[tuple[int, int, int, int]] = []
    for index, (left, right) in enumerate(groups):
        cluster = alpha.crop((left, 0, right, sheet.height))
        box = cluster.getbbox()
        if box is None:
            continue
        left_limit = groups[index - 1][1] if index > 0 else 0
        right_limit = groups[index + 1][0] if index < len(groups) - 1 else sheet.width
        boxes.append((
            max(left_limit, left + box[0] - pad),
            max(0, box[1] - pad),
            min(right_limit, left + box[2] + pad),
            min(sheet.height, box[3] + pad),
        ))
    return boxes


def main() -> None:
    source_arg = Path(sys.argv[1]) if len(sys.argv) > 1 else SOURCE_DIR / "player_village_growth_sheet_chroma.png"
    if not source_arg.exists():
        raise FileNotFoundError(source_arg)

    ASSET_DIR.mkdir(parents=True, exist_ok=True)
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)

    sheet = Image.open(source_arg)
    keyed = remove_green_key(sheet)
    keyed.save(SOURCE_DIR / "player_village_growth_sheet.png")

    for index, box in enumerate(stage_bounds(keyed, 10), start=1):
        cell = keyed.crop(box)
        cell.save(ASSET_DIR / f"player_village_stage_{index:02d}.png")


if __name__ == "__main__":
    main()
