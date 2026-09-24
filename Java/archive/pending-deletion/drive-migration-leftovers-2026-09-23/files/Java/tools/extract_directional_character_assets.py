from __future__ import annotations

from pathlib import Path

from PIL import Image

from asset_paths import character_dir
from universal_cutout import CutoutSettings, trim_alpha, universal_cutout


SOURCE = Path("assets/source")

DIRECTIONS = ("down", "up", "left", "right")

SHEETS = [
    (
        SOURCE / "imagegen-player-class-directional-sheet.png",
        Path("assets/player/base"),
        [
            "player_model",
            "class_knight_model",
            "class_cleric_model",
            "class_ranger_model",
            "class_rogue_model",
        ],
    ),
    (
        SOURCE / "imagegen-npc-directional-sheet-a.png",
        Path("assets/npcs"),
        [
            "npc_baker_model",
            "npc_bartender_model",
            "npc_blacksmith_model",
            "npc_citizen_man_model",
            "npc_citizen_woman_model",
            "npc_elowen_model",
            "npc_garruk_model",
        ],
    ),
    (
        SOURCE / "imagegen-npc-directional-sheet-b.png",
        Path("assets/npcs"),
        [
            "npc_innkeeper_model",
            "npc_kael_model",
            "npc_liora_model",
            "npc_marla_model",
            "npc_merchant_model",
            "npc_mira_sunwarden_model",
            "npc_nyx_model",
        ],
    ),
    (
        SOURCE / "imagegen-npc-directional-sheet-c.png",
        Path("assets/npcs"),
        [
            "npc_orin_model",
            "npc_quartermaster_model",
            "npc_ren_model",
            "npc_rowan_model",
            "npc_sable_model",
            "npc_torin_model",
            "npc_vexa_model",
        ],
    ),
]


def nontransparent_components(image: Image.Image) -> list[list[tuple[int, int]]]:
    px = image.load()
    seen = [[False for _ in range(image.width)] for _ in range(image.height)]
    components: list[list[tuple[int, int]]] = []
    for y in range(image.height):
        for x in range(image.width):
            if seen[y][x] or px[x, y][3] == 0:
                continue
            component: list[tuple[int, int]] = []
            stack = [(x, y)]
            seen[y][x] = True
            while stack:
                cx, cy = stack.pop()
                component.append((cx, cy))
                for oy in (-1, 0, 1):
                    for ox in (-1, 0, 1):
                        if ox == 0 and oy == 0:
                            continue
                        nx = cx + ox
                        ny = cy + oy
                        if nx < 0 or ny < 0 or nx >= image.width or ny >= image.height:
                            continue
                        if seen[ny][nx] or px[nx, ny][3] == 0:
                            continue
                        seen[ny][nx] = True
                        stack.append((nx, ny))
            components.append(component)
    return components


def component_box(component: list[tuple[int, int]]) -> tuple[int, int, int, int]:
    return (
        min(x for x, _ in component),
        min(y for _, y in component),
        max(x for x, _ in component) + 1,
        max(y for _, y in component) + 1,
    )


def box_gap(a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> int:
    ax1, ay1, ax2, ay2 = a
    bx1, by1, bx2, by2 = b
    dx = max(0, max(ax1, bx1) - min(ax2, bx2))
    dy = max(0, max(ay1, by1) - min(ay2, by2))
    return max(dx, dy)


def box_intersection_area(a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> int:
    left = max(a[0], b[0])
    top = max(a[1], b[1])
    right = min(a[2], b[2])
    bottom = min(a[3], b[3])
    return max(0, right - left) * max(0, bottom - top)


def vertical_overlap(a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> int:
    return max(0, min(a[3], b[3]) - max(a[1], b[1]))


def clean_key(image: Image.Image) -> Image.Image:
    return universal_cutout(
        image,
        CutoutSettings(
            mode="magenta",
            padding=0,
            trim=False,
            global_key=True,
            clear_strays=False,
            spill_passes=8,
        ),
    )


def extract_sprite(source: Image.Image, bounds: tuple[int, int, int, int], padding: int) -> Image.Image:
    left, top, right, bottom = bounds
    crop_bounds = (
        max(0, left - padding),
        max(0, top - padding),
        min(source.width, right + padding),
        min(source.height, bottom + padding),
    )
    crop = clean_key(source.crop(crop_bounds))
    core = (
        left - crop_bounds[0],
        top - crop_bounds[1],
        right - crop_bounds[0],
        bottom - crop_bounds[1],
    )
    components = nontransparent_components(crop)
    if not components:
        return crop

    boxes = [component_box(component) for component in components]
    scored = []
    for component, box in zip(components, boxes):
        overlap = box_intersection_area(box, core)
        box_area = max(1, (box[2] - box[0]) * (box[3] - box[1]))
        if overlap <= 0:
            continue
        scored.append((overlap / box_area, len(component), component, box))
    if not scored:
        return trim_alpha(crop, 8)

    scored.sort(key=lambda item: (item[0], item[1]), reverse=True)
    main_component = scored[0][2]
    main_box = scored[0][3]
    keep: set[tuple[int, int]] = set(main_component)
    for component, box in zip(components, boxes):
        if component is main_component:
            continue
        overlap = box_intersection_area(box, core)
        area = max(1, (box[2] - box[0]) * (box[3] - box[1]))
        near_main = box_gap(main_box, box) <= max(20, padding // 3) and vertical_overlap(main_box, box) >= 10
        if overlap / area >= 0.30 and near_main:
            keep.update(component)

    out = Image.new("RGBA", crop.size, (0, 0, 0, 0))
    src = crop.load()
    dst = out.load()
    for x, y in keep:
        dst[x, y] = src[x, y]
    return trim_alpha(out, 8)


def extract_sheet(path: Path, out_dir: Path, names: list[str]) -> None:
    if not path.exists():
        raise FileNotFoundError(f"Missing directional sheet: {path}")
    source = Image.open(path).convert("RGBA")
    cell_w = source.width // len(DIRECTIONS)
    cell_h = source.height // len(names)
    padding = max(36, min(cell_w, cell_h) // 4)
    for row, name in enumerate(names):
        target_dir = character_dir(Path("assets"), name, out_dir)
        target_dir.mkdir(parents=True, exist_ok=True)
        for col, direction in enumerate(DIRECTIONS):
            left = col * cell_w
            top = row * cell_h
            right = source.width if col == len(DIRECTIONS) - 1 else (col + 1) * cell_w
            bottom = source.height if row == len(names) - 1 else (row + 1) * cell_h
            sprite = extract_sprite(source, (left, top, right, bottom), padding)
            sprite.save(target_dir / f"{name}_{direction}.png")


def main() -> None:
    for path, out_dir, names in SHEETS:
        extract_sheet(path, out_dir, names)


if __name__ == "__main__":
    main()
