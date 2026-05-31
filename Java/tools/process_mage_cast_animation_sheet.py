from __future__ import annotations

from pathlib import Path
import sys
from collections import deque

from PIL import Image, ImageFilter

sys.path.append(str(Path(__file__).resolve().parent))


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets" / "source" / "imagegen-mage-cast-sheet-10.png"
OUT = ROOT / "assets" / "animations" / "class_mage_model_cast_anim.png"
FRAMES = 10
FRAME_W = 387
FRAME_H = 757
TOP_MARGIN = 6


def main() -> None:
    if not SOURCE.exists():
        raise FileNotFoundError(SOURCE)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    source = Image.open(SOURCE).convert("RGBA")
    keyed = remove_green_background(source)
    frame_boxes = detect_frame_boxes(keyed)
    cutouts = [keyed.crop(box) for box in frame_boxes]
    frames = normalize_frames(cutouts)
    sheet = Image.new("RGBA", (FRAME_W * FRAMES, FRAME_H), (0, 0, 0, 0))
    for index, frame in enumerate(frames):
        sheet.alpha_composite(frame, (index * FRAME_W, 0))
    sheet.save(OUT)
    OUT.with_suffix(".frames").write_text(str(FRAMES), encoding="utf-8")
    print(f"Wrote {OUT}")


def remove_green_background(image: Image.Image) -> Image.Image:
    out = image.convert("RGBA")
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if g > 150 and g >= r + 50 and g >= b + 50:
                px[x, y] = (r, g, b, 0)
    return out


def detect_frame_boxes(image: Image.Image) -> list[tuple[int, int, int, int]]:
    components = connected_components(image)
    major = sorted((component for component in components if component[0] > 1000), key=lambda item: item[1][0])
    if len(major) != FRAMES:
        raise RuntimeError(f"Expected {FRAMES} mage components, found {len(major)}.")
    groups = [box for _, box in major]
    for pixels, box in components:
        if pixels > 1000:
            continue
        if pixels < 8:
            continue
        center_x = (box[0] + box[2]) / 2.0
        nearest_index = min(range(len(groups)), key=lambda index: abs(center_x - ((groups[index][0] + groups[index][2]) / 2.0)))
        group = groups[nearest_index]
        gap = max(0, max(group[0], box[0]) - min(group[2], box[2]))
        if gap > 80:
            continue
        groups[nearest_index] = merge_boxes(group, box)
    return [expand_box(box, image.size, 6) for box in groups]


def connected_components(image: Image.Image) -> list[tuple[int, tuple[int, int, int, int]]]:
    width, height = image.size
    alpha = image.getchannel("A")
    seen = [[False for _ in range(width)] for _ in range(height)]
    components = []
    for y in range(height):
        for x in range(width):
            if seen[y][x] or alpha.getpixel((x, y)) <= 8:
                continue
            q = deque([(x, y)])
            seen[y][x] = True
            pixels = 0
            min_x = max_x = x
            min_y = max_y = y
            while q:
                cx, cy = q.popleft()
                pixels += 1
                min_x = min(min_x, cx)
                max_x = max(max_x, cx)
                min_y = min(min_y, cy)
                max_y = max(max_y, cy)
                for ny in range(cy - 1, cy + 2):
                    for nx in range(cx - 1, cx + 2):
                        if nx < 0 or ny < 0 or nx >= width or ny >= height:
                            continue
                        if seen[ny][nx] or alpha.getpixel((nx, ny)) <= 8:
                            continue
                        seen[ny][nx] = True
                        q.append((nx, ny))
            components.append((pixels, (min_x, min_y, max_x + 1, max_y + 1)))
    return components


def merge_boxes(a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> tuple[int, int, int, int]:
    return min(a[0], b[0]), min(a[1], b[1]), max(a[2], b[2]), max(a[3], b[3])


def expand_box(box: tuple[int, int, int, int], size: tuple[int, int], padding: int) -> tuple[int, int, int, int]:
    return (
        max(0, box[0] - padding),
        max(0, box[1] - padding),
        min(size[0], box[2] + padding),
        min(size[1], box[3] + padding),
    )


def normalize_frames(cutouts: list[Image.Image]) -> list[Image.Image]:
    boxes = [visible_bbox(frame) for frame in cutouts]
    normalized = []
    for frame, box in zip(cutouts, boxes):
        cropped = frame.crop(box)
        canvas = Image.new("RGBA", (FRAME_W, FRAME_H), (0, 0, 0, 0))
        x = (FRAME_W - cropped.width) // 2
        y = FRAME_H - cropped.height
        canvas.alpha_composite(cropped, (x, y))
        normalized.append(clean_green_edge(canvas))
    return normalized


def visible_bbox(image: Image.Image) -> tuple[int, int, int, int]:
    alpha = image.getchannel("A")
    bbox = alpha.getbbox()
    if bbox is None:
        return 0, 0, image.width, image.height
    left, top, right, bottom = bbox
    return (
        max(0, left - 3),
        max(0, top - 3),
        min(image.width, right + 3),
        min(image.height, bottom + 3),
    )


def clean_green_edge(image: Image.Image) -> Image.Image:
    out = image.convert("RGBA")
    px = out.load()
    alpha = out.getchannel("A")
    edge = alpha.filter(ImageFilter.FIND_EDGES)
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if g > 120 and g >= r + 38 and g >= b + 38 and edge.getpixel((x, y)) > 0:
                px[x, y] = (max(0, r - 6), min(110, (r + b) // 2), b, max(0, a - 12))
    return out


if __name__ == "__main__":
    main()
