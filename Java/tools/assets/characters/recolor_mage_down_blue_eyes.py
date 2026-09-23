from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT

import math
from dataclasses import dataclass
from pathlib import Path

from PIL import Image


BASE_FRAME_WIDTH = 388
BASE_FRAME_HEIGHT = 757
DOWN_LEFT_EYE_CENTER = (173 / BASE_FRAME_WIDTH, 95 / BASE_FRAME_HEIGHT)
DOWN_RIGHT_EYE_CENTER = (210 / BASE_FRAME_WIDTH, 95 / BASE_FRAME_HEIGHT)
LEFT_PROFILE_EYE_CENTER = (67 / BASE_FRAME_WIDTH, 86 / BASE_FRAME_HEIGHT)
RIGHT_PROFILE_EYE_CENTER = (196 / BASE_FRAME_WIDTH, 86 / BASE_FRAME_HEIGHT)


@dataclass(frozen=True)
class Target:
    path: Path
    frame_count: int
    eye_centers: tuple[tuple[float, float], ...]


TARGETS = (
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/class_mage_model.png"), 1, (DOWN_LEFT_EYE_CENTER, DOWN_RIGHT_EYE_CENTER)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/class_mage_model_down.png"), 1, (DOWN_LEFT_EYE_CENTER, DOWN_RIGHT_EYE_CENTER)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/class_mage_model_left.png"), 1, (LEFT_PROFILE_EYE_CENTER,)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/class_mage_model_right.png"), 1, (RIGHT_PROFILE_EYE_CENTER,)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/animations/class_mage_model_down_walk_anim.png"), 12, (DOWN_LEFT_EYE_CENTER, DOWN_RIGHT_EYE_CENTER)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/animations/class_mage_model_down_start_walk_anim.png"), 6, (DOWN_LEFT_EYE_CENTER, DOWN_RIGHT_EYE_CENTER)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/animations/class_mage_model_down_stop_walk_anim.png"), 6, (DOWN_LEFT_EYE_CENTER, DOWN_RIGHT_EYE_CENTER)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/animations/class_mage_model_down_idle_anim.png"), 8, (DOWN_LEFT_EYE_CENTER, DOWN_RIGHT_EYE_CENTER)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/animations/class_mage_model_left_walk_anim.png"), 12, (LEFT_PROFILE_EYE_CENTER,)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/animations/class_mage_model_left_start_walk_anim.png"), 6, (LEFT_PROFILE_EYE_CENTER,)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/animations/class_mage_model_left_stop_walk_anim.png"), 6, (LEFT_PROFILE_EYE_CENTER,)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/animations/class_mage_model_left_idle_anim.png"), 8, (LEFT_PROFILE_EYE_CENTER,)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/animations/class_mage_model_right_walk_anim.png"), 12, (RIGHT_PROFILE_EYE_CENTER,)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/animations/class_mage_model_right_start_walk_anim.png"), 6, (RIGHT_PROFILE_EYE_CENTER,)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/animations/class_mage_model_right_stop_walk_anim.png"), 6, (RIGHT_PROFILE_EYE_CENTER,)),
    Target((JAVA_ROOT / "assets/characters/player/classes/mage/animations/class_mage_model_right_idle_anim.png"), 8, (RIGHT_PROFILE_EYE_CENTER,)),
)


def scaled_eye_center(frame_width: int, frame_height: int, normalized_center: tuple[float, float]) -> tuple[int, int]:
    return (
        round(normalized_center[0] * frame_width),
        round(normalized_center[1] * frame_height),
    )


def is_warm_eye_candidate(r: int, g: int, b: int, a: int, profile_view: bool) -> bool:
    if a == 0:
        return False
    luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
    min_luminance = 10 if profile_view else 24
    max_luminance = 120 if profile_view else 110
    if luminance < min_luminance or luminance > max_luminance:
        return False
    if r < 24 or g < 8 or b < 0:
        return False
    if r > (170 if profile_view else 150) or g > (130 if profile_view else 125) or b > (110 if profile_view else 100):
        return False
    red_green_delta = 12 if profile_view else 8
    green_blue_delta = 18 if profile_view else 14
    return r >= g - red_green_delta and g >= b - green_blue_delta and r >= b


def recolor_to_blue(r: int, g: int, b: int, a: int) -> tuple[int, int, int, int]:
    intensity = max(r, g, b)
    blue = min(255, round(intensity * 1.18 + 18))
    green = min(255, round(intensity * 0.72 + 8))
    red = max(0, round(intensity * 0.28))
    return red, green, blue, a


def recolor_eye(image: Image.Image, origin_x: int, origin_y: int, center_x: int, center_y: int, frame_width: int, frame_height: int,
                profile_view: bool) -> int:
    search_radius_x = max(7, round(frame_width * 0.031))
    search_radius_y = max(6, round(frame_height * 0.015))
    max_distance = max(6.0, frame_width * 0.031)
    candidates: list[tuple[float, float, int, int]] = []

    for local_y in range(center_y - search_radius_y, center_y + search_radius_y + 1):
        if local_y < 0 or local_y >= frame_height:
            continue
        for local_x in range(center_x - search_radius_x, center_x + search_radius_x + 1):
            if local_x < 0 or local_x >= frame_width:
                continue
            pixel = image.getpixel((origin_x + local_x, origin_y + local_y))
            r, g, b, a = pixel
            if not is_warm_eye_candidate(r, g, b, a, profile_view):
                continue
            distance = math.hypot(local_x - center_x, local_y - center_y)
            if distance > max_distance:
                continue
            luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
            candidates.append((distance, abs(luminance - 58.0), local_x, local_y))

    candidates.sort()
    modified = 0
    for _, _, local_x, local_y in candidates[:20]:
        image.putpixel(
            (origin_x + local_x, origin_y + local_y),
            recolor_to_blue(*image.getpixel((origin_x + local_x, origin_y + local_y))),
        )
        modified += 1
    return modified


def recolor_target(target: Target) -> int:
    image = Image.open(target.path).convert("RGBA")
    frame_width = image.width // target.frame_count
    frame_height = image.height
    modified = 0
    profile_view = len(target.eye_centers) == 1
    for frame_index in range(target.frame_count):
        origin_x = frame_index * frame_width
        for normalized_center in target.eye_centers:
            eye = scaled_eye_center(frame_width, frame_height, normalized_center)
            modified += recolor_eye(image, origin_x, 0, eye[0], eye[1], frame_width, frame_height, profile_view)

    image.save(target.path)
    return modified


def main() -> None:
    for target in TARGETS:
        modified = recolor_target(target)
        print(f"{target.path}: recolored {modified} pixels")


if __name__ == "__main__":
    main()
