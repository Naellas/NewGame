from __future__ import annotations

import argparse
from collections import deque
from dataclasses import dataclass
from pathlib import Path

from PIL import Image

SRC_CITY_SHEET = Path("assets/source/imagegen-city-tilesheet.png")
OUT_CITY_DIR = Path("assets/city")


@dataclass
class Component:
    area: int
    left: int
    top: int
    right: int
    bottom: int
    pixels: list[tuple[int, int]]

    @property
    def width(self) -> int:
        return self.right - self.left + 1

    @property
    def height(self) -> int:
        return self.bottom - self.top + 1

    @property
    def cx(self) -> float:
        return (self.left + self.right) / 2

    @property
    def cy(self) -> float:
        return (self.top + self.bottom) / 2


def is_dark_background(rgb: tuple[int, int, int]) -> bool:
    r, g, b = rgb
    return max(r, g, b) <= 58 and b >= g - 4 and g >= r - 6


def detect_components(img: Image.Image) -> list[Component]:
    rgba = img.convert("RGBA")
    px = rgba.load()
    width, height = rgba.size
    bg = [[False for _ in range(width)] for _ in range(height)]
    q: deque[tuple[int, int]] = deque()
    seen: set[tuple[int, int]] = set()

    for x in range(width):
        q.append((x, 0))
        q.append((x, height - 1))
    for y in range(height):
        q.append((0, y))
        q.append((width - 1, y))

    while q:
        x, y = q.popleft()
        if (x, y) in seen or not (0 <= x < width and 0 <= y < height):
            continue
        seen.add((x, y))
        if not is_dark_background(px[x, y][:3]):
            continue
        bg[y][x] = True
        q.append((x + 1, y))
        q.append((x - 1, y))
        q.append((x, y + 1))
        q.append((x, y - 1))

    mask = [[not bg[y][x] for x in range(width)] for y in range(height)]
    seen2 = [[False for _ in range(width)] for _ in range(height)]
    components: list[Component] = []

    for y in range(height):
        for x in range(width):
            if seen2[y][x] or not mask[y][x]:
                continue
            qq: deque[tuple[int, int]] = deque([(x, y)])
            seen2[y][x] = True
            points: list[tuple[int, int]] = []
            left = x
            right = x
            top = y
            bottom = y
            while qq:
                cx, cy = qq.popleft()
                points.append((cx, cy))
                left = min(left, cx)
                right = max(right, cx)
                top = min(top, cy)
                bottom = max(bottom, cy)
                for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
                    if not (0 <= nx < width and 0 <= ny < height):
                        continue
                    if seen2[ny][nx] or not mask[ny][nx]:
                        continue
                    seen2[ny][nx] = True
                    qq.append((nx, ny))
            components.append(Component(area=len(points), left=left, top=top, right=right, bottom=bottom, pixels=points))

    components.sort(key=lambda c: c.area, reverse=True)
    return components


def component_image(img: Image.Image, comp: Component) -> Image.Image:
    crop = img.crop((comp.left, comp.top, comp.right + 1, comp.bottom + 1)).convert("RGBA")
    local = {(x - comp.left, y - comp.top) for x, y in comp.pixels}
    px = crop.load()
    for y in range(crop.height):
        for x in range(crop.width):
            if (x, y) not in local:
                r, g, b, _ = px[x, y]
                px[x, y] = (r, g, b, 0)
    return crop


