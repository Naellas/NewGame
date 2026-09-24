from pathlib import Path
import sys
from PIL import Image
root = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(root / "Java"))
from tools.assets.environments.import_water_imagegen_props import fit_to_canvas
source = Path(__file__).with_name("sedge-original.png")
output = root / "Java/assets/environments/terrain/biomes/marsh/deco_marsh_sedge_clump.png"
fit_to_canvas(Image.open(source).convert("RGBA")).save(output)
print(output)
