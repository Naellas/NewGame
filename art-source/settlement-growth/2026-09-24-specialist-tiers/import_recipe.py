"""Fixed source-batch import. Requires Pillow; overwrites its 90 named outputs."""
from pathlib import Path
import json, sys
from PIL import Image

BATCH=Path(__file__).resolve().parent
REPO=BATCH.parents[2]
sys.path.insert(0,str(REPO/'Java'))
from tools.assets.shared.universal_cutout import CutoutSettings, universal_cutout

GROUPS={
    'industry': (['warehouse','forestry_hut','mine'], [0,318,600,887]),
    'food': (['hunting_camp','farmstead','granary'], [0,315,577,887]),
    'civic': (['row','guild','watchtower'], [0,316,590,887]),
    'specialists': (['shrine','garden','blacksmith'], [0,328,598,887]),
    'provisions': (['bakery','apothecary','fishing_hut'], [0,312,584,887]),
}

def run():
    outputs=[]
    for group,(styles,rows) in GROUPS.items():
        source=f'{"repaired" if group in ("industry","provisions") else "strong"}-{group}.png'
        sheet=Image.open(BATCH/source).convert('RGBA')
        ys=[round(y*sheet.height/887) for y in rows]
        for row,style in enumerate(styles):
            # Generated columns have uneven gutters. Locate the quietest vertical
            # line near each expected division rather than clipping wider facades.
            alpha=sheet.getchannel('A')
            xs=[0]
            for col in range(1,6):
                center=round(col*sheet.width/6)
                radius=round(sheet.width/24)
                def score(x):
                    strip=alpha.crop((x,ys[row],x+1,ys[row+1]))
                    mass=sum(v>16 for v in strip.getdata())
                    return mass,abs(x-center)
                xs.append(min(range(center-radius,center+radius),key=score))
            xs.append(sheet.width)
            for tier in range(1,7):
                bounds=(xs[tier-1],ys[row],xs[tier],ys[row+1])
                cell=universal_cutout(sheet.crop(bounds),CutoutSettings(mode='alpha',
                    clean_spill=False,keep_largest_only=True,padding=4))
                cell.thumbnail((240,240),Image.Resampling.LANCZOS)
                canvas=Image.new('RGBA',(256,256))
                canvas.alpha_composite(cell,((256-cell.width)//2,248-cell.height))
                name=f'settlement_{style}_tier{tier}'
                target=REPO/'Java/assets/environments/settlements/player_village/buildings'/style/(name+'.png')
                target.parent.mkdir(parents=True,exist_ok=True)
                canvas.save(target)
                outputs.append({'id':name,'source':source,'crop':bounds,
                    'output':target.relative_to(REPO).as_posix()})
    (BATCH/'imports.json').write_text(json.dumps(outputs,indent=2)+'\n',encoding='utf-8')
    print(f'Imported {len(outputs)} specialist tier sprites')

if __name__=='__main__': run()
