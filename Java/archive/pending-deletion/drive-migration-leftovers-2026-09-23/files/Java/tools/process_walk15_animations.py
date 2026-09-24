from __future__ import annotations

from pathlib import Path
from shutil import copy2

from PIL import Image, ImageDraw, ImageFont

from asset_paths import animation_dir, find_asset
from universal_cutout import CutoutSettings, universal_cutout


ROOT = Path("assets")
SOURCE_OUT = ROOT / "source" / "walk15"
PREVIEW_OUT = SOURCE_OUT / "preview-walk15-contact-sheet.png"
FRAMES = 18


GENERATED_ROOT = Path.home() / ".codex" / "generated_images" / "019e7c67-4711-7b71-bdf1-e8ab8b1ab8af"


SHEETS = [
    ("player_down", "ig_0ed97a7e8a21cf4c016a1bc876a12c81919e14c76eb671b4a8.png", "down", [
        "player_model",
        "class_knight_model",
        "class_mage_model",
        "class_cleric_model",
        "class_ranger_model",
        "class_rogue_model",
    ]),
    ("player_up", "ig_0ed97a7e8a21cf4c016a1bc90851148191a699f667556f3eb5.png", "up", [
        "player_model",
        "class_knight_model",
        "class_mage_model",
        "class_cleric_model",
        "class_ranger_model",
        "class_rogue_model",
    ]),
    ("player_left", "ig_0ed97a7e8a21cf4c016a1bc94fa3088191a6e9acaa6fdd0849.png", "left", [
        "player_model",
        "class_knight_model",
        "class_mage_model",
        "class_cleric_model",
        "class_ranger_model",
        "class_rogue_model",
    ]),
    ("player_right", "ig_0ed97a7e8a21cf4c016a1bc99a8e608191b94c22a73ccc9631.png", "right", [
        "player_model",
        "class_knight_model",
        "class_mage_model",
        "class_cleric_model",
        "class_ranger_model",
        "class_rogue_model",
    ]),
    ("npc_a1_down", "ig_0ed97a7e8a21cf4c016a1bcaed19648191bb2fd043a4bbce3e.png", "down", [
        "npc_baker_model",
        "npc_bartender_model",
        "npc_blacksmith_model",
        "npc_citizen_man_model",
    ]),
    ("npc_a1_up", "ig_0ed97a7e8a21cf4c016a1bcb67011481919b01a1103e5aeebf.png", "up", [
        "npc_baker_model",
        "npc_bartender_model",
        "npc_blacksmith_model",
        "npc_citizen_man_model",
    ]),
    ("npc_a1_left", "ig_0ed97a7e8a21cf4c016a1bcbae926c819181de1c29f6ecaf58.png", "left", [
        "npc_baker_model",
        "npc_bartender_model",
        "npc_blacksmith_model",
        "npc_citizen_man_model",
    ]),
    ("npc_a1_right", "ig_0ed97a7e8a21cf4c016a1bcbfcd9188191abd926ff6be9c52a.png", "right", [
        "npc_baker_model",
        "npc_bartender_model",
        "npc_blacksmith_model",
        "npc_citizen_man_model",
    ]),
    ("npc_a2_down", "ig_0ed97a7e8a21cf4c016a1bcc4b25408191aa9a83ff48a0d41f.png", "down", [
        "npc_citizen_woman_model",
        "npc_elowen_model",
        "npc_garruk_model",
    ]),
    ("npc_a2_up", "ig_0ed97a7e8a21cf4c016a1bccbfe1b4819188b021d2ba3628d8.png", "up", [
        "npc_citizen_woman_model",
        "npc_elowen_model",
        "npc_garruk_model",
    ]),
    ("npc_a2_left", "ig_0ed97a7e8a21cf4c016a1bcd2ff16c8191a47583ea779f3f05.png", "left", [
        "npc_citizen_woman_model",
        "npc_elowen_model",
        "npc_garruk_model",
    ]),
    ("npc_a2_right", "ig_0ed97a7e8a21cf4c016a1bcdf3830081919c53f5365a3196e3.png", "right", [
        "npc_citizen_woman_model",
        "npc_elowen_model",
        "npc_garruk_model",
    ]),
    ("npc_b1_down", "ig_0ed97a7e8a21cf4c016a1bce46702c8191896461d6dd69d5fd.png", "down", [
        "npc_innkeeper_model",
        "npc_kael_model",
        "npc_liora_model",
        "npc_marla_model",
    ]),
    ("npc_b1_up", "ig_0ed97a7e8a21cf4c016a1bce95df588191a1af198d64dca50e.png", "up", [
        "npc_innkeeper_model",
        "npc_kael_model",
        "npc_liora_model",
        "npc_marla_model",
    ]),
    ("npc_b1_left", "ig_0ed97a7e8a21cf4c016a1bcee9cb448191a91ed625bd06c11b.png", "left", [
        "npc_innkeeper_model",
        "npc_kael_model",
        "npc_liora_model",
        "npc_marla_model",
    ]),
    ("npc_b1_right", "ig_0ed97a7e8a21cf4c016a1bcf310d448191a4b03e5e4811fcb3.png", "right", [
        "npc_innkeeper_model",
        "npc_kael_model",
        "npc_liora_model",
        "npc_marla_model",
    ]),
    ("npc_b2_down", "ig_0ed97a7e8a21cf4c016a1bcf8b6d6081918bc5ed6a26d96414.png", "down", [
        "npc_merchant_model",
        "npc_mira_sunwarden_model",
        "npc_nyx_model",
    ]),
    ("npc_b2_up", "ig_0ed97a7e8a21cf4c016a1bcfce2f9081918100091e172fe52a.png", "up", [
        "npc_merchant_model",
        "npc_mira_sunwarden_model",
        "npc_nyx_model",
    ]),
    ("npc_b2_left", "ig_0ed97a7e8a21cf4c016a1bd0403bfc81918869e591b24b4e78.png", "left", [
        "npc_merchant_model",
        "npc_mira_sunwarden_model",
        "npc_nyx_model",
    ]),
    ("npc_b2_right", "ig_0ed97a7e8a21cf4c016a1bd08ad5108191b6d7fbf4dc6c0c2e.png", "right", [
        "npc_merchant_model",
        "npc_mira_sunwarden_model",
        "npc_nyx_model",
    ]),
    ("npc_c1_down", "ig_0ed97a7e8a21cf4c016a1bd1818b3c8191a36639441d0c083c.png", "down", [
        "npc_orin_model",
        "npc_quartermaster_model",
        "npc_ren_model",
        "npc_rowan_model",
    ]),
    ("npc_c1_up", "ig_0ed97a7e8a21cf4c016a1bd1dfb3d88191b662291b645ea987.png", "up", [
        "npc_orin_model",
        "npc_quartermaster_model",
        "npc_ren_model",
        "npc_rowan_model",
    ]),
    ("npc_c1_left", "ig_0ed97a7e8a21cf4c016a1bd239f7948191a34adeab26ed54bb.png", "left", [
        "npc_orin_model",
        "npc_quartermaster_model",
        "npc_ren_model",
        "npc_rowan_model",
    ]),
    ("npc_c1_right", "ig_0ed97a7e8a21cf4c016a1bd285067881918d1beebe4d1af54c.png", "right", [
        "npc_orin_model",
        "npc_quartermaster_model",
        "npc_ren_model",
        "npc_rowan_model",
    ]),
    ("npc_c2_down", "ig_0ed97a7e8a21cf4c016a1c2a669d70819186aa28990a175a88.png", "down", [
        "npc_sable_model",
        "npc_torin_model",
        "npc_vexa_model",
    ]),
    ("npc_c2_up", "ig_0ed97a7e8a21cf4c016a1c2acfc3188191bc588e187ebcda1a.png", "up", [
        "npc_sable_model",
        "npc_torin_model",
        "npc_vexa_model",
    ]),
    ("npc_c2_left", "ig_0ed97a7e8a21cf4c016a1c2b1707e48191bf098bc22533cbee.png", "left", [
        "npc_sable_model",
        "npc_torin_model",
        "npc_vexa_model",
    ]),
    ("npc_c2_right", "ig_0ed97a7e8a21cf4c016a1c2b701c608191b1be0fc2d834f1e8.png", "right", [
        "npc_sable_model",
        "npc_torin_model",
        "npc_vexa_model",
    ]),
]


