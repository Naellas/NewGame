"""Shared image mechanics; importer-specific policies are explicit callbacks."""
from __future__ import annotations
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

def keyed_alpha(pixel: tuple[int, int, int, int]) -> int:
    r, g, b, a = pixel
    green_score = g - max(r, b)
    if g > 178 and green_score > 72:
        return 0
    if g > 132 and green_score > 42:
        return max(0, min(a, (g - 132) * 2))
    return a

def chroma_to_alpha(image: Image.Image, *, keyed_alpha, crop_alpha) -> Image.Image:
    rgba = image.convert("RGBA")
    keyed = []
    for pixel in rgba.getdata():
        alpha = keyed_alpha(pixel)
        if alpha == 0:
            keyed.append((0, 0, 0, 0))
            continue
        r, g, b, _ = pixel
        if g > r and g > b:
            g = min(g, int((r + b) / 2) + 24)
        keyed.append((r, g, b, alpha))
    rgba.putdata(keyed)
    return crop_alpha(rgba)

def normalize_height(image: Image.Image, target_h: int) -> Image.Image:
    if image.height <= 0:
        return image
    scale = target_h / image.height
    target_w = max(1, round(image.width * scale))
    return image.resize((target_w, target_h), Image.Resampling.LANCZOS)

def component_box(component: list[tuple[int, int]]) -> tuple[int, int, int, int]:
    return (
        min(x for x, _ in component),
        min(y for _, y in component),
        max(x for x, _ in component) + 1,
        max(y for _, y in component) + 1,
    )

def clip_blit(out: Image.Image, source: Image.Image, dx: int, dy: int) -> None:
    src_left = max(0, -dx)
    src_top = max(0, -dy)
    src_right = min(source.width, out.width - dx)
    src_bottom = min(source.height, out.height - dy)
    if src_right <= src_left or src_bottom <= src_top:
        return
    piece = source.crop((src_left, src_top, src_right, src_bottom))
    out.alpha_composite(piece, (max(0, dx), max(0, dy)))

def band(image: Image.Image, top: int, bottom: int, *, clamp) -> Image.Image:
    out = Image.new("RGBA", image.size, (0, 0, 0, 0))
    top = clamp(top, 0, image.height)
    bottom = clamp(bottom, top, image.height)
    if bottom > top:
        out.alpha_composite(image.crop((0, top, image.width, bottom)), (0, top))
    return out

def cutout(image: Image.Image) -> Image.Image:
    from tools.assets.shared.universal_cutout import CutoutSettings, universal_cutout
    return universal_cutout(
        image,
        CutoutSettings(
            mode="magenta",
            padding=3,
            trim=False,
            global_key=True,
            stray_max_gap=28,
            drop_edge_strays=True,
            spill_passes=6,
        ),
    )

def render_preview(paths: list[Path], out_path: Path, *, trim, preview_frame) -> None:
    cell_w = 112
    cell_h = 136
    label_h = 15
    columns = 8
    rows = (len(paths) + columns - 1) // columns
    sheet = Image.new("RGBA", (columns * cell_w, rows * (cell_h + label_h)), (25, 27, 32, 255))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("arial.ttf", 9)
    except OSError:
        font = ImageFont.load_default()

    for index, path in enumerate(paths):
        frame = trim(preview_frame(path, 2))
        col = index % columns
        row = index // columns
        x = col * cell_w + (cell_w - frame.width) // 2
        y = row * (cell_h + label_h) + cell_h - frame.height
        sheet.alpha_composite(frame, (x, y))
        draw.text((col * cell_w + 4, row * (cell_h + label_h) + cell_h + 1), path.stem[:24], fill=(236, 234, 220), font=font)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out_path)

def trim(img: Image.Image, padding: int = 2) -> Image.Image:
    bbox = img.getchannel("A").getbbox()
    if bbox is None:
        return img
    left, top, right, bottom = bbox
    return img.crop((
        max(0, left - padding),
        max(0, top - padding),
        min(img.width, right + padding),
        min(img.height, bottom + padding),
    ))

def remove_chroma(img: Image.Image, *, is_chroma) -> Image.Image:
    out = img.convert("RGBA")
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a > 0 and is_chroma((r, g, b, a)):
                px[x, y] = (r, g, b, 0)
    return out

def fit_footprint_lanczos(img: Image.Image, width: int, height: int) -> Image.Image:
    margin = 1
    max_w = width - margin * 2
    max_h = height - margin * 2
    scale = min(max_w / img.width, max_h / img.height)
    draw_w = max(1, int(round(img.width * scale)))
    draw_h = max(1, int(round(img.height * scale)))
    resized = img.resize((draw_w, draw_h), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    canvas.alpha_composite(resized, ((width - draw_w) // 2, height - draw_h - margin))
    return canvas

def fit_tile(img: Image.Image, *, SIZE) -> Image.Image:
    bbox = img.getchannel("A").getbbox()
    if bbox is None:
        return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    cropped = img.crop(bbox)
    scale = min(SIZE / cropped.width, SIZE / cropped.height)
    draw_w = max(1, int(round(cropped.width * scale)))
    draw_h = max(1, int(round(cropped.height * scale)))
    resized = cropped.resize((draw_w, draw_h), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    canvas.alpha_composite(resized, ((SIZE - draw_w) // 2, (SIZE - draw_h) // 2))
    return canvas

def crop_cell(source: Image.Image, index: int) -> Image.Image:
    col = index % 4
    row = index // 4
    x_edges = [round(source.width * i / 4) for i in range(5)]
    y_edges = [round(source.height * i / 4) for i in range(5)]
    return source.crop((x_edges[col], y_edges[row], x_edges[col + 1], y_edges[row + 1]))

def fit_footprint_nearest(image: Image.Image, width: int, height: int) -> Image.Image:
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

