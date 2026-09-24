from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


NPC_DIR = Path("assets/npcs")
PORTRAIT_SIZE = 96
FRAME = 4
INNER_SIZE = PORTRAIT_SIZE - FRAME * 2


def alpha_bbox(image: Image.Image) -> tuple[int, int, int, int] | None:
    return image.convert("RGBA").getchannel("A").getbbox()


def alpha_centroid_x(image: Image.Image, left: int, top: int, right: int, bottom: int) -> float:
    pixels = image.load()
    total = 0
    weighted = 0
    for y in range(top, bottom):
        for x in range(left, right):
            alpha = pixels[x, y][3]
            if alpha <= 16:
                continue
            weighted += x * alpha
            total += alpha
    return weighted / total if total else (left + right) / 2


def portrait_background() -> Image.Image:
    image = Image.new("RGBA", (PORTRAIT_SIZE, PORTRAIT_SIZE), (0, 0, 0, 255))
    pixels = image.load()
    for y in range(PORTRAIT_SIZE):
        for x in range(PORTRAIT_SIZE):
            depth = y / max(1, PORTRAIT_SIZE - 1)
            center = 1.0 - min(1.0, (((x - 48) / 54) ** 2 + ((y - 42) / 50) ** 2))
            r = int(22 + 18 * center + 12 * depth)
            g = int(25 + 20 * center + 11 * depth)
            b = int(29 + 24 * center + 10 * depth)
            pixels[x, y] = (r, g, b, 255)
    return image


def draw_frame(image: Image.Image) -> None:
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, PORTRAIT_SIZE - 1, PORTRAIT_SIZE - 1), outline=(20, 15, 12, 255), width=1)
    draw.rectangle((1, 1, PORTRAIT_SIZE - 2, PORTRAIT_SIZE - 2), outline=(117, 83, 45, 255), width=2)
    draw.rectangle((3, 3, PORTRAIT_SIZE - 4, PORTRAIT_SIZE - 4), outline=(212, 171, 99, 255), width=1)
    draw.rectangle((FRAME - 1, FRAME - 1, PORTRAIT_SIZE - FRAME, PORTRAIT_SIZE - FRAME), outline=(42, 31, 24, 255), width=1)


def portrait_from_model(model: Image.Image) -> Image.Image:
    source = model.convert("RGBA")
    bbox = alpha_bbox(source)
    if bbox is None:
        out = portrait_background()
        draw_frame(out)
        return out

    left, top, right, bottom = bbox
    body_w = right - left
    body_h = bottom - top
    head_band_top = top + int(body_h * 0.02)
    head_band_bottom = top + int(body_h * 0.26)
    body_center_x = (left + right) / 2
    focus_left = int(max(left, body_center_x - body_w * 0.28))
    focus_right = int(min(right, body_center_x + body_w * 0.28))
    center_x = alpha_centroid_x(source, focus_left, head_band_top, focus_right, head_band_bottom)
    center_y = top + body_h * 0.16
    side = max(32, int(round(body_h * 0.28)))

    crop_left = int(round(center_x - side / 2))
    crop_top = int(round(center_y - side / 2))
    crop_right = crop_left + side
    crop_bottom = crop_top + side

    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    src_left = max(0, crop_left)
    src_top = max(0, crop_top)
    src_right = min(source.width, crop_right)
    src_bottom = min(source.height, crop_bottom)
    if src_right > src_left and src_bottom > src_top:
        canvas.alpha_composite(source.crop((src_left, src_top, src_right, src_bottom)), (src_left - crop_left, src_top - crop_top))
    bust = canvas.resize((INNER_SIZE, INNER_SIZE), Image.Resampling.NEAREST)
    out = portrait_background()
    shadow = Image.new("RGBA", (INNER_SIZE, INNER_SIZE), (0, 0, 0, 0))
    shadow_alpha = bust.getchannel("A").point(lambda value: min(92, value // 2))
    shadow.putalpha(shadow_alpha)
    out.alpha_composite(shadow, (FRAME + 2, FRAME + 3))
    out.alpha_composite(bust, (FRAME, FRAME))
    draw_frame(out)
    return out


def main() -> None:
    for model_path in sorted(NPC_DIR.glob("npc_*_model.png")):
        portrait_path = model_path.with_name(model_path.name.removesuffix("_model.png") + ".png")
        portrait_from_model(Image.open(model_path)).save(portrait_path)


if __name__ == "__main__":
    main()
