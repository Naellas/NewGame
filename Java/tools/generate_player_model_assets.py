from __future__ import annotations

from pathlib import Path

from PIL import Image

from chroma_cutout import chroma_cutout, fit


SRC = Path("assets/source/imagegen-player-model-sheet.png")
OUT = Path("assets/player")

NAMES = (
    "player_model",
    "class_knight_model",
    "class_mage_model",
    "class_cleric_model",
    "class_ranger_model",
)


def recolor_preserving_value(image: Image.Image, target: tuple[int, int, int], predicate) -> Image.Image:
    out = image.convert("RGBA")
    px = out.load()
    target_value = max(target)
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0 or not predicate(r, g, b):
                continue
            value = max(r, g, b)
            factor = value / max(1, target_value)
            px[x, y] = (
                min(255, max(0, int(target[0] * factor))),
                min(255, max(0, int(target[1] * factor))),
                min(255, max(0, int(target[2] * factor))),
                a,
            )
    return out


def make_cleric(mage: Image.Image) -> Image.Image:
    def robe(r: int, g: int, b: int) -> bool:
        return b > 95 and b >= r + 18 and b >= g + 22

    cleric = recolor_preserving_value(mage, (226, 211, 172), robe)

    def trim(r: int, g: int, b: int) -> bool:
        return b > 120 and r > 70 and g > 45 and abs(r - g) < 80

    return recolor_preserving_value(cleric, (183, 147, 72), trim)


def make_rogue(ranger: Image.Image) -> Image.Image:
    def cloak(r: int, g: int, b: int) -> bool:
        return g > 80 and g >= r + 14 and g >= b + 10

    rogue = recolor_preserving_value(ranger, (63, 76, 70), cloak)

    def leather(r: int, g: int, b: int) -> bool:
        return r > 80 and g > 45 and b < 80 and r >= g + 12

    return recolor_preserving_value(rogue, (96, 62, 45), leather)


def write_world_sprites(models: dict[str, Image.Image]) -> None:
    specs = {
        "player": ("player_model", 144, 172),
        "player_battle": ("player_model", 144, 172),
        "class_knight": ("class_knight_model", 144, 160),
        "class_mage": ("class_mage_model", 144, 160),
        "class_ranger": ("class_ranger_model", 144, 160),
        "class_cleric": ("class_cleric_model", 144, 160),
        "class_rogue": ("class_rogue_model", 144, 160),
    }
    for name, (model_name, width, height) in specs.items():
        fit(models[model_name], width, height, bottom_align=True, margin=4).save(OUT / f"{name}.png")


def main() -> None:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing generated player model sheet: {SRC}")
    OUT.mkdir(parents=True, exist_ok=True)
    source = Image.open(SRC).convert("RGBA")
    cell_w = source.width // len(NAMES)
    models: dict[str, Image.Image] = {}
    for index, name in enumerate(NAMES):
        left = index * cell_w
        right = source.width if index == len(NAMES) - 1 else (index + 1) * cell_w
        models[name] = chroma_cutout(
            source.crop((left, 0, right, source.height)),
            "magenta",
            padding=8,
            keep_largest_only=True,
        )

    models["class_rogue_model"] = make_rogue(models["class_ranger_model"])

    for name, image in models.items():
        image.save(OUT / f"{name}.png")
    write_world_sprites(models)


if __name__ == "__main__":
    main()
