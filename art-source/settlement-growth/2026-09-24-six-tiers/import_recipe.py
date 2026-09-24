"""Reproduce this fixed atlas import; run from any directory. Requires Pillow."""
from pathlib import Path
import sys
from PIL import Image

BATCH = Path(__file__).resolve().parent
REPO = BATCH.parents[2]
sys.path.insert(0, str(REPO / "Java"))
from tools.assets.environments.slice_imagegen_village_assets import crop_cell

def run():
    buildings = Image.open(BATCH / "buildings.png").convert("RGBA")
    target = REPO / "Java/assets/environments/settlements/player_village/buildings/shared"
    target.mkdir(parents=True, exist_ok=True)
    # The generated row gutters are not quite evenly spaced; preserve the tall civic roofs.
    rows = [0, round(buildings.height * .335), round(buildings.height * .64), buildings.height]
    for row, family in enumerate(("residence", "workshop", "civic")):
        for col in range(6):
            cell = buildings.crop((round(col*buildings.width/6), rows[row], round((col+1)*buildings.width/6), rows[row+1]))
            box = cell.getchannel("A").getbbox()
            if box is None:
                raise ValueError("Empty building cell")
            cell = cell.crop(box)
            cell.thumbnail((240,240), Image.Resampling.LANCZOS)
            canvas = Image.new("RGBA", (256,256))
            canvas.alpha_composite(cell, ((256-cell.width)//2, 248-cell.height))
            canvas.save(target / f"settlement_{family}_tier{col+1}.png")
    ground_path = BATCH / "grounds.png"
    if ground_path.exists():
        grounds = Image.open(ground_path).convert("RGB")
        target = REPO / "Java/assets/environments/terrain/common"
        for index in range(6):
            cell = crop_cell(grounds,index%3,index//3,3,2)
            cell.resize((192,192),Image.Resampling.LANCZOS).save(target / f"settlement_ground_tier{index+1}.png")

if __name__ == "__main__":
    run()
