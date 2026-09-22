from __future__ import annotations

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "assets" / "source" / "imagegen-cave-dungeon-tilesheet.png"
TERRAIN = ROOT / "assets" / "terrain"
DECO = ROOT / "assets" / "deco"
SIZE = 48

TILES = [
    ("terrain", "dungeon_cave_floor.png"),
    ("terrain", "dungeon_cave_floor_variant_1.png"),
    ("terrain", "dungeon_cave_floor_variant_2.png"),
    ("terrain", "dungeon_cave_rubble_floor.png"),
    ("terrain", "dungeon_cave_wall.png"),
    ("terrain", "dungeon_cave_wall_cap.png"),
    ("terrain", "dungeon_cave_wall_corner.png"),
    ("terrain", "dungeon_cave_void_edge.png"),
    ("terrain", "dungeon_cave_passage.png"),
    ("terrain", "dungeon_cave_water.png"),
    ("terrain", "dungeon_cave_bridge.png"),
    ("terrain", "dungeon_cave_stair.png"),
    ("deco", "dungeon_prop_cave_torch.png"),
    ("deco", "dungeon_prop_cave_crates.png"),
    ("deco", "dungeon_prop_cave_bedroll.png"),
    ("deco", "dungeon_prop_cave_crystal.png"),
]


def crop_cell(source: Image.Image, index: int) -> Image.Image:
    col = index % 4
    row = index // 4
    x_edges = [round(source.width * i / 4) for i in range(5)]
    y_edges = [round(source.height * i / 4) for i in range(5)]
    return source.crop((x_edges[col], y_edges[row], x_edges[col + 1], y_edges[row + 1]))


def fit_square(cell: Image.Image) -> Image.Image:
    return cell.convert("RGBA").resize((SIZE, SIZE), Image.Resampling.LANCZOS)


def dungeon_void() -> Image.Image:
    return Image.new("RGBA", (SIZE, SIZE), (4, 4, 5, 255))


def main() -> None:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing imagegen source sheet: {SRC}")
    TERRAIN.mkdir(parents=True, exist_ok=True)
    DECO.mkdir(parents=True, exist_ok=True)
    source = Image.open(SRC).convert("RGBA")
    for index, (folder, filename) in enumerate(TILES):
        out_dir = TERRAIN if folder == "terrain" else DECO
        fit_square(crop_cell(source, index)).save(out_dir / filename)
        print(f"Wrote {out_dir / filename}")
    dungeon_void().save(TERRAIN / "dungeon_void.png")
    print(f"Wrote {TERRAIN / 'dungeon_void.png'}")


if __name__ == "__main__":
    main()