def roof_pixel_score(img: Image.Image, comp: Component) -> int:
    crop = component_image(img, comp).convert("RGBA")
    px = crop.load()
    score = 0
    for y in range(crop.height):
        for x in range(crop.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            # Red-brown roof shingles.
            if r >= 82 and g <= 105 and b <= 100 and r >= g - 10:
                score += 1
    return score


def stone_pixel_score(img: Image.Image, comp: Component) -> int:
    crop = component_image(img, comp).convert("RGBA")
    px = crop.load()
    score = 0
    for y in range(crop.height):
        for x in range(crop.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            if 58 <= r <= 176 and 58 <= g <= 176 and 58 <= b <= 176 and abs(r - g) < 36 and abs(g - b) < 36:
                score += 1
    return score


def banner_blue_score(img: Image.Image, comp: Component) -> int:
    crop = component_image(img, comp).convert("RGBA")
    px = crop.load()
    score = 0
    for y in range(crop.height):
        for x in range(crop.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            if b >= r + 24 and b >= g + 10 and b > 72:
                score += 1
    return score


def torch_warm_score(img: Image.Image, comp: Component) -> int:
    crop = component_image(img, comp).convert("RGBA")
    px = crop.load()
    score = 0
    for y in range(crop.height):
        for x in range(crop.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            if r > 176 and g > 106 and b < 96 and r > g:
                score += 1
    return score


def choose_house_front(img: Image.Image, components: list[Component]) -> Component:
    width, height = img.size
    best: Component | None = None
    best_score = -10**9
    for comp in components:
        if comp.area < 12000:
            continue
        if not (170 <= comp.width <= 280 and 110 <= comp.height <= 190):
            continue
        roof_score = roof_pixel_score(img, comp)
        center_penalty = abs(comp.cx - width * 0.72) + abs(comp.cy - height * 0.36)
        score = comp.area * 1.4 + roof_score * 1.7 - center_penalty * 10
        if score > best_score:
            best_score = score
            best = comp
    if best is None:
        raise RuntimeError("Could not find a house-front candidate in the city sheet.")
    return best


def choose_wall_gate(img: Image.Image, components: list[Component]) -> Component:
    width, height = img.size
    best: Component | None = None
    best_score = -10**9
    for comp in components:
        if comp.area < 9000:
            continue
        if not (150 <= comp.width <= 210 and 95 <= comp.height <= 130):
            continue
        roof = roof_pixel_score(img, comp)
        stone = stone_pixel_score(img, comp)
        stone_ratio = stone / max(1, comp.area)
        roof_ratio = roof / max(1, comp.area)
        if roof_ratio > 0.24 or stone_ratio < 0.28:
            continue
        banner = banner_blue_score(img, comp)
        torch = torch_warm_score(img, comp)
        center_penalty = abs(comp.cx - width * 0.80) + abs(comp.cy - height * 0.24)
        score = comp.area * 0.8 + stone * 1.2 + banner * 2.8 + torch * 4.2 - center_penalty * 8.0 - roof * 0.4
        if score > best_score:
            best_score = score
            best = comp
    if best is None:
        raise RuntimeError("Could not find a wall-gate candidate in the city sheet.")
    return best


def choose_wall_block(img: Image.Image, components: list[Component]) -> Component:
    best: Component | None = None
    best_score = -10**9
    for comp in components:
        if comp.area < 6500:
            continue
        if not (100 <= comp.width <= 150 and 70 <= comp.height <= 95):
            continue
        roof = roof_pixel_score(img, comp)
        stone = stone_pixel_score(img, comp)
        stone_ratio = stone / max(1, comp.area)
        roof_ratio = roof / max(1, comp.area)
        if roof_ratio > 0.10:
            continue
        piece = component_image(img, comp).convert("RGBA")
        px = piece.load()
        dark = 0
        alpha = 0
        for y in range(piece.height):
            for x in range(piece.width):
                r, g, b, a = px[x, y]
                if not a:
                    continue
                alpha += 1
                if max(r, g, b) < 126 and min(r, g, b) > 24 and abs(r - g) < 30 and abs(g - b) < 30:
                    dark += 1
        dark_ratio = dark / max(1, alpha)
        if stone_ratio < 0.25 or dark_ratio < 0.34:
            continue
        score = dark_ratio * 13000 + stone_ratio * 7000 + comp.area * 0.18 - roof_ratio * 7000 + comp.width * 6
        if score > best_score:
            best_score = score
            best = comp
    if best is None:
        raise RuntimeError("Could not find a wall-block candidate in the city sheet.")
    return best


def choose_roof_detail(img: Image.Image, house: Component, components: list[Component]) -> Component:
    best: Component | None = None
    best_score = -10**9
    target_w = house.width
    for comp in components:
        if comp.area < 4500:
            continue
        if not (140 <= comp.width <= 280 and 30 <= comp.height <= 90):
            continue
        roof = roof_pixel_score(img, comp)
        roof_ratio = roof / max(1, comp.area)
        width_penalty = abs(comp.width - target_w)
        score = roof_ratio * 10000 + comp.area * 0.2 - width_penalty * 18
        if score > best_score:
            best_score = score
            best = comp
    if best is None:
        # Fallback: derive from house upper band.
        return house
    return best


CITY_SOURCE_PROP_TARGETS = {
    "city_prop_source_barrel_open": (592.5, 611.0),
    "city_prop_source_crate_low": (642.0, 611.5),
    "city_prop_source_barrel_tall": (691.5, 611.5),
    "city_prop_source_crate_tall": (798.0, 605.5),
    "city_prop_source_stone_pile": (811.0, 788.0),
    "city_prop_source_snow_bush": (986.0, 864.5),
    "city_prop_source_reeds": (792.0, 935.0),
    "city_prop_source_snow_stump": (707.0, 935.0),
}


def choose_component_near(components: list[Component], target: tuple[float, float]) -> Component:
    tx, ty = target
    candidates = [
        comp
        for comp in components
        if 450 <= comp.area <= 7000 and 18 <= comp.width <= 90 and 18 <= comp.height <= 90
    ]
    if not candidates:
        raise RuntimeError(f"Could not find a source prop near ({tx:.1f}, {ty:.1f}).")
    return min(candidates, key=lambda comp: abs(comp.cx - tx) + abs(comp.cy - ty) + abs(comp.area - 1800) * 0.002)


def _save_component(img: Image.Image, comp: Component, out_path: Path, *, trim_alpha: bool = True) -> None:
    piece = component_image(img, comp)
    if trim_alpha:
        bbox = piece.getbbox()
        if bbox is not None:
            piece = piece.crop(bbox)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    piece.save(out_path)


def extract_city_key_assets(source: Path, out_dir: Path, replace: bool, assets: set[str]) -> None:
    if not source.exists():
        raise FileNotFoundError(f"Missing source sheet: {source}")
    out_dir.mkdir(parents=True, exist_ok=True)

    img = Image.open(source).convert("RGBA")
    components = detect_components(img)
    house: Component | None = None
    if "house" in assets or "roof" in assets:
        house = choose_house_front(img, components)
    if "house" in assets and house is not None:
        name = "city_house_front.png" if replace else "city_house_front_candidate.png"
        out_path = out_dir / name
        _save_component(img, house, out_path)
        print(f"Saved {out_path} from bbox ({house.left},{house.top})-({house.right},{house.bottom}), size={house.width}x{house.height}")

    if "wall_gate" in assets:
        gate = choose_wall_gate(img, components)
        name = "city_wall_gate.png" if replace else "city_wall_gate_candidate.png"
        out_path = out_dir / name
        _save_component(img, gate, out_path)
        print(f"Saved {out_path} from bbox ({gate.left},{gate.top})-({gate.right},{gate.bottom}), size={gate.width}x{gate.height}")

    if "wall_block" in assets:
        block = choose_wall_block(img, components)
        name = "city_wall_block.png" if replace else "city_wall_block_candidate.png"
        out_path = out_dir / name
        _save_component(img, block, out_path)
        print(f"Saved {out_path} from bbox ({block.left},{block.top})-({block.right},{block.bottom}), size={block.width}x{block.height}")

    if "roof" in assets and house is not None:
        roof_comp = choose_roof_detail(img, house, components)
        if roof_comp is house:
            house_img = component_image(img, house)
            roof_h = max(20, int(house_img.height * 0.56))
            roof_img = house_img.crop((0, 0, house_img.width, roof_h))
            bbox = roof_img.getbbox()
            if bbox is not None:
                roof_img = roof_img.crop(bbox)
        else:
            roof_img = component_image(img, roof_comp)
            bbox = roof_img.getbbox()
            if bbox is not None:
                roof_img = roof_img.crop(bbox)
        name = "city_roof_detail.png" if replace else "city_roof_detail_candidate.png"
        out_path = out_dir / name
        roof_img.save(out_path)
        print(f"Saved {out_path}, size={roof_img.width}x{roof_img.height}")

    if "props" in assets:
        for name, target in CITY_SOURCE_PROP_TARGETS.items():
            prop = choose_component_near(components, target)
            out_path = out_dir / f"{name}.png"
            _save_component(img, prop, out_path)
            print(f"Saved {out_path} from bbox ({prop.left},{prop.top})-({prop.right},{prop.bottom}), size={prop.width}x{prop.height}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Extract profile-matched key city assets.")
    parser.add_argument("--source", type=Path, default=SRC_CITY_SHEET, help="Source city sheet path.")
    parser.add_argument("--out", type=Path, default=OUT_CITY_DIR, help="Output city asset directory.")
    parser.add_argument(
        "--replace",
        action="store_true",
        help="Replace the live asset name (city_house_front.png). Otherwise writes *_candidate.png.",
    )
    parser.add_argument(
        "--assets",
        default="house,wall_gate,wall_block,roof",
        help="Comma-separated keys: house, wall_gate, wall_block, roof, props",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    requested = {item.strip().lower() for item in args.assets.split(",") if item.strip()}
    valid = {"house", "wall_gate", "wall_block", "roof", "props"}
    unknown = sorted(requested - valid)
    if unknown:
        raise ValueError(f"Unknown asset keys: {', '.join(unknown)}")
    extract_city_key_assets(source=args.source, out_dir=args.out, replace=args.replace, assets=requested)


if __name__ == "__main__":
    main()
