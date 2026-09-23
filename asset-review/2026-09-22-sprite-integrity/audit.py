from pathlib import Path
from PIL import Image, ImageDraw
from datetime import datetime
import json, hashlib
root=Path('Java/assets'); out=Path('asset-review/2026-09-22-sprite-integrity')
if (out/'inventory.json').exists():
 raise SystemExit('Pre-edit inventory already exists; refusing to overwrite restoration baseline.')
rows=[]
for p in sorted(root.rglob('*.png'),key=lambda p:(p.stat().st_mtime,str(p))):
 if 'source' in p.parts: continue
 row={'path':p.as_posix(),'modified':datetime.fromtimestamp(p.stat().st_mtime).isoformat(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
 try:
  with Image.open(p) as im:
   im.load(); a=im.convert('RGBA').getchannel('A'); hist=a.histogram(); box=a.getbbox()
   row.update(size=list(im.size),mode=im.mode,empty=box is None,opaque=hist[255]==im.width*im.height,bbox=box)
   row['edge_contact']=bool(box and (box[0]==0 or box[1]==0 or box[2]==im.width or box[3]==im.height))
 except Exception as e: row['error']=str(e)
 rows.append(row)
(out/'inventory.json').write_text(json.dumps(rows,indent=2))
def sheet(items,name):
 canvas=Image.new('RGB',(1000,220*((len(items)+4)//5)),(65,69,76));d=ImageDraw.Draw(canvas)
 for i,r in enumerate(items):
  with Image.open(r['path']) as im:
   im=im.convert('RGBA');im.thumbnail((188,177),Image.Resampling.NEAREST)
   x=(i%5)*200;y=(i//5)*220;canvas.paste(im,(x+(200-im.width)//2,y),im)
   label=Path(r['path']).stem
   d.text((x+3,y+180),label[:28],fill='white');d.text((x+3,y+194),label[28:55],fill='white');d.text((x+3,y+207),r['modified'][:10],fill='white')
 canvas.save(out/name)
sheet(rows[:60],'oldest-60.png')
city=[r for r in rows if '/city/' in r['path'] and '/overworld/' not in r['path']]
sheet(city[:60],'oldest-city-60.png')
sheet(city[-20:],'newest-city-20.png')
print('Images',len(rows),'errors',sum('error'in r for r in rows),'empty',sum(r.get('empty',False) for r in rows))
print(json.dumps(rows[:12],indent=2))
