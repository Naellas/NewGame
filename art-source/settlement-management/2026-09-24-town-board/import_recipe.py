"""Import the reviewed town board; requires Pillow, overwrites one runtime sprite."""
from pathlib import Path
from PIL import Image
BATCH=Path(__file__).resolve().parent
REPO=BATCH.parents[2]
image=Image.open(BATCH/'original.png').convert('RGBA')
image=image.crop(image.getchannel('A').getbbox())
image.thumbnail((176,176),Image.Resampling.LANCZOS)
output=Image.new('RGBA',(192,192))
output.alpha_composite(image,((192-image.width)//2,184-image.height))
output.save(REPO/'Java/assets/environments/settlements/player_village/player_village_quest_board.png')