def remove_key(image: Image.Image) -> Image.Image:
    return universal_cutout(
        image,
        CutoutSettings(mode="magenta", padding=0, trim=False, global_key=True, clear_strays=False, clean_spill=False),
    )


def universal_cutout_frame(image: Image.Image, pad: int = 2) -> Image.Image:
    return universal_cutout(
        image,
        CutoutSettings(
            mode="magenta",
            padding=pad,
            global_key=True,
            stray_max_gap=24,
            drop_edge_strays=True,
            spill_passes=6,
        ),
    )


def alpha_bbox(image: Image.Image) -> tuple[int, int, int, int] | None:
    alpha = image.getchannel("A")
    return alpha.getbbox()


def trim(image: Image.Image, pad: int = 4) -> Image.Image:
    box = alpha_bbox(image)
    if box is None:
        return image
    left, top, right, bottom = box
    return image.crop((
        max(0, left - pad),
        max(0, top - pad),
        min(image.width, right + pad),
        min(image.height, bottom + pad),
    ))


def padded_bounds(start: int, end: int, limit: int, pad: int) -> tuple[int, int]:
    return max(0, start - pad), min(limit, end + pad)


def projection_bounds(image: Image.Image, axis: str, expected: int) -> list[tuple[int, int]]:
    if axis == "x":
        length = image.width
        other = image.height
        values = [
            sum(1 for y in range(other) if image.getpixel((x, y))[3] > 0)
            for x in range(length)
        ]
    else:
        length = image.height
        other = image.width
        values = [
            sum(1 for x in range(other) if image.getpixel((x, y))[3] > 0)
            for y in range(length)
        ]

    threshold = max(3, max(values) // 80)
    runs: list[tuple[int, int, int]] = []
    start: int | None = None
    total = 0
    for index, value in enumerate(values):
        if value > threshold:
            if start is None:
                start = index
                total = 0
            total += value
        elif start is not None:
            if index - start >= 2:
                runs.append((start, index, total))
            start = None
    if start is not None:
        runs.append((start, length, total))

    # Join tiny breaks caused by split legs, weapons, or robe holes inside one frame.
    merged: list[tuple[int, int, int]] = []
    max_gap = max(4, length // (expected * 8))
    for run in runs:
        if merged and run[0] - merged[-1][1] <= max_gap:
            prev = merged[-1]
            merged[-1] = (prev[0], run[1], prev[2] + run[2])
        else:
            merged.append(run)
    runs = [run for run in merged if run[1] - run[0] >= max(3, length // (expected * 28))]

    if len(runs) != expected:
        return even_bounds(length, expected)

    centers = [(start + end) / 2.0 for start, end, _ in runs]
    bounds: list[tuple[int, int]] = []
    for index, center in enumerate(centers):
        if index == 0:
            left = max(0, int(round(center - (centers[index + 1] - center) / 2.0)))
        else:
            left = int(round((centers[index - 1] + center) / 2.0))
        if index == len(centers) - 1:
            right = min(length, int(round(center + (center - centers[index - 1]) / 2.0)))
        else:
            right = int(round((center + centers[index + 1]) / 2.0))
        bounds.append((left, right))
    return bounds


def even_bounds(length: int, expected: int) -> list[tuple[int, int]]:
    return [
        (round(index * length / expected), round((index + 1) * length / expected))
        for index in range(expected)
    ]


def source_path(label: str, filename: str) -> Path:
    generated = GENERATED_ROOT / filename
    if generated.exists():
        copy2(generated, SOURCE_OUT / f"{label}.png")
        return generated
    local = SOURCE_OUT / f"{label}.png"
    if local.exists():
        return local
    raise FileNotFoundError(f"Missing generated source {generated} and local source {local}")


def character_rows(sheet: Image.Image, rows: int) -> list[Image.Image]:
    keyed_sheet = remove_key(sheet)
    row_bounds = component_row_bounds(keyed_sheet, rows)
    row_sheets: list[Image.Image] = []
    for top, bottom in row_bounds:
        top, bottom = padded_bounds(top, bottom, sheet.height, 6)
        row_sheets.append(sheet.crop((0, top, sheet.width, bottom)))
    return row_sheets


def component_row_bounds(image: Image.Image, rows: int) -> list[tuple[int, int]]:
    components = foreground_components(image, min_pixels=32)
    if len(components) < rows:
        return projection_bounds(image, "y", rows)

    centers = weighted_row_centers(components, rows)
    assigned: list[list[dict[str, object]]] = [[] for _ in range(rows)]
    for component in components:
        box = component["box"]
        assert isinstance(box, tuple)
        center_y = (box[1] + box[3]) / 2.0
        row = min(range(rows), key=lambda index: abs(center_y - centers[index]))
        assigned[row].append(component)

    bounds: list[tuple[int, int]] = []
    fallback = projection_bounds(image, "y", rows)
    for index, group in enumerate(assigned):
        if not group:
            bounds.append(fallback[index])
            continue
        min_y = min(component["box"][1] for component in group)  # type: ignore[index]
        max_y = max(component["box"][3] for component in group)  # type: ignore[index]
        bounds.append((int(min_y), int(max_y)))
    bounds.sort(key=lambda bound: bound[0])
    return bounds


def weighted_row_centers(components: list[dict[str, object]], rows: int) -> list[float]:
    weighted: list[float] = []
    for component in components:
        box = component["box"]
        assert isinstance(box, tuple)
        count = len(component["pixels"])  # type: ignore[arg-type]
        center = (box[1] + box[3]) / 2.0
        weighted.extend([center] * max(1, min(20, count // 80)))
    weighted.sort()
    centers = [
        weighted[min(len(weighted) - 1, round((index + 0.5) * len(weighted) / rows))]
        for index in range(rows)
    ]
    for _ in range(12):
        groups: list[list[float]] = [[] for _ in range(rows)]
        for value in weighted:
            row = min(range(rows), key=lambda index: abs(value - centers[index]))
            groups[row].append(value)
        next_centers = [
            (sum(group) / len(group)) if group else centers[index]
            for index, group in enumerate(groups)
        ]
        if all(abs(a - b) < 0.1 for a, b in zip(centers, next_centers, strict=True)):
            break
        centers = next_centers
    return sorted(centers)


def foreground_components(image: Image.Image, min_pixels: int = 1) -> list[dict[str, object]]:
    px = image.load()
    width, height = image.size
    seen = [[False for _ in range(width)] for _ in range(height)]
    components: list[dict[str, object]] = []
    for y in range(height):
        for x in range(width):
            if seen[y][x] or px[x, y][3] == 0:
                continue
            stack = [(x, y)]
            seen[y][x] = True
            pixels: list[tuple[int, int]] = []
            min_x = max_x = x
            min_y = max_y = y
            while stack:
                cx, cy = stack.pop()
                pixels.append((cx, cy))
                min_x = min(min_x, cx)
                max_x = max(max_x, cx)
                min_y = min(min_y, cy)
                max_y = max(max_y, cy)
                for oy in (-1, 0, 1):
                    for ox in (-1, 0, 1):
                        if ox == 0 and oy == 0:
                            continue
                        nx = cx + ox
                        ny = cy + oy
                        if not (0 <= nx < width and 0 <= ny < height):
                            continue
                        if seen[ny][nx] or px[nx, ny][3] == 0:
                            continue
                        seen[ny][nx] = True
                        stack.append((nx, ny))
            if len(pixels) >= min_pixels:
                components.append({
                    "pixels": pixels,
                    "box": (min_x, min_y, max_x + 1, max_y + 1),
                })
    return components


def slice_character_row(row_sheet: Image.Image) -> list[Image.Image]:
    keyed_row = remove_key(row_sheet)
    col_bounds = projection_bounds(keyed_row, "x", FRAMES)
    overlap = max(6, row_sheet.width // (FRAMES * 5))
    frames: list[Image.Image] = []
    for left, right in col_bounds:
        left, right = padded_bounds(left, right, row_sheet.width, overlap)
        cell = row_sheet.crop((left, 0, right, row_sheet.height))
        frames.append(universal_cutout_frame(cell, pad=2))
    return frames


def save_strip(sprite: str, direction: str, frames: list[Image.Image]) -> Path:
    frame_w, frame_h = reference_frame_size(sprite, direction, frames)
    strip = Image.new("RGBA", (frame_w * FRAMES, frame_h), (0, 0, 0, 0))
    fitted_frames: list[Image.Image] = []
    scale = strip_scale(frames, frame_w, frame_h)
    for index, frame in enumerate(frames):
        fitted = fit_frame_to_reference(frame, frame_w, frame_h, scale)
        fitted_frames.append(fitted)
        strip.alpha_composite(fitted, (index * frame_w, 0))
    name = f"{sprite}_{direction}_walk_anim"
    out_dir = animation_dir(ROOT, name)
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / f"{name}.png"
    strip.save(out_path)
    (out_dir / f"{name}.frames").write_text(str(FRAMES), encoding="utf-8")
    save_static_if_broken(sprite, direction, fitted_frames[0])
    return out_path


def reference_frame_size(sprite: str, direction: str, frames: list[Image.Image]) -> tuple[int, int]:
    path = static_sprite_path(sprite, direction)
    if path is not None:
        image = Image.open(path).convert("RGBA")
        box = image.getchannel("A").getbbox()
        if static_bbox_valid(box):
            return box[2] - box[0], box[3] - box[1]
    trimmed = [trim(frame, 0) for frame in frames]
    return max(frame.width for frame in trimmed), max(frame.height for frame in trimmed)


def static_bbox_valid(box: tuple[int, int, int, int] | None) -> bool:
    if box is None:
        return False
    return box[2] - box[0] >= 24 and box[3] - box[1] >= 48


def save_static_if_broken(sprite: str, direction: str, frame: Image.Image) -> None:
    path = static_sprite_path(sprite, direction)
    if path is None:
        return
    image = Image.open(path).convert("RGBA")
    if static_bbox_valid(image.getchannel("A").getbbox()):
        return
    framed = Image.new("RGBA", (frame.width + 16, frame.height + 16), (0, 0, 0, 0))
    framed.alpha_composite(frame, (8, 8))
    framed.save(path)


def static_sprite_path(sprite: str, direction: str) -> Path | None:
    filename = f"{sprite}_{direction}.png"
    try:
        return find_asset(ROOT, filename)
    except FileNotFoundError:
        return None


def strip_scale(frames: list[Image.Image], width: int, height: int) -> float:
    sources = [trim(frame, 0) for frame in frames]
    max_h = max(source.height for source in sources)
    return height / max_h


def fit_frame_to_reference(frame: Image.Image, width: int, height: int, scale: float) -> Image.Image:
    source = trim(frame, 0)
    fitted = source.resize((
        max(1, round(source.width * scale)),
        max(1, round(source.height * scale)),
    ), Image.Resampling.NEAREST)
    canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    x = (width - fitted.width) // 2
    y = height - fitted.height
    src_left = max(0, -x)
    src_top = max(0, -y)
    src_right = min(fitted.width, width - x)
    src_bottom = min(fitted.height, height - y)
    if src_right > src_left and src_bottom > src_top:
        cropped = fitted.crop((src_left, src_top, src_right, src_bottom))
        canvas.alpha_composite(cropped, (max(0, x), max(0, y)))
    return canvas


def render_preview(paths: list[Path]) -> None:
    cell_w = 120
    cell_h = 118
    label_h = 18
    columns = 8
    rows = (len(paths) + columns - 1) // columns
    sheet = Image.new("RGBA", (columns * cell_w, rows * (cell_h + label_h)), (28, 31, 39, 255))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("arial.ttf", 10)
    except OSError:
        font = ImageFont.load_default()
    for index, path in enumerate(paths):
        image = Image.open(path).convert("RGBA")
        frame_w = image.width // FRAMES
        frame = image.crop((frame_w * 4, 0, frame_w * 5, image.height))
        frame = trim(frame, 0)
        scale = min((cell_w - 12) / frame.width, (cell_h - 10) / frame.height)
        frame = frame.resize((
            max(1, round(frame.width * scale)),
            max(1, round(frame.height * scale)),
        ), Image.Resampling.NEAREST)
        col = index % columns
        row = index // columns
        x = col * cell_w + (cell_w - frame.width) // 2
        y = row * (cell_h + label_h) + cell_h - frame.height
        sheet.alpha_composite(frame, (x, y))
        draw.text((col * cell_w + 4, row * (cell_h + label_h) + cell_h + 2), path.stem.removesuffix("_anim"), fill=(235, 235, 225), font=font)
    sheet.save(PREVIEW_OUT)


def main() -> None:
    SOURCE_OUT.mkdir(parents=True, exist_ok=True)
    outputs: list[Path] = []
    for label, filename, direction, names in SHEETS:
        source = source_path(label, filename)
        sheet = Image.open(source).convert("RGBA")
        rows = character_rows(sheet, len(names))
        for row_sheet, name in zip(rows, names, strict=True):
            outputs.append(save_strip(name, direction, slice_character_row(row_sheet)))
    render_preview(outputs)
    print(f"Wrote {len(outputs)} animation strips to owner animation folders")
    print(f"Wrote preview to {PREVIEW_OUT}")


if __name__ == "__main__":
    main()
