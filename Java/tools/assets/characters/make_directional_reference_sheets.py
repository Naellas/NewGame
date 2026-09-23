from __future__ import annotations

import sys as _bootstrap_sys
from pathlib import Path as _BootstrapPath
if not __package__:
    _bootstrap_sys.path.insert(0, str(_BootstrapPath(__file__).resolve().parents[3]))
from tools.project_paths import JAVA_ROOT, REPO_ROOT
from tools.assets.shared.asset_paths import find_asset

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path("assets")
SOURCE = ROOT / "source"

PLAYER_MODELS = [
    "player_model",
    "class_knight_model",
    "class_cleric_model",
    "class_ranger_model",
    "class_rogue_model",
]

NPC_MODELS = [
    "npc_baker_model",
    "npc_bartender_model",
    "npc_blacksmith_model",
    "npc_citizen_man_model",
    "npc_citizen_woman_model",
    "npc_elowen_model",
    "npc_garruk_model",
    "npc_innkeeper_model",
    "npc_kael_model",
    "npc_liora_model",
    "npc_marla_model",
    "npc_merchant_model",
    "npc_mira_sunwarden_model",
    "npc_nyx_model",
    "npc_orin_model",
    "npc_quartermaster_model",
    "npc_ren_model",
    "npc_rowan_model",
    "npc_sable_model",
    "npc_torin_model",
    "npc_vexa_model",
]


def fit(image: Image.Image, max_w: int, max_h: int) -> Image.Image:
    scale = min(max_w / image.width, max_h / image.height)
    out = image.resize((max(1, round(image.width * scale)), max(1, round(image.height * scale))), Image.Resampling.NEAREST)
    return out


def label_for(name: str) -> str:
    return name.removesuffix("_model")


def make_sheet(names: list[str], folder: str, out_name: str, columns: int) -> None:
    cell_w = 220
    cell_h = 330
    label_h = 30
    rows = (len(names) + columns - 1) // columns
    sheet = Image.new("RGBA", (columns * cell_w, rows * (cell_h + label_h)), (255, 0, 255, 255))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("arial.ttf", 15)
    except OSError:
        font = ImageFont.load_default()
    for index, name in enumerate(names):
        col = index % columns
        row = index // columns
        base_x = col * cell_w
        base_y = row * (cell_h + label_h)
        image = Image.open(find_asset(ROOT, f"{name}.png")).convert("RGBA")
        fitted = fit(image, cell_w - 28, cell_h - 16)
        x = base_x + (cell_w - fitted.width) // 2
        y = base_y + cell_h - fitted.height
        sheet.alpha_composite(fitted, (x, y))
        draw.rectangle((base_x, base_y + cell_h, base_x + cell_w, base_y + cell_h + label_h), fill=(20, 20, 30, 255))
        draw.text((base_x + 8, base_y + cell_h + 8), f"{index + 1}. {label_for(name)}", fill=(245, 238, 210, 255), font=font)
    SOURCE.mkdir(parents=True, exist_ok=True)
    sheet.save(SOURCE / out_name)


def main() -> None:
    make_sheet(PLAYER_MODELS, "player", "reference-player-directional-targets.png", 5)
    make_sheet(NPC_MODELS[:7], "npcs", "reference-npc-directional-targets-a.png", 7)
    make_sheet(NPC_MODELS[7:14], "npcs", "reference-npc-directional-targets-b.png", 7)
    make_sheet(NPC_MODELS[14:], "npcs", "reference-npc-directional-targets-c.png", 7)


if __name__ == "__main__":
    main()
